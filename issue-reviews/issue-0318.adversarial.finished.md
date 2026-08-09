# Issue #318: FEAT-014 (RESIDUAL): nets, groups and nested instances get names that survive sharing, and geometry becomes one record per view
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The individual factual claims in the issue check out against the repository at
HEAD (`d6bc8dd`, matching the pinned `evidence_commit: 2d0ca9d...`): `Element`
is `sealed permits DisplayElement, LogicElement, Wire` (`src/jls/elem/Element.java:18`),
`WireNet` is a plain class with no `getStableId` (`src/jls/elem/WireNet.java:16`,
confirmed via grep), `SubCircuit.save` inlines the nested circuit per instance
(`src/jls/elem/SubCircuit.java:282-289`), `CircuitOp.apply` still takes a
`Graphics` (`src/jls/collab/op/CircuitOp.java:51`), and the `docs/file-format.md:394-396`
duplicate-`sid`-refusal text is quoted verbatim. The scope boundary (vs. FEAT-004
net naming, FEAT-017 sharing, FEAT-013 section semantics, FEAT-015 headless ops,
FEAT-012 merge rules) is coherent and non-overlapping. Where this issue fails an
adversarial read is not in its facts but in its process apparatus: an
undeclared dependency, a headline acceptance criterion that can be satisfied
without ever being tested, and a cost/estimate structure that is either stale
or internally inconsistent — none of which is visible unless you cross-read
the child and sibling issues, which the issue's own "mirrored edge" discipline
claims to make unnecessary.

## Findings

### 1. [High] The dependency graph this issue is proudest of has an undeclared edge

`#318`'s machine block states `blocked_by: [319, 337]` only, and the DAG-walk
prose explicitly claims completeness ("a half-edge is the defect this Link
pass exists to prevent"). But `#318`'s own filed child, **#472 (TASK-0035)**,
carries `blocked_by: [468]` — TASK-0007, "exactly one net-partition walk...
instead of five copies" — with the stated reason "an id minted against one of
two partitioners that can disagree writes an ambiguous identity into the file
format" (verified: `#468` is real, open, `part_of_feature: 336` (a *different*
feature, FEAT-004), and its own `related` list names `#318` explicitly: "net
identity keys off this partition"). `#318`'s body never mentions `#468`
anywhere. Anyone scheduling or estimating `#318` from its own machine block
alone will miss that net identity — the half the issue itself calls
"expensive" — cannot start until a separate feature tree (FEAT-004) lands a
task that `#318` doesn't cite. **Recommendation:** add `#468`/FEAT-004 to
`#318`'s `blocked_by` (or at minimum a `related` note under §"Sequencing"),
and regenerate the mermaid graph — the same discipline the issue enforces on
`#319`, `#337`, `#329`, `#333`, `#357`.

### 2. [High] The one criterion that matters most can close as "done" without ever being exercised

§5 criterion 2 — uniqueness surviving a **shared, not copied**, subcircuit
instantiation, "the case flat ids fail" — is explicitly marked "vacuous until
a shared-definition fixture exists," and that fixture requires **#357
(FEAT-017)**. But `#357` is `blocked_by: [318, 319, 340]` — i.e. `#357` cannot
land before `#318` does. The Definition of Done only requires criterion 2 be
"verified... with criterion 2 explicitly marked vacuous-or-live," which means
`#318` can be formally closed having demonstrated its headline capability
("makes an address... real... including the case flat ids fail") only in the
easy, already-injective case, while the hard case it was filed to solve stays
permanently unverified pending a feature that may never land. This is a
gameable acceptance criterion: closing with "criterion 2: vacuous" satisfies
the letter of the Definition of Done while leaving the issue's own stated
reason for existing unconfirmed. **Recommendation:** either (a) require a
synthetic shared-definition fixture built without `#357` (e.g. two
`ItemKey`s manually constructed to collide) so injectivity is tested against
the *scheme*, not against a fixture that needs a sibling feature, or (b)
state explicitly in the Definition of Done that "vacuous" is an acceptable
terminal state and that the capability claim in §1 should be qualified
accordingly at close.

### 3. [Medium] The evidentiary base for the plan (cost bands, maintainer decisions D2/D3/D9/D15) does not exist anywhere in this repository

The issue repeatedly cites `docs/plan/evidence/BRIEF.md`, `docs/plan/features/FEAT-014-*`,
`docs/plan/tasks/TASK-0035-*` / `TASK-0036-*`, and decisions D2, D3, D9, D15,
all "landed in `3a81a4a...`, not present at `2d0ca9d`." Checked: `docs/plan/`
does not exist in the working tree, and `git log --all -- docs/plan` returns
**no commits, on any branch**, at any point in this repository's history. The
commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc` that supposedly carries it
is not reachable here either. Every cost figure (the "11-17 mw" band, the
"2.75x-4.25x" gap), every maintainer ruling (D9's audience decision, D15's
sidecar ruling cited by `#319`), and the corpus rule numbers ("rule 3(c)",
"rule 6", "rule 10") this issue and its children lean on for scope and
priority are therefore **unauditable from the repository an implementer would
actually be handed**. This is a transparency risk, not necessarily a factual
error — but a reviewer or implementer with only `git clone` access cannot
check a single one of these claims, and the issue presents them with the same
confidence as the `2d0ca9d`-pinned code citations, which *are* independently
verifiable. **Recommendation:** either commit `docs/plan/` to this
repository (even as an appendix) or stop citing it as load-bearing evidence
for scope/cost decisions inside issues meant to be independently actionable.

