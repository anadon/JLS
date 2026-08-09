# Issue #729: TASK-C542-1: tritanopia joins the verified colour-vision set at the existing delta-E floor, for every shipped theme
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is actually for

The stated deliverable is "a third 3x3 matrix in `ThemeTest`". The end it serves,
from #542 and CAP-26 #507, is: **a student can read wire state without relying on
colour discrimination at all.** #542 says so in its own words — grayscale
distinguishability is "the strictly stronger claim than the shipped color-distance
floor". #76 already shipped the honest mechanism for that claim: the second visual
channel (`WireRenderer.strokeFor` — thick for non-zero, dashed for HiZ, thin for
zero; open-ring endpoint glyph), pinned by `test/jls/elem/WireValueChannelTest.java`
by rendering and counting ink.

So the colour-distance ratchet is the *proxy*, and the proxy is already the weaker
of the two instruments JLS owns. Adding a third dichromacy filter to it polishes the
proxy. I endorse getting tritanopia covered — but not on this route, and not with
these acceptance criteria, for four reasons that the issue never considers.

## 1. The measured numbers say the metric is the wrong one

I reproduced `ThemeTest`'s pipeline (Viénot matrices, the same sRGB→linear→LMS→Lab
CIE76 path) outside the build and added the commonly-copied single-plane tritan
projection. Worst pairs, `Theme.DEFAULT`, over `ThemeTest.wireStates()`:

| filter | worst pair | delta-E |
|---|---|---|
| normal | touch/highlight | 35.9 |
| deuteranopia | touch/highlight | 35.0 |
| protanopia | touch/highlight | 26.5 |
| **tritanopia** | **highlight/wireOff** | **24.1** |

Now the same palette's pairwise |ΔL\*| in *normal* vision — the grayscale/monochrome
-print channel #542 actually wants:

    highlight/nonZero = 0.7      touch/wireOff = 1.3      nonZero/wireOff = 23.4

Lavender highlight and Okabe-Ito orange are **0.7 L\* apart**. Okabe-Ito blue and the
HiZ gray are **1.3 L\* apart**. Photocopy a lab handout, or project it onto a washed-
out screen, and those pairs are the same ink. No dichromacy filter can ever detect
this, because dichromats keep full luminance discrimination — the CVD legs are blind
to precisely the failure mode #542 exists to close. This task adds a third instrument
that cannot see the defect, to a palette that has it.

Notice too that the tritan 24.1 is *numerically identical* to the highlight/wireOff
ΔL\* of 24.1. That is not coincidence: tritanopia collapses the blue-yellow axis on
which lavender and gray differ, so the residual separation is pure lightness. Which
means the delta-E-after-simulation floor, for the pairs that matter, is a lightness
floor wearing a costume — and it is model-independent, so no better tritan matrix
rescues it.

## 2. AC-4 collides head-on with the parent feature's pixel gate

`Theme.DEFAULT` **fails** the assertion this task specifies, at 24.1 against a 25.0
floor. AC-4 then fires: "the palette is adjusted." But #542 AC-4 requires "the
default theme is pixel-unchanged for existing users, gated on every commit of this
feature." A task cannot be simultaneously required to change the default palette and
forbidden from changing it. An implementer will discover this after writing the
transform, and will resolve it by nudging lavender until 24.1 becomes 25.1 — a
number-chasing edit that ships a real palette change to satisfy a proxy metric while
the 0.7 ΔL\* defect survives untouched. That is the worst available outcome.

The failing pair also isn't a wire-state pair. `highlight` is documented in
`Theme.java` as "highlighted (selected) elements and wires" — a *mode affordance*,
with its own second channel (selection fill). `wireOff` is a *value*. `wireStates()`
conflates the two registers. Split them and `DEFAULT` passes tritanopia cleanly:
over `{nonZero, wireZero, wireOff, background}` alone the worst tritan pair is
nonZero/wireOff at **42.2**, and every other filter is ≥47. The pixel gate survives
untouched, and the floor lands where the semantics actually demand it.

## 3. AC-1 asks for a citation that does not exist

