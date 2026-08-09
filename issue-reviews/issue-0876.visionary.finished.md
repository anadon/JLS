# Issue #876: TASK-C543-1: the colour-vision transform is one callable framebuffer filter, proven on a colour the Theme seam does not own
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the dedup-pass archaeology, #876 asks for one thing: **a pure function
`(image, deficiency) -> image` that approximates colour-vision deficiency**, with no UI.
Everything else in the body — the roster split, the ordering-inversion flag, the
`tier:feature` defence — is bookkeeping about *who owns* that function, not about what it is.

That function is the right thing to build, and the framebuffer level is the right level.
But the issue frames it as *the preview's filter*, and that framing is what produces the
ownership fight with #542, the misplaced `ordering_after` edge, and a test (AC-2) that
entrenches the very debt it is meant to expose. Reframed as **a project-wide colour-vision
model with several consumers**, the fights disappear and the thing gets more valuable.

## Finding 1 — the model already exists in the repo, in test code

`test/jls/ThemeTest.java:143-234` already contains a complete dichromat simulation:
Hunt-Pointer-Estévez `RGB2LMS`/`LMS2RGB`, Viénot-1999 `DEUTERANOPIA`/`PROTANOPIA`
projections, sRGB linearisation, clamping, and CIE76 delta-E on top. That is the model
`Theme.java`'s shipped contract ("at least 25 CIE76 delta-E apart under normal,
deuteranopic, and protanopic vision") rests on.

As written, #876 produces a **second** copy of that math in `src/`. Worse, the cluster then
splits maintenance of it: #729 (tritanopia at the delta-E floor) adds a tritan matrix to the
*test* copy, while #876 adds three matrices to the *src* copy. Two colour-vision models,
diverging, is exactly the duplication mode the dedup pass that spawned this issue exists to
prevent — and here it would be introduced *by* that pass.

**Reframing:** the deliverable is not "the preview's filter" but
`jls.core.ColorVision` (or `jls.a11y.ColorVision`) — a small, AWT-light, headless class
holding one authoritative model with two entry points:

- `int simulate(int argb, Deficiency d)` — the primitive;
- `BufferedImage simulate(BufferedImage in, Deficiency d)` — the image op #876 asks for.

`ThemeTest` then *deletes* its private copy and calls the primitive; `#729` adds tritanopia
once, in one place, and both the palette floor and the framebuffer filter gain it
simultaneously. This is strictly less code than the issue as written, and it converts the
existing test-only math into a shipped, cited, reusable asset.

**This also dissolves the "ordering inversion, still unresolved" that the issue escalates.**
If the model is a neutral library in `core`, it is owned by neither #542 nor #543. #542's CI
legs consume it, #543's preview consumes it, #734 consumes it. There is nothing to arbitrate
and no `REPLAN:` on #507 is needed — not because the recommendation in the body is wrong, but
because the question was an artifact of putting a shared primitive inside one feature's
boundary.

## Finding 2 — AC-1's "the transform matrices" quietly locks in a wrong tritanopia

AC-1 says "the transform matrices and their source cited in the code," presupposing one 3×3
per deficiency. Viénot 1999 — the model already in `ThemeTest`, and the obvious thing to copy
— **is only valid for protanopia and deuteranopia**. Its tritan extension is known to be
poor; the tritan confusion line needs Brettel-1997's two-half-plane construction, which is
*not* a single 3×3 and cannot be expressed in the shape AC-1 assumes. Tritanopia is precisely
what #729 and CAP-26 add over the shipped state, so the whole new claim of the capstone would
rest on the least defensible matrix in the file.

**Concrete alternative:** adopt **Machado, Oliveira & Fernandes (2009)** severity-parameterised
matrices as the single model. One citation, one uniform table shape for all three deficiencies,
a defensible tritan, and — free — anomalous trichromacy, which is the *majority* of the
population CAP-26's abstract counts ("roughly 8% of male students"; dichromats are ~1%, the
rest are anomalous). A later severity slider becomes a parameter, not a rewrite. If continuity
of the shipped delta-E numbers matters, keep Viénot for protan/deutan behind the same
interface and record why — but do not let two models coexist unnamed.

## Finding 3 — AC-2 entrenches the debt it is meant to expose

AC-2 requires "a test asserts the transform reaches a colour the `Theme` seam does not own,
**naming a specific hardcoded call site**," drawn from the 113 `Color.black` sites. It is a
good instinct (it is the only criterion that can distinguish framebuffer from palette) with a
bad implementation: the test becomes a **permanent requirement that the debt persist**. If
#289's sweep ever routes those sites through the palette — the recorded direction in both
`Theme.java`'s javadoc and ARCHITECTURE's look-and-feel decision — this test must be rewritten
or deleted, and until then it is a standing incentive not to finish the sweep.

**Better and strictly stronger:** assert *totality over pixels*, not one call site. Render a
canvas offscreen (the substrate exists — `test/jls/ui/RenderAssert.java`, layer 3 of the #91
harness, and `CircuitRenderer.exportImage` already renders headlessly into a
`BufferedImage`), then assert the set of colours in the filtered output is contained in
`simulate(colours of the unfiltered output)`. A palette-level implementation fails this the
moment any colour outside the `Theme` record appears — which is every rendering — and it
cannot be special-cased, cannot rot when #289 lands, and needs no grep count in the issue body.
A one-line variant: draw a scratch element in an arbitrary colour that is in no `Theme`, assert
it comes through transformed. Either is a better proof than a named site, and neither can be
satisfied by a theme substitution.

