# TASK-0050 - Heuristic layered layout for imported netlists

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0047

## Deliverable

**Most of the algorithm already shipped.** `jls.hdl.layout` exists at HEAD -
`SchematicLayouter`, `HeuristicLayeredLayouter` (553 lines), `LayoutGraph`,
`LayoutResult`, `LayoutInvariants`, `LayoutMetrics` (453 lines),
`LayoutException` - the importer already calls it
(`src/jls/hdl/imp/NetlistImporter.java:99-107`), layout is deterministic
(`test/jls/hdl/layout/HeuristicLayeredLayouterTest.java:131-151`), grid and
port-offset invariants are checked (`LayoutInvariants.java:36-67` against
`Geometry.SPACING`), and the issue-#62 launch rubric is encoded as constants
(`LayoutMetrics.java:25-35`). What is missing is the half of #62 that makes the
rubric mean something: a corpus, an enforced gate, and the editor ergonomics.

1. **The rubric becomes a gate over real imports, not over hand-built graphs.**
   At HEAD `HeuristicLayeredLayouterTest` asserts `overlapCount == 0` and
   `leftToRightFraction == 1.0` on two synthetic graphs (`:74-111`); nothing
   asserts `MAX_CROSSINGS_PER_NET` (0.5), `MAX_CROSSINGS_ON_ONE_NET` (4) or
   `MAX_WIRE_LENGTH_RATIO` (2.0) anywhere. Add a corpus test that calls
   `LayoutMetrics.measure` on the layout of every committed import fixture and
   asserts `rubricFailures()` is empty.
2. **The showcase corpus is committed.** #62's addendum names three circuits -
   ALU slice, counter, small FSM. `test/resources/hdl/import/` holds four
   accept fixtures (`and2.json`, `aoi.json`, `mux2.json`, `const_and.json`),
   all combinational, plus one deliberately large import per #62 §10. The
   counter and the FSM require `$dff`, which is why this task is blocked.
3. **Compactness gets its reference.** `boundingBoxArea` is computed
   (`LayoutMetrics.java:75`, `:285`, `:361`) and compared against nothing. Commit
   a hand-drawn `.jls` reference per showcase circuit and assert the 4x bound
   against its measured area - the metric that catches one-element-per-layer
   towers passing every other check.
4. **Tuning against the numbers, once they exist.** Barycenter ordering and the
   greedy router are tuned only against measured corpus failures; each tuning
   change records the before/after metric table. If the rubric cannot be met
   after tuning, #62 §9 says escalate to the out-of-process ELK runner rather
   than growing the in-house layouter - do not silently relax a constant.
5. **The import arrives as one undoable unit and pre-selected.** #62's addendum
   requires a single Ctrl-Z to empty the canvas and the whole import selected
   and draggable. Neither exists, because **there is no import entry point at
   all**: `JLSStart` has no `-import` flag (its HDL branch is export-only,
   `src/jls/JLSStart.java:363-460`), there is no File->Import menu item, and
   `ImportSummary`'s own javadoc says the UI is "not built in this increment"
   (`src/jls/hdl/imp/ImportSummary.java:11-13`). This task adds the entry point,
   the one-shot summary dialog, and the single-undo grouping.
6. **The human trace trial is recorded,** not assumed: a reviewer traces three
   named signals end to end in under a minute per showcase circuit, and the
   result goes in the PR. #62 makes this the validity check on the rubric
   itself - a circuit that passes the numbers and fails the trace earns the
   rubric a new metric, not an exception.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-022 | "readable rather than a pile at the origin" is a claim only a measured corpus can support; the algorithm without the gate is untested quality |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0047 | two of the three showcase circuits are sequential, and the feedback back-edge path (`HeuristicLayeredLayouter.java:169`, `:273`) plus the rubric's feedback exemption (`LayoutMetrics.java:66-67`) have **no producer** until `$dff` imports - `LayoutGraph.connect`'s `feedback` flag (`LayoutGraph.java:239-260`) is passed `false` by every caller at HEAD. The rubric cannot be gated on circuits that cannot be imported |

## Acceptance test

`test/jls/hdl/layout/LayoutRubricCorpusTest.java`, new:

- `everyImportFixtureMeetsTheLaunchRubric()` - for each fixture in
  `test/resources/hdl/import/` that imports, layout it and assert
  `LayoutMetrics.measure(result).rubricFailures()` is empty; the failure message
  prints the whole metric table.
- `theShowcaseCircuitsMeetTheCompactnessBound()` - `boundingBoxArea` at most 4x
  the committed hand-drawn reference, per circuit.
- `theLargeImportStillMeetsTheRubric()` - the >=150-element fixture, guarding
  #62 §10's corpus-bias threat.
- `layoutOfTheCorpusCompletesUnderOneSecondPerCircuit()` - #62 prediction P2.

`test/jls/hdl/imp/ImportUndoTest.java`, new (display-tagged):
`oneUndoRemovesTheWholeImport()` and `theImportIsSelectedAfterItCompletes()`.

`test/jls/hdl/layout/LayoutInvariantsTest`, extended:
`aFeedbackEdgeRoutesOutsideTheLayerBandWithoutOverlap()` - the first assertion
over a real register back-edge.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 62 | HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process) | closes - the algorithm shipped; this task closes the addendum's rubric gate, corpus, and editor ergonomics |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | depends on - the corpus is #61's corpus, and #61's addendum owns the post-import summary dialog this task renders |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | overlaps - the import entry point is a new menu action landing in the class #84 is about, and single-undo grouping is exactly the undo model #84 names |

## Notes

- **Do not rewrite the layouter.** The cost band is for a corpus, a gate, an
  entry point and tuning. A reviewer seeing a large diff in
  `HeuristicLayeredLayouter.java` should ask which measured rubric failure it
  answers.
- **ELK stays out of process or stays out.** ELK Layered is EPL-2.0 with
  Exhibit A unfilled and no secondary-license designation, therefore
  GPL-incompatible for linking into JLS (#62 §1). The escalation path is a
  separate-JVM runner over pipes - mere aggregation - behind
  `SchematicLayouter`. Nothing in this task links ELK.
- **The rubric constants are public API of the test surface.**
  `MAX_OVERLAPS`, `MAX_CROSSINGS_PER_NET`, `MAX_CROSSINGS_ON_ONE_NET`,
  `MAX_WIRE_LENGTH_RATIO`, `MIN_LEFT_TO_RIGHT_FRACTION`
  (`LayoutMetrics.java:25-35`). Changing one is a recorded decision with the
  measurement that justified it, not a build fix.
- **Adding the menu item touches the `source==` dispatcher.** #84 measures a
  305-line source-identity dispatcher in `SimpleEditor` that already caused a
  defect. Add the action through whatever seam TASK-0020 leaves if it has
  landed; if it has not, add it and record the line count added to the
  dispatcher so the #84 measurement stays honest.
- **Geometry drift is a live threat** (#62 §10): the layout emits WireEnds at
  exact port offsets, so any change to element port geometry silently breaks
  every golden. `LayoutInvariants.check` is the guard; keep it in the corpus
  test rather than only in unit tests.

## Evidence

- `src/jls/hdl/layout/` - seven classes, 1,760 lines at HEAD;
  `HeuristicLayeredLayouter.java:155-200`, `:169`, `:273` (layering, cycle
  guard, feedback exclusion); `LayoutMetrics.java:25-35` (thresholds), `:75`,
  `:285`, `:361` (bounding box), `:66-67` (feedback exemption);
  `LayoutInvariants.java:36-67`, `:162` (12-px grid).
- `test/jls/hdl/layout/HeuristicLayeredLayouterTest.java:74-151` - what is
  asserted today: overlaps, left-to-right on two graphs, determinism.
- `src/jls/hdl/imp/NetlistImporter.java:99-107` - the layouter call site;
  `src/jls/hdl/imp/ImportSummary.java:11-13` - "the File->Import UI (not built
  in this increment)".
- `src/jls/JLSStart.java:363-460` - the HDL branch is export-only; no import
  flag exists.
- `test/resources/hdl/import/` - four accept and four reject fixtures at HEAD,
  none sequential.
- Issue #62 addendum - the five quantified rubric items and the two editor
  ergonomics requirements, verbatim.
