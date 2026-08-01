# CAP-12 - A heart rate monitor

**Status:** proposed | **Priority:** 13 | **Marginal cost:** 11-16 mw |
**Standalone cost:** 26.5-38.5 mw (cumulative through the mixed-signal stage)

## Outcome

A drawn mixed-signal circuit - photodiode front end, analog conditioning,
comparator, converter, and a student-written digital beat counter - takes a
recorded photoplethysmogram and displays a beat rate in real time, which is the
point at which JLS stops being a digital tool.

## Acceptance test

SEEN: the student draws photodiode into a transimpedance amplifier, a 0.16 Hz
high-pass, a x101 gain stage, a 5 Hz Sallen-Key, a comparator, an `Adc`, and
their own digital beat counter. Running it against a tracked recorded PPG
waveform prints BPM in real time. The transimpedance node's trace sits at
-0.19998 V with roughly 2.0 mV of pulsatile ripple - the 1% the physics
predicts, visible on a drawing the student made.

CHECK: five named tests.
- `HeartRateGoldenTest` - asserts detected **beat count and beat interval
  within a stated tick tolerance**, never an exact threshold-crossing tick.
  This is binding from the first commit, not a later refinement: measured, a
  1 nV perturbation of the solution moves a 1 mV, 1 Hz signal's
  threshold-crossing by ~1.6e5 ticks. Cross-platform the perturbation is
  exactly zero so the golden reproduces everywhere, but an exact-tick golden
  would make every legitimate solver improvement read as a regression.
- `AnalogDeterminismMatrixTest` - byte-identical transient output across 4
  platforms x 2 JDKs, with an independent spec-derived reader rather than the
  writer's own parser.
- `PortAlphabetTest` - an analog port cannot be connected to a digital net. The
  check runs *above* the width check and validates rather than widens, so a
  mis-wire is a diagnostic and not a silent coercion.
- `GroundSingularityDiagnosticTest` - a circuit with no ground, and a circuit
  with a floating analog island, each produce a diagnostic naming the drawn
  element the student must fix, never "matrix singular at row 7".
- `PaletteVisibilityTest` - a first-year drawing an adder sees no analog
  palette entry; visibility is derived from context, so the pedagogy floor is a
  test rather than an intention.

## Demo slice

