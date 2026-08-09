# Issue #707: TASK-C536-1: a print theme and a bundled font make figure rendering deterministic — no screen chrome, no OS font fallback, one Theme seam
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

The end being bought is not "a print theme". It is: *a figure exported from a
circuit looks the same everywhere and can live in a course repo under version
control* (#505 §1.4, KC-24-1). This task is the substrate under that: colours and
chrome for print, and text metrics JLS owns.

Both halves are aimed one layer too shallow. The issue scopes determinism to "the
figure render path" and scopes styling to "a variant of the `Theme` record".
Neither seam is where the non-determinism or the styling actually lives in this
tree, and the narrow scoping produces a figure that is byte-stable and still
wrong.

## What the tree says

- **`Theme` is a record of ten `Color`s applied through mutable global statics.**
  `Theme.apply()` rewrites `JLSInfo.Colors.*` (`src/jls/JLSInfo.java:133-151`);
  `Theme.ALL` is the *user-facing menu list*, looped into the View menu at
  `src/jls/JLSStart.java:1944-1960` and persisted by `UserPrefs.rememberTheme`.
  "Register a print theme through the existing registry-keyed seam" therefore
  means: (a) a **process-global mode switch** taken and restored around an export
  that, in the GUI, races EDT repaints of the open editor; and (b) a "print" row
  in the user's theme menu that `UserPrefs` will happily persist as their editor
  palette unless the registry grows a hidden-variant concept — at which point it
  is no longer "the existing seam", it is a second concept wearing the same record.
- **Chrome suppression is not a colour.** Grid, selection marks, watch fill, hint
  strip are *whether to draw* decisions living in the renderers and
  `SimpleEditor`, not entries in a `Color` record. AC-2 asks a palette type to
  carry a draw policy. That is the category error that will make this record grow
  booleans forever.
- **Metrics non-determinism is not confined to the render path.** Element sizes
  are computed *from* text metrics at `init`/load time — 20+ sites across
  `jls.elem` (`Memory.java:166`, `Register.java:207`, `State.java:195`,
  `Decoder.java:163`, …) — and `width`/`height` are omitted from the save and
  recomputed on load when `sizeIsRecomputedOnLoad()`
  (`src/jls/elem/Element.java:231-256`, #21). The GUI recomputes them with host
  metrics (`CircuitRenderer.draw` → `finishLoad(SwingTextMetrics.forGraphics(g))`,
  `src/jls/edit/CircuitRenderer.java:91`); headless passes `null` and keeps the
  saved numbers. `SvgExportTest`'s own header says it out loud: "text layout
  coordinates depend on the JDK's font metrics, which differ across machines".
- **So a bundled font used *only* in the figure path creates two metric regimes
  in one drawing.** Boxes sized under host metrics, glyphs advanced under bundled
  metrics: labels overflow their boxes, and if you instead re-`init` under bundled
  metrics for the export, ports move while wire ends stay at their saved absolute
  coordinates — a camera-ready figure with wires visibly detached from pins. AC-3
  and AC-4 both pass in that world.
- **The seam for owned metrics already exists and is already AWT-free.**
  `jls.core.TextMetrics` (#77) is exactly this interface, in a package
  `HeadlessCoreRatchetTest` forbids AWT in. Its javadoc records the coupling the
  issue never mentions: an implementation must match `FontMetrics` "so element
  geometry — and therefore saved-file bytes — is unchanged".

## Redirect: one font for the whole program, not one for the figure path

Make a bundled font **JLS's only font**, installed once at startup, backing both
`SwingTextMetrics` (via `Font.createFont` on the in-jar TTF) and a new pure-Java
`jls.core.JlsFont` that reads advance widths straight out of `hmtx`/`cmap` in font
units and scales with fixed integer rounding. Do not measure through
`FontMetrics` even on the bundled font: AWT advances still pass through the host
rasterizer's hinting and fractional-metrics settings, which vary by JDK build.
Owning the *table*, not just the file, is what makes AC-4 true by construction
rather than by three-platform CI luck.

What falls out for free, none of it in this issue's scope:

1. **Circuit geometry becomes host-independent** — a circuit drawn on macOS opens
   on Linux at identical sizes. Today it does not; it degrades quietly.
2. **Collaboration converges.** Every `jls.collab.op` mutation re-`init`s elements
   with the *local* peer's metrics (`AddElements.java:61`,
   `SetElementConfig.java:66,120`, `RotateElement.java:31`, `FlipElement.java:26`),
   so cross-OS peers compute different geometry for the same op.
3. **Save and snapshot bytes stop depending on the host** — what `CircuitSnapshot`
   (#18) and the reproducible-build discipline both want anyway.
4. **`test/jls/ui` layer 3 (render-to-image goldens, #91) becomes possible**, and
   the `fontconfig`/`fonts-dejavu-core` deployment dependency the README admits to
   for headless export — a live failure in minimal autograder containers — is gone.

That is the difference between a figure feature and a project-wide invariant. The
same 2–2.5 mw buys the invariant; the issue as written spends it on the figure.

## Second redirect: a render profile, not a theme variant

Replace "print is a `Theme`" with a value threaded through drawing —
`RenderProfile(Palette palette, ChromePolicy chrome, TextMetrics metrics)` — passed
into `CircuitRenderer` and `ElementRenderers.draw`, with the ~126 hardcoded colour
sites and the `JLSInfo.Colors` statics migrating onto it. `Theme` stays what it
is: the user's screen colour preference, one input that *builds* a profile.

This is the same sweep ARCHITECTURE.md records dark mode as blocked on ("~126
hardcoded chrome/canvas color call sites … #76's follow-up"). Print, dark, and the
accessibility capstone's tactile SVG (#505 Open Question 5) are then three values
of one type instead of three excursions through a global. It also satisfies AC-1's
real intent — *one vocabulary, not a parallel one* — far more strongly than
registry membership does: an argument cannot fork, a global can be shadowed.

## Alternative B, worth pricing before either: quantize geometry to the grid

`Memory.java:166` and `RegisterFile.java:128` already round metric-derived widths
to the grid: `(fm.stringWidth(s) + s/2)/s*s`. If **every** metric-derived size were
grid-quantized, per-host advance differences of a few pixels would collapse to zero
almost always — cross-host geometry identity, no font bundled, no jar growth, no
startup cost. It does not survive labels that cross a grid step, so it is not
sufficient alone, but it is a one-day experiment that says how much of the gap is
metrics and how much is layout policy. This is what KC-24-1's demo slice should
measure before either redirect is funded.

## Third: byte-identical is not the property instructors need

JFreeSVG emits `<text>` with font attributes; the export is
`ElementRenderers.draw(svg, el)` over an `SVGGraphics2D`
(`CircuitRenderer.java:314-358`), and there are ~177 `drawString`/`getFontMetrics`
sites feeding it. Owned metrics fix the *coordinates* in the file. They do not fix
what the file *looks like* on a machine whose viewer resolves `SansSerif`
differently — the LaTeX build, the browser, the reviewer's diff render. The
instructor-visible failure survives AC-4 intact.

Emit figure text as **glyph outlines** from the bundled font. The figure becomes
viewer-independent, and CAP-24 Open Question 3 answers itself: a direct PDF writer
over a drawing that contains only paths needs no font embedding, subsetting, or
`ToUnicode` machinery at all. The honest cost is selectable/searchable text; pay it
back with per-run `<title>` in SVG and `/ActualText` in PDF, both deterministic.

## On the acceptance criteria I am disregarding

- **AC-1** ("a variant of the existing `Theme` registry"): rejected above. Keep the
  no-forking intent; change the mechanism to a parameter.
- **AC-3** ("the figure render path never consults a host font"): too narrow, and
  satisfiable by setting the `Graphics` font while still measuring through AWT. The
  assertion worth writing is that *no* JLS path — editor, load, collab, export —
  measures anything but `JlsFont`.
- **AC-5 / KC-24-4** ("no measurable startup cost"): as stated, this ratchet pushes
  toward the worse design — the only way to add zero startup cost is to keep the
  bundled font lazy and export-local, which is the two-metric-regime trap. One
  `Font.createFont` plus a parsed advance table is single-digit milliseconds against
  a Swing/FlatLaf startup. Re-baseline the ratchet with the measurement recorded;
  do not let it choose the architecture.

## What I would fund instead, in order

1. Grid-quantization experiment (Alternative B) — cheap, and it sizes the rest.
2. `jls.core.JlsFont` + bundled TTF as the program's single font, with a parity
   test against today's `SwingTextMetrics` values so the #21 geometry churn is a
   recorded one-time event rather than a surprise in someone's saved circuit.
3. `RenderProfile` threaded through `CircuitRenderer`/`ElementRenderers`, absorbing
   the `JLSInfo.Colors` statics; print and dark land as data on day one.
4. Outline-text export mode; the SVG and PDF writers then ride it.

Steps 2 and 3 each exceed this task's 2–2.5 mw band. That is the honest finding:
the band is sized for the shallow version, and the shallow version buys a
deterministic figure of a non-deterministic circuit.
