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
  start — to gate children, block them directly.

  RULES — the scientific-task template's rules 1–7 apply here, adapted
  to this tier: evidence with file:line at a pinned commit (1); no
  padding — "N/A — <reason>" (2); claims stated observably (3); atomic
  scope (4); cross-references cite section NAMES, resolved against the
  headings of the cited issue's template, same-body ones against this
  one — headings carry no numbers (5); executor re-verification of
  evidence before acting, here § Pickup Checks (6); explicit labels,
  here `tier:feature` plus `bug` or `enhancement` matching the corpus
  (7). Task rules 9–10 (comment protocol, waivers) and 14–15 (gates,
  check-sheets) apply with the substitutions below; task rules 11–13
  (oracle custody, artifact paths resolve at filing, marked open
  questions) apply to § Integration Criteria & Evidence Plan and § Open
  Questions & Decisions Needed.

    task tier                            | this tier
    -------------------------------------|--------------------------------
    `AMENDED:`                           | `REPLAN:`
    `HANDOFF: split`                     | a REPLAN plus a new issue at
                                         |   this tier; the REPLAN carries
                                         |   the ledger moving each item to
                                         |   it
    `HANDOFF: re-tier`                   | a REPLAN plus a new issue at the
                                         |   new tier; that REPLAN carries
                                         |   the ledger, gives every roster
                                         |   entry the new issue cannot
                                         |   hold its § Re-planning
                                         |   Protocol disposition, and is
                                         |   the close-out record (task
                                         |   rule 10)
    `HANDOFF: transfer`                  | unchanged — the integration
                                         |   work changing hands
    listing feature (`requires_tasks`)   | serving capstone
                                         |   (`requires_features`;
                                         |   `serves_capstones` mirrors it)
    a listing feature's § Global         | a serving capstone's § Cross-
      Invariants entry, a handoff it     |   Feature Integration Risks
      assigns to this task, or a surface |   mitigation or shared
      its contract declares              |   interface, or § System-Level
                                         |   Acceptance Criteria artifact,
                                         |   assigned to this feature
    disposition freed / re-homed,        | released / re-parented (the
      received from a parent             |   capstone § Re-planning
                                         |   Protocol)
    § Interface & Data Contract          | § Feature-Level Interface & Data
                                         |   Contract
    § Scope Boundary                     | § Capability Statement & Scope
                                         |   Boundary
    § Method / Experimental Design step  | integration step of § Integration
                                         |   Criteria & Evidence Plan
    § Post-Implementation Validation     | § Post-Integration Validation
    sibling scope (§ Related Work)       | § Capability Statement & Scope
                                         |   Boundary (owner of adjacent
                                         |   work), § Sequencing &
                                         |   Parallelism (edges); its
                                         |   listing line → `serves_capstones`
                                         |   (rule C)

  In addition:

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
     with a `REPLAN:` comment (task rule 9, substituted). Bookkeeping at
     this tier also covers: flipping a roster Status cell; refreshing
     `roster_delegable` from the children's current blocks, citing the
     comment that re-scored each; regenerating the mermaid graph from
     the machine block; adding or removing a `serves_capstones` entry
     that mirrors a capstone's authoritative `requires_features`, citing
     it; and resolving a `planned_tasks` scope to its
     number when the filed child's § Related Work (a filed feature: its
     § Intent & Alignment) names this feature's planned scope and its
     body satisfies every gate clause here that
     reads it — the resolver posts `STATUS: progress — resolved
     "<scope>" to #T` here, saying the cited sections were read;
     otherwise the resolution is a REPLAN, posted on the child as below.
     Edges live on the
     authoritative side: whoever needs an edge on another issue edits
     that issue's block and posts the REPLAN or AMENDED there. A REPLAN
     that drops a child from `requires_tasks`, adds an already-filed
     one (a filing that lists an already-filed OPEN child posts the same
     notice on it, as `REPLAN:` led by this number, citing the filing),
     resolves a planned scope to a filed child, answers a child's
     re-plan request, or changes a § Global Invariants entry or a handoff
     assigned to a filed child, is also posted, led by this number, on
     each OPEN child the change binds — the dropped, added, resolved or
     requesting child, both parties to the handoff, every roster child
     for an invariant — which answers
     per task rule 9 (a closed child
     receives it as a notice at most; the REPLAN cites its close-out
     comment as the disposition).
  D. State lives in comments, not checkboxes. Children mirror their
     prefixed comments here as task rule 9 says (`STATUS: pickup` and
     `STATUS: progress` stay on the child); comments a child posted
     before this roster listed it are read on the child. This feature
     mirrors every prefixed comment of its own except those two, led by
     this number and in task rule 9's form, on every OPEN capstone whose
     `requires_features` lists it. A child's mirrored AMENDED that
     contradicts § Global Invariants or a handoff, or declares a modified
     or provided surface § Feature-Level Interface & Data Contract does
     not, is a re-plan request answered ON RECEIPT by a REPLAN here, not
     deferred to pickup; so is a serving capstone's REPLAN posted here
     that assigns this feature an artifact, risk mitigation or re-homed
     scope (the last under rule G(a)). A mirrored comment
     that changes
     nothing cited here needs no answer.

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
         stay `[]` at filing: they carry mirrors only.
     -->

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
                        #   each to its number when filed (rule C). A
                        #   non-empty planned_tasks means this feature's rule B
                        #   sufficiency argument is PROVISIONAL and the feature
                        #   is not Ready.
