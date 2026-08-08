# Issue #759: TASK-C576-3: CI walks distribute → mutate one submission → grade → attribute, and a malformed or missing submission is a named result rather than an aborted cohort run
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue as filed

TASK-C576-3 (band 0.5-1 mw, `ordering_after: ["TASK-C576-2"]`) is the third of
three tasks under feature #576 (FEAT-C33-2, "handing out an assignment and
grading the cohort is one documented path"). It asks for an end-to-end CI lane
(distribute → mutate one submission → grade → per-student verdicts) plus
robustness guarantees for malformed, missing, renamed, and unknown-student
submissions, with a cohort-denominator invariant. Sibling tasks: #755
(TASK-C576-1, directory-layout spec) and #757 (TASK-C576-2, the grading
command). Neither has landed; nothing under this feature exists in the tree
yet (`grep -rn "cohort|roster|distribute" src/ test/` returns nothing outside
this issue's own text and unrelated `collab` peer-roster / planning-roster
hits).

## Findings, most severe first

**1. (High) The AC requiring "the cohort denominator equals the expected
roster size" presupposes an artifact — a class roster — that is defined
nowhere in this issue, in #576, or in #755's own acceptance criteria.**
Evidence: #755's AC list is "Both layouts are specified... including how a
submission identifies its student" and "what is stable and what an instructor
may vary" — a *submission* naming convention, not a committed list of
*expected* students against which a denominator could be checked. A
repo-wide search for "roster" in a course-roster sense turns up nothing
(`docs/collaborative-editing-research.md`'s "roster" is the collab peer list;
`.github/ISSUE_TEMPLATE/feature.md`'s "roster" is this project's own
issue-planning roster — both unrelated concepts). Without a specified roster
file/manifest, "expected roster size" has no operational definition, and the
AC cannot be implemented or tested as written. This also creates a boundary
conflict: if a roster artifact is needed, it is a *layout* concern that
belongs in #755 per this issue's own Boundary line ("layouts... are
TASK-C576-1"), yet #755 does not claim it. Recommendation: either add a
roster-manifest requirement to #755 first and have #759 depend on it, or
replace "expected roster size" with something #759 can actually observe (e.g.
"count of directories present under the submission layout at CI-lane start
time"), which sidesteps the undefined-roster problem entirely.

