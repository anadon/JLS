package jls.edit;

import java.awt.Graphics;

import jls.elem.Element;

/**
 * Draws one element, GUI-side (issue #77). As the element long-tail
 * moves off AWT, each element's {@code draw(Graphics)} body relocates
 * into an {@code ElementRenderer} registered for that element type in
 * {@link ElementRenderers}; the element itself becomes a headless model.
 * Until an element is converted, the registry falls back to the
 * element's own {@code draw}, so this seam is byte-identical when empty.
 */
@FunctionalInterface
public interface ElementRenderer {

	/**
	 * Draw the element.
	 *
	 * @param g The graphics to draw on.
	 * @param el The element to draw.
	 */
	void draw(Graphics g, Element el);

} // end of ElementRenderer interface
