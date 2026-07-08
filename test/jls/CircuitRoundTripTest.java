package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Save/load round-trip tests (issue #14). A known circuit is loaded,
 * saved, reloaded, and saved again; the two saved forms must be
 * identical (the save/load pair is a fixed point). The same circuit is
 * also pushed through each on-disk container format (text, zip, xz)
 * via the real format-sniffing loader.
 */
class CircuitRoundTripTest {

	@TempDir
	Path tmp;

	/**
	 * A minimal but representative circuit: two constants feeding nothing,
	 * exercising int, Int (BigInteger), and quoted-String attributes.
	 */
	private static final String CIRCUIT_TEXT =
			"CIRCUIT testcircuit\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n"
			+ " int x 60\n"
			+ " int y 60\n"
			+ " int width 24\n"
			+ " int height 24\n"
			+ " Int value 5\n"
			+ " int base 10\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ELEMENT Constant\n"
			+ " int id 1\n"
			+ " int x 120\n"
			+ " int y 96\n"
			+ " int width 24\n"
			+ " int height 24\n"
			+ " Int value 255\n"
			+ " int base 16\n"
			+ " String orient \"LEFT\"\n"
			+ "END\n"
			+ "ENDCIRCUIT\n";

	private static Circuit load(Scanner scanner) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(scanner), () -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null), () -> "finishLoad failed: " + JLSInfo.loadError);
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
	void saveLoadIsAFixedPoint() throws Exception {
		Circuit first = load(new Scanner(CIRCUIT_TEXT));
		assertEquals(2, first.getElements().size());

		String savedOnce = save(first);
		Circuit second = load(new Scanner(savedOnce));
		String savedTwice = save(second);

		// Elements live in a HashSet, so block order and the ids assigned
		// at save time are not stable across load instances; compare the
		// order-independent, id-free canonical form instead.
		assertEquals(canonicalize(savedOnce), canonicalize(savedTwice),
				"saving a reloaded circuit must reproduce the same content");
		assertEquals(canonicalize(CIRCUIT_TEXT), canonicalize(savedOnce),
				"a loaded circuit must save back to its original content");
	}

	/**
	 * Reduce a saved circuit file to a canonical form: ELEMENT..END blocks
	 * sorted, save-order-dependent id lines removed.
	 */
	private static String canonicalize(String saved) {
		List<String> blocks = new ArrayList<>();
		StringBuilder current = null;
		StringBuilder header = new StringBuilder();
		for (String line : saved.split("\n")) {
			if (line.startsWith("FORMAT ")) {
				// the version header (issue #79) is byte-pinned by
				// FormatHeaderTest; this suite compares circuit content
				continue;
			}
			if (line.startsWith("ELEMENT")) {
				current = new StringBuilder();
			}
			if (current == null) {
				header.append(line).append('\n');
			} else if (!line.startsWith(" int id ")) {
				current.append(line).append('\n');
			}
			if (line.equals("END") && current != null) {
				blocks.add(current.toString());
				current = null;
			}
		}
		Collections.sort(blocks);
		return header + String.join("", blocks);
	}

	@Test
	void loadsFromPlainTextFile() throws Exception {
		File file = tmp.resolve("textcircuit.jls").toFile();
		FileFormatSupport.writeText(file, CIRCUIT_TEXT);
		assertLoadsViaSniffer(file);
	}

	@Test
	void loadsFromZipFile() throws Exception {
		File file = tmp.resolve("zipcircuit.jls").toFile();
		FileFormatSupport.writeZip(file, CIRCUIT_TEXT);
		assertLoadsViaSniffer(file);
	}

	@Test
	void loadsFromXZFile() throws Exception {
		File file = tmp.resolve("xzcircuit.jls").toFile();
		FileFormatSupport.writeXZ(file, CIRCUIT_TEXT);
		assertLoadsViaSniffer(file);
	}

	private void assertLoadsViaSniffer(File file) throws Exception {
		Scanner scanner = FileFormatSupport.openWithFormatSniffer(file);
		assertNotNull(scanner, "format sniffer failed to open " + file.getName());
		Circuit circuit = load(scanner);
		assertEquals(2, circuit.getElements().size());
	}
}
