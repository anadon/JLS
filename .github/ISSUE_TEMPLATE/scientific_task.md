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
     permalink, never a branch path (branch links rot the moment the
     branch is deleted). Source comments and audit summaries are hearsay
     until re-derived. Aggregate claims (counts, "all X", "no Y anywhere")
     carry the exact command that produced them and its output.
  2. No padding. A section that does not apply is "N/A — <one-line
     reason>", decided at filing time. A criterion discovered to be wrong
     during execution gets an AMENDED with evidence that corrects it
     (rule 9) — a bare comment records nothing; do not work around it
     and do not silently edit it.
  3. Predictions are observable: do X, observe Y. For defects and
     improvements, at least one prediction must fail at the named commit,
     and its failure must be OBSERVED before filing — run it and paste
     the command and wrong output into § Observations. For
     investigations, state instead the decision criterion: the
     observation that discriminates between the candidate answers.
  4. Atomic scope. One hypothesis cluster per issue; where scopes touch a
     sibling issue, § Related Work states which issue owns which fix and
     § Scope Boundary states what this issue will not absorb.
  5. Cross-references cite the section NAME — "§ Threats to Validity".
     Headings carry no numbers, so there is nothing else to cite.
     Subsections of § Interface & Data Contract and § Intent & Alignment
     are cited the same way — "§ Data transformations", "§ User impact".
     A trailing parenthetical qualifier may be dropped when citing:
     "§ Hypothesis" for "§ Hypothesis (falsifiable)", "§ Completion
     Criteria" for "§ Completion Criteria (Definition of Done)".
  6. Executors: before step one of § Method / Experimental Design, run
     § Pickup Checks in full. A hypothesis refuted mid-work stops the
     work: comment with the refuting evidence. A refuted issue is a
     successful experiment, not a task to salvage — except in an
     investigation, where a refuted candidate answer is a result (see
     the decision-task note below). An observation that fails to
     reproduce ends in `SUPERSEDED:` where the work has already landed
     (close with that note instead of re-doing it), in `REFUTED:` where
     the rule 3 failure no longer occurs — for an investigation, where
     the question it raised is closed by the non-reproduction — and
     nothing landed, and otherwise (the failure still occurs, or the
     question is still open) in an AMENDED that
     re-derives or retires the observation (Dropped/Retired ledger),
     re-anchors any hypothesis that named only it, and re-runs the gates
     from Gate — Intent & Status where evidence_commit is re-pinned,
     otherwise from Gate — Observations, Background & Related Work
     (rule 14), plus the pickup rows touched. Re-derive any drifted line numbers before trusting them; a
     stale citation is not evidence.
  7. Labels are not applied automatically for API-filed issues: set
     `bug` or `enhancement` explicitly, matching the corpus, plus the
     tier label `tier:task`. An investigation takes `enhancement` (it has
     no observed failure to label `bug`).
  8. Tier model — task → feature → capstone (canonical edge rules in
     the feature template; read them there, they are not restated
     here). This is the task tier, and the model is strictly layered:
     A TASK DEPENDS ON TASKS ONLY: `blocked_by` never names a feature
     or a capstone — upward edges are illegal. A task that seems to
     "wait on a feature" is really waiting on specific sibling TASKS
     inside it; name those. `blocks` names the tasks waiting on this
     one, and may also carry the MIRROR of a downward edge: a FEATURE
     whose own `blocked_by` names this task (that edge is the feature's,
     judged legal against the feature's tier; validator G09 asks for the
     mirror). A capstone never names a task, so no capstone mirror
     exists.
     `related` is reference-only, may point at ANY tier, and carries
     neither ordering nor ownership.

     THIS TASK DECLARES NO OWNER. A task may be shared by any number
     of features, and ownership lives solely in each feature's
     requires_tasks roster. To find the features that own this task,
     search the rosters — `requires_tasks` for this number, and
     `planned_tasks` for this scope while the number is fresh. An
     executor needs them at pickup (the listing-features row) and at
     close (the Invariants and Mirrors criteria): `owned_by_derived` or
     the readiness derivation supplies them where tooling runs, otherwise
     the executor searches. § Intent & Alignment may NAME the feature or capstone
     this task serves; that is a reference for alignment, not an
     ownership claim.

     `tier:` is fixed at filing. A re-tier is a new issue at the new
     tier plus a `HANDOFF: re-tier` on the old one (rule 9) moving its
     scope (a feature or capstone posts a REPLAN instead); an in-place
     tier edit is a filing defect. An issue's tier is defined by its machine block's `tier:` key;
     the tier:* label is a mirror for filtering — a missing or stale
     label is bookkeeping to fix, never an edge violation. The
     ORDERING GRAPH is blocked_by/blocks edges PLUS composition edges
     read child-before-parent (a parent cannot close before its
     children land); that combined graph must stay a DAG. The machine
     block in § Status & Dependencies is the source of truth for the
     edges it can express.
  9. Amendment & comment protocol. This body may be edited, but only
     together with an `AMENDED:` comment stating what changed, why,
     and on what evidence — a silent edit is invisible to executors,
     who reconstruct state from the machine block plus the prefixed
     comments (`STATUS:` / `REFUTED:` / `HANDOFF:` / `SUPERSEDED:` /
     `AMENDED:` / `WAIVED:`). `HANDOFF:` is the AMENDED of a split or
     transfer and is read as one everywhere this template says AMENDED
     (gate resets, pickup rows, validation rows, mirrors). It carries a
     sub-tag — `HANDOFF: split`, `HANDOFF: transfer` or
     `HANDOFF: re-tier` — so no issue number stands first; successor or
     new-issue numbers follow a dash after the sub-tag ("HANDOFF: split —
     successors #124, #125"), never directly after it, which is where a
     mirror places the sender's number. A re-tier HANDOFF carries the new
     issue's number and the Dropped/Retired ledger moving every item to
     it, and closes this issue (rule 10). A split HANDOFF carries the
     successor number(s), the
     Dropped/Retired ledger moving each item to them, and the gates
     re-run (rule 14); its order is fixed: REPLAN the listing feature
     first (successor as a planned_tasks scope, handoffs moved), file the
     successor, post the HANDOFF, then resolve the planned scope to the
     number as bookkeeping. Where no feature lists this task and the
     successors align to the same standing rule, the HANDOFF alone
     carries the split (successor numbers, ledger, orderings by AMENDED
     on siblings per § Related Work); where a feature is warranted
     (feature rule B satisfiable), FILING it stands in for that REPLAN: this task
     in requires_tasks, the successors in planned_tasks, its roster row
     and handoffs written against this task's contract as the HANDOFF
     will leave it, and its Gate — Decomposition, Contract & Integration
     and every later gate left unticked until the HANDOFF is posted, then
     ticked in order as bookkeeping citing it.
     A transfer HANDOFF states that the body is unchanged
     and carries the branch and commit of the work, the PR, each Method
     step already ticked with the comment that evidences it, and the
     first step the successor executes (`HANDOFF: transfer — no step
     executed` where the predecessor stopped before step one). The
     successor posts its own `STATUS: pickup` citing the HANDOFF, re-uses
     the predecessor's rows run at the same checkout, and re-runs the
     observation, supersession and evidence_commit rows where HEAD moved
     plus any row whose referent gained comments since — observation,
     supersession and evidence_commit rows at the
     work's merge-base with the default branch, all other rows at the
     handoff commit. `STATUS:` carries a sub-tag —
     `STATUS: pickup` (§ Pickup Checks outcome), `STATUS: progress`,
     `STATUS: landed`, whose first line carries — after a dash, never
     directly after the sub-tag — the fix commit, the PR, the permalink
     to the body revision holding the filled § Post-Implementation
     Validation, the AMENDED of any contract deviation still to be
     reconciled, and the open listing features it is mirrored on
     (`STATUS: landed — <sha>, PR #45, <permalink>; deviations: …;
     mirrored on #F1, #F2`). Post each such comment on THIS issue and
     mirror it, led by this issue's number (`STATUS: landed #N — …`) —
     for a landing, its first line suffices — on every OPEN feature
     whose requires_tasks roster lists this task
     (there may be several, or none) — except `STATUS: pickup` and
     `STATUS: progress`, which stay here. An AMENDED edit resets and
     re-runs gates as rule 14 says, and the AMENDED comment names the
     gates re-run. An AMENDED posted after `STATUS: pickup` also re-runs
     the § Pickup Checks rows its edit touches (observations,
     `blocked_by`, materials, paths) and records those re-runs. An
     AMENDED whose contract now contradicts a listing feature's
     § Global Invariants, or a handoff that feature's § Feature-Level
     Interface & Data Contract assigns to this task, is a re-plan
     request: the mirrored comment says so, and the gate covering the
     contradiction stays unticked until that feature's REPLAN answers.
     A `REPLAN:` a listing feature posts here, led by its number, is
     answered on receipt (at pickup if none has happened yet) — unless
     this issue has closed, when the REPLAN is a notice and no answer is
     owed (any deviation of what was built from what this body records —
     a contract subsection, a prediction or sheet cell recorded as held,
     a criterion recorded as met — discovered after close is instead
     recorded by an AMENDED on this closed issue posted by the finder:
     the cell or subsection corrected, the Dropped/Retired ledger, the
     successor now tracking any unmet obligation or why none is needed
     (standing in for rule 10's WAIVED), no gate re-run, the ADR block
     left as it scored the filing; it is mirrored on every listing
     feature, open or closed — an open one REPLANs, on a closed one the
     finder posts the closed-feature REPLAN of capstone rule D, and so on
     upward until an open issue answers or none lists it): by an
     AMENDED where any section of this body must change (typically
     § Interface & Data Contract, the machine block or § Intent &
     Alignment — a drop with the disposition "freed" restates the
     alignment, "closed" closes this issue under rule 10), otherwise by
     `STATUS: progress` naming the sections reassessed. An AMENDED answer
     is mirrored on the feature whose REPLAN it answers even when its
     roster no longer lists this task; a `STATUS: progress` answer stays
     here and the feature reads it on this issue. Where an AMENDED
     re-scores § Agentic Delegability and `action` changes, the comment
     states the new action and execution follows it from that point:
     SPLIT → the split-HANDOFF order; SPECIFY-FIRST where SC, OS or DA
     drove it → stop after the AMENDED, which names what is owed and who
     supplies it (the closed spec or oracle, or the marker and preference
     for the open decision — then follow that marker); SPECIFY-FIRST
     where the ED cap drove it → the evidence steps are named blocked and
     handed to the platform holder as the materials row of § Pickup
     Checks does, every other step proceeds; HUMAN-LED → research and
     harness steps proceed; where DA drove it the deciding step is marked
     `BLOCKING: <who decides>` and waits, where RAW drove it the AMENDED
     names who owns the spec or oracle and adds no marker; HUMAN-ONLY → the evidence steps
     are named blocked and handed to a holder as the materials row does,
     every other step proceeds; AGENT-ASSIST-ONLY → stop after the
     AMENDED, which names the spec, oracle or decomposition owed;
     DELEGATE-WITH-CHECKPOINT → the named checkpoint precedes the next
     step or the merge; DELEGATE → nothing. An AMENDED that adds a module
     or file that a sibling named in § Related Work owns, or that § Scope
     Boundary lists as out, resets from Gate — Observations, Background &
     Related Work (rule 14), since sibling scopes are read there. An own comment is never led by another issue's number:
     a number in the first position marks a mirror, or a notice posted
     on another issue. Every AMENDED or split HANDOFF names its
     predecessor — the latest own body-editing comment (AMENDED or split
     HANDOFF), or "first" — and the writer re-reads the comment stream
     immediately before the body edit and folds in any body-editing
     comment newer than its read, since body edits are last-write-wins
     and the stream is the arbiter. When an
     edit REMOVES or NARROWS any claim, observation, prediction,
     criterion, edge, or scope item, the AMENDED comment must carry a
     "Dropped/Retired" ledger enumerating each removed item with its
     disposition — retired with reason, moved to issue #N, or restated
     where — so omissions are auditable by reading the comment, not
     only by diffing bodies. Bookkeeping is exempt from the AMENDED
     requirement: ticking a gate or a review box (the tick is the
     record); filling a check-sheet's evidence and result cells from
     recorded evidence (the close-out comment — `STATUS: landed`, or
     REFUTED/SUPERSEDED where rows were already filled — links the
     revision holding them; the Mirrors cell alone is filled after the
     mirrors are posted, citing them); ticking a Completion, Pickup or
     Method checkbox whose backing evidence is in the validation row(s)
     it names once `STATUS: landed` links the revision, in the
     `STATUS: pickup` comment recording that row, or in a comment on this
     issue that quotes the box (a PR link inside that comment is the
     usual evidence); ticking the diff-review box under the sheet, which
     is a review-box tick; setting `review_clean: false` with both review
     boxes unticked and `review_evidence` pointing at the finding, on an
     open issue, and re-ticking with `true` once the finding is fixed or
     recorded as not standing; re-pinning evidence_commit after re-deriving
     citations — bookkeeping only when every cited line re-derives at
     the new commit saying what it said and every observation's command
     reproduces its output there, otherwise part of the AMENDED that
     fixes the observation; adding to § Interface & Data Contract the
     citation of the listing feature's REPLAN that answered this task's
     re-plan request by changing the invariant or handoff, together with
     ticking the gate left pending on it (a REPLAN that keeps them is
     answered by the AMENDED that re-conforms this contract, which re-runs
     the gate);
     adding or removing a `blocks` entry that mirrors a
     counterpart's authoritative `blocked_by`, citing the counterpart
     (its number, and the AMENDED or REPLAN comment where one created
     the edge); and replacing an
     "unfiled" alignment in § Intent & Alignment with the citation once
     the parent exists — provided the edit's comment states that the
     cited section was read and says what this task assumes (if it does
     not, the edit is an AMENDED one).
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
     comment leaves the issue unclosable. A close on `REFUTED:`,
     `SUPERSEDED:` or `HANDOFF: re-tier`, or on a listing feature's REPLAN
     disposition "closed" (recorded by the AMENDED that answers that
     REPLAN, citing it), or "freed" where the answering AMENDED records
     that no standing rule or open feature can be cited and closes under
     it, needs no WAIVED comments: that comment is the close-out record,
     the check-sheets are not completed (rows already filled stand, and
     the comment links the revision holding them), and any obligation
     still needed names its successor inside it.
  11. Oracle custody. Every completion criterion that compares against an
     expected value names WHO produced that value and WHEN. Values
     committed and reviewed BEFORE the implementation, and values written
     by the same change as the implementation, are different guarantees,
     and only the first is evidence. "The same change" is the merge unit
     — the PR — whatever its commit order. A value derived independently
     of the implementation (from a specification, a fixture, or a hand
     computation) whose derivation is recorded in § Data Collection &
     Analysis is not self-certified even when it lands in that PR; a
     value produced by running the implementation is. Where an
     expected-output artifact is
     generated by the same change that produces the behaviour it certifies,
     say so in § Data Collection & Analysis and apply the corresponding
     deduction in § Agentic Delegability. A check authored alongside the
     thing it checks passes indefinitely and is then cited as ground truth
     by everything built on it.
  12. Artifact paths resolve at filing. Every path a criterion names either
     exists at `evidence_commit`, or is created by a named step of this
     issue, or is created by a task named in `blocked_by` — and
     § Completion Criteria says which. A criterion pointing at something
     that does not exist is unclosable: whoever picks it up will either
     invent a location or skip the criterion silently.
  13. Open Questions are MARKED, not merely listed. Each entry ends in
     exactly one of `Recommended default: <answer>` (an executor may
     proceed and land), `PROPOSED: <answer>, pending <who confirms>` (an
     executor may draft but not land), `BLOCKING: <who decides>` (an
     executor must stop), or `HYGIENE: <what to re-derive>` (not a decision
     at all — a citation, a status check, an un-run build). This template
     requires the section, so every issue has one; without a marker a
     reader cannot tell a decision that was made from one that was dodged,
     and § Agentic Delegability scores the entry as unresolved, which is
     usually not what was meant.
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
     Delegability gives it: acting on the finding would change a mandate
     or a section's conformance to it as its comment block states it, a
     score, a criterion, a prediction, an edge, or the scope; wording is
     not. There is no bound on the number of passes.
     Gates are cumulative. Gate k covers every section through k, so
     while filing, a finding at gate k that changes an earlier section
     is fixed there and the earlier gate is not re-ticked: its tick
     records that the re-read happened when it was reached, not that
     the section was final — but the fix repeats clause (c) of the gate
     that follows the changed section, for the external context that
     gate names (siblings, the planning feature's assignments, the
     alignment target), and gate k's tick records that. The terminal gate (the two review boxes in
     § Agentic Delegability) covers the whole body, § Abstract
     included, and is the only gate whose tick asserts consistency of
     the finished issue. A filed issue with unticked gates is valid but
     unvalidated, exactly as an unticked review gate leaves the band
     unvalidated. Execution ticks no gate.
     AFTER FILING, an AMENDED or split-HANDOFF edit (rule 9; a transfer
     HANDOFF changes no section and resets no gate) unticks the gate that
     follows the changed section and every later gate, and the two
     review boxes (`review_clean: pending`); whoever amends re-ticks
     them in order, and the AMENDED comment names the gates re-run —
     that comment, not checkbox state, is what § Pickup Checks reads.
     For a machine-block key, the changed section is the one at whose
     gate the block's comment says that key is filled; for a file or
     module that a sibling named in § Related Work owns, or that § Scope
     Boundary lists as out, the reset starts at Gate — Observations,
     Background & Related Work (rule 9). Gate clauses
     never depend on a counterpart's later edit, except where a clause
     reads the counterpart's current body (a listing feature's § Global
     Invariants, rule 9) or where this rule, rule 9's split stand-in,
     feature rule C or the capstone's rule D leaves a gate pending on a
     counterpart's answer to a REPLAN or HANDOFF this issue requested
     (the comment names the pending gates). On a closed issue the
     finder's AMENDED of rule 9 unticks nothing: the gates record the
     filing, the AMENDED records what was built. Mirror entries that a
     counterpart forces onto
     the machine block are confirmed in the Counterparts box under it,
     as bookkeeping.
     § Abstract is written last and read first.
  15. Three check-sheets, distinguished by WHEN they run. The gates of
     rule 14 run at filing. § Pickup Checks runs once in full, before
     step one of § Method / Experimental Design, by whoever executes
     (rule 6), and again in full by a HANDOFF successor (rule 9); after
     a post-pickup AMENDED, rule 9 re-runs only the rows the edit
     touches, recorded in that comment.
     § Post-Implementation Validation runs at close, against the actual
     diff and PR, one row per item. § Completion Criteria states WHAT
     must be true of the finished work; the validation sheet records HOW
     each of those things, and each prediction, contract subsection,
     threat and open question, was verified. Rows of both sheets are
     ENUMERATED AT FILING with their evidence cells empty, so the gate
     after them can check that nothing the body asserts lacks a row.

  Decision/spike tasks ("evaluate X", "decide Y") use this template
  with "verdict recorded in <named doc/section>" as the completion
  contract; sections that presuppose a code defect are N/A (rule 2).
  Their hypotheses are the candidate answers, and the P/F pairs are the
  rule 3 decision criterion. A refuted hypothesis is a verdict, not a
  stop: record it and land with `STATUS: landed`; `REFUTED:` is for the
  investigation's own premise failing. The apparatus that produces the
  decision evidence lands at a named path, or is pinned by a PR that
  retains its commits, so the evidence can be re-run at that commit.

  Refactor tasks (behaviour-preserving changes) use this template with
  the preservation claim as the hypothesis, naming the call-site
  observations it covers; the rule 3 failure is the structural check
  (compilation, an analysis gate, a signature or call-graph assertion)
  that fails at `evidence_commit`. § Agentic Delegability's OS axis
  scores the preservation check: a differential or round-trip over the
  changed surface is 5; the existing suite is 4 only when § Data
  Collection & Analysis names the tests exercising every changed call
  site; otherwise score the preservation evidence actually present, at
  most 3 (a green suite not mapped to the changed call sites is
  smoke-shaped) — the structural check is the rule 3 failure, never the
  preservation oracle: OS anchor 4's analysis-gate clause does not apply
  to it, since it evidences the change, not preservation.

  These comments do not render on GitHub — leave them in place for the
  next reader of the raw issue body.
