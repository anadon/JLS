package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;

/**
 * GUI-side drawing for {@link JumpStart} (issue #77): the mutual
 * highlight with matching jump ends, the watched-value background, the
 * per-orientation box, the wire name, and the input pin. Moved verbatim
 * from the former {@code JumpStart.draw}, with field reads replaced by
 * the element's public accessors so the model stays headless.
 */
public final class JumpStartRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		JumpStart js = (JumpStart) el;
		int x = js.getX();
		int y = js.getY();
		int width = js.getWidth();
		String name = js.getName();

		// draw context
		ElementRenderSupport.drawHighlight(g, js);

		// highlight if corresponding end is selected
		for (Element other : js.getCircuit().getElements()) {
			if (!(other instanceof JumpEnd jend))
				continue;
			if (name.equals(jend.getName())) {
				if (other.isHighlighted()) {
					g.setColor(Color.orange);
					Graphics2D gg = (Graphics2D)g;
					gg.fill(js.getRect());
				}
			}
		}

		// set up corners
		int s = Geometry.SPACING;
		int top = y-s/2;
		int bottom = y+s/2;

		// draw watched background
		if (js.isWatched()) {
			g.setColor(JLSInfo.watchColor);
			g.fillRect(x, top, width-s, bottom-top);
		}

		// draw box
		Orientation orientation = js.getOrientation();
		if(orientation == Orientation.LEFT) {
			g.setColor(Color.BLACK);
			g.drawLine(x,top,x,bottom);
			g.drawLine(x,top,x+width-s,top);
			g.drawLine(x,bottom,x+width-s,bottom);
			g.drawArc(x+width-3*s/2,top,s,s,-90,180);
			g.drawLine(x+width-s/2,y,x+width,y);
			g.drawLine(x+width,y,x+width-s/4,y-s/4);
			g.drawLine(x+width,y,x+width-s/4,y+s/4);
		}
		else if(orientation == Orientation.RIGHT){
			g.setColor(Color.BLACK);
			g.drawLine(x+width,top,x+width,bottom);
			g.drawLine(x+s,top,x+width,top);
			g.drawLine(x+s,bottom,x+width,bottom);
			g.drawArc(x+s/2,top,s,s,-90,-180);
			g.drawLine(x,y,x+s/2,y);
			g.drawLine(x,y,x+s/4,y-s/4);
			g.drawLine(x,y,x+s/4,y+s/4);
		}

		// draw name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int h = fm.getDescent() + ascent;
		int w = fm.stringWidth(name);
		int tx = 0;
		if(orientation == Orientation.LEFT)
			tx = x+(width-s-w)/2+Geometry.POINT_DIAMETER/2;
		else
			tx = x+(width+0-w)/2+Geometry.POINT_DIAMETER/2;
		g.drawString(name,tx,y-h/2+ascent);

		// draw input
		js.getInputList().get(0).draw(g);

	} // end of draw method

} // end of JumpStartRenderer class
