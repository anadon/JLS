#!/usr/bin/env bash
#
# setup-architecture-project.sh — one-shot, idempotent GitHub setup for the
# grand-architecture module program (tracking issue #224).
#
# Creates what the MCP-only remote session could NOT create because `gh`
# was unavailable there: real Milestone objects, a label taxonomy, a
# Projects v2 board, and the milestone/label assignments that connect the
# existing issues to the program described in docs/grand-architecture.md.
#
# It is SAFE TO RE-RUN: milestones are created only if absent, labels use
# `--force` (create-or-update), and issue PATCH/edit calls are idempotent.
# Project item-adds may warn on a second run; that is harmless.
#
# Prerequisites:
#   - gh >= 2.40 (`gh --version`)
#   - Authenticated: `gh auth status`
#   - For the Projects v2 step, the token needs the `project` scope:
#       gh auth refresh -s project,read:project
#
# Usage:
#   scripts/setup-architecture-project.sh                 # do everything
#   DRY_RUN=1 scripts/setup-architecture-project.sh       # print, don't mutate
#   SKIP_PROJECT=1 scripts/setup-architecture-project.sh  # milestones+labels only
#   OWNER=anadon REPO=JLS scripts/setup-architecture-project.sh
#
set -euo pipefail

OWNER="${OWNER:-anadon}"
REPO="${REPO:-JLS}"
PROJECT_TITLE="${PROJECT_TITLE:-JLS Grand Architecture}"
export GH_REPO="$OWNER/$REPO"   # default repo for `gh issue`/`gh label`

run() {  # echo + execute, or just echo under DRY_RUN
	if [ -n "${DRY_RUN:-}" ]; then
		printf 'DRY: %s\n' "$*"
	else
		"$@"
	fi
}

require_gh() {
	command -v gh >/dev/null 2>&1 || { echo "error: gh not installed" >&2; exit 1; }
	gh auth status >/dev/null 2>&1 || { echo "error: run 'gh auth login'" >&2; exit 1; }
}

# --- phase -> milestone metadata -------------------------------------------
# Titles match the M0..M4 sections of tracking issue #224.
declare -a PHASES=(M0 M1 M2 M3 M4)
declare -A M_TITLE=(
	[M0]="M0 — Boundary seed & type-safety"
	[M1]="M1 — Headless kernel (keystone)"
	[M2]="M2 — Module runtime & extension points"
	[M3]="M3 — Consumer modules"
	[M4]="M4 — External providers & scale"
)
declare -A M_DESC=(
	[M0]="Registry (#78) and op layer (#167) shipped; complete type-safety #93/#94/#95. See docs/grand-architecture.md."
	[M1]="Extract the enforced-headless jls.core (#77) — the root module every other module requires."
	[M2]="Module/plugin runtime (#220), extension-point catalog (#223), isolation/trust decision (#222)."
	[M3]="gui decomposition (#84), HDL modules (#59/#61/#62/#63/#213/#215), collab (#163 stack), batch/test (#200/#214), theme (#76)."
	[M4]="External element providers (#212, demand-gated) and the CPU-scale simulation decision (#221)."
)
# Issue -> phase assignment (authoritative source: tracking issue #224).
declare -A M_ISSUES=(
	[M0]="78 167 93 94 95"
	[M1]="77"
	[M2]="220 223 222"
	[M3]="84 59 61 62 63 213 215 163 168 169 170 171 200 214 76"
	[M4]="212 221"
)

# --- label taxonomy ---------------------------------------------------------
# name|color|description   (colors are 6-hex, no leading '#')
declare -a LABELS=(
	"architecture|5319e7|Grand-architecture module program (tracking #224)"
	"kind:decision|d93f0b|A decision-and-decision-record issue; outcome recorded in ARCHITECTURE.md"
	"phase:M0|c5def5|Program phase M0 — boundary seed & type-safety"
	"phase:M1|7fdbca|Program phase M1 — headless kernel"
	"phase:M2|bfd4f2|Program phase M2 — module runtime & extension points"
	"phase:M3|fef2c0|Program phase M3 — consumer modules"
	"phase:M4|f9d0c4|Program phase M4 — external providers & scale"
	"area:core|0e8a16|jls.core kernel: model, registry, sim, persistence, ops"
	"area:module|1d76db|Module/plugin runtime and extension points"
	"area:gui|fbca04|jls.edit/jls.ui — the only AWT layer"
	"area:hdl|006b75|HDL export/import/co-sim and board/bitstream flow"
	"area:collab|b60205|Pure-P2P collaboration modules"
	"area:batch|0052cc|Headless batch/grading/export services"
	"area:distribution|5319e7|Reproducible/attested packaging & supply chain"
)
# Seed area/kind labels on the core program issues (extend later as desired).
declare -A LABEL_ISSUES=(
	[architecture]="220 221 222 223 224 77 78 167"
	[kind:decision]="221 222"
	[area:core]="77 78 167"
	[area:module]="220 222 223 212"
	[area:gui]="84 214 76"
	[area:hdl]="59 213 215"
	[area:collab]="163"
	[area:batch]="200"
)

