# TASK-0070 - The machine package and its reference runner

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A pure leaf package holding every line of architectural logic, born under the
strong coverage bar, containing an independent implementation of the machine
usable as the parity counterparty.

1. **`src/jls/mach/` and `test/jls/mach/`**, with `package-info.java` carrying
   `@NullMarked`. Zero AWT, zero Swing, zero `jls.edit`, **zero `jls.sim` and
   zero `jls.elem`** - unit-testable with no simulator and no circuit.

2. **`ArchState`** - record-shaped, `int`/`long` only, no `BitSet`: `pc`, the
   GPR file as `int[32]`, `privilege`, and the implemented CSR set. Immutable;
   `step` returns a new one.

3. **`MemoryView`** - a small interface with **two indistinguishable
   implementations**: `ArrayMemoryView` for the reference runner and, later
   (TASK-0079), a bus-backed one for the fidelity binding. That pair is what
   makes the wired core and the reference runner *provably the same code*
   rather than two hand-written machines that happen to agree.

4. **A data-only decode table** and a pure
   `static StepResult step(ArchState, MemoryView)`. `StepResult` carries the
   next `ArchState` and the retirement fields; the record type itself is
   TASK-0072's (`jls.parity.RetireRecord`), and `jls.mach` depends on
   `jls.parity` only for that leaf record - never the other way.

5. **`jls.mach.dev.Uart16550Model`** - the three-address subset
   (`docs/machine-calibration.md` §5.3), and **`jls.mach.dev.ClintModel`** with
   `mtime` driven by **simulated** time only. This is not a style preference:
   the same document measures the counter-example - `mtime` on host wall clock
   plus a human taking 8 s to type `root` ran the instruction counter to
   1.5 x 10^10, all of it idle spin.

6. **`jls.mach.Runner`** - a headless loop over `step` with a declared
   termination condition, emitting a retirement trace through TASK-0072's
   writer. This is the parity harness's counterparty and the thing TASK-0071's
   guest image is booted on.

7. **Governance in the same commit that creates the package**, because that list
   is the only thing policing it:
   - `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES`
     (`test/jls/HeadlessCoreRatchetTest.java:74-79`) gains `"src/jls/mach/"` -
     it takes **path** prefixes with a trailing slash.
   - `pom.xml` gains a `jls.mach` PACKAGE rule at the `jls.sim` bar,
     0.930/0.920/0.845, modeled on `pom.xml:449-471`.
   - `pom.xml`'s pitest `targetClasses` (`:770-785`) gains `jls.mach.*`, so the
     80/82 mutation and test-strength thresholds apply from birth.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-033 | The package, the state model and the runner are three of the feature's four parts; the fourth is the guest image (TASK-0071). |
| FEAT-034 | The harness compares two implementations. This is the second one, and it is the one whose correctness is testable without a circuit. |

## Prerequisite tasks

None. This package depends on nothing in JLS by construction - that is what
"pure leaf" means and it is why the task can start in parallel with everything
else in FEAT-032 and FEAT-035.

## Acceptance test

`test/jls/mach/StepFunctionTest`:

- `stepDoesNotMutateItsInputState()` - step the same `ArchState` twice and
  assert both results are equal and the input is unchanged. Purity is what makes
  bisection and checkpointing free later.
- `theTwoMemoryViewsAreIndistinguishable()` - run the same program under
  `ArrayMemoryView` and under a deliberately differently-shaped view (chunked,
  different internal layout) and assert the `ArchState` sequences are equal
  element for element. This is the test that keeps the bus-backed view honest
  when TASK-0079 adds it.
- `clintMtimeIsDrivenBySimulatedTimeOnly()` - a source scan asserting
  `src/jls/mach/**` contains no `System.currentTimeMillis`, `System.nanoTime`,
  `Instant.now` or `Clock.systemUTC`. Zero-tolerance from a clean baseline, in
  the `SocketConfinementRatchetTest` idiom.
