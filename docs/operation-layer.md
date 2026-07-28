# The operation layer (issue #167, collab Stage 0b)

Every editor mutation is being migrated from inline state-machine code
to a closed vocabulary of validated, invertible, serializable commands
(`jls.collab.op.CircuitOp`) applied through one entry point
(`jls.collab.op.OpSink`). The collaboration program (#163) replicates
exactly this vocabulary; precise undo, per-peer attribution, and
targeted revert all build on it. During the migration the user-facing
undo mechanism is unchanged: `SimpleEditor.markChanged()` still
snapshots the whole circuit (#18), and `OpSink.submit` runs
validate → apply → `markChanged()`. A multi-op gesture goes through
`OpSink.submitAll`, which the editor overrides to apply every op and
then `markChanged()` exactly once - one gesture, one undo snapshot,
however many ops express it.

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
 String id "<replica:counter>"     (repeatable where the kind takes a group;
                                    AddWire: the attachment anchors, in
                                    stable-id order - index i is local id i
                                    inside the blocks)
 String name "<escaped>"           (AttachProbe)
 String block "<escaped>"          (AddElements/AddWire, repeatable: one
                                    whole element or wire-end save block
                                    per line)
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
  removal cascades that way. A wired selection is expressed as one
  `RemoveWire` per attached net followed by a `RemoveElements` over
  the then-unwired elements - the kinds compose, and each keeps its
  exact inverse.

## Wire-net transplant

`NetBlocks` is the wiring sibling of `ElementBlocks`: it serializes a
whole wire net as the exact per-end blocks `WireEnd.save` writes, and
reconstructs a net by replaying the file loader's algorithm (end
construction, wire creation order, put attachment, probes, net
partition), so an added net is indistinguishable from a loaded one. A
wire-end block references other elements by the save format's
file-local `int` ids, which mean nothing outside a whole-file save, so
a net serialization renumbers locally: attachment anchors get local
ids `0..k-1` and the net's ends `k..k+n-1`, both in stable-id order,
and the anchors travel alongside the blocks as stable ids (#165).
`AddWire`/`RemoveWire` are exact mutual inverses at net granularity:

- `AddWire(attach, blocks)` validates atomically and strictly: the
  blocks must be exactly what a save writes (the parser rejects
  anything `WireEnd.save` could not have produced), form exactly one
  connected net whose ends' stable ids are fresh, list every wire and
  probe from both ends, agree on the net's tri-state flag, and attach
  only to existing, unattached puts of the anchors carried. A
  tri-state net re-arms the tri-state property of the input pins it
  drives, mirroring the editor's `connect()`.
- `RemoveWire(id)` addresses the net by any one of its wire ends and
  removes the whole net - ends, segments, probes - detaching every put
  exactly as the editor's inline wire deletion does (including
  clearing the tri-state property of elements a tri-state net drove).
  Its inverse carries the net's serialized blocks: a true, byte-exact
  restore. Net granularity is deliberate: a gesture that deletes one
  segment of a larger net re-partitions the remainder, and travels as
  `RemoveWire` plus one `AddWire` per surviving connected component,
  built by `AddWire.survivors(ends, keptWires, removedAnchors)`: the
  wire-end blocks in `WireEnd.save`'s exact format filtered to the
  surviving subgraph (kept wires and their probes only, attachments to
  removed elements dropped, the tri-state flag recomputed per
  component the way `WireNet.makeNet` does), written without touching
  the ends' save-time ids (HDL export consumes those).

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
| Move-selection commit (mouse release) | `MoveElements` | **migrated** for a pure relocation, and already the keyboard nudge (issue #75). `SimpleEditor.moveSelectionPlan` returns a one-op `MoveElements` plan when the drop is a pure relocation - every selected element is a plain, editable element carrying no attached put, and no selected element's post-move put lands on a non-selected wire end or put; the live drag is the preview, then the pre-drag positions are restored and the op is the mutation of record, batched through `OpSink.submitAll` as one undo snapshot (`MoveGestureTest`). Any drop that would form a connection, drag a wire end, or induce colinear cleanup (wired elements, wires/wire ends, subcircuits) keeps the inline `fixPosition` + `connect()` + `removeCoLinear()` commit verbatim |
| Placement drop (fixPosition + connect) | `AddElements` (+ implicit wiring) | op implemented + tested for the unwired case; gesture still inline (the drop's `connect()` needs the wiring vocabulary, and placement must become preview-then-commit) |
| Matching JumpEnd creation, context menu | `AddElements` | op implemented + tested (jump-source validation included); gesture still inline (the created end stays mouse-attached in `chosen` state, so the commit point is the later drop) |
| Delete selection (delete key, Edit menu, popup Delete, and CUT's removal half) | `RemoveWire` per attached net + `AddWire` per surviving component + `RemoveElements` | **migrated**: `SimpleEditor.deleteSelectionPlan` maps the selection to an op plan (jump starts expanded with their jump ends; per affected net one `RemoveWire` of the whole net plus, when the selection only clips it, one `AddWire.survivors` per surviving connected component; then `RemoveElements`), submitted as one batch through `OpSink.submitAll` so the gesture stays a single undo snapshot (`DeleteGestureTest`). Only subcircuits, in-progress wiring, and uneditable cascades still take the inline fallback |
| Delete wire net / detach (popup delete on a wire) | `RemoveWire` (+ `AddWire` survivors) | **migrated** via the same plan builder: a wire selection covering its whole net travels as `RemoveWire` (`DeleteGestureTest.wireOnlySelectionPlansToARemoveWire`); a segment delete that clips a larger net travels as `RemoveWire` plus one survivor `AddWire` per remaining component (`DeleteGestureTest.middleSegmentDeletePlansTwoSurvivorAddWires`) |
| Paste | `AddElements` + `AddWire` | op machinery in place (multi-block add with paste's name/jump validation; pasted nets travel as `AddWire`); gesture still inline |
| Wire-attach finish (mouse) | `AddWire` | op implemented + tested at net granularity (a commit that extends an existing net travels as `RemoveWire` of the old net plus `AddWire` of the merged one); gesture still inline — wiring gestures become commit-time ops |
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

1. Preview-then-commit for the placement and wiring gestures,
   anchored by the #91 gesture harness (`EditorGestureTest`) - the
   step that migrates the remaining gestures whose ops already exist
   (`AddElements`, `AddWire`). The move-selection drag commit is done
   for pure relocations (`MoveElements`, `moveSelectionPlan`); the
   connect-forming and wire-end-dragging drops remain inline until the
   commit-time wiring composition lands.
2. Wiring the dialog-commit gestures onto `SetElementConfig`, whose op
   already exists; the commit paths mutate in place until then.
3. Op-inverse (precise) undo activation is explicitly *not* this
   stage: snapshot undo stays user-facing until Stage 2 (#163).
