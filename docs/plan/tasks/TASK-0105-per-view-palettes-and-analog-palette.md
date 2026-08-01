# TASK-0105 - Per-view palettes and the analog palette

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0036

## Deliverable

The palette contract gains a view dimension, so "one entry per registered type"
becomes "exactly one entry in exactly one view's palette" - and the first-year
toolbar stops growing every time a registered element type is added.

Precisely what changes:

1. **`src/jls/edit/PaletteEntry.java`** gains a `String view` component
   alongside `type`, `group`, `iconName`, `fallbackText`, `tooltip` and
   `helpTopic`, defaulting to `"schematic"`. The token vocabulary is **the same
   one TASK-0036 puts on the `VIEW` section and on the geometric ops**; there
   must not be two view vocabularies.
2. **`src/jls/edit/Palette.java`**: `Group` gains a `view` (`:38-92`), the
   `entry(...)` helper (`:214`) takes it, `entries()` (`:234`) keeps returning
   everything, and a new `entries(String view)` and `groups(String view)` return
   the view's slice in declaration order. The 32 existing rows (`:123-190`) all
   declare `"schematic"`, so the default view is bit-for-bit what ships today.
3. **`src/jls/edit/SimpleEditor.java:2312-2321`** - the toolbar builder that
   walks `Palette.groups()` and `Palette.entries(group)` - takes the active
   view and walks the view-scoped accessors instead. **Visibility is derived
   from the model, not from a preference**: the analog group renders if and only
   if the editing context is a `SubCircuit` whose implementation attribute is
   the analog one. A first-year cannot reach a state where the analog group
   exists, because creating an analog subcircuit is an explicit named action.
4. **`test/jls/edit/PaletteContractTest.java:47-66`** -
   `paletteIsTotalOverTheElementRegistry` - is rewritten to
   `everyRegisteredTypeHasExactlyOneEntryInExactlyOneView()`. `NON_PALETTE_TAGS`
   (`:44-45`) is unchanged. The group-capacity check (`:123-138`) and the
   toolbar-order check (`:140-151`) become per-view.
5. **`test/jls/edit/K9PaletteRatchetTest.java`**, new - the ratchet the parity
   study says must exist before K9 is anything but aspiration. It pins the
   default view at **exactly 32 buttons**, pins that no non-schematic view's
   entries appear in the default toolbar's component tree, and turns the startup
   and per-edit cost figures into assertions rather than prose.
6. **Two new views declared, empty except where their tasks fill them:**
   `"analog"` (filled by TASK-0102's bridges and TASK-0103's devices, plus an
   `AnalogProbe`) and `"breadboard"` (filled by TASK-0092). Declaring them here
   and leaving them empty is deliberate: it makes the view dimension load-bearing
   and testable before the elements that need it exist.
7. **`src/jls/collab/op/ElementVocabulary.java:39-45`** - the hand-maintained
   34-token network allowlist that `ElementVocabularyTest` cross-checks against
   the palette contract - must be updated in the same change or that
   cross-check breaks. It is a **deny-listed** update, not an automatic widening:
   see Notes.
8. **`docs/extension-points.md`**: the `gui.palette-contributor` row's contract
   note records that a contribution now carries a view, and
   `docs/virtual-hardware-parity.md`'s K9 clause gains a pointer to the ratchet
   test that now enforces it.

Done means: the default toolbar is byte-identical to HEAD's, a registered
analog type has a palette row that a first-year never sees, and the ratchet
fails if either statement stops being true.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-049 | 22 analog element types would otherwise land as 22 mandatory first-year toolbar buttons - a 69% palette growth, enforced by a passing test. |
| FEAT-043 | The breadboard canvas is a second view with its own element set; without a view dimension its parts join the logic toolbar. |
| FEAT-029 | The N-ary element family is a third population that must not appear in a binary-logic first course. |
| FEAT-008 | Palette construction is one of the concrete things the editor decomposition moves behind a seam; doing it once, with the view dimension already present, avoids doing it twice. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0036 | The view token this task keys the palette off is the same `String view` TASK-0036 puts on the `VIEW` section and on `MoveElements`/`RotateElement`/`FlipElement`/`AddElements`. This task reads a vocabulary that task creates. Minting a second one here is exactly the "solved twice, incompatibly" failure the multi-view and collaboration work is one program to avoid. |

## Acceptance test

`test/jls/edit/PaletteContractTest`, extended:

- `everyRegisteredTypeHasExactlyOneEntryInExactlyOneView()` - replaces the
  current totality test. Builds the expected tag set from
  `ElementRegistry.all()` minus `NON_PALETTE_TAGS`, builds the actual set as the
  union over views, and additionally asserts no tag appears in two views.
- `everyGroupIsNonEmptyAndFitsItsDeclaredGridWithinItsView()` and
  `entriesComeInToolbarGroupOrderWithinEachView()` - the existing shape,
  per-view.
- `everyDeclaredViewHasAtLeastOneGroupOrIsExplicitlyEmpty()` - so an
  accidentally-unreferenced view is a failure rather than an invisible typo.

