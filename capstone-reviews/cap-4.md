**CAP-4 / #297 — a drawn CPU becomes a buildable 74-series breadboard with physical truth**

**Verdict: ready-with-gaps** — the six-feature spine is filed, open, acyclic, and five of the six are start-ready today with correctly mirrored edges; but the #341↔#322 dependency story is internally contradictory and must be resolved by `REPLAN:` before any AC-2 / steps-4-5 work is scheduled, the two capstone AC tests are assigned to features whose own bodies never mention them, and ~4-8 mw of capstone-owned scope (CLI, SAP-1 fixture, dual-run report) is filed nowhere.

All checks below were run against the live issue corpus and against master (`c5cee1b`, verified identical to the remote default branch head) on 2026-08-16.

---

### 1. Decomposition

**Filed and open — verified.** All six `requires_features` entries are open: #315 (FEAT-001 residual), #336 (FEAT-004), #337 (FEAT-015 residual), #341 (FEAT-027), #349 (FEAT-040), #365 (FEAT-041). Every one of their `requires_tasks` children is filed and open (#372/#375; #468/#373; #382; #387; #400/#450; #394/#427/#430). #297 itself has no native sub-issues — its entire composition rides the machine block, which is consistent with all six features being shared (each `serves_capstones` names at least #297 and #298). Nothing in the required set is closed or redirected.

**Gap D1 — capstone-owned scope is unfiled.** The Cost section's second table (`-breadboard` CLI flag 1-2 mw, `examples/sap1.jls` fixture 1-2 mw, dual-run undetermined-node report 2-4 mw) is required scope — §1 steps 1, 4 and 5 are unobservable without it — yet no issue owns it: `requires_tasks_exception: []` and `planned_features: []`, and the machine-block comment "every required scope is filed" is contradicted by the CAP's own Cost table. An orchestrator implementing from issues has no ticket to pick up for 4-8 mw of the critical path. Remedy: file these as tasks (the `requires_tasks_exception` mechanism exists and is empty) or explicitly record that the capstone issue itself is the work item.

**Gap D2 — TASK-0093 is unfiled and triple-ambiguous.** #365 carries "breadboard consistency check and physical binding" in `planned_tasks` (unfiled) and counts it at 2 mw in its own cost reconciliation; #341's body says TASK-0093 is *owned by #329* (the canvas — which this capstone defers); #365's body says it is *shared* with #329 and #341. Its content (placement-derived partition compared against the schematic partition as the same type) is the closest thing in the corpus to AC-1 check 2's mechanism. With #329 deferred out of this capstone, the check's implementing task has no filed home and a disputed owner.

**Gap D3 — `docs/realization-layer.md` has no producer in the required set.** The CAP's §3, Related Issues, and a Completion Criteria checkbox all cite the shared FEAT-040/FEAT-041 contract as living in `docs/realization-layer.md`; the file does not exist on master, #349's body never mentions it (verified by search), and the only issue that does is #298 (CAP-05) — an unfunded sibling this capstone explicitly does not depend on. As written, a CAP-04 completion checkbox gates on a document only a non-required capstone would author.

Minor: #337's `requires_tasks` is `[382]` only; its second native child #412 (construction verbs) is not required — acceptable for this capstone, whose step 1 needs only headless *read* access, but worth knowing the residual's verb half is not on this path.

### 2. Acceptance-criteria composition

**Gap A1 — both cross-owned AC tests are unmirrored.** AC-1 names `BreadboardPlanGoldenTest` "(new; #365 builds it)" and AC-2 names `UndeterminedNodePhysicalTruthTest` "(new; owned by #341)". Neither string — nor any dual-run/undetermined concept at all — appears in #365's or #341's bodies (verified by search of both). #341's own acceptance criteria are the strength lattice, pull elements, wired-AND sweeps and X-on-equal-strength contention; #365's are packing determinism, cascade, BOM and the two datasheet checks. Every child can therefore close green while AC-1 and AC-2 were never built. The classic compose-upward failure, and it is present.

**Gap A2 — AC-2's ownership is double-booked inside the CAP itself.** AC-2 says the test is owned by #341; the Cost section prices the dual-run report as *capstone-owned* scope "because no required feature carries it" (2-4 mw). Both cannot be true. This must be settled in the same REPLAN as Gap C1 below, since the answer decides whether #341 needs any body edit at all.

