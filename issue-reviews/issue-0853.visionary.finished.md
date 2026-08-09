# Issue #853: TASK-C579-3: a tagged release updates the Flathub manifest by itself, with the approval click the only human step — proven once end to end
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

The Outcome sentence is the right one and I endorse it without reservation: *the channel
stops being a per-release chore*. JLS already carries seven-ish delivery surfaces (deb, rpm,
two AppImages, Nix flake, msi ×2, dmg, jar, GHCR image) and the README's honesty about each
one's caveats is the project's real distribution asset. Adding an eighth surface is only
defensible if its steady-state cost is approximately zero — which is exactly why CAP-34
hangs a 0.5 mw/cycle kill criterion on it. So #853 is not "wire up a bot"; it is *the task
that decides whether Flathub is allowed to exist at all*. That framing is correct and
under-served by the ACs as written.

## The load-bearing fact the issue never names

Flathub does not host manifests in the upstream repository. Once submitted, the canonical
manifest lives in `flathub/io.github.anadon.JLS`, a repo JLS does not own, on Flathub's
branch/build conventions. TASK-C579-1 (#849) commits a manifest *in-tree*. #853 then says a
release must update "the Flathub manifest" — without saying which of the two copies is
canonical, or what keeps them from diverging. That ambiguity is the whole design, and it is
invisible in the acceptance criteria. Pick the seam explicitly:

- **Flathub-repo-canonical** (my recommendation): the in-tree file is demoted to a
  *development* manifest used by `flatpak-builder` locally and by a CI validation lane —
  never the thing that ships. The published manifest is edited only in the Flathub repo.
- **In-tree-canonical**: release.yml renders and pushes to the Flathub repo, which requires
  a long-lived cross-repo credential (see below).

## Reframing 1 — the automation already exists, and only one of the two routes is aligned

The Outcome's "via the release workflow or the Flathub update bot, whichever the submission
uses" treats these as interchangeable. They are not, and the choice is architectural.

Flathub's `flatpak-external-data-checker` reads an `x-checker-data` block *inside the module
that already declares the url and sha256* — for a GitHub release source it polls the
releases API, downloads the new asset, computes the digest itself, and opens a PR against
the Flathub repo, which Flathub's own buildbot then builds and test-installs. That is
AC-3 and AC-4 satisfied by roughly a dozen lines of YAML, with **zero JLS-side moving parts**.

The release-workflow route requires minting a token with write access to a foreign
repository and holding it as a repository secret. Read release.yml's own comments: default
`permissions: contents: read`, every job elevating only what it uses, every action pinned to
a full commit SHA, "least privilege (#68)". The README leads with an OpenSSF Scorecard badge.
A standing cross-repo PAT is a direct regression against that posture, and it buys nothing
the checker does not already give. This is not a preference — it is the project's stated
trajectory choosing for us.

**Concrete rewrite:** AC-3/AC-4 become *"the manifest declares `x-checker-data` of type
`json`/`rotating-url` against this repository's releases; the digest is computed by the
checker from the downloaded bytes, never written by JLS-side code; no JLS-owned credential
grants write access to any repository outside `anadon/JLS`."* Note this makes AC-4
("never hand-copied") tautological rather than testable, which is the point: the correct
design makes the failure mode structurally impossible instead of asserting against it.

## Reframing 2 — AC-5 defends against the wrong failure

AC-5 wants a checksum mismatch to fail visibly. Under the checker, a mismatch cannot occur
(the digest is derived from the fetched bytes), and a *missing* asset produces no PR at all.
The real failure mode of every pull-based channel is **silence**: Flathub quietly sits three
versions behind and nobody notices until a user files a bug. A pushed pipeline fails red; a
pulled one just stops, and stopping looks exactly like "no release happened."

So the property worth building is a **freshness alarm owned by JLS**: a scheduled job that
asks each downstream channel what version it is currently serving and compares it to the
newest tag, failing after a grace window. For Flathub that is one HTTP GET against the
public AppStream/API surface; no credentials, no cross-repo anything.

And this generalizes — which is where the real leverage is. #580 (winget) and #581 (Homebrew
cask) are the *same shape*: a foreign repo holds a manifest naming our asset URL and
checksum, something must keep it fresh, a human approves. Three sibling features are each
about to invent a bespoke propagation-and-verification story. Cut the seam once:
`scripts/channel-freshness.sh` plus one workflow, with each channel contributing a few lines
of config (id, query URL, extraction rule). The project already has this instinct everywhere
else — `scripts/build-installer.sh` is explicitly "the single recipe used both locally and by
CI", and every rig script has a paired `*-selftest.sh`. Distribution propagation deserves the
same treatment, and #853 is the first place it can be established rather than the third place
it is duplicated.

