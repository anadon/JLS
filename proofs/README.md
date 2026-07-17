# Machine-checked correctness proofs

This directory contains Agda proofs of the two correctness claims behind
the editor's drag/drop collision detection and repaint culling
(issues #3, #17):

| Theorem | Claim | Java it covers | Empirical twin |
|---|---|---|---|
| **THEOREM 1** `query-parity` | A uniform-grid index query (collect elements from every cell the query rectangle covers, keep those whose bounds touch it) returns **exactly** what a brute-force scan over all elements returns — the grid can never miss an element. | `jls.SpatialIndex` (`insert`/`query`/`boundsTouch`), used by `Circuit.elementsNear`/`elementsAt` for hover hit-testing, selection, and the drag overlap checks in `SimpleEditor.overlap()`/`connect()` | `SpatialIndexTest` |
| **THEOREM 2** `culling-parity` | Enumerating repaint candidates through the index with the clip grown by the draw margin, then applying the exact visibility predicate, selects **exactly** the elements a full `mayBeVisible` scan selects — index-driven dirty-region repaints are pixel-identical to full scans. | `jls.Circuit.draw` + `mayBeVisible` + `DRAW_MARGIN` | `DrawCullingParityTest` |

## How the proof connects to the Java

The proofs are stated over an idealized model (closed integer intervals,
an abstract monotone cell function). They apply to the real code exactly
to the extent the model describes it, so every modelling assumption is
named in the proof file's header — (A1) through (A5) — and each one is
pinned to the implementation by a correspondingly named test in
`test/jls/ProofBridgeTest.java`:

- **(A1)** index intervals are non-empty (`Math.max(width, 0)` clamp),
- **(A2)** `Math.floorDiv(_, CELL)` is monotone — the only property of
  the bucketing the completeness proof uses,
- **(A3)** `SpatialIndex.boundsTouch` is closed-interval overlap,
- **(A4)** `Rectangle.grow`/`Rectangle.intersects` are the model's
  interval grow and strict overlap,
- **(A5)** the draw margin is non-negative and `Circuit.mayBeVisible`
  computes the model's `MayBeVisible` formula.

Proof obligations the theorems discharge, versus what stays empirical:

- **Proven for all geometries:** no false negatives (a touching element
  always shares a grid cell with the query — `query-complete`; a visible
  element always survives the margin-grown query — `culling-complete`),
  no false positives (`query-sound`), and the resulting list-level
  filter equalities (`query-parity`, `culling-parity`).
- **Pinned by tests, not proven:** that the Java loops implement the
  modelled cell enumeration (the `Visits` reading — exercised
  end-to-end by the parity tests above), the assumptions (A1)–(A5), and
  everything about mutation (incremental `update` vs `rebuild`, covered
  by `SpatialIndexTest.staysExactAfterMovesAndInvalidation`).

## Checking the proofs

The development has **no postulates and no holes**; if `agda` exits 0,
the theorems are verified.

```sh
# Ubuntu/Debian: sudo apt-get install agda agda-stdlib
agda -i /usr/share/agda-stdlib -i proofs proofs/SpatialIndexCorrectness.agda
```

Checked with Agda 2.6.3 and the agda-stdlib packaged for Ubuntu 24.04
(v1.7.x). CI runs this check in the `proofs` job of
`.github/workflows/ci.yml`.
