# Issue #849: TASK-C579-1: a Flatpak manifest wraps the published, attested Linux artifact by checksum, and the built app launches and opens a .jls file
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is actually for

Strip the task framing away and #849 is the first physical step of CAP-34's
thesis (#518): JLS has technically excellent installers that nobody can find,
because discovery on Linux happens in GNOME Software and KDE Discover, and those
render catalogs the project is absent from. #579 wants the Flathub listing;
#849 wants the *trust* property to survive the trip — the bytes a student
installs from a store must be the bytes this repository attested.

That goal is right and it is squarely in the project's arc. The README's whole
installation section is an argument that provenance is a feature, not paperwork.
A channel that quietly forked the trust chain would be worse than no channel.

Where #849 goes wrong is not the goal but the **anchor it picks for provenance
and the seam it cuts to get there**. Both are recoverable, and the better
version is smaller than the one written down.

## 1. AC-1 anchors provenance to the project's *least* verifiable artifact

README:59 is explicit — "the installers are *not* byte-reproducible (the native
packaging tools embed wall-clock state) … the jar and `bom.json` are the
byte-reproducible artifacts." AC-1 tells the manifest to consume a deb/rpm/
AppImage: the one class of release output whose only guarantee is "GitHub says
this workflow emitted these opaque bytes." A third party cannot check that claim
against source; they can only check that the bytes did not change in transit.

Meanwhile `docs/reproducibility.md` plus the published `.buildinfo` mean anyone
— including Flathub's builder — can rebuild `jls-<version>.jar` from the tag and
get *identical bytes*. That is a strictly stronger property than attestation,
and it is the one the project already invested in.

So the inversion: **do not wrap the non-reproducible installer. Build the jar
inside the manifest and assert its sha256 equals the release's published
`SHA256SUMS` entry.** The provenance check then runs on every Flathub build,
performed by the channel's own infrastructure, and any divergence between the
store's bytes and the Releases page's bytes fails the build rather than being
noticed in a quarterly audit. AC-3's "scripted rather than eyeballed" becomes
structural instead of a helper script.

## 2. AC-1 is likely to be rejected by the destination it is built for

Flathub's submission rules require open-source applications to be **built from
source on Flathub infrastructure**; downloading a prebuilt binary (`extra-data`
or a pinned archive of a vendor build) is the accommodation for software whose
source is not available. JLS is GPL-3.0-or-later with the source in this repo.
A manifest whose defining property is "no rebuild from source inside the
manifest" is therefore aimed at the one thing the reviewer will ask to be
changed — and #849 ends at "builds locally," so that discovery lands in #853,
after this task is closed green.

Two supporting facts make the source route more attractive than the issue
assumes:

- **The project already ships a source-built channel and documents it as
  normal.** `flake.nix` uses `buildMavenPackage` with `mvnHash =
  "sha256-38VZ…"` — a single pinned hash over the entire Maven dependency
  closure, built in a network-isolated sandbox — and README:25-28 says plainly
  that the deb/rpm/AppImage "do not fit NixOS; the flake builds from source
  instead." AC-1's absolutism ("no rebuild from source") is not a project
  invariant; it is new with this issue.
- **The offline-Maven problem is already solved once.** The four runtime deps
  are small (`xz`, `org.jfree.svg`, `flatlaf`, `jspecify`); the plugin closure
  is the hard part, and nix pins it with one hash. The Flatpak equivalent is a
  `jls-<version>-maven-deps.tar` published as a release asset — attested and
  checksummed like everything else — consumed by the manifest as a pinned
  source. The dependency bundle then *is* the provenance artifact, and it is
  reusable by any future offline/air-gapped build story.

Two risks the reframed route must retire early, and neither is retired by
building the wrapper version first: (a) which JDK the current
`org.freedesktop.Sdk.Extension.openjdk` carries versus JLS's floor of 25 —
verify before writing a line of manifest; if it is short, the fallback is
pinning a JDK tarball as a source, which is ordinary and still avoids shipping
an unverifiable app payload; (b) a payload the size of a bundled jpackage
runtime duplicated on top of a Flatpak runtime is exactly what reviewers push
back on, which the jar-on-extension route removes entirely.

## 3. The seam is one channel-metadata source, not four hand-written manifests

