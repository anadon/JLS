# Issue #435: TASK-0109: two headless replicas exchange real envelopes over the real transport and save byte-identical files — with divergence reported as divergence, not as timeout
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the 450 lines and the claim is one sentence: *build the measuring instrument before
the mechanism, so the convergence claims are falsifiable while they are being written.*
That instinct is the spine of this repository, not a novelty. `DeterministicSaveTest` was
built before replication needed it; `HeadlessCoreRatchetTest`, `SocketConfinementRatchetTest`
and `ArchitectureRulesTest` are load-bearing negative space. Apparatus-first is JLS's
house style, and on that axis the issue is aligned. Endorsed on purpose.

Three things pull against the project's arc, and each has a cheaper route to the same end.

## Reframe A — this is the third seeded schedule runner, not the first

`test/jls/collab/session/RosterConvergenceTest.java` already *is* this harness, one layer up:
1000 seeded schedules, 80 steps each, a hand-rolled chaos bus that drops/duplicates/reorders,
a bounded anti-entropy round chosen explicitly "so convergence cannot hide behind
'eventually'", the seed printed on every failure, and minimized failing seeds committed as
directed regressions in `RosterTest`. `test/jls/collab/net/ChaosTransport.java` is a second
seeded fault engine at the frame layer. #435 proposes a third at the op layer, and O2's
`git grep ReplicaHarness|class Schedule` returns zero only because the existing one was
never named `Schedule`.

The elegant cut is one schedule kernel in `test/jls/collab/`, parameterised by
(replica type, event alphabet, quiescence predicate, oracle), with the roster suite, the
transport suite and the replica suite as three adapters. Then #435's actual deliverable is
a replica adapter plus a `stateHash()` oracle — perhaps 150 lines — instead of a new
apparatus, and the project gains one deterministic-simulation facility rather than N
bespoke ones.

That extraction also repays a debt this issue almost sees. §7.3 rightly insists the PRNG
algorithm be pinned or "the corpus changes under a JDK update" — but `RosterConvergenceTest`
uses bare `new Random(seed)` today, and so does `ChaosTransport`. Declaring the discipline
for new code while the two shipped harnesses violate it is the worse of both worlds. Pin it
once, in the shared kernel, and the existing corpora inherit it.

Open Question 1 dissolves under this framing: the kernel drives transports through the
`Transport` interface and never names `ChaosTransport`. Add
`public static Transport chaotic(Transport delegate, long seed, double drop, double dup,
double reorder)` to a public factory in `jls.collab.net` (test tree). Neither the class
widens nor the harness moves. A question flagged as "blocks execution, a reviewer must
decide before the task starts" is answered by a five-line factory.

## Reframe B — the middle outcome may not need to exist

