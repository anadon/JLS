package jls.ui;

import static jls.ui.CircuitAssert.assertBits;
import static jls.ui.CircuitAssert.assertConnected;
import static jls.ui.CircuitAssert.assertElementPresent;
import static jls.ui.CircuitAssert.assertNotConnected;
import static jls.ui.CircuitAssert.assertPutBits;
import static jls.ui.CircuitAssert.assertWatched;
import static jls.ui.GeometryAssert.assertAbove;
import static jls.ui.GeometryAssert.assertDimensions;
import static jls.ui.GeometryAssert.assertElementAt;
import static jls.ui.GeometryAssert.assertLeftOf;
import static jls.ui.GeometryAssert.assertOnGrid;
import static jls.ui.GeometryAssert.assertWithinGridUnits;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.AndGate;
import jls.elem.Constant;
import jls.elem.OutputPin;

/**
 * Pilot for the Layer-1 UI-verification harness (issue #91, acceptance
 * criterion P1): place two elements, wire them, and assert presence,
 * absolute + relative position, dimensions, and connectivity -- all
 * headless, against model state only.
 *
 * <p>The nested {@code EveryAssertionCanFail} class is the
 * assert-the-assertion discipline from the issue: each helper is pinned
 * by a deliberately-failing case so the harness cannot silently pass on
 * an empty circuit.</p>
 */
class UiHarnessPilotTest {

	/**
	 * The P1 fixture: a constant wired to a watched output pin, written
	 * in the on-disk text format and loaded headless (finishLoad(null)
	 * builds the wire nets, so connectivity is assertable).
	 */
	private static String wiredFixture() {
		StringBuilder text = new StringBuilder("CIRCUIT pilot\n");
		text.append("ELEMENT Constant\n int id 0\n int x 60\n int y 60\n")
				.append(" int width 24\n int height 24\n Int value 5\n")
				.append(" int base 10\n String orient \"RIGHT\"\nEND\n");
		text.append("ELEMENT OutputPin\n int id 1\n int x 240\n int y 120\n")
				.append(" String name \"out\"\n int bits 1\n int watch 1\n")
				.append(" String orient \"RIGHT\"\nEND\n");
		wire(text, 2, 0, "output", 1, "input");
		text.append("ENDCIRCUIT\n");
		return text.toString();
	}

	/**
	 * A jump-aliased variant: constant -> JumpStart "js", then a JumpEnd
	 * (name parameterized) -> output pin, plus an idle JumpStart "zz" so
	 * the mismatched name still resolves to a registered jump start.
	 */
	private static String jumpFixture(String endName) {
		StringBuilder text = new StringBuilder("CIRCUIT jumps\n");
		text.append("ELEMENT Constant\n int id 0\n int x 60\n int y 60\n")
				.append(" int width 24\n int height 24\n Int value 5\n")
				.append(" int base 10\n String orient \"RIGHT\"\nEND\n");
		text.append("ELEMENT JumpStart\n int id 1\n int x 180\n int y 60\n")
				.append(" String name \"js\"\n int bits 1\n int watch 0\n")
				.append(" String orientation \"LEFT\"\nEND\n");
		text.append("ELEMENT JumpEnd\n int id 2\n int x 300\n int y 60\n")
				.append(" String name \"").append(endName).append("\"\n")
				.append(" int bits 1\n String orientation \"RIGHT\"\nEND\n");
		text.append("ELEMENT OutputPin\n int id 3\n int x 420\n int y 60\n")
				.append(" String name \"out\"\n int bits 1\n int watch 0\n")
				.append(" String orient \"RIGHT\"\nEND\n");
		text.append("ELEMENT JumpStart\n int id 4\n int x 180\n int y 180\n")
				.append(" String name \"zz\"\n int bits 1\n int watch 0\n")
				.append(" String orientation \"LEFT\"\nEND\n");
		wire(text, 5, 0, "output", 1, "input");
		wire(text, 7, 2, "output", 3, "input");
		text.append("ENDCIRCUIT\n");
		return text.toString();
	}

