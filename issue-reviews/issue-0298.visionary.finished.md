# Issue #298: CAP-05: a drawn circuit leaves JLS as a netlist KiCad imports with zero hand editing, and the board built from it comes back working
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the 56 KB of tier apparatus and CAP-05 is one claim: **JLS's element graph
is already an electrical netlist, and the only thing standing between it and a
physical board is a name for each part's package.** That claim is correct, it is
the cheapest large capability the fork has left, and the issue's own division of
labour — JLS owns electrical content, KiCad owns physical content — is the right
line. Endorse the end without reservation.

It is also a correction the tree needs. `docs/capability-roadmap/sweep-06-physical-boundary.md`
declines PCB rows #140–#146 as "KiCad's domain," and names IPC-D-356A as the one
near-miss, declining it because "a bare-board test netlist without a board layout
has no consumer." That reasoning does not survive contact with this issue: a
*KiCad* netlist has a consumer, and it is KiCad. CAP-05 is squarely sweep-06's
own **category 3 — "being a legitimate front end to somebody else's physical
flow"** — the category the sweep proves is in scope by pointing at
`src/jls/hdl/board/PcfEmitter.java`. The issue is aligned with the arc; it just
never cites the document that already argued its case, and sweep-06 should be
amended by it.

Where I part company is the route, the size, and where the durable asset lives.

## Reframing 1: refdes purity is a self-inflicted requirement, and it costs the biggest row in the roster

AC-3 ("refdes is a pure function of circuit content") exists to protect one
thing: a student re-exports, and the board they already laid out does not
silently renumber. That requirement is what drags **#318 FEAT-014 stable
addressing and per-view geometry (11–17 mw — the single most expensive row in
the set)** into a capstone about footprints, and it drags #334 and #319 along
behind it.

Real EDA does not solve this with purity. **KiCad matches footprints to symbols
by UUID path, not by reference designator** — `(comp (ref U3) (tstamps /uuid/))`.
Refdes is stateful annotation, persisted, and deliberately so. JLS already has
the payload that field wants: `src/jls/elem/ElementId.java`, the permanent
identity shipped in #165, minted fresh and persisted when the file declares it
(`src/jls/elem/Element.java:22-23`). Emit the stable id as `tstamps` and the
board survives *any* renumbering, because KiCad was never matching on refdes.

So the reframe: **stop requiring refdes to be a pure function of content; require
the netlist to carry identity.** AC-3 becomes "re-export after moving every
element changes no `tstamps` value," which is true today with three lines of
emitter code. #318 leaves the required set, taking a third of the standalone cost
with it. #334 and #319 stay valuable — but they are the diff/merge program's
work (`docs/capability-roadmap/lf-06-diff-merge-vcs.md` measured 5,314 diff lines
for one inserted gate, 5,227 of them renumbering churn), and they will be funded
there whether or not anyone ever orders a board. Borrowing them to pad this
roster is what turns a 14–26 mw capability into a 46–78 mw program.

I am explicitly setting aside AC-3 and AC-4 as stated. AC-4 is a `.jls` file
property with no causal path to `Import Netlist…`; it belongs to lf-06.

## Reframing 2: the durable asset is a realization layer, not a package library

