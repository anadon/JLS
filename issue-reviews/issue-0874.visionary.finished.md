# Issue #874: TASK-C541-1: the handout bundle is one command over one circuit and one recorded run — a second run, or none, is refused by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two thirds of the body is roster bookkeeping — the #727 dedup, the empty-feature
disposition on #541, why splitting beats relabelling. None of that says anything about
what JLS becomes. Strip it and one durable claim remains:

> A figure JLS emits must carry the identity of the run it came from, and a set of
> figures must be unable to disagree about which run that was.

That is the whole reason CAP-24 (#505) is a capstone rather than a pile of exporters —
risk 4, and §1's "figures derived from real runs cannot silently disagree with what the
tool does." Endorse it without reservation. What I would change is nearly everything
about the *shape* proposed to deliver it: a hardcoded five-way composer, a bespoke arity
check, and a provenance criterion the issue's own composition-only boundary forbids it
from implementing.

## The hole underneath: the "recorded-run artifact" has no owner

The command's central input is undefined anywhere in this repository or in CAP-24's
plan. #505's `planned_features` are PF-1..PF-6 — print SVG/PDF, CircuiTikZ, WaveJSON,
APNG, symbol totality, the bundle. **None of them is "record a run."** The discipline is
cited to #498 §7.2, which is rescued, self-declared *non-normative* text from a branch
that will never merge, whose recording machinery (transcript + byte-identical replay) is
M2 of a 6–9-month virtual-hardware program that has not started. Grep confirms the
vocabulary appears nowhere in `docs/`, `ARCHITECTURE.md` or `README.md`.

So #874 is a task whose first argument is a file format nobody has defined, sequenced
after four tasks in features that are themselves unfiled. That must be resolved before
this can be picked up, and there are exactly two honest resolutions:

1. **Declare the recorded run to be what already ships**: the `-vcd` dump (a *frozen*
   profile, `docs/batch-interface.md` §4/§6) plus the circuit bytes and the window. This
   is nearly free and it makes the bundle buildable today. But say the consequence out
   loud: VCD carries watched signals only, so it cannot feed PF-4's per-element
   signal-value overlay on a canvas region. Either the animation degrades to watched
   signals, or —
2. **File a run-record feature under #505 by REPLAN** before #874 is scheduled, owning a
   container that carries the circuit digest, the trace samples
   (`BatchSimulator.getTraceSamples`, already the in-memory shape of this thing) and the
   time window.

Choosing (1) is my recommendation; it is the smaller claim and it retires the dependency
on a program JLS has not committed to. Either way, this task cannot own that decision and
currently pretends it is settled.

## Reframing A (the headline): the bundle is a registry sweep, not a five-way pipeline

JLS has already solved this exact problem once, in-tree and shipped:
`jls.hdl.HdlExporter.export(Circuit, HdlEmitter)` builds one neutral model and renders it
through any registered emitter, published as the typed seam `hdl.exporter`
(`src/jls/hdl/HdlExtensionPoints.java:24`, catalogued in `docs/extension-points.md`,
cardinality *many*, lifecycle *on-command (export)*). Verilog and VHDL are two rows, not
two branches of a composer.

The handout bundle is the same shape: build one `FigureModel` from (circuit, run, window)
and render it through every contribution to a `figure.exporter` point. #711/#714/#718/#722
each contribute an emitter; the bundle command iterates the registry.

What that reframing dissolves, all at once:

- **The PF-4 REPLAN plumbing** — three paragraphs across #541, #874 and #875 exist so the
  animation can only drop "by a `REPLAN:` on #505, never by silent omission." Under a
  registry sweep the bundle emits one artifact per registered emitter; retiring #539 is a
  deleted registration and the contract restates itself. Nothing to edit, nothing to
  police.
- **The "five artifact kinds" wording fight.** The follow-up comment on #541 explicitly
  instructed that AC-1 read "all artifact kinds" with no count — and #874's criterion 1,
  filed to carry that migration forward, says "**all five** artifact kinds" anyway. The
  literal came straight back on the first copy. Under a sweep, no count is expressible.
- **The dedup loop.** #727 was closed because its outcome *was* the feature's whole
  outcome; the disposition comment feared a replacement would be closed again for the
  same reason. A task that owns *a seam and its provenance contract* is structurally not
  the feature restated — it is a slice, permanently.
- **Totality becomes a ratchet, not a criterion.** PF-5 already establishes the pattern
  for print symbols, and `ExtensionPointCatalogTest` already fails the build on a
  registered-but-uncatalogued seam. A figure emitter registered without a bundle row
  fails the same way. That is stronger than any acceptance criterion an executor can
  satisfy by hand.

This also answers the question the issue never asks: *why is the bundle a feature rather
than a nine-line shell script?* Because a script hardcodes four exporters; a seam means
the next figure kind (CAP-19's browser export, #546's tactile SVG, #551's gallery — all
three already flagged as SVG-emitting in the #541 boundary comment) joins the handout by
registering, and its output is provenance-stamped for free.

## Reframing B: make arity a grammar property, not a diagnostic

Criterion 2 is right and it is the most load-bearing sentence in the issue. It is also
solved in the wrong place. `JLSStart` already models operand arity as data — `FlagSpec`
with `Arity.REQUIRED`, cross-checked by `CliFlagTableTest` — but repeated flags currently
*last-win*: `-vcd a -vcd b` assigns twice (`src/jls/JLSStart.java:1078-1079`), silently.
That is the same defect class criterion 2 describes, already live on a shipped flag.

So do not write a bespoke "did I get two runs?" check inside one command. Add one rule to
the flag table — **a flag supplied twice is a usage error, exit 2, naming the flag** —
pinned by `CliFlagTableTest`, and let the bundle inherit it. Criterion 2 is then
discharged by the CLI contract (`ARCHITECTURE.md`, "Error-reporting contracts"), every
existing flag gets the same protection, and the defect the whole feature exists to
prevent becomes unrepresentable rather than refused. Combined with reframing A's model
(one run is an argument of `FigureModel`'s constructor, not an ambient lookup), "a run is
never silently selected, inferred from a directory, or re-derived by simulating again"
stops being a promise an executor must keep and becomes a type nobody can construct twice.

## Reframing C: criterion 5 contradicts the composition-only boundary

The issue says, twice, that it "adds no rendering code of its own." Criterion 5 requires
*every artifact* to record its run identity — which means writing an SVG `<metadata>`
element, a PDF `/Info` key, a TikZ comment and a WaveJSON field. Those are four edits
inside four other issues' renderers. As filed, the criterion is either unimplementable
here or the boundary is false.

Re-home it: provenance is a field on the shared model (reframing A) and each emitter's
contract requires stamping it. Then it is #711/#714/#718/#722's work, one line each, and
— the payoff — **it applies to the exporters that already ship**. `jls -i out.svg` could
stamp circuit digest and run identity today, which is precisely #505's own stated goal
that "every figure in `docs/` and in issues can come from the tool, regenerable at a
commit." That value is available years before the bundle exists.

I agree with the #875 review that run identity must be a content digest of the run and
circuit bytes; anything session-shaped collides head-on with criterion 3's byte-identity
requirement, and criterion 5 is the one that would quietly lose.

## One smaller reuse: version the bundle layout the way JLS versions circuits

Criterion 4 wants the layout documented in-tree. Prefer a manifest file at the bundle
root carrying a `FORMAT` integer, mirroring `Circuit.readFormatHeader` and the
`NEWER_FORMAT` refusal (#79). JLS already knows how to refuse a newer artifact by name
instead of misparsing it; a bundle whose layout is only prose plus a test is a second,
weaker convention for the same problem. It also gives #875's consumer a schema to bind
to rather than paths.

## Alignment

The work strengthens the arc — CAP-24 is the only capstone whose payload is
*trustworthiness of an artifact*, and this task is where that becomes mechanical. It does
not duplicate anything. Its risk to the project is not scope; it is that as filed it adds
a second frozen batch surface (a directory layout) and a bespoke validation path, in a
codebase whose recorded direction is registries and typed seams (#78, #223) and whose
batch interface is already a frozen contract under `batch-interface.md` §6.

I am not disregarding the acceptance criteria wholesale. Keep 2, 3 and 4. I would rewrite
1 to name no count and to mean "one artifact per registered figure emitter," and move 5
upstream to the emitters where it can actually be implemented and where it pays off
immediately.

## Concrete asks

1. Settle the recorded-run artifact first — declare VCD+circuit+window, or file the
   run-record feature by REPLAN on #505. #874 cannot start until one is chosen.
2. Introduce `figure.exporter` as a catalogued seam with a `FigureModel`, modelled
   literally on `hdl.exporter` / `HdlExporter` / `HdlEmitter`; the bundle iterates it.
3. Discharge criterion 2 with a CLI-wide "repeated flag is a usage error" rule pinned by
   `CliFlagTableTest`, fixing the live `-vcd` last-win hazard in the same change.
4. Move criterion 5 into the emitter contract and stamp `-i` exports now.
5. Give the bundle manifest a `FORMAT` version and the existing newer-format refusal.
