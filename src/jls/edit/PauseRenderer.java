package jls.edit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Pause;

/**
 * GUI-side drawing for {@link Pause} (issue #77): the circle, the four
 * (or attached-only) input wires, and the centered "PAUSE" label. Moved
 * verbatim from the former {@code Pause.draw}, with field reads replaced
 * by the element's public accessors so the model stays headless. The
 * model-mutating detached-input pruning that used to run inside
 * {@code draw} now lives in {@link Pause#pruneDetachedInputs()} and is
 * run first here, preserving the original ordering.
 */
public final class PauseRenderer implements ElementRenderer {

	/**
	 * Creates a {@code PauseRenderer}.
	 */
	public PauseRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Pause p = (Pause) el;

		int s = Geometry.SPACING;

		// draw only the attached inputs, or all four if none attached
		// (model mutation preserved, run first)
		p.pruneDetachedInputs();

		// draw context
		ElementRenderSupport.drawHighlight(g, p);

		int x = p.getX();
		int y = p.getY();
		int width = p.getWidth();
		int height = p.getHeight();

		// draw circle
		g.setColor(Color.black);
		g.drawOval(x, y, width, height);

		// draw inputs
		for (Input input : p.getInputList()) {
			ElementRenderSupport.drawPut(g, input);
		}

		// draw "pause"
		Font f = g.getFont();
		Font nf = f.deriveFont((float) (f.getSize() * 0.55));
		g.setFont(nf);
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth("PAUSE");
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int sh = ascent + descent;
		g.setColor(Color.black);
		g.drawString("PAUSE", x + (width - w) / 2, y + s - sh / 2 + ascent);
		g.setFont(f);

	} // end of draw method

} // end of PauseRenderer class
