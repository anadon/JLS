# CAP-17 - Distributed execution for cluster and grid deployments

**Status:** proposed | **Priority:** 18 | **Marginal cost:** 38-62 mw |
**Standalone cost:** 62-98 mw

## Outcome

A circuit too large to fit in one machine's memory is elaborated, partitioned
across a cluster, simulated, and observed as one design - and the same
infrastructure runs a campaign of thousands of independent simulations across a
grid. The commercial-bridge target is a design on the order of 10^10 gates,
which at the measured per-element footprint does not fit on any single system.
This capstone is explicitly NOT a teaching requirement; K9's pedagogy floor is
protected by the whole capability being absent from the default experience.

## Acceptance test

**Two axes, two tests. The capacity axis is the one this capstone exists for.**

**AT-17-CAPACITY (scale-out model).** SEEN: a generated design of at least 10^9
functional elements - beyond any single-machine JLS - is loaded from a
partitioned model across N>=4 hosts, simulated for a declared number of cycles,
and reports the same watched-element values a single-host run of a 10^7-element
subset reports for the equivalent sub-design. The operator sees one design, one
run, one result set.

CHECK: `DistributedCapacityTest` - a fixed generated design (deterministic
generator, seed in the failure report) is (1) run whole on one host at a size
that fits, and (2) run partitioned across N in {2,4,8} simulated hosts over the
in-process transport. All N+1 runs must produce **byte-identical** watched output
and VCD. Partition count must not be observable in the result. A run whose
output depends on N is a failure, not a tolerance.

**AT-17-CAMPAIGN (scale-out runs).** SEEN: `jls -campaign sweep.yaml -j 200`
dispatches a fault-injection or parameter sweep across a grid and returns one
aggregated report; a course's whole submission set grades in the time one
submission used to take.

CHECK: `CampaignDeterminismTest` - the same campaign executed at -j 1 and at
-j 64, in a shuffled dispatch order, produces byte-identical aggregate output
and identical per-job artifacts. Job scheduling must not be observable.

**FALSIFICATION GUARD (must fail before it passes).** A deliberately
partition-sensitive design - a cross-partition combinational cycle - must be
REFUSED at partition time by name, not silently simulated to a different answer
than the single-host run. Without this the byte-identical assertions above can
pass vacuously on designs that happen not to exercise a boundary.

## Demo slice