### 4. [Medium] Two "not filed" facts in the body are false, and the fix was applied as comments, not as a body edit

The §2 roster table and the `planned_tasks` YAML both say TASK-0035 and
TASK-0036 are "not filed." Both are filed (`#472`, `#383` — confirmed open).
Two separate comments (2026-08-04, 2026-08-08) record this as "roster
staleness" and explicitly decline to edit the body ("rule 1 — bodies are
never edited by this pass"). The practical effect: any tool or agent that
reads the issue body only (which is exactly how issues are typically
consumed, and how this review was scoped) sees stale status, stale "not
filed" tasks, and a stale Open Questions section (see finding 5) unless it
also pulls every comment. Given that this same corpus explicitly worries
about agents re-filing already-filed work ("supersession check," "rule 6" in
child issues), leaving the body wrong while relying on comments to carry the
correction is a self-defeating process choice. **Recommendation:** apply a
body edit reconciling §2/`planned_tasks` with `#472`/`#383` now that both
are filed, rather than accumulating a third dedup-pass comment.

### 5. [Low] Open Question 1 is presented as unresolved but is already answered and implemented by a filed child

`#318` §"Open Questions" asks "Does `WireNet` become an `Element`, or carry
identity by a parallel mechanism?" and calls this a decision that "blocks
TASK-0035's implementation." But `#472` (TASK-0035) has already chosen and
fully specified option (b) — a parallel `nid` attribute on the lowest-`sid`
wire end, not widening `Element`'s `permits` list — down to the interface
contract (`WireNet.getNetId()`), the failure modes, and the migration story.
`#318`'s own text hasn't been updated to reflect that its blocking open
question is resolved. A reader of `#318` alone would reasonably believe the
`Element`-widening question is still live.

### 6. [Low] Cost accounting is either stale or the estimate is not credible, and nobody has reconciled it

`#318`'s Cost section attributes the "11-17 mw vs. 4 wk" gap largely to net
identity being "the expensive half" with "no task id." But `#472` (TASK-0035,
priced at 2 wk in `#318`'s own roster) already carries a full interface and
data contract for net identity: a new `nid` save attribute touching every
wire-net-bearing file in the golden corpus, a migrated load-time uniqueness
check across `(instancePath, sid)`, a changed `Ops.resolve` signature hitting
every op call site, and a required coordination with two other format-epoch
tasks (`#436`, `#437`) so goldens regenerate once instead of three times. That
is a materially larger 2-week task than the framing in `#318` ("net identity
is the expensive half... [with] no task id") suggests it's pricing. Either
the 11-17 mw band double-counts work that's now inside the priced 4 weeks, or
2 weeks is optimistic for what `#472` itself specifies. Open Question 4 in
`#318`'s own body concedes this is unresolved ("blocks nothing... must be
answered before anyone reports this feature as funded") — which is honest,
but it means the issue's own cost framing should not be treated as reliable
by whoever plans around it.

## What's solid

- Every code-level citation checked (Element.java, WireNet.java, SubCircuit.java,
  CircuitOp.java, docs/file-format.md, docs/operation-layer.md) is accurate
  against HEAD.
- The scope boundary against neighboring issues (#334, #357, #319, #356) is
  specific, non-overlapping, and each exclusion names its actual owner.
- The invariants (byte-identical legacy saves, no simulation-hot-plane
  reach, `mvn verify` green, one view vocabulary) are testable and concrete.
- The rejected-alternatives list (dense-id-plus-prefix, lazy net minting,
  a second x/y pair) gives real reasons tied to existing code, not just
  assertion.
- The dependency edges that *are* declared (#319, #337, and downstream #329/
  #333/#357) are each independently verified as real, open, and mutually
  consistent with what those issues themselves state.

## Verdict rationale

Not `needs-rework`: the technical design is coherent, grounded in the actual
codebase, and the scope cuts are defensible. Not `sound` outright: a
real (if second-order) planning defect — the undeclared `#468` dependency —
and a genuinely gameable core acceptance criterion (finding 2) are the kind
of thing that causes a "closed" feature to quietly not deliver the property
it was filed for. `sound-with-concerns` reflects that the issue is safe to
schedule and start (TASK-0035's non-net-identity parts, view vocabulary
registration) but should not be treated as closeable against its own
Definition of Done without the fixes in findings 1 and 2.
