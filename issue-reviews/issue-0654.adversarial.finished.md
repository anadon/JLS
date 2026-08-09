# Issue #654: TASK-C565-3: the synthesized netlist is placed and routed legibly through the #62 layouter lineage, and lands as an ordinary editable circuit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core premise — hand the truth-table synthesizer's two-level netlist to the already-landed `SchematicLayouter`/`HeuristicLayeredLayouter` seam from #62 instead of writing a second layout engine — is architecturally sound and matches what is actually on `master` (`src/jls/hdl/layout/LayoutGraph.java`, `HeuristicLayeredLayouter.java`, `LayoutInvariants.java` all exist and are netlist-agnostic by design: "a layouter sees only opaque rectangles with ports — never netlist semantics," `LayoutGraph.java:14-15`). But the issue has one undisclosed ownership collision with #62 itself, one acceptance criterion that contradicts the task's own stated boundary, and one acceptance criterion that is underspecified against machinery already in the codebase.

## Findings, most severe first

### 1. [HIGH] AC-1's "generated-netlist" consumption collides with #62's own unfiled, unresolved planned task for exactly this seam

#62's decomposition table lists a `planned_tasks` entry — *"Layout entry point for programmatically generated `.jls` netlists (generator consumers: #202 CPU, #73 sample circuits) behind the same `SchematicLayouter` seam"* — status "Not filed," and #62's own "Open Questions" §2 asks: *"Where the generated-netlist entry point lives (this feature vs. #202): recommended default — here [i.e. #62]… Blocks filing that planned task, not #290 or integration."* A truth-table-synthesized two-level netlist (#653/TASK-C565-2's output) is precisely a "programmatically generated netlist" in #62's own vocabulary — the same category as the RV32I CPU generator (#202) and sample circuits (#73) that #62 explicitly earmarks for a *shared* entry point it hasn't built or assigned yet.

#654 never references this open question, never declares a `blocked_by`/coordination edge to it, and its own boundary note ("Layout is reused, not rebuilt… capability gaps found here are filed against #62, not worked around locally") implies #654 should *not* build this adapter itself. Yet nothing says who does. As written, #654 can be implemented by silently building an ad hoc `LayoutGraph`-from-synthesized-netlist adapter, which would either duplicate or diverge from whatever #62 eventually builds for #202/#73 under the same unresolved planned task — exactly the kind of two-implementations-of-one-seam outcome #62's seam-first design was meant to prevent.

**Recommendation:** before work starts, either (a) #654 explicitly claims the generated-netlist `LayoutGraph` adapter as in-scope here and a REPLAN comment on #62 retires/reassigns that planned task to point at #654/#565, or (b) #654 is blocked on #62 filing and landing that entry point first. Leaving it implicit risks wasted or conflicting work on two issues at once.

### 2. [HIGH] AC-1's fallback clause contradicts the task's own "reused, not rebuilt" boundary and cites a source that doesn't say what it's cited for

AC-1: *"Synthesis output is placed and routed by the #62 layouter lineage where available, **with a documented fallback placement if it is not yet landed** (FEAT-C31-3 AC-3)."* But #565 (FEAT-C31-3) AC-3 actually reads: *"The generated layout is placed and routed legibly using the #62 heuristic-layouter lineage where available; the drawn result is an ordinary editable circuit, not a locked artifact."* — there is no "documented fallback placement" language in the cited source. The fallback requirement is invented in #654, not derived from the AC it cites, and no fallback design is described anywhere in either issue.

Worse, "if it is not yet landed" is ambiguous between two readings with very different scope implications:
- **Code absent from master** — false today: `HeuristicLayeredLayouter`/`SchematicLayouter`/`LayoutGraph`/`LayoutInvariants` are already marked "Landed" in #62's own decomposition table (PRs #194, #196). Under this reading the fallback clause is dead weight.
- **Issue #62 not yet closed** — true today: #62 is blocked on #290 (the rubric/goldens run) and remains open. Under this reading, AC-1 requires building and documenting an entirely separate placement algorithm whenever #62-the-issue hasn't formally closed — directly contradicting the Boundary note's "reused, not rebuilt," and doubling the task's real scope (build two layout paths) without saying so.

