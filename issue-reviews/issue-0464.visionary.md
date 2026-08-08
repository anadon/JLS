# Issue #464: TASK-0104: the analog solver becomes homework-grade — a named escape ladder, a per-device convergence veto, diagnostics that name the drawn element, and a 200-circuit hard corpus with a numeric kill criterion
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the numerics and the claim is: *a student must be able to act on an analog failure, and an
instructor must be able to bound what they assign.* Everything else in the body — the three rungs,
the veto, the 200 fixtures, the 5% number — is instrumentation for that claim. I endorse the claim
without reservation; it is the single thing that separates FEAT-046's solver (#351) from a demo. But
three of the four instruments this task picks are the wrong ones, and one of them is measuring the
wrong quantity, so the verdict is rethink rather than endorse-with-reframing.

## Where the work does strengthen the arc

- **Element-anchored failure is exactly the project's grain.** `src/jls/LoadError.java` already is a
  record `(Category, detail, line, element, hint)` whose whole design is "a fixed taxonomy tests
  assert on, wording free to improve"; `HdlExporter` already refuses by *naming the drawn offenders*
  (`describe(el)` at `:184/:191/:495`, producing `Memory "imem" at (300,60)`). #464's "name the
  drawn element, never `row 7`" is the third instance of a pattern the repo has twice converged on.
- **Purity of the rung selector (H4/P7)** is the right kind of requirement for this codebase: it is
  the same move as `getElementsInStableOrder()` versus the `HashSet` at `Circuit.java:48`, and it is
  what makes FEAT-046's byte-identity claim survivable.
- **`DeviceModel.converged` defaulting to `true`** is a genuinely elegant seam: additive, per-family,
  and the natural extension point as TASK-0103 lands devices. Keep it exactly as specified.

## Reframe 1 — the gate measures robustness; the product needs *legibility plus a declared envelope*

The binding criterion is a rate over a corpus. But a rate is not what makes a tool assignable: SPICE
itself does not converge on everything, and no instructor checks a percentage before writing a lab.
What an instructor needs is (a) a written statement of the circuit class that is *expected* to
converge, (b) a refusal to advertise beyond it, and (c) 100% of failures inside and outside that
class being locally attributable. Item (a)/(b) is already the recommended default of **#351's Open
Question 5** — "state exactly what class of circuit is expected to converge and refuse to advertise
beyond it" — which is currently unowned by any task. This issue is the natural owner and does not
claim it.

Concretely, I would replace P1 as the headline gate with:

- **G1 (absolute, not statistical):** every non-convergence, on any input, produces a diagnostic
  naming a drawn element and one actionable hint. No sampling, no percentage, no corpus selection
  bias — this is a totality property over the failure path, testable by fault injection rather than
  by 200 circuits.
- **G2 (declared envelope):** `docs/simulation-semantics.md` names the converging class (topology
  families × device families × source classes), and a fixture per named family pins it. A circuit
  outside the envelope that fails is *documented behaviour*, not a defect against a 5% budget.
- **G3 (regression ratchet):** the measured rate over whatever corpus exists is published and may
  not rise. A ratchet is honest; a 5% absolute threshold over a self-authored corpus is not.

Under G1–G3 the six-maintainer-week kill criterion becomes sharper, not softer: you can fail G1 in
week one by fault injection, which is a far earlier falsification than "run 200 circuits after the
ladder is built."

## Reframe 2 — generate the corpus, do not hand-curate 200 circuits

Open Question 5 ("where do the 200 come from if fewer than 200 failed?") is the design telling you
the artifact is wrong. Hand-minimizing 200 circuits is the most expensive line item in the task
(the issue itself budgets the *second week and beyond* to it, unbounded), and the same person builds
the ladder and picks its exam.

The elegant substitute is a **seeded, deterministic hostile-circuit generator** — a small grammar
over the named families (diode bridges, astables, comparator rings, latch-up pairs, ring
oscillators) with parameter ranges, plus automatic shrinking of any failing sample. Then:

- the "200 circuits" become a sample size and a seed, re-drawable at any commit;
- committed fixtures are exactly the *shrunk* failures — the regression suite the issue wants, but
  produced by the machine rather than curated by hand;
- the self-grading objection dissolves: hold out a seed range for CI that the implementer does not
  tune against;
- CI cost becomes a dial (sample count), which answers Open Question 6 by construction instead of
  by a 15 s drift decision.

The repository already has the precedent and the taste for this: `riscv/fuzz_diff.py` differentially
fuzzes the CPU against a reference emulator. Convergence is a *better* fuzzing target than the CPU,
because the oracle is trivial (converged / not) and shrinking is well-defined (drop a device, keep
the failure).

## Reframe 3 — diagnostics belong on the project's one error seam, not in a new package with a taboo

