# TASK-0022 - Measure the per-cycle active fraction, CPI and the calibration constant

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A two-cycle unified-memory machine, committed as a tracked fixture, driven by
an internal `Clock`, event-counted with per-callback attribution — replacing
"never measured" with three numbers and their method.

1. **Settle the clocking regime first.** `docs/machine-calibration.md` §2.5
   (`:250-273`) records 388.4 events/cycle on the anchor workload, 245.5 under
   TestGen drive and 121.5 under an internal `Clock` on **element-for-element
   identical circuits**, with a fourth figure of 243.1 contradicting the 121.5
   inside its own source — a 3.2× spread on exactly the axis a boot is forced
   onto. §6.2 (`:868-880`) says to do this attribution **before** the α
   experiment "since α is measured through the same counter", and prices it in
   hours. Extend the counting simulator to attribute posts and reactions
   **per callback class**, re-run one circuit under both drives, and publish
   the per-class breakdown. That table is what explains or refutes the 2.02×.

2. **The fixture: a 2-cycle unified-memory machine.** The conversion §6.1
   (`:861-867`) specifies: merge instruction and data memory into one
   `Memory`, add an IR `Register`, a fetch-versus-data address `Mux`, a
   PC-hold `Mux`, and a 2-state sequencer — about ten new elements over the
   shipped single-cycle datapath. Commit it as `test/fixtures/`, which
   `.gitignore:9-10` exempts from the `*.jls` ignore, and as `-text`
   (`.gitattributes:1-5`).

3. **Use `RegisterFile`, not two mirrored `Memory`s.**
   `src/jls/elem/RegisterFile.java:21-28` is a first-class multi-port register
   file with independent read and write address ports (`:139-161`), landed
   under #201. The corpus's "9 elements versus 98" register-file design choice
   is **deleted** by its existence (`BRIEF.md` §13). A fixture that mirrored
   `Memory`s would measure a machine nobody would now draw, and its α would
   not transfer.

4. **Three numbers, each with its method.** α (per-cycle active fraction),
   CPI, and the calibration constant `k` in
   `ev_instr = k × α × L × CPI` (`docs/machine-calibration.md` §4.1,
   `:421-440`), reported with the element count `L`, the pass count, and the
   drive. Write them into §6.1 and §6.2 in place of "never measured" and
   "unexplained", and correct §4.2's consistency failure (the shipped
   468 ev/instr implying α = 0.155, below the band's own floor) or record that
   it survives the measurement.

5. **The counter ships as a test class, not a script.** It lives in `test/`
   and is re-runnable, so the numbers are reproducible after the measurement
   commit rather than only at the moment it was taken.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-009 | α is the feature's headline unknown: a 3.1× spread (0.18 / 0.40 / 0.56) that is the dominant input to every structural boot estimate in the plan. Until it is measured, "every wall-clock estimate divides by measured constants" is false. |

## Prerequisite tasks

None. The fixture is drawn or built by an in-tree path and the counter extends
an existing test class; nothing here reads output another task produces.

## Acceptance test

`test/jls/sim/EventAttributionTest.java`, new:

- `postsAreAttributedByCallbackClassUnderBothDrives()` — runs one circuit
  under a `SigSim`/TestGen vector and under an internal `Clock`, asserts the
  total post counts and the per-class breakdown, and asserts the ratio between
  the two drives against the measured value. This is the test that turns the
  2.02× from an anomaly into a pinned constant — or exposes it as a
  measurement artifact.

`test/jls/sim/ActiveFractionMeasurementTest.java`, new, `@Tag("longrun")` if it
exceeds the required lane's budget (TASK-0016 supplies the tag):

- `twoCycleMachineEventsPerClockCycleIsPinned()` — runs the committed fixture
  for a fixed number of clock cycles and asserts the reacted-event count is an
  **exact equality**, in the TASK-0026 budget idiom. A change to the engine
  that moves this number is what the test exists to catch.
- `cpiOverTheDirectedProgramMatchesTheDeclaredValue()` — asserts retired
  instructions and elapsed clock cycles over a directed program, so CPI is a
  measured quotient rather than an estimate.
- `theActiveFractionIsWithinTheRecordedBand()` — asserts α computed from the
  above falls in the band the measurement commit writes into
  `docs/machine-calibration.md`, so a later engine change that silently moves
  it fails here rather than in a boot estimate.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on — #202's sample circuit is the nearest thing to this fixture, and the two must not become two unrelated CPU fixtures |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs — #232 is the optimization these numbers price. Measuring first is the whole point of the ordering |

