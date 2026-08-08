# Issue #565: FEAT-C31-3: a typed or edited truth table becomes a drawn, laid-out two-level circuit whose extracted table round-trips identically
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

CAP-31 (#515) justifies itself two ways, and they are not the same goal. The
weaker one is *parity*: Digital ships truth-table synthesis, JLS does not, so
build it. The stronger one is written in `docs/capability-roadmap/AMENDMENT.md`
(the leapfrog section, ~L630): JLS is the only tool in its class **holding both
ends — the drawing and the meaning** — and the amendment explicitly names
circuit→truth-table as "exhaustive equivalence by enumeration, the right idea
stopping at 2²⁰."

Read against that arc, #565's real purpose is not "add a table→circuit
converter." It is: **make the table and the gates two views of one thing, in
one editor, reversibly.** Every reframing below follows from taking that
seriously, and each one deletes work rather than adding it.

## Three things already in the tree that change the shape of this issue

1. **The layouter has landed.** `src/jls/hdl/layout/HeuristicLayeredLayouter.java:50`
   implements `SchematicLayouter` and is already invoked by the importer. AC-3's
   "where available" hedge, and the first review comment's claim that "#62 is
   open and unlanded," are both stale — #62's own body says the seam and engine
   are on master and what remains is the #290 rubric run.
2. **The whole netlist→drawn-circuit pipeline exists.** `NetlistImporter.Builder`
   (`src/jls/hdl/imp/NetlistImporter.java:410`–`:1030`, ~620 lines) takes
   elements + ports + nets, builds a `LayoutGraph`, solves it, and emits
   `ELEMENT`/`WireEnd` save text that loads through the real loader. Only the
   ~350 lines above it are Yosys-shaped. TASK-C565-2 (#653) and TASK-C565-3
   (#654) as written will build a second one of these.
3. **The truth table already exists as a simulating element.** `jls.elem.TruthTable`
   (1,491 lines), registered, saved (`docs/file-format.md:324`), round-tripped by
   `AllElementsRoundTripTest`, with don't-cares as first-class cells. A student
   can *already* get correct behavior from a table today. What they cannot get is
   the gates. **The value of #565 is transparency, not capability** — and the
   issue never says so, which is why its framing drifts toward "converter."

## Reframing A (the headline): Explode / Collapse, not a converter

Make synthesis and analysis a **pair of inverse editor operations on a
selection**, with the `TruthTable` element as the pivot:

- **Collapse:** select a combinational cone → it becomes one `TruthTable`
  element (this is #563's extractor plus an element constructor).
- **Explode:** select a `TruthTable` element → it becomes minimized two-level
  gates, placed and routed (this is #564's minimizer plus the netlist builder).

Why this is better than "table file in, circuit file out":

- **The round trip stops being a test and becomes an invariant of one op pair.**
  AC-1 today asks two independently-built subsystems to agree; under
  Explode/Collapse, `collapse ∘ explode == identity` on the table and
  `explode ∘ collapse ≡` logically on the cone, checkable at op level. The
  interesting bug — the two paths disagreeing on don't-care handling or input
  ordering — is caught structurally rather than by one golden.
- **AC-3 becomes true for free.** An editor op mutates the live circuit through
  `CircuitSnapshot` undo; there is no "generated artifact" to un-lock, because
  nothing ever produced one. No test needed to prove the result isn't special —
  no code path exists that could make it special.
- **It matches how the feature will actually be taught.** "Show me the gates
  behind this table" and "what does this pile of gates actually compute" are the
  two things a student asks. A File-menu converter answers neither in place.
- **Precedent exists in the codebase's direction**: `SubCircuit` is already the
  hide/reveal abstraction; Explode/Collapse is the same move at the combinational
  level, and JLS conspicuously lacks a flatten today (`grep -rn flatten src/`
  finds nothing) — this would be the first, and it argues for a subcircuit
  flatten later on the same seam.

Constraint this must respect (not a blocker): `docs/operation-layer.md` defers
`EditOrderedRows` to #163, so table *edits* stay dialog-inline. Explode/Collapse
should therefore travel as a **batch of the existing `AddElements` + `AddWire` +
`RemoveElements` ops through `OpSink.submitAll`**, exactly as
`deleteSelectionPlan` does today — one undo snapshot, no new op kind, no impact
on the collab vocabulary. That is a design constraint the issue and its three
tasks do not mention and would likely have discovered late.

## Reframing B: the "table file" already exists — do not invent one

I am **explicitly disregarding AC-5 as written** ("table file in, circuit file
out"), because it implies a truth-table file format, and
`docs/standards-landscape.md:203` records that there is deliberately no standard
for truth tables. Inventing one gives JLS a second serialization to version,
harden against hostile input (#38), and document.

The table file is a **`.jls` file containing a `TruthTable` element**. It is
already specified, already sniffed by `FileAbstractor`, already plain-text
diffable via `-savetext`, already round-trip tested. AC-5 then becomes a flag in
the existing family (`JLSStart.FLAGS`, alongside `-export`/`-savetext`):

```
jls -synth out.jls table.jls     # explode every TruthTable element in the file
```

which reuses the temp-and-rename writer at `JLSStart.java:620` and inherits the
whole CLI contract. No format, no grammar, no new hardening surface. The problem
disappears rather than being solved.

## Reframing C: promote the builder instead of writing a second one

Concrete change to the task decomposition: **replace TASK-C565-3 (#654) with
"file and land #62's planned generated-netlist layout entry point."** #62's
`planned_tasks` already contains *"Layout entry point for programmatically
generated .jls netlists (generator consumers: #202 CPU, #73 sample circuits)
behind the same SchematicLayouter seam"*, and its Open Question 2 asks where
that entry point should live. #565 is the **third consumer** — that plurality
answers the open question (it belongs in #62) and, better, synthesis becomes the
second independent `LayoutGraph` producer, which is exactly #62's **IC7**
("second consumer substitutes behind the seam with no caller changes"), listed
today as "Design intent only." #565 does not merely depend on #62; it *closes an
open integration criterion of #62*. That is the strongest alignment argument in
this issue and it appears nowhere in it.

Mechanically: promote `NetlistImporter.Builder` out to a netlist-neutral
`jls.hdl.imp.CircuitBuilder` (add element / add net / emit save text), leaving
`NetlistImporter` as the Yosys-shaped front end. Yosys import is consumer 1,
truth-table synthesis consumer 2, the #202 generator consumer 3. AC-4 of #654
(deterministic, byte-reproducible) is then inherited rather than re-proven, and
TASK-C565-2/-3 collapse from ~2 mw to well under 1.

## Reframing D: two walls, two refusals

AC-4 ("Table-entry bounds match the analysis bounds") forces one N across two
different failure modes. They are not the same wall: analysis is bounded by 2^N
row enumeration, synthesis by Quine–McCluskey prime-implicant growth over
minterm count. A 16-input table with four minterms is trivially synthesizable and
absurd to type; a 12-input table near half-density can blow up QM. Enforcing one
shared N will be either needlessly restrictive in one direction or a hang in the
other — the precise failure CAP-31 AC-3 exists to prevent. State both bounds,
refuse by naming *which* wall was hit and with which arithmetic. Entry is capped
at the lower of the two, but the message says which.

## Alternatives considered and rejected

- **Route synthesis through Yosys** (`jls.hdl.yosys.YosysLocator` already exists,
  so the plumbing is there): rejected. It makes a classroom feature depend on an
  optional external toolchain, and Yosys's technology-mapped output is not the
  two-level SoP the student's textbook and #564's QM result predict — the whole
  pedagogical contract is that the drawing matches the expression.
- **A `SynthesizedCircuit` element type**: already correctly refused by #653 on
  registry-totality grounds (#315); Explode/Collapse makes the temptation vanish
  entirely since nothing persists a "synthesized" state.
- **Espresso-class heuristic minimization**: refused by KC-31-1. Correct — and
  Reframing D makes the refusal message honest rather than a stand-in for it.

## Where this pulls against the project arc

Nowhere significant. The one live risk is duplication, and it is the default
outcome of the current decomposition: #653 and #654 read as standalone
netlist-and-layout work, and nothing in them points at
`NetlistImporter.Builder`. Left as written, JLS ends up with two gate-netlist
intermediate models (`HdlModel` for export, `YosysNetlist` for import) and a
third ad-hoc one for synthesis — three representations of "a circuit as gates and
nets" in one 30k-line project. That is the single thing this issue should be
re-planned to prevent.

## Verdict

**endorse-with-reframing.** The direction — the synthesis half of the analysis
loop — is right, is genuinely absent, and is the interesting half of CAP-31.
Re-plan it as: one Explode/Collapse op pair over the existing `TruthTable`
element, emitted through a promoted netlist-neutral circuit builder into the
already-landed #62 layouter, with the `.jls`-containing-a-table as the batch
interface and two separately-named bounds. AC-3's "where available" and AC-5's
implied new file format should both be struck; AC-1's round trip should be
restated as an op-pair invariant.
