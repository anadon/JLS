package jls.edit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;

import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Stop;

/**
 * GUI-side drawing for {@link Stop} (issue #77): the octagonal stop sign,
 * the attached inputs, and the centered "STOP" label. Moved verbatim from
 * the former {@code Stop.draw}, with field reads replaced by the element's
 * public accessors so the model stays headless. The model-mutating prune
 * of detached inputs runs first, exactly as it did in the old draw.
 */
public final class StopRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Stop stop = (Stop) el;
		int x = stop.getX();
		int y = stop.getY();
		int width = stop.getWidth();

		int s = Geometry.SPACING;

		// draw context
		ElementRenderSupport.drawHighlight(g, stop);

		// draw only the attached inputs, or all four if none attached
		stop.pruneDetachedInputs();

		// draw stop sign
		Polygon sign = new Polygon();
		int h = s/2;
		int d = s*2;
		sign.addPoint(0,h);
		sign.addPoint(h,0);
		sign.addPoint(s+h,0);
		sign.addPoint(d,h);
		sign.addPoint(d,s+h);
		sign.addPoint(s+h,d);
		sign.addPoint(h,d);
		sign.addPoint(0,s+h);
		sign.translate(x,y);
		g.setColor(Color.black);
		g.drawPolygon(sign);

		// draw inputs
		for (Input input : stop.getInputList()) {
			ElementRenderSupport.drawPut(g, input);
		}

		// draw "stop"
		Font f = g.getFont();
		Font nf = f.deriveFont((float)(f.getSize()*0.7));
		g.setFont(nf);
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth("STOP");
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int sh = ascent + descent;
		g.setColor(Color.black);
		g.drawString("STOP",x+(width-w)/2+1,y+s-sh/2+ascent);
		g.setFont(f);

	} // end of draw method

} // end of StopRenderer class
