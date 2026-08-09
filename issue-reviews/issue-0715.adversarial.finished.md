# Issue #715: TASK-C530-2: the nbgrader gradebook export joins the four-way parity vectors, and the unit README runs as CI doc-test steps
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

TASK-C530-2, part of feature #530 (FEAT-C21-5, the nbgrader adapter) under
capstone CAP-21 (#502): emit the nbgrader gradebook export in the shared
per-student parity-vector form #531's `CrossPlatformScoreParityTest`
consumes, and run the nbgrader unit's README as a scripted doc-test lane in
CI. Confirmed against the repo: no file mentions nbgrader, gradebook, or
xUnit anywhere outside `issue-reviews/`, and `docs/batch-interface.md` — the
actual normative batch-interface spec — defines no xUnit schema today
(`ARCHITECTURE.md` names it as the frozen-contract candidate, not the
frozen contract). This is scaffolding-plan work with no existing code to
extend.

## Findings, most severe first

**1. (Critical) #715 is one edge of a closed three-issue dependency cycle,
already identified by this fleet's own review of #719.** #715's machine
block reads `ordering_after: ["TASK-C530-1", 531]` — it declares it must
land *after* feature #531 (FEAT-C21-6). But #719 (TASK-C531-2, `part_of_feature:
531`) states plainly that its `CrossPlatformScoreParityTest` consumes
"each adapter's own corpus task (TASK-C525-2, TASK-C526-1, TASK-C528-2,
**TASK-C530-2**)" — i.e. #715's own output is a hard input to #719, and
#719 is itself the task that completes feature #531. So: #531 cannot be
complete without #719 landing; #719 cannot be written without #715's
vector; and #715 is ordered to wait for #531 to land first. That is a
closed cycle across three concretely identified, currently-open issues
(#531 → #719 → #715 → #531), independently confirmed in
`issue-reviews/issue-0719.adversarial.md` finding 1, which names #715 by
number and quotes this exact `ordering_after` clause. **Recommendation:**
drop bare `531` from #715's `ordering_after` — order after `TASK-C530-1`
and the CLI-contract lineage (#524) instead, matching how #701 (TASK-C526-1)
and #708 (TASK-C528-2) are correctly filed — and record the fix via a
`REPLAN:` comment on #531 per CAP-21 §5's own process.

**2. (High) AC-1's "shared parity-vector form... with no post-hoc
normalization" has no pinned schema, and #715 is one of (at least) four
issues independently claiming to emit it.** Quoting AC-1: *"The gradebook
export for the fixture class is emitted in the shared per-student
score-vector form... with no post-hoc normalization, ready for
`CrossPlatformScoreParityTest`."* Neither #715, its sibling #713
(TASK-C530-1), #530 (FEAT-C21-5), nor #531/#719 (the test that is supposed
to consume this form) specify the vector's field list, numeric type
(integer points vs. float fraction), rounding rule, or student-identifier
format. #697 (Gradescope), #701 (Classroom), #708 (PrairieLearn) each carry
near-identical "shared form, no post-hoc normalization" language with no
shared code between the four at filing time — exactly the "four
per-adapter normalizers" pattern #719 states its own design explicitly
rejects ("Extraction is defined once, in the fixture, rather than as four
per-adapter normalizers that could each paper over a real divergence"). As
filed, AC-1 could be satisfied by an nbgrader export that quietly collapses
floats to coarse points while another adapter keeps full precision — the
test would still pass. **Recommendation:** pin the vector schema once
(ideally in #524's frozen contract or #531/#719's design) and have #715
consume it by reference rather than independently defining "the shared
form."

**3. (Medium-High) AC-3's "no network dependency" claim doesn't address how
the tooling itself gets installed.** nbgrader and its Jupyter dependencies
have zero footprint in this repository today (no `requirements.txt`, no
Python reference in `.devcontainer/Dockerfile`, and README's "Optional
development tools" list is Maven/JDK plus native packages only — no
Python). AC-3 asserts the *grading lane* runs hermetically with no network
dependency, but says nothing about provisioning nbgrader/Jupyter into
whatever CI image runs "the dedicated adapter lane" — that has to come from
somewhere, and as filed it is neither this issue's scope nor any cited
issue's. Left unaddressed, "hermetic, no network dependency" is checkable
only at run time and silently permits a setup step that fetches packages
from PyPI on every CI run. **Recommendation:** either fold toolchain
provisioning into this issue's scope explicitly, or add an ordering
dependency on whatever issue extends the dev/CI container image with a
pinned Python + nbgrader layer.

**4. (Medium) AC-4 asserts a determinism check that #715's own Boundary
disclaims as someone else's job.** The Boundary states: *"the byte-identity
assertion is #531 (TASK-C531-2)"* — yet AC-4 requires exactly that shape of
assertion for the nbgrader adapter alone: *"Two consecutive autograde runs
of the full fixture class produce identical gradebook bytes."* #531's own
AC-4 covers near-identical ground at the four-adapter level: *"Two
consecutive full corpus runs produce identical bytes end to end
(determinism across container boundaries, CAP-21 risk 4)."* As filed, a
reviewer cannot tell whether #715's AC-4 is redundant work already covered
by #531, or a distinct per-adapter check that must exist independently
before #531 can run its own. **Recommendation:** state explicitly in AC-4
whether this is a local, adapter-scoped pre-check distinct from #531's
cross-adapter determinism test, or drop it here and let #531/#719 own it
per the Boundary's own stated split.

**5. (Medium) "the full fixture class" is never quantified.** #531 and
CAP-21 AC-1 both define the corpus as "the same 300-submission fixture
class"; #715's AC-1 and AC-4 say only "the fixture class" / "the full
fixture class" with no number and no cross-reference to #531 for the
count. By contrast #697 (TASK-C525-2, per the #719 review) explicitly
commits to "grades all 300 fixture submissions." As filed, an implementer
could satisfy #715's ACs by grading a handful of fixture submissions and
calling it "the full fixture class." **Recommendation:** state "300 (per
#531)" explicitly in AC-1 and AC-4.

## What's solid

- The two sibling tasks agree with each other: #715's Boundary names
  TASK-C530-1 (#713) as owning "the grading cells," and #713's own
  Boundary reciprocally names TASK-C530-2 for "parity vector emission and
  the CI doc-test lane" — a consistent split, unlike some clusters in this
  program.
- AC-2's citation of "CAP-21 AC-5" checks out verbatim: #502 §4 AC-5
  (`TemplateDocTest`) reads *"Each template README executes end-to-end as
  scripted steps in CI, from zero to a graded assignment. Spans
  PF-2..PF-5"* — an accurate quote, not a paraphrase drift.
- No licensing/security hazard from adding nbgrader: it runs at CI/dev time
  only, never links into the shipped jar, matching the project's existing
  subprocess-boundary pattern for external tools (iverilog/ghdl/yosys,
  ARCHITECTURE.md's plugin-trust-boundary decision) rather than in-process
  linking.
- The KC-21-2 citation (recording-not-session) is faithful: #498 §7.2 is a
  real, currently-open document that states exactly this discipline.

## Verdict rationale

Finding 1 is a concrete, verifiable three-issue cycle, not a matter of
interpretation — #715 cannot be implemented as ordered without either
#715 or #531 being restated first. Finding 2 shows the central acceptance
criterion is gameable as written, independent of the cycle. Findings 3-5
are real gaps (an unaddressed toolchain-provisioning dependency, an
ownership ambiguity with #531 over a determinism check, and an
unquantified corpus size) that would let an implementer land something
that technically satisfies the text without delivering the parity
guarantee CAP-21 actually needs. None of this suggests the underlying
goal — an nbgrader adapter that grades from recorded batch artifacts and
plugs cleanly into the four-way parity fixture — is unsound; the issue
needs its dependency graph and acceptance criteria tightened, via REPLAN
on #531 and cross-issue edits, before implementation starts.
