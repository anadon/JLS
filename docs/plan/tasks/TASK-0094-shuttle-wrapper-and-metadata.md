# TASK-0094 - The shuttle wrapper and its metadata

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0049, TASK-0077

## Deliverable

A `tinytapeout` target in `jls.hdl.board` that generates the fixed shuttle
top-level wrapper and the project metadata file, both validated all-or-nothing
before anything reaches disk. Structurally this is the `PcfEmitter` problem
again: walk the port set `HdlExporter.buildModel` already produced, bind it to
a fixed external interface, refuse cleanly when the binding cannot be made.

Precisely what changes:

- `src/jls/hdl/board/Board.java:33-45`: the `Format` enum gains a
  `TT_WRAPPER` constant. **This enum has exactly one constant (`PCF`) at HEAD**
  and the extension-dispatch switch in `JLSStart` will stop compiling until it
  handles the new one - that is the intended trap, not a defect.
- `src/jls/hdl/board/Boards.java`: a `tinytapeout` entry. Its "pins" are the
  fixed shuttle signal names (`ui_in[0..7]`, `uo_out[0..7]`, `uio[0..7]`,
  `clk`, `rst_n`, `ena`), so the existing `PinBindings` file format binds a
  design's ports to them with **no new binding UX at all**.
- `jls/hdl/board/ShuttleWrapperEmitter.java`: emits a Verilog module whose
  name is `tt_um_<sanitized-project-name>` and whose port list is exactly the
  template's fixed signature - `ui_in`, `uo_out`, `uio_in`, `uio_out`,
  `uio_oe`, `ena`, `clk`, `rst_n`, all 8-bit except the three scalars - and
  instantiates the JLS-exported module inside it, wiring the bound ports and
  tying every unbound output bit and `uio_oe` bit to a declared constant.
- `jls/hdl/board/ShuttleMetadata.java`: emits `info.yaml` with the declared
  `yaml_version`, the project title/author/description supplied through the
  bindings file's comment-free key section, the tile size (`1x1` through
  `8x2`), the clock frequency, and the pin-name arrays in shuttle order. Hand-
  written emission (the fields are a fixed, small set); **no YAML library is
  added** - a new dependency for eleven scalar fields fails the offline-jar
  and bill-of-materials discipline.
- Refusals, each a specific message: a design with no clock bound to `clk`; a
  design with more than eight inputs or eight outputs after `uio` allocation;
  a design whose `Register` elements carry no reset (see Notes); a design whose
  bidirectional pins cannot be assigned an output-enable expression.
- Goldens: `test/resources/hdl/board/tt_um_counter.v` and
  `test/resources/hdl/board/tt_um_counter.info.yaml`.

Done means: `jls -export out.v -board tinytapeout -pins tt.txt design.jls`
writes three files - the design's Verilog, the `tt_um_*` wrapper, and
`info.yaml` - or writes none of them and names every problem in one message.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-044 | The wrapper and metadata half of "Tiny Tapeout wrapper and shuttle handoff". |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0049 | The shuttle signature makes `uio_oe` mandatory and `uio_*` bidirectional. `HdlModel.Direction` is `{INPUT, OUTPUT}` at HEAD (`src/jls/hdl/HdlModel.java:28-33`) and there is no bidirectional pin in the element registry, so there is literally nothing for `uio_oe` to bind to. Only TASK-0049 creates it. |
| TASK-0077 | `rst_n` is mandatory in the wrapper and must actually reset the design. `Register` has no reset attribute and no reset input at HEAD; the exporter emits an initial value instead (`test/resources/hdl/counter.v:21`, `reg [3:0] count = 4'h0;`), which FPGA synthesis honors and **ASIC synthesis discards**. Wiring `rst_n` to nothing would produce a chip that never resets. |

## Acceptance test

`test/jls/hdl/board/ShuttleWrapperGoldenTest` (new class), in the
`PcfGoldenTest` regime (`test/jls/hdl/board/PcfGoldenTest.java:30-60`):

- `counterWrapperMatchesTheGolden()` and `counterInfoYamlMatchesTheGolden()` -
  byte equality, version-tokenized, regenerable under `-Djls.hdl.regenerate=true`.
- `theWrapperPortListIsExactlyTheShuttleSignature()` - parses the emitted
  module header and asserts the port names, directions and widths against a
  hard-coded expected list. The signature is externally fixed; this test is
  what stops a well-meant edit from breaking every submission.
