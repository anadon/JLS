package jls;

/**
 * Builds circuit text in the on-disk save format, for behavioral tests
 * that load through the real loader and run the real simulator. This is
 * the shared extraction of the private CircuitBuilder inside
 * BatchSimulationGoldenTest (issue #158 method step 1); new golden
 * suites build on this one, and the older in-test builders can migrate
 * here as they are touched.
 *
 * Wire ends are attached by element id + put name exactly as the editor
 * saves them. Every factory returns the new element's id for wiring.
 */
public final class CircuitTextBuilder {

	private final StringBuilder text = new StringBuilder("CIRCUIT golden\n");
	private int nextId = 0;

	/** Start an ELEMENT block with the shared position header. */
	private StringBuilder open(String type, int id) {
		return text.append("ELEMENT ").append(type).append('\n')
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(60 + 24 * id).append('\n')
				.append(" int y ").append(60 + 12 * (id % 8)).append('\n')
				.append(" int width 24\n int height 24\n");
	}

	public int constant(long value) {
		int id = nextId++;
		open("Constant", id)
				.append(" Int value ").append(value).append('\n')
				.append(" int base 10\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n");
		return id;
	}

	public int gate(String type, int bits, int numInputs) {
		int id = nextId++;
		open(type, id)
				.append(" int bits ").append(bits).append('\n')
				.append(" int numInputs ").append(numInputs).append('\n')
				.append(" String orientation \"right\"\n")
				.append(" int delay 10\n")
				.append("END\n");
		return id;
	}

	public int inputPin(String name, int bits) {
		int id = nextId++;
		open("InputPin", id)
				.append(" String name \"").append(name).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int watch 0\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n");
		return id;
	}

	public int outputPin(String name, int bits) {
		int id = nextId++;
		open("OutputPin", id)
				.append(" String name \"").append(name).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int watch 1\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n");
		return id;
	}

	/** Puts: inputs "A", "B", "Cin" (1 bit); outputs "S", "Cout" (1 bit). */
	public int adder(int bits) {
		int id = nextId++;
		open("Adder", id)
				.append(" int bits ").append(bits).append('\n')
				.append(" String orient \"UP\"\n")
				.append(" int delay 12\n")
				.append("END\n");
		return id;
	}

	/** Puts: input "input" (bits wide), output "output" (2^bits wide). */
	public int decoder(int bits) {
		int id = nextId++;
		open("Decoder", id)
				.append(" int bits ").append(bits).append('\n')
				.append(" int delay 10\n")
				.append(" String orient \"DOWN\"\n")
				.append("END\n");
		return id;
	}

	/** Puts: input "input0" (1 bit), output "output" (bits wide). */
	public int extend(int bits) {
		int id = nextId++;
		open("Extend", id)
				.append(" int bits ").append(bits).append('\n')
				.append(" int numInputs 1\n")
				.append(" String orientation \"right\"\n")
				.append(" int delay 0\n")
				.append("END\n");
		return id;
	}

	/** Puts: "input0".."inputN-1", "select", "output". */
	public int mux(int inputs, int bits) {
		int id = nextId++;
		open("Mux", id)
				.append(" int inputs ").append(inputs).append('\n')
				.append(" int bits ").append(bits).append('\n')
				.append(" int delay 10\n")
				.append(" String iOrient \"DOWN\"\n")
				.append(" String sOrient \"LEFT\"\n")
				.append("END\n");
		return id;
	}

	/**
	 * Combinational barrel shifter (issue #122). Puts: "input" (bits),
	 * "amount" (ceil log2 bits), "output" (bits). Kind is LogicalLeft,
	 * LogicalRight or ArithmeticRight.
	 */
	public int shifter(String kind, int bits) {
		int id = nextId++;
		open("ShiftRegister", id)
				.append(" String type \"").append(kind).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int delay 25\n")
				.append(" String iOrient \"DOWN\"\n")
				.append(" String sOrient \"LEFT\"\n")
				.append("END\n");
		return id;
	}