I am explicitly disregarding AC-2 as written.

## Finding 4 — AC-3's cost clause should be structural, not asserted

AC-3 correctly separates byte-identity from per-frame cost, then leaves the cost half with no
mechanism. The naive implementation ("if a deficiency is selected, render to a buffer and
filter; else draw straight through") puts a branch in `paintComponent` forever and is exactly
the shape the criterion warns about.

**Swing-native alternative:** make the filter a `javax.swing.plaf.LayerUI` installed via
`JLayer` over the canvas *only while a deficiency is selected*, and removed when it is not.
Off means the component tree is literally unmodified — no branch, no buffer, no allocation —
and the criterion becomes checkable by *inspecting the component hierarchy* rather than by
timing, which is the only kind of performance assertion that survives CI noise. It also
composes: the same `LayerUI` can cover the trace window and dialogs later without touching
their paint code.

For the op itself: precompose `LMS2RGB · D · RGB2LMS` into one 3×3 at class init, with two
256-entry sRGB↔linear tables. Nine multiplies per pixel, no allocation per frame. Unit-test
the precomposed product against its factors so the citation stays honest.

## Finding 5 — the handout is a file, not a canvas

This is the reframing I would most like the maintainer to take. The feature's outcome
sentence is "an instructor previews a handout as their colorblind students see it." A
handout, in JLS's world, is an **exported image** — `-i out.png`, `-i out.svg`, the format
README sells for "slides, lab reports and hosted docs" — produced by the same
`CircuitRenderer` the canvas uses (`CircuitRenderer.of(circuit).draw` is the single draw
entry behind canvas, print, PNG/JPEG *and* SVG). And JLS's distinctive strength, per
ARCHITECTURE and the container image, is that the headless surface is a documented contract
that autograders and CI already drive.

So the first and highest-leverage consumer of this pure function is not a GUI toggle; it is
**`jls -i out.png -cvd deuteranopia circuit.jls`** (or `-cvd all`, writing three files). That
delivery: needs no Swing surface, is testable in the existing `CliImageExportTest`, runs in
the container an instructor's course already has, produces an artifact they can paste into a
lab report or diff in CI, and reaches instructors who never open the editor. It costs one
`FlagSpec` row plus a `docs/batch-interface.md` paragraph — the flag table is already
test-pinned (`CliFlagTableTest`), so the obligation is small and known.

Under this framing #877's live-canvas mode is a genuine nice-to-have rather than the thing
PF-2 stands or falls on, and CAP-26's PF-2 outcome is reachable at roughly half the declared
band. One boundary to state explicitly: a pixel filter cannot apply to `-i out.svg`. Either
refuse the combination with one `jls: error:` line (my recommendation, documented), or filter
at the draw-call level for SVG only — do not silently emit an untransformed SVG.

## Finding 6 — grayscale belongs in the same enum, first

CAP-26's actual thesis (PF-1) is that state must survive **colour removed entirely**.
Grayscale is therefore a stronger filter than any dichromat simulation, requires no model, no
citation and no matrix argument, and simultaneously serves two other stated capstone
concerns — monochrome printing and projector washout — that no CVD simulation addresses. It
is also honest in a way the dichromat sims are not: CAP-26 §3.1's own rule is to claim no more
than the tests prove, and *no colourblind person sees what a Viénot projection outputs*.

Add `Deficiency.GRAYSCALE` (luminance per ITU-R BT.709 on linear light, not a naive channel
average), list it first wherever deficiencies are offered, and label the three dichromacies
"approximate simulation, not any individual's vision" in the help text. Near-zero cost;
materially better pedagogy and materially more defensible prose in the eventual VPAT.

## Finding 7 — the ordering edge is on the wrong issue

`ordering_after: [731]` is justified in the body by "the preview is only worth looking at once
the redundant thickness/dash/glyph channel exists." That is an argument about **#877's UI**,
not about a pure function over pixels. A colour-vision library does not care whether dash
patterns exist. Move the edge to #877, set this task's `ordering_after: []`, and it becomes
schedulable today — in parallel with #731, and available to #542's screenshot legs before the
encoding work lands, which is the direction the ordering-inversion note wanted anyway.

## Trajectory

This work pulls **with** the project's arc: it converts private test math into a shipped,
cited capability, and it lands on `jls.core`, the AWT-light package the codebase has been
growing deliberately (`Geometry`, `Bounds`, `SegmentGeometry`). Reframed per Findings 1 and 5,
it also strengthens the headless/batch contract that is JLS's most differentiated surface,
rather than adding weight to the Swing editor, which is where the project's stated debt
(~113 hardcoded sites, no dark variant) already lives.

It pulls **against** the arc in exactly one place as written — AC-2's named call site, which
makes the incompleteness of the `Theme` seam a tested requirement instead of a tracked defect.
Fix that, and this is a small, elegant, high-leverage task.

## Summary of the alternative

1. Build `jls.core.ColorVision` (Machado 2009, plus `GRAYSCALE`), owned by neither #542 nor
   #543 — the ordering inversion evaporates and no `REPLAN:` is needed.
2. `ThemeTest` deletes its private copy and consumes it; #729's tritanopia lands once.
3. Prove framebuffer-ness by pixel-set totality, not by naming a `Color.black` site.
4. First consumer is `-i ... -cvd <deficiency>` on the existing headless export path.
5. GUI preview arrives as a `JLayer`/`LayerUI` (#877), making "zero cost when off" structural.
