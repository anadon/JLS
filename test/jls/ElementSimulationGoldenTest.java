package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.OutputPin;
import jls.sim.BatchSimulator;

/**
 * Per-element simulation goldens (issue #158). BatchSimulationGoldenTest
 * and SequentialGoldenTest cover the gates, memory and the clocked
 * elements; this suite gives every remaining simulating palette element
 * a behavioral pin, so a regression in, e.g., Adder carry handling or
 * Mux select decoding fails mvn verify instead of passing silently.
 *
 * Expected values are computed independently of the implementation
 * (issue #158 section 10): adder sums and carries by arithmetic, shifter
 * results by hand, decoder one-hot by definition - never by running the
 * simulator and copying what it printed.
 *
 * The completeness ratchet at the bottom enumerates the concrete element
 * classes reflectively (ElementConstructorContractTest's sweep) and
 * fails when a simulating element has no golden and no recorded
 * exemption - so the gap this issue closes cannot silently reopen when
 * the palette grows.
 */
class ElementSimulationGoldenTest {

	// ------------------------------------------------------------------
	// harness
	// ------------------------------------------------------------------

	/** Load circuit text, run the batch simulator, return the circuit. */
	private static Circuit simulate(String circuitText) {
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		try {
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad failed: " + JLSInfo.loadError);
		} catch (Exception e) {
			throw new AssertionError("finishLoad threw", e);
		}
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();
		return circuit;
	}

