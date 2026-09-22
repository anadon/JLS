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
     "N/A — fully specified" if nothing is open.

     Mark every entry with exactly one of `Recommended default: <answer>`
     (an executor may proceed), `BLOCKING: <who decides>` (an executor must
     stop), or `HYGIENE: <what to re-derive>` (not a decision at all — a
     citation, a status check, an un-run build). The marker is what an
     executor and § Agentic Delegability both read; an unmarked entry is
     treated as blocking and scores DA<=1. -->

## Completion Criteria (Definition of Done)

- [ ] Every entry in `requires_features` and `requires_capstones` closed as landed, or removed via a `REPLAN:` comment with the §2 sufficiency argument re-derived for the reduced set; `planned_features` empty (each resolved to a filed issue or descoped)
- [ ] Every cited evidence document and permalink resolves on the default branch at close
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (task rule 10)
- [ ] Every criterion in §4 (System-Level Acceptance Criteria) verified end-to-end at a named commit; command and output recorded in a closing comment
- [ ] The §1 (Outcome Statement) walk-through executed at that commit and its transcript recorded
- [ ] Every risk in §3 (Cross-Feature Integration Risks) checked at system scale; outcome recorded
- [ ] Machine block, roster table, and mermaid graph agree with reality at close (rule A, as read against this template)
- [ ] § Agentic Delegability filled at filing and re-scored on any `REPLAN:` or `AMENDED:` edit that changed the roster or §4's acceptance criteria
- [ ] ...


## Agentic Delegability (ADR-1)

