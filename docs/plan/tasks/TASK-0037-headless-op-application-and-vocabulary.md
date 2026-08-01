# TASK-0037 - Headless op application and the complete op vocabulary

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

Op application stops taking a drawing context, `jls.collab` enters the
headless-core ratchet, and the four gestures whose ops already exist but whose
commit paths are still inline become ops - closing the vocabulary.

1. **The signature.** `CircuitOp.apply(Circuit, Graphics)`
   (`src/jls/collab/op/CircuitOp.java:51`) becomes
   `apply(Circuit, jls.core.TextMetrics)`. The nullable contract is preserved
   exactly: a null `TextMetrics` means "skip sizing", which is what a null
   `Graphics` meant (`src/jls/edit/SwingTextMetrics.java:56-70`), so headless
   application produces the same element geometry - and therefore the same saved
   bytes - it produces today.
2. **The four AWT call sites, deleted.** `SwingTextMetrics.forGraphics(g)` is
   called at `src/jls/collab/op/AddElements.java:61`,
   `SetElementConfig.java:66` and `:120`, `FlipElement.java:26`, and
   `RotateElement.java:31`. Each becomes the passed-in `TextMetrics` directly.
   The `import java.awt.Graphics` line is removed from all eleven op files and
   from `CircuitOp` itself; the `import jls.edit.SwingTextMetrics` line from the
   four.
3. **The editor supplies the metrics.** `SimpleEditor`'s anonymous `OpSink`
   (`src/jls/edit/SimpleEditor.java:5547-5570`) passes
   `SwingTextMetrics.of(getGraphics())` in `submit` and once per `submitAll`
   batch instead of `getGraphics()` per op.
4. **The ratchet.** `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES`
   (`test/jls/HeadlessCoreRatchetTest.java:74-79`) gains `"src/jls/collab/"`,
   with `BASELINE` staying `Set.of()` (`:90`) - zero tolerance, no grandfather
   entries. This is the mechanism that stops AWT creeping back into the
   replication path; without it the change is undone by the next contributor.
5. **The vocabulary closed.** The four rows in `docs/operation-layer.md`'s
   mutation-site inventory marked "op implemented + tested; gesture still
   inline" are migrated to `OpSink`: placement drop (`AddElements` plus the
   drop's wiring, as preview-then-commit), paste (`AddElements` + `AddWire`),
   wire-attach finish (`AddWire`, at net granularity), and the dialog and
   quick-edit commits (`SetElementConfig`). The inventory table is updated in
   the same change; a row that stays inline states why.
6. **Documentation.** `docs/operation-layer.md`'s "Layering" paragraph is
   restated: `jls.collab.op` depends on `jls`, `jls.elem` and `jls.core`, and
   on **neither** AWT nor Swing.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-015 | The whole point of the layer is that a mutation applies without a `Graphics`; a headless batch, a CI replica loop and a programmatic builder cannot construct one. |
| FEAT-052 | A replica applying a peer's op runs on a machine that may have no display. Applying through AWT makes convergence a function of the local toolkit. |

## Prerequisite tasks

None. The seam already exists at HEAD: `jls.core.TextMetrics`
(`src/jls/core/TextMetrics.java:22-51`) is the headless measurement interface,
`SwingTextMetrics` (`src/jls/edit/SwingTextMetrics.java:17`) is its
`FontMetrics`-backed implementation, and `TextMetricsParityTest` already pins
that the forwarding changes no pixel value.

## Acceptance test

`test/jls/collab/op/HeadlessOpApplicationTest.java`, new:

- `everyOpKindAppliesWithNullMetrics()` - one case per kind in `CircuitOp`'s
  `permits` list (`src/jls/collab/op/CircuitOp.java:36-38`), applied to a
  fixture circuit with a null `TextMetrics`, asserting no exception and the
  expected mutation. The enumeration is derived from the sealed permits list
  reflectively so a new kind fails this test until it has a case.
- `headlessApplicationProducesTheSameBytesAsGraphicsApplication()` - for the
  four sizing-sensitive kinds, apply once through a `SwingTextMetrics` built
  from a headless-safe `BufferedImage` graphics and once through the same
  metrics passed directly, and assert the canonical saves are byte-identical.
  This is the guard that the signature change moved no geometry.
