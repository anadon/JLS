package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.GridTransform;
import jls.elem.Input;
import jls.elem.Mux;

/**
 * GUI-side drawing for {@link Mux} (issue #77): the trapezoid symbol, the
 * per-orientation input index labels, and the puts. Moved verbatim from
 * the former {@code Mux.draw}, with field reads replaced by the element's
 * public accessors so the model stays headless.
 */
public final class MuxRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Mux mux = (Mux) el;
		int x = mux.getX();
		int y = mux.getY();
		int width = mux.getWidth();
		int numInputs = mux.getNumInputs();
		Orientation outputOrientation = mux.getOutputOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, mux);

		// draw shape: the conventional trapezoid symbol (#123), wide side
		// at the inputs, narrow side at the output. Canonical segments
		// (output RIGHT) are mapped through the orientation transform
		// (#24). The slant is half a grid unit, so the narrow side still
		// spans every put on it and the selector's put circle (radius
		// pointDiameter/2) still touches the slanted edge at its put.
		int s = Geometry.SPACING;
		g.setColor(Color.black);
		GridTransform.Chain t = mux.placement();
		int slant = s/2;
		int ch = (numInputs+1)*s;					// canonical height
		drawMapped(g,t,x,y,0,0,0,ch);				// input (wide) side
		drawMapped(g,t,x,y,2*s,slant,2*s,ch-slant);	// output (narrow) side
		drawMapped(g,t,x,y,0,0,2*s,slant);			// top slant
		drawMapped(g,t,x,y,0,ch,2*s,ch-slant);		// bottom slant
		// draw inputs and labels
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		int d2 = Geometry.POINT_DIAMETER/2;
		int inum = -1; // first input is selector, so start one too small
		if(outputOrientation == Orientation.LEFT || outputOrientation == Orientation.RIGHT)
		{
			for (Input input : mux.getInputList()) {
				input.draw(g);
				if (inum >= 0) {
					g.setColor(Color.BLACK);
					if(outputOrientation == Orientation.RIGHT)
					{
						g.drawString(inum+"",x+d2,input.getY()-hi/2+ascent);
					}
					else if(outputOrientation == Orientation.LEFT)
					{
						g.drawString(inum+"",x+width-5*d2,input.getY()-hi/2+ascent);
					}
					else if(outputOrientation == Orientation.UP || outputOrientation == Orientation.DOWN)
					{
						g.drawString(inum+"",input.getX()-4,y+5*d2);
					}
				}
				inum += 1;
			}
		}
		if(outputOrientation == Orientation.UP || outputOrientation == Orientation.DOWN)
		{
			if(mux.getInputList().size() == 3)
			{

				mux.getInputList().get(0).draw(g);
				mux.getInputList().get(1).draw(g);
				mux.getInputList().get(2).draw(g);
				g.setColor(Color.BLACK);
				g.drawString("0",mux.getInputList().get(1).getX()-4,y+5*d2);
				g.drawString("1",mux.getInputList().get(2).getX()-4,y+5*d2);
			}
			else
			{
				for (Input input : mux.getInputList()) {
					input.draw(g);
					if (inum >= 0 && inum%2 == 0) {
						g.setColor(Color.BLACK);
						if(outputOrientation == Orientation.RIGHT)
						{
						g.drawString(inum+"",x+d2,input.getY()-hi/2+ascent);
						}
						else if(outputOrientation == Orientation.LEFT)
						{
							g.drawString(inum+"",x+width-5*d2,input.getY()-hi/2+ascent);
						}
						else if(outputOrientation == Orientation.UP || outputOrientation == Orientation.DOWN)
						{
							g.drawString(inum+"",input.getX()-4,y+5*d2);
						}
					}
					inum += 1;
				}
			}
		}

		// draw output
		mux.getOutputList().get(0).draw(g);

	} // end of draw method

	/**
	 * Draw a chain-mapped line offset by the element origin (issue #77):
	 * the pure grid mapping lives in {@link GridTransform.Chain#mapLine},
	 * the drawing stays here with the graphics context.
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

} // end of MuxRenderer class
