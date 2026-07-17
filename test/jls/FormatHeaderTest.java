package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * The FORMAT save-format version header (issue #79). Current saves begin
 * with a "FORMAT 1" line ahead of the top-level CIRCUIT line; headerless
 * legacy files are implicitly version 0 and must keep loading unchanged;
 * a version newer than the reader fails with an explicit "this file
 * needs a newer JLS" error (#58 taxonomy) instead of a misparse; and a
 * file states its version exactly once - nested subcircuit CIRCUIT
 * blocks never repeat the header.
 */
class FormatHeaderTest {

	/** A minimal single-element legacy (headerless) circuit text. */
	private static final String LEGACY_TEXT =
			"CIRCUIT legacy\n"
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
			+ "ENDCIRCUIT\n";

	/** A circuit containing a SubCircuit with a nested CIRCUIT block. */
	private static final String NESTED_TEXT =
			"CIRCUIT main\n"
			+ "ELEMENT SubCircuit\n"
			+ " String orient \"RIGHT\"\n"
			+ " int id 0\n"
			+ " int x 120\n"
			+ " int y 120\n"
			+ " int width 0\n"
			+ " int height 0\n"
			+ "CIRCUIT inner\n"
			+ "ELEMENT InputPin\n"
			+ " int id 0\n"
			+ " int x 60\n"
			+ " int y 60\n"
			+ " String name \"a\"\n"
			+ " int bits 1\n"
			+ " int watch 0\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ENDCIRCUIT\n"
			+ "END\n"
			+ "ENDCIRCUIT\n";

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		BufferedImage img =
				new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		boolean ok = circuit.finishLoad(g);
		g.dispose();
		assertTrue(ok, () -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	private static LoadError failToLoad(String text) {
		Circuit circuit = new Circuit("");
		assertFalse(circuit.load(new Scanner(text)),
				"load should have failed");
		LoadError error = JLSInfo.lastLoadError;
		assertNotNull(error, "a failed load must set the structured error");
		return error;
	}

	@Test
	void newSavesBeginWithTheFormatHeader() throws Exception {
		// the writer emits the lowest version whose features the file
		// uses (#124); this fixture uses none past version 1
		String saved = save(load(LEGACY_TEXT));
		assertTrue(saved.startsWith("FORMAT 1\nCIRCUIT "),
				"a save must start with the FORMAT header, got:\n" + saved);
	}

	@Test
	void headerlessLegacyTextStillLoads() throws Exception {
		Circuit circuit = load(LEGACY_TEXT);
		assertEquals(1, circuit.getElements().size());
		assertEquals("legacy", circuit.getName());
	}

	@Test
	void headeredTextLoads() throws Exception {
		// every version up to the newest this JLS implements is readable
		for (int version = 1; version <= Circuit.FORMAT_VERSION; version++) {
			Circuit circuit = load("FORMAT " + version + "\n" + LEGACY_TEXT);
			assertEquals(1, circuit.getElements().size());
		}
	}

	@Test
	void newerFormatVersionFailsAsNeedsNewerJls() {
		for (String version : new String[] {
				Integer.toString(Circuit.FORMAT_VERSION + 1),
				"999", "999999999999"}) {
			LoadError error =
					failToLoad("FORMAT " + version + "\n" + LEGACY_TEXT);
			assertSame(LoadError.Category.NEWER_FORMAT, error.getCategory(),
					"FORMAT " + version + " must be refused as too new, "
							+ "got: " + error);
			assertTrue(error.render().contains("newer version of JLS"),
					"the error must say the file needs a newer JLS: "
							+ error.render());
			assertTrue(error.render().contains(version),
					"the error must name the offending version: "
							+ error.render());
		}
	}

	@Test
	void malformedFormatVersionFailsAsMalformed() {
		LoadError error = failToLoad("FORMAT banana\n" + LEGACY_TEXT);
		assertSame(LoadError.Category.MALFORMED, error.getCategory(),
				"a non-numeric FORMAT version must be malformed, got: "
						+ error);
		assertTrue(error.render().contains("banana"),
				"the error must quote the bad version: " + error.render());
	}

	@Test
	void truncatedFormatHeaderFailsAsMalformed() {
		LoadError error = failToLoad("FORMAT");
		assertSame(LoadError.Category.MALFORMED, error.getCategory(),
				"a FORMAT header with no version must be malformed, got: "
						+ error);
	}

	@Test
	void savedFileWithSubcircuitHasExactlyOneFormatLine() throws Exception {
		String saved = save(load(NESTED_TEXT));
		int formatLines = 0;
		int circuitLines = 0;
		for (String line : saved.split("\n")) {
			if (line.startsWith("FORMAT ")) {
				formatLines += 1;
			}
			if (line.startsWith("CIRCUIT ")) {
				circuitLines += 1;
			}
		}
		assertEquals(2, circuitLines,
				"the fixture must really save a nested CIRCUIT block:\n"
						+ saved);
		assertEquals(1, formatLines,
				"a file states its format version exactly once, at the "
						+ "top:\n" + saved);
		assertTrue(saved.startsWith("FORMAT 1\n"),
				"the one FORMAT line must be the first line:\n" + saved);
	}

	@Test
	void saveLoadSaveIsByteIdenticalIncludingHeader() throws Exception {
		// a single-element circuit, so save order is deterministic
		String savedOnce = save(load(LEGACY_TEXT));
		String savedTwice = save(load(savedOnce));
		assertEquals(savedOnce, savedTwice,
				"save -> load -> save must be byte-identical, header "
						+ "included");
	}
}
