package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jls.elem.Element;

/**
 * The bridge between the machine-checked proofs in
 * proofs/SpatialIndexCorrectness.agda and the Java implementation.
 *
 * The Agda development proves two theorems over an idealized model:
 * query-parity (THEOREM 1: the grid query equals a brute-force
 * boundsTouch scan) and culling-parity (THEOREM 2: index-driven draw
 * culling selects exactly the elements a full mayBeVisible scan
 * selects). Those theorems hold for the real code only if the model's
 * assumptions (A1)-(A5) describe what the Java actually does -- each
 * test here pins one assumption to the implementation, and is named
 * after it. The theorem-level statements themselves are exercised
 * end-to-end by SpatialIndexTest (THEOREM 1) and DrawCullingParityTest
 * (THEOREM 2).
 *
 * Scope note: the model works in unbounded integers; the tests sample
 * coordinates well inside int range, matching the editor's canvas
 * coordinates (overflow is out of scope for both model and editor).
 */
class ProofBridgeTest {

	/** Deterministic seed: failures must reproduce. */
	private static final long SEED = 20260717L;

	private static int cellSize() throws Exception {
		Field cell = SpatialIndex.class.getDeclaredField("CELL");
		cell.setAccessible(true);
		return cell.getInt(null);
	}

	private static boolean boundsTouch(Rectangle a, Rectangle b)
			throws Exception {
		Method m = SpatialIndex.class.getDeclaredMethod("boundsTouch",
				Rectangle.class, Rectangle.class);
		m.setAccessible(true);
		return (Boolean) m.invoke(null, a, b);
	}

	private static int drawMargin() throws Exception {
		Field margin = Circuit.class.getDeclaredField("DRAW_MARGIN");
		margin.setAccessible(true);
		return margin.getInt(null);
	}

	private static boolean mayBeVisible(Element el, Rectangle clip)
			throws Exception {
		Method m = Circuit.class.getDeclaredMethod("mayBeVisible",
				Element.class, Rectangle.class);
		m.setAccessible(true);
		return (Boolean) m.invoke(null, el, clip);
	}

	/**
	 * (A1) Index intervals are non-empty: every element's index bounds
	 * have non-negative width and height (the model's Ival.sane field),
	 * and they stay that way as elements move the way a drag moves them.
	 */
	@Test
	void a1IndexIntervalsAreNonEmpty() throws Exception {

		Circuit circuit = SpatialIndexTest.loadWired();
		Random random = new Random(SEED);
		for (int round = 0; round < 20; round += 1) {
			for (Element el : circuit.getElements()) {
				Rectangle b = el.getIndexBounds();
				assertTrue(b.width >= 0 && b.height >= 0,
						"index bounds must be non-empty (A1): " + b + " for " + el);
				if (random.nextBoolean()) {
					el.move(JLSInfo.spacing * (random.nextInt(5) - 2),
							JLSInfo.spacing * (random.nextInt(5) - 2));
				}
			}
		}
	}

	/**
	 * (A2) The cell function Math.floorDiv(_, CELL) is monotone -- the
	 * only property of the bucketing the completeness proof
	 * (query-complete) uses. Checked exhaustively on adjacent pairs
	 * around zero (adjacent monotonicity implies monotonicity) and on
	 * random ordered pairs, at the index's real cell size.
	 */
	@Test
	void a2CellFunctionIsMonotone() throws Exception {

		int cell = cellSize();
		assertTrue(cell > 0, "cell size must be positive: " + cell);

		for (int i = -5000; i < 5000; i += 1) {
			assertTrue(Math.floorDiv(i, cell) <= Math.floorDiv(i + 1, cell),
					"floorDiv must be monotone at " + i);
		}

		Random random = new Random(SEED + 1);
		for (int trial = 0; trial < 10000; trial += 1) {
			int i = random.nextInt(2_000_001) - 1_000_000;
			int j = random.nextInt(2_000_001) - 1_000_000;
			int lo = Math.min(i, j);
			int hi = Math.max(i, j);
			assertTrue(Math.floorDiv(lo, cell) <= Math.floorDiv(hi, cell),
					"floorDiv must be monotone on (" + lo + "," + hi + ")");
		}
	}

