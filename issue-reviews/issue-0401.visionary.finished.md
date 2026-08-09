# Issue #401: TASK-0092: a second canvas places parts on a solderless breadboard — its own geometry, ops, undo and default-hidden palette, added inside the editor's coverage budget
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the machinery and the ask is one sentence from #297: *the student's board is
wrong in ways the schematic cannot be wrong, and JLS should say so.* The specific
failure named there — three inputs left floating on a 74LS173 — is the payload.
Everything else in this task (a canvas, a palette, two sealed ops, an undo
discriminator) is plumbing chosen to deliver that payload through the GUI.

The plumbing is where I part company with the issue. #297 §1 already tiers its own
walk-through: **Tier A is headless** — `jls -breadboard sap1.jls -o plan/` writes
`bom.txt`, `refdes.map`, `wiring.net`, `placement.brd`, `drc.txt` — and #297's cost
section names exactly that tier as the **7-14 mw demo slice, "no GUI and no
four-state core."** Tier B is the canvas. This task is Tier B's front half, and it
has been filed first, ahead of the tier the capstone itself says carries the value
and survives descoping. `placement.brd` is a *headless artifact*: the hole
occupancy that this task treats as a by-product of dragging packages around is,
in the capstone's own outcome statement, a file JLS writes.

## The cheaper route to the same end

**Reframe: build the placement pipeline, not the canvas.** Concretely, ship
`jls.phys` (see naming note below) with:

- a declared board table — tie-point columns, rails, pitch — following the
  `jls.hdl.board.Boards` precedent exactly (small, data, no board-description
  language);
- a placement model (`refdes → origin hole + orientation`) and a hole-occupancy
  partition computed as a disjoint-set over holes, per §7.10;
- a `-breadboard` CLI mode that emits `placement.brd` and the occupancy structure
  #297's acceptance test replays;
- the honesty statement, in the `VerilogEmitter.header` idiom that already exists
  (`src/jls/hdl/VerilogEmitter.java:64-82`), written into the report.

That is roughly items 3 and 7 of §8 — the two headless bullets — and it is **the
entire value this task hands to TASK-0093**, which is the issue that actually
tells the student something true. TASK-0093 consumes "the canvas and the
hole-occupancy structure"; only the second half is load-bearing for it.

