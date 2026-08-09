# Issue #473: TASK-0042: one elaboration pass turns definitions plus bindings into a resolved design, and every binding it cannot resolve is a coded diagnostic naming the instance path — never a silent default
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## 1. Evidence citation is already conceded wrong by the issue's own thread (high)

Observation O4 quotes `HdlExporter.java` L196-204 as `String reason = REJECTED.get(el.getClass()); offenders.add(reason == null ? describe(el) : describe(el) + " (" + reason + ")");`. The issue's own bot comment (2026-08-03) concedes this snippet is branch-only: on `master` (verified live at `src/jls/hdl/HdlExporter.java:191-199`) there is no `REJECTED` map at all — the line is simply `offenders.add(describe(el));`. I confirmed this directly:
```
offenders.add(describe(el));
...
throw new HdlExportException("circuit \"" + circ.getName()
        + "\" contains elements HDL export does not support"
        + " yet: " + String.join("; ", offenders));
```
The structural claim ("one concatenated string, no per-offender reason") still holds and is if anything *stronger* evidence for the issue's thesis, but the issue body has not been corrected — an implementer who re-derives O4 from the cited line range will find code that doesn't exist on `master` and either stall or silently patch over the discrepancy. Issue #493 (open) is the tracked fix; #473 should not be picked up until its citations are re-pinned per #493 §3/§5, which lists #473 among the 29 issues "wrong about master."
**Recommendation:** re-derive O4 at current `master` before work starts; do not trust the `2d0ca9d` evidence_commit citations as filed.

## 2. Deep, entirely-unbuilt dependency chain — feasibility risk (high)

`blocked_by: 447, 472`. I verified both are open with no landed code: `jls.elab` does not exist, and neither does `DefinitionId` or `ItemKey` anywhere in `src/` or `test/` (grep returned nothing). #447 itself is `blocked_by` #340/#318/#319, and #472 is `blocked_by` #468 — none of which show any sign of having landed either (repo has no `jls.core.ItemKey`, no definition table on `Circuit`). This issue is not "ready to pick up" in any real sense; it sits at the end of a multi-task, multi-week chain (#357 itself estimates 25-36 mw against 4 wk of summed task rows for just the two child tasks, with the gap unexplained). Filing it now as if it were actionable, complete with a fully fleshed-out interface contract for a `Diagnostic` type keyed on `ItemKey`, risks the interface being designed against a definition table and addressing scheme that don't exist yet and may still change shape in #447/#472 review.
**Recommendation:** treat §7 (Interface & Data Contract) as provisional until #447 and #472 actually land and their real `DefinitionId`/`ItemKey`/binding shapes are known; re-verify the contract against the landed types before implementing, not before filing.

## 3. Cycle-detection acceptance criterion is narrower than the hypothesis it's meant to prove (medium)

H1/stage 3 defines cycles generally: `c = (d1 → d2 → ... → dk → d1)`. But the only test predicated on this, P4, is stated as "A definition reaching itself yields E-ELAB-004 listing both `DefinitionId`s in cycle order" — language that only unambiguously covers a 2-node cycle (`d1 → d2 → d1`), not a longer one. Nothing in Predictions or the Definition of Done requires a 3+ node cycle fixture. An implementation could hard-code detection/reporting for direct mutual recursion, pass every listed prediction and DoD item, and still infinite-loop or mis-report on a 3-cycle — exactly the "explicit stack, not recursive" mechanism the issue itself calls out as necessary for hostile input (7.5), but whose correctness on longer cycles is never actually pinned by a fixture.
**Recommendation:** add an explicit ≥3-node cycle fixture and assert the full ordered cycle list, not just "both" ids.

## 4. "Total, never throws" contract has an unexercised edge (medium)

