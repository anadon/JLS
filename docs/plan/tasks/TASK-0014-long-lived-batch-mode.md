# TASK-0014 - Long-lived batch mode with pause, heartbeat and clean interrupt

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A batch run that lasts hours can be observed while it runs, paused and resumed,
and interrupted without losing its output.

1. **`pause` stops being `stop`.** `BatchSimulator.pause(boolean which)`
   (`src/jls/sim/BatchSimulator.java:81-90`) sets `stopping = true` and ignores
   its argument, which makes it byte-for-byte the body of `stop()`
   (`:73-78`). Implement it against the engine's existing seam: the base
   `Simulator.beforeEvent()` hook (`src/jls/sim/Simulator.java:244-256`)
   returns `true` and is documented as the place "a mode can block (pause)".
   Override it in `BatchSimulator` to park on a monitor while a `paused` flag
   is set, and have `pause(false)` notify it. The event loop's
   `if (!beforeEvent()) continue;` (`:220-222`) already re-checks
   `stopping` and the queue on wake, so no loop edit is required.

2. **A progress heartbeat on stderr.** A `-heartbeat <seconds>` flag
   (a new `FlagSpec` row in `src/jls/JLSStart.java:758-786`, `Arity.REQUIRED`)
   makes the run emit one line per interval to **stderr** carrying simulated
   time, wall-clock elapsed, events retired since the last beat, and queue
   depth. It is computed and written inside `beforeEvent()` — on the
   simulation thread — so it never touches the queue from outside.
   stdout is untouched: `docs/batch-interface.md` §3 and §6 (`:128-207`,
   `:324-336`) freeze every byte a conforming consumer sees there.

3. **A clean interrupt.** Install a shutdown hook (batch path only,
   `src/jls/JLSStart.java:245-280`) whose *entire* body is
   `sim.stop()` — a write to the `volatile boolean stopping` field
   (`src/jls/sim/Simulator.java:38-45`, volatile precisely so a non-simulation
   thread may set it) — and a join with a bounded wait. The loop then exits
   normally, `displayOutcome()` prints `Simulation Stopped at <t>`
   (`src/jls/sim/BatchSimulator.java:564-566`), `displayResults` runs, and any
   requested VCD is written (`src/jls/JLSStart.java:258-279`). The reason
   string is already one of the four frozen outcomes
   (`docs/batch-interface.md:139`), so the contract does not move.

4. **The three flags are rejected in incoherent combinations at parse time.**
   `-heartbeat` without `-b` is a usage error, exit 2, in the established
   `usageError` form (`src/jls/JLSStart.java:1137-1147`).

5. **`docs/batch-interface.md`** gains a section for stderr: what the heartbeat
   line contains, that it is stderr-only, and that interrupt produces the
   already-specified `Simulation Stopped` outcome. §6 makes this an addition,
   not a break.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-006 | "Resumable, interruptible" is the feature's capability sentence. A multi-hour run that cannot be paused and whose only interrupt is `SIGKILL` discards its VCD and its stdout. |
| FEAT-035 | Checkpointing needs a defined quiescent instant at which the engine is not mid-event. The parked `beforeEvent()` state *is* that instant, and the checkpoint writer will take it rather than invent a second one. |

## Prerequisite tasks

None. TASK-0012 makes the long run *reachable* and this task makes it
*survivable*; neither reads state the other creates, and each is independently
useful.

## Acceptance test

`test/jls/sim/BatchPauseAndInterruptTest.java`, new:

- `pauseParksTheLoopAndResumeContinues()` — starts a free-running fixture on a
  worker thread, calls `pause(true)`, samples `now` twice across a bounded
  wait and asserts it did not advance, then `pause(false)` and asserts it did.
  **Fails at HEAD**: `pause(true)` ends the run, so the second sample equals
  the first forever and the resume assertion never passes.
- `pauseIsNotStop()` — asserts that after `pause(true)` the outcome, once the
  run is later allowed to finish, is **not** `Simulation Stopped`.
