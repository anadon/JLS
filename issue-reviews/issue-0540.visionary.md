# Issue #540: FEAT-C24-5: every palette element has a print symbol — a palette-sweep export emits zero warnings, and a new element without one fails the build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-24 (#505) step 5 wants one guarantee: *the camera-ready export never
silently degrades as the palette grows*. That guarantee is right and worth
buying. #540 buys it by inventing an artifact — "a registry-keyed mapping
gives every registered element type a print symbol" — plus a runtime warning
channel ("zero missing-print-symbol warnings") plus a ratchet over the new
map. I am endorsing the guarantee and disregarding that mechanism, because
the tree already contains two seams that make the map unnecessary and a third
concept that #540 would collide with.

## Ground truth at HEAD (not plan)

1. **Drawing is already registry-dispatched and already total-by-construction
   on the export path.** `ElementRenderers.draw(Graphics, Element)`
   (`/home/user/JLS/src/jls/edit/ElementRenderers.java:52`) is the single
   dispatch, `BuiltinElementRenderers.install()` registers essentially the
   whole palette, and `CircuitRenderer.exportImage`
   (`/home/user/JLS/src/jls/edit/CircuitRenderer.java:314-352`) drives the
   JFreeSVG canvas through *that same call*. The doc comment on
   `exportImage` says it plainly: ".svg output needs no per-element work."
   `ElementDrawSmokeTest` already sweeps one instance of every drawable type
   through both export canvases with a named exemption set (`TestGen`), and
   `PaletteContractTest.paletteIsTotalOverTheElementRegistry` already asserts
   *set equality* against `ElementRegistry.all()` minus three named tags.
   Palette-to-registry totality is not a thing to build; it ships.

2. **JLS's screen gate symbols already are ANSI/IEEE-91 distinctive shapes.**
   `AndGate.outline()` (`/home/user/JLS/src/jls/elem/AndGate.java:43-51`) is a
   flat back, two sides and a 180-degree arc — the distinctive AND shape, as
   headless coordinate data. AC-4 chooses "ANSI/IEEE-91 distinctive shapes
   first". For gates that acceptance criterion is satisfied by doing nothing.
   The rest of the palette is already labelled rectangles.

3. **The genuine second symbol vocabulary is already designed, under a
   different name, with its own totality ratchet.**
   `/home/user/JLS/docs/standards-adoption/01-iec-ieee-symbols.md` specifies
   `jls.core.SymbolSet {DISTINCTIVE, RECTANGULAR}` as a **global render mode**
   ("not per-element, not an extension point"), a `-symbols` flag on `-i`, a
   normative `docs/symbol-conformance.md` matrix, and a regression inventory
   whose first line is "a new element without a conformance row →
   `SymbolConformanceMatrixTest`". That *is* #540's ratchet, aimed at a
   user-visible standards claim instead of an internal table.

4. **Printing is a shipping surface, not a future one.** `CircuitRenderer`
   implements `Printable` (`:41`, `print` at `:186`, `addToBook` at `:261`).
   File > Print today emits screen colors and screen chrome onto paper.

## The collision #540 walks into

CAP-24 risk 2 says two symbol vocabularies must not fork. #540 forks them
anyway — inside CAP-24. The tree's axes are (a) `Theme` — semantic colors,
`/home/user/JLS/src/jls/Theme.java`, two variants, applied by rewriting
`JLSInfo.Palette` statics — and (b) `SymbolSet` — which shapes, per the
standards doc. #540 introduces a third, orthogonal to neither: "screen symbol
vs print symbol". Ask what is actually different about a printed AND gate and
the answer is *color, line weight, chrome (grid, selection fill, watch fill,
value overlays)*. Those are Theme roles. Nothing in the symbol is different.
A `printSymbolFor(ElementType)` map is a table whose every row would restate
what `ElementRenderers` already knows, and whose rows can then drift from the
canvas — the exact icon-vs-canvas drift class the standards doc calls "a real
correctness bug, not cosmetics" (§11 there).

## The alternative design

**Do not add a print-symbol mapping. Add a print *theme*, reuse the symbol
seam, and put the ratchet where the silent hole actually is.**

- **`Theme.PRINT`** — one more constant in the existing record
  (`Theme.java:47-83`): black `nonZero`/`wireZero`, white background, grid and
  selection/watch set to background so chrome vanishes. `Theme.apply()`
  already rewrites every call site. This lands in an afternoon and immediately
  improves the shipping `File > Print` path, not just a future exporter.
  Determinism is unaffected: colors are constant for the process, so
  `SvgExportTest#exportingTwiceIsByteIdentical` holds unchanged.
