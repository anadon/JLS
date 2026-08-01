# TASK-0047 - Realize sequential, memory and arithmetic cells on import

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

The importer realizes every cell type its own validator accepts. At HEAD the
two disagree, and the disagreement is the whole defect: `CellValidator.SUPPORTED`
lists nineteen cell types (`src/jls/hdl/yosys/CellValidator.java:57-68`) plus
parameter-decided memories (`:105-107`) plus hierarchy, while
`NetlistImporter.mapCell` has cases for five (`src/jls/hdl/imp/NetlistImporter.java:235-249`)
and sends the rest to a "recognized cell that this importer increment does not
yet realize" problem at `:250-258`. A design passes validation and then fails to
import.

1. **New `mapCell` cases,** each with its realization:
   - `$add` -> `Adder` (`src/jls/elem/Adder.java:222-224`), carry-in and
     carry-out bound from the cell's `A`/`B`/`Y` widths.
   - `$dff` -> `Register` with the save-file type string `"pff"` for
     `CLK_POLARITY=1` and `"nff"` for 0; `$dlatch` -> `"latch"`. The three
     names are the ones the loader already accepts
     (`src/jls/elem/Register.java:148-160`, the `type` attribute at `:359-380`,
     documented at `:557-560`).
   - Yosys `init` on the output net -> the register's initial value.
   - `$tribuf` -> `TriState` (`src/jls/elem/TriState.java:277-279`).
   - `$mem`/`$mem_v2` in the two shapes the validator already decides
     (`CellValidator.java:32-41`): one combinational read port and no write
     port -> `Memory` with `int time 0` and `String init`; one combinational
     read port and one rising-edge write port -> the same plus `int sync 1`.
     Attribute names verified against `Memory.save`
     (`src/jls/elem/Memory.java:436-468`).
   - `$reduce_and`/`$reduce_or`/`$reduce_xor`/`$reduce_xnor`/`$reduce_bool`
     and `$logic_not`/`$logic_and`/`$logic_or` -> a `Splitter` feeding an
     N-input gate, the realization the research report's §6 table names.
   - `$bmux` -> `Mux` (the builder already has `addMux`, `:606`).
   - `$pos` -> plain wiring, no element.
2. **The switch loses its escape hatch.** The `default:` arm at `:250-258`
   shrinks to the residual set only, and the residual set is *derived* from
   `CellValidator.SUPPORTED` rather than restated - a cell type in `SUPPORTED`
   with no case is a test failure, not a runtime message.
3. **New `Builder` verbs** next to the existing `addInputPin` (`:538`),
   `addOutputPin` (`:557`), `addGate` (`:577`), `addMux` (`:606`),
   `addConstant` (`:630`) and the shared `add` (`:653`): `addAdder`,
   `addRegister`, `addTriState`, `addMemory`, `addSplitter`.
4. **Feedback edges get flagged.** `LayoutGraph.connect` takes a `feedback`
   boolean (`src/jls/hdl/layout/LayoutGraph.java:239-260`) that no caller sets
   true today because nothing sequential imports. Every edge whose source is a
   register output and whose target is upstream in the same module is marked,
   which is what lets the layouter's back-edge path (`HeuristicLayeredLayouter.java:169`,
   `:273`) and the rubric's feedback exemption (`LayoutMetrics.java:66-67`)
   do their jobs.
5. **The summary grows honestly.** `ImportSummary.countElement`
   (`src/jls/hdl/imp/ImportSummary.java:40-50`) gains the new categories so the
   post-import mapping table names them.
6. **The class javadoc's scope paragraph is rewritten,** not amended:
   `NetlistImporter.java:34-47` currently enumerates what is not realized. The
   surviving list after this task is bit-level slices/concatenations (the
   Splitter/Binder mesh), width mismatches needing `Extend`, and hierarchy
   (TASK-0048).

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-020 | mapper parity with the validator is the feature; this task is the parity |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| - | none. Every element type this realizes is registered at HEAD (`src/jls/elem/ElementRegistry.java:38-77`) and the builder/layout seam it writes through already exists |

## Acceptance test

`test/jls/hdl/imp/ValidatorRealizerParityTest.java`, new - the test that makes
the gap unable to reopen:

- `theMapperRealizesEveryCellTheValidatorAccepts()` - iterate
  `CellValidator.SUPPORTED`, feed one minimal committed netlist per cell type
  through `NetlistImporter.importNetlist`, assert no `ImportException` and a
  non-zero element count. A new supported cell with no case fails here.
- `everyRealizedCircuitLoadsThroughTheRealLoader()` - `Circuit.load` then
  `finishLoad`, the assertion shape `ImportPipelineTest` already uses
  (`test/jls/hdl/imp/ImportPipelineTest.java:70-79`).

