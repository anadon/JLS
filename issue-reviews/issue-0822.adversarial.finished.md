# Issue #822: TASK-C568-1: a first-time contributor's push to their own fork returns green or red on its own, with no maintainer approving a workflow run
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

The issue wants forks to get an automatic, no-maintainer-action CI verdict
(AC-1), with secret-needing jobs skipping cleanly (AC-2), a subset that
actually covers what a first PR needs (AC-3), one real-fork end-to-end
verification (AC-4), and CONTRIBUTING coverage of what a fork run does/
doesn't prove (AC-5). It is filed as a sub-task of #568 (`part_of_feature:
568`), whose own AC-3 restates the same goal almost verbatim.

## Findings, most severe first

**1. [High] The proposed mechanism may not touch the thing actually
causing the delay.** The issue body's complaint is "a first-time
contributor's run can sit waiting on approval." Grepping `ci.yml` and
`codeql.yml` — the only two workflows that trigger on `pull_request` —
turns up **zero** `secrets.*` references anywhere in either file (the
only elevated-permission job, `dependency-submission` at `ci.yml:764-768`
needing `contents: write`, is already gated to `push` on `master` only).
That means the actual `pull_request`-triggered run against the upstream
repo (the one that becomes the PR's required check) has nothing
secret-dependent to protect and would already run fine off fork code —
the thing holding it back is almost certainly GitHub's repo-level "Require
approval for first-time contributors" Actions setting, a one-click repo
setting, not a workflow-file limitation. AC-1 through AC-4 as written are
entirely about making the fork's **own** `push`-triggered Actions tab
produce a verdict — a parallel, non-required signal that says nothing
about whether the actual PR's required check still needs a maintainer
click. AC-4 could pass ("verified once end to end from a real fork," fork
tab shows green) while the outcome stated in the issue body — "no action
by a maintainer of the upstream repo" on the real PR — remains false.
*Recommendation:* state explicitly which run is meant to satisfy "no
maintainer approval" — the fork's own push run or the upstream PR's
required check — and if it's the latter, add an AC that changes (or
explicitly declines to change, with reasoning) the repo's fork-PR-approval
setting.

**2. [High] AC-1 conflicts with `ci.yml`'s existing trigger, and the
conflict has a cited rationale the issue never engages.** `ci.yml:8-11`:
```
on:
  push:
    branches: ["master"]
  pull_request:
```
with the comment "push is limited to master so a branch with an open PR
builds once, not twice (issue #47)." `codeql.yml:6-9` has the identical
restriction. A first-time contributor virtually never pushes straight to
a branch literally named `master` on their own fork — they push a feature
branch — so under the *current* filter their push triggers nothing at
all in their fork's Actions tab, contradicting AC-1's "a push to a fork
... triggers a build-and-test run." Fixing this requires widening the
`push` trigger (e.g. to all branches, gated by `github.repository !=
'anadon/jls'`), which is a real, non-trivial workflow change the issue's
five ACs never mention, and which must be reconciled with #47's
double-build concern (a fork branch that later becomes a PR against
upstream must not now build twice). *Recommendation:* add an explicit AC
for the trigger change and its interaction with #47, since "make the
workflows fork-runnable" quietly presupposes it.

**3. [Medium] AC-2's "jobs requiring secrets ... are separated out" premise
doesn't match what's actually in scope.** As found in #1, ci.yml/codeql.yml
use no secrets at all for the push/PR path. The real constraint on a fork
run isn't secrets — it's the multi-OS **HARD GATE** GUI-boot lanes
(`gui-x11`, `gui-wayland`, `macos-gui`, `windows-gui` — `ci.yml:353-763`,
each commented as a required branch-protection check once promoted) and
the `installer-reproducibility-aarch64` job on `ubuntu-24.04-arm`
(`ci.yml:923`), which need macOS/Windows/ARM64 hosted runners and,
per the promotion-rule comments, a 10-20 consecutive green run history
before they gate at all. AC-2 as worded could be satisfied almost as a
no-op (there's nothing secret-shaped to skip), leaving the actually hard
question — whether a fork should even attempt the GUI hard gates, and
what "skip cleanly" means for a runner-availability problem rather than a
credentials problem — unaddressed. *Recommendation:* reframe AC-2 around
the jobs that actually exist (name the GUI lanes and the aarch64 leg)
rather than a generic "secrets" framing.

**4. [Medium] No regression protection, unlike every comparable guarantee
in this codebase.** ARCHITECTURE.md documents a strong house pattern:
cross-cutting properties get a ratchet test (`HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `ElementConstructorContractTest`, etc.). AC-4
settles for "verified once end to end from a real fork" — a one-time,
manual, non-repeatable check. Nothing in the ACs prevents a later PR from
adding a `secrets.*` reference or a new required job to `ci.yml` and
silently breaking fork-runnability again, with no test catching it.
*Recommendation:* pair AC-2/AC-3 with a lightweight static check (e.g. an
`actionlint`-based test, or a grep-based assertion that only an
allow-listed job set may reference `secrets.`) so the property doesn't
quietly rot the way #47's dedup logic almost did here.

