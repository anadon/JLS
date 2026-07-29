# CAP-07 - Tape out a student design on a shuttle

**Status:** proposed | **Priority:** 15 | **Marginal cost:** 11.5-18 mw |
**Standalone cost:** 14-24 mw

## Outcome

A design drawn in JLS exports as a shuttle-ready submission - the fixed
`tt_um_*` top-level signature, a pin binding, `info.yaml` and a flow config -
which an open ASIC shuttle accepts, builds in its own CI, and returns as a
physical chip the student can hold.

## Acceptance test

SEEN: the student draws a design, runs
`jls -export -board tinytapeout -bindings my.pins design.jls -o tt/`, and gets
`tt/src/project.v` whose top module is `tt_um_<name>` with the mandatory
`ui_in`, `uo_out`, `uio_in`, `uio_out`, `uio_oe`, `ena`, `clk`, `rst_n` port
list, plus `tt/info.yaml` and the flow config. The directory is pushed to a fork
of the shuttle template; the shuttle's own CI runs the open ASIC flow and
publishes GDS and a layout image; the student opens the produced GDS in KLayout.
Months later a chip arrives and the design runs on the demo board.

CHECK: three named tests.
- `TinyTapeoutWrapperGoldenTest` - the emitted top module's port list is
  byte-identical to the fixed shuttle signature, every port width matches, and
  `info.yaml` validates against the shuttle's schema version, tile count within
  the declared range. Golden-pinned like the shipped emitter goldens.
- `TinyTapeoutResetTest` - every sequential element in the exported design is
  reset by `rst_n`, and the export refuses, with a diagnostic naming the element,
  any design relying on an initial value instead. This is the falsification
  guard: at HEAD `Register` has no reset attribute and no reset input, and
  `test/resources/hdl/counter.v:21` emits `reg [3:0] count = 4'h0;`, which FPGA
  synthesis honors and ASIC synthesis discards.
- `BidirectionalPortExportTest` - a design with a bidirectional port exports with
  `uio_oe` driven per bit. Fails today: `HdlModel.Direction` is `{INPUT, OUTPUT}`
  and there is no `InOutPin` in the registry, so `uio_oe` has nothing to bind to.

## Demo slice

The wrapper emitter and the binding file alone, over a design with no
bidirectional ports and an explicit reset already drawn: `tt_um_*` top module,
`info.yaml`, the flow config, and a documented submission recipe walked once by
hand. 1.5-2 mw of the band, and it is structurally the same problem the shipped
board-constraint emitter already solves.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-044 | Tiny Tapeout wrapper and shuttle handoff | the fixed top-level signature, `info.yaml` and the documented submission path - this capstone's spine | required |
| FEAT-037 | Reset semantics, clock and domain architecture | the shuttle wrapper makes `rst_n` mandatory and ASIC synthesis discards initial values | required |
| FEAT-021 | Bidirectional ports in the IR and the element vocabulary | `uio_oe` cannot be bound without `INOUT` end to end | required |
| FEAT-018 | Hierarchical instance structure in the HDL IR | a design worth taping out is decomposed, and a decomposed design must export without flattening | required |
| FEAT-004 | Shared net-partition IR with stable net naming | one partition pass feeds the wrapper, the constraint file and the emitters | required |
| FEAT-019 | Yosys JSON write | the annotation path back from the flow keys the produced cells against what JLS emitted | beneficial |
| FEAT-023 | External toolchain differential oracle and board on-ramp | the same on-ramp that flashes a board proves the export synthesizes before it is submitted | required |
| FEAT-047 | Physical time base and the nominal real-time scalar | a clock period submitted to a flow is a physical quantity, not a unitless integer | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) CAP-07 has no tracking issue | no issue |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | depends on |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps |
| #202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | informs |
| - | (no issue) the wrapper emitter, the reset model and the `INOUT` layer | no issue |

## Open decisions

