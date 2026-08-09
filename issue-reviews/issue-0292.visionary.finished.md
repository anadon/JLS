# Issue #292: HDL export: hierarchical SubCircuit — module per subcircuit type with instantiation, lifting the reject-list entry
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## The capability belongs; the plan of record that landed on it does not

Hierarchy is not a feature of JLS, it is JLS. The README sells a tool where students "draw
circuits from gates, wires, registers, memories, state machines" and the whole point of a
subcircuit is that a thing you built once becomes a thing you use many times. An exporter
that refuses every design built that way (`HdlExporter.java:87-88`, `EXPORTED` at `:422-428`)
exports the subset of JLS that nobody teaches with. So: export hierarchy, yes.

What I am reframing is the decision recorded in the 2026-08-08 resolution comment, which
supersedes this issue's own Abstract and answers Open Question 1 **uniquified — one module per
root-to-instance path**, on the stated ground that *"the deduplicated shape is not implementable
at all until a canonical structural digest exists — that is #340."*

That premise is false on `master`, and the consequence of acting on it is that the project pays
for hierarchy and receives flattening.

## 1. A sufficient type key already ships — it is #166, not #340

`Circuit.save` is canonical by construction. `Circuit.java:1486-1491` states the invariant in
the source: *"two circuits with identical content save byte-identically, whatever their
load/edit history"* — elements sorted by stable id, wires last, file-local ids assigned in that
same order so *"id and ref lines depend only on circuit content"*, with platform-independent
newlines (`:1468-1474`, #111/#166). `SubCircuit.save` inlines the whole nested circuit
(`SubCircuit.java:287`), so the canonical text of a nested block is a total, deterministic
function of that subtree — including its own children, recursively.

Therefore `H(canonical_text(subtree))` is a **conservative type key available today**:

- equal digest ⟹ the two subtrees are the same circuit in every respect the save format
  records ⟹ emitting one module and instantiating it twice is *provably* sound;
- unequal digest ⟹ emit two modules. Wrong only in the harmless direction.

This inverts the plan of record. Deduplication becomes the default and uniquification becomes
the **collision fallback**, which is exactly the shape #59 resolved on 2026-07-17, and it needs
nothing from #340. #340 itself says so in its own § 6: *"Deduplicated hierarchy import
(FEAT-018, FEAT-020) benefits from the same identity but does not require it."*

Three honest caveats, all cheap:

1. `Circuit.save` renumbers elements (`Circuit.java:1499-1503`) and the exporter synthesizes net
   names from `el.getID()` (`HdlExporter.java:346`). Digest before the walk, into a string, or
   over a private projection — never by calling `save` mid-export.
2. A nested block's header line is the *instance* name (`Circuit.java:1479-1481`,
   `subElement.getName()`), not the definition's. Normalize or skip that one line.
3. The bytes carry coordinates and stable ids (`Element.java:24`, `:291-296`), so two hand-drawn
   copies of the same circuit over-split into two modules. Over-splitting is the safe failure and
   the dominant real case — the same file imported twice — digests equal, because both copies are
   loaded from identical bytes carrying identical declared ids.

Total cost: a projection function and a `MessageDigest`, well under a day, inside `jls.hdl`.

## 2. Uniquified hierarchical export is flattening with extra syntax

Module count under the plan of record is Σ over circuits of |instance paths| — for a design
four levels deep with two instances per level, that is the same combinatorial blowup as inlining
the subtree at every use. The output is the same size, carries the same information, and teaches
the same (wrong) lesson; it merely wears `module`/`component` clothing. Meanwhile it costs the
full hierarchy machinery: the twelfth `StatementVisitor` arm, `HdlDesign`, per-module `HdlNames`
scopes, a topological order, VHDL component declarations, and a six-golden corpus.

So the plan of record spends the feature's budget to buy the fallback's semantics. The two
defensible points are the endpoints, not the middle:

- **Cheap:** the recorded interim — inline flattening. Delivers a correct, simulatable artifact
  and the #202 oracle, at a fraction of the work, and honestly labelled.
- **Right:** deduplicated modules per §1. This is the only version that delivers what hierarchy
  is *for* — the artifact a student hands to an FPGA flow or a grader looks like the drawing.

The resolution comment concedes the teaching loss and files it as a note in a Javadoc. For a
tool whose stated purpose is teaching, "we forfeited the one-module-reused-N-times lesson" is
not a footnote; it is the deliverable being withdrawn.

## 3. The stated impact points at the wrong blocker — verified

The issue justifies itself with *"the #202 RV32I CPU is structured this way [with subcircuits],"*
and claims landing this "unblocks the #202 export-side differential oracle." Against the tree:

