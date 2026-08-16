**Capstone:** CAP-25 #506 — schematic-similarity evidence via exact canonical-equivalence bucketing
**Reviewed at:** 2026-08-16, against remote master `c5cee1b`; evidence commit `646d5ae` verified locally.

**Verdict: ready-with-gaps** — the deliberately-small first slice (#880 → #883 → #884 → #885) is fully filed, correctly chained, and startable today; the gaps are stale cross-references between the CAP body and its rewritten children, and one parent/child disagreement about exactly which PF filings the demo verdict gates. None of the gaps blocks starting #883.

## 1. Decomposition

**Filed and open, by design gate-first.** CAP-25 is a deferred capstone whose machine block requires exactly one feature: FEAT-C25-0 #880 (open, native sub-issue, in progress), the premise gate that produces KC-25-1's data. #880 carries three native TASKs, all open: #883 (corpus), #884 (Yosys canonicalization + bucket hashes), #885 (report + written KC-25-1 verdict on #506). PF-1…PF-6 are intentionally unfiled `planned_features`, gated behind the demo verdict and Open Question 1 — this is the capstone's own recorded design ("Filing PF-1…PF-6 before this measurement exists would fund 14–21 mw against an untested premise"), not a decomposition hole.

- Nothing required is closed or redirected. Every referenced issue (#300, #319, #334, #340, #356, #357, #436, #437, #491, #498, #508, #717, #872) is open.
- **No double-ownership.** #883 explicitly cedes the 300-submission corpus to #717 (CAP-06 lineage) and names growing its own corpus toward 300 as the antipattern; #884 explicitly disclaims #872's cone extractor; #880 explicitly is-not PF-1 through PF-6, with a Boundary section per PF.
- **The set does not yet compose to the capstone outcome — deliberately.** §1's 300-corpus/12-pair/6-transform-class outcome is owned entirely by unfiled PFs. What is filed composes to exactly one thing: a trustworthy KC-25-1 verdict. That is the correct shape for a gate-first capstone; the DoD's REPLAN protocol covers resolving each PF to a filed issue later.

## 2. Acceptance-criteria composition

**Child ACs compose to the gate, not the capstone — as designed.** #880's AC-1..AC-6 are fully covered by the three tasks (#883 owns AC-1 corpus/provenance/functional-equivalence; #884 owns AC-2/AC-3 canonicalization+determinism plus a per-transform-class check, AC-7, that closes the "opt_clean assumed to erase buffers" hole; #885 owns AC-4/AC-5 report+verdict, with AC-3 naming the corpus/toolchain-stop discharge so the executor cannot invent a third outcome). CAP §4 AC-1..AC-5 remain owned by unfiled PFs; no filed child passing can make the capstone falsely appear done, because the DoD requires `planned_features` empty and §4 verified end-to-end.

Composition defects found (all fixable by REPLAN comment, none blocking #883):

- **Stale gate arithmetic in #506.** KC-25-1 and the Cost section say "demo slice … 3 planted pairs" / "30-submission … 3 planted pairs"; the rewritten children specify **15 planted pairs, 5 per class across 3 transform classes** (#880 AC-1(d), #883 AC-3, #885 AC-2). The children are stricter, so the gate is sound, but #506's KC text no longer matches the instrument that answers it.
- **Stale re-scoping caveat in #506 Cost.** It states "As filed, #880/#883/#884/#885 still encode the prior calibrated-score design … re-scoping is required before KC-25-1's verdict is valid." That re-scoping has since happened: all four bodies now encode bucket membership, the skeleton is in the corpus (#880 AC-1(a), #883 Outcome/AC-5), and no score/threshold survives anywhere. #880's own Decomposition section carries the mirror-image staleness ("#883/#884/#885 bodies still describe the calibrated-score design") — also no longer true. An orchestrator reading either body verbatim would wrongly conclude re-scoping work is still owed.
- **Children cite a PF-3 that no longer exists.** #880 Boundary and #885 Boundary justify "a pass never authorizes PF-3" by calling PF-3 "the calibration and null-model work … 4–6 mw research core." #506's redirected design prices PF-3 at 1–2 mw, "no calibration, no research core." The sequencing rule itself (PF-3 waits) is harmless and worth keeping; its stated rationale is stale.
- **Gate-scope mismatch between parent and child.** #506 (KC-25-1, DoD) gates **PF-2/PF-3 funding** on the demo verdict, and gates **PF-1 filing** only on Open Question 1; #880 AC-5/#885 AC-2 additionally forbid **PF-1 filing** before the verdict comment exists. Not a contradiction — both gates can be honored — but the orchestrator must apply the union (PF-1 needs OQ1 ruling **and** the #885 verdict comment), and one body should say so.

## 3. Dependency chains

**Real and acyclic.** #506 → #880 → #883 → #884 → #885 is a clean linear chain; `blocked_by`/`blocks` edges agree on both ends (#883 blocks [884, 885]; #884 blocked_by [883], blocks [885]; #885 blocked_by [884]). No edge points at a closed issue.

- **The demo slice's decoupling from #356/#334 is verified, not asserted.** #880/#884 claim the semantic-diff canonical form doesn't exist yet: confirmed — `grep "sref\|sprobe"` over `src/ test/` returns zero hits at the current checkout, and #356 remains `blocked_by` #319 and #334, both open with unlanded task chains. Routing the measurement through `jls -export` + Yosys removes a 20+ mw unfunded prerequisite from the gate's critical path, and the KC-25-2 "second canonicalizer" objection is answered credibly (throwaway measurement substrate, never a shipping form).
- **External prerequisite is funded.** Yosys/iverilog are already CI dependencies (`.github/workflows/ci.yml` installs `iverilog ghdl yosys` on Linux with Windows/macOS equivalents), and KC-25-0-2 names the stop-don't-degrade behavior if the toolchain is absent.
- **Future-path (not current-path) prerequisites are honestly recorded:** PF-1 will depend on the #356/#334 lineage (unfunded, long) — the recorded response is sequence-not-fork (KC-25-2); transform-class-4 detection is blocked on #357 and is explicitly excluded from the demo and from AC-1 until it lands; #491 (`ElementId.parse` counter bug) is flagged as a live bug PF-1 would inherit.
- **Open Question 1 is a real, unresolved filing gate.** No comment on #506 records the AMENDMENT.md P11 ruling (the four existing comments are coverage-verification and chain-check REPLANs). Per the DoD this blocks **PF-1's filing only** — it does not touch #880/#883/#884/#885.

## 4. Staleness and gaps

- **Evidence commit resolves.** `646d5ae` exists and is a descendant of the local checkout; `docs/capability-roadmap/AMENDMENT.md` exists on remote master (the local clone is merely behind).
- **Load-bearing citations verified at the evidence commit and/or current tree:** AMENDMENT.md:478–483 contains the exact refusal quote ("any similarity score, cohort ranking or automated plagiarism flag"); lf-06-diff-merge-vcs.md:610–624 contains the ship-comparison-not-score recommendation; the `grep -rli "winnow\|weisfeiler\|plagiar\|fingerprint"` claim reproduces as 0 files in `src/ test/` excluding `collab/`; `Element.java` `fixed`/uneditable attribute is where cited; `HdlExporter.orderedElements` exists (sorts deterministically); `src/jls/hdl/yosys/` exists; `DelayGate` exists as the only pass-through element, as #883 AC-3 asserts. Task evidence commits `29afb26`/`e44b053` both resolve.
- **Cost bands are coherent where current:** #883 (0.5–1) + #884 (0.25–0.5) + #885 (0.5) ≈ #880's 1–2 mw, and the reduction from 2–3 mw is explained by the Yosys reuse. The stale numbers are the ones inherited from the superseded design (§2 above: "3 pairs," "30 submissions," "PF-3 4–6 mw").
- **Open questions:** OQ1 blocks PF-1 filing (real, unresolved, correctly recorded in the DoD); OQ3 blocks PF-5 filing. Neither blocks any filed issue. OQ2/OQ4/OQ5 ride along.

## Verdict

**ready-with-gaps.** Start #883 → #884 → #885 now; nothing filed is blocked. Before any PF is filed, land one REPLAN comment on #506 (and a matching note on #880) that: (1) updates KC-25-1/Cost to the 15-pair, skeleton-included corpus the children actually specify and retires the "re-scoping still required" caveat; (2) corrects the children's stale "PF-3 = 4–6 mw research core" rationale to the redirected 1–2 mw group-by; (3) states explicitly that PF-1 filing requires both the OQ1 maintainer ruling and #885's verdict comment. OQ1 itself remains the one decision only a maintainer can make, and it is correctly positioned as a filing gate rather than a start blocker.
