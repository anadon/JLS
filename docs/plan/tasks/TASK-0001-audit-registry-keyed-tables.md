# TASK-0001 - Audit and pin every registry-keyed table

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A written inventory, committed as `docs/registry-keyed-tables.md`, of every
table in `src/` whose key set is (or should be) the element registry, an
`Orientation`, an `EditOp` or a save tag; plus a totality test for each entry
that has none today.

Inventory rows must record, per table: the declaring file and field, how it is
populated (declarative literal vs imperative `register(...)` calls), what
happens on a miss (throw / silent fallback / silent drop), the intended
exemption set with its reason, and the test that pins it.

The tables that already have totality tests, to be recorded as covered, not
rebuilt:

| Table | Anchor | Pinned by |
|---|---|---|
| `Palette.ENTRIES` | `src/jls/edit/Palette.java:124-188` | `PaletteContractTest.paletteIsTotalOverTheElementRegistry` (`test/jls/edit/PaletteContractTest.java:48`), exemptions `SubCircuit`/`WireEnd`/`TestGen` at `:44-45` |
| `SaveTags.WRITABLE` | `src/jls/elem/SaveTags.java:41-76` | `ElementRegistryTest.everyWritableRegisteredTagIsInTheFrozenTagTable` (`test/jls/ElementRegistryTest.java:124`) |
| `HdlExporter` EXPORTED/SKIPPED/TOPOLOGY/`REJECTED` | `src/jls/hdl/HdlExporter.java:460` | `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` (`test/jls/hdl/HdlPolicyTest.java:392`), added by `b54e6ee` |
| `ElementVocabulary.ALLOWED` | `src/jls/collab/op/ElementVocabulary.java:39-46` | `ElementVocabularyTest.everyVocabularyTokenIsACreatableElement:49`, `vocabularyIsThePaletteSetPlusWireEnd:121` |

The tables with **no** totality check, each of which this task must pin:

1. `ElementRenderers.BY_TYPE` (`src/jls/edit/ElementRenderers.java:24-25`) - a
   mutable `HashMap<Class<?>, ElementRenderer>` filled by 35
   `ElementRenderers.register(...)` calls in
   `src/jls/edit/BuiltinElementRenderers.java:38-155`. A miss falls back
   silently.
2. `ElementDialogs.BY_TYPE` and `ElementDialogs.CHANGE_BY_TYPE`
   (`src/jls/edit/ElementDialogs.java:25-29`) - 33 `register` and 8
   `registerChange` calls in the same installer. On a miss,
   `ElementDialogs.change` and `setup` fall through to the element's own
   method (`src/jls/edit/ElementDialogs.java:78-80`), so an unregistered
   dialog is invisible.
3. `DialogConstructionSmokeTest` (`test/jls/ui/DialogConstructionSmokeTest.java:149-225`)
   - one hand-written `@Test` per dialog. A new element type adds no test.
   Rewrite as a registry-driven `@ParameterizedTest` over
   `ElementRegistry.all()` minus a named exemption set.
4. `ElementVocabulary.ALLOWED` is covered but is a hand-maintained duplicate of
   the registry; its own javadoc (`:27-30`) says it should delegate to the
   registry once #78 ships, which it has. Record the reconciliation decision -
   delegate or keep the literal with a derived-equality test - and implement it.

Also in scope, folded here rather than given its own id: reject incompatible
batch flag combinations at parse time in `JLSStart`'s `FLAGS` table
(`src/jls/JLSStart.java:760-788`), which today declares arity per flag
(`FlagSpec`, `:741`) but nothing about mutual exclusion; `CliFlagTableTest`
(`test/jls/CliFlagTableTest.java:82-131`) checks name agreement only.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-001 | The inventory is the feature's subject matter: without knowing which tables are registry-keyed, "totality discipline" has no scope. |
| FEAT-002 | Names the fall-through sites (renderer, dialog, vocabulary) that the fail-loud discipline must convert from silent to diagnostic. |

## Prerequisite tasks

