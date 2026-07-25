package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import jls.core.GridPoint;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.elem.SMUtil;
import jls.elem.State;
import jls.elem.State.Transition;

/**
 * GUI-side drawing for a state machine's {@link State} (issue #77).
 * Moved verbatim from the former {@code State.draw}/{@code drawCond}/
 * {@code drawArrow}/{@code getBounds}/{@code boundCond} and the output
 * print helpers, with field reads replaced by the state's public
 * accessors so the model stays headless. States draw only in the editor
 * dialog and in printing, never on the circuit canvas, so nothing here
 * reaches the circuit's image/SVG goldens.
 */
public final class StateRenderer {

	/**
	 * No instances.
	 */
	private StateRenderer() {} // no instances

	/**
	 * Draw a state - its circle, name and outward transitions.
	 *
	 * @param g The Graphics object to use.
	 * @param state The state to draw.
	 */
	public static void draw(Graphics g, State state) {

		// set up
		int x = state.getX();
		int y = state.getY();
		int d = state.getDiameter();
		int r = d/2;
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = ascent + descent;

		// draw transitions
		for (Transition tr : state.getTransitions()) {

			// set line color
			Color color = Color.black;
			if (tr.highlighted) {
				color = JLSInfo.Palette.highlightColor;
			}

			// if no midpoints, draw straight from this state to the other
			if (tr.points.isEmpty()) {

				// draw line from start state edge to end state edge
				int w = tr.nextState.getX() - x;
				int h = y - tr.nextState.getY();
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist); // start edge
				int dyf = (int)Math.rint(h*r/dist);
				int or = tr.nextState.getDiameter()/2;
				int dxt = (int)Math.rint(w*or/dist);
				int dyt = (int)Math.rint(h*or/dist);
				int endx = tr.nextState.getX()-dxt;	// end edge
				int endy = tr.nextState.getY()+dyt;
				g.setColor(color);
				g.drawLine(x+dxf,y-dyf,endx,endy);

				// draw arrow
				double angle = SMUtil.getAngle(w,h);
				drawArrow(endx,endy,angle,g);

				// draw condition info
				int midx = (x+dxf+endx)/2;
				int midy = (y-dyf+endy)/2;
				drawCond(tr,midx,midy,angle,g);
			}

			// otherwise draw transition in segments
			else {

				// draw first segment
				int nx = tr.points.get(0).x();
				int ny = tr.points.get(0).y();
				int w = nx - x;
				int h = y - ny;
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist);
				int dyf = (int)Math.rint(h*r/dist);
				g.setColor(color);
				g.drawLine(x+dxf,y-dyf,nx,ny);

				// draw condition info
				int midx = (x+dxf+nx)/2;
				int midy = (y-dyf+ny)/2;
				drawCond(tr,midx,midy,SMUtil.getAngle(w,h),g);

				// draw all but last segment
				int px = nx;
				int py = ny;
				for (int i=1; i<tr.points.size(); i+=1) {
					nx = tr.points.get(i).x();
					ny = tr.points.get(i).y();
					g.drawLine(px,py,nx,ny);
					px = nx;
					py = ny;
				}

				// draw last segment
				w = tr.nextState.getX() - px;
				h = py - tr.nextState.getY();
				dist = Math.sqrt(w*w+h*h);
				int or = tr.nextState.getDiameter()/2;
				int dxt = (int)Math.rint(w*or/dist);
				int dyt = (int)Math.rint(h*or/dist);
				int endx = tr.nextState.getX()-dxt;	// end edge
				int endy = tr.nextState.getY()+dyt;
				g.drawLine(px,py,endx,endy);

				// draw arrow
				double angle = SMUtil.getAngle(w,h);
				drawArrow(endx,endy,angle,g);

				// draw highlight point if there is one
				if (tr.highlight != null) {
					int pd = Geometry.POINT_DIAMETER;
					g.setColor(JLSInfo.Palette.highlightColor);
					g.fillOval(tr.highlight.x()-pd/2,tr.highlight.y()-pd/2,pd,pd);
				}
			}
		}

		// show initial if necessary
		if (state.isInitial()) {
			g.setColor(JLSInfo.Palette.initialStateColor);
			g.fillOval(x-r,y-r,d+1,d+1);
		}

		// highlight if necessary
		if (state.isHighlighted()) {
			g.setColor(JLSInfo.Palette.highlightColor);
			g.fillOval(x-r,y-r,d+1,d+1);
		}

		// draw circle
		g.setColor(Color.BLACK);
		g.drawOval(x-r,y-r,d,d);

		// draw name
		String name = state.getName();
		int width = fm.stringWidth(name);
		g.drawString(name,x-width/2,y-height/2+ascent);

	} // end of draw method

	/**
	 * Draw the condition information on a transition.
	 *
	 * @param trans A transition.
	 * @param x The x-coordinate of the middle of a line segment.
	 * @param y The y-coordinate of the middle of a line segment.
	 * @param angle The angle of a line segment (in degrees).
	 * @param g The Graphics object to use.
	 */
	public static void drawCond(Transition trans, int x, int y, double angle,
			Graphics g) {

		String cond = "";
		if (trans.other) {
			cond = "else";
		}
		else if (!trans.unconditional) {
			String rel = " != ";
			if (trans.equal) {
				rel = " = ";
			}
			cond = trans.signal + "[" + trans.bits + "]"+ rel + trans.value;
		}
		FontMetrics fm = g.getFontMetrics();
		int descent = fm.getDescent();
		int width = fm.stringWidth(cond);

		if (angle == 0.0 || angle == 180) {
			g.drawString(cond,x-width/2,y-descent-1);
		}
		else if (0.0 < angle && angle <= 90.0 || 180.0 < angle && angle < 270.0) {
			g.drawString(cond,x-width,y-descent-1);
		}
		else {
			g.drawString(cond,x+1,y-descent-1);
		}
	} // end of drawCond method

	/**
	 * Draw a transition arrowhead at a point and angle. Moved from the
	 * former {@code State.drawArrow} (issue #77).
	 *
	 * @param x The x-coordinate of the end point of the arrow.
	 * @param y The y-coordinate of the end point of the arrow.
	 * @param angle The angle the arrow points.
	 * @param g The Graphics object to use.
	 */
	public static void drawArrow(int x, int y, double angle, Graphics g) {

		int p = Geometry.ARROW_SIZE;
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

	/**
	 * Get the bounds of a state and all of its outward transitions.
	 *
	 * @param g The graphics object to use.
	 * @param state The state.
	 *
	 * @return The bounds.
	 */
	public static Rectangle getBounds(Graphics g, State state) {

		// add circle to bounds
		int x = state.getX();
		int y = state.getY();
		int d = state.getDiameter();
		int r = d/2;
		Rectangle bounds = new Rectangle(x-r,y-r,d,d);

		// add transition to bounds
		for (Transition tr : state.getTransitions()) {

			// add points to bounds
			for (GridPoint p : tr.points) {
				bounds.add(p.x(), p.y());
			}

			// add condition to bounds
			if (tr.points.isEmpty()) {

				// direct
				int w = tr.nextState.getX() - x;
				int h = y - tr.nextState.getY();
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist); // start edge
				int dyf = (int)Math.rint(h*r/dist);
				int or = tr.nextState.getDiameter()/2;
				int dxt = (int)Math.rint(w*or/dist);
				int dyt = (int)Math.rint(h*or/dist);
				int endx = tr.nextState.getX()-dxt;	// end edge
				int endy = tr.nextState.getY()+dyt;
				double angle = SMUtil.getAngle(w,h);
				int midx = (x+dxf+endx)/2;
				int midy = (y-dyf+endy)/2;
				bounds.add(boundCond(tr,midx,midy,angle,g));
			}

			// otherwise get bounds of segments
			else {

				// draw first segment
				int nx = tr.points.get(0).x();
				int ny = tr.points.get(0).y();
				int w = nx - x;
				int h = y - ny;
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist);
				int dyf = (int)Math.rint(h*r/dist);
				int midx = (x+dxf+nx)/2;
				int midy = (y-dyf+ny)/2;
				bounds.add(boundCond(tr,midx,midy,SMUtil.getAngle(w,h),g));
			}
		}
		return bounds;
	} // end of getBounds method

	/**
	 * Get the bounds on a transition's condition. If g is null, then
	 * return a 0x0 rectangle at x,y.
	 *
	 * @param trans A transition.
	 * @param x The x-coordinate of the middle of a line segment.
	 * @param y The y-coordinate of the middle of a line segment.
	 * @param angle The angle of a line segment (in degrees).
	 * @param g The Graphics object to use.
	 *
	 * @return The bounds of the condition on this transition.
	 */
	public static Rectangle boundCond(Transition trans, int x, int y,
			double angle, Graphics g) {

		if (g == null) {
			return new Rectangle(x,y,0,0);
		}
		String cond = "";
		if (trans.other) {
			cond = "else";
		}
		else if (!trans.unconditional) {
			String rel = " != ";
			if (trans.equal) {
				rel = " = ";
			}
			cond = trans.signal + "[" + trans.bits + "]"+ rel + trans.value;
		}
		FontMetrics fm = g.getFontMetrics();
		int descent = fm.getDescent();
		int height = descent + fm.getAscent();
		int width = fm.stringWidth(cond);

		if (angle == 0.0 || angle == 180) {
			return new Rectangle(x-width/2,y,width,height);
		}
		else if (0.0 < angle && angle <= 90.0 || 180.0 < angle && angle < 270.0) {
			return new Rectangle(x-width,y,width,height);
		}
		else {
			return new Rectangle(x,y,width,height);
		}
	} // end of boundCond method

	/**
	 * Determine the maximum printing width of a state.
	 *
	 * @param g The Graphics object to use to determine sizes.
	 * @param state The state.
	 *
	 * @return the maximum printing width.
	 */
	public static int maxWidth(Graphics g, State state) {

		FontMetrics fm = g.getFontMetrics();
		int max = fm.stringWidth("state: " + state.getName());
		for (State.Out out : state.getOuts()) {
			max = Math.max(max,fm.stringWidth(out.signal + "=" + out.value));
		}
		return max;
	} // end of maxWidth method

	/**
	 * Determine the maximum printing height of a state.
	 *
	 * @param g The Graphics object to use to determine sizes.
	 * @param state The state.
	 *
	 * @return the maximum printing height.
	 */
	public static int maxHeight(Graphics g, State state) {

		FontMetrics fm = g.getFontMetrics();
		int height = fm.getAscent() + fm.getDescent();
		return (state.getOuts().size()+1)*height;
	} // end of maxHeight method

	/**
	 * Print a state's outputs.
	 *
	 * @param g The Graphics object to use.
	 * @param state The state.
	 * @param x The x-coordinate of the upper left corner of the print area.
	 * @param y The y-coordinate of the upper left corner of the print area.
	 */
	public static void print(Graphics g, State state, int x, int y) {

		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int height = ascent + fm.getDescent();
		g.drawString("State: " + state.getName(),x,y+ascent);
		y += height;
		for (State.Out out : state.getOuts()) {
			g.drawString(out.signal + "=" + out.value,x,y+ascent);
			y += height;
		}
	} // end of print method

} // end of StateRenderer class
