# Issue #684: TASK-C529-1: clicking a waveform edge flashes the element that emitted it, scrolling the canvas to it if it is off-screen
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context checked

Repo at `/home/user/JLS`: `README.md`, `ARCHITECTURE.md`,
`src/jls/sim/SimEvent.java`, `src/jls/elem/WireNet.java`,
`src/jls/elem/Element.java`, `src/jls/edit/Trace.java`,
`src/jls/edit/InteractiveSimulator.java`,
`docs/capability-roadmap/lf-03-causal-debug.md`, and this fleet's
existing reviews of the surrounding chain: #529 (FEAT-C23-2, the parent
feature this task decomposes), #685 (TASK-C529-2, the sibling task),
#680 (TASK-C527-2, the direct `ordering_after` predecessor), #678
(TASK-C527-1, the tap seam), and #527/#504 (the owning feature/capstone).
No `ChronogramClosedCostTest` file exists anywhere in `test/` at HEAD
(`find test -iname "*Chronogram*"` returns nothing).

## Findings, most severe first

**1. (HIGH) AC-2/AC-3's premise — that "the emitting element" is always resolvable through the event stream — is false for the exact scenario the Outcome paragraph is built around, verified directly against the propagation code.**
`SimEvent.PinChanged` (`src/jls/sim/SimEvent.java:30-31`) is a zero-field
record. `WireNet.propagate` (`src/jls/elem/WireNet.java:443-486`)
computes the winning driver during tri-state resolution and then
discards it: the loop at lines 460-467 finds `actual` (the value of the
driving `Output`) but never retains *which* `Output`/element produced
it, and `value = actual;` at line 484 is the last place that driver
identity could have been kept. The event actually posted downstream
(`inp.setValue(newValue); sim.post(new SimEvent(now, (Reacts) element,
new SimEvent.PinChanged()));`, WireNet.java ~502-508) carries the
*receiving* element, not the driver, and `PinChanged` itself carries no
producer reference at all. For any net with more than one potential
driver — and JLS's tri-state elements exist and are actively simulated
here (`triState` field, conflict detection at WireNet.java:454-483) —
there is no "element that emitted it" recoverable from the event/seam
AC-2 names. This is not a corner case the feature can shrug off: it is
precisely the "student diagnosing a bad edge" scenario the Outcome
paragraph invokes, and it directly undermines AC-3 too (you cannot
report "the emitting element no longer exists by name" for an edge
whose emitting element was never identifiable in the first place). The
same fact was independently found against the parent feature #529
(finding 3) and sibling #685 (finding 3) in this fleet's reviews; #684
is the ticket that actually has to build the mechanism and inherits the
contradiction unresolved.
**Recommendation:** scope AC-2/AC-3 explicitly to elements with a single
static driver, deferring multi-driver/tri-state identity to #532
(TASK-C532-1, "the producer-to-consumer relation the scheduler discards
is retained... at zero cost when nothing is watching," which is
designed to carry this exact provenance) and say so in the issue; or
add the minimal driver-identity retention this needs and account for
its cost against AC-4's "no additional per-event cost" claim.

**2. (HIGH) AC-4 treats `ChronogramClosedCostTest` as an already-scoped, checkable gate, but the test does not exist and its ownership/definition is contested three ways upstream — #684 becomes a fourth, uncoordinated claimant.**
AC-4: "No per-event cost accrues when the chronogram is closed —
`ChronogramClosedCostTest`'s tolerance is unmoved with this task
present." `find /home/user/JLS/test -iname "*Chronogram*"` returns
nothing — the test is unwritten. This fleet's review of #678
(TASK-C527-1, finding 1) already documents that #678, #527, and #504
each list the identically-named `ChronogramClosedCostTest` as something
*they* deliver, with no stated owner, and #527's own review (finding 3)
notes its cited baseline fixture ("the first-year adder flow") doesn't
exist anywhere in the tree and no tolerance number or measurement
method is pinned down. #684 adds itself as a fourth issue whose
acceptance rests on this same unresolved test without declaring which
upstream definition it inherits, or supplying its own baseline/
tolerance. As written, "unmoved" cannot be checked because nothing
authoritative exists yet to check it against.
**Recommendation:** either point AC-4 at a concrete, already-authored
version of the test once #678/#527/#504's ownership question is
resolved, or restate AC-4 with #684's own concrete before/after
measurement plan (fixture, tolerance number, units) independent of a
test #684 cannot guarantee exists when this task lands.

