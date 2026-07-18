package jls.collab.crdt;

/**
 * The permanent identity of one replicated operation (issue #171,
 * collab Stage 2): the peer that originated it plus that peer's
 * per-origin sequence number, starting at 1. Op ids are what dedup
 * keys on — a flapping link can present the same envelope many times,
 * but an id is applied at most once ({@link CausalBuffer}) — and the
 * total order below gives logs and test transcripts a deterministic
 * shape.
 *
 * @param origin The originating peer id (1-64 characters of
 *            {@code [0-9a-z]}, see {@link VectorClock}).
 * @param seq The origin's sequence number for this op; the first op a
 *            peer originates is 1.
 */
public record OpId(String origin, long seq) implements Comparable<OpId> {

	/**
	 * Validate the components.
	 *
	 * @throws IllegalArgumentException if the origin is malformed or the
	 *             sequence number is not strictly positive.
	 */
	public OpId {

		VectorClock.checkPeer(origin);
		if (seq < 1) {
			throw new IllegalArgumentException("the sequence number "
					+ seq + " is not strictly positive");
		}
	} // end of constructor

	@Override
	public int compareTo(OpId other) {

		int byOrigin = origin.compareTo(other.origin);
		if (byOrigin != 0) {
			return byOrigin;
		}
		return Long.compare(seq, other.seq);
	} // end of compareTo method

	@Override
	public String toString() {

		return origin + "#" + seq;
	} // end of toString method

} // end of OpId record
