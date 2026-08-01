# TASK-0057 - The resolution fold

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0056

## Deliverable

Replace the first-active-driver-in-net-order scan in `WireNet.propagate`
(`src/jls/elem/WireNet.java:443-529`, the `if (triState)` block at `:454-485`)
with an order-independent per-bit fold over a cached driver list.

1. **A cached driver list.** `WireNet` gains
   `private List<Output> drivers` populated in `makeNet`
   (`WireNet.java:97-165`, the attachment walk at `:132-147`) and in `recheck`
   (`WireNet.java:272-302`, the same walk at `:277-288`), in the net's existing
   `LinkedHashSet` insertion order. Both methods already visit exactly the ends
   the fold needs; neither gains a second traversal.
2. **The fold.** A new `jls.core.Resolution` with
   `static LogicVector resolve(List<LogicVector> driven, int width)`, defined as
   a per-bit binary operator applied by `reduce`. It must be **commutative,
   associative and idempotent** - proven by test, not asserted - so the answer
   is independent of the order `drivers` happens to be in. Two-state mode keeps
   today's answer (first active driver wins, one warning); four-state mode
   resolves disagreeing drivers to `X` and agreeing drivers to their common
   value, with `Z` as the fold identity.
3. **`TriState` drives `Z` per bit.** `src/jls/elem/TriState.java:473-520`
   currently posts a `TriStateOff` payload for the whole port
   (`SimEvent.java:47`) and a `NewValue` otherwise. `TriStateOff` becomes a
   deprecated alias for `NewValue(LogicVector.allZ(width))`; the sealed
   `SimEvent.Payload` arm stays for one epoch so no `react` switch loses its
   exhaustiveness.
4. **The warning survives, relocated.** The bus-conflict message at
   `WireNet.java:477-482` moves inside the fold and is raised when the fold
   produces `X` from two non-`Z` disagreeing drivers, still once per net until
   the conflict clears (`conflictReported`, `WireNet.java:407`). Its text
   changes only where it says "the first active driver in net order wins",
   which is no longer true.

Done means: no code path selects a driver by position; a reviewer can reorder
`ends` in a fixture and get the same simulated values.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-026 | Multi-driver resolution stops being a positional accident; X is producible, which is the second half of the four-state core. |
| FEAT-027 | Strength is a second dimension on the same fold. TASK-0058 extends this operator; without it there is nothing to extend. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0056 | The fold folds `LogicVector`s carrying per-bit `Z`. At HEAD the resolution operand is `@Nullable BitSet` where `null` is whole-port HiZ (`WireNet.java:405, 443`), which cannot express "bit 3 is undriven, bit 4 is driven 1" and therefore cannot be folded per bit at all. |

## Acceptance test

- **`jls.sim.ResolutionFoldTest.foldIsOrderIndependentOverEveryDriverPermutation()`**
  (new): for a net with 3 and 4 drivers over the cross product of driven values
  `{0, 1, Z, X}`, assert `resolve` returns the same `LogicVector` for **all**
  permutations of the driver list. Fails today by construction.
- **`jls.sim.ResolutionFoldTest.foldIsIdempotentAndHasZAsIdentity()`** (new):
  `resolve([v]) == v`, `resolve([v, v]) == v`, `resolve([v, Z]) == v`.
- **`jls.SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`**
  (existing, named in `src/jls/sim/Simulator.java:71`): keep green in two-state
  mode; add a four-state sibling
  `multiDriverConflictResolvesToXAndWarnsOnce()` asserting the resolved value is
  `X` on the disagreeing bits and unchanged elsewhere.
- **`jls.SimulationSemanticsRegressionTest.agreeingTriStateDriversDoNotWarn`**
  (existing, `Simulator.java:68`): must stay green unchanged - two drivers
  agreeing is not a conflict under the fold either.
- **`jls.VcdExportGoldenTest.testVectorStimulusVcdMatchesGoldenAndCoversHiZ`**
  (existing, `Simulator.java:75`): byte-identical.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `BitSet` allocation ... value-typed (long,width) signal representation | overlaps - its §10 threat list names deterministic multi-driver resolution as a contract this work must preserve |
| 98 | (recorded decision, **closed**) surprise S1: multi-driver resolution order | informs - S1 is the surprise this task retires; cited in `WireNet.java:20, 453` and in `docs/simulation-semantics.md` §9 |

**No issue** exists for the resolution fold itself. The gap is recorded, not an
objection (D10).

## Notes

- **Trap: `docs/simulation-semantics.md` §9 is normative and says the opposite.**
  It specifies the deterministic first-driver winner and "no conflict (X) state",
  and its §12 validation table binds that to
  `SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`.
  Recorded decision #221 (closed) requires any observable divergence to be "a
  specified, documented change to `docs/simulation-semantics.md` first". The doc
  paragraph is part of this task's diff, not a follow-up.
- **Trap: `Splitter` and `Binder` have all-or-nothing HiZ special cases.**
  `docs/simulation-semantics.md` §10: a HiZ input makes every `Splitter` output
  HiZ, and a `Binder` contributes zeros for HiZ inputs unless all are HiZ. Both
  cases exist only because HiZ was whole-port. Per-bit `Z` deletes them; deleting
  them changes `Binder`'s output for a partially-HiZ input from 0 to `Z`, which
  **is** a golden change. Keep both special cases behind the two-state mode gate
  in this task and retire them with the mode flip.
- **Trap: `makeNet` overwrites rather than accumulates.** `WireNet.java:139` is
  `net.bits = put.getBits();` inside the loop - the *last* attached put wins,
  where `recheck:280` takes a `Math.max`. The driver list must be built by
  accumulation in both, and the discrepancy noticed here should be recorded
  (it is a latent width bug, not this task's to fix).
- **Trap: the `conflictReported` latch is per-net and never reset on reload.**
  `WireNet.java:407` is an instance field; nets are rebuilt on load, so this is
  benign today. Under a cached driver list the net object outlives more edits -
  re-verify.
- **Where the cost goes.** The fold itself is small. Two weeks is the doc
  paragraph, the mode gate, the `TriStateOff` deprecation across the **18 files in
  `src/jls/elem` that name it** (measured: `grep -rln TriStateOff src/jls/elem`
  returns 18, most of them exhaustive `switch` arms with no default that stop
  compiling the moment the sealed payload set moves), and keeping every
  two-state golden byte-identical while doing it.

## Evidence

- `src/jls/elem/WireNet.java:443-529` - `propagate`; the resolution block at
  `:454-485`; the comment at `:445-453` stating first-active-driver-in-net-order
  as the rule and citing issue #98 S1.
- `src/jls/elem/WireNet.java:19-22` - insertion order is *deliberately* the
  resolution order today: "insertion order (file order for a loaded circuit)
  makes the multi-driver resolution in propagate deterministic".
- `src/jls/elem/WireNet.java:132-147` (`makeNet` attachment walk), `:277-288`
  (`recheck` walk) - the two places the driver list is cheap to build.
- `src/jls/elem/TriState.java:487-505` - the whole-port off/on decision.
- `src/jls/sim/SimEvent.java:47` - the `TriStateOff` payload record.
- `docs/simulation-semantics.md` §9 and §12 - the normative statement and its
  test binding.
- `docs/capability-roadmap/keystone-b-migration.md:539` - stage 4 prices this at
  2 weeks and specifies the driver list cached at `makeNet`/`recheck`.
