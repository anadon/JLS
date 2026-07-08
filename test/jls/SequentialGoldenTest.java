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
