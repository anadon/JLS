# Issue #401: TASK-0092: a second canvas places parts on a solderless breadboard — its own geometry, ops, undo and default-hidden palette, added inside the editor's coverage budget
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-evidenced for its factual claims about the codebase — every line-pinned quote I spot-checked against the current tree (SimpleEditor.java line count, pom.xml's "deliberately unfloored" prose, CircuitOp's sealed permits list, CircuitOpReader's string-keyed switch, PaletteContractTest's totality assertion, ArchitectureRulesTest's headless rule) matches exactly. That rigor is real and should be credited. The problems are structural: two of three "hard prerequisites" don't exist as filed issues, the machine-readable dependency block is already stale relative to the issue's own most recent comment, one cited acceptance mechanism (P8) doesn't actually cover what it's claimed to cover, and the undo design section is written as if undo is per-op when the actual `UndoManager` is whole-circuit-snapshot based — a mismatch the issue never reconciles.

## Findings, most severe first

### 1. P8's enforcement mechanism is misdescribed — the cited test does not check the package it's cited for

O5 and P8 both assert: *"`test/jls/ArchitectureRulesTest.java:124-132` carries the headless rule that `jls.bread` geometry must satisfy: no `java.awt`."* I read that method (`coreDependsOnNoGuiClasses`, lines 122-136):

```java
noClasses()
    .that().resideInAnyPackage("jls.core..", "jls.elem..",
        "jls.sim..", "jls.hdl..")
    ...
    .should().dependOnClassesThat()
    .resideInAnyPackage("java.awt..", "javax.swing..", "jls.edit..")
```

The `resideInAnyPackage` selector names `jls.core`, `jls.elem`, `jls.sim`, `jls.hdl` — not `jls.bread`, which doesn't exist yet and isn't in that list. This rule will pass trivially whether or not `jls.bread` imports AWT, because it never looks at `jls.bread` in the first place. P8 ("`jls.bread` imports no `java.awt`; `ArchitectureRulesTest`'s headless rule passes") is therefore satisfiable by an implementation that *does* import AWT in the geometry package, as long as it doesn't touch the four packages this rule actually governs. The completion-criteria checklist item ("Assert `jls.bread` imports no AWT (P8)") doesn't specify adding `jls.bread` to this rule's selector or writing a new ArchUnit rule scoped to it — it's phrased as if the existing test already does this.
**Recommendation:** either (a) add `jls.bread..` to `coreDependsOnNoGuiClasses`'s `resideInAnyPackage` list, or (b) write a dedicated ArchUnit rule for `jls.bread`, and say so explicitly in §8 Method rather than citing the existing test as already-sufficient.

### 2. The undo design (H3, §7.9, §7.10) is written for per-op undo; the actual `UndoManager` is whole-circuit-snapshot based

H3 states undo is "one stack with per-view entries," and §7.10 formalizes `view(o) = v ⟹ undo(o) affects only view v` as a property of individual ops. But `src/jls/edit/UndoManager.java`'s own javadoc (landed via #84/PR #194) says plainly: *"Capacity (U7 decision on issue #84): whole-circuit snapshots are kept... `CircuitSnapshot` bounds them under 100 bytes/element."* This is a stack of whole-`Circuit` snapshots, not a log of individually-invertible `CircuitOp`s with per-entry view tags. The issue's Materials section says the canvas is wired through "the **same** `Viewport` and `UndoManager` the schematic uses" as if that settles H3, but it doesn't: a snapshot-based undo restores the *entire* circuit (schematic geometry, breadboard placements, everything) to a prior point, and the issue never explains how a "view discriminator on an op" is supposed to interact with a mechanism that has no concept of ops at all. It may well work out in practice (a snapshot taken after only a breadboard-side change will be schematic-identical to the one before it, so P3 could pass "by construction"), but that argument is never made in the issue, and it's exactly the kind of thing that should be spelled out before implementation starts rather than discovered mid-task.
**Recommendation:** add a subsection reconciling the op-level inverse formalism in §7.10 with the actual snapshot-based `UndoManager`, or state explicitly that `UndoManager` itself must change (which is a materially different, larger task than "wire through the same instance").

### 3. The dependency graph in the issue body is already stale, per the issue's own comment

The single existing comment (2026-08-08, same day as the issue's last update) states: *"MISSING ORDERING EDGE, NOW OWED HERE... `ordering_after: [84]`... add to this issue's existing edges."* It explains that #441 (which would have carried this edge) was closed as a duplicate into #84, and that #84 — the headless gesture-machine extraction — is a real prerequisite because at HEAD the interaction state machine lives inside `EditWindow extends JPanel`, uninstantiable without a display; a second canvas built before #84 lands would either duplicate the nine-state machine or be welded to the schematic's panel. I independently confirmed this coupling (`EditWindow` is a non-static inner `JPanel` at `SimpleEditor.java`, and `#84`/`#441` document the extraction as unshipped). This is a credible, well-argued dependency — but the issue's own machine-readable `blocked_by: [318, 316, 365]` block was **never updated** to include it; the fix lives only in a comment. Any executor or tool that reads the YAML block (as the issue's own filing convention implies it should be read) and not the comment thread will miss it. This is the kind of half-edge the project's own "Link phase" process elsewhere claims to guard against.
**Recommendation:** update the machine block itself (`blocked_by: [84, 318, 316, 365]`) rather than leaving the correction stranded in a comment.

### 4. Two of three "hard, not convenience" prerequisites are unfiled and untrackable

The issue is explicit that TASK-0036 (view discriminator + `VIEW` section) and TASK-0105 (per-view palette) are "hard prerequisites, not conveniences," and that TASK-0105 in particular is required because `PaletteContractTest.paletteIsTotalOverTheElementRegistry` currently forbids a second palette outright (verified — the test and its three-entry exception set at lines 44-48 match exactly). Yet both are marked "being filed concurrently" and are absent from `blocked_by` (which cannot reference numbers that don't exist). The only enforcement is a checklist bullet ("Confirm TASK-0036's view discriminator and `VIEW` section, and TASK-0105's view-dimensioned palette contract, have landed"), which is a manual, unverified gate — nothing stops an executor from starting the two "Add `jls/bread/...`" steps before either lands, since GitHub's own dependency UI shows nothing blocking. Given #329 (the parent feature) also lists these as unfiled at time of writing, the risk is concrete, not hypothetical.
**Recommendation:** don't file/open #401 for pickup until TASK-0036 and TASK-0105 exist as real issues with mirrored `blocked_by`/`blocks` edges, consistent with the project's own stated Link-phase discipline.

### 5. Open Question 1 is marked "Blocks execution absolutely" with no visible ratification mechanism

The strip-representation choice (synthetic wire ends vs. changing the pinned `WireNet` class) is flagged as blocking, with a recommended default and the statement "filing proceeded because... withholding the issue pending a decision the issue exists to frame is the circularity D10 forbids." That's a defensible filing rationale, but it leaves the actual gate ("The maintainer must choose") with no described sign-off artifact — no assignee field, no linked decision record, nothing beyond "recorded" in a future PR. As written, an implementer could simply write "ratified: option (a)" into the tracking comment and proceed; the issue provides no way to distinguish a genuine maintainer decision from a self-serving one. Given the issue itself says the wrong choice "moves this task and TASK-0093 by weeks," this is a real cost-risk, not a formality.
**Recommendation:** require an explicit maintainer comment/label before implementation starts on Open Question 1, and make that a literal completion-criteria checkbox distinct from "recorded."

### 6. The honesty statement's acceptance test (P6) checks presence of a list, not correctness of the underlying claims

P6 requires the on-screen/report statement to "name exactly what the canvas checks... and what it cannot model until #341 lands." As specified, the evidence plan (§9) says P6 is judged by reading the statement — there's no cross-check that the *checked* set (wrong pin, wrong section, wrong tie-point column) is actually exercised by a passing test, or that the *unmodelled* set is actually absent from the implementation. A statement that lists the right words could ship without the checks it describes actually existing yet (TASK-0093, which owns the consistency check, is explicitly out of scope for this task) — meaning the "checks" half of the honesty statement is, at TASK-0092's close, describing work that hasn't landed. The issue doesn't flag this as a risk despite calling the statement "the one thing here that protects a student from a false belief."
**Recommendation:** tie P6 to TASK-0092's own actual scope precisely (i.e., the statement at this task's close should describe geometry/placement checks only, since TASK-0093's consistency check doesn't exist yet) or make explicit that the statement will read differently at TASK-0092-close vs. after TASK-0093 lands.

### 7. Feasibility: 2-week estimate (from the parent, #329) looks light against the enumerated scope

#401 itself carries no cost estimate, but its parent #329 assigns "TASK-0092 (2 wk)." The task's own §8 Method lists: a new headless model + geometry package with its own coverage floor; two new sealed `CircuitOp` records touching both the compiler-checked permits list and the string-keyed reader switch; a renderer and canvas wired through legacy `Viewport`/`UndoManager`; a new palette dimension's rows; an on-screen+report honesty statement; before/after measurement of per-edit cost, startup time, and palette row count; plus ten separate P1-P10 predictions each requiring its own test, several of them display-tagged and bidirectional (P3 explicitly warns about the trivial-pass failure mode of testing only one direction). That's a wide surface for two weeks, especially layered on top of the reconciliation gaps in findings #1-#3 above, which aren't accounted for in the estimate at all.
**Recommendation:** re-derive the estimate once findings #1-#4 are resolved, rather than carrying the figure from #329 unchanged.

## What's solid

- The factual/citation discipline is genuinely good: every quoted line number, file, and code fragment I checked against the live tree matched exactly (SimpleEditor.java = 5,852 lines; pom.xml:408 wording; CircuitOp.java:34-37 permits list; PaletteContractTest.java:44-48; the `WireNet.java:19-22` determinism comment).
- The explicit non-goals section (§4 Materials, "Explicitly NOT built here") cleanly excludes TASK-0093's consistency check and #84/#316's editor decomposition, which prevents obvious scope creep into those adjacent efforts.
- The failure-mode table (§7.11) — especially "never silently dropped" for unbound placements and the refusal to emit a contention finding the engine can't yet back up — reflects real, considered design discipline rather than hand-waving.
- The migration/compatibility invariant ("a file with no breadboard view opens exactly as before and saves byte-identically") is concrete and testable as stated.

## Verdict

**needs-rework.** The issue is well-researched and its individual technical claims check out, but it is not safe to hand to an implementer as-is: the dependency graph is incomplete by the issue's own admission (finding #3), two hard prerequisites don't exist yet to block against (finding #4), one acceptance criterion cites a test that doesn't do what's claimed (finding #1), and the undo design has an unaddressed mismatch with the actual `UndoManager` implementation (finding #2). These are fixable without re-scoping the task, but they should be fixed before execution starts, not discovered during it.
