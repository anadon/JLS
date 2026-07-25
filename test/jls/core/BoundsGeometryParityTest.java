package jls.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;

/**
 * Pins that {@link Bounds} reproduces {@link java.awt.Rectangle} bug for
 * bug (issue #77). Circuit geometry — image-export size, print
 * pagination, visibility culling, drag hit-testing — is derived from
 * {@code Rectangle}'s exact behavior, including its treatment of zero-
 * and negative-size rectangles and its edge-touch conventions, so before
 * any consumer switches off {@code Rectangle} the replacement must agree
 * everywhere. The grids below deliberately include empty (zero-size),
 * non-existent (negative-size), edge-touching, and disjoint cases.
 */
class BoundsGeometryParityTest {

	private static final int[] COORDS = { -3, -1, 0, 1, 4 };
	private static final int[] SIZES = { -2, -1, 0, 1, 3, 7 };

	private static Rectangle rect(int x, int y, int w, int h) {
		return new Rectangle(x, y, w, h);
	}

	private static Bounds bounds(int x, int y, int w, int h) {
		return new Bounds(x, y, w, h);
	}

	private static String desc(int x, int y, int w, int h) {
		return "(" + x + "," + y + "," + w + "," + h + ")";
	}

	@Test
	void isEmptyAndContainsPointMatchRectangle() {
		for (int x : COORDS) {
			for (int y : COORDS) {
				for (int w : SIZES) {
					for (int h : SIZES) {
						Rectangle r = rect(x, y, w, h);
						Bounds b = bounds(x, y, w, h);
						assertEquals(r.isEmpty(), b.isEmpty(),
								"isEmpty " + desc(x, y, w, h));
						// sample points in and around the box
						for (int px = x - 2; px <= x + Math.max(w, 0) + 2; px++) {
							for (int py = y - 2;
									py <= y + Math.max(h, 0) + 2; py++) {
								assertEquals(r.contains(px, py),
										b.contains(px, py),
										"contains(" + px + "," + py + ") in "
												+ desc(x, y, w, h));
							}
						}
					}
				}
			}
		}
	}

	@Test
	void pairwiseContainsIntersectsUnionMatchRectangle() {
		// a compact grid; every ordered pair is checked
		int[] cs = { -2, 0, 3 };
		int[] ss = { -1, 0, 1, 5 };
		for (int ax : cs) for (int ay : cs) for (int aw : ss) for (int ah : ss) {
			Rectangle ra = rect(ax, ay, aw, ah);
			Bounds ba = bounds(ax, ay, aw, ah);
			for (int bx : cs) for (int by : cs) for (int bw : ss) for (int bh : ss) {
				Rectangle rb = rect(bx, by, bw, bh);
				Bounds bb = bounds(bx, by, bw, bh);
				String where = desc(ax, ay, aw, ah) + " vs "
						+ desc(bx, by, bw, bh);

				assertEquals(ra.intersects(rb), ba.intersects(bb),
						"intersects " + where);
				assertEquals(ra.contains(rb), ba.contains(bb),
						"contains " + where);

				Rectangle ru = ra.union(rb);
				Bounds bu = ba.union(bb);
				assertEquals(ru.x, bu.x(), "union.x " + where);
				assertEquals(ru.y, bu.y(), "union.y " + where);
				assertEquals(ru.width, bu.width(), "union.w " + where);
				assertEquals(ru.height, bu.height(), "union.h " + where);
			}
		}
	}

	@Test
	void includePointMatchesRectangleAdd() {
		int[] cs = { -2, 0, 3 };
		int[] ss = { -1, 0, 1, 5 };
		for (int x : cs) for (int y : cs) for (int w : ss) for (int h : ss) {
			for (int px : new int[] { -4, 0, 2, 9 }) {
				for (int py : new int[] { -4, 0, 2, 9 }) {
					Rectangle r = rect(x, y, w, h);
					r.add(px, py); // mutates r to include the point
					Bounds b = bounds(x, y, w, h).include(px, py);
					String where = desc(x, y, w, h) + " + (" + px + "," + py + ")";
					assertEquals(r.x, b.x(), "add.x " + where);
					assertEquals(r.y, b.y(), "add.y " + where);
					assertEquals(r.width, b.width(), "add.w " + where);
					assertEquals(r.height, b.height(), "add.h " + where);
				}
			}
		}
	}

	@Test
	void degenerateSentinelsAgree() {
		// the empty sentinel new Rectangle() and a deliberately degenerate
		// zero-size box (the cases the plan flags)
		assertEquals(new Rectangle().isEmpty(), new Bounds(0, 0, 0, 0).isEmpty());
		assertEquals(new Rectangle(5, 5, 0, 0).isEmpty(),
				new Bounds(5, 5, 0, 0).isEmpty());
		Rectangle r = new Rectangle(); // 0,0,0,0
		r.add(10, 20);
		Bounds b = new Bounds(0, 0, 0, 0).include(10, 20);
		assertEquals(r.x, b.x());
		assertEquals(r.y, b.y());
		assertEquals(r.width, b.width());
		assertEquals(r.height, b.height());
	}
}
