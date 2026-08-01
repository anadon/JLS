# CAP-13 - KiCad interoperation parity

**Status:** proposed | **Priority:** 2 | **Marginal cost:** 6-12 mw |
**Standalone cost:** 12-22 mw

## Outcome

A circuit drawn in JLS leaves as a file KiCad opens without hand editing, and
the net structure KiCad recovers is provably the same net structure JLS
simulated - so a student who has drawn a working circuit can start a board
instead of drawing it a second time.

## Acceptance test

**What "parity" means here.** Not feature equality with KiCad and not a
schematic that looks the same. Parity is three checkable interoperation claims,
in this order:

1. **Accepted.** The artifact JLS writes is read by the reference reader with no
   hand editing and no error.
2. **Structure preserved.** The net partition the other tool recovers is
   *isomorphic* to JLS's own partition, matched by stable id - same nets, same
   membership, same pin-to-net incidence.
3. **Round trip bounded.** Reading a KiCad netlist back into JLS yields that
   same partition, and every construct that did not survive either direction is
   named in a report rather than dropped.

SEEN: a student runs `jls -export design.sch design.jls`, opens the result in
KiCad 10 via File - Import - Non-KiCad Schematic, sees their components and
nets, assigns footprints from KiCad's own library, and pushes to `pcbnew`. The
JLS-side report lists what was approximated.

CHECK: four named tests.
- `GedaSchematicGoldenTest` - byte-identical `.sch` output for the fixture
  corpus, symbols synthesized from element geometry and embedded, `netname=`
  derived from stable id. Golden-pinned like the shipped emitter goldens.
- `LeptonNetlistAcceptanceTest` - `lepton-netlist` reads the emitted file and
  exits zero, under the shipped skip-when-absent idiom
  (`test/jls/hdl/GhdlCompileTest.java:34-36`). This is claim 1, and it is what
  turns a shape golden into "the format's reference reader accepted it".
- `NetPartitionIsomorphismTest` - the partition recovered from the emitted
  artifact is isomorphic to `jls.netlist`'s own partition for every fixture.
  This is claim 2 and it is the parity assertion; the other three are supports.
- `KiCadNetlistReadbackTest` - a KiCad `.net` read back produces the same
  partition, and the migration report enumerates every unmapped construct.

## Demo slice

Falsify the embedded-symbol claim first: one hand-written ten-line gEDA `.sch`
with a single `[ ... ]` block, opened in KiCad 10 - one afternoon, and it decides
whether the whole cheap path exists. Then emit one flat two-package design and
prove `lepton-netlist` recovers JLS's partition. **2-3 mw**, and it establishes
all three parity claims on a small design before any package data is acquired.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-042 | KiCad and gEDA netlist emitters with a manufacturability gate | the emitters are this capstone's spine; the gate is their acceptance criterion | required |
| FEAT-004 | Shared net-partition IR with stable net naming | claim 2 is an isomorphism against a partition, and there must be exactly one partition pass or JLS acquires two that can disagree | required |
| FEAT-040 | The package and pinout library as data | `pcbnew` refuses a component with no footprint, so the binding mechanism is a hard gate on the `.net` route | required |
| FEAT-041 | Packing, refdes, cascade and electrical loading checks | a KiCad component record needs a reference designator and a part value, assigned deterministically | required |
| FEAT-001 | Registry-keyed table totality discipline | a new emitter inherits the export policy; a non-total policy silently drops element types out of the netlist | required |
| FEAT-021 | Bidirectional ports in the IR and the element vocabulary | KiCad pin types include bidirectional and JLS cannot currently say it | required |
| FEAT-018 | Hierarchical instance structure in the HDL IR | a design worth a board is decomposed, and today a decomposed design is refused on export | required |
| FEAT-027 | Strength lattice, driver kinds and net kinds | a real board has pull-ups and open-drain nets; a netlist that cannot express them is not a board netlist | required |
| FEAT-003 | Uncompressed canonical default with stable-id references | the emitted artifact and the source file must both diff cleanly for a review workflow to exist | beneficial |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | refdes assignment and cross-probing key off stable addresses | beneficial |
| FEAT-019 | Yosys JSON write | reaches ngspice-shaped and gate-level consumers through Yosys without a second lowering pass in JLS | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) CAP-13 has no tracking issue | no issue |
| - | (no issue) the whole package/footprint binding mechanism, FEAT-040 through FEAT-042 | no issue |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | informs - the shared net partition and export policy this capstone consumes are that issue's substrate, but it does not cover PCB output |
| #78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - FEAT-001 builds on its registry half, which has shipped |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - the same `Board`/`PinBindings` binding shape, a different destination |

