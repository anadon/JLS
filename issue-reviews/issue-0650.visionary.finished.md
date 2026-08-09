# Issue #650: TASK-C564-3: minimized expressions display beside their truth table and export as plain text
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "a text pane." The outcome line says it plainly — the thing a student does with a
minimized expression is *put it in a lab report*. #650 is the exit door of CAP-31's
analysis loop: the point where JLS's answer leaves JLS. CAP-31 (#515) names the
competitive evidence exactly here — "Issie generates truth tables with KaTeX
expressions" — so the goal is not that the expression is visible, it is that the
expression is **portable, typeset-ready, and identical everywhere it appears**. Judged
against that, the issue is right about the destination and under-ambitious about the
route.

## Reframing 1 (load-bearing): the expression is an emitter, not a formatter

JLS already has the exact pattern this task needs, one package over.
`src/jls/hdl/HdlEmitter.java` is a two-method interface — `String emit(HdlModel)`, with
the doc comment "The output must be deterministic: same model, same bytes", plus
`fileExtension()` — and `VerilogEmitter`/`VhdlEmitter` are two renderings of one
structured IR (`HdlModel`, a visitor-dispatched statement hierarchy). TASK-C564-1 (#648)
AC-4 *already* requires the minimized expression to be "a structured value, not a
formatted string." The seam is therefore sitting there unused.

Cut along it: minimization returns a value; one headless `SopEmitter` renders it to text;
the pane in #644's view displays that emitter's string; #650's file/clipboard export
writes that string; #651's batch flag prints that string. Consequences:

- **AC-3 stops being a test obligation and becomes structurally true.** "Byte-identical
  to what is displayed" cannot be violated when only one object in the tree can produce
  those bytes. I would **disregard AC-3 as written** and replace it with a constraint the
  repo already knows how to enforce: *no expression-formatting code exists in `jls.edit`*,
  pinned by a ratchet test in the lineage of `NotificationRatchetTest` and
  `HeadlessCoreRatchetTest`. A byte-identity assertion tests one path; a ratchet forbids
  the second path from existing.
- **AC-4 becomes free.** The refusal is a case of the same result value, so "the refusal
  appears in this surface with its arithmetic" is the same code as #649 AC-4's
  "identical whether reached from the GUI or from the batch flag." As filed, #650 AC-4
  restates #649 AC-4 and #650 AC-3 duplicates #651 AC-3 — two writers of the same text.
  One emitter collapses both.
- The emitter must be AWT-free in its signature, matching #872 AC-5 and the headless
  ratchet, so `jls.edit` depends on it and never the reverse.

## Reframing 2: notation is a plurality, not a decision

AC-2 asks for "a stated operator convention." One convention will be wrong for somebody's
course — JLS's users are classrooms, and the Poplawski lineage spans `A'`, `Ā`, `!A`,
`~A`. Once rendering is an emitter, extra notations cost almost nothing, and two of them
carry disproportionate value:

- **LaTeX** (`F = \overline{A}B + A\overline{B}`) is what actually lands in a lab report
  and is the literal answer to the Issie/KaTeX gap CAP-31 cites as the reason this
  capstone exists. Plain text is the floor of AC-3 (`FEAT-C31-2` AC-4 says "plain text at
  minimum"); #650 silently promoted the floor to the ceiling.
- **Verilog `assign`** is nearly already written — `VerilogEmitter.gate` joins operands
  with `" & "` / `" | "`. Emitting it buys free consistency with the shipped `-export`
  path *and* a stronger oracle than #651 AC-1 asks for: pipe the emitted assign to
  `iverilog` (CI already installs it, per README) and difference it against the circuit.

So I would restate AC-2 as: *the rendering is a named member of a documented notation set,
selectable, each pinned by a golden* — and put that document beside `docs/file-format.md`
and `docs/batch-interface.md`, which is where this project keeps normative contracts.

## Reframing 3: JLS already ships a truth table, and the whole family ignores it

This is the biggest missed seam in the arc, and it is visible only from above #650.
`src/jls/elem/TruthTable.java` (1491 lines) is a simulating `LogicElement` with named
inputs and outputs, don't-care cells, `SaveTags` persistence, a renderer
(`edit/TruthTableRenderer.java`), a **print path** (`edit/TruthTablePrintable.java`), a
full **editing dialog** (`edit/TruthTableEditor.java` + `TruthTableDialog.java`), a
collab-vocabulary entry, and an HDL export path (`HdlExporter.buildTruthTable` →
`HdlModel.PriorityCaseStatement` → `casez`/`std_match`). Not one issue in the CAP-31
family — #515, #563, #564, #565, #644, #650, #652, #872 — names it.

The #644 boundary comment names "a second truth-table widget in the tree" as *the defect
it exists to prevent*, then scopes that worry only to the three new tasks. The widget it
should be worried about is already in the tree.

The elegant cut: **extraction yields a `TruthTable`, and the expression pane goes into
`TruthTableEditor`.** Then #650 is one panel added to a dialog that already exists rather
than half of a view that does not; #652 ("a student types and edits a truth table") is
largely already shipped; #655's round-trip gains a first-class intermediate; the student
sees *one* truth table in JLS; and — the part I care about most — the expression pane
appears for hand-placed `TruthTable` elements too, which is the first case an intro
student hits, before they can draw a cone worth extracting.

The honest obstacle, which is exactly why this must be decided now and not after #644
lands: `TruthTable.react` is **first-match priority** semantics (`HdlExporter`'s comment
says "the FIRST row"; an output don't-care becomes 0), not a complete function table. It
is not a drop-in carrier. But if CAP-31 ships a second, complete-function table
representation without reconciling this, JLS will carry two truth tables with different
semantics, both persisted, both HDL-exported, both editable, and no way for a student to
tell them apart. Either resolution — converge them, or write down why they differ and
name each in the UI — is better than discovering it at #655.

## Smaller things the issue does not see

- **AC-3's clipboard is new ground.** `grep -rn "Clipboard\|StringSelection" src/` finds
  nothing: JLS has never touched the system clipboard. There is also no GUI export path
  at all — the editor's only `JFileChooser` is `Editor.java`'s save/save-as, and HDL
  export is CLI-only. So a task framed as "displays beside the table" quietly introduces
  two first-of-their-kind GUI capabilities. Name them: a `TellUser`-consistent story when
  the clipboard is unavailable, and coverage on the Wayland-native row of the README's
  supported matrix, which `scripts/wayland-rig.sh` does not exercise for clipboard today.
- **A new student-facing surface needs a help topic.** `resources/help/**` +
  `Map.jhm` + `JLSHelpTOC.xml` with `HelpTopicsTest` enforcing completeness is the
  project's standing contract; neither #644 nor #650 mentions it.

## Alignment

This pulls *with* the trajectory — batch-first, one representation, emitter lineage,
normative docs, ratchet-enforced layering — on every axis except the table-representation
duplication above. It duplicates nothing that is filed; it duplicates a good deal of what
is already shipped. Endorse the outcome, re-cut the seam.
