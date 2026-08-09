# Issue #566: FEAT-C31-4: the shipped state-machine element measures up to Digital/DEEDS/Issie's FSM design workflow — every named gap closed or refused in writing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-31 (#515) states the end plainly: *"designs a state machine as states and
transitions that becomes registers and logic."* #566 then reduces that end to a
competitor checklist — enumerate Digital/DEEDS/Issie FSM features, close or
refuse each, mandate a document as the deliverable, and offer a "verified-close"
escape (KC-31-2) if the element is already at parity. The reduction throws away
the interesting half of the sentence. **"Becomes registers and logic" is the
pedagogical payload, and no acceptance criterion in #566 requires JLS to produce
the registers and logic.** AC-3 asks only that the FSM element's trace match a
circuit a human already built by hand — i.e. it tests fidelity of a black box,
not the abstraction reveal that makes the feature worth teaching with.

## Ground truth: what JLS actually ships today

- A real graphical state-diagram editor already exists (`StateEditor` inside
  `/home/user/JLS/src/jls/edit/StateMachineDialog.java`, 1929 lines): place
  states, drag multi-segment transitions, mark initial, align, per-state output
  editing. Help pages `resources/help/elements/stmach/{stmach,states,transitions,outputs}.html`.
  Diagram *entry* is not the gap; the assessment will find that quickly.
- The simulation model (`/home/user/JLS/src/jls/elem/StateMachine.java`) is an
  opaque primitive — help calls it "an imaginary state register." Nothing is
  ever synthesized into elements the student can open.
- Outputs live on states only (`State.Out`), so **Moore-only**. `HdlModel` even
  names the record `MooreOutput` (`/home/user/JLS/src/jls/hdl/HdlModel.java:749`).
- Transition conditions are one signal `==`/`!=` one literal, plus
  `unconditional` and `else` (`State.Transition`,
  `/home/user/JLS/src/jls/elem/State.java:121-134`). No conjunctions, no
  expressions. Digital takes arbitrary boolean expressions.
- `StateMachine.canCopy()` returns `false` — a state machine cannot be
  copied/pasted or reused. Against #510's "hierarchy/reuse 2/5" this is on the
  loudest axis JLS already loses.
- **The FSM→registers-and-logic analysis already exists, headless, in the wrong
  place.** `HdlExporter.buildStateMachine`
  (`/home/user/JLS/src/jls/hdl/HdlExporter.java:866`) computes binary state
  codes, the initial code, canonical transition ordering, and the full Moore
  output table, then hands `HdlModel.StateMachineStatement` to the Verilog and
  VHDL emitters. `test/resources/hdl/statemachine_else.v` shows the result: a
  state register plus a next-state case plus an output decoder. That *is* the
  synthesis CAP-31 wants, expressed in text instead of in elements.
