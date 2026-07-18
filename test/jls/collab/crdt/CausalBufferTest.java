package jls.collab.crdt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.collab.op.CircuitOp;
import jls.collab.op.ToggleWatched;
import jls.elem.ElementId;

/**
 * Causal delivery under adversarial arrival orders (issue #171,
 * section 5's delivery-order axis): a buffer delivers every envelope of
 * a causally complete set exactly once, never before its dependencies,
 * with duplicates dropped rather than double-applied — under scripted
 * schedules and under seeded random histories replayed in many arrival
 * permutations.
 */
class CausalBufferTest {

	/** The property-trial seed; recorded per issue #171 section 8. */
	private static final long SEED = 171_2026L;

	/** A representative op payload; delivery never looks inside it. */
	private static final CircuitOp OP =
			new ToggleWatched(ElementId.legacy(7));

	/**
	 * An envelope with an explicit clock.
	 *
	 * @param origin The originating peer.
	 * @param seq The origin's sequence number.
	 * @param clock The emission clock, peer to counter.
	 *
	 * @return the envelope.
	 */
	private static OpEnvelope envelope(String origin, long seq,
			Map<String, Long> clock) {

		return new OpEnvelope(new OpId(origin, seq),
				VectorClock.of(clock), OP);
	} // end of envelope method

	/** Out-of-order arrival buffers until dependencies arrive. */
	@Test
	void buffersUntilDependenciesArrive() {

		CausalBuffer buffer = new CausalBuffer();
		OpEnvelope a1 = envelope("a", 1, Map.of("a", 1L));
		OpEnvelope b1 = envelope("b", 1, Map.of("b", 1L));
		OpEnvelope a2 = envelope("a", 2, Map.of("a", 2L, "b", 1L));

		assertEquals(List.of(), buffer.offer(a2),
				"a2 depends on a1 and b1, neither delivered yet");
		assertEquals(1, buffer.pendingCount());
		assertEquals(List.of(b1), buffer.offer(b1));
		assertEquals(1, buffer.pendingCount(),
				"a2 still waits for a1");
		assertEquals(List.of(a1, a2), buffer.offer(a1),
				"a1 unblocks the buffered a2 in one cascade");
		assertEquals(0, buffer.pendingCount());
		assertEquals(VectorClock.of(Map.of("a", 2L, "b", 1L)),
				buffer.clock());
		assertEquals(0, buffer.duplicatesDropped());
	} // end of buffersUntilDependenciesArrive method

	/** A whole origin backlog cascades in sequence order. */
	@Test
	void cascadeDeliversABackedUpOriginInOrder() {

		CausalBuffer buffer = new CausalBuffer();
		OpEnvelope a1 = envelope("a", 1, Map.of("a", 1L));
		OpEnvelope a2 = envelope("a", 2, Map.of("a", 2L));
		OpEnvelope a3 = envelope("a", 3, Map.of("a", 3L));
		assertEquals(List.of(), buffer.offer(a3));
		assertEquals(List.of(), buffer.offer(a2));
		assertEquals(List.of(a1, a2, a3), buffer.offer(a1));
	} // end of cascadeDeliversABackedUpOriginInOrder method

	/** Re-presented envelopes are dropped, delivered or pending. */
	@Test
	void duplicatesAreDroppedNotDoubleApplied() {

		CausalBuffer buffer = new CausalBuffer();
		OpEnvelope a1 = envelope("a", 1, Map.of("a", 1L));
		OpEnvelope a3 = envelope("a", 3, Map.of("a", 3L));
		assertEquals(List.of(a1), buffer.offer(a1));
		assertEquals(List.of(), buffer.offer(a1),
				"an already-delivered envelope is a duplicate");
		assertEquals(1, buffer.duplicatesDropped());
		assertEquals(List.of(), buffer.offer(a3));
		assertEquals(List.of(), buffer.offer(a3),
				"an already-pending envelope is a duplicate");
		assertEquals(2, buffer.duplicatesDropped());
		assertEquals(1, buffer.pendingCount());
		assertEquals(VectorClock.of(Map.of("a", 1L)), buffer.clock());
	} // end of duplicatesAreDroppedNotDoubleApplied method

	/** Equivocation: same op id, different contents — first wins. */
	@Test
	void equivocatingDuplicateOfAPendingOpIsDropped() {

		CausalBuffer buffer = new CausalBuffer();
		OpEnvelope claimed = envelope("a", 2, Map.of("a", 2L));
		OpEnvelope forged = new OpEnvelope(new OpId("a", 2),
				VectorClock.of(Map.of("a", 2L)),
				new ToggleWatched(ElementId.legacy(9)));
		assertNotEquals(claimed, forged);
		assertEquals(List.of(), buffer.offer(claimed));
		assertEquals(List.of(), buffer.offer(forged));
		assertEquals(1, buffer.duplicatesDropped());
		assertEquals(List.of(
				envelope("a", 1, Map.of("a", 1L)), claimed),
				buffer.offer(envelope("a", 1, Map.of("a", 1L))),
				"the first-arrived contents are what gets delivered");
	} // end of equivocatingDuplicateOfAPendingOpIsDropped method