	/** The settled value of a named watched output pin. */
	private static long pinValue(Circuit circuit, String pinName) {
		OutputPin pin = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin
					&& pinName.equals(((OutputPin) el).getName())) {
				pin = (OutputPin) el;
			}
		}
		assertNotNull(pin, "output pin " + pinName + " not found");
		BitSet value = pin.getCurrentValue();
		assertNotNull(value, "output pin " + pinName + " never settled");
		return BitSetUtils.ToLong(value);
	}

	private static long simulate(String circuitText, String pinName) {
		return pinValue(simulate(circuitText), pinName);
	}

	// ------------------------------------------------------------------
	// Adder
	// ------------------------------------------------------------------

	@Test
	void adderSumsAndCarries() {
		// 4-bit adder: S = (A+B+Cin) mod 16, Cout = the carry out -
		// expected values by arithmetic, including both carry cases
		long[] interesting = {0, 1, 5, 7, 10, 15};
		for (long a : interesting) {
			for (long b : interesting) {
				for (long cin = 0; cin <= 1; cin++) {
					CircuitTextBuilder cb = new CircuitTextBuilder();
					int adder = cb.adder(4);
					int ca = cb.constant(a);
					int cbv = cb.constant(b);
					int cc = cb.constant(cin);
					int sum = cb.outputPin("sum", 4);
					int carry = cb.outputPin("carry", 1);
					cb.wire(ca, "output", adder, "A");
					cb.wire(cbv, "output", adder, "B");
					cb.wire(cc, "output", adder, "Cin");
					cb.wire(adder, "S", sum, "input");
					cb.wire(adder, "Cout", carry, "input");
					Circuit circuit = simulate(cb.build());
					long total = a + b + cin;
					assertEquals(total % 16, pinValue(circuit, "sum"),
							a + "+" + b + "+" + cin + " sum");
					assertEquals(total / 16, pinValue(circuit, "carry"),
							a + "+" + b + "+" + cin + " carry");
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Decoder
	// ------------------------------------------------------------------

	@Test
	void decoderOutputIsOneHot() {
		// n-bit decoder: 2^n-bit output with exactly bit[input] set
		for (long in = 0; in < 4; in++) {
			CircuitTextBuilder cb = new CircuitTextBuilder();
			int dec = cb.decoder(2);
			int c = cb.constant(in);
			int out = cb.outputPin("out", 4);
			cb.wire(c, "output", dec, "input");
			cb.wire(dec, "output", out, "input");
			assertEquals(1L << in, simulate(cb.build(), "out"),
					"decode(" + in + ")");
		}
		// 3-bit spot checks at the edges
		for (long in : new long[] {0, 5, 7}) {
			CircuitTextBuilder cb = new CircuitTextBuilder();
			int dec = cb.decoder(3);
			int c = cb.constant(in);
			int out = cb.outputPin("out", 8);
			cb.wire(c, "output", dec, "input");
			cb.wire(dec, "output", out, "input");
			assertEquals(1L << in, simulate(cb.build(), "out"),
					"decode3(" + in + ")");
		}
	}

	// ------------------------------------------------------------------
	// Extend
	// ------------------------------------------------------------------

	@Test
	void extendReplicatesTheBit() {
		// 1-bit input sign-extended across the output width
		for (long in = 0; in <= 1; in++) {
			CircuitTextBuilder cb = new CircuitTextBuilder();
			int ext = cb.extend(4);
			int c = cb.constant(in);
			int out = cb.outputPin("out", 4);
			cb.wire(c, "output", ext, "input0");
			cb.wire(ext, "output", out, "input");
			assertEquals(in == 0 ? 0 : 0b1111, simulate(cb.build(), "out"),
					"extend(" + in + ")");
		}
	}

	// ------------------------------------------------------------------
	// Mux
	// ------------------------------------------------------------------

	@Test
	void muxRoutesTheSelectedInput() {
		// 4-way mux over 8-bit data: out = input[select], with four
		// distinct data values so a wrong pick cannot alias a right one
		long[] data = {0x11, 0x22, 0x44, 0x88};
		for (long select = 0; select < 4; select++) {
			CircuitTextBuilder cb = new CircuitTextBuilder();
			int mux = cb.mux(4, 8);
			int sel = cb.constant(select);
			int out = cb.outputPin("out", 8);
			for (int i = 0; i < 4; i++) {
				int c = cb.constant(data[i]);
				cb.wire(c, "output", mux, "input" + i);
			}
			cb.wire(sel, "output", mux, "select");
			cb.wire(mux, "output", out, "input");
			assertEquals(data[(int) select], simulate(cb.build(), "out"),
					"mux select " + select);
		}
	}

	// ------------------------------------------------------------------
	// ShiftRegister (combinational barrel shifter, issue #122)
	// ------------------------------------------------------------------

	private long shifterGolden(String kind, long value, long amount) {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int sh = cb.shifter(kind, 8);
		int v = cb.constant(value);
		int a = cb.constant(amount);
		int out = cb.outputPin("out", 8);
		cb.wire(v, "output", sh, "input");
		cb.wire(a, "output", sh, "amount");
		cb.wire(sh, "output", out, "input");
		return simulate(cb.build(), "out");
	}

	@Test
	void logicalLeftShiftZeroFills() {
		// 0xB4 = 1011_0100
		assertEquals(0xB4, shifterGolden("LogicalLeft", 0xB4, 0));
		assertEquals(0x68, shifterGolden("LogicalLeft", 0xB4, 1));
		assertEquals(0xA0, shifterGolden("LogicalLeft", 0xB4, 3));
		assertEquals(0x00, shifterGolden("LogicalLeft", 0xB4, 7));
	}

	@Test
	void logicalRightShiftZeroFills() {
		assertEquals(0xB4, shifterGolden("LogicalRight", 0xB4, 0));
		assertEquals(0x5A, shifterGolden("LogicalRight", 0xB4, 1));
		assertEquals(0x16, shifterGolden("LogicalRight", 0xB4, 3));
		assertEquals(0x01, shifterGolden("LogicalRight", 0xB4, 7));
	}

	@Test
	void arithmeticRightShiftSignFills() {
		// sign bit set: ones shift in from the left
		assertEquals(0xDA, shifterGolden("ArithmeticRight", 0xB4, 1));
		assertEquals(0xF6, shifterGolden("ArithmeticRight", 0xB4, 3));
		assertEquals(0xFF, shifterGolden("ArithmeticRight", 0xB4, 7));
		// sign bit clear: behaves like logical right
		assertEquals(0x1A, shifterGolden("ArithmeticRight", 0x34, 1));
		assertEquals(0x06, shifterGolden("ArithmeticRight", 0x34, 3));
	}

	// ------------------------------------------------------------------
	// TruthTable
	// ------------------------------------------------------------------

	@Test
	void truthTableMatchesItsRows() {
		// a NAND written as an explicit table: rows are (a, b, z)
		int[][] nand = {
				{0, 0, 1},
				{0, 1, 1},
				{1, 0, 1},
				{1, 1, 0},
		};
		for (long a = 0; a <= 1; a++) {
			for (long b = 0; b <= 1; b++) {
				CircuitTextBuilder cb = new CircuitTextBuilder();
				int tt = cb.truthTable("tt" + a + b,
						new String[] {"a", "b"}, new String[] {"z"}, nand);
				int ca = cb.constant(a);
				int cbv = cb.constant(b);
				int out = cb.outputPin("out", 1);
				cb.wire(ca, "output", tt, "a");
				cb.wire(cbv, "output", tt, "b");
				cb.wire(tt, "z", out, "input");
				assertEquals((a == 1 && b == 1) ? 0 : 1,
						simulate(cb.build(), "out"),
						"table(" + a + "," + b + ")");
			}
		}
	}

	@Test
	void truthTableDontCareMatchesEitherValue() {
		// row (2, 1): input a is don't-care whenever b is 1
		int[][] table = {
				{2, 1, 1},
				{0, 0, 0},
				{1, 0, 1},
		};
		long[][] cases = {{0, 1, 1}, {1, 1, 1}, {0, 0, 0}, {1, 0, 1}};
		for (long[] c : cases) {
			CircuitTextBuilder cb = new CircuitTextBuilder();
			int tt = cb.truthTable("tt" + c[0] + c[1],
					new String[] {"a", "b"}, new String[] {"z"}, table);
			int ca = cb.constant(c[0]);
			int cbv = cb.constant(c[1]);
			int out = cb.outputPin("out", 1);
			cb.wire(ca, "output", tt, "a");
			cb.wire(cbv, "output", tt, "b");
			cb.wire(tt, "z", out, "input");
			assertEquals(c[2], simulate(cb.build(), "out"),
					"dontcare(" + c[0] + "," + c[1] + ")");
		}
	}

	// ------------------------------------------------------------------
	// Binder / Splitter
	// ------------------------------------------------------------------

	@Test
	void splitterRoutesBusBitsToRangeOutputs() {
		// 4-bit bus 1101 split into "3-2" (11) and "1-0" (01)
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int split = cb.splitter(4, new int[][] {{3, 2}, {1, 0}});
		int c = cb.constant(0b1101);
		int hi = cb.outputPin("hi", 2);
		int lo = cb.outputPin("lo", 2);
		cb.wire(c, "output", split, "input");
		cb.wire(split, "3-2", hi, "input");
		cb.wire(split, "1-0", lo, "input");
		Circuit circuit = simulate(cb.build());
		assertEquals(0b11, pinValue(circuit, "hi"), "high range");
		assertEquals(0b01, pinValue(circuit, "lo"), "low range");
	}

	@Test
	void binderBundlesRangeInputsIntoTheBus() {
		// inverse of the splitter case: 11 into "3-2" and 01 into "1-0"
		// must reassemble 1101
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int bind = cb.binder(4, new int[][] {{3, 2}, {1, 0}});
		int chi = cb.constant(0b11);
		int clo = cb.constant(0b01);
		int out = cb.outputPin("out", 4);
		cb.wire(chi, "output", bind, "3-2");
		cb.wire(clo, "output", bind, "1-0");
		cb.wire(bind, "output", out, "input");
		assertEquals(0b1101, simulate(cb.build(), "out"));
	}

	@Test
	void splitterSingleBitRangeIsolatesOneLine() {
		// 3-bit bus 101: single-bit ranges "2", "1", "0"
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int split = cb.splitter(3, new int[][] {{2}, {1}, {0}});
		int c = cb.constant(0b101);
		int b2 = cb.outputPin("b2", 1);
		int b1 = cb.outputPin("b1", 1);
		int b0 = cb.outputPin("b0", 1);
		cb.wire(c, "output", split, "input");
		cb.wire(split, "2", b2, "input");
		cb.wire(split, "1", b1, "input");
		cb.wire(split, "0", b0, "input");
		Circuit circuit = simulate(cb.build());
		assertEquals(1, pinValue(circuit, "b2"), "bit 2");
		assertEquals(0, pinValue(circuit, "b1"), "bit 1");
		assertEquals(1, pinValue(circuit, "b0"), "bit 0");
	}

	// ------------------------------------------------------------------
	// JumpStart / JumpEnd
	// ------------------------------------------------------------------

	@Test
	void jumpCarriesTheValueWirelessly() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int c = cb.constant(0b1010);
		int start = cb.jumpStart("js", 4);
		int end = cb.jumpEnd("js", 4);
		int out = cb.outputPin("out", 4);
		cb.wire(c, "output", start, "input");
		cb.wire(end, "output", out, "input");
		assertEquals(0b1010, simulate(cb.build(), "out"));
	}

	// ------------------------------------------------------------------
	// SigGen
	// ------------------------------------------------------------------

	@Test
	void sigGenDrivesItsNamedPinOverTime() {
		// in1 starts at 0 and becomes 1 at t=50; the settled value on
		// the wired-through output pin must be the final one
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int in = cb.inputPin("in1", 1);
		cb.sigGen("in1 0 for 50 1 end");
		int out = cb.outputPin("out", 1);
		cb.wire(in, "output", out, "input");
		assertEquals(1, simulate(cb.build(), "out"));
	}

	// ------------------------------------------------------------------
	// Stop
	// ------------------------------------------------------------------

	@Test
	void stopHaltsTheSimulationEarly() {
		// a free-running clock would run to the 1,000,000 time limit;
		// a Stop fed a constant 1 must end the run at once. The
		// outcome line is the observable (same one batch mode prints).
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.clock(20, 10);
		int one = cb.constant(1);
		int stop = cb.stop();
		cb.wire(one, "output", stop, "input0");

		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		try {
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad failed: " + JLSInfo.loadError);
		} catch (Exception e) {
			throw new AssertionError("finishLoad threw", e);
		}
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();

		PrintStream saved = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			sim.displayOutcome();
		} finally {
			System.setOut(saved);
		}
		String outcome = captured.toString(StandardCharsets.UTF_8);
		assertTrue(outcome.startsWith("Simulation Stopped at "),
				"a triggered Stop must end the run (got: " + outcome + ")");
		long stoppedAt = Long.parseLong(outcome
				.substring("Simulation Stopped at ".length()).trim());
		assertTrue(stoppedAt < 1_000_000,
				"the stop must fire before the time limit, at "
						+ stoppedAt);
	}

	// ------------------------------------------------------------------
	// completeness ratchet
	// ------------------------------------------------------------------

	/**
	 * Elements with a behavioral simulation golden, and where it lives.
	 * Add the element here when its golden lands - the ratchet below
	 * fails any simulating element in neither this set nor EXEMPT.
	 */
	private static final Set<String> COVERED = Set.of(
			// BatchSimulationGoldenTest
			"AndGate", "OrGate", "NandGate", "NorGate", "XorGate",
			"NotGate", "DelayGate", "Constant", "Memory", "OutputPin",
			// SequentialGoldenTest / SimulationSemanticsRegressionTest
			"Clock", "Register", "StateMachine", "SubCircuit", "TriState",
			"Pause", "InputPin", "WireEnd",
			// this suite
			"Adder", "Decoder", "Extend", "Mux", "ShiftRegister",
			"TruthTable", "Binder", "Splitter", "JumpStart", "JumpEnd",
			"SigGen", "Stop");

	/**
	 * Elements deliberately without a simulation golden. Every entry
	 * needs a reason; removing an element from the palette removes it
	 * here too.
	 */
	private static final Set<String> EXEMPT = Set.of(
			// display-only: consumes a value for the GUI, drives nothing
			"Display",
			// decorative annotation, no puts, never simulates
			"Text",
			// the -t test-vector driver; exercised end to end by the
			// batch CLI suites and VcdExportGoldenTest
			"TestGen");

	@Test
	void everySimulatingElementHasAGoldenOrARecordedExemption()
			throws Exception {
		File dir = new File(Element.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		File pkg = new File(dir, "jls/elem");
		assertTrue(pkg.isDirectory(), "compiled classes not found at " + pkg);

		Set<String> unaccounted = new TreeSet<>();
		List<String> stale = new ArrayList<>();
		Set<String> concrete = new TreeSet<>();
		for (File f : pkg.listFiles()) {
			String name = f.getName();
			if (!name.endsWith(".class") || name.contains("$")) {
				continue;
			}
			String simple = name.replace(".class", "");
			Class<?> c = Class.forName("jls.elem." + simple);
			if (!Element.class.isAssignableFrom(c)
					|| Modifier.isAbstract(c.getModifiers())) {
				continue;
			}
			if (c == jls.elem.Wire.class) {
				continue; // rebuilt from WireEnd refs, not a palette item
			}
			if (c == Element.class) {
				continue; // the (historically non-abstract) base class
			}
			concrete.add(simple);
			if (!COVERED.contains(simple) && !EXEMPT.contains(simple)) {
				unaccounted.add(simple);
			}
		}
		assertTrue(concrete.size() > 25,
				"sweep found too few element classes (" + concrete.size()
						+ ") - wrong directory?");
		// both lists must stay honest: no unknown names rotting in them
		for (String claimed : COVERED) {
			if (!concrete.contains(claimed)) {
				stale.add("COVERED: " + claimed);
			}
		}
		for (String claimed : EXEMPT) {
			if (!concrete.contains(claimed)) {
				stale.add("EXEMPT: " + claimed);
			}
		}
		assertTrue(stale.isEmpty(),
				"ratchet lists name classes that no longer exist: " + stale);
		assertTrue(unaccounted.isEmpty(),
				"simulating elements with no behavioral golden and no"
						+ " recorded exemption (add a golden to"
						+ " ElementSimulationGoldenTest, or an exemption"
						+ " with a reason): " + unaccounted);
	}
}
