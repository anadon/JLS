# FEAT-030 - Engine constant factors: the semantics-preserving stack

**Status:** proposed | **Cost:** 12-20 mw | **Owner program:** P1 |
**Spine rank:** S24

## Capability delivered

Every event the simulator retires costs measurably less, and nothing about what
the simulator computes changes. The entire existing golden corpus stays byte-
identical - not "equivalent", not "modulo known differences", byte-identical -
while the measured 318 ns per event comes down by a factor the roadmap prices at
2.26x. That converts a structural boot from 1.66-1.72 h to 44-46 min and a
character echo from 1.5 s to 0.66 s, which is the difference between a capstone
that fits a nightly lane and one that does not.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | 1.66-1.72 h becomes 44-46 min at 2.26x, and every golden must stay byte-identical |
| CAP-03 | required | roughly 30 minutes structural is what makes the ternary acceptance test fit a nightly lane |
| CAP-09 | beneficial | verification means running the same design many times |
| CAP-01 | beneficial | three simultaneous editors on one circuit raise per-edit cost, and K9's floor is continuous |
| CAP-04 | beneficial | the physical simulation runs the same machine at package granularity, so event cost is the throughput budget |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-009 | A speedup claim without measured constants is arithmetic on a guess. The budget-and-ratchet gate is what makes "2.26x" a number a test can assert and a regression a build failure |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0063 | Calendar queue with an intrusive queued flag | Replaces the priority queue and the duplicate-check set, which together are 47.7% of per-event cost |
| TASK-0064 | Zero-delay closure | Collapses events that model no elapsed time without changing any per-element propagation delay |
| TASK-0056 | Widen the value permits and migrate the value representation | Shared with FEAT-026 and FEAT-028: the plane-encoded width-carrying value is what removes the allocation churn |
| TASK-0023 | Measure the behavioral binding and the levelized cost at scale | Shared with FEAT-009 and FEAT-031: supplies the node-count and pass-count figures every derived speed claim must state |
| TASK-0026 | The simulation budget and allocation ratchet | Shared with FEAT-009: the standing gate that asserts byte-identity of the whole golden corpus across any engine change |

## Acceptance criteria

1. **The whole existing golden corpus is byte-identical** across every change in
   this feature. This is a gate, not a goal: a change that cannot achieve it is
   reverted, not documented.
2. Events per clock cycle on the committed calibration fixtures is asserted as a
   hard **equality**, not a bound - an engine change that alters the event count
   has altered semantics.
3. Nanoseconds per event and bytes allocated per event are asserted against
   declared bands, measured by the method FEAT-009 fixes, and the bands ratchet
   down and never up.
4. Total event ordering is preserved exactly by the calendar queue. A property
   test over generated event streams asserts the polled order matches the
   priority-queue order for the same input.
5. Per-element propagation delays are observably unchanged: the delay table in
   the normative semantics document still describes the shipped behavior, and
   the zero-delay closure touches only events that model no elapsed time.
6. Every speed figure published anywhere states node count and pass count, or it
   is not published.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | depends on / overlaps - #232's value-representation half is shared with FEAT-026 via TASK-0056; #232 does not cover the queue or the zero-delay closure |
| - | the queue, the zero-delay closure and the budget ratchet | **no issue** |

Recorded decision #221 (the discrete-event interpreter as the sole simulation
strategy) is **not** reopened by this feature and must not be cited as open. Its
revisit trigger belongs to a compiled or cycle-based strategy, which this
feature deliberately is not - about 48% of per-event cost is removable without
touching semantics, and that is the entire scope here.

## Design notes

**Kept separate from FEAT-005 deliberately.** The quadratic and materializing
I/O paths touch adjacent files, but FEAT-005 is ordinary defect work landing
immediately under decision D6 while this is a gated program. Merging them would
gate a two-week fix behind a five-month effort.

The cost is measured and localized. The warm event loop retires 3.14 M events/s
at 318 ns/event, of which queue plus dedup bookkeeping is 151.8 ns - 47.7%. The
mechanism is visible in one place: `src/jls/sim/Simulator.java:25` is a
`PriorityQueue<SimEvent>` and `:27` is a `HashSet<SimEvent>` used purely as a
duplicate check, with `post` adding to the set at `:167` and the loop removing
at `:225`. A time-bucketed calendar queue with an intrusive queued flag on the
event replaces both data structures and the set membership test at once - and
that is why the two halves are one task.

The allocation half is the same migration FEAT-026 is doing for semantic
reasons: about 50% of in-loop allocation among named non-`byte[]` classes is
value churn, and `SimEvent.PinChanged` is a zero-field record allocated 1.92 M
times per run. TASK-0056 is shared for this reason; this feature depends on that
task, not on all of FEAT-026.

Kill criterion K3 governs and is not negotiable: if byte-identical output across
the entire golden corpus cannot be achieved, **this feature stops at the failing
change**. No semantic change is permitted to buy speed, and there is no partial
credit.

What this feature does *not* do is move the behavioral tier. Engine work accrues
entirely to the structural tier; the behavioral row does not move
(`BRIEF.md` §13). Anyone quoting the 2.26x against a behavioral binding is
quoting it wrong.

## Risks

- **The golden corpus is the only witness.** If it is not comprehensive, byte
  identity across it proves less than it appears to. FEAT-009's tracked
  calibration fixture is what raises the corpus from "34 simulated cycles and 4
  assertions" (`BRIEF.md` §7) to something worth asserting against, which is why
  FEAT-009 is a hard prerequisite rather than a nicety.
- **Derived-figure errors are the default failure mode.** Two agents in the
  evidence corpus misread the levelized figure by 4.6x by counting logic
  elements as nodes and one pass instead of two. Criterion 6 exists because this
  mistake was made twice on this exact number.
- **2.26x does not reach the live-console requirement.** After this stack the
  echo path is about 44,000 cycles/s against a 1e5-1e6 requirement - missed by
  1.2-5x. That gap is a decision plus roughly 30-45 further maintainer-weeks,
  not a physical limit, and it belongs to a strategy this feature does not open.
- **Bus factor 1 and a five-month program.** 12-20 mw with a hard
  no-partial-credit gate is the highest-variance schedule item in the core
  column. The demo slices of CAP-02 and CAP-03 should be written so they degrade
  to a longer run rather than to no run.

## Evidence

- Queue and duplicate-check mechanism: `src/jls/sim/Simulator.java:25`, `:27`,
  `:165-170` (`post`), `:217-232` (the loop, including the `dupCheck.remove`
  at `:225` and the `maxTime` handling at `:231-232`).
- Measured constants: `BRIEF.md` §13 - 3.14 M events/s and 318 ns/event in the
  warm event loop, queue+dedup 151.8 ns of 318 (47.7%);
  `keystone-c-performance.md:126,136`.
- The 2.26x consequence: `BRIEF.md` §13 - nommu boot 1.66-1.72 h becomes
  44-46 min; echo 1.5 s/char becomes 0.66 s/char;
  `keystone-c-performance.md:474,490-495,685-697`.
- Allocation: `BRIEF.md` §13 - about 50% of in-loop allocation among named
  non-`byte[]` classes; `SimEvent.PinChanged` allocated 1.92 M times/run.
- The no-partial-credit gate: kill criterion K3, `03-determination.md` §9.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S24 (12-20 wk).
- Separation from FEAT-005: registry deduplication record item 16.
