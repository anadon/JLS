# Issue #711: TASK-C536-3: PDF comes out of the same deterministic renderer, not out of an SVG converter — and both outputs are byte-identical on all three platforms
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the ceremony and #711 asks for two unrelated things: (a) a PDF device for the
figure renderer, and (b) the test that closes CAP-24's determinism claim. The stated
goal underneath both is one sentence from #505: *a figure can live in a course repo
under version control and be diffed in review*. That goal is right and JLS is unusually
well-placed to hit it. But the issue's framing mis-prices (a), mis-locates (b), and
adopts an invariant — byte-identity — that is neither sufficient nor quite the thing
instructors need.

## The architecture has already answered Open Question 3

`CircuitRenderer.exportImage` (`/home/user/JLS/src/jls/edit/CircuitRenderer.java:301-360`)
is not "a raster exporter with an SVG special case". It is a device-independent element
paint path — `ElementRenderers.draw(g, el)` — pointed at whichever `Graphics2D` you hand
it. #154 already bought the seam: JFreeSVG 5.0.7 is a `Graphics2D` implementation bundled
in the jar (`pom.xml:64-73`), and the file even records the insight in a comment: "the
same element paint path that fills a bitmap below draws into JFreeSVG's Graphics2D
instead, so .svg output needs no per-element work".

So "PDF from the same renderer, not from a converter" is not a 1.5–2 mw design decision
waiting to be made. It is a third `Graphics2D` backend behind the same `if
(file.endsWith(...))` ladder, plus a golden. AC-1's "a test asserts no SVG-to-PDF
conversion step exists in the path" is satisfied by construction the moment the backend
is wired — there is nowhere for a converter to hide. The determinism work that is
genuinely hard already lives in #707, and the mostly-solved parts (fixed `defsKeyPrefix`,
the explicit `drawOrder` comparator over index bounds so a `HashSet` iteration order
never reaches the file) are already in this method. **Most of #711's stated band is
already spent.**

## Reframing 1: AC-4 should say "pure Java", not "no PDF toolchain"