- `applyThenInvertIsStillByteIdenticalHeadless()` - the existing `#167` inverse
  contract, re-run with no drawing context.

`test/jls/HeadlessCoreRatchetTest#coreCandidatesGainNoForbiddenImports` is the
gate: with `src/jls/collab/` added to `CORE_PACKAGE_PREFIXES` and `BASELINE`
empty, it fails on today's tree at five files and passes only when all five are
clean.

`test/jls/collab/op/CircuitOpTest` gains `everyGestureRowIsMigratedOrExplained`,
cross-checking `docs/operation-layer.md`'s inventory table against the set of
`OpSink.submit` call sites in `SimpleEditor`, so the table cannot go stale.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b) | closes - this is the remaining half of #167: the headless signature and the four unmigrated gestures |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | depends on - "closed vocabulary" is only true once every gesture goes through it; an inline mutation is a hole in the closure |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | informs - a headless `jls.collab.op` is one of the layers #224 describes |

Recorded decision, closed, cite as such and not as open: **#77** (the
`TextMetrics` seam this task consumes).

## Notes

- **`ArchitectureRulesTest.collabLayersAreHeadless` will not catch this.** It
  bans only `javax.swing..` (`test/jls/ArchitectureRulesTest.java:150-160`), so
  it passes today with five `java.awt` / `jls.edit` imports in the package. The
  `HeadlessCoreRatchetTest` prefix addition is the real gate. Consider
  tightening the ArchUnit rule in the same change; do not rely on it alone.
- **`TextMetrics` is nullable at the seam and must stay so.**
  `SwingTextMetrics.forGraphics` returns null for a null `Graphics` by design
  (`src/jls/edit/SwingTextMetrics.java:66-70`) and elements treat that as skip
  sizing. Making the parameter non-null would force every headless test to
  synthesize metrics and would silently change `width`/`height` on the elements
  that recompute size on load.
- **`AddElements` reloads through the real loader.** Its `apply`
  (`src/jls/collab/op/AddElements.java:56-67`) calls
  `ElementBlocks.load` then `el.init(...)` then `circuit.addElement(el)` - "the
  same order `Circuit.finishLoad` uses". Keep that order; the scratch-circuit
  dry run in `validate` (`:92-152`) is what makes the re-load unable to fail.
- **`SetElementConfig` initializes twice.** Once in `validate` (`:120`) as a dry
  run and once in `apply` (`:66`). Both take the metrics; passing different ones
  would make the dry run a different check from the real one.
- **Preview-then-commit is the hard half.** The placement and wiring gestures
  mutate live and compensate on cancel; migrating them means the drag becomes a
  preview and the op becomes the mutation of record, which is exactly what
  `moveSelectionPlan` already does for pure relocations. Budget the two weeks
  here, not on the signature change.
- **The undo mechanism does not change.** `OpSink.submit` runs
  validate -> apply -> `markChanged()`, and snapshot undo stays user-facing;
  op-inverse undo is explicitly a later stage.

## Evidence

- `src/jls/collab/op/CircuitOp.java:36-38` (the sealed permits list), `:51`
  (the `apply(Circuit, Graphics)` signature).
- The five AWT call sites, verified by grep over `src/jls/collab/`:
  `AddElements.java:61`, `SetElementConfig.java:66`, `SetElementConfig.java:120`,
  `FlipElement.java:26`, `RotateElement.java:31`; plus `import java.awt.Graphics`
  in all eleven op files and `CircuitOp.java:3`.
- `src/jls/edit/SimpleEditor.java:5547-5570` - the anonymous `OpSink`, whose
  `submit` is `op.apply(circuit, getGraphics()); markChanged();`.
- `test/jls/HeadlessCoreRatchetTest.java:55-58` (the forbidden-import regex,
  which covers `java.awt.`, `javax.swing.` and `jls.edit.`), `:74-79` (the
  prefix set, which does not include `src/jls/collab/`), `:90` (`BASELINE` is
  empty).
- `src/jls/core/TextMetrics.java:22-51` and `src/jls/edit/SwingTextMetrics.java:17,
  44-70` - the seam and its null-safe adapter.
- `docs/operation-layer.md` - the mutation-site inventory and the four
  "gesture still inline" rows; the layering paragraph that claims headlessness.
- Do not restate: `docs/operation-layer.md` owns the op contract;
  `docs/grand-architecture.md` owns the layering argument.
