# CAP-01 - Multi-discipline multi-user simultaneous development

**Status:** proposed | **Priority:** 5 | **Marginal cost:** 30-46 mw |
**Standalone cost:** 49-75 mw

## Outcome

Several people, each working in a different view of one circuit - schematic,
waveform, board - edit it at the same time with no server and no turn-taking,
and every replica writes byte-identical files.

## Acceptance test

**SEEN.** Three machines on a LAN, one 4-bit ALU. Alice (CS) rewires the carry
chain in the schematic view. Bob (ECE) attaches probes and toggles watched
signals in the waveform view. Carol (EE) assigns footprints and drags packages
in the board view. All three edit simultaneously. Each sees peers' cursors in
their own view and a badge on the shared object for a peer working elsewhere -
"Carol - board" beside U3 in Alice's schematic. Alice deletes a gate Carol just
footprinted: one attributed conflict banner, and the footprint record is
tombstoned **with** the element rather than orphaned. All three save; the three
files are byte-identical, and the peer panel said so before they saved.

**CHECK - three named tests.**

1. **`CircuitConvergenceTest`**, a structural sibling of the shipped
   `test/jls/collab/session/RosterConvergenceTest.java`. N headless replicas
   (N in 2..6) load one fixture from bytes; a seeded generator emits per-replica
   op streams over **all** op kinds including the view-qualified ones, biased
   toward collisions and containing the delete-versus-wire divergence witness as
   a named case; envelopes travel through `test/jls/collab/net/ChaosTransport.java`
   (seeded drop, duplicate, reorder, partition, heal); exactly **one** bounded
   anti-entropy round, so convergence cannot hide behind "eventually". Asserts:
   (a) every replica's `Circuit.save` bytes are identical; (b) the converged
   circuit **loads and elaborates** - `Circuit.finishLoad` and `WireNet.makeNet`
   both succeed, because parsing is not the criterion, elaboration is;
   (c) `CausalBuffer.pendingCount() == 0` everywhere; (d) replaying a replica's
   own op inverses returns it to the fixture bytes.
2. **`ViewIsolationTest`** - replica A emits only schematic ops, replica B only
   board ops, over the same elements; must converge with **zero** conflicts.
   This is the multi-discipline claim as an executable statement. If it fails,
   per-view separation is fictional.
3. **`OldReaderTest`** - a reader at the previous format epoch opening a file
   with a must-understand VIEW section refuses to save, does not lose the
   section, and says why.

**FALSIFICATION GUARD.** `OldReaderTest`'s negative arm must fail today for a
verified reason: `Element.setValue` returns silently on an unknown attribute
name and the loader calls it unconditionally, so an older JLS opening and
re-saving a file whose view data lives on elements destroys the EE's work with
exit status 0. The guard cannot pass by accident while that code is at HEAD.

## Demo slice

