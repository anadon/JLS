# Issue #712: TASK-C537-1: a drawn circuit exports as CircuiTikZ source that compiles standalone with no manual edits
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

CAP-24 (#505) names the prize plainly: "textbook and courseware authors who currently
redraw every schematic in CircuiTikZ by hand — the largest single time sink in
hardware-course authoring." The thing an author wants back is not a picture. A picture
they already get: `-i out.svg` ships (#154, `CircuitRenderer.exportImage`), and #536 makes
it print-styled and adds PDF. What CircuiTikZ buys over `\includegraphics{fig.pdf}` is
exactly three things: the figure typesets in the document's own fonts, it can be annotated
and restyled in LaTeX without leaving the document, and it diffs as source. Any design for
#712 has to be judged on whether it delivers those three, because nothing else here is new
value.

## The central problem: AC-2 selects the design that destroys the value

AC-2 requires that placement, orientation and routing "correspond to the drawn geometry
through the print-geometry decisions of TASK-C536-1, not through a second geometry model."
CircuiTikZ *is* a second geometry model. `\node[and port] (u1) {}` is drawn by pgf with
pgf's proportions and pgf's anchors (`u1.out`, `u1.in 1`); nothing in JLS's print theme
reaches inside it. So AC-2, read literally, forbids using CircuiTikZ's symbol library and
leaves exactly one compliant implementation: dump JLS's own strokes as `\draw` primitives
in a `tikzpicture`. That output compiles standalone (AC-1 passes, with `tikz` alone — the
`circuitikz` package would be unused), is deterministic (AC-4 passes), and is worth almost
nothing to the author it was filed for: it is an SVG with a different syntax, no more
editable than the PDF, and its labels are drawn glyph-by-glyph rather than typeset.

The contradiction is visible one level up. Parent feature #537 AC-3 demands "a row for
every element type **not rendered natively by CircuiTikZ**, naming its substitute
rendering." That criterion is meaningless under the primitive dump — nothing is rendered
natively, the table is either empty or 74 rows of "we drew it ourselves." #537 assumes
semantic emission; #712's AC-2 forbids it. One of the two has to move, and #537 is the one
carrying the user.

## The font trap AC-2 also imports, unnoticed

`jls.core.TextMetrics` records that "elements size themselves from the width and height of
their labels," measured through AWT `FontMetrics`. Element bounds are therefore a function
of Java font metrics. #536 retires its determinism risk by bundling a font and owning those
metrics — and none of that transfers. LaTeX typesets the emitted labels in the document's
font at the document's size; a box JLS sized to fit "CLK_ENABLE" in bundled-DejaVu at 12px
will be the wrong box in Computer Modern at 10pt. Inheriting print geometry across the TeX
boundary does not make the figure faithful; it makes it subtly wrong in a way that only
shows up in the author's actual document, which is the one place nobody's CI is looking.
The honest options are to let LaTeX size the nodes (semantic emission) or to emit label
text with explicit scaling and accept it will not match the surrounding prose — the issue
picks neither because it never notices the boundary exists.

## The seam is the emitter family, not the print renderer

`jls.hdl` already is the pattern this exporter wants: `HdlModel` (a flattened, named,
GUI-free view of the circuit) → `HdlEmitter.emit(model)` returning text, with `VerilogEmitter`
and `VhdlEmitter` as two tenants, `HdlNames` supplying stable sanitized identifiers, and
`hdl.exporter` a typed extension point pinned by `ExtensionPointCatalogTest`
(docs/extension-points.md). CircuiTikZ is a text emitter over a named netlist with layout
hints — structurally a third tenant of that family, not a fourth backend of the pixel
renderer. Filing it there buys, for free: deterministic identifier generation already
solved and tested for Verilog; headless by construction (AC-3 satisfied by where the code
lives); the element→shape mapping keyed on `jls.elem.ElementRegistry`, so PF-5's totality
ratchet is the same ratchet `ElementRegistryTest`/`PaletteContractTest` already run, and
#537's approximation table becomes a *generated* table rather than a hand-maintained
document that rots.

Two consequences worth stating out loud. First, AC-4 is nearly free here and retires no
risk: text emission over stable names has no platform variance to begin with, so counting
it as a share of CAP-24 AC-2 inflates the determinism claim rather than testing it — the
real determinism risk (KC-24-1) lives entirely in #536's text metrics. Second, the emitted
`.tex` becomes a compatibility surface the moment a course repo diffs it, and #712 says
nothing about that. The VCD profile and `-t` grammar are frozen in
docs/batch-interface.md; a figure-source format that lands in student-facing repos needs
the same treatment or the first symbol-mapping improvement silently churns every author's
diffs.

## Alternative A — the one I would fund first: PDF plus a LaTeX anchor file

Disregard AC-1 for a moment and ask what the smallest artifact is that lets an author stop
redrawing. It is #536's PDF *plus a coordinate file*: a generated `.tex` sidecar that
defines nothing but named anchors — `\jlsanchor{u3.out}{0.412}{0.688}` for every element
and connection point, derived from `Element.getIndexBounds()` and its `Put` positions,
normalized to the figure box — wrapped in a `tikzpicture` that `\includegraphics`es the PDF
underneath. The author writes `\draw[red,->] (jls-u3.out) -- ++(1,1) node{glitch here};`
and gets annotation, highlighting, callouts and captions in document fonts, over a figure
that regenerates from the circuit that ran. This is Inkscape's "omit text in PDF, create
LaTeX file" mode, which is how a large fraction of academic figures are already made; the
pattern is proven and the failure modes are known. Cost is a fraction of a milliweek —
`CircuitRenderer` already computes every number it needs — it is total over the palette by
construction (no shape mapping, no approximation table, no missing-symbol case), it is
byte-deterministic, and it needs no LaTeX in the jar. It does not give document-native
symbol rendering; it gives everything else, immediately.

## Alternative B — if the semantic exporter is still wanted, build it as a ladder

Tier 1 is Alternative A. Tier 2, if a literal `tikzpicture` of strokes is wanted anyway, is
a `Graphics2D` subclass that emits `\draw` — the identical trick #154 already plays with
JFreeSVG's `SVGGraphics2D` inside `exportImage`, which is why SVG cost "no per-element
work." Tier 2 is hours, not a milliweek band, and it satisfies AC-1/AC-2/AC-4 as literally
written; it should be priced and labelled as what it is rather than sold as CircuiTikZ
export. Tier 3 is the real thing: `ElementRegistry`-keyed mapping to `and port`, `or port`,
`nand port`, `xor port`, `buffer`, `flipflop D`, `muxdemux`, `dipchip` and a documented
generic-box fallback, JLS grid coordinates used as *placement hints* rather than as
authority, wire routing emitted between CircuiTikZ anchors, and the approximation table
generated from the mapping's fallback rows. Tier 3 is where #537's promise lives and where
its 2–3 mw belongs; it should be gated on a named author asking for it, because tiers 1–2
may well end the complaint.

## Explicitly disregarded, and why

- **AC-2 as written.** It cannot coexist with #537 AC-3 and it forbids the only design that
  delivers the outcome. Replace with: "element bodies are CircuiTikZ shapes where the
  registry maps one; JLS geometry supplies placement, not symbol proportions; every
  fallback appears in the generated approximation table."
- **AC-4 as a CAP-24 AC-2 share.** Keep the byte-identity test, drop the claim that it
  retires determinism risk. Add instead: the emitted source is regenerated identically from
  the same `.jls` at a later commit, or the format change is a documented, versioned one.
- **The 1–1.5 mw band and the `ordering_after: [TASK-C536-1]` dependency.** Under both
  alternatives above, nothing here needs the print theme to land first; Alternative A needs
  only #536's PDF writer, and Tier 3 needs no print geometry at all. The ordering edge is
  inherited from the wrong architectural story.

## Kept as-is

AC-3 (reachable headlessly) and AC-5 (no LaTeX in the shipped jar) are exactly right and
match the container/batch trajectory in README and ARCHITECTURE. Any redesign must keep
both, and Alternative A keeps them trivially.
