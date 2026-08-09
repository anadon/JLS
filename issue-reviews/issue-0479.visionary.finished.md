# Issue #479: TASK-0084: a hand-written monitor runs on the drawn ternary machine and prints — with the same transcript byte for byte on the behavioral and the structural binding
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and two distinct things are being asked for, braided into one deliverable:

1. **A demonstration.** `TRIT -5` prints `-++`, and a student sees a number system with no sign
   bit. The issue says so itself: "That is the demonstration, and it must be exact."
2. **An infrastructure proof.** Guest bytes cross the console door deterministically — same stream
   on two CPU bindings, unchanged when the clock period moves.

Everything else — the reset vector, the trap dispatch, the ten-entry `ECALL` table, the `.TX3`
loader, `PEEK`/`POKE`/`GO`, "QDOS" — is a CP/M costume worn by those two claims. The costume is
where nearly all the cost is, and it serves neither claim well. That is the finding.

## The proof is in the seam, not the guest

#345/#459 already require the drawn machine to agree with the reference emulator **per retired
instruction**. If that holds, then for any program the guest-visible byte streams are equal too —
H1/P4 is a corollary of the parent feature's comparator, not independent evidence. The one thing
per-retirement CPU parity does *not* cover is the part outside the CPU boundary: the console
element's polled status path, the drain point at the loop boundary a pause cannot skip (#424), and
the retirement-indexed replay log (#455). **That seam is the entire novel content of this task**,
and a monitor is a poor instrument for probing it, because when the transcript diverges at byte
1,142 of a `HELP`/`MEM`/`TRIT`/`POKE`/`GO` session, the golden tells you nothing about which of the
three mechanisms broke. Section 10's answer — "the divergence is located by retirement index" —
concedes the point: the golden is not the diagnostic; #423's comparator is, and it does not need
QDOS to run.

Note also that the normative base this task builds against does not exist in the tree:
`docs/parity-contract.md` and `docs/virtual-hardware-parity.md` are absent from `docs/` at HEAD
(§1 instructs "do not restate those documents"). Four unbuilt blockers, two unwritten specs, and the
acceptance object is a byte-exact golden of a session that will be scripted by hand. This is the
fourth storey of a building whose foundation is still a drawing.

## Reframe 1 — ship the pedagogy at HEAD, this month, with none of the blockers

The headline demonstration does not need a monitor, an ISA, an assembler, a console element or a
drawn CPU. It needs JLS to be able to *render a value in balanced ternary*. JLS already has exactly
the seam for that: `src/jls/elem/Display.java:31` carries a radix of 2, 10 or 16;
`src/jls/BitSetUtils.java:83` (`ToString(bs, radix)`), `:103` (`ToStringSigned`) and `:237`
(`toDisplay`, the `0x… (n unsigned, n signed)` string behind trace, stdout and probes) are the three
places any new rendering lands.

Add balanced ternary as a fourth rendering over the two-bits-per-trit encoding (`src` is two-state
plus HiZ — `docs/simulation-semantics.md:42` — so a trit is an encoding no matter what #345
decides, exactly as `issue-reviews/issue-0345.visionary.md` argues). Then:

- Every student who probes a wire, watches an element in batch (`-t`), or opens a trace can see
  `-++`. Reach is the whole tool, not the population who boots QDOS.
- The demonstration circuit is a `TruthTable` + `Display` on the canvas today, with zero of the four
  `blocked_by` issues and zero new element classes.
- §7.10 stage 1's derivation — including the non-negative-remainder trap, which is the one real
  implementation hazard in this issue — gets its table-driven test (P5) in Java, where it is a
  30-second unit test rather than a structural run at 0.18 s per character.

This is not a substitute for the machine programme. It is the observation that **the issue's own
stated headline is separable from its capstone, and separating it de-risks the capstone**: if
FEAT-039 slips a year, the pedagogy shipped anyway; if it lands, the monitor's `TRIT` built-in is
then a thin caller of a rendering the tree already agrees on, rather than an independent
reimplementation in assembly that can disagree with it.

## Reframe 2 — re-cut the acceptance object: a ladder of micro-transcripts, proved first on RV32I

Replace the one big session golden with a graded corpus, each entry a program of a few dozen
instructions, each isolating one mechanism:

| # | Program | What a failure means |
|---|---|---|
| 1 | write one byte, halt | the door exists and drains |
| 2 | write 4 KiB unbroken | transmit-while-busy / status polling |
| 3 | echo N bytes from the replay log | retirement-indexed input, and P8's bound |
| 4 | trap on a bad address, return, write a byte | trap dispatch survives the seam |
| 5 | print the balanced-ternary table | H5, against Reframe 1's Java rendering as oracle |

Every one of these is byte-comparable on both bindings, runs in seconds structurally rather than
minutes, and — decisively — **names its own failure**. Open Question 3's recommended default (one
session covering all six built-ins) optimizes the wrong variable: it maximizes gate cost, which
scales with how chatty `HELP` is, and minimizes diagnosability. At ~0.18 s/char a 1,500-character
session is ~4.5 minutes structural, doubled by P9's second clock period, and every future prose edit
to the `HELP` text re-prices the CI lane.

