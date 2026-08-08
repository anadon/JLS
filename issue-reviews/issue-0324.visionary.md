# Issue #324: FEAT-032: a running circuit exchanges bytes with a human or a script through one door granted at invocation, and the exchange replays without the human
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery and the want is one sentence: **a drawn machine's observable should
be a byte stream you can diff.** `riscv/README.md` already says the CPU "is not a special
JLS mode or a plugin: it is an ordinary circuit made of the elements JLS already ships" —
and today that circuit is observed by reading watched pins and memory words. #202, #295
and #301 all end at the same wall: a machine that computes is legible, a machine that
*prints* is not. The console device is the missing observable, and it is squarely on the
project's arc.

The host door is not. It is scaffolding the issue then spends its entire design budget
un-doing: a sealed contract, an enumerated permit set, a cross-thread ring, a drain slot,
a run-mode field in every artefact, and a CI ratchet that refuses goldens produced while
the door was open. Six of the seven §4 invariants exist only because a live door exists.
That is the tell. **The feature is being architected around its least valuable half.**

## Reframing A: transcript-first, file-bound. The stdio door is the last permit, not the first.

Build `Console` as a device element whose bytes arrive from and depart to *files named on
the command line*, using the pattern that already ships: `BatchSimulator.addTestGen`
(`src/jls/sim/BatchSimulator.java:190`) constructs a pseudo-element, `TestGen.setFile`
hands it a path, and `TestGen.initSim` (`src/jls/elem/TestGen.java`) opens a
`FileInputStream` on the simulation thread before the loop starts. Note what this does to
the issue's opening claim: `git grep System.in` returning zero is true, but JLS already
has a read-side host door — an element that reads a CLI-named host file, uncontroversially,
because *the circuit file cannot name it*. The grant model the issue wants to invent is
the one `-t` and `-s` have used since before the fork.

What this buys, concretely:

- No ring buffer, no foreign thread, no `beforeEvent` drain call. §4 invariant 3 —
  described in §3 as "the whole feature" — becomes vacuous rather than tested.
- No permit enum, no sealing argument, no `jls.io` package. §4 invariant 1 holds because
  the element layer *has no type that can name a host resource* (see Reframing C).
- No live/replay duality, so no run-mode field and no criterion-6 CI ratchet.
- **None of `blocked_by: [316, 330, 354]` gates it.** #354 gates only a live interactive
  session; #316 only the pane; #330 only the catalog row. The issue says as much in prose
  ("the headless port and the transcript do not wait on it") and then keeps all three
  edges, which schedules the door long after the capstones that need it.

The live keyboard becomes what it should be: a later, optional affordance whose only
addition is a byte source that appends instead of reading ahead.

## Reframing B: the byte stream is a signal, not a fourth stamped-event format

§3 defines a transcript as $(t_i, d_i, b_i)$ with $t_i$ in simulated time, canonical text
form, deterministic field order, no wall-clock header — and open question 2 asks whether
it should be text or binary. JLS already has two normative stamped-event formats with
exactly those properties: the `-t` grammar (`docs/batch-interface.md` §2; a signal's
stamped value sequence, parsed by `SigSim.initSim`) and VCD export (`-vcd`, byte-for-byte
deterministic, pinned by `VcdExportGoldenTest`). Both are documented **stability
contracts**. A third is duplication of the project's own work.

Make the console's tx/rx bytes `Watchable` (`src/jls/elem/Watchable.java`) instead. Then:

- The output half is a VCD trace and a watched-element report line — existing goldens,
  existing tooling, `examples/autograde/autograde.py` consumes it unchanged.
- The input half is a `-t` file. A byte stream at a pin is already expressible today:
  `rxdata 0x48 for 100 0x65 ...`. What's missing is not a transport, it's the *handshake* —
  the ready bit — which is exactly the element TASK-0068 builds.
- §1 criterion 5 ("replay a transcript with nobody attached, output identical") stops being
  a new test and becomes the property `BatchSimulationGoldenTest` already asserts for every
  circuit in the suite.

## Reframing C: absence of vocabulary beats an enumerated permit set

D7's instinct is right and its mechanism is weaker than it needs to be. A sealed class with
a permit enum is a list that grows: open question 1 already asks to pre-reserve two permits
"because retrofitting a second grant model is not cheap," which is the smell of a mechanism
that knows it will be widened. Hand the element a `ByteSource`/`ByteSink` with no path, no
open, no close — no vocabulary for naming a host resource at all. Binding happens in
`BatchSimulator`, above the element layer. Then invariant 1 is enforced the way #77's
headless rule already is: by `ArchitectureRulesTest`'s bytecode-level package rules
(`test/jls/ArchitectureRulesTest.java:249` is the model), not by a new ratchet in a new
idiom. An absent type cannot be widened by adding a constant.

This also dissolves open question 1 rather than answering it. A block device is a
file-bound `Memory`; a framebuffer is a `Display` that writes frames. Both ride "the CLI
binds a named file to a named element," which is the rule `-s` and `-t` already follow.
There is no second grant model to fear because there is no first one.

## A conflict the issue does not name

`docs/batch-interface.md` §1: "stdout carries *only* the simulation results, so it can be
piped and diffed." That is a versioned compatibility promise — changing it requires a
CHANGELOG entry and a major bump or a compat flag. `-serial stdio`, the model D7 quotes
verbatim, interleaves guest bytes into that stream. §3 lists the CLI flag table,
`docs/extension-points.md` and `docs/vcd-interop.md` as the documents this feature amends;
`batch-interface.md`, the one document that actually constrains it, is absent. Under the
file-bound framing the conflict never arises and stdout stays pure. If stdio is later added
as a permit, it needs its own §1 amendment, not a vcd-interop footnote.

## The out-of-the-box option: draw the UART

`riscv/README.md`'s pride is that the CPU is not a special mode. The pedagogy of a UART is
the handshake — status bit, busy-wait, holding register — and burying it in a Java element
buries the lesson. Alternative: ship **one** new primitive (a byte-wide pin bound to a
`ByteSource`/`ByteSink`) and build the console as `machines/uart.jls`, a drawn subcircuit
of shift register plus status flip-flop that students can open and modify. §2 rejects the
subcircuit shape, but on the narrow ground that FEAT-031's per-instance fidelity toggle
wouldn't reach an element — it never weighs the pedagogical case, which is the stronger
one. Cost is real (per-byte simulation cost, and the primitive is needed either way), so
this is an option to price, not an obvious win. Worth pricing before TASK-0068 is filed,
because the two shapes are not convertible afterwards.

## What survives unchanged, and what I am disregarding

Keep verbatim: polled three-address decode with **no interrupt as a tested property**
(right call — the polling loop is the lesson); no wall-clock stamps anywhere (already the
shipped VCD writer's decision, so the ratchet is a generalization rather than an invention);
TASK-0084's monitor program in the roster (a seam only its author can drive is not
integrated).

Explicitly disregarding: §1 criterion 6 and §4 invariants 3, 5 and 6, plus the §2
sealing/permit apparatus and open question 1. They are consequences of the door-first
framing, not of the capability, and they carry the 10-16 mw band and all three ordering
edges with them. The four-task cut (seam, device, record, consumer) is sound; what changes
is that TASK-0067 shrinks from a sealed package with a permit set to two interfaces and a
binding in `BatchSimulator`, and TASK-0069's transcript half largely disappears into the
VCD and `-t` formats that already ship. What remains is buildable now, against no blockers,
and gives #295 and #301 their observable this cycle rather than after three prerequisite
features land.
