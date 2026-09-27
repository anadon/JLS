---
name: Feature
about: A coherent capability composed of scientific tasks — owns the decomposition, the feature-level contract, and the integration evidence that its tasks jointly deliver it
labels: ["tier:feature"]
---

<!--
  Template: feature v5 (2026-09)

  TIER MODEL — task → feature → capstone. THIS BLOCK IS CANONICAL: the
  task and capstone templates cite it rather than restating it.

  The model is strictly layered. Each tier composes the tier below and
  orders against its own tier; NO EDGE EVER POINTS UPWARD.

    COMPOSITION — "is carried out by":
      capstone  composes  features, capstones (nesting)
      feature   composes  tasks
      task      composes  nothing

    ORDERING — "depends on" (blocked_by / blocks):
      task      depends on  tasks
      feature   depends on  features, tasks
      capstone  depends on  capstones, features

    REFERENCE — `related` is non-blocking and informational only, and
    may point at ANY tier from any tier. It carries no ownership and
    no ordering. Do not use it to record either.

  A TASK MAY BE SHARED BY ANY NUMBER OF FEATURES. Shared work is simply
  shared — there is no single-owner rule and no per-task owner field.
  Ownership is recorded ONLY here, in this feature's requires_tasks
  roster, which is the sole authority. To learn which features own a
  task, read the rosters; the task itself does not claim an owner. A
  task's § Intent & Alignment may name this feature as what it aligns
  with; that is a reference, not ownership.

  A feature may likewise serve any number of capstones; there the
  capstone's requires_features is authoritative and serves_capstones
  mirrors it.

  `tier:` is fixed at filing: a re-tier is a new issue at the new tier
  plus a `HANDOFF: re-tier` (task) or REPLAN (feature, capstone) on the
  old one moving its scope; an in-place tier edit is a filing defect. An issue's
  tier is defined by its machine block's `tier:` key; the
  tier:* label is a mirror for filtering — a missing or stale label is
  bookkeeping to fix, never an edge violation.

  THE ORDERING GRAPH IS blocked_by/blocks PLUS every composition edge
  read child-before-parent (a parent cannot close before its children
  land). That combined graph must stay a DAG at the instance level,
  across all tiers — tier legality alone does not prevent a cycle (a
  capstone requiring feature A while A is blocked_by that capstone).
  Before adding an edge, walk the machine blocks of the issues it names
  — following their listed edges outward — and confirm no path leads
  back here; record that walk in the filing or REPLAN comment. A cycle
  is a filing defect.

  Blocking a composite: an ordering edge aimed at a feature or capstone
  gates that issue's integration/close-out only, never its children's
  start — to gate children, block them directly. Because upward edges
  are illegal, a task that "waits on a feature" must instead name the
  specific sibling TASKS it waits on.

  RULES — the scientific-task template's rules 1–7 apply here, adapted
  to this tier: evidence with file:line at a pinned commit (1); no
  padding — "N/A — <reason>" (2); claims stated observably (3); atomic
  scope (4); cross-references cite section NAMES, resolved against the
  headings of the cited issue's template, same-body ones against this
  one — headings carry no numbers (5);
  executor re-verification of evidence before acting, here § Pickup
  Checks (6); explicit labels, here `tier:feature` plus `bug` or
  `enhancement` matching the corpus (7). Where a task rule names a
  task-tier section (Observations, Method), apply it to the analogous
  section here. Task rules 9–10 (comment protocol and waivers) apply
  with `REPLAN:` in place of `AMENDED:`, and with the same `STATUS:`
  sub-tags. `HANDOFF: transfer` applies unchanged when the integration
  work changes hands — body unchanged, no gate reset, the transferee
  re-runs § Pickup Checks in full and posts its own `STATUS: pickup`
  citing it; `HANDOFF: split` and `HANDOFF: re-tier` do not exist at
  this tier: a split is a REPLAN plus a new issue at this tier, a
  re-tier a REPLAN plus a new issue at the new tier — that REPLAN
  carries the Dropped/Retired ledger moving every item the new tier can
  hold to it, gives every roster entry it cannot hold a disposition per
  § Re-planning Protocol (posted per rule C), and is the close-out
  record (task rule 10 read with REPLAN in place of `HANDOFF: re-tier`). Task rules 11–13 (oracle custody, artifact paths resolve
  at filing, marked open questions) apply to § Integration Criteria &
  Evidence Plan and § Open Questions & Decisions Needed. Task rules
  14–15 (dependency order with consistency gates; three check-sheets by
  when they run) apply unchanged: gates are ticked at filing and at
  every REPLAN that touches the span they cover, § Pickup Checks runs
  before the integration work begins, § Post-Integration Validation
  runs at close. In addition:

  A. The machine block in § Status & Dependency Graph is the source of
     truth for graph assembly. Prose and the mermaid graph elaborate
     it and must agree with it; on conflict, fix the body — do not
     guess.
  B. A feature is not a folder. It must assert at least one
     integration criterion (§ Integration Criteria & Evidence Plan)
     that no single child task's completion criteria cover alone, and
     its machine block must name at least one child in requires_tasks
     or planned_tasks. Failing either test, it is a label, not a
     feature — do not file it.
  C. This body is a living plan. Plan changes — roster membership,
     contract, invariants, criteria, edges — are edited only together
     with a `REPLAN:` comment stating what changed and why; gates reset
     and re-run as task rule 14 says (the gate after the changed section
     and every later one, plus the two review boxes), and the REPLAN
     comment names the gates re-run. Bookkeeping is exempt: flipping a
     roster Status cell; refreshing `roster_delegable` from the
     children's current blocks, citing the child's AMENDED or HANDOFF;
     ticking a gate or review box (the tick is the record); ticking a DoD or check-sheet box whose backing evidence is
     already recorded in a comment on this issue that quotes the box (a
     PR link inside that comment is the usual evidence); re-pinning
     evidence_commit, bookkeeping only when every cited line re-derives
     at the new commit, otherwise part of the REPLAN; adding or removing a `blocks` or
     `serves_capstones` entry that mirrors a counterpart's authoritative
     edge, citing the counterpart (its number, and the REPLAN or
     AMENDED comment where one created the edge); resolving a planned_tasks scope to its
     number, or replacing an "unfiled" alignment with its citation —
     provided the edit's comment states that the filed issue's cited
     sections were read and agree with the row, handoffs and criteria
     written against the scope — for handoffs, "agree" means the filed
     child's § Internal interfaces consumed, § Internal interfaces
     provided — public and, for a stubbed handoff, § Internal interfaces
     provided — private name exactly the sibling handoffs this body
     assigns it; any additional sibling interface makes the resolution
     a REPLAN that re-runs Gate — Decomposition, Contract & Integration
     and every later gate (if they do not agree, the edit is a REPLAN).
     Every REPLAN is mirrored on every OPEN capstone whose
     `requires_features` lists this feature (serves_capstones mirrors
     that set; a capstone reads this feature's roster, contract, capability,
     invariants and integration criteria, so any of them changing is
     its trigger); a REPLAN that drops a child from `requires_tasks`,
     answers a child's re-plan request, or changes a § Global
     Invariants entry or a handoff assigned to a filed child is also
     posted, led by this number, on each such OPEN child, which answers
     with an AMENDED where any section of its body must change (mirrored
     here even after a drop, and acknowledged, since this REPLAN
     requested it) or otherwise a `STATUS: progress` naming the sections
     reassessed; a child that has already closed (landed, REFUTED,
     SUPERSEDED, HANDOFF: re-tier) receives it as a notice at most and
     owes no answer — the REPLAN cites its close-out comment as the
     disposition. A REPLAN that changes a handoff or § Global Invariants
     entry assigned to an open child leaves Gate — Decomposition,
     Contract & Integration and every later gate pending on that child's
     answer; the REPLAN names them as pending on #child, and the tick is
     bookkeeping citing the answering AMENDED or `STATUS: progress`.
     Every REPLAN names the own prefixed comment it was folded onto (or
     "first"); the writer re-reads the comment stream immediately before
     the body edit and folds in any prefixed comment newer than its
     read. A mirrored `HANDOFF: split` whose successor this
     feature's filing already planned is likewise one this feature
     requested. A mirrored trigger
     whose reassessment finds nothing cited here changed, or whose
     comment this feature's own REPLAN requested, is answered by a
     `STATUS: progress` comment naming the sections reassessed — not a
     REPLAN — and resets no gate, unless that comment is itself a
     re-plan request (rule D). If another agent
     edited since you read, re-read and fold the newer body into your
     edit — the REPLAN comment stream is the arbiter of intent.
  D. State lives in comments, not checkboxes. Child tasks post
     `STATUS: landed`, `REFUTED:` (hypothesis failed, with evidence),
     `HANDOFF:` (the AMENDED of a split or transfer, task rule 9 — read
     here as an AMENDED for gate and roster purposes), `SUPERSEDED:`
     (already shipped), `AMENDED:` and `WAIVED:` on their own issue AND
     mirror the same comment here, led by the child's number
     (`STATUS: pickup` and `STATUS: progress` stay on the child); this
     feature mirrors every prefixed comment of its own except
     `STATUS: pickup` and `STATUS: progress` — landing, refutation,
     supersession, waivers and every REPLAN — led by this number, on
     every OPEN capstone whose `requires_features` lists this feature
     (serves_capstones mirrors that set; task rule 9 applies
     unchanged). A roster that adopts a child by REPLAN links in that
     REPLAN the child's prefixed comments posted before the adoption
     (its landing comment, if landed), and a
     child listed at filing that landed before this feature existed has
     its landing comment linked from its § Decomposition & Rationale
     row, since no mirror was posted here. A child's mirrored AMENDED that contradicts
     § Global Invariants or a handoff, or declares a modified or provided
     surface § Feature-Level Interface & Data Contract does not, is a
     re-plan request answered ON RECEIPT by a REPLAN here, not deferred
     to pickup; so is a serving capstone's REPLAN posted here that
     assigns this feature an artifact or risk mitigation. A fresh agent reconstructs execution state from
     the machine block plus the prefixed comments; checkbox state is a
     convenience rendering, never evidence.

  These comments do not render on GitHub — leave them in place for the
  next reader of the raw issue body.
