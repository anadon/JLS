# CAP-08 - Import and run a third-party core

**Status:** proposed | **Priority:** 10 | **Marginal cost:** 14-24 mw |
**Standalone cost:** 26-42 mw

## Outcome

A published open-source CPU core that JLS did not write - a PicoRV32-class RV32
design - is imported through a synthesis-tool netlist, opens as a readable
hierarchical schematic, and executes its own test program inside JLS with the
results checked against the core's own reference.

## Acceptance test

SEEN: the student runs the core's own synthesis script to produce a netlist,
then `jls -import core.json -o core.jls`. The import prints a summary naming
every cell it realized and every construct it could not, with zero unresolved
problems for this core. The file opens in the editor as a hierarchy that still
carries the module names the core's author wrote, laid out readably rather than
piled at the origin. `jls -b -t firmware.t core.jls` runs the core's own test
program and the retired-instruction trace matches the reference implementation's.

CHECK: four named tests.
- `ThirdPartyCoreImportTest` - importing the pinned core netlist yields zero
  import problems, and the realized element counts per cell type match a
  committed manifest. Fails today by construction: at HEAD the mapper realizes
  `$not`, `$and`, `$or`, `$xor`, `$mux` and constants, and reports everything
  else, including `$add`, `$dff`, `$dlatch`, `$tribuf`, the reductions, `$bmux`,
  `$mem`, bit-level slices and hierarchy instances, as import problems.
- `ThirdPartyCoreHierarchyTest` - the imported circuit contains one subcircuit
  per source module, named as the author named it, rather than one flat sheet.
  Fails today: the importer rejects any non-`$` cell with "hierarchy (subcircuit)
  import is not built in this increment - flatten the design in Yosys and
  re-import".
- `ThirdPartyCoreExecutionTest` - the imported core executes its firmware to
  completion and its retired-instruction trace equals the reference's, compared
  per retired instruction rather than by final register state.
- `ThirdPartyCoreBudgetTest` - the run completes inside a declared wall-clock
  budget on the tracked calibration fixture's machine, with the budget divided by
  measured engine constants rather than asserted.

## Demo slice

Bit-level mesh synthesis plus the `$add`/`$dff` mapper increments, over a small
published module rather than a whole CPU: any slice or concatenation is refused
wholesale today, and that single gate blocks four of the remaining mapper
increments. 3-5 mw, and it turns "the importer refuses real designs" into "the
importer imports real designs".

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-020 | Yosys JSON read: mapper parity with the validator | the importer must realize every cell the validator already accepts - this capstone's spine | required |
| FEAT-022 | Schematic auto-layout for imported netlists | a core imported as a pile at the origin is not readable, and readability is the point | required |
| FEAT-002 | Fail-loud loader and attribute dispatch | a third-party file must not lose an attribute silently; the import summary is the contract | required |
| FEAT-018 | Hierarchical instance structure in the HDL IR | the imported hierarchy needs somewhere to land and something to round-trip back out | required |
| FEAT-016 | Subcircuit type identity, VLNV and circuit-library format | one module reused N times must import as one definition with N instances | required |
| FEAT-017 | Shared and parameterized subcircuit definitions | otherwise the same module imports as N deep copies that then diverge | required |
| FEAT-026 | Four-state value core with a resolution fold | a real core has tri-state buses and undriven nets; first-driver-wins misexecutes them | required |
| FEAT-036 | Byte lanes on `Memory` and capacity as a byte budget | the core's memory does sub-word writes, and its image is a byte budget | required |
| FEAT-037 | Reset semantics, clock and domain architecture | a third-party core is reset-driven; an initial value is not the same thing | required |
| FEAT-034 | Retirement-indexed parity harness and `RetireRecord` | `ThirdPartyCoreExecutionTest` compares per retired instruction, not by final state | required |
| FEAT-009 | Measurement gate and tracked calibration fixture | `ThirdPartyCoreBudgetTest` divides by measured constants and needs a tracked fixture | required |
| FEAT-019 | Yosys JSON write | round-tripping the imported core back out is how the import is proven lossless | beneficial |
| FEAT-023 | External toolchain differential oracle and board on-ramp | the external toolchain is the oracle that says the import behaves like the source | required |
| FEAT-024 | Black-box HDL component and external co-simulation | the parts of a core that cannot be realized can still run, in the external simulator | beneficial |
| FEAT-015 | Headless `CircuitOp` layer | import constructs a circuit from a program and must not need a `Graphics` | required |
| FEAT-038 | The drawn structural RV32 machine | a drawn machine of known shape and known cost is what an imported third-party core is compared against for size, speed and behavior | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | closes |
| #62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | closes |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | overlaps |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps |
| #202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps |
| #232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | depends on |
| - | (no issue) CAP-08 itself, the parity harness, the calibration fixture and the reset model | no issue |

