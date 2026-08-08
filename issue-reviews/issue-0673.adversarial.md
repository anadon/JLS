# Issue #673: TASK-C265-7: `GUI boot (macOS, WindowServer)` earns its required check, with TCC denial classified as environment and never as a JLS failure
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what the issue actually asks

Task-template issue under feature #265 (Stage 9): accrue a 20-run green
record for the `macos-gui` job (`GUI boot (macOS, WindowServer)`,
`.github/workflows/ci.yml:594-677`), drop `continue-on-error`, and register
the check in branch protection. The rig (`scripts/macos-rig.sh`) and its
self-test (`scripts/macos-rig-selftest.sh`) already exist and are unmodified
by this task — this is a pure burn-in/promotion task, not a construction
task.

## Findings, most severe first

**1. [High] AC3 asserts an exit-code contract the shipped rig no longer implements, and the one real run on record already falsifies it.**
AC3 reads: *"The exit taxonomy holds across the record: a WindowServer or TCC
capture block is exit 2 and never counted against JLS (#265 invariant 6)."*
That is stale. `scripts/macos-rig.sh`'s own top-of-file contract (lines
43-53) still documents "the control screenshot was blank/all-black (Screen
Recording permission likely withheld) ... is exit 2", but the actual
implementation below it (lines 379-401, the `DEGRADED` block) does the
opposite: a blank *control-window* capture with a *mapped* control window no
longer calls `die_env`/exit 2 — it sets `DEGRADED=1`, emits a `::warning`,
and continues to launch JLS, exiting **0** if the JLS window later maps.
`ci.yml:581-589` confirms this is not hypothetical: the rig's first-ever run
(`30322375242`) hit exactly this path — control mapped, 0 unique colors,
TCC withheld — and the job comment records that as the reason DEGRADED mode
exists, not as an exit-2 environment fault. So AC3, as written, describes a
contract that is already contradicted by the codebase and by the only
real-world data point cited in the same file. Whoever executes this task
cannot make AC3 literally true without either (a) reverting the DEGRADED
mode design #265 itself ratified, or (b) rewriting AC3. As written it is
unverifiable-as-stated.
*Recommendation:* rewrite AC3 to describe the actual three-way taxonomy
(exit 0 non-degraded / exit 0 degraded-on-TCC-capture-block / exit 1 JLS-side
/ exit 2 environment-other), and stop conflating "classified as environment"
with "exits 0 via a workaround" — they are different things (see #2).

**2. [High] AC1-3 are satisfiable by a record that never exercises the thing the issue's own outcome claims to gate on, and AC4 — the only clause that would catch this — is not a precondition on AC1-3.**
Given finding #1, and given that hosted macOS runners are well known (and
already observed once here) to withhold Screen Recording TCC from
non-interactive CI, it is very plausible that all 20 accrued runs will be
DEGRADED-mode passes: window-map only, pixel proof never exercised. AC1
("qualifying 20-run record... at most one failure") and AC2 (drop
`continue-on-error`, register the check) say nothing that prevents treating
20/20 degraded passes as "qualifying." AC4 is the clause that should stop
this — *"if the non-blank-screenshot gate is permanently unreachable under
TCC, the promotion criterion is re-planned on #265 rather than quietly
relaxed to degraded mode"* — but it reads as a fourth, independent checkbox,
not a gating precondition evaluated *before* AC1's record is allowed to
count. An implementer can tick AC1-3 mechanically (which is the easy,
visible, git-diffable part) and either skip AC4's judgment call or satisfy
it with a rubber-stamp "yes, degraded mode is the standing fallback per
#265 §7" without ever asking whether 20/20 degraded runs constitutes
"permanently unreachable." The real goal in the issue's own Outcome
paragraph — "a jar that cannot boot on macOS is caught before merge" — only
half-survives this: window-boot is proven, but the non-blank render proof
that distinguishes "mapped" from "painted garbage" never gets a chance to
fire, for the life of the check, on this population of runners.
*Recommendation:* make AC4's evaluation a precondition of AC1: the 20-run
record does not "qualify" until someone has explicitly counted how many of
the 20 were DEGRADED and stated, in the same comment, whether that rate
looks permanent — before continue-on-error is dropped, not after.

