# TASK-0010 - Stream the waveform dump instead of materializing it

**Status:** proposed | **Cost:** 4 d | **Blocked by:** none

## Deliverable

The VCD dump writes incrementally to a sink, and the whole-dump-as-one-string
path is removed.

1. **`BatchSimulator.toVcd()` is replaced by a sink-taking writer.**
   `src/jls/sim/BatchSimulator.java:384-476` builds the entire dump in a
   `StringBuilder` (`:386`) and returns it as a `String` (`:475`);
   `writeVcd()` (`:359-369`) then calls `toVcd().getBytes(UTF_8)` and
   `Files.write` - so the complete dump exists **three times over** at peak: the
   builder's char array, the returned `String`, and the `byte[]`. Introduce
   `void writeVcd(Appendable out)` (or a `Writer`) that emits each line as it is
   produced, and make `writeVcd()` a thin wrapper opening a buffered
   `Files.newBufferedWriter`.
   `toVcd()` is deleted, not deprecated: it is called only by `writeVcd` and by
   the tests listed below.

2. **The per-timestamp scan stops being quadratic.**
   `:456-471` iterates **every** signal at **every** change time
   (`for (long t : times)` containing
   `for (Map.Entry<String,Sig> e : signals.entrySet())`), doing a map lookup per
   pair and emitting only on a hit: O(times x signals) work for O(changes)
   output. Invert it: build one time-ordered list of (time, signal) changes
   during the fold and walk it once. The output must be byte-identical -
   signals in name order within each timestamp (`:462-470`).

3. **The trace store is bounded or documented.** `eventTrace` and `probeTrace`
   (the maps folded at `:394-411`) retain every `TraceSample` of the whole run
   in heap. Streaming the *dump* does not fix that. This task must either add
   an incremental path that writes samples as they are recorded, or state
   plainly in `docs/batch-interface.md` that peak memory is proportional to
   total recorded changes and leave the bound to FEAT-006. Pick one and say
   which; do not ship a "streaming" writer that still requires the whole trace
   resident without recording that it does.

4. `docs/batch-interface.md` - named as the format's compatibility contract at
   `src/jls/sim/BatchSimulator.java:372-374` - records the memory behavior of
   `-vcd`.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-005 | The materializing I/O path the feature names; a long batch run cannot produce a dump it cannot hold in heap. |

## Prerequisite tasks

None.

## Acceptance test

`test/jls/VcdStreamingTest.java`, new:

- `streamedOutputIsByteIdenticalToTheGolden()` - runs the same fixture as
  `VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte`
  (`test/jls/VcdExportGoldenTest.java:200`) through the new sink and asserts the
  bytes equal the existing golden. This is the whole safety argument: the format
  is a documented compatibility contract and must not shift by one byte.
- `noSingleAllocationHoldsTheWholeDump()` - writes to a counting `Appendable`
  that records the largest single `append` and the total, asserting the largest
  append is bounded by a line-sized constant while the total is far larger.
  Fails at HEAD, where one append carries the entire dump.
- `dumpTimeIsLinearInChangeCountNotSignalsTimesTimestamps()` - a fixture with
  many signals and many timestamps but few changes, asserting the 4x-size ratio
  bound rather than a wall-clock threshold.

`VcdExportGoldenTest` (all four tests, `:200, 217, 244, 326`) and
`VcdProbeExportTest.probedNetAppearsInVcd` must stay green
**unregenerated**.

## Related GitHub issues

**No issue.** The materializing paths have no tracker entry (registry TABLE 4,
FEAT-005 / TASK-0009 / TASK-0010).

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal java.util.BitSet allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - the `BitSet` values stored per `TraceSample` are #232's subject; this task changes how they are written out, not how they are represented |

Recorded decisions, closed, cite as such: **#72** (VCD export and the
`docs/batch-interface.md` contract), **#200** (probed nets unified into the
signal set).

## Notes

- **The determinism guarantees are explicit and must survive.** The javadoc at
  `src/jls/sim/BatchSimulator.java:371-378` states: signals declared and dumped
  in full-name order, no `$date`/`$version` headers, one JLS time unit per VCD
  unit. The `TreeMap<String,Sig>` (`:392`) and `TreeSet<Long>` (`:393`) are how
  that is achieved. A streaming rewrite that emits in encounter order breaks
  the golden and the contract.
- **The header cannot stream first-pass.** `$var` declarations
  (`:425-434`) need the full signal set and its widths before any value line, so
  the fold at `:394-411` still runs to completion before output begins. What
  streams is the value section (`:437-473`), which is where essentially all the
  bytes are. Say so rather than claiming a single-pass writer.
- **The name-collision loop must not move.** `:401-411` disambiguates a probe
  whose name collides with an element full name by appending `_probe`; it
  depends on `signals` being fully populated. Keep it in the header pass.
- **The trailing timestamp** at `:473-475` (`if (now > last)`) exists so viewers
  show the full simulated duration; a streaming writer must still emit it after
  the last change.
- **`writeVcd` throws `IOException`** already (`:359`), so the signature change
  costs nothing at the one production call site in `JLSStart`.
- **`vcdId`** (`:509-519`) allocates identifier codes by index over the
  name-ordered signal set; if the fold changes order the codes change and every
  golden breaks. Do not reorder `signals`.

## Evidence

- `src/jls/sim/BatchSimulator.java:359-369` - `writeVcd` calling
  `toVcd().getBytes(...)`: the triple materialization.
- `:384-386` - `toVcd()` and its single `StringBuilder`; `:475` - the returned
  `String`.
- `:392-411` - the `TreeMap`/`TreeSet` fold and the probe-name disambiguation.
- `:456-471` - the O(times x signals) emission loop.
- `:371-378` - the determinism contract and the pointer to
  `docs/batch-interface.md`.
- `test/jls/VcdExportGoldenTest.java:200, 217, 244, 326` and
  `test/jls/VcdProbeExportTest.java` - the byte-level goldens this task must not
  move.
