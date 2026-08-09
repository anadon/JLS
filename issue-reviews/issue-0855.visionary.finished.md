# Issue #855: TASK-C580-1: an in-tree winget manifest validates, and its InstallerSha256 is read from the published checksums asset rather than typed by a human
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Strip the winget-specific wording and #855 is making two claims at once:

1. JLS should be reachable through the channel people already install software
   with (the #580/#518 goal), and
2. a package-channel listing must be a *mechanical projection* of a release, not
   a hand-typed copy of one — the checksum especially.

Claim 2 is the one that matters to the project's arc, and it is much larger than
winget. The README is, more than anything else, a document about verifiable
distribution: per-asset `SHA256SUMS`, build-provenance attestations, a
reproducible jar and `bom.json`, cosign-signed images, an honest paragraph
explaining exactly why the installers are *not* byte-reproducible. A package
channel is the first place that discipline meets a surface JLS does not control.
So the interesting question is not "can we emit winget YAML" — it is "what does
it mean for a third-party channel to carry JLS's provenance rather than launder
it away". #855 gets that question right in miniature (AC-2) and then scopes it
down to one channel's file format. I would keep the task and enlarge the seam.

## Reframing 1 — the reusable artifact is a product-metadata source, not a winget emitter

#855 says it is "deliberately built first so the propagation pattern the other
channels reuse is proven on the cheapest one". A YAML emitter for
`microsoft/winget-pkgs`'s schema proves nothing reusable: Homebrew casks are
Ruby, Flathub is an AppStream + Flatpak manifest pair, and none of them share a
line with winget. What *is* reusable across all three is the metadata they all
want — id, name, summary, description, licence, homepage, tags, screenshots —
and that content is already duplicated across the tree today:

- `pom.xml:14-17` (`<name>`, `<description>`, `<url>`) — the source
  `scripts/build-installer.sh:62-66` already reads for `--description`,
  `--vendor`, `--about-url`;
- `flake.nix:12` and again `flake.nix:76-78` (a second, differently-worded
  description plus homepage and licence);
- `resources/packaging/Dockerfile:66-67` (OCI labels, a third wording);
- `README.md`'s opening paragraph (a fourth);
- `resources/packaging/resource-dir-linux/JLS.desktop` (`Comment=`, templated).

AC-4 tries to solve this by sourcing the listing text from "the shared CAP-27
(#511) text" — but #511 has no such artifact, and the ordering comment on #580
says so explicitly: CAP-27 "has two declared consumers of a screenshot/
description set it has not been told it owns for anyone but itself, and a third
potential producer in #586". AC-4 as written points at a hole.

The elegant fix is already written down elsewhere in this repo.
`docs/standards-adoption/10-desktop-and-housekeeping.md` recommends shipping one
**AppStream metainfo file** — component id `io.github.anadon.JLS`, matching the
`--mac-package-identifier` at `scripts/build-installer.sh:410` and the
`io.github.anadon:jls` Maven coordinates — validated by `appstreamcli validate
--pedantic` in CI. That file is exactly the canonical listing record every
channel wants, it is a real freedesktop standard rather than a bespoke YAML blob,
and the doc already recommends doing it *for its own sake* (the AppImage path
consumes it, and third-party packagers would).

So: make the first distribution task "one metainfo file, validated in CI, and
`pom.xml`/`flake.nix`/`Dockerfile`/`.desktop` descriptions derived from or
drift-checked against it", and make the winget locale manifest a ~40-line
projection of that file. Then PF-1 and PF-3 genuinely reuse something, AC-4's
dependency on an unfiled CAP-27 artifact evaporates, and the project stops
describing itself four different ways.

## Reframing 2 — build a checker, not a generator

AC-1 asks for a committed generator that emits the version/installer/locale
triple. Microsoft ships `wingetcreate`, whose `update --urls ... --version ...
--submit` already does the emit, tracks the manifest schema as it churns (it does
churn), and produces the PR against `microsoft/winget-pkgs`. A hand-rolled
emitter in this tree buys nothing and acquires a schema-drift maintenance tax —
precisely the per-release cost KC-34-1 says kills a channel.

More importantly, `wingetcreate` computes `InstallerSha256` by **downloading the
installer URL the manifest names and hashing those bytes**. That is a stronger
invariant than AC-2's. AC-2 reads the hash out of a sidecar ledger
(`SHA256SUMS-installers-*`, generated at
`.github/workflows/release.yml:637-649`) and therefore cannot detect the failure
mode where the ledger and the uploaded asset disagree — a real possibility given
that both Windows legs are `continue-on-error` (`release.yml:301-312`), so a leg
can half-fail and leave assets missing or stale.

Invert it: let the tool hash the URL, then have the committed script assert
`hash(bytes at the URL the manifest points to) == the entry in the published
SHA256SUMS-installers-windows-<arch> asset`, failing loudly on mismatch or
missing asset. That satisfies AC-2's intent, satisfies it *better*, and shrinks
the in-tree code to a verifier — which is the kind of thing this project is good
at maintaining.

## Reframing 3 — carry the attestation into the channel, not just the checksum

CAP-34 AC-1 says "no channel-specific rebuilds without provenance". Today that is
an aspiration; the winget manifest as specified only ever asserts a hash. One
extra line in the same verifier makes it mechanically true:

```
gh attestation verify <downloaded msi> --repo anadon/JLS
```

on the bytes fetched from the URL the manifest names. Now the claim the channel
carries is not "these bytes match a file we published" but "these bytes were
built by anadon/JLS's release workflow at a known commit" — the same guarantee
the README already gives direct downloaders, preserved across a surface JLS does
not own. This is the thing that would make JLS's distribution story *exemplary*
rather than merely correct, and it costs one command. If any single change is
made to this task, make it this one.

## Reframing 4 — do not commit version-stamped manifests

"The winget manifest lives in this repository as generated output" is a
half-position: a committed, version-stamped manifest is stale the moment the next
tag lands, and a stale committed manifest is exactly the hand-maintained file the
Outcome sets out to abolish, just relocated. Commit the template, the projection
script and the verifier; produce the manifest triple as a release-workflow
artifact alongside `SHA256SUMS-installers-*`; and let CI's dry-run path
(`release.yml` already has one, with `force-sign`) regenerate and validate
against the newest release on every push so drift is caught without a tag.

## Trajectory notes

- **The metainfo file is worth doing even if Flathub never is.**
  `docs/standards-adoption/10-desktop-and-housekeeping.md` recommends *declining*
  Flathub with specific, good reasons — the sandbox is hostile to the `.jls~`
  sibling-file recovery model (`src/jls/edit/Editor.java:103`), and a `flatpak
  run` prefix breaks every command line in `README.md` and
  `docs/batch-interface.md`. CAP-34's PF-1 asserts Flathub anyway. That conflict
  is #518's to resolve, not #855's, but it is another argument for putting the
  shared work in the metadata file rather than in any one channel: the metainfo
  survives whichever way PF-1 goes.
- **The unsigned-MSI risk is bigger than the recorded degraded mode.** #580's
  ordering comment frames it as a user-visible SmartScreen warning. In
  `microsoft/winget-pkgs` the review pipeline itself scans installers and routes
  unsigned/flagged ones to manual moderation; the realistic outcome is a PR that
  stalls, which lands squarely on KC-34-1's per-release cost threshold. Worth
  recording on #580 as "may block the submission, not just warn the user".
- **Two small facts the plan should absorb early:** the MSI is a per-user install
  (README: "per-user install, no admin rights needed"), so the manifest needs
  `Scope: user` and the right silent-install switches or `winget install` will
  behave differently from a double-click; and `winget` itself is not dependable
  on GitHub's `windows-latest` images in non-interactive runs, so AC-3 will most
  likely be met by JSON-schema validation on the existing `windows` lane
  (`ci.yml:143-146`) with `winget validate` as a manual once-per-release check.
  AC-3's "or the equivalent" already permits this; say which one is meant.

## What I would disregard, and why

AC-1 (a bespoke committed generator) and AC-4 (listing text sourced from CAP-27)
as literally written. AC-1 because the maintainable version of it is
`wingetcreate` plus a verifier, and AC-4 because it points at an artifact nobody
owns. Replace both with: *the canonical listing metadata is an in-tree AppStream
metainfo file validated in CI, and the winget submission is a projection of it
whose installer hash is verified against both the published checksums asset and
the release attestation.* AC-2, AC-3 and AC-5 survive unchanged; AC-5's cost
arithmetic gets easier, because most of the recurring cost was the generator this
reframing deletes.

## Verdict

**endorse-with-reframing.** The goal is right, the ordering (cheapest channel
first) is right, and AC-2's "never hand-typed" instinct is the correct seed of a
project-wide rule. But the task cuts along the winget file format, which is the
one seam nothing else reuses. Cut along product metadata and provenance
verification instead, and the same week of work leaves behind an artifact that
Homebrew, Flathub, the AppImage, the deb/rpm and any future third-party packager
all consume — while making JLS's provenance chain, for the first time, survive
the trip through someone else's store.
