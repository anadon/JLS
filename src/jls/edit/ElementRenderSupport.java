package jls.edit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import jls.elem.Element;

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

} // end of ElementRenderSupport class
