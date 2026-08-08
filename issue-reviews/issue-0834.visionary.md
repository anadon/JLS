# Issue #834: TASK-C333-3: a partition advances its clock only as far as its peers' committed time and lookahead allow, so no committed simulation time is ever un-committed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the vocabulary and #834 asks for one predicate in one loop. `Simulator.runEventLoop`
(`/home/user/JLS/src/jls/sim/Simulator.java:215-243`) polls the head event and assigns
`now = event.getTime()`. TASK-C333-3 is the guard placed immediately before that
assignment: *may I advance to this time yet?* Everything else in the issue — the barrier,
the peers, the committed-time equality of AC-4 — is one implementation of the answer.

That framing matters because the whole `jls.sim` package is 1,256 lines and `Simulator`
is 301 of them. The rule being added is textbook Chandy–Misra–Bryant and it is correct;
the risk is entirely in what the issue drags in alongside it.

## The seam is cut in the wrong place, and the ordering follows the wrong cut

`ordering_after` says TASK-C333-2 must land first because "the frame and the drain path
must exist first." That is true of a *network* barrier and false of the barrier. The
advance rule needs one collaborator: something that answers `min over j≠i of (T_j + L_j)`.
Whether that number arrives from a peer object in the same JVM, a `LoopbackTransport`, or
a socket is invisible to the rule.

Cut instead at the scheduler: a `jls.sim` seam — call it `ClockGate` — with a single
method `long maxAdvanceTime()`, and three implementations:

1. **unconditional** — returns `Long.MAX_VALUE`; this *is* today's whole-design run, and
   AC-5 (existing goldens byte-identical) becomes true by construction rather than by
   test, because whole runs take the same code path they take now;
2. **in-process multi-partition** — N `Simulator` instances in one JVM, one thread each or
   even round-robin on one thread, exchanging committed times through a plain object;
3. **remote** — TASK-C333-2's frames, later, if the measurements below justify them.