`test/jls/hdl/imp/NetlistImporterTest`, extended:
`aPositiveEdgeDffBecomesAPffRegister()`, `aNegativeEdgePolarityBecomesNff()`,
`aDlatchBecomesALatch()`, `anInitAttributeBecomesTheRegistersInitialValue()`,
`anAsyncReadRomBecomesMemoryWithAccessTimeZero()`,
`aRisingEdgeWritePortBecomesMemorySyncOne()`,
`aRegisterOutputFeedingItsOwnInputIsMarkedAsAFeedbackEdge()`.

`test/jls/hdl/imp/ImportPipelineTest`, extended (yosys-gated):
`aSyncResetCounterImportsAndSimulatesLikeIverilog()` - run the shipped pipeline
over a counter, import it, batch-simulate it, and compare the trace against
`iverilog` on the same stimulus. This is prediction P1 of #61 and nothing
proves it at HEAD.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | closes (with TASK-0048) - this is §7's "Cell->element mapper for the restricted set" |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - the staged tracking issue |
| 62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | informs - this task is the first producer of the feedback edges #62's back-edge handling was written for |

## Notes

- **`dffunmap` is why only plain `$dff` arrives.** The shipped pipeline
  (`ImportPipelineTest.java:108-112`) runs `dffunmap`, which rewrites
  clock-enable and synchronous-reset flip-flops into `$dff` + `$mux` exactly.
  Do not add cases for `$sdff`/`$dffe`; if one appears the pipeline is wrong,
  which is what `CellValidator`'s "pipeline leftovers" bucket already says
  (`CellValidator.java:42-47`).
- **`$adff` stays rejected.** Asynchronous reset is a teachable reject with a
  written rewrite (`CellValidator.java:75-82`). #61 §7 leaves "grow Register
  with an async-clear pin" as a decision to be made on corpus evidence.
  Recommendation: keep the reject in this task and record the reject frequency
  from the corpus run, so the decision is made on data rather than here.
- **Memory access time 0 is already decided** by the validator's resolved
  decisions (`CellValidator.java:32-41`). Do not re-open it; match it.
- **The x-coercion counter is live and must keep counting.**
  `connectConstant` calls `summary.countCoercedX` (`NetlistImporter.java:755-775`);
  new realizations that consume constant vectors must route through it, not
  around it.
- **No partial circuits, ever.** `importNetlist` collects problems and throws
  before layout (`:71-99`). Every new failure path adds to `problems`; none of
  them returns a half-built `Builder`. #61 prediction P2 is the contract.
- **Two shipped tests assert the defect and must be inverted, not deleted.**
  `NetlistImporterTest.unrealizedButValidCellIsRejectedNotMismapped()`
  (`test/jls/hdl/imp/NetlistImporterTest.java:226-236`, over
  `reject_dff.json`) and `syncWriteRamValidatesButIsNotYetRealized()`
  (`:237-248`, over `reject_ram_sync.json`) currently assert that a *valid*
  cell is refused. Both fixtures move from the reject set to the accept set and
  both tests become realization assertions.
  `teachableRejectRelaysValidatorMessage()` (`:249-257`, `reject_adff.json`)
  and `bitSliceWithoutWholeDriverIsRejected()` (`:258-266`) stay as they are.
- **`Element.setValue` is silent on an unknown attribute**
  (`src/jls/elem/Element.java:344-351`), and the loader calls it unconditionally
  at five sites (`src/jls/Circuit.java:1067,1078,1089,1105,1116`). A typo in an
  emitted attribute name here produces a circuit that loads and is wrong.
  TASK-0003 is the fix; until it lands, the round-trip assertion in the
  acceptance test is the only guard.

## Evidence

- `src/jls/hdl/yosys/CellValidator.java:57-68` (`SUPPORTED`), `:70-107`
  (teachable rejects and memory types), `:32-41` (the memory decision).
- `src/jls/hdl/imp/NetlistImporter.java:223-267` (`mapCell` and its default
  arm), `:34-47` (the scope paragraph), `:410-1010` (the `Builder`),
  `:703-745` (reader resolution and the slice/concat refusal).
- `src/jls/elem/Register.java:148-160`, `:359-380`, `:557-560` - the three
  trigger-type save names.
- `src/jls/elem/Memory.java:436-468` - `type`, `bits`, `cap`, `time`, `sync`,
  `init`/`initrle`.
- `src/jls/hdl/layout/LayoutGraph.java:239-260` - the unused `feedback` flag.
- `docs/hdl-support-research.md` §7.2 - the cell-to-element mapping table and
  the "direct or <=4-element realization" finding. Reference it; do not restate.
