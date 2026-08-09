# Issue #641: TASK-C563-1: a selected combinational region enumerates as a truth table golden-tested against exhaustive simulation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Under the tier machinery, #641 is the first brick of the one thing JLS structurally
cannot do today: **look at drawn logic and say what function it computes.** Everything
JLS ships runs in the other direction — you draw, it simulates, you read three lines of
stdout. #515 names the competitive consequence (Digital, DEEDS and Issie all close the
analysis loop; JLS ships none of it) and #306's abstract names the pedagogical one (a
submission wrong on 254 of 256 inputs passes the shipped autograder). Both capstones are
downstream of the same missing idea: *a cone of gates as a value you can reason about
rather than only a thing you can poke.*

The goal is right and the ordering correction in the first comment (re-point at #872,
not the #306 capstone) is right. Three reframings follow, in descending order of how
much they change what gets typed.

## 1. AC-1's oracle is circular, and fixing it is where the design lives

AC-1: the table is "golden-tested against exhaustive simulation of the original
circuit." On this codebase the only sane enumerator *is* exhaustive simulation — drive
the input frontier, run `Simulator.runEventLoop` to quiescence, read the output
frontier. AC-1 then asserts `f(x) == f(x)`.

The escape "write a second, non-simulation evaluator" is closed by the tree:
`ARCHITECTURE.md:341-368` records the standing decision that the event-queue interpreter
is JLS's **sole** simulation execution strategy, and binds any future second pass to
observable identity with `docs/simulation-semantics.md` plus bit-for-bit agreement with
the #202 RV32I golden. A levelized cone evaluator written inside #641 is exactly the
second strategy #221 refused, smuggled in as a test oracle.

So say it plainly: **I am disregarding AC-1's golden clause as written.** Replace it
with two criteria that are actually falsifiable today —

- **Stated goldens, not machine-derived ones.** Check in expected tables for circuits
  whose function is known by hand: 2:1 mux, 3-input XOR parity, a 2-bit adder with carry
  (multi-output, satisfying AC-3 at the same time). The table is pinned against human
  arithmetic. That is the only thing that can catch a wrong table today, and it is what
  `BatchSimulationGoldenTest` already does for the simulator itself.
- **A differential seam instead of a differential test.** Have #641 produce its table
  through a one-method interface — `ConeEvaluator.evaluate(cone, inputVector) → outputs`
  — with a single `SimulationConeEvaluator` implementation. Costs nothing now. But when
  CAP-09 builds the AIG/CNF core its abstract commits to, a second implementation drops
  in behind the same interface and **#641's goldens become the genuine differential
  oracle AC-1 wants**, with zero rework. The circularity is not resolvable in this task;
  it is resolvable by one interface declaration that lets a later task resolve it.

## 2. The cell domain is the unaddressed half of AC-4

AC-4 argues *width* — expand a bus to columns or refuse — and the first comment settles
it (MSB-first bit expansion, matching `TruthTable.makeRowCode`,
`src/jls/elem/TruthTable.java:1064-1078`). Correct call. But nobody asked what a **cell**
may contain.

`docs/simulation-semantics.md` §2 is two-state plus HiZ, HiZ is all-or-nothing per
signal, and §2/§9 note that nearly every `react` treats a HiZ input as zero. A cone whose
output is undriven for some input combinations therefore yields a cell reading `0`,
indistinguishable from a computed `0`. That is *literally* the defect #306's abstract
indicts in the shipped element — `TruthTable.java:1447-1449`, `// don't care becomes
false` / `if (outValue == 2)` / `outValue = 0;` — reproduced in the new direction, in a
task filed to serve the capstone whose thesis is "never a false pass."

Downstream this is load-bearing, not cosmetic: #564 AC-2 wants don't-care rows honored,
and #565 AC-1 wants a synthesized circuit's table to "round-trip identically." A table
whose cells are `{0,1}` cannot round-trip a table whose cells are `{0,1,X}`, and the
whole minimizer's leverage is don't-cares.

**Make the cell a 3+-valued enum in this task** — `ZERO | ONE | HIZ`, with `UNKNOWN`
reserved for #322 — even though today's evaluator will only ever emit the first two.
#641 defines the type every downstream consumer reads; widening it later is a format
break across #564, #565, #646 and the batch schema. This is the one change I would treat
as non-negotiable inside #641's own scope.

## 3. The unnamed real work: there is no way to force a net

The issue reads as if enumeration is a loop. The loop is trivial; **driving the frontier
is not.** JLS has no "force this net to a value" API — values enter through `InputPin`s
and `Simulator.initInputs` (`src/jls/sim/Simulator.java:149-156`), and propagate through
`WireNet.propagate`. A cone's input frontier is, by #872 AC-1's definition, nets driven
from *outside* the selection. Nothing today can drive them.

Two routes, and only one is in the project's grain:

- **(a) Build a cone circuit.** Copy the circuit through the existing save/load pipeline,
  delete everything outside the cone, splice `InputPin`s onto the input frontier and
  `OutputPin`s onto the output frontier, then run `BatchSimulator` on it once per row.
  `CircuitSnapshot` (`src/jls/edit/CircuitSnapshot.java:30-95`) already establishes this
  idiom — deflated save text restored through the ordinary `load`/`finishLoad` path, so
  "copy semantics are exactly save/load semantics." Headlessness (#872 AC-5) falls out
  free, because `BatchSimulator` already takes a `Circuit`. So does reuse of the entire
  test-vector and VCD surface for #646.
- **(b) Add a force-net hook to the sim core.** Touches `jls.sim`, touches §6 propagation
  semantics, and puts a non-drawn stimulus path into the one component
  `HeadlessCoreRatchetTest` and #221 guard hardest.

(a) is the elegant route and should be named in the issue, because it is where the "1 mw"
actually goes — and because a cone-as-circuit is independently the right input to #565's
synthesis comparison and to any future HDL emission of a sub-region.

## 4. The larger reframing: is the produced value a new type, or `jls.elem.TruthTable`?

The strongest out-of-the-box option here, which the CAP-31 family never considers: make
the analyzer's output **an actual `TruthTable` element**, offered for insertion into the
circuit in place of the selected region.

Everything downstream collapses:

- **The view (#644) already exists** — `jls.edit.TruthTableEditor` + `DisplayBool`.
- **The persisted format already exists** — a `SaveTags` row, `setValue` plumbing,
  `AllElementsRoundTripTest` coverage, and a Verilog emission path already sketched
  (`ISSUE-AMBIGUITIES-2026-07.md:387`, TruthTable → `case`/`casez`).
- **#565's round trip becomes a save-text comparison** — synthesize from a table element,
  re-analyze the drawn result, compare the two elements' save records. That is the
  existing round-trip infrastructure, not a new equality relation over a bespoke type.
- **The pedagogy improves.** A region of gates visibly *collapses into* a table the
  student can then re-synthesize into gates. CAP-31's analysis/synthesis loop becomes a
  gesture in the editor, not a read-only report window.

The honest cost: `jls.elem.TruthTable` must first stop destroying don't-care on output
(`:1447-1449`) and stop silently holding outputs on an unmatched row (`:1432-1434`).
But #306 already lists both as things it must fix, so this is convergence — one fix
serving two capstones — rather than added scope. Keep the in-memory analysis value
(§2's enum-celled table) as what #564 consumes; make the *element* its persisted and
viewable form. Even if #641 stops short of inserting elements, choosing the element's
column/row semantics as the value's semantics is free insurance.

## Where the seam between CAP-31 and CAP-09 is actually cut

#872 declares the shared component to be the **extractor**. That is the cheap half. The
expensive half, buildable twice, is the *functional representation of a cone*: CAP-31
needs it for tables, minimization and round-trip; CAP-09 needs it for AIG, CNF, miter and
counterexamples. If the shared value eventually becomes a canonical cone netlist rather
than a cone selection, #565's round-trip check stops being table equality (2^N-bounded)
and becomes equivalence (not bounded by 2^N at all) — a 24-input synthesis could be
verified where its table cannot be printed. I am **not** asking #641 to build that; #221
and KC-31-1 both counsel against speculative machinery. I am asking #641 to leave the
door open by returning its table from a `ConeEvaluator` (§1) rather than from a static
call into the simulator.

## Smaller notes

- **AC-2 (determinism)** is right but should *cite* #872 AC-4's stable-id frontier order
  rather than state a second ordering rule; two documents defining one convention is the
  divergence the boundary notes exist to prevent. `Circuit.getElementsInStableOrder()`
  already exists as the mechanism.
- **Row order is still unstated.** The comment fixed column semantics, not enumeration
  order. Ascending binary count over the expanded bit vector, MSB = leftmost column, one
  sentence, done — Gray-code ordering would be a gratuitous incompatibility with every
  textbook table students compare against.
- **AC-3 is already satisfied by construction** once the value is one table over #872's
  ordered output frontier; it costs one multi-output golden (the 2-bit adder above), not
  a design decision.
