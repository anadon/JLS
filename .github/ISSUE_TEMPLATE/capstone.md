---
name: Capstone
about: A milestone-level outcome gated on a required set of features — owns the system-level acceptance evidence that the features jointly deliver it
labels: ["tier:capstone"]
---

<!--
  Template: capstone v5 (2026-09)

  TIER MODEL — task → feature → capstone; the full edge-legality
  matrix is in the feature template and applies unchanged. The
  capstone-specific consequence:
    - Nested capstones: list a sub-capstone in requires_capstones (its
      whole outcome gates this one; the DAG rule covers the composition
      edge). Do not enumerate a sub-capstone's features in place of the
      sub-capstone (no composition edge: the parent could close first);
      one this capstone's criteria, risks or rule G name directly is
      listed in requires_features as well, that use being its rule E
      contribution.

  RULES — the scientific-task template's rules 1–7 apply adapted to
  this tier (evidence at a pinned commit; no padding; observable
  claims; atomic scope; section-NAME citations resolved against THIS
  template's headings for same-body references and against the cited
  issue's template otherwise — headings carry no numbers; executor
  re-verification, here § Pickup Checks; explicit labels, here
  `tier:capstone` plus `bug` or `enhancement` matching the corpus).
  Task rules 9–15 and feature rules A–D apply with the feature
  template's substitution table read one tier up:

    feature tier                          | this tier
    --------------------------------------|-------------------------------
    serving capstone (`requires_features`)| parent capstone
                                          |   (`requires_capstones`; no
                                          |   serves field — roster search)
    child task (`requires_tasks`)         | required feature or
                                          |   sub-capstone (`requires_*`)
    `planned_tasks`                       | `planned_features`
    § Feature-Level Interface & Data      | § Cross-Feature Integration
      Contract, § Global Invariants,      |   Risks, § System-Level
      § Integration Criteria & Evidence   |   Acceptance Criteria (its
      Plan (its Integration criterion     |   Acceptance criterion rows)
      rows)                               |
    final commit                          | the acceptance commit
    § Decomposition & Rationale           | § Required Feature Set &
                                          |   Sufficiency
    § Capability Statement & Scope        | § Outcome Statement,
      Boundary, § Sequencing &            |   § Cross-Feature Integration
      Parallelism                         |   Risks
    integration step                      | acceptance step (§ Outcome
                                          |   Statement walk-through)
    § Post-Integration Validation         | § Post-Acceptance Validation
    rule B (not a folder)                 | rule F below
    § Re-planning Protocol dispositions   | § Re-planning Protocol, rule G

  A required feature's REPLAN disclaiming an item assigned to it — its
  boundary excludes it or a § Global Invariants entry forbids it — is
  answered here by a REPLAN re-owning the item (feature rule C). In
  addition:

  E. The required set is a closed list with a sufficiency argument
     (§ Required Feature Set & Sufficiency): why exactly these
     features, together, make § Outcome Statement true — and why none
     is removable.
  F. A capstone must assert system-level acceptance criteria
     (§ System-Level Acceptance Criteria) that no single feature's
     completion criteria cover. If every criterion is already owned by
     a feature, this is a milestone label, not a capstone — do not
     file it.
  G. Orphaned scope. When a required entry (feature or sub-capstone)
     closes, is re-tiered (a new issue plus a REPLAN on the old one),
     REPLANs a child away, is descoped, or has its landed work reverted,
     or its close-out's cited landing found not to deliver, after close
     (the task rule 9 notices the roster pickup row and the REPLAN row
     read), while leaving scope this capstone still needs, the
     REPLAN must give that scope a disposition: (a) re-home it — assign
     the task or planned scope to an OPEN required feature by this
     REPLAN, posted on it led by this number (feature rule C), which
     adopts it into its `requires_tasks` or `planned_tasks` by its own
     REPLAN (feature rule D; a closed feature
     is never reopened — use (b)); (b) file a new feature to
     host it, or adopt the feature already hosting it (a re-parented
     feature or sub-capstone names this capstone; a sub-capstone is
     added to `requires_capstones` instead), and add that feature to
     requires_features;
     or (c) descope it, re-deriving the § Required Feature Set &
     Sufficiency argument.

  These comments do not render on GitHub — leave them in place for the
  next reader of the raw issue body.
