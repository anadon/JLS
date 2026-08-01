# CAP-05 - A manufacturable PCB

**Status:** proposed | **Priority:** 4 | **Marginal cost:** 11-19 mw |
**Standalone cost:** 15-23 mw

## Outcome

A student's drawn circuit leaves JLS as a netlist a real PCB tool imports
without hand editing, carrying footprints, refdes and a BOM, and the board built
from it passes DRC, produces a fab package, and works when populated - with JLS owning
the electrical content (part, package, section, pin, net, footprint) and the PCB
tool owning the physical content (placement, routing, stackup, DRC).

## Acceptance test

SEEN: `jls -export sap1-alu.net -parts 74ls.parts examples/sap1-alu.jls` prints
"packed 14 logic elements into 7 packages; wrote sap1-alu.net (7 components,
31 nets, 84 nodes, 0 unbound pins)". In KiCad, **File -> Import Netlist...**
reports "7 footprints added, 0 errors". The student places and routes.
`kicad-cli pcb drc --severity-error --exit-code-violations sap1-alu.kicad_pcb`
returns 0 violations; `kicad-cli pcb export gerbers` and `export drill` produce
the fab package. It is uploaded for roughly $30 and three weeks later a board
arrives, is populated, is clocked, and the accumulator adds.

CHECK, JLS side: `KicadNetlistAcceptanceTest`, headless, eight assertions -
(1) packing totality; (2) **footprint totality**, every `(comp)` record carries a
non-empty `(footprint ...)`; (3) every pin bound, including VCC/GND, with
unused-section inputs tied rather than omitted; (4) no unconnected net (>=1
Output node, >=1 Input node, >=2 nodes); (5) partition round-trip - re-parsing
the emitted `.net` induces a partition over (refdes, pin) equal to the source
`WireNet` partition pushed through the packing binding; (6) byte-identical
re-run; (7) additive-only diff after inserting one unrelated gate;
(8) fixture correspondence - the regenerated netlist is byte-identical to
`test/fixtures/sap1-alu.net`, the file the committed
`test/fixtures/sap1-alu.kicad_pcb` was built from.

CHECK, tool side: `KicadBoardDrcTest`, opt-in through
`ToolLocator.findOnPath("kicad-cli")` plus `Assumptions.assumeTrue`, the shipped
idiom - `pcb drc --format json` exits 0, `pcb export gerbers` and `pcb export
drill` match a committed manifest, and `pcb drc --schematic-parity` passes if the
schematic emitter is built. Pin the container by digest.

FALSIFICATION GUARD: checks 2 and 8 fail today for every JLS design, because no
footprint or refdes vocabulary exists at HEAD and refdes is not yet a pure
function of circuit content.

## Demo slice

Stage 1 is the SAP-1 accumulator plus adder module, roughly 7 packages, not the
full 35-package machine: the package library with a `footprint` column, the
packing pass, stable-id refdes and the netlist emitter, checked by assertions
1-7 with the tool-side test opt-in. It ends in a netlist KiCad imports cleanly.
5-9 mw of the band.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-042 | KiCad and gEDA emitters with a manufacturability gate | the netlist itself, plus the check that says whether the board can be built | required |
| FEAT-040 | Package and pinout library as data | the footprint column, the pinout and the section table the emitter projects | required |
| FEAT-041 | Packing, refdes, cascade and loading checks | packing totality, canonical refdes, the width-cascade rule and the synthetic nets it creates | required |
| FEAT-004 | Shared net-partition IR with stable net naming | check 5 compares two partitions and check 7 needs names that do not depend on save order | required |
| FEAT-001 | Registry-keyed table totality | the packing and footprint tables are registry-keyed; a fall-through is a silently unmanufacturable board | required |
| FEAT-021 | Bidirectional ports in the IR and the vocabulary | a real board has bidirectional buses; `HdlModel.Direction` has no `INOUT` at HEAD | required |
| FEAT-027 | Strength lattice, driver kinds and net kinds | open-drain and pull-ups are ordinary board practice and check 4 must not call them unconnected | required |
| FEAT-003 | Uncompressed canonical default with stable-id references | check 7's additive-only diff is a property of the saved file, not just the netlist | required |
| FEAT-014 | Stable addressing and per-view geometry | refdes must be a pure function of circuit content, which means a stable instance identity | required |
| FEAT-013 | Per-section internal versioning | the package binding rides as its own versioned section | required |
| FEAT-018 | Hierarchical instance structure in the HDL IR | a decomposed design must export without flattening for the netlist to name real modules | required |
| FEAT-026 | Four-state value core with a resolution fold | contention and undriven nets on a real board are not "0" | beneficial |
| FEAT-015 | Headless `CircuitOp` layer | the export path and its fixtures must run without a `Graphics` | beneficial |
| FEAT-017 | Shared and parameterized subcircuit definitions | one definition, N instances - otherwise N copies pack to N divergent BOMs | beneficial |
| FEAT-037 | Reset semantics, clock and domain architecture | a populated board needs an honest power-on reset, not an initial value | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) CAP-05 has no tracking issue | no issue |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps |
| #232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | depends on |
| #167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | depends on |
| #78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract | informs |
| - | (no issue) the package layer, footprint column, packing, refdes and the PCB netlist emitter | no issue |

