# TASK-0011 - Adjudicate and fix the past-limit event drop

**Status:** proposed | **Cost:** 3 d | **Blocked by:** none

## Deliverable

A recorded decision plus the code change: an event polled past the time limit is
no longer removed from the duplicate-check set and then discarded.

**The defect.** `Simulator.runEventLoop`
(`src/jls/sim/Simulator.java:216-241`):

```
SimEvent event = eventQueue.poll();      // :224  removed from the queue
dupCheck.remove(event);                  // :225  removed from the dedup set
now = event.getTime();                   // :228
if (now > maxTime) { now = maxTime; break; }   // :231-234  and dropped
```

The event is gone from both structures and was never delivered. `dupCheck` is
the set that stops a duplicate posting from being enqueued twice
(`:167-168`: `if (dupCheck.add(event)) eventQueue.add(event);`), so after the
drop the simulator's two structures disagree about what has been seen.

**Why it is observable.** The interactive simulator lets a user raise the limit
and resume: `InteractiveSimulator.setMaxTime` (`src/jls/edit/InteractiveSimulator.java:549-556`)
reads a new `maxTime` from the time-limit field, and the run continues against
the same queue - it is never cleared between runs (`reset` clears both at
`src/jls/sim/Simulator.java:181-182`, and it is not called here). The event
that was polled at the old limit is permanently lost, so the resumed run is
missing a transition that a fresh run at the higher limit would have. The stop
reason shown to the user (`InteractiveSimulator.java:657`, `"Simulation Time
Limit"`) is correct; the state behind it is not.

**The deliverable, in order:**

1. **A recorded decision** in `docs/simulation-semantics.md` stating what
   reaching the time limit means: whether the simulation is *suspended* (the
   pending event is retained and fires on resume) or *terminated* (the queue is
   conceptually discarded and resuming from a raised limit is not supported).
   Recommendation: **suspended** - it is the behavior the interactive UI already
   implies by offering a raise-and-continue, and it is what a checkpoint
   (FEAT-035) will need.
2. **The code change matching it.** Under "suspended", peek before polling -
   `beforeEvent()`'s javadoc (`src/jls/sim/Simulator.java:246-252`) already
   sanctions peeking and states that "the queue is only modified on this
   thread, so peek-then-poll returns the same event". Test
   `eventQueue.peek().getTime() > maxTime` and `break` **without** polling, so
   the event and its `dupCheck` entry both survive. Under "terminated", clear
   both structures at the break so they cannot disagree.
3. **`now = maxTime`** (`:232`) is retained or removed per the decision, and
   either way documented: it currently pads the clock forward to the limit even
   though no event at that time fired, which is what
   `InteractiveSimulator.java:653-657` calls out as the #51 misreporting hazard
   in the opposite direction.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-006 | Long-run ergonomics rests on being able to stop at a limit and continue; a resume that silently loses an event makes the whole capability untrustworthy. |

## Prerequisite tasks

None. This is a three-day fix in one method plus a semantics paragraph.

## Acceptance test

`test/jls/sim/TimeLimitResumeTest.java`, new:

- `anEventAtTheLimitSurvivesRaisingTheLimit()` - builds a circuit whose only
  activity is a single scheduled change at time T; runs with `maxTime = T - 1`;
  asserts the queue is non-empty and the event is still present; raises
  `maxTime` to `T + 1`; resumes; asserts the change was applied. Fails at HEAD,
  where the event was polled and dropped at the first run's break.
- `theDedupSetAndTheQueueAgreeAfterATimeLimitStop()` - asserts
  `dupCheck.size() == eventQueue.size()` after a limit stop. This is the
  invariant the defect breaks and it is checkable independent of which
  semantics the decision picks.
- `reachingTheLimitStillReportsTheTimeLimitStopReason()` - pins
  `InteractiveSimulator`'s reason string (`:657`) so the fix does not
  reintroduce the #51 misreport.

`test/jls/BatchSimulationGoldenTest` must stay green **unregenerated**: the
batch path sets its limit once (`src/jls/JLSStart.java:249`) and never resumes,
so no golden may move.

## Related GitHub issues

**No issue.** No tracker entry covers the event loop's time-limit behavior.
#232 is the only open issue touching the simulation hot path and it is about
value representation, not the queue.

Recorded decisions, closed, cite as such and not as open: **#25** (the shared
event loop this method is), **#51** (the stop-reason misreport its comment at
`InteractiveSimulator.java:653-656` records), **#95** (the sealed
`SimEvent.Payload`).

## Notes

- **`docs/simulation-semantics.md` is normative and is referenced, not
  restated.** This task **adds** a section to it and must not contradict an
  existing one; check §8.4 (write timing) and the propagation-delay sections
  first, since a change to when the clock advances touches both.
- **`SimEvent` is deliberately non-record and its `equals`/`hashCode` are
  non-structural** - `CONTRIBUTING.md`'s value-semantics bullet names
  `jls.sim.SimEvent` as the standing exception. `dupCheck` is a
  `HashSet<SimEvent>` (`src/jls/sim/Simulator.java:27`), so `remove` and `add`
  depend on that hashing. Read `SimEvent.equals` before assuming set membership
  behaves structurally.
- **`SimEvent.PinChanged` is a zero-field record allocated 1.92 M times per run**
  (BRIEF.md §13). Do not "fix" that here; it is FEAT-030's work and touching it
  changes the measured event constants that TASK-0022 is calibrating against.
- **Do not add a `default:` arm** to any switch over `SimEvent.Payload`;
  `CONTRIBUTING.md`'s sealed-dispatch rule names it explicitly and
  `test/jls/elem/SealedHierarchyTest.java` pins the permits tree.
- **`beforeEvent()` may already have peeked.** The stepping mode uses the peek
  path (`src/jls/sim/Simulator.java:210-213`); a second peek in the loop
  condition is safe on the same thread but must not consume.
- **`stopping` is set by other paths.** `InteractiveSimulator.java:661` sets it
  after computing the reason; a fix that also sets `stopping` at the limit
  would change which reason is reported. Leave the flag alone.
- **This is adjudication first.** The code change is small and either semantics
  is implementable; shipping the fix without the recorded decision leaves the
  next author to re-litigate it.

## Evidence

- `src/jls/sim/Simulator.java:216-241` - the loop; `:224-225` poll and
  `dupCheck.remove`; `:231-234` the past-limit discard and clock pad.
- `src/jls/sim/Simulator.java:167-168` - `dupCheck.add` gating enqueue, so the
  two structures are meant to stay in step; `:181-182` - `reset` clearing both,
  the only place they are resynchronized.
- `src/jls/sim/Simulator.java:27` - `dupCheck` is a `HashSet<SimEvent>`.
- `src/jls/sim/Simulator.java:246-252` - the `beforeEvent` javadoc sanctioning
  peek-then-poll on this thread.
- `src/jls/edit/InteractiveSimulator.java:549-556` - `setMaxTime`, the
  raise-and-resume path; `:653-657` - the stop-reason computation and its #51
  comment; `:401` - the animation timer's own `now >= maxTime` check.
- `src/jls/JLSStart.java:249` - the batch path setting the limit once.
