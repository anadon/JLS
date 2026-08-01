# TASK-0112 - Property checking, equivalence and coverage over an unfamiliar design

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0111

## Deliverable

Three things a person can do to a design they did not write: state a property
and have it checked, prove it equivalent (or not) to a reference, and see which
parts of it the vectors never touched - each reported through the one verdict
channel, with the answer-logging discipline written down.

Precisely what changes:

1. **`Assert` and `Cover` as drawn elements.** New `LogicElement` subclasses
   beside `Stop` and `Pause` - the simulation-control family
   `docs/simulation-semantics.md` §11 already names. Full registration tax:
   `src/jls/elem/LogicElement.java:17-21` (sealed, 24 permits at HEAD - every
   exhaustive switch over the hierarchy stops compiling until it handles them),
   `src/jls/elem/ElementRegistry.java:38` (35 rows), `SaveTags`, renderer,
   dialog, a per-view palette row (TASK-0105), and an `HdlExporter` policy
   bucket - an element in no bucket aborts every export of a circuit containing
   it. A property is therefore *assembled out of gates*, which is the same thing
   `Stop`'s existing condition input already is.
2. **`src/jls/sim/CoverageCollector.java`** - four measures at three
   instrumentation points that already exist, so the collector adds no new walk:
   - **toggle coverage**, per `WireNet`, per bit (0 seen, 1 seen, HiZ seen) -
     the site is `WireNet.propagate` (`src/jls/elem/WireNet.java:443`), beside
     the `sim.probeSample(...)` call at `:525`. Two long masks per net.
   - **transition coverage** on `StateMachine` - `State.getNextState()`
     (`src/jls/elem/State.java:1272-1306`) already walks the transition list and
     returns the match. Transition coverage strictly dominates state coverage
     and costs the same.
   - **row coverage** on `TruthTable` - `react` already computes `matchingRow`
     (`src/jls/elem/TruthTable.java:1408-1428`); "which rows fired" is one
     `boolean[rows]`. The silent-hold hole at `:1430-1434` - a table with no
     matching row holds its outputs and tells nobody - becomes an explicit
     **"no row matched"** bin. This is the schematic analogue of branch
     coverage and the measure that tells an instructor *"your vectors never
     exercised the carry case."*
   - **select coverage** on `Mux`/`Decoder`, plus HiZ and multi-driver-conflict
     counts, which turn today's one-shot warnings into numbers.
3. **`src/jls/formal/`**, new headless leaf: `Aig.java` (and-inverter graph),
   `Tseitin.java` (CNF), a bundled SAT solver, `Miter.java`, and
   `Counterexample.java`. Combinational equivalence first; register-boundary
   (sequential-with-matched-registers) second, which reuses the combinational
   engine with no new machinery.
4. **CLI, additive under `docs/batch-interface.md` §6**, on the report channel
   TASK-0111 built: `-cov <file>`, `-equiv <reference.jls>`, `-map <file>`,
   `-cex <file>`, `-bmc <k>`. Exit statuses extending §1's 0/1/2 and
   TASK-0111's 3: **0** proved equivalent, **3** counterexample found (simulated
   and confirmed), **4** unknown (timeout, resource limit, BMC exhausted at
   depth k), **5** not checkable. **4 and 5 are never passes.**
5. **`-cex` writes the counterexample as a `-t` test-vector file**, so the
   failure is replayable in the tool that produced it and gradeable by the
   harness that found it.
6. **The answer-logging discipline, recorded.** Every check writes its verdict,
   the seed, the vector index and the sync point into the report, so a grade is
   auditable after the fact. `docs/batch-interface.md` gains the status table;
   `docs/simulation-semantics.md` §11 gains `Assert` and `Cover` and §12's
   spec-to-golden mapping gains their rows.

Done means: an unfamiliar `.jls` can be checked against a reference, its
uncovered structure listed, and its properties proved or refuted with a
replayable counterexample.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-053 | The property, equivalence and coverage half of the feature; TASK-0111 built the front end and the channel. |
| FEAT-034 | The parity harness compares two traces; equivalence and coverage are what let it say *why* they differ and whether the exclusion set is hiding something. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0111 | Every check here needs somewhere to put its result. The report channel and the exit-status contract are a change to a **stability promise** and must be designed before anything that emits into them ships; building the checks first means designing the promise twice. |

## Acceptance test

`test/jls/formal/EquivalenceTest.java`, new:

- `aRenamedButStructurallyIdenticalCircuitProvesEquivalent()` - exit 0, with the
  proof object retained.
- `aOneBitCarryBugProducesACounterexampleThatFailsWhenReplayed()` - the
  load-bearing one. Assert exit 3, then take the `-cex` file, run it through the
  ordinary batch path against both circuits, and assert the outputs **disagree**.
  A counterexample that is not confirmed by simulation is a solver bug reported
  as a student bug; this test is what makes status 3 honest.
- `memoryOnTheBitBlastingPathReturnsNotCheckable()` - exit 5, not 0, and the
  message names `Memory`.