## Open decisions

1. **The format reframe.** Recommend adopting it explicitly: JLS emits the
   electrical netlist, KiCad owns the board. Reason: `pcbnew` discards any
   netlist component whose footprint field is empty, and that gate opens with one
   string per package in a data table - roughly one week - whereas owning
   placement and routing is a different tool.
2. **Committed board fixture.** Recommend committing a
   `test/fixtures/sap1-alu.kicad_pcb` produced by a human, once, and letting CI
   prove forever that JLS still emits exactly the netlist it was built from.
   Reason: netlist -> board cannot be automated - there is no `sch import` job
   and `pcb import --format` does not accept netlists. That is how hardware CI
   always works.
3. **Default 74-series subfamily and its footprints.** Recommend the same default
   as CAP-04, one package layer shared by construction. Reason: two libraries
   diverge within one semester.
4. **Where the cascade rule lives.** Recommend putting it in the IR from the
   start rather than in the emitter. Reason: cascading creates synthetic nets
   that exist in no `.jls` file, so the emitter is not a pure projection of the
   `WireNet` partition; bolting that on later invalidates check 5.
5. **Scope of the shipped footprint library.** Recommend shipping only the
   footprints the default package library names, and no more. Reason: KiCad's
   ~15k footprints are maintained by a team; one maintainer owns forever whatever
   ships.

## Kill criteria

- K1. If the packed fraction of a representative student design falls below the
  point where a `-parts` binding is doing most of the work, the default library
  scope is wrong and must be re-argued before more parts are added.
- K2. If check 5 (partition round-trip) cannot be made to hold in the presence of
  cascade-synthesized nets, the cascade rule is in the wrong layer; stop and move
  it into the IR before continuing.
- K3. If `kicad-cli` DRC or export invocations prove unpinnable to a digest and
  the tool-side lane becomes nondeterministic, the tool-side check is demoted to
  advisory and the capstone's claim narrows to "a netlist KiCad imports".
- K4. If the footprint column plus packing exceeds 1.5x its band before the first
  clean `Import Netlist...` in a real KiCad session, stop and re-cost.

## Evidence

- The footprint gate is one mechanical fact: `pcbnew` discards a netlist
  component with an empty footprint field
  (`board_netlist_updater.cpp:151-160`, KiCad ref 10.0), per
  `10-capstone-plan.md` §1.5 and `cap-c5-pcb.md` §2.3.
- No physical vocabulary exists at HEAD:
  `grep -rniE "footprint|refdes|pinout" src/` returns zero hits, verified at
  `b54e6ee`. That is what makes checks 2 and 8 falsification guards.
- Check 4 is computable today because `Put` is sealed over Input and Output:
  `src/jls/elem/Put.java:16-17`.
- The opt-in external-tool idiom already ships:
  `test/jls/hdl/GhdlCompileTest.java:34-36` -
  `ToolLocator.findOnPath("ghdl")` plus `Assumptions.assumeTrue`.
- The data-not-code precedent ships: `src/jls/hdl/board/PinBindings.java`
  (98 lines) and `src/jls/hdl/board/PcfEmitter.java` (199 lines).
- `INOUT` does not exist end to end: `src/jls/hdl/HdlModel.java:27-33` defines
  `Direction` as `{INPUT, OUTPUT}`, and `grep -rn "InOutPin" src/` returns
  nothing. Verified at `b54e6ee`.
- The width-decomposition gap: `Adder.resetPropDelay` is
  `propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`) for an
  arbitrary-width element; a 74LS83 is 4 bits.
- Scope reading, the seam, the eight assertions and the "9 of 35 registry types
  with no manufacturable realization" arithmetic: `10-capstone-plan.md` §1.5 and
  `cap-c5-pcb.md` §5.4, §6.
- Do not restate: `docs/file-format.md`, `docs/simulation-semantics.md`,
  `ARCHITECTURE.md`, and `docs/capability-roadmap/` own their own claims.
- **Cost reconciliation.** Marginal band 11-19 mw. Its 11 required features
  sum to 46-78 mw and its 4 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
