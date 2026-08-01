# TASK-0104 - Convergence hardening

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0097, TASK-0099

## Deliverable

The continuation, damping and limiting apparatus that turns "a tool that runs
the four circuits in this document" into "a tool a course can assign homework
on", plus the corpus of circuits that did not converge and the diagnostics that
name the drawn element responsible.

Precisely what changes:

1. **`jls/analog/Newton.java`** gains the escape ladder as an ordered,
   named-rung state machine with each rung's entry and exit condition stated as
   a pure function of solver state (a requirement, not a style note: TASK-0098's
   determinism rules forbid a rung whose selection depends on wall time,
   iteration identity or map order):
   - **rung 1** dynamic gmin ramp (one ramp, not three variants);
   - **rung 2** source stepping;
   - **rung 3** pseudo-transient continuation (`OPtran`) - the rung the
     reimplementations omit and the only one that converges a ring oscillator's
     `.op`.
2. **Per-device `convTest` veto.** `jls/analog/models/DeviceModel.java` gains
   `boolean converged(SolverState)`, defaulting to true, and the Newton loop
   requires **both** the node-voltage criterion **and** every device's veto. A
   node voltage can be stationary while a device current is not; without the
   veto the solver declares victory on a wrong answer.
3. **`jls/analog/Diagnostics.java`** - when the timestep collapses or Newton
   fails, the report names the **offending drawn element** by its element name
   and stable id, not "matrix singular at row 7". The mapping from matrix row to
   drawn element is built at elaboration and kept; it costs one array.
