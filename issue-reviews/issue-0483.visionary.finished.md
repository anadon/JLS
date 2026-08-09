# Issue #483: TASK-0112: a design you did not write can be checked, proved equivalent (or not) with a replayable counterexample, and told which parts the vectors never touched
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The goal is right and it is the project's headline leapfrog. `docs/capability-roadmap/lf-04-formal-and-grading.md`
states it exactly: JLS "has no representation of *correct*", and the differentiator is not the solver —
it is that JLS is "the only one holding both ends: the drawing and the proof." Everything below accepts
that end state. What I dispute is the engine, the seam, the ordering, and which answer gets guarded.

## 1. The issue drops the prerequisite that makes its own numbers mean anything

sweep-04 change D opens with "a `CoverageCollector` in `jls.sim`, **fed from the timestamp-closure hook (A)**",
and change B's `Assert` semantics are "**at timestamp closure (A)**, if enabled and `check` is 0, append a failure
record." Change A warns in terms this issue should have quoted: without a sampling region, "**every glitch counts
as a toggle and the numbers are noise**."

#483 does not mention change A anywhere — not in `blocked_by`, not in Threats, not in H2. Instead its H2 asserts the
opposite: four measures at "three instrumentation points that already exist", with toggle coverage placed "beside the
`probeSample` call". That call is `src/jls/elem/WireNet.java:525`, inside `propagate`, and `src/jls/sim/Simulator.java:241`
fires `afterEvent(event)` per event with no `afterTimestamp` anywhere in the file. So P9
(`toggleCoverageCountsZeroOneAndHiZSeparatelyPerBit`) will pass while measuring intra-timestamp transients that no
downstream element ever acted on. The test cannot detect this, because a glitch and a settled value look identical to
a per-event hook.

The same hole is worse on the property side. `Assert` in #483 is specified in one line — "one condition input, like
`Stop`'s" — with no severity, no message, no enable, no clock, and **no statement of when it samples**. Change B
specifies all five. An `Assert` that samples per event fires on transients and is unusable as a grading signal.
This is not a detail; it is the difference between a measurement and a rumour.

**Consequence for ordering:** change A (2–3 weeks, mostly amending `docs/simulation-semantics.md` §3/§4 and proving
non-disturbance against the goldens) is a real `blocked_by` for the coverage and property halves. It is also the
cheapest, most reusable, and least risky thing in this whole neighbourhood, and it unlocks #49/#50 independently.

## 2. The AIG is cut at the wrong seam, and the seam that exists is better

lf-04's architecture is explicit: `FormulaBuilder implements HdlModel.StatementVisitor` — "a third implementation of
an interface that already exists and is already double-dispatch clean" (`src/jls/hdl/HdlModel.java:148-193`, ten
`visit` methods, two implementors today). `HdlExporter.buildModel` already does the port walk, wire-net union-find,
jump fusion and identifier legalization a formula printer needs.

#483's §7.10 stage 1 defines α over "the elaborated model" and never mentions `HdlModel` or `HdlExporter` — except to
add `Assert`/`Cover` to an `HdlExporter` policy bucket, so it touches the file without using it. That is a second,
hand-written traversal of circuit structure, and the cost lands in three places:

- **The uncheckability gate is rebuilt from scratch.** `HdlExporter`'s `EXPORTED`/`SKIPPED`/`TOPOLOGY` sets
  (`src/jls/hdl/HdlExporter.java:421-437`) already reject `Memory`, `SubCircuit` and `RegisterFile` by name, already
  pinned by `HdlPolicyTest.memoryIsRejectedByName`. Riding the visitor makes P5 and P6 fall out of an existing,
  tested policy instead of a new one.
- **Extractor fidelity becomes the top false-pass risk and nothing tests it.** lf-04 names the mitigation: a
  differential fuzz test generating small random circuits, exhaustively simulating through `BatchSimulator` and
  comparing against the AIG's own evaluation — the `GenerativeRoundTripFuzzTest` pattern applied to semantics.
  There is no such prediction anywhere in P3–P13.
- **Two structural readers drift.** Whichever of `-export` and `-equiv` is fixed first for a given element, the other
  silently disagrees, and the disagreement is invisible because they have no shared oracle.

## 3. The risk framing is inverted: the dangerous answer is unguarded

