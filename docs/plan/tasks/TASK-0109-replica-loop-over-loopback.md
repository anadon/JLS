# TASK-0109 - The replica loop over a loopback transport

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0037, TASK-0108

## Deliverable

Two headless replicas, one process, no sockets and no wall clock - each holding
a real `Circuit`, exchanging real `OpEnvelope`s over the real `Transport`
contract, and saving byte-identical files. This is the apparatus every later
convergence claim is measured on; without it "converges" is an opinion.

Precisely what changes:

1. **`test/jls/collab/ReplicaHarness.java`**, new - a replica is
   `(PeerId, Circuit, CausalBuffer, OpSink, Transport)`. `submit` applies
   locally, stamps an `OpEnvelope`
   (`src/jls/collab/crdt/OpEnvelope.java`) with the replica's
   `VectorClock` (`src/jls/collab/crdt/VectorClock.java`) and sends; `receive`
   feeds the `CausalBuffer` (`src/jls/collab/crdt/CausalBuffer.java`), which
   delivers in causal order exactly once, and applies each delivered op through
   the same `OpSink`. Serialization is the existing strict grammar -
   `CircuitOpReader` (`src/jls/collab/op/CircuitOpReader.java`) rejects rather
   than repairs, and the envelope is the only new framing.
2. **A seeded schedule runner** - `test/jls/collab/Schedule.java`: a seed
   produces an interleaving of local gestures and delivery events across both
   replicas, replayable from the seed alone. A failing seed is the bug report.
3. **`ChaosTransport` promoted to the harness's reach.** It is
   package-private in `test/jls/collab/net/ChaosTransport.java:23`; either the
   harness lives in that package or the class becomes public within the test
   tree. Say which in the change; do not leave it to an accidental package move.
4. **`LoopbackTransport.pair()`** (`src/jls/collab/net/LoopbackTransport.java:104`)
   is the clean channel and `ChaosTransport` the hostile one (drop, duplicate,
   reorder, partition, heal). Both already implement the production `Transport`
   contract - same payload cap, same clean-close-is-null shape - so the harness
   tests against the real seam, not a mock.
5. **The convergence oracle is the canonical save.** `Circuit.save`
   (`src/jls/Circuit.java:1466-1512`) orders elements by stable id and assigns
   file-local ids in that order, so two circuits with identical content save
   byte-identically whatever their edit history. `Circuit.stateHash()` (`:1548`)
   is the cheap comparison and the byte diff is the report.
6. **The harness distinguishes three outcomes, not two**: converged to the same
   bytes; converged to *different* bytes (a merge-rule defect - the interesting
   case); did not converge within the round bound (a delivery or buffer defect).
   A harness that collapses the last two is useless for diagnosing TASK-0110.
7. **`docs/collaborative-editing-research.md`** gains the harness section: the
   schedule grammar, the round bound, and what a seed means.

Done means: `mvn verify` runs N seeded schedules on the clean transport and N on
the chaos transport, in a headless JVM, with no socket bound and no sleep.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-051 | The lifecycle claims of the session are only checkable against replicas that can be driven deterministically. |
| FEAT-052 | Every convergence, anti-entropy, compaction and per-user-undo claim in the CRDT work is measured here. Building the merge rules first and the harness second means the rules are unfalsifiable while they are being written. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0037 | A replica applies a peer's op on a machine that may have no display. `CircuitOp.apply` takes a `java.awt.Graphics` at HEAD; until it takes the headless text-metrics seam, convergence is a function of the local toolkit and the harness cannot run headless at all. |
| TASK-0108 | The replica sends and receives through a `Session`'s codec and membership. Without it there is no envelope framing, no peer identity to stamp a `VectorClock` with, and nothing to attribute a rejected frame to. |

## Acceptance test

`test/jls/collab/ReplicaConvergenceTest.java`, new:

- `twoReplicasSaveByteIdenticalFilesAfterAnyInterleaving()` - N seeded schedules
  over `LoopbackTransport.pair()`, asserting equal `stateHash()` **and**, on
  failure, emitting the unified diff of the two canonical saves plus the seed.
