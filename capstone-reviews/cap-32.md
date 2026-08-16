**Capstone:** CAP-32 (#516) — read-only, zero-install browser demo of curated examples, no second execution engine
**Verdict: ready-with-gaps** — the tree is coherent, ACs compose, dependencies are real and acyclic; start is gated as designed, but three reconciliation gaps should be closed (one line each) before orchestration so an executor cannot mis-sequence.

## 1. Decomposition

**Tree walked:** CAP #516 → native children #572 (FEAT-C32-1, tour mechanism), #573 (FEAT-C32-2, static demo page), #574 (FEAT-C32-3, shop-window links), #886 (FEAT-C32-4, share-by-link, ON HOLD). Tasks: #572 → #833/#835/#837; #573 → #840/#841/#844; #574 → #845/#847; #886 → none (correct while on hold). Shared (non-child) prerequisites: #548 (FEAT-C27-2, curated corpus) and #551 (FEAT-C27-4, gallery) — both **open**, both funded under CAP-27. All eleven in-roster issues are open; none is double-owned. Ownership seams are explicit and clean: #844 is sole owner of footer content with #574 AC-4 confirming, not implementing; #847 vs #551 link/gallery split is stated in both; #548 AC-3 is the resolved owner of the `resources/samples/samples.properties` manifest that #841/#847/#573 consume (the file does not exist yet in master — consistent, since #548 is unbuilt; `examples/` today holds only `autograde/autograde.py`, exactly as the CAP's Sequencing section states).

**Gap D1 — the "ship now" screencast has no owning issue.** The CAP's Planned-features list includes "Ship now, independent of the gate: a recorded screencast" and `demo_slice` names it as the immediately-fundable slice — but no FEAT or TASK owns it (repo-wide search finds it only in #516's and #573's prose). As written it is orchestrated from nowhere. One filed task (or an explicit note that it rides an existing CI-rig issue) fixes this.

**Noted, not a defect:** #886 is a native sub-issue yet excluded from the roster and `band_mw`. Its body carries the same split (encoding AC-1..3 fundable outside this capstone; adapter AC-4..6 void until the #516 hold resolves, per its KC-32-4-1). The two records agree; no action needed.

## 2. Acceptance-criteria composition

- **CAP AC-1 (<30 s to interactive, Fast 3G, three largest examples):** carried by #572 AC-4, #573 AC-1 (<5 s target), and — decisively — #841 AC-2, which measures **every** published example against the <30 s budget with a committed script. Composition holds through #841 even though #572 AC-6 measures a different corpus (see Gap S3).
- **CAP AC-2 (no evaluator, read-only by construction):** #572 AC-3, #833 AC-4 (static bundle inspection), #835 AC-3/AC-5 (runtime debugger/coverage check + source read), #573 AC-2, #840 AC-3 (CI-checked bundle allow-list), #841 AC-5. Redundant in the right way — build-time and runtime checks are distinct, not overlapping owners.
- **CAP AC-3 (static files, zero third-party runtime/CDN):** #573 AC-3, #572 AC-4 (zero non-origin requests), #835 AC-4 (HAR evidence), #840 AC-2 (loads with non-origin requests blocked), #574 AC-3/#845 AC-2 (link-host allowlist). Covered.
- **CAP AC-4 (honest one-sentence disclosure + installer link):** #573 AC-5 → implemented by #844 (single shared partial, byte-identical across pages, `/releases/latest` link with offline check). Covered.
- Kill criteria propagate: KC-32-1 (curation, never a live evaluator) reappears as #572 AC-5/#841 AC-3; KC-32-2 scope cliff is restated in #572/#573/#574/#886; KC-32-3 in #573's boundary notes.

**Gap S3 (consistency nit):** CAP AC-1 says "each of the three largest curated examples," while #572 AC-6 explicitly measures "the three smallest, most-legible lead-with circuits… It is not the three largest," plus one ceiling probe. No child fails the capstone — #841 AC-2's every-example measurement subsumes both — but the capstone AC's wording is contradicted by a child that cites it. A one-line CAP AC-1 amendment ("every published example, per #841 AC-2") removes the tension.

## 3. Dependency chains

Acyclic and real: #548 → #572 (#833 → #835 → #837) → #573 (#840 → #841 → #844) → #574; #551 → #574 (and #845 Phase 1 depends only on #551, correctly startable earlier); #847 additionally after #841. No edge points at a closed or redirected issue — #500 (CAP-19) is cited everywhere as closed context (`closed/not_planned`, verified), never as a dependency. No unfunded external prerequisite: #548/#551 are open, in-project, CAP-27-funded. Stale-mechanism residue from the CheerpJ era has been scrubbed — #572/#833/#835/#837/#573/#574/#847 all carry the settled SVG+lookup/VCD direction; #837 was correctly redirected from "write a go/no-go verdict" to "record the settled decision in ARCHITECTURE.md §Recorded decisions" (section exists at line 233).

**Gap S1 — the roster contradicts the capstone's own funding gate.** #516's `demo_slice` says "fund PF-1's tour build only after the blocked_by gate clears," and Sequencing/KC-32-3 gate PF-1/PF-2 on #548+#551 landing **plus** a post-landing friction measurement. But TASK-C572-1 (#833) declares `ordering_after: []` and explicitly de-blocks itself ("Rather than block this task on #548…"), building the tour mechanism now against in-repo stand-ins (`test/fixtures/fork-4.6-shiftregister.jls`, `riscv/gui/cpu.jls` — both verified present). An orchestrator reading #833 starts it today; one reading #516 must not. Either #516 should carve out "mechanism proof on stand-in fixtures (#833/#835) may precede the gate; only curated-content build-out waits," or #833 should gain the gate. One clarifying line on either issue resolves it.

**Gap S2 — nobody owns the KC-32-3 measurement.** The gate that decides whether PF-1/PF-2 are funded at all ("does gallery + screencast already close most of the gap?") has no issue to take the measurement, define its method, or record the finding. As written it is a decision that can only be forgotten or made implicitly.

## 4. Staleness and evidence

Codebase claims verified against master:
- `CircuitRenderer` SVG export via JFreeSVG at the cited region (`src/jls/edit/CircuitRenderer.java`, SVG branch ~314–358) — **confirmed**, including the deterministic draw-order and fixed defs-prefix machinery the byte-identity claim rests on.
- `SvgExportTest#exportingTwiceIsByteIdentical`, `VcdExportGoldenTest`, `DeterministicSaveTest#canonicalBytesAreIdenticalWhateverThePlatformNewline` / `#stateHashIsContentDetermined`, `UntrustedFileHardeningTest`, `FileAbstractor`, `MAX_CIRCUIT_TEXT_BYTES` — **all present**.
- `-vcd` flag and `BatchSimulator` VCD export (`src/jls/JLSStart.java:107`, `src/jls/sim/BatchSimulator.java`) — **confirmed**; `docs/vcd-interop.md`, `docs/batch-interface.md`, `docs/reproducibility.md` — **present**.
- JFreeSVG 5.0.7 (pom) **does** expose `KEY_BEGIN_GROUP`/`KEY_END_GROUP` (verified in the jar) — the per-element `<g>` enabling change is real, and is genuinely not yet implemented (no hint usage in `src/`), matching #833 AC-1's framing.
- `maven.compiler.release` = 25 — the CheerpJ bytecode-ceiling rejection argument stands. `docs/grand-architecture.md` §3 names the headless-core demotion as "the highest-leverage single change" — as cited. Stable element ids (#165, `ElementId`) — present.
- Cost bands are plausible against scope: PF-1+PF-2+PF-3 = 2.5–5 mw matches the child sum (1–2 + 1–2 + 0.5–1); task bands sum inside their features.

**Gap S4 — the screencast's "near-zero incremental cost" claim overstates the rig.** #516 asserts "`scripts/wayland-rig.sh` … captures it with `grim`; `wtype` supplies synthetic input." The script (verified) requires and uses `sway, swaymsg, grim, jq` — **`wtype` appears nowhere in the script or CI**, and `grim` takes still screenshots; there is no video-recording path (no `wf-recorder`). A scripted screencast is still a reasonable extension, but it is new work (synthetic input tooling + video capture), not a "regenerated artifact" of infrastructure "that already runs on every push." This compounds Gap D1: the one deliverable declared fundable today is both unowned and cost-underestimated.

## Verdict

**ready-with-gaps.** The decomposition is sound, the ACs compose upward with deliberate redundancy on the load-bearing properties (no evaluator, no third-party runtime), the dependency graph is acyclic with no closed or unfunded edges, and every checked codebase citation resolves. Start now means: #845 Phase 1 (after #551), #833/#835 if-and-only-if Gap S1 is resolved in their favor, and the screencast once Gap D1 gives it an owner honest about Gap S4's real cost. Before orchestrating PF-1/PF-2 content work, close S1 (gate vs. roster contradiction) and S2 (own the KC-32-3 measurement); S3 is a wording fix.

| # | Gap | Fix size |
|---|-----|----------|
| D1 | Screencast deliverable has no owning issue | file one task |
| S1 | #833 `ordering_after: []` vs #516 KC-32-3/demo_slice funding gate | one line, either issue |
| S2 | KC-32-3 post-#548/#551 measurement unowned | one task or explicit CAP note |
| S3 | CAP AC-1 "three largest" vs #572 AC-6 "not the three largest" | one-line CAP AC-1 amendment |
| S4 | wayland-rig screencast claim stale (`wtype` absent, stills-only `grim`) | correct CAP prose when D1 is filed |
