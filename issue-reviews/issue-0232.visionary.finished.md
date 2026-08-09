# Issue #232: Simulation hot path: per-signal java.util.BitSet allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Stripped of its planning apparatus, #232 asserts: *a JLS signal should be an immutable,
width-carrying value instead of a shared mutable `java.util.BitSet`, and the reason is
allocation and GC.* The first clause is right and is the single most consequential
change available to this project. The second clause — the reason — is wrong, and it is
wrong in a way that has shaped the issue's structure, its type design, and its exit
conditions. That is why this is a redirect rather than an endorsement.

The project already answered this question on master, and the answer is not the one the
issue is built on. `docs/capability-roadmap/sweep-01-values-and-logic.md` opens with
"JLS's value domain is the narrowest waist in the whole program … Every other sweep's
items are features. This one is the program," and lists roughly twenty standards blocked
by it (1164 semantics, 1364's four states, EVCD, `numeric_std` don't-cares, I²C's
wired-AND). `keystone-c-performance.md` §0 measures the performance side and states the
correction explicitly: "The keystone is not 'replace `BitSet`'. It is 'give a signal a
width, an immutable identity, and a plane encoding' … The multi-value alphabet is nearly
free once the other two land; it is the *reason* to do the work, not the *cost* of it."

#232 inverts that. It makes GC the reason, semantics a non-goal ("HiZ … explicit HiZ
state replacing today's `null`", but still whole-signal), and it retains a falsification
gate whose firing would close the feature having built nothing. Read against the arc,
outcome (a) — "a profile shows BitSet allocation is not a meaningful share; the
representation stands and the profile is the deliverable" — is not a success of method.
It is a mechanism by which a measurement about GC could be recorded as a decision against
the project's stated keystone, on grounds the project has already said are not the reason
to do it. **I am explicitly disregarding §1 outcome (a) and the H1 falsification gate.**
A null allocation result would be interesting and would change the *sequencing*; it would
not make `null`-as-HiZ, order-dependent driver resolution, or a two-state alphabet the
right value domain for JLS.

## The ownership collision, which is the substantive structural finding

#322 (FEAT-026, open) says of this issue, in terms: "**This feature closes its
value-representation half.** #232 is about allocation and this feature is about
semantics, and they are the same migration — the representation cannot be changed twice."
The absorbed #475 contract carried into #232 says the opposite: "#322 consumes it …
but does not own it." Both issues currently claim the same migration across the same
files. The 2026-08-04 three-way dedup pass settled #232 against #370 and #362; it never
adjudicated #232 against #322, which is the pair that actually overlaps.

"The representation cannot be changed twice" is not a slogan; it is the cost argument.
The expensive, risky part of this work is not writing the type. It is the judgment pass
over the coercion and clone sites — 61 `clone()` sites in `src/jls/elem` + `src/jls/sim`
(verified at HEAD), the 29 `null`-HiZ coercion decisions, the 33 value-computing methods
in `keystone-b-migration.md` §1.2. #232's own contract insists those be reviewed
"**individually, not mechanically**"; #322 §4 invariant 4 calls the same pass "the single
largest risk in this feature." Landing #232 first means performing that irreducibly
manual review against a two-state, whole-signal type, and then performing it again
against the per-bit four-state type — twice the review, on exactly the sites where a
silent lab-behaviour change hides. Sequencing them as two migrations buys nothing and
pays the only expensive part twice.

## Alternative 1 — the type's split axis is wrong, and it is the wrong axis in a way that forecloses the next stage

