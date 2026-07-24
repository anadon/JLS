package jls.edit;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JPanel;

import jls.elem.Element;
import jls.elem.Stop;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link Stop} (issue #77). Moved from the
 * former {@code Stop.setup}, so the model stays headless. Stop has no
 * options to gather, so setup simply completes the element's sizing and
 * positions it at the drop point.
 */
public final class StopDialog implements ElementDialog {

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Stop stop = (Stop) el;

		// complete initialization
		stop.init(SwingTextMetrics.forGraphics(g));

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				stop.getWidth(), stop.getHeight());
		stop.setXY(p.x, p.y);

		return true;
	} // end of setup method

} // end of StopDialog class
