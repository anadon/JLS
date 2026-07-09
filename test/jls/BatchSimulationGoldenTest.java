package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.OutputPin;
import jls.sim.BatchSimulator;

/**
 * Headless batch-simulation golden tests (issue #14). Each test builds a
 * circuit in the on-disk text format -- constants wired to a gate wired
 * to a watched output pin -- loads it through the real loader, runs the
 * real BatchSimulator, and asserts the output pin's settled value. Any
 * change to gate logic, value propagation, wiring, or the loader that
 * alters simulated behavior fails the corresponding golden.
 */
class BatchSimulationGoldenTest {

	/**
	 * Builds circuit text for: constants -> gate -> watched output pin.
	 * Wire ends are attached by element id + put name exactly as the
	 * editor saves them.
	 */
	private static final class CircuitBuilder {

		private final StringBuilder text = new StringBuilder("CIRCUIT golden\n");
		private int nextId = 0;

		int constant(long value) {
			int id = nextId++;
			text.append("ELEMENT Constant\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x 60\n int y ").append(60 + 48 * id).append('\n')
				.append(" int width 24\n int height 24\n")
				.append(" Int value ").append(value).append('\n')
				.append(" int base 10\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n");
			return id;
		}

		int gate(String type, int bits, int numInputs) {
			int id = nextId++;
			text.append("ELEMENT ").append(type).append('\n')
				.append(" int id ").append(id).append('\n')
				.append(" int x 240\n int y 120\n")
				.append(" int width 48\n int height 24\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int numInputs ").append(numInputs).append('\n')
				.append(" String orientation \"right\"\n")
				.append(" int delay 10\n")
				.append("END\n");
			return id;
		}

		int outputPin(String name, int bits) {
			int id = nextId++;
			text.append("ELEMENT OutputPin\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x 480\n int y 120\n")
				.append(" int width 48\n int height 24\n")
				.append(" String name \"").append(name).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int watch 1\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n");
			return id;
		}

		int memory(String type, int bits, int capacity, String init) {
			int id = nextId++;
			text.append("ELEMENT Memory\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x 240\n int y 240\n")
				.append(" int width 96\n int height 96\n")
				.append(" String name \"mem").append(id).append("\"\n")
				.append(" String type \"").append(type).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int cap ").append(capacity).append('\n')
				.append(" int time 10\n")
				.append(" int watch 0\n")
				.append(" String file \"\"\n")
				.append(" String init \"").append(init).append("\"\n")
				.append("END\n");
			return id;
		}

		/** Wire fromElement's put to toElement's put, as two attached wire ends. */
		void wire(int fromElement, String fromPut, int toElement, String toPut) {
			int endA = nextId++;
			int endB = nextId++;
			wireEnd(endA, fromElement, fromPut, endB);
			wireEnd(endB, toElement, toPut, endA);
		}

		private void wireEnd(int id, int attachTo, String put, int otherEnd) {
			text.append("ELEMENT WireEnd\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(12 * id).append('\n')
				.append(" int y 240\n")
				.append(" int width 8\n int height 8\n")
				.append(" String put \"").append(put).append("\"\n")
				.append(" ref attach ").append(attachTo).append('\n')
				.append(" ref wire ").append(otherEnd).append('\n')
				.append("END\n");
		}

		String build() {
			return text + "ENDCIRCUIT\n";
		}
	}

