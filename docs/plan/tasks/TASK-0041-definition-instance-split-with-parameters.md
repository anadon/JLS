# TASK-0041 - Definition/instance split with parameters

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0039

## Deliverable

A subcircuit definition exists once in a file and instances reference it with
bound parameters, replacing the three independent deep-copy sites that make
every instance an unrelated object graph.

1. **The model.** `Circuit` gains a definition table: `Map<DefinitionId,
   Circuit>` plus its canonical order. `SubCircuit` stops owning a `Circuit`
   (`src/jls/elem/SubCircuit.java:26`) and holds a `DefinitionId` plus a
   parameter binding map; `getSubCircuit()` (`:102-107`) resolves through the
   owning circuit's table and keeps its signature so the ~20 call sites in
   `SubCircuit` itself and the editor do not all change at once.
2. **The three copy sites become references.**
   - `SubCircuit.copy` (`src/jls/elem/SubCircuit.java:331-384`) - today
     constructs a fresh `Circuit`, walks every element and runs
     `Util.copy(Set<Element>, Circuit)` (`src/jls/Util.java:39`) plus
     `Util.partition(Circuit)` (`:145`). It copies the reference and the
     bindings instead.
   - `SimpleEditor.doImport` / `finishImport`
     (`src/jls/edit/SimpleEditor.java:5463-5491`, `:679-697`) - registers the
     imported circuit as a definition and creates an instance.
   - `Circuit.loadElementItems` (`src/jls/Circuit.java:1006-1024`) - a
     `SubCircuit` body that carries a `defid` and no nested `CIRCUIT` block
     resolves against the definition table; a body carrying an inlined block
     still loads, registering it as a definition keyed by its digest.
3. **Parameters.** A definition declares parameters in its `CIRCUIT` block
   (name, `int` type, default). An instance binds them with repeated
   ` String param "<name>=<value>"` items in its `SubCircuit` block. This task
   ships **declaration, binding, persistence and validation only**; resolving a
   binding into an element's width is TASK-0042's elaboration pass. An unbound
   parameter with no default is a load diagnostic here.
4. **The file form and the bump.** A `DEFINITION` section per definition (the
   frame TASK-0033 defines and TASK-0040 already uses for libraries), and a
   `SubCircuit` body that is a reference rather than an inlined circuit. That is
   a change to block structure, which `docs/file-format.md:437-441` makes a
   **required `FORMAT` bump**: `Circuit.FORMAT_VERSION`
   (`src/jls/Circuit.java:102`) advances, and `formatVersionNeeded()`
   (`:1580-1587`) emits the new version only for files that actually use a
   shared definition, so every flat and every inlined file stays readable by
   current JLS.
5. **The reader accepts both for one epoch.** An inlined `SubCircuit` body and a
   `defid` reference both load; the writer emits references. `docs/file-format.md`
   §7's `SubCircuit` row and §9's version history are restated in this change.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-017 | This is the split itself: one definition, N instances, with parameters, instead of N deep copies that have silently diverged. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0039 | An instance references a definition **by identity**. Without `defid` there is nothing to reference and without `defdigest` there is no way to detect that two files disagree about what a name means. |

## Acceptance test

`test/jls/elem/DefinitionInstanceSplitTest.java`, new:

- `tenInstancesOfOneDefinitionStoreOneCopy()` - build a circuit with one
  definition and ten instances; save; assert the file contains exactly one
  `DEFINITION` section for it, and that going from one instance to ten grows the
  file by ten instance blocks and nothing else. Must fail today, where
  `SubCircuit.save` inlines the whole definition per instance.
- `editingTheDefinitionChangesEveryInstance()` - the property that makes the
  split worth having; assert through simulation output, not through bytes.
- `anInlinedBodyStillLoadsAndIsRegisteredAsADefinition()` - the epoch's
  backward half, over a checked-in pre-split fixture.
- `aSharedDefinitionBumpsTheFormatVersionAndAFlatFileDoesNot()` - both
  directions, against `Circuit.FORMAT_VERSION`.
- `anUnboundParameterWithNoDefaultIsADiagnostic()`.
- `twoInstancesWithDifferentBindingsPersistBothBindings()`.
- `instancePathAndSidRemainUniqueAcrossSharedInstances()` - the invariant
  TASK-0035 shipped, re-asserted here because this is the change that would
  break it: bare `sid` uniqueness stops holding the moment one definition backs
  two instances.

