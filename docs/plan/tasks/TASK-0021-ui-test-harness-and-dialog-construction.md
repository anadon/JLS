# TASK-0021 - The UI test harness, including dialog construction

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

> **Scope correction, verified at HEAD.** The harness exists. `test/jls/ui/`
> holds 34 files including `CircuitAssert`, `GeometryAssert`, `RenderAssert`,
> `EditorGestureSupport`, `EdtViolationDetector` and `UiHarnessPilotTest`;
> 25 classes carry `@Tag("display")` and run in their own surefire execution
> (`pom.xml:274-295`) under Xvfb in CI (`.github/workflows/ci.yml:80-86`).
> `DialogConstructionSmokeTest` already sweeps 23 element dialogs plus
> `DelayChangeDialog`, and `DialogCoverageRatchetTest` already enforces by
> ArchUnit that every `ElementDialog` subclass is swept or exempt. What is
> missing is **validation** — the sweep constructs and cancels — and a mouse
> assertion surface. This task is that residual.

1. **Dialog validation, not just construction.**
   `DialogConstructionSmokeTest.constructAndCancel`
   (`test/jls/ui/DialogConstructionSmokeTest.java:117-140`) instantiates the
   element on the EDT, calls `ElementDialogs.setup(el, graphics, panel, 100,
   100)`, and lets a watcher cancel the dialog; the assertion is that `setup`
   *returned*. The constraint strings each element publishes —
   `Memory.CAPACITY_CONSTRAINT`/`BITS_CONSTRAINT`
   (`src/jls/elem/Memory.java:56-60`), `Clock.CYCLE_CONSTRAINT`/
   `ONE_CONSTRAINT` — are pinned only *model-side* by
   `test/jls/elem/DialogValidationTest`, which calls `checkCapacity(-5)`
   directly and asserts the loader reports it. Nothing asserts that typing an
   out-of-range value into the actual dialog surfaces that string and refuses
   the commit. Add a per-dialog validation case that does.

2. **A commit path, not only a cancel path.** The sweep never commits, so no
   dialog's OK handler is exercised. Each validation case ends by entering a
   *valid* value and committing, then asserting the element's model state
   changed accordingly — which is also the only way the OK handlers enter the
   `jls.edit` coverage the TASK-0019 floor measures.

3. **`GestureAssert`, the fifth assertion class.** `CircuitAssert` (presence,
   bits, connectivity, watched), `GeometryAssert` (position, dimensions,
   grid, relative placement) and `RenderAssert` exist; `EditorGestureSupport`
   supplies the *drivers* (`leftDrag`, `leftClick`, `rightPress`, `moveTo`,
   `middleDrag`, `ctrlWheel`, `clickPopupItem`, `pressKey`, `focusCanvas`,
   `zoomIn/Out/Reset`). Missing is the assertion vocabulary over a gesture's
   *outcome*: which elements ended selected, what the interaction state is,
   whether a drag was rejected and why. That vocabulary is what makes
   TASK-0020's extraction reviewable through the harness rather than only
   through unit tests of the machine.

4. **The suite must be shown to have run.** Every display test self-skips via
   `assumeFalse(GraphicsEnvironment.isHeadless())`; a validation case that
   silently skips is worse than no case. Add an executed-count assertion so a
   lane with a display armed fails if the display suite contributed zero
   tests.

5. **A short harness section in `docs/`** naming the five assertion classes,
   the driver class, and the rule that a new UI test uses them rather than
   reaching for `Robot` directly.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-008 | The harness is the behavior pin for every extraction in the feature; without dialog *validation* coverage, an extraction can move a validation call site and stay green. |
