package jls.ui;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Layer-3 (rendering) assertion helper (issues #91 / #162): semantic
 * checks over a {@link BufferedImage} an element painted into - never
 * brittle pixel goldens, per the package layering rules.
 *
 * <p>The one assertion this opens the layer with is <em>containment</em>:
 * everything an element paints must land inside an allowed rectangle.
 * The meaningful choice of that rectangle is the element's index bounds
 * grown by the draw margin the paint pipeline itself culls with
 * ({@code Circuit.DRAW_MARGIN}, pinned against the Agda model by
 * {@code jls.ProofBridgeTest}): an element painting outside that
 * envelope would be silently truncated by a clipped repaint, which is a
 * real rendering-bug class, not a cosmetic one.</p>
 *
 * <p>Per the package discipline this helper is pinned by
 * deliberately-failing tests in {@link jls.ui.RenderBoundsTest}.</p>
 */
public final class RenderAssert {

	private RenderAssert() {
	}

	/**
	 * Assert that everything painted into the image lies inside the
	 * allowed rectangle, and that something was painted at all (an
	 * untouched canvas would make the containment check vacuous).
	 *
	 * @param image The canvas the subject painted into.
	 * @param backgroundRGB The canvas fill color, as {@code getRGB} packed
	 *        ARGB; any other pixel value counts as painted.
	 * @param allowed The rectangle all painted pixels must fall inside.
	 * @param what The subject, for failure messages.
	 */
	public static void assertPaintsWithinBounds(BufferedImage image,
			int backgroundRGB, Rectangle allowed, String what) {
		long painted = 0;
		int strayX = -1;
		int strayY = -1;
		long strays = 0;
		for (int y = 0; y < image.getHeight(); y += 1) {
			for (int x = 0; x < image.getWidth(); x += 1) {
				if (image.getRGB(x, y) == backgroundRGB) {
					continue;
				}
				painted += 1;
				if (!allowed.contains(x, y)) {
					strays += 1;
					if (strayX < 0) {
						strayX = x;
						strayY = y;
					}
				}
			}
		}
		if (painted == 0) {
			throw new AssertionError(what + ": painted nothing - the"
					+ " containment check would be vacuous");
		}
		if (strays > 0) {
			throw new AssertionError(what + ": painted " + strays
					+ " pixel(s) outside its allowed bounds " + allowed
					+ ", first at (" + strayX + "," + strayY + ") - a"
					+ " clipped repaint would truncate this element");
		}
	} // end of assertPaintsWithinBounds method

} // end of RenderAssert class
