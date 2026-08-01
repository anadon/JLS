# TASK-0097 - Solver core and timestep control

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A new headless leaf package `jls.analog` containing a working modified-nodal-
analysis transient solver: device stamps, sparse factorization, Newton-Raphson
with junction limiting, and adaptive timestep control with a stated local-
truncation-error criterion.

Precisely what changes:

- `jls/analog/package-info.java` with `@NullMarked` (required by
  `NullMarkedRatchetTest` and `PackageInfoRatchetTest`), declaring the package
  a pure numerical leaf that imports nothing from `jls.edit`, `jls.sim` or
  `jls.elem`.
- `jls/analog/MnaMatrix.java` - the sparse matrix and RHS, with an
  **index-ordered** device array built once at elaboration in stable-id order.
  Stamp order *is* accumulation order; a `HashSet` iteration reaching the
  matrix is a determinism bug, and `Circuit.elements` is a plain `HashSet`
  (`src/jls/Circuit.java:48`), so the elaborator must sort, not iterate.
- `jls/analog/LuFactorization.java` - LU with partial pivoting and a **totally
  ordered** pivot tie-break: Markowitz product, then `|value|`, then row, then
  column, with strict `>` so the lowest row wins. The permutation vector is a
  first-class output because the acceptance test asserts on it.
- `jls/analog/Newton.java` - the Newton loop with `pnjlim`-style junction
  limiting, plus the escape ladder (gmin stepping, then source stepping) with
  each ladder rung's entry and exit condition stated as a pure function of
  solver state.
- `jls/analog/Trapezoidal.java` - trapezoidal integration with a linear
  predictor and predictor-corrector LTE, and `TimestepController.java` with the
  accept/reject rule, the growth and shrink factors, and the minimum step, all
  named constants.
- `jls/analog/Devices.java` - stamps for `R`, `L`, `C`, `V`, `I`, `D` only.
- `jls/analog/LinearFastPath.java` - an elaboration-time `isLinear()` predicate
  and a factorization cache keyed on step size. **Design it in now**; it is far
  cheaper here than retrofitted around a Newton loop later, and it is what
  makes real-time audio possible at all.
- Analyses: `.op` and `.tran`. Nothing else.
- `jls/analog/AnalogSample.java` - `(long tick, double value)`, plus the
  digest-golden harness that emits `Double.doubleToRawLongBits` hex, records
  the step size `h` **explicitly** rather than inferring it from the time
  column, and puts `steps/rejects/nrIters/nrFails/points` in the header.

Done means: an RC step response and a full-wave rectifier both solve headlessly
to the documented tolerances, the run's statistics are in the emitted header,
and two runs on the same JDK produce identical bytes.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-046 | The solver core - the thing every other analog task and every analog capstone consumes. |

## Prerequisite tasks

None. The solver owns its own time internally and has no dependency on the
physical time base (TASK-0101) or on the bridge elements (TASK-0102). Stated
explicitly because sequencing claims must be real: doing this first costs zero
extra and de-risks everything downstream.

## Acceptance test

`test/jls/analog/SolverCoreTest` (new class):

- `rcStepResponseMatchesTheClosedFormAtTheCorner()` - asserts the solved value
  at the RC corner against the analytic `1 - e^-1`, with the tolerance
  **derived** from `RELTOL`/`VNTOL`/`ABSTOL` in a comment. A bare numeric
  literal tolerance is a review defect.
- `theNumericalErrorIsNonZero()` - the anti-cheat assertion: a solver returning
  the analytic answer exactly is not integrating.
- `fullWaveRectifierReachesTheExpectedPeakAndRipple()` - the second fixture,
  asserting peak and ripple bands with the junction drop stated.
- `thePivotPermutationVectorIsExactlyTheExpectedSequence()` in
  `test/jls/analog/LuFactorizationTest` - asserts the permutation on a matrix
  constructed to have a deliberate tie, so the tie-break rule is pinned rather
  than emergent. **This is the least-derisked determinism item in the whole
  analog program**; it gets its own named test on day one.
- `perDeviceStampsMatchExactly()` in `test/jls/analog/DeviceStampTest` - one
  test per device kind asserting the exact matrix and RHS entries on raw bits.
  Expected values must be written as *the same expression the kernel computes*:
  asserting `A[0][0] == 0.02` for `2.0*1e-6/1e-4` fails, because the kernel
  produces `0.019999999999999997`.
