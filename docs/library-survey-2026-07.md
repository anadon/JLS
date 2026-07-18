# Library Survey: What JLS Could Adopt, and What It Should Not

*July 2026. A survey of third-party libraries whose functionality overlaps
with or is adjacent to code in this repository, with a recommendation for
each. Library versions and maintenance states were checked against upstream
release pages in July 2026; they are moving targets. HDL-related
infrastructure (parsers, layout engines, external simulators) was already
surveyed in depth by [`hdl-support-research.md`](hdl-support-research.md)
§3/§7 — those verdicts are summarized here, not re-litigated.*

*Revised 2026-07-17: an explicit active-maintenance policy was adopted
(ground rule 4), which demoted jqwik and Cacio-tta from the recommended
list to the rejects table. Per-library investigation issues are filed:
FlatLaf #153, JFreeSVG #154, ArchUnit #155, picocli #156, Error Prone
#157.*

## Ground rules any candidate must pass

1. **License: GPLv3-compatible for runtime code.** JLS is GPLv3 and ships
   one shaded jar, so every runtime dependency is *distributed inside the
   work*. Apache-2.0, MIT, BSD, LGPL, and GPLv3 itself are fine; plain
   EPL-2.0 (no GPL secondary-license designation) is not — that is what
   already ruled out linking ELK (`hdl-support-research.md` §3).
   **Test- and build-scope dependencies are exempt** — they are not
   distributed; JUnit (EPL-2.0) is the standing precedent in `pom.xml`.
2. **The self-contained jar is the product.** Students run
   `java -jar jls.jar` on lab machines. Every runtime dependency is paid
   for in jar size and in SBOM/supply-chain surface (the CycloneDX BOM and
   Dependabot cover whatever we add). Small and few beats featureful.
3. **Contracts are pinned by tests.** The CLI table
   (`CliFlagTableTest`), the save format (`FormatHeaderTest`, round-trip
   suites), the load-error taxonomy, and batch stdout are compatibility
   contracts. A library that would change observable behavior must
   reproduce it exactly or is disqualified.
4. **Actively maintained projects only** *(policy, adopted 2026-07-17)*.
   JDK 25 baseline, single-maintainer project: a dependency that is
   itself abandoned is a liability (the fork already removed one of
   those — JavaHelp/`jhall.jar`, dead upstream since ~2007, replaced by
   the in-jar `jls.Help` viewer). Candidates must show ongoing releases
   and responsive upstream activity; projects in declared "maintenance
   mode" or with dormant release histories are rejected regardless of
   technical fit. This applies to test/build scope too — a test harness
   dependency that cannot follow the JDK baseline blocks upgrades just
   as effectively as a runtime one.

## Recommended — runtime dependencies

### 1. FlatLaf — modern Swing look-and-feel *(highest value)*