This also hands TASK-C579-4's AC-4 and the CAP-34 kill criterion an actual data source. As
written, "record the per-release cost in maintainer-weeks" is a manual bookkeeping chore that
will plausibly consume more effort than the channel it is measuring. The freshness monitor's
log *is* the ledger: "N releases, N automatic bumps, 0 interventions" is the arithmetic,
produced for free.

## Reframing 3 — "no manual step beyond approval" is false as specified

The checker updates `url` and `sha256`. It does not touch AppStream `<release>` entries. A
Flathub app whose metainfo release list stops advancing shows a stale "What's New" in GNOME
Software and can trip AppStream validation. So under the issue's own criterion, every release
still needs a human to hand-edit release notes — the chore survives, relocated.

There is a strictly better object here that no issue in the cluster has named: **there is no
`*.metainfo.xml` anywhere in this tree.** The `.desktop` entry is heredoc'd inline in
`scripts/build-installer.sh` (and duplicated in `resources/packaging/resource-dir-linux/`),
the MIME association lives in three `jls-association-*.properties` files, and nothing ships
AppStream metadata at all — which means GNOME Software and KDE Discover render the *installed
deb/rpm* as a bare entry today, Flathub or no Flathub. One in-tree
`io.github.anadon.JLS.metainfo.xml`, with `<release>` entries generated from CHANGELOG.md at
tag time and consumed by the deb, the rpm, the AppImage *and* the Flatpak, is a smaller
artifact than what #853+#854 currently imply and improves four channels instead of one. Ship
it as a release asset, have the manifest fetch it by checksum alongside the payload, and a
single checker bump advances bytes and notes together — then "approval is the only human
step" is actually true.

## Reframing 4 — this task is two unlike things bolted together

AC-1 (submit to Flathub, pass review) is a one-time social process with unbounded external
latency. AC-3/AC-4 (propagation) is a manifest field. Bundling them means the automation is
gated behind a review queue JLS does not control, while AC-1's escape hatch ("or record why
it is blocked") lets the whole task close with no automation in existence. Meanwhile #854 is
ordered after #853, so a slow Flathub reviewer stalls the screenshots too.

Cleaner decomposition: `x-checker-data` is a property of the manifest and belongs in #849
alongside the url and sha256 it annotates — filing it separately is like filing the checksum
as its own task. #853 then reduces to *submission + the freshness monitor*, and #854's
metainfo work (per Reframing 3, needed for the manifest to be complete) can proceed in
parallel rather than waiting on a queue.

## Explicitly disregarded

I am setting aside AC-3's *"demonstrated end to end once"* on a real release as a closing
gate. It converts an engineering task into a calendar dependency on the next tag, and the
project already has the right idiom for this: the Wayland row is "verified two ways — CI on
every push, and a scripted once-per-release spot-check"
(`docs/wayland-desktop-checklist.md`, recorded as a comment on #100). Do the same here. The
CI-side proof is running the checker against the committed manifest in dry-run mode and
asserting it resolves the newest release's url and digest — provable today, on every push,
and it catches the interesting regression (a release-asset naming change silently orphaning
the channel). The real-release confirmation becomes a line on the release checklist, not a
gate on an open issue.

## Alignment

The arc this strengthens is real: JLS's distribution story is unusually principled —
attestation, reproducible jar, published `.buildinfo`, honest README caveats about what each
guarantee does and does not cover — and a Flatpak that consumes the attested asset by digest
extends that principle to a store. Nothing here pulls against the project. What pulls against
it is the *push* route's credential, and the third independent reinvention of "keep a foreign
manifest fresh." Take the pull route, build the freshness monitor once for all three
channels, factor the AppStream metadata out of the packagers, and this task becomes small,
permanent, and cheap enough to survive its own kill criterion.
