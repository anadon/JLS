# TASK-0058 - Strength lattice and pull elements

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0057

## Deliverable

Drive strength becomes a second dimension of the resolution fold, and two
drawable elements exist that can only be expressed with it.

1. **The lattice.** `jls.core.Strength`, an enum ordered
   `HIGHZ < WEAK < PULL < STRONG < SUPPLY`, and `jls.core.DrivenBit` (or the
   equivalent widening of `LogicVector` to carry a per-bit strength plane -
   decide and record which, because it is a storage decision, not a taste one).
   The resolution operator from TASK-0057 gains one rule ahead of its value
   rule: **the strictly strongest driver wins outright; equal-strength
   disagreement produces `X`; every driver at `HIGHZ` yields `Z`.** The operator
   stays commutative, associative and idempotent - the existing order-independence
   test is extended, not replaced.
2. **Driver kind on the output.** `Output` (`src/jls/elem/Output.java:15-94`,
   which today carries only `boolean triState` at `:19`) gains a
   `DriverKind { PUSH_PULL, OPEN_DRAIN, OPEN_SOURCE, PULL }`. `isTriState()`
   stays as the compatibility accessor and becomes `kind != PUSH_PULL`, so the
   four `SimpleEditor` connection sites and `WireNet.setTriState`
   (`WireNet.java:340-388`) keep working unmodified.
3. **Net kind on the net.** `WireNet` gains `NetKind { WIRE, WAND, WOR, TRI }`,
   defaulting to `WIRE`, resolved by the fold. Set from the elements attached in
   `recheck` (`WireNet.java:272-302`), never from geometry.
4. **Two new element types: `PullUp` and `PullDown`.** One input-less
   `LogicElement` each, driving a constant `1` / `0` at `Strength.PULL`, width
   taken from the net. Full registration, which at HEAD is a measured ~66-line
   tax per element across these files (from `git show --stat 38a0544`, the
   `RegisterFile` + `FieldExtend` commit: 14 files, 1,188 insertions, of which
   1,055 are the two element bodies):
   `src/jls/elem/LogicElement.java:17-21` (sealed permits),
   `src/jls/elem/ElementRegistry.java:38-77` (`ALL`),
   `src/jls/elem/SaveTags.java:41-75` (`WRITABLE`),
   `src/jls/edit/Palette.java:123+` (`ENTRIES`),
   `src/jls/collab/op/ElementVocabulary.java`,
   plus `test/jls/AllElementsRoundTripTest.java`,
   `test/jls/CircuitTextBuilder.java`, `test/jls/ElementDrawSmokeTest.java`,
   `test/jls/ElementSimulationGoldenTest.java`,
   `test/jls/edit/PaletteContractTest.java`,
   `test/jls/elem/CapabilityInterfaceTest.java`,
   `test/jls/elem/SealedHierarchyTest.java`,
   `test/jls/ui/ComponentIdentityTest.java`.
5. **`docs/simulation-semantics.md` §9 gains the lattice paragraph** in the same
   commit, per recorded decision #221's "specified, documented change ... first".

Done means: an open-drain bus with a pull-up simulates the wired-AND answer, and
the same bus without the pull-up floats to `Z` rather than to `0`.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-027 | This *is* FEAT-027's substance: the lattice, the driver kinds, the net kinds and the pull elements. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0057 | Strength is a rule applied *inside* the fold. At HEAD there is no fold - `WireNet.propagate:454-485` picks a driver by list position, and a positional winner cannot express "the strongest driver wins regardless of order". |

## Acceptance test

- **`jls.sim.StrengthResolutionTest.strongestDriverWinsRegardlessOfOrder()`**
  (new): a net with a `STRONG` 0 driver and a `PULL` 1 driver; assert the result
  is 0 for both driver orderings, and assert no bus-conflict warning is raised
  (this is the whole point - today it either warns or picks by position).
- **`jls.sim.StrengthResolutionTest.equalStrengthDisagreementIsX()`** (new):
  two `STRONG` drivers, 1 and 0; assert `X` and exactly one warning.
- **`jls.sim.StrengthResolutionTest.openDrainBusIsWiredAnd()`** (new): three
  open-drain drivers plus one `PullUp`; assert the net reads 1 only when all
  three are off, on all 8 driver combinations.