- `interruptPrintsTheStoppedOutcomeAndStillWritesTheVcd()` — drives the batch
  entry point in a subprocess (the `CliFlagTableTest` idiom,
  `test/jls/CliFlagTableTest.java:45-70`), signals it, and asserts stdout's
  last line matches `Simulation Stopped at \d+` and that the `-vcd` file
  exists and parses.
- `heartbeatGoesToStderrAndStdoutIsByteIdentical()` — runs a fixture twice,
  once with `-heartbeat` and once without, and asserts the two stdout streams
  are byte-identical while only the first stderr is non-empty. This is the
  test that protects the §6 promise.

`test/jls/CliFlagTableTest#usageDocumentsExactlyTheParserFlags()` and
`#everyTableFlagIsAcceptedByTheParser()` cover the new flag automatically once
the `FlagSpec` row exists; both must be green.

## Related GitHub issues

**No issue.** `BatchSimulator.pause` being identical to `stop` is recorded in
the study corpus (`BRIEF.md` §7, "major") and nowhere in the tracker.

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on — a CPU-scale headless run with no heartbeat is indistinguishable from a hang |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine (Digital-parity, HDL-independent) | overlaps — a GUI front end over a long batch run needs exactly this pause/progress surface; #214 does not close on it |

## Notes

- **The shutdown hook must touch nothing but `stopping`.** `Simulator.post`
  (`src/jls/sim/Simulator.java:158-170`) is unsynchronized over a plain
  `PriorityQueue` with a single-thread contract, and `dupCheck` is a plain
  `HashSet`. A hook that tried to drain, flush or inspect the queue would race
  the simulation thread. Everything the hook wants to *do* happens on the
  simulation thread after the loop returns.
- **Shutdown hooks get a bounded budget from the JVM.** The hook must set the
  flag and then wait on the simulation thread with a timeout; if the timeout
  expires, exit anyway rather than block the JVM. Say which timeout in the
  javadoc.
- **The parked thread must be interruptible.** A `pause(true)` with no
  subsequent `pause(false)` plus a signal must still terminate: check
  `stopping` in the park predicate, not just `paused`.
- **`InteractiveSimulator` already has a working pause** — its own loop and
  the `now >= maxTime` checks at `src/jls/edit/InteractiveSimulator.java:401`
  and `:657`. Read it before designing the batch one; do not copy its
  Swing-timer structure into a headless class, because
  `test/jls/HeadlessCoreRatchetTest` exists to keep AWT out of the core.
- **Heartbeat cadence is wall clock, not simulated time.** Deriving it from
  `now` makes it useless exactly when the run is slow, which is when it is
  wanted.
- **`TellUser` has a batch contract** (`test/jls/TellUserBatchContractTest`).
  If any part of the heartbeat or interrupt path reports through `TellUser`,
  that test governs the stream it lands on; prefer writing to
  `System.err` directly and say so.

## Evidence

- `src/jls/sim/BatchSimulator.java:73-78` (`stop`), `:81-90` (`pause`, with the
  comment "It doesn't make sense to pause it in batch mode" and an ignored
  parameter), `:112-137` (`runSim`), `:562-571` (`displayOutcome`).
- `src/jls/sim/Simulator.java:38-45` (`maxTime`, `volatile stopping` and its
  cross-thread javadoc), `:158-170` (`post`, unsynchronized, single-thread
  contract), `:215-242` (the loop and its `beforeEvent` gate), `:244-256`
  (the `beforeEvent` hook and its "a mode can block (pause)" javadoc).
- `src/jls/JLSStart.java:245-280` — the batch sequence: `runSim`,
  `displayOutcome`, `displayResults`, trace print, VCD write.
- `src/jls/JLSStart.java:758-786` (the `FlagSpec` table), `:1137-1147`
  (`usageError`).
- `docs/batch-interface.md:133-143` (§3.1, the four frozen outcome strings and
  their precedence), `:324-336` (§6, the stability promise and its
  "additions … are minor-version material" clause).
