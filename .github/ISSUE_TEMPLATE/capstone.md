---
name: Capstone
about: A milestone-level outcome gated on a required set of features — owns the system-level acceptance evidence that the features jointly deliver it
labels: ["tier:capstone"]
---

<!--
  Template: capstone v5 (2026-09)

  TIER MODEL — task → feature → capstone; the full edge-legality
  matrix is in the feature template and applies unchanged (`related`
  is reference-only and may point at any tier; tier identity is the
  machine block's `tier:` key, labels are mirrors). The
  capstone-specific consequences:
    - Capstones COMPOSE features (requires_features) and SUB-CAPSTONES
      (requires_capstones), and ORDER against capstones and features
      (blocked_by / blocks). Both directions are downward or
      same-tier; no edge from a capstone ever points at a task, and
      nothing upward exists above this tier.
    - Capstones never reference TASKS. A task may be shared by any
      number of features, so scope that seems to need a direct
      capstone→task edge is listed in an additional feature's roster
      instead. Rule G covers scope that no feature can host at all.
    - Nested capstones: list a sub-capstone in requires_capstones (its
      whole outcome gates this one; the DAG rule covers the composition
      edge). A parent that also needs one of the sub-capstone's features
      individually lists that feature in its own requires_features as
      well. Do not enumerate a sub-capstone's features in place of the
      sub-capstone: that form has no composition edge, so the parent
      could close before the sub-capstone's acceptance pass, and its
      spanning criteria have no place in this issue's ownership
      vocabulary.
    - Ordering between capstones is expressed DIRECTLY:
      capstone-to-capstone blocked_by / blocks is legal. Use
      requires_capstones when a sub-capstone's whole outcome is PART OF
      this one, and blocked_by when it merely must land first.
    - The ordering graph (defined in the feature template: blocked_by/
      blocks plus composition edges read child-before-parent) must
      stay a DAG. Before adding a capstone or feature to the required
      set, walk its machine-block edges outward and confirm no path
      returns to this capstone; record the walk in the filing or
      REPLAN comment.
    - Ordering edges touching this capstone are recorded on BOTH
      sides: when another capstone declares blocked_by this one,
      mirror it in `blocks` below.

  RULES — the scientific-task template's rules 1–7 apply adapted to
  this tier (evidence at a pinned commit; no padding; observable
  claims; atomic scope; section-NAME citations resolved against THIS
  template's headings for same-body references and against the cited
  issue's template otherwise — headings carry no numbers; executor
  re-verification, here § Pickup Checks; explicit labels, here
  `tier:capstone` plus `bug` or `enhancement` matching the corpus),
  together with task rules 9–10 (comment protocol and waivers, with
  `REPLAN:` in place of `AMENDED:` and the same `STATUS:` sub-tags;
  `HANDOFF: transfer` applies unchanged when the acceptance pass changes
  hands — body unchanged, no gate reset, the transferee re-runs § Pickup
  Checks in full and posts its own `STATUS: pickup` citing it; a split
  is a REPLAN plus a new issue at this tier, a re-tier a REPLAN plus a
  new issue at the new tier — that REPLAN carries the Dropped/Retired
  ledger moving every item the new tier can hold to it, gives every
  roster entry it cannot hold a disposition per § Re-planning Protocol
  and rule G, and is the close-out record, task rule 10 read with REPLAN
  in place of `HANDOFF: re-tier`),
  task rules 11–13 (oracle custody, artifact paths resolve at filing,
  marked open questions — applied to § System-Level Acceptance
  Criteria and § Open Questions & Decisions Needed), task rules
  14–15 (dependency order with consistency gates; three check-sheets
  by when they run — gates at filing and at every REPLAN touching
  their span, § Pickup Checks before the acceptance pass begins,
  § Post-Acceptance Validation at close), and feature rules A–D read
  against this template: the machine block below, in § Status &
  Required Features, is the source of truth for the edges it can
  express (A); the not-a-folder test is rule F below (B); living
  body, plan changes REPLAN-logged, gates reset and re-run as task
  rule 14 says with the REPLAN comment naming the gates re-run,
  bookkeeping exempt as rule C's full list says — among others ticking
  a gate or review box, filling a check-sheet's evidence and result
  cells (the `STATUS: landed` comment links the body revision holding
  them), mirror
  `blocks` entries citing the
  counterpart's comment, re-pinning evidence_commit (bookkeeping only
  when every cited line re-derives at the new commit, otherwise part of
  the REPLAN), flipping a roster
  Status cell, and resolving a planned_features scope to its number or
  replacing an "unfiled" alignment with its citation provided the
  edit's comment states the filed issue's cited sections were read and
  agree with what was written against the scope — the filed feature's
  § Capability Statement & Scope Boundary supplies its contribution row,
  its § Feature-Level Interface & Data Contract declares each shared
  interface § Cross-Feature Integration Risks assigns it, and its
  § Integration Criteria & Evidence Plan and § Completion Criteria
  (Definition of Done) leave every § System-Level Acceptance Criteria
  annotation naming it correct — no span it covers alone, nothing it
  disclaims (otherwise REPLAN) (C); state reconstructed from prefixed comments, never from
  checkboxes: required features and sub-capstones mirror every
  prefixed comment of theirs except `STATUS: pickup`/`progress` here,
  led by their number; a roster that adopts an entry by REPLAN links in
  that REPLAN the entry's prefixed comments posted before the adoption
  (landed work is never adopted — see § Required Feature Set &
  Sufficiency); THIS capstone
  mirrors every prefixed comment of its own except
  `STATUS: pickup`/`progress`, led by this number, on every capstone
  whose `requires_capstones` lists it and is open (roster search — there is no
  serves field at this tier), and posts a REPLAN that removes an entry
  from `requires_features` or `requires_capstones`, led by this number,
  on the removed issue as well, and a REPLAN that adds or changes an
  artifact, risk mitigation or ordering edge § System-Level Acceptance
  Criteria or § Cross-Feature Integration Risks assigns to a required
  feature, or re-homes scope into it under rule G(a), led by this
  number, on that feature, which answers per its rule C — such a REPLAN
  leaves the gate that follows the changed section (Gate — Sufficiency
  for re-homed scope, Gate — Risks, acceptance & edges for an artifact,
  risk mitigation or ordering edge) and every later gate pending on
  #feature, named as pending in the REPLAN, ticked as bookkeeping citing
  the feature's REPLAN that accepts (for an edge, the one adding its
  `blocked_by` entry) or its `STATUS: progress`; a disclaiming REPLAN is
  answered here by a REPLAN re-owning the item before the gate is
  ticked. A contract deviation discovered after a required feature
  closed is recorded by a REPLAN on that closed feature posted by the
  finder (deviating contract corrected, ledger, no gate re-run),
  mirrored on every open capstone listing it, which REPLANs; a
  mirrored trigger whose reassessment finds nothing
  cited here changed, or whose comment this capstone's own REPLAN
  requested, is answered by a `STATUS: progress` acknowledgement
  naming the sections reassessed, not a REPLAN (D). Mirror `blocks`
  entries cite the counterpart (its number, and the REPLAN comment
  where one created the edge). In addition:

  E. The required set is a closed list with a sufficiency argument
     (§ Required Feature Set & Sufficiency): why exactly these
     features, together, make § Outcome Statement true — and why none
     is removable. Adding or removing a feature is a re-plan recorded
     with a REPLAN comment, not a quiet edit.
  F. A capstone must assert system-level acceptance criteria
     (§ System-Level Acceptance Criteria) that no single feature's
     completion criteria cover. If every criterion is already owned by
     a feature, this is a milestone label, not a capstone — do not
     file it.
  G. Orphaned scope. When a required feature closes, is re-tiered (a
     new issue plus a REPLAN on the old one — the tier model; a task
     re-tiered into a feature arrives as a `HANDOFF: re-tier` on the
     task and a new feature this capstone may adopt), or
     is descoped while leaving scope this capstone still needs, the
     REPLAN must give that scope a disposition: (a) re-home it — add
     the task to the requires_tasks roster of an OPEN required feature
     (one not closed on landed, REFUTED or SUPERSEDED; a closed feature
     is never reopened — use (b)); (b) file a
     new feature to host it and add that feature to requires_features;
     or (c) descope it, re-deriving the § Required Feature Set &
     Sufficiency argument. There is NO capstone→task edge; a capstone
     that appears to need a task directly is missing a feature — file
     one where a genuine span exists (feature rule B); work already
     landed when this capstone would list it is a precondition cited by
     permalink in § Required Feature Set & Sufficiency, never a roster
     entry.

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
     does not belong on the backlog (rule 2). -->

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
         § Cross-Feature Integration Risks). `blocks` stays `[]` at
         filing: it carries mirrors only, confirmed in the Counterparts
         box.
     Regenerate the mermaid graph on every REPLAN. -->

