# JLS Architecture

The map a new contributor needs: where the code lives, how the key
flows work, what you must touch to change things, and the decisions
that are settled. Everything here describes HEAD; specifics carry a
`file` / method anchor so you can verify rather than trust. Normative
companion documents: [`docs/simulation-semantics.md`](docs/simulation-semantics.md)
(what a simulation means) and [`docs/batch-interface.md`](docs/batch-interface.md)
(the batch/grading interface contract).

## Module layout

Sources use the historical `src/` layout (not `src/main/java`);
`pom.xml` points Maven at it.

- **`jls`** — application core, no sub-package:
  - `JLSStart` — entry point and CLI: the authoritative flag table
    (`FLAGS`), batch/print/export modes, exit codes.
  - `Circuit` — the circuit model: element list, spatial index,
    save/load (`Circuit.save`, `Circuit.load`, `Circuit.finishLoad`),
    the `FORMAT` version header (issue #79).
  - `FileAbstractor` — container I/O: reads every container `.jls`
    has ever meant (XZ, zip, plain text) by sniffing content, writes
    the current one (XZ) atomically.
  - `LoadError`, `JLSInfo`, `TellUser`, `DefaultExceptionHandler` —
    error reporting (see below). `JLSInfo` also holds global
    settings/statics (grid spacing, default time limit, frame).
  - `Help` — the in-jar help viewer (issue #11): a Swing TOC tree +
    `JEditorPane` over `resources/help/**`, driven by `Map.jhm`
    (topic → page) and `JLSHelpTOC.xml` (contents tree).
  - `BitSetUtils` (value encoding), `SpatialIndex` (editor hit
    testing, issue #43), `Util`, `Tutorial`.
- **`jls.elem`** — the element hierarchy and wiring model:
  - `Element` → `LogicElement` (adds simulation via the
    `jls.sim.Reacts` interface) → ~30 concrete element classes
    (gates, register, memory, mux, state machine, …).
  - `Put`/`Input`/`Output` — connection points; `Wire`, `WireEnd`,
    `WireNet` — the wiring graph and value propagation.
  - `Attribute` — declarative save/load/dialog plumbing for element
    parameters (issue #52); `ElementDialog` — creation-dialog base.
  - Non-elements that live here for historical reasons: truth-table
    display internals (`DisplayBool`, `Cross`, `HLine`, `VLine`,
    `Entry` subclasses), signal-generator parsing (`SigSim`,
    `TestGen`).
- **`jls.edit`** — the editor:
  - `SimpleEditor` — the canvas: toolbar/menu palette
    (`makeElements`), mouse/keyboard state machine, undo/redo,
    checkpointing. ~4k lines; most editor behavior is here.
  - `Editor extends SimpleEditor` — adds file save/save-as/close for
    the application (vs. applet-era) build.
  - `CircuitSnapshot` — undo snapshots (see below).
- **`jls.sim`** — simulation:
  - `Simulator` — the shared event loop, queue, duplicate
    suppression, time limit (issue #25). Headless by construction
    (issue #77): `Simulator` and `BatchSimulator` import no AWT,
    Swing, or `jls.edit` (`HeadlessCoreRatchetTest` enforces it).
  - `BatchSimulator` (headless; `TraceSample` accumulation, VCD
    export) and `InteractiveSimulator` (GUI; step/pause/animate,
    trace window). Batch `-r` trace printing is GUI-side in
    `jls.BatchTracePrinter`, consuming
    `BatchSimulator.getTraceSamples`.
  - `SimEvent`, `Reacts` — the event model.
- **`resources/`** — bundled into the jar: `help/**` (the in-jar
  manual), `images/**`. **`src-filtered/`** — `version.properties`
  template Maven filters so the version is single-sourced from
  `pom.xml`.
- **`test/`** — see "Test layout" below.

## The save/load pipeline

Saving (`Editor.save`, `src/jls/edit/Editor.java`):
`Circuit.save(PrintWriter)` serializes to the line-oriented text
format — a `FORMAT 1` header line (`Circuit.FORMAT_VERSION`), then
nested `CIRCUIT name … ELEMENT Type … END … ENDCIRCUIT` records —
and `FileAbstractor.writeCircuit` wraps it in XZ and renames a temp
file over the target so a crash mid-write never destroys the previous
save.

Loading (`FileAbstractor.openCircuit` → `Circuit.load` →
`Circuit.finishLoad`):

1. **Container sniffing** (`FileAbstractor.openCircuit`): try XZ,
   then zip (original JLS: single `JLSCircuit` entry), then plain
   text. Every reader enforces hostile-input caps (issue #38,
   `UntrustedFileHardeningTest`); failures land in the `LoadError`
   taxonomy rather than a stack trace.
2. **Format negotiation** (`Circuit.readFormatHeader`): no header
   means version 0 (pre-fork files load unchanged); a header newer
   than `FORMAT_VERSION` is refused as `NEWER_FORMAT` ("file needs a
   newer JLS") instead of misparsing (issue #79,
   `FormatHeaderTest`).
3. **Element instantiation routes through the frozen tag table**:
   `ELEMENT Foo` resolves via `SaveTags.resolve(tag)` (canonical
   tags + alias map, issue #79 — tag text never reaches
   `Class.forName`), then `getConstructor(Circuit)` (`Circuit.load`),
   so the loader has no per-element switch — but every element class
   must keep the `(Circuit)` constructor
   (`ElementConstructorContractTest`), and every savable type needs
   a `SaveTags` row (`SaveTagsTest`, `FileFormatSpecTest`).
   Parameters arrive through the typed `setValue` protocol
   (`int`/`long`/`Int`/`String`/`ref`/`pair`/`probe` lines), largely
   routed through the `Attribute` registry.
4. `finishLoad` resolves references (wire ends to puts, subcircuits)
   and validates.

Undo snapshots reuse this pipeline: `CircuitSnapshot`
(`src/jls/edit/CircuitSnapshot.java`) stores the circuit as deflated
save-format text and restores through the ordinary load path, so
undo semantics are exactly save/load semantics, and the round-trip
tests pin both at once (issue #18). Crash recovery likewise:
checkpoints (`.jls~`) are ordinary saves written by a single
background writer thread with coalescing
(`SimpleEditor.writeCheckpointInBackground`/`cancelCheckpoint`).

## Adding an element today (the honest list)

There is no element registry yet — issue #78 will introduce one and
collapse most of this. Until then, a new element touches roughly
sixteen places:

1. a new class in `jls.elem` extending `LogicElement` (or `Element`
   for passive ones);
2. the `(Circuit)` constructor contract (reflective loading);
3. `init` — create `Input`/`Output` puts, size, orientation;
4. a creation dialog (usually an inner `ElementDialog` subclass);
5. `save` — write the `ELEMENT <Type>` record;
6. `setValue`/`Attribute` entries for every persisted parameter;
7. `copy` (paste/duplicate support);
8. `rotate`/`flip` where meaningful;
9. `showInfo`/`showCurrentValue` (status-line and probe text);
10. `initSim` and `react` (see `docs/simulation-semantics.md` §5–6);
11. `resetPropDelay`/`setDelay` if it has timing;
12. a palette entry in `SimpleEditor.makeElements`
    (`src/jls/edit/SimpleEditor.java`) — toolbar button + menu item;
13. a toolbar icon gif in `src/jls/edit/images/`;
14. a help page under `resources/help/elements/**`;
15. a topic in `resources/help/Map.jhm`, an entry in
    `resources/help/JLSHelpTOC.xml`, and the palette list in
    `test/jls/HelpTopicsTest` (the completeness test will fail until
    the topic exists);
16. a fixture in `test/jls/AllElementsRoundTripTest` (and a golden in
    the batch suite if it simulates).

If you find yourself doing this, read #78 first; the registry is the
recorded direction.

## The editor

`SimpleEditor` is a mouse-driven state machine: `private enum State
{idle, chosen, placing, moving, selecting, selected, option,
startwire, drawire}` (`src/jls/edit/SimpleEditor.java`), advanced by
the mouse listeners and rendered by `paintComponent`. Element
creation flows through `setup(new Foo(circuit), …)`: dialog →
`placing` state → click to drop.

Undo/redo are stacks of `CircuitSnapshot`s
(`SimpleEditor.undos`/`redos`); a snapshot is taken before each
mutating gesture, and byte-identical snapshots let no-op gestures
(cancelled dialogs) drop out for free. Editor hit-testing and
connection checks go through `jls.SpatialIndex` (issue #43) rather
than linear scans.

## Threading model

- **The EDT owns all Swing.** Dialogs, repaints, menu updates —
  including those triggered from simulation — must run on the event
  dispatch thread.
- **Interactive simulation runs on a dedicated thread** (the
  `"Runner"` thread, `InteractiveSimulator`,
  `src/jls/sim/InteractiveSimulator.java`). Control state shared
  between the EDT and the sim thread (stop/pause/step flags) is
  `volatile` (issue #49 finding H7, also `Simulator.stopping`); UI
  work initiated from the sim thread is routed through
  `SwingUtilities.invokeLater` (#49 H8) — the clock display is
  additionally rate-limited there. Follow this discipline for any
  new sim-thread → UI interaction.
- **Batch mode never leaves the main thread** and never touches
  Swing (headless-safe; the CI runs it without a display).
- **One background checkpoint-writer thread** (daemon) handles
  `.jls~` writes so the EDT never blocks on I/O
  (`SimpleEditor.checkpointWriter`).

## Error-reporting contracts

- **Load failures** are structured `LoadError`s (issue #58,
  `src/jls/LoadError.java`): a fixed category taxonomy tests assert
  on (`IO_ERROR`, `NOT_A_CIRCUIT`, `MALFORMED`, `NEWER_FORMAT`,
  `UNKNOWN_ELEMENT`, `ELEMENT_ERROR`, `LIMIT_EXCEEDED`), plus
  location, detail, and an actionable hint. Published through
  `JLSInfo.setLoadError`; the legacy `JLSInfo.loadError` string is a
  derived view, so every front end shows the same message.
- **User-visible messages** go through `TellUser`
  (`src/jls/TellUser.java`, issue #81) — the only place allowed to
  create message dialogs. It is headless-aware (falls back to
  stderr), and `NotificationRatchetTest` enforces that no raw
  `JOptionPane` call sites reappear.
- **CLI contract** (issue #42, `src/jls/JLSStart.java`): diagnostics
  are one `jls: error: …` line on **stderr**; exit status 0 =
  success, 1 = runtime failure, 2 = usage error
  (`JLSStart.usageError`). stdout is reserved for results (batch
  output; see `docs/batch-interface.md` §1 for the full table and
  one known deviation).

## Test layout

Tests live under `test/` (JUnit 5, headless — CI has no display):

- `test/jls/` — core: loader round-trips
  (`CircuitRoundTripTest`, `AllElementsRoundTripTest`), format/
  container (`FormatHeaderTest`, `FileAbstractorTest`,
  `ZipLoadingTest`, `UntrustedFileHardeningTest`), load-error
  taxonomy (`CircuitLoadErrorTest`, `LoadErrorReportingTest`),
  simulation goldens (`BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, `VcdExportGoldenTest`) — the oracles the
  normative docs cite — CLI contract (`CliFlagTableTest`,
  `CliSmokeTest`, `CliImageExportTest`), and help integrity
  (`HelpTopicsTest`: link checker, reachability, and the
  palette-coverage completeness test).
- `test/jls/edit/` — editor model: snapshots, checkpoint writer,
  drag bounds.
- `test/jls/elem/` — element contracts: constructor contract,
  attribute persistence, parameter validation, geometry.
- `test/jls/ui/` — the layered UI-verification harness (issue #91).
  Layer 1 (present) is headless model assertions; layers 2 (Swing
  harness under Xvfb) and 3 (render-to-image) are reserved. Read
  `test/jls/ui/package-info.java` before adding UI assertions — every
  helper must be pinned by an assert-the-assertion test.

`mvn verify` runs the suite plus SpotBugs (threshold High) with
compiler warnings as errors; keep it green (see
[CONTRIBUTING.md](CONTRIBUTING.md)).

## Recorded decisions

Decisions that look like accidents until written down. Each carries
its rationale and the trigger that reopens it.

### Internationalization: non-goal (recorded 2026-07)

All user-facing strings are inline English; there is no
`ResourceBundle`/`Locale` usage anywhere, and that is now a
**decision, not an accident** (ergonomics audit U11). Rationale: JLS
is a single-maintainer pedagogy tool serving an English-language
course ecosystem; externalizing every string across ~30 element
dialogs, the editor, and the help tree is a large, ongoing tax with
no requesting user. **Revisit triggers:** (a) a concrete request from
an instructor or course to run JLS in another language, or (b) the
element-registry work (#78) centralizing element metadata to the
point that string externalization becomes cheap as a side effect.
Until then, PRs adding partial i18n scaffolding will be declined.

### Help delivery: in-jar now, hosted docs are the planned future (recorded 2026-07)

User help ships inside the jar as HTML 3.2 rendered by the #11 Swing
viewer (`jls.Help`), because the single self-contained jar is the
deployment model students and labs rely on — help must work offline
and version-locked to the binary. Hosted, versioned web documentation
is the planned future direction (audit M5/I1: searchability, linkable
pages, one source of truth); when it happens, the in-app viewer
shrinks to context-sensitive basics pointing at the site.
**Portability discipline until then:** help content stays plain
HTML 3.2 with relative links and no viewer-specific markup, and the
`HelpTopicsTest` link checker (#70) keeps it truthful, so the same
tree can be published to the web without rewriting. Repo documents
(`README`, `docs/*.md`, this file) are already web-readable on GitHub
and are the normative home for contracts; in-jar help is the
student-facing manual.

### Plugin mechanism: removed (5.0.0, #80)

The inherited XML plugin loader was removed in 5.0.0: it activated
only when a literal `JLS.jar` was on the classpath — a name no
artifact of this fork ever shipped under — so it was unreachable in
every build, and no plugin or manifest is known to exist. A
`ServiceLoader`-based extension registry is the recorded design
direction if demand appears (see the CHANGELOG entry and #78/#80).
