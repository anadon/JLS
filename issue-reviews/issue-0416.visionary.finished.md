# Issue #416: TASK-0052: a second board exists with both halves, and the first board's bitstream path has actually been walked on hardware and written down
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two claims are bundled here, and they have nothing in common except the word
"board".

1. **The format dimension has never been exercised.** `Boards.java:81` holds
   `List.of(ICESTICK)` and `Board.java:41` holds one enum constant, so every
   generality in `jls.hdl.board` is currently unfalsifiable. True, and worth
   fixing.
2. **No one has ever taken a JLS circuit to a physical board.** The CI leg is
   stubbed tools asserting control flow; `docs/icestick-bitstream-handoff.md:113-119`
   is a table of `_TBD_`. Also true, and the more important of the two — it is
   the difference between a claimed capability and an observed one.

The end state is right and I endorse it. The *route* is where I part company,
because the repository already contains a written, costed, eight-step design for
exactly this work — `docs/standards-adoption/06-fpga-constraint-formats.md` — and
this issue never cites it, contradicts its central instruction, and inverts its
recommended slice order.

## Reframing 1 — the seam is the binder, not a second emitter class

The issue's §7.4 specifies `LpfEmitter.emit(HdlModel, Board, PinBindings)`
"mirroring `PcfEmitter`'s signature and contracts". Mirroring means copying. What
gets copied is `PcfEmitter.java:66-119` — the port walk, the four error cases, the
`diagnoseLeftover` taxonomy, the exact message strings that `UnbindablePortsTest`
pins. That is the all-or-nothing binding contract, and it is the one thing in this
package that must never differ between formats.

The in-tree design study says so in as many words (Step 0):

> **`PcfEmitter` must be generalized before a second format is added**; adding XDC
> by copy-paste would fork the all-or-nothing validation logic, which is the part
> that must never diverge between formats.

Its Step 1 and 2 give the cut: extract `PinBinder.bind(model, board, bindings) ->
List<BoundPin>` verbatim (guarded by `PcfGoldenTest` and `UnbindablePortsTest`
passing *unmodified*), then a `ConstraintEmitter` interface plus a
`ConstraintEmitters.forFormat` switch. After that, a format emitter is ~60 lines
of rendering with no validation in it at all, and there is exactly one place a
binding rule can live.

This also restores the design intent already written on `Board.java:12-14` —
"a board is deliberately just data ... so adding a board is adding a table entry
in `Boards`, never new code." Under the issue as written, adding a board *is*
new code, and adding the fourth board is a fourth fork of the binder.

