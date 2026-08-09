# Issue #718: TASK-C538-2: a headless mode turns a batch-produced VCD into WaveJSON from a signal list and a window, checked against a rendered golden SVG
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

TASK-C538-2, the headless half of FEAT-C24-3 (#538, itself PF-3 of capstone
CAP-24 / #505). It is ordered after TASK-C538-1 (#716, the GUI half). I
fetched #538, #716, #505, #216 (closed, cited as the handoff this closes the
figure gap on), and #405 (cited as untouched/reference-only). The paperwork
checks out: `part_of_feature: 538` matches, `band_mw: 1-1.5` is consistent
with #505's PF-3 total (2-3 mw split across two tasks), and the
`VcdToWaveJsonGoldenTest`/CAP-24-AC-3 citation is a verbatim match to #505 §4
("CLI output over a batch-mode VCD renders via the pinned renderer to the
checked-in golden SVG"). The problems are in what this task asks an
implementer to build against, most of which is either unspecified or
inherited-but-unresolved from its two upstream issues.

## Findings, most severe first

**1. (High) AC-1 names no CLI grammar, and the project's own flag table has
no precedent for what this AC needs.** "A headless invocation converts a
batch-produced VCD ... given a signal list and a time window" — no flag name,
no signal-list syntax (comma list? repeated flag? a companion file, the way
`-t` takes a file?), no window syntax (two numbers? a range string? which
time units — the VCD's declared `timescale 1 ns`, or raw JLS sim-time
units?). `src/jls/JLSStart.java:759-788` (`FLAGS`, "the single authoritative
flag specification") shows every existing flag is `Arity.NONE`, `OPTIONAL`,
or `REQUIRED` with exactly **one** plain string operand — there is no
existing flag that takes a list or a two-part range. `docs/batch-interface.md`
is normally rigorous here (a four-production BNF for `-t`'s grammar,
`JLSStart.usageError`'s exit-code table), which makes this task's silence
conspicuous rather than merely terse. As written, AC-1 is satisfiable by any
ad hoc syntax an implementer invents, including one that collides with the
existing `-vcd`/`-t`/`-export` flag namespace or that cannot be typed on a
Windows command line the same way as a POSIX shell. Recommendation: pin the
flag name and the signal-list/window grammar before implementation, in the
BNF style `docs/batch-interface.md` already uses for `-t`.

**2. (High) AC-2's fixture — "a committed VCD" — does not exist, and the
task that logically owns creating a shared one (#716) has the identical gap
under a different name.** `test/fixtures/` holds four `.jls` circuit files
and no `.vcd` file; the project's only existing VCD goldens
(`test/jls/VcdExportGoldenTest.java`) are inline Java string constants, not
standalone committed files a new CLI test could point `-vcd`-derived input
at. Meanwhile #716 (TASK-C538-1) AC-1 requires "the hazard-demo run," which
per this fleet's review of #538 does not exist anywhere in the tree either.
Since #718 is `ordering_after: [TASK-C538-1]` and AC-4 (below) requires the
GUI and CLI paths to agree on "the same run, signals and window," the
natural reading is that both tasks should share one fixture — but neither
task assigns ownership of producing it, and #718 doesn't even reference
#716's fixture by name. An implementer could satisfy AC-2 with a
throwaway VCD that has nothing to do with #716's eventual fixture, quietly
defeating AC-4's cross-check intent. Recommendation: name the shared fixture
(ideally reusing `fork-4.6-shiftregister.jls` or another already-committed
circuit, dropping "hazard-demo" per the #538 review's same recommendation)
and state explicitly that #716 and #718 test against the identical run.

**3. (Medium-High) AC-4 ("one converter, not two") asserts an outcome with
no named interface to enforce it, and the two paths' actual data sources
differ in a way that can defeat exactly this equality claim.** #716's GUI
path selects over "the chronogram/trace surface" — i.e. `Trace`
(`src/jls/edit/Trace.java`), which the parent #538 review found caps
scrollback at `MAX_RETAINED_CHANGES = 100_000` per signal. #718's CLI path
parses a VCD text file, which retains the full run with no such cap (per
#405's O7 on `BatchSimulator.eventTrace`/`probeTrace`). AC-4 says the two
paths "produce identical WaveJSON for the same run, signals and window" but
never says they route through one shared function taking a common
in-memory representation — only that the outcome should hold. A conforming
implementation could pass AC-2's small golden fixture with two
independently-written emitters that happen to agree there, while silently
diverging on any run long enough to hit the GUI-side cap. Recommendation:
name the shared converter function/class explicitly (e.g., both paths adapt
to one `TraceSample`-shaped or VCD-shaped intermediate before WaveJSON
emission) as an acceptance criterion, not just an outcome sentence.

**4. (Medium) AC-3's "no network and no Node dependency" has no enforcement
mechanism named, unlike this project's habit of ratchet-testing exactly this
shape of promise.** Compare `HeadlessCoreRatchetTest` (Simulator/
BatchSimulator import no AWT/Swing), `NotificationRatchetTest` (no raw
`JOptionPane`), `ElementConstructorContractTest` — every comparable
never-regress claim in this codebase gets a named test. AC-3 gets none;
"the shipped jar gains no network and no Node dependency" is currently just
prose. It is also not yet resourced: `grep -rn "node\|npm\|wavedrom"
.github/workflows/*.yml` returns nothing, so the CI-side WaveDrom renderer
pin this criterion depends on doesn't exist yet either. Recommendation: name
a test (e.g., a `pom.xml`/shaded-jar content check, or a classpath scan) that
fails the build if the WaveDrom tooling leaks into the packaged jar.

**5. (Medium) "Unknown signal names refused by name" doesn't say which exit
code or stream applies, despite the project's CLI contract (issue #42) being
unusually precise everywhere else.** `docs/batch-interface.md` §1 gives a
three-row exit-status table (0/1/2) and even calls out a "Known deviation"
for `-t` parse errors going to stdout instead of stderr — this project
clearly tracks this distinction carefully. An unknown signal name is
ambiguous between a usage error (exit 2, the operand was malformed) and a
runtime failure (exit 1, the VCD didn't contain what was asked for); AC-1
picks neither. Recommendation: state the exit code and stream explicitly, the
same way the existing table does for every other failure class.

**6. (Low) "A time window" doesn't state units or inclusive/exclusive
boundary semantics, which matters precisely because AC-4 claims byte
equality against #716's UI-selected window.** #716 AC-3 requires "the
exported window boundaries equal the selected ones exactly" for a
mouse-driven pixel/drag selection; #718 offers a numeric CLI argument with no
stated unit (VCD's declared `timescale 1 ns` vs. raw JLS sim-time units) and
no stated boundary rule (does a transition exactly at the window edge
count?). A silent mismatch here is a plausible way for AC-4 to fail on a
real run while both tasks' own goldens (built to match their own
conventions) still pass in isolation. Recommendation: state the unit and
inclusive/exclusive convention once, shared by both tasks.

## What holds up

- The `part_of_feature`/`band_mw`/`ordering_after` machine block is
  internally consistent with #538 and #505; no scheduling contradiction.
- The `VcdToWaveJsonGoldenTest` / "CAP-24 AC-3" citation is a verbatim match
  to #505 §4's own AC-3 text — no drift, unlike some siblings in this
  cluster.
- Criterion 3's KC-24-3 quote ("drop the rendered-SVG golden, keep the
  WaveJSON artifact") accurately reflects #505's kill criterion and is a
  real, checkable escape hatch if the renderer pin proves costly.
- Criterion 5's #405 reference is correct: #405 is a pure internal rewrite of
  `BatchSimulator`'s VCD *writer* (P3 in #405 requires byte-identical output
  against the existing goldens), so a VCD-parsing consumer built today is
  unaffected by #405 landing before or after this task, regardless of #405's
  own open status.
- Citing #216 (closed) as "the documented VCD-to-viewer handoff" this task
  closes a figure gap on is accurate — #216's own scope statement explicitly
  excludes figure generation, so there's no overlap or double-claim.

## Verdict rationale

Two High findings (no CLI grammar, no owned/shared fixture) mean an
implementer cannot start this task today without inventing load-bearing
design decisions the issue should have pinned, and the Medium-High AC-4 gap
means even a good-faith implementation risks a false-green pass on the one
criterion meant to guarantee GUI/CLI consistency. The parts that are
solid (citations, kill-criterion carryover, the #405/#216 scoping) are
solid throughout, but they don't offset the missing interface contract.
