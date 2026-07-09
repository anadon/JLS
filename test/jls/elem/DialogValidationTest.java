package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Dialog-side parameter validation (issue #52 addendum): every element
 * dialog checks its constraints through ElementDialog.validateInputs(),
 * and each rule is stated exactly once - a shared helper/constant on the
 * element - that both the dialog and the file loader reject with (P5:
 * one string, two surfaces, never two wordings).
 *
 * Dialogs need a display, so these tests exercise the shared rule
 * helpers the dialogs call, and pin the P5 contract by asserting the
 * loader's rejection message for the same invalid value is exactly the
 * helper's message.
 */
class DialogValidationTest {

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
	void memoryCapacityRuleIsOneStringOnTwoSurfaces() {
		assertEquals(Memory.CAPACITY_CONSTRAINT, Memory.checkCapacity(0));
		assertEquals(Memory.CAPACITY_CONSTRAINT, Memory.checkCapacity(-5));
		assertNull(Memory.checkCapacity(1));
		String error = loadExpectingFailure(element("Memory",
				" String name \"mem\"\n int cap -5\n int bits 8\n"));
		assertTrue(error.contains(Memory.checkCapacity(-5)),
				"the loader must reject with the dialog's constraint string,"
						+ " got: " + error);
	}

	@Test
	void memoryBitsRuleIsOneStringOnTwoSurfaces() {
		assertEquals(Memory.BITS_CONSTRAINT, Memory.checkBits(0));
		assertNull(Memory.checkBits(1));
		String error = loadExpectingFailure(element("Memory",
				" String name \"mem\"\n int cap 4\n int bits 0\n"));
		assertTrue(error.contains(Memory.BITS_CONSTRAINT),
				"the loader must reject with the dialog's constraint string,"
						+ " got: " + error);
	}

	@Test
	void clockCycleTimeRuleIsOneStringOnTwoSurfaces() {
		assertEquals(Clock.CYCLE_CONSTRAINT, Clock.checkCycleTime(0));
		assertNull(Clock.checkCycleTime(1));
		String error = loadExpectingFailure(element("Clock",
				" int cycle 0\n int one 0\n"));
		assertTrue(error.contains(Clock.checkCycleTime(0)),
				"the loader must reject with the dialog's constraint string,"
						+ " got: " + error);
	}

	@Test
	void clockOneTimeRuleIsOneStringOnTwoSurfaces() {
		assertEquals(Clock.ONE_CONSTRAINT, Clock.checkOneTime(10, 0));
		// the dialog additionally requires one < cycle, same wording
		assertEquals(Clock.ONE_CONSTRAINT, Clock.checkOneTime(10, 10));
		assertNull(Clock.checkOneTime(10, 5));
		String error = loadExpectingFailure(element("Clock",
				" int cycle 10\n int one -1\n"));
		assertTrue(error.contains(Clock.checkOneTime(10, -1)),
				"the loader must reject with the dialog's constraint string,"
						+ " got: " + error);
	}

	@Test
	void groupBitsRuleIsOneStringOnTwoSurfaces() {
		assertEquals(Group.BITS_CONSTRAINT, Group.checkBits(1));
		assertNull(Group.checkBits(2));
		// Binder is a concrete Group subclass
		String error = loadExpectingFailure(element("Binder",
				" int bits 1\n"));
		assertTrue(error.contains(Group.checkBits(1)),
				"the loader must reject with the dialog's constraint string,"
						+ " got: " + error);
	}

	@Test
	void groupIndexRuleHasOneWording() {
		String error = loadExpectingFailure(element("Binder",
				" String noncontig \"true\"\n pair -1 3\n"));
		assertTrue(error.contains(Group.INDEX_CONSTRAINT),
				"the loader must reject with the shared constraint string,"
						+ " got: " + error);
	}

	@Test
	void truthTableEntryRuleHasOneWording() {
		// declares a 2x2 table but writes to row 5; the dialog enforces
		// the same bounds structurally (it only builds in-range entries)
		String error = loadExpectingFailure(element("TruthTable",
				" String input \"a\"\n String output \"z\"\n"
						+ " int rows 2\n int cols 2\n pair 5 1\n"));
		assertTrue(error.contains(TruthTable.entryConstraint(5, 0)),
				"the loader must reject with the shared constraint string,"
						+ " got: " + error);
	}

	@Test
	void stateMachineInitialStateRuleIsSharedWithSimStart() {
		// zero states: the editor dialog must refuse to close
		StateMachine machine = new StateMachine(new Circuit("dialog"));
		assertEquals(StateMachine.INITIAL_STATE_CONSTRAINT,
				machine.checkInitialState());

		// a state exists but none is marked initial: still refused
		machine.setValue("state", "S");
		assertEquals(StateMachine.INITIAL_STATE_CONSTRAINT,
				machine.checkInitialState());

		// marking it initial satisfies the rule
		machine.setValue("init", 1);
		assertNull(machine.checkInitialState());
	}
}
