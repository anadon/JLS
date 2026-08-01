# TASK-0064 - Zero-delay closure

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0063

## Deliverable

Events that model no elapsed time stop going on the queue. **No per-element
propagation delay changes and no golden changes** - that is the entire
acceptance criterion, and it is checkable.

1. **The closure.** A pass in `jls.sim` that, given a value change on a net,
   evaluates the cone of **zero-delay** elements reachable from it as one
   straight-line sweep and posts only the genuinely *timed* successors to the
   queue. The zero-delay set is enumerated normatively, not guessed:
   `Splitter` and `Binder` (`docs/simulation-semantics.md` §10, "with zero
   delay"), `Extend` (§10, "its gate `Kind` default delay is 0"), `InputPin`,
   `OutputPin`, `SubCircuit` and `Constant` (§6.2). The pass **must** derive the
   set from each element's declared delay rather than from a hard-coded class
   list, or a user who sets a `DelayGate` to 0 gets different semantics from a
   `Splitter`.
2. **Levelization of the cone, with a cycle refusal.** A zero-delay cycle is a
   combinational loop; today it manifests as an unbounded same-timestamp event
   storm. The closure detects it and reports it as a named diagnostic naming the
   elements in the cycle, which is a **user-visible improvement** and must
   therefore be specified in `docs/simulation-semantics.md` in the same commit.
3. **Read-latest preserved by construction.** §6.1's rule - inputs are
   overwritten eagerly and an element's `react` always reads the latest
   same-time values of all its inputs - is what makes the collapse sound: the
   sweep evaluates each cone element **once**, after all its same-time inputs
   have settled, which is exactly what the coalesced `PinChanged` notification
   achieves today. State that equivalence in the doc; do not leave it implied.
4. **The `seq` conservation argument, written down.** `SimEvent.seq` comes from
   a monotonic counter assigned at construction (`SimEvent.java:87, 116, 119`),
   and same-time events fire in `seq` order. Eliding a posting therefore changes
   the seq values of every later event in the run. The closure is only sound if
   the *relative* order of the events that survive is unchanged. This is the
   proof obligation of the task and it belongs in the class javadoc with the
   test that discharges it.

Done means: the profile shows a large drop in queue traffic, the golden corpus
is byte-identical, and a reviewer can read why eliding events cannot reorder the
ones that remain.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-030 | The third leg of the semantics-preserving stack, and the best-evidenced one: 82.3% of all events on the RV32I census are same-timestamp `PinChanged` notifications that model no time. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0063 | The closure's "already evaluated in this cone at this timestamp" marker **is** the intrusive queued flag TASK-0063 introduces. Built against HEAD's `HashSet dupCheck` instead, the closure would have to hash a `SimEvent` per cone node - which is the cost it exists to remove - and would then be rewritten when the set goes away. |

## Acceptance test

- **`jls.EngineByteIdentityGateTest.everyGoldenIsByteIdenticalAcrossTheZeroDelayClosure()`**
  (extend the class TASK-0063 adds, or TASK-0026's gate if it landed first):
  the whole committed golden corpus - `BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, `ElementSimulationGoldenTest`, `VcdExportGoldenTest`,
  `RiscvCpuGoldenTest`, `ShiftRegisterTest` - byte-identical. This is not a
  supporting test; it **is** the acceptance criterion.
- **`jls.sim.ZeroDelayClosureTest.timedSuccessorsRetireInTheIdenticalRelativeOrder()`**
  (new): run a fixture through the closure and through the unmodified loop,
  capture only the events that *fire* (not the elided ones), and assert the two
  sequences are equal as `(time, callBack, todo)` triples in order. This is the
  seq-conservation proof made executable.
- **`jls.sim.ZeroDelayClosureTest.aZeroDelayCycleIsReportedNotStormed()`** (new):
  a combinational-loop fixture; assert the named diagnostic and assert the run
  terminates, and assert it names every element in the cycle.
- **`jls.sim.ZeroDelayClosureTest.aDelayGateSetToZeroCollapsesLikeASplitter()`**
  (new): the anti-hard-coded-list assertion. A `DelayGate` with delay 0 must be
  in the cone; a `Splitter` is not special.
- **`jls.SimulationSemanticsRegressionTest`** (existing, all methods named in
  `src/jls/sim/Simulator.java:68-73`): green unchanged, especially
  `initInputsReachesInsideSubcircuits` (the closure crosses `SubCircuit`
  boundaries, which is where it is most likely to be wrong) and
  `constantValueIsMaskedToTheNetWidth`.
- **`jls.SimulationSeedOrderTest.initSimIsSeededInStableIdOrder`** (existing,
  issue #181, named at `docs/simulation-semantics.md` §12): green unchanged. The
  time-0 seed is the one place posting order is content-determined rather than
  emergent, and the closure must not touch it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 221 | Decision: simulation execution strategy - discrete-event vs a levelized/compiled evaluation pass | informs, **closed** (completed 2026-07-27). This is option 3, the hybrid, in its narrowest form: the event queue remains the sole authority on time and the compiled pass owns only the zero-delay cone. Because no per-element delay changes, it does **not** engage the issue's "observably identical or specify the divergence" clause. Do not cite it as open. |
| 232 | Simulation hot path: per-signal `BitSet` allocation ... value-typed signal representation | overlaps (open) - the same profile, a different leg. Independent. |

**No issue** exists for the zero-delay closure. Recorded as a gap; the registry
notes that #232 covers only the value representation, not the queue, the
closure, the fidelity boundary or the parity harness.

## Notes

- **Trap: the normative doc's §3 is already stale about payloads.**
  `docs/simulation-semantics.md:70-79` still describes `todo == null` as
  "your inputs changed" and `TriState.react` using the string `"off"`. At HEAD
  these are the sealed records `SimEvent.PinChanged` and `SimEvent.TriStateOff`
  (`src/jls/sim/SimEvent.java:23-84`, issue #95). Any paragraph this task adds
  sits next to a stale one; fix the neighbor while there (the corpus assigns the
  general doc-correction sweep to TASK-0024, but leaving a contradiction inside
  the section you are editing is worse than a wider edit).
- **Trap: `SubCircuit` is a zero-delay element that crosses a boundary.**
  `SubCircuit.react` (`src/jls/elem/SubCircuit.java:621-636`) posts a
  `NewValue`/`TriStateOff` **at `now`** to each mapped `InputPin`, and
  `SubCircuit.send` (`:646-652`) is a plain `Output.propagate`. So the cone runs
  straight through instance boundaries in both directions. Any per-instance
  fidelity choice (TASK-0065) or checkpoint boundary must therefore be evaluated
  *outside* the cone, not inside it. Say so, because the two programs meet here.
- **Trap: `Memory` reads are asynchronous and are NOT zero-delay.**
  `Memory.react` posts a `MemoryRead` at `now + accessTime` on an address change
  (`src/jls/elem/Memory.java:1384, 1396, 1402`, all three
  `new SimEvent(now+accessTime, this, ...)`). Memory looks like wiring in a datapath drawing
  and is not. Same for `Adder`, whose delay is
  `propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`) - the
  surviving trace of ripple-carry in a word-level simulator.
- **Trap: the cone must not swallow `Stop` or `Pause`.**
  `src/jls/elem/Stop.java` and `Pause.java` reach the simulator from inside a
  `react` (`docs/simulation-semantics.md` §11). If they end up inside a
  straight-line sweep, a stop can be observed at the wrong point in the sweep.
  Evaluate them as timed successors or handle them explicitly.
- **Where the payoff comes from, with the assumptions stated.** The CPU census
  is 34 `Splitter`s, 9 `Binder`s, 43 `Constant`s and 5 `Extend`s of 225 logic
  elements - a third of the design is pure wiring and all of it is on the
  priority queue today. The closure preserves every per-element propagation
  delay **by construction**, because zero-delay elements have no delay to
  preserve. That is why this is the best-evidenced item in the engine set.
- **This is Mode T, not Mode C.** No flag, no user-visible switch, no
  turning `now` into a cycle counter. A whole-run levelized replacement is a
  different, later, gated thing.

## Evidence

- `docs/simulation-semantics.md` §10 (`Splitter`, `Binder`, `Extend` are zero
  delay), §6.1-§6.2 (eager input overwrite, read-latest, the delay discipline,
  zero-delay elements), §3 (FIFO within a timestamp, duplicate coalescing), §11
  (`Stop`/`Pause` reach the simulator), §12 (the golden-to-section map this task
  must leave intact).
- `src/jls/sim/SimEvent.java:87, 116, 119` - the monotonic `seq` counter that
  makes the conservation argument necessary.
- `src/jls/sim/Simulator.java:215-243` - the loop the closure sits inside;
  `:239` - the single `react` dispatch it must keep as the only entry.
- `src/jls/elem/SubCircuit.java:621-636, 646-652` - the zero-delay boundary
  crossing.
- `src/jls/elem/WireNet.java:487-510` - `propagate`'s sink loop: eager
  `inp.setValue(newValue)` then one `PinChanged` post per attached input. This is
  the exact code the closure replaces for zero-delay sinks.
- BRIEF §13 and `keystone-c-performance.md` census: `PinChanged` is 1,919,891 of
  2,331,793 events (**82.3%**); all `react()` bodies together are 4.9% of loop
  time; a levelized pass over the CPU's real shape measured 4.32 ns/node with
  plane arrays and 22.01 ns/node with `BitSet[]`. **Any figure derived from
  ns/node must state node count and pass count** - a ~580-element machine is
  ~1,345 nodes and a levelized design needs two passes per cycle.
- `04-mechanisms.md` §0.4 and conflict C5 - the ruling that Mode T ships first
  and that its acceptance criterion is literally "no golden changed".
