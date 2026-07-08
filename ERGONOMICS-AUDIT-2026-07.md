# JLS Interface & Ergonomics Audit — July 2026 (radical doubt)

**Scope.** Every interface the project presents — human (editor, dialogs, help,
onboarding) and programmatic (CLI, batch/grading I/O, save format, element
authoring API, plugin mechanism, build/distribution) — plus all project
materials (README, help content, tutorial, contributor docs). This audit was
conducted under **radical doubt**: architecture, API shape, and other normally
foundational choices are in scope and are questioned on the merits, not
grandfathered.

**Relationship to prior audits.** [AUDIT-2026-07.md](AUDIT-2026-07.md) was a
*correctness* audit (does the code do what it claims). This is the
complementary *interface* audit (are the things it presents to people and
programs the right things, shaped correctly). Correctness findings already
tracked in the #33 program are cited but not re-litigated. Verified at commit
`01854ed` (tags `v4.1.5` = legacy baseline, `v4.2.0-alpha.1` = post-modernization
snapshot).

**Method.** Full read of the CLI/startup path, simulation core, file container
layer, and build/CI by the primary auditor; three parallel deep-reads of (a)
the element-authoring API, (b) the editor interaction model, and (c) all
user/contributor-facing materials; findings cross-checked against source with
file:line evidence. One finding (U-dispatch, the popup-menu nesting bug) was
independently rediscovered during this audit and matches open issue #37 —
treated as cross-validation, not a new finding.

---

## Executive summary

The 2026 modernization fixed the build, the loaders, and a large band of
correctness bugs — invisibly. The interfaces through which anyone actually
*experiences* JLS are still shaped by decisions made for 2004: a mouse-only,
fixed-canvas, Metal-look editor with no zoom; a CLI whose own usage text
misdocuments it; a grading interface whose waveform output goes only to a
physical printer; a plugin loader that has been silently dead since the Maven
build renamed the jar; help content whose images broke in the package rename a
decade ago and whose failures are silent; and an About box that still says
"2014, All Rights Reserved."

