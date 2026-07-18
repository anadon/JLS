package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * BitSetUtils.Create(long) round-trips exactly for every non-negative
 * long (issue #50, audit finding H2): the previous Math.log/Math.ceil
 * bit count under-counted for values wider than ~48 bits and silently
 * returned an empty BitSet.
 */
class BitSetUtilsCreateTest {

	private static void assertRoundTrip(long value) {
		BitSet bs = BitSetUtils.Create(value);
		assertEquals(value, BitSetUtils.ToLong(bs),
				"Create/ToLong must be the identity for " + value);
	}

	@Test
	void everyBitPositionRoundTrips() {
		for (int k = 0; k <= 62; k++) {
			assertRoundTrip(1L << k);
			assertRoundTrip((1L << k) - 1);
			assertRoundTrip((1L << k) + 1);
		}
		assertRoundTrip(Long.MAX_VALUE);
	}

	@Test
	void randomizedLongsRoundTrip() {
		Random random = new Random(42);
		for (int i = 0; i < 10_000; i++) {
			assertRoundTrip(random.nextLong() & Long.MAX_VALUE);
		}
	}

	/**
	 * Exact bit population, not just the ToLong projection: ToLong
	 * cannot see bits at index 64 and above (its pow accumulator
	 * overflows to zero), so a PIT mutant (issue #161) that turned the
	 * high-bit count from {@code 64 - nlz} into {@code 64 + nlz}
	 * survived the round-trip tests above — Java's shift-count
	 * masking makes {@code value >> 64 == value}, planting a mirror
	 * copy of every bit at index 64+.  Cardinality and length pin the
	 * population exactly.
	 */
	@Test
	void noBitsAppearAboveTheHighBit() {
		Random random = new Random(161);
		for (int i = 0; i < 1_000; i++) {
			long value = random.nextLong() & Long.MAX_VALUE;
			BitSet bs = BitSetUtils.Create(value);
			assertEquals(Long.bitCount(value), bs.cardinality(),
					"bit count must match for " + value);
			assertEquals(64 - Long.numberOfLeadingZeros(value),
					bs.length(),
					"highest set bit must match for " + value);
		}
	}

	@Test
	void zeroYieldsEmptyBitSet() {
		assertTrue(BitSetUtils.Create(0L).isEmpty());
	}

	@Test
	void negativeValuesAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> BitSetUtils.Create(-1L));
	}
}
