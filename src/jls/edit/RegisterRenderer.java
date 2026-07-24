package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.Register;

/**
 * GUI-side drawing for {@link Register} (issue #77): the watched-value
 * background, the box, the register name, and the per-orientation
 * D/C/Q/notQ inputs, outputs, edge marks and Q-overbars. Moved verbatim
 * from the former {@code Register.draw}, with field reads replaced by the
 * element's public accessors so the model stays headless.
 */
public final class RegisterRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Register reg = (Register) el;
		int x = reg.getX();
		int y = reg.getY();
		int width = reg.getWidth();
		int height = reg.getHeight();
		Orientation orientation = reg.getOrientation();
		String name = reg.getName();
		String type = reg.getTypeName();

		// draw watched background
		int s = Geometry.SPACING;
		if (reg.isWatched()) {

			g.setColor(JLSInfo.watchColor);
			if(orientation == Orientation.RIGHT)
			{
				g.fillRect(x+s,y,width-s,height);
			}
			else if(orientation == Orientation.LEFT)
			{
				g.fillRect(x,y,width-s,height);
			}
			else if(orientation == Orientation.UP)
			{
				g.fillRect(x,y,width,height-s);
			}
			else if(orientation == Orientation.DOWN)
			{
				g.fillRect(x,y+s,width,height-s);
			}
		}

		// draw context
		ElementRenderSupport.drawHighlight(g, reg);

		// draw box
		g.setColor(Color.BLACK);

		if(orientation == Orientation.RIGHT)
		{

			g.drawRect(x+s,y,width-s,5*s);

			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-s-tw)/2)+s;
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+dy);

			// draw D input, line to it and label
			Input one = reg.getInputList().get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly,lx+s,ly);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx+s+1,ly-h/2+fm.getAscent());
			ElementRenderSupport.drawPut(g, one);

			// draw C input, line to it, label and type
			Input two = reg.getInputList().get(1);
			lx = two.getX();
			ly = two.getY();
			int d = Geometry.POINT_DIAMETER;
			g.setColor(Color.black);
			switch (type) {
			case "latch":
				g.drawLine(lx,ly,lx+s,ly);
				g.drawString("C",lx+s+1,ly-h/2+fm.getAscent());
				break;
			case "pff":
				g.drawLine(lx,ly,lx+s,ly);
				g.drawLine(lx+s,ly-d,lx+s+d,ly);
				g.drawLine(lx+s,ly+d,lx+s+d,ly);
				g.drawString("C",lx+s+d+1,ly-h/2+fm.getAscent());
				break;
			case "nff":
				g.drawLine(lx,ly,lx+s-d,ly);
				g.drawOval(lx+s-d,ly-d/2,d,d);
				g.drawLine(lx+s,ly-d,lx+s+d,ly);
				g.drawLine(lx+s,ly+d,lx+s+d,ly);
				g.drawString("C",lx+s+d+1,ly-h/2+fm.getAscent());
				break;
			}
			ElementRenderSupport.drawPut(g, two);

			// draw Q output and label
			Output three = reg.getOutputList().get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")-1,ly-h/2+fm.getAscent());
			ElementRenderSupport.drawPut(g, three);

			// draw notQ output and label
			Output four = reg.getOutputList().get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")-1,ly-h/2+fm.getAscent());
			g.drawLine(lx-fm.stringWidth("Q")-1,ly-h/2,lx-2,ly-h/2);
			ElementRenderSupport.drawPut(g, four);
		}
		else if(orientation == Orientation.LEFT)
		{
			g.drawRect(x,y,width-s,5*s);

			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-s-tw)/2);
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+dy);

			// draw D input, line to it and label
			Input one = reg.getInputList().get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly,lx-s,ly);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx-s-10,ly-h/2+fm.getAscent());
			ElementRenderSupport.drawPut(g, one);

			// draw C input, line to it, label and type
			Input two = reg.getInputList().get(1);
			lx = two.getX();
			ly = two.getY();
			int d = Geometry.POINT_DIAMETER;
			g.setColor(Color.black);
			switch (type) {
			case "latch":
				g.drawLine(lx,ly,lx-s,ly);
				g.drawString("C",lx-s-10,ly-h/2+fm.getAscent());
				break;
			case "pff":
				g.drawLine(lx,ly,lx-s,ly);
				g.drawLine(lx-s,ly-d,lx-s-d,ly);
				g.drawLine(lx-s,ly+d,lx-s-d,ly);
				g.drawString("C",lx-s-d-9,ly-h/2+fm.getAscent());
				break;
			case "nff":
				g.drawLine(lx,ly,lx-s+d,ly);
				g.drawOval(lx-s,ly-d/2,d,d);
				g.drawLine(lx-s,ly-d,lx-s-d,ly);
				g.drawLine(lx-s,ly+d,lx-s-d,ly);
				g.drawString("C",lx-s-d-9,ly-h/2+fm.getAscent());
				break;
			}
			ElementRenderSupport.drawPut(g, two);

			// draw Q output and label
			Output three = reg.getOutputList().get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx+3,ly-h/2+fm.getAscent());
			ElementRenderSupport.drawPut(g, three);

			// draw notQ output and label
			Output four = reg.getOutputList().get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx+3,ly-h/2+fm.getAscent());
			g.drawLine(lx+fm.stringWidth("Q")+2,ly-h/2,lx+2,ly-h/2);
			ElementRenderSupport.drawPut(g, four);
		}
		else if(orientation == Orientation.UP)
		{
			g.drawRect(x,y,width,height-s);

			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-tw)/2);
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+dy);

			// draw D input, line to it and label
			Input one = reg.getInputList().get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly,lx,ly-s);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx-fm.stringWidth("D")/2,ly-s-h+fm.getAscent());
			ElementRenderSupport.drawPut(g, one);

			// draw C input, line to it, label and type
			Input two = reg.getInputList().get(1);
			lx = two.getX();
			ly = two.getY();
			int d = Geometry.POINT_DIAMETER;
			g.setColor(Color.black);
			switch (type) {
			case "latch":
				g.drawLine(lx,ly-s,lx,ly);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly-s-h+fm.getAscent());
				break;
			case "pff":
				g.drawLine(lx,ly-s,lx,ly);
				g.drawLine(lx-d,ly-s,lx,ly-s-d);
				g.drawLine(lx+d,ly-s,lx,ly-s-d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly-s-d-h+fm.getAscent());
				break;
			case "nff":
				g.drawLine(lx,ly-s+d,lx,ly);
				g.drawOval(lx-d/2, ly-s, d, d);
				g.drawLine(lx-d,ly-s,lx,ly-s-d);
				g.drawLine(lx+d,ly-s,lx,ly-s-d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly-s-d-h+fm.getAscent());
				break;
			}
			ElementRenderSupport.drawPut(g, two);

			// draw Q output and label
			Output three = reg.getOutputList().get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly+d+fm.getAscent());
			ElementRenderSupport.drawPut(g, three);

			// draw notQ output and label
			Output four = reg.getOutputList().get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly+d+fm.getAscent());
			g.drawLine(lx-fm.stringWidth("Q")/2,ly+d-1,lx+fm.stringWidth("Q")/2,ly+d-1);
			ElementRenderSupport.drawPut(g, four);

		}
		else if(orientation == Orientation.DOWN)
		{
			g.drawRect(x,y+s,width,height-s);

			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-tw)/2);
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+s+dy);

			// draw D input, line to it and label
			Input one = reg.getInputList().get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly+s,lx,ly);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx-fm.stringWidth("D")/2,ly+s+1+fm.getAscent());
			ElementRenderSupport.drawPut(g, one);

			// draw C input, line to it, label and type
			Input two = reg.getInputList().get(1);
			lx = two.getX();
			ly = two.getY();
			int d = Geometry.POINT_DIAMETER;
			g.setColor(Color.black);
			switch (type) {
			case "latch":
				g.drawLine(lx,ly+s,lx,ly);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly+s+1+fm.getAscent());
				break;
			case "pff":
				g.drawLine(lx,ly+s,lx,ly);
				g.drawLine(lx-d,ly+s,lx,ly+s+d);
				g.drawLine(lx+d,ly+s,lx,ly+s+d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly+s+d+fm.getAscent());
				break;
			case "nff":
				g.drawLine(lx,ly+s-d,lx,ly);
				g.drawOval(lx-d/2,ly+s-d,d,d);
				g.drawLine(lx-d,ly+s,lx,ly+s+d);
				g.drawLine(lx+d,ly+s,lx,ly+s+d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly+s+d+fm.getAscent());
				break;
			}
			ElementRenderSupport.drawPut(g, two);

			// draw Q output and label
			Output three = reg.getOutputList().get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly-h+fm.getAscent());
			ElementRenderSupport.drawPut(g, three);

			// draw notQ output and label
			Output four = reg.getOutputList().get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly-h+fm.getAscent());
			g.drawLine(lx-fm.stringWidth("Q")/2,ly-h,lx+fm.stringWidth("Q")/2,ly-h);
			ElementRenderSupport.drawPut(g, four);

		}

	} // end of draw method

} // end of RegisterRenderer class