- **A genuine semantics defect the parity lens will not surface.** `State.trans`
  is a `HashSet` (`State.java:31`) and `getNextState()` (`State.java:1272`)
  returns on the first matching condition *in hash iteration order*. When two
  conditions from one state match simultaneously, the successor is
  nondeterministic across runs. The HDL exporter knows: the generated comment in
  `test/resources/hdl/statemachine.v` admits "JLS picks among simultaneously
  matching conditions in hash order, this export tests them in canonical (#180)
  order." So JLS's own simulator and JLS's own Verilog export can disagree on
  the same file. #180 canonicalized *save* order and stopped there. For the
  project whose single 5/5 axis is testing/grading and whose flagship claim is a
  normative, golden-pinned semantics (`docs/simulation-semantics.md`), a
  nondeterministic element is worth more than any parity checkbox.

## The reframing: one FSM IR, four consumers

Do not build an assessment that ends in a document plus scattered editor
patches. Cut the seam where the code already wants to be cut.

**Lift `buildStateMachine`'s output into a first-class headless value type** —
call it `jls.fsm.FsmModel`: states with encodings, a transition relation, an
output function over (state, input), the initial code, and the issue-#98
hold-on-no-match rule. It is pure data, AWT-free, and satisfies the
`HeadlessCoreRatchetTest` discipline by construction. Then give it consumers:

1. **The HDL emitters** (existing). `HdlExporter` stops owning FSM analysis and
   becomes a translation of `FsmModel`. Net line count goes *down*.
2. **A batch surface** — `-fsm` printing the state/transition/output tables plus
   a reachability and determinism report (unreachable states, states with no
   outgoing transition, overlapping conditions). That single flag discharges
   #566's AC-4 and CAP-31 AC-5 honestly, and it is an FSM oracle for autograders
   that *no competitor ships* — it lands on JLS's only 5/5 axis (#510 §2)
   rather than on an axis where JLS is chasing.
3. **"Explode to circuit"** — one editor action that replaces the FSM element
   with a Register plus drawn next-state and output logic, laid out by the #62
   `HeuristicLayeredLayouter` that already exists. This is the CAP-31 sentence,
   literally delivered, and it is the demo screenshot #510 §4 gate 1 says JLS is
   losing evaluations for lack of.
4. **Truth-table / minimized-expression views** of next-state and output logic —
   free, because PF-1/PF-2 will already have the table and Quine–McCluskey
   machinery, and `FsmModel` is exactly their input shape.

Under this framing PF-4 stops being a checklist appended to CAP-31 and becomes
the piece that makes CAP-31 *one capability instead of four*: the same extractor,
the same minimizer, the same synthesizer, the same layouter, reached from the
FSM path. That is the elegance argument #510 §5 says JLS must be able to survive
a repo inspection on.

## Second reframing: make AC-3 structurally true instead of testing it

I am explicitly disregarding AC-3 as written. "Simulates to the same trace as its
hand-built register-and-logic equivalent" makes a human-authored fixture the
oracle for a black box. Invert it: **the synthesized circuit is the definition**,
and the fast primitive is an optimization checked against it. The project already
ratified exactly this discipline for a different subsystem — ARCHITECTURE.md's
#221 decision binds any future evaluation strategy to be "observably identical"
to the event model with a differential golden. Apply the same pattern here:
`FsmModel` → generated circuit → differential golden vs. the primitive, over
generated input sequences. Then Moore/Mealy, condition richness, and encoding
choice all inherit correctness instead of each needing its own hand-built fixture.

That also dissolves two "gaps" the parity assessment would otherwise log as work
items. **Mealy is not a mode.** If the output function in `FsmModel` is over
(state, input), Moore is the degenerate case where the input argument is unused,
and the editor change is "outputs may be attached to a transition" — no
mode switch, no doubled UI, no doubled emitter path. **Multi-signal conditions
are not a new grammar.** PF-2 will define an expression syntax for minimized SoP
output; transition conditions should parse *that same grammar*, one parser for
the project, also reusable by the TruthTable element. Two features, one seam.

## Where the issue pulls against the trajectory

- `ordering_after: []` is wrong under any framing that shares machinery. The
  elegant route depends on PF-2's expression/minimizer work and on #62's
  layouter; sequencing PF-4 as independent guarantees a bespoke FSM
  expression parser and a bespoke FSM drawing routine that later have to be
  deleted. ARCHITECTURE.md's standing complaint is precisely this shape — "if
  you find yourself doing this, read #78 first."
- The boundary note demotes #290 to "adjacent... a layout task." #290's golden
  trio is literally an ALU slice, a counter, and **a small FSM**, with a 4x
  bounding-box compactness bound. Under the explode-to-circuit framing that
  golden is the reference deciding whether the synthesized FSM circuit is
  legible to a student. It is on the critical path, not adjacent.
- KC-31-2's verified-close path is dead on arrival, and pretending otherwise
  distorts the band. Moore-only, single-signal conditions, no copy/paste, no
  headless surface, and hash-order nondeterminism are five named gaps found in
  an hour of reading. "2–3 mw" is plausible for the assessment plus the
  determinism fix plus the batch table; it is not plausible for that *plus*
  Mealy plus expressions plus synthesis. Split it: the reframed work is two
  issues, not one.

## Concrete alternative slicing

- **Slice A (~1 mw, ship first):** make transition selection deterministic
  (canonical order, matching #180 and the exporter's already-canonical order),
  state the priority rule in `docs/simulation-semantics.md`, add the
  overlapping-condition diagnostic, and fix `canCopy()`. Pure correctness and
  reuse on JLS's strongest axes; needs no parity document to justify itself.
- **Slice B (~1–2 mw):** extract `jls.fsm.FsmModel` out of `HdlExporter`, add
  `-fsm` tables + reachability/determinism report. AC-4 discharged; HDL export
  refactored, not duplicated. The parity document now writes itself as a
  byproduct, because the IR forces every capability question to be answered
  explicitly.
- **Slice C (~2–3 mw, after PF-2/#62):** Mealy via transition-attached outputs,
  PF-2's expression grammar for conditions, and "explode to circuit" with the
  #290 FSM golden as the compactness reference and the differential trace as the
  correctness oracle.

## Verdict

**endorse-with-reframing.** The instinct — stop guessing and measure the shipped
element against what instructors actually use — is right, and #510 §2 backs it.
But keep the document as a *byproduct* of extracting the FSM IR, not as the
deliverable; replace AC-3's hand-built-fixture oracle with a synthesized-circuit
differential; add the missing outcome ("becomes registers and logic") that
CAP-31 promised and #566 dropped; sequence after PF-2 and #62 rather than
declaring no ordering; and treat the hash-order transition nondeterminism as the
first thing to fix, because it is a defect in the one property JLS sells.
