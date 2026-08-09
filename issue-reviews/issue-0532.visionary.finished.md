# Issue #532: FEAT-C23-3: "why did this change?" walks any transition back through the scheduled-event graph and shows the reconvergent paths with their unequal accumulated per-gate delays
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is right and it is the best idea in CAP-23 (#504). JLS's singular asset is a
*normatively specified, deterministic* event engine (`docs/simulation-semantics.md`,
plus the #221 recorded decision that the interpreter is the sole strategy). Every
competitor that can draw waveforms cannot explain them, because their engines are not
specified to the level where "why" is even a well-posed question. JLS can answer it.
That is the unserved lane #508 says the fork has to own.

But the issue reaches that end through the wrong seam, in the wrong medium, behind the
wrong dependencies. **I am explicitly disregarding acceptance criteria 1, 4 and 5** and
recommending a different construction; criteria 2 and 3 survive, but 2 is not
satisfiable by the design the body implies. Details below, then three alternatives.

## The design the body implies does not produce reconvergence

`Simulator.java:239` is the *only* `react` call site in `src/` (verified: `grep -rn
"\.react("` returns one hit), and `Simulator.post` is the only enqueue path (#476 §7.4
restates this as an invariant). So "retain the producer-to-consumer relation" reduces to
one field: the event currently being dispatched is the parent of everything posted
during its `react`. Six lines, entirely above the queue.

That is elegant, and it is also why the feature as specified cannot work:

1. **Posting-parents form a forest, not a DAG.** Every event has exactly one parent.
   A single transition's ancestry is therefore a *path*, never a fan-in. AC-2
   ("reconvergent fan-in renders as distinct paths") is unsatisfiable from a single
   selected transition. A static-1 hazard is a *pair* of transitions on one net; the two
   unequal-delay paths are the ancestries of the runt's leading and trailing edges. The
   inspector's unit of explanation must be the pulse, not the edge — which also means
   AC-1's "from a selected transition (via FEAT-C23-2's edge click)" is the wrong entry
   gesture.

2. **Dedup silently deletes exactly the causal edges AC-6 tests for.** `PinChanged` is a
   zero-field record, so all `PinChanged` payloads are `equals` (#476 O5). `WireNet.propagate`
   posts `SimEvent(now, element, PinChanged())` per downstream input. When two nets feeding
   one gate change at the same timestamp — true reconvergence at equal delay — the second
   `post` is suppressed and its edge is gone. Causal edges must be recorded at the *post
   attempt*, including suppressed ones. AC-6 as written checks soundness ("every reported
   event is an ancestor"); the completeness half is where the obvious implementation fails.

3. **Posting-parents are trigger causality, not data causality — and this is the real
   ceiling.** `Gate.react` (Gate.java:695) recomputes from *all* inputs; the parent event
   names only the last one to change. `Register.react` (Register.java:747) posts
   `NewValue(d)` with the clock-edge `PinChanged` as its parent, while `d` was written by
   a different, earlier event. So on any clocked circuit — i.e. everything students draw
   after week three — "why did this change?" answers "because the clock ticked" and drops
   the data lineage the student is asking about. Nothing in #532 or #504 mentions this.

## Alternative A (primary): causality by replay, not by retention

CAP-23 already commits, in PF-6/#535 and AC-2, to **deterministic re-simulation** as the
time-travel mechanism, explicitly refusing checkpoints. Take that commitment seriously and
the entire retention problem evaporates:

- Answering "why" = re-run from t=0 in a `RecordingSimulator` with full causal recording
  on, targeted at (net, time). The recording run is a *different run from the interactive
  one*.
- **KC-23-2 disappears.** No ring buffer, no N, no "lost the chain for the very glitch we
  caught". The whole history back to t=0 is available, and because the recording run is
  not the interactive run you can afford the expensive complete graph (see Alternative B).
- **AC-5 becomes structurally true rather than a measured ratchet.** There is no tap in
  the shipped engine to be free of. Compare `BatchSimulator`, which already gates trace
  accumulation on a null field inside `afterEvent` — the precedent for "zero cost when
  off" exists, but "no code at all" beats it.
- **AC-4's #476 coupling disappears.** No seam over event structures is needed.
- Cost is O(T) per query — precisely the honesty CAP-23 §3 risk 3 already accepted for
  rewind. A hazard demo settles in hundreds of events.
- It shares one oracle with AC-2: if KC-23-3 (replay determinism under the interactive
  engine) fires, PF-3 and PF-6 die together. State that coupling; it is a feature, because
  it means one determinism investment funds two features instead of one.

Architecturally this also respects the #221 hot-plane rule that ARCHITECTURE.md records
("the inner loop lives entirely inside `core` with zero plugin indirection"). Modes in JLS
are `Simulator` subclasses with the #25 hooks (`beforeEvent`/`beforeReact`/`afterEvent`);
a recorder is one more subclass. The only gap is that `post` has no hook — add
`protected void posted(SimEvent child, boolean suppressed)` and the hook vocabulary is
complete. *That* is the stable seam AC-5 gropes for, expressed in the project's existing
idiom rather than as a new abstraction layered over queue internals.

## Alternative B: last-writer-per-input instead of parent-per-event

Independent of A, fix the causal model. Record on each `Input` the event that last wrote
its value (`WireNet.propagate` already calls `inp.setValue(newValue)` — one reference
alongside it). A transition's ancestors are then the last-writers of the inputs the
element actually read, transitively. This:

- produces a genuine DAG, so reconvergent fan-in appears in a *single* transition's
  ancestry and AC-2 becomes satisfiable as written;
- survives dedup, because it does not depend on an event having been enqueued;
- answers the register case correctly ("Q changed because the clock rose *and* D was 1,
  which came from...");
- is bounded by circuit size, not by time, for the frontier — with ancestry naturally
  truncatable at the last sequential boundary, which is also the pedagogically right
  place to stop.

## Alternative C: the cheap structural answer, which may be most of the value

For the canonical teaching case the honest answer is *static*: enumerate paths from the
toggling source net to the glitching net over the element graph and sum `propDelay`
(`docs/simulation-semantics.md` §7 tabulates every default). "Path A: IN→NOT(5)→AND(10)=15;
Path B: IN→AND(10)=10; skew 5." Zero engine change, no retention, no event graph, and it
works on a circuit that has not been simulated at all — "this circuit *can* glitch". The
dynamic ancestry then only has to say which structural path was live at that moment.

This turns CAP-23 §3 risk 1 ("never two cause-chain models") from a hazard into a gift:
the structural path model is exactly what P4's static glitch detector needs, so one model
serves both programmes instead of two competing for one.

## Arc alignment

- **Budget.** #508 funds "debug-loop parity (≈3–4 mw): CAP-23 chronogram slice;
  cause-chain inspector after #476's seam." #532 alone bands 4–6 mw and #527 bands 4–6 mw;
  jointly 8–12 mw against a 3–4 mw allocation. The issue does not acknowledge the
  overrun. Alternative C plus a text renderer plausibly fits the actual allocation.
- **Audience.** #508's headline finding is zero adoption here, with the live course on
  the bsiever fork, and its top wedge is grading integrity. A GUI-only inspector serves
  zero present users. **Build it headless and text-first** — `-why <net>@<time>` emitting a
  cause chain to stdout, under the `docs/batch-interface.md` contract — with the GUI panel
  as a thin renderer. This drops #532's `ordering_after: FEAT-C23-2` and its dependence on
  #527/#529 entirely, lets it land independently, and sidesteps a real gap: AC-1 assumes a
  scripted GUI test, but `test/jls/ui` is Layer 1 only (headless model assertions);
  Layers 2 and 3 are "reserved" per ARCHITECTURE.md. #504 Open Question 4 defers the batch
  artifact to CAP-06; that deferral is backwards given #508's priorities.
- **Ordering.** Under Alternative A, #532 depends on PF-6's determinism harness, not on
  #476's seam and not on #529's edge click. That is a better graph: the risky, novel
  property gets funded once and validated by two features.

## What I would keep

AC-3/AC-6's invariant-checked completeness test is the right instinct and should be kept
verbatim — extended to assert that suppressed posts appear as edges, which is the case
that will actually fail. Open Question 1's answer ("whatever the scheduler already keys
on, no synthetic granularity") is right and should stay.