**Recommendation:** drop the fallback clause or resolve which reading is intended; if the landed layouter is meant to always be used once code exists (the sane reading given it's already on master), say so plainly instead of gesturing at an undefined contingency.

### 3. [MEDIUM] AC-4 ("saved bytes are reproducible") doesn't engage the replica-id machinery that actually governs it, making the criterion gameable

`ElementId.mintFresh()` (`src/jls/elem/ElementId.java:210-213`) mints ids from a per-process `replica` value that — absent an explicit `jls.replicaId`/`JLS_REPLICA_ID` override or a previously-persisted config file — is "a fresh random draw (32 hex digits from a UUID… SecureRandom-backed)" (`ElementId.java:46-51`). The class's own javadoc is explicit that this "makes from-scratch saves reproducible run-to-run **on one install**" — cross-install / clean-checkout byte-identity needs the documented pin. #654's AC-4 says only "Layout is deterministic for a given netlist, so a synthesized circuit's saved bytes are reproducible," never mentioning this precondition or the existing `DeterministicSaveTest`/`StableElementIdTest` pattern the codebase already uses for exactly this class of claim.

The layout math itself is already proven deterministic and unit-tested (`HeuristicLayeredLayouterTest.java:147,151` assert placement/route determinism), so a verification could trivially pass by re-running the layouter twice in-process on a fixed `LayoutGraph` (already true today, nothing new to build) while never confirming the actual AC-4 claim: that two independent synthesis-and-save runs on a fresh checkout produce byte-identical `.jls` files. Per the documented design, that additionally requires pinning the replica id — a precondition AC-4 doesn't state.

**Recommendation:** AC-4 should name the reproducibility scope explicitly (e.g., "byte-identical under a pinned `jls.replicaId`, consistent with `DeterministicSaveTest`") and require a save-to-disk round-trip test, not just a layout-coordinate comparison.

### 4. [MEDIUM] The `ordering_after` block is structurally inconsistent with the DAG-tracking rigor #62 itself uses, undermining any tooling built on these YAML headers

`ordering_after: ["TASK-C565-2 (the netlist this places)", 62]` mixes an annotated string and a bare integer in one array, and #654 carries none of the `blocked_by`/`blocks`/"Ordering-graph walk" apparatus that #62 uses for the identical kind of claim (explicit prose DAG-walk paragraph plus a mermaid diagram). If these machine-readable blocks are meant to drive tooling (as #62's own walk explicitly argues), a heterogeneous, unexplained `ordering_after` list is a real parsing/traceability hazard, not just a style nit — especially since finding #1 shows the dependency on #62 is more subtle than "wait for #62 to close."

**Recommendation:** normalize to bare issue numbers and add the same `blocked_by` field and DAG-walk sentence #62 uses, once finding #1 is resolved and the real dependency shape is known.

### 5. [LOW] AC-2 restates #62's already-landed `LayoutInvariants` contract in prose instead of citing it

"No elements overlap and no wire crosses an element in the generated layout, asserted mechanically rather than by eye" is exactly what `LayoutInvariants.check` already enforces (on-grid, anchored, orthogonal, non-overlapping bodies — landed, per #62's evidence table). Restating it invites a second, possibly divergent implementation for this task specifically. Cite `LayoutInvariants.check` directly rather than re-deriving the same property in AC prose.

## What's solid

- The reuse premise is real, not aspirational: `LayoutGraph` is genuinely netlist-agnostic by construction, so wiring a truth-table-synthesized two-level netlist through it is architecturally sound, not a stretch.
- AC-3 (ordinary editable circuit, no lock/marker, save/load/undo-identical) is concretely testable against the existing round-trip suite (`CircuitRoundTripTest`, `AllElementsRoundTripTest`) and is well-precedented in this codebase.
- No new licensing hazard: consuming the hand-rolled, already-in-repo layouter (not ELK) stays inside the GPLv3 in-process-linking constraint #62 already established; #654 introduces no new dependency.
