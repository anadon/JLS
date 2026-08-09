# Issue #735: TASK-C557-1: a scheduled lane re-runs the published workloads under TASK-0026's ceiling bands — ceilings only, so getting faster never turns it red
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what the issue asks for

A scheduled (non-per-PR) CI lane that re-runs the workloads whose numbers are
published in `docs/performance.md` (not yet created — owned by #555) and fails
only when a measured value regresses past a ceiling band owned by #442
(`test/fixtures/simulation-budget.properties`, also not yet created). Four
acceptance-criteria bullets, a one-line boundary note deferring "name the
stale number" to a sibling task TASK-C557-2 (unfiled/not found in this
repo's issue set as of writing).

Grounding: `docs/performance.md`, `test/fixtures/simulation-budget.properties`
and any `longrun`/scheduled-perf workflow do not exist in the tree at HEAD
(`grep -rn 'longrun' .github .` → no hits; no `timeout-minutes` anywhere in
`.github/workflows/`; `riscv/` is still present, i.e. #413's fixture
re-homing/deletion has not landed). Every one of the four `ordering_after`
issues (#442, #554, #555, #378) is open and, on inspection, unimplemented.

## Findings, most severe first

**1. [High] The "published workloads" and #442's ceiling-band fixtures are not
guaranteed to be the same set, yet AC-2 forbids building a second mechanism.**
#442 (TASK-0026) scopes `simulation-budget.properties` explicitly: "populate
it from #377's and #379's measurements, including the CPU-scale fixture #413
tracks. At least two rows." Its only named fixture is `riscv-sum1to10.jls`
plus the CPU-scale replacement from #413. Meanwhile #554 (FEAT-C28-1) AC-3
*requires* "at least two fixtures below CPU scale (standard small circuits,
e.g. a counter and a memory loop)" as part of the *published* workload set
that #735 is supposed to gate. Nothing commits #442 to carrying ceiling rows
for a counter or memory-loop circuit — its own issue never mentions them.
If those rows don't exist when #735 is executed, the acceptance criteria
collide: AC-2 says "no parallel band mechanism is introduced," but someone
has to either (a) extend #442's schema/file with new rows for #554's small
fixtures — arguably fine, but #735 never says who does this or that it's in
scope — or (b) build a second gate for the uncovered fixtures, which AC-2
explicitly forbids. The issue gives an executor no way to resolve this.
**Recommendation:** state explicitly whether #735 is responsible for adding
rows to `simulation-budget.properties` for every #554 fixture, and add a
precondition ("every published-number fixture already has a
`simulation-budget.properties` row; if not, file a blocking task against
#442") rather than assuming reuse is automatic.

**2. [Medium] Unit/direction of the "ceiling" is never stated relative to the
published metric.** #442's bands are cost metrics — ns/event and
bytes/event ceilings (lower is better, so "ceiling only" is the natural
regression-safe direction). #554/#555 publish *rate* metrics — events/s and
cycles/s (higher is better; a regression is a *drop*, which for a rate is
naturally expressed as a floor, not a ceiling). The issue's language
("ceiling bands," "ceilings only, so getting faster never turns it red")
is copied verbatim from #442/#557 without saying whether the lane checks the
published rate number directly (which would need a floor, not a ceiling) or
inverts it against #442's cost-side band (which is a ceiling). This is
solvable — the two are reciprocals — but it is a real ambiguity left for
the implementer to discover, in an issue whose entire point is measurement
precision. **Recommendation:** add one sentence stating which side of the
measurement (cost-per-event vs. rate) the assertion actually runs against.

**3. [Medium] No anti-vacuity requirement — the four bullets are satisfiable
by a lane that has never been proven able to fail.** Compare directly with
sibling tasks by the same author: #442's Definition of Done requires "Three
deliberate red runs recorded in the PR: a declared event count changed by 1;
the properties file emptied; a stub `*GoldenTest` added unenrolled." #378
requires P4/P5 be "seen to fail" with pasted failure output before merge,
citing its own stated principle: "A cap that has never been seen to fail
should be assumed vacuous" (`CONTRIBUTING.md:102-105`, quoted in #378). #735
has no analogous requirement. As written, "fails when a measured value
regresses beyond its declared ceiling band" (bullet 1) can be checked off by
a workflow file that contains the right `if` condition but was never
actually exercised against a real regression — exactly the failure mode
#378/#442 go out of their way to guard against. **Recommendation:** add a
completion criterion requiring one deliberately-induced red run (e.g.
temporarily lower a declared ceiling below a measured value) with the
failure output pasted into the PR, mirroring #442/#378's own convention.

**4. [Medium] Rigor is inconsistent with sibling tasks in the same
tracker/author/date range, which weakens confidence the AC bullets were
derived from evidence rather than templated.** #442 and #378 (both
`tier:task`, filed by the same author within a day of #735) each carry
Observations with pinned `git grep`/line-number evidence of the current
red state, explicit Hypotheses, Predictions, a 12-part Interface & Data
Contract, Falsification Criteria, Threats to Validity, and a ~20-item
Definition of Done checklist with a mermaid dependency graph. #735 is four
checkbox bullets and a boundary note — no evidence command showing today's
tree lacks a scheduled perf lane, no named test/workflow file, no cron
cadence, no artifact/report format, no completion checklist. This is not
merely stylistic: the missing sections are exactly where #442/#378 encode
the guardrails (anti-vacuity, exact reuse of existing mechanisms, explicit
red-run proof) that finding #3 shows #735 is missing in practice.
**Recommendation:** either justify the lighter template for a task-tier
issue, or bring #735 up to the same rigor its stated dependencies use,
since its correctness depends on details (fixture coverage, unit direction)
that only that rigor would have forced into the open.

**5. [Low-Medium] The dependency chain is declared with the weaker
`ordering_after` field, not `blocked_by`, and none of it has landed.** #442
and #378 both use an explicit `blocked_by:` YAML key with a completion-item
"every blocked_by entry has landed, or the dependency was waived." #735's
header only has `ordering_after: [442, 554, 555, 378]` — softer language,
no corresponding "must have landed" completion item. Verified against the
repo: `docs/performance.md` does not exist (#555 unlanded), `riscv/` is
still present and un-deleted and no CPU-scale fixture has been re-homed
(#413, itself a prerequisite of #554, unlanded), `simulation-budget.properties`
does not exist (#442 unlanded), and no `longrun`/scheduled-lane workflow or
`timeout-minutes` exists anywhere (#378 unlanded). Nothing in #735 itself
stops an executor from attempting it before any of these four exist, which
would fail immediately and for uninformative reasons (missing files) rather
than the reasons the issue's own acceptance criteria describe.
**Recommendation:** promote `ordering_after` to `blocked_by` here, consistent
with #442/#378's convention, and add the "must have landed" completion item.

**6. [Low] Citation mismatch: bullet 3 attributes a requirement to the wrong
acceptance criterion.** Bullet 3 — "A measured value better than its band
never fails the lane (CAP-28 AC-4, ceilings only)" — cites CAP-28 (#512)
AC-4. But #512's actual AC-4 text is "A regression beyond the band turns a
scheduled lane red before the published number is a lie," which says
nothing about ceilings; that is bullet 1's claim, and bullet 1 correctly
cites CAP-28 AC-4 for it. The "ceilings only" property is stated by the
parent feature #557 (FEAT-C28-3) in *its own* AC-4 ("Ceilings only — an
engine that gets faster never turns the lane red"), not by CAP-28. Minor,
but in an issue whose subject is measurement/citation integrity, a
traceability error like this is worth fixing. **Recommendation:** cite
bullet 3 against #557 AC-4, not CAP-28 AC-4.

**7. [Low] Scope ambiguity on redundant re-running of already-fast-lane
fixtures.** AC-1's phrasing — "runs the published-number workloads,
including any CPU-scale fixture too long for the fast lane" — implies by
contrast that the non-CPU-scale published fixtures already run somewhere in
the required per-PR lane. If so, this scheduled lane re-runs them again on a
cadence, adding CI cost the issue never justifies (is the goal to catch
non-code drift — JDK/runner changes — that a per-PR run wouldn't catch on a
stable PR? Plausible, but unstated). **Recommendation:** one sentence on why
small, already-fast-lane-covered fixtures are re-run here too, or narrow
scope explicitly to the CPU-scale fixture.

## What's solid

- The ceilings-only framing itself (never fail on getting faster) is
  well-motivated by #442's own measured 4.2x same-JVM timing spread — a
  two-sided band would be a flake factory, and the issue correctly avoids
  one.
- Deferring band *mechanics* entirely to #442 rather than reimplementing them
  is the right DRY call and matches the capstone's explicit "do not build it
  twice" instruction.
- Explicitly carving "name the stale published number on failure" out to a
  sibling task (TASK-C557-2) keeps this task's diff small and reviewable —
  a reasonable decomposition, provided that sibling task is tracked and
  actually lands (not verified here; it was not found via search).

## Verdict rationale

Sound in overall direction (consume, don't duplicate, ceiling-only), but
needs rework before an executor could implement it without inventing
answers the issue should supply: which fixtures are actually covered by
#442's bands (finding 1), which side of the measurement the ceiling check
runs against (finding 2), and a concrete non-vacuity proof requirement
(finding 3) consistent with the rigor its own sibling tasks impose on
themselves (finding 4). None of these sink the goal, but as written the
four acceptance-criteria bullets can be satisfied in a way that leaves the
regression gate hollow.