	/**
	 * Truth table. Puts are named by the signal names. The table is
	 * rows x (inputs+outputs) cells; cell value 2 in an input column is
	 * a don't-care.
	 */
	public int truthTable(String name, String[] inputNames,
			String[] outputNames, int[][] table) {
		int id = nextId++;
		StringBuilder b = open("TruthTable", id)
				.append(" String name \"").append(name).append("\"\n")
				.append(" int delay 10\n")
				.append(" int rows ").append(table.length).append('\n')
				.append(" int cols ")
				.append(inputNames.length + outputNames.length).append('\n');
		for (String in : inputNames) {
			b.append(" String input \"").append(in).append("\"\n");
		}
		for (String out : outputNames) {
			b.append(" String output \"").append(out).append("\"\n");
		}
		for (int r = 0; r < table.length; r++) {
			for (int c = 0; c < table[r].length; c++) {
				b.append(" pair ").append(r).append(' ')
						.append(table[r][c]).append('\n');
			}
		}
		b.append("END\n");
		return id;
	}

	/**
	 * Binder: bundles range inputs into one output ("output"). Each
	 * range is the list of bus bit indices it carries, highest first;
	 * its put is named like "3-2" (contiguous) or "0" (single bit).
	 */
	public int binder(int bits, int[][] ranges) {
		return group("Binder", bits, ranges);
	}

	/** Splitter: input "input", one output put per range. */
	public int splitter(int bits, int[][] ranges) {
		return group("Splitter", bits, ranges);
	}

	private int group(String type, int bits, int[][] ranges) {
		int id = nextId++;
		StringBuilder b = open(type, id)
				.append(" int bits ").append(bits).append('\n')
				.append(" String orient \"RIGHT\"\n")
				.append(" String noncontig \"true\"\n")
				.append(" int tristate 0\n");
		for (int r = 0; r < ranges.length; r++) {
			// the saved value order is meaningful: put names derive
			// from it, and the editor stores range bits ascending -
			// "pair 0 2, pair 0 3" names its put "3-2", while the
			// reverse order names it "2, 3"
			int[] bitsAscending = ranges[r].clone();
			java.util.Arrays.sort(bitsAscending);
			for (int bit : bitsAscending) {
				b.append(" pair ").append(r).append(' ').append(bit)
						.append('\n');
			}
		}
		b.append("END\n");
		return id;
	}

