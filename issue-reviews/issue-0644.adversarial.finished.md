# Issue #644: TASK-C563-3: a student selects a region and reads its truth table in a view, without leaving the editor
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Selecting a combinational region in the editor and invoking "truth-table
extraction" opens a read-only view with labeled input/output columns and
every enumerated row; refusals from a sibling task surface inline; the view
is keyboard-reachable and named for a11y. Parent: #563 (FEAT-C31-1).
Siblings: #641 (TASK-C563-1, enumeration), #642 (TASK-C563-2, size/feedback
refusal), #650/#652 (the two other consumers of the same table
representation, per the issue's own coordination comment).

## Findings, most severe first

**1. The dependency chain this task sits on top of is not built, and the
issue doesn't say so.** #644's own body only lists `ordering_after:
["TASK-C563-1", "TASK-C563-2"]` (#641, #642), but #641 itself declares
`ordering_after: [306]` and states the table it hands to this view "is what
the minimizer (#564) and the round-trip check (#565) consume," produced
from "a combinational cone... all supplied by CAP-09's (#306) shared
subgraph extractor." I grepped the tree for any such extractor
(`combinational.*cone`, `SubgraphExtract`, `extractor`, case-insensitive)
and found nothing in `src/` — only issue-review markdown files reference
the word. #306 is itself a *capstone* that `requires_features: [317, 322,
335, 347, 353, 354, 359, 369]` — eight more unfiled-in-code features. So
#644, presented as a self-contained "task," is in practice gated behind a
capstone-sized formal-verification effort that has not landed a single
line of extractor code yet. Nothing in #644's body discloses this; a reader
scoping #644 alone would reasonably assume "select a region" is a small
selection-to-signals mapping already available in `SimpleEditor`/
`SpatialIndex`, when it is actually blocked on infrastructure several
issues removed. **Recommendation:** #644 should state its real
critical-path dependency (transitively, #306's extractor) or be explicitly
marked blocked/not-ready rather than `tier:task` implying near-term,
bounded work.

**2. AC-1–AC-4 never pin down the Outcome's central promise: "without
leaving the editor."** The Outcome section's whole justification for this
task existing — "the analysis loop is worthless if reading the answer
requires a batch invocation" — is that the table appears *alongside* the
canvas, not that it blocks it. None of the four ACs test that. AC-1 only
requires "a canvas selection plus one menu or keyboard invocation opens the
table view populated for that selection" — a modal `JDialog` that freezes
the editor underneath (much like the existing `TruthTableEditor`, which
*is* modal-style via `ElementFormDialog`) would satisfy AC-1 through AC-4
literally while defeating the stated purpose. **Recommendation:** add an
AC that the editor canvas remains interactive (or at minimum visible and
re-focusable) while the table view is open — otherwise "without leaving
the editor" is marketing copy with no acceptance test behind it.

**3. AC-2's "signal names from the circuit" assumes every relevant put is
named, and JLS circuits routinely have unnamed ones.** `Attribute`-driven
elements in `jls.elem` don't require the user to name every input/output
before simulating or extracting; gates and wires commonly go unnamed in
quick student sketches. The AC gives no fallback convention (e.g.
auto-generated `IN1`/`OUT1` vs. wire/put internal id vs. refusal until
named). A literal-minded implementation could label named signals
correctly and leave anonymous columns blank or numeric, technically
"labeled with the signal names from the circuit" (there aren't any) while
failing the AC's own stated rationale — "so a row can be read back against
the drawing without counting positions." **Recommendation:** state the
naming convention for unnamed puts explicitly (this is exactly the kind of
decision #641's AC-2 language — "deterministic and documented" — models;
#644 should borrow the same discipline for column labels).

**4. Selection validity is split across two issues with no shared
contract for the boundary case.** #642 (TASK-C563-2) owns "refuses with
the row-count arithmetic" and "rejected with a diagnostic naming the
specific element," and #644's AC-3 says these "surface in the view's own
surface with the diagnostic text, not as a silent empty table or a stack
trace" — implying the table view *always* opens, even for a refused
selection, and the diagnostic is rendered inside it. But #642's own AC-1
wording is "above it extraction refuses" — refuses, not "opens a view that
then displays a refusal." Whether the refusal is: (a) an inline banner
inside the same view frame #644 builds, or (b) a `TellUser` dialog that
pre-empts the view from opening at all (the pattern `ARCHITECTURE.md`
documents as the *only* sanctioned path for user-visible messages, via
`jls.TellUser`), is left for each task's implementer to guess
independently, with #642 upstream of #644 in ordering. If #642 lands first
using `TellUser` (the documented, ratchet-tested contract —
`NotificationRatchetTest` forbids raw `JOptionPane`/ad hoc dialogs), #644
inherits a refusal path that never reaches "the view's own surface" as
AC-3 demands, and one of the two tasks will need rework. **Recommendation:**
pin the refusal delivery mechanism (view-internal banner vs. pre-empting
dialog) in #642 or #644 now, not after both land independently.

**5. The coordination comment on this issue enumerates three table-surface
owners (#644/#650/#652) and misses a fourth, pre-existing one.** The
issue's own comment says "A second truth-table widget in the tree would be
the defect this note exists to prevent," and lists #644 vs #650 vs #652 —
but the repository already ships `jls.elem.TruthTable` with
`src/jls/edit/TruthTableDialog.java`, `TruthTableEditor.java`,
`TruthTableRenderer.java`, `TruthTablePrintable.java`, and
`src/jls/edit/DisplayBool.java` (a Swing component that already renders
input columns, output columns, and every enumerated row for a
student-authored truth table — precisely the widget shape #644 asks for,
just fed by manual entry instead of circuit extraction). #644 does not
mention this existing machinery, does not say whether the new read-only
extraction view should be a `DisplayBool` reuse (read-only mode) or a
parallel component, and the coordination comment that exists specifically
to prevent duplicate widgets never accounts for this pre-existing one.
Given #565 (table *editing* for synthesis) is explicitly out of scope here
and *does* presumably reuse this element, there's a real risk #644 and
#565's implementers converge on the same rendering class from opposite
directions without anyone having said so. **Recommendation:** state
explicitly whether the extraction view is a read-only `DisplayBool`/
`TruthTableRenderer` instantiation over a synthetic (non-persisted)
`TruthTable`-shaped model, or a new component — this is exactly the kind
of decision the issue's own boundary-notes section exists to make and
currently doesn't.

**6. AC-4's a11y bar is asserted, not specified, though the project does
have a real precedent to point to.** "The view is keyboard reachable and
carries accessible names, per the standing a11y expectations" cites no
issue or doc. `docs/keyboard-a11y-verification.md` (issue #75) is the
actual standing checklist and gives a concrete test pattern
(`EditorGestureSupport.focusOwner()`/`pressKeyThroughFocusOwner`,
focus-faithful driving under Xvfb) — #644 should point at it directly so
the acceptance bar is "extend the #75 checklist with these new rows," not
a vague appeal that an implementer or reviewer has to go find on their
own. This is a specification gap, not a blocking one. Severity: low.

## What's solid

- The boundary notes cleanly separate this task from #564's expression
  view and #565's editable table, each with an issue citation — good scope
  hygiene, one line each, no argument.
- AC-3's requirement that size-refusal and feedback-refusal use distinctly
  worded diagnostics (not collapsed into one generic error) matches the
  project's existing `LoadError` taxonomy discipline described in
  `ARCHITECTURE.md` ("Error-reporting contracts") — consistent with house
  style.
- Ordering (`ordering_after: TASK-C563-1, TASK-C563-2`) is internally
  consistent with what #641 and #642 actually produce (a table value and a
  refusal path) that this task's view would consume.

## Verdict

**needs-rework.** The task is well-bounded relative to its siblings but
rests on an undisclosed multi-issue dependency chain (#306's unbuilt
extractor), leaves its headline "without leaving the editor" promise
untested by any AC, underspecifies the unnamed-signal and refusal-delivery
conventions in ways a compliant-but-useless implementation could exploit,
and never reconciles with the pre-existing `TruthTable`/`DisplayBool`
rendering code the issue's own anti-duplication comment should have
caught. None of this is fatal to the concept — it needs the boundary notes
tightened before implementation starts, not a redesign.
