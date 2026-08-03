# CAP-06 - Course delivery and autograding at scale

**Status:** proposed | **Priority:** 7 | **Marginal cost:** 12-20 mw |
**Standalone cost:** 18-30 mw

## Outcome

An instructor ships a lab as data, several hundred students submit `.jls` files,
and one batch invocation grades all of them deterministically against a
specification the instructor wrote inside the tool, producing a machine-readable
per-student report with a verdict, a counterexample and a coverage figure -
instead of a Python script diffing three literal lines of a text report format.

## Acceptance test

SEEN: the instructor runs
`jls -b -grade lab3.spec -report results.xml -reports-dir out/ submissions/`
over 300 `.jls` files. The console prints a summary
("300 submissions, 271 pass, 24 fail, 5 unloadable"). `out/` holds one report
per student naming, for each failure, the specific input vector or state where
the submission differs from the reference and which part of the spec it
violated. Re-running on a second machine produces byte-identical reports. A
student opens their own submission in the editor, opens the test panel, presses
Run, and sees the same verdict against the same spec before submitting.

CHECK: three named tests.
- `GradingDeterminismTest` - the same submission corpus graded twice, on two
  JDKs and two platforms, yields byte-identical `results.xml` and byte-identical
  per-student reports. Fails today: there is no report artifact at all.
- `GradingVerdictExitStatusTest` - a wrong submission yields a distinct exit
  status meaning "the run completed and the answer was wrong", separate from the
  shipped 0/1/2. Fails today: `docs/batch-interface.md` §1's contract has exactly
  three statuses and none of them means "wrong".
- `GradingSpecCoverageTest` - a submission that is wrong on 254 of 256 input
  vectors and right on the one the reference example checks is reported as
  FAILING, with the count of vectors exercised recorded in its report. This is
  the falsification guard: the shipped `examples/autograde/autograde.py` grades
  on three literal stdout lines for one input vector, and that submission passes
  today.

## Demo slice

The report channel and the verdict status alone, over the existing `-t`
test-vector engine: an expectation side in the test grammar, a machine-readable
xUnit-shaped report, the fourth exit status, and a directory-mode invocation
that grades a folder. No formal engine, no GUI panel. 4-7 mw, and it is already
the difference between "grading is a string diff" and "grading is a verdict".

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-053 | Test-vector front end and autograding at scale | the expectation side, the batch harness, the report format and the in-editor panel - this capstone's spine | required |
| FEAT-005 | Quadratic and materializing I/O paths eliminated | 300 submissions x a real vector set is not affordable while stimulus parse is quadratic | required |
| FEAT-006 | Simulation capacity and long-run ergonomics | a grading run must be interruptible, resumable and free of silent event drop | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | `GradingDeterminismTest` compares across platforms, so the platforms must be required checks | required |
| FEAT-003 | Uncompressed canonical default with stable-id references | an instructor reviews a submission as a diff against the handout skeleton | required |
| FEAT-016 | Subcircuit type identity, VLNV and the circuit-library format | a lab handout is a distributed library of circuits with a version, not a zip of copies | required |
| FEAT-017 | Shared and parameterized subcircuit definitions | one handout definition instantiated per student, parameterized per section | required |
| FEAT-015 | Headless `CircuitOp` layer | grading is headless and must construct and mutate circuits without a `Graphics` | required |
| FEAT-035 | Checkpoint and simulation-state serialization | a 300-submission run must survive a machine going away mid-batch | beneficial |
| FEAT-032 | Host byte port, `Console` and transcripts | labs whose answer is a transcript, graded by replay rather than by final register values | beneficial |
| FEAT-011 | Accessibility, keyboard operability and onboarding | a course cannot assign a tool a student cannot operate, and a first-run student must not be dropped on a blank canvas - but this **SUBSTANTIALLY SHIPPED**, and the residual owns no observation in this capstone's outcome. See the note below the table | **background - not required** |
| FEAT-010 | Deterministic native installers and file association | a course of 300 installs the tool without bringing its own JDK - but this **SUBSTANTIALLY SHIPPED**, and the residual owns no observation in this capstone's outcome. See the note below the table | **background - not required** |
| FEAT-012 | Semantic merge safety and per-kind merge rules | group labs merge; a merge that parses but is corrupt is an ungradable submission | beneficial |
| FEAT-050 | Module runtime consumed: extension points and providers | a course ships its own element or exporter as a module rather than a fork | beneficial |
| FEAT-051 | P2P session foundation and shared session v1 | paired lab work and live instructor assistance in a student's own file | beneficial |
| FEAT-052 | CRDT replication, collaborative undo, security hardening | a shared classroom session must not let one peer inject an element type the other did not allow | beneficial |
| FEAT-025 | Logisim-Evolution `.circ` importer and migration report | a course migrating its existing lab bank arrives with `.circ` files | beneficial |

