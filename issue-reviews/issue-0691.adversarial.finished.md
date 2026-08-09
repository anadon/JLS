# Issue #691: TASK-C532-3: reconvergent paths render side by side with their unequal accumulated delays, and completeness is checked against the scheduler's own event graph
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#691 is TASK-C532-3, the third of three tasks decomposing FEAT-C23-3 (#532,
"cause-chain inspector," PF-3 of capstone CAP-23 / #504). It builds on
TASK-C532-2 (#689, single-path ancestor-chain rendering), which builds on
TASK-C532-1 (#688, a bounded ring buffer retaining "producer, consumer,
element, delay" per retired event). The core claim — render reconvergent
fan-in as distinct paths, verified against the scheduler's event graph as an
invariant — is the namesake observation of the whole capstone (§1 step 2 of
#504). It is also the task most exposed to a data-model decision made in a
task it does not own.

## Findings, most severe first

### 1. [HIGH] The data model it depends on may not support multiple parents per event — which is the entire point of this task

TASK-C532-1 (#688), which #689 and #691 both build on, describes retention as
recording, per retired event, "which event and which element produced it"
(#532 body) and a ring-buffer row of "producer, consumer, element, delay"
(#688 AC1) — language that reads as **one producer per consumer event**, i.e.
a tree/chain, not a DAG.

But the actual event model does not have a single producer per event. Reading
the code: `Gate.react` on `PinChanged` calls `computeOutput()`, which reads
**all current input values**, not just the one input whose change woke the
element (`src/jls/elem/Gate.java:694-707`):

```java
case PinChanged _ -> {
    BitSet value = computeOutput();
    if (!value.equals(toBeValue)) {
        toBeValue = (BitSet)value.clone();
        sim.post(new SimEvent(now+propDelay,this,new NewValue(value)));
    }
}
```

A two-input AND gate's `NewValue` event is caused by **both** of its current
inputs, which is exactly the reconvergent-fan-in case this issue exists to
render. `docs/capability-roadmap/lf-03-causal-debug.md` independently
confirms this is a real design problem for the same substrate: it explicitly
chooses to "over-approximate the inputs" and default `causalInputs()` to *all
attached inputs of the element* (lines 179-189), precisely because the event
model does not cheaply expose which specific input triggered a given react.

Neither #688 nor #689's issue text says whether a single consumer event may
have **multiple** ring-buffer rows (one per causal input) — which is the
minimum representation needed for AC-1/AC-2 here ("renders as two or more
distinct paths," "each with its own accumulated delay total and its
divergence point identified"). If TASK-C532-1 ships a literal one-parent-per-
event structure, TASK-C532-3's core acceptance criteria are not achievable
without redesigning #688's already-scoped (1.5-2 mw) data model — a fact
#691 has no visibility into and cannot control, since it only orders after
#689, not #688 directly.

**Recommendation:** before starting #691, get an explicit, written commitment
in #688 (or via REPLAN on #532) that the retained relation is multi-parent per
consumer event (a DAG edge list, not a parent pointer), matching
`causalInputs()`-style over-approximation. Otherwise this task is blocked on
an unstated assumption made two tasks upstream.

### 2. [HIGH] "Checked against the scheduler's own event graph" names an oracle that does not exist and is not specified as independent of the code under test

AC-2 and AC-3 lean on `CauseChainCompletenessTest` checking the rendered set
"against the scheduler's own event graph" as ground truth. But per
`docs/capability-roadmap/lf-03-causal-debug.md` ("Nothing in the tree records
why a value is what it is. The simulator computes provenance on every
propagate and discards it on the same line," lines 12-13) and the code itself
— `WireNet.propagate` computes the winning driver and drops it on the very
next lines (`src/jls/elem/WireNet.java:464-465` vs. `:484`), and
`Simulator.runEventLoop` polls, reacts, and lets the event become garbage
(`src/jls/sim/Simulator.java:224-243`) — **no "event graph" object exists
anywhere in this codebase today.** The only candidate graph is the very ring
buffer TASK-C532-1 builds — the thing under test.

The issue does not say the test's oracle is built by an independent method
(e.g., a from-scratch backward walk over a purpose-built test harness, or a
brute-force replay-based reconstruction) rather than by re-querying the
production retention/query path. If oracle and implementation share code or
assumptions (including the single/multi-parent ambiguity in finding 1), the
"invariant, not eyeballed" framing is misleading: the test can pass while
both sides encode the same wrong assumption, e.g. a gate reacting to only its
"last changed" input rather than all of them.

**Recommendation:** state explicitly, in #691 or a linked design note, how
the test's ground-truth graph is constructed independently of TASK-C532-1's
retained structure (e.g., a small reference re-simulation with full
provenance kept only in the test, built without reusing the production ring
buffer).

### 3. [MEDIUM] The bound in AC-4 is unspecified and therefore gameable

"Path enumeration is bounded — a fan-in explosion reports the bound and the
count it stopped at instead of hanging." The AC never says what is bounded
(total paths enumerated? recursion depth? per-node fan-in degree?), what the
default value is, or whether it is configurable. An implementation could set
an extremely low bound (say, 2) and technically satisfy the letter of AC-4
("reports the bound and the count it stopped at") while providing no useful
diagnosis on any circuit with real fan-in — which defeats the feature's
purpose (the two-path glitch fixture in AC-2/AC-3 would still pass, since it
only needs 2 paths, masking the gap). Compare to #688 AC4, which at least
requires the ring-buffer capacity to be "a recorded measurement... not a round
number chosen by taste" — no equivalent discipline is applied to this task's
enumeration bound.

**Recommendation:** name what is bounded and require the default to be
derived from a measurement (e.g., observed fan-in depth on the shipped hazard
demo), matching the discipline #688 already applies to its own capacity.

### 4. [MEDIUM] band_mw: 1.5-2 looks underpriced relative to the work described, especially stacked on finding 1

This task alone must: extend a single-path renderer to a branching DAG
renderer, compute and label divergence points, implement bounded/aborting
path enumeration, build a seeded two-path glitch fixture, write an invariant
completeness test against an oracle that (per finding 2) may need independent
construction, and demonstrate both failure directions in the PR. TASK-C532-2
(#689) — the simpler single-path linear renderer — is priced at the same
1.5-2 mw band. Multi-path graph traversal with an invariant proof and dual
negative tests is a different order of complexity than hop-by-hop linear
rendering; pricing them identically suggests the estimate did not account for
the graph-algorithm and oracle-construction work, or is implicitly assuming
finding 1 resolves for free.

**Recommendation:** re-derive the band after #688/#689 land and finding 1 is
resolved, rather than trusting the filing-time estimate.

### 5. [LOW] Inherited process risk: this is PF-3, and CAP-23's own Definition of Done gates PF-3 funding on a measurement that has not visibly landed

#504 (CAP-23)'s Completion Criteria includes: "KC-23-1's tap-cost measurement
recorded from the demo slice before PF-3..PF-6 are funded." The demo slice is
PF-1 (minimal chronogram) + PF-2 (one-way cross-probing) + the AC-5
(`ChronogramClosedCostTest`) ratchet. #689's own dependency, TASK-C529-1, sits
under #529 (FEAT-C23-2, PF-2), which is **still open** at the time of this
review, and #476 (TASK-0063, the queue-replacement seam #532 AC5 depends on)
is also open. This doesn't block #691 by itself — task decomposition
naturally files ahead of landing — but #691 (like #688/#689) makes no
reference to KC-23-1's gate, so nothing here would stop TASK-C532-3 from being
picked up and built before the capstone's own kill-criterion has been
satisfied. A prior review of #504 in this same fleet flagged the identical gap
at the capstone level (issue-0504.adversarial.md, finding 1); it recurs here
unaddressed at the task level.

**Recommendation:** add an explicit check (or a comment on #532/#691) that
KC-23-1's measurement has been recorded before work on this task starts.

## What's solid

- AC-3's dual-direction failure demonstration (test fails when a path is
  dropped, and when a non-ancestor is reported, both shown in the PR) is a
  genuinely strong, hard-to-fake piece of test design — it rules out the
  common failure mode of an invariant test that only ever checks one
  direction.
- Deferring to "whatever the scheduler already keys on" for granularity
  (inherited from CAP-23 Open Question 1) correctly avoids inventing
  granularity the engine doesn't have, and is applied consistently across
  #532/#689/#691.
- Scoping strictly as "ordering_after: [TASK-C532-2]" and treating this as an
  additive extension of an already-built single-path renderer is the right
  shape for a task decomposition, contingent on finding 1 being resolved.

## Bottom line

The acceptance criteria are well-specified as prose but rest on a data-model
assumption (multi-parent retention) that the task it depends on has not
committed to in writing, and lean on an "event graph" oracle that does not
exist in the codebase and isn't specified as independent of the code under
test. Both are fixable by clarifying #688/#689 before #691 starts, which is
why this is needs-rework rather than should-not-proceed — the outcome is
sound, but the acceptance criteria as filed could pass while the real
capability (rendering true reconvergent causes, not an artifact of an
under-specified retention model) is not delivered.
