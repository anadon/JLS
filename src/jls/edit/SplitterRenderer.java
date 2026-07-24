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
 * GUI-side drawing for {@link jls.elem.Splitter} (issue #77): the split
 * line, the line in from the bundled input, and the per-output bit-group
 * labels, for each of the four orientations. Moved verbatim from the
 * former {@code Splitter.draw}, with field reads replaced by the element's
 * public accessors so the model stays headless.
 */
public final class SplitterRenderer extends GroupRenderer {

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

		if(orientation == Orientation.RIGHT)
		{
			// draw input and line from it
			Input input = group.getInputList().get(0);
			g.setColor(Color.black);
			int ypos = input.getY();
			g.drawLine(x,ypos,x+s/2,ypos);
			input.draw(g);

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,y+s,x+s/2,y+height-s);

			// draw outputs and lines to them
			FontMetrics fm = g.getFontMetrics();
			for (Output output : group.getOutputList()) {
				output.draw(g);
				ypos = output.getY();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+width-t.getWidth()-d2);
				g.drawString(output.getName(),edge,(int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+s/2,ypos,edge-d2,ypos);
			}
		}
		else if(orientation == Orientation.LEFT)
		{
			// draw input and line from it
			Input input = group.getInputList().get(0);
			g.setColor(Color.black);
			int ypos = input.getY();
			g.drawLine(x+width,ypos,x+width-s/2,ypos);
			input.draw(g);

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,y+s,x+width-s/2,y+height-s);

			// draw outputs and lines to them
			FontMetrics fm = g.getFontMetrics();
			for (Output output : group.getOutputList()) {
				output.draw(g);
				ypos = output.getY();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+Geometry.POINT_DIAMETER/2);
				g.drawString(output.getName(),edge, (int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+width-s/2,ypos,(int)(edge+t.getWidth()+d2),ypos);
			}
		}
		else if(orientation == Orientation.DOWN)
		{
			int inum = 0;
			FontMetrics fm = g.getFontMetrics();
			for (Output output : group.getOutputList()) {
				output.draw(g);
				int xpos = output.getX();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+height-Geometry.POINT_DIAMETER/2);
				if(inum%2 == 0)
				{
					g.drawString(output.getName(),xpos-(int)t.getWidth()/2, (int)(edge-t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+s,xpos,(int)(edge-t.getHeight()+d2));
				inum++;
			}

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+s,x+width-s,y+s);

			// draw output and line to it
			Input input = group.getInputList().get(0);
			g.setColor(Color.black);
			int xpos = input.getX();
			g.drawLine(xpos,y+s,xpos,y);
			input.draw(g);
		}
		else if(orientation == Orientation.UP)
		{
			int inum = 0;
			FontMetrics fm = g.getFontMetrics();
			for (Output output : group.getOutputList()) {
				output.draw(g);
				int xpos = output.getX();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+Geometry.POINT_DIAMETER/2);
				if(inum%2 == 0)
				{
					g.drawString(output.getName(),xpos-(int)t.getWidth()/2, (int)(edge+t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+height-s,xpos,(int)(edge+t.getHeight()+d2));
				inum++;
			}

			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+height-s,x+width-s,y+height-s);

			// draw output and line to it
			Input input = group.getInputList().get(0);
			g.setColor(Color.black);
			int xpos = input.getX();
			g.drawLine(xpos,y+height-s,xpos,y+height);
			input.draw(g);
		}

	} // end of drawBody method

} // end of SplitterRenderer class
