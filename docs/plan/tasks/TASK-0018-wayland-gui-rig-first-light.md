# TASK-0018 - Wayland GUI rig first light

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** none

## Deliverable

> **Scope correction, verified at HEAD.** The rig is *built and green*. The
> `gui-wayland` job boots JLS on JetBrains Runtime's `WLToolkit` under headless
> sway, screenshots it, uploads the artifacts and runs nightly
> (`.github/workflows/ci.yml:322-464`, `scripts/wayland-rig.sh`), and it has
> been a **hard gate** since a written 20/20 green record (`ci.yml:330-346`).
> First-light findings are published — `docs/wayland-desktop-checklist.md` and
> the gsettings-schemas finding recorded at `ci.yml:401-403`. What remains of
> #101 is two placeholders that the workflow itself marks as unfinished. This
> task is those two, plus the registration; it is not a rebuild.

1. **Pin the JBR digest, fail-closed.**
   `JBR_SHA256: "UNVERIFIED-PLACEHOLDER-fill-in-real-sha256-see-issue-101"`
   (`ci.yml:380`). The download step already prints the observed digest on
   every run (`:429`) and already verifies fail-closed when the value is not a
   `UNVERIFIED-` string (`:430-437`). Read the digest from a green log or from
   an unproxied `curl -fsSL "$JBR_URL" | sha256sum` (`:372`), paste it, and
   delete the placeholder branch's `::notice` (`:438-441`) so an unpinned
   runtime cannot silently return.

2. **Calibrate and arm the pixel gate.** `PIXEL_DIFF_MIN: "0"` with
   `TODO(maintainer)` (`ci.yml:381-385`): the decision recorded on #101 is to
   set it from the observed ImageMagick AE metric in the `pixel-diff.txt`
   artifact (`scripts/wayland-rig.sh:20, 35-37, 46, 60`), at roughly 10% of
   it, rather than from a blind 1% guess. Read the AE from the most recent
   green run's artifact, set the value, and record the observed number and the
   date in the comment beside it. With the gate armed, a JLS window that maps
   but renders blank turns the lane red — which is the failure mode a
   screenshot-only rig cannot see.

3. **Register the required check.** The workflow comment states the remaining
   step explicitly: "the maintainer then registers the NEW job name below
   ('GUI boot (Linux, Wayland/JBR)') as a required branch-protection check
   (repo settings)" (`ci.yml:344-346`). The same sentence appears for
   `GUI boot (Linux, X11/Xvfb)` (`:496-500`). Do both, and record in the
   commit that they are registered, since the setting lives outside the
   repository and is otherwise invisible to a reader.

4. **Close #101 against its own sections.** The issue's P2 (pixel gate) and P3
   (stability record) are the two above; section 9's exit-code classification
   (0 pass / 1 JLS-side / 2 upstream) is implemented and self-tested
   (`scripts/wayland-rig-selftest.sh`, armed at `ci.yml:412-413`). Write the
   closing comment against those section numbers rather than a bare "done".

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-007 | The Wayland lane is one of the platform-parity checks; an unpinned toolchain download and a disarmed pixel gate are the two ways it can be green while proving less than it appears to. |
| FEAT-008 | The rig is the only substrate on which a *blank-window* editor regression is detectable at all. Arming the pixel gate is what makes the editor decomposition (TASK-0019, TASK-0020) observable end to end rather than only through in-process UI tests. |

## Prerequisite tasks

None.

## Acceptance test

`test/jls/WaylandRigPinRatchetTest.java`, new (the `*RatchetTest` family in
`test/jls/`, alongside the existing `test/jls/WaylandStartupCliTest.java`):

- `theJbrDigestIsPinnedNotAPlaceholder()` — reads `.github/workflows/ci.yml`,
  finds `JBR_SHA256`, and asserts the value is 64 lowercase hex characters.
  **Fails at HEAD**: the value begins `UNVERIFIED-`.
- `thePixelGateIsArmed()` — asserts `PIXEL_DIFF_MIN` parses as a positive
  number. **Fails at HEAD**: it is `"0"`.

