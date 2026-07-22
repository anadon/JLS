# Setting up the grand-architecture project, milestones, and labels

The grand-architecture module program was planned from a remote session
where the GitHub surface was an MCP server with **no milestone/label
creation and no `gh`**. So the tracking issue (#224) and its sub-issues
exist, but the *repo-level scaffolding* — Milestone objects, a label
taxonomy, and a Projects v2 board — does not. This page is the runbook to
create it from a machine where `gh` is available (e.g. running `claude`
locally).

Everything here is captured as one idempotent script,
[`scripts/setup-architecture-project.sh`](../scripts/setup-architecture-project.sh);
this document explains what it does, the mappings it encodes, and the
manual `gh` commands if you prefer to run steps piecemeal.

## Prerequisites

```sh
gh --version          # >= 2.40
gh auth status        # authenticated against github.com
# Projects v2 needs an extra scope the default login may lack:
gh auth refresh -s project,read:project
```

## Run it

```sh
# from the repo root, on the architecture branch or master
DRY_RUN=1 scripts/setup-architecture-project.sh   # preview every mutation
scripts/setup-architecture-project.sh             # do it (safe to re-run)

# variants
SKIP_PROJECT=1 scripts/setup-architecture-project.sh   # milestones + labels only
OWNER=anadon REPO=JLS scripts/setup-architecture-project.sh
```

The script is **idempotent**: milestones are created only if a milestone
of that exact title is absent, labels are `--force`d (create-or-update),
and issue milestone/label assignments are naturally idempotent. Re-running
after new issues are added simply extends the assignments.

If you are driving this with `claude` locally, a one-line prompt is
enough, e.g. *"run scripts/setup-architecture-project.sh; if the Projects
step is skipped for scope, refresh the `project` scope and re-run just
that step."*

## What it creates

### 1. Milestones (M0–M4)

Titles and issue membership mirror the phase checklists in #224. The
script assigns each issue to its milestone via
`PATCH /repos/:owner/:repo/issues/:n`.

| Milestone | Issues |
|---|---|
| **M0 — Boundary seed & type-safety** | #78, #167 (shipped) · #93, #94, #95 |
| **M1 — Headless kernel (keystone)** | #77 |
| **M2 — Module runtime & extension points** | #220, #223, #222 |
| **M3 — Consumer modules** | #84, #59, #61, #62, #63, #213, #215, #163, #168, #169, #170, #171, #200, #214, #76 |
| **M4 — External providers & scale** | #212, #221 |

The umbrella tracker #224 is intentionally left milestone-less.

Manual equivalent for one milestone + assignment:

```sh
num=$(gh api -X POST repos/anadon/JLS/milestones \
        -f title="M1 — Headless kernel (keystone)" \
        -f description="Extract the enforced-headless jls.core (#77)." --jq '.number')
gh api -X PATCH repos/anadon/JLS/issues/77 -F milestone="$num"
```

### 2. Labels

A small, orthogonal taxonomy — a program label, a decision kind, phase
labels, and area labels:

| Label | Purpose |
|---|---|
| `architecture` | the whole module program (tracking #224) |
| `kind:decision` | a decision-and-record issue (#221, #222) |
| `phase:M0`…`phase:M4` | mirror the milestones for filtering without the milestone dropdown |
| `area:core` / `area:module` / `area:gui` / `area:hdl` / `area:collab` / `area:batch` / `area:distribution` | subsystem |

`phase:*` labels are applied to exactly the issues in each milestone.
`area:*`/`kind:*`/`architecture` are **seeded on the core program issues**
(#77/#78/#167/#212/#220–#224 and a few consumers) so the taxonomy exists
and is correct where it is applied; extend the `LABEL_ISSUES` map in the
script to cover the rest of the backlog as you triage — the script does
not attempt to auto-classify all ~50 open issues by area, on purpose.

Manual equivalent:

```sh
gh label create "phase:M1" --color 7fdbca --description "Program phase M1" --force
gh issue edit 77 --add-label "area:core,phase:M1,architecture"
```

### 3. Projects v2 board

Creates a user project titled **"JLS Grand Architecture"** (idempotent by
title) and adds the tracker plus every phased issue as items. Because the
milestones are already assigned, you do **not** need a custom field for
the phases — open the board and add a **Group by: Milestone** view to get
M0–M4 swimlanes for free.

Manual equivalent:

```sh
num=$(gh project create --owner anadon --title "JLS Grand Architecture" --format json --jq '.number')
gh project item-add "$num" --owner anadon --url https://github.com/anadon/JLS/issues/224
# …repeat item-add per issue; then in the UI: New view → Board → Group by → Milestone
```

If `gh project` errors with a scope message, run the
`gh auth refresh -s project,read:project` above and re-run
`SKIP_PROJECT=` (i.e. just the project step) — the milestone/label steps
already done are untouched.

## Cross-links and dependencies (optional, mostly already done)

- **Narrative cross-links** between issues already exist: the new issues'
  bodies and the comments on #77/#78/#167/#212/#59/#163/#96/#84 reference
  #224 and each other. Nothing to run.
- **Sub-issue hierarchy** (#220–#223 under #224) is already created via
  the native sub-issues API. To add more children later:

  ```sh
  # GraphQL: attach issue N as a sub-issue of #224
  gh api graphql -f query='mutation($p:ID!,$c:ID!){
    addSubIssue(input:{issueId:$p, subIssueId:$c}){ issue { number } } }' \
    -f p="$(gh api repos/anadon/JLS/issues/224 --jq .node_id)" \
    -f c="$(gh api repos/anadon/JLS/issues/NNN --jq .node_id)"
  ```

- **Blocking dependencies** (a stronger "blocked by" than a mention) can be
  set with the issue-dependencies REST API if you want the machine-readable
  edges of the spine — e.g. #212 blocked by #220 and #222; #220, #221, #222
  blocked by #77:

  ```sh
  gh api -X POST repos/anadon/JLS/issues/212/dependencies/blocked_by \
    -F issue_id="$(gh api repos/anadon/JLS/issues/220 --jq .id)"
  ```

  (This endpoint is newer; skip it if your `gh`/API version rejects it —
  the milestone + tracker structure already conveys the ordering.)

## After running

Verify at `https://github.com/anadon/JLS/milestones` and the repo's
**Projects** tab, then update the checkboxes in #224 as milestones
complete. When `docs/grand-architecture.md` merges to `master`, the
`blob/<branch>` links in the program's issues can be repointed to
`blob/master` (a find-and-replace in the five issue bodies).
