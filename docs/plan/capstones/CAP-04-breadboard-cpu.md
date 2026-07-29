# CAP-04 - A breadboard implementation of a simple CPU

**Status:** proposed | **Priority:** 8 | **Marginal cost:** 31-46 mw |
**Standalone cost:** 50-77 mw

## Outcome

A drawn CPU projects to a solderless-breadboard build of real 74-series DIP
packages - packing, refdes, wiring list, placement - and the physical
arrangement the student actually wired simulates with the electrical behavior
of the parts, including floating inputs, contention and fan-out.

## Acceptance test

Three tiers. The scope reading is the maintainer's binding one: *both* a canvas
*and* a physical simulation, over a named published design (SAP-1), not one JLS
invents.

**AT-C4-A (build plan; headless).**
SEEN: `jls -breadboard examples/sap1.jls -lib 74ls -o plan/` emits `bom.txt`,
`refdes.map`, `wiring.net` (`U3.1 -- U7.11 net BUS0`), `placement.brd` and
`drc.txt`; a person orders the parts and builds it.
CHECK: `BreadboardPlanGoldenTest` - (1) packing totality, every logic element in
exactly one (refdes, section), no section double-booked, errors aggregated per
the `PinBindings.parse` idiom; (2) netlist equivalence, the union-find partition
over `placement.brd` hole occupancy equals the schematic `WireNet` partition
pushed through the binding; (3) power completeness; (4) no floating input;
(5) no contention (<=1 push-pull driver per net, or all open-drain plus a pull);
(6) fan-out, with families whose DC check is vacuous reporting "not DC-limited",
never "PASS"; (7) determinism and additive-only diff on inserting one unrelated
gate.

**AT-C4-B (canvas).**
SEEN: the student drags 35 packages onto two rendered breadboards and jumpers
hole to hole; a discrepancy overlay names, in physical terms, every join the
board has that the schematic lacks and vice versa; when it empties, Run executes
`LDA 9 / ADD 10 / OUT / HLT` and the output register shows the sum.
CHECK: `BreadboardGestureReplayTest` - a recorded gesture script replays to a
byte-identical `placement.brd` and a passing AT-C4-A; plus the
progressive-disclosure guard, `PaletteContractTest` extended per view, asserting
that with the breadboard view off the first-year palette is byte-identical to
today's.

**AT-C4-C (physical truth).**
SEEN: delete one jumper from a 74LS173 CLK net and the input goes HIGH, not LOW,
and the register free-runs as the real board would; swap to a 74HC173 and the
pin shows X, which propagates.
CHECK: `FloatingInputPhysicalTruthTest` - undriven 74LS173 CLK resolves to `1` at
`pull` strength; undriven 74HC173 CLK resolves to `X`; the same pin driven LOW
resolves to `0` at `strong` strength and must differ from the first. The third
assertion is the falsification guard: at HEAD all three produce the identical
integer `0`.

## Demo slice

AT-C4-A alone, headless, no GUI and no four-state core: the package library as
data plus packing, stable-id refdes, BOM, pin-level wiring list and the static
DRC. 7-14 mw. It says "here is your drawn CPU as 7x 74LS00, 3x 74LS74, and here
are the three inputs you left floating" and is legible to a first-year, an
instructor and a hiring manager.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-040 | Package and pinout library as data | 74-series pinouts, sections, gate equivalence - the primary data the whole capstone projects onto | required |
| FEAT-041 | Packing, refdes, cascade and loading checks | assigns elements to packages, produces the BOM and wiring list, and owns the fan-out and cascade rules | required |
| FEAT-027 | Strength lattice, driver kinds, net kinds | floating-HIGH TTL, open-drain, pull-ups and contention are the pedagogical payload | required |
| FEAT-026 | Four-state value core with a resolution fold | X on a floating CMOS input, and resolution that is a fold rather than first-driver-wins | required |
| FEAT-043 | Breadboard canvas and physical-simulation binding | the second canvas and the hole-occupancy union-find that AT-C4-B replays | required |
| FEAT-004 | Shared net-partition IR with stable net naming | AT-C4-A check 2 compares two partitions; both must come from one pass | required |
| FEAT-014 | Stable addressing and per-view geometry | breadboard geometry lives in its own section keyed by the same instance identity as the schematic | required |
| FEAT-013 | Per-section internal versioning | the breadboard section must be skippable by an older reader, not fatal | required |
| FEAT-001 | Registry-keyed table totality | the packing table is keyed on the element registry; a new element must not silently fall through | required |
| FEAT-008 | `SimpleEditor` decomposition and UI harness | a second canvas cannot be added to a 4,119-line editor, and AT-C4-B needs a harness to replay gestures | required |
| FEAT-042 | KiCad and gEDA emitters, manufacturability gate | shares the cascade rule and the footprint column; CAP-04 consumes the same package layer | beneficial |
| FEAT-015 | Headless `CircuitOp` layer | the gesture replay and the headless `-breadboard` path both need mutation without a `Graphics` | required |
| FEAT-017 | Shared and parameterized subcircuit definitions | SAP-1 is drawn as repeated modules; N deep copies pack to N divergent BOMs | beneficial |
| FEAT-030 | Engine constant factors | the physical simulation runs the same machine at package granularity, so event cost is the throughput budget | beneficial |
| FEAT-031 | Per-instance fidelity toggle | bring the board up one module at a time against the schematic behavior | beneficial |
| FEAT-032 | Host byte port, `Console`, transcripts | the SAP-1 output register display and its recorded run | beneficial |
| FEAT-034 | Retirement-indexed parity harness | proves the breadboard build executes the same program as the schematic | beneficial |
| FEAT-053 | Test-vector front end and autograding | "the disagreement is the grade" is only useful if the disagreement is reportable | beneficial |
| FEAT-025 | Logisim-Evolution `.circ` importer | through-hole part data absorbed by the importer work feeds FEAT-040 | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) CAP-04 has no tracking issue | no issue |
| #84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher | depends on |
| #91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on |
| #162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on |
| #232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | depends on |
| #167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | depends on |
| #214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | overlaps |
| #78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract | informs |
| - | (no issue) the package library, the packing pass and the breadboard canvas | no issue |

