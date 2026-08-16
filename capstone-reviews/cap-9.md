# CAP-9 (#306) readiness review

**Verdict: not-ready** — the capstone body is in excellent shape (narrowed scope, fresh evidence, honest disclosures), but its sole required feature's task roster no longer covers the capstone's core outcome. The refutation channel that *is* this capstone's narrowed §1 exists only as unfiled "phase 3" roadmap prose inside #483, and the two reference corpora its kill criteria are measured over are named by #369's roster but owned by no task's actual contract. Every filed child could close green and `jls -b -refute` would still not exist.

Reviewed 2026-08-16 against the live tracker and master (`6744b56`); CAP-9's evidence commit `c5cee1b` exists in the repo.

---

## 1. Decomposition

**Feature tier: clean.** The narrowed machine block declares exactly one required row — FEAT-053 (#369), filed, open — with `requires_capstones: []`, `requires_tasks_exception: []`, `planned_features: []`. #369 reciprocates via `serves_capstones: [300, 306, 311]`. The seven demoted rows (#317, #322, #335, #347, #353, #354, #359) are all still open and correctly carried in `related` only. No row is closed-but-required, and no scope is double-owned at feature tier: #300 (CAP-06) shares FEAT-053 but the two capstones' payloads are cleanly split (vector-differential grading vs. refutation of a single unfamiliar design).

**Task tier: broken — the core scope is unfiled.** #369's own body splits into a verdict half (#466, #214, #757 — all filed, open) and a formal half (#483). CAP-9's payload is, per #369's Intended Audience section, "realized by the formal half — #483." But #483's own Proposed disposition **narrowed itself to phases 1–2 only**: timestamp-closure sampling and coverage instrumentation. Its §13 explicitly labels the rest as roadmap "to be filed as its own issues by a maintainer":

- **Phase 3 — exhaustive equivalence** (`-equiv ref.jls`, width gate, `-cex` writing the disagreeing input as a `-t` vector, "unknown/not checkable never a pass", counterexample confirmed by replay) — **this is CAP-9's entire narrowed outcome**, near-verbatim: §1's `-refute` walk-through, AC-2's replay rule, the NOT-REFUTED contract. #483's mermaid marks it "P3 (unfiled)"; its DoD §14 says "None of the items below concern equivalence."
- Phases 4–5 and the GUI failing-cone highlight (CAP-9 §1 step 3, "open it in the GUI and observe the failing cone highlighted") are likewise unfiled; #483 §13 even flags the GUI overlay: "Not filed anywhere yet … Recommended: file it as its own issue."

Consequence: `grep -rn '"-refute"\|"-equiv"\|"-formal"' src/` → 0 (verified), and **no filed task anywhere in the tree owns making it non-zero.** CAP-9's machine block asserts "the narrowed required set is entirely filed" — true at feature tier, false in effect one level down.

**The corpora are unowned.** CAP-9 keeps the two reference corpora (`test/fixtures/corpus/lab/`, `test/fixtures/corpus/submissions/`) in required scope as "FEAT-053's committed deliverable." #369's §2 roster table assigns them to #483 — but #369 itself admits in the same section: "#483's own task contract still needs the matching update." Verified: #483's body never mentions a corpus, a manifest, or `test/fixtures/corpus/`; its narrowed DoD cannot produce them. `test/fixtures/` on master holds four entries, none a corpus. KC-09-3, KC-09-4, and AC-9 are all measured over artifacts no task is contracted to build.

**#872 (cone extractor): filed and well-aligned, but CAP-9's references to it are stale.** #872 exists, is open, and its content matches CAP-9's decisions exactly (SubCircuit flattened per CAP-9's Open Question 4, three distinguishable refusal kinds, headless, never evaluates, "the bound lives with the consumer … Putting the bound here would break CAP-09's consumer"). However CAP-9's machine block and Open Question 7 say it is "homed under #563/FEAT-C31-1" with `shared_with: [306]`; #872's actual machine block has been **re-homed to FEAT-004 (#336)** with `consumers: [563, 306]` and a hard `blocked_by: [468]`. Open Question 7's "Decided here: leave it under #563" has been overtaken by events.

## 2. Acceptance-criteria composition

**The upward composition fails: every child can pass while the capstone fails.** Walk the filed children's DoDs against CAP-9's:

| CAP-9 needs | Filed owner | Gap |
|---|---|---|
| AC-1 `FormalResultTotalityTest` (two-constructor result type, uncheckable corpus refused) | none — attributed to "FEAT-053 #369" but absent from #466's, #483's, and #214's contracts | unowned |
| AC-2 `CounterexampleReplayTest` | none (the replay *principle* survives only as a "binding design constraint" on #483's unfiled phase 3) | unowned |
| AC-3 `MiterPortMatchTest` | none | unowned |
| AC-9 adversarial fixture in submission corpus → `COUNTEREXAMPLE` | none (corpus itself unowned, see §1) | unowned |
| Exit statuses for wrong/unknown/not-checkable | #466 owns exit status **3** for vector-expectation FAIL; statuses 4/5 are *reserved* in #300's design but "never emitted by this capstone's mechanism" — no task emits them | partially owned |
| Uncheckability gate (Memory, two-driver net, comb. loop, incomplete TruthTable, wide multiplier → structural refusal) | #872 owns the structural refusals for the first three cone-shaped cases; the incomplete-TruthTable refusal and the budget-vs-structural reason distinction on the verdict side are unowned | partial |

What #466 delivers (PASS/FAIL/UNRUN per vector against a user-authored expectations file) and what #483 now delivers (settled-value sampling, `-cov` coverage bins) are both genuinely useful substrate for CAP-9 — but neither, nor their union with #214/#757/#872, produces a single line of §1's walk-through. The child ACs compose upward to CAP-06's outcome far better than to CAP-9's.

One positive: CAP-9's completion criterion "Open Question 2 resolved jointly with #300" is closer to done than CAP-9's body records — #300 §3 has already designed the lattice once (3 = counterexample, 4 = unknown, 5 = not checkable, "neither 4 nor 5 is ever a pass"), which maps directly onto COUNTEREXAMPLE / NOT-REFUTED(budget) / NOT-REFUTED(structural). The joint decision needs to be *recorded on both issues*, but the design work is substantially pre-baked.

## 3. Dependency chains

**No dead edges, no cycles.** Every issue referenced by the machine block, the roster, and the transitive chain is open and unredirected: #369, #466, #483, #214, #757, #872, #563, #641, #336, #468, #316, #321, #347, #300, plus all seven demoted rows. The composition/ordering graph as declared is acyclic. No unfunded *external* (outside-repo) prerequisite exists — the exhaustion-first design deliberately avoids the SAT/solver and external-toolchain dependencies.

**But the critical path contradicts the narrowing, two ways:**

1. **The demoted programme is still in the transitive closure of the DoD.** CAP-9's first completion criterion is "FEAT-053 (#369) closed as landed." #369 §6: "This feature (#369) closes only when both tracks close," and its formal track is gated on `blocked_by: [316, 321, 347]` — the SimpleEditor decomposition (#316, itself sized 12–20 mw by #369's scope note), the Yosys JSON writer (#321), and the parity harness (#347). #347 is precisely a row CAP-9 demoted with a minimality argument saying "no new comparison harness is needed," and CAP-9's own design (in-tree exhaustive simulation, flattening extractor, no external tool) needs none of the three. The capstone discloses this ("Feasibility note … that is #369's edge to walk"), but disclosure does not defuse it: as filed, CAP-9 cannot close until work it argued is unnecessary lands. Note also an internal inconsistency in the chain itself: #369 §2 says #483 is `blocked_by: [316, 321, 347] (inherited)` while #483's own machine block says `blocked_by: [466]` only — after #483's self-narrowing to coverage, the three feature-gates plausibly no longer apply, and one of the two blocks is wrong. The clean fix is the split #369's own disposition already proposes (FEAT-053a verdict / FEAT-053b formal) or re-pointing CAP-9's required row at the narrower unit that actually carries its scope — either is a decomposition change that must precede orchestration.
2. **An undisclosed hard prerequisite: #468.** CAP-9's feasibility note names #316/#321/#347 but not the extractor's own chain: #872 is `blocked_by: [468]` (TASK-0007, NetPartition, FEAT-004 #336) as a hard dependency with "no private-traversal fallback and no WAIVED: path." So the true start-of-work chain for CAP-9's checking core is #468 → #872 → (unfiled refutation task), in parallel with #466 → (unfiled corpora/report work). #468 is open and in-repo, so this is a disclosure gap, not a blocker.

## 4. Staleness and gaps

**Evidence: fresh and accurate — the best part of this capstone.** Every load-bearing code claim re-verified against master today:

- `docs/batch-interface.md:36-40` — exactly three exit statuses, none meaning "wrong" ✓
- `src/jls/elem/ElementRegistry.java` — exactly 35 `new ElementType(...)` entries ✓
- `src/jls/elem/TruthTable.java` — silent return on `matchingRow < 0`; `if (outValue == 2) outValue = 0` don't-care destruction ✓ (line numbers match)
- `examples/autograde/autograde.py:45,53` — `EXPECTED_FINALS` / `EXPECTED_STDOUT_LINES` at the cited lines ✓
- `test/fixtures/` — four entries, no corpus ✓
- `grep -rn '"-equiv"|"-formal"' src/` → 0 ✓; `grep -i expect src/jls/elem/TestGen.java` → 0 ✓
- No `REJECTED` map in `src/jls/hdl/HdlExporter.java` ✓ (the body's own correction of its earlier claim is right)
- Nothing from #483 has landed (no `afterTimestamp`, no `CoverageCollector` in `src/jls/sim/`) ✓
- Evidence commit `c5cee1b` resolves ✓; the body's honest admission that the three cited cost docs do not exist also still holds.

**Stale items:**

- **#872 homing** (machine block, Feasibility note, Open Question 7): says `part_of_feature: 563`; actually re-homed to #336 with `consumers: [563, 306]` and `blocked_by: [468]`. OQ7's decision "(a) leave it under #563" is moot.
- **Cost band contradicted by scope as bound.** The narrowed ~10–16 mw estimate is coherent for the narrowed *work*, but the DoD binds to #369's full close, whose formal track drags #316 (12–20 mw) + #321 + #347 — the body's own "Deferred … ~55–92 mw" territory. Until the required-row binding is loosened (split #369, or bind to the specific tasks), the printed budget does not describe the printed completion criteria.
- **Open questions blocking start:** none of the nine as written (OQ1/4/7 resolved, OQ2 pre-baked on #300's side). The actual start-blocker is not listed as an open question at all: *there is no issue to hand an executor for the refutation channel or the corpora.* An orchestrator starting today could dispatch #466, #468→#872, and #483 phases 1–2 — all real substrate — and would then stall with the capstone's decisive scope unassigned.

## Verdict: **not-ready** (decomposition work needed first)

The capstone body itself needs almost nothing — it is well-evidenced, honestly disclosed, and its refutation-first design is sound. What is missing is beneath it:

1. **File the refutation-channel task** (#483's phase 3, essentially verbatim: `-refute`/`-equiv`, exhaustion engine over #872's cones, width gate, `-cex` as a `-t` vector, COUNTEREXAMPLE/NOT_REFUTED with budget-vs-structural reasons, replay-confirmation, exit statuses 3/4/5 per #300's reserved table), owning CAP-9's AC-1/AC-2/AC-3, homed under FEAT-053's formal half.
2. **Give the two corpora a contracted owner** — either amend #483's task contract (as #369 already says is needed) or file the corpus task using CAP-9 §1's definitions verbatim, including AC-9's adversarial fixture and the KC-09-4 independence guard.
3. **Resolve the #369-close binding** — execute #369's own proposed FEAT-053a/b split, or re-derive CAP-9's DoD line 1 so its close does not transit #316/#321/#347; reconcile #369's "(inherited)" blocked_by claim for #483 with #483's actual `blocked_by: [466]` in the same pass.
4. **Refresh #872 references** (homed under #336, `blocked_by: [468]`) and add #468 to the feasibility note's disclosed chain.

Items 1–2 are the gate; 3–4 are an hour of bookkeeping that should ride the same edit. With 1–3 done, this capstone would be ready with an honest ~10–16 mw floor and no undisclosed prerequisites.
