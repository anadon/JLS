# Issue #306: CAP-09: a reviewer who did not draw the circuit gets a proof, a replayable counterexample, or an honest UNKNOWN — never a false pass
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a ~72 KB capstone tracking issue (formal core: extractor → AIG → CNF →
solver → miter, plus a status lattice, coverage reporting, and two reference
corpora) sitting on top of eight required "FEAT" issues, none of which is
closed. The direction is sound and most of the code citations I checked
resolve exactly — but two of the issue's own load-bearing artifacts do not
hold up against the checked-out tree, and the kill-criteria design has an
unclosed gaming vector the issue itself only half-notices.

## Findings, most severe first

### 1. [High] AC-9 — the "single criterion that makes the whole capstone falsifiable" — is built on a mischaracterized artifact

The issue states: *"The shipped grading criterion is `examples/autograde/autograde.py:45`, `EXPECTED_FINALS = {`, and `:53`... A submission wrong on 254 of 256 inputs and right on one passes."* AC-9 then asks a closer to "Take the submission that passes today ... and observe it reported COUNTEREXAMPLE."

I read the script (`examples/autograde/autograde.py:1-34,110-125`). Its own docstring: *"This is an example ... demonstrates the supported grading pattern."* It hardcodes exactly one fixture (`fixture = repo_root / "test" / "fixtures" / "fork-4.6-shiftregister.jls"`, line ~119) and checks three fixed output pins against one fixed run. There is no concept of a variable "submission" anywhere in the file — it cannot accept or grade a different circuit at all; it is a self-check that a known-good reference fixture still produces its documented outputs. There is no "submission that passes today, wrong on 254 of 256 inputs" for a reviewer to point at — that artifact does not exist in this tree.

The general worry (a grader that checks one vector can be gamed) is legitimate and the line-number citations are accurate, but calling this file "the shipped grading criterion" and building AC-9's walkthrough around "the submission that passes today" overstates what is actually in the tree. AC-9 as written cannot literally be executed as described.

**Recommendation:** rewrite AC-9 to be honest about what exists today — there is no submission-grading pipeline to falsify yet, only a single-fixture example script — or construct and commit an actual adversarial "submission" fixture (a shift-register variant that is wrong everywhere except the one checked input) before claiming this criterion is testable against the shipped tree.

### 2. [High] The Cost section's numbers cite two documents that do not exist anywhere on `master`

AC-6 ("a verdict carries a budget") divides by "the measured `3.14 × 10⁶ events/s`" at `docs/machine-calibration.md:149`; KC-09-2's 15 mw kill threshold and the whole 66–103 mw required-feature sum are sourced to `docs/plan/REGISTRY.md` and `docs/plan/capstones/CAP-09-verify-a-design-you-did-not-write.md`. None of these three paths exist in this repository:

```
$ git show origin/master:docs/plan/REGISTRY.md
fatal: path 'docs/plan/REGISTRY.md' does not exist in 'origin/master'
$ git show origin/master:docs/machine-calibration.md
fatal: path 'docs/machine-calibration.md' does not exist in 'origin/master'
```
`find /home/user/JLS -iname "*BRIEF*"` and `-iname REGISTRY.md` also return nothing anywhere in the working tree. `docs/capability-roadmap/` has no `REGISTRY.md` or per-capstone plan files either — only `AMENDMENT.md`, `lf-0*`, `sweep-0*`, `keystone-*`, which are real and do resolve.

This is not a minor line-drift the way the branch-pin corrections elsewhere in the thread describe (comment `5171396300` and `5227515055` only flag `HdlExporter.java:460-477` as branch-only, and only note *shifted line numbers* for everything else). Two entire cited source-of-truth documents for this issue's cost model and its measured-constant budget divisor simply are not in the tree that closes this issue, on any commit — not shifted, absent. Both of the issue's own "evidence-pin" audit comments missed this even though auditing exactly this class of dangling citation was their stated purpose.

**Recommendation:** either commit `docs/plan/REGISTRY.md` and `docs/machine-calibration.md` (or their successors) before this issue is treated as having a real cost/budget basis, or strike the specific numbers (66–103 mw, 15 mw threshold, 3.14×10⁶ events/s) from AC-6/KC-09-2/Cost until they are re-derived against something present in the repository, and post a REPLAN documenting the gap the way the thread already did for the `HdlExporter` anchor.

### 3. [Medium] Kill-criteria denominators are owned by the same feature they're meant to gate

