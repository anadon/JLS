# Issue #719: TASK-C531-2: CrossPlatformScoreParityTest asserts the four adapters produce byte-identical per-student score vectors from the same xUnit input
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

TASK-C531-2, part of feature #531 (FEAT-C21-6, the hermetic four-way parity
fixture) under capstone CAP-21 (#502): write `CrossPlatformScoreParityTest`,
which extracts a normalized-at-source per-student score vector from each of
the Gradescope, GitHub Classroom, PrairieLearn and nbgrader adapters' native
outputs and asserts byte-for-byte equality across all four for the same
300-submission fixture class. Confirmed against the repo: none of this
machinery exists at HEAD (`grep -rli "gradescope\|prairielearn\|nbgrader"`
under `/home/user/JLS` outside `issue-reviews/` returns nothing), and
`docs/batch-interface.md` §1 documents only three exit statuses and no xUnit
output today — so this task, like its whole cluster, is pure scaffolding
plan, not incremental work on existing code.

## Findings, most severe first

**1. (Critical) The two named vector sources this task consumes are ordered
to run *after* the feature this task belongs to — an unresolvable cycle as
filed.** The Boundary section states plainly: *"The vectors themselves come
from each adapter's own corpus task (TASK-C525-2, TASK-C526-1, TASK-C528-2,
TASK-C530-2)."* Resolving those task IDs to real filed issues: TASK-C525-2 =
#697, TASK-C526-1 = #701, TASK-C528-2 = #708, TASK-C530-2 = #715. Checking
each one's own machine block: #697's `ordering_after` is `["TASK-C525-1",
531]`, and #715's is `["TASK-C530-1", 531]` — both list bare `531` as a
prerequisite, i.e. both declare they must run *after feature #531 lands*.
But #719 (this issue) *is* TASK-C531-2, filed with `part_of_feature: 531`,
and its own Boundary makes #697 and #715's output a hard input to this
task's own test. So: #531 cannot be complete without #719 landing (it is
531's task), #719 cannot be written without #697/#715's vectors, and
#697/#715 are ordered to wait for #531. That is a closed cycle across three
concretely identified, currently-open issues — not a hypothetical risk.
**Recommendation:** drop `531` from #697 and #715's `ordering_after` (they
should order after their own feature's first task and #524, as #701 and
#708 already correctly do), or restate #719's Boundary to consume a
snapshot/stub vector rather than the live corpus-test output, and record
which fix was chosen in a REPLAN comment on #531.

**2. (High) The issue's central design principle is already contradicted by
the tasks it depends on.** #719 states: *"Extraction is defined once, in the
fixture, rather than as four per-adapter normalizers that could each paper
over a real divergence."* But every one of the four cited source tasks
already commits, in its own acceptance criteria, to doing exactly that
independently: #697 AC4 — *"emitted score vector is in the shared form...
without per-adapter normalization applied after the fact"*; #701 AC4 —
*"emitted in the shared form #531's parity test consumes, with no post-hoc
normalization"*; #708 AC2 — *"emitted in the shared parity-vector form,
without adapter-specific normalization applied afterwards"*; #715 AC1 —
*"emitted in the shared parity-vector form, with no post-hoc normalization."*
Each of those four extraction steps is written and reviewed independently,
by construction, in four separate issues with no shared code between them
at filing time — which is precisely "four per-adapter normalizers" by
another name. By the time `CrossPlatformScoreParityTest` runs, the
adapter-side normalization this issue warns against has already happened;
a single fixture-side extractor over already-normalized input cannot
recover a divergence four independent normalizers already smoothed away.
**Recommendation:** either #719 defines and ships the single shared
extractor *first*, and #697/#701/#708/#715 are rewritten to call into it
(not to independently emit "the shared form"), or #719's stated principle
is dropped as aspirational and each adapter's own normalizer is added to
this test's own scope for review. As filed, the principle and the
dependency graph cannot both be true.