Under this cut, #834 is a ~50-line change plus tests and can be built, tested, and
*refuted* before anyone touches `jls.collab.net`. AC-1 (a peer held back, the other
stalls at the bound), AC-4 (a named, observable barrier point) and AC-3 (two partitions
byte-identical to whole) are all satisfiable at implementation 2 with no transport, no
frame kind, no second consumer of the only socket-permitted package, no #170 hostile-input
inheritance, and no security review. I am explicitly disregarding the stated
`ordering_after`: the current order puts the expensive, irreversible,
security-surface-touching change (#832) ahead of the cheap experiment that may kill the
feature. Invert it.

## The measurement that decides this issue is already in the tree, and it is bad news

Neither #834 nor #836 (which owns the lookahead *value*) checks what JLS's own semantics
make lookahead *be*. The tree answers it:

- `docs/simulation-semantics.md:285` — "Splitter, Binder, Extend, Constant, pins, jumps,
  wires, **subcircuit boundary** | 0". Zero delay.
- `docs/simulation-semantics.md:229-233` — "an arbitrarily deep chain of wiring elements
  adds zero time."
- `docs/capability-roadmap/keystone-c-performance.md:535` — **82.3% of all simulated
  events carry no time at all** (1,919,891 `PinChanged` of 2,331,793).

A partition boundary is a cut across nets. The natural place a human cuts a design is a
subcircuit boundary — which is a **zero-delay** element. On such a cut, *L_j = 0* exactly,
the bound collapses to `min T_j`, and the barrier degenerates to lock-step: every
partition advances one timestamp at a time in unison. Cut somewhere less natural and the
best available lookahead is the minimum gate delay on the path, **5** (NAND/NOR/NOT,
`simulation-semantics.md:273`), against a register-dominated cycle in the tens to
hundreds. That is a barrier firing roughly every 5 time units — 10–20 rendezvous per clock
edge — in an engine whose own profile says 47.7% of loop time is already priority-queue
bookkeeping and 4.9% is logic (`keystone-c-performance.md:190-194`).

The consequence is not "slower." It is that TASK-C333-4's refusal path — the one #836
frames as an exceptional diagnostic — is the *normal* outcome for real JLS designs, and
#834's barrier is a mechanism built for a lookahead regime this simulator's semantics do
not supply. #333's §7 anticipates this ("the refusal **will** be hit") but treats it as a
late discovery. It is a static property of the delay table, knowable now.

**Concrete AC I would add, and would gate the rest of C333 on:** before the barrier is
implemented, run a lookahead census — for each of the committed `riscv/` fixtures and the
golden suite, enumerate candidate cuts and print the distribution of per-boundary minimum
delay. If the modal cut has lookahead 0 or 5, #333 has its answer and the honest response
is a `REPLAN:` on #333, not six tasks. This costs a day and it is the same discipline
TASK-C333-1 already applies to cross-platform determinism.

## AC-2 is a grep, not an invariant

`git grep -inE "cancel|withdraw|rollback" -- src/jls/sim/` returning nothing is offered as
proof that no rollback machinery was introduced. It is a string search over English words.
It is trivially satisfiable by naming a method `retract`, `unwind`, `revertTo` or
`undoAdvance`; it is trivially violable by a comment reading "we never roll back"; and it
will be under active pressure from TASK-C333-6, whose checkpoint/resume restores a
simulator to an earlier state — mechanism indistinguishable from rollback, separated only
by vocabulary.

Replace it with the property it is a proxy for: a test asserting that across any run
`now` is monotonically non-decreasing, that no `TraceSample` already emitted is ever
rewritten, and that no event with time < `now` is ever dequeued. That is checkable, it
survives renaming, and it is the thing invariant 2 of #333 actually claims.

## Does this strengthen the project's arc, or pull against it?

It pulls. `ARCHITECTURE.md:341-368` records, as a settled decision (#221), that the
event-queue interpreter is JLS's **only** simulation execution strategy — a levelized or
compiled pass was declined as premature optimization, with one named revisit trigger
("a concrete CPU-scale design on the `riscv/` trajectory that is unusably slow
interactively"). #834 introduces a second execution *mode* that is strictly more invasive
than the pass that decision refused: it adds a network dependency, a rendezvous inside the
hot loop, a checkpoint-coherence obligation, and a class of nondeterminism the project has
never had to defend against. No revisit trigger has fired. The decision is not cited
anywhere in #333 or its six children.

That is not a reason to refuse #834 — it is a reason to say plainly that a distributed
mode is a second strategy, and that #221's §6 record must be amended in the open *before*
the barrier lands, exactly as `ARCHITECTURE.md:366-368` requires of any future pass ("a
specified, documented change to `docs/simulation-semantics.md` first, never a silent
behavioral difference between strategies"). Doing it that way also forces the equivalence
criterion to be stated once, in one place, rather than re-derived per child.

## The out-of-the-box alternative the feature never considers

#312 is explicit that this is a **capacity** capstone, not throughput — the goal is that a
design that fits nowhere *runs at all*. Capacity is a footprint problem, and #834 attacks
it with a synchronization protocol. There is a cheaper axis on the same goal, already
funded, that never appears in #333's "alternatives considered":

**Shrink the element instead of splitting the design.** `keystone-c-performance.md:780-790`
measures the value container at **22.01 ns/node as `BitSet[]` versus 4.32 ns/node as plane
arrays**, with 37.6% of loop time spent on `BitSet` clone/hash/equals. FEAT-054 (#370, flat
compact element representation) plus that value-domain change moves the single-machine
ceiling by whatever factor the footprint drops — with zero protocol, zero network, zero
determinism risk, zero new security surface, and it makes *every* JLS user faster rather
than serving a cluster nobody has. Combined with FEAT-005 (#353), which #312's own abstract
says takes the reachable size from ~165,000 to ~694,709 elements "for a two-line change",
the honest question is: how many orders of magnitude does the single-host route buy before
a barrier is needed at all? #333 does not ask. And once the model is flat arrays, an
out-of-core / memory-mapped model is a further capacity multiple on **one clock**, where
determinism is free.

If the answer is "the single-host route reaches 10^7 and the target is 10^10", then
distribution is still not the answer — three orders of magnitude is not four hosts, it is
thousands, and a global barrier at lookahead 5 across thousands of partitions is not a
system that runs.

## What I would keep, unchanged

The advance rule itself, AC-1's shape (hold a peer back, assert the other stalls at the
bound rather than overshooting), AC-4's insistence that the barrier be a *named, observable*
point rather than an emergent one, and AC-5. Those are right and they are the durable part.

## Bookkeeping finding

#333's Definition of Done requires the four planned scopes to be priced and summed against
its 10–18 mw band. The six filed children now price it: #830 1–2, #832 3–5, #834 4–6,
#836 2–3, #838 3–4, #839 3–5 — **16–25 mw**. The decomposed floor is within 2 mw of the
band's ceiling and the ceiling is ~39% over it. That arithmetic exists now and should be
posted to #333 rather than left to close.
