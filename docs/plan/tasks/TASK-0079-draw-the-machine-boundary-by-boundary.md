# TASK-0079 - Draw the machine and bring it up boundary by boundary

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0038, TASK-0065, TASK-0070, TASK-0076

## Deliverable

The method, the skeleton and the first two boundaries green. Not the whole
machine - see the cost note in Notes.

1. **`machines/cpu-rv32/` in-tree**, one `.jls` per boundary plus a top-level
   `rv32-soc.jls` that instantiates them: `alu.jls`, `regfile.jls`, `decode.jls`,
   `loadstore.jls`, `csr.jls`, `clint.jls`, `cpu.jls`. Each boundary is a
   `SubCircuit` instance carrying TASK-0065's fidelity attribute, so each can be
   run behaviorally (bound to `jls.mach`) or structurally (the drawn contents).

2. **A written element census, committed as `machines/cpu-rv32/CENSUS.md`**, one
   row per boundary: element count by registry tag, net count, and the events per
   cycle the boundary contributes. It is a **budget with a test**, not prose - the
   census file is read by the acceptance test and a boundary that drifts past its
   declared count fails. Target for the whole machine: ~580 central logic
   elements, honest band 400-870 (Sv32 ~750).

3. **Construction goes through TASK-0038's verbs, not through generated text.**
   The datapath and control are authored in the editor where structure is
   pedagogically load-bearing; bus fan-out, the decode table and the CSR file are
   generated through `CircuitOp` verbs. Nothing in this task emits `.jls` text and
   re-parses it. `riscv/build_cpu.py` is the prior art to read and the idiom to
   retire - it is a Python netlist emitter whose output JLS re-parses.

4. **The bring-up ledger, `machines/cpu-rv32/BRINGUP.md`**, a table of
   `boundary | status | harness verdict | date | commit`. The rule the ledger
   encodes: **at every commit, whatever is drawn is checked**. A boundary is not
   "drawn" until its row says the harness passed at its own boundary.

5. **Two boundaries actually green in this task: `alu` and `regfile`.** ALU
   because its total input width is small enough for exhaustive comparison at
   reduced widths and a seeded 10^6-vector sample at 32 bits; regfile because
   `RegisterFile` collapses the whole bank into one element with independent
   RA/WA ports and is therefore the cheapest boundary to state and the most
   expensive one to get wrong.

6. **The `RegisterFile` behavioral golden**, which does not exist:
   `test/jls/ElementSimulationGoldenTest` lists it exempt with a recorded reason.
   The regfile boundary cannot be trusted as a differential counterparty while
   the element under it has no model test. Landing it is part of this task.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-038 | The machine itself, and the method by which the remaining boundaries are added. Everything else in the feature is repetition of this task's discipline. |
| FEAT-031 | The fidelity toggle's first real consumer. Six sub-boundaries plus the CPU boundary is where a per-instance binding stops being a design and becomes a thing with a failure mode. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0038 | Programmatic circuit construction verbs | The generated half of the machine is built through the op vocabulary. Without it the only path is emit-text-and-reparse, which bypasses validation, undo and the collaboration observers, and produces circuits the editor could not have made. |
| TASK-0065 | The saved per-instance fidelity attribute | Every boundary in the tree carries this attribute. It is the file-level datum that makes "run this subcircuit behaviorally" expressible at all. |
| TASK-0070 | The machine package and its reference runner | The behavioral binding on the other side of each boundary is `jls.mach`. A boundary with no counterparty cannot be brought up; it can only be drawn. |
| TASK-0076 | Write-mask input on memory | `lb`/`lh`/`lbu`/`lhu`/`sb`/`sh` are unimplementable without byte lanes, and the minimum SoC's 16550 is three byte addresses on a 32-bit bus. The load/store boundary reads a memory port only that task creates. |

## Acceptance test

`test/jls/machines/Rv32BringupTest`:
- `censusFileMatchesTheDrawnBoundaries()` - parses `CENSUS.md`, loads each
  boundary `.jls`, and asserts the per-tag element count equals the declared row
  exactly. A boundary that grows silently fails here.
- `everyBoundaryDeclaresAFidelityBinding()` - every `SubCircuit` instance in
  `rv32-soc.jls` carries TASK-0065's attribute with a value in the closed set.
- `aluBoundaryMatchesTheReferenceExhaustivelyAtReducedWidth()` - all
  2^16 operand pairs at 8 bits x 8 bits for every ALU op, drawn versus
  `jls.mach`, asserting equality of the result and of every flag.
- `aluBoundaryMatchesTheReferenceOnASeededSample()` - 10^6 seeded 32-bit vectors
  plus declared corners (widths 1/31/32/33, all-ones, all-zeros, min/max signed),
  **with the seed printed in the failure report**.
- `regfileBoundaryMatchesTheReference()` - the write-read-same-cycle case, the
  x0-reads-zero case, and simultaneous RA/WA on the same address.
- `bringupLedgerHasNoGreenRowWithoutAPassingHarnessName()` - parses
  `BRINGUP.md` and asserts every row marked green names a test method that
  exists. This is the test that stops the ledger becoming aspiration.

