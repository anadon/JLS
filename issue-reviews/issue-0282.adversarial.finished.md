# Issue #282: Editor gestures: migrate placement, wiring, and paste commits behind the OpSink seam via preview-then-commit (op layer #167)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of adversarial pass

This is an unusually well-grounded issue: every permalink, line-number
citation, and cross-reference I spot-checked against the current
checkout (HEAD `5311625`) resolved correctly, with the drift the issue
itself predicts in §11 ("`SimpleEditor` line numbers drift; re-derive
at pickup") — e.g. `commitPlacing` is now at `SimpleEditor.java:2859`
vs. the cited `2865-2879`, off by exactly the amount of intervening
edits. `docs/operation-layer.md`'s inventory rows, the 41 `@Test`
methods in `CircuitOpTest`, `DeleteGestureTest`/`MoveGestureTest`,
`ArchitectureRulesTest.collabLayersAreHeadless`, and the 14
`markChanged()` call sites in `SimpleEditor.java` all match exactly
what the issue claims. That baseline accuracy raises the bar for what
counts as a real finding below — most of what I went looking for
(fabricated evidence, stale citations, contradicted architecture) was
not there. The concerns below are about the *remedy* being narrower
than the *evidence* it cites, and about acceptance criteria that can
be satisfied while leaving the collab-visibility goal largely unmet.

## Findings, by severity

### 1. (High) The remedy for "Matching-JumpEnd creation" doesn't cover the code the issue cites as evidence for it

Observation §2 item 4 cites `SimpleEditor.java#L2553-L2561` (now
`2549-2560`) as proof this gesture is inline: `circuit.addElement(nel);
… markChanged();`. I read that block
(`SimpleEditor.java:2541-2562`, the `matchJump` `actionPerformed`
handler): it constructs the `JumpEnd`, **adds it to the live circuit**,
calls `setState(State.chosen)`, and calls `markChanged()` — all at
*menu-click* time, before the user has positioned or dropped it. The
element then follows the mouse in `chosen` state
(`SimpleEditor.java:3804-3819`) until a second, independent commit:
`commitPlacing()` (`2859-2880`), which calls `markChanged()` again.

Method §8's corresponding checklist item reads only "Matching-JumpEnd
drop (**same commit point**; op already validates jump-source rules)"
— i.e. it addresses the *drop*, not the *creation* step that
Observation item 4 actually cites. Two consequences:

- **P2 ("one `markChanged` per gesture") cannot hold for this gesture
  as scoped.** Today the create-then-drop sequence already produces
  two `markChanged()` calls — two separate undo snapshots
  (`markChanged` unconditionally calls `pushCopy()`,
  `SimpleEditor.java:5497-5506`) — for one logical user action. If the
  fix only wraps the drop in `OpSink.submitAll` and leaves the
  create-time `circuit.addElement`/`markChanged` at line ~2555/2560
  untouched, the gesture still ends with two undo entries and the
  create-time mutation is still fully inline, unreached by any op.
  Verify by hand today: popup → "Create Matching End", click to place,
  press Ctrl-Z once — the element does not disappear (it reverts only
  the placement), confirming the double-snapshot.
- **H1's "in-progress gesture state stays editor-local" premise is
  already false for this gesture** — the `JumpEnd` is inserted into
  the live `Circuit` object before any op would run, not held in
  gesture-local state the way a pending drag position is. Making this
  gesture actually preview-then-commit means also deferring
  `circuit.addElement(nel)` itself to the drop, which is materially
  more work than "same commit point as placement" implies, and further
  interacts with a real second gap: `cancelGesture()`
  (`SimpleEditor.java:5667-5716`, invoked by `undo()`/`redo()` to
  reach idle first) explicitly handles `startwire`/`drawire`,
  `moving`, and `placing`, but has **no branch for `State.chosen`** —
  and there is no Escape-key handler for `chosen` either (`endWire`,
  `SimpleEditor.java:1373-1387`, only checks `startwire`/`drawire`).
  Today this is masked because `undo()` discards the whole circuit via
  `finishDo`'s snapshot restore regardless; a preview-then-commit
  rework that keeps the pending `JumpEnd` editor-local would need to
  give `chosen` an explicit cancel path it doesn't have now.

**Recommendation:** amend §8 item 2 (or file a REPLAN comment) to
explicitly scope whether the create-time `circuit.addElement`/
`markChanged` is in or out of this task's remedy. If out of scope, say
so and correct P2's claim for this gesture; if in scope, add the
`chosen`-state cancel gap to §2's observations and §8's checklist,
since it's a prerequisite the issue doesn't currently list.

### 2. (Medium) The "documented fallback" escape hatch for connect-forming drops can satisfy the Definition of Done while leaving the stated collab goal unmet

The Abstract frames the whole task around collab visibility: "until
they route through `OpSink`, replication and precise undo cannot
observe element/wire creation." But §8's placement item and the Open
Questions section explicitly sanction *not* migrating exactly the
drops most likely to matter in practice — "a drop whose `connect()`
would form connections → composite plan **or documented inline
fallback**... Recommended default: fallback first, compose in a later
slice." H1's falsification criteria reinforces this: "if a gesture
cannot express even commit-time... keep the inline fallback... REPLAN
on #167 (documented fallback is the sanctioned outcome, not a
workaround)."

