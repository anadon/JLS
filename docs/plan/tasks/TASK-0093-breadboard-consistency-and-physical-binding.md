# TASK-0093 - Breadboard consistency check and physical binding

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0086, TASK-0092,
TASK-0058

## Deliverable

Two things that together make the breadboard a simulation input rather than a
picture: a net-equivalence check between the two views, and a binding that
makes the *placed* arrangement drive the simulation, including the contention
the schematic hides.

Precisely what changes:

- `jls/bread/PhysicalNetExtractor.java`: derives the electrical net partition
  from the breadboard placement alone - tie-point strips union the pins that
  share them, the two power rails are strips like any other - producing the
  same `NetPartition` type TASK-0007 defines. This is the whole point: two
  independent derivations of one design's connectivity, comparable because
  they are the same type.
- `jls/bread/ConsistencyCheck.java`: compares the schematic partition against
  the physical partition and reports **per discrepancy**, each with a stable
  rule id and the two element/pin endpoints involved:
  - `C1_SPLIT_NET` - one schematic net realized as two or more physical nets
    (a wire the student did not run).
  - `C2_MERGED_NET` - two schematic nets realized as one (a short across a
    tie-point column).
  - `C3_UNPLACED_PART` - a packed part with no placement.
  - `C4_UNCONNECTED_PIN` - a placed pin in no strip and on no wire.
  - `C5_POWER_UNCONNECTED` - a package's declared power/ground pin on no rail.
  - `C6_CONTENTION` - two drivers on one physical net that the schematic did
    not have on one net. This is the finding the schematic *cannot* produce
    and is the reason the check is worth building.
- `jls/bread/PhysicalBinding.java`: an explicit, saved, per-circuit choice of
  which partition the simulator consumes - `SCHEMATIC` (default, unchanged
  behavior) or `PHYSICAL`. When `PHYSICAL`, the simulator is elaborated over
  `PhysicalNetExtractor`'s partition, so a missing wire fails in simulation
  exactly as it would on the bench.
- CLI: `-breadboard-check <file>` batch report, machine-readable in the
  TASK-0091 renderer's shape (`rule-id<TAB>severity<TAB>ids<TAB>message`), exit
  2 when any `ERROR`-severity discrepancy exists.

Done means: for a fixture whose breadboard is deliberately mis-wired, the
report names each discrepancy by rule id and by both endpoints; and running the
same circuit under `PHYSICAL` binding produces a simulation result that differs
from the `SCHEMATIC` run in exactly the way the report predicts.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-043 | The "consistent with the schematic" and "the physical arrangement simulates" halves - everything in FEAT-043 that is not the canvas itself. |
| FEAT-041 | Closes the packing loop: packing produces an assignment, this proves the assignment was realized. |
| FEAT-027 | Supplies the first consumer that genuinely needs driver kinds and net kinds - C6 is undecidable without them. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0092 | The physical extractor reads placements; only TASK-0092 creates them. |
| TASK-0086 | C3 and C5 read the `PackPlan` - the refdes and section assignment - and the package's declared power pins; only TASK-0086 produces it. |
| TASK-0058 | C6 reports contention. At HEAD `WireNet` resolution is "first active driver in net order wins" with no strength concept, so "these two drivers conflict" is not expressible. TASK-0058 creates the strength lattice and the driver/net kinds that make the finding decidable rather than a guess. |

## Acceptance test

`test/jls/bread/ConsistencyCheckTest` (new class):

- `everyConsistencyRuleHasAFixtureThatTripsIt()` - reflects over the rule enum
  and asserts each constant is produced by at least one committed fixture. The
  totality assertion; a new rule without a fixture fails the build.
- `aCorrectlyWiredBreadboardReportsNoDiscrepancies()` - the reference fixture
  (a packed four-NAND design placed correctly) yields an empty report and exit
  0.
