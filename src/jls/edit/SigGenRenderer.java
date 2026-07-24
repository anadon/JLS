package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.elem.Element;
import jls.elem.SigGen;

/**
 * GUI-side drawing for {@link SigGen} (issue #77): the box and centered
 * title. Moved verbatim from the former {@code SigGen.draw}.
 */
public final class SigGenRenderer implements ElementRenderer {

	/**
	 * Creates a {@code SigGenRenderer}.
	 */
	public SigGenRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		SigGen sg = (SigGen) el;
		int x = sg.getX();
		int y = sg.getY();
		int width = sg.getWidth();
		int height = sg.getHeight();

		// draw context
		ElementRenderSupport.drawHighlight(g, sg);

		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x, y, width, height);

		// draw title
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		String title = sg.getTitle();
		int w = fm.stringWidth(title);
		g.drawString(title, x + (width - w) / 2, y + (height - hi) / 2 + ascent);

	} // end of draw method

} // end of SigGenRenderer class
