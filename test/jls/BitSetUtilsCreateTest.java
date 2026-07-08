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
