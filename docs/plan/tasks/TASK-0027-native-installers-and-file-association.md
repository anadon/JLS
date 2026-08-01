# TASK-0027 - Native installers per OS with file association

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

**Correction to the registry scope, recorded rather than hidden: the installer
pipeline exists at HEAD and is substantially complete.** `scripts/build-installer.sh`
runs jar to `jdeps` to `jlink` to `jpackage` for deb, rpm, AppImage, msi and
dmg, with `--file-associations` wired per platform and real per-platform icons;
`.github/workflows/release.yml`'s `installers` job runs it on a five-leg matrix
and installs-and-smoke-tests each artifact on the runner that built it. Issue
#82 is nevertheless open, and the residual is precise. This task closes exactly
that residual.

1. **Flip the four experimental legs to required.** `release.yml`'s matrix runs
   `ubuntu-24.04-arm`, `windows-latest`, `windows-11-arm` and `macos-latest`
   under `continue-on-error: ${{ matrix.experimental }}` with
   `experimental: true`, pending one green run of the smoke-test step. The
   job's own comment states the promotion condition verbatim: "flip experimental
   to false then." Deliverable: one `workflow_dispatch` dry run (or tag push)
   green across all five legs, the flags flipped, and the manual clean-VM
   verification matrix in #82 §7 retired against that run rather than repeated
   by hand.
2. **Arm the JetBrains Runtime pin.** `scripts/build-installer.sh:298-300`
   carries `JBR_SHA256="UNVERIFIED-PLACEHOLDER-fill-in-real-sha256-see-issue-101"`
   for both Linux platforms, so the Linux legs fall back to the build JDK's
   jlink image with a warning and ship an X11/XWayland-only installer. Compute
   the two digests (`curl -fsSL <JBR_URL> | sha256sum`, the recipe is at
   `:282-284`), commit them, and confirm the bundled runtime is the JBR one.
   This is the difference between "Wayland works if the user brings the right
   JDK" and "Wayland works out of the installer".
3. **Record the per-OS artifact sizes.** #82 P4 asks for it and nothing in the
   tree carries it. Add the five sizes to the README install section or to a
   `docs/` note, with the JDK and the jlink module set they were measured
   against, so a future size regression is visible.
4. **Close the loop on the association claim.** README:48-50 states that
   installing associates `.jls` files. The deb leg's smoke test already checks
   the mime association and the desktop `%f` field code; the msi leg does a
   real per-user `msiexec` install/uninstall; the dmg leg does `hdiutil attach`
   plus a launcher run. Extend the msi and dmg smoke tests to assert the
   association itself, so all three OSes assert the property README claims.
5. **Uninstall cleanliness.** #82 §10 flags that file association writes OS
   state and requires clean uninstall. The msi leg already uninstalls; add the
   assertion that the association is removed, and record the deb/rpm/dmg
   position explicitly (including "not asserted, and why") rather than leaving
   it unstated.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-010 | The delivery half: a student with no Java knowledge installs from the Releases page and double-clicks a `.jls`. TASK-0028 owns the reproducibility and signing half |

## Prerequisite tasks

None. Every artifact this task promotes already builds at HEAD.

## Acceptance test

The primary acceptance evidence is a workflow run, and that is stated
deliberately: an installer that installs is not a property a unit test can
assert. Two in-repo assertions bound it.

`test/jls/InstallerMatrixPolicyTest.java`, new - a workflow-drift test in the
family of `test/jls/CliFlagTableTest.java`:

- `noInstallerLegRemainsExperimental()` - parses `release.yml`'s `installers`
  matrix and asserts every entry has `experimental: false`. **Fails at HEAD for
  four of five legs.** This is what turns "we meant to flip it" into a gate.
- `everyLegDeclaresASmokeTestStep()` - asserts each leg reaches the "Install and
  smoke-test the installer" step, and that the matrix declares at least five
  legs (anti-vacuity: a matrix emptied by a bad edit must not pass).
- `theJbrPinIsArmed()` - asserts `scripts/build-installer.sh` contains no
  `UNVERIFIED-PLACEHOLDER` token and that both `JBR_SHA256` values match
  `^[0-9a-f]{64}$`. **Fails at HEAD.**

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 82 | Distribution: jpackage installers per OS and `.jls` file association - remove the bring-your-own-JDK barrier | closes - this task is exactly its residual |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | depends on - #101 owns the JBR placeholder convention that item 2 discharges; the same two digests serve both |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | overlaps - the Windows promotion argument is the same argument on a different job |
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps - same, for macOS |

## Notes

- **Do not rebuild what exists.** The single most expensive mistake available
  here is re-deriving `build-installer.sh`. Read it first; it is 500-odd lines
  and it already encodes the `--app-version` numeric constraint on Windows
  (`:68`), the AppImage `--license-file`/`--file-associations` rejection
  (`:207`), the `.DirIcon` symlink convention (`:225-226`) and the mime type
  (`:235`).
- **The trap is the promotion, not the build.** Flipping `experimental` to
  `false` makes four legs able to fail a release. Do it after a green dry run,
  not before, and do it in one commit so a revert is one commit.
- **`riscv64` is out of scope on purpose.** No GitHub runner exists and
  `jpackage` cannot cross-compile its launcher; RISC-V is served by the
  container image and the architecture-independent jar
  (`release.yml:286-289`). Do not reopen this; it is a hardware fact.
- **Windows aarch64 uses Zulu, not Temurin**, because Temurin publishes no
  Windows-aarch64 JDK 25 (`release.yml:304-306`). A promotion commit that
  normalizes the matrix to one vendor will break that leg.
- **macOS signing is TASK-0028's, not this task's.** #82 §10 documents the
  Gatekeeper right-click-Open path as the accepted stance; do not silently
  upgrade the claim here.
- **Timeouts.** These are the longest jobs in the tree and none of them has a
  `timeout-minutes` at HEAD. TASK-0015 owns that; promoting a leg to required
  without it converts a stalled download into a six-hour block on a release.

## Evidence

- `scripts/build-installer.sh` at HEAD - the whole recipe; `:58` the
  `jpackage --version` probe, `:171-190` the `jpackage` invocation, `:207`
  the AppImage constraint, `:217-235` the icon/desktop/mime block, `:289-300`
  the JBR version, URL and the two placeholder digests, `:354-363`,
  `:408-410`, `:476-477` the three `--file-associations` sites.
- `resources/packaging/` - `jls.png`, `jls.icns`, `jls.ico` and the three
  `jls-association-*.properties` files, referenced by the lines above.
- `.github/workflows/release.yml:249-330` - the `installers` job comment
  recording the verification status and the promotion condition, and the
  five-leg matrix with its `experimental` flags.
- `.github/workflows/repro-installers.yml:1-40` - the report-only probe that
  measures the same legs monthly.
- `README.md:12-22` - the install section already written around installers;
  `:48-50` - the file-association claim this task's smoke tests must back.
- Issue #82 §5 P1-P4, §7 and §10 - the predictions, the method checklist and
  the signing/uninstall threats, read this session.
