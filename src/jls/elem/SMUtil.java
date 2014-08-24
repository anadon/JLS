package jls.elem;


import java.awt.*;
import java.awt.geom.*;
import jls.*;


public class SMUtil {

	/**
	 * Compute angle given a height and width of a rectangle.
	 * 
	 * @param w The width.
	 * @param h The height.
	 * 
	 * @return the angle, in degrees.
	 */
	static double getAngle(int w, int h) {

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

	/**
	 * Draw an arrow at a given point and angle.
	 * 
	 * @param x The x-coordinate of the end point of the arrow.
	 * @param y The y-coordinate of the end point of the arrow.
	 * @param angle The angle the arrow points.
	 * @param g The Graphics object to use.
	 */
	public static void drawArrow(int x, int y, double angle, Graphics g) {

		int p = JLSInfo.arrowSize;
		Line2D top = new Line2D.Double(-p,-p,0,0);
		Line2D bottom = new Line2D.Double(0,0,-p,p);
		Line2D back = new Line2D.Double(-p,p,-p,-p);
		GeneralPath arrow = new GeneralPath(top);
		arrow.append(bottom,true);
		arrow.append(back,true);
		arrow.closePath();
		AffineTransform trans = new AffineTransform();
		trans.translate(x,y);
		trans.rotate(Math.toRadians(-angle));
		arrow.transform(trans);
		Graphics2D gg = (Graphics2D)g;
		g.setColor(Color.black);
		gg.fill(arrow);

	} // end of drawArrow method

} // end of SMUtil class
