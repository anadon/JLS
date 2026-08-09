# Issue #617: TASK-C558-4: an imported Digital circuit arrives laid out and readable, not as a pile of correctly-wired elements at the origin
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The Outcome names it honestly: the difference between a migration an instructor
accepts and one they abandon. That is an adoption claim, and it is load-bearing —
#558 exists to make JLS a Digital successor, and a circuit that opens as a
correct pile is a circuit the instructor closes. The goal is right and I endorse it.

What I am disregarding is the route. The issue picks **coordinate fidelity** as
the proxy for acceptance and then, in its boundary note, walls that work off from
the one subsystem in this repository that already solves 3 of its 4 acceptance
criteria. Both moves are wrong, and the second is the more expensive one.

## The boundary note contradicts a decision the project has already recorded twice

> "This is coordinate translation, not auto-layout. The heuristic layouter lineage
> (#62) is for netlists that arrive without geometry; `.dig` arrives with it."

The project has already adjudicated this, in the opposite direction, on the `.circ`
side of the same migration play:

- #323 (FEAT-025, open) §1, Explicitly out of scope: *"Coordinate-preserving layout
  as an engine. **The seam exists (`SchematicLayouter`)**; a coordinate-preserving
  engine plugged into it is separate work, informed by #62."*
- #342 §1 (closed as duplicate, but the reasoning is the record): *"Coordinate-preserving
  layout for migrated designs … is **the same engine seam with a different engine** —
  one that honours source coordinates instead of inventing them. **Keeping
  `SchematicLayouter` engine-neutral is what makes it a new engine rather than a new
  subsystem.**"*

#617's boundary note reads as a licence to build the new subsystem. It is the single
most consequential sentence in the issue and it should be struck and replaced with
its inverse. The distinction it draws is real at the level of *objective* (honour
geometry vs. invent it) and false at the level of *contract*: both produce grid
placements plus orthogonal routes that must satisfy identical invariants.

## Three of four ACs are already discharged by shipped code — through the seam

| AC | What already exists | Where |
|----|---------------------|-------|
| AC-1 determinism | *"must be deterministic: same graph, same result"* is the seam's contract | `src/jls/hdl/layout/SchematicLayouter.java:22-23` |
| AC-3 no overlaps, deterministic collision | `checkPlacements` rejects off-grid, off-canvas and body-overlapping placements wholesale | `src/jls/hdl/layout/LayoutInvariants.java:46-93` |
| AC-4 ordinary editable circuit | Layout output is realized as plain `ELEMENT`/`WireEnd` save text — there is nothing to lock | `NetlistImporter.Builder.emit`, `src/jls/hdl/imp/NetlistImporter.java:802` |

AC-4 is unfalsifiable-by-construction *if* the work goes through the existing
realization path, and becomes a genuine risk only if a second geometry path is built.
That inverts the issue's implied cost model: the seam route makes AC-1/3/4 free and
leaves one real problem; the bespoke route re-implements grid snapping, collision
resolution, invariant checking and WireEnd realization with no rubric behind any of it.

Going through the seam also finally tests **#62 IC7** ("second engine substitutes
behind `SchematicLayouter` with no caller changes beyond construction"), which #62
records as *"Design intent only"* and which nothing on master exercises.

## AC-2 is the wrong shape and will produce a report instructors learn to ignore

AC-2 splits wires into "preserved" and "re-routed, named in the report". The split
cannot survive contact with the data: JLS port attachment offsets are JLS's, and
Digital's raster is not JLS's `Geometry.SPACING = 12`. Every wire's two endpoints
move regardless of what the interior polyline does — so "Digital's routing expressible
in JLS" is false for essentially every net, and the report grows one entry per wire.
That is precisely the failure #323 I2 warns against: *"a construct reported but
actually realised is a report that trains instructors to ignore it."* A noisy report
is worse for adoption than a silent one, which puts AC-2 in direct tension with #558's
whole thesis. **I am disregarding AC-2 as written.**

## The reframing: hint-driven legalization, not coordinate translation

Recast the problem as *detailed placement with a displacement objective* — a
well-understood, deterministic, textbook-shaped problem — behind the existing seam:

1. **Carry geometry as hints, not as answers.** Add an optional `hint` (source-space
   x/y, orientation) to `LayoutGraph.Node` and an optional source polyline to
   `LayoutGraph.Edge`. Optional means #61's Yosys producer is untouched, which is
   exactly the IC7 experiment.
2. **`GeometryPreservingLayouter implements SchematicLayouter`.** (a) map source space
   to JLS space by one rational scale, rounded to `SPACING`; (b) **legalize**: resolve
   overlaps by minimum displacement over a stable `(x, y, source-id)` order — AC-3
   falls out with no bespoke tie-break rule; (c) route *every* edge through the
   existing orthogonal router, using the source polyline as a shape preference and
   re-deriving both endpoint stubs from JLS port offsets.
3. **The expressible/not-expressible fork disappears.** Routes are always re-derived,
   never "preserved", so the report names only material displacement — an element moved
   beyond *k* grid cells relative to its neighbours, or a net whose bend count changed.
   Short, honest, actionable.

Two concrete gaps the issue does not mention that this framing closes: Digital permits
negative coordinates, and `LayoutInvariants` rejects any negative position outright
(`LayoutInvariants.java:60-63`) — a normalizing translate-to-origin is mandatory, not
optional. And `Geometry.CIRCUITSIZE = 1000` is only a scroll-extent *minimum*
(`SimpleEditor.java:2060`), so a large Digital sheet does not overflow anything; that
worry can be dropped rather than designed around.

## The acceptance criterion this task should actually carry

"Coordinates map deterministically" is testable and nearly irrelevant; "looks like what
the instructor drew" is what matters. Two substitutes, both reusing shipped code:

- **Relative-arrangement preservation.** For every element pair, does the source's
  left-of / above relation survive legalization? Report the fraction. This is what
  recognizability means, and unlike coordinate equality it is well-defined across two
  different grids.
- **Run `LayoutMetrics.measure` on the preserved layout and diff it against
  `HeuristicLayeredLayouter`'s output for the same circuit.** If the instructor's own
  geometry scores worse on overlaps, crossings and routed length than what the heuristic
  invents from nothing, preservation is *harming* that file and the import should say so.
  Nothing in #617 can detect this; the rubric already exists and costs nothing to run.

That diff also hands the feature a one-line escape hatch it currently lacks: a
"re-layout this circuit" action that swaps in the heuristic engine. Same seam, no new
code, and it answers "the import is unreadable" without redoing any of this work.

## Where this pulls against the arc

- **Duplication with CAP-16.** `.circ` also arrives with geometry, and #323 already
  names this engine as a beneficial dependency. Built inside the `.dig` importer, it is
  rebuilt for Logisim. Built behind the seam, TASK-C558-4 *is* the `.circ` answer too —
  which roughly halves the combined cost of the two migration plays and is the strongest
  argument for the reframing.
- **The unnamed blocking decision.** #617 never says how placements become a circuit.
  #323 Open Question 4 records that `NetlistImporter.Builder` is `private static final
  class` and must be promoted before a second importer forks it, and #323 §2 rejected
  "emit save text and reparse" in favour of programmatic construction verbs (#337).
  Which of those #617 inherits is the real gate on this task; the coordinate arithmetic
  is not.

## The alternative I considered and am not recommending

Skip geometry translation entirely for v1: import through the heuristic layouter and
ship a **side-by-side verification view** — the source `.dig` rendered beside the
imported circuit — since checking a migration against the original is what instructors
actually do. It needs no coordinate mapping, turns "it doesn't look the same" into a
comparison rather than a defect, and is far cheaper. I reject it as the plan because
the instructor must go on *editing* this file for years, and a circuit they cannot
navigate from memory stays unowned. But it is the right fallback if KC-29-1's 1.5×
stop-loss trips, and #558 should record it as such rather than dropping straight to
"documented external-conversion recipe".

## Verdict

**endorse-with-reframing.** Keep the Outcome, AC-1, AC-3 and AC-4 verbatim. Strike the
boundary note and require the work to land as a second `SchematicLayouter` engine.
Replace AC-2 with "every route is re-derived orthogonally from the source polyline as a
shape hint; the report names material displacement only". Add an AC for the
arrangement-preservation fraction and for the `LayoutMetrics` diff against the heuristic
engine. Resolve the builder-promotion question (#323 OQ4) before this task starts.
