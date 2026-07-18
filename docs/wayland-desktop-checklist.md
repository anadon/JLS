# Wayland Real-Desktop Spot-Check — per-release checklist

**Status: release procedure.** This is the scripted checklist that issue
#100 §10 (Threats to Validity) requires and that the decision recorded on
#100 (2026-07-17) made concrete: CI's `gui-wayland` lane proves the GUI on
a *headless* sway compositor (wlroots backend, pixman software rendering),
which can diverge from the GPU-backed compositors real users run. Once per
release, the maintainer runs this checklist on **one real Wayland desktop —
GNOME (Mutter) or KDE Plasma (KWin)** — and records the filled-in results
template below as a comment on issue #100.

The steps mirror prediction P2 of #100: launch, place each element type,
open each dialog, use the KeyPad — no NPE, no `AWTError`, no window at a
nonsensical position.

## 0. Environment

- A physical (or VM-with-GPU) machine running a Wayland session on Mutter
  or KWin. Confirm the session type:

  ```sh
  loginctl show-session "$XDG_SESSION_ID" -p Type   # expect Type=wayland
  ```

- A JetBrains Runtime (JBR) install that ships the Wakefield `WLToolkit`
  (the exact build CI pins is named in `.github/workflows/ci.yml`).
- The release jar under test (`jls-<version>.jar`).
- Run JLS with `DISPLAY` removed from the environment so XWayland cannot
  silently absorb the test — if the frame appears, it is Wayland-native
  by construction:

  ```sh
  env -u DISPLAY <jbr>/bin/java -jar jls-<version>.jar
  ```

  (JLS auto-selects `WLToolkit` on Wayland-only sessions; add
  `-Djls.toolkit=wayland` only if you need to force the decision.)

## 1. Launch

- [ ] The JLS frame appears, fits within the monitor, and has the normal
      title and menus.
- [ ] stderr shows no stack trace, no `AWTError`, no toolkit warning
      other than JBR's known experimental-toolkit notices.

## 2. Place each element type

In a new circuit, create each element once — from the menu, and from the
toolbar button where one exists. For each: the creation dialog (where the
element has one) appears on/near the editor window, and the element lands
under the cursor's last position on the canvas — never at a screen corner,
never off-window, never an NPE.

- [ ] AND gate, [ ] NAND gate, [ ] OR gate, [ ] NOR gate, [ ] NOT gate,
      [ ] XOR gate, [ ] delay gate, [ ] tri-state gate
- [ ] input pin, [ ] output pin, [ ] constant, [ ] clock
- [ ] adder, [ ] mux, [ ] decoder, [ ] extend, [ ] binder, [ ] splitter
- [ ] register, [ ] shift register, [ ] memory, [ ] truth table,
      [ ] state machine, [ ] signal generator
- [ ] jump start, [ ] jump end, [ ] display, [ ] pause, [ ] stop,
      [ ] text
- [ ] wires: connect two elements; the wire follows the pointer while
      drawing.
- [ ] a subcircuit: import or create one and place an instance.

## 3. Open each application dialog

Each dialog appears on-screen, on/near its owner window, and closes
cleanly (button and `Esc`):

- [ ] file open and file save choosers
- [ ] the print dialog(s)
- [ ] the help/about window
- [ ] element property/edit dialogs (double-click a placed register or
      memory)
- [ ] the state-machine editor window (open a placed state machine; add
      a state and a transition)
- [ ] the simulator controls and the signal-trace window (watch an
      element, run a small circuit)

## 4. KeyPad

Open a dialog with a numeric field (e.g. register or memory bits) and
invoke the KeyPad:

- [ ] it appears adjacent to the invoking field's dialog, not at a screen
      corner
- [ ] digits entered on it land in the field
- [ ] it dismisses cleanly (accept, and click-away/`Esc`) and is never
      left orphaned behind other windows
- [ ] moving the owning dialog and reopening the KeyPad places it
      correctly again

## 5. Interaction sanity

- [ ] drag a placed element; it tracks the pointer
- [ ] select-rectangle over several elements and move them together
- [ ] simulate a small circuit end to end; close JLS from the menu; the
      process exits 0

## Failure triage

A step that misbehaves here but passes on the CI rig means the headless
rig diverges from real compositors (exactly the risk #100 §10 names).
File a bug that names the compositor and version, links this checklist,
and references #100; do not mark the release matrix green.

## Results template

Paste as a comment on issue #100:

```markdown
### Wayland desktop spot-check — JLS <version>
- Date: YYYY-MM-DD
- Desktop: <GNOME x.y (Mutter) | KDE Plasma x.y (KWin)>, <distro>
- Runtime: JBR <version/build>
- Monitors/scale: <e.g. 1×2560×1440 @1x>
- §1 launch: PASS/FAIL
- §2 element placement: PASS/FAIL (failures: …)
- §3 dialogs: PASS/FAIL (failures: …)
- §4 KeyPad: PASS/FAIL
- §5 interaction: PASS/FAIL
- Anomalies / notes: …
```
