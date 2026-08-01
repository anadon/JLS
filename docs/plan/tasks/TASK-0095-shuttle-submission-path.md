# TASK-0095 - The shuttle submission path, documented and walked

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0094

## Deliverable

The end-to-end path from a JLS circuit to a submitted shuttle entry, written
down, mechanically self-tested, and **walked once** with the result recorded -
in the exact shape the repository already uses for the iCEstick handoff.

Precisely what changes:

- `docs/shuttle-submission.md`, modelled on
  `docs/icestick-bitstream-handoff.md`: the prerequisites table (git, a GitHub
  account, the shuttle template repository, an open shuttle window); the
  honesty note in that document's own words - **JLS emits the wrapper, the
  metadata and the RTL and stops there**; the exact command sequence; the
  post-submission step (open the CI-produced GDS in KLayout - one paragraph and
  a link, not a JLS feature); and a **submission record table** with the same
  `_TBD_` discipline as
  `docs/icestick-bitstream-handoff.md:117-119`, filled in when the walk
  happens.
- `scripts/shuttle-handoff.sh`: the thin wrapper that copies the three
  TASK-0094 outputs into a clone of the template repository at the paths the
  template expects, generates the stub testbench and `docs/info.md`, and stops.
  All-or-nothing preflight in `icestick-handoff.sh`'s idiom: every missing
  prerequisite reported in one pass with its role and where to get it, then one
  nonzero exit. **No JLS-side synthesis, place-and-route, or GDS logic**, per
  the `#215 H2` rule the existing script states at its head
  (`scripts/icestick-handoff.sh:1-27`).
- `scripts/shuttle-handoff-selftest.sh`: the hermetic control-flow harness, in
  `scripts/icestick-handoff-selftest.sh`'s idiom (`:1-21`) - stub `PATH`,
  assert each single missing tool prints its own preflight message and exits
  nonzero, assert all-missing lists every gap in one exit, assert the fully
  stubbed happy path reaches the final step.
- CI: the self-test added to the same lane the iCEstick self-test runs in.
- **The walk itself**: one real design submitted, and its record - date,
  shuttle name, template commit, LibreLane version, tile size, utilization,
  and the outcome - written into the table. If the walk fails, the failure is
  recorded with the same rigor; a recorded failure is the deliverable's honest
  form.

Done means: a reader who has never used the shuttle can follow the document
and get a submission-shaped repository; the self-test runs green in CI on a
machine with no external tools; and the record table has no `_TBD_` cells.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-044 | The handoff half of "Tiny Tapeout wrapper and shuttle handoff" - the part that turns generated files into a submission. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0094 | The script copies the wrapper, the metadata and the RTL into the template; only TASK-0094 produces them. There is nothing to hand off before it exists. |

## Acceptance test

- `scripts/shuttle-handoff-selftest.sh` run as a CI step, asserting: (a) each
  single missing prerequisite prints that prerequisite's preflight message and
  exits nonzero; (b) all-missing lists every gap in one nonzero exit; (c) the
  fully stubbed happy path lands all three generated files at the template's
  expected paths and exits zero; (d) a template clone that already contains a
  differing project file is refused rather than overwritten.
- `test/jls/hdl/board/ShuttleDocsAccuracyTest.everyGeneratedFileIsNamedInTheDoc()`
  (new class, in the `HotkeysHelpAccuracyTest` / `CliFlagTableTest` drift-test
  idiom): parses `docs/shuttle-submission.md` for the file names the recipe
  claims JLS produces and asserts the set equals the set
  `ShuttleWrapperEmitter` + `ShuttleMetadata` actually emit. This is the test
  that stops the document from rotting away from the code.
- `test/jls/hdl/board/ShuttleDocsAccuracyTest.theSubmissionRecordHasNoTbdCells()`
  - asserts the record table in `docs/shuttle-submission.md` contains no
  `_TBD_`. It **fails until the walk happens**, which is the point: the task is
  not done when the script works, it is done when a submission was made.

## Related GitHub issues

**no issue** for the shuttle path; `search_issues` over `anadon/jls` for
`tapeout` returns nothing. Adjacent:

| # | title | relationship |
|---:|---|---|
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - open, and this task is the same shape for a different target. Its Definition of Done lines ("handoff script produces a bitstream for the sample circuit; per-missing-tool preflight failures"; "manual flash recorded with versions where hardware exists"; "no JLS-side bitstream code") transfer verbatim. It is **not** closed by this task: #264's named targets are the iCEstick and an ECP5 board. |
| #215 | (consolidated into #264) | informs - **closed**. Its H2 "delegate, do not reimplement" is the rule this script obeys. |

## Notes

- **The walk is the deliverable, not the script.** The repository already has
  one recorded-but-unwalked handoff: the manual flash version record at
  `docs/icestick-bitstream-handoff.md:117-119` is five `_TBD_` cells. Shipping
  a second unwalked recipe would make that a pattern instead of a gap. The
  acceptance test above is written specifically so this cannot happen quietly.
- **Shuttle windows are external and time-boxed.** The walk depends on an open
  submission window; that is a real scheduling dependency on the outside world
  and must be planned around, not discovered. If no window is open, the honest
  intermediate state is a completed dry run recorded as a dry run.
- **Do not let the script grow.** The moment it starts invoking a synthesis or
  layout tool it has crossed the line the existing handoff script states in its
  own header. The whole point of the shuttle path is that the flow runs in
  someone else's CI.
- **Cost note.** Two weeks buys the document, the script, the self-test, the
  drift tests and one walk. It does not buy a second shuttle target, a
  submission GUI, or any post-route annotation import - the latter is the
  genuinely interesting follow-on (matching returned cell names against JLS
  stable ids) and is a separate project.
- **The recipe must state what LibreLane does to the design**: constant
  propagation, technology re-mapping, drive-strength replication and renaming.
  A student who expects their drawn `NandGate` to appear as one cell in the
  returned layout should learn that from the document, not from the layout.

## Evidence

- The precedent this copies, in full, at HEAD:
  `docs/icestick-bitstream-handoff.md:1-40` (the honesty note "JLS does not
  build bitstreams", the prerequisites table with role and source per tool, the
  all-or-nothing preflight description), `:105-119` (self-test framing and the
  five-`_TBD_` manual record table); `scripts/icestick-handoff.sh:1-30` (the
  delegate-do-not-reimplement header and the tool chain);
  `scripts/icestick-handoff-selftest.sh:1-21` (the hermetic stub-`PATH`
  harness and its three assertions).
- The shuttle flow itself - template contents, LibreLane in the template's CI,
  the student never producing GDS, and the KLayout viewing step costing zero
  maintainer-weeks: `08-views-determination.md` §1.5.
- Post-route name instability across the LibreLane boundary, and why the
  annotation-import problem is a different project:
  `08-views-determination.md` §1.5.
- The all-or-nothing error-aggregation contract this inherits:
  `src/jls/hdl/board/PcfEmitter.java:14-30`.