`test/jls/ElementSimulationGoldenTest` gains the `RegisterFile` model golden and
its exemption row is deleted.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | **closes (partly)** - this is direction 1 and 2 of #202: promote the differential suite into `test/` and make the CPU a curriculum artifact. Direction 3, the HDL-export oracle, is FEAT-023's. |
| 62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | depends on - #202 records that the generated `.jls` "places elements at nominal overlapping coordinates", so a machine built by verbs is unreadable until layout exists. The editor-authored half of this task is the workaround; the generated half needs #62. |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | overlaps - a ~580-element machine is the fixture that makes #232 measurable; do not couple the bring-up to the fix. |
| 73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability | overlaps - a worked CPU is the canonical sample circuit #73 wants. |

## Notes

- **Cost honesty.** FEAT-038's band is 12-26 mw and this task plus TASK-0080 is
  4 weeks. The residual is per-boundary drawing labor - decode, load/store, CSR,
  CLINT, then the CPU boundary - which is repetition of the method this task
  establishes and is deliberately not re-decomposed into task ids. Do not read
  the 2-week figure as "the machine costs two weeks".
- **The shipped fixture is not this machine.** `test/fixtures/riscv-sum1to10.jls`
  is a single-cycle RV32I with no CSRs, no MMU, no CLINT and no console.
  MEASURED at HEAD by `grep -c "^ELEMENT"` and a tag histogram: 1,038 `ELEMENT`
  records, of which 810 are `WireEnd`, leaving **228 non-wire-end elements** -
  43 `Mux`, 43 `Constant`, 34 `Splitter`, 34 `AndGate`, **32 `Register`**, 9
  `Binder`, 8 `NotGate`, 5 `XorGate`, 5 `Extend`, 4 `Adder`, 3 `ShiftRegister`,
  3 `OrGate`, 3 `Memory`, 1 `InputPin`, 1 `Decoder`. It is a good regression
  golden and a bad starting point: its register file is 32 discrete `Register`
  elements, and the native `RegisterFile` measures 6.94 events per cycle against
  18.00 for a mirrored-`Memory` pair and 114.53 for a flip-flop farm
  (`docs/machine-calibration.md:347-349`). **Draw the regfile as one
  `RegisterFile`.**
- **The two elements that make this machine cheap are the two that cannot be
  exported.** `HdlExporter.REJECTED` names `RegisterFile` and `FieldExtend` with
  reasons (`src/jls/hdl/HdlExporter.java:459-477`), alongside `Memory` and
  `SubCircuit`. So **no HDL round-trip or formal-path claim may be made about
  this machine** until FEAT-018 and the mapper increments land. Say it in
  `CENSUS.md`; do not discover it during a demo.
- **A construction path already exists and is not the op layer.**
  `new AndGate(circuit)` -> `Circuit.addElement` (`src/jls/Circuit.java:342`) ->
  `Util.partition` (`src/jls/Util.java:145`) works today with no text round-trip,
  because element constructors are public (the registry's factories are method
  references). It bypasses the `CircuitOp` layer that undo and collaboration
  observe. Use it for nothing that ships.
- **Clock discipline.** `riscv/build_cpu.py`'s header records the working
  convention: one `Clock` drives every rising edge, and data-memory writes are
  gated to commit in the clock-low phase "after combinational signals have
  settled". That is a workaround for level-sensitive RAM writes; with
  synchronous-write `Memory` (issue #199, shipped) the gate is unnecessary. Do
  not carry the workaround forward.
- **The fixture is 9,360 lines and this machine will be larger.** TASK-0016's
  large-fixture policy applies before the tree lands, not after.

## Evidence

- `docs/virtual-hardware-parity.md` L8 (`:980-1030`) - the bring-up method, the
  "build no bespoke forge" rule, the correction that a construction path already
  exists, the byte-lane blocker and the `HdlExporter` export gap.
- `docs/parity-contract.md` §2.2 - the bound boundary and its two
  implementations; §5.1 - the four observation points.
- `src/jls/hdl/HdlExporter.java:428-435` (the 22 exported classes), `:459-477`
  (the four refusals with reasons).
- `src/jls/elem/RegisterFile.java:141-154` - independent RA/WA/WD/WE ports and
  the `C` pin appended last.
- `test/jls/RiscvCpuGoldenTest.java:40-58` - the existing fixture harness: 34
  steps, a generated `-t` clock vector, half-period 1000.
- `riscv/build_cpu.py:1-19` - the generator's own description of the datapath,
  the clocking convention and the write-gating workaround.
- `docs/machine-calibration.md:347-353` - the measured marginal events per cycle:
  native `RegisterFile` **+6.94**, mirrored-`Memory` pair **+18.00**, 31-flip-flop
  farm **+114.53**; `:435` - `L` ~580 central, band 400-870 (nommu), ~750 (Sv32);
  `:400-411` - the `RegisterFile` / `FieldExtend` export gap stated at HEAD.
