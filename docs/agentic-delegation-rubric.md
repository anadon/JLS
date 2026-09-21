# Agentic Delegation Rubric (ADR-1)

*Which issues an agentic LLM system can be handed and expected to finish
without leaving technical debt behind.*

Status: **v1, 2026-09-21.** Applies to every open `tier:capstone`,
`tier:feature` and `tier:task` issue. This document is the scoring
authority; a grade comment on an issue is a rendering of it, not a
substitute for it.

---

## 0. What this rubric is measuring, and what it is not

It answers one question per issue:

> If this issue were handed to an agentic LLM system today, with repository
> access and CI, what is the probability it is finished **correctly** and
> **without the finishing act itself creating a maintenance liability**?

Two failure modes are therefore graded, not one:

1. **Non-completion** — the agent does not produce a correct change.
2. **Debt-incurring completion** — the agent produces a change that passes
   whatever gate was put in front of it while leaving the repository worse:
   duplicated logic, an assertion-free test, a premature abstraction, a
   golden file that pins the wrong behavior, a document asserting evidence
   nobody gathered, a public contract that has to be un-shipped.

The second is the dangerous one, because it is invisible to the gate that
accepted it. Every axis below exists because a measured failure mode maps
onto it.

**This rubric does not grade issue quality, importance, or urgency.** An
issue can be excellent work, correctly specified, well evidenced, and still
score F — because it needs an FPGA on a desk, or because it asks someone to
decide where a module boundary goes. F is a routing decision, not a
criticism.

### 0.1 Evidence base

The axes are not invented. Each is anchored to a measured effect:

| Effect | Measurement | Axis it drives |
|---|---|---|
| Underspecification is the dominant disqualifier of "real" issues | 68.3% of SWE-bench samples were filtered from SWE-bench Verified for underspecification / unfair tests | SC |
| Verification, not generation, is now the binding constraint; proxies get gamed (overwritten tests, deleted assertions, memorised inputs) | *The Verification Horizon* (2026) | OS |
| Multi-file changes roughly halve success relative to single-file | ~14.75% → ~7.55% on one repository-level benchmark | BR |
| Agent reliability decays with task length; 50%-success horizon is a measured, model-dependent quantity | METR time-horizon series (7-month doubling 2019–2025; ~4-month 2024–2025) | HL |
| Per-step error compounds: 95%/step over 20 dependent steps ≈ 36% end-to-end | long-horizon failure-diagnosis literature | HL, BR |
| Agents follow explicit constraints faithfully but do not choose where boundaries belong; humans make ~70% of planning decisions, agents ~80% of execution decisions | ~400k Claude Code sessions, Oct 2025–Apr 2026 | DA, PD |
| Every frontier model degrades as input length grows, worst in the middle of context | Chroma "context rot", 18 models | CF |
| AI-era commits shift from moving code to copying it: +41% within-commit copy/paste, +81% duplicated blocks, moved code 21% (2022) → 3.8% (2026) | GitClear, 211M+ LOC | RD |
| Agent-written tests are runnable but weak: null-checks instead of behavioral assertions, shared mutable fixtures, print statements preferred to assertions | VibeCheck and follow-ons | OS, RD |
| Experienced developers on mature repositories were 19% *slower* with AI while believing they were 20% faster | METR RCT, 16 devs / 246 tasks | whole-rubric calibration |

The last row is the calibration warning for this entire exercise: on a
mature codebase with a high correctness bar — which JLS is — perceived
agent capability systematically exceeds measured agent capability. Grade
against the measurement, not the feeling.

---

## 1. Structure

Nine axes. Two of them (**ED**, **DA**) are **capping** — they impose a
ceiling on the final band regardless of the additive score. Seven are
**additive**, scored 0–5.

```
Additive axes (0-5 each, max 35):  SC  OS  BR  HL  PD  CF  RD
Capping axes (0-5 each):           ED  DA
```

