---
name: Scientific task
about: A rigorously framed unit of work — defect, improvement, or investigation — with a falsifiable hypothesis and evidence-backed completion criteria
labels: ["tier:task"]
---

<!--
  Template: scientific-task v7 (2026-08)

  RULES — for humans and LLM agents alike, filing or executing.

  1. Evidence, not memory. Every code claim carries file:line at a named
     commit, re-derived at that commit — quote the line you cite. Pin that
     commit once in Status & Dependencies and cite by commit-locked
     permalink, never a branch path (branch links rot the moment the
     branch is deleted). Source comments and audit summaries are hearsay
     until re-derived. Aggregate claims (counts, "all X", "no Y anywhere")
     carry the exact command that produced them and its output.
  2. No padding. A section that does not apply is "N/A — <one-line
     reason>", decided at filing time. A criterion discovered to be wrong
     during execution gets an issue comment with evidence — do not work
     around it and do not silently edit it.
  3. Predictions are observable: do X, observe Y. For defects and
     improvements, at least one prediction must fail at the named commit,
     and its failure must be OBSERVED before filing — run it and paste
     the command and wrong output into §2 (Observations). For
     investigations, state instead the decision criterion: the
     observation that discriminates between the candidate answers.
  4. Atomic scope. One hypothesis cluster per issue; where scopes touch a
     sibling issue, §12 (Related Work) states which issue owns which fix.
  5. Cross-references cite the section NAME — "§ Threats to Validity" —
     optionally with its number. The name is canonical: numbers drift
     across template versions, names do not. Never a bare number.
     Subsections of § Interface & Data Contract may use the short form
     "§7.N (<short name>)" — e.g. "§7.4 (Internal interfaces — public)"
     — the parenthesized name is still required.
  6. Executors: before step one of §8 (Method), re-verify every
     observation in §2 (Observations) at your checkout. If one fails to
     reproduce, or a hypothesis is refuted
     mid-work, stop and comment on the issue with the refuting evidence.
     A refuted issue is a successful experiment, not a task to salvage.
     If the observations no longer fail because the work has already
     landed, the issue is superseded — close it with that note instead of
     re-doing it. Re-derive any drifted line numbers before trusting them;
     a stale citation is not evidence.
  7. Labels are not applied automatically for API-filed issues: set
     `bug` or `enhancement` explicitly, matching the corpus, plus the
     tier label `tier:task`.
  8. Tier model — task → feature → capstone (canonical edge rules in
     the feature template; read them there, they are not restated
     here). This is the task tier, and the model is strictly layered:
     A TASK'S ORDERING EDGES GO TO TASKS ONLY. Never to a feature,
     never to a capstone — upward edges are illegal in both
     directions. A task that seems to "wait on a feature" is really
     waiting on specific sibling TASKS inside it; name those.
     `related` is reference-only, may point at ANY tier, and carries
     neither ordering nor ownership.

     THIS TASK DECLARES NO OWNER. A task may be shared by any number
     of features, and ownership lives solely in each feature's
     requires_tasks roster. There is no part_of_feature field: it was
     retired in scientific-task v7 / feature v4 because the
     single-owner rule it enforced was never the intended model and
     had pushed genuine shared ownership into `related`. To find the
     features that own this task, search the rosters — an executor
     does not need them to do the work, and the readiness derivation
     supplies them to any workflow that does.

     An issue's tier is defined by its machine block's `tier:` key;
     the tier:* label is a mirror for filtering — a missing or stale
     label is bookkeeping to fix, never an edge violation. The
     ORDERING GRAPH is blocked_by/blocks edges PLUS composition edges
     read child-before-parent (a parent cannot close before its
     children land); that combined graph must stay a DAG. The machine
     block in Status & Dependencies is the source of truth for the
     edges it can express.
  9. Amendment & comment protocol. This body may be edited, but only
     together with an `AMENDED:` comment stating what changed, why,
     and on what evidence — a silent edit is invisible to executors,
     who reconstruct state from the machine block plus the prefixed
     comments (`STATUS:` / `REFUTED:` / `HANDOFF:` / `SUPERSEDED:` /
     `AMENDED:` / `WAIVED:`). Post each such comment on THIS issue
     and mirror the same comment on every feature whose requires_tasks
     roster lists this task (there may be several, or none). When an edit REMOVES or NARROWS any claim, observation,
     prediction, criterion, or scope item, the AMENDED comment must
     carry a "Dropped/Retired" ledger enumerating each removed item
     with its disposition — retired with reason, moved to issue #N, or
     restated where — so omissions are auditable by reading the
     comment, not only by diffing bodies. Bookkeeping is exempt from
     the AMENDED requirement: ticking a Method or Completion checkbox
     whose backing evidence is already recorded in a comment or PR,
     and re-pinning evidence_commit after re-deriving citations.
     Checkbox state remains a convenience rendering — the recorded
     evidence is the record. Write mechanics: cite a line range as ONE
     link — "[L100–L120](<permalink>#L100-L120)" — never two adjacent
     links joined by a dash (some write paths corrupt that form), and
     after any body edit re-fetch the issue and verify the rendered
     result before considering the edit done.
  10. Waivers. A completion criterion may be waived only via a
     `WAIVED:` comment naming the reason AND the successor issue that
     now tracks the dropped obligation (or stating explicitly why no
     successor is needed). A criterion skipped without a WAIVED
     comment leaves the issue unclosable.

  11. Oracle custody. Every completion criterion that compares against an
     expected value names WHO produced that value and WHEN. Values
     committed and reviewed BEFORE the implementation, and values written
     by the same change as the implementation, are different guarantees,
     and only the first is evidence. Where an expected-output artifact is
     generated by the same change that produces the behaviour it certifies,
     say so in §9 (Data Collection & Analysis) and apply the corresponding
     deduction in § Agentic Delegability. A check authored alongside the
     thing it checks passes indefinitely and is then cited as ground truth
     by everything built on it.
  12. Artifact paths resolve at filing. Every path a criterion names either
     exists at `evidence_commit` or is created by this issue, and §14 says
     which. A criterion pointing at something that does not exist is
     unclosable: whoever picks it up will either invent a location or skip
     the criterion silently.
  13. Open Questions are MARKED, not merely listed. Each entry ends in
     exactly one of `Recommended default: <answer>` (an executor may
     proceed), `BLOCKING: <who decides>` (an executor must stop), or
     `HYGIENE: <what to re-derive>` (not a decision at all — a citation, a
     status check, an un-run build). This template requires the section, so
     every issue has one; without a marker a reader cannot tell a decision
     that was made from one that was dodged, and § Agentic Delegability
     scores the entry as unresolved, which is usually not what was meant.

  Decision/spike tasks ("evaluate X", "decide Y") use this template
  with "verdict recorded in <named doc/section>" as the completion
  contract; sections that presuppose a code defect are N/A (rule 2).

  These comments do not render on GitHub — leave them in place for the
  next reader of the raw issue body.
