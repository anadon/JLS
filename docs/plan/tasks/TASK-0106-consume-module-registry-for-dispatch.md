# TASK-0106 - Consume the module registry for dispatch, with a typed catalog

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

The module registry stops being populated and ignored. Every seam in the
catalog gains a typed contract, a cardinality and a lifecycle phase that a test
checks in both directions, and the four dispatch sites that bypass the registry
today read it instead.

Precisely what changes:

1. **The booted runtime becomes reachable.** `src/jls/JLS.java:60` calls
   `JlsModules.boot()` and **discards the returned `ModuleRuntime`**, so nothing
   downstream can reach the populated registry - which is exactly why
   `JlsModules`' own javadoc says the registry "is populated but nothing reads
   it for dispatch yet". Add `JlsModules.runtime()`: set once inside `boot()`
   (`src/jls/boot/JlsModules.java:83-88`), throwing a named
   `IllegalStateException` when read before boot, with an explicit
   `bootedOrBuiltIn()` accessor for the not-booted case (see Notes - this is the
   whole risk).
2. **Four dispatch sites converted, each named:**
   - `src/jls/Circuit.java:918` - `ElementRegistry.forTag(elementType)` in the
     loader becomes a lookup over the `elem.element-provider` contributions.
   - `src/jls/edit/Palette.java:218` and the toolbar builder at
     `src/jls/edit/SimpleEditor.java:2312-2321` read
     `gui.palette-contributor` contributions.
   - `src/jls/JLSStart.java:382-385` - the ternary
     `path.endsWith(".v") ? new VerilogEmitter() : new VhdlEmitter()` - becomes
     a selection over `hdl.exporter` contributions by a new
     `HdlEmitter.extensions()` method. Two implementations exist, so widening
     the interface costs two edits and makes a third emitter a contribution
     rather than a third arm.
   - `src/jls/edit/SimpleEditor.java:5547-5570` - the anonymous `OpSink` -
     fans out to `collab.op-observer` contributions after each successful
     `submit`/`submitAll`. `src/jls/boot/CollabModule.java:37-41` contributes
     nothing today; the fan-out is what makes a contributed sink observable.
