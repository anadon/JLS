# CAP-09 - Verify a design you did not write

**Status:** proposed | **Priority:** 6 | **Marginal cost:** 16-26 mw |
**Standalone cost:** 20-34 mw

## Outcome

A reviewer or grader who did not draw a circuit gets a machine-readable verdict
about it - properties checked, equivalence proved or refuted with a replayable
counterexample, and coverage reported as data - instead of diffing formatted
stdout against literal expected strings.

## Acceptance test

SEEN: an instructor opens a submission for the first time and runs
`jls -b -equiv ref.jls -cex cex.t -formal-report result.xml submission.jls`.
One of exactly three things happens: it is proved equivalent over the declared
care set and the report prints the matched port table it actually proved; a
counterexample comes back as a `-t` file that replays in the GUI with the
failing cone highlighted; or the answer is UNKNOWN with a stated reason and an
exit status that is never a pass. The same submission then runs under a
property and coverage suite and yields an xUnit report naming which properties
held, which nets never toggled, which `TruthTable` rows were never matched and
which `StateMachine` transitions were never taken.

CHECK: five named tests.
- `FormalResultTotalityTest` - the result type has exactly three constructors,
  `PROVED(proofLog) | COUNTEREXAMPLE(model) | UNKNOWN(reason)`, and no default.
  A corpus of deliberately uncheckable circuits - a `Memory`, a two-driver net,
  a combinational loop, an incomplete `TruthTable`, a 24-bit multiplier - each
  exits 4 or 5. Written before the solver exists.
- `CounterexampleReplayTest` - every counterexample is written as a `-t` file,
  re-simulated through the batch simulator, and must reproduce the
  disagreement. A counterexample that does not reproduce fails the build.
- `MiterPortMatchTest` - port matching refuses on any ambiguity of name, order
  or width, and the matched-port table is printed for passing runs too.
- `CoverageReportGoldenTest` - toggle, transition, row and bin coverage over a
  fixture is byte-stable run to run and across platforms.
- `DifferentialOracleAgreementTest` - an external toolchain and JLS, run over
  the same design, agree per retired instruction with the exclusion set stated
  as data rather than hidden in the comparator.

## Demo slice

The 8-11 mw floor: formula extractor over the combinational subset, AIG,
Tseitin/CNF, a solver, miter construction, `-equiv`, counterexample-as-`-t`,
and exit statuses 0/3/4/5 - plus the ~1 wk formal-only flattening elaborator so
`SubCircuit` is not excluded on day one. That alone converts grading of every
combinational assignment from sampling to proof, and it is the slice that tests
whether the verdict is trusted before any sequential tier is funded.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-053 | Test-vector front end and autograding at scale | property checking, equivalence and coverage over an unfamiliar design is this capstone's payload | required |
| FEAT-034 | Retirement-indexed parity harness and `RetireRecord` | comparing someone else's design against a reference is the same mechanism, with over-constraining made a compile error | required |
| FEAT-026 | The four-state value core with a resolution fold | a verdict must be able to say "unknown" rather than silently resolving to 0, and don't-care-aware grading needs the fourth state | required |
| FEAT-023 | External toolchain differential oracle and the board on-ramp | an independent witness is what makes a verdict about a design you did not write credible | required |
| FEAT-009 | The measurement gate and a tracked calibration fixture | a verification run with no budget is not a verdict; the budget and its ratchet come from here | required |
| FEAT-005 | Quadratic and materializing I/O paths eliminated | a verification suite re-parses stimulus and re-emits waveforms many times per submission | required |
| FEAT-006 | Simulation capacity and long-run ergonomics | an unfamiliar design sets its own run length; a silent event drop would corrupt a verdict | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | the suite is worthless if it only holds on one platform | required |
| FEAT-024 | Black-box HDL component and external co-simulation | the part of the design that cannot be read stays in the tool that can run it | beneficial |
| FEAT-031 | The per-instance fidelity toggle and its boundary harness | parity as a property of a boundary is how an unfamiliar subcircuit is verified in isolation | beneficial |
| FEAT-030 | Engine constant factors: the semantics-preserving stack | verification means running the same design many times | beneficial |
| FEAT-032 | The host byte port, a `Console` element and transcripts | a transcript is the replayable evidence for an interactive design | beneficial |
| FEAT-033 | `jls.mach`, the reference runner and the guest software stack | the reference the unfamiliar design is checked against | beneficial |
| FEAT-035 | Checkpoint and simulation-state serialization | a multi-hour verification run must survive a handover | beneficial |
| FEAT-038 | The drawn structural RV32 machine | a design of known-correct provenance to calibrate the verification suite against before it is pointed at an unfamiliar one | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | closes (jointly with CAP-02) - its differential oracle is the shape this capstone generalizes |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | depends on (FEAT-024) |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | depends on (FEAT-026, FEAT-030) |
| 265 | CI test parity across supported platforms | depends on (FEAT-007) |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | overlaps - shares FEAT-053; CAP-06 is the capstone that closes it |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists | informs - a design you did not write frequently arrives this way |
| - | the capstone itself | **no issue** |
| - | formal equivalence, property checking, coverage as data | **no issue** - verified: an open-issue search for formal/equivalence/coverage returns only the UI-coverage issues #162 and #91 |