- **What:** [FlatLaf](https://github.com/JFormDesigner/FlatLaf)
  (JFormDesigner), Apache-2.0. Actively maintained — 3.7.2 released
  2026-07-09. One jar, no transitive dependencies, works on the JDK 25
  baseline.
- **Overlap:** JLS currently forces the cross-platform Metal
  look-and-feel (`JLSStart.java`, `UIManager.setLookAndFeel(...
  getCrossPlatformLookAndFeelClassName())`) and exits if that fails.
- **Improvement:** directly retires several recorded ergonomics findings
  at once. Ergonomics audit **U4** ("platform integration deliberately
  discarded… a 1998 visual identity", no dark mode, hardcoded cyan/yellow
  status strips) and the look-and-feel half of issue **#76** are exactly
  FlatLaf's feature set: light/dark themes with runtime switching, HiDPI
  scaling including fractional scales on Linux/Windows, native-feeling
  file choosers, and consistent rendering across the three OSes the
  installers target. Adoption is a one-line swap at the `setLookAndFeel`
  call plus a settings toggle; Metal can remain a fallback flag. The
  in-jar help viewer (`JEditorPane`) and every element dialog inherit the
  theme for free.
- **Companion (optional):** `flatlaf-extras` provides `FlatSVGIcon`,
  which is the clean fix for the *other* half of #76 — the 32×32 GIF
  toolbar icons in `src/jls/edit/images/` that upscale blurry on HiDPI.
  Redrawing ~30 icons as SVG is the real cost; the library part is
  trivial. This piece can trail the base adoption.
- **Cost:** ~1 MB in the shaded jar; a visual-regression pass over the
  dialogs (the `test/jls/ui` layer-1 assertions are layout-independent
  and should survive).
- **Verdict (2026-07-18, #153): adopt — recommended, gated on cross-OS
  visual QA.** The evaluation
  ([`flatlaf-evaluation-2026-07.md`](flatlaf-evaluation-2026-07.md))
  verified 3.7.2 on the JDK 25 baseline (installs headlessly, runtime
  light/dark switching works), measured the real shaded-jar delta
  (1,013,610 bytes, ×1.86), confirmed zero transitive dependencies and
  zero test-suite impact, audited the hardcoded colors (none block a
  light-theme default; they gate only a future dark default), and
  re-verified the comparables (Darklaf stale since 2025-09; Radiance
  active but invasive). A dependency-free `-Djls.laf` selection seam
  with Metal fallback shipped in `JLSStart` so the remaining
  screenshot-matrix QA (Linux/Windows/macOS at fractional scales — the
  one part a headless container cannot do) needs no rebuild. The
  default switch itself, the `pom.xml` dependency, and the
  ARCHITECTURE.md re-record happen together at the adoption commit.

### 2. JFreeSVG — vector image export *(ADOPTED, #154)*

- **Verdict (2026-07-17):** adopted at 5.0.7. The spike went exactly as
  predicted — `SVGGraphics2D` slotted into `Circuit.exportImage` with no
  per-element changes, and `-i out.svg` works end to end. One finding the
  survey missed: SVG serializes *draw order* into the output, and the
  element set is a `HashSet`, so a naive drop-in was byte-unstable across
  load instances (the render-path twin of the #166 save-order problem).
  The export now draws in a stable geometric order and `SvgExportTest`
  pins byte-identical output across fresh loads. Text stays as SVG
  `<text>` elements (smaller, selectable, and the font-metrics caveat is
  the same one that already rules out pixel goldens); no golden of the
  full document for that reason — determinism plus structural assertions
  instead.
- **What:** [JFreeSVG](https://github.com/jfree/jfreesvg) (jfree.org),
  **GPLv3** — the same license as JLS, so no compatibility question at
  all. Tiny (~50 KB), zero dependencies, requires Java 11+.
- **Overlap:** the `-i` image export (`JLSStart`, issue #71) renders the
  circuit's existing `Graphics2D` paint path into a `BufferedImage` and
  writes PNG/JPEG only.
- **Improvement:** `SVGGraphics2D` is a drop-in `Graphics2D`
  implementation, so the *same* element `draw` code that today fills a
  bitmap can emit resolution-independent SVG with no per-element work:
  `-i circuit.svg` just works. The audiences are real: instructors
  embedding circuits in slides/handouts, students in lab reports and
  theses, and the planned hosted documentation (ARCHITECTURE.md recorded
  decision) — all want vector figures, and the current bitmap export is
  the limiting factor. The existing `CliImageExportTest` pattern extends
  naturally (SVG is text — golden-testable more robustly than PNG).
- **Cost:** negligible; one new accepted extension in the `-i`
  validation, docs, and a golden test.

### 3. picocli — command-line parsing *(qualified recommendation)*

- **What:** [picocli](https://github.com/remkop/picocli), Apache-2.0,
  actively maintained (latest release June 2026). Deliberately
  single-artifact; can even be included as a single source file instead
  of a jar dependency.
- **Overlap:** `JLSStart.java` (~2,200 lines) hand-rolls the `FLAGS`
  table, flag parsing, `-h` output, and usage errors.
- **Improvement:** typed option binding, generated help that cannot
  drift from the accepted flags, negatable/repeatable options, and —
  the genuinely new capabilities — **shell tab-completion scripts** for
  the autograder/CI audience `docs/batch-interface.md` serves, and
  clean subcommand growth room (the CLI is accreting export modes:
  `-i`, `-vcd`, HDL export, text save).
- **Qualification:** the CLI is a *pinned contract* — one-line
  `jls: error: …` diagnostics on stderr, exit codes 0/1/2, exact flag
  table (`CliFlagTableTest`, `CliSmokeTest`, batch-interface §1).
  picocli can reproduce all of it (custom `IParameterExceptionHandler`,
  exit-code mapping), but the migration is a behavior-preservation
  exercise, not a rewrite win. Worth doing *when the flag table next
  grows* (e.g. the #59 HDL-import stages) rather than as standalone
  churn. The existing contract tests are exactly the safety net the
  migration needs — a rare case where the hard part is already done.

## Recommended — test/build scope only (not distributed)

### 4. ArchUnit — executable architecture rules *(ADOPTED, #155)*

- **Verdict (2026-07-17):** adopted at 1.4.2, core artifact only — the
  `archunit-junit5` integration pins a junit-platform line that would
  fight the JUnit 6 dependency, and plain `@Test` methods calling
  `ArchRule.check()` need no integration. JDK 25 bytecode imports fine.
  `ArchitectureRulesTest` lands three rules: the bytecode half of the
  #81 TellUser discipline (passes clean), jls.hdl reachable only via
  the JLSStart wiring point (passes clean), and jls.sim ↛ jls.edit with
  the three simulator classes pinned as a shrinking baseline (the #77
  debt, now machine-enforced instead of prose). Cost measured: ~2.5 s
  added to the test run, all in the one-time class-file import. The
  source-scan ratchets stay alongside: they see comments and would
  catch a use ArchUnit can't see (reflection), while ArchUnit catches
  fully-qualified references the text scan misses — complementary, not
  redundant. Future recorded decisions should get a rule here as their
  tripwire.
- **What:** [ArchUnit](https://github.com/TNG/ArchUnit), Apache-2.0,
  v1.4.2 (April 2026). Test-scope; runs as ordinary JUnit tests.
- **Overlap:** JLS already enforces architectural invariants with
  hand-rolled reflection/scanning tests: `NotificationRatchetTest` (no
  raw `JOptionPane` outside `TellUser`),
  `ElementConstructorContractTest` (the reflective `(Circuit)`
  constructor), the headless discipline ("batch mode never touches
  Swing").
- **Improvement:** those ratchets become declarative one-liners
  (`noClasses().that()… .should().callMethodWhere(JOptionPane…)`), and —
  more valuable — invariants that are currently *prose in
  ARCHITECTURE.md* become enforced: `jls.sim` must not depend on
  `jls.edit`; nothing outside `jls.hdl` touches emitters; no
  `java.awt`/`javax.swing` imports from batch-reachable classes; the
  `TellUser`-only dialog rule extended to `JDialog` subclasses. Each
  future recorded decision gets a cheap tripwire. This fits the
  project's demonstrated style (contract tests over convention) at
  lower maintenance cost than bespoke scanners.
- **Cost:** one test dependency; the existing bespoke ratchet tests can
  be ported incrementally or left alongside.

### 5. Error Prone — compile-time bug pattern checks *(TRIALED, #157: plumbing kept opt-in; no default-build gate)*

- **Verdict (2026-07-17):** the trial run happened (2.50.0 on the JDK 25
  build, default check set, full `src/` compile). Result: **zero
  ERROR-severity findings and zero true bugs** among 99 warnings —
  52 MissingOverride (style), 24 PatternMatchingInstanceof (useful, but
  exactly the #95 sealed-dispatch program's job), 8 InvalidParam
  (javadoc drift), 5 JdkObsolete (the Vector/LinkedList holdouts #94
  and #96 already track), and singletons that are benign or false
  positives (both ReferenceEquality hits are *intentional* identity
  comparisons of wire ends in `Element.touches`). Compile time 6 s →
  22 s. The survey's own bar was "keep only if the first report pays
  for the setup" — it did not: SpotBugs at threshold High had already
  taken the real bugs off the table. **Decision:** no `-Werror` gate
  and no baseline apparatus for EP core; but the compiler plumbing
  (the `errorprone` Maven profile: forked javac, `--add-exports`,
  processor path) stays in-tree as the ready-made substrate for #93 —
  NullAway is an Error Prone plugin and was the strategic reason to
  wire this at all. `mvn -Perrorprone clean compile` reproduces the
  report at any time.

- **What:** [Error Prone](https://github.com/google/error-prone)
  (Google), Apache-2.0, a javac plugin (build-time only; requires
  JDK 21+ to run — satisfied by the 25 baseline).
- **Overlap:** the build already runs SpotBugs (bytecode-level,
  threshold High) and `-Werror` on javac lints.
- **Improvement:** a complementary class of checks at the AST level
  (invalid format strings, misused equals, dead stores, suspicious
  boxing, `Vector`-era API misuse) reported *at compile time* with
  precise locations, matching the project's fail-fast `-Werror`
  culture. On a codebase this age, the first run typically finds real
  latent bugs.
- **Why optional:** it overlaps SpotBugs enough that the marginal
  find-rate may not justify a second baseline-and-exclusions apparatus
  plus the `--add-exports` compiler flags it needs. Reasonable to try
  once in a branch, keep only if the initial report pays for the setup.

## Surveyed and rejected (keep what exists / do not adopt)

| Candidate | Overlapping JLS code | Verdict and reason |
| --- | --- | --- |
| **Apache Commons Compress** | `FileAbstractor` container I/O | Reject. It would wrap the *same* `org.tukaani:xz` codec JLS already uses directly, adding a large dependency for zero new capability; zip reading is JDK-built-in. |
| **zstd / other codecs** | `.jls` container | Reject. The container format is a compatibility contract; XZ is fine and already the sniffed default. |
| **Jackson / Gson / kryo etc.** | `Circuit.save/load` line format | Reject. The text format *is* the compatibility contract (pre-fork files, `FORMAT` negotiation, undo snapshots, the planned HDL-import writer all speak it). A serializer swap breaks two decades of student circuits for no user-visible gain. |
| **JavaHelp (jhall)** | `jls.Help` viewer | Already rejected by history: abandoned upstream ~2007, removed by #11 in favor of the in-jar viewer; hosted docs are the recorded future direction. |
| **ELK (Eclipse Layout Kernel)** | future import auto-layout | Settled in `hdl-support-research.md` §3/§7.2: EPL-2.0 without GPL secondary designation — cannot be linked into GPLv3 JLS. Out-of-process runner remains the escape hatch; hand-rolled layered layout first. |
| **JGraphX / JUNG** | `SimpleEditor` canvas | Reject. JGraphX archived since 2020, JUNG dormant; and JLS's editor semantics (wire nets, puts, 12 px grid, save-format coupling) do not map onto generic graph widgets. The bespoke editor is the product. |
| **ANTLR verilog grammars, hdlConvertor, vMAGIC** | `jls.hdl` | Settled negative in `hdl-support-research.md` §7.3–7.4 (macros never expanded, open correctness bugs, dead projects). Emitters stay hand-written; import goes through external Yosys JSON. |
| **AssertJ-Swing (FEST lineage)** | `test/jls/ui` layer 2 | Reject. The fork chain is effectively unmaintained (upstream FEST dead; assertj-swing stale for years); betting the #91 harness on it repeats the JavaHelp mistake. Jemmy (OpenJDK) is alive but heavyweight; plain JUnit + Xvfb covers the need. |
| **jqwik** | round-trip/hardening test suites | Reject under the active-maintenance policy (was recommended in the initial draft): upstream declares maintenance mode — bug fixes and dependency updates only. Technically the best JVM property-based-testing fit; the dormant alternatives (junit-quickcheck, QuickTheories) are worse. The *capability* (generator-driven round-trip and hostile-input fuzzing) stays wanted — revisit if jqwik regains active development or a maintained successor appears. |
| **Cacio-tta (caciocavallo)** | `test/jls/ui` #91 layer-2 harness | Reject under the active-maintenance policy (was "evaluate" in the initial draft): thin maintenance plus deep coupling to JDK internals (`--add-opens` per release) is exactly the combination the policy exists to exclude — each JDK baseline raise would gamble on a quiet upstream. The #91 layer-2 substrate is `xvfb-run mvn verify` on the CI runner instead: zero dependencies, boring, Linux-CI-only. |
| **JTS / spatial index libraries** | `jls.SpatialIndex` (208 lines, #43) | Reject. A full computational-geometry suite to replace 208 pinned-by-tests lines at classroom circuit scale is negative-value. |
| **MigLayout / JGoodies Forms** | ~30 element dialogs | Defer. The dialogs are written and working; relayout churn has no user-visible payoff. Revisit only if the #78 element registry starts *generating* dialogs, where a terser layout DSL would earn its keep. |
| **SLF4J / Logback / JUL config** | `TellUser`, `DefaultExceptionHandler`, CLI stderr contract | Reject. JLS's error-reporting contracts are deliberately narrow and test-enforced; a logging framework adds configuration surface with no consumer (students don't read logs; graders read the contracted stderr lines). |
| **OpenPDF / Batik** | printing (`java.awt.print`), image export | Reject. OS print-to-PDF covers PDF; Batik is a heavyweight way to get what JFreeSVG does in 50 KB. |
| **JavaFX migration** | all of Swing | Out of scope. A platform rewrite, not a library adoption; contradicts the working Swing investment and the single-jar deployment (JavaFX is per-platform modules). |
| **VCD writer libraries** | `BatchSimulator` VCD export | Keep custom. No credibly maintained Java VCD writer exists on Maven Central; the in-tree writer is small and pinned by `VcdExportGoldenTest`. Delegating *viewing* to GTKWave/Surfer (already the docs' stance) is the right split. |
| **PIT (pitest) mutation testing** | JaCoCo ratchet | Not yet — but *not* a maintenance rejection: pitest is actively maintained. Mutation testing (deliberately seeding bugs into the bytecode and checking the tests catch them — a measure of test *strength*, where JaCoCo only measures *reach*) earns its cost once line coverage is well past the current ~18 % ratchet; today it would mostly mutate uncovered code. Revisit when the ratchet crosses ~50 %. |
| **JCommander / commons-cli / airline** | `JLSStart` FLAGS | Dominated by picocli on every axis (maintenance, help generation, completion, zero-dep option). |

## Suggested adoption order

Each surviving recommendation has an investigation issue covering
comparables, benefits, and costs before any adoption commitment:

1. **FlatLaf** (#153) — largest user-visible win per line changed;
   unblocks the U4/#76 ergonomics items. (Runtime, Apache-2.0.)
2. **JFreeSVG** (#154) — smallest effort-to-value ratio in the list;
   same license as the project. (Runtime, GPLv3.)
3. **ArchUnit** (#155) — locks in the architecture the docs describe
   while the #78 registry refactor churns the code. (Test-only.)
4. **picocli** (#156) — fold into the next CLI-growing milestone rather
   than as standalone churn. (Runtime, Apache-2.0.)
5. **Error Prone** (#157) — one trial run, coordinated with the #93
   NullAway plan (NullAway is an Error Prone plugin); keep only if the
   findings pay for the setup. (Build-only.)

Items 1–3 have no interaction with each other or with in-flight issues
and can proceed independently; 4 and 5 each have a stated gate.
