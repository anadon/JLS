# TASK-0083 - Draw the ternary CPU

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0061, TASK-0082, TASK-0038

## Deliverable

The drawn balanced-ternary machine, against a stated census, brought up against
the emulator by the same boundary-by-boundary discipline TASK-0079 establishes.

1. **`machines/cpu-t3/` in-tree**, one `.jls` per boundary plus a top-level
   `t3-soc.jls`: `talu.jls` (the ternary ALU), `balu.jls` (the binary-interop
   ALU), `regfile.jls`, `decode.jls`, `loadstore.jls`, `br3.jls` (the three-way
   branch comparator and target select), `packunpack.jls` (the BET boundary),
   `cpu.jls`. Each carries TASK-0065's fidelity attribute.

2. **`machines/cpu-t3/CENSUS.md`**, per boundary, read by the acceptance test.
   Declared target: **~620-760 logic elements, band 450-950**, and **500-620
   events per retired instruction**. That is the RV32 skeleton plus three things
   the binary machine does not have - the BET pack/unpack path, the `BR3`
   comparator, and **two ALUs** rather than one. Any row that drifts past its
   declared count fails.

3. **The word is a 32-bit bus and the machine is drawn on ordinary elements.**
   Under binary-encoded ternary a 16-trit bus **is** a 32-bit bus, so `Memory`,
   `RegisterFile`, `Splitter`, `Binder`, `Mux` and the gates carry it with zero
   change and zero format version. The N-ary element family is consumed where it
   makes the drawing legible and small - the `MIN`/`MAX`/`INV`/`CYCP`/`CYCN`/
   `LIT` cells of the ternary ALU and the sign extraction feeding `BR3` - not as
   a substrate for the buses.

4. **The `27 -> 1` register file.** One `RegisterFile` element with independent
   RA/WA ports, `r0`-reads-zero by the element's own convention, addressed by the
   6-bit BET encoding of a 3-trit register field. Not 27 `Register` elements.

5. **Boundary bring-up, two green in this task: `talu` and `br3`.** The ternary
   ALU because it is where the ISA's arithmetic actually lives and its reduced
   widths admit exhaustive comparison; `br3` because a three-way branch with two
   equal offsets, a misaligned target, and a source register holding an illegal
   BET lane is where a hand-drawn comparator is wrong and an emulator is not.

6. **`machines/cpu-t3/BRINGUP.md`**, the same ledger shape as TASK-0079's, with
   the same rule: a boundary is not drawn until its row names a passing harness
   verdict.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-039 | The machine. The ISA and the emulator are a specification and a program; this is the thing that makes the capstone a drawing you can open. |
| FEAT-029 | The N-ary family's first real consumer, and the only one that will find its holes. A ternary element set with no drawn machine over it is a palette. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0061 | The N-ary element family | The ternary ALU instantiates element types only that task registers. **The fallback is real and should be stated rather than used as a gate**: every one of those cells is expressible as a small binary subcircuit over the two BET planes, at roughly 6-10x the element count for the ALU boundary and a correspondingly larger census. If TASK-0061 slips, the machine is still drawable; it is bigger and harder to read. |
| TASK-0082 | The ternary reference emulator and assembler | Bring-up compares each boundary against the emulator's `RetireRecord` stream and each program is produced by the assembler. Both are that task's output; there is no other counterparty for a custom ISA. |
| TASK-0038 | Programmatic circuit construction verbs | The decode table, the BET lane fan-out and the two ALUs' cell arrays are mechanical repetition and must be generated through the op vocabulary rather than emitted as text and re-parsed. |

## Acceptance test

`test/jls/machines/T3BringupTest`:
- `censusFileMatchesTheDrawnBoundaries()` - per-tag element counts equal the
  declared rows exactly.
- `taluBoundaryMatchesTheEmulatorExhaustivelyAtOneAndTwoTrits()` - all 9 and all
  81 cases per ternary op, drawn versus `jls.mach.t3`.
- `taluBoundaryMatchesTheEmulatorOnTheSampledTier()` - 10^6 seeded 16-trit
  vectors from TASK-0081's corpus, seed in the failure report.