This is internally consistent and honestly flagged as a decision, not
hidden — but it means the Completion Criteria checklist ("Every
post-change prediction in §5 verified") can be satisfied by migrating
only the *unwired*-drop case for placement and moves, while every drop
that actually snaps into existing circuitry (arguably the common case
once a circuit has any wiring in it — duplicating or dropping a gate
next to existing nets) stays permanently inline behind a "documented
fallback," with no obligation in this issue to revisit it. The parent
feature #167's own I1 criterion ("no 'gesture still inline' row" at
close) can then be met by relabeling that row "fallback documented"
rather than "migrated" — technically closing both issues while the
mutation type the Abstract cares most about (connection-forming edits)
remains invisible to the collab seam.

**Recommendation:** before work starts, get an explicit call on
whether "fallback documented" is an acceptable *permanent* disposition
for connect-forming placement/move, or whether it must be revisited in
a follow-up task with an owner (the way `docs/operation-layer.md`
already tracks ordered-row edits and subcircuit import as named,
owned, deferred rows). Right now it risks becoming an un-owned,
permanently-deferred gap that nonetheless reads as "done."

### 3. (Medium) The `OpRejected`→inline-fallback recovery path is specified but not in the verification plan

§7.11 states the failure mode plainly: "Plan validation failure →
`OpRejected` → nothing applied... gesture then falls back to the
inline path under the snapshot-undo safety net." This is a real code
path every migrated gesture must implement (build a plan, submit it,
and on rejection re-run the legacy inline commit instead) — but none
of §5's Predictions (P1-P4) or §10's Falsification Criteria (H1/H2)
actually test *that* path for placement/wire-draw/paste. P1-P4 test
migrated-op-succeeds parity and single-undo-snapshot behavior; H1/H2
test whether a gesture can be expressed as a plan at all. Nothing
tests "plan submitted, validation rejects it, does the inline fallback
correctly complete the gesture with the same one-undo-snapshot
guarantee, and does the circuit end up byte-identical to what the pure
inline path would have produced." Given each gesture now carries two
independent code paths to the same end state, an untested seam between
them (e.g. partial preview cleanup before falling back) is exactly
where a silent divergence would hide.

**Recommendation:** add an explicit rejection-triggers-fallback test
per migrated gesture (or state why it's believed unreachable — e.g. if
plan validation can only fail on programmer error, not user input, say
so and cite the invariant that makes it true).

### 4. (Low) `part_of_feature` graph-cycle claim is correct but worth independently confirming when picking this up

The issue asserts "no cycle" between #282 and #167 based on #167's
`requires_tasks` listing #282 with no back-edge. I fetched #167 and
confirmed: `requires_tasks: [282, 283]`, `blocked_by: [165, 166]` (both
closed), and no edge back onto #282. This holds today, but it's worth
noting the check is manual/prose, not tool-enforced — if #167's
machine block drifts before #282 is picked up, nothing catches it
automatically. Not a defect in this issue, just a fragility inherited
from #167's format.

## What's solid (no further action needed)

- All cited files, line ranges (with acknowledged drift), test names,
  and op-kind names exist and match; this is a rare case where the
  "Observations" section is fully verifiable rather than asserted.
- The scope boundary against #84 (SimpleEditor decomposition) and #91
  (gesture test harness) is clean — this issue stays within static,
  Swing-free plan builders and doesn't reach into dispatcher rework.
- §7.1/§7.2 correctly state no save-format, CLI, or export surface
  changes — consistent with `ARCHITECTURE.md`'s save/load pipeline
  description; nothing in the plan touches `Circuit.save`/`load`.
- The rejection-leaves-circuit-unchanged and byte-exact-inverse
  invariants this task depends on are real, existing, and tested
  (`CircuitOpTest.rejectionsLeaveTheCircuitUnchanged`,
  `test/jls/collab/op/CircuitOpTest.java:761`), so the foundation this
  task builds on is sound.
- Threats to Validity (§11) already anticipates the line-drift issue
  and headless-vs-real-mouse divergence; this is better self-awareness
  than most issues in this tracker.
