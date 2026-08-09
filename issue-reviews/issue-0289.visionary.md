# Issue #289: Dark mode: FlatDarkLaf plus a dark Theme variant, currently impossible because ~113 draw sites hardcode black foregrounds
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "dark mode." The real end state is: **JLS's canvas has a palette, and every
mark drawn on it resolves through that palette.** Dark mode is the first
customer of that property, and the reason the project noticed it is missing.
Three other open items (#729/#734's per-theme CVD screenshot matrix, #707's
print theme, #727's handout figures, #876's framebuffer proof) are also
customers of the same property. If dark mode ships as a black-literal sweep,
those customers each re-derive the plumbing; if it ships as *the canvas gets a
palette parameter*, they all get it free. That is the alignment question here,
and it is worth reframing the task around.

I endorse the outcome. I am reframing the seam, the P1 gate, and the
acceptance bar, and I am proposing a delivery split that ships half the user
value before the sweep starts.

## Reframing 1 — cut at "the palette is a global static," not at "black is hardcoded"

`Theme.apply()` does not pass a theme anywhere; it *rewrites eleven mutable
public statics* on `JLSInfo.Palette` (`Theme.java` `install()`,
`JLSInfo.java:125-152`). Every draw site reads the ambient global. That single
fact is what makes the rest of this issue hard:

- **§7.1 (print/export must pin light) has no clean implementation.** With a
  global palette, "pin light for export" means save the statics, install
  `DEFAULT`, render, restore — on a code path (`CircuitRenderer.exportImage`,
  `addToBook`, the `Printable`s in `StateMachineRenderer` /
  `InteractiveSimulator`) that can run while an editor repaints. It is a
  save/restore dance around a mutable global, and it is untestable except by
  observing pixels.
- **The per-theme screenshot matrix (#734/#729) inherits the same hazard.**
  `ThemeTest` already needs `@BeforeEach restoreDefaultTheme()` purely because
  applying a theme is a global side effect. Multiply that by "render the adder
  lab under every shipped theme × three dichromacies" and the test suite is
  carrying global-state discipline forever.

The project has *already done the hard part* of the fix. Issue #77 pulled all
drawing out of the model into `src/jls/edit/*Renderer.java` — 31 renderers
behind one interface, `ElementRenderer.draw(Graphics g, Element el)`
(`ElementRenderer.java:24`). The seam exists. Widen it once:

```java
void draw(Graphics g, Element el, Theme theme);   // or a small Ink/RenderContext
```

and `Theme` becomes a value threaded down instead of a global rewritten
sideways. Consequences: `exportImage(file, Theme.DEFAULT)` *is* §7.1, no
save/restore, no race, and a one-line unit test. Rendering the same circuit
under N palettes in one process becomes ordinary. `JLSInfo.Palette` can stay as
a thin compatibility shim seeded from the active theme until callers migrate,
so this is not a big-bang change — it is the same 113+ call-site edit the issue
already plans, spent on a better structure. **Doing the sweep without widening
the seam spends the whole budget and leaves the global in place.**

## Reframing 2 — P1 counts the wrong population, and the residue is not inert

Re-derived at the current checkout:

```
Color.black|Color.BLACK  in src/jls        113   (issue's figure — correct)
all colour literals in src/jls/edit        182   (Color.* or new Color(...))
  of which: white 16, gray-family 20, other named 44, new Color( 14
```

Two of the non-black residue sites are load-bearing for dark mode, today:

- `ElementRenderSupport.drawPut` fills every *unattached* connection point with
  `Color.WHITE` (`ElementRenderSupport.java:58`), ringed in black. On a dark
  canvas that is an inverted, glaring dot on every free pin of every element —
  and it survives a black-only sweep untouched.
- `ElementRenderSupport.drawHighlight` hardcodes `Color.pink`
  (`ElementRenderSupport.java:34`), used by all 25 element renderers. **This is
  a live theming defect, not a dark-mode one:** `Theme.DEFAULT.highlight` is
  lavender `0xB8A5E3`, and `WireRenderer:67` honours it — so today, selecting
  the default theme gives you lavender highlighted *wires* and pink highlighted
  *elements*. The theme system is already half-bypassed and nobody has noticed
  because pink and lavender both read on white.

So change the gate. **P1 should be "every colour literal in the renderer layer
resolves through a palette role; the allowlist is non-empty and every entry
carries a written reason"** — which is exactly the anti-vacuity ratchet the
second comment imported from #381. Adopt that as *the* gate and let the 113
figure be a progress statistic, not the definition of done. A gate that reads
"113 → ~0" is satisfiable while leaving 69 literals that make the feature look
broken.

## Reframing 3 — the acceptance bar does not measure the thing dark mode breaks

`ThemeTest`'s ≥ 25 CIE76 bar is **pairwise between wire states**
(`ThemeTest.java:33-34, 81-111`). It never compares any role to
`Theme.background()`. `DEFAULT` passes partly by luck: the paper is white and
the ink is black. A `DARK` palette can satisfy every pairwise constraint and
still be unreadable — ten mutually-distinct dark colours on a dark ground.

H1 as written is therefore falsifiable in the wrong direction: it can come back
green on a palette no student can read. Add a **contrast-against-background
floor** per role (WCAG relative luminance, ≥ 3:1 for graphical marks, ≥ 4.5:1
for text-bearing roles). The machinery is already in the file — sRGB→linear
lives at `ThemeTest.java:186`. This is maybe thirty lines, and it is what makes
"CVD-safe dark mode" a claim rather than a hope. It also retroactively pins
`DEFAULT` and `CLASSIC`, and gives #707's print theme a bar to meet.

## Reframing 4 — decouple app appearance from canvas theme, and ship the cheap half now

H2 welds two things of wildly different cost into one deliverable:

- **L&F dark** — `FlatDarkLaf` is already on the classpath (`pom.xml`), the
  seam already exists (`JLSStart.lookAndFeelClassName()` accepts any class
  name), and the fallback path is already non-fatal. This is a menu item and a
  prefs key. Hours.
- **Canvas dark** — 182 literals, a new palette, a new contrast bar, an
  export-pinning contract, and a screenshot matrix. Weeks.

Nothing forces them to land together. Chrome-dark alone is a real ergonomic win
(menus, dialogs, help viewer, trace window, every modal a student opens), and
it is testable today by `LookAndFeelPolicyTest`. Ship `appearance =
{light, dark}` first; ship `canvas theme = {default, classic, dark}` when the
sweep lands; let #76 own the final "they flip together by default" integration.
The issue's own §14 already lists #286 as a hard blocker — splitting removes
that blocker from the half that doesn't need it.

There is also a **duplication risk between #289 and #286** worth flagging to
#76: #286 proposes chrome roles on `Theme` *seeded from `UIManager` defaults*.
If chrome colour is derived from the L&F, then installing `FlatDarkLaf`
restyles chrome by itself and the `Theme` chrome roles are a second source of
truth for the same pixels. Pick one: either chrome follows the L&F (and #286
shrinks to "stop hardcoding cyan/yellow/white, read `UIManager`"), or chrome
follows `Theme` (and the L&F is a subordinate detail of the theme). Shipping
both mechanisms is how a codebase acquires a third colour system.

Which it already has a *second* one, unmentioned by this issue: `UserPrefs`
persists free `gridColor`/`backgroundColor` overrides layered on top of the
theme (`UserPrefs.java:36, 100-113`), settable via `JColorChooser` from the
Global menu (`JLSStart.java:1978, 2002`). `rememberTheme` clears them, so
light→dark is safe; the reverse is not — pick dark, then pick a background
colour, and you get dark-tuned ink on whatever the user chose, with no
validation anywhere. These overrides are a pre-`Theme` relic. The dark work
should **decide their fate** (fold into a derived custom theme, or retire them
with migration), not silently inherit a third colour source into a dark world.

## What I would keep exactly as written

The `blocked_by: [286]` ordering; the `WAIVED:`-only relaxation rule with a
third encoding channel (#731) as the preferred escape; the pixel-parity
requirement for light/classic post-sweep (#24 baselines) — that is the right
instrument for a mechanical diff this large; the parse-with-fallback migration
story; and the refusal to ship an ambiguous palette. The issue's evidence
discipline is genuinely good: it caught its own stale `~126` and said so.

## Concrete recommendation

Keep the issue and its owner. Amend it to: (1) widen `ElementRenderer.draw` to
carry the palette as the first commit, with `JLSInfo.Palette` kept as a shim;
(2) replace the P1 black-count gate with the renderer-layer literal ratchet
over all 182 sites, allowlist non-empty and justified; (3) add a
role-vs-background contrast floor to `ThemeTest` before authoring `DARK`;
(4) split the L&F half into its own landing so students get dark chrome this
milestone; (5) resolve the `UserPrefs` colour-override relic and the
#286-vs-L&F chrome ownership question on #76 before either task starts.
