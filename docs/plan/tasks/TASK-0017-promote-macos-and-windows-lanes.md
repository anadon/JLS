# TASK-0017 - Promote the macOS and Windows headless lanes to required

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0015

## Deliverable

The full suite runs on macOS and Windows as a blocking check, with the HDL and
display suites armed rather than best-effort, and with the coverage ratchet
evaluated rather than skipped.

1. **Drop `continue-on-error`.** `macos` carries `continue-on-error: true`
   (`.github/workflows/ci.yml:263`) and `windows` carries it at the job level
   (`:156`), so both JDK legs of Windows are advisory. Remove both, keeping the
   JDK-26 leg advisory on Windows exactly as `build` does it — a matrix-scoped
   `continue-on-error: ${{ matrix.java != 25 }}` (`:41`) rather than a job-wide
   one. The two check names are already byte-stable for branch-protection
   registration: `Build (Windows, JDK 25)` (`:144`, and the comment at
   `:135-140` says it was made stable for exactly this promotion) and
   `Build (macOS, JDK 25)` (`:260`).

2. **Follow the recorded promotion rule, with the record.** `gui-wayland` was
   promoted by a written 20-consecutive-green record enumerated by run id in
   the workflow itself (`:330-346`). Do the same here: the promotion commit
   carries the run ids, the intervening non-green events, and their causes.
   That comment block is the template; do not promote on a hunch.

3. **Arm the toolchains fail-closed.** Both lanes install their HDL
   simulators best-effort and emit a `::notice` on failure —
   `brew install icarus-verilog ghdl yosys` on macOS (`:284-292`) and the
   pinned oss-cad-suite download on Windows (`:194-213`). While the lane is
   advisory that degradation is correct. Once required it is not: a skipped
   toolchain silently drops the `IverilogCompileTest` / `GhdlCompileTest` /
   yosys-armed legs, which changes what "green" means and, if JaCoCo is also
   enabled, moves coverage under a floor for a reason that has nothing to do
   with the change under test. Make the install failure fail the job, and keep
   the pinned digest check (`OSS_CAD_SHA256`, `:173`) fail-closed as it already
   is for a mismatch.

4. **Arm the display suite on macOS and keep it armed on Windows.** Windows
   already passes `-Djls.test.headless=false` (Stage W3, `:135-141`); macOS
   runs `mvn -B verify -Djacoco.skip=true` (`:295-297`) with no display
   property, so the 25 `@Tag("display")` classes self-skip via
   `assumeFalse(GraphicsEnvironment.isHeadless())`. macOS runners have a real
   window server; pass the property and let them run.

5. **Stop skipping the coverage ratchet, but never raise floors from these
   lanes.** macOS runs with `-Djacoco.skip=true` (`:296`) and Windows keeps
   the ratchet skipped as Stage W4 (`:140-142`). Enable evaluation on both.
   `CONTRIBUTING.md:90-99` is binding on the other half: floors are raised
   **from the canonical JDK 25 headless Linux run only**, so these lanes
   *check* the floors and never *set* them. The pom comment gains that
   sentence.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-007 | "The full suite is a required check on all supported platforms" is the feature's third clause and the whole of issues #265 and #111. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0015 | A required job with no `timeout-minutes` converts a hung macOS or Windows runner — the two platforms with the network-dependent toolchain installs — into a six-hour block on every pull request. The timeout must exist before the check blocks, not after. |

## Acceptance test

`test/jls/PlatformLaneRatchetTest.java`, new (the `*RatchetTest` family in
`test/jls/`):

- `noPlatformBuildJobIsJobWideAdvisory()` — scans `.github/workflows/ci.yml`
  and asserts that neither the `macos` nor the `windows` job block contains a
  job-level `continue-on-error: true`, while allowing the matrix-scoped
  expression form. **Fails at HEAD** on both.
- `everyPlatformBuildJobRunsTheCoverageRatchet()` — asserts no
  `-Djacoco.skip=true` in the `macos` or `windows` build step.
