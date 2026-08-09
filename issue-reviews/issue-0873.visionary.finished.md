# Issue #873: FEAT: every element JLS can simulate but refuses to export gets an export or a permanent, reasoned refusal — Memory, RegisterFile and FieldExtend leave the reject bucket or state why they never will
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated purpose is bucket hygiene: three classes leave `REJECTED`, or their reason
strings get better. That is not what is at stake. Every element still refused is a
*stateful* element, and stateful elements are the only reason anyone exports a JLS
circuit at all. A drawn CPU is `riscv/build_cpu.py`'s datapath plus an instruction ROM
and a data RAM; strip the memories and what is left is an ALU with nowhere to put a
result. So the real claim underneath #873 is: **JLS's export path can carry a computer,
not just a combinational block.** That claim is squarely on the project's arc — #202's
RV32I oracle, #302's tapeout, #304's imported core all die without it — and it should be
the sentence at the top of the issue. The bucket is bookkeeping that follows.

Read that way, the feature deserves to exist. Two of its five acceptance criteria,
its cost model, and its ordering edge are all artifacts of the bookkeeping framing and
should go.

## The refusal reasons are a category error, not a design question (disregarding AC-4)

`REJECTED`'s Memory reason — inherited into #873's AC-4 for `RegisterFile` — says
multi-port storage "maps to a technology-specific primitive (block RAM, LUTRAM)."
That is a **synthesis-backend** concern borrowed from a vendor P&R flow. It has no
bearing on what JLS ships. README:136-141 states the export contract plainly: "a
deployment bridge, not an HDL tutorial," Verilog-2005 structural text; the validation
gates in `test/jls/hdl/GhdlCompileTest.java` and the iverilog-gated legs are
**simulators**. Behavioral arrays with a clocked write process are the portable
rendering for every simulator and are *inferred* by every synthesizer including Yosys,
which this repository already depends on (`docs/hdl-support-research.md` §6 Stage 2 runs
`memory -nomap`, i.e. it keeps `$mem_v2` precisely because that pattern is recognized).
Primitive selection becomes real only at bitstream time, which is #213/#215 and
`docs/icestick-bitstream-handoff.md`, explicitly *not* this path.

And the semantics are not open questions. `RegisterFile.react` (src/jls/elem/RegisterFile.java:504-560)
is forty lines and entirely determinate: async combinational reads through `readWord`,
`reg0Zero` forcing address 0 to zero on both read and write, out-of-range addresses
dropped, all enabled writes committed on one shared rising edge of `C`. Each of those is
a two-line HDL construct. There is no port count at which anything "bites."

So AC-4 as written — "if the claim survives for multi-port, state the port count" —
asks for a defence of a claim that was never true for the artifact JLS produces. I am
disregarding it. The honest disposition is: `RegisterFile` exports, and #492 ships that
reason string deleted rather than qualified.

## FieldExtend needs no disposition, no new IR, and no workaround verification (disregarding AC-3)

AC-3 spends its budget proving that a sentence in an error message is true. That
obligation is self-inflicted, and it disappears if the exporter simply does the
desugaring itself. `HdlModel` already carries both halves:

- `ReplicateStatement` — one bit replicated to N (the `Extend` element's template,
  HdlExporter.java:539-547).
- `BitMapStatement` — arbitrary source-bit → target-bit routing, already emitting
  run-coalesced `assign y[hi:lo] = x[hi:lo];` (VerilogEmitter.java:630-647) and mirrored
  in `VhdlEmitter`.

Sign extension of a k-bit field to n bits is then: one `BitMapStatement` lifting bit
k−1 into a 1-bit temp, one `ReplicateStatement` widening it to n−k, one `BitMapStatement`
placing the field at `[k-1:0]` and one placing the fill at `[n-1:k]`. Zero-fill swaps
the replicate for the existing `ConstantStatement`. **Zero new statement kinds, zero
emitter changes, zero visitor churn** — and the golden test pins the equivalence that
AC-3 wanted to establish by hand against a fixture corpus. The "Splitter + Binder +
Extend" advice becomes true by construction because the exporter is the thing performing
it.

This generalizes, and it is the architectural seam #873 never considered.

## The reframing: a fourth disposition, `LOWERED`, and a conformance rig

