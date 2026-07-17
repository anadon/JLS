package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import jls.elem.Element;

/**
 * Drift tests between docs/file-format.md (the normative save-format
 * spec, issue #79) and reality. Not a full spec-derived parser - the
 * round-trip suites already assert code-vs-code consistency - but the
 * spec's checkable claims are checked against a real save of a fixture
 * circuit that contains every element type the format can express:
 *
 * 1. the FORMAT header is the first line and its version is the one
 *    the spec documents;
 * 2. every line of a canonical save matches the spec's grammar
 *    (block keywords and the seven item kinds), and blocks nest as
 *    documented (nested subcircuit CIRCUIT blocks carry no FORMAT);
 * 3. the spec's element tag table matches exactly the set of tags a
 *    full-coverage circuit actually saves, and every documented tag
 *    resolves through the loader's routing rule
 *    (jls.elem.&lt;tag&gt;, concrete Element subclass, (Circuit)
 *    constructor);
 * 4. string escaping on disk is the documented writer transform, and
 *    it round-trips.
 */
class FileFormatSpecTest {

	private static final Path SPEC = Path.of("docs", "file-format.md");

	// ------------------------------------------------------------------
	// fixture: one instance of every savable element type
	// ------------------------------------------------------------------

	private static String el(String type, int id, int x, int y,
			String attrs) {
		return "ELEMENT " + type + "\n int id " + id + "\n int x " + x
				+ "\n int y " + y + "\n" + attrs + "END\n";
	}