The headless solver alone: `R L C V I D` stamps, dense LU with partial
pivoting, Newton with junction limiting, trapezoidal integration with
predictor-corrector local-truncation-error control, and `.op` plus `.tran`,
plotting an RC step response and a full-wave rectifier from CSV. 3.5-5 mw, no
GUI, no drawn analog, no committed element type. It is also the **mandatory
calibration experiment**: take that slice to the full coverage and mutation
gate, measure the maintainer-weeks it actually took, and re-cost everything
downstream proportionally. A 328-line Java spike already produces the numbers.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-046 | The analog solver core and its determinism gate | the conditioning chain is a circuit that must be solved, and a verdict about a beat is worthless if it is not reproducible | required |
| FEAT-047 | The physical time base and the nominal real-time scalar | a heart rate is beats per minute; without a declared time base there are no minutes | required |
| FEAT-048 | A2D/D2A bridge elements and A-STEP synchronization | the comparator and `Adc` are the boundary, and the lock-step between the two loops is what makes the beat count deterministic | required |
| FEAT-049 | Analog device models, the drawn palette and convergence hardening | the drawn devices the chain is made of, and the hardening that makes the *sixth* circuit a student draws converge | required |
| FEAT-027 | Strength lattice, driver kinds and net kinds | the comparator output driving into digital logic needs an honest drive model, not an assumed ideal one | required |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | analog is a second view over one model; its geometry belongs in its own versioned section, addressed the same way every other view is | required |
| FEAT-045 | Host audio sink and source without a solver | the recorded PPG waveform arrives as a sample stream through the same resampler | beneficial |
| FEAT-043 | The breadboard canvas and its physical-simulation binding | the classroom form of this circuit is on a breadboard | beneficial |
| FEAT-008 | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | eight drawn devices on one generic dialog and one generic renderer, plus a trace pane, is editor surface that is untestable until the editor is decomposed | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the capstone itself | **no issue** |
| - | FEAT-045 through FEAT-049, the entire analog programme | **no issue** - verified: an open-issue search for "analog" in `anadon/jls` returns zero results |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | depends on - the analog stepper is a second hot loop and needs its catalog row filed before the seam exists |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | overlaps - the digital half of the mixed-signal boundary runs on the same value representation |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` | informs - analog devices are in-tree element types under D7, and a new element type costs zero format versions |
| 76 | Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode | informs - an analog trace pane inherits every requirement in that issue |
| 63 | HDL Stage 3: black-box HDL component - external GHDL/Icarus co-simulation | informs - the recorded rejection of live co-simulation is the nearest precedent for how a second engine may attach |

## Open decisions

1. **Does an analog element become the fourth `Element` permit, and does `Put`
   widen?** Recommendation: yes to both, with an `instanceof` guard on the
   reacting path. Reason: verified at HEAD, `Element` permits exactly
   `DisplayElement, LogicElement, Wire` (`src/jls/elem/Element.java:18`) and
   `Put`'s constructor takes a `LogicElement`
   (`src/jls/elem/Put.java:57`) - an analog device cannot hold a port today.
   This is the blocker that must be settled before any device is drawn.
2. **Eight drawn devices or twenty-two.** Recommendation: eight - ground,
   resistor, capacitor, inductor, voltage source, current source, diode and a
   subcircuit reference - on one generic dialog and one generic renderer.
   Reason: a stall after this stage then leaves a coherent eight-device palette
   rather than a half-supported twenty-two, and the generic dialog holds the
   coverage draw at roughly 11.7% of the shared commons rather than 71%.
3. **Sparse LU now, or dense indefinitely.** Recommendation: dense, with the
   sparse trigger set at a circuit size no capstone reaches. Reason: measured,
   every capstone circuit in the study is 7-28 equations; dense LU is 3.765 us
   at N=28; sparse buys 27x only at N=50; and Markowitz pivot-tie ordering is
   the largest un-derisked determinism item in the programme.
4. **Strict floating-point in the device evaluation loop.** Recommendation: ban
   the intrinsified math library from the analog package by ArchUnit rule - the
   mechanism is already adopted and `ArchitectureRulesTest` already exists -
   and **measure the cost in the solver's first stage, not its sixth**. Reason:
   device evaluation is a measured 51.7-59.6% of transient runtime and the JIT
   intrinsifies the fast variant but not the strict one, which puts the
   determinism cost in the hottest code. If it is large the answer is a
   deterministic software exponential (1-3 mw), not abandoning determinism.
5. **Does the PPG waveform ship as a tracked fixture?** Recommendation: yes,
   with provenance. Reason: the capstone is a sensor-only region with zero
   converters driving the analog side, so it needs no live device and the
   fixture is what makes the golden meaningful to a grader.

## Kill criteria

1. If the mixed-signal stage has not produced a **drawn, running heart rate
   monitor at 24 maintainer-weeks cumulative**, stop before the transistor
   library. The terminal deliverable is then the headless solver, host audio,
   the determinism gate, filters from vendor subcircuit data and the time
   base - documented as a lab tool, with no drawn analog palette, therefore no
   pedagogy-floor debt, no twenty-two element types to maintain, and no file
   format surface that analog owns.
2. If determinism does not hold across the 4-platform x 2-JDK matrix at the
   gate stage - week 8, 4-6 mw - the entire justification fails and the
   programme stops there. This is deliberately the earliest falsifiable point.
3. If the coverage draw exceeds **900 uncovered lines** at the end of the
   mixed-signal stage, the generic-dialog design has failed and the remaining
   fourteen element types must not be built.
4. If **two consecutive device families** score below 80 on mutation testing
   with stamp goldens already written, the device library stops at the diode
   and the shipped scope becomes linear elements plus diode plus subcircuit
   data.
5. If golden regeneration churn exceeds **one full-corpus regeneration per
   month**, demote full-waveform goldens to a nightly lane and keep only the
   digest tier in the required gate.

## Evidence

- Stage cost and the chain: `11-analog-determination.md` §5.1 (the mixed-signal
  stage at 11-16 mw, cumulative 26.5-38.5) and §6.1; risks and kills at §8.6
  and §8.7.
- Measured on ngspice-42 with Java primitives measured in the same session: the
  chain is **24 circuit equations** (18 solved node voltages plus 4 branch
  currents). Ten seconds of PPG on a 1 ms lattice gives 10,012 attempted,
  10,011 accepted, 1 rejected timepoint and 20,022 Newton iterations = 2.00 per
  timepoint, 0.053 s in C. Java dense LU at N=24 is 2.727 us; device load is
  0.80 us per iteration in C taken at 2-3x, giving 4.3-5.2 us per Newton solve
  and **8.6-10.4 ms of Java per second of PPG at a 1 kHz lattice - 96-116x real
  time**, or 0.87-1.06 ms at 100 Hz.
- It is the **easiest** analog capstone, not the hardest: op-amp macromodels
  keep the signal path almost entirely linear with no forward-biased junction
  in it. It needs only resistor, capacitor, current source, diode, subcircuit
  data and basic Newton - no transistors and no convergence hardening - and its
  timestep is set by the sample lattice the student asks for (25-250 Hz PPG,
  so 4-40 ms), not by the physics and not by error control.
- Golden fragility, measured: 1.6e5 ticks of threshold-crossing movement per
  1 nV of solution perturbation, with the cross-platform perturbation exactly
  zero. This is why the golden is on beat count and interval.
- The governing sentence for every cost figure here: analog cost is set by the
  fastest thing that MOVES in the analog region - a carrier, not a signal.
  Accepted timepoints per second of signal range from 12-1,000 for this chain
  to 11.19 M for a switching class-D stage, at 7-28 equations throughout. Any
  capacity planning based on node count will be wrong by orders of magnitude.
- HEAD facts verified at `b54e6ee`: `src/jls/elem/Element.java:18` permits
  `DisplayElement, LogicElement, Wire`; `src/jls/elem/Put.java:57` takes a
  `LogicElement`; `src/jls/elem/ElementRegistry.java:38-77` lists 35 element
  types; `grep -rn "StrictMath" src/` returns 0, so the strict-math discipline
  starts from a clean tree; `test/jls/ArchitectureRulesTest.java` exists, so
  the rule has somewhere to live.
- `docs/simulation-semantics.md:26` - simulation time is a dimensionless
  64-bit integer; the time base is what a heart *rate* needs.
- Licensing (BRIEF §13, D8): every ngspice component is absorbable into a
  GPL-3.0-or-later Java project, so "use existing implementations to speed
  development" means porting, with attribution, rather than reading.
- Stated the D10-compliant way, because it keeps being read backwards: CAP-04
  does **not** depend on this capstone. Analog, once it exists, improves CAP-04
  for free - the RC debounce network and the Schmitt or 555 clock compose at
  approximately zero marginal cost.
- **Cost reconciliation.** Marginal band 11-16 mw. Its 6 required features
  sum to 61.5-94 mw and its 3 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
