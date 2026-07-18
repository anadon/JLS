package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Exact-value contract of {@link BitSetUtils#SumCarry}, pinned by the
 * PIT mutation trial (issue #161).  The adder is exercised heavily by
 * the element goldens, but only through {@code ToLong}-style
 * projections that ignore bits at index 64 and above (the {@code pow}
 * accumulator overflows to zero there) — so a mutant that negated the
 * final carry-out test at line 221 survived: it plants a spurious bit
 * at index {@code size} on every carry-free addition, invisible to
 * every projecting assertion.  These tests compare the returned
 * {@link BitSet} for exact equality, where a stray high bit fails.
 */
class BitSetUtilsSumCarryTest {

	@Test
	void carryFreeSumsAreExact() {

		assertEquals(BitSetUtils.Create(2L),
				BitSetUtils.SumCarry(false, BitSetUtils.Create(1L),
						BitSetUtils.Create(1L)),
				"1 + 1 must be exactly the bit pattern of 2");
		assertEquals(BitSetUtils.Create(0L),
				BitSetUtils.SumCarry(false, BitSetUtils.Create(0L),
						BitSetUtils.Create(0L)),
				"0 + 0 must be exactly the empty bit set");
	}

	@Test
	void carryInIsAdded() {

		assertEquals(BitSetUtils.Create(1L),
				BitSetUtils.SumCarry(true, BitSetUtils.Create(0L),
						BitSetUtils.Create(0L)),
				"carry-in alone must produce exactly 1");
		assertEquals(BitSetUtils.Create(4L),
				BitSetUtils.SumCarry(true, BitSetUtils.Create(1L),
						BitSetUtils.Create(2L)),
				"1 + 2 + carry-in must be exactly 4");
	}

	@Test
	void carryOutOfTheTopBitIsKept() {

		// two operands with only bit 63 set: BitSet.size() is 64 for
		// both, so the sum's carry lands at index 64 via the final
		// carry-out append — the only path that sets it
		BitSet topBit = new BitSet();
		topBit.set(63);
		BitSet expected = new BitSet();
		expected.set(64);
		assertEquals(expected,
				BitSetUtils.SumCarry(false, topBit, topBit),
				"2^63 + 2^63 must be exactly 2^64");
	}

	@Test
	void randomizedSumsMatchLongArithmeticExactly() {

		Random random = new Random(161);
		for (int i = 0; i < 1_000; i++) {
			// keep each operand below 2^62 so the true sum fits in a
			// positive long and Create(a + b) is a valid oracle
			long a = random.nextLong() >>> 2;
			long b = random.nextLong() >>> 2;
			assertEquals(BitSetUtils.Create(a + b),
					BitSetUtils.SumCarry(false, BitSetUtils.Create(a),
							BitSetUtils.Create(b)),
					() -> a + " + " + b
							+ " must match long arithmetic exactly");
		}
	}

} // end of BitSetUtilsSumCarryTest class