3. **The three `pending` catalog rows get real contracts.**
   `docs/extension-points.md:33,35,36` list `hdl.importer` ("cell-map/layout
   contract to be defined"), `app.command` ("shim contract over
   `jls.module.Activation`") and `gui.theme` ("theme/preferences object
   replacing `JLSInfo` statics") as pending. Each gains a constant in its home
   package's `*ExtensionPoints` holder and a named contract interface, so all
   seven seams are typed.
4. **Cardinality and lifecycle stop being prose.**
   `src/jls/module/ExtensionPoint.java` gains `Cardinality cardinality`
   (`ONE_ACTIVE` | `MANY`) and `Phase phase` (`REGISTER` | `ON_COMMAND` |
   `OBSERVE`). `ExtensionRegistry.contribute`
   (`src/jls/module/ExtensionRegistry.java:88-99`) rejects a second contribution
   to a `ONE_ACTIVE` point, naming both contributors. The doc table's four
   columns become machine-checked instead of one.
5. **`docs/extension-points.md`** gains a "consumed at" column naming the call
   site that reads each seam, and the `Status` column's "typed now" values gain
   "consumed" - the distinction the whole task exists to close.

Done means: an added contribution to any of the seven seams changes observable
behavior, and the catalog test fails if a seam is declared, documented or
consumed without the other two.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-050 | The feature is precisely "the registry that already boots is read for dispatch". Everything else in FEAT-050 is downstream of this. |

## Prerequisite tasks

None. `ModuleRuntime`, `ExtensionRegistry`, `ExtensionPoint`, the four holders
and the four modules all exist and boot in every run mode at HEAD. This task is
the consumption half and gates on nothing.

## Acceptance test

`test/jls/ModuleDispatchTest.java`, new:

- `theLoaderResolvesElementTypesThroughTheRegistry()` - boot a runtime with an
  extra `ElementType` contributed by a test module, load a circuit whose file
  names that tag, and assert the element is created. Today this fails: the
  loader reads the static table.
- `theToolbarIsBuiltFromPaletteContributions()` - same shape, one extra
  `PaletteEntry`, asserted through the existing UI harness's component-name
  assertions.
- `theEmitterIsSelectedFromExporterContributions()` - contribute a third
  `HdlEmitter` declaring `.sv`, invoke the export path with an `.sv` target, and
  assert it was used.
- `anOpObserverSeesEverySuccessfulSubmit()` - contribute a counting `OpSink`,
  drive three gestures, assert three observations and that a rejected op
  produced none.
- `contributionOrderIsDeclarationOrderAndIsStable()` - two boots, identical
  order, because dispatch that depends on contribution order must be
  deterministic or golden tests become boot-order-dependent.
- `dispatchFallsBackToTheBuiltInTableWhenNoRuntimeIsBooted()` - construct a
  `Circuit` and load a file with no boot at all (the state every headless test
  is in) and assert it still loads. **This is the regression guard for the whole
  change.**

`test/jls/ExtensionPointCatalogTest`, extended:

- `everySeamIsTypedAndConsumed()` replaces the current tolerance for `pending`
  rows; the assertion message names the seam and which of the three legs is
  missing.
- `cardinalityAndPhaseAgreeBetweenTheDocAndTheConstants()` - the existing
  two-way id check (`test/jls/ExtensionPointCatalogTest.java:165-186`) extended
  to all four columns.
- `aSecondContributionToAOneActivePointIsRejectedNamingBoth()`.

`test/jls/JlsModulesBootTest` gains
`bootStoresTheRuntimeAndASecondBootIsRejected()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to (element provider, palette contributor, exporter, op observer, ...) | closes - the catalog exists but three of seven rows are `pending` and none of the four typed rows is consumed; this closes both halves |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | depends on / tracking - this is the "wired by" half of #224 becoming real |
| 212 | Element-provider plugin API: discover external ElementType descriptors via ServiceLoader atop the #78 registry | depends on - a discovered provider that nothing dispatches from is invisible, which is the HEAD state; #212 (TASK-0107) is unbuildable until this lands |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - #78's registry half shipped and is what `CoreModule` contributes; this task changes who reads it, not what it holds |
| 84 | Decompose SimpleEditor: 4,119 lines, a 9-state mouse machine, a 305-line source== dispatcher that already caused #37, and whole-circuit undo snapshots | overlaps - two of the four dispatch sites are inside `SimpleEditor`; sequence with the decomposition or do them together |

Recorded decision, closed, cite as such and not as open: **#220** (the module
runtime that boots today), **#80** (the removed XML plugin loader and the
recorded ServiceLoader replacement sketch).

## Notes

- **The not-booted case is the entire risk.** `JlsModules.boot()` runs from
  `JLS.main` only (`src/jls/JLS.java:60`). Every unit test that constructs a
  `Circuit` and calls `load` directly - which is most of the golden suite - has
  no booted runtime. If the loader hard-requires the registry, the suite dies.
  The fallback must be an explicit, named, tested method returning the built-in
  contribution set, **not** an implicit null check scattered at four sites.
- **`ExtensionRegistry` is documented not thread-safe**
  (`src/jls/module/ExtensionRegistry.java:33`: "populate during startup, read
  afterwards"). Reading it from the EDT while batch code reads it from main is
  fine; *contributing* after boot is not. Either freeze the registry at the end
  of `boot()` or state the thread contract in the accessor's javadoc.
- **`ArchitectureRulesTest.hdlInternalsAreOnlyWiredFromTheCli`**
  (`test/jls/ArchitectureRulesTest.java:66-88`) constrains where emitter
  selection may live. Moving the selection into a module or into `jls.hdl`
  itself may trip it; read the rule before choosing the home.
- **`ExtensionRegistry.contribute` casts through `point.contract()`**
  (`:98`), so a raw-typed wrong-class contribution fails at the boundary with
  `ClassCastException`. Keep that: it is why the registry can be a heterogeneous
  container at all.
- **Behavior must not change for a default run.** Every golden in the suite
  passes today because the registry is unread. After this change the same
  goldens must pass because the registry contains exactly the built-in
  contributions in exactly the historical order. If a golden moves, the
  contribution order is wrong, not the golden.
- **Do not fold #212's discovery into this task.** Discovery is TASK-0107; the
  separation is what lets this one be reviewed against unchanged behavior.

## Evidence

- `src/jls/JLS.java:60` - `JlsModules.boot();` with the return value discarded,
  under a comment stating "nothing reads it for dispatch yet".
- `src/jls/boot/JlsModules.java:20-36` (the javadoc: "the registry is populated
  but nothing reads it for dispatch yet - so batch, HDL, and GUI behavior is
  unchanged"), `:49-58` (the four declared points), `:64-69` (the four modules),
  `:83-88` (`boot`).
- `src/jls/module/ExtensionRegistry.java:33` (not thread-safe), `:88-99`
  (`contribute`), `:110-118` (`contributions`), `:139-158` (`requireDeclared`).
- `src/jls/module/ExtensionPoint.java:29` - the `(String id, Class<T> contract)`
  record, which carries no cardinality or phase.
- The four bypass sites, verified by grep over `src/`:
  `src/jls/Circuit.java:918`, `src/jls/edit/Palette.java:218`,
  `src/jls/JLSStart.java:382-385`, `src/jls/edit/SimpleEditor.java:5547-5570`
  (with the toolbar builder at `:2312-2321`).
- `src/jls/boot/CoreModule.java:39-45` - the only module that contributes
  anything today; `src/jls/boot/CollabModule.java:37-41` - contributes nothing,
  by comment.
- `docs/extension-points.md:30-36` - seven seam rows, four "typed now", three
  "pending"; `test/jls/ExtensionPointCatalogTest.java:137-148,165-186` - the
  four-seam floor and the two-way doc cross-check.
- Do not restate: `docs/grand-architecture.md` §4 owns the module design;
  `docs/extension-points.md` owns the catalog.