**CAP-17-MIN, 10-16 mw: the campaign axis only, on one machine's cores.**
A campaign runner over the existing deterministic batch mode - job description,
parallel dispatch, artifact collection, aggregate report - with
`CampaignDeterminismTest` green at -j 1 versus -j N. No partitioning, no
network, no model changes. It is immediately useful to CAP-06 (a course grades
in parallel) and to CAP-09 (multi-seed and fault-injection verification), and
it is not throwaway: the capacity axis needs the same job,
artifact and aggregation vocabulary.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | required/beneficial |
|---|---|---|---|
| FEAT-054 | Flat, compact element representation | The binding constraint AT SCALE: at the measured per-element live heap the target design does not fit on any single system, the flat layout is worth roughly an order of magnitude of capacity before any distribution, and it is the same layout the throughput work needs. **It is NOT the first thing to fund** - see the measured wall ordering in Evidence; heap is the FOURTH wall, not the first | required |
| FEAT-005 | Quadratic and materializing I/O paths eliminated | **PROMOTED TO REQUIRED AND TO FIRST** on measurement. `finishLoad` is the first wall any `.jls`-borne design hits, at ~165,000 runtime elements, and the fix measured here is one data-structure swap. Nothing in this capstone is reachable until it lands | required |
| FEAT-055 | Partitioned model and streaming elaboration | The circuit must exist as parts that load independently; today a design is one file under a decompressed-text cap and its load walk is superlinear in wire ends | required |
| FEAT-056 | Distributed simulation transport and barrier protocol | Cross-partition event exchange under a discipline whose result is independent of partition count and message arrival order | required |
| FEAT-057 | Campaign execution and artifact aggregation | The scale-out-runs axis, and the demo slice that makes this capstone deliverable before any partitioning exists | required |
| FEAT-004 | Shared net-partition IR with stable net naming | Partition boundaries are cut on nets; a net must have an identity that survives being cut and must name the same signal on both sides | required |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | A watched element inside one partition needs a name that does not depend on which partition it landed in | required |
| FEAT-006 | Simulation capacity and long-run ergonomics | The single-host capacity work this capstone extends; the byte budget and the long-run paths are shared | required |
| FEAT-030 | Engine constant factors: the semantics-preserving stack | The flat representation is the same work from the other side; do not fund it twice | required |
| FEAT-009 | The measurement gate and a tracked calibration fixture | Every capacity and speedup claim here divides by constants this feature measures. No number in this capstone is trustworthy before it lands | required |
| FEAT-035 | Checkpoint and simulation-state serialization | A distributed run that cannot be suspended cannot be scheduled on a shared cluster, and partition state must be serializable to move it | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | Multi-host tests need a lane that can host them | beneficial |
| FEAT-031 | The per-instance fidelity toggle and its boundary harness | Partitioning is far more tractable when a subtree can be bound behavioral at the boundary; the two mechanisms compose | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | Distributed execution for cluster and grid deployments | **no issue** - CAP-17 was added at maintainer request after the registry closed; an issue should be filed before any work here starts |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps - the per-signal half of the representation change FEAT-054 carries through to the element array |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | overlaps - the flat representation touches model internals the editor reads directly |
| 77 | Extract a headless `jls.core`: the simulator base class imports Swing, `Circuit` holds an editor reference, and `JLSInfo` is a global mutable hub | depends on, **closed** - a partition must be constructible and simulable with no graphical toolkit present |

## Open decisions

1. **Is 10^10 the target, or is 10^8-10^9 the honest commercial bridge?**
   Recommendation: state 10^10 as the ambition and 10^9 as the acceptance
   threshold. Reason: 10^9 is already ~1,400x past the current file ceiling and
   demonstrably beyond one machine at today's footprint, so it proves the
   capability; 10^10 is a scaling exercise once the mechanism exists.
2. **Conservative or optimistic synchronization?** Recommendation: conservative
   (Chandy-Misra-Bryant with null messages), and refuse designs whose lookahead
   is too low rather than adding rollback. Reason: optimistic (Time Warp) needs
   rollback, and JLS has none - `grep` for cancel/withdraw/rollback across
   `src/jls/sim` returns zero, the same absence that let the analog A-STEP design
   delete XSPICE's `EVTbackup`. Adding rollback is a second, larger program.
3. **Does the partitioner cut automatically, or does the author declare
   partitions?** Recommendation: author-declared first (a subcircuit instance is
   a partition candidate), automatic later. Reason: automatic min-cut
   partitioning is a research problem whose quality determines all performance;
   author-declared makes the mechanism testable without it.
4. **Transport.** Recommendation: reuse the `jls.collab.net` `Transport`
   abstraction and its `LoopbackTransport`/`ChaosTransport` test doubles rather
   than introducing a second networking stack. Reason: it exists, it is tested,
   and the socket-confinement ArchUnit rule already names that package.

5. **Where does CAP-17 rank against the seventeen commissioned capstones?**
   Recommendation: **unranked until the campaign demo slice ships.** Reason: the
   maintainer commissioned seventeen capstones and added this one afterwards
   without ranking it. Priority 18 in the header means "appended, not yet
   ranked", not "least important" - the demo slice alone is worth more per week
   to CAP-06 and CAP-09 than several higher-ranked items, and the capacity axis
   is worth less than any of them until FEAT-054 is measured.

## Kill criteria

- **K17-1 (capacity, before FEAT-055 starts).** If FEAT-054's flat
  representation does not reach <=150 B/element measured on a generated design,
  the 10^10 target is unreachable by distribution alone (>=1.5 TB aggregate
  before any per-host overhead) and the capstone is restated at 10^8.
