# Issue #411: TASK-0018 (RESIDUAL): the GUI lanes stop proving less than they appear to — the runtime is pinned by digest, a blank window turns the lane red, and both rigs are registered as required checks
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

One sentence: *a green GUI lane should mean the editor drew itself on a runtime we
chose.* Everything else in the 14 sections is machinery. Judged against that end, the
digest half of the issue is exactly right and should land as written. The other two
halves — the pixel gate and the ratchet — are aimed at the right target with
instruments that do not reach it, and both have a simpler, more durable route that the
issue never considers because it scoped itself to "two scalars in one file".

## Where the issue is straightforwardly right

Pinning `JBR_SHA256` is a strict improvement with the verification already fail-closed
(`.github/workflows/ci.yml` L430-L441), the skip/mismatch split already correct
(L421-L427), and nothing to design. Keeping O5's ordering is the right instinct. Add
`compare` to the preflight, yes. Those are not in dispute below.

## Reframe 1 — the invariant belongs in the rig, not in a scanner over YAML

`everyPixelGateIsArmed()` over `ci.yml` is fail-open by construction, and the tree
already contains its counterexample. There are **four** GUI lanes, not two:
`GUI boot (Linux, Wayland/JBR)` (L354), `(Linux, X11/Xvfb)` (L502),
`(macOS, WindowServer)` (L595), `(Windows, WindowStation)` (L723). Only the first two
set `PIXEL_DIFF_MIN` at all. `scripts/macos-rig.sh` L117 defaults it to `0` and the
macOS lane's `env:` never sets it — so that lane's gate is disarmed *by omission*, and
a scanner that reads literal occurrences and requires each to be positive stays green
over it forever. Worse, the cleanest way to defeat the proposed ratchet is to delete
the key: the lane goes back to `PIXEL_DIFF_MIN=0` by default and the test finds two
armed occurrences instead of two, still passes. §7.11 names this exact hazard ("a
scanner that passes because it found nothing") and then ships an instrument subject to
it.

The reframing that makes the ratchet unnecessary: **invert the default in the rigs.**
`PIXEL_DIFF_MIN="${PIXEL_DIFF_MIN:-0}"` (`wayland-rig.sh` L60, `x11-rig.sh` L95,
`macos-rig.sh` L117) becomes "unset or non-positive → exit 2, the gate is not
configured", with an explicit `PIXEL_DIFF_MIN=off` opt-out for local first-light runs.
Delete the `UNVERIFIED-*` branch at L430 the same way: unpinned is a lane failure, not
a `::notice`. Then the placeholder cannot return without the lane going red on the next
push, which is a stronger guarantee than a shape assertion in `mvn verify`, and it
covers macOS and Windows too — lanes the JUnit test would never have seen. It also
avoids opening a new precedent: no test under `test/jls/` reads `.github/` today
(verified), and once one does, `mvn verify` starts asserting facts about CI-provider
configuration for contributors running offline on unrelated changes.

## Reframe 2 — the specified gate does not turn a blank window red

This is the load-bearing objection. The metric is `AE(desktop-before, desktop-after)`
— the empty sway desktop versus the desktop with JLS on it (`wayland-rig.sh` L344-L352)
— thresholded at 10% of the observed green value. A JLS window that maps at full size
and paints only its background differs from the empty desktop across essentially its
whole area, so its AE lands near the green value, far above 10% of it. The gate fires
only when the window covers under a tenth of its usual area, or when JLS paints the
desktop's own background colour. That is a window-geometry sanity check, and P1's
`swaymsg` tree assertion (L316-L327) already proves a window of that process exists.
The abstract's headline claim — "a blank window turns the lane red" — is not delivered
by the mechanism the issue specifies, and #208's modal-dialog-then-exit shape and #316's
editor-decomposition regressions are precisely the failures that would sail through.

The alternative, and it is both simpler and threshold-free: **make JLS its own oracle by
diffing two known states of the same window instead of the window against the desktop.**
Screenshot the editor empty, then open a fixture — `test/fixtures/headless-canary-gate.jls`
already exists and the editor opens a positional `.jls` argument — and require the
*window interior* to differ. Same runner, same fonts, same theme, same compositor on both
sides, so nothing needs calibrating, nothing drifts when the runner image updates, and
H2/§11's whole "measure the AE, take 10%, watch the headroom ratio" apparatus evaporates.
It proves the canvas rendered a circuit, which is the property #316 actually needs
observable and the property a reader assumes the lane already checks. The same two-state
recipe ports unchanged to X11, macOS and Windows, replacing four independently calibrated
magic numbers with one shared contract. If a single-shot gate is wanted as well, a
distinct-colour count or entropy floor over the window rect (the geometry is already in
`tree.json`) distinguishes "drew structure" from "painted one colour" far more robustly
than a pixel count does.

## Reframe 3 — one JBR manifest, not three placeholders across two issues

`UNVERIFIED-PLACEHOLDER-fill-in-real-sha256-see-issue-101` appears three times:
`ci.yml` L380 (artifact `jbr-25.0.3-linux-x64-b508.16.tar.gz`) and
`scripts/build-installer.sh` L298-L299 (artifact `jbrsdk-…` for linux-x64 and
linux-aarch64). `.devcontainer/Dockerfile` L67-L70 fetches a fourth copy with no digest
at all. The amendment comment correctly spots that #285 is the same work and then
concludes "neither closes the other" — that conclusion is an artifact of the split, not
of the problem. The aligned move for a project that pins every action by commit SHA and
publishes reproducible jars with a BOM is a single in-tree manifest (`config/jbr-runtime.lock`
or equivalent: version, build, platform, url, sha256, one row per artifact) consumed by
the workflow, the installer script and the devcontainer. Then there is one place to pin,
one place to bump, one loud failure on a version/digest mismatch, a ratchet target that
is a lockfile rather than YAML env scalars, and #411 and #285 stop racing each other for
the same `curl | sha256sum`.

## Reframe 4 — P7 is assertable, just not from the working tree

§7.2 and §11 accept that required-check registration can only be recorded in a commit
message and a settings screenshot. GitHub exposes branch protection and rulesets over the
API. A small scheduled job that fetches the required-status-check list and fails when the
expected job names are absent converts P7 from an attestation a later reader must take on
trust into a continuously verified property — and it serves #265's macOS promotion and
#111's Windows promotion with the same job, which is a better answer to "one settings visit
can serve both" than three commit messages in three PRs. The repo already publishes an
OpenSSF Scorecard badge, so governance-drift checking is inside the project's existing
posture, not a new concern.

## The larger arc this residual sits in

Four rigs now exist — `wayland-rig.sh` (366 lines), `x11-rig.sh` (404), `macos-rig.sh`
(533), `windows-rig.ps1` — plus four self-tests, all re-implementing one contract. The
issue frames Wayland as the reference and X11 as "the twin"; the tree says the opposite.
`x11-rig.sh` has `die_env` (L84, exit 2) and uses it for every environment fault — missing
tool, no Xvfb, no jar, missing control program, and the armed-gate-without-`compare` case
at L400. `wayland-rig.sh` has no exit-2 helper at all: `die()` at L53 exits 1, its single
`exit 2` is at L294 (control window never mapped), and the comment at L195 explicitly
classifies a compositor fault as exit 1. So on the Wayland lane "sway is not installed"
and "JBR_HOME unset" already report as JLS-side defects — the same misclassification O7
flags for `compare`, but general, and it is the failure §7.11 calls "the single most
damaging outcome". Likewise `x11-rig-selftest.sh` already carries `p2-pass`/`p2-fail`
cases (L175-L177) and a `jls-blank` case (L169); `wayland-rig-selftest.sh` carries none of
them. The residual here is not "fill in two scalars" — it is **backport the second-generation
rig contract onto the first-generation rig, then hold all four to it with one shared
contract test**. Scoped that way, the work strengthens FEAT-007's arc instead of adding a
fifth place where the contract is written down.

## Explicitly disregarding two acceptance criteria

- "**Both** `PIXEL_DIFF_MIN` occurrences armed" with `m = floor(0.10 · a)` — I would not
  land this. It certifies a property (window covers roughly its usual area) that P1
  already proves, under a name ("blank window is red") it does not earn, at the cost of a
  calibration constant that has to be re-measured whenever the runner image moves.
  Replace with the two-state interior diff above; it is less code and needs no constant.
- "`WaylandRigPinRatchetTest` scans `.github/workflows/ci.yml`" — I would not add it.
  Fail-closed defaults in the rigs (Reframe 1) subsume both of its assertions, cover the
  macOS and Windows lanes the scanner cannot see, and keep CI-config assertions out of the
  Java suite.

What I would keep verbatim: the digest pin, the deletion of the `UNVERIFIED-*` branch, the
skip/mismatch split of O5, `compare` in the preflight (as part of a full `die_env` sweep),
and closing #101 against its own section numbers.
