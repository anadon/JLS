package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.Pin;

/**
 * GUI-side drawing for {@link Pin} and its concrete subtypes
 * {@link jls.elem.InputPin} and {@link jls.elem.OutputPin} (issue #77):
 * the orientation-pointed pentagon, watched background, centered name via
 * the live {@link FontMetrics}, and the pin's single put. Moved verbatim
 * from the former {@code Pin.draw} (both pins drew byte-identical shapes,
 * #27 S4), with field reads replaced by the element's public accessors so
 * the model stays headless. One renderer instance is registered for each
 * concrete pin class.
 */
public final class PinRenderer implements ElementRenderer {

	/**
	 * Creates a {@code PinRenderer}.
	 */
	public PinRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Pin pin = (Pin) el;
		int x = pin.getX();
		int y = pin.getY();
		int width = pin.getWidth();
		int height = pin.getHeight();
		Orientation orientation = pin.getOrientation();
		String name = pin.getName();

		// set up shape
		int s = Geometry.SPACING;
		Polygon p = new Polygon();
		if (orientation == Orientation.RIGHT) {
			p.addPoint(x, y);
			p.addPoint(x + width - s, y);
			p.addPoint(x + width, y + height / 2);
			p.addPoint(x + width - s, y + height);
			p.addPoint(x, y + height);
		} else if (orientation == Orientation.LEFT) {
			p.addPoint(x + s, y);
			p.addPoint(x, y + height / 2);
			p.addPoint(x + s, y + height);
			p.addPoint(x + width, y + height);
			p.addPoint(x + width, y);
		} else if (orientation == Orientation.UP) {
			p.addPoint(x + width / 2, y);
			p.addPoint(x + width, y + s);
			p.addPoint(x + width, y + height);
			p.addPoint(x, y + height);
			p.addPoint(x, y + s);
		} else if (orientation == Orientation.DOWN) {
			p.addPoint(x, y);
			p.addPoint(x + width, y);
			p.addPoint(x + width, y + height - s);
			p.addPoint(x + width / 2, y + height);
			p.addPoint(x, y + height - s);
		}
		// draw watched background
		if (pin.isWatched()) {
			g.setColor(JLSInfo.Palette.watchColor);
			g.fillPolygon(p);
		}

		// draw context
		ElementRenderSupport.drawHighlight(g, pin);

		// draw box
		g.setColor(Color.BLACK);
		g.drawPolygon(p);

		// draw name inside box
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D t = fm.getStringBounds(name, g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int bx = x;
		int by = y;
		int bwidth = width;
		int bheight = height;
		if (orientation == Orientation.LEFT) {
			bx = x + s;
			bwidth = width - s;
		}
		else if (orientation == Orientation.RIGHT) {
			bwidth = width - s;
		}
		else if (orientation == Orientation.UP) {
			by = y + s;
			bheight = height - s;
		}
		else { // DOWN
			bheight = height - s;
		}
		int dx = (int) ((bwidth - tw) / 2);
		int dy = (int) ((bheight - th) / 2 + fm.getAscent());

		g.drawString(name, bx + dx, by + dy);

		// draw the put
		for (Input in : pin.getInputList()) {
			ElementRenderSupport.drawPut(g, in);
		}
		for (Output out : pin.getOutputList()) {
			ElementRenderSupport.drawPut(g, out);
		}
	} // end of draw method

} // end of PinRenderer class
