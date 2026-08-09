# Issue #655: TASK-C565-4: a synthesized circuit's extracted table is identical to the table it was synthesized from — the loop closes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Not a test. #655 is the only place in CAP-31 (#515) where the two halves of the
analysis loop are made to *mean* something about each other. #563 proves
extraction against exhaustive simulation; #653 already proves synthesis against
the table it came from ("whose simulation matches the table on every row",
TASK-C565-2 AC-1). #655's unique contribution is not "synthesis is correct" —
that is #653's — it is the claim that JLS holds **one notion of what a truth
table means**, shared by the analyser, the synthesizer, the simulator and the
exporters. That is a genuine architectural obligation and it deserves to exist.

The mechanism chosen to discharge it — compare two table artifacts for identity,
with a bespoke don't-care escape clause — is the weakest available form of that
claim, and it pulls against three things the project has already decided.

## Where it pulls against the trajectory

**1. "Identical" and the don't-care rule contradict each other.** AC-1 requires
identity; AC-2 requires that a source `X` matched against an extracted `0`/`1`
*not* fail. Those are two different relations. The second one has a name and the
project has already chosen it: `docs/capability-roadmap/lf-04-formal-and-grading.md`
enumerates strict / **refinement (recommended default)** / two-state projection,
and says refinement "is exactly synthesis's don't-care semantics and exactly what
a grader wants." #655 is re-deriving that decision in an acceptance criterion,
informally, instead of citing it. Refinement is a partial order, not an equality,
and a test built around `assertEquals` on tables will fight that shape forever.

