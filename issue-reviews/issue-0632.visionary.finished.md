# Issue #632: TASK-C597-1: the board flow gets a File-menu entry and a board picker read from Boards.all() — keyboard-reachable, stably named, and needing no GUI change when a board is added
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The destination is #597's sentence, and it is the right destination: *a GUI-only student
takes a drawn circuit to an FPGA without opening a terminal.* That serves the project's
strongest arc — JLS is a pedagogy tool whose whole value is that a student draws a thing
and the thing becomes real (README: "students draw circuits… then simulate them"; #264:
"schematic to a flashed FPGA"). Everything I say below accepts that goal.

What #632 actually proposes is narrower and, I think, misaimed: a File-menu entry plus a
**board picker** whose central engineering content is that its list comes from
`Boards.all()` rather than from GUI source, proven by a test that injects a synthetic
board. I am explicitly disregarding AC-1 and AC-4 as written, for the reasons below.

## Three facts about the substrate that the issue does not reckon with

1. **`Boards.all()` has one element.** `src/jls/hdl/board/Boards.java:81` —
   `private static final List<Board> ALL = List.of(ICESTICK);`. `Board.Format` has one
   constant (`Board.java:41`). #416 documents this as observation O1 and calls the
   existing totality tests "vacuous over a one-element enum".
2. **The path this GUI fronts has never produced a bitstream.** #264 §5 says so in its
   own words: only the hermetic stub-PATH selftest runs; "No real synthesis,
   place-and-route, or bitstream production has been evidenced anywhere." The manual-flash
   table in `docs/icestick-bitstream-handoff.md` is all `_TBD_`.
3. **The second board is not merely unbuilt, it is blocked.** #416 is `blocked_by: [386]`
   (no `nextpnr` installed in any workflow).

So AC-1's headline property — "a board added by #264/#416 appears with no GUI change" —
can only be verified today by a mock, because the future it insures against does not
exist and cannot be reached without #386. A picker over a list of length one is a modal
that shows the student one choice they cannot decline, and a test that fabricates a second
board to prove the picker would have shown it. That is scaffolding built to hold up a
floor nobody has poured.

## The cut is the horizontal cut #264 recorded as its own founding mistake

#264 §2 states the rationale for its decomposition in exactly these terms: *"per-board
vertical slices (both halves per board) rather than horizontal layers — the horizontal cut
is exactly what let #213 and #215 drift apart (PCF-first vs ECP5-first)."*

#632 slices #597 horizontally: it takes the entry-point layer (menu + picker), leaving the
pin dialog (#597 AC-2) and the one-click run (#597 AC-3) to unfiled siblings. Landed alone,
#632 ships a menu item that opens a dialog that cannot export anything. It delivers zero
student capability and creates a surface that will drift from the pin dialog exactly as
#213 drifted from #215. The parent feature's own recorded lesson argues against its first
child's shape.

**Alternative decomposition (concrete).** Make TASK-C597-1 the thinnest *vertical* slice
instead: File > "Export for board…" for `icestick` only, no picker at all, with binding and
the handoff run included, on one board, end to end. When #416 lands a second board, the
picker is a four-line change and its "no GUI change" property is then tested against a
board that genuinely exists. Ship capability first, extensibility when there is something
to extend.

## The reframing that makes the picker and the pins file largely disappear

The deeper observation is that **#632 inherits the CLI's data model without asking whether
it is the right one for a drawing tool.** `-pins` is a side file whose keys are
*legalized HDL port names* — `PcfEmitter.emit` walks `model.ports()` and builds keys from
`port.name()`, which came out of `HdlNames`' legalization pass (non-`[A-Za-z0-9_]` → `_`,
reserved words get `_`, collisions get `_2`). A student who names a pin `count out` must
type `count_out` in the bindings file; two colliding names silently become `x` and `x_2`.
Renaming a pin in the editor silently rots the file. A modal that reproduces this
vocabulary in combo boxes inherits every one of those traps, and #597 AC-2's "produces
exactly the bindings the headless `-pins` file expresses" locks them in.

