# Issue #448: TASK-0047: every cell the validator accepts becomes a drawn element — the importer stops passing a design and then refusing it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its apparatus, the claim is: *the Yosys import path should stop being a
demo*. That is the right thing to want, it is the thing `docs/grand-architecture.md`
calls the "FPGA-deployment bridge" (§55-58), and it is unblocked — every element the
mapper needs is registered (`src/jls/elem/ElementRegistry.java`), and the Builder seam
exists. I endorse the goal without reservation.

What I do not endorse is the *measure*. The issue equates "the importer stops passing
a design and then refusing it" with the set identity `CellValidator.SUPPORTED \
mapCell-cases = ∅`. Those are not the same statement, and the gap between them is
where the whole visionary argument lives.

## 1. The stated invariant is false even when the acceptance criteria are all met

§3 asks whether parity makes it so that "a design the validator passes always
imports." It cannot. `CellValidator` inspects cell *types and parameters*; it never
looks at connectivity. Three refusal paths in `NetlistImporter` are entirely
downstream of it and survive this task by design:

- `resolveReader` (`src/jls/hdl/imp/NetlistImporter.java:735-742`) — the slice/concat
  refusal, which P10 *requires* to keep firing;
- the width-mismatch `Extend` refusals (`:280-284`, `:313-320`, `:343-353`);
- the multi-module refusal (`:156-159`), deferred to #449.

The issue's own O4 is the proof: both red fixtures fail for **two** reasons, and the
second one is the one that outlives the fix. So after this task a design can still
validate and then be refused — the exact defect in the title. The title, §3 and the
Abstract all overclaim relative to §5's predictions, and an executor reading the
title will believe they closed something they did not.

## 2. The seam is cut at the wrong place — cut it at the bit net, not the cell type

Look at what actually gates import volume. `Builder.driver`/`resolveReader` key the
driver map on `key(vector)` — the *exact whole bit vector*. A reader resolves only if
some single element drives that identical vector. Yosys' connection model is bit-level
by construction (`src/jls/hdl/yosys/YosysNetlist.java:18`), and the project's own
roadmap already names this: `docs/capability-roadmap/sweep-03-elements-and-hdl.md`
§C6 calls the Splitter/Binder mesh "arguably the largest single unlock for import
volume … the single highest-leverage importer task remaining", sizes it at 2–3 weeks,
and states it "does not depend on any other change here."

The reframing: **make `Builder.read(int[] vector) → Endpoint` the one resolution
primitive**, backed by a bit-level map from each Yosys bit id to `(element, port, bit
index)`. Resolution groups a reader's vector into runs sharing a source and
materializes a `Splitter` for a sub-range, a `Binder` for a concatenation, a
`Constant` for constant runs. Every mapper arm then calls `read(...)` and touches no
nets at all.

Once that primitive exists, most of this issue's 14 cell types stop being work:
`$reduce_*`/`$logic_*` are `read(A)` into a Splitter feeding an N-input gate — which
this task *already has to build* — `$bmux` is a select decomposition over the same
mesh, `$add`'s carry-out bit is a slice, `$pos` is `read` and forward, and the
width-mismatch arms collapse into the same code path. The issue defers "the
Splitter/Binder mesh" to a successor while simultaneously requiring Splitter emission
for the reductions: the seam is being cut twice, six weeks apart, in two issues.

Concretely I would reorder: **bit-level resolver first, cells second.** That inverts
§8's checklist and dissolves Threat #1 (fixtures tripping the other residual) and
Threat #2 (a fixture corpus so minimal it proves nothing), because the corpus can then
be *real Yosys output* instead of hand-carved whole-vector JSON that Yosys would never
emit.

## 3. Parity should be structural, not policed by a test

H2 proposes `ValidatorRealizerParityTest` as a standing guard over a duplication that
need not exist. The project has already made this exact move twice: `SaveTags` +
`ElementRegistry` replaced the loader's per-element switch with a table plus a
totality test, and `ARCHITECTURE.md` records the result as "the loader has no
per-element switch." `docs/grand-architecture.md:311-312` goes further and says
outright that "the importer uses the registry as its Yosys-cell→element table (#61)."

So: one `CellCatalog` of rows — cell type, parameter contract, realizer (or a
`Rejection` carrying the teachable message). `CellValidator.SUPPORTED` becomes
`CellCatalog.realizable()`; `mapCell` becomes a catalog lookup and invoke. Then:

- `S = R` by construction; H2 is vacuous and the parity test is unnecessary;
- the `default:` arm's drifting prose has nothing to drift from — there is no residual
  arm to derive (§7.11 item 1 and the "derived residual" deliverable both vanish);
- Open Question 2 (exemption set vs. empty) answers itself — an exemption is a row
  with a `Rejection` realizer;
- `SUPPORTED`, `TEACHABLE`, `MEMORY_TYPES`, `MEMORY_FRAGMENT_TYPES` and the
  pipeline-leftovers bucket — five parallel structures in `CellValidator.java:54-125`
  that all describe one taxonomy — become one table with a kind column.

This is strictly less code than the issue proposes and removes the failure mode
instead of detecting it.

