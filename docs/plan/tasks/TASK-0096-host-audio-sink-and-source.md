# TASK-0096 - Host audio sink and source

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

Two new in-tree elements that move PCM samples between a running circuit and
the host, each with a **file mode** so every test is deterministic and no test
needs a sound card.

Precisely what changes:

- `src/jls/elem/HostAudioSink.java` and `src/jls/elem/HostAudioSource.java`,
  both `LogicElement` subclasses. Attributes: sample rate, sample width in
  bits, channel count, and the tick-per-sample divisor. The sink consumes the
  value on its data input once per divisor tick; the source drives a new value
  once per divisor tick and schedules its own next event in `Clock`'s idiom
  (`src/jls/elem/Clock.java:404-430`, self-posted `SimEvent` at `now + when`).
- `src/jls/elem/LogicElement.java:17-21`: the sealed `permits` clause gains
  both types. **24 permits at HEAD**; every exhaustive switch over the
  hierarchy stops compiling until it handles them - that is the registration
  contract working, not a defect.
- `src/jls/elem/ElementRegistry.java:38-77`: two rows (35 entries at HEAD).
  Consequent, non-optional edits, each pinned by an existing test: the frozen
  save-tag table (`src/jls/elem/SaveTags.java`, pinned by `SaveTagsTest` and by
  the registry-to-tags totality test added in `970db41`), the palette table
  (`src/jls/edit/Palette.java`, pinned by
  `test/jls/edit/PaletteContractTest.java`), a renderer, a dialog, and the HDL
  export policy buckets at `src/jls/hdl/HdlExporter.java:429,438,443,460`
  (`EXPORTED`/`SKIPPED`/`TOPOLOGY`/`REJECTED`) - an element in no bucket makes
  **every** export of a circuit containing it abort at `:191-197`. These
  elements belong in `SKIPPED` with a stated reason.
- `src/jls/util/Wav.java`: a dependency-free PCM WAV reader/writer (RIFF
  header, `fmt ` and `data` chunks, integer PCM only).
- `src/jls/util/TickResampler.java`: the tick-lattice resampler, with the
  interpolation expression and its evaluation order written down **before**
  first use - `v0 + (v1 - v0) * (t - t0) / (t1 - t0)`, in that association -
  and added to `docs/simulation-semantics.md`. Every later analog output reuses
  this class verbatim.
- CLI: `-wav <file>` (sink to file) and `-audio-in <file>` (source from file)
  in the `FLAGS` table at `src/jls/JLSStart.java:759-789`, with parse cases and
  usage text. **Live host device access is granted only by an explicit
  invocation flag** and is never a property of the circuit file; a `.jls` that
  contains a `HostAudioSink` and is run without the flag runs in file mode or
  silently discards, never opens a device.
- Documentation: an `extension-points.md` row is **not** added (this is not a
  seam); a `SECURITY.md` paragraph is, stating that audio is a door granted at
  invocation; and `docs/batch-interface.md` gains the two flags.

Done means: a circuit with a counter, a `Memory` wavetable and a
`HostAudioSink` renders a WAV headlessly, byte-identically on two runs, and the
same circuit with `-audio-in` reads a WAV back in and reacts to it.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-045 | The whole of "host audio sink and source without a solver" - this is FEAT-045's first and largest slice. |

## Prerequisite tasks

None. This depends on nothing in the plan and on nothing that does not already
exist at HEAD. That is the reason it is first in the analog program despite
containing no analog code.

## Acceptance test

- `test/jls/elem/HostAudioModelTest` (new class):
  `sinkConsumesOneSamplePerDivisorTick()` and
  `sourceDrivesOneSamplePerDivisorTickAndSchedulesItsOwnNextEvent()` - assert
  the event sequence, not the audio; extends the model-test regime the
  `RegisterModelTest` / `MemoryModelTest` classes established.
- `test/jls/util/WavRoundTripTest.everySupportedWidthAndRateRoundTrips()` -
  writes and reads back 8/16/24-bit mono and stereo at 8 k/44.1 k/48 k and
  asserts sample equality, plus `aTruncatedChunkIsRejectedWithADiagnostic()`
  for the hostile-input case.
- `test/jls/HostAudioFileModeGoldenTest.wavetableCircuitRendersTheGoldenWav()`
  - runs the committed fixture headlessly and asserts byte equality against
  `test/resources/audio/wavetable.wav`. This is the acceptance test that proves
  determinism, because it is the artifact a student hears.
