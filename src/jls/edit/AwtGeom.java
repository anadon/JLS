package jls.edit;

import java.awt.Rectangle;

import jls.core.Bounds;

/**
 * The GUI-edge bridge between the headless {@link Bounds} value type and
 * AWT's {@link Rectangle} (issue #77). Element and circuit geometry is
 * computed in {@code Bounds}, which carries no AWT dependency; the editor
 * and renderers convert to a {@code Rectangle} only where Swing genuinely
 * needs one (clip regions, image sizes, {@code fill}/{@code union}).
 */
public final class AwtGeom {

	/**
	 * Prevents instantiation; this class only holds static conversions.
	 */
	private AwtGeom() {
	} // no instances

	/**
	 * An AWT rectangle with the same edges as the given bounds.
	 *
	 * @param b The bounds to convert.
	 * @return the equivalent {@code java.awt.Rectangle}.
	 */
	public static Rectangle awt(Bounds b) {
		return new Rectangle(b.x(), b.y(), b.width(), b.height());
	} // end of awt method

	/**
	 * Bounds with the same edges as the given AWT rectangle.
	 *
	 * @param r The rectangle to convert.
	 * @return the equivalent {@link Bounds}.
	 */
	public static Bounds bounds(Rectangle r) {
		return new Bounds(r.x, r.y, r.width, r.height);
	} // end of bounds method

} // end of AwtGeom class