Second half of this reframe: **the seam claim is machine-independent, so prove it on the machine
that already exists.** `riscv/` holds a drawn single-cycle RV32I core (`riscv/gui/cpu.jls`), a
reference emulator (`riscv_ref.py`), differential fuzzing (`fuzz_diff.py`) and an assembly→`.jls`
pipeline — the very pattern FEAT-039 is rebuilding in-jar for a novel ISA.
`docs/grand-architecture.md:50` names that RV32I work as one of the three funded trajectories.
Programs 1-4 above run identically on RV32I, today, against #454's console the moment it lands, and
they answer the counterparty problem (T4) that ternary cannot: spike, GCC and a hundred RV32
emulators are available as third opinions. Landing the console/replay determinism proof on RV32I
first means #454, #455 and #425 are validated **before** the ternary capstone depends on them,
instead of being validated by it.

## Reframe 3 — the monitor is a demo artifact, not a gate; and do not freeze an ABI

If QDOS is built (and it is a genuinely charming thing to have), let it be a demonstration in
`examples/` and the documentation, exercised by a smoke test that asserts "reaches the prompt and
echoes", not a byte-exact golden of six built-ins on two bindings. The full recorded session belongs
in the docs as a transcript a human can read, not in `test/resources` as a tripwire that fires on
every wording change.

And §7.12's claim — "the `ECALL` numbering is a contract from the moment the golden exists" — should
be inverted. Freezing a syscall ABI for an ISA invented in this repository, with exactly one program
in existence and no second consumer, buys nothing and costs the freedom to renumber when the second
program shows what the table got wrong. Declare it explicitly **unstable until a second `.TX3`
exists**; keep P6's generate-from-one-declaration mechanism (which is good and cheap), drop the
contract language.

## Alignment with the project's arc

- **Strengthens:** the determinism discipline. H2/P9 (the transcript is a function of retirement
  index, not simulated time) is the best idea in the issue and is genuinely aligned with what JLS
  already is — a tool whose batch surface is a stability contract and whose goldens are reproducible.
  It is misplaced by one layer: it is a property of #425/#455's harness and should be gated there,
  on program 1 above, not discovered through a 2,000-line guest.
- **Duplicates:** the drawn-CPU-plus-reference-emulator-plus-programs pattern that `riscv/` already
  implements, and (at P4) the per-retirement comparator that #345/#459 already own.
- **Pulls against:** a new top-level `machines/` tree with its own assemble-to-`.timg` build rule
  inside a build whose load-bearing constraint is *one self-contained jar*
  (`docs/grand-architecture.md` §1). Guest-program build steps in the main Maven reactor are new
  permanent surface for an artifact with one consumer. If the assembler is in-jar (#458), the
  `.timg` can be built by the test that needs it and never enter the build graph at all.
- **Ceremony that does not belong in acceptance criteria:** P10 (banner version single-sourcing) and
  the checklist item requiring the filesystem gap be documented "naming the missing `BlockDevice`
  element and its 3-5 week price". Both are fine as notes; neither is evidence about whether a
  drawn computer prints deterministically.

## Acceptance criteria I am explicitly disregarding, and why

- **P4 as the parity claim** ("this single comparison is the parity claim at this scale"). It is a
  corollary of #459's per-retirement agreement, and its diagnostic value is zero. Replaced by the
  five-program ladder, each byte-compared on both bindings.
- **Open Question 3's recommended default** (one session, all six built-ins). Wrong optimization
  target; the gate should be cheap and specific, the demo rich and untested.
- **§7.12's ECALL-numbering contract.** Premature ABI freeze with no counterparty.
- **P3's scope** (banner + prompt golden). Keep the concept, shrink the subject: program 1's single
  byte is the same claim without the prose dependency.

## What I would keep verbatim

H2 and P9, and their status as a gate run first. H3 (polled, no interrupt path, and "report it
against #454 rather than inventing a device the machine does not have") — that is exactly the right
instinct about seam ownership. H4's rule that no built-in may lie about having storage. P8's bounded
progress with a *measured* N_max rather than a taste-chosen number. §7.10 stage 1's worked
derivation, which should become a doc comment on the Java rendering from Reframe 1 and the authority
for every implementation of it in the tree.

## Verdict

**rethink.** The goal — a drawn machine a person can type at, with a deterministic transcript — is
right and worth the project's time. The design is a monolith where the evidence wants a ladder, it
puts its headline pedagogy behind four unbuilt blockers when that pedagogy is a rendering function
JLS can grow at HEAD, and it re-proves at the byte level what its parent already proves per retired
instruction. Split it: the balanced-ternary rendering now; the seam-determinism corpus on the
existing RV32I core as #454/#455/#425 land; QDOS last, as the demo it deserves to be rather than the
gate it is drafted as.
