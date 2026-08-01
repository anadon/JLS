# FEAT-023 - External toolchain differential oracle and the board on-ramp

**Status:** proposed | **Cost:** 6-12 mw | **Owner program:** P5 |
**Spine rank:** -

## Capability delivered

The open toolchains become witnesses rather than destinations. Simulation,
synthesis and place-and-route tools run against JLS's own emitted artifacts in
CI, so a change to an emitter that produces syntactically valid but semantically
wrong output fails a build instead of reaching a student. The same chain,
continued one step, takes a drawn circuit to a bitstream on a named board with
its pin constraints emitted rather than hand-written, walked once on real
hardware and recorded.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-07 | required | a shuttle submission is a synthesis flow whose correctness nobody can check by reading; the oracle is the check |
| CAP-08 | required | an imported core's behavior in JLS is claimed equal to its behavior elsewhere, and that claim needs a counterparty |
| CAP-09 | required | verifying a design you did not write means having an independent implementation to compare against, and the toolchains are the cheapest one |
| CAP-15 | required | parity with four toolchains is asserted by running all four |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-018 | the goldens the external compilers check must include hierarchy, or the oracle certifies only the flat subset a real design is not |
| FEAT-007 | the oracle lanes are long, tool-dependent and cross-platform; without explicit timeouts and a long-run lane they either time out the required gate or get quietly disabled |
| FEAT-021 | beneficial - a board design's real interfaces are bidirectional, and a constraint file that cannot say so constrains the wrong thing |
| FEAT-019 | beneficial - the synthesis tool reads JLS's netlist directly rather than through a re-parse of emitted HDL |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0051 | Arm the external toolchains in CI | the synthesis, place-and-route and fast-simulation tools are not yet run against shipped goldens |
| TASK-0052 | Per-board constraints and one real flash | the board table has one entry and the documented path has never been walked end to end and recorded |
| TASK-0044 | Hierarchical emitters and their goldens | shared with FEAT-018: the goldens are what the external tools consume |
| TASK-0100 | The external-simulator differential corpus | the analog counterpart of the same discipline, sharing the corpus mechanism and the tolerance method |

## Acceptance criteria

- The synthesis, place-and-route and fast-simulation tools each run against
  shipped goldens in CI, alongside the existing compile-oracle legs, under the
  shipped skip-when-absent locator so an absent tool skips rather than fails.
- A differential run exists whose failure mode is a *behavioral* disagreement,
  not merely a compile error: JLS's own waveform and the external simulator's
  are compared at settling points and the first divergence is reported with the
  signal name and the time.
- Constraints are emitted per named board with a golden each, and the board
  table has more than one entry so the format dimension is exercised.
- The circuit-to-bitstream path is documented and has been walked once on real
  hardware, with the result recorded in tree - the artifact, the tool versions
  and the outcome.
- The tool versions the oracle was validated against are pinned and recorded, and
  a version bump is a deliberate change with a regenerated record.
- The export menu item exists in the editor, so the path a course would use is
  not command-line only.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | closes |
| #202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - that issue's differential-oracle half is this feature; its worked-example half is not |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | overlaps - the same external simulators, used as a component rather than as a witness; the transport decision must be the same in both |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope | informs |

## Design notes

Two properties of the existing arrangement are load-bearing and must survive.
First, the oracle is **batch**: two finished files are compared offline. No part
of this feature puts a subprocess in the shipped source tree, and the single
self-contained offline jar is untouched - the external tools are test-scope
dependencies and a documented recipe. Second, the skip-when-absent locator is
already shared by the compile tests, and every new leg must use it rather than
re-inventing a path search.

Comparison must be at settling points, not per delta. Emitted HDL is delay-free
while JLS carries per-element propagation delays, so a per-delta comparison
would report a disagreement on every edge. This is not a workaround: the parity
framing this study inherits already places all timing in the permitted-to-differ
column, with industrial precedent.

One adjacency needs an explicit confirmation rather than an assumption. A prior
recorded decision rejects **live** co-simulation with an external simulator.
Batch differential comparison is a different thing and does not conflict with
it, but the two are close enough to be mistaken for each other; confirm the
reading before the lane is built, and write the distinction into the lane's own
documentation.

The board half is the cheapest demonstration in the plan because its mechanism
already ships in miniature: a board table, a constraint emitter over the same
port walk the HDL emitters use, and an externally supplied, all-or-nothing
validated pin binding. A second board format is a sibling emitter, not a
program.

## Risks

- **Toolchain rot.** Every armed tool is a version-compatibility surface that
  moves without asking. Pin versions, record them, and treat a bump as a change
  with a regenerated record; an oracle whose goldens regenerate on every upstream
  release is a maintenance event at bus factor 1.
- **Green by skipping.** A skip-when-absent lane that skips everywhere is
  indistinguishable from a passing lane. At least one platform must have the
  tools present and the check required, and the skip count should be reported.
- **The hardware step is once, not continuous.** A real flash cannot be a CI
  gate. It is a recorded walk with a date and a tool set; say so, and do not let
  it decay into an implied continuous claim.

## Evidence

- Verified at HEAD `addc6c5`: the board mechanism ships -
  `src/jls/hdl/board/Board.java` (159 lines) with a format enum,
  `Boards.java` (125) whose built-in table contains exactly **one** entry,
  `PcfEmitter.java` (199) and `PinBindings.java` (98).
- Verified at HEAD: the skip-when-absent idiom is shared, not copied -
  `test/jls/hdl/ToolLocator.java` documents replacing five per-test copies of a
  path search; `test/jls/hdl/GhdlCompileTest.java:34-36` is the usage shape.
- Verified at HEAD: the existing external legs are compile oracles only -
  `test/jls/hdl/GhdlCompileTest.java` and `IverilogCompileTest.java` analyze and
  compile the goldens; there is no leg that compares behavior.
- Verified at HEAD: six workflow files exist under `.github/workflows/`; the
  lane structure this feature needs is FEAT-007's, not this feature's, which is
  why the prerequisite is real rather than stylistic.
- `09-format-adoption-plan.md` Wave 3 costs the differential loop at 5-8.5
  maintainer-weeks (testbench emitter 2-3, waveform reader and comparator 2-4,
  fast-simulator lane 1-1.5) and records the settling-point requirement and the
  live-co-simulation adjacency as an explicit question to confirm.
- Cost band basis: Wave 3 plus the board on-ramp items, against
  `docs/capability-roadmap/AMENDMENT.md`'s P5 band.
- Do not restate: `docs/icestick-bitstream-handoff.md` owns the board recipe,
  `docs/parity-contract.md` owns what may and may not differ,
  `docs/vcd-interop.md` owns the waveform profile and the live-co-simulation
  decision.
