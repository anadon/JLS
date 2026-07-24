package jls.core;

/**
 * An axis-aligned integer rectangle (issue #77): the headless value type
 * that replaces {@code Rectangle} in element and circuit
 * geometry (bounding boxes, hit testing, cull rectangles), so that
 * geometry can be computed without an AWT dependency.
 *
 * <p><b>Bug-for-bug parity.</b> Circuit geometry — image-export
 * dimensions, print pagination, visibility culling, drag hit-testing —
 * is derived from {@code Rectangle}'s exact behavior, including its
 * treatment of zero- and negative-size ("non-existent") rectangles and
 * its edge-touch conventions. Every method here reproduces the algorithm
 * of the corresponding {@code Rectangle} method verbatim, and
 * {@code BoundsGeometryParityTest} pins that equivalence across empty,
 * degenerate, edge-touching, and negative cases. The mutating
 * {@code Rectangle} operations ({@code add}) are rendered as pure methods
 * that return a new {@code Bounds}, matching {@code Rectangle}'s
 * {@code union}/{@code add} <i>results</i>.
 *
 * @param x the left edge.
 * @param y the top edge.
 * @param width the width (may be zero or negative for a non-existent box).
 * @param height the height (may be zero or negative for a non-existent box).
 */
public record Bounds(int x, int y, int width, int height) {

	/**
	 * Whether this rectangle is empty — has no interior. Matches
	 * {@code Rectangle.isEmpty()}.
	 *
	 * @return true if the width or height is not positive.
	 */
	public boolean isEmpty() {
		return (width <= 0) || (height <= 0);
	} // end of isEmpty method

	/**
	 * Whether this rectangle contains the given point. Matches
	 * {@code Rectangle.contains(int,int)}.
	 *
	 * @param px the point's x.
	 * @param py the point's y.
	 * @return true if the point lies inside this rectangle.
	 */
	public boolean contains(int px, int py) {
		int w = this.width;
		int h = this.height;
		if ((w | h) < 0) {
			return false;
		}
		int rx = this.x;
		int ry = this.y;
		if (px < rx || py < ry) {
			return false;
		}
		w += rx;
		h += ry;
		return ((w < rx || w > px) && (h < ry || h > py));
	} // end of contains method

	/**
	 * Whether this rectangle wholly contains the other. Matches
	 * {@code Rectangle.contains(Rectangle)}.
	 *
	 * @param b the other rectangle.
	 * @return true if the other rectangle lies inside this one.
	 */
	public boolean contains(Bounds b) {
		int w = this.width;
		int h = this.height;
		int bw = b.width;
		int bh = b.height;
		if ((w | h | bw | bh) < 0) {
			return false;
		}
		int rx = this.x;
		int ry = this.y;
		int bx = b.x;
		int by = b.y;
		if (bx < rx || by < ry) {
			return false;
		}
		w += rx;
		bw += bx;
		if (bw <= bx) {
			if (w >= rx || bw > w) {
				return false;
			}
		} else {
			if (w >= rx && bw > w) {
				return false;
			}
		}
		h += ry;
		bh += by;
		if (bh <= by) {
			if (h >= ry || bh > h) {
				return false;
			}
		} else {
			if (h >= ry && bh > h) {
				return false;
			}
		}
		return true;
	} // end of contains method

	/**
	 * Whether this rectangle intersects the other. Matches
	 * {@code Rectangle.intersects(Rectangle)}.
	 *
	 * @param b the other rectangle.
	 * @return true if the two rectangles overlap.
	 */
	public boolean intersects(Bounds b) {
		int tw = this.width;
		int th = this.height;
		int rw = b.width;
		int rh = b.height;
		if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
			return false;
		}
		int tx = this.x;
		int ty = this.y;
		int rx = b.x;
		int ry = b.y;
		rw += rx;
		rh += ry;
		tw += tx;
		th += ty;
		return ((rw < rx || rw > tx)
				&& (rh < ry || rh > ty)
				&& (tw < tx || tw > rx)
				&& (th < ty || th > ry));
	} // end of intersects method

	/**
	 * The smallest rectangle containing both this and the other. Matches
	 * the result of {@code Rectangle.union(Rectangle)}
	 * (and {@code Rectangle.add(Rectangle)}), returning a new value
	 * rather than mutating.
	 *
	 * @param b the other rectangle.
	 * @return the union rectangle.
	 */
	public Bounds union(Bounds b) {
		long tx2 = this.width;
		long ty2 = this.height;
		if ((tx2 | ty2) < 0) {
			return b;
		}
		long rx2 = b.width;
		long ry2 = b.height;
		if ((rx2 | ry2) < 0) {
			return this;
		}
		int tx1 = this.x;
		int ty1 = this.y;
		tx2 += tx1;
		ty2 += ty1;
		int rx1 = b.x;
		int ry1 = b.y;
		rx2 += rx1;
		ry2 += ry1;
		if (tx1 > rx1) {
			tx1 = rx1;
		}
		if (ty1 > ry1) {
			ty1 = ry1;
		}
		if (tx2 < rx2) {
			tx2 = rx2;
		}
		if (ty2 < ry2) {
			ty2 = ry2;
		}
		tx2 -= tx1;
		ty2 -= ty1;
		if (tx2 > Integer.MAX_VALUE) {
			tx2 = Integer.MAX_VALUE;
		}
		if (ty2 > Integer.MAX_VALUE) {
			ty2 = Integer.MAX_VALUE;
		}
		return new Bounds(tx1, ty1, (int) tx2, (int) ty2);
	} // end of union method

	/**
	 * The smallest rectangle containing this and the given point. Matches
	 * the result of {@code Rectangle.add(int,int)}, returning a
	 * new value rather than mutating.
	 *
	 * @param px the point's x.
	 * @param py the point's y.
	 * @return the grown rectangle.
	 */
	public Bounds include(int px, int py) {
		if ((width | height) < 0) {
			return new Bounds(px, py, 0, 0);
		}
		int x1 = this.x;
		int y1 = this.y;
		long x2 = this.width;
		long y2 = this.height;
		x2 += x1;
		y2 += y1;
		if (x1 > px) {
			x1 = px;
		}
		if (y1 > py) {
			y1 = py;
		}
		if (x2 < px) {
			x2 = px;
		}
		if (y2 < py) {
			y2 = py;
		}
		x2 -= x1;
		y2 -= y1;
		if (x2 > Integer.MAX_VALUE) {
			x2 = Integer.MAX_VALUE;
		}
		if (y2 > Integer.MAX_VALUE) {
			y2 = Integer.MAX_VALUE;
		}
		return new Bounds(x1, y1, (int) x2, (int) y2);
	} // end of include method

} // end of Bounds record
