# Issue #783: TASK-C589-2: every guarantee in the white paper names the test that would fail if it stopped being true, the limits get their own section, and CAP-21's kits link it under a live link check
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context established

#783 is TASK-C589-2, one of two tasks under FEAT-C36-2 (#589, the
grading-contract white paper). Its sibling TASK-C589-1 (#781) owns writing
the paper's prose (AC-1/AC-2 of #589); #783 owns the enforcement citations,
the limits section, and CAP-21's kit references (AC-3/AC-4/AC-5 of #589).
CAP-21 is #502, a capstone shipping four grading-platform kits (Gradescope
#525, GitHub Classroom #526, PrairieLearn #528, nbgrader #530). None of this
exists in the tree today: `grep -rli "gradescope\|prairielearn\|nbgrader" .`
returns zero hits, there is no `docs/whitepapers/`, and none of #502's named
test classes (`CliContractConformanceTest`, `CrossPlatformScoreParityTest`,
`GradescopeCorpusTest`, `RecordedArtifactOnlyTest`, `TemplateDocTest`) exist
under `test/`. The closest in-tree precedent for "link check" is
`test/jls/HelpTopicsTest.java`, which link-checks the in-jar help tree, not
code-artifact citations.

## Findings, most severe first

**1. (High) AC-4 hard-codes "four" kits while the issue's own ancestor reserves the right to ship three.**
#502 (CAP-21)'s kill criteria state: *"KC-21-3. If keeping an adapter green
requires tracking an undocumented platform interface, drop the adapter...
the kit ships with three platforms rather than one scraped one."* #783's
acceptance criterion reads *"The Gradescope, GitHub Classroom, PrairieLearn
and nbgrader kits reference the paper..."* with no conditional language. If
KC-21-3 fires on any one platform before #783 starts, this criterion is
unsatisfiable as literally written and the issue needs a rewrite, not just
completion. Recommendation: word AC-4 as "every shipped kit" and cross-link
KC-21-3 explicitly so a future closer isn't stuck reconciling a stale
count.

**2. (High) "each named enforcer exists and is running" checks existence, not correctness — it is gameable.**
Quoted from the issue: *"Every guarantee in the paper names its enforcing
test, suite or ratchet, and each named enforcer exists and is running
(AC-3)."* As worded, this is satisfied by naming any real, green test next
to a guarantee — nothing requires the named test's assertions to actually
cover the claimed property. A contributor under time pressure could cite,
say, `BatchSimulationGoldenTest` next to a determinism claim it doesn't
actually pin, and AC-3 would still pass a mechanical check. The issue
specifies no review step (human spot-check, or a machine-checkable
claim→assertion mapping) that verifies semantic correctness of the
citation, only its existence. Recommendation: require each citation to name
the specific assertion/method, not just the class, and have review
explicitly confirm the assertion enforces the stated guarantee — call this
out in the acceptance criteria, not just trust it to happen in code review.

**3. (High) "Link check" conflates two different mechanisms and the harder one is unscoped.**
The issue's fourth bullet: *"A link check fails the build when a kit's
reference to the paper, or a paper claim's reference to its enforcer, goes
stale."* Checking that a kit's markdown link to the paper resolves is
exactly what `HelpTopicsTest` already does for the help tree (URL/anchor
resolution). Checking that "a paper claim's reference to its enforcer" is
still valid is a structurally different, harder problem: it means parsing
the whitepaper for test-class/method names and asserting those symbols
still exist and still run in `test/`. That's new tooling — nothing in the
repo does this today — and the issue's single "link check" phrase hides
that this bullet is really two acceptance criteria of very different cost.
Recommendation: split this bullet into two explicit criteria (doc-link
resolution vs. enforcer-symbol resolution) so the smaller, cheaper part
can't be used to claim the whole bullet is done.

**4. (Medium) "reference the paper instead of paraphrasing the contract" has no operational test.**
A kit README that reproduces the whitepaper's determinism table nearly
verbatim, with a link appended at the bottom, satisfies a literal reading
of AC-4 ("references the paper") while defeating its actual purpose (one
source of truth, one place to update when the contract changes). The issue
gives no definition of "paraphrasing" for a reviewer to hold a PR against.
Recommendation: state a concrete rule, e.g. "kit docs may quote at most
N lines / must state 'see <link> for the full contract' and may not restate
determinism axes or exit-code tables."

**5. (Medium) The 0.5-1 mw estimate looks light once the two hard problems above are counted.**
`ordering_after: ["TASK-C589-1", 525, 526, 528, 530]` correctly gates
#783 behind the whitepaper's prose and all four kit FEATs — each of those
four is itself a multi-mw feature with its own doc tree, none of which
exists yet (confirmed above). By the time #783 is actually workable, it
needs to: audit an unknown number of guarantees against real tests (finding
2), build enforcer-symbol-resolution tooling that doesn't exist yet
(finding 3), and touch four independently-owned kit doc trees for AC-4. The
project's own convention (seen in #502's cost section, which prices
comparable wiring work at 2-4 mw per adapter) suggests "0.5-1 mw" undercounts
the tooling half of this task; it may be sized correctly only for a world
where the enforcer-symbol checker is scoped out or reused from elsewhere,
which the issue doesn't say.

**6. (Low) `ordering_after` names the coarse FEAT issues, not their component TASKs.**
525/526/528/530 are FEAT-tier issues; each already has its own TASK children
filed (e.g. #694/#697/#699 under #525, #701/#705 under #526, #706/#708/#710
under #528, #713 under #530). It's not stated whether #783 should wait for
the FEAT issue to be marked resolved (which presumably requires its TASKs
closed anyway) or could start once a subset of kit docs exist. Low risk
given GitHub issue-close semantics probably make this moot, but worth an
explicit note given how precise this project's dependency bookkeeping is
elsewhere (see #502's `blocked_by`/`blocks` fields).

## What's solid

- **Decomposition against the parent is clean.** #589's five acceptance
  criteria split without overlap or gap: TASK-C589-1 (#781) owns AC-1/AC-2
  (the document and its three sections) plus the "no guarantee invented
  here" discipline; #783 owns AC-3/AC-4/AC-5 exactly, with correct
  back-references to the parent's lettering.
- **The Boundary note correctly fences off prose-writing** ("Verification
  wiring and cross-references; the document's prose is TASK-C589-1"),
  which keeps this specific ticket from scope-creeping into content
  authorship.
- **The limits-section requirement (AC-5, "placed and weighted comparably
  to the guarantees") is a good, concrete anti-pattern guard** against the
  common failure mode of burying caveats in a footnote — worth keeping as
  written.

## Recommendation

Do not start implementation until: (a) AC-4 is reworded to survive a
KC-21-3 adapter drop, (b) AC-3's "exists and is running" gets a semantic
verification step beyond symbol existence, and (c) the "link check" bullet
is split into its doc-link and enforcer-symbol halves with the latter's
tooling cost acknowledged. These are wording/scoping fixes, not a reason to
abandon the task — the underlying goal (a verifiable rather than trusted
white paper) is sound and worth keeping.
