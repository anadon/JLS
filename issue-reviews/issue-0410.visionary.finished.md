# Issue #410: TASK-0011: an event that arrives past the time limit survives the stop, so raising the limit and resuming continues the run instead of silently skipping a transition
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not a student-facing bug. The stated impact ("a student raises the limit and
resumes; the resumed run omits a transition") is not reachable in the shipped
GUI at HEAD. After the loop exits, the epilogue sets `stopping = true`
(`src/jls/edit/InteractiveSimulator.java:663`) and then `sim = null`
(`:711`), and the only buttons re-offered are Start / Step / Animate
(`:701-705`). All three call `runSim()` (`:318`, `:346-348`, `:410`), which
calls `initSimulation()` (`:603`), which clears the queue and sets `now = 0`
(`src/jls/sim/Simulator.java:181-182`). The `Resume` button only exists while
the sim thread is parked on `pauseSem` — i.e. while the run is *alive*, which
is exactly the case where the limit has not been hit. So there is no shipped
path that resumes against the never-cleared queue O5 asserts; the dropped
event is destroyed a second time by the reset before anyone could observe it.

That does not make the defect fake — it makes it **foundation work**. The
drop matters because #363 (FEAT-035 checkpoints), TASK-0014 (batch pause that
suspends), #324 (FEAT-032 console inside a live run) and P9 time-travel all
require a bounded run that is non-destructive at its bound. Judged that way,
the issue is right about the code and wrong about the shape of the fix.

## The reframe: the engine has no deadline primitive, and the repo already knows it

`stepEnd` and `maxTime` are the same concept — "advance the run until time
D" — implemented twice, in two places, with opposite discipline:

- **stepEnd**, in the GUI hook (`InteractiveSimulator.beforeEvent`, `:776-805`):
  `eventQueue.peek()`, compare `when > stepEnd`, set `now = stepEnd`, return
  false. Non-destructive, pads the clock to the deadline, resumable. Correct.
- **maxTime**, in the engine (`Simulator.java:224-234`): poll, `dupCheck.remove`,
  set `now`, *then* test the bound, discard, pad, break. Destructive.

O7 notices the peek is "sanctioned" by the javadoc; it does not notice that
the peek-and-decline pattern is already **implemented forty lines away**, in a
subclass hook, doing engine work — and mutating the loop's `now` from inside a
hook while it does so. The right change is not to add a second peek to the
engine, it is to lift the one that exists.

The repository has already specified this. `docs/capability-roadmap/lf-03-causal-debug.md:336-338`:

> `void runUntil(long targetTime)` — the existing loop with a bound. This is
> `InteractiveSimulator`'s `stepEnd` logic (`:774-805`) generalized and pushed
> down into `Simulator`, **which is where it always belonged**.

#410 never cites lf-03, never mentions `runUntil`, and never mentions the
stepEnd duplication. It proposes a two-line body edit plus a normative doc
paragraph in the one place the project's own design corpus says a method
belongs.

## Concrete alternative design

```java
/** Advance the run while the head event is at or before deadline.
 *  Returns why it stopped; the pending queue is untouched on DEADLINE. */
protected StopReason runUntil(long deadline) {
    while (!stopping) {
        if (eventQueue.isEmpty())            return StopReason.NO_MORE_ACTIVITY;
        if (eventQueue.peek().getTime() > deadline) { now = deadline; return StopReason.DEADLINE; }
        if (!beforeEvent())                  continue;
        SimEvent e = eventQueue.poll();
        dupCheck.remove(e);
        now = e.getTime();
        beforeReact();
        e.getCallBack().react(now, this, e.getTodo());
        afterEvent(e);
    }
    return StopReason.STOPPED;
}
protected void runEventLoop() { runUntil(maxTime); }   // unchanged signature
```

What falls out, none of which #410's plan buys:

1. **The `now <= maxTime` outer guard disappears.** Today it is a
   post-pad artifact that duplicates the inner test on a different variable.
   One bound, tested in one place, on the head event's time.
2. **`stepEnd` collapses into `runUntil(min(maxTime, stepEnd))`**, deleting
   ~30 lines of GUI code that peeks the engine's queue and writes the engine's
   clock. That is the seam the architecture wants: `docs/grand-architecture.md:314-330`
   puts the loop inside `core` with the GUI reaching it through a
   batched channel, not through a hook that owns half the termination logic.
3. **#51's fix becomes structurally unnecessary.** The epilogue at
   `InteractiveSimulator.java:650-661` *re-derives* the stop reason by
   inspecting `now >= maxTime` and `eventQueue.size()` — guessing at
   information the loop had exactly and threw away. That guess is precisely
   why a completed run could misreport as a time-limit stop. #410's P4 pins
   the guess in place. Returning `StopReason` deletes the guess and turns
   #51's comment-warning-to-future-editors into a type.
