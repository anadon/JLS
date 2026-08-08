# Issue #329: FEAT-043: a second canvas places parts on a solderless breadboard, and the placed arrangement — not the schematic — drives the simulation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

The capability is right and it is the capstone's private spine: make the gap between "the
schematic works" and "the board I wired works" *observable*, and make a placement a thing the
simulator can be wrong about. Criteria 3, 4 and 5 are the best-designed part of the issue —
Δ per net rather than a boolean, the two-way iff, and the escape clause ("emitting a finding
the engine contradicts is worse than emitting none") are exactly right and I would keep them
verbatim.

What I do not accept is the *seam*. The issue names the deliverable "a second canvas" and
makes everything else downstream of it: §6 says "TASK-0093 after TASK-0092, **by necessity** —
there is no placement to check or to bind until the canvas exists." That sentence is the whole
design, and it is false. A placement is data. #297's own Tier A *writes* `placement.brd` and
`wiring.net` headlessly, with no canvas anywhere. If those formats are also **inputs**, the
check and the binding — the half that carries the pedagogy — are unblocked from the canvas,
from #316, from TASK-0036, from TASK-0105, and from the display lane (#91/#162) entirely.

## The reframing: a placement is data with a renderer, not a second editor

Concretely, and in the shape this repository already uses twice:

- **`jls.board` as a data table, mirroring `jls.hdl.board`.** `Board.java`'s own javadoc:
  *"a board is deliberately just data — (name, format, pin map) — so adding a board is adding
  a table entry in `Boards`, never new code."* A breadboard is the same object: rows, rails,
  hole count. `PcfEmitter.java` is 199 lines and is the shipped proof that a *physical* binding
  (net → real pin) lives headlessly with goldens, not in the editor.
- **A placement value:** `part → (hole index, orientation)` and `jumper → (hole, hole)`.
  Parts key off `ElementId` (#165, already shipped) — that is the half of #318 this genuinely
  needs.
- **A partition source into `jls.netlist` (#336).** #336's own Abstract already names *"the
  breadboard consistency check"* and *"breadboard wiring list"* as consumers of the one shared
  partition. Δ then compares two values produced by one pass — which is exactly why #297
  requires #336 — and is a headless unit test.
- **The canvas last, as a renderer plus an editor over that data.** `CircuitRenderer.exportImage`
  already draws to PNG and to byte-identical SVG through one paint path; a breadboard *picture*
  needs no interactive surface. `jls.hdl.layout` (`SchematicLayouter`, `LayoutInvariants`,
  `LayoutResult`) is the shipped precedent for *computing* a geometry behind an interface with
  hard invariants and rejecting violating output wholesale. A `BreadboardPlacer` belongs next
  to it.

Reverse the critical path: **TASK-0093 → TASK-0092**, not the other way round.

## Three claimed hard prerequisites that this dissolves

**Open Question 1 has a third answer, and it is neither of the two offered.** "A breadboard
strip is a net with no wire in it" is only a dilemma if the strip must become a `WireNet`.
`WireNet` (531 lines, `LinkedHashSet` for #98's multi-driver determinism) is a *load-path*
structure for drawn wires. Under #336 the IR is a quotient over wire ends **and puts**; a strip
contributes equivalences over puts directly and never enters `WireNet` at all. Determinism
improves rather than degrades: strip order is (board table order, hole index), a total order,
not an insertion order. The issue calls this question a weeks-scale schedule risk that *blocks
filing children*. It does not, once the placement stops trying to be a drawing.

**TASK-0105 (per-view palettes) is not required.** `PaletteContractTest` builds `expected` from
`ElementRegistry.all()` and `actual` from `Palette.entries()` and asserts set equality on tags.
A 74LS173 is not an `ElementType` — per #349 the package library is *data* — so it never enters
`expected`, and a part browser over that library that adds no row to `Palette.entries()` cannot
break the test. The prerequisite exists only under the assumption that the breadboard reuses the
schematic's palette mechanism. It should not: a package picker over a data library and a toolbar
total over a class registry are different objects. (If a view dimension is still wanted later,
note that `gui.palette-contributor` is already a typed, many-cardinality seam —
`GuiExtensionPoints.PALETTE_CONTRIBUTOR` — so a visibility attribute on `PaletteEntry` is a
cheaper shape than re-stating totality.)

**Per-view *geometry* is not required; per-section *versioning* is.** §6 argues "without a
per-view section the two views overwrite each other's coordinates." A breadboard placement has
no free coordinates — holes are integer indices into a board table. There is nothing to
overwrite. I7 (skip-and-preserve) is real and it is **#319**'s, which the capstone already
requires. The stable-addressing half of #318 is needed; the per-view-geometry half is not.

## Second alternative: document-scoped ops, not a view discriminator on `CircuitOp`

TASK-0036 proposes widening a sealed 11-member vocabulary (`CircuitOp.java:34-37`, 21 files) with
a view tag. But a placement mutation is not a circuit mutation — it does not change `Nets(C)`;
it changes a second artifact *about* the circuit. A separate sealed `PlacementOp` family in its
own package, observed through the existing many-cardinality `collab.op-observer` seam, leaves
`CircuitOp` untouched and makes invariant 3 ("undo in one view never disturbs the other") true
**by typing** rather than by a test — and it answers §7's own "advisory rather than structural"
deviation trigger in the strongest possible way. It also generalizes: the analog view (#331) and
sweep-06's layout view (`docs/capability-roadmap/sweep-06-physical-boundary.md` §E, which
independently asks for "a second canvas mode behind the existing renderer seam" with its own
coordinate space) get the same mechanism instead of a discriminator enum that grows per view.
Honest cost: two op streams need one cross-document rule (deleting an element orphans its
placement). One explicit rule beats an implicit tag on eleven op types.

## Where this pulls against the project's arc

- **It routes the capstone's spine through the weakest structural point in the tree.**
  `SimpleEditor.java` is 5,852 lines and `pom.xml:408` says `jls.edit` is *deliberately
  unfloored*. `HeadlessCoreRatchetTest` exists precisely to keep value out of that package.
  #329 proposes to put a capstone's payload into it and then asserts (criterion 6) that doing so
  costs nothing. Under the reframing, the first two landings add nothing to `jls.edit` at all,
  and the ratchet is cheap to hold because there is nothing to hold back yet.
- **Three "second view" features are being designed three times.** #329, #331 and sweep-06 §E
  each want a second surface; #329 already shares two tasks with the other two, which is the
  tell. The issue's own rejected alternative 2 was right in principle and too small in scope:
  file the generic *second document* feature once (document identity, per-document section,
  per-document op family, per-document surface) and make breadboard, analog and layout its first
  three consumers.
- **The audience is in batch mode.** The container image, `docs/batch-interface.md` as a
  stability contract, and the autograder bridge are where instructors actually are. A
  `-breadboard-check placement.brd circuit.jls` verb reaches every one of them; a canvas reaches
  none. And a student debugging a real board has the board in front of them — re-entering 200
  jumpers by dragging is worse data entry than typing or importing a wiring list.
- **The cost gap is the tell.** The issue prints 4 unshared weeks against a 9–15 mw band and
  names the residual — "the part-placement vocabulary across the package library" — as having
  **no task id**. Under this reframing that residual *is* the feature, and it gets an id. The
  2.25×–3.75× mystery is not a rounding artifact; it is the plan pointing at its own real center
  of mass and then not filing it.

## What I am explicitly disregarding

- §6's "TASK-0093 after TASK-0092, by necessity" — inverted above.
- The hard-prerequisite status of TASK-0105 and of the per-view-geometry half of TASK-0036/#318.
- Open Question 1 as posed: both its options are rejected in favour of a third.

## What survives unchanged

Criteria 3, 4 and 5 including the unimplemented-rather-than-lying escape clause; the Δ-per-net
formulation; the pedagogy ratchet (I6); I7's skip-and-preserve, re-homed onto #319. Criterion 4's
iff becomes stronger, not weaker: it is a property over two partitions, provable headlessly, with
no xvfb and no dependence on #91/#162.

## Recommendation

Keep the outcome. Re-cut the children: (1) `jls.board` data + placement/wiring-list format +
headless Δ checker and contention set, consuming #336 — this is the un-idded residual, and it is
the feature; (2) placement as a net-partition source, so "which view drives the simulation"
becomes "which partition source the elaborator consumes," a saved property and a CLI flag rather
than a GUI preference; (3) an auto-placer next to `SchematicLayouter`, so the first placement a
student sees is computed, not typed; (4) the canvas, last, behind #316, as a renderer and editor
over data that already works. The per-issue note to #401/#396 (filed per the 2026-08-08 comment)
is that the boundary between them should move: #396 should not wait on #401.
