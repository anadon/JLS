# FEAT-009 - The measurement gate and a tracked calibration fixture

**Status:** proposed | **Cost:** 5-10 mw | **Owner program:** UNOWNED |
**Spine rank:** S15

## Capability delivered

Every wall-clock claim in the plan divides by a constant somebody measured, on a
fixture that is committed to the tree, under a standing ratchet that turns a
regression into a build failure. Today the only CPU-scale performance anchor in
the repository is untracked and lives in the directory decision D5 deletes, and
the largest remaining modeling uncertainty - the per-cycle active fraction - has
never been measured at all, with a 3.1x spread. This feature ships no product
code. It ships the numbers, their method, the fixture they were taken on, and
the gate that keeps them honest.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | Deleting `riscv/` is a maintenance deliverable and this is what unblocks it |
| CAP-02 | required | Every boot-duration figure the capstone quotes is owned here; without it the capstone's acceptance test has no budget |
| CAP-03 | required | Same, at ternary radix, where the element census is different and must be re-measured rather than scaled |
| CAP-08 | required | An imported core's cost is predicted from these constants |
| CAP-09 | required | A verification run's affordability is the whole question, and it is this arithmetic |
| CAP-17 | required | every capacity and speedup claim divides by the per-element footprint and per-event cost this feature measures |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-005 | Measuring per-event cost through a quadratic stimulus parse measures the parse. The 80%-of-wall-clock defect must be gone before the numbers mean what they say |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0022 | Measure the per-cycle active fraction, CPI and the calibration constant | The single cheapest experiment that collapses the largest uncertainty; all three fall out of one instrumented run |
| TASK-0023 | Measure the behavioral binding and the levelized cost at scale | Shared with FEAT-030 and FEAT-031: supplies the node count and pass count every derived speed claim must state |
| TASK-0024 | Write the machine-calibration document | The durable record; the document exists at HEAD and this task fills the section the measurements were missing |
| TASK-0025 | Commit the tracked calibration fixture, re-home the goldens, delete `riscv/` | The deliverable that discharges D5 |
| TASK-0026 | The simulation budget and allocation ratchet | Shared with FEAT-030: the standing gate that makes a regression a build failure |
| TASK-0016 | Split CI into a required fast lane and a long-run lane, with a fixture policy | Shared with FEAT-007: where a large fixture lives is the same decision as how CI hosts it |

## Acceptance criteria

1. The per-cycle active fraction, CPI and events-per-instruction are measured -
   not estimated - on a committed fixture, each with its method and its
   workload recorded alongside the number.
2. A CPU-scale calibration fixture is **tracked in the repository**. The
   untracked anchor under `riscv/build/` is no longer referenced by anything.
3. `riscv/` is deleted, and no deliverable, recommendation or dependency routes
   through `riscv/build_cpu.py` or `riscv/jlsbuild.py`. Whatever replaced them
   is a first-class, in-tree, tested JLS mechanism.
4. Events per clock cycle on the committed fixtures is asserted as a hard
   **equality**; nanoseconds per event and bytes allocated per event are
   asserted against declared bands that ratchet down and never up.
5. A gate asserts the entire existing golden corpus is byte-identical across any
   engine change. This is the gate FEAT-030 operates under and it belongs here.
6. Every published figure states its clocking regime, and every ns/node figure
   states node count and pass count, or it is not published.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | closes - the in-tree worked example and its golden are exactly TASK-0025's re-homing |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - #232 is a hypothesis this feature's measurements confirm or refute before FEAT-030 spends on it |
| - | the calibration fixture that blocks deleting `riscv/`, and the budget ratchet | **no issue** |

## Design notes

**Two claims in the evidence corpus are stale and must not be repeated.**
First, `BRIEF.md` §7 calls the golden oracle "gitignored, never run by CI". At
HEAD the RV32I golden is **tracked**: `git ls-files` lists
`test/fixtures/riscv-sum1to10.jls` and `test/jls/RiscvCpuGoldenTest.java` runs
it, asserting four architectural values (`:83-87`) over the 34 cycles the
program needs (`:28`). What survives of the criticism is its *size*: 34 cycles
and a handful of assertions is a functional smoke test, not a performance
anchor. Second, `docs/machine-calibration.md` already exists at HEAD as an
"evidence record" with its own quoting rules; TASK-0024 is therefore filling and
correcting it, not creating it. Its §6 is the section the L0 measurements
replace (`docs/virtual-hardware-parity.md:491`).