# --- steps ------------------------------------------------------------------
milestone_number() {  # print the number of a milestone by exact title, or empty
	gh api "repos/$OWNER/$REPO/milestones?state=all&per_page=100" \
		--jq ".[] | select(.title==\"$1\") | .number" 2>/dev/null | head -n1
}

setup_milestones() {
	echo "== Milestones =="
	for p in "${PHASES[@]}"; do
		local title="${M_TITLE[$p]}" num
		num="$(milestone_number "$title" || true)"
		if [ -z "$num" ]; then
			if [ -n "${DRY_RUN:-}" ]; then
				echo "DRY: create milestone '$title'"; num="?"
			else
				num="$(gh api -X POST "repos/$OWNER/$REPO/milestones" \
					-f title="$title" -f description="${M_DESC[$p]}" --jq '.number')"
			fi
			echo "  created $p -> #milestone $num"
		else
			echo "  exists  $p -> #milestone $num"
		fi
		for issue in ${M_ISSUES[$p]}; do
			[ "$num" = "?" ] && { echo "DRY: assign #$issue -> $title"; continue; }
			run gh api -X PATCH "repos/$OWNER/$REPO/issues/$issue" -F "milestone=$num" \
				>/dev/null && echo "  assigned #$issue -> $p"
		done
	done
}

setup_labels() {
	echo "== Labels =="
	for spec in "${LABELS[@]}"; do
		IFS='|' read -r name color desc <<<"$spec"
		run gh label create "$name" --color "$color" --description "$desc" --force \
			>/dev/null && echo "  label $name"
	done
	for name in "${!LABEL_ISSUES[@]}"; do
		for issue in ${LABEL_ISSUES[$name]}; do
			run gh issue edit "$issue" --add-label "$name" >/dev/null \
				&& echo "  #$issue += $name"
		done
	done
	# Apply the phase labels to the same issues the milestones cover.
	for p in "${PHASES[@]}"; do
		for issue in ${M_ISSUES[$p]}; do
			run gh issue edit "$issue" --add-label "phase:$p" >/dev/null \
				&& echo "  #$issue += phase:$p"
		done
	done
}

project_number() {  # print number of a user project by exact title, or empty
	gh project list --owner "$OWNER" --format json \
		--jq ".projects[] | select(.title==\"$PROJECT_TITLE\") | .number" 2>/dev/null | head -n1
}

setup_project() {
	echo "== Project (v2) =="
	if ! gh project list --owner "$OWNER" >/dev/null 2>&1; then
		echo "  skip: token lacks 'project' scope. Run: gh auth refresh -s project,read:project" >&2
		return 0
	fi
	local num; num="$(project_number || true)"
	if [ -z "$num" ]; then
		if [ -n "${DRY_RUN:-}" ]; then echo "DRY: create project '$PROJECT_TITLE'"; return 0; fi
		num="$(gh project create --owner "$OWNER" --title "$PROJECT_TITLE" --format json --jq '.number')"
		echo "  created project #$num"
	else
		echo "  exists project #$num"
	fi
	# Add the tracker + every phased issue as items. Milestones are already
	# assigned, so in the board UI you can "Group by: Milestone" with no
	# custom field. (Projects v2 surfaces the native Milestone automatically.)
	local all="224 ${M_ISSUES[M0]} ${M_ISSUES[M1]} ${M_ISSUES[M2]} ${M_ISSUES[M3]} ${M_ISSUES[M4]}"
	for issue in $all; do
		run gh project item-add "$num" --owner "$OWNER" \
			--url "https://github.com/$OWNER/$REPO/issues/$issue" >/dev/null 2>&1 \
			&& echo "  item + #$issue" || true
	done
	echo "  Tip: open the board, add a 'Group by: Milestone' view for the M0..M4 swimlanes."
}

main() {
	require_gh
	setup_milestones
	setup_labels
	[ -n "${SKIP_PROJECT:-}" ] || setup_project
	echo "Done. Review: https://github.com/$OWNER/$REPO/milestones and the Projects tab."
}
main "$@"
