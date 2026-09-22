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
     expected value names WHO produced that value and WHEN. "Pre-committed"
     (the expected values land, reviewed, before the implementation) and
     "same-change" (the executor writes the behaviour and the golden that
     certifies it together) are different guarantees, and only the first is
     evidence. Where a golden, fixture or expected-output file is generated
     by the same change that produces the behaviour, say so in §9 (Data
     Collection & Analysis) and apply the ADR-1 OS deduction below. Measured
     over the 688 open issues in 2026-09, the same-change configuration was
     the single largest predicted source of technical debt, at 58% of the
     corpus — a check that is green forever and is then cited as ground
     truth by every issue that builds on it.
  12. Artifact paths resolve at filing. Every path a criterion names either
     exists at `evidence_commit` or is created by this issue, and §14 says
     which. A criterion pointing at a file that no longer exists is
     unclosable: an executor will either invent a location or skip it
     silently. 42 open issues were in this state in 2026-09 after the docs
     tree was cleared.
  13. Open Questions are MARKED, not merely listed. Each entry ends in
     exactly one of `Recommended default: <answer>` (an executor may
     proceed), `BLOCKING: <who decides>` (an executor must stop), or
     `HYGIENE: <what to re-derive>` (not a decision at all — a citation, a
     status check, an un-run build). An unmarked entry is read as an
     unresolved structural decision and scores DA<=1 in § Agentic
     Delegability, which is usually not what the filer meant: when the
     whole corpus was graded in 2026-09 this single ambiguity depressed the
     axis by a measured 1.23 points across the board.

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
  ADR-1 v1 (2026-09). How safely this issue can be handed to an agentic LLM
  system — NOT how good, important or urgent it is. The two are close to
  orthogonal: an issue can be excellent work, correctly specified and well
  evidenced, and still band F because it needs an FPGA on a desk. A low band
  is a ROUTING DECISION, NOT A CRITICISM. Fill at filing time; re-score on
  any `AMENDED:` edit that changes scope, evidence or open decisions.

  THIS SECTION IS SELF-CONTAINED. The feature and capstone templates carry
  their own tier-adjusted copies of these anchors, worded for what those
  tiers actually deliver. That is deliberate duplication: markdown issue
  templates have no include, transclusion or file-reference mechanism in
  either the .md or the issue-forms .yml dialect, and a filer must be able
  to score an issue from the one template in front of them. The three
  copies CAN drift — when you change an axis here, change it in all three,
  and bump the version line above in each.

  Two failure modes are rated, not one: (i) the agent does not finish, and
  (ii) the agent finishes in a way that leaves a maintenance liability —
  duplicated logic, an assertion-free test, a premature abstraction, a
  golden pinning the wrong behaviour, a document asserting evidence nobody
  gathered. The second is the dangerous one: it is invisible to the gate
  that accepted it.

  SEVEN ADDITIVE AXES, 0-5 each. RAW = their sum, 0-35.

  SC  SPECIFICATION CLOSURE — is the end state fixed by the text alone?
      Test: could two competent implementers both satisfy it and produce
      artifacts differing in a way a reviewer would care about?
      5 every criterion names the artifact, its path, and the assertion
        pinning it; no "appropriate", "reasonable", "as needed"
      4 concrete; one or two open naming or layout choices no reviewer
        would litigate
      3 goal unambiguous, artifact shape is not; >=1 structural inference
      2 end state stated as an outcome, not an artifact
      1 problem and direction only; "done" is not written down
      0 open-ended, self-contradictory, or defers its own definition
      A §5 whose predictions are literally "do X, observe Y" plus a §14 of
      checkable artifacts is the SC=5 shape. A §14 item reading
      "documented"/"considered"/"reviewed" with no named file caps SC at 3.
      "N/A — <reason>" does not lower SC; template comment text left in
      place does.

  OS  ORACLE STRENGTH — the signal that would actually gate the merge. Not
      "could this be tested" but "is there a check that is cheap to run,
      faithful to intent, and hard to satisfy the wrong way?"
      5 byte or structural equality against a committed golden; a
        round-trip or idempotence property; a differential check against an
        independent implementation
      4 behavioural assertions over enumerated cases INCLUDING the named
        negative and refusal cases; or compiler, nullness or
        sealed-dispatch gates that fail loudly
      3 smoke-shaped: the artifact is produced, the command exits 0, the
        file is non-empty — satisfiable without being correct
      2 a threshold or a count (a coverage floor, a timing bound) with no
        behavioural content
      1 a human reads prose and agrees
      0 none; success is asserted by the executor
      DEDUCT 2 (floor 0) if the executor authors the oracle and the
      implementation in the same change with no pre-committed expected
      values (rule 11). That is the configuration in which a failing check
      gets "fixed" by weakening it.
      DEDUCT 1 if the acceptance evidence is a document asserting a
      measurement — a table of numbers, a scored matrix, a published
      figure. The prose is trivially producible; the measurement behind it
      is the real work and is not itself gated.

  BR  BLAST RADIUS — what must move, generated files, goldens and published
      contracts included. Higher = smaller.
      5 one file, or one new file and its test; no published contract moves
      4 2-4 files in one package
      3 5-10 files or 2 packages; one internal interface, all callers in view
      2 multiple packages, or an interface with callers this issue does not
        enumerate
      1 a tree-wide sweep, or a change to `.jls` format / the CLI surface /
        the element registry
      0 call sites unknowable without a full-tree search, or a contract with
        out-of-repo consumers

  HL  HORIZON LENGTH — the DEPENDENT-step chain in human-expert time, cold
      start to gated evidence. Parallelisable sub-steps count for less than
      serially dependent ones: compounding is what fails, not volume.
      5 <1 expert-hour   4 1-4h   3 4-16h
      2 2-5 expert-days, or >=2 integration points that must be co-designed
      1 >1 expert-week, or the chain crosses a subsystem it must first learn
      0 a multi-week programme composing other multi-day units

  PD  PRECEDENT DENSITY — is there a worked in-repo example of this shape?
      5 this issue names a specific in-repo precedent and it exists
      4 a near-identical sibling is in-tree, findable by name
      3 analogous patterns exist but need adaptation
      2 the category exists in-tree; this is the first instance of its kind
      1 no in-repo precedent; the pattern comes from an external tool or
        spec that this issue does cite
      0 no precedent named anywhere; the executor invents the shape
      PD is what predicts DUPLICATION. At 5 the agent copies a good local
      pattern; at 0 it copies whatever it saw in training, which is how a
      codebase with an explicit house style — tabs, `// end of X method`
      trailers, sealed dispatch with no `default` arm, records by default,
      the `@NullMarked` ratchet — acquires code that compiles, passes, and
      reads like it came from somewhere else.

  CF  CONTEXT FOOTPRINT — how much must be held simultaneously, not merely
      read.
      5 one class and its test
      4 one package; this issue's citations are sufficient context
      3 2-3 packages, or a package plus a file-format section
      2 a subsystem boundary (sim/core, gui/edit, hdl/elem) held on both sides
      1 a whole-tree invariant — determinism, nullness, sealedness, the
        coverage ratchet — that cannot be verified locally
      0 the tree plus an external toolchain's semantics (Yosys, nextpnr,
        cocotb, Wokwi, GTKWave)

  RD  REVERSIBILITY / DEBT SURFACE — cost of a wrong-but-plausible
      completion. Higher = cheaper to reverse.
      5 pure addition behind a test; revert is one commit, nothing depends
        on it
      4 internal; wrongness surfaces in CI or at the next touch
      3 latent but discoverable by a later reader; no external consumer
      2 the artifact becomes load-bearing: a committed golden, a ratchet
        floor, a coverage or mutation threshold, a `@NullMarked` package
      1 a published contract: `.jls` file-format text, a CLI flag, public
        API, a help page, an exported HDL shape
      0 irreversible outside the repo: a DOI, a Maven Central coordinate, a
        Marketplace listing, a tagged release, a third-party invitation
      RD=2 deserves its own care. A golden produced by the same agent that
      produced the behaviour is not evidence — it is a photograph of the
      behaviour. It will be green forever and will be cited as ground truth
      by every issue that builds on it.

  TWO CAPPING AXES. They do not add; they impose a ceiling on the band.

  ED  ENVIRONMENTAL DETERMINISM — can the evidence this issue demands be
      produced inside a headless container, by the executor, with no human,
      no hardware and no third party?
      5 headless and hermetic: `mvn verify` or an in-tree script does it
        .......................................................... no cap
      4 needs a pinned toolchain the repo can fetch — a nix devShell, a
        container, `xvfb-run` — and is reproducible ............... no cap
      3 needs a display substrate or network fetch that exists but is
        flaky, or an external corpus to download .................. cap B
      2 needs a specific host OS, a GPU, or a service account the executor
        does not hold ............................................. cap C
      1 needs physical hardware — an FPGA board, a breadboard, a screen
        reader, a display panel — or a screen recording of a real
        session ................................................... cap F
      0 needs OTHER PEOPLE: an n-of-5 trial, an independent reproducer, a
        second maintainer with merge rights, a peer reviewer, an outside
        volunteer ................................................. cap F
      ED<=1 IS NOT A CRITICISM. It means the deliverable is not a patch. An
      agent may still do most of it — write the harness, the script, the
      checklist, the analysis template — but it cannot close the issue, and
      marking it closed is exactly the fabrication this rubric exists to
      catch. Declaring ED<=1 at FILING time is the point: it tells whoever
      picks the issue up that they are not looking at a coding task.

  DA  DESIGN AUTHORITY — does finishing require a decision this issue does
      not already make?
      5 no open decisions; every choice stated or forced by existing code
        .......................................................... no cap
      4 only local reversible choices — a method name, a helper's home
        .......................................................... no cap
      3 decisions are named AND each carries a recommended default, so an
        executor proceeds unblocked ............................... cap B
      2 exactly one structural decision genuinely open, no preference
        stated .................................................... cap C
      1 two or more open with no preference, or a decision routed to
        another owner the executor must wait on ................... cap D
      0 the issue's own deliverable IS a decision — a verdict, a policy, a
        scope boundary, a does-this-premise-hold gate ............. cap D
      DA rates DESIGN authority only. These do NOT lower it:
        - evidence hygiene: "re-derive the citations", "line numbers will
          drift", "no build was run at filing"
        - bookkeeping: "confirm #N is still open", a roster re-sync
        - a question this issue answers itself with a recommended default
        - a decision already owned by a different issue this one waits on —
          that is `blocked_by`, and it scores in HL, not here
      Note the asymmetry with ED: DA=0 caps at D, not F, because an agent's
      research output on a decision is genuinely most of the value — it
      just cannot make the call. ED=0 caps at F because the agent's output
      is none of it.
      Measured, 2026-09: grading all 688 open issues WITHOUT these four
      exclusions collapsed 87% of the corpus to DA=1, because this
      template makes an Open Questions section mandatory and graders read
      "section populated" as "decision open". A re-grade with the
      exclusions moved the axis by a measured +1.23, strictly one-sided.
      A populated Open Questions section is not evidence of an open
      decision. Rule 13's markers exist to make this unambiguous.

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

  Filing a low band is a signal, not a confession. The cheapest way to
  raise one is almost always to answer an Open Question (rule 13) or to
  pre-commit the expected values (rule 11) — not to rewrite the issue.
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

<!-- One or two sentences: what an agent handed this issue today would
     actually do, where it would go wrong, and the single change that would
     raise the band. Name the section, the criterion, the artifact — not
     generic advice. -->
