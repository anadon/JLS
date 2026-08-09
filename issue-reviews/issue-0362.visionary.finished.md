# Issue #362: FEAT-030: every retired event costs measurably less and the entire golden corpus stays byte-identical — a structural boot fits a nightly lane without any semantic change
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Stripped of the machine block, the claim is: *JLS's event loop spends 95% of its
time on bookkeeping, and that is a betrayal of what the tool is.* That claim is
true and it is confirmed by the project's own surviving evidence.
`docs/capability-roadmap/keystone-c-performance.md` §3, which is on master and
which the REPLAN comment correctly re-homes the deleted citations onto, measures
the loop as **value representation 37.6%, event-queue bookkeeping 47.7%, actual
element logic 4.9%**. A teaching simulator that spends twenty times as long
deciding a heap position as it spends computing what a gate outputs has an
architecture problem, not a tuning problem. The destination — an engine whose
cost is dominated by the physics it models, with every claim about it measured
on a tracked fixture — is the right destination and I endorse it without
qualification.

What I do not endorse is the shape. This issue takes five loosely-coupled
changes, welds them into one 12–20 maintainer-week program with a
whole-feature kill criterion and bus factor 1, sequences them against the
evidence rather than with it, declares the cheapest and largest independent win
out of scope, and then writes an acceptance criterion that would fail the
single best-measured improvement in the tree. Below, three reframings and one
out-of-the-box alternative.

## Reframing 1 — the sequence is backwards; build the closure first and the queue may never be needed

§6 calls TASK-0063 → TASK-0064 a **necessity**, on the grounds that "measuring
the closure against the priority queue attributes the queue's cost to the
closure." That is a *measurement-attribution* argument dressed as an engineering
dependency. Engineering says the opposite: **remove work before you make the
remaining work cheaper.**

keystone-c §2's event census on `riscv/build/k2000.jls`: `PinChanged` is
**1,919,891 of 2,331,793 events — 82.3%**, and every one of them models zero
elapsed time. `docs/simulation-semantics.md:229-232` is the rule that creates
them. Collapsing the zero-delay plane removes ~82% of postings *and* collapses
the heap, and keystone-c §7.1 measures that heap depth alone costs ~60 ns/event
(82.32 ns poll+add at depth 64 versus 141.91 at depth 12,000). So the closure
does not merely reduce the queue's traffic — it deletes most of the reason a
calendar queue exists. A `PriorityQueue` carrying 18% of today's traffic at
shallow depth is not obviously worth replacing with a novel data structure that
must be proven order-identical over generated streams.

**Concrete restructure:** land the zero-delay closure first, re-measure, and
make the calendar queue *conditional on the residual*. The issue's own § 2 says
"splitting them produces an intermediate state with worse constants than either
endpoint" — of the queue and the dedup set. It never asks whether the queue task
survives the closure at all. It should.

There is a second reason to invert. keystone-c §7.2 item 4 sequences the dedup
replacement **after the value type lands**, and its mechanism is *per-element
pending-event slots extending the `toBeValue` pattern `Gate.react` and
`TriState.react` already use* — a mechanism whose coalescing rule compares
values. #476's intrusive-flag calendar queue is a different mechanism that never
inspects a value. The REPLAN comment identifies this correctly, and it is the
single most consequential open item on this issue: **the mechanism decision is
upstream of every ordering edge in §6, and it is unmade.** Until it is made, §6's
"every edge above is load-bearing" is not a true statement about this feature.

## Reframing 2 — Stage −1 is not out of scope; it is the baseline, and the headline number may depend on it

§1 puts "the quadratic and materializing I/O paths" out of scope, deliberately,
so as not to gate a two-week fix behind a five-month program. The instinct is
right; the conclusion is inverted. keystone-c §8.2 states the constraint the
other way round: *land them **before** the engine work "so that the value-domain
change is measured against a loop, not against a string concatenation."*

The concrete facts, re-derived in-tree:

- `src/jls/elem/SigSim.java:64,67,71,74` builds the de-commented vector by
  repeated `String +=`. At 6,004 cycles `initSimulation` is **0.568 s against a
  0.742 s loop** — 43% of run wall time — and it is **quadratic** in vector
  length (3.0×, then 3.7× per doubling) where the loop is linear.
- `SigSim` posts **every** stimulus event at t=0 (`:129,192`, confirmed: both
  post sites call `sim.post(new SimEvent(...))` inside the parse loop). The
  12,093-deep heap is an artifact of that, not of the circuit. The ~60 ns/event
  depth tax that TASK-0063 proposes to buy back with a new data structure is
  refundable by streaming the vector out of one class.

This matters beyond tidiness, because the feature's *headline deliverable* is a
wall-clock conversion: 1.66–1.72 h → 44–46 min. That division by 2.26 assumes
the boot is warm-loop-dominated. On the only fixture anyone has measured, 43% of
wall time is not in the loop and scales quadratically. **Nobody has stated which
regime a structural boot is in.** If it is vector-driven, the conversion is
simply wrong, and the whole "fits a nightly lane" argument — which is this
issue's reason to exist — is unsupported. That question is answerable in an
afternoon and must be answered before the number is quoted again.

## Reframing 3 — Integration Criterion 2 forbids the best-measured change in the tree

§5 criterion 2 asserts events-per-clock-cycle as a **hard equality**: "an engine
change that alters the event count has altered semantics and fails here."

keystone-c §4.4 ran the experiment. `jls.sim.NoDedup` removed duplicate
suppression entirely:

```
dedup=true   loop 0.778 s / 0.715 s   fired 2,331,793   final state 9f07925e
dedup=false  loop 0.649 s / 0.659 s   fired 2,596,499   final state 9f07925e
```

**Identical final architectural state, 11% more events, 9–17% less time.** The
`HashSet` that exists to avoid 264,702 events costs more than those events cost
to fire.

Criterion 2 makes that change a build failure *by construction*. It elevates an
implementation-detail counter to the rank of an invariant, and it is strictly
stronger than criterion 1 (byte-identical goldens), which is the one that
actually encodes "nothing observable changed." Event count is not observable.
Nothing in `docs/simulation-semantics.md` promises a count. **I am explicitly
disregarding acceptance criterion 2 as written.** The correct form is: *event
count is reported and any change is explained, and byte-identity of the corpus
is the gate.* Keep the tripwire, demote it from equality to a recorded
observation. As written, the feature's own gate is the thing standing between
the project and its cheapest measured win.

A related over-tightening: making K3 a *whole-feature* kill criterion, with "no
partial credit," is what converts five independently-verifiable changes into the
highest-variance item in the core column at bus factor 1. Each change in this
stack is independently semantics-preserving and independently gated by the same
golden corpus. There is no engineering reason they must land or die together —
only the desire for one composed 2.26× number, and §Cost concedes that number
now has no surviving basis while master carries a different one (3.1–4.9×, on a
different basis). Unbundle. Let each change carry its own byte-identity gate and
its own measured delta.

One further caution on K3: byte-identity is only as strong as the corpus.
TASK-0064's **combinational-loop refusal** is a new behaviour — today a
zero-delay cycle spins to `maxTime`; a refusal is a different outcome — and no
golden exercises it, so the gate would pass while behaviour changed. §7 half-
concedes this ("the corpus is the problem, not the gate"); it should be a
pre-condition, not a re-planning contingency. Note also that "derived from each
element's *declared* delay" presumes a uniform declaration that does not exist:
`propDelay` is a per-class field on timed elements only (`Adder.java:39`,
`Decoder.java:39`, `FieldExtend.java:64`); `Splitter` and `Binder` have no delay
field at all. The derivation has to be built before it can be used.

## The seam is cut along funding, not along code

