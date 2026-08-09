# Issue #281: Shared session v1: snapshot broadcast over the Transport seam — markChanged capture, epoch-tagged last-wins restore via the load path (collab Stage 1b slice)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and the claim is: *a follower's screen should show the writer's
circuit*. Everything else — the epoch, the codec, the chaos schedules — is machinery
for that one sentence. The direction is right, and right for a reason the issue never
states: `markChanged` is a **complete** mutation choke point where `OpSink` is not.
`src/jls/edit` has 19 `markChanged()` call sites against ~9 `submitOp`/`submitOps`
sites, and five element dialogs plus `jls/elem/TruthTable.java` call `markChanged`
without ever touching an op. Broadcasting ops today would silently drop half of every
edit session. That — not "Stage 1 comes before Stage 2" — is the argument for state
broadcast, and it should be in §1, because it also dates the slice: when #167's
migration finishes, `markChanged` and the op stream carry the same information and
this data path becomes redundant plumbing rather than a deliberate baseline.

## 1. The layering rule already forbids the design as written

`ArchitectureRulesTest#replicationStackDependsDownwardOnly`
(`test/jls/ArchitectureRulesTest.java:226`) says no class in `jls.collab.session..`
may depend on `jls.edit..`. `CircuitSnapshot` **is** `jls.edit.CircuitSnapshot`.
§7.4 puts the sync service in `jls.collab.session`; §7.10 has it call
`CircuitSnapshot.capture`/`restore`. As specified the slice cannot go green, and
§14's `mvn verify` gate will catch it only after the design is built.

The fix is the better design, not a workaround. Two levels:

- **Minimum:** the session service traffics in `(epoch, byte[])` only. Capture and
  restore live editor-side behind a two-method interface the service is handed. This
  is also what makes the service survive #171 unchanged — Stage 2 hands it op bytes
  instead of snapshot bytes and nothing in the service moves.
- **Better:** move state↔bytes into the headless core. `Circuit.finishLoad` already
  takes `jls.core.TextMetrics`, not `Graphics` (`src/jls/Circuit.java:1300`), so a
  core `CircuitState` (deflate + restore through `load`/`finishLoad`) is AWT-free and
  legal under `coreDependsOnNoGuiClasses`; `jls.edit.CircuitSnapshot` shrinks to the
  Swing adapter supplying `SwingTextMetrics.forGraphics(g)`. Undo, the `.jls~`
  checkpoint writer, `-savetext`, #170's hostile-input caps and collab then share one
  code path instead of four. `CircuitSnapshot` lives in `jls.edit` for the historical
  reason that undo was its first customer, not because it belongs there.

## 2. The seam is already declared, and it is not a field in `EditWindow`

`docs/extension-points.md` catalogues `collab.op-observer` (`OpSink`, cardinality
many, "register, then observe every submit", status "typed now (#167 shipped; #171
consumes)"), and `OpExtensionPoints.OP_OBSERVER` names collaboration replication as
its consumer. #281 instead cuts a bespoke hook inside `markChanged`, which lives in
`EditWindow` — a ~4700-line private inner class of a 5852-line file. That pulls
against the #223 recorded decision.

Concrete alternative: widen the declared seam to a **circuit-changed** notification
(`edit.circuit-changed`, contract "the circuit's canonical state advanced"), and make
undo push, checkpoint write, and collab broadcast three contributions to one
catalogued point. `markChanged`'s body then reads as a fan-out instead of three
inlined behaviours, `ExtensionPointCatalogTest` sees the collab wiring, #170 gets one
enforcement point, and this slice never edits `EditWindow` internals. If that is too
much for one task, at least register the hook through `ExtensionRegistry` rather than
hard-coding a session reference into the inner class.

