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
candidate bugs, per issue #85 §9 (the seven surprises found during
the original writing were adjudicated and resolved in issue #98; the
appendix records the verdicts). The sibling document
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
2. `initInputs`: every input point of every `LogicElement` at every
   depth is set to the value 0 (`LogicElement.initInputs`,
   `src/jls/elem/LogicElement.java`). `Simulator.initInputs` walks
   only the top level, but `SubCircuit.initInputs` overrides the
   walk and recurses into its inner circuit, so initialization is
   depth-uniform (pinned by
   `SimulationSemanticsRegressionTest.initInputsReachesInsideSubcircuits`).
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

The tri-state gate follows the same discipline: `TriState.react`
tracks the value in flight (with "off" as a distinct in-flight state)
and posts an output event only when the scheduled output actually
changes (pinned by
`SimulationSemanticsRegressionTest.triStateDoesNotRepostUnchangedOutputEvents`).

Zero-delay elements (`Splitter.react`, `Binder.react`,
`InputPin.react`, `OutputPin.react`, `SubCircuit.react`,
`Constant.react`) propagate within the same timestamp, so an
arbitrarily deep chain of wiring elements adds zero time.

A `Constant` is width-agnostic: it takes its width from whatever net
it is wired to, and the value it drives is its configured value
truncated to that net's declared width — value mod 2^bits
(`Constant.react` masks before propagating; pinned by
`SimulationSemanticsRegressionTest.constantValueIsMaskedToTheNetWidth`).
A constant wider than its net silently loses its high bits; this is
the width-adaptation rule, consistent with §2's
"values are interpreted at the reader's declared width".

### 6.3 Shift register (combinational shifter)

`ShiftRegister.react` (issue #122) is a Mux-style combinational
element, not a clocked register — the name and semantics are the
bsiever fork's, whose 4.6 release shipped it, so fork-authored
circuits keep their meaning upstream. Ports: `input` (n bits, n ≥ 2),
`amount` (ceil(log2 n) bits), `output` (n bits). On any input change
it recomputes

- **LogicalLeft**: `output[i] = input[i - amount]`, zero fill;
- **LogicalRight**: `output[i] = input[i + amount]`, zero fill;
- **ArithmeticRight**: as LogicalRight but vacated high bits copy the
  sign bit `input[n-1]`;

an `amount` ≥ n therefore yields 0 (logical) or all-sign-bits
(arithmetic). There is no clock, no stored value, and no reset; the
result follows the standard §6.2 transport-delay discipline (default
delay 25). An undriven `input` or `amount` reads as 0, like Mux's
select (a deliberate hardening over the fork, whose react assumed a
driven data input). Pinned by `ShiftRegisterTest`.

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
| Shift register (combinational barrel shifter, issue #122) | 25 | `src/jls/elem/ShiftRegister.java` |
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
- If no transition matches on an edge, the machine **stays in its
  current state**, updates its remembered clock value (so later
  edges are still recognized), and reports the unmatched edge once
  per run through the `TellUser` reporter — a warning dialog in the
  interactive simulator, a `jls: warning:` stderr line in batch mode
  (issue #98 S5; pinned by
  `SimulationSemanticsRegressionTest.stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce`).

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
  `WireNet.propagate` scans the net's attached outputs in **net
  order** and delivers the first active (non-null) driver value; if
  every driver is off, the net is HiZ (null) (`WireNet.propagate`,
  `src/jls/elem/WireNet.java`). With **at most one** active driver —
  the only configuration with defined meaning — this implements
  standard bus behavior: the active driver wins, and turning the
  active driver off re-resolves to the other drivers or to HiZ.
- **Bus conflicts**: two or more simultaneously active drivers with
  *different* values are a conflict. Resolution stays deterministic —
  the first active driver in net order wins, where net order is the
  order the wire ends were added to the net (for a loaded circuit:
  fixed by the file, a breadth-first walk of the net from its first
  wire end in file order; `Circuit.finishLoad` and the insertion-
  ordered sets in `WireNet`/`WireEnd` make this stable across runs
  and JVMs, issue #98 S1) — and the conflict is reported once through
  the `TellUser` reporter (re-armed when the conflict clears). Active
  drivers that agree are not a conflict. There is no wired-AND/OR and
  no conflict (X) state. Pinned by
  `SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`.
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
  (`BatchSimulator.pause`). A change *to zero* does not pause — the
  same on-non-zero condition as the Stop element, and what the help
  page says (issue #98 S4; pinned by
  `SimulationSemanticsRegressionTest.pausePausesOnlyOnNonZeroInput`).

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
| §9 bus conflicts | `SimulationSemanticsRegressionTest` (deterministic winner, one-time warning) |
| §4 termination reasons | `CliSmokeTest` / `batch-interface.md` §3.1 |
| §5 depth-uniform init, §6.2 constant width, §8.2 unmatched edge, §11 pause condition | `SimulationSemanticsRegressionTest` |

## Appendix: Surprises found while writing this spec

This appendix collects candidate bugs, per issue #85 §9: behavior the
code exhibits that no one would specify deliberately. The seven
surprises recorded here by the original spec (S1–S7) were adjudicated
and resolved in issue #98; **nothing is currently listed**. The
verdicts, for the record:

- **S1** (multi-driver conflicts resolved by hash order) — *bug,
  fixed*: resolution is now deterministic (first active driver in
  net order) and a bus conflict is reported once; spec body §9.
- **S2** (`Register.initSim` shadowed its `currentValue` field) —
  *bug, fixed*: the field is assigned, and the dead locals are gone.
- **S3** (`initInputs` believed non-recursive) — *misreading of the
  code*: `SubCircuit.initInputs` has always recursed, so input
  initialization is depth-uniform; documented in §5 and pinned by
  test.
- **S4** (Pause code vs. help page) — *code intended*: pausing on
  change-to-non-zero parallels the Stop element; the help page was
  corrected to match.
- **S5** (state machine froze silently on an unmatched edge) — *bug,
  fixed*: it stays in its current state, keeps its clock history,
  and warns once; spec body §8.2.
- **S6** (TriState posted redundant output events) — *bug, fixed*:
  it now follows the `Gate.react` change-check discipline; spec
  body §6.2.
- **S7** (Constant truncates to the net width) — *intended*: the
  Constant element is deliberately width-agnostic, and truncation is
  its width-adaptation rule; documented in §6.2. (Rejecting wide
  constants at load was considered and dropped: it would refuse
  files that load today.)

All seven are pinned by
`test/jls/SimulationSemanticsRegressionTest.java`.
