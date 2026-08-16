**CAP-7 (#302) — a design drawn in JLS comes back from an open shuttle as a physical chip**

**Verdict: ready-with-gaps** — start now (Demo slice and #336 after #315 are startable today); six named gaps below, none structural.

Reviewed against: issue tree via REST (CAP body, native sub-issues, all six required features' machine blocks, their task children, and the full upstream/downstream closure), plus code verification in the working tree. Evidence commit `333523a` confirmed reachable on the remote (2026-08-09); remote master tip is `c5cee1b`.

### 1. Decomposition

**Sound, with one mirror violation.**

- All six `requires_features` — #327, #328, #336, #339, #358, #359 — are filed and **open**, as are all their filed task children: #478, #398 (under #327), #373, #468 (under #336), #292 (under #358), #386 (under #359), #429, #432 (under #328). `planned_features` is empty as claimed. #328 is #302's sole native child, correct for the tier model since the other five are shared features.
- **Gap D1 — the corroboration claim is now false.** The machine block asserts "each of these six issues carries 302 in its own `serves_capstones`, and no other filed feature does." A body search over all open issues finds a **seventh**: **#873** (open, no native parent, filed after this body was written) declares `serves_capstones: [302, 304]` — Memory/RegisterFile/FieldExtend export-or-refusal. #328's own planned TASK-0113 already references it ("SubCircuit, Memory — see #873"). #873 needs a disposition on this capstone: required (a term-project CPU without exportable Memory cannot walk §1 step 5), beneficial, or a correction to #873's own mirror. Today the edge is one-way and rule A's "machine block agrees with reality" is violated.
- **Gap D2 — decomposition below the feature tier is incomplete on the critical path.** #359 (RESIDUAL) has `requires_tasks: []` and two of its three `planned_tasks` are explicitly "Not filed" (hierarchical emitter goldens TASK-0044; the behavioral corpus with no task id). #328's TASK-0113 is likewise "Not filed." #339 has no task decomposition at all (`requires_tasks: []`, `planned_tasks: []`, no native children) — defensible at its 2–4 mw band, but it means FEAT-021 is a single undivided unit of work.
- **Cross-ownership is coherent, not double-owned:** TASK-0052 (#416) is a native child of #264 while declaring `part_of_feature: 359`, and #328 carries it only by reference; #359's Open Question 3 is the recorded single-ownership venue, exactly as this capstone's Open Question 6 states. No closed-but-still-required issue anywhere in the tree.

### 2. Acceptance criteria

**Compose upward correctly; this is the strongest section of the issue.**

- AC-1…AC-6 + AC-8 are deliberately written as **conjunctions across features** (AC-2 = FEAT-037's model × FEAT-044's refusal; AC-3 = FEAT-021 × FEAT-044; AC-4 = FEAT-023 × FEAT-044 × FEAT-018; AC-6 keys against the flow's own artifact), so "every child passes but the capstone fails" is guarded at exactly the seams §3 names. AC-2's hierarchical-design requirement closes the reset×hierarchy vacuity hole. AC-7 correctly excluded from closure.
- Code claims underwriting the ACs all verified in the tree: `Register` has exactly `D`/`C` inputs in all four orientations (`src/jls/elem/Register.java:230-261`); `HdlModel.Direction` has two members (`src/jls/hdl/HdlModel.java:28-33`) against a three-direction read side (`YosysNetlist.java:136`, `ScannedPort.java:19`); `SubCircuit` falls through to the offender throw (`HdlExporter.java:191-196`, buckets at `:422-437`) pinned by `HdlPolicyTest.subCircuitIsRejectedCleanly()`; `counter.v:21` emits `reg [3:0] count = 4'h0;`; the ghdl oracle is `Assumptions.assumeTrue`-gated (`GhdlCompileTest.java:34-36`); one board in `Boards.java:34`.
- **Gap A1:** AC-5's wording ("module per subcircuit *type*") still inherits #358's Open Question 1 (uniquified vs deduplicated). #358's machine block now calls dedup-by-digest "the plan of record on #292," so the answer is converging toward AC-5's wording — but the completion criterion requires it *recorded* on #358/#292 before AC-5 becomes a test, and it is not yet.
- **Gap A2:** AC-8 (cross-platform byte-identity) has no owning feature — the capstone admits this ("Spans FEAT-044 and the CI configuration") and #265's slices are adjacent, not the same assertion. At close-out this AC has no child to inherit from; it is pure capstone-level work.

### 3. Dependency chains

**Real, acyclic, all edges live — but the recorded walk is stale.**

- Full re-walk performed against the features' *current* machine blocks: #328 ← {327, 339, 358, 359}; #359 ← {**292**, 317, 358}; #317 ← {353, 354, **363**}; #358 ← {315, 336}; #327 ← {336}; #292 ← {336}; #336 ← {315}; #315, #339, #353, #363 are sources. Every node **open**, no number in 295–313 anywhere in the closure, no path returns to #302. **Acyclic and clean.**
- **Gap C1 — the DAG walk printed in this body no longer matches the features.** Since 2026-08-09: #359's `blocked_by` gained **292**; #317's gained **363**; #339's `blocks` is now `[328, 429]` (was quoted `[320, 328, 360]`); #359's `blocks` is now `[328]` (was quoted `[328, 360]`); #336's `blocks` gained 292; and 307 (CAP-13, closed as duplicate) has been removed from #336/#339/#358's `serves_capstones` while this body still quotes it in the DAG note, Open Question 8, and the marginal-band paragraph. The capstone's own re-planning protocol ("a required feature's machine block gains or loses an ordering edge → re-walk before accepting") is triggered and undischarged. The re-walk above discharges it substantively — nothing broke — but the body owes a refresh.
- No unfunded external prerequisite blocks start. The shuttle program itself remains the acknowledged single point of failure (Open Question 7); Tiny Tapeout is live, and the Demo slice is exactly the cheap probe of that risk.

### 4. Staleness / gaps

- **Gap S1 — evidence-commit rot on three required features.** #327, #339, and #328 still pin `evidence_commit: 2d0ca9d` — the very branch-only commit this capstone's own machine block flags as "will be deleted and never existed on master — see #493." It is still reachable today (verified via the commits API), and #493 (citation-rot prevention) is open, but three of six required features cite a commit with a announced expiry.
- **Gap S2 — cost bands are unverifiable, as the body itself admits.** `docs/plan` does not exist on remote master (404 via contents API, confirmed). The 38.5–61 mw sum and every per-feature band trace to an out-of-tree corpus; FEAT-023's row is knowingly overpriced at its full 6–12 mw. The sourcing caveat handles this honestly — treat as order-of-magnitude, re-derive at first REPLAN.
- **Open questions that gate children but not start:** #328's Open Question 1 (shuttle template digest pin "still not recorded") blocks TASK-0094 past the wrapper-shape work; AC-5's hierarchy-form answer (Gap A1). Neither blocks the Demo slice, #336, #339, or #327's TASK-0077.

### Verdict: ready-with-gaps

Start now. The required set is filed, open, minimal, and sufficient; the dependency closure is live and acyclic; the ACs genuinely compose. Before or at first REPLAN: (1) disposition **#873** and repair the "no other feature serves 302" claim; (2) refresh the DAG-walk text to the features' current machine blocks (307 removals, #359←292, #317←363); (3) re-pin #327/#339/#328's evidence commits to master; (4) file or explicitly defer #359's two unfiled planned tasks and #328's TASK-0113; (5) record the shuttle-template digest pin (#328 OQ1) and the AC-5 hierarchy-form answer (#358 OQ1/#292) before those tests are written. Recommended entry point stands as written: the 1.5–2 mw Demo slice against a real submission window, then #336 (behind #315) as the shared-IR keystone.