The Abstract says "the riskiest surface is not the solver: it is exit statuses 4 (unknown) and 5 (not checkable)."
lf-04 says the opposite and gives the reason: "**UNSAT (equivalent) is the dangerous answer** — it is the one that
says *your circuit is correct*", mitigated by a **DRAT proof log and a checker**, so "the trusted computing base for
a passing grade shrinks to the proof checker plus the extractor." It also observes that SAT is "self-checking, for
free, using JLS itself."

#483 builds an elaborate mandatory confirmation apparatus (stage 3, H4, T2, P4, three completion criteria) around the
answer that was already self-checking, and ships nothing at all for exit 0. The word DRAT does not appear. P3 says
"with the proof object retained" and no criterion inspects it. A bundled, freshly written CDCL solver whose UNSAT
answers nobody checks, driven by a freshly written extractor nobody fuzzes, is a false-pass machine — and lf-04 is
blunt about the stakes: "a single publicised false pass is worse than not having the feature."

Statuses 4 and 5 matter, and P13 is a good test. But they fail *loudly* in the direction of refusing to grade. UNSAT
fails silently in the direction of passing everyone.

## 4. The alternative framing: enumerate first, solve second

**The out-of-the-box route.** For the audience this issue names — instructors grading first-year combinational
labs — equivalence does not need SAT at all. Run both circuits through `BatchSimulator` on every input pattern.
Digital already does circuit → truth table by enumeration; lf-04 calls it "the right idea stopping at 2^20". For
JLS's stated audience, 2^20 is not a stopping point, it is the whole syllabus.

What an exhaustive engine dissolves, item by item against this issue's own contents:

| #483 machinery | Under enumeration |
|---|---|
| `jls.formal.Aig`, `Tseitin`, bundled CDCL, licence question (Open Question 1) | does not exist |
| α's partiality; `Memory`/multi-driver/comb-loop → status 5 (P5, P6) | **checkable**, because the simulator runs them |
| stage 3 confirmation, H4, T2, "unconfirmed counterexample is the worst output" | true by construction — the counterexample *is* a simulation |
| extractor fidelity risk, missing fuzz oracle | no second semantics exists to diverge |
| DRAT, proof checking | TCB is the simulator the student already runs |

The trusted computing base for a pass becomes "the same engine that ran your circuit", which is a claim a first-year
can be told and an instructor can believe. It also grades the elements the AIG path refuses — `Memory`, `SubCircuit`,
tri-state buses, `ShiftRegister` — which lf-04 identifies as the difference between "a feature and a demo".

The honest limit is width, and it is the right limit to have: above roughly 20–22 free input bits the enumerator
returns status 4 with "input space too wide to enumerate (2^n patterns); use the solver path", which is exactly the
honest lattice #483 wants, arrived at without inventing a package. That escalation point is where SAT genuinely earns
its keep (a 32-bit ALU miter is instant for CDCL and unreachable for enumeration) — and when the solver arrives, the
enumerator is already sitting there as the differential oracle lf-04 asked for. **Build the cheap engine first and the
expensive engine gets its correctness argument for free.**

Coverage composes the same way: an exhaustive run *is* the exact uncovered set. §7.8's "build the AIG once and both
fall out of it" is the issue's stated reason for bundling formal with coverage — and it is not delivered, since no
prediction P3–P13 exercises the uncovered-set query. Enumeration delivers it for the same widths, with no AIG.

## 5. A property is a circuit, not an element

H1 already says the right thing — "a property is assembled out of gates and needs no new expression language" — and
then stops one step short. If a property is gates, it is **a circuit with one output pin**, and JLS has a first-class
artifact for that. lf-04 already uses this exact shape for the care set: `-assume valid.jls`, "a circuit over the same
inputs whose single output must be 1."

Taking `-assert prop.jls` / `-assume valid.jls` instead of two new element types costs nothing and removes:

- the ~16-place registration tax, twice, that `ARCHITECTURE.md`'s "Adding an element today (the honest list)" spells
  out, paid before #78/#372 collapse it;
- two palette rows and therefore **the #482 blocker entirely** — the dependency edge disappears rather than being
  waited on;