- `everyUnboundOutputBitAndOeBitIsTiedToADeclaredConstant()` - asserts no
  emitted wire is left dangling, which is a synthesis warning in LibreLane and
  a review finding at the shuttle.
- `aDesignWithNoClockBindingIsRefusedWithEveryProblemNamed()` and
  `aDesignExceedingTheIoBudgetIsRefusedAndWritesNothing()` - the
  `UnbindablePortsTest` contract: all problems in one exception, no partial
  output on disk.
- `everyBoardFormatHasAnEmitter()` in `test/jls/hdl/board/BoardPinOrderTest`
  (extend) - a totality assertion over `Board.Format.values()`, so adding a
  third format without an emitter fails the build.

## Related GitHub issues

**no issue** for the shuttle path itself; `search_issues` over `anadon/jls` for
`tapeout` returns nothing, and the registry records FEAT-040 through FEAT-044
as untracked. Adjacent:

| # | title | relationship |
|---:|---|---|
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - open. Its "a board is supported only when both halves exist for it" rule is exactly the discipline this task plus TASK-0095 follow. This task does not close it: #264's named targets are iCEstick and an ECP5 board. |
| #213 | Board-aware HDL export: emit pin-constraint files (PCF/XDC/QSF)… | informs - **closed** (`not_planned`, consolidated into #264). Supplies the `-board`/`-pins` UX and the P3 all-or-nothing prediction this task reuses unchanged. |
| #59 | HDL interoperability: staged VHDL/Verilog support | informs - open tracking issue; owns the Verilog this wraps. |

## Notes

- **The 2026 student path to silicon never asks for layout.** The shuttle
  template takes Verilog, `info.yaml`, a testbench and a docs file, and runs
  LibreLane in CI. There is no GDS to produce, no PDK to install, and therefore
  no reason for JLS to acquire a layout geometry model. Anything in this task
  that starts to look like polygons is out of scope.
- **The `Format` enum trap is the good kind.** Adding `TT_WRAPPER` breaks the
  constraint-file naming code at `src/jls/JLSStart.java:465-473` and the
  format check at `src/jls/hdl/board/PcfEmitter.java:61-64` until both handle
  it. Do not add a `default` arm.
- **Two files out of one flag.** The export path writes exactly one file today
  through a temp-and-rename (`src/jls/JLSStart.java:438-459`), with the
  constraint file derived by extension substitution (`:465-473`). Three
  outputs need that path generalized to a set, atomically - a partial shuttle
  submission is worse than none.
- **Name stability across the boundary is not JLS's to control, and the docs
  must say so.** LibreLane runs Yosys and OpenROAD, which constant-propagate,
  re-map, replicate for drive and rename. One drawn `NandGate` becomes zero or
  more cells with names JLS never emitted. The interesting version of that
  problem - importing the post-route annotation keyed against JLS stable ids -
  is a different and cheaper project, and it is not this one.
- **No new dependency.** The bill-of-materials guard and the single offline jar
  make a YAML library the wrong answer for eleven scalar fields.

## Evidence

- The board mechanism this extends, at HEAD: `src/jls/hdl/board/Board.java`
  (record `Board(name, fpga, format, pins)`; `Format` enum with the single
  constant `PCF` at `:33-45`), `src/jls/hdl/board/Boards.java` (the sole
  `icestick` entry), `src/jls/hdl/board/PcfEmitter.java:14-30,73`
  (all-or-nothing aggregation, model-port walk),
  `src/jls/hdl/board/PinBindings.java` (the external binding file).
- CLI wiring that must be generalized: `src/jls/JLSStart.java:363-478`
  (the headless export path), `:392,421-427` (board/bindings), `:438-459`
  (temp-and-rename), `:465-473` (constraint filename), `:908-916`
  (`-board`/`-pins` pair rule).
- The two hard blockers, verified at HEAD: `src/jls/hdl/HdlModel.java:28-33`
  (`Direction` is `{INPUT, OUTPUT}`); `test/resources/hdl/counter.v:21`
  (`reg [3:0] count = 4'h0;` - an initial value, not a reset).
- Shuttle template contents, `info.yaml` version and tile range, LibreLane 3.0
  (2026-03-25), SKY130 licensing, and the 11.5-18 wk path total:
  `08-views-determination.md` §1.5 and §5.
