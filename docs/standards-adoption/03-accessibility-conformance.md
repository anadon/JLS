## Accessibility conformance: VPAT/ACR, Section 508, EN 301 549, WCAG 2.2 (#209-#215, #256-#258)

> **Numbering warning before anything else.** In `docs/standards-landscape.md`
> the numbers `#209`–`#215` and `#256`–`#258` are *registry entries in that
> document*, not GitHub issues. The collision is real and will bite: registry
> `#210` is EN 301 549, while **GitHub issue #210** is the component-naming
> scheme (`docs/component-naming.md:1`), and registry `#213` is the Java
> Accessibility API while `docs/standards-landscape.md:257` uses "#213
> follow-ups" to mean a GitHub issue about constraint emitters. Every
> cross-reference below says which namespace it means. The GitHub issues that
> actually own the existing evidence are **#75** (keyboard operability &
> accessibility), **#76** (color themes), **#91/#162** (the UI harness and its
> Xvfb substrate), **#153** (FlatLaf), and **#210** (component naming). A new
> GitHub issue must be opened for this work; it is called *the ACR issue*
> throughout.

---

### What conformance actually means

**The artifact.** The deliverable is a completed **VPAT® → Accessibility
Conformance Report (ACR)**. "VPAT" is the *blank template* published by the
Information Technology Industry Council (ITI); the *completed* document is an
"ACR". Getting that vocabulary right matters, because procurement offices ask
for "your VPAT" and reject documents that read as marketing.

Use the **VPAT 2.5 INT (International) Edition**. The 2.x family ships four
editions — WCAG, Revised Section 508, EN 301 549, and INT. INT contains all
three tables in one document, which is exactly what a university that buys
under both US and EU rules wants, and it costs no extra work: the three tables
share the WCAG rows.

> *Unverified external facts, check before authoring:* that 2.5 is still the
> current revision (ITI has revised the template roughly annually — 2.0 in
> 2017 through 2.4Rev in 2020 and 2.5 adding WCAG 2.2), and the exact 2.5
> publication date. Download the current template from ITI directly; do not
> reuse a copy found in a vendor's ACR.

**The three conformance targets, and their different WCAG baselines.** This
is the part everyone gets wrong. They are *not* the same bar:

| Table in the ACR | Normative source | WCAG version it incorporates | What JLS must fill in |
|---|---|---|---|
| WCAG table | W3C **WCAG 2.2** (Rec., 5 Oct 2023 — *date recalled, not verified*) | 2.2 Level A + AA (AAA optional; leave AAA "Not Evaluated") | Every SC, applied to non-web software per WCAG2ICT |
| Revised Section 508 table | US Access Board **Revised 508 Standards** (36 CFR Part 1194 App. A/C, effective 18 Jan 2018 — *date recalled, not verified*) | **WCAG 2.0** A+AA, incorporated by reference | Ch. 3 (302 Functional Performance Criteria), Ch. 5 (**501–504**, Software), Ch. 6 (**601–603**, Support Documentation and Services). Ch. 4 (Hardware) = Not Applicable |
| EN 301 549 table | ETSI/CEN/CENELEC **EN 301 549** | **WCAG 2.1** A+AA in V3.2.1 (2021-03) | Clause 5 (generic), **clause 11** (Software), clause 12 (documentation & support), clause 10 (non-web documents — the in-jar help) |

> *Unverified:* which EN 301 549 version is currently harmonised under the EU
> Web Accessibility Directive / European Accessibility Act. V3.2.1 (2021-03)
> is the long-standing harmonised text; a V4.x referencing WCAG 2.2 has been
> in progress. Author against the version in force on the report date and say
> which one in the report header.

**Why "desktop Swing application" changes the criteria set.** JLS ships no web
content and no mobile app. It is *non-web software*. Concretely:

1. **Four WCAG SC are formally exempt for non-web software.** Revised 508
   **E207.2** exempts, and EN 301 549 clause 11 notes as not applicable:
   **2.4.1 Bypass Blocks**, **2.4.5 Multiple Ways**, **3.2.3 Consistent
   Navigation**, **3.2.4 Consistent Identification** — all four are scoped to
   "sets of web pages". Mark these **Not Applicable** with that citation, not
   "Supports". *(Verify the clause letter E207.2 against the current Access
   Board text; the substance of the four-SC exemption is stable.)*
2. **WCAG2ICT is the interpretation layer.** The W3C Group Note *Guidance on
   Applying WCAG 2 to Non-Web ICT* substitutes "web page" → "non-web document
   or software", "user agent" → "platform software", and so on. Every "how did
   you read this SC?" question a reviewer asks is answered by citing WCAG2ICT.
   *(The WCAG 2.2-era WCAG2ICT Note publication date is unverified here —
   cite the version you actually used, by URL and date.)*
3. **4.1.1 Parsing is obsolete** in WCAG 2.2 (removed; always passes).
   *Recalled, not verified against the Recommendation — check before writing it
   into a rated row.* Do not
   write a remark for it beyond "Not Applicable (removed in WCAG 2.2)". It is
   still live in the 508 table (WCAG 2.0), where for Swing software it is
   vacuous — say so.
4. **The criteria that carry the real weight are not WCAG at all.** For
   software, the load-bearing requirements are **508 §502.3.1–502.3.14**
   (Accessibility Services: object information, values, label relationships,
   parent/child relationships, text, list of actions, focus and selection
   attributes, change notification) and their EN twins **11.5.2.1–11.5.2.17**,
   plus **502.2.1/502.2.2** (user control of / no disruption of accessibility
   features) ≡ **11.6.1/11.6.2**, **503.2 User Preferences** ≡ **11.7**, and
   **504 Authoring Tools** ≡ **11.8**. WCAG 4.1.2 Name/Role/Value is the WCAG
   shadow of the same thing. **This is where JLS's canvas gap lands.**
5. **Clauses that are wholesale Not Applicable**, and should be stated as such
   rather than left blank: EN 301 549 clause 6 (two-way voice), 7 (video),
   8 (hardware), 9 (web), 13 (relay/emergency); 508 Chapter 4 (hardware);
   all WCAG time-based-media SC (1.2.x), all audio SC (1.4.2, 1.4.7),
   3.3.7 Redundant Entry and 3.3.8/3.3.9 Accessible Authentication (JLS has no
   authentication and no multi-step forms), 2.2.x timing (no timeouts).

**What a conformance claim rests on.** For each row the ACR states one of
**Supports / Partially Supports / Does Not Support / Not Applicable**, plus a
free-text remark. There is no certificate, no auditor, no registry. The
*only* thing a reader can lean on is the "Evaluation Methods Used" field and
whatever evidence you point it at. For JLS the evidence must be:

- `docs/keyboard-a11y-verification.md` — behaviour → observable signal →
  test table for keyboard operability (WCAG 2.1.1/2.1.2/2.4.3/2.4.7/2.1.4),
  including the *falsification* record ("red-on-break evidence, re-runnable").
- `docs/component-naming.md` — the naming/`labelFor`/accessible-name scheme
  that underwrites 1.3.1, 3.3.2, 4.1.2, 502.3.
