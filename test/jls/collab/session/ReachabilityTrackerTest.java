package jls.collab.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Heartbeat reachability semantics (issue #169, prediction P4's model
 * half): silence past k intervals marks a peer unreachable without any
 * roster effect - unreachable is not removed - and the next frame
 * resumes reachability with membership intact and no re-verification.
 */
class ReachabilityTrackerTest {

	/** The session founder in these scenarios. */
	private static final PeerId ALEX = new PeerId("aa-alex");

	/** The flaky peer in these scenarios. */
	private static final PeerId BEA = new PeerId("bb-bea");

	@Test
	void silencePastTheWindowMarksUnreachableNotRemoved() {

		Roster roster = new Roster(ALEX, "Alex");
		roster.proposeAdmit(ALEX, BEA, "Bea");
		ReachabilityTracker tracker = new ReachabilityTracker(1000, 3);

		tracker.heard(BEA, 0);
		assertTrue(tracker.isReachable(BEA, 3000));
		assertFalse(tracker.isReachable(BEA, 3001));
		assertEquals(0, tracker.lastSeen(BEA));

		// Unreachable changes nothing about membership.
		assertTrue(roster.isMember(BEA));
		assertEquals("Bea", roster.memberName(BEA));

		// The rejoin leg: one frame restores reachability, membership
		// untouched throughout.
		tracker.heard(BEA, 60000);
		assertTrue(tracker.isReachable(BEA, 60001));
		assertEquals(60000, tracker.lastSeen(BEA));
		assertTrue(roster.isMember(BEA));
	}

	@Test
	void unknownPeersAreUnreachableUntilFirstHeard() {

		ReachabilityTracker tracker = new ReachabilityTracker(1000, 3);
		assertFalse(tracker.isReachable(BEA, 0));
		assertEquals(-1, tracker.lastSeen(BEA));
		tracker.heard(BEA, 5);
		assertTrue(tracker.isReachable(BEA, 5));
	}

	@Test
	void forgetDropsStateAfterExplicitRemoval() {

		ReachabilityTracker tracker = new ReachabilityTracker(1000, 3);
		tracker.heard(BEA, 5);
		tracker.forget(BEA);
		assertFalse(tracker.isReachable(BEA, 6));
		assertEquals(-1, tracker.lastSeen(BEA));
	}

	@Test
	void boundsAndArgumentsAreValidated() {

		assertThrows(IllegalArgumentException.class,
				() -> new ReachabilityTracker(0, 3));
		assertThrows(IllegalArgumentException.class,
				() -> new ReachabilityTracker(1000, 0));
		ReachabilityTracker tracker = new ReachabilityTracker(1000, 3);
		assertThrows(IllegalArgumentException.class,
				() -> tracker.heard(null, 0));
	}

} // end of ReachabilityTrackerTest class