-->

## Intent & Alignment

<!-- Written FIRST. Everything below is checked against this section
     at every gate, and this section is checked against § Observations
     once they exist (impact claims are observations too, rule 1: where
     the harm or gap is measurable — a wrong simulation, a crash, a lost
     edit, a silent mis-load — § Observations pins it and this section
     must agree).

     Intent: the change in the world this work is for, in one
     paragraph, phrased so that a reader can later say whether it
     happened. Not the mechanism — the mechanism is § Hypothesis and
     § Method / Experimental Design.

     Alignment: what this work serves, cited by issue number AND
     section name — a feature's § Capability Statement & Scope
     Boundary, a capstone's § Outcome Statement — or the standing
     invariant, format document or project rule it upholds. A rule no
     document carries is stated here in one line ("user-visible text is
     spelled correctly"); the gate then checks only that a maintainer
     would uphold it and that the intent follows from it. This is a
     reference for checking alignment, not an ownership claim (rule 8).
     When a parent WILL exist but is not yet filed, write "owning feature
     unfiled — <one-line scope>" and replace it with the citation once it
     exists (bookkeeping, rule 9); a task that will never have a parent
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
     (rule 8). It is filled PROGRESSIVELY, the gates say when:
       - `evidence_commit` now, before anything is observed. It is the
         single SHA every § Observations file:line is pinned to — cite
         by permalink at this commit (rule 1); if HEAD has moved,
         re-derive citations before trusting them.
       - `blocked_by` and `related` at the gate after § Related Work,
         once the siblings are known. Annotate each blocked_by entry
         with the one-line reason it blocks, as a YAML comment.
         `blocks` stays `[]` at filing: it carries mirrors only, confirmed
         in the Counterparts box.
       - `owned_by_derived` is inserted by tooling, never by hand.
     Executors: § Pickup Checks reads this block first. -->

```yaml
tier: task
evidence_commit:        # SHA all § Observations citations are pinned to
# owned_by_derived: []  # OPTIONAL and NON-AUTHORITATIVE; left absent at filing. A
                        #   generated copy of the features whose requires_tasks lists
                        #   this task, inserted and regenerated by tooling so the issue
                        #   names its owners when read alone. Those rosters are the
                        #   only real record; never hand-edit it (an empty list here
                        #   counts as populated, so G21 reports it as drift as soon
                        #   as any roster lists this task).
blocked_by: []          # ordering: TASKS that must land first — tasks only.
                        #   Never a feature, never a capstone (upward edges are
                        #   illegal); name the specific sibling tasks instead.
blocks: []              # mirrors only — the counterpart's blocked_by is authoritative:
                        #   tasks, or features, whose blocked_by names this task (never
                        #   a capstone). Left [] at filing; confirmed in the
                        #   Counterparts box.
related: []             # reference only — never blocking, never ownership.
                        #   Ownership is not recorded here: it lives in each
                        #   owning feature's requires_tasks roster.
```

- [ ] **Counterparts synced** (bookkeeping, ticked after filing and re-ticked whenever a counterpart changes): every task or feature whose `blocked_by` names this task appears in `blocks`; every task this task's `blocked_by` names carries this task in its `blocks`; the alignment in § Intent & Alignment cites an open issue or a standing rule, or is still marked unfiled — a closed target is re-aligned by AMENDED.

- [ ] **Gate — Intent & Status.** § Intent & Alignment states an intent a reader could later confirm or deny; its alignment target was opened at its current revision and the cited section says what this task assumes it says, or the target is marked unfiled with a one-line scope, or it is a standing rule stated here that a maintainer would uphold and from which the intent follows; every audience named is one this change reaches. `evidence_commit` is the SHA actually checked out and is reachable from the default branch. Adversarial re-read of this span found no substantial finding.

## Observations

<!-- Numbered, reproducible facts. Each carries file:line at
     `evidence_commit` plus the quoted line(s), or the exact command and
     its output. Include the observed failure required by rule 3. Every
     measurable claim made in § Intent & Alignment has its observation
     here. -->

## Background & Prior Work

<!-- What already exists: relevant code paths, prior issues/PRs, audit
     findings, external tools or literature. Link them. Code claims
     carry file:line at `evidence_commit` (rule 1). -->

## Related Work

<!-- Sibling issues, the features whose rosters list this task or
     whose `planned_tasks` carries this scope (search both; rule 8),
     audit findings, external references. Where
     scopes touch, state which issue owns which fix. The `blocked_by`
     and `related` entries of the machine block are derived from this
     section — fill them now, and record the DAG walk (follow each named
     issue's edges outward and confirm no path returns here); `blocks`
     stays empty or mirrors-only. Include every ordering a listing or
     planning feature's § Sequencing & Parallelism records against this
     scope: into `blocked_by` here, or, where an already-filed sibling
     must wait on this task, by an AMENDED on that sibling posted by
     whoever files this task. -->

- [ ] **Gate — Observations, Background & Related Work.** `evidence_commit` is reachable from the default branch, and `git log --oneline <evidence_commit>..origin/<default> -- <every path cited in § Observations and § Background & Prior Work>` is empty or its output is pasted with each observation touching a listed path re-run at that head; every observation reproduces at `evidence_commit` with command and output pasted, or is a quoted line at a commit-locked permalink; the rule 3 failure is among them, or the task is an investigation and needs none; every code claim in § Background & Prior Work carries a permalink at `evidence_commit`; § Intent & Alignment's measurable claims each have an observation and agree with it. § Related Work names every sibling whose scope touches this one and says which owns which fix; `blocked_by` and `related` are filled from it and `blocks` is empty or carries only mirrors (every `blocked_by` entry is a task; every `blocks` entry is a task or a feature whose `blocked_by` names this one), the DAG walk is recorded, and each named issue's body was read at its current revision — nothing here contradicts a sibling's stated scope or a listing or planning feature's § Capability Statement & Scope Boundary. Adversarial re-read of everything above found no substantial finding.

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
           § Observations if yes).   Must hold after the fix: yes/no.
           Tests: H1.
       F1. If after the fix X still yields not-Y, H1 is wrong — next
           move: investigate Z / file successor / stop.

     Every hypothesis has at least one P and one F. Predictions that
     hold only after the fix say so. For investigations, the pairs are
     the decision criteria (rule 3). -->

## Materials & Apparatus

<!-- Toolchain, fixtures, test rigs, corpora, platforms: everything
     needed to perform each "do X" above. Note anything that does not
     exist yet and must be built first — § Method / Experimental Design
     must build it, or a task in `blocked_by` provides it (landed at
     pickup). -->

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
     routine and `FORMAT_VERSION` in `Circuit`) — plus whether the source is trusted or must be
     treated as hostile (a user-supplied `.jls` file is hostile input). -->

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