| Code | Axis | Question |
|---|---|---|
| SC | Specification Closure | Is the end state determined by the text alone? |
| OS | Oracle Strength | Is there a machine-checkable, hard-to-game acceptance signal? |
| BR | Blast Radius | How many files, modules and published contracts move? |
| HL | Horizon Length | How long is the dependent-step chain? |
| PD | Precedent Density | Is there an in-repo exemplar to pattern-match against? |
| CF | Context Footprint | How much of the repo must be held in mind at once? |
| RD | Reversibility / Debt Surface | If wrong, how expensive is the artifact to un-ship? |
| ED | Environmental Determinism | Can the required evidence be produced in a sandbox? |
| DA | Design Authority | Does finishing require a decision that is not yet made? |

---

## 2. Additive axes

### SC — Specification Closure (0–5)

Read only the issue body. Could two competent implementers, working
independently, produce artifacts that both satisfy it and that differ in a
way a reviewer would care about? If yes, the spec is not closed.

| Score | Anchor |
|---|---|
| 5 | Every acceptance criterion names the artifact, its location, and the assertion that pins it. No criterion contains "appropriate", "reasonable", "as needed", "etc." |
| 4 | Criteria are concrete; one or two leave a naming or layout choice open that a reviewer would not litigate. |
| 3 | The goal is unambiguous; the shape of the artifact is not. A reader must infer at least one structural decision. |
| 2 | The end state is stated as an outcome, not an artifact. Several structural decisions are left to the executor. |
| 1 | The issue states a problem and a direction. What "done" looks like is not written down. |
| 0 | Scope is open-ended or self-contradictory; or the issue explicitly defers its own definition to a future decision. |

**JLS-specific:** a filled §5 (Predictions) whose entries are literally
"do X, observe Y", plus a §14 Completion Criteria list of checkable
artifacts, is the SC=5 shape. A §14 containing "documented", "considered",
or "reviewed" without a named file is at most SC=3. Sections rendered
"N/A" with a reason do not lower SC; sections left as template comment text
do.

### OS — Oracle Strength (0–5)

Not "can it be tested" — *does a signal exist that is cheap to run,
faithful to intent, and robust against being satisfied the wrong way?*
Score the oracle that would actually gate the merge.

| Score | Anchor |
|---|---|
| 5 | Byte-level or structural equality against a committed golden, a round-trip/idempotence property, or a differential check against an independent implementation. Passing it the wrong way requires actively corrupting the golden. |
| 4 | Behavioral assertions over enumerated cases, including the named negative/refusal cases. Compiler, type, nullness or sealed-dispatch checks that fail loudly. |
| 3 | A test exists but is existence- or smoke-shaped: the artifact is produced, the command exits 0, a file is non-empty. Satisfiable without being correct. |
| 2 | The oracle is a threshold or a count (coverage floor, timing bound) with no behavioral content. |
| 1 | The oracle is a human reading prose and agreeing. |
| 0 | No oracle. Success is asserted by the executor. |

**Deduct 2** (floor 0) if the issue asks the executor to *author the oracle
and the implementation in the same change* with no pre-committed expected
values. That is the configuration in which agent-written tests reliably
degenerate into runnable-but-assertion-free tests, and in which a failing
check is "fixed" by weakening it.

**Deduct 1** if the acceptance evidence is a documentation artifact that
asserts a measurement (a table of numbers, a scored matrix, a published
figure) — the prose is trivially producible; the measurement behind it is
the real work and is not itself gated.

### BR — Blast Radius (0–5)

Count what the change *must* touch, including generated files, goldens and
published contracts. Higher score = smaller radius.

| Score | Anchor |
|---|---|
| 5 | One file, or one new file plus its test. No published contract moves. |
| 4 | 2–4 files in one package. No cross-package contract moves. |
| 3 | 5–10 files, or 2 packages. One internal interface changes with all callers in view. |
| 2 | Multiple packages, or a public/internal interface with callers the issue does not enumerate. |
| 1 | Tree-wide sweep, or a change to `.jls` file format / CLI surface / element registry that everything downstream reads. |
| 0 | Cross-cutting change whose call sites are not knowable without a full-tree search, or a change to a contract with out-of-repo consumers. |

### HL — Horizon Length (0–5)

Estimate the *dependent-step chain*, in human-expert time, from a cold
start to gated evidence. Independent, parallelisable sub-steps count less
than serially dependent ones — compounding is what kills, not volume.

