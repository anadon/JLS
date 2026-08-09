# Issue #335: FEAT-009: every wall-clock claim in the plan divides by a measured constant, taken on a tracked fixture, under a ratchet that turns a regression into a build failure
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the tier machinery and #335 is one sentence: *JLS's performance claims live in
documents whose evidence lived in a scratchpad, on a fixture that was never tracked, and
nothing stops them rotting.* That is real and it is load-bearing. `ARCHITECTURE.md:354-358`
records the single most consequential open architectural decision in the project — the
event-queue interpreter is the *sole* strategy, revisit trigger "a CPU-scale design on the
`riscv/` trajectory that is unusably slow interactively." That trigger is unfalsifiable as
written, and #335 is the issue that would make it a number. Six capstones, #232, #362 and
#370 all divide by constants nobody can currently reproduce. The *goal* is correct and I
endorse it without reservation.

The *route* is where I disagree, and the tree disagrees with it harder than I do.

## Three facts from the tree that change the shape of the problem

**1. The anchor is already tracked.** `test/fixtures/riscv-sum1to10.jls` (120 KB, tracked,
run by `test/jls/RiscvCpuGoldenTest.java`) has the **element-for-element identical census**
to keystone C's "untracked" k2000 anchor: Mux 43, Constant 43, Splitter 34, AndGate 34,
Register 32, Binder 9, NotGate 8, XorGate 5, Extend 5, Adder 4, ShiftRegister 3, OrGate 3,
Memory 3, Decoder 1, InputPin 1, 810 WireEnds — compare
`docs/capability-roadmap/keystone-c-performance.md:105-108`. It is the same circuit. What
`riscv/build/k2000.jls` had that the tracked file lacks is *a longer program in the
instruction ROM and a longer clock vector* — and `RiscvCpuGoldenTest.clockVector()` already
**generates** its vector in a `StringBuilder` loop (`:50-57`). The difference between a
34-cycle smoke test and a 6004-cycle performance anchor is an integer.

This dissolves Open Questions 1 and 2 outright. There is no tens-of-megabyte fixture. There
is no LFS sidecar decision. TASK-0025's 2-week "commit the CPU-scale fixture" is a
parameterized `@ParameterizedTest` and a few hundred extra ROM words.

**2. Option (c) was rejected on a false premise.** Open Question 1 rejects "generated at
test time from a committed generator" because "a generator is exactly what D5 deletes." The
objection to `riscv/build_cpu.py` is that it is *untracked-output, out-of-tree Python with
no test* — not that generation is bad. The tree's own idiom is the opposite:
`BatchSimulationGoldenTest`'s inner `CircuitBuilder` (`:31-45`) emits save-format text from
Java, in-tree, under test. A tracked Java generator gives you k500/k1000/k2000/k8000 for
free, diffs in review, scales when the engine gets faster, and never bloats a clone. A
committed 30 MB blob gives you one point forever. This is the single decision in the issue
most likely to be wrong, and it is the one blocking TASK-0025.

**3. The disease is not "unmeasured constants" — it is that JLS has no home for an
instrument.** `keystone-c-performance.md:801-869` lists ten harnesses — `KernelProbe`,
`KernelProbe2`, `Census`, `NoDedup`, `Levelized`, `Kernels`… — under a
`/tmp/.../scratchpad/bench/` path. None of them exist in `src/` or `test/`. Neither does
`docs/machine-calibration.md`, on this checkout. The numbers did not rot because nobody
declared a band; they rot because the *measuring device was disposable*. #335 proposes to
re-take the measurements and then guard the numbers. That guards the output and leaves the
instrument in the scratchpad again.

## Reframe A (the main one): make the simulator self-measuring, then delete the constants

Do not build a calibration program. Build **one product-side surface** and let every
consumer read it.

`Simulator` already exposes `beforeEvent`/`beforeReact`/`afterEvent` no-op hooks
(`src/jls/sim/Simulator.java:255-270`), already overridden by `BatchSimulator:140`. The
issue itself quotes "Zero changes to `jls.sim` are required." So add a `-stats` flag to the
`FLAGS` table (`src/jls/JLSStart.java:759-789`) that makes **any** batch run print its own
census and event accounting to stderr: elements/nets/max-width, events fired/posted/
dup-suppressed, per-payload and per-callback breakdown, max queue depth, cycles, wall time.

That one flag delivers, as a side effect and with no separate fixture:

- **This issue's constants, as observables rather than model parameters.** α, k, L and CPI
  exist in `E_instr = k·α·L·CPI` only so you can *predict* events-per-instruction for a
  circuit you have not run. JLS can run the circuit. Measure `events/cycle` and `cycles/s`
  directly and the whole four-parameter model — with its 3.1x α spread and its 1.07-vs-1.8
  k split — becomes unnecessary rather than measured. **The 2.02x TestGen-versus-`Clock`
  discrepancy (§5 criterion 4) stops needing an explanation and becomes two labelled rows
  in a table**, because the clocking regime is printed with every run by construction —
  which is also §4 invariant 5, enforced by the tool instead of by discipline.
