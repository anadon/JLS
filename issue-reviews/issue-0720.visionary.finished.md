# Issue #720: TASK-C539-1: a pure-Java APNG and GIF encoder with deterministic frame timing and a declared size budget
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the framing and the end is: *a reader of a lab handout sees a circuit behave across
N cycles.* #720 is the codec half of that, split out so determinism is assertable without a
canvas. The split is good instinct — the seam is just cut in the wrong place, and the thing
being built is one the platform already ships most of.

I am disregarding all four acceptance criteria as written. Reasons below, strongest first.

## 1. The animated figure should be vector, not raster — and then most of #720 evaporates

CAP-24's (#505) own premise is that "figures live in course repos under version control like
everything else in this project," and its §1 walk-through ends in a LaTeX document that
builds in CI. A 32-frame APNG is the one artifact in that bundle that betrays the premise:
git cannot diff it, review cannot read it, and a one-pixel theme change rewrites the whole
blob. Every other PF-1..PF-3 output is text.

The alternative the issue never considers: **one animated SVG**. A single `<svg>` with the
static schematic emitted once and N per-cycle `<g>` layers carrying only what changes —
wire-state colors and the signal-value overlays — sequenced by SMIL `<set>`/`<animate>` on
`visibility`, or a CSS `steps()` keyframe timeline. Consequences:

- **Pure Java by construction.** It is text emission. No encoder, no native library, no new
  dependency — AC-1 is satisfied by not existing.
- **Determinism is already shipped and already tested.** `CircuitRenderer.exportImage`
  (`/home/user/JLS/src/jls/edit/CircuitRenderer.java:314-359`) pins the fixed defs prefix
  (`svg.setDefsKeyPrefix("jls")`) and an explicit wire-layer/part-layer draw order precisely
  because "an unstable order would break byte-identical goldens," with
  `SvgExportTest#exportingTwiceIsByteIdentical` (`/home/user/JLS/test/jls/SvgExportTest.java`)
  holding it. Per-frame `SVGGraphics2D` instances with prefixes `jls-f00…jls-f31` keep IDs
  unique *and* deterministic. AC-2's determinism comes free from a discipline the repo has
  already paid for once.
- **AC-3 disappears as a category.** The size budget and its "fail rather than silently emit
  a large file" machinery exist only because 32 raster frames are 32 full images. Vector
  emits the geometry once; frames are deltas. There is no budget to police.
- **It rides PF-1's print theme and PF-5's symbol registry** instead of minting a parallel
  pixel path. CAP-24 §3 risk 2 already forbids forking the symbol vocabulary; forking the
  *output* path is the same mistake one layer down.

For the print half — animated SVG does not animate inside a PDF — the LaTeX-native answer is
`\animategraphics` over an N-page PDF, which PF-1 owes anyway (Open Question 3's recorded
default is a direct deterministic PDF renderer), and `CircuitRenderer.addToBook`
(`:280-286`) is already the paginated-output seam. Still zero encoders.

## 2. If raster survives, it is a wrapper, not a project

Raster has a real constituency the vector path does not serve: GitHub comments, LMS embeds,
non-LaTeX slides. But then #720's scope is a fraction of what it claims. Measured here on the
pinned toolchain (OpenJDK 25.0.3, the `[25,)` enforcer floor in `pom.xml`):

```
com.sun.imageio.plugins.gif.GIFImageWriter canWriteSequence=true
gif bytes=238 identical=true      # 4-frame sequence, per-frame delayTime=10, written twice
png bytes=278 identical=true
```

The JDK's GIF plugin writes animated sequences with per-frame `delayTime` metadata via
`prepareWriteSequence`/`writeToSequence`, and the output is byte-identical run to run. So
AC-1's "a GIF writer exists in pure Java; no native library, no new runtime dependency" is
satisfied by the platform *today*, with zero new encoder code, on the same `ImageIO` path
`exportImage` already uses at `:381`. The only genuinely new artifact is the APNG
**container** — `acTL`, per-frame `fcTL` (which is where AC-2's frame-timing metadata lives,
as `delay_num`/`delay_den`), `fdAT` around `ImageIO`-produced deflate streams, `CRC32` per
chunk. That is a few hundred lines of chunk assembly, not an encoder. Honest price: ~0.2–0.4
mw, not the declared 1–1.5.

