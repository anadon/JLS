# TASK-0098 - The analog determinism controls

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0097

## Deliverable

The five controls beyond strict floating point that make `jls.analog` produce
byte-identical results across platforms and runtimes, each one a test rather
than a convention, plus the CI matrix that proves it and the document that
states the rules.

Precisely what changes:

- `test/jls/analog/AnalogDeterminismRulesTest` - ArchUnit rules in the
  `ArchitectureRulesTest` idiom, one test method per control:
  - **D-1** `jls.analog` may not reference `java.lang.Math` except `sqrt` and
    `abs`; `StrictMath` only. Starts clean (`grep -rn StrictMath src/` = 0 at
    HEAD).
  - **D-2** no parallelism inside the solve: no `parallelStream`, no
    `ForkJoinPool`, no `Thread` construction, no `Arrays.parallelSort`.
    Floating-point addition is not associative.
  - **D-3** no hash-ordered iteration reaching the matrix: `jls.analog` may not
    construct `HashSet` or `HashMap`, and the elaborator's device array is
    built in stable-id order. `Circuit.elements` is a plain `HashSet`
    (`src/jls/Circuit.java:48`), so the elaborator must sort.
  - **D-4** the pivot tie-break is total and asserted on the permutation
    vector (the test TASK-0097 introduces; this task ratchets it).
  - **D-5** every adaptive decision is a pure function of solver state: no
    `System.currentTimeMillis`, no `System.nanoTime`, no `java.util.Random`,
    no `SecureRandom`, in the `SocketConfinementRatchetTest` source-scanning
    idiom (`test/jls/SocketConfinementRatchetTest.java:35-40`).
- `JLS_REPLICA_ID` pinned in `.github/workflows/ci.yml` and in the analog test
  base class. `ElementId` resolves its replica from `jls.replicaId` /
  `JLS_REPLICA_ID` and otherwise draws a `SecureRandom`-backed hex value
  (`src/jls/elem/ElementId.java:42-56,210`), and stable-id order **is** the
  floating-point accumulation order. This is benign today only because every
  committed fixture carries `legacy:N` ids whose `'l'` sorts after every hex
  digit; it stops being benign the moment a fresh-minted id enters an analog
  fixture.
- `jls/analog/GoldenWriter.java` and `test/jls/analog/GoldenReader.java` - the
  writer in `src/`, an **independent spec-derived reader** in `test/`, in the
  `VcdExportGoldenTest` idiom, so a bug in the writer cannot hide behind the
  same bug in the reader. Hex-float and digest forms only; `Double.toString`
  is banned in goldens (its output changed in JDK 19); `-0.0` is canonicalized.
- `test/jls/analog/GridFlipComparator.java` - compares header statistics
  first; if `steps`/`rejects` match, pointwise compare; if they differ,
  resample both onto the fixture's declared `tstep` and **report the flip as a
  distinct outcome, neither pass nor fail**.
- `docs/analog-determinism.md` - D-1 through D-5, the two inherited hazards
  above, the golden regeneration protocol, and the R1/R4 statistics-and-error
  ratchet.
- CI: an analog-goldens job across the platform legs the workflow already has
  (`build` on Linux with its JDK matrix, `windows`, `macos` -
  `.github/workflows/ci.yml:28,143,259`) asserting identical digests.

Done means: the same `.tran` fixture produces the same bytes on Linux, macOS
and Windows across the JDK matrix, and every one of D-1 through D-5 fails the
build if violated.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-046 | The determinism gate - the half of FEAT-046 without which the solver has no witness and the byte-golden culture cannot absorb it. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0097 | Every rule here is asserted against `jls.analog` code and the pivot permutation, and the cross-platform job compares digests emitted by the solver. There is nothing to pin before the solver exists. |

## Acceptance test

`test/jls/analog/AnalogDeterminismRulesTest` (the five rule methods above,
each named for its control: `onlyStrictMathInsideTheAnalogPackage()`,
`noParallelismInsideTheSolve()`, `noHashOrderedIterationReachesTheMatrix()`,
`thePivotTieBreakIsTotalAndPinned()`, `noWallClockOrRandomInAdaptiveDecisions()`).

