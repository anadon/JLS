# Issue #535: FEAT-C23-6: dragging the time cursor rewinds the canvas by deterministic re-simulation to T — no checkpoints, honest replay progress — and a cursor-synced external viewer follows for the professional handoff
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two unrelated wishes are bundled under one feature id.

1. **The canvas should answer "what did this circuit look like when the glitch
   happened?"** That is the fourth observation of CAP-23's walk-through (#504 §1.4)
   and it is the one that makes the chronogram a diagnosis surface rather than a
   picture.
2. **The professional handoff** — a live cursor-synced external waveform viewer.

Wish 1 is stated as an *implementation* ("deterministic re-simulation truncated at
T"), and the whole issue title, cost band and kill criterion are organised around
defending that implementation against the checkpoint machinery of FEAT-035 (#363)
and #498 §8 exclusion 2. The defence is sound and unnecessary: **there is a third
option neither the issue nor CAP-23 considers, and JLS already ships most of it.**

## The reframing: the canvas at T is a display projection, not a re-execution

`src/jls/edit/Trace.java` already keeps, per traced signal, a bounded newest-first
list of `Change(BitSet value, long when)` (`MAX_RETAINED_CHANGES = 100_000`,
~14 MB/trace, issue #121), and already answers "what was this signal at time T?"
by binary search — `Trace.firstChangeAtOrBefore(long)` at
`src/jls/edit/Trace.java:445-472`, pinned by
`jls.sim.TraceWindowingTest#firstChangeAtOrBeforeMatchesTheLinearScan()`. It
already has a time cursor: `Trace.sliderPos`, driven from
`InteractiveSimulator.Traces.mouseMoved` (`src/jls/edit/InteractiveSimulator.java:1375-1383`),
gated on `paused || stopping`, and line 408 of `Trace.java` already renders the
value in effect at the slider's time.

So the shipped tool already displays historical values at an arbitrary earlier T —
in the trace window. The gap CAP-23 step 4 names is that those values never reach
the canvas. That gap is one direction of the same seam PF-2 (cross-probing) is
already funded to build: PF-2 links a waveform edge to an *element identity*;
rewind links the cursor time to *element values*. Same lookup, same data, one more
consumer.

Concretely, the alternative design:

- The chronogram (PF-1) owns a `Recording`: per displayed signal, the transition
  list it must keep anyway to draw waveforms — a generalisation of `Trace`'s
  `changes` list, not a new subsystem.
- Dragging the cursor to T puts the canvas in **historical mode**: every element
  and net paints the value `Recording.at(T)` returns, visually marked as history,
  not live. Cost is O(log n) per displayed signal, not O(T).
- Where an element's on-canvas state is not a signal (Memory contents, a state
  machine's current state), the recording keeps its *write stream* — the same data
  `MemTrace` already collects — and reconstructs by folding writes up to T. Still
  proportional to activity in that one element, not to the whole run.
- **Replay to T survives as the fallback, not the mechanism**: when the cursor goes
  behind the retention horizon (the existing 100k-change cap), fall back to
  re-simulation with the progress UI this issue describes. That path then costs
  little and is exercised rarely, which is the right place for a progress bar.

This keeps every property the issue is protecting — no per-element checkpoint code
path, exclusion 2 untouched, FEAT-035 still not a dependency — and drops the
property nobody should want: O(T).

## Why O(T) is worse than the issue admits

"Honest and acceptable at teaching scale" is true of an adder and false of the
project's own flagship. #508 funds CAP-02's behavioural slice — "a drawn CPU boots
Linux" — as the prominence flare, and #498's kill criteria price a structural boot
in hours. On any circuit on the `riscv/` trajectory (`riscv/`, `riscv/gui/cpu.jls`,
`test/jls/RiscvCpuGoldenTest`), dragging the cursor one screen to the left re-runs
the boot. A rewind mechanism whose cost is the age of the interesting moment is
precisely backwards: the older the moment, the more you want to look at it. The
recording projection is O(log n) regardless, and degrades by *forgetting* old
history rather than by charging for it — which is the correct failure mode for a
diagnosis tool with a bounded memory budget.

Second: re-simulating from 0 under the interactive engine re-emits every trace
sample into the very `Trace` objects the chronogram is drawing, and re-runs
`initSim` on every element, so the "rewind" path has to suppress, reset and restore
the display it is serving. The projection path touches nothing but the painter.

## Determinism: the KC-23-3 risk is smaller than filed, and it argues for recording anyway

I checked the premise. `InteractiveSimulator.runSim` calls `ed.enableEditor(false)`
for the duration of the run (`src/jls/edit/InteractiveSimulator.java:636-638`), and
no element takes live user input mid-run — stimulus comes from `TestGen`/`SigGen`
and `Clock`. Element seeding is already stable-id ordered and documented as making
"every simulated value a pure function of circuit content"
(`src/jls/sim/Simulator.java:189-200`). So an interactive run *is* reproducible
from (circuit, test file, time limit) today, with one landmine the issue does not
name: `SimEvent`'s `static` sequence counter (#363 evidence 7), which makes
same-time tie-order depend on process history the moment two simulators exist in
one JVM. Replay-to-T inherits that bug; a recording does not.

## The viewer-sync half: drop it, and get the handoff a better way

The issue files scope that its own cited product review (#508 §6) recommends
cutting, marks it "the first candidate to descope", and then asks CI to run a
scripted session against a pinned external binary (AC-3). That is a network- and
toolchain-dependent lane in a project whose identity is the offline self-contained
jar, hermetic reproducible builds, and optional external tools that *skip cleanly*
when absent (README's iverilog/GHDL discipline). Zero external adoption (#508) and
a young protocol on the far side make it the least defensible mw in the capstone.

The better route to the same end already has an issue: **the recording is the
handoff.** #405 (TASK-0010, streaming dump) plus #498 §7.2's proposed amendment —
*"An interactive session is a recording device; the recording, not the session, is
the contract"* — and its M2 milestone ("a GUI session records and replays in batch
byte-identically, which finally joins JLS's two front ends") say that a session
should be continuously written as a VCD, and that replay of that artifact is the
CI-runnable thing. Then "open in external viewer" means: stream the VCD, launch
whatever the user has, and let it reload. That works with GTKWave *and* Surfer,
needs no pinned version, no protocol, no CI lane, and lands a strictly larger
benefit — a student's interactive exploration becomes a reproducible batch artifact
they can attach to a bug report or hand to an autograder, which is exactly the
grading-integrity wedge (#300/#306/#502) #508 funds first.

## Alignment: this pulls slightly against the arc as filed

- It **duplicates** a seam. Rewind-by-recording is PF-1's data plus PF-2's link;
  as filed it is a separate 3–4 mw feature with its own replay engine. Merged into
  the funded chronogram slice it is closer to 0.5–1 mw of incremental work.
- It **anticipates** #498 M2's session-recording work with a private, single-purpose
  replay path. Two mechanisms for "reproduce this run" is the same drift CAP-23 §3.1
  forbids for cause-chain models.
- It **adds tracker mass on purpose**: the body says it is filed for roster
  completeness against a review that recommends cutting half of it. #508's process
  findings say the tracker consumed a remediation cycle per planning cycle. A line
  on #504 recording "PF-6 sync half descoped per #508" costs nothing and carries the
  same information.

## What I would keep, and what I am explicitly disregarding

**Keep:** acceptance criterion 1 (`RewindEqualsReplayTest`) exactly as written. It
is stated as an *observable* — displayed state at the cursor equals a fresh run
truncated at T, byte-identically — so it is mechanism-independent and becomes the
fidelity oracle for the recording, forcing the recording to cover everything the
canvas paints. Keep criterion 4 (degradable, stands alone) and criterion 5 (sync
never feeds values back), which the file-based handoff satisfies trivially.

**Disregarding, deliberately:** criterion 2 ("rewind uses no per-element
snapshot/checkpoint code path; replay progress is displayed during long rewinds").
Its first clause is a mechanism prohibition aimed at exclusion 2 and is satisfied
by the projection too — a bounded transition log per *displayed signal* is not
per-element state serialisation, and it never touches the save format, which is
what exclusion 2 and #363 actually cost. Its second clause mandates a progress bar
for an operation that should be instant; I would demote it to the
behind-the-horizon fallback path. And criterion 3 (`ViewerSyncTest` against a
pinned viewer): cut, per #508, replaced by a streaming-recording handoff test that
runs offline.

**Suggested shape:** fold the rewind half into the funded CAP-23 chronogram slice as
"cursor drives canvas historical mode", file the streaming-session-recording handoff
against #405/#498 M2 rather than here, and close this issue with a REPLAN on #504
resolving PF-6 into those two.
