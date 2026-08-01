# TASK-0029 - Keyboard operability

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

**Correction to the registry scope, recorded rather than hidden: menu
accelerators, mnemonics, the focus policy and keyboard-only construction all
shipped at HEAD.** `docs/keyboard-a11y-verification.md` is the standing
checklist, the focus-follows-mouse grab in `mouseEntered` is gone
(`src/jls/edit/SimpleEditor.java:1294`, `:2576-2577` record the removal), and a
focus-faithful driver dispatches keys to the live `KeyboardFocusManager` focus
owner rather than force-feeding the canvas. Issue #75 is nevertheless open, and
its residual is the part it always scoped as future work plus four deliberate
coverage gaps its verification document names. This task closes them.

1. **The accessible canvas scene model.** #75 §12 calls it "the real
   assistive-tech unlock" and §10 scopes it out of the original increment. The
   editor canvas is a single custom-painted `JPanel`, so a screen reader sees
   one opaque object where a circuit is. Deliverable: an `AccessibleContext`
   over the canvas exposing each element as an accessible child with a name
   (type plus instance name), a role, a location, and its selection state; and
   accessible relations for wires (source element, destination element). Keyed
   by stable id (`docs/file-format.md` §8), so the tree survives an undo, a
   reload and a replica.
2. **Modal accelerator inertness, audited rather than asserted.** #75 §10's own
   threat: `WHEN_IN_FOCUSED_WINDOW` bindings can fire while a dialog is up.
   `docs/keyboard-a11y-verification.md` records that this was **not** tested
   because raising a modal `TellUser` blocks the EDT and the assertion is
   timing-fragile. Deliverable: an audit of every window-scoped binding against
   every modal dialog site, and a test that runs the dialog on a worker while
   the EDT drives the accelerator - the standard way out of that fragility.
3. **The Alt-navigation behavioral leg.** Mnemonics are pinned as a *property*
   (`MenuMnemonicAndAccessibleNameTest`) but no test drives the look-and-feel's
   Alt-navigation state machine to actually pop a menu open. Deliverable: one
   `Robot`-driven leg under the existing display substrate, tagged and rerun-
   tolerant, asserting a menu opens - or a recorded decision that property-level
   verification is the bound, with the reason.
4. **The macOS keymap exercised on macOS.** Cmd, Backspace-delete and
   Shift+Cmd+Z / Cmd+Y are policy-pinned in `MenuAcceleratorPolicyTest` and
   cannot be exercised behaviorally on the Linux substrate. `ci.yml` already has
   a `macos-gui` job. Deliverable: the faithful keyboard suite armed there.
5. **The three modal popup ops fired behaviorally.** Probe, Modify and Timing
   are identity-pinned only (`EditActionMatrixTest`) because they open modal
   dialogs. Same mechanism as item 2 unblocks them.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-011 | The operability half. It is the half with a real user on the other end: without an accessible scene model a screen-reader user can operate the menus and reach nothing they act on |

## Prerequisite tasks

None. The focus-faithful driver, the shared-`Action` matrix and the display
substrate all exist at HEAD.

## Acceptance test

`test/jls/ui/CanvasAccessibleTreeTest.java`, new, `@Tag("display")`:

- `everyElementAppearsAsAnAccessibleChild()` - builds a small circuit through
  the op layer, then asserts `canvas.getAccessibleContext().getAccessibleChildrenCount()`
  equals the element count and that each child's accessible name is non-blank
  and distinct. **Fails at HEAD**, where the count is zero.
- `theAccessibleTreeSurvivesUndo()` - applies an op, asserts the tree changed,
  undoes it, asserts the tree returns to its prior names **and its prior stable
  ids**. This is the assertion that makes the tree a view of the model rather
  than a snapshot rebuilt by hand.
- `wiresExposeSourceAndDestinationRelations()` - asserts an `AccessibleRelation`
  links a wire's two endpoint elements, so "what is this connected to" is
  answerable without sight.

`test/jls/ui/ModalAcceleratorInertnessTest.java`, new, `@Tag("display")`:

- `windowAcceleratorsDoNotFireWhileAModalDialogIsUp()` - raises a modal dialog
  on a worker thread, waits for it to be showing, fires Ctrl+A and Delete
  through the focus owner from the EDT, dismisses the dialog, and asserts the
  circuit is unchanged. Names the exact bindings audited in item 2.

`test/jls/ui/PopupModalOperationBehaviorTest.java`, new, `@Tag("display")` -
extends `PopupOperationBehaviorTest`'s pattern to Probe, Modify and Timing:
each op's persisted `save` block changes after the dialog is driven and
accepted.

`.github/workflows/ci.yml`, `macos-gui` job: the faithful keyboard classes
added to its suite, so the mac keymap claim is exercised where it applies.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 75 | Keyboard operability and accessibility: focus follows the mouse, the menu bar has zero accelerators/mnemonics, and no element can be manipulated without a mouse | closes - this is its §10/§12 residual and the four gaps its verification document names |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on - the harness this task extends; "relations" in its title is exactly the accessible-relation work of item 1 |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on - the Xvfb substrate every new test here runs under |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher | overlaps - the canvas accessible model is new surface on `SimpleEditor`, which is **5,852 lines at HEAD**, not the 4,119 the issue title records. Land item 1 behind the decomposition's seam or it becomes another thing to move |

## Notes

- **"A green test proved nothing about a real keystroke" is the governing
  lesson here.** The original H2 focus bug passed because the harness used
  `canvas.dispatchEvent(keyEvent)` on a hardcoded reference. Every test this
  task adds must go through `EditorGestureSupport.pressKeyThroughFocusOwner`,
  and every new claim must be falsified by a demonstrated red-on-break mutation
  recorded in `docs/keyboard-a11y-verification.md`, the way the existing five
  are.
- **The accessible tree must be keyed by stable id, not by index.** Index-keyed
  children would silently renumber under the canonical writer's sort
  (`docs/file-format.md` §8), which is exactly the class of silent breakage the
  stable-id work exists to end.
- **Do not build a second element index.** `src/jls/SpatialIndex.java` already
  maintains one for hover; an accessible tree that walks
  `circuit.getElements()` on every query reintroduces the O(n) scan
  `SpatialIndexTest` measured away.
- **`@Tag("display")` tests carry `rerunFailingTestsCount=2`** for transient
  popup-timing flakes, and the authoritative green bar is
  `xvfb-run -a mvn -B verify -Djls.test.headless=false`. A new test that only
  passes under the default headless run has not been run.
- **The modal-dialog mechanism is the crux of items 2 and 5 and it is one
  mechanism.** Solve it once - dialog on a worker, assertions on the EDT, a
  bounded wait on `isShowing` - and both items follow.
- **Coverage floors apply.** New code lands under the 93.0/92.0/84.5 JaCoCo
  package aggregate with no per-class exemption plus the 80/82 PIT thresholds.
  An accessible tree is easy to write and easy to leave untested.

## Evidence

- `docs/keyboard-a11y-verification.md:61-97` - the behavior/signal/test matrix
  for H1, H2 and the delivered accessibility items; `:108-120` - the five
  re-runnable red-on-break mutations; `:121-140` - the deferred list this task
  consumes, verbatim on the canvas scene model, Alt-navigation, modal
  inertness, the mac keymap and the three popup ops.
- `src/jls/edit/SimpleEditor.java:1294` and `:2576-2577` - the recorded removal
  of the `mouseEntered` focus grab; `:2412-2413` - the palette accessible-name
  fix; file length 5,852 lines at HEAD.
- Tests present at HEAD: `test/jls/MenuAcceleratorPolicyTest.java`,
  `test/jls/KeyPadAccessibilityPinTest.java`,
  `test/jls/HotkeysHelpAccuracyTest.java`, and the `test/jls/ui/` suite
  including `EditorGestureTest.java` and `DialogConstructionSmokeTest.java`.
- `docs/file-format.md:366-421` - §8, stable ids and canonical order.
- `src/jls/SpatialIndex.java` and `test/jls/SpatialIndexTest.java:205-244` -
  the existing index and its measured scan-versus-index comparison.
- Issue #75 §10 (the modality threat and the out-of-scope canvas AT statement)
  and §12 (the accessible canvas model as the named future unlock).
