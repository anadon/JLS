# Issue #688: TASK-C532-1: the producer-to-consumer relation the scheduler discards is retained in a bounded ring buffer, behind a seam and at zero cost when nothing is watching
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The end is right and it is one of the most valuable things JLS could gain. Today the
tool answers "what is this value now" (`ElementValueDisplays.show` →
`Element.showCurrentValue`) and nothing else; "trace it back" — which
`docs/capability-roadmap/sweep-01-values-and-logic.md` calls "the single most
transferable debugging skill in RTL" — is entirely manual. Retaining causality is the
substrate under #532's inspector, under X-source tracing, under a headless
`--why 'net@t'` that would make causal debugging *gradeable and diffable*, which is
the thing no competitor ships. I want this capability built.

I am nonetheless disregarding acceptance criteria 1, 4 and 5 as written, and
substantially rewriting 3, because the project has already done this design study and
this task contradicts it.

## The project already decided against this design

`docs/capability-roadmap/lf-03-causal-debug.md` is the recorded study for exactly this
capability. Its §(a) says, in its own words:

> **Decision: journal at net-change granularity, not per-event.** … Per-event
> journalling is the obvious design and it is wrong.

with the census as the reason: on the 6004-cycle CPU run, 1,919,891 of 2,331,793
fired events are `PinChanged` — a zero-field record (`src/jls/sim/SimEvent.java:30-31`)
carrying no information beyond "something upstream moved". #688 proposes to spend a
bounded retention budget storing four fields about each of those. Four out of five
slots in the ring buffer would hold nothing a human or an inspector can use, and the
window that AC 5 fears will be too short to hold a glitch chain is short *because of
them*.

The doc's record is `NetChange(time, siteId, value, producerId, causeIndex, kind)`,
keyed on a dense **site index** — and it states a hard ordering constraint that #688
does not mention: "the journal's site index must be designed **before** the
levelization's slot table, because they are the same table." #688's tuple
(producer, consumer, element, delay) has no site identity at all. Building it first
does not defer that decision; it forecloses it in the wrong direction and guarantees a
second, disagreeing index later.

## The relation is not "computed and discarded" — it is ambient, and three of the four fields are derivable

`Simulator.post` (`src/jls/sim/Simulator.java:165-170`) never computes a producer. The
producer is simply *the event currently reacting* — `runEventLoop` holds it in a local
at line 224 and drops it at 242. Capturing it is a field on `Simulator` plus one
assignment; nothing is being recovered, only written down.

And having written it down, the rest falls out. For an edge parent→child:

- **consumer element** = `child.getCallBack()` — already on the child.
- **consumed `propDelay`** = `child.getTime() - parent.getTime()`. Every posting site
  is `now + propDelay` on the reacting element itself (`Gate.java:708`,
  `Mux.java:547`, `Adder.java:410`, `Register.java:768`), and `WireNet.propagate`
  posts at `now` (`WireNet.java:507`), so the delta is exactly the delay, and zero
  correctly denotes the wire hop.

