# Issue #292: HDL export: hierarchical SubCircuit — module per subcircuit type with instantiation, lifting the reject-list entry
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue's core technical premise (the exporter walk can be extended across
`SubCircuit` boundaries by reusing `groupOf`/`Operand` plumbing) is sound and
its code anchors check out against the actual `master` checkout. But the
issue as it currently stands is internally self-contradictory in a way that
would mislead an implementer who reads only the title, Abstract, and
checklists (§8/§14) — which is most of what a task-tier issue is for. A
same-day comment thread claims to supersede the design the body still states,
absorb ~23 requirements from two other closed issues without touching the
body, and silently drop/alter the issue's own `blocked_by`/`blocks` edges.
None of that is reflected in the machine-readable YAML block or the
checklists, so the issue is currently unsafe to hand to an implementer
without a body rewrite.

## Findings, most severe first

### 1. The issue's own title/Abstract/H1/Predictions describe a design its own latest comment calls "not implementable at all"

The title says *"module per subcircuit type with instantiation"* and the
Abstract states the shape resolved on #59 (2026-07-17): **"module per
subcircuit type, instantiated at each use"** — i.e. deduplicated, one
definition per type regardless of instance count. §4 states H1 in those
terms and §5's P1 asserts *"emitting N module definitions for N distinct
subcircuit types regardless of instantiation count."* §8's Method checklist
still says *"Module registry + recursive walk... two subcircuit types
legalizing to the same HDL identifier."*

The 2026-08-08T17:27:28 comment (issue-comment 5227272359) explicitly
"supersedes... §4 H1 of the body" and rules the opposite: **uniquified**
(one module per instance path, no shared identity at all), with the stated
reason that dedup has no key to deduplicate on. I verified this claim
directly against the checked-out repo: `SubCircuit` holds a per-instance
`@Nullable Circuit subCircuit` field (`src/jls/elem/SubCircuit.java:26`) and
`SubCircuit.save` inlines the whole nested circuit per instance
(`src/jls/elem/SubCircuit.java:287`, `getSubCircuit().save(output);`) — so
two placements of "the same drawing" are, in the loaded model, two
independent `Circuit` objects with no shared identity to key a module
definition on. The comment's technical argument for reversing the design
is correct.

The problem is not the reversal — it's that **the issue body was never
edited to match it** ("No body or title of this issue was edited" is stated
explicitly in an earlier comment on this same issue, 5180936410). An
implementer who reads the title and §4/§5/§8 as written would build the
deduplicated shape, which the issue's own most recent authoritative comment
says cannot be built at all without #340 (a separate, unstarted feature).
The stated acceptance criteria (P1-P3, the Method checklist's "collision
test: two subcircuit types legalizing to the same HDL identifier", and DoD
item "P1–P3 verified") are therefore currently unsatisfiable as literally
written, or satisfiable only by silently reinterpreting "subcircuit type" as
"instance path" — which is exactly the kind of undocumented reinterpretation
that lets acceptance criteria pass while missing the real (now-different)
goal.

**Recommendation:** before any implementation starts, edit the issue body
itself — title, Abstract, §4 H1, §5 P1-P3, §8 checklist, §14 DoD — to state
the uniquified plan of record. Do not rely on a comment to silently override
the primary spec.

### 2. `blocked_by`/`blocks` in the machine block are stale relative to what the latest comment claims

The issue's YAML block declares `blocked_by: []` and `blocks: []`. The
2026-08-08 comment's §5 ("Ordering, corrected") states `blocked_by: [336]`
is "real, and now the only ordering edge" (citing that #358, the now-parent
feature, generates a large golden corpus that would need regenerating a
second time if net names aren't stable first — #336/FEAT-004 owns that).
I confirmed #336 is open, and both of its own planned tasks (TASK-0007,
TASK-0008) are unfiled — so this is a real, currently-unmet prerequisite,
not a formality. The comment also asserts `blocks: [359]` is "real and
mirrored" on #359. None of this is reflected in the issue's own YAML block.

Separately: #358 (the feature this issue is being folded into) itself
declares `blocked_by: [315, 336]` in its own machine block — both open,
neither's task list filed — yet #292's comment thread only discusses #336
and explicitly declines to add a #315 edge (arguing #315's residual doesn't
apply here). That reasoning may be right, but it means #292's relationship
to its own parent's stated hard blockers is currently resolved only in
prose, never mirrored into #292's own machine block, which is what any
scheduling tooling or a future reviewer skimming the issue would actually
read.

