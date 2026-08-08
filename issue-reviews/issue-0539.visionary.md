# Issue #539: FEAT-C24-4: N clock cycles of a canvas region become a deterministic APNG/GIF with signal-value overlays, encoded in pure Java
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the acceptance criteria away and the claim is: *a circuit in motion is a
teaching artifact nothing currently produces, and JLS is the only tool positioned
to produce it from a run that actually happened.* That claim is right. What #539
gets wrong is everything downstream of it: the artifact it picks (a raster
movie), the component it proposes to own (an encoder), and the customer it aims
at (the LaTeX handout). Each is separately fixable, and fixing all three turns a
2–3 mw feature that #508 already recommends cutting into roughly 1 mw of seam
plus a documentation page — while producing *more* artifact kinds, not fewer.

## The inconsistency inside CAP-24's own walls

CAP-24 already decided this exact question, one planned feature earlier, and
decided it the other way. KC-24-3 (PF-3, #538) says: the WaveJSON file is the
product, the WaveDrom renderer is a **dev-time-only pin**, and the shipped jar
must never grow the renderer. JLS emits the data; something else draws the
picture.

PF-4 inverts that with no argument. Here JLS must own the encoder, in-jar, in
pure Java, because "single-jar constraint." But the single-jar constraint is
exactly what KC-24-3 satisfied by *not* shipping the renderer. The same capstone
holds two opposite architectural stances about who turns simulation state into a
picture, and #539 inherits the losing one. This is the seam I would cut along.

## Alternative framing A (primary): ship the frame sequence, not the movie

Make the deliverable a **deterministic frame-sequence export** — "render the
recorded run at cycle *k*, for k in [0,N)" — and let the encoder live outside the
jar:

```
jls -b -frames out/frame-%04d.svg --cycles 8 --region ... run.rec circuit.jls
```

Then `docs/figure-interop.md` (informative, exactly the shape of
`docs/vcd-interop.md`) carries the one-line recipes: `ffmpeg` → APNG/WebM/MP4,
`magick` → GIF, `apngasm` → APNG, `\animategraphics` → an animation embedded in
the very PDF handout CAP-24 §1 step 2 builds. The README already installs
ImageMagick as project tooling for inspecting `-i` output; `iverilog`, `ghdl`,
`yosys` and ELK are already handled as out-of-jar toolchain (ARCHITECTURE.md,
"Plugin trust boundary" — external tools sit on the subprocess boundary and stay
there). #498's own rule for this program is "build no bespoke forge."

What this buys that #539 does not:

- **Every animated format, including the excluded ones.** Open Question 4 rules
  MP4 out because a native encoder violates the single-jar rule. Under this
  framing MP4 is simply the instructor's `ffmpeg` line — the constraint is
  satisfied by never owning any encoder, and the exclusion evaporates rather
  than being enforced.
- **Determinism becomes free instead of hard.** The frames are SVG text from a
  path that is *already* byte-identical (`SvgExportTest#exportingTwiceIsByteIdentical`;
  `CircuitRenderer.exportImage` pins a fixed `defs` prefix and sorts elements
  into a total draw order precisely so goldens hold). AC-2's byte-identity is
  asserted over SVG/TikZ/WaveJSON and conspicuously **not** over the APNG —
  because rasterized text cannot be byte-identical across platforms, which is
  KC-24-1's risk in its worst form. #539 is the one artifact in the bundle that
  is a binary blob, the largest one, and the only one that cannot be diffed in a
  course repo — against a capstone whose stated premise is "figures live in
  course repos under version control like everything else in this project."
- **The size budget disappears.** AC-2's "declared size budget" exists only
  because 32 frames of full raster is heavy. A frame sequence in which only wire
  colors and overlay text change is small, and if a single animated file is
  wanted, an animated SVG (SMIL over the same layers) is a text file that diffs.
- **The expensive part was never the drawing.** `ElementRenderers.draw(Graphics,
  Element)` already dual-targets `BufferedImage` and JFreeSVG's `Graphics2D`
  with no per-element work; `ElementValueDisplays` already knows how to render
  current values. A frame loop over restored run state is small. Essentially all
  of PF-4's 2–3 mw is encoder plus size engineering plus the test that guards
  them — i.e. all of it is spent on the part that should not exist.

## Alternative framing B: the print-native artifact is a cycle strip, not a movie

CAP-24 §1 step 2 is "the in-tree LaTeX document builds clean in CI." An APNG
cannot appear in a printed lab handout. Neither can a GIF. The animated figure
in every hardware textbook is a **small-multiples cycle strip**: N panels, one
per clock edge, values overlaid, laid out in a grid with a shared caption. That
is pure composition over PF-1's print-styled renderer — no new output format, no
encoder, deterministic vector, works on paper *and* on screen, and it is the
artifact instructors actually paste into slides. If the bundle must contain a
fifth artifact kind, make it this. It costs a layout function.

## The customer is misidentified — and the right customer changes the design

#539 aims at the instructor building a handout. But #508's measurement is that
this repo's adoption is zero and the live user base is on someone else's fork,
and its direction list puts prominence (item 4) and distribution/SVG gallery
(item 7) ahead of capability surface. The single highest-value animated artifact
JLS could produce is *a looping GIF of a circuit running at the top of its
README* — that is how Digital and Logisim-Evolution sell themselves, and it is
the artifact that makes a link land in a course-materials thread.

If that is the customer, the design collapses further: determinism is irrelevant,
GitHub markdown renders GIF and strips SVG animation, and the whole thing is a
throwaway script plus `magick`, ~0.2 mw, no jar code, no `AnimationCaptureTest`,
no acceptance criteria. It is also worth doing *before* PF-1, because it touches
the actual bottleneck. Filing that as a task and letting PF-4 die is strictly
better than funding PF-4 to serve a handout consumer that cannot display its
output.

## Criteria I am explicitly disregarding

- **AC-3 (pure-Java encoding, no native encoder).** Correct as a constraint on
  the *jar*; wrong as a requirement to *write an encoder*. Satisfied better by
  owning none.
- **AC-2 (`AnimationCaptureTest`: deterministic frame count/timing metadata,
  size budget).** These are properties of a container format JLS should not
  emit. The surviving obligation is frame-level determinism, which the SVG
  golden pattern already covers.
- **AC-1's APNG/GIF pairing.** Two formats to hedge one uncertainty is a smell;
  a frame directory makes the hedge unnecessary.

## Factual blocker worth recording on the REPLAN

AC-4 requires "a recorded run artifact." No such artifact exists in the tree —
there is `TraceSample`/`getTraceSamples` and VCD, and nothing else. The
recording-is-the-contract discipline #539 cites is §7.2 of #498, which is
**explicitly non-normative rescued branch content** whose implementation is M2
of an unfunded program. So #539's `ordering_after: []` is wrong on its face: it
depends on a format that is neither built nor ratified. This is not merely a
scheduling error — it points at where the value actually is. The recording
contract is the load-bearing invention; the animation is one consumer of it, and
the cheapest possible consumer is a frame dump.

## Recommendation for #505's REPLAN

Adopt #508's cut of PF-4 **as a shipped exporter**, and replace it with:

1. a `-frames` output mode folded into PF-6's bundle command (frames from the
   one recorded run, same as every other figure) — ~0.5 mw;
2. `docs/figure-interop.md`, informative, mirroring `docs/vcd-interop.md`,
   carrying APNG/GIF/MP4/`\animategraphics` recipes — ~0.2 mw;
3. the cycle-strip figure as a PF-1 composition, if the bundle wants a fifth
   kind — ~0.3 mw;
4. a separate, immediate, untested README-GIF task under the prominence lane.

The bundle keeps five artifact kinds, the animated-figure niche is served in
more formats than #539 proposes, the jar gains nothing, AC-2's byte-identity
extends to the animation instead of excusing it, and roughly 2 mw returns to the
grading-integrity wedge where #508 says it belongs.

## Fallback, if an in-jar encoder is non-negotiable

Drop APNG and ship GIF only. `ImageIO`'s GIF writer supports sequence writing
with per-frame metadata out of the box; APNG is the half that requires a
hand-rolled chunk/CRC writer. That alone roughly halves the feature and removes
the only genuinely novel binary-format code. But it still leaves the bundle's
one undiffable artifact, so I would not take it over the reframing above.
