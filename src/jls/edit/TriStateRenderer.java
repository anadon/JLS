package jls.edit;

import java.awt.Color;
import java.awt.Graphics;

import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.GridTransform;
import jls.elem.TriState;

/**
 * GUI-side drawing for {@link TriState} (issue #77): the buffer triangle,
 * the input/output/control wires, and the puts. Moved verbatim from the
 * former {@code TriState.draw}, with field reads replaced by the
 * element's public accessors so the model stays headless.
 */
public final class TriStateRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		TriState ts = (TriState) el;
		int x = ts.getX();
		int y = ts.getY();

		// draw context
		ElementRenderSupport.drawHighlight(g, ts);

		// draw gate: canonical segments (gate RIGHT, control DOWN)
		// mapped through the orientation transform (#24)
		int s = Geometry.SPACING;
		g.setColor(Color.black);
		GridTransform.Chain t = ts.placement();
		drawMapped(g, t, x, y, 0, s, s, s);				// input wire
		drawMapped(g, t, x, y, s, 0, s, 2 * s);			// back
		drawMapped(g, t, x, y, s, 0, 3 * s, s);			// top
		drawMapped(g, t, x, y, s, 2 * s, 3 * s, s);		// bottom
		drawMapped(g, t, x, y, 3 * s, s, 4 * s, s);		// output wire
		drawMapped(g, t, x, y, 2 * s, 3 * s / 2, 2 * s, 3 * s);	// control wire
		// draw inputs and outputs
		ElementRenderSupport.drawPut(g, ts.getInputList().get(0));
		ElementRenderSupport.drawPut(g, ts.getInputList().get(1));
		ElementRenderSupport.drawPut(g, ts.getOutputList().get(0));

	} // end of draw method

	/**
	 * Draw a chain-mapped line offset by the element origin: the pure grid
	 * mapping lives in {@link GridTransform.Chain#mapLine}, the drawing
	 * stays here with the graphics context.
	 *
	 * @param g the graphics to draw on.
	 * @param t the transform chain.
	 * @param ox the element origin x.
	 * @param oy the element origin y.
	 * @param x1 first endpoint canonical x.
	 * @param y1 first endpoint canonical y.
	 * @param x2 second endpoint canonical x.
	 * @param y2 second endpoint canonical y.
	 */
	private static void drawMapped(Graphics g, GridTransform.Chain t,
			int ox, int oy, int x1, int y1, int x2, int y2) {
		int[] l = t.mapLine(x1, y1, x2, y2);
		g.drawLine(ox + l[0], oy + l[1], ox + l[2], oy + l[3]);
	} // end of drawMapped method

} // end of TriStateRenderer class