	/** Named wireless jump source; put "input". */
	public int jumpStart(String name, int bits) {
		int id = nextId++;
		open("JumpStart", id)
				.append(" String name \"").append(name).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" int watch 0\n")
				.append(" String orientation \"RIGHT\"\n")
				.append("END\n");
		return id;
	}

	/** Named wireless jump sink; put "output". */
	public int jumpEnd(String name, int bits) {
		int id = nextId++;
		open("JumpEnd", id)
				.append(" String name \"").append(name).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" String orientation \"LEFT\"\n")
				.append("END\n");
		return id;
	}

	/**
	 * Signal generator driving named input pins over time. Grammar per
	 * SigSim: "pin initial (for dur value | until t value)* end".
	 */
	public int sigGen(String signals) {
		int id = nextId++;
		open("SigGen", id)
				.append(" String signals \"").append(signals).append("\"\n")
				.append("END\n");
		return id;
	}

	/** Stops the simulation when any attached 1-bit input is 1. */
	public int stop() {
		int id = nextId++;
		open("Stop", id).append("END\n");
		return id;
	}

	/** Puts: "address", "CS", "OE" (+"WE", "input" for RAM), "output". */
	public int memory(String type, int bits, int capacity, String init) {
		int id = nextId++;
		open("Memory", id)
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

	/** Clocked register; type "nff" (negative edge) or "pff". */
	public int register(int bits, long init, String type) {
		int id = nextId++;
		open("Register", id)
				.append(" String name \"reg").append(id).append("\"\n")
				.append(" int bits ").append(bits).append('\n')
				.append(" Int init ").append(init).append('\n')
				.append(" String orient \"RIGHT\"\n")
				.append(" int delay 8\n")
				.append(" String type \"").append(type).append("\"\n")
				.append(" int watch 1\n")
				.append("END\n");
		return id;
	}

	/** Value display; put "input". */
	public int display(int bits, int base) {
		int id = nextId++;
		open("Display", id)
				.append(" int bits ").append(bits).append('\n')
				.append(" int base ").append(base).append('\n')
				.append("END\n");
		return id;
	}

	/** Decorative annotation; no puts. */
	public int text(String content) {
		int id = nextId++;
		open("Text", id)
				.append(" String text \"").append(content).append("\"\n")
				.append(" String fn \"Dialog\"\n int fs 12\n")
				.append(" int bold 0\n int ital 0\n int color -16777216\n")
				.append("END\n");
		return id;
	}

	/** Pauses the interactive simulator; puts "input0".."input3". */
	public int pause() {
		int id = nextId++;
		open("Pause", id).append("END\n");
		return id;
	}

	/** Puts: "input", "control", "output"; control perpendicular. */
	public int triState(int bits) {
		int id = nextId++;
		open("TriState", id)
				.append(" int bits ").append(bits).append('\n')
				.append(" int delay 10\n")
				.append(" String Gorient \"UP\"\n")
				.append(" String Corient \"LEFT\"\n")
				.append("END\n");
		return id;
	}

	/**
	 * One-state machine driving output signal z with the given value on
	 * every clock edge (the SequentialGoldenTest fixture shape).
	 * Puts: "clock" and the output signal name "z".
	 */
	public int stateMachine(long zValue, int zBits) {
		int id = nextId++;
		open("StateMachine", id)
				.append(" String name \"sm").append(id).append("\"\n")
				.append(" int delay 5\n int trig 0\n")
				.append(" String state \"S\"\n")
				.append("  int x 40\n  int y 40\n  int diameter 40\n")
				.append("  int init 1\n")
				.append("  String output \"z\"\n   long value ").append(zValue)
				.append("\n   int bits ").append(zBits).append('\n')
				.append("  String trans \"always\"\n   String next \"S\"\n")
				.append("END\n");
		return id;
	}

	/**
	 * Subcircuit block wrapping a one-wire passthrough inner circuit
	 * (input pin "a" wired to output pin "y"). The block's puts take
	 * the inner pins' names: "a" (input) and "y" (output).
	 */
	public int subCircuit(String name) {
		int id = nextId++;
		text.append("ELEMENT SubCircuit\n")
				.append(" String orient \"RIGHT\"\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(60 + 24 * id).append('\n')
				.append(" int y 60\n int width 48\n int height 48\n")
				.append(" String name \"").append(name).append("\"\n")
				.append("CIRCUIT inner\n")
				.append("ELEMENT InputPin\n")
				.append(" int id 0\n int x 60\n int y 60\n")
				.append(" int width 48\n int height 24\n")
				.append(" String name \"a\"\n int bits 1\n int watch 0\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n")
				.append("ELEMENT OutputPin\n")
				.append(" int id 1\n int x 240\n int y 60\n")
				.append(" int width 48\n int height 24\n")
				.append(" String name \"y\"\n int bits 1\n int watch 0\n")
				.append(" String orient \"RIGHT\"\n")
				.append("END\n")
				.append("ELEMENT WireEnd\n")
				.append(" int id 2\n int x 120\n int y 60\n")
				.append(" int width 8\n int height 8\n")
				.append(" String put \"output\"\n ref attach 0\n ref wire 3\n")
				.append("END\n")
				.append("ELEMENT WireEnd\n")
				.append(" int id 3\n int x 180\n int y 60\n")
				.append(" int width 8\n int height 8\n")
				.append(" String put \"input\"\n ref attach 1\n ref wire 2\n")
				.append("END\n")
				.append("ENDCIRCUIT\n")
				.append("END\n");
		return id;
	}

	public int clock(int cycle, int one) {
		int id = nextId++;
		open("Clock", id)
				.append(" int cycle ").append(cycle).append('\n')
				.append(" int one ").append(one).append('\n')
				.append(" String orient \"DOWN\"\n")
				.append("END\n");
		return id;
	}

	/** Wire fromElement's put to toElement's put, as two attached wire ends. */
	public void wire(int fromElement, String fromPut, int toElement,
			String toPut) {
		int endA = nextId++;
		int endB = nextId++;
		wireEnd(endA, fromElement, fromPut, endB);
		wireEnd(endB, toElement, toPut, endA);
	}

	private void wireEnd(int id, int attachTo, String put, int otherEnd) {
		text.append("ELEMENT WireEnd\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(12 * id).append('\n')
				.append(" int y 480\n")
				.append(" int width 8\n int height 8\n")
				.append(" String put \"").append(put).append("\"\n")
				.append(" ref attach ").append(attachTo).append('\n')
				.append(" ref wire ").append(otherEnd).append('\n')
				.append("END\n");
	}

	public String build() {
		return text + "ENDCIRCUIT\n";
	}
}