<!--
  ADR-1 v1 (2026-09), capstone tier. Self-contained: the task and feature
  templates carry their own tier-adjusted copies of these anchors, because
  markdown issue templates have no include mechanism. When you change an
  axis here, change it in all three.

  How safely this capstone can be handed to an agentic LLM system — NOT how
  good, important or urgent it is. A LOW BAND IS A ROUTING DECISION, NOT A
  CRITICISM. Fill at filing; re-score on any `REPLAN:` or `AMENDED:` edit
  that changes the roster or §4.

  A CAPSTONE IS NEVER A SINGLE DELEGATION, AND THIS BLOCK DOES NOT PRETEND
  OTHERWISE. HL and CF are 0-1 by construction: a capstone composes
  features, so its own dependent-step chain is a multi-week programme and
  its own footprint is the whole system. Fill them honestly at 0-1.

  SCORE THIS CAPSTONE'S OWN DELIVERABLE — §4 (System-Level Acceptance
  Criteria) and §1 (Outcome Statement). A capstone does not inherit its
  features' scope or their open decisions; each feature carries its own
  block. THE TWO FIELDS WORTH FILING CAREFULLY ARE `ed` AND
  `roster_delegable`; the rest are near-determined by the tier.

  SEVEN ADDITIVE AXES, 0-5 each. RAW = their sum, 0-35.

  SC  SPECIFICATION CLOSURE of §4's acceptance criteria.
      5 every §4 criterion names the artifact, its path, and the assertion
        pinning it; the §1 walk-through is scripted step by step
      4 concrete; one or two open naming choices no reviewer would litigate
      3 the outcome is unambiguous, the acceptance evidence's shape is not
      2 §4 states an outcome ("the features jointly deliver X") rather than
        an artifact
      1 §1 names a direction; what acceptance proves is not written down
      0 open-ended, or §4 defers its own definition
      "Every required feature closed" is NOT an acceptance criterion — it
      is the roster. §4 must assert something no single feature asserts.

  OS  ORACLE STRENGTH of §4. Ask literally: what command, golden, or
      walk-through transcript gets pasted into the closing comment?
      5 byte or structural equality against a committed golden spanning
        features; an end-to-end round-trip property; a differential check
        against an independent implementation
      4 behavioural assertions over enumerated system-level cases INCLUDING
        the named refusal cases
      3 smoke-shaped: the end-to-end path runs, the command exits 0
      2 a threshold or a count with no behavioural content
      1 a reviewer agrees the outcome was achieved
      0 none; the features' green runs are treated as the proof
      DEDUCT 2 (floor 0) if the executor authors the acceptance oracle and
      the system it certifies with no pre-committed expected values.
      DEDUCT 1 if the acceptance evidence is a document asserting a
      measurement — a scored matrix, a comparison table, a published figure.
      The prose is trivially producible; the measurement is the work.

  BR  BLAST RADIUS of this capstone's OWN work — what the acceptance pass
      itself must touch, beyond what the features already landed.
      5 acceptance is one walk-through transcript; nothing moves
      4 2-4 files of end-to-end test and fixture in one place
      3 acceptance needs a harness spanning 2-3 packages
      2 acceptance forces reconciliation across features' contracts
      1 acceptance is system-wide, or pins a published surface — `.jls`
        format, the CLI, the element registry
      0 acceptance reaches out-of-repo consumers, or call sites unknowable
        without a full-tree search
      Expect 0-1. A capstone whose own BR is 4-5 is probably a feature.

  HL  HORIZON LENGTH of this capstone's OWN chain. 0-1 BY CONSTRUCTION — a
      capstone composes multi-day units, so the honest score is:
      1 the acceptance pass alone is >1 expert-week once the roster lands
      0 a multi-week programme composing other multi-week units
      Scores of 2-5 mean this is not really a capstone. Do not inflate to
      improve the band; the band is expected to be low here.

  PD  PRECEDENT DENSITY — is there a closed capstone in-tree whose
      acceptance walk-through this one can model on?
      5 §4 names a specific closed capstone's acceptance evidence and it
        exists
      4 a near-identical sibling capstone is in-tree, findable by name
      3 analogous acceptance patterns exist but need adaptation
      2 the category exists; this is the first acceptance of its kind
      1 no in-repo precedent; the shape comes from an external standard
        that §1 does cite
      0 none named; the executor invents what acceptance means here

  CF  CONTEXT FOOTPRINT of the acceptance work. 0-1 by construction.
      1 a whole-tree invariant — determinism, nullness, sealedness, the
        coverage ratchet — not verifiable locally
      0 the tree plus an external toolchain's semantics (Yosys, nextpnr,
        cocotb, Wokwi, GTKWave), or the tree plus a hardware target
      Score 2-5 only if this capstone's acceptance genuinely fits inside a
      subsystem, which is rare and worth a sentence in the prose line.

  RD  REVERSIBILITY / DEBT SURFACE of a wrong-but-plausible acceptance —
      i.e. declaring the outcome achieved when it is not.
      5 pure addition behind a test; revert is one commit
      4 internal; wrongness surfaces in CI or at the next touch
      3 latent but discoverable by a later reader; no external consumer
      2 the acceptance artifact becomes load-bearing: a committed
        system-level golden, a ratchet floor, a published threshold
      1 a published contract: `.jls` format text, a CLI flag, public API, a
        help page, an exported HDL shape
      0 irreversible outside the repo: a DOI, a Maven Central coordinate, a
        Marketplace listing, a tagged release, a third-party invitation, a
        public claim about a competitor
      A capstone is where RD=0 concentrates, because capstones are what
      announce things. A wrongly-accepted capstone is cited by everything
      downstream and is the most expensive thing on this backlog to unwind.

  TWO CAPPING AXES. They do not add; they impose a ceiling on the band.

  ED  ENVIRONMENTAL DETERMINISM, scored on §4. THE KEY FIELD OF THIS BLOCK.
      It answers: can this capstone's acceptance evidence be produced AT
      ALL without a person or a piece of hardware?
      5 headless and hermetic: `mvn verify` or an in-tree script  no cap
      4 a pinned toolchain the repo can fetch — nix devShell, container,
        `xvfb-run` — reproducible ............................... no cap
      3 a display substrate or network fetch that exists but is flaky, or
        an external corpus to download ......................... cap B
      2 a specific host OS, a GPU, or a service account the executor does
        not hold ............................................... cap C
      1 physical hardware — an FPGA board, a breadboard, a screen reader, a
        display panel — or a screen recording of a real session . cap F
      0 OTHER PEOPLE: an n-of-5 user trial, an independent reproducer, a
        second maintainer with merge rights, a peer reviewer, a JOSS
        editor, an instructor, an outside volunteer ............. cap F
      This is the single most useful thing this block records, because it
      is INVARIANT TO HOW GOOD THE MODELS GET, and because it is cheap to
      know at filing and expensive to discover at close.
      Measured 2026-09: 25 of the 36 open capstones were ED-capped at F.
      If that is true here, say so in §4 and in the prose line, and expect
      the close to need a human. ED<=1 is not a criticism — it means the
      deliverable is not a patch. An agent can still build the harness, the
      checklist and the analysis template; it cannot close the capstone,
      and marking it closed is exactly the fabrication this rubric exists
      to catch.

  DA  DESIGN AUTHORITY — decisions THIS CAPSTONE leaves open, in §4, §2
      (Required Feature Set & Sufficiency) or its Open Questions. A
      capstone does not inherit its features' open decisions.
      5 no open decisions; every choice stated or forced by landed code
        ......................................................... no cap
      4 only local reversible choices ........................... no cap
      3 decisions named AND each carries a recommended default .. cap B
      2 exactly one structural decision open, no preference stated  cap C
      1 two or more open with no preference, or a decision routed to
        another owner the executor must wait on ................. cap D
      0 this capstone's own deliverable IS a decision — a verdict, a
        policy, a scope boundary, a does-this-premise-hold gate . cap D
      DA rates DESIGN authority only. These do NOT lower it:
        - evidence hygiene ("re-derive citations", "line numbers drift")
        - bookkeeping ("confirm #N is still open", a roster re-sync, a
          `planned_features` entry not yet filed)
        - a question this capstone answers itself with a recommended default
        - a decision owned by a different issue this one waits on — that is
          `blocked_by`, and it scores in HL, not here
      Note the asymmetry with ED: DA=0 caps at D, not F, because an agent's
      research on a decision is genuinely most of the value — it just
      cannot make the call. ED=0 caps at F because the agent's output is
      none of it.
      Measured, 2026-09: grading all 688 open issues WITHOUT these four
      exclusions collapsed 87% of the corpus to DA=1, because an Open
      Questions section is mandatory here and graders read "section
      populated" as "decision open". Re-grading with them moved the axis
      +1.23, strictly one-sided.

  AND THE OTHER FIELD WORTH FILING CAREFULLY:
  `roster_delegable` — of the issues in `requires_features`, how many are
  themselves band A or B?
    k/n, k>=1     name which in the prose line
    0/n           expected: across the whole 2026-09 corpus this was 0 for
                  every capstone. What makes it actionable is naming, in
                  the prose line, the ONE feature whose roster is closest
                  to producing a delegable leaf
    roster empty  score `pending`; re-score when features are filed
  Across all 632 parent->child roster edges in 2026-09 the aggregate
  delegable fraction was 8/632. Decomposition that does not produce
  delegable leaves has not bought what decomposition is supposed to buy.

  BAND from RAW: 30-35 A | 24-29 B | 17-23 C | 10-16 D | 0-9 F.
  FINAL BAND = the most restrictive of (RAW band, ED cap, DA cap).

    A  DELEGATE — an agent opens the PR, CI is the gate, a human reads the
       diff once
    B  DELEGATE-WITH-CHECKPOINT — one named human approval, usually of the
       oracle or of a seam, before implementation
    C  SPECIFY-FIRST or SPLIT — something must be supplied first: a closed
       spec, a pre-committed golden, or a decomposition. The supplying act
       is often itself an A/B task, and filing it is the cheapest move
       available
    D  HUMAN-LED — the agent researches, drafts and builds harnesses; a
       human makes the call and owns the artifact
    F  HUMAN-ONLY (ED<=1) or AGENT-ASSIST-ONLY
  A capstone will land at D or F essentially always. That is the tier
  working as designed, not a finding, and not a reason to inflate an axis.

  DEBT TAG — the specific liability delegating as-is would create. Exactly
  one, most severe applicable, in this order:
    FABRICATED-EVIDENCE (ED<=1)          a checklist marked done, a
                                         measurement table with plausible
                                         numbers, a "verified" claim behind
                                         which nobody ran anything
    GOLDEN-LOCK-IN (RD=2 and OS>=4)      a golden generated from the agent's
                                         own output, now ground truth
    HOLLOW-ORACLE (OS<=2, or a deduction fired)
    SPEC-DRIFT (SC<=2)
    PREMATURE-SEAM (DA<=2)
    PARTIAL-INTEGRATION (BR<=1 or CF<=1)
    SCOPE-EXHAUSTION (HL<=1)
    DUPLICATION (PD<=1)
    NONE
  On the 36 open capstones in 2026-09, FABRICATED-EVIDENCE was the tag on
  23 of them — a direct consequence of ED, and the reason ED is the field
  worth filing carefully here.

  FILING CHECKS THIS SECTION ASSUMES (the scientific-task template states
  these as rules 11-13; they bind at every tier):
    - ORACLE CUSTODY. Every §4 criterion comparing against an expected
      value names WHO produced that value and WHEN. "Pre-committed" and
      "same-change" are different guarantees; only the first is evidence.
      Same-change authorship was the largest predicted debt source across
      the 2026-09 corpus, at 58%.
    - ARTIFACT PATHS RESOLVE AT FILING. Every path a §4 criterion names
      either exists at the pinned evidence commit or is created by this
      capstone's features. 42 open issues named a deliverable whose target
      no longer existed.
    - OPEN QUESTIONS ARE MARKED: `Recommended default:`, `BLOCKING:`, or
      `HYGIENE:`. An unmarked entry is read as an unresolved structural
      decision and scores DA<=1, which is usually not what the filer meant.

  Filing a low band is a signal, not a confession.
