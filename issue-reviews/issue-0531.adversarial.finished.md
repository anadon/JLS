# Issue #531: FEAT-C21-6: one lab, a 300-submission corpus and golden score vectors prove all four adapters byte-identical in CI — hermetic, containerized, no platform account required
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

This is FEAT-C21-6 (PF-6 of capstone CAP-21, #502): a hermetic, containerized CI
fixture — one lab in the CAP-06 lab-as-data format, a 300-submission corpus, and
golden per-student score vectors — that proves the Gradescope, GitHub Classroom
Action, PrairieLearn and nbgrader adapters (#525/#526/#528/#530, none of which
exist in-tree yet) produce byte-identical scores, with no platform account and no
network dependency. Confirmed against the repo: `grep -rli
"gradescope\|prairielearn\|nbgrader"` returns nothing under `/home/user/JLS`
outside `issue-reviews/`, and README.md/ARCHITECTURE.md describe JLS purely as an
editor/simulator with a headless batch CLI — no LMS or autograder-platform
integration exists today. #502's own body confirms this ("no file in the tree
mentions Gradescope, PrairieLearn, nbgrader or GitHub Classroom").

## Findings, most severe first

**1. AC-1 as written is untestable / impossible on its literal text.** The
checklist item reads: *"identical per-student score vectors, byte for byte,
across Gradescope results.json, the Action's summary, PrairieLearn results, and
the nbgrader gradebook export."* These are four structurally different formats —
JSON, a text/markdown summary, a PrairieLearn results payload, and a Jupyter
gradebook export (`.ipynb`/CSV-derived). Comparing their raw bytes is
definitionally impossible; only an extracted, normalized score vector can be
compared. That normalization step — *"extracts a normalized-at-source per-student
vector from each of the four adapters' native outputs"* — exists only in the
child task #719 (`TASK-C531-2`), not in this issue. As #531 is currently worded,
an implementer has no textual basis in this issue for what "byte for byte" is
actually being measured over, and a literal reading invites either an impossible
requirement or a gamed one (e.g. wrapping all four outputs in an identical
envelope regardless of what they actually score). **Recommendation:** pull
#719's "normalized-at-source per-student vector" language into this issue's AC-1
directly; don't leave the only precise definition one hop away in a child issue.

**2. A correction recorded in this issue's own comment thread was never applied
to its own acceptance criteria.** The 2026-08-08 comment ("REVISION...") states
plainly: *"#724 (TASK-C531-4) must consume #524's seeded violation, not seed its
own... One seeded violation exists; #724 asserts when it is evaluated, not that
it exists."* Yet #531's checklist item (unchanged) still reads: *"The fixture
hosts the conformance ordering: a seeded PF-1 contract violation fails the build
before any adapter test executes"* — ambiguous between "hosts a violation" and
"asserts ordering over #524's violation." Worse, #724 itself (fetched directly)
still says in its own AC: *"A seeded contract violation causes the build to fail
before any adapter test runs"* — phrasing indistinguishable from seeding its own.
The written resolution and the two issues' actual current text disagree with each
other. **Recommendation:** edit #531's AC-3 and #724's AC-1 to say explicitly
"consumes #524's seeded violation" (not "a seeded violation exists here"),
closing the gap the revision comment itself identified but didn't fix.

**3. The feature-level dependency declaration understates the real dependency
graph.** #531's YAML front matter declares `ordering_after: ["FEAT-C21-1"]`
(i.e. #524) only. But AC-1 requires running "against every adapter," i.e.
#525/#526/#528/#530 must exist first. The task-level breakdown gets this right —
#719's front matter correctly lists `ordering_after: ["TASK-C531-1", 525, 526,
528, 530]` — but the parent feature issue does not. A maintainer or scheduler
reading only #531's metadata (which is what the `ordering_after`/`serves_capstones`
convention in this repo's issue graph is for) would conclude PF-6 is startable as
soon as #524 lands, when in fact three-quarters of its real prerequisites are
absent from that field. **Recommendation:** add 525, 526, 528, 530 to #531's
`ordering_after`, matching what #719 already asserts is true.

**4. Cost/value mismatch, and no cited demand.** #502 (the parent capstone) calls
this fixture *"the hard part and the lasting asset"* of the whole CAP-21 effort,
yet #531 budgets it at only 3-4 mw against the capstone's 12-17 mw total — cheap
for "the hard part." More importantly, no instructor, course, or user is cited
anywhere in #502 or #531 as having asked for Gradescope/PrairieLearn/nbgrader/
Classroom support; the "Intended Audience & Impact" section is aspirational
prose, not a request. This is in real tension with a recorded project norm one
document over: ARCHITECTURE.md's i18n non-goal explicitly treats "no requesting
user" as sufficient reason to decline comparable-scale scope ("a large, ongoing
tax with no requesting user... PRs adding partial i18n scaffolding will be
declined"). The same bar is not applied to a four-platform integration-testing
apparatus for a single-maintainer pedagogy tool. **Recommendation:** either cite
concrete instructor/course demand somewhere in the CAP-21 cluster, or apply the
same "no requesting user" discipline used elsewhere in this repo before this
scale of investment is authorized.

**5. Hermeticity forecloses the only real correctness check.** The AC explicitly
forbids any platform account or network dependency, so the fixture can only ever
be validated against its own model of each platform's *documented* contract
(acknowledged in #502 risk 5 / KC-21-3), never against the live service. #502
anticipates vendor drift in the abstract but neither #502 nor #531 defines a
periodic re-validation trigger (e.g., a scheduled check against a real Gradescope
sandbox). A documented contract can silently diverge from the real platform
indefinitely while this CI lane stays green — the fixture proves internal
self-consistency, not platform fidelity. **Recommendation:** add an explicit,
even if infrequent, out-of-hermetic-loop revalidation step (or at minimum a
documented manual trigger tied to each platform's changelog) as part of PF-1/PF-6,
not left as an unstated gap.

**6. Unresolved "300 submissions" ambiguity, carried over from this issue's own
comment thread.** The first comment (pass 2) flags that this corpus, CAP-06's
originating corpus (#300), and CAP-25's corpus (#506, "one batch invocation over
the same 300 submissions flags every planted copied pair") are three separate
"300-submission" artifacts with no stated relationship. This is noted as
unresolved by the issue's own review process, not something this review is
introducing — but it remains open as of the issue's last update and should block
work on this fixture's corpus until resolved, since building it twice (or
building an inconsistent third one) is wasted effort either way.

**7. Solid, no action needed.** KC-21-1 is a well-built kill criterion — it
pre-commits to a REPLAN on #502 re-scoping AC-1 to "same verdicts, platform-native
presentation" if byte-identity proves infeasible, rather than allowing AC-1 to be
quietly weakened. That is exactly the discipline an adversarial reviewer wants to
see in a feature this speculative.

**8. Solid, no action needed.** Decomposition into scoped child tasks (#719 for
the parity assertion, #724 for ordering/determinism, and by naming convention
presumably #722/#723 for the rest of TASK-C531-*) keeps this feature issue from
being a monolith, and each child task carries its own falsifiable acceptance
criteria rather than restating the parent's prose.

## Verdict rationale

Findings 1-3 are concrete, checkable defects in the issue text itself (an
untestable AC as literally written, a fixed-but-unapplied internal contradiction
with a sibling issue, and a dependency graph that disagrees with its own child
task). Findings 4-5 are feasibility/governance concerns that don't block drafting
work but should be resolved before real engineering time is spent. None of this
rises to should-not-proceed — the surrounding process (kill criteria, task
decomposition, an active review thread already catching real problems) is
functioning — but the issue should not be picked up for implementation until
findings 1-3 are fixed in the issue text itself, not just acknowledged in
comments.
