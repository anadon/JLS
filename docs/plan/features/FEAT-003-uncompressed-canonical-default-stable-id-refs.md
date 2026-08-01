# FEAT-003 - Uncompressed canonical default with stable-id references

**Status:** proposed | **Cost:** 2-4 mw | **Owner program:** P11 |
**Spine rank:** S6

## Capability delivered

A saved circuit becomes an artifact a person can review. Inserting one element
changes one hunk instead of a quarter of the file, because every reference in
the file names the referent by its permanent identity rather than by its
position in the save, and because the bytes on disk are the canonical text
rather than a compressed stream whose every byte moves when one byte of input
does. Version control, code review, three-way merge, a grading hash oracle and
a collaboration replica all become possible over the same file the editor
writes, with no separate export step and no second serialization.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | the diff amplification is a measured HEAD defect and the largest single tax on every reviewed change |
| CAP-01 | required | replicas must save byte-identical files, and a merge that cannot be reviewed cannot be trusted |
| CAP-05 | required | the schematic under review and the emitted board netlist must both diff cleanly for a board review to exist |
| CAP-06 | required | an instructor distributing a skeleton and hashing submissions needs the canonical text to be the file |
| CAP-13 | beneficial | a KiCad round trip is reviewed as a diff of the JLS source, not only of the emitted artifact |
| CAP-16 | beneficial | a migrated `.circ` lands as a readable first commit rather than an opaque blob |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-001 | the writer's save-tag table is keyed on the element registry; changing the reference form rewrites every record kind, and a non-total tag table drops the kinds it does not know. Commit `970db41` is the precedent: two registered element types were missing from the frozen tag table |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0005 | Reference elements by stable id, with a diff ratchet | the reference form is the root cause; the ratchet is what stops it regressing |
| TASK-0006 | Plain text as the default container, with the autosave policy | the container flip is a one-line default plus a decided policy for autosave and undo snapshots, and it is only safe after the reference form lands |

## Acceptance criteria

- Inserting one element into a 1,000-element circuit and re-saving changes at
  most 12 lines in at most 2 hunks, for every replica-id class: a 32-hex draw, a
  string sorting before `legacy`, and a string sorting after it. Deleting one
  element is the same bound. Moving one element changes at most 4 lines in 1
  hunk, which passes today and must not regress.
- The default write path produces bytes that start with `FORMAT`, not with an XZ
  magic number, and `sha256sum` over the file equals `Circuit.stateHash()`.
- Compression remains reachable as an explicit user option, and reading still
  sniffs content rather than file name.
- The reader accepts both the old positional reference form and the new
  stable-id form for one declared epoch; every tracked fixture loads under both.
- Autosave checkpoints and in-memory undo snapshots have a written, tested
  container policy rather than an inherited one.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) the whole diff-stability and format program - decisions D1 and D2 | no issue |
| #167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | informs - `docs/operation-layer.md:60-63,82-86` records a workaround forced by the positional reference form; this feature deletes the reason for it |
| #171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo | overlaps - convergence is asserted on canonical bytes, so the canonical form is that issue's oracle |
| #163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs - the same artifact, reviewed rather than replicated |

## Design notes

The two halves are one shipment because separating them ships a format epoch
that introduces a diff regression and then fixes it: flipping the container
first exposes the amplification to git without fixing it, and fixing references
first leaves the amplification hidden inside a compressed blob where nobody can
measure the improvement. `05-diff-stability.md` §5 Part B item B1 costs the
reference change as the single most valuable line item in that report.

Sort key: prefer `(tag, sid)` over bare `sid`. They are identical on every
measured diff metric, `(tag, sid)` is dramatically more readable, and it
subsumes the existing `isWire ? 1 : 0` special case at `Circuit.java:1495` into
the general rule. Never sort on `name` or on geometry - a dialog can change
either, and `(name, sid)` was measured to turn a one-line rename into a
two-hunk block move, which is a move/edit conflict in a three-way merge.

The legacy replica ordering trap is separate from the reference form and cheaper
than it: `ElementId.compareTo` orders by replica string first, the reserved
legacy replica is the literal `"legacy"`, and every fresh replica is 32 hex
digits, so `'f' < 'l'` puts every newly added element at position 0 of every
pre-`#165` file. A per-file replica alias table fixes it and recovers the raw
size cost of 32-hex replicas at the same time.

Bulk binary payloads must leave the text body before or with the container flip;
that is FEAT-013's raw section, not this feature's. Decision D1's content-kind
table is the governing split.

## Risks

- **Working-tree size.** Plain text is roughly 98x larger than XZ at 100k
  elements and brings the 64 MiB decompressed-text cap
  (`FileAbstractor.java:65`) that much closer. Mitigated by FEAT-013 moving
  images out of the text, not by re-compressing.
- **A two-form reader is a two-form reader forever** unless the epoch has a
  written end and a migration path. TASK-0005's epoch policy must state the end
  date, and FEAT-013 owns the migration machinery.
- **Goldens regenerate once.** Every fixture rewrites. Doing this after the
  hierarchy emitters land would regenerate them twice; `09-format-adoption-plan.md`
  §3.2 makes that ordering a real dependency, not a preference.

## Evidence

- `src/jls/elem/Element.java:21-22` - `private int id`, documented as "the
  file-local reference index, reassigned on every save". Verified at HEAD.
- `src/jls/Circuit.java:1492-1503` - canonical sort is
  `(el instanceof Wire ? 1 : 0, stableId)`, then dense ids handed out `0..n-1`.
  `src/jls/elem/WireEnd.java:605,611` emits `ref attach <id>` and
  `ref wire <id>` against those dense ids. Verified at HEAD.
- `src/jls/FileAbstractor.java:43-56` - the `Container` enum with `XZ` documented
  as the default and `PLAIN_TEXT` as the opt-in; `:65` is
  `MAX_CIRCUIT_TEXT_BYTES = 64 << 20`, already measured against decompressed
  text. Verified at HEAD.
- Measured, `05-diff-stability.md` §1: inserting one `AndGate` into
  `test/fixtures/riscv-sum1to10.jls` (1,038 element blocks, 10,744 lines)
  changes 5,312 lines in 234 hunks against a true minimal edit of 10 lines in 1
  hunk. Under stable-id references the same edit is 0 removed / 9 added / 1
  hunk, and the file is 9.7% shorter.
- Measured, same source: over 100 revisions of a one-element nudge, XZ costs
  7,191 B/rev with 0 of 100 deltas; plain text costs 482 B/rev at default gc.
- Cost band basis: `05-diff-stability.md` §5 Parts A and B (A6 plus B1/B2/B3) at
  the repository's ~200-250 shipped-and-tested lines per maintainer-week
  calibration.
- Do not restate: `docs/file-format.md` owns the container grammar and the
  `FORMAT` header; `docs/reproducibility.md` owns determinism claims;
  `docs/capability-roadmap/lf-06-diff-merge-vcs.md` owns P11's program boundary.
- **Cost reconciliation.** Band 2-4 mw. Tasks named for it: TASK-0005,
  TASK-0006, totalling 3 wk. Band and task sum agree; no reconciliation is
  needed. Shared tasks counted once at the task level: TASK-0005.
