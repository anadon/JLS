# TASK-0036 - Per-view geometry section and the op view discriminator

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0033, TASK-0035

## Deliverable

Geometry becomes per-view data in its own optional versioned section, a reader
that does not know a view preserves that view's section verbatim, and the four
geometric ops carry a view discriminator with exact inverses.

1. **The `VIEW` section.** One optional (skip-and-preserve) section per view, in
   TASK-0033's frame, carrying rows of
   `ItemKey -> (x, y, width, height, orientation)`. Rows are sorted by
   `ItemKey`'s canonical order, so the section is a pure function of content.
   The `schematic` view keeps writing geometry where it writes it today -
   `Element`'s `x`/`y`/`width`/`height` base attributes
   (`src/jls/elem/Element.java:215-256`) - and does **not** get a `VIEW`
   section; only non-default views do. That keeps every existing file and
   golden byte-identical.
2. **No second `(x,y)` on `Element`.** `Element` has exactly one pair
   (`:28,30`), one `setXY` (`:72-76`), one `savePosition`/`restorePosition`
   (`:452-465`). A second view's geometry lives in the side table, reached
   through a `ViewGeometry` service on `Circuit`, never on the element.
3. **The op view discriminator.** `MoveElements`
   (`src/jls/collab/op/MoveElements.java:26-27`), `RotateElement` (`:20-21`),
   `FlipElement` and the geometry carried by `AddElements` gain a `String view`
   component. Serialized as an **optional** ` String view` line whose absence
   means `schematic`, so every op byte sequence written today still parses and
   `CircuitOpTest`'s serialization assertions stay byte-identical.
4. **The reader.** `CircuitOpReader`'s per-kind field validation
   (`src/jls/collab/op/CircuitOpReader.java:119-178`) gains the optional field
   for exactly those four kinds and continues to **reject** it on the others -
   the grammar is strict, and an unknown-for-this-kind field is a rejection, not
   a repair.
5. **Exact inverses, per view.** `MoveElements.invert` returns the negated delta
   *in the same view*; `RotateElement.invert` the opposite direction in the same
   view. A move in view A must not be undone in view B. This is asserted, not
   assumed.
6. **Documentation.** The serialized-form block in `docs/operation-layer.md`
   (the `OP <kind>` grammar listing) gains the `view` line and the default rule;
   `docs/file-format.md` gains the `VIEW` section row.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-014 | Per-view geometry and the op view discriminator are two of the four halves of the addressing scheme; the other two are TASK-0035's. |
| FEAT-043 | A breadboard canvas is a second view. Without a per-view geometry section its placements have nowhere to live, and without a view-qualified move op its drags are indistinguishable from schematic drags on the wire. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0033 | The `VIEW` section is a section in that frame and depends on its optional/must-understand flag and its skip-and-preserve reader; without it an unknown view makes the file unopenable rather than partially understood. |
| TASK-0035 | The section's rows are keyed by `ItemKey`, and the op's `view` token is the first field of that key. This task reads the key type TASK-0035 creates. |

## Acceptance test

`test/jls/ViewGeometrySectionTest.java`, new:

- `schematicGeometryStaysInTheElementBlocks()` - a circuit with only the default
  view saves byte-identically to its pre-change bytes. This is the regression
  guard for every existing golden.
- `aSecondViewsGeometryRoundTrips()` - place the same elements in a second view
  at different coordinates; save; load; assert both views' coordinates.
- `editingOneViewLeavesTheOtherSectionByteIdentical()` - move an element in view
  B, save, and assert view A's stored section bytes are unchanged.
- `anUnknownViewSectionIsPreservedVerbatim()` - hand-build a file with a
  `VIEW` section naming a view this build does not register; open and save;
  assert the section's bytes are identical, in the same position.

`test/jls/collab/op/ViewDiscriminatorTest.java`, new:

- `aMoveInOneViewDoesNotMoveTheOther()` - the isolation property.
- `theInverseOfAViewQualifiedMoveIsInTheSameView()` - apply, invert, apply the
  inverse, assert the canonical save is byte-identical to the pre-apply bytes,
  in both views.
- `anAbsentViewFieldMeansSchematic()` - parse an op block written before this
  change and assert it applies to the default view.
- `theViewFieldIsRejectedOnKindsThatDoNotTakeIt()` - one case per non-geometric
  kind, asserting `OpRejected` and not silent acceptance.

## Related GitHub issues

**No issue.** Multi-view geometry and the op view discriminator are unfiled;
decision D3's consequences and `10-capstone-plan.md` §7.2's reserved shape.

| # | title | relationship |
|---:|---|---|
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | overlaps - this extends the vocabulary #167 built; the op grammar is #167's surface |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | depends on - a new optional field widens the network-facing grammar and must land inside #170's strictness discipline, not beside it |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs - "CS rewires while EE footprints" is the multi-view merge case |

## Notes

- **Extend the op grammar once, not twice.** `10-capstone-plan.md` §7.2 is
  explicit: the op shape is in the wire envelope *and* the undo stack, so
  extending it for multi-view and again for collaboration means extending it
  incompatibly. If the nine-kind shape in §7.2 is going to land, the `view`
  field lands with it.
- **`CircuitOp` is a sealed interface.** Its `permits` list
  (`src/jls/collab/op/CircuitOp.java:36-38`) names eleven records; adding a kind
  edits the permits list *and* the reader switch, and the compiler will not
  remind you about the second. This task adds no kinds - it adds a component to
  four existing records - but the same discipline applies to the reader.
- **A record component change is a constructor change.** `MoveElements` and
  `RotateElement` are records with compact constructors; every construction site
  in `SimpleEditor`'s gesture planners (`moveSelectionPlan`,
  `deleteSelectionPlan`, the keyboard nudge) and every test in
  `test/jls/collab/op/CircuitOpTest.java` must be updated in the same change.
- **`ElementBlocks` carries geometry, so `AddElements` does too.** A block is
  the element's exact save bytes (`src/jls/collab/op/ElementBlocks.java:48`),
  and the base attributes in it include `x`/`y`. An add into a non-default view
  must therefore carry the view *and* the side-table row, or the element arrives
  at its schematic coordinates in the breadboard.
- **Do not let the section become a second source of truth for the default
  view.** If `schematic` ever gets a `VIEW` section, two places store the same
  coordinates and they will diverge. The one-view-is-special rule is deliberate;
  write it down where a later author will read it.
- **Orientation is geometry.** `Rotatable.rotate` and `flip` mutate element
  state (`src/jls/collab/op/RotateElement.java:24-32`,
  `FlipElement.java:20-27`), so a per-view orientation belongs in the section
  row, not on the element, for non-default views.

## Evidence

- `src/jls/elem/Element.java:28,30,72-76,452-465` - exactly one coordinate pair
  and one save/restore pair; `:200-256` the base attribute list that carries
  `x`, `y`, `width`, `height`.
- `src/jls/collab/op/MoveElements.java:26-27` - `MoveElements(List<ElementId>,
  int dx, int dy)`, verified to have no view field; `:69-77` the inverse.
- `src/jls/collab/op/CircuitOpReader.java:119-178` - the per-kind
  `requireFields` switch and the unknown-kind rejection at `:178`.
- `docs/operation-layer.md` - the serialized-form grammar block and the
  mutation-site inventory; the contract that a rejected op leaves the circuit
  byte-identical.
- `10-capstone-plan.md` §7.1 ("Do not add a second `(x,y)` to `Element`" - use a
  side table keyed by (view, stableId)) and §7.2 (extend the op grammar once).
- Do not restate: `docs/operation-layer.md` owns the op contract;
  `docs/file-format.md` owns the section table once TASK-0033 creates it.