None. This is a read-and-pin task over HEAD.

## Acceptance test

`test/jls/edit/ElementRendererContractTest.java`, new, mirroring
`PaletteContractTest`:

- `renderersAreTotalOverTheElementRegistry()` - for every
  `ElementType` in `ElementRegistry.all()` outside a declared exemption set,
  asserts `ElementRenderers` has a registered renderer; fails naming the
  missing tag and the `BuiltinElementRenderers.install()` line to add.
- `creationDialogsAreTotalOverTheElementRegistry()` - the same over
  `ElementDialogs.BY_TYPE`, with an exemption set naming each type that
  legitimately has no dialog and why.

`test/jls/ui/DialogConstructionSmokeTest` gains
`everyRegisteredDialogConstructsAndCancels(ElementType)` as a
`@ParameterizedTest` sourced from `ElementRegistry.all()`, replacing the 20+
hand-written per-dialog methods; it asserts construction and cancel leave no
stray window, reusing the existing `constructAndCancel` helper (`:117`).

`test/jls/CliFlagTableTest` gains `incompatibleFlagCombinationsAreRejectedByName()`,
asserting a non-zero exit and a message naming both flags for each declared
incompatible pair.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - its registry and `Orientation` halves already shipped (`src/jls/elem/ElementRegistry.java:38`, `src/jls/core/Orientation.java:12`); this task builds on them rather than closing it |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | overlaps - owns `ElementVocabulary`; item 4 above is the reconciliation #170's javadoc defers to #78 |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | overlaps - the parameterized rewrite of `DialogConstructionSmokeTest` is how "all 23" stops being a hand-counted number |

## Notes

- **The precedent is measured, not hypothetical.** `970db41` fixed `SaveTags`
  missing `RegisterFile`/`FieldExtend`; `b54e6ee` fixed `HdlExporter` missing
  the same two. Both were tables without a totality check and both drifted on
  the same merge (#201). The renderer and dialog tables are the same shape and
  have not been checked.
- **`ElementRenderers`/`ElementDialogs` are GUI-side**, so a totality test needs
  the headless display substrate; run it in the lane
  `DialogConstructionSmokeTest` already uses (`requireDisplayAndStartCloser`,
  `test/jls/ui/DialogConstructionSmokeTest.java:65`), not the required fast lane,
  until TASK-0016 splits the lanes.
- **Static-initializer trap.** `ElementDialogs` populates its maps from a static
  block calling `BuiltinElementRenderers.install()`
  (`src/jls/edit/ElementDialogs.java:31-33`). A test that reads `BY_TYPE`
  without touching `ElementDialogs` first sees an empty map. Force
  initialization explicitly.
- **Do not add a `default:` arm** to any switch this audit touches;
  `CONTRIBUTING.md` "Sealed dispatch" forbids it over `Element`/`Gate`,
  `SimEvent.Payload` and `Orientation`, and `test/jls/elem/SealedHierarchyTest.java`
  pins the permits tree.
- `TestGen` is registered but never written (`ElementRegistry.java:33-37`); every
  exemption set must name it deliberately, as `PaletteContractTest:44` does.

## Evidence

- `src/jls/elem/ElementRegistry.java:38-77` - 35 registered types.
- `src/jls/edit/BuiltinElementRenderers.java:38-155` - 35 renderer, 33 dialog,
  8 change-dialog registrations; 156 lines total.
- `src/jls/edit/ElementDialogs.java:25-29, 31-33, 78-80` - the maps, the static
  installer, the silent fallback.
- `src/jls/edit/ElementRenderers.java:24-25` - the untested map.
- `test/jls/ui/DialogConstructionSmokeTest.java:117, 149-225` - the helper and
  the hand-written per-dialog tests.
- `git show --stat 970db41`, `git show --stat b54e6ee` - the two measured drift
  incidents.
- BRIEF.md §13 CORRECTIONS: "35 types; 27 `react` impls", "Every element/react
  count in `docs/capability-roadmap/` is stale".