**3. [Medium] "Failure" is undefined for the 20-run count, and precedent in this repo already shows the count gets edited by hand.**
The sibling promotion this task's rule is modeled on (`gui-wayland`,
`ci.yml:341-344`) explicitly *excluded* one whole-run incident ("run
30226493722 ... a whole-master `mvn package` breakage where every
build/installer/GUI job failed at the shared 'Build jar' step ... not a lane
flake, and since fixed") from the qualifying record. #673 gives no rule for
which reds count against "at most one failure" — a `Build jar` breakage
upstream of `macos-gui`, a Homebrew/JDK/runner-image hiccup, and a genuine
JLS regression are all just "red" until someone decides otherwise. That
decision is exactly the kind of after-the-fact judgment call that can be
used to explain away an inconvenient run. Not fatal (the repo has apparently
made this call reasonably before), but it means AC1 is not self-verifying
from run status alone — a reviewer checking "did this qualify" has to trust
an unwritten exclusion policy.
*Recommendation:* state the exclusion rule explicitly (e.g., "a red run
counts against the record unless the job never reached the rig step, in
which case it is noted but excluded, same as the gui-wayland precedent") so
the 20-run record is auditable without re-litigating each red run's cause.

**4. [Medium] The task is calendar-bound, not effort-bound, and nothing in the issue says so.**
`band_mw: 0.5-1` reads as an effort estimate, but the actual bottleneck is
20 *natural* push/PR-triggered runs of one job (`macos-gui` explicitly
excludes the nightly cron: `if: github.event_name != 'schedule'`,
`ci.yml:596`) — i.e., calendar time gated by how often anyone pushes to
this repo, which the assignee does not control. Contrast with `gui-wayland`,
which *does* run on the nightly cron (`ci.yml:356-357`) and so accrues
one run/day even with zero human pushes; `macos-gui` has no such floor. If
push cadence is low, "0.5-1" of engineering-week effort could sit open for
weeks waiting on the record with almost no work to show for it, and there's
no fallback (e.g., `workflow_dispatch` to manufacture qualifying runs, or
adding `macos-gui` to the schedule trigger) offered or ruled out.
*Recommendation:* either add `macos-gui` to the cron-triggered set (mirroring
`gui-wayland`) so the record accrues independent of push cadence, or note
explicitly in the issue that this task is calendar-gated and the band_mw
estimate covers only the mechanical promotion edit, not the wait.

**5. [Low] Cross-issue bookkeeping: the record's home is #265, not #673.**
AC1 requires the run-ID record to be "recorded on #265," not on this issue.
That's consistent with the sibling task template (#667 did the same for the
headless lane) and with #265's own "Tracks durably" section, so it's
deliberate, not a slip — but it means anyone auditing #673's closure has to
go cross-reference a different issue's comment thread rather than finding
the evidence self-contained here. Worth flagging only because a reviewer
skimming #673 in isolation could wrongly conclude "no evidence, not done"
when the evidence is filed elsewhere by design.

**6. [Informational, checked and cleared] Duplicate risk against #406.**
#673's sibling task-1 (#667, same template, same "20-run promotion" shape,
same parent #265) was closed as a duplicate of #406 ("flagged near-verbatim
with #265 by the dedup pass"). I checked #406 (TASK-0017) for the same risk
against #673: #406's scope is explicitly the `macos`/`windows` **`Build`**
jobs (`Build (macOS, JDK 25)`, `Build (Windows, JDK 25)`) — coverage ratchet,
display-suite arming, HDL toolchain fail-closing — and does not mention the
`macos-gui` job or `GUI boot (macOS, WindowServer)` anywhere in its body.
So #673 is not currently a duplicate of #406. Given #265 and #406 have
already been flagged as overlapping once, it's worth a re-check at pickup
time in case #406's scope grows to subsume the GUI-boot rig before #673 is
executed.

## What's solid

- The byte-stable check name in the issue ("`GUI boot (macOS, WindowServer)`")
  matches the actual `name:` field at `ci.yml:595` exactly — no rename risk.
- The Boundary section's distinction from the `@Tag("display")` JUnit suite
  (TASK-C265-3, already closed as #669) is accurate: the rig genuinely
  launches the real jar via `java -jar`, not an in-process unit suite —
  confirmed in `scripts/macos-rig.sh`'s own header rationale.
- `ordering_after: []` with the "record accrues independently of Stage 3"
  note matches #265's own sequencing section ("Stage 9's record accrues
  independently of the headless path... convention, not necessity").
- The rig's exit-1-vs-exit-2 disambiguation strategy (control frame first,
  so a JLS-side failure is never confused with an environment block) is a
  sound, testable design, and `scripts/macos-rig-selftest.sh` exercises all
  the classification branches (11 scenarios) without needing a real runner.
