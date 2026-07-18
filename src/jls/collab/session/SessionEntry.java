package jls.collab.session;

/**
 * One replicated session-state change (issue #169, collab Stage 1b):
 * which peer proposes what, about whom, at which epoch. Entries are
 * immutable values; {@link Roster} folds the set of received entries
 * into membership and token state deterministically, so two replicas
 * holding the same entries agree exactly.
 *
 * Structural invariants (who a kind may be about) are enforced here at
 * construction, so a malformed entry can never exist. Semantic
 * preconditions - is the proposer currently a member, does it hold the
 * token - depend on folded state and are checked by {@link Roster}
 * during the fold instead; an entry whose precondition fails simply
 * does not apply.
 *
 * Authentication is the Stage 1a transport's job (issue #168): entries
 * arrive at a {@link Roster} only after signature verification against
 * the proposer's session key, and the wire grammar belongs to the
 * network-surface work (issue #170). This record is the payload to be
 * signed, not the signature envelope.
 *
 * The natural order sorts by epoch, then proposer - the "lowest
 * proposer id wins" rule for concurrent same-epoch proposals - then by
 * the remaining components, a total tie-break that keeps the fold
 * deterministic even for entry pairs an honest proposer would never
 * issue.
 *
 * @param epoch The position in the session's entry order; at least 1
 *            (epoch 0 is the genesis state built into {@link Roster}).
 * @param proposer The peer issuing the entry.
 * @param kind What kind of change this is.
 * @param subject The peer the change is about: the admitted, leaving,
 *            ejected, granted-to, or claiming peer.
 * @param subjectName The display name attached to an admission;
 *            empty for every other kind.
 */
public record SessionEntry(long epoch, PeerId proposer, EntryKind kind,
		PeerId subject, String subjectName)
		implements Comparable<SessionEntry> {

	/**
	 * Validate the structural invariants of the kind.
	 *
	 * @param epoch The position in the session's entry order.
	 * @param proposer The peer issuing the entry.
	 * @param kind What kind of change this is.
	 * @param subject The peer the change is about.
	 * @param subjectName The display name for an admission, else empty.
	 *
	 * @throws IllegalArgumentException if the epoch is below 1, any
	 *             component is null, or the subject or name breaks the
	 *             kind's shape (a peer admitting itself, a leave or
	 *             claim about someone else, an eject or grant aimed at
	 *             the proposer, a name on a non-admission).
	 */
	public SessionEntry {

		if (epoch < 1) {
			throw new IllegalArgumentException(
					"entry epoch must be at least 1, got " + epoch);
		}
		if (proposer == null || kind == null || subject == null
				|| subjectName == null) {
			throw new IllegalArgumentException(
					"entry components must be non-null");
		}
		switch (kind) {
		case ADMIT -> {
			if (subject.equals(proposer)) {
				throw new IllegalArgumentException(
						"a peer cannot admit itself");
			}
			if (subjectName.isEmpty()) {
				throw new IllegalArgumentException(
						"an admission carries the display name");
			}
		}
		case LEAVE, TOKEN_CLAIM -> {
			if (!subject.equals(proposer)) {
				throw new IllegalArgumentException(kind
						+ " subject must be the proposer itself");
			}
			if (!subjectName.isEmpty()) {
				throw new IllegalArgumentException(
						"only admissions carry a display name");
			}
		}
		case EJECT, TOKEN_GRANT -> {
			if (subject.equals(proposer)) {
				throw new IllegalArgumentException(kind
						+ " subject must be another peer");
			}
			if (!subjectName.isEmpty()) {
				throw new IllegalArgumentException(
						"only admissions carry a display name");
			}
		}
		}
	} // end of canonical constructor

	@Override
	public int compareTo(SessionEntry other) {

		int order = Long.compare(epoch, other.epoch);
		if (order != 0) {
			return order;
		}
		order = proposer.compareTo(other.proposer);
		if (order != 0) {
			return order;
		}
		order = kind.compareTo(other.kind);
		if (order != 0) {
			return order;
		}
		order = subject.compareTo(other.subject);
		if (order != 0) {
			return order;
		}
		return subjectName.compareTo(other.subjectName);
	} // end of compareTo method

} // end of SessionEntry record
