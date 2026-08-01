# CAP-15 - HDL toolchain parity (Yosys, Verilator, Icarus, GHDL)

**Status:** proposed | **Priority:** 3 | **Marginal cost:** 12-22 mw |
**Standalone cost:** 20-34 mw

## Outcome

Everything a student can draw in JLS leaves for the four open HDL toolchains and
comes back, with hierarchy intact, and the round trip is machine-checked in CI -
so JLS stops being a place designs go and becomes a place they pass through.

## Acceptance test

**What "parity" means here.** Not language coverage and not feature equality
with a simulator. Parity is three checkable claims, and the second is the one
that matters:

1. **Compiles.** Every element type the registry declares is in a stated export
   bucket - exported, skipped as topology, or refused with a reason - with no
   fall-through, and everything in the exported bucket compiles clean under
   `iverilog`, `ghdl`, Verilator and Yosys, including decomposed designs.
2. **Agrees.** The exported design, driven by the *same* stimulus that drove the
   JLS run, produces a waveform that matches JLS's own at settling points, with
   the first divergence reported by name and time. This is a differential run,
   not a compile check, and it is what a compile oracle cannot give.
3. **Returns.** A netlist those toolchains produce imports back and **runs**: the
   importer realizes every cell the validator accepts, and a hierarchical netlist
   comes back as subcircuits rather than one flat sheet.

SEEN: a student decomposes a design into subcircuits, exports it, runs it under
Icarus and Verilator, sees the same answers, synthesizes it with Yosys, and
imports the result back into a readable schematic. Nothing in that sentence
works at HEAD past the first flat export.

CHECK: five named tests.
- `HdlPolicyTest` - claim 1's floor, and it already ships. Extend to hierarchy.
- `IverilogCompileTest`, `GhdlCompileTest` - claim 1 across the corpus,
  including hierarchical fixtures, under the shipped skip-when-absent locator.
- `VerilogDifferentialOracleTest` - claim 2. Emit a testbench from the shipped
  `-t` stimulus grammar, run it under Icarus, read the resulting waveform back,
  and compare against JLS's own dump at settling points with first-divergence
  reporting.
- `CellRealizationParityTest` - claim 3, stated as an equality: the set of cells
  the validator accepts equals the set the importer realizes. Today that is
  **19 against 5** and the test is red by construction until the gap closes.
- `HierarchyRoundTripTest` - a decomposed design exports as hierarchical modules
  and reimports as subcircuits with the same partition.

## Demo slice

The differential loop over one flat circuit, with everything else held fixed:
testbench emitter from the `-t` grammar, a waveform reader, and a signal-aligned
first-divergence comparator. **4-7 mw**, and it converts the four toolchains
from things JLS compiles against into things JLS is *checked* against - which is
the whole idea of this capstone, demonstrated on one design.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-018 | Hierarchical instance structure in the HDL IR | a decomposed design throws on export today; hierarchy is the single largest pedagogical loss at HEAD and claim 1 is false without it | required |
| FEAT-020 | Yosys JSON read: mapper parity with the validator | claim 3 is exactly this feature stated as an equality; an imported netlist that does not run is not a round trip | required |
| FEAT-023 | External toolchain differential oracle and the board on-ramp | claim 2 lives here - the toolchains armed in CI against JLS's own output | required |
| FEAT-019 | Yosys JSON write | the write half of the round trip, and the item that back-doors the gate-level backends without JLS owning a lowering pass | required |
| FEAT-004 | Shared net-partition IR with stable net naming | signals must be comparable across the boundary by name, and the names must not be a function of save order | required |
| FEAT-001 | Registry-keyed table totality discipline | claim 1's no-fall-through requirement, as a standing build rule rather than a one-time audit | required |
| FEAT-021 | Bidirectional ports in the IR and the element vocabulary | tri-state buses are ordinary in student designs and cannot be exported honestly without `INOUT` | required |
| FEAT-026 | The four-state value core with a resolution fold | the toolchains produce X and Z; a two-state comparator must either model them or declare every X a mismatch | required |
| FEAT-037 | Reset semantics, clock and domain architecture | the most common sequential idiom students write is on the validator's reject list for want of a reset model | required |
| FEAT-022 | Schematic auto-layout for imported netlists | an imported netlist that is a pile at the origin is not a returned design | required |
| FEAT-024 | Black-box HDL component and external co-simulation | the inbound direction for code JLS will never realize as elements | beneficial |
| FEAT-016 | Subcircuit type identity, VLNV and the circuit-library format | deduplicated hierarchy - one module reused N times rather than N uniquified copies | beneficial |
| FEAT-044 | Tiny Tapeout wrapper and shuttle handoff | the same export, taken one step further, is the demonstration that the toolchain path is real | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | tracking - this capstone is that issue's completion, but no single feature closes it |
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | closes |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | closes |
| #62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | overlaps - **and it appears substantially shipped at HEAD**; see Evidence, and confirm before scoping FEAT-022 |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | depends on - the same armed-toolchain surface, taken as far as a bitstream |
| #202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the differential oracle of claim 2, applied to one worked design |
| #232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - FEAT-026 migrates the value representation this capstone's X/Z comparison needs |
| #111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | depends on - "arm the HDL-sim suites" is where claim 1 and claim 2 actually run |
| #265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | depends on |
| - | (no issue) CAP-15 has no tracking issue; neither does the write half (FEAT-019) or hierarchy (FEAT-018) | no issue |

## Open decisions

