# Issue #723: TASK-C540-1: print symbols are a registry-keyed mapping with a totality ratchet — a new element type without one fails the build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Stripped of the ratchet vocabulary, the end being bought is: *a figure exported
for a lab handout never silently omits or misdraws an element, and it stays that
way as the element set grows*. Totality over a table is a proxy for that, not the
goal. Judged against the tree at HEAD, the proxy is aimed at a seam that does not
exist yet, while the seam that does exist — and that already fails the real
goal — is left alone.

## What the tree already has

- **One draw dispatch for screen and print.** `CircuitRenderer` paints the editor,
  the PNG/JPEG export and the SVG export through the *same* per-element call,
  `ElementRenderers.draw(g, el)` (`src/jls/edit/CircuitRenderer.java:144-153`,
  `:314-358` for the JFreeSVG canvas). Print output is not a second rendering
  pipeline; it is the same `Graphics2D` calls against a vector surface. There is
  no place where a distinct "print symbol" could be consulted today.
- **A registry-shaped renderer map already exists.** `ElementRenderers`
  (`src/jls/edit/ElementRenderers.java`) keys renderers by element class, with 33
  registrations in `BuiltinElementRenderers.install()`.
- **A GUI-side per-type descriptor already exists, with a totality test.**
  `PaletteEntry`/`Palette` are the #78 two-layer split's GUI half, and
  `PaletteContractTest.paletteIsTotalOverTheElementRegistry`
  (`test/jls/edit/PaletteContractTest.java:48`) already asserts set *equality*
  against `ElementRegistry.all()` minus a named three-tag exemption set.
- **A headless, declarative symbol vocabulary already exists** — for gates:
  `jls.elem.GateOutline`, whose javadoc says in as many words that it is "the
  pattern later waves continue across the rest of `jls.elem`". `GateRenderer`
  rebuilds an AWT path from it per draw.
- **A palette-sweep export test already exists.** `test/jls/ElementDrawSmokeTest`
  loads a fixture with one of every drawable type, exports it on both the raster
  and SVG paths, and has a reflective completeness sweep that fails when a
  drawable class is neither in the fixture nor exempt.
- **A worked, normative-grade design for the symbol question already exists.**
  `docs/standards-adoption/01-iec-ieee-symbols.md` §"Implementation procedure"
  specifies `jls.core.SymbolSet {DISTINCTIVE, RECTANGULAR}`, `GateOutline`
  labels, frozen put geometry, a `-symbols` batch flag and a preference — and
  states up front: *"this is a render mode, not a per-element strategy and not an
  extension point."*

## The finding that decides the review

`FieldExtend` and `RegisterFile` are palette elements (`src/jls/edit/Palette.java:156,160`),
are in `ElementRegistry`, round-trip, simulate — and have **no registered
renderer**. `ElementRenderers.draw` looks up `el.getClass()` exactly, misses, and
falls through to `ElementRenderSupport.drawHighlight`, which paints nothing at all
unless the element is selected. Two elements a student can place currently draw as
empty space, on screen *and* in every exported figure. `ElementDrawSmokeTest` does
not catch it: it asserts the PNG decodes and the SVG is well formed.

That is precisely "print-symbol coverage decaying one element at a time" — it has
already happened, it is shipping, and the mechanism #723 proposes would not have
caught it, because the decay is in the *renderer* map, not in a print-symbol map
that does not exist. Building a second, print-only registry-keyed table with its
own ratchet, over a first table that is silently non-total, is the wrong seam.

## The reframing: symbol style is a theme axis, not a device axis

There is no such thing as a "print symbol" in JLS's architecture, and there should
not be. An AND gate's distinctive shape is not a property of paper; it is a
property of the *symbol convention* the reader was taught. What varies with the
device is colour, chrome and font metrics — which is exactly and only what #707
already scopes. What varies with the audience is ANSI/IEEE-91 distinctive vs.
IEC 60617-12 rectangular, and a US-textbook course and a European course want
different answers *on screen as well as in the handout*. Cutting the seam at
print-vs-screen makes the handout stop looking like the editor, which is
pedagogically worse than the status quo — students compare the two.

Concretely, in place of AC-1/AC-2/AC-4:

1. **Make the existing renderer map total.** Key `ElementRenderers` off
   `ElementRegistry` tags (or keep classes and derive the expected key set from
   the registry plus `Wire`), delete the silent fallback, and add
   `ElementRendererTotalityTest` with a written exemption set. It goes red today
   for `FieldExtend` and `RegisterFile` — a *real* falsification transcript, no
   scratch element needed, and a real user-visible defect fixed as the first
   fruit of the task.
2. **Generalise `GateOutline` to `ElementOutline`** (segments + the `Label` record
   doc 01 already sketches) as the single geometry vocabulary, and let each
   renderer be `(outline, SymbolSet, Theme) → backend`. "No third vocabulary
   exists" then holds *by construction*, and needs no test asserting a negative.
3. **Adopt doc 01's `SymbolSet` as the style axis**, reachable from the GUI menu,
   `UserPrefs`, and a `-symbols` batch flag, with batch never reading prefs. The
   print pipeline then does not own a symbol set; it picks a default like any
   other caller.

This makes CAP-24's risk 2 disappear rather than police it, and it pays three
other consumers at once: #43/#44 conformance needs the symbols *on screen* (its
whole claim is about what JLS draws); #537's CircuiTikZ emitter needs **symbol
identity** (`\node[and port]`), not pixel geometry, so the mapping it can use is
type → symbol id, not type → path data; #546's tactile SVG needs its own stroke
and simplification policy over the same identities. A print-only geometry table
serves the first of those poorly and the other two not at all.

## Acceptance criteria I am explicitly disregarding

- **AC-1 and AC-4 as written.** A separate print-symbol mapping contradicts the
  recorded design in `docs/standards-adoption/01-iec-ieee-symbols.md` (render
  mode, not per-element table) and duplicates `ElementRenderers` and
  `PaletteEntry`. Note also that `ElementType`'s javadoc forbids GUI concerns in
  the core descriptor — so "registry-keyed" here can only mean a tag-keyed
  GUI-side table, i.e. `PaletteEntry`, which already carries a totality test. If
  the table is kept at all, it is a field on `PaletteEntry`, not a new class, and
  AC-2 is then two lines added to `PaletteContractTest`.
- **AC-2's domain.** "Every registered element type" is the wrong key set for a
  drawing question in both directions: `WireEnd`, `TestGen` and `SubCircuit` are
  documented non-placeable types, while `Wire` is drawn everywhere and is not a
  registered type, and `State`, `Cross`, `HLine`/`VLine` are drawn without being
  element types. The drawable set is the renderer map's key set, not the
  registry's.
- **AC-3's committed red-run transcript.** A transcript is a one-time artifact
  that decays; #315's own I2 prediction (a fixture table under `test/` that a
  standing lint flags) is the durable form. Prefer the standing negative test.

## Where this sits in the larger arc

The issue claims to reuse "the FEAT-001 registry-totality pattern, not a new
one" — but FEAT-001's reusable base (#315 TASK-0002, `RegistryTotalityTestBase`)
is neither filed nor landed; `grep` finds no such class. What exists is six-plus
hand-rolled totality tests, and #723 would make it seven, plus an eighth shape in
`src/jls/collab/op/ElementVocabulary.java` (a hardcoded tag list) and a ninth in
`LogicElement`'s `permits` clause. The genuinely leveraged move is to land the
base *first* and make the symbol check its first client in four lines — that is
what #315's "each capstone otherwise pays the same audit privately" is warning
about, and CAP-24 is about to become the example.

Ordering is also inverted: #707 (print theme, chrome suppression, deterministic
metrics) is `ordering_after: [TASK-C540-1]`, yet #707 is what actually defines how
a print render differs from a screen render. Under the reframing the dependency
straightens out — #707 becomes the device axis, this task becomes the style axis,
and neither blocks the other.

## Verdict

**redirect.** The outcome is right and worth buying; the object being built is a
second table over a domain the tree already keys, in a place the tree already
recorded a different and better decision for. Point this task at: (a) making
`ElementRenderers` total and fixing the two blank elements it exposes, (b)
generalising `GateOutline` into the one symbol vocabulary, (c) adopting
`SymbolSet` from `docs/standards-adoption/01-iec-ieee-symbols.md` as the style
axis for screen and print alike. Then #725 authors *symbols*, not *print symbols*,
and #43/#44, #537 and #546 inherit them instead of re-deriving them.
