# TASK-0044 - Hierarchical emitters and their goldens

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0043

## Deliverable

Both HDL printers render module instantiation and multi-module output, with
committed goldens that the external compilers accept.

1. **Verilog.** `VerilogEmitter` gains a `visit(HdlModel.InstanceStatement)`
   arm in the anonymous `StatementVisitor` at
   `src/jls/hdl/VerilogEmitter.java:169-227`, emitting a named-port
   instantiation - `module_name inst_name (.port(net), ...);` - one binding per
   line, ports in the instantiated module's declaration order.
   `emit(HdlModel)` (`:29-40`) becomes the per-module renderer driven by the
   design-level `emit(HdlDesign)`; the header banner (`:60-81`) is written once
   per design, and each module keeps its own port and net declaration passes.
2. **VHDL.** `VhdlEmitter` gains the matching arm at
   `src/jls/hdl/VhdlEmitter.java:202-260`, emitting a `component` declaration
   into the architecture's declarative part and an instantiation with a
   `port map` into the body - the two-part split the emitter's
   `declarations`/`body` buffers (`:52-70`) already support. `entity` (`:119`)
   and `architecture` (`:154`) run once per module; the `ieee use` clauses are
   emitted per design unit as VHDL requires.
3. **Ordering.** Both emitters render `HdlDesign` in dependency order,
   definitions before uses, top last. Verilog does not require it; VHDL does,
   because an entity must be analyzed before a component that binds to it. One
   ordering serves both.
4. **New goldens** in `test/resources/hdl/`, three per language:
   `hier_one_deep`, `hier_two_deep`, `hier_two_instances`. Each golden is a
   **single self-contained file carrying every module**, because
   `IverilogCompileTest` compiles each `*.v` standalone
   (`test/jls/hdl/IverilogCompileTest.java:36-64`) and `GhdlCompileTest`
   analyzes each `*.vhdl` standalone
   (`test/jls/hdl/GhdlCompileTest.java:33-45`). A multi-file golden would need a
   skip entry in both, and a skipped golden is not an oracle.
5. **Structure checks.** `VerilogStructure.assertSane` and its VHDL sibling gain
   two assertions: every instantiated module is declared in the same file, and
   every port named in an instantiation exists on the instantiated module with
   the same width. `assertSane` already runs on every golden export
   (`test/jls/hdl/VerilogExportGoldenTest.java:40`), so this is free coverage on
   every existing export golden, not just the new ones.
6. **The export menu path.** Hierarchy is what makes HDL export usable on a real
   drawn design; if TASK-0051's menu item has not landed, note the dependency
   rather than adding a second entry point here.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-018 | The syntax half. TASK-0043's IR renders to nothing until an emitter has a template for it - literally: the visitor interface does not compile until both emitters implement the new method. |
| FEAT-023 | The external toolchain oracle can only cross-check output that exists; hierarchical goldens are the first ones that exercise `iverilog`'s and `ghdl`'s elaboration rather than only their parsers. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0043 | The emitters render `InstanceStatement`s and consume `HdlDesign`; neither type exists until TASK-0043 creates them, and `StatementVisitor`'s new method makes both emitters uncompilable in between. Land them in one branch. |

## Acceptance test

`test/jls/hdl/VerilogExportGoldenTest` gains, using the existing `assertGolden`
helper (`:35-55`):

- `hierarchyOneDeep()` - a top circuit instantiating one subcircuit;
  golden `hier_one_deep.v`.
- `hierarchyTwoDeep()` - golden `hier_two_deep.v`, asserting three modules in
  dependency order.
