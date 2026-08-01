# CAP-14 - ngspice interoperation parity

**Status:** proposed | **Priority:** 14 | **Marginal cost:** 10-16 mw |
**Standalone cost:** 37.5-55.5 mw (cumulative through the analog stage this
capstone sits on)

## Outcome

JLS emits a SPICE deck real ngspice runs, and JLS's own analog results and
ngspice's agree to a declared tolerance on a fixture corpus, nightly - so the
claim "the JLS analog solver is correct" stops resting on JLS's own tests.

## Acceptance test

**What "parity" means here.** Not dialect completeness with ngspice, and not
ngspice embedded in JLS. Parity is three checkable claims:

1. **Accepted.** A deck JLS writes runs to completion under `ngspice -b` with no
   hand editing.
2. **Agreement.** For every Tier A (closed-form) and Tier B (convergence-torture)
   fixture, JLS's own solution and ngspice's agree within a **declared 1e-4
   relative envelope**, computed by a comparator in the repository, on a nightly
   non-gating lane that skips itself when the tool is absent.
3. **Bounded, and not too good.** Disagreement carries a **lower bound** - an
   oracle agreeing to 1e-15 is not independent - and a regression that stays
   inside the envelope is still detectable, because solver statistics and the
   measured analytic error are pinned in the golden header and ratcheted.

SEEN: a maintainer opens the nightly matrix and sees every fixture green, with
the per-fixture relative deviation printed; a student runs
`jls -export design.cir design.jls`, runs `ngspice -b design.cir`, and gets the
same waveform they saw in JLS.

CHECK: four named tests.
- `SpiceDeckGoldenTest` - byte-identical deck emission for the fixture corpus,
  no date or command line in the output, positional pin order frozen.
- `NgspiceOracleTest` - claim 2. Nightly, `continue-on-error`, self-skipping via
  the shipped locator idiom (`test/jls/hdl/GhdlCompileTest.java:34-36`), promoted
  to required per device family only after a stated number of green runs.
- `SolverStatisticsRatchetTest` - claim 3. Newton iterations, accepted and
  rejected steps and the measured analytic error are equalities in the golden
  header, so a 20% convergence regression that keeps the waveform inside 1e-6 is
  still a failure.
- `AnalogDeterminismTest` - the same deck produces byte-identical JLS results on
  every supported platform and runtime, which is the property no external
  floating-point simulator can supply and which the whole claim rests on.

## Demo slice

Two parts, and the first exists today.

- **The digital half, now, at documentation cost.** JLS already emits Verilog;
  `yosys write_spice` turns that into a SPICE netlist real ngspice reads. A
  written, tested recipe converts a costed rejection into a shipped capability
  for **0.25-0.5 mw** and buys claim 1 for digital designs with no JLS code.
- **The analog half, minimum honest version.** One linear RC and one RLC
  fixture through the deck emitter, `ngspice -b`, an ASCII parser, the 1e-4
  comparator and the self-skipping nightly lane: **2-3 mw** once a linear solve
  exists. That is claims 1-3 on two circuits, and the corpus grows from there.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-046 | The analog solver core and its determinism gate | there is nothing to compare against ngspice until JLS computes an analog answer of its own; the determinism gate is claim 3 | required |
| FEAT-049 | Analog device models, the drawn palette and convergence hardening | the corpus is device-shaped - diodes, op-amps, transistors - and a fixture set of two linear circuits is not a parity claim | required |
| FEAT-047 | The physical time base and the nominal real-time scalar | a SPICE deck's `.tran` arguments are seconds; a dimensionless integer cannot be emitted as one honestly | required |
| FEAT-048 | A2D/D2A bridge elements and A-STEP synchronization | a mixed-signal fixture is the only kind JLS has that ngspice does not, and the boundary must be defined before it can be compared | required |
| FEAT-004 | Shared net-partition IR with stable net naming | node names in the deck must be a function of stable id, or every regenerated golden is a diff | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | the oracle is nightly and non-gating by design, and the cross-platform determinism leg is a required job | required |
| FEAT-001 | Registry-keyed table totality discipline | a deck emitter is another registry-keyed policy; a non-total one silently omits devices from the netlist | required |
| FEAT-019 | Yosys JSON write | the digital-to-SPICE route runs through Yosys's own backend rather than a second lowering pass inside JLS | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) CAP-14 has no tracking issue | no issue |
| - | (no issue) the entire analog program, FEAT-045 through FEAT-049 | no issue |
| #265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | depends on - the cross-platform determinism leg this capstone's claim 3 rests on is that issue's substrate |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | informs - the digital-half demo slice rides the emitters that issue staged |

