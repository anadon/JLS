# Issue #650: TASK-C564-3: minimized expressions display beside their truth table and export as plain text
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task is a plausible, narrowly-scoped UI slice — attach an expression pane to an
existing table view (#644) and let it export. It correctly cites its inputs
(TASK-C564-1's minimizer, TASK-C563-3's table view) and correctly labels its export
AC as "the export half" of FEAT-C31-2 AC-4, leaving the batch/headless half to the
sibling task (#651, confirmed to own it). But the four ACs have a real, checkable
contradiction with the task's own `ordering_after`, one AC pair with no shared
source-of-truth to make it non-gameable, and one AC that depends on a value the
cited prerequisite issue never actually states.

## Findings, most severe first

**1. HIGH — AC-4 requires consuming TASK-C564-2's output, but `ordering_after` never lists TASK-C564-2 as a dependency.**
Quoted machine block: `ordering_after: ["TASK-C564-1", "TASK-C563-3 (the table view this extends)"]`.
Quoted AC-4: *"TASK-C564-2's refusal appears in this surface with its arithmetic, rather than as a blank expression panel."* TASK-C564-2 is #649 — a separate, open, unimplemented task ("above the stated bound the minimizer refuses..."). AC-4 makes #649's refusal object a required input to this task's own acceptance test, yet #649 is absent from the ordering list that is supposed to capture exactly this kind of prerequisite. An executor who reads only the machine block (the same failure mode #563's own corrective comment warned about, reproduced here) will schedule #650 right after #648/#644 land and find AC-4 unimplementable — no refusal object exists yet to render. **Recommendation:** add `"TASK-C564-2 (AC-4's refusal source)"` to `ordering_after`.

**2. MEDIUM — AC-2 and AC-3 have no shared source of truth, making "byte-identical" gameable and "documented" unverifiable.**
AC-2: *"The rendered notation is documented and stable — a stated operator convention, not whatever the formatter happened to emit."* AC-3: *"...the exported text is byte-identical to what is displayed."* Neither AC requires the display pane and the export path to call the *same* formatting function, and neither names where the convention is documented (contrast with the project's own bar for this: `docs/batch-interface.md` is cited elsewhere in this repo's issues as the model for a stability-pinned, normatively documented text contract). As written, an implementation could hand-write two independent string builders — one for the Swing label, one for the export writer — that happen to agree on the golden test's fixture and drift on anything the golden doesn't cover (a different literal count, a different NOT placement), while still passing AC-3 as literally tested. Nothing in the AC forces "one formatter, two call sites." **Recommendation:** state the convention inline (e.g., `NOT` as `'`, `AND` as juxtaposition, `OR` as `+`, one line per output as `Name = term + term + ...`) or point at a doc page that will hold it, and require display and export to route through one shared formatting function so byte-identity is structural rather than incidentally true of one fixture.

**3. MEDIUM — AC-4's "arithmetic" has no defined value anywhere in the chain it cites.**
AC-4 promises TASK-C564-2's refusal "with its arithmetic" appears here. But #649 (TASK-C564-2) itself never states a bound value or formula — its boundary note claims *"The bound's numeric value is recorded here"* while no number or formula actually appears in that issue's body (re-verified directly against #649's fetched body). This is not a defect #650 can fix by itself, but it means AC-4 as currently written cannot be honestly tested today: there is no "arithmetic" yet for the expression pane to render, so a reviewer checking AC-4 has nothing concrete to check it against until #649 is repaired. **Recommendation:** either block #650's AC-4 explicitly on #649 stating a concrete bound (which finding 1 already requires ordering on), or note in #650 that AC-4 is provisional pending that number.

**4. MEDIUM — no accessibility acceptance criterion for the new pane, despite the task it extends carrying one.**
TASK-C563-3 (#644), the table view this task explicitly extends (per its own boundary note), has AC-4: *"The view is keyboard reachable and carries accessible names, per the standing a11y expectations."* The project maintains a standing, tested a11y checklist for editor UI (`docs/keyboard-a11y-verification.md`, issue #75, `EditorGestureSupport` focus-faithful driving, `@Tag("display")` Xvfb suite) that other editor-adjacent surfaces are held to. #650 adds a new displayed pane plus an export *action* (file/clipboard) to that same view and states no equivalent criterion — an implementation could ship a mouse-only, unlabeled expression panel and export button and satisfy every stated AC. **Recommendation:** add an AC requiring the expression pane and export control be keyboard-reachable and carry accessible names, consistent with #644 AC-4 and the #75 checklist.

**5. LOW — the clipboard half of AC-3 has no stated test path, and the project's own headless-by-default test posture makes it easy to leave untested.**
`ARCHITECTURE.md` states tests are "headless — CI has no display" by default (Layer 1); system clipboard access under `java.awt.headless=true` throws `HeadlessException`, so exercising the clipboard branch of AC-3 requires the Layer 2 Xvfb harness (`test/jls/ui/package-info.java`, `@Tag("display")`). The issue never mentions this, so "export... to a file or the clipboard" can be satisfied by shipping (and testing) only the file path while the clipboard path ships with no automated coverage at all — a silent gap the "byte-identical" AC would not catch if it's only asserted against the file writer. **Recommendation:** name the Layer-2/Xvfb test obligation for the clipboard branch explicitly, or scope AC-3 to file export only and file clipboard export as a follow-up if display-harness cost isn't wanted here.

**6. LOW — Outcome text narrows to "the circuit's own signal names," which doesn't hold for the hand-entered path this same feature is supposed to cover.**
Outcome: *"...alongside the truth table, one expression per output, using the circuit's own signal names."* But the parent FEAT-C31-2 and TASK-C564-1 both explicitly cover a table "extracted from a drawn circuit **or entered by hand**" — a hand-authored `jls.elem.TruthTable` has its own input/output names (`addInput`/`addOutput`), not names derived from "the circuit" in the sense the Outcome implies. This is a wording slip, not a functional gap (AC-1 itself just says "that output's name," which is fine for both paths), but it's worth tightening so an implementer doesn't read the Outcome as license to assume a backing circuit always exists. **Recommendation:** reword to "the table's own signal names" to match the two-path scope stated elsewhere in the chain.

## What's solid

- **The export-scope split from #651 is clean and correctly cited.** AC-3 explicitly marks itself "(FEAT-C31-2 AC-4, export half)," and #651 (TASK-C564-4, confirmed fetched) independently owns the headless/batch half — no overlap or double-booking between the two tasks.
- **The boundary note correctly defers table ownership to #644** ("The table view itself is #563's TASK-C563-3; this task adds the expression pane to it") — matches #644's own boundary note verbatim ("Expression display over the same table is #564's TASK-C564-3"), a rare case in this issue family of two boundary notes actually agreeing.
- **AC-1's scope (display, labeled by output name) is concrete and directly testable** against the existing `TruthTable`/`DisplayBool` model without inventing new abstractions.
- **Plain-text export as the deliverable fits the project's established preference for plain-text, diffable artifacts** (`-savetext`, VCD, batch output line contracts per `docs/batch-interface.md`) rather than a novel binary or rich-text format.

## Verdict rationale

`needs-rework`: the task's scope and boundaries are sound and the split from sibling tasks is unusually clean for this issue family, but AC-4 has a direct, checkable contradiction with its own `ordering_after` (finding 1), inherits an undefined value from #649 (finding 3), and AC-2/AC-3 together are satisfiable by an implementation that doesn't actually guarantee what they're meant to guarantee (finding 2). These are fixable by editing the issue text — none require re-scoping the feature.
