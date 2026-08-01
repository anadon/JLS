# TASK-0090 - The open-schematic emitter

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0007

## Deliverable

`jls.pcb.GedaSchematicEmitter` - a single-file, self-contained gEDA/Lepton
`.sch` writer that projects the `Circuit` graph (not `HdlModel`) into the
line-oriented ASCII record set, with every symbol **embedded** so the file
references nothing on disk.

Precisely what changes:

- `jls/pcb/GedaSchematicEmitter.java`: emits `v <release-date> <ver>`, then per
  element a `C x y selectable angle mirror basename` record followed by a
  `[ … ]` embedded-symbol block containing the derived symbol body
  (`B` box from `Element.getWidth()`/`getHeight()`, one `P x1 y1 x2 y2 color
  pintype whichend` per `Put` at `Put.getXr()`/`getYr()`), and an attached
  `{ … }` attribute block whose `T` records carry `refdes=`, `pinnumber=`,
  `pinlabel=`, `pintype=` and `device=`. Wires become `N x1 y1 x2 y2 color`
  records with a `{netname=…}` block from the shared partition's group name.
- `jls/pcb/SchematicTransform.java`: the coordinate transform - JLS pixels,
  Y-down, `SPACING = 12` (`src/jls/core/Geometry.java:18`) to gEDA mils, Y-up,
  100-mil grid - as a named, tested class with the scale factor and the Y-flip
  against the circuit bounding box as declared constants, **not** inline
  literals.
- Refdes: `U<stableId-counter>` derived from `Element.getStableId()`, so the
  file is a pure function of circuit content. Pin numbers: sequential `1..N`
  in `getAllPuts()` order, declared in the file header comment as *schematic*
  pin numbers, not package pin numbers.
- Element policy: **render everything.** `Memory`, `RegisterFile`,
  `FieldExtend` and `SubCircuit` all draw; the HDL export buckets do not apply.
- CLI: reuse the `-netlist` flag introduced by TASK-0089 with extension
  dispatch (`.net` -> KiCad netlist, `.sch` -> gEDA schematic), mirroring the
  existing `-export` extension dispatch at `src/jls/JLSStart.java:1081-1093`.
- Goldens: `test/resources/pcb/counter.sch` and `test/resources/pcb/bundles.sch`,
  from the fixtures the Verilog goldens already use.

Done means: the emitted `.sch` opens in a gEDA-format reader with correct
symbol geometry and correct net connectivity, with no external `.sym` library
present, and the same circuit emits byte-identical text on two runs.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-042 | The schematic half - the artifact that actually reaches KiCad's *Assign Footprints* flow, which the netlist path cannot. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0007 | Net names in the `{netname=…}` blocks must be the same names every other emitter uses. This reads the `jls.netlist` partition that only TASK-0007 creates. |

## Acceptance test

`test/jls/pcb/GedaSchematicGoldenTest` (new class):

- `counterSchematicMatchesTheGolden()` - byte equality against
  `test/resources/pcb/counter.sch`, version-tokenized, regenerable under
  `-Djls.pcb.regenerate=true`.
- `everyComponentRecordIsFollowedByAnEmbeddedSymbolBlock()` - scans the emitted
  text and asserts each `C ` record is followed by a `[` block that closes with
  `]`, and that no `C ` record names a basename without one. This is the
  assertion that the file is self-contained.
- `everyPinRecordHasAPinNumberAttribute()` - asserts each `P ` record inside an
  embedded block is followed by a `{ … }` containing a `pinnumber=` `T` record,
  and that the numbers are `1..N` with no gaps and no repeats within a symbol.
- `coordinatesAreOnTheDeclaredGridAndYIsFlipped()` in
  `test/jls/pcb/SchematicTransformTest` - asserts the transform maps the
  circuit's minimum Y to the schematic's maximum Y and that every emitted
  coordinate is a multiple of the declared grid constant.

