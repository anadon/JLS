# Issue #476: TASK-0063: the two data structures that cost 47.7% of warm loop time are replaced by a calendar queue and an intrusive queued flag, retiring the identical event sequence
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated goal is a constant-factor win on `runEventLoop`. The real goal, read against
`docs/capability-roadmap/keystone-c-performance.md` §3, is to stop the engine spending
**95% of its time on bookkeeping and 4.9% on digital logic**. That is the right target, and
this issue's §7.10 order-reproduction proof and stage-4 clear-before-`react` edge are the
best-argued paragraphs in the engine programme. I would not weaken P3 (identical retirement
sequence) or P6 (byte-identical goldens) by one line.

But the issue frames the work as *replacing two structures with two structures*. That framing
is what produces the awkward parts: a separate flag whose exact semantics must be re-derived
by hand, an Open Question whose recommended default has an unstated cost, a benchmark pinning
the wrong axis, and a dependency graph that two sibling issues describe in opposite directions.
One reframing removes all four.

## Reframing 1: it is one structure, not two — because `equals` and `compareTo` share a first key

`compareTo` is `(time, seq)`. `equals` is `(time, callBack, todo)`. **Both are keyed on time
first.** The issue notices this for ordering and misses it for suppression, so §7.5 has to
invent a global "intrusive queued flag reachable from `(callBack, todo)` identity without
hashing" and then admit "the exact shape is an implementation choice".

Cut along the seam the two contracts already share: a bucket is not a list, it is a **small
ordered pending set** — arrival-ordered for `poll`, membership-tested for `post`. Because the
bucket *is* the timestamp, membership within it needs only `(callBack, todo)`, which is
exactly what `SimEvent.equals` compares once time matches. Consequences:

- **Stage 4 stops being a hazard and becomes structure.** `poll` removes the head from the
  bucket, which *is* removal from the pending set — one operation, not two. A flag cleared
  after `react` becomes unrepresentable rather than merely tested-against. T2 calls this "the
  single subtlest behaviour in the file"; the right response to a subtle invariant is to make
  it impossible to violate, not to write a test for it.
- **H3 dissolves.** The question "can the flag be reached without hashing?" becomes "is a
  linear `equals` scan over one bucket cheaper than a hash?" — and the census answers it:
  2,331,793 events over 6004 cycles is ~388 events/cycle spread over several timestamps, so
  per-bucket occupancy is tens, not thousands. A scan over an `ArrayDeque` at that size beats
  `HashMap.putVal` and drags no `BitSet.hashCode` into the loop. That is a measurement, and
  it is a much smaller one than the issue currently sets up.
- **T3 disappears.** `PinChanged` equality is just `equals` being called; there is no separate
  keying scheme that could get it wrong.
- **§7.11's "event already pending cannot reach `add`" assertion goes away** — `post` and
  membership are the same lookup, so there is no two-call window to assert about.

The issue's own §7.4 signature (`add/poll/peek/isEmpty/size/clear`) survives unchanged; only
`post`'s body changes from two calls to one. This is strictly less code, strictly fewer proof
obligations, and it deletes `dupCheck` as the issue intends.

## Reframing 2: OQ1's recommended default has an unpriced O(#buckets) step

"A hash map from time to an append-only list, with a cached minimum" is only sparse-safe for
`add`. When the minimum bucket drains, **recovering the next minimum is a scan of the key
set** — and `SigSim.initSim` posts the *entire* `-t` vector at construction time
(`src/jls/elem/SigSim.java`, the two `sim.post(new SimEvent(newTime,pin,…))` sites), which on
`k2000` is 12,093 pending events spread over thousands of distinct future timestamps
(keystone-c §7.1). Option (a) therefore pays O(distinct future times) per bucket exhaustion on
the flagship workload. That is not a tuning constant; it is an asymptotic hole in the
recommended default.