-->

## Intent & Alignment

<!-- Written FIRST; everything below is checked against it at every
     gate.

     Intent: the capability this feature makes true, in one paragraph
     phrased so a reader can later say whether it happened. Not the
     decomposition — that is § Decomposition & Rationale.

     Alignment: what this feature serves, cited by issue number AND
     section name — each capstone's § Outcome Statement it contributes
     to (each becomes a serves_capstones entry once that capstone's
     requires_features lists this number), or the standing project
     commitment it upholds. When the parent is not yet filed, write
     "serving capstone unfiled — <one-line scope>" and replace it with
     the citation once it exists (bookkeeping, rule C). A feature
     aligned with nothing has no beneficiary and does not belong on the
     backlog (rule 2). -->

### User impact

<!-- The concrete audience(s) served and, per audience, the change they
     experience once the WHOLE feature — not any single task — has
     landed. Pick and name the ones this feature serves:
       - Students drawing and simulating circuits in the editor.
       - Instructors authoring, grading, or auto-checking work in batch
         (`-b`) mode.
       - Circuit-file authors and third-party tools that read or write
         `.jls` files against the save format's authoritative definition.
       - Packagers and distributors shipping the installers and container.
     Effects on the codebase itself belong in § Code & Project Impact
     and Consequences, below the contract that determines them. -->

