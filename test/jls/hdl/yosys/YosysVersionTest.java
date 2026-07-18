package jls.hdl.yosys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Version-banner parsing and comparison for Yosys detection (issue
 * #61): real {@code yosys -V} banners parse, garbage returns absence
 * rather than throwing (addendum P4), and ordering respects the
 * {@code major.minor+commits} scheme against the pinned floor.
 */
class YosysVersionTest {

	@Test
	void parsesARealBanner() {
		Optional<YosysVersion> v = YosysVersion.parse(
				"Yosys 0.38+92 (git sha1 84116c9a3, clang 14.0.6)");
		assertTrue(v.isPresent());
		assertEquals(0, v.get().major());
		assertEquals(38, v.get().minor());
		assertEquals(92, v.get().commits());
		assertEquals("0.38+92", v.get().toString());
	}

	@Test
	void parsesAnExactReleaseAndBareVersions() {
		assertEquals(new YosysVersion(0, 9, 0),
				YosysVersion.parse("Yosys 0.9 (git sha1 1979e0b)")
						.get());
		assertEquals(new YosysVersion(0, 38, 0),
				YosysVersion.parse("0.38").get());
	}

	@Test
	void garbageIsAbsenceNotAnException() {
		assertTrue(YosysVersion.parse(null).isEmpty());
		assertTrue(YosysVersion.parse("").isEmpty());
		assertTrue(YosysVersion.parse("command not found")
				.isEmpty());
		assertTrue(YosysVersion.parse("Yosys").isEmpty());
	}

	@Test
	void orderingFollowsTheScheme() {
		YosysVersion old = new YosysVersion(0, 9, 3962);
		YosysVersion floor = YosysVersion.MINIMUM;
		YosysVersion pastFloor = new YosysVersion(0, 38, 92);
		YosysVersion newer = new YosysVersion(0, 44, 0);
		YosysVersion oneDay = new YosysVersion(1, 0, 0);
		assertFalse(old.atLeast(floor),
				"0.9+3962 is old despite the big commit count");
		assertTrue(floor.atLeast(floor));
		assertTrue(pastFloor.atLeast(floor));
		assertTrue(newer.atLeast(floor));
		assertTrue(oneDay.atLeast(newer));
		assertFalse(floor.atLeast(pastFloor),
				"+commits sorts after the exact release");
	}

	@Test
	void theFloorIsTheDocumentedPin() {
		assertEquals("0.38", YosysVersion.MINIMUM.toString(),
				"issue #61's addendum names 0.38; change the pin"
						+ " deliberately, with CI, or not at all");
	}

} // end of YosysVersionTest class
