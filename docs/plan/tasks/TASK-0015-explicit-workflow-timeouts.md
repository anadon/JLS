# TASK-0015 - Explicit timeouts on every workflow job

**Status:** proposed | **Cost:** 1 d | **Blocked by:** none

## Deliverable

Every job in every workflow declares `timeout-minutes`, and a standing test
fails the build when a new job does not.

1. **The inventory, all of which is untimed at HEAD.**
   `grep -rn "timeout" .github/workflows/` returns **nothing**: there is not one
   `timeout-minutes` in the tree. Twenty-three jobs across six files:

   | workflow | jobs |
   |---|---|
   | `ci.yml` | `build`, `windows`, `macos`, `proofs`, `gui-wayland`, `gui-x11`, `macos-gui`, `windows-gui`, `dependency-submission`, `reproducibility`, `installer-reproducibility`, `installer-reproducibility-aarch64`, `windows-installer-msi`, `macos-installer-reproducibility` |
   | `codeql.yml` | `analyze` |
   | `mutation.yml` | `mutation-report` |
   | `release.yml` | `release`, `maven-registry`, `container-image`, `installers`, `verify-windows-signatures` |
   | `repro-installers.yml` | `probe` |
   | `scorecard.yml` | `analysis` |

2. **A `timeout-minutes` on each, chosen from its observed runtime, not
   guessed.** Read the last ten green runs of each job, take the maximum, and
   set the timeout at roughly twice it, rounded up. Record the observed
   maximum in a comment beside the value so the next person raising it has the
   basis. Two classes need explicit thought and a comment:
   - the **network-dependent** jobs (`gui-wayland`'s JBR download,
     `windows`'s oss-cad-suite download, `macos`'s `brew install`) — a stalled
     download is the failure mode this task exists to bound;
   - the **installer and reproducibility** jobs, which do two full builds by
     construction and are legitimately the longest.

3. **A step-level timeout on every network fetch.** Job timeouts bound the
   total; the four download steps get their own `timeout-minutes` so a hang is
   attributed to the download rather than to the build.

4. **A totality ratchet.** `test/jls/WorkflowTimeoutRatchetTest.java`, in the
   established `*RatchetTest` family (`test/jls/NullMarkedRatchetTest.java`,
   `PackageInfoRatchetTest.java`, `HeadlessCoreRatchetTest.java`), scans
   `.github/workflows/*.yml` and asserts every job key carries
   `timeout-minutes`. There is no YAML parser on the classpath — the
   dependencies are xz, JFreeSVG, FlatLaf, JSpecify, JUnit, ArchUnit and the
   JUnit platform launcher (`pom.xml:60-125`) — so the scan is a line matcher
   over the two-space job-key indentation the six files already use uniformly.
   Its own fragility is the thing to document: the test must fail loudly if it
   finds **zero** jobs in a file, so a reformat cannot silently make it vacuous.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-007 | "Every workflow has an explicit timeout" is the first clause of the feature. It is also the precondition for TASK-0016: a long-run lane cannot be given a large budget honestly while the required lane has an implicit six-hour one. |

## Prerequisite tasks

None.

## Acceptance test

`test/jls/WorkflowTimeoutRatchetTest.java`, new:

- `everyWorkflowJobDeclaresATimeout()` — enumerates the six workflow files,
  extracts every job key under `jobs:`, and asserts each block contains a
  `timeout-minutes:` line. Reports the offenders by `file:job` so a failure is
  actionable without opening the YAML. **Fails at HEAD for all 23 jobs.**
- `theScanFindsTheExpectedNumberOfWorkflowsAndJobs()` — asserts six files and
  a non-zero job count per file. This is the anti-vacuity clause: without it a
  scanner that silently matched nothing would pass forever.
- `everyNetworkFetchStepDeclaresAStepTimeout()` — asserts the four named
  download steps carry their own `timeout-minutes`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | depends on — promoting a lane to required (TASK-0017) without a timeout converts a hung runner into a six-hour block on every PR |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | depends on — same reason, for the Windows lane |

No issue covers CI timeouts themselves. The study corpus records the gap
(`BRIEF.md` §7, "no `timeout-minutes` anywhere; 6-hour hosted ceiling").

## Notes

- **The absence is not "no limit", it is "six hours, silently".** GitHub's
  default job timeout on hosted runners is 360 minutes. The concrete cost is
  that a wedged job burns the full ceiling and, because
  `concurrency.cancel-in-progress` (`ci.yml:22-25`) only cancels on a *new*
  push to the same ref, a hang on master is not cancelled by anything.
- **`continue-on-error: true` does not help.** `windows` (`ci.yml:156`),
  `macos` (`:263`), `macos-gui` (`:598`) and the JDK-26 leg of `build`
  (`:41`) are advisory, which means their *result* is ignored — their
  *runtime* is not. An advisory hang still costs six hours of runner time and
  still shows as in-progress.
- **Do not set a uniform value.** `proofs` (an Agda check) and
  `installer-reproducibility-aarch64` (two full builds under emulation) differ
  by more than an order of magnitude; one number would be either useless or
  a source of false failures.
- **`release.yml` deliberately has no concurrency group** (`ci.yml:19-21`
  records this). Its jobs are therefore the ones most exposed to a hang, and
  are the ones whose timeouts should be set most deliberately.
- **This is the cheapest item in CAP-00 and the one with the widest blast
  radius.** It changes no behavior on a green run at all, which is why the
  ratchet matters more than the values: without the test, the next added job
  reintroduces the gap.

## Evidence

- `grep -rn "timeout" .github/workflows/` — no matches at HEAD; verified this
  session against `b54e6ee`.
- Job inventory extracted from `.github/workflows/*.yml`: `ci.yml:28, 143,
  259, 304, 353, 501, 594, 722, 764, 798, 866, 923, 1000, 1064`;
  `codeql.yml`, `mutation.yml`, `release.yml`, `repro-installers.yml`,
  `scorecard.yml` (one to five jobs each; 23 total).
- `ci.yml:19-25` — the concurrency group and `cancel-in-progress`, and the
  note that `release.yml` has none.
- `ci.yml:41` (advisory JDK-26 leg), `:156` (advisory Windows job), `:263`
  (advisory macOS job), `:598` (advisory macOS GUI job).
- Network fetch steps: `ci.yml:415-445` (JBR download), `:194-213`
  (oss-cad-suite download, pinned by `OSS_CAD_SHA256` at `:173`), `:284-292`
  (`brew install icarus-verilog ghdl yosys`), `:72-73` and `:404-405`
  (`apt-get` for the HDL toolchain and the Wayland rig).
- `pom.xml:60-125` — the full dependency list; no YAML parser.
- Ratchet-test precedent: `test/jls/NullMarkedRatchetTest.java`,
  `test/jls/PackageInfoRatchetTest.java`,
  `test/jls/HeadlessCoreRatchetTest.java`.