- **K17-2 (determinism).** If byte-identical output across partition counts
  cannot be achieved, STOP. A distributed run that answers differently from a
  local run destroys the golden-test discipline at exactly the moment the
  designs are too large to check by hand. There is no tolerance-based fallback
  for a digital simulation.
- **K17-3 (speedup).** If a 4-host partitioned run of a feedback-dense design is
  not faster than a 1-host run of the same design at a size that fits, the
  capacity axis stands but the performance claim is withdrawn from all
  documentation, and CAP-17 is a capacity capability only.
- **K17-4 (falsification).** If the cross-partition combinational cycle is not
  REFUSED by name, the acceptance tests are vacuous and nothing downstream
  merges.
- **K9 (pedagogy floor, outranks all of the above).** Any regression to startup
  time, per-edit cost or palette size for a first-year drawing an adder stops
  the responsible task regardless of what it costs this capstone.

## Evidence

- **THE WALL ORDERING WAS MEASURED, AND IT REVERSES THIS CAPSTONE'S FIRST MOVE.**
  The original text said the flat representation was the binding constraint and
  that "nothing else here is worth doing first". That was reasoning from the
  heap arithmetic alone. Measured against HEAD, the walls arrive in this order:
  (A) a stack overflow on a recursive walk in the import path at 4,000-5,000
  cells of combinational depth, about 62,000 elements - an eight-line fix, or
  set `-Xss`; (B) `finishLoad` at about 165,000 runtime elements - a ONE-LINE
  data-structure swap, `LinkedList` to `LinkedHashSet`, measured; (C) the 64 MiB
  decompressed file cap, THIRD, not first; (D) heap, FOURTH, and only on a small
  machine. With A and B patched the reachable size goes from ~165,000 to
  ~695,000 elements - **4.2x for two lines** - and a new second-order wall
  appears behind them, the spatial-index rebuild, which becomes the largest
  single cost. So FEAT-005 is promoted to required and to first, and FEAT-054
  keeps its rationale but loses its primacy.
- Largest fixed open RISC-V design surveyed: XiangShan. Its 16-core
  configuration needs roughly twenty AMD VU19Ps - the largest monolithic FPGA
  ever built, ~9 million logic cells - so even the largest public design is far
  from this capstone's target and a generated stressor remains unavoidable.

- Per-element live heap measured at ~1,190 B (a 1,551-element CPU) and ~2,150 B
  (a 60,004-element wire-heavy chain); ~96.6 B/element on disk. 6.8x runtime
  objects per logic element. Java object headers alone at 12-16 B x 6.8e10 are
  ~1 TB before any fields.
- `FileAbstractor.MAX_CIRCUIT_TEXT_BYTES = 64L << 20` gives a ~695,000-element
  single-file ceiling, with no save-side check.
- `Circuit.finishLoad` is O(W^2); 80,000 wire ends measured at 46 s.
- Engine throughput is nearly flat in circuit size (R ~ L^-0.12), so element
  count is a CAPACITY constraint here, not a throughput one. This capstone was
  initially mis-scoped as a throughput problem; the correction is recorded
  because it changes which feature comes first.
- Plane arrays measured at 4.32 ns/node against 22.01 ns/node for `BitSet[]` on
  the real CPU shape - the speed change and the capacity change are the same
  change.
- No rollback machinery exists in `src/jls/sim` (grep-verified), which decides
  open decision 2.
- Industry context, for the decision rather than against it: at 10^10 gates
  commercial EDA does not use software simulation either - it uses emulation at
  1-5 MHz on machines costing millions. A distributed software simulator at that
  scale would be doing something the incumbents do not, which is either the
  differentiator or the signal that the workload belongs on different hardware.
- **Cost reconciliation.** Marginal band 38-62 mw. Its 10 required features
  sum to 81-134 mw and its 3 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
