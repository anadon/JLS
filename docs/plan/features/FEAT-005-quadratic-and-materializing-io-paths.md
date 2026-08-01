# FEAT-005 - Quadratic and materializing I/O paths eliminated

**Status:** proposed | **Cost:** 2-3 mw | **Owner program:** P1 |
**Spine rank:** -

## Capability delivered

The three paths that make a long run or a large circuit cost superlinear time
and unbounded memory become linear and streaming: the batch stimulus parse stops
concatenating strings per line, the load-time wire-end fixup stops being
quadratic in wire ends, and the waveform dump stops being built as one string in
the heap before it is written. These are ordinary defects with measured costs,
not a program: at HEAD the stimulus parse alone is 80% of end-to-end wall time
and 95% of all allocation on a 50,000-cycle run. Fixing them is the difference
between a multi-hour run being merely slow and being impossible.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | Two of the three are named defects at HEAD with measured costs and no issue |
| CAP-02 | required | A boot run is millions of cycles of stimulus and a waveform nobody can hold in memory |
| CAP-03 | required | Same shape at ternary scale; the dump is larger per cycle because the manifest carries radix |
| CAP-06 | required | Grading hundreds of submissions multiplies the parse cost by the submission count |
| CAP-09 | required | Verification means running the same design many times; a quadratic parse taxes every one |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | None. All three sites are self-contained defect fixes against code that exists at HEAD |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0009 | De-quadratic the stimulus parse and the load fixup | The two quadratic sites, each pinned by a large-input benchmark fixture |
| TASK-0010 | Stream the waveform dump instead of materializing it | The dump writes incrementally to a sink; the whole-dump-as-one-string path is removed |
| TASK-0008 | Key net and probe names off stable id, and validate them | Shared with FEAT-004: the streaming dump must emit variable declarations that the waveform checker accepts, which requires the naming function to be settled first |

## Acceptance criteria

1. The stimulus parse and the wire-end fixup are benchmarked at 1x and 10x input
   on committed fixtures and scale within a stated linear tolerance. The
   tolerance is a number in the test, not a judgment.
2. The waveform dump writes incrementally to a sink. The code path that returns
   the whole dump as one string is **removed**, not merely bypassed - a
   remaining materializing path is the one that gets called next year.
3. Every existing waveform golden is byte-identical after the change. Streaming
   is an allocation change, not a format change.
4. Allocation per simulated cycle on a committed fixture is asserted against a
   declared band, and the band is set from the post-fix measurement, entering
   the ratchet FEAT-009 owns.
5. A 50,000-cycle batch run completes within a stated wall-clock and heap budget
   on the calibration fixture, where today it allocates tens of gigabytes.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps - #232 is the *in-loop* allocation story owned by FEAT-026 and FEAT-030; the largest allocator of the whole run is not in the loop at all, it is this feature's stimulus parse. The two must not be conflated when reading a profile |
| - | the quadratic parse, the quadratic fixup and the materializing dump | **no issue** |

## Design notes

**Kept separate from FEAT-030 deliberately** (registry deduplication record item
16). FEAT-030 is a 12-20 week gated engine program; this is two to three weeks
of defect work that lands immediately under decision D6. Merging them would gate
a two-week fix behind a five-month program.

The three sites, exactly:

- **Stimulus parse.** `src/jls/elem/SigSim.java:64`, `:67`, `:71`, `:74` build
  `newLine` and `newSignals` by repeated `+=` inside the per-line loop. That is
  the quadratic path and it is the source of the largest `byte[]` allocation in
  the entire run - 39.58 GB of 41.76 GB measured on a 50,000-cycle run.
- **Load fixup.** `Circuit.finishLoad` (`src/jls/Circuit.java:1300-1422`) is
  O(W^2) in wire ends; 80,000 wire ends measured at 46 s. Note the ordering
  constraint recorded at `src/jls/Circuit.java:76` - wire-net construction
  depends on insertion (file) order, so the linear rewrite must preserve that
  order exactly or it changes net identity.
- **Materializing dump.** `src/jls/sim/BatchSimulator.java:475` and `:518`
  return accumulated output via `toString()`.

One measurement caution the task author must carry: a JFR profile of a batch run
attributes about 50% of *in-loop* allocation among named non-`byte[]` classes to
value churn, and separately attributes the largest total allocation to `byte[]`
from this feature's string concatenation. Those are the same run seen at two
scopes (`BRIEF.md` §13). Any claim of the form "X% of allocation" must state its
scope or it is not a measurement.

Decision D6 applies: this lands immediately and is not sequenced behind the core
extraction.

## Risks

- **The wire-end fixup rewrite can change net identity.** Insertion order is
  load-bearing at `src/jls/Circuit.java:76`. The linear implementation must
  produce the identical partition, and the witness is the existing golden
  corpus, byte-identical - the same gate FEAT-030 operates under.
- **Streaming the dump changes flush timing, not content.** A test that reads
  the dump while the run is in progress will see different intermediate states.
  No such test exists at HEAD; criterion 3 keeps it that way.
- **"Ordinary defect" is a claim about tractability, not about risk.** The
  stimulus parse is on the path of every batch invocation including every
  golden. Criterion 3 is what makes the change safe to land quickly.

## Evidence

- Quadratic stimulus parse: `src/jls/elem/SigSim.java:64,67,71,74` (`+=` inside
  the per-line loop). Measured 80% of end-to-end wall time and 95% of all
  allocation, 39.58 GB of 41.76 GB on a 50,000-cycle run: `BRIEF.md` §7.
- Quadratic load fixup: `src/jls/Circuit.java:1300-1422` (`finishLoad`);
  80,000 wire ends measured at 46 s (`BRIEF.md` §7). Order constraint:
  `src/jls/Circuit.java:76`.
- Materializing dump: `src/jls/sim/BatchSimulator.java:475`, `:518`.
- Allocation-scope caution: `BRIEF.md` §13.
- Separation from FEAT-030: registry deduplication record item 16.
- Sequencing: decision D6, `BRIEF.md` §12.
- Normative batch and VCD surface (not restated here):
  `docs/batch-interface.md`, `docs/vcd-interop.md`.
