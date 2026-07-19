package jls.util;

import java.awt.Component;
import java.awt.Point;

/**
 * Event-local placement math (issue #103).
 *
 * Element creation used to place the new element by reading the global
 * pointer position and subtracting the editor window's screen origin.
 * That idiom can throw by API contract (the pointer-info read is
 * specified to return null with no pointer) and is meaningless on Wayland,
 * where clients see no global coordinates at all.  This helper derives
 * the same answer from information the editor already has: the last
 * mouse position it observed via ordinary MouseEvents, in the canvas's
 * own coordinate system.
 *
 * Contract: the element is centered on the last known cursor position
 * in the canvas; when no position is known the center of the canvas is
 * used; a missing pointer can never cause a crash.
 *
 * @author David A. Poplawski
 */
public final class Placement {

	/**
	 * Prevents instantiation; this class holds only static helpers.
	 */
	// no instances
	private Placement() { }

	/**
	 * Pick the anchor point for a placement: the last known mouse
	 * position if there is one, otherwise the center of the canvas.
	 *
	 * @param canvas The canvas (editor window) the placement is in, or null.
	 * @param x The x-coordinate of the last known mouse position, in
	 *        canvas coordinates, or negative if unknown.
	 * @param y The y-coordinate of the last known mouse position, in
	 *        canvas coordinates, or negative if unknown.
	 *
	 * @return the anchor point, in canvas coordinates.
	 *
	 * @jls.testedby jls.util.PlacementTest#anchorPrefersTheKnownPositionOverTheCanvas()
	 * @jls.testedby jls.util.PlacementTest#missingCanvasAndPositionNeverCrashes()
	 * @jls.testedby jls.util.PlacementTest#partiallyUnknownPositionAlsoFallsBack()
	 */
	public static Point anchor(Component canvas, int x, int y) {

		if (x >= 0 && y >= 0) {
			return new Point(x,y);
		}
		if (canvas != null) {
			return new Point(canvas.getWidth()/2,canvas.getHeight()/2);
		}
		return new Point(0,0);
	} // end of anchor method

	/**
	 * Compute the top left corner that centers an element of the given
	 * size on the last known mouse position (or the canvas center when
	 * no position is known).
	 *
	 * @param canvas The canvas (editor window) the element goes in, or null.
	 * @param x The x-coordinate of the last known mouse position, in
	 *        canvas coordinates, or negative if unknown.
	 * @param y The y-coordinate of the last known mouse position, in
	 *        canvas coordinates, or negative if unknown.
	 * @param width The width of the element.
	 * @param height The height of the element.
	 *
	 * @return the top left corner for the element, in canvas coordinates.
	 *
	 * @jls.testedby jls.util.PlacementTest#dropCentersElementOnLastKnownPosition()
	 * @jls.testedby jls.util.PlacementTest#dropUsesIntegerCenteringForOddSizes()
	 * @jls.testedby jls.util.PlacementTest#originIsAValidKnownPosition()
	 * @jls.testedby jls.util.PlacementTest#unknownPositionFallsBackToCanvasCenter()
	 */
	public static Point dropPoint(Component canvas, int x, int y,
			int width, int height) {

		Point anchor = anchor(canvas,x,y);
		return new Point(anchor.x-width/2,anchor.y-height/2);
	} // end of dropPoint method

} // end of Placement class
