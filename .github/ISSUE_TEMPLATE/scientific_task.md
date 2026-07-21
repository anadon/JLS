---
name: Scientific task
about: A rigorously framed unit of work — defect, improvement, or investigation — with a falsifiable hypothesis and evidence-backed completion criteria
---

<!--
  Template: scientific-task v2 (2026-07)

  RULES — for humans and LLM agents alike, filing or executing.

  1. Evidence, not memory. Every code claim carries file:line at a named
     commit, re-derived at that commit — quote the line you cite. Pin that
     commit once in Status & Dependencies and cite by commit-locked
     permalink, never a branch path (branch links rot the moment the
     branch is deleted). Source comments and audit summaries are hearsay
     until re-derived. Aggregate claims (counts, "all X", "no Y anywhere")
     carry the exact command that produced them and its output.
  2. No padding. A section that does not apply is "N/A — <one-line
     reason>", decided at filing time. A criterion discovered to be wrong
     during execution gets an issue comment with evidence — do not work
     around it and do not silently edit it.
  3. Predictions are observable: do X, observe Y. For defects and
     improvements, at least one prediction must fail at the named commit,
     and its failure must be OBSERVED before filing — run it and paste
     the command and wrong output into §2 (Observations). For
     investigations, state instead the decision criterion: the
     observation that discriminates between the candidate answers.
  4. Atomic scope. One hypothesis cluster per issue; where scopes touch a
     sibling issue, §11 (Related Work) states which issue owns which fix.
  5. Cross-references cite number and name — "§10 (Threats to Validity)" —
     never a bare section number.
  6. Executors: before step one of §7, re-verify every §2 observation at
     your checkout. If one fails to reproduce, or a hypothesis is refuted
     mid-work, stop and comment on the issue with the refuting evidence.
     A refuted issue is a successful experiment, not a task to salvage.
     If the observations no longer fail because the work has already
     landed, the issue is superseded — close it with that note instead of
     re-doing it. Re-derive any drifted line numbers before trusting them;
     a stale citation is not evidence.
  7. Labels are not applied automatically for API-filed issues: set
     `bug` or `enhancement` explicitly, matching the corpus.

  These comments do not render on GitHub — leave them in place for the
  next reader of the raw issue body.
-->

## Abstract

<!-- 2-4 sentences: what is wrong or missing, why it matters, and the
     one-line shape of the proposed remedy. -->

## Intended Audience & Impact

<!-- Who is this work for, and how is it meaningful to them? Name the
     concrete audience(s) JLS actually serves and, per audience, the
     change they experience — what they can do afterward that they can't
     today, or what stops going wrong for them. Impact claims are
     observations too (rule 1): where the harm or gap is measurable
     (a wrong simulation, a crash, a lost edit, a silent mis-load),
     point at it.

     The recurring audiences — pick and name the ones this task serves,
     do not list them all:
       - Students drawing and simulating circuits in the editor.
       - Instructors authoring, grading, or auto-checking work in batch
         (`-b`) mode.
       - Circuit-file authors and third-party tools that read or write
         `.jls` files against docs/file-format.md.
       - Contributors, maintainers, and LLM agents working the codebase.
       - Packagers and distributors shipping the installers and container.
     If a task genuinely serves an internal audience only (a refactor, a
     CI gate), say so and name the downstream audience it protects; "N/A"
     with no audience means the work has no beneficiary and does not
     belong on the backlog (rule 2). -->

## Status & Dependencies

<!-- The front matter an executor reads before touching anything. Keep it
     at the top, not buried in §11 (Related Work) — an issue picked up cold
     must not miss a blocker (a fix hardening code another issue is about
     to delete is wasted work).
       - Evidence commit — the single SHA every §2 (Observations) file:line
         is pinned to. Cite by permalink at this commit (rule 1); if HEAD
         has moved, re-derive the citations before trusting them.
       - Blocked by — issues that must land first, each with the one-line
         reason it blocks. "None" if free-standing.
       - Blocks — issues waiting on this one.
       - Supersession check — confirm this work has not already shipped
         (rule 6); if it has, close as superseded rather than re-doing it. -->

- Evidence commit:
- Blocked by:
- Blocks:

## 1. Background & Prior Work

<!-- What already exists: relevant code paths, prior issues/PRs, audit
     findings, external tools or literature. Link them. -->

## 2. Observations

<!-- Numbered, reproducible facts. Each carries file:line at a named
     commit plus the quoted line(s), or the exact command and its output.
     Include the observed failure required by rule 3. -->

## 3. Research Question

<!-- The single question this work answers, phrased so the answer is
     yes or no. -->

## 4. Hypothesis (falsifiable)

<!-- H1, H2, ...: statements about root cause or expected effect that the
     Method can prove wrong. If no observation could refute it, it is not
     a hypothesis — rewrite it. -->

## 5. Predictions

<!-- P1, P2, ...: concrete observable outcomes if the hypothesis holds,
     each phrased as: do X, observe Y. Mark which fail at the named
     commit (pre-fix) and which must hold after the fix. -->

## 6. Materials & Apparatus

<!-- Toolchain, fixtures, test rigs, corpora. Note anything that does not
     exist yet and must be built first. -->

## 7. Method / Experimental Design

<!-- Ordered checklist of the work, each step small enough to review.
     Every behavioral fix carries a regression test that fails at the
     pre-change commit and passes with the fix. -->

- [ ] ...

## 8. Data Collection & Analysis

<!-- How results are recorded and judged: which tests assert what; what
     manual verification (with platform) is recorded in the PR. -->

## 9. Falsification Criteria

<!-- For each hypothesis: the specific post-fix observation that refutes
     it, and the next move if refuted. "If after the fix X still occurs,
     H1 is wrong — investigate Y instead." -->

## 10. Threats to Validity

<!-- What could make the results misleading: platform differences,
     headless-vs-GUI divergence, stale line numbers or counts, fixture
     bias, tests that shortcut the real code path. -->

## 11. Related Work

<!-- Tracking issue, sibling issues, audit findings, external references.
     Where scopes touch, state which issue owns which fix. -->

## 12. Conclusion & Future Work

<!-- Expected end state in one or two sentences; follow-ups explicitly
     out of scope here. -->

## Open Questions & Decisions Needed

<!-- Decisions this task cannot make for itself — cost, custody, policy,
     taste, or unverified external state. Separate what is answerable now
     from what genuinely needs a maintainer, so an executor knows what is
     safe to proceed on. For each: the question, the options with a
     recommended default, and whether it blocks filing, blocks execution,
     or can ride along. "N/A — fully specified" if nothing is open. -->

## 13. Completion Criteria (Definition of Done)

<!-- How anyone — author, reviewer, or agent — recognizes this task is
     finished. Every box is checkable by pointing at evidence: a test
     name, a CI run, a command's output pasted in the PR. Edit the
     pre-filled boxes to fit (rule 2 governs inapplicable ones), and add
     criteria specific to this task.

     Integrity rule: tests verify the work, they do not define it. If a
     criterion below turns out to be wrong or unsatisfiable, follow
     rule 2 — comment with evidence, do not work around it. -->

- [ ] Every post-fix prediction in §5 (Predictions) verified; command and output recorded in the PR
- [ ] Every check in §9 (Falsification Criteria) performed post-fix; outcome (not refuted / refuted → action taken) recorded in the PR
- [ ] New regression tests fail at the pre-change commit and pass at the fix commit
- [ ] Existing tests pass unmodified, except tests whose asserted behavior this issue intentionally changes — each named, with the §5 prediction that justifies the new expectation
- [ ] `mvn verify` green (tests + SpotBugs, warnings-as-errors)
- [ ] No new entries in `config/spotbugs-exclude.xml`, or each new entry is `Class`-scoped with a justification
- [ ] No changes outside the scope of §7 (Method); adjacent work discovered en route is filed as new issues
- [ ] Every "Blocked by" in Status & Dependencies has landed, or the dependency was explicitly waived with a reason
- [ ] Not superseded: the §2 (Observations) failures still reproduced at pickup (rule 6); citations re-derived if HEAD had moved
- [ ] Every decision in Open Questions & Decisions Needed is resolved (or explicitly deferred), none left blocking
- [ ] ...
