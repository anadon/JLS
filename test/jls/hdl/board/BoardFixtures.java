package jls.hdl.board;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import jls.Circuit;
import jls.JLSInfo;

/**
 * The shared circuit fixture for the board-export tests (issue #213):
 * "blinky", a 2-bit registered pass-through with every port kind the
 * exporter produces — a multi-bit input pin ({@code sw}), a Clock
 * element (which exports as the input port {@code clk}), and a
 * multi-bit output pin ({@code led}). Built in the on-disk text format
 * and loaded through the real loader, the same technique as
 * {@code jls.hdl.HdlCircuitBuilder} (which is package-private to
 * {@code jls.hdl} and therefore mirrored, not reused, here).
 */
final class BoardFixtures {

	/** Not instantiable; all fixtures are static. */
	private BoardFixtures() {
	} // not instantiable

	/**
	 * The blinky circuit in on-disk text form: InputPin "sw" (2 bits)
	 * feeding Register "r" (positive-edge, clocked by a Clock element),
	 * whose Q output drives OutputPin "led" (2 bits). Module ports:
	 * {@code sw[1:0]} in, {@code clk} in, {@code led[1:0]} out.
	 *
	 * @return the circuit file text.
	 */
	static String blinkyText() {

		return "CIRCUIT blinky\n"
				// sw: 2-bit input pin (id 0)
				+ "ELEMENT InputPin\n"
				+ " int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " String name \"sw\"\n int bits 2\n int watch 0\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				// the clock (id 1); exports as input port "clk"
				+ "ELEMENT Clock\n"
				+ " int id 1\n int x 72\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " int cycle 20\n int one 10\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				// r: 2-bit positive-edge register (id 2)
				+ "ELEMENT Register\n"
				+ " int id 2\n int x 84\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " String name \"r\"\n int bits 2\n Int init 0\n"
				+ " String orient \"RIGHT\"\n int delay 50\n"
				+ " String type \"pff\"\n int watch 0\n"
				+ "END\n"
				// led: 2-bit output pin (id 3)
				+ "ELEMENT OutputPin\n"
				+ " int id 3\n int x 96\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " String name \"led\"\n int bits 2\n int watch 0\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				// sw.output -> r.D
				+ wireEnd(4, 0, "output", 5)
				+ wireEnd(5, 2, "D", 4)
				// clock.output -> r.C
				+ wireEnd(6, 1, "output", 7)
				+ wireEnd(7, 2, "C", 6)
				// r.Q -> led.input
				+ wireEnd(8, 2, "Q", 9)
				+ wireEnd(9, 3, "input", 8)
				+ "ENDCIRCUIT\n";
	} // end of blinkyText method

	/**
	 * One wire end in on-disk text form, attached to an element put and
	 * joined to its partner end.
	 *
	 * @param id The wire end's element id.
	 * @param attachTo The id of the element the end attaches to.
	 * @param put The name of the put on that element.
	 * @param other The id of the partner wire end.
	 *
	 * @return the element text.
	 */
	private static String wireEnd(int id, int attachTo, String put,
			int other) {

		return "ELEMENT WireEnd\n"
				+ " int id " + id + "\n"
				+ " int x " + 12 * id + "\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"" + put + "\"\n"
				+ " ref attach " + attachTo + "\n"
				+ " ref wire " + other + "\n"
				+ "END\n";
	} // end of wireEnd method

	/**
	 * The blinky circuit, loaded through the real loader so wire nets
	 * are the real thing.
	 *
	 * @return the loaded, fully assembled circuit.
	 *
	 * @throws Exception if loading fails (a test bug, not a product one).
	 */
	static Circuit blinky() throws Exception {

		Circuit circuit = new Circuit("blinky");
		assertTrue(circuit.load(new Scanner(blinkyText())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	} // end of blinky method

} // end of BoardFixtures class