The three-outcome classifier is the issue's proudest deliverable, and the argument for it
is airtight *given the architecture it assumes*: `CausalBuffer` gives order and
exactly-once but not commutativity, so non-commuting concurrent ops diverge, so per-kind
merge rules (#279, TASK-0110: add-wins element sets, LWW attributes, OR-set wires, RGA)
are needed, so the harness must name their defects. Every step follows. I want to attack
the premise instead.

Three facts already shipped:
- `CircuitOp.invert(Circuit before)` exists on the sealed interface and is contractually
  pinned: "applying the op and then its inverse returns the circuit to its prior canonical
  bytes (issue #166 is the oracle)".
- Elements are addressed by stable id (#165), never by reference, so an op "means the same
  thing on a restored or replicated circuit".
- `Circuit.stateHash()` gives cheap content equality, pinned by `DeterministicSaveTest`.

Those are exactly the preconditions for **deterministic total-order replay** — rebase, not
merge. Each replica keeps its op log; `OpId` plus `PeerId` gives a total order; on
receiving a concurrent op that sorts before local ops, the replica inverts its local ops
back to the join point, applies the arrival, and re-applies the local ops, *revalidating*
each. Convergence stops being a property to test per op-kind and becomes a theorem: both
replicas hold the same ops, in the same total order, over the same base, and `apply` is
deterministic. There are no per-kind merge rules. No RGA. No OR-set. The entire #279 /
TASK-0110 body of work evaporates, and with it the classifier's middle case — which, if it
ever fired, would mean `apply` is nondeterministic, a far sharper accusation than "some
merge rule is wrong".

The cost is honest and should be stated: a rebased local op can now fail revalidation (your
wire's endpoint was concurrently deleted) and is reported as a rejected edit rather than
silently merged. For a *circuit* that is the better answer. Add-wins resurrection produces
a graph no user drew; a half-attached OR-set wire is not a circuit. And the op layer's
whole character is already `CircuitOpReader` **rejecting rather than repairing** (O6) —
rebase-with-rejection is that philosophy applied to concurrency, while per-kind CRDT merge
rules are its exact opposite bolted on above it. The research doc chose op-based CRDTs
against Raft, on availability grounds; rebase keeps every availability property it wanted
(no quorum, lone peer keeps editing) and drops the merge-rule surface. Log growth is
answered by the compaction #171 already names, and 2–8 peers on a LAN bound the undo window.

If that reframe is taken, **I am disregarding the stated acceptance criterion that the
three-outcome classifier is a deliverable.** It becomes: same bytes (pass) / different
bytes (apply or invert is not deterministic — stop everything, same severity as H1) /
unconverged (delivery defect). Same three buckets, different accusations, and the second is
now a bug rather than an expected finding.

## Reframe C — the most valuable half is unblocked today

The issue chains itself behind #382 and #169 (via the closed TASK-0108/#433), which by the
comment trail has already cost it two parent-pointer corrections and both of its named
features. But the property that actually decides Reframe B needs neither:

> apply(o1); apply(o2)  ==bytes==  apply(o1); invert(o1); apply(o2); apply(o1)

That is a pure test over `CircuitOp`, `Circuit.save` and `stateHash` — no `Transport`, no
`OpEnvelope`, no replica, no session, no socket. It is writable against `master` this week,
it falsifies or confirms the invert contract that everything above depends on, and a
failure there would invalidate the harness's plan before the harness is built. File it as
its own task and run it first.

The remaining blocker is smaller than O3 makes it sound. `jls/core/TextMetrics.java` already
exists as the headless seam, and the four metric-touching ops (`RotateElement`,
`FlipElement`, `AddElements`, `SetElementConfig`) reach it via
`SwingTextMetrics.forGraphics(g)`. #382 is a parameter-type swap plus one headless
implementation — not a feature. The other seven op kinds ignore `g` entirely, so a replica
loop over those seven runs headless today. And TASK-0108's envelope *framing* is only needed
to cross a socket; this suite binds none (P8), and `OpEnvelope` already ships. Only #382
genuinely blocks, and only partially.

## One product idea the apparatus framing hides

§7.1 insists this stays in the test tree, "no chaos transport in the shipped jar" — correct
about the chaos transport, over-broad about the oracle. The research doc §2 already names
the shipped version: peers exchange `stateHash()` and the peer panel shows "in sync ✓ /
syncing… / diverged". That is a handful of lines over code that already exists, it gives
students the divergence signal directly, and it turns the harness's oracle into a live
safety net instead of test-only apparatus. Worth filing alongside.

## What to keep, unchanged

The seed as the unit of record; failing seeds minimized and committed as named regressions;
a *stated* round bound rather than "eventually"; no sleeps and no wall clock (and note
`ReachabilityTracker` already takes the clock as a parameter, so the injected tick source is
a convention the codebase keeps, not a new one); byte equality of canonical saves as the
oracle, with H1 as the tripwire under it; and the coverage bound imposed by the unmigrated
gestures (#282, #283) stated rather than implied. Those are all right, and none of them
requires the 450-line spec around them — the spec is at higher risk of decay than the code,
as this issue's own three correction comments demonstrate.
