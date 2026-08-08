# Issue #561: FEAT-C29-4: a Falstad text-format circuit's logic subset opens in JLS as a working circuit, and every analog element is a named loss by design — the smallest importer proves the shared-report generalization
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue's own framing — "sibling" of CAP-16, independent, the *smallest* of the three CAP-29 importers — is contradicted by its own `ordering_after` field, which routes the true critical path through CAP-16's entire required-feature set (a 30-50 mw capstone) via a two-hop dependency. Beyond that structural problem, AC-1's single-circuit acceptance bar is markedly weaker than the corpus standard its own capstone sibling (CAP-16) already established for the same class of claim, and AC-2's "written mapping table" has no totality/registry discipline attached even though the sibling capstone explicitly identified that exact gap as a correctness hazard (the shift-register name-collision precedent). No Falstad-related code, fixture, or reference exists anywhere in the repository today — confirmed by `grep -rIl falstad src/ test/ docs/` returning nothing — so every claim here is unverified against the actual target format.

## Findings, most severe first

### 1. "Sibling, independent of CAP-16" is contradicted by the issue's own ordering chain, which runs through CAP-16's full required set

Boundary note: *"`.circ` belongs to CAP-16 (#311) / FEAT-025 (#323); this is a CAP-29 (#513) sibling."* This reads as a claim of independence from the `.circ`/CAP-16 line. But `ordering_after` names `"FEAT-C29-1 (shared report contract — this importer emits that shape)"` — issue #556 — and #556's own `ordering_after` is `["#323 FEAT-025 (CAP-16's .circ importer and its loss-naming report land first and define what is being generalized)", "#314 FEAT-002"]`. So #561 cannot start (per its own stated ordering) until #556 lands, and #556 cannot start until #323 (FEAT-025) lands, and #323 is CAP-16's centerpiece — itself `blocked_by: [314, 349]` and part of a required-feature set CAP-16 (#311) prices at a **30-50 mw standalone band**, none of which exists yet (`src/jls/imp/` does not exist; confirmed by `ls`). The "smallest importer, 2-3 mw" framing is true only of the marginal Falstad-specific work; the actual wall-clock critical path this issue sits on is dominated by an unrelated, much larger capstone it explicitly disclaims dependence on.

**Recommendation:** Either state plainly in the machine block that #561's real earliest-start date is gated by CAP-16's required set (via #556→#323), or decouple #556 from #323 so the "shared report contract" can be generalized from a synthetic/abstract spec rather than waiting on the concrete `.circ` importer — the latter is what the "sibling, independent" framing implies should be possible but the ordering graph as written forecloses.

### 2. AC-1's one-circuit bar is gameable, and weaker than the corpus standard the same capstone already set for this exact claim shape

