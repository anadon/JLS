---
name: Feature
about: A coherent capability composed of scientific tasks — owns the decomposition, the feature-level contract, and the integration evidence that its tasks jointly deliver it
labels: ["tier:feature"]
---

<!--
  Template: feature v1 (2026-08)

  TIER MODEL — task → feature → capstone. Edge legality:
    - Tasks (scientific_task template) may hold edges to tasks and
      features, never to capstones.
    - Features may hold edges to tasks, other features, and capstones.
    - Capstones hold edges to features only — never to tasks directly.
  Edge kinds are distinct and every link declares which it is:
    - Composition: a task is part_of at most ONE feature; a capstone
      requires_features. Single-owner for tasks; a feature may serve
      any number of capstones.
    - Ordering: blocked_by / blocks. Across ALL tiers, ordering edges
      must form a DAG at the instance level — tier legality alone does
      not prevent a cycle (capstone requires feature A while A is
      blocked_by that capstone). Check reachability before adding an
      edge; a cycle is a filing defect.
    - Reference: related — non-blocking, informational only.

  RULES — rules 1–7 of the scientific-task template apply here
  unchanged (evidence with file:line at a pinned commit; no padding —
  "N/A — <reason>"; observable predictions; atomic scope; cite
  sections by name; executor re-verification; explicit labels, here
  `tier:feature`). In addition:

  A. The machine block in Status & Dependency Graph is the source of
     truth for graph assembly. Prose and the mermaid graph elaborate
     it and must agree with it; on conflict, fix the body — do not
     guess.
  B. A feature is not a folder. It must assert at least one
     integration criterion (§5, Integration Criteria) that no single
     child task's completion criteria cover alone. If it cannot, it is
     a label, not a feature — do not file it.
  C. This body is a living plan — unlike a task body, which is frozen
     during execution. Edits are permitted, but every edit is logged
     with a `REPLAN:` comment stating what changed and why. An
     unlogged edit is invisible to agents, which reconstruct state
     from the comment stream.
  D. State lives in comments, not checkboxes. Child tasks report here
     with prefixed comments — `STATUS:` (landed / progress),
     `REFUTED:` (hypothesis failed, with evidence), `HANDOFF:` (work
     split or transferred), `SUPERSEDED:` (already shipped). A fresh
     agent reconstructs execution state from the machine block plus
     the prefixed comments; checkbox state is a convenience rendering,
     never evidence.
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
requires_tasks: []      # composition: part_of children, e.g. [101, 102];
                        # planned-but-unfiled as "TBD: <one-line scope>"
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
     public; consumed by #102"). When a child lands with a contract
     deviation, this section is stale until a REPLAN comment resolves
     it. -->

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
     deviation → §3 (Feature-Level Interface & Data Contract)
     reconciliation; a serving capstone descoped → whether this
     feature still has a beneficiary. Every response ends in a REPLAN
     comment here. -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself. For each: the
     question, options with a recommended default, and whether it
     blocks filing children, blocks integration, or can ride along.
     "N/A — fully specified" if nothing is open. -->

## Completion Criteria (Definition of Done)

- [ ] Every entry in `requires_tasks` closed as landed, or descoped via a `REPLAN:` comment with the roster updated
- [ ] Every prediction in §5 (Integration Criteria & Evidence Plan) verified at a named commit; command and output recorded in a closing comment
- [ ] §3 (Feature-Level Interface & Data Contract) re-checked against the integrated result; deviations recorded, none silently absorbed
- [ ] §4 (Global Invariants) hold at the final commit, re-verified — not inferred from children's green runs
- [ ] Every capstone in `serves_capstones` notified with a `STATUS:` comment
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A)
- [ ] ...
