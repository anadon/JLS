package jls.elem;

/**
 * Pure state-machine geometry: the angle from a rectangle's dimensions,
 * used to orient transition arrows. Headless by construction (issue
 * #77) - the arrowhead *rendering* that used to live here moved to the
 * GUI-drawing side (State.drawArrow), so this class carries no AWT.
 */
public class SMUtil {

	/**
	 * Creates a helper instance; all functionality is in static methods.
	 */
	public SMUtil() {
	} // end of constructor

	/**
	 * Compute angle given a height and width of a rectangle.
	 * 
	 * @param w The width.
	 * @param h The height.
	 * 
	 * @return the angle, in degrees.
	 */
	public static double getAngle(int w, int h) {

		double angle;

		// special cases
		if (w == 0) {
			if (h > 0)
				angle = 90;
			else
				angle = 270;
		}
		else if (h == 0) {
			if (w > 0)
				angle = 0;
			else
				angle = 180;
		}
		else

			// general case, but doesn't understand which quadrant
			angle = Math.toDegrees(Math.atan(Math.abs(h)*1.0/Math.abs(w)));

		// adjust for quadrant
		if (w < 0 && h > 0)
			angle = 180 - angle;
		else if (w < 0 && h < 0)
			angle = 180 + angle;
		else if (w > 0 && h < 0)
			angle = 360-angle;
		return angle;
	} // end of getAngle method

} // end of SMUtil class
