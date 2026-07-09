package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.OutputPin;
import jls.sim.BatchSimulator;

/**
 * Headless batch-simulation goldens for the sequential elements (issue
 * #14): Register (flip-flop and latch) and StateMachine. Together with
 * BatchSimulationGoldenTest's combinational goldens these pin the
 * clocked half of the simulator: edge triggering, initial values, and
 * state-machine outputs.
 */
class SequentialGoldenTest {

	private static final int CLOCK_CYCLE = 20;

	/** Circuit text builder in the on-disk format (see #14 harness). */
	private static final class CircuitBuilder {

		private final StringBuilder text = new StringBuilder("CIRCUIT seqgolden\n");
		private int nextId = 0;

		int constant(long value) {
			int id = nextId++;
			text.append("ELEMENT Constant\n int id ").append(id)
				.append("\n int x 60\n int y ").append(60 + 48 * id)
				.append("\n int width 24\n int height 24\n Int value ").append(value)
				.append("\n int base 10\n String orient \"RIGHT\"\nEND\n");
			return id;
		}

		int clock() {
			int id = nextId++;
			text.append("ELEMENT Clock\n int id ").append(id)
				.append("\n int x 60\n int y ").append(60 + 48 * id)
				.append("\n int width 24\n int height 24\n int cycle ").append(CLOCK_CYCLE)
				.append("\n int one ").append(CLOCK_CYCLE / 2)
				.append("\n String orient \"RIGHT\"\nEND\n");
			return id;
		}

		int inputPin(String name, int bits) {
			int id = nextId++;
			text.append("ELEMENT InputPin\n int id ").append(id)
				.append("\n int x 60\n int y ").append(60 + 48 * id)
				.append("\n int width 48\n int height 24\n String name \"")
				.append(name).append("\"\n int bits ").append(bits)
				.append("\n int watch 0\n String orient \"RIGHT\"\nEND\n");
			return id;
		}

		int register(int bits, long init, String type) {
			int id = nextId++;
			text.append("ELEMENT Register\n int id ").append(id)
				.append("\n int x 240\n int y 120\n int width 60\n int height 60\n")
				.append(" String name \"reg").append(id).append("\"\n int bits ").append(bits)
				.append("\n Int init ").append(init)
				.append("\n String orient \"RIGHT\"\n int delay 5\n String type \"")
				.append(type).append("\"\n int watch 0\nEND\n");
			return id;
		}

		/**
		 * A one-state machine: initial state S drives output signal z
		 * with the given value and loops to itself unconditionally.
		 */
		int stateMachine(long zValue, int zBits) {
			int id = nextId++;
			text.append("ELEMENT StateMachine\n int id ").append(id)
				.append("\n int x 240\n int y 360\n int width 96\n int height 96\n")
				.append(" String name \"sm").append(id).append("\"\n int delay 5\n int trig 0\n")
				.append(" String state \"S\"\n")
				.append("  int x 40\n  int y 40\n  int diameter 40\n  int init 1\n")
				.append("  String output \"z\"\n   long value ").append(zValue)
				.append("\n   int bits ").append(zBits).append('\n')
				.append("  String trans \"always\"\n   String next \"S\"\n")
				.append("END\n");
			return id;
		}

		/**
		 * A three-state machine (issue #56: more than one self-loop
		 * state): S0 -> S1 -> S2 -> S0, advancing on a rising clock edge
		 * while input "en" is 1 and holding the current state via an
		 * "else" self-loop while en is 0, with a distinct 2-bit output
		 * per state: S0 drives z=3, S1 drives z=1, S2 drives z=2 (S0's
		 * value is deliberately nonzero so "never left the initial
		 * state" and "output never driven" are distinguishable).
		 */
		int threeStateMachine() {
			int id = nextId++;
			text.append("ELEMENT StateMachine\n int id ").append(id)
				.append("\n int x 240\n int y 360\n int width 96\n int height 96\n")
				.append(" String name \"sm").append(id)
				.append("\"\n int delay 5\n int trig 1\n");
			appendState("S0", 3, "S1", 40);
			appendState("S1", 1, "S2", 140);
			appendState("S2", 2, "S0", 240);
			text.append("END\n");
			return id;
		}

