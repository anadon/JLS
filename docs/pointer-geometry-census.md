# Pointer/Geometry Census — the static half of issue #102

**Status: record of the conversion.** This is the classification table
issue #102 called for, covering every global-pointer / absolute-geometry
call site in `src/` and the portable replacement each one received. All
"old" line numbers are at commit `6e2b95ffa22a81c9a14927656b0cfee71282d74a`
(the pre-conversion tree); the conversion itself landed with issues
#103 (event-local element placement), #104 (owner-relative windows,
KeyPad re-homing), and #86 (KeyPad dismissal). The extinction of the
three APIs is enforced by `test/jls/PointerApiRatchetTest.java`.

**What is *not* covered here.** Two pieces of #102's scope remain open
elsewhere:

- `src/jls/JLSStart.java:874` (`getScreenSize` sizing the main frame)
  plus the overall toolkit policy are handled under **#105**; the
  ratchet test carries a shrinking one-file baseline for it, deleted
  when #105 lands.
- The **dynamic half of #102** — probing what each API concretely does
  under JBR's `WLToolkit` (throws / null / degenerate / works) — is
  gated on the #101 rig and is still to be run. This document is the
  static classification only; the probe results land as comments on
  #102.

## Classifications

| classification | meaning | replacement idiom |
|---|---|---|
| element-placement | place a newly created element under the cursor via a global pointer read | `jls.util.Placement.dropPoint(editWindow, x, y, w, h)` using the editor's last event-local mouse position |
| dialog-placement | position a dialog at the cursor / at absolute screen coordinates | `setLocationRelativeTo(owner)` (in element dialogs via `ElementDialog.finishDialog()`) |
| keypad | KeyPad popup placement, parent-chasing, hidden dismissal | owned `JDialog`, placed relative to the invoking button; standard dismissal |
| screen-size | size a window from `Toolkit.getScreenSize()` | the window's `getGraphicsConfiguration().getBounds()` |

## MouseInfo (23 sites) — issue #103

All 22 `jls.elem` sites were the same idiom: after the (modal) creation
dialog closed, re-read the global pointer, subtract the edit window's
screen origin, center the element there — with the null check *after*
the dereference and on the wrong variable (the latent NPE of #103 §2,
quoted from `Gate.java:176-181`). Every one is now
`Placement.dropPoint(editWindow,x,y,width,height)`, where `x,y` is the
last mouse position the editor observed via ordinary `MouseEvent`s
(passed into `setup()` by `SimpleEditor`), falling back to the center
of the visible canvas when unknown. The NPE pattern is extinct.

| old site | old idiom | classification | replacement |
|---|---|---|---|
| `edit/SimpleEditor.java:1593` | global-pointer fallback when `getMousePosition()==null` (matchJump) | element-placement | tracked `x,y` passed straight to `nel.setup(...)` |
| `elem/Adder.java:78` | pointer − window origin, center element | element-placement | `Placement.dropPoint` |
| `elem/Binder.java:67` | same | element-placement | `Placement.dropPoint` |
| `elem/Clock.java:76` | same | element-placement | `Placement.dropPoint` |
| `elem/Constant.java:85` | same | element-placement | `Placement.dropPoint` |
| `elem/Decoder.java:103` | same | element-placement | `Placement.dropPoint` |
| `elem/Display.java:101` | same | element-placement | `Placement.dropPoint` |
| `elem/Gate.java:176` | same, plus the misplaced null check (latent NPE) | element-placement | `Placement.dropPoint` |
| `elem/JumpEnd.java:97` | same | element-placement | `Placement.dropPoint` |
| `elem/JumpStart.java:79` | same | element-placement | `Placement.dropPoint` |
| `elem/Memory.java:96` | same | element-placement | `Placement.dropPoint` |
| `elem/Mux.java:73` | same | element-placement | `Placement.dropPoint` |
| `elem/Pause.java:47` | same | element-placement | `Placement.dropPoint` |
| `elem/Pin.java:101` | same | element-placement | `Placement.dropPoint` |
| `elem/Register.java:86` | same | element-placement | `Placement.dropPoint` |
| `elem/SigGen.java:79` | same | element-placement | `Placement.dropPoint` |
| `elem/Splitter.java:61` | same | element-placement | `Placement.dropPoint` |
| `elem/StateMachine.java:123` | same | element-placement | `Placement.dropPoint` |
| `elem/Stop.java:45` | same | element-placement | `Placement.dropPoint` |
| `elem/SubCircuit.java:146` | same | element-placement | `Placement.dropPoint` |
| `elem/Text.java:80` | same | element-placement | `Placement.dropPoint` |
| `elem/TriState.java:75` | same | element-placement | `Placement.dropPoint` |
| `elem/TruthTable.java:112` | same | element-placement | `Placement.dropPoint` |

