# Issue #333: FEAT-056: partitions on separate hosts exchange boundary events, and the result is byte-identical to running the design whole
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the cluster imagery and #333 asserts one thing worth having: **a JLS run may be
executed as N cooperating pieces and the result is a function of the design alone** —
`O ⊥ n`. That is a statement about the *semantics of the simulator*, not about sockets.
The distribution across hosts is one consumer of that property; a compiled pass (#221's
deferred second strategy), a multi-core run, a campaign of many runs in one JVM (#350),
and a checkpoint/resume (#363) are all consumers of the same property. The issue has
mistaken its most speculative consumer for its subject, and has therefore cut the work
along a transport seam instead of a semantics seam.

Everything below follows from that. I endorse the capability. I am disregarding the
stated first child ("boundary-event marshalling over the existing Transport seam"), the
placement of criterion 4's refusal, and the "on separate hosts" clause as a near-term
requirement, and I say why.

## The finding that reorders the whole feature

`src/jls/sim/SimEvent.java:87` is `private static long sequence = 0;` and `:116-119`
assign `seq` from it at construction. `SimEvent.compareTo` (`:130-150`) breaks same-time
ties on that number, and `docs/simulation-semantics.md` §3 states the consequence
plainly: *"same-time events fire in the order they were posted — FIFO within a
timestamp… FIFO makes ordering deterministic only if the posting order is itself
content-determined."*

**A per-JVM monotonic post counter is not content-determined across a cut.** Partition
each design and each partition gets its own counter; the relative order of two same-time
events posted in different partitions is a function of how many events each partition
happened to post beforehand, which is a function of *n* and of where the cut landed.
The duplicate-suppression rule has the same defect: `Simulator.dupCheck` coalesces on
(time, callBack, todo) over a *global* pending set, and that set is partition-local in a
partitioned run, so a coalescing that happens whole may not happen split.

Criterion 1 is therefore not merely untested at `2d0ca9d`; it is **false by
construction** under JLS's current ordering rule, and no barrier protocol repairs it —
the barrier decides *when* a partition may advance, not how two same-time events order
once both are admitted. The invariance suite at n ∈ {1,2,4,8} would find this, and would
find it after the transport, the protocol and the checkpoint work had all been built on
sand.

**The first child should be: replace the same-time tie-break with a partition-independent
total order.** (time, stable element id, payload discriminant, deterministic port index) —
the stable id already ships, per #312's own note on `src/jls/elem/Element.java:24`
(`ElementId.mintFresh()`, #165), and #181 already made time-0 seeding walk
`Circuit.getElementsInStableOrder`. This child has `blocked_by: []`, lands entirely
inside a 301-line `Simulator` plus a 224-line `SimEvent`, is a specified change to
`docs/simulation-semantics.md` §3 with a golden re-cut, and it deletes a global mutable
static from the engine — which also unblocks #350's many-runs-per-JVM case and any future
second strategy. It converts criterion 1 from an empirical sample at four values of *n*
into a **theorem**: if the partitioned schedule is a refinement of a content-determined
total order, `O ⊥ n` holds for every *n*, not the four you tested.

## Reframing A: cut at the link, not at the transport

The issue's §1 spends its strongest paragraphs defending reuse of `jls.collab.net`. The
security argument is correct and I would not weaken `ArchitectureRulesTest.java:249-262`
either. But it answers a question that need not be asked yet.

Make the seam `jls.sim.part.BoundaryLink` — `offer(BoundaryFrame)`, `drain()`,
`peerCommittedTime()` — with the barrier protocol written against it. Two
implementations: an in-process queue link, and (later, separately) a `Transport`-backed
one. Then:

- criteria 1, 2, 3 and 5 are all satisfiable **in one JVM with no socket** — the
  reordering double becomes an in-process link that applies `ChaosTransport`'s same
  bounded-holdback discipline, and is if anything a sharper instrument than a byte
  transport for permuting arrival order;
- criterion 6 holds trivially, because nothing new goes near a socket;
- `blocked_by` collapses from `[318, 332, 169, 363]` to `[332]` plus #318 for watch
  naming — the row #312 calls *"the deepest row in the set"* stops being deepest;
- the second-consumer security review of the sole socket-owning package happens once, in
  its own issue, when a cluster user exists, against a protocol already proven
  deterministic.

This is the same staging discipline ARCHITECTURE.md already records for plugins (#222):
in-process first, the out-of-process boundary *"reserved for a future untrusted-provider
case; it is not built speculatively."* #333 as written builds the socket half first and
speculatively. The transport is the last slice, not the first.

## Reframing B: lookahead is a partition-time property, so criterion 4's refusal is in the wrong place

`docs/simulation-semantics.md` §6.1 — wires are ideal, zero simulation time — and §6.2/§7:
`Splitter`, `Binder`, `Constant`, `InputPin`, `OutputPin` and **the subcircuit boundary**
all have delay 0. The subcircuit boundary is precisely where a human would cut a large
design. So the natural cut has lookahead exactly zero, criterion 4 fires, and the feature
refuses the only cuts anyone wants to make.

The fix is not a better refusal message. Lookahead here is not a runtime discovery at
all: it is `min` over boundary nets of the driving element's `propDelay`, a static
property of the cut. Therefore **make it a cut-legality rule and the runtime problem
disappears**: a legal cut crosses only nets driven by a delay-bearing element with
delay ≥ L_min — in practice cut at registers (50) or gates (5–10), which is what every
real distributed simulator does. Lookahead is then positive by construction, the
lookahead computation and the refusal path vanish from the protocol, and the barrier
never starves. This belongs in #332, which already owns *"uncuttable constructs refused
by name at partition time"* — it is one more clause in a rule that exists, not a new
mechanism in a protocol that does not.

## Reframing C: the question the issue never asks

#312 is explicit that capacity, not speed, is the axis, and that *"no sentence here may
be read as a speedup claim"* — engine throughput is nearly flat in circuit size. A
conservative barrier makes a distributed run **strictly slower** than the same design on
one host with enough memory. So the honest form of the capacity question is: *is
distribution the cheapest way to hold 10^9–10^10 elements, or merely the most obvious?*

#312 already answers it partly: FEAT-054 (#370) *"is worth roughly an order of magnitude
of capacity before any distribution."* At a flat 16–32 B/element, 10^9 elements is
16–32 GB — a single commodity server today — and 10^10 is 320 GB, which is a large single
node or a memory-mapped flat state file on NVMe. Out-of-core on one host delivers exactly
what the capacity axis claims ("the design runs at all") with **no protocol, no second
network consumer, no partition-count invariance risk, and no new failure mode**, and it
degrades in the one dimension #312 has already disclaimed. Before funding a barrier
protocol, someone should price `#370 + memory-mapped state` against `#332 + #333` for the
stated target. If the single-node route reaches 10^10, #333's network half may never need
to be built, and its determinism half is still needed — which is exactly the split
Reframing A produces.

## What I would keep, unchanged and promoted

Criterion 8 is the best thing in this issue and it is filed as open question 3. Nothing
in the tree asserts a simulation is bit-identical across a JDK upgrade or across
operating systems, and **every byte-identity claim in JLS's batch/grading contract
depends on it** — not just this feature's. `docs/batch-interface.md` is a documented
stability contract, the container image exists for autograders, and #265 arms the
platforms. Run one circuit on three CI platforms and diff the VCDs; it is a day of work
that serves every present-day user and de-risks four criteria here. Promote it out of
#333 entirely and do it now, independently of whether this feature is ever funded.
(Note: `docs/parity-contract.md`, cited at `:469-477` throughout, does not exist at this
checkout — one more reason to settle the question in the tree rather than in prose.)

## Alignment with the project's arc

ARCHITECTURE.md's #221 decision declines even a levelized compiled pass as *"premature
optimization until CPU-scale designs are actually common,"* with a revisit trigger of a
concrete riscv/ design that is unusably slow. A cluster simulator is a far larger
commitment than the strategy the project has already declined, aimed at a user the
project has not yet met, in a repository whose i18n non-goal is justified by "a
single-maintainer pedagogy tool." Under Reframing A the tension resolves: the part of
#333 that strengthens the arc — a determinism guarantee strong enough to survive being
split — is startable now, cheap, and pays into #350, #362, #363 and #221's future second
strategy alike; the part that pulls against the arc is the socket, and it can wait for
its user without holding the rest hostage.

## Concrete restatement of the roster

1. Partition-independent same-time event ordering (`blocked_by: []`, startable today;
   spec change + golden re-cut). *New, and first.*
2. `BoundaryLink` seam + the barrier protocol over it, in-process only.
3. The invariance suite at n ∈ {1,2,4,8}, loopback link **and** reordering link — written
   first against a stub, per §6's own better-order note.
4. Checkpoint at a barrier, over #363.
5. `Transport`-backed link and its second-consumer security review — **separate issue,
   filed when a cluster user exists.**

Lookahead legality moves to #332. Cross-platform determinism leaves as a standalone
change. On that roster the cost band's basis also changes shape, so the §4-of-open-
questions pricing should be redone against these five, not the original four.