```yaml
tier: capstone
evidence_commit:        # SHA the roster and acceptance claims are pinned to
requires_features: []   # composition — the closed required set (rule E), FILED numbers only
requires_capstones: []  # composition — sub-capstones whose whole outcome gates this one
                        #   (nesting; the only sanctioned form, see the tier-model note)
planned_features: []    # one-line scopes for required features not yet filed; verify each
                        #   scope is ABSENT at evidence_commit before listing it; resolve
                        #   each to its number when filed — bookkeeping under rule C when
                        #   the filed body agrees with what was written against the
                        #   scope, otherwise REPLAN. A non-empty
                        #   planned_features means the sufficiency argument is
                        #   PROVISIONAL and this capstone is not Ready.
blocked_by: []          # ordering: capstones or features that must land before this
                        #   capstone closes, beyond the required set. Never tasks.
blocks: []              # mirrors only — the counterpart's blocked_by is authoritative:
                        #   capstones whose blocked_by names this one. Left [] at
                        #   filing; confirmed in the Counterparts box.
related: []             # reference only — never blocking, never ownership
```

```mermaid
flowchart TD
  %% Arrow A --> B means "A must land before B" (A blocks B).
  %% Show this capstone, its required features, and the ordering
  %% edges among those features (including edges to features or
  %% capstones outside this set). Regenerate on every REPLAN.
```

