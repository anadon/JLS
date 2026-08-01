# FEAT-052 - CRDT replication, collaborative undo and security hardening

**Status:** proposed | **Cost:** 14-22 mw | **Owner program:** P11 |
**Spine rank:** -

## Capability delivered

Concurrent edits from several peers converge to the same circuit with no server
deciding the order, undo is per-user rather than a shared stack, and everything
arriving from the network is checked against a closed op vocabulary with an
element-type allowlist and size caps, so a peer cannot introduce an element type
the receiving install did not agree to accept. Replication carries anti-entropy
so a peer that was disconnected catches up, and compaction so a long session
does not grow without bound.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | Concurrent edits must converge without a server, and network input must not be able to introduce an element type the peer did not allow |
| CAP-06 | beneficial | A shared classroom session must not let one peer inject an element type the other did not allow |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-051 | Replication needs a session to replicate over: identity, an encrypted link and a membership set are what an op envelope is addressed within |
| FEAT-015 | The op vocabulary must be complete and headless before it can be closed. An op set with four gestures still inline cannot be a closed vocabulary |
| FEAT-012 | Convergence produces files that were never written by one editor. The per-record-kind merge rules and the post-merge semantic validation are what stop a converged file from being a parsing-but-corrupt one |
| FEAT-001 | The element-type allowlist for network input is a registry-keyed table. Delegating it to the element registry without a tested deny list silently admits every type the registry gains, including ones that exist for batch use only |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0110 | Convergent replication, collaborative undo and input hardening | Op replication with anti-entropy and compaction, a convergent type for the model's ordered collections, per-user undo, and the closed vocabulary with caps and ratchets |
| TASK-0109 | The replica loop over a loopback transport | Shared with FEAT-051: the two-replica loop is where convergence is first observable |
| TASK-0032 | The per-record-kind merge rule table | Shared with FEAT-012: convergence and three-way merge are the same question asked at two different times |
| TASK-0037 | Headless op application and the complete op vocabulary | Shared with FEAT-015: a vocabulary cannot be closed until it is complete |

## Acceptance criteria

1. Concurrent op sets applied in any order on two replicas produce the same
   circuit, asserted by byte-identical saves.
2. A replica that misses a window of ops and reconnects reaches the same state
   through anti-entropy, without a full resynchronization.
3. The op log compacts under a stated policy, and compaction does not change
   the resulting circuit.
4. Undo undoes the local user's last operation and not another user's, and the
   inverse of every op in the closed vocabulary is exact.
5. An op naming an element type outside the allowlist is rejected by name; the
   allowlist is derived from the element registry minus an explicit, tested
   deny list, so a newly registered type does not become network-reachable by
   default.
6. Oversize and malformed network input is rejected against stated caps, with
   ratchet tests that fail if a cap is removed.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2) | closes |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting) | closes |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b) | depends on - the op layer this feature replicates; it is closed by FEAT-015, not here |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking - this feature and FEAT-051 are its two halves |

## Design notes

The allowlist in criterion 5 is a latent security regression the code actively
invites. The vocabulary class at HEAD is a hand-written token list whose own
javadoc instructs a future author to delegate to the element registry once that
registry lands. The registry has landed. A bare delegation would admit the
batch-only signal-generator stand-in from a network peer - exactly the widening
the hardening issue exists to prevent. The correct form is registry minus an
explicit, tested deny list, and this is the reason FEAT-001 is a real
prerequisite rather than an adjacency.

Convergence and three-way merge are the same question at two different times,
which is why the merge-rule table is shared rather than duplicated. If they are
authored independently they will produce two different answers for the same
record kind and the file that results from a merge will not be the file that
results from a converged session.

## Risks

- **Convergent types for ordered collections are the hard part.** Element
  ordering in the model is observable in the saved bytes, so a convergent type
  that reorders is a byte-identity failure, not a cosmetic one.
- **Per-user undo across a shared document has no obviously right semantics.**
  Undoing an operation another user has since built on is a decision, not a
  bug, and it must be recorded.
- **A closed vocabulary is a maintenance obligation.** Every new element type
  is a decision about network reachability, and the default must be closed.

## Evidence

- The hand-written allowlist and its own instruction to delegate:
  `src/jls/collab/op/ElementVocabulary.java`, with the element registry it would
  delegate to at `src/jls/elem/ElementRegistry.java`.
- The replication primitives already in the tree: `src/jls/collab/crdt/`
  (`CausalBuffer`, `OpEnvelope`, `OpId`, `VectorClock`).
- The op layer and its remaining inline gestures: `docs/operation-layer.md`,
  `src/jls/collab/op/`.
- The vocabulary contract: `docs/collab-vocabulary.md`; the research record:
  `docs/collaborative-editing-research.md`.
- Issues #171, #170, #167 and #163, all open, verified against
  `list_issues(state=OPEN)`.
- Owner: P11 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 14-22 mw; TASK-0110, TASK-0109, TASK-0032 and
  TASK-0037 total 8 wk, of which three are shared with FEAT-051, FEAT-012 and
  FEAT-015 and counted once at the task level. The unshared remainder is 2 wk
  against a 14-22 mw band; the residual is the convergent type for the model's
  ordered collections and the anti-entropy protocol, which no task id names.
