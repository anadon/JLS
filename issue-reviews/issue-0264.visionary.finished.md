# Issue #264: Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated goal — "no board ships half-supported" — is a process rule invented to
heal a process failure (#213 went PCF-first, #215 went ECP5-first). The *purpose*
underneath it is the FPGA-deployment trajectory in
`docs/grand-architecture.md:55-57`: "carry the drawn circuit to a bitstream on a
named board," with the settled stance "orchestrate external tools, never
reimplement HDL semantics." Judged against that arc the feature belongs; judged
against its own decomposition, both planned tasks are the wrong next slice, and
the "both halves" rule is being enforced by convention where the codebase already
knows how to enforce things structurally.

## The reframing: a board is a data row, not a slice of work in two languages

Today a board's identity is split across two languages with no link between them.
`Boards.ICESTICK` (`src/jls/hdl/board/Boards.java:34-35`) says iCE40-HX1K/TQ144 in
Java; `scripts/icestick-handoff.sh:145-147` says `--hx1k --package tq144` in shell.
Nothing tests that they agree, and §3 of the issue admits it: "a Stage 2 board entry
must extend both halves in the same slice or this section goes stale (REPLAN
required)." A contract whose preservation requires a REPLAN comment is not a
contract; it is a note in the margin.

The elegant cut is one level up. `Board` is already deliberately pure data
(`Board.java:26-27`, "adding a board is adding a table entry, never new code") —
finish that thought. Give the record a `Toolchain` component alongside `Format`:
synthesis command template, P&R device/package flags, packer, programmer(s), and
install pointers. Then:

- `scripts/icestick-handoff.sh` becomes one generic `scripts/fpga-handoff.sh`
  parameterized by board — or, better, JLS *emits the recipe* as a third export
  artifact next to `design.v` and `design.pcf`: a `design.mk`/`build.sh` that the
  student can read, diff, and run without the JLS repo checked out.
- The recipe gets the same treatment as the PCF: a byte-deterministic golden under
  `test/resources/hdl/board/`. The device flags are then pinned against
  `Boards.ICESTICK` by a test, not by a human remembering §3.
- "Both halves exist" stops being a convention. A board with no toolchain descriptor
  does not compile; a board with a wrong one fails a golden. The founding rationale
  of this whole consolidation becomes unnecessary — the problem disappears.
- Downstream consumers get it free: #597's board picker reading `Boards.all()` needs
  no GUI change, and #598's "one diagnostic vocabulary, two surfaces" has exactly one
  source for the missing-tool text (which today lives only in shell, at
  `icestick-handoff.sh:108-120`, unreachable from Java).

This also lands where `docs/grand-architecture.md:483-490` says it should — inside
the "headless services-and-export module orchestrating external HDL/FPGA toolchains
out of process." An emitted recipe is that module's output. N shell scripts in
`scripts/` are not a module.

## Open Question 2 is already answered in the repo root

The issue asks whether a CI lane can install oss-cad-suite for a real synth/P&R
smoke. `flake.nix` is right there with a `devShells` output and nixpkgs carries
`yosys`, `nextpnr`, and `icestorm`. A `devShells.fpga` plus a lane running
`nix develop .#fpga -c ...` gives a real-toolchain P&R gate with *pinned* tool
versions — and `flake.lock` then **is** the version record that
`docs/icestick-bitstream-handoff.md:113-121` currently asks a human to type into a
table by hand. Apt-pinned or manually-recorded tool versions pull against this
project's strongest cultural commitment (byte-reproducible jar, `.buildinfo`,
goldens everywhere). Answer the question with the flake and the recommended
"fall back to a manual run" default never gets exercised.

## Disregarding two stated criteria, and why

**1. "Manual hardware flash recorded with board + tool versions per board."** I would
strike this in its current form. A single filled-in table row is a photograph: it
decays, it guards nothing, and it cannot fail. The project already solved this exact
shape of problem — README.md:179-183 verifies the Wayland-native row *two* ways: a CI
lane on every push, plus a scripted once-per-release spot-check on real hardware
(`docs/wayland-desktop-checklist.md`). Adopt that pattern verbatim: a required CI
lane whose falsifiable claim is "the real toolchain accepts our RTL and our
constraints" (nextpnr's exit code — that is the actual scientific content of this
feature), plus `docs/fpga-board-checklist.md` as a per-release hardware spot-check.
Replace the `_TBD_` table with that.

**2. Stage 2 (a second board) as a planned task.** Doing ECP5 next copies the
Java/shell duplication into a second board before the duplication is removed —
doubling the maintenance surface of precisely the drift this consolidation exists to
prevent. Sequence it after the recipe-emission reframing, at which point the ECP5
stage is a table row, an LPF emitter, and two goldens.

## The gap neither planned task touches

The promise in §5.2 is that "the sample circuit **behaves** on the physical board."
Nothing in either planned task moves that, because the obstacle is semantic, not
scripting:

- **Clocks.** `HdlExporter` turns a JLS `Clock` into an input port, with the cycle
  time recorded only in a comment (`HdlExporter.java:286-288`). On the iCEstick, `CLK`
  binds to pin 21 = 12 MHz. A design that visibly blinks in JLS simulation blinks
  ~10^7 times too fast on hardware. That is the canonical first-FPGA failure, and it
  is unmentioned anywhere in the recipe.
- **Internal tri-state.** `VerilogEmitter.triState` emits `assign out = ctl ? in :
  N'bz` — legal Verilog, but iCE40 fabric has no internal tri-state. Yosys either
  infers muxes via `tribuf` or the flow dies at P&R. JLS refuses nothing today.
- **Memory initial contents**, RLE'd in the save format, have no BRAM-init story.

So the highest-value work on this feature is neither of the planned tasks: it is a
written **hardware-fidelity contract for board #1** — what survives the trip, what is
refused at export with a named fix (the honest home for #598's "un-exportable
element" class), and one worked `examples/` circuit (there is none; `examples/` holds
only `autograde`) carrying a clock divider so the demo actually blinks. That single
slice is what turns "a `.bin` was produced" into "a student's circuit ran," which is
the audience statement this issue opens with.

## Arc check

Strengthens the arc: yes — this is the bitstream leg of #224 and the concrete end of
the #59→#63 staging. Duplicates: no. Pulls against: mildly, in two places — a
per-board shell script accretes outside the module boundary
`docs/grand-architecture.md:483-490` draws, and hand-recorded tool versions sit
against the reproducibility culture. Also worth stating plainly: seven filed C38
tasks (#632–#647) now build on a pipeline this feature owns and no filed task
delivers. If the recipe-emission reframing is adopted, file it as this feature's
first child — it is the piece all seven of those actually depend on.