blocked_by: []          # ordering: features or tasks that must land first
                        #   (never capstones — upward edges are illegal)
blocks: []              # mirrors only — the counterpart's blocked_by is authoritative:
                        #   features or capstones whose blocked_by names this feature.
                        #   Left [] at filing; G09 reports drift.
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
     Alignment and § Hypothesis (falsifiable) as they read NOW. Below
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
     (GitHub math rendering), never prose alone. -->

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

     EVERY CRITERION NAMES ITS NEXT MOVE ON FAILURE: "if not-Y after all
     spanning children landed → fix child by REPLAN (scope stated) |
     `REFUTED:` — the premise of § Capability Statement & Scope Boundary
     fails". A feature's `REFUTED:` is that premise failure: the comment
     quotes the failing criterion and carries the command and output,
     and the sheet is not completed — rows already filled stand and the
     REFUTED links the revision holding them (task rule 10). Where
     roster entries are still open or planned, the REFUTED follows a
     REPLAN giving each its § Re-planning Protocol disposition (posted
     on each filed child, rule C) and cites it.

     ANNOTATE OWNERSHIP PER CRITERION. A blanket sentence ("none of
     these is covered by any single child alone") is not checkable and
     is forbidden; give every criterion exactly one of:
       - "spans #A + #B" — and state what each contributes, so the
         claim can be falsified by reading either child.
       - "covered alone by #A" — honest, and it simply does not count
         toward rule B. Listing it is fine; miscalling it a span is not.
       - "no child; built by this feature's close-out".
       - "UNOWNED — no builder yet" — a real gap, worth stating.

     -->

- [ ] **Gate — Decomposition, Contract & Integration.** Every FILED child body was read at its current revision; every filed roster row's one-line contract matches the child's own § Intent & Alignment and § Hypothesis (falsifiable); every handoff names provider child, consumer child and both contract subsections by name, and each filed child's contract actually declares its side; no two children hand off in both directions (a mutual handoff is re-cut with an interface-first child, or the consuming child of one direction declares a private stub of that interface under its own § Internal interfaces provided — private so that direction carries no `blocked_by` edge, and the handoff names the child that later retires the stub); every feature-boundary transformation is fully defined in math; every integration criterion carries exactly one ownership annotation and names its next move on failure, and at least one is a genuine span or close-out criterion (rule B); no child claims a criterion here as its own deliverable; every artifact a criterion names exists at `evidence_commit` or has a named builder; every expected value names its custodian, date and provenance; the rejected decompositions are stated. `requires_tasks` and `planned_tasks` are filled, together non-empty, every planned scope verified absent at `evidence_commit`, every filed child's tier is task. Adversarial re-read of everything above found no substantial finding.

## Global Invariants

<!-- What EVERY child must preserve at every intermediate landing —
     e.g. historical `.jls` files still load, save output
     byte-identical unless a version bump is declared, `mvn verify`
     green, no new SpotBugs exclusions. Each stated so a test can pin
     it at any landing. -->

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

- [ ] **Gate — Invariants & consequences.** Every invariant is pinnable by a test at any intermediate landing; for every filed child, its § Compatibility, versioning & migration was read and contradicts none of these; for every child another feature's roster also lists, that feature's § Global Invariants were read and none contradicts these; every interface the feature-level contract moves has its consequence stated; every cost identified is stated or the section says there is none and why; no consequence was accepted that § Intent & Alignment would not justify. Adversarial re-read of everything above found no substantial finding.

## Sequencing & Parallelism

<!-- The critical path through the roster, which tasks are mutually
     independent (safe to execute concurrently by separate agents), and
     any ordering that is convention rather than necessity — marked as
     such, so a scheduler may break it knowingly. Every necessary order
     here is a `blocked_by` edge in the child's own machine block; fill
     this feature's `blocked_by` and the mermaid graph now and record
     the DAG walk (`blocks` and `serves_capstones` stay empty or
     mirrors-only). -->

- [ ] **Gate — Sequencing & edges.** Every ordering a serving or planning capstone's § Cross-Feature Integration Risks records against this feature is in `blocked_by` here, or — where a filed feature must wait on this one — was added to it by that feature's REPLAN adding the edge, posted on it by whoever files this feature (rule C) and cited here, or the other feature is still planned and the capstone's § Cross-Feature Integration Risks records the edge for its filing; every necessary ordering is an edge in the filed children's machine blocks, or is recorded here to be added when a planned child is filed, or — for an ordering both of whose children landed before this feature existed — § Sequencing & Parallelism records the landing order in place of the edge (an open child waiting on a landed one still carries the edge in its `blocked_by`), and appears in the mermaid graph; tasks called mutually independent share no handoff in § Feature-Level Interface & Data Contract other than one stubbed under Gate — Decomposition, Contract & Integration (the retiring child is then ordered after both); convention-only orderings are marked; `blocked_by` and `related` are filled, `blocks` and `serves_capstones` are empty or carry only mirrors, no edge points upward, the DAG walk is recorded, and the mermaid graph agrees with the machine block (rule A). Adversarial re-read of everything above found no substantial finding.

## Re-planning Protocol

<!-- What invalidates this plan and the required response. At minimum:
     a child REFUTED → which siblings' premises are affected and who
     re-plans; a child split → the REPLAN task rule 9 requires of every
     listing feature; a child re-tiered → a REPLAN dropping it with
     disposition closed, citing the HANDOFF (its handoffs re-cited to
     the new feature's children where this feature still needs them, and
     the serving capstone applies rule G to the new issue); a child
     transferred → no plan change; a contract
     deviation → § Feature-Level Interface & Data Contract
     reconciliation; an integration criterion, or a § Global Invariants
     entry, fails with every spanning child landed → the criterion's
     named next move (a REPLAN adding the fix child, or `REFUTED:` as
     § Integration Criteria & Evidence Plan says; an invariant's next
     move is always the fix child, since it is no premise; a criterion or
     contract stage found wrong, the children being right → corrected by
     REPLAN with the evidence, task rule 2, no next move fired); a serving
     capstone's REPLAN assigning this feature an artifact, risk
     mitigation or re-homed scope → rule D; a child's mirrored AMENDED or
     WAIVED → roster row and handoffs re-derived (rule D); a child's
     landing or AMENDED after which no criterion in § Integration
     Criteria & Evidence Plan is a genuine span or close-out criterion
     (rule B) → once the remaining roster has landed, close with
     `SUPERSEDED: — label, not a feature (rule B); every criterion
     covered alone by #…`, mirrored
     to serving capstones, which re-derive their sufficiency citing the
     landed work by permalink as preconditions; a shared child's other
     listing feature adds or changes a § Global Invariants entry that
     conflicts with one here → the feature whose invariant is newer
     re-plans; a serving capstone descoped, or this feature released
     from one → whether this feature still has a beneficiary (none: the
     answering REPLAN closes this feature under task rule 10); a child
     dropped from the roster or this feature descoped → the REPLAN gives
     EACH affected child a disposition:
     re-homed (an OPEN feature already listing it, named; one not yet
     listing it adopts it by its own REPLAN — rule C, posted on the child
     — which this REPLAN cites), freed
     (in no roster; the child answers per task rule 9), or closed; a
     planned scope is moved to a named open feature's `planned_tasks`,
     or dropped with the § Decomposition & Rationale argument (rule B)
     re-derived; closing with scope a serving capstone still needs →
     that capstone's rule G disposition, cited in the closing REPLAN. -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself. For each: the question, options
     with a recommended default, and whether it blocks filing children,
     blocks integration, or can ride along. "N/A — fully specified" if
     nothing is open.

     Mark every entry per task rule 13 — `Recommended default:`,
     `PROPOSED:`, `BLOCKING:` or `HYGIENE:`. -->

- [ ] **Gate — Re-planning & decisions.** Every trigger named in § Re-planning Protocol has a response that ends in a REPLAN comment where anything cited changed, and names which sections it re-derives; the protocol covers every child in the roster and every serving or planning capstone (alignment targets, and any whose `planned_features` carries this scope); every open question carries exactly one task rule 13 marker; every decision the integration will hit is listed or settled above; nothing marked `Recommended default:` contradicts the contract or an invariant. Adversarial re-read of everything above found no substantial finding.

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
- [ ] Every OPEN capstone whose `requires_features` lists this feature notified with a `STATUS: landed` comment citing the REPLAN of any contract deviation those capstones must reconcile (`serves_capstones` mirrors that set) [row: Mirrors]
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A) [row: Roster]
- [ ] Every decision in § Open Questions & Decisions Needed is resolved or explicitly deferred, none left blocking [rows: Open question]
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10) [row: Waivers]
- [ ] Every cited evidence document and permalink resolves on the default branch at close [row: Links]
- [ ] Every artifact named above exists at `evidence_commit` or is created by its named builder — this list says which (task rule 12) [row: Paths]
- [ ] § Agentic Delegability re-scored on any `REPLAN:` edit that changed the roster, the integration evidence, or the open decisions [row: Re-plans]
- [ ] ... [row: <added below>]

