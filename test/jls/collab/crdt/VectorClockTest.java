package jls.collab.crdt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * The vector-clock algebra the replication substrate rests on (issue
 * #171): counters, increment, entrywise-max merge with its lattice laws
 * (commutative, associative, idempotent), and the causal-order
 * comparison including its duality and its agreement with merge.
 */
class VectorClockTest {

	/** The property-trial seed; recorded per issue #171 section 8. */
	private static final long SEED = 424171L;

	/** How many random clock triples the law trials draw. */
	private static final int TRIALS = 2_000;

	/**
	 * A random clock over a small peer universe, so trials collide
	 * often enough to exercise every comparison outcome.
	 *
	 * @param random The seeded source.
	 *
	 * @return the clock.
	 */
	private static VectorClock randomClock(Random random) {

		Map<String, Long> counters = new HashMap<String, Long>();
		for (String peer : new String[] { "a", "b", "c", "d" }) {
			long count = random.nextInt(4);
			if (count > 0) {
				counters.put(peer, count);
			}
		}
		return VectorClock.of(counters);
	} // end of randomClock method

	/** Unobserved peers count zero; increment observes one op. */
	@Test
	void countersStartAtZeroAndIncrementPerPeer() {

		VectorClock clock = VectorClock.EMPTY;
		assertEquals(0, clock.counter("alice"));
		VectorClock one = clock.increment("alice");
		assertEquals(1, one.counter("alice"));
		assertEquals(0, one.counter("bob"));
		assertEquals(0, clock.counter("alice"),
				"increment must not mutate the receiver");
		assertEquals(2, one.increment("alice").counter("alice"));
	} // end of countersStartAtZeroAndIncrementPerPeer method

	/** Two clocks that observed the same history are equal. */
	@Test
	void equalityIsByObservedHistory() {

		VectorClock viaIncrement =
				VectorClock.EMPTY.increment("a").increment("b")
						.increment("a");
		VectorClock viaOf =
				VectorClock.of(Map.of("a", 2L, "b", 1L));
		assertEquals(viaOf, viaIncrement);
		assertEquals(viaOf.hashCode(), viaIncrement.hashCode());
		assertEquals(viaOf.entries(), viaIncrement.entries());
	} // end of equalityIsByObservedHistory method

	/** Malformed peers and non-positive counters are rejected. */
	@Test
	void ofRejectsMalformedPeersAndCounts() {

		assertThrows(IllegalArgumentException.class,
				() -> VectorClock.of(Map.of("UPPER", 1L)));
		assertThrows(IllegalArgumentException.class,
				() -> VectorClock.of(Map.of("", 1L)));
		assertThrows(IllegalArgumentException.class,
				() -> VectorClock.of(Map.of("a".repeat(65), 1L)));
		assertThrows(IllegalArgumentException.class,
				() -> VectorClock.of(Map.of("a", 0L)));
		assertThrows(IllegalArgumentException.class,
				() -> VectorClock.of(Map.of("a", -3L)));
		assertThrows(IllegalArgumentException.class,
				() -> VectorClock.EMPTY.increment("no spaces"));
	} // end of ofRejectsMalformedPeersAndCounts method

	/** A counter at the representable maximum refuses to advance. */
	@Test
	void incrementOverflowIsRefused() {

		VectorClock atMax =
				VectorClock.of(Map.of("a", Long.MAX_VALUE));
		assertThrows(IllegalStateException.class,
				() -> atMax.increment("a"));
	} // end of incrementOverflowIsRefused method

	/** Merge is the entrywise maximum. */
	@Test
	void mergeTakesEntrywiseMax() {

		VectorClock left = VectorClock.of(Map.of("a", 3L, "b", 1L));
		VectorClock right = VectorClock.of(Map.of("b", 4L, "c", 2L));
		VectorClock merged = left.merge(right);
		assertEquals(VectorClock.of(Map.of("a", 3L, "b", 4L, "c", 2L)),
				merged);
	} // end of mergeTakesEntrywiseMax method

	/** Merge satisfies the join-semilattice laws on random clocks. */
	@Test
	void mergeIsCommutativeAssociativeIdempotent() {

		Random random = new Random(SEED);
		for (int trial = 0; trial < TRIALS; trial += 1) {
			VectorClock a = randomClock(random);
			VectorClock b = randomClock(random);
			VectorClock c = randomClock(random);
			assertEquals(a.merge(b), b.merge(a), "commutativity");
			assertEquals(a.merge(b).merge(c), a.merge(b.merge(c)),
					"associativity");
			assertEquals(a, a.merge(a), "idempotence");
			VectorClock joined = a.merge(b);
			assertTrue(joined.compare(a) != VectorClock.Order.BEFORE
					&& joined.compare(a) != VectorClock.Order.CONCURRENT,
					"the join dominates its left input");
			assertTrue(joined.compare(b) != VectorClock.Order.BEFORE
					&& joined.compare(b) != VectorClock.Order.CONCURRENT,
					"the join dominates its right input");
		}
	} // end of mergeIsCommutativeAssociativeIdempotent method

	/** Each causal-order outcome on crafted clocks. */
	@Test
	void compareDistinguishesAllFourOrders() {

		VectorClock base = VectorClock.of(Map.of("a", 2L, "b", 1L));
		VectorClock same = VectorClock.of(Map.of("a", 2L, "b", 1L));
		VectorClock later = base.increment("b");
		VectorClock sibling = VectorClock.of(Map.of("a", 1L, "c", 1L));
		assertEquals(VectorClock.Order.EQUAL, base.compare(same));
		assertEquals(VectorClock.Order.BEFORE, base.compare(later));
		assertEquals(VectorClock.Order.AFTER, later.compare(base));
		assertEquals(VectorClock.Order.CONCURRENT,
				base.compare(sibling));
		assertEquals(VectorClock.Order.CONCURRENT,
				sibling.compare(base));
		assertEquals(VectorClock.Order.BEFORE,
				VectorClock.EMPTY.compare(base));
	} // end of compareDistinguishesAllFourOrders method

	/** Comparison is dual and agrees with merge on random clocks. */
	@Test
	void compareIsDualAndAgreesWithMerge() {

		Random random = new Random(SEED + 1);
		for (int trial = 0; trial < TRIALS; trial += 1) {
			VectorClock a = randomClock(random);
			VectorClock b = randomClock(random);
			VectorClock.Order forward = a.compare(b);
			VectorClock.Order backward = b.compare(a);
			switch (forward) {
			case EQUAL:
				assertEquals(VectorClock.Order.EQUAL, backward);
				assertEquals(a, b);
				break;
			case BEFORE:
				assertEquals(VectorClock.Order.AFTER, backward);
				assertEquals(b, a.merge(b),
						"a BEFORE b means b already contains a");
				break;
			case AFTER:
				assertEquals(VectorClock.Order.BEFORE, backward);
				assertEquals(a, a.merge(b),
						"a AFTER b means a already contains b");
				break;
			case CONCURRENT:
				assertEquals(VectorClock.Order.CONCURRENT, backward);
				assertTrue(!a.merge(b).equals(a)
						&& !a.merge(b).equals(b),
						"concurrent clocks each miss something");
				break;
			default:
				throw new AssertionError("unreachable");
			}
		}
	} // end of compareIsDualAndAgreesWithMerge method

} // end of VectorClockTest class