- [ ] **Counterparts synced** (bookkeeping, ticked after filing and re-ticked whenever a counterpart changes): every capstone whose `blocked_by` names this one appears in `blocks`; every feature in `requires_features` lists this capstone in its `serves_capstones`; every capstone or feature this capstone's `blocked_by` names carries the mirror in its `blocks`; every parent capstone cited in § Intent & Alignment still lists this capstone in `requires_capstones`; the alignment cites an open issue or a standing commitment, or is still marked unfiled — a closed target is re-aligned by REPLAN.

- [ ] **Gate — Intent & Status.** § Intent & Alignment states an outcome a reader could later confirm or deny; each alignment target was opened at its current revision and its cited section needs what this capstone claims to deliver, or the target is marked unfiled with a one-line scope, or it is a standing commitment stated here that a maintainer would uphold and from which the intent follows; every audience named is one the system-level change reaches. `evidence_commit` is the SHA actually checked out and is reachable from the default branch. Adversarial re-read of this span found no substantial finding.

## Outcome Statement

<!-- What becomes true of the project when this capstone lands, phrased
     as an observation: the demo script, command sequence, or
     acceptance walk-through a reviewer (or agent) executes to see it.
     "Do X, observe Y" at the system level. This walk-through is the
     top-level acceptance criterion; § System-Level Acceptance Criteria
     pins its parts. -->

- [ ] **Gate — Outcome.** The walk-through is executable step by step by someone with only the repository and the named platform; each step states what is observed; it is the intent of § Intent & Alignment and not a wider one; every audience in § User impact is reached by some observed step. Adversarial re-read of everything above found no substantial finding.

## Required Feature Set & Sufficiency

<!-- One row per required feature (and per sub-capstone), filed or
     planned, then the sufficiency argument (rule E): why this set
     jointly delivers § Outcome Statement, and per feature, what breaks
     in the walk-through if it were removed — the minimality check. A
     feature with no answer to the second question does not belong in
     the set. Write each contribution against the feature's own
     § Capability Statement & Scope Boundary as it reads NOW; a feature
     whose boundary disclaims the contribution claimed here is a plan
     defect. Work already landed when this capstone lists it (at filing
     or by an adopting REPLAN) is a precondition cited here by permalink,
     never a roster entry; a feature filed only to group landed tasks
     fails feature rule B. Fill
     `requires_features`, `requires_capstones` and
     `planned_features` now, and record the DAG walk. -->

| Feature | Contribution to the outcome | Status |
|---------|-----------------------------|--------|
| #       |                             |        |

