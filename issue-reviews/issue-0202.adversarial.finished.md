# Issue #202: RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#202 is a `tier:feature` tracking issue with a typed machine block, pinned at
`evidence_commit: 29afb26`, that harvests the `riscv/` RV32I CPU toolchain
(landed via PRs #203/#211) into (1) CI integration goldens, (2) a curriculum
sample circuit, and (3) an HDL-export differential oracle. The one filed,
actionable child is #278 (fib/memtest golden promotion). Everything I could
directly verify against the checkout — `RiscvCpuGoldenTest.java`,
`HdlExporter.java`'s reject/EXPORTED lists, `pom.xml`'s test source root,
`RegisterFile.java`/`FieldExtend.java` — matches the issue's citations. The
problems are not in the cited facts; they are in governance: the issue's
own machine-readable status has gone stale relative to its own comment
thread, and the comment thread has silently exploded the issue's scope in a
way the issue's own rules (stated in its own body) forbid.

## Findings, most severe first

### 1. [Critical] The machine block's `blocked_by` names a dependency that has since closed without landing, and the issue's own DoD requires this to be caught
The body's YAML reads `blocked_by: [59, 62]  # gate this feature's CLOSE-OUT only`. I fetched #59: it is **CLOSED, `state_reason: not_planned`**. Its Memory-export scope (the actual thing #202's HDL-oracle direction needs) was re-owned onto tasks #291/#292 *before* #59 closed, and comment 5227468378 on #202 itself states it "now lives on #291 under the new feature #873." Yet #202's machine block still names the closed #59 as a live blocker. #202's own Completion Criteria demand: *"Machine block, roster table, and mermaid graph agree with reality at close"* — this is exactly the drift that rule exists to prevent, and it has not been fixed across at least one comment cycle after #59 closed (#59 closed 2026-08-03; #202's newest comment is 2026-08-08 and does not correct `blocked_by`).
**Recommendation:** Update the machine block's `blocked_by` to point at #291/#873 (or drop #59), and add a REPLAN comment recording the correction, per the issue's own re-planning protocol.

### 2. [Critical] Scope was redefined by an unreviewable comment thread that directly contradicts the body's stated foundation, and the contradiction is acknowledged but not resolved
Comment 5175893525 "absorbs" #326 (FEAT-038) into #202 — an enormous, differently-styled scope (regenerable boundary-decomposed machine, element-census budgets, `jls.mach` reference, seven new `blocked_by` edges: #317/#324/#325/#337/#343/#347/#364) — under a dedup rule that explicitly says "no body is edited." The result: a reader of the *body* sees a modest three-direction harvest of the existing `riscv/` toolchain ("Landed foundation... The full `riscv/` toolchain is on master"), while the *comments* record a binding decision (D5, migrated from #326) that **deletes `riscv/` entirely** and forbids any deliverable from routing through `riscv/build_cpu.py`/`riscv/jlsbuild.py`. Comment 5176134295 admits this outright: *"Decision D5 deletes `riscv/`… this cuts against #202's stated foundation."* The contradiction is recorded in prose but never resolved in the machine block, the roster table, or the DoD — which is precisely the failure mode #202's own template rules exist to prevent (rule: body integrates comment-borne truth so it "alone is accurate"). A contributor who reads only the body (the normal way to pick up work) will build "direction 1" on a foundation another equally-authoritative part of this same issue is actively slated to delete.
**Recommendation:** Either revert the #326 absorption to its own issue, or do the REPLAN the issue's own rules require: rewrite the body (not just add comments) so the `riscv/`-deletion decision and the harvest-of-`riscv/` foundation are not both asserted as current truth in the same issue.

### 3. [High] The Definition of Done is gameable: the machine-readable roster and GitHub's actual sub-issue graph both diverge from what the comments claim is owed
The body's `requires_tasks: [278]` is the only machine-readable gate, and I confirmed via `get_sub_issues` that #202 has exactly **one** formally linked sub-issue: #278. But comment 5227468378 asserts a three-item "adopted roster" (#392, #425, #278), entirely through prose: *"#392 and #425 have been given the `part_of_feature: 202` correction"* — a correction that exists nowhere in the body's `requires_tasks` list or in GitHub's sub-issue linkage. Per #202's own DoD, closing this issue requires only that *"Every entry in `requires_tasks` closed as landed."* Read literally, that checklist is satisfied by closing #278 alone; #392 and #425 (and the entire absorbed #326/FEAT-038 capability statement they're supposed to discharge) can be left permanently outside the checked gate. This is a real "verification could pass while the real goal fails" gap, not a hypothetical one — it already exists today, between what the machine block says and what the newest comment claims.
**Recommendation:** Add #392 and #425 to `requires_tasks` in the body (with a REPLAN comment), or explicitly state in the body why they are tracked outside the formal roster.

### 4. [High] Two of the three "roster" tasks cite evidence that the issue's own thread says will not survive
Comment 5227468378: *"#392 and #425 declare `evidence_commit: 2d0ca9d`, which #493 records as branch-only and scheduled for deletion… #278 was filed against `29afb26` and its citations resolve… prefer it, and re-derive the other two at pickup."* So two-thirds of the claimed roster is pinned to a commit the issue's own tracking says is going away, and the comment thread's own remedy ("re-derive... at pickup") is not itself a filed task — it is advice buried in comment prose that a future implementer must remember to apply. #202's DoD requires *"every cited evidence document and permalink resolves on the default branch at close"*; as written today, #392/#425 fail that bar by the issue's own admission.
**Recommendation:** File the "re-derive #392/#425 against a surviving commit" step as its own tracked action, not a prose aside.

### 5. [High] One-third of the issue's namesake deliverable — the thing in its own title — remains completely unfiled after eight status cycles
The title promises "integration golden, sample circuit, **and HDL-export differential oracle**." The newest comment (5227468378, same day as the issue's `updated_at`) states plainly: *"This issue's title names an HDL-export differential oracle… None of #392, #425 or #278 owns it… That is an unfiled row on your roster, not a discharged one."* This has been re-stated, not resolved, across the comment history (comments at 07-27, 07-28, 08-01 all note the same gap under a different blocking issue each time — #59, then #59 again, then #291/#873). Re-stating a known gap on every cycle-check without ever filing the task is scope drift by omission: the issue keeps its title's third promise alive in name only.
**Recommendation:** File the HDL-oracle task now (even if gated/unstartable), so it is a tracked, REPLAN-able row instead of a recurring footnote.

### 6. [Medium] IC1's evidence plan fully delegates to an unstarted, unlanded child with no independent check
§5's IC1 ("an induced simulator fault in memory-write timing is caught by the integrated suite even where the original single golden passes") is "evidenced by #278's P3 fault-injection experiment" alone. #278 is filed but has zero comments and is not yet in progress. If #278 lands with a synthetic/contrived P3 fault rather than one representative of real regressions, IC1 is satisfied by construction without demonstrating the broader net's real value. The criterion as written doesn't specify what makes a P3 fault representative, so it's underspecified rather than strictly gameable.
**Recommendation:** Add a concrete example class of fault (e.g., "gating condition removed from a real historical memory-write commit") to §5 so IC1 can't be discharged by a toy fault.

### 7. [Medium] `blocked_by` reasoning is now three issues deep in indirection and none of it is in the body
Tracing why #202's HDL-oracle direction is gated requires: #202's body → #59 (closed, not_planned) → #59's body's re-homing of #291/#292 → comment on #202 saying #291 now sits under "new feature #873" (not fetched/verified in this review — it is not cited anywhere in #202's own body or machine block, only in one comment). A reader trying to do the "Ordering-graph walk" the issue's own template mandates ("no path returns here") cannot do it from the body alone anymore.
**Recommendation:** Fold the #873/#291 pointer into the machine block's `blocked_by`, not just a comment.

## What is solid

- Every concrete, checkable citation in the body holds up against the checkout: `RiscvCpuGoldenTest.java` matches its cited line ranges and behavior (34-cycle golden, `x1/x2/x3` + one memory word, `test/fixtures/riscv-sum1to10.jls`); `HdlExporter.java`'s reject-policy comment ("Reject … SubCircuit, Memory") is accurate at lines 87-90, and both classes are indeed absent from the `EXPORTED` set (~422-428); `pom.xml`'s `<testSourceDirectory>test</testSourceDirectory>` is confirmed; `RegisterFile.java` and `FieldExtend.java` exist as claimed for #201.
- #278, the one formally filed and actionable child, is well-scoped, falsifiable, not blocked, and consistent with #202's stated direction 1 — it is the one clean, currently-trustworthy piece of this whole complex.
- The sub-word-memory scope exclusion is accurately sourced to `riscv/README.md`'s documented scope note.

## Bottom line

The verifiable technical claims in #202 are accurate, and its one filed child (#278) is sound and can proceed independently. But #202 itself, as a tracking/governance artifact, has drifted: its machine-readable status block is stale relative to its own comment thread (#59 closed but still listed as a blocker), an entire absorbed feature's scope directly contradicts the body's stated foundation without a body-level REPLAN, its literal Definition of Done can be satisfied without touching two of its three claimed child tasks, and its own title's third promise has gone unfiled through eight cycle-checks. None of this blocks #278 from landing, but #202 needs a REPLAN pass before its own close-out criteria can be trusted to mean what they say.
