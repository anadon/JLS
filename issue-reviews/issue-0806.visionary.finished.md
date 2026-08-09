# Issue #806: TASK-C593-3: a wire segment selects as the thing under the cursor, not the thing nearest to it — and bsiever-fork #18 gets its disposition
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Underneath the wire-specific framing, #806 wants one thing: **the editor should
manipulate the object the user can see under the pointer.** That is the right
goal and it serves the project's arc — but the issue names the wrong defect, cuts
the wrong seam, and stores the resulting contract in the wrong place. All three
are worth changing before a line is written.

## The stated defect does not exist; the real one is worse

The issue says selection resolves to "whichever segment the hit test happened to
find nearest." There is no nearest computation anywhere in the picking path. The
actual code (`src/jls/edit/SimpleEditor.java:2617`, and again at `:2672`, `:3241`,
`:3347`, `:3764`) is:

```java
for (Element el : circuit.elementsAt(x,y)) {
    if (el.contains(x,y)) { ... }
```

`Circuit.elementsAt` (`src/jls/Circuit.java:588`) delegates to
`SpatialIndex.query`, which returns `new HashSet<Element>()`
(`src/jls/SpatialIndex.java:191`). `Element` overrides neither `equals` nor
`hashCode`, so iteration order is identity-hash order. The picked element among
several overlapping candidates is therefore **arbitrary and JVM-run-dependent**,
not nearest, not topmost, not first-drawn. "Nearest" would at least be a rule.

Two consequences the issue never considers, both larger than the one it states:

1. **Picking contradicts painting.** `CircuitRenderer.java:262` draws via
   `circuit.getElementsInStableOrder()` — a deterministic z-order that already
   exists and is already test-pinned (#182, `PrintPageOrderTest`,
   `SimulationSeedOrderTest`). So the project already knows what is on top. The
   pick path simply doesn't ask.
2. **Hover contradicts click.** The idle-motion path at `:3760-3792` builds a set
   `under` of *every* element containing the point, highlights all of them, and
   puts all of them in `selected`. A click at the identical pixel then takes
   exactly one, arbitrarily. The editor visibly promises three things and
   delivers one unpredictable one. No tolerance or tie-break tuning fixes that;
   only a shared rule does.

This matters for scope: the bug is not wire-shaped. It is the same defect for
gate-over-wire, wire-end-over-wire, and text-over-anything. Fixing it "for wire
segments" leaves the class alive and re-opens it under a different issue number
later.

## Reframing 1 — the missing abstraction is a Pick function, and it is one line of spec

Add a pure, AWT-free `Pick` (natural home: `jls.core` alongside
`SegmentGeometry`, or a new `jls.edit.pick`) with a total function

```
pick(Circuit, x, y) -> List<Element>   // deterministic, documented order
```

and define its order by the invariant the codebase has already earned:

> **The pick list is the reverse of the paint list, restricted to elements whose
> `contains(x,y)` is true.**

Nothing about tolerance, tie-break, or z-order needs inventing. Tolerance is
already `Wire.contains`'s half-spacing band (`Wire.java:244`, `SPACING/2 = 6px`
with a `POINT_DIAMETER` exclusion near ends); z-order is already stable-id order;
the tie-break falls out of reversing a list. "The thing under the cursor" becomes
"the thing you can see" *by construction* rather than by calibration.

This is exactly the shape of assertion JLS already likes: it is a parity theorem
between two orderings, the sibling of `DrawCullingParityTest#culledCandidatesMatchFullScan`
and of THEOREM 1 in `proofs/SpatialIndexCorrectness.agda`. One headless test —
"the head of `pick` is the last-painted element containing the point" — pins the
entire behaviour, on a `Circuit` with no display, no `MouseMachine`, no Swing.

## Reframing 2 — ship the list, not just the head, and the tolerance argument dies

Because hover already highlights every candidate, the project is one small step
from something better than a correct single pick: **repeat-click (or Alt-click)
cycles down the pick list**, the Inkscape/CAD idiom. Two wires 12px apart, a wire
crossing a gate body, a wire end on a segment — all become one keystroke to
disambiguate instead of a tolerance the maintainer has to defend forever. And
because the artifact is a `List<Element>`, cycling is testable headlessly as
list-index arithmetic; it needs no pixels and no display. This is the
out-of-the-box move the issue never entertains, and it is cheaper than the
tolerance tuning AC-1 implies.

## Reframing 3 — separate eligibility from picking, and refusals become sayable

At `:2626-2630` the left-button path does `if (end1.isAttached()) continue;`
*inside the pick loop*. Picking and gesture-eligibility are fused: when a gesture
cannot use the top candidate it silently reaches down to a lower one. That is
the invisible half of the complaint in the title, and it is architecturally the
more damaging half.

