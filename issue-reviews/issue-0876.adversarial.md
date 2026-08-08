# Issue #876: TASK-C543-1: the colour-vision transform is one callable framebuffer filter, proven on a colour the Theme seam does not own
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the claim

#876 is one of two tasks split out of #543 (FEAT-C26-2) after its sole
original child (#736) was absorbed as a near-verbatim duplicate and left
the feature with an empty roster. This task owns the CVD colour-transform
math (protanopia/deuteranopia/tritanopia over a rendered image); its
sibling #877 owns the in-app live-canvas surface. The split itself is
well-reasoned and the factual claims that ground it check out against the
repo. The findings below are about unverifiable/unlabeled criteria and
cross-issue coordination gaps, not about whether the task should exist.

## Findings, most severe first

**1. AC-3's "no per-frame cost is added" has no test methodology anywhere in this repo, and its cited authority ("K9 / D9") does not resolve to anything checkable.**
The criterion reads: *"no per-frame cost is added when no transform is
selected (K9 / D9)."* A repo-wide search for perf/benchmark tests
(`Benchmark`, `nanoTime`, `JMH`, `PerfTest`) turns up nothing except an
unrelated `SpatialIndexTest`, and neither `K9` nor `D9` is defined
anywhere in `docs/`, `ISSUE-AMBIGUITIES-2026-07.md`, or any `.java` file
in this checkout — the only occurrences of those tokens in the whole repo
are in other synthetic planning issues. Worse, where `K9`/`D9` *are*
glossed (capstone #507's abstract and kill-criteria: `KC-26-4` = "pixel-
identical default theme is a gate on every commit," `D9` = "audience-fit
objections withdrawn") neither one is actually about per-frame CPU cost.
The issue itself acknowledges the claim needs its own proof ("an
implementation that produces identical bytes by filtering-then-inverting
every frame satisfies the first and violates the second") but supplies no
way to falsify the second half other than eyeballing the diff — exactly
the "no criterion claimed without a named test" failure mode CAP-26's own
PF-5 rule warns against.
*Recommendation:* replace the citation with a concrete, named check (a
microbenchmark test with a documented budget, or an architectural
assertion such as "the filter branch is unreached when `Theme` is active
and no CVD mode is selected, verified by a coverage/mutation check"), and
drop the unresolvable `K9 / D9` shorthand or point it at where those codes
are actually defined.

**2. AC-4's promise that #877 and #734 can drive the transform "without a second code path" is undercut by #877's own review, which was not consulted here.**
`issue-reviews/issue-0877.adversarial.md` (finding 2) establishes that
`SimpleEditor.paintComponent` (`src/jls/edit/SimpleEditor.java:2448-2524`)
draws straight to the on-screen `Graphics2D` Swing hands it — the method's
own comment warns *"the paint-pass Graphics is NOT cached."* Getting a
live-canvas `BufferedImage` for #877 to hand this transform therefore
requires restructuring the live paint path, which #876 neither mentions
nor budgets for. #876's AC-1 grounds "rendered image, callable headlessly"
in the existing `CircuitRenderer.exportImage` off-screen path (confirmed
at `src/jls/edit/CircuitRenderer.java:364`, a real `BufferedImage`
factory used by batch `-i` export) — solid for the headless/#734 case, but
that is not evidence the *live* canvas can hand this transform an image
without its own new code path, which is exactly what AC-4 promises won't
be needed.
*Recommendation:* either scope AC-4's "no second code path" claim to the
headless/CI consumer only, or add an explicit dependency note pointing at
#877's paint-path restructuring so the promise isn't read as already
satisfied by #876 alone.

**3. The tritanopia LMS matrix is likely to be implemented twice, with no edge forcing them to agree.**
`test/jls/ThemeTest.java:158-167` already hardcodes `DEUTERANOPIA` and
`PROTANOPIA` 3×3 matrices (Vienot et al. 1999) as **test-only** constants
used to filter `Theme` colors for the delta-E ratchet. #729 (open,
`ordering_after: []`) will add the third matrix, **tritanopia, to that
same test file**, per its own body: *"a tritanopia simulation transform
is added alongside the existing... transforms, with its derivation cited
in the test."* #876 separately needs all three matrices **in production
code** ("the transform matrices and their source cited in the code") for
the framebuffer filter. #876 correctly notes #729 is "a sibling," not a
prerequisite (no ordering edge is claimed), but nowhere does either issue
require the two derivations to share a single source of truth. Two
independent implementations of the same physiological matrices, landed by
different contributors at different times, is a classic drift risk — one
copy gets updated (rounding, a corrected coefficient) and the other
doesn't, and nothing in either issue's acceptance criteria would catch it.
*Recommendation:* have #876 (or #729, whichever lands first) extract the
matrices into a small shared, headless utility both `ThemeTest`'s
delta-E harness and the new framebuffer filter import, rather than two
literal copies.

**4. The capstone -> feature -> task chain the issue is explicitly protective of is not actually wired up in GitHub.**
The body's rationale for existing as a third disposition rests on not
"breaking the capstone → feature → task chain," and the disposition
comment on #543 files a "roster, filed" table listing #876 and #877 as
#543's two tasks. But `issue_read(get_parent)` on #876 returns
`{"parent": null}`, and `issue_read(get_sub_issues)` on #543 returns `[]`
— GitHub's actual sub-issue linkage was never created for either new
task. The chain the issue argues so carefully for preserving exists only
in issue-body prose and YAML front matter (`part_of_feature: 543`), not
in the tracker's structured hierarchy, so any tooling or query that walks
real parent/child links (rather than grepping issue text) will not find
#876 under #543 or under capstone #507.
*Recommendation:* use `sub_issue_write` to actually link #876 (and #877)
under #543 before or immediately after filing, so the roster table is
backed by data, not prose.

**5. Minor: `task_id: TASK-C543-1` is reused verbatim from the closed, duplicate-superseded issue #736.**
#736 (closed as a near-verbatim duplicate of #543) carried the identical
`task_id: TASK-C543-1` and covered the full original scope (transform +
GUI surface). #876 reuses that same task id for a narrower scope
(transform only). Anyone searching the tracker or a commit log for
`TASK-C543-1` will find two issues with different scopes under the same
identifier, one closed-duplicate, one open. Not load-bearing, but
avoidable bookkeeping confusion in a project that otherwise treats these
ids as stable keys.
*Recommendation:* mint a fresh task id (e.g. `TASK-C543-1a` or similar)
for the split-off scope rather than reusing the superseded one.

**6. Minor: AC-2's "a specific hardcoded call site" is never named.**
The criterion requires naming a call site in a test but the issue itself
does not pick one from the 113 candidates it enumerates, leaving the
choice entirely to the implementer. Reasonable to defer, but a one-line
pointer (e.g. a call site visible in the shipped adder lab, matching
#734's fixture) would have removed any ambiguity about whether the chosen
site is representative or an edge case.

## What's solid

- The 113-occurrence `Color.black`/`Color.BLACK` grep count is verified
  correct both at the cited commit `29afb26` and at current HEAD
  (`5b05d67`), and the issue is right that `Theme.java:28`'s "~126" is
  stale — a real, checkable fact, correctly flagged as something not to
  quote.
- The framebuffer-vs-palette distinction is real and technically sound:
  `ThemeTest` today only ever filters `Theme` record colors, never a
  rendered pixel buffer, so a palette-level implementation genuinely
  would leave the ~113 hardcoded sites untouched — the premise behind
  AC-2 holds.
- The `ordering_after: [#731]` resolution (not #729, not #734) is well
  argued and traces cleanly through the #736/#543 disposition history
  without contradiction.
- The task/feature split from #543 into #876 (math) and #877 (surface) is
  a real seam — independently testable, and #876 correctly disclaims
  ownership of the GUI surface rather than re-absorbing it.
- The #542 ordering-inversion risk (whether the transform is #542's to
  write and this cluster's to wrap) is disclosed rather than silently
  decided, with an explicit `REPLAN:`-on-#507 escape hatch — good
  practice even though (per finding 3) it leaves a live duplication risk
  unresolved.

## Verdict

**sound-with-concerns.** The task boundary and its factual grounding
(grep counts, existing test precedent, ordering resolution) are solid.
What needs fixing before an executor starts: AC-3's cost claim is cited
against decision codes that don't resolve to anything in this repo and
have no test infrastructure to back them (finding 1); AC-4's "no second
code path" promise is not yet consistent with what #877's own review
found about the live paint architecture (finding 2); and the tritanopia
matrix and the capstone→feature→task linkage are both likely to exist in
two places that nothing forces to agree (findings 3-4).
