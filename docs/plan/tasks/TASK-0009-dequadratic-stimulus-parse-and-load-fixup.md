# TASK-0009 - De-quadratic the stimulus parse and the load fixup

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

Three measured quadratics are removed, each pinned by a large-input benchmark
fixture that fails on the old code within a stated time budget.

**1. The stimulus parse.** `SigSim.initSim`
(`src/jls/elem/SigSim.java:39-95`) builds its normalized input by string
concatenation:

- `newLine += " " + value` / `newLine += " " + token`
  (`src/jls/elem/SigSim.java:64, 67`) - quadratic in the tokens of one line;
- `newSignals += newLine... + " "` (`:71, 74`) - quadratic in the whole input,
  because every line reallocates and recopies the accumulated string.

Both become a single `StringBuilder`. The measured consequence is not marginal:
JFR attributes the largest allocator of a whole simulation run to `byte[]` from
this concatenation (BRIEF.md §13, "the same defect already recorded at 95%").

**2. The stimulus pin lookup.** Immediately below, `:83-90` resolves each signal
name by scanning **every element in the circuit** looking for an `InputPin`
with a matching name - O(signals x elements), and it does not stop on a hit
(`pin = ip;` continues the loop). Build one `Map<String,InputPin>` before the
loop and look up in it; keep last-wins semantics if the scan's fall-through
behavior is load-bearing, and say so.

**3. The load-time net partition.** `Circuit.finishLoad`
(`src/jls/Circuit.java:1345-1392`): `ends` is a
`LinkedList<WireEnd>` (`:1345`) and the BFS calls `ends.remove(vend)`
(`:1369`) for **every** end it visits. `LinkedList.remove(Object)` is a linear
scan, so the partition is **O(E^2)** in the number of wire ends - on a circuit
whose wire ends dominate the element count, which is the normal case. Replace
the worklist with a set-membership structure that preserves iteration order
(`LinkedHashSet`, or an explicit visited flag over an
`ArrayList`); `WireEnd` already carries a `marked` field used for exactly this
purpose (`src/jls/elem/WireEnd.java:33-34`).

Each fix ships with a fixture under `test/resources/perf/` sized so the old
code exceeds a stated wall-clock budget and the new code does not.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-005 | These are two of the three paths the feature names (the third, the materializing waveform dump, is TASK-0010). |

## Prerequisite tasks

None. Independent of TASK-0007: if the extraction lands first this fixes the
walk in `jls.netlist`; if this lands first the extraction carries the fixed
code. Neither reads data the other creates.

## Acceptance test

`test/jls/perf/StimulusParseScalingTest.java`, new:

- `stimulusParseIsLinearInInputSize()` - parses a generated stimulus file at
  size N and at 4N, asserting the 4N time is under a stated multiple of the N
  time (not a bare wall-clock threshold, so the test survives a slower CI
  machine). Must fail at HEAD, where 4x the input is ~16x the copying.
- `signalResolutionDoesNotScaleWithCircuitSize()` - the same stimulus against a
  small circuit and a 10x larger one, asserting parse time is flat.

`test/jls/perf/LoadPartitionScalingTest.java`, new:
`netPartitionIsSubQuadraticInWireEndCount()` - loads a generated circuit with E
and 4E wire ends across many small nets and asserts the same ratio bound. The
`p1000.jls`/`p10000.jls`/`p100000.jls` shape used during the measurement study
is the fixture generator's model.

`test/jls/BatchSimulationGoldenTest` and
`test/jls/ElementSimulationGoldenTest` must stay green **unregenerated** - the
proof that these are complexity fixes and not behavior changes.

## Related GitHub issues

**No issue.** The quadratic and materializing paths have no tracker entry
(registry TABLE 4, "Plan items with NO issue"; FEAT-005, TASK-0009,
TASK-0010).

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal java.util.BitSet allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps - #232 covers the in-loop `BitSet` churn, a different allocator. The `byte[]` from this concatenation is the larger one and #232 does not name it |

## Notes

- **The two SigSim concatenations are one pass and must both go.** Fixing only
  `newSignals` leaves `newLine` quadratic per line, which for a wide test
  vector is the same defect at a smaller scale.
- **`initSim` runs before the warm event loop**, so this cost sits in the
  including-`initSimulation` figure, not the 3.14 M events/s warm-loop one
  (BRIEF.md §13). State which figure any measurement here belongs to; the
  corpus records sibling agents getting this wrong.
- **The partition's determinism is a hard constraint.** `loadedElements` is a
  `LinkedHashSet` precisely so wire-net construction and multi-driver
  resolution are deterministic (`src/jls/Circuit.java:76-79`, #98). A
  replacement worklist that iterates in hash order silently breaks
  byte-identical saves and the collaboration state hash. `LinkedHashSet` is
  safe; `HashSet` is not.
- **`WireEnd.marked` may be stale.** The field exists for partitioning
  (`src/jls/elem/WireEnd.java:33-34`) but is not reset in `finishLoad`; if the
  fix uses it, reset it explicitly at entry or a second load in the same JVM
  partitions wrongly.
- **`WireEnd.init` has a smaller quadratic and a latent bug** at
  `src/jls/elem/WireEnd.java:102-155`: the `for (Wire wire : wires)` dedup scan
  at `:138` is O(d^2) in one end's degree, and the whole `loadPut` attachment
  block at `:104-130` sits **inside** the loop over `loadWires`, so an end that
  declares a put but no wires never attaches at all. Fix both here; the second
  is a correctness defect, so it carries its own regression fixture.
- **`Circuit.lineNumber` is `private static`** (`src/jls/Circuit.java:89`);
  a scaling test that loads repeatedly in one JVM must not depend on it.
- **Do not fold in `Memory.react`'s self-event posting** (`Memory.java`
  ~`:1379-1403`, the measured 18.00 events/cycle mirrored-regfile cost). That
  is engine work under FEAT-030, not a load-path quadratic.

## Evidence

- `src/jls/elem/SigSim.java:43` (`String newSignals = ""`), `:64, 67`
  (`newLine +=`), `:71, 74` (`newSignals +=`), `:83-90` (the per-signal full
  element scan).
- `src/jls/Circuit.java:1345` (`LinkedList<WireEnd> ends`), `:1355` (the
  worklist loop), `:1369` (`ends.remove(vend)`, the linear scan per visited
  end).
- `src/jls/Circuit.java:76-79` - the determinism constraint on this exact pass.
- `src/jls/elem/WireEnd.java:33-34` (`marked`), `:102-155` (`init`: the
  misplaced attach block and the O(d^2) dedup scan).
- BRIEF.md §13 CORRECTIONS: "The largest allocator of the whole run is `byte[]`
  from `SigSim`'s quadratic string concatenation - the same defect already
  recorded at 95%".
