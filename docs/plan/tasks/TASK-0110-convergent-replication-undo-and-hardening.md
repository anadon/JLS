# TASK-0110 - Convergent replication, collaborative undo and input hardening

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0032, TASK-0109

## Deliverable

Concurrent edits converge without a server, undo is per-user, and network input
cannot introduce an element type the peer did not allow.

Precisely what changes:

1. **Per-kind merge semantics**, one class per rule, each implementing the row
   TASK-0032 wrote:
   - `jls/collab/crdt/ElementSet.java` - add-wins observed-remove set over
     stable ids, so a concurrent add and remove of the same element resolves to
     present rather than to a dangling reference.
   - `jls/collab/crdt/AttributeRegister.java` - per-attribute last-writer-wins
     with the tie broken by `PeerId`, never by wall clock.
   - `jls/collab/crdt/WireSet.java` - OR-set over wire nets, because a wire is
     identified by its endpoints and a concurrent redraw must not produce two.
   - `jls/collab/crdt/OrderedRows.java` - RGA for the ordered collections
     (state-machine transitions, truth-table rows, signal-generator programs) -
     the `EditOrderedRows` op that `docs/operation-layer.md` defers to this
     stage.
2. **Anti-entropy and compaction.** A periodic digest exchange over
   `VectorClock` finds gaps; a peer requests the missing envelopes by `OpId`.
   Compaction replaces a fully-acknowledged prefix of the log with a snapshot,
   with the acknowledged frontier defined as the pointwise minimum of the
   rosters' clocks - so a peer that is unreachable but not removed does not
   lose its history.
3. **Per-user undo.** Each replica keeps its own stack of the ops **it**
   submitted. Undo submits the inverse computed against the *current* circuit,
   not the stale pre-apply inverse `CircuitOp.invert()` returns - see Notes.
   An undo that can no longer apply is reported, not silently skipped.
4. **The element-type allowlist reconciled with the registry, narrowly.**
   `src/jls/collab/op/ElementVocabulary.java:39-45` is a hand-maintained 34-token
   set whose own javadoc (`:26-30`) says it "should delegate to" the element
   registry once that registry exists. The registry exists
   (`src/jls/elem/ElementRegistry.java:38`, **35 types**). The delegation is
   `registry minus an explicit deny list` - not a bare delegation, which would
   silently admit `TestGen` from a network peer.
5. **Caps and ratchets on network input**, per `docs/collab-vocabulary.md`'s caps
   section: bounded op count per frame, bounded element count per op, bounded
   string lengths, bounded coordinates - all checked before allocation - plus
   the ratchet tests that fail when a new op kind ships without a cap.
6. **`docs/collab-vocabulary.md`** gains the merge-rule column so the payload
   grammar and the convergence rules are one document, and
   `docs/operation-layer.md`'s deferred `EditOrderedRows` row is closed.

Done means: the TASK-0109 harness reports "converged, identical bytes" on every
seeded schedule including the non-commuting pairs that previously diverged, and
no network-reachable path can name a type outside the allowlist.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-052 | The feature in full: convergence, per-user undo, and the hardened network surface. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0032 | This implements the per-record-kind merge rule table. Writing the rules and implementing them in one change means the rules are whatever the code happened to do; the table is the specification the tests are generated from, one method per row. |
| TASK-0109 | Every claim here is a convergence claim, and a convergence claim is only checkable against two replicas and a seeded schedule. Without the harness the merge rules ship unfalsified. |

## Acceptance test

`test/jls/collab/crdt/MergeRuleTest.java`, new - **one method per row of
TASK-0032's table**, each driving the TASK-0109 harness with the concurrent pair
the row describes:

- `concurrentAddAndRemoveOfTheSameElementResolvesToPresent()`;
- `concurrentAttributeWritesResolveByPeerIdNotByClock()` - run the same schedule
  with the two peers' clocks skewed in opposite directions and assert the result
  is unchanged;
- `concurrentWireRedrawProducesOneNetNotTwo()`;
- `concurrentOrderedRowInsertsPreserveBothAndAgreeOnOrder()`;
- `everyTableRowHasATest()` - reflectively cross-check the method set against the
  committed table, so a new rule cannot ship untested.

`test/jls/collab/crdt/AntiEntropyTest`:

- `aPeerThatMissedEnvelopesRecoversThemByDigestExchange()`;
- `compactionNeverDropsHistoryAPeerStillNeeds()` - one peer unreachable but not
  removed across the compaction point, then reachable again.