	private static void wire(StringBuilder text, int firstId, int fromElement,
			String fromPut, int toElement, String toPut) {
		int endA = firstId;
		int endB = firstId + 1;
		wireEnd(text, endA, fromElement, fromPut, endB, 120, 72);
		wireEnd(text, endB, toElement, toPut, endA, 216, 132);
	}

	private static void wireEnd(StringBuilder text, int id, int attachTo,
			String put, int otherEnd, int x, int y) {
		text.append("ELEMENT WireEnd\n int id ").append(id)
				.append("\n int x ").append(x).append("\n int y ").append(y)
				.append("\n String put \"").append(put)
				.append("\"\n ref attach ").append(attachTo)
				.append("\n ref wire ").append(otherEnd).append("\nEND\n");
	}

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	// ------------------------------------------------------------------
	// P1 pilot
	// ------------------------------------------------------------------

	@Test
	void pilotPresenceGeometryDimensionsAndConnectivity() throws Exception {
		Circuit circuit = load(wiredFixture());

		// presence, by type and by (type, name)
		Constant constant = assertElementPresent(circuit, Constant.class);
		OutputPin pin = assertElementPresent(circuit, OutputPin.class, "out");

		// absolute position, on the snap grid
		assertElementAt(constant, 60, 60);
		assertElementAt(pin, 240, 120);
		assertOnGrid(constant);
		assertOnGrid(pin);

		// dimensions: the model bounding box (getRect); a RIGHT-facing
		// constant pads its declared 24x24 body by half a grid spacing
		// above and below for the value label
		assertDimensions(constant, 24, 24 + JLSInfo.spacing);

		// relative position: whole-box comparisons and grid distance
		assertLeftOf(constant, pin);
		assertAbove(constant, pin);
		assertWithinGridUnits(constant, pin, 13);

		// connectivity: constant output -> pin input, and directional
		assertConnected(circuit, constant, pin);
		assertNotConnected(circuit, pin, constant);

		// characteristics
		assertBits(pin, 1);
		assertPutBits(pin, "input", 1);
		assertWatched(pin, true);
		assertWatched(constant, false);
	}

	@Test
	void connectivityFollowsJumpAliases() throws Exception {
		Circuit circuit = load(jumpFixture("js"));
		Constant constant = assertElementPresent(circuit, Constant.class);
		OutputPin pin = assertElementPresent(circuit, OutputPin.class, "out");
		// the only path is constant -> JumpStart js ... JumpEnd js -> pin
		assertConnected(circuit, constant, pin);
	}

	@Test
	void jumpAliasingRequiresMatchingNames() throws Exception {
		Circuit circuit = load(jumpFixture("zz"));
		Constant constant = assertElementPresent(circuit, Constant.class);
		OutputPin pin = assertElementPresent(circuit, OutputPin.class, "out");
		// JumpEnd "zz" does not alias JumpStart "js", so no path
		assertNotConnected(circuit, constant, pin);
	}

	// ------------------------------------------------------------------
	// assert-the-assertion: every helper must be able to fail
	// ------------------------------------------------------------------

	/**
	 * Assert-the-assertion suite: each test pins one harness helper by
	 * feeding it a case that must fail, proving the assertion actually
	 * rejects bad input rather than silently passing on an empty or
	 * mismatched circuit.
	 */
	@Nested
	class EveryAssertionCanFail {

		@Test
		void elementPresentByTypeFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			assertThrows(AssertionError.class,
					() -> assertElementPresent(circuit, AndGate.class),
					"absent type must fail");
			// ambiguity must fail too: the jump fixture has two JumpStarts
			Circuit jumps = load(jumpFixture("js"));
			assertThrows(AssertionError.class, () -> assertElementPresent(
					jumps, jls.elem.JumpStart.class),
					"multiple matches must fail the unnamed overload");
		}

