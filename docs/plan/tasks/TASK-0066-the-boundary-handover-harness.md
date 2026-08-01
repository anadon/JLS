# TASK-0066 - The boundary handover harness

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0065

## Deliverable

The machinery that makes a fidelity choice **testable**: a declared boundary, a
declared instant at which it can be switched, a state mapping across that
switch, and a gate proving that switching to the same thing changes nothing.

1. **`Boundary` - the enforceable statement of what is observable.** A type in
   `jls.sim` exposing only ports and values: no method may take or return
   `Circuit`, `Element`, `Put`, `WireNet`, `WireEnd` or `LogicElement`. It wraps
   what `SubCircuit` already has and nothing more - `inmap: Input -> InputPin`
   and `outmap: OutputPin -> Output` (`src/jls/elem/SubCircuit.java:33-35`) are
   the whole of the in/out surface, with `react` (`:621-636`) the entire inbound
   path and `send` (`:646-652`) the entire outbound path. The boundary contract
   is therefore a **description** of existing behavior, not a new restriction.
2. **`jls.sim.equiv`, a new headless package.** `Dut(Circuit, instancePath,
   implId)`; `Stimulus` sealed over `Exhaustive(totalInputBits)` (<= 20),
   `Seeded(seed, words)` and `FromTestFile(Path)`; `Schedule` sealed over
   `Quiescence(eventBudget)` and `SyncNet(probeName, Edge)`; `Observation(index,
   in, out)`; `Verdict` sealed over `Equivalent`, `Divergent`, `Inconclusive`
   and `PortMismatch`; and
   `BoundaryEquivalence.compare(reference, candidate, stimulus, schedule)`.
   **`Inconclusive` being a first-class arm is the point**: a non-quiescent
   design can never be reported as passing.
3. **`Simulator.runUntilQuiescent(long budget)`** - the existing loop body
   (`src/jls/sim/Simulator.java:215-243`) with one extra termination condition.
   Queue drained => settled => sample. Budget exhausted => `Inconclusive`. This
   is also the combinational-loop detector, and it is the *only* engine change
   the harness needs.
4. **Stimulus injection through the existing path, not a new one.** For each
   word: write into the instance's `Input`s via `Input.setValue` and post one
   `SimEvent(now, instance, PinChanged())` - **exactly the two operations
   `WireNet.propagate` already performs** (`src/jls/elem/WireNet.java:496-509`).
   No drawn testbench, and the replay is faithful because it is the same code
   path. The instance is located by the dotted qualified name
   `LogicElement.getFullName()` already builds.
5. **The handover.** Toggling an instance's binding at a declared instant maps
   that boundary's state: quiesce, snapshot the boundary's port values and the
   instance's element state, install the other implementation, restore. The
   state encoding is designed **once, here**, so the identical bytes can later
   become the body of an optional checkpoint section - that is the ruling in
   `04-mechanisms.md` C2 and it is why this task enables FEAT-035 rather than
   depending on it.
6. **The null-toggle equivalence gate.** Toggling structural -> structural at
   any declared instant must produce a byte-identical continuation. If the null
   toggle is not free, the state mapping is incomplete, and no non-null toggle
   result means anything.

Done means: a maintainer can point at one subcircuit instance, switch its
implementation mid-run, and get either a byte-identical continuation or a named
divergence with an index and a port.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-031 | The harness half. TASK-0065 makes the choice expressible; this makes it *checkable*, which is what turns parity from an assertion into an experiment. |
| FEAT-035 | The state encoding and the quiesce-snapshot-restore sequence are the checkpoint primitive, designed once so TASK-0074 promotes the same bytes into a section rather than inventing a second encoding. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0065 | The harness compares two `Dut`s distinguished by `implId` and toggles the binding an instance names. Both the id space and the sealed `SubCircuitImpl` set it selects from exist only after TASK-0065; without them there is nothing to toggle and nothing to name in a verdict. |

## Acceptance test

Harness self-tests first - a harness that cannot fail is worthless.

- **`jls.sim.equiv.HarnessSelfTest.structuralAgreesWithItself()`** (new):
  compare `StructuralImpl` to `StructuralImpl` on a nontrivial fixture; assert
  `Equivalent(n)` with `n` equal to the expected observation count. Catches
  harness nondeterminism.
- **`jls.sim.equiv.HarnessSelfTest.aDeliberatelyWrongImplIsCaughtAtTheFirstDivergingWord()`**
  (new): inject a one-row-mutated `TruthTable`; assert `Divergent` with the
  **exact** index and port name. The single most important test in the task.
- **`jls.sim.equiv.HarnessSelfTest.aNonQuiescentDesignIsInconclusiveNotEquivalent()`**
  (new): a ring-oscillator fixture; assert `Inconclusive`, and assert it is
  **not** `Equivalent`.
- **`jls.sim.equiv.HarnessSelfTest.portMismatchIsDetectedBeforeAnySimulation()`**
  (new): a candidate with a renamed port; assert `PortMismatch` **and** assert
  zero events were posted.
- **`jls.sim.equiv.NullToggleTest.togglingToTheSameImplIsByteIdentical()`**
  (new, the gate named in the registry): run to a declared instant, toggle
  structural -> structural, run to completion; assert the produced VCD and the
  watched-element report are byte-identical to an uninterrupted run.
- **`jls.sim.equiv.BoundarySurfaceTest.boundaryExposesNothingInside()`** (new):
  reflectively assert no public method of `Boundary` has a parameter or return
  type in `{Circuit, Element, Put, WireNet, WireEnd, LogicElement}`. The
  boundary contract as a compile-independent, mutation-resistant assertion.
