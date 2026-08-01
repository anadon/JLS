# TASK-0085 - The package data schema and footprint binding

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A logic element is not a chip. This task is the schema that says what a chip is,
and the mechanism that says which chip a drawn element becomes. It ships **no
geometry**.

1. **`src/jls/pkg/`, a new headless package.**
   - `PartPackage` - part number, family (`74LS`, `74HC`, `74HCT`), pin count,
     an ordered `List<Section>`, the power pins (`VCC`, `GND` by pin number), the
     **footprint name** as an opaque resolvable string
     (`Package_DIP:DIP-14_W7.62mm`), the default value string, an
     `Electrical` record, an optional `Cascade` descriptor, and a `Provenance`
     record. Pinned to an immutable, naturally-ordered value the way `Board` is
     (`src/jls/hdl/board/Board.java:27-80`).
   - `Section` - a section name (`A`..`D`) and a `Map<String,Integer>` from
     logical pin role (`A`, `B`, `Y`, `D`, `CLK`, `Q`) to package pin number.
   - `Electrical` - drive capability and input load, both **in unit loads of the
     part's own family**, plus a flag for families where the DC fan-out check is
     vacuous. TASK-0088 consumes this and nothing else.
   - `Cascade` - which pin is carry-in, which is carry-out, the slice width, and
     what terminates the chain. Declared here; consumed by TASK-0087.
   - `Provenance` - source document, license and the attribution notice that must
     travel with the row. Absorbed data carries its notice or it does not ship.
   - `PartLibrary` - `all()`, `byPartNumber()`, `forElement(ElementType, int
     bits)`. The built-in table is loaded from a resource, not compiled in.

2. **The library is data.** `resources/parts/74series.parts`, a line-oriented
   grammar with a `SCHEMA <version>` header, loaded through
   `getResourceAsStream` (the idiom at `src/jls/JLSInfo.java:36` and
   `src/jls/Help.java:180`). Adding a part is adding rows to a data file; adding
   a *kind* of thing a part can say is a schema-version bump. **A course can ship
   its own `.parts` file** and that is the point.

3. **The `-parts <file>` flag**, one new `FlagSpec` row in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:758-788`), supplying additional or overriding rows.
   Parsing follows `PinBindings.parse`'s shipped idiom: **every malformed line and
   every duplicate key is collected and reported together with its line number**,
   so a user learns the whole repair job from one failure
   (`src/jls/hdl/board/PinBindings.java:37-70`).

4. **`PartBinding`, the element-to-package mechanism.** A binding maps an
   `ElementType` tag plus a width to a `(PartPackage, Section)` candidate set, and
   an individual element - keyed by `Element.getStableId()` - to a chosen
   `(partNumber, sectionName, footprintName, value)`. Bindings from a `-parts`
   file override library defaults; the resolution order is declared and tested.

5. **The realization policy is total over the element registry, in four buckets,
   with reasons.** Exactly the `HdlExporter` shape
   (`src/jls/hdl/HdlExporter.java:428-495`): `REALIZED` (has a default part),
   `TOPOLOGY` (`Wire`, `WireEnd`, `JumpStart`, `JumpEnd` - not components),
   `SKIPPED` (`Text`, `Pause`, `Stop`, `SigGen`, `TestGen`, `Display` - no
   physical meaning), and `NO_DEFAULT_REALIZATION` - a map from class to the
   reason and to what a `-parts` row would have to supply. The nine types with no
   default: `Memory`, `RegisterFile`, `TruthTable`, `StateMachine`, `FieldExtend`,
   `SigGen`, `TestGen`, `Display`, `SubCircuit`. **Two of them, `Memory` and
   `RegisterFile`, are exactly what makes JLS good at CPUs**, so the message must
   name the `-parts` escape rather than saying no: a `Memory` bound to a 62256
   with its own pin map is a perfectly good row.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-040 | The whole feature. Pinout, sections, gate equivalence, substitution and provenance are this schema; the data that fills it is TASK-0055's. |
| FEAT-042 | The emitters cannot write a `(comp)` record without a footprint name and a package pin number, and both are fields defined here. |

## Prerequisite tasks

None. Stable element identity already ships (`Element.getStableId()`,
`src/jls/elem/Element.java:619-622`, issue #165), which is the one datum a refdes
policy needs. Recorded explicitly because the schema is the classic thing to
defer behind the data, and deferring it is how the data ends up shaped by
whatever the first emitter happened to want.

## Acceptance test

`test/jls/pkg/PartLibrarySchemaTest`:
- `everySectionPinNumberIsWithinThePartsPinCount()` and
  `noPinNumberIsUsedTwiceWithinAPart()` - over every row of the shipped library.
- `everyPartDeclaresPowerPinsAndAFootprintName()` - a non-empty `(footprint …)`
  is literally the condition a PCB tool's netlist updater tests before discarding
  a component, so an empty one must be impossible at the source.
- `everyPartCarriesProvenanceWithALicenseNotice()` - fails on a row with an empty
  attribution field.
- `sectionsOfOnePartAreInterchangeable()` - gate equivalence asserted, not
  assumed: every section of a part exposes the same role set.
- `schemaVersionIsRefusedWhenNewerThanTheReader()` - the version-negotiation
  shape `Circuit.readFormatHeader` already uses (`src/jls/Circuit.java:765-771`).

`test/jls/pkg/RealizationPolicyTest`:
- `policyIsTotalOverTheElementRegistry()` - every one of the 35 registered types
  (`src/jls/elem/ElementRegistry.java:38-77`) lands in **exactly one** bucket, and
  a type in none fails the build. This is FEAT-001's discipline applied to a new
  table on the day the table is created rather than after the first gap.
- `everyNoDefaultRealizationEntryNamesWhatAPartsRowWouldSupply()` - asserts the
  reason string is actionable, not "unsupported".

`test/jls/pkg/PartsFileParseTest`:
- `everyMalformedLineIsReportedWithItsLineNumber()` and
  `aPartBoundTwiceIsAParseError()` - the two assertions
  `test/jls/hdl/board/UnbindablePortsTest` already makes for pin bindings.

`test/jls/CliFlagTableTest` extends over `-parts`, since
`JLSStart.commandLineFlags()` drives both the parser and `usage()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the package/pinout schema, the footprint binding and the `-parts` mechanism | **no issue.** The entire physical program (FEAT-040 through FEAT-044) is untracked. |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the realization policy is a new registry-keyed table and inherits #78's authoring contract; its registry half already shipped. |
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end | overlaps - `Board`/`Boards`/`PinBindings` are the working precedent for "a target is data, adding one is a table entry, never new code". Same shape, different target class. |

