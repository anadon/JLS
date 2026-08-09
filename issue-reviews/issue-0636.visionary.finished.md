# Issue #636: TASK-C597-3: one button carries the design from the dialog to a programmed board — export, constraints and the handoff script run from the GUI with the outcome reported, and no terminal
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the button away and the goal is: *a student who drew a circuit gets it running on
real silicon, and JLS does not abandon them at the boundary.* That goal is right and it
is squarely on the project's arc — #264, #359, #522, the whole `jls.hdl.board` package
and `scripts/icestick-handoff.sh` exist to serve it.

The issue then encodes that goal as a *UI affordance* ("one button, no terminal") and
picks the one implementation of that affordance the architecture forbids. Its own
binding constraint KC-38-1 ("no vendor-toolchain process driving lives in this surface")
contradicts its AC-1 in the same paragraph. #359 §4.1 makes it an invariant with a
grep-based DoD check, and `grep -rn "ProcessBuilder\|Runtime.getRuntime().exec" src/`
still returns 0 at head. The adversarial comment on this issue already caught that and
corrected the scope. I agree with the correction and go further: **the corrected scope
is still cutting at the wrong seam, and I am disregarding AC-1 through AC-4 as written.**

## Where the seam actually is

`JLSStart.java:386-475` is the whole board-aware export path, and it is *inline in
`main`*, interleaved with `System.err.println(...)` + `System.exit(1)`:

```java
Board board = boardName == null ? null : Boards.byName(boardName);
...
constraintText = PcfEmitter.emit(model, board, bindings);
...
Path temp = Path.of(exportFile + ".tmp");
```

There is no callable "export this circuit for this board with these bindings" service
anywhere in `src/`. This single fact explains most of what is odd about #636:

- **AC-3 polices a duplication a real seam would make impossible.** "A test asserts the
  artifacts the GUI produces are byte-identical to the CLI's" is only a meaningful test
  when there are two producers. Extract `BoardExport.run(Circuit, Board, PinBindings,
  Path outDir) -> Result` (returning diagnostics rather than calling `System.exit`), have
  `JLSStart` call it and the dialog call it, and byte-identity is *structural*, not
  asserted after the fact. AC-3 becomes a tautology, which is what a correct design
  feels like.
- **AC-4's premise is false today.** The CLI is all-or-nothing *per file*, not across the
  artifact pair: the `.v` is written and atomically renamed into place (`:440-452`), and
  only then is the `.pcf` written (`:470-474`). An I/O failure on the constraint write
  leaves the Verilog on disk. A GUI button that "matches the all-or-nothing discipline
  the handoff script's preflight and `PcfEmitter` already hold" would inherit a discipline
  that does not exist at the level the criterion claims. The extracted service is where
  a real two-file transaction belongs — stage both to temp, rename both, unlink both on
  failure.

So the first honest unit of work under #636 is not a button. It is lifting the export
orchestration out of a 2000-line Swing bootstrap class into `jls.hdl.board`. The button
is then ~40 lines of consumer code, and #597 AC-2 and AC-5 fall out for free.

## The reframing: emit a handoff *bundle*, not a command string

The corrected scope's destination — "present the handoff command line, copyable" — is a
clipboard string that dies when the dialog closes. That is a weak artifact for a project
whose entire identity in this repository is *durable, recorded, checkable artifacts*:
`SHA256SUMS`, `bom.json`, `.buildinfo`, build-provenance attestations, golden constraint
files, `docs/reproducibility.md`. A copyable string is culturally foreign here.

**Alternative design.** The one action writes a self-describing *handoff directory*:

```
blinky-icestick/
  blinky.v            # HdlExporter output
  blinky.pcf          # PcfEmitter output
  build.sh            # scripts/icestick-handoff.sh, parameterized for this board
  MANIFEST.txt        # board name, top module, bindings, JLS version, sha256 of each file
  README.md           # the tools needed, and the one command to run
```

