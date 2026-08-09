# Issue #287: HiDPI-clean toolbar icons: 33 24×24 GIF bitmaps upscale blurry under JEP 263 scaling — redraw as SVG (FlatSVGIcon) through the makeElement seam
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "SVG icons." The end is: **the palette should show the student the
thing they are about to place, sharply, at whatever scale and theme the machine
is running.** #287 is one of the last raster surfaces in an application that has
otherwise spent the whole #74/#77/#78 arc becoming resolution-independent and
data-driven. Judged against that arc, the goal is exactly right and the issue is
well-scoped, evidence-backed, and correctly bounded against #286 and #381.

The *mechanism* is the part I would change, because the repository already
contains the vector source this issue proposes to hand-author a second copy of.

## The observation the issue never makes

JLS no longer has raster or ad-hoc element drawing anywhere. `grep "void draw("
src/jls/elem/*.java` returns nothing — the #77 wave moved every element's
rendering out of the model into `jls.edit`:

- `src/jls/edit/ElementRenderer.java` + `ElementRenderers.java` — a
  class-keyed renderer registry; `BuiltinElementRenderers.install()` registers a
  renderer for essentially every palette type (Adder, Mux, Memory, Register,
  StateMachine, TruthTable, the six gates via a shared `GateRenderer`, …).
- `src/jls/elem/GateOutline.java` — gate symbols expressed as *pure coordinate
  data* (lines, arcs, cubics, bubbles) with no AWT dependency; `AndGate.outline()`
  is literally the AND symbol as vectors.
- `src/jls/edit/Viewport.java` — the canvas already draws all of this through an
  `AffineTransform` at 0.25×–4.0× zoom, so element rendering is already
  scale-independent and proven so at 4×.
- `src/jls/edit/CircuitRenderer.java:312-320` — the same renderers already emit
  **real SVG** through JFreeSVG's `SVGGraphics2D` for `-i out.svg`.
- `src/jls/elem/Element.java:415` `init(TextMetrics)` computes width/height from
  a headless metrics interface (`jls/core/TextMetrics.java`, `SwingTextMetrics`),
  so an element's bounds are obtainable without a dialog or a canvas.

So the project *already has* a single, authoritative, scale-free vector
description of every element, plus a proven pipeline for painting it at any
scale and for exporting it as SVG. #287 proposes to hand-draw 33 more vector
glyphs describing the same shapes, in a different format, maintained by a
different mechanism, kept in sync by nobody.

## Alternative framing: the icon is the element, drawn small

Replace `getImage(String)` (`SimpleEditor.java:2363`) with a
`PaletteIcon implements javax.swing.Icon` built from the entry itself:

1. once per entry, `Element proto = entry.type().create(prototypeCircuit)` —
   the same call `makeElement`'s action already makes on click — then
   `proto.init(SwingTextMetrics)` to get valid bounds;
2. in `paintIcon(c, g, x, y)`: `Graphics2D gg = (Graphics2D) g.create()`,
   translate to `x,y`, `gg.scale(24.0 / max(w,h))` fit-to-box, then
   `ElementRenderers.draw(gg, proto)`.

Under JEP 263 the `Graphics2D` handed to `paintIcon` already carries the device
transform, so this is crisp at 100/125/150/200% **by construction** — there is no
rasterization decision to get wrong, and §10's `BaseMultiResolutionImage`
fallback becomes unreachable. What this buys beyond H1:

- **No new dependency.** `flatlaf-extras` (plus svgSalamander) never enters the
  shaded jar, the SBOM, or the reproducible-build surface — a real consideration
  for a project that publishes `bom.json`, attestations and byte-reproducible
  jars as headline features.
- **Zero assets, and one fewer step to add an element.** ARCHITECTURE.md's
  "adding an element today" list, step 13, is *"a toolbar icon gif in
  `src/jls/edit/images/`"*. #287 as written keeps that step and merely changes
  its file extension. The reframing deletes it. That is precisely the direction
  #78's registry arc is pulling; a 33-SVG asset set pulls the other way.
