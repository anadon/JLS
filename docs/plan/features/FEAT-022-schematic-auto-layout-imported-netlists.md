# FEAT-022 - Schematic auto-layout for imported netlists

**Status:** proposed | **Cost:** 4-8 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

An imported design is readable. Cells arrive placed on the grid in signal-flow
order with orthogonal routes anchored at exact port offsets, hierarchy instances
are placed as instances rather than as their flattened contents, and the result
is measured against a fixed readability rubric rather than judged by eye. A
student who imports a core opens a schematic they can navigate, not a pile at
the origin.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-08 | required | a third-party core that imports as an unreadable pile has not been imported in any sense a person cares about |
| CAP-15 | required | round-trip parity with the toolchains includes the direction that produces a drawing |
| CAP-16 | required | migrated designs need placement wherever the source geometry cannot be preserved |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-020 | there is nothing to lay out that is not already laid out until the mapper realizes more cell kinds and hierarchy instances; the layout problem grows with the vocabulary, not with the layouter |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0050 | Heuristic layered layout for imported netlists | the in-process layered layout and its quality metric - **substantially shipped at HEAD**, see the correction below |
| TASK-0048 | Realize hierarchy instances on import | a hierarchical import needs instance placement, which the current graph does not carry |

## Acceptance criteria

- Every realized cell kind places and routes. Adding a cell kind to the mapper
  cannot produce a layout the invariant checker rejects; a test asserts this over
  the full realized vocabulary, not over the founding subset.
- The hard drawing invariants hold for every fixture: grid alignment, routes
  anchored at exact port offsets, orthogonal segments only, and no overlapping
  element bodies.
- The readability rubric is reported as numbers and gated: zero overlaps, an
  average and a per-net bound on crossings, a bound on routed length against the
  per-net Manhattan lower bound, and a floor on the fraction of non-feedback nets
  that flow left to right.
- Layout is deterministic: the same graph produces the same result, byte for
  byte, across platforms and runs.
- A hierarchical netlist places its instances as instances, and descending into
  one shows a laid-out interior.
- The rubric is met at core scale, not only at fixture scale, and the measured
  numbers at that scale are recorded.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #62 | HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process) | closes - **but its in-process half has already shipped**; what remains is vocabulary coverage, hierarchy and scale |
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | depends on - the layout problem is defined by what the mapper realizes |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - a black box is one placed rectangle, which is the cheapest layout case |

## Design notes

**Correction, verified this session, and it changes this feature's scope.** The
evidence corpus prices auto-layout as unstarted work. It is not. At HEAD there
is a complete `jls.hdl.layout` package - an engine-neutral layout interface, a
placement graph carrying per-port pixel offsets and marked register feedback
back-edges, a shipped heuristic layered layouter, a hard-invariant checker and a
quantified metrics rubric with launch thresholds - and the importer already calls
it on every import. Five test classes cover it. TASK-0050 as written in the
registry is therefore largely satisfied and this feature's remaining cost is
lower than its band, or the band should be re-spent on the parts that are not
done. The Link phase should reconcile this rather than an author silently
re-scoping it.

What is genuinely not done, and is what a task author should work on: the
layouter is exercised against the founding cell subset the mapper realizes;
hierarchy instances have no placement because the mapper does not produce them;
and the rubric's thresholds have not been reported at the scale of a real
imported core. Each of those is a real increment with a measurable end.

The engine decision is already adjudicated and recorded in the package: an
external layered-layout library is usable only as an out-of-process runner
because its license is incompatible with this project's, and everything in the
package is engine-neutral so a runner can be substituted without importer
changes. Do not relitigate that; do not link it in process.

A second consumer is coming. Coordinate-preserving layout for migrated designs
(FEAT-025) is the same seam with a different engine - one that honors source
coordinates instead of inventing them. Keeping the interface engine-neutral is
what makes that a new engine rather than a new subsystem.

## Risks

- **Scale.** The rubric's thresholds were fixed against fixture-sized graphs. A
  core-sized netlist is where a layered heuristic either holds or produces
  something no one will read, and nobody has measured it. Measure before
  promising.
- **Vocabulary drift.** Every mapper increment adds a cell shape. Without a test
  that runs the invariant checker over the whole realized vocabulary, a new cell
  kind can ship with a layout that violates the grid contract and nothing fails.
- **Rubric as a gate.** Thresholds that are gates on a heuristic can become
  gates on unrelated changes. State whether the rubric is a required check or a
  reported metric, and be consistent.

## Evidence

- Verified at HEAD `addc6c5`: `src/jls/hdl/layout/` contains
  `SchematicLayouter.java` (the engine seam, 32 lines),
  `HeuristicLayeredLayouter.java` (553), `LayoutGraph.java` (301),
  `LayoutInvariants.java` (165), `LayoutMetrics.java` (453) and
  `LayoutResult.java` (221).
- Verified at HEAD: `src/jls/hdl/imp/NetlistImporter.java:104` calls
  `new HeuristicLayeredLayouter().layout(builder.graph())` on every import, and
  `:112` emits the placed result. Layout is not optional and not a later stage.
- Verified at HEAD: `src/jls/hdl/layout/LayoutMetrics.java:10-12,25-33` fixes
  the rubric - overlap count as a hard zero, crossings per net, routed length
  against the per-net Manhattan lower bound, and a left-to-right flow fraction -
  with named launch thresholds, stated as having been fixed before any tuning
  began.
- Verified at HEAD: `src/jls/hdl/layout/package-info.java` records the
  2026-07-17 engine adjudication - an out-of-process runner for the external
  layered-layout library, in-process linking blocked on its license, a
  hand-rolled alternative explicitly out of scope, and the package kept
  engine-neutral.
- Verified at HEAD: `test/jls/hdl/layout/` contains five test classes totaling
  761 lines.
- Verified at HEAD: `src/jls/hdl/imp/NetlistImporter.java:125-161` refuses a
  multi-module netlist, so no hierarchical placement problem is ever constructed.
- Cost band basis: `09-format-adoption-plan.md` W5.3 (hierarchy import, 2-3 wk)
  plus the vocabulary and scale increments; the founding layouter's cost is
  already spent.
- Do not restate: issue #62 owns the rubric's derivation and the engine
  adjudication; `ARCHITECTURE.md` owns the package layering.
- **Cost reconciliation.** Band 4-8 mw. Tasks named for it: TASK-0048,
  TASK-0050, totalling 3.5 wk. The named tasks are the leading, dividable
  slices of this feature, not the whole of it; the residual has no task id,
  because the registry's task space is closed at TASK-0112. Do not read 3.5 wk
  as the feature.
