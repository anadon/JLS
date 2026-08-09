# Issue #838: TASK-C333-5: the same design at partition counts 1, 2, 4 and 8 produces byte-identical output under both the loopback and the reordering double, and nothing in the output reveals the count
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the distributed-simulation framing, #838 asks for one thing: an oracle that
proves **the observable output of a JLS run is a function of the design alone**, and not
of how the run was arranged. #333 writes it as `O ⊥ n` (output independent of partition
count). But `n` is only one arrangement parameter, and it is the one that does not exist
yet. The others already do, and the project is already leaning on the property without
having ever tested it.

That is the correct instinct in this issue and the reason it deserves to survive: an
evidence artefact separate from the thing it judges is exactly right, and #333 §2's
defence of that separation ("a barrier protocol that passes its own author's tests at
partition count 2 tells you almost nothing") is the best paragraph in the whole subtree.
My argument is that the artefact has been aimed at the wrong axis, measured at the wrong
granularity, and given a structural check that is weaker than the instruments this
project already builds.

## Against the project's trajectory

Two facts from the tree, pulling in opposite directions.

**Pulling against.** `ARCHITECTURE.md` §"Simulation execution strategy: discrete-event
interpreter is the sole strategy (recorded 2026-07-26, #221)" declines a *levelized
compiled pass* — a purely local, single-process optimization — as premature, on the
grounds that "classroom-scale gate circuits are the present workload". Its revisit
trigger is "a concrete CPU-scale design on the `riscv/` trajectory that is unusably slow
interactively", and it has not fired. The largest circuit committed to this repository is
`test/fixtures/riscv-sum1to10.jls` at 120 KB; `riscv/gui/cpu.jls` is 8.9 KB. A
multi-host distributed execution mode is a strategy an order of magnitude beyond the one
the project explicitly refused, for a memory pressure that no artefact in the tree
exhibits. #838 inherits that misalignment from #333 wholesale.

**Pulling with.** The same recorded decision attaches a binding **equivalence
criterion** to any future second strategy: it "must be observably identical to the event
model as specified in `docs/simulation-semantics.md`… and it must agree bit-for-bit with
the #202 RV32I integration golden run as a differential oracle. Any divergence is a
specified, documented change… never a silent behavioral difference between strategies."
That harness does not exist. #838 is the first concrete proposal to build it — and it
builds it welded to a partitioned run mode that is four unfiled/unbuilt dependencies away
(FEAT-055, FEAT-014, TASK-C333-3, TASK-C333-4). The project's single most reusable piece
of test infrastructure is being scoped so that it can only be built last, in a subtree
that may never ship, and against a single arrangement axis.

Worse, #333 §5 criterion 8 concedes that **every byte-identity claim in this feature
rests on an unverified assumption** — cross-platform/cross-JDK run determinism — and
cites `docs/parity-contract.md:469-477` for it. `docs/parity-contract.md` is **not in this
repository** (`ls docs/` confirms; it is cited across a dozen issues and exists nowhere).
So #838's foundational premise is documented in a missing file, and the experiment that
would settle it is described by #333 itself as cheap and already-provisioned in CI.

## Reframing 1 (primary): make the axis "arrangement", not "partition count"

Build `ExecutionArrangementInvarianceTest` (name aside) parameterized over an opaque
**arrangement** dimension, with the oracle and the diff machinery written once:

| arrangement | buildable today | value |
|---|---|---|
| whole run, repeated in one JVM | yes | pins seeding/iteration-order regressions beyond `SimulationSeedOrderTest` |
| whole run, JDK 25 vs. newest GA | yes (the advisory CI lane exists) | settles the JDK half of criterion 8 |
| whole run, linux / macOS / windows | yes (#265's lanes) | settles the platform half of criterion 8 |
| whole run vs. future compiled pass | when #221 reopens | discharges the recorded equivalence criterion |
| partition counts 1/2/4/8, loopback | when FEAT-055 lands | **this issue's stated deliverable, for free** |
| partition counts 1/2/4/8, chaos | same | AC-2, for free |

The reframed task lands *now*, unblocked by everything #838 is blocked by, de-risks four
of #333's eight criteria on day one, and is useful to #184/#185, #202, #265 and #221
regardless of whether distributed simulation is ever funded. Partition count then arrives
as one more row in a table, which is also the order #333 §6 says is the better one
("a scheduler may build the suite first against a stub and let it drive the protocol…
a suite written after the fact tends to test what was built rather than what was
required"). #838 as written guarantees the worse order.

## Reframing 2: compare the committed result, not the rendered bytes

AC-1 asks for a byte-diff of the VCD and watched output that "names the first differing
byte **and the signal it belongs to**". That second clause is the issue admitting the
oracle is at the wrong altitude and then proposing to reconstruct the right altitude from
a byte offset — i.e. write a VCD parser to undo the rendering you just did.

The canonical structure already exists: `BatchSimulator.getTraceSamples()` returns
`Map<LogicElement, List<TraceSample>>` (`src/jls/sim/BatchSimulator.java:329`,
`TraceSample` is `record TraceSample(long time, BitSet value)`), and `toVcd()` renders
*from* it (`:384`). Re-key that map by FEAT-014's partition-independent stable name — the
project already has `Circuit.getElementsInStableOrder()` (`src/jls/Circuit.java:479`) as
the precedent — and compare **that**. You get:

- a failure message that is `signal q, t=140: expected 0101, got 0100` without writing a
  parser;
- a *stronger* property, because the trace map covers signals the renderer may elide;
- artefact byte-identity as a corollary — same map + a renderer already pinned
  byte-for-byte by `VcdExportGoldenTest` implies same bytes.

AC-1 as stated should be disregarded and replaced by trace-map equality plus a single
smoke assertion that the rendered bytes still match.

## Reframing 3: make AC-3 unrepresentable rather than grepped

AC-3 — "a structural check on the artefact format asserts no partition identifier, count,
or field derived from either appears in any output" — is the weakest instrument in a
project that is unusually good at strong ones. A text scan cannot see a count encoded in
an ordering, a bucket boundary, or a timestamp granularity, and #333 §4 invariant 4
explicitly worries about "no ordering artefact".

The project's own idiom is bytecode ratchets: `ArchitectureRulesTest
.socketEndpointsAreConfinedToCollabNet()` (`test/jls/ArchitectureRulesTest.java:249`),
`HeadlessCoreRatchetTest`, `SocketConfinementRatchetTest`, `NullMarkedRatchetTest`. Apply
it here. If the distributed mode's only contract with the artefact writers is "merge into
the same `Map<stableName, List<TraceSample>>`", then no partition identity is *in scope*
at any writer, and one ArchUnit rule — no class in the distributed package is reachable
from `toVcd`/`BatchTracePrinter`/the watched-element printer — enforces AC-3 by
construction, permanently, including against ordering leaks a grep would never see.
That is the reframing that makes the problem disappear rather than testing for its
absence.

## Reframing 4: vary cuts, not counts

1/2/4/8 is a proxy for the real variable. AC-4 concedes as much by requiring "at least
one" fixture "with a cut that actually crosses nets" — the cut is the variable; the count
is bookkeeping. An 8-way cut through inert regions proves less than a 2-way cut through a
cross-coupled latch. The failure modes live in cuts that cross: a feedback loop, a
tri-state bus with drivers on both sides (JLS's multi-driver/HiZ resolution,
`docs/simulation-semantics.md` §2/§9), a clock net, and a zero-delay path — the last
being the exact input that must trip TASK-C333-4's lookahead refusal, so the suite should
be the thing that finds it.

Replace fixed counts with **seeded generative cutting** over one or two fixtures, in the
established in-tree idiom: `GenerativeRoundTripFuzzTest` (issue #160, plain
`java.util.Random`, jqwik rejected by policy) and `ChaosTransport`'s "print the seed in
every assertion message". Keep 1/2/4/8 as a fast smoke row. This turns a four-point check
into a property, at roughly the same cost.

## Two concrete facts about the chosen double

- **`ChaosTransport` drops and duplicates, not only reorders.** Its constructor takes
  `dropProbability`, `duplicateProbability`, `reorderProbability`
  (`test/jls/collab/net/ChaosTransport.java`), and `send` returns early on a drop. A
  byte-identical distributed result under nonzero `dropProbability` is impossible unless
  the barrier protocol layers retransmission — machinery no child of #333 owns. AC-2 must
  pin `dropProbability = 0` explicitly and say why. Conversely, `duplicateProbability > 0`
  exercises a real and currently unnamed protocol property: **boundary-event delivery must
  be idempotent**. That belongs in this suite and is absent from the issue.
- **The double is package-private** (`final class ChaosTransport implements Transport`) in
  `jls.collab.net` under `test/`. A suite owned by `jls.sim` cannot see it. Widen it into
  a shared test double deliberately and record the decision, rather than discovering it as
  an implementation accident that pushes the invariance suite into the networking package
  where it does not belong.

## Verdict

**endorse-with-reframing.** The artefact is real, the separation-from-the-protocol
argument is correct, and this is the only child of #333 whose value survives the parent
being descoped — *if* it is built along the arrangement axis rather than the partition
axis. As written it is the last thing buildable in a subtree whose premise (a design too
large for one machine) has no witness in this repository, its oracle is a byte-diff that
the acceptance criterion itself admits is too coarse, and its leak check is a grep where
this project would normally write a bytecode ratchet. I would disregard AC-1's byte-offset
diagnostic and AC-3's structural scan, generalize the parameterization, and land the
harness against the arrangements that exist today — starting with the cross-platform /
cross-JDK determinism experiment that #333 §5 criterion 8 says four of its own criteria
already depend on, and whose supporting document (`docs/parity-contract.md`) is not in the
tree at all.
