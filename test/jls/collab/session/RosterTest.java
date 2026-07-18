package jls.collab.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Directed cases pinning the roster's fold rules (issue #169):
 * membership lifecycle (admit, leave, eject), the same-epoch conflict
 * rule (lowest proposer id wins, loser re-issues - hypothesis H1),
 * floor control with the epoch fence (grant, claim, reclaim after the
 * holder leaves or crashes - hypothesis H2 and prediction P2's model
 * half), order-independence of receipt, and the structural validation
 * of entries. The randomized counterpart is
 * {@link RosterConvergenceTest}.
 */
class RosterTest {

	/** The session founder; lowest id, wins same-epoch conflicts. */
	private static final PeerId ALEX = new PeerId("aa-alex");

	/** A second peer. */
	private static final PeerId BEA = new PeerId("bb-bea");

	/** A third peer. */
	private static final PeerId CARL = new PeerId("cc-carl");

	/** A fourth peer. */
	private static final PeerId DANA = new PeerId("dd-dana");

	/**
	 * Build a fresh replica of Alex's session.
	 *
	 * @return the replica.
	 */
	private static Roster session() {

		return new Roster(ALEX, "Alex");
	} // end of session method

	/**
	 * Assert two replicas fold to identical state.
	 *
	 * @param one The first replica.
	 * @param two The second replica.
	 */
	private static void assertConverged(Roster one, Roster two) {

		assertEquals(one.members(), two.members());
		assertEquals(one.tokenHolder(), two.tokenHolder());
		assertEquals(one.highestEpoch(), two.highestEpoch());
		assertEquals(one.entries(), two.entries());
	} // end of assertConverged method

	@Test
	void genesisSeatsTheCreatorWithTheToken() {

		Roster roster = session();
		assertTrue(roster.isMember(ALEX));
		assertEquals("Alex", roster.memberName(ALEX));
		assertEquals(ALEX, roster.tokenHolder());
		assertEquals(0, roster.highestEpoch());
		assertEquals(1, roster.members().size());
	}

	@Test
	void admissionLeaveAndEjectionRunTheLifecycle() {

		Roster roster = session();
		roster.proposeAdmit(ALEX, BEA, "Bea");
		roster.proposeAdmit(BEA, CARL, "Carl");
		assertEquals(3, roster.members().size());
		assertEquals("Carl", roster.memberName(CARL));

		roster.proposeLeave(CARL);
		assertFalse(roster.isMember(CARL));
		assertNull(roster.memberName(CARL));

		roster.proposeEject(BEA, ALEX);
		assertFalse(roster.isMember(ALEX));
		assertTrue(roster.isMember(BEA));
		assertEquals(4, roster.highestEpoch());
	}

	@Test
	void sameEpochConflictGoesToTheLowestProposerAndLoserReissues() {

		// Two replicas at the same state propose different admissions
		// for the same epoch.
		Roster alexReplica = session();
		Roster beaReplica = session();
		SessionEntry admitBea = alexReplica.proposeAdmit(ALEX, BEA,
				"Bea");
		beaReplica.receive(admitBea);

		SessionEntry alexAdmitsCarl = alexReplica.proposeAdmit(ALEX,
				CARL, "Carl");
		SessionEntry beaAdmitsDana = beaReplica.proposeAdmit(BEA, DANA,
				"Dana");
		assertEquals(alexAdmitsCarl.epoch(), beaAdmitsDana.epoch());

		// Cross-deliver: both fold both entries; Alex's id is lower.
		beaReplica.receive(alexAdmitsCarl);
		alexReplica.receive(beaAdmitsDana);
		assertConverged(alexReplica, beaReplica);
		assertTrue(alexReplica.isMember(CARL));
		assertFalse(alexReplica.isMember(DANA));
		assertTrue(beaReplica.applied(alexAdmitsCarl));
		assertFalse(beaReplica.applied(beaAdmitsDana));

		// The loser re-issues at a fresh epoch and the intent lands.
		SessionEntry reissue = beaReplica.proposeAdmit(BEA, DANA,
				"Dana");
		alexReplica.receive(reissue);
		assertConverged(alexReplica, beaReplica);
		assertTrue(alexReplica.isMember(DANA));
	}

	@Test
	void tokenGrantMovesTheSingleWriter() {

		Roster roster = session();
		roster.proposeAdmit(ALEX, BEA, "Bea");
		roster.proposeTokenGrant(ALEX, BEA);
		assertEquals(BEA, roster.tokenHolder());

		// The old holder can no longer grant.
		assertThrows(IllegalStateException.class,
				() -> roster.proposeTokenGrant(ALEX, BEA));
	}

	@Test
	void claimFencesOutACrashedHolder() {

		// Bea holds the token and "crashes": her replica stops seeing
		// entries. Alex times her out and claims.
		Roster alexReplica = session();
		Roster beaReplica = session();
		SessionEntry admit = alexReplica.proposeAdmit(ALEX, BEA, "Bea");
		SessionEntry grant = alexReplica.proposeTokenGrant(ALEX, BEA);
		beaReplica.receive(admit);
		beaReplica.receive(grant);
		assertEquals(BEA, beaReplica.tokenHolder());

		SessionEntry claim = alexReplica.proposeTokenClaim(ALEX);
		assertEquals(ALEX, alexReplica.tokenHolder());

		// Bea still believes she writes - until the fence arrives: on
		// folding the claim she stops being the holder.
		assertEquals(BEA, beaReplica.tokenHolder());
		beaReplica.receive(claim);
		assertEquals(ALEX, beaReplica.tokenHolder());
		assertConverged(alexReplica, beaReplica);
	}