**3. (Medium-High) Coverage gap: only two of the four cited source tasks
commit to a 300-submission run.** #719's parity test needs, per its own
title, "per-student score vectors" for the fixture *class* across all four
platforms — matching #531/#502's 300-submission corpus. Checking the four
source tasks: #697 (Gradescope) explicitly commits to *"grades all 300
fixture submissions"*; #715 (nbgrader) commits to *"two consecutive
autograde runs of the full fixture class"*. But #701 (Classroom) AC2 reads
*"Pushing a fixture-class submission triggers the Action and produces
Classroom points"* — grammatically singular, one submission, with no AC
anywhere in #701 requiring a 300-submission sweep. #708 (PrairieLearn) AC1
is scoped to *"the fixture lab"* (singular) with AC2 referencing "the
fixture class" only for score-vector *form*, not count. If #701 and #708
ship as currently scoped, #719 has no textual guarantee of receiving 300
rows of Classroom/PrairieLearn data to diff against Gradescope's and
nbgrader's 300 rows — the parity test could only ever compare 1-vs-300,
which is not what AC-1 asserts. **Recommendation:** add an explicit
"grades all 300 fixture submissions" criterion to #701 and #708, matching
#697/#715, before #719 is implementable as written.

**4. (Medium) "Byte-for-byte" over an unspecified vector schema is
gameable in exactly the way the issue's own kill criterion warns against.**
AC-1 requires byte-for-byte equality of a "normalized-at-source per-student
vector," but neither #719 nor any of its four source tasks pins the
vector's schema: field list, numeric type (integer points vs. float
percentage), rounding/precision rule, or student-identifier format. A
normalizer that collapses everything to coarse pass/fail buckets, or
truncates a Gradescope float and a PrairieLearn float to the same integer,
would make this assertion pass while silently discarding the exact
precision divergence KC-21-1 (cited correctly by this issue) is meant to
catch. The perturbation-test criterion ("a deliberately perturbed adapter
output fails the test with a diff naming the student, the test and the two
differing values") only proves the comparator *can* detect an injected
diff at whatever granularity the extractor happens to preserve — it does
not prove the extractor preserves the granularity that matters.
**Recommendation:** pin the vector schema (fields, numeric type, rounding
rule) in this issue's acceptance criteria, not left to the implementer's
discretion inside "normalized-at-source."

**5. (Medium) Foundational dependency (#717/TASK-C531-1) and the entire CLI
contract beneath it are unstarted, and the 1-1.5 mw band covers only this
task's own slice.** #719's own listed prerequisite `TASK-C531-1` (#717 —
the fixture lab, 300-submission corpus and golden vectors) is itself fully
open with no landed work, and #717 in turn orders after #524 (the frozen
CLI contract, itself unbuilt) and #300 (CAP-06). None of the four adapters'
own scaffolding (#694, #697, #701, #706, #708, #713, #715) exists either.
This is consistent with the rest of the CAP-21 cluster's cost pattern
already flagged by sibling reviews (#531, #526) and isn't unique to #719,
but is worth restating here because #719's YAML declares a specific,
narrow-looking cost (`band_mw: 1-1.5`) that a reader landing on this issue
alone could mistake for the real cost of getting to a runnable test.

## What's solid

- The perturbation-test criterion — *"a diff naming the student, the test
  and the two differing values"* — is a genuinely falsifiable, well-specified
  negative test, sharper than most of the surrounding cluster's acceptance
  criteria.
- The REPLAN-on-#502/KC-21-1 citation is accurate: #502's actual kill
  criterion text ("re-scope the outcome... by REPLAN before shipping any
  adapter — do not quietly weaken AC-1") matches what #719 promises to do on
  a genuine divergence, and the commitment not to relax the assertion is a
  real check against silent scope erosion.
- Scoping this issue to "the assertion" only, and explicitly pointing the
  vector-production work back to the four adapters' own corpus tasks, is the
  right decomposition in principle — the problem (finding 1-3) is that the
  edges of that decomposition don't yet agree with each other across issues.

## Verdict rationale

Finding 1 is a concrete, verifiable cycle across three currently-open,
specifically-identified issues (#719, #697, #715) — not a matter of
interpretation. Finding 2 shows the issue's own stated design rationale is
already undercut by the acceptance criteria of every task it depends on.
Either alone would block an implementer from writing this test as specified;
together with the coverage gap (finding 3) and the unpinned vector schema
(finding 4), this needs the dependency graph and the extraction-ownership
question resolved in the issue text — via REPLAN comments on #531/#697/#715,
per this cluster's own stated process — before implementation starts. Nothing
here suggests the underlying goal (a real, falsifiable parity check) is
unsound; the graph as currently filed just doesn't support it yet.