		/** One state of the three-state machine (S0 is initial). */
		private void appendState(String name, long zValue, String next, int x) {
			text.append(" String state \"").append(name).append("\"\n")
				.append("  int x ").append(x).append("\n  int y 40\n")
				.append("  int diameter 40\n  int init ")
				.append(name.equals("S0") ? 1 : 0).append('\n')
				.append("  String output \"z\"\n   long value ").append(zValue)
				.append("\n   int bits 2\n")
				.append("  String trans \"en\"\n   int eq 0\n   int value 1\n")
				.append("   int bits 1\n   String next \"").append(next)
				.append("\"\n")
				.append("  String trans \"else\"\n   String next \"")
				.append(name).append("\"\n");
		}

		int outputPin(String name, int bits) {
			int id = nextId++;
			text.append("ELEMENT OutputPin\n int id ").append(id)
				.append("\n int x 480\n int y ").append(60 + 48 * id)
				.append("\n int width 48\n int height 24\n String name \"").append(name)
				.append("\"\n int bits ").append(bits)
				.append("\n int watch 1\n String orient \"RIGHT\"\nEND\n");
			return id;
		}

		void wire(int fromElement, String fromPut, int toElement, String toPut) {
			int endA = nextId++;
			int endB = nextId++;
			wireEnd(endA, fromElement, fromPut, endB);
			wireEnd(endB, toElement, toPut, endA);
		}

		private void wireEnd(int id, int attachTo, String put, int otherEnd) {
			text.append("ELEMENT WireEnd\n int id ").append(id)
				.append("\n int x ").append(12 * id)
				.append("\n int y 600\n int width 8\n int height 8\n String put \"")
				.append(put).append("\"\n ref attach ").append(attachTo)
				.append("\n ref wire ").append(otherEnd).append("\nEND\n");
		}

		String build() {
			return text + "ENDCIRCUIT\n";
		}
	}

	private static long simulate(String circuitText, String pinName, long timeLimit)
			throws Exception {
		Circuit circuit = new Circuit("seqgolden");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(timeLimit);
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

	private static long simulateWithVectors(String circuitText, String vectors,
			String pinName, long timeLimit) throws Exception {
		Circuit circuit = new Circuit("seqgolden");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		java.nio.file.Path vectorFile =
				java.nio.file.Files.createTempFile("seqgolden", ".txt");
		java.nio.file.Files.writeString(vectorFile, vectors);
		try {
			BatchSimulator sim = new BatchSimulator();
			sim.setCircuit(circuit);
			sim.setTimeLimit(timeLimit);
			sim.setTestFile(vectorFile.toString());
			sim.addTestGen();
			sim.runSim();
		} finally {
			java.nio.file.Files.deleteIfExists(vectorFile);
		}

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

	/**
	 * The one scenario that tells a flip-flop from a transparent latch
	 * (issue #56): a single rising edge captures 5, then the data input
	 * changes to 10 while the clock stays high. An edge-triggered
	 * register must keep 5; a latch follows the input to 10.
	 */
	@Test
	void flipFlopIgnoresDataChangesBetweenEdges() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int data = cb.inputPin("data", 4);
		int clk = cb.inputPin("clk", 1);
		int reg = cb.register(4, 0, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		String vectors = "clk 0 until 10 1 end\n"
				+ "data 5 until 30 10 end\n";
		assertEquals(5, simulateWithVectors(cb.build(), vectors, "q", 100),
				"an edge-triggered register must hold the value captured at"
						+ " the edge when data changes between edges");
	}

	@Test
	void latchFollowsDataChangesWhileClockHigh() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int data = cb.inputPin("data", 4);
		int clk = cb.inputPin("clk", 1);
		int reg = cb.register(4, 0, "latch");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		String vectors = "clk 0 until 10 1 end\n"
				+ "data 5 until 30 10 end\n";
		assertEquals(10, simulateWithVectors(cb.build(), vectors, "q", 100),
				"a transparent latch must follow data while the clock is high"
						+ " - this is what discriminates it from the"
						+ " flip-flop golden above");
	}

	@Test
	void positiveEdgeFlipFlopCapturesDataOnClockEdge() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int data = cb.constant(5);
		int clk = cb.clock();
		int reg = cb.register(4, 0, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		assertEquals(5, simulate(cb.build(), "q", 10 * CLOCK_CYCLE),
				"positive-edge flip-flop must capture the driven data");
	}

	@Test
	void flipFlopComplementOutputTracksQ() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int data = cb.constant(5);
		int clk = cb.clock();
		int reg = cb.register(4, 0, "nff");
		int nq = cb.outputPin("nq", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "notQ", nq, "input");
		assertEquals(10, simulate(cb.build(), "nq", 10 * CLOCK_CYCLE),
				"notQ must be the bitwise complement of the captured data");
	}

