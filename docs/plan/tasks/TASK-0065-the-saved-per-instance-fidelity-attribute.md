# TASK-0065 - The saved per-instance fidelity attribute

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A `SubCircuit` **instance** names which implementation of its definition runs,
the choice survives a save/load round trip, and no file can change fidelity
silently.

1. **The sealed selection type.** `jls.sim.SubCircuitImpl`, a sealed interface
   permitting `StructuralImpl` (the only implementation this task ships) with
   `String id()`, `List<PortSpec> ports()`, `initSim(Boundary, Simulator)` and
   `react(long, Simulator, SimEvent.Payload, Boundary)`. Core-internal and
   **not** an extension point: `docs/grand-architecture.md` §6 puts the
   simulation inner loop inside core with zero plugin indirection. It gets a
   *pending* row in `docs/extension-points.md` (whose `:23-24` states it: "Pending
   seams are named here first ... before its contract exists") so the seam is catalogued before it is populated - that is the
   documented order (`ExtensionPointCatalogTest` enforces it).
2. **Two saved attributes on `SubCircuit`.** `String impl` (absent => the id
   `"structural"`) and `int implDelay` (absent => the structural critical-path
   default; must be >= 1). Both are legal `string-item`/`int-item` records under
   `docs/file-format.md` §3 with **no grammar change**. Parsed in
   `SubCircuit.setValue(String, String)` (`src/jls/elem/SubCircuit.java:311-326`,
   which today handles exactly one name, `"orient"`, and delegates the rest to
   `super.setValue`), written by `SubCircuit.save` (`:282-289`) in the existing
   position beside `String orient`.
3. **Per-instance, not per-definition.** Instances are already independent at
   HEAD: `SubCircuit.copy` deep-copies (`:332-...`) and `SubCircuit.save` writes
   **the whole nested circuit block per instance** (`:287`). A file may therefore
   legitimately contain `String impl "levelized"` on one instance next to a
   byte-identical sibling with `"structural"`, and that A/B configuration is the
   entire point: hold the design and the program fixed, change one boundary.
   **If definitions ever become shared by reference (FEAT-017), `impl` stays on
   the reference site.** Write that into this task's javadoc so the later
   program inherits it.
4. **FORMAT 3, gated per file.** `docs/file-format.md` §5 says unknown attribute
   *names* are silently ignored, so an old reader would load a compiled instance
   as structural - a silent behavioral change in a product used for grading.
   Therefore: **a file requires FORMAT 3 if and only if some `impl` attribute in
   it has a value other than `"structural"`.** Files with only structural
   instances keep emitting FORMAT <= 2 and stay byte-identical. The hook exists:
   `SubCircuit.saveFormatVersion()` (`:299-302`) already propagates a nested
   requirement upward into `Circuit.formatVersionNeeded()`
   (`Circuit.java:1482` writes it), and `Circuit.readFormatHeader`
   (`Circuit.java:732-774`) already refuses a newer version with
   `NEWER_FORMAT_HINT` (`:776-779`). `Circuit.FORMAT_VERSION` goes 2 -> 3
   (`Circuit.java:102`).
5. **`-fidelity <id>`**, a new `FlagSpec` row in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:759-789`, 14 rows at HEAD), which `CliFlagTableTest`
   and `jls -h` pick up automatically. `-fidelity structural` - "run the
   reference" as one flag - is the single most important affordance here.
   Per-instance override lands in the `-s` parameter file as
   `ELEMENT <qualified.name> FIDELITY <id>`, beside the existing
   `ELEMENT <name> WATCHED true`. **Precedence, normative:** flag > parameter
   file > file attribute > `"structural"`.
6. **Self-identification in output.** `docs/batch-interface.md` is a stability
   contract ("exactly two things to stdout"), so a fidelity manifest is printed
   **only when at least one instance is non-structural**. An all-structural run
   is byte-identical to today.

Done means: an instance carries a fidelity id, a grader cannot receive a
silently-degraded run, and adding the second implementation is a new permit plus
a class - not a redesign.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-031 | The saved half of the per-instance fidelity toggle. Every later implementation is a permit added to this sealed set; without the attribute the choice has nowhere to live and no way to survive a save. |

## Prerequisite tasks

None. The attribute, the sealed type, the format gate and the flag are all
expressible against HEAD: `SubCircuit.setValue`, `SubCircuit.save`,
`SubCircuit.saveFormatVersion`, `Circuit.readFormatHeader` and `JLSStart.FLAGS`
all exist and all already do the analogous job for `orient` and for the nested
format requirement.

## Acceptance test

- **`jls.elem.SubCircuitModelTest.implAttributeRoundTripsPerInstance()`**
  (extend the existing class, `test/jls/elem/SubCircuitModelTest.java`): save a
  circuit with two instances of one definition carrying different `impl` values;
  reload; assert each instance kept its own value and that the two nested
  circuit blocks are otherwise identical.
- **`jls.FormatHeaderTest.aNonStructuralImplRequiresFormat3()`** (extend the
  existing class, `test/jls/FormatHeaderTest.java`): assert a file with only
  `impl "structural"` emits `FORMAT 2` **byte-identically to today**, and a file
  with any other value emits `FORMAT 3`.
- **`jls.FormatHeaderTest.anOlderReaderRefusesAFidelityFileLoudly()`** (new
  method in the same class): parse a `FORMAT 3` file with the version ceiling
  pinned at 2; assert `LoadError.Category.NEWER_FORMAT` and the
  `NEWER_FORMAT_HINT` text. This is the assertion that no grader silently gets a
  different fidelity.
- **`jls.CliFlagTableTest`** (existing, `test/jls/CliFlagTableTest.java`): green
  with the new row, and the `-h` output golden regenerated - **a golden that must
  be regenerated is a trap worth naming, not a surprise at review time**.
- **`jls.FidelityPrecedenceTest.flagBeatsParameterFileBeatsAttributeBeatsDefault()`**
  (new): all four levels present at once; assert the winner at each of the four
  removals.
- **`jls.FileFormatSpecTest`** (existing): the spec's attribute table, the
  writers' output and the code stay in lock-step - it will fail until
  `docs/file-format.md` gains the two rows.
- **`jls.ExtensionPointCatalogTest`** (existing): fails until the pending
  `sim.subcircuit-implementation` row exists in `docs/extension-points.md`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | overlaps (open) - this task adds a **pending** row, which the catalog's own `:23-24` requires; it does not close #223 |
| 221 | Decision: simulation execution strategy | informs, **closed**. A second *implementation of a subcircuit* is a model change, not a second execution strategy; the distinction is what keeps this task outside #221's amendment clause, and it is only true if implementations never touch the queue (TASK-0066's `SoleStrategyTest`). |

**No issue** exists for the fidelity toggle. Recorded as a gap. Maintainer
direction D4 is its provenance, not a tracker item.

## Notes

- **Trap: FORMAT 2 -> 3 is a whole-file gate with wide blast radius.**
  `Circuit.FORMAT_VERSION` at `Circuit.java:102` is read by `readFormatHeader`
  (`:732-774`) and written by `save` (`:1482`). Everything asserting exact save
  bytes is in scope: `DeterministicSaveTest`, `CircuitRoundTripTest`,
  `AllElementsRoundTripTest`, `FileFormatSpecTest`, `CliTextSaveTest`,
  `GenerativeRoundTripFuzzTest`. The conditional gate is what keeps them all
  green; an unconditional bump changes every golden in the tree.
- **Trap: `Element.setValue` returns silently on an unknown attribute.**
  `src/jls/elem/Element.java:344-351` (the int overload, verified; the same shape
  on the sibling overloads) loops the declared attributes and just returns if none matched, and
  `Circuit.load` calls it unconditionally at five sites
  (`src/jls/Circuit.java:1067, 1078, 1089, 1105, 1116`). So a typo'd `impll`
  loads as structural with no diagnostic. That is TASK-0003/TASK-0004's defect,
  not this task's to fix - but the FORMAT 3 gate above is what makes it
  *non-silent for this attribute specifically*, and that is why the gate is not
  optional.
- **Trap: `implDelay` is a *behavioral* attribute.**
  `docs/file-format.md:470` says a writer should prefer a version bump over an
  "ignorable" attribute whenever dropping it would change simulation behavior,
  and names `Memory`'s `initrle`/`sync` as the bad class. `implDelay` is exactly
  that class - it rides the same FORMAT 3 gate as `impl`, never independently.
- **What this task deliberately does not do.** It ships no second
  implementation. `StructuralImpl` alone is a refactor of today's behavior behind
  an interface, which is why 1.5 weeks is credible; the second implementation is
  a separate cost in FEAT-031's band.

## Evidence

- `src/jls/elem/SubCircuit.java:282-289` (`save`, writing `String orient` then
  the whole nested circuit), `:299-302` (`saveFormatVersion` propagating the
  nested requirement), `:311-326` (`setValue`, the one-name chain), `:592-611`
  (`initSim`, seeding the nested circuit in stable-id order), `:621-636`
  (`react`, the entire inbound boundary), `:646-652` (`send`, the entire
  outbound boundary).
- `src/jls/Circuit.java:102` (`FORMAT_VERSION = 2`), `:732-774`
  (`readFormatHeader`), `:776-779` (`NEWER_FORMAT_HINT`), `:1482`
  (`out.println("FORMAT " + formatVersionNeeded())`).
- `src/jls/JLSStart.java:759-789` - the 14-row `FlagSpec[] FLAGS` table.
- `docs/file-format.md` §3 (item grammar), §5 (unknown attribute names silently
  ignored; unknown tags are hard errors), `:470` (prefer a version bump);
  `docs/batch-interface.md` §3.2 (the two-things-to-stdout contract).
- `mech-fidelity-toggle.md` §1 (the HEAD fact table: the boundary is already
  narrow and already total; instances are already independent), §2 (per-instance,
  not per-definition, with the gem5 and ARM-hybrid precedents), §6.1 (the save
  format and the FORMAT 3 argument), §6.2 (the flag and the precedence rule).
- BRIEF §12 D4 - the maintainer's own direction of travel, adopted as a
  per-subcircuit toggle because it makes parity a property of a boundary.
