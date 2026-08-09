# Issue #558: FEAT-C29-2: a Digital .dig circuit opens in JLS as a working, laid-out circuit — every element mapped by semantics or refused by name, never silently approximated
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "JLS reads one more file format." Per #510 §3 and §5, `.dig` is the *user-side* half of the
Digital-successor positioning — the half that lets an instructor bring a decade of course material —
while the contributor-side half (dark mode, live subcircuit dive, keybindings, <1-week PR turnaround)
is winnable today at near-zero engineering cost. The end is right and I endorse it: of the four
migration levers in #510 §4, `.dig` is the one whose absence blocks a *positioning claim*, not merely
a user segment. What I want to change is the shape of the work, on four axes.

## Reframing 1 — oracle-first: the boundary with #562 is drawn on the wrong side

Digital `.dig` files carry embedded test cases. Of the four CAP-29 formats, this is the only one that
ships its own per-file, machine-checkable correctness oracle, and CAP-29 already names the standard
(#513 AC-2: "translated vectors pass under `-t` with the same verdicts Digital reports"). #558 pushes
that oracle into #562, *downstream*, and keeps only "parse and preserve" — so the thing that can prove
the importer right is scheduled after the importer it would prove.

The cost of that ordering is not schedule, it is the acceptance instrument. AC-1 ("zero unexplained
losses") is a claim about the *loss report*, and a loss report is structurally blind to the failure
mode that actually ruins an instructor's lab: a circuit where every construct mapped and the wires
landed in the wrong net partition. #323 §3 states the correct invariant precisely —
P(imp(f)) ≅ P_src(f), compared by net count *and* membership, mechanically — and calls silent
disconnection "the worst failure mode available, because the file opens and looks right." #558
inherited #323's report-totality equality and dropped its partition equality. `.dig` connectivity is
geometric in exactly the same way (`<wires>` segments plus port coincidence, port offsets derived from
each element's shape and bit width), so the hazard transfers intact.

Concretely: keep #562 owning the emitted `-t` artifact (its byte-determinism AC-3, its report of
untranslatable Digital test-language constructs), but pull *verdict parity as this importer's
acceptance oracle* into #558. Then AC-1 stops being "a human read a report on one file" and becomes
"the source file's own tests pass," which scales over any corpus for free and answers the
single-file-evidence weakness from a direction that costs nothing extra. Add the partition-equality
assertion as a peer of AC-1. I am explicitly disregarding AC-1 as written: loss-reporting totality
answers "what did not come across" and never answers "is what came across right," and only the second
determines whether the migrated lab still grades.

## Reframing 2 — build the import as ops, not as a builder

The issue inherits `NetlistImporter`'s no-partial-circuit discipline by prose (AC-5), and #323's Open
Question 4 is still "how does `NetlistImporter.Builder` get promoted so a second importer does not fork
it." Both questions dissolve if the importer emits a **`jls.collab.op.CircuitOp` sequence** applied
through `OpSink.submitAll` rather than building a `Circuit` privately or emitting save text.

That machinery is shipped and tested at head — `src/jls/collab/op/` (`AddElements`, `AddWire`,
`SetElementConfig`, `MoveElements`, `ElementBlocks`, `CircuitOpReader`) with `docs/operation-layer.md`
as its contract: validate-then-apply, a rejected op leaves the circuit byte-identical, `invert` against
the pre-apply circuit, one gesture one undo snapshot. That is AC-5 in its entirety, already written,
already golden-tested, already the surface collab and #170 harden. #323 listed the headless op layer
(#337) as merely "beneficial" and rejected the emit-save-text-and-reparse alternative; the layer has
since largely landed, so the seam is available *now* and the "shared importer builder" every CAP-29
sibling wants is the op vocabulary rather than a promoted private class.

The honest caveat: the vocabulary listed above has no subcircuit-definition op. If `.dig` hierarchy
needs one, extending `CircuitOp` is the right deliverable — it serves collab, scripting, #337 and all
four importers at once — and is a far better use of the 4–6 mw than a fourth private construction path.

## Reframing 3 — the mapping table is data, and it can be *total*, because upstream is frozen

#510 records Digital at 3 commits YTD, no release in 23 months, 97.7% single-author. The issue treats
that as strategic context; it is also a *technical* gift the design never collects. A frozen upstream
means the element universe is closed and enumerable, so the mapping need not be an estimate validated
by a corpus — it can be a **complete matrix**, one row per Digital element and attribute key, each with
a disposition (map / refuse-by-name / approximate-with-explanation) and a rationale, shipped as
versioned classpath data with attribution, with the importer as a table interpreter over it.

What that buys: AC-2's "written, reviewable table" becomes the executable artifact rather than a
document that drifts from a switch statement; the accept table is total by construction rather than
sampled; `.cv` and Falstad reuse the interpreter and contribute only data; and — the part that serves
#510 §5 — a stranded Digital contributor can add a mapping row without learning JLS internals, which is
the cheapest possible on-ramp for exactly the developer pool this capstone exists to court. It is also
the same shape #323/TASK-0055 already chose for part data, so it is not a new pattern in the project.

## Reframing 4 — measure the free path before committing 4–6 mw

KC-29-1 names a "documented external-conversion recipe" as this feature's *fallback*. That recipe is
buildable today with zero new code: Digital exports Verilog/VHDL, and JLS already has
`src/jls/hdl/imp/NetlistImporter.java` (Yosys JSON, #61) plus the `-board`/`-pins` flow at head —
which #510 footnote 2 records as functional but wired to no flag or menu ("releasing and surfacing
them is cheap score"). Run that pipeline over real published `.dig` files *first*, as a measurement.
It costs days and it tells you empirically what the bespoke importer actually buys: layout, labels,
hierarchy names, initial memory contents, non-synthesizable pedagogy elements (displays, keyboards,
seven-segments), and the test sections. My expectation is that the gap is large and pedagogically
decisive — which is an argument *for* this issue, made with evidence instead of assertion. This is
#323's own I6 discipline ("the corpus run is the gate on the estimate, not a follow-up") applied to a
4–6 mw commitment that currently carries no measurement at all.

## Where the work pulls against the project's arc

- **XStream is a trap the ACs do not name.** `.dig` is XStream's serialization of Digital's own object
  graph, so the obvious implementation is to depend on XStream and deserialize. XStream's history is
  gadget-chain RCE; adding it to a tool whose safety story is #38 and `UntrustedFileHardeningTest`
  would invert that story. AC-4 says "XXE-proof," which is necessary and not sufficient. It should say:
  parse as *data* with a hardened StAX/DOM reader, never reconstruct a Java object graph reflectively,
  no new deserialization dependency — alongside the entity vectors #612 already enumerates. And since
  `.circ` and `.dig` both need it, one shared hardened-XML reader utility (there is no XML parse in
  `src/` today) is a better artifact than two independently hardened parsers that can diverge later.
- **The license question is asymmetric with #323.** #323 blocks filing children on the incumbent's
  GPLv3-only notice costing JLS its "or later". The semantics and attribute-key names in this issue's
  mapping table come from reading Digital's GPL-3.0-only source — the same shape of question, recorded
  nowhere here. The resolution is probably cheap (derive dispositions from observed behavior, carry
  attribution with the shipped data), but cheap is a decision, not a default.
- **"Laid-out" is the coordinate reading, and that is still real work.** I agree with the dedup
  comment: `.dig` carries coordinates, #62's engine must not be re-owned, and #617 is the right home.
  The visionary point is that the coordinate map should be one pure function from the Digital model
  into `jls.core` (`GridPoint`, `GridSize`, `Bounds`, `Geometry` all exist), validated by partition
  equality rather than by looking at it — because port offsets are shape-derived and getting them
  subtly wrong is precisely how a circuit imports "laid out" and silently disconnected.

## Verdict

**endorse-with-reframing.** Keep the goal; it is the hard dependency of the strongest positioning
claim #510 supports. Restructure the design as: oracle-first (verdict parity and net-partition equality
as acceptance, test translation pulled forward rather than deferred), op-layer-atomic (import as a
`CircuitOp` sequence, so AC-5 is inherited rather than rebuilt), mapping-as-total-data (exploit the
frozen upstream), and measured-before-committed (run the free Verilog/Yosys path first).

One sequencing consequence worth stating plainly: if a first importer is to define the shared report
contract (#556), `.dig` is a better definer than `.circ` — it brings its own oracle, its element set is
closed, and it carries no license-blocked part-data prerequisite or 6–12 mw parent. CAP-29 chose its
demo slice by size (Falstad); size is the wrong criterion when one candidate ships the means of
verifying itself. Running `.dig` ahead of, or in parallel with, #323 — rather than strictly behind it —
is the ordering I would argue for, with #556's contract falling out of whichever lands first.
