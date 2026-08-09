# Issue #285: Linux installer runtime: arm the pinned JBR sha256s and Wayland-verify a JBR-bundled deb
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the two string constants, #285 is the last load-bearing step of a long
arc: #100 → #105 (`ToolkitPolicy` auto-selects `WLToolkit`) → #101 (a CI rig that
proves it boots) → #82 (installers that remove the bring-your-own-JDK barrier).
Every link of that chain is built except the one that reaches an actual student:
the deb/rpm/AppImage a student installs still carries a runtime that cannot do
Wayland. README.md:176 lists the Wayland-native row as "supported" on the strength
of "JBR / Wakefield builds" — a runtime the project ships nowhere. So the goal is
right and the arc genuinely needs this. I endorse the destination.

What I do not endorse is the seam the issue cuts along: a human on an unproxied
laptop pastes two digests into a shell script, and the project's Wayland story stays
spread across three independently hand-pinned copies of the same fact.

## Reframing 1 — the "environmental blocker" is already solved, in this repo

§6 names "an unproxied machine with curl + sha256sum" as *the only* blocker, and the
DoD's first box says "from an unproxied machine." But `.github/workflows/ci.yml:416-444`
already downloads a JBR from `cache-redirector.jetbrains.com` on every push and
nightly cron, and line 429 literally prints `observed JBR sha256: $observed`. The
release installer legs (`release.yml`, `ubuntu-latest` / `ubuntu-24.04-arm`) run on
the same unproxied GitHub-hosted runners. The authoring environment is proxied; the
project's own machines are not.

Concretely, before anything else: open one recent green `gui-wayland` run and read
the `observed JBR sha256:` line. That is the x64 *runtime* flavor (`jbr-…tar.gz`),
not the `jbrsdk-…tar.gz` the installer pins, so it is not the digest to paste — but
it settles whether the CDN is reachable from CI, and if it is, the whole §6 apparatus
collapses into a five-line `workflow_dispatch` job that curls both `jbrsdk` URLs and
prints their digests. Strike the "unproxied machine" checkbox; it encodes a
constraint of the author's terminal as a constraint of the project.

Note also that no leg anywhere has ever fetched `jbrsdk-25.0.3-linux-aarch64-b508.16`.
The aarch64 pin is not merely unarmed, it is unvalidated — the build may not exist
under that name. A CI fetch job discovers that in one run; a laptop session
discovers it after someone has already booked the work.

## Reframing 2 — mirror the tarball; the pin then defends something

This is the change I would actually make, and it is already on the table: #101's
**Open Question 1** offers exactly two options — "keep fail-open with `gui-x11` as
the hard gate (current)" or "mirror the JBR tarball to make the download reliable
and then fail-close." #285 quietly takes neither. It arms a pin against a
third-party CDN and leaves the fetch in the release critical path.

Mirror both `jbrsdk` tarballs once as assets on a dedicated tag in this repository
(GPLv2+CPE redistribution was adjudicated permissible on #82 — that adjudication
licenses mirroring, not just bundling), pin *those* digests, and point
`build-installer.sh` and `ci.yml` at them. What that buys, beyond #285's stated
outcome:

- §11's "JetBrains may re-publish a tarball under the same name (digest drift)"
  stops being a threat to mitigate and becomes structurally impossible. #285's own
  §10 falsification path ("if the digests drift on re-fetch, re-pin via a fresh
  adjudication") is a recurring tax the mirror simply removes.
- #101's §5 "honest limits" — the Wayland lane fail-opens on CDN failure and
  therefore gates only on lucky runs — is repaired at the same time. One action
  closes an open decision on the parent's sibling.
- The release build stops depending on a vendor CDN's uptime at tag-push time.
- Both digests are computed once, in CI, by machine, and never by a human reading
  a terminal.

If mirroring is rejected on hosting or license-hygiene grounds, say so on #101 and
close its Open Question 1 — but do not arm a pin *without* resolving it, because
arming makes the CDN a hard dependency of every Linux release where today it is
merely an aspiration.

## Reframing 3 — one JBR pin, not three (the real architectural seam)

The same runtime identity is currently written down in three places, in three
shapes, with no mechanism keeping them in agreement:

- `scripts/build-installer.sh:289-301` — `jbrsdk` flavor, per-arch URL, two placeholders
- `.github/workflows/ci.yml:363,380` — `jbr` flavor, x64 only, one placeholder
- `.devcontainer/Dockerfile:62-71` — `JBR_URL` build arg, the URL only in a comment

`config/` exists and holds exactly one file. A `config/jbr-pin.env` (version, build,
per-flavor/per-arch URL and sha256) sourced by the script, read by `ci.yml`, and
defaulted into the Dockerfile turns "which runtime carries `WLToolkit`" into a single
project-level fact with one place to bump it. #101 recorded "pin URL+checksum in one
place" as *retired* — but what PR #196 actually inverted was the *fail-closed* half,
not the *one place* half. The single-source-of-truth idea was thrown out with the
enforcement policy it happened to be bundled with. It is worth recovering, and #285
is the moment: this issue is the second consumer of the pin, which is exactly when a
constant becomes a config.

Without it, the JBR bump that #101's §7 anticipates ("`WLToolkit` regression on a JBR
bump → REPLAN the pin") is a three-file, two-flavor, three-digest edit that a
single maintainer will do partially.

## Reframing 4 — the flake ships the same gap, for free

`flake.nix:28` sets `jdk = pkgs.jdk25` and line 68 wraps `${jdk}/bin/java`. NixOS
users — disproportionately Wayland-native — get a `jls` that cannot select
`WLToolkit`, and README.md:25-28 sends them there precisely *because* the deb/rpm
do not fit their system. nixpkgs ships `jetbrains.jdk`, which is JBR. If the answer
is "Linux desktop JLS runs on a WLToolkit-bearing runtime," the flake is a one-line,
zero-bytes-bundled instance of that answer and it belongs in the same change as the
installer. If the answer is instead "only the installers do," README's supported
matrix should say so per-channel. Either way the inconsistency should be decided,
not inherited.

## Reframing 5 — P3 measures the wrong thing

P3 accepts "boots on `WLToolkit` rather than XWayland" as success, evidenced by an
`awt.toolkit` dump. That is a proxy for a proxy. Students on Wayland are not
currently *broken* — they get XWayland, and `ToolkitPolicy` prints an actionable
error when they do not. The payoff this issue is spending a substantially larger
bundled runtime on is the thing XWayland does badly: crisp rendering under fractional
and HiDPI scaling, correct cursor and popup placement, no blur. `WLToolkit` being
selected and the app looking *worse* than XWayland is a real, unmeasured outcome —
`WLToolkit` is experimental, and `docs/wayland-desktop-checklist.md` already exists
as the instrument for judging a real GPU-backed desktop rather than headless pixman.

So I am disregarding one stated acceptance criterion: replace "record toolkit
evidence (e.g. `awt.toolkit` in a diagnostic dump)" with "run
`docs/wayland-desktop-checklist.md` against the *installed deb* at 100% and at 150%
fractional scaling, and compare against the same checklist under XWayland." If
WLToolkit does not win that comparison, §10's own instruction applies — "do not ship
a larger runtime for no Wayland gain" — and the right outcome is to keep the
fallback and document JBR as the opt-in Wayland runtime instead. That is a possible
and honorable result, and the issue as written cannot reach it, because its evidence
plan cannot tell "Wayland-native" from "Wayland-native and better."

## Alignment

Nothing here pulls against the project's arc; it is the arc's last mile. The
duplication risk is narrow and named above (three pins, plus the flake going its own
way). The one thing that does pull against the project's character is the shape of
the work: a distribution stack this deliberate — attested installers, reproducible
jar, a rig with an exit-code failure taxonomy, a per-release physical-desktop
checklist — should not have its Linux runtime identity depend on a human
transcribing hex from a terminal it cannot reach.

## Recommendation

Endorse the goal; re-cut the task as: (1) a CI job that computes both `jbrsdk`
digests, (2) a mirrored tarball with the digest pinned against the mirror, closing
#101's Open Question 1, (3) `config/jbr-pin.env` as the single pin consumed by the
script, `ci.yml`, and the devcontainer, (4) a decision recorded on the flake's
runtime, and (5) a real-desktop scaling comparison as P3 in place of the
`awt.toolkit` string. Keep the loud fail-closed fallback exactly as it is — it is
the best-designed part of the existing code and the reason this can be attempted at
all without risking the green release lane.