## Open decisions

1. **Port the numerics, or shell out to ngspice?** *Recommendation: port, and
   keep real ngspice as a maintainer-side, non-shipping, self-skipping CI
   oracle.* Reason: `11-analog-determination.md` records the measurement that
   decides it - a shared library breaks the single offline jar, its code models
   are separate on-disk objects referenced by filesystem path, and a native
   `abort()` in one of them kills the editor with the student's unsaved circuit.
   The port and the validation are different jobs and neither substitutes.
2. **What is the required gate and what is the nightly lane?**
   *Recommendation: JLS's own goldens gate; ngspice never gates.* Reason: two
   ngspice builds were measured to disagree by 5.38e-4 on a two-element linear
   circuit, and one version's two solver options differ on hundreds of points -
   a required golden built on an external solver breaks on the oracle's release
   schedule, not on JLS's changes.
3. **Pin the oracle version, or leave it loose?** *Recommendation: loose, and do
   not tighten the envelope below ~1e-8 without pinning.* Reason: a pinned
   oracle rots into a golden; a loose one keeps testing. The 1e-4 envelope is
   deliberately two decades above the oracle's own build noise and two decades
   below real model error, and that gap is what makes it meaningful.
4. **Which SPICE dialect is the target?** *Recommendation: the Spice3f5-era model
   cards plus ngspice spellings, documented as the target, not ngspice-current.*
   Reason: the measured parameter counts differ by more than a factor of seven
   on a single device card; chasing the current dialect is unbounded and the
   teaching subset is not.
5. **Does this reopen the recorded refusal of device-level SPICE?**
   *Recommendation: yes, explicitly, and record why.* Reason: that refusal is
   semantic - a SPICE deck's leaves are devices and a JLS element is a logic
   function - and it is correct **for a JLS that has no electrical content**.
   This capstone is the decision to acquire that content. The refusal is
   superseded by funding FEAT-046 and FEAT-049, not by ignoring it, and the
   refusal stands unchanged for any JLS build that does not.

## Kill criteria

- K1. If the envelope must be loosened above 1e-3 to keep the lane green, the
  oracle no longer distinguishes a correct solver from an incorrect one - real
  model error is that size - and the lane should be deleted rather than
  weakened.
- K2. If keeping the lane green requires pinning one exact oracle build, claim 2
  has degenerated into a second golden and must be re-stated as such.
- K3. If the anti-cheat lower bound on disagreement is ever violated - agreement
  at or below the oracle's own build-to-build noise - the comparator is
  comparing JLS against itself and the result is not evidence.
- K4. If more than a stated fraction of the convergence-torture corpus cannot be
  made to converge at all after the hardening feature lands, the solver is not
  ready for homework-grade circuits and this capstone is blocked behind the
  hardening rather than merely late.
- K5. If the deck emitter's positional pin order is not frozen and test-pinned
  before the first device family is promoted, stop: a mis-ordered device line
  parses, simulates and silently produces a different circuit.

## Evidence

- Verified at `b54e6ee`: `grep -rli spice src/` returns **0**; there is no
  analog anything at HEAD. `grep -rl ProcessBuilder src/` returns **0** while
  `test/` returns **15** - the shipped discipline is that external tools live in
  test scope only, and this capstone must not change that.
- The determinism substrate: `src/jls/hdl/board/PcfEmitter.java` (199 lines) and
  the opt-in external-tool idiom at `test/jls/hdl/GhdlCompileTest.java:34-36`
  are the two shipped precedents this capstone's emitter and oracle copy.
- The oracle-drift measurements, the 1e-4 envelope and its derivation, the
  five in-tolerance regression detectors, the Tier A/B/C corpus shape and the
  S8 cost band: `11-analog-determination.md` §1.2, §4.2, §4.3, §5.1 S8.
- The digital-half route and its documentation cost:
  `09-format-adoption-plan.md` §4.1 and W0.5; the semantic refusal this capstone
  reopens is §4.4 and §7.
- Do not restate: `docs/simulation-semantics.md` owns JLS's value and time
  domain today, `docs/parity-contract.md` owns what may and may not differ
  across two implementations of one machine, `docs/machine-calibration.md` owns
  measured constants, `docs/capability-roadmap/` owns program boundaries.
