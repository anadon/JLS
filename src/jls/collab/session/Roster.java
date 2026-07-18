package jls.collab.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * One replica of a shared session's roster and floor-control state
 * (issue #169, collab Stage 1b): the current members with their
 * display names, and the single editing token.
 *
 * Correctness model (research doc section 4 - Raft's revised
 * membership discipline, applied to the peer roster rather than a
 * document): every change is an explicit, epoch-numbered
 * {@link SessionEntry}, applied one at a time in epoch order.
 * Concurrent proposals for the same epoch resolve deterministically -
 * lowest proposer id wins, the loser observes {@link #applied} is
 * false and re-issues at a later epoch. The folded state is a pure
 * function of the received entry <em>set</em> plus the shared genesis:
 * any two replicas holding the same entries agree exactly, whatever
 * the arrival order (a late entry for an old epoch triggers a refold
 * from genesis), and receipt is idempotent (the log is a set), so
 * duplicated or re-ordered delivery is harmless. Anti-entropy is
 * exchanging {@link #entries()}.
 *
 * Membership is not reachability (research doc section 5.3): a member
 * that stops answering heartbeats is tracked as unreachable by
 * {@link ReachabilityTracker} - local knowledge, never replicated -
 * and stays a member, its key trusted and its edits valid, until an
 * explicit leave or eject entry removes it.
 *
 * Floor control: the token holder is derived from the same folded log,
 * so replicas that agree on entries agree on the single writer. When
 * the holder leaves or is ejected the token becomes unheld (nobody may
 * write) until a member issues a claim; a crashed holder is handled by
 * timeout-then-claim, and the epoch fence guarantees the returning old
 * holder folds the claim entry and stops writing.
 *
 * Headless and transport-free by architecture rule (issue #163): no
 * Swing, no sockets. Not internally synchronized - the owner confines
 * calls to one thread (in the application, the session service thread;
 * UI reads are marshalled snapshots).
 */
public final class Roster {

	/** The founding member, part of the shared session genesis. */
	private final PeerId creator;

	/** The founding member's display name, also part of the genesis. */
	private final String creatorName;

	/** Every entry ever received, in fold order; receipt dedups here. */
	private final TreeSet<SessionEntry> log = new TreeSet<SessionEntry>();

	/** Folded state: current members and their display names. */
	private final TreeMap<PeerId, String> members =
			new TreeMap<PeerId, String>();

	/** Folded state: the current token holder; null while unheld. */
	private PeerId tokenHolder;

	/** Folded state: the entries that won their epoch and applied. */
	private final Set<SessionEntry> appliedEntries =
			new HashSet<SessionEntry>();

	/**
	 * Start a replica of a session's roster from its genesis: the
	 * founding member, alone and holding the editing token, at epoch 0.
	 * Every replica of one session must be constructed from the same
	 * genesis - it travels with the join handshake (issue #168)
	 * alongside the entry log.
	 *
	 * @param creator The founding member's id.
	 * @param creatorName The founding member's display name; non-empty.
	 *
	 * @throws IllegalArgumentException if the creator is null or the
	 *             name is null or empty.
	 */
	public Roster(PeerId creator, String creatorName) {

		if (creator == null) {
			throw new IllegalArgumentException(
					"session creator must be non-null");
		}
		if (creatorName == null || creatorName.isEmpty()) {
			throw new IllegalArgumentException(
					"creator display name must be non-empty");
		}
		this.creator = creator;
		this.creatorName = creatorName;
		refold();
	} // end of constructor

	/**
	 * Receive one entry, locally proposed or from the network (after
	 * the transport has verified its signature - issue #168). Receipt
	 * is idempotent and order-independent: duplicates change nothing,
	 * and an entry for an old epoch refolds the state from genesis.
	 *
	 * @param entry The entry to fold in.
	 *
	 * @return true if the folded membership or token state changed;
	 *         false for duplicates and for entries that lost their
	 *         epoch or failed their precondition without effect.
	 *
	 * @throws IllegalArgumentException if the entry is null.
	 */
	public boolean receive(SessionEntry entry) {

		if (entry == null) {
			throw new IllegalArgumentException("entry must be non-null");
		}
		if (!log.add(entry)) {
			return false;
		}
		TreeMap<PeerId, String> beforeMembers =
				new TreeMap<PeerId, String>(members);
		PeerId beforeHolder = tokenHolder;
		refold();
		return !members.equals(beforeMembers)
				|| !Objects.equals(tokenHolder, beforeHolder);
	} // end of receive method

	/**
	 * Propose admitting a new peer, validated against this replica's
	 * current view and folded in locally. The caller broadcasts the
	 * returned entry to every other member; if a concurrent proposal
	 * wins the epoch instead ({@link #applied} on the returned entry
	 * turns false), re-issue.
	 *
	 * @param proposer The admitting member (this replica's owner).
	 * @param subject The peer to admit.
	 * @param subjectName The new peer's display name.
	 *
	 * @return the entry to broadcast.
	 *
	 * @throws IllegalStateException if the proposer is not a member or
	 *             the subject already is one, in this replica's view.
	 * @throws IllegalArgumentException if the entry components are
	 *             structurally invalid ({@link SessionEntry}).
	 */
	public SessionEntry proposeAdmit(PeerId proposer, PeerId subject,
			String subjectName) {

		return propose(new SessionEntry(nextEpoch(), proposer,
				EntryKind.ADMIT, subject, subjectName));
	} // end of proposeAdmit method

	/**
	 * Propose this member's own clean departure, validated against this
	 * replica's current view and folded in locally. If the leaver holds
	 * the token, the token becomes unheld once the entry applies.
	 *
	 * @param proposer The leaving member (this replica's owner).
	 *
	 * @return the entry to broadcast.
	 *
	 * @throws IllegalStateException if the proposer is not a member in
	 *             this replica's view.
	 */
	public SessionEntry proposeLeave(PeerId proposer) {

		return propose(new SessionEntry(nextEpoch(), proposer,
				EntryKind.LEAVE, proposer, ""));
	} // end of proposeLeave method

	/**
	 * Propose ejecting another member, validated against this replica's
	 * current view and folded in locally. Loudly attributed in the UI;
	 * if the ejected member holds the token, the token becomes unheld
	 * once the entry applies.
	 *
	 * @param proposer The ejecting member (this replica's owner).
	 * @param subject The member to eject.
	 *
	 * @return the entry to broadcast.
	 *
	 * @throws IllegalStateException if either peer is not a member in
	 *             this replica's view.
	 * @throws IllegalArgumentException if the proposer and subject are
	 *             the same peer (leave instead).
	 */
	public SessionEntry proposeEject(PeerId proposer, PeerId subject) {

		return propose(new SessionEntry(nextEpoch(), proposer,
				EntryKind.EJECT, subject, ""));
	} // end of proposeEject method

	/**
	 * Propose passing the editing token to another member, validated
	 * against this replica's current view and folded in locally.
	 *
	 * @param proposer The current token holder (this replica's owner).
	 * @param subject The member to hand the token to.
	 *
	 * @return the entry to broadcast.
	 *
	 * @throws IllegalStateException if the proposer does not hold the
	 *             token or the subject is not a member, in this
	 *             replica's view.
	 * @throws IllegalArgumentException if the proposer and subject are
	 *             the same peer.
	 */
	public SessionEntry proposeTokenGrant(PeerId proposer,
			PeerId subject) {

		return propose(new SessionEntry(nextEpoch(), proposer,
				EntryKind.TOKEN_GRANT, subject, ""));
	} // end of proposeTokenGrant method

	/**
	 * Propose claiming the editing token without a grant - the reclaim
	 * path after the holder crashed (heartbeat timeout, judged by the
	 * caller via {@link ReachabilityTracker}), left, or was ejected.
	 * Validated against this replica's current view and folded in
	 * locally. Safe under races: whichever claim wins its epoch is the
	 * one every replica folds, and a superseded holder stops writing
	 * when it folds the claim.
	 *
	 * @param proposer The claiming member (this replica's owner).
	 *
	 * @return the entry to broadcast.
	 *
	 * @throws IllegalStateException if the proposer is not a member in
	 *             this replica's view.
	 */
	public SessionEntry proposeTokenClaim(PeerId proposer) {

		return propose(new SessionEntry(nextEpoch(), proposer,
				EntryKind.TOKEN_CLAIM, proposer, ""));
	} // end of proposeTokenClaim method

	/**
	 * Whether a peer is currently a member in this replica's view.
	 *
	 * @param peer The peer to look up.
	 *
	 * @return true if the peer is a member.
	 */
	public boolean isMember(PeerId peer) {

		return members.containsKey(peer);
	} // end of isMember method

	/**
	 * The current members and their display names, sorted by id - the
	 * peer panel's row set.
	 *
	 * @return an unmodifiable snapshot copy.
	 */
	public SortedMap<PeerId, String> members() {

		return Collections.unmodifiableSortedMap(
				new TreeMap<PeerId, String>(members));
	} // end of members method

	/**
	 * The display name of a current member.
	 *
	 * @param peer The peer to look up.
	 *
	 * @return the name, or null if the peer is not a member.
	 */
	public String memberName(PeerId peer) {

		return members.get(peer);
	} // end of memberName method

	/**
	 * The peer currently holding the editing token - the session's one
	 * writer - in this replica's view.
	 *
	 * @return the holder, or null while the token is unheld (after the
	 *         holder left or was ejected, before anyone claims it).
	 */
	public PeerId tokenHolder() {

		return tokenHolder;
	} // end of tokenHolder method

	/**
	 * The highest epoch present in the log, whether or not that entry
	 * applied. Proposals are issued at the next epoch after this.
	 *
	 * @return the highest received epoch, or 0 for a fresh genesis.
	 */
	public long highestEpoch() {

		return log.isEmpty() ? 0 : log.last().epoch();
	} // end of highestEpoch method

	/**
	 * Every entry this replica has received, in fold order - the
	 * payload of anti-entropy: feeding one replica's entries to
	 * {@link #receive} on another makes their folded states identical.
	 *
	 * @return a snapshot copy of the log.
	 */
	public List<SessionEntry> entries() {

		return new ArrayList<SessionEntry>(log);
	} // end of entries method

	/**
	 * Whether an entry won its epoch and passed its precondition, so
	 * its change is part of the folded state. A proposer checks its own
	 * broadcast entries here after syncing: false means a concurrent
	 * proposal won the epoch (or the precondition no longer held) and
	 * the intent, if still wanted, must be re-issued at a fresh epoch.
	 *
	 * @param entry The entry to check.
	 *
	 * @return true if the entry is applied in the current fold.
	 */
	public boolean applied(SessionEntry entry) {

		return appliedEntries.contains(entry);
	} // end of applied method

	/**
	 * The epoch the next local proposal uses.
	 *
	 * @return one past the highest received epoch.
	 */
	private long nextEpoch() {

		return highestEpoch() + 1;
	} // end of nextEpoch method

	/**
	 * Validate a proposal against this replica's current view, fold it
	 * in, and hand it back for broadcast.
	 *
	 * @param entry The freshly built entry, at {@link #nextEpoch()}.
	 *
	 * @return the same entry.
	 *
	 * @throws IllegalStateException if the entry's precondition fails
	 *             against the current folded state.
	 */
	private SessionEntry propose(SessionEntry entry) {

		if (!precondition(entry)) {
			throw new IllegalStateException("proposal invalid against"
					+ " this replica's state: " + entry);
		}
		receive(entry);
		return entry;
	} // end of propose method

	/**
	 * Recompute the folded state from genesis over the whole log:
	 * epochs ascending, the lowest-proposer entry of each epoch is the
	 * winner, and a winner applies only if its precondition holds
	 * against the state folded so far. Pure function of the log set, so
	 * replicas with equal logs are identical.
	 */
	private void refold() {

		members.clear();
		appliedEntries.clear();
		members.put(creator, creatorName);
		tokenHolder = creator;
		long foldedEpoch = 0;
		for (SessionEntry entry : log) {
			if (entry.epoch() == foldedEpoch) {
				continue; // same-epoch loser: a lower proposer won
			}
			foldedEpoch = entry.epoch();
			if (precondition(entry)) {
				apply(entry);
				appliedEntries.add(entry);
			}
		}
	} // end of refold method

	/**
	 * Whether an entry's semantic precondition holds against the state
	 * folded so far. Deterministic: depends only on folded replicated
	 * state, never on local knowledge such as reachability.
	 *
	 * @param entry The epoch winner to check.
	 *
	 * @return true if the entry may apply.
	 */
	private boolean precondition(SessionEntry entry) {

		if (!members.containsKey(entry.proposer())) {
			return false;
		}
		return switch (entry.kind()) {
		case ADMIT -> !members.containsKey(entry.subject());
		case LEAVE -> true;
		case EJECT -> members.containsKey(entry.subject());
		case TOKEN_GRANT -> entry.proposer().equals(tokenHolder)
				&& members.containsKey(entry.subject());
		case TOKEN_CLAIM -> true;
		};
	} // end of precondition method

	/**
	 * Apply one epoch winner's change to the folded state.
	 *
	 * @param entry The winning, precondition-passing entry.
	 */
	private void apply(SessionEntry entry) {

		switch (entry.kind()) {
		case ADMIT -> members.put(entry.subject(), entry.subjectName());
		case LEAVE, EJECT -> {
			members.remove(entry.subject());
			if (entry.subject().equals(tokenHolder)) {
				tokenHolder = null;
			}
		}
		case TOKEN_GRANT, TOKEN_CLAIM -> tokenHolder = entry.subject();
		}
	} // end of apply method

} // end of Roster class
