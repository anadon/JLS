# FEAT-057 - Campaign execution and artifact aggregation

**Status:** proposed | **Cost:** 6-8 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A described set of independent simulation runs - a parameter sweep, a
fault-injection campaign, a directory of student submissions - is dispatched
across available cores or hosts, each run's artifacts are collected, and one
aggregate report is produced whose contents do not depend on how many workers
ran or in what order they finished. It is the scale-out-runs axis, and it is
useful on one machine on the first day.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-17 | required | The scale-out-runs axis, and the demo slice that makes the capstone deliverable before any partitioning exists |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-006 | A campaign is many long batch runs. Pause, heartbeat, clean interrupt and an unbounded run duration are what make an individual job survivable and cancellable |
| FEAT-035 | A job evicted from a shared machine must resume rather than restart, which is checkpointing |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was added with CAP-17 after it closed | - |

## Acceptance criteria

1. A campaign description names its jobs, their inputs and their expected
   artifacts, and is itself a committed, diffable file.
2. The same campaign executed at one worker and at N workers, in a shuffled
   dispatch order, produces byte-identical aggregate output and identical
   per-job artifacts. Scheduling is not observable in the result.
3. A failed job is reported as failed with its inputs and its output, and does
   not silently reduce the denominator of the aggregate.
4. Artifacts are collected per job under a stated naming scheme derived from the
   job description, not from dispatch order.
5. The campaign runner requires no distributed transport to run on one machine's
   cores.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | Campaign execution and artifact aggregation | **no issue** |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | overlaps - a course's whole submission set is a campaign, and the grading harness in FEAT-053 is the same dispatch and aggregation shape applied to submissions |

## Design notes

This feature is deliberately independent of everything else in the capacity
capstone: it runs many whole simulations rather than one partitioned one, so it
needs no model change, no transport and no partitioner. That independence is
what makes it the demo slice, and it is also what makes it immediately useful to
grading and to multi-seed verification.

It is not throwaway work. The capacity axis needs the same job, artifact and
aggregation vocabulary, so building it first buys the vocabulary the harder
axis will need anyway.

The overlap with the grading harness is real and should be resolved by sharing
rather than by duplication: whichever of the two ships first should own the job
description and the aggregation format, and the other should consume it.

## Risks

- **Two independent implementations of "run many things and collect the
  output"** is the failure mode. The overlap with the grading harness must be
  claimed by one of them explicitly.
- **Determinism under parallel dispatch is easy to lose** through any aggregate
  that appends in completion order.
- **Resource exhaustion at high worker counts** is a support problem the
  campaign description must be able to bound.

## Evidence

- The batch surface a job runs through: `docs/batch-interface.md`,
  `src/jls/sim/BatchSimulator.java`.
- The long-run ergonomics an individual job depends on: FEAT-006, whose
  TASK-0012 and TASK-0014 remove the silent time ceiling and make pause distinct
  from stop.
- Issue #214, open, verified against `list_issues(state=OPEN)`.
- Owner: **UNOWNED**; added with CAP-17 after the capability roadmap was
  committed.
- **Cost reconciliation.** Band 6-8 mw with no tasks; part of CAP-17's own
  38-62 mw arithmetic for its four new features (12-20 + 10-16 + 10-18 + 6-8 =
  38-62 mw, which is the capstone's marginal band exactly). Not a task rollup.