Two headless replicas over the in-tree loopback transport, schematic ops only,
asserting byte-identical `Circuit.save` and `pendingCount() == 0` -
TASK-0037 + TASK-0109 = **~4 maintainer-weeks** on top of the shipped op layer.
It proves convergence is real before any view work or any GUI work is funded.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | required/beneficial |
|---|---|---|---|
| FEAT-051 | P2P session foundation and shared session v1 | Without verified identity, an encrypted transport and membership there is no session for ops to travel over | required |
| FEAT-052 | CRDT replication, collaborative undo and security hardening | Concurrent edits must converge without a server, and network input must not be able to introduce an element type the peer did not allow | required |
| FEAT-015 | The headless, programmatic `CircuitOp` layer | Replication replays ops; ops that need a `Graphics` cannot be replayed headlessly, and the vocabulary must be closed before it can be made total under concurrency | required |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | "One artifact, N views" is exactly this feature; without per-view geometry the disciplines collide on one geometry record | required |
| FEAT-013 | Per-section internal versioning with must-understand semantics | The VIEW section must be refusable by an old reader rather than silently dropped - this is `OldReaderTest` | required |
| FEAT-003 | Uncompressed canonical default with stable-id references | Byte-identical save is the convergence oracle; dense reassigned ids make that oracle noise | required |
| FEAT-012 | Semantic merge safety and per-kind merge rules | `OpRejected` is not a merge outcome; the per-record-kind rule table is what makes the 11 op kinds total under concurrency | required |
| FEAT-002 | Fail-loud loader and attribute dispatch | Supplies the falsification guard, and a replica that silently drops an attribute converges to the wrong thing | required |
| FEAT-017 | Shared and parameterized subcircuit definitions | Concurrent edits to a definition instantiated N times must land once, not N times divergently | required |
| FEAT-016 | Subcircuit type identity, VLNV and the circuit-library format | Peers must agree on which definition they are editing | beneficial |
| FEAT-008 | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | Peer cursors, the peer panel and the conflict banner are editor surface, and it is currently unfloored and untestable | required |
| FEAT-050 | Module runtime consumed: extension points and providers | The per-view palette and per-view element contributions dispatch through the registry that boots and is not read | beneficial |
| FEAT-030 | Engine constant factors: the semantics-preserving stack | Three simultaneous editors on one circuit raise per-edit cost; K9's floor is continuous | beneficial |
| FEAT-031 | The per-instance fidelity toggle and its boundary harness | Lets a team member run one subcircuit behaviorally while another edits it structurally | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking - this capstone is its multi-discipline reading |
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo | closes |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | closes |
| 169 | Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel | closes |
| 168 | P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification | closes |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | depends on / closes its headless half |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on (via FEAT-008) |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on |
| 84 | Decompose `SimpleEditor` | depends on |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | informs (via FEAT-050) |
| - | Per-view geometry, stable addressing and the format epoch (FEAT-013, FEAT-014) | **no issue** |
| - | Semantic merge safety and the per-record-kind rule table (FEAT-012) | **no issue** |
| - | CAP-01 as a capstone | **no issue** - only its collaboration half (#163) is tracked |

## Open decisions

1. **The instance-addressing scheme.** Recommendation: `view:instancePath:sid`,
   decided **before** TASK-0035. Reason: it is the shared key for the CRDT, the
   per-view geometry section and the conflict banner; three subsystems will each
   invent one otherwise.
2. **The op-grammar shape.** Recommendation: extend the existing `CircuitOp`
   grammar **once**, deliberately, to carry a view discriminator, rather than
   adding a second grammar for views. Reason: D9's synthesis - multi-view and
   collaboration are one design problem, and two grammars means two merge
   tables.
3. **The multi-discipline merge unit.** Recommendation: **field granularity, not
   block granularity.** `SetElementConfig(ElementId, String block)` carries the
   element's entire serialized save block, so any last-writer-wins rule discards
   one discipline's whole edit including fields the other never touched. The fix
   is a per-view sidecar plus a typed `SetAttribute` op over the existing
   `Element.savedAttributes()` enumeration.
4. **The format epoch.** Recommendation: one epoch carrying stable-id references,
   section framing and the VIEW section together. Reason: shipping them
   separately ships a diff regression and then fixes it.
5. **Floor control: does any view get an exclusive lock?** Recommendation: **no
   locks**, presence and attribution only. Reason: a token is the weaker reading
   this capstone exists to exclude.

## Kill criteria

- **KC-01-1.** `CircuitConvergenceTest` cannot be made to pass over all 11 op
  kinds after FEAT-012 lands: the capstone narrows to multi-user
  single-view, the op set that cannot be made total is named, and the
  multi-discipline claim is withdrawn in writing rather than left implied.
- **KC-01-2.** `ViewIsolationTest` reports any nonzero conflict count with
  disjoint per-view op streams: per-view separation is fictional and FEAT-014
  stops at the failing case. This is the criterion that distinguishes CAP-01
  from ordinary multi-user editing.
- **KC-01-3.** K9. Per-edit cost with three connected replicas exceeds the
  single-editor HEAD figures (58 ms at 10,000 elements, 552 ms at 100,000) by
  more than 2x: the responsible feature stops. The pedagogy audience is the
  product.
- **KC-01-4.** More than 16 maintainer-weeks into FEAT-051 plus FEAT-052 without
  two headless replicas converging over `ChaosTransport`: the transport is
  replaced with a relayed one and the pure-P2P claim in #163 is re-argued on
  cost.
- **KC-01-5.** The op vocabulary cannot be closed - a new op kind is still being
  added after TASK-0037 declares it complete: security hardening (#170) has no
  stable surface to allowlist against, and network input is disabled until it
  does.

## Evidence

- Op layer shipped: `#167` closed for its command half; `CausalBuffer`'s own
  javadoc records that merge rules "layered above this buffer" are out of scope
  for delivery, and no such rules exist in the tree.
- The divergence witness, verified: replicas A and B start byte-identical with
  unwired `AndGate` E and neighbor N. A issues `RemoveElements([E])` (valid -
  `RemoveElements.java:126-137` requires unwired); B concurrently issues
  `AddWire(attach=[E,N])`. Causal delivery is satisfied on both sides and the
  canonical saves differ. See `10-capstone-plan.md` §1.1.
- The multi-discipline-specific defect: `src/jls/collab/op/SetElementConfig.java:48-53`
  documents that the op carries "the reconfigured element's serialized block".
  There is no field granularity to merge at today.
- Falsification guard anchors: `src/jls/elem/Element.java:344-351`;
  `src/jls/Circuit.java:1067, 1078, 1089, 1105, 1116`. Verified at HEAD.
- Shipped precedent for the convergence harness:
  `test/jls/collab/session/RosterConvergenceTest.java` (1000 seeded schedules,
  80 steps, up to 6 peers, seed printed on failure) and
  `test/jls/collab/net/ChaosTransport.java`.
- Byte-identical save as an oracle rests on canonical order and stable ids,
  described in `docs/file-format.md` §8. Referenced, not restated.
- D9 is the binding ruling that puts CS, ECE and EE on one trajectory and makes
  multi-view and collaboration one program rather than two; D2 makes diff
  stability a requirement; D10 forbids refusing any of this for absence of
  demand.
- Owner programs: FEAT-003, FEAT-012 and FEAT-052 under P11; FEAT-014 and
  FEAT-004 under P3; FEAT-015 and FEAT-050 under P12; FEAT-016 and FEAT-017
  under P7; FEAT-031 under P8. FEAT-051 and FEAT-008 are **UNOWNED**.
- Marginal band 30-46 mw assumes the spine features (FEAT-003, 013, 014, 015)
  are funded by CAP-00 and the format epoch; standalone 49-75 mw is the same set
  with nothing shared (`10-capstone-plan.md` §3.1, row C1).
