package jls;

import static jls.ui.CircuitAssert.assertConnected;
import static jls.ui.CircuitAssert.assertPutBits;
import static jls.ui.CircuitAssert.assertWatched;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.LogicElement;
import jls.elem.OutputPin;
import jls.elem.ShiftRegister;
import jls.sim.BatchSimulator;

/**
 * The shift-register element (issue #122): a combinational barrel
 * shifter under the fork-compatible tag "ShiftRegister"
 * (docs/simulation-semantics.md section 6.3). Two instruments:
 *
 * 1. semantics - each shift kind computed through the real loader and
 *    batch simulator, including the identity shift, the sign-fill
 *    rule, and amounts at or beyond the data width (only expressible
 *    when the width is not a power of two);
 * 2. fork interop (H2/P3 of the issue) - the committed fixture
 *    test/fixtures/fork-4.6-shiftregister.jls was written by the
 *    bsiever fork's own loader+writer, built from source at
 *    bsiever/JLS@038a5b67 (the element's last fork revision before
 *    the 4.6 release); it must load upstream, wire up (pinned with
 *    the issue-#91 UI-harness assertions), and simulate to the fork
 *    semantics.
 */
class ShiftRegisterTest {

	// ------------------------------------------------------------------
	// circuit text builder (the #14/#98 harness idiom)
	// ------------------------------------------------------------------

	/**
	 * Emits JLS circuit-description text programmatically, handing out
	 * auto-incrementing element ids and appending the ELEMENT/WireEnd
	 * records so a test can assemble a loadable circuit without a fixed
	 * on-disk fixture.
	 */
	private static final class CircuitBuilder {

		private final StringBuilder text;
		private int nextId = 0;

		CircuitBuilder(String name) {
			text = new StringBuilder("CIRCUIT " + name + "\n");
		}

		private int element(String type, String attrs) {
			int id = nextId++;
			text.append("ELEMENT ").append(type).append('\n')
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(60 + 48 * id).append('\n')
				.append(" int y 60\n")
				.append(" int width 24\n int height 24\n")
				.append(attrs)
				.append("END\n");
			return id;
		}

		int constant(long value) {
			return element("Constant", " Int value " + value
					+ "\n int base 10\n String orient \"RIGHT\"\n");
		}

		int outputPin(String name, int bits) {
			return element("OutputPin", " String name \"" + name
					+ "\"\n int bits " + bits
					+ "\n int watch 0\n String orient \"RIGHT\"\n");
		}

		int shiftRegister(String type, int bits) {
			return element("ShiftRegister", " String type \"" + type
					+ "\"\n int bits " + bits + "\n int delay 25\n"
					+ " String iOrient \"RIGHT\"\n String sOrient \"DOWN\"\n");
		}

		/** Wire fromElement's put to toElement's put (two-ended net). */
		void wire(int fromElement, String fromPut, int toElement,
				String toPut) {
			int endA = nextId++;
			int endB = nextId++;
			wireEnd(endA, fromElement, fromPut, endB);
			wireEnd(endB, toElement, toPut, endA);
		}

		private void wireEnd(int id, int attachTo, String put, int other) {
			text.append("ELEMENT WireEnd\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(12 * id).append('\n')
				.append(" int y 240\n")
				.append(" int width 8\n int height 8\n")
				.append(" String put \"").append(put).append("\"\n")
				.append(" ref attach ").append(attachTo).append('\n')
				.append(" ref wire ").append(other).append('\n')
				.append("END\n");
		}

		/** A full shifter lane: constants for data and amount, the
		 *  shifter, and a named output pin. */
		void lane(String type, int bits, long value, long amount,
				String outName) {
			int data = constant(value);
			int amt = constant(amount);
			int sr = shiftRegister(type, bits);
			int out = outputPin(outName, bits);
			wire(data, "output", sr, "input");
			wire(amt, "output", sr, "amount");
			wire(sr, "output", out, "input");
		}

		String build() {
			return text + "ENDCIRCUIT\n";
		}
	}

	private static Circuit load(String circuitText) throws Exception {
		Circuit circuit = new Circuit("shift");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		BufferedImage img =
				new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		boolean ok = circuit.finishLoad(g);
		g.dispose();
		assertTrue(ok, () -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	private static Circuit simulate(Circuit circuit) {
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(200);
		sim.runSim();
		return circuit;
	}

	private static long pinValue(Circuit circuit, String name) {
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin
					&& name.equals(((OutputPin) el).getName())) {
				BitSet value = ((OutputPin) el).getCurrentValue();
				assertNotNull(value, "output pin " + name + " never settled");
				return BitSetUtils.ToLong(value);
			}
		}
		throw new AssertionError("output pin " + name + " not found");
	}