- [ ] **Gate — Sufficiency.** Every FILED required feature's body was read at its current revision and its § Capability Statement & Scope Boundary supplies the contribution claimed; every row names the walk-through step that breaks without it; the set, together with the landed preconditions cited, covers every step of § Outcome Statement; every planned scope is verified absent at `evidence_commit`; `requires_features`, `requires_capstones`, `planned_features` are filled, every filed entry is of the tier its key requires, and the DAG walk for the composition edges is recorded. Adversarial re-read of everything above found no substantial finding.

## Cross-Feature Integration Risks

<!-- Where the required features touch: shared interfaces (cite each
     feature's § Feature-Level Interface & Data Contract by section
     name), ordering hazards, contract handoffs that cross feature
     boundaries, and the threats to validity that only appear at system
     scale — per-feature evidence that shortcuts the integrated code
     path, platform divergence, invariants that hold per-feature but not
     jointly. Each risk names its mitigation — a criterion in
     § System-Level Acceptance Criteria (written together with this
     section), an ordering edge, a feature's own invariant — or is
     explicitly accepted. Every necessary ordering is an edge: fill
     `blocked_by` and the mermaid graph at this group's gate (`blocks`
     stays empty or mirrors-only; the other side mirrors in its
     Counterparts box). -->

## System-Level Acceptance Criteria

<!-- Predictions spanning multiple features: do X, observe Y — each one
     not covered by any single feature's completion criteria (rule F).
     Name the end-to-end test, golden artifact, or recorded procedure
     that pins each, and which feature, `blocked_by` predecessor, or
     this issue's close-out builds the ones that do not exist yet (task
     rule 12). For every
     golden artifact or expected value: who produced it, when, and
     whether it was pre-committed, independently derived, or will be
     produced by the system it certifies (task rule 11). Together they
     pin every step of § Outcome Statement and every risk mitigation
     assigned here.

     EVERY CRITERION NAMES ITS NEXT MOVE ON FAILURE with every required
     entry landed: a fix feature under rule G(b) adopted by REPLAN, or
     `REFUTED:` — the premise of § Outcome Statement fails — posted after
     the dispositions of § Re-planning Protocol, quoting the failing
     criterion with command and output, the sheet left empty (task
     rule 10), mirrored per rule D.

     A "spans #A, #B" ANNOTATION IS NOT EVIDENCE. Before writing it,
     open #A and #B and read their § Integration Criteria & Evidence
     Plan and § Completion Criteria (Definition of Done). State what
     each contributes. If one of them already asserts the whole thing,
     say "covered alone by #A" — honest, and it simply does not count
     toward rule F. A capstone needs only ONE genuine system-level
     criterion, but it does need one.

     Defects to check for, each of which voids a criterion:
       - the second party named in a "span" is not in requires_features
         at all — then it is not a composition claim and the criterion
         is effectively unowned;
       - a criterion whose named owner DISCLAIMS it (one feature's
         scope boundary explicitly refuses the work the capstone
         assigns it);
       - a criterion no feature covers and no task owns — acceptance
         evidence with no work item anywhere; mark it UNOWNED;
       - text left stale by a feature's later REPLAN. -->

