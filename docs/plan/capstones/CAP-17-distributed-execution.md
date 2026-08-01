# CAP-17 - Distributed execution for cluster and grid deployments

**Status:** proposed | **Priority:** 12 | **Marginal cost:** 38-62 mw |
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
in parallel), CAP-09 (multi-seed verification) and the fault campaigns in
FEAT-041's orbit, and it is not throwaway: the capacity axis needs the same job,
artifact and aggregation vocabulary.

## Prerequisite features

| Feature | Why THIS capstone needs it | Grade |
|---|---|---|
| FEAT-054 flat off-heap element representation | The binding constraint. At the measured ~1,190-2,150 B/element live heap, 10^10 elements is ~15 TB and does not fit anywhere; at ~100 B/element it is ~1 TB, which is a large but purchasable single machine. This one change is worth ~15x capacity BEFORE any distribution, and it is the same plane-array representation measured in-tree at 4.32 ns/node against 22.01 for `BitSet[]`. Nothing else in this capstone is worth doing first. | required |
| FEAT-055 partitioned model and streaming elaboration | The circuit must exist as parts that load independently. Today a design is one file under a 64 MiB decompressed cap (~695k elements - 10^10 is ~14,000x past it) and `finishLoad` is O(W^2). | required |
| FEAT-056 distributed simulation transport and barrier protocol | Cross-partition event exchange with a synchronization discipline that is deterministic and independent of partition count and message arrival order. | required |
| FEAT-057 campaign execution and artifact aggregation | The scale-out-runs axis; also the demo slice. | required |
| FEAT-004 shared net partition IR, stable net naming | Partition boundaries are cut on nets; a net must have an identity that survives being cut and must name the same signal on both sides. | required |
| FEAT-014 stable addressing and per-view geometry | A watched element inside partition 7 needs a name that does not depend on which partition it landed in. | required |
| FEAT-006 simulation capacity and long-run ergonomics | The single-host capacity work this capstone extends; the byte-budget and long-run paths are shared. | required |
| FEAT-030 engine constant factors | The flat representation is the same work from the other side; do not fund it twice. | required |
| FEAT-009 measurement gate and calibration fixture | Every capacity and speedup claim here divides by constants this feature measures. No number in this capstone is trustworthy before it lands. | required |
| FEAT-035 checkpoint and simulation state serialization | A distributed run that cannot be suspended cannot be scheduled on a shared cluster, and partition state must be serializable to move it. | required |
| FEAT-007 CI long-run lanes, timeouts, platform parity | Multi-host tests need a lane that can host them. | beneficial |
| FEAT-005 quadratic and materializing IO paths | O(n^2) load and whole-dump materialization are fatal at this scale; they are already defects at current scale. | beneficial |
| FEAT-031 per-instance fidelity toggle | Partitioning is far more tractable when a subtree can be bound behavioral at the boundary; the two mechanisms compose. | beneficial |

## Related GitHub issues

| Issue | Relationship |
|---|---|
| no issue | The distributed-execution capability has no tracker representation. CAP-17 was added at maintainer request after the registry was fixed; an issue should be filed before any task here starts, per CONTRIBUTING.md. |
| #84 decompose SimpleEditor | overlaps - the flat representation touches model internals the editor reads directly |
| #77 headless core (closed) | depends on - the partitioned model must be constructible and simulable with no GUI present |

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
