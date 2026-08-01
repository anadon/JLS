# TASK-0045 - The synthesis-tool netlist writer

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A third `HdlEmitter` that writes the Yosys `write_json` netlist schema, so JLS
output can be fed back into the synthesis tool it already shells out to.

1. **A write path in `JsonValue`.** `src/jls/hdl/yosys/JsonValue.java` is
   parse-only at HEAD: `parse` at `:85`, accessors `:102-196`, a private
   `Parser` at `:230`, and no serializer anywhere in the 580 lines. Add
   constructors (`object()`, `array()`, `of(String)`, `of(long)`) and
   `String write()` emitting canonical JSON: insertion-ordered keys, no
   trailing whitespace, `\n` line endings, escapes exactly inverse to
   `parseEscape` (`:407`). `MAX_DEPTH = 200` (`:33`) bounds the reader; the
   writer must not emit deeper than it, and a test asserts that.
2. **`src/jls/hdl/yosys/YosysJsonEmitter.java`,** implementing `HdlEmitter`
   (`src/jls/hdl/HdlEmitter.java:9-28`): `emit(HdlModel)` and
   `fileExtension()` returning `"json"`. Registered at
   `HdlExtensionPoints.EXPORTER` (`src/jls/hdl/HdlExtensionPoints.java:23-26`)
   alongside `VerilogEmitter` and `VhdlEmitter`.
3. **A stated mapping or a stated refusal for all eleven statement kinds.**
   `HdlModel` carries exactly eleven `Statement` subclasses at HEAD:
   `GateStatement` (`:202`, `Op` at `:205`), `ReplicateStatement` (`:253`),
   `ConstantStatement` (`:285`), `TriStateStatement` (`:317`),
   `AdderStatement` (`:353`), `RegisterStatement` (`:401`, `Kind` at `:404`),
   `BitMapStatement` (`:468`), `SelectStatement` (`:538`),
   `PriorityCaseStatement` (`:615`), `StateMachineStatement` (`:725`),
   `ShiftStatement` (`:840`). Each gets a row in one table in the class
   javadoc and one `case` in a switch with **no default arm**, so a twelfth
   statement kind stops the compile. `ConstantStatement`, `BitMapStatement`
   and `ReplicateStatement` cost nothing: constants and bit routing live in
   the connection array, not in cells.
4. **Bit encoding reuses the reader's constants.** The writer emits net ids
   and the `"0"`/`"1"`/`"x"`/`"z"` string bits through the same
   `YosysNetlist.BIT_*` values the parser recognizes
   (`src/jls/hdl/yosys/YosysNetlist.java`), not through a second private
   encoding.
5. **The command line learns `.json` in both places it decides.**
   `src/jls/JLSStart.java:383-385` is a two-way ternary
   (`endsWith(".v") ? new VerilogEmitter() : new VhdlEmitter()`) and
   `:1088-1090` is the argument validation
   (`!hdlName.endsWith(".v") && !hdlName.endsWith(".vhd") && !hdlName.endsWith(".vhdl")`).
   Both change, or the emitter ships behind a switch that can never select it.
6. **Goldens plus a schema check.** One `.json` golden per fixture in
   `test/resources/hdl/` (the corpus is ~20 paired `.v`/`.vhdl` files today).
   The netlistsvg machine-readable schema (`lib/yosys.schema.json5`, MIT) is
   vendored under `test/resources/hdl/` with its license notice, because CI
   has no network; goldens validate against the vendored copy.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-019 | this is the writer the feature is named for; everything else in it is documentation over this output |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| - | none. `HdlModel` and the exporter walk exist at HEAD and this task reads them unchanged |

## Acceptance test

`test/jls/hdl/yosys/JsonValueWriteTest.java`, new:
`parseOfWriteIsTheIdentity()` over every committed netlist fixture;
`writeIsDeterministic()` (same value, same bytes, 100 iterations);
`writingDeeperThanMaxDepthIsRefused()`.

