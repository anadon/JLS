# FEAT-054 - Flat, compact element representation

**Status:** proposed | **Cost:** 12-20 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A circuit's runtime state lives in flat, primitive-typed arrays indexed by
element rather than in a graph of per-element objects, so the per-element memory
footprint falls by roughly an order of magnitude and the largest circuit that
fits on one machine grows by the same factor. This is a capacity change first
and a speed change second, and the two are the same change: the array layout
that removes the object headers is also the layout that removes the pointer
chase.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-17 | required | The binding constraint. At the current per-element footprint the target design does not fit anywhere; nothing else in that capstone is worth doing before this |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-009 | Every capacity and speedup claim here divides by a per-element footprint and a per-event cost that the measurement gate establishes. Without it the band is a guess and the acceptance threshold has no denominator |
| FEAT-026 | The value carried per element is the thing being flattened. A plane-encoded, width-carrying immutable value is the representation the arrays hold; flattening around the current per-signal bit-set object would have to be redone |
| FEAT-030 | The same work approached from the throughput side. The two must be one purchase or the array layout is written twice with different invariants |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was added with CAP-17 after it closed. Minting its tasks is a maintainer decision recorded in `README.md` and in the registry addendum | - |

## Acceptance criteria

1. Per-element live heap on a stated generated design is at or below a declared
   byte budget, measured by the same method the measurement gate defines, and
   the measurement is committed as data.
2. Every existing simulation golden is byte-identical after the change. A
   representation change that moves a golden has changed semantics.
3. The editor's per-edit cost and startup time do not regress on the measured
   fixtures. This outranks the capacity gain.
4. The element-facing behavior interface is unchanged from the outside: an
   element author writes the same code against the same contract.
5. The flat representation and the event queue agree on one indexing scheme,
   asserted by a test rather than by convention.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps - #232 names the per-signal allocation half; the flat per-element layout is the same change carried through to the element array, and neither closes the other alone |
| - | The flat element representation as a capacity change | **no issue** |

## Design notes

The framing correction matters more than the technique: engine throughput is
nearly flat in circuit size, so element count is a capacity constraint rather
than a throughput one. A reader who takes this feature for a performance
optimization will scope it against the wrong acceptance test.

The overlap with the engine constant-factor work is not an adjacency, it is the
same code. Whichever of the two is funded first pays for the array layout, and
the second must be re-scoped rather than re-estimated. This is stated here and
in CAP-17 so it is not discovered twice.

## Risks

- **The model is read directly by the editor.** Flattening the runtime state
  without flattening what the editor reads produces two representations that
  must agree; that is the largest source of latent divergence in this feature.
- **Criterion 3 can veto the whole thing.** A capacity win that costs per-edit
  responsiveness is refused under the pedagogy floor regardless of what it buys
  the capacity capstone.
- **The band is unanchored until the measurement gate lands**, because the
  target byte budget is expressed relative to a measured baseline.

## Evidence

- The single-file ceiling the current representation implies:
  `src/jls/FileAbstractor.java:65` (`MAX_CIRCUIT_TEXT_BYTES = 64L << 20`),
  enforced at `:152` and `:306`, with no save-side check.
- The load path whose cost is superlinear in wire ends:
  `src/jls/Circuit.java:1300-1422` (`finishLoad`), with the `LinkedList` worklist
  at `:1345` and the linear `ends.remove(vend)` scan per visited end at `:1369`.
- The value representation being flattened: issue #232, open, verified against
  `list_issues(state=OPEN)`.
- Owner: **UNOWNED**; added with CAP-17 after the capability roadmap was
  committed, so no program pays for it.
- **Cost reconciliation.** Band 12-20 mw with no tasks. The band is CAP-17's
  own arithmetic for its four new features (12-20, 10-16, 10-18, 6-8, summing
  to 38-62 mw, which is CAP-17's marginal band). It is a projection against the
  measured per-element footprint, not a task rollup.
