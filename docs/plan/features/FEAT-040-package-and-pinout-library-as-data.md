# FEAT-040 - The package and pinout library as data

**Status:** proposed | **Cost:** 4-8 mw | **Owner program:** UNOWNED |
**Spine rank:** S21

## Capability delivered

JLS knows what a real part is. A versioned data file states, per part number,
its pin count and pin names, how many independent sections it holds, which
sections are logically equivalent, which parts substitute for which, what
footprint name a PCB tool should be told to place, and what the part's inputs
and outputs cost and supply electrically. A design element can be bound to a
part, a section and a pin, so that everything downstream - a bill of materials,
a wiring list, a netlist a fab tool will accept, a fan-out check, a breadboard
placement - is a query against data rather than a table hard-coded in an
emitter. The library is extensible by a user with a text file and no Java.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | required | the artifact *is* packages, sections, pins and jumpers; without part data there is nothing to place |
| CAP-05 | required | a netlist component with an empty footprint field is discarded by the board tool, so the footprint column is the gate |
| CAP-13 | required | the netlist a KiCad user imports is component records, refdes and footprints - all of it part data |
| CAP-16 | beneficial | the migrating tool ships a TTL component library; parity means the imported design's parts resolve to entries here |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | none required. The schema and the data can be authored before anything consumes them, and that is the recommended order |

FEAT-027 is a *semantic* prerequisite of the electrical columns rather than a
build-order one: unit loads and drive capacity describe a strength model that
only FEAT-027 makes real. The columns can be transcribed first and left inert;
they must not be interpreted before FEAT-027 lands, or the fan-out check will
report numbers the simulator contradicts.

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0085 | The package data schema and footprint binding | The versioned schema, its provenance fields, and the mechanism binding a logic element to a package, section, footprint name and part value |
| TASK-0055 | Absorb the through-hole part data | The transcription itself, with attribution and license notices; shared with FEAT-025, which needs the same parts to migrate a foreign design |

## Acceptance criteria

1. A part entry is data, not code: adding a part number requires editing one
   text file and no Java, and a test proves an added part is visible to the
   packing pass, the emitters and the loading check without recompilation.
2. Every entry carries pin count, per-pin name and direction, section count,
   the section-to-pin map, the supply pins, a gate-equivalence class, a
   substitution list, a footprint name, and per-family input and output loading
   figures with their units stated.
3. Every entry carries provenance: which datasheet or which absorbed source it
   came from, and the license notice that source requires. A test asserts no
   entry has an empty provenance field.
4. The schema is versioned, and a library file declaring a version the reader
   does not understand is refused with a message naming the version - never
   partially read.
5. Malformed entries are aggregated and reported together, in the shipped
   `PinBindings.parse` idiom, so a user learns the whole repair job from one
   failure.
6. A user-supplied library file overrides or extends the shipped one by an
   explicit flag, and the resolution order is documented and tested.
7. Round-trip: for every shipped entry, the pin map is self-consistent - no pin
   appears in two sections, every section's pins exist, and every supply pin is
   marked as such.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the package and pinout library, its schema, and the absorbed part data | **no issue** |

No open issue touches the physical program at all. The gap is recorded here
rather than left blank: this is new primary data in a tree that has never had
any (`grep -rniE "footprint|refdes|pinout" src/` returns zero hits, verified at
`addc6c5`).

## Design notes

**This data does not have to be authored from datasheets, and that is the whole
reason the band is 4-8 rather than 6-10.** Two GPL-3.0 Java simulators already
carry it: Logisim-Evolution's `std/ttl/` (69 of 108 `Ttl74xxx` files) and
hneemann's Digital. JLS is GPL-3.0-or-later, so under D8 this is absorption with
attribution - transcription, not design. Do not bulk-import Fritzing's parts
data: its app is GPL-3.0 but its parts library is CC-BY-SA-3.0, and the
per-part share-alike obligation is a real burden at bus factor one. The Fritzing
*format* is free to implement and is a reasonable importer.

Two schema decisions are worth making once and stating in the file header. The
first is what a "section" is when a part's sections are not identical (a 74LS139
is two decoders; a 74LS153 is two multiplexers sharing select lines) - shared
pins must be expressible, or the packing pass will produce plans that cannot be
wired. The second is the default subfamily: the shipped library should declare
one (74LS or 74HC) and say so, because the fan-out arithmetic and the
floating-input behavior differ between them and a student who mixes them is
making a real engineering error the tool should be able to name.

The footprint column is one string per package and it is the entire content of
the previously refused PCB gate. It should be present from the first entry, not
added when CAP-05 asks.

## Risks

- **Transcription errors are silent and expensive.** A wrong pin number produces
  a board that cannot work and a check that says nothing. Every absorbed entry
  needs a cross-check against a second source, and the test that a design
  packed, emitted and re-parsed reproduces the same partition (FEAT-041,
  FEAT-042) is what actually catches it.
- **Library growth as a support burden.** The library must be extensible by
  users, and the shipped set must stay small and named, or bus factor one
  acquires a parts-curation job forever.
- **Premature electrical interpretation.** See the note under prerequisite
  features: transcribed loading figures that get consumed before FEAT-027
  produce confident wrong answers.

## Evidence

- The spine row and its band: `10-capstone-plan.md:618` region, row S21
  (package library `logical` section - pinout, sections, gate equivalence,
  substitution; 4-8 wk after absorption).
- The absorption finding and the license split, including the counts
  (69 of 108 `Ttl74xxx` files) and the Fritzing CC-BY-SA caveat:
  `10-capstone-plan.md:652-672` (§2.3).
- The footprint fact that opens the PCB gate - a netlist component with an empty
  footprint field is discarded by `pcbnew`: `10-capstone-plan.md:486-491`.
- Verified at HEAD `addc6c5`: `grep -rniE "footprint|refdes|pinout" src/` = 0
  hits; there is no part data in the tree today.
- The error-aggregation idiom to copy:
  `src/jls/hdl/board/PinBindings.java:36-55` (every malformed line collected and
  reported together, named by line number).
- D8 - reimplementation and absorption are a cost question, and GPL-3.0-or-later
  can absorb GPL-compatible sources with their notices: `BRIEF.md` §13 D8.
