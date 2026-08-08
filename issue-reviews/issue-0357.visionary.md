# Issue #357: FEAT-017: one subcircuit definition, N instances that reference it with bound parameters — editing the definition changes every instance instead of one copy
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two different wants are fused in the body. One is **authoring**: a module should be
reusable at more than the width it was drawn at, and a fix to a block should be a
fix to every use of it. The other is **representation**: the object graph and the
saved file should store the block once. The issue treats the second as the route to
the first. The project's own leapfrog study says the opposite, at length, and I
agree with the study.

`docs/capability-roadmap/lf-01-parameterization.md` and the P7 section of
`docs/capability-roadmap/AMENDMENT.md:218-250` are this issue's subject written out
in full. The study's headline finding is not that instances are copied — it says
flatly that *"JLS's hierarchy is elaborated-by-copy already… elaboration is not a
new concept to introduce, it is an existing implicit phase that has never had
inputs."* The gap it names is that **there is nothing an instantiation site can
say to a definition**, and its evidence is the flagship design: `riscv/build/addi.jls`
contains 228 logic elements and **zero** `SubCircuit`, because the register file is
a `for` loop in `riscv/build_cpu.py` emitting a second, independently maintained
implementation of the normative save format. Hierarchy is unused in JLS's own
largest circuit not because copies are expensive but because a copy of a fixed-width
block buys nothing.

## The load-bearing observation: this feature's parameters do nothing

§3's elaborator is `subst(D[d(i)], ρ_i)` — substitute a resolved environment into a
definition. The issue never says **what a bound parameter substitutes into**. There
is no expression language, no expression-backed attribute, and no statement that
binding `N=32` changes a `Register`'s `bits`. lf-01's D4 is exactly that missing
piece and prices it at 3–5 weeks across the 14 element classes that carry a saved
`"bits"` int, with `Memory`'s handwritten save and `Group`'s `pair`-encoded ranges
called out as the hard cases. None of that is in this feature's scope, in either
child, or in the residual.

Check it against §5. Five integration criteria: file size, definition-edit
propagation, golden byte-identity, pre-split migration policy, diagnostic totality.
**Not one of them asserts that a parameter changes a circuit.** At close, a user can
declare a parameter, bind it, and get a well-coded diagnostic when the binding is
wrong — and the circuit is identical either way. That is a parameter plumbing
feature whose parameters are inert. It is the strongest signal that the cut is in
the wrong place.

## Where the 25–36 mw band came from (answering Open Question 4)

The band is not mysterious. `lf-01`'s size section prices the **whole P7 program** —
expression language, `Parameter` element, expression-backed attributes, `jls.core.elab`,
`SubCircuit` bindings and dialog, the `Array` element, FORMAT 3, collab ops, HDL
export — at **25–36 maintainer-weeks**, and the AMENDMENT's table (`:145`, `:971`)
carries the same number. This issue inherited P7's band while scoping roughly
`jls.core.elab` (4–6 wk) plus a consumer migration. The "largest band-versus-sum gap
in the plan" is a scope inheritance, not a hidden residual. Read the other way, the
band is a promise this feature cannot keep: a scheduler paying 25–36 weeks against
it will not get the thing lf-01 says 25–36 weeks buys.

## The seam is cut in the wrong place

TASK-0041 cuts at the field: *"`SubCircuit` stops owning a `Circuit`."* Everything
painful follows from that one clause — the 25 `getSubCircuit()` call sites (I
re-measured: 25 across 8 files at HEAD, matching the issue), the golden-drift risk,
the format break, and all three `blocked_by` edges.

lf-01's D6 cuts at a **phase boundary** instead: `elaborate(design, bindings)` returns
a *fresh, ordinary* `Circuit` tree and runs **before** the existing `finishLoad`,
which is unchanged. Its acceptance criterion is one sentence — *nothing in `jls.sim`
changes.* Under that cut, `SubCircuit` keeps owning a `Circuit`, `getSubCircuit()`
keeps returning one, and the residual that this issue calls "larger than both tasks
combined" **does not exist**. Sharing lives in the authored plane and in the editor's
edit path; the runtime graph stays elaborated-by-copy, which is what every one of
those 25 consumers, the simulator, VCD export, collab ops and the goldens already
assume.

That in-memory dedup is also against the grain of a recorded decision. ARCHITECTURE.md
§"Simulation execution strategy" refuses a second evaluation strategy because
"classroom-scale gate circuits are the present workload" and a second strategy is
"premature optimization until CPU-scale designs are actually common." Sharing one
`Circuit` object across eight instances is the same trade in the same workload, and
the flagship design has zero subcircuits to share.

## The format break trades the best property in the plan for bytes

§1 promises `|saved| = b + Nβ + O(N)` replacing `Nb`, and §4.6 requires the change to
ride #319's must-understand mechanism — i.e. **older readers must refuse the file**.
lf-01 names this exact move as failure mode 3 of the program: *"Someone 'fixes' the
file size by dropping the elaborated plane. That trades the entire compatibility
story — zero migration, every old file works, old readers degrade gracefully — for
bytes in an XZ container."* Its rule is one line: *a `.jls` always contains the fully
elaborated circuit; the parameterization is additive metadata beside it.*

Two supporting claims in §"Intended Audience" do not survive contact with the tree:

- **Diff size.** lf-06 measured that inserting one gate into `riscv/build/addi.jls`
  produces a 5,314-line diff of which 5,227 lines are pure id renumbering churn. A
  `.jls` diff is dominated by id churn, not by duplicated subcircuit bodies; and the
  container is XZ, in which N identical blocks compress hard.
