package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;

/**
 * GUI-side drawing for {@link JumpEnd} (issue #77): the mutual highlight
 * with the matching jump start, the per-orientation box, the wire name,
 * and the output pin. Moved verbatim from the former {@code JumpEnd.draw},
 * with field reads replaced by the element's public accessors so the
 * model stays headless.
 */
public final class JumpEndRenderer implements ElementRenderer {

	/**
	 * Creates a {@code JumpEndRenderer}.
	 */
	public JumpEndRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		JumpEnd je = (JumpEnd) el;
		int x = je.getX();
		int y = je.getY();
		int width = je.getWidth();
		String name = je.getName();

		// draw context
		ElementRenderSupport.drawHighlight(g, je);

		// highlight if corresponding start is selected
		for (Element other : je.getCircuit().getElements()) {
			if (!(other instanceof JumpStart jstart))
				continue;
			if (name.equals(jstart.getName())) {
				if (other.isHighlighted()) {
					g.setColor(Color.orange);
					Graphics2D gg = (Graphics2D)g;
					gg.fill(AwtGeom.awt(je.getRect()));
				}
			}
		}

		// draw box
		int s = Geometry.SPACING;
		int top = y-s/2;
		int bottom = y+s/2;

		Orientation orientation = je.getOrientation();
		if(orientation == Orientation.LEFT) {
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,top,x+width-s/2,bottom);
			g.drawLine(x+s/2,top,x+width-s/2,top);
			g.drawLine(x+s/2,bottom,x+width-s/2,bottom);
			g.drawArc(x,top,s,s,-90,-180);
			g.drawLine(x+width-s/2,y,x+width,y);
			g.drawLine(x+width-s/2,y,x+width-s/4,y-s/4);
			g.drawLine(x+width-s/2,y,x+width-s/4,y+s/4);
		}
		else if(orientation == Orientation.RIGHT) {
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,top,x+s/2,bottom);
			g.drawLine(x+s/2,top,x+width-s/2,top);
			g.drawLine(x+s/2,bottom,x+width-s/2,bottom);
			g.drawArc(x+width-s,top,s,s,-90,180);
			g.drawLine(x,y,x+s/2,y);
			g.drawLine(x+s/2,y,x+s/4,y-s/4);
			g.drawLine(x+s/2,y,x+s/4,y+s/4);
		}

		// draw name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int h = fm.getDescent() + ascent;
		int w = fm.stringWidth(name);
		int tx = 0;
		if(orientation == Orientation.RIGHT)
			tx = x+s/2+(width-s-w)/2+Geometry.POINT_DIAMETER/2;
		else if(orientation == Orientation.LEFT)
			tx = x+s/2+(width-2*s-w)/2+Geometry.POINT_DIAMETER/2;
		g.drawString(name,tx,y-h/2+ascent);

		// draw output
		ElementRenderSupport.drawPut(g, je.getOutputList().get(0));

	} // end of draw method

} // end of JumpEndRenderer class
