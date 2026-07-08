package jls.elem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * A legacy-format Display carrying "int orient 0" (Top) must re-save
 * with its marker intact (issue #57, audit finding M14): the omitted
 * predicate used to test orient <= 0, conflating the -1 "no marker"
 * sentinel with the valid legacy value 0, so one save silently
 * converted a working legacy file into one that fails to load.
 */
class DisplayLegacyOrientTest {

	private static String displayFixture(String orientLine) {
		return "CIRCUIT disp\n"
				+ "ELEMENT Display\n"
				+ " int id 0\n int x 60\n int y 60\n int width 32\n int height 24\n"
				+ " int bits 1\n int base 10\n"
				+ orientLine
				+ "END\n"
				+ "ENDCIRCUIT\n";
	}

	private static String loadAndSaveElement(String fixture) {
		Circuit circuit = new Circuit("disp");
		assertTrue(circuit.load(new Scanner(fixture)),
				() -> "load: " + JLSInfo.loadError);
		try {
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad: " + JLSInfo.loadError);
		} catch (Exception ex) {
			throw new AssertionError("finishLoad threw", ex);
		}
		Element display = circuit.getElements().iterator().next();
		display.setID(0);
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			display.save(writer);
		}
		return out.toString().replace(System.lineSeparator(), "\n");
	}

	@Test
	void legacyOrientZeroSurvivesResave() {
		String saved = loadAndSaveElement(displayFixture(" int orient 0\n"));
		assertTrue(saved.contains(" int orient 0\n"),
				"orient 0 (Top) is a valid legacy marker and must be re-saved:\n"
						+ saved);
	}

	@Test
	void allLegacyOrientValuesSurviveResave() {
		for (int orient = 0; orient <= 3; orient++) {
			String saved = loadAndSaveElement(
					displayFixture(" int orient " + orient + "\n"));
			assertTrue(saved.contains(" int orient " + orient + "\n"),
					"legacy orient " + orient + " must be re-saved:\n" + saved);
		}
	}

	@Test
	void newFormatDisplayStillOmitsTheMarker() {
		String saved = loadAndSaveElement(displayFixture(""));
		assertFalse(saved.contains("orient"),
				"a new-format Display (sentinel -1) must not save a marker:\n"
						+ saved);
	}
}