- `theUartModelMatchesTheMeasuredDecode()` - `@ParameterizedTest` over the
  offsets in `docs/machine-calibration.md` §5.3, including the all-ones LSR
  refusal.

`test/jls/HeadlessCoreRatchetTest#coreCandidatesGainNoForbiddenImports()` must
be green **on the same commit** with `src/jls/mach/` in the prefix set and
nothing added to `BASELINE` (`test/jls/HeadlessCoreRatchetTest.java:90`, which
is `Set.of()`).

The JaCoCo `jls.mach` rule must pass on the same commit. Plan the tests before
the code: a package that lands under the bar and gets its rule "next sprint" is
how the weak floors elsewhere in the tree happened.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | `jls.mach`, the reference runner and the guest software stack | **no issue** |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - the worked example's oracle is this runner; today the issue has no oracle other than the drawn circuit itself |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - `ArchState` is `int`/`long` by construction, which is what #232 is arguing for on the element side; the two must not be conflated, this package is not on the event loop at all |

## Notes

- **There is no `module-info.java` anywhere in `src/`** (verified: zero files).
  A permitted subclass of a sealed type must therefore sit in its parent's
  package. That is precisely why the ISA logic must **not** live behind an
  element: putting ~3,000 lines of architectural logic in `jls.elem` files it
  under 0.730/0.700/0.585 (`pom.xml:475-493`) instead of 0.930/0.920/0.845
  (`:449-471`). The package boundary is the coverage decision.
- **`riscv/` is scheduled for deletion** by TASK-0025. Nothing here may be homed
  under it, and the salvaged reference-runner design from `riscv/riscv_ref.py`
  is input to this task, not a dependency of it.
- **The counterparty problem is real and is recorded as a known weakness.** Two
  models written by the same author can be wrong together
  (`docs/parity-contract.md` §9.3). This task ships the second model; the third
  object - an external reference and its home - is TASK-0073's problem, and the
  cheap resolution is committed test ELFs plus reference signatures in the
  blocking lane with the toolchain-bearing run informational-nightly.
- **`ArchitectureRulesTest#nothingUsesJavaObjectSerializationStreams`**
  (`test/jls/ArchitectureRulesTest.java:201-212`) is zero-tolerance repo-wide.
  `ArchState` serialization, if any, is textual.
- **Sync point zero.** JLS supplies a reset the design does not have, so two
  machines can agree on every record from instruction 1 and disagree at
  instruction 0. `ArchState`'s constructor must state the power-on value of
  every architecturally visible register, and that statement is part of the
  machine definition, not of the runner.
- Scope this at RV32IMA nommu. Sv32/OpenSBI is a hedge with a separate cost
  band and a third guest artifact; do not build for it speculatively.

## Evidence

- `find src -name module-info.java` returns zero files (verified at HEAD).
- `pom.xml:449-471` - the `jls.sim` package rule (0.930/0.920/0.845);
  `:475-493` - `jls.elem` (0.730/0.700/0.585); `:770-785` - pitest
  `targetClasses`; `:813-814` - `mutationThreshold` 80 / `testStrengthThreshold`
  82.
- `test/jls/HeadlessCoreRatchetTest.java:74-79` (path prefixes), `:90` (empty
  baseline), `:93` (the test).
- `test/jls/ArchitectureRulesTest.java:201-212` - the serialization ban.
- `docs/machine-calibration.md` §5.3 - the minimum SoC: one UART, one CLINT, one
  syscon, no PLIC; the CLINT's two registers; `msip` unimplemented and Linux
  still boots. §5.4 - the simulated-time requirement and the 1.5 x 10^10
  counter-example, measured.
- `docs/parity-contract.md` §9.3 - both implementations can be wrong together.
- `docs/virtual-hardware-parity.md` L5 - the forced-by-a-verified-constraint
  argument for a separate package, and the "added to
  `CORE_PACKAGE_PREFIXES` in the same commit that creates it" rule.