## Open decisions

1. **Which artifact carries the claim: the gEDA `.sch` schematic or the KiCad
   `.net` netlist?** Two corpus documents propose different routes.
   *Recommendation: the schematic.* Reason: `09-format-adoption-plan.md` §4.2 and
   §10.2 record that the netlist is mechanically dead-ended for a footprint-less
   component while KiCad 10 ships a gEDA importer and gEDA embeds its symbols, so
   the schematic route reaches KiCad *and* lepton-schematic *and* lepton-netlist's
   backends with no library curated. Fund the `.net` emitter second, over the same
   partition, once package data exists.
2. **Does JLS ever ship footprint or symbol geometry?** *Recommendation: never.*
   Reason: `08-views-determination.md` §5 prices the incumbent library in the
   thousands of parts maintained by a team; one maintainer would own every part
   forever. Refuse the library, ship the emitter - they are separable precisely
   because the destination tool has the library.
3. **Is positional pin order frozen before or after the first emitter?**
   *Recommendation: before, and measure first.* Reason: it is the only
   silent-when-wrong item in the format study - a mis-ordered component record
   parses, imports and yields the wrong board.
4. **Does parity include geometry?** *Recommendation: no, and say so in the
   emitted header.* Reason: claim 2 is an isomorphism on nets; asserting visual
   fidelity would put JLS on the hook for a coordinate system KiCad owns.
5. **Who owns the package binding?** *Recommendation: a standalone item, not a
   silicon program slice.* Reason: `09-format-adoption-plan.md` §6.1 records that
   both sibling surveys route packages to the silicon program, which owns cells,
   not packages - so the work is unowned and would inherit that program's gating
   by accident.

## Kill criteria

- K1. If the embedded-symbol falsification fails and the emitter must ship
  per-symbol sidecar files, and the sidecar count per design scales with the
  part count rather than the element-type count, the "no library curated" claim
  has collapsed and the schematic route should be re-costed against the netlist
  route before any further weeks are spent.
- K2. If fewer than 100% of components in an exported netlist can be given a
  footprint by the binding mechanism, the `.net` route yields zero usable
  components - the destination tool refuses all of them, not most of them - and
  that route must be dropped rather than partially shipped.
- K3. If `NetPartitionIsomorphismTest` cannot be made green for any fixture
  because of a KiCad or gEDA semantic JLS's value domain cannot express, the
  parity claim must be narrowed in writing to the fixture classes that pass,
  before release, rather than asserted generally.
- K4. If the destination tool's importer changes shape in two consecutive major
  releases such that the golden must be regenerated each time, the path is
  perishable maintenance at bus factor 1 and should be documented as a recipe
  rather than shipped as an emitter.

## Evidence

- Verified at `b54e6ee`: `grep -rli kicad src/`, `geda`, `lepton` each return
  **0** - nothing on this path exists at HEAD.
- The binding mechanism ships in miniature and is the structural precedent:
  `src/jls/hdl/board/PinBindings.java` (98 lines),
  `src/jls/hdl/board/PcfEmitter.java` (199 lines),
  `src/jls/hdl/board/Board.java` (159), `Boards.java` (125) with a `Board.Format`
  enum a second board format extends.
- The projection thesis is shipped, not proposed: `VerilogEmitter.java` (752
  lines) and `VhdlEmitter.java` (1,149) render the same `HdlModel` port walk in
  unrelated syntaxes; a netlist emitter is a fourth renderer over the same walk.
- The hierarchy gate is real at HEAD: `src/jls/hdl/HdlExporter.java:460-477` rejects
  `SubCircuit` with the reason "the HDL model has no module-instantiation
  statement"; `grep -rn InstanceStatement src/` returns **0**.
- The `INOUT` gap: `src/jls/hdl/HdlModel.java:28-33` defines `Direction` as
  `{INPUT, OUTPUT}`.
- Cost band basis: `08-views-determination.md` §2.1 (netlist printer 1-2 wk over
  the package table) plus its Tier 3 schematic path (5-8 wk);
  `09-format-adoption-plan.md` §4.2 and W4.1-W4.3 for the schematic route, and
  §10.2 for the recorded verdict flip.
- Do not restate: `docs/file-format.md` owns the `.jls` container,
  `docs/simulation-semantics.md` owns the value domain, `ARCHITECTURE.md` owns
  the layering, `docs/capability-roadmap/` owns program boundaries and their
  cost bands.
