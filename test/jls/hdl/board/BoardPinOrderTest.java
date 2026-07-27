package jls.hdl.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The pin listing a {@link Board} shows in unknown-pin errors must be
 * scannable: digit runs compare numerically ({@code PMOD2} before
 * {@code PMOD10}), not lexically, so multi-digit pin names never
 * interleave out of order and the user can find the pin they meant.
 */
class BoardPinOrderTest {

	/** A board with the given pins, each mapped to a dummy location. */
	private static Board boardWith(String... pinNames) {
		Map<String, String> pins = new HashMap<String, String>();
		int location = 1;
		for (String pin : pinNames) {
			pins.put(pin, Integer.toString(location));
			location += 1;
		}
		return new Board("testboard", "test FPGA", Board.Format.PCF,
				pins);
	}

	@Test
	void digitRunsSortNumericallyNotLexically() {

		Board board = boardWith(
				"PMOD10", "PMOD2", "PMOD1", "J1_10", "J1_3", "CLK");
		assertEquals("CLK, J1_3, J1_10, PMOD1, PMOD2, PMOD10",
				board.pinNames());
	}

	@Test
	void distinctNamesWithEqualNumericValuesBothSurvive() {

		// A1 and A01 are numerically equal but must stay two map keys
		Board board = boardWith("A1", "A01");
		assertEquals(2, board.pins().size());
		assertEquals("A01, A1", board.pinNames());
	}

	@Test
	void theRealIcestickListingIsInNaturalOrder() {

		Board icestick = Boards.byName("icestick");
		assertTrue(icestick != null);
		String names = icestick.pinNames();
		// lexically J1_10 < J1_3; naturally the reverse
		int j13 = names.indexOf("J1_3");
		int j110 = names.indexOf("J1_10");
		if (j13 >= 0 && j110 >= 0) {
			assertTrue(j13 < j110, names);
		}
	}
}
