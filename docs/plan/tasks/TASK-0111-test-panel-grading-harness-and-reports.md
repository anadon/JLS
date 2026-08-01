# TASK-0111 - The test panel, the grading harness and its reports

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0021

## Deliverable

An expectation surface, one shared runner behind both the CLI and a GUI panel,
a machine-readable report, and one complete worked lab that a course author can
copy.

**The gap this closes, stated precisely.** `docs/batch-interface.md` §2.2's `-t`
grammar has four productions and **not one of them mentions an output**;
`SigSim.initSim` posts stimulus at parse time and nothing compares anything. The
exit contract (§1) has three statuses and **none of them means "the run
completed and the answer was wrong"**. So "per-vector pass/fail" is not a UI
task over an existing verdict - the verdict does not exist yet.

Precisely what changes:

1. **`src/jls/sim/Expectations.java`** - a separate expectations file, **not** a
   change to the `-t` grammar. Grammar:
   `expect ::= name ("at" time | "after" delay) value`, where `name` names a
   watched element from §3.2's three-type whitelist (`Register`, `Memory`,
   `OutputPin`). Keeping expectations out of the `-t` file is what lets §6's
   freeze hold and satisfies #214's "the `-t` grammar is unchanged" completion
   criterion without argument.
2. **`src/jls/sim/TestVectorRunner.java`** - the one headless parse-run-compare
   entry point, usable from `JLSStart` and from Swing. #214's H2 is that a
   single shared runner is what stops CLI and GUI verdicts drifting; this class
   is that guarantee, and the parity test is its proof.
3. **`src/jls/sim/GradeReport.java`** - xUnit XML as primary (what CI and every
   LMS autograder already ingests) plus a plain line format for humans and
   diffs. Both byte-deterministic and golden-pinned in the style of
   `test/jls/VcdExportGoldenTest`: no timestamps, no host names, no durations.
4. **CLI, additive under §6.** `src/jls/JLSStart.java:759-789` (14 `FlagSpec`
   entries at HEAD) gains `-check <file>` and `-report <file>`, each with its
   `CliFlagTableTest` row. **Exit status 3 = a check failed**, and it is reachable
   only when `-check` is present. A run without the flag returns 0/1/2 and prints
   the same bytes as today - that non-movement is the compatibility proof, and
   `BatchSimulationGoldenTest` and `VcdExportGoldenTest` are how it is checked.
5. **`src/jls/edit/TestPanel.java`** - the editor front end: load an expectation
   set, run against the in-memory circuit through `TestVectorRunner`, show
   per-vector pass/fail and the first mismatch. Wired to a menu action with a
   mnemonic and full keyboard operability. A malformed expectation is a located,
   non-fatal panel error - no crash, no `System.exit`.
6. **`examples/autograde/lab-01/`** - the complete worked lab: the circuit, the
   `-t` vectors, the expectations, a rubric mapping checks to points, an
   instructor README, and `grade.py` running the lab over a directory of
   submissions and emitting one xUnit file each plus a summary. The existing
   `examples/autograde/autograde.py` stays as the "grade the emitted bytes"
   pattern (pinned by `test/jls/AutogradeBridgeExampleTest.java:33-74`) and gains
   a pointer to the new one.
7. **`docs/batch-interface.md`** gains §2.5 (the expectations grammar), the
   status-3 row in §1, and a §6 note recording that both are gated behind a new
   flag. CHANGELOG entry.

Done means: `jls -b -t v.txt -check e.txt -report r.xml c.jls` returns 3 on a
wrong circuit and 0 on a right one, the panel agrees vector for vector, and a
directory of 200 submissions grades in one command.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-053 | The front end, the harness and the report - the whole of the feature except the property and coverage measures, which are TASK-0112. |
| FEAT-019 | A grading run over an imported or exported design needs a verdict channel; the netlist writer's differential checks report through `GradeReport`. |
| FEAT-034 | The retirement-indexed parity harness produces verdicts at declared sync points and needs the same report channel and exit status rather than a second one. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0021 | The panel is GUI. Issue #214 names #91 as a real blocker in its own Status section - "without #91 its own tests would be weak" - and the panel's acceptance tests assert component presence, menu wiring and dialog behavior, which is exactly what the harness provides. |

## Acceptance test

`test/jls/GradingParityTest.java`, new - #214's P2 and P3 made executable:

- `panelVerdictsEqualBatchVerdicts()` - for each shared fixture (the circuits and
  vector files the batch tests already use), run
  `jls -b -t v -check e` and the panel over the same in-memory circuit, and
  assert the verdict lists are equal element for element. If they diverge, the
  two paths are not sharing a runner.
- `aMalformedExpectationIsLocatedAndNonFatalInThePanel()` - assert the panel
  reports a line and column and stays open, while the same file in batch mode is
  fatal per §2.4.
- `aCircuitWithSignalGeneratorsIsReportedAsSubstitutedNotSilentlyChanged()` -
  `BatchSimulator.addTestGen` **removes** the top-level signal generators when
  `-t` is supplied (`src/jls/sim/BatchSimulator.java:183-212`), so the panel is
  not running the circuit the user is looking at. Assert the panel says so.