	/**
	 * Every element type tag the spec documents, exercised in one
	 * circuit (an extension of AllElementsRoundTripTest's fixture with
	 * Binder, Splitter, StateMachine and SubCircuit).
	 */
	private static String fixture() {
		StringBuilder t = new StringBuilder("CIRCUIT everything\n");
		int id = 0;
		int x = 120;
		for (String g : new String[] {"AndGate", "OrGate", "NandGate",
				"NorGate", "XorGate"}) {
			t.append(el(g, id++, x += 120, 120,
					" int bits 2\n int numInputs 3\n String orientation \"up\"\n int delay 10\n"));
		}
		t.append(el("NotGate", id++, x += 120, 120,
				" int bits 1\n int numInputs 1\n String orientation \"left\"\n int delay 5\n"));
		t.append(el("DelayGate", id++, x += 120, 120,
				" int bits 1\n int numInputs 1\n String orientation \"right\"\n int delay 20\n"));
		t.append(el("Extend", id++, x += 120, 120,
				" int bits 4\n int numInputs 1\n String orientation \"right\"\n int delay 0\n"));
		t.append(el("Adder", id++, x += 120, 120,
				" int bits 4\n String orient \"UP\"\n int delay 12\n"));
		t.append(el("Clock", id++, x += 120, 120,
				" int cycle 20\n int one 10\n String orient \"DOWN\"\n"));
		t.append(el("Constant", id++, x += 120, 120,
				" Int value 255\n int base 16\n String orient \"LEFT\"\n"));
		t.append(el("Decoder", id++, x += 120, 120,
				" int bits 3\n int delay 10\n String orient \"DOWN\"\n"));
		t.append(el("Display", id++, x += 120, 120,
				" int bits 4\n int base 16\n"));
		t.append(el("InputPin", id++, x += 120, 120,
				" String name \"in1\"\n int bits 4\n int watch 0\n String orient \"RIGHT\"\n"));
		t.append(el("OutputPin", id++, x += 120, 120,
				" String name \"out1\"\n int bits 4\n int watch 1\n String orient \"LEFT\"\n"));
		t.append(el("JumpStart", id++, x += 120, 120,
				" String name \"js\"\n int bits 4\n int watch 0\n String orientation \"RIGHT\"\n"));
		t.append(el("JumpEnd", id++, x += 120, 120,
				" String name \"js\"\n int bits 4\n String orientation \"LEFT\"\n"));
		t.append(el("Memory", id++, x += 120, 360,
				" String name \"mem0\"\n String type \"ROM\"\n int bits 8\n int cap 40\n"
				+ " int time 10\n int watch 0\n String file \"\"\n String init \"0 1\\n1 255\\n\"\n"));
		t.append(el("Mux", id++, x += 120, 120,
				" int inputs 4\n int bits 2\n int delay 10\n String iOrient \"DOWN\"\n String sOrient \"LEFT\"\n"));
		t.append(el("Pause", id++, x += 120, 120, ""));
		t.append(el("Register", id++, x += 120, 120,
				" String name \"reg\"\n int bits 4\n Int init 3\n String orient \"RIGHT\"\n"
				+ " int delay 8\n String type \"nff\"\n int watch 1\n"));
		t.append(el("ShiftRegister", id++, x += 120, 120,
				" String type \"ArithmeticRight\"\n int bits 8\n int delay 25\n"
				+ " String iOrient \"DOWN\"\n String sOrient \"LEFT\"\n"));
		t.append(el("SigGen", id++, x += 120, 120,
				" int width 48\n int height 24\n String signals \"in1 0 1 0 1\\n\"\n"));
		t.append(el("Stop", id++, x += 120, 120, ""));
		t.append(el("Text", id++, x += 120, 120,
				" String text \"" + NASTY_ESCAPED + "\"\n String fn \"Dialog\"\n int fs 12\n"
				+ " int bold 1\n int ital 0\n int color -16777216\n"));
		t.append(el("TriState", id++, x += 120, 120,
				" int bits 1\n int delay 10\n String Gorient \"UP\"\n String Corient \"LEFT\"\n"));
		t.append(el("TruthTable", id++, x += 120, 360,
				" String name \"tt\"\n int delay 10\n int rows 2\n int cols 2\n"
				+ " String input \"a\"\n String output \"z\"\n"
				+ " pair 0 0\n pair 0 1\n pair 1 1\n pair 1 0\n"));
		t.append(el("Binder", id++, x += 120, 480,
				" int bits 4\n String orient \"RIGHT\"\n String noncontig \"true\"\n"
				+ " int tristate 0\n pair 0 2\n pair 0 3\n pair 1 0\n pair 1 1\n"));
		t.append(el("Splitter", id++, x += 120, 480,
				" int bits 4\n String orient \"RIGHT\"\n String noncontig \"true\"\n"
				+ " int tristate 0\n pair 0 3\n pair 1 0\n pair 1 1\n pair 1 2\n"));
		t.append(el("StateMachine", id++, x += 120, 600,
				" String name \"sm\"\n int delay 10\n"
				+ " String state \"s0\"\n  int x 50\n  int y 50\n"
				+ "  int diameter 50\n  int init 1\n"
				+ "  String output \"z\"\n   long value 1\n   int bits 1\n"
				+ "  String trans \"always\"\n   String next \"s0\"\n"));
		// a SubCircuit wrapping a trivial nested circuit
		t.append("ELEMENT SubCircuit\n")
			.append(" String orient \"RIGHT\"\n")
			.append(" int id ").append(id++).append('\n')
			.append(" int x ").append(x += 120).append('\n')
			.append(" int y 600\n int width 48\n int height 48\n")
			.append(" String name \"inner\"\n")
			.append("CIRCUIT inner\n")
			.append("ELEMENT InputPin\n")
			.append(" int id 0\n int x 60\n int y 60\n")
			.append(" int width 48\n int height 24\n")
			.append(" String name \"a\"\n int bits 1\n int watch 0\n")
			.append(" String orient \"RIGHT\"\n")
			.append("END\n")
			.append("ENDCIRCUIT\n")
			.append("END\n");
		// a real wire, so WireEnd (and a probe) reach the saved file
		int src = id++;
		t.append(el("InputPin", src, x += 120, 720,
				" String name \"wsrc\"\n int bits 1\n int watch 0\n String orient \"RIGHT\"\n"));
		int dst = id++;
		t.append(el("OutputPin", dst, x + 240, 720,
				" String name \"wdst\"\n int bits 1\n int watch 0\n String orient \"LEFT\"\n"));
		int ea = id++;
		int eb = id++;
		t.append("ELEMENT WireEnd\n int id " + ea + "\n int x " + (x + 48)
				+ "\n int y 732\n int width 8\n int height 8\n String put \"output\"\n"
				+ " ref attach " + src + "\n ref wire " + eb
				+ "\n probe " + eb + " \"p0\"\nEND\n");
		t.append("ELEMENT WireEnd\n int id " + eb + "\n int x " + (x + 240)
				+ "\n int y 732\n int width 8\n int height 8\n String put \"input\"\n"
				+ " ref attach " + dst + "\n ref wire " + ea + "\nEND\n");
		t.append("ENDCIRCUIT\n");
		return t.toString();
	}

	/** Raw nasty string exercising every escape of spec §6. */
	private static final String NASTY_RAW =
			"a\\b \"quoted\"\nline2 trailing\\";

	/** The same string as the documented writer transform emits it. */
	private static final String NASTY_ESCAPED =
			"a\\\\b \\\"quoted\\\"\\nline2 trailing\\\\";

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("everything");
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

	// ------------------------------------------------------------------
	// the spec's own claims, parsed from the document
	// ------------------------------------------------------------------

	private static String spec() throws Exception {
		assertTrue(Files.isRegularFile(SPEC), "missing " + SPEC);
		return Files.readString(SPEC, StandardCharsets.UTF_8);
	}

	/**
	 * The tag table of spec §7: markdown rows "| `Tag` | ... |". Tags
	 * are class simple names, so the leading capital distinguishes them
	 * from the (lowercase) attribute-name tables elsewhere in the spec.
	 */
	private static Set<String> documentedTags(String spec) {
		Set<String> tags = new TreeSet<String>();
		Matcher rows = Pattern.compile("(?m)^\\| `([A-Z][A-Za-z]+)` \\|")
				.matcher(spec);
		while (rows.find()) {
			tags.add(rows.group(1));
		}
		return tags;
	}

	// ------------------------------------------------------------------
	// 1 + 2: header and line grammar of a real save
	// ------------------------------------------------------------------

