---
name: Feature
about: A coherent capability composed of scientific tasks — owns the decomposition, the feature-level contract, and the integration evidence that its tasks jointly deliver it
labels: ["tier:feature"]
---

<!--
  Template: feature v3 (2026-08)

  TIER MODEL — task → feature → capstone. Edge legality (composition
  and ordering edges; `related` is reference-only and may point at ANY
  tier from any tier):
    - Tasks (scientific_task template) may hold edges to tasks and
      features, never to capstones.
    - Features may hold edges to tasks, other features, and capstones.
    - Capstones hold edges to features and sub-capstones; to tasks
      only via the recorded orphaned-scope exception (capstone rule G).
  An issue's tier is defined by its machine block's `tier:` key; the
  tier:* label is a mirror for filtering — a missing or stale label is
  bookkeeping to fix, never an edge violation.
  Edge kinds are distinct and every link declares which it is:
    - Composition: a task is part_of at most ONE feature; a capstone
      requires_features. Single-owner for tasks — the task's
      part_of_feature field is authoritative, and a roster that
      disagrees must REPLAN. A feature may serve any number of
      capstones; there the capstone's requires_features is
      authoritative and serves_capstones mirrors it.
    - Ordering: blocked_by / blocks. THE ORDERING GRAPH IS blocked_by/
      blocks PLUS every composition edge read child-before-parent (a
      parent cannot close before its children land). That combined
      graph must stay a DAG at the instance level, across all tiers —
      tier legality alone does not prevent a cycle (a capstone
      requiring feature A while A is blocked_by that capstone; a task
      blocked_by its own parent feature). Before adding an edge, walk
      the machine blocks of the issues it names — following their
      listed edges outward — and confirm no path leads back here;
      record that walk in the filing or REPLAN comment. A cycle is a
      filing defect.
    - Blocking a composite: an ordering edge aimed at a feature or
      capstone gates that issue's integration/close-out only, never
      its children's start — to gate children, block them directly.
    - Reference: related — non-blocking, informational only.

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

  QUOTING THROUGH THIS TRACKER — settled by experiment 2026-08-02.
  Do not re-derive it, and do not "fix" what it explains.
    - The API read path that tooling uses (mcp__github__issue_read and
      equivalents) DELETES tag-shaped runs — anything matching
      `<word...>` — from the body it hands back, INCLUDING inside fenced
      code blocks. Java generics (List of ElementType written with angle
      brackets), XML element names, and HTML tags all vanish from the
      READ. Angle brackets that are not tag-shaped (`1 << 22`, `A>`,
      mermaid's `-->`) survive.
    - The STORED body is NOT affected. The rendered issue page carries
      the intact text. Confirmed by comparing the two paths on this
      repository's own issues.
    - Therefore WRITE the byte-exact line. Never substitute lookalike
      characters for angle brackets, never paraphrase a generic type
      argument into prose, and never add a note claiming this tracker
      strips generics — it does not, and the note is then itself a false
      claim sitting in the issue body.
    - Empty backticks, or a bare `List` where a generic belongs, in a
      body you READ are a read artifact and not a defect. Do not report
      one and do not repair one. Judge any quote containing angle
      brackets against the source file at `evidence_commit`, which is
      the authority — the evidence rule is satisfied there, not in a
      read-back.

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
planned_tasks: []       # one-line scopes for children not yet filed; verify each
                        #   scope is ABSENT at evidence_commit before listing it
                        #   (a landed scope is Background, not a plan); resolve
                        #   each to a number via REPLAN when it is filed
blocked_by: []          # ordering: tasks, features, or capstones that must land first
blocks: []              # ordering: issues waiting on this feature
serves_capstones: []    # capstones whose required set includes this feature
related: []             # reference only — never blocking
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
     this issue's close-out) builds them. -->

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
     child a disposition: re-homed (new part_of_feature), freed
     (part_of_feature: none), or closed — no dangling owners. Closing
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
     "N/A — fully specified" if nothing is open. -->

## Completion Criteria (Definition of Done)

- [ ] Every entry in `requires_tasks` closed as landed, or descoped via a `REPLAN:` comment with the roster updated and each child's disposition recorded; `planned_tasks` empty (each resolved to a filed issue or descoped)
- [ ] Every cited evidence document and permalink resolves on the default branch at close
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10)
- [ ] Every prediction in §5 (Integration Criteria & Evidence Plan) verified at a named commit; command and output recorded in a closing comment
- [ ] §3 (Feature-Level Interface & Data Contract) re-checked against the integrated result; deviations recorded, none silently absorbed
- [ ] §4 (Global Invariants) hold at the final commit, re-verified — not inferred from children's green runs
- [ ] Every capstone in `serves_capstones` notified with a `STATUS:` comment
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A)
- [ ] ...
