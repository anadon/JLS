package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.Input;
import jls.elem.LogicElement;
import jls.elem.OutputPin;
import jls.elem.Pause;
import jls.elem.Register;
import jls.elem.SubCircuit;
import jls.elem.TriState;
import jls.sim.BatchSimulator;
import jls.sim.SimEvent;
import jls.sim.Simulator;

/**
 * Regression pins for the seven semantic surprises adjudicated in issue
 * #98 (docs/simulation-semantics.md, formerly its appendix):
 *
 * - S1: multi-driver tri-state resolution is deterministic (first
 *   active driver in net order) and a bus conflict warns once;
 * - S2: Register.initSim assigns the currentValue field (no shadowing
 *   local), so the watched value at time 0 is the initial value;
 * - S3: input points inside subcircuits are initialized to 0 like
 *   top-level ones (SubCircuit.initInputs recurses - the spec appendix
 *   had this wrong, pinned here as intended behavior);
 * - S4: Pause pauses only when its input changes to a non-zero value
 *   (the help page now says so too);
 * - S5: a state machine with no matching transition stays in its
 *   current state, keeps recognizing clock edges, and warns once
 *   instead of freezing silently;
 * - S6: TriState suppresses redundant output events like Gate.react;
 * - S7: Constant masks its value to the attached net's width
 *   (documented as intended in the spec, section 6.2).
 */
class SimulationSemanticsRegressionTest {

	/** Circuit text builder in the on-disk format (see #14 harness). */
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
				.append(" int x ").append(60 + 12 * id).append('\n')
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

		int clock(int cycle, int one) {
			return element("Clock", " int cycle " + cycle + "\n int one "
					+ one + "\n String orient \"RIGHT\"\n");
		}

		int inputPin(String name, int bits) {
			return element("InputPin", " String name \"" + name
					+ "\"\n int bits " + bits
					+ "\n int watch 0\n String orient \"RIGHT\"\n");
		}

		int outputPin(String name, int bits) {
			return element("OutputPin", " String name \"" + name
					+ "\"\n int bits " + bits
					+ "\n int watch 0\n String orient \"RIGHT\"\n");
		}

		int triState(int bits) {
			return element("TriState", " int bits " + bits
					+ "\n int delay 5\n String Gorient \"RIGHT\"\n"
					+ " String Corient \"DOWN\"\n");
		}

		int register(String name, int bits, long init, String type) {
			return element("Register", " String name \"" + name
					+ "\"\n int bits " + bits + "\n Int init " + init
					+ "\n String orient \"RIGHT\"\n int delay 5\n"
					+ " String type \"" + type + "\"\n int watch 1\n");
		}

		int pause() {
			return element("Pause", "");
		}

		/**
		 * A two-state machine (falling-edge triggered, delay 5) with a
		 * 1-bit output z: initial state S drives z=0 and moves to T
		 * when 1-bit input a equals 1; T drives z=1 and loops forever.
		 */
		int stateMachine() {
			return element("StateMachine", " String name \"sm\"\n"
					+ " int delay 5\n int trig 0\n"
					+ " String state \"S\"\n"
					+ "  int x 40\n  int y 40\n  int diameter 40\n  int init 1\n"
					+ "  String output \"z\"\n   long value 0\n   int bits 1\n"
					+ "  String trans \"a\"\n   int eq 0\n   int value 1\n"
					+ "   int bits 1\n   String next \"T\"\n"
					+ " String state \"T\"\n"
					+ "  int x 80\n  int y 80\n  int diameter 40\n  int init 0\n"
					+ "  String output \"z\"\n   long value 1\n   int bits 1\n"
					+ "  String trans \"always\"\n   String next \"T\"\n");
		}

