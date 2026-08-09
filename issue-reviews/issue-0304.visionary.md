# Issue #304: CAP-08: a published RV32 core JLS did not write imports, opens as a readable hierarchy, and executes its own firmware against the author's reference
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Strip the 78k characters of tier bookkeeping and one sentence remains: *JLS should
stop being a tool you can only put your own work into.* That is a real and
correctly-identified gap, and it is aligned with the project's stated trajectory
(`docs/grand-architecture.md` §2, "an FPGA-deployment bridge … #59/#61/#62/#63
stage HDL export → Yosys-netlist import"). The importer is genuinely half-built:
`src/jls/hdl/yosys/CellValidator.java` accepts 19 cell types; the switch in
`src/jls/hdl/imp/NetlistImporter.java` realizes `$not/$and/$or/$xor/$mux` plus
constants, and refuses every non-`$` cell with "hierarchy (subcircuit) import is
not built in this increment". `src/jls/hdl/layout/` (8 files) is already wired in
and solving the placement problem. Everything the issue says about the *state of
the tree* is true.

What it gets wrong is the shape of the ask. The issue treats one sentence as one
outcome, and then discovers that outcome costs 113–175 maintainer-weeks — two to
three and a half years of a single-maintainer project, for one issue, before the
nine out-of-set prerequisites it does not cost. That number is not a budgeting
problem to be managed with kill criteria. It is the signal that the outcome was
cut along the wrong seam.

## The seam: "read" and "run" are two products, and only one is JLS's

Split the fourteen required rows by which §1 step they serve:

- **Read** — FEAT-020 (#320) mapper, FEAT-018 (#358) IR instances, FEAT-016
  (#340) type identity, FEAT-017 (#357) shared definitions, FEAT-022 (#342)
  layout residual, FEAT-019 (#321) netlist write, FEAT-002 (#314), FEAT-015
  (#337). Roughly 48–76 mw.
- **Run and prove parity** — FEAT-026 (#322) four-state, 28–36 mw; FEAT-037
  (#327) reset, 13–18; FEAT-034 (#347) retirement parity, 10–16; FEAT-023
  (#359) external oracle, 6–12; FEAT-009 (#335) measurement gate, 5–10;
  FEAT-036 (#364) byte lanes, 3–7. Roughly 65–99 mw — **the majority of the
  capstone.**

The "run" half is where this pulls against the project's own settled position.
`docs/grand-architecture.md:58` records the stance in terms: *"orchestrate
external tools, never reimplement HDL semantics."* `docs/hdl-support-research.md`
finding 3 is blunter: *"Simulation of full HDL semantics in-house is a trap …
every serious educational tool surveyed delegates HDL simulation to mature
external simulators."* AC-3/AC-4/AC-5 are not literally a Verilog simulator —
the netlist arrives gate-mapped — but they are the *acceptance form* of one:
per-retired-instruction agreement with the core author's reference model, an
independent external witness that must actually run in CI, and a wall-clock
budget derived by dividing 600 s by a measured 3.14 × 10⁶ events/s. That is how
you validate an RTL simulator. It is not how you validate a teaching tool.

Worse, AC-4 and KC-08-1 quietly fire a recorded decision. `ARCHITECTURE.md`
("Simulation execution strategy: discrete-event interpreter is the sole
strategy", #221) names its revisit trigger as *"a concrete CPU-scale design on
the `riscv/` trajectory that is unusably slow interactively"* — and the recorded
response is to *file a follow-up issue for a levelized compiled pass*, not to
declare a ten-minute budget and a kill criterion. #304 is that trigger arriving,
and it neither cites the decision nor takes its stated next step. KC-08-5 (stop
if four-state costs 2× against #232's `BitSet` churn) is the same collision
observed from the other side.

## Alternative 1 — make the schematic a *view* over an engine, not the engine

**JLS writes VCD and cannot read it.** `BatchSimulator.writeVcd` exists;
`grep -rn 'parseVcd\|readVcd' src/` returns nothing. The trace window, the
`TraceSample` model, and probes all exist and are already the surface students
look at.

So: import the core's *structure* (the "read" half, which JLS must build anyway),
run its own firmware under `iverilog`/Verilator — the tools `ToolLocator` and
`docs/icestick-bitstream-handoff.md` already say JLS shells out to — and load the
resulting VCD back into the imported schematic, binding VCD signal names to
imported nets by the provenance the importer already knows. The student then
opens a real CPU, scrubs time, probes any net, and single-steps it. That is §1
steps 3, 4 and 5 delivered.

Cost: a VCD reader plus a name binding plus schematic annotation. Call it 8–15 mw
against 65–99. And it collapses three of the issue's hardest problems rather than
solving them:

- **AC-5 becomes tautological.** The "independent witness" is the engine, so
  there is no JLS-vs-JLS degenerate case to guard against, and no
  `Assumptions.assumeTrue` CI hole to police.
- **AC-4 evaporates.** No 1.884 × 10⁹-event budget, because JLS is not
  evaluating the events.
- **AC-3's four-state exposure disappears at the boundary.** X and Z arrive as
  VCD symbols to *display*, not as a value domain the whole element tree must
  carry. FEAT-026 remains worth doing on its own merits (first-driver-wins is a
  defect for hand-drawn tri-state buses too, independent of any import) — but it
  stops being a prerequisite for opening someone else's CPU.

This is also the generalizable move. A schematic that can replay any trace is
simultaneously: the debugger for JLS's own batch runs, the replay surface for the
collaborative-editing trajectory, and the visualization layer for grading. One
mechanism, three of `grand-architecture.md` §2's trajectories — versus one
mechanism (a parity-grade interpreter) serving one.

## Alternative 2 — import the hierarchy, black-box the leaves

The issue's pedagogic promise is "a student who wants to read a real CPU". Push
on that. PicoRV32's author never drew a schematic; Yosys destroys source
structure below the module boundary. What survives synthesis is module names and
port lists — everything under them is a machine-generated gate mesh of tens of
thousands of cells. `LayoutMetrics.java` can score it, but "scored readable" and
"a human reads it" are different claims, and §1 step 3 conflates them.

The honest product is therefore the *module-level* view: a navigable block
diagram carrying the author's names and ports, drilling down until it bottoms out
in a leaf that is a stated black box. That is FEAT-024 (#360) — which this issue
grades **beneficial** and keeps out of the required set. Invert it. Black-box
leaves plus hierarchy is the spine; full gate realization of every leaf is the
optional increment, taken cell family by cell family as demand appears. That
single inversion removes the mapper-parity obligation (AC-1's "zero unresolved
problems over a whole real core") that drives FEAT-020's scope, and it dissolves
AC-7 — an unrealizable construct has somewhere to *go* rather than needing a
named refusal.

## Alternative 3 — ship a corpus, not a compatibility guarantee

Re-read the "Intended Audience" section. Every reader named there wants *circuits
to open*. None of them needs `jls -import` to be a supported command over the
unbounded space of published cores. Run the pipeline offline, over a curated set
of published modules, commit the resulting `.jls` files as an examples library —
the same delivery model as the in-jar help tree, and the same model `riscv/`
already uses. The importer then has to work once, on the maintainer's machine,
not forever against a moving Yosys JSON schema.

This is the cheapest reframing and it makes the issue's most elaborate apparatus
unnecessary: AC-0's `core-pin.properties` (name, revision, Yosys version, script,
provenance-asserting test) and KC-08-4's "re-pick, do not chase" both exist to
manage the rot of an external pin. A checked-in `.jls` does not rot.

## The row that is mis-filed, and is the best thing here

`src/jls/elem/SubCircuit.java:332` — `public Element copy()` deep-copies the
entire subcircuit. N instances of a student's full adder are N unrelated
circuits that diverge the moment one is edited. That is a live defect in the
*hand-drawn* world, affecting every student who has ever built a ripple-carry
adder, and it has nothing whatever to do with third-party cores. FEAT-016 (#340)
and FEAT-017 (#357) fix it, and here they are priced as import scaffolding
(3–5 + 25–36 mw), gated behind a Yosys mapper, inside a 113–175 mw program.

Promote them out. "A subcircuit is a component, not a stamp" is its own outcome
with its own audience and its own demo, it strengthens the element-registry arc
(#78, `ElementRegistry` at 35 types, closed by construction) that
`grand-architecture.md` §3 calls the seed of the module system, and the import
path inherits it for free when it arrives.

## What I am disregarding, and why

**I am disregarding AC-3, AC-4, AC-5 and AC-8, KC-08-1 and KC-08-5, and §1 steps
4–6 as written.** Not because they are badly specified — they are the most
carefully specified criteria in the issue — but because they buy the wrong thing.
They spend the majority of the budget making JLS's discrete-event interpreter
answerable to a hardware designer's standard of proof, when the project's own
research doc and grand architecture both say that standard belongs to the tools
JLS orchestrates. The student outcome those criteria exist to serve — *watch a
real CPU execute, probe inside it* — is delivered by Alternative 1 at roughly a
sixth of the cost, on the right side of the recorded stance.

I endorse §1 steps 0–3 and 7, AC-0, AC-1 (narrowed to the realized set rather
than a whole core), AC-2, AC-6, KC-08-2 and KC-08-3 — the "no silent mis-mapping"
line in particular is exactly right and should outlive every reframing here.

## Two notes on the artifact itself

The evidence base has failed twice: `evidence_commit: 2d0ca9d` is branch-only and
dead, the follow-up pin `07a0bea` is also branch-only and dead, and the entire
`docs/plan/` tree this issue sources every cost band and roster row from **does
not exist on master** (`ls docs/plan` fails at HEAD). Every number in the Cost
section currently cites a document no reader can open. That is not a citation
hygiene complaint; it means the plan corpus and the repository have become two
systems, and the tracker is tracking the wrong one.

Four of the six comments are pure bookkeeping — withdrawn mermaid edges, mirror
obligations, commit-pin corrections — and none moved a line of code. A planning
apparatus that generates more artifact than the engineering it plans is a cost
the project is paying out of the same maintainer-week pool it is budgeting. The
right home for CAP-08 is not an issue: it is a `## Recorded decision` under
`ARCHITECTURE.md`, in the exact form that file already uses ("here is the
direction, here is what reopens it"), with **one** open issue against it — the
3–5 mw demo slice (bit-level mesh synthesis plus `$add` and `$dff` over a single
published module). That slice is the honest next move, it is the cheapest test of
whether the remaining bands are real, and it is the only part of this issue a
maintainer can actually start on Monday.
