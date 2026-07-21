# How we know #75 works for users — keyboard operability & accessibility

This is the standing checklist for issue #75 (keyboard operability &
accessibility). It enumerates every user-observable behavior the feature
promises, the concrete signal that shows the behavior happened, and the test
that pins that signal. When you change the editor's input handling, focus
handoff, menus, accelerators, or the shared-`Action` wiring, re-check this
list: each row's test must still go **red if that behavior breaks**.

Run the whole thing with the authoritative green bar:

```
xvfb-run -a mvn -B verify -Djls.test.headless=false
```

The `@Tag("display")` UI tests run under the #162 Xvfb substrate; display
tests carry `rerunFailingTestsCount=2` for transient popup-timing flakes.

## Why "the test is green" has to mean "a real user's key works"

The original H2 focus bug — choosing a gate from the tool bar left keyboard
focus on the palette *button*, so a real user's arrow/Enter keys never
reached the canvas — **passed the tests** for a while. The reason: the gesture
harness drove keys with `canvas.dispatchEvent(keyEvent)` on a hardcoded canvas
reference, which force-feeds the canvas regardless of where focus actually is.
A green test proved nothing about a real keystroke.

The fix is a **focus-faithful driver**. Keyboard input is dispatched to the
**live `KeyboardFocusManager` focus owner**, read at dispatch time — the same
authority the AWT event pipeline consults when routing a real user's key:

- `EditorGestureSupport.focusOwner()` — reads
  `KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()` on
  the EDT.
- `EditorGestureSupport.pressKeyThroughFocusOwner(keyCode[, mods])` —
  dispatches `KEY_PRESSED` to that live owner (throws if there is none, since a
  real keystroke would also have nowhere to go).
- `EditorGestureSupport.focusCanvas()` / `giveFocusTo(c)` — stage a focus
  owner and **poll the KFM until the handoff actually lands**, so driving is
  deterministic rather than racing a transient focus state.

Because the driver reads the real focus owner, a regression that strands focus
off the canvas turns the faithful tests **red**, where the old
`canvas.dispatchEvent` path stayed green. This is proven, not asserted: two
independent mutations were demonstrated to flip the suite red — (A) deleting
the `setup()` `requestFocusInWindow()` handoff (three faithful tests hard-red),
and (B) making the driver force-feed the canvas instead of the live owner
(`FocusFaithfulKeyboardTest` leg 1 hard-red, proving the faithfulness guard is
not a no-op).

Why dispatch to the focus owner rather than `java.awt.Robot`: under the
WM-less #162 Xvfb a single `Robot` key press auto-repeats into ~30
`KEY_PRESSED` events, which makes exact "moved one grid step" assertions flaky.
Focus-owner dispatch delivers exactly one event while still honoring the real
focus subsystem (it routes to the button, not the canvas, when the button
holds focus, and vice versa — both verified). `Robot` *is* used where a genuine
pointer is unavoidable (paste reads `getMousePosition()`), and `Robot`-real
focus is confirmed feasible here (`EditorKeyboardConstructionTest` becomes the
canvas focus owner after a real tool-bar `doClick()`).

## Behavior → observable signal → faithful test

### H2 — keyboard-only construction (the crux: focus-faithful)

| User-observable behavior | Observable signal | Test (faithful unless noted) |
|---|---|---|
| Choosing a palette element hands focus to the canvas (so the next key reaches it) | The live KFM focus owner becomes the canvas after a tool-bar `doClick()` | `FocusFaithfulKeyboardTest.*` (leg 2); `EditorKeyboardConstructionTest` re-checks after **both** the OR-gate and NOT-gate choices |
| An arrow moves the placing element / caret one grid step | The placing gate's / caret's model position advances exactly one `JLSInfo.spacing` | `FocusFaithfulKeyboardTest` (leg 2); `KeyboardEditingFaithfulTest.arrowNudge…` |
| Enter places (commits) the positioned element | After Enter the editor is idle: a further arrow moves only the caret, the committed gate stays put | `KeyboardPlacementFaithfulTest.enterThroughFocusOwnerCommitsThePlacedGate` (waits on a positive observable — the caret moving — not a fixed sleep) |
| Arrow-nudge of a selection is undoable | Ctrl+Z restores the pre-nudge X; Ctrl+Y re-applies it | `KeyboardEditingFaithfulTest.arrowNudge…IsUndoableWithCtrlZ`, `.redoThroughFocusOwnerReappliesTheNudge` |
| `R` rotates / `F` flips the selection | The element's persisted `save` block (orientation + geometry) changes | `KeyboardEditingFaithfulTest.rotateKeyRotates…`, `.flipKeyFlips…` |
| `Delete` removes the selection | The gate is gone from the circuit model | `KeyboardEditingFaithfulTest.deleteKeyRemoves…` |
| `W` starts a wire, `Esc` abandons it | A `WireEnd` appears in the model, then disappears | `KeyboardEditingFaithfulTest.wireStartAndEscape…`; `EditorKeyboardConstructionTest.keyboardDrawsAWireInOpenSpace` |
| The whole two-gate tutorial circuit builds with no pointer | An OR + NOT gate, wired output-to-input, exactly as the mouse tutorial produces | `EditorKeyboardConstructionTest.keyboardBuildsTheTwoGateCircuit` (every post-choice key faithful; both palette handoffs re-checked) |
| Selecting an editor tab hands focus to its canvas (focus no longer follows the mouse) | The live KFM focus owner becomes that editor's canvas after the tab is selected | `TabSelectionFocusTest` |

### H1 — one shared `Action` per op, reused across three surfaces