- `everyPlatformBuildJobArmsTheDisplaySuite()` — asserts both pass
  `-Djls.test.headless=false`.
- `toolchainInstallIsFailClosedOnRequiredLanes()` — asserts the macOS `brew`
  step and the Windows oss-cad-suite step contain no `exit 0` fallback on the
  install failure path.

The substantive proof is the CI run itself: the first green
`Build (macOS, JDK 25)` and `Build (Windows, JDK 25)` with the ratchet
enabled, the display suite armed and no `continue-on-error`. Record both run
ids in the promotion commit alongside the 20-run stability record.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | closes |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg (ex: Windows test-suite failures, fixed) | closes — Stages W1, W4 and the required-check registration; W2/W3/W5 already shipped |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | informs — its promotion record (`ci.yml:330-346`) is the procedure this task follows |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | overlaps — this task arms #162's suite on two more substrates; it does not close it |

## Notes

- **The #233 flake is the specific hazard.** `CONTRIBUTING.md:83-89` records a
  `jls.collab.op` BRANCH floor of 0.770, set from a JDK 25 run, measuring
  0.768 on JDK 26 and turning the build red for no code reason. Cross-platform
  jitter is larger than cross-JDK jitter, so the floors need at least the
  documented epsilon before they are evaluated on two more operating systems —
  and the honest first step is to *measure* the three platforms' spread and
  widen the epsilon from data, not to assume the existing headroom suffices.
- **Windows already runs the display suite as advisory first light** and
  `ci.yml:135-138` warns that some display tests carry "Xvfb/WM-less timing
  and pointer-exclusivity workarounds" for the Robot-driven flows, explicitly
  **not** pre-muted. Promotion has to adjudicate each such failure, not tag it
  out. #265 Stage 2 owns that taxonomy; this task consumes it.
- **`rerunFailingTestsCount = 2`** on the display execution
  (`pom.xml:288-294`) will mask genuine cross-platform flakiness on the
  promoted lanes. Decide out loud whether it stays; the recommendation is that
  it stays for one release and then drops, with the flake list published.
- **The `-text` gitattributes exist because of this lane.**
  `.gitattributes:1-11` cites issue #111: a CRLF rewrite on checkout silently
  changed what byte-for-byte goldens compared against. Any new golden added
  while this task is in flight must land under a `-text` path.
- **Two full platform lanes double runner minutes on every PR.** Say so; that
  is the displacement this task costs, and the fast/long-run split
  (TASK-0016) is what keeps the required half short.

## Evidence

- `.github/workflows/ci.yml:143-156` — the `windows` job, its JDK matrix and
  its job-wide `continue-on-error: true` at `:156`; `:164-173` the pinned
  oss-cad-suite URL and SHA-256; `:194-213` the download and its
  `::notice`-and-skip failure path.
- `:127-142` — the Windows lane comment: Stage W2 (HDL armed), W3 (display
  first light, `-Djls.test.headless=false`), W4 (JaCoCo ratchet "stays
  skipped"), W5 (JDK-26 leg), and the byte-stable check name.
- `:259-297` — the `macos` job: `continue-on-error: true` at `:263`, the
  best-effort `brew` step at `:284-292`, and
  `mvn -B verify -Djacoco.skip=true` at `:295-297`.
- `:28-41` — the `build` job's matrix-scoped advisory expression, the form to
  copy.
- `:330-346` — the `gui-wayland` promotion record: the 20-run enumeration by
  id, the intervening cron and the one non-lane failure, and the rule that
  promotion drops `continue-on-error` and the maintainer then registers the
  check name.
- `pom.xml:261-296` — the two surefire executions and the display retry count.
- `CONTRIBUTING.md:83-99` — the #233 zero-margin incident, the epsilon rule,
  and "raise floors from the canonical JDK only" / "from headless numbers
  only".
- 25 classes carry `@Tag("display")` at HEAD, all under `test/jls/ui/`.
