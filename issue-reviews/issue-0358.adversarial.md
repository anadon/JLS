# Issue #358: FEAT-018: a decomposed design exports — the HDL IR carries more than one module and an instantiation statement, so nesting becomes hierarchical Verilog and VHDL instead of a refusal
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The underlying engineering idea — give the HDL IR an instantiation statement
and a multi-module design container so nested `SubCircuit`s export instead of
being refused — is sound and matches real code (sealed `StatementVisitor` in
`src/jls/hdl/HdlModel.java`, `SubCircuit.save` inlining the nested circuit at
`src/jls/elem/SubCircuit.java:282-289`). But the issue as filed is not safe to
hand to an implementer today: its own §1 evidence section cites code that does
not exist on `master`, its machine block (planned_tasks, blocked_by, Open
Question 1) is already superseded by its most recent comment but was never
edited to match, and the cost model that shaped four days of "Open Question 1"
debate was built on a premise (dedup is a cost trade-off) that turns out to
have been categorically wrong. None of this is fatal to the capability — it
is a currency/self-consistency problem the issue must fix before work starts.

## Findings, most severe first

### 1. [Critical] The issue's core evidence (§1) cites code that is not on `master` — verified independently

The issue's "Absent at `2d0ca9d`, verified" section quotes a `REJECTED` map
with per-entry reason strings at `src/jls/hdl/HdlExporter.java:460`, e.g.:

> `SubCircuit.class, "subcircuits cannot be exported yet: the HDL model has no module-instantiation statement, ..."`

I checked the actual checkout at `/home/user/JLS` (HEAD matches
`origin/master`, only `issue-reviews/` files differ). There is no `REJECTED`
symbol anywhere in `src/jls/hdl/HdlExporter.java`. Classification is a
three-bucket fall-through: `EXPORTED`/`SKIPPED`/`TOPOLOGY` sets at lines
422-437, with anything not in one of the three (including `SubCircuit`, which
is referenced nowhere in that file) falling to `offenders.add(describe(el))`
at line 191 — a bare position description, **no parenthetical reason**. I also
confirmed `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` and
`classifiedElementClasses()`, both cited elsewhere in this issue's thread as
already existing, are absent from `test/jls/hdl/HdlPolicyTest.java` (only
`subCircuitIsRejectedCleanly` at line 77 exists).

This is not a new discovery on my part — the issue's own first comment
("Evidence-pin notice", 2026-08-03) already flags `evidence_commit` as
branch-only and names `HdlExporter.java:460` as a branch-only anchor, and the
2026-08-08 comment independently re-derives the same conclusion in more
detail. **But the issue body itself was never corrected.** Anyone who reads
the issue top-to-bottom — the normal way to consume a GitHub issue — inherits
a capability statement, a refusal-message string, and a "policy bucket" that
do not exist in the tree they are about to branch from.

Recommendation: edit §1 in place to cite the real `EXPORTED`/`SKIPPED`/
`TOPOLOGY` fall-through structure (already correctly described by #292, whose
citations I spot-checked and which do resolve on master), not the branch-only
`REJECTED` map. Do not leave the correction only in a comment thread.

### 2. [Critical] The visible machine block contradicts the most recent comment; nothing forces the body to be updated

The issue body's YAML machine block still reads:

```yaml
planned_tasks:
  - "TASK-0043 - ..."
  - "TASK-0044 - ..."
blocked_by: [315, 336]
```

