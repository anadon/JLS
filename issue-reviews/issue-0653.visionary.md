# Issue #653: TASK-C565-2: a minimized table becomes a two-level netlist built only from elements already in the palette
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The direction is right and belongs in JLS's arc: CAP-31 (#515) names analysis/synthesis
parity as the instructor-conversion bar, and table -> gates is the half JLS genuinely
lacks. But note what the payload actually is. A student who wants a *working* circuit for
a table already has one: `jls.elem.TruthTable` is a registered element type
(`src/jls/elem/ElementRegistry.java:75`) that draws, simulates, saves and exports to
Verilog. Nothing about function is missing. What is missing is **visible structure** — the
AND plane, the OR plane, the inverters, laid out so the student can read their own
specification as gates and then edit it. Synthesis here is a *teaching* artifact, not a
correctness artifact. Every design choice below follows from taking that seriously.

## Finding A: the netlist builder this task proposes already ships

`src/jls/hdl/imp/NetlistImporter.java` (33 KB) is a complete, headless, palette-only
netlist-to-JLS-circuit constructor. Its private `Builder` (`:410`) already does, verbatim,
what #653 lists as its deliverable:

- instantiates palette elements only, by save tag: `addGate(saveType, bits, numInputs)`
  (`:577`), `addInputPin` (`:538`), `addOutputPin` (`:557`), `addMux` (`:606`),
  `addConstant` (`:630`) — and `addGate` already emits `int numInputs`, so wide AND/OR
  planes need no new machinery (`Gate.setNumInputs`, `src/jls/elem/Gate.java:455`);
- resolves drivers/readers into edges, rejecting multiply-driven nets (`driver`, `:665`);
- builds a `LayoutGraph`, solves it with `HeuristicLayeredLayouter`, and emits
  `ELEMENT`/`WireEnd` save text anchored at exact port offsets (`emit` `:806`,
  `emitWires` `:838`).

`src/jls/hdl/layout/` — `SchematicLayouter`, `HeuristicLayeredLayouter`,
`LayoutInvariants`, `LayoutMetrics`, `LayoutResult` — is in-tree and tested
(`test/jls/hdl/layout/`). The #62 seam is landed code, not a pending dependency. That
makes #654's "documented fallback placement if it is not yet landed" and #565 AC-3b's
`WAIVED:` branch dead limbs, and it means the layouter's determinism contract
(`SchematicLayouter.layout`: "same graph, same result") already discharges #653 AC-4 for
free — restating determinism as a local criterion invites a second, weaker definition.

Written as it stands, #653 builds a parallel construction path next to this one. That is
the single largest thing wrong with the task, and it is invisible from inside the CAP-31
tree because the CAP-31 family was planned against #62's issue state rather than HEAD.

## Reframing 1 (primary): extract the builder, then merge #653 into #654

Cut the seam at *structural circuit construction*, not at "two-level synthesis":

1. Lift `NetlistImporter.Builder` into a public, AWT-free class — say
   `jls.build.StructuralCircuitBuilder` — with the vocabulary it already has: `addGate`,
   `addPin`, `addConstant`, `connect(source, target)`, `build() -> save text`. Keep it on
   the headless side so `HeadlessCoreRatchetTest` guards it and CAP-31 AC-5 (batch-callable)
   is satisfied by construction rather than by a second headless plumbing effort.
2. `NetlistImporter` becomes front-end #1 (Yosys cells in). SoP synthesis becomes front-end
   #2 (product terms in). Front-end #2 is then roughly a `for` loop over #564's minimized
   terms; the interesting content of #653 shrinks to about a day.
3. The synthesized circuit lands by going through `Circuit.load` on the emitted text — the
   same path undo snapshots already use (ARCHITECTURE.md, "The save/load pipeline"). This
   *proves* #653's boundary note ("nothing marks synthesized elements as special") instead
   of asserting it: an element that arrived through the ordinary loader cannot be special.

Given that, **the structure/geometry split between #653 and #654 is a false seam.** The
shipped pipeline realizes structure and geometry in one pass, because ports carry fixed
pixel offsets and wire ends are emitted at routed coordinates; a "structure only" artifact
would be a `LayoutGraph` nobody else consumes. Merge the two tasks. The saved
coordination is worth more than the illusion of two 1-mw slices.

## Finding B: AC-1 and AC-2 as written are satisfiable by a degenerate implementation

"Simulation matches the table on every row" plus "only existing palette element types"
plus "deterministic" are all satisfied by emitting **one `TruthTable` element** wired to
the declared pins. That passes every stated criterion and delivers nothing. I am
disregarding AC-1/AC-2 as the acceptance bar and proposing structural criteria, because
the pedagogical payload is the only thing here that is not already shipped:

- SC-1: the emitted element multiset contains no `TruthTable`, `Memory`, `Mux` or
  `SubCircuit`; it is drawn from {`AndGate`, `OrGate`, `NotGate`, `InputPin`,
  `OutputPin`, `WireEnd`} only.
