# TASK-0020 - Extract the mouse machine and replace the source-identity dispatcher

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0019

## Deliverable

The nine-state interaction machine becomes a class with no drawing dependency
in its transitions, and the last `event.getSource() ==` comparison in the
editor becomes an `Action`.

1. **The state enum moves out of `SimpleEditor`.** `enum State` sits on the
   outer class at `src/jls/edit/SimpleEditor.java:770-789` (nine constants:
   `idle, chosen, placing, moving, selecting, selected, option, startwire,
   drawire`), directly under the comment "can't be in EditWindow, but should
   be" (`:762-763`), while the live `private State currentState` field is on
   the inner `EditWindow` at `:1218`. Promote it to
   `src/jls/edit/InteractionState.java`, public, javadoc preserved.

2. **`src/jls/edit/MouseMachine.java`, new.** It owns `currentState` and
   exposes `Transition transition(InteractionState from, Gesture g, Context c)`
   returning a record `{InteractionState next, String message, MessageTone
   tone, boolean invalidatesIndex}`. `setState`
   (`SimpleEditor.java:3936-3976`) becomes: ask the machine, then apply — the
   `circuit.invalidateIndex()` call at `:3940` and the nine-arm switch that
   sets `message.setText(...)`/`message.setBackground(...)` at `:3942-3975`
   move to the *application* side, and the *decision* moves to the machine.
   The machine must reference no `java.awt.*` and no `javax.swing.*` type;
   `MessageTone` is a plain enum, not a `Color`.

3. **The eight mouse handlers route through it.** `mousePressed` (`:2572`),
   `mouseReleased` (`:3328`), `mouseDragged` (`:3503`), `mouseMoved` (`:3736`),
   `mouseClicked` (`:3869`), `mouseEntered` (`:3880`), `mouseExited` (`:3888`)
   and `mouseWheelMoved` (`:3909`) stop calling `setState(State.x)` directly
   and instead submit a gesture. Each handler's residual body is geometry and
   model mutation; that split is the reviewable outcome.

4. **The last source-identity dispatch goes.**
   `EditWindow.actionPerformed` (`:2526-2564`) contains exactly one
   `event.getSource() == matchJump` comparison (`:2541`), and its own javadoc
   says so: "the one popup item still wired through the `ActionListener`;
   every other editing operation now dispatches through its shared `Action`
   (#75)". Replace it with a `JumpMatchAction extends AbstractAction` in the
   established local form (`:479`, `:545`, `:1373`, `:1481-1568` are the
   existing `AbstractAction` sites), then delete `actionPerformed` and drop
   `ActionListener` from `EditWindow`'s `implements` clause (`:1121`) once it
   is empty.

5. **The `SimpleEditor.java` line count is recorded before and after** against
   the plan TASK-0019 writes. This extraction's own target goes in that plan.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-008 | The interaction machine is the largest untestable body in the editor and the one every canvas addition must reuse. FEAT-043's breadboard canvas and FEAT-014's per-view geometry both need a gesture machine that is not welded to one `JPanel`. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0019 | The only instrument that can show this extraction did not lose coverage is a `jls.edit` package floor, and there is none at HEAD (`pom.xml:408-411`). Moving several hundred lines between classes changes which classes JaCoCo attributes coverage to; without the floor in place first, "the suite is still green" is compatible with a real coverage loss and the acceptance criterion below is unmeasurable. |

## Acceptance test

`test/jls/edit/MouseMachineTest.java`, new (headless — the machine has no
display dependency, which is the point):

- `everyStateAndGesturePairHasADeclaredOutcome()` — a totality test over the
  full `InteractionState × Gesture` product in the TASK-0002 registry-lint
  style, asserting each pair either yields a `Transition` or is explicitly
  declared rejected. Adding a tenth state fails this test until the table is
  updated.
- `transitionsTouchNoAwtOrSwingType()` — an ArchUnit rule
  (`ClassFileImporter`, already used at
  `test/jls/ui/DialogCoverageRatchetTest.java:60-89`; ArchUnit is a declared
  test dependency, `pom.xml:111`) asserting `jls.edit.MouseMachine` and
  `jls.edit.InteractionState` depend on no `java.awt..` or `javax.swing..`
  class.
- `everyTransitionThatChangesGeometryDeclaresAnIndexInvalidation()` — asserts
  the `invalidatesIndex` flag is set for exactly the transitions that today
  reach `setState`, so the unconditional `circuit.invalidateIndex()` at
  `:3940` is preserved as a per-transition property rather than lost.

`test/jls/ui/PopupOperationBehaviorTest.java`, extended:
`createMatchingEndFiresThroughItsAction()` — asserts the JumpStart contextual
item produces the same model state when invoked as an `Action` as it does
through the listener at HEAD.

**Must stay green, unchanged, as the behavior pin** (#84 §4 H1): the existing
gesture suites `test/jls/edit/MoveGestureTest`, `DeleteGestureTest`,
`CtrlWGestureTest`, `DragCandidateBoundTest`, `WireSweepSymmetryTest`, and
`test/jls/ui/EditorGestureTest`, `EditActionMatrixTest`,
`MidPlacementPaletteFeedbackTest`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | closes — the two named bodies. **The dispatcher half is already 99% done at HEAD**: one `getSource() ==` site remains, not 305 lines |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on — the harness is the behavior pin; its residual is TASK-0021 |
| 75 | Keyboard operability and accessibility: focus follows the mouse, the menu bar has zero accelerators/mnemonics, and no element can be manipulated without a mouse | informs — #75's shared-`Action` layer is what already removed the dispatcher; this task finishes the one item it left behind |

Recorded decisions, closed, cite as such: **#37** (a brace error made every
popup action dead code — the failure mode the dispatcher pattern enabled),
**#39** (gesture-state reset on undo), **#126** (the gesture decision made
package-private for testing, the reason `State` is package-visible on the outer
class at `:761`), **#17** (the spatial index), **#103** (the event-local mouse
position `x`, `y` used by the `matchJump` body).

