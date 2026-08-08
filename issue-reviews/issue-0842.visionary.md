# Issue #842: TASK-C370-1: per-element live heap is measured on a generated design at scale and committed as data, so every capacity claim in the feature has a denominator
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its parent's vocabulary, #842 buys one thing: **the cheapest possible
probe of the most expensive program in the tracker.** #370 is banded at 12-20
maintainer-weeks and marked UNOWNED; CAP-17 (#312) hangs a 10^10-element target
off it; and the entire case rests on three numbers (~1,190 B/element, ~2,150
B/element, 6.8 runtime objects per logic element) that exist nowhere in this
repository. I looked: `docs/machine-calibration.md` and `docs/plan/` are absent
from the working tree and from history reachable here, and none of the figures
(`1,190`, `2,150`, `96.6`, `165,000`, `694,709`) appears anywhere under `docs/`.
The denominator is currently issue prose citing issue prose. Taking a real number
before spending 12-20 weeks is unambiguously right, and it is the project's own
discipline: #335 exists precisely to stop arithmetic-on-a-guess.

So I endorse the *act*. I do not endorse the *shape* of the measurement, which
is chosen so badly that AC-4 has to pre-write its own excuse.

## The reframing: measure a cost model, not a heap

AC-1 and AC-4 ask for the live heap of a **constructed circuit above ~165,000
elements**. Everything painful about this task follows from that one choice:

- It inherits a hard ordering dependency on FEAT-005 (#353) — which #335 also
  sits behind — so #842's real position is two features deep, not "after #335."
- AC-4 therefore has to contemplate its own failure in advance ("or the record
  states plainly that it was taken below the wall and that #370's integration
  criterion 1 is therefore unattainable"). An acceptance criterion whose
  alternative branch is *"declare the parent unverifiable"* is a design smell,
  not a contingency.
- AC-2 and AC-4 are close to jointly unsatisfiable. At the quoted ~96.6 B/element
  on disk, a fixture above the wall is ≥16 MB of circuit text, and the
  post-FEAT-005 ceiling of "~694,709 elements" is not a load-path property at
  all — 67,108,864 / 96.6 = 694,709.8, i.e. that number *is*
  `FileAbstractor.MAX_CIRCUIT_TEXT_BYTES` (`src/jls/FileAbstractor.java:65`,
  `64L << 20`) divided by the on-disk rate. The band in which a committed fixture
  is both above the wall and openable by JLS runs from 165k to the reader's own
  refusal threshold. Meanwhile #335's Open Question 1 ("where does a
  tens-of-megabyte fixture live?") is **unresolved and blocks TASK-0025**, and
  its option (c), generate-at-test-time from a committed generator, is explicitly
  rejected there. #842 says "named committed generated design" without saying
  which side of that open question it lands on.
- A scalar live-heap number is not even reproducible in the way AC-2 hopes.
  Object header size h ∈ [12,16] B — #370 §3 says so itself — flips with
  compressed oops, which flips when the heap crosses ~32 GB. Committing "the JVM
  version and the heap settings" records the dependency without removing it, and
  the README's own build posture (JDK 25 floor plus an advisory newest-GA lane)
  guarantees the number will be taken under more than one JVM.

**The alternative: produce a function, not a scalar.** Measure a per-element-type
structural cost model — for each of the ~35 registered element types plus `Wire`
and `WireEnd`, the object graph shape and field widths it retains — calibrated on
circuits JLS can comfortably load, and validated by whole-heap measurement on the
largest circuit that *does* load. The deliverable committed to the tree is

    B(design) = Σ_t n_t · b_t   (+ a stated residual from the validation fit)

with the per-type column `b_t` and the fit residual as the data. Consequences:

1. **The wall stops mattering.** You can state the footprint of a 10^10-element
   design without constructing one, because you are extrapolating a validated
   linear model, not building a heap. AC-4's dilemma evaporates; so does #842's
   dependency on FEAT-005; so does the tens-of-megabyte fixture question, since a
   calibrated model needs only small committed fixtures.
2. **It answers a question the scalar cannot.** #370's real need is not "is the
   number above or below 150?" — it is *where the bytes are*, so the flattening
   work can be aimed. A per-type column ranks the targets on day one.
3. **h becomes an explicit parameter** instead of an unstated JVM property, so
   the number reproduces across the compressed-oops boundary rather than
   silently changing meaning.

## The reason this reframing may shrink #370 by an order of magnitude

A field-level attribution is likely to show that much of the per-element heap is
**not runtime state at all**, and therefore is not what a flat-array rewrite is
needed to remove. From the tree at HEAD:

- `WireEnd.loadPut` (`src/jls/elem/WireEnd.java:40`) is a `String` per wire end,
  populated in `setValue` (`:662`), read once in `init` (`:104-123`), and
  **never cleared afterwards**. `loadAttach` (`:38`) and `loadTriState` (`:42`)
  are the same story. A retained String plus its `byte[]` on every wire end is a
  plausible large share of why the wire-heavy shape measures ~2,150 B against the
  processor shape's ~1,190 B — which would mean the "range across two shapes"
  #842 wants to honour is mostly *load scaffolding*, not a real spread in
  runtime cost.
- `WireEnd.wires` (`:28`) is a `LinkedHashSet` per wire end — a HashMap plus node
  objects — for a set that is nearly always size 1 or 2.
- `WireEnd.myCopy` (`:36`), `Element.savex`/`savey`
  (`src/jls/elem/Element.java:44,46`) and `Element.highlight` (`:42`) are editor
  scratch resident on every element of every headless batch run.
- `Element.stableId` (`:24`) is a separate `ElementId` object per element
  (`src/jls/elem/ElementId.java:184,186`: a shared `String replica` plus a
  `long`), i.e. an extra header and reference where a `long` would do.

The precedent for the cheap fix is already in the file the issue cites.
`Circuit.finishLoad` ends with `elementMap.clear()` under the comment "*the id
map is only needed while wire ends resolve their refs above; keeping it pinned
every loaded element (#51)*". Dropping the per-element load scratch after
`finishLoad` is the identical move, one level down — days of work, not weeks.
If that alone moves 1,190 B toward 500 B, then "roughly an order of magnitude"
and CAP-17's ≤150 B/element threshold are being argued against a baseline
inflated by removable dead weight, and #370 gets re-scoped rather than funded.
**The measurement should be shaped so it can discover this.** A scalar cannot;
a per-type, per-field attribution does, and costs the same instrumented run.

## Two smaller corrections

**The denominator is mix-dependent and the issue does not say so.** "Bytes per
element" divides by a count that mixes `LogicElement`, `Wire` and `WireEnd` — and
#370 §3 records 6.8 runtime objects per *logic* element, a different denominator
again. A metric that swings 1.8x with fixture shape cannot serve as K17-1's kill
criterion, because the verdict becomes a function of which fixture someone chose.
#335's own criterion 6 already solves this for the sibling metric: no ns/node
figure publishes without node count and pass count, and §3 defines n = L + W.
**#842 should adopt that verbatim** — commit the element census (per-type counts,
and L vs W) beside every byte figure. It currently does not, which makes it
inconsistent with the gate it claims to be measured by.

**Widen the run, not the task.** #335's stated cutting principle is to cut by
*what one instrumented run yields* — TASK-0022 settles three constants at once
rather than running the same machine three times. #842 takes heap only. But
#370's §4 invariant 3 (editor per-edit cost and startup time) is the criterion
that "can veto the whole feature," and its integration criterion 3 needs a
before-baseline on **the same fixtures**. Take it in the same run at the same
commit. Otherwise a second measurement task gets filed later with its own
fixture-provenance argument, and the veto criterion is the one left without a
baseline — the exact failure #842 was written to prevent for heap.

## Against the project's arc

I am not disregarding the acceptance criteria, but I want the *reporting* aimed
one level up from where #842 aims it. ARCHITECTURE.md's recorded decision on
simulation strategy (#221) states that the event-queue interpreter is JLS's
**sole** strategy, that classroom-scale gate circuits are the present workload,
and that a second strategy is "premature optimization until CPU-scale designs are
actually common" — with a revisit trigger tied to a concrete design being
unusably slow. README opens with "an educational digital logic circuit editor and
simulator." CAP-17's 10^10-element target pulls hard against both. #842 is the
honest instrument for testing that tension, and it should be pointed at it:
alongside the generated shapes, record the footprint of the designs JLS users
actually build — `test/fixtures/riscv-sum1to10.jls`, the `examples/` tree — so
the committed data shows the ratio between what the project serves today and what
the capacity program asks for. That single extra column costs nothing and is the
number most likely to change a funding decision. A measurement task that can only
say "#370's denominator is X" has answered the smaller question.

## Verdict

**endorse-with-reframing.** Take the measurement; take it now; do not wait for
FEAT-005. But produce a validated per-type cost model with the census attached,
not a scalar live heap on a fixture that cannot exist yet — the model dissolves
AC-4's dilemma, dissolves the inherited fixture-hosting question, survives the
JVM it was taken on, and is the only form of the measurement that can tell the
maintainer whether #370 is a 12-20 week rewrite or a two-week deletion of dead
fields.
