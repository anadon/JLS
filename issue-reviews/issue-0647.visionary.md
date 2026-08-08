# Issue #647: TASK-C599-2: the Basys-3 verdict lands as code or as a recipe — a Boards entry with a byte-pinned golden, or the documented path a Basys-3 owner follows instead, with the open-toolchain goldens proven unmoved
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the branch structure away and one sentence remains: *a course that owns
Basys-3 hardware should be able to get a JLS-drawn circuit onto it.* Everything
else in #647 — the golden, the dispatch, the recipe document — is machinery in
service of that. The capstone (#522 AC-4) and the feature (#599) frame it as a
*verdict*: supported, or refused-with-arithmetic. #647 is the consequence-of-the-
verdict task, and it inherits the binary whole.

The binary is the problem. It is an artifact of #264's "both halves" rule, which
was written for open toolchains where JLS can ship the second half as a shell
script. On a vendor board JLS can carry the *first* half completely — the XDC
file, generated from the same `HdlExporter.buildModel` port walk the Verilog
comes from, which is the tedious, transcription-error-prone part a student gets
wrong — and it can carry none of the second, because `git grep ProcessBuilder --
src/` returning nothing is a load-bearing invariant of the whole HDL arc (#215
H2, and AC-5 here is right to guard it). Forced into "supported or refused",
#264's rule makes JLS either overclaim or withhold something genuinely useful.
That is the pull against the project's trajectory, and it is worth naming: JLS's
arc is *emit text and stop* — README's export section, `docs/icestick-bitstream-
handoff.md`'s delegation rule, ARCHITECTURE.md's recorded removals. A
constraints-only board is the most on-arc thing in this whole feature. The
"both halves" rule is a rule about *hardware evidence*, not about architecture,
and #647 applies it as if it were the latter.

## The reframing: make the support level data, not prose

I am explicitly disregarding AC-1/AC-2/AC-3 as branch-structured, and proposing
that both branches collapse into one code path.

```java
public record Board(String name, String fpga, Format format,
        Support support, Map<String, Pin> pins) {
    /** What JLS carries for this board, and therefore what it may claim. */
    public enum Support { OPEN_FLOW, CONSTRAINTS_ONLY }
}
```

- `OPEN_FLOW` — constraints **and** a scripted path **and** a flash-record row.
  #264's rule, unchanged, for icestick and #416's ECP5.
- `CONSTRAINTS_ONLY` — constraints, plus a named external procedure and a
  *dated vendor-acceptance record* row. Never called "supported" anywhere.

Then put a total switch over `Support` (no `default` arm — the same trap #416 H2
builds for `Format`) at the three places that make a claim:

1. `Boards.names()` → the `-board` help text and the unknown-board message
   (`src/jls/hdl/board/Boards.java:116`), rendering `basys3 (constraints only —
   Vivado)`. `test/jls/hdl/board/CliBoardExportTest.java:164` already uses
   `basys3` as its *unknown board* fixture; that test moves, and its movement is
   the smallest possible proof the claim changed.
2. #597's picker label — still `Boards.all()`, still no per-board GUI code.
3. #416's planned `FlashRecordTest`, keyed by support level: `OPEN_FLOW` demands
   a flash row, `CONSTRAINTS_ONLY` demands an acceptance row. Without this key,
   a Basys-3 entry either fails a test it can never satisfy or forces the test
   to be weakened for everyone.

This is what makes the problem disappear. AC-3's "does not imply support
anywhere in the repository" stops being a promise a human keeps by grepping prose
and becomes a compile-plus-test obligation. And the deliverable stops being a
document that will drift from the four other documents already describing this.

## The duplication the issue does not see

The decision #599/#645 exist to produce is substantially **already written**, in
two places #647 never cites:

- `docs/standards-adoption/README.md` row 9: *"XDC, QSF, LPF — do-it-if, 8–10
  maintainer-days, nobody assesses (the vendor tool is the oracle, via a manual
  per-release acceptance record) … Gated on a named board a named user owns."*
  That is the D8 cost arithmetic and the trigger condition, already in
  D8-compliant table form.
- `docs/standards-adoption/06-fpga-constraint-formats.md` is a full
  implementation design: exact XDC bytes for `basys3` with the Artix-7 ordering
  code, why `IOSTANDARD` is not optional (Vivado's `UCIO-1`/`NSTD-1` DRC), the
  `PinBinder` extraction, the `ConstraintEmitter` seam, why Vivado cannot run in
  CI, and the exact refusals (no `-iostd` flag, no per-binding attributes).

A third document restating this is a maintenance liability, not an answer. If
anything lands as prose, it should be a **row added to the board table in
`docs/hdl-support-research.md` §7.5** — the one place a reader looking for boards
actually lands — cross-linking the two above. Note also the searchability
wrinkle #645's AC-1 walks into: the repo spells it `Basys 3` and `basys3`; the
hyphenated `Basys-3` the acceptance criterion demands appears **nowhere** in the
tree. Fix the spelling in one canonical place rather than minting a document to
satisfy a grep.

## Two things the "supported" branch under-specifies

**The `Board` record must change, and the issue says it will not.** AC-1 pins
"the same `NATURAL_PIN_ORDER`, no second ordering" — fine — but `Board.pins` is
`Map<String, String>` (name → location), and XDC needs a per-pin I/O standard or
Vivado errors at bitstream time, and a board-oscillator frequency for
`create_clock`. That is *precisely* #416's H1 falsification condition ("an
attribute like an IO standard that is not a pin name"), and #416's own
recommended next move is: do not widen `Board` silently, record the deviation.
#647 ordering-after #416 means it will inherit either a widened `Board` or a
recorded refutation — and it plans for neither. The design doc's `Pin`/
`IoStandard` value types (with `xdc()`/`qsf()`/`lpf()` spellings kept inside the
emitters, board data staying vendor-neutral) are the right answer and are already
written down.

**A byte-pinned golden is not evidence of support.** The standards doc says it
outright: goldens "prove shape and determinism, nothing about vendor acceptance."
#647's supported branch can land fully green with an XDC file Vivado rejects —
the exact failure mode #359 exists to stop ("an emitter that produces
valid-but-wrong output fails a build"). Under the reframing this is structural:
`OPEN_FLOW` needs a flash record, `CONSTRAINTS_ONLY` needs a dated acceptance
record, and neither is satisfiable by CI. Without such a row, the board is not
in the table at all.

## Sequencing that pulls the wrong way

#647 orders after #416, which is `blocked_by` #386, which needs nextpnr installed
in CI, and #416 itself needs a human with ECP5 hardware to walk a flash. So the
answer to "what can a Basys-3 course do today" is gated on physical hardware for
a *different* board. Meanwhile AC-2 asserts an invariant about a GUI picker that
does not exist (#597 is open and unbuilt), so that criterion is vacuous today —
evidence the ordering is inverted. Under the reframing the honest sequence is:
the `Support` field and the §7.5 row cost well under the 1.5–3 mw band and depend
on nothing; a `basys3` *entry* rides after #597 so its picker clause means
something; and the entry itself fires only when README row 9's gate fires — a
named course with named hardware. Today the ASEE claim is traceable only to
#510's evidence line in `docs/grand-architecture.md`; no named user appears
anywhere in the tree. That gate is the project's own, and it should be honored
rather than routed around by a task that assumes the demand.

## If a Basys-3 entry does land, scope the transcription

iCEstick is 35 pins with one functional-block comment per group. Basys-3's master
XDC is 150+ (16 switches, 16 LEDs, 5 buttons, four 7-seg digits plus anodes, four
Pmods, VGA, USB-HID, clock), and there is no automated defense against a wrong
digit — #416 §7.11 says so and it applies double here, since no CI tool will ever
parse this file. Transcribe only the blocks a first course uses (clk, sw, led,
btn, seg/an, one Pmod), say in the javadoc that the table is deliberately partial
and why, and let it grow on demand — which is #213 H2's rule anyway.

## What to keep

AC-4 (open-toolchain goldens asserted unmoved, not assumed) and AC-5 (per-block
source comments; `ProcessBuilder` grep still empty) are the two criteria that
serve the larger arc rather than the branch, and both survive the reframing
unchanged. Keep them verbatim.