**FEAT-010 and FEAT-011 left the required set, and the correction is recorded rather than made silently (D16, 2026-08-03).** This table graded both `required`; the filed capstone issue **#300** omits both from `requires_features`, and the disagreement is decided in the issue's favour on evidence rather than on the template's authority rule alone:

- **FEAT-010, deterministic native installers and `.jls` file association - SUBSTANTIALLY SHIPPED.** `scripts/build-installer.sh` drives `jpackage` with the file association wired per platform (`:363`, `:409`, `:477`); determinism is measured by `.github/workflows/repro-installers.yml` as a report-only probe; #190 (the msi determinism leg) closed as completed. The live residual is promoting the probe to a gate and clean-machine install verification (#188, #191, #284, #285), now carried at feature tier by **#338**.
- **FEAT-011, keyboard operability and accessibility - SUBSTANTIALLY SHIPPED.** `docs/keyboard-a11y-verification.md` exists at `2d0ca9d` and is 146 lines; `test/jls/ui/` holds 34 files, of which 28 match `*Test.java`. #75's own title records keyboard operability as landed with a named residual, and #73 owns first-run onboarding. Now carried at feature tier by **#355**.
- **Two independent witnesses on the far side.** **#338 declares `serves_capstones: []`** - it serves no capstone at all - and **#355 declares `serves_capstones: [296]`** - CAP-00, not CAP-06. A feature that believed itself required by this capstone would carry `300` there; neither does.
- **The minimality test is what actually decides it.** Neither residual has an answer to *"what breaks in the outcome statement if it is removed"*. Both are asserted not to have regressed, by that capstone issue's AC-6, which needs no feature to build it.

Both remain in `## Related GitHub issues` below and in this table as **background**; neither is in the required set, and the required-feature sum in the Cost section is the eight-row sum with their 8-16 mw and 6-10 mw removed.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine (Digital-parity, HDL-independent) | closes |
| #82 | Distribution: jpackage installers per OS and `.jls` file association | closes |
| #188 | Deterministic native installers: per-format byte-reproducibility program | depends on |
| #190 | Deterministic Windows installer: reproducible (or bounded-residual) msi | depends on |
| #191 | Deterministic macOS installer: reproducible (or bounded-residual) dmg | depends on |
| #75 | Keyboard operability and accessibility | depends on |
| #76 | Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode, persistent preferences | depends on |
| #73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots | depends on |
| #265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | depends on |
| #169 | Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel | overlaps |
| #171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo | overlaps |
| #223 | Extension-point catalog: enumerate and type the seams modules contribute to | overlaps |
| - | (no issue) CAP-06 itself, the grading contract, the report channel and the verdict exit status | no issue |

## Open decisions

1. **Report artifact shape.** Recommend xUnit XML as the wire format, with a
   sidecar JSON for the counterexample. Reason: every CI system, LMS bridge and
   grading server already ingests it, and the roadmap's verification program
   already proposes that artifact shape.
