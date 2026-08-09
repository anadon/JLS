# Issue #718: TASK-C538-2: a headless mode turns a batch-produced VCD into WaveJSON from a signal list and a window, checked against a rendered golden SVG
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the mechanism away and the want is one sentence: **a timing figure for a
handout must be regenerable from a commit, headlessly, without a human opening a
GUI.** That is a real gap and it is squarely on the project's arc — CAP-24 (#505)
wants figures that live in course repos under version control, and #216 already
established the shape of the answer for the *viewer* handoff (VCD out, external
tool displays) while explicitly declining to make a figure.

Everything else in #718 — VCD as the input, WaveDrom-the-Node-package as the
checker, "signal list and window" as CLI arguments — is one implementation route
to that want, and it is not the one I would take. Three of the five acceptance
criteria are, in my reading, artifacts of the chosen route rather than of the
outcome.

## Reframing 1 (the load-bearing one): convert the trace, not the VCD

AC-4 already states the truth the rest of the issue argues against: *one
converter, not two*. But the two paths as specified do not consume the same
thing. TASK-C538-1 (#716) converts the live run — it has `LogicElement`s,
`getBits()`, and the fact that a signal *is* a `Clock` element. #718 converts a
VCD, where all of that has been flattened to `$var wire 1 ! clk`
(`BatchSimulator.toVcd`, `src/jls/sim/BatchSimulator.java`).

The consequence is concrete and, as written, fatal to AC-4: **clock-ness is not
recoverable from a JLS VCD.** #716 AC-2 requires clock signals to be emitted as
WaveDrom clock lanes ("p"/"n" rather than a "0101..." wave). From the VCD the CLI
must either guess by periodicity — in which case the two paths produce different
WaveJSON for the same run and AC-4 is false — or the GUI path must throw its
element knowledge away to match, which makes the *good* path worse to satisfy an
identity claim. The third option, annotating the VCD with a hint, collides with
`docs/batch-interface.md` §6: the VCD profile is frozen and every golden in
`VcdExportGoldenTest`/`VcdProbeExportTest` moves if a byte does. Bus width
survives the round trip; clock identity and hierarchy semantics do not.

The route that dissolves this: **make the converter's input the in-memory trace,
and give batch mode a `-wavejson` flag beside `-vcd`.** `BatchSimulator` already
exposes `getTraceSamples()` and the probe trace; `TraceSample(long time, BitSet
value)` is exactly the model both paths need; `jls.sim` is already AWT-free by
ratchet (`HeadlessCoreRatchetTest`), so one converter in `jls.sim` is literally
reachable from both the GUI trace surface and `JLSStart`. Then:

- AC-4 is true by construction rather than by test — there is one input type, one
  converter, one output.
- The CI recipe becomes `jls -b -wavejson fig.json --signals ... --window ...`,
  one invocation instead of two plus an intermediate file.
- No new inbound parser enters the shipped jar (see Reframing 2).
- `FigureDeterminismTest` (CAP-24 AC-2) has one producer to pin, not two.

**I am disregarding acceptance criterion 1 and the VCD-consuming half of AC-5**
for this reason: the stated input is the reason AC-4 cannot hold, and the outcome
("timing figures regenerate in CI without the GUI") is fully served without it.

## Reframing 2: if a VCD reader is built, it is not a figure feature

The issue treats "read a VCD" as free plumbing. It is not. `docs/capability-
roadmap/sweep-01-values-and-logic.md` row 66 records the fact plainly: JLS has
**no VCD reader**. Building one inside the shipped jar creates a new
untrusted-input parsing surface, and this project has a settled doctrine about
those — hostile-input caps (#38, `UntrustedFileHardeningTest`), a structured
error taxonomy rather than stack traces (`LoadError`, #58), fuzzing
(`ContainerMutationFuzzTest`). #718's only error requirement is "unknown signal
names refused by name". Nothing about a truncated `$var`, a value change against
an undeclared code, non-monotonic timestamps, a 2 GB dump, or the `x` values that
*every* third-party VCD contains and JLS's value domain cannot represent. A
1–1.5 mw band is priced for a converter, not for a hardened parser with its own
error contract.

And the reader, if built, is worth far more than a figure. A VCD reader is the
natural oracle for the HDL lineage: run the exported Verilog (`VerilogEmitter`)
under Icarus, read its VCD back, diff against the JLS run — that is a real
equivalence check for #33/#59, and it is also the shape FEAT-053 (#369) needs for
"expectation" input. **My recommendation: file the reader as its own capability
(`jls.vcd`, with a `LoadError`-style taxonomy and a fuzz corpus), owned by the
verification lineage, with figure export as a later consumer — not as an
unnamed dependency inside a 1 mw figure task.**

One trap to record while doing it: `VcdExportGoldenTest.vcdIsStructurallyWell
FormedAndTwoStatePlusHiZ` is deliberately "a parser written from the spec
document, not from the emitter". If a production reader lands, the temptation to
reuse it there will be strong and it would silently destroy the independence
that makes that test an oracle. The test's checker must stay separate.

## Reframing 3: JLS should render the figure, not pin a Node renderer

AC-2/AC-3 spend the issue's whole risk budget on a WaveDrom renderer pin and then
carry KC-24-3 as an escape hatch — a golden that the issue itself concedes may be
dropped. Consider the inverse: CAP-24's PF-1 is already committing to a
deterministic, byte-identical SVG writer with owned text metrics. A WaveDrom-
subset waveform renderer is rectangles, polylines and text — a small consumer of
that same substrate — and `jls.sim.TraceGeometry` already holds the headless,
AWT-free tic-increment and label-spacing logic extracted from the trace viewer
for exactly this kind of reuse.

If JLS emits **both** the WaveJSON (interchange, for authors who want WaveDrom)
and a deterministic SVG (the artifact the LaTeX document actually `\includegraphics`),
then: the golden is an in-tree byte comparison on all three CI platforms, CI
never needs Node, KC-24-3 never fires, and — the part that matters pedagogically —
an instructor rebuilding a handout needs a JDK and nothing else. Compare the
status quo the issue proposes: a course repo that must install a JS toolchain to
turn JSON into a picture. That is a worse deliverable than "screenshot the
simulator", which is what CAP-24 exists to replace.

## What I would keep unchanged

- AC-5's discipline (this consumes runs, does not change how VCDs are produced;
  #405's streaming rewrite is untouched) — correct, and worth restating in
  whatever replaces AC-1.
- "Unknown signal names refused by name rather than silently dropped." This is
  the single best line in the issue and generalizes: a figure that silently omits
  the signal the caption talks about is the exact failure CAP-24 exists to kill.
  It should be a named error in the CLI contract (`jls: error: ...`, exit 2), not
  a warning.
- The ordering behind #716. The GUI path first is right — it is where the
  signal-selection vocabulary gets designed.

## Alignment check

Does this pull against the project's arc? Only in its chosen seam. The trajectory
is unambiguous: one headless core, one converter behind two front ends (#369 §3
states the same principle for verdicts — "two runners is how a panel and a CLI
come to disagree"), no runtime dependencies in the jar, contracts pinned by
byte-goldens, untrusted input hardened at a named boundary. Converting the trace
rather than the dump, deferring the reader to a capability that can pay for its
own hardening, and rendering with JLS's own deterministic SVG writer are all
*more* aligned with that arc than the issue as filed. Rewrite AC-1..AC-3 along
those lines and this becomes a task I would fund immediately.
