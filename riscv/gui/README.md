# GUI-driven construction toolkit

This directory builds JLS circuits **strictly through the GUI**: a pure-JDK,
offline "Squish-class" driver (`GuiDriver.java`) boots the real JLS application
in-process and drives it with genuine `java.awt.Robot` OS-level mouse/keyboard
input, locating menus, palette buttons and dialog fields by walking the live
Swing tree (`getLocationOnScreen`), never by hard-coded pixels. Keyboard actions
that Robot cannot route reliably under a headless window manager are dispatched
to the real focus owner (the same `InputMap`/`ActionMap` a physical key fires) —
the faithful key path the repo's own construction tests use.

Nothing here writes `.jls` text by hand: every element and wire in the circuit
gets there because the editor's real event handlers ran in response to a real
click, drag, dialog entry, or keystroke.

## Proven primitives
- Menu / dialog navigation with real input (`File > New`, creation dialogs,
  radio buttons, name-selector lists).
- `placeExact(tip, fields, radios, x, y)` — choose an element from the palette,
  fill its real creation dialog, and position it at an exact grid cell using the
  keyboard construction caret (issue #75).
- **Coincidence connection** — dropping an element so its port lands exactly on
  another element's port auto-connects them (the editor's own `connect()` runs
  on placement). No fragile free-hand wire routing.
- **Named nets** — `JumpStart`("name a wire") placed coincident on a driver
  output plus `JumpEnd`("connect to a named wire") placed coincident on each sink
  input join by name, so arbitrary fan-out / crossing topology needs no routing.

## Run
```sh
mvn -q package -DskipTests                 # build the JLS jar
Xvfb :99 -screen 0 1920x1200x24 & DISPLAY=:99 openbox &   # headless X + WM
javac -cp target/jls-*.jar -d riscv/gui/out riscv/gui/*.java
DISPLAY=:99 java -Djava.awt.headless=false \
  -cp target/jls-*.jar:riscv/gui/out <MainClass>
```
