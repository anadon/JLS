# Issue #682: TASK-C527-3: two cursors measure a time delta in ticks, and in physical units the moment a circuit declares what a tick means
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Add a draggable, snap-to-edge cursor pair to the (not-yet-built) chronogram
panel, reporting a tick delta always and a physical-unit delta when a circuit
declares one via FEAT-047. `band_mw: 1`, `ordering_after: [TASK-C527-2]`
(#680, the chronogram panel itself, also open/unbuilt).

## Findings, most severe first

**1. The issue's own body cites a closed, duplicate-superseded dependency, and the real dependency edge exists only in a comment, not in the machine-readable frontmatter.**
The body text says the physical-unit half applies "When the circuit declares
a physical time unit (FEAT-047 #367, TASK-0101 #431)". #431 (TASK-0101) is
**closed, `state_reason: duplicate`**, closed 2026-08-08T16:47:12Z — before
this issue's own last update. Its substance was re-filed as **#882
(TASK-C367-1)**, which explicitly declares `blocks: [682]` and states "The
`blocked_by: [882]` edge is recorded on both ends." That claim is false as
written: #682's own YAML frontmatter is
```yaml
task_id: TASK-C527-3
part_of_feature: 527
band_mw: 1
ordering_after: [TASK-C527-2]
```
— no `blocked_by` field at all, physical-unit or otherwise. The only place
the #882 dependency is recorded on this issue is a same-day comment
(2026-08-08T18:36:13Z), not the issue body or a structured field. An
executor or scheduling tool that reads only the frontmatter (which is the
issue's stated machine-parseable contract, per the pattern #367/#882
themselves rely on) will see zero blockers and a dead link to #431.
**Recommendation:** edit the issue body to replace the #431 citation with
#882, and add `blocked_by: [882]` (for the physical-unit half only — the
comment already proposes a `WAIVED:` disposition for a tick-only landing)
to the frontmatter itself, not just a comment.

**2. `band_mw: 1` is not credible against the work actually described, and against the project's own pricing of the adjacent conversion logic.**
TASK-0101/#882's *pure logic* half (a `TimeBase` record, parse, conversion,
version bump, docs) alone was banded at "1.5 wk" and the parent feature at
"2-3 mw" specifically because the row sum did not cover the full sweep. This
issue asks for, on `band_mw: 1`: two independently draggable cursors, edge
snapping against arbitrary numbers of grouped/reordered signals (per #680),
a continuously-updating delta readout, a conditional secondary readout keyed
to an external optional feature, a fixture-driven "known transition times"
test, and a negative test proving no fabricated unit appears when none is
declared — layered on top of a chronogram panel (#680) that does not exist
in the tree yet either. No file, class, or existing test in
`src/jls/edit/` or `test/jls/edit/` implements multi-cursor or snap
behavior today (`Trace.java` has one `sliderPos` int, not a pair, and no
snapping). **Recommendation:** re-derive the band against #680's actual
shape once #680 lands, or explicitly split "cursor placement + tick delta"
from "physical-unit rendering" as separate bands, since the comment itself
already treats them as separable work.

**3. "Snappable to transition edges" is underspecified for a multi-signal chronogram.**
AC-1: "draggable and snappable to transition edges" — but #680 (this
issue's own prerequisite) promises "grouped signals" (plural) on the same
timeline. Snap targets are not defined: does a cursor snap to the nearest
edge on *any* visible signal, only a "selected"/probed one, or the union of
all signals' edges? Two signals can transition at different times a pixel
apart; the criterion gives no tie-break or scope rule. As written, an
implementation that snaps only to the first-added trace's edges (ignoring
every other grouped signal) satisfies AC-1's literal text and would still
plausibly be judged wrong by any real user. **Recommendation:** name the
snap universe explicitly (e.g., "any edge on any signal currently displayed
in the panel, nearest-wins, ties broken toward the earlier time").

**4. AC-1's "delta between them displayed continuously" is not tied to any test, unlike every other criterion.**
AC-2 through AC-5 each name or imply a concrete assertion (fixture,
recompute-from-tick, absence check, no-file-diff check). AC-1's "displayed
continuously" — presumably meaning live-updating during a drag, not merely
correct after mouse-up — has no corresponding verification method named
anywhere in the issue. A GUI continuously-updating label during a drag is
exactly the kind of behavior that is easy to implement as "compute on
mouseReleased" and still pass every *other* criterion, while failing the
actual UX goal silently. The project's test suite is headless by policy
(`ARCHITECTURE.md`/README: `mvn verify` runs headless, no X server) and
there is no established pattern in this repo for driving synthetic
mouse-drag sequences against a `JPanel` (no `Robot`, no AssertJ-Swing
dependency found in `pom.xml`/`src`). **Recommendation:** either specify
that "continuously" is verified by decoupling the drag-delta computation
from paint (e.g., a testable method called on every drag-motion event, unit
tested without a real mouse), or drop "continuously" to "updated after each
placement," which is what a headless test can actually check.

**5. Criterion 3's "consumed if present, never required" is a real, worthwhile constraint but is not independently testable within this issue as scoped.**
Given finding #1, if this task lands before #882, "when #367/#431's declared
time unit is present" is presently *unreachable* — there is no way today to
construct a circuit with a declared time unit to drive that branch of the
test. The issue does not say what "asserted" means for AC-3 if #882 hasn't
landed: is the physical-unit rendering path shipped untested (contradicting
the project's own "predictions are asserted, not claimed" convention seen
in #367/#882), or is it deferred with a `WAIVED:` comment as #882's own note
suggests? The issue itself does not choose. **Recommendation:** state the
disposition explicitly in the issue (the comment already suggests the
right answer — waive AC-3 explicitly to #882 — but the acceptance criteria
section itself should say so, not just a comment).

**6. Scope-boundary ambiguity with the sibling TASK-C535-1 (#702, "Dragging the chronogram cursor...").**
#702 is `ordering_after: [TASK-C527-3]` and refers to "the chronogram
cursor" (singular) for rewind-to-state-at-T. #682 introduces *two* cursors.
Neither issue says which of the two cursors (or whether either) drives
rewind, nor whether rewind-drag and delta-measurement-drag are the same
gesture on the same object. This is a minor but real interface gap between
two issues meant to compose. **Recommendation:** #682 or #702 should name
which cursor (primary/left, or a new single "playhead" distinct from the
two measurement cursors) rewind acts on.

## What's solid

- AC-4 (no unit fabricated when none is declared) and AC-5 (cursor state is
  view-only, not persisted, no save-format touch) are both crisp, testable,
  and correctly scoped — they don't leak into #882/#367's territory.
- The ordering choice (`ordering_after: [TASK-C527-2]` rather than a hard
  `blocked_by`) is consistent with how the rest of this issue family (#367,
  #431, #882) distinguishes composition/sequencing from blocking, and #680
  is a sensible prerequisite for a panel that doesn't exist yet.
- AC-2 (tick delta always available, fixture-verified) is unambiguous and
  cheaply testable independent of #367/#882 entirely, matching the
  comment's own read that "the tick half is unblocked and complete on its
  own."
