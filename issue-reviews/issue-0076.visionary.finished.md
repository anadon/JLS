# Issue #76: Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI-clean icons, dark mode, persistent preferences
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two different goods are bundled under one title. The first is a **correctness
guarantee**: every simulation state is readable by every student, on every
display, under every vision. The second is **modernity**: dark mode, crisp
icons, preferences that stick. The first is architecture; the second is polish.
Bundling them has already cost this issue something concrete — the delta-E
guarantee in `ThemeTest` applies only to the ten `Theme` record constants, while
**113 unconditional `Color.black` draws** (`grep -rn "Color\.black\b\|Color\.BLACK\b" src/jls`)
and the ~12 hardcoded chrome colors sit *outside* the guaranteed set and no test
can see them. The guarantee is real for the colors that go through the seam and
vacuous everywhere else, and the roster's answer is a one-time 113-site sweep
that restores the same condition for the *next* profile (high contrast, print,
#546's tactile, #536's camera-ready).

The durable deliverable here is not "a dark theme." It is **"no ink without a
role,"** mechanically enforced. That reframing is the spine of everything below.

## The seam is cut in the wrong place

The rest of this project has spent a year moving cross-cutting behavior behind
named, typed, testable seams: `SaveTags` replacing `Class.forName` (#79),
`ElementRenderers` pulling `draw` out of 29 element classes (#77),
`ExtensionRegistry` with a catalog totality test (#223), `makeElement(PaletteEntry)`
collapsing 30 hand-built buttons into one table (#78), `TellUser` as the sole
dialog site with a ratchet test (#81). The direction is unmistakable: *make the
cross-cutting thing a parameter, then pin it with a test that fails when someone
routes around it.*

`Theme` does the opposite. `Theme.apply()` rewrites ten mutable public statics
in `JLSInfo.Palette` (`src/jls/Theme.java:147-160`), and 39 draw sites read those
statics directly. This is a global mutable singleton — the presentation-layer
analogue of the pre-#78 element switch and the removed plugin loader. Every
downstream awkwardness in the roster and in the six declared consumers falls out
of that one choice:

- **#289 §7.1 cannot be honored cleanly.** "Exports and printing must pin the
  light/classic palette" means, on a global-statics design, *install a different
  palette around the export and restore it afterwards* — a global mutation on a
  path (`CircuitRenderer.exportImage`, `src/jls/edit/CircuitRenderer.java:301`)
  that is CLI-reachable and already background-adjacent. Note the latent bug that
  exists **today**: that method hardcodes `svg.setColor(Color.white)` /
  `g.setColor(Color.white)` for the background but draws every wire through
  `ElementRenderers` off the live `Palette`. Export under CLASSIC and you get the
  red/green pair back on a white page. Nobody has hit it only because DARK does
  not exist yet.
- **#543 exists in its current form because of it.** Its case for a
  *framebuffer-level* CVD filter is explicitly "so colours outside the `Theme`
  seam are transformed too." The set of colors outside the seam is an artifact of
  the seam being a color bag installed at startup rather than a value passed at
  paint time. Shrink that set to zero and #543's mechanism becomes a one-line
  wrap of the same paint call CI uses.
- **The screenshot matrix is specified four times** (#76 planned task + IC2,
  #542 AC-2, #543 AC-3) — and the dedup sweep already flagged this. It is
  specified four times because no one instrument can express "render this circuit
  in *that* appearance"; each consumer has to build the whole rig.
- **Per-window or per-document appearance is structurally impossible**, which
  also forecloses the obvious settings-dialog affordance (a live sample canvas
  that previews the theme you are hovering).

## The reframing: a paint-time style, not a startup-time palette

Thread a `RenderStyle` value — palette roles, the state→encoding table, and an
optional vision filter — through the seam #77 already built:

```java
public interface ElementRenderer { void draw(Graphics g, Element el, RenderStyle style); }
```

29 implementations, mechanical, one reviewable PR, and `ElementRenderers.draw`
keeps a two-arg overload defaulting to `RenderStyle.active()` so no call site
outside `jls.edit` moves. Then:

- **Dark mode** is `draw(g, el, RenderStyle.DARK)`. The 113-site sweep still
  happens, but it lands into a parameter, so it is the **last** such sweep rather
  than the first of five.
- **Print/export theme-independence** (#289 §7.1) is passing `RenderStyle.PRINT`
  at `CircuitRenderer.exportImage`. One argument, no global save/restore, and the
  existing `SvgExportTest#exportingTwiceIsByteIdentical` keeps guarding it.
- **The CVD matrix** is a headless loop over {styles} × {normal, deutan, protan,
  tritan, grayscale} rendering into a `BufferedImage`. **The apparatus already
  exists** — `exportImage`'s offscreen path plus `jls.ui` Layer 3
  (`RenderAssert`, `RenderBoundsTest`, per `test/jls/ui/package-info.java`). It
  does not need #91's Layer 2, does not need Xvfb, and does not need the PR #266
  boot rigs. IC2's dependency on the #91 harness should be struck; IC4 (crisp
  icons at 200%) is the only genuinely display-bound criterion left, and even it
  reduces to a scaled `AffineTransform` render plus one confidence screenshot.
- **#543's instructor preview** is the same call with a filter in the style —
  precisely the "one code path serves the instructor mode and CI" outcome #543
  already says it wants, reached without a framebuffer post-pass.
- **#542's registry-keyed state→encoding totality** is a field of `RenderStyle`.
  The encoding logic is currently only in three places (`WireRenderer.draw`,
  `WireEndRenderer`, `ElementRenderSupport`), so this is cheap right now and gets
  expensive with every element that grows a state.

**The ratchet is the point.** Once ink flows through a parameter, add the test
this repo would write anyway, in the idiom of `HeadlessCoreRatchetTest` and
`NotificationRatchetTest`: *no `java.awt.Color` literal in `jls.edit` draw
paths.* That converts "we swept 113 sites once" into "ink cannot leave the seam
again," and it is what makes every later appearance profile — high contrast,
tactile, camera-ready, grayscale — a table entry rather than another sweep.

**Ordering consequence, and it is time-sensitive.** The roster's critical path is
#286 → #289. I would insert the style-parameter refactor *before* #289 and treat
it as the real blocker. Doing #289's 113-site sweep against `Theme.active()`
globals means touching all 113 sites a second time when the parameter arrives.
That sweep is the most expensive irreversible edit in this feature; it should be
done once, into the final shape.

## Second reframing: #287 is authoring assets JLS can already draw

#287 proposes hand-authoring 33 SVGs to replace 33 GIFs. But those icons are
pictures of the very elements this codebase renders vectorially — 29
`ElementRenderer`s, and a working JFreeSVG path that turns them into scalable
vector output. Authoring them again by hand creates a **second source of truth
that will drift from the renderers**, needs an artist, produces an unreviewable
PR, and leaves ARCHITECTURE.md's "adding an element" step 13 ("a toolbar icon
gif") in place forever.

Alternative: implement a `javax.swing.Icon` whose `paintIcon` calls
`ElementRenderers.draw` on a prototype element with a scale transform and the
active style. Resolution-independent by construction, **theme-aware for free**
(the dark toolbar needs no second asset set), guaranteed to match what gets
placed on the canvas — which is a genuine pedagogic win, not just a maintenance
one — and it deletes step 13 from the element-authoring list, serving #78's arc
directly. Roughly six palette entries are not elements (`go`, `pause`, `stop`,
`up`, `down`, `text`), so 33 hand-authored assets become ~6. The `makeElement(PaletteEntry)`
seam from PR #246 and `PaletteContractTest` are exactly the place to land it.

I am explicitly setting aside #287's stated method ("replace the 33 GIFs with
`FlatSVGIcon`") and #289's Method step 1 as written. The *outcomes* both assert
still hold; the routes should change.

## Third reframing: preferences want `Attribute`, not more accessors

`UserPrefs` is one method per key over a private `get`/`put`. The planned scope
adds recent files, last directory, window geometry, per-window zoom, and #570
wants a persisted keymap on top — plus a settings dialog that displays all of
them. That trajectory ends in a hand-maintained dialog with N hardcoded rows and
2N accessors.

This project already solved this exact shape once: `jls.elem.Attribute` —
"declarative save/load/dialog plumbing for element parameters (issue #52)." Make
preferences a list of typed `Preference<T>` descriptors (key, type, default,
label, validator) and **generate** both persistence and the settings dialog from
it. That dissolves Open Question 2 (one modal dialog vs. per-item Global-menu
entries): the dialog stops being a design decision and becomes a projection of
the descriptor list. #570's keybinding page and #73's recent-files become
descriptors, not new windows. And it earns a `PreferenceCatalogTest` totality
check in the established idiom of `ExtensionPointCatalogTest` and
`CliFlagTableTest`. Open Question 3 (per-window vs. global geometry) becomes a
per-descriptor scope flag rather than a policy debate.

## Where the issue is right, and what to keep

The capability statement in §1 is correct and worth having, the CVD work that
already landed is the best-evidenced thing in this repo's UI history (a
literature-grounded Vienot-1999 transform run as a unit test rather than an
eyeball check is exemplary), and the second visual channel is the right instinct:
color is one channel and this app needs two. Invariants 2 and 3 (`.jls`
byte-identical; CLASSIC reproduces the old look) are exactly the right
guardrails. The `blocks: [75]` edge being demoted to convention is the right
call. Keep all of it.

What I am arguing is that the remaining roster (#286 → #289 sweep, #287 asset
authoring, a fourth screenshot rig, a hand-built settings dialog) is four
separate one-time efforts that each restore the condition they were meant to
fix, when one seam change plus one ratchet makes all four cheap and makes the
six declared downstream consumers (#355/#381, #542, #543, #546, #570, #596)
into table entries instead of forks. The escalation at the end of the 2026-08-04
dedup comment — "the Theme seam has six declared consumers and no named seam
owner" — is the real finding on this issue. The answer is not to name an owner
for a bag of statics; it is to make the seam a value that can be passed.
