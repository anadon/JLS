# Issue #724: TASK-C531-4: a seeded CLI-contract violation fails the build before any adapter test runs, and two full corpus runs are byte-identical end to end
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#724 is a `tier:task` issue, child of FEAT-C21-6 (#531), itself part of the CAP-21
(#502) "one autograding kit, byte-identical scores on Gradescope/Classroom/
PrairieLearn/nbgrader" capstone. Its stated job is narrow and reasonable in
concept — order the CLI-conformance gate ahead of the four adapter lanes, and
prove two full corpus runs are byte-identical — but the issue as filed fails the
repo's own bar for what a task issue must contain, and its one comment reveals
that the acceptance criteria were already found broken once and only
partially fixed.

## Findings, most severe first

**1. [HIGH] The issue does not conform to `.github/ISSUE_TEMPLATE/scientific_task.md`, the template its own `tier:task` label commits it to.**
The template (v6, dated 2026-08 — the same month this issue was filed,
2026-08-04) mandates a machine block with `tier:`, `evidence_commit:`,
`part_of_feature:`, `blocked_by: []`, `blocks: []`, `related: []`, plus
Abstract, Intended Audience & Impact, Observations (numbered, file:line-cited,
rule 1), a falsifiable Hypothesis, Predictions ("do X, observe Y", rule 3), a
12-subsection Interface & Data Contract, Method, Falsification Criteria, and a
14-item Completion Criteria checklist. #724's actual body is three headings —
`## Outcome`, `## Acceptance criteria`, `## Boundary` — plus a YAML header
using fields (`task_id`, `part_of_feature`, `band_mw`, `ordering_after`) that
do not match the template's schema at all: there is no `blocked_by`/`blocks`
array (the mandated ordering-edge fields), no `evidence_commit`, and no
`tier:` key. Sibling issues #531 and #524 have the identical problem but are
at least `tier:feature`, where the analogous mismatch against
`feature.md` is just as total. Zero code citations, zero commit pin, despite
making concrete engineering claims about build ordering and byte-determinism.
**Recommendation:** either re-file #724 against the current template, or if
the short `Outcome/Acceptance criteria/Boundary` format is an intentionally
retained legacy shape for this issue family, say so explicitly in the issue
(or retire the mismatched template) so a reader isn't left wondering which
contract governs it.

**2. [HIGH] The one comment substantially rewrites the acceptance criteria, but the issue body's checkboxes were never edited to match.**
The 2026-08-08 comment opens "**do not seed a second one**" and states a
"pass-2 boundary review found the two criteria are the same one written
twice" (#724's original AC-1 duplicating #524's AC-2), then gives a
"**Restated acceptance shape**" with four new bullets that materially narrow
scope (from "a seeded violation... and the falsification transcript is
recorded" to "the conformance check runs, and fails, before any of the four
adapter lanes... asserted on lane ordering, not observed by inspection of a
log"). The issue body's `## Acceptance criteria` section is untouched — it
still contains the original, now-disavowed duplicate item as an unchecked
box. Both the scientific_task and feature templates require exactly this
situation to be handled by editing the body together with an `AMENDED:` /
`REPLAN:` comment (rule 9 / rule C) precisely so "a silent edit is invisible
to executors, who reconstruct state from the machine block plus the prefixed
comments" — here the comment exists but the edit doesn't, which is the
inverse failure mode and just as dangerous: an executor (or another LLM
agent) working strictly off the rendered issue body will re-seed the
duplicate CLI-contract violation the comment explicitly forbids.
**Recommendation:** edit the body's Acceptance criteria section to the
restated shape now, or strike through the superseded bullet with a pointer
to the comment.

**3. [HIGH] The comment's claim about a dependency-graph edit on a sibling issue does not check out against that issue's actual current body.**
The comment asserts: "#524's `ordering_after` has been corrected from
`[369, 466]` to `[466]`." Fetching #524 (FEAT-C21-1) directly shows its
machine block still reads `ordering_after: [369, 466]   # FEAT-053 /
TASK-0111 (CAP-06 lineage) build the verdict half of the contract this issue
freezes; this edge is recorded on both ends per CAP-21 §5` — i.e. unchanged,
still naming #369. (#466's own `blocked_by: []` is indeed empty, so that half
of the claim holds.) Either the correction to #524 was never actually applied,
or it exists somewhere other than #524's canonical body — in which case the
project's own amendment rule is being violated a second way: a claimed
correction to issue X should be recorded as a comment *on X* (and mirrored
per protocol), not asserted only in a comment on a downstream issue Y.
Consequence for #724: its own conclusion — "this task's transitive
prerequisite chain terminates at #466" — currently does not hold as written,
since #531 (#724's parent) is `ordering_after: [FEAT-C21-1]` = #524, and #524
still declares itself ordered after both #369 and #466.
**Recommendation:** verify #524's true state before relying on the
"terminates at #466" claim; if the correction is intended, post it as an
`AMENDED:`/`REPLAN:` comment on #524 itself and only then restate the
downstream consequence on #724.

**4. [MEDIUM] The acceptance criteria are not written as observable predictions and are gameable.**
- "an adapter lane cannot be scheduled ahead of the conformance gate by
  reordering a workflow file" — names no mechanism (GitHub Actions `needs:`,
  a topology-checking meta-test, a linter) and no test. As written this could
  be "satisfied" by simply keeping everything in one linear job today and
  regressing silently the day someone adds a second workflow file or a matrix
  — there is no named regression test that would catch that.