**2. The oracle #655 would pin is the bug #306 exists to remove.** JLS already
ships the artifact this task treats as hypothetical: `jls.elem.TruthTable` stores
`0/1/2` per cell (`src/jls/elem/TruthTable.java:79`), honours `2` on the input
side (`:1413`), **destroys it on the output side** (`:1447-1449`, `// don't care
becomes false`), and silently latches on an unmatched row (`:1432-1434`). CAP-09
(#306) names those exact lines as the thing that makes vector grading "actively
wrong", and lf-04 calls the don't-care-capable value type "the single hardest
dependency in the whole capability." A CI round-trip whose stated don't-care rule
is "the synthesized circuit produces *some* definite value there" will, in
practice, be satisfied by whatever the synthesizer emits for `X` today — and once
it is green in CI it becomes a third site (after `TruthTable.react` and
`HdlExporter.buildTruthTable`, `src/jls/hdl/HdlExporter.java:792,823`) that has
to be renegotiated when P1's four-state work lands.

**3. Its failures are, by its own boundary note, not actionable here.** "A
failure traced to the extractor is filed against #306, not patched here." Combine
that with #653 AC-1 already covering the synthesis leg, and #655's failure mode is
almost always someone else's bug surfacing through a fixture set this task owns.
That is a property of the *loop*, not a task with its own fixtures.

**4. A JLS truth table is not a function, and only functions round-trip.** The
shipped semantics are a **priority** table: first match wins, and no match leaves
the outputs holding (`HdlModel.java:592-596` documents this as normative for the
emitters — `casez` with no `default`, `std_match` with no `else`). So a
hand-authored source table can specify something that is *not realizable* as the
combinational circuit #563's extractor is required to accept: an incomplete table
is a latch, and #563 AC-3 rejects sequential content by name. #655's fixture list
(AC-3: multi-output, shared products, constant-output) never mentions the
incomplete table, which is precisely where the round trip is undefined rather
than merely hard.

## Alternative framing A (primary): the round trip is element↔expansion substitutability

JLS already has both endpoints as *model objects*, not as views: `TruthTable` is
a palette element with an editor (`src/jls/edit/TruthTableEditor.java`,
`TruthTableDialog`, `TruthTableRenderer`), and `SubCircuit` exists. Reframe
FEAT-C31-3 as **"expand a TruthTable element into gates"** and #655 becomes:

> Replacing a `TruthTable` element with its synthesized expansion does not change
> the behaviour of the circuit it sits in.

What that buys, concretely:

- **The don't-care rule disappears as a special case.** Substitutability is
  checked against the element's *own* simulation semantics, whatever they are
  today and whatever P1 makes them tomorrow. When `:1447-1449` is fixed, the test
  does not need editing — it re-derives.
- **#563 and #306 come off the critical path.** No extractor is involved. The
  oracle is the shipped batch simulator and the existing golden harness
  (`BatchSimulationGoldenTest` lineage), which every other correctness claim in
  this repo already rides on.
- **TASK-C565-1 mostly evaporates.** "A student types and edits a truth table"
  is a shipped dialog. The synthesis entry point becomes a context action on an
  element that is already in the palette, already saved, already round-tripped by
  `AllElementsRoundTripTest`.
- **TASK-C565-3's layout problem shrinks.** Free placement becomes "fill this
  element's footprint, honour its existing pin positions" — anchors, not a blank
  canvas.
- **It pays a debt elsewhere.** `TruthTable` is one of the special-cased entries
  in `HdlExporter.EXPORTED` (`:428`) needing hand-written priority-`casez` and
  `std_match` templates in two emitters. An expansion pass gives the exporters a
  gate netlist they already handle, and gives `-savetext` diffs something a human
  can read. Synthesis stops being a CAP-31-only feature and becomes a general
  lowering pass with three consumers.

The inverse operation completes the picture and is the *real* prize: #563's
extraction, today framed as "a table in a view", could instead produce a
`TruthTable` **element** — abstract a selected region into one element, expand it
back into gates. Then CAP-31's loop closes inside the circuit model, as two
editor operations that are inverse up to refinement, and the round-trip property
is "abstract-then-expand preserves behaviour." That is a far more teachable
artifact than a table view, and it is the same gesture Digital's users know.

## Alternative framing B: let CAP-09's miter be the comparator

If the extractor leg must stay in scope, do not write a table-diffing harness with
"names the first differing row and column" (AC-4). That is a hand-rolled
counterexample reporter for a project that is building a real one: #306's formal
core produces a counterexample that *replays in the GUI*. Express #655 as a miter
between the source table (instantiated as a `TruthTable` element) and the
synthesized netlist, with don't-care rows constrained out of the care set —
textbook don't-care equivalence checking. One verdict engine, N consumers, and
CAP-31 becomes an early customer that pressure-tests CAP-09 rather than growing a
parallel comparison vocabulary. The cost is ordering: #655 then waits on the
formal core, which is why framing A is the one I'd take first.

## If neither reframing is taken: the minimum repair

Keep the task, but (a) replace "identical" with **containment/refinement**, cited
to lf-04, stated once as `extracted ⊒ source`; (b) reuse #653's fixtures rather
than defining a second set — the fixture list is the same three shapes plus the
one that matters; (c) add the incomplete-table fixture and state its outcome
(synthesis of an incomplete table is *refused with a reason*, since the honest
realization is a latch and #563 will reject it); (d) drop the bespoke row/column
differ in favour of whatever #563's machine-readable batch output already emits.

## What I am disregarding, and why

I am disregarding AC-1's word "identical" and AC-2's "stated round-trip rule" as
written. Identity is the wrong relation — the project has already ruled that
refinement is the right one, and encoding a local don't-care convention in a green
CI test creates a fourth copy of a semantics the roadmap intends to change. I am
also disregarding AC-3's fixture ownership: fixtures for the synthesis leg belong
to #653, and a loop-closure assertion should consume them.

## Second-order

If framing A lands, CAP-31 PF-4 (FSM parity) gets the same shape for free:
`StateMachine` is also a palette element with an editor, and "expand a state
machine into registers and logic, and prove the expansion substitutable" is the
identical property with a different element. One `expand` seam, two capstone
features, and a lowering pass the HDL exporters and any future compiled-evaluation
strategy (#221's revisit trigger) can all stand on. That is the version of this
issue that strengthens the arc rather than merely closing a loop.