#232's carried type is `sealed interface LogicVector permits Binary, FourState` — a
union split on **state count**, with a two-plane aval/bval `FourState` and a `Binary`
whose internals are deliberately left open ("whether `Binary` keeps a `BitSet` inside …
is an implementation detail"). The roadmap's committed shape splits on **width**:
`sealed interface LogicValue permits Word, Wide` with `record Word(int width, long a,
long b, long u)` — planes always present, three of them (README:134-136); #322's Open
Question 1 flags the two-plane/three-plane disagreement as load-bearing because #295
(CAP-03) spends the spare code points on radix 3 and 4.

The width split is strictly better and the reason is architectural, not stylistic:

- Splitting on state count makes every binary operation dispatch over a 2×2 matrix of
  implementation pairs with promotion rules, and buys the "pure binary path pays nothing"
  invariant with branching rather than with encoding. Splitting on width gives one code
  path; the binary case is free because `b = u = 0` and the ops are plain `long`
  operations that never look at the planes' contents.
- `keystone-c-performance.md` §8.3 states a constraint #232's contract does not carry:
  the value ops must exist in two forms, one over values and one over a
  `(long[] a, long[] b, long[] u, int index)` plane slot, "a small constraint if it is
  stated up front and an expensive rewrite if it is not." A `Binary` that may hold a
  `BitSet` cannot be materialised into or written back from a plane slot. #232's freedom
  is precisely the freedom to foreclose the next stage.
- The measured numbers on master say the richer type is the faster one: 10.85 ns/op for
  the three-plane `Word` including a dormant `U` plane against 21.11 ns/op for today's
  `BitSet` at 32 bits. #232's frozen-field-list decision ("a fourth plane costs +32% and
  +16 bytes") is cited to `docs/plan/evidence/mvl-determination.md` at `3a81a4a` —
  `docs/plan/` does not exist on master (verified) and the last comment records that the
  commit does not resolve. A design frozen in a javadoc as a deliverable, justified by a
  document no reviewer can open, contradicting a document that *is* on master, should not
  be frozen at all.

## Alternative 2 — if the goal really is "the loop should stop churning", #232 is aimed at the smaller half

On the measured profile, value-container work is 37.6% of loop time and **event-queue
machinery is 47.7%** (`PriorityQueue` 22.3% + `dupCheck` `HashSet` 25.4%), with actual
element logic at 4.9%. `SimEvent.PinChanged` is a zero-field record allocated 1.92 M
times per run — one line to intern. `SigSim` posts the entire `-t` vector during setup
(`SigSim.java:129,192`, verified), which is why the heap sits 12,093 deep and costs ~60 ns
per event in depth tax for reasons that have nothing to do with the circuit.
`SimEvent.sequence` is a mutable static written 2.6 M times per run. Those four fixes —
`keystone-c` §7.2's stage −1 — are days of work, touch no golden's semantics, and are
priced larger than the value swap. #232 never considers them, because it starts from
"BitSet is the problem" rather than from "where does the loop go".

A corollary worth stating: most of #232's *own* allocation win comes from immutability,
not from `(long, width)` packing. `WireNet.propagate` clones the value once per attached
input (`WireNet.java:495`) and once more for the probe copy; an immutable value is shared
by every sink, and the 61 clones delete. You could capture most of H2 with an immutable
wrapper that still held a `BitSet` — which is the tell that the title's `(long, width)`
framing was never the load-bearing idea.

## What I would do instead

1. **Close #232 as the frame, keep it as the measurement.** Move the representation
   migration to one owner — #322/TASK-0056, whose type shape is the one the capstones
   require — and let #232 survive as what only it specifies: the before/after allocation
   and GC contract, i.e. `keystone-c` §8.1's Stage-0 perf gate ("the loop is no slower,
   expectation 15–25% faster", against the recorded 318 ns/event, 2,331,793 events,
   `riscv/build/k2000.jls`). That answers Open Question 1 by citation rather than by a
   new corpus study, and it resolves the ownership collision in the direction the code
   costs argue for.
2. **Land stage −1 first**, unconditionally and independent of everything above: intern
   `PinChanged`, stream `SigSim`, per-`Simulator` sequence counter, cache `WireNet`'s
   driver/sink arrays. Cheap, semantics-free, and it makes every later measurement
   attributable instead of drowned in setup.
3. **Delete §5 integration criterion 3 rather than satisfy it.** The 2026-08-08 REPLAN is
   right that no in-tree circuit exceeds 32 bits, so the `>64` fallback has no witness.
   The correct response is not to build a synthetic wide-bus golden to feed a criterion;
   it is to note that the `Wide` arm is exercised by property tests over generated widths
   (the roadmap's bake-off already measures at 96 bits) and stop asserting a corpus
   property the corpus cannot have.

## What #232 got right and must not be lost in the move

The failure-mode table (width-mismatch as a programming error, `toLong` refusing X/Z
rather than zero-filling), the width-sensitive `equals` and its consequence for
`SimEvent` dedup coalescing, the "no compatibility overload taking a bare `BitSet`" rule,
the `Adder` carry-at-index-`bits` decision, the `TraceSample` HiZ-marker byte-identity
requirement, and the clone census as an acceptance criterion returning zero are all
first-rate and none of them appear in #322. They should be carried into TASK-0056
verbatim, not re-derived — this issue's real contribution is that contract, not its
hypothesis.