- `aMultiDriverNetAndACombinationalLoopEachReturnNotCheckable()` - exit 5 each.
- `aSolverTimeoutReturnsUnknownNotEquivalent()` - exit 4, asserted with a
  deliberately hard miter and a low budget.

`test/jls/sim/CoverageCollectorTest.java`, new:

- `truthTableNoRowMatchedIsItsOwnBin()` - drive a table into the silent-hold
  path and assert the bin is reported. This surfaces a real defect class the
  element hides today.
- `toggleCoverageCountsZeroOneAndHiZSeparatelyPerBit()`.
- `transitionCoverageDominatesStateCoverage()` - a state entered by two
  transitions of which one never fires reports 100% state and 50% transition.
- `coverageIsOffByDefaultAndCostsNothingWhenOff()` - assert the collector is not
  installed without `-cov`, and pin an event-count budget on a fixture run so a
  hot-loop regression fails the test rather than the release. The warm event
  loop is roughly 318 ns/event with about 48% of it already queue and
  dedup bookkeeping; an unguarded per-event hook is a measurable tax.

`test/jls/GradeReportGoldenTest` gains
`coverageAndEquivalenceSectionsMatchGoldenByteForByte()`.

`test/jls/CliSmokeTest` gains
`unknownAndNotCheckableAreNeverReportedAsPasses()` - exercised through the
grading harness, because the failure mode that matters is a grader that treats 4
or 5 as success and silently grades nothing.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the differential oracle is the first real consumer of an equivalence verdict over a design the grader did not write |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | overlaps - an imported netlist is precisely "a design you did not write"; equivalence against the drawn original is the check that makes an import trustworthy |
| - | no issue | Property checking, equivalence checking and coverage have no tracking issue. `docs/capability-roadmap/lf-04-formal-and-grading.md` and `sweep-04-verification.md` changes B/D/H are their written owners. |

The standards issues these documents cite (UCIS toggle coverage, the VHDL/SV
test runners whose xUnit artifact shape is adopted) are **not in the 34 open
issues**; do not cite them as open.

## Notes

- **Coverage says "did you exercise it"; formal says "is it right."** They
  compose: with a property you can also compute the **uncovered set** - the
  input patterns no vector reaches - as a SAT query, which is coverage computed
  exactly rather than counted approximately. Build the AIG once and both fall
  out of it.
- **Statuses 4 and 5 are the whole risk surface of a grading application.** A
  grader that maps "unknown" or "not checkable" to a pass grades nothing and
  says it graded everything. Make the harness's default mapping explicit and
  assert it.
- **Do not become a model checker.** Unbounded sequential equivalence -
  induction, PDR/IC3, reachability - is the scope creep this task must refuse by
  name. Combinational first, register-boundary second, bounded (`-bmc k`) third
  and clearly labelled as bounded.
- **`Memory` is not checkable on the bit-blasting path**, and saying so is a
  feature. An equivalence tool that quietly ignores a memory is worse than one
  that refuses.
- **The exhaustive switches are the registration contract working.** Adding
  `Assert` and `Cover` to `LogicElement`'s permits stops the build until every
  switch handles them. That is the mechanism, not a defect; budget for it.
- **Equivalence over all states includes unreachable ones**, so a check can
  report a difference a student can never provoke. Say so in the counterexample
  rendering, or the first support question is "this state can't happen".
- **The elements are drawn, so the property is a circuit.** That is the
  pedagogical point and it is also why this needs no new expression language.

## Evidence

- `src/jls/elem/WireNet.java:443` (`propagate`), `:525` (the existing
  `sim.probeSample` call the toggle counters sit beside).
- `src/jls/elem/State.java:1272-1306` (`getNextState`, which already walks and
  matches the transition list).
- `src/jls/elem/TruthTable.java:80,94` (`int[][] table`, `rows`), `:1400`
  (`react`), `:1408-1428` (the `matchingRow` computation), `:1430-1434` (the
  silent hold when no row matches).
- `src/jls/elem/LogicElement.java:17-21` - the sealed permits clause, 24 entries;
  `src/jls/elem/ElementRegistry.java:38` - 35 rows.
- `src/jls/JLSStart.java:759-789` - the `FLAGS` table the five new flags join.
- `docs/batch-interface.md` §1 (three exit statuses), §6 (the additive route).
- `docs/capability-roadmap/lf-04-formal-and-grading.md:382-410` - the batch
  contract, the flag list, and the 0/3/4/5 status table with "4 and 5 are never
  passes"; `:412-432` - the uncovered-set-as-SAT-query composition.
- `docs/capability-roadmap/sweep-04-verification.md:280-315` - the four coverage
  measures with their existing instrumentation points; `:470-500` - change H,
  the report channel and the exit-status promise.
- Warm-loop cost figures (318 ns/event, ~48% queue and dedup) are measured
  engine constants recorded in the parity study's brief; they bound what a
  per-event hook may cost.
- Do not restate: `docs/simulation-semantics.md` owns the event model and §11's
  simulation-control elements; `docs/batch-interface.md` owns the exit contract.
