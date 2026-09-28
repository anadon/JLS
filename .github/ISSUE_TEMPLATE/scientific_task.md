---
name: Scientific task
about: A rigorously framed unit of work — defect, improvement, or investigation — with a falsifiable hypothesis and evidence-backed completion criteria
labels: ["tier:task"]
---

<!--
  Template: scientific-task v8 (2026-09)

  RULES — for humans and LLM agents alike, filing or executing.

  1. Evidence, not memory. Every code claim carries file:line at a named
     commit, re-derived at that commit — quote the line you cite. Pin that
     commit once in § Status & Dependencies and cite by commit-locked
     permalink at this repository's URL (a fork's URL is never cited:
     the commit, reachable from the default branch, is cited here),
     never a branch path. Source comments and audit summaries
     are hearsay until re-derived. Aggregate claims (counts, "all X", "no Y anywhere")
     carry the exact command that produced them and its output.
  2. No padding. A section that does not apply is "N/A — <one-line
     reason>", decided at filing time. Evidence that applies but must not
     yet be disclosed (a security fix before release) is "WITHHELD — held
     by <custodian> until <event>" (an event that will never come:
     "until never — <reason>"); the custodian re-runs it where § Pickup
     Checks says and, at the event where one is named, un-redacts it by
     AMENDED (rule 9; a held posting's cell is filled when they post,
     bookkeeping — the Mirrors row). A criterion, prediction, hypothesis
     or expected
     value found wrong — or unfalsifiable — during execution is corrected
     by AMENDED with the evidence (rule 9), never worked around, closed
     `REFUTED:` or silently edited.
  3. Predictions are observable: do X, observe Y. For defects and
     improvements, at least one prediction must fail at the named commit,
     and its failure must be OBSERVED before filing — run it and paste
     the command and wrong output into § Observations (a failure that
     does not occur on every run: the loop as the command, with the run
     count and how many failed — every re-run of it, in § Pickup Checks
     and § Post-Implementation Validation, repeats the count and any
     redaction an AMENDED made to the paste; the count is the sample
     size, not the oracle — OS scores the assertion each run makes), or
     record
     it WITHHELD per rule 2 with its custodian named. For
     investigations, state instead the decision criterion: the
     observation that discriminates between the candidate answers.
  4. Atomic scope. One hypothesis cluster per issue; where scopes touch a
     sibling issue, § Related Work states which issue owns which fix and
     § Scope Boundary states what this issue will not absorb.
  5. Cross-references cite the section NAME — "§ Threats to Validity".
     Headings carry no numbers, so there is nothing else to cite; a
     trailing parenthetical may be dropped ("§ Hypothesis").
     Subsections of § Interface & Data Contract and § Intent & Alignment
     are cited the same way — "§ Data transformations", "§ User impact".
  6. Executors: before step one of § Method / Experimental Design, run
     § Pickup Checks in full. A hypothesis refuted mid-work stops the
     work where no other hypothesis in § Hypothesis (falsifiable) still
     stands; otherwise execution follows the fired F's next move, the
     refutation is recorded by `STATUS: progress` quoting the F, the
     contract subsections, steps and criteria only the fallen hypothesis
     motivated are retired by AMENDED (Dropped/Retired ledger) before
     close, and `REFUTED:` closes the issue only when the last standing
     hypothesis falls — or when the rule 3 failure is shown not to be
     one: the standing rule or format document § Intent & Alignment
     cites says, at its current revision, that the observed behaviour is
     correct (the premise failing, as the decision-task note has it; the
     close-out cites that section and names under rule 10 the task
     correcting whatever document misled the observer, or none). Comment
     with the refuting evidence. An F whose
     antecedent occurs while the evidence shows its hypothesis right —
     the fix is necessary and the residual failure has an independent
     cause — refutes nothing: the prediction was wrong (rule 2), and the
     AMENDED correcting it (ledger) re-anchors the P on the observation
     that isolates this cause, pasted in § Observations, and names the
     successor that owns the other cause — the sibling already
     `blocked_by` this task with the § Related Work P/F pair, or one
     filed now — in § Related Work and § Scope Boundary (the
     competing-hypothesis pattern of § Related Work, this task being the
     sibling past step one), or, where the cause is the substrate and no
     change to this repository removes it, the § Threats to Validity
     entry that accepts it with its reason. A refuted issue is a
     successful experiment, not a task to salvage — except in an
     investigation, where a refuted candidate answer is a result (see
     the decision-task note below). An observation that fails to
     reproduce is routed, never worked around: the change this task
     would make has already landed, by whatever issue or commit →
     `SUPERSEDED:` citing it (a regression test the failure still lacks is an
     obligation rule 10 routes); the rule 3 failure no longer occurs
     under the environment § Observations records and no change removing
     it landed (for an investigation: the question it raised is closed
     by the non-reproduction) → `REFUTED:`; otherwise →
     an AMENDED that re-derives or retires the observation
     (Dropped/Retired ledger) and
     re-anchors any hypothesis that named only it.
  7. Labels are not applied automatically for API-filed issues: set
     `bug` or `enhancement` explicitly, matching the corpus, plus the
     tier label `tier:task`. An investigation takes `enhancement` (it has
     no observed failure to label `bug`).
  8. Tier model — task → feature → capstone; the edge rules are
     canonical in the feature template's TIER MODEL block and the
     machine-block comments in § Status & Dependencies. THIS TASK
     DECLARES NO OWNER
     (TIER MODEL); listing features are found by roster search (§ Related
     Work; the listing-features pickup row).
  9. Amendment & comment protocol. This body may be edited, but only
     together with an `AMENDED:` comment — edit first, then post it
     (validator H06 reads the order); before editing, fold in every
     body-editing comment newer than your last read, since edits are
     last-write-wins — stating what changed, why, and on what evidence; a
     silent edit is invisible to executors, who reconstruct state from
     the machine block plus the prefixed
     comments (`STATUS:` / `REFUTED:` / `HANDOFF:` / `SUPERSEDED:` /
     `AMENDED:` / `WAIVED:`).
       Sub-tags and mirrors. `STATUS:` carries `pickup`, `progress` or
     `landed`; `HANDOFF:` carries `split`, `transfer` or `re-tier`. In
     an own comment, numbers follow the prefix or sub-tag after a dash,
     never directly after it (`HANDOFF: split — successors #124, #125`;
     `STATUS: landed — <fix or apparatus sha as it lands on the default
     branch: the squash or rebased commit, or the merge commit where the
     PR merged by one; an investigation: the pinned apparatus commit,
     the decision-task note>, PR #N, <permalink to the body revision holding
     the filled § Post-Implementation Validation, its Mirrors row filled
     afterwards
     (bookkeeping)>; contract deviations: <AMENDED
     links, or none>`; a fix-up merged before close posts a second
     `STATUS: landed` naming both PRs, carrying the fix-up's sha as the
     fix commit and a revision whose rows were re-run there — a posted
     prefixed comment is never edited except to redact a secret, the
     redaction marked in place; a later one supersedes it). Every
     prefixed comment except `STATUS: pickup` and `STATUS: progress` is
     posted on THIS
     issue and mirrored, led by this issue's number directly after the
     prefix or sub-tag (`STATUS: landed #N — …`; a landing mirror carries
     the first line), on every OPEN feature whose `requires_tasks`
     roster lists this task. A number directly after the prefix or
     sub-tag marks a mirror, a notice posted on another issue, or a
     counterpart's REPLAN or AMENDED editing this body (feature rule C;
     § Related Work).
       HANDOFF is the AMENDED of a split, transfer or re-tier and is
     read as one everywhere this template says AMENDED. A split HANDOFF
     carries the successor number(s) and the Dropped/Retired ledger
     moving each item to them; every listing feature is REPLANned to
     carry the successors — in place of this task where it closes (every
     item moved, rule 10) — with handoffs moved. A re-tier HANDOFF
     carries the new issue's number and the ledger moving every item to
     it, and closes this issue (rule 10); listing features answer per
     their § Re-planning Protocol. After either, or after a `REFUTED:`
     or `SUPERSEDED:` naming a successor for an obligation this task
     still owes (rule 10), a sibling whose `blocked_by` names this task
     for an item its ledger or close-out hands to a successor re-points
     it at that successor (after a re-tier, the issue at a legal tier
     that carries the item) by AMENDED (§ Related Work); where that
     successor
     is still a planned scope, the edge is retired and the planning
     feature's § Sequencing & Parallelism records the ordering for that
     child's filing (feature
     rule C). A transfer HANDOFF — posted by the predecessor or, where
     the predecessor has gone silent, by the successor from the branch
     and the comment stream; a reviewer whose rework lands by a PR of
     their own in place of the executor's is that successor — states
     that the body is unchanged and
     carries the branch and commit of the work, the PR, each Method step
     already ticked with the comment that evidences it at a commit the
     handoff commit reaches (a tick with no such comment is listed as
     cleared), and the first step the successor executes; the successor
     posts its own
     `STATUS: pickup` citing it, names the handoff commit as its checkout
     (or, where unreachable, the commit actually checked out,
     re-evidencing or clearing each tick whose evidence no longer
     resolves), and runs § Pickup Checks in full there — observation and
     supersession rows at the work's merge-base with the default branch,
     and `evidence_commit`, if re-pinned, re-pinned to that merge-base, never
     to the branch commit.
       Consequences of an AMENDED (re-read confirmation: rule 14).
     Posted after `STATUS: pickup` and before close,
     it also re-runs the § Pickup Checks rows its edit touches and
     records them, and where it changes § Interface & Data Contract or
     § Method / Experimental Design names each ticked Method step that
     stands and each whose tick is cleared. When it REMOVES or NARROWS
     any claim, observation, prediction, criterion, edge, or scope item,
     it carries a "Dropped/Retired" ledger enumerating each removed item
     with its disposition — retired with reason, moved to issue #N, or
     restated where.
       Re-plan requests and answers. An AMENDED that is a re-plan request
     (feature rule D: it contradicts a listing feature's § Global
     Invariants entry or a handoff that feature assigns to this task, or
     declares a surface that feature's contract does not) says so in its
     mirror and marks the Method steps consuming the contested item
     `BLOCKING: #F REPLAN` until that feature's REPLAN answers here
     (feature rule C); the marks are cleared on that REPLAN — bookkeeping
     where it adopted what this body says, otherwise by the AMENDED
     restoring the contract (ledger); where two listing features'
     REPLANs answer oppositely, the contract is restored and the
     adopting feature, its entry the newer, re-plans (the listing tier's
     § Re-planning Protocol). A `REPLAN:` a listing feature posts
     here, led by its number, that changes anything this body cites is
     answered by an AMENDED — on receipt once a pickup has happened,
     otherwise at pickup, unless the tier's own rules say on receipt; one
     that first lists this task is re-checked as if every invariant and
     handoff of that feature had changed, and answered by AMENDED where
     any of them requires an edit; a drop with disposition "freed"
     restates the alignment (a target closed `SUPERSEDED: — label` is
     restated as the standing rule the section the alignment cited
     states) or, where none can be cited, closes this issue under rule
     10 by the AMENDED answering it; "re-homed" moves an alignment that
     cited the dropping feature to the listing one (AMENDED); "closed"
     closes it under rule 10 on receipt, by whoever posted the REPLAN,
     with an `AMENDED:` here citing it, unless another OPEN roster lists
     this task, which makes the disposition re-homed and is answered by
     the re-homing AMENDED naming that roster; an issue already closed,
     or one where a `STATUS: landed` already stands (the landing is its
     disposition), receives any of these as a notice (feature rule C); a
     REPLAN
     that changes nothing cited here and answers no re-plan
     request needs no answer. A deviation of what was built from what
     this body records — a contract subsection, a prediction or sheet
     cell recorded as held, a criterion recorded as met, a close-out
     comment's cited landing that did not remove the rule 3 failure (a
     composite: did not make its capability or outcome observable), or
     the landed change reverted from the default branch — found after
     close (found before close, the executor's own AMENDED in this same
     form, the re-land then posting a second `STATUS: landed` in the
     fix-up form above, or the criteria the revert leaves unmet
     `WAIVED:` naming their successor, rule 10) is recorded by the
     finder's AMENDED on this closed issue: the cell or
     subsection corrected (a revert: the P/F rows, re-run at the
     reverting commit and citing it), the Dropped/Retired ledger, and
     the successor tracking any unmet obligation (after a revert, the
     re-land: a filed issue, or a listing feature whose `planned_tasks`
     now carries it — never "unfiled") or why none is needed — mirrored
     also on closed listing
     features, and on an open feature whose REPLAN dropped this task
     citing that close-out, as a notice.
       Bookkeeping — an edit that records evidence already in this
     issue's comment stream or in a counterpart's authoritative block,
     and changes no claim, edge, criterion or score — needs no AMENDED:
     ticking (or, per a transfer HANDOFF or its `STATUS: pickup`, or a
     `review_clean` set to `false`, clearing) a review, Completion, Pickup
     or Method box
     whose evidence is in the validation row it names, the
     `STATUS: pickup` comment, a comment here quoting the box, or the
     review comment at `review_evidence`; filling check-sheet cells from
     recorded evidence; re-pinning `evidence_commit` when every cited
     line re-derives
     unchanged there (renamed paths followed, each permalink re-issued
     at the new commit) and every observation reproduces (otherwise it
     is part of the AMENDED that fixes the observation); adding or
     removing a
     `blocks` entry that mirrors a counterpart's authoritative
     `blocked_by`, citing the counterpart; adding to § Internal
     interfaces consumed or § Data consumed (structure) the file:line of
     a `blocked_by` task's landed surface or structure where it matches
     that task's declaration (the interfaces pickup row); rewriting
     § Related Work's listing line to match the rosters, citing the
     REPLAN or feature rule C resolution comment that warrants it; and
     replacing an "unfiled" alignment with
     its citation when the cited section says what this task assumes (a
     plain comment says so); any other edit neither this rule nor the
     tier's own rules name bookkeeping is an AMENDED.
       Write mechanics: a line range is one link (`…#L100-L120`;
     validator H17 rejects two dash-joined links); re-fetch after every
     edit.
  10. Waivers. A completion criterion may be waived only via a
     `WAIVED:` comment naming the reason AND the successor that now
     tracks the dropped obligation — a filed issue, or the feature whose
     `planned_tasks` carries or whose § Re-planning Protocol supplies it
     (the Invariants row); a successor no mirror of this comment reaches
     receives it as a notice led by this number, a feature so named
     adopting it per its § Re-planning Protocol — or stating explicitly
     why no successor is needed; after close, the finder's AMENDED of
     rule 9 stands in for the WAIVED of any criterion it records as not
     met and corrects the Waivers row; an issue closed with no close-out
     record (a bare close) receives that AMENDED recording each
     criterion as not met, or as met where the sheet or the stream holds
     its evidence, and it is the close-out record a listing feature's
     REPLAN cites. A criterion skipped without a WAIVED
     comment leaves the issue unclosable. A close on `REFUTED:`,
     `SUPERSEDED:`, `HANDOFF: re-tier` or a `HANDOFF: split` whose ledger
     moves every item, or on a listing feature's REPLAN disposition
     "closed", or "freed" where no alignment can be cited (either by the
     AMENDED answering it, rule 9), or on the AMENDED recording that its
     last alignment target was withdrawn and no alignment replaces it,
     needs no WAIVED comments:
     that comment is the close-out record, the check-sheets are not
     completed (rows already filled stand, and the comment links the
     revision holding them), and any obligation still needed names its
     successor inside it.
  11. Oracle custody. Every completion criterion that compares against an
     expected value names WHO produced that value and WHEN. Values
     committed and reviewed BEFORE the implementation, and values written
     by the same change as the implementation or captured from its
     output afterwards, are different guarantees, and only the first is
     evidence — a value whose derivation cannot be shown, or shown
     pre-committed, is scored as the second. "The same change" is the
     merge unit
     — the PR — whatever its commit order. A value derived independently
     of the implementation (from a specification, a fixture, or a hand
     computation) whose derivation is recorded in § Data Collection &
     Analysis is not self-certified even when it lands in that PR; a
     value produced by running the implementation is. Where an
     expected-output artifact is generated by the same change that
     produces the behaviour it certifies, or captured from its output
     afterwards, say so in § Data Collection & Analysis and apply the
     corresponding deduction in § Agentic Delegability.
  12. Artifact paths resolve at filing. Every path a criterion names either
     exists at `evidence_commit`, or is created by a named step of this
     issue, or is created by a task named in `blocked_by` — and
     § Completion Criteria says which (a path a step removes exists at
     `evidence_commit` and the criterion names that step).
  13. Open Questions are MARKED, not merely listed. Each entry ends in
     exactly one of `Recommended default: <answer>` (an executor may
     proceed and land), `PROPOSED: <answer>, pending <who confirms>` (an
     executor may draft but not land), `BLOCKING: <who decides>` (an
     executor must stop), or `HYGIENE: <what to re-derive>` (not a decision
     at all — a citation, a status check, an un-run build). An unmarked
     entry is scored on its substance by § Agentic Delegability and the
     marker is fixed. An entry answered after filing keeps its marker
     and appends the answer with the comment that gave it (the Open
     question row's resolving comment), by the AMENDED that re-scores
     DA (rule 9).
  14. Filing order and consistency gates. Sections appear in DEPENDENCY
     order, not read order: each section is written from the sections
     above it and from nothing below it. Sections that define each other
     are grouped and written together; within a group the order is free.
     After each section or group stands a GATE checkbox. Tick a gate only
     after (a) re-reading every section above it against that section's
     own mandate, (b) re-reading them against each other, (c) re-reading
     them against the external context the gate names — usually another
     issue's body at its current revision, and code at evidence_commit —
     and then (d) an adversarial re-read of the same span, fixing each
     finding and re-reading until a pass yields no SUBSTANTIAL finding.
     Substantial has the meaning the review gate in § Agentic
     Delegability gives it. There is no bound on the number of passes.
     Gates are cumulative. Gate k covers every section through k, so
     while filing, a finding at gate k that changes an earlier section
     is fixed there and the earlier gate is not re-ticked: its tick
     records that the re-read happened when it was reached, not that
     the section was final — but the fix repeats clause (c) of the gate
     that follows the changed section, for the external context that
     gate names, and gate k's tick records that. A filed issue with unticked gates is valid but
     unvalidated, exactly as an unticked review gate leaves the band
     unvalidated. Execution ticks no gate.
     AFTER FILING, gate boxes are not touched (the two review boxes of
     § Agentic Delegability excepted): an AMENDED or split HANDOFF
     (rule 9; a transfer HANDOFF changes no section) names the
     sections it changed and confirms that every later section, and the
     external context the gates over that span name, were re-read
     against the change per (a)–(d) — an edit that takes a file, module
     or scope item a sibling or planning feature holds changes § Related
     Work here and, by an AMENDED posted on the sibling led by this
     number (ledger), its § Related Work, § Scope Boundary and Method
     (rule 4) — for a planning feature, by the REPLAN narrowing its
     planned scope, posted by whoever makes the edit (feature rule C).
     That comment, not checkbox state, is what § Pickup Checks reads.
     Gate clauses read a counterpart's body at
     its current revision and never depend on its later edit; a gate is
     never left pending on another issue.
  15. Three check-sheets: the gates of rule 14 at filing; § Pickup
     Checks once before step one (rule 6); § Post-Implementation
     Validation at close. Each sheet's comment block says what it
     records.

  Decision/spike tasks ("evaluate X", "decide Y") use this template
  with "verdict recorded in <named doc/section>" as the completion
  contract; sections that presuppose a code defect are N/A (rule 2).
  A refuted hypothesis is a verdict, not a stop, and so is an
  indeterminate one — the decision criterion cannot be observed with the
  data available; the AMENDED of rule 2 retires the P/F pairs it leaves
  unobservable and names the successor that obtains the data, or
  "unfiled": record it and land with `STATUS: landed`; `REFUTED:` is for
  the investigation's own premise failing — the question § Research
  Question asks is not one § Observations raises. The apparatus that
  produces the
  decision evidence lands at a named path, or is pinned by a PR that
  retains its commits, so the evidence can be re-run at that commit.
  The verdict is DA 0 (cap D); § Data Collection & Analysis names who
  records it.

  Refactor tasks (behaviour-preserving changes) use this template with
  the preservation claim as the hypothesis, naming the call-site
  observations it covers; the rule 3 failure is the structural check
  (compilation, an analysis gate, a signature or call-graph assertion)
  that fails at `evidence_commit`. § Agentic Delegability's OS axis
  scores the preservation check, not the structural one (OS anchor 4's
  analysis-gate clause does not reach it).

  These comments do not render on GitHub — leave them in place for the
  next reader of the raw issue body.
-->

## Intent & Alignment

<!-- Written FIRST. Everything below is checked against this section
     at every gate.

     Intent: the change in the world this work is for, in one
     paragraph, phrased so that a reader can later say whether it
     happened. Not the mechanism — the mechanism is § Hypothesis and
     § Method / Experimental Design.

     Alignment: what this work serves, cited by issue number AND
     section name — a feature's § Capability Statement & Scope
     Boundary, a capstone's § Outcome Statement — or the standing
     invariant, format document or project rule it upholds. A rule no
     document carries is stated here in one line ("user-visible text is
     spelled correctly"). When a parent
     WILL exist but is not yet filed, write "owning feature unfiled —
     <one-line scope>" and replace it with the citation once it exists
     (bookkeeping, rule 9); a task that will never have a parent
     cites its standing rule instead. A task that aligns with nothing has
     no beneficiary and does not belong on the backlog (rule 2). -->

### User impact

<!-- The concrete audience(s) this task serves and, per audience, the
     change they experience: what they can do afterward that they
     cannot today, or what stops going wrong for them. Pick and name
     the ones this task serves; do not list them all:
       - Students drawing and simulating circuits in the editor.
       - Instructors authoring, grading, or auto-checking work in batch
         (`-b`) mode.
       - Circuit-file authors and third-party tools that read or write
         `.jls` files against the save format's authoritative definition.
       - Packagers and distributors shipping the installers and container.
     If the task serves no user directly (a refactor, a CI gate), say so
     and name the downstream audience the work protects; the effects on
     the codebase itself belong in § Code & Project Impact and
     Consequences, below the contract that determines them. -->

## Status & Dependencies

<!-- The machine block is the source of truth for graph assembly
     (feature rule A). It is filled PROGRESSIVELY, the gates say when:
       - `evidence_commit` now, before anything is observed. It is the
         single SHA every § Observations file:line is pinned to — cite
         by permalink at this commit (rule 1).
       - `blocked_by` and `related` at the gate after § Related Work,
         once the siblings are known. Annotate each blocked_by entry
         with the one-line reason it blocks, as a YAML comment.
     A release-only defect names the release branch the fix targets as
     a YAML comment on `evidence_commit` (`# release: <branch>`; validator
     G18 reads it); wherever this template says "the default branch",
     that branch is read instead. -->

```yaml
tier: task
evidence_commit:        # SHA all § Observations citations are pinned to
# owned_by_derived: []  # OPTIONAL, tooling-generated, NON-AUTHORITATIVE copy of the
                        #   rosters listing this task; left absent at filing, never
                        #   hand-edited (G21 reports drift).
blocked_by: []          # ordering: tasks of this repository that must land first —
                        #   never a feature or capstone (G03); name the siblings.
blocks: []              # mirrors only — the counterpart's blocked_by is authoritative:
                        #   tasks, or features, whose blocked_by names this task (never
                        #   a capstone). Left [] at filing; G09 reports drift.
related: []             # reference only — never blocking, never ownership.
```

- [ ] **Gate — Intent & Status.** § Intent & Alignment states an intent a reader could later confirm or deny; its alignment target was opened at its current revision and the cited section says what this task assumes it says, or the target is marked unfiled with a one-line scope, or it is a standing rule stated here that a maintainer would uphold and from which the intent follows; every audience named is one this change reaches. `evidence_commit` is the SHA actually checked out and is reachable from the default branch, or from the release branch named beside it. Adversarial re-read of this span found no substantial finding.

## Observations

<!-- Numbered, reproducible facts. Each carries file:line at
     `evidence_commit` plus the quoted line(s), or the exact command —
     the locale, time zone, display or other environment its output
     depends on set inline or stated beside it — and its output. Include
     the observed failure required by rule 3. -->

## Background & Prior Work

<!-- What already exists: relevant code paths, prior issues/PRs, audit
     findings, external tools or literature. Link them. Code claims
     carry file:line at `evidence_commit` (rule 1). -->

## Related Work

<!-- Sibling issues, the features whose rosters list this task or
     whose `planned_tasks` carries this scope (search both; rule 8),
     every open issue whose criteria or walk-through name a path a
     Method step removes (rule 12: their Paths row breaks on the removal
     — record the ordering here where the issue is a task, or their
     criterion's re-derivation by an AMENDED — on a composite, a REPLAN
     — posted there led by this number, rule 14), audit findings,
     external references
     — with the search commands and their output. Where scopes touch,
     state which issue owns which fix. An
     OPEN sibling whose § Observations pins the SAME rule 3 failure is a
     competing hypothesis, not a touching scope (a closed one is prior
     work: the finder's AMENDED of rule 9 on it names this task as
     successor): add this hypothesis,
     its P/F pair and rejected candidates to the sibling by AMENDED and
     do not file; only where the sibling is past step one, file
     `blocked_by` it with a P/F pair predicting the failure persists
     after it lands (next move `SUPERSEDED:`), and its REFUTED, or the
     AMENDED narrowing it to the cause its fix removes (rule 6), names
     this task as successor (rule 10; the AMENDED, rule 6). The
     `blocked_by`
     and `related` entries of the machine block are derived from this
     section — fill them now, and record the DAG walk (feature template,
     TIER MODEL). Include every ordering a listing or
     planning feature's § Sequencing & Parallelism records against this
     scope: into `blocked_by` here, or, where an already-filed sibling
     must wait on this task, by an AMENDED on that sibling posted by
     whoever files this task. -->

- [ ] **Gate — Observations, Background & Related Work.** `git log --oneline <evidence_commit>..origin/<default, or the release branch named beside evidence_commit> -- <every path cited in § Observations and § Background & Prior Work>` is empty or its output is pasted with each observation touching a listed path re-run at that head; every observation reproduces at `evidence_commit` with command and output pasted, or is a quoted line at a commit-locked permalink, or is WITHHELD with custodian and event (rule 2); no OPEN sibling pins the same rule 3 failure (search command and output recorded), or this task is `blocked_by` it with the § Related Work P/F pair, or it is the predecessor whose split files this task, and a closed one pinning it receives the finder's AMENDED of rule 9 naming this task on filing; the rule 3 failure is among them, or the task is an investigation and needs none; every code claim in § Background & Prior Work carries a permalink at `evidence_commit`; § Intent & Alignment's measurable claims each have an observation and agree with it. § Related Work names every sibling whose scope touches this one and says which owns which fix; `blocked_by` and `related` are filled from it and `blocks` is empty or carries only mirrors, the DAG walk is recorded, and each named issue's body was read at its current revision — nothing here contradicts a sibling's stated scope or a listing or planning feature's § Capability Statement & Scope Boundary. Adversarial re-read of everything above found no substantial finding.

## Research Question

<!-- The single question this work answers, phrased so the answer is
     yes or no — or, for an investigation, so that it enumerates the
     candidate answers the hypotheses stand for. It is the question
     § Observations raises and § Intent & Alignment needs answered — not
     a wider one. -->

## Hypothesis (falsifiable)

<!-- H1, H2, ...: statements about root cause or expected effect that
     § Method / Experimental Design can prove wrong. If no observation
     could refute it, it is not a hypothesis — rewrite it. Each Hn names
     the observation(s) it explains. Candidate causes rejected before
     filing are listed here with the observation that rejected them, so
     an executor does not re-suspect them. -->

- [ ] **Gate — Question & Hypothesis.** The research question is answerable yes or no, or enumerates the candidate answers of an investigation, and is the question the observations raise; every hypothesis explains at least one observation and names it, or, for an investigation, names the candidate answer of § Research Question it stands for, or, for a refactor, names the call-site observations it preserves; every hypothesis is refutable by an observation-shaped check; each rejected candidate names its refuting observation; no hypothesis reaches beyond the question; together they cover the intent stated in § Intent & Alignment. Adversarial re-read of everything above found no substantial finding.

## Predictions & Falsification Criteria

<!-- One paired entry per prediction. A falsification criterion is a
     prediction's negation plus the next move; keeping them apart is how
     they drift apart, so they are written together:

       P1. do X, observe Y.   Fails at evidence_commit: yes/no (pasted in
           § Observations if yes; an investigation: "not run — apparatus
           built by step N").   Must hold after the fix (an investigation:
           at the apparatus commit): yes/no.   Tests: H1.
       F1. If after the fix X still yields not-Y, H1 is wrong — unless
           the evidence shows the fix necessary and the residual failure
           independently caused, a wrong P, not a fired F (rule 6) —
           next move: investigate Z / file successor / stop this line
           (the issue stops only when no hypothesis stands, rule 6).

     For investigations, the pairs are the decision criteria (rule 3).
     -->

## Materials & Apparatus

<!-- Toolchain, fixtures, test rigs, corpora, platforms: everything
     needed to perform each "do X" above. Note anything that does not
     exist yet and must be built first — § Method / Experimental Design
     must build it, or a task in `blocked_by` provides it (landed at
     pickup); a material only a named holder has, or one withheld until
     an event (rule 2), is listed with its holder. -->

- [ ] **Gate — Predictions & Materials.** Every hypothesis has at least one P and one F; every P is do-X-observe-Y with X executable using only the materials named; every P marked failing at `evidence_commit` has its failure pasted in § Observations; every F names the hypothesis it refutes and the next move; materials that do not exist yet are flagged for § Method / Experimental Design to build or named as provided by a `blocked_by` task. Adversarial re-read of everything above found no substantial finding.

## Interface & Data Contract

<!-- The shape of the work before the work: everything this task touches
     at a boundary, stated precisely enough that a reviewer can check the
     eventual diff against it. This section MUST be complete before any
     proposed diff — diffs belong in § Method / Experimental Design or
     later, never ahead of the contract they implement. Rule 2 applies per
     subsection: inapplicable ones are "N/A — <one-line reason>", and
     claims about existing code carry file:line evidence (rule 1).
     Subsections are in dependency order with gates between the groups:
     declarations first, then the properties defined over them, then the
     behaviour at their edges. -->

### External interfaces consumed

<!-- Surfaces outside this codebase the work depends on: JDK/Swing/AWT
     APIs, file formats read, environment variables, fonts, OS services,
     CI facilities. Note version or platform assumptions. -->

### Internal interfaces consumed

<!-- Classes, methods, or packages elsewhere in this codebase that the
     work calls or depends on, especially ones a sibling task provides:
     the surface, the contract relied on, and the file:line at
     `evidence_commit` where it lives today (or the sibling's § Internal
     interfaces provided — public, if it does not exist yet). -->

### Data consumed (structure)

<!-- Each input: where it comes from, its format/schema/encoding/units,
     and the authoritative definition of that structure — link it: a
     format document where one exists, otherwise the code that writes
     it, by permalink at `evidence_commit` (for `.jls`, the save
     routine and `FORMAT_VERSION` in `Circuit`), or, where a `blocked_by`
     task will write it, that task's § Data provided (structure), or,
     where a sibling ordered after this task will write it, the listing
     feature's § Feature-Level Interface & Data Contract that defines it
     — plus
     whether the source is trusted or must be treated as hostile (a
     user-supplied `.jls` file is hostile input). -->

### External interfaces modified

<!-- Surfaces visible outside the process that this work changes: the
     `.jls` file format, CLI/batch (`-b`) flags and
     exit codes, exported HDL, printed or exported output, GUI surfaces
     third parties depend on, installer/container layout. For each: the
     surface, the change, and who sees it. -->

### Internal interfaces provided — public

<!-- Classes, methods, or packages this work adds or changes that OTHER
     parts of the codebase are meant to call: the signature, the expected
     caller(s), and the behavioral contract (pre/postconditions, error
     behavior). -->

### Internal interfaces provided — private

<!-- Implementation-only helpers introduced: what they do and how their
     privacy is enforced (visibility modifier, package placement) so they
     do not silently become load-bearing API. -->

### Data provided (structure)

<!-- Each output: format/schema/encoding/units and where that structure
     is authoritatively defined; for a new structure, define it here or
     name the doc that will. -->

### Data durably tracked

<!-- State that outlives the process: files written, settings, on-disk
     formats. For each: where it lives, its structure and version, and
     the migration story for data written by older versions. -->

### Data ephemerally used

<!-- Transient state: caches, undo stacks, simulation state, temp files.
     For each: its lifetime, what invalidates it, and what happens if it
     is lost mid-operation. -->

- [ ] **Gate — Contract declarations.** Every interface or datum consumed names its provider and every one provided names its consumer, or says none exists yet; every structure links its authoritative definition or defines it here; every claim about existing code carries file:line at `evidence_commit`; hostile inputs are marked; every surface a prediction exercises appears in some subsection above, and no provided or modified declaration lacks a hypothesis or prediction that motivates it; every artifact or handoff that a listing or planning feature's § Feature-Level Interface & Data Contract or § Integration Criteria & Evidence Plan assigns to this task appears above, and no sibling interface consumed or provided above is one that contract does not assign to this task unless a REPLAN on that feature assigns it (feature rule C) and is cited here; nothing above is a diff. Adversarial re-read of everything above found no substantial finding.

### Concurrency model

<!-- For each interface and data item declared above: synchronous,
     asynchronous, parallel, or concurrent — and the mechanism (EDT vs.
     background thread, SwingWorker, locks, immutability, single-threaded
     batch mode). Name what guards each piece of shared mutable state;
     "synchronous, EDT-only" is a complete answer where true. -->

### Data transformations

<!-- How the inputs of § Data consumed (structure) and the stored data of
     § Data durably tracked become the outputs of § Data provided
     (structure): the pipeline stage by stage, with the representation at
     each stage boundary. Every transform must be FULLY DEFINED — no
     "then it is processed" hand-waving — and expressed as embedded
     LaTeX math using GitHub's math rendering (inline $`...`$ or display
     $$...$$ blocks; syntax reference:
     https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/writing-mathematical-expressions).
     For each stage: name the transform, give its signature over the
     structures declared above, and define the mapping itself, e.g.

       $$f_{\mathrm{route}} : \mathrm{Wire} \times \mathrm{Grid}
         \to \mathrm{Segment}^{*}, \qquad
         f_{\mathrm{route}}(w, g) = \ldots$$

     with side conditions and partiality explicit — wherever a
     transform is undefined, § Failure modes & error handling owns the
     behavior. A reviewer must be able to locate each defined stage in
     the diff. -->

- [ ] **Gate — Concurrency & transformations.** Every piece of shared mutable state declared above has a named guard, or the model is stated single-threaded where that is true; every transform's signature ranges over structures declared above and nothing undeclared; every transform is fully defined in math with its domain stated; every point where a transform is partial is listed for § Failure modes & error handling. Adversarial re-read of everything above found no substantial finding.

### Failure modes & error handling

<!-- At each boundary above, and at each partial point of § Data
     transformations: what happens on malformed input, missing resources,
     I/O failure, or interrupted operation. Which errors are surfaced to
     the user, which are recovered, which abort — and what durable data
     is guaranteed not to be corrupted on the way down. -->

### Compatibility, versioning & migration

<!-- What existing artifacts and callers keep working: old `.jls` files
     still load, save output stays byte-identical (or the version bump is
     documented), in-tree callers still compile, round-trips still hold.
     State each compatibility claim so a test can pin it. Every surface
     in § External interfaces modified, every datum in § Data durably
     tracked, and every surface in § Internal interfaces provided —
     public that already has callers at `evidence_commit` has a claim
     here. For a durable format or command surface,
     also state what a consumer at the PREVIOUS version does with the
     new output — refuses with a named message, ignores it, mis-loads —
     pinned by a test or by a recorded procedure naming the previous
     build's commit. -->

- [ ] **Gate — Contract complete.** Every partial point flagged at the previous gate has an owner in § Failure modes & error handling; every durable datum names what is guaranteed uncorrupted on failure; every modified external surface, durable datum, and already-called public internal surface has a compatibility claim stated so a test can pin it, and every durable format or command surface states the previous version's behaviour on the new output; every compatibility claim that matters has a prediction in § Predictions & Falsification Criteria, or one was added; the contract as a whole is what § Hypothesis (falsifiable) implies — no more, no less; nothing in § Compatibility, versioning & migration contradicts the § Global Invariants of any OPEN feature whose `requires_tasks` lists this task or whose `planned_tasks` carries this scope (two listing features' invariants that contradict each other become a `BLOCKING:` entry of § Open Questions & Decisions Needed when reached naming the feature whose invariant is newer, which re-plans per its § Re-planning Protocol). Adversarial re-read of everything above found no substantial finding.

