# macOS dmg reproducibility (issue #191)

Groundwork for making the macOS installer reproducible, or failing
that, bounding its residual precisely. Everything in sections 1–2 is
verified against the shipped packager source, not assumed; nothing
here claims a measurement that has not run. The double-build
measurement itself (issue #191 P1/P2) requires a macOS machine —
`hdiutil` exists nowhere else — and is packaged, ready to run, as
`scripts/measure-dmg-repro.sh`.

**Implementation status (this branch).** Route A's checksum-safe
normalization now exists as `scripts/normalize-dmg.py` — a stdlib-only
tool with two in-place, same-length rewrites and a `--self-test` that
proves convergence, idempotence, content-sensitivity of the derived
identifiers, and malformed-input refusal on synthetic images (so it is
verifiable off a Mac, exactly like `normalize-msi.py` for #190):

- `koly` mode pins the UDIF trailer `SegmentID` (E1) on the finished
  compressed dmg — outside every UDIF checksum, so it is safe with no
  round-trip and no risk to the license resource. This is wired into
  the macOS lane of `build-installer.sh` **by default**.
- `hfs` mode pins the HFS+ volume-header dates and volume UUID (F1) on
  the *uncompressed* read-write image before it is re-compressed — the
  only checksum-safe window for the volume header. It refuses anything
  that is not a bare HFS+/HFSX volume (signature at offset 1024). This
  is wired behind the **opt-in** `JLS_DMG_FULL_NORMALIZE=1`, because
  the read-write round-trip it needs (§4 Route A steps 1–5) has not yet
  been exercised on real macOS hardware; a maintainer validates it per
  §5 before it enters the default release path.

The `macos-installer-reproducibility` leg of `ci.yml` now runs the
**full** pass (`JLS_DMG_FULL_NORMALIZE=1`) on the `macos-latest` runner
— the first place the Route-A read-write round-trip actually executes,
since `hdiutil` exists only on macOS. It double-builds, then **verifies
the normalized dmg still passes `hdiutil verify`, attaches, and exposes
`JLS.app` with a valid `Info.plist` and the `.jls` document type** (the
CI-runnable subset of the §5 checklist below; clean-VM install and
Gatekeeper still need a human), records whether the two dmgs match, and
on any residual runs diffoscope + uploads the pair. It stays
`continue-on-error` — an honest measurement, not a green "reproducible"
claim (#188 §10): Route A is expected to leave the Finder-written
`.DS_Store` bookmark blob (F4) as a bounded residual, so the leg
promotes to a gate only once that residual is confirmed-and-documented
or Route B closes it.

Status at commit time:

- No `SOURCE_DATE_EPOCH` or mtime-clamp plumbing exists anywhere in
  this repository yet (that shared work is issue #188 §7). Predictions
  below are tagged by whether that clamp can even reach them.
- The macOS release leg (`release.yml` `installers` matrix:
  `macos-latest`, Apple silicon, Temurin 25, `JLS_SKIP_BUILD=1`) is
  `experimental: true` and uninstalled on real hardware (#82).
- Integrity today comes from the per-installer provenance attestation
  plus `SHA256SUMS-installers-macos-aarch64`, not from reproducibility.

## 1. What the dmg lane actually runs

`scripts/build-installer.sh` (Darwin case) invokes a single
`jpackage --type dmg` with `--license-file LICENSE`, an icon, the
`.jls` file association, and `--mac-package-identifier
io.github.anadon.jls`, then renames the output to
`JLS-<version>-<arch>.dmg`.

Inside the JDK (verified in `jdk.jpackage`,
`macosx/classes/jdk/jpackage/internal/MacDmgPackager.java`, tag
`jdk-25-ga` — the release workflow pins `java-version: 25`), that one
call expands to:

1. `hdiutil create -quiet -srcfolder <app-image> -volname JLS -ov
   proto.dmg -fs HFS+ -format UDRW` — a read-write HFS+ image built
   from the staged `JLS.app` tree.
2. `hdiutil attach proto.dmg -quiet -mountroot <workdir>` — the image
   is mounted read-write.
3. Writes into the mounted volume, at build wall-clock time:
   - `.background/background.tiff` (copied in);
   - `osascript JLS-dmg-setup.scpt` — the bundled `DMGsetup.scpt`
     drives Finder: icon view, window bounds, background picture,
     icon positions, and `ln -s /Applications Applications`; Finder
     persists all of this by writing `.DS_Store` in the volume root;
   - `.VolumeIcon.icns`, plus `SetFile -c icnC` on it and `SetFile -a
     C` on the volume root when a working `SetFile` is found.
4. `hdiutil detach`.
5. `hdiutil convert proto.dmg -format UDZO -o JLS-<v>.dmg` — the
   shipped compressed image (no `-imagekey`, default zlib level).
6. `hdiutil udifrez JLS-<v>.dmg -xml JLS-license.plist` — the license
   (base64 of `LICENSE`, so itself deterministic) is attached as a
   UDIF resource because `--license-file` is passed.

jpackage exposes no control over image UUIDs, volume dates, or any
`hdiutil` flag in this sequence.

## 2. Variance inventory

Candidate nondeterminism, from UDIF/HFS+ format mechanics plus the
pipeline above. "Clamp" means #188's planned `SOURCE_DATE_EPOCH` +
staged-tree mtime clamp on `JLS.app` before jpackage runs.
Measurement (section 3) confirms or eliminates each candidate.

UDIF envelope:

- **E1 — koly `SegmentID`.** The 512-byte UDIF trailer ("koly block")
  at the end of the dmg carries a 16-byte segment UUID at trailer
  offsets 64–79, freshly randomized by every `hdiutil convert`.
  Not reachable by any clamp. Crucially it is *outside* every UDIF
  checksum (`DataChecksum` and `MasterChecksum` cover the data
  fork/blkx data, not the trailer), so it can be pinned post-hoc
  without corrupting the image.
- **E2 — checksums, blkx table, XML plist.** Pure functions of the
  compressed filesystem bytes; they vary only when something else
  does. No independent variance.

HFS+ filesystem inside the image:

- **F1 — volume header.** Bytes 1024–1535 of the raw image (mirrored
  in the alternate header starting 1024 bytes before the end):
  `createDate`, `modifyDate`, `backupDate`, `checkedDate` are
  wall-clock; `finderInfo[6..7]` is the 64-bit volume UUID randomized
  at creation; `writeCount`/`nextAllocation` churn during the mounted
  phase. Not reachable by the staged-tree clamp.
- **F2 — catalog dates of staged files.** `-srcfolder` copies the
  `.app` tree; modification dates follow the source, so the #188
  clamp covers them. HFS+ also stores creation, attribute-change and
  access dates per node — measurement must show what `hdiutil create`
  derives them from (source birthtime vs. copy time).
- **F3 — mounted-phase writes (key refinement of H1).**
  `.background/background.tiff`, `.VolumeIcon.icns`, the
  `Applications` symlink and `.DS_Store` are created *inside the
  mounted image* at build time. No clamp on the staged tree can ever
  reach their dates; issue #191's H1 wording ("dominated by the
  volume UUID and volume timestamps") understates this. P2 should
  expect residual timestamps on exactly these nodes even after #188
  lands.
- **F4 — `.DS_Store` content.** Layout data (positions, bounds,
  background reference) is fixed by the script, but Finder stores the
  background-picture reference as a bookmark/alias blob, and such
  blobs embed the volume UUID and creation dates — so `.DS_Store` can
  differ build-to-build even with a byte-identical layout script.
- **F5 — `.fseventsd`.** While the image is attached (the setup
  script holds it open with `delay 5`), fseventsd may drop
  `/.fseventsd/fseventsd-uuid` containing a fresh random UUID into
  the volume.
- **F6 — allocation layout.** File copy order and mounted-phase churn
  decide extent placement, `nextCatalogNodeID` and the allocation
  bitmap. Plausibly stable on one runner image; a candidate until
  measured.
- **F7 — payload `.app` bytes.** jlink runs per build; runtime-image
  and launcher determinism belong to #188/#189. The harness reports
  payload-content diffs separately from envelope diffs so this is
  attributed, never conflated with the dmg envelope.

## 3. Measurement protocol (P1/P2)

`scripts/measure-dmg-repro.sh` (macOS only) builds the installer
twice — the second run under `JLS_SKIP_BUILD=1`, so both dmgs wrap
the *same jar* and jar-lane variance (already double-build-gated in
`ci.yml`, #44) cannot pollute the attribution — then writes
`target/dmg-repro/` containing:

- `sha256.txt` — the headline P1 answer;
- `koly-*.hex`, `koly.diff`, `segment-id-*.txt` — E1 evidence;
- `imageinfo.diff` — `hdiutil imageinfo` envelope comparison;
- `raw-diff-ranges.txt` — both dmgs converted to raw (`UDTO`) and
  `cmp`'d, differing bytes aggregated into contiguous ranges and
  labeled when they fall in the primary/alternate volume header
  (F1) — this is the "exact, minimal set of bytes" record the issue
  asks for, without needing diffoscope on the runner;
- `payload-content.diff` / `payload-metadata.diff` — per-file sha256
  and per-node permission/size/mtime/birthtime manifests of the two
  mounted volumes (F2/F3/F4/F5/F7);
- `volume-info.diff` — `diskutil info` of each mounted volume,
  capturing the volume UUID (F1) directly;
- `SUMMARY.txt` — counts and the verdict.

The script is a measurement, not a gate: it exits 0 whether or not
the images match (it exits nonzero only when tooling fails). A CI
double-build gate is added only after byte-identical builds are
demonstrated — the honesty gate of #188 §10: no green checkmark may
say "reproducible" while any byte still varies. Run it locally on a
Mac, or as a temporary step on the `macos-latest` leg of a dry-run
workflow dispatch.

## 4. Normalization plan (H2)

One hard rule shapes everything: **never rewrite data-fork bytes of a
UDZO image after `convert`** — blkx chunk checksums, `DataChecksum`
and `MasterChecksum` cover them, and a patched image fails
`hdiutil verify`/`attach`. There are exactly two checksum-safe patch
windows: the raw UDRW image *before* `convert` (checksums are
computed afterwards, over patched bytes), and the koly trailer fields
*after* `convert` (the trailer is outside all checksums).

Two candidate routes, to be chosen by what P2 actually measures:

**Route A — post-pass on jpackage's dmg (smaller change).** Keep
`jpackage --type dmg` exactly as-is, then in `build-installer.sh`:

1. `hdiutil convert JLS-<v>.dmg -format UDRW -o work.dmg` (this
   drops the udifrez license resource; step 6 restores it).
2. Attach read-write with `-nobrowse`; delete `.fseventsd` (F5);
   clamp every node's modification date (`touch -t` from
   `SOURCE_DATE_EPOCH`) and creation date (`SetFile -d`, noting
   `SetFile` is deprecated — a small `python3` `setattrlist` fallback
   is the durable alternative) — covers F2/F3; detach.
3. Patch the UDRW volume header and alternate header in place:
   pin the volume UUID (`finderInfo[6..7]`) to a value derived from
   the release commit, and set
   `createDate`/`modifyDate`/`backupDate`/`checkedDate` from
   `SOURCE_DATE_EPOCH` (F1).
4. `hdiutil convert work.dmg -format UDZO -imagekey zlib-level=9 -o
   out.dmg` (level pinned explicitly rather than inheriting a
   default).
5. Re-apply `hdiutil udifrez out.dmg -xml <license plist>` with the
   same base64-of-LICENSE plist jpackage used.
6. Pin the koly `SegmentID` (trailer offsets 64–79) to the same
   commit-derived value (E1).

Expected end state for Route A: everything pinned except the
`.DS_Store` bookmark blob (F4) — its embedded dates/UUID were
serialized by Finder *at build time*, before steps 2–3 run, and
rewriting `.DS_Store` from outside Finder is exactly the brittle
surgery §9 warns against. If measurement confirms that, Route A lands
on **H2(b)**: a bounded residual of one file's alias blob (plus
anything F6 shows), documented, attestation-covered.

**Route B — own the imaging (candidate for byte-identical, H2(a)).**
Build the `.app` via `jpackage --type app-image` (as the Linux
AppImage lane already does), then script the image creation directly,
inserting determinism at the right points: create UDRW → patch header
UUID/dates *before* the Finder phase (so anything Finder serializes,
bookmark blobs included, embeds the pinned values) → attach → run the
same layout script → clamp dates → detach → re-patch header
`modifyDate`/`checkedDate` → convert → udifrez → pin `SegmentID`.
Byte-identical output is plausible here because every input to every
serialized blob is fixed. Costs: `build-installer.sh` takes over
~40 lines of jpackage behavior, and two things must be verified on a
Mac first — that the `.jls` association (`CFBundleDocumentTypes`)
still lands in the app-image's `Info.plist` (macOS `jpackage` rejects
`--file-associations` for `--type app-image`, so the plist would need
a post-edit; the bundle is unsigned per #82, so editing breaks no
seal), and that the license sheet still appears on open.

Decision rule: run the measurement; if Route A's residual is only F4
(± F6), prefer Route A + H2(b) documentation over Route B's added
surface, per the issue's own falsification guidance ("do not force a
brittle header rewrite that risks a corrupt image" generalizes to not
forcing brittleness anywhere).

## 5. Validation checklist before any normalized dmg ships

Per issue #191 §10, on a clean macOS VM:

- `hdiutil verify` passes; the dmg attaches and mounts.
- The app launches from the mounted volume and from `/Applications`
  after drag-install.
- The `.jls` association survives (`CFBundleDocumentTypes` present;
  double-clicking a `.jls` file opens JLS).
- The license agreement sheet still appears on open (udifrez intact).
- The Finder window layout and background still render.
- The documented Gatekeeper right-click-Open flow (#82) is unchanged.

## 6. Dependencies and sequencing

1. #188's `SOURCE_DATE_EPOCH` + staged-tree clamp — **done**
   (`build-installer.sh` exports it from `project.build.outputTimestamp`
   and clamps `input/`, `runtime/`, and the app-image tree).
2. Route A normalization — **done** as `scripts/normalize-dmg.py`
   (`koly` on by default, `hfs` behind `JLS_DMG_FULL_NORMALIZE=1`),
   wired into `build-installer.sh`'s Darwin case only, with the tool's
   `--self-test` gating it before it touches a real dmg.
3. **In progress (on `macos-latest` CI):** the
   `macos-installer-reproducibility` leg now runs with
   `JLS_DMG_FULL_NORMALIZE=1` and validates the CI-runnable subset of
   the §5 checklist (`hdiutil verify`, attach, `JLS.app` + valid
   `Info.plist` + `.jls` document type). Still needs a human on a clean
   VM for the launch/Gatekeeper items, and `scripts/measure-dmg-repro.sh`
   for the full byte-range attribution.
4. Byte-identical → promote the macOS leg to a required gate; residual
   → this file and #185's `docs/reproducibility.md` state it exactly,
   with the provenance attestation as the integrity guarantee.
