# Issue #378: TASK-0016: an hours-long test has a scheduled lane to run in, the required gate has a stated budget, and a large fixture has a declared home
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a well-evidenced task issue: every "RULE 3 RED STATE" observation
(O1-O9) was independently re-run against the current checkout
(`2d0ca9d`-identical for `src`, `test`, `pom.xml`, `.github/workflows`) and
every one still holds exactly as claimed — no `timeout-minutes` anywhere in
`.github/workflows/` (`grep -rn timeout-minutes .github/workflows/` exits 1),
no `longrun` token in the tree outside `issue-reviews/`, `pom.xml:268-296`
has exactly the two surefire executions described, `mutation.yml:11-14` and
`ci.yml:8-30` match the cited shape byte-for-byte, and the five largest
tracked fixtures match O6's numbers exactly (120179/5680/4132/3039/2365
bytes). The design (three surefire executions gated by tag, a `longrun.yml`
copying `mutation.yml`'s shape, a fixture-size ratchet keyed off
`ArchUnit`/plain-XML reads) is consistent with the codebase's existing
idioms (`DialogCoverageRatchetTest.java`, the JaCoCo floor ratchet at
`pom.xml:317-321`) and the falsification criteria are genuinely falsifiable.
That said, several load-bearing numbers are left as unconstrained "pick
something defensible" decisions with no upper bound a test can check, which
is exactly the vacuity failure mode the issue itself warns about elsewhere.

## Findings

### 1. [HIGH] The one dependency this issue names as "real" is not encoded anywhere machine-checkable, and the promised fix has already lapsed