## Scope Boundary

<!-- What is OUT: the adjacent work an executor might be tempted to
     absorb, each item with the issue that owns it instead, or
     "unfiled — file at close". Derived from § Related Work and the
     contract: anything the contract does not declare and Method does
     not step through is out, and this section says so where a reader
     would otherwise assume it is in. -->

## Method / Experimental Design

<!-- Ordered checklist of the work, each step small enough to review,
     naming the files it touches, and traceable to a contract subsection
     or a prediction. Every
     behavioral fix carries a regression test — or, for a build or CI
     setting, the job whose run at each commit is the check — that fails
     at the pre-change commit and passes with the fix — or, where no
     automated
     check can observe it, § Data Collection & Analysis names the
     recorded manual procedure, with platform, that stands in for it and
     § Threats to Validity carries the resulting threat with its
     acceptance reason (the MANUAL-PROCEDURE ALTERNATIVE). -->

- [ ] ...

## Code & Project Impact and Consequences

<!-- What changes for contributors, maintainers, LLM agents working the
     codebase, and the project's own commitments, now that the contract
     and the method fix what moves: interfaces that move (from
     § Interface & Data Contract), invariants that tighten or loosen,
     build or CI behaviour, published surfaces, maintenance cost taken
     on or retired. State consequences as well as benefits — what
     becomes harder, what a later change must now respect, what this
     forecloses. A consequence too costly to accept is a reason to
     narrow § Scope Boundary or § Method / Experimental Design, which is
     why the three are gated together. Each claim about existing code is
     an observation (rule 1): one line here, citation in
     § Observations. -->

