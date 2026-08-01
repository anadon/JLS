# TASK-0102 - Bridge elements and the synchronization protocol

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0097, TASK-0105

## Deliverable

Two drawable level converters, and the lock-step contract between the analog
solver and the discrete-event loop written down with its forward-only rule
stated - no rollback, because the ownership is inverted so that none is needed.

Precisely what changes:

1. **`src/jls/elem/Adc.java`** - a `LogicElement` with attributes
   `vlow`, `vhigh`, `tdelay`. `vlow < vhigh` yields thresholding, a dead band
   and hysteresis from one function; `vlow > vhigh` is refused at validation as
   a typo, not silently swapped. `tdelay >= 1` tick is a **mandatory
   publication floor**: a crossing published at `now` could be consumed by logic
   that changes a boundary input at `now`, and JLS has no bound on same-timestamp
   event passes, so the floor is what makes the loop terminate. **Sample rate is
   not a parameter.** Sampling is drawn: `Adc -> Register <- Clock`.
2. **`src/jls/elem/Dac.java`** - attributes `vlow`, `vhigh`, `vhiz`, `trise`,
   `tfall`, `rout`. A digital change at tick `t_d` **ramps** as PWL over
   `[t_d, t_d + trise]` and registers a breakpoint at `t_d + trise`. Ramping is
   not optional: a step into a continuous solver is a discontinuity the
   integrator must reject and re-take. Because both endpoints are integers,
   landing on the breakpoint is exact and no minimum-break heuristic is needed.
3. **`src/jls/elem/IdealAdc.java` / `IdealDac.java`** - the behavioral rung of
   the abstraction ladder, so "draw it behaviorally, then replace it with
   resistors, and have the tool prove they agree" is a passing test rather than
   a slogan.
4. **The registration tax, all of it.** `src/jls/elem/LogicElement.java:17-21`
   (sealed, **24 permits at HEAD**) gains the four types; every exhaustive
   switch over the hierarchy stops compiling until it handles them - that is the
   contract working. `src/jls/elem/ElementRegistry.java:38` (35 rows at HEAD)
   gains four; `SaveTags`, a renderer, a dialog, the per-view palette rows
   (TASK-0105), and an `HdlExporter` policy bucket each follow - an element in no
   bucket aborts **every** HDL export of a circuit containing it.
5. **`src/jls/analog/AnalogRegion.java`** - the region as a self-scheduling
   element holding exactly one pending self-event, posted as
   `new SimEvent(t_next, this, new PinChanged())`. This is `Clock`'s exact idiom
   (`src/jls/elem/Clock.java:392,421`) and `Adder`/`Memory`'s post-a-computed-
   future-value idiom. Reusing `PinChanged` means **no new `SimEvent.Payload`
   record**, therefore no edits to the exhaustive payload switches.
6. **`src/jls/sim/Simulator.java`** gains five lines:
   `protected final long nextEventTime()` returning `eventQueue.peek()`'s time
   or `Long.MAX_VALUE`. Observational only; it must not mutate the queue. The
   loop's own javadoc already sanctions the peek (`:210-218`).
7. **The step cap, in two shipped regimes** (the third is deferred and named):
   - region has **zero** `Dac`s: cap is unbounded. A region with no `Dac` cannot
     be invalidated by any digital event, because the only path from the engine
     into the solver is a `Dac`. This deletes rollback for the sensor capstones.
   - every `Dac` driven only by clocked elements: cap is
     `min(delta_LTE, next breakpoint, nextEventTime() - t)`, exact because
     `Clock` self-schedules its next transition, so the earliest possible `Dac`
     change is already in the queue.
   - asynchronous speculation: **not built**, named in the javadoc with its
     mechanism (epoch counter, accepted-point ring, invalidate-and-re-integrate)
     so its absence is a decision and not an omission.
8. **`docs/simulation-semantics.md`** gains the A-STEP section: the digital loop
   owns `now`; the solver's private `double t` is the only thing that ever moves
   backwards; only accepted results become posted events.
   **`docs/batch-interface.md` §3.1 gains the documented behavior change** (see
   Notes).

Done means: a drawn `Dac -> RC -> Adc` loop runs to a stated tolerance, the
digital stream out of the `Adc` is byte-identical when the LTE tolerance is
tightened by 10x, and no analog code appears in `runEventLoop`.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-048 | The bridges and the synchronization contract are the feature. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0097 | The crossing search bisects **accepted timepoints of a solved trajectory**. `Newton`, `Trapezoidal` and `TimestepController` are what produce them; there is nothing to bisect before they exist. |
| TASK-0105 | `PaletteContractTest.paletteIsTotalOverTheElementRegistry` (`test/jls/edit/PaletteContractTest.java:47-66`) is green and asserts exactly one palette entry per registered type outside `{SubCircuit, WireEnd, TestGen}`. Registering four analog types therefore *forces* four buttons onto the first-year toolbar. The view dimension must exist first or K9 is broken by a passing test. |

## Acceptance test

`test/jls/analog/BridgeTest.java`, new:

- `adcCrossingIsTheEarliestTickAtWhichTheSolvedTrajectoryCrosses()` - a ramp
  through the threshold, asserting the published tick equals the bisected
  crossing and that the bisection terminated within `ceil(log2(delta_ticks))`
  extra Newton solves.
- `theDigitalStreamIsIndependentOfTheTimestepController()` - the same fixture at
  three LTE tolerances spanning 10x, asserting the emitted digital transition
  ticks are identical. This is the payoff the crossing policy exists for; if it
  fails, the crossing is being read off accepted points instead of bisected.
- `hysteresisFallsOutOfVlowLessThanVhigh()` and
  `vlowGreaterThanVhighIsRejectedAtValidation()`.
- `tdelayBelowOneTickIsRejected()` - the publication floor, asserted as a
  validation error with the same message shape as the causal delay floor.
- `dacRampRegistersABreakpointAtTdPlusTrise()` and
  `landingOnTheBreakpointIsExactWithNoMinimumBreakHeuristic()`.

`test/jls/analog/AStepTest.java`, new:

- `aRegionWithNoDacRunsUncapped()` - assert the cap query returns unbounded and
  that the number of analog visits per digital event is 1.
- `aSynchronousDacRegionCapsAtTheNextQueuedEvent()` - assert the cap equals
  `nextEventTime() - t` when that is the minimum, and that a 44.1 kHz `Dac`
  against a fast clock does **not** produce one analog visit per queued digital
  event (the naive conservative cap, which is arithmetically fatal).
- `theRegionHoldsExactlyOnePendingSelfEvent()` - asserted after every step,
  because two would multiply the queue.

`test/jls/BatchSimulationGoldenTest` gains
`ananalogRegionReportsTimeLimitNotNoMoreActivity()`, pinning the documented
outcome-line change on an analog fixture and pinning that a purely digital
fixture's outcome line is unchanged.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | no issue | The entire analog program - the solver, the bridges, the device models and the palette - has no tracking issue. `11-analog-determination.md` §2.5-2.6 is its written owner. |

## Notes

- **One documented behavior change, and it must be documented, not discovered.**
  A region always holds one pending self-event, so `runEventLoop`'s
  `!eventQueue.isEmpty()` guard (`src/jls/sim/Simulator.java:217`) never fails
  while analog time remains, and `BatchSimulator.displayOutcome`
  (`:562-572`) will always report `"Simulation Time Limit"` and never
  `"Simulation: No More Activity"` for an analog circuit. That is correct - it
  is SPICE's `.tran tstop`, and `-d`/`maxTime` **becomes** `tstop` for free -
  but `docs/batch-interface.md` §3.1's four frozen reasons and their precedence
  are a stability contract, so amend the document in the same change.
- **The self-event coalesces with a `WireNet`-posted event at the same tick.**
  `Simulator.post` dedups through `dupCheck` and `SimEvent.equals`
  (`src/jls/sim/SimEvent.java:162-172`) deliberately excludes `seq`. One visit
  per tick handles both, which is what you want - but it means the region cannot
  rely on its self-event surviving as a distinct object.
- **Do not put a sample rate on either bridge.** It creates a second clock
  competing with `Clock` for who owns time, which is how mixed-signal simulators
  acquire their worst bugs. An 8-bit converter is 8 `Dac`s and 16 resistors, or
  a comparator plus R-2R plus a SAR register split across the boundary.
- **The bridges live inside the analog region.** The region's external ports stay
  digital and `BitSet`-valued, so port congruence type-checks unmodified and the
  digital half of the model learns nothing about volts.
- **Until four-state values land, a `t=0` sample inside the hysteresis band lands
  on 0** and must be *reported* in the house coerce-count-report idiom rather
  than silently resolved.

## Evidence

- `src/jls/sim/Simulator.java:165-170` (`post` with `dupCheck`), `:210-218` (the
  javadoc sanctioning a peek), `:217` (the `isEmpty` guard), `:228` (`now` set
  from the event).
- `src/jls/sim/SimEvent.java:162-172` (`equals` excluding `seq`), `:186-192`
  (`hashCode`).
- `src/jls/elem/Clock.java:392,421` - `sim.post(new SimEvent(...))` self-events,
  the idiom the region reuses.
- `src/jls/sim/BatchSimulator.java:562-572` - `displayOutcome` and its four
  reasons in precedence order.
- `src/jls/elem/LogicElement.java:17-21` - the sealed permits clause, 24 entries
  at HEAD; `src/jls/elem/Element.java:17-18` - 3 permits, unchanged by this task.
- `src/jls/elem/ElementRegistry.java:38` - the `ALL` table, 35 rows at HEAD.
- `test/jls/edit/PaletteContractTest.java:44-45,47-66` - the non-palette tag set
  and the totality assertion that forces the palette row.
- `11-analog-determination.md` §2.5 (D-A7, the bridges and crossing policy P-b),
  §2.6 (D-A8, the inverted ownership and the three step-cap regimes).
- Do not restate: `docs/simulation-semantics.md` owns the event model;
  `11-analog-determination.md` owns the A-STEP derivation.
