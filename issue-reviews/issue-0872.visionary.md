# Issue #872: TASK-C563-0: the combinational-cone extractor exists as one callable pass — frontier identification and a named refusal on sequential or feedback content
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two capstones on different trajectories — CAP-31's truth-table/minimization teaching arc
(#515 → #563 → #641/#642) and CAP-09's formal grading arc (#306: AIG, CNF, miter, `-equiv`)
— both need one thing: **a defensible answer to "where does combinational logic end?"**
Both declared it, neither filed it, and each told its executor not to build it. Filing the
component instead of reporting the gap is the right instinct, and the "Why this is filed
here" section is the best piece of tracker hygiene I have read in this repo.

But the issue answers a narrower question than the one both consumers actually ask. It
delivers a *boundary* (element set + frontier) and a *refusal*. Neither consumer can act on
a boundary alone: #641 needs an output value per input assignment, #306 needs a Boolean
formula per element. The shared claim — "both consumers call the same code and cannot
disagree about what 'combinational' means" — is therefore only half-delivered by this
design. What they will disagree about is not the boundary; it is the semantics inside it.

## Reframing 1: the shared artifact is a classification, not a pass

AC-2 already gropes toward this ("derived from a registry-keyed table, not hand-listed in
the extractor"). Take it one step further and the extractor stops being a component at all.

Put the behavior class **on `ElementType`** (`src/jls/elem/ElementType.java`), the descriptor
`ElementRegistry` already keys by tag, and whose totality `test/jls/ElementRegistryTest.java`
already enforces. `docs/grand-architecture.md` §5 calls that registry "the mechanism,
generalized" and the seed of the module system; ARCHITECTURE.md's "Adding an element today"
list exists precisely to be collapsed into it (#78). Then:

- **cone extraction is a query, not a pass**: reachability over #468's `NetPartition`,
  stopping at non-combinational nodes, plus a cycle check. Forty lines, no new concept.
- **three existing private tables collapse into it**: `HdlExporter.EXPORTED` / `SKIPPED` /
  `TOPOLOGY` (`src/jls/hdl/HdlExporter.java:422-437`) are the same classification, hand-listed
  by `Class<?>`, and `jls/hdl/yosys/CellValidator.SUPPORTED` is a fourth. #872 as written adds
  a fifth. That is the exact pathology #468 was filed about, one level up the stack.
- **it is the only shape that survives #212.** An external element provider's element cannot
  be added to a hand-list living inside a cone extractor. It must *declare* its own behavior
  class or it can never be enumerated, exported, or checked. Homing the classification on
  the descriptor is not tidiness; it is the difference between a capability that extends and
  one that fossilizes at 35 built-in types.

The mechanical precedent is already in tree: `test/jls/elem/PinFaceContractTest.java` walks
`ElementRegistry.all()`, asserts a per-type contract, and pins its exemptions
(`KNOWN_UNINITIALIZABLE`, `KNOWN_PUTLESS`) as documented sets. That is exactly the discipline
AC-2 asks for, and it should be cited as the pattern rather than re-invented.

## Reframing 2: the classification is not a boolean, and AC-2's list is wrong

I am disregarding AC-2's enumerated element list as written. It is not a rounding error; it
contradicts the project's own normative documents and it omits the hardest case.

- **`ShiftRegister` is combinational.** `docs/file-format.md:316` — "combinational barrel
  shifter … despite the name, stateless"; `docs/simulation-semantics.md` §6.3 — "There is no
  clock, no stored value, and no reset." Refusing a cone because it contains one refuses
  correct work and puts the extractor in conflict with the normative spec. This single error
  is the strongest argument for the reframing above: a hand-list at the point of use *will*
  drift from the semantics doc; a field on the descriptor sits next to the element.
- **`TriState` and `DelayGate` are not state.** TriState is a combinational driver with a HiZ
  output (`docs/simulation-semantics.md` §2, §9); DelayGate is logically neutral
  (`BatchSimulationGoldenTest.delayGatePassesValueThrough`). The real reason to refuse a
  tri-state cone is that HiZ is not a Boolean — a *value-domain* refusal, which is what #306's
  uncheckable corpus already calls out ("a `Memory`, a two-driver net, a combinational loop").
  Filing it under "holds state" teaches the wrong concept in the diagnostic a student reads.
- **`SubCircuit` is absent entirely.** It is the case that decides whether the capability is
  usable on real coursework, and #306 names it: the extractor inherits the exporter's
  refusals, and `SubCircuit` is a day-one exclusion unless a flattening elaborator is funded
  (#306 Open Question 4, recommended default: fund it). A boolean table cannot answer for
  `SubCircuit` at all — the answer is "recurse."

So the classification needs at least: `COMBINATIONAL`, `STATEFUL`, `HIERARCHICAL` (recurse),
`NON_LOGIC` (Display, Text, Stop, Pause — present in a selection, irrelevant to the cone),
and `UNSUPPORTED_VALUE_DOMAIN` (HiZ drivers, and the multi-driver *net* condition, which is a
property of the partition, not of any element). Five cases, each with a distinguishable
diagnostic — which strictly strengthens AC-3's "the two diagnostics are distinguishable" from
two kinds to five.

## Reframing 3: do not let the cone grow an evaluator

The extractor must never evaluate — and nothing in #872 says so, while #641 downstream will
have to. Two consequences worth writing into this issue:

- **#641 should enumerate by driving the existing headless `BatchSimulator`.** Its own AC-1
  makes exhaustive simulation the oracle. Make the oracle the implementation: the golden then
  pins determinism and column order rather than comparing two independent evaluators, and the
  class of "my table disagrees with the simulator" bugs never exists.
- **ARCHITECTURE.md's recorded decision on #221** states the discrete-event interpreter is
  JLS's *sole* simulation execution strategy, with a binding equivalence criterion for any
  future second one. A private evaluator inside a cone extractor (or inside #641) would be an
  unratified second strategy. Say so here, in the component both consumers sit on.

And name the genuinely shared middle honestly: `HdlExporter` already elaborates drawn logic
into `HdlModel` — ports, jump-fused nets, per-element typed statements, with sequential
constructs (`RegisterStatement`, the clocked-case state machine) distinguished from
combinational ones. That is 90% of a headless design IR, and #306 already observed the formal
extractor inherits its policy. The high-leverage move for the whole formal/analysis arc is to
promote that IR out of `jls.hdl` and make Verilog/VHDL one emitter over it, with AIG/CNF,
truth-table enumeration, the PCB netlist (#366) and coverage instrumentation as siblings.
That is a bigger issue than this one, but #872 is the moment the seam becomes visible, and
this task should be written so it does not foreclose it.

## Re-homing: this belongs to the netlist layer, not to CAP-31

`part_of_feature: 563` with `shared_with: [306]` is a workaround for the very ambiguity the
issue exists to fix, and the body concedes it ("if CAP-09 later files a feature that properly
owns it, this task re-homes"). A component with two unrelated consumers belongs to the layer
they both stand on. **FEAT-004 (#336), "exactly one net partition in JLS," already is that
layer**, already owns `jls.netlist` via #468, and already has further consumers queued —
#366 (PCB netlist), #332 (partition cut), #318 (net identity). Re-home now: `part_of_feature:
336`, consumers `[563, 306]`. Then the `shared_with` hack disappears, the ordering dependency
on #468 becomes intra-feature, and the third consumer that shows up in six months does not
have to negotiate with CAP-31.

Corollary: **AC-6's waiver is contrary to #468 and should be deleted.** "Write a private
traversal so #468 can absorb it" produces the sixth copy #468 exists to prevent, and #468
lands `ArchitectureRulesTest#netPartitionHasExactlyOneImplementation()`, which would then be
born red or born with an exemption. This task is already `ordering_after: [468]`; make that a
hard block, or at minimum require the waived traversal to be born inside `src/jls/netlist/`
so absorption is a merge within one package rather than a cross-package move.

## Smaller things the reframing does not cover

- **AC-4's ordering is not yet definable.** "Stable-id order" over *frontier nets* presumes
  nets have stable ids; they do not — #318 mints them and #373 names them, both unlanded.
  Order by the stable id (#165/#166) of the element owning the driving/reading put, then by
  put name, and say so; otherwise AC-4 is unimplementable as written and will be silently
  reinterpreted.
- **The multi-driver net** is a refusal condition that lives on the partition, not on any
  element, so it cannot come from a registry table at all. AC-2/AC-3 have no slot for it.
- **The "bound lives with the consumer" note is exactly right** and should survive any
  reframing: a 40-input cone is a good cone, and CAP-09's consumer has no 2^N cost.

## Verdict

**endorse-with-reframing.** The gap is real, filing it rather than reporting it is right, and
the pure/headless/value-returning shape (AC-1, AC-3, AC-5) is correct. Change three things:
move the classification onto `ElementType` and make it a five-case enum rather than a
state/not-state boolean (which fixes the `ShiftRegister`, `TriState`, `DelayGate` and
`SubCircuit` errors in AC-2 at the root); re-home the task under FEAT-004 (#336) so the
component belongs to the layer both consumers share; and delete AC-6's waiver so the sixth
net walk never exists. Add one sentence binding the arc: this pass classifies and bounds, the
`Simulator` evaluates (#221), and no consumer of this cone may grow a second evaluator.