Related (same family, no `MouseInfo`): `elem/DelayGate.java:89` used a
guarded `editWindow.getMousePosition()` for the same drop — converted
to `Placement.dropPoint` for consistency (#103).

## getLocationOnScreen (38 sites)

### Pointer-idiom arithmetic — issue #103

These reads existed only to convert a global-pointer read to
window-relative coordinates; they were deleted together with the
`MouseInfo` reads they served.

| old site | old idiom | classification | replacement |
|---|---|---|---|
| `edit/SimpleEditor.java:1594` | subtract editor origin from global pointer | element-placement | deleted (tracked `x,y`) |
| `edit/SimpleEditor.java:1595` | same | element-placement | deleted (tracked `x,y`) |
| `elem/Pause.java:48` | editor origin for the drop subtraction | element-placement | deleted (`Placement.dropPoint`) |
| `elem/Stop.java:46` | same | element-placement | deleted (`Placement.dropPoint`) |
| `elem/TruthTable.java:113` | same | element-placement | deleted (`Placement.dropPoint`) |
| `elem/StateMachine.java:124` | same | element-placement | deleted (`Placement.dropPoint`) |

### Creation-dialog anchoring in `setup()` — issues #103/#104

In the remaining 19 element `setup()` methods the single
`Point win = editWindow.getLocationOnScreen()` read fed **both** the
creation dialog's screen position (`new XxxCreate(pos.x+win.x, ...)`)
and the drop subtraction above. The drop half became
`Placement.dropPoint` (#103); the dialog half became owner-relative:
the dialogs no longer take coordinates at all, and
`ElementDialog.finishDialog()` does `pack();
setLocationRelativeTo(getOwner()); setVisible(true)` (#104), making
`Help.java`'s pattern the norm as #104 §7 prescribed.

| old site | old idiom | classification | replacement |
|---|---|---|---|
| `elem/Adder.java:59` | dialog at cursor + drop arithmetic | dialog-placement | `new AdderCreate()` + `finishDialog()` |
| `elem/Binder.java:51` | same | dialog-placement | `new GroupCreate("Bundler")` |
| `elem/Clock.java:60` | same | dialog-placement | `new ClockCreate()` |
| `elem/Constant.java:64` | same | dialog-placement | `new ConstantCreate()` |
| `elem/Decoder.java:87` | same | dialog-placement | `new DecoderCreate()` |
| `elem/DelayGate.java:72` | same | dialog-placement | `new DelayCreate()` |
| `elem/Display.java:85` | same | dialog-placement | `new DispCreate()` |
| `elem/Gate.java:148` | same | dialog-placement | `new GateCreate(type)` |
| `elem/JumpEnd.java:60` | same | dialog-placement | `new EndCreate()` |
| `elem/JumpStart.java:63` | same | dialog-placement | `new StartCreate()` |
| `elem/Memory.java:80` | same | dialog-placement | `new MemoryEdit(true)` |
| `elem/Mux.java:57` | same | dialog-placement | `new MuxCreate()` |
| `elem/Pin.java:85` | same | dialog-placement | `new PinCreate(type)` |
| `elem/Register.java:70` | same | dialog-placement | `new RegisterEdit(true)` |
| `elem/SigGen.java:63` | same | dialog-placement | `new EditSignals(true)` |
| `elem/Splitter.java:45` | same | dialog-placement | `new GroupCreate("Unbundler")` |
| `elem/SubCircuit.java:130` | same | dialog-placement | `new SubCreate()` |
| `elem/Text.java:60` | same | dialog-placement | `new TextEdit(true)` |
| `elem/TriState.java:59` | same | dialog-placement | `new TriStateCreate()` |

### Change/property-dialog anchoring — issue #104

Pure dialog placement (no adjacent pointer read); same conversion.

| old site | old idiom | classification | replacement |
|---|---|---|---|
| `elem/Clock.java:330` | change dialog at cursor | dialog-placement | `new ClockCreate()` |
| `elem/Constant.java:577` | same | dialog-placement | `new ConstantChange()` |
| `elem/Element.java:648` | timing-change dialog at cursor | dialog-placement | `new DelayChange(isMemory)` |
| `elem/Memory.java:979` | same | dialog-placement | `new MemoryEdit(false)` |
| `elem/Register.java:1134` | same | dialog-placement | `new RegisterEdit(false)` |
| `elem/SigGen.java:204` | same | dialog-placement | `new EditSignals(false)` |
| `elem/Text.java:278` | same | dialog-placement | `new TextEdit(false)` |
| `elem/StateMachine.java:1076` | name-change dialog at editArea origin + cursor | dialog-placement | `new CreateState("Change",...)` |
| `elem/StateMachine.java:1482` | state-create dialog, same | dialog-placement | `new CreateState("Create","")` |
| `elem/State.java:1588` | outputs viewer at owner origin + cursor math | dialog-placement | `show.setLocationRelativeTo(theDialog)`; `showOuts` lost its `Point` parameter |

Related (same family, no forbidden API): `elem/State.java:1397,1709`
passed the fixed screen point `(100,100)` to `finishDialog` — now
owner-relative like every other element dialog;
`elem/ElementDialog.java:121` itself (the shared `setLocation`
centering math) is now `setLocationRelativeTo(getOwner())`.

### Window-position tracking — issue #104

| old site | old idiom | classification | replacement |
|---|---|---|---|
| `sim/InteractiveSimulator.java:247` | cache absolute window origin on resize to place MemTrace frames | dialog-placement | deleted; `MemTrace.showit(Component)` uses `setLocationRelativeTo(owner)` |
| `sim/InteractiveSimulator.java:251` | same, on move | dialog-placement | deleted (the whole `componentMoved` tracker) |

### KeyPad — issues #104 (placement) and #86 (dismissal)

| old site | old idiom | classification | replacement |
|---|---|---|---|
| `KeyPad.java:153` | popup at button's absolute screen position | keypad | owned `JDialog`, `setLocationRelativeTo(button)` + relative offset below it |
| `KeyPad.java:110-127` | `componentMoved` listener chasing the parent dialog by diffing successive `getLocation()` values | keypad | deleted; the popup is an owned window of the dialog it serves |
| `KeyPad.java:129-141` | hidden BUTTON3-on-digit dismiss gesture (the *only* no-value exit besides the hide button) | keypad | deleted (#86); Escape on the popup's root pane and focus loss (click outside) now dismiss; the visible hide button stays |

## getScreenSize (5 sites)

| old site | old idiom | classification | replacement | issue |
|---|---|---|---|---|
| `About.java:54` | center About by whole-screen math | screen-size | `pack()` + `setLocationRelativeTo(JLSInfo.frame)` | #104 |
| `elem/StateMachine.java:881` | size editArea from the whole screen | screen-size | `getGraphicsConfiguration().getBounds()` | #104 |
| `elem/StateMachine.java:898` | same (fallback branch) | screen-size | `getGraphicsConfiguration().getBounds()` | #104 |
| `elem/StateMachine.java:982` | size dialog from the whole screen, pin at `(50,50)` | screen-size | `getGraphicsConfiguration().getBounds()` + `setLocationRelativeTo(getOwner())` | #104 |
| `JLSStart.java:874` | size the main frame from the whole screen | screen-size | **not converted here** — owned by #105 (toolkit policy); ratchet baseline entry | #105 |
