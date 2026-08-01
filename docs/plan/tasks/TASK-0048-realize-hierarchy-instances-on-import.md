# TASK-0048 - Realize hierarchy instances on import

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A multi-module netlist imports as nested subcircuits instead of being refused.
At HEAD both halves refuse: `selectModule` reports "the netlist has N modules
... and no single top; multi-module (hierarchy) import is not built in this
increment - flatten the design" (`src/jls/hdl/imp/NetlistImporter.java:149-160`),
and any cell whose type does not start with `$` is refused as
"hierarchy (subcircuit) import is not built in this increment"
(`:225-232`). The design already arrives hierarchical: the shipped pipeline runs
`hierarchy -auto-top` and **no** `flatten` (`test/jls/hdl/imp/ImportPipelineTest.java:108-112`).

1. **Module selection becomes a reachability walk.** `selectModule` keeps
   picking the root from the `top` attribute (`:140-147`) but stops treating
   multiple modules as an error; instead it returns the root and the set of
   modules reachable from it through non-`$` cell types. A module in the
   netlist that nothing reaches is reported as an informational line in the
   summary, not a failure.
2. **A non-`$` cell becomes a `SubCircuit` instance.** The early return at
   `:225-232` is replaced by instance realization: emit `ELEMENT SubCircuit`
   whose body is one nested `CIRCUIT` block holding the recursively realized
   definition (`docs/file-format.md:323`, `:360-372`). The instance's puts are
   built from the definition's `InputPin`/`OutputPin` set, which is how
   `SubCircuit` builds them at load
   (`src/jls/elem/SubCircuit.java:236-258`), so the connection is by port
   **name**, matching the netlist's own keying.
3. **Nested-block invariants are honored, not approximated.** A nested
   `CIRCUIT` block carries no `FORMAT` line (`docs/file-format.md:187`) and its
   element ids restart at 0 (`:372`). The `Builder`'s id counter is therefore
   per-definition, not per-file - today there is one flat namespace
   (`NetlistImporter.java:410-530`).
4. **Each instance gets its own definition copy, and that is stated.** JLS has
   no shared subcircuit definitions until FEAT-017; two instances of one module
   emit the module twice. The summary reports the definition count and the
   instance count separately so the blow-up is visible rather than discovered.
   A depth and total-element bound refuses a netlist whose expansion would
   exceed it, with the computed figure in the message.
5. **A cycle in the instance graph is a named refusal,** not a
   `StackOverflowError`: the walk carries the instantiation path and reports it.
6. **The instance is a layout node.** `LayoutGraph.Node` takes an id, a kind
   and a width/height (`src/jls/hdl/layout/LayoutGraph.java:74-124`); a
   subcircuit's box is sized from its port count, and its `Port` offsets come
   from the same pin ordering the nested definition emits.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-020 | the last structural refusal in the mapper; "an imported netlist actually runs" is false for any design a student wrote in more than one module |
| FEAT-022 | layout over a hierarchy is layout over boxes with ports rather than over a flattened gate soup, which is what makes an imported CPU legible at all |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| - | none. `SubCircuit` is a registered element at HEAD (`src/jls/elem/ElementRegistry.java:71`) and the nested-block grammar is already specified and loadable |

TASK-0047 is a strong ordering preference and **not** a dependency: a hierarchy
of pure combinational modules imports without it. Every realistic multi-module
design contains a register, so land TASK-0047 first or the corpus for this task
is artificial.

## Acceptance test

`test/jls/hdl/imp/HierarchyImportTest.java`, new:

- `aTwoLevelNetlistImportsAsNestedCircuitBlocks()` - a committed two-module
  JSON fixture; assert the emitted text contains one `ELEMENT SubCircuit` and
  one nested `CIRCUIT` block, and that the nested block carries no `FORMAT`
  line.
- `theImportedHierarchyLoadsAndReSavesIdentically()` - `Circuit.load` +
  `finishLoad` (`ImportPipelineTest.java:70-79`), then save and compare bytes.
  This is #61 prediction P3 and nothing asserts it at HEAD.
- `instancePortsBindByNameNotByPosition()` - a fixture whose module declares
  ports in an order different from the definition's pin order; assert the
  connectivity is still correct.
- `bothInstancesOfOneModuleGetIndependentNestedBlocks()` - assert two
  definition copies and a summary that says so.
- `anInstanceCycleIsRefusedWithThePath()` - assert the message names every
  module on the cycle.
- `anExpansionOverTheBoundIsRefusedWithTheComputedSize()`.

`test/jls/hdl/imp/ImportPipelineTest`, extended (yosys-gated):
`aHierarchicalDesignImportsWithoutFlatten()` - run the shipped pipeline on a
two-module Verilog fixture and import the result unmodified.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | closes (with TASK-0047) - §7's hierarchy round-trip item and prediction P3 |
| 62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | overlaps - a hierarchy node is a new node kind the layouter must size |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - the staged tracking issue |

## Notes

- **Import gains hierarchy before export has it.** `HdlExporter.REJECTED`
  refuses `SubCircuit` with "the HDL model has no module-instantiation
  statement" (`src/jls/hdl/HdlExporter.java:465-468`); TASK-0043 fixes the
  export side. The asymmetry is real and temporary: a design imported by this
  task cannot be re-exported until TASK-0043 lands. Say so in the summary text
  the user sees, not only here.
- **The `top` attribute is a string, and Yosys writes `"00000000000000000000000000000001"`.**
  The existing check is `!top.isEmpty() && !top.equals("0")` (`:141-146`) -
  reuse it exactly; do not parse it as a boolean.
- **Definition duplication is the cost driver, not the mapper.** A CPU with
  eight instances of one ALU slice imports eight ALU slices. Measure the
  element count on the corpus and record it; that measurement is what prices
  FEAT-017 honestly.
- **Do not invent a definition-sharing scheme here.** TASK-0041 owns the
  definition/instance split. This task emits what HEAD's format can load.
- **Names must survive legalization.** `legalize` (`:1055`) rewrites cell and
  module names for the save format; two distinct modules must not legalize to
  one name. Add a collision check with both original names in the message.
- **Do not restate the format.** `docs/file-format.md` §7 owns nested `CIRCUIT`
  blocks and the id-restart rule.

## Evidence

- `src/jls/hdl/imp/NetlistImporter.java:125-160` (`selectModule` and the
  multi-module refusal), `:223-232` (the non-`$` refusal), `:410-530` (the
  `Builder` and its single id namespace), `:1055` (`legalize`).
- `src/jls/elem/SubCircuit.java:236-258` - puts built from the subcircuit's
  pins by name; `:282-310` - `save`.
- `docs/file-format.md:127`, `:187`, `:323`, `:360-372` - the `circuit-block`
  production, the no-`FORMAT` rule, the `SubCircuit` row, and the per-block id
  restart.
- `test/jls/hdl/imp/ImportPipelineTest.java:104-112` - the shipped pipeline
  string, with `hierarchy -auto-top` and no `flatten`.
- `src/jls/hdl/layout/LayoutGraph.java:74-151` - `Node` and `Port`, the shapes
  an instance must fill.