1. **Uniquified or deduplicated hierarchy first?** *Recommendation: ship
   uniquified, with the emitted header saying so, then add the type digest.*
   Reason: uniquified needs no digest, no collision policy and no type identity,
   and it is accepted by all four toolchains; the honest cost is that it forfeits
   the "one module reused N times" lesson, which is a pedagogical call rather
   than a cost one.
2. **Does writing Yosys JSON create a round-trip obligation?**
   *Recommendation: publish an explicit no-round-trip claim, or fund the mapper
   increments alongside.* Reason: JLS would write a format whose reader realizes
   5 of the 19 cells it accepts, so a naive round trip fails loudly on almost
   any real design - which is fine if stated and a bug report if not.
3. **Is a batch differential oracle the live co-simulation that was refused?**
   *Recommendation: confirm the reading explicitly with the maintainer.* Reason:
   this oracle diffs two finished files offline and takes no dependency on a
   running simulation, but it is adjacent enough to the recorded refusal to be
   mistaken for it, and the mistake would be discovered mid-feature.
4. **Comparison at settling points or per delta?** *Recommendation: settling
   points, stated in the contract, not treated as a workaround.* Reason: emitted
   Verilog is delay-free while JLS carries per-element propagation delays; timing
   is already in the permitted-to-differ column with industrial precedent.
5. **Which of the four is the correctness oracle and which is the speed lane?**
   *Recommendation: build against Icarus and GHDL for correctness; add Verilator
   as a second, faster lane afterward.* Reason: Verilator is a measured speed
   upgrade on identical source, not a different verdict, and it needs lint
   suppressions the correctness lane does not.
6. **Does the export bucket ever shrink?** *Recommendation: no - the refused
   bucket must carry a reason that states what would have to exist first.*
   Reason: that discipline already ships at `HdlExporter.java:460-477` and it is
   what keeps claim 1 honest as the element set grows.

## Kill criteria

- K1. If closing the validator-to-realizer gap requires a bit-level lowering
  pass inside JLS rather than word-level realization, stop: gate-mapped import
  was measured at roughly 4x the element count and 4.3x the wall clock of
  word-mapped import for the same design, so the cheap-looking route makes every
  later capstone slower.
- K2. If the differential comparison diverges outside the timing column - that
  is, on a value at a settling point - and the cause is a JLS semantic rather
  than a testbench artifact, the parity contract's permitted-to-differ column is
  wrong and must be re-adjudicated before claim 2 is asserted anywhere.
- K3. If hierarchy goldens have to be regenerated a second time because stable
  net naming did not land first, stop and land it: the regeneration is the
  measured cost of skipping the ordering, and it recurs for every emitter added
  afterward.
- K4. If any of the four toolchains cannot be installed on a supported platform's
  CI lane, the claim narrows in writing to the platforms where it runs, rather
  than being asserted from one platform's green check.
- K5. If the export policy ever regains a fall-through - an element type in no
  bucket - the totality lint has stopped working and claim 1 is void until it is
  restored.

## Evidence

All verified at `b54e6ee`.

- The realization gap, exactly: `src/jls/hdl/yosys/CellValidator.java:58-68`
  lists **19** accepted cell types; `src/jls/hdl/imp/NetlistImporter.java:235-248`
  realizes **5** (`$not`, `$and`, `$or`, `$xor`, `$mux`), everything else
  falling to the `default` arm at `:249`.
- Hierarchy is refused with a reason at `src/jls/hdl/HdlExporter.java:460-477`:
  "the HDL model has no module-instantiation statement, so hierarchy cannot be
  rendered". `grep -rn InstanceStatement src/` returns **0**;
  `src/jls/hdl/HdlModel.java:891` carries a single `moduleName` and
  `grep -c "void visit" src/jls/hdl/HdlModel.java` returns **11**, none of which
  instantiates a module.
- The export policy is now total: `HdlExporter.java:429-477` carries EXPORTED,
  SKIPPED, TOPOLOGY and a REJECTED map with four entries and their reasons,
  pinned by `test/jls/hdl/HdlPolicyTest.java`. Commit `b54e6ee` is titled
  "fix(hdl): make the export policy total over the element registry" - **the
  corpus's non-total policy finding is closed and must not be re-cited as open**.
- `INOUT` is absent: `src/jls/hdl/HdlModel.java:28-33` is `{INPUT, OUTPUT}`.
- The compile oracles already ship: `test/jls/hdl/IverilogCompileTest.java`,
  `test/jls/hdl/GhdlCompileTest.java`, both over `test/jls/hdl/ToolLocator.java:46`.
  `grep -rl ProcessBuilder src/` is **0**; `test/` is **15**.
- Auto-layout appears shipped: `src/jls/hdl/layout/HeuristicLayeredLayouter.java`
  (553 lines) with `LayoutGraph`, `LayoutMetrics` and `LayoutInvariants`, wired at
  `src/jls/hdl/imp/NetlistImporter.java:104`. FEAT-022's band should be re-checked
  against this before it is funded, and #62 checked for closability.
- Cost band basis: `09-format-adoption-plan.md` §3 rows 1-2 (Yosys JSON write
  3-4 wk, hierarchy 4-6 wk uniquified), W3.1-W3.3 (the oracle, 5-8.5), W5.2-W5.3
  (mapper increments, 5-7, plus hierarchy import 2-3).
- Do not restate: `docs/hdl-support-research.md` owns the toolchain landscape,
  `docs/vcd-interop.md` owns the waveform profile and the recorded co-simulation
  refusal, `docs/parity-contract.md` owns what may differ, `docs/batch-interface.md`
  owns the `-t` grammar and its stability promise, `docs/capability-roadmap/`
  owns the programs.
