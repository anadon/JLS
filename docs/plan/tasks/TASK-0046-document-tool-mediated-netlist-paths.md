# TASK-0046 - Document the tool-mediated netlist paths

**Status:** proposed | **Cost:** 3 d | **Blocked by:** TASK-0045

## Deliverable

Written, CI-exercised recipes that produce EDIF, BLIF and a SPICE-shaped
netlist from JLS output by running the external synthesis tool - replacing
three costed rejections with a documented capability, and stating plainly that
JLS does not emit them directly and why.

1. **`docs/tool-mediated-netlists.md`,** new. One section per route. Each
   section carries: the exact command line, the JLS invocation that produced
   its input, the tool version the recipe was verified against, the consumer
   the output is for, and the honest limitation. The four routes:

   | route | command shape | consumer |
   |---|---|---|
   | EDIF | `yosys -p 'read_json d.json; synth_<target>; write_edif d.edf'` | Vivado, Quartus |
   | BLIF | `yosys -p 'read_json d.json; synth; write_blif d.blif'` | ABC, VPR/VTR |
   | SPICE | `yosys -p 'read_json d.json; synth; write_spice d.sp'` | ngspice, per CAP-14 |
   | schematic / browser sim | `netlistsvg d.json -o d.svg`; `yosys2digitaljs` | a shareable rendering |

2. **The refusal, written down.** A section titled so a reader searching for
   "EDIF" finds it: direct EDIF and direct BLIF emitters are refused, the
   reason is the format's value level (both are instance-of-cell or bit-level
   models needing a technology-mapping pass plus, for EDIF, a target cell
   library), and the named alternative is this document. Cite
   `docs/standards-adoption/11-costed-rejections.md`; do not re-argue it.
3. **A test-visible recipe source.** The command lines live in one fenced
   block per route with a stable marker, and the test in the next section
   parses them out of the document and runs them, so the document cannot drift
   from what CI proves. This is the discipline `ExtensionPointCatalogTest`
   already applies to `docs/extension-points.md`.
4. **Cross-links, not copies.** `docs/hdl-support-research.md` §7.2 gains a
   pointer; `README.md`'s format list gains one line per route. Neither
   restates the commands.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-019 | the feature's claim is "JLS reaches the gate-level interchange formats through real technology mapping"; without these recipes the claim is a netlist file and a shrug |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0045 | every recipe's first argument is a netlist only TASK-0045 writes. There is nothing to `read_json` before it lands |

## Acceptance test

`test/jls/hdl/ToolMediatedNetlistRecipeTest.java`, new. Skips as a unit when
`ToolLocator.findOnPath("yosys")` is null
(`test/jls/hdl/ToolLocator.java:57-73`), the idiom `ImportPipelineTest` uses
(`test/jls/hdl/imp/ImportPipelineTest.java:86-91`).

- `everyDocumentedRecipeRunsAndProducesOutput()` - for each fenced command in
  `docs/tool-mediated-netlists.md`, substitute a committed golden netlist,
  run it, assert exit status 0 and a non-empty output file whose text contains
  the module name JLS declared.
- `theEdifRouteNamesTheTopCell()` - asserts the emitted `.edf` contains a
  `cell` declaration for the top module.
- `theBlifRouteEmitsModelAndNames()` - asserts `.model` and at least one
  `.names` line.
- `theSpiceRouteEmitsASubcktForTheTopModule()` - asserts `.subckt <top>`.
- `theDocumentedCommandsAreExactlyTheTestedCommands()` - the doc-drift guard:
  the parsed command set equals the set the test executed, so adding a route to
  the document without arming it fails.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - documents what the staged path reaches without a new stage |

**No issue** proposes these recipes. Three separate costed rejections exist in
`docs/standards-adoption/` and none of them was ever converted into a route.

## Notes

- **This is three days because it is documentation over an emitter, not an
  emitter.** The corpus prices the same work at 0.5 wk for EDIF, 0.25 wk for
  BLIF, 0.75 wk for all of them together. The extra day here is the drift test.
- **`synth_<target>` is a real choice and must not be left as a placeholder.**
  A generic `synth` produces a technology-independent netlist that Vivado will
  read but not necessarily place. Name one concrete target per consumer in the
  document and mark the rest as untested.
- **The trap is claiming coverage the JSON writer does not have.** Anything
  `HdlExporter.REJECTED` refuses (`src/jls/hdl/HdlExporter.java:459-478`:
  `Memory`, `SubCircuit`, `RegisterFile`, `FieldExtend`) never reaches the
  netlist, so no recipe downstream can produce it. State this once, in the
  document's limitations section.
- **No network in CI.** Recipes must use tools installed by the workflow
  (TASK-0051 arms them); `netlistsvg` and `yosys2digitaljs` are npm packages
  and are therefore documented but **not** CI-exercised. Say which routes are
  proven and which are described - the difference is the whole value of the
  drift test.
- **Version drift is the standing threat** (#61 §10). Each recipe records the
  version it was verified against and the test prints the observed version into
  the failure message.

## Evidence

- `docs/icestick-bitstream-handoff.md:8-12` - the precedent for this document's
  voice: "JLS does not build bitstreams... delegate, do not reimplement".
- `docs/hdl-support-research.md:229` - Yosys is already the accepted external
  dependency, ISC-licensed, subprocess-only, no linking question.
- `docs/extension-points.md` + `ExtensionPointCatalogTest` - the precedent for
  a test that pins a document against code.
- `09-format-adoption-plan.md` W0.5 and the adoption table rows for
  `yosys write_edif` (0.5 wk), `write_blif` (0.25 wk), `write_spice` - the cost
  basis and the consumer list.
- `test/jls/hdl/imp/ImportPipelineTest.java:104-112` - the shipped example of a
  yosys command line built in a test and run as a subprocess.
