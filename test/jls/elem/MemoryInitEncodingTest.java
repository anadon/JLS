package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.BitSetUtils;
import jls.Circuit;
import jls.JLSInfo;
import jls.sim.BatchSimulator;

/**
 * Run-length encoding of Memory initial contents in saved files (#21).
 * Canonical "addr value" dumps save as a compact initrle attribute; text
 * with comments or hand formatting keeps the raw init attribute so it
 * round-trips exactly; the loader accepts both encodings, and a memory
 * loaded from initrle simulates identically.
 */
class MemoryInitEncodingTest {

	// --- encode/decode unit level ---

	@Test
	void encodesRunsCompactly() {
		// 16 consecutive words of ff starting at 0, then a lone word
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < 16; i += 1) {
			text.append(Integer.toHexString(i)).append(" ff\n");
		}
		text.append("20 1a2b\n");
		String rle = Memory.encodeInitRLE(text.toString());
		assertEquals("0:ff:10 20:1a2b", rle);
		assertEquals(text.toString(), Memory.decodeInitRLE(rle));
	}

	@Test
	void toleratesMissingFinalNewline() {
		String rle = Memory.encodeInitRLE("0 5\n1 9");
		assertEquals("0:5 1:9", rle);
		assertEquals("0 5\n1 9\n", Memory.decodeInitRLE(rle));
	}

	@Test
	void refusesNonCanonicalText() {
		assertNull(Memory.encodeInitRLE(""), "empty stays raw");
		assertNull(Memory.encodeInitRLE("# boot image\n0 ff\n"), "comments stay raw");
		assertNull(Memory.encodeInitRLE("0 FF\n"), "uppercase hex stays raw");
		assertNull(Memory.encodeInitRLE("0 0ff\n"), "leading zeros stay raw");
		assertNull(Memory.encodeInitRLE("0  ff\n"), "extra spacing stays raw");
		assertNull(Memory.encodeInitRLE("1 5\n0 9\n"), "descending addresses stay raw");
		assertNull(Memory.encodeInitRLE("0 5\n0 9\n"), "duplicate addresses stay raw");
		assertNull(Memory.encodeInitRLE("0 ff junk\n"), "trailing tokens stay raw");
	}

	@Test
	void decodeRejectsGarbage() {
		assertThrows(IllegalArgumentException.class, () -> Memory.decodeInitRLE("0"));
		assertThrows(IllegalArgumentException.class, () -> Memory.decodeInitRLE("0:zz"));
		assertThrows(IllegalArgumentException.class, () -> Memory.decodeInitRLE("0:1:0"));
		assertThrows(IllegalArgumentException.class, () -> Memory.decodeInitRLE("a:b:c:d"));
	}

	@Test
	void decodeRejectsHostileRuns() {
		// a ~2^31 run used to expand into a StringBuilder (issue #38)
		assertThrows(IllegalArgumentException.class,
				() -> Memory.decodeInitRLE("0:0:7fffffff"));
		// addr + run int overflow
		assertThrows(IllegalArgumentException.class,
				() -> Memory.decodeInitRLE("7fffffff:0:2"));
		// explicit capacity bound
		assertThrows(IllegalArgumentException.class,
				() -> Memory.decodeInitRLE("4:1", 4));
	}

	@Test
	void outOfCapacityAddressesStayRaw() {
		// the raw init form is loaded leniently (zeros assumed at
		// simulation start), but the RLE decoder rejects addresses at
		// or past the capacity - so encoding them would save a file
		// the loader refuses to read back. Found by the generative
		// fuzzer (issue #160).
		assertNull(Memory.encodeInitRLE("22 3\n", 32),
				"an address past the capacity bound must stay raw");
		assertNotNull(Memory.encodeInitRLE("1f 3\n", 32),
				"the last in-capacity address must still encode");
	}

	@Test
	void bigValuesSurvive() {
		String text = "0 ffffffffffffffffffff\n"; // wider than a long
		String rle = Memory.encodeInitRLE(text);
		assertNotNull(rle);
		assertEquals(text, Memory.decodeInitRLE(rle));
	}

	// --- circuit level ---

	private static String memoryCircuit(String initAttr, String encoded) {
		return "CIRCUIT memtest\n"
				+ "ELEMENT Memory\n"
				+ " int id 0\n int x 240\n int y 240\n int width 96\n int height 96\n"
				+ " String name \"mem0\"\n String type \"ROM\"\n"
				+ " int bits 8\n int cap 40\n int time 10\n int watch 0\n"
				+ " String file \"\"\n"
				+ " String " + initAttr + " \"" + encoded + "\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n";
	}

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("memtest");
		assertTrue(circuit.load(new Scanner(text)), () -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null), () -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	@Test
	void dumpSavesAsRleAndIsAFixedPoint() throws Exception {
		// legacy raw file containing a canonical dump (a long zero run)
		StringBuilder init = new StringBuilder();
		for (int i = 0; i < 32; i += 1) {
			init.append(Integer.toHexString(i)).append(" 0\\n");
		}
		Circuit circuit = load(memoryCircuit("init", init.toString()));

		String savedOnce = save(circuit);
		assertTrue(savedOnce.contains(" String initrle \"0:0:20\""),
				"canonical dump should re-save run-length encoded, got:\n" + savedOnce);

		// and the new encoding is a save/load fixed point
		Circuit reloaded = load(savedOnce);
		assertEquals(savedOnce, save(reloaded));
	}

	@Test
	void commentedTextKeepsRawEncodingExactly() throws Exception {
		String raw = "# boot image\\n0 ff\\n";
		Circuit circuit = load(memoryCircuit("init", raw));
		String saved = save(circuit);
		assertTrue(saved.contains(" String init \"" + raw + "\""),
				"commented text must stay in the raw encoding, got:\n" + saved);
	}

	@Test
	void rleMemorySimulatesLikeRawMemory() throws Exception {
		// same ROM golden as BatchSimulationGoldenTest.romReadsInitialContents,
		// but with the contents supplied through the new encoding
		String circuitText = "CIRCUIT golden\n"
				+ "ELEMENT Memory\n"
				+ " int id 0\n int x 240\n int y 240\n int width 96\n int height 96\n"
				+ " String name \"mem0\"\n String type \"ROM\"\n"
				+ " int bits 8\n int cap 4\n int time 10\n int watch 0\n"
				+ " String file \"\"\n"
				+ " String initrle \"0:5 1:9\"\n"
				+ "END\n"
				+ "ELEMENT Constant\n int id 1\n int x 60\n int y 60\n int width 24\n int height 24\n"
				+ " Int value 1\n int base 10\n String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT Constant\n int id 2\n int x 60\n int y 108\n int width 24\n int height 24\n"
				+ " Int value 0\n int base 10\n String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT Constant\n int id 3\n int x 60\n int y 156\n int width 24\n int height 24\n"
				+ " Int value 0\n int base 10\n String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 4\n int x 480\n int y 120\n"
				+ " String name \"out\"\n int bits 8\n int watch 1\n String orient \"RIGHT\"\nEND\n"
				+ wireEnds(5, 1, "output", 0, "address")
				+ wireEnds(7, 2, "output", 0, "CS")
				+ wireEnds(9, 3, "output", 0, "OE")
				+ wireEnds(11, 0, "output", 4, "input")
				+ "ENDCIRCUIT\n";

		Circuit circuit = load(circuitText);
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();

		OutputPin pin = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin) {
				pin = (OutputPin) el;
			}
		}
		assertNotNull(pin);
		BitSet value = pin.getCurrentValue();
		assertNotNull(value, "output pin never settled");
		assertEquals(9, BitSetUtils.ToLong(value),
				"ROM loaded from initrle must read the same word as raw init");
	}

	private static String wireEnds(int firstId, int fromEl, String fromPut,
			int toEl, String toPut) {
		return "ELEMENT WireEnd\n int id " + firstId + "\n int x " + (12 * firstId)
				+ "\n int y 240\n int width 8\n int height 8\n"
				+ " String put \"" + fromPut + "\"\n"
				+ " ref attach " + fromEl + "\n ref wire " + (firstId + 1) + "\nEND\n"
				+ "ELEMENT WireEnd\n int id " + (firstId + 1) + "\n int x " + (12 * firstId + 6)
				+ "\n int y 240\n int width 8\n int height 8\n"
				+ " String put \"" + toPut + "\"\n"
				+ " ref attach " + toEl + "\n ref wire " + firstId + "\nEND\n";
	}
}
