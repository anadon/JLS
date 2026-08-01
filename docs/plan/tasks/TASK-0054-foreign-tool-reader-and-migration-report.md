# TASK-0054 - The foreign-tool reader and its migration report

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0003

## Deliverable

A Logisim-Evolution `.circ` file opens in JLS, and every construct that did not
survive is named in a report rather than silently dropped.

1. **`src/jls/imp/circ/CircReader.java`.** Parses the `.circ` XML into a
   construct model: circuits, components (library + name + attribute set),
   wires, appearance. **This is the first XML parse in the shipped source tree**
   - no file under `src/` references `DocumentBuilderFactory`, `SAXParser` or
   `XMLInputFactory` - so it must not be the weakest one. Hardening, each with
   its own test: external general and parameter entities disabled, DTDs
   disallowed (`disallow-doctype-decl`), XInclude off, entity resolver
   neutralized, secure processing on, and a bounded expansion budget.
2. **`docs/logisim-construct-map.md`,** the written construct map. One row per
   source component: the library it comes from, the JLS element it becomes (or
   the refusal and its reason), and which attributes carry over. The map is a
   document because a switch statement is not reviewable by an instructor
   deciding whether to migrate a course.
3. **`src/jls/imp/circ/MigrationReport.java`.** Per unmapped or approximated
   construct: the source location (circuit name and the component's source
   coordinates), the construct as written, the reason, and - where one exists -
   the rewrite. Rendered as a one-shot dialog on interactive import and as text
   on the command line, the shape `ImportSummary` established for the Yosys
   path (`src/jls/hdl/imp/ImportSummary.java`).
4. **Name collisions are a loud reject, never a mapping.** A source construct
   whose name matches a JLS element with different semantics - the two tools
   share vocabulary and do not share meaning - is refused by name with both
   definitions stated. Guessing here produces a circuit that loads, simulates,
   and is wrong.
5. **Completeness is asserted, not asserted-to.** The reader records the set of
   constructs it saw and the set it realized; the report is the difference; a
   test asserts the report equals that difference exactly. That equality is the
   entire value of the feature.
6. **The corpus is public files, not fixtures.** Accept and reject tables are
   run over a corpus of published `.circ` files and the result is recorded in
   the PR, per FEAT-025's measurement requirement.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-025 | the reader, the map and the report are the feature's spine |
| FEAT-002 | this is the fail-loud discipline applied to a foreign format, and the first place where "unknown input names its own gap" is a user-visible product rather than an internal invariant |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0003 | the completeness assertion in item 5 is false against HEAD's loader. `Element.setValue` returns silently when no saved attribute matches (`src/jls/elem/Element.java:344-351`) and `Circuit.load` calls it unconditionally at five sites (`src/jls/Circuit.java:1067,1078,1089,1105,1116`), so a construct the reader believes it mapped can be discarded during load and never reach the report. A report built on that loader is confidently incomplete |

## Acceptance test

`test/jls/imp/circ/CircReaderTest.java`, new:

- `aSimpleGateCircuitImportsWithTheExpectedNetStructure()` - net partition
  compared, not pixel positions.
- `everyUnmappedConstructAppearsInTheReport()` - the completeness equality of
  item 5, over a fixture seeded with three deliberately unmappable components.
- `aNameCollisionIsRefusedNotMapped()` - assert the message states both
  definitions.
- `anApproximatedConstructIsReportedAsApproximatedNotAsMapped()`.
- `theImportedCircuitLoadsAndReSavesIdentically()` - `Circuit.load` +
  `finishLoad`, the assertion shape `ImportPipelineTest` uses
  (`test/jls/hdl/imp/ImportPipelineTest.java:70-79`).

`test/jls/imp/circ/CircHardeningTest.java`, new - one test per attack vector,
because item 1 says so:
`aDoctypeDeclarationIsRefused()`, `anExternalGeneralEntityIsNotResolved()`,
`anExternalParameterEntityIsNotResolved()`,
`aRecursiveEntityExpansionIsBounded()`,
`aFileUriInASystemIdentifierIsNotFetched()`.

`test/jls/imp/circ/ConstructMapTest.java`, new:
`everyRealizedConstructHasARowInTheDocument()` and
`everyRowInTheDocumentNamesARealConstruct()` - the doc-drift guard in both
directions, the discipline `ExtensionPointCatalogTest` applies to
`docs/extension-points.md`.

`test/jls/imp/circ/CircCorpusTest.java`, new (tagged long-run):
`theCorpusAcceptAndRejectTablesAreRecorded()`.

## Related GitHub issues

**No issue.** The migration-parity path is unfiled in its entirety - FEAT-025
and both of its tasks. Every other capstone's spine has at least one tracker
item; the one that moves users rather than files has none.

| # | title | relationship |
|---:|---|---|
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - `NetlistImporter` is the precedent for aggregate-all-problems, refuse-before-opening, and a post-import summary; reuse its shape rather than inventing a second one |

## Notes

- **Refuse before any circuit window opens.** `NetlistImporter.importNetlist`
  collects every problem and throws before layout
  (`src/jls/hdl/imp/NetlistImporter.java:71-99`). Half-imported circuits are the
  failure mode both importers must not have.
- **`.circ` carries geometry; JLS's grid is 12 px.** Source coordinates that do
  not land on `Geometry.SPACING` must be snapped or re-laid out, and which one
  happened belongs in the report. The layout seam is `SchematicLayouter`
  (`src/jls/hdl/layout/SchematicLayouter.java`), already used by the Yosys path.
- **Licensing is favorable and must still be checked.** Logisim-Evolution is
  GPLv3, so reading its format and consulting its sources is compatible with
  JLS's own GPLv3. Do not copy code without recording provenance; do not copy
  anything from a differently licensed fork.
- **Do not build a second construction path.** FEAT-015's programmatic verbs
  (TASK-0038) are the right substrate; until they land, emit save text as
  `NetlistImporter` does, and structure the code so the emitter is one class.
- **Do not restate the save grammar.** `docs/file-format.md` owns it; this
  reader writes through it.

## Evidence

- Repository-wide search at HEAD: no file under `src/` references
  `DocumentBuilderFactory`, `SAXParser` or `XMLInputFactory`. The hardening
  requirement in item 1 is therefore greenfield, not a retrofit.
- `src/jls/elem/Element.java:344-351` - the silent unknown-attribute return;
  `src/jls/Circuit.java:1067,1078,1089,1105,1116` - the five unconditional call
  sites.
- `src/jls/hdl/imp/NetlistImporter.java:71-99` (problem aggregation and
  refuse-before-build), `src/jls/hdl/imp/ImportSummary.java:1-13` (the
  post-import report data and its unbuilt UI).
- `docs/hdl-support-research.md:151-195` - the verified account of
  Logisim-Evolution as the other Java reference point, including its
  HDL/FPGA implementation and its cautionary co-simulation history.
- FEAT-025 acceptance criteria - the completeness assertion, the loud-reject
  rule for name collisions, and the untrusted-input hardening requirement.