	/**
	 * Load the circuit text, run the batch simulator to quiescence, and
	 * return the settled value of the named watched output pin.
	 */
	private static long simulate(String circuitText, String pinName) throws Exception {
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();

		OutputPin pin = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin && pinName.equals(((OutputPin) el).getName())) {
				pin = (OutputPin) el;
			}
		}
		assertNotNull(pin, "output pin " + pinName + " not found");
		BitSet value = pin.getCurrentValue();
		assertNotNull(value, "output pin " + pinName + " never settled");
		return BitSetUtils.ToLong(value);
	}

	/** Golden for a two-input, single-bit gate across its whole truth table. */
	private static void assertTruthTable(String gateType, long[] expected) throws Exception {
		int row = 0;
		for (long a = 0; a <= 1; a++) {
			for (long b = 0; b <= 1; b++) {
				CircuitBuilder cb = new CircuitBuilder();
				int constA = cb.constant(a);
				int constB = cb.constant(b);
				int gate = cb.gate(gateType, 1, 2);
				int pin = cb.outputPin("out", 1);
				cb.wire(constA, "output", gate, "input0");
				cb.wire(constB, "output", gate, "input1");
				cb.wire(gate, "output", pin, "input");
				long got = simulate(cb.build(), "out");
				assertEquals(expected[row], got,
						gateType + "(" + a + "," + b + ") must be " + expected[row]);
				row++;
			}
		}
	}

	@Test
	void andGateTruthTable() throws Exception {
		assertTruthTable("AndGate", new long[] { 0, 0, 0, 1 });
	}

	@Test
	void orGateTruthTable() throws Exception {
		assertTruthTable("OrGate", new long[] { 0, 1, 1, 1 });
	}

	@Test
	void nandGateTruthTable() throws Exception {
		assertTruthTable("NandGate", new long[] { 1, 1, 1, 0 });
	}

	@Test
	void norGateTruthTable() throws Exception {
		assertTruthTable("NorGate", new long[] { 1, 0, 0, 0 });
	}

	@Test
	void xorGateTruthTable() throws Exception {
		assertTruthTable("XorGate", new long[] { 0, 1, 1, 0 });
	}

	@Test
	void notGateInverts() throws Exception {
		for (long a = 0; a <= 1; a++) {
			CircuitBuilder cb = new CircuitBuilder();
			int constA = cb.constant(a);
			int gate = cb.gate("NotGate", 1, 1);
			int pin = cb.outputPin("out", 1);
			cb.wire(constA, "output", gate, "input0");
			cb.wire(gate, "output", pin, "input");
			assertEquals(1 - a, simulate(cb.build(), "out"),
					"NOT(" + a + ")");
		}
	}

	@Test
	void delayGatePassesValueThrough() throws Exception {
		for (long a = 0; a <= 1; a++) {
			CircuitBuilder cb = new CircuitBuilder();
			int constA = cb.constant(a);
			int gate = cb.gate("DelayGate", 1, 1);
			int pin = cb.outputPin("out", 1);
			cb.wire(constA, "output", gate, "input0");
			cb.wire(gate, "output", pin, "input");
			assertEquals(a, simulate(cb.build(), "out"), "DELAY(" + a + ")");
		}
	}

	@Test
	void multiBitAndGate() throws Exception {
		// 4-bit AND: 0b1100 & 0b1010 = 0b1000
		CircuitBuilder cb = new CircuitBuilder();
		int constA = cb.constant(0b1100);
		int constB = cb.constant(0b1010);
		int gate = cb.gate("AndGate", 4, 2);
		int pin = cb.outputPin("out", 4);
		cb.wire(constA, "output", gate, "input0");
		cb.wire(constB, "output", gate, "input1");
		cb.wire(gate, "output", pin, "input");
		assertEquals(0b1000, simulate(cb.build(), "out"));
	}

	@Test
	void multiBitXorGate() throws Exception {
		// 4-bit XOR: 0b1100 ^ 0b1010 = 0b0110
		CircuitBuilder cb = new CircuitBuilder();
		int constA = cb.constant(0b1100);
		int constB = cb.constant(0b1010);
		int gate = cb.gate("XorGate", 4, 2);
		int pin = cb.outputPin("out", 4);
		cb.wire(constA, "output", gate, "input0");
		cb.wire(constB, "output", gate, "input1");
		cb.wire(gate, "output", pin, "input");
		assertEquals(0b0110, simulate(cb.build(), "out"));
	}

	@Test
	void threeInputAndGate() throws Exception {
		// only all-ones input produces 1
		for (long mask = 0; mask < 8; mask++) {
			CircuitBuilder cb = new CircuitBuilder();
			int gate = cb.gate("AndGate", 1, 3);
			int pin = cb.outputPin("out", 1);
			for (int i = 0; i < 3; i++) {
				int c = cb.constant((mask >> i) & 1);
				cb.wire(c, "output", gate, "input" + i);
			}
			cb.wire(gate, "output", pin, "input");
			assertEquals(mask == 7 ? 1 : 0, simulate(cb.build(), "out"),
					"AND3 with mask " + mask);
		}
	}

	@Test
	void romReadDeliversInitialContents() throws Exception {
		// ROM holding 5 at address 0 and 9 at address 1; CS and OE are
		// active low; read address 1 onto a watched pin
		CircuitBuilder cb = new CircuitBuilder();
		int rom = cb.memory("ROM", 8, 4, "0 5\\n1 9");
		int addr = cb.constant(1);
		int select = cb.constant(0);
		int enable = cb.constant(0);
		int pin = cb.outputPin("out", 8);
		cb.wire(addr, "output", rom, "address");
		cb.wire(select, "output", rom, "CS");
		cb.wire(enable, "output", rom, "OE");
		cb.wire(rom, "output", pin, "input");
		assertEquals(9, simulate(cb.build(), "out"));
	}

	@Test
	void ramWriteStoresTheWord() throws Exception {
		// RAM with CS low (selected), WE low (writing), OE high (not
		// reading): the driven word must land at the driven address
		CircuitBuilder cb = new CircuitBuilder();
		int ram = cb.memory("RAM", 8, 4, "");
		int addr = cb.constant(2);
		int data = cb.constant(7);
		int select = cb.constant(0);
		int write = cb.constant(0);
		int enable = cb.constant(1);
		cb.wire(addr, "output", ram, "address");
		cb.wire(data, "output", ram, "input");
		cb.wire(select, "output", ram, "CS");
		cb.wire(write, "output", ram, "WE");
		cb.wire(enable, "output", ram, "OE");

		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();

		jls.elem.Memory memory = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof jls.elem.Memory) {
				memory = (jls.elem.Memory) el;
			}
		}
		assertNotNull(memory);
		BitSet stored = memory.getCurrentValue(2);
		assertNotNull(stored, "the write must have stored a word at address 2");
		assertEquals(7, BitSetUtils.ToLong(stored));
	}

	@Test
	void watchedElementsPrintInNameOrder() throws Exception {
		// Two watched output pins at the same circuit level, declared in
		// anti-alphabetical order. Circuit.elements is a HashSet, so
		// before the issue #72 fix displayResults printed them in hash
		// order; the batch output contract (docs/batch-interface.md)
		// pins name order, byte for byte.
		CircuitBuilder cb = new CircuitBuilder();
		int one = cb.constant(1);
		int zero = cb.constant(0);
		int pinZ = cb.outputPin("zz", 1);
		int pinA = cb.outputPin("aa", 1);
		cb.wire(one, "output", pinZ, "input");
		cb.wire(zero, "output", pinA, "input");

		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();

		PrintStream saved = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			JLSStart.displayResults(circuit, "");
		} finally {
			System.setOut(saved);
		}
		assertEquals("Output Pin aa: 0x0 (0 unsigned, 0 signed)\n"
				+ "Output Pin zz: 0x1 (1 unsigned, -1 signed)\n",
				captured.toString(StandardCharsets.UTF_8),
				"watched elements must print in name order, byte-exactly");
	}

	@Test
	void chainedGatesPropagate() throws Exception {
		// (1 AND 1) XOR 0 = 1, through two gate stages
		CircuitBuilder cb = new CircuitBuilder();
		int c1 = cb.constant(1);
		int c2 = cb.constant(1);
		int c3 = cb.constant(0);
		int and = cb.gate("AndGate", 1, 2);
		int xor = cb.gate("XorGate", 1, 2);
		int pin = cb.outputPin("out", 1);
		cb.wire(c1, "output", and, "input0");
		cb.wire(c2, "output", and, "input1");
		cb.wire(and, "output", xor, "input0");
		cb.wire(c3, "output", xor, "input1");
		cb.wire(xor, "output", pin, "input");
		assertEquals(1, simulate(cb.build(), "out"));
	}
}
