# Issue #656: TASK-C565-5: synthesis runs headless — a table file in, a circuit file out
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The outcome sentence is the honest one: *an instructor should be able to generate reference
circuits from a build script and diff them like tracked artifacts.* That is squarely on the
project's arc. `docs/grand-architecture.md` §1 records "two co-equal front ends, not one" —
the headless surface is first-class, not a GUI afterthought — and CAP-31 AC-5 makes every
analysis capability headless-callable so graders can use it. Nothing in the goal needs
defending.

The *design* is a single sentence of framing — "a batch flag takes a truth-table file and
writes a laid-out `.jls`" — and that sentence quietly commits JLS to three things it should
not want: a second file format, a second netlist-to-circuit path, and a second CLI verb bolted
onto a contract that is being frozen this quarter. All three already exist in tree in better
form. The band of 1 mw is roughly right; it is right for a *different* task than the one
written.

## The machinery already exists, and the issue does not know it

`src/jls/hdl/imp/NetlistImporter.java` is a headless static entry point
(`importNetlist(YosysNetlist) → ImportResult`), `ImportResult.saveText()` returns circuit save
text, `src/jls/hdl/layout/HeuristicLayeredLayouter.java` and `SchematicLayouter` place it, and
`FileAbstractor.writeCircuit(File, String, Container)` writes it through temp-file/atomic-rename
("the previous complete file survives a crash mid-write"). Neither the importer nor the
layouter imports `java.awt`. That is *literally* "netlist in, laid-out circuit file out,
headless, no partial file" — shipped, tested, and with an extension point already reserved for
it: `docs/extension-points.md` row `hdl.importer`, "cell-map/layout contract to be defined,
pending #61/#62".

So the correct framing of #656 is not "add a synthesis flag to the batch CLI." It is:

> **Truth-table synthesis is the second consumer of the netlist-import path — the one that
> forces `hdl.importer`'s input contract to become a value type instead of a Yosys JSON
> parser.**