**No issue** for the measurement itself, for α, or for the calibration
fixture. Recorded in the registry as one of the explicit gaps.

## Notes

- **`Memory` posts a self-event on every inbound `PinChanged`.**
  `src/jls/elem/Memory.java:1384, 1396, 1402` each do
  `sim.post(new SimEvent(now+accessTime, this, …))`. Merging instruction and
  data memory into one `Memory` therefore changes events per cycle **by
  construction** — the merged element sees both address streams. Report the
  merged-memory event count as its own line or the effect will be attributed
  to α, which is precisely the error this experiment exists to avoid. The same
  mechanism is why a mirrored-`Memory` register file measured 18.00 ev/cycle
  against `RegisterFile`'s 6.94 (`BRIEF.md` §13).
- **`Memory` reads are asynchronous.** An address change posts a read at
  `now + accessTime` with no clock, which is why CPI adjudicated to ~2.9
  rather than 6.6 (`docs/machine-calibration.md` §3.3, `:359-380`): fetch is
  one cycle and a load is one cycle. A two-cycle machine must not accidentally
  re-introduce a synchronous read and then report the result as CPI.
- **A boot cannot use TestGen drive at all.** `SigSim.initSim` pre-posts every
  transition (`docs/machine-calibration.md:266-269`), so a multi-billion-cycle
  vector would have to fit in the queue before the first event fires. The
  fixture must be internally clocked, which is the regime measured *lowest*
  and *least reliably* — hence item 1 before item 2.
- **`riscv/` may not be a dependency.** D5 (`BRIEF.md` §0.1, §11) is binding:
  `riscv/build_cpu.py` and `riscv/jlsbuild.py` are being deleted and nothing
  may route through them. The fixture is committed as a file, or produced by
  an in-tree mechanism (TASK-0038's construction verbs), and never regenerated
  from `riscv/`. `riscv/build/k2000.jls` — the only CPU-scale performance
  anchor — is untracked by `riscv/.gitignore:1` and is TASK-0025's problem,
  not this task's.
- **Any figure derived from ns/node must state node count and pass count.**
  `BRIEF.md` §13 records two sibling agents getting this wrong by 4.6× and by
  presenting an underated ceiling as achievable. The measurement report states
  both, every time.
- **The existing counter is a nested test class, not an API.**
  `test/jls/SimulationSemanticsRegressionTest.java:292-309` — `CountingSimulator`
  overrides `post` and `pause` and counts. Per-callback attribution means
  keying a map on `event.getCallBack()`'s class; that is a small extension of
  the same idiom, and `src/jls/sim/Simulator.java:163` already names this class
  in an `@jls.testedby` tag, so moving it needs that tag updated.
- **Do not quote a boot wall clock from this task.** §2.5 forbids quoting one
  without naming the constant and the regime; this task supplies the constant,
  and TASK-0024 writes the document that uses it.

## Evidence

- `docs/machine-calibration.md:250-273` (§2.5, the 388.4 / 245.5 / 121.5 /
  243.1 spread and the internal-`Clock` requirement), `:421-440` (§4.1, the
  `ev_instr = k × α × L × CPI` formula and the table of inputs, α marked
  "**never measured**"), `:442-470` (§4.2, the two values of `k` and the
  α = 0.155 consistency failure), `:359-380` (§3.3, asynchronous `Memory`
  reads and the CPI adjudication), `:846-880` (§6, §6.1 the α experiment and
  §6.2 "Do this before the α experiment").
- `src/jls/elem/Memory.java:1384, 1396, 1402` — the three self-posts at
  `now + accessTime`; `:108` — `accessTime` default 100.
- `src/jls/elem/RegisterFile.java:21-28` (the #201 javadoc: "collapses the
  ~95 elements … into a single first-class element"), `:139-161` (independent
  `RA`/`WA` ports and the shared write clock).
- `src/jls/elem/Clock.java` — the internal clock element the fixture is driven
  by.
- `test/jls/SimulationSemanticsRegressionTest.java:292-309` — `CountingSimulator`;
  `src/jls/sim/Simulator.java:158-170` — `post`, the site it wraps.
- `.gitignore:8-10` (`test/fixtures/**/*.jls` exempted from the `*.jls`
  ignore), `.gitattributes:1-5` (`*.jls -text`).
- `riscv/.gitignore:1` — `build/`, which is why `k2000.jls` is untracked;
  `git ls-files riscv/` returns 26 tracked files at HEAD.
- `BRIEF.md` §13 — the `RegisterFile` correction (18.00 vs 6.94 vs 114.53
  ev/cycle), and the ns/node caution requiring node count and pass count.