-->

```yaml
adr: 1
sc:                  # 0-5  specification closure of §4's acceptance criteria
os:                  # 0-5  oracle strength of §4 — not "the features landed"
br:                  # 0-5  this capstone's OWN blast radius (expect 0-1)
hl:                  # 0-5  0-1 by construction — a capstone composes
pd:                  # 0-5  precedent density
cf:                  # 0-5  0-1 by construction — system-wide
rd:                  # 0-5  reversibility / debt surface
raw:                 # sc+os+br+hl+pd+cf+rd, 0-35
ed:                  # 0-5  environmental determinism (CAPPING) — scored on §4.
                     #   THE KEY FIELD: 0 = §4 needs other people,
                     #   1 = needs physical hardware or a recording
da:                  # 0-5  design authority (CAPPING) — this capstone's own
band:                # A|B|C|D|F — most restrictive of RAW band, ED cap, DA cap
action:              # HUMAN-LED | HUMAN-ONLY | AGENT-ASSIST-ONLY | SPLIT
debt:                # predicted debt mode, or NONE
roster_delegable:    # k/n — requires_features entries at band A or B, or `pending`
```

<!-- One or two sentences: if ED<=1, exactly which §4 criterion needs a
     person or hardware and what an agent can still produce for it (the
     harness, the checklist, the analysis template); and which single
     feature's roster is closest to yielding a delegable leaf. Name the
     criterion and the artifact; not generic advice. -->