## Pickup Checks

<!-- Run once per executor by whoever begins the integration work of
     § Integration Criteria & Evidence Plan (task rule 6, applied at this
     tier). The
     rows are fixed and never deleted: a row whose referent is N/A or
     empty is recorded "none" in the `STATUS: pickup` comment here (not
     mirrored; one line may list every "none" row) and neither passes
     nor fails; that comment names the checkout commit and lists the
     rows run and the rows not reached. A failed row ends in that comment
     naming the integration steps blocked (or all of them) and what
     unblocks them; only those steps wait. -->

- [ ] `action` read and followed per the task template's `action` pickup row (at this tier a SPLIT is a REPLAN plus a new issue)
- [ ] Every `REPLAN:` that edited this body read (own, or a counterpart's posted here led by its number); each names the sections changed and re-read, and the body matches their fold in stream order, bookkeeping edits aside — a mismatch is repaired by re-applying the fold from the comments and the body's edit history before proceeding; where `review_clean` is `false`, the finding at `review_evidence` is answered before integration begins — by the REPLAN fixing it, or by a comment quoting it and recording why it does not stand
- [ ] Every `REPLAN:` a serving capstone posted here read and answered by REPLAN where anything cited changed (rule D)
- [ ] Every child in `requires_tasks` has a `STATUS: landed` comment mirrored here or on the child, and no later `STATUS: landed` or `AMENDED:` notice on the child qualifies what it landed without a REPLAN here reconciling it or naming the successor it tracks, or its disposition is recorded; `planned_tasks` is empty
- [ ] Every `AMENDED:`, `HANDOFF:` or `WAIVED:` from a child read (mirrored here, or on the child for one posted before this roster listed it), and every `REPLAN:` another listing feature posted on a shared child; each AMENDED checked for contract deviations (recorded only by AMENDED, task rule 9), including surfaces the feature-level contract does not declare; roster rows, handoffs, § Global Invariants and § Feature-Level Interface & Data Contract re-derived by REPLAN where they changed
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
     added completion criterion) with evidence cells empty; the rows of
     pre-filled criteria marked N/A are deleted at filing unless another
     criterion names the row. Evidence is a command with its output, a test
     name at a commit, a permalink, or a comment link — never "done". -->