## Open decisions

1. **In-tree CDCL solver or a library.** Recommendation: in-tree CDCL with DRAT
   logging and a proof checker. Reason: the single offline jar is load-bearing,
   ground rule 4 of `docs/library-survey-2026-07.md:35` (actively maintained
   projects only) would have to be applied to any solver dependency and could
   force it back out later, and every SAT answer is confirmed by simulation
   regardless. Record it in `ARCHITECTURE.md` with a revisit trigger.
2. **The report channel and exit-status lattice.** Recommendation: design it
   once, before the first checker, with every consumer's verdict in it. Reason:
   `docs/batch-interface.md` §1 has exactly three statuses and no way to say
   "the run completed and the answer was wrong"; reopening a documented promise
   twice is worse than designing it once.
3. **What UNKNOWN means to a course.** Recommendation: statuses 4 and 5 are
   never passes, and the grading bridge must report them as ungraded-by-tool
   rather than as failures. Reason: one publicized false pass ends the feature,
   because the fallback - vectors - is still there and is at least honestly weak.
4. **The checkable subset on day one.** Recommendation: fund the flattening
   elaborator so `SubCircuit` is in scope, and state in writing that `Memory`,
   `RegisterFile` and `FieldExtend` are not. Reason: those are exactly the four
   entries in the export policy's refusal map at HEAD.
5. **Unbounded sequential checking.** Recommendation: never implemented in
   tree; delegated via BTOR2. Reason: reachability, induction, PDR and liveness
   are individually reasonable and collectively a research programme.

## Kill criteria

1. **One** false pass reaching a user - a submission reported PROVED that a
   `-t` vector refutes - withdraws equivalence from the grading path
   permanently; vectors remain the only supported grader.
2. If the floor slice exceeds **15 mw** actual against its 8-11 mw estimate,
   stop before the sequential tiers and ship the combinational capability only.
3. If, after the flattening elaborator, the checkable subset covers less than
   **60%** of element instances in the reference lab corpus, stop at the
   coverage half and do not build the formal half.
4. If the UNKNOWN rate on the reference submission corpus exceeds **20%**, the
   feature is a demo; stop and re-scope.
5. If coverage reports are not byte-stable run to run on a single platform,
   stop. A non-deterministic verdict about someone else's design is worse than
   no verdict.

## Evidence

- Cost band: the formal-and-coverage half of the amended P5,
  `docs/capability-roadmap/AMENDMENT.md:155-219` (P5 amended to 33-50 mw), with
  the per-slice table and the 8-11 mw floor at
  `docs/capability-roadmap/lf-04-formal-and-grading.md:729-762`.
- The shipped grading criterion is three literal stdout lines:
  `examples/autograde/autograde.py:45,53` (`EXPECTED_FINALS`,
  `EXPECTED_STDOUT_LINES`), pinned by
  `test/jls/AutogradeBridgeExampleTest.java`. A submission wrong on 254 of 256
  inputs and right on one passes.
- No verdict channel exists: `docs/batch-interface.md:33-48` defines exactly
  three exit statuses - 0 run completed, 1 runtime failure, 2 usage error.
- Don't-care is destroyed on the output side:
  `src/jls/elem/TruthTable.java:1447-1449` (`if (outValue == 2) outValue = 0;`),
  and an unmatched row returns silently holding its outputs at
  `src/jls/elem/TruthTable.java:1430-1434`.
- The `Assert` archetype already exists: `src/jls/elem/Stop.java:147-161` and
  `src/jls/elem/Pause.java:167-180` are 1-bit-in, zero-out elements whose whole
  job is a side effect when a condition holds.
- There is no timestamp-closure hook: `src/jls/sim/Simulator.java:215`
  (`runEventLoop`) advances `now` per event.
- **Correction to the corpus, verified at HEAD `b54e6ee`.**
  `docs/capability-roadmap/AMENDMENT.md:199-201` states the HDL export reject
  list as `Memory`, `SubCircuit`, `ShiftRegister` at
  `src/jls/hdl/HdlExporter.java:418-424`. At HEAD the refusal map is
  `{Memory, SubCircuit, RegisterFile, FieldExtend}` at
  `src/jls/hdl/HdlExporter.java:460-476`, and `ShiftRegister` is in the
  exported set at `src/jls/hdl/HdlExporter.java:435`. Both the membership and
  the line numbers are stale; the *conclusion* - that the formula extractor
  inherits the exporter's refusals - survives.
- The verdict discipline this capstone must obey is stated normatively in
  `docs/parity-contract.md`; the wall-clock budget discipline in
  `docs/machine-calibration.md`. Both are referenced, not restated here.
- D10 (BRIEF §11-13) governs: this is a path and a cost, not a refusal.
- **Cost reconciliation.** Marginal band 16-26 mw. Its 8 required features
  sum to 66-103 mw and its 7 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