- `br3TakesTheCorrectArmForEachSignIncludingEqualOffsets()` - negative, zero and
  positive, plus the two-arms-equal encoding and the misaligned-target trap.
- `anIllegalBetLaneReachesTheSameTrapInBothImplementations()` - inject `00` into
  each of the 16 lanes of `rs1` and assert the drawn machine and the emulator
  produce the same trap, not merely the same non-answer. **This is the test that
  the whole BET encoding exists for.**
- `eventsPerRetiredInstructionAreWithinTheDeclaredBand()` - counted, not timed,
  against the `CENSUS.md` band. An events-per-instruction budget is a
  hard number; a wall-clock one is not.
- `bringupLedgerHasNoGreenRowWithoutAPassingHarnessName()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the drawn JLS-T3 machine and its boundary bring-up | **no issue.** The entire ternary program is untracked. |
| 62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | depends on - the generated half of this machine is unreadable in the GUI without layout, exactly as #202 records for the RV32 generator. |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | informs - a ~700-element machine at 500-620 events per instruction is the fixture that makes the hot-path work measurable; do not couple bring-up to the fix. |

## Notes

- **This machine cannot be exported to HDL and must say so in `CENSUS.md`.**
  `HdlExporter.REJECTED` names `Memory`, `SubCircuit`, `RegisterFile` and
  `FieldExtend` with reasons (`src/jls/hdl/HdlExporter.java:459-477`); the T3
  datapath uses at least three of the four. No HDL round-trip or formal-path
  claim about the drawn T3 machine is admissible until FEAT-018 and the mapper
  increments land.
- **Two ALUs is a design statement, not an accident.** C's `&`, `|`, `^`, `<<`,
  `>>` on `unsigned` cannot be cheaply synthesized from balanced-ternary
  primitives - a binary shift is not a base-3 shift. Providing the binary ALU is
  the same mixed-radix boundary the ISA puts at `LDB`/`STB`, moved into the
  datapath, and **drawing that symmetry explicitly is better teaching than hiding
  it**. Every real ternary processor design does this or does without C.
- **`NEG` must be drawn as a plane swap** - a wire crossing between the two BET
  planes, one cycle, no carry propagation - not as complement-plus-one. It is one
  of the four T-null models precisely because the wrong implementation is
  plausible and passes casual tests. The drawing is the demonstration.
- **The clock story is TASK-0078's, not this task's.** One `Clock` root, one
  domain, no crossings. If the pack/unpack path is later clocked separately, that
  is a crossing and the checker must see it; do not introduce a second clock here
  to solve a timing problem.
- **Do not draw a 27-element register bank.** MEASURED at HEAD: native
  `RegisterFile` costs **+6.94** marginal events per cycle, a mirrored-`Memory`
  pair **+18.00**, and a 31-flip-flop farm **+114.53**
  (`docs/machine-calibration.md:347-349`). At 500-620 events per instruction the
  flip-flop farm alone would be a fifth of the budget.
- **Interaction rate, so nobody is surprised.** At the behavioral tier a QDOS
  echo path is ~10^3 instructions and echo is roughly 0.004 s per character; the
  structural tier is roughly 0.18 s per character - a slow 1970s terminal, and
  still usable. This is the one machine in the plan where the behavioral tier is
  comfortably interactive, because the guest is four orders of magnitude smaller
  than Linux.

## Evidence

- `docs/machine-calibration.md:347-349` - the measured marginal events per cycle
  for the three register-file constructions; `:435` - the element-count band
  method this census follows.
- `src/jls/elem/RegisterFile.java:141-154` - independent RA/WA/WD/WE ports and
  the trailing `C` pin.
- `src/jls/elem/Memory.java:1224,1234` - `DENSE_CAPACITY_LIMIT = 1 << 22` and the
  `bits <= 64` dense-store predicate: a 16-trit word is 32 bits and fits.
- `src/jls/hdl/HdlExporter.java:459-477` - the four refusals with their reasons.
- `docs/virtual-hardware-parity.md` L8 (`:980-1030`) - the boundary-by-boundary
  method and the "build no bespoke forge" rule this task reuses.
- `docs/parity-contract.md` §3.1 - the ordered per-retired-instruction state
  delta the bring-up compares.
