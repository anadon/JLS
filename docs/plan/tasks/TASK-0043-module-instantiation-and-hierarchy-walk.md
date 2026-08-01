# TASK-0043 - Module instantiation and the hierarchy walk

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

The HDL IR gains an instantiation statement and multi-module output, and the
exporter walks the subcircuit hierarchy binding instance ports to parent nets,
detecting cycles and propagating rejection with the instance path.

1. **The IR statement.** `HdlModel.InstanceStatement extends Statement`
   (alongside the eleven kinds at `src/jls/hdl/HdlModel.java:202-878`),
   carrying: the legalized module name, the legalized instance name, and an
   ordered list of `(portName, Operand)` bindings reusing the existing
   `Operand` record (`:69-110`) so an unattached instance input becomes a zero
   literal exactly as every other unattached input does.
2. **The visitor.** `StatementVisitor` (`src/jls/hdl/HdlModel.java:143-200`)
   gains `void visit(InstanceStatement statement)`. This is the mechanism the
   class documents at `:141-144` - "a new statement kind fails to compile until
   every emitter handles it" - so both emitters stop compiling until TASK-0044
   lands. That is intended; the two tasks land in one branch.
3. **Multi-module output.** `HdlEmitter.emit(HdlModel)`
   (`src/jls/hdl/HdlEmitter.java:19`) renders one module. Add
   `HdlDesign` - an ordered list of `HdlModel` with definitions before uses and
   the top last - and `HdlEmitter.emit(HdlDesign)` as a default method
   concatenating per-module renders. `HdlExporter.export`
   (`src/jls/hdl/HdlExporter.java:157-163`) and its `Result` record (`:122`)
   keep their signatures; the text they carry is now the whole design.
4. **The walk.** `buildModel` (`:175`) becomes `buildDesign`: one `HdlModel`
   per distinct definition, produced by the existing single-circuit walk;
   one `InstanceStatement` per `SubCircuit` in the parent, whose port bindings
   come from the parent's fused net groups through the same `groupOf` path
   every other element's operands use. Modules are deduplicated by the
   TASK-0039 digest **when it is available** and by nested circuit name plus a
   uniquifying suffix otherwise - so this task does not wait on TASK-0039.
5. **Per-module name scopes.** `HdlNames` (`src/jls/hdl/HdlNames.java:27,80`)
   reserves identifiers in one flat namespace. Each `HdlModel` gets its own
   `HdlNames`; module names are reserved in a design-level scope. Without this,
   two modules' identically named nets collide and one silently renames.
6. **`SubCircuit` stops being rejected.** `SubCircuit.class` is removed from
   `HdlExporter.REJECTED` (`:461-478`) and added to `EXPORTED` (`:429`);
   `classifiedElementClasses()` (`:487-493`) stays total over
   `ElementRegistry`, which `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`
   (`test/jls/hdl/HdlPolicyTest.java:392`) already pins.
7. **Cycles and rejection.** A definition reaching itself is an
   `HdlExportException` naming the cycle. An element that is still rejected -
   `Memory`, `RegisterFile`, `FieldExtend`, which stay in `REJECTED` - inside a
   subcircuit is reported with its **instance path**, not with a bare
   `describe(el)` that names two identically-placed elements identically.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-018 | This is the IR and walk half; TASK-0044 is the syntax half. A decomposed design currently throws rather than exporting. |

## Prerequisite tasks

None. The walk reads `SubCircuit.getSubCircuit()`
(`src/jls/elem/SubCircuit.java:102-107`), which returns a real nested `Circuit`
at HEAD because `SubCircuit.save` inlines one per instance. Deduplicating N
instances of one drawing into one emitted module needs TASK-0039's digest and
TASK-0041's shared definition; **emitting hierarchy at all does not**, and
sequencing this behind them would delay the capability by a quarter for a
constant-factor gain in output size.

## Acceptance test

`test/jls/hdl/HierarchyWalkTest.java`, new:

- `subcircuitBecomesAnInstanceStatement()` - a top circuit with one subcircuit;
  assert the design has two `HdlModel`s and that the top's statement list
  contains exactly one `InstanceStatement` whose bindings name the parent's
  nets.
- `nestedTwoDeepEmitsThreeModules()` - and asserts the ordering invariant
  (a module appears before every module that instantiates it).
- `anUnattachedInstanceInputBecomesAZeroLiteral()` - the same absent-input rule
  the rest of the exporter follows (`docs/simulation-semantics.md`).
- `twoModulesWithIdenticallyNamedNetsDoNotCollide()` - the per-module `HdlNames`
  scope; must fail against a single flat namespace.
- `aCycleIsRejectedNamingBothDefinitions()`.
- `aRejectedElementInsideASubcircuitNamesItsInstancePath()` - a `Memory` two
  levels down; assert the exception message carries the path, and that
  `rejectionListsEveryOffenderInOneMessage`'s one-message property still holds
  across levels.