## 4. Build real elements; do not hand-write attribute text

`Builder.addGate`/`addMux`/`addConstant` (`:577-637`) hand-write attribute strings
(`" int bits " + bits + …"`) and port geometry per element type. That is `jls.hdl.imp`
re-declaring knowledge owned by `jls.elem`, and it is the *root cause* of O8: the
importer types attribute names as string literals in a package that has no way to
check them. §7.4 proposes five more such verbs, so the duplication triples.

`Circuit.addElement` (`src/jls/Circuit.java:342`), `Element.setXY`
(`src/jls/elem/Element.java:72`) and `Circuit.save(PrintWriter)` (`:1466`) all exist.
Construct real elements through `ElementRegistry`'s factories, set coordinates after
layout, and emit through the real writer. The importer then never writes an attribute
name — the element's own `save` does — and O8's silent-`setValue` hazard is
**eliminated for this path without waiting for #404**, rather than merely guarded by
a round-trip assertion that §11 itself admits cannot catch a wrong *value*. It also
makes the geometry/port sketch element-owned, which is the only version of "the
importer uses the registry as its cell→element table" that actually holds.

## 5. The oracle should be committed, not gated

§11 names the sharpest threat in the issue: "a skipped differential oracle is
indistinguishable from a passing one." Then P8 — the only evidence that any of this
*simulates* correctly — is put behind `Assumptions.assumeTrue(yosys != null)`, exactly
as `ImportPipelineTest.requireYosys()` does today, and the mitigation is to hope #386
lands. That is the threat, restated as the plan.

Alternative, costing near nothing: **commit the generated artifacts.** Generate the
Yosys JSON and the `iverilog` reference trace once, with tool versions recorded in the
fixture, and make the differential a tool-free test comparing JLS batch-sim output to
the committed golden. The repo already does half of this — `and2.json` sits beside
`imp_gates.v` in the same directory. The gated pipeline test then proves only that
regeneration still yields the same JSON (a drift check), while H5 — the hypothesis
whose refutation means a realization is *semantically wrong* — gets checked on every
`mvn verify`.

Stronger still, and available only to this project: JLS owns both emitter and
importer. `sweep-03` §C9 identifies exactly this control — export a hand-drawn
circuit, push it through Yosys, re-import, batch-simulate both, compare traces — as
what round-trippability requires, and notes no other tool in the survey can do it.
That oracle needs no second simulator at all and is a better use of the effort than a
single gated counter test.

## 6. Two smaller design corrections

**Feedback marking (Stage 5) is over-thought and self-referential.** The rule "target
is upstream in the module's topological pre-order" needs a topological order of a
graph that has cycles — which is why feedback marking exists in the first place; §11
concedes the mutual-recursion ambiguity. Simpler and sufficient: **mark every edge
whose source is a state element's output.** A register/memory output is a cut point by
definition, so cutting all of them guarantees the acyclic residual that
`HeuristicLayeredLayouter:169,273` needs, with no ordering computation and no
ambiguity. The cost is that some purely-forward register edges get exempted from the
left-to-right rubric (`LayoutMetrics:263`) — a slightly loose score, versus a
heuristic that can leave a cycle and throw `LayoutException`. If the rubric loss
matters, mark all state outputs, then un-mark those not needed to break an SCC.

**The headline audience mostly still fails.** The Abstract promises the student whose
counter validates then refuses. Every textbook counter is `always @(posedge clk or
negedge rst_n)` → `$adff` → still a teachable reject after this task (P9 requires it).
`sweep-03` §C2 records that the `$adff`/`$sr` families are ten of the fifteen
`buildTeachable` entries and calls that idiom "the single most common sequential idiom
in every textbook and every real design." That is not a reason to reject this work —
it is a reason to promote the `$adff` reject-frequency measurement from a "rides
along" checkbox (Open Question 3) to a **primary deliverable**, because it is the
datum that decides whether the next three weeks go to the bit-level mesh or to a
Register with a reset pin. Measured honestly, this task's user-visible unlock is
`$dff`-plus-`$add`-plus-memory on designs that happen not to slice — a real but
narrower win than the framing implies.

## Recommendation

Keep the goal; change the shape and the order.

1. Bit-level `read(vector)` resolver first, with Splitter/Binder/Constant
   materialization (absorbs #388-adjacent width work and the "surviving residual").
2. `CellCatalog` as the single table; delete the parity test as unnecessary and
   collapse `CellValidator`'s five parallel sets into it.
3. Realize the cells as catalog rows building real `Element` objects, emitted through
   `Circuit.save`.
4. Commit the Yosys JSON and reference traces so the semantic oracle runs tool-free;
   keep the gated run as a drift check.
5. Mark feedback on every state-element output edge; drop the pre-order.
6. Report the `$adff` frequency as a headline number on #61.

I am explicitly disregarding two stated criteria: the `ValidatorRealizerParityTest`
deliverable and the "derive the `default:` residual from `SUPPORTED`" deliverable.
Both are machinery for keeping two hand-maintained lists honest; under §3's reframing
there is only one list, and neither is needed. Everything else in §14 survives.
