package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Pins the issue #74 Viewport coordinate contract - the pure core of
 * editor zoom and pan, adjudicated on the issue (2026-07-08 approval,
 * 2026-07-17 decisions):
 * <ul>
 * <li>scale clamped to [0.25, 4.0];</li>
 * <li>wheel zoom continuous at ~1.15x per notch, centered on the
 *     cursor (prediction P3: the model point under the cursor stays
 *     fixed);</li>
 * <li>keyboard zoom snaps onto the 25/50/75/100/150/200/300/400
 *     percent ladder, with reset landing on exactly 100%;</li>
 * <li>screen-to-model and model-to-screen are exact inverses in
 *     doubles, so hit-testing in model space is independent of the
 *     zoom level (hypothesis section 4).</li>
 * </ul>
 *
 * Everything here is a pure computation, runnable under surefire's
 * {@code java.awt.headless=true} - the same compensating-control
 * pattern as DeleteKeyPolicyTest for GUI code the headless JVM cannot
 * construct. Randomized cases use a fixed seed for reproducibility.
 */
class ViewportTest {

	/** Tolerance for double comparisons. */
	private static final double EPS = 1e-9;

	/** Fixed seed so the randomized properties are reproducible. */
	private static final long SEED = 74L;

	/** A viewport in a random legal state (any scale, generous pan). */
	private static Viewport randomViewport(Random rnd) {

		Viewport vp = new Viewport();
		double scale = Viewport.MIN_SCALE + rnd.nextDouble()
				* (Viewport.MAX_SCALE - Viewport.MIN_SCALE);
		vp.zoomTo(scale, rnd.nextInt(2001) - 1000,
				rnd.nextInt(2001) - 1000);
		vp.pan(rnd.nextInt(4001) - 2000, rnd.nextInt(4001) - 2000);
		return vp;
	}

	// ---- initial state ----

	/** A fresh viewport is the identity: 100%, no pan. */
	@Test
	void freshViewportIsIdentity() {

		Viewport vp = new Viewport();
		assertTrue(vp.isIdentity());
		assertEquals(1.0, vp.getScale());
		assertEquals(0.0, vp.getTranslateX());
		assertEquals(0.0, vp.getTranslateY());
		assertTrue(vp.createTransform().isIdentity());
	}

	/** At the identity, integer mapping is the exact identity. */
	@Test
	void identityMapsIntegersExactly() {

		Viewport vp = new Viewport();
		Point p = new Point(123, -45);
		assertEquals(p, vp.toModel(new Point(p)));
		assertEquals(p, vp.toScreen(new Point(p)));
		Rectangle r = new Rectangle(12, 24, 36, 48);
		assertEquals(r, vp.toScreen(new Rectangle(r)));
		assertEquals(r, vp.toModel(new Rectangle(r)));
	}

	// ---- adjudicated constants ----

	/** The range is [0.25, 4.0] and the wheel step is ~1.15-1.2. */
	@Test
	void adjudicatedConstants() {

		assertEquals(0.25, Viewport.MIN_SCALE);
		assertEquals(4.0, Viewport.MAX_SCALE);
		assertTrue(Viewport.WHEEL_STEP >= 1.15
				&& Viewport.WHEEL_STEP <= 1.2,
				"wheel step must be ~1.15-1.2x per notch");
	}

	/** The keyboard ladder is exactly 25/50/75/100/150/200/300/400%. */
	@Test
	void ladderStopsAreTheAdjudicatedPercentages() {

		double[] expected = { 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0 };
		assertEquals(expected.length, Viewport.LADDER.length);
		for (int i = 0; i < expected.length; i++)
			assertEquals(expected[i], Viewport.LADDER[i],
					"ladder stop " + i);
	}

	// ---- round-trip exactness (hypothesis section 4) ----

