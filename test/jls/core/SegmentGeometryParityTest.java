package jls.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SegmentGeometry} to the AWT geometry it replaces (issue #77).
 * Wire and state-machine hit-testing moved off {@code java.awt.geom} onto
 * these pure-math ports; a divergence here would silently change which
 * drags, hovers, and collisions register. The test measures nothing about
 * the model - it compares each port to its {@code Line2D}/{@code Point2D}/
 * {@code Rectangle2D} original over a deterministic random sweep, so the
 * ports stay bug-for-bug identical.
 */
class SegmentGeometryParityTest {

	/** Deterministic so a failure reproduces. */
	private static final long SEED = 20260724L;

	private static double coord(Random r) {
		return r.nextInt(400) - 200;
	}

	@Test
	void ptSegDistMatchesLine2D() {
		Random r = new Random(SEED);
		for (int i = 0; i < 200_000; i += 1) {
			double x1 = coord(r);
			double y1 = coord(r);
			double x2 = coord(r);
			double y2 = coord(r);
			double px = coord(r);
			double py = coord(r);
			assertEquals(Line2D.ptSegDist(x1, y1, x2, y2, px, py),
					SegmentGeometry.ptSegDist(x1, y1, x2, y2, px, py), 0.0,
					"ptSegDist diverged at (" + x1 + "," + y1 + ")-("
							+ x2 + "," + y2 + ") p(" + px + "," + py + ")");
			assertEquals(Line2D.ptSegDistSq(x1, y1, x2, y2, px, py),
					SegmentGeometry.ptSegDistSq(x1, y1, x2, y2, px, py), 0.0,
					"ptSegDistSq diverged");
		}
	}

	@Test
	void distanceMatchesPoint2D() {
		Random r = new Random(SEED + 1);
		for (int i = 0; i < 200_000; i += 1) {
			double x1 = coord(r);
			double y1 = coord(r);
			double x2 = coord(r);
			double y2 = coord(r);
			assertEquals(Point2D.distance(x1, y1, x2, y2),
					SegmentGeometry.distance(x1, y1, x2, y2), 0.0,
					"distance diverged");
		}
	}

	@Test
	void segmentIntersectsRectangleMatchesRectangle2D() {
		Random r = new Random(SEED + 2);
		for (int i = 0; i < 200_000; i += 1) {
			// include zero/negative sizes: Rectangle2D.intersectsLine has
			// specific outcode behavior for degenerate rectangles that the
			// port must reproduce
			int rx = r.nextInt(200) - 100;
			int ry = r.nextInt(200) - 100;
			int rw = r.nextInt(60) - 5;
			int rh = r.nextInt(60) - 5;
			double x1 = coord(r);
			double y1 = coord(r);
			double x2 = coord(r);
			double y2 = coord(r);
			Rectangle rect = new Rectangle(rx, ry, rw, rh);
			assertEquals(rect.intersectsLine(x1, y1, x2, y2),
					SegmentGeometry.segmentIntersectsRectangle(
							rx, ry, rw, rh, x1, y1, x2, y2),
					"segmentIntersectsRectangle diverged for rect ("
							+ rx + "," + ry + "," + rw + "," + rh
							+ ") line (" + x1 + "," + y1 + ")-("
							+ x2 + "," + y2 + ")");
		}
	}

	@Test
	void portsAreFinite() {
		// a smoke check that the ports never return NaN for real inputs,
		// which would poison the < comparisons in the hit tests
		assertTrue(Double.isFinite(SegmentGeometry.ptSegDist(0, 0, 0, 0, 3, 4)));
		assertEquals(0.0, SegmentGeometry.distance(7, 7, 7, 7), 0.0);
	}
}