-->

## Abstract

<!-- 2-4 sentences: what is wrong or missing, why it matters, and the
     one-line shape of the proposed remedy. -->

## Intended Audience & Impact

<!-- Who is this work for, and how is it meaningful to them? Name the
     concrete audience(s) JLS actually serves and, per audience, the
     change they experience — what they can do afterward that they can't
     today, or what stops going wrong for them. Impact claims are
     observations too (rule 1): where the harm or gap is measurable
     (a wrong simulation, a crash, a lost edit, a silent mis-load),
     point at it.

     The recurring audiences — pick and name the ones this task serves,
     do not list them all:
       - Students drawing and simulating circuits in the editor.
       - Instructors authoring, grading, or auto-checking work in batch
         (`-b`) mode.
       - Circuit-file authors and third-party tools that read or write
         `.jls` files against docs/file-format.md.
       - Contributors, maintainers, and LLM agents working the codebase.
       - Packagers and distributors shipping the installers and container.
     If a task genuinely serves an internal audience only (a refactor, a
     CI gate), say so and name the downstream audience it protects; "N/A"
     with no audience means the work has no beneficiary and does not
     belong on the backlog (rule 2). -->

## Status & Dependencies

<!-- The front matter an executor reads before touching anything. Keep it
     at the top, not buried in §12 (Related Work) — an issue picked up cold
     must not miss a blocker (a fix hardening code another issue is about
     to delete is wasted work). The machine block is the source of truth
     for graph assembly (rule 8); annotate each blocked_by entry with the
     one-line reason it blocks, as a YAML comment. Evidence commit is the
     single SHA every §2 (Observations) file:line is pinned to — cite by
     permalink at this commit (rule 1); if HEAD has moved, re-derive
     citations before trusting them. Before executing, run the
     supersession check: confirm the work has not already shipped
     (rule 6); if it has, close as superseded rather than re-doing it. -->

