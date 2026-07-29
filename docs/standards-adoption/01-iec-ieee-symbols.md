## IEEE 91/91a-1991 and IEC 60617-12 schematic symbol conformance (#43, #44, #45)

### What conformance actually means

**The documents.** Three registry entries collapse into two live documents and one
that should be dropped:

- **#43 — ANSI/IEEE Std 91-1984**, *IEEE Standard Graphic Symbols for Logic
  Functions*, together with its supplement **IEEE Std 91a-1991**. This is the
  document that defines the *rectangular-outline* symbol system: a qualifying
  symbol in the outline, negation and polarity indicators at the pins, and
  **dependency notation** (the letter/number pairing that makes a symbol read as
  a specification rather than a picture). 91-1984 also blesses the
  distinctive-shape set JLS already draws, which is why the current gates are not
  *non*-conformant so much as *unclaimed*.
- **#44 — IEC 60617-12**, *Graphical symbols for diagrams — Part 12: Binary logic
  elements*. Technically aligned with IEEE 91; it is the international
  publication and the one European curricula teach from.
- **#45 — IEEE 315 / ANSI Y32.2** is graphic symbols for *electrical and
  electronics diagrams* generally (resistors, sources, connectors). JLS draws no
  analog components. **Drop #45 from scope and say so in the conformance
  statement** — claiming it would be padding.

**What would be claimed.** A JLS conformance claim is necessarily *partial and
scoped*, because IEEE 91 and IEC 60617-12 are catalogs of symbols for a device
universe far larger than JLS's element set. The claim to make is:

> When the rectangular symbol set is selected, JLS draws each element in the
> table below using the outline, qualifying symbol, indicators and dependency
> labels specified by IEEE 91-1984/91a-1991 and IEC 60617-12 for that function.
> Elements marked *no standard symbol* are drawn as a plain rectangle with a
> JLS-local designation and are outside the claim.

Explicitly **not** claimed:

- **Nothing about the distinctive-shape mode.** It stays byte-identical (see the
  parity test below) and carries no claim beyond "these are the customary
  distinctive shapes".
- **No polarity-indicator (mixed-logic) rendering.** IEEE 91 supports both the
  negation circle (positive-logic convention) and the polarity triangle
  (direct-polarity convention). JLS's simulation semantics are positive-logic
  throughout (`docs/simulation-semantics.md`), so only the negation circle is
  drawn. State this as a deliberate convention choice, not a gap.
- **No analog, no transmission gates, no bidirectional/open-collector output
  qualifiers, no signal-flow-direction reversal arrows.** JLS has no elements
  that need them.
- **No claim about line style, sheet layout, reference designators, or title
  blocks** — those are IEC 60617 parts 1–2 and IEEE 315 territory.

**The artifact the claim rests on.** Not the code. The claim rests on a new
normative document, `docs/symbol-conformance.md`, written in the house style
(RFC 2119 keywords, cited issue numbers, verified/unverified marks like
`docs/hdl-support-research.md`), containing a **clause-by-clause conformance
matrix**: one row per JLS element, giving the outline, the qualifying symbol, the
indicators, the dependency labels, the clause of IEEE 91 / IEC 60617-12 that
specifies it, and the test that pins it. A reader who has the standards in hand
must be able to audit JLS against that table without reading Java. Without that
document there is no conformance claim, only rectangles.

**The scope table** (what each JLS element becomes; confidence marked):

| Element | Rectangular rendering | Confidence |
|---|---|---|
| `AndGate` | rectangle, qualifying symbol `&` | certain |
| `OrGate` | rectangle, `≥1` | certain |
| `XorGate` | rectangle, `=1` | certain |
| `NotGate` | rectangle, `1`, negation circle on the output | certain |
| `NandGate` | rectangle, `&`, negation circle on the output | certain |
| `NorGate` | rectangle, `≥1`, negation circle on the output | certain |
| `DelayGate` | no standard symbol — JLS-local; rectangle with `DELAY` or the delay value, out of claim | n/a |
| `Extend` | no standard symbol — sign/zero extension is a JLS-local wiring aid, out of claim | n/a |
| `TriState` | rectangle, `1`, control input `EN`, three-state output indicator (open downward triangle at the output) | high, verify indicator geometry |
| `Mux` | rectangle, `MUX`, select input as a **G dependency range** (`G0/3` for a 4-way), data inputs numbered `0..3` — the index labels `MuxRenderer` already draws become the dependency identifiers | high |
| `Decoder` | rectangle, code-converter qualifying symbol `X/Y`, input weights `1,2,4,…`, outputs numbered `0..2ⁿ-1` | high |
| `Adder` | rectangle, `Σ`, operand weights on the A and B groups, `CI`/`CO` on the carries | high |
| `Register` | rectangle, `RG n`, common-control section with `C1` (dynamic-input triangle already drawn by `RegisterRenderer`), data inputs `1D`, `Q`/`Q̄` outputs | high |
| `ShiftRegister` | **honest problem**: `SRG n` denotes a stage-shifting register clocked one position per edge; JLS's `ShiftRegister` is a combinational barrel shifter by an amount input. There is no IEC qualifying symbol for that. Draw a rectangle with a JLS-local `SHIFT` designation and **record the non-conformance explicitly** | n/a |
| `Memory` | rectangle, `ROM` or `RAM`, address input with range annotation (`A 0/1023`), `EN` on output enable, write port as a `C`/`D` dependency pair | medium — the exact memory dependency wiring is the clause most in need of the primary document |
| `SubCircuit` | already a rectangle; a user-defined block with a designation is permitted. No change, in-claim as a generic element | high |
| `StateMachine`, `TruthTable` | no standard symbol (the landscape survey §3 already says the ASM/state-diagram space is unstandardized). Out of claim | n/a |
| `Clock`, `Constant`, `Display`, `SigGen`, `Pause`, `Stop`, `Text`, `InputPin`, `OutputPin`, `JumpStart`, `JumpEnd`, `Binder`, `Splitter`, `Wire`, `HLine`/`VLine`, `Cross`, `Group` | sources, sinks, wiring aids and annotations; no IEC 60617-12 binary-logic symbol. Out of claim, unchanged in both modes | n/a |

