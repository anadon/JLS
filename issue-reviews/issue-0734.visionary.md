# Issue #734: TASK-C542-3: the shipped adder lab is screenshot-tested for state distinguishability in full grayscale and under all three dichromacies
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

One sentence in the Outcome carries the whole task: *state must survive with colour
removed entirely, on screen, including anti-aliased blending an in-memory palette test
cannot see.* That is the load-bearing claim of CAP-26 PF-1 — the thing that makes JLS's
accessibility story stronger than "we picked Okabe-Ito." It is right, it is the correct
next assertion after #729/#731, and it should exist.

Everything else in the issue — the adder lab, the screenshot, the display substrate, the
four filters — is *apparatus*, chosen at filing time, and all four choices pull against
where the codebase already went. I am disregarding acceptance criteria 1–4 and proposing a
replacement that proves a strictly stronger claim, headlessly, in plain `mvn verify`.

## The four things the repo already decided differently

**1. The project explicitly rejected the screenshot matrix — with reasons still valid.**
`test/jls/ThemeTest.java:15-19` opens: *"run as a unit test instead of a screenshot matrix:
simulate deuteranopia and protanopia over the semantic palette directly (the same LMS-matrix
filter a screenshot pass would apply per-pixel)."* #734 reverses that decision without
engaging the rationale. Reversal may be justified — the anti-aliasing gap is real — but the
issue does not argue it, so the reversal will be re-litigated in review.

**2. The pixel-level half already ships.** `test/jls/elem/WireValueChannelTest.java` renders
a real 84px wire through `ElementRenderers.draw` into a `BufferedImage` and counts
non-background ink per state: thick > 2× thin, dashed < thin, touching-end ring > plain end.
That is exactly "the anti-aliased blending a palette test cannot see," already measured,
already headless, already in `mvn verify`. #734 does not cite it. Its genuine delta over
HEAD is narrower than the Outcome implies: *pairwise totality* across all states, and a
*mutation-proven* red run. Those two are worth having. The rest is re-derivation.

**3. AC-4 files the test in the wrong lane, and the wrong layer.** Rendering a circuit needs
no display: `ElementDrawSmokeTest` says so in its own javadoc — *"None of this needs a
display: paint code is plain Graphics2D"* — and `test/jls/ui/package-info.java` reserves
Layer 3 for precisely this: *"paint to a BufferedImage without a window and make semantic
checks, never brittle pixel goldens."* AC-4 instead pins the test to Layer 2, the xvfb lane,
which `pom.xml` configures with `rerunFailingTestsCount` and which the required PR check
excludes (`<excludedGroups><param>display</param>`). The net effect: JLS's strongest
accessibility invariant would live in the flakiest, retried, non-required lane. Meanwhile
`docs/standards-adoption/03-accessibility-conformance.md:469` heads its verification plan
"**Automatable, headless, joins plain `mvn verify`**". AC-4 is not a small substrate
preference; it downgrades the guarantee.

