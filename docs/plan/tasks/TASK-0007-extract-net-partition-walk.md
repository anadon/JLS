# TASK-0007 - Extract the net-partition walk into its own package

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

The net-partition pass moves into a new `jls.netlist` package with its own
tests, and both of today's callers consume it instead of carrying private
copies.

There are **two** partition passes at HEAD, not one, and both move:

1. **The load-time partition**, `Circuit.finishLoad`
   (`src/jls/Circuit.java:1345-1392`): a `LinkedList<WireEnd> ends` worklist,
   an inner BFS building one `WireNet` per connected component, setting each
   end's net, folding tri-state flags and bit widths, and attaching wires to
   the net. This is the primary electrical partition and everything downstream
   depends on it.
2. **The export-time jump fold**, `HdlExporter`
   (`src/jls/hdl/HdlExporter.java:216-264`): a private `UnionFind`
   (`:1161`) that registers every `WireNet` and unions the nets bridged by
   same-named `JumpStart`/`JumpEnd` pairs, producing one `Group` (`:1146`) per
   electrical net. The exporter's own javadoc (`:98-107`) describes this as
   "the same aliasing CircuitAssert's connectivity BFS honors" - a third
   implementation of the same idea.

New package `src/jls/netlist/`, `@NullMarked`, with:

- `NetPartition` - the connected-component walk over wire ends, returning an
  immutable partition. No Swing, no `Circuit` mutation: it takes the ends and
  returns the grouping, so `Circuit.finishLoad` applies the result rather than
  computing it inline.
- `JumpAliasing` - the same-named jump fold, taking a partition and returning a
  coarser one.
- `package-info.java` declaring `@NullMarked` (`CONTRIBUTING.md`: new packages
  are born marked, and `test/jls/NullMarkedRatchetTest.java`'s list only grows).

`Circuit.finishLoad` and `HdlExporter.buildModel` both call into it. The
exporter's private `UnionFind` and `Group` are deleted or reduced to the
exporter's naming concerns only.

**This is a move, not a rewrite.** Behavior must be byte-identical: the same
nets, the same multi-driver resolution order, the same HDL output. Complexity
fixes to the walk belong to TASK-0009.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-004 | The shared net-partition IR is the feature; it cannot be shared while the walk exists twice, once inside a loader method and once inside an exporter. Every later consumer - the PCB netlist emitter, the SPICE net list, the breadboard binding - needs it as a callable pass. |

## Prerequisite tasks

None.

## Acceptance test

`test/jls/netlist/NetPartitionTest.java`, new:

- `disjointWireGroupsBecomeDistinctNets()` - two unconnected wire chains
  partition into two nets.
- `aChainOfEndsIsOneNet()` - N ends in a line partition into one net,
  independent of file order.
- `triStateAndBitWidthFoldOverTheComponent()` - a component containing one
  tri-state end reports tri-state; a component containing an `Output` put
  reports the driven flag and that put's width, matching
  `src/jls/Circuit.java:1373-1382`.
- `sameNamedJumpsFuseTwoNets()` - the `JumpAliasing` case, mirroring
  `HdlPolicyTest.twoNetsBridgedByJumpsBecomeOneVerilogNet`
  (`test/jls/hdl/HdlPolicyTest.java:332`).

The move is proved by **the goldens not moving**: `mvn verify` must stay green
with no regenerated file. Specifically `test/jls/hdl/VhdlExportGoldenTest`,
`VhdlEmitterPolicyTest`, `HdlPolicyTest` and the fixtures under
`test/resources/hdl/` are the contract that this refactor changed nothing, and
`DeterministicSaveTest.twoLoadInstancesSaveByteIdentically`
(`test/jls/DeterministicSaveTest.java:65`) is the contract that partition order
stayed deterministic.

## Related GitHub issues

**No issue.** No tracker entry covers the net-partition extraction.

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | informs - #59's staged tracking issue owns the exporter this pass is being lifted out of; nothing in it closes on this refactor |

## Notes

- **Determinism is the constraint, not performance.** `loadedElements` is a
  `LinkedHashSet` specifically so wire-net construction, and with it
  multi-driver resolution, is deterministic (`src/jls/Circuit.java:76-79`,
  issue #98). The extracted pass must consume ends in the same order; a
  `HashSet` anywhere in the new package silently breaks byte-identical saves
  and the collaboration state hash
  (`DeterministicSaveTest.stateHashIsContentDetermined:136`).
- **`WireNet` construction has side effects on ends.** `vend.setNet(net)` and
  `wire.setNet(net)` (`src/jls/Circuit.java:1372, 1390`) mutate the elements.
  Keep the *computation* pure and do the mutation in `finishLoad`, or the
  package is not reusable by the exporter, which must not re-net a loaded
  circuit.
- **The exporter's `Group` is not just a net** - it carries the chosen HDL
  name and bit width (`src/jls/hdl/HdlExporter.java:1146`), assigned by a
  five-stage naming precedence at `:266-382`. Naming is the exporter's
  business and stays there; only the partition moves.
- **`getElementsInStableOrder`** (`src/jls/Circuit.java:465-470`) is the
  documented way to iterate deterministically; `getElements()` iterates in hash
  order and its javadoc says so. Use the former in the new package's callers.
- **A third copy exists**: `CircuitAssert`'s connectivity BFS, named in the
  exporter's javadoc (`src/jls/hdl/HdlExporter.java:100-101`). Audit it during
  the move and either fold it in or record why it stays separate; leaving three
  copies after a task titled "extract the walk" is not done.
- **SpotBugs runs at threshold High** on `mvn verify`; a new package with a
  worklist algorithm commonly trips `DM_`/`EI_` findings. Fix them, do not
  add blanket `config/spotbugs-exclude.xml` entries.

## Evidence

- `src/jls/Circuit.java:1345-1392` - the load-time partition: `LinkedList` ends
  worklist, BFS, `WireNet` per component.
- `src/jls/Circuit.java:76-79` - `loadedElements` is a `LinkedHashSet` for
  determinism of exactly this pass (#98, S1).
- `src/jls/hdl/HdlExporter.java:216-264` - the export-time union-find over
  `WireNet`s; `:1146` `Group`; `:1161` `UnionFind`; `:98-107` the javadoc
  naming the third implementation.
- `test/jls/hdl/HdlPolicyTest.java:332` - the jump-fusion behavior that must not
  change.
- `CONTRIBUTING.md` - `@NullMarked` ratchet for new packages, SpotBugs
  threshold, the no-blanket-exclusions rule.
