# TASK-0101 - The nominal real-time scalar

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

One optional physical time unit per circuit, declared in the save file, carried
into the VCD header, the waveform axis and every delay display - and computed
from the integer tick every time, never accumulated.

Precisely what changes:

1. **`src/jls/core/TimeBase.java`**, new: a record `(int mult, Unit unit)` where
   `mult` is `1|10|100` and `Unit` is `S|MS|US|NS|PS|FS` - VCD's own
   `$timescale` grammar, so the exporter and the model agree by construction.
   Methods: `parse(String)` (rejects, never repairs), `toString()` emitting the
   canonical `1ps` spelling, `exponent()`, and
   `seconds(long ticks) = ticks * mult * 10^exponent` **recomputed from the tick
   count on every call**. There is no accumulating `double t` field and there
   must never be one. `jls.core` is inside `HeadlessCoreRatchetTest`'s
   `CORE_PACKAGE_PREFIXES` (`test/jls/HeadlessCoreRatchetTest.java:74-79`) with
   `BASELINE` empty (`:90`), so the class is born AWT-free and stays so.
2. **`src/jls/Circuit.java`**: a `@Nullable TimeBase timebase` field with
   accessors. `save` (`:1466-1512`) writes `TIMEBASE <spelling>` on the line
   after `CIRCUIT <name>` **only when the field is set**, so every existing file
   re-saves byte-identically. `loadCircuit`'s token loop (`:886-900`) - which
   today accepts exactly `ELEMENT` or `ENDCIRCUIT` and calls
   `failLoad(MALFORMED, "expected ELEMENT or ENDCIRCUIT here, ...")` on anything
   else - gains a `TIMEBASE` arm before the `ELEMENT` arm.
3. **`Circuit.FORMAT_VERSION` 2 -> 3** (`:102`), and `formatVersionNeeded()`
   (`:1580-1588`) returns `max(element versions, timebase == null ? 1 : 3)`. A
   file with no timebase still declares `FORMAT 1` or `FORMAT 2`; the refusal
   path for a newer file is unchanged (`:765-769`).
4. **`src/jls/sim/BatchSimulator.java:423`**: `out.append("$timescale 1 ns
   $end\n")` becomes the circuit's timebase when present, and the literal
   `1 ns` when absent. Absent is the default, so
   `VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte` does not
   move.
5. **Display.** `src/jls/edit/Trace.java:77-78` (`scaleFactor`, simulation time
   units per pixel) and the tic labelling that reads it at `:263,276` render
   axis labels as physical time when a timebase is present and as bare ticks
   when it is not. Element delay dialogs gain the unit suffix in their label
   text only - the stored attribute stays an integer tick count.
6. **Docs.** `docs/simulation-semantics.md` §1 amended: time is still an integer
   tick count; a timebase, when declared, is the *interpretation*, and
   `t_seconds` is recomputed from the tick, never accumulated.
   `docs/file-format.md` §9 version history gains a `3:` row stating the
   block-structure justification for the bump. `docs/batch-interface.md` §4.2's
   admitted fiction is replaced by the real rule, under §6's "additions that
   cannot break a conforming consumer" clause. CHANGELOG entry.

Done means: a circuit with no `TIMEBASE` behaves and saves exactly as at HEAD;
a circuit with one reports `fmax` in MHz, labels its waveform axis in ns, and
emits a VCD whose `$timescale` is the declared unit.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-047 | This *is* the feature's substance: the declared unit and the recompute-from-integer rule. |

## Prerequisite tasks

None. Every seam this touches exists at HEAD: the `CIRCUIT` block, the
`FORMAT` header negotiation, the VCD emitter and the trace scale factor. Doing
it before the analog solver costs nothing and is owed regardless - `docs/
capability-roadmap/sweep-02-timing.md` Change F prices it at 2-3 mw as a P4
obligation of SDF, Liberty and SDC, independent of any analog work.

## Acceptance test

`test/jls/TimeBaseTest.java`, new:

- `absentTimebaseSavesAndLoadsExactlyAsAtHead()` - load every fixture in
  `test/fixtures/`, re-save, assert byte equality with the committed file and
  that the declared `FORMAT` version did not change.
- `aDeclaredTimebaseRoundTripsAndBumpsToFormatThree()` - set a timebase, save,
  assert the file's first line is `FORMAT 3` and the third line is
  `TIMEBASE 1ps`, reload and assert equality.
