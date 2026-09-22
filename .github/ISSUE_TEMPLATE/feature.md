---
name: Feature
about: A coherent capability composed of scientific tasks — owns the decomposition, the feature-level contract, and the integration evidence that its tasks jointly deliver it
labels: ["tier:feature"]
---

<!--
  Template: feature v4 (2026-08)

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
  task, read the rosters; the task itself does not claim an owner.
  (v3 and earlier imposed a single-owner rule via a task-side
  `part_of_feature` field. That was never the intended model. The rule
  was fully obeyed — at the time of the v4 correction no open task had
  more than one owner — so the cost is not a corrupted corpus but an
  unrepresentable one: wherever work genuinely served several features,
  the plan could not say so, and the relationship could only be noted
  in `related`, which carries no ownership. The field is retired; each
  task's single claim was preserved into its owning feature's roster
  first, so nothing was lost.)

  A feature may likewise serve any number of capstones; there the
  capstone's requires_features is authoritative and serves_capstones
  mirrors it.

  An issue's tier is defined by its machine block's `tier:` key; the
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
  scope (4); cross-references cite section NAMES, resolved against
  THIS template's own headings (5); executor re-verification of
  evidence before acting (6); explicit labels, here `tier:feature` (7).
  Where a task rule names a task-tier section (Observations, Method),
  apply it to the analogous section here. Task rules 9–10 (comment
  protocol and waivers) also apply, with `REPLAN:` in place of
  `AMENDED:`. In addition:

  A. The machine block in Status & Dependency Graph is the source of
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
     with a `REPLAN:` comment stating what changed and why.
     Bookkeeping is exempt: flipping a roster Status cell, ticking a
     DoD box whose backing evidence is already recorded in a comment
     or PR, and re-pinning evidence_commit. If another agent edited
     since you read, re-read and fold the newer body into your edit —
     the REPLAN comment stream is the arbiter of intent.
  D. State lives in comments, not checkboxes. Child tasks post
     `STATUS:` (landed / progress), `REFUTED:` (hypothesis failed,
     with evidence), `HANDOFF:` (work split or transferred),
     `SUPERSEDED:` (already shipped) on their own issue AND mirror the
     same comment here; this feature mirrors its own landing or
     refutation as a `STATUS:`/`REFUTED:` comment on every capstone in
     serves_capstones. A fresh agent reconstructs execution state from
     the machine block plus the prefixed comments; checkbox state is a
     convenience rendering, never evidence.
-->

## Abstract

<!-- 2–4 sentences: the capability, why it matters, and the one-line
     shape of the decomposition. -->

## Intended Audience & Impact

<!-- Same discipline as the task template: name the concrete
     audience(s) served and the change they experience once the whole
     feature — not any single task — has landed. -->

## Status & Dependency Graph

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
                        #   each to a number via REPLAN when it is filed. A
                        #   non-empty planned_tasks means this feature's rule B
                        #   sufficiency argument is PROVISIONAL and the feature
                        #   is not Ready.
blocked_by: []          # ordering: features or tasks that must land first
                        #   (never capstones — upward edges are illegal)
blocks: []              # ordering: features or capstones waiting on this feature
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

## 1. Capability Statement & Scope Boundary

<!-- What the feature makes true, phrased observably, and explicitly
     what is OUT of scope — the adjacent work an executor might be
     tempted to absorb, with the issue that owns it instead. -->

## 2. Decomposition & Rationale

<!-- One row per child task. The one-line contract is the handoff
     summary an agent reads before deciding whether to open the child
     at all. Below the table: why these cuts and not others — the
     alternative decompositions considered and rejected, so a
     re-planning agent does not re-derive them from scratch. -->

| Task | One-line contract | Status |
|------|-------------------|--------|
| #    |                   |        |

## 3. Feature-Level Interface & Data Contract

<!-- §7 (Interface & Data Contract) of the task template, applied at
     the feature boundary: what the feature as a whole modifies,
     consumes, provides, tracks durably, uses ephemerally, its
     concurrency model, and its transformations — stated so the
     integrated result can be checked against it. Then the internal
     handoffs: which child provides which interface to which sibling,
     citing the child's contract subsection by name (e.g. "#101
     provides `ElementRegistry` per its Internal interfaces provided —
     public; consumed by #102"). Transformations at the feature
     boundary follow the task template's §7.10 (Data transformations)
     discipline: fully defined and expressed in embedded LaTeX math
     (GitHub math rendering), never prose alone. When a child lands
     with a contract deviation, this section is stale until a REPLAN
     comment resolves it. -->

