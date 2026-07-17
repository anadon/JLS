package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Vertical (UP/DOWN) orientation for wire groups - Binder and Splitter
 * (issue #124). Groups were the last element family restricted to
 * LEFT/RIGHT: the loader ignored vertical orient values, there was no
 * rotate operation, and the size computation assumed a horizontal
 * layout. These tests pin, headlessly against model state:
 *
 * <ul>
 * <li>P1: vertical orient values load, and rotate cycles a group
 *     through all four orientations (the geometry uses the across/along
 *     decomposition, both axes grid-snapped);</li>
 * <li>P2: horizontal files are untouched - they round-trip to the same
 *     bytes and keep declaring save-format version 1;</li>
 * <li>P3: a vertical group round-trips to the same bytes, and the file
 *     declares save-format version 2 so that older readers refuse it
 *     cleanly instead of silently loading the group horizontal
 *     (docs/file-format.md section 9 evolution policy).</li>
 * </ul>
 *
 * Geometry note: a group's across size is metrics-driven (widest range
 * label) but grid-snapped; the labels here are single digits, so the
 * expected sizes hold for any font whose digit advance is under one
 * grid unit (12px), the same stability argument as the orientation
 * geometry baseline.
 */
class GroupOrientationTest {

	private static final int S = JLSInfo.spacing;

	/** One bundler/unbundler of 2 single bits at the given orientation. */
	private static String groupText(String type, String orient) {
		return "CIRCUIT grp\n"
				+ "ELEMENT " + type + "\n"
				+ " int id 0\n int x 240\n int y 240\n"
				+ " int bits 2\n"
				+ " String orient \"" + orient + "\"\n"
				+ " String noncontig \"true\"\n"
				+ " int tristate 0\n"
				+ " pair 0 0\n pair 1 1\n"
				+ "END\n"
				+ "ENDCIRCUIT\n";
	}

	/** Load a circuit and finish it with a real (image) graphics. */
	private static Circuit load(String text) {
		Circuit circuit = new Circuit("grp");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		BufferedImage img =
				new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try {
			assertTrue(circuit.finishLoad(g),
					() -> "finishLoad failed: " + JLSInfo.loadError);
		} catch (Exception ex) {
			throw new AssertionError("finishLoad threw", ex);
		} finally {
			g.dispose();
		}
		return circuit;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString().replace(System.lineSeparator(), "\n");
	}

	private static Group group(Circuit circuit) {
		for (Element el : circuit.getElements()) {
			if (el instanceof Group) {
				return (Group) el;
			}
		}
		throw new AssertionError("no group in circuit");
	}

	/** The group's puts as sorted "name@relX,relY" strings. */
	private static List<String> puts(Element el) {
		List<String> puts = new ArrayList<>();
		for (Put p : el.getAllPuts()) {
			puts.add(p.getName() + "@" + (p.getX() - el.getX()) + ","
					+ (p.getY() - el.getY()));
		}
		java.util.Collections.sort(puts);
		return puts;
	}

	// ------------------------------------------------------------------
	// P1: vertical orientations load with vertical geometry
	// ------------------------------------------------------------------

	@Test
	void verticalSplitterLoadsWithVerticalGeometry() {
		Group up = group(load(groupText("Splitter", "UP")));
		// along the horizontal axis: one grid step per put plus one;
		// across the vertical axis: the snapped label width
		assertEquals(3 * S, up.getRect().width, "width must be the along size");
		assertEquals(2 * S, up.getRect().height, "height must be the across size");
		assertEquals(List.of("0@12,0", "1@24,0", "input@12,24"), puts(up),
				"UP splitter: outputs across the top, input at the bottom");

		Group down = group(load(groupText("Splitter", "DOWN")));
		assertEquals(List.of("0@12,24", "1@24,24", "input@12,0"), puts(down),
				"DOWN splitter: outputs across the bottom, input at the top");
	}

	@Test
	void verticalBinderLoadsWithVerticalGeometry() {
		Group up = group(load(groupText("Binder", "UP")));
		assertEquals(3 * S, up.getRect().width);
		assertEquals(2 * S, up.getRect().height);
		assertEquals(List.of("0@12,24", "1@24,24", "output@12,0"), puts(up),
				"UP binder: inputs across the bottom, output at the top");

		Group down = group(load(groupText("Binder", "DOWN")));
		assertEquals(List.of("0@12,0", "1@24,0", "output@12,24"), puts(down),
				"DOWN binder: inputs across the top, output at the bottom");
	}

	@Test
	void horizontalGeometryIsUnchanged() {
		// the pre-#124 layout, pinned so the across/along rewrite cannot
		// move existing puts (wires attach to these coordinates)
		Group right = group(load(groupText("Binder", "RIGHT")));
		assertEquals(2 * S, right.getRect().width);
		assertEquals(3 * S, right.getRect().height);
		assertEquals(List.of("0@0,12", "1@0,24", "output@24,12"), puts(right));

		Group left = group(load(groupText("Splitter", "LEFT")));
		assertEquals(List.of("0@0,12", "1@0,24", "input@24,12"), puts(left));
	}

	// ------------------------------------------------------------------
	// P1: rotate cycles the four orientations
	// ------------------------------------------------------------------

