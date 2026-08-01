# TASK-0042 - The elaboration pass and its diagnostics

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0041

## Deliverable

One shared elaborator turns a definition table plus instance bindings into a
resolved design, and every binding it cannot resolve is reported with the
instance path, the definition and the parameter - not swallowed and not
defaulted.

1. **The pass.** `jls.elab.Elaborator.elaborate(Circuit) -> ElaborationResult`,
   in a headless leaf package born under
   `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` with no baseline entry.
   `ElaborationResult` is a record of the elaborated `Circuit` plus an ordered,
   immutable `List<Diagnostic>`; `Diagnostic` carries a severity, a stable
   machine-readable code, the `ItemKey` instance path (TASK-0035), the
   `DefinitionId` (TASK-0039) and human text.
2. **What it resolves.** Per instance, in canonical instance-path order:
   parameter bindings against the definition's declarations; defaults where a
   binding is absent and a default exists; then the substitution of resolved
   parameter values into the definition's parameterized element attributes.
   Elaboration is a pure function of (definition table, bindings) - two runs
   produce byte-identical elaborated circuits.
3. **The diagnostic classes, each with a code and each tested.**
   - `E-ELAB-001` unresolved binding: a parameter with no binding and no
     default.
   - `E-ELAB-002` unknown parameter: a binding naming a parameter the definition
     does not declare. A diagnostic, **not** a silent drop - this is the same
     failure mode the loader has for unknown attribute names
     (`docs/file-format.md:222-228`), and elaboration must not repeat it.
   - `E-ELAB-003` missing definition: a `defid` with no entry in the table and
     no library resolution.
   - `E-ELAB-004` cyclic instantiation: a definition reaching itself, reported
     with the whole cycle as a list of `DefinitionId`s.
   - `E-ELAB-005` type or range violation: a bound value outside the
     parameter's declared range, or of the wrong item kind.
   - `E-ELAB-006` width mismatch at a boundary: a resolved instance pin width
     that disagrees with the net attached to that put.
4. **The existing implicit elaborator is the implementation.**
   `Util.copy(Set<Element>, Circuit)` (`src/jls/Util.java:39`) plus
   `Util.partition(Circuit)` (`:145`) are what performs elaboration-by-copy
   today; the pass drives them with resolved inputs rather than reimplementing
   a second copy path.
5. **One caller per consumer, not one per site.** `Circuit.finishLoad`,
   `HdlExporter.buildModel` (`src/jls/hdl/HdlExporter.java:175`) and the
   simulator's setup all call the same pass; a consumer that needs an elaborated
   design and does not call it is a defect this task's test catches.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-017 | Parameters that are declared, bound and persisted but never resolved are decoration. This is the half that makes a parameterized definition mean something at simulation and export time. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0041 | The pass reads the definition table and the instance bindings, both of which TASK-0041 creates. There is nothing to elaborate against a model where every instance is already a private deep copy. |

## Acceptance test

`test/jls/elab/ElaborationDiagnosticsTest.java`, new - one method per code,
each asserting the code, the instance path and the definition id, not merely
that something threw:

- `unresolvedBindingIsReported()`, `unknownParameterIsReported()`,
  `missingDefinitionIsReported()`, `typeViolationIsReported()`,
  `widthMismatchAtABoundaryIsReported()`.
- `aCycleIsReportedWithTheWholeCycle()` - A instantiates B instantiates A;
  assert the diagnostic lists both `DefinitionId`s in cycle order and that the
  pass terminates rather than recursing.
- `diagnosticsAreOrderedByInstancePath()` - determinism of the report itself.
- `everyDiagnosticCodeIsExercised()` - reflect over the code enum and assert
  each appears in a test, so a new code cannot ship untested.

`test/jls/elab/ElaborationEquivalenceTest.java`, new:

- `elaboratingALegacyInlinedFileIsByteIdenticalToItsPreSplitSave()` - load a
  checked-in pre-TASK-0041 fixture, elaborate, save; assert byte-identical to
  the fixture's own canonical save. This is the guard that the pass changed
  nothing for files that had no parameters, and it is the reason the whole
  golden corpus can survive TASK-0041 and TASK-0042 together.
