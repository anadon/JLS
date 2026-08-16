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
| 3a. Fixer fleet: one Sonnet agent per issue rewrites the live GitHub issue per both reviews + purges in-content history | DONE — 605/605. Authoritative state: `/workflow-state/fix-status.json` (chunked completion by a second session after the original run's journal was rolled back by a container restart; GitHub edits were durable) |
| 3b. Reconcile pass: apply cross-issue dependency-edge fixes | PREPARED, NOT EXECUTED — 858 edits over 389 targets, ready as four self-contained workflow scripts in `/workflow-state/reconcile-chunks/rchunk-0[1-4].js`. CLAIM before running (commit a claim note, per the chunk protocol in the fix-phase history) to avoid two sessions racing. |
| 4. Tracking audit: Haiku agent per issue enforces native sub-issue hierarchy (TASK -> FEAT -> CAP per designators), labels, milestones; emits `gh` scripts for what this environment cannot do | QUEUED |
| 5. Capstone reviews: one Fable agent per open capstone (33 CAP-NN issues) reviews the full FEAT/TASK tree for readiness; reports to `capstone-reviews/cap-NN.md`, report-only | QUEUED |
| 6. Final deliverables: comment-purge `gh` script, Project-board sync `gh` script, consolidated report | QUEUED |

## Stage 3: state and what remains

Fix phase is COMPLETE (605/605). The original per-agent prompt is preserved
in `fix-issues-per-reviews.workflow.js` in this directory; per-issue fixer
outputs live in `/workflow-state/chunk-results/chunk-0[1-9].json`; merged
cross-issue edge edits (with requesting issue and rationale) are in
`/workflow-state/reconcile-input.json` (389 targets, 858 edits).

To finish stage 3, execute the four prepared reconcile workflows
(`/workflow-state/reconcile-chunks/rchunk-0[1-4].js`) — after committing a
claim. Caveat from the second session (in `fix-status.json`): the
superseded-comment purge manifest is INCOMPLETE — fixer reports for the
issues completed before the Aug-12 rollback were lost, so the comment-purge
script (stage 6) must be built by direct audit: for each issue, list live
comments and mark those fully superseded by the rewritten body (they carry
changelog/REPLAN content the body no longer references). This audit can fold
into stage 4's per-issue Haiku agents.

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