Two better shapes, both worth measuring: a `TreeMap<Long, Bucket>` (O(log *distinct times*),
which is already far better than today's O(log *events*) heap), or a ladder/hierarchical
timing wheel if the census justifies it. Note what this makes visible: **the correct bucket
store depends on whether the stimulus vector is streamed**, and streaming (keystone-c §7.2
item 1 — post only the next transition per pin) is *unowned*: #376 fixes `SigSim`'s quadratic
`String` concatenation and explicitly does not touch the posting pattern. So OQ1 cannot be
resolved honestly without either filing that work or measuring under both regimes.

A related credit the issue never claims: keystone-c §7.1 measures ~60 ns/event of pure *depth*
tax (82 ns poll+add at depth 64 vs 142 ns at depth 12,000). Bucketing makes depth free by
construction. That is ~40% of the 151.8 ns headline, it is caused by a fixture-shaped posting
pattern rather than by any circuit, and it is the strongest single argument for this task.
Lead with it.

## Reframing 3: P7 pins the axis that is already safe and leaves the dangerous one open

`SameTickScalingBenchmark.sameTickCostIsNearLinearInBurstSize()` guards against #231's
quadratic returning. Under any append-to-bucket design same-tick is linear *by construction* —
the benchmark can only pass. The axis that can actually degrade is the one T4 names and P7
omits: **many distinct sparse timestamps**, which is precisely what the un-streamed `-t`
vector produces. Add a distinct-timestamp-count benchmark (fixed burst, 1k/4k/16k distinct
future times) or P7 is ceremony.

## Reframing 4: the dependency edges are asserted in both directions, and the shared mechanism is not shared

#393 §Status says TASK-0063 "is a **real** prerequisite: the closure's 'already evaluated in
this cone at this timestamp' marker **is** that intrusive flag", and its DoD requires "the cone
marker is TASK-0063's intrusive flag, not a second per-node hash set". This issue says #393 is
merely `related` — "independent work over the same loop… whichever lands second rebases".

Both cannot be right, and on inspection **#393's premise is wrong**: a cone-visited marker is
keyed by *element*, scoped to one sweep, and cleared when the sweep ends; the pending-set
predicate is keyed by `(time, callBack, todo)`, scoped to the interval between `post` and
`poll`, and carries `equals` semantics. They share a word, not a mechanism. Under Reframing 1
they are not even the same kind of object. The edge should be dropped from #393 rather than
added here.

The *real* coupling between the two runs the other way and neither issue states it: **#393
removes ~82% of the traffic this task is optimising.** `PinChanged` is 1,919,891 of 2,331,793
events and models no elapsed time (keystone-c §6.3). Land #393 first and the queue carries
~412k postings instead of 2.6M, the 12,093-deep heap collapses to the timed working set, and
the 47.7% share is re-based to something much smaller. The two are non-additive, so #362 §6's
claim that they can run concurrently is a scheduling assertion about merge conflicts being
sold as one about value. P10 must therefore state its denominator's *provenance*, not just its
scope: "47.7% of a loop that still queues zero-delay events" is a different number from
"47.7% of the loop we intend to ship".

I am not arguing #476 should wait on #393 — the bucket design is the cleanest thing to build
under either ordering, and #393's soundness argument is harder than this one. I am arguing the
two issues currently disagree about their own edge, and that #476's abstract claims a share of
the profile that a sibling is designed to evaporate.

## Where this sits in the project's arc

Honest framing, from keystone-c §0: the queue "is not the keystone… no standard is blocked on
it, no pedagogy depends on it, and it unlocks nothing." Compare the value type (#232/#878/#879),
which unlocks four-state logic, IEEE 1164 projection, HDL fidelity and — per keystone-c §6.2 —
**80% of any future levelized pass's win**. This task is pure constant factor, entering the
strictest JaCoCo (0.930/0.920/0.845) and PIT (80/82) gates in the tree, with three bespoke
fixtures and a benchmark lane, for a win whose true size is unknown until #393 and the
streaming question are settled.

That is still worth doing, for two reasons the issue does not give: it is the only change here
that makes queue depth structurally free, and it **retires `seq`**. Once ordering comes from
bucket arrival order, `compareTo` has no caller, and `SimEvent.sequence` — the mutable static
that keystone-c §7.2 calls "a hard barrier to any future parallel or re-entrant evaluation",
and that the adversarial comment §5 correctly refuses to touch inside this task — becomes
deletable in a follow-up with no ordering consequence at all. That dissolves the
per-`Simulator`-counter argument rather than adjudicating it, and it is a better closing
sentence for §13 than "the queue is replaced".

## Disregarded, and why

I am setting aside three of the issue's stated positions:

1. **OQ1's option list and its recommended default** — option (a)'s min-recovery cost is
   unpriced and the workload is adversarial to it (Reframing 2). Add the ordered-key-map option
   and make the streaming regime an explicit parameter of the measurement.
2. **"Calendar queue + intrusive flag" as the design** — one time-bucketed pending set does
   both jobs, makes stage 4 structural, and dissolves H3 and T3 (Reframing 1). H1's proof is
   unaffected; H2 becomes trivially true.
3. **`related: 393 — independent, whichever lands second rebases`** — the mechanism-sharing
   claim in #393 is false and the traffic-sharing dependency is real and unstated (Reframing 4).

Everything else stands, and P3's three named ordering hazards, P6's byte-identity gate, P9's
two-runs-in-one-JVM check and T1's warning against bucketing on `seq` should be carried into
the reframed design unchanged.
