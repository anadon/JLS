# TASK-0056 - Widen the value permits and migrate the value representation

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

Two things ship together, in `jls.core` and the value plumbing that reads it.

1. **The type.** New `jls.core.LogicVector`: a sealed interface permitting
   `Binary` (one plane, bit-identical to today's `BitSet` path) and `FourState`
   (aval/bval plane pair, the IEEE 1364 `s_vpi_vecval` encoding). Every instance
   carries its own `int width`; every instance is immutable; the op set is
   `and/or/xor/not`, `slice`, `concat`, `signExtend`, `zeroExtend`, `resolve`,
   `toLong`, `toBigInteger`, radix formatting, `equals`, `hashCode`.
   `docs/capability-roadmap/keystone-b-migration.md:535` is the specification of
   this step and is not restated here. The record's field list is **frozen in
   writing** in the class javadoc, with the measured reason: a tagged union with
   a fourth plane costs 9.24 vs 7.01 ns/op (+32%) and +16 bytes per value in a
   *pure binary* circuit (`07-mvl-determination.md` §3.2, corollary 2).
2. **The accessors, reserved at radix 2.** `Put.getRadix()` and
   `Put.getDigits()`, and the same pair on `WireNet`, both returning 2 and
   `getBits()` respectively, with no caller able to produce any other value.
   Nothing validates radix in this task - TASK-0059 does. The accessors exist
   here so that TASK-0059 edits call sites rather than inventing them.
3. **The plumbing widened, and the defensive clones deleted.** `Put.currentValue`,
   `Input`/`Output` `setValue`/`getValue`, `Output.propagate`
   (`src/jls/elem/Output.java:136-169`), `WireNet.setValue`/`getValue`/`propagate`
   (`src/jls/elem/WireNet.java:415-434, 443-529`), `SimEvent.NewValue`,
   `SimEvent.MemoryWrite`, `SimEvent.TableOutput`
   (`src/jls/sim/SimEvent.java:39, 65, 83`) and `jls.sim.TraceSample.value`
   (`src/jls/sim/TraceSample.java:19`) all take `LogicVector`. `null` stops
   meaning HiZ and becomes `LogicVector.allZ(width)`. Because the value is
   immutable, the **59 `clone()` calls in `src/jls/elem` and 2 in `src/jls/sim`**
   (measured at HEAD by `grep -rn 'clone()'`) that exist only to defend against
   shared mutable `BitSet`s are deleted, not ported. Every element keeps
   computing in `Binary` via an explicit `coerceUndrivenToZero()` call, so no
   `react` body changes its arithmetic in this task.

Done means: `mvn verify` green; `jls.core` still AWT-free under
`ArchitectureRulesTest` and `HeadlessCoreRatchetTest`; every golden byte-identical.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-026 | The value that can hold X and Z at all. Nothing else in the four-state core is expressible until the currency changes. |
| FEAT-028 | Radix 3 and 4 ride this record's spare code points with zero new fields; the reserved accessors are the port-radix seam. |
| FEAT-030 | Removes the 37.6% `BitSet` share of loop time and the per-value heap object; the width-carrying immutable value is one of the three legs of the semantics-preserving stack. |

## Prerequisite tasks

None. This is the first code in the value program and touches only types it
introduces plus the plumbing that already exists at HEAD.

## Acceptance test

- **`jls.core.LogicVectorTest`** (new): ~25 cases over the op set; asserts
  `Binary.and/or/xor/not` agree bit-for-bit with `BitSet` on random operands at
  widths 1, 8, 32, 64, 96; asserts immutability by mutating the operand's
  backing array through a slice and re-reading; asserts `equals`/`hashCode` are
  width-sensitive (`Binary(4, 0b0001)` != `Binary(8, 0b0001)`).
- **`jls.ValueMigrationGoldenTest.everyShippedGoldenIsByteIdenticalAfterTheValueSwap()`**
  (new): runs the whole existing golden corpus - `BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, `ElementSimulationGoldenTest`, `VcdExportGoldenTest`,
  `RiscvCpuGoldenTest`, `ShiftRegisterTest` - and asserts each produced artifact
  is byte-identical to the committed golden. This is the "radix 2 provably
  byte-identical" clause and it is an equality assertion, not a tolerance.
- **`jls.elem.WireValueChannelTest`** (existing, `test/jls/elem/`): extend with
  `undrivenIsAllZNotNull()`, asserting the HiZ channel is a `LogicVector` state
  rather than a `null` reference.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | closes (this is its §7 method and its §12 definition of done) |
| 231 | `SimEvent.hashCode()` == `(int)time` makes the dupCheck HashSet O(n2) | informs, **closed** - do not cite as open; its threat-model section names the `equals`/`hashCode` contract this task must not break |
| 221 | Decision: simulation execution strategy | informs, **closed** as a recorded decision; its equivalence criterion is what the golden test above discharges |

No issue exists for the port-radix accessors or for the four-state semantics -
recorded as a gap, not a blocker (D10).

## Notes

- **Trap: `null` is load-bearing in 29 places.** `sweep-01-values-and-logic.md`
  counts 29 explicit `if (value == null) value = new BitSet()` coercions across
  17 element classes. Each is a decision site. This task does **not** change any
  of their outcomes; it renames the mechanism. Changing an outcome here would
  move a golden, which the acceptance test forbids.
- **Trap: the trace marker BitSet.** `TraceSample`'s javadoc
  (`src/jls/sim/TraceSample.java:7-17`) encodes HiZ as a value of width
  `bits + 1` with the top bit set, and `BatchSimulator.vcdValue`
  (`src/jls/sim/BatchSimulator.java:538-555`) reconstructs that marker with
  `new BitSet(bits + 1); off.set(bits)` to test for it. Widen `TraceSample` but
  keep the marker's *observable output* (`z`, `bz`) identical; retiring the
  marker is TASK-0057's and keystone-b stage 3's work, not this task's.
- **Trap: `Adder` reads the carry as the bit at index `bits`.**
  `src/jls/elem/Adder.java:418-421` does `carry.set(0, sum.get(bits)); sum.clear(bits)`
  on a `BitSet` one wider than the port. A width-carrying value makes that an
  out-of-range read unless `LogicVector` slice semantics permit reading position
  `width`. Decide explicitly: either `SumCarry` returns a `width + 1` vector
  (recommended - it is what the code means) or the carry becomes a second return.
- **Trap: `SimEvent.equals` compares `todo` structurally.**
  `src/jls/sim/SimEvent.java:171` is `this.todo.equals(oth.todo)`, and
  `NewValue` is a record over the value. A `LogicVector` whose `equals` is not
  width-sensitive would silently change dedup coalescing and therefore the event
  count. `SimEventDedupTest` and `SimEventContractTest` (`test/jls/sim/`) are the
  guards; extend both rather than trusting them.
- **`PinChanged` is a zero-field record allocated 1.92 M times per run**
  (BRIEF §13, JFR-measured). It is not this task's target, but a reviewer will
  ask: it is TASK-0063's, via the intrusive queued flag.
- **Cost honesty.** `keystone-b-migration.md:535-536` prices "the type" at 2 wk
  and "widen the plumbing" at 2.5 wk, total 4.5. The registry band is 2 wk. Both
  numbers are recorded; the plan should reconcile them rather than the author
  choosing silently.

## Evidence

- Defensive clone census, measured at HEAD: 59 in `src/jls/elem`, 2 in
  `src/jls/sim`, 77 across `src/jls` (`grep -rn 'clone()' src/jls --include=*.java`).
- `src/jls/elem/Output.java:136-169` - `propagate` clones on every non-equal
  value and short-circuits on `currentValue.equals(value)` at `:143`.
- `src/jls/elem/WireNet.java:415-421, 428-434, 496-498, 513-516` - four more
  clones on the propagate path alone.
- `src/jls/elem/InputPin.java:185, 210-211` - clones twice per event.
- `src/jls/elem/Register.java:709, 724, 735, 767-805` - eleven clones.
- `src/jls/sim/SimEvent.java:39, 65, 83` - the three `BitSet`-carrying payloads.
- `docs/capability-roadmap/keystone-b-migration.md:535-536` - stages 1 and 2,
  the owning program's own decomposition.
- BRIEF §13: `BitSet` is 37.6% of warm loop time; 318 ns/event, 3.14 M events/s.
- `pom.xml:429, 452, 475, 498` - the only four package floors at HEAD are `jls`,
  `jls.sim` (93.0/92.0/84.5), `jls.elem` (73.0/70.0/58.5) and `jls.collab.op`.
  **`jls.core` has no floor**, so new value-type code lands unfloored unless this
  task adds a rule; adding one at the measured value is part of the deliverable.
