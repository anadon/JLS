# FEAT-015 - The headless, programmatic `CircuitOp` layer

**Status:** proposed | **Cost:** 4-7 mw | **Owner program:** P12 |
**Spine rank:** S4, S13

## Capability delivered

Every mutation of a circuit is an op: validated, invertible, serializable, and
applicable with no windowing system present. That makes three things possible
at once that are today three separate hacks - replaying another person's edit,
scripting a circuit into existence from a program, and running an import or a
grading pass on a headless machine. It also closes the vocabulary, which is the
precondition for ever trusting an op that arrived over a network.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | replication replays ops; ops that need a `Graphics` cannot be replayed headlessly, and the vocabulary must be closed before it can be made total under concurrency |
| CAP-06 | required | grading is headless and must construct and mutate circuits without a `Graphics` |
| CAP-08 | required | import constructs a circuit from a program and must not need a `Graphics` |
| CAP-04 | required | gesture replay and the headless breadboard path both need mutation without a `Graphics` |
| CAP-05 | beneficial | the export path and its fixtures must run without a `Graphics` |
| CAP-02 | beneficial | building a ~580-element machine by hand is what kill criterion K6 counts revisions against |
| CAP-03 | beneficial | drawing a ternary datapath by hand is what K6 counts revisions against |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | **None.** Ops already address elements by stable id, and the text-metrics abstraction the ops need already exists at HEAD. This is the highest-scoring spine row that gates on nothing except its own migration (`10-capstone-plan.md` §2.1, S4). |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0037 | Headless op application and the complete op vocabulary | Replaces `Graphics` with `TextMetrics` at the op boundary and closes the remaining gestures |
| TASK-0038 | Programmatic circuit construction verbs | Turns the closed vocabulary into a documented, tested way to build a circuit from a program |

## Acceptance criteria

1. `CircuitOp.apply` takes no `java.awt.Graphics`. A test constructs, mutates
   and saves a circuit in a JVM started with `-Djava.awt.headless=true` and no
   display, and the resulting canonical save matches the GUI path's byte for
   byte.
2. The op vocabulary is closed over the editor's mutating gestures: every
   `markChanged()` call site in `SimpleEditor` is either reached through
   `OpSink` or is named in a committed, reviewed deferral list with a reason.
3. Placement drop, paste, wire-attach finish and dialog commit are ops.
4. For every op kind, `apply` then `invert().apply` returns the canonical save
   to its prior bytes, on live and on save/load-restored circuits alike
   (the existing contract, extended to the new kinds).
5. A documented verb set builds a nontrivial circuit from a program, with a
   test that asserts the produced circuit against a committed golden - and the
   emit-text-and-reparse idiom is gone from every in-tree generative path.
6. The op grammar remains strict: unknown kinds, unknown fields, malformed
   values and oversized input are rejections, never repairs.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b) | closes - this feature is the completion of #167's migration plus its headless half |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | depends on - #167's parent; replication consumes exactly this vocabulary |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | overlaps - the remaining gestures live inside the machine #84 owns; #84 closes under FEAT-008, not here |

## Design notes

Most of this has landed. `jls.collab.op` exists with 11 permitted op kinds
(`src/jls/collab/op/CircuitOp.java:35-37`), `OpSink` is the single entry point,
op serialization and inverses are contracted and tested
(`docs/operation-layer.md`). Two things remain.

First, **the `Graphics` parameter**. All eleven op classes and the interface
itself still take `java.awt.Graphics`
(`src/jls/collab/op/CircuitOp.java:51` and the eleven `apply` overrides). The
replacement abstraction already exists in core: `jls.core.TextMetrics`
(`src/jls/core/TextMetrics.java:19`), whose own javadoc states the intent -
element layout takes a `TextMetrics` and the GUI supplies a
`FontMetrics`-backed implementation at the call site. This is a mechanical
substitution against an already-designed seam, which is why the band is weeks
and not months.

Second, **the remaining gestures**. `SimpleEditor` still has 14 `markChanged()`
sites and the file has grown to 5,852 lines. Both #84's "4,119 lines" and
#167's "4,477 lines / 17 sites" are stale; cite the measured figures, not the
issue text. The gestures the registry names as outstanding - placement drop,
paste, wire-attach finish, dialog commits - are the ones #167 §9 predicted
would need commit-time modeling rather than per-gesture modeling.

The construction verbs are decision D5's replacement mechanism: `riscv/` is to
be deleted, its programmatic word-level construction *approach* survives, and
whatever replaces it must be first-class, in-tree and tested (`BRIEF.md` §0.1).
This is that replacement, and it is deliberately not a public API surface -
`10-capstone-plan.md` §2.4 measures that each capstone needs *internal*
programmatic construction, not `jls.api`.

## Risks

- **Scope bleed into FEAT-008.** #167 §11 draws the boundary: this feature owns
  the mutation vocabulary and entry point; the mouse machine and the
  `source ==` dispatcher are FEAT-008's. Moves that shrink FEAT-008's surface
  are welcome but must not be required here, or a 4-7 mw feature is gated
  behind a 12-20 mw one.
- **Headless parity that is only model-deep.** Tests that drive the model but
  not the real gesture path can pass while the GUI path diverges. At least
  placement, move and wire gestures need anchoring through the UI harness
  (TASK-0021, FEAT-008) at the event level.
- **Deferral list abuse.** Criterion 2 permits a deferral list precisely so it
  cannot be quietly skipped; a list that grows instead of shrinking means the
  vocabulary is not closing and CAP-01 cannot proceed.

## Evidence

- Op layer at HEAD: `src/jls/collab/op/` (11 kinds + `OpSink` +
  `CircuitOpReader`); `src/jls/collab/op/CircuitOp.java:35-37`, `:51`.
- The `Graphics` coupling, all eleven sites:
  `AddElements.java:51`, `AddWire.java:111`, `AttachProbe.java:22`,
  `FlipElement.java:20`, `MoveElements.java:40`, `RemoveElements.java:54`,
  `RemoveProbe.java:19`, `RemoveWire.java:46`, `RotateElement.java:24`,
  `SetElementConfig.java:55,98`, `ToggleWatched.java:20`.
- The replacement abstraction already in core: `src/jls/core/TextMetrics.java:1-19`.
- Editor size and remaining sites: `src/jls/edit/SimpleEditor.java` is 5,852
  lines with 14 `markChanged();` sites, measured at `addc6c5` - both #84 and
  #167 quote smaller, stale figures.
- No construction API: `BRIEF.md` §7 - all six in-tree generative paths emit
  save-format text and re-parse it.
- Cost: `10-capstone-plan.md` §2.1 rows S4 (1-2 wk, score 2.67) and S13
  (3-5 wk); merged per the registry's deduplication record item 4.
