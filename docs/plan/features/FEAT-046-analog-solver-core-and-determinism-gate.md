# FEAT-046 - The analog solver core and its determinism gate

**Status:** proposed | **Cost:** 17.5-26 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

JLS can solve a continuous-time circuit: it forms modified nodal analysis
equations from a set of devices, factors them, iterates Newton-Raphson to a
stated convergence criterion, advances time under a stated local-truncation-error
criterion, and reports node voltages and branch currents. It does this in pure
Java, single-threaded, with no external simulator linked or subprocessed, and it
produces the same bits on every supported platform and JDK - which is the
property that lets an analog result be a committed golden rather than a
screenshot. Alongside the solver it delivers the evidence that the solver is
*correct* and not merely *reproducible*: closed-form fixtures with derived
tolerances, per-device matrix-stamp assertions, and a nightly comparison against
a real external analog simulator.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-12 | required | a photodiode front end, a high-pass, a gain stage and a filter are continuous-time; nothing about the monitor works without a solver |
| CAP-14 | required | parity with an external analog simulator is a claim about a solver; the differential corpus is this feature's own acceptance evidence |
| CAP-11 | required | an audio input front end below the mixed-signal stage is not usefully demonstrable; the preamp is analog |
| CAP-10 | beneficial | the no-solver tier needs nothing here. The drawn R-2R ladder and LC reconstruction filter need the solver *and* its linear fast path |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | none. The solver owns its own time internally and needs no declared physical time base, no format change and no value-system change to run headless |

FEAT-047 becomes necessary the moment a solved waveform must be reported in
seconds rather than ticks, and FEAT-048 the moment the solver must exchange
values with the event loop - but building this feature first costs nothing extra
and de-risks the largest unknown in the analog program.

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0097 | Solver core and timestep control | Stamps, factorization, Newton with junction limiting, the escape ladder, and adaptive timestep control |
| TASK-0098 | The analog determinism controls | The controls beyond strict floating point that make results byte-identical, each with its own test |
| TASK-0099 | Controlled sources, waveforms and model cards | Controlled sources with native polynomial support, standard waveforms, and model and subcircuit card reading |
| TASK-0100 | The external-simulator differential corpus | The nightly tolerance comparison against a real analog simulator, and the method for detecting a regression that stays inside tolerance |

## Acceptance criteria

1. A `.tran` run of a fixture circuit produces **byte-identical** output across
   Linux, macOS and Windows, on x64 and aarch64, on two JDK versions. This is a
   required gate, not a nightly report.
2. Tolerance tests against closed-form solutions pass with the tolerance
   **derived** from the run's own convergence parameters, with the derivation in
   a comment. Two anti-cheat assertions hold: the numerical error is asserted
   nonzero, and disagreement with an independent oracle is asserted to have a
   lower bound.
3. Per-device matrix stamps are asserted entry by entry on raw bit patterns, so
   a defect whose effect is below physical tolerance - a sign error on a
   conductance floor, for instance - is caught by a test rather than by a user.
4. Solver statistics (accepted steps, rejected steps, Newton iterations, Newton
   failures, points) are pinned in every golden's header, so a convergence
   regression that leaves the waveform inside tolerance still fails.
5. A grid flip - the accepted timestep sequence changing - is reported as a
   distinct outcome, neither a pass nor a fail, and the comparison falls back to
   resampling on the fixture's declared step.
6. Nightly, non-gating: JLS and a real external analog simulator agree within a
   stated relative envelope on every fixture in the corpus. The lane self-skips
   when the external tool is absent, using the shipped optional-tool idiom.
7. Architectural rules are tests, not conventions: no non-strict math library
   call, no parallelism, and no hash-ordered iteration reaching the matrix,
   inside the analog package.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the analog solver, its determinism gate and its external oracle | **no issue** |
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - the opt-in external-tool CI lane this oracle rides on is the same lane the board on-ramp arms; build the lane once |

## Design notes

**The integration decision is to port, not to link.** Build the numerics in
Java, in-tree, and keep a real external simulator as an optional, non-gating,
maintainer-side oracle. Under D8 this is a cost judgment and it is licensable:
JLS is GPL-3.0-or-later and can absorb the relevant open-source solvers with
their notices. Linking or subprocessing an external solver from `src/` would
break the single offline jar, and external float solvers are not reproducible
across platforms or versions - which is exactly the property criterion 1 sells.