§7.4 states `elaborate` is total for resolution failures and "may still throw" only for a programming error (null circuit) — framed as a clean dichotomy. But the six diagnostic codes (E-ELAB-001…006) don't obviously cover every hostile-input shape from a `.jls`-derived definition table: e.g., a binding whose *value* is structurally malformed in a way that isn't a range/kind mismatch (E-ELAB-005) — the issue assumes values arrive already well-typed from #447's own parser, but #447 hasn't landed, so this assumption is untestable now and unverified against the real type. If #447's binding values can be malformed in a way none of the six codes anticipates, `elaborate` either throws (violating "never a throw for a resolution failure") or silently miscategorizes the failure as a different code — and nothing in the DoD would catch that, since P6 only checks that every *declared* code is exercised, not that every *possible* hostile input maps to one of the six.
**Recommendation:** once #447 lands, explicitly enumerate the malformed-value cases its parser can hand to the elaborator and map each to one of the six codes (or add a seventh) before implementation, not after.

## 5. Third caller ("the simulator's setup") is asserted without the citation discipline used everywhere else (low-medium)

§7.4 lists expected callers as "`Circuit.finishLoad`, `HdlExporter.buildModel` (O5), and the simulator's setup." The first two carry direct O#/file:line evidence; the third does not — no observation, no file, no method name. Given the issue's otherwise rigorous "every claim gets a permalink" discipline, this is a bare scope expansion: it silently adds a third production call site (with its own 7.11 failure-mode row: "does not start a simulation of a design that did not elaborate") that was never located in the codebase, so nobody has checked whether it's one call site or several, or whether it's even straightforward to wire (e.g. `BatchSimulator` vs `InteractiveSimulator` vs both — ARCHITECTURE.md documents them as separate classes with different threading models).
**Recommendation:** cite the actual simulator setup call site(s) (plural, if both batch and interactive need it) before the Method checklist step "Convert … the simulator setup to the one pass" is treated as a single bounded task.

## 6. Open Questions are load-bearing but deferred to implementer judgment (low)

OQ1 (does E-ELAB-003 consult a library resolver) and OQ2 (is E-ELAB-006 ERROR or WARNING) are both marked "blocks execution" of specific stages, yet both are left to a "recommended default" the assigned implementer applies unilaterally rather than a maintainer ratifying before work starts (contrast with #357's OQ1, which is explicitly "blocks filing TASK-0041" until a maintainer decision). If OQ2 resolves to WARNING post-hoc, `HdlExporter`'s refusal behavior in the 7.11 table (which currently reads as unconditional for any ERROR-severity diagnostic) needs no change, but any code or test written today assuming ERROR would need rework — a rebase cost the issue doesn't flag as a risk.
**Recommendation:** ratify OQ1/OQ2 as maintainer comments before implementation begins, not silently via "recommended default."

## What's solid

- The core thesis (diagnostics as structured data, not concatenated prose; O2/O4 loader-vs-elaborator contrast) is well-motivated and the O2 citation (`docs/file-format.md:220`) checks out verbatim against the current file.
- `Util.copy`/`Util.partition` citations (`Util.java:39`, `:145`) are accurate against current `master`.
- The purity/idempotence/append-only-code contracts (H1, H3, §7.12) are the right invariants for a pass whose output feeds a byte-equivalence test and a future grading harness.
- P10's claim about `HeadlessCoreRatchetTest.BASELINE` being `Set.of()` today is verified correct — no free pass is being smuggled in.
- The residual scope boundaries (out-of-scope: #447's model, #472's key, #385's hierarchical HDL, #466's report channel) are stated clearly and don't creep into this issue's own Method checklist.

## Verdict

**sound-with-concerns.** The engineering design (pure elaborator, six coded diagnostics, explicit-stack cycle detection, byte-equivalence gate) is well-reasoned and the invariants are the right ones to test. But the issue currently cites code that doesn't exist on `master` (already flagged by #493 and unresolved in the body), rests entirely on two large unlanded prerequisites whose real shapes aren't known yet, and leaves one acceptance criterion (cycle reporting) narrower than the property it's meant to establish. None of these are fatal to the design, but none should be waved through as "ready" without a re-derivation pass at current `master` and a longer-cycle fixture added to the plan.