## Status & Dependency Graph

<!-- The machine block is the source of truth for graph assembly
     (rule A). It is filled PROGRESSIVELY, the gates say when:
       - `evidence_commit` now: the SHA every roster and contract claim
         is pinned to.
       - `requires_tasks`, `planned_tasks` at the gate after
         § Integration Criteria & Evidence Plan, once the cuts are made.
       - `blocked_by`, `related` and the mermaid graph at the gate after
         § Sequencing & Parallelism. `blocks` and `serves_capstones`
         stay `[]` at filing: they carry mirrors only, confirmed in the
         Counterparts box.
     Regenerate the mermaid graph on every REPLAN. -->

```yaml
tier: feature
evidence_commit:        # SHA the roster and contract claims are pinned to
requires_tasks: []      # composition: FILED children only, numbers, e.g. [101, 102]
                        #   AUTHORITATIVE for ownership. A task may appear in any
                        #   number of feature rosters — a shared task is shared,
                        #   and lists it in each. Tasks carry no owner field.
planned_tasks: []       # one-line scopes for children not yet filed; verify each
                        #   scope is ABSENT at evidence_commit before listing it
                        #   (a landed scope is Background, not a plan); resolve
                        #   each to its number when filed — bookkeeping under rule C
                        #   when the filed body agrees with what was written against
                        #   the scope, otherwise REPLAN. A
                        #   non-empty planned_tasks means this feature's rule B
                        #   sufficiency argument is PROVISIONAL and the feature
                        #   is not Ready.
blocked_by: []          # ordering: features or tasks that must land first
                        #   (never capstones — upward edges are illegal)
blocks: []              # mirrors only — the counterpart's blocked_by is authoritative:
                        #   features or capstones whose blocked_by names this feature.
                        #   Left [] at filing; confirmed in the Counterparts box.
serves_capstones: []    # capstones whose required set includes this feature
                        #   (mirror; the capstone's requires_features is authoritative)
related: []             # reference only — never blocking, never ownership
```

```mermaid
flowchart TD
  %% Arrow A --> B means "A must land before B" (A blocks B).
  %% Show this feature, every child task, and all ordering edges
  %% among them and to external issues. Regenerate on every REPLAN.
```

