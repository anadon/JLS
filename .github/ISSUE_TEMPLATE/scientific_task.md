---
name: Scientific task
about: A rigorously framed unit of work — defect, improvement, or investigation — with a falsifiable hypothesis and evidence-backed completion criteria
labels: ["tier:task"]
---

<!--
  Template: scientific-task v6 (2026-08)

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
     sibling issue, §12 (Related Work) states which issue owns which fix.
  5. Cross-references cite the section NAME — "§ Threats to Validity" —
     optionally with its number. The name is canonical: numbers drift
     across template versions, names do not. Never a bare number.
     Subsections of § Interface & Data Contract may use the short form
     "§7.N (<short name>)" — e.g. "§7.4 (Internal interfaces — public)"
     — the parenthesized name is still required.
  6. Executors: before step one of §8 (Method), re-verify every
     observation in §2 (Observations) at your checkout. If one fails to
     reproduce, or a hypothesis is refuted
     mid-work, stop and comment on the issue with the refuting evidence.
     A refuted issue is a successful experiment, not a task to salvage.
     If the observations no longer fail because the work has already
     landed, the issue is superseded — close it with that note instead of
     re-doing it. Re-derive any drifted line numbers before trusting them;
     a stale citation is not evidence.
  7. Labels are not applied automatically for API-filed issues: set
     `bug` or `enhancement` explicitly, matching the corpus, plus the
     tier label `tier:task`.
  8. Tier model — task → feature → capstone (canonical edge rules in
     the feature template). This is the task tier: COMPOSITION and
     ORDERING edges go to tasks and features only, never to capstones
     (the sole exception: a capstone may hold a recorded orphaned-scope
     edge to this task per capstone rule G — that edge lives on the
     capstone's side). `related` is reference-only and may point at ANY
     tier. An issue's tier is defined by its machine block's `tier:`
     key; the tier:* label is a mirror for filtering — a missing or
     stale label is bookkeeping to fix, never an edge violation. A
     task is part_of at most ONE feature (composition, single-owner);
     the task's part_of_feature field is authoritative for ownership —
     a feature roster that disagrees must REPLAN, not win. The
     ORDERING GRAPH is blocked_by/blocks edges PLUS composition edges
     read child-before-parent (a parent cannot close before its
     children land); that combined graph must stay a DAG.
     Consequences: a task must never be blocked_by its own parent
     feature (deadlock), and `blocks` aimed at a feature gates that
     feature's integration/close-out only — never its children's
     start; block the children directly to gate them. The machine
     block in Status & Dependencies is the source of truth for the
     edges it can express.
  9. Amendment & comment protocol. This body may be edited, but only
     together with an `AMENDED:` comment stating what changed, why,
     and on what evidence — a silent edit is invisible to executors,
     who reconstruct state from the machine block plus the prefixed
     comments (`STATUS:` / `REFUTED:` / `HANDOFF:` / `SUPERSEDED:` /
     `AMENDED:` / `WAIVED:`). Post each such comment on THIS issue
     and, when part_of_feature is set, mirror the same comment on that
     feature. When an edit REMOVES or NARROWS any claim, observation,
     prediction, criterion, or scope item, the AMENDED comment must
     carry a "Dropped/Retired" ledger enumerating each removed item
     with its disposition — retired with reason, moved to issue #N, or
     restated where — so omissions are auditable by reading the
     comment, not only by diffing bodies. Bookkeeping is exempt from
     the AMENDED requirement: ticking a Method or Completion checkbox
     whose backing evidence is already recorded in a comment or PR,
     and re-pinning evidence_commit after re-deriving citations.
     Checkbox state remains a convenience rendering — the recorded
     evidence is the record. Write mechanics: cite a line range as ONE
     link — "[L100–L120](<permalink>#L100-L120)" — never two adjacent
     links joined by a dash (some write paths corrupt that form), and
     after any body edit re-fetch the issue and verify the rendered
     result before considering the edit done.
  10. Waivers. A completion criterion may be waived only via a
     `WAIVED:` comment naming the reason AND the successor issue that
     now tracks the dropped obligation (or stating explicitly why no
     successor is needed). A criterion skipped without a WAIVED
     comment leaves the issue unclosable.

  Decision/spike tasks ("evaluate X", "decide Y") use this template
  with "verdict recorded in <named doc/section>" as the completion
  contract; sections that presuppose a code defect are N/A (rule 2).

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
     at the top, not buried in §12 (Related Work) — an issue picked up cold
     must not miss a blocker (a fix hardening code another issue is about
     to delete is wasted work). The machine block is the source of truth
     for graph assembly (rule 8); annotate each blocked_by entry with the
     one-line reason it blocks, as a YAML comment. Evidence commit is the
     single SHA every §2 (Observations) file:line is pinned to — cite by
     permalink at this commit (rule 1); if HEAD has moved, re-derive
     citations before trusting them. Before executing, run the
     supersession check: confirm the work has not already shipped
     (rule 6); if it has, close as superseded rather than re-doing it. -->

```yaml
tier: task
evidence_commit:        # SHA all §2 citations are pinned to
part_of_feature: none   # owning feature number, or the literal `none` if free-standing
blocked_by: []          # ordering: tasks or features that must land first
                        #   (never capstones, never this task's own parent feature)
blocks: []              # ordering: tasks or features waiting on this one
                        #   (a feature here = gates its close-out, not its children)
related: []             # reference only — never blocking
```

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

## 7. Interface & Data Contract

<!-- The shape of the work before the work: everything this task touches
     at a boundary, stated precisely enough that a reviewer can check the
     eventual diff against it. This section MUST be filled in before any
     proposed diff — diffs belong in §8 (Method) or later, never ahead of
     the contract they implement. Rule 2 applies per subsection:
     inapplicable ones are "N/A — <one-line reason>", and claims about
     existing code carry file:line evidence (rule 1). -->

### 7.1 External interfaces modified

<!-- Surfaces visible outside the process that this work changes: the
     `.jls` file format (docs/file-format.md), CLI/batch (`-b`) flags and
     exit codes, exported HDL, printed or exported output, GUI surfaces
     third parties depend on, installer/container layout. For each: the
     surface, the change, and who sees it. -->

### 7.2 External interfaces consumed

<!-- Surfaces outside this codebase the work depends on: JDK/Swing/AWT
     APIs, file formats read, environment variables, fonts, OS services,
     CI facilities. Note version or platform assumptions. -->

### 7.3 Data consumed (structure)

<!-- Each input: where it comes from, its format/schema/encoding/units,
     and the authoritative definition of that structure (link it — e.g.
     docs/file-format.md), plus whether the source is trusted or must be
     treated as hostile (a user-supplied `.jls` file is hostile input). -->

### 7.4 Internal interfaces provided — public

<!-- Classes, methods, or packages this work adds or changes that OTHER
     parts of the codebase are meant to call: the signature, the expected
     caller(s), and the behavioral contract (pre/postconditions, error
     behavior). -->

### 7.5 Internal interfaces provided — private

<!-- Implementation-only helpers introduced: what they do and how their
     privacy is enforced (visibility modifier, package placement) so they
     do not silently become load-bearing API. -->

### 7.6 Data provided (structure)

<!-- Each output: format/schema/encoding/units and where that structure
     is authoritatively defined; for a new structure, define it here or
     name the doc that will. -->

### 7.7 Data durably tracked

<!-- State that outlives the process: files written, settings, on-disk
     formats. For each: where it lives, its structure and version, and
     the migration story for data written by older versions. -->

### 7.8 Data ephemerally used

<!-- Transient state: caches, undo stacks, simulation state, temp files.
     For each: its lifetime, what invalidates it, and what happens if it
     is lost mid-operation. -->

### 7.9 Concurrency model

<!-- For each interface and data item above: synchronous, asynchronous,
     parallel, or concurrent — and the mechanism (EDT vs. background
     thread, SwingWorker, locks, immutability, single-threaded batch
     mode). Name what guards each piece of shared mutable state;
     "synchronous, EDT-only" is a complete answer where true. -->

### 7.10 Data transformations

<!-- How the inputs of §7.3 (Data consumed) and the stored data of §7.7
     (Data durably tracked) become the outputs of §7.6 (Data provided):
     the pipeline stage by stage, with the representation at each stage
     boundary. Every transform must be FULLY DEFINED — no "then it is
     processed" hand-waving — and expressed as embedded LaTeX math
     using GitHub's math rendering (inline $`...`$ or display $$...$$
     blocks; syntax reference:
     https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/writing-mathematical-expressions).
     For each stage: name the transform, give its signature over the
     structures declared in §7.3 (Data consumed) and §7.6 (Data
     provided), and define the mapping itself, e.g.

       $$f_{\mathrm{route}} : \mathrm{Wire} \times \mathrm{Grid}
         \to \mathrm{Segment}^{*}, \qquad
         f_{\mathrm{route}}(w, g) = \ldots$$

     with side conditions and partiality explicit — wherever a
     transform is undefined, §7.11 (Failure modes & error handling)
     owns the behavior. Prose may accompany the math; it may not
     replace it. A reviewer must be able to locate each defined stage
     in the diff. -->

