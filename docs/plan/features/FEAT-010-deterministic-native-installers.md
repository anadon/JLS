# FEAT-010 - Deterministic native installers and file association

**Status:** proposed | **Cost:** 8-16 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A person who has never installed a JDK can install JLS from a native package
for their operating system, double-click a `.jls` file and have it open in the
editor. The bytes of that package are either reproducible from source by an
independent rebuilder or carry a published, bounded and named residual, and the
Windows artifacts are signed so the operating system does not warn the user
away from them. This removes the first barrier a new user and a whole classroom
hit, and it converts seven open distribution issues from an unordered pile into
one build pipeline with one acceptance surface.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | beneficial | Seven open issues on one pipeline; the bring-your-own-JDK barrier is the first thing a new user hits. Its full band is booked against CAP-06, which requires it |
| CAP-06 | required | A course of 300 cannot be asked to install a JDK; the install step is where a lab loses its first hour |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-007 | The reproducibility check and the per-format install-and-smoke legs are CI jobs on three operating systems; they need the platform lanes to be required checks rather than best-effort, or a broken installer leg is invisible |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0027 | Native installers per OS with file association | The pipeline itself and the `.jls` association; the residual on the shipped script is the five-leg matrix promotion and the runtime pin |
| TASK-0028 | Installer reproducibility, independent rebuild and signing | Byte-reproducibility or a declared bounded residual per format, the BOM guard, and Authenticode signing |

## Acceptance criteria

1. For each of deb, rpm, AppImage, msi and dmg, a job builds the installer,
   installs it on the runner that built it, launches JLS and exits cleanly.
   None of the five legs is marked experimental.
2. A `.jls` file opened from the desktop file manager opens in JLS, asserted by
   a per-platform check rather than by a screenshot.
3. Every runtime input is pinned by digest. No artifact in the pipeline is
   fetched by a mutable tag or carries an unverified placeholder hash.
4. For each format, either two independent builds of the same source produce
   byte-identical output, or a published record names the differing bytes, the
   reason, and the bound. A format with neither is a failing check, not an
   undocumented one.
5. The Windows installers are signed, and the signature is verified in CI by a
   check that fails if the signature is absent or does not chain.
6. A bill-of-materials guard fails the build when a dependency changes without
   the record changing.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 82 | Distribution: jpackage installers per OS and `.jls` file association - remove the bring-your-own-JDK barrier | closes |
| 188 | Deterministic native installers: per-format byte-reproducibility program | tracking / closes |
| 185 | Reproducible Builds conformance: independent-rebuild verification, published `.buildinfo`, and a declared reproducible-artifact scope | closes |
| 184 | Release-artifact reproducibility gaps: container apt pinning, installer `SOURCE_DATE_EPOCH`, and a BOM reproducibility guard in CI | closes |
| 191 | Deterministic macOS installer: reproducible (or bounded-residual) dmg | closes |
| 190 | Deterministic Windows installer: reproducible (or bounded-residual) msi | closes |
| 134 | Authenticode-sign the Windows installers (SignPath OSS / Azure Trusted Signing) | closes |
| 189 | Deterministic Linux installers: byte-reproducible deb, rpm, and AppImage | informs, **closed** - the Linux half of the same program; its method is the template for the remaining three formats |

## Design notes

The pipeline is not greenfield and the two tasks must be scoped against what
ships. `scripts/build-installer.sh` already runs jar to jdeps to jlink to
jpackage for all five formats and passes `--file-associations`;
`.github/workflows/release.yml` already runs a five-leg matrix that installs and
smoke-tests each artifact on the runner that built it. What remains is narrow
and named in the tree itself: four of five legs are `experimental: true` pending
one green run, and the bundled-runtime hash is an unverified placeholder. Anyone
scoping this feature as "write installers" will rewrite working code.

Reproducibility is a claim about a stated scope, not an absolute. `#185` already
frames it as a declared scope plus a residual, and `docs/reproducibility.md`
records the build-side conventions; `scripts/normalize-dmg.py` and
`scripts/normalize-msi.py` exist because the residual for those two formats is
known to be nonzero. The correct output of this feature is a published record
per format, not a uniform claim.

Signing is blocked on enrollment in an OSS signing program, not on code. The
workflow wiring exists; treat the enrollment as a scheduled dependency with a
calendar cost rather than as engineering work.

## Risks

- **The residual for dmg and msi may not be bounded at all.** Both formats
  embed timestamps and platform-specific metadata that the tooling does not
  expose. Mitigation is criterion 4's second branch: publish the residual and
  narrow the claim rather than dropping it.
- **Signing depends on a third party.** An OSS signing grant can be refused or
  delayed. The feature ships unsigned with the check recorded as pending rather
  than deleting the check.
- **Five formats is five maintenance surfaces at bus factor 1.** Every format
  promoted from experimental to required is a lane that can turn the release
  red. The mitigation is that each leg installs and smoke-tests on the runner
  that built it, so a break is attributable to one format.

## Evidence

- The pipeline at HEAD: `scripts/build-installer.sh` (jar to jdeps to jlink to
  jpackage for deb, rpm, AppImage, msi, dmg, with `--file-associations`), and
  the `installers` job in `.github/workflows/release.yml`.
- The residual tooling that exists because the residual is nonzero:
  `scripts/normalize-dmg.py`, `scripts/normalize-msi.py`,
  `scripts/measure-dmg-repro.sh`, `docs/dmg-reproducibility.md`,
  `docs/windows-msi-determinism.md`, `docs/reproducibility.md`.
- The seven open issues in the table above, all verified open against
  `list_issues(state=OPEN)`.
- Owner: **UNOWNED** in `docs/capability-roadmap/`. The committed roadmap does
  not pay for distribution, which is the structural reason seven issues on one
  pipeline stayed open.
- **Cost reconciliation.** Band 8-16 mw; TASK-0027 and TASK-0028 total 4 wk.
  The two tasks are the leading slices - the promotion of the matrix and the
  first reproducibility record. The residual is per-format hardening across
  five formats and three operating systems, which the closed task id space
  (TASK-0001 through TASK-0112) does not name. Do not read 4 wk as the feature.