Three capstones (#297 breadboard, #298, the now-closed #307) fight over who owns
"the package library," and the issue resolves it by declaring itself owner and
hosting the contract in its own §3. That fight is a symptom: **the contested
thing is more general than any of the three consumers, and none of them is the
right home for it.**

What FEAT-040 actually describes is a *realization*: a mapping from an abstract
JLS element (`Adder`, 32 bits) to a concrete implementation with pins, sections,
loading and a target-specific identifier. The identifier is a footprint string
for PCB, a chip+row for breadboard, a Liberty cell name for sweep-06's change D,
an FPGA primitive for the yosys path. Sweep-06 independently identifies **"D. a
technology-cell layer"** as the unlock for #109, #100, #87, #89 and #111 — and D
is the same data model with a different string column. Cut the seam at
*realization*, and one table serves four flows and every future one. Cut it at
*package*, and #298 owns a 74-series table that change D will have to duplicate.

Two concrete consequences:

- The layer belongs behind a **typed extension point**, alongside
  `elem.element-provider` and `hdl.exporter` in `docs/extension-points.md`, not
  as a bespoke `-parts` reader. The catalog is normative for point ids and
  contracts and already anticipates this shape.
- **The v1 table should be `Boards.java`-shaped.** `src/jls/hdl/board/Boards.java`
  records hypothesis H2 of #213 verbatim: "kept deliberately tiny … the table
  grows on demand rather than through a general board-description format." #349
  proposes the opposite — "extensible with a text file and no Java" — while
  citing #213's emitter as its precedent. One of those is wrong. On the evidence
  of the shipped one, twenty 74-series entries as Java constants get the first
  clean import weeks earlier and defer the format-design argument until a second
  consumer actually exists.

## Reframing 3: the netlist is the bridge; the schematic is the artifact

Open Question 1 offers a false binary — (a) JLS emits a netlist, (b) JLS owns
placement and routing — and picks (a). There is a third option the issue never
considers, and the coverage comment of 2026-08-04 records that its owner
(#307) is closed and the decision now dangles: **emit a KiCad schematic
(`.kicad_sch`), and let KiCad derive the netlist itself.**

The objection recorded on #307 — JLS should "never" ship symbol geometry — does
not apply, because you no more ship symbol geometry than you ship footprint
geometry. `Symbol:74xx:74LS83` is a string into someone else's library, exactly
like `Package_DIP:DIP-16_W7.62mm`. It is the *same* one-string-per-package gate,
paid once. What you get for it: a student opens an editable schematic instead of
a blob pcbnew swallowed; an instructor reviews a drawing instead of an
S-expression; `pcb drc --schematic-parity` (which AC-2 currently lists as
conditional) becomes unconditional; and the netlist stops being JLS's format
problem forever, because KiCad generates it.

I would still ship the netlist first — it is smaller, and it proves the reframe.
But **name the schematic as the terminus in §1 rather than leaving it as an
orphaned open question**, or the netlist emitter will accrete symbol-adjacent
scope that the schematic route would have made free.

## The capability and the demonstration are different things

AC-7 makes closure depend on a $30 fab run and three weeks of shipping. That is
not an acceptance criterion; it is a field report. The falsifiable engineering
claim here is "KiCad imports it with zero errors and the induced partition is
isomorphic to the drawn circuit" — AC-1 and AC-6, both automatable, both cheap.
Everything after `Import Netlist…` is a normal board job that thousands of people
do without JLS, and the issue says so itself.

The precedent is in the tree and it is unambiguous:
`docs/icestick-bitstream-handoff.md` delivers "JLS onto real $30 hardware"
without a capstone at all — an emitter, a handoff script, and a recipe document,
under #215's recorded H2, "delegate, do not reimplement." **CAP-05's shape should
be that document with `kicad` where `nextpnr` is.** Ship the capability; publish
`docs/kicad-handoff.md`; let the built board be a photograph in a release note.
Nothing is lost, and the capability stops being hostage to shipping logistics and
to KC-05-3's fear of a skipped CI lane.

## Where the contract lives

§3 declares that this issue "hosts the one Feature-Level Interface & Data
Contract" for #349/#365/#366. That pulls directly against the project's own
recorded practice: ARCHITECTURE.md states that repo documents "are the normative
home for contracts," and every comparable seam — `docs/extension-points.md`,
`docs/component-naming.md`, `docs/file-format.md`, `docs/batch-interface.md` — is
a file, versioned with the code it constrains, not an issue body. An issue is a
work order; it closes. A contract three capstones cite by section name must
outlive it.

Put the realization layer in `docs/realization-layer.md` with a recorded decision
in ARCHITECTURE.md, and the ownership dispute with #297 and #307 evaporates —
along with the mirror-REPLAN obligations in §3 and §5, which the coverage comment
already found half-collapsed onto a closed issue.

## What survives unchanged

The core sequencing insight is right and worth defending: **the cascade rule
belongs in the IR, not the emitter** (Open Question 4, KC-05-2). Synthesized nets
that exist in no `.jls` file are a genuine ontological event, and an emitter that
invents them is no longer a projection. That is the one place in this issue where
the tier machinery earned its keep. Likewise the sealed-dispatch constraint —
`src/jls/elem/Put.java` is `sealed permits Input, Output`, and #339's `INOUT` work
must extend direction without unsealing it — is a real, checkable invariant.

## Bottom line

Endorse the outcome; endorse the division of labour; endorse the cascade-layer
call. Reframe the route: emit identity instead of legislating refdes purity
(deleting the 11–17 mw row), build a realization layer behind a typed seam
instead of a capstone-owned package library, put the contract in `docs/`, aim at
the schematic, and demote the physical board from acceptance criterion to
recipe document. What remains is #349 + #365 + #366 — the issue's own 5–9 mw demo
slice — which is the entire reframe, proven, before anyone argues about the other
eight features.
