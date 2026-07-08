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
 * within a box of the given width and height, and return the offset in
 * the transformed box (whose dimensions swap under rotation).
 */
public final class GridTransform {

	private GridTransform() {
	} // end of constructor

	/**
	 * Rotate a point 90 degrees clockwise within a w-by-h box; the result
	 * lives in an h-by-w box.
	 */
	public static Point rotateCW(int px, int py, int width, int height) {

		return new Point(height - py, px);
	} // end of rotateCW method

	/**
	 * Rotate a point 90 degrees counter-clockwise within a w-by-h box; the
	 * result lives in an h-by-w box.
	 */
	public static Point rotateCCW(int px, int py, int width, int height) {

		return new Point(py, width - px);
	} // end of rotateCCW method

	/**
	 * Rotate a point 180 degrees within a w-by-h box.
	 */
	public static Point rotate180(int px, int py, int width, int height) {

		return new Point(width - px, height - py);
	} // end of rotate180 method

	/**
	 * Mirror a point across the box's vertical axis (flip left/right).
	 */
	public static Point mirrorX(int px, int py, int width, int height) {

		return new Point(width - px, py);
	} // end of mirrorX method

	/**
	 * Mirror a point across the box's horizontal axis (flip up/down).
	 */
	public static Point mirrorY(int px, int py, int width, int height) {

		return new Point(px, height - py);
	} // end of mirrorY method

	/**
	 * The dimensions of a box after a quarter-turn rotation.
	 */
	public static Dimension rotatedSize(int width, int height) {

		return new Dimension(height, width);
	} // end of rotatedSize method

} // end of GridTransform class
