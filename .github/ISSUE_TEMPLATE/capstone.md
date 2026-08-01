---
name: Capstone
about: A milestone-level outcome gated on a required set of features — owns the system-level acceptance evidence that the features jointly deliver it
labels: ["tier:capstone"]
---

<!--
  Template: capstone v1 (2026-08)

  TIER MODEL — task → feature → capstone; the full edge-legality
  matrix is in the feature template and applies unchanged. The
  capstone-specific consequences:
    - Capstones reference features ONLY. If a capstone appears to need
      a task directly, that task belongs inside one of its features —
      file it there and cite the feature.
    - Ordering between capstones is expressed through features: a
      later capstone's features are blocked_by the earlier capstone.
      No direct capstone-to-capstone edges.
    - Features may be blocked_by capstones, so cycles are possible
      across tiers: verify the ordering graph stays a DAG before
      adding any feature to the required set.

  RULES — rules 1–7 of the scientific-task template and rules A–D of
  the feature template apply unchanged (evidence at a pinned commit;
  machine block as source of truth; living body with `REPLAN:`
  comments; state reconstructed from prefixed comments, never from
  checkboxes; explicit labels, here `tier:capstone`). In addition:

  E. The required set is a closed list with a sufficiency argument
     (§2, Required Feature Set & Sufficiency): why exactly these
     features, together, make §1 (Outcome Statement) true — and why
     none is removable. Adding or removing a feature is a re-plan
     recorded with a REPLAN comment, not a quiet edit.
  F. A capstone must assert system-level acceptance criteria (§4)
     that no single feature's completion criteria cover. If every
     criterion is already owned by a feature, this is a milestone
     label, not a capstone — do not file it.
-->

## Abstract

<!-- 2–4 sentences: the outcome, why it matters, and the one-line
     shape of the feature set that gates it. -->

## Intended Audience & Impact

<!-- The audiences for whom the project is materially different once
     this capstone lands — named concretely, with the change they
     experience at the system level, not per-feature. -->

## Status & Required Features

```yaml
tier: capstone
evidence_commit:        # SHA the roster and acceptance claims are pinned to
requires_features: []   # composition — the closed required set (rule E),
                        # e.g. [78, 84]; planned-but-unfiled as "TBD: <one-line scope>"
related: []             # reference only — never blocking
```

```mermaid
flowchart TD
  %% Arrow A --> B means "A must land before B" (A blocks B).
  %% Show this capstone, its required features, and the ordering
  %% edges among those features (including edges to features or
  %% capstones outside this set). Regenerate on every REPLAN.
```

## 1. Outcome Statement

<!-- What becomes true of the project when this capstone lands,
     phrased as an observation: the demo script, command sequence, or
     acceptance walk-through a reviewer (or agent) executes to see it.
     "Do X, observe Y" at the system level. -->

## 2. Required Feature Set & Sufficiency

<!-- One row per required feature, then the sufficiency argument
     (rule E): why this set jointly delivers §1 (Outcome Statement),
     and per feature, what breaks in §1 if it were removed — the
     minimality check. A feature with no answer to the second question
     does not belong in the set. -->

| Feature | Contribution to the outcome | Status |
|---------|-----------------------------|--------|
| #       |                             |        |

## 3. Cross-Feature Integration Risks

<!-- Where the required features touch: shared interfaces (cite each
     feature's Feature-Level Interface & Data Contract by section
     name), ordering hazards, contract handoffs that cross feature
     boundaries, and the threats to validity that only appear at
     system scale — per-feature evidence that shortcuts the integrated
     code path, platform divergence, invariants that hold per-feature
     but not jointly. -->

## 4. System-Level Acceptance Criteria

<!-- Predictions spanning multiple features: do X, observe Y — each
     one not covered by any single feature's completion criteria
     (rule F). Name the end-to-end test, golden artifact, or recorded
     procedure that pins each, and which feature (or this issue's
     close-out) builds the ones that do not exist yet. -->

## 5. Re-planning Protocol

<!-- What invalidates this plan and the required response: a required
     feature descoped or refuted → re-derive the sufficiency argument
     in §2; a feature's contract deviates → reassess §3 and §4; the
     outcome itself is re-scoped → REPLAN with the old and new §1 both
     quoted. Every response ends in a REPLAN comment here. -->

## Open Questions & Decisions Needed

<!-- Decisions this plan cannot make for itself — for each: the
     question, options with a recommended default, and whether it
     blocks filing features, blocks acceptance, or can ride along.
     "N/A — fully specified" if nothing is open. -->

## Completion Criteria (Definition of Done)

- [ ] Every entry in `requires_features` closed as landed, or removed via a `REPLAN:` comment with the §2 sufficiency argument re-derived for the reduced set
- [ ] Every criterion in §4 (System-Level Acceptance Criteria) verified end-to-end at a named commit; command and output recorded in a closing comment
- [ ] The §1 (Outcome Statement) walk-through executed at that commit and its transcript recorded
- [ ] Every risk in §3 (Cross-Feature Integration Risks) checked at system scale; outcome recorded
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A of the feature template)
- [ ] ...