- SC-2: the longest input-pin-to-output-pin path is exactly three elements for a
  complemented literal (NOT -> AND -> OR) and two otherwise — that *is* "two-level", and
  it is mechanically checkable on the emitted graph.
- SC-3: element counts match the minimized cover: one AND per distinct product term, one
  OR per output with more than one term, one NOT per distinct complemented input.
- SC-4 (keep as-is, restated positively): the diff of the landing PR touches no line of
  `ElementRegistry.ALL`. That is the honest one-line form of AC-2's registry-totality
  concern (#315), and it needs no new subset test — `PaletteContractTest` and
  `ElementRegistryTest` already hold the totality line.

## Finding C: "the table" is not one concept, and the mismatch will bite at the round trip

`TruthTable.react` (`src/jls/elem/TruthTable.java:1400`) is a **priority** table: rows are
scanned in order, first match wins, input cells may be don't-care (`table[row][col] == 2`),
and on **no matching row the outputs hold their previous value** (`:1431`). An output
don't-care becomes 0 (`:1447`). None of that is a sum-of-products over minterms:

- a priority table with input don't-cares must be canonicalized to a minterm/don't-care set
  before Quine-McCluskey means anything, and later rows are masked by earlier ones;
- an *incomplete* table is not combinational at all — hold-on-no-match is state. Two-level
  gates cannot reproduce it, so synthesis must either refuse an incomplete table by name
  or declare "unmatched rows are don't-cares" as a documented, user-visible semantic
  change. #653 says neither, and its AC-1 ("matches the table on every row") is silent
  precisely on the rows that do not exist — the only rows where the two models differ.

The architectural fix is a canonical currency: a small immutable `TruthTableIR`
(input names, output names, per-output minterm set, per-output don't-care set) that #563's
extractor, the shipped `TruthTable` element, #564's minimizer and this synthesizer all
speak. The priority element becomes a *projection into* that IR, with the projection
carrying the refusal. Without it, #565 AC-1's "round-trips identically" is untrue by
construction, since output don't-cares collapse to 0 on the way out and never come back.

## Reframing 2 (out of the box): "Realize as…", not "synthesize"

Once the builder seam of Reframing 1 exists, two-level SoP is one realization among
several that JLS's existing palette already supports, each a small mapper over the same
IR:

| Realization | Elements | Needs #564? | Teaches |
|---|---|---|---|
| Two-level SoP | And/Or/Not | yes | minimization, the AND-OR plane |
| NAND-NAND | Nand | yes | De Morgan, real gate libraries |
| Decoder + OR | `Decoder`, `OrGate` | **no** | canonical minterms, decoders |
| Shannon mux tree | `Mux` | **no** | recursive decomposition, BDDs |
| ROM | `Memory` with initial contents | **no** | table-as-memory, lookup vs logic |

Three of those need no minimizer at all. That matters for sequencing: CAP-31's demo slice
currently waits on #563 -> #564 -> #653 before anything is visible, but "Realize as
Decoder + OR" is buildable the moment the builder is extracted and gives a student the
side-by-side comparison — 8 gates vs 3 — that makes minimization *mean* something. The
comparison is the lesson; a single blessed netlist is only an answer. I would ship the
zero-minimizer realization first as CAP-31's demo slice and let #564 land into a seam that
already has a consumer.

## Reframing 3 (the larger arc): this is element expansion, not table synthesis

#653's stated constraint — no new element type for synthesis — is right, and its true
justification is stronger than the registry-totality argument it gives. The generalization
is: **any composite element should be able to expand into an equivalent gate-level
subcircuit.** `Adder`, `Decoder`, `Mux`, `Register`, `ShiftRegister` are all black boxes a
student is eventually asked to build from gates; "Expand to gates" as a right-click
operation, over the same `StructuralCircuitBuilder`, is the same lesson repeated across
the palette, and it is exactly the "open the box" move that separates a teaching simulator
from a drawing tool. `TruthTable` is then simply the first — and easiest — expansion, not
a bespoke feature. I would record that as the direction even if only the `TruthTable` case
is built now, because it changes where the code goes (a general expansion seam in the
AWT-free core, consumed by both the editor and a batch flag) and keeps #653 from
hard-coding a table-shaped API that the next expansion cannot use.

## What survives unchanged

AC-3 (share product terms across outputs) is real and stays: with the builder seam it is
common-subexpression elimination on the term list plus fan-out at one `WireEnd`, which
`portEnd` (`:900`) already handles — one attached end per port, all readers off it. Keep
the "or record the decision" escape; duplicated planes per output are the textbook drawing
and may be the better default for two or three outputs. And the "ordinary circuit, nothing
locked" boundary note is the best sentence in the issue — Reframing 1 upgrades it from a
promise to a property.
