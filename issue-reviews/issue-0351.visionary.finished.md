# Issue #351: FEAT-046: JLS solves a continuous-time circuit in pure Java, produces the same bits on every platform, and proves the answer against closed forms and a real external simulator
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the numerics and the claim is: **a student in a digital course should be able to draw the
places where the world is continuous — a photodiode front end, a preamp, an R-2R ladder and its
reconstruction filter — and have JLS compute them, offline, with an answer you can commit.** That
is a legitimate ambition and I do not want it abandoned. But the issue converts it into "port a
SPICE-class engine and prove byte identity," and almost every structural decision that follows —
the four-task split, the headline claim, the version-one scope, the sequencing against the rest of
the roadmap — is downstream of that conversion rather than of the goal. Three of those decisions
are wrong on the project's own evidence, so this is `rethink`: keep the ambition, rebuild the plan.

I am explicitly disregarding the stated acceptance criteria in two places, named in Reframes 1
and 2 below.

## 1. The headline claim is nearly a theorem, and the schedule is built around it as if it were a risk

§ 6 says byte identity "is the justification for the whole approach," § 7 calls its failure "the
anticipated `REFUTED:`," and IC-1 makes it the required gate that "no child asserts alone." That
weighting does not survive contact with the platform or with this repository:

- **Java has had always-strict floating point since JDK 17 (JEP 306); JLS's floor is JDK 25.**
  `strictfp` is a no-op and double arithmetic is bit-defined by the language spec. Invariant 2
  ("strict floating point throughout, asserted as a test") is asserting something the language
  already guarantees. The real residue is one rule — `StrictMath` (fdlibm, specified identical
  everywhere) instead of `Math` (1-ulp latitude on transcendentals); `Math.sqrt` is exactly
  rounded by spec and safe either way. That is a static-analysis lint, not a research question.
- **Total device order already exists and is already tested.** `Circuit.getElementsInStableOrder()`
  (`src/jls/Circuit.java:479`) is consumed by `src/jls/sim/Simulator.java:151,196`,
  `src/jls/elem/SubCircuit.java:577,605` and `src/jls/edit/CircuitRenderer.java:262`, and pinned by
  `test/jls/SimulationSeedOrderTest.java`. The `HashSet` at `Circuit.java:48` is storage, not
  iteration order; the issue reads it as an open hazard when the mitigation shipped.
- **The determinism matrix mostly exists.** `.github/workflows/ci.yml` already runs
  `ubuntu-latest`, `windows-latest`, `macos-latest` and `ubuntu-24.04-arm`, and the project already
  ships a byte-reproducible jar rebuilt and re-checked on every push (`docs/reproducibility.md`,
  `repro-installers.yml`, `docs/windows-msi-determinism.md`, `docs/dmg-reproducibility.md`) plus
  byte-exact simulation goldens (`BatchSimulationGoldenTest`, `VcdExportGoldenTest`).

So TASK-0098 is roughly: one lint, one CI job wired into an existing matrix, one golden-format rule
(raw bits, invariant 5 — correct and cheap), and pinning `JLS_REPLICA_ID`. Two maintainer-weeks is
plausible; *"the load-bearing cut"* and *"schedule the falsification early"* are not. The unfalsified
risks in this programme are convergence on student circuits and whether JLS should own an analog
engine at all — and the plan spends its scheduling attention on the one claim it is nearly certain
to win. **I am disregarding IC-1's status as the feature's central claim.** Keep it as a gate; it
is cheap and correct. Do not organize the feature around it.

## 2. The feature is designed to change nothing, which is the deepest problem here

§ 3 states **"Modifies. Nothing existing."** and § 6 celebrates having no prerequisite feature. That
disjointness is presented as what makes criterion 1 achievable — but read it against the project's
own frame: `docs/capability-roadmap/README.md` ranks work by *capability gained*, and at close,
FEAT-046 gains a student exactly zero. Every user-visible outcome is realized in #331 (drawn
palette, convergence) or #368 (bridges). The largest single feature in the analog programme —
17.5–26 maintainer-weeks by its own band, on a single-maintainer project — lands with no drawn
element, no waveform on screen, and no lab a student can do. Open Question 5's half-finished-engine
risk is a *consequence of this cut*, and the recommended answer (write down what converges) is
documentation applied to a structural problem.

