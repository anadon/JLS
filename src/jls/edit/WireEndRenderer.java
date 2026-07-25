package jls.edit;

import java.awt.Color;
import java.awt.Graphics;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.WireEnd;

/**
 * GUI-side drawing for {@link WireEnd} (issue #77): the two-oval point and
 * the issue #76 touching ring. Moved verbatim from the former
 * {@code WireEnd.draw}, with field reads replaced by the element's public
 * accessors so the model stays headless.
 */
public final class WireEndRenderer implements ElementRenderer {

	/**
	 * Creates a {@code WireEndRenderer}.
	 */
	public WireEndRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		WireEnd we = (WireEnd) el;
		int x = we.getX();
		int y = we.getY();

		Color inside;
		if (we.isTouching()) {
			inside = JLSInfo.Palette.touchColor;
		}
		else if (we.isHighlighted()) {
			inside = JLSInfo.Palette.highlightColor;
		}
		else if (we.getPut() != null) {
			if (we.isAttached())
				return;
			inside = Color.black;
		}
		else if (we.getWires().size() <= 1) {
			inside = Color.WHITE;
		}
		else {
			if (we.degree() == 2)
				return;
			inside = Color.black;
		}
		g.setColor(Color.BLACK);
		int pd = Geometry.POINT_DIAMETER;
		int pr = pd/2;
		g.fillOval(x-pr,y-pr,pd,pd);
		g.setColor(inside);
		g.fillOval(x-pr+1,y-pr+1,pd-2,pd-2);
		if (we.isTouching()) {
			// second, color-independent channel (issue #76): an open
			// ring around the point marks a connection lining up even
			// when the touch color cannot be told apart
			g.setColor(JLSInfo.Palette.wireZeroColor);
			int rd = pd+4;
			g.drawOval(x-rd/2,y-rd/2,rd,rd);
		}
	} // end of draw method

} // end of WireEndRenderer class