- "the falsification transcript is recorded" — recorded where, in what
  format, asserted by what test? Contrast with #524's own AC-2, which at
  least names the artifact category ("falsification transcript... recorded");
  #724 repeats the phrase without adding the "structurally enforced" test
  that would make it checkable rather than aspirational.
- Compare to the fourth AC ("names the first differing artifact and the axis
  that moved") which *is* concrete and testable — worth keeping as the model
  for how the other three should read.

**5. [MEDIUM] The task's own dependency list omits the one thing its premise requires to exist.**
The whole point of "an adapter lane cannot be scheduled ahead of the
conformance gate" presupposes the four adapter lanes are already real CI
jobs. #724's `ordering_after` lists only `TASK-C531-3` (#721, confirmed open:
"the whole four-way fixture runs containerized in CI...") and `TASK-C524-2`
(#687, confirmed open: the CLI-contract ratchet/seeded-violation task) —
neither of the ordering_after entries is any of the four adapter
feature/task issues (#525 Gradescope, #526 Classroom, #528 PrairieLearn, #530
nbgrader, and their TASK-C52x-* children, all confirmed open). If #724 lands
before those, "the ordering guarantee" has nothing real to order yet, and the
acceptance criteria ("evaluated before any of the four adapter lanes...
starts") cannot actually be exercised — only simulated with stub lanes, which
is a materially weaker claim than the issue implies it is proving.
**Recommendation:** either add the four adapter issues (or their landing) as
an explicit dependency, or state plainly in Boundary that this task ships
against stub/placeholder lanes and re-verification against the real four is
a follow-up.

**6. [MEDIUM] Cost is unsized and not obviously covered by the stated `band_mw: 0.5-1`.**
"Two consecutive full 300-submission corpus runs, in freshly created
containers, produce identical bytes end to end" implies, once the four real
adapters exist, on the order of 300 submissions × 2 runs × up to 4 platform
pipelines × fresh-container overhead, on every invocation of this lane. A
half-to-one maintainer-week plausibly covers writing the ordering assertion
and the diff-diagnostic logic, but nothing in the issue prices or bounds the
recurring CI-minutes cost this fixture imposes going forward (PR-gating vs.
nightly-only is left unstated), which is exactly the kind of unbounded
ongoing cost `CONTRIBUTING.md`/CI-hygiene norms elsewhere in this repo (e.g.
the #101 20-consecutive-run promotion bar, the `ui-interaction` quarantine
policy) are careful to schedule deliberately (nightly cron, not push-gating)
rather than leave implicit.

**7. [LOW] Corpus provenance is silently inherited, not restated.**
#531's own Boundary note says "The 300-submission corpus concept originates
in CAP-06 (#300); this fixture consumes the lab-as-data format, it does not
re-own verdict content" — but #724 (which actually runs the corpus through
containers and gates the build on it) never restates that boundary or points
at #300. A reader of #724 in isolation cannot tell whether the 300
submissions are synthetic fixtures or something that could carry real
student data; that provenance question matters more, not less, once this
task starts running the corpus in "freshly created containers" as a public
CI gate.

**8. [LOW] Cross-references use project-internal task IDs, not GitHub issue links.**
`TASK-C531-3` and `TASK-C524-2` are not clickable and don't self-resolve;
they only resolve to #721 and #687 respectively via a full-text title search
(confirmed in this review). A reader following the issue as rendered on
GitHub has no direct path from the ordering_after list to the actual blocking
issues.

## What's solid

- The core idea — a fixture-scale gate proving (a) conformance ordering and
  (b) end-to-end determinism, ahead of committing to the more expensive
  four-adapter parity work in #531/#525/#526/#528/#530 — is a sensible,
  cheap-to-fail-fast slice, and the `ordering_after` entries that are named
  (#721, #687) are real, open, and genuinely relevant prerequisites.
- The fourth acceptance criterion (naming the first differing artifact and
  the axis that moved: rerun/container/locale/host) is concrete and
  observable, and is the one criterion in the issue that would actually
  survive a "could this pass while the real goal fails?" test.
- The Boundary section's scope discipline — explicitly deferring ratchet
  mechanics to TASK-C524-2 and envelope determinism to "TASK-C524-4" — is the
  right instinct (atomic scope, rule 4), even though the issue's own body
  format doesn't give it the machine-readable teeth (`blocked_by`/`related`)
  the template would.
