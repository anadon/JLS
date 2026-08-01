# TASK-0026 - The simulation budget and allocation ratchet

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** TASK-0022, TASK-0023,
TASK-0025

## Deliverable

A standing CI gate that turns a simulation regression into a build failure, in
four assertions of deliberately different kinds. The kinds matter: one is an
equality, two are bands, one is byte-identity.

1. **Events per clock cycle as a hard EQUALITY on committed fixtures.** Not a
   band. The event count for a given circuit, stimulus and clocking regime is a
   deterministic integer, and any change to it is a semantic change that must
   be seen and consciously re-baselined. Applies to the TASK-0025 calibration
   fixture and to `test/fixtures/riscv-sum1to10.jls`.
2. **A nanoseconds-per-event band.** A ratio-based assertion in the shape
   `test/jls/SpatialIndexTest.java:218-244` already uses - measured against a
   same-run baseline rather than an absolute wall-clock number, because CI
   runner speed varies by more than any honest engine regression. Declared
   ceiling only; the floor is never asserted, so an improvement never fails.
3. **A bytes-allocated-per-event band.** Measured via
   `com.sun.management.ThreadMXBean.getThreadAllocatedBytes` on the simulation
   thread, which needs no agent and no dependency. This is the assertion #232
   is actually about: ~62% of allocation is `BitSet` and `long[]` churn on
   values that are all 64 bits or narrower.
4. **The whole existing golden corpus asserted byte-identical across any
   engine change.** Not a new corpus - an aggregating gate over the goldens
   that already exist: `BatchSimulationGoldenTest`, `ElementSimulationGoldenTest`,
   `SequentialGoldenTest`, `SimulationSemanticsRegressionTest`,
   `VcdExportGoldenTest`, `RiscvCpuGoldenTest`. The point is that FEAT-030's
   constant-factor work operates under one named gate rather than under six
   tests somebody must remember to run.
5. **The declared numbers live in one file, not six.** A
   `test/fixtures/simulation-budget.properties` carrying fixture path, clocking
   regime, expected event count, ns/event ceiling and bytes/event ceiling per
   fixture, with the ratchet reading it. Re-baselining is then a reviewable
   one-file diff with a stated reason, which is the only mechanism that makes
   "ratchets down and never up" enforceable by a human.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-009 | Criteria 4 and 5 in full - the equality, the two bands, and the byte-identity gate over the golden corpus |
| FEAT-030 | This is the gate the 12-20 mw constant-factor program operates under. Without it, "semantics-preserving" is an intention rather than a checked property |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0022 | The event-count equality and the ns/event ceiling are its output. A band invented rather than measured ratchets against nothing |
| TASK-0023 | Supplies the levelized ceiling the ns/event band is read against, and the behavioral figure that says which side of the boundary a cost belongs to |
| TASK-0025 | The equality is asserted "on committed fixtures". Until the CPU-scale fixture is tracked there is only the 34-cycle `riscv-sum1to10` golden to assert on, and 34 cycles does not exercise the queue |

## Acceptance test

`test/jls/sim/SimulationBudgetRatchetTest.java`, new:

- `eventCountPerCycleIsExactlyTheDeclaredValue()` - loads each fixture named in
  `simulation-budget.properties`, runs it under a `BatchSimulator` subclass
  overriding `afterEvent`, and asserts `assertEquals` on the total event count
  and on events-per-cycle. Failure message prints declared, actual and the
  delta, and names the properties file as the place to re-baseline.
- `nanosecondsPerEventStayUnderTheDeclaredCeiling()` - best-of-N with the first
  two reps discarded (the §7.3 warm-up discipline), asserting only the ceiling.
- `bytesAllocatedPerEventStayUnderTheDeclaredCeiling()` - same shape, reading
  `getThreadAllocatedBytes` before and after `runEventLoop`.
- `everyFixtureInThePropertiesFileExists()` - the anti-vacuity clause. Asserts
  the file declares at least two fixtures and that each resolves. Without it a
  properties file emptied by a bad merge would make the whole gate pass
  silently, which is the exact failure mode this task exists to prevent.

