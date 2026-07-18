package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Builds circuits in the on-disk text format for the HDL export tests
 * (issue #60), the same technique as BatchSimulationGoldenTest: text
 * exactly as the editor saves it, loaded through the real loader so
 * wire nets are the real thing. Element ids are caller-visible so
 * wires can reference puts by (id, put name).
 */
final class HdlCircuitBuilder {

	private final StringBuilder text;
	private int nextId = 0;

	HdlCircuitBuilder(String circuitName) {
		text = new StringBuilder("CIRCUIT " + circuitName + "\n");
	}

	private int element(String type, String attrs) {
		int id = nextId++;
		text.append("ELEMENT ").append(type).append('\n')
			.append(" int id ").append(id).append('\n')
			.append(" int x ").append(60 + 12 * id).append('\n')
			.append(" int y 60\n")
			.append(" int width 24\n int height 24\n")
			.append(attrs)
			.append("END\n");
		return id;
	}

	int inputPin(String name, int bits) {
		return element("InputPin", " String name \"" + name + "\"\n int bits "
				+ bits + "\n int watch 0\n String orient \"RIGHT\"\n");
	}

	int outputPin(String name, int bits) {
		return element("OutputPin", " String name \"" + name + "\"\n int bits "
				+ bits + "\n int watch 0\n String orient \"RIGHT\"\n");
	}

	int constant(long value) {
		return element("Constant", " Int value " + value
				+ "\n int base 10\n String orient \"RIGHT\"\n");
	}

	int gate(String type, int bits, int numInputs) {
		return element(type, " int bits " + bits + "\n int numInputs "
				+ numInputs + "\n String orientation \"right\"\n int delay 10\n");
	}

	int extend(int bits) {
		return element("Extend", " int bits " + bits
				+ "\n int numInputs 1\n String orientation \"right\"\n int delay 0\n");
	}

	int triState(int bits) {
		return element("TriState", " int bits " + bits
				+ "\n int delay 5\n String Gorient \"RIGHT\"\n String Corient \"DOWN\"\n");
	}

	int adder(int bits) {
		return element("Adder", " int bits " + bits
				+ "\n int delay 30\n String orient \"RIGHT\"\n");
	}

	int register(String name, int bits, long init, String type) {
		return element("Register", " String name \"" + name + "\"\n int bits "
				+ bits + "\n Int init " + init
				+ "\n String orient \"RIGHT\"\n int delay 50\n String type \""
				+ type + "\"\n int watch 0\n");
	}

	int clock(int cycle, int one) {
		return element("Clock", " int cycle " + cycle + "\n int one " + one
				+ "\n String orient \"RIGHT\"\n");
	}

	int jumpStart(String name, int bits) {
		return element("JumpStart", " String name \"" + name + "\"\n int bits "
				+ bits + "\n int watch 0\n String orientation \"LEFT\"\n");
	}

	int jumpEnd(String name, int bits) {
		return element("JumpEnd", " String name \"" + name + "\"\n int bits "
				+ bits + "\n String orientation \"RIGHT\"\n");
	}

	/** A bundler: one pair line per (input index, bundle bit). */
	int binder(int bits, int[][] ranges) {
		return element("Binder", " int bits " + bits
				+ "\n String orient \"RIGHT\"\n String noncontig \"true\"\n"
				+ " int tristate 0\n" + pairs(ranges));
	}

	/** An unbundler: one pair line per (output index, bundle bit). */
	int splitter(int bits, int[][] ranges) {
		return element("Splitter", " int bits " + bits
				+ "\n String orient \"RIGHT\"\n String noncontig \"true\"\n"
				+ " int tristate 0\n" + pairs(ranges));
	}

	private static String pairs(int[][] ranges) {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < ranges.length; r += 1) {
			for (int bit : ranges[r]) {
				sb.append(" pair ").append(r).append(' ').append(bit)
						.append('\n');
			}
		}
		return sb.toString();
	}

	/** Puts: "select", "input0".."inputN-1" (bits wide), "output". */
	int mux(int numInputs, int bits) {
		return element("Mux", " int inputs " + numInputs + "\n int bits "
				+ bits + "\n int delay 25\n String iOrient \"RIGHT\"\n"
				+ " String sOrient \"DOWN\"\n");
	}

	/** Puts: "input" (bits wide), "output" (2^bits wide). */
	int decoder(int bits) {
		return element("Decoder", " int bits " + bits
				+ "\n int delay 15\n String orient \"LEFT\"\n");
	}

	int display(int bits) {
		return element("Display", " int bits " + bits + "\n int base 10\n");
	}

	/** A signal generator; it has no puts (it drives pins by name). */
	int sigGen() {
		return element("SigGen", " String signals \"\"\n");
	}

	int stop() {
		return element("Stop", "");
	}

	int memory(String name, int bits, int capacity) {
		return element("Memory", " String name \"" + name + "\"\n String type"
				+ " \"ROM\"\n int bits " + bits + "\n int cap " + capacity
				+ "\n int time 10\n int watch 0\n String file \"\"\n"
				+ " String init \"\"\n");
	}

	/** A subcircuit wrapping a trivial nested circuit. */
	int subCircuit(String name) {
		int id = nextId++;
		text.append("ELEMENT SubCircuit\n")
			.append(" String orient \"RIGHT\"\n")
			.append(" int id ").append(id).append('\n')
			.append(" int x ").append(60 + 12 * id).append('\n')
			.append(" int y 60\n int width 48\n int height 48\n")
			.append(" String name \"").append(name).append("\"\n")
			.append("CIRCUIT inner\n")
			.append("ELEMENT InputPin\n")
			.append(" int id 0\n int x 60\n int y 60\n")
			.append(" int width 48\n int height 24\n")
			.append(" String name \"a\"\n int bits 1\n int watch 0\n")
			.append(" String orient \"RIGHT\"\n")
			.append("END\n")
			.append("ENDCIRCUIT\n")
			.append("END\n");
		return id;
	}

	/** Wire fromElement's put to toElement's put (two attached ends). */
	void wire(int fromElement, String fromPut, int toElement, String toPut) {
		int endA = nextId++;
		int endB = nextId++;
		wireEnd(endA, fromElement, fromPut, endB);
		wireEnd(endB, toElement, toPut, endA);
	}

	/** A three-ended net: one source put fanning out to two sink puts. */
	void wire3(int fromElement, String fromPut, int toElement1, String toPut1,
			int toElement2, String toPut2) {
		int endA = nextId++;
		int endB = nextId++;
		int endC = nextId++;
		wireEnd(endA, fromElement, fromPut, endB, endC);
		wireEnd(endB, toElement1, toPut1, endA);
		wireEnd(endC, toElement2, toPut2, endA);
	}

	private void wireEnd(int id, int attachTo, String put, int... others) {
		text.append("ELEMENT WireEnd\n")
			.append(" int id ").append(id).append('\n')
			.append(" int x ").append(12 * id).append('\n')
			.append(" int y 240\n")
			.append(" int width 8\n int height 8\n")
			.append(" String put \"").append(put).append("\"\n")
			.append(" ref attach ").append(attachTo).append('\n');
		for (int other : others) {
			text.append(" ref wire ").append(other).append('\n');
		}
		text.append("END\n");
	}

	String buildText() {
		return text + "ENDCIRCUIT\n";
	}

	/** Load through the real loader; wire nets are built by finishLoad. */
	Circuit load() throws Exception {
		Circuit circuit = new Circuit(circuitName());
		assertTrue(circuit.load(new Scanner(buildText())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	private String circuitName() {
		String first = text.substring("CIRCUIT ".length(),
				text.indexOf("\n"));
		return first;
	}

} // end of HdlCircuitBuilder class
