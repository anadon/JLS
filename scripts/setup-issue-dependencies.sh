#!/usr/bin/env bash
#
# setup-issue-dependencies.sh — populate native GitHub "blocked by" issue
# dependency edges so implementation order is machine-computable.
#
# The planning session could set sub-issues and milestones but NOT native
# dependency edges (the MCP surface has no dependency writer, and `gh` was
# unavailable). Every open issue therefore has an empty dependency graph
# (blocked_by = 0), even though the ordering is known. This script writes
# the authoritative edge set — the same one recorded in the comment on
# tracking issue #224 ("Authoritative dependency graph & implementation
# order") — via the GitHub issue-dependencies REST API.
#
# Edge semantics: `CHILD BLOCKER` means CHILD is *blocked by* BLOCKER
# (BLOCKER must land first). Only open->open edges are encoded; closed
# prerequisites are done and omitted; tracking issues are aggregators and
# are omitted.
#
# Prerequisites:
#   - gh >= 2.40, authenticated (`gh auth status`)
#   - The issue-dependencies REST API is recent; if your GitHub/gh version
#     rejects it, the script reports the failure per edge and continues.
#
# Usage:
#   DRY_RUN=1 scripts/setup-issue-dependencies.sh    # print, don't mutate
#   scripts/setup-issue-dependencies.sh              # apply (safe to re-run)
#   OWNER=anadon REPO=JLS scripts/setup-issue-dependencies.sh
#
# Idempotent: an edge that already exists returns HTTP 422 and is treated
# as "already present" rather than an error.
#
set -euo pipefail

OWNER="${OWNER:-anadon}"
REPO="${REPO:-JLS}"

# --- authoritative edges: "CHILD BLOCKER" (child is blocked by blocker) -----
# Mirror of the #224 "Authoritative dependency graph" comment. Edit here and
# re-run to keep the native graph in sync with the source of truth.
EDGES=(
	# Module program
	"220 77"
	"221 77"
	"222 220"
	"223 78"  "223 220"
	"212 77"  "212 78"  "212 84"  "212 220"  "212 222"
	"95 78"   "95 84"
	"210 84"
	# Collaboration
	"170 167"
	"169 168"
	"171 167" "171 168" "171 169" "171 170"
	# HDL / FPGA
	"59 78"
	"61 59"   "61 78"
	"62 61"
	"63 61"
	"213 59"
	"215 213"
	"202 199" "202 201" "202 59"
	"216 200"
	# GUI
	"76 75"
	"162 84"  "162 91"
	"214 91"
	# Distribution
	"134 82"
	"185 184"
	"190 82"
	"191 82"
)

command -v gh >/dev/null 2>&1 || { echo "error: gh not installed" >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "error: run 'gh auth login'" >&2; exit 1; }

# Resolve issue number -> internal database id (the dependencies API keys on
# the blocking issue's id, not its number). Cached to avoid repeat lookups.
declare -A ID_CACHE
issue_id() {
	local n="$1"
	if [ -z "${ID_CACHE[$n]:-}" ]; then
		ID_CACHE[$n]="$(gh api "repos/$OWNER/$REPO/issues/$n" --jq '.id')"
	fi
	printf '%s' "${ID_CACHE[$n]}"
}

echo "== Native issue dependencies ($OWNER/$REPO) =="
fail=0
for edge in "${EDGES[@]}"; do
	read -r child blocker <<<"$edge"
	if [ -n "${DRY_RUN:-}" ]; then
		echo "DRY: #$child  blocked_by  #$blocker"
		continue
	fi
	bid="$(issue_id "$blocker")"
	# POST .../issues/{child}/dependencies/blocked_by  {issue_id: <blocker id>}
	if out="$(gh api -X POST \
			"repos/$OWNER/$REPO/issues/$child/dependencies/blocked_by" \
			-F "issue_id=$bid" 2>&1)"; then
		echo "  #$child  blocked_by  #$blocker  (added)"
	elif printf '%s' "$out" | grep -qiE '422|already|exist'; then
		echo "  #$child  blocked_by  #$blocker  (already present)"
	else
		echo "  #$child  blocked_by  #$blocker  FAILED: $(printf '%s' "$out" | head -1)" >&2
		fail=1
	fi
done

if [ "$fail" = 1 ]; then
	cat >&2 <<'EOF'

One or more edges failed. Most likely the issue-dependencies REST API is
not available on your gh/GitHub version yet. The relationships remain
authoritatively recorded in the #224 comment and the M0-M4 milestones;
re-run this script when the API is available, or add the edges in the
issue UI (the "Relationships" / "blocked by" control).
EOF
	exit 1
fi
echo "Done. Verify on any issue: gh api repos/$OWNER/$REPO/issues/212/dependencies/blocked_by --jq '.[].number'"