- [ ] **Gate — Risks, acceptance & edges.** Every shared interface cites both features' contracts by section name and each FILED feature's § Feature-Level Interface & Data Contract declares its side (a planned feature's side is confirmed at resolution, rule C); every ordering hazard is an edge in the waiting feature's machine block (assigned by a REPLAN posted on it, rule D) or, where this capstone itself waits, in this block, with no edge pointing at a task; `blocked_by` and `related` are filled, `blocks` is empty or carries only mirrors, and the DAG walk for the edges added here is recorded; the mermaid graph agrees with the machine block (rule A); every risk names a mitigation — a criterion here, an edge, a feature's invariant — or is accepted with a reason. Every acceptance criterion carries exactly one ownership annotation written against each FILED feature's actual text at its current revision (a planned feature's side and annotation are written against its planned scope and confirmed by the rule C resolution comment, otherwise REPLAN) and names its next move on failure; at least one is a genuine span or close-out criterion (rule F); every step of § Outcome Statement is pinned by some criterion; every risk mitigation assigned to this section exists here; no span names a feature outside the required set; no owner disclaims what is assigned to it; every artifact named exists at `evidence_commit` or has a named builder (task rule 12); every expected value names its custodian, date and provenance (task rule 11). Adversarial re-read of everything above found no substantial finding.

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

- [ ] **Gate — Consequences.** Every published surface or commitment the outcome moves has its consequence stated; every cost identified is stated or the section says there is none and why; none was accepted that § Intent & Alignment would not justify. Adversarial re-read of everything above found no substantial finding.

## Re-planning Protocol

<!-- What invalidates this plan and the required response: a required
     feature descoped or refuted → re-derive the sufficiency argument in
     § Required Feature Set & Sufficiency and apply rule G to any
     orphaned scope; a feature's contract deviates → reassess
     § Cross-Feature Integration Risks and § System-Level Acceptance
     Criteria (a required feature's mirrored REPLAN is the trigger); a
     sub-capstone's mirrored REPLAN → re-derive § Required Feature Set &
     Sufficiency; an acceptance criterion fails with every required
     entry landed → its named next move (a rule G(b) fix feature adopted
     by REPLAN, or `REFUTED:` after the dispositions below); this capstone re-tiered (a REPLAN plus a new issue at
     the new tier) → each required entry the new tier cannot hold is
     moved to the new issue's roster where its tier fits, re-parented to
     a named open capstone, or released with the rule D notice (it
     re-checks its beneficiary), and scope the outcome still needs goes
     through rule G applied by the new issue where it is a capstone, or
     becomes a `planned_tasks` entry there where it is a feature, or is
     released with a disposition in the REPLAN; the outcome itself is re-scoped → REPLAN with the old and
     new § Outcome Statement both quoted, then § Intent & Alignment
     re-checked; a required feature's mirrored WAIVED → the waived
     obligation checked against § System-Level Acceptance Criteria, and
     a successor outside the required set is rule G scope; a re-scope
     after which no criterion in § System-Level Acceptance Criteria is a
     genuine span or close-out criterion (rule F) → once the remaining required
     entries have landed, close with `SUPERSEDED: label — every criterion
     covered alone by #…`, mirrored to parent capstones, which re-derive
     their sufficiency with the features listed directly. Every response
     ends in a REPLAN comment here and a re-run of the gates covering
     the changed span — or, when the reassessment finds nothing cited
     here changed (or the trigger was a comment this capstone's own
     REPLAN requested), in a `STATUS: progress` acknowledgement naming
     the sections reassessed, which resets no gate (rule C). -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself. Include every design
     decision the acceptance pass will certainly hit, whether or not the
     text above names it: an unlisted one is open, not absent (§ Agentic
     Delegability, DA axis). For each: the question, options with a
     recommended default, and whether it blocks filing features, blocks
     acceptance, or can ride along. "N/A — fully specified" if nothing
     is open.

     Mark every entry with exactly one of `Recommended default: <answer>`
     (proceed and land), `PROPOSED: <answer>, pending <who confirms>` (draft
     but do not land), `BLOCKING: <who decides>` (stop), or
     `HYGIENE: <what to re-derive>` (not a decision at all — a
     citation, a status check, an un-run build). The marker is what an
     executor and § Agentic Delegability both read; an unmarked entry is
     scored on its substance, and the marker fixed. -->

- [ ] **Gate — Re-planning & decisions.** Every trigger named in § Re-planning Protocol has a response ending in a REPLAN comment where anything cited changed and in an acknowledgement otherwise, naming the sections it re-derives; the protocol covers every entry of the required set; every open question carries exactly one task rule 13 marker; every decision the acceptance pass will hit is listed or settled above; nothing marked `Recommended default:` contradicts a criterion or a risk mitigation. Adversarial re-read of everything above found no substantial finding.

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
- [ ] Every risk in § Cross-Feature Integration Risks is mitigated as stated, checked at system scale [rows: Risk]
- [ ] Every expected value the acceptance evidence compares against was pre-committed or independently derived, or the ADR-1 OS deduction was applied (task rule 11) [row: Oracle custody]
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A) [row: Roster]
- [ ] Landing reported with a `STATUS: landed` comment on every OPEN capstone whose `requires_capstones` lists this one (rule D) [row: Mirrors]
- [ ] Every decision in § Open Questions & Decisions Needed is resolved or explicitly deferred, none left blocking [rows: Open question]
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10) [row: Waivers]
- [ ] Every cited evidence document and permalink resolves on the default branch at close [row: Links]
- [ ] Every artifact named above exists at `evidence_commit` or is created by its named builder — this list says which (task rule 12) [row: Paths]
- [ ] § Agentic Delegability re-scored on any `REPLAN:` edit that changed the roster, the acceptance criteria, or the open decisions [row: Re-plans]
- [ ] ... [row: <added below>]

## Pickup Checks

<!-- The rows are fixed and never deleted: a row whose referent is N/A
     or empty is recorded "none" in the pickup comment and neither
     passes nor fails. Run once per executor by whoever begins (or, after a
     `HANDOFF: transfer`, takes over) the acceptance pass (task rule 6,
     applied at this tier). Preconditions for starting, not completion
     criteria. Record the outcome in a `STATUS: pickup` comment here
     (not mirrored; one line may list every "none" row). -->

- [ ] `action` read; execution follows the task rule 9 mapping for it (stop, block named steps, or proceed)
- [ ] Every own `REPLAN:` comment read; each names its predecessor (the previous own REPLAN) and the gates it re-ran, and the body matches the fold of all of them in stream order — one whose named predecessor is not the previous REPLAN is re-folded first
- [ ] Every entry in `requires_features` and `requires_capstones` has a `STATUS: landed` comment mirrored here (rule D), or its disposition is recorded; `planned_features` is empty
- [ ] Every mirrored `REPLAN:` or `WAIVED:` from a required feature or sub-capstone read, each REPLAN checked for contract deviations (recorded only by REPLAN, feature rule C); § Cross-Feature Integration Risks, § System-Level Acceptance Criteria and § Required Feature Set & Sufficiency reassessed by REPLAN where anything they cite changed
- [ ] Every parent capstone cited in § Intent & Alignment still lists this capstone in `requires_capstones`; a parent's REPLAN dropping it was read and § Intent & Alignment re-checked for a remaining beneficiary
- [ ] Not superseded: the § Outcome Statement walk-through does not already succeed at the checkout for reasons outside this plan
- [ ] Every `blocked_by` entry has landed, or the edge was removed by a `REPLAN:` comment with a Dropped/Retired ledger entry
- [ ] Every `BLOCKING:` entry in § Open Questions & Decisions Needed is answered; `PROPOSED:` entries noted as draft-only
- [ ] `evidence_commit` re-pinned and roster claims re-derived if HEAD has moved
- [ ] Every artifact a criterion names exists, or its named builder has landed, or its builder is this capstone's close-out and is scheduled (task rule 12)
- [ ] Every platform, device or apparatus the walk-through or a criterion names is available to whoever picks up, or the `STATUS: pickup` comment names the acceptance steps blocked and the holder they are handed to, or that no holder is yet known (those steps stay blocked); every other step may proceed

## Post-Acceptance Validation

<!-- Run at close, at one named commit. One row per item; rows are
     enumerated AT FILING (a row per walk-through step, per acceptance
     criterion, per risk, per required entry, per open question, per
     added completion criterion) with evidence cells empty; the rows of
     pre-filled criteria marked N/A are deleted at filing unless another
     criterion names the row. Evidence is a transcript, a command with its
     output, a test name at the commit, a permalink, or a comment link —
     never "done". -->

| Item | Check | Evidence | Result |
|------|-------|----------|--------|
| Walk-through step 1 | executed at the commit; observation matches § Outcome Statement | | |
| Acceptance criterion 1 | do X end-to-end, observe Y; each spanning feature's contribution shown | | |
| Risk 1 | mitigation in place at system scale | | |
| Required #A | deviations reconciled (landing and its mirror were blocked on at pickup) | | |
| Open question 1 | resolving comment, or permalink to the diff landing the `Recommended default:` / the re-derived `HYGIENE:` item | | |
| Oracle custody | each expected value: who, when, pre-committed / independently derived / produced by the system and deducted | | |
| Roster | machine block, table and mermaid agree; `planned_features` empty | | |
| Mirrors | `STATUS: landed` posted on every open capstone whose `requires_capstones` lists this one | | |
| Links | every permalink and document resolves on the default branch | | |
| Paths | each artifact a criterion names exists at the acceptance commit, created by its named builder (task rule 12) | | |
| Waivers | every skipped criterion has its `WAIVED:` comment | | |
| Re-plans | every `REPLAN:` comment reconciled; the body describes what was delivered; ADR re-scored where a re-plan changed roster, criteria or open decisions | | |

- [ ] Adversarial review of the delivered system against this issue found no substantial finding

- [ ] **Gate — Criteria & sheets.** Every completion criterion names its artifact, location and assertion; every path a criterion names exists at `evidence_commit` or has a named builder (task rule 12); every pre-filled criterion not marked N/A has its bracketed rows (a per-item row family with zero items satisfies its bracket) and every added criterion, walk-through step, acceptance criterion, risk, required entry and open question has a validation row, and no row remains for an N/A pre-filled criterion that no other criterion names; every pickup check refers to something the body contains or marks N/A; nothing in the sheets contradicts § Required Feature Set & Sufficiency. Adversarial re-read of everything above found no substantial finding.

## Abstract

<!-- Written last, read first. 2–4 sentences: the outcome, why it
     matters, and the one-line shape of the feature set that gates it —
     drawn from § Intent & Alignment, § Outcome Statement and § Required
     Feature Set & Sufficiency, contradicting none of them. The terminal
     gate in § Agentic Delegability covers it. -->

## Agentic Delegability (ADR-1)

<!--
  ADR-1 v5, outcome tier. How safely this capstone can be handed to an
  automated executor, and what maintenance liability delegating it as-is
  would create. NOT a rating of how good, important or urgent the work is.
  SCORE THE ACCEPTANCE PASS — this capstone's own system-level criteria and
  outcome statement — never the union of its features.
  Fill at filing; re-score when a re-plan or amendment changes the roster,
  the criteria or the open decisions. Where two ANCHORS in one axis
  could apply to one fact, take the lower. Caps and deductions stated inside
  an axis apply on top of the anchor chosen; they are not in competition
  with it.
  Self-contained by design; the other tier templates carry their own
  tier-adjusted copies. They can drift — change an axis in all of them and
  bump the version above together.

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
      1 a person reads prose and agrees (review of prose only)
      0 none; success asserted by whoever did the work, with no enumerated
        procedure or transcript
      A person operating the software and recording enumerated
      observations is scored by the shape of the check (4 or 3), then the
      document deduction below applies.
      DEDUCTIONS, CUMULATIVE, floor 0. Deduct 2 if the same change authors
      both the implementation and the expected values certifying it (task
      rule 11 defines "the same change" and the independently-derived
      exemption). Deduct
      1 if the acceptance evidence is a document asserting a measurement.
      `os:` records the score AFTER deductions; `os_deduction:` records the
      total deducted, 0-3.

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
      start to gated evidence. Parallel steps count for less than ordered
      ones: reliability falls with chain length, not with volume.
      5 under an hour   4 one to four hours   3 half a day to two days
      2 several days    1 more than a week, or the chain crosses a subsystem
        it must first learn                    0 a multi-week programme
      Measured as if the roster had landed: do not count time spent waiting
      for features. Cross-feature composition is what makes this tier what it
      is, so it is never by itself a score here — measure elapsed dependent
      time only. Where a harness, fixture or script the criteria rely on does
      not exist yet, score building it, wherever that work is assigned.

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
      REQUIRES, not only what the acceptance criteria happen to list.
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
        then it scores 2 and that harness is a named builder.
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

  `roster_delegable` — k/n over every FILED entry in the composition
  roster (`requires_features` and `requires_capstones`); an entry counts
  as delegable when its own block records band A or B. Unfiled planned entries are
  not counted; where any exist the fraction is provisional and the prose
  line must say so. Score `pending` when the roster is empty or its
  children are not yet scored, and name in the prose line which entry is
  closest to delegable. This predicts executability better than this
  issue's own band does.

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
      section's mandate, a completion criterion, a prediction, an edge, or
      the scope. Wording is not.
    - Unticked means not yet reviewed. It is not a defect and does not block
      filing; it means the band above, and the body, are still unvalidated.
    - Where a review comment exists, point `review_evidence` at it.
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
     device and what can still be produced unattended for it; and which
     feature is closest to yielding a delegable leaf. -->
