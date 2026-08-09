# Issue #822: TASK-C568-1: a first-time contributor's push to their own fork returns green or red on its own, with no maintainer approving a workflow run
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "fork CI." The end is: **an outsider learns whether their diff is
acceptable before their attention decays.** #514's evidence is that both 2026
external PR authors bounced; #568 wants a stocked shelf plus a self-serve
verdict. Fork CI is one delivery mechanism for the verdict, and the issue treats
it as the only one.

Judged against that end, the task is worth doing and is cheap. But its stated
mechanism misdiagnoses this repository, and the acceptance criteria aim the work
at a problem JLS does not have while leaving the actual blocker unnamed.

## The diagnosis in the Outcome is wrong for this repo

The Outcome says a first-time contributor's run "can sit waiting on approval."
That is a description of `pull_request` runs in the **upstream** repo, gated by
Settings → Actions → *Fork pull request workflows from outside collaborators*.
Runs in a contributor's **own fork** are never subject to any upstream approval
gate. So the stated symptom and the stated fix (AC-1, fork-side runs) are about
two different things.

Worse, AC-2's premise is nearly empty here. Measured:

- `.github/workflows/ci.yml` — **zero** `secrets.` references, top-level
  `permissions: contents: read`, and exactly one `: write` in 1145 lines:
  `dependency-submission` (`contents: write`, ci.yml:764-768), *already* gated
  `if: github.event_name == 'push' && github.ref == 'refs/heads/master'`.
- `codeql.yml` — zero secrets; `security-events: write`.
- `release.yml` — all six `secrets.` uses, tag-triggered only, cannot fire on a
  fork branch push.
- `mutation.yml`, `repro-installers.yml` — schedule/dispatch only, zero secrets.

"Identify the jobs requiring secrets, upload permissions, or the release
identity and make them skip on forks" is a finished sentence in this repo: one
job, one condition, already written. AC-2 is a day of auditing that finds
nothing.

## The blocker the issue never names

`ci.yml:8-13` is `on: push: branches: ["master"]` plus `pull_request`. A fork
contributor pushing `fix-wire-drag` to their fork triggers **no run at all** —
not a run awaiting approval, not a red run: nothing. That branch filter exists
deliberately (issue #47, build-once-per-PR) and it is the entirety of AC-1.

Two further facts the ACs should have absorbed:

1. GitHub disables Actions on newly created forks until the owner clicks
   "I understand my workflows, go ahead and enable them." No upstream workflow
   edit removes that click. AC-1's "with no action by a maintainer" is
   achievable; the implied "with no setup by the contributor" is not, and
   CONTRIBUTING (AC-5) must say so or the first-timer concludes CI is broken.
2. CONTRIBUTING promises "Pull requests must also pass CodeQL code scanning."
   CodeQL needs `security-events: write`, which a fork owner has in their own
   repo but which fork-PR runs upstream do not. A green fork run therefore
   cannot promise the check that CONTRIBUTING names as blocking. That is the
   most load-bearing sentence AC-5 has to write, and the issue does not
   anticipate it.

## Reframing: cut along the hot-path/assurance seam, not the fork/not-fork seam

AC-2 as written implies threading `if:` guards through fifteen jobs and then
auditing every future job for fork-safety forever. There is a seam already
latent in this repo that does the same work once.

ci.yml's fifteen jobs are two populations:

- **Verdict on the diff** — `build` (compile-warnings-as-errors, headless
  tests, SpotBugs, NullAway, JaCoCo ratchet). One job. This is what a first PR
  needs, and it is exactly AC-3's list.
- **Assurance about the artifact** — jar reproducibility, four installer
  reproducibility lanes (incl. `ubuntu-24.04-arm`), four GUI-boot rigs, Agda
  proofs, dependency submission. None of it tells a contributor anything about
  their diff; `gui-wayland` alone pulls a ~100 MB JetBrains Runtime and boots a
  compositor, and it carries no `github.event_name != 'schedule'` guard, so a
  naive `on: push` widening runs it on every fork branch push.

So: **split the file.** `ci-core.yml` — one Linux/JDK-25 job, `on: push` (all
branches) + `pull_request`, `permissions: contents: read`, no privileged step,
minutes not tens of minutes. `ci.yml` keeps the heavy lanes and guards its
`push` half with `github.repository == 'anadon/JLS'` (a branch filter is not
enough — without the repository guard, widening `on: push` re-breaks #47 by
double-building every PR branch upstream).

This is not a new idea in this repo; it is the third instance of a pattern the
maintainer has already gotten right twice. `mutation.yml` was pulled off the
push path so it is "never a required PR check." `repro-installers.yml` was
pulled off the push path as report-only. Both were the same recognition:
assurance work must not sit on the feedback loop. #822 should be executed the
same way rather than by per-job fork conditionals.

The split pays three ways instead of one: forks get AC-1; **upstream** gets a
fast required check (today the required leg waits behind
`apt-get install iverilog ghdl yosys xvfb`); and AC-5 stops being drift-prone
prose — "what a green fork run covers" becomes "the contents of ci-core.yml,"
one file a contributor can read.

## Second framing: the best network round-trip is none

The stated goal is a five-minute loop. The floor for that is not GitHub; it is
the contributor's terminal. `mvn verify` already *is* the gate — CONTRIBUTING
leads with it, `.devcontainer` bakes the toolchain. What is missing is the
**equivalence claim**: nothing tells a contributor that local green equals
reviewer green.

Concretely: `scripts/preflight.sh` that runs precisely the ci-core command and
prints the same summary, plus a check asserting preflight and ci-core invoke the
same recipe. The repo already believes in this shape —
`scripts/build-installer.sh` is "the single recipe used both locally and by CI,"
and every rig has a `*-rig-selftest.sh` guarding it. Seconds, offline, and it
works for the contributors on managed accounts where fork Actions are
unavailable entirely. This does not replace AC-1; for the ninety-nine loop
iterations before the push, it dominates it.

## The zero-code change worth more than the whole task

Because ci.yml carries no secrets and a read-only token, the upstream
first-time-contributor approval setting — the thing the Outcome paragraph
actually describes — can be relaxed to "require approval for first-time
contributors who are new to GitHub," or none. That is a settings toggle, costs
nothing, is defensible *here* in a way it would not be in a secret-carrying
repo, and removes the maintainer from the loop for the PR run the contributor
actually cares about. It appears nowhere in the ACs. If only one thing ships
from #822, ship this.

## I am disregarding AC-4 as written

"Verified once end to end from a real fork" is a one-shot manual ritual whose
result expires the next time someone adds a job. That is not how this project
holds anything else true: coverage floors ratchet, the permits tree is pinned by
`test/jls/elem/SealedHierarchyTest.java`, the `@NullMarked` list only grows, the
rigs have selftests. Do the one-time fork run for #568's link, then replace the
criterion with a standing one: a workflow-lint test in `test/` (sibling to
`CliFlagTableTest`) asserting that every job in the contributor lane declares no
`permissions:` beyond `contents: read` and references no `secrets.`. That turns
"verified once" into "cannot regress," which is this repo's native idiom.

## One honest caution about leverage

"The single cheapest way to lose a drive-by contributor" is asserted, not
evidenced. #514 names three real data points (#4/#5, #187) and its own analysis
puts the blame on review latency (AC-4) and the 5,852-line SimpleEditor (AC-5).
Check those three PRs: if none bounced on CI feedback, #822 is hardening a stage
of the funnel that was not leaking. Still worth its 0.5–1 mw — the split makes
upstream faster regardless — but it should not be sequenced ahead of the
bottlenecks #514's own evidence identifies.
