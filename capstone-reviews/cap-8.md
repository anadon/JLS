# CAP-8 (#304) readiness review

**Capstone:** CAP-08 — a published RV32 core JLS did not write imports, opens as a readable hierarchy, and a student can watch its own firmware run inside it.

**Verdict: ready-with-gaps** — the startable slice is real and unblocked, but the machine block carries two edges to closed-as-duplicate issues, one loudly-asserted ordering claim is no longer true on either side, and the headline "watch its firmware run" criterion has no filed feature behind it (declared in-body).

---

## 1. Decomposition

The machine block declares `requires_features: [314, 320, 321, 337, 340, 342, 357, 358]`, `requires_capstones: []`, `requires_tasks_exception: []`. Native children of #304: only #321 (the other seven are shared features owned elsewhere, consistent with the corpus convention).

**Six of eight required features are open with open TASKs:**

| Feature | State | Children (all open) |
|---|---|---|
| FEAT-002 #314 fail-loud loader | open | #404, #408 |
| FEAT-019 #321 Yosys JSON write | open (native child) | #414, #418 |
| FEAT-015 #337 headless CircuitOp (residual) | open | #382, #412 |
| FEAT-016 #340 VLNV / type identity | open | #417, #446 |
| FEAT-017 #357 shared parameterized definitions | open | #447, #473 |
| FEAT-018 #358 hierarchical HDL IR | open | #292 |

**Two of eight are closed — and the body does not know it.**

- **FEAT-020 #320 (the spine)** was closed **`duplicate` of #61** on 2026-08-04 ("Closing as a duplicate of #61 — feature deduplication, pass 2"). #61 is open and carries the identical scope as native sub-issues: **#448** (TASK-0047, mapper parity + bit-level mesh) and **#449** (TASK-0048, hierarchy import) — both open. #61's own rewritten body confirms it is the canonical owner.
- **FEAT-022 #342 (layout residual)** was closed **`duplicate` of #62** on the same pass. #62 is open and carries the scope as **#290** (rubric/corpus) and **#388** (TASK-0050, per-cell invariants + hierarchy-instance placement) — both open.

The #304 body was rewritten (retitled 2026-08-09) *after* those closes, yet still lists both as "**required** — filed" and answers its own Open Question 5 ("narrow #62 in place") as if the dedup had not already resolved it the other way. The three-counts invariant the body brags about (machine block = table = mermaid, all 8) is internally consistent but consistent with a roster that no longer exists. **No scope was lost** — every step of the Outcome Statement still has an open owner once 320→#61 and 342→#62 are repointed — but the repoint must be recorded via the body's own `REPLAN:` protocol before orchestration keys off `requires_features`. (Sibling note: per #61's body, CAP-15 #310's `requires_features` carries the same stale #320 citation.)

**Unfiled scope, declared:** step 4 / AC-3 (VCD capture-and-replay) is carried in `planned_features` with an explicit "No feature issue exists for this yet." Verified by search: nothing filed covers loading an external simulator's VCD back into an imported schematic (#704 is adjacent and states "not currently funded by any capstone"; #63 — open — is the black-box/co-sim stage the body itself says must be checked before filing, as it may be the "named demand"). **AC-0** (the core pin) is capstone-direct work owned by no child; the body makes it deliverable-first, which is coherent, but note it is unowned by any FEAT/TASK.

## 2. Acceptance criteria composition

- **AC-0** (pin exists) — fails today as claimed: `test/fixtures/core-pin.properties` absent at the evidence commit. Owner: capstone-direct. The licensing rider (derivative `.jls`/round-tripped netlist/VCD obligations from a copyleft core) is correctly forced into the pin decision rather than deferred.
- **AC-1** (zero problems on realized set, externally-computed manifest) — composes onto #61/#448. The *independent manifest script* is not explicitly in #448's scope; it is carried by this capstone's own completion checklist, so it cannot silently fall between issues, but the orchestrator should treat it as CAP-level work.
- **AC-2** (named hierarchy as instances) — composes onto #449 (import) + #358 (IR) + #340/#357 (identity/dedup) + #388 (instance placement). No overlap conflict: #449 uniquified-first vs #340/#357 dedup matches Open Question 3's default.
- **AC-3** (firmware runs visibly) — **no filed backing** (see above). Every child could close and this criterion would still be unstartable. The body says so itself; the gap is honest but real, and it is the capstone's title.
- **AC-4** (lossless round trip) — composes onto #321/#414 (writer) + #358/#292 (export half). Sound.
- **AC-5** (unrealizable construct named, never mis-mapped) — already held by shipped code (`NetlistImporter` default arm; pinned by `NetlistImporterTest.java:227`, `unrealizedButValidCellIsRejectedNotMismapped()`, present at the evidence commit) and satisfiable on the required set alone. Sound.