	/** toModel and toScreen are exact double inverses at any state. */
	@Test
	void doubleRoundTripIsExact() {

		Random rnd = new Random(SEED);
		for (int i = 0; i < 1000; i++) {
			Viewport vp = randomViewport(rnd);
			double mx = rnd.nextInt(4001) - 2000;
			double my = rnd.nextInt(4001) - 2000;
			Point2D.Double s = vp.toScreen(mx, my);
			Point2D.Double back = vp.toModel(s.x, s.y);
			assertEquals(mx, back.x, EPS, "x round trip, case " + i);
			assertEquals(my, back.y, EPS, "y round trip, case " + i);
		}
	}

	/** createTransform agrees with toScreen for random points. */
	@Test
	void transformMatchesToScreen() {

		Random rnd = new Random(SEED + 1);
		for (int i = 0; i < 200; i++) {
			Viewport vp = randomViewport(rnd);
			AffineTransform t = vp.createTransform();
			double mx = rnd.nextInt(2001) - 1000;
			double my = rnd.nextInt(2001) - 1000;
			Point2D.Double viaTransform = new Point2D.Double();
			t.transform(new Point2D.Double(mx, my), viaTransform);
			Point2D.Double viaViewport = vp.toScreen(mx, my);
			assertEquals(viaViewport.x, viaTransform.x, EPS);
			assertEquals(viaViewport.y, viaTransform.y, EPS);
		}
	}

	/**
	 * Integer screen-to-model rounding stays within half a model unit
	 * of the exact answer - far below the 12-pixel grid snap that
	 * absorbs it (threat-to-validity section 10: hit-test in model
	 * space, transformed once).
	 */
	@Test
	void integerConversionErrorIsSubHalfUnit() {

		Random rnd = new Random(SEED + 2);
		for (int i = 0; i < 1000; i++) {
			Viewport vp = randomViewport(rnd);
			int sx = rnd.nextInt(4001) - 2000;
			int sy = rnd.nextInt(4001) - 2000;
			Point rounded = vp.toModel(new Point(sx, sy));
			Point2D.Double exact = vp.toModel(sx, sy);
			assertTrue(Math.abs(rounded.x - exact.x) <= 0.5 + EPS,
					"x rounding, case " + i);
			assertTrue(Math.abs(rounded.y - exact.y) <= 0.5 + EPS,
					"y rounding, case " + i);
		}
	}

	// ---- zoom-at-cursor (prediction P3) ----

	/** Zooming keeps the model point under the anchor fixed. */
	@Test
	void zoomAtAnchorKeepsModelPointFixed() {

		Random rnd = new Random(SEED + 3);
		for (int i = 0; i < 1000; i++) {
			Viewport vp = randomViewport(rnd);
			double ax = rnd.nextInt(1601);
			double ay = rnd.nextInt(1201);
			Point2D.Double before = vp.toModel(ax, ay);
			double factor = rnd.nextBoolean() ? Viewport.WHEEL_STEP
					: 1.0 / Viewport.WHEEL_STEP;
			vp.zoomBy(factor, ax, ay);
			Point2D.Double after = vp.toModel(ax, ay);
			assertEquals(before.x, after.x, 1e-6,
					"anchor model x, case " + i);
			assertEquals(before.y, after.y, 1e-6,
					"anchor model y, case " + i);
		}
	}

	/** The anchor invariant holds even when the scale clamps. */
	@Test
	void zoomClampingPreservesAnchor() {

		Viewport vp = new Viewport();
		vp.zoomTo(4.0, 100, 100);
		Point2D.Double before = vp.toModel(100, 100);
		vp.zoomBy(10.0, 100, 100); // clamps at 4.0
		assertEquals(4.0, vp.getScale());
		Point2D.Double after = vp.toModel(100, 100);
		assertEquals(before.x, after.x, EPS);
		assertEquals(before.y, after.y, EPS);

		vp.zoomTo(0.001, 100, 100); // clamps at 0.25
		assertEquals(0.25, vp.getScale());
		after = vp.toModel(100, 100);
		assertEquals(before.x, after.x, EPS);
		assertEquals(before.y, after.y, EPS);
	}