The issue's own machine block says `blocked_by: []` while the prose two
paragraphs later says "the TASK-0015 edge is real rather than stylistic...
the edge is `blocked_by: [TASK-0015]` in scope and a link pass adds the
number." At the time #378 was filed (2026-08-03T14:15:33Z) TASK-0015 was
not a placeholder — it is **issue #374**, filed 6 minutes earlier
(2026-08-03T14:09:01Z) by the same author, and #374's own body makes the
identical promise in the other direction: "Sibling tasks TASK-0016... are
being filed concurrently and their numbers do not exist yet... A link pass
adds both edges." I fetched both issues live: as of today (2026-08-09,
six days after filing) `#374.updated_at` and `#378.updated_at` are both
still their creation timestamps — the link pass never ran. `#374`'s
`blocks:` list is `[265, 111]`, missing 378; `#378`'s `blocked_by:` is
still `[]`. Any executor or tooling that walks the DAG through the machine
block alone (which is the methodology this repo's own issues use — see
#317's "DAG walk, recorded per the tier-model note") will see #378 as
unblocked today, even though its own H2/O3 reasoning says starting it before
#374 lands makes the `timeout-minutes` convention "a moving target." Since
the number is now trivially knowable, deferring this to an unscheduled
"link pass" is an unforced gap.
**Recommendation:** update `#378`'s machine block to `blocked_by: [374]`
(and `#374`'s to add `378` to `blocks:`) now, rather than leaving a
self-declared hard blocker undiscoverable by anything but reading the prose.

### 2. [MEDIUM] The required-lane wall-clock budget is a "pick a defensible number" decision with no enforced ceiling

Open Question 1 states "It must be a number, and it must be defensible,"
recommends "(a) the measured current `mvn verify` time plus a stated
headroom," and marks it "Blocks execution, not filing." No formula bounds
what counts as reasonable headroom (contrast with sibling TASK-0015/#374,
which pins its own timeout values to a concrete `⌈2·max(R(j))⌉` formula).
None of P1-P7 or the Completion Criteria checks that the *chosen* budget is
tight — only that a number exists and that a workflow-lint test exists. An
executor can satisfy every stated acceptance criterion by picking a budget
of, say, 90 minutes with a one-line justification, which defeats the
feature's actual purpose (#317's own criterion: "the required gate's wall
clock stays inside a stated budget... exceeding it fails as a budget
violation rather than passing slowly") while passing every check this issue
defines. This is exactly the "vacuous ratchet" failure class the issue
itself names for the fixture cap (§11) but does not defend against here.
**Recommendation:** add a falsifiable upper bound on the budget itself (e.g.
"B must be within Nx of the measured `mvn verify` baseline, or the PR must
justify the multiplier in a comment"), not just a requirement that *some*
budget be written down.

### 3. [MEDIUM] The fixture cap has the identical gap, one level removed

Open Question 2 offers "(a) a cap comfortably above [120,179 bytes]... so
the ratchet is not immediately red" as the recommended default, with no
stated ceiling on "comfortably." P5 only proves the ratchet can fail *at
all* (a manually-created, manually-deleted oversized file) — it says
nothing about whether the chosen cap is small enough to ever bind against a
real future fixture. Combined with H3's steer toward "generate-at-test-time
from a committed generator" for anything above the cap, a sufficiently
generous cap (e.g. 50 MB) plus that steer could mean the ratchet is
never exercised by a real fixture for the practical life of the project —
decorative in the same way O8 warns JaCoCo floors become when set with no
margin discipline. Unlike the JaCoCo floor convention this repo already
has (`pom.xml:317-321`, "keeps at least a point of headroom under its
headless measurement" — a stated, checkable margin rule), no analogous
margin rule is proposed for the fixture cap.
**Recommendation:** state the cap as a fixed small multiplier of the
current largest tracked fixture (e.g. 2x of 120,179 B) rather than leaving
"comfortably above" undefined, and require any future cap raise to justify
the new multiplier the way `pom.xml`'s coverage-floor comments already do.

### 4. [MEDIUM] TASK-0051 file-conflict hazard is named and then left with no enforcement, which matters specifically because this repo is worked by multiple concurrent agents

"TASK-0051... edits the same workflow file and should be sequenced
alongside to avoid three rounds of conflicts, but neither creates data the
other reads, so it is not an ordering edge." Correct that it's not a *data*
dependency, but the issue supplies no coordination mechanism (no
`blocked_by`, no claim/lock convention, no note in either issue's body
pointing at the other beyond this one prose sentence) — I confirmed TASK-0051
is filed as #386 and its body was not checked for a reciprocal pointer back
to #378. In a fleet-of-agents execution model (which this review itself is
running under), "should be sequenced" with nothing machine-checkable is a
coin flip that two different sessions pick up #378 and #386 concurrently,
producing exactly the merge-conflict cost the sentence predicts.
**Recommendation:** at minimum, cross-link #378 and #386 in both bodies'
`related` fields (currently #378's `related` list is `[335, 202, 265, 111]`
— #386/TASK-0051 is absent even from `related`, despite being named in
prose two sections later).

### 5. [LOW] The three ratchets' non-vacuity all rest on a one-time, easy-to-skip manual demonstration

P5 ("Add a temporary file above the cap... observe fail... Remove it;
observe green") and the equivalent `displaylongrun` demonstration for P4
are both pasted into the PR once at merge time, not preserved as a
self-triggering negative test that CI re-runs on every future change. This
mirrors the existing, accepted convention for the JaCoCo floors (O8: "a
floor that has never been seen to fail should be assumed vacuous") so it
is not a defect unique to this issue, but stacking three separate new
ratchets (`longrun` exclusion, fixture cap, `CONTRIBUTING.md` cap-text
sync) onto the same single-shot manual-proof discipline in one task
triples the number of places a future silent regression (e.g. someone
"fixing" a broken build by loosening `excludedGroups` back to a bare
`display`) would go undetected between now and the next time a human
happens to re-run the demonstration.
**Recommendation:** no action required beyond what's already asked, but
flag in the PR description which of the three checks got a genuine mutation
test versus only the one-time paste, since DoD as written doesn't
distinguish them.

## What's solid

- Every observation (O1-O9) was independently re-verified against the live
  tree and is accurate, including exact fixture byte counts and exact
  `pom.xml`/`mutation.yml`/`ci.yml` line content.
- The ownership split between #317 (owns "on the merits") and #335 (named
  as "the other consumer") is internally consistent — I read #335 in full
  and it explicitly declines to draw a `blocked_by` edge to #317 specifically
  to avoid a near-cycle, and both issues agree TASK-0016 is a single shared
  child counted once. No contradiction found there.
- H2's risk framing (long-run lane might need something `mutation.yml`
  lacks, e.g. a self-hosted runner) is a real, well-scoped falsification
  path with a concrete next move if refuted.
- The JaCoCo-isolation requirement (§7.11, H4) correctly identifies the
  actual hazard already documented in this repo's own `CONTRIBUTING.md:
  100-103` and requires a `pom.xml` comment recording the choice — good
  discipline, matches existing conventions.
- The symlink-outside-repo guard for the fixture walk (§7.11) is a sensible,
  easily-forgotten security-adjacent detail that the issue does not skip.