`jls.analog.Diagnostics` with the contract "no message contains the substring `row `" (P3) is a
string-level ratchet defending a structural property. The stronger cut, and the one that pays for
three other issues at once, is to **generalize `LoadError` into an element-anchored diagnostic
record** — category, detail, drawn-element identity (stable id + name + location), hint — and make
the analog failure path emit *that type*. Then a matrix row is not forbidden by a test; it is
**unrepresentable**, because the type demands an element. The same record retires
`HdlExportException`'s ad-hoc string joining, and it is the natural home for **#331's invariant 5**
("no student-reachable path emits a matrix-singularity message") and its datum/node-partition
diagnostics, which today are specified twice in two issues in two vocabularies.

This also relocates the elaboration-time row→element map (H3) correctly: it is not a convergence
artifact, it is the **provenance index** any analog-facing surface needs (probes, waveform pane,
datum diagnostics, future SPICE netlist naming). Build it as such, owned one level up, and #464
consumes it instead of inventing it.

## Disregarding P5: the ring-oscillator `.op` is a pedagogically wrong target

P5 makes rung 3's justification executable: disable pseudo-transient, the ring oscillator's `.op`
fails; enable it, it passes. I am explicitly disregarding this acceptance criterion. A ring
oscillator has no stable DC solution; the point OPtran converges to is an unstable equilibrium the
physical circuit never occupies. Shipping that as the showcase teaches a student that the tool's
answer for an astable is a DC number — the exact class of "plausible, wrong, reproducible answer"
that #331's own limiting-protocol rationale calls the worst failure available to a teaching tool.

The better behaviour, and it is cheaper: **make "this circuit has no stable operating point" a
first-class result**, with the hint "simulate transient from stated initial conditions". That is a
fourth outcome next to converged/failed, it is what the student should learn, and it is what the
waveform pane wants anyway. Rung 3 then survives on its real merit — hard-to-start circuits that
*do* have an operating point (latch-up pairs, heavily-loaded bias chains) — and gets a fixture that
does not mis-teach. If no such fixture can be built, H1 has been refuted honestly and the ladder is
two rungs, which is a better outcome than a rung defended by a misleading demo.

## The seam nobody in this chain has cut: a tiered solve

The analog programme's stance is "port the numerics, never subprocess a solver" (#351 §Cost, D-A1),
justified by the single offline jar and byte-identical goldens. Note the asymmetry against the
project's other settled stance, `docs/grand-architecture.md` §2: for HDL, JLS **orchestrates
external tools and refuses to reimplement semantics** (#61/#63, with the `ToolLocator.findOnPath`
self-skip idiom already shipped in `test/jls/hdl/ToolLocator.java`). For analog it does the reverse,
and #464 is where that reversal is paid for — six maintainer-weeks of convergence engineering that
ngspice has had thirty years to do.

The reconciliation is not "subprocess everything", it is **tier the solve against the declared
envelope of G2**: an in-jar deterministic solver for the envelope (linear, diode, single-transistor
bias — the circuits a golden should be committed for), and, *outside* it, an optional external
solver that #397/TASK-0100 is already building the lane for, promoted from nightly oracle to a
user-visible escape hatch with the same self-skip. A student on a lab machine without ngspice gets
"this circuit is outside JLS's solver envelope; here is why, here is the element, here is the
netlist you can hand to ngspice" — which is *more* actionable than a 5%-of-the-time silent failure,
and the netlist printer is costed at ~1 week in
`docs/capability-roadmap/sweep-06-physical-boundary.md` (#111 row). Under this design the ladder
becomes an optimization that widens the envelope incrementally, with each rung justified by the
circuit families it moves inside — which is a far better rung-entry rationale than "SPICE has three
gmin variants and we will ship one."

## What I would keep unchanged

The veto and its H2 argument (a node step inside `VNTOL` can move a diode current by orders of
magnitude) is correct and non-negotiable; P8 is the right test. The purity requirement (H4/P7), the
no-`jls.sim`-contact constraint (O2), the single-threaded model (§7.9), and the `converged` default
of `true` all stand. So does the human rejection line — it is the one output in the issue that a
student reads without being taught to.

## Bottom line

The goal is right and under-claimed: this task is where the analog programme becomes honest rather
than impressive. But it buys that honesty with a statistical gate over a self-authored corpus, a
bespoke diagnostics package that duplicates a seam the repo has already built twice, and a headline
demo that teaches a physical falsehood. Re-cut it as: totality-of-attribution plus a declared
envelope (G1/G2) as the gate, a generated-and-shrunk corpus in place of 200 curated fixtures, the
element-anchored diagnostic record hoisted to a shared seam owned above this task, "no stable
operating point" as a first-class result instead of P5, and the ladder justified rung-by-rung by the
envelope it widens — with the external solver kept as the documented escape hatch beyond it. That
version is smaller, falsifiable in week one instead of week six, and pays for #331's invariant 5 and
#351's Open Question 5 on the way past.
