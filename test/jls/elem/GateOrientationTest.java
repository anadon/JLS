package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Pins gate orientation behavior across the issue #78 H3 unification:
 * gates now share the one {@link JLSInfo.Orientation} enum instead of
 * their private lowercase duplicate, and these tests hold the two
 * contracts that must not move.
 *
 * 1. Persistence stays byte-identical: gates have always saved their
 *    orientation as a lowercase name ({@code String orientation "up"}),
 *    unlike the shared uppercase {@code orient} attribute, and every
 *    historical file must keep loading and re-saving unchanged for all
 *    four values (the all-element round-trip fixture only exercises
 *    three of them).
 *
 * 2. Rotation and flip semantics are unchanged now that the hand-rolled
 *    transition tables are {@link JLSInfo.Orientation#ccw()},
 *    {@link JLSInfo.Orientation#cw()}, and
 *    {@link JLSInfo.Orientation#flipped()}: rotating LEFT is a
 *    quarter-turn counterclockwise, RIGHT clockwise, flip reverses.
 */
class GateOrientationTest {

	/**
	 * Load a one-AndGate circuit whose gate has the given saved
	 * orientation string.
	 *
	 * @param orientation The saved lowercase orientation name.
	 *
	 * @return the loaded, finished gate.
	 *
	 * @throws Exception if finishing the load fails unexpectedly.
	 */
	private static Gate loadGate(String orientation) throws Exception {

		Circuit circuit = new Circuit("gateorient");
		String file = "CIRCUIT gateorient\nELEMENT AndGate\n int id 0\n"
				+ " int x 120\n int y 120\n int bits 1\n int numInputs 2\n"
				+ " String orientation \"" + orientation + "\"\n"
				+ " int delay 10\nEND\nENDCIRCUIT\n";
		assertTrue(circuit.load(new Scanner(file)),
				() -> "load failed: " + JLSInfo.loadError);
		BufferedImage img = new BufferedImage(64, 64,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		boolean ok = circuit.finishLoad(g);
		g.dispose();
		assertTrue(ok, () -> "finishLoad failed: " + JLSInfo.loadError);
		Gate gate = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof Gate) {
				gate = (Gate) el;
			}
		}
		assertNotNull(gate, "fixture must contain the AndGate");
		return gate;
	} // end of loadGate method

	/**
	 * The lowercase orientation name a gate's save output contains.
	 *
	 * @param gate The gate to save.
	 *
	 * @return the value of the {@code String orientation} save line.
	 */
	private static String savedOrientation(Gate gate) {

		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			gate.save(writer);
		}
		for (String line : out.toString().split("\n")) {
			if (line.contains("String orientation")) {
				int open = line.indexOf('"');
				int close = line.lastIndexOf('"');
				return line.substring(open + 1, close);
			}
		}
		return null;
	} // end of savedOrientation method

	@Test
	void everyLowercaseOrientationLoadsAndResavesIdentically()
			throws Exception {

		for (String o : new String[] {"up", "down", "left", "right"}) {
			assertEquals(o, savedOrientation(loadGate(o)),
					"gate orientation '" + o + "' must persist "
							+ "byte-identically through load and save");
		}
	}

	@Test
	void rotatingLeftIsAFullCounterclockwiseCycle() throws Exception {

		Gate gate = loadGate("up");
		BufferedImage img = new BufferedImage(64, 64,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		String[] cycle = {"left", "down", "right", "up"};
		for (String expected : cycle) {
			gate.rotate(JLSInfo.Orientation.LEFT, g);
			assertEquals(expected, savedOrientation(gate),
					"rotate LEFT must be a quarter-turn "
							+ "counterclockwise");
		}
		g.dispose();
	}

	@Test
	void rotatingRightIsAFullClockwiseCycle() throws Exception {

		Gate gate = loadGate("up");
		BufferedImage img = new BufferedImage(64, 64,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		String[] cycle = {"right", "down", "left", "up"};
		for (String expected : cycle) {
			gate.rotate(JLSInfo.Orientation.RIGHT, g);
			assertEquals(expected, savedOrientation(gate),
					"rotate RIGHT must be a quarter-turn clockwise");
		}
		g.dispose();
	}

	@Test
	void flipReversesEveryOrientation() throws Exception {

		BufferedImage img = new BufferedImage(64, 64,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		String[][] pairs = {{"left", "right"}, {"right", "left"},
				{"up", "down"}, {"down", "up"}};
		for (String[] pair : pairs) {
			Gate gate = loadGate(pair[0]);
			gate.flip(g);
			assertEquals(pair[1], savedOrientation(gate),
					"flip must reverse '" + pair[0] + "'");
		}
		g.dispose();
	}

} // end of GateOrientationTest class
