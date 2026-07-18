package jls.collab.session;

/**
 * The identity of a session peer (issue #169, collab Stage 1b): an
 * opaque, totally ordered fingerprint string. When the Stage 1a
 * transport lands (issue #168) this becomes the public-key fingerprint
 * of the peer's long-term signing key (research doc section 5.1);
 * until then it is any non-empty stable string. Display names are
 * labels carried by admission entries ({@link SessionEntry}), never
 * identity - two peers named "Alex" never collide, because ids are
 * keys and names are labels.
 *
 * The total order ({@link #compareTo}) is load-bearing: it is the
 * deterministic tie-break for concurrent same-epoch roster proposals
 * ("lowest proposer id wins" - see {@link Roster}).
 *
 * @param fingerprint The opaque identity string; non-empty.
 */
public record PeerId(String fingerprint) implements Comparable<PeerId> {

	/**
	 * Validate the fingerprint.
	 *
	 * @param fingerprint The opaque identity string; non-empty.
	 *
	 * @throws IllegalArgumentException if the fingerprint is null or
	 *             empty.
	 */
	public PeerId {

		if (fingerprint == null || fingerprint.isEmpty()) {
			throw new IllegalArgumentException(
					"peer id fingerprint must be non-empty");
		}
	} // end of canonical constructor

	@Override
	public int compareTo(PeerId other) {

		return fingerprint.compareTo(other.fingerprint);
	} // end of compareTo method

	@Override
	public String toString() {

		return fingerprint;
	} // end of toString method

} // end of PeerId record
