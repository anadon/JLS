# Session handoff: JLS issue-backlog overhaul pipeline

Last updated: 2026-08-16. This file lets ANY fresh Claude session continue the
pipeline from durable state alone — no access to the original session's
container is required. Update this file and `fixer-state.json` whenever a
stage advances.

## Where things stand

| Stage | Status |
|---|---|
| 1. Review fleet: 605 open issues x 2 lenses (adversarial=Sonnet, visionary=Opus) | DONE — 1,210 `issue-NNNN.<lens>.finished.md` files in this directory |
| 2. Completeness audit: 605 Haiku judges renamed every review `.finished`/`.incomplete` | DONE — 1 real truncation repaired (0409 visionary), 1 false positive (0426) |
| 3. Fixer fleet: one Sonnet agent per issue rewrites the live GitHub issue per both reviews + purges in-content history; then a reconcile pass applies cross-issue dependency-edge fixes | IN PROGRESS — see `fixer-state.json`: `fix_done` (215) vs `fix_remaining` (390). Reconcile phase NOT started. |
| 4. Tracking audit: Haiku agent per issue enforces native sub-issue hierarchy (TASK -> FEAT -> CAP per designators), labels, milestones; emits `gh` scripts for what this environment cannot do | QUEUED |
| 5. Capstone reviews: one Fable agent per open capstone (33 CAP-NN issues) reviews the full FEAT/TASK tree for readiness; reports to `capstone-reviews/cap-NN.md`, report-only | QUEUED |
| 6. Final deliverables: comment-purge `gh` script, Project-board sync `gh` script, consolidated report | QUEUED |

## Stage 3: how to continue the fixer fleet

The workflow script is committed here as `fix-issues-per-reviews.workflow.js`
(it contains the full per-agent prompt — read it; it IS the spec). The
original run's journal cache is same-session-only, so a fresh session must
re-scope instead of resuming: edit the script's `const ISSUES = [...]` to the
`fix_remaining` list from `fixer-state.json`, then launch it with the
Workflow tool. Do NOT re-run issues in `fix_done` — their GitHub issues are
already rewritten; re-running would double-edit them.

The Fix phase returns per-issue `other_issue_edits` (cross-issue dependency
edge changes) and `superseded_comment_ids`. The 215 finished issues'
contributions are already captured in `fixer-state.json`
(`edge_edits_so_far`, 563 entries; `superseded_comments_so_far`, 623
entries). A re-scoped run only reports these for the issues it runs, so the
Reconcile phase and comment-purge script must MERGE the new results with the
saved ones. Simplest: strip the Reconcile phase from the re-scoped script
(return the Fix results), merge edge edits with `edge_edits_so_far`, group by
target issue, then run the reconcile prompts (in the script) over the merged
set as a second workflow.

Operational notes learned the hard way:
- Container restarts kill workflows AND in-container watchdogs silently, with
  no notification. Guard every long run with a server-side check-in
  (claude-code-remote `send_later`, ~240 min, self-re-arming) that checks the
  workflow journal mtime and resumes/relaunches on staleness.
- Same-session resume: `Workflow({scriptPath, resumeFromRunId: "wf_81379076-1db"})`
  replays completed agents from cache. Only valid in the session that
  launched it.
- Pace observed: ~13 issues/hour (container CPU-bound). Fix remainder ~30 h.
- GitHub writes go through the MCP `mcp__github__*` tools (no `gh`, no
  GraphQL; the proxy blocks non-pinned GraphQL). Rate-limit retries: 4x.

## Stage 4 spec (queued): tracking audit

One Haiku agent per issue (605). Enforce with `issue_read`/`issue_write`/
`sub_issue_write`: every TASK-CNNN-K is a sub-issue of its FEAT, every
FEAT-CNN-K a sub-issue of its CAP-NN (designators are in titles/machine
headers); labels and milestones consistent with the rewritten machine
headers. `list_issue_fields` is empty and issue types are unavailable
(personal account) — skip those. Anything requiring the Project (v2) board
CANNOT be done from a remote container (no gh, GraphQL blocked): collect
per-issue board actions into `issue-reviews/project-board-sync.sh` (a `gh`
script with board-field discovery at top) for the user to run locally.

## Stage 5 spec (queued): capstone readiness reviews

One Fable agent per open capstone. The 33 open CAP issues (numbers):
296 (CAP-00), 301 (02), 297 (04), 298 (05), 300 (06), 302 (07), 304 (08),
306 (09), 308 (10), 305 (12), 309 (14), 310 (15), 311 (16), 312 (17),
313 (18), 502 (21), 504 (23), 505 (24), 506 (25), 507 (26), 511 (27),
512 (28), 513 (29), 514 (30), 515 (31), 516 (32), 517 (33), 518 (34),
519 (35), 520 (36), 521 (37), 522 (38), 888 (39).
Each agent: walk the capstone's full FEAT/TASK tree in its POST-FIX state
plus relevant code/docs in this repo; judge readiness-to-undertake
(decomposition complete/coherent; acceptance criteria compose upward to the
capstone promise; dependency chains real and acyclic; gaps/staleness named).
Output: `capstone-reviews/cap-NN.md` on this branch. Report-only — no issue
edits.

## Stage 6: user-side deliverables

- `comment-purge.sh`: `gh api -X DELETE /repos/anadon/jls/issues/comments/<id>`
  for every entry in the merged superseded-comments list (623 so far; more
  arrive as the fixer runs).
- `project-board-sync.sh`: from stage 4.
- Consolidated report of all stages. The user then: runs both scripts,
  rules on `## Proposed disposition` sections fixers left on issues whose
  reviews argued close/merge/split, and acts on capstone-review flags.

## Conventions

- Branch: `claude/github-issue-review-agents-j99xga`. Commit and push after
  every meaningful state change; the container is ephemeral.
- Repo: anadon/jls (owner "anadon", repo "jls"). Reviews dated 2026-08-08;
  fixer verifies findings against live issues before applying.
- Do not close issues, do not create issues, do not post comments (except
  where a stage explicitly says otherwise). Proposed dispositions go in the
  issue body.