`test/jls/GradeReportGoldenTest.java`, new:

- `xunitReportMatchesGoldenByteForByte()` for a passing and a failing run.
- `theReportContainsNoTimestampHostNameOrDuration()` - asserted directly, because
  those are the three fields an xUnit writer adds by default and each one breaks
  determinism.

`test/jls/CliSmokeTest`, extended:

- `checkFlagReturnsThreeWhenAnExpectationFails()` and
  `checkFlagReturnsZeroWhenEveryExpectationHolds()`.
- `aRunWithoutCheckPrintsTheSameBytesAndReturnsZeroOneOrTwo()` - the
  compatibility proof, asserted rather than assumed.

`test/jls/BatchSimulationGoldenTest.watchedElementsPrintInNameOrder` and both
`VcdExportGoldenTest` goldens must pass **unmodified**. If either moves, the new
observable is not gated behind the flag.

`test/jls/AutogradeBridgeExampleTest` gains
`theWorkedLabGradesThreeSubmissionsWithTheExpectedScores()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 214 | In-editor test panel: a GUI front-end over the batch -t test-vector engine (Digital-parity, HDL-independent) | closes |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on - named as a blocker by #214 itself |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | overlaps - the panel is a new dialog surface and inherits #162's construction-coverage requirement |
| 75 | Keyboard operability and accessibility: focus follows the mouse, the menu bar has zero accelerators/mnemonics, and no element can be manipulated without a mouse | overlaps - #214 requires the panel to respect #75 |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the worked example is a natural second consumer of the report channel |

Cited by `examples/autograde/autograde.py` and by `docs/capability-roadmap/`
but **not open**: #216, #200, #57, #55. Cite them as recorded work, never as
open issues.

## Notes

- **#214's own completion criterion forbids changing the `-t` grammar**, and its
  scope note forbids "any new assertion semantics beyond what batch already
  supports". Batch supports none. The resolution is a *separate* expectations
  file behind a *new* flag: additive, §6-blessed, and it leaves the `-t` grammar
  literally untouched. Write that reading into the issue's PR so the criterion
  is satisfied on the record rather than by interpretation.
- **The stability promise is the real constraint, not the code.** §6 freezes the
  `-t` grammar, the stdout format and the VCD profile. Every new observable -
  the report file, the status 3, the panel's verdicts - is gated behind
  `-check`. Design the gate first; retrofitting it is how a promise gets broken.
- **Exit status 3 must be reachable only through the flag**, including on error
  paths. A parse failure of the expectations file is a usage error (2), not a
  check failure (3), or a broken grader scores every submission as wrong.
- **Determinism in the report is not optional.** An xUnit writer that emits a
  timestamp cannot be goldened, and a grading artifact that is not byte-stable
  cannot be diffed between a student's run and the instructor's.
- **The panel is a driver of existing semantics.** New assertion types are scope
  creep; property checking, equivalence and coverage are TASK-0112 and land
  behind their own flags on the same report channel.
- **The worked lab is the deliverable course authors actually copy.** Budget
  real time for it: a rubric, an instructor README, and three seeded submissions
  (right, subtly wrong, structurally wrong) that the acceptance test scores.

## Evidence

- `docs/batch-interface.md` §2.2 (the four-production `-t` grammar), §2.4
  (batch parse errors are fatal), §3.1 (four frozen outcome reasons), §3.2 (the
  `Register`/`Memory`/`OutputPin` whitelist), §5 (which goldens pin which
  section), §6 (the stability promise and what an additive change requires).
- `src/jls/JLSStart.java:759-789` - the `FLAGS` table, 14 `FlagSpec` entries at
  HEAD; `:382-385` shows the same table's flags being consumed.
- `src/jls/sim/BatchSimulator.java:183-212` (`addTestGen`, which replaces
  top-level signal generators), `:562-572` (`displayOutcome`).
- `src/jls/elem/TestGen.java:19-65` - `extends SigSim`, opens the file and
  delegates to `SigSim.initSim`, which is where stimulus is posted at parse time.
- `examples/autograde/autograde.py:1-60` - the shipped "grade the bytes" pattern
  and its `EXPECTED_STDOUT_LINES` constant; `test/jls/AutogradeBridgeExampleTest.java:33-74`
  pins it.
- `docs/capability-roadmap/lf-04-formal-and-grading.md:9-40` - "JLS has no
  representation of 'correct'", the four-production quote, and the observation
  that there is no exit status meaning the answer was wrong.
- `docs/capability-roadmap/sweep-04-verification.md:470-500` - change H: the
  new-flag route §6 blesses, xUnit as the primary artifact shape, status 3, and
  the one-week sizing with the CHANGELOG and `CliFlagTableTest` rows.
- Do not restate: `docs/batch-interface.md` owns the grammar and the stability
  promise; `docs/vcd-interop.md` owns the waveform grading pattern.
