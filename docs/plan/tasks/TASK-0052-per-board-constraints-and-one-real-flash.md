# TASK-0052 - Per-board constraints and one real flash

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0051

## Deliverable

A second board with both halves - constraints and a scripted bitstream path -
plus the hardware evidence that the first board's path actually works. #264's
rule is that a board is "supported" only when both halves exist for it; at HEAD
exactly one board exists (`Boards.ALL = List.of(ICESTICK)`,
`src/jls/hdl/board/Boards.java:81`), one format exists
(`Board.Format` has a single constant `PCF("pcf")`,
`src/jls/hdl/board/Board.java:35-41`), and the flash has never been recorded.

1. **A second `Boards` entry: an ECP5 board (ULX3S class),** with its pin map
   transcribed from vendor documentation and each block commented with its
   source, in the shape the iCEstick entry already uses
   (`Boards.java:34-79`).
2. **`Board.Format.LPF`,** and an emitter selection that is a switch over
   `Board.Format.values()` with **no default arm**. `PcfEmitter.emit` guards
   with `if (board.format() != Board.Format.PCF) throw new IllegalArgumentException`
   (`src/jls/hdl/board/PcfEmitter.java:61-63`); adding a constant without a
   total dispatch reproduces exactly the registry-totality failure mode
   FEAT-001 exists to stop.
3. **`LpfEmitter`,** with the same contracts `PcfEmitter` established: sorted
   deterministic output for byte-stable goldens (the pin map is pinned to a
   naturally-sorted immutable copy in the record's compact constructor,
   `src/jls/hdl/board/Board.java:72`, `:76-84`), a header comment naming the format and
   flow (`:127`), and **all** unbindable ports aggregated into one
   `HdlExportException` (`:52-58`, `:116`) rather than failing on the first -
   the #213 P3 contract `UnbindablePortsTest` pins.
