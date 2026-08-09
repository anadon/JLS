# Issue #700: TASK-C534-3: a fired trigger opens the chronogram centred on its capture, and the capture leaves through the existing VCD path and no other
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The Outcome paragraph contains the single most valuable sentence in the whole
CAP-23 tree: *"an instrument capture and a graded artifact are the same bytes
produced by the same code."* That is not a routing preference. It is the claim
that JLS should have **one** notion of "what the simulation did", shared by the
grader, the waveform panel, and the instruments — and that everything else
(chronogram lanes, VCD files, analyzer captures) is a *view* of it.

The issue then states that claim as a rule to be enforced by prohibition
("a test asserts no second trace writer exists"). The rule is the shadow of a
missing type. Cut the seam where the type belongs and the acceptance criteria
stop needing to be policed — they become facts.

## The seam the issue does not name

The codebase already has the duplication #700 is trying to forbid, and it
predates the instruments:

- `jls.sim.TraceSample` — `record TraceSample(long time, BitSet value)`,
  headless, accumulated by `BatchSimulator.afterEvent` into `eventTrace` /
  `probeTrace`, rendered by `BatchSimulator.toVcd`.
- `jls.edit.Trace` — `private record Change(BitSet value, long when)`
  (`src/jls/edit/Trace.java:51`), accumulated by
  `InteractiveSimulator.afterEvent` into per-`Trace` lists, bounded by
  `MAX_RETAINED_CHANGES = 100_000` (#121), rendered to pixels.

Same record, same dedup discipline, two packages, two accumulators, zero shared
code. And `BatchSimulator extends Simulator`, `InteractiveSimulator extends
Simulator` — they are *siblings*, so the interactive side cannot reach `toVcd`
even if it wanted to. #700 asks the GUI to export through a method on the batch
simulator; there is no inheritance path, and `docs/grand-architecture.md` §5
puts `gui requires: core` while listing `vcd` under `batch/services`, so the
call edge #700 implies is one the recorded module graph forbids.

Meanwhile #678 is about to introduce an "event tap seam that costs nothing when
nobody is listening" — which is precisely what
`if (!JLSInfo.printTrace && vcdFileName == null) return;` in
`BatchSimulator.afterEvent` already is, unnamed and gated on a global static.
And #696 will add a pre-trigger ring buffer, and KC-23-2 anticipates a
cause-chain ring buffer. That is four bounded value-history buffers in one
subsystem, three of them written after the concept existed.

## The alternative design

Extract, in `core`, the object all four are approximations of:

1. **`jls.sim.Recording`** — signal id → bit width + deduplicated
   `List<TraceSample>`, plus a declared time range and a retention policy
   (which absorbs #121's `MAX_RETAINED_CHANGES`). One method carries the whole
   instrument story: `Recording window(long from, long to)`, returning a
   Recording whose baseline is the value *in effect at* `from`.
2. **`jls.sim.VcdWriter.render(Recording, Profile)`** — the only VCD emitter in
   the tree. `BatchSimulator.toVcd` becomes a delegation; the migration's proof
   is that `VcdExportGoldenTest` and `VcdProbeExportTest` pass byte-for-byte
   *unchanged*.
3. **`Simulator` owns a nullable `TraceRecorder`**, not `BatchSimulator`. That
   *is* #678's tap, and it ships with an existing proven consumer (batch VCD)
   instead of a speculative one. It also retires the `JLSInfo.printTrace`
   global in favour of per-simulator state, which is #93/#94 territory.
4. **`jls.edit.Trace` renders a `Recording` window** instead of owning
   `Change`; the chronogram (#680) reads the same object rather than minting a
   third representation.
5. **An analyzer capture is `recording.window(fire - pre, fire + post)`.**

Then #700's three substantive criteria are not tested, they are typed. AC-1 is
`chronogram.show(window)`. AC-2 is a ratchet in this project's established idiom
(`HeadlessCoreRatchetTest`, `NotificationRatchetTest`): *no class outside
`VcdWriter` emits `$var` or `$dumpvars`*, and *`jls.edit` declares no value-history
record*. AC-3 is true by construction, because interactive and batch feed one
recorder and render through one writer.

**I am explicitly disregarding AC-2 as written.** "No second trace writer exists
*for instrument captures*" is scoped so narrowly that today's genuine
duplication — `Trace.Change` versus `TraceSample` — passes it. The test would
certify the invariant the issue does not care about while the one it does care
about stays broken.

## Two frozen contracts #700 walks into

These are symptoms of the missing seam, not separate oversights.

- `docs/batch-interface.md` §4 names `BatchSimulator.toVcd` as *the* emitter and
  freezes the literal header `$comment JLS batch simulation trace $end`; §6
  makes the profile a stability promise. An interactive export either emits the
  word "batch" (a lie, and a doc change) or breaks AC-3's byte-identity. Under
  the reframing the comment line is a `Profile` parameter, §4 is amended once to
  say "emitter: `VcdWriter`", and the capture profile is specified beside the
  batch profile rather than smuggled in.
- §4 also pins `#0`/`$dumpvars` as the baseline. A capture window starts at a
  non-zero tick. Rebasing it to 0 destroys the correlation with the graded run —
  which is the entire point of the Outcome sentence. Emitting `$dumpvars` at
  `#<window-start>` keeps absolute time and is legal IEEE 1364, but it is a
  profile *addition* and needs §6's ritual. `Recording.window` is where that
  decision lives, once, for every consumer.

There is also an unowned consequence in AC-3: for a batch run to produce *the
same capture*, batch must arm the analyzer and window its VCD — a `-vcd` surface
change touching the §1 flag table pinned by `CliFlagTableTest`. Neither #700 nor
#696 owns it.

## Sequencing

#700 is banded at 1 mw on the assumption that the plumbing exists. It does not,
and #696, #698 and #680 each silently assume someone else built it. File the
extraction (Recording + VcdWriter into core, batch as first consumer) as a
prerequisite ordered before TASK-C534-1 and TASK-C527-2 — call it 1.5–2 mw — and
#700 genuinely becomes 1 mw: window, centre, export. Net across FEAT-527 and
FEAT-534 this is cheaper, because it deletes three capture buffers that would
otherwise be written independently.

## Where the issue is right and should not be softened

The refusal of a second trace format (VCD only, FST out per CAP-23 OQ-5), the
insistence that instruments work headlessly, and AC-4's demand that an unarmed
circuit be indistinguishable from today are all correct and load-bearing. The
reframing strengthens AC-4 rather than threatening it: with one recorder, "is
anyone listening?" is one null check in one place, instead of the two
uncoordinated gates (`printTrace || vcdFileName != null`, and the interactive
`traceMap`) that exist now.
