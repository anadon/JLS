package jls.edit;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JPanel;

import jls.elem.Element;
import jls.elem.Pause;
import jls.util.Placement;

/**
 * GUI-side placement for {@link Pause} (issue #77). Moved from the former
 * {@code Pause.setup}, so the model stays headless. Pause has no creation
 * form: setup simply completes the element's sizing and drops it at the
 * mouse position.
 */
public final class PauseDialog implements ElementDialog {

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Pause pause = (Pause) el;

		// complete initialization
		pause.init(g);

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				pause.getWidth(), pause.getHeight());
		pause.setXY(p.x, p.y);
		return true;
	} // end of setup method

} // end of PauseDialog class