**Recommendation:** mirror the `blocked_by: [336]` edge (or explicitly
record the waiver, per the comment's own "Waivable with that cost accepted,
and if waived, say so") into the issue's YAML block, not just a comment.

### 3. Scope was expanded ~3x via unilateral comment-absorption of two now-closed issues, with no corresponding edit to §8/§14

The final comment closes #385 (TASK-0043, IR + hierarchy walk — cycle
detection, per-module `HdlNames` scoping with a *guaranteed* net-name
collision test, reject-path folding that carries the instance path, jump
aliasing scoped correctly, a synthesized `clk` port for a `Clock` inside a
subcircuit that the parent must bind) and #384 (TASK-0044, both emitters'
instantiation syntax, six new goldens with "no new skip entry" as the load
-bearing acceptance bar, two new structural assertions
(`assertEveryInstantiatedModuleIsDeclared`, `assertEveryInstancePortExists`)
run against the *entire* 64-file existing golden corpus, a `ghdl -a`
width-mismatch negative-control experiment, golden-regeneration-must-not-
move-existing-files check) as duplicates, and states "everything below is
added to this issue's §8 Method and §14 DoD" — but never actually edits §8
or §14. That's roughly 23 enumerated sub-requirements (the comment's own
numbering) layered onto a body whose checklist still has five bullet points.
Combined with finding #1, an implementer who trusts the visible §8/§14 will
under-scope this task by a large margin — missing, at minimum, the
guaranteed-collision negative-control test, the `Clock`-port binding
decision, the cycle-detection-with-named-path requirement, and the "no new
skip entry" acceptance bar for the goldens, all of which materially affect
correctness and are exactly the kind of omission that lets a superficially
green PR pass review while missing real defects (e.g. two modules silently
colliding on `net_3`, or a subcircuit `Clock` port left unbound so the
emitted design "analyzes and does not run").

**Recommendation:** either paste the absorbed §4 items 1-23 into the issue's
own §8/§14 verbatim, or reference the comment by permalink from a top-level
"Absorbed scope" section the body actually contains — do not leave load-
-bearing acceptance criteria buried three comments deep.

### 4. Task-tier sizing is self-admittedly at the edge of feasibility for one branch/PR, with no fallback plan if it overflows

§7 of the final comment states this issue is "at the top of the task band"
and cannot be cut smaller "without producing a branch that does not
compile at either boundary" — an IR change to a *sealed* visitor
(`StatementVisitor`, 11 existing `void visit(...)` arms, confirmed by direct
grep of `HdlModel.java`) forces both `VerilogEmitter` and `VhdlEmitter` to
be updated in the same commit or the build simply doesn't compile. That is
a real, verifiable constraint (not exaggerated), and it means this is
effectively the full scope of two other issues (#385 ≈2wk + #384 ≈1.5wk =
~3.5 maintainer-weeks per #358's own cost table) landing as a single
task-tier PR — bigger than most of this tracker's task-tier issues, with a
larger single-review blast radius (IR + two full-language emitters + name
legalization + cycle detection + goldens across two toolchains). The only
stated fallback ("the only honest cut is at the golden-corpus boundary")
is not written into §8's checklist as an actual contingency step.

**Recommendation:** treat this explicitly as feature-tier-adjacent for
review-planning purposes (larger reviewer time budget, plan for a draft PR
checkpoint after code lands but before the golden corpus, per the comment's
own suggested cut point) rather than task-tier by label alone.

### 5. Process/authority concern: an automated pass reversed a previously-recorded "maintainer call" and closed two sibling issues, citing an unverifiable "maintainer grant"

Three earlier comments on this issue (2026-08-04 ×2, 2026-08-08 morning)
each explicitly declined to resolve the uniquified-vs-deduplicated question,
calling it "a maintainer decision... not a dedup finding." The final comment
overrides that with "under the maintainer grant this pass carries, a fourth
boundary comment would be a failed review" and proceeds to close #385/#384
as duplicates and rewrite #292's design authority. The technical argument is
sound (verified above), but the claimed authorization to make what three
prior passes called a maintainer-only call is not independently visible
anywhere in the three issues I read (#292, #385, #384) — it's asserted, not
evidenced. This is a governance gap worth flagging even though the outcome
looks technically correct: the same class of process (an agent asserting
its own authority to override a recorded human decision) could go wrong
silently in a case where the technical argument is weaker.

**Recommendation:** get an explicit human maintainer comment on #292
confirming the uniquified decision and the #385/#384 closures, independent
of the automated pass's self-declared authority.

## What's solid (no rework needed)

- The core technical anchors (reject-list Javadoc at
  `HdlExporter.java:87-88`, `EXPORTED` lacking `SubCircuit.class`, the
  `subCircuitIsRejectedCleanly` test) all check out against the current
  `master` tree as I read it directly — good evidence discipline for the
  parts that are current.
- H1's port-binding approach (reuse the existing `groupOf`/fused-net
  `Group`/`UnionFind` path so an unattached instance input becomes a zero
  literal like every other unattached input) is a reasonable, low-risk
  design choice consistent with existing exporter conventions
  (`docs/simulation-semantics.md`'s absent-input rule).
- The one-file-output decision and the "rejection stays total and one-pass"
  invariant are both consistent with the exporter's existing shipped
  contract and don't introduce new risk.
- Flagging the sealed-visitor mechanism as something that must not be
  weakened with a `default:` arm is the right thing to call out explicitly,
  and is independently corroborated by `CONTRIBUTING.md`'s "Sealed dispatch"
  rule.

## Bottom line

Fix before starting work: sync the body (title/Abstract/§4/§5/§8/§14) with
the uniquified plan of record and the absorbed #385/#384 scope, mirror the
`blocked_by: [336]` edge (or record its waiver) into the machine block, and
get an explicit maintainer confirmation of the design reversal recorded on
the issue itself rather than inferred from a comment claiming standing
authority. The underlying engineering plan is workable; the issue as
currently rendered is not safe to hand to an implementer without that
cleanup.
