# Issue #61: HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stated purpose: coursework Verilog becomes an editable JLS circuit, or a teachable
refusal. That is the surface. The deeper thing #61 is doing — visible only when you
read it against `docs/capability-roadmap/sweep-03-elements-and-hdl.md` — is
**publishing JLS's element vocabulary as a contract with the outside world**.

The evidence is that every refusal in the import path is an element that does not
exist, not a translation that was not written. `src/jls/hdl/yosys/CellValidator.java`
holds four hand-written apologies (async reset, set/reset, `$mul/$div/$mod/$pow`,
clocked/multi-port memory); each is a sentence telling a student to rewrite ordinary
Verilog because JLS lacks a pin or an operator. `src/jls/hdl/imp/NetlistImporter.java`
adds four more of the same shape — width mismatch needs an `Extend` (`:279-285`,
`:311-319`), a bit slice needs a Splitter/Binder mesh (`:736-741`), a `z` bit needs
TriState (`:722-728`), hierarchy needs an instance (`:156-159`, `:227-233`). The sweep
says it plainly: *"the importer's reject list is a mirror of the element set."*

So #61 is not a pipeline feature that happens to hit element gaps. It is an
element-vocabulary program wearing a pipeline costume. That matters because the issue's
decomposition — runner, techmap, mapper, UI, parity — schedules the costume and leaves
the vocabulary (C2 register control pins, C6 width conversion + bit mesh, C5 hierarchy
instance) as things "later increments add" or as open questions waiting on corpus data.

## Where the trajectory already agrees, and where the issue lags it

