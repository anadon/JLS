# CAP-11 - Audio input

**Status:** proposed | **Priority:** 12 | **Marginal cost:** 2-3 mw (after
CAP-12) | **Standalone cost:** 5-7 mw

## Outcome

Recorded or live host audio enters a drawn circuit as samples at 44.1 kHz, an
analog front end the student drew conditions it, and the circuit's reaction
leaves as data the student can plot or play back.

## Acceptance test

SEEN: a student runs `jls -b -audio-in speech.wav -wav out.wav preamp.jls`,
watches the preamp output on a trace as the waveform plays through, and gets an
output WAV in which the circuit's effect on the signal is audible. With the
host door granted at invocation, a live microphone drives the same circuit and
the samples land in the digital half of the drawing.

CHECK: five named tests.
- `HostAudioSourceGoldenTest` - a fixed input WAV through a fixed circuit
  produces a byte-identical output WAV across the platform and JDK matrix.
- `AudioInDoorGrantTest` - with no invocation-time grant there is no
  microphone; the source element refuses with a diagnostic naming the drawn
  element and the circuit still simulates to completion.
- `IdealAdcQuantizationTest` - the ideal converter's output is a pure function
  of (sample, vref, bits) with pinned behavior at and beyond both rails, so the
  cheap rung has a specified meaning independent of the solver.
- `AudioInRealTimeTest` - the front end sustains real time on the reference
  machine, asserted with margin against the measured ~0.84 s of Java per second
  of audio.
- `SarConverterFidelityTest` - the drawn successive-approximation converter
  produces the same digital word as the ideal converter on a pinned corpus, at
  whatever wall clock it takes. Nightly, never required: it is the offline
  fidelity rung.

## Demo slice

The source half of the host-audio feature plus an ideal converter, with no
analog solver at all: a WAV read into a digital circuit through `IdealAdc`, and
back out through `IdealDac`. Roughly 1.5-2 mw of the standalone band. It buys
the read-side governance answer - the one that matters most, since the read
side is what `.jls`-files-are-data hardening is protecting - for the price of a
codec and a resampler that are already built for CAP-10.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-045 | Host audio sink and source without a solver | the source element, the codec and the resampler; the sink is what makes the result audible | required |
| FEAT-047 | The physical time base and the nominal real-time scalar | 44.1 kHz is a physical rate and simulation time is dimensionless at HEAD | required |
| FEAT-046 | The analog solver core and its determinism gate | the conditioning chain - coupling cap, preamp, Sallen-Key, comparator - is an analog circuit | required |
| FEAT-048 | A2D/D2A bridge elements and A-STEP synchronization | samples crossing into the digital engine is exactly the A2D boundary, and the lock-step between the two loops is what makes it reproducible | required |
| FEAT-043 | The breadboard canvas and its physical-simulation binding | a microphone preamp is a breadboard circuit before it is anything else | beneficial |
| FEAT-008 | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | the live trace on the preamp node is editor surface | beneficial |
| FEAT-049 | Analog device models, the drawn palette and convergence hardening - **ADMISSION SLICE ONLY** | the analog port permit and the datum/node-partition contract, i.e. the element-hierarchy widening that lets an analog terminal exist at all. FEAT-048's own prerequisite table names it, and the filed FEAT-048 issue #368 confirms it from its own side with `blocked_by: [331, 351]`, so a closed required set has to contain it. **Nothing else of FEAT-049 - no model library, no drawn palette, no convergence hardening - is needed by any step of this capstone's outcome**; those stay out and are funded by CAP-12. Added 2026-08-03 under D16 | required |

**FEAT-049's element-admission slice entered the required set, and the correction is recorded rather than made silently (D16, 2026-08-03).** This document previously read *"Deliberately absent: FEAT-049"* - a stronger claim than `beneficial`, and the one being refuted. The filed capstone issue **#303** carries **#331** in `requires_features`, and the filed FEAT-049 issue **#331** declares `serves_capstones` including **303**. Both ends say required; the document was the outlier. Three independent checks decide it on evidence rather than on the template's authority rule alone:

1. **The mechanism genuinely does not exist.** At `2d0ca9d`, `src/jls/elem/Element.java:17-18` is `public abstract sealed class Element` permitting `DisplayElement, LogicElement, Wire`, and `src/jls/elem/Put.java:57` takes a `LogicElement` - so an analog bridge has no legal terminal. Without the widening, FEAT-048 cannot be built at all.
2. **FEAT-048's own prerequisite table names it**, and the filed #368 mirrors that with `blocked_by: [331, 351]`. A required set that omits a required feature's own stated prerequisite is a rule-E defect.
3. **The carve is a decision, not a drift.** The admission slice - the analog port permit plus the datum/node-partition contract - is required here; the rest of FEAT-049 is not. FEAT-049's own document draws that boundary at `docs/plan/features/FEAT-049-analog-device-models-palette-convergence.md:47-53`, requiring that *"The port widening lands as a **standalone commit carrying no analog code**"*.

**The slice is UNPRICED and that is stated rather than hidden.** The corpus prices FEAT-049 whole at 21-33 mw and does not decompose it, so this capstone's required sum is a **lower bound** until the slice has a band. #331 whole is funded by CAP-12 (#305); a reader adding the two capstone sums today counts the slice exactly once, in CAP-12, and must not read this capstone's figure as having absorbed it. **No number was invented to close the gap.**

