# Issue #877: TASK-C543-2: the CVD preview is selectable in-app over the live canvas — not a static snapshot — and CI's three CVD legs drive through it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

An instructor should be able to find out, before students do, that their lab handout
encodes something in a colour half the room cannot separate. That is a real outcome, it is
CAP-26's PF-2, and it belongs in JLS: the project already shipped the hard half (`Theme`,
Okabe-Ito, the delta-E ratchet) and CAP-26 §1 step 1 is written around this exact moment.
I endorse the goal without reservation.

What I do not endorse unchanged is the shape. Two of the six criteria cut the seam in a
place the codebase argues against, one criterion does no work at all here, and a better
version of the goal is visible and cheaper. Four reframes follow; the third is the one I
would actually fight for.

## Reframe 1 — CAP-26 Open Question 5 posed a false binary; there is a third seam that dominates both

OQ5 offers theme-level (misses the colours `Theme` does not own) or framebuffer-level
(catches everything, at the cost of an offscreen buffer and a full-canvas pixel pass on
every repaint). Both siblings inherit that binary as settled. It is not settled, because
there is a third seam neither issue names: **wrap the `Graphics2D`, not the framebuffer.**
A `CvdGraphics2D` delegate that transforms the argument of every `setColor` catches every
call site — hardcoded or themed — because it intercepts at the drawing API rather than at
the palette.

Whether that is *total* over the canvas is a measurable property of this codebase, and at
`5b05d67` it measures cleanly:

```
$ grep -rn "drawImage" src/jls/edit --include=*.java | wc -l                       0
$ grep -rn "setPaint\|GradientPaint\|TexturePaint\|AlphaComposite" src/jls ...     0
$ grep -rn "setColor" src/jls/edit --include=*.java | wc -l                      135
$ grep -rn 'Color\.black\b\|Color\.BLACK\b' src/jls --include=*.java | wc -l      113
```

No images, no paints, no gradients, no alpha compositing anywhere in the render path: the
canvas is shapes and strings under `setColor`, 135 sites, and the 113 hardcoded blacks
`#876` builds its falsifiable proof on are among them. A `setColor` decorator reaches all
113. `#876` criterion 2 passes as written; the palette-level failure it is designed to
catch is still caught.

What that seam then buys, which framebuffer level does not:

- **It composes with every other rendering surface for free.** `CircuitRenderer.draw`,
  `CircuitRenderer.print`, raster `-i` export and the JFreeSVG `-i out.svg` path
  (`src/jls/edit/CircuitRenderer.java:301-361`) all take a `Graphics`. One wrapper serves
  the live canvas, printing, PNG, SVG and #734's headless render — five surfaces, one
  class. A framebuffer filter serves raster only, and would need a second, unrelated
  mechanism to CVD-check an SVG handout.
- **Criterion 5 becomes true by construction, not by care.** "Byte-identical and no
  per-frame cost when off" is satisfied by *not wrapping*: there is no branch in the paint
  path at all. Under a framebuffer filter, `paintComponent` must render to an image and
  blit unconditionally-or-conditionally, and the honest reading of "no per-frame cost" gets
  argued about.
- **It survives the interactive simulator.** A running simulation repaints the canvas
  continuously; a full-canvas per-pixel LMS round trip on every one of those frames, at
  zoomed-out canvas sizes, is the cost that decides whether an instructor leaves the
  preview on while the circuit runs — which is precisely the use criterion 2 exists to
  protect. A per-`setColor` transform is a few dozen matrix multiplies per frame.

The one thing framebuffer level genuinely buys is anti-aliased edge fidelity: CVD
simulation is not linear end to end (sRGB decode → LMS → simulate → encode), so
filter-then-blend and blend-then-filter differ at AA edges, and #734's outcome text
explicitly claims "the anti-aliased blending an in-memory palette test cannot see." That is
a real difference and I am not hand-waving it. But note what #734 then *measures*: AC-2
requires distinguishability be asserted on stroke width, dash period or glyph presence —
**explicitly not on residual colour surviving the filter.** Sub-pixel edge chromaticity is
not an input to the only assertion #734 makes. The property AA fidelity protects is not the
property being tested.

