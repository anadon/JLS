package jls.edit;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * The editor's view transform (issue #74): a single object owning the
 * zoom scale and pan translation between the circuit's model
 * coordinates (the absolute, grid-snapped integers that are saved to
 * files and consumed by hit-testing) and the screen coordinates of the
 * editor canvas.
 *
 * The design was adjudicated on issue #74 (2026-07-08 approval,
 * 2026-07-17 ambiguity sweep):
 * <ul>
 * <li>zoom range is [{@link #MIN_SCALE}, {@link #MAX_SCALE}] = [0.25,
 *     4.0];</li>
 * <li>wheel zoom is continuous at {@link #WHEEL_STEP} (~1.15 per
 *     notch) and is centered on the cursor - the model point under the
 *     cursor stays fixed ({@link #zoomBy});</li>
 * <li>keyboard zoom snaps to a fixed ladder of stops
 *     (25/50/75/100/150/200/300/400 percent, {@link #ladderUp} /
 *     {@link #ladderDown}) so that zoom-reset lands on exactly 100%
 *     and the issue #91 screenshot grid gets predictable
 *     percentages;</li>
 * <li>fit-to-circuit ({@link #fit}) is the "reset the view" affordance
 *     - the model is never shrunk to match the window;</li>
 * <li>hit-testing happens in model space only: every mouse-event entry
 *     point inverts the transform exactly once ({@link #toModel}) and
 *     all downstream arithmetic stays in model coordinates, so model
 *     geometry and saved files are identical at every zoom level.</li>
 * </ul>
 *
 * The screen-to-model direction is: {@code model = (screen -
 * translate) / scale}; model-to-screen is {@code screen = model *
 * scale + translate}. Both are exact in doubles - integer convenience
 * overloads round once, at the boundary, and the sub-pixel error is
 * absorbed by the editor's 12-pixel grid snap.
 *
 * This class is deliberately pure (no Swing components, no listeners,
 * no static mutable state) so the whole coordinate contract is
 * unit-testable under the surefire JVM's {@code java.awt.headless=true}
 * - the same pattern as {@link DeleteKeyPolicy}. The editor wiring
 * (paint transform, wheel/keyboard gestures, pan drag) consumes this
 * object; device-scale (HiDPI) factors compose here as well when that
 * work lands.
 *
 * @jls.testedby jls.edit.ViewportTest
 */
final class Viewport {

	/**
	 * The smallest permitted zoom scale (25%), per the issue #74
	 * adjudication.
	 */
	static final double MIN_SCALE = 0.25;

	/**
	 * The largest permitted zoom scale (400%), per the issue #74
	 * adjudication.
	 */
	static final double MAX_SCALE = 4.0;

	/**
	 * The continuous zoom factor applied per mouse-wheel notch. The
	 * adjudication asked for ~1.15-1.2x per notch; 1.15 keeps seven
	 * notches inside the [0.25, 4.0] range in each direction from 100%.
	 */
	static final double WHEEL_STEP = 1.15;

	/**
	 * The fixed keyboard-zoom ladder, in ascending order:
	 * 25/50/75/100/150/200/300/400 percent. The first and last stops
	 * coincide with {@link #MIN_SCALE} and {@link #MAX_SCALE}, and 1.0
	 * is a stop so zoom-reset is exactly 100%.
	 */
	static final double[] LADDER =
			{ 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0 };

	/**
	 * Tolerance used when comparing the current scale against ladder
	 * stops, so that a scale reached by continuous wheel zoom that is
	 * numerically indistinguishable from a stop is treated as being at
	 * that stop.
	 */
	private static final double EPSILON = 1e-9;

	/**
	 * Current zoom scale; always within [MIN_SCALE, MAX_SCALE].
	 */
	private double scale = 1.0;

	/**
	 * Horizontal translation, in screen pixels, applied after scaling.
	 */
	private double translateX = 0.0;

	/**
	 * Vertical translation, in screen pixels, applied after scaling.
	 */
	private double translateY = 0.0;

	/**
	 * Create a viewport at the identity transform: 100% zoom, no pan.
	 */
	Viewport() {

		// identity is the field defaults
	} // end of Viewport method

	/**
	 * The current zoom scale (1.0 = 100%).
	 *
	 * @return the scale, always within [MIN_SCALE, MAX_SCALE].
	 */
	double getScale() {

		return scale;
	} // end of getScale method

	/**
	 * The current horizontal translation in screen pixels.
	 *
	 * @return the x translation.
	 */
	double getTranslateX() {

		return translateX;
	} // end of getTranslateX method

	/**
	 * The current vertical translation in screen pixels.
	 *
	 * @return the y translation.
	 */
	double getTranslateY() {

		return translateY;
	} // end of getTranslateY method

	/**
	 * Whether this viewport is the identity transform (exactly 100%
	 * zoom and no pan), so the editor's paint path can skip the
	 * transform entirely and dirty regions need no mapping.
	 *
	 * @return true at the identity transform.
	 */
	boolean isIdentity() {

		return scale == 1.0 && translateX == 0.0 && translateY == 0.0;
	} // end of isIdentity method

	/**
	 * The affine transform to apply to the editor's Graphics2D in
	 * paintComponent: scale about the origin, then translate. A fresh
	 * object is returned each call so callers cannot mutate viewport
	 * state through it.
	 *
	 * @return a new AffineTransform equal to
	 *         translate(translateX, translateY) composed with
	 *         scale(scale, scale).
	 */
	AffineTransform createTransform() {

		return new AffineTransform(scale, 0.0, 0.0, scale,
				translateX, translateY);
	} // end of createTransform method

	/**
	 * Map a screen point to exact (double) model coordinates. This is
	 * the single inversion each mouse-event entry point performs; all
	 * hit-testing and placement arithmetic downstream stays in model
	 * space, per the issue #74 hypothesis.
	 *
	 * @param screenX The x coordinate in screen pixels.
	 * @param screenY The y coordinate in screen pixels.
	 *
	 * @return the corresponding model point, exact in doubles.
	 */
	Point2D.Double toModel(double screenX, double screenY) {

		return new Point2D.Double((screenX - translateX) / scale,
				(screenY - translateY) / scale);
	} // end of toModel method

	/**
	 * Map a screen point to integer model coordinates, rounding once
	 * at the boundary. The at-most-half-model-unit rounding error is
	 * far below the editor's 12-pixel grid snap distance.
	 *
	 * @param screen The point in screen pixels.
	 *
	 * @return the nearest integer model point.
	 */
	Point toModel(Point screen) {

		Point2D.Double m = toModel(screen.x, screen.y);
		return new Point((int)Math.round(m.x), (int)Math.round(m.y));
	} // end of toModel method

	/**
	 * Map a model point to exact (double) screen coordinates.
	 *
	 * @param modelX The x coordinate in model units.
	 * @param modelY The y coordinate in model units.
	 *
	 * @return the corresponding screen point, exact in doubles.
	 */
	Point2D.Double toScreen(double modelX, double modelY) {

		return new Point2D.Double(modelX * scale + translateX,
				modelY * scale + translateY);
	} // end of toScreen method

	/**
	 * Map a model point to integer screen coordinates, rounding once.
	 *
	 * @param model The point in model units.
	 *
	 * @return the nearest integer screen pixel.
	 */
	Point toScreen(Point model) {

		Point2D.Double s = toScreen(model.x, model.y);
		return new Point((int)Math.round(s.x), (int)Math.round(s.y));
	} // end of toScreen method

	/**
	 * Map a model-space rectangle (e.g. a dirty region from
	 * issues #35/#43) to the smallest integer screen rectangle that
	 * fully encloses its image under the transform, so a repaint of
	 * the result covers every pixel the model region can touch.
	 *
	 * @param model The rectangle in model units.
	 *
	 * @return the enclosing rectangle in screen pixels.
	 */
	Rectangle toScreen(Rectangle model) {

		double x1 = model.x * scale + translateX;
		double y1 = model.y * scale + translateY;
		double x2 = (model.x + model.width) * scale + translateX;
		double y2 = (model.y + model.height) * scale + translateY;
		int left = (int)Math.floor(x1);
		int top = (int)Math.floor(y1);
		return new Rectangle(left, top,
				(int)Math.ceil(x2) - left, (int)Math.ceil(y2) - top);
	} // end of toScreen method

	/**
	 * Map a screen-space rectangle (e.g. a paint clip) to the smallest
	 * integer model rectangle that fully encloses its pre-image, so
	 * drawing everything inside the result covers the whole clip.
	 *
	 * @param screen The rectangle in screen pixels.
	 *
	 * @return the enclosing rectangle in model units.
	 */
	Rectangle toModel(Rectangle screen) {

		double x1 = (screen.x - translateX) / scale;
		double y1 = (screen.y - translateY) / scale;
		double x2 = (screen.x + screen.width - translateX) / scale;
		double y2 = (screen.y + screen.height - translateY) / scale;
		int left = (int)Math.floor(x1);
		int top = (int)Math.floor(y1);
		return new Rectangle(left, top,
				(int)Math.ceil(x2) - left, (int)Math.ceil(y2) - top);
	} // end of toModel method

	/**
	 * Pan the view by a screen-pixel delta (space-drag or middle-drag,
	 * per the adjudicated gesture set): the circuit appears to move by
	 * exactly (dx, dy) pixels.
	 *
	 * @param dx The horizontal pan in screen pixels.
	 * @param dy The vertical pan in screen pixels.
	 */
	void pan(double dx, double dy) {

		translateX += dx;
		translateY += dy;
	} // end of pan method

	/**
	 * Set the zoom scale, clamped to [MIN_SCALE, MAX_SCALE], keeping
	 * the model point currently under the given screen anchor fixed at
	 * that anchor - the standard zoom-at-cursor expectation (issue #74
	 * prediction P3). With the anchor at the view center this also
	 * implements keyboard zoom.
	 *
	 * @param newScale The requested scale; values outside the
	 *                 permitted range are clamped.
	 * @param anchorX  The x screen coordinate to hold fixed.
	 * @param anchorY  The y screen coordinate to hold fixed.
	 */
	void zoomTo(double newScale, double anchorX, double anchorY) {

		double s = clampScale(newScale);

		// the model point currently under the anchor
		double modelX = (anchorX - translateX) / scale;
		double modelY = (anchorY - translateY) / scale;

		// re-derive the translation so that point stays put
		scale = s;
		translateX = anchorX - s * modelX;
		translateY = anchorY - s * modelY;
	} // end of zoomTo method

	/**
	 * Multiply the zoom scale by a factor (clamped), keeping the model
	 * point under the anchor fixed. A wheel notch toward the user is
	 * {@code zoomBy(WHEEL_STEP, ...)}; away is
	 * {@code zoomBy(1 / WHEEL_STEP, ...)}.
	 *
	 * @param factor  The multiplicative zoom change; must be positive.
	 * @param anchorX The x screen coordinate to hold fixed.
	 * @param anchorY The y screen coordinate to hold fixed.
	 */
	void zoomBy(double factor, double anchorX, double anchorY) {

		zoomTo(scale * factor, anchorX, anchorY);
	} // end of zoomBy method

	/**
	 * Reset to the identity transform: exactly 100% zoom, no pan. This
	 * is the state a freshly opened editor starts in.
	 */
	void reset() {

		scale = 1.0;
		translateX = 0.0;
		translateY = 0.0;
	} // end of reset method

	/**
	 * Fit a model-space bounding box into a view of the given size and
	 * center it - the adjudicated "reset the view" affordance for the
	 * View menu's Fit item. The scale is the largest that shows the
	 * whole box, clamped to the permitted range (a tiny circuit is
	 * shown at 400%, not blown up further; an enormous one may still
	 * overflow at 25% and is centered). Degenerate arguments (an empty
	 * box or view) reset to the identity instead.
	 *
	 * @param bounds     The model-space box to show, e.g. the
	 *                   circuit's used bounds.
	 * @param viewWidth  The visible viewport width in screen pixels.
	 * @param viewHeight The visible viewport height in screen pixels.
	 */
	void fit(Rectangle bounds, int viewWidth, int viewHeight) {

		if (bounds == null || bounds.width <= 0 || bounds.height <= 0
				|| viewWidth <= 0 || viewHeight <= 0) {
			reset();
			return;
		}

		double s = clampScale(Math.min(
				viewWidth / (double)bounds.width,
				viewHeight / (double)bounds.height));
		scale = s;
		translateX = (viewWidth - s * bounds.width) / 2.0
				- s * bounds.x;
		translateY = (viewHeight - s * bounds.height) / 2.0
				- s * bounds.y;
	} // end of fit method

	/**
	 * Clamp a requested scale to the permitted [MIN_SCALE, MAX_SCALE]
	 * range.
	 *
	 * @param s The requested scale.
	 *
	 * @return s limited to the permitted range.
	 */
	static double clampScale(double s) {

		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
	} // end of clampScale method

	/**
	 * The next keyboard-zoom stop above a scale: the smallest ladder
	 * stop strictly greater than it (within tolerance), or
	 * {@link #MAX_SCALE} when already at or beyond the top stop. From
	 * a scale between stops (reached by wheel zoom) this snaps up onto
	 * the ladder.
	 *
	 * @param s The current scale.
	 *
	 * @return the ladder stop to zoom in to.
	 */
	static double ladderUp(double s) {

		for (double stop : LADDER) {
			if (stop > s + EPSILON)
				return stop;
		}
		return MAX_SCALE;
	} // end of ladderUp method

	/**
	 * The next keyboard-zoom stop below a scale: the largest ladder
	 * stop strictly less than it (within tolerance), or
	 * {@link #MIN_SCALE} when already at or below the bottom stop.
	 * From a scale between stops this snaps down onto the ladder.
	 *
	 * @param s The current scale.
	 *
	 * @return the ladder stop to zoom out to.
	 */
	static double ladderDown(double s) {

		for (int i = LADDER.length - 1; i >= 0; i--) {
			if (LADDER[i] < s - EPSILON)
				return LADDER[i];
		}
		return MIN_SCALE;
	} // end of ladderDown method

	/**
	 * Zoom in one keyboard ladder stop (menu/accelerator zoom-in),
	 * keeping the model point under the anchor fixed.
	 *
	 * @param anchorX The x screen coordinate to hold fixed, normally
	 *                the view center.
	 * @param anchorY The y screen coordinate to hold fixed.
	 */
	void zoomInLadder(double anchorX, double anchorY) {

		zoomTo(ladderUp(scale), anchorX, anchorY);
	} // end of zoomInLadder method

	/**
	 * Zoom out one keyboard ladder stop (menu/accelerator zoom-out),
	 * keeping the model point under the anchor fixed.
	 *
	 * @param anchorX The x screen coordinate to hold fixed, normally
	 *                the view center.
	 * @param anchorY The y screen coordinate to hold fixed.
	 */
	void zoomOutLadder(double anchorX, double anchorY) {

		zoomTo(ladderDown(scale), anchorX, anchorY);
	} // end of zoomOutLadder method

} // end of Viewport class
