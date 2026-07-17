package jls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct tests of the SVG branch of Circuit.exportImage (issue #154).
 * The CLI-level behavior rides in CliImageExportTest; this pins the
 * properties that matter for reproducible-build and golden-test use:
 * exporting the same circuit twice yields byte-identical SVG, and the
 * document is structurally an SVG image with the circuit's drawn
 * content in it. Deliberately no full-document golden - text layout
 * coordinates depend on the JDK's font metrics, which differ across
 * machines (the same reason CliImageExportTest avoids pixel goldens).
 */
class SvgExportTest {

	@TempDir
	Path tmp;

	private static final String CIRCUIT_TEXT = "CIRCUIT svgexport\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n int x 60\n int y 60\n"
			+ " int width 24\n int height 24\n"
			+ " Int value 1\n int base 10\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ELEMENT InputPin\n"
			+ " int id 2\n int x 120\n int y 240\n"
			+ " int width 48\n int height 24\n"
			+ " String name \"src\"\n int bits 1\n int watch 0\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ELEMENT OutputPin\n"
			+ " int id 1\n int x 480\n int y 120\n"
			+ " int width 48\n int height 24\n"
			+ " String name \"out\"\n int bits 1\n int watch 0\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ELEMENT WireEnd\n"
			+ " int id 4\n int x 168\n int y 252\n"
			+ " int width 8\n int height 8\n"
			+ " String put \"output\"\n ref attach 2\n ref wire 5\n"
			+ "END\n"
			+ "ELEMENT WireEnd\n"
			+ " int id 5\n int x 480\n int y 132\n"
			+ " int width 8\n int height 8\n"
			+ " String put \"input\"\n ref attach 1\n ref wire 4\n"
			+ "END\n"
			+ "ENDCIRCUIT\n";

	private static Circuit load() throws Exception {
		Circuit circuit = new Circuit("svgexport");
		assertTrue(circuit.load(new Scanner(CIRCUIT_TEXT)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void exportingTwiceIsByteIdentical() throws Exception {
		Circuit circuit = load();
		Path first = tmp.resolve("first.svg");
		Path second = tmp.resolve("second.svg");
		circuit.exportImage(first.toString());
		circuit.exportImage(second.toString());
		assertArrayEquals(Files.readAllBytes(first),
				Files.readAllBytes(second),
				"two exports of the same circuit must be byte-identical");

		// fresh loads of the same text must also export the same bytes,
		// so goldens and reproducible-builds expectations hold. The
		// element set is a HashSet whose identity-hash iteration order
		// varies across load instances (issue #166); the export path
		// must not let that order reach the SVG bytes. Several loads
		// raise the odds of actually getting a different order.
		for (int i = 0; i < 4; i++) {
			Path fresh = tmp.resolve("fresh" + i + ".svg");
			load().exportImage(fresh.toString());
			assertArrayEquals(Files.readAllBytes(first),
					Files.readAllBytes(fresh),
					"a fresh load of the same circuit must export the"
					+ " same bytes");
		}
	}

	@Test
	void theDocumentIsAnSvgImageWithDrawnContent() throws Exception {
		Circuit circuit = load();
		Path out = tmp.resolve("out.svg");
		circuit.exportImage(out.toString());
		String svg = Files.readString(out, StandardCharsets.UTF_8);
		assertTrue(svg.startsWith("<?xml"), "missing XML prolog");
		assertTrue(svg.contains("<svg"), "no <svg element");
		assertTrue(svg.contains("</svg>"), "unterminated document");
		// the output pin draws its name as text; its presence proves
		// the element paint path ran against the SVG canvas
		assertTrue(svg.contains("out"),
				"the pin label should appear in the SVG text");
	}
}