- `chaosTransportDropDuplicateReorderStillConverges()` - the same schedules over
  `ChaosTransport`, asserting convergence within the stated round bound after
  the partition heals.
- `aDuplicatedEnvelopeIsAppliedExactlyOnce()` - the op-id dedup contract, tested
  through the harness rather than only through `CausalBufferTest`, because the
  interesting duplication is at the transport, not at the buffer's front door.
- `anOutOfCausalOrderEnvelopeIsBufferedNotApplied()` - assert the circuit is
  untouched until the dependency arrives.
- `replicaLoopRunsHeadless()` - the whole suite under
  `-Djava.awt.headless=true`, asserting no `HeadlessException` and no
  `Graphics` acquisition. This is the assertion that keeps TASK-0037's work from
  regressing.
- `divergenceIsReportedAsDivergenceAndNotAsTimeout()` - inject a deliberately
  non-commuting op pair and assert the harness reports "converged, different
  bytes" with both saves attached.

`test/jls/SocketConfinementRatchetTest` must stay green: the harness binds no
socket. If it needs one, it is testing the wrong layer.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2) | depends on / overlaps - this builds the apparatus #171's claims are measured on; #171 itself is closed by TASK-0110 |
| 169 | Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel (collab Stage 1b) | depends on - #169 names the seeded schedule runner as "to build - shared with Stage 2's convergence tests"; this is that runner |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b) | depends on - the harness replays `CircuitOp`s and round-trips them through `CircuitOpReader`; an unmigrated inline gesture is invisible to it |

## Notes

- **Byte equality is a real oracle only because of canonical save.** Element
  order is by stable id and file-local ids are assigned in that order, so a
  content-equal circuit saves identically regardless of history. If that ever
  stops being true, this harness silently becomes a history comparator.
- **`CausalBuffer` guarantees order and exactly-once, not commutativity.** It
  will happily deliver two ops that do not commute, in causal order, exactly
  once, to both replicas, and the replicas will diverge. That is the merge-rule
  defect TASK-0110 fixes; the harness's job is to *name* it, which is why the
  three-outcome distinction is a deliverable and not a nicety.
- **No sleeps, no wall clock.** Reachability timeouts in the harness are driven
  by an injected tick source, not `System.nanoTime`. A test that sleeps is a
  test that flakes on a loaded CI runner, and the long-run lane is not the place
  to discover that.
- **Round bound, not "eventually".** Every convergence assertion states a bounded
  number of delivery rounds. An unbounded claim cannot distinguish convergence
  from livelock.
- **Keep the harness in the test tree.** It is apparatus, not product; putting
  it in `src/` would put a chaos transport in the shipped jar.
- **Seeds are the deliverable of a failure.** A failing schedule is minimized and
  committed as a named regression, exactly as the roster convergence tests
  already do.

## Evidence

- `src/jls/collab/crdt/package-info.java` - the substrate's own statement that
  per-kind merge semantics, anti-entropy, compaction and collaborative undo
  "build on top of this substrate and remain issue #171 work in progress".
- `src/jls/collab/crdt/CausalBuffer.java` (173 lines),
  `OpEnvelope.java` (303), `VectorClock.java` (238), `OpId.java` (50) - the
  substrate that exists at HEAD, with `CausalBufferTest`, `OpEnvelopeTest` and
  `VectorClockTest` beside them.
- `src/jls/collab/net/LoopbackTransport.java:104,232` - the in-memory pair over
  bounded queues; `src/jls/collab/net/Transport.java:38` - the frame-channel
  seam the replication stack talks through.
- `test/jls/collab/net/ChaosTransport.java:23` - `final class ChaosTransport`,
  package-private, with `ChaosTransportTest` beside it.
- `src/jls/Circuit.java:1466-1512` (canonical save order), `:1548`
  (`stateHash`).
- `src/jls/collab/op/CircuitOpReader.java` - the strict reader that rejects
  rather than repairs; `src/jls/collab/op/CircuitOp.java` - the sealed
  vocabulary the harness replays.
- Do not restate: `docs/operation-layer.md` owns the op contract and its
  migration inventory; `docs/collaborative-editing-research.md` owns the
  replication design.