Everything in it is produced by the extracted service; `build.sh` is the *existing*
script, not a reimplementation (#215 H2, "delegate, do not reimplement", holds
untouched). No subprocess in `src/`. The student's remaining step is one command in a
directory that tells them what it is — and, unlike a clipboard string, it can be
committed, emailed to a TA, attached to a lab report, diffed, and checksummed. It is
also directly gradeable, which connects this work to the autograder audience
`docs/batch-interface.md` and `docs/vcd-interop.md` already serve.

**The out-of-the-box move that dissolves the persona problem.** The acceptance criteria
describe "a GUI-only student who never opens a terminal" and then hand them a button
that only works if `yosys`, `nextpnr-ice40`, `icepack` and `openFPGALoader` are already
installed. That persona is internally incoherent: someone who has never opened a terminal
has not installed oss-cad-suite either. This project already solved the same problem once
— it ships `ghcr.io/anadon/jls` so that autograders need no local Java. Do it again: have
the emitted `README.md`/`build.sh` carry a `docker run` line against an oss-cad-suite
image as the no-local-toolchain path. That is the only route by which the stated persona
ever actually gets a bitstream, and it costs `src/` nothing.

## The higher-value goal this task is displacing

"No terminal" is optimizing the step JLS is *worst* positioned to own. Shelling out to
yosys is something any script can do. The step only JLS can do — because only JLS has the
schematic, the propagation delays, the tri-state/HiZ semantics of
`docs/simulation-semantics.md`, and the parity framing of `docs/parity-contract.md` — is
telling the student **why their drawn circuit will not survive synthesis**: combinational
loops, multi-driver nets, unbound top-level ports, a clock that is not on a clock-capable
pin, HiZ used where the fabric has no tri-state.

A student's real failure on this path is almost never "I could not find the command." It
is "the bitstream flashed and the board does nothing." A board-preflight panel that names
those defects at the schematic level would be a genuinely novel capability for an
educational simulator, is entirely in-process, breaks no invariant, and would make the
eventual button worth pressing. #597 has no such task. It should.

## Trajectory check, and the ordering problem

- **Strengthens:** extraction of `BoardExport` out of `JLSStart` — unambiguously good for
  the module map in ARCHITECTURE.md and for #91-harness drivability.
- **Pulls against:** as literally written, three recorded decisions at once — KC-38-1,
  #359 §4.1, and #215 H2 via the handoff script's own header. A single task should not be
  the place where three recorded invariants get quietly renegotiated.
- **Ordering:** #264's own body records that *no real bitstream has ever been produced by
  this path* — only the hermetic stub-PATH selftest, control-flow assertions with fake
  tools. #264's iCEstick hardware-walk task is still unfiled. Shipping a button titled
  "carries the design to a programmed board" onto a path whose success case has never once
  been observed is the kind of claim this repository elsewhere goes out of its way not to
  make (see the README's careful scoping of what checksums vs. attestations guarantee).
  The hardware walk must precede the button, not trail it. #636's `ordering_after: [264]`
  already says so; the title does not.

## Concrete recommendation

Split #636 into three, in this order:

1. **`BoardExport` extraction** (no GUI). Lift `JLSStart.java:386-475` into
   `jls.hdl.board.BoardExport`, diagnostics returned not printed, two-file transaction,
   CLI rewired to call it. Pure refactor, existing goldens and `CliBoardExportTest` are
   the regression net. This is the task with the highest ratio of value to risk in the
   whole #597 slice.
2. **Bundle emission from the dialog.** One action → the handoff directory above, with
   MANIFEST and checksums. Keyboard-reachable, stable names per
   `docs/component-naming.md`, driven headlessly by #91. No subprocess. This delivers the
   student outcome honestly and is what should carry #597 AC-3, reworded.
3. **Board preflight** (new, file against #597 or #522). Schematic-level diagnosis of
   what will not synthesize, before any export runs.

Retitle this issue. "One button carries the design to a programmed board" is a claim the
architecture forbids and the evidence does not support; "one action produces a complete,
self-describing handoff bundle" is a claim that is both true and, for the student, very
nearly as good.
