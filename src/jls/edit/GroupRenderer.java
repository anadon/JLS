package jls.edit;

import java.awt.Color;
import java.awt.Graphics;

import jls.elem.Element;
import jls.elem.Group;
import jls.elem.Input;
import jls.elem.Output;

/**
 * GUI-side drawing shared by the binder/splitter renderers (issue #77):
 * the selection highlight and the gray "unattached" bounding box that the
 * former {@code Group.draw} drew, moved verbatim with field reads replaced
 * by the element's public accessors. Each concrete subclass adds its own
 * per-orientation body through {@link #drawBody}, matching the way
 * {@code Binder.draw}/{@code Splitter.draw} called {@code super.draw(g)}
 * before drawing their symbol.
 */
abstract class GroupRenderer implements ElementRenderer {

	/**
	 * Creates a {@code GroupRenderer}.
	 */
	GroupRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Group group = (Group) el;

		// draw context
		ElementRenderSupport.drawHighlight(g, group);

		// draw the box if some input or output is unattached
		boolean doit = false;
		for (Input input : group.getInputList()) {
			if (!input.isAttached())
				doit = true;
		}
		for (Output output : group.getOutputList()) {
			if (!output.isAttached()) {
				doit = true;
			}
		}
		if (doit) {
			g.setColor(Color.gray);
			g.drawRect(group.getX(), group.getY(), group.getWidth(),
					group.getHeight());
		}

		// draw the element's own symbol
		drawBody(g, group);

	} // end of draw method

	/**
	 * Draw the element-specific symbol (the split line, put labels, and
	 * connecting lines) over the shared context.
	 *
	 * @param g The graphics to draw on.
	 * @param group The group being drawn.
	 */
	abstract void drawBody(Graphics g, Group group);

} // end of GroupRenderer class
