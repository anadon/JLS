# Issue #305: CAP-12: a drawn mixed-signal circuit turns a recorded photoplethysmogram into a live beat rate — the point at which JLS stops being a digital tool
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the machinery and the ask is one sentence: **a student should be able to draw
one artifact whose verdict is a physical measurement.** BPM off a recorded PPG is
the payload; the transducer→conditioning→threshold→counter chain is the vehicle;
"JLS stops being a digital tool" is the identity claim. That goal is genuinely
good, and it is the only capstone in the analog family that names a *marked
competency* rather than a *fidelity claim* (#309 proves parity with ngspice; #303
and #308 prove I/O). If the project should acquire electrical content anywhere, it
should acquire it here.

Everything below is about whether MNA + Newton + a device palette is the route.

## 1. The programme reverses an in-tree determination, and the issue never says so

The issue's Related-Issues section searched the *tracker* and concluded nothing
duplicates. But the *tree* holds a recorded determination against the entire
programme, written after and over the standards survey:

- `docs/capability-roadmap/README.md:1035-1043` — §6 "What still stays out",
  ground **(a) different tool class**: "**Continuous-time and analog** … Supporting
  these means being a SPICE-class solver — a different tool, not a deeper digital
  model."
- `docs/capability-roadmap/sweep-03-elements-and-hdl.md:632-634` — same finding,
  same words.
- `docs/capability-roadmap/sweep-06-physical-boundary.md:83` and `:553-556` —
  "No continuous-time solver, and none should be added"; "Adding one is building
  SPICE, which is ground (a)."

And `ARCHITECTURE.md`'s "Recorded decisions" for #221 says the event-queue
interpreter "remains JLS's **only** simulation execution strategy," with a revisit
trigger (CPU-scale designs unusably slow) that analog does not satisfy and an
equivalence criterion written entirely for a digital pass. A-STEP is a second
numerical kernel on the hot plane; calling it "one process, one clock owner" is
true and beside the point. #221 is not in `related`. Neither is #77.

For an issue that cites `Element.java:18`'s `permits` clause to the character, this
is the one place it does not anchor, and it is the load-bearing one. **A capstone
that overturns the architecture-of-record must overturn it in the record** — an
amendment on `capability-roadmap/README.md` §6(a) and a row in `ARCHITECTURE.md`'s
recorded-decisions section, naming the reason, *before* 61.5-94 mw is scheduled
against it. Right now the issue reads as a programme that outgrew its own docs
without noticing them.

## 2. The reframing: the issue's Cost section refutes the issue's solver

Read the Cost section against §2's minimality argument. Cost says: op-amp
macromodels keep the path "almost entirely linear with no forward-biased junction
in it"; 24 equations; 2.00 Newton iterations per timepoint (a linear solve plus a
convergence check); "its timestep is set by the sample lattice the student asks
for (25-250 Hz PPG, so 4-40 ms), not by the physics and not by error control."

A system that is linear, fixed-step, and 24 equations wide is not a SPICE problem.
It is a **discrete-time linear-system problem**, and JLS can express it without a
matrix at all.

