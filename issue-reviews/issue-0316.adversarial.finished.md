# Issue #316: FEAT-008 (RESIDUAL): the editor's nine-state mouse machine is assertable without a display, and jls.edit carries a coverage floor
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-grounded: every line-number and file-content citation I spot-checked against the working tree at HEAD (`d6bc8dd`, one commit past the issue's pinned `evidence_commit: 2d0ca9d`) matched exactly — `SimpleEditor.java` is 5,852 lines with `enum State` at :770, `pom.xml:408-409` carries the prose exemption verbatim, `ci.yml:380` carries the `UNVERIFIED-PLACEHOLDER` digest, `CircuitOp.apply(Circuit, Graphics)` still takes a `Graphics`, `OpSink.submit`/`submitAll` are at the cited lines, `test/jls/ui/` has 34 files, and `ElementRegistry.ALL` has exactly 35 entries. That rigor is real and should be credited. But the issue has three defects severe enough to block a confident start: an unresolved, self-acknowledged design contradiction on its own critical-path deliverable, an acceptance criterion that conflicts with how the codebase's existing dialog-coverage mechanism actually works, and a completion-criteria checkbox that is already vacuously satisfied before any work begins.

## Findings, most severe first

### 1. [HIGH] The core deliverable (TASK-0020) has two incompatible, unresolved designs, and this issue's own §7 says that requires a REPLAN here that has not happened

Issue §3 "Provides" specifies: "A public interaction-state contract in `jls.edit` with per-event handlers plus `enter`/`exit` hooks... The concrete state classes stay package-private." That is the GoF-state design, also restated in #84 §7.4/§7.5.

The 2026-08-08 16:44 comment on this issue reports that TASK-0020 was filed as #441, closed as a duplicate of #84, and that #441's absorbed contract instead specifies "a behaviourless `enum InteractionState` plus a `public final class MouseMachine` returning a `Transition` record" — a *public* class, not package-private states, and no per-state `enter`/`exit` objects at all. The same comment states plainly: "This feature's §3 'Provides' states the GoF form as the feature-level contract, choosing option (b) or (c) is a **contract deviation requiring a `REPLAN:` here**... It blocks TASK-0020's execution."

As of the fetch for this review (issue `updated_at: 2026-08-08T18:14:08Z`, same day), no such REPLAN comment exists on #316. The issue's own escalation mechanism has fired and not been answered. Starting TASK-0020 today means picking a design the parent feature has explicitly flagged as unresolved and contract-breaking either way.

**Recommendation:** Do not schedule TASK-0020 (#84) until a REPLAN comment on #316 picks one design and updates §3 "Provides" to match. This is a blocking prerequisite the issue itself names but has not discharged.

### 2. [HIGH] The dialog-count acceptance criterion contradicts the existing, working coverage mechanism it says it will extend

Completion Criteria: "The dialog count in every test derives from `ElementRegistry`, and `git grep -n \"23 element dialogs\"` finds no surviving hard-coded count." §1 also frames the derived count as "35 types at `2d0ca9d`, `:38-77`."

I read `test/jls/ui/DialogCoverageRatchetTest.java` (the exact mechanism §5 criterion 3 says this feature extends). It does **not**, and structurally cannot, use a 1:1 count against `ElementRegistry`'s 35 registered element types: it maintains a `SWEPT` set of ~26 element classes plus a separate `REPRESENTED` map of family exemptions (e.g. `Gate` is "abstract; its dialog is swept via AndGate, DelayGate, and Extend" — six gate types share one dialog; `Pin` covers `InputPin`/`OutputPin`; some registered types, e.g. `WireEnd`, have no create-dialog at all). Deriving "the count" from `ElementRegistry.java`'s 35-entry list, as the issue's completion criteria literally demand, would either break this family-representative pattern or require the criterion to mean something looser than what it says. The issue never reconciles the two.

**Recommendation:** Rewrite the criterion to name the actual invariant `DialogCoverageRatchetTest` enforces (every `ElementDialog` subclass swept, directly or via a documented exemption) rather than a literal count derived from `ElementRegistry`, which is not the unit dialogs are keyed on.

### 3. [MEDIUM] A Definition-of-Done checkbox is already trivially satisfied, so it verifies nothing

`git grep -n "23 element dialogs"` over the current tree returns zero hits — I ran it. That means this checkbox in the Completion Criteria is checked off for free, before TASK-0021 or TASK-0019 do any work. It provides no actual signal that a count is *derived* rather than hard-coded; a future author could hard-code a different literal (`"35 element dialogs"`, or the bare integer with no comment) and satisfy both the letter of this check and defeat its intent.

**Recommendation:** Replace the string-absence check with a positive assertion (e.g. a test that fails if the swept-set size and `ElementRegistry`'s applicable subset diverge), not a grep for a specific stale phrase.

### 4. [MEDIUM] The formal "behavior-preserving" claim is unfalsifiable in practice, and the issue's own dependency (#84) admits the gap

§3 states the extraction must satisfy `∀ s ∈ S, ∀ g, ∀ c : δ'(s,g,c) = δ(s,g,c)` — an equality over an unbounded domain of gestures and circuits. The only verification method offered (§5 criterion 1, and #84's H1) is "the existing gesture-test suite stays green." #84 §11 "Threats to Validity" itself says: "states with thin coverage (`option`, `selecting`) need their transition tests written *before* extraction, not after" — i.e., the existing suite is known-insufficient for exactly two of the nine states. As written, the acceptance test (suite green) can pass while the universally-quantified equality silently fails for `option`/`selecting`, the two states the issue's own prerequisite task already flags as weak.

**Recommendation:** Make writing thin-coverage transition tests for `option` and `selecting` an explicit, ordered precondition of TASK-0020's extraction step for those states (not a "rides along" note), and drop the ∀-circuits framing from the acceptance-facing text — it reads as a proof obligation the plan cannot actually discharge.

### 5. [MEDIUM] Scope bundling and feasibility: this "feature" fans out to at least 8 other tracker issues and two large, wholly-unstarted prerequisite features

The six `planned_tasks` are shared with FEAT-007 (#317), FEAT-053 (#369), FEAT-032/034 (#455), and FEAT-049/043/029 (#482) — a console-pane/transcript I/O subsystem (`src/jls/io/` does not exist in the tree today, confirmed) is bundled in via TASK-0069 alongside a state-machine refactor, a coverage floor, dialog validation, and a CI supply-chain digest pin. `blocked_by: [317, 337]` names two *other* wide-open `tier:feature` issues, both of which I fetched and are themselves multi-week, zero-tasks-filed efforts (#337's own planned tasks are "not filed"; #317's four tasks are also "not filed"). So #316 cannot meaningfully close until at least ~7-13 additional maintainer-weeks of unstarted prerequisite work (FEAT-015 band 4-7 mw, FEAT-007 band 3-6 mw) lands first.

The issue's own Cost section admits its internal arithmetic doesn't reconcile: task rows sum to 10.5 weeks against a stated band of "12-20 mw," and rather than resolving which number governs, the issue prints both and calls the band "superseded" — an explicit, unresolved estimate.

**Recommendation:** Split the CI-digest pin (TASK-0018) and the console-pane transcript (TASK-0069) out to their true owners (FEAT-007, FEAT-032) rather than carrying them here as "shared," and get a single reconciled cost figure before treating this as schedulable.

### 6. [LOW] The multi-issue dependency mesh this issue participates in is demonstrably unstable, even within the review window

The three comments on #316 are dated 2026-08-04 and twice on 2026-08-08 (the fetch date), and two of the three are themselves *corrections* to the graph: one narrows an over-broad `ordering_after` edge that had wrongly gated an unrelated feature's AC-1/AC-3/AC-4 behind #316, and one documents a roster reconciliation after a duplicate-task merge produced the design collision in Finding 1. This is direct evidence that the cross-issue bookkeeping scheme (`blocked_by`/`blocks`/`serves_capstones`/`related` mirrored across dozens of issues) is still accumulating defects during the planning phase, before any code has been touched. A contributor picking up #316 must currently trust the freshness of at least #84, #317, #337, #440, #441, #470, and #571 to not have drifted since this review.

**Recommendation:** Treat the dependency graph as provisional until it goes a full review pass with zero corrections; do not let a downstream issue inherit `blocked_by: [316]` at feature scope (the issue's own 2026-08-08 comment already calls this pattern out as a recurring mistake).

## What is solid (no action needed)

- Evidence citations against the actual repository are accurate and current, not stale relative to the pinned commit — a real strength given how fast `SimpleEditor.java` churns (five re-baselines noted in #84, and the file grew again between the issue's evidence commit and HEAD without breaking any cited line reference I checked).
- The scope boundary correctly excludes `Graphics` removal from `CircuitOp.apply` (owns it to #337) and correctly excludes whole-circuit-undo replacement — both boundaries are consistent with the cited sibling issues' own stated scope.
- The global invariants (byte-identical save, EDT-only, AWT-free headless kernel, floor-never-lowers) are concrete and checkable, and match conventions actually recorded in `pom.xml:395-418`.
- The GitHub sub-issue link (#316 → #84) is correctly wired and confirmed via `get_sub_issues`.

## Bottom line

The underlying goal — extract the mouse machine, floor `jls.edit`, close the two Wayland-rig placeholders — is real, well-evidenced against the live codebase, and worth doing. But the issue cannot be picked up as written: its own escalation trail shows the critical-path task's design is contradictory and unresolved, one of its acceptance criteria conflicts with the coverage mechanism it claims to extend, and its Definition of Done contains at least one already-satisfied no-op check. These need to be fixed in the issue (or in a REPLAN comment) before implementation starts.