### 7.11 Failure modes & error handling

<!-- At each boundary above: what happens on malformed input, missing
     resources, I/O failure, or interrupted operation. Which errors are
     surfaced to the user, which are recovered, which abort — and what
     durable data is guaranteed not to be corrupted on the way down. -->

### 7.12 Compatibility, versioning & migration

<!-- What existing artifacts and callers keep working: old `.jls` files
     still load, save output stays byte-identical (or the version bump is
     documented), in-tree callers still compile, round-trips still hold.
     State each compatibility claim so a test can pin it. -->

## 8. Method / Experimental Design

<!-- Ordered checklist of the work, each step small enough to review.
     Every behavioral fix carries a regression test that fails at the
     pre-change commit and passes with the fix. Proposed diffs go here
     (or in later sections) — always after §7 (Interface & Data
     Contract), never before it. -->

- [ ] ...

## 9. Data Collection & Analysis

<!-- How results are recorded and judged: which tests assert what; what
     manual verification (with platform) is recorded in the PR. -->

## 10. Falsification Criteria

<!-- For each hypothesis: the specific post-fix observation that refutes
     it, and the next move if refuted. "If after the fix X still occurs,
     H1 is wrong — investigate Y instead." -->

## 11. Threats to Validity

<!-- What could make the results misleading: platform differences,
     headless-vs-GUI divergence, stale line numbers or counts, fixture
     bias, tests that shortcut the real code path. -->

