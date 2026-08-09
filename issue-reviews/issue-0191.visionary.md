# Issue #191: Deterministic macOS installer: reproducible (or bounded-residual) dmg
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Not identical dmg bytes. The purpose, stated in its own Intended Audience section, is
that "any party can rebuild and compare the release artifact instead of trusting the
runner." Byte-identity is one implementation of that property, chosen because it is the
implementation the Reproducible Builds definition names. The whole of #188 is organized
around that one implementation, per format, and #191 is the last format where it does
not work.

The project has already discovered the better seam and declined to generalize it.
`docs/windows-msi-determinism.md` closes #190 with: *"The installed payload (the embedded
cabinet: the jlink runtime and the application jar) now IS byte-reproducible; the
irreducible residual is in the MSI database streams."* That sentence is the real result of
the whole program. It was recorded as a consolation prize inside one format's postmortem
instead of being promoted to a claim, a gate, and a release asset. #191 is about to
produce the identical sentence for the dmg — envelope volatile, payload stable — and, if
it follows the template, bury it the same way.

## 1. Even the success branch of P3 buys less than it reads

Suppose Route B works and two `macos-latest` builds hash equal. What has been proven is
*same-runner-image determinism*, not the §1 property of `docs/reproducibility.md` ("any
party can recreate bit-by-bit identical copies"). The dmg's bytes are produced by
`hdiutil`, a closed Apple tool with no version pin, no archive, and no way for a third
party to install the build's exact copy. `repro-installers.yml` says so in its own header
comment: the monthly cron exists to catch "runner image updates swapping dpkg/WiX/hdiutil
versions underneath us." For deb/rpm the verifier can pin `dpkg-deb` in a container and
genuinely reproduce; for the dmg, a verifier on macOS 27 cannot reproduce a macOS 26
image even in principle, and a verifier on Linux cannot run the check at all.

So the gate branch of P3 would put a green "reproducible" checkmark on a claim materially
weaker than the one the same word carries three rows up in the §1 table — inside a program
whose Global Invariant 2 is an honesty gate against exactly that. The failure mode here is
not over-claiming a residual; it is over-claiming a *gate*.

## 2. Route B pulls against the project's stated architecture

Route B means `build-installer.sh` takes over `MacDmgPackager`'s sequence: create UDRW,
patch the header before the Finder phase, run `DMGsetup.scpt`, clamp, convert, udifrez,
pin. #188 §1 lists installer creation as owned by #82 and out of scope here, and Global
Invariant 1 is "no content change" — Route B is a change of *who constructs the installer*,
which is the largest content-risk change available in this program. It also forces an
`Info.plist` post-edit to keep the `.jls` association (`--file-associations` is rejected
for `--type app-image` on macOS), i.e. hand-editing the bundle metadata that #82 owns, on
the one platform where nobody has ever installed the result on real hardware
(`release.yml:308` — `macos-latest` leg, "uninstalled on real hardware (#82)"). Invariant 3
("never ship a corrupting normalization") has already been cashed once by Route A. Trading
a shipping, verified-mountable koly-only dmg for a hand-rolled imaging pipeline, to obtain
the degraded property of §1, is the worst trade on the board.

## 3. Alternative: make *payload* reproducibility the claim, gated and shipped

The seam to cut along is the one `build-installer.sh` already has. `$INPUT` (the shaded
jar) plus `$RUNTIME` (the jlink image) is the staged tree every format wraps — deb, rpm,
AppImage, msi, dmg — and it is already mtime-clamped at line 169 before any packager sees
it. Concretely:

- **Tier 1 (cheap, works today).** `scripts/payload-manifest.sh`: emit sorted
  `sha256  mode  relpath` lines over `$INPUT` + `$RUNTIME`, hash that manifest to one
  digest, write it next to the installers. No packaging tools, no `hdiutil`, identical
  code on all five runners. Gate it: two builds of one commit must produce the same digest
  — a *hard* gate on macOS and Windows, achievable today, that fails only on a real
  regression (jlink or shading nondeterminism), which is the one failure a user would
  actually care about.
- **Tier 2 (optional).** Build `jpackage --type app-image` once, manifest that tree, and
  hand it to each packager via `--app-image`. This extends the claim to the generated
  launcher and `Info.plist`, and removes the duplicated jlink/stage work. It inherits the
  macOS `--file-associations` wrinkle from Route B, so it is genuinely optional; Tier 1
  already covers the overwhelming majority of installed bytes.
- **Ship the verification.** Publish `PAYLOAD-<os>-<arch>.sha256` beside
  `SHA256SUMS-installers-<os>-<arch>`, and add `scripts/verify-installer.sh` that extracts
  the payload from a downloaded artifact (`7z`/`ar`/`hdiutil attach`) and compares it to a
  locally rebuilt manifest. A verifier on Linux can then check a macOS dmg's payload — a
  thing byte-identity will never permit — and a verifier who cannot rebuild at all still
  gets a manifest to diff against the mounted volume.
- **Restate the docs.** `docs/reproducibility.md` §1 gains a column: *artifact bytes
  reproducible* / *installed payload reproducible* / *integrity*. `msi` and `dmg` become
  "envelope: no (bounded residual, attested) — payload: **yes, CI-gated**." That is a
  stronger and truer public statement than either #190 or #191 currently produces, and it
  retires the framing problem for every future format.

Note that `scripts/measure-dmg-repro.sh` already computes `payload-content.diff` as a
per-file sha256 manifest of the mounted volume. The measurement apparatus for the claim I
am recommending exists; it is filed as a diagnostic instead of as the product.

## 4. Radical alternative: delete the envelope

The dmg exists for a background picture, an Applications symlink, and a GPL license sheet.
The app is unsigned and un-notarized by deliberate policy (#128/#135), so first launch
already requires the documented right-click-Open dance. `ditto -c -k --sequesterRsrc
--keepParent JLS.app JLS-<v>-aarch64.zip` is Apple's own app-shipping container (it is what
notarization submissions use), preserves symlinks/resource forks/permissions, and is a zip
— the one archive format this project already reproduces bit-for-bit. Every variance class
F1–F6 in `docs/dmg-reproducibility.md` disappears because HFS+ disappears. The costs are
honest and small: no license sheet (LICENSE ships inside the bundle and in the repo), no
Finder window art, and a slightly less idiomatic download. Given the dmg lane is
`experimental: true` and has never been installed by a human on real hardware, "the dmg is
the macOS idiom" is currently an assumption of the same species the whole #188 program was
created to refute. The low-risk version: ship the zip *alongside* the dmg as the
reproducible macOS path, and the dmg's residual becomes permanently uninteresting rather
than a research program.

## 5. Priority inversion, and the highest-leverage unfiled action

- **P4 has never been performed, for any dmg.** Mount, launch, `.jls` association,
  Gatekeeper right-click-Open on real hardware — Open Question 3 defers it as a rider, and
  every branch of this issue is gated on it (DoD bullet 4). It is also the only macOS work
  here that a student or instructor would ever notice. It should be its own issue,
  executed first, and not held hostage to a byte-comparison result.
- **Nobody has filed upstream.** `docs/windows-msi-determinism.md` names the upstream
  jpackage lever as the only thing that could close the msi, and the same is true of the
  dmg (jpackage exposes no `hdiutil` control and does not honor `SOURCE_DATE_EPOCH` in
  either packager). Two rigorous variance inventories are already written. Filing one JDK
  bug with them attached costs an hour and is the only action available that could close
  #190 and #191 simultaneously and permanently. It currently belongs to no issue.

## 6. Acceptance criteria I am explicitly disregarding

- **DoD 1 (run P2, attribute the residual to F1–F6) and DoD 2 (route decision).** The
  decision is already determined by the evidence in the body: F3–F5 are Finder-serialized
  at wall clock and unreachable, F4's bookmark blob embeds a volume UUID created before any
  post-pass can run, and Route A corrupts. Spending a Mac session to re-derive a conclusion
  the doc already states is measurement as ritual. Record H2(c) now, on the evidence
  already cited, and say so.
- **The gate half of DoD 3.** Do not promote the macOS leg to a hard gate even if it ever
  goes green, for the reason in §1 — or promote it only under a different name
  ("same-runner determinism"), which is a claim worth having and is not what the table
  means by reproducible.
- Keep, unchanged: DoD 4 (validation), DoD 5 (the stale "Status at commit time" block,
  which currently denies plumbing that exists — a live correctness defect in a doc whose
  entire purpose is honesty), the residual statement, and the `STATUS:` on #188.

Adjacent drift worth one line in the same landing: README lines 53–60 still say "the
installers are *not* byte-reproducible," which `docs/reproducibility.md` §1 has contradicted
for the Linux formats since #189. The public claim surface is where this program's value
actually lives, and it is already out of sync.

## Close plan

1. Close #191 on H2(c) with the residual enumerated as F1–F6 and the koly pin retained.
2. File **payload reproducibility** as the successor task under #188 (Tier 1 gate +
   `PAYLOAD-*.sha256` asset + `verify-installer.sh` + the §1 table column), replacing
   per-format byte-identity as the program's organizing goal.
3. File the human macOS hardware validation (#82 successor) and do it.
4. File the upstream jpackage determinism bug with both inventories attached.
5. Optionally evaluate the `ditto` zip as a second, genuinely reproducible macOS asset.

If the maintainer's aim is instead that JLS be a showcase of reproducibility practice, the
showcase is #188's three green Linux gates plus a cross-format payload claim no comparable
project publishes — not an HFS+ image whose bytes no third party can ever recreate.