Agreement, and it is strong: no in-house Verilog parser (research report §2, Lööw);
external tools on the subprocess boundary (ARCHITECTURE.md's #222 decision names Yosys,
GHDL/Icarus and ELK explicitly); one simulation strategy (#221). #61 does not pull
against the arc — it is the arc's inbound half. Endorse that without reservation.

Where the body has fallen behind reality: `blocked_by: [60, 62]` still carries #62, but
`jls.hdl.layout` is on master and `NetlistImporter` already imports and calls
`HeuristicLayeredLayouter` (`:13-15`, `:104`). The layouter interface is consumed, not
awaited. The #61 ↔ #62 reciprocal the absorb comment escalates is an artifact of cutting
the seam at "feature", not at "vocabulary vs. geometry"; with the reframing below there
is no reciprocal to adjudicate — the mapper produces a `LayoutGraph`, #62 places it,
and the edge is one-way forever.

## Reframing 1 — make the cell↔element correspondence the artifact, not a test over three copies

The absorbed #320 material states the invariant $V \setminus (R \cup X) = \emptyset$ and
proposes a cross-check test that breaks the build when $V$ grows without $R \cup X$.
That is the right instinct aimed one level too low. The correspondence between Yosys
cells and JLS elements currently exists **four** times, hand-maintained:

1. `CellValidator.SUPPORTED` — a `HashSet` of 19 strings (`:57-68`);
2. `NetlistImporter.mapCell`'s switch — 5 realized arms (`:234-259`);
3. `test/resources/hdl/jls_map.v` — the techmap rules that legalize into (1);
4. `HdlExporter.EXPORTED` — 22 element classes (`:422-428`), the export-side mirror,
   which #321's writer is separately forbidden from forking.

Four copies of one fact, reconciled by review discipline and, in the proposal, by one
ratchet test. The elegant route is a single declarative table — cell type, element save
type, port mapping, arity/width rule, and either a realization or an exclusion with its
reason and owning issue — from which the validator's accepted set, the mapper's
dispatch, the teachable reject prose, the techmap coverage list, and the export-side
emitted vocabulary are all **derived**. Then $V \setminus (R \cup X) = \emptyset$ is
true by construction and the cross-check test becomes a tautology rather than a
tripwire, and "the vocabulary must not fork with #321" stops being a promise two issues
make to each other.

This is not a novel invention here; it is the project's own recorded idiom. #223's
extension-point catalog is exactly this shape — typed constants cross-checked against a
documented table *in both directions* by `ExtensionPointCatalogTest` — and #61's own
open question 3 gropes toward it ("a source constant cross-checked against a documented
table, mirroring the shipped `ExtensionPointCatalogTest` idiom"). Promote that from an
open question to the feature's spine. It also collapses the promoted-`Builder`
sequencing trap (open question 2): the table, not the builder, is what a second
importer needs to share.

## Reframing 2 — the primary corpus is JLS's own output, not solicited coursework

#61's largest unresolved evidence risk is written into it twice: open question 3
("corpus representativeness — solicit real assignments") and the migrated criterion 5
("which published open-source core?", explicitly blocking integration). Both are asking
outsiders for the oracle. JLS does not need to ask.

`sweep-03` §C9 records the thing no other surveyed tool can claim: **JLS owns both the
emitter and the importer.** `export → yosys → import → save`, asserted equal to the
original modulo element ids, is a golden-testable property, and the corpus for it
already exists in the tree — `AllElementsRoundTripTest`'s fixtures, the batch goldens,
and the `riscv/` RV32I CPU. `HdlExporter.EXPORTED` already covers gates, `Mux`,
`Adder`, `Register`, `Splitter`/`Binder`, `Decoder`, `TriState`, `Extend`, `Constant`
and pins — a closed subset large enough to start today, and each element added to it
grows the import corpus for free, in both directions.

Adopt this as the feature's evidence engine and three things happen at once. P1 stops
waiting on donated assignments. Criterion 5's "pick a core" question dissolves — the
flagship subject is `riscv/`, which the project already verifies against an independent
emulator. And the reject list and the export policy are forced to converge instead of
drifting apart, which is the failure mode the #321 no-fork constraint exists to police.
The honest claim — *"JLS-exported HDL re-imports to the circuit it came from, and CI
proves it"* — is also a better headline than "coursework Verilog imports", because it is
one the project can keep.

## Reframing 3 — total-or-nothing is the most expensive line in the design

Invariant 4 ("rejection is total or import is total; no half-imported circuits, ever")
reads as rigor. Its actual effect is that 19 validator-accepted cells sit behind 5
realized ones and *nothing is usable until the mapper is complete* — which is precisely
why this issue has been open thirteen months with a landed front end and no user-visible
capability. The rule optimizes against silent wrongness, a real hazard, using the
bluntest available instrument.

The out-of-the-box alternative the issue never considers is sitting in its own sibling.
#63 is building a **black-box HDL component element** whose behavior runs in an external
iverilog/GHDL subprocess. Compose them: any cell or module the mapper cannot realize
imports as a black box with its ports drawn and its semantics delegated, and the design
always imports. Realization then becomes *progressive refinement* — each cell family
JLS learns converts a black box into drawn elements, and a user can ask to explode one.

This does not violate D10's "realization without semantics is forbidden": that rule
forbids *faking* a tri-state cell over a two-state domain, and delegation to iverilog is
strictly more honest than either faking it or refusing the whole design. It matches
#222's recorded architecture (external tools are subprocesses, and stay there). It
retires the `$adff` question without corpus data — an async-reset flop is a black box
until `Register` grows a `CLR` pin. And it changes what "reject" means from *a wall* to
*a performance and editability note*, which is a far better teaching surface than a
rewrite instruction.

The cost is real and should be stated: a black box is not editable, so the pedagogy
claim weakens for any design that leans on them, and it makes JLS's import quality
depend on tool availability. If that trade is refused, then at minimum stop letting
totality gate *usability* — ship the mesh first (sweep-03 calls C6's mesh synthesis "the
single highest-leverage importer task remaining", and it depends on nothing else here).

## Acceptance criteria I am explicitly disregarding

- **"The `$adff` decision is closed on recorded corpus evidence, whichever way it goes."**
  This is ritual. C2 already answers it on stronger grounds than a corpus could: the
  async family is 10 of the 15 entries in `buildTeachable`, and
  `always_ff @(posedge clk or negedge rst_n)` is the most common sequential idiom in
  every textbook. Grow `Register` with optional `CLR`/`PRE`/`EN` pins in the opt-in,
  byte-identical-re-save style #199 established for `Memory.syncWrite`, and delete ten
  apologies. Do not spend a corpus to learn this.
- **`blocked_by: [62]`** — stale; the layouter is consumed on master.
- **Criterion 5's unnamed open-source core as an integration gate** — replaced by the
  round-trip property over the in-tree corpus plus `riscv/`. Keep an external core as a
  stretch demonstration, not as the thing that decides whether the feature closes.

## What I would keep untouched

The refusal to own a Verilog parser or simulator. The pinned pass pipeline with no
`flatten` (`hierarchy -auto-top` is why hierarchy *arrives* and TASK-0048 is a refusal
to lift rather than a capability to invent). Teachable rejects as a teaching surface.
The 2-state honesty rule. Yosys version recorded in the import report. These are the
parts of the issue that are load-bearing for the project's whole arc.

## Verdict

**endorse-with-reframing.** The direction is right and uniquely well-matched to this
project. But the issue's centre of gravity is misplaced: it schedules a pipeline and
defers the element vocabulary that is the actual capability, it seeks its oracle
outside when it owns the only closed loop in the surveyed field, and its totality rule
converts a long feature into an all-or-nothing one. One derived correspondence table,
round-trip-of-own-output as the evidence engine, and black-box degradation instead of
total refusal would make most of the remaining roster smaller, and the parts that
survive would each be shippable alone.
