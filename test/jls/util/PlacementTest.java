package jls.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the event-local placement math (issue #103). Pure
 * coordinate arithmetic - no window is ever shown, so these run
 * headless.
 */
class PlacementTest {

	/** A canvas of known size, never displayed. */
	private static JPanel canvas(int width, int height) {

		JPanel canvas = new JPanel();
		canvas.setSize(width, height);
		return canvas;
	}

	@Test
	void dropCentersElementOnLastKnownPosition() {

		// element 40x20 centered on (100,60): top left (80,50)
		Point p = Placement.dropPoint(canvas(400, 300), 100, 60, 40, 20);
		assertEquals(new Point(80, 50), p);
	}

	@Test
	void dropUsesIntegerCenteringForOddSizes() {

		// matches the historical p.x-width/2 (integer division)
		Point p = Placement.dropPoint(canvas(400, 300), 100, 60, 25, 15);
		assertEquals(new Point(100 - 25 / 2, 60 - 15 / 2), p);
	}

	@Test
	void originIsAValidKnownPosition() {

		Point p = Placement.dropPoint(canvas(400, 300), 0, 0, 10, 10);
		assertEquals(new Point(-5, -5), p);
	}

	@Test
	void unknownPositionFallsBackToCanvasCenter() {

		// negative coordinates mean "no position known": use the
		// center of the canvas
		Point p = Placement.dropPoint(canvas(400, 300), -1, -1, 40, 20);
		assertEquals(new Point(200 - 20, 150 - 10), p);
	}

	@Test
	void partiallyUnknownPositionAlsoFallsBack() {

		Point p = Placement.anchor(canvas(400, 300), 100, -1);
		assertEquals(new Point(200, 150), p);
	}

	@Test
	void missingCanvasAndPositionNeverCrashes() {

		// the never-crash half of the contract: no pointer, no canvas
		Point p = Placement.anchor(null, -1, -1);
		assertEquals(new Point(0, 0), p);
	}

	@Test
	void anchorPrefersTheKnownPositionOverTheCanvas() {

		Point p = Placement.anchor(canvas(400, 300), 33, 44);
		assertEquals(new Point(33, 44), p);
	}
}
