**Capstone:** CAP-07 (#302) — a design drawn in JLS comes back from an open shuttle as a physical chip.
**Verdict: ready-with-gaps** — start now (the Demo slice, per the issue's own § Cost, is the right first move); the gaps below are reconciliation and bookkeeping work, not re-decomposition.

## 1. Decomposition

**Required set is filed, open, and largely composes.** All six of `requires_features: [327, 328, 336, 339, 358, 359]` are open features, each carrying `302` in its own `serves_capstones` (verified per issue). The native child (#328 FEAT-044) has its two spine tasks filed and natively linked (#429 TASK-0094, #432 TASK-0095, both open). The transitive closure — #315, #317, #353, #354, #363, #292, #416, #468, #373, #478, #398, #386 — is entirely open; no required edge points at a closed or redirected issue. The sufficiency/minimality argument in §2 of the issue is unusually concrete and each §1 step has exactly one owner among the six.

**Gap D-1 (largest finding): #873 breaks the corroboration claim.** The capstone asserts "each of these six issues carries 302 in its own `serves_capstones`, and no other filed feature does." That is false today: **#873** (open, FEAT: Memory/RegisterFile/FieldExtend export-or-reasoned-refusal, created 2026-08-08, serves reverified 2026-08-16) declares `serves_capstones: [302, 304]` and states in prose that "#302's tapeout path" needs stateful-element export and "none of them get it until this lands." Either #873 overclaims (a tapeout design can be drawn from `Register`/`ShiftRegister` alone, so the capstone survives without `Memory` export) or CAP-07 under-requires (a "design worth taping out" — the issue's own phrase, with #202's RV32I named as a plausible AC-5 fixture — plausibly uses `RegisterFile`). This must be adjudicated in a `REPLAN:` on #302 (add #873 to `requires_features` with the three-count update, or demote #873's edge to `related`) before AC-4's 20-design corpus is committed, because the corpus composition depends on which element vocabulary the export path must carry.

**Gap D-2: task-tier holes inside the required set.**
- **#339 FEAT-021** has `requires_tasks: []`, `planned_tasks: []`, and zero sub-issues — the only required feature with no decomposition at all. At 2–4 mw that may be intentional (single-unit feature), but nothing on #339 says so; its absorbed duplicate (TASK-0049/#474, closed 2026-08-08) suggests tasks were expected.
- **#359 FEAT-023** carries two `planned_tasks` explicitly marked **"Not filed"**: the hierarchical-golden work (TASK-0044, shared with #358) and the committed behavioral corpus, which "has no task id — the registry's task id space is closed at TASK-0112." Required-for-302 residual scope with no filed task is exactly the shape rule G exists for; today it is neither filed nor recorded as an exception (`requires_tasks_exception: []` on #302 is accurate only if this is resolved feature-side).
- **#328's roster inconsistency:** TASK-0094 and TASK-0095 are filed (#429, #432) and natively linked, yet still sit in `planned_tasks` while `requires_tasks` is `[]`. Filed tasks belong in `requires_tasks`; as written, #328 could "close" with no required child by its own machine block.

No double-ownership found: #416 (TASK-0052) is correctly single-owned by #359 and carried by reference on #328; #292 is single-owned as #358's sole task child.

## 2. Acceptance criteria

**Upward composition is deliberate and mostly sound.** AC-1..AC-8 are genuine system-level criteria: the issue correctly identifies that AC-2 (reset refusal on a *hierarchical* design), AC-4 (local-vs-shuttle zero-tolerance corpus), and AC-8 (cross-platform byte-identity) are conjunctions no single feature's criteria cover, and it owns them at capstone close rather than pretending a child owns them. AC-7 (chip-in-hand) being explicitly non-gating is the right call and prevents an undischargeable Definition of Done. #429's own DoD ("writes four files or none of them and names every problem") matches AC-1's shape.

**Gap A-1:** AC-5 is worded for the **deduplicated** hierarchy form while #358's Open Question 1 records that the **uniquified** form ships first (live disagreement with #292). The capstone's own Completion Criteria acknowledge this must be settled before AC-5 becomes a test — it is an open decision on the critical path of AC-5, currently unresolved on both #358 and #292.

**Gap A-2:** AC-4/AC-5 depend on the shuttle's CI accepting submissions — an external system with submission windows the project does not control. The issue names this (OQ7, KC-07-1) but no AC has a fallback if the program category loses its remaining entrant; the acceptance surface would need full re-derivation. Acknowledged single point of failure, accepted as-is.

**Gap A-3 (consequence of D-1):** if #873 is adjudicated as required, every AC passes today's wording while a student's `Memory`-bearing design still cannot reach the shuttle — the classic "every child green, capstone false" hole. Resolving D-1 closes this.

## 3. Dependency chains

**Real and acyclic — verified against the features' live machine blocks, not the capstone's prose.** Chain: #315→#336→{#327, #358}; #358→{#359, #328, #429}; {#353, #354, #363}→#317→#359; {#292, #317, #358}→#359; {#327, #339, #358, #359}→#328→CAP-07. No capstone number (295–313) appears anywhere in the closure; no cycle; every endpoint open.

**However, the capstone's printed DAG walk and mermaid graph are stale against the features' current blocks:**
- **#328** `blocked_by` is `[327, 339, 358, 359]` — the capstone prints `[327, 339, 359]` and the mermaid omits the now-direct F018→F044 edge (only transitively present via F023).
- **#358** `blocked_by` is `[336]` (dropped 315); `blocks` is `[359, 328, 429]` (capstone prints `[359]`).
- **#359** `blocked_by` is `[292, 317, 358]` (gained 292 — a task-tier issue, still acyclic).
- **#339** `blocks` is `[328, 429]` — capstone prints `[320, 328, 360]`; both 320 and 360 edges are gone.
- **#336** `blocks` gained 292; its `serves_capstones` no longer contains 307, nor does #339's — both contradict the capstone's DAG-walk note and marginal-cost prose.
- **#317** `blocked_by` is `[353, 354, 363]` — the capstone's upstream walk omits #363 (open, so no break, but the walk is incomplete).

None of these changes the verdict — the capstone's own Re-planning Protocol already mandates regenerating the graph from the features' machine blocks — but the graph as published no longer satisfies its stated generation rule ("an edge X→Y is drawn iff Y's `blocked_by` contains X").

**Unfunded external prerequisite:** the shuttle program itself (see A-2). Not a filing blocker; a calendar and continuity risk the issue prices in.

## 4. Staleness and open questions

- **Evidence-commit rot, the exact failure #493 tracks.** CAP-07 claims it was "re-pinned to master," but `333523a` is **not on master** — it sits 15 commits ahead of master on branch `claude/github-issue-review-agents-j99xga` (all 15 touch only `issue-reviews/`, so every code citation does verify against current master today — I re-verified all of them: `tt_um`/`tinytapeout` grep = 0, `Register.java:230-231` D/C-only inputs, 8 case-insensitive `reset` hits at exactly the cited lines, `HdlModel.Direction` two-member enum vs `YosysNetlist.java:136`/`ScannedPort.java:19` INOUT, `NetPartition` grep = 0, `counter.v:21` initial value, `HdlPolicyTest` `subCircuitIsRejectedCleanly`, `GhdlCompileTest.java:34-36` assumeTrue, `board/` = 609 lines, `Boards.java:34` ICESTICK, `docs/plan` absent). If that branch is deleted, the pin dangles exactly as `2d0ca9d` did. Worse, **three of the six required features (#327, #328, #339) still pin the dead `2d0ca9d`** — a commit that no longer exists in any clone branch and survives only as a dangling API object. #493 (open) owns the fix; it should be treated as a pre-start hygiene item for this program.
- **#59 is closed** but CAP-07's Related Issues section describes it in the present tense as an open overlapping `tier:capstone` issue that "owns the interchange outcome." Reference-only, but the prose is stale, and #873 exists precisely because #59's closure orphaned `Memory`'s export task (#291) — reinforcing D-1.
- **Cost bands:** honestly flagged as unverifiable — the sourcing corpus (`docs/plan/`) does not exist in the tree (verified). FEAT-023 is carried at its full 6–12 mw band although its residual is smaller and unmeasured; the issue says so itself. Order-of-magnitude only; not contradicted by scope, but not audited either.
- **Open questions that gate start:** none block the Demo slice. OQ7/#429's own Open Question 1 (shuttle template digest pin "still not recorded") blocks TASK-0094 past the wrapper-shape work — record the pin early. OQ5's vendored-signature design and OQ1 (handoff-only) have recommended defaults and block #328's children, not this capstone's start. OQ6 and OQ8 block acceptance bookkeeping at close, as the issue already states.

## Verdict: ready-with-gaps

Start now — with the Demo slice (1.5–2 mw) against a real submission window, exactly as § Cost recommends. Before the full required set is funded, close these in order:
1. **Adjudicate #873 vs `requires_features`** in a `REPLAN:` on #302 (D-1/A-3) — the only finding that could silently invalidate the sufficiency argument.
2. **Regenerate the DAG walk and mermaid** from the features' current machine blocks (#328+358 edge, #339/#358/#359 blocks drift, #317's #363 edge, serves-lists without 307).
3. **Fix the machine-block rosters:** promote #429/#432 into #328's `requires_tasks`; file or rule-G-record #359's two "Not filed" planned tasks; state on #339 whether zero tasks is intentional.
4. **Land #493** (or at minimum re-pin #327/#328/#339 off the dead `2d0ca9d` and put `333523a`'s content on master) before implementation cites any of these issues as evidence.
5. **Answer #358 OQ1 (uniquified vs deduplicated) on #358 and #292** before AC-5 is written as a test, and **record the shuttle template digest pin** before TASK-0094 proceeds past wrapper shape.
