# TASK-0086 - Packing, refdes, BOM and wiring list

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0007, TASK-0008, TASK-0085

## Deliverable

The headless batch report that turns a drawing into a parts order. Zero GUI code
and therefore zero draw on the coverage commons.

1. **`jls.pkg.Packer.pack(Circuit, PartLibrary, PartBinding) -> PackPlan`.**
   Deterministic **first-fit over a fixed section count per package**, walking
   `Circuit.getElementsInStableOrder()` (`src/jls/Circuit.java:479-485`). This is
   first-fit, not bin packing with obstacle routing: the section count per part is
   fixed, the order is canonical, and the result is therefore a pure function of
   the circuit content and the library version.

2. **Reference designators keyed on stable id.** A refdes is assigned by walking
   elements in `getStableId()` order and numbering per prefix (`U` for logic, `R`,
   `C`, `J` for future part classes). **`U3` must not become `U7` on the next
   edit**: inserting one unrelated gate may append a new designator and must not
   renumber an existing one. The assignment rule is written down in
   `docs/plan/` terms and pinned by a test, not left as an implementation
   accident.

3. **Four artifacts, written to a directory**, each a stable text grammar:
   - `refdes.map` - `<stableId> <refdes> <part> <section>`, one line per realized
     element, sorted by stable id.
   - `bom.txt` - one line per distinct part: quantity, part number, the refdes
     list, the footprint name; then a summary line - *"35 packages, 3 unused
     sections"*.
   - `wiring.net` - the **point-to-point** list, `U3.1 -- U7.11 net BUS0`, derived
     from TASK-0007's net partition and named by TASK-0008's stable-id-keyed net
     names. Power and ground pins of every placed package appear here, not only
     signal pins.
   - `pack.log` - the diagnostics: unrealizable elements with the reason and the
     `-parts` row that would fix each, unused sections, and packages with a
     section left over.

4. **The `-pack <dir>` flag**, one new `FlagSpec` row in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:758-788`), valid only with `-b`, and rejected at parse
   time in combination with flags it cannot coexist with - the shape
   `JLSStart` already uses for `-board`/`-pins`/`-export`
   (`src/jls/JLSStart.java:911-917`). Exit status follows
   `docs/batch-interface.md` §1: 0 when every element is realized, 1 with one
   `jls: error:` line when any is not.

5. **Errors aggregate.** Every unrealizable element, every double-booked section
   and every unbound pin is collected and reported together, following
   `PinBindings.parse`'s idiom, so a user learns the whole repair job from one
   run rather than one element at a time.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-041 | The packing, refdes and report half. It is also the payload both the PCB and the breadboard consume: build it once and two capstones get their deliverable. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0007 | Extract the net-partition walk into its own package | The wiring list **is** the net partition pushed through the packing binding. It must be the shared partition; today the walk exists twice, in `Circuit.finishLoad` and in `HdlExporter.UnionFind` (`src/jls/hdl/HdlExporter.java:1161-1226`), and a third copy here would guarantee the netlist and the wiring list disagree eventually. |
| TASK-0008 | Key net and probe names off stable id, and validate them | `wiring.net` prints net names. At HEAD names derive from `getID()`, which `src/jls/elem/Element.java:21-22` documents as reassigned on every save, so the report would churn on an unrelated edit and the diff-stability assertion could not pass. |
| TASK-0085 | The package data schema and footprint binding | Reads the section count, the pin roles, the power pins and the footprint name. There is nothing to pack into without it. |

## Acceptance test

`test/jls/pkg/PackReportGoldenTest`, over a committed small fixture (a SAP-1
class ALU slice, ~14 logic elements, ~7 packages):
- `everyLogicElementMapsToExactlyOneRefdesAndSection()` - totality, and no
  section double-booked. Errors aggregated the way
  `test/jls/hdl/board/UnbindablePortsTest` asserts them.
- `powerPinsOfEveryPlacedPackageAppearInTheWiringList()` - VCC and GND are in the
  supply nets. A wiring list a person builds from must include the pins that make
  the chip work.
- `reRunIsByteIdentical()` - the determinism assertion, the same discipline
  `test/jls/DeterministicSaveTest` applies to the save path.
- `insertingOneUnrelatedGateProducesAnAdditiveOnlyDiff()` - **the load-bearing
  test.** Load the fixture, add one gate not connected to anything packed,
  regenerate all four artifacts, and assert every hunk is an insertion. A single
  changed `U`-number anywhere fails it. This is what "refdes keyed on stable id"
  means operationally.
- `unusedSectionsAreReportedWithTheirPackage()` - *"U1 has one unused section"* is
  the pedagogical payload: a logical gate is not a chip, and one drawn NAND is a
  quarter of a package that costs the same as all four.
- `anUnrealizableElementNamesThePartsRowThatWouldFixIt()` - drop a `Memory` into
  the fixture and assert the diagnostic names the `-parts` escape rather than
  refusing.
- `exitStatusIsOneWhenAnyElementIsUnrealized()`.

`test/jls/CliFlagTableTest` extends over `-pack`; `docs/batch-interface.md` §1's
flag table is drift-tested against `JLSStart.commandLineFlags()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | packing, reference designators, the BOM and the wiring list | **no issue.** The physical program is untracked end to end. |
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end | overlaps - same batch-report shape, different target class; `PcfEmitter` (`src/jls/hdl/board/PcfEmitter.java`) is the precedent for a small deterministic emitter over a data table. |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | informs - if a GUI ever fronts the pack report, it fronts this headless engine, exactly as #214 fronts `-t`. Do not build a GUI here. |

