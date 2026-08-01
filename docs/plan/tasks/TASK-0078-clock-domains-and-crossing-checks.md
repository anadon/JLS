# TASK-0078 - Clock domains and crossing checks

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0007, TASK-0077

## Deliverable

A clock stops being an ordinary wire. Five parts, each independently reviewable.

1. **`jls.elem.Clocked`, a capability interface in the `Timed` idiom.**
   `Input clockPin()` and `Edge activeEdge()` (`RISING`, `FALLING`, `LEVEL_HIGH`),
   implemented by `Register`, `StateMachine`, `RegisterFile` and sync-write
   `Memory`. It returns the **named** pin, looked up by name, never an index -
   see the trap in Notes. `Memory.clockPin()` returns null when
   `isSyncWrite()` is false, and the interface documents that as the only
   nullable case.

2. **`Clock` gains a saved `phase` attribute**, `0 <= phase < cycle`, absent
   meaning 0, with a `checkPhase(int cycle, int phase)` constraint string on the
   `checkCycleTime`/`checkOneTime` pattern (`src/jls/elem/Clock.java:52-71`) so
   the dialog and the loader share one rule. `initSim` posts the first rising
   transition at `(cycle - one + phase) mod cycle` instead of `cycle - one`.

3. **`src/jls/timing/`, a new headless package.**
   - `ClockDomains.infer(Circuit)` -> a `DomainMap` of `LogicElement` to
     `ClockId` plus a `List<ClockDerivation>`. Roots are `Clock` outputs and
     top-level `InputPin`s carrying a declared clock role. Each `Clocked` sink's
     clock pin is walked backward over the partition from TASK-0007. Pure
     combinational cone to a root => `GATED` (enable recorded); through a
     `Clocked` element => `GENERATED` (ratio and phase recorded); two roots =>
     `MUXED`; no root => `UNDRIVEN`.
   - `ClockReport` - the clock list (root, period, phase, derivation, sink
     count) - is emitted **before** any violation. The domain model is the thing
     that is wrong first, and a CDC tool with a wrong domain model produces
     confidently wrong violations.
   - `CrossingCheck.run(DomainMap, Partition)` -> `List<Crossing>`. A crossing is
     a `Clocked` sink in domain B whose data cone contains a `Clocked` source in
     domain A != B, or an undomained top-level input, with no recognized
     synchronizer. Recognition: a chain of >= 2 `Register`s in the destination
     domain **including the fanout condition** - the intermediate q net drives
     nothing but the next flop. A multi-bit crossing through per-bit flops is
     reported `UNSAFE_BY_CONSTRUCTION`, not merely unsynchronized.
   - **The reset rule**: a reset net (TASK-0077's `CLR`/`PRE` pin) whose driver
     is in a different domain from the sink is itself a crossing.

4. **The IR carries the domain.** `HdlModel.RegisterStatement` and
   `StateMachineStatement` gain a `ClockId clockDomain` field, and `HdlModel`
   gains `List<ClockSpec> clocks()` (root net, period, phase, derivation).
   `HdlExporter.buildStatement` populates it from the `DomainMap`. The emitters
   print it as a header comment only in this task; SDC emission is not here.

5. **Waivers and the batch surface.** A `waive` attribute on the destination
   element, keyed by source stable id and a required reason string, saved in the
   `.jls`. One new `FlagSpec` row in `JLSStart.FLAGS` (`src/jls/JLSStart.java:758`)
   - `-cdc` writes `ClockReport` then the crossing list to stdout and exits per
   `docs/batch-interface.md` §1: 0 when clean, 1 with a `jls: error:` line when
   an unwaived crossing exists.

**Silent by default on a single-clock design.** Zero roots or one root and no
undomained input => zero output beyond the clock list. A checker that talks in
the common case is one students learn to ignore.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-037 | The domain half. `Clocked` is the refactor everything else in the feature stands on: every consumer needs to ask "which pin is the clock" and today must know each element's port order. |
| FEAT-041 | The physical checks need to know a net is a clock. A clock net on a data pin, and a clock routed through a breadboard rail, are package-layer errors that cannot be stated without a domain map. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0007 | Extract the net-partition walk into its own package | The backward clock-cone walk and the forward data-cone walk are graph queries over the net partition. It must be **the** partition, not a third private copy - `Circuit.finishLoad` (`src/jls/Circuit.java:1300`) and `HdlExporter.UnionFind` (`src/jls/hdl/HdlExporter.java:1161-1226`) already build it twice. |
| TASK-0077 | Honest reset on the register element | The reset-crossing rule reads a reset pin that does not exist at HEAD; `Register`'s only inputs are `D` and `C` (`src/jls/elem/Register.java:230-231`). Without it that rule has no input. |

## Acceptance test

`test/jls/timing/ClockDomainInferenceTest`:
- `everyDerivationKindIsProducedByAFixture()` - one fixture per `ClockDerivation`
  kind (root, gated, generated, muxed, undriven); asserts the recorded ratio for
  a divide-by-two is period x 2 and phase + the divider's `propDelay`.
- `clockListIsEmittedBeforeAnyViolation()` - asserts report ordering on a
  circuit that has both.
- `undrivenClockIsReportedHereNotOnlyOnExport()` - **fails at HEAD**: the only
  report of an unconnected clock today is a warning inside the HDL exporter
  (`src/jls/hdl/HdlExporter.java:640-643`), reachable only through `-export`.

`test/jls/timing/CrossingCheckTest`:
- `twoFlopChainIsRecognizedOnlyWhenTheIntermediateNodeFansOutToOne()` - the
  two-flop chain passes; the same chain with the intermediate q net also driving
  a third sink is reported. This is the condition students get wrong and it is
  the test that makes the recognizer worth having.
- `multiBitCrossingThroughPerBitFlopsIsUnsafeByConstruction()` - asserts the
  verdict enum, not merely a non-empty list.
- `singleClockDesignProducesNoCrossingOutput()` - run over
  `test/fixtures/riscv-sum1to10.jls`, which has exactly one clock source.
- `aWaiverRequiresAReasonAndSurvivesARoundTrip()` - a waiver with an empty
  reason is a load error; a waived crossing round-trips through save/load.
- `resetCrossingIsACrossing()` - a `CLR` driven from another domain is reported.

`test/jls/elem/CapabilityInterfaceTest#capabilityInterfacesMatchExpectedSetsForAllRegisteredTypes()`
is extended with a `CLOCKED` tag set, so a new element type cannot ship without
a clock decision.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | clock domains, phase and crossing checks | **no issue** |
| 199 | Memory: optional synchronous (clock-edge) write mode for glitch-safe RAM in combinational datapaths | informs (closed) - it created the fourth clock sink and the conditional clock pin this task must handle |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - `Clocked` is a new row in the capability-interface program #78 opened; the registry half already shipped |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | overlaps - the three edge detectors this task consolidates are `int` comparisons and must stay `int`; do not couple the consolidation to a value-representation change |

## Notes

- **The clock pin index is a magic number in three places and it must not become
  a fourth.** `Register`'s clock is `inputs.get(1)` only because `init` happens to
  add `D` then `C`, repeated for four orientations
  (`src/jls/elem/Register.java:230-231,240-241,250-251,260-261`); the exporter
  reads exactly that (`src/jls/hdl/HdlExporter.java:640` takes `ins.get(1)`).
  `RegisterFile`'s `C` is appended **after** a variable number of RA/WA/WD/WE
  pins (`src/jls/elem/RegisterFile.java:141-154`), so an index is not even
  expressible. `clockPin()` looks up by name.
- **`Memory`'s clock pin is conditional and deliberately appended last**
  (`src/jls/elem/Memory.java:193-196`, "appended last so the pre-#199 input
  indices are unchanged"). Preserve that; do not reorder inputs.
- **Three private edge detectors get deleted**: `Register.currentC`
  (`Register.java:698`, tested at `:772,:783`, updated at `:794`),
  `StateMachine.oldClock` (`StateMachine.java:657,737-785`), `Memory.lastClock`
  (`Memory.java:996,1374,1390`). `RegisterFile` has its own as well. Every
  element golden in `test/jls/ElementSimulationGoldenTest` and
  `test/jls/SequentialGoldenTest` must stay byte-identical - the consolidation is
  semantics-preserving by construction and any diff is a bug.
- **`phase` needs no FORMAT bump and that is the hazard.**
  `docs/file-format.md` §9 says adding an attribute to an existing element type
  needs no bump because "older readers silently ignore unknown attribute names".
  A phase-shifted clock therefore loads as phase 0 in an older reader and the
  circuit quietly changes meaning. Record it in the file-format doc as a known
  silent-drop case; it is the same defect class FEAT-002 covers.
- **`Clock` sits in the combinational palette drawer** -
  `src/jls/edit/Palette.java:173` registers it under `Group.COMBINATIONAL`, whose
  javadoc reads "Combinational building blocks and the clock"
  (`Palette.java:47-48`). Moving it is a `test/jls/edit/PaletteContractTest`
  change and is **out of scope here**; note it and leave it.
- **The exporter cannot tell a clock from a port**: `HdlExporter.java:547` is
  `if (el instanceof InputPin || el instanceof Clock) { return; }`. That line is
  correct for net construction and must stay; the domain annotation is added
  alongside it, not by changing it.
- **`jls.timing` is a new package**: it needs a `package-info.java` with
  `@NullMarked` (`test/jls/PackageInfoRatchetTest#everyPackageHasPackageInfo`,
  `test/jls/NullMarkedRatchetTest`) and a per-package JaCoCo floor in `pom.xml`
  alongside the `jls.sim` / `jls.elem` / `jls.collab.op` rules
  (`pom.xml:449-515`). Born floored, per issue #159's discipline.

## Evidence

- `src/jls/elem/Timed.java:25-40` - the capability-interface idiom and its stated
  purpose ("call sites `instanceof`-check the capability instead of the base
  class branching on concrete leaf types").
- `src/jls/elem/Clock.java:42-71` (the one-string-two-surfaces constraint
  pattern), `:74-77` (`cycleTime`, `oneTime`, `orientation` are the whole saved
  state - there is no phase).
- `docs/simulation-semantics.md:362-370` - the first rising edge is at
  `cycle - one`, normatively; phase must be introduced without moving it for
  `phase = 0`.
- `src/jls/hdl/HdlModel.java:401-461` - `RegisterStatement`, whose clock field
  doc reads "1-bit clock; a literal clock never ticks".
- `docs/capability-roadmap/lf-08-clocks-and-cdc.md` §"The capability" C1-C3 - the
  design this task implements; §"Size and risk" prices C3 at 3-4 wk against P5's
  ERC at 3-5, and the structural floor C1+C2+C3 at 8-11 wk.
- `docs/capability-roadmap/AMENDMENT.md:151` - P13 "Clock, reset and domain
  architecture", 13-18 wk marginal, structural floor 8-11 wk.
- `docs/batch-interface.md:33-48` - the three exit statuses the `-cdc` flag must
  honor.
