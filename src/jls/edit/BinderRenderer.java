package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Group;
import jls.elem.Input;
import jls.elem.Output;

/**
 * GUI-side drawing for {@link jls.elem.Binder} (issue #77): the split
 * line, the per-input bit-group labels, and the connecting lines to the
 * bundled output, for each of the four orientations. Moved verbatim from
 * the former {@code Binder.draw}, with field reads replaced by the
 * element's public accessors so the model stays headless.
 */
public final class BinderRenderer extends GroupRenderer {

	/**
	 * Creates a {@code BinderRenderer}.
	 */
	public BinderRenderer() {
	} // end of constructor

	@Override
	void drawBody(Graphics g, Group group) {

		int x = group.getX();
		int y = group.getY();
		int width = group.getWidth();
		int height = group.getHeight();
		Orientation orientation = group.getOrientation();

		// set up
		int d2 = Geometry.POINT_DIAMETER/2;
		int s = Geometry.SPACING;

		// draw inputs
		FontMetrics fm = g.getFontMetrics();

		if(orientation == Orientation.RIGHT)
		{
			for (Input input : group.getInputList()) {
				ElementRenderSupport.drawPut(g, input);
				int ypos = input.getY();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+Geometry.POINT_DIAMETER/2);
				g.drawString(input.getName(),edge, (int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+width-s/2,ypos,(int)(edge+t.getWidth()+d2),ypos);
			}

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,y+s,x+width-s/2,y+height-s);

			// draw output and line to it
			Output output = group.getOutputList().get(0);
			g.setColor(Color.black);
			int ypos = output.getY();
			g.drawLine(x+width,ypos,x+width-s/2,ypos);
			ElementRenderSupport.drawPut(g, output);
		}
		else if(orientation == Orientation.LEFT)
		{
			for (Input input : group.getInputList()) {
				ElementRenderSupport.drawPut(g, input);
				int ypos = input.getY();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+width-Geometry.POINT_DIAMETER/2-t.getWidth());
				g.drawString(input.getName(),edge, (int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+s/2,ypos,(int)(edge-d2),ypos);
			}

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,y+s,x+s/2,y+height-s);

			// draw output and line to it
			Output output = group.getOutputList().get(0);
			g.setColor(Color.black);
			int ypos = output.getY();
			g.drawLine(x,ypos,x+s/2,ypos);
			ElementRenderSupport.drawPut(g, output);
		}
		else if(orientation == Orientation.DOWN)
		{
			int inum = 0;
			for (Input input : group.getInputList()) {
				ElementRenderSupport.drawPut(g, input);
				int xpos = input.getX();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+Geometry.POINT_DIAMETER/2);
				if(inum%2 == 0)
				{
					g.drawString(input.getName(),xpos-(int)t.getWidth()/2, (int)(edge+t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+height-s,xpos,(int)(edge+t.getHeight()+d2));
				inum++;
			}

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+height-s,x+width-s,y+height-s);

			// draw output and line to it
			Output output = group.getOutputList().get(0);
			g.setColor(Color.black);
			int xpos = output.getX();
			g.drawLine(xpos,y+height-s,xpos,y+height);
			ElementRenderSupport.drawPut(g, output);
		}
		else if(orientation == Orientation.UP)
		{
			int inum = 0;
			for (Input input : group.getInputList()) {
				ElementRenderSupport.drawPut(g, input);
				int xpos = input.getX();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+height-Geometry.POINT_DIAMETER/2);
				if(inum%2 == 0)
				{
					g.drawString(input.getName(),xpos-(int)t.getWidth()/2, (int)(edge-t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+s,xpos,(int)(edge-t.getHeight()+d2));
				inum++;
			}

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+s,x+width-s,y+s);

			// draw output and line to it
			Output output = group.getOutputList().get(0);
			g.setColor(Color.black);
			int xpos = output.getX();
			g.drawLine(xpos,y+s,xpos,y);
			ElementRenderSupport.drawPut(g, output);
		}
	} // end of drawBody method

} // end of BinderRenderer class