**The other seam: bind at the pin element, not in a modal.** `jls.elem.Pin` already carries
its parameters through the declarative `Attribute` registry (`Pin.java:165-215`:
`name`, `bits`, `watch`, `orient`). Add one optional `StringAttribute("boardpin")` there,
plus a circuit-level board name, and:

- The pin-assignment dialog **mostly ceases to exist**. A student sets the board pin in the
  InputPin/OutputPin dialog where they already type the port name, from a combo fed by
  `Boards.byName(circuit.board()).pins().keySet()`. Unknown-pin and wrong-direction are
  unrepresentable at entry, which is #597 AC-2's goal reached by construction rather than
  by validation code.
- The name-mangling trap vanishes: the GUI never shows a legalized identifier; the emitter
  derives `PinBindings` while it walks `model.ports()`, where the JLS pin → HDL name map is
  already in hand.
- **#632 AC-4 becomes true by construction and needs no test.** The GUI holds no board list
  (the board name is a string in the circuit), no format list (never named in the GUI), and
  no pin table (resolved from `Boards`). A negative structural assertion is a weak
  instrument anyway; this repo's idiom for that shape is a ratchet
  (`HeadlessCoreRatchetTest`, `NotificationRatchetTest`), and the best ratchet is one you do
  not need.
- The binding **travels with the circuit**, which serves arcs #632 never considers: an
  instructor hands out a pre-bound circuit; plain-text saves diff the bindings in git
  (README "Circuit files"); the container autograder runs `jls -export design.v design.jls`
  with no side file. The batch surface gets *simpler*, not doubled.
- `-board`/`-pins` stay the CLI truth (#632 AC-5 preserved): an explicit `-pins` file
  overrides what the circuit carries, and GUI/CLI byte-identity holds because both funnel
  into the same `PinBindings` value.

Honest costs, named rather than waved at: a `.jls` format addition (`docs/file-format.md` is
normative; `FORMAT` versioning per #79 exists for exactly this) — though note
`Element.setValue` silently ignores unknown keys, so current readers tolerate the new line;
`Attribute.copy` semantics for paste (two pins bound to one board pin — the editor should
warn, and `PcfEmitter`'s double-binding aggregation already catches it at export); and
subcircuit pins, which are not module ports, so a binding there must be inert and say so.

## Second reframing: one Export surface, not three

#288 is still open and unimplemented, so #632's `ordering_after: [288]` is a dependency on a
pattern that does not exist. That is an opportunity, not a problem. The File menu already
carries "Export Image" (`JLSStart.java:1525`); #288 adds "Export HDL…"; #632 adds a third
export entry. #288's own Open Questions already flag the contended mnemonic namespace ("E"
is taken). Three sibling export items with three chooser flows, three component-name
prefixes, and three keyboard-reachability tests is worse for the keyboard-only constituency
(#75) than **one "Export…" dialog with a target selector** — Verilog / VHDL / Image /
Board — since board export *is* an export, and the target already determines the emitter in
the CLI by extension inference. That is one component-naming budget, one accelerator, one
`MenuAcceleratorPolicyTest`-pattern test. It also lands the board target inside the seam the
architecture already declares: `HdlExtensionPoints.EXPORTER` ("board-aware export (#213)
and bitstream handoff (#215) extend this seam", `HdlExtensionPoints.java:20-22`) — a seam
#632 does not mention and would bypass by hanging a second flow off the menu instead.

## Recommendation

Keep #597's destination. Re-cut its first task as: **one board, end to end, in the editor**
(entry → binding on the pin elements → one action → handoff → reported outcome), folded into
a single Export surface, with no picker and no synthetic-board test. File the persisted
`boardpin` attribute as its own small design issue against `docs/file-format.md`, since it is
the piece with real contract consequences. And note plainly on #597 that no GUI work on this
path should close before #264's stage-1 evidence exists — a one-click button that has never
once produced a bitstream is a worse student experience than the terminal it replaces.