-->

## Intent & Alignment

<!-- Written FIRST; everything below is checked against it at every
     gate.

     Intent: what becomes true of the project when this capstone lands,
     in one paragraph phrased so a reader can later say whether it
     happened. Not the walk-through — that is § Outcome Statement.

     Alignment: what this capstone serves — a parent capstone's
     § Outcome Statement it is part of (requires_capstones on the parent
     side), a roadmap or release commitment, a standing project goal —
     cited by issue number and section name where one exists. When
     the parent is not yet filed, write "parent capstone unfiled —
     <one-line scope>" and replace it once it exists (bookkeeping,
     rule C). A capstone aligned with nothing has no beneficiary and
     does not belong on the backlog (task rule 2). -->

### User impact

<!-- The audiences for whom the project is materially different once
     this capstone lands — named concretely, with the change they
     experience at the system level, not per-feature. Pick and name:
       - Students drawing and simulating circuits in the editor.
       - Instructors authoring, grading, or auto-checking work in batch
         (`-b`) mode.
       - Circuit-file authors and third-party tools that read or write
         `.jls` files against the save format's authoritative definition.
       - Packagers and distributors shipping the installers and container.
     Effects on the codebase and the project's commitments belong in
     § Code & Project Impact and Consequences, below the required set
     that determines them. -->

## Status & Required Features

<!-- The machine block is the source of truth for the edges it can
     express (rule A). It is filled PROGRESSIVELY, the gates say when:
       - `evidence_commit` now: the SHA every roster and acceptance
         claim is pinned to.
       - `requires_features`, `requires_capstones`, `planned_features`
         at the gate after § Required Feature Set & Sufficiency.
       - `blocked_by`, `related` and the mermaid graph at the gate after
         § System-Level Acceptance Criteria (which is grouped with
         § Cross-Feature Integration Risks).
     -->

```yaml
tier: capstone
evidence_commit:        # SHA the roster and acceptance claims are pinned to
requires_features: []   # composition — the closed required set (rule E), FILED, not yet landed when listed
requires_capstones: []  # composition — sub-capstones (nesting; see the tier-model note)
planned_features: []    # one-line scopes for required features not yet filed; verify each
                        #   scope is ABSENT at evidence_commit before listing it; resolve
                        #   each to its number when filed (rule C). Non-empty: G19
                        #   warns; the board derives Blocked.
blocked_by: []          # ordering: capstones or features that must land before this
                        #   capstone closes, beyond the required set. Never tasks.
blocks: []              # mirrors only — the counterpart's blocked_by is authoritative:
                        #   capstones whose blocked_by names this one. Left [] at
                        #   filing; G09 reports drift.
related: []             # reference only — never blocking, never ownership
```

```mermaid
flowchart TD
  %% Arrow A --> B means "A must land before B" (A blocks B).
  %% Show this capstone, its required features, and the ordering
  %% edges among those features (including edges to features or
  %% capstones outside this set). Regenerate on every REPLAN.
```

- [ ] **Gate — Intent & Status.** § Intent & Alignment states an outcome a reader could later confirm or deny; each alignment target was opened at its current revision and its cited section needs what this capstone claims to deliver, or the target is marked unfiled with a one-line scope, or it is a standing commitment stated here that a maintainer would uphold and from which the intent follows; every audience named is one the system-level change reaches. `evidence_commit` is the SHA actually checked out and is reachable from the default branch. Adversarial re-read of this span found no substantial finding.

## Outcome Statement

<!-- What becomes true of the project when this capstone lands, phrased
     as an observation: the demo script, command sequence, or
     acceptance walk-through a reviewer (or agent) executes to see it.
     "Do X, observe Y" at the system level. -->