- `aMissingJumperIsReportedAsC1WithBothEndpoints()` - deletes one jumper from
  the reference fixture and asserts a single `C1_SPLIT_NET` finding naming the
  two stable ids that should have been one net.
- `twoDriversOnOneStripAreReportedAsC6()` - the fixture the schematic view
  passes and the breadboard view fails.
- `physicalBindingChangesTheSimulationExactlyWhereTheReportSaysItDoes()` in
  `test/jls/bread/PhysicalBindingTest` - runs the mis-wired fixture under both
  bindings and asserts the outputs differ at, and only at, the nets the report
  named.
- `schematicBindingIsByteIdenticalToHead()` - the whole committed golden corpus
  re-run with the binding present and set to `SCHEMATIC`, asserting byte
  identity. This is the no-regression gate.

## Related GitHub issues

**no issue.** `search_issues` over `anadon/jls` for `breadboard` returns
nothing; the registry records FEAT-040 through FEAT-044 as having no tracker
representation at all. Adjacent:

| # | title | relationship |
|---:|---|---|
| #232 | Simulation hot path: per-signal `java.util.BitSet` allocation… evaluate a value-typed (long,width) signal representation | informs - the value-representation work FEAT-026/FEAT-027 ride on; C6's honesty depends on that program, not on this task. |

## Notes

- **This is LVS at teaching scale, and that framing is the useful one.** Net
  equivalence between two views of one model is the same check a P5-shaped ERC
  does, it is gradeable headless, and it produces a per-discrepancy report
  rather than a verdict. Do not let it become a layout-versus-schematic tool
  for boards; the scope is two JLS views.
- **The C6 trap.** Contention is the finding that justifies the whole task, and
  it is the one that is dishonest without FEAT-027. Reporting "contention"
  while the engine resolves by draw order would produce a report the simulation
  contradicts. If TASK-0058 has not landed, ship C1-C5 and mark C6 explicitly
  unimplemented in the rule enum with a `@Deprecated`-style marker the totality
  test recognizes - never emit it speculatively.
- **The default binding must stay `SCHEMATIC`.** Every committed golden was
  produced against the schematic partition. Flipping the default would
  invalidate the golden corpus for a feature almost nobody uses.
- **`WireNet` ordering is pinned.** `SimulationSemanticsRegressionTest.
  multiDriverConflictResolvesDeterministicallyAndWarnsOnce`
  (`test/jls/SimulationSemanticsRegressionTest.java:321`) pins the current
  resolution. If the physical extractor fabricates synthetic `Wire`s for strips
  (TASK-0092's safe option), their insertion order becomes load-bearing and
  must be a declared function of tie-point address, not of placement order.
- **Fan-out is a separate report.** TASK-0088 owns the DC loading check;
  this check must reference its findings rather than recompute them, exactly as
  TASK-0091 does.

## Evidence

- The two-derivations design is possible because the schematic partition is
  already computed in shipped code: `src/jls/hdl/HdlExporter.java:208-241,253,1146-1161`
  (union-find over `WireNet`, jump-alias union, `Group{name,bits,isPort}`),
  which TASK-0007 extracts into `jls.netlist`.
- Contention is not expressible at HEAD: `docs/simulation-semantics.md:47-49`
  ("There is no unknown/X state anywhere in the simulator") and `:60-67`
  (a null/HiZ input is read as zero by nearly every `react`); `WireNet` is
  `LinkedHashSet<WireEnd>` + `LinkedHashSet<Wire>`
  (`src/jls/elem/WireNet.java:22,24`) with order-dependent resolution.
- Fan-out is unmodeled at HEAD - one gate drives arbitrarily many inputs with
  no cost - which is why C5/C6 need package data
  (`08-views-determination.md` §1.6).
- The consistency check as the real content of "does JLS want LVS":
  `08-views-determination.md` §1.6 and its Tier-3 table (checker costed 2-3 wk).
- Op addressing by stable id, which the placements inherit:
  `src/jls/collab/op/CircuitOp.java:30-32`.