The study's recommended slice order is `(1) PinBinder/ConstraintEmitter refactor
with PCF bytes unchanged; (2) LPF + ULX3S`. This issue is slice (2) with slice (1)
skipped. Slice (1) is one maintainer-day by that document's own sizing, it moves
no bytes, and it is on the critical path of #647 as well — if the Basys-3 verdict
comes back *supported*, XDC lands as a table entry and 60 lines instead of a third
fork.

## Reframing 2 — H1 is refuted by construction; widen `Board` deliberately

H1 says the `Board` record needs no change. It does. An ECP5 LPF is not
`set_io name loc`; it is a `LOCATE COMP` plus an `IOBUF PORT ... IO_TYPE=` per
port, plus a `FREQUENCY PORT` on the board oscillator. `Map<String, String>` can
carry neither an I/O standard nor a clock frequency. So the executor has two
outcomes: hardcode `LVCMOS33` and no clock constraint for every ULX3S pin (a
statement that is accidentally true for one board and false the first time a
1.8 V bank appears), or trip §10's "H1 refuted → record the deviation on #359 and
stop".

An issue whose central hypothesis is knowably false before execution, and whose
falsification path is "file a comment", is spending a review cycle to discover
something the tree already knows. The study's Step 3 has the answer designed:
`record Pin(String location, @Nullable IoStandard io, @Nullable Long clockHz,
Pull pull, @Nullable Integer drive)` with a board-level default, plus an
`IoStandard` enum that spells the *same* electrical standard per vendor
(`LVCMOS33` / `"3.3-V LVTTL"` / `LVCMOS33`) — which is the only way vendor syntax
stays inside vendor emitters. Its Step 5 clock rule is the genuinely subtle call
and is already decided: never derive a period from the JLS `Clock` element's
simulation units; emit a timing constraint only from the *pin's* declared
`clockHz`, and warn when a clock port lands on a pin without one.

None of this is a board-description file format, so #213 H2 is untouched: the
table is still Java constants grown on demand, unlike the 29-XML-file board
databases `docs/hdl-support-research.md:173` records from Digital and
Logisim-evolution.

## Reframing 3 — the flash record should be a claim on the board, not a gate on the build

`everySupportedBoardHasAFlashRecordRow()` is the part I would change most. Its
only possible failure is "someone forgot to type a row", and its author already
knows this — §7.11 requires the javadoc to say the test asserts presence and
shape, never truth. That is an admirable sentence attached to a mechanism that
does not deserve it. Worse, as a *gate* it says: you may not add a board you do
not personally own. That forecloses #647's second admissible outcome (a documented
recipe rather than a `Boards` entry) and it makes the table grow only as fast as
the maintainer's hardware shelf.

Concrete alternative: put the evidence level in the data model.

```java
enum Evidence { EMITTED_ONLY, TOOL_ACCEPTED, FLASHED }   // + doc anchor
```

as a `Board` component. Then:

- the emitted `.pcf`/`.lpf` **header line carries it** — every artifact JLS writes
  states how much has actually been checked about the board it targets;
- `jls -h` and the unknown-board message carry it, so a student choosing a board
  sees the claim before they wire anything;
- one test asserts each entry's declared level matches `docs/board-flash-record.md`
  — the same mechanical check the issue wants, but the failure mode becomes *a
  false claim* rather than *a missing row*, and adding an unflashed board stays
  legal and honest.

This is the convention the repo already practices elsewhere: the constraint-format
study's "emitted but not yet accepted by the vendor tool" marking, the
once-per-release `docs/wayland-desktop-checklist.md`, and README's careful
sentence about exactly what a checksum proves versus what an attestation proves.
Encode honesty in the artifact; do not enforce it with a counter.

## Reframing 4 — spend the evidence budget on nextpnr, not on a second stub script

P8 buys a second stubbed-PATH selftest — 164 more lines of shell asserting
control flow over fake tools, which the issue itself labels as proving nothing
about real tools. Meanwhile nothing in the tree has ever confirmed that the PCF
JLS emits today is even *parseable* by `nextpnr-ice40`. The study's slice (0) —
"arm `nextpnr-ice40` against the *existing* PCF golden — cheapest confidence, no
new surface" — is a single test using machinery that already ships
(`test/jls/hdl/ToolLocator.java`, the `assumeTrue` idiom at
`GhdlCompileTest.java:34-36`, the `test/jls/hdl/yosys/` suite). It can *refute*
the shipped emitter. Nothing in this issue can.

That reorders the whole task: the reason to pick the ULX3S is precisely that its
flow is machine-checkable end to end short of the flash, so a `NextpnrConstraintTest`
gives the second board harder evidence than a hardware walk gives the first, at a
fraction of the logistics. Do that leg, and #386's dependency stops being
bookkeeping and starts paying.

## Reframing 5 — one handoff script, parameterized by the table

Two 157-line scripts differing in four tool names and two device flags, each with
its own selftest, is the horizontal-cut mistake #264 diagnosed on the other axis.
The board facts (`--hx1k --package tq144`, the tool triple) are already in
`Boards`; `scripts/board-handoff.sh <board> design.v design.pcf` with a per-board
block, and one table-driven selftest, makes "adding a board needs no new script"
true — the shell counterpart of the totality this issue wants in Java, and the
counterpart of #632's "adding a board needs no GUI change". P9's doc move then
follows the code instead of leading it.

## What I am disregarding, and what I would keep

Disregarded: **H1** (widen `Board` deliberately, as designed); **P7's mechanism**
(note also that only switch *expressions* and pattern switches must be exhaustive
in Java — an arrow-form statement over an enum with no `default` is not a compile
error, so the demonstration must use an expression or a registry, or P7 quietly
proves nothing); **P8's second stub script**; **P3 as a build gate**.

Kept and endorsed: the second board with a second format; the ULX3S choice; the
byte-stable golden per (board, format); the aggregated-bind contract as the thing
that must not fork; §7.11's sentence about presence versus truth; and the
recorded hardware walk as a standing document any board owner can add to.

Sequenced as: **(a)** `nextpnr-ice40` against the shipped PCF golden; **(b)** the
binder/emitter/`Pin` refactor with PCF bytes byte-identical; **(c)** ULX3S entry +
LPF + real `nextpnr-ecp5` acceptance; **(d)** the flash record and the evidence
level in `Board`, as a document and a claim rather than a gate. (a) and (b) are
independently shippable and refutable today; only (d) needs hardware and a human,
and it should stop blocking anything.