**Alternative design — the signal-chain view.** Instead of a netlist of devices
solved by MNA, ship a small palette of *drawn conditioning blocks* carrying declared
characteristics, evaluated as fixed-step IIR sections on the #367 time base:
transimpedance (R_f, C_f), first-order high-pass (f_c), gain (A), second-order
low-pass (f_c, Q), comparator (threshold, hysteresis), ADC/DAC (#368 unchanged).
Eight blocks, one generic dialog — the same shape OQ-2 already recommends, minus
the physics engine underneath.

What this deletes, not defers:

| Deleted | Because |
|---|---|
| FEAT-046's LU, pivoting, Newton, LTE controller (17.5-26 mw) | no matrix, no iteration, fixed step |
| Most of FEAT-049's device models + convergence hardening (of 21-33 mw) | blocks are closed-form; nothing to fail to converge |
| **AC-4** (ground/singular-matrix diagnostics) | there is no matrix and no datum node |
| **OQ-3** (sparse vs dense LU; "the largest un-derisked determinism item") | no factorization |
| **OQ-4 / AC-6** (strict FP in 51.7-59.6% of runtime) | a fixed IIR chain in strict FP over a fixed operation order costs almost nothing |
| **KC-12-3 / KC-12-4** (22 device types, per-family mutation scores) | the library is ~8 blocks and never grows toward transistors |
| **KC-12-1a**'s 61.5 mw delivery threshold | the remaining set is #367 + #368 + #318 + a block library — plausibly under 20 mw |

What survives intact: §1 steps 1, 2, 5 (reworded: a block with no declared corner
frequency, or an unconnected input, names the drawn element), and 6. AC-1's
`T²/120` derivation survives verbatim and gets *easier*, because a fixed lattice
removes the accepted-step-history dependence that motivated it. AC-2 survives and
becomes near-trivial. AC-3 survives unchanged.

**I am explicitly disregarding §1 step 3 and AC-4 as written.** Step 3's
"−0.19998 V with 2.0 mV of ripple *at the transimpedance node*" is an acceptance
criterion of the *implementation the issue chose*, not of the outcome it named in
its own Abstract. A block chain gives you a probeable transimpedance *output* with
the same 1% pulsatile ripple the physics predicts; what it does not give you is a
solved node voltage inside a topology the student drew from R's and C's. That is a
real loss and it should be stated plainly — but it is a loss of *component
realization*, which is a second course, and which KC-12-1a already tells the
project to stop before ("stop before the transistor library"). For an
instrumentation lab the marked competency is *did you pick gain and corner
frequencies such that a 1% pulsatile signal survives to the comparator* — and a
block whose f_c is a declared parameter makes that competency **more** visible, not
less, because the design intent is on the drawing instead of hidden inside two
resistor values.

## 3. A second alternative, and why the issue is right to reject it — but for the wrong reason

The project already holds a delegation stance in writing: emit the artifact, let
the specialist tool consume it (`sweep-03:640-648` on nextpnr/openFPGALoader;
`sweep-06:83` prices "a structural `.subckt` printer over `HdlExporter.buildModel`"
at roughly a week). By that logic SPICE belongs to SPICE, and JLS should emit a
netlist and shell out to ngspice, exactly as it shells out to Yosys.

The issue rejects this (A-STEP is "one process, one clock owner") and is correct —
but its stated justification is licensing (D8, "porting with attribution"), which
is the weakest available argument. The decisive one is never made:
`docs/grand-architecture.md:29-32` fixes the single self-contained jar as
load-bearing — "the architecture may not assume a network, a server, or an install
step" — and AC-2's byte-identity across 4 platforms × 2 JDKs is unclaimable for an
external binary on a lab machine. **Make that argument; it is the one that holds.**
Note it also argues *for* §2's reframing: a fixed-step IIR chain in pure Java is
more single-jar-native than a Newton solver is.

## 4. The structural misalignment in the required set

Three of the six required rows are not analog work and are independently wanted:

- **#341 (FEAT-027, strength lattice)** — the roadmap calls this the "*digital
  shadow* of IBIS … P1 and **in scope**" (`README.md:1041-1043`,
  `sweep-06:83`). It unblocks #22 I²C (definitionally open-drain wired-AND), #67
  EVCD, #129 BSDL, and IEEE 91's output qualifiers. It wants funding on its own.
- **#318 (FEAT-014 residual, per-view geometry)** — owned by the multi-view /
  collaboration arc (#299, #163 stack), which the issue itself says must be solved
  once or it will be solved twice.
- **#367 (FEAT-047, physical time base)** — makes
  `docs/simulation-semantics.md:26-29`'s honest admission ("the VCD exporter's
  nominal `1 ns` timescale is a tool-compatibility mapping only") stop being an
  admission, and is a precondition for SDF/Liberty (P6) regardless of analog.

Those rows are 19-29 mw of the 61.5-94 mw sum. Bundling them makes the analog
decision look 30-45% cheaper than it is (the true marginal analog cost is
#351 + #368 + #331 = 42.5-65 mw), and simultaneously makes three
independently-justified features read as contingent on a programme the tree rules
out. The required-set formalism is internally correct and arc-wrong: it is optimized
for closure under rule E, not for telling a scheduler which of these the project
wants irrespective of whether analog ever happens.

## 5. What I would do

1. **Record the reversal or accept it.** Amend `capability-roadmap/README.md` §6(a)
   and add an `ARCHITECTURE.md` recorded decision, or the analog family stays
   unscheduled. This is cheap and it is the gate.
2. **Split the set.** File #341, #318, #367 as roadmap work with their own
   justifications. Whatever analog shape wins then costs what it actually costs.
3. **Run the block-chain spike against the 328-line solver spike.** Both produce a
   BPM from the same fixture. Compare on: mw, determinism-proof difficulty, the
   diagnostic quality of a broken circuit, and what an instructor can mark. AC-7
   already mandates a calibration experiment; make it a *comparison* rather than a
   measurement of one branch. That single experiment decides §2 honestly, and it is
   a smaller commitment than KC-12-2's week-8 gate.
4. If the solver wins that comparison, **fund it as CAP-14 (#309, ngspice parity)**,
   where general nonlinear correctness is the deliverable — and let this capstone be
   the block chain, which is what its own Cost section describes.

The outcome in the Abstract is worth having. The route is the most expensive one
available, it is chosen against a written determination it does not cite, and the
issue's own measurements are the best argument for the cheaper route.