As worded, "the shipped jar gains no external PDF toolchain dependency" reads to a
literal implementer as *write your own PDF serializer*. That contradicts the project's
own precedent three times over: XZ, FlatLaf (#153, with a written evaluation), and
JFreeSVG for exactly this pipeline. The real constraints the project cares about are
license compatibility (GPLv3-or-later), single self-contained jar, no native binary, no
subprocess, no network — all of which a small pure-Java `Graphics2D`-to-PDF library
satisfies as well as JFreeSVG does. Restate AC-4 in those terms and hold the *same*
evaluation bar #153 set (`docs/flatlaf-evaluation-2026-07.md` is the template).

The one honest argument for hand-rolling is determinism control (see below). That is
decidable in a two-hour spike — "can this library be pinned to a fixed `/CreationDate`,
a fixed `/ID`, and uncompressed streams?" — not in a 1.5–2 mw task. Make the spike the
first hour of the work, and let it choose.

## Reframing 2: byte-identity is the wrong invariant — you want diffability and render-identity

This is the substantive disagreement.

**Diffability.** #505's premise is figures that diff in review. A byte-identical,
Flate-compressed PDF is still an opaque binary blob to `git diff`; it satisfies AC-2 and
delivers nothing the outcome asked for. A schematic figure is kilobytes. Emit
**uncompressed** PDF with one operator per line and deterministic object ordering, and
the PDF diffs like the SVG does — and, as a side effect, the largest single source of
PDF nondeterminism (deflate implementation and level drifting across JDK builds)
disappears rather than being tested for. That is the "make the problem vanish" move here.
The remaining traps are then small and enumerable: `/CreationDate` and `/ModDate` (omit
or pin — `SOURCE_DATE_EPOCH` is already vocabulary this project speaks, per
`docs/reproducibility.md`), the `/ID` array (usually a hash of wall-clock; make it a hash
of content), and locale-sensitive float formatting (`Locale.ROOT`, which this file
already applies to `toLowerCase`).

**Render-identity.** `SvgExportTest`'s own header states the residual: "text layout
coordinates depend on the JDK's font metrics, which differ across machines". Note what
that does *not* say — it says nothing about the consumer. Element geometry is persisted
in the save file (`int width 48`, `int height 24` in the fixture), so much of the
positioning in an exported figure comes from the `.jls` file, not from the exporting
host. Meanwhile the emitted `<text>` element carries a *font family name*, and every
browser, Inkscape and `pdflatex` resolves that against its own font set. The result is a
pipeline that could pass AC-2 on all three platforms while the figure visibly differs in
the instructor's PDF, in GitHub's SVG preview, and in the lab handout. **Byte-identity
across producers is a diff signal; it is not the guarantee the outcome promised.**

The fix is one of two, and both are cheaper than they look:

1. Render text as vector outlines from the bundled font (JFreeSVG exposes this as a
   rendering hint; PDF backends do the equivalent). The figure becomes self-contained
   geometry with no font reference at all — no consumer variance, and the only remaining
   host dependency is the JDK's own outline extraction for a bundled TTF, which is JDK
   code rather than OS font machinery.
2. Keep real text (better for accessibility, and #546's tactile SVG will want it) but
   embed the subset font and write *explicit per-glyph positions* — an `x` list in SVG,
   `TJ` arrays in PDF — so no consumer re-lays-out the string.

Either way, add an AC to #711 that no output references a host font by name. That single
assertion is worth more than the three-platform matrix.

## Reframing 3: the platform matrix is the expensive way to state a checked-in golden

AC-2 as written ("bytes identical across the three CI platforms") implies cross-job
artifact upload and a comparison job that `ci.yml` does not have today — the linux,
`windows` (`ci.yml:143`) and `macos` (`ci.yml:259`) lanes each run `mvn verify`
independently. The equivalent-but-stronger construction is a **committed golden**: check
the exact SVG and PDF bytes into the fixture corpus and assert equality in-lane. Three
platforms then agree because each agrees with the same file, no plumbing is added, and —
the part the matrix cannot do — the golden also catches drift over *time*, which is how
figure regressions actually reach a printed lab. `SvgExportTest` deliberately has no
full-document golden today precisely because determinism was not yet owned; landing that
golden is the honest completion of #707, and #711 should consume it rather than invent a
comparison mechanism.

## Reframing 4 (the one that changes the ordering): the seam is a figure document, not a Graphics2D

`Graphics2D` is the cheap 80%, and it is the right backend for PDF. But it is lossy in
exactly the dimension CAP-24 needs downstream: by the time an AND gate reaches
`Graphics2D` it is anonymous bezier paths. PF-2 (CircuiTikZ, #537) wants the opposite —
a symbol id and a transform, so it can emit `\node[and port]`. PF-5's totality ratchet
wants to enumerate symbols. #546 (tactile) and #551 (gallery) want to restyle them.
None of that can ride the `Graphics2D` path; each will re-walk the element list on its
own. CAP-24 will end up with two or three traversals of the same circuit, which is the
"three renderers or one seam?" question the #536 boundary comment already flagged and
deferred.

The alternative I would actually build: have the print render emit an immutable **figure
document** — a list of `(symbolId, transform, path[], textRun[])` with advances already
resolved from the owned metrics — and make SVG, PDF and TikZ ~200-line serializers over
it. Consequences worth weighing: determinism becomes a property of one data structure,
provable by one test on one platform, with the platform matrix demoted to a smoke check;
PF-2's approximation table becomes a per-`symbolId` mapping (a table, reviewable) instead
of a rendering judgement call; PF-5's ratchet checks symbol ids in the document rather
than warnings in an exporter; and #546/#551 become themes over the same document. It
costs perhaps 2–3 mw more than the Graphics2D route and takes ~2 mw straight out of
PF-2. **The moment to decide is now**, at #711's slot — a PDF backend hard-wired to
`Graphics2D` makes the display-list seam a refactor later instead of a shape today.
If the answer is "not yet", record it as a scope decision in ARCHITECTURE.md with a
revisit trigger (PF-2 filing), the way #221 and #222 were recorded.

## One alignment note the issue misses

`CircuitRenderer implements Printable`, and JLS already ships `-p`/`-v`/`-r` paper
printing. A print *theme* and a print *renderer* that are not the paper-print path give
JLS two "print" renderings that will drift — the exact failure #536 AC-4 forbids for
symbol vocabularies, one level up. Route paper printing through the same theme once #707
lands, and PDF export becomes literally "print to a PDF device", inheriting page setup
for free. That also forces a decision the issue never states: a camera-ready figure PDF
should be a single page sized to the content bounding box, not US Letter, while paper
printing paginates. Say so explicitly or two people will implement it two ways.

## Where the kill criterion belongs

AC-3 restates KC-24-1 here, but the determinism bet is won or lost in #707 (bundled font,
owned metrics) and #508 already prices the demo slice as *the risk-retiring first step*.
By the time #711 runs, #709 has been funded on an unretired risk. Move KC-24-1's gate to
#707's exit and leave #711 holding only the PDF-specific residuals — compression, dates,
document id. Keeping the gate at #711 makes it decorative.

## Bottom line

Endorse the outcome; keep "direct, not converted" — the codebase made that choice in
#154 and it costs almost nothing to honour. Reframe the work as: pin the invariant to
*diffable and render-identical* rather than *byte-identical*, restate AC-4 as a
pure-Java/license constraint rather than an implicit mandate to write a PDF serializer,
replace the cross-platform comparison with committed goldens, move the kill gate back to
#707, and decide the figure-document question before the PDF backend fixes the seam at
`Graphics2D`.