## 4. Global Invariants

<!-- What EVERY child must preserve at every intermediate landing —
     e.g. historical `.jls` files still load, save output
     byte-identical unless a version bump is declared, `mvn verify`
     green, no new SpotBugs exclusions. Children cite this section
     instead of restating it. -->

## 5. Integration Criteria & Evidence Plan

<!-- Feature-level predictions: do X, observe Y — each one something
     no single child's completion criteria assert (rule B). Name the
     integration test, golden file, or recorded manual procedure that
     pins each, and note which do not exist yet and which child (or
     this issue's close-out) builds them.

     ANNOTATE OWNERSHIP PER CRITERION. Do NOT open this section with a
     blanket sentence like "none of these is covered by any single
     child alone" and then leave the individual criteria unattributed.
     A 2026-08 audit of all 173 open features found that pattern is the
     single highest-yield defect in the corpus: features carrying a
     blanket preamble almost always had at least one criterion that a
     child's own Definition of Done covered end to end, while features
     that attributed each criterion individually had almost none. A
     blanket claim is not checkable, so it does not get checked.

     Instead, give every criterion one of:
       - "spans #A + #B" — and state what each contributes, so the
         claim can be falsified by reading either child.
       - "covered alone by #A" — honest, and it simply does not count
         toward rule B. Listing it is fine; miscalling it a span is not.
       - "no child; built by this feature's close-out".
       - "UNOWNED — no builder yet" — a real gap, worth stating.

     WRITE THIS AGAINST THE CHILD'S ACTUAL TEXT, not from memory or
     from the plan you filed it under. Several features in this corpus
     record that they were written without reading their children
     ("child issue body not read during this migration"), and their
     span claims are wrong as a direct result. If a child later amends
     its scope by REPLAN, this section is stale until re-derived.

     A child that cites one of these criteria by number as its own
     deliverable is proof the criterion is not a span; validator check
     G22 reports that case. -->

## 6. Sequencing & Parallelism

<!-- The critical path through the roster, which tasks are mutually
     independent (safe to execute concurrently by separate agents),
     and any ordering that is convention rather than necessity —
     marked as such, so a scheduler may break it knowingly. -->

## 7. Re-planning Protocol

<!-- What invalidates this plan and the required response. At minimum:
     a child REFUTED → which siblings' premises are affected and who
     re-plans; a child split (HANDOFF) → roster update; a contract
     deviation → § Feature-Level Interface & Data Contract
     reconciliation; a serving capstone descoped → whether this
     feature still has a beneficiary; a child dropped from the roster
     or this feature descoped → the REPLAN comment gives EACH affected
     child a disposition: re-homed (added to another feature's
     requires_tasks), freed (in no roster — legal, it is simply
     unowned), or closed. Because a task may be shared, dropping it
     from THIS roster does not orphan it if another roster still lists
     it — check before assuming a disposition is needed. Closing
     this feature with scope UNMET while a serving capstone still
     needs it → each unmet scope item gets a disposition in the
     closing REPLAN: re-homed into another required feature, filed as
     a task the capstone adopts via its rule G orphaned-scope
     exception, or descoped with the capstone's sufficiency argument
     re-derived — never silently dropped. Every response ends in a
     REPLAN comment here. -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself. For each: the
     question, options with a recommended default, and whether it
     blocks filing children, blocks integration, or can ride along.
     "N/A — fully specified" if nothing is open.

     Mark every entry the way the task template's rule 13 requires —
     `Recommended default:`, `BLOCKING:`, or `HYGIENE:`. The marker is
     what an executor and § Agentic Delegability both read; an unmarked
     entry is treated as blocking. -->

## Completion Criteria (Definition of Done)

- [ ] Every entry in `requires_tasks` closed as landed, or descoped via a `REPLAN:` comment with the roster updated and each child's disposition recorded; `planned_tasks` empty (each resolved to a filed issue or descoped)
- [ ] Every cited evidence document and permalink resolves on the default branch at close
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10)
- [ ] Every prediction in §5 (Integration Criteria & Evidence Plan) verified at a named commit; command and output recorded in a closing comment
- [ ] §3 (Feature-Level Interface & Data Contract) re-checked against the integrated result; deviations recorded, none silently absorbed
- [ ] §4 (Global Invariants) hold at the final commit, re-verified — not inferred from children's green runs
- [ ] Every capstone in `serves_capstones` notified with a `STATUS:` comment
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A)
- [ ] § Agentic Delegability filled at filing and re-scored on any `REPLAN:` or `AMENDED:` edit that changed the roster, the integration evidence, or the open decisions
- [ ] ...


