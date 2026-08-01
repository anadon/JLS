# FEAT-053 - Test-vector front end and autograding at scale

**Status:** proposed | **Cost:** 9-15 mw | **Owner program:** P5 |
**Spine rank:** -

## Capability delivered

The batch test-vector engine gains an expectation side, a graphical front end
and a reporting format, so that "this design was exercised" becomes "this design
passed or failed, here is which vector and which signal". On top of that, a
harness runs one lab's vectors across many submissions and emits
machine-readable reports, and the same expectation machinery carries property
checking, equivalence between two designs, and coverage reported as data.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | beneficial | "The disagreement is the grade" is only useful if the disagreement is reportable |
| CAP-06 | required | The expectation side, the batch harness, the report format and the in-editor panel - this capstone's spine |
| CAP-09 | required | Property checking, equivalence and coverage over an unfamiliar design is this capstone's payload |
| CAP-16 | required | Migration claim 2 is a vector replay, and the batch engine it replays through is this feature |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-008 | The in-editor panel is editor code. Adding it to an unfloored editor with no UI harness means a grading surface with no regression detector |
| FEAT-034 | Equivalence between two designs and the answer-logging discipline are the parity comparator applied to a pair of designs rather than to a design and a reference |
| FEAT-019 | Equivalence checking and coverage over an unfamiliar design run through an external tool, and the netlist writer is how a JLS design reaches one |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0111 | The test panel, the grading harness and its reports | The front end, the harness over many submissions, the machine-readable export, and one complete worked lab as the reference for course authors |
| TASK-0112 | Property checking, equivalence and coverage over an unfamiliar design | Properties expressible and checked, an equivalence check between two designs, and toggle and branch coverage reported as data |
| TASK-0021 | The UI test harness, including dialog construction | Shared with FEAT-008: the panel cannot be regression-tested without it |
| TASK-0073 | The differential comparator, exclusion set and sync points | Shared with FEAT-034: equivalence between two designs reuses the comparator and its exclusion discipline |

## Acceptance criteria

1. A test description carries expectations, and a run reports pass or fail per
   vector with the disagreeing signal named.
2. The existing batch test-vector grammar is unchanged; expectations arrive
   through an additive path so that every existing test description still runs
   and still means the same thing.
3. The batch exit status distinguishes "ran and passed", "ran and failed" and
   "could not run", and the distinction is asserted by a test.
4. A harness runs one lab's vectors over a directory of submissions and emits
   one machine-readable report per submission plus one aggregate, deterministic
   under reordering of the submissions.
5. A property is expressible over a design the author did not write, is checked,
   and a vacuous pass is detectable - a property that cannot fail is reported as
   such.
6. Coverage is reported as data with a stated metric, not as a percentage with
   no denominator.
7. One complete worked lab ships as the reference a course author copies.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine (Digital-parity, HDL-independent) | closes |
| 72 | Make batch mode a documented, tested grading API: test-vector format spec, output-format goldens, and VCD waveform export | informs, **closed** - it established the documented batch grammar and the output goldens this feature adds an expectation side to |
| 216 | Waveform + verification interop: document the VCD to GTKWave/Surfer handoff and provide a batch-CLI autograde-bridge example | informs, **closed** - the autograde bridge it describes is the outer shape of criterion 4 |
| - | Property checking, equivalence and coverage | **no issue** |

## Design notes

The one non-obvious constraint is that the batch engine at HEAD has no verdict
at all. Its test-vector grammar posts stimulus; nothing in it expresses an
expected value, and the batch exit contract has three statuses, none of which
means "completed and the answer was wrong". The front-end issue's own definition
of done forbids changing that grammar and forbids new assertion semantics. Both
can be satisfied at once only by putting expectations in a separate file behind
a new flag, leaving the existing grammar literally untouched. That reading is
what makes the issue closable on the record, and a maintainer should ratify it
rather than let it be assumed.

Criterion 5's vacuity check is the difference between a verification feature and
a green light. A property that cannot fail passes on every design including a
wrong one, and a grading harness built on unfalsifiable properties grades
nothing.

## Risks

- **Grading is a trust surface.** A report that says "passed" for a submission
  that did not run is worse than no harness, which is why criterion 3 separates
  "could not run" from "failed".
- **Coverage invites a number that gets used as a target.** Reporting it as
  data with a stated metric is a deliberate refusal of a single score.
- **Equivalence between two arbitrary designs is undecidable in general.** The
  feature must state the class it decides and refuse the rest by name.

## Evidence

- The batch grammar and the exit contract this feature extends without changing:
  `docs/batch-interface.md`; the stimulus path at `src/jls/elem/SigSim.java`.
- The batch simulator the harness drives: `src/jls/sim/BatchSimulator.java`.
- The verification and grading program that owns the band:
  `docs/capability-roadmap/lf-04-formal-and-grading.md`.
- Issue #214, open, verified against `list_issues(state=OPEN)`; #72 and #216
  closed.
- Owner: P5 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 9-15 mw; TASK-0111, TASK-0112, TASK-0021 and
  TASK-0073 total 8 wk, of which TASK-0021 and TASK-0073 are shared with
  FEAT-008 and FEAT-034 and counted once at the task level. The unshared
  remainder is 4 wk against a 9-15 mw band; the residual is the formal half -
  properties and equivalence beyond their leading slice - which the registry
  records as deliberately folded into this feature rather than given its own id.
