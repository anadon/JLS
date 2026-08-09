# Issue #325: FEAT-031: one subcircuit instance runs as drawn logic or as a fast implementation of the same definition, and a harness proves the two agree
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings, most severe first

### 1. CRITICAL — the entire feature is normatively anchored to a document that does not exist in this repository
§1 states plainly: *"The contract exists at HEAD in draft and must be referenced, not restated,"* and cites `docs/parity-contract.md` by section and line number seven separate times (§2.2 `:132-142`, §3 `:262`, §4 `:402`, §5.2 `:479`, §5.3 `:514`, §6 `:572`, plus quoted text at `:3-7` and `:516-518`). Checked against the actual tree:

```
$ ls docs/parity-contract.md
ls: cannot access 'docs/parity-contract.md': No such file or directory
$ git log --all --oneline -- docs/parity-contract.md
(no output)
```

The file has never existed anywhere in this repository's git history — not "unratified," *absent*. The `evidence_commit` the issue pins everything to, `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`, is likewise not a reachable object (`git cat-file -t` fails), and the commit `b299d63` the issue credits with "demoting" the contract to unratified doesn't resolve either. Every downstream claim that depends on this document — the observation function being "written down before any binding exists" (Open Question 1's own precondition for closing integration), the permitted-to-differ set, the refusal list, the null-test wording quoted verbatim — is unverifiable from the actual codebase. This is not cosmetic: the issue's own text says *"Blocks integration: criterion 2 requires the observation function be written down before any binding exists, and an unratified document is not 'written down' in the binding sense"* — and the document isn't merely unratified, it doesn't exist, which is the same failure mode one level worse than the issue itself contemplates.
**Recommendation:** Before TASK-0065/0066 are filed, either commit `docs/parity-contract.md` for real at a real, resolvable commit, or fold its cited content (observation function, refusal set, null-test statement) directly into this issue's own body so the spec doesn't depend on a phantom file.

### 2. HIGH — the `blocks` edge to #326 is already stale
The machine block declares `blocks: [326, 347]` and the mermaid graph draws `F --> F38["#326 FEAT-038: the drawn structural RV32 machine"]`, with prose asserting *"every edge written here is mirrored on the issue it names."* Checked #326 directly: it is **closed, `state_reason: duplicate`**, closed 2026-08-04T07:50:55Z — about 45 minutes after #325's last comment (2026-08-04T07:05:00Z) — and #325 has not been updated to reflect this. The acyclicity argument in §"DAG walk" ("Forward from every `blocks` entry… #326 (FEAT-038)…") now names a node that isn't a live feature to block. It is unclear which issue #326 was a duplicate of, so whether #325 should still list a `blocks` edge at all, and to what, is unresolved.
**Recommendation:** Re-run the link pass on this issue; either retarget the `blocks` edge to whatever #326 was merged into, or drop it and record why in a `REPLAN:` comment, per the issue's own re-planning protocol (§7).

### 3. MEDIUM — a technical gap in the invariant, not just a citation gap
Criterion 5 requires *"a reflective guard asserts no binding touches the event queue."* But the entire simulator is event-queue-driven (`Simulator`/`SimEvent`/`Reacts`, confirmed in `ARCHITECTURE.md`), and a boundary's output must still reach the rest of the circuit somehow when the binding is active. The issue never states the mechanism by which a non-structural binding's computed output values are propagated onto the boundary's `Output` puts without "touching the event queue" — is a synthetic settle-event still posted on the binding's behalf by the harness (in which case the guard needs a precise definition of what counts as "the binding" touching it vs. the harness doing so), or does the binding write pin values directly, bypassing the event model entirely (in which case what enforces the observation function's own claim about sampling at "quiescence points")? As written this reads as an aspiration rather than a mechanism, and a reviewer implementing TASK-0065/0066 could satisfy the letter of "the binding doesn't call `Simulator.post`/similar" while still violating the spirit by wiring output changes through some other side channel.
**Recommendation:** Add one paragraph naming the exact seam a binding uses to drive its boundary's outputs and how that differs from "touching the event queue," or cite the (currently nonexistent) contract section that would answer this — see finding 1.

### 4. MEDIUM — several Integration Criteria are gameable as worded
- **I1** ("both runs produce the same golden… the run with a non-structural binding carries the banner") never specifies where the banner text must appear or what makes a banner "carry" the requirement beyond existing — a literal static string unconditionally printed on every run would satisfy a naive reading.
- **I6** ("Non-structural bindings are unavailable and the restriction is visible") — "visible" has no test surface named; nothing stops a UI element that's technically present but easy to miss from being certified as satisfying this.
- The harness coverage rule specifies a *"seeded $10^6$-vector sample"* but the issue never pins the seed value itself at the feature level. A null test built against an unpinned seed is a flaky/gameable test by construction — re-seeding until the failing case that should fail keeps failing, or (worse) until it stops, is indistinguishable from a correct implementation without a fixed seed on record.
**Recommendation:** Name the seed (or the seeding policy) here rather than leaving it to the child task, and replace "visible"/banner-appears language with a concrete assertion (e.g., "the CLI/GUI outcome line and VCD header both contain string X, checked by test Y").

### 5. LOW — feasibility/cost risk is self-acknowledged but worth restating plainly
Every one of the four planned tasks is unfiled, and the 5-8 mw band is a straight sum of four independently-unbenchmarked task estimates (1.5+1.5+2+2=7) with no reference to any completed task of comparable shape in this repository to calibrate against. That's consistent with how the rest of this issue corpus is scoped, so it isn't unique to #325, but it means the "no reconciliation needed" note in Open Question 4 is really "no reconciliation was possible," not "the estimate was checked."

## What's solid
- The out-of-scope boundary (compiled engine, general state serialization, RISC-V/Linux dependency for the demo) is drawn tightly and each exclusion cites a real, verifiable decision (#221, confirmed closed with `state_reason: completed`, and its Option 1 — discrete-event-only — is accurately represented).
- The `SubCircuit.java:282-289` and `Element.java:17-18` code citations are byte-accurate against HEAD (verified directly); the "cheap mechanism" argument built on them is sound.
- The #357 boundary comment (dedup pass) is a well-reasoned, non-duplicative split and is mutually consistent with #357's own text, which independently corroborates the boundary.
- The null-test-first sequencing decision (§6, §7 K4) is the correct engineering call and is stated as a hard stop, not a suggestion.

## Verdict rationale
The core mechanism (per-instance fidelity attribute, ports-and-values-only `Boundary`, null-toggle gate, falsifying null test) is a reasonable, well-scoped design. But the issue cannot honestly proceed to filing TASK-0065/0066 as written: its own Open Question 1 makes ratifying the parity contract a blocker for integration, and that contract does not exist anywhere in the repository — an issue defect, not an implementation defect, and one a filer would only discover after starting work. Combined with a stale `blocks` edge to a now-closed-as-duplicate issue, this needs a documentation/link-pass correction before it's ready to spawn child tasks.