## Agentic Delegability (ADR-1)

<!--
  ADR-1 v1 (2026-09), feature tier. THE ANCHOR TABLE IS CANONICAL IN THE
  SCIENTIFIC-TASK TEMPLATE — read the nine axis definitions, the band
  arithmetic and the debt tags there; they are not restated, the same way
  this template's edge-legality matrix is not restated in that one. Only
  the feature-tier deltas are below.

  This section rates DELEGABILITY TO AN AGENTIC LLM SYSTEM, not quality,
  importance or urgency. A low band is a routing decision, not a criticism.

  DELTA 1 — SCORE THIS FEATURE'S OWN DELIVERABLE, NOT THE UNION OF ITS
  CHILDREN. A feature's own work is the integration evidence in §5
  (Integration Criteria & Evidence Plan) and the contract in §3
  (Feature-Level Interface & Data Contract). Score SC, OS, ED, DA and RD
  against those. Do not roll up the children's scope: each child carries
  its own block.

  DELTA 2 — BR, HL AND CF ARE COMPOSITION-DOMINATED AND WILL BE LOW. A
  feature that mostly composes tasks scores BR 0-1, HL 0-1 and CF 0-1 by
  construction, because its own dependent-step chain is "wait for the
  roster, then verify". That is the tier working as designed. Do not
  inflate them to make the band look better, and do not read them as a
  defect: the feature's own band answers only "can this be handed over as
  ONE unit", and for a composing feature the answer is structurally no.

  DELTA 3 — THE NUMBER THAT ACTUALLY MATTERS IS `roster_delegable`. Of the
  issues in `requires_tasks`, how many are individually band A or B? That,
  not this feature's own band, predicts whether the feature can be executed
  by delegation at all.
    - k/n with k>=1  : at least one child can be handed over today; name
                       which in the prose line.
    - 0/n            : nothing in this roster is delegable as filed. The
                       fix is at the TASK tier — answer a child's Open
                       Question, or pre-commit a child's expected values —
                       not here. Rewriting the feature will not move it.
    - roster empty   : score `pending` and re-score when children are filed.
  Measured across the 688 open issues in 2026-09: the aggregate delegable
  fraction over all 632 parent->child roster edges was 8/632, and no
  capstone had a single A- or B-band child. A 0/n roster is the normal
  case on this backlog, not an alarm — but it is the number worth watching,
  because decomposition that does not produce delegable leaves has not
  bought what decomposition is supposed to buy.

  DELTA 4 — ED IS SCORED ON §5's INTEGRATION EVIDENCE. If the integration
  criteria require a person (an outside reviewer, an instructor, an
  independent reproducer) or hardware, this feature is ED<=1 and bands F
  however delegable its children are. Declaring that at filing time is the
  point: it tells whoever picks it up that closing it is not a coding task.
-->

```yaml
adr: 1
sc:                  # 0-5  specification closure of §5's integration criteria
os:                  # 0-5  oracle strength of the integration evidence
br:                  # 0-5  this feature's OWN blast radius (composition => low)
hl:                  # 0-5  this feature's OWN chain      (composition => low)
pd:                  # 0-5  precedent density
cf:                  # 0-5  this feature's OWN footprint  (composition => low)
rd:                  # 0-5  reversibility / debt surface
raw:                 # sc+os+br+hl+pd+cf+rd, 0-35
ed:                  # 0-5  environmental determinism (CAPPING) — on §5
da:                  # 0-5  design authority          (CAPPING)
band:                # A|B|C|D|F — most restrictive of RAW band, ED cap, DA cap
action:              # DELEGATE | DELEGATE-WITH-CHECKPOINT | SPECIFY-FIRST |
                     #   SPLIT | HUMAN-LED | HUMAN-ONLY | AGENT-ASSIST-ONLY
debt:                # predicted debt mode, or NONE
roster_delegable:    # k/n — requires_tasks entries at band A or B, or `pending`
```

<!-- One or two sentences: which children (if any) can be delegated today,
     which single child's Open Question or missing pre-committed golden is
     costing the roster the most, and — if ED<=1 — what part of §5's
     evidence needs a person or hardware. -->
