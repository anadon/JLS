# Issue #543: FEAT-C26-2: an instructor previews a handout as their colorblind students see it — protanopia, deuteranopia and tritanopia simulated in-app over the live canvas
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The outcome is right and I would not trade it away: an instructor should be able to find out,
before class, that the handout they are about to print is unreadable to two students in the
room. CAP-26 §1 step 1 needs a human loop, `Theme.java`'s javadoc contract is a *palette*
promise that says nothing about the 113 call sites that never ask the palette anything
(`grep -rn "Color\.black\b\|Color\.BLACK\b" src/jls --include=*.java` → 113 at this
checkout, confirming #289 over the stale "~126"), and the gap between "the palette is safe"
and "this circuit is legible" is exactly where an instructor gets burned. That gap is real.

Where I disagree is the seam. The issue inherits CAP-26 Open Question 5's binary — *theme
level or framebuffer level* — and picks framebuffer. The tree offers a third cut that neither
the capstone nor the three dedup comments considered, and it is strictly better on every axis
those comments argue about. Three facts drive it.

## Three facts from the tree

**1. The CVD transform already exists, in the wrong compilation unit.**
`test/jls/ThemeTest.java:143-205` carries a complete Viénot et al. 1999 dichromat simulation:
`RGB2LMS`/`LMS2RGB` Hunt-Pointer-Estevez matrices, the missing-M and missing-L cone
projections, sRGB→linear-light conversion, and CIE Lab for the delta-E floor. It is
`private static` inside a test. The entire ownership dispute the pass-2 comment adjudicates —
"#542 owns the CVD colour-transform primitive as a testable unit; this feature wraps that same
primitive" — is a dispute over who gets to write code that is already written. Promote it to
main source as a pure, AWT-light `jls.core.CvdSimulation` (a `Color → Color` function per
deficiency), have `ThemeTest` consume it instead of hiding it, and the ordering inversion
between #542 and #543 dissolves without a `REPLAN:` on #507. Neither issue owns the transform;
`core` does, the way `Geometry` and `BitSetUtils` already do.

**2. There is one device-independent paint path, and it is not a framebuffer.**
`CircuitRenderer` draws the same element code into three sinks: a `BufferedImage`
(`:364-365`), a `Printable` page (`addToBook`), and JFreeSVG's `SVGGraphics2D` (`:311-358`,
"so .svg output needs no per-element work"). Elements draw through the registry-keyed
`ElementRenderers` (`:21-51`). `SimpleEditor.paintComponent` (`:2448-2524`) is a fourth sink
over the same `CircuitRenderer.of(circuit).draw(...)`. The canvas is *pure line art*: zero
`drawImage`, zero `setPaint`, zero `GradientPaint`, zero `AlphaComposite` anywhere in
`src/jls`. Every colour on that canvas — themed or hardcoded — passes through `setColor` on a
`Graphics2D`.

That makes a **paint-time colour decorator** — a `Graphics2D` wrapper that maps every colour
through `CvdSimulation` on its way in — behaviourally equivalent to a framebuffer filter for
this application's content, while beating it everywhere else:

| | theme level | framebuffer | **paint-time decorator** |
|---|---|---|---|
| catches the 113 hardcoded sites | no | yes | **yes** |
| works on `-i` PNG/SVG export and print | no (and see fact 3) | no — vectors have no framebuffer | **yes** |
| testable with no display | yes | needs a rig | **yes** (`ElementDrawSmokeTest` shape) |
| cost when off | zero | a per-frame branch in the paint path at best | **zero — `g` passed through unchanged** |

The AC-2 test as written (a named hardcoded call site comes through transformed) passes under
framebuffer *and* decorator; it only falsifies the theme-level cut. So AC-2 does not actually
defend the framebuffer decision — it defends the decision the decorator also satisfies.

**3. Theme-level is not merely weak, it is unsafe — and the issue never says why.**
`Theme.install` (`Theme.java:139-152`) mutates `JLSInfo.Palette` *global statics*. A
theme-level preview would therefore silently change what `jls -i` writes and what the print
path emits while preview is on. The decorator is per-`Graphics`, per-paint, and holds no
global state at all. That is the argument OQ5 should have been decided on, and it points at
the third option rather than at framebuffer.

## The larger reframing: a handout is an exported artifact, not a live canvas

The title says *previews a handout*. A handout is a PNG, an SVG, or a printed page — produced
by `JLSStart`'s `-i` flag (`:764-765`, `:353-355`), which already runs headless in the
published container image. The instructor's actual workflow ends at a file. A live-canvas
filter cannot preview that file at all if the filter lives in the framebuffer, because the SVG
path never rasterizes.