- **Downstream consumers "actually want" one module instantiated 8 times.**
  `HdlExporter` refuses `SubCircuit` outright today (`src/jls/hdl/HdlExporter.java:88`);
  the consumer that would benefit is #292, unbuilt. And it does not need a
  representation change to get what it wants: `Circuit.stateHash()` already exists and
  is content-determined (`src/jls/Circuit.java:1548`, pinned by
  `DeterministicSaveTest.stateHashIsContentDetermined`). Equal `stateHash` **is** "these
  copies are the same block," computable today, with no format change and no #340.

## The three blockers are consequences of the route, not of the goal

- **#340 (definition identity).** Intra-file, a definition already has an identity: its
  circuit name, which the editor actively manages (`Circuit.removeName`,
  `SimpleEditor.updateNamesUsed`, `SaveAsNameCheckTest`). A digest is needed for
  cross-file libraries — which this issue's own scope boundary excludes.
- **#318 (instance-path addressing).** A dotted instance path already ships as a
  documented stability contract: `docs/batch-interface.md` §3.2–§3.3's `QUAL` qualifier,
  byte-pinned by `BatchSimulationGoldenTest.watchedElementsPrintInNameOrder`. A
  diagnostic can name `alu2.adder.carry` today.
- **#319 (must-understand versioning).** Needed *only because* §1 chose a
  representation the old reader cannot degrade through. Under the additive rule it
  reduces to lf-01's FORMAT-3 bump for two new tags — honest, but not a blocking
  program.

A three-edge inward cone reduces to zero hard edges under the alternative framing.
That is the shape of a route problem, not a dependency problem.

## Two elaborators is the architectural hazard nobody has flagged

AMENDMENT's amended spine (`:779`) introduces **THE ELABORATOR** as a new shared node —
flatten hierarchy, union nets across jumps, dense ids, node↔element map — with
**seven** consumers (P3 hierarchy IR, P4 timing DAG, P5 formal flattener, P8 netlist,
P9 site index, P10 ATPG cut, P13 clock slice), noting the code already exists in the
wrong place as `HdlExporter`'s `UnionFind` net walk (`src/jls/hdl/HdlExporter.java:210`),
and warning that *"two implementations of 'which drawn wires are one signal' is how the
engines come to disagree."* TASK-0042 mints a second thing called "the elaboration
pass," in a different package, resolving bindings only, with no stated relation to that
node. Either they are the same phase — in which case this issue should name the spine
node and inherit its consumers — or they are deliberately disjoint and the issue must
say so, because the next reader will assume otherwise.

## Concrete alternative A (my recommendation): the elaborated plane is a cache

1. `SubCircuit` gains `String defref "alu"` and repeated `String bind "N=32"` items —
   two existing item kinds, no grammar change (lf-01 D2).
2. The nested `CIRCUIT` block stays exactly as written today, redesignated **derived
   content**: the elaborated view of `defref` under those bindings.
3. `Circuit` gains a definitions section holding each definition once, as the authored
   source of those derived blocks.
4. Editing a definition re-elaborates and rewrites every instance's derived block; the
   observable capability sentence ("edit once, see it everywhere") is delivered by the
   editor's edit path, not by object identity.
5. CI property that makes the redundancy safe (lf-01): `load → re-elaborate from the
   saved bindings → save` is byte-identical to `load → save`, asserted via `stateHash`.

Consequences: `getSubCircuit()` and all 25 call sites are untouched; simulation goldens
are byte-identical *by construction* rather than by vigilance; every existing file loads
with no migration and older readers still simulate correctly; #340/#318/#319 leave the
critical path; and the freed budget goes to D4 — the expression-backed `bits` attributes
that make a parameter mean something. That is lf-01's stated **useful floor, 8–11 weeks,
"one adder drawing, any width," parity with Digital's headline claim** — reachable
without a single format break.

## Alternative B: a definition is a file

Digital and Issie resolve subcircuits as separate files through a library. That is
where #340/D15 ("the guest image is a FILE; files require import-to-subcircuit") is
already heading. If definitions live in their own `.jls`, dedup is a side effect of
external reference, the library format and the definition table stop being two designs,
and "edit the definition, every instance changes" is the familiar `#include` semantics
students already understand. Cost: link breakage and handout bundling. Worth pricing
before building an intra-file definition table that a file-level one would subsume.

## Alternative C: make it a save mode, not a representation

If deduplicated storage is wanted on its own merits, it is a **writer** feature.
JLS already ships a save-mode axis (`-savetext`, plain vs XZ, documented in the README's
"Circuit files"). "Definition-once" as an explicit opt-in save mode costs no consumer
migration, breaks no default file, and lets the compatibility argument be made by the
user who chose it.

## What to keep, and what I am explicitly disregarding

Keep, unchanged and excellent: the no-silent-default rule, stable machine-readable
diagnostic codes with the instance path, the purity requirement on elaboration, the
diagnostic-totality sweep in §5.5, and the refusal to let a pre-split file auto-merge
deliberately divergent copies (Open Question 1, option (a) — and `stateHash` gives the
report cheaply).

Disregarded, deliberately: §1's first bullet (definition serialized once, sublinear
saved size), §5 criterion 1 (the file-size assertion), §4.6's must-understand
requirement, and TASK-0041's clause *"`SubCircuit` stops owning a `Circuit`."* Those
four are the storage goal, and with them go the three `blocked_by` edges and the entire
25-call-site residual the cost note calls the bulk of the band. I am disregarding them
because the project's own written analysis identifies the authoring gap as the thing
worth 25–36 weeks, identifies the storage optimization as a named failure mode of
pursuing it, and shows the cheaper seam that reaches the same capability sentence.

**Verdict: redirect.** Retarget this feature at parameters that resize what they are
bound into, on lf-01's additive-plane route; keep the elaborator and its diagnostics;
drop the representation change, the format break, and the consumer migration.
