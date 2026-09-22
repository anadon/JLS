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

     Mark every entry with exactly one of `Recommended default: <answer>`
     (an executor may proceed), `BLOCKING: <who decides>` (an executor must
     stop), or `HYGIENE: <what to re-derive>` (not a decision at all — a
     citation, a status check, an un-run build). The marker is what an
     executor and § Agentic Delegability both read; an unmarked entry is
     treated as blocking and scores DA<=1. -->

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
  ADR-1 v1, composition tier. How safely this feature can be handed to an
  automated or semi-automated executor, and what maintenance liability
  delegating it as-is would create. This is NOT a rating of how good,
  important or urgent the work is. A LOW BAND IS A ROUTING DECISION, NOT A
  CRITICISM.

  SCORE THIS FEATURE'S OWN DELIVERABLE — the integration evidence and the
  declared contract — never the union of its children. Each child carries
  its own block; a parent that inherited its roster's axes would merely
  restate them.

  Fill at filing time; re-score whenever a re-plan or amendment changes the
  roster, the integration evidence, or the open decisions.

  Two failure modes are rated, not one: the work is not finished, and the
  work is finished in a way that leaves a liability behind. The second is
  the dangerous one, because it is invisible to the gate that accepted it.

  THIS SECTION IS SELF-CONTAINED. The other tier templates carry their own
  tier-adjusted copies of these anchors, because issue templates have no
  include mechanism. The copies CAN drift, and nothing detects it
  automatically: when an axis changes here, change it in every copy and
  bump the version marker above in all of them together. A copy whose
  marker disagrees with its siblings is stale by definition.

  WHY THESE AXES. Each is anchored to a repeatedly measured effect rather
  than to intuition. Underspecification is the dominant reason real issues
  turn out not to be solvable as written. Verification, not generation, is
  the binding constraint on automated work, and proxy checks get satisfied
  the wrong way when they are the reward. Changes spanning many files
  succeed at markedly lower rates than changes confined to one. Per-step
  error compounds over a dependent chain, so reliability falls with horizon
  length even when every individual step is strong. Output quality degrades
  as the amount of context that must be held grows. And automated authoring
  shifts work measurably from moving code to copying it, which is a
  duplication pressure that precedent suppresses. The two capping axes
  exist because two constraints do not trade against the others at all:
  evidence that requires the physical or social world, and decisions that
  have not been made.

  SEVEN ADDITIVE AXES, 0-5 each. RAW = their sum, 0-35.

  SC  SPECIFICATION CLOSURE — is the end state fixed by the text alone?
      The test: could two competent implementers each satisfy § Integration Criteria & Evidence Plan
      and produce artifacts differing in a way a reviewer would care about?
      If so, it is not closed.
      5 every integration criterion names the artifact, its location, and the assertion
        that pins it; no "appropriate", "reasonable", "as needed"
      4 concrete; one or two open naming or layout choices no reviewer would
        litigate
      3 the goal is unambiguous, the artifact's shape is not; at least one
        structural decision must be inferred
      2 the end state is stated as an outcome rather than as an artifact
      1 a problem and a direction; what "done" looks like is not written down
      0 open-ended, self-contradictory, or deferring its own definition
      A criterion reading "documented", "considered", "reviewed" or
      "improved" with no named artifact caps this at 3. A section honestly
      marked not-applicable with a reason does not lower it; unfilled
      template boilerplate does.
      "Every child closed" is not an integration criterion — that is the
      roster. This section must assert something no single child asserts.

  OS  ORACLE STRENGTH — the check that would actually gate the merge. Not
      "could this be tested" but "is there a signal that is cheap to run,
      faithful to intent, and hard to satisfy the wrong way?"
      5 exact comparison against a pre-existing expected artifact spanning
        children; an algebraic property across the composed boundary; or a
        differential check against an independent implementation
      4 behavioural assertions over enumerated cases INCLUDING the named
        negative and failure cases; or a compiler, type or analysis gate
        that fails loudly
      3 existence- or smoke-shaped: the artifact is produced, the command
        exits zero, the output is non-empty — satisfiable without being
        correct
      2 a threshold or a count, with no behavioural content
      1 a person reads prose and agrees
      0 none; the children's green runs are treated as the proof
      DEDUCT 2 (floor 0) if the same change authors both the implementation
      and the expected values that certify it. That is the configuration in
      which a failing check gets "fixed" by weakening it.
      DEDUCT 1 if the acceptance evidence is a document asserting a
      measurement. The document is trivially producible; the measurement
      behind it is the real work and is not itself gated.

  BR  BLAST RADIUS — everything this feature's own integration work must move, including generated
      files, expected-output artifacts, and anything published. Higher is
      smaller.
      5 the integration evidence is one new test over already-landed
        children; nothing in the declared contract moves
      4 a handful of integration files within one module
      3 the declared contract changes one internal interface, all children
        in view
      2 the contract changes across modules, or children the roster does not
        enumerate must change with it
      1 the contract is a published surface every child must conform to
      0 the contract has consumers outside this repository, or integration
        touches call sites unknowable without searching the whole tree

  HL  HORIZON LENGTH — the DEPENDENT-step chain, in expert time, from a
      cold start to gated evidence. Steps that can run in parallel count
      for less than steps that must run in order: reliability falls with
      the length of the dependent chain, not with total volume.
      Measured AFTER the roster lands. This is NOT the sum of the children's
      horizons; it is the wait-then-reconcile-then-verify chain.
      5 under an expert-hour: run one integration check
      4 one to four hours
      3 half a day to two days: integration surfaces contract mismatches to
        reconcile
      2 several days, or two or more children whose contracts must be
        designed together
      1 more than a week, or integration crosses a subsystem it must first
        learn
      0 a multi-week programme in its own right
      A feature that mostly composes scores 0-1 here in the ordinary case —
      not as an absolute, since a thin feature really can be a one-hour
      integration check. Where it is low, that is the tier working as
      designed. Do not inflate it and do not read it as
      a defect: this block's own band answers only "can this be handed over
      as ONE unit", and for something that composes, the answer is
      structurally no.

  PD  PRECEDENT DENSITY — is there a landed sibling whose integration evidence this can be modelled on already in this repository?
      5 this issue names a specific in-repository precedent and it exists
      4 a near-identical sibling exists in-tree and is findable by name
      3 analogous patterns exist but need adaptation
      2 the category exists in-tree; this is the first instance of its kind
      1 no in-repository precedent; the pattern comes from an external
        specification this issue does cite
      0 no precedent named anywhere; whoever executes invents the shape
      This axis predicts duplication. With a precedent, the work copies a
      local pattern; without one, it reproduces whatever pattern is most
      common in the wider world — which is how a codebase with a deliberate
      house style acquires code that compiles, passes, and reads as though
      it came from somewhere else.

  CF  CONTEXT FOOTPRINT — how much must be held in mind simultaneously, not
      merely read.
      5 one child's module plus the integration check
      4 one module; the declared contract is sufficient context
      3 two or three modules, or a module plus a format specification
      2 a subsystem boundary held from both sides at once
      1 a repository-wide invariant declared by this feature and not
        verifiable locally
      0 the repository plus the semantics of an external toolchain
      A feature that declares repository-wide invariants commonly scores 1,
      correctly.

  RD  REVERSIBILITY / DEBT SURFACE — the cost of a completion that is wrong
      but plausible. Higher is cheaper to reverse.
      5 a pure addition behind a check; reverting is one commit and nothing
        depends on it
      4 internal; wrongness surfaces in CI or at the next person to touch it
      3 latent but discoverable by a later reader; no consumer outside this
        repository
      2 the artifact becomes load-bearing: a committed expected-output file,
        a ratchet floor, a published threshold, an opt-in invariant others
        are then measured against
      1 a published contract: a file format, a command-line flag, a public
        API, user-facing documentation
      0 irreversible outside this repository: an archival identifier, a
        package-registry coordinate, a public listing, a tagged release, a
        commitment made to a third party
      A score of 2 deserves particular care. An expected-output artifact
      produced by the same process that produced the behaviour is not
      evidence of that behaviour; it is a record of it. It will pass
      indefinitely, and it will be cited as ground truth by everything built
      on top of it.

  TWO CAPPING AXES. They do not add; they impose a ceiling on the band.

  ED  ENVIRONMENTAL DETERMINISM — can the integration evidence be produced by whoever
      executes this, unattended, inside the project's own automated
      environment?
      5 fully self-contained: the project's standard build-and-test command,
        or a script already in the tree, produces everything ...... no cap
      4 needs a pinned toolchain the project can fetch and reproduce
        automatically ............................................. no cap
      3 needs a substrate or network resource that exists but is
        unreliable, or an external corpus that must be downloaded .. cap B
      2 needs a particular host platform or device class, or credentials
        the executor does not hold ................................ cap C
      1 needs hardware someone must physically possess or operate, or a
        recording of a real session ............................... cap F
      0 needs OTHER PEOPLE: a trial with human subjects, an independent
        reproducer, a second maintainer holding approval rights, an
        external reviewer or publisher ............................ cap F
      A low score is NOT a criticism. It means the deliverable is not a
      patch. Much of the work may still be produced unattended — the
      harness, the script, the checklist, the analysis template — but the
      issue cannot be CLOSED that way, and closing it anyway is precisely
      the failure this rubric exists to prevent. Recording this at filing
      time is the point: it tells whoever picks the issue up that they are
      not looking at a coding task.
      A low score here bands the whole feature F however delegable its
      children are.

  DA  DESIGN AUTHORITY — does finishing require a decision the issue does
      not already make? Scored on this feature itself. A feature does not inherit its children's open
      decisions; each child scores its own.
      A design decision is a choice that changes the shape of the artifact
      and that a reviewer could reasonably contest: where a boundary goes,
      what a type carries, which of two mechanisms is used, what a published
      surface looks like, what is in or out of scope.
      5 no open decisions; every choice is stated here or forced by existing
        code ...................................................... no cap
      4 only local, reversible choices remain — a name, where a helper
        lives ..................................................... no cap
      3 decisions are named AND each carries a stated preference, so
        execution proceeds unblocked .............................. cap B
      2 exactly one structural decision is genuinely open, no preference
        stated .................................................... cap C
      1 two or more are open with no preference, or a decision is routed to
        another owner who must answer first ....................... cap D
      0 the deliverable IS a decision: a verdict, a policy, a scope
        boundary, a gate on whether a premise holds ............... cap D
      This axis rates DESIGN authority only. These do NOT lower it:
        - evidence hygiene: re-deriving citations, refreshing line numbers,
          re-running something that was not run at filing time
        - bookkeeping: confirming another issue's status, re-syncing a
          roster, an entry not yet filed
        - a question this issue poses and then answers with a stated
          preference
        - a decision already owned by another issue this one waits on —
          that is an ordering dependency, and it scores under Horizon Length
      A template that requires an open-questions section will have one on
      every issue. A populated section is therefore NOT evidence of an open
      decision: score the entries, not the section's existence. Failing to
      make that distinction collapses this axis to a constant and destroys
      its value.
      Note the asymmetry with Environmental Determinism: a 0 here caps at D,
      not F, because research and drafting on an undecided question is
      genuinely most of the value — what cannot be delegated is the
      decision itself. A 0 on ED caps at F because none of the value can be
      produced.

  AND THE NUMBER THAT USUALLY MATTERS MORE THAN THE BAND:
  `roster_delegable` — of the entries in `requires_tasks`, how many are
  individually band A or B? That, not this feature's own band, predicts
  whether the feature can be executed by delegation at all.
    k/n, k>=1     at least one child can be handed over today; name which
                  in the prose line
    0/n           nothing in this roster is delegable as filed. The fix is
                  at the child tier — answer a child's open question, or
                  pre-commit a child's expected values — not here.
                  Rewriting the parent will not move it
    roster empty  score `pending`; re-score once children are filed
  A decomposition that does not yield delegable leaves has not bought what
  decomposition is supposed to buy. This number is how you notice.

  BAND from RAW: 30-35 A | 24-29 B | 17-23 C | 10-16 D | 0-9 F.
  FINAL BAND = the most restrictive of (RAW band, ED cap, DA cap).

    A  DELEGATE — automated end to end; the gate is CI and one human
       reading the diff
    B  DELEGATE-WITH-CHECKPOINT — one named human approval, usually of the
       oracle or of a boundary, before implementation
    C  SPECIFY-FIRST or SPLIT — something must be supplied first: a closed
       specification, pre-committed expected values, or a decomposition.
       Supplying it is often itself an A- or B-band unit of work, and
       filing that is the cheapest move available
    D  HUMAN-LED — research, drafts and harnesses can be produced
       unattended; a person makes the call and owns the artifact
    F  HUMAN-ONLY (ED<=1) or AGENT-ASSIST-ONLY

  A feature that mostly composes will land at C, D or F, and that is the
  tier working as designed — not a finding, and never a reason to inflate
  an axis to escape it. A and B remain reachable, but only for a thin
  feature whose own integration work is genuinely small; if you score one
  that high, the prose line should say why this feature is not simply a
  task.

  DEBT TAG — the specific liability that delegating as-is would create.
  Exactly one, the most severe that applies, in this order:
    FABRICATED-EVIDENCE (ED<=1)     a checklist marked done, a measurement
                                    table with plausible numbers, a
                                    "verified" claim behind which nothing
                                    was run. Invisible to every automated
                                    gate, which is why it ranks first
    GOLDEN-LOCK-IN (RD=2, OS>=4)    a load-bearing expected-output artifact
                                    that later work is then graded against.
                                    The oracle is strong and may be
                                    perfectly correct today; the liability
                                    is that it entrenches, and revising it
                                    later invalidates everything measured
                                    against it. NOTE the boundary with the
                                    tag below: an artifact authored by the
                                    same change it certifies fires the
                                    deduction, so its oracle cannot reach 4
                                    and it is HOLLOW-ORACLE, not this
    HOLLOW-ORACLE (OS<=2, or a deduction fired)
    SPEC-DRIFT (SC<=2)
    PREMATURE-SEAM (DA<=2)
    PARTIAL-INTEGRATION (BR<=1 or CF<=1)
    SCOPE-EXHAUSTION (HL<=1)
    DUPLICATION (PD<=1)
    NONE
  A tag whose trigger is met ONLY because an axis is structurally bounded at
  this tier reports a property of the tier, not of this issue, and carries
  no information. Skip it and take the next tag whose trigger reflects
  something about this issue specifically; if none does, record NONE. In
  particular, where Horizon Length or Context Footprint are low by
  construction rather than by circumstance, SCOPE-EXHAUSTION and
  PARTIAL-INTEGRATION are not the finding.

  THREE FILING CHECKS THIS SECTION ASSUMES:
    - ORACLE CUSTODY. Every criterion that compares against an expected
      value names who produced that value and when. Values committed and
      reviewed BEFORE the implementation, and values written by the same
      change as the implementation, are different guarantees; only the
      first is evidence.
    - ARTIFACT PATHS RESOLVE AT FILING. Every path a criterion names either
      exists already or is created by this work, and the criteria say
      which. A criterion pointing at something that does not exist is
      unclosable, and whoever picks it up will either invent a location or
      skip it silently.
    - OPEN QUESTIONS CARRY A MARKER, rather than merely being listed:
      a stated preference an executor may proceed on, a blocking question
      naming who must answer, or an item that is hygiene rather than a
      decision at all. An unmarked entry reads as an unresolved structural
      decision and scores DA<=1, which is usually not what was meant.

  A low band is a signal, not a confession. The cheapest way to raise one
  is almost always to answer an open question or to pre-commit the expected
  values — rarely to rewrite the issue.