`test/jls/pcb/LeptonNetlistAcceptanceTest.leptonNetlistAcceptsTheEmittedSchematic()`
- the external-oracle leg, in the `GhdlCompileTest` idiom
(`test/jls/hdl/GhdlCompileTest.java:34-35`): `ToolLocator.findOnPath("lepton-netlist")`
plus `Assumptions.assumeTrue`, run the tool over the emitted file, and assert
the net partition it reports round-trips to the one JLS emitted. Self-skips
when the tool is absent.

## Related GitHub issues

**no issue.** `search_issues` over `anadon/jls` for `kicad OR netlist OR pcb OR
breadboard OR footprint` returns no schematic-export issue. Adjacent:

| # | title | relationship |
|---:|---|---|
| #59 | HDL interoperability: staged VHDL/Verilog support… | informs - open, but a staged tracking issue for HDL text. This emitter is not HDL and does not close any part of it. |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff | overlaps - shares the `jls.hdl.board` emitter-and-binding pattern; different destination. |

## Notes

- **The finding that makes this task worth more than TASK-0089:** KiCad 10.0
  (released 2026-03-19) ships a native gEDA/Lepton schematic importer with an
  embedded-symbol path, so a self-contained `.sch` reaches KiCad *and*
  lepton-schematic *and* lepton-netlist's downstream backends from one emitter.
  Emitting the older, simpler format reaches more live tools than emitting
  KiCad's own native schematic.
- **Verify the central claim first, in an afternoon.** KiCad's gEDA importer
  is new and its embedded-symbol path is in the code but not in the user
  manual. Hand-write a ten-line `.sch` with one embedded symbol and open it in
  KiCad 10 before writing the emitter. If the embedded path regresses, the
  documented degradation is "rectangular fallback symbols with correct nets",
  and the emitter must ship with the external-`.sym` fallback documented.
- **Do not route through `HdlModel`.** It is a derived, flat, RTL-dataflow
  model with no instance statement; the `Circuit` graph carries hierarchy
  directly and gEDA expresses hierarchy as a `source=` attribute naming a
  sub-`.sch`. Hierarchy on this path therefore does **not** wait on TASK-0043.
  (Multi-file output at the CLI is the follow-on slice, not this task -
  `src/jls/JLSStart.java:438-459` writes exactly one file today.)
- **Angle is restricted to `{0, 90, 180, 270}`** in the gEDA component record;
  JLS `Orientation` is a four-value enum, so the mapping is total, but assert
  it rather than assume it.
- **The coordinate transform is the difference between "looks like the JLS
  drawing" and "looks like confetti."** Pin it with a golden and a named
  constant; do not let it become a number someone tunes.
- Package and headless traps are identical to TASK-0089: `@NullMarked`
  `package-info.java` (pinned by `NullMarkedRatchetTest` and
  `PackageInfoRatchetTest`), and `jls.pcb` added to the headless-layering rule
  at `test/jls/ArchitectureRulesTest.java:124-132`.

## Evidence

- gEDA record grammars (`v`, `N`, `C`, `P`, `T`, `{}`, `[]`), the embedded-
  component path, the mils/Y-up convention and KiCad 10's importer:
  `fmt-kicad-geda.md` §2.3, §6, §7 slice K2, read from lepton-eda and KiCad
  `10.0` sources; licenses GPL-2.0-or-later and GPL-3.0-or-later, absorbable
  under D8.
- Everything the emitter needs exists at HEAD: `src/jls/Circuit.java:479-485`
  (deterministic order), `src/jls/elem/Element.java:162,172` (width/height),
  `:619-622` (stable id), `src/jls/elem/Put.java:145,169,180` (bits, relative
  position), `src/jls/core/Geometry.java:17-18` (`CIRCUITSIZE`, `SPACING`).
- `HdlModel` is flat and has no instance statement:
  `src/jls/hdl/HdlModel.java:28-33,43,925`; the export policy aborts on
  unbucketed elements at `src/jls/hdl/HdlExporter.java:191-197`.
- External-oracle test pattern: `test/jls/hdl/ToolLocator.java:67`,
  `test/jls/hdl/GhdlCompileTest.java:34-35`,
  `test/jls/hdl/IverilogCompileTest.java:33-34`.