- `secondsIsRecomputedFromTheTickAndNeverAccumulates()` - assert
  `seconds(n)` equals `seconds(1) * n` exactly for a decade of `n` up to 2^53,
  and that summing `seconds(1)` n times does **not** equal `seconds(n)` - the
  test states the hazard the rule exists to avoid.
- `aVersionTwoReaderRejectsAFormatThreeFileLoudly()` - drive
  `Circuit.readFormatHeader` with `FORMAT 3` against `FORMAT_VERSION = 2` and
  assert `LoadError.Category.NEWER_FORMAT`, not `MALFORMED`.
- `theQuantizationOfFortyFourPointOneKilohertzIsTheDocumentedFigure()` - assert
  the tick error of a 44.1 kHz period at a 1 ps base is within 2 ppb, pinning
  the number the audio capstone documentation quotes.

`test/jls/VcdExportGoldenTest` gains
`vcdTimescaleFollowsTheDeclaredTimebase()`, with a second golden file for a
timebase-carrying fixture. The two existing goldens must not change; if they
do, the default is not absent and the change is wrong.

`test/jls/FileFormatSpecTest` gains a `TIMEBASE` row so the grammar in
`docs/file-format.md` and the parser cannot drift.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | no issue | The physical time base has no tracking issue. `docs/capability-roadmap/sweep-02-timing.md` Change F is its written owner. |

The standards issues this unblocks (SDF, Liberty, SDC) are cited in
`sweep-02-timing.md` by number but are **not in the 34 open issues**; do not
cite them as open.

## Notes

- **The loader's default arm is the trap.** `Circuit.loadCircuit:894-900` treats
  every unrecognized token as a malformed file. That is what makes the version
  bump mandatory rather than optional: a version-2 reader given a `TIMEBASE`
  line would report "not a JLS circuit" instead of "needs a newer JLS". Add the
  arm and the bump in the same commit.
- **`Circuit.stateHash()` (`:1548`) hashes the saved text.** A circuit that
  gains a timebase changes its hash. That is correct and only affects files that
  opt in, but `DeterministicSaveTest` and `CircuitRoundTripTest` must be read
  before assuming so.
- **Nested subcircuits never write the record.** `save` already suppresses the
  `FORMAT` line for an imported circuit (`:1477-1483`); `TIMEBASE` follows the
  same rule. One timebase per file, owned by the top level, or two subcircuits
  could declare different seconds for the same tick.
- **Do not add a real-number delay attribute in this task.** Change F's optional
  real form (`0.37`) stored as a scaled integer is a separate increment; mixing
  it in here doubles the format surface and the review.
- **Range is asserted, not discovered.** At a 1 ps base, 2^63 ticks is 106.8
  days and 2^53 ticks - the exactly-representable-in-`double` limit device
  equations care about - is 2.5 hours. Put both in the javadoc.
- **44.1 kHz is not exactly expressible on any decimal lattice** because
  `44100 = 2^2 * 3^2 * 5^2 * 7^2`. The residual is 1.7 ppb at 1 ps. Document it
  in the audio capstone text so a student who FFTs a WAV finds an explanation
  rather than files a bug.

## Evidence

- `src/jls/Circuit.java:102` (`FORMAT_VERSION = 2`), `:765-769` (the newer-file
  refusal), `:886-900` (the `ELEMENT`/`ENDCIRCUIT`-only token loop and its
  `MALFORMED` default), `:1466-1512` (`save`), `:1548` (`stateHash`),
  `:1580-1588` (`formatVersionNeeded`).
- `src/jls/sim/BatchSimulator.java:423` - `$timescale 1 ns $end`, emitted
  unconditionally today; javadoc at `:370-382` calls it nominal.
- `docs/simulation-semantics.md:25-29` - "Time units are abstract; nothing binds
  them to seconds", with the VCD mapping named as tool compatibility only.
- `docs/batch-interface.md:261-263` (the admitted fiction), §6 (the stability
  promise that blesses an additive change).
- `docs/file-format.md` §9 - the bump-required list; a circuit-level record is
  "a change to the block structure".
- `src/jls/edit/Trace.java:77-78,263,276` - the scale factor and the tic
  increment that would carry a unit.
- `test/jls/HeadlessCoreRatchetTest.java:74-79,90` - `jls.core` is policed from
  birth with no baseline entries.
- Do not restate: `docs/simulation-semantics.md` owns the time model;
  `docs/file-format.md` owns the evolution policy;
  `docs/capability-roadmap/sweep-02-timing.md` owns Change F's rationale.
