package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.OutputPin;

/**
 * Direct coverage of Circuit's subcircuit lookup, which the -circuit
 * option of -i image export uses: by name, by path of names, and the
 * list of direct subcircuit names. Circuits are loaded in-process from the
 * on-disk text format; each level's output pin name identifies it.
 */
class CircuitSubcircuitLookupTest {

	/**
	 * The body of a one-level circuit (a constant wired to an output
	 * pin named pin), without its ENDCIRCUIT line, so a caller can
	 * append SubCircuit elements before closing it.
	 */
	private static String levelBody(String name, String pin) {
		return "CIRCUIT " + name + "\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " Int value 1\n int base 10\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT OutputPin\n"
				+ " int id 1\n int x 480\n int y 120\n"
				+ " int width 48\n int height 24\n"
				+ " String name \"" + pin + "\"\n int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n"
				+ " int id 2\n int x 24\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"output\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n"
				+ " int id 3\n int x 36\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"input\"\n ref attach 1\n ref wire 2\n"
				+ "END\n";
	}

	/** A SubCircuit element wrapping the (unclosed) circuit body. */
	private static String subCircuit(int id, String body) {
		return subCircuit(id, "", body);
	}

	/**
	 * A SubCircuit element with an explicit stable id line (sid, e.g.
	 * "t:1"), or none if sid is empty, wrapping the circuit body.
	 */
	private static String subCircuit(int id, String sid, String body) {
		return "ELEMENT SubCircuit\n"
				+ " String orient \"RIGHT\"\n"
				+ " int id " + id + "\n int x 300\n int y 300\n"
				+ " int width 60\n int height 40\n"
				+ (sid.isEmpty() ? "" : " String sid \"" + sid + "\"\n")
				+ body
				+ "ENDCIRCUIT\n"
				+ "END\n";
	}

	private static Circuit load(String text) throws Exception {
		final Circuit circuit = new Circuit("top");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * top (pin "out") holds alu (pin "r") and mem (pin "m"); alu holds
	 * inner (pin "q").
	 */
	private static Circuit nested() throws Exception {
		final String alu = levelBody("alu", "r")
				+ subCircuit(10, levelBody("inner", "q"));
		return load(levelBody("top", "out")
				+ subCircuit(20, alu)
				+ subCircuit(21, levelBody("mem", "m"))
				+ "ENDCIRCUIT\n");
	}

	/** The name of the circuit's (only) output pin. */
	private static String pinOf(Circuit circuit) {
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin pin) {
				return pin.getName();
			}
		}
		throw new AssertionError("no output pin in " + circuit.getName());
	}

	@Test
	void findsADirectSubcircuitByName() throws Exception {
		final Circuit top = nested();
		final Circuit alu = top.getSubCircuit("alu");
		assertNotNull(alu);
		assertEquals("r", pinOf(alu));
		final Circuit mem = top.getSubCircuit("mem");
		assertNotNull(mem);
		assertEquals("m", pinOf(mem));
		assertNull(top.getSubCircuit("inner"),
				"inner is only inside alu, not directly inside top");
		assertNull(top.getSubCircuit("nope"));
	}

	@Test
	void followsAPathIntoNestedSubcircuits() throws Exception {
		final Circuit top = nested();
		assertSame(top, top.getSubCircuit(List.of()),
				"the empty path is the circuit itself");
		final Circuit inner = top.getSubCircuit(List.of("alu", "inner"));
		assertNotNull(inner);
		assertEquals("q", pinOf(inner));
		assertSame(top.getSubCircuit("alu"), top.getSubCircuit(List.of("alu")));
	}

	@Test
	void aPathWithAMissingStepFindsNothing() throws Exception {
		final Circuit top = nested();
		assertNull(top.getSubCircuit(List.of("nope")));
		assertNull(top.getSubCircuit(List.of("alu", "nope")));
		assertNull(top.getSubCircuit(List.of("nope", "inner")));
		assertNull(top.getSubCircuit(List.of("alu", "inner", "nope")));
	}

	@Test
	void namesTheDirectSubcircuitsOnly() throws Exception {
		final Circuit top = nested();
		assertEquals(List.of("alu", "mem"), top.getSubCircuitNames());
		final Circuit alu = top.getSubCircuit("alu");
		assertNotNull(alu);
		assertEquals(List.of("inner"), alu.getSubCircuitNames());
		final Circuit inner = alu.getSubCircuit("inner");
		assertNotNull(inner);
		assertEquals(List.of(), inner.getSubCircuitNames());
		final List<String> names = top.getSubCircuitNames();
		assertThrows(UnsupportedOperationException.class,
				() -> names.add("x"), "the name list must be unmodifiable");
	}

	@Test
	void aRepeatedNameResolvesToTheFirstInStableOrder() throws Exception {
		// only a hand-edited file can repeat a name; the element with
		// the lower stable id wins even though it comes second in the
		// file, so the result follows stable-id order, not file or hash
		// order
		final Circuit top = load(levelBody("top", "out")
				+ subCircuit(10, "t:2", levelBody("dup", "later"))
				+ subCircuit(11, "t:1", levelBody("dup", "earlier"))
				+ "ENDCIRCUIT\n");
		final Circuit dup = top.getSubCircuit("dup");
		assertNotNull(dup);
		assertEquals("earlier", pinOf(dup));
		assertEquals(List.of("dup", "dup"), top.getSubCircuitNames());
	}
}