- [ ] **Gate — Scope, Method & Consequences.** Every step is reviewable on its own, names the files it touches (each within the scope § Related Work assigns to this task, a file a sibling's scope covers being restated there), and names the contract subsection or prediction it serves; every behavioral change has its regression-test step or invokes the manual-procedure alternative of § Method / Experimental Design; every not-yet-existing material has its build step or is named as provided by a `blocked_by` task (rule 12); no step crosses § Scope Boundary and no out-of-scope item lacks an owning issue or an explicit "unfiled"; every contract subsection that declares a change is implemented by some step; every interface or invariant the contract moves has its consequence stated, every cost identified is stated or the section says there is none and why, and no consequence was accepted that § Intent & Alignment would not justify. Adversarial re-read of everything above found no substantial finding.

## Data Collection & Analysis

<!-- How results are recorded and judged: which tests, or which exact
     commands with pasted output, assert which prediction; what manual
     verification (with platform) is recorded in
     the PR. For every expected value compared against, who produced it
     and when (rule 11). -->

## Threats to Validity

<!-- What could make the results misleading: platform differences,
     headless-vs-GUI divergence, stale line numbers or counts, fixture
     bias, tests that shortcut the real code path. Each threat names its
     mitigation — a step in § Method / Experimental Design, a check in
     § Data Collection & Analysis — or is explicitly accepted with the
     reason. -->

- [ ] **Gate — Evidence & threats.** Every P and F has a named test, the exact command whose pasted output is its observation, or a recorded manual procedure with platform in § Data Collection & Analysis; every expected value names its custodian and date; every threat has a mitigation located in a named section or is accepted with a reason; every Method step invoking the manual-procedure alternative names its recorded procedure with platform in § Data Collection & Analysis and its accepted threat in § Threats to Validity, and that platform or apparatus appears in § Materials & Apparatus; no test named shortcuts the code path its prediction is about. Adversarial re-read of everything above found no substantial finding.

## Open Questions & Decisions Needed

<!-- Decisions this task cannot make for itself — cost, custody, policy,
     taste, or unverified external state. For each: the question, the
     options with a recommended default, and whether it blocks filing,
     blocks execution, or can ride along. "N/A — fully specified" if
     nothing is open.

     Mark every entry per rule 13 — `Recommended default:`, `PROPOSED:`,
     `BLOCKING:` or `HYGIENE:`. -->

## Conclusion & Future Work

<!-- Expected end state in one or two sentences, stated so that
     § Intent & Alignment is visibly satisfied by it; follow-ups
     explicitly out of scope here, each consistent with § Scope Boundary
     and carrying its owning issue or "unfiled". -->

- [ ] **Gate — Decisions & conclusion.** Every open question carries exactly one rule 13 marker; every design decision an executor will hit is listed or is settled by the text above; nothing marked `Recommended default:` contradicts the contract; every future-work item is outside § Scope Boundary and has an owner or "unfiled"; the end state satisfies the intent stated in § Intent & Alignment. Adversarial re-read of everything above found no substantial finding.

## Completion Criteria (Definition of Done)

<!-- WHAT must be true of the finished work — not how it is checked
     (that is § Post-Implementation Validation) and not what is checked
     before starting (that is § Pickup Checks). Every box names the
     artifact, its location, and the assertion that pins it; that is the
     SC=5 test in § Agentic Delegability. Edit the pre-filled boxes to
     fit (rule 2 governs inapplicable ones; a criterion quantified over
     listing features is never N/A at filing — its row records the
     close-time roster search), and add criteria specific to
     this task; each pre-filled box names in brackets the row(s) of
     § Post-Implementation Validation that verify it, and each added
     criterion gets a row of its own. -->

- [ ] Every post-fix prediction in § Predictions & Falsification Criteria holds at the fix commit (an investigation: at the pinned apparatus commit), or its falsification criterion fired and its next move was taken (rule 6) — none fired unaddressed [rows: P/F]
- [ ] The post-change code satisfies § Interface & Data Contract in every subsection — interfaces provided/consumed, structures, concurrency model, failure behaviour, compatibility claims — with any deviation recorded by an AMENDED of the deviating subsection before close (rule 9), never by a bare comment or silently absorbed [row: Contract]
- [ ] Every behavioral change has a regression test that fails at the pre-change commit and passes at the fix commit, or the manual-procedure alternative of § Method / Experimental Design was recorded with platform; a test added with no behavioural change fails at a commit, or under a fault, that its row names, where the behaviour it pins is broken [row: Regression tests]
- [ ] Existing tests pass unmodified, except tests whose asserted behavior this issue intentionally changes — each named, with the prediction that justifies the new expectation [row: Existing tests]
- [ ] § Global Invariants of every OPEN feature whose `requires_tasks` lists this task hold at the fix commit as merged into the default branch (the merge result, not the branch tip) [row: Invariants]
- [ ] `mvn verify` green at the fix commit as merged into the default branch (tests + SpotBugs, warnings-as-errors), and no new entry in `config/spotbugs-exclude.xml`, or each new entry `Class`-scoped with a justification [row: Standing gates]
- [ ] No changes outside § Method / Experimental Design or into what § Scope Boundary lists as out; adjacent work discovered en route is filed as new issues [row: Scope]
- [ ] Every expected value this issue was graded against was pre-committed or independently derived, or § Data Collection & Analysis records that it was produced by the implementation, or that its derivation cannot be shown, and the ADR-1 OS deduction was applied (rule 11) [row: Oracle custody]
- [ ] Every decision in § Open Questions & Decisions Needed is resolved or explicitly deferred, none left blocking [rows: Open question]
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (rule 10) [row: Waivers]
- [ ] Every cited evidence document resolves on the default branch at close and every permalink is commit-locked and resolves — no branch-path links, no deleted docs [row: Links]
- [ ] Every path named above exists at `evidence_commit`, or is created by a named step of § Method / Experimental Design, or by a task in `blocked_by` — this list says which (rule 12) [row: Paths]
- [ ] Landing reported with a `STATUS: landed` comment on every OPEN feature whose `requires_tasks` lists this task, citing the AMENDED of any contract deviation those plans must reconcile — or that posting handed to a holder, the Mirrors row [row: Mirrors]
- [ ] § Agentic Delegability re-scored on any `AMENDED:` edit that changed scope, evidence, or open decisions [row: Amendments]
- [ ] ... [row: <added below>]

## Pickup Checks

<!-- Run once per executor, before step one of § Method /
     Experimental Design (rule 6). The rows are fixed and never
     deleted: a row whose referent is N/A or empty is recorded "none" in
     the pickup comment (one line may list every "none" row) and neither
     passes nor fails. Record the outcome in a
     `STATUS: pickup` comment on this issue (not mirrored, rule 9) that
     names the checkout commit and lists the rows run and the rows not
     reached. A failed row ends in the `STATUS: pickup` naming the steps
     blocked (or all of them) and what unblocks them, and only those
     steps
     wait. -->

- [ ] `action` read and followed — DELEGATE and DELEGATE-WITH-CHECKPOINT: proceed, the named checkpoint precedes the merge; SPECIFY-FIRST, SPLIT and AGENT-ASSIST-ONLY: stop after `STATUS: pickup`, which names what is owed (spec, oracle, decomposition or split) and who supplies it — an executor who may supply it does so by AMENDED and proceeds — an ED-driven SPECIFY-FIRST instead hands the evidence steps to a holder as the materials row does; HUMAN-LED and HUMAN-ONLY: steps needing a person's decision, action or evidence are named blocked with who decides or holds, every other step proceeds
- [ ] Every `AMENDED:` or `HANDOFF:` read (own, or a counterpart's posted here led by its number); each body-editing one names the sections changed and re-read, and the body matches their fold in stream order, rule 9 bookkeeping edits aside — a mismatch is repaired by re-applying the fold from the comments and the body's edit history before proceeding; a re-plan request no listing feature has answered leaves its `BLOCKING: #F REPLAN` steps blocked, named in the `STATUS: pickup`; where `review_clean` is `false`, the finding at `review_evidence` is answered before step one — by the AMENDED fixing it, or by a comment quoting it and recording why it does not stand
- [ ] Every `REPLAN:` a listing feature posted here read; § Interface & Data Contract re-checked against each changed — or, for a feature that first listed this task, each — invariant, handoff or drop disposition, and answered by AMENDED where anything cited changed or, for a first-listing REPLAN, where any of them requires an edit (rule 9)
- [ ] Citations re-derived at the checkout if HEAD has moved past `evidence_commit` or `evidence_commit` is not reachable from the default branch (renamed paths followed; a deleted path retires or re-derives its citation by AMENDED, ledger entry included); `evidence_commit` re-pinned (rule 9)
- [ ] Not superseded: the rule 3 failure still occurs (where its apparatus is handed to a holder or WITHHELD, the observations row's "not run — held by" stands for this clause until their re-run), or, for an investigation, the question is still open; if the change this task would make has already landed, close with a `SUPERSEDED:` comment citing the landing (rule 6); and no OPEN task filed since this issue was created pins the same rule 3 failure or names a file § Method / Experimental Design touches (search command and output in the `STATUS: pickup`; a successor this issue's own `HANDOFF: split` names, one named alongside this issue by the HANDOFF that created it, or a task in `blocked_by` here, excepted) — a later-filed competitor not yet past step one folds its hypothesis here by AMENDED and closes `SUPERSEDED:` (a planning feature's resolution to it is corrected by REPLAN, feature rule C), or is `blocked_by` this task with the § Related Work P/F pair; one already past step one becomes, by AMENDED here, the sibling § Related Work says this task is `blocked_by`, with its P/F pair
- [ ] Every observation in § Observations re-verified at the checkout in the environment it records; command and output recorded — a non-reproducing observation is routed as rule 6 says, never worked around; an observation whose apparatus is a material the materials row hands to a holder, or that is WITHHELD under rule 2, is recorded "not run — held by <holder or custodian>", re-run by them before the evidence steps — command and output (a WITHHELD observation: the result, its output withheld in rule 2's form) in a `STATUS: progress` citing the pickup, routed as rule 6 says where it does not reproduce — and neither passes nor fails
- [ ] Every `blocked_by` entry has landed (a `SUPERSEDED:` close counts where it cites the landed work this edge waited for; the interfaces row reads that citation), or closed `REFUTED:` naming this task as its successor (rule 10) — one closed by a `HANDOFF:`, or naming another successor for the item this edge waits on, is re-pointed or retired by AMENDED as rule 9 says — or the edge was removed by an `AMENDED:` comment with a Dropped/Retired ledger entry
- [ ] Every `BLOCKING:` entry in § Open Questions & Decisions Needed is answered; `PROPOSED:` entries noted as draft-only
- [ ] Open features whose `requires_tasks` lists this task located (roster search) — these receive the mirrored comments of rule 9, and their § Global Invariants bind this work; a feature whose `planned_tasks` still carries this scope is resolved to this number by whoever reaches this row first (feature rule C) before it is owed anything; a closed or withdrawn alignment target is re-aligned by AMENDED or, where nothing replaces it, closed under rule 10 by it
- [ ] Every ordering a listing feature's § Sequencing & Parallelism records against this task is in `blocked_by` here or, where the sibling waits on this task, in that sibling's `blocked_by` (mirrored in `blocks`), or its absence is explained by an AMENDED
- [ ] For every `blocked_by` task providing an interface or structure § Internal interfaces consumed or § Data consumed (structure) relies on: its `STATUS: landed` comment (or the `SUPERSEDED:` citation the `blocked_by` row accepted) read and the provided surface or structure re-derived from code at its fix commit and still present at the checkout, the file:line added to that subsection — bookkeeping when it matches that task's declaration, otherwise (changed, or removed by a later landing — a revert of it, the finder's AMENDED of rule 9 records on the predecessor) an AMENDED here before step one
- [ ] Every material in § Materials & Apparatus, and write access to each issue this pass posts on (the mirrors of rule 9, a sibling's AMENDED under rule 14, a competitor's close under the Not superseded row), available, or scheduled by its Method step, or provided by a `blocked_by` task that has landed (the `blocked_by` row), or needed only by named steps or postings — then the `STATUS: pickup` comment names those steps or postings as blocked and the holder they are handed to, or that no holder is yet known (those steps stay blocked; a row a held posting leaves unfillable is WITHHELD — held by that holder until they post, rule 2), and every other step may proceed
- [ ] Every path a completion criterion names exists at the checkout, or the Method step that creates it is scheduled, or the `blocked_by` task that creates it has landed and the path is still present at the checkout (rule 12)

## Post-Implementation Validation

<!-- Run at close against the actual diff and PR, every row at the fix
     commit as it lands on the default branch (the sha `STATUS: landed`
     carries — never a branch commit a rebase or squash replaced; an
     investigation: the pinned apparatus commit, as the P/F row says; a
     step that runs only after deployment is observed at the deployed
     commit reaching that sha, named in its cell). One row per item;
     rows are enumerated AT FILING (a row per P/F pair, per threat, per
     open question, per added completion criterion) with the evidence
     cells empty; the pre-filled rows of criteria marked
     N/A are deleted at filing unless another criterion names the row.
     Evidence is a command with
     its output, a test name at a commit, a permalink into the diff, or
     a comment link — never "done". A row whose evidence exists but may
     not yet be disclosed is a rule 2 WITHHELD, never a blank; one whose
     step has not run leaves its criterion unmet (rule 10) — a posting
     handed to a holder excepted: the row citing it is WITHHELD, as the
     Mirrors row says; one whose referent an AMENDED retired cites it.
     -->

| Item | Check | Evidence | Result |
|------|-------|----------|--------|
| P/F 1 | do X at the fix commit (for an investigation, at the pinned apparatus commit), observe Y; F cell filled only if the falsification fired, with the next move taken | | |
| Contract | one line per subsection not N/A: declared vs. observed in the diff, file:line; who sees a modified external surface was told; each failure path exercised or shown unreachable; each compatibility claim pinned by a test or a recorded procedure naming the previous build's commit | | |
| Threat 1 | mitigation applied, or the acceptance re-confirmed at the fix commit | | |
| Open question 1 | resolving comment, or permalink to the diff landing the `Recommended default:` / the re-derived `HYGIENE:` item | | |
| Regression tests | each fails at pre-change commit, passes at fix: command and output (a test added alone: fails at the broken commit, or under the fault, this cell names); or the recorded manual procedure's transcript with platform | | |
| Existing tests | unmodified, or each change justified by a named prediction | | |
| Invariants | each open listing feature's § Global Invariants re-verified at the fix commit as merged into the default branch; one that also fails at the pre-change commit is recorded so here, command and output, and its criterion `WAIVED:` naming that feature (its § Re-planning Protocol supplies the fix child) | | |
| Scope | files in the diff ⊆ files the § Method / Experimental Design steps name, and each hunk at the landed sha (a merge commit: its diff against its first parent) is one a step describes (a merger's squash edit included); extras filed as #, or the step added by an AMENDED before close where they serve the contract (rule 9); a hunk undoing part of a sibling's landed change is a revert of it — the finder's AMENDED of rule 9 on that sibling, this executor being the finder, naming the re-land | | |
| Standing gates | `mvn verify` run link at the landed sha on the default branch (the merge result, as the Invariants row); SpotBugs exclusions unchanged or justified | | |
| Oracle custody | each expected value: who, when, pre-committed / independently derived / produced by the implementation, or of unshowable derivation, and deducted | | |
| Links | every permalink commit-locked, resolving and at a commit reachable from the default branch (an investigation's pinned apparatus commit: from the PR that pins it, the decision-task note); every document on the default branch | | |
| Paths | each path a criterion names exists at the fix commit, or is absent where the criterion names the step that removes it; created ones by their named step or by the named `blocked_by` task (rule 12) | | |
| Waivers | every skipped criterion has its `WAIVED:` comment, or after close the finder's AMENDED standing in for it (rule 10) | | |
| Mirrors | `STATUS: landed` posted on every open listing feature, cited; a posting handed to a holder (the materials row): WITHHELD — held by them until they post, the close not waiting on it (rule 9's landed form), the cell filled when they post (bookkeeping) | | |
| Amendments | every `AMENDED:` or `HANDOFF:` comment reconciled; the body describes what was built; ADR re-scored where an edit changed scope, evidence or decisions | | |

- [ ] Adversarial review of the diff against this issue found no substantial finding standing (each fixed before close, or the criterion it fails `WAIVED:` per rule 10)

- [ ] **Gate — Criteria & sheets.** Every completion criterion names its artifact, location and assertion; every path a criterion names exists at `evidence_commit`, or is created by a named step of § Method / Experimental Design or by a task in `blocked_by`, and the criterion says which (rule 12); every pre-filled criterion not marked N/A has its bracketed rows and every added criterion, P/F pair, threat and open question has a validation row, the Contract row carries a line per contract subsection not marked N/A and none for an N/A subsection, and no row remains for an N/A pre-filled criterion that no other criterion names (a per-item row family with zero items satisfies its bracket); nothing in the sheets contradicts § Method / Experimental Design or § Scope Boundary. Adversarial re-read of everything above found no substantial finding.

## Abstract

<!-- Written last, read first. 2–4 sentences: what is wrong or missing,
     why it matters, and the one-line shape of the proposed remedy —
     each drawn from § Intent & Alignment, § Observations, § Hypothesis
     (falsifiable) and § Conclusion & Future Work, and contradicting
     none of them. -->

## Agentic Delegability (ADR-1)

<!--
  ADR-1 v5. How safely this issue can be handed to an automated executor,
  and what maintenance liability delegating it as-is would create.
  Fill at filing. Where two ANCHORS in one axis could apply to one fact,
  take the lower. Caps and deductions stated inside an axis apply on top of
  the anchor chosen; they are not in competition with it.

  SEVEN ADDITIVE AXES, 0-5 each. RAW = their sum, 0-35.

  SC  SPECIFICATION CLOSURE — is the end state fixed by the text alone?
      Test: could two competent implementers each satisfy the completion criteria and
      produce artifacts differing in a way a reviewer would care about?
      5 every completion criterion names the artifact, its location, and the
        assertion that pins it
      4 concrete; one or two open naming choices no reviewer would litigate
      3 the goal is unambiguous, the artifact's shape is not
      2 stated as an outcome rather than as an artifact
      1 a problem and a direction; "done" is not written down
      0 open-ended, or deferring its own definition
      A criterion reading "documented", "considered" or "reviewed" with no
      named artifact caps this at 3.

  OS  ORACLE STRENGTH — the check that would actually gate the merge.
      5 exact comparison against an expected artifact that pre-exists the
        implementation or is independently derived (rule 11); an
        algebraic property (round-trip, idempotence, invariance); or a
        differential check against an independent implementation
      4 behavioural assertions over enumerated cases including the named
        failure cases; or a compiler, type or analysis gate THIS CHANGE
        WOULD FAIL BEFORE THE FIX (a standing project-wide gate every change
        already passes is not this issue's oracle)
      3 existence- or smoke-shaped: produced, exits zero, non-empty
      2 a threshold or a count, no behavioural content
      1 a person reads prose and agrees
      0 none; success asserted by whoever did the work, with no enumerated
        procedure or transcript
      For an investigation, score the P/F apparatus, not the verdict
      document; the document deduction applies only where the apparatus
      does not land or is not pinned (decision-task note).
      DEDUCTIONS, CUMULATIVE, floor 0. Deduct 2 if the expected values
      certifying the implementation were produced by running it — by the
      same change, or captured from the existing implementation's output
      by a later one — or their derivation cannot be shown (rule 11
      defines "the same change", the unshowable case and the
      independently-derived exemption). Deduct
      1 if the acceptance evidence is a document asserting a measurement.

  BR  BLAST RADIUS — everything this change must move, generated files,
      expected-output artifacts and anything published included.
      5 one file, or one new file and its test; nothing published moves
      4 a handful of files in one module
      3 several files across two modules, every caller identified
      2 more than two modules, or an interface whose callers are not
        enumerated here
      1 a repository-wide sweep, or a published format, command surface or
        registry that everything downstream reads
      0 call sites unknowable without searching the whole tree, or a
        contract with consumers outside this repository

  HL  HORIZON LENGTH — the DEPENDENT-step chain, in expert time, from a cold
      start to gated evidence; parallel steps count for less than ordered
      ones.
      5 under an hour   4 one to four hours   3 half a day to two days
      2 several days    1 more than a week, or the chain crosses a subsystem
        it must first learn                    0 a multi-week programme

  PD  PRECEDENT DENSITY — is there a worked example of this shape already in
      this repository?
      5 this issue names one and it exists
      4 a near-identical sibling is in-tree and findable by name
      3 analogous patterns exist but need adaptation
      2 the category exists; this is the first instance of its kind
      1 no in-repository precedent; the pattern comes from an external
        specification this issue cites
      0 none named; whoever executes invents the shape
  CF  CONTEXT FOOTPRINT — how much must be held in mind at once, not merely
      read.
      5 one unit and its test
      4 one module; this issue's own citations suffice
      3 two or three modules, or a module plus a format specification
      2 a subsystem boundary held from both sides at once
      1 a repository-wide invariant not verifiable locally
      0 the repository plus an external toolchain's semantics

  RD  REVERSIBILITY / DEBT SURFACE — cost of a completion that is wrong but
      plausible.
      5 a pure addition behind a check; reverting is one commit
      4 internal; wrongness surfaces at the next check or the next reader
      3 latent but discoverable; no consumer outside this repository
      2 load-bearing: a committed expected-output artifact, a ratchet floor,
        a published threshold others are then measured against
      1 a published contract: a format, a command flag, a public API,
        user-facing documentation
      0 irreversible outside this repository: an archival identifier, a
        registry coordinate, a public listing, a tagged release, a
        commitment to a third party
  TWO CAPPING AXES. They do not add; they impose a ceiling on the band.

  ED  ENVIRONMENTAL DETERMINISM — can the evidence be produced unattended,
      wherever this project runs its checks (its automation, or a
      maintainer's own checkout if it has none)? Score the evidence the work
      REQUIRES, not only what the completion criteria happen to list.
      5 self-contained: the project's standard check command, a script
        already in the tree, or its automation's own run produces it
        .......................................................... no cap
      4 needs a pinned toolchain the project can fetch and reproduce no cap
      3 needs an unreliable substrate, or an external corpus to download
        .......................................................... cap B
      2 YES, but not with what the executor has — another host platform, a
        device class, a credential that automation COULD be given, or a
        fixture only a named custodian holds, withheld until a disclosure
        event or never disclosable (rule 2) ...................... cap C
      1 NO, because something physical must be connected, operated or
        observed by hand against an enumerated procedure, or because the
        evidence is a recording of a real session with no named unattended
        harness ................................................... cap F
        A recorded manual procedure (one a person performs by hand — a
        command run in a shell with its output pasted is not one) scores 1
        unless the issue names the
        in-tree or CI substrate (a headless display run, a device farm) and
        the harness that would produce the same observation unattended —
        then it scores 2, that harness is a regression-test step § Method /
        Experimental Design must name, and the procedure is interim
        evidence.
      0 NO, because a person's participation or judgement IS the evidence:
        a human-subject trial, an independent reproducer, an external
        publisher whose acceptance is itself the evidence ........... cap F
      Reviewing a diff or approving a merge is NOT evidence production and
      does not score here — that is the band-B checkpoint, and most projects
      require it. Running the software on a platform and reporting the
      result IS evidence production and does score here.
  DA  DESIGN AUTHORITY — design decisions this issue leaves for whoever
      executes it. A design decision changes the artifact's shape and a
      reviewer could contest it: where a boundary goes, what a type carries,
      which of two mechanisms is used, what a published surface looks like,
      what is in scope.
      Count a decision whether or not the issue names it: one an executor
      will certainly hit that the text never mentions is OPEN, not absent.
      Do not count evidence hygiene, bookkeeping, or a decision another
      issue has ALREADY ANSWERED and this one cites.
      5 none open ............................................... no cap
      4 only local reversible choices remain (a name, a helper's home); or
        every open decision carries a preference an executor may act on AND
        that preference is forced by existing code, by a declared contract,
        or by the filer's own authority to decide it (a `Recommended
        default:` entry the filer may land) ...................... no cap
      3 an open decision carries a preference the filer is NOT authorised to
        make, offered pending someone's confirmation (a `PROPOSED:`
        entry) ................................................... cap B
      2 one open decision, no preference stated ................. cap C
      1 two or more open with no preference; or a `BLOCKING:` entry — an
        open decision another owner must answer before work can START. A
        `PROPOSED:` entry is 3, not this: work may begin against the stated
        preference ................................................ cap D
      0 the deliverable IS a decision: a verdict, a policy, a scope
        boundary, a gate on whether a premise holds .............. cap D

  BAND from RAW: 30-35 A | 24-29 B | 17-23 C | 10-16 D | 0-9 F.
  FINAL BAND = the most restrictive of (RAW band, ED cap, DA cap).
  Where the RAW band is C or D while SC>=4, OS>=4, DA>=4 and ED>=3 (no cap
  below B), SPECIFY-FIRST and SPLIT presuppose that SC or OS is what is
  low: record `action: DELEGATE-WITH-CHECKPOINT` and name the checkpoint
  (the review of the published surface, the format document, the flag's
  help text, the release note).
    A  DELEGATE                    automated end to end
    B  DELEGATE-WITH-CHECKPOINT    one named human approval first
    C  SPECIFY-FIRST or SPLIT      supply a closed spec, pre-committed
                                   expected values, or a decomposition
    D  HUMAN-LED                   research and harnesses can be produced
                                   unattended; a person decides and owns it
    F  HUMAN-ONLY (ED<=1) or AGENT-ASSIST-ONLY

  DEBT TAG — the liability delegating as-is would create. Exactly one, the
  first whose trigger fires:
    FABRICATED-EVIDENCE      ED<=1          evidence nobody could produce,
                                            produced anyway
    IRREVERSIBLE-PUBLICATION RD<=1          a published contract or
                                            out-of-repo commitment others
                                            may already rely on
    HOLLOW-ORACLE            OS<=2, or the 2-point deduction fired
    GOLDEN-LOCK-IN           RD=2           an expected artifact this work
                                            commits that later work is then
                                            graded against
    SPEC-DRIFT               SC<=2
    PREMATURE-SEAM           DA<=2
    PARTIAL-INTEGRATION      BR<=1 or CF<=1
    SCOPE-EXHAUSTION         HL<=1
    DUPLICATION              PD<=1
    NONE                     nothing above fired
  A low band is a routing decision, not a criticism.

  REVIEW GATE — THE TERMINAL GATE OF RULE 14. The two boxes below cover
  the ENTIRE body, § Abstract included: an issue with both ticked has been
  read adversarially and by a peer, end to end, and neither read left
  anything substantial outstanding. Tick them yourself; the filer reviewing
  their own issue is fine, and so is an executor reviewing before step
  one — that is review, not execution (rule 14).
    - SUBSTANTIAL means acting on the finding would change a score above, a
      section's mandate or a section's conformance to its mandate as its
      comment block states it, a completion criterion, a prediction, an
      edge, or the scope. Wording is not.
    - `false` means the review comment at `review_evidence` names a
      substantial finding nobody has fixed; whoever finds one and does
      not fix it sets it (bookkeeping), and the AMENDED fixing it, or a
      comment recording why it does not stand, sets `true`.
-->

```yaml
adr: 5
sc:              # 0-5  specification closure
os:              # 0-5  oracle strength, AFTER deductions
os_deduction:    # 0-3  total deducted, 0 if none
br:              # 0-5  blast radius
hl:              # 0-5  horizon length
pd:              # 0-5  precedent density
cf:              # 0-5  context footprint
rd:              # 0-5  reversibility / debt surface
raw:             # sc+os+br+hl+pd+cf+rd, 0-35
ed:              # 0-5  environmental determinism  (CAPPING)
da:              # 0-5  design authority           (CAPPING)
band:            # A|B|C|D|F — most restrictive of RAW band, ED cap, DA cap
action:          # DELEGATE | DELEGATE-WITH-CHECKPOINT | SPECIFY-FIRST |
                 #   SPLIT | HUMAN-LED | HUMAN-ONLY | AGENT-ASSIST-ONLY
debt:            # first tag whose trigger fires, or NONE
review_clean:    # true|false|pending — both boxes below
review_evidence: # permalink to the review comment, if there is one
```

- [ ] Adversarial review of this issue found no substantial finding
- [ ] Peer review of this issue found no substantial finding

<!-- One or two sentences: what an executor would actually do with this
     issue, where it would go wrong, the checkpoint where `action` is
     DELEGATE-WITH-CHECKPOINT (the `action` pickup row reads it here),
     and the single change that would raise the band. Name the section,
     the criterion, the artifact. -->