**2. (High) The 2026-08-08 ordering correction on #576 was never propagated
to this issue, and #759 has no acceptance criterion for the degraded-mode
contingency that governs its own core AC.** #576's second comment
(`ordering_after_by_criterion`) explicitly re-derives that feature AC-3 —
verbatim the same criterion as this task's AC-1 ("planted failure... caught
and attributed to the correct student") — requires **#466** (the actual
verdict/report engine), plus #755 and #757. That engine does not exist in the
tree: #369's own evidence section runs `git grep -l "Expectations\|class
Assert\|class Cover" 2d0ca9d -- src/` and gets no hits, and #369 is itself
`blocked_by: [316, 321, 347]` (SimpleEditor decomposition, Yosys JSON writer,
parity harness) — none of which are close to landing. The correction comment's
own "Mirrors" section lists only #755, #369 and #300 as notified — **#757 and
#759 are not mentioned**, even though #759 is the task that literally
implements feature AC-3. #759's machine block (`ordering_after:
["TASK-C576-2"]`) is technically not wrong (it transitively inherits #757's
dependency on #466), but #759 carries none of #576's or #757's explicit
degraded-mode escape hatch ("if the CAP-06 verdict slice has not landed, ships
against today's three-exit-status contract... and the docs say plainly which
form is in effect"). A contributor who opens only #759 has no way to know
whether "attribution" means a structured per-student report or a bare
exit-code/filename match under the fallback. Recommendation: mirror the
ordering-correction comment onto #757 and #759, and add an AC to #759
explicit about which grading-command contract (full #466 verdict vs. degraded
three-status) the CI lane is required to exercise.

**3. (Medium) "Attributed to the correct student" is gameable under the very
fallback path #576/#757 explicitly permit.** Today's batch contract has three
exit statuses and, per `docs/batch-interface.md:36-40` and #369's own
evidence, none of them means "ran and the answer was wrong" — comparison
happens outside the tool via string diff (`examples/autograde/autograde.py`'s
`EXPECTED_STDOUT_LINES`, three literal lines). If #757 ships in its permitted
degraded form, the only "attribution" mechanically available to a CI lane is
matching a mutated submission's file/directory name to itself in a "failed"
list — which a test can satisfy by mutating `alice/*.jls` and grepping
"alice" out of stderr, without exercising any real per-student isolation,
counterexample content, or cross-contamination check (e.g., that mutating
student A's file does not also flip student B's verdict). As written, the AC
does not state a minimum evidentiary bar for "attribution," so a technically
passing CI lane could still leave the actually-interesting failure mode
(misattribution under concurrent/batched grading) unverified. Recommendation:
specify what the per-student result must minimally carry (failing test name,
or at minimum a status distinct from every other student's), independent of
which grading-command version is in effect.

**4. (Medium) Feasibility/cost: the band (0.5-1 mw) looks priced for "wire a
CI script," not for the five separable, individually-fixtured scenarios
actually requested** — planted-mutation attribution, malformed submission,
missing submission, renamed submission, and unknown-student submission — each
needing its own test fixture and expected-output assertion, on top of a CI
lane that cannot be written concretely until both #755 (layout) and #757
(grading command) land, and #757 itself is priced at 1-1.5 mw and is gated on
an engine several features deep in a blocked chain (finding 2). A 0.5-1 mw
estimate for the *downstream* integration-and-robustness task, sitting behind
a longer critical path than its own estimate, is optimistic and worth
revisiting once #755/#757's actual command surface is known.

**5. (Medium) "Distribute" is treated as an exercised action, but no task in
this family builds a distribute command.** The Outcome text says "starters
are distributed" and AC-1 has the CI lane "walk distribute → mutate → grade,"
which reads as if there is a product behavior named "distribute" to invoke.
But #576's own abstract calls this "a starter-file distribution layout they
hand to students" (a directory convention) and #755's scope is explicitly
"Conventions only" with "No hosted service, account or server... required or
implied anywhere in the layout" — i.e. there is no distribute *tool*, only a
layout a human copies. If the CI lane's "distribute" step is really just
fixture setup (copying a template into N directories) inside the test itself
rather than product code under test, the AC should say so plainly; as
written it risks an implementer either inventing an out-of-scope distribute
command (scope creep against #576's explicit "no server" stance) or a
reviewer being unable to tell whether the "distribute" leg of the CI lane is
testing anything at all.

**6. (Low) Cohort-size figure is an unexplained, unsourced constant.** "one
broken upload must not cost the other 199 students their grades" implies a
200-student cohort, while sibling capstone #300's abstract cites "300 student
submissions" / "a course of 300," and #576 fixes no number at all. Cosmetic on
its own, but in a project whose other planning documents pin every figure to
an evidence commit (see #369's "Evidence" section), an unexplained round
number here invites exactly the "is there a fixed roster size somewhere"
question raised in finding 1.

**7. (Low) No file/anchor references.** Unlike sibling planning documents in
this repo (#369 cites exact line numbers throughout), #759 names no target
CI workflow file, no test directory, no class to extend. For a task-tier issue
meant to be picked up directly this is a minor but real gap versus this
project's own citation norms — low severity because #755/#757 have not yet
fixed the surface this task would anchor to.

## What is solid

- The core intent — one bad submission must not abort or zero out the other
  199 students' grades — is a genuinely important robustness requirement and
  is stated clearly and testably in principle (finding 3 aside).
- The Boundary section correctly delegates layout definition to #755 and the
  grading command itself to #757, keeping this task's scope to CI wiring and
  robustness, which is the right cut.
- AC numbering (AC-3, AC-4) is consistent with the parent feature #576's own
  AC-3/AC-4, so the mapping back to the feature is unambiguous — the specific
  content mismatch is in finding 2, not in the cross-referencing itself.

## Bottom line

The robustness goal is sound and the task boundary is well drawn, but the
issue leans on two artifacts that do not exist and are not specified
anywhere reachable from it: a class-roster manifest (finding 1) and a
resolved verdict-engine dependency whose own project just corrected its
ordering graph five days after this issue was filed without telling this
issue (finding 2). Both should be resolved — either by adding the missing
spec to #755/#757 or by rewriting #759's ACs to not require them — before
implementation starts, or the CI lane that gets built will either be
untestable as specified (roster size) or trivially gameable under the
permitted degraded mode (attribution).
