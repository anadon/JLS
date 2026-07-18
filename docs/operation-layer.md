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
 int dx <n>  /  int dy <n>         (MoveElements)
 int cw <0|1>                      (RotateElement)
END
```

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
| Move-selection commit (mouse release) | `MoveElements` | op implemented + tested; gesture still inline (live drag must become preview-then-commit) |
| Placement drop (fixPosition + connect) | `AddElement` (+ implicit wiring) | deferred — needs the element-transplant machinery |
| Matching JumpEnd creation, context menu | `AddElement` | deferred — same machinery |
| Delete selection | `RemoveElements` | deferred — inverse must restore elements *and* their wires; snapshot-fallback inverse is the sanctioned interim (#167 §9) |
| Paste | `PasteGroup` | deferred — same restore machinery as delete's inverse |
| Wire-attach finish (mouse) | `AddWire` | deferred — wiring gestures become commit-time ops |
| Wire-draw cancel (right button / end-wire key, two sites) | none — gesture-local cleanup of the in-progress wire; these `markChanged` calls compensate for live mutation and disappear when wiring is commit-time | deferred |
| Quick attribute edit commit (`quickReset`) | `SetAttributes` (element-state replace) | deferred — dialog commits mutate in place today |
| Element dialog commits (all element edit dialogs) | `SetAttributes` | deferred — same |
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

1. The element-transplant helper (serialize one element, load it into
   a target circuit), which unlocks `AddElement`, `RemoveElements`
   with true inverses, and `PasteGroup`.
2. Preview-then-commit for the move and wiring gestures, anchored by
   the #91 gesture harness (`EditorGestureTest`).
3. `SetAttributes` from the dialog-commit paths.
4. Op-inverse (precise) undo activation is explicitly *not* this
   stage: snapshot undo stays user-facing until Stage 2 (#163).
