# A RISC-V CPU built strictly through the JLS GUI

This directory builds a working, minimal **single-cycle RISC-V (RV32I)
accumulator CPU** as a real JLS logic circuit — placed and wired **strictly
through the GUI**, by driving the live JLS application with real
`java.awt.Robot` OS-level mouse and keyboard input. No `.jls` text is authored
by hand: every element and every connection exists because the editor's own
event handlers ran in response to a real click, drag, dialog entry, or
keystroke.

## The GUI-automation driver (`GuiDriver.java`)

A pure-JDK, offline **"Squish-class"** GUI driver. It boots the genuine JLS
application in-process (`jls.JLS.main`) and drives it with real OS-level input,
locating menus, palette buttons, and dialog fields by walking the live Swing
tree (`getLocationOnScreen`) — object-based targeting, never hard-coded pixels.
This is the same strategy commercial tools like froglogic Squish and
open-source AssertJ-Swing / NetBeans Jemmy use; here it is hand-rolled in the
JDK so it needs no network and no license.

Keyboard actions that Robot cannot route reliably under a headless window
manager are dispatched to the real focus owner (the same `InputMap`/`ActionMap`
a physical key fires) — the faithful key path the repo's own construction tests
use.

Proven primitives (each exercises real editor code paths):

- **Menu / dialog navigation** — `File > New`, element creation dialogs, radio
  buttons, and the JumpEnd name-selector list, all via real input.
- **`placeExact`** — choose an element from the palette, fill its real creation
  dialog, and position it at an exact grid cell using the keyboard construction
  caret (issue #75).
- **Coincidence connection** — dropping an element so its port lands exactly on
  another element's port auto-connects them (the editor's own `connect()` runs
  on placement).
- **Named nets** — `JumpStart` ("name a wire") coincident on a driver output
  plus `JumpEnd` ("connect to a named wire") coincident on each sink input join
  by name, so fan-out and crossing topology need no fragile wire routing. Jump
  placement is offset-aware (a jump's port offset depends on its label width).
- **`watch`** — a rubber-band selection plus the editor's Ctrl+W watch action
  marks a register/pin watched so batch simulation reports it.

## The CPU (`RiscvCpu.java`)

A minimal single-cycle datapath, clocked, executing the RV32I instruction
`addi x1, x1, 3` every rising clock edge:

```
    program counter                    accumulator (x1)
    ---------------                    ----------------
    PC(8b) --+--> [PC + 1] --> PC       ACC(32b) --+--> [ACC + 3] --> ACC
             |                                     |
    clock ---+-------------------------------------+
```

Real JLS elements — two positive-edge `Register`s, two `Adder`s, a `Clock`, and
`Constant`s for the immediate and the increment — wired into two feedback loops.
After N clock cycles the hardware register `ACC` holds `3*N` and `PC` holds `N`,
exactly the RV32I `addi` semantics.

## Verification (`verify_gui_cpu.py`)

The GUI-built circuit is checked against the independent reference emulator
`../riscv_ref.py`: driven in JLS batch mode at several clock durations, its
watched `PC`/`ACC` registers are compared to what the reference computes after
running that many `addi x1,x1,3` instructions. All cycle counts match:

```
-d   3000: hardware PC= 1 ACC=  3 | reference x1 after 1 addi =   3 | MATCH
-d   9000: hardware PC= 4 ACC= 12 | reference x1 after 4 addi =  12 | MATCH
-d  15000: hardware PC= 7 ACC= 21 | reference x1 after 7 addi =  21 | MATCH
-d  21000: hardware PC=10 ACC= 30 | reference x1 after 10 addi =  30 | MATCH
-d  41000: hardware PC=20 ACC= 60 | reference x1 after 20 addi =  60 | MATCH
```

`AluDemo.java` is a smaller worked example — a GUI-built ALU add (`5 + 3 = 8`),
the execute stage of the datapath.

## Scope note

This accumulator uses a single hardwired addi immediate (a visible `Constant`
on the canvas) rather than a PC-indexed instruction memory. A Mux- or ROM-based
instruction memory, and shifter-based immediate decode, need **mid-body**
selector/enable ports (`Mux.select`, `Memory.CS`/`OE`, `ShiftRegister.amount`)
that sit inside an element's outline. JLS's coincidence/overlap rules do not let
a driver connect those without free-hand orthogonal wire routing (a coincident
driver whose body clears the element does not trigger `connect()`, and one whose
body overlaps is rejected by `overlap()`). Every connection the accumulator
needs lands on a **side** port, which is why it builds and verifies cleanly; the
adder carry-in defaults to 0 when left open.

## Run

The `run.sh` wrapper in this directory is a convenience for the `java` step:
it runs one `GuiDriver`-family main class under a headless display (default
`:99`) and captures its artifacts and output — the jar and `riscv/gui/out`
classes must already be built. The full manual sequence is:

```sh
mvn -q package -DskipTests                                  # build the JLS jar
Xvfb :99 -screen 0 1920x1200x24 & DISPLAY=:99 openbox &     # headless X + WM
javac -cp target/jls-*.jar -d riscv/gui/out riscv/gui/*.java
DISPLAY=:99 java -Djava.awt.headless=false \
  -cp target/jls-*.jar:riscv/gui/out RiscvCpu                # builds cpu.jls
python3 riscv/gui/verify_gui_cpu.py /path/to/cpu.jls         # checks vs reference
```
