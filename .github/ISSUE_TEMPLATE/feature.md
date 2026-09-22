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
     (proceed and land), `PROPOSED: <answer>, pending <who confirms>` (draft
     but do not land), `BLOCKING: <who decides>` (stop), or
     `HYGIENE: <what to re-derive>` (not a decision at all — a
     citation, a status check, an un-run build). The marker is what an
     executor and § Agentic Delegability both read; an unmarked entry is
     scored on its substance, and the marker fixed. -->

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
  ADR-1 v2, composition tier. How safely this feature can be handed to an
  automated executor, and what maintenance liability delegating it as-is
  would create. NOT a rating of how good, important or urgent the work is.
  SCORE THIS FEATURE'S OWN DELIVERABLE — its integration evidence and its
  declared contract — never the union of its children; each child carries
  its own block.
  Fill at filing; re-score when a re-plan or amendment changes the roster,
  the integration evidence or the open decisions. Each axis is scored in
  exactly one place: where two rules could apply to one fact, the anchor
  ladder wins.
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
      5 exact comparison against a pre-existing expected artifact spanning
        children; an algebraic property across the composed boundary; or a
        differential check against an independent implementation
      4 behavioural assertions over enumerated cases including the named
        failure cases; or a compiler, type or analysis gate THIS CHANGE
        WOULD FAIL BEFORE THE FIX (a standing project-wide gate every change
        already passes is not this issue's oracle)
      3 existence- or smoke-shaped: produced, exits zero, non-empty
      2 a threshold or a count, no behavioural content
      1 a person reads prose and agrees
      0 none; success asserted by whoever did the work
      DEDUCTIONS, CUMULATIVE, floor 0. Deduct 2 if the same change authors
      both the implementation and the expected values certifying it. Deduct
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
      2 needs a particular host platform, device class, or credentials the
        executor does not hold .................................... cap C
      1 needs hardware someone must physically possess or operate, or a
        recording of a real session ............................... cap F
      0 needs ANOTHER PERSON TO PRODUCE EVIDENCE: a human-subject trial, an
        independent reproducer, someone who must personally run or witness a
        step and report what they saw, an external publisher whose
        acceptance is itself the evidence ......................... cap F
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
      1 two or more open with no preference; or any open decision another
        owner must answer before this can finish (a `BLOCKING:` entry)
        .......................................................... cap D
      0 the deliverable IS a decision: a verdict, a policy, a scope
        boundary, a gate on whether a premise holds .............. cap D
      Score the entries, not the presence of an open-questions section:
      a template that mandates one will have one on every issue. Score an
      unmarked entry on its substance and fix the marker.

  `roster_delegable` — of the entries in `requires_tasks`, how many are
  individually band A or B. This predicts whether the feature can be
  executed by delegation better than its own band does. Score `pending` if
  the roster is empty or its children are not yet scored; name in the prose
  line which child is closest to delegable.

  BAND from RAW: 30-35 A | 24-29 B | 17-23 C | 10-16 D | 0-9 F.
  FINAL BAND = the most restrictive of (RAW band, ED cap, DA cap).
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
    GOLDEN-LOCK-IN           RD=2 and OS>=4 a correct expected artifact that
                                            entrenches as ground truth
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
-->

```yaml
adr: 2
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
roster_delegable: # k/n over requires_tasks, or `pending`
```

<!-- One or two sentences: which children are delegable today, which
     child is costing the roster most, and — if ED<=1 — which part of the
     integration evidence needs a person or a device. -->