KC-09-3 (60% checkable-element floor) and KC-09-4 (20% UNKNOWN-rate ceiling) both key off corpora that FEAT-053 (#369) itself is responsible for building and manifesting (§1: *"Both are absent at `2d0ca9d`... there is no corpus of any kind in the tree."*). The issue names the gaming vector for KC-09-3 explicitly ("60% of element instances are checkable can be made true by populating the corpus with checkable types") and adds a type-coverage floor against it — but names no equivalent guard for KC-09-4: nothing stops the mutation catalogue's author (also #369, per §1: "the mutation catalogue is the same artifact as AC-7's planted-defect catalogue") from skewing defects toward loud, easily-caught classes to keep the UNKNOWN rate low. A single implementer controls both the yardstick and the thing being measured for two of five kill criteria.

**Recommendation:** either have the mutation catalogue reviewed/authored independently of whoever builds the formal checker, or add a minimum-severity/subtlety floor to the catalogue analogous to the type-coverage floor already applied to KC-09-3.

### 4. [Medium] A refusal map load-bearing for Open Question 4 and §3 does not exist on `master`, and the body text was never updated to reflect that

`src/jls/hdl/HdlExporter.java:460-477`, the `REJECTED` map (`Memory`, `SubCircuit`, `RegisterFile`, `FieldExtend`) that Open Question 4 uses to justify funding a ~1 wk flattening elaborator, and that §3 calls "the single largest determinant of KC-09-3's percentage," is absent from the current `HdlExporter.java` (grep for `REJECTED` returns nothing; the file exists at 104+ lines with `class HdlExporter` but no such map). The issue's own comment thread (`5171396300`) already found this and correctly labels it branch-only, present code, not landed. But the correction is a comment, not an edit: Open Question 4's "recommended default" and the Cost section's demo-slice bullet still speak of the four-way refusal in the present tense as a live constraint shaping the day-one scope decision. A reader of the issue body alone (rather than all seven comments) will scope Open Question 4 against code that is not there.

**Recommendation:** fold the comment-thread correction into the body text (or a superseding REPLAN section) rather than leaving it as a comment a body-only reader will miss.

### 5. [Low] "D5 deletes `riscv/`" does not resolve to a locatable decision in this repository

The Cost/AC-7 discussion treats "D5 deletes `riscv/`" as settled fact governing FEAT-038's residual status. `riscv/` is fully present in the checked-out tree (README.md, jlsbuild.py, riscv_ref.py, etc.), which is expected since the deletion is prospective — but grepping the repo for a "D5" decision that actually specifies an `riscv/`-directory deletion turns up nothing; the only in-repo "D5" that resolves is `docs/capability-roadmap/lf-01-parameterization.md:228`, *"D5. Replication: an `Array` element, not a for-generate"* — an unrelated decision in an unrelated document. A peer review of a sibling issue in this same repo (`issue-reviews/issue-0889.adversarial.md`) independently flagged the same "D5"/"D7" citation pattern as unresolvable elsewhere in the tracker, so this looks like a systemic labeling collision across the roadmap ecosystem rather than a one-off typo in #306.

**Recommendation:** cite the actual PR/issue number that deletes `riscv/`, or state plainly that the "D5" naming has collided across roadmap documents and needs disambiguation.

## What holds up

- Every code citation I could check against `HEAD` outside of §3/Open Question 4's refusal map resolved exactly, including small line-range details: `TruthTable.java:1432` (`matchingRow < 0`), `:1448-1449` (`outValue == 2` / `outValue = 0`), `Simulator.java:231-232` (`now > maxTime` / `now = maxTime`), `BatchSimulator.java:87,89` (`pause`/`stopping = true`), `docs/batch-interface.md:36-40`'s three-row exit-status table, `ElementRegistry.java`'s 35 `new ElementType(...)` entries, and `SigSim.java:71,74`'s string-concatenation loop. This issue's authors clearly ran the greps they claim to have run for the parts that are grounded.
- The required-vs-beneficial split (rule E, "what breaks in §1 if removed") is applied consistently and the minimality argument in §2 is genuinely load-bearing, not decorative.
- The self-correction machinery visible in the comment thread (REPLAN comments withdrawing 7 of 9 mermaid edges once checked against the *filed* feature blocks, the D13 ruling trimming Open Question 10 per explicit maintainer direction, the evidence-pin re-anchoring after a branch rename) is the process working as designed — it caught real drift, even though findings 2 and 4 above show it isn't complete.
- Scope discipline against sibling capstone #300 (Open Question 6, explicit non-duplication argument) is well-reasoned and avoids obvious scope creep into grading-at-scale territory that #300 already owns.

## Feasibility note

All eight required features (#317, #322, #335, #347, #353, #354, #359, #369) are open, as are five further out-of-set prerequisites (#316, #321, #325, #343, #358) the issue itself documents as blocking its payload feature. Zero of this capstone's dependency graph has landed. That is disclosed honestly in the issue rather than hidden, but it means the acceptance criteria and kill criteria above are being adversarially reviewed against a plan, not against any code path a reviewer can currently exercise.
