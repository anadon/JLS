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
  ADR-1 v1, outcome tier. How safely this capstone can be handed to an
  automated or semi-automated executor, and what maintenance liability
  delegating it as-is would create. This is NOT a rating of how good,
  important or urgent the work is. A LOW BAND IS A ROUTING DECISION, NOT A
  CRITICISM.

  SCORE THE ACCEPTANCE PASS, NOT THE PROGRAMME. Every axis here is scored
  on what remains once the roster has landed: the work of demonstrating the
  outcome, not the work of building it. Waiting on an unlanded roster is an
  ordering dependency — it belongs in the roster and in `blocked_by`, and it
  is NOT a horizon or footprint score. A capstone whose features have all
  landed and whose acceptance is one scripted walk-through against a
  committed transcript genuinely has a short chain, and must be allowed to
  score like one.
  In practice this tier still tends to land low, because acceptance criteria
  are the least likely to be closed and the most likely to need a person.
  That must FALL OUT of the scores; it is never imposed on them.

  SCORE THIS CAPSTONE'S OWN DELIVERABLE — the system-level acceptance
  criteria and the outcome statement — never the union of its features.

  Fill at filing time; re-score whenever a re-plan or amendment changes the
  roster or the acceptance criteria.

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
      The test: could two competent implementers each satisfy § System-Level Acceptance Criteria
      and produce artifacts differing in a way a reviewer would care about?
      If so, it is not closed.
      5 every acceptance criterion names the artifact, its location, and the assertion
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
      "Every required feature closed" is not an acceptance criterion — that
      is the roster. This section must assert something no single feature
      asserts.

  OS  ORACLE STRENGTH — the check that would actually gate the merge. Not
      "could this be tested" but "is there a signal that is cheap to run,
      faithful to intent, and hard to satisfy the wrong way?"
      5 exact comparison against a pre-existing expected artifact spanning
        features; an end-to-end algebraic property; or a differential check
        against an independent implementation
      4 behavioural assertions over enumerated cases INCLUDING the named
        negative and failure cases; or a compiler, type or analysis gate
        that fails loudly
      3 existence- or smoke-shaped: the artifact is produced, the command
        exits zero, the output is non-empty — satisfiable without being
        correct
      2 a threshold or a count, with no behavioural content
      1 a person reads prose and agrees
      0 none; the features' green runs are treated as the proof
      DEDUCT 2 (floor 0) if the same change authors both the implementation
      and the expected values that certify it. That is the configuration in
      which a failing check gets "fixed" by weakening it.
      DEDUCT 1 if the acceptance evidence is a document asserting a
      measurement. The document is trivially producible; the measurement
      behind it is the real work and is not itself gated.
      Ask it literally: what command, artifact or transcript gets pasted
      into the closing comment? If the answer is "a reviewer agrees the
      outcome was achieved", this axis is 1.

  BR  BLAST RADIUS — everything this capstone's own acceptance work must move, including generated
      files, expected-output artifacts, and anything published. Higher is
      smaller.
      5 acceptance is one transcript; nothing moves
      4 a handful of end-to-end test and fixture files
      3 acceptance needs a harness spanning two or three modules
      2 acceptance forces reconciliation across the features' contracts
      1 acceptance is system-wide, or pins a published surface
      0 acceptance reaches consumers outside this repository, or call sites
        unknowable without searching the whole tree
      Acceptance that only observes scores high; acceptance that must add
      or change production code to become demonstrable scores low, and that
      is a signal the features did not finish the job.

  HL  HORIZON LENGTH — the DEPENDENT-step chain, in expert time, from a
      cold start to gated evidence. Steps that can run in parallel count
      for less than steps that must run in order: reliability falls with
      the length of the dependent chain, not with total volume.
      Measured AFTER the roster lands. This is NOT the sum of the features'
      horizons, and it is NOT the length of the programme; it is the
      demonstrate-reconcile-record chain of the acceptance pass itself.
      5 under an expert-hour: run one scripted walk-through
      4 one to four hours
      3 half a day to two days: acceptance surfaces cross-feature
        mismatches that must be reconciled before the outcome can be shown
      2 several days, or two or more features whose behaviour must be
        demonstrated together in a way neither demonstrates alone
      1 more than a week, or acceptance crosses a subsystem it must first
        learn
      0 acceptance is itself a multi-week programme
      A high score here is legitimate and reachable: a capstone whose roster
      has landed and whose acceptance is already scripted is a short piece
      of work. If you score 4-5 while the roster is incomplete, you have
      scored the wrong thing — the waiting is an ordering dependency, not a
      horizon.

  PD  PRECEDENT DENSITY — is there a closed sibling whose acceptance evidence this can be modelled on already in this repository?
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
      Scored on the acceptance pass, not on the system the features built.
      5 one entry point and its transcript
      4 one module; the outcome statement is sufficient context
      3 two or three modules, or a module plus a format specification
      2 a subsystem boundary that must be held from both sides at once
      1 a repository-wide invariant that cannot be verified locally
      0 the repository plus the semantics of an external toolchain, or the
        repository plus a hardware target
      Acceptance that merely drives an already-built system from its
      outside edge can legitimately score high; acceptance that must reason
      about the system's internals to know whether the outcome holds cannot.

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
      This tier is where a score of 0 concentrates, because this tier is
      what announces things. An outcome wrongly accepted is cited by
      everything downstream and is the most expensive kind of mistake on a
      backlog to unwind.

  TWO CAPPING AXES. They do not add; they impose a ceiling on the band.

  ED  ENVIRONMENTAL DETERMINISM — can the acceptance evidence be produced by whoever
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
      THIS IS THE KEY FIELD OF THIS BLOCK. It is the one quantity here that
      is invariant to how capable the tooling becomes, and it is cheap to
      know at filing and expensive to discover at close.

  DA  DESIGN AUTHORITY — does finishing require a decision the issue does
      not already make? Scored on this capstone itself. A capstone does not inherit its features' open
      decisions; each feature scores its own.
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

  AND THE OTHER FIELD WORTH FILING CAREFULLY:
  `roster_delegable` — of the entries in `requires_features`, how many are
  themselves band A or B?
    k/n, k>=1     name which in the prose line
    0/n           common at this tier and not by itself an alarm. What
                  makes it actionable is naming, in the prose line, the ONE
                  feature whose roster is closest to yielding a delegable
                  leaf
    roster empty  score `pending`; re-score once features are filed
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

  EVERY BAND IS REACHABLE AT THIS TIER, INCLUDING A. A capstone whose
  features have landed, whose acceptance criteria name their artifacts, and
  whose outcome is demonstrated by a committed transcript is genuinely
  delegable, and the rubric must be able to say so. In practice this tier
  lands low far more often than the others — but that has to be earned by
  the scores, not imposed by the tier. If you find yourself reaching for a
  low score because "capstones are not delegable", stop: score the
  acceptance pass honestly and let the capping axes do their work.
  Equally, do not inflate an axis to escape a low band. The band is a
  routing decision; a low one costs nothing but a different route.

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
sc:                  # 0-5  specification closure of the acceptance criteria
os:                  # 0-5  oracle strength of the acceptance evidence
br:                  # 0-5  the acceptance pass's own blast radius
hl:                  # 0-5  the acceptance pass, measured after the roster lands
pd:                  # 0-5  precedent density
cf:                  # 0-5  the acceptance pass's own footprint
rd:                  # 0-5  reversibility / debt surface
raw:                 # sc+os+br+hl+pd+cf+rd, 0-35
ed:                  # 0-5  environmental determinism (CAPPING). THE KEY FIELD:
                     #   0 = acceptance needs other people,
                     #   1 = needs hardware someone must operate
da:                  # 0-5  design authority (CAPPING) — this capstone's own
band:                # A|B|C|D|F — most restrictive of RAW band, ED cap, DA cap
action:              # DELEGATE | DELEGATE-WITH-CHECKPOINT | SPECIFY-FIRST |
                     #   SPLIT | HUMAN-LED | HUMAN-ONLY | AGENT-ASSIST-ONLY
debt:                # predicted debt mode, or NONE
roster_delegable:    # k/n — requires_features entries at band A or B, or `pending`
```

<!-- One or two sentences: if ED<=1, exactly which acceptance criterion
     needs a person or hardware and what can still be produced unattended
     for it (the harness, the checklist, the analysis template); and which
     single feature's roster is closest to yielding a delegable leaf. Name
     the criterion and the artifact — not generic advice. -->
