# Issue #536: FEAT-C24-1: a schematic exports as print-styled SVG and PDF — print symbols, no screen chrome, byte-identical across platforms
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and the wish is: *the picture in the handout is the
circuit, and stays the circuit.* CAP-24 (#505) says so plainly — the failure it exists to
kill is the figure that silently disagrees with the tool. Everything else in #536 — print
theme, direct PDF, byte-identity across three CI platforms — is machinery chosen to serve
that, and #505's own §3 concedes the machinery is the risk.

That goal is well aligned with the project's arc, and unusually so for a capstone-tier
item: the person who benefits is the *instructor*, and the instructor is the person who
chooses the simulator. #508's highest-leverage direction is reconciling with the live user
base (WashU/bsiever). A figure factory is one of the few capability capstones whose primary
user is that decision-maker. I would keep this outcome funded. I would not build it the way
#536 describes.

## What the tree actually offers (and the issue mis-describes)

Three load-bearing facts, all checkable:

1. **`Theme` is not a symbol seam and is not registry-keyed.** `src/jls/Theme.java` is a
   10-field colour record (`touch, highlight, selection, watch, nonZero, initialState,
   wireOff, wireZero, grid, background`) whose `apply()` mutates global statics
   (`JLSInfo.Palette.*`, 46 read sites). It carries no symbol vocabulary, no line weights,
   and no "draw the watch fill?" switch. AC-4 ("the print theme extends the existing
   `Theme` registry-keyed seam — no parallel symbol vocabulary is minted") therefore
   describes a seam that does not exist. Worse, the mechanism is a global static swap: a
   headless print export that "applies" a theme rewrites the colours of every open editor
   in the same JVM. That is directly at odds with AC-5 (KC-24-4, zero editor cost) the
   moment export runs from the GUI.
2. **The real symbol seam already exists, one layer down.** `jls.edit.ElementRenderers` is
   a class-keyed renderer registry with 29 `*Renderer` classes behind
   `ElementRenderer.draw(Graphics, Element)`; `jls.elem.GateOutline` expresses gate body
   geometry as *pure headless coordinate data* and its Javadoc states outright: "this is
   the pattern later waves continue across the rest of `jls.elem`." `jls.core.TextMetrics`
   already abstracts string measurement away from `FontMetrics`. The seam the print figure
   needs is a widened `ElementRenderer` (or an outline-style description sink), not a
   colour record.
3. **The current SVG path is a Graphics2D recording, not a figure.**
   `CircuitRenderer.exportImage` swaps a `SVGGraphics2D` in for the `BufferedImage`
   graphics and replays the same `ElementRenderers.draw` calls. `SvgExportTest` asserts the
   pin label appears in the output as the literal text `out` — i.e. text is emitted as
   `<text>` nodes resolved by the *viewer's* fonts, and the same test's header records why
   there is no golden: "text layout coordinates depend on the JDK's font metrics, which
   differ across machines."

## Reframing 1 — cut the seam at the symbol description, not at the theme

Widen `ElementRenderer` from "draw yourself on a `Graphics`" to "describe yourself as
geometry" — the `GateOutline` pattern generalised: segments, filled regions, text runs with
anchor + role, all in integer grid coordinates, plus a role tag per primitive (body, wire,
label, state-value, watch-marker, selection). Then:

- a **print figure** is a *filter over roles* (drop watch/selection/highlight/grid, force
  monochrome), not a colour palette. That is the honest expression of "no screen chrome" —
  chrome is drawn by conditionals (`if watched → fill watchColor` in `PinRenderer`,
  `MemoryRenderer`, `RegisterRenderer`, `JumpStartRenderer`), and no `Theme` field can turn
  those off;
- **SVG, PDF, CircuiTikZ (#537), tactile SVG (#546), gallery SVG (#551), and CAP-19's
  browser export become backends over one description**, which is the answer the pass-1
  boundary comment on this issue explicitly left open ("are these three renderings of one
  symbol vocabulary through one seam, or three renderers?"). Answered structurally rather
  than by policy;
- **CircuiTikZ stops being a second renderer.** Walking `Graphics2D` calls into TikZ is
  miserable; mapping a role-tagged outline to `\draw` paths (with native `\node[and port]`
  where a symbol matches) is mechanical, and #505's "approximation table" writes itself
  from the roles that have no TikZ equivalent;
- **Open Question 3 dissolves.** PDF is not "direct vs. converted from SVG"; it is a third
  backend over the same description. (`org.jfree.svg` is already a dependency;
  `org.jfree.pdf` is its GPL-compatible sibling with the identical Graphics2D shape.)

This is also the only version of the work that obeys AC-4's *intent*. As written, #536
must either mint 29 print renderers (the parallel vocabulary AC-4 forbids) or thread a
style parameter through a seam it says it is not changing.

## Reframing 2 — text as outlines makes "the hard part" vanish

#505 prices PF-1 at 4–6 mw because "text metrics is most of it," and KC-24-1 makes text
metrics the program's kill gate. That expense exists only under the assumption that the
figure keeps emitting `<text>`. Bundle one permissively-licensed font in the jar, measure
through the existing `jls.core.TextMetrics` seam against *that* font, and **fill glyph
outlines** (`Font.createFont` → `GlyphVector.getOutline` → `g.fill`) on the figure path.
Consequences:

- byte-identity across platforms follows from the pipeline touching no OS font state — it
  becomes a property to *prove*, not a matrix to *sample*;
- PDF needs no font embedding and no subsetting, so the PDF backend gets cheap;
- and it fixes a defect #536 does not notice: a `<text font-family="SansSerif">` figure
  renders *differently in every viewer* — Inkscape, Chrome, and `\includesvg` will disagree
  about the label width, so labels will collide with wires in the handout even when the
  bytes are identical everywhere. Byte-identity is not camera-ready; outlines are.

The cost is real but small and native to this repo: a bundled font is a `bom.json` row and
a reproducible-build input, both of which the project already handles deliberately.

## Reframing 3 — figures are build products; the determinism gate is aimed at the wrong thing

The stated reason for byte-identity is "so the figure can live in a course repo under
version control and be diffed in review." The standard answer for a generated artifact is
not to make it reproducible enough to commit — it is *not to commit it*. JLS already ships
every piece needed: a headless `-i` export, a reproducible jar, a multi-arch container
image, and `-savetext` so the `.jls` source itself diffs cleanly as plain text. A course
repo that versions `circuit.jls` and builds `fig.svg` in its Makefile/CI gets
figure-vs-circuit agreement *structurally* — the mismatch CAP-24 exists to kill becomes
unrepresentable — and #541's "one command, one run" bundle contract collapses into an
ordinary build rule.

Under that framing:

- **KC-24-1 should not be a program gate.** Per-platform byte churn stops being fatal to
  the premise, so "stop and re-plan CAP-24 if two platforms disagree" is a gate on a
  property the outcome no longer depends on. Keep the cross-platform check as a
  corroborating smoke test; do not let it hold PF-2..PF-6 hostage.
- **The AC that actually serves "diffed in review" is diff *locality*, which #536 never
  asks for.** A Graphics2D recording with absolute coordinates and `defs` prefixes turns a
  one-gate move into a whole-file diff — byte-identical across machines and useless in
  review. Replace AC-2 with: *moving one gate changes only the hunks for that gate;
  renaming a pin changes only its text node; element ids in the SVG derive from JLS element
  ids, and output order is the element id order.* That criterion is testable on one
  platform, is what a reviewer actually experiences, and is impossible to satisfy by
  accident — the current export cannot pass it.
- **Determinism becomes a ratchet, which is this repo's own idiom.** A
  `FigurePurityRatchetTest` in the shape of `HeadlessCoreRatchetTest` /
  `SocketConfinementRatchetTest` — the figure path may not reach `GraphicsEnvironment`,
  `Toolkit`, default-locale formatting, `HashSet` iteration, or wall-clock — is a stronger
  and cheaper guarantee than three CI runners agreeing once.

## What I am disregarding, and why

Explicitly: **AC-2 (three-platform byte-identity as the primary determinism claim), AC-4
(the print theme extends `Theme`), and KC-24-1 as a funding gate.** AC-2 buys an empirical
sample of a property that should be structural and misses the property the outcome
actually needs (diff locality, viewer-independent appearance). AC-4 names a seam that is a
colour record mutating global statics and cannot express "no screen chrome" at all. KC-24-1
gates the whole capstone on a risk that reframing 3 retires. AC-1, AC-3 and AC-5 survive
unchanged; AC-3's "direct, not converted" is right, and gets cheap under reframings 1–2.

## Sequencing, and one honest reservation

`ordering_after: [FEAT-C24-5]` (the print-symbol totality ratchet) is correct in spirit and
becomes enforceable under reframing 1: totality is "every registered element has a
description under the print role filter," which rides `ElementRegistry`'s existing totality
test (`ElementRegistryTest`) exactly as #505 hopes. Under the issue as written, totality
would have to be policed over 29 hand-written print renderers — the decaying theme KC-24-2
already fears.

The reservation is scope discipline, not direction. #508 places CAP-24 in *keep-strategic:
cheap slice now, rest gated*, and closes with a planning ratchet — "no new tier:feature
until two capstones close" — that this `tier:feature` issue, filed the next day, sits
awkwardly against. The reframed slice is genuinely cheap: role-tagged description for the
gate/wire/pin subset, glyph-outline text against a bundled font, the diff-locality test,
and the purity ratchet. That is the 2–3 mw demo #508 priced, and it retires more risk than
the byte-identity matrix would. PDF, CircuiTikZ, and the bundle should wait behind it —
not behind a determinism verdict, but behind evidence that a real instructor wants the
figure at all, which #508 says nobody has yet asked for.
