# TASK-0100 - The external-simulator differential corpus

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0098, TASK-0099,
TASK-0051

## Deliverable

A three-tier fixture corpus comparing `jls.analog` against real ngspice within
a stated tolerance, run nightly and non-gating, plus the detectors that catch a
regression which stays *inside* that tolerance.

Precisely what changes:

- `test/jls/analog/oracle/NgspiceDeck.java` - emits a SPICE deck from a JLS
  analog fixture. Deterministic text; no `.control` section.
- `test/jls/analog/oracle/NgspiceRunner.java` - `ProcessBuilder` invocation of
  `ngspice -b`, **in `test/` only, never in `src/`**, located through
  `ToolLocator.findOnPath` (`test/jls/hdl/ToolLocator.java:67`) with
  `Assumptions.assumeTrue` self-skip, exactly as
  `test/jls/hdl/GhdlCompileTest.java:34-35` and
  `test/jls/hdl/IverilogCompileTest.java:33-34` already do. Strips the
  rawfile's `Date:` and `Command:` header lines before parsing - they carry a
  wall-clock timestamp and a build string.
- `test/jls/analog/oracle/RawfileParser.java` - the ASCII rawfile reader.
- `test/jls/analog/oracle/EnvelopeComparator.java` - the 1e-4 relative
  comparator, with the tolerance's derivation in a comment: two decades above
  ngspice's own measured build-to-build noise (~1e-13) and two decades below
  real device-model error (~1e-2).
- The corpus, all committed as fixtures:
  - **Tier A** - 10 closed-form fixtures with derived tolerances, each
    carrying the two anti-cheat assertions: the numerical error must be
    **nonzero**, and disagreement with an "independent" oracle must have a
    **lower bound** (agreement to 1e-15 means the oracle is not independent).
  - **Tier B** - 11 convergence-torture fixtures with pinned solver
    statistics: series diodes, a bridge with no DC path, a bistable latch, a
    Schmitt trigger swept through threshold, a 1e6-gain op-amp, an ideal switch
    at a breakpoint, a 1e9 time-constant-ratio stiff RC, a floating capacitor,
    zero-valued R/L/C, a relaxation oscillator, a charge pump.
  - **Tier C** - 5 invariants asserted without any oracle: reciprocity,
    Tellegen power balance, charge conservation, superposition, and
    node-ordering invariance.
- The detectors, wired into the golden header rather than added as separate
  tests: **R1** pin `steps`/`rejects`/`nrIters`/`nrFails`/`points` exactly;
  **R4** store the measured analytic error and ratchet it, so a move from
  6.5e-7 to 3.1e-6 is visible in the diff even though both pass; **R3** report
  the residual norm as a first-class output; **R5** keep two or three fixtures
  deliberately near a convergence limit.
- CI: `ngspice` appended to the best-effort apt line at
  `.github/workflows/ci.yml:73` (which already installs `iverilog ghdl yosys
  xvfb` with `|| echo "some optional tools unavailable; their tests will
  skip"`), and an `analog-oracle` job on the existing nightly schedule
  (`.github/workflows/ci.yml:12`, cron `17 4 * * *`, with its own concurrency
  group at `:24`), marked `continue-on-error: true` in the idiom the `windows`
  and `macos` jobs already use (`:156`, `:263`). Promotion to non-advisory is
  **per device family, after 20 green runs**, recorded in the job's comment.

Done means: on a machine with ngspice installed, every Tier A and Tier B
fixture agrees with ngspice inside 1e-4; on a machine without it, the whole
lane skips with a stated reason and nothing else changes.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-046 | The external-oracle half - the evidence that the solver is *correct*, not merely *reproducible*. FEAT-046's byte-goldens say nothing about correctness by construction. |
| FEAT-023 | Adds a fifth external toolchain to the differential-oracle surface, on the same locator, skip and lane machinery as `iverilog`, `ghdl`, Yosys and nextpnr. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0098 | The comparator reads the golden header's statistics block and the digest writer's record format, both of which only TASK-0098 defines. Comparing against an oracle before run-versus-run equality holds would attribute JLS's own nondeterminism to the oracle. |
| TASK-0099 | Nine of the eleven Tier B fixtures and most of Tier A need controlled sources, `PULSE`/`PWL`/`SIN` waveforms or a `.model` card. They are unexpressible before TASK-0099. |
| TASK-0051 | This adds one more tool to the CI toolchain lane and its promotion policy; TASK-0051 is what establishes that lane's shape and its required-versus-advisory split. Landing an analog oracle into an unstructured lane duplicates that decision. |

## Acceptance test

`test/jls/analog/oracle/NgspiceDifferentialTest` (new class):

- `everyTierAFixtureAgreesWithNgspiceWithinTheDerivedEnvelope()` -
  parameterized over the 10 closed-form fixtures; self-skips when ngspice is
  absent.