Today `YosysNetlist` is only constructible via `parse(String jsonText)`. Cutting the seam one
layer in — a small internal netlist value (cells, ports, nets, bit vectors) that `YosysNetlist.parse`
produces and `NetlistImporter` consumes — makes #653 (netlist construction), #654 (layout),
and #656 (headless) collapse into "emit that value from the minimized cover, call the existing
importer, call the existing writer." That is the reuse story #654 already accepts for layout;
#656 is refusing the same trade one layer up and paying for a private path instead. It also
pays forward: BLIF import (surveyed as "COULD", `docs/standards-landscape.md` #77), Verilog
structural import, and #62's Yosys path all land on one contract rather than three.

A cheap validation oracle falls out of this and is worth stating: the same table synthesized
in-tree, and synthesized via `-export` → yosys → `NetlistImporter`, should produce equivalent
circuits. JLS already runs differential oracles this way for RV32I (`ARCHITECTURE.md` §Recorded
decisions). The yosys leg is a *test-time* oracle only — the shipped path must stay
self-contained per §1.

## The "table file" should not be a new file format

AC-3 says the input is "the same one TASK-C563-4 writes." I am disregarding that criterion as
stated, because it inherits a format that should not be invented at all.

`jls.elem.TruthTable` already exists: a headless model class (the GUI was split out to
`jls/edit/TruthTableEditor.java` under #77), with named inputs and outputs, don't-cares
(cell value `2`), a `save(PrintWriter)` block inside the normative, versioned, spec'd `.jls`
format (`docs/file-format.md`, drift-tested by `FileFormatSpecTest`), and a shipped editor
dialog. `docs/standards-landscape.md` §3 already records that no truth-table interchange
standard exists — which is an argument for reusing the one representation JLS already
specifies, not for minting an unspecified sixteenth.

Reframed: **the table file is a `.jls` file containing a `TruthTable` element**, and synthesis
is a *circuit-to-circuit* transformation — "expand this `TruthTable` element into an equivalent
two-level gate network," the same shape as flattening a `SubCircuit`. What that buys:

- AC-3 becomes true by construction, in both directions, with no converter and no new grammar.
- The format spec, the version-bump policy, the reader/writer conformance targets, the
  canonical-order rules and the compression container all come along free.
- The GUI gets the identical operation for free (right-click a table element → expand), which
  is FEAT-C31-3's real user story; the batch flag becomes a thin wrapper on a model op rather
  than a CLI-shaped feature.
- #655's round trip becomes a comparison of two canonical element blocks, not of two ad-hoc
  table files.
- It makes visible how much of #652 ("a student types and edits a truth table") already ships.
  #652 should be re-scoped to a gap assessment against `TruthTableEditor` (keyboard/a11y,
  bound wording) rather than a new table-entry UI.

One semantic landmine this reframing exposes, and it is load-bearing for the whole family:
`TruthTable.react` leaves outputs **unchanged** when no row matches (issue #52's fix for
`table[-1]`). A partial table therefore *holds state*; a synthesized two-level network drives a
definite 0. So (a) #655 AC-2's don't-care round-trip rule must be decided against this
behavior, not around it, and (b) #872 AC-2's registry-keyed state-holding table — which lists
`Register`, `Memory`, `StateMachine`, `Clock`, `DelayGate`, `SigGen`, `TriState` — must classify
`TruthTable`, because a cone containing one with an incomplete table is not combinational.
That is a cross-issue defect the current framing hides.

## AC-2 is not achievable as written, and the fix is one decision

"Byte-deterministic for a given table, so it can be committed as a golden" fails across
machines today. Canonical save order sorts element blocks by stable id (#166), and freshly
minted stable ids are `replica:counter` where the replica is per-*install* — the
`jls.replicaId` property / `JLS_REPLICA_ID` env var if set, else a persisted random draw
(`docs/file-format.md` §8, `Element.stableId = ElementId.mintFresh()`). Two instructors
synthesizing the same table get different `sid`s, hence different sort order, hence different
save-time ids, hence different bytes. The golden diffs on every machine.

The fix is small and belongs in this task: synthesized elements mint ids under a **reserved
deterministic replica** — exactly the trick already used for legacy loads (replica `legacy`,
numbered in file order) — with counters assigned in a canonical order derived from the table
(input frontier order, then product terms in minimized-cover order, then outputs). Then the
synthesized file is a pure function of the table plus the layouter's determinism (#654 AC-4),
and AC-2 is true rather than aspirational. Without this, "commit it as a golden" is a promise
the format cannot keep.

## AC-4 is already delivered; do not rebuild it

"Writes no partial circuit file" is `FileAbstractor.writeCircuit`'s existing temp-file/atomic-rename
behavior, pinned by `FileAbstractorTest#writeCircuitReplacesExistingFileAndLeavesNoTemp`. The
all-or-nothing *computation* precedent is also already set and named:
`docs/standards-adoption/11-costed-rejections.md` cites `PcfEmitter`'s contract — collect every
problem, throw one exception, return no text, "so a partial or invalid constraint file can never
reach disk." Synthesis should copy that (`HdlExportException`-shaped refusal, or its `imp` peer
`ImportException`), and AC-4 reduces to exit-status wording plus a test. Writing a bespoke
partial-file guard here would be a third copy of a solved problem.

## The CLI seam: this is the forcing function for the subcommand decision

The boundary note ("the batch CLI stability promise is #524; this adds a flag under it") treats
flag addition as free. It is not, at this moment. `JLSStart.FLAGS` is a 15-row single-dash table
whose prefix ambiguity already needed a longest-match rule (`-v` vs `-vcd`, #72). #646 adds a
flag; #656 adds another; CAP-09 and CAP-21 have their own. `docs/picocli-evaluation-2026-07.md`
states exactly what would flip its recommendation: "a real subcommand surface (`jls export`,
`jls sim`, `jls grade` …) … would be a *new* contract negotiated at design time rather than an
old one reproduced shim-for-shim."

CAP-31's two batch tasks are that forcing function. The worst possible order is: freeze the
single-dash surface under #524, then immediately append two subcommand-worthy verbs
(*analyze*, *synthesize*) to it, and owe both a deprecation window forever. The visionary
recommendation is to put a `STATUS:` note on #524 and #646 saying the analysis verbs are
arriving, and let #524 decide the shape *once* — either "flags only, forever, additive" written
down as policy, or a `jls <verb>` surface with the legacy flags aliased. Either answer is fine;
deciding it twice is not.

## What I would file instead

Keep the outcome sentence verbatim. Replace the criteria with:

- AC-1 (unchanged): synthesis runs with no windowing system present.
- AC-2': the synthesized circuit is byte-identical across installs, because synthesized
  elements mint ids under a reserved deterministic replica in a canonical table-derived order.
- AC-3': the input is a `.jls` circuit containing a `TruthTable` element — the same
  representation #646 writes and `TruthTableEditor` edits — so extraction, editing and
  synthesis compose with no converter and no new format spec.
- AC-4': refusals reuse the `PcfEmitter` all-or-nothing contract and `FileAbstractor`'s atomic
  write; the task adds exit-status wording and its test, not new machinery.
- AC-5 (new): the operation is a model-layer circuit-to-circuit transformation that both the
  batch surface and the editor invoke; the batch path adds no synthesis logic of its own.
- AC-6 (new): the netlist handed to `NetlistImporter` is a value, not parsed JSON — the
  `hdl.importer` input contract `docs/extension-points.md` lists as "to be defined" is defined
  here or in #653, and #62 consumes the same one.

That is still about 1 mw, and it leaves JLS with one table representation, one netlist-import
path, and one CLI decision instead of three of each.
