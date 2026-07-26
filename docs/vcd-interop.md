# VCD Interop: Waveform Viewers and Autograding

**Status: informative guide** (issue #216). The normative contract —
the `-t` grammar, the stdout report format, and the VCD profile — lives
in [`batch-interface.md`](batch-interface.md); nothing here adds to or
changes it. This page is the worked recipe: produce a VCD from a batch
run, open it in GTKWave or Surfer, and build an autograder on the batch
CLI.

## What JLS offers (and what it does not)

- **Offered:** a conformant waveform export — `-vcd` writes IEEE
  1364-2001 section 18 VCD, deterministic byte-for-byte and verified in
  CI by a spec-derived parser (`test/jls/VcdExportGoldenTest.java`) —
  plus a **stable batch contract** for stdout, exit codes, and the VCD
  profile ([`batch-interface.md`](batch-interface.md), a documented
  stability promise).
- **Not offered: live co-simulation.** JLS runs a batch simulation to
  completion; external tools consume the finished outputs. A live
  co-simulation transport (cocotb/VPI/DPI style, an external testbench
  stepping JLS interactively) was evaluated and **rejected** — see
  issue [#63](https://github.com/anadon/JLS/issues/63). Graders must
  not depend on interacting with a running simulation.

## 1. Produce a VCD from a batch run

Any circuit with at least one **watched** element (or probed net, see
[`batch-interface.md`](batch-interface.md) 4.1) works. This recipe uses
the committed fixture `test/fixtures/fork-4.6-shiftregister.jls`
(three barrel shifters computing 181 shifted by 2 into watched pins
`ll`, `lr`, `ar`).

JLS requires the circuit file's name (minus `.jls`) to start with a
letter and contain only letters, digits, and underscores, so copy the
hyphenated fixture under a conforming name first:

```sh
cp test/fixtures/fork-4.6-shiftregister.jls shiftdemo.jls
jls -b -vcd out.vcd shiftdemo.jls
```

(No installed `jls`? `java -jar jls-<version>.jar -b -vcd out.vcd
shiftdemo.jls` and the `ghcr.io/anadon/jls` container are equivalent —
see the README.)

Exit status 0, and stdout reports the watched pins per the contract:

```
Simulation: No More Activity at 25
Output Pin ar: 0xED (237 unsigned, -19 signed)
Output Pin ll: 0xD4 (212 unsigned, -44 signed)
Output Pin lr: 0x2D (45 unsigned, 45 signed)
```

`out.vcd` now holds the value-change history of every watched signal.
For clocked circuits add `-d limit` (simulation time cap) and `-t
vectors.txt` (input stimulus); to watch elements without editing the
circuit, use a `-s` parameter file with `ELEMENT <name> WATCHED true`.

## 2. Open the waveform in GTKWave

```sh
gtkwave out.vcd
```

In the left-hand SST (Signal Search Tree) pane, click the `shiftdemo`
scope; the signals `ar[7:0]`, `ll[7:0]`, `lr[7:0]` appear in the pane
below it. Select them and press **Append** (or **Insert**) to add them
to the wave view, then **Time → Zoom → Zoom Best Fit** to frame the
run. One VCD time unit is one JLS simulation time unit (the `1 ns`
timescale is nominal, [`batch-interface.md`](batch-interface.md) 4.2).

## 3. Open the waveform in Surfer

```sh
surfer out.vcd
```

Surfer loads the file directly: expand the `shiftdemo` scope in the
sidebar and click each signal to add it to the view. (Without a local
install, <https://app.surfer-project.org> opens the same VCD in the
browser.) Multi-bit signals default to hex display; right-click a
signal to switch to unsigned/signed decimal or binary.

## 4. Autograde over the batch CLI

The supported grading pattern is a plain subprocess bridge:

1. run `jls -b [-t vectors.txt] [-d limit] [-s params.txt] -vcd out.vcd
   circuit.jls`;
2. treat the **exit status** as the failure signal
   ([`batch-interface.md`](batch-interface.md) 1);
3. grade either or both stable surfaces: the watched-element **stdout
   report** (sections 3.2–3.4) and the **VCD** (section 4 — final
   values, or full timing if the grade depends on *when* signals
   change).

A runnable example lives at
[`examples/autograde/autograde.py`](../examples/autograde/autograde.py):
it runs the batch CLI on the fixture above, parses the emitted VCD with
a dependency-free parser, and asserts the expected `ll`/`lr`/`ar`
values against both surfaces. Run it as

```sh
python3 examples/autograde/autograde.py                # jls from PATH
python3 examples/autograde/autograde.py -- java -jar jls-<version>.jar
```

CI keeps the bridge green against the fixture on every push
(`test/jls/AutogradeBridgeExampleTest.java`, which skips when `python3`
is absent).

The example is deliberately **not co-simulation** (issue
[#63](https://github.com/anadon/JLS/issues/63)): it never talks to a
running simulation. If your grader needs a signal the VCD cannot show
(an unwatchable internal net, say), that is a batch-observability gap
owned by issue [#200](https://github.com/anadon/JLS/issues/200) — file
it there rather than working around it with GUI automation.
