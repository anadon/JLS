# TASK-0049 - Bidirectional ports end to end

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

`INOUT` exists from the element vocabulary through the intermediate
representation to every emitter, and every place that currently means
"not INPUT, therefore output" either says `inout` or refuses by name. At HEAD
`HdlModel.Direction` has exactly two constants
(`src/jls/hdl/HdlModel.java:28-33`) while the HDL header scanner already
produces three (`src/jls/hdl/scan/ScannedPort.java:13-20`, including
`INOUT`) - the scanner is honest and nothing downstream can carry what it
reports.

1. **`HdlModel.Direction` gains `INOUT`** (`:28-33`).
2. **Four ternaries become exhaustive switches with no default arm,** so a
   fourth direction stops the compile rather than silently emitting `output`:
   - `src/jls/hdl/VerilogEmitter.java:111-114` -
     `port.direction() == INPUT ? "input" : "output"` -> `inout` for the new
     case.
   - `src/jls/hdl/VhdlEmitter.java:128-131` - `"in " : "out "` -> `"inout "`.
   - `src/jls/hdl/board/PcfEmitter.java:74-76` - the direction comment.
   - `src/jls/hdl/board/PcfEmitter.java:193-194` - the unknown-port error text.
3. **A bidirectional pin element.** `Pin` is
   `abstract sealed class Pin ... permits InputPin, OutputPin`
   (`src/jls/elem/Pin.java:19-20`). The permits list is widened to admit
   `InoutPin`; that one line is the change that makes the rest compile.
   `InoutPin` carries one `Input` and one `Output` put plus a 1-bit enable,
   saves as `ELEMENT InoutPin` with `name`, `bits`, `watch` and `orientation`
   in the shape `InputPin.save` uses (`src/jls/elem/InputPin.java:95-102`), and
   implements `TriProp` as `InputPin` does (`:23`).
4. **Registration and policy, or the build fails.** A row in
   `ElementRegistry.ALL` (`src/jls/elem/ElementRegistry.java:38-77`), and
   membership in exactly one `HdlExporter` bucket - `EXPORTED`
   (`src/jls/hdl/HdlExporter.java:429-435`) is the intended one. Omitting it
   fails `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`
   (`test/jls/hdl/HdlPolicyTest.java:392-407`). This is the FEAT-001 ratchet
   working as designed; do not route around it.
5. **The exporter builds the port.** `HdlExporter` constructs `HdlModel.Port`
   at three sites (`:278`, `:292`, `:301`), all with a hard-coded direction; a
   fourth is added for the new element.
6. **Readback is the existing tri-state substrate, not a new one.** When the
   enable is low the element drives nothing and its `Input` reads the resolved
   net value; when high it drives. Resolution, conflict detection and the
   one-time conflict warning are already specified and implemented
   (`docs/simulation-semantics.md` §9, `:409-441`; `WireNet.recheck`/`makeNet`
   in `src/jls/elem/WireNet.java`). Reference that section; do not restate or
   modify it.
7. **The importer stops refusing inout module ports.** `mapPorts`'s `default:`
   arm (`src/jls/hdl/imp/NetlistImporter.java:184-189`) currently reports
   "an inout (tri-state bus) ... this importer increment does not yet emit";
   it becomes a realization.
8. **`docs/file-format.md` gains the element row** in the table at `:305-325`,
   with the format bump its own rules require (`:427-446`).

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-021 | this is the feature end to end: the IR case, the element, and every emitter's handling of it |
| FEAT-027 | a bidirectional pin is the first element whose correctness *depends* on multi-driver resolution rather than merely tolerating it, which is what turns the strength lattice from a nicety into a requirement |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| - | none. Tri-state nets, driver disabling and deterministic conflict resolution all exist at HEAD (`docs/simulation-semantics.md` §9); this task consumes them |

## Acceptance test

`test/jls/elem/InoutPinTest.java`, new:

- `anEnabledInoutPinDrivesTheNet()` / `aDisabledInoutPinDrivesNothing()` -
  assert the net's resolved value and that the disabled pin contributes no
  driver, using the tri-state assertions
  `SimulationSemanticsRegressionTest` already establishes.
- `aDisabledInoutPinReadsWhatAnotherElementDrives()` - the readback half; this
  is the assertion that fails if the element is built as an output-only pin
  with a second name.
- `twoEnabledInoutPinsWithDifferentValuesReportOneConflict()` - reuses the
  §9 conflict contract: deterministic winner, warning reported once.
- `anInoutPinRoundTripsThroughSaveAndLoad()`.

`test/jls/hdl/InoutPortExportTest.java`, new:
`verilogDeclaresInout()`, `vhdlDeclaresInout()`,
`theConstraintFileLabelsTheDirectionAsInout()` - one assertion per switch
converted in item 2, so none of the four can regress to the old ternary.

`test/jls/hdl/HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` must stay
green **without being edited**; if it needs editing, the element was added
without an export decision.

`test/jls/hdl/imp/NetlistImporterTest`, extended:
`anInoutModulePortImportsAsAnInoutPin()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | overlaps - #63's shipped scanner already emits `ScannedPort.Direction.INOUT`, which nothing downstream can represent |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | overlaps - removes one of the importer's remaining structural refusals |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the registry half already shipped and is exactly what forces item 4 |

**No issue** proposes bidirectional ports. Six external formats need them and
the tracker has never named them.

## Notes

- **The sealed permits clause is the trap that bites first.**
  `src/jls/elem/Pin.java:19-20`. A new pin subclass will not compile until that
  line changes, and a reviewer who does not know the class is sealed will read
  the error as unrelated.
- **The four ternaries are the trap that bites silently.** Each of them today
  maps a hypothetical `INOUT` to `output`/`out` with no diagnostic. Convert all
  four in one change; converting three produces a build that emits a legal
  Verilog module and an unusable constraint file.
- **`blinky_icestick.pcf` is a golden.**
  (`test/resources/hdl/board/blinky_icestick.pcf`, pinned by `PcfGoldenTest`.)
  It contains no inout port today, so it should not change; if it does, the
  direction-comment switch was written wrong.
- **Do not decide the strength model here.** FEAT-027 owns driver kinds, pull
  elements and the lattice. This task ships a bidirectional pin over HEAD's
  two-state, no-wired-AND/OR semantics, which `docs/simulation-semantics.md`
  §9 states explicitly ("There is no wired-AND/OR and no conflict (X) state").
  Write the element so that adding strengths later changes resolution, not the
  element.
- **The importer's `Direction` and the model's are different enums.**
  `YosysNetlist.Port.direction()` (consumed at `NetlistImporter.java:176-190`)
  is the netlist's; `HdlModel.Direction` is the model's; `ScannedPort.Direction`
  is the scanner's. Three enums, three switches - do not unify them in this
  task, but do make all three total.

## Evidence

- `src/jls/hdl/HdlModel.java:28-33` - two constants at HEAD; `:43-46` the
  `Port` record.
- `src/jls/hdl/VerilogEmitter.java:111-114`, `src/jls/hdl/VhdlEmitter.java:128-131`,
  `src/jls/hdl/board/PcfEmitter.java:74-76`, `:193-194` - the four ternaries.
- `src/jls/hdl/HdlExporter.java:278`, `:292`, `:301` - port construction;
  `:429-435`, `:459-478`, `:480-495` - the policy buckets and
  `classifiedElementClasses`.
- `src/jls/elem/Pin.java:19-20` - the sealed permits clause.
- `src/jls/elem/InputPin.java:23`, `:95-102` - `TriProp` and the save shape.
- `src/jls/hdl/scan/ScannedPort.java:13-20` - `INOUT` already scanned.
- `docs/simulation-semantics.md:409-441` - tri-state nets, resolution, bus
  conflicts, the deterministic winner and the one-time warning. Normative;
  reference only.
- `09-format-adoption-plan.md` - the 2-4 mw band and the count of external
  formats that cannot be honest without `INOUT`.
