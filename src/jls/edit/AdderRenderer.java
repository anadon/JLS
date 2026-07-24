package jls.edit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Adder;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;

/**
 * GUI-side drawing for {@link Adder} (issue #77): the box, the plus sign,
 * and the per-orientation A/B/S/Cin/Cout labels. Moved verbatim from the
 * former {@code Adder.draw}, with field reads replaced by the element's
 * public accessors so the model stays headless.
 */
public final class AdderRenderer implements ElementRenderer {

	/**
	 * Creates an {@code AdderRenderer}.
	 */
	public AdderRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Adder a = (Adder) el;
		int x = a.getX();
		int y = a.getY();
		int width = a.getWidth();
		int height = a.getHeight();
		Orientation orientation = a.getOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, a);

		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x, y, width, height);

		// draw plus sign
		int s = Geometry.SPACING;
		if (orientation == Orientation.UP || orientation == Orientation.DOWN) {
			g.drawLine(x + 2 * s, y + s, x + 2 * s, y + 2 * s);
			g.drawLine(x + 3 * s / 2, y + 3 * s / 2, x + 5 * s / 2, y + 3 * s / 2);
		} else if (orientation == Orientation.LEFT
				|| orientation == Orientation.RIGHT) {
			g.drawLine(x + 2 * s, y + 3 * s / 2, x + 2 * s, y + 5 * s / 2);
			g.drawLine(x + 3 * s / 2, y + 2 * s, x + 5 * s / 2, y + 2 * s);
		}

		// draw input and output labels
		int d = Geometry.POINT_DIAMETER;
		FontMetrics fm = g.getFontMetrics();
		if (orientation == Orientation.RIGHT) {
			int ascent = fm.getAscent();
			Rectangle2D t = fm.getStringBounds("A", g);
			g.drawString("A", x + d / 2, (int) (y + s - t.getHeight() / 2) + ascent);
			t = fm.getStringBounds("B", g);
			g.drawString("B", x + d / 2, (int) (y + 3 * s - t.getHeight() / 2) + ascent);
			t = fm.getStringBounds("S", g);
			g.drawString("S", (int) (x + width - t.getWidth() - d / 2),
				(int) (y + 2 * s - t.getHeight() / 2) + ascent);

			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float) (fs * 0.75));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			int descent = fm.getDescent();
			t = fm.getStringBounds("Cin", g);
			g.drawString("Cin", x + (int) (width - t.getWidth()) / 2, y + ascent);
			t = fm.getStringBounds("Cout", g);
			g.drawString("Cout", x + (int) (width - t.getWidth()) / 2, y + height - descent);
			g.setFont(f);
		} else if (orientation == Orientation.LEFT) {
			int ascent = fm.getAscent();
			Rectangle2D t = fm.getStringBounds("A", g);
			g.drawString("A", (int) (x + width - t.getWidth() - d / 2), (int) (y + s - t.getHeight() / 2) + ascent);
			t = fm.getStringBounds("B", g);
			g.drawString("B", (int) (x + width - t.getWidth() - d / 2), (int) (y + 3 * s - t.getHeight() / 2) + ascent);
			t = fm.getStringBounds("S", g);
			g.drawString("S", (int) (x + d / 2),
				(int) (y + 2 * s - t.getHeight() / 2) + ascent);

			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float) (fs * 0.75));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			int descent = fm.getDescent();
			t = fm.getStringBounds("Cin", g);
			g.drawString("Cin", x + (int) (width - t.getWidth()) / 2, y + ascent);
			t = fm.getStringBounds("Cout", g);
			g.drawString("Cout", x + (int) (width - t.getWidth()) / 2, y + height - descent);
			g.setFont(f);
		} else if (orientation == Orientation.DOWN) {
			int ascent = fm.getAscent();
			int descent = fm.getDescent();
			Rectangle2D t = fm.getStringBounds("A", g);
			g.drawString("A", (int) (x + s - t.getWidth() / 2), (int) (y + t.getHeight() / 4) + ascent);
			t = fm.getStringBounds("B", g);
			g.drawString("B", (int) (x + width - t.getWidth() - d / 2), (int) (y + t.getHeight() / 4) + ascent);
			t = fm.getStringBounds("S", g);
			g.drawString("S", x + (int) (width - t.getWidth()) / 2, y + height - descent);

			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float) (fs * 0.70));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			descent = fm.getDescent();
			t = fm.getStringBounds("Cin", g);
			g.drawString("Cin", x + 5, y + height / 2 + ascent);
			t = fm.getStringBounds("Cout", g);
			g.drawString("Cout", x + (int) (width - t.getWidth() - 5), y + height / 2 + ascent);
			g.setFont(f);
		} else if (orientation == Orientation.UP) {
			int ascent = fm.getAscent();
			Rectangle2D t = fm.getStringBounds("A", g);
			g.drawString("A", (int) (x + s - t.getWidth() / 2), (int) (y + height - t.getHeight()) + ascent);
			t = fm.getStringBounds("B", g);
			g.drawString("B", (int) (x + width - t.getWidth() - d / 2), (int) (y + height - t.getHeight()) + ascent);
			t = fm.getStringBounds("S", g);
			g.drawString("S", x + (int) (width - t.getWidth()) / 2, y + (int) (t.getHeight()));

			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float) (fs * 0.70));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			t = fm.getStringBounds("Cin", g);
			g.drawString("Cin", x + 5, y + height / 2 + ascent);
			t = fm.getStringBounds("Cout", g);
			g.drawString("Cout", x + (int) (width - t.getWidth() - 5), y + height / 2 + ascent);
			g.setFont(f);
		}
		// draw inputs and outputs
		for (Input input : a.getInputList()) {
			ElementRenderSupport.drawPut(g, input);
		}
		for (Output output : a.getOutputList()) {
			ElementRenderSupport.drawPut(g, output);
		}

	} // end of draw method

} // end of AdderRenderer class