	/** The saved orient value of the (re-inited) group. */
	private static String orientOf(Group group) {
		group.setID(0);
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			group.save(writer);
		}
		String saved = out.toString();
		int at = saved.indexOf("String orient \"");
		assertTrue(at >= 0, "no orient in save:\n" + saved);
		int from = at + "String orient \"".length();
		return saved.substring(from, saved.indexOf('"', from));
	}

	@Test
	void rotateCyclesAllFourOrientations() {
		Circuit circuit = load(groupText("Binder", "RIGHT"));
		Group group = group(circuit);
		String savedBefore = save(circuit);
		assertTrue(group.canRotate(),
				"an unattached group must be rotatable");

		BufferedImage img =
				new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try {
			List<String> seen = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				group.rotate(JLSInfo.Orientation.RIGHT, g);
				seen.add(orientOf(group));
			}
			assertEquals(List.of("DOWN", "LEFT", "UP", "RIGHT"), seen,
					"clockwise rotation must cycle all four orientations");
			assertEquals(savedBefore, save(circuit),
					"four quarter-turns must restore the identical save");

			group.rotate(JLSInfo.Orientation.LEFT, g);
			assertEquals("UP", orientOf(group),
					"counterclockwise from RIGHT is UP");
		} finally {
			g.dispose();
		}
	}

	@Test
	void flipTogglesVerticalOrientations() {
		Circuit circuit = load(groupText("Splitter", "UP"));
		Group group = group(circuit);
		assertTrue(group.canFlip());

		BufferedImage img =
				new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try {
			// Splitter.flip re-runs init itself
			group.flip(g);
			assertEquals("DOWN", orientOf(group));
			group.flip(g);
			assertEquals("UP", orientOf(group));
		} finally {
			g.dispose();
		}
	}

	@Test
	void attachedGroupRefusesRotateAndFlip() {
		StringBuilder text = new StringBuilder("CIRCUIT grp\n");
		text.append("ELEMENT Binder\n int id 0\n int x 240\n int y 240\n")
				.append(" int bits 2\n String orient \"RIGHT\"\n")
				.append(" String noncontig \"true\"\n int tristate 0\n")
				.append(" pair 0 0\n pair 1 1\nEND\n");
		text.append("ELEMENT OutputPin\n int id 1\n int x 360\n int y 240\n")
				.append(" String name \"out\"\n int bits 2\n int watch 0\n")
				.append(" String orient \"RIGHT\"\nEND\n");
		text.append("ELEMENT WireEnd\n int id 2\n int x 264\n int y 252\n")
				.append(" String put \"output\"\n ref attach 0\n ref wire 3\nEND\n");
		text.append("ELEMENT WireEnd\n int id 3\n int x 360\n int y 252\n")
				.append(" String put \"input\"\n ref attach 1\n ref wire 2\nEND\n");
		text.append("ENDCIRCUIT\n");

		Group group = group(load(text.toString()));
		assertFalse(group.canRotate(),
				"a group with an attached wire must not rotate");
		assertFalse(group.canFlip(),
				"a group with an attached wire must not flip");
	}

	// ------------------------------------------------------------------
	// P2/P3: persistence
	// ------------------------------------------------------------------

	@Test
	void verticalGroupRoundTripsAndDeclaresFormat2() {
		String savedOnce = save(load(groupText("Splitter", "UP")));
		assertTrue(savedOnce.startsWith("FORMAT 2\n"),
				"a vertical group is a version-2 feature; older readers "
						+ "must refuse the file, not load it horizontal:\n"
						+ savedOnce);
		assertTrue(savedOnce.contains(" String orient \"UP\"\n"),
				"the vertical orientation must be saved:\n" + savedOnce);
		assertEquals(savedOnce, save(load(savedOnce)),
				"save -> load -> save must be a fixed point");
	}

	@Test
	void horizontalGroupRoundTripsAndKeepsFormat1() {
		String savedOnce = save(load(groupText("Binder", "LEFT")));
		assertTrue(savedOnce.startsWith("FORMAT 1\n"),
				"a horizontal-groups-only file must stay readable by "
						+ "older JLS versions:\n" + savedOnce);
		assertTrue(savedOnce.contains(" String orient \"LEFT\"\n"));
		assertEquals(savedOnce, save(load(savedOnce)),
				"save -> load -> save must be a fixed point");
	}

	@Test
	void verticalGroupInsideASubcircuitBumpsTheFileHeader() {
		// a file states its format version once, at the top (#79), so a
		// version-2 feature inside a nested block must surface there
		StringBuilder text = new StringBuilder("CIRCUIT outer\n");
		text.append("ELEMENT SubCircuit\n String orient \"RIGHT\"\n")
				.append(" int id 0\n int x 120\n int y 120\n")
				.append(" int width 48\n int height 48\n")
				.append(" String name \"inner\"\n")
				.append("CIRCUIT inner\n")
				.append("ELEMENT Binder\n int id 0\n int x 240\n int y 240\n")
				.append(" int bits 2\n String orient \"UP\"\n")
				.append(" String noncontig \"true\"\n int tristate 0\n")
				.append(" pair 0 0\n pair 1 1\nEND\n")
				.append("ENDCIRCUIT\n")
				.append("END\n")
				.append("ENDCIRCUIT\n");
		String savedOnce = save(load(text.toString()));
		assertTrue(savedOnce.startsWith("FORMAT 2\n"),
				"a nested vertical group must bump the top-level header:\n"
						+ savedOnce);
		assertEquals(1, savedOnce.split("FORMAT ", -1).length - 1,
				"the FORMAT header must still appear exactly once:\n"
						+ savedOnce);
		assertEquals(savedOnce, save(load(savedOnce)),
				"save -> load -> save must be a fixed point");
	}

	@Test
	void unknownOrientValueStillFallsBackToTheDefault() {
		// the historical loader ignored unknown orient strings; that
		// permissiveness must survive the four-orientation extension
		Group group = group(load(groupText("Binder", "BANANA")));
		assertEquals("RIGHT", orientOf(group));
	}
}