- `test/jls/util/TickResamplerTest.interpolationMatchesTheDocumentedExpressionBitForBit()`
  - asserts against values computed by the exact expression written in
  `docs/simulation-semantics.md`, using `Double.toHexString` in the failure
  message. **Never `Double.toString` in a golden** - its output changed in
  JDK 19.
- `test/jls/HostAudioGrantTest.aCircuitWithoutTheFlagNeverOpensAHostDevice()` -
  in the `SocketConfinementRatchetTest` idiom
  (`test/jls/SocketConfinementRatchetTest.java:35-40`): a source-scanning
  ratchet asserting `javax.sound.sampled` device acquisition appears in exactly
  the one class permitted to hold it, and nowhere else.

## Related GitHub issues

**no issue.** The registry records the whole analog program (FEAT-045 through
FEAT-049) as having no tracker representation, and `search_issues` over
`anadon/jls` for `audio OR analog OR spice` returns only two unrelated hits
(#220, #51, both closed). Adjacent:

| # | title | relationship |
|---:|---|---|
| #78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - its registry half shipped and is exactly what makes this a ~70-line registration tax instead of a project. Not closed by this task. |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - open. `docs/vcd-interop.md:18-24` records that **live co-simulation was evaluated and rejected** under #63. A host audio door is not co-simulation, and the document must say why in the same paragraph, or the two decisions will be read as contradictory. |

## Notes

- **This is the first read-side host door in JLS's history.** `grep -rn
  "System.in" src/` returns zero at HEAD, and `grep -rn "javax.sound" src/
  test/` returns zero. `SECURITY.md` treats a `.jls` as untrusted data. The
  governance question - does a host door survive review? - is retired here, for
  two weeks, instead of in month seven for thirty. Design the grant so the
  answer is obviously yes: one door, granted at invocation, never inferable
  from file content.
- **No new dependency, and no module widening.** `javax.sound.sampled` is in
  `java.desktop`, already on a Swing application's classpath, and there is no
  `module-info.java` anywhere in `src/` to widen (verified this session).
- **Cost honesty.** This task is the 2-week slice; the stage it belongs to is
  costed at 3-4.5 maintainer-weeks including the documentation reconciliation
  and the resampler specification. FEAT-045's 5-7 mw band carries the
  remainder. Do not read the 2 weeks as the stage.
- **Real-time headroom is measured, not assumed:** ~209,000 samples/s against
  a 44.1 kHz requirement, 4.7x margin, with no solver involved at all.
- **The saved-parameter trap that arrives later.** There is no real-number item
  kind in the file format (`docs/file-format.md:118-140`: item kinds are
  `int|long|bigint|string|ref|pair|probe|circuit-block`, and a reader
  encountering anything else MUST fail the load) and `Element.setValue` has
  exactly four overloads (`src/jls/elem/Element.java:344,359,374,389`). Audio
  parameters here are integers and dodge it; every later analog parameter must
  be saved as a `String` holding its SPICE spelling. Do not introduce a
  `double` item kind for this task's convenience.

## Evidence

- Registration surface, verified at HEAD: `src/jls/elem/LogicElement.java:17-21`
  (sealed, 24 permits), `src/jls/elem/Element.java:17-18` (sealed over
  `DisplayElement`, `LogicElement`, `Wire`),
  `src/jls/elem/ElementRegistry.java:38-77` (35 types),
  `src/jls/hdl/HdlExporter.java:191-197,429,438,443,460` (unbucketed element
  aborts export), `src/jls/edit/Palette.java:27-60` (palette table).
- The self-scheduling element idiom: `src/jls/elem/Clock.java:392,404-430`;
  the sealed event payload set it posts through:
  `src/jls/sim/SimEvent.java:22-24`.
- Absence of any host or audio surface at HEAD: `javax.sound` 0 hits in `src/`
  and `test/`; `System.in` 0 hits in `src/`; `ProcessBuilder` 0 hits in `src/`;
  no `module-info.java` under `src/` (all four grepped this session).
- Measured audio throughput, the "no solver needed" finding, and the
  reconciliation obligation against `docs/vcd-interop.md`:
  `11-analog-determination.md` §5 stage S0.
- WAV-writer size estimate (~150 lines, no dependencies) and the
  one-resampler-two-consumers rule: `spice-jls-integration.md` §6.5;
  `11-analog-determination.md` §2.10.
