# Issue #580: FEAT-C34-2: `winget install jls` works, and every release bumps the manifest without anyone remembering to
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not winget. #518 says it outright — PF-2 is "the demo slice: the smallest channel
and the only fully scriptable one, so it proves the propagation pattern the other
channels reuse." The deliverable that matters is a **release-to-channel propagation
seam**; the winget manifest is the first thing that rides it. Judge the work by
whether channels 2 and 3 cost fifty lines each afterward, not by whether the
manifest lands.

The issue as written does not name that seam anywhere. Its five ACs describe a
winget-shaped artifact (manifest in-tree, PR to `microsoft/winget-pkgs`, sha from
`SHA256SUMS-installers-*`, blurb from CAP-27, cost recorded). Execute those
literally and you get one bespoke pipeline plus a precedent that the next channel
is another bespoke pipeline. That is the reframing this review is about.

## Where it sits in the project's arc

Strikingly well, and better than its siblings. JLS has a *recorded* deployment
model — single self-contained artifact, no sandbox, no invocation prefix, batch
mode usable from shells and autograders (ARCHITECTURE.md § "Help delivery",
`docs/batch-interface.md`). `docs/standards-adoption/10-desktop-and-housekeeping.md:81-99`
declines Flathub on exactly that basis: a second packaging pipeline with none of
the reproducibility plumbing carrying over, `--filesystem=home`, and a `flatpak run`
prefix that "breaks every command line in `README.md`."

winget breaks none of it. It ships *the same msi bytes* from the same Release, no
sandbox, no prefix, no second build. Among CAP-34's three channels it is the only
one that is purely a pointer. That is the real reason it is the demo slice, and it
should be stated as a **channel admission test** rather than left as an accident of
size — see the next section. (Corollary the capstone has not absorbed: PF-1 Flathub
re-proposes something this repository already adjudicated against, in writing, with
four reasons. #580 does not have to resolve that, but #518 does, and #580 is the
evidence that settles it.)

## Reframing 1 — build the descriptor, not the manifest

Concretely, cut the seam here: the `installers` job already computes
`SHA256SUMS-installers-<os>-<arch>` and attests `target/installer/dist/*`
(`release.yml:625-651`). Add one step in that same job that emits a
channel-neutral **release descriptor** — version, per-arch installer filename,
download URL, sha256, license, project description/blurb, screenshot URLs — and
publish it as a release asset alongside `bom.json`. Then:

- The winget manifest is a ~30-line template render over that descriptor.
- The Homebrew cask (#579's PF-3) is another render over the same descriptor.
- The AppStream metainfo that `10-desktop-and-housekeeping.md` recommends shipping
  is a third consumer of the same description/blurb fields, so CAP-27's blurb is
  written once and consumed structurally rather than pasted per channel.

This makes AC-3 ("`InstallerSha256` read from the published asset, never
hand-copied") a *structural property* instead of a per-channel discipline that each
future channel must re-earn. Better still: generate the descriptor **before** the
attestation step so the descriptor is itself an attested subject. Then the chain is
end-to-end verifiable — a third party can check that the sha256 winget shipped is
the sha256 this repository's workflow attested — instead of the manifest being an
unattested restatement of an attested fact. That is a genuine strengthening of
JLS's actual differentiator, which is not channel count (Logisim-Evolution wins
that four to zero) but *verifiable provenance*, and today no channel listing
carries any of it.

## Reframing 2 — make "preserves the KPI" the channel admission test

#518 pairs the channels (PF-1..3) with a download-count KPI (PF-4) and never
notices that the two interact. winget and the Homebrew cask both resolve to
`github.com/anadon/JLS/releases/download/...`, so every install through them
increments the very counter PF-4 proposes to adopt as the adoption metric. Flathub
builds on Flathub infrastructure and serves its own bytes: every Flathub install is
invisible to PF-4, and simultaneously violates AC-1's "no channel-specific rebuilds
without provenance."

So there is a single criterion that decides AC-1, PF-4's validity, and the Flathub
question at once: **a channel is admissible iff it redistributes the attested bytes
by reference.** #580 is the first instance and should say so in one line. That
sentence is worth more to CAP-34 than the manifest is.

## Reframing 3 — the identity ledger

`winget` needs a `PackageIdentifier` of the form `Publisher.Package`. JLS already
carries three identities and they have already drifted: Maven `io.github.anadon:jls`
(`pom.xml:7`), macOS bundle `io.github.anadon.jls` (`build-installer.sh:410`), and
the AppStream ID `io.github.anadon.JLS` that `10-desktop-and-housekeeping.md:44`
declares correct — note the case mismatch, live today. #580 adds a fourth namespace,
#579 a fifth (cask token), PF-1 a sixth. A public package identifier is
effectively permanent: winget-pkgs renames are a migration, not an edit.

Cheap, high-leverage, entirely in this project's idiom (`docs/component-naming.md`
exists for exactly this reason on a different axis): one identity table — Maven
coordinates, AppStream ID, macOS bundle ID, container ref, winget
`PackageIdentifier`, cask token — recorded once, with a drift test. Deciding
`Anadon.JLS` in passing inside a winget PR is how the sixth namespace ends up
inconsistent with the first.