- `everyTierBFixtureConvergesAndItsStatisticsMatchThePin()` - parameterized
  over the 11 torture fixtures; asserts convergence **and** the pinned
  `steps`/`rejects`/`nrIters` values, so a run that stays inside 1e-6 while
  taking 20% more Newton iterations fails.
- `disagreementWithNgspiceHasANonZeroLowerBound()` - the anti-cheat assertion:
  agreement to 1e-15 means the deck being compared is not independent.
- `theRawfileHeaderIsStrippedBeforeComparison()` - asserts the parser rejects
  a rawfile whose `Date:` line survived, so the stripping cannot silently stop
  working.

`test/jls/analog/InvariantTest` (Tier C, no oracle, therefore **required** and
not nightly): `reciprocityHolds()`, `tellegenPowerBalances()`,
`chargeIsConserved()`, `superpositionHolds()`,
`solutionIsInvariantUnderNodeRenumbering()`. The last one is the strongest
single test in this task - it fails exactly when D-3's ordering discipline has
been violated somewhere the ArchUnit rule cannot see.

## Related GitHub issues

**no issue** for the analog oracle itself; `search_issues` over `anadon/jls`
for `spice OR analog` returns nothing open. Adjacent:

| # | title | relationship |
|---:|---|---|
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - open; owns the external-toolchain-in-CI surface this joins. Not closed by this task. |
| #265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps - open; governs which lanes are required. The analog oracle lane must stay advisory regardless, for the reason in Notes. |
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists | informs - open; established the `YosysLocator` PATH-preflight pattern this reuses. |

## Notes

- **This lane must never become a required gate, and that is a measurement not
  a preference.** Two ngspice builds on a two-element linear RC differed in
  66% of internal time points and 99.3% of sample values, worst relative
  difference 5.38e-04, with first divergence exactly at the pulse breakpoint -
  traceable to a documented "equalise the last two time steps before a
  breakpoint" change. One binary on one machine differed in 987 of 1,022 rows
  between `.options klu` and `.options sparse`. A required golden built on
  that would break on every ngspice release and would need a tolerance loose
  enough to hide real regressions.
- **Leave the oracle version unpinned, deliberately.** A pinned oracle rots; a
  loose oracle keeps working. Do not tighten the envelope below ~1e-8 without
  pinning, and do not pin.
- **The tolerance test and the byte-golden test are different jobs and neither
  substitutes for the other.** A byte-identical golden pins JLS against itself
  and says nothing about correctness; a tolerance test against a closed form
  says nothing about a regression at the 1e-12 level. Both are required layers;
  only one of them can involve ngspice.
- **Tolerance comparison is meaningful because the error propagation is
  linear, measured:** perturbing a rectifier's load by 1/2/8/1,000/100,000 ulps
  gave worst deviations of 2.118e-22 / 4.235e-22 / 1.694e-21 / 4.841e-14 /
  1.784e-11, exactly linear, with the accepted-step count unchanged at 5,785 in
  every case. **Three stated exceptions:** self-oscillators accumulate phase
  error forever (compare in the frequency or RMS domain), chaotic circuits make
  pointwise comparison meaningless at any tolerance, and a circuit at a
  bifurcation flips the accept/reject decision - which is what the grid-flip
  detector from TASK-0098 exists for.
- **`ProcessBuilder` stays out of `src/`.** There is not one in all of `src/`
  at HEAD (grepped this session); the single `Runtime.getRuntime()` call is
  `halt(code)` in the crash handler, not a subprocess. A `.jls` file must never
  cause an external binary to run.
- **Cost honesty.** 2 weeks is the corpus-and-lane slice; the stage is 7-11
  maintainer-weeks including the full 26-fixture build-out and the `jls.analog`
  JaCoCo/PIT floors. FEAT-046's band carries the remainder.

## Evidence

- Measured ngspice cross-version and cross-configuration divergence (3,056-row
  RC comparison; 987/1,022 klu-versus-sparse rows; rawfile `Date:` and
  `Command:` header lines): `11-analog-determination.md` §1.3.
- The four-tier golden discipline, the 1e-4 envelope derivation, per-family
  promotion after 20 green runs, and the R1-R5 detectors:
  `11-analog-determination.md` §4.2, §4.3.
- The corpus contents (Tier A 10, Tier B 11, Tier C 5) and the stage cost:
  `11-analog-determination.md` §5 stage S8.
- CI machinery this reuses, verified at HEAD: `.github/workflows/ci.yml:12`
  (nightly cron), `:24` (nightly concurrency group), `:73` (best-effort apt for
  `iverilog ghdl yosys xvfb` with a skip message), `:156,:263`
  (`continue-on-error` advisory jobs); `test/jls/hdl/ToolLocator.java:67`;
  `test/jls/hdl/GhdlCompileTest.java:34-35`;
  `test/jls/hdl/IverilogCompileTest.java:33-34`.
- `ProcessBuilder` 0 hits in `src/` and `DefaultExceptionHandler`'s
  `Runtime.getRuntime().halt(code)` being the only `Runtime` use (grepped this
  session).
