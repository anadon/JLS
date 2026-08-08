# Issue #850: TASK-C370-5: the spatial index reads the flat state rather than a second copy of it, and its rebuild cost is measured on the way past
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Underneath the wording, #850 wants one thing: **the editor's accelerator must stop
being a second truth about where elements are.** That is a genuine and important
goal — `SpatialIndex` really does keep a private `Map<Element, Bounds> indexed`
(`src/jls/SpatialIndex.java:52`) recording the bounds each element *was* filed
under, and correctness today rests on nine hand-placed sync calls: four
`index.invalidate()` in `Circuit`, two `invalidateIndex()` call sites in
`SimpleEditor`, and three `reindexAfterMove(selected)` calls at
`SimpleEditor.java:3109,3552,3835`. Every one of those is a place a future
contributor forgets. #370 invariant 4 is right to forbid it.

But the task as filed names the wrong state, depends on scope nobody has funded,
duplicates its sibling, and — most costly — treats as hygiene the one place in
FEAT-054 where the capacity budget is most likely to be *missed*. I am explicitly
disregarding AC-1 and AC-2 as written, for the reasons below.

## 1. The category error: the spatial index does not read runtime state

FEAT-054 flattens **runtime simulation state** — signal values, register
contents, the things `react` bodies read and write. `SpatialIndex` reads
`Element.getIndexBounds()`, which is `getRect()` (`src/jls/elem/Element.java:782-785`)
— **editor geometry**, persisted layout, not runtime state. TASK-C370-2 (#843)
scopes the column store to runtime state; TASK-C370-3 (#846) moves runtime state
into it. Neither creates a geometry column.

So AC-1's "the spatial index derives from the columns" is unsatisfiable against
the columns #843/#846 actually build, and AC-2 —

> a test mutates runtime state and asserts the spatial index reflects it with no
> explicit sync call

— asserts a coupling that **must not exist**. If toggling a register's contents
moves an element in the spatial index, that is a bug, not a proof of agreement.
Written literally the test is either vacuous (it passes because nothing happens)
or it specifies wrong behaviour. The property worth asserting is: *mutate an
element's geometry and the next query reflects it with no explicit sync call.*

This is not a wording nit. It means #850's stated `ordering_after: TASK-C370-3`
is wrong: nothing in #850's real content waits on the runtime-state migration.
The task can land **today**, against HEAD, and it would remove a live class of
staleness bugs from the editor before FEAT-054 exists.

## 2. The reframing: geometry gets a single writer and a version, not a copy

AC-1 says "no separately maintained copy of element position or extent
survives". Taken literally that forbids every spatial acceleration structure
ever built: a grid *must* remember which cells it filed you under, or it cannot
unfile you when you move. Chasing the letter of AC-1 produces a worse index.

The property that actually kills the bug class is not *absence of a derived
copy* but *impossibility of undetected staleness*. Concretely:

- Geometry writes go through one setter on the model, which bumps a monotonic
  `geometryVersion` (single writer: the EDT).
- `SpatialIndex` records the version it was built at.
- `query()` compares versions and rebuilds on mismatch.

Divergence becomes structurally impossible — staleness is *detected*, never
*remembered* — and all nine sync call sites above delete. `invalidate()`,
`invalidateIndex()`, and `reindexAfterMove()` disappear from the public surface
of `Circuit`. The drag fast path survives as a bounded per-element stamp
(rebuild only elements whose own version moved), which is what `update()` already
approximates by convention.

The honest version of AC-1 is therefore: **the index holds no state that is not a
pure function of the geometry columns, and it cannot answer a query from a stale
function without noticing.** That is testable, achievable, and strictly stronger
than "no copy survives".

## 3. "Provably in agreement" can be literal here, and the issue settles for less

This repository already has `proofs/SpatialIndexCorrectness.agda` THEOREM 1
(`query-parity`), pinned to the Java by `test/jls/ProofBridgeTest.java`
assumptions A1-A5. Read the theorem's signature: it is parameterised by
`bounds : E → Rect` — **a function**. The Agda model already assumes exactly one
source of truth for geometry. The Java `indexed` snapshot is precisely the
un-modelled gap: the proof holds w.r.t. the bounds the grid was built from, and
says nothing about whether those are the bounds an element has now.

So invariant 4, for this reader, is not a JUnit test at all — it is a **sixth
proof-bridge assumption (A6): the bounds used at query time are the elements'
current bounds.** #850 uses the word "provably" and then delivers one sampled
test. In a project that ships an Agda proof of its hit-testing, that is leaving
the best available answer on the table. Any redesign of `SpatialIndex` must carry
the proof forward regardless; #850 does not mention the proof or `ProofBridgeTest`
once, which is a gap a implementer will discover the hard way.

## 4. The measurement is aimed at the wrong number

AC-3 measures *rebuild cost* and recommends where to home it. Two problems.

**First, `rebuild` never runs headless.** `index.rebuild` is reached only from
`Circuit.elementsNear` (`Circuit.java:567-568`), which is called only from editor
hit-testing and draw culling. Batch mode, VCD export, and image export never
query, so the index stays `dirty = true` and is never built. #370 §6's "the
spatial-index rebuild becomes the largest single cost once the load-path and
footprint walls are patched" is therefore a claim about **opening a 694,709-element
circuit in the GUI editor**, not about simulation capacity. That should be said
out loud, because it changes who the unowned cost belongs to.

**Second, and more important: the index's own footprint is the number at risk.**
K17-1 sets ≤150 B/element. Per indexed element `SpatialIndex` holds a
`HashMap.Node` (~40 B with table slot), a `Bounds` record (~32 B), plus
membership in every overlapping cell: an `ArrayList` slot per cell, a boxed
`Long` key (24 B) and another `HashMap.Node` per occupied cell. At the 4x12 px
`CELL` a typical element spans one to four cells. That is plausibly of the same
order as the entire per-element budget FEAT-054 is trying to hit — the
accelerator built to keep the editor responsive could single-handedly fail the
capacity kill criterion. **This, not rebuild latency, is the measurement #850
should be taking**, and it belongs in TASK-C370-1's (#842) footprint record, on
both fixtures, with and without an index resident.

## 5. The out-of-the-box move: scope the index to the viewport and the cost vanishes

AC-3 asks where to *home* the unowned rebuild work (#370 §6 recommends FEAT-005).
There is a better answer: **do not build a global index at all.**

A human cannot edit 700,000 elements on a canvas; they edit the few thousand in
the viewport. Index only the viewport plus a margin, rebuilt when the view
scrolls or zooms. Rebuild cost becomes O(visible), independent of circuit size,
*forever* — no re-homing, no owner needed, and the index's footprint stops
scaling with N, which resolves §4 as well. Hit-testing and draw culling both
already work against a query rectangle, so the call sites do not change shape.
The viewport is also the natural editor-side partition for FEAT-055 (#332)
streaming elaboration, so this pulls *with* the arc rather than adding a
structure FEAT-055 must later dismantle.

This is the reframing that makes the problem disappear rather than moving it.

## 6. It duplicates its sibling along a seam #370 drew as one row

#370 §2 row 4 and §6 name one scope: "the editor and the spatial index". It was
filed as two issues, #850 and #851 (TASK-C370-6). But `SimpleEditor` reaches
geometry *through* `Circuit.elementsNear` — they are one reader, not two. The
consequence is visible in the ACs: #850 AC-2 and #851 AC-2 are the same
assertion, and #851 AC-3's "per-edit cost measured before and after" **is**
#850 AC-3's rebuild measurement, since per-edit cost on a large circuit is
dominated by hit-testing. Two issues will measure the same thing by two methods
and reconcile at close-out. Splitting a row to parallelise, then handing both
halves the same seam, reimports the coordination the split was meant to remove.

## Recommendation

Restate #850 as: **"geometry gets one writer and a version stamp; the spatial
index becomes a detectably-stale pure function of it, and its own per-element
footprint is measured."** Then:

1. Drop `ordering_after: TASK-C370-3`. This work does not need the runtime-state
   migration and is more valuable before it.
2. Replace AC-2 with a geometry mutation, and add the A6 proof-bridge assumption
   so "provably" means what the repo already means by it.
3. Move the footprint measurement (index bytes/element, both fixtures) into
   #842's record; keep rebuild latency as a secondary number.
4. Merge with #851, or redraw the split as *mechanism* (version-stamped geometry,
   both readers) versus *evidence* (one measurement pass covering startup,
   per-edit, and index footprint).
5. Record the viewport-scoped index as the recommended disposition of the
   unowned rebuild cost, in place of re-homing it to FEAT-005.

The seam #850 names is real and worth cutting. The cut as drawn is on the wrong
axis, and one axis over is a change that is smaller, lands sooner, and retires
the cost instead of relocating it.
