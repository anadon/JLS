package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;

import jls.JLSInfo;
import jls.elem.Element;

/**
 * Layer-1 (headless, model-level) geometry assertions over
 * {@link Element} positions and bounding boxes: absolute grid location,
 * relative location (left-of / above / within N grid units), and
 * dimensions. See {@link jls.ui the package javadoc} for the layering
 * plan (screen-space assertions under a viewport transform arrive with
 * issue #74; Swing interaction and rendering are Layers 2 and 3).
 *
 * <p>All coordinates are model units: one grid unit is
 * {@link JLSInfo#spacing} model units, and snapped elements sit at
 * multiples of it. Relative assertions compare bounding boxes
 * ({@link Element#getRect()}), so "left of" means the whole box of A is
 * left of the whole box of B, not just its origin.</p>
 */
public final class GeometryAssert {

	private GeometryAssert() {
	}

	// ------------------------------------------------------------------
	// absolute location
	// ------------------------------------------------------------------

	/** Assert the element's upper-left corner is exactly (x, y) in model units. */
	public static void assertElementAt(Element el, int x, int y) {
		assertEquals(x + "," + y, el.getX() + "," + el.getY(),
				"position of " + CircuitAssert.describe(el));
	}

	/**
	 * Assert the element sits on the snap-to grid: both coordinates are
	 * multiples of {@link JLSInfo#spacing}.
	 */
	public static void assertOnGrid(Element el) {
		int s = JLSInfo.spacing;
		assertTrue(el.getX() % s == 0 && el.getY() % s == 0,
				CircuitAssert.describe(el) + " is off the " + s
						+ "-unit snap grid");
	}

	// ------------------------------------------------------------------
	// dimensions
	// ------------------------------------------------------------------

	/**
	 * Assert the element's bounding box ({@link Element#getRect()}) has
	 * the given width and height. Note this is the box the editor uses
	 * for hit-testing and drawing, which some elements pad beyond their
	 * declared snap footprint (e.g. a horizontal Constant adds half a
	 * grid spacing above and below for its value label).
	 */
	public static void assertDimensions(Element el, int width, int height) {
		Rectangle rect = el.getRect();
		assertEquals(width + "x" + height, rect.width + "x" + rect.height,
				"dimensions of " + CircuitAssert.describe(el));
	}

	// ------------------------------------------------------------------
	// relative location
	// ------------------------------------------------------------------

	/** Assert a's bounding box lies entirely left of b's (no x overlap). */
	public static void assertLeftOf(Element a, Element b) {
		Rectangle ra = a.getRect();
		Rectangle rb = b.getRect();
		assertTrue(ra.x + ra.width <= rb.x,
				CircuitAssert.describe(a) + " (right edge "
						+ (ra.x + ra.width) + ") is not left of "
						+ CircuitAssert.describe(b) + " (left edge " + rb.x
						+ ")");
	}

	/** Assert a's bounding box lies entirely above b's (no y overlap). */
	public static void assertAbove(Element a, Element b) {
		Rectangle ra = a.getRect();
		Rectangle rb = b.getRect();
		assertTrue(ra.y + ra.height <= rb.y,
				CircuitAssert.describe(a) + " (bottom edge "
						+ (ra.y + ra.height) + ") is not above "
						+ CircuitAssert.describe(b) + " (top edge " + rb.y
						+ ")");
	}

	/**
	 * Assert the gap between the two bounding boxes is at most
	 * {@code units} grid units on both axes (overlap counts as zero gap).
	 */
	public static void assertWithinGridUnits(Element a, Element b, int units) {
		int gap = Math.max(axisGap(a.getRect().x, a.getRect().width,
						b.getRect().x, b.getRect().width),
				axisGap(a.getRect().y, a.getRect().height, b.getRect().y,
						b.getRect().height));
		assertTrue(gap <= units * JLSInfo.spacing,
				CircuitAssert.describe(a) + " and " + CircuitAssert.describe(b)
						+ " are " + gap + " model units apart, more than "
						+ units + " grid units (" + units * JLSInfo.spacing
						+ ")");
	}

	/** Gap between intervals [aStart, aStart+aLen] and [bStart, bStart+bLen]. */
	private static int axisGap(int aStart, int aLen, int bStart, int bLen) {
		return Math.max(0,
				Math.max(bStart - (aStart + aLen), aStart - (bStart + bLen)));
	}
}