4. **The rejected-timestep rate as first-class output.** The run's statistics
   header (TASK-0098's format) already carries `steps/rejects/nrIters/nrFails`;
   this task adds the human line - *"11,748 of 46,395 timesteps were rejected
   (25%); the largest contributor was B1"* - to the batch report and to the
   analog waveform pane. It converts the most opaque part of analog simulation
   into something a student can act on.
5. **`test/fixtures/analog/hard/`** - the **200-circuit hard corpus**: every
   circuit that failed during S1-S9 development, minimized, committed with the
   symptom that produced it, plus deliberately hostile constructions (hard
   comparators, diode bridges driving astables, ring oscillators, latch-up
   topologies). Each fixture is bounded (under 20,000 steps) so the corpus can
   run outside the required gate without an unbounded CI cost.
6. **`docs/simulation-semantics.md`** gains the analog convergence section: the
   ladder, its rungs in order, the veto, and the statement that a non-converged
   solve is a **failure**, never a silently accepted point.

Done means: the 200-circuit corpus reports a non-convergence rate, that rate is
below the gate, and every non-convergence in it is attributed to a named drawn
element.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-049 | The half of the feature that decides whether the device models are usable on homework rather than on the four circuits their goldens were written against. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0097 | The ladder is inside `Newton`'s loop and the accept/reject rule is inside `TimestepController`; both are created there, along with the `R L C V I D` stamps the corpus is first built from. |
| TASK-0099 | The corpus's hard circuits are diode bridges, astables and hard comparators - all of which need controlled sources, standard waveforms and `.model` card reading to exist. A corpus of linear circuits converges trivially and proves nothing. |

**Not** blocked by TASK-0103, and the ordering runs the other way: the analog
determination sequences the transistor stage *after* this one ("Depends: S5,
realistically S10"), because the transistor models are the hardest thing to
converge and building them against an unhardened ladder means debugging two
things at once. Land this first, then extend the `convTest` veto to each new
device family as TASK-0103 adds it.

## Acceptance test

`test/jls/analog/ConvergenceCorpusTest.java`, new, in the long-run lane:

- `hardCorpusConvergesAtOrAboveTheGate()` - run all 200 fixtures; assert the
  non-convergence rate is **at or below 5%**, and write the failing fixture
  names into the assertion message. This is the numeric gate the kill criterion
  reads.
- `everyNonConvergenceNamesADrawnElement()` - for each failure, assert the
  diagnostic contains an element name present in the fixture, and that no
  diagnostic contains the substring `"row "`.
- `theCorpusIsBoundedAndStaysOutsideTheRequiredGate()` - assert every fixture is
  under 20,000 steps and that the corpus is tagged for the long-run lane, so it
  cannot silently move onto the 141 s required gate.

`test/jls/analog/EscapeLadderTest.java`, new:

- `aRingOscillatorOperatingPointConvergesOnlyViaPseudoTransient()` - assert that
  disabling rung 3 makes the fixture fail and enabling it makes it pass. The
  test documents *why* the rung exists; without it the rung is untestable
  dead weight the next contributor deletes.
- `eachRungIsEnteredOnlyOnItsStatedCondition()` - a state-machine trace,
  asserted against the documented condition list.
- `theLadderIsAPureFunctionOfSolverState()` - run the same failing fixture twice
  and assert the rung-entry trace is identical, closing the determinism hole.

`test/jls/analog/ConvTestVetoTest.aStationaryNodeVoltageWithAMovingDeviceCurrentDoesNotConverge()`
- a diode driven so its terminal voltages are within tolerance while its current
is not, asserting the veto keeps the loop running. This is the specific wrong
answer the veto exists to prevent.

`test/jls/analog/RejectedStepReportTest.theRejectionRateAndLargestContributorAreReported()`
- a hard comparator fixture, asserting both numbers appear and that the
contributor named is the comparator.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | no issue | The analog program has no tracking issue. `11-analog-determination.md` §5 stage S10 and §8.3 are its written owner, including the kill criterion this task's acceptance test measures. |

## Notes

- **This stage is not LOC-bounded and must not be compressed.** The code is
  roughly one week; the rest is a validation loop measured in
  circuits-that-converge. The band could be 2x wrong in either direction. Budget
  the second week for the corpus, not for the ladder.
- **The measured contrast that justifies the work:** at the *same circuit size*,
  a photoplethysmograph front end runs at 2.00 Newton iterations per timepoint
  with 1 rejection in 10,012, while a diode bridge plus astable runs at 20.4
  iterations per timepoint with 15.1% rejection. A 10x spread from the models
  alone. A hard ternary comparator rejected 11,748 of 46,395 timepoints - 25.3%
  - versus 0.02% for the sensor chain.
- **KILL criterion, numeric and binding:** if after 6 maintainer-weeks inside
  this work the 200-circuit corpus non-convergence rate is **above 5%**, the tool
  is not homework-grade. Restrict the shipped palette to the linear plus diode
  set, publish the refusal with the corpus results attached, and stop. Do not
  spend 10 more weeks hoping.
- **Build one gmin ramp, not three.** The reference implementation's three gmin
  variants are historical accretion; each extra variant is another rung whose
  entry condition must be deterministic and tested.
- **CI budget is a second kill.** The bounded corpus budgets to roughly 4 s
  against a 141 s required gate. If the *required* analog lane ever exceeds 15 s,
  move it to nightly - that is a kill for "analog goldens are a required gate",
  not for analog, but it costs the autograding capstone its strongest claim, so
  it must be a decision rather than a drift.
- **Do not let the ladder touch `jls.sim`.** Every rung runs inside one
  `react()` call at one value of `now`; the digital engine must not learn that
  retries exist.

## Evidence

- `src/jls/sim/Simulator.java:217,228` - the loop and the single point where
  `now` advances; nothing in this task may modify either.
- `test/jls/HeadlessCoreRatchetTest.java:74-79` - the headless prefix set
  `jls.analog` joins under TASK-0097; the diagnostics must not import `jls.edit`
  to reach an element name, so the elaboration-time row-to-element map is the
  mechanism.
- `11-analog-determination.md` §5 stage S10 (6-10 mw, depends on S3, the
  measured iteration and rejection figures, the 156-line limiting apparatus),
  §8.3 (the risk, the mitigations, the 5% kill criterion), §8.4 (the CI-time
  kill criterion and the 141 s required-gate figure).
- Do not restate: TASK-0097 owns `Newton` and `TimestepController`; TASK-0098
  owns the determinism controls and the golden format; TASK-0103 owns the
  limiters and the `icheck` protocol.
