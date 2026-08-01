# TASK-0103 - Device and transistor models

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0097, TASK-0099

## Deliverable

The semiconductor device set a teaching lab needs, each with a stated parameter
tier, a stamp, a limiter and a golden - plus the inspector that tells a student
*before* simulation starts whether the vendor file they downloaded will load.

Precisely what changes:

1. **`jls/analog/models/`**, new subpackage of the headless `jls.analog` leaf:
   - `DiodeModel.java` - Spice3f5 level 1. **12 of the 88 parameters ngspice's
     card accepts**; size against SpiceSharp's 15, not against ngspice.
   - `BjtModel.java` - Gummel-Poon **only**. Ebers-Moll is not a second model:
     absent `VAF/VAR/IKF/IKR` makes the inverse Early voltages zero, `q1 = 1`,
     `qb = 1`, and the equations degenerate to pure Ebers-Moll. Building both
     would waste 3-6 mw. Expose Ebers-Moll as a documented week-1 parameter tier
     (`IS`, `BF`, `BR`), ~28 of 143 parameters tiered by week.
   - `MosfetLevel1.java` - Shichman-Hodges, ~14 of 36 parameters.
   - `JfetModel.java` - the cheapest semiconductor and the one the electret
     condenser front end is built out of.
   - `SwitchModel.java` - `S` (voltage-controlled) and `W` (current-controlled).
   - `MosCommon.java` - bulk diodes, Meyer capacitances, limiting and
     temperature, factored **so MOSFET level 3 drops in beside it later without
     touching level 1**.
2. **`jls/analog/Limiting.java`** - the four limiters (`pnjlim`, `fetlim`,
   `limvds`, `limvgs`/`limvbs`) ported from the 156-line Modified-BSD limiting
   apparatus, carrying its attribution and license notice in the file header and
   in `NOTICE`. **The `icheck` protocol must be preserved verbatim**: a limited
   step forces another Newton iteration *regardless of residual*.
3. **`jls/analog/CardReader.java`** (created by TASK-0099) gains level dispatch:
   `.model <name> <kind>(...)` selects the model class by kind plus `LEVEL`.
   Unknown Spice3f5-era parameters are **parsed, warned once, and ignored** -
   the OP177A card literally carries `KF=2E-17, AF=1`, and rejecting unknown
   parameters makes every real vendor file unloadable.
4. **`jls/analog/ModelInspector.java`** - ~200 lines, reporting per library file
   which subcircuits are fully supported and which need an unimplemented model:
   *"this .lib defines 14 subckts and 31 models; 12 are fully supported; AD8620
   needs MOSFET level 2; OP2177 uses TABLE, PSpice dialect."* CLI surface:
   `-inspect <file>`, added to the `FLAGS` table
   (`src/jls/JLSStart.java:759-789`, 14 entries at HEAD) with its
   `CliFlagTableTest` row.
5. **Not shipped, named:** MOSFET **level 3** is deferred (re-entry cost 1.5-3.0
   mw, and it drops into `MosCommon` unchanged). BSIM3/4 and the rest of the
   compact-model zoo are out on arithmetic - BSIM4 alone is 25,006 SLOC
   declaring 897 model parameters. JLS **curates no vendor library**: it reads
   downloaded `.model`/`.subckt`/`.lib` files as data and redistributes none.

Done means: each of the five device families solves three named topologies to
the documented tolerance, the Gummel-Poon degeneracy is asserted rather than
assumed, and the inspector's report is a golden.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-049 | The device half of the feature. The palette half is TASK-0105 and the convergence half is TASK-0104. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0097 | A device model is a stamp into `MnaMatrix` plus a contribution to the Newton residual and Jacobian. Both types, and the escape ladder the limiters feed, are created there. |
| TASK-0099 | Model cards arrive through `CardReader`; a level-dispatched model with no card grammar to dispatch from is untestable against any real vendor file. |

## Acceptance test

`test/jls/analog/models/DeviceStampTest.java`, new - one nested class per
device, each with three topologies and a digest golden in the TASK-0098 format
(raw `double` bits, explicit step size, `steps/rejects/nrIters/nrFails/points`
header):

- `diodeRectifierMatchesItsStampGolden()`,
  `diodeReverseBreakdownIsNotSilentlyClamped()`.