```yaml
tier: task
evidence_commit:        # SHA all §2 citations are pinned to
owned_by_derived: []    # OPTIONAL and NON-AUTHORITATIVE. A generated copy of the
                        #   features whose requires_tasks lists this task, so the
                        #   issue names its owners when read alone. Those rosters
                        #   are the only real record; regenerate this rather than
                        #   hand-editing it. Validator G21 reports any drift.
blocked_by: []          # ordering: TASKS that must land first — tasks only.
                        #   Never a feature, never a capstone (upward edges are
                        #   illegal); name the specific sibling tasks instead.
blocks: []              # ordering: TASKS waiting on this one — tasks only
related: []             # reference only — never blocking, never ownership.
                        #   Ownership is not recorded here: it lives in each
                        #   owning feature's requires_tasks roster (feature v4).
```

## 1. Background & Prior Work

<!-- What already exists: relevant code paths, prior issues/PRs, audit
     findings, external tools or literature. Link them. -->

## 2. Observations

<!-- Numbered, reproducible facts. Each carries file:line at a named
     commit plus the quoted line(s), or the exact command and its output.
     Include the observed failure required by rule 3. -->

## 3. Research Question

<!-- The single question this work answers, phrased so the answer is
     yes or no. -->

## 4. Hypothesis (falsifiable)

<!-- H1, H2, ...: statements about root cause or expected effect that the
     Method can prove wrong. If no observation could refute it, it is not
     a hypothesis — rewrite it. -->

## 5. Predictions

<!-- P1, P2, ...: concrete observable outcomes if the hypothesis holds,
     each phrased as: do X, observe Y. Mark which fail at the named
     commit (pre-fix) and which must hold after the fix. -->

## 6. Materials & Apparatus

<!-- Toolchain, fixtures, test rigs, corpora. Note anything that does not
     exist yet and must be built first. -->

## 7. Interface & Data Contract

<!-- The shape of the work before the work: everything this task touches
     at a boundary, stated precisely enough that a reviewer can check the
     eventual diff against it. This section MUST be filled in before any
     proposed diff — diffs belong in §8 (Method) or later, never ahead of
     the contract they implement. Rule 2 applies per subsection:
     inapplicable ones are "N/A — <one-line reason>", and claims about
     existing code carry file:line evidence (rule 1). -->

### 7.1 External interfaces modified

<!-- Surfaces visible outside the process that this work changes: the
     `.jls` file format (docs/file-format.md), CLI/batch (`-b`) flags and
     exit codes, exported HDL, printed or exported output, GUI surfaces
     third parties depend on, installer/container layout. For each: the
     surface, the change, and who sees it. -->

### 7.2 External interfaces consumed

<!-- Surfaces outside this codebase the work depends on: JDK/Swing/AWT
     APIs, file formats read, environment variables, fonts, OS services,
     CI facilities. Note version or platform assumptions. -->

### 7.3 Data consumed (structure)

<!-- Each input: where it comes from, its format/schema/encoding/units,
     and the authoritative definition of that structure (link it — e.g.
     docs/file-format.md), plus whether the source is trusted or must be
     treated as hostile (a user-supplied `.jls` file is hostile input). -->