Desktop identity is already copy-pasted three times in-tree: the jpackage
template (`resources/packaging/resource-dir-linux/JLS.desktop`), the inline
heredoc in `scripts/build-installer.sh:227` (AppImage AppDir), and
`desktopItems` in `flake.nix:53`. Name, comment, categories, icon and
`application/x-jls-circuit` are duplicated across all three. #849 adds a fourth
copy plus a first AppStream metainfo file; #580 (winget), #581 (Homebrew) and
#854 (listing copy) each add another, and each is scheduled to grow its own
bespoke "check the sha256 against SHA256SUMS" script.

The elegant route is one small release-time artifact — call it
`packaging/release-index.json`: version, per-arch asset URL and sha256 (read out
of the published `SHA256SUMS*`), app ID `io.github.anadon.JLS`, MIME type,
description, screenshot paths — and one renderer under `packaging/<channel>/`
producing the Flatpak manifest, the winget manifest, the Homebrew cask, the
metainfo XML and the `.desktop` files. Build that once here and #580/#581/#854
become template files; skip it and CAP-34 ships four drift surfaces plus four
checksum scripts, which is precisely the "four-vendor drift lesson" its own
KC-34-1 cites. #849's AC-3 is where that generic binder wants to be born.

Note also that the AppStream metainfo file is a shared prerequisite no issue in
the chain owns: the standards survey scoped it to deb/rpm/AppImage, Flathub
needs the same file, and if #849 writes a private copy the two diverge on the
first release. One file, one owner, both consumers.

## 4. Disregarding AC-5, deliberately

AC-5 forbids re-litigating #338/#443. Fine — those are installer-matrix
concerns. But AC-5 does not cover the thing that actually needs settling first,
and I am setting the boundary aside to say it:
`docs/standards-adoption/10-desktop-and-housekeeping.md:17,81-99` records a
considered **"Do not pursue Flathub"** with four named reasons — second
pipeline, `--filesystem=home` for a file editor whose checkpoint story writes
`<circuit>.jls~` beside the user's file, `flatpak run` breaking every command
line in README and `docs/batch-interface.md`, and screenshot hosting — and
instructs that the decline be recorded in ARCHITECTURE.md § Recorded decisions.
That entry does not exist, and neither #518, #579 nor #849 rebuts a single one
of the four reasons.

A capstone filed under maintainer directive may absolutely supersede a survey
recommendation. But the first artifact of this chain should be the **recorded
reversal** — three sentences in ARCHITECTURE.md § Recorded decisions saying what
changed (discovery was reweighted; the sandbox concerns are answered by X, Y, Z)
— not a manifest. Otherwise the repository simultaneously documents "we decided
not to" and "we are doing it," and the next maintainer cannot tell which is
current. That reversal is also where the honest answer to reason 3 belongs:
`flatpak run io.github.anadon.JLS -b -t tests circuit.jls` is the invocation for
every batch example in a shipped normative doc, and the fix is either a
documented alias line or an explicit "the Flatpak channel is the GUI channel;
autograders use the jar or the container image."

## 5. Slicing

#849 (manifest builds locally), #852 (portal MIME), #853 (submit) each deliver
zero user-visible value alone, and #849's central design decision is only
validated in #853 by a reviewer. For a 0.5–1 mw band this is over-cut: one
"the Flatpak channel lands or is refused with arithmetic" unit would surface the
build-from-source question in week one rather than after two closed tasks.

## What to keep

The instincts worth preserving verbatim: checksum binding must be scripted, not
eyeballed; declared architectures must match assets that actually exist (AC-4 is
correct and cheap); and "the installed bytes trace back to the release" is the
right non-negotiable. The reframe keeps all three — it just satisfies them with
the reproducible jar, checked at build time, instead of a pinned copy of an
artifact nobody can independently rebuild.

## Verdict

**rethink.** Right destination, wrong anchor and wrong seam. Concretely:
(1) invert AC-1 — build the jar from the tag inside the manifest and assert its
sha256 against the published `SHA256SUMS`, using an attested Maven-deps bundle
the way `flake.nix` uses `mvnHash`; (2) make AC-3's checksum binding the generic
`packaging/release-index.json` + renderer that #580/#581/#854 will each
otherwise reinvent; (3) record the Flathub reversal in ARCHITECTURE.md before
any manifest lands. If the openjdk-extension JDK floor makes (1) impossible,
that finding — written down — is worth more than the wrapper manifest, because
it is the fact that decides whether this channel exists at all.
