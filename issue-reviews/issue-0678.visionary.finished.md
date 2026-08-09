# Issue #678: TASK-C527-1: the simulation event stream gains a tap seam that costs nothing when nobody is listening, proven red against an unconditional build first
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

#678 is the invisible half of #527's chronogram slice, which is PF-1 of CAP-23 (#504).
Its end is real and I endorse it: CAP-23 wants cross-probing, cause chains and rewind,
and every one of those needs a stable place to stand while #476 rips the `PriorityQueue`
and `dupCheck` out from underneath. Wanting that place to exist before the panel is built
on it is good sequencing instinct.

But the seam already exists, has existed since #25, and is already the thing the
chronogram is fed by. `Simulator.runEventLoop` calls `afterEvent(SimEvent)`
(`src/jls/sim/Simulator.java:241`, declared empty at `:269-270`).
`InteractiveSimulator.afterEvent` (`src/jls/edit/InteractiveSimulator.java:879-896`)
already calls `Trace.addValue` for every watched element and every probed wire, on the sim
thread, live. `BatchSimulator.afterEvent` (`:140-180`) already consumes the same stream for
`-r` and VCD. `probeSample` (`Simulator.java:285-287`) is the second, public, hook for the
net-level half. Between them these two hooks are a retire-path observer interface defined
over event identity and value, in the simulation package, naming no concrete queue type.

That is AC-4 already satisfied and AC-1 already satisfied in the only sense a JVM can
honour. It is also not my inference: **sibling task #390 (TASK-0072) is an entire new
consumer of this stream — a third `Simulator` subclass — and its P6 is a source ratchet
asserting that no member is added to `src/jls/sim/**` at all**, on the explicit hypothesis
(its H2) that `afterEvent` and `probeSample` "at their existing visibilities are
sufficient". #390 and #678 are, at the same evidence commit, making opposite claims about
the same two methods. One of them is building a new consumer with zero engine change; the
other is proposing a maintainer-week or two to add the seam that would let a consumer be
built. #678 loses that argument on the evidence.

Worse, the thing #476 actually threatens is not the tap. Grep the tree for direct queue
access and you find `InteractiveSimulator.java:659` (`eventQueue.size()`),
`InteractiveSimulator.java:776` (`eventQueue.peek()`, the stepping mode),
`BatchSimulator.java:569`, and four assertions in `SimEventDedupTest`. Those are the call
sites a queue swap breaks, and they are `protected`-field reaches, not observer callbacks.
A task genuinely aimed at "#476 lands without touching a consumer" would make `eventQueue`
private and publish a narrow `peekNextTime()`/`pendingCount()` on `Simulator` — a
half-day, mechanical, and it removes the real coupling. #678 leaves that coupling
untouched and hardens the one place that was never at risk.

## AC-1 cannot be met by the mechanism AC-1 implies

"With no consumer registered, the retire path executes the same instruction sequence it
does today (no per-event branch on a live collection)" is a correct instinct pointed at
the wrong construct. A registration/deregistration path over a collection *always* costs
at least the emptiness check or the iterator that AC-1 forbids. The only construct in Java
that literally compiles to nothing when unused is exactly what is already there: an empty
virtual method whose call site the JIT devirtualizes and inlines away under class-hierarchy
analysis. AC-1 is therefore an argument *against* a registry and *for* the existing hook,
written by an author who had not noticed the hook was there.

If multiple consumers are genuinely wanted later, the zero-cost route is composition at
construction, not registration at runtime: the fan-out lives in a `Simulator` subclass (or
a small chain object) chosen when the run is created, so the no-consumer configuration
never constructs a fan-out and there is nothing to branch on. Zero-cost by construction
beats zero-cost by measurement, and it needs no `ChronogramClosedCostTest` to defend it.

Note also that AC-1's absolute ("same instruction sequence") and AC-2's statistical
("within a stated, measured tolerance") are different claims, and only the second is
checkable on a JIT. Whichever seam is chosen, only one of those two sentences should
survive.

## The seam it specifies is the wrong seam for what CAP-23 needs next

AC-4 makes it binding that the seam carries "event identity and value only". Read
`docs/capability-roadmap/lf-03-causal-debug.md`: the entire causal-debug leapfrog rests on
the observation that identity-and-value is *precisely what JLS already has and precisely
what is insufficient*. `WireNet.propagate` holds the winning driver at `:464-465` and drops
it at `:484`; `SimEvent` has no cause field; `PinChanged` is a zero-field record that is
82.3% of all events fired. lf-03's own summary of the gap is that the trace machinery is
"one field short of being a causality log, twice", and that the missing field is available
at both write sites — `BatchSimulator.afterEvent:140` and `InteractiveSimulator.afterEvent`
already receive the `SimEvent`.

So #678 proposes to spend a task freezing, as a tested contract, the exact payload shape
that CAP-23's later PFs will have to break. That is not a neutral cost; a test asserting
the consumer signature names nothing but identity and value is a ratchet pointed at the
project's own roadmap.