Cut this way, the task loses **every one of its blockers**. No view discriminator
(TASK-0036) because there are no new ops. No view-dimensioned palette (TASK-0105)
because there is no second palette. No `SimpleEditor` decomposition (#316/#84,
and the ordering edge the 2026-08-08 comment adds) because nothing is grafted onto
a 5,852-line class. No display lane (#91/#162). No K9 exposure at all, because the
first-year never opens a CLI flag — which is a stronger guarantee than "the palette
is default-hidden", and free rather than defended by a three-quantity ratchet. A
task currently gated behind three open features and two unfiled siblings becomes
schedulable now.

## The reframing that dissolves Open Question 1

The issue calls the strip representation — synthetic `WireEnd`s versus changing
the pinned `WireNet` — an absolute execution blocker that "moves this task and
TASK-0093 by weeks." It blocks TASK-0093. It does not block this task, and §7.10
already proves why: the breadboard partition is defined as a transitive closure
over holes and jumper edges, **entirely independently of `WireNet`**. A
union-find over declared strips plus jumpers is a headless, ~150-line, fully
testable object that never touches `jls.elem`. The question of how that partition
is handed to the propagator only arises when the placement drives a simulation —
TASK-0093's job. Under the redirect, P10 (`WireNet` determinism unmodified) holds
by construction rather than by vigilance, and the self-declared hard blocker
simply is not this task's question.

## Where the issue pulls against the codebase as it stands

**H3 and P3 describe an undo manager JLS does not have.** `src/jls/edit/UndoManager.java`
is two stacks of `CircuitSnapshot` — deflated whole-circuit save text restored
through the load path (ARCHITECTURE.md, "The save/load pipeline"). Ops do not sit
in the undo stack; `OpSink` applies an op and then records a snapshot
(`src/jls/collab/op/OpSink.java:5-13`, `SimpleEditor.java:5547-5612`). So if
breadboard placements live in the save file's `VIEW` section — as §7.7 requires —
then a snapshot restore rewinds **both views at once**, and P3 ("each undo affects
only its own view") is false by construction of the shipped undo, not merely
untested. Satisfying it means converting editor undo from snapshots to op
journalling: that is #167/#282/#283 and the #337 residual, a large dependency this
task neither names nor budgets. The issue's exactness metric (apply-then-invert
returns the canonical save to its prior bytes) is the op layer's oracle (#166),
which reinforces the confusion: §5 measures the op layer, §6 wires the snapshot
manager, and the two are different mechanisms.

**Placements are not circuit mutations, so they should not be `CircuitOp`s.**
`CircuitOp`'s own contract is "one editor mutation … the future network surface of
collaborative editing", addressed by stable element id, validating *circuit*
invariants. Moving a DIP from row 12 to row 19 changes no element, no pin, no net
in the circuit graph. Putting `PlacePart`/`MovePlacement` into that sealed
vocabulary is what *creates* the ambiguity that TASK-0036's view discriminator then
exists to resolve — a prerequisite manufactured by the design choice it defends
against. The cleaner seam is the one the project already uses for non-circuit
document data: a section in the save format, snapshot-restored for free.

**`jls.bread` is the wrong name for the abstraction H1 asserts.** H1 says the board
is declared data so that a different layout is data, not code. Then the package is
named after one form factor. `jls.hdl.board` already models "a physical target as a
declared table" for FPGA boards; a perfboard, a protoboard, or a dev-board header
strip is the same abstraction as a solderless breadboard with a different table.
Name it for the concept (`jls.phys`, holes/strips/placement) and H1 is expressed in
the package structure rather than asserted in a hypothesis.

**The placement problem is already solved once in this tree.** `jls.hdl.layout` is
an engine-neutral placement pipeline — `LayoutGraph` (elements with per-port
offsets, connections grouped into nets) → `SchematicLayouter` → `LayoutResult`
(grid positions), with `LayoutInvariants` for hard geometric rules and
`LayoutMetrics` for a quantified quality rubric. Breadboard placement is that shape
with a coarser grid and an occupancy constraint. This issue proposes to build a
placement model, a geometry, and an invariant set from scratch without mentioning
that package once. Either reuse the seam or record why it does not fit; silently
building a second one is how a single-maintainer project grows two placement
subsystems.

## The pedagogical argument against manual placement

Tier B step 4 is "drag 35 packages onto two rendered breadboards and jumper hole to
hole." That is the physical build, performed twice — once in a mouse-driven
simulator and once with tweezers. The simulator run adds no information the real
board lacks; it is the tedium of construction without the learning of construction.
The inversion is more useful and much cheaper: **JLS computes the placement and
hands the student a wiring list**, and the diagnostic loop is "here is what you
built versus what the plan says", which the student can express by editing
`placement.brd` or by a future capture path. Auto-placement plus a report beats
manual placement plus a report, for the same reason `jls.hdl.layout` exists rather
than asking users to hand-place imported netlists.

## Disregarding the stated acceptance criteria

I am explicitly setting aside §14's canvas-shaped criteria — a placeable/movable/
rotatable part on a drawing surface, two new sealed op records, a default-hidden
palette, the three-quantity K9 ratchet, and the display-tagged P1/P3/P5 runs. They
are internally coherent, but they are the acceptance criteria of the tier #297 lists
second and prices highest, and each one imports a prerequisite the headless cut does
not need. What I would keep unchanged: P6 (the honesty statement — genuinely the
best idea in the issue, and it belongs in `drc.txt` where a golden test can pin its
text), P7 (layout as declared data, zero renderer hunks — trivially true when there
is no renderer), P8 (no AWT), and P10 (`WireNet` untouched).

## What I would file instead

1. **TASK-0092a — headless breadboard placement and occupancy.** `jls.phys` with a
   declared board table, the placement model, the union-find partition, the
   `-breadboard` emitter for `placement.brd`, the honesty block in the HDL-header
   idiom, `@NullMarked` + a per-package JaCoCo floor, born floored. No blockers.
   Deliverable measured by golden files, not by Xvfb.
2. **TASK-0092b — a read-only breadboard renderer**, after #84 lands: a view that
   *draws* the placement the headless pass produced, with no palette, no ops, no
   undo, no editing. This is a `CircuitRenderer` peer, not a second `SimpleEditor`,
   and it costs near nothing against K9 because there is nothing to click.
3. **TASK-0092c — interactive placement editing**, filed only if an instructor asks
   for it after 1 and 2 ship. This is where the ops, the view discriminator, the
   per-view palette and the undo question genuinely belong — and by then the
   op-journalled undo they assume may actually exist.

If the maintainer rejects the redirect and keeps the canvas as filed, the minimum
change I would insist on is reconciling §6/H3 with the snapshot-based `UndoManager`
at HEAD, and dropping Open Question 1 from this task's blocking set — it is
TASK-0093's question, and holding this issue for it is weeks of stall bought for
nothing.
