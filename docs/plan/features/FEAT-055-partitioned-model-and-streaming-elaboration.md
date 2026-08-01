# FEAT-055 - Partitioned model and streaming elaboration

**Status:** proposed | **Cost:** 10-16 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A circuit can exist as parts that load independently: a design is described by a
set of partition files plus a boundary description, each part elaborates without
the others being resident, and the boundary nets are named identically on both
sides of every cut. This removes the single-file ceiling as the limit on how
large a design can be and is the precondition for a design being simulated by
more than one process.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-17 | required | The circuit must exist as parts that load independently before anything can be distributed |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-054 | Partitioning a representation that costs an order of magnitude more per element than it needs to buys an order of magnitude less capacity per host |
| FEAT-004 | A partition boundary is a cut through nets. A net must have an identity that survives being cut and must name the same signal on both sides, which is exactly what the shared partition pass and stable net naming provide |
| FEAT-013 | A multi-part design is a section-framed artifact set; the must-understand policy is what stops an older reader from opening one part and believing it has the design |
| FEAT-005 | Superlinear load and whole-run materialization are already defects at current scale; at partition scale they are the ceiling |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was added with CAP-17 after it closed | - |

## Acceptance criteria

1. A design described as N parts loads with only one part resident at a time,
   measured rather than asserted.
2. A boundary net names the same signal on both sides of a cut, asserted by a
   test over a generated design.
3. Elaboration is streaming: peak resident memory during load is bounded by the
   largest single part plus the boundary description, not by the whole design.
4. A partitioned design and its single-file equivalent, where both fit, produce
   byte-identical simulation output.
5. A design whose cut crosses a construct that cannot be cut - a combinational
   cycle spanning partitions - is refused by name at partition time rather than
   simulated to a different answer.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | The partitioned model and streaming elaboration | **no issue** |

## Design notes

Criterion 5 is the falsification guard for the whole capacity capstone, and it
belongs here rather than downstream: refusal is a property of the partitioner,
not of the transport. Without it, the byte-identity assertions further down can
pass vacuously on designs that never exercise a boundary.

Whether the partitioner cuts automatically or the author declares the cuts is
CAP-17's open decision 3, and this feature is written for the author-declared
case first because it makes the mechanism testable without also solving a
partitioning-quality problem.

## Risks

- **Cut quality determines all downstream performance** and is a research
  problem in its own right; declaring cuts by hand sidesteps it at the cost of
  usability.
- **Two representations of one design** - partitioned and single-file - is a
  second format surface to keep in agreement.
- **Criterion 3 is easy to claim and hard to hold**, because any convenience
  index over the whole design silently reintroduces the whole-design footprint.

## Evidence

- The ceiling this feature removes: `src/jls/FileAbstractor.java:65`, enforced
  at `:152` and `:306`.
- The load path that must become streaming: `src/jls/Circuit.java:1300-1422`,
  with the linear removal at `:1369` inside the partition walk.
- The net identity a cut depends on: FEAT-004, whose TASK-0007 extracts the
  partition walk that today exists in three copies and whose TASK-0008 keys net
  names off stable id.
- Owner: **UNOWNED**; added with CAP-17 after the capability roadmap was
  committed.
- **Cost reconciliation.** Band 10-16 mw with no tasks; part of CAP-17's own
  38-62 mw arithmetic for its four new features. Not a task rollup.
