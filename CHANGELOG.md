# Changelog

All notable changes to JLS are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and JLS uses
[semantic versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`) from
4.3.0 onward. A release is made by pushing a `v<version>` tag.

## [5.0.2] — 2026-07-16

### Added
- A batch-mode container image on the GitHub container registry:
  `docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests c.jls`
  gives autograders and CI the whole headless surface (#72's batch API,
  VCD export, image export, HDL export) without a local Java runtime.
  Headless only — no display stack. Multi-stage build: jdeps/jlink trim
  the runtime from the shaded jar (the installer recipe's approach)
  onto ubuntu:24.04 with fontconfig + DejaVu so `-i` image export
  renders text; one recipe, `scripts/build-container.sh` +
  `resources/packaging/Dockerfile`, shared by CI and local builds;
  `latest` tracks stable releases only.

## [5.0.1] — 2026-07-16

### Added
- A Linux AppImage release asset, `JLS-<version>-x86_64.AppImage`: the
  jpackage app-image tree (launcher + jlink-trimmed runtime) folded into
  one self-mounting executable by a version- and sha256-pinned
  appimagetool, with desktop metadata and the `.jls` MIME type inside.
  Built by the same `scripts/build-installer.sh` recipe on the Linux leg;
  runs on distros the deb/rpm cannot target.
- A Nix flake, so NixOS users (whom the deb/rpm/AppImage do not serve)
  build from source: `nix run github:anadon/JLS` runs the editor,
  `nix profile install` gives the `jls` command plus a desktop entry,
  icon, and `.jls` file association; `nix develop` opens a JDK 25 +
  Maven shell. The package builds against the pinned nixpkgs JDK 25 with
  the dependency closure fingerprinted by `mvnHash` (tests remain CI's
  job — the sandbox has no fonts, the same reasoning as #111).
- Releases also publish the jar to the GitHub Packages Maven registry
  (`maven.pkg.github.com/anadon/JLS`), populating the repository's
  Packages sidebar. GitHub's Maven registry requires an access token
  even for public downloads, so the Releases page remains the primary
  distribution channel; the registry serves Maven tooling.

## [5.0.0] — 2026-07-16

*(Renumbered from 4.3.0-SNAPSHOT: the plugin-mechanism removal below is a
feature removal, which is a MAJOR version event under semantic versioning.)*

### Added
- VHDL export completes HDL stage 1 (#60): `jls -export out.vhdl circuit.jls`
  (or `.vhd`) writes the same circuits the Verilog exporter handles as a
  single structural VHDL entity, accepted by both VHDL-93 and VHDL-2002
  analyzers, sharing the language-neutral model walk (element policy, jump
  aliasing, name legalization) with the Verilog emitter; goldens are pinned
  and, where `ghdl` is installed, analyzed as part of the test suite.
- A normative save-format specification, `docs/file-format.md` (#79):
  containers (plain text, zip, XZ), the `FORMAT` header and version
  negotiation, the full grammar, string escaping, the per-type tag table,
  and the evolution policy — every claim derived from code and pinned by a
  spec-drift test (`FileFormatSpecTest`).
- Startup toolkit policy for Wayland (#105, toward #100): on a
  Wayland-only Linux session, JLS selects the JetBrains Runtime's
  `WLToolkit` when the running JDK has it, and otherwise fails fast with
  one actionable `jls: error:` line (XWayland or JBR/Wakefield hints)
  instead of the misleading generic display error; `-Djls.toolkit=
  default|wayland` forces either branch; batch/X11/Windows/macOS paths are
  untouched. The decision function is pure and unit-tested across the
  whole environment matrix.
- A headless-Wayland GUI rig (#101, authoring slice): `scripts/wayland-rig.sh`
  boots JLS under headless sway with a pinned JBR, asserts the window
  appears, and captures screenshots/tree/stderr; a `gui-wayland` first-light
  CI lane runs it (non-blocking until stable; the JBR checksum pin awaits a
  maintainer with access to the JetBrains CDN).
- A pointer/geometry API ratchet test and a census document
  (`docs/pointer-geometry-census.md`) covering all 66 former global-read
  sites and their replacements (#102).
- jpackage installers with a bundled runtime and a `.jls` file
  association (#82): `deb`/`rpm` on Linux (locally verified end-to-end,
  including the `Exec=… %f` fix without which double-clicked files never
  reached the app), `msi` on Windows and `dmg` on macOS (authored,
  non-blocking until first verified on real runners); one build recipe,
  `scripts/build-installer.sh`, shared by CI and local builds; app icons
  generated programmatically and checked in with their generator.
- Verilog export, HDL stage 1 (#60): `jls -export out.v circuit.jls`
  writes the drawn circuit as one structural Verilog-2005 module,
  deterministically (no timestamps; goldens pin the bytes). Covered
  elements: input/output pins (module ports), constants, the gate
  family (AND/OR/NAND/NOR/XOR/NOT/DELAY), Extend, TriState (0/1/z —
  JLS never simulates x), Adder with carry in/out, Register (latch,
  positive- and negative-edge flip-flop, with initial values), Clock
  (exported as a module input port to drive from a testbench), and
  Binder/Splitter (part-selects). Same-named jump starts/ends fold
  into one named net; user names survive legalization, with any
  renames documented in the generated header. Simulation-control and
  annotation elements (Display, SigGen, Pause, Stop, Text, TestGen)
  are skipped with a warning; circuits containing SubCircuit, Memory,
  Mux, Decoder, StateMachine or TruthTable are rejected with one
  message naming every offending element, and nothing is written
  (temp-file + atomic-rename). The exporter (`jls.hdl`) is
  headless-core clean and split into a language-neutral model plus a
  Verilog emitter so the VHDL emitter can share the walker; when
  `iverilog` is installed, the test suite also compiles every golden
  export (it skips cleanly where the tool is absent). VHDL, subcircuit
  hierarchy, Memory/Mux/Decoder/StateMachine/TruthTable templates, and
  the GUI menu entry are deliberately follow-up slices.
- Normative and contributor documentation (#85):
  `docs/simulation-semantics.md` specifies the simulation model —
  time, event ordering, per-element delays, edge triggering,
  initialization, tri-state/HiZ resolution — with every claim
  anchored to code or a golden test, and an appendix filing the
  semantic surprises found while writing it as candidate bugs;
  `ARCHITECTURE.md` maps the codebase for contributors (modules,
  save/load pipeline, element authoring surface, editor, threading,
  error contracts, test layout) and records three scope decisions:
  internationalization is a non-goal with explicit revisit triggers,
  help stays in-jar with hosted docs as the planned future direction,
  and the plugin mechanism stays removed (#80). The README gained a
  Documentation section linking all project documents.
- A help-coverage completeness test (#85): every element in the
  editor palette must have a Map.jhm topic resolving to a bundled
  help page (`HelpTopicsTest.everyPaletteElementTypeHasAMappedHelpTopic`,
  static palette list until #78's registry exists). Re-counting the
  ergonomics audit's "21 undocumented element classes" against the
  real palette found every user-creatable element already documented
  (the remainder are internal classes), so the test pins completeness
  rather than repairing it; the truth-table page, previously findable
  only from its dialog's Help button, is now also in the help table
  of contents.
- Batch mode is now a documented, tested grading API (#72):
  `docs/batch-interface.md` is a normative spec — and a stability
  contract — for the `-t` test-vector grammar, the watched-element
  stdout format, the exit/stream contract, and the new waveform export.
- A `-vcd file` flag: batch runs export the value-change history of all
  watched signals as IEEE 1364-2001 VCD (readable by GTKWave/Surfer and
  autograders). JLS's two-state-plus-HiZ values map to `0`/`1`/`z`
  (never `x`); output is byte-deterministic and pinned by golden tests
  (#72).
- A development container image (`.devcontainer/`) with Maven, Temurin
  JDK 21, and the optional development tools now documented in the README:
  XZ/zip utilities for `.jls` files, a Wayland-only display stack
  (sway/grim/wtype — X11 is excluded by project policy), ImageMagick,
  and the Icarus Verilog/GHDL/Yosys toolchain for the HDL roadmap
  (#33, #59).

### Removed
- The XML plugin loader (#80). It activated only when a literal `JLS.jar`
  was on the classpath — a name no artifact of this project ever shipped
  under — so it has been unreachable in every build; no plugin, manifest,
  or plugin documentation is known to exist anywhere. Anyone affected is
  invited to open an issue; a ServiceLoader-based extension registry is
  the recorded design direction if demand appears.

### Fixed
- Multi-driver nets resolve deterministically — the winning driver is
  fixed by file order instead of hash-set iteration — and a bus conflict
  (two active drivers disagreeing) warns once, naming the net and time;
  single-driver behavior is unchanged (#98 S1).
- Re-initializing a simulation restores a `Register`'s initial value
  again: `initSim` assigned a shadowing local instead of the field, so a
  second run started from the previous run's captured value (#98 S2).
- A `StateMachine` that sees a clock edge with no matching transition no
  longer freezes for the rest of the simulation; it stays in its current
  state, keeps tracking the clock, and warns once per run (#98 S5).
- `TriState` no longer floods the event queue re-announcing an unchanged
  output on every input event (#98 S6). The remaining "surprises" from the
  semantics-spec appendix were adjudicated as intended behavior and are
  now specified in the document body: subcircuit input initialization
  (S3, a spec misreading), Pause's pause-on-non-zero rule (S4, the help
  page now matches the code), and Constant's width-adaptive masking (S7).
- Element and dialog placement no longer reads the global pointer or
  global screen coordinates (MouseInfo ×23, `getLocationOnScreen` ×38,
  whole-screen sizing ×5 — all now zero, enforced by a ratchet test):
  new elements drop at the editor's last event-local mouse position (or
  the visible-canvas center from the toolbar — which also fixes a doubled
  viewport offset), dialogs center on their owner window, and the latent
  `MouseInfo.getPointerInfo()` null-dereference pattern is extinct. This
  is the Wayland-correctness groundwork for #100 (#103, #104, #102).
- The keypad popup is a well-behaved owned dialog (#104, #86): it anchors
  to the field that opened it (no more chasing window moves by polling
  screen coordinates), and it dismisses on Esc, on focus loss, and on a
  click outside — the undocumented right-click-on-a-digit dismiss gesture
  is gone (the visible hide button remains).
- Opening a circuit from disk records its directory again, so Save writes
  back to the source file instead of the filesystem root, and checkpoints
  (`.jls~`) land beside the circuit (#34).
- The unsaved-changes flag survives a failed save: it is now cleared only
  after the file write succeeds, so a write failure still prompts to save
  and still checkpoints (#41).
- A checkpoint queued before a save can no longer be written afterwards and
  resurrect stale content (#45).
- Every popup-menu action works again — a merge-conflict brace error had
  made all of them (rotate, flip, cut/copy/paste, undo/redo, probe, …)
  unreachable (#37).
- Print mode (`-p`/`-v`) actually loads the circuit file instead of
  printing blank pages (#48).
- Values wider than ~48 bits no longer silently become zero
  (`BitSetUtils.Create`) (#50).
- Saved strings containing backslashes round-trip correctly (#53).
- Circuits containing a `JumpEnd` load deterministically on every JVM
  (explicit constructor selection) (#55).
- A legacy `Display` with `orient 0` (Top) no longer corrupts its file on
  re-save (#57).
- Load failures report their own cause: no more stale error messages,
  swallowed subcircuit failures, or I/O errors disguised as format errors
  (#58).
- Wire hover highlights appear and disappear correctly again (#35).
- Undo/redo cancels any in-flight gesture first and refreshes other
  editors' Import menus, so nothing operates on the discarded circuit (#39).
- Invalid element parameters in circuit files (negative memory capacity,
  non-positive clock times, out-of-range truth-table entries, negative
  group indices) are rejected at load instead of crashing or livelocking
  the simulator; Memory and Clock dialogs enforce the same constraints (#52).
- Element dialogs validate input through one shared mechanism (#52
  addendum): a `validateInputs()` hook on the dialog base rejects invalid
  values with an inline error message — the same wording the loader uses
  for the same rule — focuses the offending field with its text selected,
  and exposes the message to assistive technology; StateMachine,
  TruthTable, and Group gained dialog-side validation (StateMachine now
  refuses OK without an initial state), and Memory's bits/word rule is
  enforced at load as well as in the dialog.
- Interactive simulator: Stop/Pause/Step can no longer be missed
  (volatile control state), UI updates happen on the event-dispatch
  thread at a bounded rate, Step always advances, and "Run (in
  background)" reliably suppresses UI updates for the whole run (#49).
- Malformed batch invocations print one `jls: error:` line and exit with
  status 2 (usage) or 1 (runtime failure) instead of crashing (#42).
- Assorted small fixes: hex/binary field maximums, doubled menu actions
  in the truth-table signal editor, crash reports no longer dump all
  system properties, Windows path handling, and more (#51).
- Batch mode prints watched elements in element-name order instead of
  hash-set iteration order, so the output is stable across runs, JVMs,
  and code changes; anyone diffing old batch output may see lines
  reordered (never reworded) once (#72).
- The batch signal trace no longer records a duplicate event on every
  reaction of an element that stays at HiZ (#72).

### Changed
- The language baseline is Java 25, the current LTS (#92): the build, CI
  (with an advisory build on the newest GA feature release), release and
  CodeQL workflows, and the dev container all moved from 17/21 to 25;
  running JLS now needs a Java 25 runtime (or an installer with the
  bundled runtime, above). The floor follows the current LTS and is
  revisited once per LTS cycle (#96).
- Release engineering: manual `workflow_dispatch` runs of the release
  workflow are dry-runs (build, verify, checksums — no publish), and CI
  builds the jar twice and fails if the bytes differ, keeping the build
  reproducible (#44).
- Command-line flags may now be longer than one letter, with
  longest-name matching: `-vcd` is the VCD export flag, no longer
  parsed as `-v` with the attached printer name `cd` (#72).
- The proprietary MTU license acceptance gate at startup is removed; JLS
  is GPLv3 (see `LICENSE` and `pop_GPLv3.pdf`) and the About dialog now
  says so (#40).
- The application version is single-sourced from the Maven build; the
  About dialog, window title, and crash reports no longer claim
  "4.1, built 2014" (#36).
- Crash reports point to the GitHub issue tracker instead of the original
  author's email.

### Security
- Circuit files are treated as untrusted input: decompression is bounded
  (zip and XZ), run-length-encoded memory images are capped by the
  declared capacity, the format sniffer no longer leaks a file descriptor
  per open, and the plugin manifest parser refuses DOCTYPEs and external
  entities (#38, #46).

## [4.2.0-alpha.1] — 2026-07-01

Post-modernization snapshot: Maven build, CI, headless test suite,
performance work, and de-duplication (the #16 program). Never released
as a binary.

## [4.1.5] — 2014-03-18

The last legacy-era state, self-reporting "JLS 4.1 build 5, 2014".
