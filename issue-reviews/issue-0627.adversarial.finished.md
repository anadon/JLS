# Issue #627: TASK-C523-1: the emitted netlist is re-parsed and proven to be the circuit JLS simulated — a stable-id net-partition isomorphism over the whole fixture corpus
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being attacked

TASK-C523-1 is the task-level operationalization of FEAT-C05-1 (#523), filed
by the same author about 13 hours earlier the same day. It specifies a
`NetPartitionIsomorphismTest` that re-parses an emitted KiCad `.net` and
checks it against the shared net partition JLS simulated from. The idea is
sound and the gap it names is real (a byte golden cannot tell a structurally
wrong netlist from a right one once the golden itself is wrong). The problems
below are about the issue's own text: it silently contradicts its stated
parent, silently drops precision its parent's version of the same criteria
had, and rests on a prerequisite tree of which zero rungs exist in this
checkout — verified by grep, not asserted.

## Findings, most severe first

**1. The issue's own opening justification is false against its own declared parent, filed hours earlier.**
`#627` opens: *"It exists because no filed row owns it today... This is
#298's AC-1 check 5 given an executable owner."* But `#627`'s own YAML names
`part_of_feature: 523`, and #523 (created `2026-08-04T02:56:54Z`, #627 created
`2026-08-04T15:10:45Z` — the same day, ~13 hours earlier) states in its own
Outcome section that it is precisely the row that owns this check: *"this
issue is the feature row that owns both"* (referring to the isomorphism
check and the parity-narrowing discipline). So at the moment #627 was filed,
a filed row (#523) already claimed ownership of exactly the check #627
claims has none. #627 never names #523 anywhere in its prose — only in the
YAML `part_of_feature` field an executor may not read as a citation. An
executor who reads #627's body alone has no path to discover the parent
issue that already defines this check's acceptance criteria, its kill
criteria, or its relationship to #307/#298.
**Recommendation:** rewrite the opening to "no *task-level* row owns it,"
and cite #523 by number in the Outcome paragraph, not only in the machine
block.

**2. AC2, as written, doesn't supply the input AC1 requires — and #627 silently dropped the clause that #523 used to cover it.**
AC1 requires the comparison to be over `(refdes, pin)`. AC2 says: *"The
source side is read from the one shared partition (`jls.netlist`, #336)."*
But `jls.netlist` (per #336's own §3) is a partition over `WireEnd`/`Put`
objects — it has no concept of a refdes or a package pin; that mapping is
produced by the (entirely unbuilt) packing layer, `jls.pkg.PackPlan`
(#394/TASK-0086, a child of #365). #523's own version of this criterion says
so explicitly: *"compares it to the source partition **pushed through the
packing binding**, computed by union-find."* #627's AC2 drops "pushed
through the packing binding" entirely. As literally written, AC2 gives no
account of how the `(refdes, pin)`-space source object gets built at all,
which leaves the actual join key (element → refdes/pin, sourced from
`PackPlan`) outside anything the criteria audit. A gaming implementation
could build that mapping any way it likes — including re-deriving
connectivity from the packing layer in a way that duplicates work the
partition already did — without technically violating "the source side is
read from the one shared partition," because the packed-binding step is
simply never named.
**Recommendation:** restore #523's "pushed through the packing binding"
clause (or an equivalent), and name `PackPlan`/the refdes-binding artifact
as an explicit second input alongside `jls.netlist`.

**3. AC3 drops a load-bearing conditional clause present in the parent it copies from.**
#627's AC3 states, almost verbatim from #523: *"the synthetic inter-slice
nets FEAT-041 (#365) creates appear in both partitions... the test fails and
KC-05-2 fires."* #523's own AC3 has one clause #627 omits: *"which holds
**only while the cascade rule lives in the IR**."* That clause is not
decorative — it scopes the whole criterion to a specific architectural
condition (the cascade rule staying in the netlist IR rather than moving
into an emitter, which is exactly the failure mode KC-05-2 exists to catch).
Dropping it turns a conditionally-true statement into what reads as an
unconditional one.
**Recommendation:** restore the "only while... in the IR" qualifier so AC3
matches what it's silently copied from.

**4. #523's AC4 — the parity-narrowing discipline — has no counterpart anywhere in #627, and #627 never says why.**
#523's AC4 requires that where the isomorphism cannot be proven for a
fixture class, "the claim is narrowed in writing before release" (absorbed
from #307's AC-5). #627 has five acceptance criteria, none of which mention
narrowing, documented exceptions, or a release-header statement of scope.
If #627 is meant to be the executable discharge of #523 (which its
`part_of_feature` link and near-verbatim AC text strongly imply), closing
#627 with all five of its own criteria green would look — to anyone not
independently re-reading #523 — like the isomorphism work is done, while a
real, currently-undischarged acceptance criterion of the parent feature
(parity narrowing for the unprovable case) has no owner in either issue's
task list.
**Recommendation:** either add #523's AC4 to #627's own criteria, or state
explicitly in #627 that AC4 is deliberately out of this task's scope and
name where it will be discharged.

**5. The named prerequisite ring is a small fraction of the real transitive dependency tree, and every rung — named or not — is currently open.**
`ordering_after: [336, 365, 366, 460, 468]` names five issues, all open. But
walking one level further: #460 (TASK-0089, the emitter this test re-parses)
is itself `blocked_by: [400, 394, 427]` (all open); #365 (FEAT-041, the
packing/refdes/cascade feature) has five child tasks, all "Not filed";
#366 (FEAT-042) has three more "not yet filed" tasks besides #460. None of
`jls.netlist`, `jls.pkg.PackPlan`, `jls.pkg.PhysicalNetlist`,
`jls.pkg.PartLibrary`, or `jls.pcb.KicadNetlistEmitter` exist in this
checkout — confirmed by `grep -r "jls\.pkg\|PhysicalNetlist\|PackPlan\|PartLibrary" src/`
returning no matches. The test #627 specifies cannot be written, let alone
made green, until roughly a dozen further open issues land, most unfiled.
The C-series convention of `ordering_after` (versus the FEAT/TASK series'
`blocked_by`) elsewhere in this programme is explicitly used for
scheduling-convention orderings that "a scheduler may break knowingly" (see
#336 §6) as opposed to technical necessities. Every one of these five is a
hard technical necessity — the test cannot compile without types none of
them yet define — so labeling them `ordering_after` undersells how blocking
they are relative to the vocabulary the rest of the programme uses.
**Recommendation:** either adopt `blocked_by` for this issue given the
dependencies are hard, technical, and total, or state explicitly that
`ordering_after` here is being used in the stronger sense, since the two
usages elsewhere in this tracker are not interchangeable.

**6. Foundation risk inherited without acknowledgment: the artifact this test re-parses has its own load-bearing premise unverifiable in this repository.**
The prior adversarial review of #460 (TASK-0089, the KiCad netlist emitter
that produces the `.net` file #627's test consumes) found that its single
most load-bearing claim — KiCad refuses any footprint-less component,
turning an incomplete export into a silently empty board — is sourced to
documents (`09-format-adoption-plan.md`, `fmt-kicad-geda.md`) that do not
exist anywhere in this checkout, and that KiCad/PCB export appears nowhere
in the project's own roadmap survey (`docs/standards-landscape.md`). #627
builds a substantial correctness gate on top of that artifact without
mentioning either concern. Not a defect unique to #627, but a real
foundation risk that compounds: if #460's premise is wrong or its scope is
never funded, #627's entire test has nothing to test.

**7. "Matched by stable id, not by name string" is the check's central claim, repeated three times, and is never operationally defined.**
Per #460/#366's own data contract, the emitted KiCad `.net` records only
`ref`/`value`/`footprint` for components and `code`/`name`/`node(ref,pin)`
for nets — plain strings, with no stable-id metadata field anywhere in the
format. #627 never states the mechanism by which a re-parsed `(ref, pin)`
pair is resolved back to an `Element.getStableId()` value (presumably via
`PackPlan`, per Finding 2, but that is not written down). Without that
mechanism named, "matched by stable id" is asserted, not specified.

**8. "KC-05-2 fires" (AC3) is used as a defined term but is defined nowhere in this issue.**
It is presumably a kill-criterion belonging to #298 (per #523's own
cross-reference: "#298 KC-05-2... are the kill criteria criteria 3 and 4
answer to"), but #627 gives no pointer. An executor reading #627 in
isolation cannot tell what "firing" it means procedurally — a failed test?
A required issue comment? A block on #298's closure?

**9. Minor: #627 is markedly thinner than its sibling `tier:task` issues in the same programme.**
Compare to #460 and #468 (also `tier: task`, same programme): both carry
Background & Prior Work, an Observations section with re-derived file:line
citations, a falsifiable Hypothesis/Prediction/Falsification structure,
Materials & Apparatus, a Method checklist, and a 15-20 item Completion
Criteria list. #627 has none of these — just a machine block, an Outcome
paragraph, and five acceptance criteria. This may be intentional for the
lighter "C-series" template (#523 is similarly compact), but it means #627
carries far less evidentiary rigor than the programme's own established
standard for a task an executor is meant to pick up and run with, despite
depending on a comparably large unbuilt surface.

## What's solid (no rework needed)

- AC4 (a deliberately corrupted, single-pin-reassignment fixture must make
  the test red, naming the net) is a genuinely good mutation-style check —
  it directly defends against the check becoming vacuously green, and has
  no equivalent in #523.
- AC5 (the test runs in the same lane as the existing netlist goldens, and
  a new fixture is covered without editing the test) correctly forecloses
  the "one-off demo test" failure mode and is a sound, testable requirement.
- The core diagnosis — that #366's evidence plan and #460's byte golden
  jointly fail to prove structural correctness of the emitted netlist — is
  accurate and well-reasoned once #523's prior ownership claim is accounted
  for (Finding 1).
- AC2's core discipline ("artifact against source, never walk against
  walk") is the right defense against a self-fulfilling isomorphism test,
  and correctly reuses #336's IC-1 vocabulary.

## Verdict rationale

`needs-rework`: the underlying check is worth having and the two net-new
acceptance criteria (AC4, AC5) are good work. But the issue's text has
concrete, evidenced problems that would mislead an executor picking it up
cold — a false "no owner exists" claim contradicted by its own declared
parent filed hours earlier, an acceptance criterion (AC2) that omits the
packing-binding input its own AC1 requires, a silently dropped conditional
clause in AC3, and a parent acceptance criterion (parity narrowing) with no
counterpart or disposition here. None of these require abandoning the task,
but all of them should be reconciled against #523's text — ideally by
diffing the two issues line by line — before anyone treats #627 as a
ready-to-execute, self-contained spec. Separately, and not a drafting
defect: the test cannot be written today. Every type it needs to import
(`jls.netlist.NetPartition`, `jls.pkg.PackPlan`, `jls.pcb.KicadNetlistEmitter`)
is absent from `src/` in this checkout, and the prerequisite chain to get
there is closer to a dozen open issues than the five named.
