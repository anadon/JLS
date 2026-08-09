# Issue #522: CAP-38: a drawn circuit lands on the FPGA board a classroom actually has — from the GUI, through the open toolchain, with the pin map checked before the cable is touched
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its planned features, CAP-38 asserts one thing: **JLS's endpoint
should be physical hardware, not a screenshot.** That claim is correct and it is
already load-bearing elsewhere in the tree. `docs/capability-roadmap/sweep-06-physical-boundary.md`
argues at length that "being a legitimate front end to somebody else's physical
flow" is *not* another tool class, "and provably so: JLS already does the FPGA
analogue of exactly this at `src/jls/hdl/board/PcfEmitter.java`." #510 §2 names
the missing FPGA flow as one of five places JLS loses head-to-head. The
direction is endorsed without reservation.

The *route* is where I disagree, in three places, and one of them is
disqualifying for AC-1 as written.

## The binding constraint is not the GUI

CAP-38 spends 4–6 mw (PF-1 + PF-2) putting a dialog on a pipe that refuses most
of what a student will draw. At head:

- `src/jls/hdl/HdlExporter.java` — `EXPORTED` is 22 classes; **`SubCircuit` and
  `Memory` are hard rejects**, and `test/jls/hdl/HdlPolicyTest.java`'s
  `memoryIsRejectedByName` / `subCircuitIsRejectedCleanly` *pin the rejection as
  intended behaviour*.
- sweep-06 measured the consequence on this repository's own flagship:
  `jls -export cpu.v riscv/build/addi.jls` fails on four Memories and three
  ShiftRegisters. "It cannot reach step one of the open flow."

So AC-1 ("draw → pins → programmed board, from the GUI, no terminal") will be
demonstrated with a blinky or a counter and will fail on the second lab of any
real course, because lab 2 is where a student factors a design into subcircuits.
Worse, sweep-06 already named this as a *teaching inversion*: "a student who
structures their design well is punished by the exporter; one who draws a
1000-element flat mess is rewarded." CAP-38 builds a GUI that makes that
inversion the first thing a student meets, and PF-2 dignifies it — "unsupported
element" becomes a polished, tested, permanent diagnostic for a limitation that
sweep-06 prices at 5–8 mw to remove (change **A**: hierarchy + Memory +
ShiftRegister).

**Concretely:** CAP-38's `ordering_after` should carry sweep-06 change A (or
whatever feature owns it) as a hard predecessor, ahead of #264 and #288. Without
it the capstone's outcome sentence — *student draws, clicks, sees the board do
it* — is true only for circuits so small that the board adds nothing over the
interactive simulator. With it, the capstone is the payoff for work the roadmap
already wants for #110/#109/#304/EDIF/BLIF anyway. That is the strengthening
move: CAP-38 becomes the *demand pull* for hierarchy export rather than a
parallel investment that routes around it.

## Reframing 1: pin bindings belong in the circuit, not in a side file

`PinBindings` parses `-pins` and its own javadoc calls that "binding UX option
(a): headless and autograder-friendly" — announcing that an option (b) exists and
was deferred. PF-1 fills the gap with a bespoke modal dialog, and the issue never
says what that dialog *writes*. If it writes a transient `pins.txt`, the binding
is not part of the circuit: it does not survive save, does not diff, does not
travel in a lab template, is invisible to undo, and cannot be graded.