- **The icon cannot lie.** Today the GIF is a 20-year-old drawing of a symbol the
  renderer may since have changed. A renderer-drawn icon is definitionally the
  shape that lands on the canvas.
- **Dark mode falls out for free.** #287 §13 lists "dark-mode icon recoloring via
  FlatSVGIcon color filters" as future work. With renderer-drawn icons there is
  nothing to recolor: the icons theme themselves the moment #286's `Theme`
  seam replaces the `JLSInfo.Palette` / hardcoded-color call sites, because
  icons and canvas run the same code. Two issues collapse into one.
- **It answers the open question by dissolving it.** "Hand-author or auto-trace
  the 33 SVGs?" — neither. And if a checked-in SVG is ever wanted (docs, web
  help, the #796-#798 screenshot-as-build-product work), `CircuitRenderer`'s
  existing `SVGGraphics2D` path can *generate* it from the same renderer.

## Where the reframing is honestly weaker, and the hybrid I'd actually ship

A shrunken schematic is not automatically a good 24px glyph. The text-bearing
boxes — Memory, RegisterFile, Register, StateMachine, TruthTable, SigGen,
Display — will render as an unreadable box with grey mush where a label was, and
the classic GIFs are optically simplified for exactly that reason. Splitter and
Binder may degenerate similarly.

So: **renderer-drawn by default, per-entry asset override where the miniature
fails.** `PaletteEntry` already carries `iconName`; make it optional and add
`iconOverride`. That is roughly 5–8 hand-authored glyphs instead of 33, and the
override list is a visible, test-enforced statement of "these symbols do not
survive shrinking" rather than an invisible 33-file parallel asset tree. If the
override path is kept, `FlatSVGIcon` is a fine way to serve those few — the
`flatlaf-extras` dependency becomes an optional tail, not the premise.

The one genuine unknown, and the experiment I would substitute for H1: **is a
freshly `create`d, `init`ed element drawable off-circuit?** Several elements get
their size from their creation dialog (Memory address/data bits, Mux inputs,
Register width), so a blank prototype may have degenerate or default bounds, and
some renderers may touch put/attachment/highlight state. `ElementRenderers.draw`
falling through to `ElementRenderSupport.drawHighlight` for an unregistered class
would draw *nothing*, silently — the fallback must be a `PaletteContractTest`
failure ("every entry has a registered renderer and paints a non-blank 24×24"),
not a blank button. That test is the same shape as the one §8 already proposes,
and it is a stronger contract than "the SVG resource resolves": it asserts the
icon has *ink*, which the current GIF contract never did.

## On the stated acceptance criteria

I am not disregarding them, but three change under the reframing: "author one SVG
per GIF (33 glyphs)" becomes "author overrides only"; "add `flatlaf-extras`"
becomes conditional on whether any override survives; "grep shows no runtime
`.gif` references" stays and is the right ratchet. Keep everything else —
including the `Dimension(32,232)` typo fix, the display-rig screenshots at
100%/200% as ground truth (paint-to-image sharpness assertions are the weaker
oracle and §11 is right to say so), and the `STATUS:` report to #76.

One further alignment note: the boundary comment against #381 is correct and
should hold, but if the reframing is taken, this issue's value to #381 grows —
it becomes the piece that makes #381's dark-variant work cover the toolbar for
free, rather than a separate icon-recoloring chore #381 would inherit.

## Bottom line

Endorse the goal; reframe the mechanism. The blurry toolbar is not an
asset-format problem, it is the last place in JLS where a drawing is stored
instead of drawn. Cut the seam at "the palette renders elements" rather than at
"the palette loads better pictures of elements," and the 33-asset maintenance
burden, the new dependency, the dark-mode follow-up, and the icon/canvas drift
risk all disappear together — leaving a handful of deliberately hand-tuned
overrides as the only assets that remain.