- [ ] **Gate — Outcome.** The walk-through is executable step by step by someone with only the repository, the named platform and the external services the steps name; each step states what is observed; it is the intent of § Intent & Alignment and not a wider one; every audience in § User impact is reached by some observed step. Adversarial re-read of everything above found no substantial finding.

## Required Feature Set & Sufficiency

<!-- One row per required feature (and per sub-capstone), filed or
     planned, then the sufficiency argument (rule E): why this set
     jointly delivers § Outcome Statement, and per feature, what breaks
     in the walk-through if it were removed — the minimality check.
     Write each contribution against the feature's own § Capability
     Statement & Scope Boundary (a sub-capstone: its § Outcome
     Statement, resolved against its own template's headings — task
     rule 5) as it reads NOW; a feature
     whose boundary disclaims the contribution claimed here is a plan
     defect. Work already landed when this capstone lists it (at filing
     or by an adopting REPLAN) is a precondition cited here by permalink,
     never a roster entry. Fill
     `requires_features`, `requires_capstones` and
     `planned_features` now, and record the DAG walk. -->

| Feature | Contribution to the outcome | Status |
|---------|-----------------------------|--------|
| #       |                             |        |

- [ ] **Gate — Sufficiency.** Every FILED required entry's body was read at its current revision and its § Capability Statement & Scope Boundary (a sub-capstone: its § Outcome Statement, resolved against its own template's headings — task rule 5) supplies the contribution claimed; every row names the walk-through step that breaks without it, or, for a sub-capstone's feature listed under the tier-model note, the criterion, risk or rule G item that names it; the set, together with the landed preconditions cited, covers every step of § Outcome Statement; every planned scope is verified absent at `evidence_commit`; `requires_features`, `requires_capstones`, `planned_features` are filled, every filed entry is of the tier its key requires and had not landed when listed (checked at the `evidence_commit` of the revision that listed it), and the DAG walk for the composition edges is recorded. Adversarial re-read of everything above found no substantial finding.

## Cross-Feature Integration Risks

<!-- Where the required features touch: shared interfaces (cite each
     feature's § Feature-Level Interface & Data Contract by section
     name), ordering hazards, contract handoffs that cross feature
     boundaries, and the threats to validity that only appear at system
     scale — per-feature evidence that shortcuts the integrated code
     path, platform divergence, invariants that hold per-feature but not
     jointly, and every external service or platform § Outcome Statement
     names. Each risk names its mitigation — a criterion in
     § System-Level Acceptance Criteria (written together with this
     section), an ordering edge, a feature's own invariant — or is
     explicitly accepted. Every necessary ordering is an edge: fill
     `blocked_by` and the mermaid graph at this group's gate. -->

## System-Level Acceptance Criteria