So this is a live REPLAN candidate on #507, not a settled default: **OQ5 should be
re-answered as "draw-call level (a `Graphics2D` decorator), which is a superset of theme
level and reaches the same 113 call sites as framebuffer level."** If the maintainer still
wants pixel-exact AA fidelity for the CI measurement, the clean split is: framebuffer
filter for #734's oracle, draw-call wrapper for the live preview — but then criterion 3
must be restated as "the same *transform*", not "the same *rendering path*", because they
would no longer be the same path.

**Consequence the split should absorb honestly:** under this reframe the two-task roster
loses most of its rationale. #543's disposition comment justifies the split as "two failure
modes, two PRs," where the risky half is the framebuffer decision. A decorator plus a menu
radio group is one small class, one menu block, two tests — comfortably one PR at the
combined 1-2 mw band. Splitting a one-PR change across two issues buys coordination cost
and a criterion (5) that exists only to restate the sibling's criterion 3 at a surface
where it cannot independently fail.

## Reframe 2 — criterion 3 points the dependency arrow against the project's grain

"#734's `CvdStateDistinguishabilityTest` drives its three CVD legs through this preview
path" makes an accessibility oracle depend on `jls.edit`, on Swing, and on an interactive
editor surface. That direction fights three recorded commitments at once: the
headless-by-construction invariant enforced by `HeadlessCoreRatchetTest`
(ARCHITECTURE.md, "jls.sim"), the #91 UI-test layering that keeps render assertions at
layer 3 over a `BufferedImage` (`test/jls/ui/RenderAssert.java`, `package-info.java`), and
#734's own AC-4 ("does not stand up a second display apparatus").

The invariant criterion 3 actually wants is *"there is no second CVD rendering path"* —
and this repository already has the idiom for stating that directly. `NotificationRatchetTest`
forbids raw `JOptionPane` call sites; `HeadlessCoreRatchetTest` forbids AWT imports in the
sim core. The same move here:

> One `CvdRender.render(circuit, view, Deficiency) -> BufferedImage` in a headless-capable
> package. `SimpleEditor.paintComponent` consumes it (or the decorator it wraps); #734's
> legs consume it. A ratchet test asserts no other site constructs a CVD transform or a
> deficiency matrix.

That is strictly stronger than criterion 3 as written — criterion 3 only proves the test
*can* reach the GUI path, not that a second path does not exist elsewhere — and it costs
the test suite nothing in display dependency. I would replace criterion 3 with the ratchet
formulation.

## Reframe 3 — a viewer gives an impression; the instructor needs a verdict

This is the reframe I would argue hardest for, and it is the one place I am setting aside
the issue's stated framing rather than sharpening it.

CAP-26 §3 risk 1 records that machine checks are necessary but not sufficient. The converse
is equally true and is recorded nowhere in this cluster: **a trichromat looking at a
simulated dichromacy is a weak oracle.** They already know what is on the canvas, they
retain contrast and search strategies a dichromat does not, and simulation conveys the
colour shift without conveying the discrimination loss. An instructor who looks at a
deuteranopia-filtered adder and thinks "that seems fine" has produced almost no evidence.
This is the standard critique of every simulate-CVD tool, and it applies here with full
force.

Meanwhile #734 computes the actual answer mechanically: every state pair distinguishable on
a non-colour channel, under grayscale and all three dichromacies. That result is currently
destined to live only inside a CI assertion. The highest-value in-app surface is therefore
not a filter — it is **"Check this circuit for colour accessibility"**, producing a named
list: *"HiZ and zero are indistinguishable under tritanopia in this circuit; both render
as a thin solid line at this zoom"*, with a jump-to-element. The preview filter then becomes
the *explanation* of the finding rather than the instrument of the check.

Three things follow, and they are all in the project's favour:

1. The instructor gets a claim they can act on and put in a lab review, instead of an
   impression they have no calibration for.
