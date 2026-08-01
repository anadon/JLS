# FEAT-007 - CI long-run lanes, timeouts and cross-platform parity

**Status:** proposed | **Cost:** 3-6 mw | **Owner program:** UNOWNED |
**Spine rank:** S14

## Capability delivered

Continuous integration gains the three properties a multi-hour capstone needs
from it: every job has an explicit timeout so a hang costs minutes rather than
the hosted six-hour ceiling, long runs live in their own scheduled lane so the
required gate stays short, and the full suite is a required check on every
supported platform rather than a best-effort leg. Without this, every capstone
whose acceptance test takes hours has nowhere to run, and every capstone that
claims cross-platform behavior is claiming it from one platform's evidence.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | Zero jobs across six workflow files carry a timeout today, and the platform lanes are not required checks |
| CAP-02 | required | A boot run is 1.66-1.72 h at HEAD constants; it has no lane and would not fit the required gate |
| CAP-03 | required | The ternary acceptance run has the same shape and the same lane requirement |
| CAP-06 | required | A grading batch over many submissions is a long-run-lane workload, not a required-gate workload |
| CAP-09 | required | Differential and property runs are long by construction |
| CAP-14 | required | The ngspice differential corpus is explicitly a nightly comparison, which is this lane |
| CAP-17 | beneficial | multi-host and multi-partition tests do not fit the short blocking lane and need a lane that can host them |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-005 | A long-run lane that hosts a quadratic parse hosts a quadratic parse for hours; the lane must be worth its wall clock |
| FEAT-006 | A lane cannot host a run that the default time limit terminates, that cannot be paused, and that reports no progress |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0015 | Explicit timeouts on every workflow job | One day of work that bounds every future hang; it needs nothing and blocks nothing |
| TASK-0016 | Split CI into a required fast lane and a long-run lane, with a fixture policy | Shared with FEAT-009: the fixture-size policy and the storage mechanism are the same decision as where the calibration fixture lives |
| TASK-0017 | Promote the macOS and Windows headless lanes to required | The lanes exist; making them required and arming their suites is the work |
| TASK-0018 | Wayland GUI rig first light | Shared with FEAT-008: a GUI lane is what makes editor tests runnable at all |

## Acceptance criteria

1. Every job in every file under `.github/workflows/` carries a
   `timeout-minutes`. A test or lint asserts this over the directory, so a new
   workflow cannot arrive without one. Count today: zero.
2. The required gate's wall clock stays inside a stated budget, and a run
   exceeding it fails as a budget violation rather than passing slowly.
3. A scheduled long-run lane exists, is distinct from the required gate, and
   hosts at least one run measured in hours end to end.
4. The full suite runs on Linux, macOS and Windows as **required** checks, with
   the display and HDL-simulator suites armed rather than best-effort, and a
   coverage floor on each.
5. Fixtures above a stated size have a declared storage mechanism and a CI guard
   that fails when a fixture exceeds it without using that mechanism. Today
   there is no policy and no Git LFS.
6. The GUI boots under a headless compositor in CI, is screenshotted, and the
   findings are published as a document rather than as a green check.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | closes |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | closes |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | closes - shared with FEAT-008 via TASK-0018 |
| - | job timeouts, the long-run lane and the large-fixture policy | **no issue** |

## Design notes

The starting state is more built than the issue titles suggest, and the task
author must not re-do what exists. At HEAD `.github/workflows/` holds six files -
`ci.yml`, `codeql.yml`, `mutation.yml`, `release.yml`, `repro-installers.yml`,
`scorecard.yml`. `ci.yml` already defines a Linux matrix job (`:29-31`), a
Windows job (`:144-146`), a macOS JDK-25 job (`:260-262`) and an Agda proof job
(`:305-307`), and it already has a nightly cron at `:12-13` (`"17 4 * * *"`)
whose concurrency group is separated at `:24` so a cron firing never cancels a
push run. So the macOS lane in #265 **exists**; what does not exist is its
promotion to a required check and the arming of its suites - the HDL toolchain
step on both Windows (`:194`) and macOS (`:284`) is labeled "best-effort".

What is genuinely absent is uniform: `grep -rn "timeout-minutes"` over
`.github/workflows/` returns nothing. Six workflow files, zero timeouts, against
a six-hour hosted ceiling.

The nightly cron is the seed of the long-run lane rather than the lane itself -
today it runs only the GUI-Wayland leg (`ci.yml:4-7`), and every other job is
gated `if: github.event_name != 'schedule'`. TASK-0016 widens it, and widening it
is where the fixture-size policy becomes unavoidable: a 2.4 MiB kernel image is
a 33 MB `.jls` at the measured 15.87 bytes per word, and there is no Git LFS and
no declared policy. That is the same decision TASK-0071 must make about where a
guest image lives, and the same one FEAT-009 must make about the calibration
fixture - which is why TASK-0016 is shared rather than duplicated.

"Required check" is a repository setting, not a file. Criterion 4 is therefore
satisfied by a settings change plus the evidence that the lane is green enough
to be required; the plan cannot assert the setting from the tree.

## Risks

- **Promoting a lane to required makes its flakiness everyone's problem.** The
  measured precedent is PR #233 - a pull request, not an issue: zero-margin
  per-package coverage floors
  flake across the JDK matrix (`jls.collab.op` branch measured 0.768 against a
  0.770 floor on JDK 26), which is why every floor now keeps a point of
  headroom (`pom.xml:400-408`). The same discipline applies to the platform
  lanes: promote with headroom or promote flake.
- **A long-run lane costs hosted minutes continuously.** A nightly multi-hour
  run is a standing budget commitment, and the plan should state whose. This is
  the strongest argument for the lane hosting one *representative* run rather
  than the whole capstone suite.
- **UNOWNED at 3-6 mw.** No committed program pays for CI structure, and it is a
  prerequisite of four capstones. It is the clearest case in the plan of shared
  infrastructure with no owner.

## Evidence

- Six workflow files: `.github/workflows/ci.yml`, `codeql.yml`, `mutation.yml`,
  `release.yml`, `repro-installers.yml`, `scorecard.yml`.
- Zero timeouts: `grep -rn "timeout-minutes" .github/workflows/` returns no
  matches at HEAD.
- Existing lanes: `.github/workflows/ci.yml:29-31` (Linux matrix), `:144-146`
  (Windows), `:260-262` (macOS JDK 25), `:305-307` (Agda proofs), each gated
  `if: github.event_name != 'schedule'`.
- Nightly cron and its isolation: `.github/workflows/ci.yml:4-7`, `:12-13`,
  `:21-24`.
- Best-effort HDL arming: `.github/workflows/ci.yml:194` (Windows), `:284`
  (macOS).
- Required-gate duration 141 s, six-hour hosted ceiling, no Git LFS and no
  large-fixture policy, 15.87 bytes per word: `BRIEF.md` §7.
- Floor-flake precedent (PR #233, not an issue) and the headroom convention: `pom.xml:400-408`.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S14.
- **Cost reconciliation.** Band 3-6 mw. Tasks named for it: TASK-0015,
  TASK-0016, TASK-0017, TASK-0018, totalling 4.7 wk. Band and task sum agree;
  no reconciliation is needed. Shared tasks counted once at the task level:
  TASK-0016, TASK-0018.
