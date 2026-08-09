# Issue #705: TASK-C526-3: the Classroom starter repo template ships, its README runs as CI doc-test steps, and runner-image drift turns the adapter lane red
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

TASK-C526-3 is the third of three tasks decomposing FEAT-C21-3 (#526, PF-3 of
capstone CAP-21 / #502): commit an in-tree GitHub Classroom starter-repo
template (layout, workflow wiring the `jls-grade` Action, starter circuit)
whose README executes as CI doc-tests in a dedicated lane, and use that same
lane to surface GitHub Actions runner-image drift as a red build rather than
a live course failure. `ordering_after: ["TASK-C526-1"]` (#701, the Action
itself; confirmed live). Sibling task #703 (TASK-C526-2, annotations) exists
but is correctly out of `ordering_after` since annotations aren't required
for a bare graded run. Nothing in the tree mentions GitHub Classroom today
(`grep -rli classroom .` outside `issue-reviews/` is empty) — greenfield.

## Findings, most severe first

**1. (High) The digest-pin/cache design this task depends on (#701) is in
tension with the drift premise this task is built on, and the issue never
names the surviving mechanism.** #701's own outcome text is explicit: "The
build is pinned by digest and cached, **so a run is both reproducible**."
If the JLS payload the Action runs is pinned by digest and cached, the one
thing most likely to make a grading run non-reproducible — which JLS build
actually executes — is deliberately engineered out. Yet #705's title and
AC-3 assert that "GitHub Actions runner-image drift... turns the adapter
lane red," treating drift as a live, expected failure mode for the same
pinned artifact. The in-tree precedent for "runner image drift" that #705's
language is clearly borrowing from — `.github/workflows/repro-installers.yml`
line 19, "the monthly run catches toolchain drift (runner image updates
swapping dpkg/WiX/hdiutil versions underneath us)" — is drift in *native,
non-containerized* OS packaging tools that installer builds invoke directly.
A digest-pinned `jls-grade` Action most naturally ships as a Docker
container action (matching #701's "pins... by digest"), which is insulated
from exactly that class of native-tool drift; what's left exposed is a much
narrower surface (the container runtime/cgroup version on the runner,
`actions/checkout`/`actions/cache` compatibility, YAML syntax changes) that
#705 never names. As written, an implementer cannot tell whether "runner
drift" here means the same failure mode as the installer probe or something
else entirely.
**Recommendation:** name the actual mechanism — e.g. "the container runtime
or `actions/*` pinned-action versions on `ubuntu-latest` change under the
Action's own scaffolding" — and state explicitly that the pinned JLS payload
itself is not the thing expected to drift.

**2. (High) AC-3's three-way failure classification names no verification
mechanism and is gameable as written.** AC-3: "its failure output
distinguishes runner-image drift from an adapter or contract fault." This
asks the lane to correctly attribute root cause across three distinct
failure domains (GitHub's hosted image moving, a bug in this adapter's own
logic, #524's CLI contract changing underneath it) but states no test that
checks the *correctness* of that attribution — only that the output
"distinguishes" them. An implementer can satisfy the letter of this AC by
printing three different string templates selected by which of three
hardcoded probes happened to fail, without ever proving the classification
is accurate on a case where two failure modes could plausibly produce the
same symptom (e.g., a runner image update that also happens to coincide
with a contract-breaking JLS release). This is the identical class of gap
already flagged in this same cluster's sibling task, #710 (TASK-C528-3)
finding 4, for the structurally identical PrairieLearn-lane AC.
**Recommendation:** name concrete seeded-fault fixtures — one per failure
domain — that the lane must correctly attribute, mirroring #524 AC-2's
seeded-violation-transcript requirement.

**3. (Medium-High) Two independent digest-pinning mechanisms are implied
with no stated reconciliation.** #701 AC-1 requires the Action itself to
"pin... by digest, and cache it." #705 AC-4 separately requires "the
template records which JLS build digest it pins and how an instructor
updates it." It is not stated whether AC-4 is the Action's own pin restated
in the template for human legibility (redundant but harmless) or a second,
independent pin point in the workflow YAML that could drift out of sync
with whatever #701's Action resolves internally at run time — a consistency
hazard of its own. This is the same duplicate-ownership shape #710 finding
6 flagged between #706 and #710 for the PrairieLearn image/lane pair.
**Recommendation:** state explicitly that the template's recorded digest
*is* the value #701's Action reads, not a second independently-maintained
copy.

**4. (Medium) "Reports Classroom points" and "grades itself on push with no
manual wiring" assume a platform integration contract that is never named.**
GitHub Classroom's own autograding UI reads scores through a specific
mechanism (the `education/autograding` family of reporter actions, or the
Classroom API) — a bespoke `jls-grade` Action does not automatically surface
"Classroom points" in the Classroom gradebook UI merely by computing a
score; it must either shell out to Classroom's documented reporter contract
or replicate its exact output shape. Neither #701 nor #705 names this
mechanism anywhere. Separately, "no manual wiring" (AC-1) is stated as an
absolute, but posting Checks-API annotations and even reporting scores from
a generated repo depends on the org's default `GITHUB_TOKEN` workflow
permissions, which vary by GitHub Classroom organization configuration —
already flagged as a real gap in the parent feature (#526 finding 5) and
never addressed at the task level where it would actually need to be tested.
**Recommendation:** name the actual Classroom scoring contract the Action
must conform to, and add "no manual wiring" as conditioned on the
org's default `GITHUB_TOKEN` permissions being sufficient — with a
documented fallback for when they aren't.

**5. (Medium) The fan-out problem of updating an already-generated cohort's
pinned digest is not addressed.** AC-4 requires the template to record "how
an instructor updates it" (the digest) — but GitHub Classroom generates one
independent repo per student at assignment-creation time; editing the
*template* after that point does not propagate to repos already generated
from it. If a bug fix ships a new pinned digest mid-semester, AC-4's
"how an instructor updates it" reads as a template-level operation, not a
live-cohort one, and the issue never says whether "updates it" means
re-editing N already-generated student repos, re-issuing the assignment, or
something else GitHub Classroom actually supports.
**Recommendation:** state which of these AC-4's "how an instructor updates
it" actually refers to, since the two have very different operational cost
for a course already in progress.

**6. (Medium) Real dependency depth is undisclosed, consistent with the rest
of this cluster.** `ordering_after: ["TASK-C526-1"]` names only the direct
predecessor. The real chain is #524 (FEAT-C21-1, open, itself
`ordering_after: [369, 466]`, both open) → #701 (TASK-C526-1, open) → #705.
`band_mw: 0.5-1` carries no caveat that this figure is marginal-only,
contingent on #701 and transitively #524/#369/#466 landing first — the same
defect already flagged in #526 finding 4, #706 finding 2, and #710 finding 5
for every other member of this feature cluster.
**Recommendation:** one line noting the estimate is marginal-only, matching
the discipline #369/#466 already apply to themselves.

**7. (Low) The "lab's starter circuit" named in the outcome text has no
corresponding acceptance criterion.** The outcome promises the template
ships "the lab's starter circuit," but none of the four ACs mention its
content, format, or relationship to #701's "lab's hidden vectors." It's
plausibly implicit (the starter circuit is whatever the fixture-class
submission in #701 AC-2 targets) but never stated.
**Recommendation:** either fold a one-line criterion pinning the starter
circuit's provenance (same fixture as #701 consumes) or drop the phrase
from the outcome text.

**8. (Low) AC-2's "doc-tests... zero to a graded assignment" walk-through
could plausibly be read as including annotations (CAP-21 §1 step 2 bundles
grading + annotations + points in one observation), but #705's own boundary
correctly assigns annotations to #703.** Worth one clarifying line that the
doc-tested happy path stops at points, not annotated failures, so a reader
comparing #705 against CAP-21 §1 doesn't expect annotation output from this
lane alone.
**Recommendation:** add a clause to AC-2 or the boundary noting the
doc-tested walk-through excludes annotation output by design.

## What's solid

- CAP-21 AC-5 citation checks out verbatim against #502 ("Each template
  README executes end-to-end as scripted steps in CI, from zero to a graded
  assignment. Spans PF-2..PF-5").
- CAP-21 risk 1 citation checks out verbatim against #502 ("The adapters
  must live in dedicated CI lanes, not the core matrix").
- The Marketplace-publication deferral matches #502 Open Question 3 exactly
  ("Blocks PF-3's shipping, not its development") — a clean, correctly-scoped
  non-blocker.
- Task boundary is clean against its two siblings: grading (#701) and
  annotations (#703) are neither duplicated nor silently absorbed here.
- The underlying "runner-image drift is a real CI risk" premise is not
  fabricated — `repro-installers.yml` already treats it as a recognized
  hazard class in this repo, just for a different (native-tool) mechanism
  than the containerized Action this task would actually be gating (finding 1).

## Verdict rationale

The task-boundary discipline and capstone citations are accurate, and the
premise that runner-image drift deserves its own lane has real precedent in
this codebase. But the central claim in the title and AC-3 — that drift in
the runner image is what turns this lane red — sits in unexamined tension
with the sibling task's explicit digest-pin-for-reproducibility design, the
three-way failure classification names no verification mechanism and is
gameable exactly as the structurally identical PrairieLearn task (#710) was
already flagged for, and the Classroom scoring-contract and fan-out-update
gaps are the kind of thing that surfaces as "the Action ran green in CI but
students see no grade" bug reports in the field. **needs-rework.**