-->

```yaml
adr: 1
sc:                  # 0-5  specification closure of the integration criteria
os:                  # 0-5  oracle strength of the integration evidence
br:                  # 0-5  this feature's OWN blast radius (composition => low)
hl:                  # 0-5  this feature's OWN chain       (composition => low)
pd:                  # 0-5  precedent density
cf:                  # 0-5  this feature's OWN footprint   (composition => low)
rd:                  # 0-5  reversibility / debt surface
raw:                 # sc+os+br+hl+pd+cf+rd, 0-35
ed:                  # 0-5  environmental determinism (CAPPING)
da:                  # 0-5  design authority          (CAPPING) — this feature's own
band:                # A|B|C|D|F — most restrictive of RAW band, ED cap, DA cap
action:              # DELEGATE | DELEGATE-WITH-CHECKPOINT | SPECIFY-FIRST |
                     #   SPLIT | HUMAN-LED | HUMAN-ONLY | AGENT-ASSIST-ONLY
debt:                # predicted debt mode, or NONE
roster_delegable:    # k/n — requires_tasks entries at band A or B, or `pending`
```

<!-- One or two sentences: which children (if any) can be delegated today,
     which single child's open question or missing pre-committed expected
     value is costing the roster the most, and — if ED<=1 — which part of
     the integration evidence needs a person or hardware. Name the section,
     the criterion, the artifact — not generic advice. -->