## Open decisions

1. **Default 74-series subfamily.** Recommend 74LS as the shipped default:
   AT-C4-C's floating-HIGH assertion is only true for TTL, and the HC contrast is
   what makes the X case teach. Reason: the pedagogy needs both, and one must be
   the default the library ships.
2. **Canvas connectivity storage.** Recommend storing connectivity *nowhere* in
   the breadboard view and deriving it by union-find over hole occupancy, with
   the schematic relationship expressed as a check rather than a merge. Reason:
   Fritzing stores per-view connectivity and reconciles by ratsnest; that is its
   single longest-standing complaint. This is also what keeps the canvas at
   5-8 weeks rather than a program.
3. **How much of the strength lattice CAP-04 buys.** Recommend funding the
   floating/pull/strong/open-drain subset in full rather than a subset. Reason:
   the corpus measures no cheap subset of the value work that buys physical
   truth; a partial one leaves AT-C4-C unsatisfiable.
4. **Whether the discrepancy message is a budget line.** Recommend budgeting
   iteration on the *message*, not only the computation. Reason: the
   schematic-to-breadboard correspondence is not a bijection, so a wrongly joined
   pair of holes has no logical net name to attribute the error to.
5. **Does CAP-04 pay for the second canvas or does CAP-12?** Recommend CAP-04
   pays, since FEAT-043 is its private spine and CAP-10/11/12 consume it later.

## Kill criteria

- K1. If, after FEAT-027 and FEAT-026 land, `FloatingInputPhysicalTruthTest`'s
  three assertions cannot be made to differ - all three still yield the same
  resolved value - the physical-truth tier is unbuildable and AT-C4-C is struck.
- K2. If AT-C4-A's check 2 (partition equivalence) cannot be made to run in
  O(n a(n)) over a 35-package SAP-1 within the fast CI lane's budget, the check
  moves to the long-run lane or the capstone loses its regression gate.
- K3. If the coverage commons cannot absorb a second canvas without dropping the
  `jls.edit` floor, AT-C4-B is deferred and the capstone ships as AT-C4-A only.
- K4. If more than one further element type joins the 9-of-35 registry types with
  no manufacturable realization, the packing pass covers a shrinking fraction of
  real designs and the default library scope must be re-argued.
- K5. If the strength-lattice work exceeds 1.5x its band before the first
  falsifying assertion in `FloatingInputPhysicalTruthTest` turns green, stop and
  re-cost.

## Evidence

- Floating inputs are LOW at HEAD: `src/jls/elem/LogicElement.java:477-482`,
  "Initialize all inputs to 0", `in.setValue(BitSetUtils.Create((long)0))`.
  Verified at `b54e6ee`. This is what makes AT-C4-C's third assertion a
  falsification guard rather than a formality.
- No physical vocabulary at HEAD: `grep -rniE "footprint|refdes|pinout" src/`
  returns zero hits. Verified at `b54e6ee`.
- The aggregation idiom AT-C4-A check 1 follows already ships:
  `src/jls/hdl/board/PinBindings.java` (98 lines), with
  `src/jls/hdl/board/PcfEmitter.java` (199 lines) and
  `src/jls/hdl/board/Boards.java` (125 lines) as the data-not-code precedent.
- Sealed `Put` makes AT-C4-A checks 4-6 computable: `src/jls/elem/Put.java:16-17`,
  `public abstract sealed class Put permits Input, Output`.
- Word-level vs package-level gap: `Adder.resetPropDelay` is
  `propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`) for an
  element of arbitrary width, while a 74LS83 is 4 bits - the cascade rule
  FEAT-041 owns.
- 35 registry element types at HEAD (`src/jls/elem/ElementRegistry.java:38`,
  35 `new ElementType(` entries), of which the corpus identifies 9 with no
  cascadable 74-series realization.
- Scope reading, tiers, costs and the "compared by a check, not reconciled by a
  merge" insight: `10-capstone-plan.md` §1.4; the P1 subset pricing and the
  packing arithmetic: `cap-c4-breadboard.md` §3.1, §5.3, §7.
- Do not restate: simulation semantics live in `docs/simulation-semantics.md`;
  the file format in `docs/file-format.md`; program ownership in
  `docs/capability-roadmap/`.
