# TASK-0108 - Session foundation: identity, transport, membership and sync

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

The wiring that turns three finished, unwired subsystems into a session two
people can actually join. At HEAD the crypto core is complete, the roster and
reachability tracker are complete, and **`Roster` and `ReachabilityTracker` have
zero consumers outside their own package** - verified by grep over `src/`.

Precisely what changes:

1. **`src/jls/collab/session/Session.java`**, new - the object that owns a
   `Transport`, a `Roster`, a `ReachabilityTracker` and a frame codec, and turns
   frames into roster entries and back. It is the first thing in the tree that
   calls `Roster.receive` (`src/jls/collab/session/Roster.java:116`) and the
   five `propose*` methods (`:150-235`) from outside a test.
2. **The wire grammar for session frames.** `docs/collab-vocabulary.md` already
   owns the payload kinds and the caps; this task implements the codec for the
   session half - signed `SessionEntry` records, heartbeats, presence frames and
   snapshot chunks - with the hostile-input discipline the net package already
   follows: length caps checked before allocation, typed rejection, never
   repair.
3. **Snapshot sync, across a layering wall.**
   `src/jls/edit/CircuitSnapshot.java:60,85` already implements "state to bytes"
   and "bytes to state through the hardened load path" - but it lives in
   `jls.edit`, and `ArchitectureRulesTest.transportKnowsNothingOfCircuits`
   (`test/jls/ArchitectureRulesTest.java:173-200`) forbids the transport layer
   from knowing about circuits. **The session takes a byte-supplier/byte-consumer
   seam**; the editor supplies `CircuitSnapshot::capture` and a restore callback.
   Do not move `CircuitSnapshot` into `jls.collab` and do not import `jls.Circuit`
   into `jls.collab.session`.
4. **Heartbeat and floor control wired.** A scheduled heartbeat drives
   `ReachabilityTracker`; `Roster.tokenHolder()` (`:284`) gates
   `OpSink.submit` in `src/jls/edit/SimpleEditor.java:5547-5570`, so a
   non-holder's gesture is **rejected with a visible read-only affordance**, never
   silently dropped. Token reclaim on holder timeout is a roster entry like any
   other, so it inherits the same epoch fence.
5. **`src/jls/collab/ui/`**, new and the only collaboration package permitted
   Swing (`ArchitectureRulesTest.collabLayersAreHeadless`,
   `test/jls/ArchitectureRulesTest.java:150-172`, names it as the exception):
   the Share gesture, the join/verify dialog rendering the seven SAS glyphs, the
   key-change warning, the peer panel (members, verified badge, reachability,
   last-seen, attribution colors, sync-state indicator, token controls, partition
   banner), and rate-limited presence overlays drawn in `paintComponent`.
6. **Any-member frame forwarding** so one reachable member suffices for
   connectivity. No other NAT machinery.

Done means: two installs on one LAN join, verify out of band, pass the token,
edit, drop and rejoin, and end with identical rosters and identical canonical
saves.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-051 | This is the feature's substance: the session that the finished crypto and the finished roster were built for. |

## Prerequisite tasks

None. Every piece this assembles exists at HEAD:
`IdentityKey`/`Handshake`/`Sas`/`SecureLink`/`KnownPeers`/`SessionListener`/
`SocketSession`/`Transport`/`LoopbackTransport` in `jls.collab.net` (3,367
lines excluding `package-info`), `Roster`/`ReachabilityTracker`/`SessionEntry`/
`PeerId` in `jls.collab.session` (756 lines), and `CircuitSnapshot` in
`jls.edit`. The gap
is that nothing joins them.

## Acceptance test

`test/jls/collab/session/SessionLifecycleTest.java`, new, over
`LoopbackTransport.pair()` (`src/jls/collab/net/LoopbackTransport.java:104`) and
the seeded chaos decorator:

- `joinLeaveEjectConvergesTheRosterOnAllReachablePeers()` - 1,000 seeded
  randomized schedules over two- and three-instance sessions (join, verify,
  edit, pass token, drop a peer, rejoin, eject), asserting identical
  `Roster.members()` and identical `Circuit.stateHash()`
  (`src/jls/Circuit.java:1548`) on every reachable peer **within a bounded number
  of rounds**, not "eventually" - an unbounded convergence claim hides livelock.
- `killingTheTokenHolderMidEditReleasesTheTokenAfterReclaimTimeout()` -
  asserting followers become editable and the session continues from the last
  received snapshot.
- `aNonHolderSubmitIsRejectedNotSilentlyDropped()` - asserted at the model
  level: `OpSink.submit` throws `OpRejected`, and the editor's read-only
  affordance is asserted through the UI harness.