- [ ] **Gate — Contract declarations.** Every interface or datum consumed names its provider and every one provided names its consumer, or says none exists yet; every structure links its authoritative definition or defines it here; every claim about existing code carries file:line at `evidence_commit`; hostile inputs are marked; every surface a prediction exercises appears in some subsection above, and no provided or modified declaration lacks a hypothesis or prediction that motivates it; every artifact or handoff that a listing or planning feature's § Feature-Level Interface & Data Contract or § Integration Criteria & Evidence Plan assigns to this task appears above, and no sibling interface consumed or provided above is one that contract does not assign to this task unless the filer has REPLANned that feature to assign it (feature rule C — the plan is living) and cites the REPLAN here; nothing above is a diff. Adversarial re-read of everything above found no substantial finding.

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
     behavior. Prose may accompany the math; it may not replace it. A
     reviewer must be able to locate each defined stage in the diff. -->

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

- [ ] **Gate — Contract complete.** Every partial point flagged at the previous gate has an owner in § Failure modes & error handling; every durable datum names what is guaranteed uncorrupted on failure; every modified external surface, durable datum, and already-called public internal surface has a compatibility claim stated so a test can pin it, and every durable format or command surface states the previous version's behaviour on the new output; every compatibility claim that matters has a prediction in § Predictions & Falsification Criteria, or one was added; the contract as a whole is what § Hypothesis (falsifiable) implies — no more, no less; nothing in § Compatibility, versioning & migration contradicts the § Global Invariants of any feature whose `requires_tasks` lists this task or whose `planned_tasks` carries this scope. Adversarial re-read of everything above found no substantial finding.

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
     behavioral fix carries a regression test that fails at the
     pre-change commit and passes with the fix — or, where no automated
     check can observe it, § Data Collection & Analysis names the
     recorded manual procedure, with platform, that stands in for it and
     § Threats to Validity carries the resulting threat with its
     acceptance reason (the MANUAL-PROCEDURE ALTERNATIVE; the gate and
     the completion criterion cite it). Every material flagged as
     not yet existing is built by a named step or is named as provided
     by a `blocked_by` task (rule 12). Proposed diffs go here
     (or in later sections) — always after § Interface & Data Contract,
     never before it. -->

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

