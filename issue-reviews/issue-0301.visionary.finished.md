# Issue #301: CAP-02: a CPU drawn in the JLS editor boots Linux to a shell and answers typed commands against the same golden the behavioral tier produces
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of the machine block, CAP-02 makes one architectural claim: **JLS should be able to
run part of a drawn design as something other than drawn logic, and prove that the substitution
changed nothing.** Linux is the workload chosen to make that vivid; the fidelity toggle
(#325) and the retirement-indexed comparison (#347) are the mechanism. Everything else in the
sixteen-row roster is either scaffolding the project needs anyway or the CPU itself.

I endorse that claim. I do not endorse the seam it cuts along, the artifact it nominates as
evidence, or the order in which it spends money — and I am explicitly setting aside AC-2's
status as *the* acceptance criterion. Reasons below.

## Grounding

I verified every code anchor the issue leans on and they are all exact: `Circuit.java:1466`,
`Circuit.java:102` (`FORMAT_VERSION = 2`), `Memory.java:1224` (`DENSE_CAPACITY_LIMIT = 1 << 22`)
with its comment at `:1221-1222`, `Simulator.java:224-234` (poll, `dupCheck.remove`, then the
`now > maxTime` break), `BatchSimulator.java:77/:89` (pause is byte-identical to stop),
`SigSim.java:64-74` (four string `+=` in the per-line loop), `FileAbstractor.java:65`,
`Element.java:18` / `LogicElement.java:18`. `grep -rlE 'MeasurementGate|CalibrationFixture'`
returns zero files. B-01…B-11 are real findings, precisely stated. This is a well-evidenced issue.

Two grounding facts the issue does not carry, both load-bearing for the visionary question:

1. **`docs/machine-calibration.md`, `docs/parity-contract.md` and `docs/plan/` are absent from
   the working tree.** Every wall-clock band, every kill-criterion threshold, and rulings D13/D15
   themselves live outside the repository.
2. **`docs/capability-roadmap/` is in the tree, is anchored to HEAD line numbers, and covers most
   of the same ground — and this issue cites it zero times** (grep for `capability-roadmap|keystone|sweep-0`
   across the body and all five comments: 0 hits). Yet `keystone-c-performance.md:655` records
   **318 ns/event, 8,090 cycles/s** and the issue's constants table quotes "318 ns/event" from
   `machine-calibration.md §4`. The same measurement, re-derived in a parallel corpus, under a
   different vocabulary (FEAT-0NN/CAP-0N vs P1–P13/sweeps/keystones), with no cross-citation.

That is the first thing I would fix, and it costs nothing but a day: **reconcile the plan corpus
with the capability roadmap before funding 159–253 maintainer-weeks against it.** Two planning
universes that measure the same engine and never cite each other means one of them is going to be
maintained as dead weight, and the maintainer will discover which one during month eighteen.

## Reframe 1 — the seam is the *definition*, not the *instance*

FEAT-031 is written as "one attribute on one instance selects drawn or fast." That is the
demo's phrasing, not an architecture. The same capability stated one level up is far more
powerful and no more expensive:

> **A subcircuit definition may have more than one body — a drawn body and a native body — and
> an instance binds which body it runs.**

This is mixed-abstraction simulation, which is what every industrial simulator has (behavioral
Verilog and gates in one netlist) and what JLS conspicuously lacks. Cut here and:

- The CPU stops being a special case. A 74-series library part, a RAM, a UART, an FFT block, and
  CAP-03's ternary core are all the same feature. #357 (shared parameterized definitions, at
  25–36 mw the single largest row in the roster) is already building the definition/instance split
  this rides on — the native body is a third kind of body in a structure being built anyway.
