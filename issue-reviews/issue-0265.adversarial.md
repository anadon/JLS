# Issue #265: CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

Stage 1 (the advisory `Build (macOS, JDK 25)` lane, `ci.yml:259-297`) is real,
landed, and matches the body's description — that part is sound and
independently verified against the checked-out tree. But the issue itself,
as a work specification, is internally contradicted by its own most recent
comment, carries acceptance criteria that can be satisfied without achieving
the stated goal, and has scope-crept into asserting cross-platform CI
authority from a nominally macOS-only feature. Nine separate
REPLAN/dedup/boundary comments in three weeks, several reversing each
other, are themselves evidence the issue is not a stable spec to build
against right now.

## Findings, most severe first

### 1. The body's Completion Criteria (DoD) is stale and contradicts the issue's own latest comment — real risk of duplicated work
The body's unchecked DoD boxes still list as this issue's own work: "Burn-in
failures fixed at source... failure-taxonomy note... (Stage 2)", "Both
headless lanes... promoted to required... (Stage 3)", HDL execute-evidence
(Stage 4), display-suite arming (Stage 5), and the JaCoCo floor (Stage 6).
But the newest comment (2026-08-08T17:20:50Z, `#5227247068`) states
outright: *"Stages 2-6 of this feature are discharged through #406 and
#386... Closed as duplicates: #667 TASK-C265-1, #668 TASK-C265-2, #669
TASK-C265-3, #670 TASK-C265-4."* The body was never rewritten to match
(this repo's own convention, cited in the same comment thread, is that
"bodies are never edited... #489" — corrections live only in comments).
A contributor who reads the body's DoD as the spec — which is exactly what
a DoD section is for — will start Stage 2/4/5/6 work already claimed by
#406/#386 and produce the exact double-edit to `ci.yml` the issue's own
comment (#5181437546) warns against: *"Whoever executes first must comment
on the other before touching `ci.yml`, or the same four edits land
twice."* **Recommendation:** before anyone picks up work here, the body
itself (not just a comment) must be edited to drop Stages 2-6 from this
issue's own DoD/roster and mermaid graph, leaving only Stage 1 (landed) and
Stages 7-9. As written, the file a contributor opens and the ground truth
disagree.

### 2. The machine block (`requires_tasks`, roster table) is also stale, and nothing enforces staleness detection
`requires_tasks: []` and every unlanded roster row reads "Planned
(unfiled)" in the body, even though comment `#5227056475` records seven
filed children and comment `#5227247068` records four of those closed as
duplicates thirty-eight minutes later. If any tooling (bot, dashboard, or a
future agent) parses the YAML machine block as source of truth — which is
its stated purpose ("Status & Dependency Graph") — it will compute an
incorrect open/closed state. There is no automated check anywhere in this
repo (verified: no CI step validates issue machine blocks) that a
REPLAN comment's claims were folded back into the body. **Recommendation:**
either the body must be edited on every REPLAN (contradicting the repo's
stated "bodies are never edited" convention), or the machine block must be
dropped as unreliable and downstream tooling pointed at the comment stream
instead — the issue can't have it both ways.

### 3. Promotion criteria (§4 invariant 1, DoD Stage 3/9) never mention job timeout, and the repo has zero `timeout-minutes` anywhere
§4 invariant 1 says a stage may drop `continue-on-error` once "20
consecutive runs, at most one failure" are recorded. Verified against
`.github/workflows/ci.yml` (1145 lines): `grep -n timeout-minutes` returns
zero matches in the whole file — no job in the workflow, including
`macos` and `macos-gui`, has a bounded runtime. The issue's own comment
`#5227056475` flags this precisely: *"a required macOS job with no
timeout — the platform with a network-dependent Homebrew install — is a
six-hour block on every pull request... must land before #667 or #673
drops continue-on-error."* That dependency (#374) is noted in a comment but
is **not** encoded as a precondition anywhere in §4's invariants or the
Stage 3/9 DoD checkboxes. As written, a literal reading of the DoD lets
someone satisfy "20 green runs, continue-on-error dropped" and check the
box while shipping an unbounded-runtime required check — exactly the
failure mode the comment warns about, and exactly the kind of gap between
stated verification and actual goal this lens is asked to find.
**Recommendation:** add "the job under promotion carries a
`timeout-minutes` bound" as an explicit §4 invariant, not just a comment
aside.

### 4. Stage 9's promotion target can silently downgrade without failing any stated check
`ci.yml:594-676` and the Re-planning Protocol both document that when
Screen Recording (TCC) permission is withheld — which the workflow's own
comment calls the *expected* outcome on hosted runners ("This FREQUENTLY
fails: the system TCC.db is SIP-protected (even root cannot write it)") —
the rig falls back to "degraded mode (window-map as boot gate)" instead of
asserting a non-blank screenshot. The Re-planning Protocol then says: *"if
the non-blank screenshot gate proves permanently unreachable, REPLAN the
promotion criterion for the rig."* So the "required" check named `GUI boot
(macOS, WindowServer)` can end up meaning something strictly weaker than
its Linux/Windows siblings (which do assert non-blank screenshots per
README's Wayland section) while carrying the identical byte-stable name
and identical "required" status — a required check whose actual guarantee
varies run to run based on ambient CI permissions state, with the
downgrade path pre-authorized by the issue itself rather than flagged as a
regression. **Recommendation:** if degraded mode becomes the permanent
steady state, the check's name or description should say so explicitly
(e.g. distinguish "window-map only" from "window-map + non-blank
screenshot") rather than reusing the stronger-sounding name silently.

### 5. Scope creep: a "macOS lane" issue asserts binding invariants over the Linux job
§4 invariant 2 states: *"The Linux `build` job remains sole gating
authority for the JaCoCo ratchet... and SpotBugs until Stage 6 lands
per-OS floors"* — verified real (`pom.xml:348`, `coverage-ratchet`
execution exists and is Linux-only today). That is a constraint on the
Linux job, not a macOS deliverable, asserted from an issue whose title and
Capability Statement say it "owns the macOS lanes." The issue also claims
program-wide "burn-in/promotion machinery and staging" ownership in its
own title suffix ("...promote the cross-platform suites to required
checks") while comment `#5227247068` now says the actual promotion work
(Stages 2-6) lives in #406. The title is consequently no longer an
accurate description of what this issue does after its own latest
disposition. **Recommendation:** either narrow the title to "macOS
headless lane + Stages 7-9" to match current ownership, or explicitly keep
§4's Linux-affecting invariants but cite that they're duplicated/binding on
#406 too so a reader of #406 doesn't miss them.

### 6. Promotion to "required" is a manual, unverifiable GitHub Settings action with no in-repo evidence artifact
Open Question 3 concedes: *"Promotion registration is a maintainer console
action (branch protection)... each promotion REPLAN should record that the
registration actually happened."* There is no test, script, or CI step in
this repo (checked: no `branch-protection` references in `.github/`) that
asserts required-check membership matches the DoD's claims. A DoD box like
"promoted to required per the 20-run rule" can be checked off on the
strength of a comment's prose with no verifiable state — gameable in the
literal sense the lens asks about (the stated verification, a checked box
+ a comment claiming a console click happened, can pass while the actual
branch-protection state is wrong or reverted later with nothing to catch
it). **Recommendation:** low-cost fix — a scheduled workflow that calls the
branch-protection API and diffs required-check names against the
byte-stable names this issue pins (`Build (macOS, JDK 25)`, `GUI boot
(macOS, WindowServer)`) would make this self-verifying instead of
attestation-by-comment.

### 7. Process overhead is disproportionate to remaining engineering content
Eight comments across twelve days, several of which are pure dedup/boundary
adjudications against sibling issues (#111, #317, #406, #386) that
reverse each other (Stage 2's taxonomy ownership alone moves from "owned by
#265" in comment 4 to "consumed, not produced, by #265" in comment 6 to
"never owned by #265, now explicit on #406" in comment 7). None of this
changed a line of code. For a reviewer or new contributor, distinguishing
current truth from superseded prose requires reading the entire comment
history in order — the machine block and body, which exist precisely to
avoid that, are the stale parts (see #1, #2). This is a process-cost
finding, not a code one, but it is a real tax on anyone picking up Stage
7/8/9 next.

## What's solid
- Stage 1's landed lane (`ci.yml:259-297`) matches the body's description
  exactly, including the byte-stable `name:` and the `continue-on-error`
  gate — verified in the checked-out tree.
- The `HotkeysHelpAccuracyTest` VK_DELETE fix citation
  (`test/jls/HotkeysHelpAccuracyTest.java:111-125`) is accurate and a
  genuine, well-evidenced macOS-only bug catch — good motivating evidence
  for the feature's value.
- The `/proc`-gated test citations (`FileHandleReleaseTest.java:44`,
  `UntrustedFileHardeningTest.java:75`) are accurate; Stage 8's premise
  (these need a macOS analogue or a narrow documented skip) is real and
  well-scoped, not speculative.
- §4's "no broad `assumeTrue(!isMac)`, fix at source" invariant is a good,
  concrete, checkable engineering principle — the one part of this issue
  that reads like a spec rather than bookkeeping.
