# FEAT-047 - The physical time base and the nominal real-time scalar

**Status:** proposed | **Cost:** 2-3 mw | **Owner program:** P4 |
**Spine rank:** -

## Capability delivered

A circuit can declare what one simulation tick means in physical time, once, in
one place. Everything that reports time - the waveform export, element delays, a
sample rate, a solver's step - then reports it in that unit instead of in
abstract counts, and an external tool that reads the output is being told the
truth rather than a nominal constant. Declaring nothing keeps today's behavior
exactly: time stays dimensionless and every existing file and every existing
golden is untouched.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-12 | required | a 0.16 Hz high-pass and a 5 Hz filter are statements about seconds; without a unit the design cannot be specified, let alone checked |
| CAP-14 | required | parity with an external analog simulator is parity in seconds; a tick has no counterpart there |
| CAP-10 | required | a sample rate is samples per second. The drawn tier inherits this through the analog region; the no-solver tier can carry a rate on the element instead |
| CAP-11 | required | as above, for the capture direction |
| CAP-07 | beneficial | timing data handed to a fabrication flow is in real units; the tape-out path needs the same declaration for a different reason |
| CAP-18 | required | a 345.6 ps flight time and a 50 ps edge rate are meaningless against a dimensionless tick, and the electrical-length lint multiplies a time by a velocity. The declared physical length also rides this feature's format bump rather than minting one. Added 2026-08-03 under D16: the filed issue #367 declares `serves_capstones: [... 313 ...]` and #313 carries 367 in `requires_features`; this table's omission was a transcription defect |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | none. A single optional attribute on the circuit record, plus a conversion, plus a documentation change |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0101 | The nominal real-time scalar | The attribute, its grammar, the conversion class, the version policy and the normative documentation change |

## Acceptance criteria

1. A circuit may declare one time base using the waveform standard's own
   timescale grammar - a magnitude of 1, 10 or 100 and a decimal unit from
   seconds down to femtoseconds. Absent means dimensionless, exactly as today.
2. **Absent by default keeps every existing golden byte-identical**, including
   the waveform export golden. A test asserts this rather than a reviewer
   claiming it.
3. Conversion to seconds is recomputed from the integer tick every time and is
   never accumulated. A test asserts that stepping a million times and
   converting equals converting the millionth tick, exactly.
4. A file that declares a time base is refused by a reader that predates the
   attribute, with a message naming the version - it is never silently ignored,
   because ignoring it would misread every number in the file.
5. Above the tick count at which a double can no longer represent an integer
   exactly, the conversion asserts rather than silently losing precision, and
   the limit is documented with its arithmetic.
6. The waveform export writes the declared unit when one is declared, and the
   documentation stops describing its constant as nominal in that case.
7. The quantization error of a rate that is not exactly expressible on a decimal
   tick lattice is documented with a worked figure, so a student who measures it
   finds it explained.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the declared physical time base and its conversion | **no issue** |

The committed roadmap already owns the item - the timing program lists a
per-circuit timescale, with the tick staying an integer count of precision units
and the timescale being the interpretation
(`docs/capability-roadmap/README.md:405-406`) - but no tracker issue was ever
opened for it.

## Design notes

**The fiction is already in the tree and already load-bearing on an external
tool.** The normative semantics document states that simulation time is
dimensionless and that nothing binds it to seconds
(`docs/simulation-semantics.md:24-29`), while the batch waveform exporter writes
a fixed `1 ns` timescale into every trace
(`src/jls/sim/BatchSimulator.java:423`, documented as a nominal
tool-compatibility mapping at `docs/batch-interface.md:253-262`). This feature
does not create that tension; it makes it operative, which is an argument for
resolving it rather than against.

**Why the whole-file version must move.** The file format ignores unknown
attributes silently. An old reader that ignores a declared time base would
misread every duration in the file, so this is exactly the case a
must-understand mechanism exists for. Today that means the whole-file format
version (`src/jls/Circuit.java:102`, refused at `:765-769`). If FEAT-013 lands
first, the time base should ride its per-section must-understand flag instead and
this feature costs less; if it lands second, FEAT-013 must adopt the attribute
rather than leave two version mechanisms in the tree.

**Pick a default precision and state the range.** At a 1 picosecond base the
64-bit tick space covers about 107 days of simulated time and about 2.5 hours
before ticks stop being exactly representable in a double. At 1 nanosecond those
become 292 years and 104 days. The picosecond base is the recommendation for
mixed-signal work; the choice is a maintainer decision that should be recorded
with this arithmetic, not discovered by a user hitting a limit.

**Keep the decimal lattice.** A rational time base would make audio rates exact
and would break the one reason for adopting the waveform standard's grammar. The
resulting quantization is 11.6 parts per million at a nanosecond base and 1.7
parts per billion at a picosecond base for a 44.1 kHz rate - small, real, and
worth documenting rather than hiding.

## Risks

- **A default that is not absent would break every golden in the tree.** The
  attribute must be optional and unset by default; this is the single decision
  that keeps the change cheap.
- **Two version mechanisms.** If this ships as a whole-file epoch and FEAT-013
  later ships per-section must-understand flags, JLS carries both forever unless
  the sequencing note above is honored.
- **Scope creep into the delay model.** A declared time base invites rewriting
  element delays in physical units in the same change. That is the timing
  program's structured-delay work and is an order of magnitude larger; this
  feature declares the unit and converts, and stops there.

## Evidence

- The determination, the grammar, the no-accumulation contract, the range table
  and the audio-rate quantization arithmetic:
  `11-analog-determination.md:534-581` (§2.7); stage S4 and its 2-3 mw band at
  `:1151-1159`.
- The normative clause this feature amends: `docs/simulation-semantics.md:24-29`.
- The constant it makes honest: `src/jls/sim/BatchSimulator.java:423`, described
  as nominal at `docs/batch-interface.md:245-262`.
- The version mechanism and its refusal path: `src/jls/Circuit.java:102`
  (`FORMAT_VERSION = 2`), `src/jls/Circuit.java:765-769`.
- The committed roadmap's own statement of the item, which prices it inside the
  timing program: `docs/capability-roadmap/README.md:405-406`.
- **Cost reconciliation.** Band 2-3 mw. Tasks named for it: TASK-0101,
  totalling 1.5 wk. The named tasks are the leading, dividable slices of this
  feature, not the whole of it; the residual has no task id, because the
  registry's task space is closed at TASK-0112. Do not read 1.5 wk as the
  feature.