- The named tests: `test/jls/ui/MenuMnemonicAndAccessibleNameTest.java`,
  `PaletteButtonAccessibilityTest.java`, `KeyPadAccessibilityTest.java`,
  `test/jls/KeyPadAccessibilityPinTest.java`,
  `test/jls/ui/ComponentIdentityTest.java`,
  `test/jls/ui/FocusFaithfulKeyboardTest.java`,
  `KeyboardEditingFaithfulTest.java`, `MenuAcceleratorFiringTest.java`,
  `TabSelectionFocusTest.java`, `test/jls/ThemeTest.java`,
  `test/jls/elem/WireValueChannelTest.java`.
- New artifacts this project creates (below): a golden accessible-tree dump,
  a contrast test, and a manual AT audit log.

**What is explicitly *not* claimed.** JLS's ACR must not claim conformance
for: the `.jls` file format, the batch/CLI surface (a CLI is not exempt but is
covered by the platform terminal, and 508/EN treat it as software with
platform-provided accessibility — rate the batch surface's `jls: error:` line
contract under 1.4.1/3.3.1 and otherwise Not Applicable), the container image,
or any web property. The report covers **the GUI editor and simulator of a
named version at a named commit, on named platforms.**

---

### Implementation procedure

The order matters: **fix the two defects that would force a "Does Not
Support" on the most-scrutinised rows before authoring the report**, then do
the gap analysis, then the AT audit, then write.

#### 1. Fix the bundled-runtime accessibility-bridge gap (highest impact, ~1 day)

This is the single most consequential finding in the tree and it is a
one-line change.

`scripts/build-installer.sh:143-146` derives the jlink module set from the
shaded jar:

```sh
MODULES="$(jdeps --print-module-deps --ignore-missing-deps "$JAR")"
```

`jdeps` reports *static* dependencies. **`jdk.accessibility` — the module that
carries the Java Access Bridge on Windows — can never appear**, because
nothing in the jar references it. Consequence: every `.msi` produced by the
release pipeline bundles a runtime with no Java Access Bridge, so **NVDA and
JAWS get nothing at all from an installed JLS**. Only `java -jar` on a full
JDK works. An ACR authored today would have to rate 508 §502.3.\*, EN 11.5.2.\*
and WCAG 4.1.2 as **Does Not Support** *for the primary Windows distribution*.

Note the platform asymmetry, because the fix differs per OS:

- **Windows:** Swing does **not** expose UI Automation. NVDA/JAWS read Swing
  through the **Java Access Bridge** (`jdk.accessibility`), which presents an
  IAccessible2-shaped API. Fix: append `jdk.accessibility` to `MODULES`
  unconditionally, and ship the bridge enabled. `jabswitch -enable` writes a
  per-user `.accessibility.properties`; for a bundled runtime, write
  `assistive_technologies=com.sun.java.accessibility.AccessBridge` into the
  jlink image's `conf/accessibility.properties` in the same build step that
  runs `clamp_mtimes` (`scripts/build-installer.sh:129-131`), so the mtime
  clamp still covers it and installer reproducibility posture is unchanged.
- **macOS:** the Cocoa peer in `java.desktop` implements NSAccessibility from
  `AccessibleContext` directly. No extra module. VoiceOver works out of the
  box; quality is the open question, not availability.
- **Linux:** Orca reads AT-SPI2, and the Java→ATK→AT-SPI2 bridge is
  **java-atk-wrapper (JAW)**, which is *not* part of the JDK (Debian:
  `libatk-wrapper-java`, `libatk-wrapper-java-jni`) and is thinly maintained.
  A jlink'd bundled runtime has its own `conf/` and cannot pick up the
  distro's wrapper. **Recommendation: do not attempt to bundle JAW.** Instead
  (a) add `Recommends: libatk-wrapper-java-jni` to the deb control and the rpm
  equivalent via the existing `--resource-dir` override
  (`scripts/build-installer.sh:351`), and (b) document, in README and the ACR
  remarks, that Linux screen-reader users should run the jar on a system JDK
  with `libatk-wrapper-java-jni` installed. That is an honest "Partially
  Supports" with a documented path, not a hidden failure.

**Pin it:** new `test/jls/AccessBridgeModuleTest.java` (headless), reading
`scripts/build-installer.sh` from the repo tree and asserting `jdk.accessibility`
is in the module set and that the accessibility-properties step exists — the
same source-grep compensating-control pattern already used by
`test/jls/KeyPadAccessibilityPinTest.java` and `MenuAcceleratorPolicyTest`.

**No stability contract is touched.** Module set, installer internals, and
GUI wiring are all outside `docs/batch-interface.md` and `docs/file-format.md`.
`HeadlessCoreRatchetTest` is unaffected — every change here is GUI- or
packaging-side and nothing new is imported into `jls.core` candidates
(`jls.sim.*`, `jls.Circuit` and collaborators, `jls.elem.*`).

#### 2. Fix the contrast defects (~2 days)

Two are provable from the tree today, with numbers.

**(a) The keyboard focus caret is effectively invisible.**
`src/jls/edit/SimpleEditor.java:2392-2404` draws the #75 keyboard-construction
caret in `JLSInfo.Palette.selectionColor`. In `Theme.DEFAULT`
(`src/jls/Theme.java:60,66`) that is `(240,240,240)` — the *same value as the
grid* — on a `Color.white` background. WCAG relative-luminance contrast:
**≈1.14:1**, against the 3:1 required by **1.4.11 Non-text Contrast** for
focus/UI indicators. The keyboard-accessibility feature the whole #75 story
rests on has an indicator a low-vision user cannot see.

**(b) Two semantic wire colors fail 1.4.11 against the white canvas.**
Computed from the sRGB values in `src/jls/Theme.java` with the WCAG
relative-luminance formula (my computation; the new test below is the
authority):

| Role | Current | Contrast vs `Color.white` | 1.4.11 (3:1) | Suggested replacement (stays in Okabe-Ito) | New ratio |
|---|---|---|---|---|---|
| `touch` | `#0072B2` | 5.19:1 | pass | — | — |
| `wireOff` | `#707070` | 4.96:1 | pass | — | — |
| `nonZero` | `#E69F00` | **2.10:1** | **fail** | `#D55E00` (vermillion) | 3.87:1 |
| `watch` | `#56B4E9` | **2.31:1** | **fail** | `#009E73` (bluish green) | 3.42:1 |
| `highlight` | `#B8A5E3` | ~1.9:1 | **fail** | needs a darker lavender | — |
| `selection` / caret | `(240,240,240)` | **1.14:1** | **fail** | see below | — |

The constraint is *joint*: `test/jls/ThemeTest.java` already enforces ≥25
CIE76 ΔE between every pair of wire states under normal, deuteranopic and
protanopic vision (`DISTINGUISHABLE = 25.0`). Any recolor must keep that green
while adding the ≥3:1 floor. Do the two together, in one PR, with both tests
in the same run.

Design decisions, with recommendations:

- **Change `Theme.DEFAULT`, never `Theme.CLASSIC`.** `ThemeTest.
  classicReproducesTheLegacyPalette` pins CLASSIC byte-for-byte to the
  pre-#76 palette and that promise ("no user is forced off the old colors")
  should stand. CLASSIC will fail the new contrast test — **exempt it
  explicitly** in the test, and say in the ACR that the shipped *default*
  conforms while a legacy-compatibility theme is offered under EN 11.7 / 508
  503.2 user preferences.
- **Do not add a separate "high contrast" theme as the fix.** A conforming
  non-default theme does not make the default conform, and 1.4.11 is judged on
  what the user gets out of the box.
- **Caret: stop reusing `selectionColor`.** Add a dedicated `focus` role to
  the `Theme` record and paint the caret in it. This is a record component
  addition — `Theme` is a `record` with a canonical constructor used by two
  literals in the same file plus `UserPrefs` lookups by name; adding a
  component is a source-compatible change inside the project (no public API
  contract exists for `Theme`). Recommended value: `Color.black` or the
  existing `touch` blue, both ≥5:1.
- **Also add a component-level focus indicator for the canvas.** The canvas is
  `setFocusable(true)` (`SimpleEditor.java:1187`) but there is **no
  `FocusListener` and no `hasFocus()`-conditional painting anywhere in
  `SimpleEditor`** — grep confirms exactly one focus-related call site in
  5,712 lines. When the canvas holds focus, nothing on screen says so, which
  is **2.4.7 Focus Visible** and **2.4.11 Focus Not Obscured (Minimum)**
  territory. Draw a 2px inset border in the new `focus` role on
  `focusGained`/`focusLost`.

#### 3. Fix the documentation accessibility defects (~1 day)

Measured in the tree: `resources/help/**` contains **83 HTML files**, **10
`<img>` tags, zero with `alt=`, and zero files with any `lang=` attribute**.
That is a straight **1.1.1 Non-text Content** and **3.1.1 Language of Page**
failure in the product documentation — which is in scope as EN 301 549
**clause 10** (non-web documents) / **12.1.2** and 508 **602.2**.

- Add `alt` to all ten images (`resources/help/elements/wiring/*.html`,
  `resources/help/elements/keypad.html`). The palette icons are element
  symbols; the alt text is the element name, which is already the accessible
  name set by `SimpleEditor.makeElement` — reuse the same string so they can
  never desync.
- Add `<html lang="en">` to all 83 files. This does not violate the recorded
  i18n non-goal (`ARCHITECTURE.md:238`): declaring the language you *are* in
  is not internationalisation.
- Keep the HTML 3.2 constraint from `ARCHITECTURE.md:252` — `alt` and `lang`
  are both HTML 3.2-era attributes and `javax.swing.JEditorPane` ignores what
  it does not understand, so the in-app viewer is unaffected.

Also required by **508 §602.3** and **EN 12.1.1**: the documentation must
*list and explain how to use* the product's accessibility features. Create
`resources/help/overview/accessibility.html` — **but check the layout first**:
`resources/help/overview.html` is a top-level *file* today, and the help tree's
only subdirectories are `editor/`, `elements/`, `execution/`, `images/`,
`menus/` and `simulator/`. Either add the page at the top level next to
`overview.html` or put it under an existing subdirectory; do not create an
`overview/` directory that shadows the existing file name. Wire it into
`Map.jhm` and
`JLSHelpTOC.xml` (both already integrity-checked by
`test/jls/HelpTopicsTest.java`), covering: full keyboard construction and the
hot-key table, mnemonics and accelerators, the theme chooser, zoom, the
component naming scheme, the known canvas limitation, and the per-platform
screen-reader setup notes from step 1. Mirror a short version into README.

#### 4. Close or disclose the interaction gaps (~3 days if closed)

- **2.5.7 Dragging Movements (AA, new in WCAG 2.2).** Palette placement is
  already click-then-click (`test/jls/ui/PaletteDropTest.java` synthesises a
  click on the palette entry, then the element lands at the click location) —
  that leg **Supports**. But *moving an existing selection* and *drawing a
  wire with the pointer* are drag gestures (`SimpleEditor.java:2515-2520`,
  `:3358`). Keyboard alternatives exist (arrow-nudge, `W` to start a wire) but
  **a keyboard alternative does not satisfy 2.5.7** — the SC requires a
  *single-pointer, non-dragging* path. Recommendation: add a click-to-pick /
  click-to-drop mode for move and a click-endpoint / click-endpoint mode for
  wires, reusing the existing caret state machine, or rate **Partially
  Supports** with an accurate remark. Closing it is the better answer and is
  ~1.5 days on top of the existing keyboard state machine.
- **EN 301 549 clause 5.9 Simultaneous User Actions.** Shift+click multi-select
  and space-drag panning (`SimpleEditor.java:1447-1461`, `:2470`) are
  simultaneous actions. Confirm during gap analysis that each has a
  sequential alternative; if multi-select has no keyboard-only path beyond
  Ctrl+A, that is a real finding.
- **The trace window is mouse-only.** `src/jls/edit/Trace.java:20` declares
  `class Trace extends JPanel implements MouseListener, MouseMotionListener` —
  no key bindings, no `setFocusable`, no accessible wiring, in 626 lines. The
  interactive simulator's trace display is therefore **not keyboard operable**
  (**2.1.1 Keyboard**, Level **A**) and exposes no accessible objects. This is
  the second-largest gap after the canvas and, unlike the canvas, it is
  cheaply fixable: the trace is a list of rows with sampled values. Make
  `Trace` focusable, add arrow-key row/time navigation, and expose each row's
  current value through `getAccessibleContext().setAccessibleName/Description`.
  ~2 days. If not done, rate 2.1.1 **Partially Supports** and name the trace
  window in the remark — do **not** rate 2.1.1 "Supports" on the strength of
  the editor alone.
- **SVG export has no text alternative.** `src/jls/edit/CircuitRenderer.java:
  314-358` emits SVG through JFreeSVG with no `<title>`/`<desc>`. Under 508
  §504.2 / EN **11.8.2** (authoring tools producing content), post-process
  `svg.getSVGDocument()` to inject `<title>` (circuit name) and `<desc>`
  (element and pin inventory). This changes `test/jls/SvgExportTest.java`
  expectations — but **SVG is not a documented stability contract**: grep
  confirms `docs/batch-interface.md` contains no SVG clause; it normatively
  covers the `-t` grammar, the watched-element output format, and the VCD
  profile only. So this is a golden update, not a deviation.
  `SvgExportTest.exportingTwiceIsByteIdentical` must stay green — inject
  deterministically, before serialisation. **Note for sequencing:** the
  IEC/IEEE symbol item (§01 of this playbook) proposes committing SVG goldens
  under `test/resources/symbols/`; this injection changes their bytes. Land
  this change first, or expect to regenerate those goldens.

#### 5. Gap-analysis procedure (~2 days)

Do this as a table in a working file, then transcribe. For every criterion in
the union of {WCAG 2.2 A+AA, 508 Ch.3/5/6, EN clauses 5/10/11/12}:

1. **Scope it.** Which of five surfaces does it touch — (i) Swing chrome
   (menus, toolbar, dialogs, help viewer), (ii) the editor canvas, (iii) the
   interactive simulator + trace window, (iv) in-jar help and repo docs,
   (v) the CLI/batch surface.
2. **Find the evidence.** Cite a test class, a doc row, or an AT session log.
   A criterion with no evidence is **Not Evaluated**, and Not Evaluated is
   only permitted for AAA — so every A/AA row needs a real answer.
3. **Rate conservatively.** If *any* in-scope surface fails, the product-level
   rating is at best **Partially Supports**. If no surface conforms, **Does
   Not Support**. Resist "Supports with exceptions" — that is not a valid
   VPAT value and reviewers treat it as evasion.
4. **Write the remark in the form "what works / what does not / where".**
   A remark that names the specific component is worth ten that say "generally
   conformant".

Priority criteria that need new work rather than transcription: **1.4.3**
(chrome text over the hardcoded `setBackground` colors audited in
`docs/flatlaf-evaluation-2026-07.md` §(b)), **1.4.11** (step 2), **1.4.4 /
1.4.10** (resize text / reflow at 200% — the canvas zoom ladder is
1.0/1.5/2.0 per `test/jls/ui/EditorZoomTest.java:110-116`, but that zooms the
*drawing*, not the UI chrome text; chrome scaling comes from FlatLaf HiDPI /
`-Dsun.java2d.uiScale`, and must be tested, not assumed), **2.1.1** (trace
window), **2.4.7 / 2.4.11** (canvas focus ring), **2.5.7** (dragging),
**2.5.8 Target Size** (palette icons measure exactly 24×24 px — verified with
`struct.unpack` over the GIF headers; the icons `PaletteEntry` actually
resolves live in **`src/jls/edit/images/`** (33 GIFs, `PaletteEntry.java:34`),
not `src/jls/images/`, which is a 34-file duplicate no Java source
references — so button bounds
with FlatLaf insets exceed 24×24 and this should pass; measure it, do not
assume), **3.3.2 Labels or Instructions** (covered by
`ElementFormDialog.labelled(...)`'s `setLabelFor`, `docs/component-naming.md:
41-48`), **4.1.2 / 502.3.\*** (the canvas), and **502.2.2 No Disruption**
(does JLS's FlatLaf install or key handling disrupt platform a11y? test it).

#### 6. The canvas disclosure — how to write it

`docs/keyboard-a11y-verification.md:123-127` already states the residual
honestly: *"Elements drawn on the editor canvas are not exposed as individual
accessible objects to a screen reader (the canvas is a single custom-painted
component)."* The ACR must carry that forward without softening it.

**Ratings.** The correct product-level rating is **Partially Supports**, not
"Does Not Support", for the criteria below — because the Swing chrome (menu
bar, palette, ~30 element dialogs, keypad) genuinely does expose names, roles,
label relationships and focus, and that is a real part of the product. Reserve
"Does Not Support" for a criterion where *nothing* in the product conforms.

| Criterion | Rating | Why |
|---|---|---|
| WCAG 1.1.1 Non-text Content | Partially Supports | icon-only controls named; the circuit drawing has no text alternative |
| WCAG 1.3.1 Info and Relationships | Partially Supports | dialogs use `labelFor`; canvas topology is visual-only |
| WCAG 2.4.7 Focus Visible | Partially Supports → Supports after step 2 | chrome yes; canvas indicator invisible/absent today |
| WCAG 4.1.2 Name, Role, Value | Partially Supports | chrome exposes all three; canvas children expose none |
| 508 502.3.1–502.3.14 / EN 11.5.2.\* | Partially Supports | same split, per service |
| 508 502.3.6 Relationships / EN 11.5.2.7 | Partially Supports | container relationships in chrome; no `AccessibleRelation` between circuit elements |

**Verbatim remark to reuse (adapt the version/commit):**

> The application's menus, toolbar, element dialogs, keypad, and help viewer
> are standard Swing components and expose accessible name, role, state, and
> label relationships through the Java Accessibility API; these are verified
> by automated tests on the running application (see Evaluation Methods).
> The circuit editing canvas is a single custom-painted component: the gates,
> wires, and other elements drawn on it are **not** exposed as individual
> accessible objects, so a screen reader can report that the canvas has focus
> but cannot enumerate, name, or describe the circuit's contents or the
> connections between them. All editing operations on the canvas are fully
> keyboard operable, and the keyboard construction path is separately verified
> against the live focus owner. A user who cannot see the canvas can therefore
> operate the editor but cannot read the circuit back from it. This is a known
> limitation, documented at
> `docs/keyboard-a11y-verification.md` ("Deliberately out of scope /
> deferred"), and is not scheduled for a specific release.

**Could the gap be closed with the Java Accessibility API, and at what cost?**
Technically yes. The shape:

1. Override `getAccessibleContext()` on the canvas component (the inner class
   around `SimpleEditor.java:1029-1187`) with an `AccessibleJComponent`
   subclass implementing `getAccessibleChildrenCount()` /
   `getAccessibleChild(int)` over the circuit's elements and wires.
2. One lightweight `Accessible` proxy per `Element`/`Wire`, implementing
   `AccessibleContext` (`getAccessibleRole()`, name = element name + type,
   description = current value and connections) and `AccessibleComponent`
   (bounds, `getLocationOnScreen`) so AT can hit-test and highlight.
3. `AccessibleSelection` on the canvas, mirroring the editor's selection, and
   `firePropertyChange(ACCESSIBLE_SELECTION_PROPERTY / ACCESSIBLE_STATE_PROPERTY)`
   on model changes — routed off the draw hot path, which
   `proofs/SpatialIndexCorrectness.agda` and `DrawCullingParityTest` guard.
4. Netlist topology via `AccessibleRelation.CONTROLLER_FOR` / `CONTROLLED_BY`
   between driving outputs and driven inputs — the JAAPI has exactly these
   constants, and this is the honest mapping for a schematic.
5. A screen-reader navigation model (next/previous element, follow connection)
   layered on the existing #75 caret.

Realistic cost: **8–15 maintainer-days** for a credible v1 — the proxy layer
is 400–700 lines, but events, caching against the draw path, and testing
dominate — plus permanent maintenance on every new element type, plus a
genuine risk that java-atk-wrapper surfaces none of it to Orca.

**Recommendation: do not build it as part of this project.** Instead, build
the cheap equivalent that works with all three AT stacks *today*: a
**keyboard-reachable circuit outline view** — a `JTree` or `JList` of
elements, their names, their values, and their connections, shown beside the
canvas. Because it is a stock Swing component, it is accessible for free on
Windows, macOS and Linux with no bridge-specific work. ~3–4 days. It converts
1.1.1 / 1.3.1 / 4.1.2 from "the canvas is opaque" to "an equivalent,
accessible representation of the same information is provided", which
Revised 508 **E101.2 Equivalent Facilitation** explicitly contemplates. Gate
the full JAAPI canvas tree on an actual user request.

#### 7. Migration and compatibility

- **Saved files:** untouched. No change in this project reads or writes `.jls`.
- **Batch interface:** untouched. No `-t`, watched-element, or VCD change.
  The only batch-adjacent change is SVG `<title>`/`<desc>` (§4), which is not
  a documented contract.
- **Existing users:** the visible changes are (a) two default wire colors and
  the caret/focus colors, (b) a canvas focus ring, (c) optional new
  interaction modes, (d) `Theme.CLASSIC` still available unchanged for anyone
  who wants the old look — announce all four in `CHANGELOG.md`.
- **Recorded decision:** add an `ARCHITECTURE.md` "Recorded decisions" entry —
  *"Accessibility conformance: self-asserted ACR, canvas scene model
  deliberately not exposed"* — with the revisit trigger being a concrete
  request from an instructor or a procurement office, mirroring the form of
  the existing i18n and look-and-feel entries.

---

### Testing procedure

The rule: **an ACR row that is not backed by either a test that goes red on
regression or a dated AT session log is not evidence.** Everything below is
new work unless marked otherwise.

#### Automatable, headless, joins plain `mvn verify`

- **`test/jls/ThemeContrastTest.java`** (to be created). Implements the WCAG
  relative-luminance and contrast-ratio functions and asserts, for every role
  in `Theme.DEFAULT`, ≥3:1 against `background()`; asserts the new `focus`
  role ≥3:1; explicitly exempts `Theme.CLASSIC` with a comment naming the
  legacy-compatibility promise in `Theme`'s javadoc. **Red today** at 2.10:1
  (`nonZero`), 2.31:1 (`watch`), 1.14:1 (`selection`/caret). Pair it with the
  existing `test/jls/ThemeTest.java` ΔE≥25 assertions so the joint constraint
  is enforced in one run. *Regression that turns it red:* any palette edit
  that lightens a semantic color, or a revert of the caret color change.
- **`test/jls/HelpAccessibilityTest.java`** (to be created). Walks
  `resources/help/**.html` (the tree-reading idiom already used by
  `test/jls/HelpTopicsTest.java`) and asserts: every `<img>` has a non-empty
  `alt`; every file declares `lang`; every file has exactly one `<h1>`; the
  accessibility help topic exists and is TOC-reachable. **Red today**: 10
  images with zero `alt`, 83 files with zero `lang`. *Regression:* a new help
  page or screenshot added without alt/lang.
- **`test/jls/AccessBridgeModuleTest.java`** (to be created). Source-grep pin
  on `scripts/build-installer.sh` for `jdk.accessibility` and the
  `accessibility.properties` write — same compensating-control pattern as
  `test/jls/KeyPadAccessibilityPinTest.java`. *Regression:* someone
  "simplifies" the module list back to the bare `jdeps` output.
- **`test/jls/SvgExportTest.java`** (exists) gains a `titleAndDescAreEmitted`
  leg, keeping `exportingTwiceIsByteIdentical` green.

#### Automatable, display-tagged (`@Tag("display")`, #162 substrate)

Run with the authoritative bar already documented in
`docs/keyboard-a11y-verification.md`:

```
xvfb-run -a mvn -B verify -Djls.test.headless=false
```

- **`test/jls/ui/AccessibleTreeGoldenTest.java`** (to be created) — *the
  single most valuable artifact in this project.* Boot `JLSStart` plus one
  representative element dialog, walk the component tree depth-first, and emit
  one canonical line per component: `depth | getName() | AccessibleRole |
  AccessibleName | focusable`. Compare byte-exact against
  **`test/resources/a11y/accessible-tree.txt`** (to be created), the house
  golden-file style used by `BatchSimulationGoldenTest` /
  `VcdExportGoldenTest`. This test is simultaneously the regression guard and
  **the evidence appendix the ACR cites** — a reviewer can read exactly what
  the AT sees. *Regression:* any lost accessible name, changed role, or newly
  unfocusable control shows as a one-line diff. Note the ordering hazard:
  serialise deterministically (component order, no hash-set iteration), the
  same discipline `DeterministicSaveTest` and `SvgExportTest` already impose.
- **`test/jls/ui/AccessibleNameCoverageTest.java`** (to be created) — the
  ratchet form of the existing spot checks. Over every window the app boots
  *and* every dialog in the `DialogConstructionSmokeTest` sweep (whose
  completeness is already ratcheted by
  `test/jls/ui/DialogCoverageRatchetTest.java`), assert every focusable
  component has a non-blank `getAccessibleContext().getAccessibleName()` and a
  non-null role. This generalises `PaletteButtonAccessibilityTest` and
  `KeyPadAccessibilityTest` from named components to *all* of them, so a new
  icon-only button cannot ship nameless.
- **`test/jls/ui/CanvasFocusVisibleTest.java`** (to be created) — render the
  canvas focused and unfocused via the existing `test/jls/ui/RenderAssert.java`
  helper, assert the images differ and that the indicator's measured contrast
  against the background is ≥3:1.
- **`test/jls/ui/TargetSizeTest.java`** (to be created) — every palette
  button's `getSize()` is ≥24×24 (2.5.8). Should pass on day one; pin it.
- **`test/jls/ui/TraceKeyboardTest.java`** (to be created, only if §4's trace
  work is done) — focus-faithful, using
  `test/jls/ui/EditorGestureSupport.pressKeyThroughFocusOwner`, asserting
  arrow keys move the trace row/time cursor and that the row exposes its value
  as an accessible description. Follow the doc's own warning: dispatch to the
  live focus owner, never `canvas.dispatchEvent`.
- **`test/jls/ui/NoDragRequiredTest.java`** (to be created, only if §4's
  2.5.7 work is done) — move and wire a circuit using only press/release
  pairs at distinct points, no intermediate drag events.

Update `docs/keyboard-a11y-verification.md`'s "Behavior → observable signal →
faithful test" table with every new row, and update its "Deliberately out of
scope" section so the canvas residual and any un-closed gap stay stated in
one place. That file is the repo's contract for this feature area.

#### External-tool validation with skip-when-absent

The house pattern is `iverilog`/`ghdl`/`yosys` (`README.md:223-231`, the
"Arm the HDL toolchain (best-effort)" steps in `.github/workflows/ci.yml`).
The accessibility analogue is `accerciser` / `pyatspi` on Linux: a test could
assert that a booted JLS appears on the AT-SPI2 bus with a non-empty
accessible tree, skipping cleanly via `Assumptions.assumeTrue` when the bus,
`java-atk-wrapper`, or the Python bindings are absent.

**Recommendation: do not add this to CI.** It requires a session D-Bus, the
GNOME GSettings schemas (the `gui-wayland` lane already had to install
`gsettings-desktop-schemas` to stop JBR failing to map a window), and JAW
itself. The `gui-wayland` lane's own history — twenty runs to earn promotion,
a `UNVERIFIED-PLACEHOLDER` checksum still in `ci.yml`, and a `PIXEL_DIFF_MIN`
still parked at `0` — is the honest cost estimate for a fragile GUI lane. A
red-for-substrate-reasons a11y lane would erode the value of a green build.

#### CI lane changes

- **`.github/workflows/ci.yml`: no new job.** The headless tests join the
  default surefire execution; the display-tagged ones join the existing
  `display-tests` execution (`pom.xml:275-289`) that already runs under
  `xvfb-run` on the Linux `build` job and, advisory, on the Windows lane.
- **Coverage ratchet:** new display-only tests measure on the xvfb run, not
  the headless one. Per `CONTRIBUTING.md`, floors are raised **from headless
  numbers on JDK 25 only** — so do not raise floors from these.
- **`.github/workflows/release.yml`:** add the rendered ACR to the `files:`
  list at `:111-114`. `fail_on_unmatched_files: true` is set, so the file must
  exist in `target/` at release time — generate it in the release job or check
  in a built copy.

#### Property / fuzz opportunities

- Property-test the contrast function itself (round-trip against a table of
  known WCAG reference pairs, e.g. `#767676` on white = 4.54:1) so a bug in
  the *checker* cannot silently pass a failing palette.
- Fuzz candidate palettes against the joint constraint (ΔE≥25 under three
  vision models **and** ≥3:1 against background) to find a conforming
  Okabe-Ito-derived set mechanically instead of by hand.

#### What only a human can do

No test replaces these. They belong in a new
**`docs/accessibility-at-checklist.md`** (to be created), written in the style
of the existing `docs/wayland-desktop-checklist.md` — a scripted,
once-per-release manual spot-check with a recorded result, precisely because a
headless rig can diverge from the real thing:

| Platform | AT | Bridge | Setup |
|---|---|---|---|
| Linux (GNOME/Mutter, X11 and Wayland) | **Orca** | AT-SPI2 via java-atk-wrapper | `apt install libatk-wrapper-java-jni orca accerciser`; `assistive_technologies=org.GNOME.Accessibility.AtkWrapper` in the runtime's `conf/accessibility.properties` |
| Windows 11 | **NVDA** (free) and JAWS if available | **Java Access Bridge** — *not* UIA | `jabswitch -enable`, or the bundled properties file from §1 |
| macOS 14+ | **VoiceOver** | built into `java.desktop`'s Cocoa peer | none |

Per platform, per release, record: does the menu bar read; do palette buttons
announce element names; do dialog fields announce their labels; does focus
move audibly through the tab order; what does the canvas announce (expected:
the component, not its contents); does the help viewer read; and the exact AT
and OS versions. **That log is the "Evaluation Methods Used" field of the
ACR.** An ACR written from unit tests with no screen-reader session is the
failure mode this whole item exists to avoid.

---

### Certification / conformance procedure

**Who assesses it: nobody. This is a self-assertion, full stop.** There is no
accredited body, no registry, no certificate, no mark, no listing, no
application, no fee, and no expiry. ITI publishes the template and does not
review, approve, validate, or publish completed reports. Anyone claiming to
"certify" a VPAT is selling an audit, not a certification.

**What a credible self-assertion consists of.** In descending order of what a
university accessibility reviewer actually checks:

1. **Named, dated, version-scoped.** Product name, exact version *and commit
   SHA*, report date, and the platforms/OS versions evaluated. JLS already has
   `VersionIdentityTest` and reproducible builds — say "JLS 5.x.y, commit
   `<sha>`, jar SHA-256 `<hash>`", which is a stronger identity statement
   than most commercial ACRs carry.
2. **A real "Evaluation Methods Used" paragraph.** Name the automated suite
   (`mvn verify` plus the xvfb display run), the AT versions used per
   platform, and the manual checklist. Do not write "internal testing".
3. **"Partially Supports" used liberally and "Does Not Support" used at
   least once.** An all-"Supports" ACR for a schematic editor with a
   custom-painted canvas is not credible and will be treated as such.
4. **Public, versioned, permanently linkable evidence.** Link
   `docs/keyboard-a11y-verification.md`, `docs/component-naming.md`, the
   accessible-tree golden, and the AT checklist by URL.
5. **A stated feedback path.** EN 301 549 clause 12.2 / 508 §603 expect a
   support channel; the GitHub issue tracker plus the `SECURITY.md`-style
   contact convention satisfies this — say so explicitly and label a11y
   issues.

**Signature and liability.** VPAT 2.x has no signature block; it has author /
organisation / contact fields. The maintainer's name and the report date are
the signature in practice.

- There is **no contract** between this project and any university. JLS is
  distributed free under GPLv3; GPLv3 §15–§16 disclaim warranty for the
  software. An ACR is a *factual statement about* the software, not a warranty
  term, so the disclaimer does not launder an inaccurate claim — but with no
  consideration, no procurement contract, and no representation made to induce
  a purchase, the realistic exposure is **reputational**, not legal.
- The theoretical legal vector — a knowingly false accessibility
  representation made to a **US federal agency** in a procurement, implicating
  the False Claims Act (31 U.S.C. §3729) — does not attach here because there
  is no federal contract. *This is a plain-English reading of the structure,
  not legal advice; I have not verified any case law on VPAT-based FCA
  claims.*
- Add a scope line to the report: *"This is a good-faith self-assessment of
  the version identified above. It is not a warranty, and it does not extend
  to modified builds, forks, or the behaviour of third-party assistive
  technologies or Java runtimes."*

**Cost and elapsed time.**

| Path | Cost | Elapsed |
|---|---|---|
| Self-authored ACR (recommended) | **$0** | see sizing below |
| Third-party audit + ACR from an accessibility consultancy | commonly quoted **$5,000–$25,000** for a product this size — **unverified market estimate**, get quotes | typically 4–8 weeks — **unverified** |

Third-party authoring is disproportionate for a free single-maintainer tool
and buys little here: the reviewer's confidence comes from the linked
evidence, which is public either way.

**Validity, renewal, and what invalidates it.** No formal validity period
exists. Procurement convention is that an ACR should describe the version
being offered and be no more than roughly one to two years old — **unverified
as a formal rule; it is institutional practice and varies**. Concrete policy
to adopt:

- Regenerate on every **minor** release, and immediately on any change to: the
  menu bar or accelerators, the palette, element dialogs, the look-and-feel
  default, `Theme` colors, canvas interaction, the trace window, the help
  tree, or the installer module set. Several of these are already
  test-guarded, so the accessible-tree golden diff *is* the trigger signal.
- Invalidated by: shipping a new element with a nameless control; a
  look-and-feel change; dropping `jdk.accessibility`; a JDK bump that changes
  the Cocoa or Access Bridge behaviour; and — importantly — a WCAG/EN/508
  revision, since the tables are version-pinned.

**Where it is published, so procurement can find it.**

1. **`docs/accessibility-conformance-report.md`** — canonical, versioned,
   diffable, reviewed like any other change. Linked from README's
   "## Documentation" list alongside `docs/batch-interface.md` and
   `docs/file-format.md`.
2. **`ACCESSIBILITY.md`** at the repo root — a short front door pointing at
   the ACR, the known limitations, and the feedback channel. GitHub does not
   treat this as a special community-health file the way it does
   `SECURITY.md`, but it is the convention reviewers look for and it is
   trivially findable.
3. **A rendered copy attached to every GitHub Release** via
   `.github/workflows/release.yml:111-114`. Procurement asks for "the ACR for
   version X"; this makes that a URL rather than an email.
4. **In-jar help** (a new accessibility help page, §3 — see the layout caveat
   there) — offline
   discoverability for the student and the lab admin, consistent with the
   recorded "in-jar now, hosted later" help decision.
5. There is **nowhere to submit it.** No registry, no clearinghouse. If a
   university asks, you send the URL.

**The ADA Title II driver — stated without overclaiming.** The US DOJ final
rule under ADA Title II (published 24 April 2024, 89 FR 31320) adopts **WCAG
2.1 Level AA** as the technical standard for the **web content and mobile
applications** of state and local government entities, which includes public
universities. Compliance dates are **24 April 2026** for entities with a
population of 50,000 or more (and state governments) and **26 April 2027** for
smaller entities and special districts. *Verify these against the Federal
Register text before quoting them in the ACR.*

What that does and does not mean for JLS, precisely:

- **The rule's technical standard does not cover a native desktop
  application** that a university merely installs on lab machines. It covers
  web content and mobile apps. JLS is neither. There is **no legal obligation
  on this project**, which has no contract with any institution and is not a
  covered entity.
- **What is real** is the second-order effect: the rule has made accessibility
  a line item in university procurement and IT-review workflows, and those
  workflows are not carefully scoped — a reviewer processing a request to
  install JLS in a lab will ask for an ACR whether or not the rule technically
  reaches desktop software. Separately, the university's own general Title II
  obligations (program accessibility, effective communication) *do* apply to
  the course, and a tool a blind student cannot use is the university's
  problem regardless of which technical standard applies.
- **So the driver is procurement friction, not liability.** That is the honest
  framing and it should be the framing in the issue and the CHANGELOG: JLS
  publishes an ACR so an instructor does not have to fight their own IT
  department to assign it. Do not write "JLS is required to comply with the
  ADA" anywhere — it is false and it would undermine the report's credibility
  on its first page.

---

### Effort, risk, and failure modes

**Sizing** (maintainer-days; single maintainer, `mvn verify` must stay green,
each item includes its tests):

| Work | Days | Reasoning |
|---|---|---|
| §1 bundled-runtime bridge fix + `AccessBridgeModuleTest` | 1 | one-line module change; the time is verifying a real MSI against NVDA |
| §2 contrast: `focus` role, caret, canvas focus ring, palette recolor, `ThemeContrastTest` | 2 | the palette recolor is a joint optimisation against the existing ΔE≥25 test; the fuzz helper pays for itself |
| §3 help alt/lang, accessibility help topic, `HelpAccessibilityTest` | 1 | 10 alt strings, 83 mechanical `lang` edits, one new page wired into `Map.jhm`/TOC |
| §5 gap analysis across three standards | 2 | ~90 criteria × 5 surfaces; unavoidable desk work |
| Accessible-tree golden + name-coverage ratchet + target-size test | 2 | deterministic serialisation is the fiddly part |
| Manual AT audit, three platforms, first pass | 3 | needs a Windows machine, a Mac, and a GNOME session; Orca setup alone can eat a day |
| ACR authoring, review, publication wiring | 2 | including README/ACCESSIBILITY.md/release.yml |
| **Subtotal — honest ACR with the cheap gaps closed** | **13** | |
| §4 trace-window keyboard operability | 2–3 | `Trace` is 626 lines of mouse-only `JPanel` |
| §4 2.5.7 non-dragging move/wire modes | 1.5 | reuses the #75 caret state machine |
| §4 SVG `<title>`/`<desc>` | 0.5 | golden update |
| §6 accessible circuit outline view (the recommended canvas mitigation) | 3–4 | stock `JTree` over the existing model |
| **Full programme** | **20–22** | |
| *(Rejected: full JAAPI canvas scene model)* | *8–15 more* | *and permanent per-element maintenance* |

**Minimum viable, if the goal is only "answer the procurement email":** §1 +
§5 + AT audit + authoring = **~8 days**, publishing an ACR that says
"Partially Supports" in the right places. Everything else improves the
product, not the paperwork.

**Top three ways this goes wrong.**

1. **The ACR is written from the test suite and never from a screen reader.**
   Every automated signal in this repo reads the Java Accessibility API. Not
   one of them proves a *bridge* delivers anything to a *user*. Given the §1
   finding — that the Windows installers ship a runtime with no Access Bridge
   at all — it is entirely possible to have a fully green suite and an ACR
   claiming 502.3 support for a build where NVDA reads nothing. **Mitigation:
   no ACR row may be rated above "Not Evaluated" for 502.3/11.5.2/4.1.2
   without a dated AT session in `docs/accessibility-at-checklist.md`.**
2. **Over-rating, then being caught.** The temptation is to rate 2.1.1
   "Supports" on the strength of `docs/keyboard-a11y-verification.md` and
   forget the mouse-only trace window, or to rate 1.4.11 "Supports" without
   ever computing a contrast ratio (both would be *wrong today*, provably, at
   1.14:1 and at `Trace.java:20`). A university reviewer who finds one false
   "Supports" discounts the entire document — and this project's credibility
   rests on documents like `docs/reproducibility.md` and
   `docs/keyboard-a11y-verification.md` being scrupulously honest about what
   was *not* verified. **Mitigation: the gap analysis must produce a written
   evidence citation per row before any rating is entered.**
3. **The ACR becomes stale and nobody notices.** Colors change, an element
   ships with a nameless button, a JDK bump changes the module set — and the
   published document silently becomes false. **Mitigation: the
   accessible-tree golden and `ThemeContrastTest` are precisely the tripwires;
   wire the ACR regeneration trigger into the same place the CHANGELOG
   discipline lives, and put the report date and commit SHA *in the report*
   so staleness is visible.**

**Do NOT do this if any of the following hold.**

- **No maintainer access to all three platforms with real AT.** If you cannot
  run Orca, NVDA, and VoiceOver against a real build, publish
  `ACCESSIBILITY.md` — a plain statement of what is keyboard operable, what
  the canvas limitation is, and how to report problems, linking the existing
  verification docs — and stop there. That document is honest, useful, costs a
  day, and can be upgraded later. **A half-audited ACR is worse than no ACR.**
- **The §1 bridge fix is not landed first.** Publishing an ACR while the
  primary Windows distribution has no Access Bridge means either publishing
  "Does Not Support" on the flagship interoperability rows or publishing a
  false claim. Fix, then report.
- **It is being done to close a checkbox rather than because a real
  institution asked.** `docs/standards-landscape.md` **§13.2 item 1**
  (lines 753-756) ranks this *first* among the institutional/project-conformance
  items precisely because it is *likely to be asked for* — the value is in
  answering a real request accurately. Producing an unrequested compliance
  document for a free tool, at the cost of two to three weeks that could go to
  the trace window and the outline view, inverts the point. If you only have
  budget for one, **build the outline view and fix the contrast** — that helps
  an actual blind student; the ACR only helps a procurement officer.

---

### Sources

**Primary external documents** (consult directly; do not rely on the summaries
above for clause numbers):

- ITI, **VPAT® 2.5 INT Edition** template and instructions — `itic.org`.
  *Unverified: current revision number and its publication date.*
- W3C, **WCAG 2.2** (W3C Recommendation, 5 October 2023).
- W3C, **WCAG2ICT** Group Note, *Guidance on Applying WCAG 2 to Non-Web
  Information and Communications Technologies*. *Unverified: publication date
  of the WCAG 2.2-era edition.*
- US Access Board, **Revised Section 508 Standards**, 36 CFR Part 1194
  Appendices A/C (published 18 January 2017, effective 18 January 2018) —
  E205.4, **E207.2** (the four-SC non-web-software exemption), **E101.2**
  Equivalent Facilitation, Ch. 5 §§501–504, Ch. 6 §§601–603. *Unverified:
  exact clause lettering in the current text.*
- ETSI/CEN/CENELEC, **EN 301 549 V3.2.1 (2021-03)** — clauses 5, 10, 11
  (11.1–11.4 WCAG mapping, 11.5 interoperability with AT, 11.6, 11.7, 11.8),
  12. *Unverified: whether a later version (V4.x, WCAG 2.2-based) is now the
  harmonised text.*
- US DOJ, **ADA Title II final rule**, 89 FR 31320 (24 April 2024), WCAG 2.1
  AA for web content and mobile apps; compliance dates 24 April 2026 /
  26 April 2027. *Verify dates and thresholds against the Federal Register.*
- Oracle/OpenJDK: `jdk.accessibility` module, Java Access Bridge (Windows);
  `javax.accessibility` (`AccessibleContext`, `AccessibleRole`,
  `AccessibleRelation.CONTROLLER_FOR`/`CONTROLLED_BY`, `AccessibleSelection`).
  *Unverified: current maintenance state of `java-atk-wrapper` on major
  Linux distributions — check before promising Orca support.*
- **Unverified market figures:** third-party ACR/audit pricing ($5k–$25k) and
  the "ACR no older than 1–2 years" procurement convention. Both are
  recollected industry practice, not sourced.
- **Not legal advice:** the False Claims Act / GPLv3-disclaimer discussion is
  a structural reading, not a verified legal analysis.

**Repository paths, all verified by reading the tree:**

- `docs/standards-landscape.md` — registry entries 209–215 (§11.6, lines
  503-509), 256–258
  (§12.d, lines 605-607), and the ranking, which since commit `9ab4797` is
  **§13.2 item 1** (line 753) — first among the institutional/project items,
  listed after §13.1's logic-design entries. Any "§13 item 3 / third of ten"
  citation predates that split.
- `docs/keyboard-a11y-verification.md` — the #75 evidence table, the
  red-on-break record, and lines 123–127 (the canvas residual).
- `docs/component-naming.md` — `palette.<slug>` / `menu.elements.<slug>` /
  `dialog.<slug>.<field>` and the `labelled(...)` helper that sets
  `setLabelFor` plus accessible name.
- `docs/flatlaf-evaluation-2026-07.md` — the hardcoded-color audit, §(a)
  canvas colors and §(b) ~126 chrome `setBackground`/`setForeground` sites.
- `docs/wayland-desktop-checklist.md` — the template for the manual
  per-release AT checklist.
- `src/jls/Theme.java:57-83` — `DEFAULT` and `CLASSIC` palettes (the sRGB
  values the contrast numbers above were computed from).
- `src/jls/edit/SimpleEditor.java:1187` (`setFocusable(true)`, the only
  focus-related call site), `:2392-2404` (the caret painted in
  `selectionColor`), `:1447-1461` and `:2470` (space-drag pan),
  `:2515-2520` (wire drag).
- `src/jls/edit/Trace.java:20` — `implements MouseListener,
  MouseMotionListener`, no keyboard, no accessible wiring.
- `src/jls/edit/InteractiveSimulator.java:125-155` — control buttons with
  tooltips and hardcoded background colors.
- `src/jls/edit/CircuitRenderer.java:314-358` — the JFreeSVG export path.
- `src/jls/edit/images/*.gif` — the palette icons `PaletteEntry` names
  (33 GIFs), measured 24×24. (`src/jls/images/` holds a 34-file duplicate set
  that no `src/**.java` references; do not cite it as the palette source.)
- `resources/help/**` — 83 HTML files, 10 `<img>` tags, **zero** `alt`
  attributes, **zero** `lang` attributes.
- `scripts/build-installer.sh:141-146` (`jdeps --print-module-deps`),
  `:129-131` (`clamp_mtimes`), `:157-166` (jlink), `:351` (deb
  `--resource-dir`).
- `.github/workflows/ci.yml` — the xvfb-armed Linux build step, the advisory
  Windows and macOS lanes, and the `gui-wayland` lane (the cautionary
  precedent for adding a fragile GUI lane).
- `.github/workflows/release.yml:107-117` — release asset publication.
- `pom.xml:266-289` — the headless `excludedGroups=display` execution and the
  `display-tests` execution with `rerunFailingTestsCount`.
- Existing tests cited as evidence: `test/jls/ThemeTest.java`,
  `test/jls/elem/WireValueChannelTest.java`, `test/jls/HelpTopicsTest.java`,
  `test/jls/HotkeysHelpAccuracyTest.java`, `test/jls/KeyPadAccessibilityPinTest.java`,
  `test/jls/SvgExportTest.java`, `test/jls/HeadlessCoreRatchetTest.java`,
  and under `test/jls/ui/`: `MenuMnemonicAndAccessibleNameTest.java`,
  `PaletteButtonAccessibilityTest.java`, `KeyPadAccessibilityTest.java`,
  `ComponentIdentityTest.java`, `FocusFaithfulKeyboardTest.java`,
  `KeyboardEditingFaithfulTest.java`, `KeyboardPlacementFaithfulTest.java`,
  `MenuAcceleratorFiringTest.java`, `TabSelectionFocusTest.java`,
  `EditorZoomTest.java`, `PaletteDropTest.java`,
  `DialogConstructionSmokeTest.java`, `DialogCoverageRatchetTest.java`,
  `RenderAssert.java`, `EditorGestureSupport.java`.
- **To be created** (none of these exist today): `test/jls/ThemeContrastTest.java`,
  `test/jls/HelpAccessibilityTest.java`, `test/jls/AccessBridgeModuleTest.java`,
  `test/jls/ui/AccessibleTreeGoldenTest.java`,
  `test/jls/ui/AccessibleNameCoverageTest.java`,
  `test/jls/ui/CanvasFocusVisibleTest.java`, `test/jls/ui/TargetSizeTest.java`,
  `test/jls/ui/TraceKeyboardTest.java`, `test/jls/ui/NoDragRequiredTest.java`,
  `test/resources/a11y/accessible-tree.txt`,
  `docs/accessibility-conformance-report.md`,
  `docs/accessibility-at-checklist.md`, `ACCESSIBILITY.md`,
  a new accessibility help page under `resources/help/` (§3 names the layout
  constraint — `resources/help/overview` is a file, not a directory).