- `grep -rn SubCircuit riscv/*.py` → **zero hits**. `riscv/build_cpu.py` builds one flat
  `Circuit` (`build_cpu_circuit`, `:464`).
- `riscv/gui/cpu.jls` contains exactly one `CIRCUIT` block and no `SubCircuit` element.
- What the CPU *does* use is `Memory`, three times: `c.memory("imem", ...)` (`:349`),
  `c.memory("ctrl", ...)` (`:366`), `c.memory("dmem", ...)` (`:398`).

The RV32I oracle is blocked by **#291 (Memory)**, not by this issue. That reverses the sequencing
the resolution comment records ("this issue lands while `Memory` is still rejected, and #291 lands
after"): if unblocking the flagship consumer is the goal, #291 lands **first**, alone, and is
worth more per week than everything here. Reject-propagation across the hierarchy can be tested
against any still-unclassified element (that is #492's fall-through, `HdlExporter.java:180-192`)
without holding Memory hostage to it.

## 4. A better seam: export mints the identity that #340 later adopts

The corpus currently reads "#292 waits on #340, or gives up dedup." There is a third arrangement
that strengthens both: **#292 mints a local projection π and #340's TASK-0039 promotes it.**
Export is the ideal proving ground — it is headless, it has goldens, and it produces an immediate,
visible answer to #340's own Open Question 3 ("what exactly is in π?"): whatever HDL dedup needs
to ignore is exactly what identity should ignore. #340 gets an empirically grounded projection
instead of a designed-in-advance one; #292 gets its key today.

The same key serves the import side (#61 / `NetlistImporter`): a Yosys netlist with one module
instanced N times should import as N `SubCircuit`s whose definitions share a digest. One digest
utility, both directions, conventions that cannot diverge — which is precisely the convergence
this issue's §12 asks for and has no mechanism to enforce.

## 5. A stronger oracle than six goldens: round-trip through the netlist model

The absorbed plan's items 17-19 hand-write two structure assertions (`assertEveryInstantiatedModuleIsDeclared`,
`assertEveryInstancePortExists`) because `ghdl -a` analyzes but does not elaborate. That is a
correct observation answered with the weaker instrument. The repo already ships
`src/jls/hdl/yosys/YosysNetlist.java`, `CellValidator`, and `jls/hdl/imp/NetlistImporter`.
Export → `yosys read_verilog; write_json` → `NetlistImporter` → structural compare against the
original circuit tree turns both assertions into a *property* over every design, catches the
width-mismatched port map neither analyzer catches, and is the same machinery #202's differential
oracle needs anyway. Goldens pin text; the round trip pins meaning. I would fund the round trip
before the sixth golden.

## 6. Two real semantic risks, and the cut that actually works

- **`Clock` inside a subcircuit** (absorbed item 11) is the one item that can produce a module
  that analyzes and does not run. It deserves to be a prediction, not item eleven of twenty-three.
- **Tri-state across a boundary.** `HdlModel.Direction` has exactly `INPUT` and `OUTPUT`
  (`HdlModel.java:28-33`) — no `INOUT`. JLS boundary pins are `InputPin`/`OutputPin`, so a
  bidirectional bus cannot cross a boundary in the drawing either, which probably saves this; but
  two instances' `TriState` outputs driving one parent net become two module outputs on one wire,
  and that must be pinned by a simulating test, not an analyzing one.
- **The cut.** The resolution comment argues the sealed visitor forces one branch. True for
  IR-vs-emitters; false for the whole change. `HdlDesign` as a list-of-one plus per-module
  `HdlNames` scoping changes **no output** and can land first, alone, with "no golden moves under
  `-Djls.hdl.regenerate=true`" as its entire acceptance criterion — which is also the check the
  absorbed item 19 is most afraid of failing late.

## What I would change on this issue

1. Re-answer Open Question 1: **deduplicated by canonical-save digest, uniquified on digest
   inequality.** Record #340 as beneficial, not a prerequisite — with §1's evidence, not the
   cost argument.
2. Strike the #202 justification or correct it: this issue does not unblock #202; **#291 does.**
   Re-sequence #291 ahead.
3. Split off the no-output-change refactor (`HdlDesign` + per-module name scopes) as its own
   landing.
4. Add the netlist round-trip as the structural oracle; keep goldens for text.
5. If, after §1, the maintainer still prefers uniquified, then say the honest thing and ship the
   recorded interim (inline flattening) instead — it delivers the same information for a fraction
   of the cost, and leaves the budget for the deduplicated version that is actually worth having.
