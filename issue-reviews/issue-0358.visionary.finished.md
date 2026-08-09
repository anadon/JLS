# Issue #358: FEAT-018: a decomposed design exports — the HDL IR carries more than one module and an instantiation statement, so nesting becomes hierarchical Verilog and VHDL instead of a refusal
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## The goal is right, and the project has already said why better than this issue does

`docs/capability-roadmap/README.md:356-361` states the case in one sentence: "JLS
teaches hierarchical design as a first-class idea and then silently refuses to carry
that hierarchy across the tool's own most important boundary. A student who
structures a design *well* is punished by the exporter; one who draws a 1000-element
flat mess is rewarded. **That is a teaching inversion sitting in the tree right
now, pinned by two passing tests.**" Those two tests are
`HdlPolicyTest.subCircuitIsRejectedCleanly:77` and
`rejectionListsEveryOffenderInOneMessage:88`. #358 is the ticket for that inversion
and I endorse it without reservation as an outcome.

What I do not endorse is the shape it has settled into. Three of its own commitments
— the no-FORMAT-bump invariant, the uniquified projection, and the decision to grow
`HdlModel` rather than cut a new seam — combine to ship the half of the capability
that the project's own roadmap calls the lesser half, and to freeze a large golden
corpus around it.

## Reframe 1 — §4 invariant 2 is the constraint that forbids the win

§1 calls "hierarchy is not new primary data" the "single most load-bearing fact for
anyone estimating it," and §4.2 hardens it into "no `.jls` format change and no
`FORMAT` bump." Verified on `master` at `3b6d6ec`, the fact is half true:

- `Circuit.save:1479-1480` writes a nested block as `CIRCUIT <subElement.getName()>`
  — the **instance** name. The imported circuit's type name is never persisted at all.
- `Circuit.loadElementItems:1015-1024` constructs `new Circuit("")` per nested block
  and then sets the instance name *from* it.
- `Circuit.java:50` holds a one-to-one `subElement` back-pointer, and
  `SimpleEditor.doModify:5159-5175` opens an editor tab on that instance's own
  `Circuit` — so two placements of one drawing can diverge by a single edit and
  nothing anywhere records that they started as the same thing.

So hierarchical *structure* is indeed already in the file. Reuse *identity* is not,
and never was. The ROSTER RESOLVED comment gets the fact right and the conclusion
half-right: deduplication is not waiting on #340's structural digest, it is waiting
on the save format having a component table. `docs/capability-roadmap/README.md:325-334`
names precisely this and prices "interface bundles 3-4 plus reuse identity 3-6"
*including* the FORMAT bump and the one-way migration of inline subcircuits.

A structural digest is also the wrong key even if #340 ships it. It fuses two
conceptually distinct circuits that happen to be isomorphic, and it refuses to fuse
two instances of one component that diverged by a stray edit — exactly the two cases
a student hits. Declared identity is correct; a digest is a heuristic that
deduplication should never be founded on.

