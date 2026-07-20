package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.elem.AndGate;
import jls.elem.Element;

/**
 * Layer-2 zoom characterization (issue #74): boots a real
 * {@link jls.edit.Editor} and exercises the editor wiring of the
 * {@link jls.edit.Viewport} - the keyboard zoom ladder, actual-size
 * reset, and, crucially, hit-testing and element movement under a
 * non-identity view transform (the issue's §4 hypothesis / prediction
 * P1: model geometry produced by a gesture is identical whether the
 * gesture is performed at 100% or at a zoomed scale).
 *
 * Synthetic {@link java.awt.event.MouseEvent}s carry canvas-relative
 * component coordinates; the editor inverts them once through the view
 * transform, so a drag whose component delta is {@code modelDelta *
 * scale} must move the model element by exactly {@code modelDelta}. That
 * is the property these tests pin.
 *
 * Tagged {@code display}: runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false), self-skips
 * headless.
 */
@Tag("display")
@Timeout(60)
class EditorZoomTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/** A circuit with a single AND gate near the top-left of the canvas. */
	private static Circuit oneGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static int centerX(Element el) {
		return el.getX() + el.getRect().width / 2;
	}

	private static int centerY(Element el) {
		return el.getY() + el.getRect().height / 2;
	}

	/** Serialize a circuit to its on-disk text, deterministically. */
	private static String serialize(Circuit c) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			c.save(pw);
		}
		return sw.toString();
	}

	/**
	 * Drive a single ladder step at a time until the editor's scale hits
	 * the requested ladder stop. Only ladder values reachable by
	 * {@link EditorGestureSupport#zoomIn()} / {@code zoomOut()} are valid
	 * targets.
	 */
	private static void zoomTo(EditorGestureSupport ui, double target)
			throws Exception {
		int guard = 0;
		while (ui.zoomScale() < target - 1e-9 && guard++ < 20) {
			ui.zoomIn();
		}
		while (ui.zoomScale() > target + 1e-9 && guard++ < 40) {
			ui.zoomOut();
		}
		assertEquals(target, ui.zoomScale(), 1e-9,
				"reached ladder stop " + target);
	}

	/** Ladder scales chosen so model*scale lands on integers for (72,120,96). */
	private static final double[] SCALES =
			{0.25, 0.5, 0.75, 1.5, 2.0, 3.0, 4.0};

	@Test
	void keyboardLadderAndResetHitExactStops() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			assertEquals(1.0, ui.zoomScale(), 1e-9, "fresh editor is 100%");
			ui.zoomIn();
			assertEquals(1.5, ui.zoomScale(), 1e-9, "one stop up is 150%");
			ui.zoomIn();
			assertEquals(2.0, ui.zoomScale(), 1e-9, "two stops up is 200%");
			ui.zoomReset();
			assertEquals(1.0, ui.zoomScale(), 1e-9, "reset is exactly 100%");
			ui.zoomOut();
			assertEquals(0.75, ui.zoomScale(), 1e-9, "one stop down is 75%");
		}
	}

	@Test
	void zoomIsClampedToTheAdjudicatedRange() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			for (int i = 0; i < 12; i++) {
				ui.zoomIn();
			}
			assertEquals(4.0, ui.zoomScale(), 1e-9, "clamped to 400% at top");
			for (int i = 0; i < 12; i++) {
				ui.zoomOut();
			}
			assertEquals(0.25, ui.zoomScale(), 1e-9,
					"clamped to 25% at bottom");
		}
	}

	@Test
	void dragMovesElementByModelDeltaAtTwoHundredPercent() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			int fromMX = centerX(gate), fromMY = centerY(gate);

			// zoom to 200%; the point under the (screen-fixed) view center
			// changes, but hit-testing must still invert to model space
			ui.zoomIn();
			ui.zoomIn();
			assertEquals(2.0, ui.zoomScale(), 1e-9, "at 200%");

			// a component drag of (240,192) at scale 2 is a model drag of
			// (120,96) - the same model delta the 100% suite uses
			double s = ui.zoomScale();
			int fromCX = (int) Math.round(fromMX * s);
			int fromCY = (int) Math.round(fromMY * s);
			int toCX = fromCX + (int) Math.round(120 * s);
			int toCY = fromCY + (int) Math.round(96 * s);
			ui.leftDrag(fromCX, fromCY, toCX, toCY);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved");

			GeometryAssert.assertOnGrid(gate);
			assertTrue(Math.abs(gate.getX() - (beforeX + 120)) <= 12
					&& Math.abs(gate.getY() - (beforeY + 96)) <= 12,
					"gate moved by the model delta despite zoom: "
							+ gate.getX() + "," + gate.getY());
		}
	}

	@Test
	void sameGestureYieldsSameModelGeometryAtAnyZoom() throws Exception {
		// P1: run the identical model-delta drag at 100% and at 200%; the
		// resulting gate position must be identical.
		int at100X, at100Y;

		Circuit c1 = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(c1)) {
			AndGate gate = assertElementPresent(c1, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			int fromMX = centerX(gate), fromMY = centerY(gate);
			ui.leftDrag(fromMX, fromMY, fromMX + 120, fromMY + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved at 100%");
			at100X = gate.getX();
			at100Y = gate.getY();
		}

		Circuit c2 = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(c2)) {
			AndGate gate = assertElementPresent(c2, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			int fromMX = centerX(gate), fromMY = centerY(gate);
			ui.zoomIn();
			ui.zoomIn();
			double s = ui.zoomScale();
			int fromCX = (int) Math.round(fromMX * s);
			int fromCY = (int) Math.round(fromMY * s);
			ui.leftDrag(fromCX, fromCY,
					fromCX + (int) Math.round(120 * s),
					fromCY + (int) Math.round(96 * s));
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved at 200%");
			assertEquals(at100X, gate.getX(),
					"same model x whether dragged at 100% or 200%");
			assertEquals(at100Y, gate.getY(),
					"same model y whether dragged at 100% or 200%");
		}
	}

	@Test
	void sameMoveYieldsSameGeometryAcrossTheWholeZoomRange() throws Exception {
		// P1 broadened: the same model-delta drag at every ladder stop
		// across [0.25, 4.0] - including the fractional-zoom rounding regime
		// the two-point test never touched - must land the gate on the
		// identical model coordinate as the 100% baseline.
		int baseX, baseY;
		Circuit base = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(base)) {
			AndGate gate = assertElementPresent(base, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			int fromMX = centerX(gate), fromMY = centerY(gate);
			ui.leftDrag(fromMX, fromMY, fromMX + 120, fromMY + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"baseline gate moved");
			baseX = gate.getX();
			baseY = gate.getY();
		}

		for (double scale : SCALES) {
			Circuit c = oneGate();
			try (EditorGestureSupport ui = new EditorGestureSupport(c)) {
				AndGate gate = assertElementPresent(c, AndGate.class);
				int beforeX = gate.getX(), beforeY = gate.getY();
				int fromMX = centerX(gate), fromMY = centerY(gate);
				zoomTo(ui, scale);
				int fromCX = (int) Math.round(fromMX * scale);
				int fromCY = (int) Math.round(fromMY * scale);
				ui.leftDrag(fromCX, fromCY,
						fromCX + (int) Math.round(120 * scale),
						fromCY + (int) Math.round(96 * scale));
				ui.waitFor(
						() -> gate.getX() != beforeX || gate.getY() != beforeY,
						"gate moved at scale " + scale);
				GeometryAssert.assertOnGrid(gate);
				assertEquals(baseX, gate.getX(),
						"model x identical at scale " + scale);
				assertEquals(baseY, gate.getY(),
						"model y identical at scale " + scale);
			}
		}
	}

	@Test
	void savedFileIsByteIdenticalRegardlessOfZoom() throws Exception {
		// P2: the same edit performed at any zoom must serialize to a
		// byte-identical file as the 100% edit (placement snaps in model
		// space, so this is true by construction - pinned here so a
		// regression that leaks screen units into the model would fail).
		String golden;
		Circuit base = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(base)) {
			AndGate gate = assertElementPresent(base, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			int fromMX = centerX(gate), fromMY = centerY(gate);
			ui.leftDrag(fromMX, fromMY, fromMX + 120, fromMY + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"baseline gate moved");
			golden = serialize(base);
		}

		for (double scale : SCALES) {
			Circuit c = oneGate();
			try (EditorGestureSupport ui = new EditorGestureSupport(c)) {
				AndGate gate = assertElementPresent(c, AndGate.class);
				int beforeX = gate.getX(), beforeY = gate.getY();
				int fromMX = centerX(gate), fromMY = centerY(gate);
				zoomTo(ui, scale);
				int fromCX = (int) Math.round(fromMX * scale);
				int fromCY = (int) Math.round(fromMY * scale);
				ui.leftDrag(fromCX, fromCY,
						fromCX + (int) Math.round(120 * scale),
						fromCY + (int) Math.round(96 * scale));
				ui.waitFor(
						() -> gate.getX() != beforeX || gate.getY() != beforeY,
						"gate moved at scale " + scale);
				assertEquals(golden, serialize(c),
						"save byte-identical to 100% edit at scale " + scale);
			}
		}
	}

	@Test
	void ctrlWheelZoomsContinuouslyAboutTheCursor() throws Exception {
		// The editor's applyZoom path (Ctrl/Cmd+wheel), distinct from the
		// keyboard ladder: one notch scales by the continuous wheel step and
		// keeps the model point under the cursor fixed on screen (P3 at the
		// editor level, not just pure Viewport.zoomTo).
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			double mx = centerX(gate), my = centerY(gate);

			double s0 = ui.zoomScale();
			assertEquals(1.0, s0, 1e-9, "starts at 100%");
			Point vp0 = ui.viewPosition();
			// at 100% the component coord of a model point equals the model
			// coord; screen position is that minus the scroll offset
			double screenX0 = mx * s0 - vp0.x;
			double screenY0 = my * s0 - vp0.y;

			ui.ctrlWheel((int) Math.round(mx * s0), (int) Math.round(my * s0),
					true);
			double s1 = ui.zoomScale();
			assertTrue(s1 > s0, "Ctrl+wheel up increased the zoom: " + s1);
			assertTrue(s1 <= 4.0 + 1e-9, "still within the clamp");
			Point vp1 = ui.viewPosition();
			double screenX1 = mx * s1 - vp1.x;
			double screenY1 = my * s1 - vp1.y;
			assertTrue(Math.abs(screenX1 - screenX0) <= 2.0
					&& Math.abs(screenY1 - screenY0) <= 2.0,
					"cursor's model point stayed fixed on screen: was ("
							+ screenX0 + "," + screenY0 + ") now (" + screenX1
							+ "," + screenY1 + ")");

			ui.ctrlWheel((int) Math.round(mx * s1), (int) Math.round(my * s1),
					false);
			assertTrue(ui.zoomScale() < s1, "Ctrl+wheel down decreased zoom");
		}
	}

	@Test
	void dragAfterWheelZoomStillMovesByModelDelta() throws Exception {
		// Hit-testing correctness specifically through the continuous
		// applyZoom (wheel) path rather than the ladder: after a Ctrl+wheel
		// zoom, a component drag inverts to the same model delta.
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			double mx = centerX(gate), my = centerY(gate);

			ui.ctrlWheel((int) Math.round(mx), (int) Math.round(my), true);
			ui.ctrlWheel((int) Math.round(mx * ui.zoomScale()),
					(int) Math.round(my * ui.zoomScale()), true);
			double s = ui.zoomScale();

			int fromCX = (int) Math.round(mx * s);
			int fromCY = (int) Math.round(my * s);
			ui.leftDrag(fromCX, fromCY, fromCX + (int) Math.round(120 * s),
					fromCY + (int) Math.round(96 * s));
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved after wheel zoom");
			GeometryAssert.assertOnGrid(gate);
			assertTrue(Math.abs(gate.getX() - (beforeX + 120)) <= 12
					&& Math.abs(gate.getY() - (beforeY + 96)) <= 12,
					"gate moved by the model delta despite wheel zoom: "
							+ gate.getX() + "," + gate.getY());
		}
	}

	@Test
	void middleDragPansTheViewWithoutMovingTheModel() throws Exception {
		// The pan gesture (issue #74): a middle-button drag scrolls the
		// view (changes the scroll position) but must never perturb model
		// geometry. Zoom in first so the view is larger than the extent and
		// there is room to scroll.
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			ui.zoomIn();
			ui.zoomIn();
			assertEquals(2.0, ui.zoomScale(), 1e-9, "at 200%");

			Point vp0 = ui.viewPosition();
			// diagonal middle-drag; the pan handler moves the view opposite
			// to the drag, and 200% leaves ample scroll room in both axes
			ui.middleDrag(600, 500, 450, 380);
			Point vp1 = ui.viewPosition();

			assertTrue(!vp0.equals(vp1),
					"middle-drag scrolled the view: " + vp0 + " -> " + vp1);
			assertEquals(beforeX, gate.getX(), "pan left model x untouched");
			assertEquals(beforeY, gate.getY(), "pan left model y untouched");
		}
	}
}