**5. [Medium] AC-4's verification step has no owner and is awkward for a
single-maintainer project.** ARCHITECTURE.md's i18n section states
plainly that JLS "is a single-maintainer pedagogy tool." AC-4 requires
testing "from a real fork (an account other than the maintainer's, or a
clean fork with no prior approval history)" — i.e. the maintainer needs
either an outside volunteer or a second personal account, and the issue
names neither an owner for that step nor a fallback if no tester
materializes. For a task banded `0.5-1`, this is a real scheduling risk
the issue doesn't surface. *Recommendation:* name who performs AC-4, or
state that a maintainer-controlled alt account is explicitly acceptable.

**6. [Low] AC-3's "style/static-analysis gates" is unenumerated and
gameable.** SpotBugs (threshold High, run inside `mvn verify` in the
`build` job — see ARCHITECTURE.md "`mvn verify` runs the suite plus
SpotBugs... keep it green") is the obvious target, but CodeQL is a
separate workflow whose findings land in the Security tab rather than
blocking merge directly, so it's ambiguous whether AC-3 requires it. A
minimal implementation could satisfy the letter of AC-3 by covering only
`build`'s compile+test+SpotBugs and never touch CodeQL (or vice versa),
without anyone having agreed that's sufficient. *Recommendation:*
enumerate the exact check names AC-3 must cover.

**7. [Low] AC-5 has no content spec and is the exact place today's
scope gaps would surface.** "CONTRIBUTING states what a fork run does and
does not cover" is satisfiable with one vague sentence. Given finding #3,
the real "does not cover" list is the GUI hard gates and the aarch64 leg
— exactly the part most likely to surprise a contributor whose green fork
run is later blocked by a required check their fork never ran.
*Recommendation:* require AC-5's text to name covered vs. not-covered job
names explicitly, not merely assert the distinction exists.

## What's solid

- AC-2's demand for an "explicit, readable reason" on every skip is good
  discipline and matches this repo's existing error-taxonomy habits
  (`LoadError`, `TellUser`) — worth keeping once the underlying job list
  is corrected per finding #3.
- Scoping the "what a first PR needs" bar to compile + unit tests +
  static analysis (AC-3) is a reasonable floor, independent of the
  enumeration gap noted in finding #6.
- Insisting on a real external fork rather than a simulated one for
  verification (AC-4) is the right instinct — this is GitHub-platform
  behavior that unit tests genuinely cannot observe — the concern above is
  about ownership/repeatability, not the method itself.

## Bottom line

The stated outcome (a first PR gets fast feedback with no maintainer
click) is worth pursuing, but the issue diagnoses the wrong bottleneck for
at least part of its acceptance criteria (finding #1), proposes a fix that
directly conflicts with an existing, documented trigger restriction tied
to #47 without reconciling it (finding #2), and frames the remaining work
around a "secrets" problem that doesn't actually exist in the affected
workflows (finding #3). These need to be resolved before implementation
starts, or the work risks landing a green fork-tab checkmark that doesn't
change what a contributor's actual PR experiences.