**The concrete consequence the issue understates.** Uniquified export emits one
module per *instance path* (§3's `Σ_c |Π(c)|`). That is flattening with module
syntax. For an HDL reader it is still a genuine win — readability, instance paths in
a VCD, a synthesis boundary that is visible. For the five downstream consumers §1
recruits it is close to worthless: a KiCad netlist carrying four distinct definitions
of one 1-bit adder, an IP-XACT component per placement, a shuttle wrapper over a tree
of single-use modules. The Abstract's claim that "every downstream format that is
structurally an instance-of-module model becomes reachable for the first time" does
not survive uniquification, and the issue should stop making it.

**Alternative A, the one I would take.** File the reuse-identity slice first, as a
small additive format increment rather than as #340's digest: `SubCircuit` gains a
persisted component name and version; `SubCircuit.save:287` writes the nested block
once per distinct component and a reference thereafter (or keeps inlining and adds a
`component "name"` attribute, first inline block wins, divergence reported at load).
That is FORMAT 3, additive, and an old reader that ignores the attribute still loads
the inline copies — the same compatibility argument P7 makes at `AMENDMENT.md:249-256`
("strictly better-behaved than the two cases `docs/file-format.md:458-478` already
flags as known hazards"). Then hierarchy export is deduplicated on day one, the
golden corpus is generated once instead of twice, Open Question 1 evaporates, #340
and #357 are never needed for this, and the lesson the issue records as a forfeited
caveat ships with the feature. Honest cost: it puts a 3-6 mw slice ahead of a 4-6 mw
feature, and the maintainer may decline that. But if it is declined, the honest
statement of what #358 delivers is "flattening with braces, to unblock #359's
oracle" — not the downstream-unlock claim in the Abstract.

**Alternative B, cheap, and compatible with either answer to A.** If the format work
is declined, do not fabricate identity — *report* its absence. Compute the structural
hash anyway during the walk and emit one warning per fused-looking group: "modules
`top_add0..top_add3` are structurally identical; JLS cannot record that they are the
same component." That costs about a day, turns a silent pedagogical regression into a
visible and teachable limitation, and hands #340 a ready-made corpus. Note that the
ROSTER comment's plan to record the choice "in the exporter's element-policy Javadoc
where a course author reads it" targets the wrong surface; students and instructors
read the warning stream, which `HdlExporter.Result.warnings` already carries to the
CLI.

## Reframe 2 — hierarchy does not belong inside `jls.hdl`

The boundary comment's table puts `HdlDesign`, `InstanceStatement` and
`HdlEmitter.emit(HdlDesign)` in `jls.hdl`. Four independent parts of the project want
that same object at a lower altitude:

- **P3 interchange** (`README.md:311-350`) lists EDIF, BLIF, SPICE `.subckt`,
  IP-XACT, structural SystemC, KiCad `.net`. Every one is instance-and-net. `HdlModel`'s
  eleven visitor arms are `GateStatement`, `PriorityCaseStatement`,
  `StateMachineStatement`, `ShiftStatement` — a structural printer wants none of them.
- **P7** (`AMENDMENT.md:243-248`) asks for `jls.core.elab`, "a real named elaboration
  phase running *before* the existing `Circuit.finishLoad`."
- **P8** (`AMENDMENT.md:280-288`) observes "there is no elaboration step, so there is
  nowhere to put a compiled netlist," and wants an elaborator with a bidirectional
  nodeId ↔ drawn-element map.
- **The importer**, in the opposite direction: `NetlistImporter.java:156-159` refuses
  any netlist with more than one module and tells the user to run Yosys `flatten`.
  That is the *same missing type*, and #358 §1 files it away as "a coordination
  obligation, not shared scope."

The walk TASK-0043 describes — recurse the `SubCircuit` tree, name instances, bind
ports to parent nets, detect cycles, carry an instance path — *is* the elaboration
walk all four need, and it is the expensive part. Landing it as a private detail of
the printer stack means KiCad, EDIF, the board flow and the compiled engine each
re-derive it.

**Proposal:** put the walk in an HDL-free package (`jls.design`, or P7's
`jls.core.elab`) producing `Design{Module, Instance, Net, PortBinding}` keyed by
stable element id. `HdlExporter.buildModel` becomes `Design → HdlModel` per module;
`HdlEmitter.emit(Design)` renders. Same walk, same two printers, same goldens, same
sealed-visitor discipline — one different package and one extra record. The delta
today is near zero, and it is the difference between one elaborator and four.

There is already evidence `HdlModel` is carrying more than the printers.
`jls.hdl.board.PcfEmitter:58,73` consumes `model.ports()` — meaning *the top
module's* ports — to write the iCEstick `.pcf`, fed from `JLSStart.java:424`'s
`buildModel` call. §3 asserts "the exporter's public entry points keep their
signatures" and never names this consumer, though §1 recruits the board on-ramp
(#264) as a beneficiary. Under a `Design` seam, "the top module's ports" becomes a
named thing (`design.top().ports()`) rather than an implicit meaning that quietly
shifts the day the model goes multi-module.

## Reframe 3 — pull the import side in, rather than promising to coordinate with it

Open Question 3 asks whether one naming scheme binds the import side and answers "one
legalizer, shared." Under the `Design` seam the question dissolves into a type: there
is one `Design`, `NetlistImporter` builds one, and its "flatten the design" refusal
goes away as a side effect. That in turn makes the roadmap's most distinctive claim
testable — `README.md:339-348`'s round-trip CI property, "`export → yosys → import →
save` equal to the original modulo element ids," of which the roadmap says "no tool
in the survey makes that claim." That property is unreachable for as long as the two
directions carry hierarchy in two different shapes, which is what #358 as written
guarantees for the duration of FEAT-020.

## What I am explicitly disregarding, and what I would keep

**Disregarding §5 criterion 2** as a headline risk. It is called "the criterion the
identifier legalizer is most likely to fail," and it is — but only because
uniquification makes `name : Π → Σ*` injective over *paths*, so identifier length
grows with depth (§3 says so outright). Under a component table, module names are
component names and do not grow with depth at all. The highest-variance item in the
plan is an artifact of the chosen projection, not of the problem. Likewise §7's fear
that answering Open Question 1 the deduplicated way makes #340 and #357 required
edges: under a component table neither is required, so the "single largest edge
consequence" never fires — for a better reason than the ROSTER comment gives.

**Keeping, unchanged:** §4 invariant 3 (the sealed statement visitor stays total —
this is the project's best structural habit and the real reason the two tasks land in
one branch), and §5 criterion 4 (reject propagation carrying the instance path; the
observation that catch-and-rethrow-per-level destroys it is correct and is where the
bugs will be). Both survive every reframing above intact.

**One hazard currently unowned.** The third comment flags that a `Clock` inside a
subcircuit gains a synthesized `clk` port the parent must drive, and that leaving it
unbound produces a module that "analyzes and does not run" — invisible to a green
external-compiler leg. It then attributes this to "this issue's Open Question 3,"
which is the naming question, so the hazard is in fact owned by nobody. It is a port
*role* problem, and P3's port-metadata slice already names its root cause:
`README.md:335-338`, "`HdlExporter` treats `Clock` and `InputPin` identically; JLS
cannot say a wire *is* a clock." Either give it an open question here or route it to
that slice — do not let it ride on a misattribution.

## Verdict

**endorse-with-reframing.** Ship the capability; it closes a teaching inversion the
project has already written down in stronger words than this issue uses. But drop the
no-FORMAT-bump invariant as a *goal* rather than defending it as a virtue, land
reuse identity as a small declared-component format increment before the golden
corpus is generated, and put the hierarchy walk in an HDL-free design/elaboration
package so the importer, the structural printers, the board flow and the eventual
compiled engine inherit it instead of re-deriving it. If the format increment is
declined, say plainly in the Abstract that what ships is flattening with module
syntax, and add the structural-identity warning so the forfeited lesson is at least
visible to the student who loses it.
