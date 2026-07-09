# JLS Simulation Semantics

**Status: normative.** This document specifies what a JLS simulation
*means*: the time model, the event queue and its ordering, the value
domain, initialization, propagation, per-element delays, edge
triggering, and tri-state resolution. It is written from the
implementation at HEAD, and every claim carries a code anchor
(`file` / method) or a golden-test anchor. If code and document
disagree, one of them has a bug; the golden tests
(`test/jls/BatchSimulationGoldenTest.java`,
`test/jls/SequentialGoldenTest.java`,
`test/jls/VcdExportGoldenTest.java`) pin the load-bearing parts.

Behavior found while writing this spec that nobody would specify
deliberately is **not** documented as intended: it is listed in the
[appendix](#appendix-surprises-found-while-writing-this-spec) as
candidate bugs, per issue #85 §9. The sibling document
[`batch-interface.md`](batch-interface.md) specifies the batch
*interface* (test-vector grammar, stdout format, VCD profile); this
one specifies the simulation *model* both simulators share.

## 1. Time model

- Simulation time is a dimensionless non-negative 64-bit integer
  (`long now`, `src/jls/sim/Simulator.java`). Time units are abstract;
  nothing binds them to seconds (the VCD exporter's nominal
  `1 ns` timescale is a tool-compatibility mapping only, see
  `batch-interface.md` §4.2).
- Every run starts at time 0 (`Simulator.initSimulation`).
- Time advances only by dequeuing events: `now` is set to the time of
  the event about to react (`Simulator.runEventLoop`). Between events
  nothing happens; there is no implicit clock.
- A run has a time limit, default `JLSInfo.defaultTimeLimit`
  (100,000,000; `src/jls/JLSInfo.java`), settable per run
  (`Simulator.setTimeLimit`; `-d` in batch mode). Events scheduled
  *at* the limit still execute (the loop runs while `now <= maxTime`);
  the first event *after* the limit terminates the run and clamps
  `now = maxTime` (`Simulator.runEventLoop`).

## 2. Value domain: two states plus HiZ

- A signal value is a `java.util.BitSet` in which bit *i* holds the
  2^*i* place of the unsigned binary value
  (`BitSetUtils.Create`, `src/jls/BitSetUtils.java`). Bits are
  two-state: 0 or 1. There is no unknown/X state anywhere in the
  simulator (pinned by
  `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`).
- High impedance (an undriven tri-state net) is represented by a
  **null** value reference, not by a special BitSet
  (`TriState.initSim` sets its output to null,
  `src/jls/elem/TriState.java`; `WireNet.propagate` passes null
  through, `src/jls/elem/WireNet.java`).
- HiZ is all-or-nothing per signal: a value is either fully driven or
  fully HiZ. There is no per-bit HiZ (see `batch-interface.md` §4.3).
- Bit *width* is a property of elements and wire nets
  (`WireNet.getBits`), not of the BitSet, which is unbounded. Reading
  code interprets a value at the reader's declared width.
- Nearly every element's `react` treats a null (HiZ) input as zero
  before computing (e.g. `Gate.computeOutput`'s contract,
  `src/jls/elem/Gate.java`; `Register.react`,
  `src/jls/elem/Register.java`; `Adder.react`,
  `src/jls/elem/Adder.java`). HiZ is therefore observable only at
  watched/displayed points (trace, VCD, stdout `HiZ`), not as a
  distinct input state inside combinational logic.

## 3. Events and ordering

An event (`src/jls/sim/SimEvent.java`) is a triple:

- **time** — when it fires;
- **callBack** — the element (`jls.sim.Reacts`) whose `react` runs;
- **todo** — a payload. By convention `todo == null` means "your
  inputs changed, re-read them"; a non-null todo is element-specific,
  most commonly the output value the element scheduled for itself
  after its propagation delay (`Gate.react`), and occasionally a
  sentinel (`TriState.react` uses the string `"off"` for
  turn-off events).

Ordering (`SimEvent.compareTo`): events fire in ascending **time**;
events at the same time fire in ascending **sequence number**, a
global counter assigned at construction. Since events are constructed
at their `post` site, same-time events fire in the order they were
posted — FIFO within a timestamp. Ordering is fully deterministic.

**Duplicate suppression** (`Simulator.post` +
`SimEvent.equals`/`hashCode`): posting an event equal in
(time, callBack, todo) to an event still *pending* in the queue is a
no-op. The pending set is maintained in `dupCheck`; an event is
removed from it when polled, so re-posting an identical event after
the original has fired is allowed. For `todo == null` events this
coalesces multiple same-time "inputs changed" notifications to one
`react` call per element per timestamp.

## 4. The event loop and termination

`Simulator.runEventLoop` is shared by both simulators (issue #25):

```
while not stopping, queue non-empty, and now <= maxTime:
    poll the earliest event; now = event.time
    if now > maxTime: now = maxTime; stop
    event.callBack.react(now, sim, event.todo)
```

Mode-specific behavior (pausing, stepping, tracing, display) lives in
the `beforeEvent`/`beforeReact`/`afterEvent` hooks, not in the loop.

A run therefore ends for exactly one of three reasons:

1. **stopped** — `stopping` was set (the Stop button, or a `Stop`
   element calling `Simulator.stop`);
2. **time limit** — the next event lies beyond `maxTime`;
3. **no more activity** — the event queue drained.

In batch mode these surface as the stdout outcome line
(`BatchSimulator.displayOutcome`); the exact strings and their
precedence are specified in `batch-interface.md` §3.1 — this document
does not duplicate them.

## 5. Initialization

`Simulator.initSimulation` (`src/jls/sim/Simulator.java`) runs before
every simulation:

1. `now = 0`; the event queue and duplicate-suppression set are
   cleared; `stopping` is reset.
2. `initInputs`: every input point of every top-level `LogicElement`
   is set to the value 0 (`LogicElement.initInputs`,
   `src/jls/elem/LogicElement.java`). This walk is *not* recursive
   into subcircuits; see appendix S3.
3. `initSim(sim)` on every top-level `LogicElement`.
   `SubCircuit.initSim` recurses into its inner circuit
   (`src/jls/elem/SubCircuit.java`), so every element at every depth
   is initialized.

The `initSim` conventions:

- Outputs start at 0 (`Output.setValue(new BitSet())`), or **null**
  when the output is tri-state (`TriState.initSim`,
  `SubCircuit.initSim`, `Splitter.initSim`).
- An element whose correct time-0 output is *not* 0 posts a time-0
  event to drive it: gates whose all-zero-inputs output is 1 (NAND,
  NOR, NOT — `Gate.initSim`), `Constant` (its configured value,
  `Constant.initSim`), `Register` (its configured initial value,
  `Register.initSim`), `Clock` (its first transition,
  `Clock.initSim`). Values therefore settle through ordinary
  event-driven propagation starting at time 0; there is no separate
  "settling" phase and time-0 events are observable in traces.
- Test vectors (`-t`) and signal generators post their entire
  schedules during `initSim`
  (`SigSim.initSim`, `src/jls/elem/SigSim.java`; grammar and timing
  in `batch-interface.md` §2).

Pinned by: `SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge`
(a register with a never-ticking clock reads back its `init` value)
and `BatchSimulationGoldenTest.notGateInverts` (a NOT gate over a
constant-0 input settles at 1, which requires the time-0 drive).

## 6. Propagation

### 6.1 Outputs and wires

`Output.propagate(value, now, sim)` (`src/jls/elem/Output.java`):

- No-op if the value equals the output's current value (change
  detection at the source).
- Otherwise the output stores the value and hands it to its wire
  net's `WireNet.propagate`.

`WireNet.propagate` (`src/jls/elem/WireNet.java`):

- **Wires are ideal**: propagation across a net takes zero simulation
  time and the whole net carries one value. There is no per-wire or
  per-segment delay.
- For every `Input` attached to the net, the input's value is
  **overwritten immediately** (synchronously, inside the driving
  element's `react`), and a `todo == null` notification event is
  posted **at the current time** for the input's element.
- On a tri-state net the value actually driven is resolved first;
  see section 9.

Consequence: all delay in JLS lives in elements, none in wiring. A
receiving element reacts at the same timestamp its input changed, and
because notifications are coalesced (section 3) and inputs are
mutated eagerly, an element's `react` always reads the **latest**
same-time values of all its inputs, regardless of how many of them
changed. Same-time races (e.g. a clock edge and a data change at the
identical timestamp) are resolved by this read-latest rule plus FIFO
event order — deterministically, but with no setup/hold modeling.

### 6.2 Element delay discipline

Delayed elements follow one pattern (`Gate.react` is the archetype):

- On `todo == null` ("inputs changed"): recompute the output value
  from current inputs. If it differs from the value already
  propagating through the element (`toBeValue`), post
  `SimEvent(now + propDelay, this, newValue)` and record it as
  `toBeValue`.
- On `todo != null`: propagate the payload to the output(s).

This is **transport delay**: a pulse narrower than the propagation
delay is not swallowed — both scheduled transitions fire, shifted by
the delay. (There is no inertial-delay glitch suppression; the only
suppression is the equal-pending-event rule of section 3 and the
`toBeValue` change check.)

Zero-delay elements (`Splitter.react`, `Binder.react`,
`InputPin.react`, `OutputPin.react`, `SubCircuit.react`,
`Constant.react`) propagate within the same timestamp, so an
arbitrarily deep chain of wiring elements adds zero time.

## 7. Propagation delays per element

Defaults, from the code (all user-adjustable per instance via the
element's "delay" attribute where one exists; "Global → Reset
Propagation Delays" restores these defaults via
`LogicElement.resetPropDelay`):

| element | default delay | anchor |
|---|---|---|
| AND, OR, XOR gate | 10 | `Kind` in `src/jls/elem/AndGate.java`, `OrGate.java`, `XorGate.java` |
| NAND, NOR, NOT gate | 5 | `NandGate.java`, `NorGate.java`, `NotGate.java` |
| DELAY gate | user-specified at creation (dialog seeds 1); exempt from delay reset | `src/jls/elem/DelayGate.java` |
| Tri-state gate | 5 | `src/jls/elem/TriState.java` |
| Adder | 30 × bits (ripple-carry model, recomputed from width) | `src/jls/elem/Adder.java` |
| Decoder | 15 | `src/jls/elem/Decoder.java` |
| Mux | 25 | `src/jls/elem/Mux.java` |
| Register | 50 | `src/jls/elem/Register.java` |
| State machine | 30 | `src/jls/elem/StateMachine.java` |
| Truth table | 30 | `src/jls/elem/TruthTable.java` |
| Memory | 100 (access time) | `src/jls/elem/Memory.java` |
| Splitter, Binder, Extend, Constant, pins, jumps, wires, subcircuit boundary | 0 | section 6.2 |

Pinned by: `BatchSimulationGoldenTest.delayGatePassesValueThrough`
(delay gate is logically neutral) and every golden that asserts a
settled value within the default time limit.

## 8. Sequential semantics: edge triggering

### 8.1 Register (`src/jls/elem/Register.java`)

Three types (the save-file `type` attribute): transparent latch
(`latch`), positive-edge flip-flop (`pff`), negative-edge flip-flop
(`nff`). Two outputs: `Q` and `notQ` (bitwise complement of Q over
the declared width). Behavior, from `Register.react`:

- The register remembers the clock value it saw on its previous
  react (`currentC`). A **positive edge** is a react in which the
  remembered clock is 0 and the current clock input is 1; a
  **negative edge** is the reverse. Because reacts fire on *any*
  input change, a data-only change while the clock is steady is never
  an edge (the remembered clock equals the current one).
- **pff/nff**: on its edge, the register samples D as of that
  timestamp (the read-latest rule of §6.1) and schedules Q (and notQ)
  for `now + propDelay` — unless D equals the value already latched
  or in flight (`toBeValue`), in which case nothing is posted.
  Between edges, D changes are ignored.
- **latch**: while the clock input is non-zero the latch is
  transparent — any D change (and the clock's own rising change)
  schedules the new D after `propDelay`. While the clock is 0,
  D changes are ignored (the latch holds).
- **Initial value**: the `init` attribute is driven onto Q at time 0
  (via a time-0 event posted in `Register.initSim`) and held until
  the first capture. With no edge ever arriving, the register holds
  its initial value forever.

Pinned by `test/jls/SequentialGoldenTest.java`:
`positiveEdgeFlipFlopCapturesDataOnClockEdge` (capture on edge),
`flipFlopComplementOutputTracksQ` (notQ complement),
`registerInitialValueAppearsBeforeAnyClockEdge` (no edge ⇒ hold
init), `latchPassesDataWhileClockHigh` (transparency).

Worked derivation (how the first golden's value falls out of this
document): clock `cycle 20, one 10` is 0 on [0,10) and first rises at
t=10 (§8.3); the register (pff, delay 5) sees remembered-clock 0,
clock 1 at t=10 and samples D=5; Q becomes 5 at t=15 and propagates
through the zero-delay output pin at t=15; the run ends at the time
limit with `q = 5`, which is what the golden asserts.

### 8.2 State machine (`src/jls/elem/StateMachine.java`)

- Edge-triggered on its `clock` input; the `trig` attribute selects
  rising (1) or falling (otherwise) edges, detected with the same
  remembered-previous-clock scheme as the register
  (`StateMachine.react`).
- On its edge, the machine evaluates the current state's transitions
  against the current inputs (`State.getNextState`), then becomes
  **busy** and posts the state change for `now + propDelay`. While
  busy (a transition in flight), further edges are ignored — a clock
  faster than the propagation delay drops edges rather than queueing
  them.
- When the state-change event fires, the machine enters the new
  state, drives that state's output values (Moore outputs,
  `State.sendOutputs`), and clears busy.
- At time 0 the machine enters its initial state and drives that
  state's outputs (`StateMachine.initSim`); a save without a marked
  initial state falls back to an arbitrary state rather than crash
  (issue #52).
- If no transition matches, the machine freezes; see appendix S5.

Pinned by `SequentialGoldenTest.stateMachineDrivesStateOutput`.

### 8.3 Clock (`src/jls/elem/Clock.java`)

Parameters `cycle` (period) and `one` (time high per period). The
output starts at 0; `Clock.initSim` posts the first rising transition
at `cycle − one`, and `Clock.react` thereafter alternates: high for
`one`, low for `cycle − one`. The high phase therefore sits at the
*end* of each period: the output is 0 on [0, cycle−one), 1 on
[cycle−one, cycle), and so on. The first rising edge is at
`cycle − one`, not at 0 and not at `cycle`.

## 9. Tri-state and multi-driver resolution

- A wire net is tri-state iff at least one attached `Output` is
  tri-state (`WireNet.recheck`/`makeNet`,
  `src/jls/elem/WireNet.java`). Tri-state-ness propagates at *edit*
  time through pass-through elements (`TriProp` implementors —
  pins, splitter/binder — via `WireNet.setTriState`); it is a static
  property of the drawing, not a simulation-time state.
- A tri-state gate drives its data input when its 1-bit control input
  is non-zero, and drives **null** (HiZ) when the control is 0, both
  after its propagation delay (`TriState.react`). A memory's data
  output is likewise HiZ while not enabled (pinned by
  `VcdExportGoldenTest.testVectorStimulusVcdMatchesGoldenAndCoversHiZ`).
- **Resolution**: when any driver on a tri-state net changes,
  `WireNet.propagate` scans the net's attached outputs and delivers
  the first non-null driver value it finds; if every driver is off,
  the net is HiZ (null) (`WireNet.propagate`,
  `src/jls/elem/WireNet.java`). With **at most one** active driver —
  the only configuration with defined meaning — this implements
  standard bus behavior: the active driver wins, and turning the
  active driver off re-resolves to the other drivers or to HiZ.
  With two or more simultaneously active drivers the winner is
  arbitrary; see appendix S1. There is no wired-AND/OR and no
  conflict (X) state.
- Readers of an HiZ net see null and, per section 2, almost all
  compute with it as zero. The distinct visible renderings of HiZ
  (`HiZ` on stdout, `z` in VCD, mid-level trace line) are specified
  in `batch-interface.md` §3.4/§4.3.

## 10. Bundles: splitter, binder, extend

- A **Binder** (`src/jls/elem/Binder.java`, palette "BIND") composes
  its output from configured input ranges with zero delay. If *every*
  input is HiZ the output is HiZ; if only some are, the HiZ inputs
  contribute zeros (per-signal HiZ, section 2).
- A **Splitter** (`src/jls/elem/Splitter.java`, palette "SPLIT")
  extracts configured bit ranges into its outputs with zero delay; a
  HiZ input makes every output HiZ (`Splitter.react`).
- An **Extend** (`src/jls/elem/Extend.java`, palette "1-to-N")
  replicates its 1-bit input; zero delay (its gate `Kind` default
  delay is 0).

## 11. Simulation-control elements

- **Stop** (`src/jls/elem/Stop.java`): when its attached input
  changes to a non-zero value, calls `Simulator.stop()` — the run
  terminates with the "stopped" reason (section 4).
- **Pause** (`src/jls/elem/Pause.java`): when its attached input
  changes to a non-zero value, calls `Simulator.pause(true)`. In the
  interactive simulator this pauses the run (resumable); in batch
  mode pause is meaningless and is treated as stop
  (`BatchSimulator.pause`). Note the on-non-zero condition
  contradicts this element's help page; see appendix S4.

## 12. Validation against the goldens

Per issue #85 H1, the golden expectations must be re-derivable from
this document alone. The mapping:

| spec section | pinned by |
|---|---|
| §2 value domain, HiZ | `VcdExportGoldenTest` (0/1/z, never x; `bz` for HiZ) |
| §5 initialization | `SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge`; `BatchSimulationGoldenTest.notGateInverts` |
| §6–7 delays settle within limit | every `BatchSimulationGoldenTest` truth-table golden |
| §8.1 register | all four register goldens in `SequentialGoldenTest` (worked derivation in §8.1) |
| §8.2 state machine | `SequentialGoldenTest.stateMachineDrivesStateOutput` |
| §8.3 clock phase | the flip-flop goldens (first edge must exist before the 10-cycle limit) |
| §9 HiZ resolution | `VcdExportGoldenTest.testVectorStimulusVcdMatchesGoldenAndCoversHiZ` |
| §4 termination reasons | `CliSmokeTest` / `batch-interface.md` §3.1 |

## Appendix: Surprises found while writing this spec

Candidate bugs, per issue #85 §9: behavior the code exhibits that no
one would specify deliberately. Listed here instead of being
documented above as intended; none of the simulation code was changed
in this slice. Each is worth its own issue.

- **S1 — Multi-driver tri-state conflicts resolve arbitrarily.**
  `WireNet.propagate` (`src/jls/elem/WireNet.java`) takes the first
  non-null driver produced by iterating a `HashSet` of wire ends.
  With two simultaneously active drivers the delivered value depends
  on hash iteration order — stable within a run, but unspecified and
  not meaningful. A bus conflict should be reported (or at least
  resolved by a defined rule), not silently won by an arbitrary
  driver.
- **S2 — `Register.initSim` shadows its `currentValue` field.**
  `Register.initSim` (`src/jls/elem/Register.java`) declares a
  *local* `BitSet currentValue`, so the field keeps its stale value
  until the register's time-0 event reacts; `findWatched`
  (`src/jls/sim/BatchSimulator.java`) samples the stale field for the
  trace's time-0 entry. The error is masked downstream (the time-0
  event overwrites the entry at the same timestamp, and VCD folding
  takes the last value per timestamp), but the shadowing — and the
  dead `notQOut` local next to it — is a latent trap.
- **S3 — `initInputs` is not recursive.** `Simulator.initInputs`
  initializes input points to 0 for *top-level* elements only;
  inputs inside subcircuits start as null and are read as zero by
  each element's null-tolerant `react`. Benign today, but the
  null-versus-zero asymmetry between depths is an accident, and any
  future `react` that dereferences without the null check will fail
  only inside subcircuits.
- **S4 — Pause element: code and help page disagree.**
  `Pause.react` (`src/jls/elem/Pause.java`) pauses only when the
  input changes to a **non-zero** value; the help page
  (`resources/help/elements/timing/pause.html`) says it pauses when
  the input "changes value". One of them is wrong (the Stop element's
  page, by contrast, matches its code).
- **S5 — A state machine with no matching transition freezes
  silently.** `StateMachine.react` sets `busy = true` and returns
  when `getNextState()` finds no transition ("stay busy forever"),
  so the machine ignores every subsequent clock edge with no
  warning to the user — and that branch also skips the
  remembered-clock update, unlike every other early return in the
  method.
- **S6 — TriState posts an output event on every input change.**
  Unlike `Gate.react`, `TriState.react` has no `toBeValue` change
  check, so it schedules a (frequently redundant) event per input
  notification. Correctness is preserved by `Output.propagate`'s
  change detection; the cost is queue traffic only.
- **S7 — Constant truncates to the attached net's width at
  simulation time.** `Constant.react` masks its configured value to
  `getWireEnd().getBits()` bits before propagating; a value wider
  than the net it ends up attached to silently loses its high bits
  during simulation rather than being rejected when wired.
