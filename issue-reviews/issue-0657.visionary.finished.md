# Issue #657: TASK-C566-1: the FSM-workflow parity assessment exists as a document — every Digital, DEEDS and Issie capability listed against what the shipped element does today
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#657 is a decision instrument, not a deliverable. Its real job is to decide
whether the 3–4 mw of #658/#659/#660 should be spent on the state-machine
element at all. Everything else — the rows, the citations, the `docs/` landing
site — is scaffolding for that one call. Judged as a decision instrument, 1 mw
is cheap and the document should exist. Judged on *what it measures*, the
issue picks the wrong axis, and the wrong axis is load-bearing because
KC-31-2 lets a "parity" finding close the entire feature.

## The reframing: measure CAP-31's outcome, not competitors' feature rows

CAP-31 (#515) states the outcome in its own title: an FSM "is designed as
states, not gates," and per its Outcome paragraph, one that "becomes registers
and logic." #566 repeats the phrase. #657 then throws that away and enumerates
"state-diagram entry, transition conditions, Moore and Mealy output modes"
against three competitors.

Grounded in the code, those two framings give opposite answers:

- On the competitor axis JLS looks close to parity. `StateMachineDialog`
  (`/home/user/JLS/src/jls/edit/StateMachineDialog.java`) is a real graphical
  state-diagram editor: a painted `editArea` with drag-to-draw transitions,
  rubber-band selection, popup menus for `make initial state` / `add transition`
  / `edit outputs`, and `align states horizontally|vertically`. `StateRenderer`
  draws the circles; `CircuitRenderer:266-268` even prints the diagram and an
  output summary. Transition conditions, unconditional and "if no other
  condition" arms all exist (`State.Transition`).
- On the CAP-31 axis JLS is at zero. The element is a black-box simulation
  primitive: `StateMachine.react` walks transitions directly, and the only
  place it ever becomes hardware-shaped is `HdlExporter.buildStateMachine`
  (`src/jls/hdl/HdlExporter.java:866`) emitting a clocked case statement to
  Verilog/VHDL. **Nothing in JLS ever turns an FSM into registers and gates on
  the canvas.** The student never sees states become hardware, which is the
  entire pedagogical claim.

So #657 as written can honestly return "parity," #566 closes verified under
KC-31-2, and CAP-31's headline outcome is still unimplemented with the capstone
believing PF-4 is done. That is the failure mode this issue should be designed
to prevent, and instead it is the failure mode this issue makes most likely.

## The design that makes the problem mostly disappear

CAP-31 already plans PF-1 (combinational subgraph → truth table), PF-2
(Quine–McCluskey + expressions), PF-3 (truth table → drawn, laid-out circuit).
JLS already ships a `TruthTable` element with its own editor, and already owns
the layout spine: `src/jls/hdl/layout/` (`HeuristicLayeredLayouter`,
`LayoutMetrics`, `LayoutInvariants`) built for #62/#61.

An FSM's next-state and output functions *are* a truth table over
(encoded state ⊕ inputs). So the missing capability is not a checklist row —
it is one command:

**"Expand state machine to circuit"** — enumerate the state table from
`States`/`Transitions`, choose and *state* an encoding, hand the table to PF-3's
synthesizer, hand the result to `HeuristicLayeredLayouter`, drop the output in
as a `SubCircuit`.

This is composition, not new machinery. It makes PF-4 the fourth consumer of a
spine CAP-31 is already paying for, exactly as the capstone's own
`ordering_after` note argues for PF-1 ("one extractor, two consumers; do not
build twice"). It also hands #659 its test for free: the expanded circuit must
trace-match the black-box element on the same stimulus — a golden that writes
itself and that no competitor-parity row would have produced.

**I am explicitly disregarding AC-3.** AC-3 rules that layout is "adjacent
evidence, not part of this assessment," quarantining #290's small-FSM layout
golden. That is precisely the seam where the good design lives. Under the
expansion framing, the small-FSM golden stops being adjacent and becomes the
acceptance artifact. AC-3 draws the boundary that hides the answer.

## The second pull against the project's arc: the walled garden

`StateMachineDialog` is 1,929 lines; `State` is 1,542; `StateRenderer` and
`StateMachineRenderer` add 703 more. That is a **second editor** inside JLS,
with its own canvas, its own selection model, its own drag state machine, its
own alignment commands, its own print path — none of it shared with
`SimpleEditor`, and none of it reachable from the project's newer seams.
`jls.collab.op.ElementVocabulary:45` lists `"StateMachine"` as a single opaque
token: states and transitions have no operations, so no undo granularity, no
collaborative editing, no scripting, no headless authoring.

This matters for #658. If gaps are closed the cheap way — another `JMenuItem`
on that dialog — the feature makes JLS *worse* on the axis #510 §2 names as its
strongest under-leveraged card ("elegance of implementation"), while #510 §5's
dev-draw play is simultaneously trying to decompose `SimpleEditor` so the
elegance pitch survives repo inspection. The assessment must name this, because
nothing downstream will. Minimum ask: any gap #658 closes routes state and
transition edits through the operation layer (`docs/operation-layer.md`) rather
than growing the dialog's private world.

## Gaps the checklist frame will likely miss

1. **Conditions are one signal against one value.** `State.Transition` carries
   `signal`/`bits`/`equal`/`value` — equality or inequality on a single signal.
   No compound boolean conditions. A "transition conditions: present" row would
   be true and useless.
2. **Unmatched-transition reporting is a one-shot dialog warning.**
   `StateMachine.java:762-778` fires `TellUser.warn` once per run behind
   `noMatchReported`, with no severity, no count, no machine-readable channel —
   the exact deficiency `docs/capability-roadmap/sweep-04-verification.md:191`
   catalogues. In batch mode this is a *silent* FSM misbehaviour on JLS's only
   5/5 axis (grading, #510). A hundred students hitting the same dead state get
   nothing in the autograder. That is worth more than any Digital UI row and no
   competitor-parity enumeration will surface it.

## The cheaper thing that may beat all of it

#510's DEEDS row names the winnable segment as "DEEDS instructors at forced
migration moments" and the minimum bar as a textbook-mapped lab pack plus
"chronogram + **FSM tutorial content**" — not FSM capability parity. And
`examples/` contains exactly one entry (`autograde`): no FSM circuit ships that
a user can find, confirming #510 §4's shop-window gate.

So the concrete alternative for #657's own 1 mw: **build the same FSM in JLS,
Digital, DEEDS and Issie and write the transcript** — every step, every place
JLS made you do something the others did not. It still lands in `docs/`, it
still yields a numbered gap list for #658, it is not self-graded against an
author-chosen checklist, and it throws off the missing example circuit and the
missing tutorial page as byproducts. Documentation-by-use finds gaps that
documentation-by-checklist cannot, because the checklist is written by someone
who already knows the answer they can defend.

## Recommendation

Keep the task, change the axis. (1) Make the CAP-31 outcome — states become
registers and logic on the canvas — a mandatory row, and forbid a parity
finding while it is absent. (2) Drop AC-3's layout quarantine; fold the
expansion-and-layout design into the assessment. (3) Add the operation-layer
constraint as a standing note for #658. (4) Prefer the comparative-transcript
form over the capability matrix, or run the matrix as an appendix to it.
