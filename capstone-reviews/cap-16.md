# CAP-16 (#311) readiness review — 2026-08-16

**Verdict: ready-with-gaps** — the startable frontier is fully filed, open and coherently scoped; four named bookkeeping/ownership gaps must be repaired before the mid- and late-stage acceptance criteria (AC-3, AC-6) can be orchestrated from the tree. All of the gaps trace to one event: the feature-deduplication pass of 2026-08-04, which post-dates this capstone's last rewrite (evidence commit `8288226`, 2026-08-02).

Tree walked: #311 → native child #323 (FEAT-025) → #451 (TASK-0054); shared required features #314, #315, #340, #342, #369 and their children (#372, #404, #408, #466, #214, #483, #446); ordering set #316, #319, #320, #321, #347; redirect targets #61, #62 (+ #290, #388, #448); related #349, #513. Code claims checked against the working master checkout (`master` head `c5cee1b`, 6 commits ahead of the evidence commit, which is its ancestor).

---

## 1. Decomposition

**Five of the six required features are open, filed, and scoped as the capstone describes**: #314 (FEAT-002, fail-loud loader), #315 (FEAT-001 residual, table totality), #323 (FEAT-025, the spine), #340 (FEAT-016, subcircuit identity), #369 (FEAT-053 residual, verdict/replay). #323's decomposition into #451 is strong — hardened XML intake (Seam A, shared with #612), residue-tracked construct model, fragment-valued realization map with a typed inert placeholder, per-file partition-agreement gate (P9), corpus with pinned size and licence provenance.

**Finding D-1 (the decisive one): required feature #342 (FEAT-022 residual) is CLOSED — `state_reason: duplicate`, redirected to #62 on 2026-08-04.** The capstone's `requires_features`, roster table (“filed #342, open (residual)”), mermaid graph, and Cost table all still present it as open. The composition edge survives from the far end — #62 (open) declares `serves_capstones: [59, 304, 310, 311]` — but two obligations did not cleanly transfer:

- **#62 retired the core-scale readability criterion** ("Readability at the scale of a full CPU is not this feature's problem"; the retired criterion is recorded in its body), and its golden corpus (#290) is Yosys-import fixtures plus three showcase circuits — synthesized-netlist shapes. **No issue anywhere now owns a readability rubric floor scored over imported hand-drawn course circuits**, which is exactly what CAP-16 took from FEAT-022 (AC-6) and exactly the divergence §3 risk 5 warned about (rubric tuned for netlists vs. course circuits). The risk's predicted failure mode has occurred at the bookkeeping level.
- The capstone's independent cross-check ("filtering all filed features for 311 in `serves_capstones` returns exactly these six") now returns a different set: open {#314, #315, #323, #340, #369, **#62**} plus closed #342.