The better seam is the one JLS already owns. Pin assignment is an element
attribute: `InputPin`/`OutputPin` gain a board-pin attribute through the existing
declarative `Attribute` machinery (ARCHITECTURE.md §"Adding an element today",
issue #52), and the circuit gains a target-board field behind a `FORMAT` bump.
Then:

- The "pin-assignment dialog" is **ordinary element attribute editing** — no new
  modal surface, no new keyboard/a11y story, no new `#91` harness fixtures. PF-1
  shrinks from 2–3 mw to a board picker plus a validation pass.
- "Unassigned pin" and "wrong direction" stop being a bespoke pre-flight layer
  and become circuit validation of the kind `finishLoad` already performs — so
  the CLI, the GUI, and CI get the same diagnostic from one implementation
  instead of PF-2's "unified diagnostic vocabulary across GUI and headless."
- `-pins` degrades gracefully to an *override* for autograders, keeping the
  headless contract intact.
- An instructor can ship `lab3-template.jls` with the board and pins already
  bound. That is the actual classroom artifact, and neither #264 nor CAP-38
  currently produces it.

This is strictly smaller than what PF-1/PF-2 propose and it delivers something
they cannot.

## Reframing 2: boards are a target table, not a per-board project

`Board` is already a record — "a board is deliberately just data … so adding a
board is adding a table entry in `Boards`, never new code." The design intent is
right and stops one step short: the *toolchain recipe* is not data. It is
hard-coded in `scripts/icestick-handoff.sh` (iCE40-HX1K/TQ144 baked in), which
#264 §3 flags as its own staleness hazard.

Finish the abstraction the code started: a target = (board data, constraint
format, toolchain stage chain). Then PF-3's Basys-3 question stops being a
strategic decision that needs a written go/no-go (AC-4) and becomes two separable
facts: (1) `Board.Format.XDC` plus a printer — the same shape as `PcfEmitter`,
roughly 1 mw, and *unconditionally worth having* because it is what the ASEE
courses' boards consume; (2) whether JLS drives Vivado — already answered "no" by
KC-38-1, permanently, and the documented-recipe form needs no arithmetic to
justify. PF-3 drops from 2–4 mw to ~1 mw plus a docs page, and #416's second
board rides the table rather than an unresolved ownership dispute. AC-4's
deliverable — a decision document — is a symptom of the missing abstraction; with
the table there is no decision left to write.

## Reframing 3: the CI lane should check fidelity, not exit codes

PF-4 as written asserts that yosys/nextpnr exit 0 on generated Verilog. That
catches syntax, which is Logisim-Evolution's #1871 failure, and stops there. JLS
can do something no competitor in #510's matrix can, and the parts are already in
the tree:

> circuit → `VerilogEmitter` → yosys → JSON netlist → **`jls.hdl.imp.NetlistImporter`**
> → circuit → `BatchSimulator` → compare against the original circuit's existing batch golden.

`src/jls/hdl/imp/NetlistImporter.java` and `src/jls/hdl/yosys/` exist (#61);
today they cover the combinational core and *report* every construct they cannot
map. That closes the loop into a **differential oracle**: the drawn circuit and
the synthesized netlist are asserted to agree under JLS's own normative
semantics, with no hardware, no vendor tool, and no new golden format — the same
technique `docs/simulation-semantics.md` already binds any future simulation
strategy to (the #202 RV32I differential run). It also converts the exporter's
weakest claim — "structurally valid" — into "semantically checked," which is
exactly the axis where #510 scores JLS strongest and Logisim-Evolution weakest.
Same 1–2 mw budget; incomparably more truth per lane. It also gives the importer
a reason to grow past combinational cells, which nothing currently does.

## The cheapest move, which the capstone does not contain

#510 §2 records that the board flow is "at head of main (unreleased,
unreachable)". Half the competitive loss CAP-38 exists to close is a *shipping*
loss, not a building loss. Cutting a release, adding a `docs/` page, and putting
one screenshot in the comparison table changes what every evaluator sees for
approximately zero maintainer-weeks — before any of the 8–13 mw is committed.
That should be its own tiny predecessor, not folded into AC-5's screenshot
pipeline at the end of a capstone.

## Disregarding two acceptance criteria, explicitly

- **AC-1's "no terminal" is the wrong success measure.** The valuable property is
  that the flow is *reachable and diagnosable* from the GUI, not that a terminal
  never appears. `iceprog` needs USB permissions, and a toolchain install is a
  terminal act on every platform; a design that must hide that will either
  swallow errors (Logisim-Evolution #91, the anti-pattern PF-2 names) or
  reimplement a package manager. Better criterion: **every failure is named in the
  GUI with the exact command that fixes it**, and the happy path needs no typing.
  That is honest, testable, and does not push JLS toward owning toolchain
  detection forever.
- **AC-4 (the Basys-3 decision document)** dissolves under Reframing 2, as above.

## Verdict

**endorse-with-reframing.** The endpoint is right and strengthens the project's
arc; three of the four planned features cut along seams that are more expensive
and less useful than the ones the codebase already offers. Reordered: (0) release
what exists; (1) hierarchy + Memory export as a hard predecessor; (2) pin binding
as circuit data via `Attribute`, which collapses PF-1 and PF-2 into validation;
(3) the target table, which collapses PF-3; (4) the round-trip equivalence lane
in place of PF-4's exit-code lane. That plan reaches the same outcome sentence
for a circuit a student would actually be proud of, and it leaves behind
capabilities the rest of the roadmap already needs.
