# Issue #716: TASK-C538-1: selected signals over a chosen window export as WaveJSON, with clock grouping and bus lanes that match the run they came from
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the WaveJSON vocabulary away and the claim underneath is: *a JLS run
should be a first-class, addressable artifact that other things render.*
CAP-24 (#505) says this in its §3 risk 4 — "the bundle must come from one
recorded run" — and #498 §7.2 says it in one sentence the whole tracker
quotes: "an interactive session is a recording device; the recording, not
the session, is the contract." #716 is the first issue that has to actually
produce that recording, and it is written as if the recording already
exists and only needs a new file writer bolted onto the trace window.

It does not exist. That gap is the whole review.

## The recording does not exist on the path this issue targets

- The batch path has one: `BatchSimulator.getTraceSamples()` returns
  `Map<LogicElement,List<TraceSample>>`, headless by construction
  (`src/jls/sim/BatchSimulator.java`, `HeadlessCoreRatchetTest`).
- The interactive path has none. `InteractiveSimulator` is in package
  `jls.edit` (`src/jls/edit/InteractiveSimulator.java:1`, despite
  `ARCHITECTURE.md`'s module map filing it under `jls.sim`), and the run
  history it produces is stored inside a Swing component: `Trace` is a
  `JPanel` whose `private record Change(BitSet value, long when)` list is
  the only place an interactive run's transitions live
  (`src/jls/edit/Trace.java:20,51,73`).

So AC-1's phrase "matches the run" has no headless referent on the GUI
path. Written as-is, this task's exporter must read history out of a
`JPanel`, which (a) cannot be asserted against a golden without a Swing
harness, and (b) makes #718's AC-4 — "one converter, not two" — a promise
that has to be kept by discipline rather than by construction, since the
CLI would read a VCD into one model and the GUI would read a JPanel into
another.

## The model this issue needs already exists — as a local variable

`BatchSimulator.toVcd()` builds exactly the structure a WaveJSON emitter
needs, and then throws it away:

```java
record Sig(int bits, TreeMap<Long,BitSet> byTime) { }
TreeMap<String,Sig> signals = new TreeMap<String,Sig>();
TreeSet<Long> times = new TreeSet<Long>();
```

Ordered signals, widths, folded time→value histories, a shared change-time
axis, deterministic iteration order. That is the recorded run, scoped to
one method body inside a serializer.

**Concrete alternative framing.** #716's deliverable is not a WaveJSON
writer. It is `jls.sim.RunTrace` — that `Sig`/`fold` pair promoted to a
public headless value type with a `slice(Set<String> signals, long from,
long to)` operation — plus a WaveJSON writer that is a pure function of it.
Everything else falls out:

- `toVcd()` becomes `RunTrace → String`; the existing
  `VcdExportGoldenTest` goldens are the refactor's proof, unchanged.
- #718's CLI becomes `String → RunTrace` (a VCD reader) into the same
  writer. "One converter" stops being an assertion and becomes a type.
- `InteractiveSimulator` records into a `RunTrace`; `Trace` the JPanel
  becomes a *view* over it instead of the owner of the history. This is
  the same move the project already made once, deliberately, in
  `jls.sim.TraceGeometry` — tick/label math pulled out of
  `Trace.paintComponent` into the headless core precisely so its
  properties could be unit-tested (issue #121). #716 extends that
  precedent from the geometry to the data.
- AC-3's "exported window boundaries equal the selected ones exactly"
  becomes a property of `slice()`, provable in `test/jls/` Layer 1.

This is strictly *less* code than the issue as written, because it deletes
the second history implementation rather than adding a third consumer of it.

## The problem the issue never considers: WaveDrom is a sampled grid

WaveJSON's `wave` string is a uniform character-cell grid — one cell per
diagram time unit, `.` for continuation. JLS's trace is event-driven over
arbitrary integer simulation times. Exporting one into the other is a
*projection*, and the projection needs a quantum: how many simulation time
units per cell.

AC-1 says the WaveJSON's transitions must "match the run's transitions in
that window." With a fixed or guessed quantum that is not achievable in
general — transitions that don't land on a cell boundary get snapped, and
the figure silently disagrees with the simulation. That is the exact evil
the Outcome paragraph names. And the chosen fixture makes it worst-case: a
**hazard demo** is a circuit whose point is a glitch narrower than the
clock. `Clock.cycleTime` defaults to 2 time units
(`src/jls/elem/Clock.java:30`); a one-unit gate-delay glitch inside it is
precisely what a coarse quantum erases.

**Design proposal.** Make the quantum computed and declared, not assumed:
the gcd of all transition deltas in the sliced window, bounded by a cell
budget; emit it in the WaveJSON (`config`/`head` tick metadata) so the
figure is self-describing; and if the gcd would blow the budget, **refuse
by name** rather than approximate. Refusal is this project's house style —
`NEWER_FORMAT` refuses rather than misparsing (`Circuit.readFormatHeader`),
the `LoadError` taxonomy refuses rather than stack-tracing, #718 AC-1
refuses unknown signal names rather than dropping them, and CAP-24 makes
PF-2 name every TikZ approximation rather than simplify silently. A timing
figure exporter that quietly rounds is the one artifact in the bundle that
cannot be trusted, and untrustworthy is worse than absent.

## The clock/bus asymmetry is a decision this task forfeits by going first

AC-2 wants clock lanes and bus lanes. In-process, that information is
exact: `Watchable`/`isWatched()` selects the signals, the element *is* a
`jls.elem.Clock` with a known `getCycleTime()`, and `getBits()` gives bus
width. From a VCD — #718's input — none of that survives: VCD carries a
name, a width, and `wire`. So the GUI path structurally knows more than the
CLI path, and #718's AC-4 ("identical WaveJSON for the same run") is in
tension with #538's own boundary note that this feature *consumes* VCDs
without changing how they are produced. `docs/batch-interface.md:327`
freezes the §4 VCD profile, so smuggling roles into the VCD is a
major-bump-or-flag change, not a drive-by.

The resolution belongs in `RunTrace`: signal **role** (clock with period,
bus, plain) is a field of the recorded run, populated exactly from the
elements in-process, and populated on the CLI path from explicit
`--clock`/`--bus` arguments or a sidecar written next to the VCD — never
inferred by heuristic, because a heuristic that misclassifies a slow
data signal as a clock produces a confidently wrong figure. #716 going
first means this decision gets made here or gets made by accident in #718.

## Ordering: this task should not be first

`ordering_after: []` on #716 and `[TASK-C538-1]` on #718 puts the GUI path
first and the headless path second. Under the reframing, the dependency
inverts: `RunTrace` + the WaveJSON writer + the VCD reader are verifiable
today on all three CI platforms with no display, while the GUI half is a
menu item plus a selection gesture over a surface that has no range
selection yet. Building the CI-verifiable core first is also how AC-4
(`FigureDeterminismTest` byte-identity) gets cheap: for a text artifact
produced by a `TreeMap`-ordered serializer, determinism is a property
`VcdExportGoldenTest` already demonstrates the project can hold.

## Disregarding AC-3 as written

**AC-3 — "signal selection and window selection are made in the UI over
the chronogram/trace surface" — should not gate this task, and I am
disregarding it.** `test/jls/ui/package-info.java` documents Layer 1
(headless model assertions) as the only layer that exists; Layers 2 (Swing
under a display) and 3 (render-to-image) are reserved, and CI has no
display. Binding a 1–1.5 mw task's acceptance to a UI gesture either makes
it unverifiable in CI or silently prices in building Layer 2. The
verifiable content of AC-3 is the *selection model*: a headless
`TraceSelection` record (ordered signal names + `[from,to]`) that `slice()`
consumes, asserted in Layer 1, with the trace-window gesture as a thin,
hand-verified editor of that record. Same guarantee, testable, and it
matches how the project already splits `TraceGeometry` from `Trace`.

## What I would fund

1. `jls.sim.RunTrace` (+ role metadata, `slice`), `toVcd()` refactored onto
   it with existing goldens unchanged.
2. `RunTrace → WaveJSON` writer with a computed, declared quantum and
   refusal-on-loss; golden-asserted.
3. `InteractiveSimulator` records into a `RunTrace`; `Trace` reads from it.
4. `TraceSelection` + the trace-window gesture on top.

Items 1–2 are the honest content of #538 and of CAP-24 PF-3. Item 3 is the
first real installment of #498 §7.2's "the recording is the contract" — a
payment this project owes several capstones, and #716 is where it comes due.

## Also worth flagging

The `hazard-demo` circuit named by #505, #538 and #716 does not exist in
the tree (`find . -iname '*hazard*'` is empty; `examples/` contains only
`autograde`). Three issues' acceptance criteria depend on a fixture no
issue owns. Under the reframing it matters less — the goldens ride on
`RunTrace` and can be seeded from the existing batch fixtures — but
somebody still has to draw the glitch that makes the quantum question real.