So AC 1's four-field record is really **one reference per event**. That matters
because the whole apparatus of AC 4 — a capacity constant derived by measuring "events
retained per simulated tick on the shipped hazard demo" — exists to ration a record
three-quarters of which is redundant. Ration the right thing and the constant may not
be needed at all: lf-03 puts a *full-fidelity* journal of the entire flagship RV32I run
at ≈9 MB, and "a classroom circuit's whole run fits in tens of kilobytes". The
recorded workload (#221: classroom-scale gate circuits are the present workload) does
not need a ring buffer. It needs an append-only list.

## The bounded window is the wrong answer to a problem lf-03 already solved

AC 5 pre-declares the failure mode — the window cannot hold the chain for the very
glitch the analyzer caught — and schedules a re-scope to "trigger-window retention" as
a *finding*. That finding is already in the tree. lf-03 §(b) works out checkpoint and
replay in full: `SimCheckpoint`, `restore`, `runUntil`, an adaptive interval by fired-
event count with a logarithmic retention ladder, and the sizing — the entire simulation
state of a 1,551-element RV32I CPU is ≈9 KB raw, 2-3 KB deflated. Its §"Why (a) and
(b) belong in one program" states the exact conversion #688 is groping toward:
"Retain the journal only in a window around the cursor; when the student scrolls back
past the window, restore the nearest checkpoint and replay **with journalling on**…
That converts causal tracing's storage from O(run) to O(window)." Oldest-first drop
with the drop "visible to callers" is a worse version of that: it loses history the
user is about to ask for and reports the loss instead of regenerating it.

**The reframing that makes the problem disappear.** JLS's determinism is a written
contract (`docs/simulation-semantics.md` §3: "a pure function of circuit content";
#181's stable-id seeding; #442's byte-identity gate; #476's P9). A deterministic
simulator does not need to *retain* causality — it needs to be able to *reproduce* it.
Record nothing on the hot path; when the inspector opens on a transition at time T,
replay to T with journalling enabled and keep everything. Then:

- "Zero cost when nothing is watching" stops being a measured tolerance (AC 2's
  `ChronogramClosedCostTest` bound) and becomes structural — there is no retention
  code in the default loop to measure.
- Truncation cannot lose the chain, so AC 5's failure mode is not merely unlikely, it
  is unreachable.
- The capability composes with time travel instead of duplicating a slice of it.

Cost: replay latency, bounded by checkpoints. Without checkpoints, replay from t=0 is
milliseconds at classroom scale and unacceptable at RV32 scale (1.66 h boot). So the
honest sequencing is: journal + site index first, checkpoints second, and the ring
buffer never. The one genuinely new requirement replay imposes is a stimulus log for
interactive runs (input-pin toggles), which is O(user actions) — tiny, and independently
worth having.

## The seam is already in the tree, twice, and AC 3's test is a proxy for it

AC 3 asks for an accessor "defined over event identity and value" pinned by a test that
"asserts it names no concrete queue type". That is a grep standing in for a structural
property the codebase already knows how to express two ways:

1. **The mode-hook vocabulary (#25).** `Simulator.afterEvent(SimEvent)`
   (`Simulator.java:269`) is already the per-retirement observation point, already
   consumed by `BatchSimulator.afterEvent:140` and `InteractiveSimulator.afterEvent:879`,
   and already queue-agnostic by construction. `probeSample` is the same pattern for
   nets. A retention consumer that lives here cannot name a queue type because it is
   never handed one.
2. **The typed seam catalog (#223, `docs/extension-points.md`).** `collab.op-observer`
   → `jls.collab.op.OpSink` ("register, then observe every submit") is the exact
   precedent. A `sim.journal-sink` row with a `jls.sim` contract gets cardinality,
   lifecycle phase and `ExtensionPointCatalogTest` cross-checking for free, and a
   `static final` no-op default makes "zero when nothing is watching" a property of the
   type system rather than of a benchmark's tolerance.

Relatedly, lf-03's observation that "the trace machinery is one field short of being a
causality log, twice" is the cheapest path in the tree: `TraceSample(long time, BitSet
value)` (`src/jls/sim/TraceSample.java:19`) and `Trace.Change(BitSet value, long when)`
(`src/jls/edit/Trace.java:51`) are append-only per-signal histories written at sites
that *already hold the `SimEvent`*, and `Trace` already has the binary search the
backward walk needs (`firstChangeAtOrBefore`, `Trace.java:445-458`, unit-tested) and
already has a retention bound (`MAX_RETAINED_CHANGES = 100_000`, `Trace.java:32`).
#688 would build a second, parallel, differently-bounded history alongside them — and
#532's own boundary note forbids exactly this shape of duplication ("never two
cause-chain models").

## One more pull against the arc: this instruments the function #476 is about to delete

`Simulator.post` is four lines, and #476 replaces both structures inside it under a
byte-identity gate, a 0.930/0.920/0.845 JaCoCo rule and 80/82 PIT thresholds, in
service of a measured 151.8 ns of 318 ns per event. #532 AC 5 and #508 both say the
inspector lands *after* #476's seam. #688 orders itself after TASK-C527-1 instead and
proposes to add a retention branch to `post`/the retirement path first. That forces
#476 to rebase its hottest function onto instrumentation and muddies the attribution
#476's threat T5 is explicitly protecting. Whatever design wins, it should land on the
other side of #476.

## What I would file instead

Split #688 into two tasks under #532, sequenced after #476:

- **TASK-C532-1a — the site index and the journal record.** Define the dense value-site
  index (Puts and nets, keyed off `Element.java:24`'s permanent id) and `NetChange` per
  lf-03 §(a), written from the existing `afterEvent`/`probeSample` seam, gated on a
  `static final` no-op sink. Deliverable is `jls -b --why 'net[bit]@T'` printing the
  causality tree deterministically to stdout — headless, diffable, CI-testable, and a
  complete user-visible capability with no GUI attached.
- **TASK-C532-1b — window regeneration.** `checkpoint`/`restore`/`runUntil` on
  `Simulator` (lf-03 §(b)), so the journal window is regenerated rather than rationed.

Keep from #688 as written: AC 2's insistence that the closed-path cost be *shown* red
against an unconditionally-enabled build before any pass counts, and AC 3's instinct
that the consumer must not see the queue. Discard AC 1's per-event ring buffer, AC 4's
capacity constant, and AC 5's scheduled rediscovery of a decision already recorded.
