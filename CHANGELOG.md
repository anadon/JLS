# Changelog

All notable changes to JLS are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and JLS uses
[semantic versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`) from
4.3.0 onward. A release is made by pushing a `v<version>` tag.

## [Unreleased] — 5.0.5-SNAPSHOT

### Added
- Batch advancement of the open issue backlog (multi-agent workflow
  session, one worker + one adversarial auditor per issue; each
  increment's detail is recorded on its issue thread and in the
  per-issue commits on this branch). Highlights: collaboration
  Stages 0b-2 groundwork (#167 element-transplant ops, #168
  jls.collab.net identity/handshake/framing, #169 jls.collab.session
  roster and floor control, #170 ElementVocabulary + security
  ratchets, #171 jls.collab.crdt causal substrate); the element
  registry (#78) and frozen save-tag table (#79) replacing
  Class.forName in the loader; the sealed element hierarchy (#95);
  JSpecify/NullAway on the default build for jls.sim/jls.util (#93);
  value-semantics increment (#94); headless jls.core ratchet step
  (#77, BatchTracePrinter extraction); editor UndoManager extraction
  (#84); keyboard accelerators/mnemonics and focus model (#75);
  CVD-safe theme + wire glyphs (#76); editor zoom Viewport core
  (#74); subcircuit-disabled banner (#86); tutorial refresh (#73);
  menu-bar spec test (#91) and display-substrate additions (#162);
  editor save/clipboard smoke tests (#56); HDL Stage 1 mux/decoder
  export (#59), Stage 2 Yosys front end (#61), layout seam (#62),
  Stage 3 header scanners (#63); installer reproducibility plumbing
  and gates (#184/#185/#188/#189/#190/#191); GPG and Authenticode
  signing scaffolding (#136/#134); Wayland rig hardening (#101);
  FlatLaf and picocli evaluations (#153, #156 — picocli rejected);
  PIT mutation-testing trial (#161); coverage climb (#159); private
  doclint progress (#192). Issues #81, #111, #156, #180-#183 closed.
- State machines now save deterministically (#180): `StateMachine.save`
  and `State.save` iterated `HashSet`s whose members override no
  `hashCode`, so state and transition blocks were emitted in
  identity-hash order — per-run-variable bytes for the same machine,
  breaking canonical serialization (#166) and the `stateHash`
  convergence oracle (#163). States now save sorted by name (grid
  position tie-break) and transitions unconditional-first, then
  `else`, then conditionals by (signal, eq, value, bits, next) — a
  total order drawn from the data itself.
- Simulation results are now reproducible for order-sensitive
  circuits (#181): the time-0 event seed (`Simulator.initSimulation`,
  `SubCircuit.initSim` at every nesting depth) iterated the element
  `HashSet`, so same-time event sequence numbers — the tie-breaker
  that decides which of two simultaneous drivers a cross-coupled
  latch or plain multi-driver net settles on — were assigned in
  identity-hash order, varying between runs and machines. The seed
  now walks the circuit's canonical stable-id order
  (`Circuit.getElementsInStableOrder`), making every simulated value
  a pure function of circuit content.
- Printed page order is now deterministic (#182): `Circuit.addToBook`
  collected state-machine, truth-table, and subcircuit pages by
  iterating the element `HashSet`, so successive prints of one
  circuit could order those pages differently. All three passes now
  follow the canonical stable-id order.
- Circuits built from scratch now save reproducibly (#183): the
  replica half of every freshly minted stable id was a random UUID
  drawn once per JVM, so a never-yet-saved circuit produced different
  `sid` bytes on every process. The replica is now per-install: an
  explicit `jls.replicaId` system property / `JLS_REPLICA_ID`
  environment override (the deterministic knob for CI and
  reproducible export) wins, then a value persisted in
  `$XDG_CONFIG_HOME/jls/replica-id` (`~/.config/jls/replica-id`),
  then a fresh draw persisted for later starts. Loaded-file saves,
  the `legacy` minting path, and identity semantics are unchanged.
- Circuits saved on Windows are now byte-identical to the same
  circuits saved on Linux (#111): `Circuit.save` pins `\n` line
  endings whatever the platform separator is, which the
  deterministic-serialization guarantee (#166) and `stateHash`
  require. Also fixed the first of the Windows test failures
  (`CircuitRoundTripTest.saveLoadIsAFixedPoint`).
- Opening a circuit no longer keeps the file locked (#111): the
  XZ and plain-text read paths drained the file lazily through the
  returned Scanner, holding an open handle for the circuit's whole
  editing lifetime - which on Windows blocks deletion (and broke
  the test suite's temp-dir cleanup). All three container paths now
  read into memory (already bounded by the 64 MiB hostile-input cap)
  and release the file before returning. A `.gitattributes` entry
  additionally protects the byte-exact `.jls` fixtures from CRLF
  rewriting on Windows checkouts.
- The remaining Windows test failure (#111) — the orientation-geometry
  baseline "diverging" on Windows — was golden-file line endings, not
  geometry: an `autocrlf` checkout rewrote `test/resources/` goldens
  to CRLF while the tests compare against `\n`-joined strings. The
  `.gitattributes` protection now covers `test/resources/**` (the
  geometry baseline and all HDL export goldens). The `TriState
  finishLoadError=invalid element` lines noted in the same report are
  deliberate: collinear gate/control orientation pairs have always
  been rejected, and the baseline records that. CI gains an advisory
  `windows-latest` lane so `mvn verify` exercises Windows on every
  build (promoted to required once stable, like the Wayland lane).
- Printing a freshly loaded circuit containing a truth table no longer
  crashes with a NullPointerException: `TruthTable.print` assumed the
  table's display panel had been built by the edit dialog, which is
  only true after the dialog has been opened once in the session. The
  panel is now built lazily at print time. Found by the new print-path
  smoke test.
- A Memory whose initial-contents text names addresses at or past the
  declared capacity no longer saves a file JLS itself refuses to load
  (#160): such text is accepted leniently at load (zeros assumed at
  simulation start, as before), but the save path re-encoded it as an
  `initrle` attribute whose loader enforces the capacity bound. The
  RLE encoder now falls back to the raw `init` encoding for
  out-of-capacity addresses. Found by the new generative fuzzer on
  its second seed.

### Added
- Documentation is now build-enforced: `mvn verify` runs a javadoc
  doclint gate (all groups except unresolvable-by-design test
  references) over the public/protected API with warnings as
  failures, and a new package-info ratchet requires every package in
  both trees to carry a package comment. The gate is scoped to API
  visibility because under `-private` the standard doclet warns on
  every test-tree `@see` target in a way doclint exclusions cannot
  reach (verified empirically; recorded in the pom comment).
  Fixed en route: eleven malformed doc comments the #186 pass left
  behind - duplicated `@param v1` tags in five files, three stale
  javadoc blocks orphaned above attribute tables by the #23
  refactor, a mistyped `@returns`, two empty comments, and a VCD
  format description whose angle-bracket placeholders parsed as
  unclosed HTML.
- Per-package coverage floors (#159): the JaCoCo ratchet now guards
  `jls`, `jls.sim`, `jls.elem`, and the new `jls.collab.op` package
  individually (instruction, line, and branch), so a regression in
  the tested core can no longer hide behind coverage gains elsewhere
  in the bundle - verified by a synthetic regression (deleting the
  op-layer test class) that the bundle instruction/line floors do not
  catch but the package floors do. `jls.edit` stays deliberately
  unfloored until the UI-harness work makes editor code testable.
- Operation layer, first slice (#167, collaboration stage 0b): a new
  `jls.collab.op` package reifies editor mutations as a closed, sealed
  vocabulary of validated, invertible, serializable commands
  (`CircuitOp`) applied through one entry point (`OpSink`), addressing
  elements by stable id (#165) and verified against the canonical
  serialization oracle (#166): apply-then-inverse restores the exact
  prior bytes, rejected ops change nothing, and the strict reader
  (`CircuitOpReader`) round-trips every kind byte-identically while
  rejecting malformed input. Six op kinds exist (`ToggleWatched`,
  `AttachProbe`/`RemoveProbe`, `RotateElement`, `FlipElement`,
  `MoveElements`) and six editor gestures now go through the entry
  point (watch toggles via ctrl-W and menu, rotate both directions,
  flip, probe attach/remove); the full mutation-site inventory and
  migration status live in `docs/operation-layer.md`. Snapshot undo
  is unchanged - `OpSink.submit` still runs the existing
  `markChanged` bookkeeping. An ArchUnit rule pins the #163 layering
  from day one: no collab package outside `jls.collab.ui` may touch
  Swing.
- Stable element identity (#165, collaboration stage 0a): every
  element now carries a permanent id (`replica:counter`), minted at
  creation and persisted in the save format as a new `sid` base
  attribute. Files that predate the field mint deterministic
  `legacy:N` ids at load, in file order, so every load of the same
  file agrees. Identity survives save/load, undo restore, and
  checkpoint recovery; copying mints a fresh id (a paste is a new
  element). A file declaring the same id twice is refused with a
  structured load error. No format-version bump: the attribute is
  simulation-neutral metadata under the documented evolution policy
  (`docs/file-format.md` §8-9).
- Deterministic canonical serialization (#166, collaboration stage
  0c): a circuit's saved form is now a pure function of its content.
  Elements are emitted sorted by stable id with the file-local
  `id`s (and so the wire `ref` lines) assigned in that order -
  previously two loads of the same file saved differently in 261 of
  276 lines on the fork shift-register fixture. Round-trip suites
  now assert byte equality end to end; the test-side `canonicalize`
  workaround (documented as unsound for circuits with wires) is
  deleted. `Circuit.stateHash()` exposes the SHA-256 of the
  canonical bytes as the convergence oracle for #163.
- Per-element simulation goldens with a completeness ratchet (#158):
  every simulating palette element now has a behavioral pin -
  Adder (sum/carry sweep), Decoder (one-hot), Extend, Mux, the
  barrel shifter's three kinds, TruthTable (rows and don't-cares),
  Binder/Splitter range routing, JumpStart/JumpEnd, SigGen and Stop
  join the existing gate/memory/sequential goldens. Expected values
  are computed independently of the implementation. A reflective
  ratchet fails the build when a palette element has neither a
  golden nor a commented exemption, so the gap cannot reopen.
  Detection power was verified the issue's way: a hand-mutated Mux
  select decode passes the entire pre-existing suite (40 tests) and
  fails the new golden precisely.
- CLI subprocess coverage (#159): the JaCoCo agent now rides into
  the JVMs the CLI suites spawn, so JLSStart's exercised paths are
  measured (4.7% -> 25.5% line on its own).
- Headless draw- and print-path smoke coverage (#91 layer 1, #162):
  every palette element (including a nested SubCircuit) draws on
  both export canvases - raster and SVG - with a reflective
  completeness sweep keeping the fixture honest; every page the
  print Book collects (circuit, state machine and its output
  summary, truth table, nested subcircuit) renders into a
  Graphics2D. Plus direct tests for the clipboard copy machinery
  (`Util.copy`/`partition`, including partial-selection pruning),
  the pure Util helpers, and Memory's file-based initialization.
- The display-test substrate (#162): a `display`-tagged surefire
  execution, headless (self-skipping) by default and run for real
  under `xvfb-run -a mvn -B verify -Djls.test.headless=false`, which
  CI now does. Tenants:
  - `DialogConstructionSmokeTest` constructs all 24 element
    create/edit dialog families through the editor's real `setup()`
    entry and dismisses each through the close-box cancel path.
  - `EditorGestureTest` (#91 layer 2, #84 safety net) drives the
    SimpleEditor mouse state machine with synthetic events - move,
    rubber-band select, right-click delete, undo/redo - and asserts
    the resulting circuit model. Synthetic `MouseEvent` dispatch, not
    `Robot`: the move/select/menu paths read `event.getX()/getY()`,
    so the whole suite runs deterministically in ~1.3 s. Because it
    touches only surfaces that survive the #84 decomposition (canvas
    events, popup item texts, the circuit model), it is the
    characterization safety net for that refactor.
  The bulk of the suite stays headless in its own execution either
  way (with a display present, `TellUser` turns interactive, so
  mixing the two would let a stray warning block a test on a modal
  dialog).
- The coverage ratchet floors rose twice from 17.5%/18.0% (set
  2026-07-08): first to 34.5% instruction / 34.0% line with a first
  32.5% BRANCH floor, then to 42.0% / 40.0% / 36.5% (measured
  headless: 42.65% / 40.88% / 37.21%; under the display substrate
  the same commit measures 51.13% / 48.85% / 38.26%).
- Vector circuit image export (#154): `-i out.svg` writes the circuit
  as resolution-independent SVG through the same element paint path
  the PNG/JPEG export uses, via JFreeSVG (GPLv3, same license as JLS,
  ~50 KB, no transitive dependencies). Exports are deterministic -
  the same circuit produces byte-identical SVG across runs and load
  instances (elements draw in a stable geometric order, not HashSet
  order), so SVG goldens and reproducible-builds expectations hold.
- Executable architecture rules (#155): ArchUnit 1.4.2 (test scope
  only, nothing ships in the jar) checks the compiled bytecode for
  the invariants the source-scan ratchets state - only `TellUser`
  touches `JOptionPane` (#81), HDL internals are wired only from the
  CLI (#60), and `jls.sim` must not depend on `jls.edit` (#77, with
  the three existing simulator classes pinned as a shrinking
  baseline). New violations fail `mvn test` immediately.
- An opt-in `errorprone` Maven profile (#157):
  `mvn -Perrorprone clean compile` runs Error Prone 2.50.0's default
  checks over the build. The trial verdict (no default-build gate;
  the plumbing waits for #93 NullAway) is recorded in
  `docs/library-survey-2026-07.md`.
- Seeded generative fuzzing of the save/load pair (#160), dependency
  free: `GenerativeRoundTripFuzzTest` drives random circuits (element
  mix, boundary bit widths, escape-space names, wired pin pairs)
  through the save → load → save fixed point and asserts truncated
  saves classify cleanly; `ContainerMutationFuzzTest` bit-flips,
  truncates, splices and corrupts valid text/zip/XZ containers and
  asserts every outcome is a classified `LoadError`, never an escaped
  exception. Failures reproduce from printed seeds.
- Opt-in plain-text saves (#129): the Save As dialog offers a
  plain-text file type, and the new `-savetext out.jls circuit.jls`
  flag rewrites an existing circuit file uncompressed. Plain-text
  `.jls` files diff cleanly in version control and open in JLS forks
  that removed the XZ reader; saves stay XZ-compressed by default,
  and the loader has always accepted both.

## [5.0.4] — 2026-07-16

### Added
- The container image is signed and attested (#133): tag pushes sign
  the multi-arch manifest digest with keyless cosign (bound to the
  release workflow's OIDC identity — no maintainer-held keys) and
  attach the same build-provenance attestation the jar and installers
  carry, pushed to the registry. Verification commands are in the
  README; dry-runs neither sign nor push. Signing goes on the digest,
  never a tag.

## [5.0.3] — 2026-07-16

### Added
- Multi-architecture distribution (ARM and RISC-V):
  - The container image `ghcr.io/anadon/jls` is now a multi-arch
    manifest — `linux/amd64`, `linux/arm64`, `linux/riscv64` — built on
    `ubuntu:26.04` + OpenJDK 25, the one JDK-25-bearing base published
    for all three architectures (eclipse-temurin has no riscv64);
    arm64/riscv64 legs build under QEMU, and dry-runs prove every
    platform without pushing.
  - aarch64 Linux installers (deb, rpm, AppImage) from an
    `ubuntu-24.04-arm` runner, with a per-architecture pinned
    appimagetool, and an aarch64 Windows installer from a
    `windows-11-arm` runner (both experimental until checked on real
    hardware, like their x86_64 predecessors were).
  - Installer names now carry the architecture
    (`JLS-<version>-<arch>.msi`/`.dmg`; deb/rpm already did), and the
    per-leg checksum assets became `SHA256SUMS-installers-<os>-<arch>`
    so same-OS legs cannot overwrite each other.
  - RISC-V gets no native installer — no GitHub runners exist and
    jpackage cannot cross-compile its launcher — so it is served by the
    container image and the architecture-independent jar. The macOS dmg
    was already ARM: macos-latest runners are Apple silicon.

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