**4. The fixture does not exist.** There is no shipped adder lab. `resources/samples/` is
absent; the repo's four `.jls` files are `riscv-sum1to10`, `fork-4.6-shiftregister`,
`headless-canary-gate`, `riscv/gui/cpu.jls`. The sample set — "full adder (combinational),
N-bit counter, a mux demo, …" — is #73's *resolved but unbuilt* plan
(`ISSUE-AMBIGUITIES-2026-07.md`, §#73). `ordering_after` lists only TASK-C542-2, so the task
carries an undeclared hard dependency on unfiled onboarding work.

## The measurement problem AC-2 does not see

AC-2 requires distinguishability asserted on "measured stroke width, dash period or glyph
presence" recovered *from a rendered screenshot of a whole adder lab*. That is a computer-
vision task, not a test: diagonal anti-aliased strokes, wires crossing gate bodies, labels
and pin ink in the same raster, and — the killer — the measurement must be attributed back
to *which wire is in which state*, which requires the model anyway. Once the test holds the
model, the honest oracle is the model, not blob analysis. The likely outcome of implementing
AC-2 as written is a fragile pixel heuristic tuned until green, i.e. the "brittle pixel
golden" the `jls.ui` package rules were written to forbid.

The redundancy is worse than the fragility. If distinguishability is genuinely asserted on a
non-colour channel, then applying protanopia, deuteranopia and tritanopia matrices to that
measurement is provably a no-op — a dash period does not change under an LMS transform. AC-1
and AC-2 contradict each other: either the assertion is colour-free (and three of the four
filters prove nothing, at 4× cost) or it is not (and AC-2 is violated). Only **grayscale**
carries content, and only because it collapses *luminance against the background* — which is
a palette property, not a stroke property.

## The reframing: split the claim along its natural seam, then swap the fixtures

Two different claims are fused here. Separate them and each gets an exact oracle.

**(a) "The encoding reached the drawing surface, injectively" — assert on the SVG, not pixels.**
`CircuitRenderer.exportImage` (`src/jls/edit/CircuitRenderer.java:301`) already drives the
*same* `ElementRenderers.draw` calls into a JFreeSVG `Graphics2D`, with a fixed defs prefix
and a deterministic draw order, pinned byte-identical by
`SvgExportTest#exportingTwiceIsByteIdentical`. An SVG export names `stroke-width` and
`stroke-dasharray` per path *as text*. Assert pairwise injectivity of the (width, dash,
glyph) tuple over the SVG DOM: exact, deterministic on every arch including the riscv64
container, human-auditable in a diff, no display, no CV. Equivalently, a recording
`Graphics2D` proxy capturing `setStroke`/`BasicStroke` per state gives the same rigour
without the parse. Either seam proves *totality and injectivity as actually drawn* — a
strictly stronger statement than TASK-C542-2's registry-level totality test, because it
catches a renderer that ignores the registry.

**(b) "It survives colour removal, anti-aliasing included" — per-state micro-render, one
filter.** Extend `WireValueChannelTest`'s existing apparatus: one horizontal wire on a blank
canvas, one image per state, converted to relative luminance. On a controlled scanline the
measurement is arithmetic, not vision: run-length gives dash period, perpendicular profile
gives stroke width, and background contrast falls out directly. Assert every ordered pair
differs on at least one channel, and assert every state's luminance contrast against
`background()` ≥ 3:1 — the WCAG 1.4.11 floor that
`docs/standards-adoption/03-accessibility-conformance.md:472` already schedules for
`ThemeContrastTest` and which is **red today** (`nonZero` 2.10:1, `watch` 2.31:1). That is
the only real grayscale failure mode, and #734 as written would not isolate it.

**(c) Give the adder lab the job it is actually good at.** The whole-circuit render is a bad
distinguishability oracle and an excellent *legibility* oracle. KC-26-1 — "if glyph
escalation cannot achieve distinguishability without wrecking legibility for sighted users
(measured by the same screenshot apparatus)" — is a kill criterion with no owner, and #734's
ACs never produce its measurement. Point the full-circuit render at that: ink density,
glyph–glyph and glyph–body collision counts on a dense schematic, ratcheted. Then the
fixture earns its place and the kill criterion becomes checkable instead of rhetorical. This
also removes the #73 dependency from the critical path: a legibility ratchet can run on
`riscv/gui/cpu.jls` (far denser than a full adder, and a better worst case) today.

## Two more alignment notes

- **The CVD filter should be product code, not test code.** CAP-26 AC-1 "Spans PF-1+PF-2",
  and OQ-5's recommended default is a *framebuffer-level* preview filter. #734 builds a
  test-private filter that the shipped preview mode will later duplicate. Write the filter
  once as PF-2's `CvdFilter` in `jls.edit`, have the test call it, and AC-1's two halves are
  served by one mechanism — the test then proves the thing instructors actually use. This is
  a one-line scope change with a large payoff.
- **Bind the test to its criterion now.** The repo already has a `@jls.testedby` javadoc
  convention. PF-5's VPAT generator requires "no criterion without a named passing test."
  Emit that binding at birth (`@jls.criterion WCAG-1.4.1 / 1.4.11`) rather than making PF-5
  reverse-engineer it from test names.

## What I would keep, verbatim

AC-3. The mutation requirement — flatten one state's dash, record the red run before any
pass counts — is the single most valuable line in the issue and survives every reframing
above unchanged. Keep it, and apply it to both (a) and (b).

## Suggested reshape

Retitle to *"every wire state is distinguishable with colour removed — proven on the drawn
output, headlessly"*; replace AC-1/AC-2 with the SVG/recording-Graphics2D injectivity
assertion plus the per-state luminance/contrast assertion; keep AC-3; replace AC-4 with
"joins plain `mvn verify`, Layer 3, no display"; drop the adder-lab dependency and file the
legibility ratchet (KC-26-1's missing measurement) as the sibling that consumes a dense
fixture. Cost should fall below the stated 1–1.5 mw band, not rise.