`test/jls/DeterministicSaveTest` gains
`aCircuitWithSharedDefinitionsSavesByteIdenticallyFromEitherEditHistory()`.

## Related GitHub issues

**No issue.** The parameterization program has no tracker entry; its written
form is `docs/capability-roadmap/lf-01-parameterization.md` (program P7).

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - a shared definition is what lets hierarchical export emit one module for N instances rather than N uniquified modules (TASK-0043, TASK-0044) |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs - two peers must agree on which definition they are editing, which is only a well-posed question once definitions exist |

## Notes

- **Elaboration already exists; it has just never had inputs.** lf-01 states it
  plainly and this study verified all three sites: JLS's hierarchy is
  elaborated-by-copy today. This task does not introduce a new phase, it gives
  the existing implicit one something to read.
- **`Circuit.setImported` / `getSubElement` assume one circuit per instance.**
  `src/jls/Circuit.java:1664-1686` stores a single `SubCircuit subElement`
  (`:50`), and `Circuit.save` branches on it to decide whether to emit the
  `FORMAT` line (`:1478-1484`). A shared definition has N sub-elements and must
  not be "imported" by any one of them. This is the single most invasive
  consequence of the split; plan it first.
- **`SubCircuit.init` derives puts from the definition's pins**, so a parameter
  that changes a pin width changes the instance's put list. Until TASK-0042
  lands, bindings are persisted and validated but do not reach pin widths;
  say so in the dialog, or a user will bind a parameter and see nothing happen.
- **`SubCircuit.saveFormatVersion()` delegates to the nested circuit**
  (`src/jls/elem/SubCircuit.java:293-302`); with the body gone it must delegate
  to the referenced definition, and the definition must be resolvable at save
  time or the version computation throws.
- **Width is a saved integer on fourteen element classes** (lf-01, verified by
  `grep -l '"bits"' src/jls/elem/`), and `Memory` is handwritten. None of them
  changes in this task. Resisting the urge to parameterize widths here is what
  keeps this two weeks.
- **`Util.copy` and `Util.partition` stay.** They remain the elaborator's
  implementation (TASK-0042) and are still the paste path. Do not delete them
  while removing their call from `SubCircuit.copy`.
- **Every golden with a subcircuit regenerates**, and the inlined-body fixture
  must be checked in *before* the writer changes, or there is nothing to test
  the backward half against.

## Evidence

- `src/jls/elem/SubCircuit.java:26-37` - the five fields that are a subcircuit's
  entire state; `:102-107` `getSubCircuit`; `:281-289` `save` inlining the whole
  nested circuit; `:293-302` `saveFormatVersion`; `:331-384` `copy`, the deep
  copy via `Util.copy` + `Util.partition`.
- `src/jls/edit/SimpleEditor.java:5463-5491` (`doImport`) and `:679-697`
  (`finishImport`, which constructs the `SubCircuit` and calls
  `impCirc.setImported(sub)`).
- `src/jls/Circuit.java:1006-1024` - a fresh `new Circuit("")` per nested block
  on load; `:50` and `:1664-1686` - the single `subElement` back-reference;
  `:102` `FORMAT_VERSION = 2`; `:1478-1484` and `:1580-1587` - the version
  emission.
- `src/jls/Util.java:39` (`copy(Set<Element>, Circuit)`), `:145`
  (`partition(Circuit)`).
- `docs/file-format.md:323` (the `SubCircuit` body), `:355-360` (nested blocks
  recurse), `:437-441` (a block-structure change requires a bump), `:447-458`
  (the version history this change extends).
- `docs/capability-roadmap/lf-01-parameterization.md` - "There is no parameter
  mechanism anywhere in the model", "Instantiation is already a deep copy",
  "Width is a saved integer on 14 element classes", and the three-site
  inventory. Program P7 owns the cost band.
- Decision D4 in `BRIEF.md` §11: "Nested / shared / parameterized subcircuit
  definitions" as a named direction of travel; D7 on libraries as the payoff.
- Do not restate: lf-01 owns the parameterization design;
  `docs/file-format.md` owns the grammar and version history.
