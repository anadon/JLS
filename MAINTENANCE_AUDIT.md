# JLS Maintenance Audit — July 2026

This document records the findings of a full maintenance audit of the JLS
(Java Logic Simulator) repository. The actionable items are tracked on GitHub:
see the roadmap in issue #16, which links every item below as a sub-issue.

## Headline findings

- **The code still compiles.** All 80 `.java` files under `src/` plus the
  vendored XZ sources compile on OpenJDK 21 with zero errors. There are 36
  lint warnings (deprecation/removal/unchecked) — see "Modernization" below.
- **There is no infrastructure.** No build system beyond Eclipse project
  metadata, no CI, no tests, no git tags, no GitHub Releases, no distributable
  artifact. This — not code rot — is the main obstacle to maintaining and
  shipping JLS.
- **The known `.jls` (zip) loading bug has an identifiable root cause**
  (issue #2): `JLSStart.getZipScanner` closes the `ZipFile` before the
  `Scanner` reading from its entry stream has been consumed. `ZipFile.close()`
  closes all streams obtained from it, so only the Scanner's already-buffered
  data survives. Small files load fine (fully buffered), larger files
  truncate — exactly matching the intermittent "text ends early" symptom
  described in the last commit message (ed14ecd).

## State of the repository

| Aspect | State |
| --- | --- |
| Last substantive commit | 2015–2017 era (20 commits total) |
| Origin | Fork of JLS 4.1 by David A. Poplawski, Michigan Technological University |
| Version identity | Hardcoded in `JLSInfo.java`: 4.1.5, "built on March 18, 2014" |
| Build | Eclipse `.classpath`/`.project` only, targeting JavaSE-1.7 |
| Dependencies | `lib/jhall.jar` (JavaHelp 2.0, abandoned upstream ~2007), vendored XZ library sources in `xz/` |
| Tests | None |
| CI | None |
| Releases/tags | None |
| License | GPLv3 (`LICENSE`), with `pop_GPLv3.pdf` documenting the original author's grant |
| README | One line |

## Work items (tracked on GitHub)

### Phase 1 — Foundation
- **#6 Build system**: adopt Maven/Gradle; unvendor XZ (`org.tukaani:xz`);
  consume JavaHelp from Maven Central; target a modern LTS; derive version
  constants from the build.
- **#12 Repo hygiene**: 31 `CVS/` metadata directories are tracked in git;
  `.gitignore` has duplicate entries; Eclipse metadata needs a decision.
- **#7 CI**: GitHub Actions build on JDK 17/21 with lint, artifact upload.
- **#14 Tests**: JUnit 5; save/load round-trips for all three file formats
  (`.jls` zip, `.jls_txt`, `.jls_xz`); headless batch-mode simulation tests;
  a corpus of known-good circuit files for backward compatibility.

### Phase 2 — Correctness
- **#2 Zip loading bug**: root cause above; fix by not closing the `ZipFile`
  until the data is fully read, and stop swallowing errors via
  `catch (Throwable) { return null; }` in the format-sniffing chain.
- **#15 Consolidate file I/O**: three inconsistent implementations exist
  (`JLSStart` scanner helpers, the older zip-sniffing path in the image-export
  branch, and the unused `jls/fileAbstractor.java` draft, which has its own
  zip bugs — no `getNextEntry()`/`putNextEntry()`). Unify behind one
  `FileAbstractor` with real error propagation.

### Phase 3 — Modernization
- **#9 Remove applet support**: `JLSApplet` extends `JApplet`, which is
  deprecated for removal and being dropped from the JDK; browsers removed
  applet support years ago. Remove the class and the `JLSInfo.isApplet` paths.
- **#10 Deprecated APIs**: 28× `InputEvent.CTRL_MASK` → `CTRL_DOWN_MASK` /
  `getMenuShortcutKeyMaskEx()` (fixes macOS shortcut convention too);
  `new URL(String)` → `URI.create(...).toURL()`.
- **#11 JavaHelp**: decide between consuming `javax.help:javahelp:2.0.05`
  from Maven Central or replacing the help viewer with browser-launched HTML;
  either way remove the checked-in `lib/jhall.jar`, the stale absolute path
  in `.project`, and the binary `JavaHelpSearch` index files.

### Phase 4 — Ship it
- **#8 Release infrastructure**: version scheme decision, shaded runnable
  jar (`Main-Class: jls.JLS`), tag-triggered release workflow, optional
  `jpackage` installers.
- **#13 README**: what JLS is, screenshot, run/build instructions, provenance
  and licensing (explain `pop_GPLv3.pdf`), file-format notes, contribution
  pointers.

### Later / performance
- **#3 Faster collision checking** (pre-existing issue): skeleton started in
  commit f3e3b1c; a uniform grid keyed on the 12px snap spacing or a quadtree
  over element bounding boxes would replace the current linear scans. Do after
  tests exist.

## Items investigated and considered fine for now

- **Security**: no network-facing code beyond loading local files; the XZ
  vendored code is the pure-Java org.tukaani implementation (unrelated to the
  2024 xz-utils backdoor, which affected the C library's build scripts).
  Moving to the Maven artifact still improves supply-chain hygiene.
- **`pop_GPLv3.pdf` in the repo root**: unusual but load-bearing — it appears
  to document the original author's GPLv3 relicensing grant. Keep (perhaps
  under `docs/`) and explain in the README rather than delete.
- **Swing as the UI toolkit**: still fully supported in modern JDKs; no
  migration needed.
