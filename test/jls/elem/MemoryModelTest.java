package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import jls.sim.BatchSimulator;

/**
 * Headless tests for the Memory model (issue #159, following the
 * TruthTableModelTest pattern). The #77 extraction already moved the
 * dialogs out, so the dialog-side mutators, the init-text validator
 * with its four error messages, the file-vs-built-in initialization
 * fallbacks, the out-of-range read/write guards, the write-activity
 * trace, and the changed-locations report can all be driven and
 * observed without any GUI. Reads and writes go through the real
 * loader and BatchSimulator, never a hand-posted event.
 */
class MemoryModelTest {

	/** A fresh memory on a throwaway circuit. */
	private static Memory newMemory() {
		return new Memory(new Circuit("memmodel"));
	}

	/** The element's on-disk text, the observable model state. */
	private static String saved(Memory mem) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			mem.save(writer);
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

	/** Load circuit text, run the batch simulator, return the memory. */
	private static Memory simulate(String circuitText) {
		Circuit circuit = new Circuit("memmodel");
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
		return findMemory(circuit);
	}

	private static Memory findMemory(Circuit circuit) {
		Memory memory = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof Memory m) {
				memory = m;
			}
		}
		assertNotNull(memory);
		return memory;
	}

	/**
	 * A RAM circuit with the control lines driven by constants: CS and
	 * WE and OE are active low.
	 */
	private static String ramCircuit(int bits, int capacity, String init,
			long addr, long data, long cs, long we, long oe) {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int ram = cb.memory("RAM", bits, capacity, init);
		int a = cb.constant(addr);
		int d = cb.constant(data);
		int select = cb.constant(cs);
		int write = cb.constant(we);
		int enable = cb.constant(oe);
		cb.wire(a, "output", ram, "address");
		cb.wire(d, "output", ram, "input");
		cb.wire(select, "output", ram, "CS");
		cb.wire(write, "output", ram, "WE");
		cb.wire(enable, "output", ram, "OE");
		return cb.build();
	}

	// ---------------------------------------------------------------
	// dialog-side mutators
	// ---------------------------------------------------------------

	@Test
	void mutatorsReachTheSavedAttributes() {
		Memory mem = newMemory();
		mem.setName("m1");
		mem.setType(false);
		mem.setBits(8);
		mem.setCapacity(16);
		mem.setInitialValue("0 5\n");
		mem.setFileName("image.dat");
		assertEquals("m1", mem.getName());
		assertFalse(mem.isRAM());
		assertEquals(8, mem.getBits());
		assertEquals(16, mem.getCapacity());
		assertEquals("0 5\n", mem.getInitialValue());
		assertEquals("image.dat", mem.getFileName());
		String text = saved(mem);
		assertTrue(text.contains(" String name \"m1\""), text);
		assertTrue(text.contains(" String type \"ROM\""), text);
		assertTrue(text.contains(" int bits 8"), text);
		assertTrue(text.contains(" int cap 16"), text);
		assertTrue(text.contains(" String file \"image.dat\""), text);
		mem.setType(true);
		assertTrue(mem.isRAM());
	}

	@Test
	void setMemFileClearsTheBuiltInInitialValue() {
		Memory mem = newMemory();
		mem.setInitialValue("0 5\n");
		mem.setMemFile("image.dat");
		assertEquals("image.dat", mem.getFileName());
		assertEquals("", mem.getInitialValue(),
				"a file initialization replaces the built-in text");
	}

	@Test
	void specsNameCapacityAndKindAndRequireInit() {
		Memory mem = newMemory();
		assertThrows(IllegalStateException.class, mem::getSpecs,
				"specs are computed by init");
		mem.setBits(8);
		mem.setCapacity(4);
		mem.init(null);
		assertEquals(" 4x8 RAM ", mem.getSpecs());
		assertTrue(mem.infoText().contains("built-in initializaion"),
				mem.infoText());
		Memory rom = newMemory();
		rom.setType(false);
		rom.setBits(8);
		rom.setCapacity(4);
		rom.setFileName("image.dat");
		rom.init(null);
		assertEquals(" 4x8 ROM ", rom.getSpecs());
		assertTrue(rom.infoText().contains("initialization file"),
				rom.infoText());
	}

	@Test
	void reinitRecomputesTheBoxForTheNewName() {
		Memory mem = newMemory();
		mem.setName("m");
		mem.init(metrics());
		int narrow = mem.getWidth();
		mem.setName("a_much_longer_memory_name");
		mem.reinit(metrics());
		assertTrue(mem.getWidth() > narrow,
				"the box must grow to fit the longer name");
	}

	@Test
	void timingContractResetsToTheDefaultAccessTime() {
		Memory mem = newMemory();
		assertTrue(mem.hasTiming());
		assertTrue(mem.usesAccessTime(),
				"memory is the one element with an access time");
		assertEquals(100, mem.getDelay(), "documented default access time");
		mem.setDelay(60);
		assertEquals(60, mem.getDelay());
		mem.resetPropDelay();
		assertEquals(100, mem.getDelay());
	}

	@Test
	void watchAndChangeContract() {
		Memory mem = newMemory();
		assertTrue(mem.canWatch());
		assertTrue(mem.canChange());
		assertFalse(mem.isWatched());
		mem.setWatched(true);
		assertTrue(mem.isWatched());
	}

	@Test
	void removeUnregistersTheNameFromTheCircuit() {
		Circuit circuit = new Circuit("memmodel");
		Memory mem = new Memory(circuit);
		mem.setName("m1");
		assertTrue(circuit.addName("m1"));
		mem.remove(circuit);
		assertFalse(circuit.hasName("m1"),
				"remove must free the name for reuse");
	}

	// ---------------------------------------------------------------
	// init-text validation
	// ---------------------------------------------------------------

	@Test
	void initOKReportsEachSyntaxError() {
		Memory mem = newMemory();
		assertNull(mem.initOK("# comment\n\n0 5\n", 4, 8, false),
				"comments and blank lines are ignored");
		String badAddr = mem.initOK("zz 5\n", 4, 8, false);
		assertNotNull(badAddr);
		assertTrue(badAddr.contains("missing or invalid address"), badAddr);
		String outOfRange = mem.initOK("ff 5\n", 4, 8, false);
		assertNotNull(outOfRange);
		assertTrue(outOfRange.contains("invalid address"), outOfRange);
		String noValue = mem.initOK("0\n", 4, 8, false);
		assertNotNull(noValue);
		assertTrue(noValue.contains("missing or invalid value"), noValue);
		String tooWide = mem.initOK("0 ff\n", 4, 4, false);
		assertNotNull(tooWide);
		assertTrue(tooWide.contains("more bits than word size"), tooWide);
		String lineNumbered = mem.initOK("0 1\nzz 5\n", 4, 8, false);
		assertNotNull(lineNumbered);
		assertTrue(lineNumbered.startsWith("line 2:"), lineNumbered);
	}

	@Test
	void initOKRefusesToStoreBeforeSimulationSetup() {
		Memory mem = newMemory();
		assertThrows(IllegalStateException.class,
				() -> mem.initOK("0 1\n", 4, 8, true),
				"storing requires the initSim-created store");
	}

	// ---------------------------------------------------------------
	// RLE encoding corners (MemoryInitEncodingTest has the main paths)
	// ---------------------------------------------------------------

	@Test
	void rleEncoderRefusesNonHexTokensAndBlankText() {
		assertNull(Memory.encodeInitRLE("g 1\n"),
				"a non-hex address must fall back to the raw encoding");
		assertNull(Memory.encodeInitRLE("\n\n"),
				"text with no pairs at all must stay raw");
	}

	@Test
	void rleDecoderSkipsEmptyTokensFromRepeatedSpaces() {
		assertEquals("0 5\n1 5\n", Memory.decodeInitRLE("0:5  1:5"));
	}

	// ---------------------------------------------------------------
	// simulation: bounds, stores, activity
	// ---------------------------------------------------------------

	@Test
	void readPastCapacityTurnsTheOutputOff() {
		// capacity 3 gives a 2-bit address bus, so address 3 is
		// representable but out of range: the read must tri-state the
		// output instead of faulting
		Memory mem = simulate(ramCircuit(8, 3, "0 5", 3, 0, 0, 1, 0));
		assertNull(mem.getCurrentValue(),
				"an out-of-range read must not drive the output");
	}

	@Test
	void writePastCapacityStoresNothing() {
		Memory mem = simulate(ramCircuit(8, 3, "", 3, 7, 0, 0, 1));
		assertTrue(mem.storedAddresses().isEmpty(),
				"an out-of-range write must be dropped");
		assertEquals("", mem.getActivityTrace(),
				"a dropped write must not enter the activity history");
	}

	@Test
	void inRangeWriteIsStoredTracedAndListed() {
		Memory mem = simulate(ramCircuit(8, 4, "", 2, 7, 0, 0, 1));
		BitSet stored = mem.getCurrentValue(2);
		assertNotNull(stored);
		assertEquals(7, BitSetUtils.ToLong(stored));
		assertEquals(java.util.Set.of(2), mem.storedAddresses(),
				"the dense store must list exactly the written address");
		String trace = mem.getActivityTrace();
		assertTrue(trace.contains("0x7 written to location 0x2 at time "),
				trace);
	}

	@Test
	void wordsWiderThan64BitsUseTheSparseStore() {
		// 65-bit words force the sparse (map) store; a ROM read must
		// round-trip a value that cannot fit one long
		BigInteger wide = new BigInteger("1ffffffffffffffff", 16);
		Memory mem = simulate(buildRom(65, 4, "0 1ffffffffffffffff", 0));
		BitSet stored = mem.getCurrentValue(0);
		assertNotNull(stored);
		assertEquals(BitSetUtils.Create(wide), stored);
		assertEquals(java.util.Set.of(0), mem.storedAddresses());
		BitSet current = mem.getCurrentValue();
		assertNotNull(current, "the read must drive the output");
		assertEquals(BitSetUtils.Create(wide), current);
	}

	/** A ROM wired for a read of the given address. */
	private static String buildRom(int bits, int capacity, String init,
			long addr) {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int rom = cb.memory("ROM", bits, capacity, init);
		int a = cb.constant(addr);
		int select = cb.constant(0);
		int enable = cb.constant(0);
		cb.wire(a, "output", rom, "address");
		cb.wire(select, "output", rom, "CS");
		cb.wire(enable, "output", rom, "OE");
		return cb.build();
	}

	@Test
	void currentValueAndStoredAddressesBeforeSimulationAreEmpty() {
		Memory mem = newMemory();
		assertNull(mem.getCurrentValue(0),
				"no store exists before initSim");
		assertTrue(mem.storedAddresses().isEmpty(),
				"storedAddresses must create an empty store, not fault");
	}

	// ---------------------------------------------------------------
	// initialization fallbacks (headless messages, zeros assumed)
	// ---------------------------------------------------------------

	@Test
	void missingInitializationFileFallsBackToZeros() {
		Circuit circuit = new Circuit("memmodel");
		assertTrue(circuit.load(new Scanner(
				buildRom(8, 4, "", 1))),
				() -> "load failed: " + JLSInfo.loadError);
		try {
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad failed: " + JLSInfo.loadError);
		} catch (Exception e) {
			throw new AssertionError("finishLoad threw", e);
		}
		Memory mem = findMemory(circuit);
		mem.setMemFile("no_such_directory/no_such_file.dat");
		String out = captureAllOutput(() -> {
			BatchSimulator sim = new BatchSimulator();
			sim.setCircuit(circuit);
			sim.setTimeLimit(1_000_000);
			sim.runSim();
		});
		assertTrue(out.contains("cannot be read, all zeros assumed"), out);
		BitSet current = mem.getCurrentValue();
		assertNotNull(current, "the read must still drive the output");
		assertEquals(0, BitSetUtils.ToLong(current));
	}

	@Test
	void initializationFileContentsAreParsedAndServed() throws Exception {
		java.nio.file.Path image =
				java.nio.file.Files.createTempFile("memmodel", ".dat");
		try {
			java.nio.file.Files.writeString(image, "0 5\n1 9\n");
			Circuit circuit = new Circuit("memmodel");
			assertTrue(circuit.load(new Scanner(buildRom(8, 4, "", 1))),
					() -> "load failed: " + JLSInfo.loadError);
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad failed: " + JLSInfo.loadError);
			Memory mem = findMemory(circuit);
			mem.setMemFile(image.toString());
			BatchSimulator sim = new BatchSimulator();
			sim.setCircuit(circuit);
			sim.setTimeLimit(1_000_000);
			sim.runSim();
			BitSet current = mem.getCurrentValue();
			assertNotNull(current);
			assertEquals(9, BitSetUtils.ToLong(current));
		} finally {
			java.nio.file.Files.deleteIfExists(image);
		}
	}

	@Test
	void invalidBuiltInInitFallsBackToZeros() {
		String out = captureAllOutput(() -> {
			Memory mem = simulate(buildRom(8, 4, "zz", 1));
			BitSet current = mem.getCurrentValue();
			assertNotNull(current);
			assertEquals(0, BitSetUtils.ToLong(current));
		});
		assertTrue(out.contains("all zeros assumed"), out);
	}

	// ---------------------------------------------------------------
	// changed-locations report
	// ---------------------------------------------------------------

	@Test
	void printChangedValuesReportsWritesAgainstTheInitialImage() {
		// address 2 changes 5 -> 7; the initialized-but-unwritten
		// address 0 must not be reported
		Memory mem = simulate(ramCircuit(8, 4, "0 3\\n2 5", 2, 7, 0, 0, 1));
		String out = captureStdout(() -> mem.printChangedValues(""));
		assertTrue(out.contains("Changed locations in memory mem0"), out);
		assertTrue(out.contains("0x2:"), out);
		assertFalse(out.contains("0x0:"), out);
		String qualified = captureStdout(() -> mem.printChangedValues("top"));
		assertTrue(qualified.contains("in memory top.mem0"), qualified);
	}

	@Test
	void printChangedValuesReportsNoChangesForAnEchoedWrite() {
		// writing the value a location already held is not a change
		Memory mem = simulate(ramCircuit(8, 4, "2 7", 2, 7, 0, 0, 1));
		String out = captureStdout(() -> mem.printChangedValues(""));
		assertTrue(out.contains("No changes in memory mem0"), out);
		String qualified = captureStdout(() -> mem.printChangedValues("top"));
		assertTrue(qualified.contains("No changes in memory top.mem0"),
				qualified);
	}

	@Test
	void printChangedValuesSaysNothingForROM() {
		Memory mem = simulate(buildRom(8, 4, "0 5", 0));
		assertEquals("", captureStdout(() -> mem.printChangedValues("")));
	}

	@Test
	void printChangedValuesRequiresASimulation() {
		Memory mem = newMemory();
		assertThrows(IllegalStateException.class,
				() -> mem.printChangedValues(""));
	}

	/** Run an action with stdout captured, returning what it printed. */
	private static String captureStdout(Runnable action) {
		PrintStream oldOut = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			action.run();
		} finally {
			System.setOut(oldOut);
		}
		return captured.toString(StandardCharsets.UTF_8);
	}

	/**
	 * Run an action with stdout and stderr captured together: the
	 * zeros-assumed diagnostics go to stdout in batch mode but through
	 * TellUser (stderr) otherwise, and these tests only care that the
	 * user was told.
	 */
	private static String captureAllOutput(Runnable action) {
		PrintStream oldOut = System.out;
		PrintStream oldErr = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			PrintStream both = new PrintStream(captured, true,
					StandardCharsets.UTF_8);
			System.setOut(both);
			System.setErr(both);
			action.run();
		} finally {
			System.setOut(oldOut);
			System.setErr(oldErr);
		}
		return captured.toString(StandardCharsets.UTF_8);
	}
}