**Reframe 1 — cut the first slice vertically, not horizontally.** Version one should be one drawn
circuit end to end: an R, a C, a voltage source and a probe, drawn on the existing canvas, solved,
and plotted in the existing trace window. That touches the element hierarchy, the palette, the
value domain and the trace view — i.e. it is *not* a leaf package, and it will be harder in exactly
the places that matter. The engine behind it can be one hundred lines of dense LU over a constant
matrix. Everything IC-1 through IC-8 asserts remains assertable about that slice, and the
half-finished-engine risk becomes bounded by construction: the tool does what the palette offers.

## 3. Linear first is a different, much better engine — and the issue already saw it and buried it

The Intended Audience section notes the ladder-DAC student needs "the solver *and* its linear fast
path," treating linearity as an optimization inside a nonlinear engine. Invert that. Of the four
named audiences, the filter, the ladder, the reconstruction filter and most of the audio chain are
**LTI networks**. A linear engine is not a subset of the nonlinear one; it is a categorically
simpler artifact:

- No Newton loop, no junction limiting, no escape ladder — which deletes the entire convergence
  programme (#464/TASK-0104) from the critical path.
- No LTE controller and no adaptive step, so **criterion 5's grid flip cannot occur** — the third
  outcome, the resampling fallback and IC-5 all evaporate rather than being engineered.
- One factorization for the whole run (topology fixed), so criterion 4's statistics tuple collapses
  to something trivially stable, and invariant 6's four forbidden optimizations mostly become moot.
- Byte identity becomes near-certain rather than merely likely (see §1).
- Closed-form fixtures (IC-2) get *sharper*: an RC step, an RLC ring and a ladder have exact
  solutions, and the derived-tolerance derivation is a two-line argument instead of an argument
  about an accepted-step sequence.

**Reframe 2 — FEAT-046 v1 is "JLS solves linear networks exactly and deterministically." Newton,
limiting, LTE control and device models become FEAT-046b, gated on v1 shipping in a course.** This
is a different seam through the same material: it cuts along *mathematical class* rather than along
*engine vs. determinism vs. sources vs. corpus*, and the mathematical seam is the one that predicts
which risks are correlated. **I am disregarding § 2's four-task decomposition and § 6's critical
path** — TASK-0097 as specified (stamps + factorization + Newton + limiting + adaptive timestep in
one task) fuses the certain part with the uncertain part, which is the same error § 2 correctly
avoids elsewhere when it refuses to merge the solver with its determinism controls.

## 4. Model cards are the scope-creep vector and serve nobody named

Open Questions 3 and 4 are the author feeling this. The measured contrast is stated in the issue
itself: 88 settable parameters on a current card versus 14 in the target era. Then OQ4 recommends
polynomial controlled sources as **mandatory** so vendor op-amp macromodels "arrive free as data."
Nothing arrives free as data. Ingesting third-party macromodels means owning a dialect-compatibility
surface, `.subckt` scoping, convergence behaviour of models tuned against another engine's limiting
algorithm, and a support burden with no upper bound — for an audience (a student drawing a preamp)
that is far better served by three curated, JLS-native op-amp elements with a gain/GBW/slew dialog.
Note the precedent: the project demand-gates external element providers (#212, ARCHITECTURE.md's
plugin-trust decision) and declined i18n outright on "no requesting user." **Cut card ingestion from
version one and demand-gate it the same way.** If it returns, its trigger is a named instructor with
a named card, not "op-amps arrive free."

## 5. The delegation stance, and the one axiom that should be a priced tradeoff

Invariant 1 forbids `ProcessBuilder` in `src/` "ever." Elsewhere this project's identity is
*delegation across a subprocess boundary*: Yosys, GHDL, Icarus, openFPGALoader, and ARCHITECTURE.md's
recorded decision that "the external tool integrations (#61, #63, #62) already sit on that subprocess
boundary and stay there." The offline-jar argument for the ban is real and I would probably keep the
ban — but it should be a **priced** decision, and the price is 17.5–26 maintainer-weeks. The
unconsidered alternative is the one the repo already ships one tier down at
`src/jls/hdl/board/PcfEmitter.java` and in `-export out.v`: **emit the drawn analog subcircuit as a
SPICE deck, and read a waveform back.** That is a printer plus a reader, weeks not quarters, it
reuses the shipped self-skip idiom (`test/jls/hdl/GhdlCompileTest.java:34-36`), and it delivers every
named capstone student experience on a lab machine that has ngspice. Pair it with the linear engine
of §3 and you get: offline determinism for the linear subset, full fidelity where a solver is
present, and no in-repo compact-model library — at perhaps a third of the band. The issue never
considers this because D-A1 settled the port before the scope was settled; a port is cheap for a
linear engine and unbounded for a card-consuming one.

## 6. The alignment problem the issue does not name

`docs/capability-roadmap/README.md:1035-1040` places continuous-time analog under **§6 "What still
stays out," ground (a), different tool class**, naming #28/#35/#37/#42/#88/#113 and the analog half
of #112. `sweep-06-physical-boundary.md:83` is blunter: *"No continuous-time solver, and none should
be added."* `sweep-03:632` and `sweep-05:577` repeat it independently. Those documents are normative
at HEAD. The issue's basis for reversing them, `docs/plan/evidence/analog-determination.md`, **does
not exist in this checkout** (`docs/plan/` is absent) — it postdates the pinned evidence commit, and
§ Cost says so. The issue then states *"No open issue covers the analog solver… the gap is recorded
here rather than left blank,"* as if analog were an oversight, when the roadmap declined it four
times in writing.

That is a governance gap, not a nitpick. ARCHITECTURE.md's "Recorded decisions" section is where this
project keeps reversals with their rationale and revisit triggers (i18n, plugin mechanism, execution
strategy). **A reversal of §6 ground (a) belongs there before any analog task lands**, or the project
carries two contradictory normative positions and the next contributor cannot tell which governs.
Add the same trigger discipline the other entries carry — and note that the honest trigger for
analog is the one i18n was denied for: a named course, a named instructor, a named lab.

## 7. Smaller things worth carrying

- The roster is stale: all four rows say "unfiled," but TASK-0098/0099/0100 are filed as #481, #402,
  #397, and #464 (TASK-0104) exists under #331. The Definition of Done requires the roster, machine
  block and mermaid graph to agree with reality at close; they do not agree today.
- Open Question 1's 3.25× gap between the 17.5–26 mw band and the 8.0 mw of named rows is, under
  Reframes 1–3, largely a scope artifact: the residual *is* Newton, limiting, the escape ladder and
  the card surface. Deferring those does not hide the gap — it removes most of it.
- Invariant 7 (audit every absorbed file for unseeded RNG, wall clock, hash iteration, parallel
  reduction, as a merge gate) is the best idea in the issue and should survive every reframing. So
  should invariant 5 (raw-bit goldens) and IC-3 (stamps asserted on bit patterns) — the latter is
  the one test in the plan that catches the defect class users would otherwise find.
- Opportunity cost deserves one line in the issue: `sweep-06` measures that the repository's own
  flagship RV32I design cannot be exported at all (`HdlExporter` covers 21 of 33 element types;
  Memory, SubCircuit and ShiftRegister are hard rejects). Change **A** unblocks a whole tier for a
  fraction of this band. An analog programme that precedes it should say why.

## What I would endorse tomorrow

1. Record the §6 reversal in ARCHITECTURE.md with its trigger and its demand gate.
2. Ship a vertical slice: R, C, L, V, I drawn on the canvas; dense LU on a constant matrix; fixed
   declared step; waveform in the existing trace window; raw-bit golden; per-device stamp bit
   assertions; the StrictMath lint; `JLS_REPLICA_ID` pinned in CI; the existing OS/arch matrix
   extended by one job. Weeks, not quarters, and a student can do a lab at the end of it.
3. Add the ngspice nightly oracle against that slice — cheap there, and it validates the stamps and
   the integrator, which is what an oracle is for.
4. Hold Newton, limiting, LTE control, device models and cards behind a course that asks for them.
