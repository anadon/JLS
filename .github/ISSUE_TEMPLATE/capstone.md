---
name: Capstone
about: A milestone-level outcome gated on a required set of features — owns the system-level acceptance evidence that the features jointly deliver it
labels: ["tier:capstone"]
---

<!--
  Template: capstone v4 (2026-08)

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
    - Capstones never reference TASKS. Since feature v4 a task may be
      shared by any number of features, so scope that once needed a
      direct capstone→task edge is now simply listed in an additional
      feature's roster. See rule G, which is retained only for scope
      that no feature can host at all.
    - Nested capstones: list a sub-capstone in requires_capstones (its
      whole outcome gates this one; the DAG rule covers the composition
      edge), OR enumerate the sub-capstone's features directly in
      requires_features (the consume-its-features form). If you choose
      the second, record a mirror obligation in BOTH issues: any REPLAN
      to either roster must re-sync the other, or the two silently
      drift.
    - Ordering between capstones is now expressed DIRECTLY:
      capstone-to-capstone blocked_by / blocks is legal. (v3 forbade
      it and routed inter-capstone ordering through the later
      capstone's features being blocked_by the earlier capstone. That
      indirection is retired: it required an upward feature→capstone
      edge, which the corrected model makes illegal.) Use
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
  template's headings; executor re-verification; explicit labels, here
  `tier:capstone`), together with task rules 9–10 (comment protocol
  and waivers, with `REPLAN:` in place of `AMENDED:`) and feature
  rules A–D read against this template: the machine block below, in
  Status & Required Features, is the source of truth for the edges it
  can express (A); the not-a-folder test is rule F below (B); living
  body, plan changes REPLAN-logged, bookkeeping exempt (C); state
  reconstructed from prefixed comments mirrored between this capstone
  and its features, never from checkboxes (D). In addition:

  E. The required set is a closed list with a sufficiency argument
     (§2, Required Feature Set & Sufficiency): why exactly these
     features, together, make §1 (Outcome Statement) true — and why
     none is removable. Adding or removing a feature is a re-plan
     recorded with a REPLAN comment, not a quiet edit.
  F. A capstone must assert system-level acceptance criteria (§4)
     that no single feature's completion criteria cover. If every
     criterion is already owned by a feature, this is a milestone
     label, not a capstone — do not file it.
  G. Orphaned scope. When a required feature closes, is re-tiered, or
     is descoped while leaving scope this capstone still needs, the
     REPLAN must give that scope a disposition: (a) re-home it — add
     the task to another feature's requires_tasks roster; (b) file a
     new feature to host it and add that feature to requires_features;
     or (c) descope it, re-deriving the §2 (Required Feature Set &
     Sufficiency) argument.
     There is NO capstone→task edge. The v3 `requires_tasks_exception`
     field is retired: it existed only because the single-owner rule
     could leave a task with nowhere to go, and feature v4's shared
     ownership removes that condition entirely — option (a) is now
     always available, since a task may sit in any number of rosters.
     A capstone that appears to need a task directly is missing a
     feature; file one.
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
requires_features: []   # composition — the closed required set (rule E), FILED numbers only
requires_capstones: []  # composition — sub-capstones whose whole outcome gates this one
                        #   (nesting; see the tier-model note on the consume-its-features
                        #   alternative and its mirror obligation)
planned_features: []    # one-line scopes for required features not yet filed; verify each
                        #   scope is ABSENT at evidence_commit before listing it; resolve
                        #   each to a number via REPLAN when it is filed. A non-empty
                        #   planned_features means the §2 sufficiency argument is
                        #   PROVISIONAL and this capstone is not Ready.
blocked_by: []          # ordering: capstones or features that must land before this
                        #   capstone closes, beyond the required set. Never tasks.
blocks: []              # ordering: capstones waiting on this one (mirror of their
                        #   blocked_by entry naming this capstone)
related: []             # reference only — never blocking, never ownership
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
     close-out) builds the ones that do not exist yet.

     A "Spans #A, #B" ANNOTATION IS NOT EVIDENCE. A 2026-08 audit of
     all 36 capstones found this to be the most repeated defect at
     this tier: a criterion annotated as spanning two features while
     one of those features' own §5 or Definition of Done already
     carried the whole assertion, sometimes word for word. Four
     capstones failed rule F outright because EVERY criterion turned
     out to be single-feature-covered — they are milestone labels, not
     capstones.

     So: before writing "spans #A, #B", open #A and #B and read their
     Integration Criteria and DoD. State what each contributes. If one
     of them already asserts the whole thing, say "covered alone by
     #A" — that is honest, and it simply does not count toward rule F.
     A capstone needs only ONE genuine system-level criterion, but it
     does need one.

     Watch for these, all found in this corpus:
       - the second party named in a "span" is not in requires_features
         at all — then it is not a composition claim and the criterion
         is effectively unowned;
       - a criterion whose named owner DISCLAIMS it (one feature's
         scope boundary explicitly refuses the work the capstone
         assigns it);
       - a criterion no feature covers and no task owns — acceptance
         evidence with no work item anywhere;
       - text left stale by a feature's later REPLAN. -->

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

- [ ] Every entry in `requires_features` and `requires_capstones` closed as landed, or removed via a `REPLAN:` comment with the §2 sufficiency argument re-derived for the reduced set; `planned_features` empty (each resolved to a filed issue or descoped)
- [ ] Every cited evidence document and permalink resolves on the default branch at close
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10)
- [ ] Every criterion in §4 (System-Level Acceptance Criteria) verified end-to-end at a named commit; command and output recorded in a closing comment
- [ ] The §1 (Outcome Statement) walk-through executed at that commit and its transcript recorded
- [ ] Every risk in §3 (Cross-Feature Integration Risks) checked at system scale; outcome recorded
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A, as read against this template)
- [ ] ...
