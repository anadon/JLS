# Issue #538: FEAT-C24-3: selected signals over a chosen window become a WaveJSON timing figure — interactively and from a VCD in headless batch
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is actually for

Strip the format names away and the claim is: *a timing figure in a handout must be
derived from the run, and must stay derived from it.* That is the same claim CAP-24
(#505) makes for schematics and the same claim the batch contract already makes for
stdout and VCD. It is a good claim and it belongs in JLS. Nothing in the tree owns it
today: `grep -rli "wavedrom\|wavejson" src/ test/` is empty, and the closed VCD-handoff
work (#216, `docs/vcd-interop.md`) deliberately stops at "an external viewer displays
it" — a viewer session is not an artifact you can commit.

So I endorse the outcome. I do not endorse either of the two seams the issue picks to
reach it, and I am explicitly disregarding acceptance criterion 2 as written. Reasons
below.

## Reframing 1 — the VCD is the wrong source

The issue makes the batch VCD the input to the headless path and (by omission) leaves
the GUI path to read live traces. Two consequences the issue never confronts:

- **JLS's VCD is a deliberately lossy, pinned subset.** `BatchSimulator.toVcd`
  (`src/jls/sim/BatchSimulator.java:384-476`) emits `$var wire <bits> <id> <fullname>`
  for *everything*. There is no element kind, no clock identity, no bus/field grouping,
  no radix, and the timescale is nominal (`docs/batch-interface.md` §4.2). The subset is
  a contract, pinned by `VcdExportGoldenTest`, and rightly so.
- Acceptance criterion 1 demands **clock grouping and bus lanes**. From a JLS circuit
  that information is free — there is a `Clock` element, elements carry `getBits()`, and
  the trace rows already know their bit width (`Trace(name, el, bits, …)`). From a VCD
  it is *gone*, and the CLI can only recover it by periodicity heuristics or by asking
  the user to name the clock on the command line. AC-1 (GUI, rich) and AC-3 (CLI,
  golden) therefore test two different-quality products, and the golden pins the poorer
  one.
- There is **no VCD reader anywhere in the shipped tree** — the only parser is the
  spec-derived one inside `test/jls/VcdExportGoldenTest.java:32`, written specifically
  *not* to share code with the emitter. The CLI mode as specified silently requires
  JLS's first production VCD parser, which then inherits the whole hostile-input
  discipline (`UntrustedFileHardeningTest`, the `LoadError` taxonomy) that every other
  reader in this project pays. That is not inside a 2–3 mw band.

CAP-24 already names the right seam and #538 walks past it: risk 4 of #505 says the
bundle "must take a **recorded run** artifact as its input", citing #498 §7.2's
recording-is-the-contract discipline. Make *that* the source. One pure function

    WaveJson.of(RecordedRun, signalSelection, window) -> String

with `RecordedRun` produced by the batch simulator directly (it already holds
`Map<LogicElement,List<TraceSample>>` plus probe traces) and by the interactive
simulator's committed `Trace` change lists. The GUI path and the batch path then differ
only in *how the run was recorded*, never in what the figure means, and clock/bus
metadata survives because it never had to round-trip through a wire-only format.
VCD→WaveJSON becomes at most a documented, explicitly degraded convenience mode
("no clock lanes, buses by width only") for VCDs JLS did not produce — or is dropped
entirely from v1.

## Reframing 2 — WaveDrom is the wrong renderer, and JLS already owns a better one

AC-2 pins an external WaveDrom renderer to produce a golden SVG; KC-24-3 exists solely
to contain the Node dependency that pin drags into CI. That whole apparatus is
avoidable, because the pieces of a native renderer are already in the jar:

- `org.jfree.svg` is a **shipped runtime dependency** (`pom.xml:67-71`), and
  `CircuitRenderer` (`src/jls/edit/CircuitRenderer.java:312-358`) already produces
  deterministic SVG by pointing the ordinary `Graphics2D` drawing code at
  `SVGGraphics2D` — "so `.svg` output needs no per-element work".
- `Trace.paintComponent` (`src/jls/edit/Trace.java:229-431`) is exactly that: waveform
  drawing against a `Graphics`. Point it at an `SVGGraphics2D` and a timing figure falls
  out of code that already exists and is already regression-tested.
- The headless geometry is already extracted and unit-tested for this purpose:
  `jls.sim.TraceGeometry` (tic increment, label stride) is explicitly AWT-free and
  guarded by `HeadlessCoreRatchetTest`.

Two caveats, and both *strengthen* the case rather than weaken it. `Trace.paintComponent`
draws in hardcoded screen colors (`Color.pink` at :257, `Color.magenta` at :418) — the
same screen-vs-print problem #536 (FEAT-C24-1) must solve for schematics; and byte-identical
text needs the owned-metrics path #536 must build anyway (`jls.core.TextMetrics`,
`jls.edit.SwingTextMetrics`). So a native timing renderer is **co-funded** by #536's
print theme and metrics work, not a parallel stack. One theming seam, two subjects
(schematic, waveform), one deterministic SVG writer, one determinism test.

**I am disregarding acceptance criterion 2.** A golden that renders JLS's WaveJSON
through pinned WaveDrom and diffs the SVG is, on any red run, primarily a test of
WaveDrom's rendering stability — the one component the project does not control and has
promised never to ship. Replace it with: (a) a WaveJSON **semantic** golden (parse the
emitted JSON, assert lanes/values/groups against the run — the same "parser written from
the spec, not from the emitter" discipline `VcdExportGoldenTest` already uses), and
(b) a byte-identical golden over **JLS's own** rendered SVG, which is the artifact the
instructor actually pastes into the handout. KC-24-3 then has nothing to kill.

## Reframing 3 — the missing deliverable is a profile document

Every other interchange surface in this project is a normative document plus a golden:
`docs/batch-interface.md`, `docs/file-format.md`, `docs/simulation-semantics.md`, the
whole `docs/standards-adoption/` tree. #538 proposes an exporter and two goldens and no
document — and the project has already written down why that is dangerous here.
`docs/capability-roadmap/sweep-01-values-and-logic.md:46` (row 47) states it flatly:
WaveJSON's alphabet is `0 1 x z = u d 2..9`, "JLS can populate three of those
characters", and "a WaveDrom exporter today would emit a strict subset and misrepresent
itself the way … an EVCD writer would." That is the same pretence the project refused
for EVCD (`standards-adoption/07-waveform-formats.md`) and disclosed in the generated
VHDL header. #538 cites neither the sweep nor the constraint.

There is a second reason the document is the load-bearing artifact: **#369 (FEAT-053)
will consume the same notation as a test expectation.** CAP-24 §2 splits export from
expectation across two features, which is a defensible scope cut *only* if both sides
read one written profile. Without it, an exporter dialect and an expectation dialect
appear independently and drift — precisely the "two runners disagree about a verdict"
failure #369 §4 invariant 5 exists to prevent, transposed onto a file format. So:
`docs/wavejson-profile.md`, normative, naming the populated alphabet subset, the
non-populated characters and why, the clock-lane rule, the bus radix rule, the
time-compression rule, and the signal-naming rule shared with the VCD profile. That
document is what makes #369's half cheap, and it is the piece of #538 I would fund first.

## Cost, honestly

The `band_mw: 2-3` is understated under the issue's own design, before my reframing:
a production VCD parser (does not exist), a signal-selection and time-window UI (does
not exist — trace rows are populated purely from `Watchable.isWatched()` and probes at
`InteractiveSimulator.java:967-1002`; the only time controls are a scale factor and a
cursor slider), clock/bus inference, and the profile document. The hazard-demo circuit
AC-1 names is also not in `test/fixtures/` — someone has to build and pin it, and it is
shared with #536/#537/#541, so it should be filed once rather than assumed four times.
Under the reframing the VCD parser and the WaveDrom pin drop out and the native renderer
rides #536's metrics work, which is roughly a wash on cost and a large win on risk.

## What I would keep unchanged

The GUI-and-headless-from-one-engine instinct; the refusal to let WaveJSON-as-expectation
leak in from #369; the insistence on cross-platform byte-identity (AC-3); and the
absolute "no Node or network in the shipped jar" line — which my reframing does not
merely satisfy but makes unnecessary to state.

## Restated shape

1. `docs/wavejson-profile.md` — normative, with the honest-subset declaration. Land first.
2. `RecordedRun` as the source of every figure (shared with #541/PF-6), not the VCD.
3. `WaveJson.of(RecordedRun, selection, window)` — one pure function, two front ends.
4. A native timing-figure SVG renderer over `Trace`'s drawing code + JFreeSVG, on #536's
   print theme and owned text metrics.
5. Goldens: WaveJSON parsed and asserted semantically; JLS's own SVG diffed byte-for-byte
   on three platforms. No external renderer in any lane.
6. VCD→WaveJSON: a separately-filed, explicitly degraded mode, or not in v1.
