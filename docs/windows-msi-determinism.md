# Deterministic Windows msi (issue #190, installer child of #188)

The Windows lane of `scripts/build-installer.sh` produces the msi with
a single `jpackage --type msi` call, which drives the WiX Toolset
internally. jpackage exposes none of WiX's identity or timestamp
controls, so two builds of one commit differ byte-wise even when every
input is identical. This document enumerates the volatile byte set,
describes the normalization pass that now runs in the Windows lane
(`scripts/normalize-msi.py`), and states exactly what has and has not
been verified — per the honesty gate of #188 §10, **no
reproducibility claim is made here**: that claim requires a passing
double-build gate on a real Windows runner, which has not run yet.

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

## Verified vs. not verified

Verified (Linux, self-test + CLI fixtures):

- two synthetic msi images differing only in the four volatile regions
  normalize to byte-identical output (equal SHA-256);
- normalization is idempotent and refuses malformed input untouched.

**Not yet verified** — these are the open items of #190, all of which
need a Windows runner or VM:

1. **P1/P2 measurement**: build the real msi twice on
   `windows-latest`, run `diffoscope` before and after normalization,
   and confirm the residual is exactly the enumerated set (in
   particular: that ProductCode is indeed stable, that WiX's cab uses
   `flags == 0`, and that no additional stream varies).
2. **Install semantics**: a normalized msi must install, associate
   `.jls`, upgrade over a previous version, and uninstall cleanly on a
   clean Windows VM before any release relies on it.
3. **CI gate**: the `windows-installer-reproducibility` leg of
   `ci.yml` now double-builds the msi on `windows-latest`, normalizes
   each build via `normalize-msi.py`, and requires the two to be
   byte-identical (uploading the pair for offline attribution on any
   mismatch, since diffoscope's msi support needs msitools, absent on
   the Windows image). It runs `continue-on-error` while its stability
   record accrues — the same promotion rule the Windows build lane
   uses — so a red leg surfaces a residual (honesty gate, #188 §10)
   without yet blocking. Promote it to a required gate, and add the
   same comparison to `release.yml`, once (1)/(2) confirm byte-identical
   normalized output and clean install semantics. Until then releases
   keep the per-installer provenance attestation as the integrity
   guarantee and this document as the bounded-residual statement.

If measurement shows a residual beyond the enumerated set, that
falsifies #190 H1 and the fallback is documented bounded residual
(H2 b), not forced rewriting.