## The reframe: cut along the recorded history, not along the callback

The chronogram's real defect at HEAD is not the tap. It is that the history is thrown away.
`InteractiveSimulator.runSim` clears `traces`, `traceMap`, `wireMap` and `memTraces` at
`:612-619` before every run; `Trace.Change` is a private record inside a `jls.edit` Swing
component bounded at `MAX_RETAINED_CHANGES = 100_000` (`Trace.java:32`) with no persistence
behind it; `TraceSample` (`jls.sim`) is a parallel, incompatible, record of the same thing.
Two half-histories, one in the GUI and one in batch, neither surviving a run, neither
carrying provenance.

The high-leverage move — and I would spend #678's budget here instead — is a headless
**recorded-history model**: one `jls.sim`-side per-signal history type with a slot for the
producing event, written once by the existing `afterEvent`/`probeSample` hooks, read by
`Trace` for drawing, by `BatchSimulator.toVcd` for export, and later by cross-probing,
cause-chains and rewind. Under that framing:

- The "tap seam" question **disappears**. Nothing new touches the retire path; the hooks
  that already run do one more field write when a recorder exists.
- "Free when closed" **disappears** as a measurement problem and becomes a construction
  fact: no recorder constructed, no recorder called. AC-5 (headless/batch gain no code) is
  satisfied by not building the object.
- #476 becomes trivially compatible, because the consumer never sees the queue — same
  guarantee AC-4 wants, obtained by not creating a second mechanism.
- #680's grouping/radix work and #390's retirement trace both land on one history model
  rather than three.
- lf-03's causality log becomes a field addition to a type that exists, not a re-litigation
  of a frozen seam.

## Two smaller alignment problems worth recording

**It invents a parallel mechanism the architecture already forbids.** `docs/extension-points.md`
is normative under #223: seams are `jls.module.ExtensionPoint` constants, catalogued with a
row and an owning issue *before* their contract exists, and the stated reason for that rule
is verbatim "so nobody invents a parallel mechanism in the meantime". The closest precedent
is `collab.op-observer` / `jls.collab.op.OpSink` — many contributions, register-then-observe
— which is exactly the shape #678 describes. #678 proposes a bespoke registry in `jls.sim`
with no catalog row and no point id. If, contrary to everything above, a registered-consumer
seam is still wanted, it must arrive as a catalogued `sim.event-observer` point, and the
tension between `ExtensionRegistry`'s list dispatch and AC-1's no-branch rule has to be
resolved in the open rather than discovered in implementation.

**`ordering_after: []` is wrong, and the apparatus is the expensive part.** AC-2 wants
`ChronogramClosedCostTest` to compare "kernel event throughput and the first-year adder
flow against the recorded baseline". #476 states plainly that no scaling benchmark exists
and that #378 owns the long-run lane a benchmark would run in; and per ARCHITECTURE.md's
test layout, `test/jls/ui` layers 2 (Swing under Xvfb) and 3 (render-to-image) are
*reserved*, not present — so "the first-year adder flow" is not measurable in CI today at
all. The measurement harness is a larger and more valuable asset than the seam, and it is
shared: #476, #475, #393 and CAP-23 all need the same warm-loop throughput ratchet. That
ratchet should be one project-level gate under #378's lane, owned once, not a
chronogram-shaped test that will be re-implemented per feature.

## Disregarding the stated acceptance criteria

I am explicitly setting AC-1 through AC-5 aside rather than grading against them. AC-1 and
AC-4 describe a property the tree already has via `afterEvent`; AC-4 additionally freezes a
payload shape lf-03 needs to widen; AC-3's falsification ritual is sound practice but is
being applied to prove a regression in code that need not be written; and AC-2/AC-5 depend
on measurement infrastructure that #476 and ARCHITECTURE.md both record as absent.

## What I would do instead

1. **Close #678 as already-shipped-in-substance**, with a comment recording that
   `Simulator.afterEvent`/`probeSample` are the seam, that `InteractiveSimulator.afterEvent`
   already feeds the chronogram, and that #390 relies on exactly this and ratchets against
   any change to `jls.sim`.
2. **Split off the genuine #476-decoupling task**, which is small: make `eventQueue` and
   `dupCheck` private, publish `peekNextTime()`/`pendingCount()`, and fix the four call
   sites in `InteractiveSimulator`, `BatchSimulator` and `SimEventDedupTest`. This is the
   thing that actually lets #476 land without touching a consumer.
3. **Split off the kernel-cost ratchet** as a shared gate under #378's long-run lane, owned
   by the engine programme (#362), consumed by CAP-23 rather than owned by it. Keep the
   falsification-first discipline of AC-3 — it is the best idea in the issue — and apply it
   to that gate.
4. **File the recorded-history model** as the real PF-1 substrate, with a provenance slot
   from the start, and let #680 and #390 render and serialize from it.

Endorse the goal. Redirect the work.