		@Test
		void elementPresentByNameFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			assertThrows(AssertionError.class, () -> assertElementPresent(
					circuit, OutputPin.class, "nope"));
		}

		@Test
		void elementAtFails() throws Exception {
			Constant constant = assertElementPresent(load(wiredFixture()),
					Constant.class);
			assertThrows(AssertionError.class,
					() -> assertElementAt(constant, 60, 61));
		}

		@Test
		void onGridFails() throws Exception {
			// LogicElement setXY/move snap, so logic elements hold the
			// grid invariant by construction; a display element inherits
			// the base non-snapping setXY (Element is abstract since the
			// #95 sealing), which is exactly the state this must catch
			jls.elem.Element raw = new jls.elem.Text(load(wiredFixture()));
			raw.setXY(61, 60);
			assertThrows(AssertionError.class, () -> assertOnGrid(raw));
		}

		@Test
		void dimensionsFail() throws Exception {
			Constant constant = assertElementPresent(load(wiredFixture()),
					Constant.class);
			assertThrows(AssertionError.class,
					() -> assertDimensions(constant, 24, 12));
		}

		@Test
		void leftOfFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			Constant constant = assertElementPresent(circuit, Constant.class);
			OutputPin pin = assertElementPresent(circuit, OutputPin.class,
					"out");
			assertThrows(AssertionError.class,
					() -> assertLeftOf(pin, constant));
		}

		@Test
		void aboveFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			Constant constant = assertElementPresent(circuit, Constant.class);
			OutputPin pin = assertElementPresent(circuit, OutputPin.class,
					"out");
			assertThrows(AssertionError.class,
					() -> assertAbove(pin, constant));
		}

		@Test
		void withinGridUnitsFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			Constant constant = assertElementPresent(circuit, Constant.class);
			OutputPin pin = assertElementPresent(circuit, OutputPin.class,
					"out");
			// the fixture gap is exactly 13 grid units
			assertThrows(AssertionError.class,
					() -> assertWithinGridUnits(constant, pin, 12));
		}

		@Test
		void connectedFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			Constant constant = assertElementPresent(circuit, Constant.class);
			OutputPin pin = assertElementPresent(circuit, OutputPin.class,
					"out");
			// wrong direction: the pin has no output wired to the constant
			assertThrows(AssertionError.class,
					() -> assertConnected(circuit, pin, constant));
		}

		@Test
		void notConnectedFails() throws Exception {
			Circuit circuit = load(wiredFixture());
			Constant constant = assertElementPresent(circuit, Constant.class);
			OutputPin pin = assertElementPresent(circuit, OutputPin.class,
					"out");
			assertThrows(AssertionError.class,
					() -> assertNotConnected(circuit, constant, pin));
		}

		@Test
		void bitsFail() throws Exception {
			Circuit circuit = load(wiredFixture());
			Constant constant = assertElementPresent(circuit, Constant.class);
			OutputPin pin = assertElementPresent(circuit, OutputPin.class,
					"out");
			assertThrows(AssertionError.class, () -> assertBits(pin, 4));
			// elements without a single bit width fail cleanly, not with
			// a raw UnsupportedOperationException
			assertThrows(AssertionError.class, () -> assertBits(constant, 1));
		}

		@Test
		void putBitsFail() throws Exception {
			OutputPin pin = assertElementPresent(load(wiredFixture()),
					OutputPin.class, "out");
			assertThrows(AssertionError.class,
					() -> assertPutBits(pin, "input", 4));
			assertThrows(AssertionError.class,
					() -> assertPutBits(pin, "no-such-put", 1));
		}

		@Test
		void watchedFails() throws Exception {
			Constant constant = assertElementPresent(load(wiredFixture()),
					Constant.class);
			assertThrows(AssertionError.class,
					() -> assertWatched(constant, true));
		}
	}
}
