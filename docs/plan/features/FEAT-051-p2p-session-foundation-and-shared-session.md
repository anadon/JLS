# FEAT-051 - P2P session foundation and shared session v1

**Status:** proposed | **Cost:** 12-18 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

Two installs of JLS establish a session with each other directly: each has a
per-install identity key, the link between them is encrypted and verified out
of band by a short authentication string, and on top of that link sits a
session with a membership lifecycle, presence, floor control and an initial
snapshot synchronization. It is the transport and the social contract that
concurrent editing rides on; it carries no replication of its own.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | Without verified identity, an encrypted transport and membership there is no session for ops to travel over |
| CAP-06 | beneficial | Paired lab work and live instructor assistance inside a student's own file |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-015 | A session synchronizes a circuit by exchanging ops. Until every editor mutation is an op that applies without a drawing context, there is nothing headless to send and nothing to apply on receipt |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0108 | Session foundation: identity, transport, membership and sync | Identity keys, the encrypted transport, membership lifecycle, floor control, presence and snapshot synchronization |
| TASK-0109 | The replica loop over a loopback transport | Two headless replicas over an in-tree loopback and a chaos transport, saving byte-identical files - the first end-to-end demonstration that the session carries a circuit |

## Acceptance criteria

1. Each install has a persistent identity key, and a peer's key change is
   reported rather than accepted silently.
2. The link is encrypted and its short authentication string is verifiable out
   of band; a mismatched string aborts the session by name.
3. Membership has a stated lifecycle - join, present, leave, drop - and every
   transition is observable to the other peer.
4. A joining peer receives a snapshot and reaches the same circuit state as the
   host, asserted by byte-identical saves rather than by visual agreement.
5. Two headless replicas over the in-tree loopback transport converge and save
   byte-identical files; under the chaos transport they either converge or
   report failure, and never silently diverge.
6. No network path can be reached from any package outside the networking
   package, asserted by an architecture rule.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 168 | P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification (collab Stage 1a) | closes |
| 169 | Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel (collab Stage 1b) | closes |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | tracking - this feature and FEAT-052 are its two halves; neither closes it alone |

## Design notes

Most of the algorithmic work already exists in the tree and the tasks must be
scoped to the wiring rather than to the mechanism. The networking package holds
identity keys, the handshake, the short-authentication-string verification, the
secure link, the known-peers store, the socket session, the transport
abstraction and a loopback transport; the session package holds the roster,
reachability tracking, session entries and peer ids. What is measurably missing
is a consumer: no file outside those two packages reads any of it, and there is
no user-facing session surface at all.

The chaos transport that criterion 5 needs exists in the test tree but is
package-private there. Whether the harness moves into that package or the class
widens within the test tree is a decision a reviewer should make before the
task starts rather than inside it.

## Risks

- **A shipped-but-unconsumed subsystem drifts from what a consumer needs.** The
  first real consumer will find contract mismatches the unit tests did not.
- **Out-of-band verification is a human step.** If the user interface makes it
  skippable, the encryption is decorative.
- **Direct peer connectivity is a network-topology problem** that no amount of
  in-tree correctness solves; the plan should be explicit that some peers will
  not be able to connect.

## Evidence

- The shipped foundation at HEAD: `src/jls/collab/net/` (`IdentityKey`,
  `Handshake`, `Sas`, `SecureLink`, `KnownPeers`, `SessionListener`,
  `SocketSession`, `Transport`, `LoopbackTransport`, `Crypto`) and
  `src/jls/collab/session/` (`Roster`, `ReachabilityTracker`, `SessionEntry`,
  `PeerId`, `EntryKind`).
- The measured gap: no file outside those packages references the roster or the
  reachability tracker, and there is no `jls.collab.ui` package.
- The op layer this feature carries: `src/jls/collab/op/`,
  `docs/operation-layer.md`.
- The handshake review that records the cryptographic choices:
  `docs/collab-handshake-review.md`.
- Issues #168, #169 and #163, all open, verified against
  `list_issues(state=OPEN)`.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 12-18 mw; TASK-0108 and TASK-0109 total 4 wk,
  and TASK-0109 is shared with FEAT-052. The residual is the session user
  interface - the peer panel, presence and floor control as things a person
  operates - which no task id names. Do not read 4 wk as the feature.