| User-observable behavior | Observable signal | Test |
|---|---|---|
| Canvas key, popup item, and menu-bar item are the **same** `Action` object per op | Object identity over all `EditOp.values()` and the menu items | `EditActionMatrixTest` (identity); `KeyboardEditingFaithfulTest.faithfulKeysDriveTheSharedActions` |
| Popup Rotate CW / Rotate CCW / Flip actually run the op | The gate's persisted `save` block changes after `doClick` on the shown item | `PopupOperationBehaviorTest` (popup-open hardened with a bounded right-press retry) |
| Cut / Copy / Paste run (pre-#75 ops, re-homed onto the shared `Action`) | Clipboard-circuit model contents | `EditorSaveAndClipboardTest` + identity tie in `EditActionMatrixTest` |
| A window-scoped menu accelerator fires from a **non-canvas** focus owner | Ctrl+A selects the whole circuit; Delete removes it — both fired from a focused tool-bar button | `MenuAcceleratorFiringTest.windowScopedAcceleratorsFireFromANonCanvasFocusOwner` |
| Ctrl/Cmd+S saves from any focus location (the literal P1 prediction) | Firing Ctrl+S from a focused tool-bar button clears the dirty flag and writes the `.jls` file | `MenuAcceleratorFiringTest.fileSaveAcceleratorSavesFromANonCanvasFocusOwner` |
| Ctrl/Cmd+W → Watch (the overload split from plain-W → wire-start) | Firing Ctrl+W flips the selected element's watched flag; plain-W instead starts a wire | `KeyboardEditingFaithfulTest.watchKeyTogglesWatchedThroughFocusOwner` (Ctrl+W) + `.wireStartAndEscape…` (plain-W); `EditActionMatrixTest` (map identity) |
| Edit + Element menus present, correctly ordered, accelerators shown | The canonical booted-menu-bar tree | `MenuBarSpecTest` |

### Accessibility (the delivered #75 scope, items 1–7)

| User-observable behavior | Observable signal | Test |
|---|---|---|
| Top-level menus have distinct mnemonics | `menu.getMnemonic()` on the real booted bar, all distinct | `MenuMnemonicAndAccessibleNameTest` |
| Menu items announce an accessible name | `getAccessibleContext().getAccessibleName()` == label, on the real bar | `MenuMnemonicAndAccessibleNameTest` |
| Palette buttons announce an accessible name and are keyboard-focusable | Real `getAccessibleName()` is non-blank; button is focusable | `PaletteButtonAccessibilityTest` (pins the `SimpleEditor` fix that set the name from the tooltip on icon-only buttons) |
| In-app keyboard help exists, is reachable, and its documented keys match the code | Map target resolves; TOC-reachable; inline links resolve; **every documented hot key equals the `EditOp` accelerator** | `HelpTopicsTest` (existence/reachability) + `HotkeysHelpAccuracyTest` (content: `hotkeys.html` keys pinned against `EditOp.accelerator`, so the docs cannot silently desync) |

## Real implementation gap this verification caught and fixed

Icon-only palette buttons had `Action.NAME == ""`, so
`getAccessibleContext().getAccessibleName()` returned the empty string — a
screen reader focusing a palette button announced nothing. Fixed in
`SimpleEditor.makeElement` by setting the accessible name from the tooltip on
both the tool-bar button and its mirror "elements" menu item. Falsification
confirmed: with the fix reverted, `PaletteButtonAccessibilityTest` goes red on
all reruns.

## Red-on-break evidence (re-runnable)

- Delete the `setup()` `requestFocusInWindow()` handoff → the faithful
  keyboard-construction tests hard-red (the palette handoff proof).
- Make the driver force-feed the canvas → `FocusFaithfulKeyboardTest` leg 1
  hard-red (the faithfulness guard is real).
- No-op the `WATCH` op's `submitOp(new ToggleWatched(...))` →
  `KeyboardEditingFaithfulTest.watchKeyTogglesWatchedThroughFocusOwner`
  times out red (all reruns).
- Change any key in `hotkeys.html` →
  `HotkeysHelpAccuracyTest.documentedKeysMatchTheEditOpAccelerators` red.
- Revert the palette a11y-name fix → `PaletteButtonAccessibilityTest` red.

## Deliberately out of scope / deferred (not covered, and why)

- **Canvas scene-model assistive-technology boundary.** Elements drawn on the
  editor canvas are not exposed as individual accessible objects to a screen
  reader (the canvas is a single custom-painted component). #75 scopes this as
  future work (§10); it is a genuine residual, not a regression, and no test
  pins it because the behavior does not yet exist.
- **Alt+letter physically *opening* a menu.** The mnemonic *property* is read
  on the real components, but no test drives the look-and-feel's Alt-navigation
  state machine to pop a menu open. That needs `Robot` Alt-key delivery into
  the menu-selection manager, exactly the auto-repeat/timing surface the
  focus-owner driver avoids; property-level verification is the robust bound.
- **Modal `TellUser` dialog accelerator inertness.** Asserting that a raised
  modal dialog does not leak window accelerators blocks the EDT and is
  inherently timing-fragile in this suite; not added.
- **mac-only keymap** (Cmd, Backspace-delete, Shift+Cmd+Z / Cmd+Y redo alias).
  Policy-pinned in `MenuAcceleratorPolicyTest`; this Linux substrate cannot
  exercise the mac keymap behaviorally.
- **Probe / Modify / Timing popup ops fired behaviorally.** These open modal
  dialogs; they are identity-pinned (`EditActionMatrixTest`) and
  structure-pinned (`MenuBarSpecTest`). Rotate/Flip (the geometry-mutating
  popup ops) are the ones covered behaviorally.

A known pre-existing flake, `EditorGestureTest.undoRestoresADeletedElement…`
(popup timing), is absorbed by `rerunFailingTestsCount=2` and is not
attributable to #75.