2. It costs little extra work *if* #734's distinguishability metric is shaped as a library
   with a test on top, rather than as a test with the metric buried in it. That is a small
   shape change in #734, made now, while it is unimplemented — the single highest-leverage
   note in this review.
3. It dissolves the static-snapshot argument entirely. A verdict is computed on demand
   over current model state; there is nothing to debate about whether it is live.

I would keep criterion 1 and 2 (the filter is worth having, and live is the right form for
it), and add the checker as the primary affordance. If the roster can only afford one, the
checker is the one that produces evidence.

## Reframe 4 — the control already exists; do not add one

Criterion 6 asks for progressive disclosure and criterion 4 for keyboard reachability plus
an accessible name, with #756's gate as the enforcement. Both are free if the preview is
not a new control at all. `JLSStart` (~line 1941) already builds a **"Color scheme"**
submenu as a `ButtonGroup` of `JRadioButtonMenuItem`s over `Theme.all()`. A "Simulate
colour vision" sub-submenu — none / protanopia / deuteranopia / tritanopia — in exactly
that shape:

- is invisible to a first-year drawing an adder (criterion 6, by construction, one nesting
  level deeper than a menu they already never open);
- inherits menu traversal and accessible names from infrastructure
  `test/jls/ui/MenuMnemonicAndAccessibleNameTest` already gates, so criterion 4 is met
  without #756 being a prerequisite at all — which removes an ordering edge the roster
  currently carries implicitly;
- reuses `refreshEditorColors()` and the repaint-every-editor path the theme items already
  use.

One design point neither issue considers and which bites every tool in this class:
**do not persist the preview.** `UserPrefs.rememberTheme` persists the theme; a CVD preview
left on across sessions produces "JLS's colours are broken" reports from users who forgot.
Reset to none on startup, and put a standing marker in the editor status line while active.

## Reframe 5 — the handout is an exported artifact, and batch mode is right there

The thing an instructor hands out is a printed page, a PDF, or an SVG in a course site —
not the editor canvas. JLS already exports all of that headlessly (`-i out.png`,
`-i out.svg`), already ships a container image documented for exactly this kind of
non-interactive use, and the wrapper of Reframe 1 slots into that path with no additional
mechanism:

```sh
jls -i handout-deut.png --cvd deuteranopia lab3.jls
```

One row in `JLSStart`'s `FLAGS` table (with `CliFlagTableTest` keeping it honest) gives an
instructor a vetting artifact they can attach to a review, diff between revisions, or run
over an entire course's lab set in a loop. That is a *cheaper* real vetting loop than the
live filter, and the issue does not consider it. It is not a substitute for criterion 1,
but it belongs in the roster, and per mw it may be the better buy.

## Alignment

The work strengthens the project's arc rather than pulling against it: CAP-26 is the
most differentiated thing on this roadmap, and PF-2 is its human-loop half. Nothing here
duplicates #542, #731, or #76's matrix — the pass-2 boundary comment on #543 settles those
correctly. The one place it pulls against the grain is criterion 3's dependency direction
(Reframe 2), and the one place it under-reaches is treating the instructor as an eye rather
than as someone owed a verdict (Reframe 3).

## What I would change, in priority order

1. Reshape #734's distinguishability metric as a callable library now, and add an in-app
   "check this circuit" verdict surface to this task (Reframe 3).
2. Replace criterion 3 with a no-second-path ratchet over one shared headless render
   entry point; drop the requirement that the CI test route through the GUI (Reframe 2).
3. Take draw-call-level (a `Graphics2D` decorator) to #507 as OQ5's third answer, with the
   grep evidence above; if adopted, collapse #876/#877 back into one issue (Reframe 1).
4. Specify the control as a sub-submenu of the existing "Color scheme" menu, not persisted,
   with a status-line marker while active (Reframe 4).
5. File `--cvd` on `-i` export as a sibling task (Reframe 5).
6. Note that criterion 5, inherited verbatim from #876 criterion 3, cannot independently
   fail at this surface; keep it as a reference, not as an acceptance criterion.
