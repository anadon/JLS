package jls.elem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Invalid element parameters in circuit files must be rejected at load
 * with a specific message instead of crashing or livelocking the
 * simulator later (issue #52, audit findings M12/M13 + Clock/Group Lows):
 * negative Memory capacity crashed DenseWordStore at simulation start,
 * non-positive Clock times livelocked the event loop at t=0, and
 * negative Group indices NPE'd inside the loader.
 */
class ParameterValidationTest {

	private static String loadExpectingFailure(String text) {
		Circuit circuit = new Circuit("invalid");
		boolean ok = circuit.load(new Scanner(text));
		assertFalse(ok, "an invalid parameter must fail the load");
		return JLSInfo.loadError;
	}

	private static String element(String type, String attrs) {
		return "CIRCUIT invalid\n"
				+ "ELEMENT " + type + "\n"
				+ " int id 0\n int x 60\n int y 60\n int width 32\n int height 32\n"
				+ attrs
				+ "END\n"
				+ "ENDCIRCUIT\n";
	}

	@Test
	void negativeMemoryCapacityFailsTheLoad() {
		String error = loadExpectingFailure(element("Memory",
				" String name \"mem\"\n int cap -5\n int bits 8\n"));
		assertTrue(error.contains(Memory.CAPACITY_CONSTRAINT),
				"expected the capacity constraint, got: " + error);
	}

	@Test
	void zeroClockCycleTimeFailsTheLoad() {
		String error = loadExpectingFailure(element("Clock",
				" int cycle 0\n int one 0\n"));
		assertTrue(error.contains(Clock.CYCLE_CONSTRAINT),
				"expected the cycle-time constraint, got: " + error);
	}

	@Test
	void negativeClockOneTimeFailsTheLoad() {
		String error = loadExpectingFailure(element("Clock",
				" int cycle 10\n int one -1\n"));
		assertTrue(error.contains(Clock.ONE_CONSTRAINT),
				"expected the one-time constraint, got: " + error);
	}

	@Test
	void negativeGroupIndexFailsTheLoad() {
		// Binder is a concrete Group subclass
		String error = loadExpectingFailure(element("Binder",
				" String noncontig \"true\"\n pair -1 3\n"));
		assertTrue(error.contains("non-negative"),
				"expected the group index constraint, got: " + error);
	}

	@Test
	void outOfRangeTruthTableEntryFailsTheLoad() {
		// declares a 2x2 table but writes to row 5
		String error = loadExpectingFailure(element("TruthTable",
				" String input \"a\"\n String output \"z\"\n"
						+ " int rows 2\n int cols 2\n pair 5 1\n"));
		assertTrue(error.contains("table"),
				"expected the table-bounds constraint, got: " + error);
	}

	@Test
	void validValuesStillLoad() {
		Circuit circuit = new Circuit("valid");
		assertTrue(circuit.load(new Scanner(element("Clock",
				" int cycle 20\n int one 10\n"))),
				() -> "valid clock must load: " + JLSInfo.loadError);
	}
}
