# Issue #856: TASK-C580-2: a tagged release opens the winget-pkgs PR on its own, and `winget install jls` on a clean Windows box installs the attested MSI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "a PR against `microsoft/winget-pkgs`". Two things: (1) a Windows user installs JLS
the way they install everything else, and (2) the maintainer never carries a propagation
chore. CAP-34 (#518) adds a third, larger purpose that #856 barely acknowledges: winget is
the **demo slice** — "the propagation pattern the other channels reuse". So the real
deliverable of this task is a *pattern*, and winget is only its first instance.

Judged against the repository's trajectory, the goal is well chosen. JLS's actual
differentiator is not installer count — it is the integrity chain: reproducible jar and
BOM, per-installer provenance attestations, cosign-signed images, per-OS-and-arch
`SHA256SUMS-installers-*`, SignPath Authenticode. README spends ~50 lines teaching users
the *scope* of each guarantee. A channel is aligned with that arc only if the chain
survives the channel; it pulls against the arc if the listing becomes a fourth place where
claims about JLS can quietly go stale. Three of #856's five ACs are aimed at exactly that
risk, which is why the goal earns endorsement. The **shape** of the task is what I would
change, in four places.

## Reframe 1 — the seam is "release propagation", not "winget submission"

#855 (generator), #856 (submission), #579 (Flathub), #581 (Homebrew) will each grow their
own generator, own workflow, own secret, own verification. That is four bespoke pipelines
where the capstone asked for one pattern proven on the cheapest channel.

Cut the seam differently, and the winget work becomes the first adapter:

- **One release-facts document**, emitted by the release workflow next to the checksums:
  version, per-asset URL, per-asset sha256 (parsed from `SHA256SUMS-installers-*`, never
  recomputed), attestation subject, plus the CAP-27 (#511) description/blurb/tags. Every
  channel manifest, and eventually the README install table, is a *projection* of that one
  file.
- **One `scripts/publish-channel.sh <channel> <tag>`**, mirroring `build-installer.sh`
  ("the single recipe used both locally and by CI"), with `packaging/winget/`,
  `packaging/flathub/`, `packaging/homebrew/` supplying generate / validate / submit hooks.

The drift class this closes is already live in-tree: README asserts in the present tense
that the Windows installers *are* Authenticode-signed while no signed MSI exists (#134,
re-confirmed on #580). Hand-written prose about artifacts rots. A listing is prose about
artifacts. Derive it, do not write it.

## Reframe 2 — AC-4 cannot be met by workflow failure; it needs reconciliation

AC-4 wants "a failed or rejected submission surfaces as a failed workflow ... never a
silent no-op that leaves the published listing stale." A winget-pkgs PR is *reviewed days
later, in someone else's repository, by a bot and humans*. By then this workflow has
succeeded and exited. There is no workflow left to fail. As written, AC-4 is unsatisfiable
in the failure mode it names — the stale-listing mode.

The thing that actually detects staleness is steady state, not event time: a small
scheduled **channel-drift monitor** that asks, per channel, "is the version published
there equal to our newest release tag?" and goes red (or opens an issue) past a threshold.
One cron workflow subsumes AC-4 for winget, and — free — for Flathub and Homebrew when
they land; it supplies #857's ledger with turnaround data instead of the maintainer
timing themselves; and it converts CAP-34 AC-2 from a one-time claim into a standing one.
It is the same instinct as the nightly `gui-wayland` cron lane already in CI.

## Reframe 3 — stop hand-rolling submission; hand-roll the integrity check instead

Microsoft ships `wingetcreate` (`microsoft/winget-create`) precisely for this: fork,
manifest update, PR, token-scoped. Confirm its current contract before relying on it, but
the strategic point holds regardless of tool details: submission mechanics are commodity
and someone else maintains them; the part nobody else will do is **cross-checking the hash
that tool computed against this project's own published checksum asset and attestation**.
Building a bespoke submitter spends the task's 0.25–0.5 mw on the commodity half and
starves the half that is JLS's actual differentiator — and it directly loads KC-34-1's
per-cycle cost, the criterion that decides whether this channel lives.

Corollary for #855: if submission is commodity, the generator shrinks to a locale/metadata
template plus the assertion "the hash we are about to publish equals the one we attested".

## Reframe 4 — "verified once on a real release and linked" is the wrong completion test

I am explicitly disregarding **AC-3** as a completion condition, and rewriting **AC-2**.

This repository has already decided, repeatedly, that a human doing a thing once is not
evidence: `scripts/wayland-rig.sh` + `wayland-rig-selftest.sh`, `macos-rig.sh` +
selftest, `windows-rig.ps1` + `windows-rig-selftest.ps1` — each with a documented
exit 0/1/2 classification contract, each run by CI on every event, plus
`docs/wayland-desktop-checklist.md` for what a rig genuinely cannot cover. #856 proposes a
one-shot manual clean-box run and a link in a comment. That is the idiom the project
outgrew.

The project-native form is `scripts/winget-rig.ps1` (+ stub selftest, per the
`windows-rig-selftest.ps1` pattern), run post-release on `windows-latest` — which *is* a
clean box — asserting, with the rigs' exit-code contract:

1. `winget install jls` succeeds;
2. the installed launcher runs (`JLS.exe -h`, exit 0);
3. sha256 of the installer winget actually fetched == the entry in
   `SHA256SUMS-installers-windows-<arch>`;
4. `gh attestation verify <that file> --repo anadon/JLS` passes.

Step 4 is the one no other channel implementation in the ecosystem does, and it is the
whole reason JLS's presence on winget means something different from Logisim-Evolution's
(#510). Note also that steps 1–2 largely exist already: release.yml's installers job does
`msiexec /i` → locate `JLS.exe` → `-h` → `msiexec /x`. Reuse that logic; do not write a
second copy. (Verify `winget` availability on the hosted image rather than assuming it;
if absent, that is a rig exit-2 environment verdict, not a JLS failure — the contract
already covers this.)

## One ordering judgement the task should carry

#580's ordering comment permits shipping while the MSI is unsigned, and that is right for
the *generator*, the *rig*, and the *monitor* — all verifiable today. It is wrong for the
**submission**. A public listing is a front door; a first-run SmartScreen
unknown-publisher prompt at the moment of `winget install` inverts the channel's entire
promise ("it works like everything else"), and it is a worse first impression than
absence. Land everything here now; gate only the `--submit` step on #134, using the exact
`if: secrets… != ''` degradation already in release.yml for SignPath — manifests published
as release assets unconditionally (so a second maintainer can submit by hand), submission
conditional with a `::notice::`. That also disarms AC-5's custody risk, which is the same
bus-factor-1 shape that has held #134 open since 2026-07-18.

## Verdict

**endorse-with-reframing.** Right goal, right channel, right place in CAP-34's arc. Keep
AC-1 and AC-5. Rewrite AC-2 as a committed self-testing rig in the project's rig idiom,
drop AC-3's once-and-link as the completion test (keep the first real cycle as *evidence*,
not as the criterion), and replace AC-4's event-time failure with a scheduled channel-drift
monitor shared by all three channels. Build the release-facts + channel-adapter seam here,
because that is the "pattern the other channels reuse" the capstone actually asked for —
and spend the saved effort on the attestation cross-check, which is the only part of this
channel that is uniquely JLS's to do.