### 7.4 Internal interfaces provided — public

<!-- Classes, methods, or packages this work adds or changes that OTHER
     parts of the codebase are meant to call: the signature, the expected
     caller(s), and the behavioral contract (pre/postconditions, error
     behavior). -->

### 7.5 Internal interfaces provided — private

<!-- Implementation-only helpers introduced: what they do and how their
     privacy is enforced (visibility modifier, package placement) so they
     do not silently become load-bearing API. -->

### 7.6 Data provided (structure)

<!-- Each output: format/schema/encoding/units and where that structure
     is authoritatively defined; for a new structure, define it here or
     name the doc that will. -->

### 7.7 Data durably tracked

<!-- State that outlives the process: files written, settings, on-disk
     formats. For each: where it lives, its structure and version, and
     the migration story for data written by older versions. -->

### 7.8 Data ephemerally used

<!-- Transient state: caches, undo stacks, simulation state, temp files.
     For each: its lifetime, what invalidates it, and what happens if it
     is lost mid-operation. -->

### 7.9 Concurrency model

<!-- For each interface and data item above: synchronous, asynchronous,
     parallel, or concurrent — and the mechanism (EDT vs. background
     thread, SwingWorker, locks, immutability, single-threaded batch
     mode). Name what guards each piece of shared mutable state;
     "synchronous, EDT-only" is a complete answer where true. -->

### 7.10 Data transformations

<!-- How the inputs of §7.3 (Data consumed) and the stored data of §7.7
     (Data durably tracked) become the outputs of §7.6 (Data provided):
     the pipeline stage by stage, with the representation at each stage
     boundary. Every transform must be FULLY DEFINED — no "then it is
     processed" hand-waving — and expressed as embedded LaTeX math
     using GitHub's math rendering (inline $`...`$ or display $$...$$
     blocks; syntax reference:
     https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/writing-mathematical-expressions).
     For each stage: name the transform, give its signature over the
     structures declared in §7.3 (Data consumed) and §7.6 (Data
     provided), and define the mapping itself, e.g.

       $$f_{\mathrm{route}} : \mathrm{Wire} \times \mathrm{Grid}
         \to \mathrm{Segment}^{*}, \qquad
         f_{\mathrm{route}}(w, g) = \ldots$$

     with side conditions and partiality explicit — wherever a
     transform is undefined, §7.11 (Failure modes & error handling)
     owns the behavior. Prose may accompany the math; it may not
     replace it. A reviewer must be able to locate each defined stage
     in the diff. -->

### 7.11 Failure modes & error handling

<!-- At each boundary above: what happens on malformed input, missing
     resources, I/O failure, or interrupted operation. Which errors are
     surfaced to the user, which are recovered, which abort — and what
     durable data is guaranteed not to be corrupted on the way down. -->

### 7.12 Compatibility, versioning & migration

<!-- What existing artifacts and callers keep working: old `.jls` files
     still load, save output stays byte-identical (or the version bump is
     documented), in-tree callers still compile, round-trips still hold.
     State each compatibility claim so a test can pin it. -->

## 8. Method / Experimental Design

<!-- Ordered checklist of the work, each step small enough to review.
     Every behavioral fix carries a regression test that fails at the
     pre-change commit and passes with the fix. Proposed diffs go here
     (or in later sections) — always after §7 (Interface & Data
     Contract), never before it. -->

- [ ] ...

## 9. Data Collection & Analysis

<!-- How results are recorded and judged: which tests assert what; what
     manual verification (with platform) is recorded in the PR. -->

## 10. Falsification Criteria

<!-- For each hypothesis: the specific post-fix observation that refutes
     it, and the next move if refuted. "If after the fix X still occurs,
     H1 is wrong — investigate Y instead." -->

## 11. Threats to Validity

<!-- What could make the results misleading: platform differences,
     headless-vs-GUI divergence, stale line numbers or counts, fixture
     bias, tests that shortcut the real code path. -->