While there: the "a remote restore must not re-broadcast" invariant currently holds
only as an emergent consequence of floor control (non-holders don't send). Make it an
explicit applying-remote reentrancy guard; it is the invariant, and it must survive
the token changing hands.

## 3. The restore target is wrong — and fixing it deletes H2, P4, and half of §11

This is the deepest gap. §2 Obs. 1 has the writer capture the **top-level** circuit
via the `isImported()` walk. The issue never says what the follower restores *into*.
`finishDo` (`src/jls/edit/SimpleEditor.java:5728`) — the precedent it leans on —
restores exactly **one** circuit into **one** tab and then does per-tab repair:
`Editors.unregister`/`register`, re-link `SubCircuit` and `remapPins` when imported,
`refreshInImportMenu` on sibling tabs, `updateJumpStarts`, `updateNamesUsed`,
`sim.setCircuit`. Opening a subcircuit spawns a whole new `Editor` in `tabbedParent`
(`SimpleEditor.java:5175`), so a follower routinely holds several tabs over children
of the top-level circuit. Swapping the top level orphans every one of them: dead
`Circuit` objects, stale `Editors` registrations, undo stacks over discarded graphs,
and a simulator pointed at the old tree. No whole-tree swap machinery exists, and §8
does not plan one. "Undo already restores through the load path" is true only for the
single-circuit case.

**The reframing: a follower does not need an editor.** #169's own roster lists
"token gating + read-only affordance for non-holders" as a sibling slice; by
construction a follower has no token, so it is a viewer. Render received state through
a read-only view — a `Circuit` plus the existing paint path, no `EditWindow` state
machine, no undo stack, no `Editors` registration, no simulator binding — and the
whole hazard class evaporates: no in-flight gesture to cancel (H2, P4 gone), no
selection pointing at dead elements, no undo entry predating a remote edit, no tab
tree to re-link, and §11's "headless restore vs GUI EDT restore diverge" threat
shrinks to "does the view repaint". Taking the token becomes one seeding step: build
an editor from the last received snapshot. As a bonus the follower can browse
subcircuits independently of the writer, which the swap-the-editor design structurally
cannot allow.

**I am explicitly disregarding §14's "no changes outside the scope of §8" here, and
§12's "token gating is a sibling slice, not here."** As scoped, #281 ships a follower
that is fully editable and whose local edits are destroyed without warning by every
incoming snapshot — provable in tests, unusable as a demo, and precisely the "silent
dropped edit" #169's I3 forbids. The slice boundary is cut across the user-visible
capability rather than around it. Merge #281 with the read-only slice and ship
"followers see the writer's circuit, read-only" as one coherent thing.

## 4. Two smaller reframings

**One frame grammar, not three.** §7.6 mints a second wire grammar in
`jls.collab.session`, and the Open Questions ask "separate frame kind vs piggyback the
roster exchange". The real question is whether the session layer gets more than one
framing/dispatch layer at all. `Transport` is one opaque-frame channel; roster
anti-entropy, snapshot sync, presence, and #171's op frames must all multiplex over
it. Decide the demux now — one session envelope with a kind tag, one strict reader —
or three slices each invent framing and #170 hardens three readers. Generalize
`OpEnvelope`'s discipline; don't clone it.

**Put `stateHash()` in the frame, beside the epoch.** Near-zero cost, three wins:
(a) a follower whose hash already matches skips the restore entirely, which makes
duplicates free and demotes the coalescing question from correctness to taste;
(b) the sync indicator #169's panel slice wants and the research doc promises (§2's
"in sync" indicator, §5.6's "identical to Sam's copy") gets delivered here instead of
waiting for the panel; (c) it is the seed of anti-entropy — hash beacon between
snapshots, full payload only on mismatch — which is the only way this stops being
O(whole circuit) per gesture as the `riscv/` trajectory (#200–#202) grows circuits
past a few KB. Push-full-state is right for the 2–8-peer LAN target today; the hash
belongs in the frame from day one regardless.

## 5. Trajectory note

ARCHITECTURE.md is "the map a new contributor needs… describes HEAD", and it does not
mention `jls.collab` at all — ~8k lines across four packages, the largest new
subsystem in the fork, with four layering rules already enforced by
`ArchitectureRulesTest` (headless, downward-only, opaque transport, no reflection) and
none of them recorded in the map. Every collab slice cites the research doc instead,
which is a design under execution rather than a description of HEAD. #281 is the first
slice that puts collab code *inside the editor* — the first one a contributor reading
ARCHITECTURE.md would trip over. File the ARCHITECTURE.md collab section as the
adjacent work this slice discovers; it is a one-screen edit and this is the moment.

## Recommendation

Endorse the goal and the state-first data path. Land it as: opaque `(epoch, hash,
bytes)` over one session frame envelope; capture/restore behind a core-side state
codec so the session package stays legal and #171 reuses the service verbatim; the
hook registered through the extension-point catalog rather than nailed into
`EditWindow`; and the follower a read-only view, merged with the token-gating slice,
so H2/P4 never need to be tested because they cannot happen.