	/** Repeated wheel zoom never leaves the permitted range. */
	@Test
	void wheelZoomStaysInRange() {

		Viewport vp = new Viewport();
		for (int i = 0; i < 50; i++)
			vp.zoomBy(Viewport.WHEEL_STEP, 400, 300);
		assertEquals(Viewport.MAX_SCALE, vp.getScale());
		for (int i = 0; i < 100; i++)
			vp.zoomBy(1.0 / Viewport.WHEEL_STEP, 400, 300);
		assertEquals(Viewport.MIN_SCALE, vp.getScale());
	}

	// ---- keyboard ladder ----

	/** From a stop, up goes to the next stop; the top stop holds. */
	@Test
	void ladderUpFromStops() {

		assertEquals(0.5, Viewport.ladderUp(0.25));
		assertEquals(1.5, Viewport.ladderUp(1.0));
		assertEquals(4.0, Viewport.ladderUp(3.0));
		assertEquals(4.0, Viewport.ladderUp(4.0));
	}

	/** From a stop, down goes to the previous; the bottom holds. */
	@Test
	void ladderDownFromStops() {

		assertEquals(3.0, Viewport.ladderDown(4.0));
		assertEquals(0.75, Viewport.ladderDown(1.0));
		assertEquals(0.25, Viewport.ladderDown(0.5));
		assertEquals(0.25, Viewport.ladderDown(0.25));
	}

	/** Between stops (wheel zoom drift), keyboard zoom snaps on. */
	@Test
	void ladderSnapsFromBetweenStops() {

		assertEquals(1.5, Viewport.ladderUp(1.15));
		assertEquals(1.0, Viewport.ladderDown(1.15));
		assertEquals(0.5, Viewport.ladderUp(0.3));
		assertEquals(0.25, Viewport.ladderDown(0.3));
	}

	/**
	 * Zooming in one ladder stop from any reachable scale, then Ctrl+0
	 * semantics: zoomTo(1.0) is exactly 100%, never 99.999%.
	 */
	@Test
	void resetToHundredPercentIsExact() {

		Viewport vp = new Viewport();
		for (int i = 0; i < 7; i++)
			vp.zoomBy(Viewport.WHEEL_STEP, 333, 222);
		vp.zoomTo(1.0, 400, 300);
		assertEquals(1.0, vp.getScale(),
				"keyboard reset must land on exactly 100%");
	}

	/** zoomInLadder/zoomOutLadder move along the ladder at an anchor. */
	@Test
	void ladderZoomMethodsUseTheLadderAndAnchor() {

		Viewport vp = new Viewport();
		Point2D.Double before = vp.toModel(200, 150);
		vp.zoomInLadder(200, 150);
		assertEquals(1.5, vp.getScale());
		Point2D.Double after = vp.toModel(200, 150);
		assertEquals(before.x, after.x, EPS);
		assertEquals(before.y, after.y, EPS);
		vp.zoomOutLadder(200, 150);
		vp.zoomOutLadder(200, 150);
		assertEquals(0.75, vp.getScale());
	}

	// ---- pan ----

	/** Panning moves the view by exactly the screen delta. */
	@Test
	void panShiftsScreenMapping() {

		Random rnd = new Random(SEED + 4);
		for (int i = 0; i < 200; i++) {
			Viewport vp = randomViewport(rnd);
			double scaleBefore = vp.getScale();
			Point2D.Double before = vp.toScreen(60.0, 84.0);
			double dx = rnd.nextInt(401) - 200;
			double dy = rnd.nextInt(401) - 200;
			vp.pan(dx, dy);
			Point2D.Double after = vp.toScreen(60.0, 84.0);
			assertEquals(before.x + dx, after.x, EPS);
			assertEquals(before.y + dy, after.y, EPS);
			assertEquals(scaleBefore, vp.getScale(),
					"pan must not change the scale");
		}
	}

