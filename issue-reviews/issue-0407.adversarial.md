# Issue #407: TASK-0024: the calibration record stops describing experiments and states measurements, and its re-measurement procedure names a fixture that exists
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## Summary

The stated goal — turn a documentation section that "describes an experiment
and stops" into recorded measurements, and point a re-measurement procedure
at a real, tracked fixture — is reasonable editorial hygiene. But the task's
entire evidentiary and textual basis does not exist in this repository: the
pinned evidence commit is unreachable, and `docs/machine-calibration.md` —
the 1,124-line document the issue says it "fills and corrects rather than
creates," and which every quoted line number, every predicate (P1–P9), and
most of the Definition of Done are anchored to — is not present at HEAD or
anywhere in git history. On top of that foundational problem, the issue's
own dependency bookkeeping is already stale relative to a sibling issue
(#413) that exists in this same repository. This is not a task that is
merely hard; as filed, it cannot be executed against the checked-out tree at
all.

## Findings, most severe first

### 1. [Critical] The evidence commit `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is unreachable in this repository

```
$ git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7 ; echo $?
128   (fatal: Not a valid object name)
$ git log --all --oneline | grep '^2d0ca9d'
(no output)
```

Every "Observations" quote in the issue (O1–O9) is stamped `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and presented as verified fact — `git ls-tree`, `git show`, `python3 mc_check.py` output, exact line numbers. None of it is reproducible from this checkout because the commit does not exist here. **Recommendation:** re-derive every O1–O9 citation against a commit that actually resolves in this repository (`master` is at `c5cee1b`), or state explicitly that the evidence was gathered against a different repository/branch and requires re-verification before anyone acts on it — do not let an unreachable SHA stand in as ground truth.

### 2. [Critical] `docs/machine-calibration.md` — the document this entire task edits — does not exist anywhere in this repository's history

```
$ git log --all --oneline -- docs/machine-calibration.md
(no output)
$ ls docs/ | grep -i calib
(no output)
```

The Abstract states as settled fact: *"`docs/machine-calibration.md` already exists at `2d0ca9d` — 1,124 lines. The task is not to write it."* §1 repeats it: *"its own preamble says it authorizes nothing."* `docs/virtual-hardware-parity.md`, quoted verbatim at "`:493-495`" and cited throughout as the document whose sentence "is the job in one sentence," is equally absent (`git log --all --oneline -- docs/virtual-hardware-parity.md` returns nothing). Every one of §5's predictions (P1–P9), §7.10's formal discharge predicate, and roughly half the Definition of Done presuppose editing specific existing prose at specific line numbers in a document that is not in the tree. A reviewer independently found and confirmed the identical defect against the parent feature, #335 (`issue-reviews/issue-0335.adversarial.md` finding 2), so this is not a one-off misreading on my part — it reproduces across two separate reviews of two separate issues in the same planning cluster. **Recommendation:** locate the actual source of these quotes (different repo, different branch, an uncommitted draft) and relink the issue to it, or strike the "already exists, this task edits it" framing entirely and re-scope as "author `docs/machine-calibration.md` from scratch, using this task's §6/§7 shape as a specification" — a substantially larger, differently-resourced task than the one filed.

### 3. [High] The issue's own dependency graph is already stale: TASK-0025 exists as #413, contradicting the "not yet fileable" claim

The YAML block states: *"NOT YET FILEABLE AS AN EDGE: TASK-0025 (commit the fixture, re-home the goldens, delete `riscv/`) is a third genuine prerequisite... It is being filed concurrently and has no number here yet; a link pass adds it."* The mermaid graph draws `T25 -.-> T0024` labeled `"link pass adds this edge"`.

But TASK-0025 is already filed, as **#413**, opened the same day (2026-08-03) as this issue, with its own fully-specified `blocked_by: [377, 379]` and an explicit statement in its §1: *"docs/machine-calibration.md already exists as an evidence record — TASK-0024 fills and corrects it rather than creating it"* — i.e. #413 repeats and compounds this issue's finding-2 defect rather than catching it. The promised "link pass" that would add the `T25 → T0024` edge and update `blocked_by` never happened: #407's `blocked_by` is still `[377, 379]` only, and its own §8 first checklist item ("Confirm #377 and #379 have landed and TASK-0025's deletion has happened") treats TASK-0025 as a precondition that isn't even wired into the machine-readable dependency block. This is the exact drift pattern the sibling review of #335 flagged (finding 3: "the elaborate machine-readable apparatus... is designed to prevent [drift], and it has already happened within 48 hours of filing"). **Recommendation:** add `blocked_by: [377, 379, 413]` in the same edit that updates this comment, or explain why #413 is deliberately excluded from the ordering.

### 4. [High] The task is fully blocked and none of its three prerequisites has landed — it cannot start

`blocked_by: [377, 379]`, both confirmed open via `issue_read` and both are themselves large, unstarted measurement tasks (a sealed-hierarchy change to `LogicElement`'s `permits` clause is required for #379; a new fixture and instrumentation extension for #377). The "third genuine prerequisite" TASK-0025/#413 is also open and itself blocked by the same two issues. So at minimum three chained, unlanded issues stand between filing and any work being possible on §6.1, §6.2, §6.4, and §6.6 — over half of the seven "describes-an-experiment-and-stops" subsections this task exists to discharge. The remaining subsections (§6.3, §6.5, §6.7, §6.8–§6.10) are explicitly unresolved even in principle: Open Question 1 states plainly *"§6.3... §6.5... §6.7... §6.8... §6.9... §6.10 are each different in kind"* with no owner named for several, and is flagged **"Blocks execution."** A task whose completion criteria depend on decisions not yet made, filed as if ready to work, invites either indefinite stall or a rushed resolution of those open questions under schedule pressure. **Recommendation:** do not schedule or estimate this task until #377, #379, and #413 have landed and Open Questions 1, 2, and 4 are actually resolved (the issue's own Definition of Done already says as much — "Questions 1, 2 and 4 [resolved] before execution" — so the issue is internally aware it is not ready and should not be marked actionable).

### 5. [Medium] The acceptance mechanism (H3) explicitly admits it can be gamed, and the stated mitigation has no enforcement

§4 states outright: *"H3 is refuted by a §6 rewrite that satisfies the parser while still stating no measurement — in which case the assertion is decorative and must be strengthened or dropped with a `WAIVED:` comment."* This is a real, named risk that the task's own falsification section (§10) repeats: *"H3 refuted if the rewritten §6 satisfies the parser while stating no measurement... A decorative assertion is worse than none because it launders a gap as a gate."* The proposed `MachineCalibrationDocTest` (per the `mc_check.py` prototype in O1) checks only for the *presence* of a measured-value-shaped line or a line beginning `"Still open:"` — a regex-over-headings check, by the issue's own §7.11 admission ("regex-over-headings, faithful to the document's current convention"). Nothing stops `### 6.1` being rewritten to read `Still open: pending future work.` for all seven subsections, which passes P1 while discharging nothing. The only backstop offered is a `WAIVED:` comment convention with no automated check that one exists, and no reviewer gate named beyond "a maintainer's assent" mentioned for a different, unrelated edit (the ARCHITECTURE.md trigger). **Recommendation:** require the test to additionally assert a minimum structural shape for a "measured value" line — e.g. a unit, a date, and a workload token, not just "not the word Experiment" — before treating H3 as adequately guarded; the issue itself names this remedy but does not require it as a P1 precondition.

### 6. [Medium] P9 — the only end-to-end functional check — is an unaudited human narrative, not a test

§5 states: *"P9 is a recorded human walkthrough with the operator named and the clean-clone commands pasted. There is no automated substitute and the issue does not pretend there is."* §11 repeats: *"'Executable procedure' is judged by one walkthrough. One operator on one platform is weak evidence."* This is honest about the limitation, but it means the single criterion that actually verifies §7's re-measurement procedure works end-to-end (the issue's own stated purpose — "make its §7.1 name a tracked fixture instead of a path that... was never tracked") is satisfiable by a closer writing a plausible-sounding narrative under time pressure, exactly the failure mode the sibling #335 review flagged for its criterion 5 ("a closer under time pressure can write 'reproduced within band' without a genuinely naive execution ever having happened"). **Recommendation:** at minimum require the walkthrough commands to be pasted as a runnable transcript (not prose) attached to the closing PR, and consider scripting the non-interactive parts (fixture load, checksum, band comparison) even if the human judgment step (readability) stays manual.