## Open decisions

1. **Which core is the named target.** Recommend naming one published
   PicoRV32-class core and pinning its revision and its synthesis script in the
   fixture set. Reason: an unnamed "a third-party core" makes the acceptance test
   unfalsifiable, and an unpinned one makes it perishable.
2. **Import order.** Recommend bit-level mesh synthesis before any further cell
   increment. Reason: it is measured as the single highest-leverage import task
   and it gates four of the eight remaining increments; real designs slice
   constantly and today any slice is refused wholesale.
3. **Uniquified or deduplicated hierarchy first.** Recommend uniquified first -
   one module per instance, no type identity - and the structural digest as a
   later increment. Reason: that is the sequencing the format work already
   proposes, and deduplication depends on an identity scheme that does not exist
   yet.
4. **What "runs" means.** Recommend per-retired-instruction comparison against
   the core's own reference, not final-state comparison. Reason: final-state
   comparison passes designs that are wrong throughout and right at the end,
   which is the same defect that makes vector grading weak in CAP-06.
5. **Where an unrealizable construct goes.** Recommend reporting it as a named
   import problem and, where the co-simulation seam exists, offering the
   black-box component - never a silent mis-mapping. Reason: the shipped importer
   already holds that line and it should not be relaxed under schedule pressure.

## Kill criteria

- K1. If the imported core cannot execute its own firmware inside a wall-clock
  budget a person will wait for, on the tracked calibration fixture's machine,
  the capstone's execution tier is struck and it ships as import-and-read only.
- K2. If bit-level mesh synthesis cannot be delivered inside its band, the
  remaining mapper increments should not be started: they are gated on it and
  would deliver an importer that still refuses real designs.
- K3. If realizing a cell requires relaxing the "no silent mis-mapping" rule the
  shipped importer holds, stop: a core that imports wrong is worse than a core
  that refuses to import.
- K4. If the chosen core's synthesis script or netlist schema drifts faster than
  the pinned fixture can be maintained, re-pick the core rather than chase it.

## Evidence

- The gap is realization, not validation, verified at `b54e6ee`:
  `src/jls/hdl/imp/NetlistImporter.java:38-46` names the accepted-but-unrealized
  set (`$add`, `$dff`, `$dlatch`, `$tribuf`, the reductions, `$bmux`, hierarchy
  instances, bit-level slices and concatenations, width mismatches), and
  `NetlistImporter.java:234-258` is the switch that realizes exactly
  `$not`/`$and`/`$or`/`$xor`/`$mux` and reports the rest.
- Hierarchy is refused with a named diagnostic:
  `src/jls/hdl/imp/NetlistImporter.java:227-232` - "hierarchy (subcircuit) import
  is not built in this increment - flatten the design in Yosys and re-import".
- Auto-layout is further along than the corpus assumes: `src/jls/hdl/layout/`
  ships `HeuristicLayeredLayouter` (553 lines), `LayoutGraph`, `LayoutInvariants`
  and `LayoutMetrics`, and `NetlistImporter.java:13-16,102-112` already builds a
  `LayoutGraph`, solves it and realizes the placed result. Issue #62 is
  substantially landed at HEAD though the issue is open; FEAT-022's remaining
  cost should be re-measured before it is funded.
- The validator side already accepts far more than the mapper realizes:
  `src/jls/hdl/yosys/CellValidator.java` (276 lines) with the memory-cell
  decision at `CellValidator.java:229-249`.
- The scanner half of the black-box path ships but no production code consumes
  it: `src/jls/hdl/scan/VerilogHeaderScanner.java` (1,489 lines) and
  `VhdlEntityScanner.java` (891 lines) are referenced by tests
  (`test/jls/hdl/scan/`, `test/jls/hdl/imp/ImportPipelineTest.java`) and by no
  `src/` package outside `jls.hdl.scan` at `b54e6ee`. FEAT-024's remaining cost
  is the co-simulation contract, not the scanner.
- Bit-level mesh synthesis is the highest-leverage task and gates four of eight
  remaining increments: `09-format-adoption-plan.md` W1.5; the mapper increments
  and hierarchy import: W5.2 and W5.3; uniquified-before-deduplicated:
  §"Ship uniquified first".
- The "gap moved from validation to realization" correction and the note that
  `$dff` currently has a committed test asserting rejection: `BRIEF.md` §13.
- Do not restate: `docs/hdl-support-research.md` and
  `docs/standards-adoption/` own the interoperability landscape;
  `docs/simulation-semantics.md` owns execution semantics.
- **Cost reconciliation.** Marginal band 14-24 mw. Its 13 required features
  sum to 110-171 mw and its 3 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
