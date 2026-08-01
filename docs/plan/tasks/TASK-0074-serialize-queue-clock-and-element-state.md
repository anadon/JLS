# TASK-0074 - Serialize the queue, the clock and stateful element contents

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0011, TASK-0033

## Deliverable

The running simulation becomes a saveable object: the queue, the clock, the
dedup set and every element's contents, written back rather than rebuilt from
init text.

1. **A checkpoint is an optional, internally versioned section** in TASK-0033's
   framing - never a new file kind and never a second grammar. Its loader runs
   **before** `Circuit.finishLoad`, so nothing in `src/jls/sim/` changes to read
   one.

2. **Engine state.** `Simulator.eventQueue`
   (`src/jls/sim/Simulator.java:25`), `dupCheck` (`:27`), `now` (`:36`),
   `maxTime` (`:38`), `stopping` (`:44`) - and **`SimEvent.sequence`, which is a
   `static`** (`src/jls/sim/SimEvent.java:87`) and must become a per-`Simulator`
   field in this task. A resumed run whose sequence base differs from the
   uninterrupted one breaks same-time tie-ordering, which is the one property
   `SimEvent.compareTo` exists to guarantee.

3. **The event codec.** Each `SimEvent` serializes as
   `(time, seq, callback stable id, put selector, Payload)`. `SimEvent.Payload`
   is a sealed interface over seven records (`src/jls/sim/SimEvent.java:22-79`);
   the codec switches over it **with no `default` arm**, so adding an eighth
   payload kind is a compile error at the codec exactly as it already is at all
   27 `react` implementations.

4. **Net state.** `WireNet.value` and `conflictReported`
   (`src/jls/elem/WireNet.java:405,407`). `Input.getValue`/`Output.getValue`
   delegate to the net (`src/jls/elem/Output.java:108-124`), so nets are the
   only place values live and puts need no separate capture.

5. **Element state, written back rather than rebuilt.** The 30 `initSim`
   implementations under `src/jls/elem/` are the work list. The ones with real
   state:
   - `Memory`: `mem`, `initMem`, `currentValue`, `lastClock`, and the bounded
     `activity` list (`src/jls/elem/Memory.java:982-1024`).
   - `Register`: `currentValue`, `toBeValue`, `currentC`
     (`src/jls/elem/Register.java:693-698`).
   - `RegisterFile`: `words`, `currentC` (`src/jls/elem/RegisterFile.java:448-451`)
     - which today has **no `init`, no `file` and no write-back at all**;
     `initSim` zeroes every word (`:460-472`). Content initialization for it is
     part of this task.
   - `StateMachine`: current state, `busy`, `noMatchReported`
     (`src/jls/elem/StateMachine.java:659,664`).
   - `Clock`, `SigGen`, `TestGen`: phase and input cursor.

6. **An input-log cursor and a retirement index alongside `now`**, because
   simulated time is a permitted divergence (`docs/parity-contract.md` §4) and
   the same input log must replay into both tiers from the same point.

7. **A `pinned` flag exempt from any retention policy** - or the boot snapshot
   is evicted by the policy that exists to protect it.

8. **Bulk contents ride TASK-0034's raw binary section**, not hex text. A 16 MiB
   32-bit memory image as save text is ~66.6 MB against a 64 MiB cap
   (`src/jls/FileAbstractor.java:65`; `docs/machine-calibration.md` §5.2).

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-035 | The state. Resumption, handover and bisection all read exactly what this task writes. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0011 | Adjudicate and fix the past-limit event drop | A checkpoint taken at or after termination serializes a queue that is *missing a dropped event*. What the codec writes is undefined until the drop is adjudicated - this reads a decision only that task records. |
| TASK-0033 | Section framing, must-understand flags and the epoch policy | The checkpoint *is* an optional internally versioned section. It rides framing only that task creates; without it there is nowhere to put it that an old reader can skip. |

## Acceptance test

`test/jls/sim/CheckpointStateTest`:

- `payloadCodecIsTotalOverTheSealedInterface()` - reflection over
  `SimEvent.Payload.class.getPermittedSubclasses()` asserting the codec handles
  every one and that a synthetic unknown throws rather than silently encoding as
  a `PinChanged`.
- `sequenceIsPerSimulatorNotStatic()` - two `Simulator` instances in one JVM
  produce independent `seq` streams. **Fails at HEAD**:
  `SimEvent.sequence` is a class-level `static` (`src/jls/sim/SimEvent.java:87`).
