# Issue #468: TASK-0007: exactly one net-partition walk exists as a callable pass, instead of five copies nothing outside their own files can call
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated goal is de-duplication. The real goal, visible in the dependents
(#318 net identity, #366 PCB netlist, #332 partition cut, #373 naming), is
that **JLS does not currently have a type that owns the electrical-net
invariant**. Nets are an emergent property maintained by convention at every
site that touches wires. FEAT-004's outcome sentence — "exactly one net
partition in JLS" — is about ownership, not about textual copies of a BFS.
That distinction decides whether this task actually closes #336, and as
written it does not.

## The inventory is short by two, and the two it misses are the interesting ones

O1 counts three BFS bodies in `src/` plus `HdlExporter`'s union-find plus
`CircuitAssert`. Verified at the working tree, but there are two more
production partitioners:

- **`src/jls/edit/SimpleEditor.java:979-995`** — the delete-gesture planner
  partitions the *survivor* subgraph into connected components with its own
  worklist loop, seeded in stable-id order. It is not textually the O1 body
  because it walks an **edge-filtered** graph: `if (!survivors.contains(wire))
  continue`.
- **`src/jls/elem/WireNet.java:97` `makeNet`** plus
  **`WireEnd.traverse`/`Wire.traverse`** — a mark-based reachability walk used
  to *split* a net when an end is removed (`WireEnd.java:306`). This is the
  same connected-component computation in mutual-recursion form, with its own
  fold (`bits` last-writer, tri-state or-fold, plus the `TriProp` un-tristate
  pass).

That makes seven, in two regimes the issue never names: a **batch-recompute**
regime (loader, `Util.partition`, `AddWire`, exporter, test harness) and an
**incremental-maintenance** regime (`absorb` on merge at
`SimpleEditor:4092,4210`, `makeNet` on split, `recheck` on detach at
`LogicElement:259,268,441,450`, plus hand-rolled `add`/`setNet` at five more
editor sites). The task extracts the first regime and leaves the second
untouched — so after it lands, JLS still has two independent implementations
of what a net is, and `ArchitectureRulesTest#netPartitionHasExactlyOneImplementation()`
as specified ("the walk body appears in exactly one file") passes anyway,
because `traverse()` and the survivor loop are not that body. **A completion
criterion that certifies FEAT-004 done while the other regime survives is a
worse outcome than not having the criterion.**

Worse for the proposed API specifically: `NetPartition.of(List<WireEnd>,
EnumSet<Fold>)` has **no edge predicate**, so the survivor planner — the most
intricate partition consumer in the tree — cannot consume it. An abstraction
that fits three of seven call sites and is unable to express the fourth is
not the seam.

## The folds already disagree three ways, and this task freezes that in

O3 catches `Util.partition` vs `finishLoad`. The divergence is wider:

- `Circuit.finishLoad:1378` and `WireNet.makeNet:139` — `bits` is
  **last-writer-wins** over visit order.
- `WireNet.recheck:279` — `bits = Math.max(p.getBits(), bits)`.

Those are different functions of the same component. `docs/simulation-semantics.md`
treats "the net's declared width" as a well-defined property that `Constant`
truncates to (§S7), and cites **`WireNet.recheck`/`makeNet`** — not
`finishLoad` — as the normative authority for tri-state-ness. So the spec's
anchor is in the regime this task does not touch, and §7.10's formalization
of `b(C)` as last-writer-wins mints a *second* normative authority for net
width that the spec does not cite. Publishing a formal definition of a fold
that contradicts the one the spec points at is a step away from having one
net semantics, not toward it.

## The determinism defect being deferred is not latent

Open Question 1 recommends preserving `Util.partition`'s hash-ordered input
"because changing it is a behaviour change this task's goldens cannot
validate." Check where it runs: `SimpleEditor:5096` (paste),
`SimpleEditor:5481` (copy), `SubCircuit:352` (subcircuit expansion). It
iterates `Circuit.getElements()`, and `Circuit.java:48` is
`private Set<Element> elements = new HashSet<Element>()`. Per
`docs/simulation-semantics.md` §9, net order *is* multi-driver resolution
order. So **after a paste, which driver wins a bus conflict is a function of
identity hashes** — and the circuit is then saved with that net order baked
into wire-end sequence. #98 exists precisely to forbid this. Deferring it
under "the goldens can't see it" inverts the reasoning: the goldens can't see
it because there is no golden for a pasted circuit, not because it is benign.
A visionary read says fix it here and add the fixture, or at minimum stop
calling it an open question and call it a filed bug with a number.

## Reframing: give the invariant an owner, not the algorithm a package

Concrete alternative, in the order I would build it:

1. **Put the walk on `WireNet`, in `jls.elem`.** `WireNet` already *is* the
   component type: ordered `LinkedHashSet` ends and wires (whose order
   §9 makes normative), `bits`, `hasinput`, `triState`, plus `absorb`,
   `makeNet`, `recheck`. The proposed `NetPartition.Component` record
   `(ends, wires, bits, triState, driven)` is a field-for-field shadow of it.
   Add `WireNet.partition(Sequence<WireEnd>, EdgePredicate)` returning
   `List<WireNet>`. `jls/elem/` is **already** in
   `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` with `BASELINE = Set.of()`
   (`test/jls/HeadlessCoreRatchetTest.java:74-79,90`), so P7's entire
   ceremony — new package, `package-info.java`, two ratchet registrations —
   evaporates. No new top-level package is minted for `docs/grand-architecture.md`
   §5 to have to fold into `jls.core` later; the arc there is *one* headless
   kernel, and `jls.netlist` is a sibling that will need moving again at #77.
2. **Drop the purity hypothesis (H1).** Count the pure consumers: `finishLoad`
   mutates immediately, `Util.partition` mutates immediately, `AddWire`
   mutates immediately, `HdlExporter` never walks at all (it reads
   `we.getNet()` and only fuses). H1 buys a pure/mutating split that **zero
   call sites need**, at the cost of a duplicate data structure and a stage-4
   application step re-implemented in three callers. `net.add(end)` and
   `end.setNet(net)` are two halves of one invariant; making them separable is
   the disease, not the cure.
3. **Add the edge predicate from day one**, and convert the survivor planner
   and `WireNet.makeNet` as first-class targets rather than an "audit
   `CircuitAssert`" footnote. `makeNet` becomes `partition` restricted to one
   net's edges; the survivor planner becomes `partition` with
   `survivors::contains`. That is when the copy count actually reaches one,
   and when the ArchUnit rule can assert something real: *no worklist over
   `Wire.getOtherEnd` outside `jls.elem`*, which `traverse()` and the survivor
   loop both violate today.
4. **Unify the fold against `recheck`'s `Math.max`, in a separate commit with
   its own goldens.** The task is right that this must not ride on "the
   goldens did not move." It is wrong that it can therefore be indefinitely
   deferred — two definitions of net width is exactly the ambiguity #318 will
   persist into the file format.

## The jump coarsening is aimed at the wrong tier

`JumpAliasing.fuse` is framed as a coarsening that only `HdlExporter` needs.
Look at the consumers: #366 (PCB netlist) needs jump-transitive nets — a
KiCad net list that splits one electrical node in two because a jump crossed
it is simply wrong. #332 (partition cut) needs jump-transitive. #373 (naming)
derives names *from* jump names. `CircuitAssert` needs jump-transitive.
#318 mints a persisted id per net — and the issue says it keys off *this*
partition, the fine one.

So five of six downstream consumers want the coarse relation, and the fine
one is wanted by exactly one consumer: the simulator, because `JumpStart`/
`JumpEnd` propagate as ordinary elements (`JumpEnd.react:407-428`). The fine
partition is a **simulation implementation detail**. Naming the coarse
relation `ElectricalNet` and treating the wire-only grouping as
`WireNet`-the-propagation-group would make the tiering legible, and would stop
#318 from minting persisted, file-format-visible identities against a
simulator artifact. That is the reframing I would most want the author to
weigh before #318 starts, because it is the one that gets expensive after ids
are on disk.

## What I keep

Everything about the *invariance* discipline: byte-identical goldens as the
primary datum (§9), no fixture regeneration, the explicit refusal to fold
#376's complexity fix in (§11), `IllegalStateException` text preserved
verbatim, and the demand that `Util.partition`'s divergence be recorded rather
than silently harmonised. That is the right way to move code. My objection is
to *where it moves to* and to *how much is declared done* when it lands.

## Verdict

**endorse-with-reframing.** The goal is right and load-bearing for #336, #318,
#366 and #332. I am explicitly disregarding P1, P6 and P7 as written — the
`jls.netlist` package, and the ArchUnit rule phrased as "the walk body appears
in exactly one file." Substitute: the pass lands in `jls.elem` on `WireNet`
with an edge predicate; the rule asserts no wire-graph worklist exists outside
`jls.elem`; `WireNet.makeNet`, `WireEnd.traverse`/`Wire.traverse` and
`SimpleEditor:979-995` are in scope, not audited-and-noted; and
`Util.partition`'s hash-ordered input is a filed bug with a number, not an
open question with a "preserve it" default.
