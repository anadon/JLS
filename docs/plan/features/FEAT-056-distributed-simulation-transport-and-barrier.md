# FEAT-056 - Distributed simulation transport and barrier protocol

**Status:** proposed | **Cost:** 10-18 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

Partitions of one design, running in separate processes on separate hosts,
exchange boundary events under a synchronization discipline whose result does
not depend on how many partitions there are or on the order messages arrive. The
observable output of a distributed run is byte-identical to the output of the
same design run whole, and the partition count is not recoverable from the
result.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-17 | required | Cross-partition event exchange with a deterministic synchronization discipline is the capacity axis itself |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-055 | There is nothing to synchronize between until the design exists as parts with named boundaries |
| FEAT-051 | The networking abstraction, its loopback and its chaos test double already exist and are already the only package permitted to open a socket; a second networking stack would be a second security surface |
| FEAT-035 | A distributed run that cannot be suspended cannot be scheduled on a shared cluster, and partition state must be serializable to be moved |
| FEAT-014 | A watched element inside one partition needs a name that does not depend on which partition it landed in |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was added with CAP-17 after it closed | - |

## Acceptance criteria

1. The same design run at partition counts 1, 2, 4 and 8 produces byte-identical
   watched output and waveform dump. A result that varies with partition count
   is a failure, not a tolerance.
2. Message arrival order does not affect the result, asserted under the existing
   chaos transport rather than only under the loopback.
3. The synchronization discipline is conservative: no committed simulation time
   is ever un-committed, because no rollback machinery exists in the engine and
   adding it is a separate, larger program.
4. A design whose lookahead is too low for the discipline is refused by name
   rather than run slowly and silently.
5. A partitioned run can be checkpointed and resumed, and the resumed run
   produces the byte-identical continuation.
6. No package outside the networking package opens a socket, preserved by the
   existing architecture rule.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | Distributed simulation transport and the barrier protocol | **no issue** |
| 77 | Extract a headless `jls.core`: the simulator base class imports Swing, `Circuit` holds an editor reference, and `JLSInfo` is a global mutable hub | depends on, **closed** - a partition must be constructible and simulable with no graphical toolkit present, which is the property that extraction established |

## Design notes

Criterion 3 decides the architecture and it is decided by a measured absence
rather than by preference: there is no cancel, withdraw or rollback path
anywhere in the simulation package, so an optimistic discipline would have to
build one first. That is the same absence that shaped the analog synchronization
design, and it should be cited as a fact rather than re-argued.

Reusing the collaboration transport is not thrift for its own sake. That package
is the only one the architecture rules permit to open a socket, and it already
ships a loopback and a chaos double that criteria 1 and 2 need.

## Risks

- **Conservative synchronization stalls on low lookahead.** Criterion 4 turns
  that from a mystery slowdown into a refusal, but the refusal will be hit by
  real designs.
- **Byte-identical output across hosts inherits an unverified assumption.** The
  parity contract records cross-platform run determinism as unasserted; every
  byte-identity claim here rests on it.
- **A distributed run that answers differently from a local run destroys the
  golden discipline** at exactly the scale where hand-checking is impossible.
  There is no tolerance-based fallback for a digital simulation.

## Evidence

- No rollback machinery at HEAD: a search of `src/jls/sim/` for cancel,
  withdraw or rollback returns nothing.
- The transport this feature reuses: `src/jls/collab/net/Transport.java`,
  `LoopbackTransport.java`, and the chaos double in the test tree; the
  socket-confinement architecture rule in `test/jls/ArchitectureRulesTest.java`.
- The unasserted cross-platform determinism this feature depends on:
  `docs/parity-contract.md` §5.1.
- Issue #77, closed, verified by direct read.
- Owner: **UNOWNED**; added with CAP-17 after the capability roadmap was
  committed.
- **Cost reconciliation.** Band 10-18 mw with no tasks; part of CAP-17's own
  38-62 mw arithmetic for its four new features. Not a task rollup.
