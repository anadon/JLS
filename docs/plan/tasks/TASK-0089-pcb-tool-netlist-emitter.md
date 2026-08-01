# TASK-0089 - The PCB-tool netlist emitter

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0007, TASK-0085,
TASK-0086, TASK-0087

## Deliverable

A new class `jls.pcb.KicadNetlistEmitter` (package `jls.pcb`, with
`package-info.java` carrying `@NullMarked`) that renders the KiCad
s-expression netlist from three inputs already produced elsewhere: the
physical netlist (`jls.pkg.PhysicalNetlist`, TASK-0087 over TASK-0007's
`jls.netlist.NetPartition`), the packing and refdes assignment
(`jls.pkg.PackPlan`, TASK-0086) and the part library with its footprint names
(`jls.pkg.PartLibrary`, TASK-0085).

Precisely what changes:

- `jls/pcb/KicadNetlistEmitter.java` - one static
  `emit(PhysicalNetlist nets, PackPlan packing, PartLibrary parts)` returning
  the complete text, and never a partial file. Output shape is the minimum the
  KiCad reader actually accepts: `(export (version "E") (components
  (comp (ref …) (value …) (footprint …))…) (nets (net (code …) (name …) (node
  (ref …) (pin …))…)…))`. `libparts`, `libraries`, `design`, `sheetpath`,
  `tstamp`, `libsource` are omitted deliberately - the reader skips them.
- `jls/pcb/PcbExportException.java` - modelled on
  `src/jls/hdl/HdlExportException.java`, aggregating every problem into one
  message in `PcfEmitter`'s idiom.
- CLI: one `FlagSpec` row `-netlist <file>` in the `FLAGS` table at
  `src/jls/JLSStart.java:759-789`, a parse case beside `case "board":`
  (`:1096-1107`), and a headless write through the existing temp-and-rename
  path (`:438-459`). The `-parts` flag it consumes is **TASK-0085's**, not a
  new one; this task adds no second parts-file grammar.
- One golden: `test/resources/pcb/nand4_dip.net`, produced from a committed
  four-NAND fixture bound to `Package_DIP:DIP-14_W7.62mm`.

Done means: `jls -netlist out.net -parts parts.txt design.jls` writes a netlist
in which every `comp` carries a non-empty `footprint`, every `net` has
`code >= 1`, and every `node` names a `ref` that appears in `components`.
Any unbound element, any unknown footprint key, any element with no package
assignment is collected and reported in one exception and **no file is
written**.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-042 | The netlist half of "KiCad and gEDA netlist emitters with a manufacturability gate". |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0007 | This reads the `jls.netlist.NetPartition` that only TASK-0007 creates. Re-deriving it here would give JLS two net partitioners that can disagree, destroying the `PcfEmitter` invariant recorded at `src/jls/hdl/board/PcfEmitter.java:14-22`. |
| TASK-0085 | `footprint` is new primary data; only TASK-0085 defines `PartPackage`, its footprint name, the `.parts` grammar and the `-parts` flag. Without it every `comp` record is footprint-less and the artifact is mechanically useless (see Notes). |
| TASK-0086 | This reads the `PackPlan` - the refdes and package-section assignment - that only TASK-0086 produces. `U<stableId>` is a fallback for the schematic path, not for a board netlist where four drawn gates share one physical `U1`. |
| TASK-0087 | A `node` record names a *package pin*. An 8-bit `Adder` has no package and no pins until it is decomposed into slices with their synthetic inter-slice nets, which only TASK-0087 produces. Without it the emitter can describe 1-bit designs only. |

## Acceptance test

`test/jls/pcb/KicadNetlistGoldenTest` (new class), in the `PcfGoldenTest`
regime (`test/jls/hdl/board/PcfGoldenTest.java:30-60`):

- `nand4NetlistMatchesTheGolden()` - emits from the committed four-NAND
  fixture and asserts byte equality against `test/resources/pcb/nand4_dip.net`
  after replacing `JLSInfo.versionString` with the `@VERSION@` token;
  regeneration under `-Djls.pcb.regenerate=true`.
- `everyComponentCarriesANonEmptyFootprint()` - parses the emitted text and
  asserts no `(comp …)` record has an empty or absent `footprint` field. This
  is the assertion that makes the artifact loadable rather than merely
  well-formed.
- `everyNodeRefResolvesToAComponent()` - asserts the `ref` of every `node` is
  in the `components` block, and that every `net` has `code >= 1`.
- `anUnboundElementAbortsWithEveryProblemNamed()` - one fixture with two
  unbound elements asserts a single exception naming both, and asserts no
  output file exists afterwards (the `UnbindablePortsTest` contract).

