#!/usr/bin/env bash
#
# sync-roadmap-project.sh — ensure every OPEN issue in the repo is tracked as
# an item on the "JLS Roadmap" Projects v2 board.
#
# The board is the planning surface: an open issue that is not on it is work
# nobody has scheduled. This script closes that gap. It only ever ADDS items
# — it never removes anything, so closed issues left on the board (history,
# recently-closed columns) are preserved untouched.
#
# It is SAFE TO RE-RUN: the add set is computed as (open issues) minus
# (issues already on the board), so a second run with no new issues does
# nothing at all.
#
# Prerequisites:
#   - gh >= 2.40 (`gh --version`), authenticated (`gh auth status`)
#   - The token needs the `project` scope:
#       gh auth refresh -s project,read:project
#
# Usage:
#   scripts/sync-roadmap-project.sh                  # add missing issues
#   DRY_RUN=1 scripts/sync-roadmap-project.sh        # report, don't mutate
#   OWNER=anadon REPO=JLS scripts/sync-roadmap-project.sh
#   PROJECT_TITLE="JLS Roadmap" scripts/sync-roadmap-project.sh
#
# Exit status:
#   0  board is in sync (either it already was, or the adds succeeded)
#   1  precondition failure (no gh, no auth, board not found, missing scope)
#   2  one or more item-adds failed; the failures are listed on stderr
#
set -euo pipefail

OWNER="${OWNER:-anadon}"
REPO="${REPO:-JLS}"
PROJECT_TITLE="${PROJECT_TITLE:-JLS Roadmap}"
ISSUE_LIMIT="${ISSUE_LIMIT:-2000}"   # hard ceiling on issues considered
export GH_REPO="$OWNER/$REPO"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }
note() { printf '%s\n' "$*" >&2; }

command -v gh >/dev/null 2>&1 || die "gh not on PATH"
command -v python3 >/dev/null 2>&1 || die "python3 not on PATH"
gh auth status >/dev/null 2>&1 || die "gh is not authenticated (run: gh auth login)"

# --- resolve the board -------------------------------------------------------

note "==> resolving project '$PROJECT_TITLE' for owner '$OWNER'"
projects_json="$(gh project list --owner "$OWNER" --format json 2>&1)" || {
	printf '%s\n' "$projects_json" >&2
	die "gh project list failed — the token may lack the 'project' scope (gh auth refresh -s project,read:project)"
}

# Resolve title -> number in one shot; a missing or ambiguous title is fatal
# rather than a guess, because guessing here writes to the wrong board.
resolved="$(
	printf '%s' "$projects_json" | python3 -c '
import json, sys
title = sys.argv[1]
data = json.load(sys.stdin)
hits = [p for p in data.get("projects", []) if p.get("title") == title]
if len(hits) != 1:
    print(f"matched {len(hits)} projects", file=sys.stderr)
    sys.exit(1)
print(hits[0]["number"], hits[0]["url"])
' "$PROJECT_TITLE"
)" || die "expected exactly one project titled '$PROJECT_TITLE' for owner '$OWNER'"

PROJECT_NUMBER="${resolved%% *}"
PROJECT_URL="${resolved#* }"

note "    project #$PROJECT_NUMBER — $PROJECT_URL"

# --- gather both sides of the comparison -------------------------------------

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

note "==> listing open issues in $GH_REPO"
gh issue list --state open --limit "$ISSUE_LIMIT" --json number,title >"$workdir/open.json"

note "==> listing items already on the board"
gh project item-list "$PROJECT_NUMBER" --owner "$OWNER" --format json \
	--limit "$ISSUE_LIMIT" >"$workdir/items.json"

cat >"$workdir/diff.py" <<'PY'
import json, sys

repo, open_path, items_path = sys.argv[1:4]

with open(open_path) as fh:
    open_issues = json.load(fh)
with open(items_path) as fh:
    items = json.load(fh).get("items", [])

# Board items are matched on issue number *within this repo*, so a board that
# also tracks another repo's issues cannot produce false matches. Items with
# no repository field recorded are assumed to be this repo's.
on_board = set()
for item in items:
    content = item.get("content") or {}
    if content.get("type") != "Issue":
        continue
    if content.get("repository") not in (repo, None):
        continue
    on_board.add(content["number"])

for issue in sorted(open_issues, key=lambda i: i["number"]):
    if issue["number"] not in on_board:
        print(f'{issue["number"]}\t{issue["title"]}')

print(
    f"    open={len(open_issues)} tracked_on_board={len(on_board)}",
    file=sys.stderr,
)
PY

# "<number>\t<title>" for every open issue absent from the board.
missing="$(python3 "$workdir/diff.py" "$GH_REPO" "$workdir/open.json" "$workdir/items.json")"

if [ -z "$missing" ]; then
	note "==> board is already in sync: every open issue is tracked"
	exit 0
fi

missing_count="$(printf '%s\n' "$missing" | wc -l | tr -d ' ')"
note "==> $missing_count open issue(s) missing from the board"

# --- add them ----------------------------------------------------------------

failed=0
while IFS=$'\t' read -r number title; do
	[ -n "$number" ] || continue
	url="https://github.com/$GH_REPO/issues/$number"
	if [ -n "${DRY_RUN:-}" ]; then
		printf 'DRY: gh project item-add %s --owner %s --url %s   # #%s %s\n' \
			"$PROJECT_NUMBER" "$OWNER" "$url" "$number" "$title"
		continue
	fi
	printf '    + #%-5s %s\n' "$number" "$title" >&2
	if ! gh project item-add "$PROJECT_NUMBER" --owner "$OWNER" --url "$url" >/dev/null; then
		note "    !! failed to add #$number"
		failed=$((failed + 1))
	fi
done <<<"$missing"

if [ -n "${DRY_RUN:-}" ]; then
	note "==> DRY_RUN: nothing was mutated"
	exit 0
fi

if [ "$failed" -gt 0 ]; then
	die "$failed of $missing_count item-add(s) failed"
fi

note "==> added $missing_count issue(s); board is in sync"
