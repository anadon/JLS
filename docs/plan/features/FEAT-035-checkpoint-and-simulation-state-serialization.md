# FEAT-035 - Checkpoint and simulation-state serialization

**Status:** proposed | **Cost:** 10-17 mw | **Owner program:** P9 |
**Spine rank:** S25

## Capability delivered

A running simulation can be written to disk and picked up again exactly where it
was - the same simulation time, the same pending events in the same order, the
same memory and register contents, the same clock phase. Runs stop being
all-or-nothing: a multi-hour structural run survives a laptop closing, a grading
batch survives a machine going away, a long boot can be snapshotted once and
then re-entered many times to study the part that matters, and a handover
between two people or two machines is a file copy.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | a 1.66-1.72 h run that cannot be resumed is a run that cannot survive an interruption |
| CAP-03 | required | makes a long structural run resumable and a boundary handover free |
| CAP-06 | beneficial | a 300-submission grading run must survive a machine going away mid-batch |
| CAP-09 | beneficial | a multi-hour verification run must survive a handover |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-013 | The checkpoint is bulk binary state, and the save format's item grammar has no kind that can carry it. Per-section versioning with must-understand semantics is the mechanism that lets a checkpoint section ride along and lets an old reader refuse it honestly |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0074 | Serialize the queue, the clock and stateful element contents | The core of it: the pending events, simulation time, and memory and register contents written back rather than rebuilt from init text |
| TASK-0075 | Checkpoint round-trip equivalence test | The only acceptance evidence that matters |
| TASK-0014 | Long-lived batch mode with pause, heartbeat and clean interrupt | Shared with FEAT-006: there is nothing to checkpoint from until batch can stop without stopping |
| TASK-0066 | The boundary handover harness | Shared with FEAT-031: mapping one boundary's state at a declared instant is the small case of the same state-mapping problem |

## Acceptance criteria

1. Resuming from a checkpoint produces the **byte-identical continuation** of an
   uninterrupted run: same trace, same waveform dump, same stdout, from the
   checkpoint instant to the end.
2. The event queue is serialized with its total order intact, including the
   duplicate-check state, so a resumed run posts and coalesces identically.
3. `Memory` and `Register` write back their **running** contents, not their
   authored initial text, and restore them without re-running initialization.
4. Simulation time and clock phase are restored, so a resumed run's timestamps
   continue rather than restart.
5. What cannot be checkpointed is **named and refused loudly** - not silently
   skipped. A run holding an open host byte port, or containing an element kind
   with no declared state mapping, refuses to checkpoint with the reason.
6. A checkpoint declares the format version and the JLS version that wrote it,
   and a reader that cannot honor it refuses rather than producing a subtly
   different continuation.
7. Checkpoint and resume are available from batch, not only from the GUI.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | simulation-state serialization, the checkpoint section and its round-trip gate | **no issue** - this is the largest untracked gap in the core column |

## Design notes

The gap is total, not partial, and that is the first thing to internalize.
`Circuit.save` (`src/jls/Circuit.java:1466`) takes a `PrintWriter` and no
simulator: there is no time, no queue, no state argument anywhere in the
signature. `Register` saves `initialValue` and the loader resets the displayed
current value; the running fields are in a separate simulation block and are
never touched by save. `Memory` writes `init` or `initrle` derived from the
initial-value *text field*, while the running store and the initial image are
null before `initSim` and are rebuilt from that text on every start. So there is
no partial state serialization to extend - the feature builds the whole path.

The format cannot carry it today either. `docs/file-format.md:122-131`
enumerates the complete item grammar - `int`, `long`, `bigint`, `String`, `ref`,
`pair`, `probe`, circuit block - with no binary or blob kind, and adding one is
a format version bump. FEAT-013's must-understand sections are how a checkpoint
rides along without every reader needing to understand it, which is why that
dependency is required rather than convenient.

The prior art is worth following rather than re-deriving. gem5's checkpoint is a
directory with an INI-style text file plus binary memory images;
`unserialize()` runs per object; objects with no checkpoint section restore to
their constructed state; microarchitectural state is deliberately *not*
checkpointed, so a restored run starts cold; and checkpoints are explicitly not
portable across versions, with an upgrade utility to match. The two rules JLS
should take verbatim are: name what you refuse to checkpoint, and version the
checkpoint independently of the circuit.

There is a precondition that is easy to miss: batch mode cannot currently stop
without stopping. `BatchSimulator.pause(boolean)`
(`src/jls/sim/BatchSimulator.java:87-90`) sets `stopping = true` - it is
literally identical to `stop()`, and the javadoc says so. TASK-0014 is listed as
a prerequisite because a checkpoint needs a moment that is neither running nor
over.

## Risks

- **Round-trip equivalence is the whole feature.** A checkpoint that resumes to
  a *nearly* identical continuation is worse than none, because it silently
  invalidates every parity claim built on top of it. TASK-0075 is not a
  verification step at the end; it is the definition of done and should be
  written before TASK-0074.
- **The refusal list will be unpopular.** Host I/O state, in-flight external
  interactions and any element whose state mapping is undeclared all have to
  refuse. gem5 panics rather than checkpoint classic caches; that is the right
  instinct and the demo cost has to be accepted.
- **Version skew.** A checkpoint is a snapshot of an implementation, not of a
  design. Users will expect a checkpoint from last year's JLS to open in this
  year's. Say no in writing, in the file header, at the start.
- **Cost band.** 10-17 mw covers the queue, the stateful elements, the section
  and the round-trip gate. It does not cover checkpointing a design that spans a
  fidelity boundary handover mid-run, which is FEAT-031's harness and is scoped
  there.

## Evidence

- No simulation-state serialization at all: `src/jls/Circuit.java:1466`
  (`save(PrintWriter)`, no simulator argument); `src/jls/elem/Register.java:311-328`
  saves `initialValue` and, on load, resets the displayed current value;
  `src/jls/elem/Memory.java:110` is the initial-value *text* field that
  `save` (`:436`) derives `init`/`initrle` from, while the running store and the
  initial image are `@Nullable` and null before `initSim` (`:981-987`,
  `initSim` at `:1245`).
- The format has no carrier: `docs/file-format.md:122-131` (item grammar), §4
  (`:159-165`, the FORMAT header and version negotiation).
- Batch cannot pause: `src/jls/sim/BatchSimulator.java:82-90`, whose own javadoc
  says "It doesn't make sense to pause it in batch mode".
- Prior art and the rules taken from it: `recon-checkpoint-restore.md` §5
  (gem5's `m5.cpt` layout, per-object `unserialize`, no cross-version
  portability, the boot-then-restore-in-detail workflow).
- Gap severity: `BRIEF.md` §7 grades this "fatal" and it is the first row of the
  gap list.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S25 (10-17 wk,
  lowest score in the spine - required by two capstones and by nothing else).
