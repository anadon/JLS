# Deterministic Windows msi (issue #190, installer child of #188)

The Windows lane of `scripts/build-installer.sh` produces the msi with
a single `jpackage --type msi` call, which drives the WiX Toolset
internally. jpackage exposes none of WiX's identity or timestamp
controls, so two builds of one commit differ byte-wise even when every
input is identical. This document enumerates the volatile byte set,
describes the normalization pass that now runs in the Windows lane
(`scripts/normalize-msi.py`), and records the result of the real
`windows-latest` double-build measurement.

**Conclusion (measured, see "Measurement result" below): the Windows
msi is not byte-reproducible with the jpackage/WiX toolchain, and this
is an upstream limitation JLS cannot close from the build script.** The
installed *payload* (the embedded cabinet: the jlink runtime and the
application jar) now IS byte-reproducible; the irreducible residual is
in the MSI **database** streams, which jpackage regenerates with
per-build identifiers (component GUIDs, ProductCode, and row ordering)
that it exposes no control over. Normalizing the payload and the four
timestamp/GUID regions below is real and worthwhile; a *fully*
byte-identical msi would require either an upstream jpackage fix or a
full MSI-relational-database canonicalizer, which is out of proportion
to the benefit. The CI reproducibility check was therefore **removed**
rather than kept as a perpetually-red measurement (see "Standing
posture" below).

## The volatile byte set

An msi is an MS-CFB compound file (the OLE structured-storage format)
holding the installer database streams, a `\x05SummaryInformation`
property-set stream, and one embedded cabinet stream with the payload
files. Four regions are per-build:

1. **Package code** — SummaryInformation property 9
   (`PID_REVNUMBER`), a braced GUID. WiX's linker generates it
   randomly on every link (the authoring convention is `PackageCode
   ="*"`), and jpackage offers no override. This is the residual #184
   H3 predicted and #190 H1 names.
2. **SummaryInformation FILETIMEs** — properties 11/12/13
   (last-printed, create, last-save) carry the build wall-clock.
3. **CFB directory timestamps** — every structured-storage directory
   entry has 8-byte creation/modification FILETIMEs.
4. **Cabinet member times** — each `CFFILE` entry in the embedded cab
   stores the DOS date/time of the staged file's mtime at packaging
   time; the staged tree is recreated per build, so these are the
   build time too (until #188's staged-tree mtime clamp lands — and
   possibly after it, since jpackage copies the tree internally).

Expected to be **stable already** (to be confirmed by measurement, not
assumed): the ProductCode and UpgradeCode rows of the MSI database —
jpackage derives them name-based from vendor/name/version rather than
randomly. The database streams themselves are generated from the same
inputs each build.

## What `normalize-msi.py` does

`scripts/normalize-msi.py` (stdlib-only Python, runs on a bare GitHub
runner) rewrites exactly the four regions above, in place, with
same-length patches — file structure, offsets, and size never change:

- CFB directory timestamps are zeroed ("not set" is valid per
  MS-CFB);
- SummaryInformation FILETIMEs and cab member times are clamped to
  `SOURCE_DATE_EPOCH` (the build lane passes the commit date until
  #188's shared export lands and takes precedence);
- the package code is replaced by a GUID derived from the SHA-256 of
  the whole file with the package-code bytes masked.

The content-derived package code preserves Windows Installer's
semantics: the package code is the identity of the *file*, two
byte-identical packages are legitimately the same package, and any
content change (version bump, payload change, even a one-byte edit)
yields a fresh GUID — so the installer cache can never confuse two
genuinely different msis. ProductCode/UpgradeCode are deliberately not
touched, so install/upgrade/uninstall logic is unaffected.

Failure posture: anything structurally unexpected (missing summary
stream, non-GUID package code, multi-part or reserve-flagged cabinet,
truncated chains) is a **hard error** — the release fails rather than
shipping a silently half-normalized msi. `JLS_SKIP_MSI_NORMALIZE=1`
is the explicit escape hatch, at the documented cost of a
non-deterministic msi. The tool's `--self-test` (synthetic compound
files: convergence of two divergent builds, idempotence, package-code
content-sensitivity, malformed-input refusal) runs in the Windows lane
before the tool is allowed near the real msi.

## Measurement result

The `windows-latest` double-build measurement of #190 H1 has now run
(CI run 29763534341, job "Windows installer reproducibility (msi)").
Two things had to be fixed first for the measurement to be meaningful:

- **Parser completeness.** Real jpackage msis are MS-CFB *major
  version 4* (4096-byte sectors) whose physical file ends mid-sector;
  `normalize-msi.py` initially crashed on them ("sector N beyond end
  of file"). The reader now zero-pads the parse buffer to a whole
  sector (mirroring how Windows/olefile tolerate a short final sector)
  so it parses the real installer. See the `--self-test` cases.
- **Per-stream attribution.** `normalize-msi.py --diff A.msi B.msi`
  reuses the CFB reader to name every divergent stream (decoding MSI's
  private-range table-name mangling to readable table names), and, for
  the cabinet, to localize the difference to CFFILE metadata vs the
  compressed CFDATA payload. The Windows lane runs it on any mismatch,
  so the residual is a bounded, inspectable claim printed straight into
  the CI log even though the diverged artifacts cannot be downloaded.

The measurement outcome, after normalization of both builds:

- **The payload is reproducible.** The embedded cabinet stream
  (`Disk1Cab`) is byte-identical between the two builds — the jlink
  runtime, the application jar, and every CFFILE (names, order, sizes,
  clamped times) match. The four enumerated volatile regions and the
  compressed payload are fully handled.
- **The MSI database is not.** Every divergent byte is in the
  relational-database streams: `_StringData`, `_StringPool`,
  `Component`, `File`, `Directory`, `Registry`, `RemoveFile`,
  `FeatureComponents`, `Property`, and `MsiFileHash` all differ, each
  at the *same size* as its counterpart — values swapping in place, not
  content growing. That is the signature of jpackage generating
  per-build **component GUIDs and identifiers** (and a per-build
  **ProductCode** — hence `Property` differs, falsifying the earlier
  "ProductCode expected stable" assumption), which cascade through
  every table that references them and through the shared string pool.
  `SummaryInformation` also differs, but only as a *cascade*: the
  content-derived package code legitimately changes once anything else
  does.

This **falsifies #190 H1** ("the residual is exactly the four
regions") and lands on the documented bounded-residual fallback
(#190 H2 b): the msi database non-determinism is upstream in
jpackage/WiX, which exposes no control over these identifiers, so JLS
cannot close it from `build-installer.sh`. Closing it would require
either an upstream jpackage change (deterministic component GUIDs /
ProductCode) or a full MSI relational-database canonicalizer inside
`normalize-msi.py` (parse `_Tables`/`_Columns`, re-key the random GUIDs
to content-derived values, re-sort every table's rows, and rebuild
`_StringData`/`_StringPool` — effectively reimplementing part of
libmsi). Neither is justified by the benefit: the payload a user
installs is already reproducible, and provenance attestation covers
installer integrity.

## Standing posture

- **The msi reproducibility check has been removed** from `ci.yml`.
  Because the residual is a permanent upstream limitation rather than a
  regression to drive to zero, a perpetually-red double-build leg was
  noise, not signal. `scripts/normalize-msi.py` still runs in the build
  (it strips the volatile timestamp/GUID set and makes the payload
  reproducible), and its `--diff` mode remains available to attribute a
  residual on demand — but nothing in CI asserts the whole msi is
  byte-identical, because it is not and cannot be with this toolchain.
- Verified on Linux (self-test + CLI fixtures): two synthetic msi
  images — 512- and 4096-byte-sector, aligned and mid-sector-truncated
  — differing only in the volatile regions normalize to byte-identical
  output; normalization is idempotent, length-preserving, and refuses
  malformed input untouched.
- Still requires a Windows VM to confirm (not a reproducibility
  question): that a normalized msi installs, associates `.jls`,
  upgrades over a previous version, and uninstalls cleanly.

If a future jpackage/JDK exposes deterministic component-GUID and
ProductCode generation, revisit: with the payload already reproducible,
that upstream lever alone would make the whole msi byte-identical and
the leg could then be promoted to a gate.