### 7. [Low] Scope-creep surface: a documentation task carries a ~30-item Definition of Done and edits a normative architecture document's binding language

The task is scoped as prose-only ("N/A for product surfaces... changes no `.jls` format, no CLI flag" — §7.1) plus one new test class, yet the Method (§8) has 12 top-level steps and the Definition of Done has ~19 checkboxes, several requiring cross-issue coordination (`STATUS:` comment on #335, waiver comments naming successor issues, re-verifying O1–O10 "at the executor's checkout," a human walkthrough with platform recorded). One step — restating `ARCHITECTURE.md`'s recorded #221 revisit trigger "quantitatively" — is itself flagged as a normative change needing "a maintainer's assent" (Open Question 4), bundled into what is otherwise framed as a mechanical documentation-discharge task. Bundling a binding-architecture-decision edit with routine doc reconciliation raises the risk that the quantitative threshold (currently proposed ad hoc in a comment attributed to "`keystone-c`" — itself a document that does not exist in this tree, see finding 2) gets waved through as part of a large checklist rather than reviewed on its own merits. **Recommendation:** split the `ARCHITECTURE.md` #221 trigger restatement into its own reviewable change, gated on the constants actually landing from #377/#379, rather than folding it into TASK-0024's checklist.

## What holds up

- **`ElementRegistry` registers exactly 35 element types at HEAD**, confirmed independently (`grep -c "new ElementType(" src/jls/elem/ElementRegistry.java` → 35), matching O1/O2's claim and supporting the premise that the "33 registered types" statements scattered through `docs/capability-roadmap/` are stale — that correction (P3) is a real, checkable defect in the tree regardless of the missing-document problem above.
- **O6's `ProcessBuilder` scope claim is accurate against HEAD**: zero hits in `src/`, fifteen files under `test/` — the "carry its `src/` scope" correction is a genuine, verifiable fix.
- **O7's `$dff` wording is accurate against HEAD**: `src/jls/hdl/imp/NetlistImporter.java:40-47` states almost verbatim what O7 quotes — cells "accepted... but this increment does not yet realize... reported as import problems," matching the recommended replacement wording exactly.
- **`ARCHITECTURE.md:354` genuinely names `riscv/` in the #221 revisit trigger**, confirmed by direct grep — O5's premise (the trigger names a directory a sibling task deletes) is real and worth fixing regardless of how the rest of this issue is resourced.
- **The current `riscv/` and `test/fixtures/` file inventories match the issue's cited counts** (26 tracked files under `riscv/`, four tracked `.jls` files repo-wide) — these specific factual claims hold up against the live tree even though they're sourced to an unreachable commit, suggesting the underlying facts are probably right even where the citation mechanics are broken.
- **The explicit non-goals section (§13) is well-drawn**: taking the measurements, deleting `riscv/`, and the standing ratchet are correctly identified as other issues' work, keeping this task's boundary legible in principle (once it has something real to work from).
