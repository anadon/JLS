# Issue #658: TASK-C566-2: every named FSM gap is closed or refused in writing — no item leaves the assessment without a disposition
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

#566 wants instructors to believe JLS's state-machine element is real tooling, not a
box on a palette (#510 §2 names FSM tooling as a JLS loss). #658 is the *disposition*
step of a four-issue pipeline — #657 writes the gap list, #658 dispositions it, #659
tests the closures, #660 makes the survivors headless.

Read as a claim about what JLS should become, #658 says: *the way we get a credible FSM
story is a competitor checklist, walked row by row.* That is the part I want to
challenge. I am explicitly disregarding this issue's acceptance criteria, all four of
which are about the bookkeeping of a list rather than about any capability, and AC-2 of
which makes "file a new issue" a valid disposition — so #658 can complete with zero
change to `StateMachine`, having converted a list into a list of issue numbers.

## The assessment's answers are already in the source

The pipeline defers every load-bearing decision into a document nobody has written.
But the structural gaps are not discoverable-by-survey; they are visible in an
afternoon's reading, and they are three:

1. **Moore-only, by data model.** `State.Out` hangs off `State`
   (`/home/user/JLS/src/jls/elem/State.java:29,89`); `State.Transition`
   (`:121–135`) carries no outputs. Mealy is not a row to be researched — it is a
   field that does not exist. The HDL exporter already concedes it by name:
   `StateMachineStatement.MooreOutput`
   (`/home/user/JLS/src/jls/hdl/HdlExporter.java:935`).
2. **One-signal conditions.** A transition is `signal`, `equal`/`!equal`, `value`,
   plus `unconditional` and `other` (`State.java:123–134`). `A·B̄` is inexpressible;
   the student must draw an AND gate and feed a synthetic 1-bit input back into the
   machine — which is precisely the "designing in gates" the feature says it abolishes.
3. **The two lowerings already disagree.** `State.getNextState`
   (`State.java:1278`) iterates `trans`, a `HashSet`, and returns on first match;
   `HdlExporter.buildStateMachine` walks `getTransitionsInSaveOrder()` and emits an
   ordered chain. On a machine with two simultaneously-true conditions, the simulator's
   winner is hash-order and the exported Verilog's winner is save-order. They can
   differ, and the ordering is unspecified in `docs/simulation-semantics.md`. That is a
   live correctness bug sitting inside the very element #566 is assessing, and no
   checklist against Digital/DEEDS/Issie will surface it, because it is not a missing
   feature — it is a seam.

Point 3 is the tell. JLS states FSM semantics twice: behaviorally in
`StateMachine.react` (`StateMachine.java:722–806`) and structurally in
`HdlExporter.buildStateMachine` (`HdlExporter.java:866+`, plus
`HdlModel.StateMachineStatement` and a case in each of `VerilogEmitter`/`VhdlEmitter`).
#659 AC-3 then demands *hand-built committed circuits* — a third independent statement
of the same machines, required by that AC to be built by a different code path. #660
would add a fourth reader. Four statements of "what an FSM means," maintained by hand,
kept in agreement by a test suite. That is the architecture this pipeline is walking
toward, and the gap list will never name it.

## The reframing: one lowering, exposed to the student

Make the elaboration first-class *inside JLS*. Add **expand-to-circuit**: a
state machine lowers into ordinary JLS elements — a `Register` holding the state code,
next-state logic, output logic — using exactly the encoding
`buildStateMachine` has already pinned down (binary codes, initial state = 0, canonical
`(name, x, y)` order, `stateBits = ceil(log2 n)`). The specification is written; it is
just aimed at Verilog instead of at JLS's own element model.

What that buys, against the project's actual trajectory:

- **The pedagogical payload of #566 stops being a slogan.** The feature body says a
  machine "becomes registers and logic." Today it does not — it is simulated as a
  monolith, and the only way to see the gates is `-export out.v`, i.e. in a language the
  first-year student this tool exists for cannot read. "Show me what my state diagram
  actually is" is the single most valuable thing Digital does in this space, and it is
  one row among twenty on any parity checklist.
- **#659 becomes a property, not a fixture-authoring project.** Trace-equivalence
  between the black-box element and its expansion, over generated machines — with the
  expansion simulated by the ordinary engine, satisfying AC-3's "not the same code path"
  honestly, and with no hand-drawn goldens to drift.
- **#660 largely evaporates.** The machine-readable FSM artifact is the expanded
  circuit, written through `-savetext` under `docs/file-format.md`'s existing contract:
  byte-deterministic already, no new schema, no new flags to keep stable under #524.
- **The HDL exporter shrinks.** An expanded FSM exports as the gates it became. The
  `StateMachine` special case (`HdlExporter.java:428,704,866`, the model statement, and
  the FSM case in both emitters) can eventually be deleted — a net-negative diff across
  two target languages, and the ordering divergence in point 3 disappears because there
  is one chain, not two.
- **It pays the FSM tax once.** `docs/capability-roadmap/` already routes through this
  element repeatedly: FSM-transition coverage (`sweep-04-verification.md:340`),
  assertions on FSM state (`:170`), sequential elements cut out of compiled evaluation
  (`lf-02-compiled-evaluation.md:256`), FSM state in causal-debug snapshots
  (`lf-03-causal-debug.md:263`), "prove this FSM cannot reach the error state"
  (`lf-04-formal-and-grading.md:480`), the JTAG TAP (`sweep-06:87`). Every one of those
  subsystems must special-case a black box, or get the FSM free from ordinary elements.
  That is the alignment argument, and it is worth more than parity with three
  competitors.

Two guardrails, stated so this is not read as more than it is. Expansion must be
**optional and one-way**: keep the black-box element as the simulation path (it owns
`propDelay`, `busy`, and the edge semantics students are taught), and let expansion be a
lowering the user asks for and then edits as gates. The moment anyone proposes live
bidirectional sync between diagram and expansion, JLS has started building a synthesis
IDE it cannot staff. And expansion does not by itself fix gaps 1 and 2 — Mealy outputs
and multi-signal conditions are still model changes — but it makes both *cheap*, because
the lowering absorbs them rather than each subsystem learning about them.

## Cheaper alternative, if the parity story must be kept

Fold #658 into #657. The split exists only because the disposition "cannot be sized
before TASK-C566-1 lands" — but #658 AC-4 already requires the dispositions to be
written back into #657's document, so two issues produce one file, the second editing
the first. That is a seam cut through a document, not through the work. Land
`docs/fsm-workflow-parity.md` with its dispositions inline, then cut **one issue per gap
actually being closed**. Gaps are the natural unit; "the disposition pass" is not.

## Verdict

**redirect.** The end #566 serves is right and well-aligned; #658 as written is a ledger
that cannot fail and cannot ship anything. Redirect its budget to the one architectural
move — expand-to-circuit, on the encoding `HdlExporter` already fixed — that closes a
large share of the eventual gap list by construction, gives the student the artifact the
feature promises, and stops JLS from acquiring a fourth hand-maintained statement of
what a state machine means. Fix the `HashSet` transition ordering
(`State.java:1278`) first and specify it in `docs/simulation-semantics.md`, because
neither the assessment nor its disposition will find it.
