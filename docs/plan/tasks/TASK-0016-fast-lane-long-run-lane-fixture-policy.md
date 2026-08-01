# TASK-0016 - Split CI into a required fast lane and a long-run lane, with a fixture policy

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0015

## Deliverable

The required gate stays short, runs measured in hours move to a scheduled lane
of their own, and a fixture above a stated size has a declared home and a check
that enforces it.

1. **A `longrun` test tag with its own surefire execution.** `pom.xml`'s
   surefire plugin already carries exactly this shape for the display suite:
   a `default-test` execution with `<excludedGroups>display</excludedGroups>`
   and a second `display-tests` execution with `<groups>display</groups>`
   (`pom.xml:261-296`). Add a third execution, `longrun-tests`, bound to
   `<groups>longrun</groups>` and **skipped by default** behind a property;
   add `longrun` to `default-test`'s excluded groups. The `excludedGroups`
   element takes a comma-separated list — getting that wrong is silent, so the
   ratchet below asserts it.

2. **A scheduled long-run workflow.** `.github/workflows/longrun.yml`, on
   `schedule` plus `workflow_dispatch` and never on `push`/`pull_request`,
   modeled on `mutation.yml` — which is already a blocking-but-not-required
   weekly lane (`mutation.yml:1-24`, cron `0 5 * * 0`) and is the precedent to
   copy rather than reinvent. It carries a `timeout-minutes` sized for the
   lane (TASK-0015 supplies the convention), uploads its artifacts on failure,
   and turns red on a breach rather than warning.

3. **The required gate's budget is stated and enforced.** Add to
   `CONTRIBUTING.md` §"Coverage ratchet" a sibling section giving the required
   lane's wall-clock budget and the rule that a test which cannot meet it
   carries `@Tag("longrun")`. The ratchet test asserts no test class outside
   the tagged set declares a timeout beyond the budget.

4. **A fixture-size policy, written and checked.** At HEAD there is no Git LFS
   and no size rule. `.gitignore:8-10` ignores `*.jls` and exempts
   `test/fixtures/**/*.jls`; `.gitattributes:1-5` marks `*.jls` and
   `test/resources/**` as `-text` so they are never CRLF-rewritten. The policy
   states: (a) a size cap for a newly tracked fixture; (b) the mechanism for
   anything above it — generate-at-test-time from a committed generator, or an
   external artifact fetched by the long-run lane only, **not** a new binary in
   the repository; (c) that the cap is a raise-only ratchet like the coverage
   floors. `CONTRIBUTING.md` carries the rule; a test enforces it.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-007 | "Long runs have their own lane" is the feature's second clause. Without it, every long-run acceptance test in the plan (TASK-0080's boot run, TASK-0100's nightly analog corpus, TASK-0075's checkpoint round-trip) has nowhere to run. |
| FEAT-009 | The calibration fixture TASK-0025 commits is exactly the artifact the size policy governs, and the measurement runs TASK-0022 and TASK-0023 produce are exactly the workload the long-run lane hosts. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0015 | The long-run lane's whole premise is that it may take hours while the required lane may not. That distinction is unexpressible while every job shares one implicit 360-minute ceiling: this task writes `timeout-minutes` values that TASK-0015 establishes the convention and the totality test for. |

## Acceptance test

`test/jls/LongRunLaneRatchetTest.java`, new:

- `longRunTaggedTestsAreExcludedFromTheRequiredExecution()` — reads `pom.xml`,
  locates the `default-test` execution, and asserts its `excludedGroups`
  contains both `display` and `longrun` as list members (not as a substring —
  the point is to catch `displaylongrun`).
- `everyLongRunTaggedClassIsInTheDeclaredSet()` — an ArchUnit rule
  (`ClassFileImporter`, the `DialogCoverageRatchetTest` idiom,
  `test/jls/ui/DialogCoverageRatchetTest.java:60-89`) asserting that a class
  carrying `@Tag("longrun")` lives under the packages the policy names, so the
  tag cannot be used to quietly exempt an ordinary slow test.

`test/jls/FixturePolicyRatchetTest.java`, new:

- `noTrackedFixtureExceedsTheDeclaredCap()` — walks `test/fixtures/` and
  `test/resources/`, asserts every file is at or under the cap, and names the
  offenders with their sizes. **Green at HEAD** by construction (that is the
  point: it is a ratchet, not a bug report) and it must be *shown* to fail by
  a temporary oversized file during review.
- `theCapIsStatedInContributing()` — asserts the number in the test and the
  number in `CONTRIBUTING.md` agree, so the documented rule and the enforced
  rule cannot drift.

## Related GitHub issues

**No issue** for the lane split or the fixture policy.

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on — #202's integration golden is the first fixture the size policy has to adjudicate, and its run is the first candidate for the long-run lane |
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps — #265 decides which lanes are required; this task decides what a required lane is allowed to cost |

## Notes

- **The JaCoCo agent appends to `target/jacoco.exec`.** `CONTRIBUTING.md:100-105`
  records this the hard way: "a floor that should trip never will" after an
  unclean rerun. A long-run execution in the same reactor would union its
  coverage into the floors and make them permanently unmeetable by the
  required lane. Either give the long-run lane `-Djacoco.skip=true` or a
  separate destination file, and say which in the pom comment.
- **`rerunFailingTestsCount` is set to 2 on the display execution**
  (`pom.xml:288-294`) because Xvfb popup realization is nondeterministic. Do
  **not** copy that onto the long-run execution: a multi-hour test that is
  retried twice costs three times as much and hides a real flake.
- **The fixture cap must not be justified by GitHub's limits.** State it in
  terms of what a clone costs a student on a slow link — that is the
  constraint the project actually has (`CONTRIBUTING.md` "Getting started"
  says JDK and Maven are the only requirements).
- **Git LFS is a decision, not a default.** It adds a client-side requirement
  to `git clone`, which cuts against the same constraint. If the policy adopts
  it, the recommendation must say so explicitly and cost the onboarding
  change; the alternative — regenerate large fixtures from a committed
  generator — is cheaper and is the one to prefer unless a byte-exact binary
  is the thing under test.
- **`*.jls -text` and `test/resources/** -text`** (`.gitattributes:1-11`) exist
  because a CRLF rewrite silently changed what byte-for-byte goldens compared
  against (issue #111). Any new fixture location must inherit that attribute
  in the same commit, or the Windows lane (TASK-0017) breaks in a way that
  looks like a test bug.
- **A cap that has never been seen to fail should be assumed vacuous** —
  `CONTRIBUTING.md:103-105` states that rule for coverage floors and it applies
  here verbatim.

## Evidence

- `pom.xml:261-296` — the surefire plugin: the `default-test` execution with
  `<excludedGroups>display</excludedGroups>` and `-Djava.awt.headless=true`,
  and the `display-tests` execution with `<groups>display</groups>`,
  `-Djava.awt.headless=${jls.test.headless}` and
  `<rerunFailingTestsCount>2</rerunFailingTestsCount>`.
- `.github/workflows/mutation.yml:1-24` — a scheduled, on-demand, blocking but
  never-required lane; the structural precedent.
- `.github/workflows/ci.yml:8-13` — the nightly cron that runs only
  `gui-wayland`; `:29-30` and every sibling's
  `if: github.event_name != 'schedule'` — the existing mechanism for
  restricting a lane to a schedule.
- `CONTRIBUTING.md:69-125` — the coverage-ratchet section, the raise-only
  convention, the `mvn clean verify` requirement and the vacuous-floor rule.
- `.gitignore:8-10` — `*.jls` ignored, `test/fixtures/**/*.jls` exempted.
- `.gitattributes:1-11` — `*.jls -text`, `*.jls~ -text`,
  `test/resources/** -text`, each with its issue-#111 rationale.
- `test/jls/ui/DialogCoverageRatchetTest.java:60-89` — the ArchUnit
  `ClassFileImporter` ratchet idiom this task reuses.
- `test/fixtures/` at HEAD: four entries (`fork-4.6-shiftregister.jls`,
  `headless-canary-gate.jls`, `legacy-4.1`, `riscv-sum1to10.jls`).
