# Issue #576: FEAT-C33-2: handing out an assignment and grading the cohort is one documented path — distribute starters, collect submissions, one command returns per-student verdicts
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

The strategic claim behind CAP-33 (#517) is the sharpest one in the roadmap: DEEDS-dependent
instructors face a forced migration, no importer is possible for a closed format, so **the course
must port even though the files cannot**. #576 is the piece of that port that is not content — it
is the claim that JLS should own the *instructor's semester loop*, not just the simulator the
loop invokes. I endorse that claim without reservation. It is also correctly bounded: no service,
no accounts, "nothing that can die holding student work" is exactly the right instinct for a
single-maintainer GPL tool, and it is the one property no hosted competitor can copy.

What I do not endorse is where the issue cuts the seam. AC-1 makes the deliverable *a directory
layout*, and AC-2 makes it *a command that walks that layout*. Both are the wrong noun, and one of
them is already owned elsewhere.

## The duplication the pass-1 boundary note missed

The dedup comment on this issue adjudicates #576 against #577, #578 and #350 — its siblings. It
never checks #576 against **#466 (TASK-0111)**, the engine issue it declares it consumes. #466's
own §6 Materials list requires building `examples/autograde/lab-01/` including *"`grade.py`
running the lab over a directory of submissions and emitting one xUnit file each plus a summary"*;
its §7.3 already types the input as *"a directory of submissions | `grade.py` | one `.jls` per
student | hostile; a malformed submission scores as a failure with a diagnostic, never crashes the
batch"*; its §7.11 restates it (*"One bad submission must not stop a 200-submission run"*); its
Open Question 6 already decides the parallelism default. That is #576's **AC-2 and AC-4, verbatim,
inside #466's Definition of Done** — and #466's completion checklist gates on the worked lab
existing. Today `examples/` contains exactly one file, `autograde.py`, so neither has been built
and the collision is still cheap to resolve.

The resolution I would take is not "merge" — it is to move the boundary one notch. **#466 owns the
cohort loop** (it is already committed to writing it, and it is the issue that can pin it with
`AutogradeBridgeExampleTest`). **#576 owns the identity and reporting contract that loop runs
against.** That is a smaller issue, a better-defined one, and it stops two issues from each
shipping a `grade.py`.

## The reframing: JLS does not own the tree, it owns the mapping

AC-1 specifies "distribution and submission directory layouts ... in tree, with a worked example
an instructor can copy." Consider where submissions actually come from. Canvas bulk-download emits
`lastnamefirstname_12345_67890_lab3.jls`. Moodle emits a per-student folder with an assignment
suffix. Gradescope emits a flat zip. GitHub Classroom emits one repo per student. A shared drive
emits whatever forty undergraduates typed. **JLS specifying a tree guarantees that every real
instructor writes a shim converting their LMS's export into JLS's shape** — which is precisely the
friction this issue exists to delete. A specified layout is a contract JLS cannot enforce on the
upstream that produces the data.

The elegant contract is one level down and about a tenth the size:

> A **submission set** is any directory, plus a rule that extracts a student identifier from each
> circuit file's path. The rule is either a documented default (`<id>.jls` at any depth), a
> filename regex with one capture group, or a roster CSV mapping path → id. Grading is a pure
> function from (kit, submission set, roster) to one deterministic cohort report.

Everything in the current AC set falls out of that and mostly disappears as a special case:

- **AC-4 stops being robustness engineering and becomes arithmetic.** "Renamed" = the id rule did
  not match, so the file is reported as *unattributed*, by path. "Missing" = a roster id with no
  matching path, reported as *no submission*, by name — which a layout spec can never detect,
  because a layout has no roster and therefore cannot know a student exists. That is a real
  capability the current framing forfeits: as written, a student who submits nothing is invisible.
- **AC-3's "attributed to the right student"** becomes a property of the id rule rather than a
  property of the CI script, and is testable against adversarial filenames rather than one planted
  mutation.
- **#502 CAP-21's four platform adapters** stop needing to reshape trees and only need to emit a
  roster mapping — which is what each platform natively has. The identity contract is the shared
  seam between #576 and #502, and neither issue currently names it.
- **The layout demotes to example content**, where it belongs: a worked example instructors copy,
  not a specification they must conform to.

This also settles a question the issue leaves open that will bite later: `docs/batch-interface.md`
§6 freezes the `-t` grammar, the stdout format and the VCD profile, and this project's whole
character is frozen, byte-pinnable, golden-tested contracts. A directory layout that courses build
semesters of infrastructure on is a compatibility surface, but it is not one a golden test can pin
byte-exactly. An id-extraction rule and a cohort report format *are*. Put the mapping and the
report under §6's promise; leave the tree informative.

## A second reframing: the kit is the data model, and #578's ordering is backwards

#578 defines what a kit *is* (labs + vectors + schedule + rubric + validator), and records
`ordering_after: FEAT-C33-2` — the workflow before the thing the workflow operates on. That is
inverted. Define the kit manifest first — a single declarative file naming the circuit-under-test
entry point, the vector and expectations sets, the rubric, and the id rule — and the "distribution
layout" ceases to exist as a separate artifact: distribution is *the kit*, and submission is *the
kit's declared student-authored role, filled in*. Two conventions collapse into one manifest with a
validator (#578 is already building the validator). Specifying a distribution layout in #576 and a
kit format in #578 is how a project ends up with two overlapping description languages for the same
files.

## The thing nobody owns, and where I would spend the marginal week

AC-2 delivers counterexamples *to the instructor, in a report*. But #517's evidence names DigiSim's
starter/submit pattern as worth adopting — and what makes that pattern pedagogically valuable is
not the instructor's spreadsheet, it is the round trip back to the student. JLS is uniquely placed
here and no hosted service does it well: the grading run can emit, per failing student, an artifact
they **open in the editor** — the failing vector as a `-t` file plus the VCD of that run — so
"you failed test 7" becomes "here is the waveform where your carry chain diverged." Every piece
exists or is already planned: deterministic VCD (§4, shipped), the counterexample record (#466),
the `TestPanel` that renders verdicts in the editor (#466). The cost is a directory of files and a
sentence in the docs. That is the feature that would actually win a DEEDS instructor, and it is
the one this issue does not mention. I would add it as an acceptance criterion here or file it as
CAP-33's fifth planned feature.

## What I would keep exactly as written

- The no-service constraint, stated as strongly as it is. It is a durability argument, not a
  scoping convenience, and it should appear in the shipped docs in the issue's own words.
- The round-2 comment's degraded-mode rule — *"whatever AC-2's command reports without the engine
  must be a strict subset of what it reports with it — same verdict vocabulary, fewer fields ...
  under-report rather than approximate"*. That is the best sentence attached to this issue and it
  generalizes: it is the same discipline as §6's gated-observable rule and #466's H3.
- The refusal to re-own verdict semantics. Correct, and it should extend to refusing to re-own the
  cohort loop.

## One caution on AC-5

With the ordering edge corrected, AC-5 is now a live scheduling option rather than a contingency —
but be honest about what the degraded mode can contain. Against today's three statuses there is no
per-vector signal at all; the only available oracle is a string diff over §3's stdout, the
`EXPECTED_STDOUT_LINES` pattern `lf-04-formal-and-grading.md` already indicts (*"a submission that
is wrong on 254 of the 256 possible inputs and right on that one passes"*). So the degraded command
can report *ran / failed to run / output differs from reference* — and AC-2's counterexamples are
simply unreachable. Shipping that is defensible only if it is the id-and-report contract shipping
early, not a second grading semantics. If AC-1 is re-cut as proposed, the degraded ship becomes
genuinely valuable: the mapping, the roster reconciliation and the deterministic report are all
engine-independent and are the parts an instructor can build on immediately.

## Restated acceptance criteria under this reframing

1. The submission-identity contract is specified and pinned: default rule, regex form, roster form;
   unmatched files are reported by path, unmatched roster entries by name.
2. The cohort report format is specified and byte-deterministic, and re-grading the same inputs is
   byte-identical (subsumes #757's AC-4).
3. A worked kit — distribution and submission — is committed as an example, explicitly
   non-normative, conforming to #578's manifest.
4. CI walks distribute → mutate one submission → grade → correct attribution, plus the adversarial
   identity cases (renamed, missing, duplicate id, id collision).
5. The cohort loop itself is #466's `grade.py`, consumed here, not rebuilt.
6. On failure, each student receives an editor-openable counterexample artifact.

The outcome this issue names is right and strategically well chosen. The artifacts it names are
one level too high, and two of them are already promised elsewhere.