	// ---- rectangle mapping (dirty regions, issues #35/#43) ----

	/**
	 * The screen image of a model rectangle is fully enclosed by
	 * toScreen(Rectangle): every transformed corner lies inside, so a
	 * repaint of the result covers the whole model region.
	 */
	@Test
	void screenRectangleEnclosesTransformedModelRect() {

		Random rnd = new Random(SEED + 5);
		for (int i = 0; i < 500; i++) {
			Viewport vp = randomViewport(rnd);
			Rectangle m = new Rectangle(rnd.nextInt(2001) - 1000,
					rnd.nextInt(2001) - 1000,
					1 + rnd.nextInt(500), 1 + rnd.nextInt(500));
			Rectangle s = vp.toScreen(m);
			Point2D.Double c1 = vp.toScreen((double)m.x, (double)m.y);
			Point2D.Double c2 = vp.toScreen((double)m.x + m.width,
					(double)m.y + m.height);
			assertTrue(s.x <= c1.x + EPS && s.y <= c1.y + EPS,
					"top-left enclosed, case " + i);
			assertTrue(s.x + s.width >= c2.x - EPS
					&& s.y + s.height >= c2.y - EPS,
					"bottom-right enclosed, case " + i);
		}
	}

	/**
	 * The model pre-image of a screen rectangle is fully enclosed by
	 * toModel(Rectangle): drawing everything inside the result covers
	 * the whole screen clip.
	 */
	@Test
	void modelRectangleEnclosesClipPreImage() {

		Random rnd = new Random(SEED + 6);
		for (int i = 0; i < 500; i++) {
			Viewport vp = randomViewport(rnd);
			Rectangle s = new Rectangle(rnd.nextInt(2001) - 1000,
					rnd.nextInt(2001) - 1000,
					1 + rnd.nextInt(800), 1 + rnd.nextInt(800));
			Rectangle m = vp.toModel(s);
			Point2D.Double c1 = vp.toModel((double)s.x, (double)s.y);
			Point2D.Double c2 = vp.toModel((double)s.x + s.width,
					(double)s.y + s.height);
			assertTrue(m.x <= c1.x + EPS && m.y <= c1.y + EPS,
					"top-left enclosed, case " + i);
			assertTrue(m.x + m.width >= c2.x - EPS
					&& m.y + m.height >= c2.y - EPS,
					"bottom-right enclosed, case " + i);
		}
	}

	// ---- fit-to-circuit ----

	/** Fit shows the whole bounds, centered, at the largest scale. */
	@Test
	void fitShowsWholeBoundsCentered() {

		Viewport vp = new Viewport();
		Rectangle bounds = new Rectangle(100, 200, 800, 400);
		vp.fit(bounds, 400, 400);

		// 400/800 = 0.5 horizontally binds (400/400 = 1 vertically)
		assertEquals(0.5, vp.getScale(), EPS);

		// the box maps inside the view, centered
		Rectangle onScreen = vp.toScreen(bounds);
		assertTrue(onScreen.x >= -1 && onScreen.y >= -1,
				"fit box inside view: " + onScreen);
		assertTrue(onScreen.x + onScreen.width <= 401
				&& onScreen.y + onScreen.height <= 401,
				"fit box inside view: " + onScreen);
		Point2D.Double center = vp.toScreen(
				bounds.x + bounds.width / 2.0,
				bounds.y + bounds.height / 2.0);
		assertEquals(200.0, center.x, 0.5);
		assertEquals(200.0, center.y, 0.5);
	}

	/** A tiny circuit fits at the 400% clamp, not blown up further. */
	@Test
	void fitClampsForTinyCircuits() {

		Viewport vp = new Viewport();
		vp.fit(new Rectangle(0, 0, 24, 24), 1000, 1000);
		assertEquals(Viewport.MAX_SCALE, vp.getScale());
	}