- the `.jls` format hazard. #483 accepts that an older reader refuses the file by name, which is loud but total: a
  student who drops an `Assert` on a lab circuit makes the whole file unloadable in the instructor's older JLS, for
  an element whose entire semantics is observation-only;
- two `HdlExporter` policy-bucket decisions and two `SaveTags` rows, on a registry #372 is trying to inventory.

`Cover` is weaker still. sweep-04 change D's four measures are all **automatic** over structures that already exist —
nets, FSM transitions, truth-table rows, mux selects. A user-drawn `Cover` element is #483's own addition, owned by no
roadmap document, and its bin label is a named probe by another name. If a drawn assertion is wanted later for
in-simulation severity and messages, that is change B, it needs change A first, and it deserves its own issue with
change B's full port and attribute list rather than the one-line stub in §7.4.

## 6. What the issue omits that the arc actually needs

- **Care sets / don't-cares.** lf-04 calls this "the single largest teaching change in the capability": a reference
  `TruthTable` with `-` outputs, a miter conjoined with a care predicate, and a student who exploits the don't-care to
  save gates **passes** with the report saying so. #483 has no `-assume`, no care predicate, no mention. It ships the
  version of grading that still punishes the Karnaugh-map lesson.
- **Counterexample minimization.** "About a hundred lines", and it is the difference between "here is one of 512
  failing inputs" and "here is the pattern that breaks it". Absent.
- **The drawing.** The counterexample overlay and the failing cone highlight are the *entire* competitive claim in
  lf-04 §"Competitive position". #483's GUI deliverable is two palette rows in a non-default view, and the overlay is
  not even filed as a successor. The engine ships and the differentiator does not.
- **The printers.** AIGER/BTOR2/SMT-LIB are how a class gets from "JLS proved my adder" to "Yosys proved my adder",
  and they are the delegation stance `ARCHITECTURE.md` already records. #483 has an in-jar solver and no escape hatch.

## 7. Scope

sweep-04 and lf-04 price the contents of this one "task" at roughly change A (2–3) + change B (2–3) + change D (2–3)
+ the formal floor (8–11) = **15–20 maintainer-weeks**, filed as a `tier: task` behind two other tasks. The
consequence is not just schedule: the highest-value, lowest-risk deliverable in the whole issue — row coverage telling
an instructor "your vectors never exercised the carry case", plus the O3 `⊥` bin that fixes a real latent defect — is
held hostage to a bundled SAT solver with an unresolved licence question (Open Question 1) and an unguarded UNSAT path.

## Recommendation

I am disregarding the stated acceptance criteria, and specifically the "one task" framing, the `jls.formal` engine
choice, and the two new element types. Keep the end state verbatim. Change the route:

1. **Change A first.** Timestamp closure and the `afterTimestamp` sampling region, proved non-disturbing against the
   existing goldens. Everything downstream is noise without it.
2. **Coverage alone, on that hook.** The four measures, the `⊥` row bin (O3/P8), transition-dominates-state (P10),
   off by default with P11's event budget. No formal, no new element types, no #482 edge. This is shippable value in
   ~3 weeks and it is what instructors will use most.
3. **Exhaustive equivalence.** `-equiv ref.jls` by enumeration through `BatchSimulator`, with the honest width gate
   returning status 4, and `-cex` writing the `-t` vector that produced the disagreement. Statuses 0/3/4 arrive with
   no solver, no AIG, no licence question, and a TCB the audience already trusts.
4. **Properties as circuits.** `-assert prop.jls` and `-assume care.jls` over the same engine. Care sets — the largest
   teaching change — become available here, not after `Bits4`.
5. **Then `jls.formal`**, as the escalation path for widths enumeration cannot reach, built on
   `HdlModel.StatementVisitor`, validated against the enumerator as a differential oracle, with DRAT on the UNSAT path
   and the AIGER/BTOR2/SMT-LIB printers as the delegation escape hatch.
6. **File the counterexample overlay and cone highlight** as their own issue now. Without it, this is a CLI formal
   tool competing with Yosys, which lf-04 correctly says JLS should not do.

Two things in #483 should survive intact and be carried into whichever issues replace it: **"4 and 5 are never
passes", asserted through the grading harness (P13)**, and **the `⊥` "no row matched" bin (P8)**, which turns a
silent defect into a number. Both are exactly right.