- **`jls.sim.StrengthResolutionTest.aFloatingOpenDrainBusIsZNotZero()`** (new):
  the same bus with the `PullUp` deleted; assert `Z`. This is the assertion that
  catches the silent-zero coercion the four-state core exists to remove.
- **`jls.elem.PullUpModelTest` / `jls.elem.PullDownModelTest`** (new, following
  the house pattern of `RegisterModelTest`, `MemoryModelTest`,
  `SubCircuitModelTest` in `test/jls/elem/`): construction, save/load round trip,
  orientation, and `initSim` drive value.
- **`jls.ElementRegistryTest`**, **`jls.elem.SaveTagsTest`**,
  **`jls.edit.PaletteContractTest`**, **`jls.elem.SealedHierarchyTest`**
  (all existing): each is a totality test over the registry and each fails until
  the two new types are added everywhere - that is the intended forcing function
  (FEAT-001).

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - its registry half already shipped and is the mechanism that makes adding two element types a compile error until complete; this task does not close it |
| 98 | (recorded decision, **closed**) tri-state and bus-conflict surprises S1/S6 | informs - S6's "don't repost an unchanged off" logic at `TriState.java:487-505` must survive the strength rewrite |

**No issue** exists for the strength lattice, for driver/net kinds, or for
`PullUp`/`PullDown`. Recorded as a gap.

## Notes

- **Trap: `jls.elem` is floored at 73.0/70.0/58.5** (`pom.xml:475-491`) against a
  measured 74.65/71.64/60.62, and `pom.xml:317-321` records that the floor only
  ever moves up. Two new element bodies (the measured average for the last two shipped
  elements was 528 lines, though a pull element is far smaller) is enough new
  uncovered code to trip the floor if the model tests are deferred. Ship
  `PullUpModelTest`/`PullDownModelTest` in the same commit as the elements, not
  after; `07-mvl-determination.md` §1.3 does this arithmetic for the eight N-ary
  types and reaches ~4-6 new element classes per release.
- **Trap: `WireNet.setTriState` propagates to `TriProp` implementors.**
  `src/jls/elem/WireNet.java:340-388` walks the net and calls
  `TriProp.setTriState` (`src/jls/elem/TriProp.java:13`) on every attached
  element, and `makeNet:150-162` / `recheck:289-301` *un*-tri-state elements when
  the net loses its last tri-state driver. `PullUp` on a net makes it
  strength-resolved but not tri-state; if `PullUp` implements `TriProp` the
  un-tri-stating walk will silently disable it. Decide explicitly and pin with a
  test.
- **Trap: a new element type costs zero format version** (BRIEF §13, verified:
  unknown *tags* are a hard error at `docs/file-format.md` §3, so no old reader
  ever silently mis-loads a `PullUp`). But `DriverKind` and `NetKind` as *saved
  attributes on existing elements* are exactly the class `docs/file-format.md:470`
  says must bump the version rather than ship as ignorable. Keep them
  derived from element identity, not saved, or take the version bump knowingly.
- **Trap: `isTriState()` has 49 call sites in `src/jls`** (measured) across
  `jls.elem` and `jls.edit`, including the four `SimpleEditor` connection checks. Redefining it
  in terms of `DriverKind` is safe only if `PULL` counts as tri-state for
  connection purposes - a `PullUp` must be attachable to a net that already has
  a driver.

## Evidence

- `src/jls/elem/Output.java:19` - `private boolean triState = false;` is the
  entire driver model at HEAD.
- `src/jls/elem/WireNet.java:29-30, 340-398` - the net's tri-state flag and its
  propagation; `:404-407` - the net's cached value and `conflictReported` latch.
- `src/jls/elem/TriProp.java:6-13` - the whole tri-state capability interface.
- `src/jls/elem/LogicElement.java:17-21` - 24 permits at HEAD; `PullUp` and
  `PullDown` make 26.
- `src/jls/elem/ElementRegistry.java:38-77` - 35 registered types at HEAD.
- `git show --stat 38a0544` - the measured 14-file registration surface and the
  ~66-lines-per-element registration tax.
- `pom.xml:475-491` (the `jls.elem` floor), `pom.xml:317-321` (floors only rise).
- `docs/simulation-semantics.md` §9 - the paragraph this task rewrites.
