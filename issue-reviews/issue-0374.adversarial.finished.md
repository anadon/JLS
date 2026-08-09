# Issue #374: TASK-0015: a wedged CI job costs minutes instead of the silent six-hour ceiling, and a new job cannot arrive unbounded
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

Add `timeout-minutes` to all 23 CI jobs across 6 workflow files, add step-level
timeouts to "four" network-fetch steps, and add a line-matching
`WorkflowTimeoutRatchetTest` that fails the build when a new job arrives
without a timeout. Core facts were re-verified against HEAD
(`5311625`, 2026-08-09, vs. the issue's pinned `2d0ca9d`): `git grep -c
'timeout-minutes' .github/workflows/` still returns nothing, the job count is
still 23 across 6 files, and `test/jls/**RatchetTest.java` still numbers
eight. The issue's central claim holds.

## Findings, most severe first

**1. O5's "four network fetches" undercounts the actual fetch surface — the step-timeout scope is too narrow even for the evidence commit, not just due to drift.**
O5 lists only the Windows `curl` (oss-cad-suite), the macOS `brew install`,
the `gui-wayland` JBR `curl`, and "the two apt-get steps" at `:72-73`/`:404-405`.
Checked directly against the pinned commit
(`git show 2d0ca9d:.github/workflows/ci.yml | grep -n 'apt-get install\|curl -fsSL\|brew install'`),
there are at least four more network-fetch steps not enumerated: the `proofs`
job's `sudo apt-get install ... agda agda-stdlib` (line 312), the `gui-x11`
job's `sudo apt-get install ... xvfb xdotool x11-utils ...` (line 530), and
**two** `sudo apt-get install -y fakeroot rpm diffoscope-minimal` steps in
`installer-reproducibility` and `installer-reproducibility-aarch64` (lines
887, 942) — jobs the issue itself calls out in the Abstract as "the ones most
exposed to a hang." §7.10's strict-inequality step-bound requirement and §8's
Method checklist are scoped explicitly to "the four fetch steps of O5," so
these four additional stalling points ship this task unbounded at the step
level even after full completion — H3's premise ("a stalled fetch inside a
bounded job fails at the job timeout with no step attribution") will still be
true for exactly the jobs (`proofs`, `gui-x11`, both installer-reproducibility
jobs) the issue flags as the highest-value targets.
Recommendation: correct O5 to the full fetch inventory before work starts, or
explicitly descope the omitted four with a reason (their job-level timeout is
tight enough that step attribution doesn't matter) rather than have the
Method silently miss them.

**2. O4's "four legs are advisory" undercounts `continue-on-error: true` at HEAD, which weakens Open Question 2's mitigation and the P6 negative control.**
O4 names build's matrix leg plus `windows` (`:156`), `macos` (`:263`), and
`macos-gui` (`:598`) as job-level advisory. Verified against the current
tree: `windows-gui` (line 726), `windows-installer-msi` (line 1004), and
`macos-installer-reproducibility` (line 1071) are also job-level
`continue-on-error: true` — six job-level instances plus the matrix-scoped
one, not four. Open Question 2 ("What bound do the four advisory jobs get,
given the thinnest history?") explicitly singles out "`windows`, `macos` and
`macos-gui`" for provisional-value treatment because they're advisory and
gated off the cron, but omits `windows-gui`, `windows-installer-msi`, and
`macos-installer-reproducibility`, which share exactly the same
thin-run-history problem and aren't flagged for the same caution. A reader
following the Open Questions section as written will under-provision slack
for three jobs that need it as much as the three named ones.
Recommendation: re-derive O4/OQ2 against current HEAD (not just the pinned
commit) before the run-history reading pass, since the set of thin-history
advisory jobs has grown since `2d0ca9d`.

**3. The automated acceptance test (P1/P2) can pass while the real goal — bounds derived from actual observed runtime — silently fails.**
§7.10 defines the asserted predicate as `offenders = ∅ ∧ ∀j: τ(j) ≤ 360`.
Open Question 3 explicitly decides *not* to assert a tighter per-job upper
bound ("assert presence and the ceiling only... a magnitude assertion in a
test duplicates the comment and goes stale"). That means `timeout-minutes:
360` on every job — a value that changes nothing about the six-hour exposure
this task exists to close — passes `WorkflowTimeoutRatchetTest`,
`git grep -c timeout-minutes`, and every other automated check in §5/§9. The
entire load-bearing part of the fix (values actually derived from `2×max
R(j)`, and the P7 comment recording that basis) is enforced by nothing but
human review of a PR-pasted table that "cannot be re-derived from the tree
later" (§6, §9). This is explicitly acknowledged as a design tradeoff, not a
blind spot — but it means the Definition of Done's automated boxes are
gameable by construction, and a reviewer who only reruns the pasted commands
(rather than independently spot-checking a few values against the Actions
UI) cannot detect a task that "closed" via `360` everywhere.
Recommendation: at minimum have the ratchet test assert every job's timeout
is *below* the ceiling (e.g. `< 360`), which costs nothing, doesn't go stale,
and closes the most trivial gaming vector while leaving magnitude review to
humans as intended.

**4. Feasibility gap: several jobs whose bound this task must set have too little run history to honestly compute `⌈2·max R(j)⌉`.**
`macos-gui` and `windows-gui` scaffolding landed via PR #266 only around
2026-07-28 per #265/#111 (fetched and cross-checked); `windows-installer-msi`
and `macos-installer-reproducibility` are similarly recent additions. §6
calls for "ten green runs per job" as the input data, but Open Question 2
already concedes three of these jobs may not have ten runs to read, and
Finding 2 above shows the actual affected set is at least six jobs, not
three. H1 ("all 23 jobs lack a timeout for the same mechanical reason, a
single sweep closes it") is formally true, but the *values* chosen for these
newer jobs will not be "justified by its own observed runtime" as the
Abstract promises — they'll be justified by however many runs (possibly one
or two) exist, contradicting the stated methodology without tripping any
falsification criterion, since H1's refutation condition is "no honest bound
exists" (a much weaker bar than "ten runs exist").
Recommendation: add an explicit floor (e.g. "fewer than N runs → use the
ceiling with a `PROVISIONAL:` comment, not the formula") so a thin-history
job doesn't get a formula-shaped number that looks derived but isn't.

**5. Minor: the scanner's own robustness is untested by the issue's predictions for the workflow files that don't match the `ci.yml` shape.**
P4 (anti-vacuity) and H2 (line-matcher safety) are stated generally, but the
only concrete threat named is `push:`/`pull_request:`/`schedule:` sharing
`ci.yml`'s two-space indentation. `codeql.yml`, `mutation.yml`, `release.yml`,
`repro-installers.yml`, and `scorecard.yml` were not spot-checked in the
issue for the same hazard (e.g. a top-level `env:` or `permissions:` block
with a two-space single-word key that could be miscounted as a job). This is
a plausible-but-unverified gap, not a confirmed bug — a competent
implementer following P4's anti-vacuity discipline will likely catch it
during implementation regardless.
Recommendation: run the scanner design against all six files during
implementation, not just the two ci.yml examples cited, before trusting the
job count.

## What's solid

- The core defect (zero `timeout-minutes` anywhere, 23/23 jobs, unbounded by
  GitHub's silent 360-minute default) is real and independently re-verified
  at current HEAD, not just at the pinned commit — this task is not stale.
- The "no YAML parser" constraint (O6) is accurate (`pom.xml` has no
  snakeyaml/yaml dependency) and the line-matcher-with-anti-vacuity design is
  a reasonable, low-risk response to that constraint.
- Dependency graph against #317/#265/#111 is internally consistent: all three
  are open, #317's own machine block independently corroborates the 23-job/
  6-file count and cites the same `continue-on-error` lines for `windows`/
  `macos` this task defers to TASK-0017 — no contradiction found there.
- Scope discipline is genuinely tight: explicitly leaves `continue-on-error`
  removal, lane promotion, and branch-protection changes to sibling
  tasks/issues, and P6's negative control (a green run must stay green) is a
  correct and cheap check against the obvious failure mode of setting values
  too low.
- Zero new dependencies, zero runtime/format changes — low blast-radius as
  claimed.

## Bottom line

The mechanism (line-matcher ratchet, per-job derived values, step-level
fetch bounds) is sound and the core diagnosis is real and current. The
concrete gaps are in the issue's own evidence-gathering: O4 and O5 were
already incomplete relative to the very commit they're pinned to (fakeroot/
rpm/diffoscope and agda/xvfb-toolchain fetches; three additional advisory
job-level `continue-on-error` sites), and the automated acceptance criteria
(§7.10, Open Question 3) are deliberately weak enough that a `timeout-minutes:
360` sweep satisfies every test in the Definition of Done without closing the
six-hour exposure. None of this is fatal — it's fixable by widening O4/O5
before the sweep and tightening the ratchet's ceiling assertion — but it
should not proceed as filed without that correction pass, since an
implementer working strictly from §8's Method checklist will reproduce both
gaps.
