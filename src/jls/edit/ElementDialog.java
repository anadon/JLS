package jls.edit;

import java.awt.Graphics;

import javax.swing.JPanel;

import jls.elem.Element;

/**
 * Runs an element's creation/edit dialog, GUI-side (issue #77). As the
 * element long-tail moves off Swing, each element's {@code setup(...)}
 * dialog relocates into an {@code ElementDialog} registered for that
 * element type in {@link ElementDialogs}. Until an element is converted,
 * the registry falls back to the element's own {@code setup}, so this
 * seam is byte-identical when empty.
 */
@FunctionalInterface
public interface ElementDialog {

	/**
	 * Present the element's creation dialog and apply the result.
	 *
	 * @param el The element being created.
	 * @param g The graphics context (for sizing).
	 * @param editWindow The editor panel to parent the dialog to.
	 * @param x The drop x-coordinate.
	 * @param y The drop y-coordinate.
	 * @return true if the user cancelled, matching {@code Element.setup}.
	 */
	boolean setup(Element el, Graphics g, JPanel editWindow, int x, int y);

} // end of ElementDialog interface