`scripts/wayland-rig-selftest.sh`, extended with two cases against the stub
toolchain it already drives (`ci.yml:406-413`):

- a digest mismatch exits non-zero and never extracts the archive;
- an AE below `PIXEL_DIFF_MIN` exits with the JLS-side code (1), not the
  environment code (2), because a blank window is a JLS failure and the
  section-9 classification is the rig's whole contract.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | closes — sections P2 and P3 and the required-check registration; the rig, the screenshots and the published findings already shipped |
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps — the same required-check registration step, for different lanes |

Recorded decisions, closed, cite as such: **#100** (the Wayland spot-check
prediction P2 that `docs/wayland-desktop-checklist.md:1-14` implements),
**#105** (the JBR release choice), and **PR #244** (the coverage-floor
reconciliation whose measurement record the promotion record cites). #244 is a
**pull request, not an issue**; cite it as such.

## Notes

- **Do not "re-do first light".** The registry title predates the merge that
  landed the lane. Rebuilding a rig that is already gating would cost a week
  and buy nothing; the two placeholders are the residual and they are named in
  the workflow as such. Recorded here so the next reader does not scope from
  the title.
- **The unpinned path is deliberate today, not an oversight.** `ci.yml:373-379`
  argues it: TLS to the JetBrains CDN is the trust boundary and the checksum
  is hardening on top, so the lane was not blocked on a hash it could not
  fetch from the authoring sandbox (`:377-378`: egress-policy 403). Pinning is
  therefore a strict improvement, not a correction of a mistake, and the
  comment should be edited rather than deleted.
- **A CDN outage must stay non-fatal.** `:422-427` sets `skip=true` and exits 0
  on a download failure, so the gate is not wedged by JetBrains. Pinning must
  not change that: a *mismatch* is fatal, an *outage* is a skip. The two paths
  are already distinct in the script; keep them so.
- **The X11 lane is the twin and has the same open registration.**
  `gui-x11` (`ci.yml:466-593`) is also a hard gate on a 20/20 record and also
  awaits branch-protection registration (`:496-500`). Do both in one commit;
  they are one repo-settings edit.
- **`PIXEL_DIFF_MIN` gates only when set** (`scripts/wayland-rig.sh:35-37`),
  and `compare` is optional otherwise (`:35`). Arming it makes ImageMagick a
  hard requirement of the lane — it is already installed (`ci.yml:405`), but
  the rig's tool preflight (`:66`) checks only `sway swaymsg grim jq` and must
  gain `compare`.

## Evidence

- `.github/workflows/ci.yml:322-352` — the lane's purpose, the exit-code
  classification, the nightly cadence, the 20/20 promotion record by run id,
  and the outstanding branch-protection registration.
- `:353-464` — the job: env pins at `:359-385`, the rig tool install at
  `:404-405`, the self-test at `:412-413`, the download and its integrity
  logic at `:415-445`, the rig invocation at `:451-453`, artifact upload at
  `:455-462`.
- `:380` — `JBR_SHA256` placeholder; `:381-385` — `PIXEL_DIFF_MIN: "0"` with
  `TODO(maintainer)` and the "calibrate from measurement, not the blind 1%
  guess" decision.
- `:401-403` — the published first-light finding (missing
  `gsettings-desktop-schemas` prevented the control frame mapping).
- `:466-500` — the X11 twin and its identical open registration.
- `scripts/wayland-rig.sh:14-20` (artifact list including `pixel-diff.txt`),
  `:33-37` (requirements and the `compare` conditionality), `:46` and `:60`
  (`PIXEL_DIFF_MIN` default), `:53` (`die` → exit 1), `:191-236` (the
  readiness gate and its rig-versus-JLS classification).
- `scripts/wayland-rig-selftest.sh` — the stub-toolchain control-flow guard.
- `docs/wayland-desktop-checklist.md:1-14` — the published per-release
  spot-check that the CI lane complements.
