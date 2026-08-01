# TASK-0092 - The breadboard canvas

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0021, TASK-0036

## Deliverable

A second canvas - breadboard placement - with its own geometry, its own ops,
its own undo entries and its own palette, added without spending the editor's
whole coverage headroom.

Precisely what changes:

- `jls/bread/BreadboardModel.java`: the placement model. A board is a grid of
  tie points; a placement binds one packed part (refdes from TASK-0086) to an
  origin tie point and an orientation. Tie-point columns and the two power
  rails are declared data, not literals in a renderer.
- `jls/bread/BreadboardGeometry.java`: tie-point pitch, column/row addressing
  and the strip-to-tie-point incidence, headless and tested. It must not
  import `java.awt` (`ArchitectureRulesTest`'s headless rule,
  `test/jls/ArchitectureRulesTest.java:124-132`).
- Two new `CircuitOp` records - `PlacePart` and `MovePlacement` - added to the
  sealed permits list at `src/jls/collab/op/CircuitOp.java:34-37`, each
  addressing parts by stable id (the existing rule at `:30-32`), each with an
  exact inverse, each carrying the **view discriminator** TASK-0036 introduces.
- `jls/edit/BreadboardRenderer.java` + `jls/edit/BreadboardCanvas.java`: the
  drawing surface, wired through the same `Viewport`
  (`src/jls/edit/Viewport.java`) and `UndoManager`
  (`src/jls/edit/UndoManager.java`) the schematic canvas uses, so undo is one
  stack over both views rather than two.
- `jls/edit/Palette.java`: the palette table gains a **view** dimension so the
  breadboard palette is a separate list, default-hidden. This is the K9
  mechanism: the first-year drawing an adder must not see it.
- An on-screen and in-report honesty statement, in the generated-HDL-header
  idiom, stating exactly what the canvas checks (topology: wrong pin, wrong
  section, wrong tie-point column) and what it cannot model (floating inputs,
  contention, fan-out, decoupling) until FEAT-026/FEAT-027 land.

Done means: a part can be placed, moved, rotated and deleted on the breadboard
canvas; every one of those is an op with an exact inverse; the placements
survive save/load through TASK-0036's per-view geometry section; and the
schematic canvas's behavior and byte output are unchanged.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-043 | The canvas half of "the breadboard canvas and its physical-simulation binding". Without it FEAT-043 is a packing report with no placement. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0036 | Placements are per-view geometry. This writes into the versioned per-view geometry section and emits ops carrying the view discriminator - both of which only TASK-0036 creates. Without it the placements have nowhere in the file to live and a reader that does not know the view would drop them. |
| TASK-0021 | A second canvas cannot be asserted by a human. This task's acceptance test asserts element presence, geometry and mouse interaction on a canvas, which is exactly the harness TASK-0021 builds. Adding 1,000+ lines of untested `jls.edit` code is what the coverage budget forbids. |

## Acceptance test

`test/jls/bread/BreadboardCanvasTest` (new class), over the TASK-0021 harness:

- `placingAPartBindsItToTheTiePointUnderThePointer()` - drives a synthetic
  press/drag/release and asserts the resulting placement's origin tie point,
  by column and row, not by pixel.
- `everyBreadboardOpHasAnExactInverse()` - a parameterized test over
  `PlacePart` and `MovePlacement`: apply, invert, and assert the model is
  structurally identical to the pre-apply model. Extends the existing op
  round-trip discipline rather than inventing one.
- `placementsSurviveSaveAndLoadByteIdentically()` - save, reload, re-save, and
  assert byte equality of the whole file.
- `theBreadboardPaletteIsAbsentFromTheDefaultView()` in
  `test/jls/edit/PaletteContractTest` (extend the existing class) - asserts the
  default view's palette entry list is unchanged from HEAD's, so K9 is a test
  and not an intention.
- `breadboardGeometryHasNoAwtImports()` - extend
  `test/jls/HeadlessCoreRatchetTest` to cover `jls.bread`.