4. **The ECP5 handoff path,** as a sibling of `scripts/icestick-handoff.sh`:
   `yosys -> nextpnr-ecp5 -> ecppack -> openFPGALoader`, with the same
   all-or-nothing preflight that reports every missing tool in one pass
   (`docs/icestick-bitstream-handoff.md:31-34`), and no JLS-side bitstream logic
   (#215 H2).
5. **The recipe document covers exactly the boards with both halves.**
   `docs/icestick-bitstream-handoff.md` becomes `docs/board-handoff.md` with a
   section per board; the iCEstick section is moved, not rewritten, and a
   redirect stub is left at the old path.
6. **The flash record.** A tracked `docs/board-flash-record.md`: board, revision,
   host OS, the exact version of every tool in the chain, the date, the circuit
   flashed, and what was observed on the hardware. #215 P2 has been outstanding
   through two issue consolidations with no record; a table with one row per
   board is the artifact.
7. **The CLI surfaces the new board.** `Boards.names()` (`:116`) feeds usage
   text and the unknown-board message; `JLSStart`'s `-board` parsing
   (`src/jls/JLSStart.java:111-114`, used at `:392`) needs no change, but the
   help golden does.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-023 | the board on-ramp half of the feature: a named board going schematic to bitstream, proven once on hardware |
| FEAT-044 | the shuttle handoff is the same shape - export, constraints, external tool, recorded artifact - and it inherits this task's preflight and record discipline |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0051 | the place-and-route validation leg reads `nextpnr-ecp5` and `ecppack` from `PATH`; no workflow installs any nextpnr at HEAD, so without TASK-0051 the leg can only skip and the DoD row it satisfies cannot be checked |

## Acceptance test

`test/jls/hdl/board/LpfGoldenTest.java`, new, mirroring `PcfGoldenTest`:
`theEcp5ConstraintFileMatchesItsGolden()` against a committed
`test/resources/hdl/board/blinky_ulx3s.lpf`, and
`reEmittingIsByteIdentical()`.

`test/jls/hdl/board/BoardFormatTotalityTest.java`, new - the guard that makes a
third format cheap and a forgotten one impossible:
`everyBoardFormatHasAnEmitter()` iterating `Board.Format.values()` and asserting
a non-null emitter for each; `everyBoardsFormatIsEmittable()` iterating
`Boards.all()`.

`test/jls/hdl/board/UnbindablePortsTest`, extended:
`theEcp5EmitterAggregatesEveryUnbindablePort()` - one exception naming all of
them, not the first.

`test/jls/hdl/board/BoardPinOrderTest`, extended for the ECP5 entry.

`scripts/ecp5-handoff-selftest.sh`, run from `ci.yml` in the same slot as
`scripts/icestick-handoff-selftest.sh` (`.github/workflows/ci.yml:47-57`):
stubs the chain and asserts the preflight and control flow.

`test/jls/hdl/board/FlashRecordTest.java`, new:
`everySupportedBoardHasAFlashRecordRow()` - parses
`docs/board-flash-record.md` and asserts one row per entry in `Boards.all()`.
Adding a board without recording a flash fails the build, which is #264's
"both halves" rule expressed as a test.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | closes - this task is Stage 2 plus the outstanding Stage 1 rows (manual flash recorded, CI synth+P&R) |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - owns the Verilog the constraint file accompanies |

Recorded decisions, **closed**, cite as such and not as open: **#213**
(per-board pin constraints, whose constraint half shipped in PR #242) and
**#215** (scripted bitstream handoff), both consolidated into #264.

## Notes

- **The golden is the deliverable's teeth and its trap.**
  `test/resources/hdl/board/blinky_icestick.pcf` is byte-pinned. Any change to
  the header comment, the sort order, or the direction comment (which is one of
  the ternaries TASK-0049 converts, `PcfEmitter.java:74-76`) regenerates it.
  Sequence with TASK-0049 or regenerate twice.
- **Pin maps are transcription, and transcription is where the defects are.**
  The iCEstick entry cites the user guide and the icestorm example constraints
  in comments (`Boards.java:34-79`). Hold the ECP5 entry to the same standard:
  a comment per functional block naming the source. A wrong pin number produces
  a bitstream that flashes and does nothing.
- **`Board` is a record with a `Map<String,String>`** (`Board.java:26-27`);
  the natural pin ordering lives in the record (`:81-82`). Reuse it; a second
  ordering would make the two goldens sort differently for no reason.
- **Do not put bitstream logic in JLS.** #215 H2, restated in
  `docs/icestick-bitstream-handoff.md:8-12`. The emitters emit text; the script
  calls tools; JLS builds nothing binary.
- **The flash record cannot be faked by CI.** It is a human artifact. The test
  asserts its *presence and shape*, never its truth - say so in the test's
  javadoc so nobody mistakes a green build for a working board.

## Evidence

- `src/jls/hdl/board/Boards.java:34-79` (the transcribed iCEstick map), `:81`
  (`ALL = List.of(ICESTICK)`), `:88`, `:99`, `:116`.
- `src/jls/hdl/board/Board.java:26-27` (the record), `:35-41` (`Format` with one
  constant), `:72` and `:76-84` (`NATURAL_PIN_ORDER` and the sorted immutable
  pin map that makes goldens byte-stable).
- `src/jls/hdl/board/PcfEmitter.java:52-63` (the format guard), `:116` (the
  aggregated bind failure), `:127` (the header).
- `test/jls/hdl/board/` - `PcfGoldenTest`, `BoardPinOrderTest`,
  `UnbindablePortsTest`, `CliBoardExportTest`, `BoardFixtures`.
- `docs/icestick-bitstream-handoff.md:1-40` - the prerequisites table, the
  delegation statement, and the preflight description.
- Issue #264 "Remaining scope, per board" and "Definition of Done" - the source
  of items 1, 4, 5 and 6.