	@Test
	void holderDepartureLeavesTheTokenUnheldUntilClaimed() {

		Roster roster = session();
		roster.proposeAdmit(ALEX, BEA, "Bea");
		roster.proposeTokenGrant(ALEX, BEA);
		roster.proposeLeave(BEA);
		assertNull(roster.tokenHolder());

		// Nobody may write until an explicit claim.
		roster.proposeTokenClaim(ALEX);
		assertEquals(ALEX, roster.tokenHolder());

		// Same rule for ejection.
		roster.proposeAdmit(ALEX, CARL, "Carl");
		roster.proposeTokenGrant(ALEX, CARL);
		roster.proposeEject(ALEX, CARL);
		assertNull(roster.tokenHolder());
	}

	@Test
	void entriesFromAnEjectedPeerNoLongerApply() {

		Roster alexReplica = session();
		Roster beaReplica = session();
		SessionEntry admit = alexReplica.proposeAdmit(ALEX, BEA, "Bea");
		beaReplica.receive(admit);

		// Same-epoch race: Alex ejects Bea while Bea, not yet knowing,
		// admits Carl. Alex's lower id wins the epoch.
		SessionEntry eject = alexReplica.proposeEject(ALEX, BEA);
		SessionEntry beaAdmit = beaReplica.proposeAdmit(BEA, CARL,
				"Carl");
		alexReplica.receive(beaAdmit);
		beaReplica.receive(eject);
		assertConverged(alexReplica, beaReplica);
		assertFalse(alexReplica.isMember(BEA));
		assertFalse(alexReplica.isMember(CARL));

		// A later entry proposed by the ejected peer is folded but
		// never applies: its proposer is no longer a member.
		SessionEntry stale = new SessionEntry(
				alexReplica.highestEpoch() + 1, BEA, EntryKind.ADMIT,
				DANA, "Dana");
		alexReplica.receive(stale);
		assertFalse(alexReplica.isMember(DANA));
		assertFalse(alexReplica.applied(stale));
	}

	@Test
	void receiptIsIdempotentAndOrderIndependent() {

		Roster inOrder = session();
		SessionEntry admitBea = inOrder.proposeAdmit(ALEX, BEA, "Bea");
		SessionEntry admitCarl = inOrder.proposeAdmit(BEA, CARL,
				"Carl");
		SessionEntry grant = inOrder.proposeTokenGrant(ALEX, CARL);

		// Deliver backwards: nothing applies until the causal earlier
		// epochs arrive, then the refold catches everything up.
		Roster reversed = session();
		assertFalse(reversed.receive(grant));
		assertEquals(ALEX, reversed.tokenHolder());
		assertFalse(reversed.receive(admitCarl));
		assertTrue(reversed.receive(admitBea));
		assertConverged(inOrder, reversed);
		assertEquals(CARL, reversed.tokenHolder());

		// Duplicates change nothing.
		assertFalse(reversed.receive(admitCarl));
		assertFalse(reversed.receive(grant));
		assertConverged(inOrder, reversed);
	}

	@Test
	void proposalsInvalidAgainstTheLocalViewAreRefused() {

		Roster roster = session();
		// Non-members cannot propose.
		assertThrows(IllegalStateException.class,
				() -> roster.proposeAdmit(BEA, CARL, "Carl"));
		assertThrows(IllegalStateException.class,
				() -> roster.proposeLeave(BEA));
		assertThrows(IllegalStateException.class,
				() -> roster.proposeTokenClaim(BEA));
		// Double admission is refused.
		roster.proposeAdmit(ALEX, BEA, "Bea");
		assertThrows(IllegalStateException.class,
				() -> roster.proposeAdmit(ALEX, BEA, "Bea"));
		// Granting without the token is refused.
		assertThrows(IllegalStateException.class,
				() -> roster.proposeTokenGrant(BEA, ALEX));
		// Ejecting a non-member is refused.
		assertThrows(IllegalStateException.class,
				() -> roster.proposeEject(ALEX, CARL));
	}

	@Test
	void structurallyInvalidEntriesCannotExist() {

		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(0, ALEX, EntryKind.LEAVE, ALEX,
						""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.ADMIT, ALEX,
						"Alex"));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.ADMIT, BEA,
						""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.LEAVE, BEA,
						""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.TOKEN_CLAIM,
						BEA, ""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.EJECT, ALEX,
						""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.TOKEN_GRANT,
						ALEX, ""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionEntry(1, ALEX, EntryKind.EJECT, BEA,
						"Bea"));
		assertThrows(IllegalArgumentException.class,
				() -> new PeerId(""));
		assertThrows(IllegalArgumentException.class,
				() -> new Roster(ALEX, ""));
		assertThrows(IllegalArgumentException.class,
				() -> session().receive(null));
	}

} // end of RosterTest class