	private static long shifted(String type, int bits, long value,
			long amount) throws Exception {
		CircuitBuilder cb = new CircuitBuilder("one");
		cb.lane(type, bits, value, amount, "out");
		return pinValue(simulate(load(cb.build())), "out");
	}

	// ------------------------------------------------------------------
	// semantics (docs/simulation-semantics.md section 6.3)
	// ------------------------------------------------------------------

	@Test
	void logicalLeftShiftsInZeroesAndDropsHighBits() throws Exception {
		// 0b10110101 << 2 = 0b(10)11010100, high bits fall off
		assertEquals(0b11010100, shifted("LogicalLeft", 8, 0b10110101, 2));
	}

	@Test
	void logicalRightShiftsInZeroes() throws Exception {
		assertEquals(0b00101101, shifted("LogicalRight", 8, 0b10110101, 2));
	}

	@Test
	void arithmeticRightCopiesTheSignBit() throws Exception {
		assertEquals(0b11101101,
				shifted("ArithmeticRight", 8, 0b10110101, 2));
	}

	@Test
	void arithmeticRightOfANonNegativeValueMatchesLogicalRight()
			throws Exception {
		assertEquals(0b00010101,
				shifted("ArithmeticRight", 8, 0b01010100, 2));
	}

	@Test
	void amountZeroIsTheIdentity() throws Exception {
		assertEquals(0b10110101, shifted("LogicalLeft", 8, 0b10110101, 0));
		assertEquals(0b10110101, shifted("LogicalRight", 8, 0b10110101, 0));
		assertEquals(0b10110101,
				shifted("ArithmeticRight", 8, 0b10110101, 0));
	}

	/**
	 * An amount at or past the data width is expressible only when the
	 * width is not a power of two (the amount input is ceil(log2 bits)
	 * wide): 5-bit lanes with amount 6 must give 0 for the logical
	 * kinds and all sign bits for the arithmetic one.
	 */
	@Test
	void amountBeyondTheWidthZeroFillsOrSignFills() throws Exception {
		assertEquals(0, shifted("LogicalLeft", 5, 0b10110, 6));
		assertEquals(0, shifted("LogicalRight", 5, 0b10110, 6));
		assertEquals(0b11111, shifted("ArithmeticRight", 5, 0b10110, 6));
		assertEquals(0, shifted("ArithmeticRight", 5, 0b01110, 6));
	}

	// ------------------------------------------------------------------
	// fork interop (issue #122 H2, prediction P3)
	// ------------------------------------------------------------------

	private static final Path FORK_FIXTURE =
			Path.of("test", "fixtures", "fork-4.6-shiftregister.jls");

	@Test
	void forkAuthoredCircuitLoadsWiresAndSimulatesEquivalently()
			throws Exception {
		String text = Files.readString(FORK_FIXTURE, StandardCharsets.UTF_8);
		Circuit circuit = load(text);

		// structure, pinned with the #91 UI-harness assertions: three
		// shifters, each with the fork's put names and widths, each
		// feeding a watched output pin
		List<ShiftRegister> shifters = new ArrayList<ShiftRegister>();
		for (Element el : circuit.getElements()) {
			if (el instanceof ShiftRegister) {
				shifters.add((ShiftRegister) el);
			}
		}
		assertEquals(3, shifters.size(),
				"the fork fixture contains three shift registers");
		for (ShiftRegister sr : shifters) {
			assertPutBits(sr, "input", 8);
			assertPutBits(sr, "amount", 3);
			assertPutBits(sr, "output", 8);
		}
		for (String pin : new String[] {"ll", "lr", "ar"}) {
			OutputPin out = jls.ui.CircuitAssert.assertElementPresent(
					circuit, OutputPin.class, pin);
			assertWatched(out, true);
			boolean fed = false;
			for (ShiftRegister sr : shifters) {
				fed |= feeds(circuit, sr, out);
			}
			assertTrue(fed, "output pin " + pin
					+ " must be fed by a shift register");
		}

		// behavior: the fork built the circuit as 181 shifted by 2 in
		// each of its three kinds
		simulate(circuit);
		assertEquals(0b11010100, pinValue(circuit, "ll"),
				"LogicalLeft lane");
		assertEquals(0b00101101, pinValue(circuit, "lr"),
				"LogicalRight lane");
		assertEquals(0b11101101, pinValue(circuit, "ar"),
				"ArithmeticRight lane");
	}

	/** True when source's output net reaches dest (assertConnected,
	 *  without failing the test when this pair isn't wired). */
	private static boolean feeds(Circuit circuit, LogicElement source,
			LogicElement dest) {
		try {
			assertConnected(circuit, source, dest);
			return true;
		} catch (AssertionError notWired) {
			return false;
		}
	}

}