- **Symbol choice stays `SymbolSet`**, filed from the standards doc rather
  than reinvented here. AC-4's "ANSI first, IEC as a later second theme
  through the same seam" is then literally true — one enum, two values, one
  conformance matrix — instead of aspirationally true across two tables.
- **The ratchet moves down one level, to where the tree is actually silent.**
  `ElementRenderers.draw` falls back to
  `ElementRenderSupport.drawHighlight(g, el)` when no renderer is registered
  (`ElementRenderers.java:53-58`). An element type registered with no renderer
  therefore draws *nothing but its highlight* — on screen, in raster export,
  in SVG export, and on paper — and `ElementDrawSmokeTest` still passes,
  because it is smoke-grade ("draws without throwing, produces a decodable
  image"). No test in `test/` asserts `ElementRenderers` totality over
  `ElementRegistry.all()` (grep: only `RenderBoundsTest`, `MuxSymbolTest`,
  `WireValueChannelTest` mention the class at all). That is the
  one-element-at-a-time decay #540 is worried about, one layer below where
  #540 puts its guard, and it exists *today*. A `RendererTotalityTest` —
  equality against `ElementRegistry.all()` minus a named exemption set,
  extending #315's base when it exists — is a strictly stronger AC-4 than a
  map-coverage check, costs one file, and needs no new product surface.

**Delete the warning channel.** AC-1 asks a palette-sweep export to "complete
with zero missing-print-symbol warnings". A diagnostic whose only correct
value is zero is dead code in the shipping product: it must be invented,
plumbed through the headless batch path (which has a documented one-line
stderr contract — `ARCHITECTURE.md` "CLI contract", `docs/batch-interface.md`),
tested, and specced, purely so a test can assert its absence. KC-24-2 already
says the point is a build-time property. Make it one: no runtime warning, a
red build instead. That also removes a batch-interface surface from CAP-24's
blast radius entirely.

## What this does to CAP-24's shape

The `PF5 --> PF1` edge in #505's ordering graph exists only because the
capstone assumed a symbol table must precede the exporter. Under this
reframing PF-5 is a test plus a `Theme` constant, so it lands *with* or
*after* PF-1 and the edge dissolves — one fewer serialization point on the
capstone's critical path, and PF-1's demo slice (the KC-24-1 text-metrics
risk, the genuinely hard part) can start immediately. That is the highest-value
consequence here: #540 as written is a gate in front of the only part of
CAP-24 that can fail.

## Two defects to fix while re-deriving

- **The co-funding citation is wrong.** #540's boundary note and #505 cite
  "#43/#44" as the IEC/IEEE symbol-conformance roadmap items. In this tracker
  #43 is *editor hot paths* and #44 is *release pipeline hardening*, both
  closed. The real referents are **rows 43/44 of
  `docs/standards-landscape.md`**, whose design lives in
  `docs/standards-adoption/01-iec-ieee-symbols.md` and which have **no filed
  issue**. So the claimed cost co-funding has no counterparty: if #540 lands a
  private print-symbol map first, the standards work must later unify or fork
  it. File the `SymbolSet` issue first and make #540 its consumer.
- **The #315 boundary note is right but now understates itself.** Its
  recommendation (be a row in #315's inventory, extend #315's base, assert
  equality not containment) is correct. Under this reframing #540's entire
  residue *is* that row plus one `Theme` constant — which is a good outcome,
  not a diminished one: a 1-2 mw feature that mostly deletes planned work.

## Concrete re-derivation for #505

Replace PF-5 with: *"print theme (`Theme.PRINT`) plus a renderer-totality
ratchet: every registered element type has a registered `ElementRenderer`
(equality over `ElementRegistry.all()` minus a named exemption set), so a
palette-sweep print/SVG export renders every element by construction; symbol
choice is `SymbolSet`, filed separately from the standards-adoption design."*
Then #505 §1 step 5 reads "**Observe** the palette-sweep export renders every
registered element type; a scratch element with no renderer fails the build" —
the same guarantee, one artifact fewer, no new runtime diagnostic, and the
CAP-19 / standards seam shared rather than promised.

Keep AC-2 exactly as written. The falsification transcript obligation is the
best thing in this issue and applies unchanged to the renderer ratchet: a
scratch `ElementType` with no renderer must be shown red before any green run
counts.