- [ ] **Gate — Scope, Method & Consequences.** Every step is reviewable on its own, names the files it touches (the same `git log` as at Gate — Observations, Background & Related Work pasted empty for those files, or the step re-derived at that head; each file within the scope § Related Work assigns to this task, a file a sibling's scope covers being restated there — after filing, rule 9 resets from that gate), and names the contract subsection or prediction it serves; every behavioral change has its regression-test step or invokes the manual-procedure alternative of § Method / Experimental Design; every not-yet-existing material has its build step or is named as provided by a `blocked_by` task (rule 12); no step crosses § Scope Boundary and no out-of-scope item lacks an owning issue or an explicit "unfiled"; every contract subsection that declares a change is implemented by some step; every interface or invariant the contract moves has its consequence stated, every cost identified is stated or the section says there is none and why, and no consequence was accepted that § Intent & Alignment would not justify. Adversarial re-read of everything above found no substantial finding.

## Data Collection & Analysis

<!-- How results are recorded and judged: which tests assert which
     prediction; what manual verification (with platform) is recorded in
     the PR. For every expected value compared against, who produced it
     and when (rule 11). -->

## Threats to Validity

<!-- What could make the results misleading: platform differences,
     headless-vs-GUI divergence, stale line numbers or counts, fixture
     bias, tests that shortcut the real code path. Each threat names its
     mitigation — a step in § Method / Experimental Design, a check in
     § Data Collection & Analysis — or is explicitly accepted with the
     reason. -->

- [ ] **Gate — Evidence & threats.** Every P and F has a named test or a recorded manual procedure with platform in § Data Collection & Analysis; every expected value names its custodian and date; every threat has a mitigation located in a named section or is accepted with a reason; every Method step invoking the manual-procedure alternative names its recorded procedure with platform in § Data Collection & Analysis and its accepted threat in § Threats to Validity, and that platform or apparatus appears in § Materials & Apparatus; no test named shortcuts the code path its prediction is about. Adversarial re-read of everything above found no substantial finding.

## Open Questions & Decisions Needed

<!-- Decisions this task cannot make for itself — cost, custody, policy,
     taste, or unverified external state. Include every design decision
     an executor will certainly hit, whether or not the text above has
     named it: an unlisted one is open, not absent (§ Agentic
     Delegability, DA axis). For each: the question, the options with a
     recommended default, and whether it blocks filing, blocks
     execution, or can ride along. "N/A — fully specified" if nothing is
     open.

     Mark every entry per rule 13 — `Recommended default:`, `PROPOSED:`,
     `BLOCKING:` or `HYGIENE:`. The marker is what an executor and
     § Agentic Delegability both read; an unmarked entry is scored on its
     substance, and the marker fixed. -->

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
     fit (rule 2 governs inapplicable ones), and add criteria specific to
     this task; each pre-filled box names in brackets the row(s) of
     § Post-Implementation Validation that verify it, and each added
     criterion gets a row of its own.

     Integrity rule: tests verify the work, they do not define it. If a
     criterion below turns out to be wrong or unsatisfiable, follow
     rule 2 — an AMENDED with evidence, do not work around it. -->

- [ ] Every post-fix prediction in § Predictions & Falsification Criteria holds at the fix commit, and no falsification criterion fired unaddressed [rows: P/F]
- [ ] The post-change code satisfies § Interface & Data Contract in every subsection — interfaces provided/consumed, structures, concurrency model, failure behaviour, compatibility claims — with any deviation recorded by an AMENDED of the deviating subsection before close (rule 9), never by a bare comment or silently absorbed [rows: Contract]
- [ ] Every behavioral change has a regression test that fails at the pre-change commit and passes at the fix commit, or the manual-procedure alternative of § Method / Experimental Design was recorded with platform [row: Regression tests]
- [ ] Existing tests pass unmodified, except tests whose asserted behavior this issue intentionally changes — each named, with the prediction that justifies the new expectation [row: Existing tests]
- [ ] § Global Invariants of every feature whose `requires_tasks` lists this task hold at the fix commit [row: Invariants]
- [ ] `mvn verify` green (tests + SpotBugs, warnings-as-errors) [row: Standing gates]
- [ ] No new entries in `config/spotbugs-exclude.xml`, or each new entry is `Class`-scoped with a justification [row: Standing gates]
- [ ] No changes outside § Method / Experimental Design and inside § Scope Boundary; adjacent work discovered en route is filed as new issues [row: Scope]
- [ ] Every expected value this issue was graded against was pre-committed or independently derived, or § Data Collection & Analysis records that it was produced by the implementation and the ADR-1 OS deduction was applied (rule 11) [row: Oracle custody]
- [ ] Every decision in § Open Questions & Decisions Needed is resolved or explicitly deferred, none left blocking [rows: Open question]
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (rule 10) [row: Waivers]
- [ ] Every cited evidence document and permalink resolves on the default branch at close — no branch-path links, no deleted docs [row: Links]
- [ ] Every path named above exists at `evidence_commit`, or is created by a named step of § Method / Experimental Design, or by a task in `blocked_by` — this list says which (rule 12) [row: Paths]
- [ ] Landing reported with a `STATUS: landed` comment on every OPEN feature whose `requires_tasks` lists this task, citing the AMENDED of any contract deviation those plans must reconcile [row: Mirrors]
- [ ] § Agentic Delegability re-scored on any `AMENDED:` edit that changed scope, evidence, or open decisions [row: Amendments]
- [ ] ... [row: <added below>]

## Pickup Checks

<!-- Run once by the executor, before step one of § Method /
     Experimental Design (rule 6). Each box is a precondition for
     starting, not a completion criterion. The rows are fixed and never
     deleted: a row whose referent is N/A or empty is recorded "none" in
     the pickup comment (one line may list every "none" row) and neither
     passes nor fails. Record the outcome in a
     `STATUS: pickup` comment on this issue (not mirrored, rule 9) that
     names the checkout commit and lists the rows run and the rows not
     reached — a later executor at that checkout cites the rows run and
     re-runs only the observation, supersession and evidence_commit rows
     where HEAD moved plus any row whose referent gained comments since.
     A
     failed supersession or observation row ends as rule 6 says —
     `SUPERSEDED:`, `REFUTED:`, or an AMENDED that re-derives or retires
     the observation; any other failed row ends in the `STATUS: pickup`
     comment recording the block, and only the steps it names as blocked
     wait. -->

- [ ] `review_clean` is not `false`; a `false` is answered before step one by the AMENDED fixing the cited finding, or by a comment quoting the finding and recording why it does not stand (re-ticking is then bookkeeping) — where the executor cannot complete that AMENDED, the `STATUS: pickup` records a whole-issue block naming what is owed and who supplies it, and no step starts
- [ ] `action` read; execution follows the rule 9 mapping — SPLIT: the split-HANDOFF order before step one; SPECIFY-FIRST (SC, OS or DA driven) and AGENT-ASSIST-ONLY: stop after `STATUS: pickup`, which names what is owed and who supplies it; HUMAN-LED: research and harness steps proceed; where DA drove it the `STATUS: pickup` names the deciding step and who decides (an unmarked decision is an AMENDED adding the marker, not a pickup act), where RAW drove it it names who owns the spec or oracle; HUMAN-ONLY and ED-driven SPECIFY-FIRST: evidence steps through the materials row; DELEGATE-WITH-CHECKPOINT: the checkpoint named
- [ ] Every `AMENDED:` or `HANDOFF:` comment read; each body-editing one names its predecessor and the gates it re-ran (a transfer HANDOFF states the body is unchanged), and the body matches the fold of all body-editing ones in stream order — one whose named predecessor is not the previous body-editing comment is re-folded first
- [ ] Every `REPLAN:` a listing feature posted here read; § Interface & Data Contract re-checked against each changed invariant, handoff or drop disposition, and answered by AMENDED or `STATUS: progress` (rule 9) — where the answer needs an AMENDED the executor cannot complete, the `STATUS: pickup` records a whole-issue block naming what is owed and who supplies it (the filer, or the feature that posted the REPLAN), and no step starts
- [ ] Citations re-derived if HEAD has moved past `evidence_commit` (renamed paths followed); `evidence_commit` re-pinned — bookkeeping only under rule 9's condition, otherwise part of the AMENDED below
- [ ] Not superseded: the rule 3 failure still occurs, or, for an investigation, the question is still open; if the work has already landed, close with a `SUPERSEDED:` comment
- [ ] Every observation in § Observations re-verified at the checkout; command and output recorded — a non-reproducing observation is routed as rule 6 says (SUPERSEDED, REFUTED, or AMENDED re-deriving or retiring it, using the row above's determination), never worked around
- [ ] Every `blocked_by` entry has landed, or the edge was removed by an `AMENDED:` comment with a Dropped/Retired ledger entry
- [ ] Every `BLOCKING:` entry in § Open Questions & Decisions Needed is answered; `PROPOSED:` entries noted as draft-only
- [ ] Open features whose `requires_tasks` lists this task located (roster search) — these receive the mirrored comments of rule 9, and their § Global Invariants bind this work; a feature whose `planned_tasks` still carries this scope is asked to resolve it to this number (feature rule C) before it is owed anything
- [ ] Every ordering a listing feature's § Sequencing & Parallelism records against this task is in `blocked_by` here or, where the sibling waits on this task, in that sibling's `blocked_by` (mirrored in `blocks`), or its absence is explained by an AMENDED
- [ ] For every `blocked_by` task providing an interface § Internal interfaces consumed relies on: its `STATUS: landed` comment read and its § Internal interfaces provided — public re-read at the landed commit; a deviation from what this task assumes is an AMENDED here before step one
- [ ] Every material in § Materials & Apparatus available, or scheduled by its Method step, or provided by a `blocked_by` task that has landed (row above), or needed only by named evidence steps — then the `STATUS: pickup` comment names those steps as blocked and the holder they are handed to, or that no holder is yet known (those steps stay blocked), and every other step may proceed
- [ ] Every path a completion criterion names exists at the checkout, or the Method step that creates it is scheduled, or the `blocked_by` task that creates it has landed (rule 12)

## Post-Implementation Validation

<!-- Run at close against the actual diff and PR. One row per item;
     rows are enumerated AT FILING (a row per P/F pair, per contract
     subsection not marked N/A, per threat, per open question, per added
     completion criterion) with the evidence cells empty, so the gate
     below can check that nothing above lacks a row; the pre-filled rows
     of subsections marked "N/A — <reason>", and of pre-filled criteria
     marked N/A, are deleted at filing unless another criterion names
     the row. Evidence is a command with
     its output, a test name at a commit, a permalink into the diff, or
     a comment link — never "done". A row that cannot be filled is a
     rule 2 comment, not a blank. -->

| Item | Check | Evidence | Result |
|------|-------|----------|--------|
| P/F 1 | do X at the fix commit (for an investigation, at the pinned apparatus commit), observe Y; F cell filled only if the falsification fired, with the next move taken | | |
| Contract: § External interfaces consumed | declared vs. observed in the diff, file:line | | |
| Contract: § Internal interfaces consumed | each relied-on surface present as declared | | |
| Contract: § Data consumed (structure) | declared vs. observed | | |
| Contract: § External interfaces modified | declared vs. observed; who sees it was told | | |
| Contract: § Internal interfaces provided — public | signature and contract as declared; callers as named | | |
| Contract: § Internal interfaces provided — private | privacy enforced as declared | | |
| Contract: § Data provided (structure) | structure as declared and documented | | |
| Contract: § Data durably tracked | location, version, migration as declared | | |
| Contract: § Data ephemerally used | lifetime and loss behaviour as declared | | |
| Contract: § Concurrency model | every guard present at its named state | | |
| Contract: § Data transformations | each defined stage located in the diff | | |
| Contract: § Failure modes & error handling | each failure path exercised or shown unreachable | | |
| Contract: § Compatibility, versioning & migration | each claim pinned by a test, or by a recorded procedure naming the previous build's commit | | |
| Threat 1 | mitigation applied, or the acceptance re-confirmed at the fix commit | | |
| Open question 1 | resolving comment, or permalink to the diff landing the `Recommended default:` / the re-derived `HYGIENE:` item | | |
| Regression tests | each fails at pre-change commit, passes at fix: command and output; or the recorded manual procedure's transcript with platform | | |
| Existing tests | unmodified, or each change justified by a named prediction | | |
| Invariants | each listing feature's § Global Invariants re-verified at the fix commit | | |
| Scope | files in the diff ⊆ files the § Method / Experimental Design steps name; extras filed as # | | |
| Standing gates | `mvn verify` run link; SpotBugs exclusions unchanged or justified | | |
| Oracle custody | each expected value: who, when, pre-committed / independently derived / produced by the implementation and deducted | | |
| Links | every permalink and document resolves on the default branch | | |
| Paths | each path a criterion names exists at the fix commit; created ones by their named step or by the named `blocked_by` task (rule 12) | | |
| Waivers | every skipped criterion has its `WAIVED:` comment | | |
| Mirrors | `STATUS: landed` posted on every open listing feature (the one cell filled after the linked revision, citing the mirror comments) | | |
| Amendments | every `AMENDED:` or `HANDOFF:` comment reconciled; the body describes what was built; ADR re-scored where an edit changed scope, evidence or decisions | | |

- [ ] Adversarial review of the diff against this issue found no substantial finding

- [ ] **Gate — Criteria & sheets.** Every completion criterion names its artifact, location and assertion; every path a criterion names exists at `evidence_commit`, or is created by a named step of § Method / Experimental Design or by a task in `blocked_by`, and the criterion says which (rule 12); every pre-filled criterion not marked N/A has its bracketed rows and every added criterion, P/F pair, contract subsection not marked N/A, threat and open question has a validation row, and no row remains for an N/A subsection or an N/A pre-filled criterion that no other criterion names (a per-item row family with zero items satisfies its bracket); every pickup check refers to something the body contains or that the body marks N/A; nothing in the sheets contradicts § Method / Experimental Design or § Scope Boundary. Adversarial re-read of everything above found no substantial finding.

## Abstract

<!-- Written last, read first. 2–4 sentences: what is wrong or missing,
     why it matters, and the one-line shape of the proposed remedy —
     each drawn from § Intent & Alignment, § Observations, § Hypothesis
     (falsifiable) and § Conclusion & Future Work, and contradicting
     none of them. The terminal gate in § Agentic Delegability covers
     it. -->

## Agentic Delegability (ADR-1)

<!--
  ADR-1 v5. How safely this issue can be handed to an automated executor,
  and what maintenance liability delegating it as-is would create. NOT a
  rating of how good, important or urgent the work is. Two failure modes
  are rated: the work is not finished, and the work is finished in a way
  that leaves a liability behind — the second is the dangerous one, because
  it is invisible to the gate that accepted it.
  Fill at filing; re-score when an amendment changes scope, evidence or
  open decisions. Where two ANCHORS in one axis could apply to one fact,
  take the lower. Caps and deductions stated inside an axis apply on top of
  the anchor chosen; they are not in competition with it.
  Self-contained by design; the other tier templates carry their own
  tier-adjusted copies. They can drift — change an axis in all of them and
  bump the version above together.

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
      1 a person reads prose and agrees (review of prose only)
      0 none; success asserted by whoever did the work, with no enumerated
        procedure or transcript
      A person operating the software and recording enumerated
      observations is scored by the shape of the check (4 or 3), then the
      document deduction below applies.
      For an investigation, score the check behind the rule 3 decision
      criterion (the P/F pairs), not the verdict document.
      DEDUCTIONS, CUMULATIVE, floor 0. Deduct 2 if the same change authors
      both the implementation and the expected values certifying it (rule 11
      defines "the same change" and the independently-derived exemption). Deduct
      1 if the acceptance evidence is a document asserting a measurement.
      `os:` records the score AFTER deductions; `os_deduction:` records the
      total deducted, 0-3.

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
      start to gated evidence. Parallel steps count for less than ordered
      ones: reliability falls with chain length, not with volume.
      5 under an hour   4 one to four hours   3 half a day to two days
      2 several days    1 more than a week, or the chain crosses a subsystem
        it must first learn                    0 a multi-week programme
      Do not count time spent waiting on another issue; that is an ordering
      dependency, recorded in `blocked_by`.

  PD  PRECEDENT DENSITY — is there a worked example of this shape already in
      this repository?
      5 this issue names one and it exists
      4 a near-identical sibling is in-tree and findable by name
      3 analogous patterns exist but need adaptation
      2 the category exists; this is the first instance of its kind
      1 no in-repository precedent; the pattern comes from an external
        specification this issue cites
      0 none named; whoever executes invents the shape
      This predicts duplication: without a local pattern to copy, the work
      reproduces whatever pattern is commonest elsewhere. A young repository
      scores low across the board, which is accurate rather than punitive,
      and self-corrects as each first instance lands.

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
      At 2, note that an expected-output artifact produced by the same
      process that produced the behaviour records it rather than evidencing
      it, and will be cited as ground truth by everything built on it.

  TWO CAPPING AXES. They do not add; they impose a ceiling on the band.

  ED  ENVIRONMENTAL DETERMINISM — can the evidence be produced unattended,
      wherever this project runs its checks (its automation, or a
      maintainer's own checkout if it has none)? Score the evidence the work
      REQUIRES, not only what the completion criteria happen to list.
      5 self-contained: the project's standard check command, or a script
        already in the tree, produces it ......................... no cap
      4 needs a pinned toolchain the project can fetch and reproduce no cap
      3 needs an unreliable substrate, or an external corpus to download
        .......................................................... cap B
      The last three are disjoint on one question: could automation ever
      produce this unattended?
      2 YES, but not with what the executor has — another host platform, a
        device class, or a credential that automation COULD be given ....
        .......................................................... cap C
      1 NO, because something physical must be connected, operated or
        observed by hand against an enumerated procedure, or because the
        evidence is a recording of a real session with no named unattended
        harness ................................................... cap F
        A recorded manual procedure scores 1 unless the issue names the
        in-tree or CI substrate (a headless display run, a device farm) and
        the harness that would produce the same observation unattended —
        then it scores 2, that harness is a regression-test step § Method /
        Experimental Design must name, and the procedure is interim
        evidence; without that step the score stays 1.
      0 NO, because a person's participation or judgement IS the evidence:
        a human-subject trial, an independent reproducer, someone whose
        judgement of what they saw — not an enumerated observation of the
        software's behaviour that anyone holding the apparatus would
        repeat, which is 1 — is the report, an external publisher whose
        acceptance is itself the evidence .......................... cap F
      Reviewing a diff or approving a merge is NOT evidence production and
      does not score here — that is the band-B checkpoint, and most projects
      require it. Running the software on a platform and reporting the
      result IS evidence production and does score here.
      A low score is not a criticism; it means the deliverable is not a
      patch. Much of the work may still be produced unattended, but the
      issue cannot be closed that way.

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
      Score the entries, not the presence of an open-questions section:
      a template that mandates one will have one on every issue. Score an
      unmarked entry on its substance and fix the marker.

  BAND from RAW: 30-35 A | 24-29 B | 17-23 C | 10-16 D | 0-9 F.
  FINAL BAND = the most restrictive of (RAW band, ED cap, DA cap).
  Where the RAW band is C or D while SC>=4, OS>=4, DA>=4 and ED>=3 (no cap
  below B), the low band is driven by blast radius, reversibility, horizon,
  precedent or footprint, not by the specification or the environment: SPECIFY-FIRST and SPLIT presuppose that SC or
  OS is what is low, so record `action: DELEGATE-WITH-CHECKPOINT` and name
  the checkpoint (the review of the published surface, the format
  document, the flag's help text, the release note).
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
    IRREVERSIBLE-PUBLICATION RD<=1          a published or out-of-repo
                                            commitment, wrong and not
                                            cheaply withdrawn
    HOLLOW-ORACLE            OS<=2, or the 2-point deduction fired
    GOLDEN-LOCK-IN           RD=2           an expected artifact this work
                                            commits that later work is then
                                            graded against. HOLLOW-ORACLE
                                            above already took the
                                            self-certified cases
    SPEC-DRIFT               SC<=2
    PREMATURE-SEAM           DA<=2
    PARTIAL-INTEGRATION      BR<=1 or CF<=1
    SCOPE-EXHAUSTION         HL<=1
    DUPLICATION              PD<=1
    NONE                     nothing above fired
  Where an axis is low because of what this tier inherently is rather than
  because of this issue, its tag reports the tier and not the issue: take
  the next tag whose trigger reflects this issue specifically.

  A low band is a routing decision, not a criticism. Issues can be excellent
  work and band F because closing them needs a person or a device.

  REVIEW GATE — THE TERMINAL GATE OF RULE 14. The two boxes below cover
  the ENTIRE body, § Abstract included: an issue with both ticked has been
  read adversarially and by a peer, end to end, and neither read left
  anything substantial outstanding. The section gates above record that
  each span was re-read when reached; only these two boxes assert that the
  finished issue is consistent. Tick them yourself; the filer reviewing
  their own issue is fine.
    - SUBSTANTIAL means acting on the finding would change a score above, a
      section's mandate or a section's conformance to its mandate as its
      comment block states it, a completion criterion, a prediction, an
      edge, or the scope. Wording is not.
    - Unticked means not yet reviewed. It is not a defect and does not block
      filing; it means the band above, and the body, are still unvalidated.
    - Where a review comment exists, point `review_evidence` at it.
    - `false` is set — both boxes unticked, `review_evidence` pointing at
      the comment naming the substantial finding — by whoever finds one
      and does not fix it, as bookkeeping, on an OPEN issue; the AMENDED
      (REPLAN) that fixes the finding, or a comment recording why it does
      not stand, re-ticks. § Pickup Checks reads it. On a closed issue the
      boxes and `review_clean` stand as filed; the finding is recorded by
      the after-close AMENDED (REPLAN) of task rule 9 whether or not the
      finder fixes it.
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
     issue, where it would go wrong, and the single change that would raise
     the band. Name the section, the criterion, the artifact. -->