So invert the deliverable order. The primary artifact should be an **accessibility contact
sheet plus a verdict on the instructor's own circuit**:

```
jls -i adder-lab.png --cvd=all
# writes adder-lab.png plus normal/protan/deutan/tritan/grayscale panels,
# and reports: "wireZero vs wireOff indistinguishable under tritanopia
#               (delta-E 11.3 < 25) at 4 wire runs — see panel 4"
```

This is stronger than a preview on three counts. It runs headless, so CAP-26 AC-1's
`CvdStateDistinguishabilityTest` becomes an ordinary JUnit test in the existing layer-3
render-to-image harness (`test/jls/ui/package-info.java`, `RenderAssert`/`RenderBoundsTest`)
with no Xvfb and no dependence on the #101 Wayland rig — which #543 AC-3's "same code"
requirement then satisfies *by construction* rather than by discipline. It runs in the
autograder container, so an instructor can gate a course-materials repo in CI. And it does not
ask a trichromat to exercise perceptual judgement they do not have: a simulated image still
has to be *judged*, and the machine already knows the delta-E answer that `ThemeTest` computes
today. CAP-26 §3 risk 1 says automated checks are necessary but not sufficient; that is an
argument for keeping the visual panels, not for making the eyeball the only instrument.

The in-app live toggle then survives as what it should have been all along: a thin consumer —
install decorator, repaint — worth perhaps a tenth of the band, useful for the "wiggle the
circuit and watch" moment, and no longer the thing the CI apparatus has to be bent to fit.

## A fidelity risk the issue and the capstone both miss

`ThemeTest`'s Viénot 1999 method is a **single-plane** simplification whose own paper restricts
it to protanopes and deuteranopes. Adding a `TRITANOPIA` matrix in the same style produces a
picture that is wrong in the blue-yellow direction — and an instructor who ships a handout
because a *wrong* simulation looked fine is worse off than one who never previewed. Tritanopia
needs the two-plane Brettel-Viénot-Mollon 1997 construction (or Machado 2009 if severity
grading is ever wanted), cited by year in the source, with a test pinning known anchor colours.
This belongs in the promoted `CvdSimulation` from fact 1, once, rather than being rediscovered
in #542's leg, #876's filter, and #76's matrix.

## What I am disregarding, and what I keep

I am **disregarding AC-2's "operates on the rendered framebuffer"** as the specification of
mechanism, and OQ5's framing as a two-way choice. I keep AC-2's *property* verbatim — colours
outside the `Theme` seam are transformed, proven at a named hardcoded call site on-screen in
the shipped adder lab — because that is the criterion that can fail, and the decorator passes
it. If the maintainer wants the framebuffer answer preserved as an audit, there is a better
criterion available than either body wrote: **assert the live decorator output and an offline
per-pixel filter over the exported PNG agree within tolerance on the adder lab.** That pins the
decorator's one real approximation (antialiased edge pixels are transform-then-blend rather
than blend-then-transform) instead of leaving it assumed, and it costs one test.

I keep AC-4 whole, including #736's cost clause — under the decorator it is satisfied by
construction rather than by vigilance, since the off path hands `paintComponent` the same
`Graphics` it has today. I keep #877's keyboard-reachability criterion; a CAP-26 feature whose
own control is unreachable would be self-refuting. I keep the #543-vs-#76 boundary: a
user-facing mode and a CI artifact are different outcomes.

## Concrete roster this implies

The #876/#877 split cuts along framebuffer-vs-GUI. Cut it along **pure-vs-plumbed** instead:

1. **`jls.core.CvdSimulation`** — the three transforms promoted out of `ThemeTest`, tritanopia
   on the correct two-plane construction, anchor-colour tests, `ThemeTest` refactored onto it.
   No GUI, no rendering, no ownership dispute with #542. Days, not weeks.
2. **`--cvd` on the export path** — contact sheet plus the distinguishability verdict over the
   instructor's own circuit, headless, container-runnable; this *is* AC-1's apparatus.
3. **The live in-app toggle** — decorating `Graphics2D`, one menu item, keyboard-reachable,
   byte-identical and branch-free when off, pinned against (2) by the agreement test above.

Item 2 is the one that closes the human half of the loop for real, and it is the one neither
this issue nor #736 nor #876/#877 currently owns. `band_mw: 1-2` still looks right for all
three, because item 1 is a file move and item 3 is a wrapper.