`test/jls/hdl/yosys/YosysJsonEmitterGoldenTest.java`, new:
`everyFixtureMatchesItsGolden()` - byte-exact over the `test/resources/hdl/`
corpus; `reEmittingIsByteIdentical()` - the `HdlEmitter` determinism contract
(`HdlEmitter.java:11-12`); `everyGoldenValidatesAgainstTheVendoredSchema()`;
`ourOwnValidatorAcceptsWhatWeWrite()` - `CellValidator.validate` returns
`List.of()` for every golden, which is the cheapest proof the cell vocabulary
is real (`src/jls/hdl/yosys/CellValidator.java:57-68`).

`test/jls/hdl/yosys/YosysReadsOurNetlistTest.java`, new, skip-when-absent via
`ToolLocator.findOnPath("yosys")` (`test/jls/hdl/ToolLocator.java:57-73`):
`yosysReadsTheEmittedNetlistAndReportsTheSameInterface()` - runs
`read_json <golden>; hierarchy -check; write_json` and asserts the round-tripped
port list equals the one JLS declared.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - the tracking issue for the export half; no single task closes it |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - #61 built the reader this task inverts |

No issue proposes the writer. The corpus rates it the best formats-per-week
item in the plan and the tracker does not mention it; that gap is the finding.

## Notes

- **The export policy bounds the writer, not the other way round.**
  `HdlExporter.REJECTED` (`src/jls/hdl/HdlExporter.java:459-478`) refuses
  `Memory`, `SubCircuit`, `RegisterFile` and `FieldExtend`, so those never
  reach any emitter. The JSON writer inherits exactly that coverage and must
  not advertise more. `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`
  (`test/jls/hdl/HdlPolicyTest.java:392-407`) is what keeps that honest.
- **Golden churn is a scheduling hazard, not a dependency.** Net names derive
  from save order today; TASK-0008 makes them a function of `stableId`. Landing
  this task first means regenerating every `.json` golden when TASK-0008 lands.
  Either accept one regeneration or sequence TASK-0008 first - do not build a
  second name scheme here.
- **`StateMachineStatement` is the one real decision.** It is a behavioral
  construct (`HdlModel.java:725-838`) with no single Yosys cell. Recommendation:
  refuse it by name in this task with the reason, rather than hand-lowering an
  FSM into `$dff` + `$pmux` and owning a second synthesis pass - the whole point
  of the format is that the external tool does the lowering.
- **Do not restate the format.** `docs/hdl-support-research.md` §7.2 owns the
  cell mapping evidence; `docs/file-format.md` owns JLS's own save grammar.
- **Positional pin order is the silent failure mode.** The corpus flags it as
  the only item in the study that is wrong without being loud. A netlist is
  name-keyed, so this writer is not exposed - but say so, because the KiCad and
  SPICE emitters that consume the same model are.

## Evidence

- `src/jls/hdl/yosys/JsonValue.java:30-228` - parse-only at HEAD; `:33`
  `MAX_DEPTH`; `:85` `parse`; `:230` the private `Parser`.
- `src/jls/hdl/HdlEmitter.java:9-28` - the two-method contract and the
  determinism requirement.
- `src/jls/hdl/HdlExtensionPoints.java:23-26` - the published `EXPORTER` seam,
  catalogued in `docs/extension-points.md` and pinned by
  `ExtensionPointCatalogTest`.
- `src/jls/hdl/HdlModel.java:202-924` - the eleven statement classes.
- `src/jls/JLSStart.java:383-385`, `:1088-1090` - the two suffix decisions.
- `09-format-adoption-plan.md` §3 datum 1 and W1.3 - the 3-4 mw band, the
  "~120 L writer" estimate, the netlistsvg schema pointer, and the consumer
  list (netlistsvg, DigitalJS, and Yosys's own EDIF/BLIF/SPICE backends).