## Notes

- **Falsification guard, verified at HEAD.** `grep -rniE "footprint|refdes|pinout"
  src/` returns **zero hits**. Every test in this task fails today for every JLS
  design and cannot be satisfied by accident.
- **Ship the footprint *name*, ship none of the geometry.** The field is a string
  the PCB tool resolves from its own installed footprint library. A bundled
  symbol/footprint geometry library is a different and much larger obligation -
  KiCad's own is on the order of 15,000 curated footprints maintained by a team -
  and it is deliberately refused here. The curation obligation this task creates
  is **one column in a ~30-row table**.
- **Do not put the physical library in the `.jls` file.** The circuit references
  parts by number; the library is a separate versioned artifact. Otherwise every
  circuit carries a copy of the table and a table fix is a mass file edit.
- **`Boards.ALL = List.of(ICESTICK)` is the precedent and also the warning**
  (`src/jls/hdl/board/Boards.java:81`): a single hardcoded entry with its map
  transcribed from vendor documentation. It has worked because there is one
  board. A ~30-row part table compiled into Java would not, which is why this
  library loads from a resource.
- **Sorted, immutable, deterministic.** `Board` pins its pin map to a
  naturally-sorted immutable copy so error listings scan in the order a user
  expects (`src/jls/hdl/board/Board.java:64-80`, `NATURAL_PIN_ORDER`). Do the
  same for sections and pins here; a HashMap iteration order in a BOM is a
  non-reproducible artifact.
- **New package, new obligations**: `package-info.java` with `@NullMarked`
  (`test/jls/PackageInfoRatchetTest`, `test/jls/NullMarkedRatchetTest`) and a
  per-package JaCoCo floor in `pom.xml` beside the `jls.sim` / `jls.elem` /
  `jls.collab.op` rules (`pom.xml:449-515`).

## Evidence

- `src/jls/hdl/board/Board.java:8-27` - "a board is deliberately just data …
  adding a board is adding a table entry in `Boards`, never new code"; `:64-80` -
  the natural pin ordering and the immutable copy.
- `src/jls/hdl/board/PinBindings.java:37-70` - the collect-and-report-together
  parse idiom and its `@jls.testedby` anchors.
- `src/jls/hdl/HdlExporter.java:428-495` - the four-bucket total policy, its
  stated rationale ("adding an element type without deciding its export policy
  fails a test rather than surfacing as an unexplained export error"), and
  `classifiedElementClasses()`, the accessor the totality test reads.
- `src/jls/elem/ElementRegistry.java:38-77` - the 35 registered types the policy
  must cover.
- `src/jls/elem/Element.java:619-622` - `getStableId()`, the key a refdes policy
  and a per-element binding hang off.
- `src/jls/Circuit.java:765-771` - the newer-version refusal shape the schema
  header follows.
- `src/jls/JLSInfo.java:36`, `src/jls/Help.java:180` - the `getResourceAsStream`
  idiom for shipped data.