		/**
		 * A subcircuit wrapping an inner circuit in which input pin a
		 * feeds output pin y, so the inner circuit has an input point
		 * that needs initializing.
		 */
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
				.append(" int id 3\n int x 200\n int y 60\n")
				.append(" int width 8\n int height 8\n")
				.append(" String put \"input\"\n ref attach 1\n ref wire 2\n")
				.append("END\n")
				.append("ENDCIRCUIT\n")
				.append("END\n");
			return id;
		}

		/** Wire fromElement's put to toElement's put (two-ended net). */
		void wire(int fromElement, String fromPut, int toElement, String toPut) {
			int endA = nextId++;
			int endB = nextId++;
			wireEnd(endA, fromElement, fromPut, false, endB);
			wireEnd(endB, toElement, toPut, false, endA);
		}

		/**
		 * A three-ended tri-state net; net order (which decides the
		 * multi-driver winner) is the order the ends appear here.
		 */
		void triWire3(int el1, String put1, int el2, String put2,
				int el3, String put3) {
			int endA = nextId++;
			int endB = nextId++;
			int endC = nextId++;
			wireEnd(endA, el1, put1, true, endB, endC);
			wireEnd(endB, el2, put2, true, endA);
			wireEnd(endC, el3, put3, true, endA);
		}

		private void wireEnd(int id, int attachTo, String put,
				boolean triState, int... others) {
			text.append("ELEMENT WireEnd\n")
				.append(" int id ").append(id).append('\n')
				.append(" int x ").append(12 * id).append('\n')
				.append(" int y 240\n")
				.append(" int width 8\n int height 8\n");
			if (triState) {
				text.append(" int tristate 1\n");
			}
			text.append(" String put \"").append(put).append("\"\n")
				.append(" ref attach ").append(attachTo).append('\n');
			for (int other : others) {
				text.append(" ref wire ").append(other).append('\n');
			}
			text.append("END\n");
		}

		String build() {
			return text + "ENDCIRCUIT\n";
		}
	}

	/** Load through the real loader (real Graphics: subcircuits size
	 * themselves with font metrics during finishLoad). */
	private static Circuit load(String circuitText) throws Exception {
		Circuit circuit = new Circuit("semantics");
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

	@SuppressWarnings("unchecked")
	private static <T extends Element> T find(Circuit circuit, Class<T> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return (T) el;
			}
		}
		throw new AssertionError("no " + type.getSimpleName() + " in circuit");
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

	/** Run body with stderr captured (TellUser diagnostics land there
	 * in headless runs) and return what was written. */
	private static String captureStderr(ThrowingRunnable body)
			throws Exception {
		PrintStream saved = System.err;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setErr(new PrintStream(buffer, true,
				StandardCharsets.UTF_8.name()));
		try {
			body.run();
		} finally {
			System.setErr(saved);
		}
		return buffer.toString(StandardCharsets.UTF_8.name());
	}

	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	private static int count(String haystack, String needle) {
		int hits = 0;
		for (int at = haystack.indexOf(needle); at >= 0;
				at = haystack.indexOf(needle, at + 1)) {
			hits += 1;
		}
		return hits;
	}

	/** A simulator that counts post and pause calls. */
	private static final class CountingSimulator extends Simulator {
		int posts = 0;
		int pauses = 0;
		@Override
		public void post(SimEvent event) {
			posts += 1;
			super.post(event);
		}
		@Override
		public void stop() {
		}
		@Override
		public void pause(boolean which) {
			if (which) {
				pauses += 1;
			}
		}
	}

	// ---------------------------------------------------------------
	// S1
	// ---------------------------------------------------------------

	/**
	 * Two tri-state drivers simultaneously active with different values
	 * on one net: the first active driver in net order (tri1, wired
	 * first) wins deterministically, and the conflict is reported once.
	 */
	@Test
	void multiDriverConflictResolvesDeterministicallyAndWarnsOnce()
			throws Exception {
		CircuitBuilder cb = new CircuitBuilder("conflict");
		int d1 = cb.constant(5);
		int d2 = cb.constant(9);
		int on1 = cb.constant(1);
		int on2 = cb.constant(1);
		int tri1 = cb.triState(4);
		int tri2 = cb.triState(4);
		int o = cb.outputPin("o", 4);
		cb.wire(d1, "output", tri1, "input");
		cb.wire(d2, "output", tri2, "input");
		cb.wire(on1, "output", tri1, "control");
		cb.wire(on2, "output", tri2, "control");
		cb.triWire3(o, "input", tri1, "output", tri2, "output");
		Circuit circuit = load(cb.build());
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(100);
		String stderr = captureStderr(() -> sim.runSim());
		assertEquals(5, pinValue(circuit, "o"),
				"the first active driver in net order must win");
		assertEquals(1, count(stderr, "bus conflict"),
				"a bus conflict must be reported exactly once, got:\n"
						+ stderr);
	}

	/** Simultaneously active drivers that agree are not a conflict. */
	@Test
	void agreeingTriStateDriversDoNotWarn() throws Exception {
		CircuitBuilder cb = new CircuitBuilder("agree");
		int d1 = cb.constant(5);
		int d2 = cb.constant(5);
		int on1 = cb.constant(1);
		int on2 = cb.constant(1);
		int tri1 = cb.triState(4);
		int tri2 = cb.triState(4);
		int o = cb.outputPin("o", 4);
		cb.wire(d1, "output", tri1, "input");
		cb.wire(d2, "output", tri2, "input");
		cb.wire(on1, "output", tri1, "control");
		cb.wire(on2, "output", tri2, "control");
		cb.triWire3(o, "input", tri1, "output", tri2, "output");
		Circuit circuit = load(cb.build());
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(100);
		String stderr = captureStderr(() -> sim.runSim());
		assertEquals(5, pinValue(circuit, "o"));
		assertFalse(stderr.contains("bus conflict"),
				"agreeing drivers must not warn, got:\n" + stderr);
	}

	// ---------------------------------------------------------------
	// S2
	// ---------------------------------------------------------------

	/**
	 * Register.initSim must assign the currentValue field (issue #98,
	 * S2: a shadowing local left the field stale) - the trace's time-0
	 * sample (findWatched) reads this field before any event runs.
	 * A fresh load masks the bug (the loader also seeds the field), so
	 * the pin is a re-initialization after a run that captured a
	 * different value: the watched value at time 0 of the second run
	 * must be the initial value again, not the first run's result.
	 */
	@Test
	void registerInitSimResetsTheWatchedCurrentValue() throws Exception {
		CircuitBuilder cb = new CircuitBuilder("reginit");
		int clk = cb.clock(20, 10);
		int data = cb.constant(5);
		int reg = cb.register("reg", 4, 9, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		Circuit circuit = load(cb.build());
		Register register = find(circuit, Register.class);

		// first run: the register captures 5 on the first rising edge
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(100);
		sim.runSim();
		assertEquals(5, BitSetUtils.ToLong(register.getCurrentValue()),
				"the first run must capture the driven data");

		// re-initialization: the watched value must be the initial
		// value again before any event of the second run fires
		register.initSim(new BatchSimulator());
		BitSet current = register.getCurrentValue();
		assertNotNull(current);
		assertEquals(9, BitSetUtils.ToLong(current),
				"immediately after initSim the watched value must be the"
						+ " register's initial value, not the previous"
						+ " run's final value");
	}

	// ---------------------------------------------------------------
	// S3
	// ---------------------------------------------------------------

	/**
	 * Input points inside subcircuits are initialized to 0 exactly like
	 * top-level ones: Simulator.initInputs only walks the top level,
	 * but SubCircuit.initInputs recurses (the spec appendix S3 claimed
	 * otherwise; this pins the depth-uniform behavior).
	 */
	@Test
	void initInputsReachesInsideSubcircuits() throws Exception {
		CircuitBuilder cb = new CircuitBuilder("nested");
		cb.subCircuit("sub");
		Circuit circuit = load(cb.build());
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.initInputs(circuit);
		SubCircuit sub = find(circuit, SubCircuit.class);
		int checked = 0;
		for (Element el : sub.getSubCircuit().getElements()) {
			if (!(el instanceof LogicElement)) {
				continue;
			}
			for (Input in : ((LogicElement) el).getInputList()) {
				BitSet value = in.getValue();
				assertNotNull(value, "input " + in.getName() + " of inner "
						+ el.getClass().getSimpleName()
						+ " must be initialized, not null");
				assertEquals(0, BitSetUtils.ToLong(value));
				checked += 1;
			}
		}
		assertTrue(checked > 0,
				"the inner circuit must contribute at least one input point");
	}

	// ---------------------------------------------------------------
	// S4
	// ---------------------------------------------------------------

	/**
	 * Pause pauses when its input changes to a non-zero value, and only
	 * then (this is what the help page and spec now say; a change to
	 * zero must not pause).
	 */
	@Test
	void pausePausesOnlyOnNonZeroInput() throws Exception {
		CircuitBuilder cb = new CircuitBuilder("pausing");
		int a = cb.inputPin("a", 1);
		int pause = cb.pause();
		cb.wire(a, "output", pause, "input0");
		Circuit circuit = load(cb.build());
		Pause element = find(circuit, Pause.class);
		Input attached = null;
		for (Input in : element.getInputList()) {
			if (in.isAttached()) {
				attached = in;
			}
		}
		assertNotNull(attached, "the pause element must have a wired input");
		CountingSimulator sim = new CountingSimulator();
		element.initSim(sim);
		attached.setValue(BitSetUtils.Create(0));
		element.react(10, sim, null);
		assertEquals(0, sim.pauses, "a zero input must not pause");
		attached.setValue(BitSetUtils.Create(1));
		element.react(20, sim, null);
		assertEquals(1, sim.pauses, "a non-zero input must pause");
		attached.setValue(BitSetUtils.Create(0));
		element.react(30, sim, null);
		assertEquals(1, sim.pauses, "a change back to zero must not pause");
	}

	// ---------------------------------------------------------------
	// S5
	// ---------------------------------------------------------------

	/**
	 * A state machine that sees clock edges with no matching transition
	 * must stay in its current state and keep recognizing later edges
	 * (issue #98, S5: it used to set busy forever and silently ignore
	 * the rest of the run), and it must say so exactly once.
	 */
	@Test
	void stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
			throws Exception {
		CircuitBuilder cb = new CircuitBuilder("nomatch");
		int a = cb.inputPin("a", 1);
		int clk = cb.clock(20, 10);
		int sm = cb.stateMachine();
		int z = cb.outputPin("z", 1);
		cb.wire(a, "output", sm, "a");
		cb.wire(clk, "output", sm, "clock");
		cb.wire(sm, "z", z, "input");
		Circuit circuit = load(cb.build());

		// a is 0 for the falling edges at 20 and 40 (no transition
		// matches), then 1 before the edge at 60 (S -> T, z becomes 1)
		java.nio.file.Path vectors =
				java.nio.file.Files.createTempFile("nomatch", ".txt");
		java.nio.file.Files.writeString(vectors, "a 0 until 50 1 end\n");
		String stderr;
		try {
			BatchSimulator sim = new BatchSimulator();
			sim.setCircuit(circuit);
			sim.setTimeLimit(100);
			sim.setTestFile(vectors.toString());
			sim.addTestGen();
			stderr = captureStderr(() -> sim.runSim());
		} finally {
			java.nio.file.Files.deleteIfExists(vectors);
		}
		assertEquals(1, pinValue(circuit, "z"),
				"the machine must still take the transition once it"
						+ " matches - it must not freeze on the earlier"
						+ " unmatched edges");
		assertEquals(1, count(stderr, "no transition"),
				"the unmatched edge must be reported exactly once, got:\n"
						+ stderr);
	}

	// ---------------------------------------------------------------
	// S6
	// ---------------------------------------------------------------

	/**
	 * TriState.react must not post an output event when the scheduled
	 * output cannot have changed (issue #98, S6): same value while on,
	 * or turning off while already off.
	 */
	@Test
	void triStateDoesNotRepostUnchangedOutputEvents() {
		Circuit circuit = new Circuit("tri");
		TriState tri = new TriState(circuit);
		tri.init(null);
		tri.initInputs();
		CountingSimulator sim = new CountingSimulator();
		tri.initSim(sim);
		Input data = tri.getInputList().get(0);
		Input control = tri.getInputList().get(1);

		// turn on with data 5: one event
		control.setValue(BitSetUtils.Create(1));
		data.setValue(BitSetUtils.Create(5));
		tri.react(10, sim, null);
		assertEquals(1, sim.posts, "the first value must be scheduled");

		// unrelated notification, same output: no new event
		tri.react(20, sim, null);
		assertEquals(1, sim.posts,
				"an unchanged output must not be rescheduled");

		// data change: one more event
		data.setValue(BitSetUtils.Create(9));
		tri.react(30, sim, null);
		assertEquals(2, sim.posts, "a changed value must be scheduled");

		// turn off: one event; turning off again: none
		control.setValue(BitSetUtils.Create(0));
		tri.react(40, sim, null);
		assertEquals(3, sim.posts, "turning off must be scheduled");
		tri.react(50, sim, null);
		assertEquals(3, sim.posts,
				"an already-off gate must not be rescheduled");
	}

	// ---------------------------------------------------------------
	// S7
	// ---------------------------------------------------------------

	/**
	 * A Constant is width-agnostic: it drives its configured value
	 * truncated to the attached net's declared width (value mod 2^bits)
	 * - defined behavior per the spec, section 6.2.
	 */
	@Test
	void constantValueIsMaskedToTheNetWidth() throws Exception {
		CircuitBuilder cb = new CircuitBuilder("mask");
		int c = cb.constant(31);
		int o = cb.outputPin("o", 4);
		cb.wire(c, "output", o, "input");
		Circuit circuit = load(cb.build());
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(100);
		sim.runSim();
		assertEquals(15, pinValue(circuit, "o"),
				"a constant wider than its net drives value mod 2^bits");
	}
}