- `twoInstancesOfOneDrawingEmitTwoModulesUntilDigestDeduplicationLands()` - the
  honest statement of this task's scope, replaced by a
  `sharedDefinitionEmitsOneModule()` assertion once TASK-0039 and TASK-0041
  land.

`test/jls/hdl/HdlPolicyTest#subCircuitIsRejectedCleanly` (`:82`) is **deleted**
and replaced by `subCircuitIsExportedAsAnInstance()`;
`exportPolicyIsTotalOverTheElementRegistry` (`:392`) must keep passing
unchanged, which is the proof the policy stayed total.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope for import | overlaps - #59 is the staged tracking issue; no single task closes it, and hierarchy is the gap its Stage 1 left |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - `CellValidator` already accepts hierarchy instances on the import side; realizing them is TASK-0048, and the two directions must agree on what an instance is |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | overlaps - a black box is an instance whose body JLS does not own; it must emit through the same `InstanceStatement` |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - a readable drawn CPU is decomposed, and a differential oracle over a design that cannot export is not a test |

## Notes

- **The refusal text is a promise this task keeps.** `REJECTED`'s `SubCircuit`
  entry says "the HDL model has no module-instantiation statement, so hierarchy
  cannot be rendered - flatten the circuit to export it"
  (`src/jls/hdl/HdlExporter.java:465-468`). The map's own javadoc says
  membership "does not mean never: each entry states what would have to exist
  first". This is that thing existing.
- **`Map.of` caps at ten pairs.** `REJECTED` currently has four
  (`:461-478`); removing one is free, but a later author adding to it past ten
  gets a compile error with an unhelpful message. Note it where the map lives.
- **The fused-net `Group` is per-circuit and must become per-module.**
  `HdlExporter`'s private `Group` (`:1146-1158`) and `UnionFind` (`:1161-1200`)
  are built once per `buildModel` call; running the walk per definition gives
  each its own, which is correct, but the jump-alias union
  is *circuit-scoped* - a `JumpStart` in a parent and one in a child with the
  same name are two nets, not one. Assert that.
- **Net names are synthesized from save ids today** - `net_<id>` at
  `src/jls/hdl/HdlExporter.java:353` and `net_u<id>` at `:381` - and save ids
  are per-`CIRCUIT`-block dense (`docs/file-format.md:366-372`), so two modules
  will both contain `net_3`. Per-module scoping makes that harmless; TASK-0008
  moves the derivation to stable id and makes it meaningful. Do not do TASK-0008
  here.
- **`Clock` synthesizes a port.** The walk adds a 1-bit `clk` input per `Clock`
  element; a subcircuit containing a `Clock` therefore gains a port its parent
  must drive. Decide and test whether the parent binds it from its own clock or
  the child keeps a free-running one - silently leaving it unbound produces a
  module that analyzes and does not run.
- **Warnings must carry the path too.** `model.addWarning` messages for skipped
  elements (`:210-213`) currently name the element only; two `Display`s in two
  instances produce two identical warnings.

## Evidence

- `src/jls/hdl/HdlModel.java:141-144` (the compile-until-handled contract),
  `:143-200` (`StatementVisitor`, eleven visit methods), `:69-110` (`Operand`),
  `:889-905` (the model's fields: one module name, one port list, one net list,
  one statement list).
- `src/jls/hdl/HdlEmitter.java:19` - `String emit(HdlModel model)`, one module.
- `src/jls/hdl/HdlExporter.java:122` (`Result`), `:157-163` (`export`),
  `:175` (`buildModel`), `:191-203` (the rejection that lists every offender),
  `:429` (`EXPORTED`), `:461-478` (`REJECTED`, with the `SubCircuit` reason),
  `:487-493` (`classifiedElementClasses`), `:1146-1200` (the fused-net `Group`
  and `UnionFind`).
- `src/jls/hdl/HdlNames.java:27,80` - one flat reservation scope per exporter
  run.
- `test/jls/hdl/HdlPolicyTest.java:82` (`subCircuitIsRejectedCleanly`), `:93`
  (`rejectionListsEveryOffenderInOneMessage`), `:392`
  (`exportPolicyIsTotalOverTheElementRegistry`).
- `src/jls/elem/SubCircuit.java:102-107`, `:281-289` - the nested circuit the
  walk descends into, and why it exists per instance today.
- `docs/standards-adoption/08-ipxact-export.md` item 7 - verified that
  `buildModel` "cannot even be pointed at a circuit containing a subcircuit",
  and that this is what a hierarchy-consuming use case hits first.
- Do not restate: `docs/simulation-semantics.md` owns the absent-input rule;
  `docs/hdl-support-research.md` owns the staged HDL plan.