	/** A flood of far-future envelopes hits the buffer cap. */
	@Test
	void pendingOverflowIsRefused() {

		CausalBuffer buffer = new CausalBuffer();
		for (long seq = 2; seq <= 10_001; seq += 1) {
			assertEquals(List.of(), buffer.offer(
					envelope("a", seq, Map.of("a", seq))));
		}
		assertEquals(10_000, buffer.pendingCount());
		assertThrows(IllegalStateException.class,
				() -> buffer.offer(envelope("a", 10_002,
						Map.of("a", 10_002L))));
	} // end of pendingOverflowIsRefused method

	/**
	 * The property trial (issue #171 P1's delivery-order axis): a
	 * seeded random causal history over three peers, replayed to a
	 * fresh replica in many arrival permutations. Every permutation
	 * delivers every op exactly once, never before a causal
	 * dependency, with per-origin sequence order intact, and every
	 * replica ends at the identical clock.
	 */
	@Test
	void randomPermutationsAllDeliverExactlyOnceInCausalOrder() {

		Random random = new Random(SEED);
		List<OpEnvelope> history = randomHistory(random, 120);
		VectorClock everything = VectorClock.EMPTY;
		for (OpEnvelope envelope : history) {
			everything = everything.merge(envelope.clock());
		}

		for (int permutation = 0; permutation < 40; permutation += 1) {
			List<OpEnvelope> arrival =
					new ArrayList<OpEnvelope>(history);
			Collections.shuffle(arrival, random);
			CausalBuffer replica = new CausalBuffer();
			List<OpEnvelope> delivered = new ArrayList<OpEnvelope>();
			for (OpEnvelope envelope : arrival) {
				delivered.addAll(replica.offer(envelope));
			}

			assertEquals(history.size(), delivered.size(),
					"every op delivered, none lost");
			Set<OpId> seen = new HashSet<OpId>();
			for (OpEnvelope envelope : delivered) {
				assertTrue(seen.add(envelope.id()),
						"no op delivered twice");
			}
			assertEquals(0, replica.pendingCount(),
					"a causally complete set leaves nothing buffered");
			assertEquals(0, replica.duplicatesDropped());
			assertEquals(everything, replica.clock(),
					"every arrival order ends at the same clock");
			assertCausalOrder(delivered);
		}
	} // end of randomPermutationsAllDeliverExactlyOnceInCausalOrder
		// method

	/**
	 * Assert a delivered sequence never plays an op before one it
	 * causally depends on, and per-origin sequence numbers ascend.
	 *
	 * @param delivered The delivery order to check.
	 */
	private static void assertCausalOrder(List<OpEnvelope> delivered) {

		Map<String, Long> lastSeq = new HashMap<String, Long>();
		for (OpEnvelope envelope : delivered) {
			Long previous = lastSeq.get(envelope.origin());
			assertEquals(previous == null ? 1 : previous + 1,
					envelope.seq(),
					"per-origin delivery is gapless and in order");
			lastSeq.put(envelope.origin(), envelope.seq());
		}
		for (int later = 0; later < delivered.size(); later += 1) {
			for (int earlier = 0; earlier < later; earlier += 1) {
				assertNotEquals(VectorClock.Order.AFTER,
						delivered.get(earlier).clock()
								.compare(delivered.get(later).clock()),
						"an op never precedes its causal dependency");
			}
		}
	} // end of assertCausalOrder method

	/**
	 * Generate a valid causal history: three simulated peers, each
	 * emitting against its own replica clock, with random gossip
	 * between them (including redeliveries, which replicas must and do
	 * ignore).
	 *
	 * @param random The seeded source.
	 * @param steps How many emit-or-gossip steps to run.
	 *
	 * @return every emitted envelope, in emission order.
	 */
	private static List<OpEnvelope> randomHistory(Random random,
			int steps) {

		String[] peers = { "a", "b", "c" };
		Map<String, CausalBuffer> replicas =
				new HashMap<String, CausalBuffer>();
		for (String peer : peers) {
			replicas.put(peer, new CausalBuffer());
		}
		List<OpEnvelope> history = new ArrayList<OpEnvelope>();
		for (int step = 0; step < steps; step += 1) {
			String peer = peers[random.nextInt(peers.length)];
			CausalBuffer replica = replicas.get(peer);
			if (history.isEmpty() || random.nextInt(10) < 4) {
				VectorClock emission =
						replica.clock().increment(peer);
				OpEnvelope envelope = new OpEnvelope(
						new OpId(peer, emission.counter(peer)),
						emission, OP);
				assertEquals(List.of(envelope),
						replica.offer(envelope),
						"a peer self-delivers its own op immediately");
				history.add(envelope);
			} else {
				replica.offer(history
						.get(random.nextInt(history.size())));
			}
		}
		return history;
	} // end of randomHistory method

} // end of CausalBufferTest class
