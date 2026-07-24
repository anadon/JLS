package jls.edit;

import java.awt.Color;
import java.awt.Graphics;

import jls.core.Geometry;
import jls.elem.Clock;
import jls.elem.Element;

/**
 * GUI-side drawing for {@link Clock} (issue #77): the rounded box and the
 * clock-waveform glyph, then the output pin. Moved verbatim from the
 * former {@code Clock.draw}, with field reads replaced by the element's
 * public accessors so the model stays headless.
 */
public final class ClockRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Clock c = (Clock) el;
		int x = c.getX();
		int y = c.getY();

		// draw context
		ElementRenderSupport.drawHighlight(g, c);

		// draw rounded rectangle
		int s = Geometry.SPACING;
		int s2 = s/2;
		int s4 = s/4;
		int d = s*2;
		g.setColor(Color.BLACK);
		g.drawRoundRect(x,y,d,d,s2,s2);

		// draw waveform

		int bottom = y+s+s2;
		int top = y+s2;
		int left = x+s4;
		g.drawLine(left,bottom,left+s4,bottom);
		left += s4;
		g.drawLine(left,bottom,left,top);
		g.drawLine(left,top,left+s2,top);
		left += s2;
		g.drawLine(left,top,left,bottom);
		g.drawLine(left,bottom,left+s2,bottom);
		left += s2;
		g.drawLine(left,bottom,left,top);
		g.drawLine(left,top,left+s4,top);

		// draw output
		c.getOutputList().get(0).draw(g);
	} // end of draw method

} // end of ClockRenderer class