## Related GitHub issues

**no issue.** The registry records the whole physical program (FEAT-040 through
FEAT-044) as untracked, and a targeted `search_issues` over `anadon/jls` for
`kicad OR netlist OR pcb OR footprint` returns only the HDL-interop issues
(#59, #61, #62) and unrelated hits. Adjacent, cited as precedent not as scope:

| # | title | relationship |
|---:|---|---|
| #213 | Board-aware HDL export: emit pin-constraint files (PCF/XDC/QSF)… | informs - **closed** (`not_planned`, consolidated into #264). Its `-board`/`-pins` UX decision and its P3 all-or-nothing prediction are the pattern this task copies. |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff (consolidates #213 + #215) | overlaps - open; owns the FPGA-constraint half of the same "external binding file" mechanism. This task does not close it. |

## Notes

- **The trap that makes this task second-rate on its own.** KiCad's
  `BOARD_NETLIST_UPDATER::addNewFootprint` refuses any component with an empty
  footprint field - one `RPT_SEVERITY_ERROR` per component, the footprint is
  not placed, and because no pads exist **none of that component's nets are
  applied**. There is no in-KiCad path from a netlist to footprint assignment:
  `SCH_IO_MGR`'s sixteen schematic importers include no netlist reader, and
  `kicad-cli pcb import --format` does not accept one. A footprint-less netlist
  is not a partial artifact, it is an empty board. That is why TASK-0085 is a
  hard blocker and not a nicety, and why TASK-0090 (the schematic path) is
  ranked ahead of this one in `fmt-kicad-geda.md` §7 (slice K6 vs K2).
- **`TriState` has no honest netlist rendering** until `HdlModel.Direction`
  gains a bidirectional case (`src/jls/hdl/HdlModel.java:28-33` is
  `{INPUT, OUTPUT}` at HEAD; TASK-0049 widens it). Until then this emitter must
  refuse a design containing a `TriState` with a specific message, not emit a
  lie. That refusal is a named rule, not a silent skip.
- **The exhaustive-switch trap.** If element-kind dispatch here is written as a
  `switch` over the sealed `LogicElement` hierarchy
  (`src/jls/elem/LogicElement.java:17-21`, 24 permits), adding an element type
  stops the build here. That is the desired behavior under FEAT-001 - do not
  add a `default` arm.
- **Do not go through `HdlModel`.** `HdlExporter` aborts on any element outside
  its EXPORTED/SKIPPED/TOPOLOGY buckets (`src/jls/hdl/HdlExporter.java:191-197`).
  A board netlist has no synthesis constraint; `Memory`, `RegisterFile`,
  `FieldExtend` and `SubCircuit` must be describable even though they do not
  synthesize.
- **Flag-table traps.** `CliFlagTableTest.usageDocumentsExactlyTheParserFlags`
  (`test/jls/CliFlagTableTest.java:82-89`) fails the moment a `FlagSpec` row is
  added without the usage text agreeing; `everyTableFlagIsAcceptedByTheParser`
  (`:102`) fails if the parse case is missing.
- **Architecture trap.** `ArchitectureRulesTest` confines `jls.hdl` consumers
  to `jls.JLSStart` and `jls.boot` (`test/jls/ArchitectureRulesTest.java:69-75`)
  and forbids the headless packages from importing `jls.edit` (`:124-132`).
  `jls.pcb` must be headless and must be added to the headless rule's package
  list in the same commit.

## Evidence

- Net partition already computed in shipped code: `src/jls/hdl/HdlExporter.java:208-241`
  (registration and jump-alias union), `:253` (`Map<WireNet, Group>`),
  `:1146-1160` (`Group{name,bits,isPort}`), `:1161` (`UnionFind`).
- The binding-file precedent, in full: `src/jls/hdl/board/PinBindings.java`
  (98 lines, all-or-nothing parse) and `src/jls/hdl/board/PcfEmitter.java:14-22,73`.
- Deterministic element order for byte-stable output:
  `src/jls/Circuit.java:479-485` (`getElementsInStableOrder`);
  stable ids at `src/jls/elem/Element.java:619-622`.
- Terminal data available per element: `src/jls/elem/Put.java:145` (`getBits`),
  `:169`/`:180` (`getXr`/`getYr`).
- KiCad reader/updater behavior, minimum accepted record set, and the
  sixteen-importer finding: `09-format-adoption-plan.md` Family 2 study
  (`fmt-kicad-geda.md` §2.1, §3, §7 slice K6), read from KiCad `10.0` sources.
- Golden regeneration idiom and version tokenization:
  `test/jls/hdl/board/PcfGoldenTest.java:30-60`.
