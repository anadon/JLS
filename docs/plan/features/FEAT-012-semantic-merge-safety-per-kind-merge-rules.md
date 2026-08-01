# FEAT-012 - Semantic merge safety and per-kind merge rules

**Status:** proposed | **Cost:** 9-13 mw | **Owner program:** P11 |
**Spine rank:** -

## Capability delivered

A merged circuit file either loads as a circuit that means what both authors
meant, or it is refused with a diagnostic that names the reason. The class of
outcome that exists today - a textual merge that produces no conflict markers,
parses, loads, and simulates a circuit neither author drew - stops existing.
Every record kind in the format declares how it merges, so the answer for a
geometry record, a wire attachment, a memory image and a checkpoint are each
decided once and tested, rather than being whatever a line-oriented merge
happened to do.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | beneficial | silent corruption on a supported workflow is a defect, and the workflow is "two students on one lab" |
| CAP-01 | required | concurrent editing is merge by another name; a convergent replication layer that can converge onto a corrupt state has converged onto nothing |
| CAP-06 | beneficial | an instructor merging a skeleton update into student work must be told when it cannot be done |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-003 | merge rules are expressible only over references that name their referent; under save-time indices two independent insertions renumber overlapping line ranges, which is what produces the measured clean-but-wrong merge |
| FEAT-013 | merge participation is a per-section property - structural sections merge, bulk images and checkpoints are resolved to one side with a recorded conflict - and there are no sections to declare it on until the section frame exists |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0031 | Semantic validation of a merged file | the load path enforces referential integrity and no semantic integrity; the two invariants it misses are exactly the two silent merge hazards |
| TASK-0032 | The per-record-kind merge rule table | the rules must be written down per kind with a test per row, or they are decided ad hoc by whoever hits the case |
| TASK-0005 | Reference elements by stable id, with a diff ratchet | shared with FEAT-003: the reference form is the precondition |

## Acceptance criteria

- A headless `Circuit.validate()` runs at the end of the load path and from a
  batch flag with its own exit status. It rejects, with a distinct diagnostic
  per class: dangling references, unknown named puts, doubly-attached puts,
  duplicate stable ids, **net bit-width disagreement**, and **duplicate element
  names**. The last two are the ones that pass silently today.
- A merge-safety matrix test is table-driven over at least nine scenarios -
  independent adds, delete versus append, insert-early versus wire-spares,
  independent width edits, dangling reference, unknown put, double-attached put,
  duplicate stable id, duplicate name. For every row the assertion is: either
  the textual merge conflicts, or the merged file loads, validates, and its
  element multiset equals the intended result. Two rows fail today.
- Every record kind named in the format specification has a declared merge rule
  and a test asserting it. "Undeclared" is not a valid state for a kind.
- A merge driver presented with divergent bulk-image or checkpoint sections
  records a conflict and resolves to one side. It never combines them line by
  line.
- The in-tree git integration - a diff attribute with a working hunk-header
  pattern, and a textconv driver over the canonical text - ships with written
  limits, including in bold that textconv does not restore delta compression.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) semantic merge safety and the validator gap | no issue |
| #171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo | overlaps - a convergent type decides *which* concurrent edits win; this feature decides which merged results are legal at all, and the two must agree per record kind |
| #170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | overlaps - both harden the non-editor entry paths into the model; the validator is the shared floor |
| #163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs |

## Design notes

The framing that makes this tractable: any invariant enforced only by the editor
is a silent-merge hazard by construction, because a merged file enters through
the load path and the load path does not run editor code. The two missing checks
already exist on the editor path and in the operation layer's paste rules. This
feature is mostly about moving them to where every entry path sees them - merge,
hand edit, generated file, peer snapshot, importer output - and then declaring
which invariants are format-level rather than editor-level.

Merge rules per kind are not a taxonomy exercise. The interesting rows are the
ones where line-oriented merge is wrong in a specific way: geometry records
(last-writer-wins per artifact, never per line), wire attachments (a set, not a
sequence), memory images (never merged), checkpoints (never merged, and outside
the file's identity hash), and per-view sections (merged independently, since
two authors in two views are the CAP-01 case).

`docs/collaborative-editing-research.md` and `docs/operation-layer.md` own the
op-level design; this feature is the file-level counterpart and must not restate
them. The two levels meet at exactly one place: the set of legal states, which
this feature defines and the op layer must preserve.

## Risks

- **Validation that is too strict rejects files JLS itself wrote.** Every new
  invariant must be run over the entire tracked fixture corpus and over the
  output of every importer and emitter before it is armed, and the arming should
  be a ratchet rather than a flag day.
- **A merge driver is a maintenance surface at bus factor 1.** The cheaper
  half - the validator - delivers most of the safety and none of the surface; it
  should ship first and independently, and the driver should be justified on its
  own after that.
- **Sequencing.** Building the rule table before the section frame exists means
  writing rules for records that are about to be re-framed. The prerequisite on
  FEAT-013 is real, and skipping it costs the table twice.

## Evidence

- Measured, `05-diff-stability.md` §1 fact 3: two independent one-element
  additions merged with `git merge-file` exit zero with **zero conflict
  markers**, producing 1,040 element blocks with one duplicate reference id;
  JLS then refuses the file with "The file may be truncated - recover from the
  `.jls~` checkpoint".
- Measured, same source: a second constructed merge loaded **and simulated**,
  printing `Output Pin out: 0xFFF (4095 unsigned, -4081 signed)` on a pin the
  file declares as 4 bits - a circuit the editor's own connect check would not
  let anyone draw.
- Measured, same source: of six invariant classes, four fail loudly on load and
  two fail silently, and the two silent ones are net bit-width agreement and
  element-name uniqueness.
- Verified at HEAD: the connect-time width check lives on the editor path
  (`src/jls/edit/SimpleEditor.java`, the "Bits don't match" rejection) and not on
  the load path; `src/jls/Circuit.java` `finishLoad` enforces reference
  resolution, named-put existence, single attachment and stable-id uniqueness
  only.
- Verified at HEAD: `src/jls/edit/CircuitSnapshot.java:19-27` stores save-format
  text, so a stable-id collision poisons the undo stack as well as the file.
- Cost band basis: `05-diff-stability.md` §5 A2, A4, A7 plus §6 tests T4, T7, T8
  and `docs/capability-roadmap/lf-06-diff-merge-vcs.md`'s P11 band.
- Do not restate: `docs/file-format.md` owns the invariant list as specified,
  `docs/operation-layer.md` owns op semantics,
  `docs/collaborative-editing-research.md` owns the replication survey.
- **Cost reconciliation.** Band 9-13 mw. Tasks named for it: TASK-0005,
  TASK-0031, TASK-0032, totalling 5.5 wk. The named tasks are the leading,
  dividable slices of this feature, not the whole of it; the residual has no
  task id, because the registry's task space is closed at TASK-0112. Do not
  read 5.5 wk as the feature. Shared tasks counted once at the task level:
  TASK-0005, TASK-0032.
