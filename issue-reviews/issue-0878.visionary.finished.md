# Issue #878: TASK-C232-1: the immutable, width-carrying signal value type exists in `jls.core` with its op set and its frozen field list — and nothing else in the tree changes yet
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "add a class to `jls.core`." Three separate programs are queued behind one
missing noun. `docs/capability-roadmap/keystone-c-performance.md` §0 measures the
event loop as **37.6% value-container overhead, 47.7% queue bookkeeping, 4.9%
actual digital logic**; #322 (four-state), #888/CAP-39 (N-ary alphabets), #391
(resolution fold), #422 (operator kernel) and the levelized pass of keystone C §6.3
all wait on the same swap. The task's instinct — land the type alone, with a hard
negative control (P8) so the byte-identity claim in #879 is attributable — is
correct and well ahead of the norm for this repo. The sequencing is right, the
tier boundary is right, and I would not merge #878 into #879.

What I am reframing is the **one thing this task exists to produce**: the frozen
field list. §5 freezes it on a shape that the only measurement resident on master
rejects, and §6 then disguises that as a naming vote.

## 1. The field list is frozen on the rejected representation

§5 specifies "a sealed interface … permitting a one-plane binary case and a
two-plane four-state case (the IEEE 1364 aval/bval layout)." Follow that to its
source: `keystone-b-migration.md` stage 1 (line 535) spells it out as `Binary`
(one **`BitSet`** plane) plus `FourState` (a `BitSet` pair), which is
`sweep-01-values-and-logic.md` §V1's proposal. Keystone C benchmarked exactly that
as **R1** and returned (§4 table, §11):

