# TASK-0063 - Calendar queue with an intrusive queued flag

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0011

## Deliverable

Replace the two data structures that cost 47.7% of warm loop time with one that
preserves the total order exactly.

1. **The queue.** `jls.sim.Simulator.eventQueue`
   (`src/jls/sim/Simulator.java:25`, a `java.util.PriorityQueue<SimEvent>`)
   becomes a time-bucketed calendar queue in a new `jls.sim.CalendarQueue`.
   Buckets are keyed on `time`; within a bucket, insertion order **is**
   ascending `seq`, because `SimEvent.seq` is assigned from a monotonic counter
   at construction (`SimEvent.java:87, 116, 119`) and events are constructed at
   their `post` site. The queue therefore reproduces `SimEvent.compareTo`'s
   `(time, seq)` total order (`SimEvent.java:133-150`) without ever comparing.
   `SimEvent.compareTo` stays, unused by the queue, until a follow-up deletes it.
2. **The dedup set replaced by an intrusive flag.** `Simulator.dupCheck`
   (`:27`, a `HashSet<SimEvent>`), `post`'s `if (dupCheck.add(event))`
   (`:165-170`) and `runEventLoop`'s `dupCheck.remove(event)` (`:225`) all go.
   In their place: a `queued` bit reachable from the callback + payload identity
   without hashing. The coalescing contract is unchanged and is the constraint:
   posting an event equal in `(time, callBack, todo)` to one still **pending**
   is a no-op; re-posting after the original has fired is allowed
   (`docs/simulation-semantics.md` §3). `SimEvent.equals` compares `callBack` by
   **reference identity** and `todo` structurally (`SimEvent.java:162-172`) -
   the flag design must preserve exactly that, including the fact that
   `PinChanged()` is a zero-field record so all `PinChanged` payloads are equal.
3. **`Simulator.post` stays the only enqueue path** (`:165-170`). Nothing else
   in `src/` may touch the queue; the existing invariant is what makes this a
   local change.
4. **A benchmark fixture pinning near-linear same-tick scaling**, in the
   long-run CI lane, so the quadratic cliff cannot come back. Issue #231's
   pilot table (1k/4k/16k/32k events per tick) is the shape.

Done means: the event loop retires the identical event sequence on every
committed fixture, and the queue+dedup share of a warm profile has dropped from
its measured 151.8 ns of 318 ns per event.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-030 | The largest single leg of the semantics-preserving stack: 47.7% of warm loop time is `PriorityQueue` + `dupCheck`. Nothing else in the engine program moves a comparable share. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0011 | The past-limit event drop is an emergent property of the exact code this task deletes: `runEventLoop` polls, calls `dupCheck.remove(event)` at `:225`, then `break`s at `:233` if `now > maxTime` - so the event is gone from both structures and never fires. A calendar queue must implement *some* behavior there, and it cannot implement an unadjudicated one. TASK-0011 records the decision; this task encodes it. |

## Acceptance test

- **`jls.sim.CalendarQueueOrderTest.retiresTheIdenticalEventSequenceAsThePriorityQueue()`**
  (new): run a corpus - **including a cross-coupled latch, a tri-state bus and a
  zero-delay ring** - through both implementations behind the same `Simulator`
  and assert the two `(time, seq, callBack, todo)` retirement sequences are
  element-wise equal. The three fixtures are named because each is a distinct
  same-timestamp ordering hazard; a corpus without them proves nothing.
- **`jls.sim.SimEventDedupTest`** (existing, `test/jls/sim/SimEventDedupTest.java`):
  keep every assertion green against the intrusive flag. Add
  `repostingAfterTheOriginalFiredIsNotSuppressed()` if it is not already there -
  that is the half of the contract a naive "have I ever seen this" flag breaks.
- **`jls.sim.SimEventContractTest`** (existing): the `equals`/`hashCode`
  consistency assertions must stay green even though the hash is no longer on
  the hot path, because `SimEvent` remains a value carrier.
- **`jls.EngineByteIdentityGateTest.everyGoldenIsByteIdenticalAcrossTheQueueSwap()`**
  (new, and reused by TASK-0064 and every later engine change): the whole
  committed golden corpus, asserted byte-identical. This is TASK-0026's gate;
  if TASK-0026 has landed, extend it instead of adding a second one.