- **`jls.sim.equiv.SoleStrategyTest.implementationsNeverTouchTheEventQueue()`**
  (new): bytecode-scan every `SubCircuitImpl` permit - `Simulator.post` allowed;
  `eventQueue`, `dupCheck`, `poll` and `runEventLoop` forbidden. **This is the
  mechanical proof that the toggle is a model change and not a second execution
  strategy**, which is what keeps it outside recorded decision #221's amendment
  clause. Without it, that claim is an argument.
- **`jls.HeadlessCoreRatchetTest`** (existing, `test/jls/HeadlessCoreRatchetTest.java`):
  extend its package list to cover `jls.sim.equiv` - the harness must import no
  AWT, no Swing and no `jls.edit` (issue #77).

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on / informs (open) - the differential oracle #202 asks for is the same comparison shape; this task builds the boundary-level version and TASK-0073 builds the retirement-indexed one. This task does not close #202. |
| 221 | Decision: simulation execution strategy - discrete-event vs a levelized/compiled evaluation pass | informs, **closed**. Its equivalence criterion ("observably identical ... per-element propagation delays") is precisely what `BoundaryEquivalence` computes; `SoleStrategyTest` is what shows the criterion is not engaged in the first place. |

**No issue** exists for the fidelity boundary or the parity harness. Recorded as
a gap; #232 covers only the value representation.

## Notes

- **Trap: there is no programmatic construction API at HEAD.** Every in-tree
  generative path emits save-format text and re-parses it. The harness sidesteps
  this by loading each DUT with `Circuit.load` and injecting through
  `Input.setValue` + one `PinChanged` post - do **not** wait for TASK-0038's
  construction verbs, and do **not** add a seventh emit-text-and-reparse path.
- **Trap: state persists across stimulus words, deliberately.** Reset happens
  once, at `initSimulation` (`Simulator.java:177-201`), which also hands the
  index-0 observation for free. A harness that reset per word could not test a
  sequential DUT at all.
- **Trap: four elements leak out of a subcircuit and each must be enumerated.**
  `SigGen` self-disables inside an imported circuit
  (`src/jls/elem/SigGen.java:171-173`), and jumps are circuit-local
  (`src/jls/elem/JumpStart.java:112`, `getCircuit().addJumpStart(...)`) - so
  those two do not leak. `Clock`, `Stop`, `Pause` and `Display` have **no such
  guard**, and `Stop`/`Pause` reach the simulator directly
  (`docs/simulation-semantics.md` §11). A boundary containing any of them is
  either refused with a named diagnostic or its leak is a documented accepted
  loss. Enumerate; do not discover.
- **Trap: `Memory` reads are asynchronous.** An address change posts a
  `MemoryRead` at `now + accessTime` (`src/jls/elem/Memory.java:1384, 1396,
  1402`). "Quiescent" for a boundary containing a `Memory` therefore means "the
  queue is drained", not "no more inputs are changing" - which is what
  `runUntilQuiescent` implements and what the budget bounds.
- **Trap: `SubCircuit.initSim` recurses in stable-id order**
  (`src/jls/elem/SubCircuit.java:592-611`, issue #181) and the outer walk does
  the same (`Simulator.java:196-200`). Any restore path must re-seed in that same
  order or the continuation is not byte-identical - the null-toggle gate catches
  it.
- **The handover instant is one type, not two.** The record/replay retirement
  index and this harness's declared sync point are the same notion;
  `04-mechanisms.md` C6 rules it one type owned by the parity harness, consumed
  by TASK-0072/TASK-0073. Do not mint a second.
- **Where the cost goes.** The comparison is small. Two weeks is the boundary
  type, the eight tests above (two of them reflective or bytecode-level), and
  `jls.sim`'s 93.0/92.0/84.5 floor plus the 80/82 PIT bar on `jls.sim.*`
  (`pom.xml:452-470, 781, 812-813`) applying to all of it.

## Evidence

- `src/jls/elem/SubCircuit.java:33-35` (`inmap`/`outmap` - the whole boundary),
  `:592-611` (`initSim`, stable-id recursion), `:621-636` (`react` - the entire
  inbound boundary), `:646-652` (`send` - the entire outbound boundary).
- `src/jls/elem/WireNet.java:496-509` - `inp.setValue(newValue)` followed by one
  `sim.post(new SimEvent(now, element, new SimEvent.PinChanged()))`: the two
  operations the harness replays.
- `src/jls/sim/Simulator.java:165-170` (`post` is the only enqueue path),
  `:177-201` (`initSimulation`), `:215-243` (the loop `runUntilQuiescent`
  extends), `:252-270` (the mode hooks a mode uses instead of a second loop).
- `src/jls/elem/SigGen.java:171-173` (self-disable when imported),
  `src/jls/elem/JumpStart.java:112` (jumps are circuit-local).
- `mech-fidelity-toggle.md` §3.2 (`Boundary`), §4.4 (the observation function
  and the two sampling tiers), §7.1 (the API this deliverable restates in one
  place), §7.2 (how it runs using only HEAD mechanics), §7.3 (T1-T9, from which
  the acceptance tests above are drawn).
- `04-mechanisms.md` C2 (sidecar first, one state encoding promotable to a
  section), C6 (one retirement/sync type, owned by the parity harness).
- `ARCHITECTURE.md:341-368` - recorded decision #221 as ratified: the
  interpreter is the sole strategy and any second one must be observably
  identical.