	/** One line of the documented grammar, canonical layout (§2, §3). */
	private static final Pattern LINE = Pattern.compile(
			"FORMAT \\d+"
			+ "|CIRCUIT [A-Za-z][A-Za-z0-9_]*"
			+ "|ENDCIRCUIT"
			+ "|ELEMENT [A-Za-z]+"
			+ "|END"
			+ "| +(?:int|long|Int|ref) [A-Za-z][A-Za-z0-9_]* -?\\d+"
			+ "| +String [A-Za-z][A-Za-z0-9_]* \".*\""
			+ "| +pair -?\\d+ -?\\d+"
			+ "| +probe -?\\d+ \".*\"");

	@Test
	void savedTextMatchesTheDocumentedGrammar() throws Exception {
		String saved = save(load(fixture()));
		String[] lines = saved.split("\n");

		// §4: the FORMAT header is the first line, stating the version
		// this JLS writes; the spec documents that same version
		assertEquals("FORMAT " + Circuit.FORMAT_VERSION, lines[0],
				"the FORMAT header must be the first line");
		assertTrue(spec().contains("**format version "
						+ Circuit.FORMAT_VERSION + "**"),
				"the spec must document the version the code writes ("
						+ Circuit.FORMAT_VERSION + ")");
		assertTrue(lines[1].startsWith("CIRCUIT "),
				"the top-level CIRCUIT line must follow the header");
		assertEquals("ENDCIRCUIT", lines[lines.length - 1],
				"the file must end with the top-level ENDCIRCUIT");

		// §2/§3: every line of a canonical save matches the grammar
		int depth = 0;
		int formatLines = 0;
		for (String line : lines) {
			assertTrue(LINE.matcher(line).matches(),
					"saved line does not match the documented grammar: '"
							+ line + "'");
			if (line.startsWith("FORMAT")) {
				formatLines += 1;
				assertEquals(0, depth,
						"FORMAT may only appear at the top of the file");
			}
			if (line.startsWith("CIRCUIT")) {
				depth += 1;
			}
			if (line.equals("ENDCIRCUIT")) {
				depth -= 1;
			}
		}
		assertEquals(0, depth, "CIRCUIT/ENDCIRCUIT must balance");
		assertEquals(1, formatLines,
				"§4: a file states its format version exactly once - "
						+ "nested subcircuit blocks carry no FORMAT line");
	}

	// ------------------------------------------------------------------
	// 3: the documented tag list is exactly reality
	// ------------------------------------------------------------------

	@Test
	void documentedTagsAreExactlyTheTagsAFullCircuitSaves()
			throws Exception {
		Set<String> saved = new TreeSet<String>();
		for (String line : save(load(fixture())).split("\n")) {
			if (line.startsWith("ELEMENT ")) {
				saved.add(line.substring("ELEMENT ".length()).trim());
			}
		}
		assertEquals(documentedTags(spec()), saved,
				"spec §7's tag table and the tags a full-coverage circuit"
						+ " really saves must match exactly (fix the spec"
						+ " or extend this fixture)");
	}

	@Test
	void everyDocumentedTagResolvesLikeTheLoader() throws Exception {
		// the loader's routing rule: jls.elem.<tag>, a concrete Element
		// subclass, constructible from a (Circuit) constructor
		List<String> broken = new ArrayList<String>();
		for (String tag : documentedTags(spec())) {
			try {
				Class<? extends Element> c = Class.forName("jls.elem." + tag)
						.asSubclass(Element.class);
				assertFalse(Modifier.isAbstract(c.getModifiers()),
						tag + " must be concrete");
				c.getConstructor(Circuit.class);
			} catch (ReflectiveOperationException | ClassCastException ex) {
				broken.add(tag + " (" + ex + ")");
			}
		}
		assertTrue(broken.isEmpty(),
				"documented tags that do not resolve through the loader's"
						+ " routing rule: " + broken);
	}

	// ------------------------------------------------------------------
	// 4: escaping is the documented transform, and it round-trips
	// ------------------------------------------------------------------

	@Test
	void stringEscapingOnDiskIsTheDocumentedTransform() throws Exception {
		String saved = save(load(fixture()));
		assertTrue(saved.contains(
						" String text \"" + NASTY_ESCAPED + "\""),
				"§6: the on-disk encoding must be the documented writer"
						+ " transform (\\ then \" then newline):\n" + saved);

		// and the documented reader scan is its exact inverse: the value
		// survives a save -> load -> save round trip byte-for-byte
		String again = save(load(saved));
		assertTrue(again.contains(" String text \"" + NASTY_ESCAPED + "\""),
				"§6: escaping must round-trip");
		// belt and braces: the raw value really contains what §6 claims
		assertTrue(NASTY_RAW.contains("\\") && NASTY_RAW.contains("\"")
						&& NASTY_RAW.contains("\n")
						&& NASTY_RAW.endsWith("\\"),
				"the fixture string must exercise every documented escape");
	}

} // end of FileFormatSpecTest class
