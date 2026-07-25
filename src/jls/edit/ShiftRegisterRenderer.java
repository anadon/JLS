package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.ShiftRegister;

/**
 * GUI-side drawing for {@link ShiftRegister} (issue #77): the box and the
 * "in" label on the data input so it can't be confused with the (narrower)
 * amount input. Moved verbatim from the former {@code ShiftRegister.draw},
 * with field reads replaced by the element's public accessors so the model
 * stays headless - it no longer references {@code jls.edit} at all.
 */
public final class ShiftRegisterRenderer implements ElementRenderer {

	/**
	 * Creates a {@code ShiftRegisterRenderer}.
	 */
	public ShiftRegisterRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		ShiftRegister sr = (ShiftRegister) el;
		int x = sr.getX();
		int y = sr.getY();
		int width = sr.getWidth();
		int height = sr.getHeight();
		Orientation outputOrientation = sr.getOutputOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, sr);

		// draw shape
		g.setColor(Color.black);
		g.drawRect(x, y, width, height);

		// label the data input so it can't be confused with the
		// (narrower) amount input
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		int d2 = Geometry.POINT_DIAMETER / 2;
		Input data = sr.getInputList().get(1);
		g.setColor(Color.BLACK);
		if (outputOrientation == Orientation.RIGHT) {
			g.drawString("in", x + d2, data.getY() - hi / 2 + ascent);
		}
		else if (outputOrientation == Orientation.LEFT) {
			g.drawString("in", x + width - 5 * d2, data.getY() - hi / 2 + ascent);
		}
		else { // UP or DOWN
			g.drawString("in", data.getX() - 4, y + 5 * d2);
		}

		// draw inputs and output
		for (Input input : sr.getInputList()) {
			ElementRenderSupport.drawPut(g, input);
		}
		ElementRenderSupport.drawPut(g, sr.getOutputList().get(0));

	} // end of draw method

} // end of ShiftRegisterRenderer class