**1. `LOWERED` belongs in the policy model.** #492's four buckets — exported / skipped /
topology / rejected — have no slot for "expressible, but as a rewrite into statements
the IR already has." That slot is where `FieldExtend` lives, and probably where a
`RegisterFile` with a Memory-array statement in hand lives too. This matters more than
it looks: `HdlModel.StatementVisitor` is a closed vocabulary whose stated virtue is that
"a new statement kind fails to compile until every emitter handles it" (HdlModel.java:141-146).
That virtue is a tax, paid by every present *and future* printer — and the roadmap
contemplates a third (SystemC, `docs/hdl-support-research.md` §5) plus #302's TinyTapeout
wrapper. Adding one array statement for `Memory` is worth the tax. Adding statement
kinds for elements that are compositions is paying it for nothing. Make lowering a
first-class disposition and the IR stays small as the element count grows toward the
thirty-sixth type #315 is worried about.

**2. The deliverable should be the oracle, not three dispositions.** AC-2 already
requires "matching JLS batch output on shared stimulus under the external simulators."
That is a *reusable instrument* described as a per-element chore. Build it once: given
any circuit and any `-t` stimulus file, export → compile under iverilog/ghdl → drive →
diff against `BatchSimulator` output. The pieces are all present (`docs/batch-interface.md`
is a normative contract, `riscv/verify.py` is the same pattern against a Python
reference, VCD export exists). Once that rig exists, "does Memory export correctly" is a
measurement rather than an argument — and the same rig immediately serves #292/#358
(SubCircuit), #304 (import round-trip), #302, and the equivalence criterion
ARCHITECTURE.md:359-368 binds any future simulation strategy to. Landing three element
templates buys three elements; landing the rig buys every element that will ever be
added, and turns KC-1's delta-cycle read-during-write worry from a judgement call into a
diff.

**3. Land `FieldExtend` before #492, not after.** #492's Open Question 2 says the
FieldExtend workaround sentence must be verified before it ships. If `FieldExtend`
exports first — cheapest of the three, by #873's own costing, and per above it is a
lowering with no new IR — then that sentence never ships, #492 populates three entries
instead of four, and the verification obligation vanishes from both issues.

## The `blocked_by: [492]` edge is self-inflicted

#873 defends the edge as "a real prerequisite, not a preference," on the grounds that
"this feature's unit of work is removing an entry from `REJECTED`." That sentence is the
tell: the feature has defined itself in terms of a data structure rather than an outcome.
Landing Memory export requires `Memory.class` to move into `EXPORTED` (HdlExporter.java:422-428)
and a golden to appear. It does not require a map that does not yet exist on master. The
totality test is genuinely valuable — it is what stops the *next* `FieldExtend` — but it
guards the same invariant whether it is written before or after. Under the outcome
framing the two issues are independent and land in either order, which is strictly better
for a single-maintainer repo than a serialized chain of four (#492 → #292 → #873 → #457).
Keep #492 as `related`; drop it as a blocker.

## What I would keep verbatim

AC-5 is the best idea in the issue and is larger than the issue: *"No portable rendering
yet" is not a reason; it is a placeholder.* Promote it out of an acceptance criterion and
into ARCHITECTURE.md's "Recorded decisions" section, which already uses exactly the right
shape — rationale plus the trigger that reopens it (see the i18n and simulation-strategy
entries, ARCHITECTURE.md:238-250 and :341-368). A refusal without a revisit trigger is
project debt in the project's own vocabulary. That rule outlives whatever happens to
Memory, RegisterFile and FieldExtend, and it is the thing that would have caught #201's
drift at the time.

KC-2 (do not build a primitive-selection mechanism twice) is also right, though under
this reframing it should never fire: there is no primitive selection to build.

## Summary of the reframing

Same destination, different route: stop describing this as three classes leaving a
bucket and describe it as *stateful circuits become exportable*. Add `LOWERED` as a
disposition so compositions do not grow the IR. Make the export-vs-simulate conformance
rig the feature's primary artifact and the three elements its first subjects. Delete —
do not qualify — the technology-primitive refusal reason, because it is a synthesis
claim about a simulation deliverable. Land FieldExtend first so #492 never has to ship a
sentence it cannot verify. And unblock this from #492.