AC-1: *"One real published Falstad circuit imports with zero unexplained losses."* Compare CAP-29's own parent capstone AC-1 for the sibling `.circ` importer (CAP-16 #311), which required, after an explicit re-derivation: *"a corpus of **at least 30 `.circ` files drawn from at least 3 independent public course repositories**"* (KC-16-1), specifically because a single hand-picked file cannot demonstrate general format coverage and the corpus measurement "could reorder every increment that follows." #561's AC-1 inherits none of that discipline — a single circuit, chosen by the implementer, can trivially avoid every hard case (multi-driver labeled nodes, subcircuit/IC blocks, mixed logic-inputs feeding an analog scope probe, Falstat's "custom logic" truth-table element) and still satisfy the letter of AC-1 and AC-2 while leaving the "logic subset" claim essentially untested.

**Recommendation:** Require a small corpus (even N≥3-5, proportionate to the 2-3 mw band) drawn from independently-authored circuits, not one file, mirroring KC-16-1's rationale.

### 3. AC-2's "written mapping table" has no totality/registry discipline, and the sibling capstone already identified this exact gap as a live correctness defect

CAP-16 (#311) required FEAT-001 (#315) specifically because *"a construct map **is** a registry-keyed table [and] a non-total map is exactly how a construct gets dropped silently"* — and names a concrete precedent: `src/jls/hdl/HdlExporter.java:83-84` records that Falstad-adjacent tooling and Logisim both have a construct (shift register) whose name maps to the *wrong* JLS semantics if mapped by name rather than by verified behavior (KC-16-3, "a correctness defect, not a polish item"). #561's `ordering_after` cites only #556 and #314 — FEAT-001/#315 appears nowhere. AC-2 asks for "a written mapping table" with no totality test, no registry-keyed structure requirement, and no falsification discipline analogous to CAP-16's AC-2 (which demands the report-totality assertion — "the set of constructs dropped equals the set reported" — be proven in *both directions*, not merely asserted). Nothing here stops the exact silent-partial-map failure CAP-16 spent significant machinery guarding against.

**Recommendation:** Add FEAT-001 (#315) or an equivalent totality test to this issue's dependencies, and state AC-2's mapping-table completeness as a bidirectional equality (element types encountered in the corpus ⟺ element types the table names), not a prose promise.

### 4. "The Falstad text format" is treated as one stable spec; it is not, and the issue names no version/dialect to target

Falstad's circuit text format has multiple lineages (the original Java-applet `falstad.com/circuit` exporter and the actively-developed CircuitJS1 JS port, which has added element codes and a `$` header-flags scheme over years of releases; some element type codes are positional/ordinal and shift meaning across format revisions). The issue body never names a target version, a canonical grammar reference, or how format-drift in an upstream project JLS does not control will be handled going forward. This is the same category of hazard CAP-16 flags for `.circ`'s hard-coded per-component geometry rules, but CAP-16 at least names the specific upstream artifact and raises licensing/geometry-absorption as an open question (#311 Open Questions 1-2); #561 raises neither.

**Recommendation:** Pin a specific upstream format-version/commit as the target spec, and record what happens when a file declares a newer/different dialect (reject loudly, per the project's own `FORMAT`-header precedent in `docs/file-format.md`, rather than silently misparsing).

### 5. AC-5's "never silently rewrites semantics" is in tension with the by-design exclusion of analog elements at the logic/analog boundary

Boundary note: *"Analog elements are named losses by design"* — reasonable as a scoping decision. But real coursework circuits frequently have mixed-signal constructs at exactly this boundary (e.g., an RC network feeding a Schmitt-trigger/comparator used for debouncing, or an analog oscillator clocking a counter) where the "logic" element that survives import is only meaningful in combination with the "analog" element that is dropped. Naming the analog piece as a loss in the report is honest, but the retained "logic" element (e.g., a bare comparator with a now-unconnected input) can behave nothing like its role in the original circuit — which is a semantic rewrite of the surviving circuit's behavior even though every dropped construct is individually named. AC-2's "maps by semantics" and AC-5's "never silently rewrites semantics" do not jointly define what happens to a logic element that becomes meaningless once its analog context is stripped.

**Recommendation:** Add an explicit rule for logic elements whose sole driver/consumer is an excluded analog element (e.g., also refuse/flag them, don't leave a dangling half-imported gate), and make it part of the mapping table's completeness contract from Finding 3.

### 6. "Hardened per the #38 standard" cites a closed issue whose fixes are specific to a different container format, with no bounds stated for this one

#38 is closed and its concrete fixes (zip-entry size caps, `initrle` run-length caps, XZ line-read bounds, FD-leak fixes) are specific to the `.jls` container/RLE grammar — there is no generic "untrusted-text-parser hardening library" #561 can invoke by reference. AC-4 says "malformed and hostile inputs refuse loudly with bounds tests" but names no concrete bounds (max element count, max line length, numeric-field overflow, recursion/nesting depth for subcircuits) for the Falstad grammar specifically, unlike #38 itself, which enumerated exact payloads (`initrle "0:0:7fffffff"`, high-ratio zip, etc.) before calling itself done.

**Recommendation:** Enumerate the hostile-input classes for Falstad-text specifically (oversized coordinate/value fields, unbounded element counts, malformed `$`-flag headers, recursive/self-referential labeled-node references) the way #38 enumerated its own, rather than pointing at #38 by name and assuming the discipline transfers.

### 7. Kill criterion's stop-loss doesn't state what it's measured against, given Finding 1's hidden dependency cost

KC-29-1: *"stop-loss at 1.5× the 2-3 mw band"* (i.e., 4.5 mw). If this is meant to include the wait/critical-path cost of #556→#323→CAP-16 (Finding 1), it is not remotely realistic — CAP-16 alone is priced at 30-50 mw standalone. If it is meant to price only the Falstad-specific marginal work once shared infrastructure already exists, that should be stated, the way CAP-16's own Cost section explicitly distinguishes "standalone" from "marginal" bands for exactly this reason.

**Recommendation:** State explicitly whether the 2-3 mw / 4.5 mw kill criterion assumes #556 and its transitive dependencies are already landed, mirroring CAP-16's standalone-vs-marginal cost split.

### 8. The dedup/boundary comment's defense of non-merger is sound — no issue

The issue's one comment (2026-08-04) argues #561 must stay a separate closeable unit from #556/#558/#559 because AC-3 ("no Falstad-specific report dialect") is only a meaningful, falsifiable claim if the adopter and the contract are different issues. That reasoning holds up: folding #561 into #556 would make AC-3 a tautology, exactly as argued. No further issue here.

### 9. AC-3 and the "no Falstad-specific report dialect" framing are concretely testable — no issue

Given #556 lands with a golden-tested schema, AC-3 is a clean, mechanically checkable requirement (schema-conformance test against the shared contract). No notes.

## Verdict rationale

The issue is internally well-argued on the one point it defends explicitly (non-merger, Finding 8/9), but on every other axis it is thinner than its own sibling capstone already demonstrated is necessary for this exact class of claim: a corpus standard instead of one file (Finding 2), a totality-tested mapping table instead of prose (Finding 3), a licensing/format-version discipline CAP-16 already modeled and this issue skips (Finding 4), and — most severe — an "independent sibling" framing that its own `ordering_after` field contradicts by routing through CAP-16's entire required set (Finding 1). These are fixable without discarding the feature's premise, but the acceptance criteria as written could be satisfied by an implementer while leaving the actual "logic subset opens as a working circuit" claim substantively untested. **needs-rework.**
