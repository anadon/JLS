# CAP-10 - Audio output

**Status:** proposed | **Priority:** 11 | **Marginal cost:** 2-3 mw (after
CAP-12) | **Standalone cost:** 5-7 mw (the solver-free rung alone)

## Outcome

A circuit drawn in JLS makes a sound the user hears from the host speakers in
real time, and the same circuit renders to a WAV file offline when the fidelity
the student chose is too expensive to play live.

## Acceptance test

SEEN: a student draws a counter, a `Memory` holding a wavetable and a
`Register`, runs `jls -b -wav tone.wav wavetable.jls`, and plays the file; with
the host door granted at invocation, the same circuit plays live. At the drawn
rung, an 8-bit R-2R ladder feeding an LC reconstruction filter plays a 440 Hz
tone through the speakers in real time with a live trace on the filter node.

CHECK: four named tests.
- `HostAudioSinkGoldenTest` - a fixed circuit rendered to WAV produces a
  byte-identical PCM payload across the platform and JDK matrix. The
  tick-resampler is a pure function of tick rate and sample rate, so this is an
  equality assertion, not a tolerance one.
- `AudioDoorGrantTest` - with no invocation-time grant the sink refuses with a
  diagnostic naming the drawn element, and the circuit still simulates to
  completion. A `.jls` file on its own can never open an audio device.
- `AudioRealTimeBudgetTest` - the solver-free rung sustains at least 44,100
  samples per second on the CI reference machine, asserted with margin against
  the measured ~209,000 samples/s ceiling.
- `LinearFastPathAudioTest` - the drawn R-2R plus LC circuit renders one second
  of 440 Hz audio inside a pinned wall-clock budget on the reference machine,
  and the factorization-cache hit rate stays above a pinned floor. Its failure
  mode is the interesting one: the fast path silently falling back to a full
  Newton loop per timestep.

## Demo slice

The solver-free rung on its own: `HostAudioSink` as an in-tree `LogicElement`
over `javax.sound.sampled`, a PCM WAV codec, the tick-resampler that every
later analog output reuses verbatim, a `-wav` batch flag, and the
one-door-at-invocation grant. 3-4.5 mw, zero analog code, zero new
dependencies. It produces a capstone-shaped artifact before the analog
programme exists and it retires the programme's only governance question - does
a host door survive review - in week 4 rather than week 25.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-045 | Host audio sink and source without a solver | the whole standalone rung: the sink, the codec and the resampler | required |
| FEAT-047 | The physical time base and the nominal real-time scalar | a sample rate is a physical quantity; simulation time is dimensionless at HEAD, so there is nothing to resample against | required |
| FEAT-046 | The analog solver core and its determinism gate | the drawn rung is an analog filter, and its linear fast path is what makes the drawn rung real time rather than a render | required |
| FEAT-048 | A2D/D2A bridge elements and A-STEP synchronization | the drawn ladder is a `Dac` and its edges are breakpoints the solver must be told about | required |
| FEAT-049 | Analog device models, the drawn palette and convergence hardening | only for the class-D rung, which is a rendered THD lab and not a live demo | beneficial |
| FEAT-043 | The breadboard canvas and its physical-simulation binding | the classroom form of an audio output stage is on a breadboard | beneficial |
| FEAT-008 | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | the live trace pane and the palette entry are editor surface, and the editor is untestable until it is decomposed | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the capstone itself | **no issue** |
| - | FEAT-045 through FEAT-049, the entire analog and host-audio programme | **no issue** - verified: an open-issue search for "analog" in `anadon/jls` returns zero results |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs, and must be reconciled explicitly - `docs/vcd-interop.md:18-23` rejects live co-simulation under this issue, and live audio playback is a host door adjacent to that rejection |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | depends on - `docs/extension-points.md` needs the audio-door row filed before the seam exists, as that catalog's own rules require |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` | informs - under decision D7 the host door is explicitly NOT a plugin seam; the audio elements are in-tree `LogicElement`s |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | overlaps - the drawn rung drives the same value-representation hot path |

## Open decisions

1. **Does the host audio door survive review?** Recommendation: take it to
   review at the solver-free rung, in week 4, and reconcile #63 explicitly in
   `docs/vcd-interop.md` and `SECURITY.md` rather than by implication. Reason:
   this is the first host door of its kind in JLS's history - `System.in` does
   not appear anywhere in `src/` at HEAD - and finding out costs 3-4.5 mw here
   against 30 mw later.
2. **Live playback or file-out first.** Recommendation: file-out first, live
   playback second behind the same grant. Reason: if the door is refused the
   capstone still lands as file-only and nothing built is wasted.
