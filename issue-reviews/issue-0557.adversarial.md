# Issue #557: FEAT-C28-3: a published performance number cannot silently go stale — the scheduled lane re-runs the suite under TASK-0026's ceiling bands and turns red before the doc lies
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched #557 (open, 0 comments) plus its full dependency chain: #442
(TASK-0026, open), #554 (FEAT-C28-1, open), #555 (FEAT-C28-2, open), #512
(CAP-28, open), #378 (TASK-0016, open). Confirmed against the tree at HEAD:
no `test/fixtures/simulation-budget.properties`, no `docs/performance.md`,
no `.github/workflows/longrun.yml`, no `longrun` JUnit tag anywhere
(`grep -rn longrun .github test` empty except in `docs/plan`), and `riscv/`
still holds the raw `bench_kernel.py` harness FEAT-C28-1 is meant to
promote. So every one of #557's stated prerequisites is genuinely
unimplemented today, matching what its own `ordering_after` block claims —
that part is honest.

## Findings, most severe first

**1. [High] AC-1's regression gate is inherited wholesale from TASK-0026 and nothing here keeps it honest against the *published* number specifically — the exact failure mode the issue's title claims to prevent.**

TASK-0026 (#442) is explicit that its own ceiling bands are allowed to be
demoted the first time they cause a false failure on an unrelated PR
("H2 refuted... demote the timing assertion to a reported number... Do not
widen the band repeatedly") and that a band that never fails "should be
assumed vacuous." #557 AC-2 says the bands are "TASK-0026's — one
declared-numbers mechanism, consumed here" with no additional constraint
tying the ceiling's tightness to the number actually printed in
`docs/performance.md`. A ceiling that is loose enough to survive CI noise
(which TASK-0026 itself measured at a 4.2x in-JVM spread) can sit well
above the published number while the measured value drifts steadily worse
underneath it — the lane stays green, `docs/performance.md` is stale, and
AC-1 ("fails when a measured value regresses beyond its declared ceiling
band") is satisfied by the letter while the outcome ("a published number
cannot silently go stale") fails. Recommendation: add an explicit
tolerance/co-location check — e.g. the ceiling used by the scheduled lane
must be within a stated percentage of the number quoted in
`docs/performance.md`, checked by a test that reads both — not just "some
ceiling exists and is TASK-0026's."

**2. [High] AC-3 ("a lane failure identifies which published number in `docs/performance.md` is stale") has no defined mapping mechanism anywhere in the cited chain, so it is untestable as written.**

Neither #442's `simulation-budget.properties` schema (fixture path,
clocking regime, event count, ns/event ceiling, bytes/event ceiling) nor
#555's AC for `docs/performance.md` (prose with hardware/JDK/flags/node
counts) commits to a machine-checkable identifier that ties a fixture row
to a specific sentence or number in the doc. A failing test whose message
merely names the fixture file (which is all TASK-0026 promises — "declared,
actual, delta and the properties file's path") would satisfy AC-3's letter
by pointing at `simulation-budget.properties`, without ever naming *which
line of docs/performance.md* is now false — which is what AC-3 actually
asks for. As written, an implementer could close this criterion by writing
a comment saying "see docs/performance.md" and nothing would catch the gap
in review, since no existing test format enforces the linkage.
Recommendation: define an explicit ID (e.g. a fixture-name convention or
anchor) that is required in both the properties file and the doc, and add
a cross-reference test analogous to TASK-0026's own anti-vacuity checks.

**3. [Medium-High] Unit/fixture mismatch between what TASK-0026 measures and what FEAT-C28-1/2 publish — AC-2's "consumed here, not a parallel implementation" may not be achievable without new translation work that is itself scope creep.**

TASK-0026's bands are **ns/event** and **bytes/event** ceilings, populated
(per its own Method section) "from #377's and #379's measurements,
including the CPU-scale fixture #413 tracks" — nothing in #442 commits to
covering sub-CPU-scale circuits. #554 (FEAT-C28-1) AC-3 separately requires
"at least two fixtures below CPU scale (standard small circuits)" and
publishes **events/s and cycles/s at stated node counts** — a throughput
figure, not TASK-0026's per-event cost figure, and over a fixture
population TASK-0026 was never scoped to include. #554's own boundary note
underlines the separation: "#442 TASK-0026 owns the event-count equality,
ceiling bands... this suite reports numbers; it does not build a second
gate." If TASK-0026's properties file ends up covering only the CPU-scale
fixture (its stated scope) while FEAT-C28-1 publishes numbers for smaller
circuits too, #557's AC-1 ("runs the published-number workloads and fails
when a measured value regresses beyond its declared ceiling band") has no
band for most of what it's supposed to guard — and building one would be
exactly the "parallel implementation" AC-2 forbids. Recommendation: before
work starts, confirm (or make an explicit decision recorded on this issue)
that TASK-0026's fixture set is, or will become, a superset of FEAT-C28-1's
published fixtures; otherwise split ownership explicitly rather than
asserting reuse that may not exist.

**4. [Medium] A real dependency on #378 (TASK-0016) is understated to a "boundary note" instead of a hard `ordering_after` entry, and #378 is itself unimplemented.**

The Outcome section states the lane "covers the published workloads —
including any CPU-scale fixture too long for the fast lane." Which CI lane
may host a long-running workload, and the very mechanism for tagging one
(`longrun`, a scheduled `longrun.yml`), is #378's explicit, exclusive
ownership: "#378 (TASK-0016) — owns which CI lane hosts a long run... this
task must not decide that." #378 is open and, per the tree, entirely
unbuilt — no `longrun` tag, no fixture-size ratchet, no `longrun.yml`.
#557's machine block lists #378 only under `Boundary / reference notes`,
not `ordering_after`. If #557 is picked up before #378 lands, the
CPU-scale-fixture clause in its own Outcome is either unfulfillable or
forces #557 to improvise the lane-hosting decision #378 explicitly reserves
for itself — a boundary violation the issue is trying to avoid elsewhere.
Recommendation: add #378 to `ordering_after`, or explicitly scope the
CPU-scale-fixture clause as conditional on #378 having landed by the time
#557 executes.

**5. [Medium] Feasibility/estimate risk: three of #557's prerequisites (#442, #554, #555) are simultaneously open and unimplemented, and findings 1–3 above suggest the integration surface between them is larger than "wire an existing gate to a cron."**

`band_mw: "1-2"` reads as sized for "point a scheduler at code that already
exists," but nothing it depends on exists yet, and at least one of those
dependencies (#442) may not produce output in the shape #557 needs (finding
3). This isn't disqualifying on its own — estimates commonly firm up once
blockers land — but it is worth flagging now rather than after #442/#554/
#555 close with a shape #557 can't consume cleanly.

## What's solid

- **AC-4 ("ceilings only — an engine that gets faster never turns the lane
  red")** is well-specified, testable by reading the diff, and correctly
  mirrors TASK-0026's own one-sided design. No concerns.
- **Scope discipline is otherwise good**: the boundary notes correctly
  defer ratchet mechanics to #442, CI-lane-budget policy to #378, and the
  internal-claims ratchet to #335/#476/#475, each with a one-line
  "reference, do not duplicate" — this is the right shape for avoiding
  scope creep even though finding 3 shows the "do not duplicate" premise
  needs verifying rather than assuming.
- **The `ordering_after` list correctly sequences #442, #554 and #555**
  ahead of this feature, and the tree confirms none of the three has landed
  — the issue is not lying about its own prerequisites.

## Bottom line

The issue is well-boundaried in prose but its acceptance criteria don't
yet close the loop between "a ceiling exists somewhere" and "the specific
published claim in `docs/performance.md` is defended" — AC-1 is gameable
by a loose or demoted band, and AC-3 has no mapping mechanism to satisfy it
against. Combined with the unit/fixture mismatch between TASK-0026's
internal ceilings and FEAT-C28's public throughput numbers, and the
understated dependency on #378's still-nonexistent long-run lane, this
needs another pass on the acceptance criteria before it's ready to
implement.
