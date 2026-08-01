# TASK-0075 - Checkpoint round-trip equivalence test

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** TASK-0074

## Deliverable

One property test, its fixture set, and its null - the gate that replaces
reviewing a field list that decays every time an element ships.

1. **The property.** `replay(ckpt[i]) == ckpt[i+1]` **byte-identically, in CI**.
   Run an uninterrupted reference run taking checkpoints at a declared cadence;
   then, for each `i`, resume from `ckpt[i]`, run forward to the same instant,
   serialize, and compare bytes against `ckpt[i+1]`. One uncaptured mutable
   field produces a replay that is *almost* right, which is exactly what a code
   review does not catch.

2. **The continuation observable, not only the state.** The VCD produced from
   the resumed run's remainder must be byte-identical to the corresponding tail
   of the uninterrupted run's VCD. `BatchSimulator.toVcd`
   (`src/jls/sim/BatchSimulator.java:384-476`) is deterministic by construction -
   signals declared and dumped in full-name order, no `$date`/`$version`
   (`:420-422`) - and `docs/batch-interface.md` §4 freezes the profile, so a tail
   comparison is well-defined.

3. **A fixture set spanning the state classes that actually break**, one
   `.jls` each under `test/fixtures/` (which today holds four files, 144 KiB
   total):
   - a tri-state bus mid-conflict, so `WireNet.conflictReported`
     (`src/jls/elem/WireNet.java:407`) is `true` at the checkpoint;
   - a `Memory` with a `MemoryWrite` in flight - checkpoint strictly between the
     post at `now + accessTime` (`src/jls/elem/Memory.java:1383-1387`) and its
     arrival;
   - a `Register` with a non-null `toBeValue` in flight
     (`src/jls/elem/Register.java:693,771-780`);
   - a `StateMachine` with `busy` true (`src/jls/elem/StateMachine.java:659`);
   - a `Clock` checkpointed mid-phase, i.e. inside `[cycle-one, cycle)`
     (`docs/simulation-semantics.md` §8.3);
   - a `TestGen` mid-vector, so the input cursor is nonzero.

4. **The null.** A committed codec variant that omits exactly one field -
   `Register.currentC` - asserted to **fail** the round trip, with the report
   naming the fixture and the first differing byte offset. Without it the round
   trip passes vacuously for every field the fixtures never exercise, and a
   green check mark replaces an unexamined assumption.

5. **Lane placement.** The suite runs in TASK-0016's long-run lane, not the
   required fast lane, with the reference run's length and wall-clock band
   stated in the test's javadoc.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-035 | The acceptance criterion the feature is written against. A checkpoint format without this test is a claim. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0074 | Serialize the queue, the clock and stateful element contents | This test reads and compares checkpoint bytes that only TASK-0074 produces, and resumes through a loader only it provides. |

## Acceptance test

`test/jls/sim/CheckpointRoundTripTest`:

- `resumingFromEachCheckpointReproducesTheNextByteForByte()` - a
  `@ParameterizedTest` over (fixture x checkpoint index). The assertion is on
  bytes, not on a field-by-field comparator, because a comparator written by the
  same author as the codec shares its blind spots.
- `theResumedTailVcdIsByteIdenticalToTheUninterruptedTail()` - the observable
  check, which catches a state that round-trips but simulates differently (an
  event restored with the wrong `seq`, for instance, changes same-time ordering
  without changing any field's value).
- `aDeliberatelyDroppedFieldFailsTheRoundTrip()` - the null, asserting failure
  **and** the report text.
- `aCheckpointTakenAtTheTimeLimitIsWellDefined()` - `now > maxTime` sets
  `now = maxTime` and breaks out of the loop
  (`src/jls/sim/Simulator.java:231-234`), leaving a queue whose head is past the
  limit. TASK-0011 and TASK-0012 decide what that means; this test pins whichever
  answer they record, so the decision cannot quietly reverse.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the checkpoint round-trip equivalence test | **no issue** |
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps - once this suite exists it is the cheapest existing probe of the unverified cross-platform determinism assumption: run one fixture on all three platforms and compare the checkpoints |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | overlaps - same reason, other platform |

## Notes

- **Gate on the property, not on the list.** TASK-0074's reflective field sweep
  is a map that tells an author where to look; this test is the gate. A
  field-enumeration review is already stale twice over in the evidence base, and
  it will be stale again the next time an element ships.
- **`.jls~` autosave "checkpoints" are a different mechanism**
  (`ARCHITECTURE.md:108-113`). Do not let a test name collide.
- **No wall clock in the artifact.** The VCD header deliberately omits
  `$date`/`$version` so the same run always produces the same bytes
  (`src/jls/sim/BatchSimulator.java:420-422`). A checkpoint that stamps wall
  clock reintroduces exactly what that decision removed and makes this test
  impossible to write.
- **`SimEvent.sequence` must already be per-simulator** (TASK-0074 deliverable
  2). If it is still `static` (`src/jls/sim/SimEvent.java:87`), the resumed run
  inherits a different base and `theResumedTailVcdIsByteIdenticalToTheUninterruptedTail`
  fails for a reason that looks like a codec bug and is not.
- **Cadence matters for cost, not for correctness.** Checkpointing every event
  makes the suite quadratic in run length. Declare the cadence in the test, keep
  the reference run short enough to sit inside the long lane's budget, and state
  what fraction of the state space the fixtures actually reach.
- **Mixed-fidelity is out of scope**, matching TASK-0074's restriction: every
  fixture here is single-fidelity. Say so in the javadoc so the gap is visible
  rather than assumed covered.

## Evidence

- `src/jls/sim/BatchSimulator.java:384-476` - `toVcd`, deterministic by
  construction; `:420-422` - the deliberate omission of `$date`/`$version`.
- `docs/batch-interface.md` §4 - the frozen VCD profile that makes a tail
  comparison meaningful; §6 - the stability promise.
- `src/jls/sim/Simulator.java:231-234` - the `now > maxTime` termination path.
- `src/jls/sim/SimEvent.java:87` - the static sequence counter.
- `src/jls/elem/WireNet.java:407` - `conflictReported`.
- `src/jls/elem/Memory.java:1383-1387` - the in-flight `MemoryWrite` post.
- `src/jls/elem/Register.java:693,771-780` - `toBeValue` and the pff capture that
  sets it.
- `src/jls/elem/StateMachine.java:659` - `busy`.
- `docs/simulation-semantics.md` §8.3 - `Clock`'s phase, so "mid-phase" is a
  well-defined instant.
- `test/fixtures/` - the existing fixture home (4 files, 144 KiB), and
  `.gitattributes:1-5` - the `-text` discipline byte-exact fixtures need.
- `docs/virtual-hardware-parity.md` P15 - "Gate on the property test, not on
  reviewing a list that decays every time an element ships", and the
  non-negotiable `replay(ckpt[i]) == ckpt[i+1]` criterion.
