package jls;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.elem.Element;

/**
 * Parity tests for index-driven draw culling (issues #3, #17): repaints
 * enumerate clip candidates through the spatial index instead of scanning
 * every element, so a dirty-region repaint during a drag costs O(visible),
 * not O(circuit). The contract: for any clip, the index query (clip padded
 * by the draw margin) plus the exact mayBeVisible predicate accepts exactly
 * the elements a full scan would, so the pixels drawn are unchanged.
 */
class DrawCullingParityTest {

	/** Deterministic seed: failures must reproduce. */
	private static final long SEED = 20260717L;

	/** Mirrors Circuit's draw margin (labels drawn outside bounds). */
	private static final int MARGIN = 8 * JLSInfo.spacing;

	/** Mirrors Circuit.mayBeVisible: the exact culling predicate. */
	private static boolean mayBeVisible(Element el, Rectangle clip) {

		Rectangle b = el.getIndexBounds();
		b.grow(MARGIN, MARGIN);
		return b.intersects(clip);
	}

	/**
	 * For any clip, filtering index candidates (clip grown by the margin)
	 * with the exact predicate must select the same elements as filtering
	 * the whole circuit -- including after drag-style moves keep the index
	 * current incrementally.
	 */
	@Test
	void culledCandidatesMatchFullScan() throws Exception {

		Circuit circuit = SpatialIndexTest.loadWired();
		Random random = new Random(SEED);

		for (int round = 0; round < 10; round += 1) {
			for (int i = 0; i < 200; i += 1) {
				Rectangle clip = new Rectangle(random.nextInt(700) - 50,
						random.nextInt(400) - 50,
						random.nextInt(300) + 1, random.nextInt(300) + 1);

				Set<Element> expected = new HashSet<Element>();
				for (Element el : circuit.getElements()) {
					if (mayBeVisible(el, clip)) {
						expected.add(el);
					}
				}

				Rectangle query = new Rectangle(clip);
				query.grow(MARGIN, MARGIN);
				Set<Element> actual = new HashSet<Element>();
				for (Element el : circuit.elementsNear(query)) {
					if (mayBeVisible(el, clip)) {
						actual.add(el);
					}
				}

				assertEquals(expected, actual,
						"culling must match full scan for clip " + clip);
			}

			// move a subset the way a drag does and re-check
			Set<Element> moved = new HashSet<Element>();
			for (Element el : circuit.getElements()) {
				if (random.nextBoolean()) {
					el.move(JLSInfo.spacing * (random.nextInt(5) - 2),
							JLSInfo.spacing * (random.nextInt(5) - 2));
					moved.add(el);
				}
			}
			circuit.reindexAfterMove(moved);
		}
	}

	/**
	 * End-to-end pixel parity: drawing the circuit through many small
	 * clipped draws (as dirty-region repaints do) must produce the same
	 * image as one full-canvas draw. The circuit is a grid of constants
	 * whose visuals do not overlap, so draw order within a layer cannot
	 * affect pixels.
	 */
	@Test
	void tiledClippedDrawsMatchFullDraw() throws Exception {

		StringBuilder text = new StringBuilder("CIRCUIT grid\n");
		int id = 0;
		for (int gx = 0; gx < 8; gx += 1) {
			for (int gy = 0; gy < 6; gy += 1) {
				text.append("ELEMENT Constant\n int id ").append(id++)
					.append("\n int x ").append(180 + gx * 48)
					.append("\n int y ").append(180 + gy * 48)
					.append("\n int width 24\n int height 24\n Int value 1\n int base 10\n")
					.append(" String orient \"RIGHT\"\nEND\n");
			}
		}
		text.append("ENDCIRCUIT\n");
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text.toString())));
		assertTrue(circuit.finishLoad(null));

		int width = 720;
		int height = 600;
		BufferedImage full = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = full.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		circuit.draw(g, new HashSet<Element>(), null);
		g.dispose();

		BufferedImage tiled = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		g = tiled.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		int tile = 100;
		for (int tx = 0; tx < width; tx += tile) {
			for (int ty = 0; ty < height; ty += tile) {
				g.setClip(tx, ty, tile, tile);
				circuit.draw(g, new HashSet<Element>(), null);
			}
		}
		g.dispose();

		assertArrayEquals(
				full.getRGB(0, 0, width, height, null, 0, width),
				tiled.getRGB(0, 0, width, height, null, 0, width),
				"tiled clipped draws must reproduce the full-canvas image");
	}
}