- **#232's blocked baseline.** #232 Open Question 1 ("the baseline corpus is not concretely
  named") is #335 Open Question 2 wearing a different hat, and #232's *entire* falsification
  gate is a profile #335 plans to take again. One instrument closes both.
- **#554/#557/#560** (the public suite, the staleness lane, the cross-tool comparison). A
  `-stats` line on stdout *is* the machine-readable output #730 wants, *is* what #557 diffs
  on a schedule, and *is* how you compare against Digital and Logisim-Evolution without a
  bespoke harness. Three boundary comments on this issue carefully partition ownership of
  the *numbers*; none of them owns the *instrument*, and so it risks being built four times.
- **Users and autograders get it free** — "my circuit is slow" becomes a paste-able census.

Every tracked `.jls` in `test/fixtures/` and every golden becomes a benchmark. The
"CPU-scale anchor" question stops being architectural.

## Reframe B: gate the deterministic counters; publish the clock, never gate it

Acceptance criterion 4 asks for one equality (events/cycle) plus two ratcheting bands
(ns/event, bytes/event), and Open Question 4 then asks how wide an honest band is — "too
tight and the lane flakes, too loose and the gate is decorative." That question has no good
answer on shared CI runners, and answering it costs a task.

Delete the wall-clock bands. **Events/cycle is the right gate and very nearly the only one
you need**: it is exactly deterministic, machine-independent, JIT-independent, and it moves
if and only if the engine's *behaviour* changed — which makes it a semantic ratchet in the
same family as `HeadlessCoreRatchetTest` and `NotificationRatchetTest`, patterns this
project already trusts. Add posted/dup-suppressed counts and max queue depth as further
equalities and you have caught every change keystone C's §8 stage table predicts, without a
timer. Wall time and ns/event go to a scheduled lane that *records a trend* and never fails
a build. Open Question 4 disappears; TASK-0026 shrinks to a day.

Also check criterion 5 before funding it: the golden corpus is already byte-asserted by
`BatchSimulationGoldenTest`, `SequentialGoldenTest`, `VcdExportGoldenTest` and
`ElementSimulationGoldenTest`, and `pom.xml`'s PIT gate over `jls.sim.*` (per #232) already
fails a silent semantic weakening. "A gate asserts the corpus stays byte-identical" may
already be true.

## Where the issue pulls against the project's arc

- **Cost.** 5-10 maintainer-weeks (8.5 in rows) of **zero product code** on a
  single-maintainer educational tool, to produce parameters for a model of a program that
  has not been approved. Under the three reframes above the same capability lands at roughly
  1-2 weeks: `-stats` + parameterized RV32I generator + one events/cycle equality test.
- **Measurement as a substitute for the fix.** keystone C already names the actionable
  findings — 37.6% `BitSet`, 47.7% queue bookkeeping, **4.9% actual logic**; `SigSim`'s
  quadratic `newSignals +=` (still present at HEAD, `src/jls/elem/SigSim.java:63-74`) is "a
  `StringBuilder` and an afternoon"; `PinChanged` is a zero-field record allocated 1.92M
  times. You do not need α to know to fix those. The risk here is a measurement program that
  consumes the budget the engine work needed, gated behind #353 which is itself the
  `StringBuilder` fix.
- **Instrument-as-scope, not deliverable.** `docs/machine-calibration.md` is 1,124 lines of
  a document the issue then spends a task discharging. Prose about how to re-measure is what
  §12 of keystone C already was, and it did not survive one session. A `-stats` flag cannot
  bit-rot without failing `CliFlagTableTest`.

## What I would keep verbatim

§4 invariant 4 (**fixture tracked before `riscv/` is deleted**) is genuinely a correctness
constraint and the amendment about re-homing all six `riscv/examples/*` files rather than
two is exactly right. §5 criterion 3 — *deliberately perturb the engine and watch the gate
go red* — is the best line in the issue and should be a permanent test, not a close-out
ritual. And the deletion of `riscv/` (2,686 lines of untested out-of-tree Python that
`ARCHITECTURE.md` never mentions) is overdue on its own merits.

## Verdict and the criteria I am disregarding

**endorse-with-reframing.** The end — measurement as a permanent, tracked, product-side
instrument under a deterministic gate — is right, is load-bearing for the project's largest
open architectural decision, and should be funded. I am explicitly disregarding: acceptance
criterion 1's α/k/CPI *model parameters* (measure the observable, delete the model);
criterion 4's ns/event and bytes/event *bands* (gate deterministic counters, publish the
clock); Open Questions 1, 2 and 4 (dissolved by the tracked fixture and by dropping timing
gates); and TASK-0024's document-discharge framing (the flag is the document). Recommend
re-scoping to: (a) `-stats` on the batch surface, (b) a tracked, parameterized RV32I
generator in `test/`, (c) one events/cycle-equality ratchet plus the perturbation test,
(d) `riscv/` deleted after (b). Then reopen #232's falsification gate with a real
instrument — and revisit `ARCHITECTURE.md:354-358` with a threshold like "below 10 kcycles/s
on the #202 golden", which is the sentence this whole feature exists to make writable.