and Open Question 1 is presented as an unresolved maintainer trade-off
("Recommended default: uniquified first... This blocks filing the
children"). The 2026-08-08 17:29 comment ("ROSTER RESOLVED") supersedes all
of this: TASK-0043/TASK-0044 were filed as #385/#384, both closed as
duplicates of #292 (verified — both show `state: closed`,
`state_reason: duplicate`, closed 2026-08-08); `requires_tasks` is now
`[292]`; `blocked_by: [315]` is explicitly withdrawn ("the edge's premise
does not hold, and its conclusion does not follow either"); and Open Question
1 is declared answered (uniquified) on **new** grounds (SubCircuit holds no
shared identity across placements, so dedup isn't a cost choice, it's
currently infeasible).

None of this is reflected in the rendered issue description, the
decomposition table in §2, the dependency mermaid diagram, or the Open
Questions section. A contributor or an automated scheduler that reads the
issue body (not all four comments in order) will: file two child tasks that
already exist and are already closed as duplicates; treat `blocked_by: [315]`
as live when it has been withdrawn; and treat Open Question 1 as blocking
when it is resolved. This is a process defect specific to how this
issue-corpus is maintained (REPLAN outcomes recorded only as comments, never
folded back into the body) and it directly undermines the issue's own DoD
item "Machine block, roster table, and mermaid graph agree with reality at
close" — that item is already false mid-flight, not just at close.

Recommendation: before any implementer picks this up, edit the body's machine
block, §2 table, Open Questions, and mermaid diagram to match the current
`requires_tasks: [292]` state, or explicitly close #358 in favor of tracking
directly on #292 with #358 kept as the feature-tier record of the five
integration criteria only.

### 3. [High] Three successive, mutually exclusive justifications for the same `blocked_by:315` edge, none checked against the actual repo until the fourth pass

- #358 (original): blocked on #315 because "the bucket must be total over the
  element registry before entries start moving out of it."
- #385 (a since-closed duplicate): argued #315 should be `related`, not
  `blocked_by`, because the totality test (`HdlPolicyTest:392`,
  `exportPolicyIsTotalOverTheElementRegistry`) *already exists* and already
  pins totality.
- #358 comment 4 (2026-08-08): both of the above were "equally false" —
  neither the `REJECTED` map/bucket nor the totality test exist on `master`
  at all; classification is an untotaled fall-through, and #492 (not reviewed
  here) is the open issue that would make it total.

Three different technical justifications for one dependency edge were
asserted with confidence across this issue's six-day life, all resting on
citations nobody validated against the actual tree until the last pass. This
is a pattern, not an isolated slip (see Finding 1), and it means every other
unverified edge still standing in this issue's machine block (`blocks: [359]`,
`serves_capstones: [298, 302, 304, 307, 310]`, the `related` list) should be
treated as unverified rather than authoritative until someone re-derives it
against `master` the way comment 4 did for #315.

### 4. [High] The central "Open Question 1" cost/pedagogy trade-off framing was wrong from the start, and the issue's Cost section still reflects the wrong framing

§ "Open Questions & Decisions Needed" #1 and the Cost section frame
uniquified-vs-deduplicated as a maintainer judgment call between "4-6 weeks"
and "6-8 weeks", with a stated pedagogical cost to shipping uniquified first
("forfeits the 'one module reused N times' lesson"). Comment 4 shows this was
never a real choice: I independently confirmed
`private @Nullable Circuit subCircuit;` at `SubCircuit.java:26` — each
`SubCircuit` instance holds its own independent `Circuit` object with no
shared identity across two placements of "the same" drawing, so there is no
key to deduplicate on without FEAT-016's (#340's) structural digest. Dedup
was infeasible, not merely more expensive, and multiple issue-days (and the
"6-8 wk" figure baked into the Cost section) were spent debating a magnitude
question that should have been a five-minute feasibility check against
`SubCircuit.java`. The issue's Cost section is not corrected to reflect this.

### 5. [Medium] DoD item "every cited evidence document and permalink resolves on the default branch at close" is already failing, and nothing gates further planning on fixing it

Comment 4 continues restructuring the roster, closing children, and
re-answering Open Questions while the underlying §1 citations remain
uncorrected in the body (Finding 1). If the DoD's own checklist items were
actually enforced as gates rather than aspirational text, this issue would
have stopped to fix §1 before touching the roster. As written, the DoD reads
as a checklist assembled at closing time, not a set of invariants maintained
throughout — worth being explicit about, since a future closer could tick
that box by spot-checking only the newest comment's citations (which are
accurate) and miss that the body's older citations are not.

