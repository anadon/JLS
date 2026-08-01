# TASK-0055 - Absorb the through-hole part data

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0085

## Deliverable

The 74-series and DIP part data JLS needs, transcribed into the versioned
schema, shipped as data on the classpath, with attribution and license notices
that survive an audit. At HEAD there is no part data of any kind: `resources/`
contains only `help` and `packaging`, and no source file carries a pinout, a
package name or a footprint.

1. **The transcription.** One entry per part number covering, at minimum, the
   common TTL/CMOS teaching set: quad 2-input gates (7400/7402/7408/7432/7486),
   hex inverter (7404), the 74x74 dual flip-flop, the 74x161/163 counters, the
   74x138/139 decoders, the 74x153/157 multiplexers, the 74x245 transceiver,
   the 74x373/374 latch/register, and the 74x283 adder. Each entry carries the
   fields the schema defines: pin count and pin names, section count, which
   sections are logically equivalent, substitution set, footprint name, and the
   electrical columns (unit loads presented, drive capacity supplied).
2. **Provenance per entry, not per file.** Each part records where its data came
   from and under what license. Pin numbering and pin function are facts and are
   not copyrightable, but transcribed *text* is: descriptions are written fresh
   or taken from a license-compatible source, never pasted from a datasheet.
3. **`NOTICE`-style attribution in tree,** listing every absorbed source, its
   license, its version or retrieval date, and which part entries came from it.
   The repository is GPLv3, so GPL-compatible upstreams - including
   Logisim-Evolution's own TTL component library, which is GPLv3 - are usable;
   anything else is refused with the reason recorded.
4. **Data, not code.** The library is a file the build copies onto the
   classpath and into the installer resource set, editable by a user with a text
   editor and no Java, per FEAT-040's first acceptance criterion.
5. **The electrical columns ship inert.** FEAT-040 states this explicitly: unit
   loads and drive capacity describe a strength model that only FEAT-027 makes
   real, so they are transcribed and **not interpreted**. A test asserts that no
   shipped code path reads them yet, so the fan-out check cannot report numbers
   the simulator contradicts.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-040 | the schema without data is an empty table; this is the data half |
| FEAT-025 | a migrated design references TTL parts by number, and those numbers must resolve to entries here or the migration reports every part as unmapped |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0085 | this task writes files in a schema TASK-0085 defines - the field set, the provenance fields, the versioning rule and the loader. Transcribing first means transcribing into a shape that will change, and re-transcribing is the whole cost |

## Acceptance test

`test/jls/part/PartLibraryTest.java`, new:

- `everyPartCarriesProvenanceAndALicenseTag()` - no entry may ship without
  both; this is the audit's teeth.
- `pinCountEqualsTheSumOfSectionPinsPlusPowerPins()` - the arithmetic check
  that catches the most common transcription error.
- `everyPinNameIsUniqueWithinAPart()`.
- `everySubstitutionTargetExistsAndIsSymmetricOrDeclaredOneWay()` - a
  substitution pointing at an absent part is a dangling reference, not a
  fallback.
- `everyEquivalentSectionSetHasIdenticalPinFunctions()`.
- `addingAPartRequiresNoRecompilation()` - write a synthetic entry to a temp
  copy of the library, load it, and assert it is visible; FEAT-040's
  data-not-code criterion expressed as a test.

`test/jls/part/ElectricalColumnsAreInertTest.java`, new:
`noShippedCodeReadsTheElectricalColumns()` - asserts the accessor has no
production call sites, so the columns cannot be half-used before FEAT-027.

`test/jls/LicenseAuditTest.java`, new or extended:
`everyAbsorbedDataFileHasANoticeEntry()` and
`everyNoticeEntryNamesALicenseOnTheAllowedList()`.

## Related GitHub issues

**No issue.** The entire physical program - FEAT-040 through FEAT-044 - is
unfiled, and so is the migration path (FEAT-025) that shares this data.

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the registry established that element identity is data with a compiler-enforced contract; the part library is the same idea one level out, for physical parts rather than logical elements |

## Notes

- **Two weeks is transcription, not design.** If this task grows a schema
  argument, the argument belongs in TASK-0085 and the transcription is blocked
  behind it, not merged with it.
- **The seductive shortcut is scraping.** Machine-readable part libraries exist
  in other tools' repositories; most are usable and some are not. Every source
  gets a license check *before* transcription, and the check is recorded even
  when it passes. A part with an unclear source is omitted, and the omission is
  listed - a gap is information.
- **Gate equivalence is where correctness lives.** Section-to-section
  equivalence is what makes packing legal; declaring two sections equivalent
  when their pin functions differ produces a wiring list that fits on the
  breadboard and does not work. The equivalence test is not optional.
- **Power pins are part of the pinout and are not logic.** A schema that omits
  Vcc/GND produces a BOM with the right parts and a board with no power. State
  the convention once in the data and assert it.
- **The library must reach the installer.** `resources/packaging` is where the
  installer inputs live at HEAD; adding a classpath resource without adding it
  to the packaging inputs produces a build that passes tests and an installed
  application with no parts.
- **Do not restate FEAT-040's schema.** Reference it. This document is about
  what goes in the table and where it came from.

## Evidence

- `resources/` at HEAD contains exactly two directories, `help` and
  `packaging`; there is no part, package, footprint or pinout data anywhere in
  the tree.
- `src/jls/elem/ElementRegistry.java:38-77` - the 35 registered element types,
  none of which carries a physical package; the logical/physical gap this task
  starts to close.
- FEAT-040 acceptance criterion 1 (data not code) and its note that the
  electrical columns must stay inert until FEAT-027.
- FEAT-025's third capstone consumer (CAP-04): "the incumbent's through-hole
  and 74-series material is the readiest source of the part data a breadboard
  view needs" - the reason one transcription serves two features.
- `docs/hdl-support-research.md:151-195` - the verified account of
  Logisim-Evolution, the GPLv3 upstream whose TTL library is the primary
  license-compatible candidate.