## Notes

- **First-fit, not an optimizer, and say so in the report header.** The breadboard
  study correctly refused bin-packing-plus-obstacle-routing; this is not that. A
  deterministic first-fit over a fixed section count is days of work and produces
  a result a student can check by hand, which an optimizer's output is not.
- **Determinism has a specific enemy here and it is map iteration order.** Use
  sorted, immutable structures throughout, the way `Board` pins its pin map to a
  naturally-sorted immutable copy (`src/jls/hdl/board/Board.java:64-80`). A
  `HashMap` in the BOM path is a non-reproducible artifact that will pass CI on
  one JDK and fail on another.
- **The wiring list must be legible in physical terms.** `U3.1 -- U7.11 net BUS0`
  is a person's instruction; a set of stable ids is not. This matters more than it
  looks: TASK-0093's breadboard discrepancy report has the same problem and no
  in-tree precedent, and the vocabulary chosen here is the one it will inherit.
- **Not every element becomes a chip, and the report says which and why.** Nine of
  the 35 registered types have no default realization at any width - `Memory`,
  `RegisterFile`, `TruthTable`, `StateMachine`, `FieldExtend`, `SigGen`,
  `TestGen`, `Display`, `SubCircuit` (which flattens). Two of them are exactly
  what makes JLS good at CPUs. That is a property of physics, not of JLS's code,
  and the honest boundary is "gate level plus the cascadable word-level elements
  plus a `-parts` row for anything else".
- **`SubCircuit` flattens before packing.** Decide it here and test it: a
  hierarchy that packs per-instance and a hierarchy that flattens produce
  different BOMs, and the difference must not be discovered by a student ordering
  parts.
- **Do not emit a netlist here.** `wiring.net` is a human artifact. The PCB-tool
  netlist is TASK-0089's and has its own grammar, its own goldens and its own
  acceptance conditions.

## Evidence

- `src/jls/Circuit.java:479-485` - `getElementsInStableOrder()`, sorted by
  `Element::getStableId`; the canonical order this task's determinism rests on.
- `src/jls/elem/Element.java:21-26,619-622` - the id "reassigned on every save"
  versus the permanent `stableId` (#165), and `hasFileStableId`.
- `src/jls/hdl/board/PinBindings.java:37-70` - the aggregate-error parse idiom.
- `src/jls/hdl/board/Board.java:64-80` - natural ordering and immutable copies for
  deterministic listings.
- `src/jls/JLSStart.java:758-788` (the single authoritative flag table),
  `:911-917` (flag-combination validation at parse time).
- `src/jls/hdl/HdlExporter.java:1161-1226` - the second, private net partition
  that must not become a third.
- `docs/batch-interface.md:33-48` - the exit-status contract; §6 - the stability
  promise these four artifacts join.
- `test/jls/DeterministicSaveTest` - the byte-identical-re-run discipline this
  report adopts.
