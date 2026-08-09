# Issue #515: CAP-31: a truth table becomes a minimized drawn circuit and a drawn circuit becomes its truth table and minimal expressions — and an FSM is designed as states, not gates
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not truth tables. The stated end is instructor conversion: `#510 §2` names circuit
analysis as a JLS loss against Digital/DEEDS/Issie, and the Digital teardown calls
"parameterization + FSM + synthesis parity" the minimum bar. So CAP-31 is an
**objection-removal capstone**, not a differentiation one. That matters, because the
project's own strategy document says so explicitly and in the opposite direction:
`docs/capability-roadmap/AMENDMENT.md:634-636` calls circuit→truth-table "the
near-miss, the right idea stopping at 2²⁰," and ranks proof-based grading (CAP-09,
#306) as leapfrog #1. CAP-31 closes a checkbox an instructor scans for; CAP-09 is
why they stay. Both are worth doing. But the issue is written as though CAP-31 owns
an algorithm stack, and it should be written as though it owns a **set of views and
lowerings over machinery the tree either already has or is already funding.**

## Three things already in the tree that this issue plans to build again

1. **The circuit walk exists.** `src/jls/hdl/HdlExporter.java` already elaborates the
   whole element graph into a semantic IR (`HdlModel`) with a declared refusal map
   (`:460-477`, `{Memory, SubCircuit, RegisterFile, FieldExtend}`) and an accepted set
   that includes `TruthTable`, `StateMachine`, `ShiftRegister` (`:428`). #306 already
   observes that its formal extractor "inherits the exporter's refusals." CAP-31 now
   proposes a third consumer of a walk it describes as CAP-09's. The real hazard is
   not building the extractor twice — the ordering row guards that — it is **three
   refusal policies drifting apart**, so `-export`, `-equiv` and `-truthtable` disagree
   about which circuits are analyzable. Cut the seam once: one elaboration, one
   refusal map, three backends (HDL statements, AIG/CNF, cofactor enumeration).