**3. (MEDIUM-HIGH) AC-1's "bounded interval" flash has no duration, revert trigger, or interaction rule with existing selection state, and the only highlight primitive in the codebase is not time-bounded.**
`Element.highlight` (`src/jls/elem/Element.java:41-42`) is a plain
persistent `boolean` toggled by `setHighlight`/`isHighlighted`
(:565-588) — no timer, no expiry, and no distinction from "this element
is currently selected." A literal implementation can call
`setHighlight(true)` once and never revert it, satisfying "visibly
highlights... for a bounded interval" under a loose reading (the
session's remaining lifetime is technically bounded), while being
visually indistinguishable from ordinary selection and colliding with
whatever the user has actually selected. Nothing in AC-1 states a
duration or what happens when a second edge-click lands before the
first flash would have ended.
**Recommendation:** specify the flash as a distinct, timed visual state
(a duration in ms, or an explicit revert trigger such as "next click or
selection change") and state its relationship to ordinary
selection-highlighting, closing the identical gap already flagged one
tier up on parent #529 (that review's finding 4).

**4. (MEDIUM) AC-2's stated test ("rename the signal, assert the link still resolves") is close to tautological given how identity is already stored in this codebase, and doesn't exercise the case that's actually hard.**
`Trace` already stores a direct Java object reference
(`private Element element;`, `src/jls/edit/Trace.java:57`), not a name
string. As long as the new resolution path is built the obvious way
(object reference, not name lookup), a rename test is close to
guaranteed to pass regardless of implementation quality — it never
touches the multi-driver case (finding 1) or the cross-subcircuit case
(finding 5), which are where identity resolution is genuinely difficult.
**Recommendation:** keep the rename test as a cheap sanity check but do
not let it stand in as the sole identity-resolution acceptance gate;
pair AC-2 with a test that actually exercises a multi-driver net (once
finding 1 is resolved) as the discriminating criterion.

**5. (MEDIUM) Cross-subcircuit navigation is unaddressed, though this is the task where it has to be decided.**
"Scrolling the canvas to it if it is off-screen" (title, AC-1) presumes
the emitting element lives on the currently open canvas tab. JLS's
circuits are hierarchical (`src/jls/elem/SubCircuit.java`). The same gap
was already flagged at the parent-feature level (#529 review, finding
5) but is not closed here, at the concrete implementation ticket where
"off-screen" has to mean something specific: does the editor auto-descend
into an unopened subcircuit instance, open a new tab, or silently no-op?
**Recommendation:** add an explicit note (or an explicit out-of-scope
statement with a follow-up issue) for the subcircuit case before
implementation starts.

**6. (LOW) AC-2's prose names a different predecessor than the machine-readable `ordering_after` block, though the chain resolves transitively.**
AC-2 says identity resolution goes "through TASK-C527-1's seam," but the
YAML frontmatter's `ordering_after` lists only `[TASK-C527-2]` (#680),
not TASK-C527-1 (#678) directly. This does resolve: #680's own
`ordering_after` correctly chains back to #678 (confirmed via this
fleet's #680 and #685 reviews), so #684 does transitively reach the seam
AC-2 names. Not a blocking defect, but worth a one-line fix so a reader
scanning only the machine block doesn't have to hop two more issues to
find the thing AC-2's prose is actually about.
**Recommendation:** either add `TASK-C527-1` to `ordering_after`
alongside `TASK-C527-2`, or note in the Outcome that the dependency is
transitive through #680.

## What's solid (no rework needed)

- The core design target — resolving identity through element/event
  identity rather than mutable display names — is the right property to
  want, and is consistent with this repo's general practice of keying
  behavior off stable identity rather than strings elsewhere (e.g.
  `SaveTags.resolve` routing element types through canonical tags, not
  raw class-name strings, per ARCHITECTURE.md's save/load section).
- AC-3's "deleted between capture and click" scenario is actually
  reachable in a real session, not hypothetical: `InteractiveSimulator
  .beforeEvent` re-enables the editor while the simulation is paused
  (`src/jls/edit/InteractiveSimulator.java:763-764`,
  `ed.enableEditor(true)`), and trace/event data is not cleared on
  pause (only on a fresh run start, `InteractiveSimulator.java:612-615`)
  — so a user genuinely can pause, delete an element, and later click a
  stale edge referencing it.
- Scroll-into-view has existing precedent in the editor for the
  same-canvas case (`JViewport` ancestor lookups used elsewhere in
  `SimpleEditor.java`/`InteractiveSimulator.java`, per the #529 review's
  "what's solid" section), so AC-1's basic mechanism is feasible modulo
  finding 5's subcircuit gap.
- `band_mw: 1-1.5` sums cleanly with sibling #685's `1-1.5` to the
  parent #529's stated `2-3` band — no hidden cost drift at the task
  level.

## Verdict rationale

The task correctly narrows one direction of #529's cross-probing into a
self-contained ticket, and the mechanical pieces it needs (highlight
primitive, scroll-into-view, a stored element reference on `Trace`)
already exist in some form. But its two central claims — that "the
emitting element" is always resolvable (AC-2/AC-3, finding 1) and that
`ChronogramClosedCostTest` is a stable, checkable gate (AC-4, finding
2) — are each contradicted or unsupported by the current codebase and
by this task's own upstream dependency chain, and neither is a
cosmetic wording problem: an implementer following #684 literally can
ship code that passes AC-2's named test while failing on the multi-driver
nets the feature is meant to help diagnose. **needs-rework.**