| Item | Check | Evidence | Result |
|------|-------|----------|--------|
| Capability | do X at the final commit, observe Y, as § Capability Statement & Scope Boundary states | | |
| Scope | integrated diffs stay inside the boundary; extras filed as # | | |
| Integration criterion 1 | do X at the named commit, observe Y; each spanning child's contribution shown | | |
| Contract: modified / consumed / provided | declared at the feature boundary vs. observed in the integrated code | | |
| Contract: durable / ephemeral / concurrency | as declared | | |
| Contract: transformations | each defined stage located across the children's diffs, and their composition observed to be the declared math | | |
| Contract: handoff #A → #B | provider side and consumer side both present as declared | | |
| Invariant 1 | re-verified at the final commit: command and output | | |
| Child #A | landed — mirror here, or the landing comment on the child; later `STATUS: landed` or `AMENDED:` notices on the child re-read at close; deviations reconciled | | |
| Open question 1 | resolving comment, or permalink to the diff landing the `Recommended default:` / the re-derived `HYGIENE:` item | | |
| Oracle custody | each expected value: who, when, pre-committed / independently derived / produced by the implementation and deducted | | |
| Roster | machine block, table and mermaid agree; `planned_tasks` empty | | |
| Mirrors | `STATUS: landed` posted on every open capstone whose `requires_features` lists this feature, cited | | |
| Links | every permalink and document resolves on the default branch | | |
| Paths | each artifact a criterion names exists at the final commit, created by its named builder (task rule 12) | | |
| Waivers | every skipped criterion has its `WAIVED:` comment, or after close the finder's AMENDED (REPLAN) standing in for it (task rule 10) | | |
| Re-plans | every `REPLAN:` comment reconciled; the body describes what was built; ADR re-scored where a re-plan changed roster, evidence or decisions | | |