Split it: `pick` answers "what is here, topmost first"; each gesture applies a
**named** predicate over that list. A gesture that cannot act on the top hit then
*says so* — "this wire's ends are both attached; drag an end, not the segment" —
which is the project's own settled idiom (`LoadError`'s taxonomy + actionable
hint, `TellUser` as the sole message channel, and #593's own "refuses by name").
Right now the editor refuses by silently doing something else.

## Reframing 4 — "segment" is a red herring; AC-2 is an op-vocabulary task

In JLS's model a `Wire` **is** a segment (two `WireEnd`s); a polyline is a
`WireNet` of them. So AC-2's move/split/delete "as the segment the user clicked
on, preserving the rest of the wire's connectivity" is not geometry work at all —
it is command work, and `jls.collab.op` already carries `RemoveWire`, `AddWire`,
`MoveElements`, with `OpSink.submitAll` giving one undo snapshot per gesture
(`docs/operation-layer.md`). Split = `RemoveWire` + new `WireEnd` + two `AddWire`s
in one `submitAll`. Landing it there rather than in the editor buys undo,
validated-or-byte-identical rejection, collab replication (#163), and headless
assertability *for free* — and it satisfies KC-37-1 immediately, because
`jls.collab.op` is already outside `SimpleEditor` and already ArchUnit-fenced
(`ArchitectureRulesTest.collabLayersAreHeadless`). `docs/operation-layer.md`'s own
"what lands next" list is missing a `SplitWire`-class op; this issue is the
occasion to add it.

## Where the issue pulls against the project's arc

**AC-1 puts a live invariant in a dead document.** JLS's settled pattern is that
normative behaviour lives in a *spec* pinned by tests — `docs/simulation-semantics.md`,
`docs/batch-interface.md`, `docs/file-format.md` — while surveys carry dates and
go stale (`docs/library-survey-2026-07.md`, `docs/flatlaf-evaluation-2026-07.md`,
`docs/pointer-geometry-census.md`, which itself opens with "Status: record of the
conversion"). #592's parity catalog is a survey: a graded comparison against three
incumbents at a moment in time. The pick rule is a permanent contract. It belongs
in a normative `docs/pointer-picking.md` (or an ARCHITECTURE.md recorded decision,
where the editor's state machine is already described); the catalog should *cite*
it, not *be* it. I am explicitly disregarding "stated in the catalog row" as the
contract's home.

**AC-4 is a stopwatch aimed at the wrong quantity.** Re-timing a human building a
4-bit counter cannot detect a picking regression — the noise floor of a human
task dwarfs the signal, and the baseline it compares against (#592 AC-5) does not
exist yet; `docs/` contains no catalog. I am disregarding AC-4 for this task. The
right regression instrument is the pick-list parity test above plus the existing
`SpatialIndexTest#reportsIndexVsScanTiming` habit. Leave the timed task to CAP-37,
where a whole-feature claim is being made.

**AC-3 is bookkeeping stapled to engineering.** bsiever-fork #18's disposition is
a #593-level or #592-level record, not a deliverable of a hit-test fix. Keeping it
here means the code change cannot be called done until an unrelated ledger is
reconciled.

**The blocking chain has become avoidance, not discipline.** #806 waits on #805,
which waits on #804, which waits on #441 (`MouseMachine`/`InteractionState` — no
such types exist anywhere in `src/` or `test/`), which waits on #316. Meanwhile
`SimpleEditor.java` is 5852 lines; ARCHITECTURE.md still calls it "~4k lines."
The god class grew while a four-deep gate protected it. A pure `Pick` function
plus a `SplitWire` op is precisely the increment that *shrinks* SimpleEditor
(three duplicated pick loops collapse into one call each) and lands entirely
outside it. That honours KC-37-1's intent rather than its queue position.

## What I would fund instead, in order

1. `Pick` — pure function, deterministic reverse-paint order, plus the parity
   test asserting head-of-list == topmost-painted-containing element. Land it and
   rewire the five call sites in `SimpleEditor`. Fixes hover/click divergence as a
   side effect.
2. `docs/pointer-picking.md` — normative: tolerance (`Wire.contains`'s existing
   band), order (reverse stable-id), the eligibility/pick split, and the named
   refusals. Catalog cites it.
3. Gesture-eligibility predicates with named refusals; delete the bare `continue`s.
4. `SplitWire` in `jls.collab.op` with an inverse, per the operation-layer contract.
5. Alt/repeat-click cycling over the pick list — small, and it retires the
   tolerance debate permanently.

Endorsed as to outcome; the wire-only scope, the "nearest" diagnosis, the
catalog-as-contract, and the stopwatch acceptance criterion should all go.