- `elaborationIsIdempotent()` - elaborating an already-elaborated circuit is the
  identity.
- `twoRunsProduceIdenticalElaboratedBytes()`.

`test/jls/ArchitectureRulesTest` gains `elaborationHasExactlyOneImplementation`,
asserting no class outside `jls.elab` calls `Util.copy` on a subcircuit's
element set - the guard against a second, divergent elaborator.

## Related GitHub issues

**No issue.** The elaboration pass is unfiled, as is the whole of program P7.

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - `HdlExporter.buildModel` is one of the pass's callers, and a hierarchical export of an unelaborated design would emit parameters it cannot render |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - an imported netlist arrives already elaborated by the external tool; the pass must be a no-op over it, which is a useful sanity check |

## Notes

- **Cycles are unreachable today and become reachable here.** Import makes a
  copy at import time (`src/jls/edit/SimpleEditor.java:5463-5491`), so a
  subcircuit cannot contain itself; under references it can. Cycle detection is
  not defensive programming, it is a new obligation created by TASK-0041, and
  the test must construct the cycle through the model, not through a file.
- **Do not default silently.** The loader's unknown-attribute valve
  (`docs/file-format.md:222-228`) is the format's forward-compatibility
  mechanism and is deliberate there; inside elaboration the same behavior is
  silent data loss of exactly the kind FEAT-002 and TASK-0003 exist to remove.
  Elaboration reports; it never repairs.
- **Diagnostics are data, not strings.** A grading harness (FEAT-053) and a
  batch run both consume them; a `List<String>` forces every consumer to parse
  prose. `HdlExporter` already learned this the hard way - its rejection path
  concatenates every offender into one message
  (`src/jls/hdl/HdlExporter.java:198-203`), which reads well and machine-parses
  badly.
- **Order is part of the contract.** `HdlExporter` sorts its walk for
  determinism and the save path sorts by stable id
  (`src/jls/Circuit.java:1492-1497`); an unordered diagnostic list would make
  a CI report differ run to run for no semantic reason.
- **The width check needs the net partition, not just the pins.**
  `E-ELAB-006` compares a resolved pin width against the attached net's width,
  which means the pass runs after `Util.partition` (`src/jls/Util.java:145`) or
  reads the partition TASK-0007 extracted. Sequence it explicitly rather than
  discovering it as a null net.
- **Budget the two weeks on the equivalence test, not the resolver.** The
  resolver is a map lookup with defaults; the expensive, load-bearing part is
  proving that elaborating every existing fixture reproduces its bytes.

## Evidence

- `src/jls/Util.java:39` (`copy(Set<Element>, Circuit)`), `:145`
  (`partition(Circuit)`) - the existing implicit elaborator.
- `src/jls/elem/SubCircuit.java:331-384` - `copy`, which drives both, and is the
  site TASK-0041 converts to a reference.
- `src/jls/edit/SimpleEditor.java:5463-5491`, `:679-697` - import as a copy,
  which is why cycles cannot occur today.
- `src/jls/Circuit.java:1006-1024` - a fresh `Circuit` per nested block at load;
  `:1492-1497` - the canonical order the diagnostic ordering mirrors.
- `src/jls/hdl/HdlExporter.java:175` (`buildModel`, a consumer), `:198-203`
  (the concatenated-message rejection shape not to copy).
- `docs/file-format.md:222-228` - unknown attribute names are silently ignored,
  and why elaboration must not inherit that.
- `docs/capability-roadmap/lf-01-parameterization.md` - "JLS's hierarchy is
  elaborated-by-copy already … an existing implicit phase that has never had
  inputs". Program P7 owns the cost band.
- Do not restate: lf-01 owns the parameterization design; TASK-0041 owns the
  model change; `docs/simulation-semantics.md` owns what an elaborated circuit
  must simulate as.