The rest of FEAT-049 stays out for the reason this document already gave, which remains correct for everything except the slice: the minimum needs no transistor models at all - an op-amp macromodel arriving as a `.subckt` data file gets the same capstone, which is why audio input lands ahead of the device library rather than behind it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the capstone itself | **no issue** |
| - | FEAT-045 through FEAT-048, the host-audio and analog boundary work | **no issue** - verified: an open-issue search for "analog" in `anadon/jls` returns zero results |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs, and must be reconciled explicitly - `docs/vcd-interop.md:18-23` rejects live co-simulation under this issue, and a live microphone is the read-side host door |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | depends on - the audio-in door needs its `docs/extension-points.md` row filed before the seam exists |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` | informs - under D7 the host door is not a plugin seam; a host-touching provider, if ever sanctioned, goes out of process |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | overlaps - 44.1 kHz sample delivery into the digital engine drives the same allocation path |

## Open decisions

1. **Live microphone capture, or file-in only.** Recommendation: file-in first;
   live capture behind the same invocation-time grant as CAP-10's sink, and
   reviewed in the same pass. Reason: a microphone is a privacy-relevant host
   resource and it is the read direction that `.jls`-files-are-data hardening
   is written against. Deciding the two doors together, once, is cheaper than
   twice.
2. **Is the converter an analog model or a digital state machine?**
   Recommendation: ship the ideal converter for the capstone and draw the
   successive-approximation converter - comparator and R-2R ladder in analog,
   register and sequencer in JLS's digital engine - as the offline fidelity
   rung. Reason: drawing the SAR costs a measured 108x, because the ladder node
   steps 353,200 times per second of audio and the analog region then accepts
   5.50 M timepoints per audio second. It is also the best available
   demonstration of *why* the mixed-signal boundary exists.
3. **Transistor preamp or op-amp macromodel.** Recommendation: op-amp
   `.subckt` as data. Reason: it lands the capstone 7-11 maintainer-weeks
   before the transistor library exists, at zero maintainer-weeks of model
   library, under decision D7's "circuit libraries are data".
4. **How the sample rate binds to the time base.** Recommendation: the sample
   rate derives from the circuit's declared time base, and the resampler
   refuses a non-integer tick ratio with a stated ppm figure rather than
   accumulating drift. Reason: an audio path that drifts silently produces a
   golden that fails for reasons no one can attribute.

## Kill criteria

1. If the read-side host door is **refused at review**, live capture is
   withdrawn and the capstone becomes file-in only. Same discovery cost as
   CAP-10's: 3-4.5 mw.
2. If the conditioning front end exceeds **1.5 s of Java per second of audio**
   against a measured ~0.84, audio input is offline-render only.
3. If the output WAV is not byte-identical across the 4-platform x 2-JDK
   matrix, move to a tolerance comparator and withdraw the determinism claim
   **in writing**.
4. If an op-amp macromodel preamp cannot be made to work without transistor
   models, this capstone moves behind the device library and its priority drops
   below CAP-14 - it stops being the cheap one, which is the only reason it is
   separate from CAP-10.
5. Inherits CAP-12's kill: no drawn, running heart rate monitor at **24
   maintainer-weeks cumulative** terminates this capstone at the
   ideal-converter demo slice.

## Evidence

- Stage cost and chain: `11-analog-determination.md` §5.1 (S6, 2-3 mw,
  depending on S5 and S0) and §6.3.
- Measured front end: electret capsule into a coupling cap, a preamp, a 15 kHz
  Sallen-Key and a comparator is **26 circuit equations**. 100 ms of audio on a
  22.6 us (44.1 kHz) lattice gives 5,489 attempted / 5,065 accepted / 4
  rejected timepoints and 16,876 Newton iterations = 2.19 per timepoint, 0.034
  s in C. That is 0.34 s of C per second of audio and ~0.84 s of Java
  (measured solve-only at N=26 is 0.615 us). Real time.
- The drawn SAR costs 108x: the ladder node steps 353,200 times per second of
  audio, giving 5.50 M accepted timepoints per audio second.
- The cost governor for every figure above: analog cost is set by the fastest
  thing that MOVES in the analog region - a carrier, not a signal. Across the
  whole programme the spread is six orders of magnitude (12 accepted timepoints
  per signal-second for a PPG chain against 11.19 M for class-D) at 7-28
  equations throughout. Node count is nearly irrelevant across that range.
- HEAD facts verified at `b54e6ee`: `grep -rn "System.in" src/` returns 0, so
  no read-side host door exists today and this capstone creates the first one;
  `grep -rn "javax.sound" src/` returns 0; there is no `module-info.java` in
  `src/`.
- `docs/simulation-semantics.md:26` - simulation time is a dimensionless
  64-bit integer, which is why the time base is a hard prerequisite.
- `docs/vcd-interop.md:18-23` - the recorded rejection of live co-simulation
  under #63, which this capstone must reconcile explicitly rather than by
  implication.
- Decision D7 (BRIEF §12) on the one-door-at-invocation grant and on circuit
  libraries as data; decision D8 (BRIEF §13) on reimplementation as a cost
  judgment.
- **Cost reconciliation.** Marginal band 2-3 mw. Its 4 required features sum
  to 28.5-42 mw and its 2 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
