# FEAT-006 - Simulation capacity and long-run ergonomics

**Status:** proposed | **Cost:** 3-5 mw | **Owner program:** P2 |
**Spine rank:** -

## Capability delivered

A batch simulation can run for hours without hitting a silent ceiling, can be
paused without being killed, reports that it is alive, can be interrupted
cleanly, and can address enough memory for a real guest. Four independent
ceilings exist at HEAD - a default time limit that a long run exceeds, a batch
`pause` that is literally `stop`, an event that is dropped when it crosses the
time limit, and a memory store whose capacity meets the guest minimum with zero
headroom. Each is small; together they are the difference between a run that can
be attempted and a run that cannot.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | Four named defects at HEAD, none with an issue, all in the same file pair |
| CAP-02 | required | A structural boot is 1.66-1.72 h and needs at least 12 MiB of guest RAM; every ceiling here is crossed by it |
| CAP-03 | required | Same run shape at ternary radix; the monitor program needs a run that survives an interrupt |
| CAP-06 | required | A grading batch over hundreds of submissions needs a heartbeat and a clean interrupt more than it needs speed |
| CAP-09 | required | Verification runs are long by construction; a silent event drop is a wrong answer, not a slow one |
| CAP-17 | required | the single-host capacity work a distributed run extends; the byte budget and the long-run paths are shared |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-005 | A long run is not expressible while the stimulus parse is quadratic and the dump is materialized: raising the time limit without fixing those converts a fast failure into a slow one |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0011 | Adjudicate and fix the past-limit event drop | The only one of the four that may be a correctness defect rather than an ergonomic one; it needs a recorded decision before a code change |
| TASK-0012 | Unbounded run duration | An explicit no-limit run mode so the default stops being a silent ceiling |
| TASK-0013 | Memory capacity as a byte budget, initialized copy-on-write | Shared with FEAT-036: the word-count cliff becomes a byte budget with headroom above the guest minimum |
| TASK-0014 | Long-lived batch mode with pause, heartbeat and clean interrupt | Shared with FEAT-035: pause stops being stop, and a checkpoint needs a pause that is not a kill |

## Acceptance criteria

1. A run started with the no-limit mode reaches a simulated time greater than
   the current default without terminating, and a run started without it behaves
   exactly as it does today. The default is not changed silently.
2. Batch `pause(true)` suspends and `pause(false)` resumes, with a test that
   asserts continuation is byte-identical to an uninterrupted run of the same
   length. `stop()` remains distinct and terminal.
3. An event whose time crosses the limit is either reacted, re-queued, or
   dropped **by a recorded decision** that the test cites by name. Whichever is
   chosen, the outcome is observable rather than inferred from source.
4. A progress heartbeat is emitted on a stated interval, and SIGINT leaves the
   output stream well-formed rather than truncated mid-record.
5. Memory capacity is expressed as a byte budget with stated headroom above 12
   MiB, and initialization does not transiently double the heap. A fixture at
   the guest minimum runs within a declared heap ceiling.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - #232 is per-event cost, owned by FEAT-030; this feature is capacity and control flow. They meet only at TASK-0013's heap budget |
| - | the time limit, the pause-equals-stop defect, the past-limit event drop and the memory cliff | **no issue** |

## Design notes

The four sites are small and adjacent, which is why the band is 3-5 mw for what
reads as four separate concerns.

- **Time limit.** `src/jls/JLSInfo.java:69` -
  `defaultTimeLimit = 100000000`. The batch loop at
  `src/jls/sim/Simulator.java:217` runs `while (!stopping && !eventQueue.isEmpty()
  && now <= maxTime)`, with `maxTime` initialized from that constant at `:38`.
- **The past-limit drop.** In the same loop the polled event is removed from the
  duplicate-check set at `src/jls/sim/Simulator.java:225`, and *then* the limit
  is tested at `:231-232`. An event past the limit is therefore removed from
  `dupCheck` and discarded without reacting and without re-queueing. Whether
  that is a defect or the intended behavior is listed among the study's open
  contradictions (`BRIEF.md` §10) and TASK-0011 exists to adjudicate it rather
  than to guess.
- **Pause equals stop.** `src/jls/sim/BatchSimulator.java:75-78` is `stop()` and
  `:87-90` is `pause(boolean)`; both set `stopping = true` and nothing else. The
  javadoc at `:82` states the position plainly - "It doesn't make sense to pause
  it in batch mode" - so this is a decision to reopen with a reason, not a bug
  to fix quietly. The reason is FEAT-035: a checkpoint needs a suspend point.
- **Memory capacity.** `Memory.DenseWordStore`
  (`src/jls/elem/Memory.java:1072`) is the dense store, and
  `src/jls/elem/Memory.java:65` records that an invalid capacity crashes it at
  simulation start. Linux needs at least 12 MiB for a usable shell and 16 MiB is
  recommended; a 32-bit-word dense store reaches 16 MiB at 2^22 words. They meet
  with zero headroom (`BRIEF.md` §3).

A separate and unrelated ceiling worth naming so nobody conflates them:
`FileAbstractor.MAX_CIRCUIT_TEXT_BYTES` is 64 MiB
(`src/jls/FileAbstractor.java`), a deliberate anti-decompression-bomb bound from
issue #38. That is a *file* bound, not a simulation bound, and it belongs to
FEAT-013's bulk-image section, not here.

Decision D6 applies to the three defect items; TASK-0013's byte budget is shared
with FEAT-036 and sequences with it.

## Risks

- **The past-limit drop may be load-bearing.** If any golden depends on the
  current behavior, "fixing" it changes results. TASK-0011 is explicitly an
  adjudication task for this reason, and the recorded decision must precede the
  code change.
- **An unbounded run is an unbounded resource commitment.** Criterion 4's
  heartbeat and clean interrupt are what make it operable; shipping the no-limit
  mode without them produces runs nobody can supervise.
- **Byte-budgeted memory changes a user-visible configuration surface.** An
  existing circuit configured by word count must keep loading and meaning the
  same thing. This is a format-adjacent change and must be reviewed against
  `docs/file-format.md` rather than decided inside the element.

## Evidence

- Default time limit: `src/jls/JLSInfo.java:69`
  (`defaultTimeLimit = 100000000`), consumed at
  `src/jls/sim/Simulator.java:38`, `:106`.
- The loop and the drop: `src/jls/sim/Simulator.java:217` (loop condition),
  `:225` (`dupCheck.remove(event)`), `:231-232` (the `maxTime` clamp).
- Pause is stop: `src/jls/sim/BatchSimulator.java:75-78` and `:87-90`, with the
  position recorded at `:82`.
- Memory store: `src/jls/elem/Memory.java:1072` (`DenseWordStore`), `:65` and
  `:374` (capacity validation notes).
- Guest RAM minimum and the zero-headroom coincidence: `BRIEF.md` §3.
- The past-limit drop as an open contradiction: `BRIEF.md` §10.
- The unrelated 64 MiB file bound: `src/jls/FileAbstractor.java`
  (`MAX_CIRCUIT_TEXT_BYTES`, issue #38).
- Normative event model (not restated here): `docs/simulation-semantics.md`.
- **Cost reconciliation.** Band 3-5 mw. Tasks named for it: TASK-0011,
  TASK-0012, TASK-0013, TASK-0014, totalling 4 wk. Band and task sum agree; no
  reconciliation is needed. Shared tasks counted once at the task level:
  TASK-0013, TASK-0014.
