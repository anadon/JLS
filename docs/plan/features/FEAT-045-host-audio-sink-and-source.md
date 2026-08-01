# FEAT-045 - Host audio sink and source without a solver

**Status:** proposed | **Cost:** 5-7 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A circuit can be heard, and can hear. A drawn design writes sample values to a
sink element and the run either plays them through the machine's speakers or
writes a WAV file; a source element reads samples from a WAV file or from the
machine's microphone and presents them to the circuit. No analog engine is
involved at all - the samples are integers on ordinary digital nets, and the
only new machinery is a sample-rate resampler that maps simulation ticks onto
audio frames. This is the cheapest capstone-shaped artifact in the entire
program: a student draws a counter, a memory holding a wavetable and a register,
runs it headless, and plays the result.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-10 | required | the entire no-solver demonstration of audio output is this feature; the drawn-DAC version adds to it rather than replacing it |
| CAP-11 | required | samples must arrive from somewhere before any front end can process them |
| CAP-12 | beneficial | a recorded PPG waveform played into the circuit is the fixture the beat detector runs on, and it arrives the same way audio does |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | none. Measured: this stage depends on nothing in the analog program, nothing in the engine program and nothing in the format program |

That is a finding, not an omission. It is why this feature should be built
first among everything analog-adjacent: it retires the one governance question
the whole analog and device program rests on - does a host door survive review -
for 5-7 weeks instead of 30.

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0096 | Host audio sink and source | The two elements, the WAV codec, the tick resampler, the two batch flags and the invocation-time grant |

## Acceptance criteria

1. `HostAudioSink` and `HostAudioSource` are drawable elements. A sink consumes
   a value and a strobe; a source produces a value and a strobe. Neither takes a
   sample rate from the circuit file without the rate being visible in the
   element's dialog and saved with it.
2. Every audio path has a **file mode** that requires no sound hardware, and CI
   uses only the file mode. A WAV written from a fixture circuit is
   byte-identical across runs, platforms and JDKs.
3. Live playback and live capture are granted at invocation, exactly as the
   host byte port is granted, and the grant is named on the run's outcome line.
   A `.jls` file cannot open an audio device.
4. No `javax.sound` call, no file handle and no host resource is touched on any
   path reachable from `Reacts.react()`; samples are buffered and moved at a
   declared boundary, asserted by the same ratchet that guards the byte port.
5. The tick-to-sample resampler is a separate, tested unit with a stated
   rounding rule, because every later analog output reuses it verbatim.
6. A golden produced with live audio granted is refused by CI, the same rule and
   the same mechanism as for a live console transcript.
7. The measured throughput is recorded in the documentation with its method, so
   a student who finds their circuit cannot keep up knows what "keep up" means.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | host audio sink and source, the WAV codec and the tick resampler | **no issue** |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | overlaps - the audio door is a host door and belongs in the same catalog row set as `elem.host-port`, or the catalog is not total |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - `docs/vcd-interop.md:19-24` cites #63 while rejecting live co-simulation; live audio is a host door, not a co-simulation transport, and the distinction must be written down rather than argued later |

## Design notes

**One door, not two.** FEAT-032 defines a sealed host-byte seam with an
invocation-time grant. Audio is a second host door with the same governance
question and it must not acquire a second grant model, a second ratchet or a
second place where "was this run live" is recorded. Whichever of the two lands
first defines the mechanism and the other conforms to it. Neither is a build
prerequisite of the other; both are prerequisites of the same one-page policy.

`javax.sound.sampled` is in `java.desktop`, already on a Swing application's
classpath, and there is no `module-info.java` in `src/` to widen - so this
introduces no dependency and does not disturb the single self-contained offline
jar. Measured on JDK 25 in the study session.

Sample rates are where honesty is cheap and lies are expensive. 44.1 kHz is not
exactly expressible on a decimal tick lattice - `44100 = 2^2 * 3^2 * 5^2 * 7^2`
and the `3^2 * 7^2` is fatal - so a declared timebase (FEAT-047) produces a
quantization error of 11.6 ppm at a 1 ns base and 1.7 ppb at 1 ps. Put the
figure in the capstone documentation, so a student who measures it finds it
explained rather than files a bug. FEAT-047 is not required to ship this
feature; it is required for the sample rate to have a unit.

## Risks

- **The host-door review is the real risk and it is why this ships first.** If
  a read-side host door is refused, the audio capstone pair changes shape. The
  cost of finding out here is 5-7 weeks; the cost of finding out after the
  analog program is thirty.
- **Live audio is a determinism hazard by construction.** Underruns, device
  buffer sizes and clock drift are all outside JLS. The mitigation is that live
  mode never produces an artifact CI reads, enforced rather than documented.
- **Rate mismatch as a support burden.** A circuit whose strobe rate does not
  match the declared sample rate will produce pitch-shifted audio and a
  confusing bug report. The resampler's rounding rule and an explicit warning
  when the rates disagree by more than a stated tolerance are cheaper than the
  support thread.

## Evidence

- Stage S0, its contents, its cost band and its measured throughput ceiling
  (~209,000 samples/s against 44.1 kHz, a 4.7x margin), and the finding that it
  depends on nothing: `11-analog-determination.md:1073-1095`.
- Audio input measured at ~0.84 s of Java per second of audio; audio output on
  the linear fast path at 0.72-0.88 s per second:
  `11-analog-determination.md:1182-1201`.
- The 44.1 kHz quantization arithmetic and the ppm/ppb figures:
  `11-analog-determination.md:534-581` (§2.7).
- The one-door-at-invocation grant model this feature must share:
  `BRIEF.md` §12 D7; `03-determination.md:240-283`.
- Verified at HEAD `addc6c5`: `grep -rn "System.in" src/` = 0 and
  `grep -rn "ProcessBuilder" src/` = 0 - there is no host door of any kind in
  `src/` today, so the policy this feature sets is the first one.
- The live co-simulation clause that must be reconciled rather than contradicted:
  `docs/vcd-interop.md:19-24`.
- **Cost reconciliation.** Band 5-7 mw. Tasks named for it: TASK-0096,
  totalling 2 wk. The named tasks are the leading, dividable slices of this
  feature, not the whole of it; the residual has no task id, because the
  registry's task space is closed at TASK-0112. Do not read 2 wk as the
  feature.
