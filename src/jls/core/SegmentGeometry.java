package jls.core;

/**
 * Headless line-segment geometry (issue #77): the pure-integer/double math
 * that wire and state-machine hit-testing used to reach for through
 * {@code Line2D}, {@code Point2D} and
 * {@code Rectangle2D}. Each method reproduces the algorithm
 * of the corresponding AWT geometry method verbatim, so hit-testing — and
 * therefore every drag, hover and collision decision — is unchanged;
 * {@code SegmentGeometryParityTest} pins that equivalence against the AWT
 * originals.
 */
public final class SegmentGeometry {

	/**
	 * Prevents instantiation; all members are static.
	 */
	private SegmentGeometry() {
	} // no instances

	// Rectangle2D outcode bits (same values AWT uses).
	/** Outcode bit set when the point is to the left of the rectangle. */
	private static final int OUT_LEFT = 1;
	/** Outcode bit set when the point is above the rectangle. */
	private static final int OUT_TOP = 2;
	/** Outcode bit set when the point is to the right of the rectangle. */
	private static final int OUT_RIGHT = 4;
	/** Outcode bit set when the point is below the rectangle. */
	private static final int OUT_BOTTOM = 8;

	/**
	 * The distance from a point to a line segment, exactly as
	 * {@code Line2D.ptSegDist} computes it.
	 *
	 * @param x1 The segment's first endpoint x.
	 * @param y1 The segment's first endpoint y.
	 * @param x2 The segment's second endpoint x.
	 * @param y2 The segment's second endpoint y.
	 * @param px The point's x.
	 * @param py The point's y.
	 * @return the distance from the point to the segment.
	 */
	public static double ptSegDist(double x1, double y1, double x2, double y2,
			double px, double py) {
		return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
	} // end of ptSegDist method

	/**
	 * The squared distance from a point to a line segment, exactly as
	 * {@code Line2D.ptSegDistSq} computes it.
	 *
	 * @param x1 The segment's first endpoint x.
	 * @param y1 The segment's first endpoint y.
	 * @param x2 The segment's second endpoint x.
	 * @param y2 The segment's second endpoint y.
	 * @param px The point's x.
	 * @param py The point's y.
	 * @return the squared distance from the point to the segment.
	 */
	public static double ptSegDistSq(double x1, double y1, double x2,
			double y2, double px, double py) {
		// Adjust vectors relative to x1,y1.
		x2 -= x1;
		y2 -= y1;
		px -= x1;
		py -= y1;
		double dotprod = px * x2 + py * y2;
		double projlenSq;
		if (dotprod <= 0.0) {
			// px,py is on the side of x1,y1 away from x2,y2.
			projlenSq = 0.0;
		} else {
			// switch to backwards vectors relative to x2,y2.
			px = x2 - px;
			py = y2 - py;
			dotprod = px * x2 + py * y2;
			if (dotprod <= 0.0) {
				// px,py is on the side of x2,y2 away from x1,y1.
				projlenSq = 0.0;
			} else {
				// px,py is between x1,y1 and x2,y2.
				projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
			}
		}
		double lenSq = px * px + py * py - projlenSq;
		if (lenSq < 0) {
			lenSq = 0;
		}
		return lenSq;
	} // end of ptSegDistSq method

	/**
	 * The distance between two points, exactly as
	 * {@code Point2D.distance} computes it.
	 *
	 * @param x1 The first point's x.
	 * @param y1 The first point's y.
	 * @param x2 The second point's x.
	 * @param y2 The second point's y.
	 * @return the distance between the two points.
	 */
	public static double distance(double x1, double y1, double x2, double y2) {
		x2 -= x1;
		y2 -= y1;
		return Math.sqrt(x2 * x2 + y2 * y2);
	} // end of distance method

	/**
	 * Whether a line segment intersects an axis-aligned rectangle, exactly
	 * as {@code Rectangle2D.intersectsLine} computes it.
	 *
	 * @param rx The rectangle's left edge.
	 * @param ry The rectangle's top edge.
	 * @param rw The rectangle's width.
	 * @param rh The rectangle's height.
	 * @param x1 The segment's first endpoint x.
	 * @param y1 The segment's first endpoint y.
	 * @param x2 The segment's second endpoint x.
	 * @param y2 The segment's second endpoint y.
	 * @return true if the segment intersects the rectangle.
	 */
	public static boolean segmentIntersectsRectangle(double rx, double ry,
			double rw, double rh, double x1, double y1, double x2, double y2) {
		int out1;
		int out2;
		if ((out2 = outcode(rx, ry, rw, rh, x2, y2)) == 0) {
			return true;
		}
		while ((out1 = outcode(rx, ry, rw, rh, x1, y1)) != 0) {
			if ((out1 & out2) != 0) {
				return false;
			}
			if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
				double x = rx;
				if ((out1 & OUT_RIGHT) != 0) {
					x += rw;
				}
				y1 = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
				x1 = x;
			} else {
				double y = ry;
				if ((out1 & OUT_BOTTOM) != 0) {
					y += rh;
				}
				x1 = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
				y1 = y;
			}
		}
		return true;
	} // end of segmentIntersectsRectangle method

	/**
	 * The Rectangle2D outcode of a point relative to a rectangle, exactly
	 * as {@code Rectangle2D.outcode} computes it.
	 *
	 * @param rx The rectangle's left edge.
	 * @param ry The rectangle's top edge.
	 * @param rw The rectangle's width.
	 * @param rh The rectangle's height.
	 * @param x The point's x.
	 * @param y The point's y.
	 * @return the outcode bits describing the point's position relative
	 *         to the rectangle.
	 */
	private static int outcode(double rx, double ry, double rw, double rh,
			double x, double y) {
		int out = 0;
		if (rw <= 0) {
			out |= OUT_LEFT | OUT_RIGHT;
		} else if (x < rx) {
			out |= OUT_LEFT;
		} else if (x > rx + rw) {
			out |= OUT_RIGHT;
		}
		if (rh <= 0) {
			out |= OUT_TOP | OUT_BOTTOM;
		} else if (y < ry) {
			out |= OUT_TOP;
		} else if (y > ry + rh) {
			out |= OUT_BOTTOM;
		}
		return out;
	} // end of outcode method

} // end of SegmentGeometry class