`test/jls/collab/CollaborativeUndoTest`:

- `undoAffectsOnlyTheUndoersOwnOps()`;
- `undoAfterARemoteOpTouchedTheSameElementIsReportedNotSilentlyWrong()` - the
  stale-inverse hazard, asserted.

`test/jls/CollabSecurityRatchetTest`, extended:

- `theNetworkAllowlistIsTheRegistryMinusTheDenyList()` - assert the deny list is
  non-empty and contains `TestGen`, and that the allowlist has not grown
  silently.
- `everyOpKindHasACapAndEveryCapIsChecked()` - reflective over
  `CircuitOp`'s sealed permits list, so a new op kind fails until it is capped.
- The existing `collabDoesNoReflection` (`:87-97`),
  `classForNameStaysAtItsPinnedSites` (`:110-126`),
  `javaObjectSerializationIsBannedEverywhere` (`:48-66`) and
  `socketsAppearOnlyUnderCollabNet` (`:68-80`) stay green unmodified.

`test/jls/collab/op/ElementVocabularyTest` keeps its three-way cross-check
(this list, the writer literals, the palette contract) green against the
per-view palette of TASK-0105.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2) | closes |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting) | closes |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b) | depends on - a gesture that still mutates inline is invisible to replication; the vocabulary must be closed first |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - `ElementVocabulary`'s javadoc says the reconciliation "is to be recorded on issue #78"; record it there |

## Notes

- **The naive registry delegation is a security regression, and the code invites
  it.** `ElementVocabulary` says to delegate once the registry lands. The
  registry has 35 types; the allowlist has 34. The difference is `TestGen`, the
  batch-only stand-in for a signal generator - a type a peer should never be
  able to name. Delegate through a deny list with a test, and record the
  reconciliation on the issue the javadoc names.
- **A recorded inverse goes stale under concurrency.** `CircuitOp.invert()`
  computes an exact inverse **against the pre-apply circuit**, which is correct
  for single-user snapshot undo and wrong the moment a remote op touched the
  same element in between. Per-user undo must recompute against current state
  and report when it cannot; a silently-skipped undo is the worst outcome
  because the user believes it happened.
- **Undo is per-user, not per-replica.** Undoing another peer's op is a
  different feature with different UX and different consent questions. Do not
  ship it accidentally by keying the stack on the replica instead of the peer.
- **LWW must never read a clock.** Wall-clock tie-breaks make convergence a
  function of NTP. `PeerId` order is the tie-break, and the skewed-clock test
  above is what keeps it that way.
- **Compaction is where history is lost for good.** Get the acknowledged
  frontier from the roster, not from the transport's view of who is connected -
  unreachable is not removed.
- **`ArchitectureRulesTest.replicationStackDependsDownwardOnly`**
  (`test/jls/ArchitectureRulesTest.java:226-247`) constrains the direction of
  dependencies inside `jls.collab`; the merge-rule classes belong in `crdt`,
  which may depend on `op`, not the reverse.

## Evidence

- `src/jls/collab/op/ElementVocabulary.java:26-30` (the delegation note naming
  #78), `:39-45` (the 34-token `ALLOWED` set);
  `src/jls/elem/ElementRegistry.java:38` (35 registered types).
- `src/jls/collab/crdt/package-info.java` - names exactly this task's scope as
  remaining: "add-wins element set, per-attribute last-writer-wins, OR-set
  wires, RGA sequences), anti-entropy resync, log compaction, and collaborative
  undo build on top of this substrate and remain issue #171 work in progress".
- `src/jls/collab/op/package-info.java` - the inverse contract ("computes an
  exact inverse against the pre-apply circuit") and the statement that this
  grammar "is the future network surface (issue #170)".
- `docs/operation-layer.md` - the mutation-site inventory, whose
  `EditOrderedRows` row is "deferred - Stage 2 sequence semantics (#163)", and
  the "What lands next" item 3 stating that op-inverse undo is explicitly not
  the previous stage.
- `test/jls/CollabSecurityRatchetTest.java:40` (the class),
  `:48-66,68-80,87-97,110-126` (the four standing ratchets).
- `test/jls/ArchitectureRulesTest.java:226-247` - the downward-only rule.
- Do not restate: `docs/collab-vocabulary.md` owns the payload kinds, caps and
  prohibitions; `docs/collaborative-editing-research.md` owns the CRDT design.
