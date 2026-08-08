# Issue #464: TASK-0104: the analog solver becomes homework-grade — a named escape ladder, a per-device convergence veto, diagnostics that name the drawn element, and a 200-circuit hard corpus with a numeric kill criterion
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## 1. The issue's entire evidentiary foundation cites documents that do not exist in the tree

The issue repeatedly cites `11-analog-determination.md` §5 stage S10, §8.3, §8.4 as "the written owner" of this stage, "including the kill criterion this task's acceptance test measures" — the 5% non-convergence gate, the 141s required-CI-gate figure that P4's 4s corpus budget is checked against, and the RELTOL/VNTOL/ABSTOL-derived tolerances the sibling task (#463) needs. It also leans on `docs/plan/evidence/BRIEF.md` "ruling D10" for the claim that "nothing here refuses a capability by citing its absence."

Neither file exists anywhere in the current tree: `find . -iname "*analog-determination*"` and `find . -iname "BRIEF.md"` both return nothing, and `git log --all -- '**/BRIEF.md' '**/11-analog-determination.md'` returns no history either (repo is a shallow clone, so pre-history can't be fully ruled out, but the files are absent at HEAD and the issue itself claims they "landed" at a specific commit that is also not resolvable in this checkout). Every load-bearing number in this task — the 5% kill criterion, the 141s gate, the tolerance derivations — traces back to a document a reviewer cannot read. Compare with #463, which cites the same phantom document for RELTOL/VNTOL/ABSTOL and literally lists "What are RELTOL, VNTOL and ABSTOL?" as an *unresolved open question* in its own body — so the numbers this task depends on are not even settled in the one task that's supposed to define them.

**Recommendation:** do not accept this issue's gate figures as given. Require `11-analog-determination.md` and `docs/plan/evidence/BRIEF.md` to be committed and reviewable (or replace every citation to them with a self-contained derivation) before treating "5%" and "141s" as binding.

## 2. Direct contradiction with the repository's own recorded architecture decision

`docs/capability-roadmap/sweep-06-physical-boundary.md:83` states, verbatim, about IBIS/Touchstone: **"No continuous-time solver, and none should be added."** That line is not a stray aside — it's a considered position in the same capability-roadmap document set that #464, #463, #402 and #331 all lean on for cost/scope framing, and it flatly contradicts the premise that JLS should grow an MNA/Newton/LU transient solver at all. README.md and ARCHITECTURE.md are unambiguous about current identity too: "JLS is an educational **digital** logic circuit editor and simulator," and ARCHITECTURE.md's recorded decision "Simulation execution strategy: discrete-event interpreter is the sole strategy" (#221) discusses only the two-state-plus-HiZ digital value domain — there is no analog value domain, no continuous time base, and no design note reconciling one with `Simulator.runEventLoop`'s single discrete `now`.

This issue treats the analog programme as settled scope ("Per BRIEF.md ruling D10, that document's survey evidence stands") while the one architecture document actually in the tree says the opposite. That's not a minor tension — it's the kind of scope decision that needs a maintainer ruling recorded in ARCHITECTURE.md, not an assumption buried five issues deep in an unfiled dependency chain.

**Recommendation:** resolve the standing "no continuous-time solver" recorded position explicitly (supersede it in writing, in ARCHITECTURE.md or the capability-roadmap doc, with a dated rationale) before any task in this chain is picked up.

## 3. The 200-circuit corpus and its 5% gate are gameable by construction

§10's binding kill criterion is "if after 6 maintainer-weeks... the 200-circuit corpus non-convergence rate is above 5%... restrict the shipped palette... and stop." But Open Question 5 in the same issue admits the corpus's composition is not fixed: *"Where does the 200-circuit corpus come from if fewer than 200 circuits failed during development? Recommended: make up the balance with the deliberately hostile constructions already named."* The same engineer who builds the escape ladder also selects which circuits go in the pass/fail corpus that measures the ladder, with only a soft, non-binding request to "record the split." A corpus weighted toward circuits the ladder is known to handle will trivially clear 5% while saying nothing about real robustness — the issue's own §11 admits as much ("A corpus assembled to pass is not evidence... A corpus weighted toward easy topologies would report a rate that says nothing") but supplies no independent check (no external circuit source, no blind review, no held-out set) to prevent exactly that.

**Recommendation:** require the corpus's provenance split (historical failures vs. constructed) to be reviewed and signed off *before* the ladder implementation is finalized, not recorded post hoc at close-out as currently specified.

## 4. Feasibility/cost: this task sits three unlanded, multi-week dependencies deep on a single-maintainer project

`blocked_by: [463, 402]` — both open, both unimplemented (`git grep -lci "gmin|pseudo-transient|convtest|optran|analog|spice|newton|nodal" -- src/ test/` returns nothing at HEAD, confirming §2 O1's own claim). #463 alone is costed at "3.5-5 maintainer-weeks including the linear fast path" for just the solver core; #402 adds controlled sources, waveforms and a card grammar on top; #464 itself budgets "roughly one week" for the ladder code plus an explicitly *unbounded* second stage ("This stage is not LOC-bounded and must not be compressed... Budget the second week for the corpus"), with a 6-maintainer-week kill clock layered on. README.md and ARCHITECTURE.md both describe JLS as effectively a single-maintainer pedagogy tool (see the recorded i18n decision: "JLS is a single-maintainer pedagogy tool"). Stacking three multi-week, sequentially-blocked tasks (#463 → #402 → #464) before any user-visible capability lands is a multi-month critical-path commitment for one person, on top of the maintenance backlog the rest of the open-issue set already represents.

**Recommendation:** either commit explicitly to the multi-month timeline with checkpoints, or (preferably, given Finding 2) revisit whether this belongs on the roadmap before de-risking it task by task.

## 5. Internal ordering/scope tension with TASK-0103 and FEAT-049

The issue asserts "This task is not blocked by TASK-0103" and that the two run in a deliberate reverse order (harden first, then extend the veto per device family as TASK-0103 lands). That's a reasonable sequencing argument on its face, but TASK-0103 is unfiled — there is no issue number, no committed scope for the device models #464's own H1/H3 falsification steps assume will eventually consume `DeviceModel.converged(SolverState)`. The plan is coherent only if TASK-0103 is filed with a compatible contract; nothing here binds that. Similarly, #331 (FEAT-049) sequences TASK-0104 (this issue) *after* TASK-0105 (per-view palettes, also unfiled) and TASK-0103 in its own roster table, while #464's `blocked_by` only lists #463/#402 — the feature-level roster and this task's own dependency block disagree on where TASK-0104 sits relative to TASK-0105/TASK-0103, even though both documents were filed the same day (2026-08-03) by the same author.

**Recommendation:** reconcile the `blocked_by` graph across #331, #463, #402 and #464 explicitly; right now three sibling documents disagree in mutually non-obvious ways about ordering.

## Solid parts (brief)

- O1-O3's file:line citations against the current codebase (`Simulator.java:215-228`, `HeadlessCoreRatchetTest.java`'s `CORE_PACKAGE_PREFIXES`) are accurate and verifiable — I checked them directly against HEAD and they match.
- The mathematical formalization of the convergence predicate, the pivot-tie-break-adjacent escape ladder, and the H2 diode-current-vs-node-voltage argument (§7.10) is internally consistent and a genuinely well-known SPICE-convergence problem, not invented physics.
- The constraint that no ladder rung may touch `Simulator.runEventLoop`'s loop condition or `now` advance (O2) is a correct, specific, checkable boundary, and P7's determinism requirement (pure function of solver state) is testable as stated.
- `DeviceModel.converged` defaulting to `true` is a genuinely safe, additive interface choice that doesn't break existing device kinds.

## Bottom line

The task is mathematically coherent and its in-tree citations are accurate, but it rests on load-bearing external documents that do not exist in this repository, contradicts a recorded architectural position in the repo's own capability roadmap ("No continuous-time solver, and none should be added"), specifies a self-graded acceptance corpus with no independent check, and sits at the far end of a three-deep chain of unlanded, multi-week dependencies on what the project's own docs describe as a single-maintainer tool. None of that is fixable by editing this issue alone — it needs the phantom documents committed or the citations replaced, and it needs an explicit maintainer ruling superseding the "no continuous-time solver" position, before it's safe to schedule.
