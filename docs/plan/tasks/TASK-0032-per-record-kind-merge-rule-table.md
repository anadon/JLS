# TASK-0032 - The per-record-kind merge rule table

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0005, TASK-0031

## Deliverable

The merge rule table, written once, in a state-based form, offline first - and
therefore usable as the executable specification and test oracle of the online
CRDT rather than as a second, incompatible answer to the same question.

1. **The table itself, as data.** One row per record kind and per situation, in
   `docs/merge-rules.md`, with a STRICT column (the offline git behavior, which
   may report a conflict) and an AUTO column (the online behavior, which must be
   total). The AUTO column is derived from STRICT by appending exactly one
   deterministic tiebreak - `(Lamport counter, peer id)` - to each conflict
   class. The rows are enumerated in
   `docs/capability-roadmap/lf-06-diff-merge-vcs.md:412-436`; this task turns
   them into a normative table with a test per row, it does not re-derive them.
2. **`jls.merge.MergeRules`**, a data-only class holding the table keyed by
   `(record kind, situation)`, so the table has exactly one representation and
   the document is generated from or checked against it.
3. **`jls.merge.ThreeWayMerge`**, taking base, ours and theirs as loaded
   `Circuit`s and emitting a `List<CircuitOp>` submitted through `OpSink`, or a
   list of conflicts. Emitting **ops** is the load-bearing design decision: the
   merger then inherits every invariant the editor enforces - atomic
   validate-then-mutate, name-collision rejection, jump-start/jump-end cascade
   rules, tri-state net re-arming - for free. A merge expressed as ops cannot
   produce a file JLS refuses to load, and no textual merge can promise that.
4. **A git merge driver entry point** plus the `.gitattributes` line and the
   `-merge base ours theirs` flag on `JLSStart.FLAGS`, so the same code serves
   git, CI and a person.
5. **The cross-check property, which is the point of building offline first.**
   For any two op sequences A and B from a common base,
   `merge3(base, apply(base,A), apply(base,B))` must canonical-save
   byte-identically to the state two replicas reach after exchanging A and B
   through `jls.collab.crdt.CausalBuffer`. If that property holds, the offline
   tool tests the online tool, and collaboration Stage 2 gains an oracle it
   does not have.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-012 | The feature is this table plus TASK-0031's validator. Together they are the whole "either loads and elaborates or is a reported conflict" contract |
| FEAT-052 | The AUTO column **is** the CRDT's merge policy. Building it here means the online work ships a tiebreak, not a merge semantics |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0005 | Every rule is keyed by stable id. Against dense save-time ids - reassigned on every save - "the same element on both sides" is not expressible |
| TASK-0031 | The STRICT column's non-conflict outcome is "the merged circuit is valid". Without the semantic check, "valid" means "parsed", and the row that justifies the whole design (two sides independently naming a pin `out`) is precisely the one that parses |

## Acceptance test

`test/jls/merge/MergeRuleTableTest.java`, new - **one test method per table
row**, which is the deliverable's own acceptance criterion made literal:

- `bothSidesAddDifferentElementsMergesBoth()`
- `bothSidesAddTheSameStableIdRefusesTheMerge()` - reachable only on legacy ids;
  asserts refusal of the whole merge, not of one element.
- `disjointAttributeEditsOnOneElementMergeBoth()`
- `conflictingEditsToOneAttributeConflictUnderStrict()`
- `geometryOnlyConflictsResolveToOursAndAreNoted()` - asserts the note is
  emitted, not merely that ours won.
- `deleteVersusEditConflicts()` / `deleteVersusWireConflicts()`
- `bothSidesRerouteOneNetConflictsAtNetGranularity()`
- `independentIdenticalPinNamesProduceAValidDeltaAndAnInvalidCircuit()` - the
  row that justifies the design: asserts the delta is clean **and** that
  TASK-0031's check reports it, so the failure surfaces at the merge, not at
  the user's next simulation.
- `anOrderedSubstructureTouchedByBothConflictsWhole()`
- `theTableHasNoRowWithoutATest()` - reflective: enumerates `MergeRules`'
  entries and asserts each is named by a test method. **This is the assertion
  that keeps the table and its coverage from diverging**, and it fails if the
  table is empty.

`test/jls/merge/MergeConvergenceTest.java`, new:

- `offlineMergeAgreesWithReplicaExchange()` - the §C4 item-6 property, as a
  randomized property test over generated op sequences with a fixed seed. No
  socket: it drives `CausalBuffer` directly. Oracle is canonical-save byte
  equality, which `DeterministicSaveTest` already establishes as the
  convergence oracle.
- `everyAutoOutcomeIsTotal()` - asserts the AUTO column produces a result for
  every situation the STRICT column can conflict on. An online merge that can
  ask a question is a bug.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2) | depends on / informs - #171 needs a merge policy; this task produces it and its oracle. The AUTO column is #171's specification |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | depends on - shipped; `jls.collab.op` is the output type this merger emits into |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | overlaps - a merged file from a colleague and an op from a peer are the same untrusted-input class, and `ElementVocabulary` is already the type-token gate |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking |

No issue covers offline merge or the rule table. The registry records the whole
diff/merge/format line - FEAT-003, FEAT-012, FEAT-013, FEAT-014 - as having none.

## Notes

- **`src/jls/collab/crdt/` does not give merge semantics, and knowing exactly
  why prevents two weeks of wrong work.** `VectorClock` answers
  BEFORE/AFTER/CONCURRENT over *peer observation states*; two git branch tips
  carry no such state and no op log is persisted, so a clock could only be
  reconstructed from something that does not exist. `CausalBuffer` is a
  *delivery* discipline and git already delivers exactly once - its own javadoc
  disclaims the merge. `OpEnvelope` is transport metadata that is meaningless
  offline.
- **`CircuitOp` is a sealed interface over 11 permits**
  (`src/jls/collab/op/CircuitOp.java:33-36`) and `CircuitOpReader` dispatches on
  a string switch with one `case` per kind (`:119-176`). If the merger needs a
  verb the vocabulary lacks, widening the permits list breaks both, and the
  switch will not stop compiling because it is on `String`. That silence is the
  trap: add a `default` that throws, or make the reader table-driven off the
  same enumeration the merger uses.
- **Build the offline (partial) direction first, deliberately.** Online merge
  must be total - you cannot pause a peer's typing to ask a question - so it
  resolves every concurrency by tiebreak. Offline may be partial, because git
  has a first-class notion of conflict, and a partial merge that reports a
  conflict strictly dominates a total one that silently picks a winner.
- **`ElementVocabulary` is a stopgap constant list**
  (`src/jls/collab/op/ElementVocabulary.java:26-30`) that its own javadoc says
  should delegate to the #78 element registry once it exists. The registry
  exists at HEAD (`ElementRegistry.ALL`, 35 entries). Reconciling them is
  TASK-0001's, not this task's - but a merger that reads the stale list will
  reject `RegisterFile` and `FieldExtend`. Check before shipping.
- **Reference `docs/operation-layer.md` for op invariants; do not restate
  them.** It is the normative description of what an op guarantees, and item 3's
  entire argument rests on those guarantees being someone else's to maintain.
- **`.gitattributes` already exists** and carries `*.jls -text` with a stated
  reason (issue #111). A merge-driver line joins it; it does not replace it.

## Evidence

- `docs/capability-roadmap/lf-06-diff-merge-vcs.md:347-411` - the analysis of
  why the CRDT package does not supply merge, the six-link argument for
  building offline first, and the stated architectural insight; `:412-436` -
  the ten-row conflict taxonomy in both columns; `:437-446` - the pin-name row
  and the acceptance criterion.
- `src/jls/collab/op/CircuitOp.java:8-36` - the closed-vocabulary rationale,
  the validate-then-mutate and invertibility contract, and the sealed permits
  list; `:37-51` - `apply`'s throw-before-mutate guarantee.
- `src/jls/collab/op/CircuitOpReader.java:119-176` - the string switch, one
  case per op kind.
- `src/jls/collab/op/ElementVocabulary.java:1-40` - the closed element-type
  allowlist for network input and its stopgap note.
- `src/jls/collab/crdt/` at HEAD - `VectorClock.java`, `CausalBuffer.java`,
  `OpEnvelope.java`, `OpId.java`; the four files the analysis above is about.
- `docs/file-format.md:366-421` - stable ids and canonical order, the
  convergence oracle the property test uses.
- `test/jls/DeterministicSaveTest.java`, `test/jls/CollabSecurityRatchetTest.java` -
  the existing oracle and the existing hardening ratchet.
- `.gitattributes:1-5` - the `*.jls -text` rule.