- `twoRunsInOneJvmProduceIdenticalDigests()` - the run-versus-run equality the
  tree lacks entirely today.

## Related GitHub issues

**no issue.** The registry records FEAT-045 through FEAT-049 as untracked, and
`search_issues` over `anadon/jls` for `spice OR analog OR solver` returns two
unrelated closed issues (#220, #51). Nothing in the open tracker touches
analog simulation.

## Notes

- **Why a port and not an external simulator, stated as measurement.** Two
  ngspice builds on a two-element linear RC with a pulse source differed in
  2,026 of 3,056 internal time points and 3,035 of 3,056 sample values, worst
  relative difference 5.38e-04, first divergence exactly at the pulse
  breakpoint. One binary on one machine with `.options klu` versus
  `.options sparse` differed in 987 of 1,022 rows. A byte-identical golden
  cannot be built on an external solver at any tolerance loose enough to
  survive a release. A 229-line Java MNA/Newton/LU/trapezoidal/LTE kernel
  produced the identical digest across seven JVM configurations including
  `-Xint`, `-XX:-UseFMA -XX:UseAVX=0`, serial GC and JDK 21.
- **`StrictMath` only, from the first line.** `Math.exp` and `StrictMath.exp`
  differ on 96,260 of 1,000,000 sampled inputs; swapping only `exp` changed a
  whole rectifier trajectory digest. `grep -rn StrictMath src/` returns **0**
  at HEAD, so the rule starts clean. `Math.sqrt`/`Math.abs` are permitted
  (IEEE-exact). The ArchUnit rule that enforces this is TASK-0098's, but the
  discipline starts here or TASK-0098 becomes a rewrite. Measured cost of the
  discipline: 1.66-1.73x on `exp` over the argument range a diode visits -
  about two seconds on a research-scale run, unmeasurable at teaching scale.
- **Do not absorb an op-amp from CircuitJS1.** Its `OpAmpElm` calls an
  unseeded `java.util.Random` inside the Newton step as a saturation tie-break.
  Every absorbed file needs an audit for `java.util.Random`, `Math.*`,
  wall-clock reads, hash iteration and parallel reduction, and that lint must
  be a merge gate rather than a later check.
- **Coverage placement is deliberate.** JaCoCo's `<element>PACKAGE</element>`
  rules match exactly (`pom.xml:426-514`), so a brand-new `jls.analog` is
  unfloored on arrival, like `jls.edit`. `jls.sim.analog` would fall *outside*
  the `jls.sim` rule while looking like it was inside it. Take the package to
  the full bar in the same program and add its floors then - the measured
  precedent is 86% mutation / 88% test strength once per-device stamp tests
  exist, against the repo's 80/82 gate (`pom.xml:812-813`).
- **No `double` item kind.** Analog parameters save as `String` holding their
  SPICE spelling (`String r "4.7k"`), because the format's item kinds are
  closed (`docs/file-format.md:118-140`) and a reader MUST fail on an unknown
  one. That is a format decision this task must not pre-empt by inventing a
  fifth `setValue` overload.
- **Cost honesty.** 2 weeks is the core slice; the stage it belongs to is
  3.5-5 maintainer-weeks including the linear fast path and the mandatory
  calibration measurement (measure the real weeks and re-cost everything
  downstream proportionally). FEAT-046's 17.5-26 mw band carries the rest.

## Evidence

- Cross-version and cross-configuration ngspice divergence, the seven-
  configuration Java digest stability result, and the `Math`-versus-
  `StrictMath` divergence count: `11-analog-determination.md` §1.3, §1.4.
- The five determinism controls, the pivot tie-break as the largest
  un-derisked item, and the four golden record-format rules (raw bits, explicit
  `h`, statistics in the header, expected values as the kernel's own
  expression): `11-analog-determination.md` §4.1, §4.2.
- The PIT/JaCoCo measurement (76%/79% waveform-only versus 86%/88% with stamp
  goldens) and the package-placement finding:
  `11-analog-determination.md` §4.4; repo gates at `pom.xml:355-372,426-514,812-813`.
- HEAD facts: `src/jls/Circuit.java:48` (`elements` is a `HashSet`),
  `:479-485` (`getElementsInStableOrder`); `StrictMath` 0 hits in `src/`;
  `docs/file-format.md:118-140` (closed item-kind set);
  `src/jls/elem/Element.java:344,359,374,389` (four `setValue` overloads).
