# FEAT-044 - Tiny Tapeout wrapper and shuttle handoff

**Status:** proposed | **Cost:** 11.5-18 mw | **Owner program:** P6 |
**Spine rank:** -

## Capability delivered

A design drawn in JLS is wrapped in the fixed top-level signature a shuttle
program requires, described by the project metadata file that program consumes,
and carried along a documented path from the drawn circuit to a submitted
shuttle entry - a path that has been walked once, end to end, with the result
recorded rather than predicted.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-07 | required | The fixed top-level signature, the metadata file and the documented submission path - this capstone's spine |
| CAP-15 | beneficial | The same export, taken one step further, is the demonstration that the toolchain path is real |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-021 | The shuttle top-level signature includes bidirectional pins. A wrapper that cannot express `inout` cannot be generated honestly |
| FEAT-037 | The wrapper makes an active-low reset mandatory, and ASIC synthesis discards initial values, so a design whose state comes from an initial value has nothing to bind to `rst_n` |
| FEAT-023 | The path runs through the same external synthesis and place-and-route tools the differential oracle arms; without them in CI the handoff is a document nobody executes |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0094 | The shuttle wrapper and its metadata | The fixed signature and the metadata file, generated and validated |
| TASK-0095 | The shuttle submission path, documented and walked | The end-to-end path, walked once, with the result recorded |
| TASK-0052 | Per-board constraints and one real flash | Shared with FEAT-023: the constraint-emission and real-hardware discipline the shuttle handoff reuses |

## Acceptance criteria

1. A generated top level matches the shuttle template's fixed signature exactly,
   asserted against the template rather than against a transcription of it.
2. The metadata file validates against the program's published schema, and a
   missing or malformed field is reported by name before submission.
3. The generated wrapper synthesizes through the open flow without manual
   edits, exercised in the toolchain lane.
4. The submission path is documented as commands, each of which has been run,
   with the outputs recorded in the tree.
5. The record table for the walked submission contains no placeholder cells at
   completion; while placeholders remain, the acceptance check fails rather than
   passing with an incomplete record.
6. The template revision the wrapper targets is pinned by digest, and a change
   in the upstream template is detected rather than discovered at submission.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | The shuttle wrapper, its metadata and the submission path | **no issue** |
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - the shuttle handoff reuses the constraint-emission and real-hardware discipline #264 establishes for FPGA boards, and shares TASK-0052 with it |

## Design notes

The one thing this feature must not do is transcribe a template revision into
the source tree as literal text. The evidence corpus already records a case of
an obsolete template being cited; criterion 6 exists because the upstream
signature and schema move on a shuttle cadence the project does not control.

Criterion 5 is deliberately uncomfortable. The existing board-handoff document
in the tree has carried placeholder cells since it shipped, and an acceptance
test that passes with placeholders present is an acceptance test that certifies
an unwalked path.

The submission itself depends on an externally scheduled shuttle window. That
is a calendar dependency, not an engineering one, and it should be scheduled
rather than estimated.

## Risks

- **The shuttle window is external.** The walked path in criterion 4 can be
  blocked for months by a submission calendar nobody in the project controls.
- **Two other programs gate the start.** The bidirectional-port work and the
  reset work belong to different owners; this feature's start date is set by
  them.
- **A returned chip is not a returned test.** Silicon coming back does not by
  itself demonstrate that the drawn design was correct; the acceptance must
  rest on the pre-submission flow, with the chip as the demonstration.

## Evidence

- The existing real-hardware handoff this feature's discipline copies, and its
  standing placeholder cells: `docs/icestick-bitstream-handoff.md`,
  `scripts/icestick-handoff.sh`, `scripts/icestick-handoff-selftest.sh`.
- The constraint emitter and its golden: `src/jls/hdl/board/PcfEmitter.java`,
  `src/jls/hdl/board/Boards.java`, `test/resources/hdl/board/`.
- The determination that prices the shuttle path and records the obsolete
  template hazard: `08-views-determination.md` §5.
- Issue #264, open, verified against `list_issues(state=OPEN)`.
- Owner: P6 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 11.5-18 mw; TASK-0094, TASK-0095 and TASK-0052
  total 6 wk, of which TASK-0052 is shared with FEAT-023 and counted once. The
  residual is the ASIC-flow hardening between a synthesizable wrapper and a
  submittable entry, which no task id names. Do not read 6 wk as the feature.