- `gummelPoonWithoutEarlyOrKneeParametersEqualsEbersMoll()` - the load-bearing
  one. Run the same common-emitter fixture twice: once with `VAF/VAR/IKF/IKR`
  absent and once against a hand-computed Ebers-Moll reference, and assert the
  node voltages are bit-identical, not merely close. If this fails, two models
  were built where one was needed.
- `mosfetLevel1TripleTopologyMatchesGolden()` and
  `mosfetLevelThreeIsRejectedWithANamedDiagnostic()` - the deferral is asserted,
  so the refusal is legible rather than a parse error.
- `jfetSourceFollowerBiasesAsTheElectretCapsuleDoes()`.
- `switchHysteresisIsMonotonic()`.

`test/jls/analog/LimitingTest.java`, new:

- `aLimitedStepForcesAnotherNewtonIterationRegardlessOfResidual()` - inject a
  step that the limiter clamps while the residual is already below tolerance,
  and assert the iteration count increased. **This is the test that stops the
  worst failure mode**: getting `icheck` wrong yields plausible, wrong,
  *reproducible* answers.
- `limitersAreDeterministicAcrossRuns()` - two runs, identical digests.

`test/jls/analog/ModelInspectorTest.java`:

- `aRealVendorLibraryReportsPerSubcircuitSupport()` against a committed
  redistributable fixture.
- `anUnknownSpice3f5ParameterWarnsOnceAndLoads()` - `KF`/`AF` on a diode card.
- `anUnsupportedConstructIsNamedNotASyntaxError()` - a `LAPLACE` card yields
  `"unsupported construct LAPLACE from PSpice dialect"`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | no issue | The analog device program has no tracking issue. `11-analog-determination.md` §3.2 (D-A13, D-A14) is its written owner. |

## Notes

- **Ship Spice3f5-era cards, not ngspice-current ones.** ngspice's diode card
  has 88 settable model parameters; Spice3f5's had 14. The extra 74 are JUNCAP,
  tunnelling, recombination, self-heating, temperature coefficients of
  temperature coefficients, and safe-operating-area limits. The tiering *is* K9
  progressive disclosure applied to model cards.
- **`POLY(n)` is mandatory and belongs in the controlled sources, not here.** It
  is TASK-0099's `E F G H`. Without it JLS cannot read the majority of real
  vendor op-amp macromodels, which falsifies "op-amps are free via `.subckt`".
  Implement it natively; do not inherit the rewrite-into-code-models detour that
  silently degrades timestep tolerance.
- **Op-amps cost zero incremental model work.** A measured real transimpedance
  amplifier built from unmodified vendor macromodels histograms to `R D V C G F
  E I Q X L` with `.model` kinds npn/pnp/d only - exactly this task's set. Ship
  a behavioral op-amp element too, purely for progressive disclosure, and do
  **not** invent an op-amp `.model` type.
- **Absorbed BSD code is fine and must be attributed.** JLS is GPL-3.0-or-later
  and can absorb permissively licensed model code outright; the hazard is
  GPL-incompatible licenses, not permissive ones. Every absorbed file carries
  its notice.
- **The kill criterion belongs to the library story, and it is measured at the
  card-reader stage, not here:** if fewer than half of a representative sample
  of real downloaded vendor `.lib` files load with a *named* diagnostic rather
  than a parse error, drop the "download the model for the part you will solder"
  promise and ship a small curated redistributable set instead. Record the
  reversal.

## Evidence

- `src/jls/JLSStart.java:759-789` - the `FLAGS` table, 14 specs at HEAD, where
  `-inspect` lands; `test/jls/CliFlagTableTest.java` pins it.
- `test/jls/HeadlessCoreRatchetTest.java:74-79,90` - the headless prefixes;
  `jls.analog` is created by TASK-0097 as a policed-from-birth leaf and
  `jls.analog.models` inherits that.
- `11-analog-determination.md` §3.2 - the sixteen-model scope, the Gummel-Poon
  degeneracy finding with its source-line citation, the 88-vs-14-vs-15 parameter
  measurement, the JFET addition, D-A13 (level 3 deferred) and D-A14 (libraries
  are data).
- `11-analog-determination.md` §8.3 - the 156-line limiting apparatus and the
  `icheck` protocol warning; §8.5 - the model-curation kill criterion.
- Do not restate: TASK-0097 owns the solver types; TASK-0098 owns the golden
  format; TASK-0099 owns the card grammar and `POLY`.