Note the pleasant surprise the code makes visible: **`Mux`, `Decoder`, `Adder`,
`Register`, `ShiftRegister`, `Memory`, `StateMachine` and `SubCircuit` are
already drawn as labelled rectangles** (`src/jls/edit/MuxRenderer.java` is the
only non-rectangular one — a trapezoid, pinned by
`test/jls/elem/MuxSymbolTest.java`). For those elements the work is *labels and
dependency notation*, not new geometry. The genuinely new drawing is the six
gate leaves plus `TriState`.

---

### Implementation procedure

**Design decision, stated up front: this is a render mode, not a per-element
strategy and not an extension point.**

- Not per-element: mixing symbol systems inside one schematic is
  non-conformant and pedagogically indefensible. One setting, whole application.
- Not an extension point: `docs/extension-points.md` lists a `gui.theme`
  ("Preferences / theme contributor", `one active`) seam as **pending (#76)**,
  and the module mechanism in `src/jls/module/` exists for *external*
  contributions. A first-party symbol set that must ship in the single jar and
  must be reachable from headless batch export does not belong behind a module
  boundary. If the `gui.theme` seam is ever typed, the symbol set can be folded
  into it then; do not build it to that seam speculatively.
- The mode is a **global, headless-representable enum** consulted by the
  GUI-side renderers, exactly mirroring how `jls.Theme.apply()` rewrites
  `JLSInfo.Palette` statics that the drawing code already reads.

**Steps.**

1. **Add `src/jls/core/SymbolSet.java`** — `public enum SymbolSet { DISTINCTIVE,
   RECTANGULAR }` with a `name()`-based lookup and `DISTINCTIVE` as the default.
   It goes in `jls.core` because `jls.elem` (a headless-core package under
   `test/jls/HeadlessCoreRatchetTest`) must be able to name it. No AWT.

2. **Add the active-set static.** `src/jls/JLSInfo.java` already holds the
   theme-derived drawing statics; add `public static SymbolSet symbolSet =
   SymbolSet.DISTINCTIVE;`. `JLSInfo` is not in `HeadlessCoreRatchetTest`'s
   `CORE_FILES` (it already imports `java.awt.Color`), so this is free. Add it to
   `test/jls/JLSInfoNullableFieldsTest` coverage if that test enumerates fields.

3. **Extend `GateOutline` with labels, not with a new `Segment` kind.**
   `src/jls/elem/GateOutline.java`'s `Segment` is a record over `double[]
   coords`; do **not** widen it — `test/jls/elem/GateOutlineParityTest.java`
   reconstructs paths from it and would need rewriting. Instead add a parallel
   list:

   ```java
   public enum Anchor { CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BASELINE, ... }
   public record Label(String text, double x, double y, Anchor anchor,
           boolean overbar) {}
   public List<Label> labels();          // empty for every existing outline
   ```

   plus `Builder.label(...)`. Existing outlines return an empty label list, so
   `GateOutlineParityTest` stays green untouched. Text placement stays *data*;
   the font and `FontMetrics` work stays in `jls.edit`, preserving the headless
   split the class's javadoc describes.

4. **Give each gate leaf a rectangular outline.** Change the abstract method on
   `src/jls/elem/Gate.java` from `outline()` to:

   ```java
   public GateOutline outline(SymbolSet set);
   public final GateOutline outline() { return outline(JLSInfo.symbolSet); }  // NO
   ```

   — do **not** read the global from the model. Make it
   `public abstract GateOutline outline(SymbolSet set);` with a `default`-style
   base implementation on `Gate` that returns the existing distinctive outline
   for `DISTINCTIVE` and a shared rectangular builder for `RECTANGULAR`, and let
   each leaf supply only (a) its distinctive outline as today and (b) its
   qualifying-symbol string and output-negation flag. Keep `outline()` (no-arg)
   as a deprecated delegate to `outline(DISTINCTIVE)` for one release so nothing
   outside the tree breaks. `src/jls/edit/GateRenderer.java:53` becomes
   `gatePathFrom(gate.outline(JLSInfo.symbolSet))` — the global is read on the
   GUI side, where it belongs.

5. **Fix the rectangle's geometry to the existing envelope. This is the
   load-bearing compatibility rule.** `Gate.init()` sets
   `width = SPACING*4`, `height = SPACING*(max(n,3)-1)` and places puts at
   `inx = 0` / `outx = SPACING*4` (`src/jls/elem/Gate.java:168-248`);
   `Gate.getRect()` grows that by `SPACING/2` top and bottom.
   `Geometry.SPACING == 12`, `Geometry.POINT_DIAMETER == 6`
   (`src/jls/core/Geometry.java:19,21`).

   **The symbol set MUST NOT change `init()`, put coordinates, `getRect()`, or
   any saved geometry.** Wires attach at put coordinates; moving them would
   silently rewire circuits when a preference changes. Instead, the rectangular
   body is drawn to fill the envelope that already exists: horizontally the same
   `x+SPACING .. x+3*SPACING` span the distinctive body occupies, vertically
   exactly `getRect()`'s extent, i.e. from half a spacing above the first input
   to half a spacing below the last. Check the arithmetic: a 2-input gate has
   inputs at local `y = 0` and `y = 2s`, output at `y = s`; a 5-input gate has
   inputs at `y = 0..4s` and output at `y = 2s`. In both cases the rectangle
   `[-s/2, height + s/2]` puts every input a clean half-spacing inside a corner
   and centres the output. The renderer's input fan-out
   (`GateRenderer.draw`, the `inc`-stepped loop at lines 96-124) becomes straight
   perpendicular stubs in rectangular mode — one branch, no size change.

   A 24 px-wide body holds `&`, `≥1`, `=1` and `1` at the default 12 pt sans
   face. Verify with `jls.core.TextMetrics` at build time rather than by eye
   (see the testing section); if `≥1` overflows on some platform font, widen the
   *drawn* rectangle toward the puts, never the element bounds.

6. **Qualifying symbols and dependency labels on the box elements.** Touch
   `src/jls/edit/MuxRenderer.java`, `DecoderRenderer.java`, `AdderRenderer.java`,
   `RegisterRenderer.java`, `ShiftRegisterRenderer.java`, `MemoryRenderer.java`,
   `TriStateRenderer.java`. Each gets a `SymbolSet` branch that changes only the
   *label text and its placement* (and, for `Mux`, swaps the trapezoid for a
   rectangle). `RegisterRenderer` already draws `D`, `C` with the dynamic-input
   triangle, and `Q`/`Q̄` with an overbar
   (`src/jls/edit/RegisterRenderer.java:91-137`) — the change is `D` → `1D`,
   `C` → `C1`, plus the `RG n` qualifying symbol. That is representative of how
   small most of this is.

7. **User preference storage.** Add `SYMBOLS_KEY = "symbolSet"` to
   `src/jls/UserPrefs.java`, a `rememberSymbolSet(String)` alongside
   `rememberTheme`, and one line in `applyStartup()`
   (`src/jls/UserPrefs.java:81-87`) setting `JLSInfo.symbolSet`. The
   corrupt/missing-value fallback is `DISTINCTIVE`, matching the
   `parseColor`/`parseUndoDepth` pattern already in the file. `UserPrefs` already
   degrades to an in-memory map when the backing store is unavailable, so no new
   failure mode.

8. **GUI selector.** A `Symbol set` radio-button submenu in
   `JLSStart.globalMenu()` immediately after `Color scheme`
   (`src/jls/JLSStart.java:1941-1964`), built the same way, calling
   `prefs.rememberSymbolSet(...)` and then the existing repaint sweep
   (`refreshEditorColors()`, or a renamed `refreshEditors()`). **This changes the
   menu bar, which is a pinned contract**: update the expectation block in
   `test/jls/ui/MenuBarSpecTest.java:95-127` and add the item to
   `docs/keyboard-a11y-verification.md`'s menu inventory and a mnemonic to
   `src/jls/MenuAcceleratorPolicy.java` if that table enumerates Global items.

9. **Batch/CLI reachability.** Add one row to the `FLAGS` table in
   `src/jls/JLSStart.java:759`:
   `new FlagSpec("symbols", Arity.REQUIRED, "set", "a symbol set", ...)`.
   `jls -h` usage is generated from that table and
   `test/jls/CliFlagTableTest.java` cross-checks it, so the flag is nearly free.
   **Design call: batch mode MUST NOT read `UserPrefs`.** `applyStartup()` is
   called only at GUI startup (`src/jls/JLSStart.java:1230`) and it must stay
   that way — a headless `-i` export whose pixels depend on the invoking
   developer's desktop preferences would break the reproducibility stance in
   `docs/reproducibility.md` and make golden images environment-dependent. In
   batch, `-symbols` alone selects; absent it, `DISTINCTIVE`.

   This is adjacent to `docs/batch-interface.md`, a **documented stability
   contract**, and §6 explicitly permits it: "Additions that cannot break a
   conforming consumer (a new flag, a new optional output gated behind a new
   flag) are minor-version material but still belong in the CHANGELOG."
   **Correction to an earlier draft of this step: do *not* add `-symbols` to the
   §1 synopsis line.** §1's synopsis is the `-b` batch-mode invocation
   (`jls -b [-s paramfile] [-t testfile] [-d limit] [-vcd file] [-r printer]
   [--] circuit.jls`); it deliberately omits `-i`, `-export`, `-board`, `-pins`
   and `-savetext`, and defers to "the flag table in `src/jls/JLSStart.java`
   (`FLAGS`) … the single authoritative flag list". Since `-symbols` modifies
   `-i`, which §1 never mentions, adding it to that synopsis would misrepresent
   the contract's scope — this is the same reading §08 (IP-XACT) applies to
   `-ipxact`. `FLAGS` plus `CliFlagTableTest` plus a CHANGELOG entry is the
   whole obligation. No deviation document is needed; a *silent* default change
   would need one, which is the reason the default stays `DISTINCTIVE`.

10. **Image export and printing follow for free.**
    `CircuitRenderer.exportImage` (`src/jls/edit/CircuitRenderer.java:301-390`)
    dispatches through `ElementRenderers.draw` for SVG and through
    `CircuitRenderer.draw` for raster, and printing books pages through the same
    renderers (`addToBook`, lines ~260-287). Nothing there needs a symbol-set
    parameter: the renderers read `JLSInfo.symbolSet`. The determinism property
    `SvgExportTest#exportingTwiceIsByteIdentical` relies on stable draw order and
    a fixed defs prefix; the symbol set is constant for the process, so it holds.

11. **The palette icon problem.** `src/jls/edit/images/` holds 33 hand-drawn
    GIFs; `PaletteEntry` names one per element and `test/jls/edit/PaletteContractTest`
    enforces that the resource exists. In rectangular mode the toolbar would keep
    showing distinctive shapes — a real correctness bug, not cosmetics, because a
    student clicking a curved-OR icon would place a `≥1` rectangle.
    **Recommendation: keep the GIFs for `DISTINCTIVE` (zero visual regression)
    and render the `RECTANGULAR` icons at runtime from the same symbol model.**
    Concretely: a `PaletteIcons` helper in `jls.edit` that, for the eight
    `Group.GATES` entries plus `Mux`, paints the element's rectangular symbol
    into a `BufferedImage` at icon size using the same `GateRenderer` translation
    path, cached per symbol set. This eliminates the icon-vs-canvas drift class
    permanently for the affected entries rather than creating a second set of
    hand-drawn assets nobody can verify. `PaletteEntry` keeps its `iconName`
    (still required non-blank, `PaletteContractTest` unchanged); the renderer
    just overrides the image when the active set is `RECTANGULAR`. Fallback if
    the runtime rendering proves fussy at 16–24 px: ship parallel
    `and-iec.gif`-style assets and resolve by suffix — cheaper to write, much
    harder to keep honest.

12. **Help and docs.**
    - New `docs/symbol-conformance.md` — the normative conformance matrix
      described above. This *is* the deliverable.
    - A new help page under `resources/help/elements/gates/` (or
      `resources/help/elements/other/`) explaining the
      two symbol sets, plus its `resources/help/Map.jhm` topic and
      `resources/help/JLSHelpTOC.xml` entry — `test/jls/HelpTopicsTest` is a link
      checker and reachability test and will fail until both exist.
    - A recorded decision in `ARCHITECTURE.md` under "Recorded decisions":
      *"Symbol sets: one global render mode, never per-element, never in the save
      file"*, with the reopening trigger (a course that needs mixed notation).
    - `docs/standards-landscape.md` §1 gains two `HAVE` rows (#43, #44) and §3
      loses two `COULD`s; **§13.1**'s ranked list item 1 gets struck. §14's tally
      counts are `grep`-checkable and must be re-run.
    - `CHANGELOG.md` entry (Keep a Changelog style, minor version).

**Migration and compatibility story.**

- **Saved files do not change. At all.** The symbol set is a `UserPrefs` key and
  a CLI flag, never an `ELEMENT` attribute, never a `CIRCUIT` property, never a
  `FORMAT` version bump. `docs/file-format.md` is untouched;
  `test/jls/DeterministicSaveTest`, `AllElementsRoundTripTest`,
  `FileFormatSpecTest` and `GenerativeRoundTripFuzzTest` should all pass without
  edits, and the fact that they do is itself evidence.
- **Existing users see no change** until they choose the setting: default is
  `DISTINCTIVE`, and the distinctive drawing is byte-identical (pinned).
- **Existing scripts see no change**: `-i` without `-symbols` produces the same
  bytes as before.
- **Stability contracts touched**: none, strictly. The additive flag is
  permitted by `docs/batch-interface.md` §6 and lives in `JLSStart.FLAGS`, which
  §1 names as authoritative rather than duplicating. Image export is *not*
  covered by a stability promise today —
  `docs/batch-interface.md` never mentions `-i` — and this work should not
  create one; say so in the conformance doc so nobody later treats a symbol
  refinement as a breaking change.

---

### Testing procedure

The honest framing first: **you cannot prove conformance to a graphical standard
by testing.** There is no conformance suite, no reference renderer, no oracle.
What tests can do is (a) prove the geometry is what the conformance document
*says* it is, (b) prove the document and the code cannot drift apart, and (c)
prove the distinctive mode did not regress. That triad, plus a human reading the
standard once, is the whole proof. Design the tests so that the human review is
done against the *conformance matrix*, and the matrix is mechanically tied to
the code.

**New test classes (all "to be created"):**

1. **`test/jls/elem/RectangularSymbolTest.java`** — geometry assertions in the
   style of the existing `test/jls/elem/MuxSymbolTest.java`: load each gate
   headlessly from circuit text, paint to a `BufferedImage`, probe *hand-written*
   integer lattice points (corners of the rectangle, midpoints of each side,
   centre of the negation circle, the clear interior above and below the
   qualifying symbol). `MuxSymbolTest`'s discipline applies verbatim and its
   javadoc explains why: probes written by hand, not derived through
   `GridTransform`, so the test is an independent check and not a tautology; kept
   ≥ 9 px from any put centre so the 6 px put ring cannot touch them; clear of
   label glyphs so font metrics cannot move them. Assert for all four
   orientations and for input counts 1, 2, 3, 5, 8.

2. **`test/jls/elem/SymbolSetParityTest.java`** — the regression guard. For every
   `Gate` leaf and every orientation, assert `outline(DISTINCTIVE)` flattens to
   the identical point stream as the historical outline. Reuse the flattening
   helper from `test/jls/elem/GateOutlineParityTest.java` (which already does
   exactly this against a hand-copied reference of the pre-#77 inline
   construction). **This turns red the moment someone "improves" the distinctive
   shapes while working on the rectangular ones** — the single most likely
   regression.

3. **`test/jls/elem/SymbolConformanceMatrixTest.java`** — the anti-drift test,
   and the one that makes the conformance claim maintainable. Parse the table in
   `docs/symbol-conformance.md`, and assert in both directions against the
   element registry: every element in `jls.elem.ElementRegistry` has a row; every
   row names a real element tag; the qualifying-symbol string in the row is
   exactly the string the renderer draws (expose it as a
   `String qualifyingSymbol(SymbolSet)` accessor on the model side so the test
   never needs pixels). Precedent in the tree: `ExtensionPointCatalogTest` does
   exactly this two-way check against `docs/extension-points.md`, and
   `test/jls/edit/PaletteContractTest` does it for the palette. Adding an element
   without a conformance row becomes a build failure — which is how the claim
   stays true in 2029.

4. **`test/jls/elem/SymbolLabelFitTest.java`** — property-ish test over
   `jls.core.TextMetrics`: for every qualifying symbol string in the matrix,
   assert the rendered advance width plus a margin fits inside the drawn body
   width for the smallest legal element of that type, on the platform font the
   run has. Catches the `≥1`-overflows-on-a-CJK-fallback-font class of bug that
   no golden image on one runner would find.

5. **`test/jls/SymbolSetImageExportTest.java`** — the end-to-end lane, modelled on
   `test/jls/CliImageExportTest.java` (which already forks a JVM with
   `-Djava.awt.headless=true` and runs `jls.JLS`). Assert:
   `-i out.svg` without `-symbols` is byte-identical to today's output;
   `-i out.svg -symbols rectangular` differs; running it twice is byte-identical
   (the `SvgExportTest#exportingTwiceIsByteIdentical` property, per set);
   `-symbols bogus` is a usage error with exit 2 and `jls: error:` on stderr, per
   the `docs/batch-interface.md` §1 contract.

**Golden-file strategy.** House style is byte-exact goldens under
`test/resources/` (see `test/resources/hdl/*.v`). **Use SVG, not PNG, as the
golden.** Rationale, and it is the important call in this section: raster goldens
are exactly what `CliImageExportTest`'s javadoc says the project rejects —
"deliberately no pixel goldens, which are brittle across JDK font rendering" —
and `test/jls/ui/package-info.java` layer 3 says "never brittle pixel goldens".
SVG output from `CircuitRenderer.exportImage` is already deterministic and
byte-comparable, and a symbol is a *path*, so an SVG golden pins the geometry
without pinning glyph rasterization. Store
`test/resources/symbols/gates-rectangular.svg` and
`test/resources/symbols/gates-distinctive.svg` from a single fixture circuit
holding one of each gate. **Caveat to handle explicitly:** JFreeSVG emits text as
`<text>` elements whose *position* is font-metric-derived, so the golden will
still drift with the platform font. Mitigate by generating the golden from a
fixture drawn with a fixed logical font and asserting on the path data (`d=`
attributes) with text elements filtered out, or by tagging the golden test
`@Tag("display")`-free but font-pinned. If that proves fragile, fall back to
asserting the *path* subset only — the geometry is the conformance-relevant part;
the label text is covered by test 3.

**Sequencing note — this collides with the accessibility section.** The
accessibility item (§03 of this playbook) proposes injecting `<title>` and
`<desc>` elements into `CircuitRenderer.exportImage`'s SVG output for 508
§504.2 / EN 11.8.2. That changes the bytes of every SVG golden, including the
two proposed here. The two changes are compatible but not order-independent:
land the `<title>`/`<desc>` injection **before** minting
`test/resources/symbols/*.svg`, or budget a golden regeneration. Both sections
agree that SVG output is **not** a documented stability contract
(`docs/batch-interface.md` names only `-t`, the watched-element stdout format
and the VCD profile), so this is a golden update, not a deviation — but say so
in whichever PR lands second.

**Existing tests that must be extended, not replaced:**

- `test/jls/ElementDrawSmokeTest.java` — its fixture sweep (raster and SVG export
  paths, plus the completeness check that fails when the palette grows) should
  run under **both** symbol sets. Two parameterized runs; catches
  `NullPointerException`-on-a-missing-label immediately.
- `test/jls/ui/RenderBoundsTest.java` — the `RenderAssert.assertPaintsWithinBounds`
  sweep must pass in rectangular mode too. This is the test that proves step 5's
  "no size change" rule: a rectangle drawn outside `getRect() + DRAW_MARGIN`
  would be truncated by a clipped repaint, and this turns red.
- `test/jls/ui/MenuBarSpecTest.java` — expectation block updated for the new
  submenu (it is `@Tag("display")`, so it runs in the xvfb lane).
- `test/jls/CliFlagTableTest.java` — picks up `-symbols` automatically via
  `usageDocumentsExactlyTheParserFlags`; add an explicit parse test for the
  operand values.
- `test/jls/edit/PaletteContractTest.java` — add an assertion that every
  `Group.GATES` entry produces a non-blank icon in *both* symbol sets.
- `test/jls/HeadlessCoreRatchetTest.java` — no edit expected, and that is a
  deliverable: `SymbolSet` and the `GateOutline.Label` record must not add an AWT
  import to `jls.core`/`jls.elem`. If the baseline needs a new line, the design
  is wrong.
- `test/jls/HelpTopicsTest.java` — fails until the new help page, `Map.jhm` topic
  and TOC entry exist.

**Property / fuzz opportunities.** One is genuinely worth it: a bounded property
over (gate kind × input count 1..16 × orientation × symbol set) asserting the
drawn body is a closed convex quadrilateral whose corners are the four expected
lattice points and which contains every input stub endpoint on its boundary. That
is a real invariant of the standard's outline requirement and it is cheap to
state over `GateOutline` without rendering. `test/jls/GenerativeRoundTripFuzzTest`
and `test/jls/ContainerMutationFuzzTest` establish the style.

**External tool validation.** There is none available — no validator exists for
IEEE 91 or IEC 60617-12 symbols. Do **not** invent a skip-when-absent hook for
something that does not exist. The one adjacent external check worth adding is
optional SVG well-formedness via `xmllint` if present (skip when absent, the
pattern `README.md` uses for `iverilog`/`ghdl`/`yosys`), which catches malformed
label escaping rather than conformance.

**CI lane changes (`.github/workflows/ci.yml`).** None required, and that is the
right answer. Everything above runs in the default `mvn -B verify` lane except
`MenuBarSpecTest`, which is already `@Tag("display")` and already runs under
`xvfb-run -a mvn -B verify -Djls.test.headless=false` on the Linux job
(`.github/workflows/ci.yml` lines ~69-74; the tag split is configured in
`pom.xml`'s default `excludedGroups=display` and the `display-tests` surefire
execution). Resist adding a lane; if the golden SVG turns out to be
font-sensitive, pin the font in the test rather than pinning the runner.

**What turns the tests red (the regression inventory):**
- Any change to distinctive-shape geometry → `SymbolSetParityTest`.
- Any change to gate size, put positions, or `getRect()` → `RenderBoundsTest`,
  `test/jls/elem/OrientationGeometryTest` and the existing
  `test/resources/orientation-geometry.txt` golden.
- A new element without a conformance row, or a renamed qualifying symbol →
  `SymbolConformanceMatrixTest`.
- A symbol whose label overflows its body → `SymbolLabelFitTest`.
- A symbol-set-dependent save → the untouched round-trip suite.
- Non-determinism in export → `SymbolSetImageExportTest` / `SvgExportTest`.

---

### Certification / conformance procedure

**There is no certifying body. This is self-assertion, full stop.** IEEE SA and
IEC publish symbol standards; neither operates a conformance program, a
registry, a mark, or an accreditation scheme for drawing tools. No accredited
laboratory tests schematic symbols. Nobody will audit JLS. Any page that implies
otherwise would be wrong, and the project's own framing in
`docs/standards-landscape.md` §0 — "Conformance is usually self-asserted (nobody
audits your VCD writer)" — already says the right thing.

So the entire "certification procedure" is: **make a self-assertion credible
enough that a skeptical instructor can check it.** That consists of five things,
all of which are artifacts, not processes:

1. **Own the primary documents.** You cannot credibly assert conformance to a
   document you have not read. Obtain **ANSI/IEEE Std 91-1984 + IEEE Std
   91a-1991** from the IEEE SA store and/or **IEC 60617-12** from the IEC
   webstore. *Costs and availability are the facts I could not verify and you
   must check before budgeting*: IEEE SA historically prices individual standards
   in the low hundreds of USD with member discounts, and IEC in the low hundreds
   of CHF; both figures are unverified here. Two status questions also need
   checking at the source rather than trusting this document:
   - IEEE 91-1984/91a-1991 appear to have been moved to inactive/withdrawn status
     — **unverified**. A withdrawn-but-universally-used standard is normal in this
     field (`docs/standards-landscape.md` §0 says so explicitly) and does not
     invalidate a conformance claim; it does mean the claim must cite the edition
     and note the status.
   - IEC 60617 parts were consolidated into the online **IEC 60617 database**
     (the paper parts having been withdrawn); if so, the citable artifact is a
     database subscription and a symbol reference number rather than a part
     number — **unverified**, and it changes how the conformance doc cites
     clauses. Resolve this before writing the matrix.
2. **Publish `docs/symbol-conformance.md`** with the clause-by-clause matrix,
   RFC 2119 keywords, explicit exclusions (no polarity indicators, no #45, no
   analog), and the two named non-conformances (`ShiftRegister`, `DelayGate`).
   A self-assertion that lists what it does *not* do is the only kind worth
   believing.
3. **Publish the evidence.** Name, in the doc, the tests that pin each row
   (the matrix's last column) and the exported reference sheet. Generate
   `docs/symbols-reference.svg` from a fixture circuit via
   `jls -i docs/symbols-reference.svg -symbols rectangular examples/…` as a build
   step or a scripted refresh under `scripts/`, so the picture in the docs is
   produced by the code it documents and cannot go stale.
4. **State the version the claim applies to** and add a CHANGELOG entry. The
   claim is versioned like everything else under SemVer (`#169`).
5. **Have one person who is not you check it.** The cheapest external validation
   available is a European instructor who teaches IEC symbols reviewing the
   reference sheet. That is not certification, but it is the only outside
   signal in this whole item, and it is free.

**Validity, renewal, maintenance.** No expiry, no renewal fee, no surveillance
audit. The maintenance burden is entirely internal and entirely handled by
`SymbolConformanceMatrixTest`: every new element forces a matrix row. The
external event that invalidates the claim is a **revision of the underlying
standard** — if IEC or IEEE republishes, the doc must be re-checked against the
new edition and the claim re-dated. Set no calendar reminder; note in the doc the
edition cited and let the next reader check.

**What would invalidate it in practice:** shipping an element drawn with a
qualifying symbol the standard does not define while still claiming conformance;
letting the matrix and the code drift (prevented by test 3); or advertising
"IEC 60617-12 compliant" in the README while the default mode is distinctive
shapes. The README wording must be "supports IEC/IEEE rectangular symbols
(see `docs/symbol-conformance.md`)", never a bare compliance badge.

---

### Effort, risk, and failure modes

**Sizing: 8–12 maintainer-days.** Reasoning, from the code actually read:

| Slice | Days | Why |
|---|---|---|
| `SymbolSet` + `JLSInfo` static + `UserPrefs` key + Global menu + `MenuBarSpecTest` + `-symbols` flag + `CliFlagTableTest` | 1 | All of it is copy-the-`Theme`-pattern work; the flag table generates its own usage |
| `GateOutline.Label` + `Gate.outline(SymbolSet)` + rectangular builder + `GateRenderer` branch + 7 leaves | 1.5–2 | The geometry is a rectangle in an envelope that already exists; the fiddly part is the input-stub branch and four orientations |
| Qualifying symbols + dependency labels on Mux/Decoder/Adder/Register/ShiftRegister/Memory/TriState | 2–3 | Seven renderers × four orientations of label placement; `RegisterRenderer` is 346 lines of per-orientation branching and will be the slowest |
| Palette icon rendering for the GATES group | 0.5–1 | Small, but 16 px rendering always costs more than expected |
| Tests (5 new classes + 6 extended) | 2–3 | `RectangularSymbolTest` alone is `MuxSymbolTest`-sized (hand-written probes × orientations × input counts) |
| `docs/symbol-conformance.md` + help page + Map.jhm + TOC + ARCHITECTURE decision + landscape update + CHANGELOG | 1.5–2 | The matrix is the deliverable and writing it *against the standard* is slow, careful work |

Plus **0.5–1 day of reading the primary documents** before any code, which is not
optional and is not padding.

**Top three ways this goes wrong.**

1. **Conformance theater.** The project draws rectangles with `&` in them,
   nobody buys the standard, the matrix is written from Wikipedia, and JLS
   claims IEC 60617-12 conformance it cannot defend. This is the failure mode
   the repo's own culture is most allergic to (`docs/standards-landscape.md` §6
   calls EDIF adoption "conformance theater" in exactly these words). It is also
   the *likely* outcome if the document purchase is deferred "until later".
   Mitigation: the primary documents are step zero, and if they are not obtained,
   ship the feature with the conformance claim removed (see the abort condition
   below).
2. **Scope creep into a symbol-library engine.** Dependency notation is deep —
   nested dependencies, common-output elements, arrays with weighted inputs,
   embedded symbols — and it is genuinely interesting, which makes it dangerous.
   The gravitational pull is toward a general "IEC symbol description language"
   in `jls.core`, then a symbol editor, then user-defined symbols. Mitigation:
   the scope table above is the contract; `SymbolConformanceMatrixTest` makes
   adding rows deliberate; anything the table marks *out of claim* stays a plain
   labelled rectangle forever.
3. **Geometry drift breaking existing circuits.** Someone decides the rectangle
   "needs" to be wider or that input spacing should change, edits `Gate.init()`,
   and every saved circuit's wires now attach at the wrong place — or the
   distinctive mode silently changes and nobody notices because they were staring
   at the rectangular one. Mitigation: the `init()`-is-untouchable rule in step 5,
   `SymbolSetParityTest`, `RenderBoundsTest`, and the existing
   `test/resources/orientation-geometry.txt` golden.

Two smaller ones worth naming: the SVG golden turning out to be font-sensitive
enough to flake across the Linux/Windows/macOS CI matrix (mitigated by asserting
on path data only), and the `≥` / `Σ` / `Q̄` glyphs failing to render in a
minimal container that ships no fonts — the README already warns that headless
batch image export needs at least one installed font, and the rectangular set
raises the glyph-coverage requirement beyond ASCII. Add a note to
`docs/symbol-conformance.md` and consider an ASCII fallback (`>=1`) only if a
real user hits it; do not build it speculatively.

**Do NOT do this if:**

- **You will not obtain IEEE 91-1984/91a-1991 or IEC 60617-12.** Then build the
  feature if you want the pedagogy, call it "rectangular (IEC-style) symbols",
  and make **no conformance claim** — which also means not updating
  `docs/standards-landscape.md` §1. Half the value, a third of the cost, and
  honest. This is the most likely branch and it is a perfectly good outcome.
- **The gate model/render split from #77 is still in motion.** This work sits
  directly on `GateOutline`/`GateRenderer`; landing it mid-refactor doubles the
  merge cost. Check that `HeadlessCoreRatchetTest`'s baseline is stable for
  `jls.elem` first.
- **No user has asked and no course is waiting.** The pedagogical argument
  (European curricula teach IEC symbols) is real but currently hypothetical in
  this tree — there is no issue, no user report, and no course cited anywhere in
  `docs/`. Ten maintainer-days is the same budget as the VPAT/ACR
  (`docs/standards-landscape.md` **§13.2 item 1**, line 753 — the survey's §13
  was split into §13.1 logic-design conformance and §13.2 institutional
  conformance in commit `9ab4797`, so any "§13 item N" citation predating that
  split is stale), which `docs/standards-landscape.md` §12.d says is "the
  certification family
  with the highest real-world probability of being demanded of JLS". If you can
  only do one this quarter, **do the VPAT.** This item is the better *engineering*
  and the worse *bet* until an instructor asks.

---

### Sources

**Primary standards documents (all UNVERIFIED — not consulted in this pass; the
scope table's symbol assignments are from general engineering knowledge and MUST
be checked against the documents before any conformance claim):**
- ANSI/IEEE Std 91-1984, *IEEE Standard Graphic Symbols for Logic Functions*, and
  IEEE Std 91a-1991 (supplement). Publication status, current edition, and price
  unverified.
- IEC 60617-12, *Graphical symbols for diagrams — Part 12: Binary logic elements*.
  Whether the citable artifact is the part or the IEC 60617 online database is
  unverified.
- IEEE 315 / ANSI Y32.2 — named only to exclude it (#45).

**Repository files read and cited (verified):**
- `src/jls/elem/Gate.java` — `init()` sizing and put placement (lines 168-248),
  `getRect()` (489-498), abstract `outline()` (259), declarative attributes
  (264-328).
- `src/jls/elem/GateOutline.java` — `Segment` record, `Kind` enum, `Builder`.
- `src/jls/elem/AndGate.java`, `OrGate.java`, `NotGate.java`, `NandGate.java`,
  `NorGate.java`, `XorGate.java`, `DelayGate.java`, `Extend.java` — the
  distinctive-shape outlines and `Kind` descriptors.
- `src/jls/edit/GateRenderer.java` — `gatePathFrom` (140-172), orientation
  transform and input fan-out (56-124).
- `src/jls/edit/ElementRenderers.java`, `ElementRenderSupport.java`,
  `BuiltinElementRenderers.java` — the renderer registry and shared helpers.
- `src/jls/edit/MuxRenderer.java`, `DecoderRenderer.java`, `AdderRenderer.java`,
  `RegisterRenderer.java` (D/C/Q labels and dynamic-input triangle, lines 91-137),
  `ShiftRegisterRenderer.java`, `MemoryRenderer.java`, `StateMachineRenderer.java`,
  `TriStateRenderer.java`.
- `src/jls/edit/CircuitRenderer.java` — `exportImage` (301-390, SVG determinism
  and draw ordering), `addToBook` printing path.
- `src/jls/edit/Palette.java`, `PaletteEntry.java`, `src/jls/edit/images/` (33 GIFs).
- `src/jls/UserPrefs.java` — key/`applyStartup`/fallback pattern.
- `src/jls/Theme.java`, `src/jls/JLSInfo.java` — the "rewrite statics the drawing
  code already reads" precedent.
- `src/jls/JLSStart.java` — `FLAGS` table (759-795), `globalMenu`/Color scheme
  (1873-1975), `prefs.applyStartup()` (1230), batch `-i` path (300-360).
- `src/jls/core/Geometry.java` — `SPACING = 12`, `POINT_DIAMETER = 6`.
- `src/jls/module/`, `docs/extension-points.md` — why this is not an extension
  point (`gui.theme` seam is pending under #76).
- `test/jls/elem/GateOutlineParityTest.java`, `MuxSymbolTest.java`,
  `OrientationGeometryTest.java`, `test/resources/orientation-geometry.txt`.
- `test/jls/ui/package-info.java` (the three-layer harness rules and the
  no-pixel-goldens discipline), `RenderAssert.java`, `RenderBoundsTest.java`,
  `MenuBarSpecTest.java` (menu expectation block, lines 95-127).
- `test/jls/ElementDrawSmokeTest.java`, `SvgExportTest.java`,
  `CliImageExportTest.java`, `CliFlagTableTest.java`, `HelpTopicsTest.java`,
  `HeadlessCoreRatchetTest.java`, `ExtensionPointCatalogTest.java`,
  `test/jls/edit/PaletteContractTest.java`.
- `docs/standards-landscape.md` §3 (#43-#45, lines 179-181), §13.1 item 1
  (line 724), §14.
- `docs/batch-interface.md` §1 (flag table, exit codes) and §6 (stability
  promise, additive-flag rule).
- `docs/file-format.md` §9 (evolution policy) — cited to show it is *not* touched.
- `ARCHITECTURE.md` (test layout, recorded decisions), `CONTRIBUTING.md`
  (`mvn verify` gate, coverage ratchet), `README.md` (font requirement for
  headless image export), `pom.xml` (`display` tag surefire split),
  `.github/workflows/ci.yml` (Linux xvfb lane).

**Claims I could not verify and that must be checked before work starts:** the
publication status, current edition and price of IEEE 91-1984/91a-1991 and
IEC 60617-12; whether IEC 60617-12 exists as a citable part or only inside the
IEC 60617 database; the exact clause numbers for every row of the scope table;
the precise specified proportions of the negation circle, dynamic-input triangle
and three-state output indicator; and whether IEC 60617-12 carries the
distinctive shapes in an annex (which would let the *existing* mode be claimed
too, and would be a pleasant and cheap bonus if true).