- **`jls.sim.SameTickScalingBenchmark.sameTickCostIsNearLinearInBurstSize()`**
  (new, long-run lane): assert the 32k/1k cost ratio is under a committed bound.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 231 | Simulation hot path: `SimEvent.hashCode()` == `(int)time` makes the dupCheck `HashSet` O(n2) in events-per-tick | informs, **closed** (completed 2026-07-27). Its fix was the mixing hash, which is at HEAD (`SimEvent.java:186-192`). This task removes the set the hash was for. Its §10 threat list - identity-hashed callback, structural payload, ordering independent of `hashCode` - is the checklist this task must satisfy. Do not cite it as open. |
| 221 | Decision: simulation execution strategy - discrete-event vs a levelized/compiled evaluation pass | informs, **closed**. Its criterion ("observably identical ... per-element propagation delays") is what the byte-identity gate discharges. A queue swap is not a second strategy and does not engage it. |
| 232 | Simulation hot path: per-signal `BitSet` allocation ... value-typed signal representation | overlaps (open) - the other 37.6% of the same profile. Independent work; do not merge the two. |

**No issue** exists for the calendar queue itself. Recorded as a gap: #232
covers only the value representation, not the queue.

## Notes

- **Trap: `seq` is a `static long` counter shared process-wide**
  (`SimEvent.java:87`, `sequence`, incremented at `:119`). It is never reset -
  not even by `initSimulation`, which clears the queue and the set
  (`Simulator.java:181-182`) but not the counter. Ordering is therefore
  determined by *relative* seq, and any implementation that buckets on
  `seq % something` or assumes seq starts near zero per run is wrong. Two runs in
  one JVM already start from different seq values and produce identical results;
  keep that property.
- **Trap: the duplicate-suppression window is "pending", not "ever".**
  `dupCheck.remove(event)` at `:225` happens **before** `react`, so an element
  that re-posts an identical event from inside its own `react` is not
  suppressed. An intrusive flag cleared after `react` instead of before would
  silently drop those re-posts. This is the single subtlest behavior in the file.
- **Trap: `beforeEvent()` may decline an iteration** (`:220-221`,
  `if (!beforeEvent()) continue;`) and the hook contract explicitly permits a
  mode to **peek** the head before polling (`:209-213`). The calendar queue must
  therefore expose a stable `peek` whose result equals the next `poll`.
  `jls.edit.InteractiveSimulator`'s stepping mode is the consumer.
- **Trap: `PinChanged()` is a zero-field record allocated 1.92 M times per run**
  (BRIEF §13, JFR-measured) and is 1,919,891 of 2,331,793 events (82.3%) on the
  RV32I census. Interning it to a singleton is free and is worth doing here
  rather than as a separate change - but note that `SimEvent.equals` compares
  `todo` with `.equals`, so interning changes allocation, not semantics.
- **Bucket sizing must not be tuned to one fixture.** The census shape - large
  same-timestamp bursts on clock edges, sparse timestamps between - is what the
  bucket policy must serve. Record the policy and its measurement rather than a
  constant.

## Evidence

- `src/jls/sim/Simulator.java:25` (`PriorityQueue`), `:27` (`HashSet dupCheck`),
  `:165-170` (`post`), `:215-243` (`runEventLoop`), `:224-225` (poll then
  remove), `:231-234` (the past-limit break), `:181-182` (`initSimulation`
  clears both).
- `src/jls/sim/SimEvent.java:87, 116, 119` (the static `sequence` counter),
  `:133-150` (`compareTo` over `(time, seq)`), `:162-172` (`equals`, callback by
  identity), `:186-192` (the mixing `hashCode` from #231), `:30-31`
  (`PinChanged` as a zero-field record).
- `docs/simulation-semantics.md` §3 - the normative statement of ordering and of
  duplicate suppression, including "an event is removed from it when polled, so
  re-posting an identical event after the original has fired is allowed".
- BRIEF §13: 3.14 M events/s, 318 ns/event warm; **queue + dedup is 151.8 ns of
  318 (47.7%)**; `PinChanged` 82.3% of events; `SimEvent.PinChanged` allocated
  1.92 M times per run.
- `pom.xml:452-470` - `jls.sim` is floored at 93.0/92.0/84.5, the strictest
  package in the tree, and `pom.xml:781` puts `jls.sim.*` under PIT at 80/82. New
  queue code enters both gates.
