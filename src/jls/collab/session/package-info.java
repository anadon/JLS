/**
 * The shared-session state layer of the collaborative-editing program
 * (issue #169, collab Stage 1b): the replicated peer roster with
 * explicit admission, leave, and ejection; the single editing token
 * (floor control); and heartbeat reachability tracking. This is the
 * lifecycle machinery Stage 2 (issue #163) inherits unchanged when the
 * token-plus-snapshot data path is replaced by CRDT operations.
 *
 * {@link jls.collab.session.Roster} deterministically folds an
 * idempotent set of epoch-ordered {@link
 * jls.collab.session.SessionEntry} values - Raft's revised
 * one-at-a-time membership discipline applied to the peer roster,
 * research doc section 4 - into membership and token state: same
 * entries, same state, whatever the delivery order, duplication, or
 * loss healed later by anti-entropy. Concurrent same-epoch proposals
 * resolve by lowest proposer id; the loser re-issues.
 * {@link jls.collab.session.ReachabilityTracker} keeps the separate,
 * never-replicated reachable/unreachable view - unreachable is not
 * removed (research doc sections 5.3-5.4).
 *
 * Everything here is headless and transport-free: the Stage 1a
 * transport (issue #168) authenticates and carries entries, the wire
 * grammar belongs to the network-surface work (issue #170), and the
 * architecture rules forbid this package from touching Swing.
 */
package jls.collab.session;
