# Issue #365: FEAT-041: a drawn design becomes a buildable parts list — every element assigned a package and section deterministically, word-level parts cascaded into real slices, with the BOM, the wiring list and two datasheet checks
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

FEAT-041 is a large, formally-written feature issue for a headless "physical program" (packing → BOM/wiring-list → cascade decomposition → fan-out/loading check) sitting on top of two prerequisite features (#336, #349). The technical narrative is well-grounded in real code, but the issue as currently readable **misrepresents its own filing state** and **cites an evidence base that does not exist on the default branch and was subsequently deleted**, both of which directly violate the issue's own Definition of Done. There is also a real gap in the element-taxonomy census the issue treats as authoritative.

## Findings, most severe first

### 1. [High] The roster table and mermaid graph are stale and contradict the issue's own comment — three of five "Not filed" tasks are filed, open issues

The body's §2 Decomposition table and the machine-block mermaid graph both mark TASK-0086, TASK-0087 and TASK-0088 as **"Not filed."** Quoting the table: `| planned — TASK-0086 (2 wk) | ... | Not filed |`, likewise for TASK-0087 and TASK-0088.

But the issue's own comment (posted 2026-08-04T15:44:36Z, same author, `anadon`) states plainly: `#365 = #394 + #427 + #430`, naming #394 as TASK-0086, #427 as TASK-0087, #430 as TASK-0088. I fetched all three: they exist, are `state: "open"`, and their titles and bodies match the descriptions verbatim (#394 "a drawn circuit becomes a parts order...", #427 "an 8-bit adder becomes two cascaded parts...", #430 "a net that drives more load than its weakest driver can carry..."). `updated_at` for #365 is exactly the comment's timestamp, meaning the comment was added but the body's own status table/graph were never edited to match.

This is not cosmetic: an engineer opening #365 today, without reading every comment, will believe none of the five planned children exist, will not discover that TASK-0086/87/88 already carry substantial dependency detail (e.g. #427 declares `blocked_by: [400]` and a real ordering conflict with #394's goldens; #430 declares `blocked_by: [400, 394]` plus an unlinked dependency on #427), and risks re-filing or re-scoping work that is already in flight. This also directly fails #365's own Completion-Criteria bullet: *"Machine block, roster table, and mermaid graph agree with reality at close (rule A)"* — it does not agree with reality now, well before close.

**Recommendation:** Edit §2's table and the mermaid graph to reflect `#394`, `#427`, `#430` as filed/open, and update the `planned_tasks` YAML block (which still lists prose descriptions instead of issue numbers) to match. This is a five-minute edit; leaving it stale erodes trust in every other "Status" claim in the document.

### 2. [High] The cited evidence corpus is not on the default branch, and the only branch that ever had it later deleted it

The issue pins `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and repeatedly cites `docs/plan/evidence/capstone-plan.md` and siblings, saying they "landed in `3a81a4a`... after the evidence commit." I checked ancestry directly:

```
git merge-base --is-ancestor 2d0ca9d origin/master   → NOT an ancestor
git merge-base --is-ancestor 3a81a4a origin/master   → NOT an ancestor
git ls-tree -r --name-only origin/master -- docs/plan → (empty)
git branch -a --contains 3a81a4a                      → only origin/claude/jls-virtual-hardware-linux-njsoma
```

Both the evidence commit and the `docs/plan/evidence/` corpus exist only on a side branch, never merged to `master` (confirmed default branch via `git remote show origin`). Worse, that same side branch **later deleted the entire corpus** in commit `742da74` ("docs: remove the planning corpus now that it is encoded in issues"). So the cost bands this issue reconciles against (§ Cost: "Band 5-8 mw," the S9/S16/S17 spine rows) rest on a document that (a) a reader on the default branch cannot open today, and (b) no longer exists in the tree that produced it either.

This directly fails #365's own Completion-Criteria bullet: *"Every cited evidence document and permalink resolves on the default branch at close."* It already fails to resolve at filing time — "at close" is not a meaningfully higher bar here since the source was deleted, not merely unmerged. The code-level claims (Adder, ElementRegistry, Circuit, PinBindings — see "Solid parts" below) are independently verifiable and do check out on `master`; the *planning-corpus* claims (cost bands, capstone plan) are not.

**Recommendation:** Either re-derive and re-cite the cost figures against a document that is actually reachable from `master`, or explicitly flag the cost section as provisional/unverifiable pending a link that resolves. Do not let a Definition-of-Done checkbox reference a document that has already been deleted from the branch that authored it.

### 3. [Medium] The "35 registered types, 9 non-decomposable" census the issue treats as settled leaves 13 types unclassified

Evidence §2 asserts, as fact: 35 registered types (verified — `ElementRegistry.ALL` on `master` has exactly 35 entries), of which 9 are non-decomposable (Memory, RegisterFile, TruthTable, StateMachine, FieldExtend, SigGen, TestGen, Display, SubCircuit) and names 13 more as explicitly decomposable ("Adder, Register, ShiftRegister, Mux, Decoder, the gates, Constant, TriState" = Adder, Register, ShiftRegister, Mux, Decoder, AndGate, OrGate, NotGate, NandGate, NorGate, XorGate, Constant, TriState).

9 + 13 = 22. The remaining 13 registered types are never classified anywhere in the issue: `Binder`, `Clock`, `DelayGate`, `Extend`, `InputPin`, `JumpEnd`, `JumpStart`, `OutputPin`, `Pause`, `Splitter`, `Stop`, `Text`, `WireEnd`. I read each class's javadoc directly:

- `Text.java:8-9`: *"Put text into the circuit. Has nothing to do with simulation, used simply to annotate the circuit."* — not a physical component at all.
- `Pause.java:16-17` / `Stop.java:14-15`: simulator control directives ("Causes simulator to pause/terminate when input is asserted") — no board-level realization exists for "pause the simulator."
- `WireEnd.java:9`, `Binder.java:9`, `Splitter.java:9`, `JumpStart.java:9`, `JumpEnd.java:9`: routing/wire-bundling constructs, not gates — arguably not "packable into a package/section" at all, since on a real board they are just wire, not a chip.

The issue's own Integration Criterion 5 demands exactly this kind of totality: *"swept over `ElementRegistry` rather than a fixed list, so a thirty-sixth type cannot ship into a packer that silently ignores it."* But the issue's own worked example, for the current 35 types, is not total — over a third of the registry has no stated disposition (realizable? unbound-with-reason? structurally exempt because it has no physical existence?). This isn't fatal to the feature, but it means Integration Criterion 5, which the issue presents as an assured outcome, has not actually been demonstrated even informally at the issue-writing stage, and a category the issue never names (schematic/annotation/control elements that are not physical components) is missing from the taxonomy entirely.

**Recommendation:** Before filing further children, produce (even informally, in a comment) the full 35-row classification — including a third bucket for "not a physical component, therefore out of packing scope by construction" — so TASK-0086's implementer has an actual total mapping to build `decompositionIsTotalOverTheRealizationPolicy()`-style tests against, rather than reconstructing it from scratch.

### 4. [Medium] Open Question 1 claims to gate TASK-0086's filing, but TASK-0086 was filed without the question being marked resolved on #365

§ Open Questions, Q1 (the user-supplied part-binding mechanism for `Memory`/`RegisterFile`) states: *"Blocks filing TASK-0086, because the unbound-list contract depends on whether an unbound element is an error or an invitation."* TASK-0086 is filed as #394, and #394's own body (O7, P7) does encode an answer — the `-parts` escape-row mechanism — but #365 has no comment recording that Q1 was answered, and #365's Open Questions section still lists it as open with no resolution note. Either the gating language ("Blocks filing") is not actually enforced in practice, or the process for marking a blocking open question resolved is being skipped. Given Finding #1 (the roster table is already known to be stale), this looks like the same underlying failure mode: child issues are being filed and are drifting ahead of the parent issue's own bookkeeping.

**Recommendation:** When a child issue's design answers a parent's "blocks filing" open question, say so explicitly in a comment on the parent (a one-line "Q1 resolved by #394, adopting default (a)" suffices) rather than leaving the open question apparently unresolved.

### 5. [Low] Cost figures are numerically self-consistent but their sourcing is compromised by Finding 2

The 9.5 mw / 5.5 mw reconciliation arithmetic is internally consistent (2+2+1.5+2+2=9.5; unshared remainder 2+2+1.5=5.5), and the shared-task accounting logic (TASK-0078 with FEAT-037, TASK-0093 with FEAT-043/FEAT-027) is coherently explained. The problem is purely that the number these figures are checked against (the "5-8 mw" band from the deleted `capstone-plan.md`) is not independently auditable by anyone reading the repo today — see Finding 2. Not a separate defect, but worth listing so it isn't mistaken for "the math is wrong": the math is fine, the citation underneath it is not.

## Solid parts (no rework needed)

- **The core cascade-decomposition motivation is real and correctly cited.** `Adder.resetPropDelay()` at `src/jls/elem/Adder.java:259-261` genuinely computes `propDelay = bits * defaultPropDelay`, confirming JLS models an adder as one arbitrary-width element with no physical-part boundary — the "netlist emitter is not a pure projection of the WireNet partition" claim is sound.
- **Stable-id ordering is real, already load-bearing, and correctly cited.** `Circuit.getElementsInStableOrder()` at `src/jls/Circuit.java:479-485` exists exactly as quoted, and is consumed at `Simulator.java:151` and `:196` for the documented reason (`:193-195`, verified verbatim). Building refdes purity on this precedent is a reasonable, low-risk design choice.
- **The error-aggregation idiom citation is accurate.** `PinBindings.java:37-53`'s javadoc matches the quoted text exactly, and is a sound precedent to reuse for the packer's diagnostics.
- **Scope boundaries (out-of-scope list) are coherent and correctly deconflicted against sibling features** (#341's deliberate non-dependency for the loading check, #329's placement ownership, #366's emitter ownership) — cross-checked against #336 and #349's own bodies, which mirror the same edges from their side.
- **The `blocked_by: [336, 349]` edges are correctly mirrored**: both #336 and #349 list `365` in their own `blocks` arrays, and both are genuinely open with unlanded prerequisite tasks of their own, so the dependency claim is honest rather than aspirational.
- **The zero-GUI/headless framing is consistent with the actual project architecture** (README.md and ARCHITECTURE.md both describe an existing headless batch surface with `-b`, HDL/VCD export, and a documented exit-status contract in `docs/batch-interface.md`), so this feature's shape is not inventing a new concurrency/deployment model.

## Net assessment

The engineering reasoning is careful and the code citations that matter for implementers (stable-id order, the aggregate-error idiom, the Adder width problem) are accurate and reproducible on `master`. But the issue fails two of its own explicit Definition-of-Done bullets right now — the roster/graph does not agree with reality, and the cited evidence does not resolve on the default branch (and never will, since the source was deleted) — plus it presents an incomplete taxonomy as if it were a completed census. These are all fixable by editing the issue text rather than by redesigning the feature, which is why this lands at "needs-rework" rather than "should-not-proceed": nothing here indicates the feature itself is wrong, only that the issue's paperwork about its own state and its own sourcing is currently wrong.