**Composes otherwise.** AC-1's seven checks trace cleanly to #365 (1, 7), #336 (2), #349 (3), #341 (4-6); AC-3 is genuinely pinned by #315's residual (#375's totality base is exactly the mechanism); AC-4 is correctly capstone-level and correctly separated from step 3's internal-consistency Observe clauses. The three-way falsification guard in AC-2 is well-constructed — if the ownership gaps are fixed, the criteria do cover the outcome.

### 3. Dependency chains

**Sound within the six.** Edges verified against each feature's own machine block: #336 `blocked_by:[315]` (mirrored in #315 `blocks:[...,336]`), #365 `blocked_by:[336,349]`, #315/#337/#349 unblocked. Acyclic; the CAP's mermaid graph matches the filed blocks exactly. #315, #337, #349 are startable today; #336 after #315; #365 after #336+#349.

**Gap C1 (the decisive one) — #341's dependency story contradicts the CAP's narrowing, three ways.**
- CAP-04 dropped #322 (FEAT-026, four-state value core) from the required set, asserting the dual-run diff needs only driver/net-kind distinctions that #341 "already scopes independently."
- #341's own filed body says the opposite: `blocked_by: [322]`, "**Consumes.** The four-state per-bit value #322 defines — strength is meaningless over a two-state value," and its graph draws `#322 → #341 → #297`. Since the CAP's Definition of Done requires every `requires_features` entry "closed as landed," #322 — and transitively #879 (TASK-C232-2, open), which #322 is `blocked_by` — re-enter the critical path that the disposition and the 26-45 mw total claim to have severed.
- The task layer tells a *third* story: #341's only child #387 declares `blocked_by: [391]` alone, and #391 (the resolution fold, a #322 task) declares `blocked_by: []` — the shallow path the CAP's narrative wants, but recorded nowhere at feature level. `jls.core.Strength`/`Resolution` do not exist on master, so none of this has landed.

The fix is a defined REPLAN, not new design: either narrow #341 for this capstone (a residual whose `blocked_by` is `[391]`, matching what #387 already records) or re-admit #322 with its cost. Until one is chosen, steps 4-5 / AC-2 have no honest schedule. This does not block starting the other five features.

**Gap C2 — #387 is not executable as filed.** #341's "Driver model" section (8-level Verilog lattice, strength *pair* on `Output`, no global FORMAT bump) explicitly supersedes the model #387 currently implements against (a 5-level private lattice per #387's own text, four-vocabulary model per #341's account), and #341 says #387 "needs to be updated to this contract **before execution continues**," tracked as a pending edit that has not been applied. The sole child of the AC-2-critical feature carries a superseded spec.

No edge in the required set points at a closed issue; no cycle exists; the only external prerequisite is the #322/#879 chain described above, which is exactly the thing the REPLAN must adjudicate.

### 4. Staleness and remaining gaps

**The CAP's own evidence is current and honest.** `evidence_commit: c5cee1b` is the live default-branch head (compare: identical). Every code anchor re-verified on master: `LogicElement.java:473/:480` (undriven→0, verbatim), `Put.java:16-17` (sealed, permits Input, Output), `CircuitOp.java:34-37/:51` (sealed interface; `apply(Circuit, Graphics)` still takes the graphics context), `ElementRegistryTest.java:45`, `HdlPolicyTest.java:88`, `Adder.java:261` (`propDelay = bits * defaultPropDelay`), 35 registry entries counted, the four `src/jls/hdl/board/*` files at exactly the cited line counts, `grep footprint|refdes|pinout src/` → 0, `examples/` containing only `autograde/autograde.py`, `docs/plan` missing, `timeout-minutes` absent from all workflows, and `WireNet.propagate`'s HiZ-null first-active-driver block at :443-485 exactly as the dual-run design assumes. The CAP also correctly self-flags its cost table and the KC-04-2 60 s figure as provisional.

**S1 — stale mirror claim.** §3/OQ5 state #349's and #365's `serves_capstones` "still name 297, 298 **and 307**"; both actually read `[297, 298]`. Cosmetic, but it is precisely the kind of mirror assertion the Completion Criteria promise to keep true.

**S2 — cost rows contradicted by the features' own current fields.** #336: CAP says 2-3 mw; #336's own body says 3-5 and explicitly "a 2-3 mw band undercounts." #341: CAP says 6-9; #341's own body withdraws that band ("no longer describes this issue's scope in either direction") and prices its exclusive scope at ~2 mw, with the fold funded through #322 — i.e., the CAP's headline 26-45 mw simultaneously overstates #341's row and omits the fold cost that AC-2's substrate needs. #365: CAP says 5-8; #365's own reconciliation is a 7.5 mw task-sum (5.5 unshared) with the external band withdrawn. The CAP's instruction to re-derive before sprint planning is the right protocol; these three rows are where it will bite.

**S3 — feature-side evidence commits not on the default branch.** #336 pins `333523a` (ahead of master, unmerged); #337 and #349 pin `2d0ca9d` (diverged, unreachable from master) — the identical defect #365 already documented and withdrew for itself. Feature-issue hygiene, not a capstone blocker, but the CAP's close-out checkbox "every cited evidence document and permalink resolves on the default branch" currently fails for half the required set.

**Open questions:** none blocks start. Q1 (74LS default) blocks only #349's consumers at integration time; Q2 is "decided" in the CAP but the decision is not mirrored into #341 (subsumed by Gap C1); Q4's recommended default (keep original ids) is already the filed state.

---

### Verdict: ready-with-gaps

Start now on the unaffected spine — #315 and #337 and #349 immediately, #336 behind #315, #365 behind #336+#349; that is five of six features and AC-1/AC-3/AC-4 end to end. Before any steps-4-5 / AC-2 work is scheduled, one `REPLAN:` comment on #297 must: (1) resolve #341's granularity — residual-#341 with `blocked_by:[391]`, or re-admit #322+#879 with cost (Gap C1) — and settle AC-2 test ownership (Gap A2); (2) file or explicitly capstone-own the CLI/fixture/dual-run scope (Gap D1) and give TASK-0093 a filed home (Gap D2); (3) name a producer for `docs/realization-layer.md` or drop the citation to a planned-document form (Gap D3). #387's reconciliation to #341's driver model (Gap C2) must land before #387 is picked up. S1-S3 are paper fixes the existing Completion Criteria already force at close.