## Reframing 4 — "without anyone remembering to" is a test, not a workflow step

The title's second clause is the interesting half, and CI automation only half
delivers it: a release step that bumps the manifest is exactly as reliable as
someone remembering to update that step when the matrix gains a leg or an asset is
renamed. This repository's answer to that class of problem is a drift test —
`CliFlagTableTest`, `ExtensionPointCatalogTest`, `HelpTopicsTest`, and #443's
proposed `InstallerMatrixPolicyTest` all fail the build when two in-tree sources of
truth disagree. Add `DistributionManifestTest`: the in-tree manifest's version
equals `pom.xml`'s, its installer set equals the `release.yml` matrix's Windows
legs, and every `InstallerSha256` is a placeholder or descriptor-derived, never a
literal committed by hand. Offline, no network, same family as the rest. Then the
remembering is enforced rather than automated.

## Two things the ACs promise that the design cannot

- **AC-1's literal command.** `winget install jls` resolves through name/moniker
  search across a namespace JLS does not control; only `winget install Anadon.JLS`
  is a promise this project can keep. Promise the identifier, offer the moniker,
  and write the README line to match — otherwise the README acquires a second
  present-tense claim that external state can falsify, which is precisely the defect
  #134 Observation 3 is open about.
- **AC-5's measurement.** Recording winget's cost "against KC-34-1's 0.5 mw
  threshold" measures the wrong quantity: after Reframing 1 the maintainer cost per
  release rounds to zero for *every* channel, so the threshold passes trivially and
  decides nothing. The number that actually governs channel N+1 is **elapsed time
  from tag to availability, and PR round-trips with the channel's reviewers**.
  Record those. I am explicitly setting AC-5 aside as written.

## One ordering point the round-2 comment missed

The comment handles the signing half of AC-1 well (ship unsigned, never *claim*
signed, treat the SmartScreen warning as recorded risk rather than duplicating
#134's custody stall) — that boundary is drawn correctly. But it discusses only
signing, not the **gate**: both Windows legs are `continue-on-error` (#443 O3,
`release.yml:293-312`), so a broken msi silently omits itself from a release today.
Publishing to winget makes that msi the *default* Windows acquisition path, at which
point a silently-missing asset becomes a broken `winget install` for everyone rather
than a missing file on a page. Unlike #134, #443's promotion needs no custody
action and is already scoped. AC-2's "zero manual steps" should ride behind it.

## Verdict

**endorse-with-reframing.** The channel choice is right, it is the one CAP-34
channel that conforms to this project's recorded deployment model, and the boundary
work in the round-2 comment is sound. What is missing is the thing #518 said this
slice was for: cut the release-descriptor seam and attest it, state the
preserves-the-attested-bytes admission test, record the identity ledger, and enforce
the freshness with a drift test rather than a workflow step. Do that and channels
two and three are cheap and the Flathub question answers itself; skip it and #580
ships a manifest and proves nothing.
