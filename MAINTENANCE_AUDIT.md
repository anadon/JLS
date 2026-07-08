# JLS Maintenance Audit — July 2026

> **Historical snapshot.** This document describes the repository as it was
> *before* the modernization program it proposed. That program is complete —
> all 25 roadmap issues (#2–#27) are closed, and statements below such as
> "no build system", "no tests", and "no CI" no longer describe the tree.
> For the current state of the project, see the follow-up audit in
> [AUDIT-2026-07.md](AUDIT-2026-07.md). This file is kept unedited below as
> the record of the program's starting point and rationale.

This document records the findings of a full maintenance audit of the JLS
(Java Logic Simulator) repository, plus a follow-up performance/memory/file-size
audit. The actionable items are tracked on GitHub: see the roadmap in issue
#16, which links every item below as a sub-issue.

> **Status update (this branch):** a Maven build (`mvn verify`) now exists with
> lint (`-Xlint` + `-Werror`) and SpotBugs gates plus a GitHub Actions workflow
> (progress on #6/#7/#10). The lint-and-correctness pass fixed 19 findings,
> including a guaranteed `ClassCastException` in the JumpEnd name dialog, a
> latent NPE in `StateMachine`, and platform-default-encoding file I/O at 15
> sites. Details in commit 1f67a7b.

> **Issue format:** every GitHub issue in this program is structured as a
> compact scientific paper — Abstract, Background, Observations, Research
> Question, falsifiable Hypothesis, Predictions, Materials, Method/
> Experimental Design (task checklists live here), Data Collection &
> Analysis, Falsification Criteria, Threats to Validity, Related Work,
> Conclusion — following the canonical scientific-method sequence
> (observation → question → hypothesis → prediction → controlled test →
> analysis → replication/review). Roadmap #16 is the umbrella research
> program tying the studies together.

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

### Phase 3 — Performance & efficiency

Prompted by the report that selections with many elements are unworkably slow.
All findings verified against the source; line references are to the current
`master`.

- **#17 Editor interaction scales badly**. Every mouse event does
  full-circuit work:
  - Hover (`SimpleEditor.mouseMoved`, line 2140): loops all elements and
    triggers a full-canvas `repaint()` on every pixel of motion.
  - Rubber-band selection (`mouseDragged`, line 2066): O(n) scan + full
    repaint per event.
  - Moving a selection (`overlap()`, line 2794): nested loop over
    selected × all elements per drag event — **O(S×N) per event** — with
    `untouchAll()` (line 3198) full scans on failure paths. This is the
    reported "large selection" case. A commented-out optimization draft
    already sits inside `overlap()` (relates to pre-existing issue #3).
  - Repaint (`Circuit.draw`, line 640): iterates the element list four
    times and draws everything; no clip-rect, dirty regions, or cached
    static layer. Every wire segment and wire end is a separate Element,
    so N is much larger than the visible component count.
  - `Element.getRect()` allocates a new `Rectangle` per hit test → GC churn.
  - Remedies: uniform-grid spatial index on the 12px snap spacing,
    selection bounding-box broad phase, `repaint(Rectangle)` dirty regions,
    offscreen static layer, hover short-circuit.
- **#18 Undo deep-copies the whole circuit on every change**
  (`markChanged` → `pushCopy` → `Util.copy`, SimpleEditor lines 3745/3793):
  O(circuit) time per edit on the EDT and up to 11 live full copies in
  memory. Remedy: command-pattern undo (deltas), or compact serialized
  snapshots as an interim.
- **#19 Checkpoints are written synchronously on the Swing event thread**
  (every 10th change in `markChanged`): visible UI freezes on large
  circuits; also non-atomic (truncated `.jls~` on crash) and zip-format
  while saves are XZ. Remedy: background write, temp-file + rename, unify
  format with #15.
- **#3 Collision checking** (pre-existing): subsumed by the spatial index
  in #17.
- **#20 Memory efficiency**: the `Memory` element stores words as
  `HashMap<Integer, BitSet>` (~100 bytes per word vs 8 in a `long[]`), and
  its `activity` write history grows unbounded during a simulation with two
  `BitSet` clones per write. Remedy: dense `long[]` for `bits <= 64` with
  sparse fallback; bounded history; audit simulator-wide `BitSet` cloning.
- **#21 Saved file size**: saves already write XZ (default preset 6) into
  files named `.jls`, while checkpoints use zip; the text format is
  dominated by per-wire-end `ELEMENT` blocks and saves recomputable
  attributes; `Memory` initial contents are escaped decimal text. Remedy:
  measure real circuits first, then preset 9, attribute pruning, hex+RLE
  memory encoding — keeping the loader backward compatible (#14 guards).

### Phase 4 — Code health: de-duplication, simplification, generalization

Findings from a dedicated duplication/simplification pass; counts verified
against the source.

- **#22 Gate subclasses**: `And/Or/Nand/Nor/Xor/Not/DelayGate` (~1,550 lines
  over the 842-line `Gate` base) each duplicate the `previous*` settings
  statics, `setup()`, `copy()`, and save plumbing; only the outline path and
  the boolean reduction differ. Make `Gate` data-driven (name + shape +
  reduction + inversion flag); this also removes 15 of the 25 baselined
  SpotBugs static-write findings.
- **#23 Element persistence**: loading is already generic
  (`Circuit.java:350–441` typed-attribute parser + reflective instantiation)
  but every element hand-writes `save()` and `copy()`, duplicating attribute
  names as string literals in both directions. Declare attributes once per
  element; derive save/load/copy from the declaration.
- **#24 Orientation geometry**: rotation-aware elements enumerate coordinates
  per orientation in `if/else` ladders (orientation-conditional references:
  `TriState` 92, `Mux` 84, `Decoder` 49, `Register` 48, `Gate` 46,
  `Constant` 46, `Adder` 45, `Clock` 39). Define canonical geometry once and
  rotate on the 12px grid; verify with the existing headless image export as
  a visual-regression tool.
- **#25 Simulator merge**: `BatchSimulator.runSim` and `InterractiveSimulator`
  duplicate the init + event-loop skeleton (and the interactive class
  re-implements `Simulator.post()`); template-method the loop in the
  `Simulator` base with pacing/UI hooks. Fix the `InterractiveSimulator`
  typo while touching it.
- **#26 Dialog framework**: 29 hand-rolled `extends JDialog` inner classes
  across 25 files, with 73 `ok`/`cancel`/`help` button declarations in
  `jls/elem` alone, each re-wiring the same skeleton and drifting in
  Enter/Escape behavior. Extract a shared `ElementDialog` base.
- **#27 Grab-bag**: dead `Circuit.load_JLS2` path referencing the nonexistent
  `jls.elem2` package (would throw `ClassNotFoundException` if called);
  `Circuit.getElements()` leaking its live mutable set; 88 `instanceof`
  checks in `SimpleEditor` (plus the 4-pass `instanceof Wire` draw) to fold
  into polymorphism or split collections; parallel `InputPin`/`OutputPin` and
  `JumpStart`/`JumpEnd` structure to hoist into common bases.

### Phase 5 — Modernization
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

### Phase 6 — Ship it
- **#8 Release infrastructure**: version scheme decision, shaded runnable
  jar (`Main-Class: jls.JLS`), tag-triggered release workflow, optional
  `jpackage` installers.
- **#13 README**: what JLS is, screenshot, run/build instructions, provenance
  and licensing (explain `pop_GPLv3.pdf`), file-format notes, contribution
  pointers.

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