What is genuinely missing is the CPU-scale anchor. `riscv/.gitignore:1` ignores
`build/`, so `riscv/build/k2000.jls` - referenced by six documents under
`docs/` - is untracked, unreproducible from the tree, and about to be deleted
along with its generator. TASK-0025 is the only thing standing between D5 and
losing the measurement basis of the whole plan.

The measurement targets, with their current state:

- Warm event loop: **3.14 M events/s, 318 ns/event**, of which queue plus
  duplicate-check bookkeeping is 151.8 ns (47.7%). The older 2.0-2.6 M figure is
  the *including-`initSimulation`* number and must always be labeled.
- **386-409 reacted events per clock cycle** for the shipped 228-logic-element
  single-cycle RV32I, from four independent instrumented counters; about 1.8
  events per active logic element per cycle.
- Throughput is nearly flat in circuit size, R ~ L^-0.12 - element count enters
  boot time once, via events per instruction, not twice.
- **The per-cycle active fraction has never been measured**, with a 3.1x spread.
  This is the dominant uncertainty in the entire model and TASK-0022 is the
  experiment that settles it: convert the shipped single-cycle demo into a
  two-cycle unified-memory machine (about ten new elements) and count events
  with an internal `Clock`. One experiment, three constants.
- The **2.02x TestGen-versus-Clock events-per-cycle discrepancy** (245.5 versus
  121.5 on element-for-element identical circuits) is unexplained and must be
  explained or bounded, not averaged away.

Two derived-figure disciplines are non-negotiable because both were already
violated in this corpus: an events-per-cycle figure without its clocking regime
is not a measurement, and a ns/node figure without node count and pass count is
not a measurement. Two sibling agents misread the levelized figure by 4.6x by
counting logic elements as nodes and one pass instead of two.

## Risks

- **D5 and this feature are in a race.** If `riscv/` is deleted before TASK-0025
  lands, the plan loses its only CPU-scale anchor and the arithmetic in six
  documents becomes unreproducible. The ordering constraint is hard: fixture
  first, deletion second, in that order, in TASK-0025.
- **A large tracked fixture collides with the large-fixture policy that does not
  exist.** A CPU-scale `.jls` is tens of megabytes at 15.87 bytes per word.
  TASK-0016 is shared for exactly this reason and must land with it.
- **Measuring the active fraction may move every downstream estimate.** That is
  the point, and the plan should expect the boot-duration bands to change rather
  than treating a change as a failure of the measurement.
- **UNOWNED at 5-10 mw, and it gates FEAT-030's 12-20 mw.** An unmeasured
  speedup program is arithmetic on a guess; this is the cheapest possible
  insurance on the most expensive item in the core column.

## Evidence

- The tracked golden and its scope: `git ls-files test/fixtures/` lists
  `test/fixtures/riscv-sum1to10.jls`; `test/jls/RiscvCpuGoldenTest.java:28`
  (34 cycles), `:83-87` (four architectural assertions), `:43` (fixture path).
- The untracked CPU-scale anchor: `riscv/.gitignore:1` ignores `build/`;
  `riscv/build/k2000.jls` is referenced by `docs/virtual-hardware-parity.md`,
  `docs/machine-calibration.md`, `docs/capability-roadmap/keystone-c-performance.md`,
  `docs/capability-roadmap/lf-03-causal-debug.md`,
  `docs/capability-roadmap/lf-07-api-and-platform.md` and
  `docs/capability-roadmap/AMENDMENT.md`.
- The existing evidence record and its quoting rules:
  `docs/machine-calibration.md` §2.5, §2.6, §4.5, §4.6, §7.3; its §6 is what L0
  fills (`docs/virtual-hardware-parity.md:418-491`).
- Engine constants: `BRIEF.md` §2 and §13;
  `docs/capability-roadmap/keystone-c-performance.md:126,136`.
- The unmeasured active fraction and the 2.02x discrepancy: `BRIEF.md` §10.
- The one experiment that settles three constants: `BRIEF.md` §10, final
  paragraph.
- The 4.6x derived-figure error made twice: `BRIEF.md` §13.
- D5 (`riscv/` will be deleted): `BRIEF.md` §11.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S15.
- **Cost reconciliation.** Band 5-10 mw. Tasks named for it: TASK-0016,
  TASK-0022, TASK-0023, TASK-0024, TASK-0025, TASK-0026, totalling 8.5 wk.
  Band and task sum agree; no reconciliation is needed. Shared tasks counted
  once at the task level: TASK-0016, TASK-0023, TASK-0026.
