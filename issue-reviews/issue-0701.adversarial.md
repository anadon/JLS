# Issue #701: TASK-C526-1: the jls-grade Action runs a pinned, cached JLS build against a lab's hidden vectors and reports Classroom points
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C526-1 asks for a `jls-grade` GitHub Action that grades a student's
pushed circuit against hidden vectors "through the frozen CLI contract" and
reports GitHub Classroom points. The scope cut (Action only; annotations and
starter repo are separate tasks) is sensible in principle, but the issue
treats several things as already true or already settled that are neither:
the CLI contract it depends on does not exist in the repo, its own
acceptance criteria require an artifact its own Boundary excludes, its
policy citation is drawn from a document that explicitly disclaims being
policy, and it makes a "hidden vectors" confidentiality promise the
described architecture cannot actually keep.

## Findings, most severe first

### 1. The dependency this task needs does not exist, and the chain behind it is three layers of open, unfiled work

The issue's frontmatter declares `ordering_after: [524]` and every
acceptance criterion leans on "the frozen CLI contract" as if it is
available to build against ("scores derive from recorded batch artifacts
through the frozen CLI contract"). It is not. `docs/batch-interface.md`
(read at HEAD) and `src/jls/JLSStart.java` confirm the batch interface has
exactly three exit statuses (0/1/2) and no verdict channel at all — there is
no pass/fail comparison, no xUnit report, no exit status 3. Issue #524
(FEAT-C21-1, "the headless CLI... becomes a frozen, versioned promise...
including status 3") is itself **open** and its own body says it "orders
behind CAP-06's verdict machinery — FEAT-053 (#369) and TASK-0111 (#466)
must land the verdict/xUnit surface this contract freezes." Both #369 and
#466 are **open**, and #466 (which would actually build `Expectations`,
`TestVectorRunner`, `GradeReport`, and exit status 3) is itself
`blocked_by` **TASK-0021, which has not even been filed yet** ("its
number cannot be verified here... A link pass must add it"). So #701 sits
at the end of a chain of at least four unresolved dependencies, only the
first of which (#524) it names. A `band_mw: 1-1.5` estimate for #701 that
does not surface this is not crediting the real order of operations —
there is currently nothing to "consume through the frozen CLI contract."

**Recommendation:** either add the true ordering (`524 → 369 → 466 →
TASK-0021`, unfiled) explicitly to `ordering_after`, or descope #701 to
build against the CLI's *current* three-status/watched-element contract
with an explicit migration note for when #466 lands. Shipping #701 first
against fictitious infrastructure invites a rewrite the moment #466's
actual `GradeReport` schema is decided (which fields exist, xUnit vs.
plain-line default, whether timestamp/hostname/duration are omitted).

### 2. Acceptance criterion #2 requires an artifact this issue's own Boundary excludes

The Outcome paragraph and AC #2 both require exercising the Action against
"a repo generated from the starter template" — that is literally the test:
"Pushing a fixture-class submission triggers the Action and produces
Classroom points from the lab's hidden vectors." But the Boundary section
says plainly: "the starter repo and doc-tests are TASK-C526-3" —
out of scope for this task. #701 records no `ordering_after` on
TASK-C526-3 (its number is not even given), so nothing prevents this task
from being scheduled and "completed" before the starter repo exists. If
that happens, AC #2 cannot be verified as written and an implementer is
left to invent an ad hoc, non-canonical fixture repo to satisfy the letter
of the criterion — which is exactly the kind of substitute that could pass
review while not proving the actual student-facing flow works.

**Recommendation:** add an explicit ordering edge (`ordering_after:
[524, "TASK-C526-3"]`, once TASK-C526-3 is filed), or narrow AC #2 to
state it is provisionally verified against a minimal throwaway fixture
repo pending TASK-C526-3, with a follow-up re-verification criterion once
the real starter template lands.

### 3. AC #3 cites a document that explicitly disclaims being policy

AC #3 justifies "no interactive session is opened" by citing "CAP-21 AC-4,
#498 §7.2." Issue #498 is a rescue of a branch-only design document and
states outright, twice: "It is explicitly non-normative... Nothing in it
may be cited as settled policy," and §7.2 itself (quoted in #498's body)
describes the *process* still required to make the "no live co-simulation
for graders" rule real — "a decision issue quoting both sentences,"
edited section text, a CHANGELOG entry, and "an `ARCHITECTURE.md` decision
block carrying a rationale and a revisit trigger as `ARCHITECTURE.md`
requires" — none of which has happened. The currently-live text in
`docs/vcd-interop.md` (quoted inside #498's own body) says the opposite:
live co-simulation "was evaluated and **rejected**." Citing §7.2 as if it
already settles the question skips the very ratification step #498
insists on. This isn't a nitpick: an implementer reading only #701 would
reasonably believe the "recording, not the session, is the contract"
wording is already normative guidance to build against, when today it is
a proposal awaiting a maintainer decision.

**Recommendation:** cite the actually-normative source (today,
`docs/vcd-interop.md`'s existing "graders must not depend on interacting
with a running simulation" language) or file/land the #498 §7.2
ratification first and cite the resulting `ARCHITECTURE.md` decision
block, not the non-normative rescue issue.

### 4. "Hidden vectors" is stated as a solved property; the described architecture cannot deliver it unmodified

The Action runs inside the student's own generated repository's Actions
environment — a runner and workflow file the student has full push
access to. Nothing in #701 (or in #526, its parent) specifies how vector
confidentiality survives a student who edits `.github/workflows/*.yml` to
add a step that dumps environment variables, `cat`s files pulled by the
pinned container, or uploads the container's filesystem as a build
artifact. Baking vectors into the pinned digest-referenced image does not
solve this by itself: a student-controlled workflow can still `docker
run --entrypoint sh` or add a step to copy files out of the running
container before or after grading. This is a known, general limitation of
running "hidden" autograding entirely inside a student-owned CI
environment (as distinct from Gradescope's model, where grading runs on
platform-owned infrastructure the student never touches) — and CAP-21's
own risk list (#502 §3.5, "Gradescope is proprietary… no scraping, no
undocumented endpoints") shows the maintainer is alert to platform trust
boundaries elsewhere but this specific one goes unaddressed for the
Classroom adapter. The outcome and acceptance text both assert "hidden
vectors" flatly, with no named mitigation (e.g., restricting what the
workflow file is allowed to contain via branch protection Classroom
manages, or never materializing raw vectors in the workspace at all).

**Recommendation:** either state explicitly that vector secrecy against a
student who edits the workflow is *not* a guarantee this Action makes
(matching how real-world Classroom autograding actions are used today —
on the honor system, with only the final "don't publish full logs"
mitigation), or specify the actual mitigation (e.g., branch protection on
`.github/workflows/` that GitHub Classroom's "protected repository"
feature provides) as part of this task's scope, not left implicit.

### 5. "No post-hoc normalization" is unfalsifiable as written

AC #4 requires the Action's score summary to be emitted "with no post-hoc
normalization," but no document in the repo (this issue, #524, #526, or
#531) defines what "normalization" means operationally in this pipeline —
rounding? re-weighting visible vs. hidden tests? rescaling to 100? Without
a precise, testable definition, a reviewer cannot write a test that
asserts its absence, and two subtly different but internally consistent
scoring transforms could both plausibly claim compliance. This is exactly
the shape of acceptance criterion that reads as rigorous but is gameable
in practice.

**Recommendation:** pin the scoring function once, in the frozen CLI
contract (#524) or the xUnit schema (#466's `GradeReport`), as an exact
arithmetic definition (e.g., "points = sum of per-testcase weights for
passing testcases, integer, no rounding"), and have every adapter
(including this one) assert byte-for-byte equality against that
definition rather than an unquantified "no normalization" rule.

### 6. Circular/undefined hand-off with sibling issue #531

AC #4 also requires the summary be emitted "in the shared form #531's
parity test consumes." #531 (`CrossPlatformScoreParityTest`, the
300-submission hermetic fixture) is itself open and `ordering_after:
["FEAT-C21-1"]` only — it does not name #526/#701 as a prerequisite, yet
its own AC-1 needs all four adapters, including this Action, to exist
before it can compare them. Neither issue names the other as the owner of
the shared score-vector schema, so two independent implementers could
reasonably each assume the other already defines it. Given #524 is the
one issue both cite as authoritative for the CLI/xUnit surface, the score
schema should be pinned there, not left to be inferred from whichever of
#701/#531 lands first.

**Recommendation:** state explicitly in both #701 and #531 that the score
vector's byte format is owned by #524's xUnit schema, and that both
adapters and the parity fixture consume it, not each other.

### 7. Caching acceptance criterion is under-specified for the runner model that will actually grade students

"caches it so a warm run does not re-download the build" is stated as a
plain fact to be observed, but GitHub-hosted Actions runners are
ephemeral per job by default — persisting a pinned-digest image pull
across pushes requires an explicit mechanism (`actions/cache` keyed on
the digest, BuildKit/GHCR layer caching, or a self-hosted runner pool)
that the issue does not name. A verification that only exercises caching
on a long-lived or self-hosted runner would pass while the real
classroom deployment (GitHub-hosted, ephemeral, one runner per push)
still eats a cold pull on every submission — the opposite of the stated
goal ("a student is not waiting on a cold download").

**Recommendation:** name the caching mechanism explicitly (most likely
`actions/cache` on the image digest, since GHCR images here are already
digest-pinned per README) and require the acceptance test to run on a
fresh, non-reused GitHub-hosted-style runner to prove the warm path
actually engages.

### 8. Minor: "pinned JLS build" is ambiguous between the jar and the container image

README documents both a runnable jar and a purpose-built headless
container image (`ghcr.io/anadon/jls`, "for autograders and CI"). The
container image is clearly the intended target given #531's requirement
that the whole four-adapter fixture run "fully containerized," but #701
never says so explicitly, leaving an implementer free to pin a JDK+jar
combination instead — a materially different caching and CI story.

**Recommendation:** state explicitly that the Action pins `ghcr.io/anadon/jls`
by digest, consistent with #531's containerized-fixture requirement.

## What is solid

- **Digest pinning of a container build** is fully consistent with
  existing project practice: `ghcr.io/anadon/jls` already ships signed,
  multi-arch, cosign/attestation-verifiable images (README, "Container
  image (batch mode only)" section), so this half of the design has a
  real foundation to build on once the CLI contract itself lands.
- **The Boundary section's scope discipline** — explicitly deferring
  annotations to TASK-C526-2, the starter repo/doc-tests to TASK-C526-3,
  and marketplace publication to CAP-21 Open Question 3 — is the right
  instinct for keeping this task reviewable in isolation; it is
  undercut only by finding #2 (AC #2 needing the very artifact the
  Boundary defers).

## Verdict rationale

This is not a case of the goal being wrong — the parent feature (#526)
and capstone (CAP-21, #502) are coherently scoped, and TASK-C526-1's slice
of them is a reasonable cut. But as filed, #701 (a) depends on
infrastructure that is three to four issues deep in unfinished, partly
unfiled work while presenting it as available, (b) contains an internal
contradiction between its own acceptance criteria and its own Boundary,
(c) leans on a citation the cited document itself says must not be treated
as policy, and (d) states a security property ("hidden" vectors) the
described architecture does not actually secure. These need resolving
before implementation starts, not during it — hence needs-rework rather
than sound-with-concerns.
