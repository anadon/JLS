# Issue #691: TASK-C532-3: reconvergent paths render side by side with their unequal accumulated delays, and completeness is checked against the scheduler's own event graph
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

One sentence in the body carries the whole capstone: *"the two unequal-delay routes
behind a runt pulse are something the tool points at."* That is the payoff of CAP-23
(#504) and, per `docs/capability-roadmap/lf-03-causal-debug.md`, the thing no competitor
has — because no competitor's engine is specified tightly enough (`docs/simulation-semantics.md`
§6.2: pure transport delay, no inertial suppression, deterministic ordering) for "why"
to be a well-posed question. The end is right. I am keeping it and discarding the route.

**I am explicitly disregarding acceptance criteria 1 and 4, and replacing the oracle in
AC-2/AC-3.** What follows says why, and what I would build instead.

## The route as filed cannot reach the end, for a structural reason

#688 retains "producer, consumer, element, delay" — one producer per consumer event.
`Simulator.post` (`Simulator.java:163`) is the sole enqueue path and `Simulator.java:239`
the sole `react` call site, so posting-parents are *exactly* one per event: a **forest**.
A single transition's ancestry in a forest is a path. It can never fan in. AC-1 asks a
tree renderer to render a DAG it will never be handed. Worse, `Gate.react`
(`Gate.java:694-707`) recomputes from *all* inputs while the parent event names only the
last to move, and `Register.react` posts `NewValue(d)` parented to the clock edge — so on
any clocked circuit the answer is "because the clock ticked," and the data lineage the
student asked about is gone. `lf-03` already diagnosed this and prescribed
`causalInputs()` over-approximation; the task decomposition dropped that decision on the
floor.

That is the narrow finding, and the adversarial pass reaches it too. The visionary point
is different: **the fan-in problem is a symptom of choosing the wrong unit of
explanation.**

## Reframe 1 (primary): the unit of explanation is the pulse, not the transition

A static hazard is not one transition. It is a *pair* of transitions on one net, close
together. The "two unequal-delay paths" are not two branches of one edge's ancestry —
they are **the ancestries of the runt's leading and trailing edges**, which are two
chains that share a common tail and diverge at one point. So:

- Select a **pulse** (two adjacent transitions on a net), not an edge.
- Walk each edge's chain — each is a path, which is exactly what #688's forest yields.
- Align the two chains from the root; the last common event is the **divergence point**,
  named for free, with no graph algorithm.
- The two spans after the divergence are the two paths, each with its accumulated delay.

Everything AC-1 asks for falls out of a two-chain diff on the data model that actually
exists, and **AC-4 dissolves entirely**: there is no combinatorial path enumeration to
bound, so there is no "fan-in explosion," no bound to pick, no count to report. An
acceptance criterion whose whole content is "don't hang" is a tell that the design is
enumerating something it should not be enumerating.

### The oracle this unlocks, which is better than the one filed

AC-2 says correctness is checked "against the scheduler's own event graph." No such
object exists (`lf-03`: provenance is computed and discarded on the same line;
`WireNet.java:464-465` vs `:484`), so the only candidate graph is #688's buffer — the
thing under test. The invariant is circular.

The pulse framing supplies a genuinely independent one: **the arithmetic must close.**

> accumulated delay of path A − accumulated delay of path B == observed pulse width

Both sides come from different places — the left from the rendered explanation, the right
from `Trace`/`TraceSample`, which the chronogram already records and which
`Trace.firstChangeAtOrBefore` already indexes. If the tool renders 15 and 10 and the
waveform shows a 5-wide runt, the explanation certifies itself against data it did not
produce. If it does not close, either a hop is wrong or a third path exists — and the
test says which. That is a stronger claim than "every reported event is an ancestor," it
is checkable in batch, and it cannot pass by sharing a wrong assumption with the
implementation. Keep AC-3's dual-direction discipline and point it at this identity.

## Reframe 2: the reconvergent-path model is *static*, and building it here buys two features