§1's "funding it twice is the named failure mode" is an admission that the
feature boundary between this issue and #370 (flat element representation) is
wrong. keystone-c §6.2 says it plainly: the per-value `record Word(int width,
long a, long b, long u)` and the per-netlist `long[] a; long[] b; long[] u` are
**"the same change applied at two scopes."** This issue owns the throughput
scope; #370 owns the capacity scope; the REPLAN comment reports #370 already has
six children and that #879/#846 are the same migration from two directions.

Throughput and capacity are two *acceptance tests*. They are not two features.
The architectural seam that exists in the code is the **encoding contract** —
the plane semantics and the truth-table expressions, written once, materialisable
both as an immutable value and as an indexed slot in a plane array. Cut there and
the duplication cannot recur; cut along outcome categories and it recurs every
time either side is funded. **Alternative framing:** promote the encoding
contract to the owning artifact (one feature, one owner, one test oracle), and
demote FEAT-030 and FEAT-054 to acceptance suites over it. That also disposes of
#848's index contract colliding with #476's queue replacement, since the index
would belong to the contract rather than to whichever engine landed first.

## The out-of-the-box alternative the issue never considers

The issue's real user story is CAP-02/CAP-03: *someone bringing up a drawn
processor wants their acceptance test to be schedulable.* The issue treats that
as an engine-throughput problem and then concedes, in Open Question 2, that even
after 2.26× the interactive requirement is still missed by 1.2–5× and needs
another 30–45 maintainer-weeks and a strategy invariant 5 forbids opening.

A boot is 99% identical on every run. **Checkpoint and resume** — serialise
simulator state (queue contents, net values, element state, `now`) at a settled
point and restart from it — converts a 1.7 h boot into a one-time cost and makes
"fits a nightly lane" true regardless of ns/event. It is orthogonal to every
constant in this issue, it is byte-identity-testable in the strongest possible
sense (resume from checkpoint must produce the same continuation as an
uninterrupted run), it composes with rather than competes against the engine
work, and it becomes *substantially easier* once TASK-0056's immutable,
width-carrying value lands — which this feature is funding anyway. It is also
the only option on the table that helps a student iterating on the last 5% of a
boot, which no constant-factor improvement does.

Second, cheaper still: the issue fixes the tier and then optimises the engine.
If the acceptance test's purpose is to catch regressions, running it against the
behavioural binding (2.5 min, and explicitly not moved by any of this work) with
a *sampled* structural differential is a scheduling answer that costs zero
maintainer-weeks of engine work. The issue never asks whether the structural
tier has to run in full, nightly, at all.

## What to keep, unchanged

- **Byte-identity of the whole golden corpus as the gate.** Right invariant,
  right corpus, and the four suites plus `VcdExportGoldenTest` and
  `RiscvCpuGoldenTest` exist today.
- **"Every published speed figure states node count and pass count."** This is
  the best paragraph in the issue. It is a general epistemic rule the project
  should apply beyond this feature, and keystone-c §2 now gives it a
  master-resident source (`elements(all,recursive)=1551 wireNets=297`, 225 logic
  elements + 297 nets, two passes per cycle).
- **Refusing to buy speed with semantics.** Invariant 4 is exactly right and is
  what distinguishes this from a Verilator-envy rewrite.
- **Measure before you divide.** Keep TASK-0023. But heed keystone-c §8.1 on the
  gate's *form*: "a timing assertion in CI is a flake factory … the numbers
  should be produced by the same command that produces the goldens, so that a
  regression is visible rather than discovered." TASK-0026 currently proposes a
  build failure. Master's own measured advice says produce, publish, and ratchet
  by review — not fail the build on a timing number.

## Bottom line

Right destination, wrong vehicle. Keep the aim, the byte-identity gate, and the
denominators rule; discard the single-stack/no-partial-credit shape, invert the
closure/queue order, absorb Stage −1 into the baseline instead of exiling it,
demote criterion 2 from equality to observation, settle the coalescing mechanism
before any ordering edge is treated as load-bearing, and move the encoding
contract to a single owner shared with #370. Then ask, before spending 12–20
maintainer-weeks, whether checkpoint/resume gets CAP-02 and CAP-03 what they
actually want for a fraction of it.
