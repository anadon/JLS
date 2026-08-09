# Issue #537: FEAT-C24-2: a drawn circuit becomes CircuiTikZ code that compiles standalone, with every approximation named and a sample document built in CI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "TikZ output". The end is: **a figure in a course document is derived
from the circuit that ran, and stays derived** — no hand-redrawing, no drift between
the picture and the behavior. CAP-24 (#505) says that plainly, and it is squarely on
JLS's trajectory: batch artifacts as documented contracts (`docs/batch-interface.md`),
`.jls` plain-text saves so circuits diff in version control, reproducible jars, VCD
handoff to external viewers, structural Verilog/VHDL emission. JLS has been steadily
becoming *a source of derived, checkable artifacts*. A figure is one more such
artifact. Endorse the goal without reservation.

The reframing is about where to cut, what to order it after, and what to test.

## Reframing 1 — TikZ is a language, not a render target; the seam already exists

The issue says this is "a from-scratch exporter behind the same theming seam as
FEAT-C24-1" and sets `ordering_after: [FEAT-C24-1]`. That places a *language emitter*
behind a *pixel renderer*, and behind a seam that cannot carry it: `src/jls/Theme.java`
is a 162-line record of eleven `Color` roles. CircuiTikZ output is typically
monochrome and carries no JLS colors at all. It consumes essentially nothing from
`Theme`, and nothing from FEAT-C24-1's hard part (owned text metrics — LaTeX sets the
type). What it consumes is **symbol identity**, which is FEAT-C24-5's registry-keyed
mapping (#540).

The pattern this feature actually wants is already in the tree, one package over:
`jls.hdl` — a language-neutral structural model (`HdlModel`, 1005 lines: ports, nets,
statements, plus a `renames` map recording exactly what legalization changed "so
emitters can document it"), two emitters over it (`VerilogEmitter`, `VhdlEmitter`),
and a typed contribution seam (`HdlExtensionPoints.EXPORTER` = `hdl.exporter`,
catalogued in `docs/extension-points.md`). That is precisely the shape of
schematic-figure export, minus geometry.

Concrete alternative: **`jls.figure.FigureModel` + `FigureEmitter`, a `figure.emitter`
extension point.** The model holds placed nodes keyed by the element registry's symbol
key, named ports/anchors per node, nets as polylines in grid coordinates, labels, and
a `substitutions` map (the analogue of `HdlModel.renames`). Emitters: CircuiTikZ
(#537), print SVG/PDF (#536), and — already visible in the tracker — CAP-19's browser
export, the accessibility capstone's tactile SVG, and #62's ELK auto-layout, which is
a *producer* of FigureModel rather than yet another exporter. The seam the issue names
has one consumer; this one has four or five, and it is the seam CAP-24 risk 2 ("two
symbol vocabularies must not fork") is groping toward without naming.

Consequence: `ordering_after` should read `[FEAT-C24-5]`, not `[FEAT-C24-1]`. This
feature is the one figure path immune to KC-24-1 (font determinism), so ordering it
behind the feature that owns that risk is backwards.

## Reframing 2 — the fork the acceptance criteria never name

There are two utterly different things that both "compile standalone under LaTeX":

- **Pixel-transcribed TikZ**: JLS's own symbol geometry dumped as `\draw` primitives.
  Trivially achievable — JLS already renders through a `Graphics2D` shim
  (`CircuitRenderer.java:313-358` uses `org.jfree.svg.SVGGraphics2D`), so a
  `TikzGraphics2D` would produce a compiling `.tex` in a weekend. It offers a
  courseware author almost nothing over `\includegraphics` of FEAT-C24-1's PDF: not
  editable in any meaningful sense, not in CircuiTikZ's vocabulary, not restyleable.
- **Anchor-native CircuiTikZ**: `\node[and port] (u1) at (4,3) {};` with wires as
  `\draw (u1.out) -- (u2.in 1);`. This is what authors hand-write and why they want
  it: the document's symbol library, the document's line weights, re-layoutable. But
  CircuiTikZ port bodies have their own sizes, pin spacing and anchor offsets, so
  JLS's grid coordinates do **not** transfer — wire routing must be re-derived against
  anchors. That re-derivation *is* the engineering core of this feature, and the issue
  never mentions it.

AC-1 ("compiles standalone with no manual edits") is satisfied by both, including by
the near-worthless one. I am disregarding AC-1 as written: it does not test the
property that decides whether this feature is worth 2–3 mw. Replace it with checks a
machine can run on the emitted source:

1. Every element with a CircuiTikZ-native counterpart emits a library node, not a
   hand-drawn substitute (assert the node vocabulary appears; assert the count of raw
   `\draw` shape primitives for those elements is zero).
2. Every wire segment is expressed between named node anchors, never absolute
   coordinates, except for explicit routing waypoints.
3. Structural golden: the emitted figure's node/anchor connectivity equals the
   circuit's net structure — the same `HdlExporter`-style walk both artifacts derive
   from, so a figure cannot silently disagree with the netlist.

## Reframing 3 — generate the approximation table, do not write it

Naming every loss (AC-3) is the right instinct, inherited from CAP-16. Hand-written,
it decays exactly the way PF-5's totality ratchet exists to prevent. And the loss
surface starts earlier than the issue thinks: not just memories, state machines and
truth-table displays, but gates with more inputs than the CircuiTikZ port shape
provides, bubbles, buses/vectors, tri-state, and wire-crossing conventions.

Make each element type declare its emission strategy in the registry —
`native | composed | boxed-substitute` — and emit the table as a build product of the
registry, ratcheted the same way PF-5 ratchets symbol totality. Precedent is in tree:
`HdlModel.renames` already exists so emitters can document what they changed. Then
print the applicable rows as a header comment in the generated `.tex` itself, so the
approximation travels with the artifact into the author's document, not only in a repo
file nobody opens.

## Reframing 4 — the LaTeX CI tax is mostly avoidable

AC-2 (byte-identical `.tex` across three platforms) is string generation; it needs no
LaTeX whatsoever. AC-1's compile check does, but compilation outcome is
platform-independent given a pinned distribution. Split them: byte-identity on all
three platforms with zero LaTeX installed; the compile lane runs once, on Linux,
against a pinned TeX Live image. This is the discipline the project already uses for
`iverilog`/`ghdl` (compile when present, skip cleanly when not) and the one KC-24-3
demands for the WaveDrom renderer. A full TeX toolchain in the required matrix on
macOS and Windows would be one of the heaviest dev dependencies in the repo, bought
for no fidelity.

## The out-of-the-box alternative, stated plainly

**Consider making CircuiTikZ the primary camera-ready vector path and cutting the
direct deterministic PDF renderer (#536's 4–6 mw, most of it text metrics).**

If the emitted artifact is *source*, byte-identity is trivially achievable and
KC-24-1 — the capstone's declared #1 risk, the one whose failure re-plans everything —
simply does not arise: LaTeX owns the fonts, the document owns the type. For non-LaTeX
users, screen-styled SVG already ships (#154) and "print theme" can be a flag on that
existing exporter rather than a new renderer with owned metrics;
`CircuitRenderer implements Printable` already gives a serviceable, if
non-deterministic, PDF via the platform print path. Under that plan #537 becomes the
spine of CAP-24 rather than its second act, the capstone loses its riskiest 4–6 mw, and
the deterministic-renderer work is deferred until someone actually needs a
pixel-exact PDF that LaTeX cannot produce. This should be adjudicated on #505 *before*
#536 is funded, not after — the two issues are currently ordered as if the answer were
settled.

## One alignment friction to record

An export authors are meant to hand-edit and an export CI regenerates byte-identically
pull against each other. Decide it explicitly, the way the project decided plain-text
saves: **sealed by default** (header comment naming source circuit, JLS version and
format, "regenerated — edits will be overwritten"), with a documented one-shot "seed"
mode for authors who intend to diverge. Provenance comments must carry no timestamps,
for the same reason the jar is reproducible.

## Net

The outcome is right and this may be the best-leveraged feature in CAP-24. The seam,
the ordering, and two of the five acceptance criteria are not. Also worth noting: the
"hazard-demo circuit" that AC-1 and four sibling issues test against does not exist in
the tree (`test/fixtures/` has three `.jls` files; none is it) — a shared figure corpus
is a prerequisite the whole capstone is assuming into existence.
