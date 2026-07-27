package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.BitSetUtils;
import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.core.Orientation;
import jls.sim.BatchSimulator;

/**
 * Headless tests for the Register model (issue #159, following the
 * TruthTableModelTest pattern). The #77 extraction already moved every
 * dialog concern out of this class, so the mutators the GUI-side dialog
 * applies (name, bits, base, type, initial value, orientation), the
 * geometry operations (rotate, flip, resize), and the CLI-facing
 * printing can all be driven and observed without any GUI. The clocking
 * corners SequentialGoldenTest leaves open - a latch seeing a repeated
 * edge with unchanged data, a negative-edge register watching data move
 * while the clock is high - are pinned through the real loader and
 * BatchSimulator.
 */
class RegisterModelTest {

	/** A fresh register on a throwaway circuit. */
	private static Register newRegister() {
		return new Register(new Circuit("regmodel"));
	}

	/** The element's on-disk text, the observable model state. */
	private static String saved(Register reg) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			reg.save(writer);
		}
		return out.toString().replace(System.lineSeparator(), "\n");
	}

	/** Headless TextMetrics backed by a scratch image. */
	private static jls.core.TextMetrics metrics() {
		BufferedImage img = new BufferedImage(8, 8,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		return jls.edit.SwingTextMetrics.of(g);
	}

	// ---------------------------------------------------------------
	// dialog-side mutators
	// ---------------------------------------------------------------

	@Test
	void typeNameRoundTripsAndUnknownNamesAreIgnored() {
		Register reg = newRegister();
		assertEquals("latch", reg.getTypeName());
		reg.setTypeName("pff");
		assertEquals("pff", reg.getTypeName());
		reg.setTypeName("nff");
		assertEquals("nff", reg.getTypeName());
		reg.setTypeName("bogus");        // the loader's lenient contract
		assertEquals("nff", reg.getTypeName());
		reg.setTypeName("latch");
		assertEquals("latch", reg.getTypeName());
	}

	@Test
	void applyInitialValueResetsTheDisplayedCurrentValue() {
		Register reg = newRegister();
		reg.setBits(4);
		reg.applyInitialValue(BigInteger.valueOf(9));
		assertEquals(BigInteger.valueOf(9), reg.getInitialValue());
		assertEquals(9, BitSetUtils.ToLong(reg.getCurrentValue()),
				"applyInitialValue must reset the watched value too");
	}

	@Test
	void setInitialValueAloneLeavesTheDisplayedValueUntouched() {
		Register reg = newRegister();
		reg.setBits(4);
		reg.applyInitialValue(BigInteger.valueOf(9));
		reg.setInitialValue(BigInteger.valueOf(3));
		assertEquals(BigInteger.valueOf(3), reg.getInitialValue());
		assertEquals(9, BitSetUtils.ToLong(reg.getCurrentValue()),
				"setInitialValue is the dialog's staging setter and must"
						+ " not disturb the displayed value");
	}

	@Test
	void mutatorsReachTheSavedAttributes() {
		Register reg = newRegister();
		reg.setName("r1");
		reg.setBits(4);
		reg.setBase(16);
		reg.setOrientation(Orientation.DOWN);
		reg.setInitialValue(BigInteger.valueOf(5));
		assertEquals("r1", reg.getName());
		assertEquals(4, reg.getBits());
		assertEquals(16, reg.getBase());
		assertEquals(Orientation.DOWN, reg.getOrientation());
		String text = saved(reg);
		assertTrue(text.contains(" String name \"r1\""), text);
		assertTrue(text.contains(" int bits 4"), text);
		assertTrue(text.contains(" Int init 5"), text);
		assertTrue(text.contains(" String orient \"DOWN\""), text);
		assertTrue(text.contains(" String type \"latch\""), text);
	}

	@Test
	void timingContractResetsToTheDefaultDelay() {
		Register reg = newRegister();
		assertTrue(reg.hasTiming());
		assertEquals(50, reg.getDelay(), "documented default delay");
		reg.setDelay(75);
		assertEquals(75, reg.getDelay());
		reg.resetPropDelay();
		assertEquals(50, reg.getDelay());
	}

	@Test
	void capabilityContract() {
		Register reg = newRegister();
		assertFalse(reg.canCopy(), "registers are named, so not copyable");
		assertTrue(reg.canChange());
		assertTrue(reg.canWatch());
		assertFalse(reg.isWatched());
		reg.setWatched(true);
		assertTrue(reg.isWatched());
	}

	@Test
	void infoTextNamesBitsTypeAndValue() {
		Register reg = newRegister();
		reg.setBits(4);
		reg.applyInitialValue(BigInteger.valueOf(5));
		assertTrue(reg.infoText().startsWith("4 bit latch"), reg.infoText());
		reg.setTypeName("pff");
		assertTrue(reg.infoText().contains("positive edge triggered"),
				reg.infoText());
		reg.setTypeName("nff");
		assertTrue(reg.infoText().contains("negative edge triggered"),
				reg.infoText());
		assertTrue(reg.infoText().contains("value = "), reg.infoText());
	}

	@Test
	void removeUnregistersTheNameFromTheCircuit() {
		Circuit circuit = new Circuit("regmodel");
		Register reg = new Register(circuit);
		reg.setName("r1");
		assertTrue(circuit.addName("r1"));
		reg.remove(circuit);
		assertFalse(circuit.hasName("r1"),
				"remove must free the name for reuse");
	}

	// ---------------------------------------------------------------
	// geometry: rotate / flip / resize
	// ---------------------------------------------------------------

	@Test
	void unattachedRegisterRotatesQuarterTurnsAndRebuildsPuts() {
		Register reg = newRegister();
		reg.init(metrics());
		assertTrue(reg.canRotate());
		reg.rotate(Orientation.LEFT, metrics());
		assertEquals(Orientation.UP, reg.getOrientation(),
				"rotating left is a quarter-turn counterclockwise");
		assertEquals(2, reg.getInputList().size(),
				"rotation must rebuild, not accumulate, the puts");
		assertEquals(2, reg.getOutputList().size());
		reg.rotate(Orientation.RIGHT, metrics());
		assertEquals(Orientation.RIGHT, reg.getOrientation());
		reg.rotate(Orientation.RIGHT, metrics());
		assertEquals(Orientation.DOWN, reg.getOrientation());
		assertEquals(2, reg.getInputList().size());
	}

	@Test
	void unattachedRegisterFlipsToTheOppositeOrientation() {
		Register reg = newRegister();
		reg.init(metrics());
		assertTrue(reg.canFlip());
		reg.flip(metrics());
		assertEquals(Orientation.LEFT, reg.getOrientation());
		assertEquals(2, reg.getInputList().size());
		assertEquals(2, reg.getOutputList().size());
		reg.flip(metrics());
		assertEquals(Orientation.RIGHT, reg.getOrientation());
	}

	@Test
	void wiredRegisterRefusesRotationAndFlip() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int data = cb.constant(5);
		int clk = cb.constant(0);
		int reg = cb.register(4, 0, "pff");
		int q = cb.outputPin("q", 4);
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		cb.wire(reg, "Q", q, "input");
		Circuit circuit = new Circuit("regmodel");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		Register register = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof Register r) {
				register = r;
			}
		}
		assertNotNull(register);
		assertFalse(register.canRotate(),
				"attached puts must pin the orientation");
		assertFalse(register.canFlip());
	}

	@Test
	void resizeForNewNameRecomputesTheBoxFromTheFont() {
		Register reg = newRegister();
		reg.setName("r");
		reg.init(metrics());
		int narrow = reg.getWidth();
		reg.setName("a_much_longer_register_name");
		reg.resizeForNewName(metrics());
		assertTrue(reg.getWidth() > narrow,
				"the box must grow to fit the longer name");
		assertEquals(2, reg.getInputList().size(),
				"resize must rebuild, not accumulate, the puts");
		assertEquals(2, reg.getOutputList().size());
	}

	// ---------------------------------------------------------------
	// CLI-facing printing
	// ---------------------------------------------------------------

	@Test
	void printValueQualifiesTheNameOnlyWhenNested() {
		Register reg = newRegister();
		reg.setName("r1");
		reg.setBits(4);
		reg.applyInitialValue(BigInteger.valueOf(5));
		PrintStream oldOut = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			reg.printValue("");
			reg.printValue("top");
		} finally {
			System.setOut(oldOut);
		}
		String text = captured.toString(StandardCharsets.UTF_8);
		assertTrue(text.contains("Register r1: "), text);
		assertTrue(text.contains("Register top.r1: "), text);
	}

	@Test
	void showCurrentValueReportsHexUnsignedAndSignedHeadlessly() {
		Register reg = newRegister();
		reg.setBits(4);
		reg.applyInitialValue(BigInteger.valueOf(15));
		PrintStream oldErr = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			// headless TellUser.info prints instead of dialoging (#81)
			reg.showCurrentValue(0, 0);
		} finally {
			System.setErr(oldErr);
		}
		String text = captured.toString(StandardCharsets.UTF_8);
		assertTrue(text.contains("0xF"), text);
		assertTrue(text.contains("15 unsigned"), text);
		assertTrue(text.contains("-1 signed"), text);
	}

	// ---------------------------------------------------------------
	// clocking corners (through the real loader and simulator)
	// ---------------------------------------------------------------

	/** Load, simulate to quiescence, return the register's value. */
	private static long simulateRegister(String circuitText, String vectors,
			long timeLimit) throws Exception {
		Circuit circuit = new Circuit("regmodel");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(timeLimit);
		if (vectors != null) {
			java.nio.file.Path vectorFile =
					java.nio.file.Files.createTempFile("regmodel", ".txt");
			java.nio.file.Files.writeString(vectorFile, vectors);
			try {
				sim.setTestFile(vectorFile.toString());
				sim.addTestGen();
				sim.runSim();
			} finally {
				java.nio.file.Files.deleteIfExists(vectorFile);
			}
		} else {
			sim.runSim();
		}
		Register register = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof Register r) {
				register = r;
			}
		}
		assertNotNull(register);
		BitSet value = register.getCurrentValue();
		assertNotNull(value);
		return BitSetUtils.ToLong(value);
	}

	/**
	 * A BatchSimulator that counts the events the register itself posts
	 * (the CountingSimulator precedent from
	 * SimulationSemanticsRegressionTest): the final value alone cannot
	 * distinguish "later edges are no-ops" from "later edges re-post
	 * the same value", so the pin is the post count.
	 */
	private static final class RegisterPostCountingSimulator
			extends BatchSimulator {
		int registerPosts = 0;

		@Override
		public void post(jls.sim.SimEvent event) {
			// only the register's own output-driving posts (non-null
			// todo): input-change notifications also name the register
			// as callback but carry a null todo
			if (event.getCallBack() instanceof Register
					&& event.getTodo() != null) {
				registerPosts += 1;
			}
			super.post(event);
		}
	}

	@Test
	void latchIgnoresRepeatedEdgesWithUnchangedData() throws Exception {
		// a free-running clock re-presents the same data every cycle;
		// after the first capture each later edge must be a no-op, not
		// a re-post of the same value
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int data = cb.constant(5);
		int clk = cb.clock(20, 10);
		int reg = cb.register(4, 0, "latch");
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		Circuit circuit = new Circuit("regmodel");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		RegisterPostCountingSimulator sim =
				new RegisterPostCountingSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(200);
		sim.runSim();
		Register register = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof Register r) {
				register = r;
			}
		}
		assertNotNull(register);
		assertEquals(5, BitSetUtils.ToLong(register.getCurrentValue()));
		assertEquals(2, sim.registerPosts,
				"exactly the initSim seed and the first capture: the"
						+ " ~9 later rising edges with unchanged data"
						+ " must not re-post");
	}

	@Test
	void negativeEdgeRegisterHoldsWhileDataMovesUnderAHighClock()
			throws Exception {
		// clock rises at 10 and never falls; data then changes at 30.
		// a negative-edge register must ignore both events and keep its
		// initial value
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int data = cb.inputPin("data", 4);
		int clk = cb.inputPin("clk", 1);
		int reg = cb.register(4, 2, "nff");
		cb.wire(data, "output", reg, "D");
		cb.wire(clk, "output", reg, "C");
		String vectors = "clk 0 until 10 1 end\n"
				+ "data 5 until 30 9 end\n";
		assertEquals(2, simulateRegister(cb.build(), vectors, 100),
				"no negative edge ever fires, so the initial value holds");
	}
}