2. **Exit-status extension.** Recommend adding statuses beyond the shipped 0/1/2
   in one design pass, reserving room for later formal verdicts, rather than
   adding one now and renumbering later. Reason: the status contract is a public
   interface pinned by a CLI table test; changing it twice breaks every grading
   script written against the first version.
3. **Vector grading vs proof grading.** Recommend shipping vector grading with an
   explicit coverage figure first, and treating equivalence proof as a later
   increment on the same report channel. Reason: proof grading depends on the
   four-state core for don't-care handling; without it a proof mode would give
   confident wrong answers on exactly the designs students get wrong.
4. **Where the spec lives.** Recommend the spec as a separate versioned file the
   instructor distributes, not a section inside the student's `.jls`. Reason: a
   spec inside the submission is a spec the student can edit.
5. **Does CAP-06 fund the collaboration features or merely consume them?**
   Recommend consuming: CAP-01 owns FEAT-051 and FEAT-052 and CAP-06 should not
   be blocked on them. Reason: classroom pairing is a beneficial, not required,
   line in this capstone's own table.

## Kill criteria

- K1. If `GradingDeterminismTest` cannot be made green across two platforms after
  FEAT-007 lands, grading at scale is not reproducible and the capstone's central
  claim fails; stop and fix determinism before adding grading surface.
- K2. If a 300-submission run cannot complete inside a single instructor sitting
  on commodity hardware after FEAT-005 and FEAT-006 land, the batch path is the
  wrong substrate and the capstone must be re-scoped to per-submission
  invocation.
- K3. If the report channel plus the verdict status exceeds 1.5x the demo slice's
  band before one real lab is graded end to end, stop and re-cost.
- K4. If adding the expectation side to the `-t` grammar would break any existing
  test file, the grammar extension is wrong; it must be additive.

## Evidence

- Grading today is a string diff over bytes: `examples/autograde/autograde.py`
  grades on three literal stdout lines, pinned in CI by
  `test/jls/AutogradeBridgeExampleTest.java`. Both files exist at `b54e6ee`.
- There is no verdict: `docs/batch-interface.md` §1's exit-status table has
  exactly three values - 0 run completed, 1 runtime failure, 2 usage error -
  with none meaning "the run completed and the answer was wrong".
- The `-t` grammar has no expectation side; the batch invocation form is
  `jls -b [-s paramfile] [-t testfile] [-d limit] [-vcd file] [-r printer] [--]
  circuit.jls` (`docs/batch-interface.md:22`).
- Program ownership and the grading-contract analysis:
  `docs/capability-roadmap/lf-04-formal-and-grading.md` (the verification
  program's formal and grading half) - referenced, not restated.
- The measured cost basis and the priority-7 placement: `registry.md` Table 1;
  `10-capstone-plan.md` §3.
- Do not restate: `docs/batch-interface.md` owns the CLI contract,
  `docs/reproducibility.md` and `docs/windows-msi-determinism.md` own installer
  determinism, `docs/keyboard-a11y-verification.md` owns accessibility
  verification.
- **Cost reconciliation.** Marginal band 12-20 mw. **Its 8 required features
  sum to 51-81 mw** and its 7 beneficial features are additional.
  **Recomputed 2026-08-03 under D16, and the arithmetic is shown rather than
  the total edited:** this line read *"10 required features sum to 65-107 mw"*,
  which counted FEAT-010 at 8-16 mw and FEAT-011 at 6-10 mw. Both left the
  required set as background (see the note under the prerequisite table), so
  `65 - 8 - 6 = 51` and `107 - 16 - 10 = 81`. The eight remaining rows are
  FEAT-003, FEAT-005, FEAT-006, FEAT-007, FEAT-015, FEAT-016, FEAT-017 and
  FEAT-053, which is exactly `requires_features` on #300. **The marginal band
  is unchanged at 12-20 mw**: removing rows cannot raise a marginal band, and
  no row was re-priced. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