| Score | Anchor |
|---|---|
| 5 | < 1 expert-hour. A handful of dependent steps. |
| 4 | 1–4 hours. Single sitting, one investigation. |
| 3 | 4–16 hours. Multiple dependent investigations, one integration point. |
| 2 | 2–5 expert-days, or ≥ 2 integration points that must be co-designed. |
| 1 | > 1 expert-week, or the chain crosses a subsystem it must first learn. |
| 0 | Multi-week programme; composition of other multi-day units. |

**Tier prior:** a capstone composes features and therefore cannot score
above HL=1 *as a single delegation*. That is not a defect of the capstone;
it is the reason the tier exists. Grade the capstone on whether its
*children* are delegable, and say so in the verdict.

### PD — Precedent Density (0–5)

Does the repository already contain a worked example of this exact shape
that the agent can copy the structure of?

| Score | Anchor |
|---|---|
| 5 | The issue names a specific in-repo precedent to model on, and that precedent exists. (JLS does this well: "on the `docs/wayland-desktop-checklist.md` precedent", "modeled on the icestick-bitstream-handoff".) |
| 4 | A near-identical sibling exists in-tree and is easy to find by name. |
| 3 | Analogous patterns exist but need adaptation. |
| 2 | The category exists in-tree; this instance is the first of its kind. |
| 1 | No in-repo precedent; the pattern must be imported from an external tool or spec that the issue does cite. |
| 0 | No precedent anywhere the issue names. The executor invents the shape. |

PD is the axis that most directly predicts *duplication* debt. At PD=5
the agent copies a good pattern; at PD=0 it copies whatever it saw in
training, which is how a codebase with an explicit house style (tabs,
`// end of X method` trailers, sealed dispatch with no `default` arm,
records-by-default, `@NullMarked` ratchet) acquires code that compiles,
passes, and reads like it came from somewhere else.

### CF — Context Footprint (0–5)

How much of the tree must be simultaneously understood — not read, *held* —
for the change to be correct?

| Score | Anchor |
|---|---|
| 5 | One class and its test. Self-contained. |
| 4 | One package; the issue's citations are sufficient context. |
| 3 | Two or three packages, or a package plus a file-format section. |
| 2 | A subsystem boundary (sim↔core, gui↔edit, hdl↔elem) must be held on both sides. |
| 1 | Whole-tree invariant (determinism, nullness, sealedness, coverage ratchet) that cannot be locally verified. |
| 0 | Requires holding the tree plus an external toolchain's semantics (Yosys, nextpnr, cocotb, Wokwi) at once. |

### RD — Reversibility / Debt Surface (0–5)

If the agent completes this *wrongly but plausibly*, how expensive is the
mistake to discover and undo? Higher score = cheaper to reverse.

| Score | Anchor |
|---|---|
| 5 | Pure addition behind a test. Revert is a single `git revert`; nothing depends on it. |
| 4 | Internal change; wrongness surfaces in CI or at the next touch. |
| 3 | Wrongness is latent but discoverable by a later reader; no external consumer. |
| 2 | The artifact becomes load-bearing: a committed golden, a ratchet floor, a coverage/mutation threshold, a `@NullMarked` package. Wrong values entrench and later work is graded against them. |
| 1 | Published contract: `.jls` file-format text, CLI flag, public API, help page, exported HDL shape. Un-shipping costs a deprecation. |
| 0 | Irreversible outside the repo: a DOI, a Maven Central coordinate, a Marketplace listing, a tagged release, a third-party invitation. |

RD=2 deserves its own warning. A committed golden produced by the same
agent that produced the behavior is not evidence — it is a photograph of
the behavior. It will be green forever and will be *cited* as evidence by
every subsequent issue. This is the single highest-yield debt mechanism
available to an agent in this repository.

---

## 3. Capping axes

These do not add. They cap.

### ED — Environmental Determinism (0–5)

Can the evidence the issue demands be produced inside a headless CI
container, by the agent, with no human, no hardware, and no third party?

