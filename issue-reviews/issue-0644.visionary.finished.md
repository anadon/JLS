# Issue #644: TASK-C563-3: a student selects a region and reads its truth table in a view, without leaving the editor
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The outcome is right and I endorse it without reservation: CAP-31 (#515) exists because JLS
loses instructors to Digital, DEEDS and Issie on the analysis loop, and an analysis loop whose
answer arrives only through a batch invocation converts nobody. A student must select gates and
see the table. #644 is the moment CAP-31 stops being a library and becomes a tool.

What I am rejecting is the *seam*. #644 is written as "build a read-only table view," and the
coordination comment on this issue (the #644/#650/#652 split) states that "whichever of the three
lands first establishes the table model and the panel." That question is already answered, and
none of the four C31 features cite the answer.

## The finding that changes the design

**JLS already ships a truth-table model, a truth-table view, and a truth-table editor.**

- `src/jls/elem/TruthTable.java` (1491 lines) — a first-class element whose behavior *is* a
  truth table: `inputNames`/`outputNames` vectors in column order, `int[][] table` with cells
  `0`/`1`/`2` where 2 is don't-care, `addInput`/`addOutput`/`removeInput`/`removeOutput`,
  `toggleOutput`, `makeDontCare`, `undoDontCare`, `removeRow`, `renameInput`, `moveOutputLeft`.
- `src/jls/edit/DisplayBool.java` (382 lines) — the rendered table: named input columns, a
  vertical rule, named output columns, per-cell layout with column widths measured from font
  metrics, don't-care rendered as `x`, per-signal popup for delete/rename/reorder.
- `src/jls/edit/TruthTableEditor.java`, `TruthTableDialog.java`, `TruthTablePrintable.java` —
  the dialog, the create/change dispatcher, and the print path.
- It is a palette entry (`Palette.java:185`, group COMPLEX), a `SaveTags` row, in
  `AllElementsRoundTripTest`, in the collab op vocabulary (`ElementVocabulary.java:46`), in
  `HdlExporter`'s *exported* set (so it already emits to Verilog and VHDL), and it has a shipped
  help tree at `resources/help/truth`.

ARCHITECTURE.md even names these files, filed under "non-elements that live here for historical
reasons: truth-table display internals (`DisplayBool`, `Cross`, `HLine`, `VLine`, `Entry`
subclasses)". The C31 task set reads as if that paragraph does not exist.

## The reframing: extraction produces a TruthTable element, not a panel

Cut the seam one layer lower. "Analyze selection → Truth table" should build a
`jls.elem.TruthTable` from the extracted cone — frontier inputs and outputs become the signal
names, `#641`'s enumeration fills `int[][] table` — and then open the *existing*
`TruthTableEditor` on it. The view is not built; it is reused.

Then the extracted table is not a dead-end read-only widget. It is a circuit object. The
student can drop it on the canvas beside the gates they selected and **simulate it** — which is
the actual pedagogical claim ("this cone of gates behaves as this table") made checkable by the
existing simulator rather than asserted by a panel. It saves into the `.jls` file. It prints.
It diffs in version control via `-savetext`. It exports to Verilog and VHDL through
`HdlExporter`, unchanged. A bespoke `JTable` gets none of that, and every one of those
properties would eventually be re-requested as its own issue.

### What the reframing does to the rest of CAP-31

| Task | As written | Under the reframing |
|---|---|---|
| #644 (this) | build a read-only view | wire extraction into `TruthTableEditor`; add refusal banner |
| #652 / FEAT-C31-3 "type and edit a table" | build table editing | **already shipped** — `addInput`, `toggleOutput`, `makeDontCare` are the feature; the task shrinks to enforcing #642's bound in `addInput` |
| #653 synthesis "table → drawn circuit" | new netlist builder | becomes "expand a `TruthTable` element into gates" — a well-posed element-level operation with the layouter lineage (`jls.hdl.layout.HeuristicLayeredLayouter`) already in tree |
| #655 round-trip assertion | bespoke fixture plumbing | `extract(expand(T)) == T` over a `TruthTable` element, with the golden stored as an ordinary `.jls` file |
| #650 expressions beside the table | new panel region | a second pane in the dialog that already has a `BorderLayout` and an error label |

That is one representation, four consumers, and it retires the exact defect the coordination
comment was written to prevent ("a second truth-table widget in the tree") — by observing that
the *first* widget is not any of the three, it is the one already shipped.

## What the reframing costs, honestly

None of this is free, and the costs are the interesting part:

1. **The element's semantics have two known defects, both cited in CAP-09's #306 body.**
   `TruthTable.react` at `:1447-1449` collapses a don't-care output to 0 ("`// don't care becomes
   false`"), and an unmatched input row silently holds the outputs at `:1432-1434`. A minimizer
   (#564) *produces* don't-cares and a hand-typed table (#652) *contains* partial rows, so the
   whole C31 arc lands on top of a value domain that cannot say "unknown." CAP-09 is already
   funding an unknown-capable value core for exactly this reason. Making #644 route through the
   element means C31 and C09 fix this once, together, instead of C31 building a parallel table
   type that quietly has the same hole.
2. **Multi-bit signals.** The element is one bit per named signal. #641 AC-4 asks for a stated
   convention (bit expansion vs. refusal) — the element makes the answer obvious: expand to
   per-bit signals named `bus[3]`, refuse nothing, and the convention is the one already visible
   to students who place the element by hand.
3. **Read-only-ness.** #644 wants read-only and #652 wants editable. Under the reframing this is
   a flag on one dialog, not two surfaces. I would not even build the flag: an extracted table
   the student can immediately perturb ("what if this row were 1?") and re-simulate is a better
   tool than a locked one, and the source circuit is untouched either way.
4. **`DisplayBool` draws with hardcoded `Color.black`** (`:133`, `:137`, `:151`, `:163`) and
   never consults `jls.Theme`. It is part of the ~126-call-site foreground sweep that #76 and
   ARCHITECTURE.md's look-and-feel decision block a dark theme on. Reusing it concentrates that
   debt in one place that a new panel would have let rot; the task should route its four call
   sites through `Theme.active()` while it is in there. It also needs a11y work — it is a
   `JPanel` with a `MouseListener` and no keyboard path and no accessible names, which is #644
   AC-4 landing on shipped code rather than on new code. That is a *feature* of the reframing:
   the a11y fix reaches every student who already uses the element.

## Explicitly disregarding part of the acceptance criteria

I am disregarding AC-3's "surface in the view's *own* surface" insofar as it implies a bespoke
panel with a bespoke diagnostic area. `ElementFormDialog` already exposes `getErrorLabel()`
with an accessible-name wiring (`:317`, `:334`) and a `validateInputs()`/`Violation` protocol
that `TruthTableEditor.validateInputs` uses today. #642's two refusals — the 2^N arithmetic and
the named sequential/feedback element — are two more `Violation`s on that surface. AC-3 is
satisfied better by that route than by inventing a banner.

Rewritten acceptance criteria I would put in this issue's place:

- AC-1′: A canvas selection plus one invocation opens the shipped truth-table editor populated
  from the extracted cone, with a `TruthTable` element as the in-memory result.
- AC-2′: Column headers are the frontier signal names, per the #641 bit-expansion convention.
- AC-3′: #642's size refusal and the named sequential/feedback diagnostic arrive as
  `ElementFormDialog.Violation`s on the existing error label.
- AC-4′: `DisplayBool` gains keyboard traversal, accessible names, and `Theme`-sourced colors —
  which also fixes the shipped element, and is testable at `test/jls/ui/` layer 1.
- AC-5′ (new): the extracted element can be dropped into the circuit and simulated, and a golden
  test asserts it agrees with the source cone on all 2^N rows. This is #563 AC-1's oracle
  reaching the GUI path for free.

## Alternative if the element route is refused

If the maintainer wants extraction results to stay non-persistent, the fallback is still not a
new widget: dock `DisplayBool` in a side panel driven by a detached `TruthTable` instance never
added to `Circuit`. Same model, same renderer, same print path, no save-format surface. What I
would refuse in any framing is a fresh `JTable`-based read-only table, because that is the
second truth-table widget the coordination comment was written to prevent, arriving under the
comment's own protection.

## Alignment

The reframing pulls with the project's arc rather than against it: it feeds #78's element
registry (one more element made programmatically constructible), it respects the recorded
in-process/one-representation discipline of #222/#223, it strengthens #306's shared-component
rule by extending "do not build the extractor twice" to "do not build the table twice," and it
turns CAP-31's demo slice from a new subsystem into the activation of a subsystem JLS has been
shipping since Poplawski wrote it.
