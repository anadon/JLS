/**
 * Tests for the shared-session state layer (issue #169, collab Stage
 * 1b): directed lifecycle and floor-control cases pinning the roster's
 * fold rules ({@link jls.collab.session.RosterTest}), heartbeat
 * reachability semantics
 * ({@link jls.collab.session.ReachabilityTrackerTest}), and the seeded
 * randomized-schedule convergence harness driving replicas through a
 * chaos bus - drop, duplicate, reorder - then asserting identical
 * rosters and at most one writer after one anti-entropy round
 * ({@link jls.collab.session.RosterConvergenceTest}).
 */
package jls.collab.session;