Composition upward is complete except through AC-3; there is no criterion where all children pass and the capstone silently fails — the two ways the capstone can fail (no pin, no replay feature) are both named in the body.

## 3. Dependency chains

- **Broken edges as filed:** two of the eight composition edges (`#320 → CAP08`, `#342 → CAP08`) point at closed/redirected issues. After repointing to #61/#62 the graph is real and acyclic: #61 declares `blocked_by: []`; #62 declares `blocked_by: [448]` (a task inside #61, mirrored from #62's side); #340→#357 is mirrored. No cycle returns to #304.
- **The #339 claim is stale on both sides.** The body asserts, emphatically, that #339 (FEAT-021, bidirectional ports) "must land before #320 regardless of anything else in this issue," and the completion checklist requires #339 closed before CAP-08 closes. That mirrored edge no longer exists anywhere: #339's rewritten machine block declares `blocks: [328, 429]` only, and canonical #61 declares `blocked_by: []` with bidirectional ports explicitly out of scope and non-gating. The spine is *less* blocked than the body claims. (Substantively fine for the outcome: a PicoRV32-class core has no top-level `inout`.)
- **Prerequisite closure needs re-derivation.** The recorded closure (#315, #318, #319, #334, #336, #339 — all currently open, so no dead references) was walked from the old required set; walked from {#61, #62, #314, #321, #337, #340, #357, #358} it differs (at minimum #339 drops out). The completion checklist inherits this staleness.
- **External prerequisites:** the external HDL simulator for AC-3 is runtime tooling of the class the tree already shells out to, not an unfunded engineering dependency; the unfunded piece is the internal replay feature, covered above. Nothing else external sits on the critical path. The demo slice (bit-level mesh + `$add`/`$dff` over a small module) is genuinely startable now: it is #448's scope, and #448 is open and unblocked.

## 4. Staleness and gaps

- **Evidence commit resolves and claims verify.** `evidence_commit: c5cee1b` is a real default-branch head (the review clone's local master is behind it; c5cee1b is *newer*, not dangling). Verified at c5cee1b: `core-pin.properties` absent; `NetlistImporter` realizes exactly `$not/$and/$or/$xor/$mux` + constants with a fail-loud `default:` arm and refuses non-`$` cells; `CellValidator` accepts exactly 19 types; `HdlExporter`'s `EXPORTED` set omits `SubCircuit` with the reject javadoc; the pinning test sits at `NetlistImporterTest.java:227`; `src/jls/collab/op/` is 21 files; `src/jls/hdl/layout/` exists (8 files, 1,794 lines). Every load-bearing "fails today" claim is true.
- **The one stale region is issue-graph state, not code state:** the #320/#342 duplicate closes and the #339 narrowing all predate or accompany the corpus rewrite, and the body missed them (§1, §3 above).
- **Cost:** no per-feature bands asserted; the body explicitly declines to cite the absent `docs/plan/REGISTRY.md` rather than restating phantom numbers. No contradiction to flag; the demo slice is correctly identified as the cheapest honest evidence.
- **Open questions:** OQ1 (name the core + settle derivative licensing) blocks AC-0/AC-1 and #320-successor mapper work against the pinned core, but not the demo slice. OQ2's co-decision with CAP-15 #310 is pre-answered in-body with #310's own KC-15-1 reading — low risk. None blocks start.

## Verdict: ready-with-gaps

Start now with (a) the demo slice (#448: bit-level mesh + `$add`/`$dff`) and (b) the AC-0 core pin including the licensing determination. Before orchestration consumes this capstone's machine block, land one `REPLAN:` comment that: repoints `requires_features` 320→**#61** and 342→**#62** and regenerates the table/mermaid so the three counts match reality; re-derives the prerequisite closure (dropping the unmirrored "#339 gates the spine" claim or deliberately re-mirroring it); and resolves Open Question 5 to match the dedup outcome (#62 is the canonical owner; #342 is gone). File the VCD-replay feature (checking #63 as the possible named demand) before any AC-3 work is funded — until then the capstone can progress but cannot complete.
