package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Orientation geometry baseline (issue #24): pins the exact bounding box
 * and put positions of orientation-aware elements at every orientation
 * (and orientation pair, for the two-level TriState and Mux). This is the
 * control instrument for replacing per-orientation if/else geometry
 * ladders with one canonical geometry plus a grid transform: a conversion
 * is correct only if this baseline is unchanged, because put positions are
 * what wires attach to and what saved files record.
 *
 * Covered elements are the ones whose geometry does not depend on font
 * metrics, so the baseline is stable across JDKs and platforms. Pixel
 * comparison (P1 of the issue) stays with the manual image-export
 * protocol; this pins P2 (persisted geometry parity) headlessly.
 *
 * Regenerate (after an INTENDED geometry change only) by deleting
 * test/resources/orientation-geometry.txt and running this test once; it
 * writes the new baseline and fails, then passes on the next run.
 */
class OrientationGeometryTest {

	private static final Path BASELINE =
			Path.of("test", "resources", "orientation-geometry.txt");

	private static final String[] ORIENTS = {"LEFT", "RIGHT", "UP", "DOWN"};
	private static final String[] GATE_ORIENTS = {"left", "right", "up", "down"};

	private static String elementBlock(String type, String attrs) {
		return "CIRCUIT geo\nELEMENT " + type + "\n int id 0\n int x 240\n int y 240\n"
				+ attrs + "END\nENDCIRCUIT\n";
	}

	/**
	 * The failure category, not the full message: the baseline must not
	 * break when diagnostic wording improves (issues #58/#24 P5).
	 */
	private static String errorCategory() {
		return JLSInfo.lastLoadError == null ? JLSInfo.loadError
				: JLSInfo.lastLoadError.getCategory().label();
	}

	/** Load one element headlessly and describe its geometry. */
	private static String describe(String label, String type, String attrs) {
		Circuit circuit = new Circuit("geo");
		try {
			if (!circuit.load(new Scanner(elementBlock(type, attrs)))) {
				return label + " loadError=" + errorCategory();
			}
			BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			boolean finished = circuit.finishLoad(g);
			g.dispose();
			if (!finished) {
				return label + " finishLoadError=" + errorCategory();
			}
		} catch (Exception ex) {
			return label + " exception=" + ex.getClass().getSimpleName();
		}
		Element el = null;
		for (Element e : circuit.getElements()) {
			if (!(e instanceof Wire) && !(e instanceof WireEnd)) {
				el = e;
			}
		}
		if (el == null) {
			return label + " missing";
		}
		StringBuilder line = new StringBuilder(label);
		java.awt.Rectangle r = el.getRect();
		line.append(" rect=").append(r.x - el.getX()).append(',')
			.append(r.y - el.getY()).append(',').append(r.width).append(',').append(r.height);
		List<String> puts = new ArrayList<>();
		for (Put p : el.getAllPuts()) {
			puts.add(p.getName() + "@" + (p.getX() - el.getX()) + "," + (p.getY() - el.getY()));
		}
		java.util.Collections.sort(puts);
		line.append(" puts=").append(String.join(";", puts));
		return line.toString();
	}

	private static String buildAll() {
		List<String> lines = new ArrayList<>();

		for (String o : GATE_ORIENTS) {
			lines.add(describe("AndGate " + o, "AndGate",
					" int bits 1\n int numInputs 2\n String orientation \"" + o + "\"\n int delay 10\n"));
			lines.add(describe("NotGate " + o, "NotGate",
					" int bits 1\n int numInputs 1\n String orientation \"" + o + "\"\n int delay 5\n"));
		}
		for (String o : ORIENTS) {
			lines.add(describe("Adder " + o, "Adder",
					" int bits 4\n String orient \"" + o + "\"\n int delay 10\n"));
			lines.add(describe("Clock " + o, "Clock",
					" int cycle 20\n int one 10\n String orient \"" + o + "\"\n"));
		}
		// two-level orientation: the hardest cases (#24)
		for (String go : ORIENTS) {
			for (String co : ORIENTS) {
				lines.add(describe("TriState G=" + go + " C=" + co, "TriState",
						" int bits 1\n int delay 10\n String Gorient \"" + go
						+ "\"\n String Corient \"" + co + "\"\n"));
				lines.add(describe("Mux I=" + go + " S=" + co, "Mux",
						" int inputs 4\n int bits 1\n int delay 10\n String iOrient \"" + go
						+ "\"\n String sOrient \"" + co + "\"\n"));
				lines.add(describe("ShiftRegister I=" + go + " S=" + co, "ShiftRegister",
						" String type \"LogicalLeft\"\n int bits 8\n int delay 25\n"
						+ " String iOrient \"" + go + "\"\n String sOrient \"" + co + "\"\n"));
			}
		}
		// wire groups gained UP/DOWN in #124; their across size is
		// metrics-driven but grid-snapped, and the labels here are
		// single digits, so the entries are stable for any font whose
		// digit advance is under one grid unit
		for (String o : ORIENTS) {
			String groupAttrs = " int bits 2\n String orient \"" + o
					+ "\"\n String noncontig \"true\"\n int tristate 0\n"
					+ " pair 0 0\n pair 1 1\n";
			lines.add(describe("Binder " + o, "Binder", groupAttrs));
			lines.add(describe("Splitter " + o, "Splitter", groupAttrs));
		}
		return String.join("\n", lines) + "\n";
	}

	@Test
	void geometryMatchesBaseline() throws IOException {
		String actual = buildAll();
		if (!Files.exists(BASELINE)) {
			Files.createDirectories(BASELINE.getParent());
			Files.writeString(BASELINE, actual, StandardCharsets.UTF_8);
			assertTrue(false, "baseline was missing; wrote " + BASELINE
					+ " - review and commit it, then re-run");
		}
		assertEquals(Files.readString(BASELINE, StandardCharsets.UTF_8), actual,
				"orientation geometry diverged from the committed baseline; if the"
				+ " change is intended (it should not be for #24 conversions),"
				+ " delete the baseline file and re-run to regenerate");
	}
}