## 12. Related Work

<!-- Tracking issue, sibling issues, audit findings, external references.
     Where scopes touch, state which issue owns which fix. -->

## 13. Conclusion & Future Work

<!-- Expected end state in one or two sentences; follow-ups explicitly
     out of scope here. -->

## Open Questions & Decisions Needed

<!-- Decisions this task cannot make for itself — cost, custody, policy,
     taste, or unverified external state. Separate what is answerable now
     from what genuinely needs a maintainer, so an executor knows what is
     safe to proceed on. For each: the question, the options with a
     recommended default, and whether it blocks filing, blocks execution,
     or can ride along. "N/A — fully specified" if nothing is open.

     Mark every entry per rule 13 — `Recommended default:`, `BLOCKING:`, or
     `HYGIENE:`. The marker is what an executor and § Agentic Delegability
     both read; an unmarked entry is treated as blocking. -->

## 14. Completion Criteria (Definition of Done)

<!-- How anyone — author, reviewer, or agent — recognizes this task is
     finished. Every box is checkable by pointing at evidence: a test
     name, a CI run, a command's output pasted in the PR. Edit the
     pre-filled boxes to fit (rule 2 governs inapplicable ones), and add
     criteria specific to this task.

     Integrity rule: tests verify the work, they do not define it. If a
     criterion below turns out to be wrong or unsatisfiable, follow
     rule 2 — comment with evidence, do not work around it. -->

- [ ] Every post-fix prediction in §5 (Predictions) verified; command and output recorded in the PR
- [ ] Every check in §10 (Falsification Criteria) performed post-fix; outcome (not refuted / refuted → action taken) recorded in the PR
- [ ] Post-change code re-checked against §7 (Interface & Data Contract): interfaces provided/consumed, data structures, concurrency model, and compatibility claims hold as declared; any deviation recorded as an issue comment (rule 2), not silently absorbed
- [ ] New regression tests fail at the pre-change commit and pass at the fix commit
- [ ] Existing tests pass unmodified, except tests whose asserted behavior this issue intentionally changes — each named, with the §5 prediction that justifies the new expectation
- [ ] `mvn verify` green (tests + SpotBugs, warnings-as-errors)
- [ ] No new entries in `config/spotbugs-exclude.xml`, or each new entry is `Class`-scoped with a justification
- [ ] No changes outside the scope of §8 (Method); adjacent work discovered en route is filed as new issues
- [ ] Every `blocked_by` entry in Status & Dependencies has landed, or the dependency was waived per rule 10
- [ ] Landing reported with a `STATUS:` comment on every feature whose `requires_tasks` lists this task, including any contract deviations those plans must reconcile
- [ ] Every cited evidence document and permalink resolves on the default branch at close — no branch-path links, no deleted docs
- [ ] Every skipped or waived criterion carries a `WAIVED:` comment naming its successor issue (rule 10)
- [ ] Not superseded: the §2 (Observations) failures still reproduced at pickup (rule 6); citations re-derived if HEAD had moved
- [ ] Every decision in Open Questions & Decisions Needed is resolved (or explicitly deferred), none left blocking, and every entry carries its rule 13 marker
- [ ] Every expected value this issue was graded against was pre-committed, or §9 records that it was authored in the same change and the ADR-1 OS deduction was applied (rule 11)
- [ ] § Agentic Delegability filled at filing and re-scored on any `AMENDED:` edit that changed scope, evidence, or open decisions
- [ ] ...


## Agentic Delegability (ADR-1)