| Score | Meaning | Cap |
|---|---|---|
| 5 | Fully headless and hermetic. `mvn verify` or an in-tree script produces everything. | none |
| 4 | Needs a pinned toolchain the repo can fetch (nix devShell, Docker image, `xvfb-run`). Reproducible. | none |
| 3 | Needs a display substrate or network fetch that is available but flaky, or a pinned external corpus that must be downloaded. | **B** |
| 2 | Needs a specific host OS, GPU, or a service account the agent does not hold. | **C** |
| 1 | Needs physical hardware (FPGA board, breadboard, screen reader, a specific display panel) or a screen recording of a real session. | **F** |
| 0 | Needs other people: n-of-5 human trials, an independent reproducer, a second maintainer with merge rights, a peer reviewer, a third-party volunteer. | **F** |

ED≤1 is not a criticism of the issue. It means *the deliverable is not a
patch*. An agent may still do 80% of it — write the harness, the script,
the checklist, the analysis template — but it cannot close it, and marking
it closed would be the fabrication mode of debt.

### DA — Design Authority (0–5)

Does completing this require making a decision that the issue does not
already make?

| Score | Meaning | Cap |
|---|---|---|
| 5 | No open decisions. Every choice is stated or forced by existing code. | none |
| 4 | Only local, reversible choices (a method name, a helper's location). | none |
| 3 | One named decision with the alternatives enumerated and a stated preference. | **B** |
| 2 | One structural decision left open: where a seam goes, what a type's field list is, which of two mechanisms to use. | **C** |
| 1 | Multiple coupled structural decisions, or a decision the issue explicitly routes to `## Open Questions & Decisions Needed` without answering. | **D** |
| 0 | The issue *is* the decision. Its deliverable is a verdict, a policy, a scope boundary, or a "does this premise hold" gate. | **D** |

A DA=0 issue can be *researched* by an agent to a very high standard and
must be *decided* by a human. Delegating the decision is how a project
acquires architecture nobody chose. Note the asymmetry with ED: DA=0 caps
at **D**, not F, because the agent's research output is genuinely most of
the value; ED=0 caps at F because the agent's output is none of it.

---

## 4. Composite and bands

```
RAW   = SC + OS + BR + HL + PD + CF + RD          (0-35)
CAP   = min(cap(ED), cap(DA))                      (band ceiling)
BAND  = min(band_from(RAW), CAP)
```

| RAW | Band before capping |
|---|---|
| 30–35 | **A** |
| 24–29 | **B** |
| 17–23 | **C** |
| 10–16 | **D** |
| 0–9  | **F** |

### Band meanings — these are routing instructions

| Band | Meaning | Action |
|---|---|---|
| **A** | Delegable end-to-end. Agent opens the PR, CI is the gate, a human reads the diff once. | `DELEGATE` |
| **B** | Delegable with one named human checkpoint — usually approving an oracle or a naming/seam choice before implementation. | `DELEGATE-WITH-CHECKPOINT` |
| **C** | Not delegable as written. Something must be supplied first: a closed spec, a pre-committed golden, or a decomposition. The supplying act is itself often an A/B task. | `SPECIFY-FIRST` or `SPLIT` |
| **D** | Human-led. The agent contributes research, drafts, harnesses and analysis; the human makes the call and owns the artifact. | `HUMAN-LED` |
| **F** | Not agent-completable. Either the evidence requires the physical/social world (ED≤1), or the composite is so low that any agent output would be a liability. | `HUMAN-ONLY` / `AGENT-ASSIST-ONLY` |

### Predicted debt mode

Every grade names the *specific* liability that delegating as-is would
create. Use exactly one of these tags, chosen by the lowest-scoring
relevant axis:

| Tag | Trigger | What it looks like six months later |
|---|---|---|
| `SPEC-DRIFT` | SC ≤ 2 | The artifact satisfies the issue and not the intent; the gap is discovered by the next issue that builds on it. |
| `HOLLOW-ORACLE` | OS ≤ 2, or the OS=-2 deduction fired | A green test that asserts non-nullity. Regression coverage that is decorative. |
| `GOLDEN-LOCK-IN` | RD = 2 and OS ≥ 4 | A golden generated from the agent's own output, now cited as ground truth by five downstream issues. |
| `DUPLICATION` | PD ≤ 1 | A second implementation of something the tree already has, in a style the tree does not use. |
| `PREMATURE-SEAM` | DA ≤ 2 | An abstraction chosen to make the change tractable, now load-bearing and wrong. |
| `PARTIAL-INTEGRATION` | BR ≤ 1 or CF ≤ 1 | Call sites the agent never found. Compiles, passes, silently wrong on the path nobody tested. |
| `FABRICATED-EVIDENCE` | ED ≤ 1 | A checklist marked done, a measurement table with plausible numbers, a "verified" claim behind which no one ran anything. **The most severe mode.** |
| `SCOPE-EXHAUSTION` | HL ≤ 1 | The agent lands the first third, declares victory, and leaves scaffolding the next executor must reverse-engineer. |
| `NONE` | no axis in its trigger range | — |

### Decomposition (capstones and features)

For `tier:capstone` and `tier:feature`, the parent's own band is nearly
useless in isolation — a capstone is a composition, so it is HL-capped by
construction and would read as D/F no matter how good it is. The parent's
band therefore answers only "can this be handed over as one unit" (almost
always: no). The useful quantity is the **delegable fraction** of its
roster — how many of its declared children are individually band A or B —
and the **blocking child**, the one whose ED or DA cap propagates upward.

Both are computed corpus-wide, after every tier has been graded, and are
reported in the corpus report rather than in the per-issue comment: a
parent is graded before its children's bands exist, so a per-comment
fraction would be either stale or a second pass. Each capstone/feature
grade records its declared roster (`requires_tasks` / `requires_features`
/ `requires_capstones`) so the fraction is derivable without re-reading
anything.

---

## 5. The grade comment format

Posted as an issue comment, verbatim shape:

```markdown
## Agentic Delegation Grade — ADR-1

| Axis | Score | Note |
|---|---|---|
| SC — Specification Closure | n/5 | … |
| OS — Oracle Strength | n/5 | … |
| BR — Blast Radius | n/5 | … |
| HL — Horizon Length | n/5 | … |
| PD — Precedent Density | n/5 | … |
| CF — Context Footprint | n/5 | … |
| RD — Reversibility / Debt Surface | n/5 | … |
| **RAW** | **n/35** | |
| ED — Environmental Determinism *(capping)* | n/5 | cap: X |
| DA — Design Authority *(capping)* | n/5 | cap: X |

**Band: X — ACTION**  ·  **Predicted debt mode: `TAG`**

<one paragraph: what an agent would actually do with this issue, where it
would go wrong, and what single change would raise the band>

<capstone/feature only: Delegable fraction: k/n children band A-B.
Blocking child: #N (reason).>

---
*Graded against [`docs/agentic-delegation-rubric.md`](…) (ADR-1). This
grades delegability, not issue quality or importance — a low band is a
routing decision, not a criticism.*
```

---

## 6. Known limitations of ADR-1

Stated so they are not discovered as surprises:

1. **The axes are correlated.** BR, CF and HL co-move; an issue touching
   many files usually needs more context and more time. RAW therefore
   over-weights size relative to a decorrelated model. Accepted: the
   correlation is real in the world too, and a decorrelated score would be
   harder to apply consistently across hundreds of graders.
2. **Scores are judgments, not measurements.** SC and OS are reasonably
   objective; HL is an estimate; PD depends on how hard the grader looked.
   Inter-rater agreement is unmeasured. The bands are wide (5–7 RAW points
   each) specifically to absorb this.
3. **It is calibrated to the current frontier.** The METR horizon series
   has been doubling every 4–7 months. An HL anchor written in September
   2026 will be wrong by mid-2027, and the direction of the error is
   known: HL will be too harsh. Re-anchor HL, and only HL, each time the
   horizon doubles.
4. **It grades the issue as written, at one moment.** An issue whose
   blockers land becomes more delegable without its text changing. Grades
   are perishable.
5. **It cannot detect a wrong premise.** An issue can be SC=5, OS=5, A-band
   and still be work that should not happen. That is what `FEAT-C25-0`-style
   premise-first gating is for, and it is outside this rubric's scope.