**Size the model cards against a teaching subset, not against a current
production simulator.** The measured contrast: a current diode model card exposes
88 settable parameters; the Spice3f5-era card had 14. The extra parameters are
tunneling, self-heating, temperature coefficients of temperature coefficients and
safe-operating-area limits. Target the era, document the target, and emit
"unsupported construct X from dialect Y" rather than a syntax error.

**Polynomial controlled sources are mandatory, not an extension.** Real vendor
op-amp macromodels use them; without polynomial support the claim that op-amps
arrive free as data is false. Implement them natively inside the controlled
sources rather than routing through a code-model detour that silently degrades
the timestep tolerance.

**Four things are forbidden in version one by written rule**: bypass, adaptive
matrix reuse, symbolic-factorization reuse across topology changes, and any
parallelism inside the solve. Each makes the answer depend on the history of the
run. Each would need its own byte-golden proof, and each is cheaper to forgo
than to prove.

Sparse factorization is deliberately deferred: dense factorization was measured
at 3.765 microseconds at 28 unknowns, and every capstone circuit examined is
between 7 and 28 unknowns. The re-entry trigger is a real circuit above roughly
100 unknowns, and the deferred item carries the specified total-order pivot
tie-break with it.

## Risks

- **Cross-platform byte identity might not hold.** It is the justification for
  the whole approach and it is falsifiable early - the determinism gate is
  reachable in roughly week eight of the program for 4-6 weeks of work. If it
  fails, the tolerance tier survives and the byte tier is withdrawn, and that is
  a decision to take with evidence rather than a surprise at week forty.
- **The pivot tie-break is the least de-risked determinism item.** A pivot choice
  that depends on anything but a total order over (fill-in estimate, magnitude,
  row, column) reintroduces platform dependence through the back door. Assert the
  permutation vector, not the answer.
- **Two inherited hazards must be closed before the first analog fixture lands.**
  The replica id used for stable-id ordering is per-install and random when
  unset, and it is not pinned in CI (verified: no `JLS_REPLICA_ID` in
  `.github/`). In a numerical kernel, device iteration order *is* floating-point
  accumulation order. Separately, decimal double formatting changed between JDK
  versions, so goldens must record raw bits or hex, never a formatted decimal.
- **Absorbed code carries hazards that a reading will not surface.** At least one
  candidate source calls an unseeded random generator inside its Newton step as a
  saturation tie-break. Budget an audit pass over every absorbed file for random
  sources, non-strict math, wall clock, hash iteration and parallel reduction,
  and make that lint a merge gate.
- **The half-finished engine is the largest program risk.** A solver that runs
  four demonstration circuits and fails on homework is worse than no solver,
  because it is claimed. The convergence work that prevents that is FEAT-049's
  and must not be treated as optional polish.

## Evidence

- The integration decision, its four weighed surfaces and the license finding:
  `11-analog-determination.md:54-173`, `:1452-1504`.
- Stage contents and bands that compose this feature's 17.5-26 mw: S1 solver and
  calibration 3.5-5, S2 determinism gate 4-6, S3 controlled sources, waveforms
  and cards 3-4, S8 corpus and external oracle 7-11:
  `11-analog-determination.md:1096-1150`, `:1202-1219`, `:1271-1294`.
- The five determinism controls, the two inherited hazards and the absorbed-code
  random-generator finding: `11-analog-determination.md:868-904`.
- The four-tier golden discipline and the sub-tolerance regression detectors:
  `11-analog-determination.md:905-977`.
- Scope in and out, the 88-versus-14 model-parameter measurement, the mandatory
  polynomial finding, and the forbidden-optimizations rule:
  `11-analog-determination.md:715-859`.
- Verified at HEAD `addc6c5`: `grep -rn "StrictMath" src/` = 0 (the architectural
  rule starts clean); `src/jls/Circuit.java:48` (`elements` is a `HashSet`, so
  elaboration must sort); `src/jls/Circuit.java:479-485`
  (`getElementsInStableOrder` exists and is the ordering to elaborate through);
  `src/jls/elem/ElementId.java:20,45` (the replica id is an environment
  variable, unpinned in CI).
- The optional-external-tool CI idiom to reuse:
  `test/jls/hdl/GhdlCompileTest.java:33-36`.
