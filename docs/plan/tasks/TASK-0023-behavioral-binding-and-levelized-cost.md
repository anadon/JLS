# TASK-0023 - Measure the behavioral binding and the levelized cost at scale

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0022

## Deliverable

Two of the nine unmeasured quantities in `docs/machine-calibration.md` §6 stop
being estimates: **§6.4** (behavioral events per retired instruction, modeled at
12, never measured) and **§6.6** (levelized ns/node, two irreconcilable figures
for the same pass, taken at 2.6x too few slots). These are L0(c) and L0(d) of
`docs/virtual-hardware-parity.md:418-500`.

1. **A behavioral accumulator machine, event-counted on a real bus.**
   A ~200-line element that fetches, decodes and retires a small
   accumulator ISA in one `react()`, wired to a real `jls.elem.Memory` over
   real `Put`s, driven by an internal `jls.elem.Clock`. Deliverable: retired
   instructions, events fired, and events per retired instruction, over at
   least three programs whose dynamic instruction mixes differ (a load/store
   loop, an arithmetic loop, a branch-dense loop), each reported with its
   census and its clocking regime.
2. **The levelized harness re-run at CPU scale.** `keystone-c`'s levelized
   model measured 522 slots (225 logic elements + 297 nets). Re-run it at
   ~1,346 and ~1,400 slots with the same activity variants (100 / 50 / 25 /
   10 percent live), and **instrument both pass timings inside one run** so
   the 4.32 ns/node and 3.10 ns/node figures come from a single measurement
   instead of two sections of one document that never reconcile them
   (`docs/machine-calibration.md:274-320`).
3. **Both results written into `docs/machine-calibration.md`.** §6.4 and §6.6
   are replaced by measurements; §2.6's three quoting rules gain the measured
   values; §6.11's map marks L0(c) and L0(d) discharged. The prose edit is
   this task's, not TASK-0024's - TASK-0024 owns the document's coherence
   after all of L0 has landed, this task owns these two numbers.
4. **The harnesses committed under `test/`, not thrown away.** Both are
   `@Tag("slow")` JUnit classes so the numbers are re-derivable by anyone,
   and so TASK-0026 has something to ratchet against.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-009 | Two of the six criteria: every published ns/node figure states node count and pass count, and the behavioral tier stops being costed off a modeled constant |
| FEAT-030 | The engine constant-factor program's ceiling claim is the levelized number. A 12-20 mw program justified by an unreconciled 1.39x is not justified |
| FEAT-031 | The fidelity toggle's whole value proposition is the behavioral/structural cost ratio. This measures the numerator |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0022 | This task reuses the event counter and the reconciled clocking regime TASK-0022 builds. An events-per-instruction figure quoted against an unreconciled 3.2x events-per-cycle constant (121.5 / 243.1 / 245.5 / 388.4, `docs/machine-calibration.md:250-273`) is not a measurement |

## Acceptance test

`test/jls/sim/BehavioralBindingMeasurementTest.java`, new, `@Tag("slow")`:

- `accumulatorMachineReportsEventsPerRetiredInstruction()` - builds the machine
  programmatically, runs each of the three programs under a `BatchSimulator`
  subclass overriding `afterEvent`, and asserts (a) the final accumulator and
  memory contents equal the values an in-test reference loop computes, so the
  machine is known to be correct before its cost is quoted, and (b) the printed
  record carries circuit census, clocking regime, retired count, event count
  and the derived ev/instr - the §7.3 step-6 discipline, asserted rather than
  hoped for.
- `everyProgramReportsItsOwnMix()` - asserts three distinct ev/instr figures
  are produced and none is reported without its program name. This is the
  anti-averaging clause; a single blended number is the error §2.5 exists to
  prevent.

`test/jls/sim/LevelizedScaleMeasurementTest.java`, new, `@Tag("slow")`:

- `bothPerNodeCostsComeFromOneRun()` - runs the levelized pass at 522, ~1,346
  and ~1,400 slots and asserts both the plane-array pass timing and the
  100%-activity pass timing are recorded **from the same invocation**, with
  their ratio printed. Fails if either is absent - the defect being fixed is
  that the source printed two costs and reconciled neither.
- `nodeCountAndPassCountAreReportedWithEveryFigure()` - asserts the emitted
  record contains node count, pass count and slots-per-element, so no ns/node
  figure can escape this suite bare.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - the 5.1x BitSet-versus-plane-array gap in the levelized model is the strongest existing evidence for #232, and this task is what makes it a measurement at CPU scale rather than at 522 slots |

No issue covers the behavioral tier or the levelized re-measurement. The
registry records the whole parity/machine layer (FEAT-030 through FEAT-039) as
having no issue; that is accurate for this task.

## Notes

- **`Element` is sealed and this is the trap.** `src/jls/elem/Element.java:17-18`
  is `sealed ... permits DisplayElement, LogicElement, Wire`, and
  `src/jls/elem/LogicElement.java:17-21` is sealed over 24 permits. A
  behavioral element **cannot** be written in `test/`. The measurement class
  must live in `src/jls/elem/` and be added to `LogicElement`'s permits list -
  a one-line product-code change in a task whose layer "ships no product code".
  Say so out loud rather than discovering it in week one.
- **It must NOT be added to `ElementRegistry.ALL`.** That list
  (`src/jls/elem/ElementRegistry.java:38-77`, 35 entries at HEAD) is the frozen
  save-tag set. An entry there is a format commitment. Consequence: the
  measurement circuit **cannot be a `.jls` file** and must be constructed in
  Java by the harness, since element constructors take `(Circuit)`.
  TASK-0038's construction verbs do not exist yet; the harness builds by hand.
- **TASK-0001/TASK-0002's registry totality lint will see the new type.** An
  element in `jls.elem` that is deliberately absent from `ALL` is exactly the
  shape that lint is built to flag. Coordinate: the lint needs an explicit,
  named, single-entry exclusion with its reason, not a silent skip.
- **Use an internal `Clock`, never a `-t` vector.** `SigSim.initSim` pre-posts
  every stimulus transition during elaboration; `Clock.initSim` posts exactly
  one event and `react` self-reposts one
  (`docs/virtual-hardware-parity.md:437-451`). The regime is the axis of the
  3.2x discrepancy, so the wrong one silently produces a number that cannot be
  compared to anything a boot would do.
- **Report warm and including-init timings separately.** They differ by 1.76x
  and `docs/machine-calibration.md:1024-1027` records that conflating them
  produced this study's worst errors.
- **`stackdepth=512` if profiling.** At the JFR default roughly 30% of samples
  are truncated and mis-attributed, enough to invert the section-2.3 ranking
  (`docs/machine-calibration.md:1048-1062`).

## Evidence

- `docs/machine-calibration.md:890-921` - §6.4 and §6.6 as they stand, each
  with its named cheapest experiment.
- `docs/machine-calibration.md:274-320` - §2.6, the levelized model, the 522
  slots, the two per-node costs and the three quoting rules.
- `docs/machine-calibration.md:250-273` - §2.5, the clocking-regime spread.
- `docs/machine-calibration.md:1016-1047` - §7.2/§7.3, the three harnesses and
  the procedure whose step 6 this task's assertions encode.
- `docs/virtual-hardware-parity.md:418-500` - L0, with (c) and (d) verbatim.
- `src/jls/elem/Element.java:17-18`; `src/jls/elem/LogicElement.java:17-21` -
  the two sealed declarations, read at HEAD.
- `src/jls/elem/ElementRegistry.java:38-77` - `ALL`, 35 entries at HEAD.
- `src/jls/sim/Simulator.java:269` (the `afterEvent` no-op) and
  `src/jls/sim/BatchSimulator.java:140` (the existing override) - the
  instrumentation seam, which needs no change to `jls.sim`.