- `heartbeatLossMarksAPeerUnreachableWithoutRemovingIt()` and
  `aRejoinWithAKnownKeySkipsReVerification()`.
- `concurrentSameEpochProposalsResolveByLowestProposerId()` - the loser
  re-issues; asserted directly rather than through a schedule, so a failure
  points at the rule instead of at a seed.

`test/jls/collab/session/SnapshotSyncTest.aTornSnapshotIsRejectedByTheLoadPathNotPartiallyApplied()`
- the hardened load path is the absorber for a truncated snapshot; assert the
follower's circuit is unchanged, not half-applied.

`test/jls/ArchitectureRulesTest` must stay green unmodified:
`collabLayersAreHeadless` (`:150`), `transportKnowsNothingOfCircuits` (`:173`),
`replicationStackDependsDownwardOnly` (`:226`),
`socketEndpointsAreConfinedToCollabNet` (`:249`). If any of them needs an
exception, the layering is wrong, not the rule.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 169 | Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel (collab Stage 1b) | closes - its predictions P1-P4 are this task's acceptance tests verbatim |
| 168 | P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification (collab Stage 1a) | closes - the crypto core shipped; the join/verify and key-change dialogs are the remaining slice, named as "the following #168 slice" in `jls/collab/net/package-info.java` |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | depends on - the peer panel and the read-only affordance are GUI; without #91 their tests are weak |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | overlaps - #169 asks for panel screenshots via the sway rig |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting) | overlaps - the session frame codec is network input and inherits #170's caps; the allowlist itself is TASK-0110 |

## Notes

- **The unwired-data-structure state is the finding.** `grep -rn "Roster\|
  ReachabilityTracker" src/ --include=*.java` returns only files under
  `src/jls/collab/session/`. The lifecycle logic is done and tested
  (`RosterTest`, `RosterConvergenceTest`, `ReachabilityTrackerTest`); nothing
  drives it. Scope the two weeks at the wiring, the codec and the UI, not at the
  algorithm.
- **Cancel the gesture before applying a remote restore.** A snapshot restore
  swaps the object graph under the editor, and the mouse machine can hold
  references to dead elements. The editor already has the cancel-then-apply
  discipline for undo; remote restores must take the same path or a drag in
  progress corrupts.
- **`SecureLink` is fail-closed and poisons on rejection.** A session that
  treats a `FrameRejected` as recoverable and keeps reading has defeated the
  design. Close the link, report, and let the peer rejoin.
- **Loopback timing is kinder than a real network.** The chaos toggles must
  include delay distributions, not only drops, or the schedules pass for the
  wrong reason.
- **One writer, no merge.** Concurrent editing is TASK-0110. Shipping the token
  first is what exercises every lifecycle path the CRDT stage inherits
  unchanged, and it delivers instructor demos and pair work on its own.
- **Sockets stay confined.** `SessionListener` is the only place in JLS that
  binds a server socket and only after an explicit Share gesture; a default GUI
  start and batch mode open no port, pinned by
  `test/jls/SocketConfinementRatchetTest`.

## Evidence

- `src/jls/collab/session/Roster.java:52` (the class), `:116` (`receive`),
  `:150-235` (the five `propose*` entries), `:248-323` (membership, token holder,
  epoch and entry accessors) - complete, and called from nowhere in `src/`.
- `src/jls/collab/session/package-info.java` - "Everything here is headless and
  transport-free: the Stage 1a transport (issue #168) authenticates and carries
  entries, the wire grammar belongs to the network-surface work (issue #170)".
- `src/jls/collab/net/package-info.java` - the full #168 inventory, and "The
  join/verify and key-change dialogs are the following #168 slice, under
  `jls.collab.ui`", a package that does not exist at HEAD (`ls src/jls/collab/`
  returns `crdt net op session`).
- `src/jls/collab/net/LoopbackTransport.java:104` (`pair()`), `:232`
  (`Pair`); `test/jls/collab/net/ChaosTransport.java:23` - the seeded chaos
  decorator, package-private in the test tree.
- `src/jls/edit/CircuitSnapshot.java:60` (`capture`), `:85` (`restore` through
  the ordinary load path).
- `src/jls/edit/SimpleEditor.java:5547-5570` - the anonymous `OpSink` the token
  gate wraps.
- `test/jls/ArchitectureRulesTest.java:150,173,226,249` - the four layering
  rules this task must not need an exception to.
- Do not restate: `docs/collaborative-editing-research.md` owns the lifecycle
  design; `docs/collab-vocabulary.md` owns the payload kinds and caps;
  `docs/collab-handshake-review.md` owns the handshake analysis.