### 6. [Medium] Cost accounting is unverifiable post-collapse

The issue states "Band 4-6 mw. Printed task sum: 3.5 wk ... The band exceeds
the sum by 1.14x-1.71x." After comment 4 collapses the two-task roster
(3.5 wk sum) into a single task, #292, the "Cost, restated honestly" section
says only "the band still exceeds the row" without stating the row's value —
#292's own body carries no maintainer-week estimate field at all (it is a
`tier:task` issue with a §-by-§ scientific-method template, not a cost
table). A reviewer cannot check whether "4-6 mw" is still a sane estimate for
what #292 alone now scopes, versus what TASK-0043+TASK-0044 combined would
have cost. Recommendation: either #292 gains an explicit cost line, or the
comment restates the row's value rather than asserting the inequality without
data.

### 7. [Medium] Deep-fixture identifier-length risk is named but not bounded

§5 criterion 2 and §3's transformation section correctly flag that module
identifier length grows with instance-path depth and that "the identifier
legalizer must therefore be shown a deep fixture" — this is a real and
well-called-out risk. But nothing in the criteria states a maximum practical
identifier length or a truncation/hashing fallback. Verilog-2005 and VHDL-93
tools commonly impose (or silently truncate at) identifier-length limits well
below what four-plus levels of verbosely-named student subcircuits could
produce (e.g., a chain of `Adder Slice 4`-style user-typed subcircuit names
uniquified per path). A criterion that only checks "every emitted module
identifier is distinct" (byte-identical + injective) can pass on a corpus of
short synthetic fixture names while the legalizer still emits identifiers
real classroom circuits would produce that a real compiler rejects — which is
exactly the class of gap the "must compile under iverilog/ghdl" integration
criterion (§5.1) is supposed to catch, but only if the deep-fixture test
(§5.2) actually exercises names long/ugly enough to trigger it. As written,
nothing requires that.

### 8. [Low] Acceptance criteria that are solid as written

- Criterion 3 (nesting cycle reported with instance path, no stack overflow)
  and criterion 4 (reject propagation carries instance path) are concrete,
  falsifiable, and correctly identify the likely bug ("the natural
  implementation, catching and re-throwing per level, loses the path").
- The "two tasks in one branch" architectural reasoning is verified real:
  `HdlModel.java`'s `StatementVisitor` pattern is genuinely sealed/total per
  its own javadoc, so adding a statement kind does break both emitters'
  compilation until handled — that is a legitimate reason not to split the
  IR change from the printer change, independent of the corpus's own
  meta-process problems.
- Global invariant 1 (flat-circuit output stays byte-identical) and
  invariant 6 (policy test re-pointed, never deleted) are exactly the right
  guardrails for this kind of change and are testable as stated.
- No licensing, security, or GPLv3 in-process-linking hazard: this is a
  pure-Java IR/codegen change with no new external dependency; the existing
  subprocess boundary for `iverilog`/`ghdl` (validation only, install-or-skip)
  is unchanged, consistent with ARCHITECTURE.md's recorded plugin-trust-
  boundary decision (#222).

## Verdict rationale

The technical shape of the feature is defensible and grounded in real code.
But the issue is not currently safe to execute against as filed: its
evidentiary basis is demonstrably stale (Finding 1, independently
reproduced), its visible plan contradicts its own latest resolution
(Finding 2), and the process that produced three successive, unverified
justifications for the same dependency edge (Finding 3) and an entirely
wrong cost framing that stood for days (Finding 4) raises real doubt about
every other unverified claim still standing in the corpus. This is
`needs-rework`: the body needs to be brought into agreement with reality and
with its own latest REPLAN comment before a child branch starts, not
`should-not-proceed` — the capability itself, and the #292 task that now
solely carries it, are sound.