- `twoInstancesOfOneDrawing()` - golden `hier_two_instances.v`, asserting two
  instantiations and (until TASK-0039's digest deduplication lands) two
  uniquified module definitions, with the uniquifying suffix pinned by the
  golden so it cannot drift silently.

`test/jls/hdl/VhdlExportGoldenTest` gains the same three against `.vhdl`
goldens, asserting the component declaration appears in the declarative part and
the `port map` in the body.

`test/jls/hdl/IverilogCompileTest#everyGoldenCompilesUnderIverilog` and
`GhdlCompileTest#everyGoldenAnalyzesUnderGhdl` pick the new goldens up
automatically through their directory streams - **no new skip entries**. That is
the acceptance criterion that matters most: a hierarchical golden that needs a
skip is a hierarchical golden the external toolchain rejected.

`test/jls/hdl/VerilogStructure` gains
`assertEveryInstantiatedModuleIsDeclared(String)` and
`assertEveryInstancePortExists(String)`, called from `assertSane`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope for import | overlaps - the export half of #59's staging, completed for hierarchy |
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | informs - the board path consumes exported Verilog, and a design that must be flattened by hand before synthesis is not an on-ramp |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - the oracle compares JLS against an external simulator running JLS's own export, which requires the export to carry the design's structure |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | overlaps - a black box emits as an instantiation with no accompanying module definition, which is the one case where the "declared in the same file" structure check must be relaxed deliberately |

## Notes

- **Regenerating goldens regenerates all of them.** The flag is
  `-Djls.hdl.regenerate=true` (`test/jls/hdl/VerilogExportGoldenTest.java:26-27`)
  and it rewrites every golden the test class touches - `test/resources/hdl/`
  holds 32 `.v` and 32 `.vhdl` files at HEAD, one of which (`jls_map.v`) is a
  techmap library rather than an export golden. Review the diff like source; a pre-existing golden that moves during
  this task means the per-module refactor changed single-module output, which is
  a bug in TASK-0043, not an intended reformat.
- **The `@VERSION@` token is substituted per file** (`:32`, `:42`); a design-level
  header emitted once must still tokenize, or the goldens break on every version
  bump - which is exactly what the token exists to prevent.
- **VHDL architecture names are entity-scoped.** `architecture structural of
  <name>` (`src/jls/hdl/VhdlEmitter.java:157`) repeats the same architecture
  name per entity, which is legal and should stay - do not uniquify it and
  churn every existing VHDL golden.
- **`jls_map.v` is skipped by name** in `IverilogCompileTest` (`:41-47`) with a
  written reason: it is a Yosys techmap library, not an export golden. That is
  the bar for any future skip. Do not add one for a hierarchy golden.
- **`ghdl` analyzes, it does not elaborate, in the current test.** Analysis
  catches an undeclared component; it does not catch a port map that binds the
  wrong width to the right name. If the structure checks in item 5 are skipped,
  that class of defect ships. Consider a `ghdl -e` elaboration leg as a follow-on
  in TASK-0051 and say so rather than assuming analysis is enough.
- **Instance names must be legalized and unique per module.** They come from
  `SubCircuit.getName()` (`src/jls/elem/SubCircuit.java:115-121`), which is the
  local name in the parent circuit - user text, not an identifier. Route it
  through `HdlNames` in the parent module's scope, and record the rename in
  `model.renames()` so the header legend explains it, exactly as port renames
  are handled today.

## Evidence

- `src/jls/hdl/VerilogEmitter.java:29-40` (`emit`, one module), `:60-81` (the
  header banner and rename legend), `:161-227` (the `statement` dispatcher and
  its anonymous visitor with eleven arms).
- `src/jls/hdl/VhdlEmitter.java:52-70` (`emit`, with `declarations` and `body`
  buffers), `:119-143` (`entity`), `:154-167` (`architecture structural of`),
  `:202-260` (the visitor's eleven arms).
- `src/jls/hdl/HdlModel.java:141-144` - the compile-until-every-emitter-handles-it
  contract that makes this task's coupling to TASK-0043 mechanical.
- `test/jls/hdl/VerilogExportGoldenTest.java:26-27` (the regenerate flag), `:32`
  and `:42` (the `@VERSION@` token), `:35-55` (`assertGolden`, which also runs
  `VerilogStructure.assertSane`).
- `test/jls/hdl/IverilogCompileTest.java:33-64` - the directory stream over
  `*.v`, the `-g2005` compile, and the single documented skip;
  `test/jls/hdl/GhdlCompileTest.java:33-45` - the `*.vhdl` sibling.
- `test/resources/hdl/` - 67 entries at HEAD: 32 `.v` (one of them `jls_map.v`,
  the techmap library) and 32 `.vhdl`, plus the `board`, `import` and `scan`
  subdirectories.
- Do not restate: `docs/hdl-support-research.md` owns the staged HDL plan;
  `docs/vcd-interop.md` and `docs/parity-contract.md` own the comparison
  discipline the external oracle inherits.
