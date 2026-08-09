# Issue #848: TASK-C370-4: the column store and the event queue resolve an element to the same index, and a test fails the moment either grows a private mapping
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of the acceptance criteria, #848 asks for one thing: **a circuit should have
exactly one notion of "which element is this", and it should be impossible — not merely
discouraged — for a second one to appear.** That end is right, it is the load-bearing
half of #370 invariant 4 ("no second representation maintained by discipline"), and it is
the single place where #370's and #362's "one contract cited by both" stops being
prose. I endorse the end without reservation.

I am, however, disregarding AC-1 and AC-3 as written, because the route they take
manufactures the coupling they exist to police, and lands it after the point where it
would have done its work.

## Ground truth in the tree

- `src/jls/sim/SimEvent.java:100` — the queue's element identity today is
  `private final Reacts callBack`, a bare object reference. `equals` compares it by
  *reference identity* (`:161-172`), `seq` excluded. There is no element→index map on
  the queue side and no place one wants to live.
- `src/jls/sim/Reacts.java` has exactly one implementor:
  `src/jls/elem/LogicElement.java:17-22`, an `abstract sealed class ... permits` 24 named
  subtypes. The queue's identity universe is therefore already closed and nameable.
- `src/jls/elem/Element.java:17-18` is `sealed ... permits DisplayElement, LogicElement,
  Wire`. The *column store's* universe (`src/jls/Circuit.java:47-48`, a `HashSet` of all
  elements including `Wire` and display-only elements) is strictly larger than the
  queue's.
- 27 `new SimEvent(` sites exist in `src/`. One of them,
  `src/jls/elem/WireNet.java:506-507`, casts `(Reacts) element` on a `LogicElement` that
  already implements `Reacts` — a small tell that the callback slot is typed at the wrong
  altitude.
- `test/jls/ArchitectureRulesTest.java` + `pom.xml:110` mean the ArchUnit machinery AC-2
  asks for already exists, alongside the source-scan ratchet pattern
  (`HeadlessCoreRatchetTest`, `NotificationRatchetTest`).

## The reframing: make the index the identity, and there is nothing left to agree

#848 treats "same index" as a *property of two subsystems* and buys a test to watch it.
The cheaper and far more durable move is to make it a property of the *type system*:

1. The allocator from #843 mints the dense index and stamps it on the element itself —
   one `final int` on the sealed `Element` (or `LogicElement`) hierarchy, no map anywhere.
2. `SimEvent` stops carrying `Reacts callBack` and carries that `int`. Dispatch becomes
   `elements[index].react(...)` through a `Reacts[]` column owned by the same store.
   `equals`'s reference-identity semantics are preserved *exactly* — for a loaded
   circuit the index is bijective with the element — so #476's O5/P5 contract is unmoved.
3. #476's hardest open hypothesis, H3 ("the flag can be reached from `(callBack, todo)`
   identity **without hashing**"), stops being a hypothesis. The intrusive queued flag
   becomes a bit per `(element index, payload kind)` — a `long[]` column indexed exactly
   like every other column in #370's store, allocated and cleared by the same owner.

Under this framing AC-1 has nothing to assert (there are not two mappings to compare),
AC-2's structural rule collapses to "only the allocator may mint an index", and AC-3's
"stated in exactly one place" is discharged by a type rather than by a sentence and a
review convention. **The problem disappears rather than being watched.**

Note the direction of causation the issue misses: as written, #848 requires the queue to
*acquire* an element→index resolution it does not have and, per #476's design, does not
need — solely so a test can compare it against the column store. That is a test creating
its own subject.

## Three concrete defects in the criteria as written

**AC-1 may forbid the correct design.** The two index universes are genuinely different
sizes: the columns must address `Wire` and `DisplayElement`; the queue only ever
addresses `LogicElement`. A dense-over-reactors space is the cache-friendlier one for a
flag column, and it is *legitimate* — provided it is a total injective projection owned by
the same allocator rather than a parallel map. "Identical indices" outlaws that by
accident. The invariant worth stating is **one allocator, one identity, derived spaces are
functions of it**, not "the same integer falls out of both".

**AC-1 tests the trivial half.** Resolving a static, freshly loaded fixture proves
agreement at t=0. The failure that actually happens is temporal:
`src/jls/Circuit.java:342` and `:356` add and remove elements at *edit* time, so
"contiguous from zero, stable for the lifetime of the loaded circuit" (#843 AC-1) meets a
user deleting a gate mid-session. Either the space goes sparse or it renumbers and every
pending event and flag bit is silently misaddressed. The test that earns its 1-2 mw
mutates the circuit — add, delete, undo, reload — and asserts the queue's in-flight
identities survive it. That test would have caught something; a sample scan will not.

**AC-3's enforcement is a review convention in a repo that ratchets.** "A second
statement of the contract fails review" is precisely the discipline #370 invariant 4
refuses to accept elsewhere. The house style is available: a grep-ratchet asserting the
contract sentence occurs in exactly one file, in the manner of `NotificationRatchetTest`,
plus an ArchUnit rule that no class outside the allocator declares a `Map`-typed field
whose name matches the index vocabulary. Both are cheap; neither is a promise.

## Sequencing: this is early work, not late work

`ordering_after` puts #848 behind #843 and #846. That is backwards for the half that
matters. #476 will otherwise choose its own queued-flag representation (its Open
Question 1 explicitly leaves the bucket/flag shape to the implementer), and *that* choice
is the "private mapping" #848 later polices. The index contract must exist **before**
#476 executes, or the one-contract-cited-by-both claim is retrofitted onto a fait
accompli. Concretely: fold the contract and the type into #843 (which already asserts
dense stable indices over all 35 registered types — AC-1 here largely re-samples it),
mark it a hard predecessor of #476, and keep #848 only as the mutation-and-structure test
described above, at a smaller band.

## Alignment with the project's arc

This is one of the few pieces of the CAP-17 program whose value is independent of CAP-17.
If the 10^10-element capacity capstone is descoped tomorrow — and the pedagogy floor that
#370 invariant 3 makes vetoing over its own capacity gain suggests that tension is real
for an educational tool — a single element identity with a hashless flag column still pays
for itself in #362's 47.7% queue+dedup share, and still simplifies `WireNet`'s cast. That
argues for promoting it out of the FEAT-054 tail and into the shared foundation both
features stand on, which is exactly what the reframing does.

## If the issue is kept as written

Minimum changes to make it worth executing: (a) restate AC-1 as "one allocator; any
derived space is a recorded total injective projection of it", (b) require the test to
exercise element add/remove/reload rather than a static sample, (c) replace AC-3's review
convention with a ratchet, (d) move the contract statement itself into #843 and re-order
this task before #476. AC-4 (golden corpus unchanged) is correct and needs nothing.