Everything AC-1 renders — divergence point, two routes, unequal accumulated delays — is a
property of the **circuit graph and `propDelay`**, not of any particular run. The
topology is already walkable with public API today: `Put.getWireEnd()` →
`WireNet.getAllEnds()` (`WireNet.java:340`) → attached `Put`s → elements, with a uniform
delay accessor already factored out as `jls.elem.Timed.getDelay()` (issue #78; 12
implementors). A path enumerator over that graph, summing `Timed.getDelay()`, prints

```
IN → NOT(5) → AND(10)  = 15
IN →          AND(10)  = 10   skew 5, divergence at net "a"
```

with **zero engine change, zero retention, no ring buffer, no #476 seam, and no
simulation run at all** — it answers "this circuit *can* glitch," which is the question a
student needs answered *before* they have a waveform. The dynamic chain then has only one
residual job: say which structural path was live at that instant.

This is where the arc argument bites. #532's boundary note says CAP-23 must "never mint a
parallel cause-chain data model" against P4's static glitch detector — priced at ~1.5 wk
and called "the highest teaching-value-per-week item in six sweeps." The static path
model *is* the shared model. Build it at #691 and one 1.5–2 mw task delivers the
reconvergent rendering **and** the substrate P4's detector needs, instead of two
programmes converging on a REPLAN later. Building the dynamic-only version here is the
one move that guarantees the duplication #532 promised to avoid.

## Reframe 3: text first, and #689 AC-4 pulls against the roadmap that justifies this work

`lf-03` states the artifact plainly — an ASCII causality tree from
`jls -b --why 'alu_out[3]@41200' circuit.jls` — and says: *"That is the thing that makes
causal debugging gradeable, diffable and CI-testable, and it is what nobody else has."*
Yet #689 AC-4 requires the inspector "is not constructed at all in headless/batch runs,"
and #691 inherits a GUI-only surface. Per #508 the live course is on another fork and the
top wedge is grading integrity; a GUI-only inspector serves zero present users, while a
deterministic text tree under `docs/batch-interface.md` is diffable, gradeable, and
testable without the Layer-2/3 GUI harness that `ARCHITECTURE.md:224-225` records as
*reserved, not built*. AC-2's "not against a recorded screenshot" is the right instinct
half-expressed: with a text artifact, screenshots never enter the conversation, and the
golden file plus the arithmetic identity give two independent checks.

Text-first also cuts this task's dependency chain from four deep
(#476 → #529/TASK-C529-1 → #688 → #689 → #691, of which #476 and #529 are still open) to
approximately zero for the static half.

## Arc alignment

- **Budget.** #508 funds the whole debug-loop parity slice at ≈3–4 mw; #532 alone bands
  4–6. The static path model plus a text renderer is the version that fits the actual
  allocation and lands standalone.
- **Ordering.** Under reframes 1–2, #691 stops depending on #688's retention shape — the
  exact unstated dependency that makes this task un-startable today.
- **Hot plane.** `docs/grand-architecture.md`'s rule (per `lf-03`) is that the inner loop
  stays cost-free. A static analyzer touches it not at all; a pulse-diff over an existing
  retained forest touches it once. Both beat a DAG tap.

## What I would keep verbatim

- CAP-23 Open Question 1's answer — granularity follows what the scheduler already keys
  on, no synthetic granularity. Right, and it should survive every reframing here.
- AC-3's requirement to demonstrate *both* failure directions in the PR. That is the best
  sentence in the issue; retarget it at the arithmetic-closure oracle and at a dropped
  path, and it becomes a real proof rather than a self-consistent one.

## Bottom line

Right destination, wrong vehicle. Explain **pulses**, not transitions; enumerate paths
**statically** over `Timed` and the net graph, where reconvergence is native and free;
prove the explanation by **closing the arithmetic against the waveform**, not by
consulting a graph the tool itself invented; and ship it **as text in batch** so it is
gradeable on day one. That version is smaller than the one filed, has no upstream
dependency risk, and hands P4's glitch detector its substrate as a side effect.
