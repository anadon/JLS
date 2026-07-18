package jls.collab.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * The seeded randomized-schedule harness for issue #169's hypotheses
 * H1 and H2 (prediction P1's roster-and-token half): replicas of one
 * session propose admissions, departures, ejections, grants, and
 * claims from stale local views while a chaos bus drops, duplicates,
 * and reorders deliveries. After each schedule, one anti-entropy round
 * (every replica folds the union of all logs - research doc section
 * 5.4's resync, bounded to a single round so convergence cannot hide
 * behind "eventually") must leave every replica with an identical
 * roster, identical names, an identical token holder, and at most one
 * peer believing itself the writer.
 *
 * Failures print the seed; add any minimized failing seed as a
 * directed regression in {@link RosterTest}.
 */
class RosterConvergenceTest {

	/** How many seeded schedules to run. */
	private static final int SCHEDULES = 1000;

	/** Random steps per schedule before the anti-entropy round. */
	private static final int STEPS = 80;

	/** The peer pool; the design target is 2-8 peers per session. */
	private static final PeerId[] PEERS = {
			new PeerId("p0"), new PeerId("p1"), new PeerId("p2"),
			new PeerId("p3"), new PeerId("p4"), new PeerId("p5") };

	/** One queued network delivery on the chaos bus. */
	private record Delivery(PeerId target, SessionEntry entry) {

	} // end of Delivery record

	@Test
	void allSeededSchedulesConvergeInOneAntiEntropyRound() {

		for (int seed = 0; seed < SCHEDULES; seed++) {
			runSchedule(seed);
		}
	}

	/**
	 * Run one seeded schedule and assert convergence.
	 *
	 * @param seed The schedule seed.
	 */
	private static void runSchedule(int seed) {

		Random random = new Random(seed);
		Map<PeerId, Roster> replicas =
				new LinkedHashMap<PeerId, Roster>();
		for (PeerId peer : PEERS) {
			replicas.put(peer, new Roster(PEERS[0], "Peer 0"));
		}
		List<Delivery> bus = new ArrayList<Delivery>();
		List<SessionEntry> everIssued = new ArrayList<SessionEntry>();

		for (int step = 0; step < STEPS; step++) {
			int dice = random.nextInt(10);
			if (dice < 4 && !bus.isEmpty()) {
				// Deliver one queued message, in random order.
				Delivery delivery =
						bus.remove(random.nextInt(bus.size()));
				replicas.get(delivery.target())
						.receive(delivery.entry());
			} else if (dice < 5 && !bus.isEmpty()) {
				// Drop one queued message; anti-entropy heals it.
				bus.remove(random.nextInt(bus.size()));
			} else if (dice < 6 && !everIssued.isEmpty()) {
				// Duplicate an old delivery to a random replica.
				replicas.get(PEERS[random.nextInt(PEERS.length)])
						.receive(everIssued.get(
								random.nextInt(everIssued.size())));
			} else {
				propose(random, replicas, bus, everIssued);
			}
		}

		// One anti-entropy round: every replica folds the union.
		TreeSet<SessionEntry> union = new TreeSet<SessionEntry>();
		for (Roster replica : replicas.values()) {
			union.addAll(replica.entries());
		}
		for (Roster replica : replicas.values()) {
			for (SessionEntry entry : union) {
				replica.receive(entry);
			}
		}

		// H1: identical rosters everywhere after quiescence.
		Roster reference = replicas.get(PEERS[0]);
		for (Map.Entry<PeerId, Roster> pair : replicas.entrySet()) {
			Roster replica = pair.getValue();
			String at = "seed " + seed + ", replica " + pair.getKey();
			assertEquals(reference.members(), replica.members(), at);
			assertEquals(reference.tokenHolder(),
					replica.tokenHolder(), at);
			assertEquals(reference.highestEpoch(),
					replica.highestEpoch(), at);
			assertEquals(reference.entries(), replica.entries(), at);
		}

		// H2: at most one peer believes itself the writer.
		long writers = replicas.entrySet().stream()
				.filter(pair -> pair.getKey()
						.equals(pair.getValue().tokenHolder()))
				.count();
		assertTrue(writers <= 1,
				"seed " + seed + ": " + writers + " writers");
	} // end of runSchedule method

	/**
	 * Have a random peer propose a random change that is valid in its
	 * own, possibly stale, local view - exactly what a real client
	 * would do - and broadcast it onto the chaos bus.
	 *
	 * @param random The schedule's randomness.
	 * @param replicas Every peer's replica.
	 * @param bus The pending deliveries.
	 * @param everIssued Every entry ever broadcast, for duplication.
	 */
	private static void propose(Random random,
			Map<PeerId, Roster> replicas, List<Delivery> bus,
			List<SessionEntry> everIssued) {

		// Only a peer that believes itself a member proposes.
		List<PeerId> proposers = new ArrayList<PeerId>();
		for (PeerId peer : PEERS) {
			if (replicas.get(peer).isMember(peer)) {
				proposers.add(peer);
			}
		}
		if (proposers.isEmpty()) {
			return;
		}
		PeerId proposer =
				proposers.get(random.nextInt(proposers.size()));
		Roster view = replicas.get(proposer);

		List<PeerId> insiders = new ArrayList<PeerId>();
		List<PeerId> outsiders = new ArrayList<PeerId>();
		for (PeerId peer : PEERS) {
			if (peer.equals(proposer)) {
				continue;
			}
			if (view.isMember(peer)) {
				insiders.add(peer);
			} else {
				outsiders.add(peer);
			}
		}

		SessionEntry entry = null;
		int action = random.nextInt(100);
		if (action < 40) {
			if (!outsiders.isEmpty()) {
				PeerId subject = outsiders
						.get(random.nextInt(outsiders.size()));
				entry = view.proposeAdmit(proposer, subject, "Peer "
						+ subject.fingerprint().substring(1));
			}
		} else if (action < 55) {
			if (proposer.equals(view.tokenHolder())
					&& !insiders.isEmpty()) {
				entry = view.proposeTokenGrant(proposer, insiders
						.get(random.nextInt(insiders.size())));
			}
		} else if (action < 75) {
			// Claim when the token looks unheld, or - rarely - on a
			// simulated heartbeat-timeout suspicion of the holder.
			if (view.tokenHolder() == null
					|| random.nextInt(5) == 0) {
				entry = view.proposeTokenClaim(proposer);
			}
		} else if (action < 88) {
			if (!insiders.isEmpty()) {
				entry = view.proposeEject(proposer, insiders
						.get(random.nextInt(insiders.size())));
			}
		} else if (action < 94) {
			entry = view.proposeLeave(proposer);
		}
		// else: idle step.

		if (entry != null) {
			everIssued.add(entry);
			for (PeerId target : PEERS) {
				if (!target.equals(proposer)) {
					bus.add(new Delivery(target, entry));
				}
			}
		}
	} // end of propose method

} // end of RosterConvergenceTest class
