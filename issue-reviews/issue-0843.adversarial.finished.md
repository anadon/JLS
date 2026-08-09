# Issue #843: TASK-C370-2: every element gets a dense integer index and a primitive column store exists beside the object graph, with behaviour unchanged by construction
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is a "seam PR" task: assign every element a dense `int` index and
stand up an inert primitive column store, changing nothing observable. Its
own acceptance criteria are unusually precise and check out against the
tree (see "What's solid" below). The problems are not in what it says but
in what it omits: it does not acknowledge the hard prerequisites its own
parent feature declares, it has no policy for index churn under circuit
mutation, and it locks in a column schema before the value representation
it must hold is decided.

## Findings, most severe first

**1. The issue is silent about #370's declared hard prerequisites, one of
which explicitly warns this exact work may need to be redone ("pay
twice").** #843's yaml only declares `ordering_after: ["TASK-C370-1"]`
(#842). But its `part_of_feature: 370` parent (#370, FEAT-054) declares
`blocked_by: [322, 335, 362]` — FEAT-026 (#322, the four-state value
core), FEAT-009 (#335, the measurement gate), and FEAT-030 (#362, the
engine/queue constant-factor work) — and I verified all three are still
`state: open` as of today. #370 §6 states plainly: "FEAT-030 (#362) …
because it is the same work from the throughput side and the two must be
**one purchase** or the array layout is written twice with different
invariants," and its Re-planning Protocol names the exact failure mode:
"FEAT-030 (#362) is funded first: this feature is **re-scoped, not
re-estimated** — the REPLAN removes the array-layout work from the band.
**This is the most likely way to pay twice.**" TASK-C370-2 *is* that
array-layout work (#370 §2 row 2). Landing it now, with none of #322,
#335, #362 landed and no REPLAN check performed, risks building exactly
the scenario #370 names as its most likely failure. The issue should
either cite a REPLAN confirming these three are safely decoupled, or
declare `blocked_by` on them explicitly.

**2. No policy for index churn under circuit mutation — "stable for the
lifetime of the loaded circuit" is undefined for a mutable model.**
`Circuit.elements` (`src/jls/Circuit.java:48`, a `HashSet<Element>`) is
mutated constantly by the editor (`SimpleEditor` add/delete/undo/redo),
and per `ARCHITECTURE.md` undo "restores through the ordinary load path"
— i.e. every undo/redo effectively reconstructs the element set from
scratch. The issue never says whether: (a) an element added *after*
load gets an index at all (if not, TASK-C370-3's migration has nowhere to
put its state for editor-created elements, silently breaking the very
elements a student is drawing); (b) a deleted element's index is
tombstoned, reused, or leaks; or (c) undo/redo re-indexes everything
(cheap if rare, expensive if it fires on every gesture, which
`CircuitSnapshot`s do). AC-1's "stable for the lifetime of the loaded
circuit" reads as if elements are only ever added at load time, which
contradicts how JLS is actually used. This gap should block #843 as
written, since #850/#851 (TASK-C370-5/6, the spatial index and the
editor) depend on exactly this contract and neither restates it.

**3. The column schema shape is unspecified for 35 structurally
different element types, and locking it in now conflicts with an
undecided value representation.** The Outcome paragraph says "the
primitive-typed column store is introduced with its columns allocated
and addressable," but never states whether this is one flat schema
shared across all 35 registered types (`ElementRegistry.ALL`, verified
at exactly 35 entries) or per-type column sets. #370's whole
justification is a byte budget (≤150 B/element, §3/§Open-Question-2);
a naive union schema across `Register`, `Memory`, `TruthTable`,
`Gate`, etc. could easily blow that budget before TASK-C370-3 ever
migrates real state into it, and none of #843's ACs test the schema's
fitness against that budget — that check is explicitly deferred. Worse,
FEAT-026 (#322) has an **open, unresolved** disagreement about the value
type's shape itself (two-plane `aval/bval` vs. three-plane
`Word(width,a,b,u)` — #322 Open Question 1, "these disagree, and the
disagreement is load-bearing"). Any column allocated for per-signal
value state now is a bet on an answer #322 hasn't given yet.

**4. No performance budget on the allocator itself, despite feeding a
feature-veto invariant.** #370 invariant 3 — "the editor's per-edit cost
and startup time do not regress … outranks the capacity gain … can veto
the whole feature" — is the criterion #370 itself calls "most likely to
fail the feature" (§5 criterion 3). TASK-C370-2 is the task that
introduces the index allocator every future edit may have to call
(per finding 2), yet none of its five ACs mention allocator cost,
big-O, or even a smoke measurement. If the allocator turns out to be
O(n log n) or triggers a full table rebuild per edit, that surfaces as a
regression two tasks later (#851) rather than being caught here where
the allocator is actually written.

**5. AC-1 is gameable as worded.** "Every element in a loaded circuit
has a dense index, contiguous from zero, stable for the lifetime of the
loaded circuit; asserted on a fixture with all 35 registered element
types present" describes a one-shot check at load time. A test that
loads the fixture once and asserts `indices == {0..34}` satisfies the
letter of AC-1 while saying nothing about stability across the mutation
paths in finding 2, or about what happens on a second circuit loaded in
the same process (does an allocator have global vs. per-`Circuit` state —
another unstated design decision that affects thread/GC lifetime and
whether closing one circuit leaks index slots for another).

**6. The task-to-feature link is informal only.** `issue_read get` on
#843 returns `has_parent: false`; the `part_of_feature: 370` relationship
lives only in the yaml prose block, not GitHub's structured sub-issue
relationship the tracker otherwise uses elsewhere in this same task
family. That weakens the enforceability of finding 1: nothing in the
tracker's own hierarchy view would surface #370's `blocked_by` graph to
someone opening #843 cold.

## What's solid

- **AC-1's "35" and AC-2's "27" are both exactly correct against the
  current tree.** `ElementRegistry.ALL` (`src/jls/elem/ElementRegistry.java:38-72`)
  has exactly 35 entries; `grep -c "public void react("` across
  `src/jls/elem/*.java` returns exactly 27. The issue's numbers are not
  asserted, they are verified — a genuine strength.
- **`src/jls/Circuit.java:47-48` is quoted correctly** — line 47 is the
  doc comment, line 48 is `private Set<Element> elements = new
  HashSet<Element>();`, exactly as cited.
- **AC-3/AC-4/AC-5 correctly restate #370's own invariants 1, 5, 6, 7**
  (byte-identical goldens, the headless ratchet, no format change, no new
  SpotBugs exclusion), and the cited files (`test/jls/HeadlessCoreRatchetTest.java`,
  `config/spotbugs-exclude.xml`) exist. No drift from the parent
  feature's stated contract on the points it does restate.
- **The "seam, zero behaviour change" framing is sound engineering
  practice** — separating "introduce the addressing scheme" from
  "migrate state into it" (TASK-C370-3) is exactly the kind of
  reviewable-diff discipline #370 §2 argues for, and I have no
  objection to the decomposition itself, only to what it leaves unsaid.

## Recommendation

Before this lands: (a) add an explicit `blocked_by`/REPLAN note
addressing #322/#335/#362's open status against #370's own "pay twice"
warning; (b) state the index/column churn policy for post-load mutation
and undo/redo, and add an AC that exercises it (add an element, delete
one, undo, assert on indices); (c) either name the column schema's
granularity (per-type vs. shared) and state how it avoids pre-committing
to #322's undecided value shape, or explicitly scope this PR to
structural columns only (no signal-value columns) and say so in AC-1/AC-2.
