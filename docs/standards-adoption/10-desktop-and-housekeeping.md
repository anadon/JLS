## Packaging and desktop-integration housekeeping: AppStream, XDG base directories, IEC 60027-2 (#165, #174, #175)

Three items that share nothing except size. Two are worth doing in a
reduced form; one should be declined and the decline recorded.

**Numbering warning.** `#165`, `#174`, `#175` here are
`docs/standards-landscape.md` registry rows (§11.1 and §11.3), *not*
GitHub issue numbers. JLS issue #165 is stable element identity (see
`docs/file-format.md` §"Stable ids" and `CHANGELOG.md`), which is a
different thing entirely. Any issue opened for this work must say
"landscape entry #165" explicitly or the confusion is guaranteed.

Headline recommendations:

| Item | Verdict |
|---|---|
| AppStream metainfo (#175) | Do it in the **reduced** form: one metainfo file, shipped in deb/rpm/AppImage, validated in CI. **Do not** pursue Flathub. |
| XDG Base Directory Spec (#174) | Do it, in two stages. Stage 1 (unify the resolver, fix two real spec defects, move `UserPrefs` off `java.util.prefs`) is unambiguously worth it. Stage 2 (platform-native paths on Windows/macOS) touches a normative doc and needs a recorded deviation. |
| IEC 60027-2 binary prefixes (#165) | **No.** JLS's memory UI is not denominated in bytes at all, and the one place that does show a byte magnitude already says `MiB`. Record the decline; optionally add a cheap ratchet so it stays true. |

---

### What conformance actually means

#### (a) AppStream metainfo (#175)

The document is the **AppStream specification**, maintained by
freedesktop.org (upstream: `github.com/ximion/appstream`; spec text at
`https://www.freedesktop.org/software/appstream/docs/`). The spec
version is roughly 1.0 (*unverified* — read the version banner on the
spec page before quoting it in a doc). There are no levels or profiles.
The relevant clauses are:

- **§ "Metainfo Files" → component type `desktop-application`.** Required
  tags: `<id>`, `<name>`, `<summary>`, `<metadata_license>`,
  `<project_license>`, `<description>`, and — for `desktop-application` —
  a `<launchable type="desktop-id">` naming the installed `.desktop`
  file. Recommended: `<url type="homepage">`, `<screenshots>`,
  `<releases>`, `<content_rating>`, `<developer>`, `<provides>`.
- **File location clause.** The file MUST be installed as
  `/usr/share/metainfo/<component-id>.metainfo.xml`. `/usr/share/appdata`
  and the `.appdata.xml` suffix are the deprecated legacy spellings.
- **Component ID clause.** For a `desktop-application` the ID SHOULD be
  reverse-DNS. For JLS the correct ID is `io.github.anadon.JLS` — it
  matches `--mac-package-identifier io.github.anadon.jls` already used in
  `scripts/build-installer.sh:410` and the `io.github.anadon:jls` Maven
  coordinates in `pom.xml`.
- **`metadata_license` clause.** The *metadata* license must be a
  permissive one (`CC0-1.0`, `FSFAP`, `MIT`, `CC-BY-SA-4.0`, …).
  `GPL-3.0-only` is rejected/warned by the validator for this tag even
  though it is correct for `project_license`. (*Behavior of
  `appstreamcli validate`; confirm on the runner rather than trusting
  this line.*)

**What is claimed:** "JLS ships a valid AppStream metainfo file in its
deb, rpm and AppImage." **What is not claimed:** that JLS is listed in
any software centre, that it is in any distro archive, or that it is on
Flathub. Those are downstream consequences of *other people's* indexing,
not of conformance.

**The artifact a claim rests on:** `appstreamcli validate --pedantic`
exiting 0 against the *generated* file, captured as a CI step in
`.github/workflows/ci.yml`. Not the checked-in template — see
"Implementation procedure", the shipped file is templated.

**What it actually unlocks, honestly.** Very little on its own. GNOME
Software and KDE Discover render metainfo out of the *distribution's*
AppStream catalog, which distros build from packages in their archives —
a `.deb` a student downloads and `apt install ./`s is not in an archive
and will generally not appear in a software centre regardless. JLS's deb
installs into `/opt/jls` (see `resources/packaging/resource-dir-linux/postinst`),
which is fine for a third-party package but is not the layout Debian or
Fedora archives accept, so archive inclusion is not on the table either.
The one real, immediate consumer is the **AppImage** path: AppImageHub /
`appimage.github.io` and AppImage-aware desktop integration daemons read
`usr/share/metainfo/*.xml` from inside the AppDir. The second is
**future-proofing**: if the project is ever packaged by a third party
(nixpkgs, an Ubuntu PPA, a distro maintainer), the metainfo already
exists and is correct.

**Flathub: recommend no.** Flathub is a genuine consumer of AppStream
and would give real discoverability, but it is a *different deployment
model*, not a metadata file:

- It needs a Flatpak manifest in a separate `flathub/io.github.anadon.JLS`
  repository, built on Flathub infrastructure against a Flatpak runtime —
  a second packaging pipeline alongside the jpackage one, with none of
  the reproducibility plumbing (`SOURCE_DATE_EPOCH`, the double-build
  gates in `.github/workflows/ci.yml`) carrying over.
- The sandbox is hostile to the tool's actual job. JLS is a file editor
  whose recovery story writes `<circuit>.jls~` next to the user's file
  (`src/jls/edit/Editor.java:103`, `src/jls/edit/SimpleEditor.java:5388`)
  and whose batch mode is used from shells and autograders. Under
  Flatpak that means `--filesystem=home` (which reviewers push back on)
  and a `flatpak run` invocation prefix that breaks every command line in
  `README.md` and `docs/batch-interface.md`.
- It contradicts the recorded deployment model: single self-contained
  jar plus per-OS installers, no install step assumed, no network.
- Screenshots must be HTTPS-reachable at Flathub build time (below).

Record the decline the way `ARCHITECTURE.md` § "Recorded decisions"
records the others.

**Screenshots and the hosting problem.** `<screenshots>` are
*recommended*, not required; `appstreamcli validate --pedantic` will emit
an info/warning for their absence but the file is still valid. Screenshot
`<image>` elements are URLs — the spec has no mechanism for an embedded
or file-relative image, so an offline-first project with no website has
exactly three options:

1. **Omit screenshots.** Valid; costs a pedantic-level hint. This is the
   recommendation while Flathub is declined.
2. **Reference `raw.githubusercontent.com` at a pinned tag**, with the
   PNGs committed under `resources/packaging/screenshots/`. Works, but
   bakes a GitHub URL into a shipped file and is only fetched by
   consumers that resolve URLs.
3. GitHub Pages. The project has no published site today
   (`ARCHITECTURE.md` § "Help delivery: in-jar now, hosted docs are the
   planned future"), so this presupposes work not yet done.

If option 2 is ever taken, the screenshots can be produced for free by
the existing GUI rigs — `scripts/wayland-rig.sh` and `scripts/x11-rig.sh`
already boot the editor and capture a screenshot as a CI artifact.

#### (b) XDG Base Directory Specification (#174)

The document is the **XDG Base Directory Specification**, freedesktop.org,
version 0.8 (*revision number unverified; check the spec header*). It is
a short spec with no levels. The clauses that bind:

- `$XDG_CONFIG_HOME` is the base for user-specific configuration;
  default `$HOME/.config` when unset **or empty**.
- `$XDG_DATA_HOME` (default `$HOME/.local/share`),
  `$XDG_STATE_HOME` (default `$HOME/.local/state`),
  `$XDG_CACHE_HOME` (default `$HOME/.cache`).
- **All paths in these variables MUST be absolute; a relative value MUST
  be ignored** (treated as unset). ← JLS violates this today.
- Directories created under a base SHOULD be created with mode `0700`.
- Applications SHOULD confine themselves to a single subdirectory named
  after the application.

**Where JLS's state actually lives today** (all verified in the tree):

| What | Code | Location today |
|---|---|---|
| Theme, grid/background colour overrides, undo depth | `src/jls/UserPrefs.java:68` — `Preferences.userNodeForPackage(JLSInfo.class)`, package `jls`, so node path `/jls` | **Not XDG.** Linux: `~/.java/.userPrefs/jls/prefs.xml` (OpenJDK `FileSystemPreferences`, root from the `java.util.prefs.userRoot` property defaulting to `user.home`). Windows: registry `HKCU\Software\JavaSoft\Prefs\jls`. macOS: `~/Library/Preferences/com.apple.java.util.prefs.plist` (OpenJDK `MacOSXPreferencesFactory`). *The Windows and macOS backing-store paths are from the OpenJDK implementation, not verified on this repo's runners — verify before publishing them in user docs.* |
| Per-install replica id (stable element ids, JLS issue #183) | `src/jls/elem/ElementId.java:71-79` | `$XDG_CONFIG_HOME/jls/replica-id`, else `~/.config/jls/replica-id` — **on every platform**, including Windows and macOS |
| Collaboration Ed25519 identity | `src/jls/collab/net/IdentityKey.java:93-100` | `$XDG_CONFIG_HOME/jls/collab-identity`, mode 600 |
| Verified peers | `src/jls/collab/net/KnownPeers.java:96-102` | `$XDG_CONFIG_HOME/jls/known-peers` |
| Crash-recovery checkpoints | `src/jls/edit/Editor.java:103`, `SimpleEditor.java:5388`, deleted at `JLSStart.java:2314` | `<circuit dir>/<name>.jls~` — **next to the user's file**, not in any base directory |

So the honest statement of the gap is *not* "JLS ignores XDG". It is:

1. Three of four config stores already use `$XDG_CONFIG_HOME`, with the
   same eight-line resolver **copy-pasted three times**.
2. That resolver has **two real conformance defects**: it accepts a
   relative `$XDG_CONFIG_HOME` (the spec says ignore it) and it does not
   create directories `0700`.
3. The fourth store — user preferences — sits outside XDG entirely,
   inside `java.util.prefs`.
4. The XDG resolver is applied on **Windows and macOS too**, where
   `~/.config/jls` is a foreign convention.

**What is claimed after the work:** "on POSIX platforms JLS reads and
writes user configuration only under `$XDG_CONFIG_HOME` (defaulting to
`~/.config`), in a single `jls/` subdirectory, honouring the spec's
absolute-path rule and creating directories mode 0700; on Windows and
macOS it uses the platform convention instead." **What is not claimed:**
that JLS uses `$XDG_DATA_HOME`, `$XDG_STATE_HOME`, or `$XDG_CACHE_HOME`
(it stores nothing that belongs there), or that `.jls~` checkpoints move
(they deliberately do not — see below).

**The artifact a claim rests on:** a new `test/jls/XdgBaseDirectoryTest.java`
that drives the resolver with a matrix of environment values and asserts
each spec clause, plus `test/jls/UserPrefsMigrationTest.java` for the
no-loss migration.

**Stability-contract collision — read this before touching anything.**
`docs/file-format.md:387` is a **normative spec** and it names the path
literally: the replica id resolves from "the install's config file
(`$XDG_CONFIG_HOME/jls/replica-id`, defaulting to `~/.config/jls/replica-id`)".
`SECURITY.md:113` likewise pins `jls/collab-identity` "under the XDG
config base with owner-only permissions (mode 600 on POSIX)". Moving
those files on Windows/macOS is a documented-contract change and needs
the deviation recorded, not just the code changed.

#### (c) IEC 60027-2 binary prefixes (#165)

The clause is **IEC 60027-2 Amendment 2 (1999)**, which introduced the
binary prefixes Ki/Mi/Gi (kibi, mebi, gibi) so that `K` could keep its
SI meaning of 1000. That content now lives in **IEC 80000-13:2008**
(*whether 60027-2 was formally withdrawn: unverified*). Conformance is a
purely editorial property of displayed strings: a quantity of 1024 bytes
is written `1 KiB`, never `1 KB`.

**JLS's actual byte-magnitude surface, enumerated:**

- `src/jls/FileAbstractor.java:152, 310, 328, 409` — the 64 MiB circuit
  size limit, printed in user-facing error text. **Already writes `MiB`**
  (`MAX_CIRCUIT_TEXT_BYTES = 64L << 20`, `FileAbstractor.java:65`), and
  `docs/file-format.md:70` says `64 MiB` to match.
- `src/jls/edit/MemoryDialog.java:214-218` — the Memory element dialog's
  only size fields are **`Bits/Word:`** and **`Capacity (words):`**.
- `resources/help/elements/memory/memory.html:34-46` — same vocabulary:
  words, bits per word, "at least two words".
- Two internal code comments (`src/jls/edit/Trace.java:30` "~14 MB",
  `src/jls/elem/Memory.java:1222` "32 MB of longs") — not user-visible.

**Therefore there is no conformance gap to close.** The Memory element is
not denominated in bytes; a JLS memory is *N* words of *B* bits, where
*B* is frequently not 8. Introducing "KiB" into a dialog that never said
"KB" would not be a correctness improvement, it would be the invention of
a unit the model does not have. And the one place a byte magnitude *is*
shown already conforms.

The teaching argument cuts the same way. A student who has just learned
that a 10-bit address bus addresses 1024 words is being taught the binary
exponent, and JLS displays that as `1024`, exactly. Nothing is rounded or
abbreviated, so there is nothing for a prefix to disambiguate.

**Recommendation: decline.** The deliverable is a recorded decision, not
code.

---

### Implementation procedure

#### (a) AppStream metainfo — 8 steps

1. **Create the template** at
   `resources/packaging/io.github.anadon.JLS.metainfo.xml.in` (new file).
   Sketch — every value cross-checked against `pom.xml` and the existing
   desktop entries:

   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <component type="desktop-application">
     <id>io.github.anadon.JLS</id>
     <metadata_license>CC0-1.0</metadata_license>
     <project_license>GPL-3.0-only</project_license>
     <name>JLS</name>
     <summary>Educational digital logic circuit editor and simulator</summary>
     <description>
       <p>...</p>
     </description>
     <launchable type="desktop-id">@DESKTOP_ID@</launchable>
     <provides><mediatype>application/x-jls-circuit</mediatype></provides>
     <url type="homepage">https://github.com/anadon/JLS</url>
     <url type="bugtracker">https://github.com/anadon/JLS/issues</url>
     <content_rating type="oars-1.1"/>
     <releases>
       <release version="@APP_VERSION@" date="@RELEASE_DATE@"/>
     </releases>
   </component>
   ```

   **Design decision — `project_license`.** Use `GPL-3.0-only`, matching
   `flake.nix:78`'s `licenses.gpl3Only`. No source file in `src/` carries an
   "or (at your option) any later version" clause (verified by grep), and
   `pom.xml:19-23` names plain "GNU General Public License v3.0". If the
   maintainer intends *or-later*, fix `flake.nix` and `pom.xml` in the
   same PR rather than letting three files disagree.

   > **Cross-section conflict — do not resolve this here.** Section 09
   > (CRA & supply chain) §Step 0 recommends the **opposite** identifier,
   > `GPL-3.0-or-later`, because `CONTRIBUTING.md:138` binds every
   > contributor to "GPLv3-**or-later**" — a live contribution term this
   > section's grep of `src/` does not see. Both sections propose editing
   > `pom.xml`'s `<licenses>` block, so they cannot both land. The
   > identifier is **one decision**, blocked on reading `pop_GPLv3.pdf`
   > (is the Poplawski grant v3-only or v3-or-later?); make it once,
   > record it in `ARCHITECTURE.md`, and apply it to `pom.xml`,
   > `flake.nix`, `README.md`, `CONTRIBUTING.md`, this
   > `<project_license>`, and `REUSE.toml` together. Whatever is chosen,
   > the metainfo template must follow it rather than lead it.

   **Design decision — the `<releases>` block.** Do **not** hand-maintain
   a full release history mirroring `CHANGELOG.md`; it will go stale on
   the first release someone forgets. Emit a single `<release>` for the
   version being built, substituted at package time.

2. **Substitute at build time in `scripts/build-installer.sh`.** Add a
   `render_metainfo()` next to the existing `package()` helper. It must
   derive `@RELEASE_DATE@` from `SOURCE_DATE_EPOCH`
   (`date -u -d "@${SOURCE_DATE_EPOCH}" +%F`), **never** from the wall
   clock. `SOURCE_DATE_EPOCH` is already computed and exported at
   `scripts/build-installer.sh:96-117`; the `date -u -d` / BSD `date -u -r`
   fallback pair at lines 121-122 is the pattern to copy. Using `date +%F`
   would make two builds of one commit differ on a day boundary and turn
   the **required** `installer-reproducibility` gate
   (`.github/workflows/ci.yml:855`) red — this is the single highest-risk
   line in the whole item.

3. **deb and rpm placement.** `jpackage` has no "install this file to
   `/usr/share/metainfo`" flag. Copy the rendered file into `$INPUT`
   before `jpackage` runs (it lands in the app image, i.e. `/opt/jls/lib/app/`),
   then install it from there in the maintainer scripts. In
   `resources/packaging/resource-dir-linux/postinst`, after the existing
   `xdg-mime install` line:

   ```sh
   install -Dm644 /opt/jls/lib/app/io.github.anadon.JLS.metainfo.xml \
       /usr/share/metainfo/io.github.anadon.JLS.metainfo.xml
   ```

   and the matching `rm -f` in `prerm`. Both files are already
   `--resource-dir` overrides (`scripts/build-installer.sh:361-367`), so
   this is two lines in files the project already owns.

   **Design decision — rpm.** The rpm gets its scriptlets from
   `jpackage`'s generated spec, and overriding it means adding
   `template.spec` to the resource dir — a large JDK-version-coupled file
   that would have to be re-diffed on every JDK bump, exactly the
   maintenance cost the existing `postinst` override header apologises
   for. **Recommendation: ship the metainfo inside the rpm's `/opt/jls`
   payload but do not add a `template.spec` override to relocate it.**
   Document that the deb and AppImage carry a spec-located metainfo file
   and the rpm carries it only under `/opt`. Revisit if and only if a
   Fedora/openSUSE packager asks.

4. **AppImage placement.** In `build_appimage()`
   (`scripts/build-installer.sh:205-267`), alongside the existing
   `cp resources/packaging/jls.png "$appdir/jls.png"`, add
   `install -Dm644 <rendered> "$appdir/usr/share/metainfo/io.github.anadon.JLS.metainfo.xml"`.
   This is the placement AppImage tooling looks for. It must happen
   **before** the `clamp_mtimes "$appdir"` call at line 242 or the
   reproducibility gate fails.

5. **Reconcile the two desktop-file names.** The deb installs
   `jls-JLS.desktop` (`postinst`, `prerm`), while `build_appimage` writes
   a top-level `JLS.desktop` (`scripts/build-installer.sh:227-237`) and
   `flake.nix:54-63` produces `jls.desktop`. AppStream's
   `<launchable type="desktop-id">` must name the file as installed.
   **Recommendation:** substitute `@DESKTOP_ID@` per format
   (`jls-JLS.desktop` for deb/rpm, `JLS.desktop` for the AppImage) rather
   than renaming shipped files — renaming the deb's entry would orphan
   the `.desktop` copy that `postinst`'s fallback branch installs on
   upgrade from an older package.

6. **Nix.** `flake.nix` is out of scope for step 1–5 but should get the
   file too, for symmetry: one `install -Dm644` line in `installPhase`
   next to the existing icon install (`flake.nix:71`), plus the matching
   `@DESKTOP_ID@` value `jls.desktop`. Optional; do it only if the
   rendering is factored so Nix can call it.

7. **Documentation.** Add a short paragraph to `README.md`'s "Installing
   JLS" section noting that the Linux packages carry AppStream metadata,
   and a recorded decision in `ARCHITECTURE.md` § "Recorded decisions"
   titled *"Flathub: not pursued (recorded <date>)"* with the four
   reasons from the section above and the reopening trigger ("a course or
   lab asks for a Flatpak, or the sandbox gains a workable
   document-portal story for `.jls~` siblings").

8. **Migration / compatibility.** None. This adds a file to three package
   payloads; no saved file, no API, no CLI surface changes. Adding
   `/usr/share/metainfo/...` in `postinst` means an *upgrade* from a
   pre-metainfo package installs it and `prerm` removes it — the standard
   deb lifecycle. **No stability contract is touched.** The only
   downstream byte-level effect is that installer checksums change (they
   are not reproducible across commits anyway; see `README.md`'s
   "Installing JLS" paragraph on installer non-reproducibility across
   rebuilds).

#### (b) XDG base directories — staged

**Stage 1 (recommended, no path changes on Linux).**

1. **Create `src/jls/util/ConfigDir.java`** (new; `src/jls/util/` exists).
   Keep it AWT-free and Swing-free — `src/jls/elem/ElementId.java` is a
   core-candidate class under `test/jls/HeadlessCoreRatchetTest.java`'s
   rules, and it will depend on this. Surface:

   ```java
   public static Path base();            // the platform config base
   public static Path file(String name); // base().resolve(name)
   static Path base(Map<String,String> env, String os, String home); // testable
   ```

   Semantics, one per spec clause: `$XDG_CONFIG_HOME` wins **only if
   non-null, non-empty, and `Path.isAbsolute()`**; otherwise
   `$HOME/.config`; the `jls` subdirectory is appended by `file`; and a
   `mkdirs` helper creates missing directories with POSIX permissions
   `rwx------` where the filesystem supports them, falling back silently
   where it does not (Windows).

2. **Repoint the three existing call sites** at it —
   `ElementId.defaultReplicaFile()` (`src/jls/elem/ElementId.java:71`),
   `IdentityKey.defaultFile()` (`src/jls/collab/net/IdentityKey.java:93`),
   `KnownPeers.defaultFile()` (`src/jls/collab/net/KnownPeers.java:96`).
   All three keep their existing observable paths on Linux. `IdentityKey`
   must keep writing mode 600 (`SECURITY.md:113`); the new 0700 directory
   creation is strictly additional hardening.

3. **Move `UserPrefs` off `java.util.prefs`.** Replace
   `Preferences.userNodeForPackage(JLSInfo.class)` (`src/jls/UserPrefs.java:68`)
   with a `java.util.Properties` file at `ConfigDir.file("prefs")`.
   Keep the class shape exactly as it is — the private
   `get`/`put`/`remove` trio (`UserPrefs.java:168-220`) and the in-memory
   fallback map are already the right seam, and `test/jls/UserPrefsTest.java`
   constructs `new UserPrefs(node)` directly, so the package-visible
   constructor becomes `UserPrefs(Path)` (or an interface) and the
   existing tests need a one-line change each.

   **Migration, and it must be lossless.** On `open()`: if the new
   properties file does not exist, read the legacy
   `Preferences.userNodeForPackage(JLSInfo.class)` node; if it holds any
   of the four keys (`theme`, `gridColor`, `backgroundColor`,
   `undoDepth`), copy them into the new file and write it. **Do not
   delete or clear the legacy node** — leaving it means a user who
   downgrades JLS gets their settings back, and the cost is four dead
   registry values / one dead XML file. Wrap the legacy read in the same
   `SecurityException | IllegalStateException | BackingStoreException`
   swallow the class already uses (`UserPrefs.java:70, 196, 216`) so a
   sandbox with no prefs backend still starts. Keep the legacy-read code
   for at least two minor releases, then delete it in a release whose
   `CHANGELOG.md` entry says so.

4. **Expected build side-effect, plan for it.**
   `scripts/build-installer.sh:145` derives the jlink module set with
   `jdeps --print-module-deps`; its comment at lines 143-144 claiming
   "no `java.util.prefs` usage" is **already stale** (`UserPrefs.java:6-7`
   imports it). Dropping `java.util.prefs` should remove `java.prefs`
   — and, expected but *unverified*, its `requires java.xml` — from the
   derived module set, shrinking the bundled runtime and changing every
   installer's bytes. That is fine (the double-build gate compares two
   builds of *one* commit), but re-run `jdeps --print-module-deps` and fix
   the stale comment in the same PR.

**Stage 2 (platform-native paths; separate PR, separate decision).**

5. Extend `ConfigDir.base()` with an OS switch: on `os.name` starting
   `Windows`, use `%APPDATA%\JLS` (falling back to
   `${user.home}\AppData\Roaming\JLS`); on `Mac OS X`, use
   `~/Library/Application Support/JLS`; everything else, XDG as in stage 1.
   `~/Library/Application Support` is the Apple convention for
   application-managed files (`~/Library/Preferences` is reserved for
   `NSUserDefaults` plists and should not be hand-written into).

6. **Read-through fallback, forever.** `ConfigDir.file(name)` returns the
   native path if it exists, else the legacy `~/.config/jls/<name>` path
   if *that* exists, else the native path (for creation). This is
   strictly better than a one-shot copy-and-delete migration: no
   migration step to get wrong, no window where a crash loses a key, and
   an existing Windows install's `replica-id` keeps winning so
   from-scratch saves do not silently change their `sid` replica half.
   Losing a `collab-identity` file is worse than losing a preference —
   peers' `known-peers` entries would no longer match the fingerprint —
   so the fallback is load-bearing, not a nicety.

7. **Contract deviation, documented.** Edit `docs/file-format.md:384-390`
   to replace the bare `$XDG_CONFIG_HOME/jls/replica-id` sentence with a
   per-platform table plus the legacy-fallback rule, in RFC 2119 language
   with the issue number cited, per the repo's docs style. Same for
   `SECURITY.md:113`. Note in both that the `jls.replicaId` /
   `JLS_REPLICA_ID` override — the knob CI and reproducible export use —
   is **unchanged**, so nothing in `docs/batch-interface.md` or
   `docs/reproducibility.md` is affected.

**Explicitly out of scope: `.jls~` checkpoints.** They stay beside the
user's circuit file. They are document-scoped recovery artifacts (the
`vim` `file~` model), not application state; the recovery UX in
`src/jls/DefaultExceptionHandler.java:160` tells the user to look for
`<file>.jls~`, `JLSStart.java:2242` puts them in the file-chooser filter,
and `JLSStart.java:2314` deletes them from the circuit's own directory.
Moving them to `$XDG_STATE_HOME` would break all three and would break
recovery entirely for a circuit on a USB stick moved between machines.
The XDG spec does not ask for it — it governs *application* files, not
user documents. Say this in the issue so nobody re-litigates it.

#### (c) IEC 60027-2 — the decline

1. Add a recorded decision to `ARCHITECTURE.md` § "Recorded decisions":
   *"Binary prefixes (IEC 60027-2 / IEC 80000-13): no UI change (recorded
   <date>, landscape #165)"*, carrying the enumeration above — Memory is
   words × bits/word, `FileAbstractor` already emits `MiB` — and the
   reopening trigger: *"JLS gains a UI that abbreviates a byte count
   (e.g. a file-size column, an export size estimate). At that point it
   MUST use KiB/MiB, matching `FileAbstractor`."*
2. Optionally (half a day) add `test/jls/UnitPrefixPolicyTest.java` in
   the established policy/ratchet style (`ToolkitPolicyTest`,
   `LookAndFeelPolicyTest`, `MenuAcceleratorPolicy`,
   `PointerApiRatchetTest`): scan `src/**/*.java` string literals for
   `\b\d+\s?(KB|MB|GB)\b` and fail, with an allowlist of the two code
   comments in `Trace.java:30` and `Memory.java:1222`. That converts the
   decision from a doc into something `mvn verify` keeps true.
3. No code change, no migration, no contract touched.

---

### Testing procedure

Everything below is **to be created** unless a path is cited as existing.

#### (a) AppStream

- **CI validation lane — the primary evidence.** Add a step to the
  existing `installer-reproducibility` job in `.github/workflows/ci.yml`
  (it already runs on ubuntu-latest, already installs packaging tools at
  the "Install packaging tools" step, and already builds the installers
  twice), or to a small new `appstream` job. Steps:

  ```yaml
  - name: Install AppStream tooling
    run: sudo apt-get install -y appstream desktop-file-utils || \
         echo "appstream unavailable; validation will skip"

  - name: Validate AppStream metainfo
    run: |
      f=target/installer/appimage/JLS/usr/share/metainfo/io.github.anadon.JLS.metainfo.xml
      if ! command -v appstreamcli >/dev/null; then
        echo "::notice::appstreamcli absent; skipping (#175)"; exit 0
      fi
      appstreamcli validate --pedantic --explain "$f"
  ```

  Skip-when-absent matches the house pattern for external tools (the
  `iverilog`/`ghdl`/`yosys` suites in `.github/workflows/ci.yml`'s
  "Install HDL toolchain" step, and `rpmbuild` in
  `scripts/build-installer.sh:373`). A network-suppressing flag
  (`--no-net`, *exact spelling unverified — check `appstreamcli validate --help`
  on the runner*) should be added if screenshots are ever introduced, so
  the lane cannot fail on someone else's outage.
- **`desktop-file-validate`** on the two `.desktop` files in the same
  step — free, since `desktop-file-utils` is already being installed, and
  it catches the `Categories=`/`MimeType=` typos that would silently
  break the association.
- **Golden-file test.** `test/jls/PackagingMetadataTest.java` (to be
  created), following the `test/resources/` golden-file house style
  (`test/resources/orientation-geometry.txt`, `test/resources/hdl/`).
  Render the template with fixed substitutions
  (`APP_VERSION=5.0.0`, `RELEASE_DATE=2000-01-01`,
  `DESKTOP_ID=jls-JLS.desktop`) and byte-compare against
  `test/resources/packaging/io.github.anadon.JLS.metainfo.golden.xml`.
  This makes the metainfo file reviewable in diffs and catches accidental
  edits. The same test should assert the `<id>` matches
  `--mac-package-identifier` in `scripts/build-installer.sh` and the
  `<mediatype>` matches `resources/packaging/jls-association-linux.properties`
  — three files that must agree and currently have nothing keeping them
  in sync.
- **Reproducibility regression.** Already covered: if `@RELEASE_DATE@` is
  ever taken from the wall clock, the existing required
  `installer-reproducibility` gate (`.github/workflows/ci.yml:855`) goes
  red on any build that straddles midnight UTC. Add one line to
  `docs/reproducibility.md` naming the metainfo date as a
  `SOURCE_DATE_EPOCH`-derived input so the next person knows why.
- **What turns it red:** an invalid tag or a missing required tag
  (validator, hard fail); a component-ID/desktop-ID mismatch after
  someone renames a `.desktop` file (golden test); a wall-clock date
  (repro gate).

#### (b) XDG / config paths

- **`test/jls/XdgBaseDirectoryTest.java`** (to be created) — the
  conformance artifact. One test per spec clause, all against the
  injectable `ConfigDir.base(env, os, home)` overload so nothing touches
  the developer's real `$HOME` (the hermetic-test discipline
  `test/jls/UserPrefsTest.java`'s class comment already states):
  - `xdgConfigHomeIsHonoured` — absolute value used verbatim.
  - `unsetFallsBackToDotConfig` — `~/.config/jls`.
  - `emptyIsTreatedAsUnset` — the case the current code does handle.
  - `relativeIsIgnored` — `XDG_CONFIG_HOME=relative/path` must fall back.
    **This one fails against today's code**, which is the point.
  - `createdDirectoriesAreOwnerOnly` — assert `PosixFilePermissions` are
    `rwx------` on a `@TempDir`, with
    `assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))`.
  - `windowsUsesAppData` / `macUsesApplicationSupport` (stage 2).
  - `legacyDotConfigPathStillWins` (stage 2) — the read-through fallback.
- **`test/jls/UserPrefsMigrationTest.java`** (to be created): seed an
  in-memory `AbstractPreferences` node (reuse the `MemoryPreferences`
  helper already inside `test/jls/UserPrefsTest.java`) with all four keys,
  point the new store at a `@TempDir`, open, and assert (i) every value is
  present in the new file, (ii) `applyStartup()` produces the same
  `Theme.active()` and `JLSInfo.Palette` values as the legacy path did,
  (iii) the legacy node is **untouched**, (iv) a second `open()` does not
  re-migrate over newer values, and (v) an unreadable/absent legacy
  backend degrades to defaults without throwing.
- **Consistency ratchet.** `test/jls/ArchitectureRulesTest.java` exists;
  add (or create `test/jls/ConfigPathPolicyTest.java`) a source scan
  asserting that `System.getenv("XDG_` appears in exactly one production
  file — `src/jls/util/ConfigDir.java`. That is what stops the
  copy-pasted resolver from coming back. Same shape as
  `test/jls/UserDirSeedTest.java`, which already pins "exactly one
  production read of `user.dir`".
- **Existing tests that must stay green:**
  `test/jls/elem/ElementIdReplicaTest.java` (the `resolveReplica`
  precedence chain), `test/jls/UserPrefsTest.java`,
  `test/jls/HeadlessCoreRatchetTest.java` (the new class must not import
  AWT/Swing), `test/jls/CollabSecurityRatchetTest.java`, and the
  `DeterministicSaveTest` / reproducibility suite — a botched replica-id
  migration shows up there first.
- **Property/fuzz opportunity, small but real:** a JUnit
  `@ParameterizedTest` over generated `XDG_CONFIG_HOME` values (empty,
  whitespace, relative, absolute, trailing slash, `~`-prefixed literal,
  NUL-ish, very long) asserting the result is always absolute and always
  under a `jls` directory. The repo already has fuzz precedent
  (`test/jls/GenerativeRoundTripFuzzTest.java`,
  `test/jls/ContainerMutationFuzzTest.java`).
- **What turns it red:** re-introducing a local `getenv("XDG_...")`
  (policy test); accepting a relative base (spec test); a migration that
  drops a key or clobbers a newer value (migration test); a
  platform-native path change that orphans an existing config file
  (fallback test).

#### (c) IEC 60027-2

- `test/jls/UnitPrefixPolicyTest.java` (to be created, optional) as
  described above. Red when someone adds a `"... MB"` string to a dialog.
- No golden files, no external tool, no CI change.

---

### Certification / conformance procedure

**All three are self-asserted. There is no certifying body, no registry,
no fee, no application, no expiry, and no audit for any of them.** State
that plainly in whatever doc records the outcome — the value of the
statement is entirely in the evidence behind it.

- **AppStream** is a freedesktop.org specification. freedesktop.org
  issues no certificates and maintains no conformance registry. The
  nearest thing to an external assessor is `appstreamcli validate`, the
  reference implementation's own linter — the same tool distro packaging
  CI runs. A credible self-assertion is therefore: *"the metainfo file
  shipped in release `vX.Y.Z` passes `appstreamcli validate --pedantic`
  (appstream version N) with exit 0; the check runs on every push in
  `.github/workflows/ci.yml`"* — with the workflow run linkable. Nothing
  more is available and nothing more is needed.
  - The **one** place an external body does assess anything is
    **Flathub**, and it is a *publication* review, not a certification:
    a PR to `github.com/flathub/flathub`, reviewed by Flathub volunteers,
    resulting in an app hosted on flathub.org. Cost: **free**. Elapsed
    time: *unverified — commonly reported as days to a few weeks
    depending on reviewer availability; do not put a number in a project
    doc without checking Flathub's current documentation.* Ongoing
    burden: a manifest repo to keep building against runtime updates,
    which is the real cost and the reason this playbook says no.
    Evidence package if ever pursued: the manifest, the metainfo file
    with OARS content rating and at least one reachable screenshot, and
    an app ID matching a domain or GitHub account under the maintainer's
    control (`io.github.anadon.*` qualifies).
  - AppImageHub (`appimage.github.io`) similarly accepts a PR listing;
    free, no review of substance, no validity period.
- **XDG Base Directory Specification** — freedesktop.org again; no body,
  no badge, no test suite published by the spec authors. A credible
  self-assertion is a named test class plus a per-platform table in the
  docs: *"`test/jls/XdgBaseDirectoryTest.java` asserts one clause of the
  spec per test method; `docs/file-format.md` and `SECURITY.md` state the
  resulting paths normatively"*. Anyone can verify it in ten minutes with
  `XDG_CONFIG_HOME=$(mktemp -d) java -jar jls.jar` and `find` — say so in
  the doc, because *reproducible by a stranger* is what makes a
  self-assertion worth anything.
- **IEC 60027-2 / IEC 80000-13** are paid ISO/IEC standards (the
  documents cost money to read; *current price unverified*). There is no
  conformance mark for using a unit symbol correctly, no body that
  assesses it, and no meaningful sense in which a program is "certified"
  for writing `MiB`. The self-assertion, if made at all, is a one-line
  statement plus the ratchet test.

Nothing here can be "invalidated" by a third party. What invalidates the
first two is the project's own drift: a metainfo file that stops
validating after a spec revision (the validator will say so on the next
CI run, because the lane runs on every push, not once at release), or a
new config write that bypasses `ConfigDir`. Both failure modes are
caught by tests, which is the only maintenance mechanism that survives a
single maintainer.

---

### Effort, risk, and failure modes

**Sizing** (maintainer-days, one experienced maintainer who knows this
tree):

| Work | Days | Reasoning |
|---|---|---|
| (a) metainfo template + `build-installer.sh` rendering + deb `postinst`/`prerm` + AppDir placement | 1.5 | Two small files, ~30 lines of shell, three insertion points already scaffolded by the existing `--resource-dir` overrides |
| (a) golden test + CI validation step + docs + recorded Flathub decline | 1 | Golden test is ~60 lines; the CI step is 10 lines in an existing job |
| (a) local verification (build deb + rpm + AppImage, install the deb in a container, `appstreamcli validate`) | 0.5 | `scripts/build-container.sh` and `.devcontainer/` give the substrate |
| (b) stage 1: `ConfigDir` + three call-site repoints + `UserPrefs` rewrite + migration | 2 | The `UserPrefs` `get`/`put`/`remove` seam makes the store swap mechanical; the migration is the fiddly part |
| (b) stage 1: `XdgBaseDirectoryTest`, `UserPrefsMigrationTest`, policy ratchet, adjust `UserPrefsTest` | 1.5 | ~12 test methods |
| (b) stage 2: platform-native paths + read-through fallback + `docs/file-format.md` / `SECURITY.md` deviation | 1.5 | Code is small; getting the normative text right and reviewing the `sid`-replica implications is most of it |
| (c) recorded decision (+ optional ratchet test) | 0.5 | |
| **Total** | **5–8** | Lower bound skips stage 2 and the optional ratchet |

**Top three ways this goes wrong**

1. **The metainfo release date is taken from the wall clock and breaks
   the required reproducibility gate.** `.github/workflows/ci.yml:855`
   ("Linux installer reproducibility") builds the installers twice and
   fails on any checksum difference. A `date +%F` inside
   `build-installer.sh` is invisible in review, passes locally, and turns
   `master` red at midnight UTC — intermittently, which is worse than
   consistently. Mitigation: derive from `SOURCE_DATE_EPOCH` (already
   exported at `scripts/build-installer.sh:116`), and put the reason in a
   comment next to the line, matching how the existing `clamp_mtimes`
   block explains itself.
2. **The `UserPrefs` migration silently loses settings, and nobody
   notices for a release.** The failure is quiet by construction: the
   class is designed to swallow every backing-store exception
   (`UserPrefs.java:70, 196, 216`) so a missing store looks exactly like
   a user who never set a theme. A migration bug therefore presents as
   "my colours reset", reported weeks later if at all. Mitigations: never
   delete the legacy node; test the migration explicitly rather than
   inferring it from the round-trip tests; and consider a one-line
   `stderr` note on first migration so the event is visible in a bug
   report.
3. **Stage 2 moves `~/.config/jls/collab-identity` on macOS/Windows
   without the read-through fallback, and collaboration peers stop
   recognising each other.** The identity keypair is the peer id
   (`SECURITY.md`: "the public-key fingerprint is the peer id"), and
   `KnownPeers` entries are keyed on it; regenerating it silently
   invalidates every prior verification, which is exactly the failure the
   SAS handshake exists to make visible. `IdentityKey.loadOrCreate` only
   generates when no file exists at the path it is given — so a path
   change *is* a regeneration. Mitigation: the fallback in step 6 is
   mandatory, not optional, and `test/jls/CollabSecurityRatchetTest.java`
   plus a dedicated fallback test must both cover it.

**Do NOT do this when:**

- **All of it**, if it displaces work on the ranked items in
  `docs/standards-landscape.md` §13.2 — the VPAT/ACR (item 1), the
  safety-critical scope statement (item 2), the OpenSSF badge (item 3).
  §13.2 lists *this housekeeping bundle* — SPDX SBOM, REUSE headers,
  AppStream, XDG — last, as **item 5 of 5**, for a reason. This is
  housekeeping, not a capability.
- **(a)**, if the answer to "who reads this file?" is nobody. If Flathub
  stays declined, no distro archive is targeted, and AppImageHub listing
  is not wanted, the metainfo file is a correct, validated, unread
  artifact. It is still cheap and still worth having as future-proofing
  — but do not let it be sold as unlocking software-centre listings,
  because for a hand-downloaded `.deb` installing into `/opt/jls` it does
  not.
- **(b) stage 2**, if the maintainer is not prepared to amend
  `docs/file-format.md` and `SECURITY.md` and record the deviation. A
  code change that contradicts a normative doc is worse than the
  inconsistency it fixes. Stage 1 has no such constraint and can land on
  its own.
- **(b) at all**, in a release where collaborative editing
  (`jls.collab.*`) is under active change. The identity/known-peers files
  are the riskiest thing being touched; do not move them and rewrite the
  code that reads them in the same release.
- **(c)**, always. Do not add "KiB" to the Memory dialog. The dialog says
  words and bits per word, and that is the correct vocabulary for the
  model.

---

### Sources

**Primary standards documents**

- AppStream specification, freedesktop.org —
  `https://www.freedesktop.org/software/appstream/docs/`. Clauses used:
  metainfo file location, `desktop-application` required tags,
  `<launchable>`, `metadata_license` restrictions, component-ID
  convention. *Spec version (~1.0) and the exact `metadata_license`
  rejection list are unverified in this pass; confirm against the page
  and against `appstreamcli validate` output.*
- XDG Base Directory Specification, freedesktop.org, version 0.8.
  Clauses used: `$XDG_CONFIG_HOME` and its default, the absolute-path
  requirement, the 0700 directory-creation recommendation, the
  single-subdirectory recommendation. *Revision number unverified.*
- Flathub documentation (`docs.flathub.org`) — app-ID rules, required
  metainfo content (OARS rating, screenshots), submission via PR to
  `flathub/flathub`. *Review turnaround time and current requirement list
  unverified; not fetched in this pass.*
- IEC 60027-2 Amendment 2 (1999), binary prefixes; superseded/absorbed by
  IEC 80000-13:2008. *Withdrawal status of 60027-2 and current purchase
  price unverified.*
- `docs/standards-landscape.md:434` (#165), `:453-454` (#174, #175) and
  `:767-769` (§13.2 item 5, which ranks these last among the cheap items).

**Repository paths verified by reading**

- `scripts/build-installer.sh` — `SOURCE_DATE_EPOCH` derivation (96-117),
  `clamp_mtimes` (129-131), `jdeps --print-module-deps` and its **stale**
  "no `java.util.prefs` usage" comment (143-145), `package()` (172-189),
  `build_appimage()` and the inline AppDir `.desktop` (205-267), Linux
  `--resource-dir` flag block (361-372), rpm leg and its
  `command -v rpmbuild` skip (373-398), macOS
  `--mac-package-identifier io.github.anadon.jls` (410).
- `resources/packaging/resource-dir-linux/{JLS.desktop,postinst,prerm}` —
  the existing overrides and the `xdg-mime` / `xdg-icon-resource`
  installs the metainfo install would sit beside.
- `resources/packaging/jls-association-linux.properties` —
  `application/x-jls-circuit`.
- `src/jls/UserPrefs.java` — `Preferences.userNodeForPackage(JLSInfo.class)`
  (68), the four keys (32-38), the exception-swallowing store seam
  (168-220).
- `src/jls/elem/ElementId.java:71-79`,
  `src/jls/collab/net/IdentityKey.java:93-100`,
  `src/jls/collab/net/KnownPeers.java:96-102` — the three copies of the
  XDG resolver.
- `src/jls/edit/Editor.java:103`, `src/jls/edit/SimpleEditor.java:5388`,
  `src/jls/JLSStart.java:2242, 2314`,
  `src/jls/DefaultExceptionHandler.java:160` — `.jls~` checkpoint
  placement and lifecycle.
- `src/jls/FileAbstractor.java:65, 152, 310, 328, 409` — the existing,
  already-conformant `MiB` usage.
- `src/jls/edit/MemoryDialog.java:214-218` and
  `resources/help/elements/memory/memory.html:34-46` — Memory is words ×
  bits/word, no byte units anywhere.
- `docs/file-format.md:70` (`64 MiB`), `:384-390` (the normative
  replica-id path sentence — the stability-contract collision).
- `SECURITY.md:105-118` — `jls/collab-identity` under the XDG config
  base, mode 600.
- `.github/workflows/ci.yml` — optional-tool install with
  skip-when-absent (61-62), the required `installer-reproducibility`
  double-build gate (846-898), the advisory aarch64 twin (899-940).
- `.github/workflows/release.yml:289-400` — the `installers` matrix.
- `.github/workflows/repro-installers.yml` — the report-only monthly probe.
- `flake.nix:53-78` — `makeDesktopItem` (`jls.desktop`), icon install,
  `licenses.gpl3Only`.
- `pom.xml:19-23` — declared license.
- `test/jls/UserPrefsTest.java` (the `MemoryPreferences` in-memory node),
  `test/jls/elem/ElementIdReplicaTest.java`,
  `test/jls/HeadlessCoreRatchetTest.java`,
  `test/jls/UserDirSeedTest.java` (the "exactly one production read"
  ratchet shape to copy), `test/jls/ArchitectureRulesTest.java`,
  `test/resources/orientation-geometry.txt` (golden-file house style).
- `ARCHITECTURE.md:204-232` (test layout), `:233-360` (recorded-decision
  format), `CONTRIBUTING.md` (gating).

**Unverified / needs a maintainer to check**

- `java.util.prefs` backing-store paths on Windows
  (`HKCU\Software\JavaSoft\Prefs\jls`) and macOS
  (`~/Library/Preferences/com.apple.java.util.prefs.plist`): taken from
  the OpenJDK implementation, not observed on this project's runners. The
  `windows` and `macos` jobs in `.github/workflows/ci.yml` could confirm
  both in one throwaway step before the migration is written.
- Whether `java.prefs` (and transitively `java.xml`) currently appears in
  `jdeps --print-module-deps` output for the shaded jar, and therefore
  how much the jlink image shrinks when `java.util.prefs` is dropped.
- The exact `appstreamcli validate` flag for suppressing network access.
- Whether `appstreamcli` rejects or merely warns on a copyleft
  `metadata_license`.