3. **Trapezoidal or Gear-2 on the LC tank.** Recommendation: build both,
   default trapezoidal, document `method=gear`, and spend one day at the drawn
   rung driving a step discontinuity onto the filter node. Reason: measured,
   trapezoidal's amplification factor at `h/tau = 1000` is -0.996 - it
   alternates sign and decays 0.4% per step, which is the ringing - against
   0.0223 for Gear-2; but at `h/tau = 0.1` Gear-2 is 4.3x less accurate and
   damps a tank that physically should not damp.
4. **Is the class-D rung in the capstone's written definition?**
   Recommendation: yes, and stated explicitly as a rendered offline THD lab.
   Reason: measured 2.8-4.9 minutes of wall clock per second of audio. Writing
   it down is what stops a student from believing it should play live.

## Kill criteria

1. If the host audio door is **refused at review**, the live-playback half is
   withdrawn and the capstone becomes file-only. Cost of finding out: 3-4.5 mw.
2. If the solver-free rung cannot sustain **44,100 samples/s with at least 2x
   margin** on the reference machine, drop live playback and ship rendering
   only.
3. If the drawn rung exceeds **1.5 s of Java per second of audio** after the
   linear fast path - against a measured band of 0.72-0.88 - the drawn rung is
   offline-render only and "hear your circuit live" survives on the solver-free
   rung alone.
4. If WAV output is not byte-identical across the 4-platform x 2-JDK matrix,
   move the audio golden to a tolerance comparator and withdraw the determinism
   claim **in writing**.
5. This capstone inherits CAP-12's kill. If the analog programme has not
   produced a drawn, running heart rate monitor at **24 maintainer-weeks
   cumulative**, CAP-10 terminates permanently at the solver-free rung.

## Evidence

- Stage costs and the rung table: `11-analog-determination.md` §5.1 (S0
  3-4.5 mw, S7 2-3 mw) and §6.2.
- Measured, per second of audio: solver-free wavetable rung ~209,000 samples/s
  = 4.7x real time; drawn PWM into an LC filter, 11 nodes, 4.34 M accepted
  timepoints, 0.72-0.88 s of Java; drawn 8-bit R-2R into RC, 28 nodes, 1.17 M
  accepted timepoints, ~0.81 s of Java; transistor class-D half-bridge, 17
  nodes, 11.19 M accepted timepoints (335,830 accepted + 118,104 rejected =
  26.0% rejection), 2.8-4.9 minutes.
- Linear fast path, measured Java copy+factor+solve against solve-only with the
  factorization reused: N=11 0.541 -> 0.167 us (3.2x), N=17 1.469 -> 0.317
  (4.6x), N=24 2.727 -> 0.545 (5.0x), N=28 3.765 -> 0.692 (5.4x). It costs
  0.5-1.0 mw and it is the highest performance-per-week item in the analog
  programme; it belongs in the solver's first stage, not its tail.
- **Correction carried forward:** the R-2R-versus-class-D fidelity ratio is
  **7.2x, not 40x**. The 40x figure came from a fixture in which seven of eight
  R-2R bit sources were static DC; regenerated with all eight bits stepping at
  44.1 kHz from a real 440 Hz sine (105,233 bit transitions per audio second)
  it is 11.7 s of C against class-D's 84.6. Do not quote 40x.
- HEAD facts verified at `b54e6ee`: `grep -rn "System.in" src/` returns 0 -
  there is no read-side host door today; there is no `module-info.java` in
  `src/`, so `java.desktop` needs no module widening; `grep -rn "javax.sound"
  src/` returns 0.
- `docs/simulation-semantics.md:26` - "Simulation time is a dimensionless
  non-negative 64-bit integer". This is why FEAT-047 is required rather than
  beneficial: without a declared time base a sample rate has no meaning.
- `docs/vcd-interop.md:18-23` - live co-simulation was evaluated and rejected
  under issue #63; that rejection is about an external testbench stepping JLS,
  not about JLS writing samples to a device, and the distinction must be made
  in writing rather than assumed.
- Decision D7 (BRIEF §12): host access is one door granted at invocation, the
  `-serial stdio` model; the port is a sealed in-tree intermediate, not a
  plugin seam.
- Decision D8 (BRIEF §13): "orchestrate, never reimplement" is revoked as
  policy; ngspice is BSD and absorbable into a GPL-3.0-or-later tree, so the
  drawn rung's solver is a cost judgment.
- **Cost reconciliation.** Marginal band 2-3 mw. Its 4 required features sum
  to 28.5-42 mw and its 3 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