The radical-doubt verdicts, in one paragraph: **keep** the drawn-circuit +
event-driven-simulation product concept, the line-oriented text save format
(with a version header and a written spec), Swing as the current toolkit (but
behind a layered core so it stops being a life sentence), and the element-graph
data model. **Question and replace**: the global-static plumbing (`JLSInfo` as
frame/simulator/error bus), the `Element` inheritance contract
(abstract-by-runtime-crash, capability boolean pairs, hand-enumerated palette),
the save format's identity coupling to Java class names, the plugin mechanism
(dead, and wrong in shape — remove or redesign, don't harden), printer-centric
output (replace with files: image export exists, waveform export doesn't), the
forced Metal look-and-feel, and the assumption that discovery-by-right-click
plus an in-jar HTML 3.2 help viewer constitutes onboarding.

The unifying ergonomic thesis: **for a teaching tool, the product is the
student's first ten minutes and the grader's script.** Both are currently the
weakest surfaces in the project. A student meets a bare jar that needs a JDK,
an empty canvas with no guidance, red/green-only signal semantics, and help
pages with broken images. A grader meets an undocumented test-vector format,
ad-hoc stdout, and a trace that can only be printed on paper. Every priority
in Part 6 serves one of those two people.

---

## Part 1 — Foundations (architecture-level doubts)

### F1 (high) — There is no layering: the engine cannot exist without the GUI

The abstract simulator base class imports Swing, AWT, and the editor package
(`src/jls/sim/Simulator.java:3-12` — `jls.edit.*`, `java.awt.*`,
`javax.swing.*` in a class whose job is a priority queue and an event loop).
`Circuit` holds an `Editor` back-reference (`Circuit.java:1196-1206`) and its
`draw` takes a `SimpleEditor` parameter (`Circuit.java:772`). `BatchSimulator`
imports `javax.print` and Swing (`BatchSimulator.java:6-12`). Headless batch
operation works because `java.awt.headless=true` is set at runtime
(`JLSStart.java:106`), not because any layer is actually headless.

Consequences: the simulator cannot be embedded (in an autograder, a test
harness, a web service, or a future non-Swing UI); GUI-free unit tests of
simulation exist only because the classes happen not to touch the Swing they
import; every future UI idea (HiDPI canvas, web front-end, VS Code-style
extension) is priced at "first untangle the core."

**Verdict:** the single most consequential structural debt. A `jls.core`
(circuit model + simulation + persistence, zero AWT/Swing imports, enforced by
an ArchUnit-style test or a Maven module split) is the enabling move for half
the findings below.

### F2 (high) — The save format's identity is Java implementation detail

Element type tags in `.jls` files are Java class simple names, resolved by
reflection against a hardcoded package: `Class.forName("jls.elem." +
elementType)` (`Circuit.java:399`). Every element hand-writes its own tag
(`println("ELEMENT Register")`) that must equal its class name — 24 duplicated
stringly contracts (e.g. `Gate.java:59-60`, whose comment admits "must match
the class name Circuit.load resolves"). The format has **no version marker**
anywhere (`Circuit.save`, `Circuit.java:721-743`, writes `CIRCUIT name … ELEMENT
… ENDCIRCUIT` with no header), and no written specification — the loader's
code is the spec. The `.jls` extension has meant three different container
formats over the years (README.md:41-59, the one place this is documented).

Consequences: renaming or repackaging a class is a silent file-format break
(this nearly happened once already — the `edu.mtu.cs.jls` → `jls` rename; only
the tag's use of *simple* names saved the installed base); no non-Java tool
can be written against the format with confidence; format evolution (new
attributes, deprecations) has no negotiation mechanism, only "omitted
predicate" conventions whose bugs are already in the tracker (#57).

**Verdict:** keep the line-oriented text format (it diffs, it compresses, it
has 20 years of files behind it) — but the type namespace must become a
format-owned registry (tag → element, one table), a `FORMAT <n>` header line
must be added ahead of 4.3, and the format deserves a normative spec document
against which the round-trip tests assert.

### F3 (high) — The element model: ~16 uncheckable touchpoints per element

A new element must satisfy roughly **15–17 distinct wiring points across four
source/resource areas**, almost none compiler-enforced: live in package
`jls.elem` (`Circuit.java:399`); put its `(Circuit)` constructor *first* in
declaration order (`Circuit.java:400-401`, `getConstructors()[0]` — issue
#55); override `init`/`copy`/`save`/`initSim`/`react`, all of which are
runtime-throw or print-and-continue stubs rather than abstract methods
(`Element.java:295-305`, `LogicElement.java:374-377,436-439`); duplicate its
class name in its save tag; register by hand in the ~330-line
`SimpleEditor.makeElements()` (`SimpleEditor.java:869-1197`); ship a
`edit/images/<name>.gif` loaded by unchecked `getResource`
(`SimpleEditor.java:1204-1207` — a missing gif NPEs at startup); and add help
topics to two XML files. Capabilities are boolean-gated method pairs
(`canRotate()/rotate()`, `canChange()/change()`, `hasTiming()/getDelay()`…)
defaulting to throw/noop, instead of composable interfaces. There are two
competing `Orientation` enums (`Gate.java:24` lowercase vs `JLSInfo.java:51`
uppercase), which blocks gates from using the shared `OrientationAttribute`
(`Attribute.java:220-243`) and forces duplicated parse loops. Rotation is
implemented everywhere as full teardown-and-rebuild (`inputs.clear();
width=0; init(g)` — `Gate.java:600-604`) because pins have no concept of a
face. `Element.changeTiming` branches on `instanceof Memory`
(`Element.java:650-653`) — the base class knows a subclass. `Element.intersects`
special-cases `Wire`/`WireEnd` (`Element.java:380-392`) — the root of the
hierarchy hardcodes two leaves.

The bright spots point at the fix: `Gate.Kind` (`Gate.java:57-78`) is a real
per-type descriptor (display name, save name, defaults) and the #23 attribute
registry already made persistence declarative. Neither was generalized.

**Verdict:** the hierarchy needs a *descriptor/registry* seam: one
`ElementDescriptor` per type owning tag, palette entry, icon, help topic, and
dialog factory; capability traits as interfaces; one `Orientation` enum; the
compiler (abstract methods) enforcing the authoring contract. This is also
the precondition for a real palette, for plugin elements (F4), and for the HDL
importer's mapping table (#61) to stay table-shaped.

### F4 (high) — The plugin mechanism is dead code with the wrong shape

Plugin discovery requires the literal path `JLS.jar` on the classpath
(`JLS.java:57-73`); the Maven build ships `jls-<version>.jar`, so **the entire
mechanism has been silently unreachable in every artifact this repository has
ever built**. Even when it ran, it was not an extension API: it loads exactly
one class named in one XML manifest and invokes its static
`config(args, exHandler)` (`JLS.java:129-140`) — replacing the launcher
wholesale. A plugin cannot add a palette element (hand-enumerated,
`SimpleEditor.java:869-1197`), cannot make its element loadable from a file
(package-locked, `Circuit.java:399`), and only the first manifest found is
honored (`JLS.java:119`). Open issue #46 currently proposes *hardening* this
code path (CWD bug, NPE, XML parser).

**Verdict:** decide before hardening. The defensible options are (a) delete
the mechanism outright (it has had zero possible users for years), or (b)
redesign it as an element-provider API on top of the F3 registry. Spending
effort making a dead, wrongly-shaped loader safer is the one clearly wrong
option, and it is the currently scheduled one.

### F5 (medium) — Distribution assumes a 2004 user: bare jar + your own JDK

The only install path is "download `jls-<version>.jar`, have JDK 17+, run
`java -jar`" (README.md:12-22). For the actual audience — students on
university-managed or personal machines — the JDK prerequisite is the
highest-friction step in the whole product, and it is outsourced to the user.
`jpackage` (in the JDK since 17) produces self-contained per-OS installers;
issue #8 deferred exactly this. No install-size, no double-click launch, no
file association for `.jls` files on any OS.

**Verdict:** treat installers as a release deliverable (#44's pipeline is the
natural place), not a nice-to-have. File association for `.jls` +
double-click-to-open is part of the same story (and depends on fixing #34).

### F6 (medium) — Printer-centric outputs in a file-centric world

The batch trace facility (`-r`) renders waveforms **only to a print service**
(`BatchSimulator.printTrace`, `BatchSimulator.java:174+`; `-p`/`-v` print the
schematic). There is no machine-readable waveform output at all — no VCD (the
universal standard every wave viewer reads), no CSV. Meanwhile the one
file-producing CLI feature, `-i` image export, is undocumented in `usage()`
(`JLSStart.java:479-490`), hardcodes lossy JPEG for line art, and offers no
output-path control (`name + ".jpg"`, `JLSStart.java:251`).

**Verdict:** invert the priority: files first (VCD/CSV trace export, PNG/SVG
schematic export with a path argument), printing as a legacy convenience with
a deprecation decision per #48's addendum.

### F7 (medium) — `JLSInfo` is a global mutable hub wired through everything

One static class carries the main frame, the running simulator, batch mode,
the load-error string, all semantic colors, all geometry constants, and the
version constants (`JLSInfo.java` throughout; ~640 references across the
tree, including 40 uses of `JLSInfo.frame` as dialog parent and 55 of
`JLSInfo.loadError`). Per-class "previous settings" statics
(`Constant.java:34-36`, `Gate.java:65-67`) hold creation state outside any
instance. This is the mechanism by which the GUI leaks into everything (F1),
errors go stale (#58), and tests interfere with each other.

**Verdict:** decompose along the seams that already have issues attached: a
per-load result object (#58 addendum), a per-run simulator handle (#49), a
theme/preferences object (U4/U8 below), and a build-derived version record
(#36). `JLSInfo` should end as geometry constants or disappear.

### F8 (low) — Applet-era vestiges still shape code and copy

`Tutorial.java` carries an `isDemo` "Applet version demo" parameter all four
call sites pass `false` (`Tutorial.java:24-28`); the tutorial text warns about
network loading delays from the applet days (`tutorial1.html:24-25`); the
execution help page still calls the artifact `JLS.jar`
(`execution/execution.html:34-51`). Dead framing misleads both users and
contributors about what the program is.

---

## Part 2 — Programmatic interfaces

### I1 (high) — The CLI misdocuments itself, three different ways

Real flags: `-h -b -i -r -p -v -s -d -t` (`JLSStart.java:330-457`). `usage()`
documents seven of them — `-i` (image export) and `-v` (print preview) are
absent (`JLSStart.java:479-490`). The help page documents eight (omits `-i`);
the README summarizes four. No single correct list exists anywhere.
Additional contract defects, beyond the crash bugs already tracked in #42:
usage shows only glued operands (`-sname`) while the parser also accepts
separated ones — but rejects any separated operand beginning with `-`
(`JLSStart.java:354-362`); `-d` claims "a positive integer" but accepts zero
and negatives (`JLSStart.java:433-439`); "invalid flag" doesn't name the flag
(`JLSStart.java:457`); errors print to stdout while usage prints to stderr.
The #42 addendum's exit-code/stream contract covers the crash-and-stream half;
the *self-documentation* half (usage generated from the parser's own table,
asserted by test) is new.

### I2 (high) — The grading interface is undocumented and unversioned

Batch mode is JLS's API for instructors: `-t` test vectors in, watched-element
values out. The test-vector format is defined only by `TestGen`'s parser; the
output format is whatever `printValue`/`printChangedValues` print
(`JLSStart.displayResults`, `JLSStart.java:285-316`); neither is documented in
README, help, or a spec; neither is covered by a golden test that a grader
could treat as a compatibility promise (the suite's goldens pin *simulation*
behavior, not the CLI text contract). Any refactor of those prints silently
breaks every grading script in existence.

### I3 (medium) — No waveform data export (see F6) — VCD is the missing verb

Duplicated here as an interface item because it has an exact, cheap spec: a
`-vcd <file>` batch flag emitting standard VCD for watched signals would make
JLS output consumable by GTKWave/Surfer and by autograders, and is a natural
sibling of the HDL program (#59-#63) whose external simulators all speak VCD.

### I4 (medium) — Element authoring API (see F3) — summarized as an interface

For a project that says "issues and PRs welcome," the de-facto element API is
the contributor-facing API, and it is currently ~16 touchpoints, half of which
fail only at runtime. Tracked as the F3 registry redesign.

### I5 (low) — `TellUser` exists but lost the adoption war

The codebase has a notification abstraction (`TellUser.note/warn/err`,
`src/jls/TellUser.java`) — and ~160-190 raw `JOptionPane` call sites across
20+ files that bypass it (`State.java` alone has ~38; census in Part 3, U9).
`TellUser` itself prints errors to **stdout**, violating the #42 stream
contract, and takes a boolean "popup" flag rather than knowing the runtime
mode. The right shape (one reporter, headless-aware, severity-typed) is
exactly what #42/#58 addenda specify; this finding is the inventory of what
must migrate to it.

### I6 (low) — Image export API details

`-i` writes `<name>.jpg` next to the input with no path/format control
(`JLSStart.java:250-255`); JPEG is the wrong codec for schematics (compression
artifacts on line art); there is no SVG option though the drawing layer is
plain Java2D (SVG via JFreeSVG/Batik or hand-rolled emitter is cheap). Also
undocumented (I1).

---

## Part 3 — Human interface: the editor and GUI

(Read of `SimpleEditor.java` 4119 lines, `Editor.java`, `JLSStart.java`,
`KeyPad.java`, `JLSInfo.java`.)

### U1 (high) — No zoom; fixed canvas

There is no zoom anywhere in the editor (no scale transform, no mouse-wheel
handling; the only `scaleFactor` in the tree is the waveform time axis,
`Trace.java:33`). The canvas is a fixed 1000×1000 square
(`JLSInfo.circuitsize`, `JLSInfo.java:27`) that grows only via a manual
"increase size by 10%" button (`SimpleEditor.java:218-226,330-335`) or menu
item. For a diagram editor this is the single largest usability ceiling: real
coursework circuits outgrow both the canvas and the fixed 100% view.

### U2 (high) — Keyboard-only operation is impossible; accessibility is zero

Grep-verified: no `AccessibleContext`, `setMnemonic`, `setDisplayedMnemonic`,
or `setLabelFor` anywhere in `src/`. Canvas key bindings only fire when the
pointer is over the canvas because focus is grabbed on `mouseEntered`
(`SimpleEditor.java:2362`) — focus literally follows the mouse. No element can
be selected, moved, rotated, or wired without a mouse. The main menu bar has
**no accelerators and no mnemonics at all** — File→New/Open/Save/Save As have
no Ctrl/Cmd+N/O/S (`JLSStart.java:635-746`); the only menu-bar shortcuts in
the app are F5/F7 on the simulator. Editing shortcuts exist but live
exclusively in the canvas popup (`SimpleEditor.java:464-508`), where a
menu-bar-browsing user never sees them. Rotate/flip have no binding at all —
the intended `R` accelerator is commented out (`SimpleEditor.java:483-484`).
`Ctrl/Cmd+W` is overloaded wire-vs-watch depending on selection state
(`SimpleEditor.java:535-591`); `Ctrl+Y` redo is wrong for macOS convention.

### U3 (high) — Color is the only channel, and the channel is red/green

Semantic state is communicated purely by hardcoded colors
(`JLSInfo.java:32-42`): **green** = connection touching, **red** = non-zero
wire value — the classic deuteranopia-indistinguishable pair — plus cyan =
watched over pink = highlighted. None are user-changeable (only grid and
background are, `JLSStart.java:864-891`, and those two reset every launch —
there is no preferences persistence anywhere; `java.util.prefs` grep = 0). No
shape/pattern/width redundancy exists for any signal.

### U4 (medium) — Platform integration deliberately discarded

Cross-platform Metal look-and-feel is forced (`JLSStart.java:499`), with
`System.exit(1)` if it can't be set (`:504`). This buys "same everywhere" at
the cost of native menu bar on macOS, native HiDPI scaling, native file
dialogs, and a 1998 visual identity. All dimensions are integer pixels
(12px grid, 32×32 toolbar buttons, 6px points — `JLSInfo.java:28-31`,
`SimpleEditor.java:1218-1219`), so on HiDPI displays the app renders tiny.
No dark mode (hardcoded white background, black foregrounds); the status
strip is hardcoded cyan and the hint bar yellow (`SimpleEditor.java:198,
2395-2424`) regardless of any user setting.

### U5 (medium) — Discovery model: everything important is hidden in hover and right-click

There is no Edit menu in the menu bar; cut/copy/paste/undo/select-all exist
only in transient context menus and unlabeled key bindings. Element placement
hints appear in a transient yellow status bar (`SimpleEditor.java:2397-2400`)
that the tutorial explains but nothing in the app points to. First-run is an
empty gray canvas and a bare "Enter circuit name (without .jls)" input dialog
(`JLSStart.java:976`) — no welcome content, no sample circuit, no pointer to
Help→Tutorial (see M-part).

### U6 (medium) — The one-class editor architecture is at its limit

`SimpleEditor.java` is 4119 lines: a 9-state hand-rolled mouse state machine
(`:392-393, 2385-2427`), all key bindings, a ~330-line hand-enumerated
toolbar factory (`:869-1197`), undo, clipboard, import management, and a
~305-line `if (event.getSource() == …)` dispatcher (`:1295-1600`) that has
already produced the #37 dead-code criticals. State is split between the
outer class and an inner `EditWindow` with an apologetic comment ("can't be
in EditWindow, but should be", `:392`). This is not a style complaint: the
mega-dispatcher pattern *caused* a critical bug, and every interaction fix in
the tracker (#35, #39, #43) pays a comprehension tax to this file.

### U7 (medium) — Undo: depth 10, whole-circuit snapshots, no visibility

Undo is 10 levels deep, hardcoded (`JLSInfo.undoStackDepth`,
`JLSInfo.java:44`), implemented as full save-format snapshots per edit
(`SimpleEditor.java:125-126, 1283`). No UI indication of undo availability or
depth; no preference. Gesture-state bugs are already tracked (#39); the
*capacity and cost* model is this separate, foundational choice.

### U8 (medium) — No preferences system at all

Nothing the user adjusts survives restart (colors, window size/split
position, last-opened directory, recent files — none exist as concepts).
There is no recent-files menu. Every session starts from scratch. A tiny
`java.util.prefs`/properties-file layer would carry: theme, colors, undo
depth, last directory, recent files, window geometry.

### U9 (low) — Dialog anarchy

~160 `JOptionPane` calls across 20 files with inconsistent parenting
(`getTopLevelAncestor()` vs `JLSInfo.frame` vs bare `null` — null-parented
dialogs can appear on the wrong monitor: `Editor.java:88,262`,
`JLSStart.java:868`), ad-hoc titles ("Error", "WARNING", "Option"), and
varying button sets for the same class of question. The shared `ElementDialog`
base (#26) covered element *property* dialogs only. Consolidation target =
the I5 reporter plus one confirm/prompt helper.

### U10 (low) — Gesture conflicts

`KeyPad`'s popup dismisses on a **global right-click hook**
(`KeyPad.java:130-141`), colliding with the editor's own
right-click-context-menu convention; subcircuit tabs disable the parent
editor while open (`SimpleEditor.java:3645-3647`), a modal coupling that
surprises users who expect tabbed independence.

### U11 (low) — Internationalization: none, but number handling is safe

All strings are inline English (no `ResourceBundle`; grep = 0). Radix-based
number parsing (`BigInteger(str, base)`) is locale-independent — correct for
this domain. i18n is a deliberate non-goal to *document*, not necessarily to
fix; the finding is that it is currently an accident, not a decision.

### U12 (low) — Status/feedback channels are nonstandard

Hints and wire values appear in two colored strips above the canvas
(`SimpleEditor.java:198-202`); there is no standard status bar, no cursor
changes per mode, no disabled-state explanation (menu items are enabled/
disabled without tooltips saying why).

---

## Part 4 — Materials (docs, help, tutorial, onboarding)

### M1 (high) — Shipped help content is broken and fails silently

Seven wiring-help pages embed images by the **pre-fork package path**
(`<img src=../../../edu/mtu/cs/jls/images/…>` — `const.html:11`, also
`extend/output/input/start/end/bundle.html`): broken since the
`edu.mtu.cs.jls` → `jls` rename, rendering as broken-image glyphs in every
copy shipped since. `keypad.html` references `down.gif`/`up.gif` where the
files are `down.GIF`/`up.GIF` — broken inside a jar. Three `Map.jhm` topics
point at files that don't exist (`editor/overview.html`,
`simulator/interactive/run.html`, `simulator/batch/run.html` — `Map.jhm:35,
82,83`), and the viewer **silently no-ops** on missing resources and dead
links (`Help.java:87-95, 182-186`), so none of this is ever reported. Six
shipped pages are orphans nothing links to (state-machine
`states/outputs/transitions.html`, `wires.html`, `keypad.html`,
`editor/circuits/overview.html`). `HelpTopicsTest` validates only
code-referenced topics and TOC→map consistency (`HelpTopicsTest.java:25-80`)
— it never checks map→file existence or inline `href`/`img` targets, which is
precisely why all of this passes CI green.

### M2 (high) — The About box is false

`About.java:27-33` + `JLSInfo.java:16-20` render "JLS Version 4.1.5 …
Copyright 2014 … Michigan Technological University … **All Rights
Reserved**" — wrong version, wrong year, wrong rights statement for a GPLv3
fork, no mention of the fork or its maintainer. Already covered mechanically
by #36 (version) and #40 (license story); listed here because it is also a
*materials* defect: the single most authoritative in-app statement about the
software is wrong on every line.

### M3 (medium) — Onboarding does not exist

From `java -jar` to first simulated circuit, the app offers: an empty canvas,
a bare name-input dialog (`JLSStart.java:976`), and — if the user finds it —
Help→Tutorial, which nothing surfaces. The README never mentions the tutorial.
There are no sample circuits, no empty-state hints, no "Getting Started"
prompt. The tutorial itself is competent but stale: applet-era "takes a while
over the network" copy (`tutorial1.html:24-25`), a fixed 400×500 dialog with
no navigation (`Tutorial.java:16-48`), and an orphaned 21 KB `tutorial.html`
master file shipped but unreachable.

### M4 (medium) — Reference documentation has structural holes

40 element help pages exist for 61 element classes — roughly a third of the
palette is undocumented. There is no file-format spec beyond the README's
(good) container-format section, no simulation-semantics document (delay
model, event ordering, tri-state resolution, bundle semantics — nothing
normative anywhere), and no architecture overview for contributors
(`docs/` contains one HDL research report; CONTRIBUTING/ARCHITECTURE do not
exist — tracked partially by #69). The strong materials that *do* exist
(SECURITY.md, the two audits, the HDL report) are not linked from README.

### M5 (medium) — Help delivery: in-jar HTML 3.2 with no search

The #11 viewer is a TOC tree + `JEditorPane` (HTML 3.2 rendering) with no
search, no index, no history (`Help.java:120-236`). Content is written down
to that renderer. The 2026-appropriate question is not "which Swing HTML
widget" but whether user docs should live as versioned web pages (searchable,
screenshot-friendly, linkable from README) with the in-app viewer reduced to
context-sensitive basics — one source of truth instead of the current three
(usage(), help HTML, README) that have already drifted apart (I1).

### M6 (low) — README serves contributors well, students partially

Honest, accurate build/run/format/license sections. Missing for the student
audience: any screenshot, any feature overview, any positioning versus
Logisim-evolution/Digital (the project has a 35 KB internal comparison in the
HDL report but no user-facing one), and any pointer to the tutorial. Stale
comment in `ci.yml:28` ("once they exist" — they exist) already tracked in #47.

---

## Part 5 — Verified sound (kept under radical doubt)

- **The product concept** — draw-first, simulate-in-place, batch-gradable —
  remains differentiated and pedagogically right; nothing found argues for
  abandoning it in favor of "just use Logisim."
- **The text save format's spirit**: line-oriented, diffable, XZ-compressed,
  sniffing loader with honest fallbacks (`FileAbstractor` is well-shaped and
  well-documented; atomic temp-file writes are correct). Fix identity and
  versioning (F2); keep the format.
- **The event-driven simulation core**: time+sequence deterministic ordering
  (`SimEvent.compareTo`), duplicate suppression, hook-based mode extension
  after #25 (`Simulator.runEventLoop` and its three hooks) — a clean core
  worth extracting, which is exactly why F1 matters.
- **`Gate.Kind` + the #23 attribute registry** — the correct descriptor
  pattern, already in-tree, waiting to be generalized (F3).
- **Platform-aware accelerators where they exist**
  (`getMenuShortcutKeyMaskEx`, `SimpleEditor.java:464+`) and radix-based
  number parsing (locale-safe).
- **The audit/security/research documents** as contributor materials — a
  genuinely unusual strength of this repository.

---

## Part 6 — Priorities

Ordered by the two-persona test (student's first ten minutes; grader's
script), cheapest-decisive-move first:

1. **Fix the broken materials now** (M1, M2): help images/paths/dead topics +
   a link-checking test that makes recurrence impossible; About box truth
   rides #36/#40. Days of work, immediately visible.
2. **Publish the contracts graders depend on** (I1, I2, I3): one true flag
   list generated/tested from the parser; documented test-vector and output
   formats pinned by golden tests; VCD export. This turns batch mode into an
   API.
3. **Onboarding floor** (M3, U5): first-run panel pointing at tutorial +
   sample circuits; menu-bar Edit menu + standard file accelerators (U2's
   cheap half).
4. **Platform & perception** (U4, U3, U1): system look-and-feel behind a
   flag, HiDPI-safe scaling, color-vision-safe palette with a second visual
   channel, zoom. Zoom is the largest single UX investment and the most
   requested class of fix for any diagram editor.
5. **Foundational refactors, in dependency order** (F1 → F3 → F4/F7):
   extract the headless core; generalize the descriptor/registry; then decide
   the plugin question from the registry vantage (and only then spend on #46
   or close it).
6. **Format stewardship** (F2): version header + spec + registry-owned tags,
   staged with the 4.3 release so the first shipped artifact writes versioned
   files.
7. **Distribution** (F5): jpackage installers + `.jls` association as a 4.3.x
   follow-on once #44's pipeline exists.

Items 1–3 are individually small; everything in them is testable with the
existing harness. Items 4–7 are programs, not patches, and belong in the
tracker as first-class issues with the same falsifiable framing as the #33
corpus.
