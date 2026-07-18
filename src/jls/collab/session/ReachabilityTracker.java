package jls.collab.session;

import java.util.HashMap;
import java.util.Map;

/**
 * Heartbeat failure detection for a shared session (issue #169,
 * research doc section 5.4): a peer missing k consecutive heartbeat
 * intervals is unreachable. Unreachable is <em>not</em> removed - this
 * is local knowledge for the peer panel and for reclaim policy, never
 * replicated state, and it never touches the {@link Roster}; a member
 * stays a member, key trusted, until an explicit leave or eject entry.
 * A returning peer resumes on its next frame with no re-verification.
 *
 * At the design target of 2-8 peers a fixed timeout is adequate
 * (phi-accrual detection is overkill - research doc section 5.4).
 * Callers pass the clock into every query, which keeps the tracker
 * deterministic under test. A peer never heard from is unreachable
 * until its first frame. Not internally synchronized: the owner
 * confines calls to one thread, like {@link Roster}.
 */
public final class ReachabilityTracker {

	/** The expected milliseconds between peer heartbeats. */
	private final long intervalMillis;

	/** How many consecutive intervals may pass before unreachable. */
	private final int missedIntervalsAllowed;

	/** When each peer was last heard, in caller-clock milliseconds. */
	private final Map<PeerId, Long> lastSeen =
			new HashMap<PeerId, Long>();

	/**
	 * Create a tracker.
	 *
	 * @param intervalMillis The expected milliseconds between
	 *            heartbeats; positive.
	 * @param missedIntervalsAllowed How many consecutive intervals may
	 *            pass silent before a peer counts as unreachable;
	 *            positive.
	 *
	 * @throws IllegalArgumentException if either bound is not positive.
	 */
	public ReachabilityTracker(long intervalMillis,
			int missedIntervalsAllowed) {

		if (intervalMillis <= 0 || missedIntervalsAllowed <= 0) {
			throw new IllegalArgumentException(
					"heartbeat interval and allowed misses must be"
							+ " positive");
		}
		this.intervalMillis = intervalMillis;
		this.missedIntervalsAllowed = missedIntervalsAllowed;
	} // end of constructor

	/**
	 * Record that a frame - heartbeat or any other traffic - arrived
	 * from a peer.
	 *
	 * @param peer The peer heard from.
	 * @param nowMillis The current time on the caller's clock.
	 *
	 * @throws IllegalArgumentException if the peer is null.
	 */
	public void heard(PeerId peer, long nowMillis) {

		if (peer == null) {
			throw new IllegalArgumentException("peer must be non-null");
		}
		lastSeen.put(peer, nowMillis);
	} // end of heard method

	/**
	 * Whether a peer is currently reachable: heard within the allowed
	 * window (interval times allowed misses).
	 *
	 * @param peer The peer to check.
	 * @param nowMillis The current time on the caller's clock.
	 *
	 * @return true if heard recently enough; false if silent past the
	 *         window or never heard at all.
	 */
	public boolean isReachable(PeerId peer, long nowMillis) {

		Long seen = lastSeen.get(peer);
		return seen != null && nowMillis - seen
				<= intervalMillis * missedIntervalsAllowed;
	} // end of isReachable method

	/**
	 * When a peer was last heard - the peer panel's last-seen column.
	 *
	 * @param peer The peer to look up.
	 *
	 * @return the caller-clock time of the last frame, or -1 if the
	 *         peer has never been heard.
	 */
	public long lastSeen(PeerId peer) {

		Long seen = lastSeen.get(peer);
		return seen == null ? -1 : seen;
	} // end of lastSeen method

	/**
	 * Drop a peer's state, after an explicit leave or eject removed it
	 * from the roster.
	 *
	 * @param peer The removed peer.
	 */
	public void forget(PeerId peer) {

		lastSeen.remove(peer);
	} // end of forget method

} // end of ReachabilityTracker class