| FEAT-053 | The autograding front end is a GUI over the batch test-vector engine (#214). Its panel needs exactly this construct-drive-commit-assert vocabulary, and building it twice is the failure this task prevents. |

## Prerequisite tasks

None. Everything this task extends exists at HEAD.

## Acceptance test

`test/jls/ui/DialogValidationSmokeTest.java`, new, `@Tag("display")`:

- `everySweptDialogRejectsAnOutOfRangeValue()` — one case per dialog family
  in the `DialogConstructionSmokeTest` shape: open the dialog, set the field
  to a value the element's own `check*` helper rejects, attempt commit, assert
  the dialog reports the element's published constraint string **verbatim**
  (so the "one string, two surfaces" contract at
  `src/jls/elem/Memory.java:56-60` is checked on the surface it was written
  for) and that the element's model state is unchanged.
- `everySweptDialogCommitsAValidValue()` — the same set, with a valid value,
  asserting the model state changed.

`test/jls/ui/DialogCoverageRatchetTest.java`, extended:

- `everySweptDialogHasAValidationCase()` — the second ratchet, in the same
  ArchUnit `ClassFileImporter` form as `everyElementDialogIsSweptOrExempt()`
  (`:60-89`): every dialog in the swept set has a matching case in the
  validation suite or is listed as exempt with a reason. **Fails at HEAD** for
  all 23.

`test/jls/ui/GestureAssertTest.java`, new: an `EveryAssertionCanFail` nested
class, matching the assert-the-assertion discipline
`UiHarnessPilotTest` already applies (`test/jls/ui/UiHarnessPilotTest.java:36-40`),
pinning each new helper to fail on a violated expectation.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | closes — the mouse-interaction assertion surface is its last unbuilt clause; presence, geometry, relations, actions and menus are delivered (`CircuitAssert`, `GeometryAssert`, `EditActionMatrixTest`, `MenuBarSpecTest`, `MenuAcceleratorFiringTest`) |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | closes — the substrate, the 23-dialog sweep and `InteractiveSimulatorSmokeTest` shipped; this adds the validation half the title's word "coverage" implies |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | depends on — #84 §6 names the smoke test as the prerequisite safety net; TASK-0020 consumes what this task adds |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine (Digital-parity, HDL-independent) | overlaps — TASK-0111 builds the panel; it will be tested with this vocabulary |

## Notes

- **`rerunFailingTestsCount` is 2 on the display execution**
  (`pom.xml:288-294`), added because Xvfb popup realization is
  nondeterministic without a window manager. A new validation case that is
  genuinely flaky will be retried into green. Land the new cases, then read
  the surefire reports for reruns before declaring them stable.
- **Everything must construct on the EDT.**
  `DialogConstructionSmokeTest:106-116` records why: some element constructors
  build Swing state (`Constant`'s quick-change menu) and
  `EdtViolationDetector` "rightly flags" construction anywhere else. The
  detector's `assertClean` already runs in the sweep's `@AfterAll`
  (`:92-105`); new cases inherit that and must not leak windows.
- **The modal-dialog timeout is the failure mode to preserve.** The sweep's
  latch (`:135-137`) reports "dialog did not come back down - stuck modal
  window" rather than hanging the build. Validation cases add a *second*
  interaction before the cancel, so they need the same bounded wait around
  each step, not one wait around the whole case.
- **Families share dialogs deliberately** (`:142-147`: "AndGate stands in for
  the Gate dialog used by all five two-input gate kinds and NotGate"). The
  validation ratchet must key on the *dialog* class, exactly as
  `DialogCoverageRatchetTest` already does, or it will demand 35 cases for 23
  dialogs.
- **Two legacy dialogs are inner classes of their element**
  (`test/jls/ui/DialogCoverageRatchetTest.java:70`). They are already handled
  by the exemption mechanism; do not re-derive it.
- **Do not add a new UI test framework.** ArchUnit and JUnit are the declared
  test dependencies (`pom.xml:99-125`); the harness is deliberately built on
  `Robot` plus plain assertions, and the single-offline-jar governance is the
  reason.

## Evidence

- `test/jls/ui/` — 34 files at HEAD, including `CircuitAssert.java`,
  `GeometryAssert.java`, `RenderAssert.java`, `EditorGestureSupport.java`,
  `EdtViolationDetector.java`, `UiHarnessPilotTest.java`,
  `DialogConstructionSmokeTest.java`, `DialogCoverageRatchetTest.java`,
  `EditActionMatrixTest.java`, `MenuBarSpecTest.java`,
  `MenuAcceleratorFiringTest.java`, `InteractiveSimulatorSmokeTest.java`,
  `PopupOperationBehaviorTest.java`.
- `test/jls/ui/DialogConstructionSmokeTest.java:55` (class), `:65-90` (the
  display requirement and the cancel watcher), `:92-105`
  (`EdtViolationDetector.assertClean`, "dialog sweep (issue #162 P2)"),
  `:117-140` (`constructAndCancel` — construct, `setup`, cancel, assert
  returned), `:142-292` (23 dialog cases plus `elementDelayChangeDialog`).
- `test/jls/ui/DialogCoverageRatchetTest.java:26-93` — the ArchUnit ratchet,
  the `SWEPT` set, the exempt set and the legacy-inner-class carve-out.
- `test/jls/ui/UiHarnessPilotTest.java:32-40` — the #91 P1 criterion and the
  `EveryAssertionCanFail` discipline; `:3-14` — the assertion vocabulary in
  use.
- `test/jls/ui/EditorGestureSupport.java:166-551` — the gesture drivers.
- `test/jls/elem/DialogValidationTest.java:27-90` — the model-side
  "one string on two surfaces" assertions for `Memory` and `Clock`.
- `src/jls/elem/Memory.java:56-60` (the two constraint strings), `:62-75`
  (`checkCapacity` and its "one constraint string, two surfaces (issue #52)"
  comment).
- `src/jls/edit/ElementDialogs.java:22, 101` — the single `setup` entry point
  the sweep drives.
- `pom.xml:274-295` — the `display-tests` surefire execution, its
  `jls.test.headless` property and its retry count.
- `.github/workflows/ci.yml:65-86` — how the display suite is armed in CI.