	/** An enormous circuit clamps at 25% and stays centered. */
	@Test
	void fitClampsForEnormousCircuits() {

		Viewport vp = new Viewport();
		Rectangle bounds = new Rectangle(0, 0, 100000, 100000);
		vp.fit(bounds, 500, 500);
		assertEquals(Viewport.MIN_SCALE, vp.getScale());
		Point2D.Double center = vp.toScreen(50000.0, 50000.0);
		assertEquals(250.0, center.x, 0.5);
		assertEquals(250.0, center.y, 0.5);
	}

	/** Degenerate bounds or view sizes reset to the identity. */
	@Test
	void fitDegeneratesToIdentity() {

		Viewport vp = new Viewport();
		vp.zoomTo(2.0, 50, 50);
		vp.fit(null, 400, 300);
		assertTrue(vp.isIdentity());

		vp.zoomTo(2.0, 50, 50);
		vp.fit(new Rectangle(0, 0, 0, 10), 400, 300);
		assertTrue(vp.isIdentity());

		vp.zoomTo(2.0, 50, 50);
		vp.fit(new Rectangle(0, 0, 10, 10), 0, 300);
		assertTrue(vp.isIdentity());
	}

	// ---- clamping helper ----

	/** clampScale pins values to [0.25, 4.0] and passes the rest. */
	@Test
	void clampScaleLimits() {

		assertEquals(Viewport.MIN_SCALE, Viewport.clampScale(0.0));
		assertEquals(Viewport.MIN_SCALE, Viewport.clampScale(0.25));
		assertEquals(1.0, Viewport.clampScale(1.0));
		assertEquals(Viewport.MAX_SCALE, Viewport.clampScale(4.0));
		assertEquals(Viewport.MAX_SCALE, Viewport.clampScale(99.0));
	}

	// ---- model geometry independence (hypothesis section 4) ----

	/**
	 * The core of the issue #74 hypothesis, in miniature: a grid-snap
	 * computed in model space after one inversion is identical at any
	 * zoom. Simulates clicking the integer screen pixel over the
	 * grid point (72, 36) at randomized zoom/pan and snapping to the
	 * 12-pixel grid - the result must always be the same model point
	 * a 100% click produces. (At 25% one screen pixel spans four model
	 * units, so the combined rounding error is at most ~2.5 model
	 * units - well inside the 6-unit snap radius around a grid
	 * point.)
	 */
	@Test
	void gridSnapInModelSpaceIsZoomInvariant() {

		final int spacing = 12; // JLSInfo.spacing
		Random rnd = new Random(SEED + 7);
		for (int i = 0; i < 1000; i++) {
			Viewport vp = randomViewport(rnd);

			// integer screen pixel nearest model point (72, 36)
			Point2D.Double s = vp.toScreen(72.0, 36.0);
			Point click = new Point((int)Math.round(s.x),
					(int)Math.round(s.y));

			// invert once, snap in model space
			Point model = vp.toModel(click);
			int snapX = Math.round(model.x / (float)spacing) * spacing;
			int snapY = Math.round(model.y / (float)spacing) * spacing;

			assertEquals(72, snapX, "snap x at scale "
					+ vp.getScale() + ", case " + i);
			assertEquals(36, snapY, "snap y at scale "
					+ vp.getScale() + ", case " + i);
		}
	}

	/** isIdentity goes false when zoomed or panned, true after reset. */
	@Test
	void isIdentityTracksState() {

		Viewport vp = new Viewport();
		vp.pan(1, 0);
		assertFalse(vp.isIdentity());
		vp.reset();
		assertTrue(vp.isIdentity());
		vp.zoomTo(2.0, 0, 0);
		assertFalse(vp.isIdentity());
		vp.reset();
		assertTrue(vp.isIdentity());
	}

} // end of ViewportTest class