- [ ] **Counterparts synced** (bookkeeping, ticked after filing and re-ticked whenever a counterpart changes): every capstone whose `requires_features` lists this feature appears in `serves_capstones`, and every capstone in `serves_capstones` still lists this feature; every feature or capstone whose `blocked_by` names this feature appears in `blocks`; every task or feature this feature's `blocked_by` names carries the mirror in its `blocks`; the alignment in § Intent & Alignment cites an open issue or a standing commitment, or is still marked unfiled — a closed target is re-aligned by REPLAN.

- [ ] **Gate — Intent & Status.** § Intent & Alignment states a capability a reader could later confirm or deny; each alignment target was opened at its current revision and its cited section needs what this feature claims to supply, or the target is marked unfiled with a one-line scope, or it is a standing commitment stated here that a maintainer would uphold and from which the intent follows; every audience named is one the whole feature reaches. `evidence_commit` is the SHA actually checked out and is reachable from the default branch. Adversarial re-read of this span found no substantial finding.

## Capability Statement & Scope Boundary

<!-- What the feature makes true, phrased observably — do X, observe Y
     at the feature boundary — and explicitly what is OUT of scope: the
     adjacent work an executor might be tempted to absorb, with the
     issue that owns it instead, or "unfiled — file at close". Children
     cite this section as the alignment target of their own § Intent &
     Alignment, so it must say what they will assume it says. -->

- [ ] **Gate — Capability.** The capability is stated as an observation at the feature boundary; it is the intent of § Intent & Alignment and not a wider one; every out-of-scope item names its owning issue or "unfiled"; the boundary was read against each alignment target's text and contradicts none of it, and every artifact, risk mitigation or re-homed scope a serving or planning capstone's § System-Level Acceptance Criteria, § Cross-Feature Integration Risks or rule G assigns to this feature is inside the boundary. Adversarial re-read of everything above found no substantial finding.

## Decomposition & Rationale

<!-- One row per child task, filed or planned. The one-line contract is
     the handoff summary an agent reads before deciding whether to open
     the child at all; it must match the child's own § Intent &
     Alignment and § Hypothesis (falsifiable) as they read NOW. A child
     that landed before this feature existed links its landing comment
     in its row (rule D). Below
     the table: why these cuts and not others — the alternative
     decompositions considered and rejected, so a re-planning agent does
     not re-derive them from scratch. -->

| Task | One-line contract | Status |
|------|-------------------|--------|
| #    |                   |        |

## Feature-Level Interface & Data Contract