`test/jls/edit/K9PaletteRatchetTest.java`, new:

- `defaultViewShowsExactlyThirtyTwoButtons()` - the number, asserted. It changes
  only by a reviewed edit to this test.
- `analogGroupIsAbsentUnlessTheContextIsAnAnalogSubcircuit()` - build the toolbar
  for a plain top-level circuit and assert no component whose accessible name is
  an analog element's tooltip exists; then build it for an analog subcircuit
  context and assert they do.
- `noPreferenceCanMakeTheAnalogGroupVisibleInTheDefaultView()` - the visibility
  predicate takes the model, not `UserPrefs`; asserted by construction.

`test/jls/collab/op/ElementVocabularyTest` gains
`theNetworkAllowlistIsTheRegistryMinusTheExplicitDenyList()`, so the three-way
cross-check survives the palette change with the widening made explicit.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 84 | Decompose SimpleEditor: 4,119 lines, a 9-state mouse machine, a 305-line source== dispatcher that already caused #37, and whole-circuit undo snapshots | overlaps - `#84` is what routes palette construction through the contributor seam; this task changes the shape of what flows through it, so the two must agree on the entry type |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the two-layer descriptor split (`ElementType` core, `PaletteEntry` GUI) is #78's; the view belongs on the GUI half, not the core half |
| 75 | Keyboard operability and accessibility: focus follows the mouse, the menu bar has zero accelerators/mnemonics, and no element can be manipulated without a mouse | overlaps - a view-scoped toolbar changes the keyboard traversal order; `PaletteButtonAccessibilityTest` and `FocusFaithfulKeyboardTest` both read the palette |
| - | no issue | The analog palette itself, and the view dimension, have no tracking issue. |

## Notes

- **A currently-green test enforces the violation.** This is the unusual case
  where the obstacle is a passing assertion, not missing code.
  `PaletteContractTest.paletteIsTotalOverTheElementRegistry`
  (`test/jls/edit/PaletteContractTest.java:47-66`) asserts one palette entry per
  registered type outside `{SubCircuit, WireEnd, TestGen}`, and an *unregistered*
  type cannot round-trip through the save format at all - so there is no way to
  add an analog element without adding a first-year toolbar button. Rewriting
  the test is the deliverable, not a side effect.
- **The rewrite must not weaken totality.** "Exactly one entry in exactly one
  view" is strictly stronger than a per-view totality check, which a typo could
  satisfy by putting a gate in two views. Assert the cross-view uniqueness.
- **Do not widen the network allowlist automatically.**
  `ElementVocabulary`'s own javadoc says it "should delegate to" the registry
  once the registry exists - and the registry does exist, with 35 types against
  the allowlist's 34. A naive delegation silently admits `TestGen` from a
  network peer, which is exactly the widening the collaboration hardening
  forbids. The delegation is registry **minus an explicit, tested deny list**.
- **K9 becomes a visibility gate, not an existence gate.** The pedagogy floor is
  "no regression for a first-year student drawing an adder". Under progressive
  disclosure the machinery may exist as long as the first-year never sees it:
  views default-hidden and opt-in, palettes per-view rather than one growing
  global palette, startup and per-edit cost still ratcheted, conceptual load in
  the default view unchanged.
- **The palette becomes a correct description of what is legal.** The analog
  region already refuses `Clock`, `Stop`, `Pause`, `Display` and `SigGen`
  inside a bound region; a per-view palette stops offering buttons that the
  model would reject.
- **Ship this before the first analog element**, not with it. It is roughly two
  days of the two weeks; the rest is the three-way cross-check and the
  accessibility fallout.

## Evidence

- `test/jls/edit/PaletteContractTest.java:26` (the contract statement),
  `:44-45` (`NON_PALETTE_TAGS`), `:47-66` (the totality assertion),
  `:123-138` (group capacity), `:140-151` (toolbar order).
- `src/jls/edit/Palette.java:38-92` (the `Group` enum with rows/cols/standalone),
  `:123-190` (the 32 `ENTRIES`), `:191` (`GROUPS`), `:214` (the `entry` helper),
  `:218` (`ElementRegistry.forTag`), `:234` (`entries()`).
- `src/jls/edit/SimpleEditor.java:2312-2321` - the toolbar builder walking
  `Palette.groups()` then `Palette.entries(group)`.
- `src/jls/elem/ElementRegistry.java:38` - 35 registered types at HEAD, against
  32 palette entries; the difference is exactly the three documented
  non-palette tags.
- `src/jls/collab/op/ElementVocabulary.java:26-30` (the "delegate to the
  registry when it lands" note), `:39-45` (the 34-token list).
- `src/jls/edit/GuiExtensionPoints.java:16-28` and `docs/extension-points.md:31`
  - the palette-contributor seam and its "typed now (#78 shipped; #84 consumes)"
  status.
- `11-analog-determination.md` §2.8 (D-A10) - the view dimension, the
  derived-from-the-model visibility rule, and the 22-types/69%-growth
  arithmetic.
- Do not restate: `docs/virtual-hardware-parity.md` owns K9;
  `docs/extension-points.md` owns the seam catalog.