- [ ] Adversarial review of the integrated result against this issue found no substantial finding

- [ ] **Gate — Criteria & sheets.** Every completion criterion names its artifact, location and assertion; every path a criterion names exists at `evidence_commit` or has a named builder (task rule 12); every pre-filled criterion not marked N/A has its bracketed rows (a per-item row family with zero items satisfies its bracket) and every added criterion, integration criterion, contract item, invariant, child and open question has a validation row, and no row remains for an N/A pre-filled criterion that no other criterion names; nothing in the sheets contradicts § Sequencing & Parallelism or § Capability Statement & Scope Boundary. Adversarial re-read of everything above found no substantial finding.

## Abstract

<!-- Written last, read first. 2–4 sentences: the capability, why it
     matters, and the one-line shape of the decomposition — drawn from
     § Intent & Alignment, § Capability Statement & Scope Boundary and
     § Decomposition & Rationale, contradicting none of them. -->

## Agentic Delegability (ADR-1)

<!--
  ADR-1 v5, composition tier. How safely this feature can be handed to an
  automated executor, and what maintenance liability delegating it as-is
  would create.
  SCORE THIS FEATURE'S OWN DELIVERABLE — its integration evidence and its
  declared contract — never the union of its children; each child carries
  its own block.
  Fill at filing; re-score when a re-plan or amendment before close changes
  the roster, the integration evidence or the open decisions. Where two ANCHORS in one axis
  could apply to one fact, take the lower. Caps and deductions stated inside
  an axis apply on top of the anchor chosen; they are not in competition
  with it.
  Self-contained by design; scripts/tests/test_adr_blocks.py checks the
  three tier copies agree.

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
      and in `blocked_by`.

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
      REQUIRES, not only what the integration criteria happen to list.
      5 self-contained: the project's standard check command, or a script
        already in the tree, produces it ......................... no cap
      4 needs a pinned toolchain the project can fetch and reproduce no cap
      3 needs an unreliable substrate, or an external corpus to download
        .......................................................... cap B
      The last three are disjoint on one question: could automation ever
      produce this unattended?
      2 YES, but not with what the executor has — another host platform, a
        device class, a credential that automation COULD be given, or a
        fixture withheld until a named disclosure event (task rule 2) .
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
        judgement of what they saw is the report, an external publisher whose
        acceptance is itself the evidence .......................... cap F
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
  roster (`requires_tasks`); an entry counts as delegable when its own
  block records band A or B. Unfiled planned entries are
  not counted; where any exist the fraction is provisional and the prose
  line must say so. Score `pending` when the roster is empty or its
  children are not yet scored, and name in the prose line which entry is
  closest to delegable.

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

  A low band is a routing decision, not a criticism. Issues can be excellent
  work and band F because closing them needs a person or a device.

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
roster_delegable: # k/n over requires_tasks, or `pending`
```

- [ ] Adversarial review of this issue found no substantial finding
- [ ] Peer review of this issue found no substantial finding

<!-- One or two sentences: which children are delegable today, which
     child is costing the roster most, the checkpoint where `action` is
     DELEGATE-WITH-CHECKPOINT, and — if ED<=1 — which part of the
     integration evidence needs a person or a device. -->