4. **TASK-0012's `NO_TIME_LIMIT` sentinel stops being needed.** An unbounded
   run is `runUntil(Long.MAX_VALUE)`. FEAT-006 §6 calls TASK-0011 → TASK-0012
   a *necessity* edge because "the sentinel makes the post-poll clamp
   unreachable"; under `runUntil` there is no clamp to make unreachable, and
   the necessity edge dissolves rather than being satisfied.
5. **#363 gets its suspend point for free.** lf-03 wants a checkpoint at "a
   quiescent point (between events)"; a `DEADLINE` return *is* that point, by
   construction, with no separate adjudication to consume.

## Disregarding two of the stated acceptance criteria, and why

**P5 / Open Question 1 (adjudicate "suspended" vs "terminated" in
`docs/simulation-semantics.md`).** This binary is already closed, and not by
this issue. "Terminated" would make lf-03's `restore(); runUntil(T-ε)` illegal,
would contradict FEAT-006's own recommended default ("re-queue"), and would
require inventing a user-visible refusal (P7) for a workflow the GUI does not
offer. Spending the first deliverable on ruling on a settled question, then
writing a normative paragraph that a test must cite *by name*, is ceremony
around a decision the design corpus made. What belongs in
`docs/simulation-semantics.md` §4 is not "suspended or terminated" — it is the
sentence "a bounded run is non-destructive at its bound: `runUntil(D)` leaves
the pending set intact and `now = D`", i.e. the contract of the primitive,
stated once, covering the time limit, the step bound and the future checkpoint
bound together.

**P3 (dedup-set / queue membership as a normative post-condition).**
`docs/capability-roadmap/keystone-c-performance.md:291-300` measures
`dupCheck` as a net *loss* — dedup-free is 0.649 s vs 0.715-0.778 s with
identical final register state — and FEAT-006 §7 already anticipates that
"FEAT-030's calendar-queue task removes `dupCheck` entirely". Enshrining a
`Q = D` invariant in the normative semantics document, weeks before the
performance keystone proposes deleting `D`, writes a constraint the project
intends to break. Assert it in the test if you like; do not make it doctrine.
(O4's correction — that the *size* form is green at `2d0ca9d` and proves
nothing — is excellent work, and the strongest paragraph in the issue.)

## A better oracle than the four hand-written assertions

§8 proposes four named tests, each pinning one prediction of one probe. The
property the whole feature actually wants is stated in §7.10 and then not
tested: `Φ*_{L2} ∘ Φ*_{L1} = Φ*_{L2}` for `L1 ≤ L2`. Test *that*, over the
existing golden corpus:

> For each golden circuit, run once to completion and record the trace; then
> run the same circuit in K segments with deadlines chosen across the run's
> span, resuming without re-init; assert the concatenated trace is
> byte-identical.

Segmentation is safe here because test vectors and signal generators post
their entire schedules during `initSim` (`docs/simulation-semantics.md` §5),
so a segment boundary introduces no new stimulus. One property test then
covers P1, P2, the clock-pad question (a wrong pad shows up as a trace
divergence rather than needing a separate ruling), the stepEnd unification,
and — for free — FEAT-006 integration criterion 2, "a batch run paused at
event k and resumed produces output byte-identical to an uninterrupted run",
which the feature currently plans to build from scratch at close-out. That is
one artifact serving two tiers instead of four assertions serving one.

## Where the issue pulls against the arc, briefly

- It routes a change through a doc adjudication when the same repo already
  holds the design (lf-03) and the measurement (keystone-c). Cite the corpus;
  do not re-derive it from a probe named `red.LimitProbe`.
- It leaves `runEventLoop`'s termination logic split between the engine and a
  Swing subclass at the moment three other features (#324, #363, FEAT-048)
  queue up to touch the same method — the issue's own §11 flags the collision
  and then declines to remove its cause.
- Its impact section describes a user workflow that `sim = null` at `:711`
  forecloses. Filing foundation work honestly as foundation work costs nothing
  and stops the next reader from hunting a student-visible symptom that is not
  there.

## What I would ship

`Simulator.runUntil(long) -> StopReason`, with `runEventLoop()` as
`runUntil(maxTime)`; `InteractiveSimulator.beforeEvent`'s stepEnd branch
deleted in favour of it; the epilogue consuming the returned reason instead of
re-deriving it; §4 of `docs/simulation-semantics.md` gaining the
non-destructive-bound sentence; and one segmented-replay property test over
the golden corpus. That is the same afternoon of work #410 prices, it makes
the "suspended vs terminated" question unaskable, and it is the piece three
downstream features are each waiting to invent separately.
