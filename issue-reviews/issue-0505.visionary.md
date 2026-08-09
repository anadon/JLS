# Issue #505: CAP-24: every figure in a lab handout — schematic, timing diagram, eight-cycle animation — is exported camera-ready from the circuit that actually ran, and the LaTeX doc builds in CI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

The stated product is five file formats. The real product is one property:
**an artifact in someone else's document that cannot disagree with what JLS
does.** Everything else in the body — TikZ, WaveJSON, APNG, PDF — is a
delivery vehicle for that property, and the vehicles are separable from it.

Strategically the target is right and better than the issue admits. #508's
audit says adoption, not capability, is the bottleneck: zero external issues
here, the live course (WashU CSE 260M) on a different fork. Figures are the
one artifact class that leaves the tool and lands in front of students who
have never run it. A handout with `exported by JLS 5.x, circuit at commit
abc123` in the caption is a distribution channel disguised as a feature —
the same insight behind #508's "SVG gallery" line item, at capstone scale.
So: the goal is endorsed. The decomposition is where I part company.

## The trajectory this sits on, and the seam it should have cut

The tree has already done most of this work under a different issue number.
Issue #77's wave extracted per-element rendering out of the element classes
into a registry of renderers (`src/jls/edit/ElementRenderers.java`,
~30 `src/jls/edit/*Renderer.java` classes). SVG export at
`src/jls/edit/CircuitRenderer.java:312-360` then works by handing those same
renderers a *different* `Graphics2D` (JFreeSVG's) — with a deterministic draw
order, a pinned defs prefix, and a byte-identity test
(`test/jls/SvgExportTest#exportingTwiceIsByteIdentical`). Totality is already
ratcheted: `test/jls/ElementDrawSmokeTest#everyElementDrawsOnTheSvgExportPath`
sweeps the compiled element classes.

That is one-pass-many-sinks, shipped and tested. The only thing pinning it to
one output family is the *type of the sink*: `java.awt.Graphics`.

**The reframe: the deliverable is not five exporters. It is one
device-independent draw interface plus thin back ends, applied to two
scenes.**

- One sink interface (`jls.figure.FigureSink`, AWT-free): `line`, `path`,
  `rect`, `arc`, `text(x, y, hAlign, vAlign, style, String)`, `symbol(id,
  box, orientation)`, `beginElement(kind, id)` for structure. Renderers draw
  to it instead of `Graphics`.
- Back ends: **AWT** (screen and raster, byte-identical to today), **SVG**
  (our own emitter, ~300 lines), **TikZ**, and **frame sequence** for
  animation. Each is a few hundred lines because it implements one narrow
  interface, not a format-specific redraw of thirty elements.
- Two scenes: the schematic (`CircuitRenderer`) and the timing trace
  (`src/jls/edit/Trace.java`, 626 lines — whose layout math is *already*
  headless and AWT-free in `src/jls/sim/TraceGeometry.java`, extracted by
  #121 for exactly this kind of reuse).

Five planned features collapse into one seam and four adapters. And the seam
is not CAP-24's private asset: CAP-19 (browser export), CAP-23's chronogram,
the accessibility capstone's tactile SVG, and the #43/#44 IEC/IEEE symbol
work all want the same thing. The issue treats that sharing as *integration
risk 2* ("two symbol vocabularies must not fork"). Under the reframe there is
no risk to manage, because there is only ever one vocabulary — the seam is
the feature and the themes are data.

## What the reframe does to each planned feature

**PF-5 (print-symbol totality ratchet) should not exist as authored work.**
As written it commissions a second symbol library, drawn separately from the
screen symbols, and then commissions a ratchet to keep the two in sync — a
problem created and then solved in the same feature. One geometry
parameterized by a theme (stroke weight, fill, font, color) has nothing to
drift. The real gap is narrower and already named in `ARCHITECTURE.md`: the
`#76` foreground sweep. `src/jls/Theme.java` (162 lines) centralizes
*wire-state* colors only; element bodies and labels are hardcoded black at
~126 call sites. A print theme is that sweep finishing, which #76 owes
anyway. Totality then rides `ElementDrawSmokeTest`'s existing sweep with a
third parameterization, not a new ratchet. **OQ-1 (ANSI vs IEC) stops being
a filing blocker** — it becomes a theme value that #43/#44 supplies later.

**PF-2 (CircuiTikZ) should emit TikZ paths first, CircuiTikZ macros only
where they are strictly better.** The issue commits to a symbol mapping and
then commissions an "approximation table" cataloguing what CircuiTikZ cannot
express — memories, state machines, truth-table displays, i.e. precisely
JLS's distinctive elements. A path dump from the sink is 1:1 by construction:
zero mapping, zero approximation table, zero possibility that the figure
disagrees with the tool — which is the capstone's entire premise. Hand-mapped
CircuiTikZ symbols can disagree; exported geometry cannot. Keep a small
opt-in macro-substitution table for gates, where an author genuinely wants
the canonical ANSI glyph in the document's own line weight. Risk 3 and the
last DoD checkbox dissolve.

**PF-1's PDF is redundant and should be dropped.** `\documentclass{standalone}`
over the TikZ file *is* the camera-ready PDF, in the document's own fonts —
strictly better than a JLS-emitted PDF for the LaTeX audience, and the
non-LaTeX audience wants SVG. **OQ-3 (PDF direct vs. conversion) is
answered "neither"**, and one of the two questions blocking a filing
disappears rather than being adjudicated.

**PF-3 should render the timing figure, not delegate it.** The chain
"JLS → WaveJSON → pinned WaveDrom renderer → golden SVG" (AC-3, KC-24-3,
risk 5) exists only because the plan assumes JLS cannot draw a waveform. It
already does, in `Trace.java`, with headless geometry in `TraceGeometry`.
Point the trace renderer at the sink and the timing figure comes out in SVG
and TikZ from the same three back ends — no Node in CI, no renderer pin, no
golden that depends on an external tool's version. WaveJSON then demotes to
what it actually is: an *interchange* format for authors who want to hand-edit
in WaveDrom, emitted from `BatchSimulator`'s `TraceSample` accumulation
beside the VCD writer for a fraction of the 2–3 mw priced. The standalone
VCD→WaveJSON CLI is a pure file-to-file transform over an already-documented
format (`docs/batch-interface.md`) with no dependency on JLS internals — it
belongs in `scripts/` next to the existing `docs/vcd-interop.md` bridge
recipe, not inside the shipped jar.

**PF-4: cut the APNG, ship the filmstrip.** #508 already recommends cutting
this and #539 records it. I agree, but for a design reason rather than a
budget one: an animated GIF does not animate in the printed handout, the PDF
lab manual, or the slide PDF that is the stated audience's medium. The
artifact that actually serves "show me eight cycles" on paper is a
**filmstrip** — N cycles rendered side by side in one static SVG, values
overlaid, one caption. That is the frame-sequence back end plus a layout
loop, it costs a fraction of a pure-Java APNG encoder, it prints, it diffs in
review, and it lives in a course repo under version control — the outcome's
own premise, which an APNG satisfies worst of all the five artifacts. If a
moving figure is later wanted for the web, that is CAP-19's medium, not this
one's. AC-5 goes away.

## The determinism claim is a design choice, not a measurement

KC-24-1 and AC-2 treat byte-identical SVG as an empirical property to be
measured on three CI platforms, with "owning text metrics" as the known-hard
part and most of PF-1's 4–6 mw. That framing is downstream of an
implementation detail: today's renderers call `g.getFontMetrics()` (31 files
under `src/`; `AdderRenderer.java:60,75,97,119,140`, `MemoryRenderer:55`,
`MuxRenderer:57`, `DecoderRenderer:45`, `CircuitRenderer:201`, and a dozen
more) and bake measured pixel offsets into the coordinates JFreeSVG
serializes. Metric-derived numbers in the file are what makes the file
platform-dependent.

Byte-identity of a *text file* does not require metric determinism. It
requires that no measured quantity be serialized. Emit
`text-anchor="middle"`, `dominant-baseline`, and the string; let the viewer
measure. Then make it structural: **the sink interface simply does not expose
`getFontMetrics()`**, so no renderer can reintroduce the dependency on the
export path, and the ratchet is the compiler rather than a three-platform CI
matrix. The residual is genuine but small — a handful of elements size their
box to fit a label — and is served by one embedded width table for one
bundled font, used for layout only.

Two payoffs the issue does not claim. Our own SVG emitter drops the JFreeSVG
dependency from the shaded jar; and anchored text plus a self-emitted
document means headless export stops needing an installed font at all — the
`ghcr.io/anadon/jls` container can drop `fontconfig` and `fonts-dejavu-core`,
which README currently lists as required even for batch image export.

**I am explicitly disregarding AC-2, AC-3, AC-5 and OQ-3 as stated.** AC-2
becomes a unit assertion that the export path never touches `FontMetrics`,
plus the existing byte-identity test; AC-3 becomes a golden over JLS's own
rendered timing SVG; AC-5 becomes a filmstrip layout test; OQ-3 is answered
by deleting the PDF back end. Each of these is a stronger guarantee obtained
more cheaply, not a weakened one.

## Two things the plan is missing

1. **PF-6's input does not exist.** Risk 4 requires the bundle to consume "a
   recorded run artifact" (#498 §7.2), yet `requires_features`,
   `requires_capstones` and `requires_tasks_exception` are all empty and
   nothing in `src/` records or replays a run. Either PF-6 takes a
   circuit + test-vector file + time window (which exists today via `-t`)
   and derives the run itself, or CAP-24 has an unfiled hard dependency.
   The former is the right answer: the batch interface is already a stability
   contract, so "one run" means "one deterministic batch invocation" and
   self-consistency follows for free.
2. **The caption is the feature.** Nothing in five PFs stamps a figure with
   the circuit hash, JLS version, and simulation window that produced it.
   That one line is what makes a figure auditable a year later, and it is the
   cheapest thing in this issue. Put it in the sink so every back end gets it.

## What I would fund

The demo slice, re-cut: **the sink interface, the AWT back end (proving
byte-identical screen output), the SVG back end with anchored text, and the
schematic scene** — the gate/wire/pin subset first, then the sweep. That is
roughly the 2–3 mw the issue already budgets for its demo slice, but it buys
the shared seam four other capstones need rather than one exporter's
determinism experiment. The TikZ back end lands next at well under PF-2's
2–3 mw, the trace scene after it. Standalone band lands nearer 5–8 mw than
12–19, and it lands on #77's critical path instead of beside it.

Endorse the outcome. Re-cut it along the sink.
