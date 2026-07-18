package jls.collab.crdt;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * An immutable vector clock over peer ids (issue #171, collab Stage 2):
 * one monotone counter per peer, recording how many of that peer's
 * operations a replica has observed. Vector clocks order the causal
 * history of a collaborative session — {@link #compare} answers whether
 * one observation state happened before another or whether the two are
 * concurrent — and drive causal delivery ({@link CausalBuffer}) and,
 * later, anti-entropy resync (research doc
 * {@code docs/collaborative-editing-research.md} &sect;5.4).
 *
 * Representation invariant: only strictly positive counters are stored,
 * so two clocks that have observed the same history are {@code equals}
 * regardless of how they got there. Peer ids are 1-64 characters of
 * {@code [0-9a-z]} — the same alphabet as {@link jls.elem.ElementId}
 * replica ids (issue #165), so they survive the save format untouched
 * and need no escaping on the wire.
 */
public final class VectorClock {

	/** The peer-id alphabet, shared with stable-id replicas (#165). */
	private static final Pattern PEER = Pattern.compile("[0-9a-z]{1,64}");

	/** The clock that has observed nothing. */
	public static final VectorClock EMPTY =
			new VectorClock(new TreeMap<String, Long>());

	/**
	 * How two clocks relate in the causal order.
	 */
	public enum Order {

		/** Both clocks observed exactly the same history. */
		EQUAL,

		/** This clock's history is a strict prefix of the other's. */
		BEFORE,

		/** The other clock's history is a strict prefix of this one's. */
		AFTER,

		/** Each clock observed something the other has not. */
		CONCURRENT;

	} // end of Order enum

	/** Per-peer counters; every stored value is strictly positive. */
	private final SortedMap<String, Long> counters;

	/**
	 * Bind an already-validated counter map. Private: callers build
	 * clocks through {@link #EMPTY}, {@link #of}, {@link #increment} and
	 * {@link #merge}.
	 *
	 * @param counters The counters; ownership transfers to this clock.
	 */
	private VectorClock(TreeMap<String, Long> counters) {

		this.counters = Collections.unmodifiableSortedMap(counters);
	} // end of constructor

	/**
	 * Build a clock from explicit per-peer counters.
	 *
	 * @param counters Peer id to counter; every counter must be
	 *            strictly positive and every peer id well-formed.
	 *
	 * @return the clock.
	 *
	 * @throws IllegalArgumentException if a peer id is malformed or a
	 *             counter is not strictly positive.
	 */
	public static VectorClock of(Map<String, Long> counters) {

		TreeMap<String, Long> copy = new TreeMap<String, Long>();
		for (Map.Entry<String, Long> entry : counters.entrySet()) {
			checkPeer(entry.getKey());
			long count = entry.getValue();
			if (count < 1) {
				throw new IllegalArgumentException("the counter for peer '"
						+ entry.getKey() + "' is " + count
						+ "; counters are strictly positive");
			}
			copy.put(entry.getKey(), count);
		}
		return copy.isEmpty() ? EMPTY : new VectorClock(copy);
	} // end of of method

	/**
	 * Reject a malformed peer id.
	 *
	 * @param peer The candidate peer id.
	 *
	 * @throws IllegalArgumentException if the id is not 1-64 characters
	 *             of {@code [0-9a-z]}.
	 */
	static void checkPeer(String peer) {

		if (peer == null || !PEER.matcher(peer).matches()) {
			throw new IllegalArgumentException("the peer id '" + peer
					+ "' is not 1-64 characters of [0-9a-z]");
		}
	} // end of checkPeer method

	/**
	 * How many of the given peer's operations this clock has observed.
	 *
	 * @param peer The peer id.
	 *
	 * @return the counter, 0 if the peer has never been observed.
	 */
	public long counter(String peer) {

		Long count = counters.get(peer);
		return count == null ? 0 : count;
	} // end of counter method

	/**
	 * The clock after observing one more operation from a peer.
	 *
	 * @param peer The peer id.
	 *
	 * @return a new clock with that peer's counter one higher.
	 *
	 * @throws IllegalArgumentException if the peer id is malformed.
	 * @throws IllegalStateException if the counter would overflow.
	 */
	public VectorClock increment(String peer) {

		checkPeer(peer);
		long current = counter(peer);
		if (current == Long.MAX_VALUE) {
			throw new IllegalStateException("the counter for peer '"
					+ peer + "' would overflow");
		}
		TreeMap<String, Long> copy =
				new TreeMap<String, Long>(counters);
		copy.put(peer, current + 1);
		return new VectorClock(copy);
	} // end of increment method

	/**
	 * The entrywise maximum of two clocks: the observation state of a
	 * replica that has seen everything either clock has seen. Merge is
	 * commutative, associative and idempotent — the least upper bound
	 * in the causal order.
	 *
	 * @param other The other clock.
	 *
	 * @return the merged clock.
	 */
	public VectorClock merge(VectorClock other) {

		TreeMap<String, Long> copy =
				new TreeMap<String, Long>(counters);
		for (Map.Entry<String, Long> entry : other.counters.entrySet()) {
			Long mine = copy.get(entry.getKey());
			if (mine == null || mine < entry.getValue()) {
				copy.put(entry.getKey(), entry.getValue());
			}
		}
		return new VectorClock(copy);
	} // end of merge method

	/**
	 * Where this clock stands relative to another in the causal order.
	 *
	 * @param other The other clock.
	 *
	 * @return {@link Order#EQUAL}, {@link Order#BEFORE},
	 *         {@link Order#AFTER}, or {@link Order#CONCURRENT}.
	 */
	public Order compare(VectorClock other) {

		boolean anyLess = false;
		boolean anyGreater = false;
		TreeMap<String, Long> union =
				new TreeMap<String, Long>(counters);
		union.putAll(other.counters);
		for (String peer : union.keySet()) {
			long mine = counter(peer);
			long theirs = other.counter(peer);
			if (mine < theirs) {
				anyLess = true;
			} else if (mine > theirs) {
				anyGreater = true;
			}
		}
		if (anyLess && anyGreater) {
			return Order.CONCURRENT;
		}
		if (anyLess) {
			return Order.BEFORE;
		}
		if (anyGreater) {
			return Order.AFTER;
		}
		return Order.EQUAL;
	} // end of compare method

	/**
	 * The per-peer counters, sorted by peer id. Every value is strictly
	 * positive; peers never observed are absent.
	 *
	 * @return an unmodifiable sorted view of the counters.
	 */
	public SortedMap<String, Long> entries() {

		return counters;
	} // end of entries method

	@Override
	public boolean equals(Object other) {

		return other instanceof VectorClock clock
				&& counters.equals(clock.counters);
	} // end of equals method

	@Override
	public int hashCode() {

		return counters.hashCode();
	} // end of hashCode method

	@Override
	public String toString() {

		return counters.toString();
	} // end of toString method

} // end of VectorClock class