- `everySimulationFieldIsCapturedOrDeclaredDerived()` - a reflective sweep over
  every class with an `initSim` override, asserting each non-final instance
  field assigned in `initSim` or `react` is either in the codec's field map or on
  a declared-derived list carrying a reason. This is the **map**, not the gate;
  TASK-0075's property test is the gate, because a hand-reviewed field list
  decays every time an element ships.
- `dupCheckAndQueueAgreeAfterRestore()` - asserts every restored queue member is
  in the restored dedup set and vice versa.
- `registerFileContentsSurviveARoundTrip()` - the element with no write-back
  today.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | checkpoint and simulation-state serialization | **no issue** |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps - if the signal representation changes, the codec's value encoding changes with it. Write the codec against `BitSet` today and against a documented encoding, not against the field type |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - a multi-hour worked example is what makes resumption worth having |

## Notes

- **`ObjectInputStream`/`ObjectOutputStream` are banned repo-wide**, zero
  tolerance, by `ArchitectureRulesTest#nothingUsesJavaObjectSerializationStreams`
  (`test/jls/ArchitectureRulesTest.java:201-212`). The codec is the textual
  save-format grammar with typed rejection, like everything else.
- **`SimEvent.equals`/`hashCode` deliberately exclude `seq` and compare the
  callback by reference identity** (`src/jls/sim/SimEvent.java:88-95`;
  `jls.sim.SimEventDedupTest`). A restore that recreates callbacks as new
  objects therefore changes what `dupCheck` coalesces. Restore against the
  *existing* element instances, resolved by stable id.
- **`Memory.initSim` re-reads `fileName` from the host filesystem**
  (`src/jls/elem/Memory.java:1250-1288`). A resumed run must not re-read it: a
  file edited between the checkpoint and the resume would silently change state
  that the checkpoint claims to have captured. Resume takes the serialized
  image; `initSim`'s file path is start-of-run only.
- **`Memory.activity` is bounded at `ACTIVITY_LIMIT = 10_000`, newest first**
  (`:1021-1024`). Serializing it is optional, but both the bound and the order
  must survive or `getActivityTrace` output shifts and `MemTrace` shows a
  different history after a resume than before.
- **`RegisterFile` is the only simulating element with no behavioral golden** -
  `ElementSimulationGoldenTest` lists it `EXEMPT` with a recorded reason
  (`test/jls/ElementSimulationGoldenTest.java:533-546`). Writing its checkpoint
  path without one is writing untested code; land the golden first.
- **`.jls~` autosave "checkpoints" are a different mechanism with the same
  word** (`ARCHITECTURE.md:108-113`;
  `SimpleEditor.writeCheckpointInBackground`). Pick a distinct term in code and
  docs, or every future grep is ambiguous.
- **Mixed-fidelity checkpoints are open and undesigned by anyone**: what a
  checkpoint contains when one subcircuit runs on the interpreter at event
  granularity and another compiled at timestamp granularity. Do not solve it
  here; state the restriction (single-fidelity only) in the section's version
  and let TASK-0066's boundary harness widen it.

## Evidence

- `src/jls/sim/Simulator.java:25,27,36,38,44` - the engine fields; `:177-201` -
  `initSimulation`, which is what a resume must *not* re-run; `:215-243` - the
  loop and the `now > maxTime` break at `:231-234`.
- `src/jls/sim/SimEvent.java:22-79` - the sealed `Payload` and its seven
  records; `:87` - the static sequence counter; `:88-95` - the equals/hashCode
  comment.
- `src/jls/elem/WireNet.java:405,407` - net value and conflict flag.
- `src/jls/elem/Memory.java:982-1024` (running state and the bounded activity
  list), `:1245-1321` (`initSim`, including the file read at `:1250-1288` and
  `mem = initMem.copy()` at `:1309`).
- `src/jls/elem/Register.java:693-698,719-737`; `src/jls/elem/RegisterFile.java:448-472`;
  `src/jls/elem/StateMachine.java:659,664`; `src/jls/elem/Clock.java:384-394`.
- `test/jls/ArchitectureRulesTest.java:201-212` - the serialization ban.
- `test/jls/ElementSimulationGoldenTest.java:533-546` - the `RegisterFile`
  exemption and its recorded reason.
- `docs/virtual-hardware-parity.md` P15 - the section shape, the loader
  placement, the `RegisterFile` content-initialization gap, the input-log cursor
  and retirement index, the `pinned` flag, and the dependency on the `maxTime`
  adjudication.