**Finding D-2: two pieces of capstone-closing work have no filed issue.**
- The **corpus run** (AC-1's 30-file/3-repo table) is declared inside #323 as "*residual — no task id* … the registry's task space is closed at TASK-0112, so this carries no id". It is named, so it is not silently absent, but an orchestrator scheduling from issues will not find it; `requires_tasks_exception: []` does not carry it either.
- The **nearest actionable slice** (the two-week Logisim→HDL→Yosys→`NetlistImporter` bridge, which the capstone recommends as the *first funded increment* and folds the KC-16-1 measurement into) exists only as prose in #311. #451 explicitly rules it out of its own Definition of Done ("Considered and not adopted … Out of this task's Definition of Done"). Nothing in the tree can be picked up to execute it.

**Finding D-3 (minor, declared): #451 is double-booked** — native child of #323 (`requires_tasks: [451]`) and also in #314's `requires_tasks` ("TASK-0054, shared with FEAT-025"). Both features gate close on one task. Declared on both sides, so acceptable, but #314's roster note still says #451 is `blocked_by: [404]` while #451's own machine block now reads `blocked_by: []` — stale on #314's side.

No required scope is *missing* an owner other than the two items in D-1/D-2; no feature is owned twice except the resolved #342/#62 pair.

## 2. Acceptance-criteria composition

- **AC-1 (corpus coverage): composes.** #323's criterion 6 mirrors the ≥30-file/≥3-repo floor and the licence/provenance requirement verbatim. #451's P9 corpus (≥15 files, ≥4 component libraries) is a smaller staged increment, consistent with #323 closing on the larger corpus.
- **AC-2 (nothing dropped without diagnostic): composes.** #314 (diagnostic channel) + #323 §4 invariant 1 / #451 H1 (placeholder model, report-totality equality, circuit refuses to simulate with placeholders present). The falsification-transcript requirement lives only at capstone level, which is appropriate for a system-scale gate.
- **AC-3 (behavioural parity on the comparable sublattice): GAP.** The capstone asserts FEAT-053's residual "now includ[es] a vector generator and the `INCOMPARABLE` verdict this issue's step 2 now requires" — **#369's filed body contains neither**. Grep of #369 and #466 finds no `INCOMPARABLE` verdict, no vector generator, and no cross-tool (Logisim-headless) replay harness; #466's verdict surface is pass/fail/could-not-run over a user-supplied expectations file, and #369 itself says CAP-16 "needs only the replay half". #323 explicitly disclaims behavioural parity ("whether the imported circuit *behaves* identically … is CAP-16's own AC-3"). Result: **every child of the required set could close and AC-3's machinery — generator, sublattice restriction, `INCOMPARABLE` tri-state, dual-tool diff — would still be unbuilt and unowned.** (#451's I1 use of Logisim-headless is a *connectivity* oracle, not the vector-parity harness.)
- **AC-4 (name collision → fragment or refusal): composes exactly.** #323 §4 invariant 6 + interface equation (semantics-keyed map, `approximate(x, generated fragment)`) and #451 Stage 3 (`μ : Construct → {mapped, approximated, unmapped, refused}`) carry the revised policy, including the ShiftRegister case (I4). #315's totality residual makes it reachable.
- **AC-5 (XXE, one test per vector): composes.** #451 Seam A (`jls.imp.xml`), H3, P6 — one test per vector, DTD/entity/XInclude lockdown, shared with #612. Matches KC-16-4.
- **AC-6 (readability, measured): ORPHANED.** The capstone's own text says the floor and rubric "are owned by FEAT-022's residual scope" — a closed issue. See D-1: #62/#388/#290 own invariants and netlist-corpus goldens, not a course-circuit floor. AC-6 is currently a placeholder pointing at an issue that will never land.

## 3. Dependency chains

- **Acyclicity: holds** on every edge walked. Mirrors verified both ways: #314 `blocks:[323]` ↔ #323 `blocked_by:[314]`; #319 `blocks:[…340…]` ↔ #340 `blocked_by:[319]`; #321 `blocks:[369]` and #347 `blocks:[…369]` ↔ #369 `blocked_by:[316,321,347]`. No path returns to #311; no feature names a capstone in `blocked_by`.
- **Finding C-1: the capstone's `blocked_by` contains a closed issue.** #320 closed 2026-08-04 as duplicate of #61 (open, `blocked_by: []`). Under the capstone's own projection rule ("re-derive this capstone's `blocked_by` from the six required features' machine blocks"), the set re-derives to **{#316, #319, #321, #347}** plus **#448** (open, TASK-0047 under #61) if #62 is adopted as the sixth row, since #62 is `blocked_by: [448]`.
- **Finding C-2: one half-mirror.** #369 declares `blocked_by: [316]`, but #316's machine block reads `blocks: []` — the mirror is missing on #316's side (the exact half-edge defect the corpus's own rules prohibit).
- **Stale-but-relaxed edges (good direction, text-only):** #314 is no longer `blocked_by: [315]` (now `requires_at_closeout: [372]`), so the capstone's "F001 → F002" arrow and "#314 gated by #315" roster note are stale; #319 is now `blocked_by: []` (was [334]), shortening #340's inward cone to just {#319}. Both make the plan *more* startable than the capstone states.
- **Resolved as requested:** the #349→#323 spurious edge the capstone flagged is gone on both ends — #323 `blocked_by: [314]` only, #349 `blocks: [365, 366]` and `serves_capstones: [297, 298]` with #311 explicitly demoted to related. The corresponding completion-criteria checkbox is satisfiable today; the body's "still gated by #349 in practice" warning is obsolete.
- **No unfunded external prerequisite on the critical path.** The geometry default (fitted δ-table, adopted by #451 as H6/I5) carries no upstream-licence dependency; Open Question 2 gates only the never-started fallback (KC-16-5 enforces that). Corpus licensing is scoped work, not an external gate. Logisim-Evolution-headless as oracle is an external *tool* dependency, but a run-time one on public software, not a funding gate.

## 4. Staleness and open questions

- **Evidence commit `8288226` resolves** and is an ancestor of current master (master 6 ahead). Every load-bearing code citation re-verified on the current tree: `grep -rli logisim src/` → 0; XML-parse grep → 0; `NetlistImporter.java:104` layouter call; `HdlExporter.java:78-79` ShiftRegister barrel-shift note; `docs/file-format.md:220` "Unknown attribute names are silently ignored"; `docs/batch-interface.md:298-303` "JLS has no per-bit HiZ"; `JLSStart.java:787-788` `-savetext`; `Element.java:23-24` stableId; all six totality tests present; `src/jls/hdl/layout/` (8 files) + 5 test classes present. **No evidence rot.**
- **Everything stale traces to the 2026-08-04 dedup pass** (#342→#62, #320→#61, and the quote drift below). The capstone was written 2026-08-02 and has not been reconciled since.
- **Quote drift in §2's cross-check:** #340 now declares `serves_capstones: [304, 311]` (the capstone quotes `[300, 304, 311]`; #300 was dropped by CAP-06's own rewrite); #315 now declares `[296, 297, 298, 309, 310, 311]` (capstone quotes 307, closed 2026-08-03 as duplicate). Membership of 311 is intact in both, so the composition claim survives; the quoted lists do not.
- **Cost bands:** the FEAT-022 row (4–8 mw) now prices a closed issue; the residual's real cost lives in #62/#388/#290 and has not been re-derived — KC-16-1's re-costing discipline should absorb this. #323 faithfully mirrors the 12–18 mw higher-band standing instruction. The marginal-band claim (#323 is the only feature serving 311 alone) still holds among *open* features, with the caveat that #62 now also serves 311.
- **Open questions:** none block start. OQ1/OQ3/OQ4/OQ6 have recommended defaults that #451 has already operationalized (δ-table, no coordinate preservation in increment 1, one-way migration, report-as-artifact). OQ2 gates only the fallback. OQ5 is answered (#513 filed, non-blocking).

---

## Verdict: ready-with-gaps

**Start now.** The startable frontier is clean: #315 and #314 are both `blocked_by: []` today, #451/#323 opens the moment #314 lands (and #451's corpus assembly can start immediately in parallel). The spine (#323/#451) is the best-specified feature/task pair in the walked set.

**Gaps to repair before AC-3/AC-6 phases are orchestrated (named fixes, all issue-edit-sized):**

1. **Re-home FEAT-022's obligation** — replace #342 with #62 in `requires_features` (or file a course-circuit readability-floor residual declaring `serves_capstones: [311]`), and explicitly assign the course-circuit rubric corpus that #62 does not carry. Until then AC-6 has no owner and #311's roster points at a closed issue.
2. **Re-derive `blocked_by`** per the capstone's own projection rule: drop closed #320; result {#316, #319, #321, #347} (+ #448 if #62 is adopted).
3. **File the AC-3 machinery** — vector generator, sublattice restriction, `INCOMPARABLE` verdict, dual-tool (Logisim-headless vs. JLS batch) diff harness — as a task under #369 or #323, or record it in `requires_tasks_exception`. It is currently asserted as FEAT-053 residual scope that FEAT-053 does not contain.
4. **File the bridge/corpus-measurement slice** (the recommended first funded increment and KC-16-1's measurement vehicle) as an issue, since #451 excludes it and nothing else owns it.
5. Minor mirrors: add `blocks: [369]` to #316; refresh #314's stale note on #451's `blocked_by`; refresh §2's quoted `serves_capstones` lists for #315/#340.

Not **not-ready**, because the gaps are mechanical re-pointings after a dedup pass the capstone predates, none touches the spine's scope, and the capstone's own Re-planning Protocol already prescribes exactly these repairs. Not **blocked**, because the only external question (upstream licence) gates a fallback path that the adopted default never takes.
