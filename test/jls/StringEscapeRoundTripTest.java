package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;

/**
 * Saved strings containing backslashes must survive a save/load
 * round-trip (issue #53, audit finding H3). The writer escapes
 * backslash, quote and newline; the loader's old sequential replace()
 * passes were not the inverse function: a literal backslash-n collapsed
 * into a real newline, and a trailing backslash lost the backslash and
 * mis-trimmed a quote.
 */
class StringEscapeRoundTripTest {

	/** Save a Text element carrying the given text and load it back. */
	private static String roundTrip(String text) throws Exception {
		Circuit circuit = new Circuit("esc");
		String escaped = text.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n");
		String file = "CIRCUIT esc\n"
				+ "ELEMENT Text\n"
				+ " int id 0\n int x 60\n int y 60\n int width 24\n int height 24\n"
				+ " String text \"" + escaped + "\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n";
		assertTrue(circuit.load(new Scanner(file)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		Element el = circuit.getElements().iterator().next();
		el.setID(0);
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			el.save(writer);
		}
		String saved = out.toString().replace(System.lineSeparator(), "\n");
		// the writer escapes raw newlines, so the whole value is one line
		String marker = " String text \"";
		for (String line : saved.split("\n")) {
			if (line.startsWith(marker) && line.endsWith("\"")) {
				return line.substring(marker.length(), line.length() - 1);
			}
		}
		throw new AssertionError("no text line in:\n" + saved);
	}

	private static void assertRoundTrips(String text) throws Exception {
		String escaped = text.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n");
		assertEquals(escaped, roundTrip(text),
				"text must re-save byte-identically: " + text);
	}

	@Test
	void literalBackslashNSurvives() throws Exception {
		assertRoundTrips("a\\nb");
	}

	@Test
	void trailingBackslashSurvives() throws Exception {
		assertRoundTrips("C:\\");
	}

	@Test
	void embeddedQuotesSurvive() throws Exception {
		assertRoundTrips("say \"hi\"");
	}

	@Test
	void quoteAfterBackslashSurvives() throws Exception {
		assertRoundTrips("say \"hi\"\\");
	}

	@Test
	void realNewlineSurvives() throws Exception {
		assertRoundTrips("line1\nline2");
	}

	@Test
	void backslashRunsSurvive() throws Exception {
		assertRoundTrips("\\\\\\");
		assertRoundTrips("\\n\\\\n\\\\\\n");
	}

	@Test
	void randomizedBackslashHeavyStringsSurvive() throws Exception {
		char[] alphabet = { 'a', '\\', '"', '\n', 'n' };
		Random random = new Random(7);
		for (int i = 0; i < 200; i++) {
			StringBuilder sb = new StringBuilder();
			int len = 1 + random.nextInt(12);
			for (int j = 0; j < len; j++) {
				sb.append(alphabet[random.nextInt(alphabet.length)]);
			}
			assertRoundTrips(sb.toString());
		}
	}
}
