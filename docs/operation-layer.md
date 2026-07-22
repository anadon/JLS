# The operation layer (issue #167, collab Stage 0b)

Every editor mutation is being migrated from inline state-machine code
to a closed vocabulary of validated, invertible, serializable commands
(`jls.collab.op.CircuitOp`) applied through one entry point
(`jls.collab.op.OpSink`). The collaboration program (#163) replicates
exactly this vocabulary; precise undo, per-peer attribution, and
targeted revert all build on it. During the migration the user-facing
undo mechanism is unchanged: `SimpleEditor.markChanged()` still
snapshots the whole circuit (#18), and `OpSink.submit` runs
validate → apply → `markChanged()`.

## Contract

- `apply(Circuit, Graphics)` validates first and mutates only if the
  whole op is valid — a rejected op leaves the circuit byte-identical
  (`CircuitOpTest.rejectionsLeaveTheCircuitUnchanged`).
- `invert(Circuit before)` is computed against the pre-apply circuit;
  apply-then-inverse returns the canonical save (#166) to its prior
  bytes, on live and on save/load-restored circuits alike.
- `save(PrintWriter)` writes the save-format idiom; `CircuitOpReader`
  is its exact inverse and is strict: unknown kinds, unknown fields,
  malformed values, and oversized input are rejections, never repairs
  (this grammar is the future network surface — #170 hardens it
  further).
- Ops address elements by stable id (#165), never by object reference.

Serialized form:

```
OP <kind>
 String id "<replica:counter>"     (repeatable where the kind takes a group)
 String name "<escaped>"           (AttachProbe)
 String block "<escaped>"          (AddElements, repeatable: one whole
                                    element save block per line)
 int dx <n>  /  int dy <n>         (MoveElements)
 int cw <0|1>                      (RotateElement)
END
```

## Element transplant

`ElementBlocks` is the transplant helper: it serializes one element as
the exact bytes the element's own `save` method writes (canonical
`'\n'` line endings, #166), and loads such a block back through the
exact reader the file loader uses (`Circuit.loadElement`), against a
scratch circuit so the target's load bookkeeping is never touched. An
added element is therefore indistinguishable from a loaded one, and
`AddElements`/`RemoveElements` are exact mutual inverses:

- `AddElements(blocks)` validates atomically against the editor's paste
  rules (blocks must load; stable ids must be new; names must not
  collide within the op or with the circuit; a jump start's name must
  be free; a jump end must have a source already present or arriving in
  the same op) before anything is added. Wire, wire-end, and
  subcircuit blocks are rejected - those travel through their own op
  kinds - so an added element always arrives unwired.
- `RemoveElements(ids)` computes a true, byte-exact inverse (the
  removed elements' blocks), not a snapshot fallback. It rejects
  elements with wires attached to their puts (detaching would mutate
  wire state the inverse could not restore) and requires a jump start
  to bring every one of its jump ends along, because the editor's
  removal cascades that way. Wired selections stay on the inline
  delete path, under the snapshot-undo safety net, until the wiring
  vocabulary lands.

## Mutation-site inventory (§7 step 1)

The audit of every `markChanged()` call site in `SimpleEditor` (plus
the dialog-commit paths), each mapped to an op kind. "Migrated" means
the gesture now goes through `OpSink.submit`; line numbers are as of
the migration commit.

| Gesture (commit point) | Op kind | Status |
| --- | --- | --- |
| Watch toggle, ctrl-W key | `ToggleWatched` | **migrated** |
| Watch toggle, context menu | `ToggleWatched` | **migrated** |
| Rotate CW, context menu | `RotateElement(cw)` | **migrated** |
| Rotate CCW, context menu | `RotateElement(ccw)` | **migrated** |
| Flip, context menu | `FlipElement` | **migrated** |
| Probe attach/remove, context menu | `AttachProbe` / `RemoveProbe` (name prompt stays in the gesture; ops are pure data) | **migrated** |
| Move-selection commit (mouse release) | `MoveElements` | op implemented + tested, and wired into the keyboard nudge (issue #75, `submitOp(new MoveElements(...))` at `SimpleEditor.java:3254`); the mouse-release drag commit is still inline (live drag must become preview-then-commit) |
| Placement drop (fixPosition + connect) | `AddElements` (+ implicit wiring) | op implemented + tested for the unwired case; gesture still inline (the drop's `connect()` needs the wiring vocabulary, and placement must become preview-then-commit) |
| Matching JumpEnd creation, context menu | `AddElements` | op implemented + tested (jump-source validation included); gesture still inline (the created end stays mouse-attached in `chosen` state, so the commit point is the later drop) |
| Delete selection | `RemoveElements` | op implemented + tested with a **true inverse** (the removed elements' blocks) for unwired selections; wired selections stay inline until the wiring vocabulary lands (#167 §9's fallback narrowed to just that case). Gesture still inline |
| Paste | `AddElements` | op machinery in place (multi-block add with paste's name/jump validation); gesture still inline (pasted wires need the wiring vocabulary) |
| Wire-attach finish (mouse) | `AddWire` | deferred — wiring gestures become commit-time ops |
| Wire-draw cancel (right button / end-wire key, two sites) | none — gesture-local cleanup of the in-progress wire; these `markChanged` calls compensate for live mutation and disappear when wiring is commit-time | deferred |
| Quick attribute edit commit (`quickReset`) | `SetElementConfig` (element-state replace) | op implemented (a record in the sealed `CircuitOp permits` list, `CircuitOp.java:37`, with its reader case at `CircuitOpReader.java:157`); gesture still inline — dialog commits mutate in place today |
| Element dialog commits (all element edit dialogs) | `SetElementConfig` | op implemented; gesture still inline — same |
| Ordered-row edits (state machine, truth table, sig-gen programs) | `EditOrderedRows` | deferred — Stage 2 sequence semantics (#163) |
| Subcircuit import | `ImportSubcircuit` | deferred |

H1 refinement: the audit confirms the ~10-kind estimate, with two
notes — (1) the wire-draw *cancel* sites are not ops at all (they undo
gesture-local live mutation), and (2) probe toggling splits into an
attach/remove pair so each op stays pure data and the pair are exact
mutual inverses.

## Layering

`jls.collab.op` depends on `jls` and `jls.elem` and on AWT (`Graphics`
for geometry recomputation), never on Swing — enforced zero-tolerance
by `ArchitectureRulesTest.collabLayersAreHeadless` (#163 dependency
rule 2; only the future `jls.collab.ui` may touch Swing).

## What lands next

1. The wiring vocabulary (`AddWire`/`RemoveWire` over wire ends,
   segments, and attachments), which unlocks the wired cases of
   delete/paste and the wire-drawing gesture's commit-time op.
2. Preview-then-commit for the move, placement, and wiring gestures,
   anchored by the #91 gesture harness (`EditorGestureTest`) - the
   step that migrates the gestures whose ops already exist
   (`MoveElements`, `AddElements`, `RemoveElements`).
3. Wiring the dialog-commit gestures onto `SetElementConfig`, whose op
   already exists; the commit paths mutate in place until then.
4. Op-inverse (precise) undo activation is explicitly *not* this
   stage: snapshot undo stays user-facing until Stage 2 (#163).