`test/jls/GoldenCorpusByteIdentityTest.java`, new:

- `everyGoldenInTheCorpusIsByteIdentical()` - enumerates the six golden classes
  by name, runs each, and asserts the aggregate. Also asserts the enumerated
  list equals the set of `*GoldenTest` classes discoverable under `test/jls/`,
  so a newly added golden joins the gate automatically instead of silently
  sitting outside it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | depends on - #232 proposes replacing the signal representation. The bytes-per-event band is how anyone will know whether the replacement worked, and the event-count equality is how anyone will know it changed no semantics |

No issue covers the ratchet itself. The registry records the whole engine and
parity layer (FEAT-030 onward) as having no issue; #232 covers the value
representation only, not the queue, the zero-delay closure or the gate.

## Notes

- **The equality is the load-bearing assertion; the bands are the cheap ones.**
  A timing band on a shared CI runner is noisy and will be loosened over time.
  An event count is exact, is a semantic invariant, and cannot be loosened
  without somebody typing a new number into a tracked file.
- **The band must be a ceiling only.** An assertion that fails when the engine
  gets faster trains people to re-baseline reflexively, which destroys the
  gate.
- **The clocking regime must be recorded per fixture.** Events per cycle has
  been measured at 121.5, 243.1, 245.5 and 388.4 on element-for-element
  identical circuits depending on whether the drive was an internal `Clock` or a
  `-t` vector through `SigSim` (`docs/machine-calibration.md:250-273`). A
  declared event count without its regime is not a baseline, it is a coin flip.
- **`SigSim.initSim` pre-posts every stimulus transition.** A `-t`-driven
  fixture therefore allocates during elaboration, not during the loop, and its
  bytes-per-event figure will not mean what a `Clock`-driven one means. Prefer
  `Clock`-driven fixtures for the allocation band, or record the regime and
  keep the two populations separate.
- **`afterEvent` is the seam and it needs no change to `jls.sim`.** The no-op
  is `src/jls/sim/Simulator.java:269`; `BatchSimulator` already overrides it at
  `:140` for trace accumulation. The ratchet's subclass must live in package
  `jls.sim` to reach the protected loop.
- **Which lane runs this is TASK-0016's decision, not this task's.** A
  CPU-scale fixture is not a required-fast-lane workload; the required gate at
  HEAD is 141 s. Do not smuggle a long-run policy in here.
- **Do not put a floor on queue depth or on dup-suppression counts.** They are
  legitimate targets of FEAT-030's work; pinning them would gate the program
  this ratchet exists to protect.

## Evidence

- `src/jls/sim/Simulator.java:269` - the `afterEvent` no-op;
  `src/jls/sim/BatchSimulator.java:140-180` - the existing override.
- `docs/machine-calibration.md:250-273` - §2.5, the clocking-regime spread and
  the rule never to quote an events-per-cycle figure without it.
- `docs/machine-calibration.md:1028-1047` - §7.3, the warm-up and reporting
  procedure the timing assertions follow.
- `test/jls/SpatialIndexTest.java:218-244` - the in-tree precedent for a
  same-run ratio timing assertion rather than an absolute one.
- Existing goldens verified present at HEAD: `test/jls/BatchSimulationGoldenTest.java`,
  `test/jls/ElementSimulationGoldenTest.java`, `test/jls/SequentialGoldenTest.java`,
  `test/jls/SimulationSemanticsRegressionTest.java`,
  `test/jls/VcdExportGoldenTest.java`, `test/jls/RiscvCpuGoldenTest.java`.
- `test/fixtures/` at HEAD holds three tracked `.jls` fixtures; `.gitignore:8-10`
  is the exemption that lets a fourth be added.
- BRIEF §2 - ~48% of loop time in `PriorityQueue` plus `HashSet` dup-check
  bookkeeping; ~62% of allocation in `BitSet` and `long[]` churn. These are the
  two quantities the bands are aimed at.