And ask whether APNG earns its slot at all. GIF's 256-color ceiling is a constraint for a
screen-styled canvas and a *non*-constraint for a print-styled schematic, which is flat-color
line art far under 256 colors by construction. If PF-5's print theme is the frame source —
and it must be, or the animation is the only screen-styled figure in a camera-ready bundle —
then GIF alone suffices and the second writer is dead weight from day one.

## 3. The determinism claim tests the easy half

AC-2 asserts that encoding the same frames twice is byte-identical. That is same-process
determinism, which any pure function gives you for free; my four-frame probe above passes it
without a line of JLS code. The claim that matters for a version-controlled course repo is
CAP-24 AC-2's claim — byte identity across the Linux/macOS/Windows CI matrix — and for
*compressed* output that is a property of the deflate implementation, not of the calling
code. A hand-rolled encoder calling `java.util.zip.Deflater` inherits the JDK's bundled zlib;
identity across the matrix holds only because the JDK is pinned, and nothing in #720 tests
it. To own the strong claim you would have to emit stored (uncompressed) deflate blocks —
trivially deterministic, much larger, which drives you straight back into AC-3's budget. The
vector path has no such tension: text has no compressor.

Note that CAP-24 AC-2 pointedly covers "SVG, TikZ and WaveJSON," not the animation. The
animation is the one artifact excluded from the cross-platform byte-identity claim — a tell
that the raster choice was already known to be the awkward one.

## 4. The reusable asset is a timeline, not a codec

#720 cuts the seam at *frames in → container bytes out*. Cut it one layer up instead: a
`Timeline` value type — an ordered sequence of per-cycle signal deltas plus a timing table,
derived from the recorded run (#498 §7.2) — with thin emitters hanging off it (animated SVG,
N-page PDF, GIF via `ImageIO`). This keeps everything #720 wanted from AC-4: the type knows
nothing about circuits, synthetic timelines drive the tests, determinism is asserted without
a canvas. But the durable artifact is the abstraction rather than a codec.

That matters because the same time-window extraction is owed to **PF-3** (WaveJSON over a
selected window) and to **CAP-23's chronogram slice (#504)** — which is on #508's funded
list, unlike this one. Three independent extractions of "these signals, these cycles, from
this recorded run" is the duplication this project's registry-totality instincts exist to
prevent. One timeline feeding chronogram, WaveJSON and animation is real consolidation, and
it makes the animation a ~0.3 mw emitter on top of already-funded work rather than a
standalone 1–1.5 mw codec on top of nothing.

## 5. Against the arc

The disposition note is honest and I will not relitigate it: #508 recommends cutting FEAT-C24-4
outright, and both #539 and #720 say do not fund ahead of an explicit REPLAN on #505. Worth
adding that #508 also lands a planning ratchet — "no new tier:feature/tier:task until two
capstones close" — and #720 was filed the day after, into a ~600–1,700 mw filed programme
against bus factor 1 and zero adoption. The animated figure has no requesting user; the live
user base (bsiever/WashU) has asked for nothing here. If PF-4 survives adjudication at all, it
should survive as the cheapest emitter on an already-funded timeline, which is exactly what
the redirect above delivers.

## Concrete redirect

1. Do not file or fund a codec. Close #720 into a REPLAN on #505 that re-scopes PF-4 to
   **animated SVG + N-page PDF**, produced by PF-1's print renderer over a `Timeline`.
2. Land `Timeline` under CAP-23 #504 / PF-3, where it is already needed; AC-4's
   circuit-ignorant testability lives there.
3. If a raster deliverable is still wanted after that, spend ~0.2 mw on a `GifSequence`
   wrapper over `ImageIO`'s existing sequence writer, and file the APNG chunk writer only if
   someone names a consumer that GIF fails. Rewrite AC-2 to assert byte identity across the
   CI matrix, not within one process — the within-process version proves nothing.