## Notes

- **The nine-arm switch at `:3942` has no `default` arm.** It is a switch
  *statement* over an enum, so today a tenth constant compiles and silently
  falls through. Make the extracted decision a switch **expression** over the
  promoted enum so a tenth constant is a compile error — that is the
  "exhaustive switch with no default" the plan is meant to convert into a
  build failure, and it is free at the moment the enum moves.
- **`setState` invalidates the spatial index unconditionally** (`:3936-3941`,
  with the comment "gesture transitions are where uncovered geometry changes
  … land; one lazy index rebuild per transition keeps the spatial index
  honest (#17)"). `proofs/SpatialIndexCorrectness.agda` and
  `test/jls/ProofBridgeTest` pin the query-parity and draw-culling theorems the
  drag path relies on, and `test/jls/SpatialIndexTest` and
  `DrawCullingParityTest` pin the Java side. A transition that stops
  invalidating breaks a *proved* property, which is why the flag is part of the
  `Transition` record rather than a caller's judgment.
- **`EditWindow` is a non-static inner class** (`:1121`) that closes over the
  outer `SimpleEditor` — that closure is exactly why `State` "can't be in
  EditWindow". The machine must take what it needs as parameters; capturing
  the editor would reproduce the coupling in a new file.
- **`getGraphics()` appears inside the `matchJump` body** (`:2552`:
  `ElementDialogs.setup(nel, this.getGraphics(), this, x, y)`). The `Action`
  replacement inherits that call, unchanged, in this task —
  `Graphics`-freeing op application is TASK-0037's job, not this one. Do not
  bundle them.
- **Do not also extract clipboard, import management or `EditWindow` itself.**
  Each is named in TASK-0019's plan with its own row. This task is two bodies;
  a bigger diff cannot be reviewed against the behavior pin.
- **`HeadlessCoreRatchetTest` does not cover `jls.edit`** — it guards the core
  packages. The new ArchUnit rule is what keeps AWT out of the machine, and it
  belongs in the same commit as the machine.

## Evidence

- `src/jls/edit/SimpleEditor.java:761-789` — the "can't be in EditWindow, but
  should be" comment and the nine-constant `State` enum, package-private for
  #126.
- `:1121` — `private class EditWindow extends JPanel implements
  ActionListener, MouseListener, MouseMotionListener, MouseWheelListener`.
- `:1218` — `private State currentState = State.idle;`.
- `:3936-3976` — `setState`: `circuit.invalidateIndex()` at `:3940`, then a
  nine-arm switch with no `default`, each arm setting `message.setText` and
  `message.setBackground`.
- Mouse handlers: `:2572` (`mousePressed`), `:3328` (`mouseReleased`),
  `:3503` (`mouseDragged`), `:3736` (`mouseMoved`), `:3869` (`mouseClicked`),
  `:3880` (`mouseEntered`), `:3888` (`mouseExited`), `:3909`
  (`mouseWheelMoved`).
- `:2526-2564` — `actionPerformed` and its javadoc naming `matchJump` as "the
  one popup item still wired through the `ActionListener`"; the single
  `event.getSource() == matchJump` at `:2541`.
- Existing `AbstractAction` sites, the form to copy: `:479`, `:545`, `:1373`,
  `:1481`, `:1499`, `:1515`, `:1529`, `:1542`, `:1557`, `:1568`; the shared
  `editAction(EditOp)` at `:605-608` and `:1594`.
- `pom.xml:111` — ArchUnit as a test dependency.
- `pom.xml:408-411` — `jls.edit` unfloored at HEAD, the reason for the
  blocked-by.
- Behavior pins at HEAD: `test/jls/edit/{MoveGestureTest, DeleteGestureTest,
  CtrlWGestureTest, DragCandidateBoundTest, WireSweepSymmetryTest}`,
  `test/jls/ui/{EditorGestureTest, EditActionMatrixTest,
  PopupOperationBehaviorTest, MidPlacementPaletteFeedbackTest}`.
- `proofs/SpatialIndexCorrectness.agda`, `test/jls/ProofBridgeTest.java`,
  `test/jls/SpatialIndexTest.java`, `test/jls/DrawCullingParityTest.java`.