Plus the two that make it more than a lint:

- `test/jls/analog/CrossRuntimeDigestTest.theRectifierDigestIsTheCommittedConstant()`
  - asserts the emitted digest equals a committed constant. Run on every CI
  leg, this is the cross-platform assertion; run locally it is a regression
  assertion. One test, two jobs.
- `test/jls/analog/GridFlipComparatorTest.aStepGridChangeIsReportedAsAFlipAndNotAsAFailure()`
  - constructs two runs with differing `steps` counts and asserts the
  comparator returns the flip outcome rather than a boolean.

## Related GitHub issues

**no issue.** `search_issues` over `anadon/jls` for `analog OR spice OR solver`
returns nothing open. Adjacent:

| # | title | relationship |
|---:|---|---|
| #265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | depends on - the cross-platform digest job is only meaningful as a **required** check, and #265 is what makes the macOS and Windows legs required. Until it lands, this task's matrix job is advisory. This task does not close it. |
| #111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | depends on - same reason, Windows leg. |

## Notes

- **The go/no-go, and it must be scheduled early.** If byte-identical results
  do not hold across platforms, the entire justification for porting the
  numerics into Java rather than orchestrating an external simulator collapses,
  and the program should change shape. That question is falsifiable here, for
  two weeks, rather than in month ten. Treat a red matrix as information, not
  as a bug to be worked around by loosening the comparison.
- **`strictfp` is a no-op and that is not the same as determinism.** JEP 306
  made all FP arithmetic strict from JDK 17 and the build pins release 25, so
  no extended precision, no permitted reassociation and no FMA contraction are
  already in force. What is *not* in force is anything about summation order,
  pivot order or node numbering - none of which is a `Math` call. A rule that
  only bans `java.lang.Math` would give false confidence; the gate must test
  the matrix path.
- **`Double.toString` is the quiet trap.** Its output changed in JDK 19. A
  golden written with it is a JDK-version golden wearing a value golden's
  clothes. Use `Double.toHexString` or a digest over `doubleToLongBits`.
- **There are no `timeout-minutes` on any job in any workflow at HEAD**
  (grepped across `.github/workflows/*.yml`, zero hits). Adding a cross-
  platform analog job to an untimed workflow inherits that gap; TASK-0015 owns
  the fix, and this task should not land a new job without one.
- **Cost honesty.** 2 weeks is the controls-plus-matrix slice; the stage is
  4-6 maintainer-weeks including `docs/analog-determinism.md` and the full
  4-platform x 2-JDK wiring. FEAT-046's band carries the remainder.

## Evidence

- The five controls, the two inherited hazards (`JLS_REPLICA_ID` and
  `Double.toString`), the absorbed-code audit list, and the grid-flip rule:
  `11-analog-determination.md` §4.1, §4.3.
- Measured basis for expecting success: one Java kernel produced an identical
  digest across JDK 25 (twice), `-Xint`, `-XX:TieredStopAtLevel=1`,
  `-XX:-UseFMA -XX:UseAVX=0 -XX:UseSSE=2`, `-XX:+UseSerialGC -Xmx32m` and
  JDK 21 - the FMA and AVX rows being the two mechanisms by which x86-64 and
  aarch64 results would diverge (`11-analog-determination.md` §1.3).
- HEAD facts: `src/jls/elem/ElementId.java:42-56` (replica resolution order,
  `SecureRandom`-backed default), `:210` (`mintFresh`);
  `src/jls/Circuit.java:48` (`HashSet` elements); `StrictMath` 0 hits in
  `src/`; `.github/workflows/ci.yml:28,143,259` (the three platform jobs) and
  zero `timeout-minutes` across all six workflow files.
- Existing ratchet idioms this copies: `test/jls/SocketConfinementRatchetTest.java:35-40`
  (source-scanning confinement), `test/jls/ArchitectureRulesTest.java:69-75,124-132`
  (ArchUnit layering rules), `test/jls/VcdExportGoldenTest.java` (independent
  spec-derived reader).