<!-- § Interface & Data Contract of the task template, applied at the
     feature boundary: what the feature as a whole modifies, consumes,
     provides, tracks durably, uses ephemerally, its concurrency model,
     and its transformations — stated so the integrated result can be
     checked against it. Then the internal handoffs: which child
     provides which interface to which sibling, citing the child's
     contract subsection by name (e.g. "#101 provides `ElementRegistry`
     per its § Internal interfaces provided — public; consumed by #102
     per its § Internal interfaces consumed"). Transformations at the
     feature boundary follow the task template's § Data transformations
     discipline: fully defined and expressed in embedded LaTeX math
     (GitHub math rendering), never prose alone. When a child lands with
     a contract deviation, this section is stale until a REPLAN comment
     resolves it. -->

## Integration Criteria & Evidence Plan

<!-- Feature-level predictions: do X, observe Y — each one something no
     single child's completion criteria assert (rule B). Name the
     integration test, golden file, or recorded manual procedure that
     pins each, and note which do not exist yet and which child,
     `blocked_by` predecessor, or this issue's close-out builds them
     (task rule 12). A recorded manual procedure names its platform and
     operator class (a person, a display substrate, a device). For every golden
     file or expected value: who produced it, when, and whether it was
     pre-committed, independently derived, or will be produced by the
     implementation it certifies (task rule 11 — the ADR OS deduction
     reads this).

     ANNOTATE OWNERSHIP PER CRITERION. A blanket sentence ("none of
     these is covered by any single child alone") is not checkable and
     is forbidden; give every criterion exactly one of:
       - "spans #A + #B" — and state what each contributes, so the
         claim can be falsified by reading either child.
       - "covered alone by #A" — honest, and it simply does not count
         toward rule B. Listing it is fine; miscalling it a span is not.
       - "no child; built by this feature's close-out".
       - "UNOWNED — no builder yet" — a real gap, worth stating.

     WRITE THIS AGAINST THE CHILD'S ACTUAL TEXT at its current revision,
     not from memory or from the plan it was filed under. If a child
     later amends its scope, this section is stale until re-derived.
     A child that cites one of these criteria as its own deliverable is
     proof the criterion is not a span; validator check G22 reports
     that case. -->

- [ ] **Gate — Decomposition, Contract & Integration.** Every FILED child body was read at its current revision; every filed roster row's one-line contract matches the child's own § Intent & Alignment and § Hypothesis (falsifiable); every handoff names provider child, consumer child and both contract subsections by name, and each filed child's contract actually declares its side; no two children hand off in both directions (a mutual handoff is re-cut with an interface-first child, or the consuming child of one direction declares a private stub of that interface under its own § Internal interfaces provided — private so that direction carries no `blocked_by` edge, and the handoff names the child that later retires the stub; a one-way handoff whose consumer must not wait may be stubbed the same way, and § Sequencing & Parallelism then lists the pair as independent with a third child that retires the stub, ordered after both provider and consumer); every feature-boundary transformation is fully defined in math; every integration criterion carries exactly one ownership annotation and at least one is a genuine span or close-out criterion (rule B); no child claims a criterion here as its own deliverable; every artifact a criterion names exists at `evidence_commit` or has a named builder; every expected value names its custodian, date and provenance; the rejected decompositions are stated. `requires_tasks` and `planned_tasks` are filled, together non-empty, every planned scope verified absent at `evidence_commit`, every filed child's tier is task. Adversarial re-read of everything above found no substantial finding.

## Global Invariants

<!-- What EVERY child must preserve at every intermediate landing —
     e.g. historical `.jls` files still load, save output
     byte-identical unless a version bump is declared, `mvn verify`
     green, no new SpotBugs exclusions. Each stated so a test can pin
     it at any landing. Children's pre-filled completion criterion
     "§ Global Invariants of every feature whose `requires_tasks` lists
     this task" binds them to this section; a task shared by several
     features satisfies all of their invariants. -->

## Code & Project Impact and Consequences

<!-- What changes for contributors, maintainers, LLM agents working the
     codebase, and the project's own commitments once the feature is
     integrated, now that the contract fixes what moves: interfaces that
     move, invariants that tighten or loosen, build or CI behaviour,
     published surfaces, maintenance cost taken on or retired. State
     consequences as well as benefits — what becomes harder, what a
     later change must now respect, what this forecloses. A consequence
     too costly to accept is a reason to narrow § Capability Statement &
     Scope Boundary or re-cut § Decomposition & Rationale (REPLAN). -->

- [ ] **Gate — Invariants & consequences.** Every invariant is pinnable by a test at any intermediate landing; for every child another feature's roster also lists, that feature's § Global Invariants were read and none contradicts these; every interface the feature-level contract moves has its consequence stated; every cost identified is stated or the section says there is none and why; no consequence was accepted that § Intent & Alignment would not justify. Adversarial re-read of everything above found no substantial finding.

## Sequencing & Parallelism

<!-- The critical path through the roster, which tasks are mutually
     independent (safe to execute concurrently by separate agents), and
     any ordering that is convention rather than necessity — marked as
     such, so a scheduler may break it knowingly. Every necessary order
     here is a `blocked_by` edge in the child's own machine block; fill
     this feature's `blocked_by` and the mermaid graph now and record
     the DAG walk (`blocks` and `serves_capstones` stay empty or
     mirrors-only). -->

- [ ] **Gate — Sequencing & edges.** Every necessary ordering is an edge in the filed children's machine blocks, or is recorded here to be added when a planned child is filed, or — for an ordering both of whose children landed before this feature existed — § Sequencing & Parallelism records the landing order in place of the edge (an open child waiting on a landed one still carries the edge in its `blocked_by`), and appears in the mermaid graph; tasks called mutually independent share no handoff in § Feature-Level Interface & Data Contract other than one stubbed under Gate — Decomposition, Contract & Integration (the retiring child is then ordered after both); convention-only orderings are marked; `blocked_by` and `related` are filled, `blocks` and `serves_capstones` are empty or carry only mirrors, no edge points upward, the DAG walk is recorded, and the mermaid graph agrees with the machine block (rule A). Adversarial re-read of everything above found no substantial finding.

## Re-planning Protocol

<!-- What invalidates this plan and the required response. At minimum:
     a child REFUTED → which siblings' premises are affected and who
     re-plans; a child split → the REPLAN precedes the HANDOFF (task
     rule 9's order: successor as a planned scope, handoffs moved), the
     mirrored HANDOFF is acknowledged and the planned scope resolved to
     the successor's number as bookkeeping; a transfer HANDOFF is
     acknowledged; a contract
     deviation → § Feature-Level Interface & Data Contract
     reconciliation; a serving capstone's REPLAN assigning this feature
     a new artifact, risk mitigation or re-homed scope (rule G) → Gate
     — Capability re-run: a
     REPLAN widening § Capability Statement & Scope Boundary and the
     roster, or one disclaiming it (the capstone then re-owns the
     criterion); a child re-tiered (`HANDOFF: re-tier`) → dropped by
     REPLAN with disposition closed, and the serving capstone applies
     rule G to the new issue (the drop cites the child's close-out comment and is not posted on it, rule C); a child's mirrored AMENDED or WAIVED → roster row
     and handoffs re-derived, and where it contradicts § Global
     Invariants the answering REPLAN is posted on receipt; a shared child's other listing feature
     adds or changes a § Global Invariants entry that conflicts with
     one here → the feature whose invariant is newer re-plans; a
     serving capstone descoped → whether this feature
     still has a beneficiary; a child dropped from the roster or this
     feature descoped → the REPLAN comment gives EACH affected child a
     disposition: re-homed (added to the requires_tasks of an OPEN
     feature — one not closed on landed, REFUTED or SUPERSEDED),
     freed (in no roster — pending the child's answering AMENDED, which
     restates its § Intent & Alignment to a standing rule, a format
     document or another open feature, or records that none can be
     cited and closes under task rule 10), or closed.
     Because a task may be shared, dropping it from THIS roster does not
     orphan it if another roster still lists it — check before assuming
     a disposition is needed. Closing this feature with scope UNMET
     while a serving capstone still needs it → each unmet scope item
     gets a disposition in the closing REPLAN: re-homed into another
     required feature, filed as a new feature the capstone adopts under
     its rule G, or descoped with the capstone's sufficiency argument
     re-derived — never silently dropped. Every response ends in a
     REPLAN comment here and a re-run of the gates covering the changed
     span — or, when the reassessment finds nothing cited here changed
     (or the trigger was a comment this feature's own REPLAN requested),
     in a `STATUS: progress` acknowledgement naming the sections
     reassessed, which resets no gate (rule C). -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself. Include every design
     decision the integration work will certainly hit, whether or not
     the text above names it: an unlisted one is open, not absent
     (§ Agentic Delegability, DA axis). For each: the question, options
     with a recommended default, and whether it blocks filing children,
     blocks integration, or can ride along. "N/A — fully specified" if
     nothing is open.

     Mark every entry with exactly one of `Recommended default: <answer>`
     (proceed and land), `PROPOSED: <answer>, pending <who confirms>` (draft
     but do not land), `BLOCKING: <who decides>` (stop), or
     `HYGIENE: <what to re-derive>` (not a decision at all — a
     citation, a status check, an un-run build). The marker is what an
     executor and § Agentic Delegability both read; an unmarked entry is
     scored on its substance, and the marker fixed. -->

- [ ] **Gate — Re-planning & decisions.** Every trigger named in § Re-planning Protocol has a response that ends in a REPLAN comment where anything cited changed and in an acknowledgement otherwise, and names which sections it re-derives; the protocol covers every child in the roster and every serving or planning capstone (alignment targets, and any whose `planned_features` carries this scope); every open question carries exactly one task rule 13 marker; every decision the integration will hit is listed or settled above; nothing marked `Recommended default:` contradicts the contract or an invariant. Adversarial re-read of everything above found no substantial finding.

## Completion Criteria (Definition of Done)

<!-- WHAT must be true of the integrated feature — not how it is
     checked (§ Post-Integration Validation) and not what is checked
     before integration starts (§ Pickup Checks). Every box names the
     artifact, its location and the assertion that pins it; each
     pre-filled box names in brackets the row(s) of § Post-Integration
     Validation that verify it, and each added criterion gets a row of
     its own. -->

- [ ] Every entry in `requires_tasks` closed as landed, or descoped via a `REPLAN:` comment with the roster updated and each child's disposition recorded; `planned_tasks` empty (each resolved to a filed issue or descoped) [rows: Child, Roster]
- [ ] The capability of § Capability Statement & Scope Boundary is observed at the final commit [row: Capability]
- [ ] Nothing outside § Capability Statement & Scope Boundary was absorbed; adjacent work is filed [row: Scope]
- [ ] Every prediction in § Integration Criteria & Evidence Plan holds at a named commit [rows: Integration criterion]
- [ ] The integrated result satisfies § Feature-Level Interface & Data Contract; deviations recorded by REPLAN, none silently absorbed [rows: Contract]
- [ ] § Global Invariants hold at the final commit, re-verified — not inferred from children's green runs [rows: Invariant]
- [ ] Every expected value the integration evidence compares against was pre-committed or independently derived, or the ADR-1 OS deduction was applied (task rule 11) [row: Oracle custody]
- [ ] Every capstone whose `requires_features` lists this feature notified with a `STATUS: landed` comment citing the REPLAN of any contract deviation those capstones must reconcile (`serves_capstones` mirrors that set) [row: Mirrors]
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A) [row: Roster]
- [ ] Every decision in § Open Questions & Decisions Needed is resolved or explicitly deferred, none left blocking [rows: Open question]
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10) [row: Waivers]
- [ ] Every cited evidence document and permalink resolves on the default branch at close [row: Links]
- [ ] Every artifact named above exists at `evidence_commit` or is created by its named builder — this list says which (task rule 12) [row: Paths]
- [ ] § Agentic Delegability re-scored on any `REPLAN:` edit that changed the roster, the integration evidence, or the open decisions [row: Re-plans]
- [ ] ... [row: <added below>]

## Pickup Checks

<!-- Run once per executor by whoever begins (or, after a
     `HANDOFF: transfer`, takes over) the integration work of § Integration
     Criteria & Evidence Plan (task rule 6, applied at this tier).
     Preconditions for starting, not completion criteria. Record the
     outcome in a `STATUS: pickup` comment here (not mirrored). -->

- [ ] Every own `REPLAN:` comment read; each names its predecessor and the gates it re-ran, and the body matches the fold of all of them in stream order — one whose named predecessor is not the one before it in the stream is re-folded first
- [ ] Every `REPLAN:` a serving capstone posted here read and answered (rule D)
- [ ] Every child in `requires_tasks` has a `STATUS: landed` comment mirrored here, or the adopting REPLAN or its roster row links its landing comment, or its disposition is recorded; `planned_tasks` is empty
- [ ] Every mirrored `AMENDED:`, `HANDOFF:` or `WAIVED:` from a child read (or, for a comment posted before this roster listed the child, read on the child or from the adopting REPLAN's links), every `REPLAN:` another listing feature posted on a shared child, and each child's `STATUS: progress` answering an own REPLAN read on the child — a REPLAN posted on an OPEN child with no answer is chased before integration begins; roster rows, handoffs and § Global Invariants re-derived by REPLAN where they changed
- [ ] Every child's mirrored AMENDED checked for contract deviations (a deviation is recorded only by AMENDED, task rule 9), including surfaces the feature-level contract does not declare; each reconciled in § Feature-Level Interface & Data Contract by REPLAN
- [ ] Every capstone in `serves_capstones` still lists this feature in `requires_features`; a capstone's REPLAN dropping this feature was read and § Intent & Alignment re-checked for a remaining beneficiary
- [ ] Not superseded: the capability of § Capability Statement & Scope Boundary is not already observable at the checkout for reasons outside this plan (the roster's own landings do not count)
- [ ] Every `blocked_by` entry has landed, or the edge was removed by a `REPLAN:` comment with a Dropped/Retired ledger entry
- [ ] Every `BLOCKING:` entry in § Open Questions & Decisions Needed is answered; `PROPOSED:` entries noted as draft-only
- [ ] `evidence_commit` re-pinned and roster claims re-derived if HEAD has moved
- [ ] Every artifact an integration criterion names exists at the checkout, or its named builder has landed, or its builder is this feature's close-out and is scheduled (task rule 12)
- [ ] Every platform, device or apparatus the evidence plan names is available to whoever picks up, or the `STATUS: pickup` comment names the integration steps blocked and the holder they are handed to, or that no holder is yet known (those steps stay blocked); every other step may proceed

## Post-Integration Validation

<!-- Run at close against the integrated result. One row per item; rows
     are enumerated AT FILING (a row per integration criterion, per
     contract item, per invariant, per child, per open question, per
     added completion criterion) with evidence cells empty. Evidence is a command with its output, a test
     name at a commit, a permalink, or a comment link — never "done". -->

| Item | Check | Evidence | Result |
|------|-------|----------|--------|
| Capability | do X at the final commit, observe Y, as § Capability Statement & Scope Boundary states | | |
| Scope | integrated diffs stay inside the boundary; extras filed as # | | |
| Integration criterion 1 | do X at the named commit, observe Y; each spanning child's contribution shown | | |
| Contract: modified / consumed / provided | declared at the feature boundary vs. observed in the integrated code | | |
| Contract: durable / ephemeral / concurrency | as declared | | |
| Contract: transformations | each defined stage located across the children's diffs | | |
| Contract: handoff #A → #B | provider side and consumer side both present as declared | | |
| Invariant 1 | re-verified at the final commit: command and output | | |
| Child #A | landed; `STATUS: landed` mirrored here or linked from the adopting REPLAN or its roster row; deviations reconciled | | |
| Open question 1 | resolving comment, or permalink to the diff landing the `Recommended default:` / the re-derived `HYGIENE:` item | | |
| Oracle custody | each expected value: who, when, pre-committed / independently derived / produced by the implementation and deducted | | |
| Roster | machine block, table and mermaid agree; `planned_tasks` empty | | |
| Mirrors | `STATUS: landed` posted on every capstone whose `requires_features` lists this feature | | |
| Links | every permalink and document resolves on the default branch | | |
| Paths | each artifact a criterion names exists at the final commit, created by its named builder (task rule 12) | | |
| Waivers | every skipped criterion has its `WAIVED:` comment | | |
| Re-plans | every `REPLAN:` comment reconciled; the body describes what was built; ADR re-scored where a re-plan changed roster, evidence or decisions | | |

- [ ] Adversarial review of the integrated result against this issue found no substantial finding

- [ ] **Gate — Criteria & sheets.** Every completion criterion names its artifact, location and assertion; every path a criterion names exists at `evidence_commit` or has a named builder (task rule 12); every pre-filled criterion's bracketed rows exist and every added criterion, integration criterion, contract item, invariant, child and open question has a validation row; every pickup check refers to something the body actually contains; nothing in the sheets contradicts § Sequencing & Parallelism or § Capability Statement & Scope Boundary. Adversarial re-read of everything above found no substantial finding.

## Abstract

<!-- Written last, read first. 2–4 sentences: the capability, why it
     matters, and the one-line shape of the decomposition — drawn from
     § Intent & Alignment, § Capability Statement & Scope Boundary and
     § Decomposition & Rationale, contradicting none of them. The
     terminal gate in § Agentic Delegability covers it. -->

## Agentic Delegability (ADR-1)

<!--
  ADR-1 v5, composition tier. How safely this feature can be handed to an
  automated executor, and what maintenance liability delegating it as-is
  would create. NOT a rating of how good, important or urgent the work is.
  SCORE THIS FEATURE'S OWN DELIVERABLE — its integration evidence and its
  declared contract — never the union of its children; each child carries
  its own block.
  Fill at filing; re-score when a re-plan or amendment changes the roster,
  the integration evidence or the open decisions. Where two ANCHORS in one axis
  could apply to one fact, take the lower. Caps and deductions stated inside
  an axis apply on top of the anchor chosen; they are not in competition
  with it.
  Self-contained by design; the other tier templates carry their own
  tier-adjusted copies. They can drift — change an axis in all of them and
  bump the version above together.

  SEVEN ADDITIVE AXES, 0-5 each. RAW = their sum, 0-35.

  SC  SPECIFICATION CLOSURE — is the end state fixed by the text alone?
      Test: could two competent implementers each satisfy the integration criteria and
      produce artifacts differing in a way a reviewer would care about?
      5 every integration criterion names the artifact, its location, and the
        assertion that pins it
      4 concrete; one or two open naming choices no reviewer would litigate
      3 the goal is unambiguous, the artifact's shape is not
      2 stated as an outcome rather than as an artifact
      1 a problem and a direction; "done" is not written down
      0 open-ended, or deferring its own definition
      A criterion reading "documented", "considered" or "reviewed" with no
      named artifact caps this at 3.

  OS  ORACLE STRENGTH — the check that would actually gate the merge.
      5 exact comparison against an expected artifact spanning children that
        pre-exists the implementation or is independently derived (task
        rule 11); an algebraic property across the composed boundary; or a
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
      DEDUCTIONS, CUMULATIVE, floor 0. Deduct 2 if the same change authors
      both the implementation and the expected values certifying it (task
      rule 11 defines "the same change" and the independently-derived
      exemption). Deduct
      1 if the acceptance evidence is a document asserting a measurement.
      `os:` records the score AFTER deductions; `os_deduction:` records the
      total deducted, 0-3.

  BR  BLAST RADIUS — everything this feature's own integration work must move, generated files,
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
      for children; that is an ordering dependency, recorded in the roster
      and in `blocked_by`. "Every child closed" is the roster, not a
      criterion — the criteria must assert something no child asserts.

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
      REQUIRES, not only what the integration criteria happen to list.
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
  roster (`requires_tasks`); an entry counts as delegable when its own
  block records band A or B. Unfiled planned entries are
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
roster_delegable: # k/n over requires_tasks, or `pending`
```

- [ ] Adversarial review of this issue found no substantial finding
- [ ] Peer review of this issue found no substantial finding

<!-- One or two sentences: which children are delegable today, which
     child is costing the roster most, and — if ED<=1 — which part of the
     integration evidence needs a person or a device. -->
