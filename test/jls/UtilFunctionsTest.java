package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * Direct coverage of jls.Util: the clipboard copy machinery (the same
 * code paste and cross-circuit copy run through in the editor, testable
 * headlessly because it only touches the model), wire-net partitioning,
 * and the small pure helpers (name validation, base conversion).
 */
class UtilFunctionsTest {

	/** A gate fed by two wired constants - elements, ends and wires. */
	private static Circuit source() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int a = cb.constant(1);
		int b = cb.constant(0);
		int gate = cb.gate("AndGate", 1, 2);
		int out = cb.outputPin("out", 1);
		cb.wire(a, "output", gate, "input0");
		cb.wire(b, "output", gate, "input1");
		cb.wire(gate, "output", out, "input");

		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void copyReproducesElementsWiresAndAttachment() throws Exception {
		Circuit from = source();
		Set<Element> selection = new HashSet<Element>(from.getElements());

		Circuit to = new Circuit("golden");
		Point min = Util.copy(selection, to);
		assertNotNull(min);

		int gates = 0, constants = 0, pins = 0, wires = 0, ends = 0;
		for (Element el : to.getElements()) {
			switch (el.getClass().getSimpleName()) {
			case "AndGate" -> gates++;
			case "Constant" -> constants++;
			case "OutputPin" -> pins++;
			case "Wire" -> wires++;
			case "WireEnd" -> ends++;
			default -> throw new AssertionError(
					"unexpected copy: " + el.getClass().getSimpleName());
			}
		}
		assertEquals(1, gates, "gate must copy");
		assertEquals(2, constants, "constants must copy");
		assertEquals(1, pins, "output pin must copy");
		assertEquals(3, wires, "all three wires must copy");
		assertEquals(6, ends, "all six wire ends must copy");

		// the copied wires must connect copied ends, not originals
		for (Element el : to.getElements()) {
			if (el instanceof Wire wire) {
				WireEnd end = wire.getEnd();
				assertTrue(to.getElements().contains(end)
						&& to.getElements().contains(wire.getOtherEnd(end)),
						"copied wire must join copied ends");
			}
		}
	}

	@Test
	void copyOfAPartialSelectionDropsDanglingWires() throws Exception {
		Circuit from = source();
		// select everything except the output pin and its attached
		// wire end (the shape a rectangle selection produces): the
		// gate-to-pin wire keeps only one copied end, so it must not
		// be copied, and the surviving lone end must be pruned
		Set<Element> selection = new HashSet<Element>();
		for (Element el : from.getElements()) {
			if (el instanceof jls.elem.OutputPin) {
				continue;
			}
			if (el instanceof WireEnd end && end.getPut() != null
					&& end.getPut().getElement()
							instanceof jls.elem.OutputPin) {
				continue;
			}
			selection.add(el);
		}
		Circuit to = new Circuit("golden");
		Util.copy(selection, to);

		int wires = 0, ends = 0;
		for (Element el : to.getElements()) {
			if (el instanceof Wire) {
				wires++;
			} else if (el instanceof WireEnd) {
				ends++;
			}
		}
		assertEquals(2, wires,
				"only the two fully-selected wires may copy");
		assertEquals(4, ends,
				"wire ends without a surviving wire must be pruned");
	}

	@Test
	void partitionRebuildsWireNets() throws Exception {
		Circuit from = source();
		Circuit to = new Circuit("golden");
		Util.copy(new HashSet<Element>(from.getElements()), to);
		// the copy has wires and ends but no nets yet; partitioning
		// must group each wire's ends into a common net without error
		Util.partition(to);
		for (Element el : to.getElements()) {
			if (el instanceof WireEnd end) {
				assertNotNull(end.getNet(),
						"every wire end must belong to a net after"
								+ " partition");
			}
		}
	}

	@Test
	void nameValidationAcceptsIdentifiersOnly() {
		assertTrue(Util.isValidName("a"));
		assertTrue(Util.isValidName("counter_4"));
		assertTrue(!Util.isValidName(""));
		assertTrue(!Util.isValidName("4bit"), "leading digit");
		assertTrue(!Util.isValidName("_x"), "leading underscore");
		assertTrue(!Util.isValidName("a b"), "space");
		assertTrue(!Util.isValidName("a-b"), "hyphen");
	}

	@Test
	void fileNameValidationStripsDirectoriesOnBothSeparators() {
		assertEquals("circ", Util.isValidFileName("circ"));
		assertEquals("circ", Util.isValidFileName("/home/user/circ"));
		assertEquals("circ", Util.isValidFileName("C:\\work\\circ"));
		assertNull(Util.isValidFileName("/home/user/4bad"));
		assertNull(Util.isValidFileName(""));
	}

	@Test
	void baseConversionMatchesItsDisplayContract() {
		assertEquals("1010", Util.convert(BigInteger.valueOf(10), 2, false));
		assertEquals("1010B", Util.convert(BigInteger.valueOf(10), 2, true));
		assertEquals("10", Util.convert(BigInteger.valueOf(10), 10, false));
		assertEquals("a", Util.convert(BigInteger.valueOf(10), 16, false));
		assertEquals("0xa", Util.convert(BigInteger.valueOf(10), 16, true));
	}
}
