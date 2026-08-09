# Issue #542: FEAT-C26-1: every wire state survives grayscale — tritanopia joins the verified set, and thickness, dash and glyph carry state when color carries nothing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

Not "more accessibility channels." The end this serves is: **a JLS circuit's runtime
state should be a nameable datum, not an emergent property of pixels.** CAP-26 (#507)
proves it — PF-3 needs to *speak* wire state, PF-4 needs to *narrate* it and emboss it,
PF-5 needs to *cite a test for* it, and PF-1 needs to *draw* it without color. Four
surfaces, one missing artifact. This issue frames itself as a theming ratchet over #76,
which is the smallest and least durable reading of its own outcome.

I endorse the outcome. I am explicitly disregarding AC-2's screenshot apparatus and
AC-4's pixel-identity gate as written, for reasons below.

## Reframe 1 — the deliverable is a wire-state vocabulary in the headless core

The Outcome enumerates five states: "high, low, HiZ, bus values, error." The code has
**three** drawn value states. `WireRenderer.draw` branches on `touching / highlighted /
null / non-empty / empty`; `strokeFor` maps exactly `{null → dashed, non-zero → thick,
zero → thin}`. There is no "high" vs "bus value" distinction — a 1-bit 1 and a 32-bit
`0x2A` render identically. And "error" is not a visual state at all: the bus conflict
detected in `src/jls/elem/WireNet.java:456-478` (#98) is reported as a *message* and
never reaches the canvas.

So the claim "strictly stronger than the shipped color-distance floor" is false in a way
that matters. Two of the five named states are not weaker-encoded today — they are
**unencoded**, in full color, for trichromats. This feature is not a ratchet over #76's
floor; it is where JLS first decides what a wire state *is*.

Cut there. `jls.core.WireState` (or `jls.elem`), headless, AWT-free: a closed vocabulary
`{HIGH, LOW, FLOATING, BUS(value,width), CONFLICT}` with a canonical name and a
short spoken/printable rendering per state, derived from `WireNet` — plus a
`WireStateEncoding` record `⟨strokeWidth, dash, glyph, colorRole⟩`. Color becomes *one
projection* of the datum. Then #543's preview, #546's tactile depiction, PF-3's Orca
announcement, PF-4's prose narrative, the status line, and VCD export all read the same
object instead of each re-deriving state from `BitSet` nullness. The grayscale claim
falls out; it stops being a rendering trick.

This also fixes the surprise the current code hides: `touching` and `highlighted` are
color-only overrides that *replace* the value color, and `WireEndRenderer` gives touch a
ring but highlight nothing. Under grayscale, a highlighted non-zero wire and a
highlighted zero wire are the same pixels today. A vocabulary makes that a totality
failure instead of a discovery.

## Reframe 2 — visual channels do not scale; the scaling channel is the value itself

Thickness, dash and glyph are a ~3-4 value alphabet before schematic legibility dies —
which is exactly what KC-26-1 anticipates. They cannot carry "bus values," and no
escalation will. The honest architectural answer: **a bus's state is a number, and the
only encoding of a number that survives grayscale, tritanopia, projection washout and
swell paper is the number.**

The out-of-the-box move CAP-26 never considers: render values inline. JLS already has
the machinery — `Wire` carries an optional probe label drawn by `WireRenderer`, and the
watch/status path formats values. Draw the value adjacent to the wire above a zoom
threshold (or on a "show values" toggle), the way an oscilloscope annotates a trace.
That single feature serves the colorblind student, the student reading a photocopied
handout, the instructor projecting onto a washed-out screen, *and* the ordinary sighted
student who today must hover-probe to read a bus. It is the one change here whose value
is not conditional on a disability. Glyph escalation should stop at the four core states
and hand buses to text.

## Reframe 3 — the seam is wrong, and the project has already recorded that

The issue says the encoding "composes over the existing Theme seam." That seam is
`Theme.install` writing **mutable global statics** (`JLSInfo.Palette.*`, 46 read sites
across 14 files). `docs/extension-points.md` already lists `gui.theme` as *pending* with
the contract "theme/preferences object replacing `JLSInfo` statics (#76)." Building the
state-to-encoding registry beside the statics builds it on a seam the project has
recorded as being demolished.

Worse, the global blocks the very sibling this feature is ordered before. #543 wants a
live CVD preview; the natural instructor gesture is side-by-side ("normal | as your
deuteranopic student sees it"), and a global palette cannot render two views in one
frame. It also serializes every test that touches color — `ThemeTest` already needs
`@AfterEach restoreDefaultTheme()` to clean up after itself.

Concrete alternative: widen `ElementRenderer.draw(Graphics, Element)` to
`draw(Graphics, Element, RenderStyle)`, where `RenderStyle` carries the palette *and*
the state-encoding table *and* an optional post-filter. ~30 renderers, mechanical, and
the wave is young enough (#77) that the cost never gets lower than today. Grayscale then
is a `RenderStyle`, not a mode; CVD preview is a second draw pass; the tests are pure.
Land `gui.theme` as that object and this feature stops being a special case.

## Reframe 4 — the apparatus already exists, headless, and is not a screenshot matrix

The consolidated-scope comment correctly notes that three issues each specify the same
instrument. My objection is stronger: **none of them should build it.** JLS already
renders a whole circuit offscreen with no display — `CircuitRenderer` (`src/jls/edit/
CircuitRenderer.java:312-358`) draws to a `BufferedImage` or JFreeSVG for `-i` export,
exercised by `CliImageExportTest`. #543's framebuffer-level decision means the CVD
transform is a per-pixel matrix. Compose the two and AC-2 is:

    export the adder lab headlessly → apply {identity, protan, deutan, tritan, gray}
    → assert every state-pair's rendered footprint differs by a non-color measure

That is an ordinary `mvn verify` test with no compositor, no Xvfb, no #91 layer 3, no
Wayland rig, and no flaky GUI capture. `WireValueChannelTest.renderedInkDiffersByValue
StateAloneNotJustColor` is already this pattern at wire scale. Delete "automated
screenshot test" from AC-2 and say "headless render fingerprint" instead.

The shared primitive should be a real class, not test-private: `ThemeTest` currently
carries its own copy of the Viénot matrices (lines 143-234). Promote that to
`jls.core.ColorVision` (AWT-free, `int[]`/`float[]` in and out), have `ThemeTest`, the
new render test, and #543's live filter all use the one copy. That *is* resolution (a)
of the ordering-inversion comment, made concrete — and it removes the duplication rather
than just declaring who owns it.

**Technical trap in AC-1 as written:** Viénot et al. 1999 is a single-matrix reduction
valid for protanopia and deuteranopia; the authors say plainly it is unreliable for
tritanopia. Adding a "tritanopia matrix" to `ThemeTest` at the same delta-E floor will
produce a number that passes and means little. Tritanopia needs the Brettel/Viénot/
Mollon 1997 two-half-plane construction. Whichever is chosen must be cited in the code,
because #547's VPAT will name this test.

## Reframe 5 — the totality test guards the wrong mapping

AC-3 asks for a registry "so a new element type that lacks an encoding fails the build."
But wire-value encoding is not per element type — it is one closed table for all wires.
What *is* open-ended is per-element state depiction (watch fill, initial state, tri-state
output, memory/display contents) and the ~126 hardcoded-black bodies. So AC-3 as written
guards a mapping the feature never defines.

Split it: (a) the wire-state table is closed — prove it exhaustive with a switch over the
sealed vocabulary from Reframe 1, which the compiler enforces for free; (b) the
per-element encoding hangs off the **already-shipped** #78 element registry
(`jls.elem.ElementType`, `ElementRegistryTest`) as a required accessor, cross-checked the
way `ExtensionPointCatalogTest` cross-checks seams. No new registry. That is the
FEAT-001 lineage the issue invokes, honored rather than re-implemented.

## Reframe 6 — the pixel-identity gate contradicts the feature

AC-4 requires the default theme "pixel-unchanged for existing users … gated on every
commit," while the Outcome adds glyph markers to default rendering. Both cannot hold.
Either the encoding is off by default — and then CAP-26 §1 step 1's "every student in
every section" is served by nothing — or the pixels change.

Restate K9 as what it actually protects: **no color role changes and no legibility
regression**, enforced by a golden-image ratchet with an explicit intentional-update
workflow (goldens are review artifacts, like `BatchSimulationGoldenTest`), not by
immutability. Every project that gates on "pixels never change" either freezes or routes
around the gate.

And ship the honest user-facing form of this work: a **`mono` theme** (grayscale roles +
maximal non-color encoding), selectable and reachable from `-i out.svg`. That is a
printable, projectable, photocopy-safe handout in one release, it is the groundwork
PF-4's tactile SVG needs, and it costs a `Theme` constant plus the encoding table.

## Trajectory

Strengthens the arc: JLS has spent this cycle moving state out of AWT into headless,
inspectable form (#77 renderer split, `HeadlessCoreRatchetTest`, `jls.core`, the
extension-point catalog, VCD and SVG export). A named wire-state vocabulary is the same
move applied to the one thing still trapped in pixels. Pulls against it only in its
current framing — a GUI-side theming addendum bolted to a global that the project has
already scheduled for replacement, verified by a screenshot rig it does not need.

## Revised first slice (replacing the demo slice)

1. `jls.core.WireState` + `WireStateEncoding`, derived from `WireNet` — including
   `CONFLICT`, which finally puts #98's bus conflict on the canvas.
2. `jls.core.ColorVision` promoted out of `ThemeTest`, tritanopia via Brettel 1997,
   cited.
3. Headless render-fingerprint test over `CircuitRenderer`'s existing export path;
   no screenshot matrix anywhere.
4. A `mono` theme shipped, so the work is usable before PF-2..PF-6 exist.

Same 2-3 mw band, and it hands #543, #546, #547 an object instead of a promise.