	@Test
	void registerInitialValueAppearsBeforeAnyClockEdge() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int data = cb.constant(9);
		int clk = cb.constant(0); // clock held low: no edge ever fires
		int reg = cb.register(4, 3, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		assertEquals(3, simulate(cb.build(), "q", 10 * CLOCK_CYCLE),
				"with no clock edge the register must hold its initial value");
	}

	@Test
	void latchPassesDataWhileClockHigh() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int data = cb.constant(6);
		int clk = cb.constant(1); // clock held high: latch is transparent
		int reg = cb.register(4, 0, "latch");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		assertEquals(6, simulate(cb.build(), "q", 10 * CLOCK_CYCLE),
				"a transparent latch must pass the driven data");
	}

	/** Build the three-state machine with vector-driven clk and en pins. */
	private static String threeStateCircuit() {
		CircuitBuilder cb = new CircuitBuilder();
		int clk = cb.inputPin("clk", 1);
		int en = cb.inputPin("en", 1);
		int sm = cb.threeStateMachine();
		int z = cb.outputPin("z", 2);
		cb.wire(clk, "output", sm, "clock");
		cb.wire(en, "output", sm, "en");
		cb.wire(sm, "z", z, "input");
		return cb.build();
	}

	/** Rising edges at t = 10, 30, 50, then the clock stays high. */
	private static final String THREE_EDGES =
			"clk 0 until 10 1 until 20 0 until 30 1 until 40 0 until 50 1 end\n";

	@Test
	void multiStateMachineDrivesInitialStateOutputBeforeAnyEdge()
			throws Exception {
		String vectors = "clk 0 end\nen 1 end\n";
		assertEquals(3, simulateWithVectors(threeStateCircuit(), vectors, "z", 100),
				"before any clock edge the machine must drive the initial"
						+ " state's output");
	}

	@Test
	void multiStateMachineAdvancesOneStatePerRisingEdge() throws Exception {
		String oneEdge = "clk 0 until 10 1 end\nen 1 end\n";
		assertEquals(1, simulateWithVectors(threeStateCircuit(), oneEdge, "z", 100),
				"one rising edge must move S0 -> S1");
		String twoEdges = "clk 0 until 10 1 until 20 0 until 30 1 end\nen 1 end\n";
		assertEquals(2, simulateWithVectors(threeStateCircuit(), twoEdges, "z", 100),
				"two rising edges must move S0 -> S1 -> S2");
	}

	@Test
	void multiStateMachineWrapsToInitialStateAfterFullCycle() throws Exception {
		String vectors = THREE_EDGES + "en 1 end\n";
		assertEquals(3, simulateWithVectors(threeStateCircuit(), vectors, "z", 100),
				"three rising edges must walk the full cycle back to S0"
						+ " (whose output is distinct from the undriven value)");
	}

	@Test
	void multiStateMachineHoldsStateWhileConditionIsFalse() throws Exception {
		// en is 0 at the first edge (t=10): the else self-loop holds S0;
		// en is 1 at the second (t=30): advance to S1; en is 0 again at
		// the third (t=50): hold S1. The input changes between edges,
		// so a machine sampling anywhere but the edge fails this.
		String vectors = THREE_EDGES + "en 0 until 25 1 until 45 0 end\n";
		assertEquals(1, simulateWithVectors(threeStateCircuit(), vectors, "z", 100),
				"the else self-loop must hold the state while en is 0 and"
						+ " the conditional transition must fire only on"
						+ " edges where en is 1");
	}

	@Test
	void stateMachineDrivesStateOutput() throws Exception {
		CircuitBuilder cb = new CircuitBuilder();
		int clk = cb.clock();
		int sm = cb.stateMachine(1, 1);
		int z = cb.outputPin("z", 1);
		cb.wire(clk, "output", sm, "clock");
		cb.wire(sm, "z", z, "input");
		assertEquals(1, simulate(cb.build(), "z", 10 * CLOCK_CYCLE),
				"the single-state machine must drive its state output");
	}
}
