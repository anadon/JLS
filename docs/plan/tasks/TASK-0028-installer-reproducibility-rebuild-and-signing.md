# TASK-0028 - Installer reproducibility, independent rebuild and signing

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

Five open issues (#184, #185, #188, #190, #191, #134) share one build pipeline,
and most of the pipeline exists at HEAD. This task closes the residual, and the
residual splits cleanly into "finish it" and "bound it honestly".

**Finish it.**

1. **The container image's apt pinning and the BOM reproducibility guard**
   (#184). `docs/reproducibility.md:196-201` records the container image as out
   of scope precisely because it installs distribution packages at build time.
   Pin them, or state a bounded-residual scope for the image the way §1 does for
   the specified artifacts. The BOM guard is the cheaper half: assert in CI that
   `target/bom.json` is byte-identical across two builds of the same commit.
2. **The independent-rebuild verification** (#185). `release.yml:82-95` already
   runs `maven-artifact-plugin:3.6.0:buildinfo` in the same Maven session and
   publishes `target/*.buildinfo` alongside `bom.json` and `SHA256SUMS`.
   `docs/reproducibility.md:79-126` documents the rebuild and
   `mvn artifact:compare` recipes. What is missing is a **CI leg that performs
   the independent rebuild** rather than documenting it, and the
   reproducible-central submission `docs/reproducibility.md:203-209` names as
   future work.
3. **Windows Authenticode signing** (#134). `release.yml:319-330` and
   `:564-620` carry the complete SignPath wiring behind a `SIGNPATH_ENROLLED`
   flag that is `'false'`, with every signing and verification step skipping
   cleanly and a `verify-windows-signatures` job that runs `osslsigncode` when
   signing ran. The residual is **enrollment and one proven run**: the
   maintainer enrolls in SignPath's OSS program, sets the two non-secret
   values, and a force-sign dry run exercises the path before a real tag. This
   is a maintainer action with an engineering follow-through, not an
   engineering task with a maintainer follow-through; say so.

**Bound it honestly.**

4. **The msi residual, closed as a bounded claim** (#190). `normalize-msi.py`
   rewrites the WiX package-code GUID, the SummaryInformation FILETIMEs, the
   CFB directory timestamps and the cabinet member times, and the embedded
   cabinet payload *is* reproducible. jpackage/WiX regenerates non-payload
   database identifiers (component GUIDs, ProductCode) with no override, so the
   msi **cannot** be byte-identical with the current toolchain. Deliverable:
   a declared **payload-reproducible** scope in `docs/reproducibility.md` §1
   with a gate that asserts the cabinet payload digest is stable across two
   builds - a real gate over a real property, instead of no gate over an
   unreachable one.
5. **The dmg residual, likewise** (#191). `normalize-dmg.py` pins the koly UDIF
   `SegmentID`; the full HFS+ normalization stays disabled because its
   round-trip corrupts the image on macOS. Same treatment: declare what is
   pinned, gate that, and record what is not and why.
6. **`docs/reproducibility.md` §1's table expanded and §6 pruned** to match
   whatever ends up true, so the document keeps its property of never claiming
   more than it gates.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-010 | The trust half: a third party can verify what they downloaded, and Windows users stop seeing an unsigned-publisher warning. TASK-0027 owns the delivery half |

## Prerequisite tasks

None. Every gate here runs over artifacts that build at HEAD. TASK-0027 and
this task are independent: `ci.yml`'s reproducibility jobs invoke
`scripts/build-installer.sh` directly and do not read `release.yml`'s
`experimental` flags.

## Acceptance test

`test/jls/ReproducibilityScopeTest.java`, new - a drift test in the
`CliFlagTableTest` family:

- `everyArtifactClaimedReproducibleHasAGate()` - parses the §1 specified-artifact
  table of `docs/reproducibility.md`, and for each row asserts a matching
  double-build job name exists in `.github/workflows/ci.yml`. **The point is the
  converse too**: a row with no gate fails. This is the assertion that keeps the
  document from claiming ahead of the pipeline.
- `boundedResidualRowsNameTheirResidual()` - asserts every row marked
  payload-reproducible or bounded-residual carries a non-empty residual
  description and a link to its owning issue.
- `theBomGuardExists()` - asserts a `bom.json` double-build comparison step is
  present. **Fails at HEAD.**

CI additions, which are the substantive acceptance evidence:

- `installer-payload-reproducibility` (Windows) - builds the msi twice and
  asserts the extracted cabinet payload digests are equal, failing with
  `diffoscope` output on mismatch. Mirrors the existing
  `installer-reproducibility` job's shape (`ci.yml:866`).
- `independent-rebuild` - rebuilds the jar from the published `.buildinfo` on a
  differently-configured runner and asserts the artifact digest matches. This
  is the assertion #185 is about, and nothing at HEAD performs it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 188 | Deterministic native installers: per-format byte-reproducibility program | closes - the parent program; deb/rpm/AppImage already gate, this task lands the remaining two as bounded claims |
| 185 | Reproducible Builds conformance: independent-rebuild verification, published `.buildinfo`, and a declared reproducible-artifact scope | closes |
| 184 | Release-artifact reproducibility gaps: container apt pinning, installer `SOURCE_DATE_EPOCH`, and a BOM reproducibility guard in CI | closes - `SOURCE_DATE_EPOCH` is already done (`docs/reproducibility.md:148-160`); the apt pinning and the BOM guard are not |
| 190 | Deterministic Windows msi: reproducible (or bounded-residual) msi | closes - as a bounded residual, which is the option the issue title itself allows |
| 191 | Deterministic macOS installer: reproducible (or bounded-residual) dmg | closes - same |
| 134 | Authenticode-sign the Windows installers (SignPath OSS / Azure Trusted Signing) | closes - blocked on maintainer enrollment, not on code |

## Notes

- **The single largest trap is claiming byte-identity where the toolchain
  forbids it.** #190's history is a double-build gate that ran on a real Windows
  runner and was then **intentionally removed** because it could never pass.
  Reintroducing it would be a regression dressed as rigor. The payload gate is
  the version of that gate that can hold.
- **The dmg's disabled normalization is not laziness.** The full HFS+
  round-trip corrupts the image on macOS, and the shipped koly-only dmg passes
  `hdiutil verify`. Any re-enablement must re-run
  `scripts/measure-dmg-repro.sh` and pass `hdiutil verify` before it ships.
- **#134 is gated on a person, not a program.** Every step is written and
  skips cleanly. The plan must say "the maintainer enrolls" rather than
  restating the wiring as if it were work - and must not treat the absence of
  enrollment as an argument against the capability.
- **Do not re-pin what is already pinned.** All actions are pinned to full
  commit SHAs (`repro-installers.yml:21-22`); Dependabot keeps them fresh.
- **`SOURCE_DATE_EPOCH` derives from `project.build.outputTimestamp`** - the
  same stamp that makes the jar reproducible - and `clamp_mtimes()` re-stamps
  the staged trees before `jpackage` sees them. The rpm leg additionally stages
  a `.rpmmacros` pinning `%_buildhost` and the two source-date macros. A change
  to the pom timestamp moves all of it at once.
- **The aarch64 lane was promoted from advisory to a hard gate** after a
  sustained green record (#188 §7 item 4). That is the template for every
  promotion in this task: measure first, promote second.

## Evidence

- `docs/reproducibility.md:11-26` (§1 specified artifacts), `:47-61` (what must
  match), `:79-126` (the rebuild and `artifact:compare` recipes), `:127-147`
  (the CI gate), `:148-201` (§5, the installer and container status, verbatim
  on msi and dmg), `:203-209` (§6 future work).
- `docs/windows-msi-determinism.md:27-141` - the volatile byte set, what
  `normalize-msi.py` does, and the measurement result.
- `docs/dmg-reproducibility.md:100-158` (variance inventory), `:190-255`
  (the normalization plan), `:256-268` (the validation checklist).
- `.github/workflows/ci.yml:866` (`installer-reproducibility`), `:1000`
  (`windows-installer-msi`), `:1064` (`macos-installer-reproducibility`),
  `:798` (`reproducibility`) - the four jobs that exist at HEAD.
- `.github/workflows/release.yml:82-95` (buildinfo goal and `SHA256SUMS`),
  `:113-114` (bom and buildinfo assets), `:319-330` (the `SIGNPATH_ENROLLED`
  flag and its stated semantics), `:564-620` (the sign and note-unsigned
  steps), `:661-662` (the `osslsigncode` verification job).
- `.github/workflows/repro-installers.yml:1-31` - the report-only monthly probe
  and its stated promotion rule.
- `scripts/normalize-msi.py`, `scripts/normalize-dmg.py`,
  `scripts/measure-dmg-repro.sh` - present at HEAD.