## Related GitHub issues

**no issue.** The registry records the whole physical program (FEAT-040 through
FEAT-044) as untracked, and `search_issues` for `breadboard` returns nothing.
Preconditions that do have issues:

| # | title | relationship |
|---:|---|---|
| #84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher… | depends on - a second canvas grafted onto the undecomposed editor makes #84 strictly worse. Note the issue's line count is **stale**: `src/jls/edit/SimpleEditor.java` is 5,852 lines at HEAD. |
| #91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on - this task's acceptance test is written against that harness. |
| #162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on - supplies the substrate the canvas test runs on. |
| #167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | overlaps - shipped; this task extends its sealed vocabulary by two records. |

## Notes

- **The sealed-permits trap, and it is the good kind.** `CircuitOp` is a sealed
  interface (`src/jls/collab/op/CircuitOp.java:34-37`, eleven permits at HEAD).
  Adding `PlacePart` and `MovePlacement` stops the build at every exhaustive
  consumer - the reader, the security allowlist, the merge-rule table - until
  each handles them. Do not add a `default` arm anywhere to quiet it; the whole
  point of #170's closed vocabulary is that the compiler enumerates the work.
- **`CircuitOp.apply` takes a `Graphics`** at HEAD (`:52`) because rotate and
  flip re-derive size from font metrics. TASK-0037 replaces that with a text-
  metrics abstraction. Breadboard ops need no font metrics at all - write them
  to accept the abstraction, not a `Graphics`, so they do not have to be
  rewritten when TASK-0037 lands.
- **The unresolved implementation question that can move this estimate by
  weeks:** a breadboard strip is a net with **no wire in it**, and `WireNet`
  is a pair of `LinkedHashSet`s (`src/jls/elem/WireNet.java:22,24`) whose
  insertion order is load-bearing for deterministic multi-driver resolution
  (pinned by `SimulationSemanticsRegressionTest.multiDriverConflictResolves
  DeterministicallyAndWarnsOnce`, `test/jls/SimulationSemanticsRegressionTest.java:321`).
  Either fabricate synthetic `Wire` objects for strips - safe, ugly - or change
  a class whose ordering is pinned. **Decide this before writing the model, in
  writing.** The safe option is the default recommendation.
- **The honest-limits statement is not optional.** JLS reads a floating input
  as zero (`docs/simulation-semantics.md:60-67`) while a real 74-series input
  floats indeterminate and must be tied, and there is no X state at all
  (`:47-49`). A breadboard view that showed an unwired NAND input as solid LOW
  would teach the opposite of the truth on the very first bug. Until FEAT-026
  lands, the canvas checks topology and says so.
- **The cheaper answer must stay on the table.** The packing report
  (TASK-0086) delivers the whole pedagogical payload - "a logic gate is not a
  chip" - with no canvas, no per-view geometry and no `jls.edit` code. This
  task is for the constituency with no lab access or parts budget, who are in
  scope under D9. Ship the report first regardless.

## Evidence

- Editor state at HEAD: `src/jls/edit/SimpleEditor.java` = 5,852 lines (`wc -l`,
  this session), against #84's recorded 4,119 - the decomposition debt has
  grown, not shrunk. `src/jls/edit/EditOp.java:32` is the single-`Action`
  vocabulary a second canvas must reuse rather than fork.
- Op layer at HEAD: `src/jls/collab/op/CircuitOp.java:30-37,52`,
  `src/jls/collab/op/Ops.java:34-42` (stable-id resolution).
- Palette contract at HEAD: `src/jls/edit/Palette.java:27-60` (the `Group`
  enum, one flat global list), pinned by `test/jls/edit/PaletteContractTest.java`.
- Model geometry constants: `src/jls/core/Geometry.java:17-18`.
- The canvas cost, the per-view-geometry tax, and the "report half is the
  valuable half" split: `08-views-determination.md` §1.6 (first-view tax 7-11
  wk, canvas 6-10 wk, consistency check 2-3 wk).