<!-- Predictions spanning multiple features: do X, observe Y — each one
     not covered by any single feature's completion criteria (rule F).
     Name the end-to-end test, golden artifact, or recorded procedure
     that pins each, and which feature, `blocked_by` predecessor, or
     this issue's close-out builds the ones that do not exist yet (task
     rule 12). For every
     golden artifact or expected value: who produced it, when, and
     whether it was pre-committed, independently derived, or will be
     produced by the system it certifies — any required feature's, or its
     child's, code counts (task rule 11). A threshold or count also
     names the substrate it was set against and the run count its do-X
     repeats; the Acceptance criterion row and the Oracle custody row
     carry both (task rule 3's loop clause). Together they pin every
     step of § Outcome Statement and every risk mitigation assigned
     here.

     EVERY CRITERION NAMES ITS NEXT MOVE ON FAILURE with every required
     entry landed: a fix feature under rule G(b) adopted by REPLAN, or
     `REFUTED:` — the premise of § Outcome Statement fails — quoting the
     failing criterion with command and output (the close-out record,
     task rule 10; § Re-planning Protocol's close clause runs first).

     A "spans #A, #B" ANNOTATION IS NOT EVIDENCE: state what each
     contributes, read from its § Integration Criteria & Evidence Plan
     and § Completion Criteria (Definition of Done) as they read now. If
     one of them already asserts the whole thing, say "covered alone by
     #A" — honest, and it simply does not count toward rule F — or "no
     feature; built by this capstone's close-out".

     A criterion nothing covers is marked UNOWNED. -->

- [ ] **Gate — Risks, acceptance & edges.** Every shared interface cites both features' contracts by section name and each FILED feature's § Feature-Level Interface & Data Contract declares its side (a planned feature's side is confirmed at resolution, rule C); every ordering hazard is an edge in the waiting feature's machine block (assigned by a REPLAN posted on it, rule C; an edge involving a planned feature is recorded here and carried into the machine blocks at its filing, or by the REPLAN resolving the scope to a feature filed without it — into its own `blocked_by`, or, where a filed feature waits on it, by the REPLAN posted on that feature, feature rule C) or, where this capstone itself waits, in this block, with no edge pointing at a task; `blocked_by` and `related` are filled, `blocks` is empty or carries only mirrors, and the DAG walk for the edges added here is recorded; the mermaid graph agrees with the machine block (rule A); every risk names a mitigation — a criterion here, an edge, a feature's invariant — or is accepted with a reason; every external service or platform § Outcome Statement names is a risk here. Every acceptance criterion carries exactly one ownership annotation written against each FILED feature's actual text at its current revision (a planned feature's side and annotation are written against its planned scope and confirmed by the rule C resolution comment, otherwise REPLAN) and names its next move on failure; at least one is a genuine span or close-out criterion (rule F); every step of § Outcome Statement is pinned by some criterion; every risk mitigation assigned to this section exists here; no span names a feature outside the required set; no owner's § Capability Statement & Scope Boundary or § Global Invariants disclaims or forbids what is assigned to it; every artifact named exists at `evidence_commit` or has a named builder (task rule 12); every expected value names its custodian, date and provenance (task rule 11). Adversarial re-read of everything above found no substantial finding.

## Code & Project Impact and Consequences

<!-- What changes for contributors, maintainers, LLM agents working the
     codebase, and the project's own commitments once the outcome
     holds, now that the required set and the acceptance criteria fix
     what moves: published
     surfaces and formats, release or packaging commitments, invariants
     that the whole system must now respect, maintenance cost taken on
     or retired. State consequences as well as benefits — what becomes
     harder, what this forecloses. A consequence too costly to accept is
     a reason to re-scope § Outcome Statement (REPLAN, old and new
     quoted). -->

## Re-planning Protocol

<!-- What invalidates this plan and the required response: a required
     feature or sub-capstone descoped, refuted or REPLANned → re-derive
     § Required Feature Set & Sufficiency and apply rule G to orphaned
     scope; a feature's contract deviates → reassess § Cross-Feature
     Integration Risks and § System-Level Acceptance Criteria (its
     mirrored REPLAN is the trigger); an acceptance criterion fails with
     every required entry landed → its named next move (§ System-Level
     Acceptance Criteria; a criterion, risk or walk-through step found
     wrong, the required entries being right → corrected by REPLAN with
     the evidence, task rule 2, no next move fired); the outcome itself
     is re-scoped → REPLAN
     with the old and new § Outcome Statement both quoted, then § Intent
     & Alignment re-checked and § Required Feature Set & Sufficiency and
     § System-Level Acceptance Criteria re-derived, each released entry
     whose scope an OPEN parent capstone still needs named unmet for that
     parent's rule G REPLAN (a notice here, feature rule C); a parent
     capstone releasing this one, or the commitment § Intent & Alignment
     cites withdrawn → whether this capstone still has a beneficiary
     (none: the answering REPLAN gives every required and planned entry
     its disposition below, posted on each filed one — rule C — and
     closes this capstone under task rule 10, being its close-out
     record); a close on `REFUTED:` or `SUPERSEDED:` with required
     entries still open or planned → a REPLAN giving each its
     disposition below first, cited by the close-out; a required
     feature's mirrored WAIVED → the waived obligation checked against
     § System-Level Acceptance
     Criteria, and a successor outside the required set is rule G scope;
     a re-scope after which no criterion in § System-Level Acceptance
     Criteria is a genuine span or close-out criterion (rule F) → once
     the remaining required entries have landed, close with
     `SUPERSEDED: — label, not a capstone (rule F); every criterion
     covered alone by #…`, mirrored
     to parent capstones, which re-derive their sufficiency citing the
     landed work by permalink as preconditions. Dispositions for a
     removed required entry: re-parented to a named open capstone,
     released (it re-checks its beneficiary — a parent it was named
     unmet for is one), or, already closed, its close-out cited (feature
     rule C); for a planned one: moved
     to a named
     open capstone's `planned_features`, or dropped with the sufficiency
     argument re-derived (rule G(c)). -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself. For each: the
     question, options with a recommended default, and whether it blocks
     filing features, blocks
     acceptance, or can ride along. "N/A — fully specified" if nothing
     is open.

     Mark every entry per task rule 13 — `Recommended default:`,
     `PROPOSED:`, `BLOCKING:` or `HYGIENE:`. -->

- [ ] **Gate — Consequences, re-planning & decisions.** Every published surface or commitment the outcome moves has its consequence stated; every cost identified is stated or the section says there is none and why; none was accepted that § Intent & Alignment would not justify. Every trigger named in § Re-planning Protocol has a response ending in a REPLAN comment where anything cited changed, naming the sections it re-derives; the protocol covers every entry of the required set; every open question carries exactly one task rule 13 marker; every decision the acceptance pass will hit is listed or settled above; nothing marked `Recommended default:` contradicts a criterion or a risk mitigation. Adversarial re-read of everything above found no substantial finding.

## Completion Criteria (Definition of Done)

<!-- WHAT must be true when this capstone closes — not how it is checked
     (§ Post-Acceptance Validation) and not what is checked before the
     acceptance pass starts (§ Pickup Checks). Every box names the
     artifact, its location and the assertion that pins it; each
     pre-filled box names in brackets the row(s) of § Post-Acceptance
     Validation that verify it, and each added criterion gets a row of
     its own. -->

- [ ] Every entry in `requires_features` and `requires_capstones` closed as landed, or removed via a `REPLAN:` comment with the sufficiency argument re-derived for the reduced set; `planned_features` empty (each resolved to a filed issue or descoped) [rows: Required, Roster]
- [ ] Every criterion in § System-Level Acceptance Criteria holds end-to-end at a named commit [rows: Acceptance criterion]
- [ ] The § Outcome Statement walk-through succeeds at that commit [rows: Walk-through step]
- [ ] Every risk in § Cross-Feature Integration Risks is mitigated as stated, checked at system scale, or its acceptance re-confirmed at the acceptance commit [rows: Risk]
- [ ] Every expected value the acceptance evidence compares against was pre-committed or independently derived, or the ADR-1 OS deduction was applied (task rule 11) [row: Oracle custody]
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A) [row: Roster]
- [ ] Landing reported with a `STATUS: landed` comment on every OPEN capstone whose `requires_capstones` lists this one (rule D) [row: Mirrors]
- [ ] Every decision in § Open Questions & Decisions Needed is resolved or explicitly deferred, none left blocking [rows: Open question]
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10) [row: Waivers]
- [ ] Every cited evidence document resolves on the default branch at close and every permalink is commit-locked and resolves [row: Links]
- [ ] Every artifact named above exists at `evidence_commit` or is created by its named builder — this list says which (task rule 12) [row: Paths]
- [ ] § Agentic Delegability re-scored on any `REPLAN:` edit that changed the roster, the acceptance criteria, or the open decisions [row: Re-plans]
- [ ] ... [row: <added below>]

## Pickup Checks

<!-- Run once per executor by whoever begins the acceptance pass (task rule 6,
     applied at this tier). The rows are fixed and never deleted: a row
     whose referent is N/A or empty is recorded "none" in the
     `STATUS: pickup` comment here (not mirrored; one line may list every
     "none" row) and neither passes nor fails; that comment names the
     checkout commit and lists the rows run and the rows not reached. A
     failed row
     ends in that comment naming the acceptance steps blocked (or all of
     them) and what unblocks them; only those steps wait. -->

- [ ] `action` read and followed per the task template's `action` pickup row (at this tier a SPLIT is a REPLAN plus a new issue)
- [ ] Every `REPLAN:` that edited this body read (own, or a counterpart's posted here led by its number); each names the sections changed and re-read, and the body matches their fold in stream order, bookkeeping edits aside — a mismatch is repaired by re-applying the fold from the comments and the body's edit history before proceeding; where `review_clean` is `false`, the finding at `review_evidence` is answered before the acceptance pass begins — by the REPLAN fixing it, or by a comment quoting it and recording why it does not stand
- [ ] Every entry in `requires_features` and `requires_capstones` has a `STATUS: landed` comment mirrored here or on the entry (rule D), and no later `STATUS: landed`, `REPLAN:` or `AMENDED:` notice on the entry qualifies what it landed without a REPLAN here reconciling it or naming the successor it tracks, or its disposition is recorded; `planned_features` is empty
- [ ] Every `REPLAN:` or `WAIVED:` from a required feature or sub-capstone read (mirrored here or on the entry — rule D; an entry filed from a template that does not mirror is read on the entry throughout), each REPLAN checked for contract deviations (recorded only by REPLAN, feature rule C); § Cross-Feature Integration Risks, § System-Level Acceptance Criteria and § Required Feature Set & Sufficiency reassessed by REPLAN where anything they cite changed
- [ ] Every OPEN capstone whose `requires_capstones` lists this one located (roster search) — these receive the mirrored comments of rule D; each parent cited in § Intent & Alignment still lists it, or its REPLAN dropping it was read and § Intent & Alignment re-checked for a remaining beneficiary
- [ ] Not superseded: the § Outcome Statement walk-through does not already succeed at the checkout for reasons outside this plan (the required set's own landings do not count) — where it does, close with `SUPERSEDED:` citing the landing (task rule 6; § Re-planning Protocol's close clause runs first)
- [ ] Every `blocked_by` entry has landed, or the edge was removed by a `REPLAN:` comment with a Dropped/Retired ledger entry
- [ ] Every `BLOCKING:` entry in § Open Questions & Decisions Needed is answered; `PROPOSED:` entries noted as draft-only
- [ ] `evidence_commit` re-pinned and roster claims re-derived if HEAD has moved
- [ ] Every artifact a criterion names exists, or its named builder has landed, or its builder is this capstone's close-out and is scheduled (task rule 12)
- [ ] Every platform, device or apparatus the walk-through or a criterion names, and write access to each issue the pass or its close-out posts on (rule D mirrors, rule C postings, a finder's REPLAN or AMENDED under task rule 9), is available to whoever picks up, or the `STATUS: pickup` comment names the acceptance steps or postings blocked and the holder they are handed to, or that no holder is yet known (those steps stay blocked; a row a held posting leaves unfillable is WITHHELD held by that holder, task rule 2; a holder's or custodian's re-run: command and output in a `STATUS: progress` citing the pickup, as the task observations row says); every other step may proceed

## Post-Acceptance Validation

<!-- Run at close, at one named commit. One row per item; rows are
     enumerated AT FILING (a row per walk-through step, per acceptance
     criterion, per risk, per required entry, per open question, per
     added completion criterion) with evidence cells empty; the rows of
     pre-filled criteria marked N/A are deleted at filing unless another
     criterion names the row. Evidence is a transcript, a command with its
     output, a test name at the commit, a permalink, or a comment link —
     never "done". A row whose evidence exists but may not yet be
     disclosed is a task rule 2 WITHHELD, never a blank; one whose step
     has not run leaves its criterion unmet (task rule 10); one whose
     referent a REPLAN retired cites it. -->

| Item | Check | Evidence | Result |
|------|-------|----------|--------|
| Walk-through step 1 | executed at the commit; observation matches § Outcome Statement | | |
| Acceptance criterion 1 | do X end-to-end, observe Y; each spanning feature's contribution shown; a threshold or count: run count and substrate | | |
| Risk 1 | mitigation in place at system scale, or the acceptance re-confirmed at the acceptance commit | | |
| Required #A | landed — mirrored here or the landing comment on the entry (rule D), or removed — the REPLAN and its disposition cited; later `STATUS: landed`, `AMENDED:` or `REPLAN:` notices on the entry re-read at close; deviations reconciled | | |
| Open question 1 | resolving comment, or permalink to the diff landing the `Recommended default:` / the re-derived `HYGIENE:` item | | |
| Oracle custody | each expected value: who, when, pre-committed / independently derived / produced by the system, or of unshowable derivation, and deducted; a threshold or count: its substrate and run count | | |
| Roster | machine block, table and mermaid agree; `planned_features` empty | | |
| Mirrors | `STATUS: landed` posted on every open capstone whose `requires_capstones` lists this one, cited; a posting handed to a holder (the platform row): WITHHELD held by them, the close not waiting on it, the cell filled when they post (bookkeeping) | | |
| Links | every permalink commit-locked, resolving and at a commit reachable from the default branch; every document on the default branch | | |
| Paths | each artifact a criterion names exists at the acceptance commit, created by its named builder, or is absent where the criterion names the step that removes it (task rule 12) | | |
| Waivers | every skipped criterion has its `WAIVED:` comment, or after close the finder's AMENDED (REPLAN) standing in for it (task rule 10) | | |
| Re-plans | every `REPLAN:` comment reconciled; the body describes what was delivered; ADR re-scored where a re-plan changed roster, criteria or open decisions | | |

- [ ] Adversarial review of the delivered system against this issue found no substantial finding

- [ ] **Gate — Criteria & sheets.** Every completion criterion names its artifact, location and assertion; every path a criterion names exists at `evidence_commit` or has a named builder (task rule 12); every pre-filled criterion not marked N/A has its bracketed rows (a per-item row family with zero items satisfies its bracket) and every added criterion, walk-through step, acceptance criterion, risk, required entry and open question has a validation row, and no row remains for an N/A pre-filled criterion that no other criterion names; nothing in the sheets contradicts § Required Feature Set & Sufficiency. Adversarial re-read of everything above found no substantial finding.

## Abstract

<!-- Written last, read first. 2–4 sentences: the outcome, why it
     matters, and the one-line shape of the feature set that gates it —
     drawn from § Intent & Alignment, § Outcome Statement and § Required
     Feature Set & Sufficiency, contradicting none of them. -->

## Agentic Delegability (ADR-1)

<!--
  ADR-1 v5, outcome tier. How safely this capstone can be handed to an
  automated executor, and what maintenance liability delegating it as-is
  would create.
  SCORE THE ACCEPTANCE PASS — this capstone's own system-level criteria and
  outcome statement — never the union of its features.
  Fill at filing. Where two ANCHORS in one axis could apply to one fact,
  take the lower. Caps and deductions stated inside
  an axis apply on top of the anchor chosen; they are not in competition
  with it.

  SEVEN ADDITIVE AXES, 0-5 each. RAW = their sum, 0-35.

  SC  SPECIFICATION CLOSURE — is the end state fixed by the text alone?
      Test: could two competent implementers each satisfy the acceptance criteria and
      produce artifacts differing in a way a reviewer would care about?
      5 every acceptance criterion names the artifact, its location, and the
        assertion that pins it
      4 concrete; one or two open naming choices no reviewer would litigate
      3 the goal is unambiguous, the artifact's shape is not
      2 stated as an outcome rather than as an artifact
      1 a problem and a direction; "done" is not written down
      0 open-ended, or deferring its own definition
      A criterion reading "documented", "considered" or "reviewed" with no
      named artifact caps this at 3.

  OS  ORACLE STRENGTH — the check that would actually gate the merge.
      5 exact comparison against an expected artifact spanning features that
        pre-exists the implementation or is independently derived (task
        rule 11); an end-to-end algebraic property; or a differential check
        against an independent implementation
      4 behavioural assertions over enumerated cases including the named
        failure cases; or a compiler, type or analysis gate THIS CHANGE
        WOULD FAIL BEFORE THE FIX (a standing project-wide gate every change
        already passes is not this issue's oracle)
      3 existence- or smoke-shaped: produced, exits zero, non-empty
      2 a threshold or a count, no behavioural content
      1 a person reads prose and agrees
      0 none; success asserted by whoever did the work, with no enumerated
        procedure or transcript
      DEDUCTIONS, CUMULATIVE, floor 0. Deduct 2 if the expected values
      certifying the acceptance were produced by running the system as
      § System-Level Acceptance Criteria defines it, or their derivation
      cannot be shown (task rule 11; the independently-derived exemption
      stands). Deduct
      1 if the acceptance evidence is a document asserting a measurement.

  BR  BLAST RADIUS — everything this capstone's own acceptance work must move, generated files,
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
      Measured as if the roster had landed: do not count time spent waiting
      for features.

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
      REQUIRES, not only what the acceptance criteria happen to list.
      5 self-contained: the project's standard check command, or a script
        already in the tree, produces it ......................... no cap
      4 needs a pinned toolchain the project can fetch and reproduce no cap
      3 needs an unreliable substrate, or an external corpus to download
        .......................................................... cap B
      2 YES, but not with what the executor has — another host platform, a
        device class, a credential that automation COULD be given, or a
        fixture only a named custodian holds, withheld until a disclosure
        event or never disclosable (task rule 2) ................. cap C
      1 NO, because something physical must be connected, operated or
        observed by hand against an enumerated procedure, or because the
        evidence is a recording of a real session with no named unattended
        harness ................................................... cap F
        A recorded manual procedure (one a person performs by hand — a
        command run in a shell with its output pasted is not one) scores 1
        unless the issue names the
        in-tree or CI substrate (a headless display run, a device farm) and
        the harness that would produce the same observation unattended —
        then it scores 2 and that harness is a named builder.
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

  `roster_delegable` — k/n over every FILED entry in the composition
  roster (`requires_features` and `requires_capstones`); an entry counts
  as delegable when its own block records band A or B. Unfiled planned entries are
  not counted; where any exist the fraction is provisional and the prose
  line must say so. Score `pending` when the roster is empty or its
  children are not yet scored, and name in the prose line which entry is
  closest to delegable.

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
  Where an axis is low because of what this tier inherently is rather than
  because of this issue, its tag reports the tier and not the issue: take
  the next tag whose trigger reflects this issue specifically.

  A low band is a routing decision, not a criticism.

  REVIEW GATE — THE TERMINAL GATE OF TASK RULE 14. The two boxes below cover
  the ENTIRE body, § Abstract included: an issue with both ticked has been
  read adversarially and by a peer, end to end, and neither read left
  anything substantial outstanding. Tick them yourself; the filer reviewing
  their own issue is fine, and so is an executor reviewing before the
  first step — that is review, not execution (task rule 14).
    - SUBSTANTIAL means acting on the finding would change a score above, a
      section's mandate or a section's conformance to its mandate as its
      comment block states it, a completion criterion, a prediction, an
      edge, or the scope. Wording is not.
    - `false` means the review comment at `review_evidence` names a
      substantial finding nobody has fixed; whoever finds one and does
      not fix it sets it (bookkeeping), and the REPLAN fixing it, or a
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
roster_delegable: # k/n over requires_features + requires_capstones (filed), or `pending`
```

- [ ] Adversarial review of this issue found no substantial finding
- [ ] Peer review of this issue found no substantial finding

<!-- One or two sentences: if ED<=1, which criterion needs a person or a
     device and what can still be produced unattended for it; the
     checkpoint where `action` is DELEGATE-WITH-CHECKPOINT; and which
     feature is closest to yielding a delegable leaf. -->
