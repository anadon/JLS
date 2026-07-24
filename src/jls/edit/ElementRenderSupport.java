package jls.edit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.Put;

/**
 * Shared GUI-side drawing helpers for the element renderers extracted by
 * the #77 element wave. Reproduces the common bits that used to live on
 * the model base class {@code Element.draw} - notably the selection
 * highlight - so a renderer can call it in place of {@code super.draw(g)}
 * without the element importing AWT.
 */
public final class ElementRenderSupport {

	private ElementRenderSupport() {} // no instances

	/**
	 * Draw the selection highlight for an element, exactly as the former
	 * {@code Element.draw} did: a pink fill of the element's bounding
	 * rectangle when it is highlighted.
	 *
	 * @param g The graphics to draw on.
	 * @param el The element being drawn.
	 */
	public static void drawHighlight(Graphics g, Element el) {
		if (el.isHighlighted()) {
			g.setColor(Color.pink);
			Graphics2D gg = (Graphics2D) g;
			gg.fill(el.getRect());
		}
	} // end of drawHighlight method

	/**
	 * Draw a connection point (put), exactly as the former
	 * {@code Put.draw} did (issue #77): a black-ringed dot, filled by its
	 * touch/attached/unattached color, plus the issue #76 color-independent
	 * open ring when it is touching a wire end. An attached put draws
	 * nothing (its wire covers it).
	 *
	 * @param g The graphics to draw on.
	 * @param p The put to draw.
	 */
	public static void drawPut(Graphics g, Put p) {

		Color inside;
		if (p.isTouching()) {
			inside = JLSInfo.touchColor;
		} else if (p.isAttached()) {
			return;
		} else {
			inside = Color.WHITE;
		}
		int d = Geometry.POINT_DIAMETER;
		int r = d / 2;
		int x = p.getElement().getX() + p.getXr();
		int y = p.getElement().getY() + p.getYr();
		g.setColor(Color.BLACK);
		g.fillOval(x - r, y - r, d, d);
		g.setColor(inside);
		g.fillOval(x - r + 1, y - r + 1, d - 2, d - 2);
		if (p.isTouching()) {
			// second, color-independent channel (issue #76): an open ring
			// around the point marks a connection lining up even when the
			// touch color cannot be told apart
			g.setColor(JLSInfo.wireZeroColor);
			int rd = d + 4;
			g.drawOval(x - rd / 2, y - rd / 2, rd, rd);
		}
	} // end of drawPut method

} // end of ElementRenderSupport class