- The parity invariant becomes *general* ("a native body is observationally equal to its drawn
  body at the definition boundary") and therefore testable on dozens of small definitions rather
  than resting on one 4×10⁷-instruction boot. AC-5's T0 already specifies exactly the right test
  — exhaustive at ≤16 input bits — and applying it to an adder, a mux, a register file and a UART
  is where the invariant actually gets proven.
- ARCHITECTURE.md's "Extension points: the typed seam catalog" (#223) and the plugin trust-boundary
  decision (#222) already describe the governance for this. A native body is a first-party
  compiled-in contribution through a typed seam. The issue reinvents this as a fidelity attribute
  and inherits none of the existing policy.

The issue never considers this framing. It is not a smaller ambition — it is the same mechanism
with a name that survives the CPU.

## Reframe 2 — the evidence hierarchy is inverted

§3 risk 4 concedes, honestly, that AC-2 localizes a divergence to a **console byte**, not an
instruction, because timer ticks land between different instructions on the two tiers by
construction. So the flagship acceptance criterion produces the *weakest diagnostic in the plan*,
while AC-5's T1 — riscv-tests plus a fuzz corpus through both bindings, retirement traces
byte-identical, the only arm with instruction-level localization — is filed as a per-push side arm.

That is backwards. **T1 is the proof; the boot is the poster.** A byte-identical retirement trace
over riscv-tests through both bindings *is* the parity claim, with localization, in minutes, on a
required gate. The boot transcript adds reach and a screenshot, not epistemic weight — and the
issue itself already says AC-2 "can pass while the two tiers are running different devices"
(§3 risk 1, AC-7, owned by nobody). A capstone whose headline criterion can pass vacuously and
whose sound criterion is a footnote has its labels swapped.

Concretely: promote T1 to the primary system-level criterion, keep AC-2 as the demonstration
milestone, and AC-7's unowned device differential stops being the thing that decides whether the
capstone means anything.

## Reframe 3 — parity that *cannot* be vacuous already exists in this repository, twice

`docs/capability-roadmap/lf-02-compiled-evaluation.md` §2.7 ("Keeping the two engines identical —
specify this, do not hope") specifies an engine-vs-engine differential harness at **2–3 weeks**:
every `.jls` in `test/fixtures/`, `examples/`, `riscv/build/`, `riscv/gui/cpu.jls`, compared as
full VCD byte-for-byte plus stdout byte-for-byte, with a ~200-line structural circuit fuzzer.
Its own justification is the point: *"Mode T's comparison is byte-identity, which needs no new
oracle at all — the existing goldens are the test."*

Compare CAP-02's parity: #325 + #347 = 15–24 mw, a new retirement index, a new record type, a new
boundary contract, a device differential nobody owns, and a residual vacuity risk the issue
documents itself. Same word, opposite epistemics, and the reason is general: **differential testing
is cheap and sound when the two sides share a specification and differ in implementation strategy;
it is expensive and unsound when the two sides are two different artifacts that agree only by
construction at a hand-drawn boundary.** CAP-02 chose the expensive kind without noticing the cheap
kind is already specified in the tree and already made binding by ARCHITECTURE.md's #221 equivalence
criterion ("must agree bit-for-bit with the #202 RV32I integration golden run as a differential
oracle").

The second existing instance is `riscv/`: a working drawn RV32I CPU (`riscv/gui/cpu.jls`,
committed), an independent reference emulator (`riscv_ref.py`), a directed suite (`verify.py`),
and a **randomized differential fuzzer** (`fuzz_diff.py`) that already requires the hardware's
final register file and data memory to match the reference exactly. That is AC-5/T1, shipped, in
Python, out of tree — and D5 deletes it, while #343 spends 14–22 mw rebuilding the oracle half in
Java. Deleting the only working demonstration of your capstone's central mechanism, in order to
rebuild it, while your top-listed risk is that the two sides are never related by a harness, is the
one move in this plan that pulls against the project's arc rather than with it. Promote `riscv/`
to the parity harness's first client and commit `cpu.jls`-class fixtures instead; §3 risk 7's
untracked `k2000.jls` problem dissolves at the same time.

## Reframe 4 — the cost decomposition the issue does not print

The Cost section splits the roster by *which capstone funds the spine*. The decision-relevant split
is **roadmap work versus CPU work**:

| Group | Rows | Cost |
|---|---|---|
| Work the project should do regardless | #353, #354, #317, #335, #319, #357, #322, #362, #363, #327 | **105–158 mw** |
| CPU-specific | #325, #324, #343, #347, #326, #364 | **54–95 mw** |
| — of which buys an *emulator*, not a drawn machine | #343 + #324 | 24–38 mw |
| — of which is the drawn machine and its parity | #325 + #347 + #326 | **27–50 mw** |

(105 + 54 = 159; 158 + 95 = 253 — the issue's own totals.) The first group maps almost row-for-row
onto capability-roadmap programs P1, P7, P8-stage-1, P9 and P13, whose amendment prices P7–P13
together at **130–190 wk**. CAP-02 standalone therefore costs more than the entire seven-program
capability roadmap, while *being* most of it. That is not an argument against CAP-02 — it is an
argument that the issue is mis-titled. Its real content is "fund the capability roadmap, and add a
CPU," and the CPU's honest marginal price is 27–50 mw, not 159–253.

Stated that way the funding conversation becomes possible. Stated the current way, a maintainer
reads "3–5 maintainer-years for a Linux boot" and files it under never.

## The failure mode this plan makes most likely

The ~10 mw demo slice boots Linux behaviorally with nothing drawn. Be clear about what that
artifact is: **an RV32 emulator bolted into a logic simulator.** Java and JS RISC-V emulators boot
Linux routinely; JLS doing so carries no credibility at all, and the issue's own audience section
agrees ("either true with a byte-compared transcript or it is marketing"). If single-maintainer
funding stops after the cheapest, most demo-able slice — the overwhelmingly likely outcome across a
3–5 year program — JLS is left with a second CPU implementation to maintain forever, no parity, and
a headline it cannot defend. Sequencing the *least* aligned artifact first because it is cheapest
is the trajectory risk, and no kill criterion covers it. Under Reframe 1 the cheapest first slice is
instead a native-bodied `Adder` definition proven equal to the drawn one exhaustively — small,
aligned, and immediately useful to the first-year student KC-02-8 protects.

## What I would keep, unchanged

- §1's discipline of fixing the observation before the mechanism. It is the best thing in the issue.
- AC-3, the clock-period falsification guard, funded with its rung. One week, and it is the only
  thing standing between "shared golden" and "golden that encodes time." Under any reframing it survives.
- AC-4's insistence that a *subtly* wrong binding be rejected and the report **text** asserted.
- KC-02-8 ("any regression to the first-year student drawing an adder → the responsible feature
  stops"). Promote it from kill criterion to design driver: under Reframe 1 that student *gains* —
  a definition with a native body simulates instantly and the ten-box machine is browsable — rather
  than merely not being harmed.
- The four-state core (#322) and clock/reset/domains (#327). Neither needs a CPU to justify it.

## What I am disregarding, and why

AC-2 as the flagship criterion, and the "same golden shared between a behavioral tier and a
structural tier" framing that the abstract calls "what no drawn logic simulator has done." I am
disregarding it because the issue's own §3 risk 1 and risk 4 show it can pass vacuously and cannot
localize, because AC-5/T1 proves the same property soundly and sooner, and because the property
worth claiming is not "our emulator and our circuit agree on one boot" but "any definition in JLS
may have a native body that is provably equal to its drawn body." The second claim is bigger,
cheaper, generalizes to CAP-03 and to every library part JLS will ever ship, and is the one that
changes what JLS *is*.

**Verdict: endorse-with-reframing.** The direction is right and the evidence discipline is
exemplary. Retarget the headline from "boots Linux" to mixed-abstraction simulation, swap the
evidence hierarchy so T1 leads and the boot demonstrates, reconcile with the capability roadmap
before spending against `machine-calibration.md`, and keep `riscv/` alive as the first client
rather than deleting the only working proof of the idea.