	/**
	 * (A3) SpatialIndex.boundsTouch is the closed-interval overlap test
	 * of the model (Touch, per axis): lo_a <= hi_b and lo_b <= hi_a on
	 * both axes, counting zero-area contact. Sampled over random
	 * rectangles including zero-width/height ones (wire spans, points).
	 */
	@Test
	void a3BoundsTouchIsClosedIntervalOverlap() throws Exception {

		Random random = new Random(SEED + 2);
		for (int trial = 0; trial < 20000; trial += 1) {
			Rectangle a = new Rectangle(random.nextInt(200) - 100,
					random.nextInt(200) - 100, random.nextInt(40), random.nextInt(40));
			Rectangle b = new Rectangle(random.nextInt(200) - 100,
					random.nextInt(200) - 100, random.nextInt(40), random.nextInt(40));
			boolean model = a.x <= b.x + b.width && b.x <= a.x + a.width
					&& a.y <= b.y + b.height && b.y <= a.y + a.height;
			assertEquals(model, boundsTouch(a, b),
					"boundsTouch must match the model Touch for " + a + " vs " + b);
		}
	}

	/**
	 * (A4) java.awt semantics the culling model relies on:
	 * Rectangle.grow(m,m) maps the interval [lo, hi] to [lo-m, hi+m] on
	 * each axis, and Rectangle.intersects on non-degenerate rectangles
	 * is the open-interval overlap test (Cross: strict inequalities).
	 */
	@Test
	void a4AwtGrowAndIntersectsMatchModel() {

		Random random = new Random(SEED + 3);
		for (int trial = 0; trial < 20000; trial += 1) {
			int x = random.nextInt(400) - 200;
			int y = random.nextInt(400) - 200;
			int w = random.nextInt(60);
			int h = random.nextInt(60);
			int m = random.nextInt(100);
			Rectangle grown = new Rectangle(x, y, w, h);
			grown.grow(m, m);
			assertEquals(x - m, grown.x, "grow must move lo down by m");
			assertEquals(y - m, grown.y, "grow must move lo down by m");
			assertEquals(x + w + m, grown.x + grown.width,
					"grow must move hi up by m");
			assertEquals(y + h + m, grown.y + grown.height,
					"grow must move hi up by m");

			Rectangle a = new Rectangle(random.nextInt(200) - 100,
					random.nextInt(200) - 100,
					random.nextInt(40) + 1, random.nextInt(40) + 1);
			Rectangle b = new Rectangle(random.nextInt(200) - 100,
					random.nextInt(200) - 100,
					random.nextInt(40) + 1, random.nextInt(40) + 1);
			boolean cross = a.x < b.x + b.width && b.x < a.x + a.width
					&& a.y < b.y + b.height && b.y < a.y + a.height;
			assertEquals(cross, a.intersects(b),
					"intersects must match the model Cross for " + a + " vs " + b);
		}
	}

	/**
	 * (A5) The draw margin is non-negative (the model needs 0 <= m to
	 * keep the grown clip a valid interval), and Circuit.mayBeVisible
	 * computes exactly the model's MayBeVisible: the element's index
	 * bounds grown by the margin, strictly intersected with the clip.
	 */
	@Test
	void a5DrawMarginAndMayBeVisibleMatchModel() throws Exception {

		int m = drawMargin();
		assertTrue(m >= 0, "draw margin must be non-negative: " + m);

		Circuit circuit = SpatialIndexTest.loadWired();
		Random random = new Random(SEED + 4);
		for (int trial = 0; trial < 2000; trial += 1) {
			Rectangle clip = new Rectangle(random.nextInt(900) - 150,
					random.nextInt(600) - 150,
					random.nextInt(300) + 1, random.nextInt(300) + 1);
			for (Element el : circuit.getElements()) {
				Rectangle b = el.getIndexBounds();
				long loX = (long) b.x - m;
				long hiX = (long) b.x + b.width + m;
				long loY = (long) b.y - m;
				long hiY = (long) b.y + b.height + m;
				boolean model = loX < clip.x + (long) clip.width
						&& clip.x < hiX
						&& loY < clip.y + (long) clip.height
						&& clip.y < hiY;
				assertEquals(model, mayBeVisible(el, clip),
						"mayBeVisible must match the model for " + b + " vs " + clip);
			}
		}
	}
}
