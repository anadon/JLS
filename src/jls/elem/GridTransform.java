package jls.elem;

import java.awt.Dimension;
import java.awt.Point;

/**
 * Exact integer geometry transforms on the snap grid (issue #24).
 *
 * Element geometry (put offsets, dimensions, label anchors) lives on the
 * 12px grid, so rotations and mirrors are exact integer maps - no rounding
 * ambiguity. Elements that today enumerate their geometry per orientation
 * in if/else ladders can instead declare one canonical orientation and
 * derive the rest through these transforms; the committed baseline in
 * OrientationGeometryTest pins what any such conversion must reproduce.
 *
 * All point transforms take an offset relative to the element's origin
 * plus the box dimension the transform depends on, and return the offset
 * in the transformed box (whose dimensions swap under rotation).
 */
public final class GridTransform {

	/**
	 * Prevents instantiation; this class holds only static transforms.
	 */
	private GridTransform() {
	} // end of constructor

	/**
	 * Rotate a point 90 degrees clockwise within a w-by-h box; the result
	 * lives in an h-by-w box.
	 *
	 * @see jls.elem.GridTransformTest#quarterTurnsCompose()
	 */
	public static Point rotateCW(int px, int py, int height) {

		return new Point(height - py, px);
	} // end of rotateCW method

	/**
	 * Rotate a point 90 degrees counter-clockwise within a w-by-h box; the
	 * result lives in an h-by-w box.
	 *
	 * @see jls.elem.GridTransformTest#andGateOrientationsAreTransformsOfEachOther()
	 * @see jls.elem.GridTransformTest#quarterTurnsCompose()
	 */
	public static Point rotateCCW(int px, int py, int width) {

		return new Point(py, width - px);
	} // end of rotateCCW method

	/**
	 * Rotate a point 180 degrees within a w-by-h box.
	 *
	 * @see jls.elem.GridTransformTest#quarterTurnsCompose()
	 */
	public static Point rotate180(int px, int py, int width, int height) {

		return new Point(width - px, height - py);
	} // end of rotate180 method

	/**
	 * Mirror a point across the box's vertical axis (flip left/right).
	 *
	 * @see jls.elem.GridTransformTest#adderLeftIsMirrorOfRight()
	 * @see jls.elem.GridTransformTest#andGateOrientationsAreTransformsOfEachOther()
	 */
	public static Point mirrorX(int px, int py, int width) {

		return new Point(width - px, py);
	} // end of mirrorX method

	/**
	 * Mirror a point across the box's horizontal axis (flip up/down).
	 *
	 * @see jls.elem.GridTransformTest#andGateOrientationsAreTransformsOfEachOther()
	 */
	public static Point mirrorY(int px, int py, int height) {

		return new Point(px, height - py);
	} // end of mirrorY method

	/**
	 * The dimensions of a box after a quarter-turn rotation.
	 *
	 * @see jls.elem.GridTransformTest#quarterTurnsCompose()
	 */
	public static Dimension rotatedSize(int width, int height) {

		return new Dimension(height, width);
	} // end of rotatedSize method

	/**
	 * Start a composed transform from a canonical box of the given size.
	 * Operations are applied in the order they are chained.
	 */
	public static Chain chain(int width, int height) {

		return new Chain(width, height);
	} // end of chain method

	/**
	 * A composed sequence of grid transforms over a canonical box. An
	 * element declares its geometry once, in one canonical orientation,
	 * builds the chain for its current orientation, and maps every put
	 * offset and drawing coordinate through it (issue #24).
	 */
	public static final class Chain {

		private static final int CW = 0, CCW = 1, R180 = 2, MX = 3, MY = 4;

		private final int canonicalWidth;
		private final int canonicalHeight;
		private final java.util.List<Integer> ops =
				new java.util.ArrayList<Integer>();

		/**
		 * Create an empty chain over a canonical box of the given size.
		 *
		 * @param width the canonical box width.
		 * @param height the canonical box height.
		 */
		private Chain(int width, int height) {

			canonicalWidth = width;
			canonicalHeight = height;
		} // end of constructor

		/**
		 * Append a 90-degree clockwise rotation to the chain.
		 *
		 * @return this chain, for further chaining.
		 */
		public Chain rotateCW() {

			ops.add(CW);
			return this;
		} // end of rotateCW method

		/**
		 * Append a 90-degree counter-clockwise rotation to the chain.
		 *
		 * @return this chain, for further chaining.
		 */
		public Chain rotateCCW() {

			ops.add(CCW);
			return this;
		} // end of rotateCCW method

		/**
		 * Append a 180-degree rotation to the chain.
		 *
		 * @return this chain, for further chaining.
		 */
		public Chain rotate180() {

			ops.add(R180);
			return this;
		} // end of rotate180 method

		/**
		 * Append a mirror across the vertical axis (flip left/right) to the
		 * chain.
		 *
		 * @return this chain, for further chaining.
		 */
		public Chain mirrorX() {

			ops.add(MX);
			return this;
		} // end of mirrorX method

		/**
		 * Append a mirror across the horizontal axis (flip up/down) to the
		 * chain.
		 *
		 * @return this chain, for further chaining.
		 */
		public Chain mirrorY() {

			ops.add(MY);
			return this;
		} // end of mirrorY method

		/**
		 * Map a point from canonical coordinates through every chained
		 * transform.
		 */
		public Point map(int px, int py) {

			int w = canonicalWidth;
			int h = canonicalHeight;
			Point p = new Point(px, py);
			for (int op : ops) {
				switch (op) {
				case CW:
					p = GridTransform.rotateCW(p.x, p.y, h);
					int t = w; w = h; h = t;
					break;
				case CCW:
					p = GridTransform.rotateCCW(p.x, p.y, w);
					t = w; w = h; h = t;
					break;
				case R180:
					p = GridTransform.rotate180(p.x, p.y, w, h);
					break;
				case MX:
					p = GridTransform.mirrorX(p.x, p.y, w);
					break;
				default:
					p = GridTransform.mirrorY(p.x, p.y, h);
					break;
				}
			}
			return p;
		} // end of map method

		/**
		 * The box dimensions after every chained transform.
		 */
		public Dimension size() {

			int w = canonicalWidth;
			int h = canonicalHeight;
			for (int op : ops) {
				if (op == CW || op == CCW) {
					int t = w; w = h; h = t;
				}
			}
			return new Dimension(w, h);
		} // end of size method

		/**
		 * Draw a line whose endpoints are canonical coordinates, mapped
		 * through the chain and offset by the element position.
		 */
		public void drawLine(java.awt.Graphics g, int originX, int originY,
				int x1, int y1, int x2, int y2) {

			Point p1 = map(x1, y1);
			Point p2 = map(x2, y2);
			g.drawLine(originX + p1.x, originY + p1.y,
					originX + p2.x, originY + p2.y);
		} // end of drawLine method

	} // end of Chain class

} // end of GridTransform class