<!--
  ADR-1 v1. How safely this issue can be handed to an automated or
  semi-automated executor, and what maintenance liability delegating it
  as-is would create. This is NOT a rating of how good, important or urgent
  the work is — the two are close to orthogonal. An issue can be excellent
  work, correctly specified and well evidenced, and still band F because
  closing it requires someone to physically operate a device. A LOW BAND IS
  A ROUTING DECISION, NOT A CRITICISM.

  Fill at filing time; re-score whenever an amendment changes the scope,
  the evidence, or the open decisions.

  Two failure modes are rated, not one: (i) the work is not finished, and
  (ii) the work is finished in a way that leaves a liability behind —
  duplicated logic, a check that asserts nothing, an abstraction chosen to
  make the change tractable, an expected-output file pinning the wrong
  behaviour, a document asserting evidence nobody gathered. The second is
  the dangerous one, because it is invisible to the gate that accepted it.

  THIS SECTION IS SELF-CONTAINED. The other tier templates carry their own
  tier-adjusted copies of these anchors, because issue templates have no
  include mechanism and a filer must be able to score from the one template
  in front of them. The copies CAN drift, and nothing detects it
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
      The test: could two competent implementers each satisfy the completion criteria
      and produce artifacts differing in a way a reviewer would care about?
      If so, it is not closed.
      5 every completion criterion names the artifact, its location, and the assertion
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

  OS  ORACLE STRENGTH — the check that would actually gate the merge. Not
      "could this be tested" but "is there a signal that is cheap to run,
      faithful to intent, and hard to satisfy the wrong way?"
      5 exact comparison against a pre-existing expected artifact; an
        algebraic property (round-trip, idempotence, invariance); or a
        differential check against an independent implementation
      4 behavioural assertions over enumerated cases INCLUDING the named
        negative and failure cases; or a compiler, type or analysis gate
        that fails loudly
      3 existence- or smoke-shaped: the artifact is produced, the command
        exits zero, the output is non-empty — satisfiable without being
        correct
      2 a threshold or a count, with no behavioural content
      1 a person reads prose and agrees
      0 none; success is asserted by whoever did the work
      DEDUCT 2 (floor 0) if the same change authors both the implementation
      and the expected values that certify it. That is the configuration in
      which a failing check gets "fixed" by weakening it.
      DEDUCT 1 if the acceptance evidence is a document asserting a
      measurement. The document is trivially producible; the measurement
      behind it is the real work and is not itself gated.

  BR  BLAST RADIUS — everything this unit of work must move, including generated
      files, expected-output artifacts, and anything published. Higher is
      smaller.
      5 one file, or one new file and its test; nothing published moves
      4 a handful of files within one module
      3 several files across two modules, every caller already identified
      2 more than two modules, or an interface whose callers this issue does
        not enumerate
      1 a repository-wide sweep, or a change to a published format, a
        command-line surface, or a registry everything downstream reads
      0 call sites unknowable without searching the whole tree, or a
        contract with consumers outside this repository

  HL  HORIZON LENGTH — the DEPENDENT-step chain, in expert time, from a
      cold start to gated evidence. Steps that can run in parallel count
      for less than steps that must run in order: reliability falls with
      the length of the dependent chain, not with total volume.
      5 under an expert-hour     4 one to four hours
      3 half a day to two days  2 several days, or two or more integration
        points that must be designed together
      1 more than a week, or a chain crossing a subsystem it must first learn
      0 a multi-week programme composed of other multi-day units

  PD  PRECEDENT DENSITY — is there a worked example of this exact shape already in this repository?
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
      5 one unit and its test
      4 one module; this issue's own citations are sufficient context
      3 two or three modules, or a module plus a format specification
      2 a subsystem boundary that must be held from both sides at once
      1 a repository-wide invariant that cannot be verified locally
      0 the repository plus the semantics of an external toolchain

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

  ED  ENVIRONMENTAL DETERMINISM — can the evidence this issue demands be produced by whoever
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

  DA  DESIGN AUTHORITY — does finishing require a decision the issue does
      not already make? Scored on this issue.
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
sc:              # 0-5  specification closure
os:              # 0-5  oracle strength — apply the deductions
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
debt:            # predicted debt mode, or NONE
```

<!-- One or two sentences: what an executor handed this issue today would
     actually do, where it would go wrong, and the single change that would
     raise the band. Name the section, the criterion, the artifact — not
     generic advice. -->