1. **Does JLS own the flow or only the handoff?** Recommend handoff only: emit
   the wrapper, the metadata and the recipe, and let the shuttle's own CI run the
   flow. Reason: the 2026 student path never asks the student for layout; the
   flow runs in the shuttle's CI for free, and orchestrating it locally breaks
   the single offline jar.
2. **The layout view.** Recommend one paragraph and a link to a free layout
   viewer, at zero maintainer-weeks, rather than a layout canvas. Reason: JLS's
   geometry model has no polygon type at all; a layout reader was re-derived at
   12-19 weeks against a free, better tool.
3. **Reset model shape.** Recommend a first-class reset input and attribute on
   the sequential elements rather than a synthesized reset wrapper. Reason: a
   wrapper makes the exported reset invisible in the drawing, which is exactly
   the lesson the capstone is supposed to teach.
4. **What the differentiator claim is.** Recommend claiming *name stability
   across a boundary JLS does not control* - an annotation-import problem keyed
   against stable ids - rather than "JLS did the technology mapping". Reason: the
   shuttle's flow re-maps, replicates for drive and renames, so one drawn gate
   becomes cells JLS never emitted.
5. **Perishability.** Recommend pinning the template revision and the schema
   version in the emitter and testing against the pin. Reason: the shuttle
   template and its metadata schema move between shuttles, and an unpinned
   emitter silently emits last year's format.

## Kill criteria

- K1. If the shuttle program's template or metadata schema changes in a way the
  emitter cannot track within a fraction of the wrapper's own band each cycle,
  the handoff is perishable maintenance and should be documented as a recipe
  rather than shipped as an emitter.
- K2. If `TinyTapeoutResetTest` cannot be made green without a reset model - that
  is, if the reset work slips past its program - the capstone is blocked, not
  merely late, because ASIC synthesis discards the initial values every JLS
  design relies on today.
- K3. If bidirectional support cannot be delivered end to end, the capstone is
  restricted to designs that use no bidirectional pins, which must be stated in
  the acceptance test rather than discovered at submission.
- K4. If more than a small number of student designs pass CI export but fail the
  shuttle's own checks, the export is not the acceptance boundary and a local
  pre-check must be funded before the capstone is claimed.

## Evidence

- The reset gap, verified at `b54e6ee`: `src/jls/elem/Register.java` has no reset
  attribute and no reset input (`grep -niE "reset" src/jls/elem/Register.java`
  returns only propagation-delay and display resets), and
  `test/resources/hdl/counter.v:21` emits `reg [3:0] count = 4'h0;`.
- The `INOUT` gap, verified at `b54e6ee`: `src/jls/hdl/HdlModel.java:27-33`
  defines `Direction` as `{INPUT, OUTPUT}`; `grep -rn "InOutPin" src/` returns
  nothing.
- The wrapper is structurally the shipped constraint-binding problem:
  `src/jls/hdl/board/PinBindings.java` (98 lines) and
  `src/jls/hdl/board/PcfEmitter.java` (199 lines), with the end-to-end recipe
  precedent in `docs/icestick-bitstream-handoff.md` - "JLS does not build
  bitstreams. It emits HDL and a constraint file and stops there."
- The opt-in external-tool test idiom the CI checks follow:
  `test/jls/hdl/GhdlCompileTest.java:34-36`.
- The costed item list totaling 11.5-18 weeks, the "never asks the student for
  layout" finding, the re-derived layout-reader cost, and the name-stability
  reframe: `08-views-determination.md` §1.5 and §5.
- The wrapper's own emitter estimate and its dependency on hierarchy:
  `09-format-adoption-plan.md` W7.1 and W2.1.
- Do not restate: `docs/standards-adoption/06-fpga-constraint-formats.md` owns
  the constraint-format landscape; `docs/capability-roadmap/` owns program
  boundaries; `ARCHITECTURE.md` owns the layering.