"A tritanopia simulation transform is added alongside the existing deuteranopia and
protanopia transforms, with its derivation cited in the test." Viénot, Brettel &
Mollon (1999) — the paper `ThemeTest` cites — is a deliberate *single-plane
simplification valid for protanopes and deuteranopes only*; the authors state it does
not extend to tritanopes, because the tritan confusion locus needs two half-planes
hinged on the neutral axis (Brettel, Viénot & Mollon 1997). There is no Viénot-1999
tritan matrix to cite. Every widely-copied "tritanopia 3x3" on the internet, including
the one I measured above, is folklore. Satisfying AC-1 literally means either a fake
citation or a quiet downgrade in rigour on the one project that wrote "with its
derivation cited" into the criteria.

This is the tell that the abstraction shape is wrong. `double[][] filter` is not the
right type; `Deficiency → (colour → colour)` is. Model deficiency as a *function* and
tritanopia stops being an awkward guest that doesn't fit the matrix slot — the
two-half-plane construction is just a function with a branch. **The reframing makes
AC-1's problem disappear rather than solving it.**

## 4. The transform is being built at the wrong seam

#542's own pass-2 consolidation comment picked resolution (a): this feature "owns the
CVD colour-transform primitive **as a testable unit**", and #543 wraps *that same
primitive* as a live framebuffer filter — explicitly so the transform is written once.
#543 AC-3 then requires the instructor preview and the CI apparatus to be the same
code. #546 and #547 are named downstream consumers.

AC-1 as written buries the primitive as `private static double[][]` constants inside
`test/jls/ThemeTest.java`, operating on `java.awt.Color` one colour at a time. That is
unreachable from `src/`, unreachable from #543, and the wrong data shape for a
framebuffer sweep. The task would land the exact duplication its parent's
adjudication comment was written to prevent.

## The alternative design

Concretely, and it is smaller than what the issue asks for:

1. **`jls.core.ColorVision`** — an enum `NORMAL, PROTANOPIA, DEUTERANOPIA,
   TRITANOPIA, ACHROMATOPSIA` with `int simulate(int srgb)` over packed 0xRRGGBB, plus
   `void simulate(int[] raster)`. `jls.core`'s package-info forbids AWT imports, which
   forces exactly the int-raster API that #543's framebuffer filter needs — the
   constraint and the consumer agree. `Theme`/`ThemeTest` convert via `Color.getRGB()`.
   Tritanopia gets the Brettel-1997 two-plane construction, honestly cited; the
   existing two keep Viénot-1999 with a comment saying why they may.
2. **`ACHROMATOPSIA` (full grayscale) is the headline leg, not an afterthought.** It
   is the strongest filter, it is what monochrome printing and projector washout
   reduce to, and it subsumes all three dichromacies for any palette that passes it.
   Assert an |ΔL\*| floor over the wire-*value* set under it.
3. **Split the ratchet's state sets**: wire values (strong floor, all filters) vs
   editor affordances (a weaker floor, or none — they carry non-colour channels
   already). Iterate `Theme.all()` as AC-3 wants, but per-set, so `CLASSIC` — which
   the existing `classicValueVersusTouchPairCollidesUnderDeuteranopia` test
   *deliberately* proves broken under deuteranopia, and which #76 pins byte-exact —
   does not become collateral damage of "iterate the shipped theme set". (Measured:
   `CLASSIC` passes tritanopia at 37.9 worst-pair, so it survives this task by luck,
   not by design. The next theme will not.)
4. **File the palette defect the grayscale leg exposes as its own issue** against
   #542, where the pixel-identity gate can be adjudicated by a human, rather than
   letting a task-band change ship a new default palette as a side effect.

## What I am disregarding, and why

- **AC-1's "in the test"**: disregarded. The primitive goes in `src/jls/core`, per
  #542's own resolution (a) and #543's reuse requirement.
- **AC-4's "the palette is adjusted"**: disregarded at this band. A 0.9-delta-E
  shortfall on an affordance-vs-value pair, under a folklore matrix, against a
  per-commit pixel gate, is not a mandate to change the shipped default colours.
  Escalate; do not tune.
- **AC-2/AC-3 as written**: kept in spirit (iterate `Theme.all()`, no hard-coded
  list), amended in scope (per state-set, not one flat pair matrix).

The goal — tritanopia in the verified set — is right and I endorse it. The route is
one filter too narrow, one package too deep in `test/`, and one citation short.
