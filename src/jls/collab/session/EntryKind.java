package jls.collab.session;

/**
 * The closed vocabulary of replicated session-state changes (issue
 * #169, collab Stage 1b): membership lifecycle (research doc section
 * 5.3) plus the single editing token (floor control). Every kind is
 * carried by a {@link SessionEntry} and applied by {@link Roster}
 * under the one-at-a-time, epoch-ordered discipline borrowed from
 * Raft's revised membership changes (research doc section 4).
 */
public enum EntryKind {

	/** A member admits a new peer into the session. */
	ADMIT,

	/** A member announces its own clean departure. */
	LEAVE,

	/**
	 * A member removes another member. Loudly attributed in the UI
	 * ("Alex removed Sam"): for a pedagogy tool the check on abuse is
	 * social visibility, not cryptographic governance (research doc
	 * section 5.3, a recorded deliberate simplification).
	 */
	EJECT,

	/** The token holder passes the editing token to another member. */
	TOKEN_GRANT,

	/**
	 * A member claims the editing token without a grant: the reclaim
	 * path after the holder crashes, leaves, or is ejected. The epoch
	 * fence in {@link Roster} makes reclaim safe - once the claim
	 * entry is folded, every replica (including the old holder, when
	 * it returns) agrees the old holder lost the token.
	 */
	TOKEN_CLAIM;

} // end of EntryKind enum