| width | R0 `BitSet` today | **R1 `BitSet` pair (this issue's §5)** | R4 3-plane record |
|---:|---:|---:|---:|
| 1 | 28.72 | 77.63 | **5.78** |
| 32 | 21.11 | 81.05 | **10.85** |
| 64 | 20.17 | 81.36 | **9.95** |

> "**R1 is the one option that would make the frame's opponents right.** If a
> value-domain program is attempted with R1 and someone benchmarks it, the program
> dies." — keystone C §4, and §11 calls this "the most important single correction
> this sweep has to make to the other sweeps."

Keystone A §5.1 rejects it independently and for a different reason: the split is
on *state-ness*, which flips constantly during a run — every net that goes X and
comes back changes shape, producing megamorphic call sites at every op — where the
recommended split is on **width**, fixed for a signal's lifetime:

```java
sealed interface LogicValue permits Word, Wide { … }
record Word(int width, long a, long b, long u)         // width <= 64
record Wide(int width, long[] a, long[] b, long[] u)   // sparse b/u above
```

A record holding a mutable `BitSet` also cannot honor P3 without defensive copies
on construction *and* on every accessor — i.e. the 61 clones this task exists to
make deletable get re-homed inside the type instead of deleted. **I am
disregarding §5's case split.** The frozen field list should be
`(int width, long a, long b, long u)` with the width-split `Wide` fallback, and
the freeze should cite keystone C §4's table and §10's risk row ("sparse nullable
`b`/`u` planes measured at parity; take that design"), both of which are on master.

## 2. §6 is not a naming decision

§6 tells the executor two documents "name this type differently" and to pick one
and mark the other stale. They do not name one type differently — they specify
**two different types**: different case axis, different plane count, different
storage, 81 ns/op versus 10.85. Resolving it by vote on the identifier is how the
wrong representation ships with a `git blame` that says "naming cleanup." The PR
should record a **representation** decision against keystone C's measured table;
`LogicValue` then falls out as the name because it is the name attached to the
design that wins. Say in the PR that `keystone-b-migration.md` stage 1 and
`sweep-01` §V1 are stale on representation, not on migration order.

Related: the third plane. §5 permits two planes; keystone C §4 measured the `U`
plane at **1.71 ns, ≈0.5% of a 318 ns event**, and confirms keystone A's "ship it
dormant on day one so the migration happens once." An issue whose entire
justification is *freeze it so we migrate once* that drops the plane measured free
is arguing against itself — and #888's own encoding wants three sentinels
(`N`=X, `N+1`=Z, `N+2`=U).

## 3. The seam the issue never cuts: value form and slot form are the same ops

This is the reframe I would most like adopted. Keystone C §8.3, verbatim:

> `LogicValue`'s API should be specified so that a `Word` can be *materialised
> from* and *written back to* a `(long[] a, long[] b, long[] u, int index)` triple
> without going through the object … **That is a small constraint if it is stated
> up front and an expensive rewrite if it is not.**

And §11: the per-value object and the per-netlist array "are not sequential; they
are the same change applied at two scopes," with the levelized pass at
**4.32 ns/node over plane arrays vs 22.01 over `BitSet[]`** — 80% of that pass's
win lives in this decision.

The issue's comment stream already grasps the principle — *"if the value migration
ships without this, the remaining work becomes a second value migration — the
expensive kind"* — but applies it only to a one-line `lo()`/`hi()` accessor pair,
and never to the storage seam that actually costs a rewrite across 24 migrated
`react` bodies. Concretely, freeze the **plane algebra**, not the object:

- The truth tables live in package-private `static long`/`static void` kernels over
  `(a, b, u)` words — one set of expressions, no `BitSet` anywhere.
- `Word` is a boxed slot that calls them; a `Planes(long[] a, long[] b, long[] u)`
  cursor calls the same ones at an index. Both land in #878; only the cursor's
  *consumers* are deferred.
- Class javadoc freezes the plane layout and names both consumers, so the levelized
  stage is a new caller, never a re-encoding.

A pleasant consequence: the `> 64` obligation stops being the unwitnessed
correctness hazard §1/O4 frets over and P2 hunts at width 96. Under plane kernels,
`Wide` is a **fold over the same kernel**, so the 96-bit property test verifies a
loop, not a second op set. The reason O4 reads as scary is that under
`Binary(BitSet)` there is no width at all to get wrong — `BitSet` is unbounded —
so the width-96 case has to be invented rather than derived. Cut on planes and the
threat-to-validity mostly evaporates.

## 4. Reserve a type, not accessors — the body and the amendment now contradict

§5 reserves `Put.getRadix()`/`getDigits()` returning `2` and `getBits()`. The
second comment supersedes exactly that formulation with `Put.lo()`/`hi()` returning
`0`/`1`. Both are in force in the issue as it stands; an executor reading top to
bottom reserves the superseded pair. Note the pattern: this accessor pair has now
been respecified twice before a line of it exists, because an alphabet is being
smuggled through the API as loose `int`s.

Reserve **one value** instead: a `jls.core.Interval` (or `Alphabet`) record with
`Interval.BINARY` as the constant, returned by `Put.interval()` and the `WireNet`
pair. `radix`, `lo`, `hi` and #888's future `N` are all fields or derivations of
that one type, and the next program adds a field rather than a third accessor pair
and a fourth supersession comment. This is also the cheaper thing to hold inside
the frozen field list, which is the reason the reservation was routed to #878 at
all.

## 5. The completion criteria never measure anything

Comment 1 item 3 requires the freeze figure be **re-measured on the shipped type**,
not inherited from the unrecoverable `3a81a4a`. §5 says "whichever number the
executor can re-derive on master." Then the Completion Criteria contain P2
(correctness properties), P6 (a JaCoCo floor) and `mvn verify` — **no benchmark
item at all**. As written the task can go green with the field list frozen on a
number nobody produced.

Fix it inside P8's boundary, which is where it happens to be cheap: land the
`ValueRep`/`WideSparse`/`Levelized` harnesses of keystone C §12 under `test/jls/core/`
as a `main`-invoked micro-benchmark (not a JUnit timing assertion — keystone C §8.1
is right that those are flake factories). Then the frozen field list's number is
produced by code that ships, keystone C's scratchpad measurements stop being
unreproducible, and #879's k2000 gate inherits a harness instead of building one.
That is a better use of this task's isolation than a JaCoCo floor.

## 6. Trajectory check

One misalignment worth naming. #232's body still asserts "no levelized pass is
being built … so this representation change stands solely on interpreter-path
merits," and `ARCHITECTURE.md`'s recorded #221 decision binds any future strategy to
"the two-states-plus-HiZ value domain." Keystone C §8.3/§11 and #888's governance
motion 1 both overturn that frame. Filing #878 as a child of #232 without saying so
risks freezing a field list optimized for #232's GC-churn story — where a `BitSet`
pair looks like a small diff — rather than for the array-lift story, which is what
the measurements actually reward. One sentence in #878 recording that #232's
"levelized pass deliberately does not exist" line is stale would keep the executor
pointed at the right constraint.

## Bottom line

File it, land it first, keep P8. Change what it freezes: planes not `BitSet`s,
width-split not state-split, three planes with `U` dormant, ops as shared kernels
callable from both a boxed value and a plane-array cursor, and the alphabet
reserved as one type rather than a drifting pair of `int` accessors. Add the
benchmark harness so the freeze rests on a number this repository can reproduce.
Done that way, #878 is the keystone commit of the whole roadmap. Done as §5 writes
it, it is a 4×-slower type with a frozen field list, and the first person to
benchmark it ends the programme.