2. **"Netlist becomes a drawn circuit" exists and is tested.**
   `NetlistImporter.importNetlist` (`src/jls/hdl/imp/NetlistImporter.java:70`) runs
   validate → `Builder` → `HeuristicLayeredLayouter` → **JLS save text**, and #62's
   `LayoutInvariants`/`LayoutMetrics` already pin grid, orthogonality, overlap and
   readability. PF-3 says it "uses the existing element palette and the HDL-import
   layouter lineage (#62) where available" — that hedge is where a second drawing
   generator gets born. Make it binding instead: **PF-3 emits a two-level cover in
   netlist shape and hands it to the existing importer path.** AC-2's round-trip,
   grid-locking, and "indistinguishable from hand-drawn" all come free, proven by
   #62's rubric rather than by new goldens. If `Builder` is too Yosys-JSON-specific,
   the right move is a one-week refactor of `Builder` to a netlist-shaped input —
   which also serves #202 and #73, the other netlist producers #62 already names.

3. **FSM → registers and logic is already implemented — in the wrong output format.**
   `HdlExporter.buildStateMachine` (`:866`) already assigns binary state codes in
   canonical order with the initial state at 0, splits each state's transitions into
   ordered conditional arms plus an unconditional/else shape, and lowers Moore outputs
   to a combinational case with unspecified outputs zeroed. That *is* state encoding
   plus next-state and output logic. PF-4 as written spends 2–3 mw writing a parity
   *assessment document*. The far better use of that budget: **render the lowering
   JLS already computes as a drawn circuit** through reframing (2). "Design as states,
   press a button, see the register and the gate cloud you would have drawn by hand,
   then simulate both and prove them equivalent with CAP-09" is a capability none of
   Digital, DEEDS or Issie offers — and it is mostly assembly, not invention.

## The reframing I would actually build: Collapse and Expand

The issue frames analysis as **views** — a truth-table window, an expression display.
JLS already has a `TruthTable` *element* (`src/jls/elem/TruthTable.java`, 1491 lines)
with a working table editor (`jls.edit.TruthTableEditor`, `TruthTableDialog`) that
simulates directly. So the elegant shape is not a report panel but **a pair of inverse
canvas refactorings**:

- **Collapse to behavior:** select a combinational region → it is replaced by a real
  `TruthTable` element with the extracted table.
- **Expand to gates:** select a `TruthTable` (or a `StateMachine`) → it is replaced by
  the synthesized, laid-out gate implementation.

Both are ordinary circuit mutations, so they ride `CircuitSnapshot` undo, save/load,
and the existing element palette with zero new UI surface. AC-2's round-trip becomes
"Expand then Collapse is the identity," checkable in one test. And the pedagogy is
strictly better than a read-only view: the student *holds the same circuit at two
abstraction levels on one canvas* and can simulate either. A report panel teaches
"the tool knows the answer"; Collapse/Expand teaches "these are the same thing."
This also gives CAP-09 a free gift — an extracted `TruthTable` is a reference model,
so `-equiv student.jls golden.jls` gets its golden from a drawing.

## Where PF-2 is aiming slightly wrong

KC-31-1's refusal of Espresso is right. But "minimized SoP expressions displayed and
exportable" is the *least* interesting output Quine–McCluskey produces. Two changes:

- **Ship the prime-implicant table, not just the winner.** The chart QM builds is a
  K-map by another name, and it is the artifact the course actually teaches with.
  `docs/capability-roadmap/README.md:190` already promises "mark `-`, watch the
  synthesizer exploit it, see the smaller gate count." The comment notes searches for
  "Karnaugh" found nothing — that absence is in this issue's own scope, not elsewhere.
- **Offer a hazard-free cover as a second selection.** `README.md:449-453` states the
  problem outright: hazard analysis is core K-map content whose motivation is a glitch
  the student cannot see, and the fix is *adding the consensus term* — a term the
  minimal cover deletes. A minimizer that only ever returns the minimum **actively
  fights the lesson the roadmap wants taught.** Emitting the consensus-closed cover is
  a small QM variant, and combined with the timing capstone it is a differentiator no
  incumbent in the survey ships: draw the minimal cover, see the glitch; draw the
  hazard-free cover, watch it vanish.

## The dependency the issue does not name

Don't-cares are destroyed today: `TruthTable.java:1447-1449` (`// don't care becomes
false` / `if (outValue == 2)` / `outValue = 0;`), and an unmatched row silently holds
outputs (`:1432`). #306 flags this as the motivating case for FEAT-026 (#322), the
four-state value core. Quine–McCluskey **without** honest don't-cares is a worse
minimizer producing bigger circuits, and PF-3's round-trip AC would then be
round-tripping a lie. CAP-31 should declare #322 a prerequisite of PF-2/PF-3 (not
merely `ordering_after: #306`), or state in writing that v1 minimizes over a fully
specified table and that `-` is refused at the boundary rather than silently zeroed.

## Interface: one verb, coordinated with CAP-09

AC-5 is correct and well-aligned with `docs/batch-interface.md` being a stability
contract. But #306 warns precisely about this: two capstones extending a public
lattice independently, and every script breaking. Do not add `-truthtable`,
`-minimize`, `-synth` as three ad-hoc flags beside CAP-09's `-equiv`. Design **one
analysis verb with structured (JSON) output** covering table, cover, expression, and
refusal reason, sharing CAP-09's verdict/exit-status extension. Graders then parse
one schema, not four stdout dialects.

## What I would change in the acceptance criteria

Keeping AC-1 and AC-3 as written. Replacing the rest:

- **AC-2 →** Expand-then-Collapse is the identity on a committed corpus, and the
  Expanded circuit passes `LayoutInvariants` and #62's `LayoutMetrics` thresholds —
  i.e. it is proven by the layout rubric that already exists.
- **AC-4 →** a drawn FSM Expands to a register plus gate cloud whose simulation is
  golden-identical to the `StateMachine` element's over an exhaustive input/edge
  sequence. A written parity document is a fine byproduct; it is a weak artifact as
  the criterion, and KC-31-2 lets 2–3 mw close on prose.
- **New AC:** exactly one elaboration walk and one refusal map serve `-export`,
  CAP-09's checker, and CAP-31's analysis — enforced by a test asserting the three
  agree on a corpus containing each refused element type.
- **New AC:** the minimizer emits the prime-implicant cover, and a hazard-free cover
  option exists or is refused by name with a successor issue.

## Verdict

**endorse-with-reframing.** The goal is right and the ordering discipline is good.
But as written CAP-31 reads like a fourth subsystem, when three quarters of it is
already sitting on master in `HdlExporter`, `NetlistImporter` and
`HeuristicLayeredLayouter`, and the fourth quarter is CAP-09's core. Rewritten as
"one elaboration, one netlist→drawing path, Collapse/Expand as inverse refactorings,
and a cover — not just an expression — as the minimizer's output," the same outcome
lands smaller, strengthens two other capstones instead of shadowing them, and turns
a parity checkbox into something the incumbents do not have.