## 12. Related Work

<!-- Tracking issue, sibling issues, audit findings, external references.
     Where scopes touch, state which issue owns which fix. -->

## 13. Conclusion & Future Work

<!-- Expected end state in one or two sentences; follow-ups explicitly
     out of scope here. -->

## Open Questions & Decisions Needed

<!-- Decisions this task cannot make for itself — cost, custody, policy,
     taste, or unverified external state. Separate what is answerable now
     from what genuinely needs a maintainer, so an executor knows what is
     safe to proceed on. For each: the question, the options with a
     recommended default, and whether it blocks filing, blocks execution,
     or can ride along. "N/A — fully specified" if nothing is open. -->

## 14. Completion Criteria (Definition of Done)

<!-- How anyone — author, reviewer, or agent — recognizes this task is
     finished. Every box is checkable by pointing at evidence: a test
     name, a CI run, a command's output pasted in the PR. Edit the
     pre-filled boxes to fit (rule 2 governs inapplicable ones), and add
     criteria specific to this task.

     Integrity rule: tests verify the work, they do not define it. If a
     criterion below turns out to be wrong or unsatisfiable, follow
     rule 2 — comment with evidence, do not work around it. -->

- [ ] Every post-fix prediction in §5 (Predictions) verified; command and output recorded in the PR
- [ ] Every check in §10 (Falsification Criteria) performed post-fix; outcome (not refuted / refuted → action taken) recorded in the PR
- [ ] Post-change code re-checked against §7 (Interface & Data Contract): interfaces provided/consumed, data structures, concurrency model, and compatibility claims hold as declared; any deviation recorded as an issue comment (rule 2), not silently absorbed
- [ ] New regression tests fail at the pre-change commit and pass at the fix commit
- [ ] Existing tests pass unmodified, except tests whose asserted behavior this issue intentionally changes — each named, with the §5 prediction that justifies the new expectation
- [ ] `mvn verify` green (tests + SpotBugs, warnings-as-errors)
- [ ] No new entries in `config/spotbugs-exclude.xml`, or each new entry is `Class`-scoped with a justification
- [ ] No changes outside the scope of §8 (Method); adjacent work discovered en route is filed as new issues
- [ ] Every `blocked_by` entry in Status & Dependencies has landed, or the dependency was waived per rule 10
- [ ] If `part_of_feature` is set: landing reported on that feature with a `STATUS:` comment, including any contract deviations the feature's plan must reconcile
- [ ] Every cited evidence document and permalink resolves on the default branch at close — no branch-path links, no deleted docs
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (rule 10)
- [ ] Not superseded: the §2 (Observations) failures still reproduced at pickup (rule 6); citations re-derived if HEAD had moved
- [ ] Every decision in Open Questions & Decisions Needed is resolved (or explicitly deferred), none left blocking
- [ ] ...
