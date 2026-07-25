package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import jls.Util;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Constant;
import jls.elem.Element;

/**
 * GUI-side drawing for {@link Constant} (issue #77): the bounding box and
 * the value string centered inside it. Moved verbatim from the former
 * {@code Constant.draw}, with field reads replaced by the element's public
 * accessors so the model stays headless. The draw-time
 * {@code getStringBounds} centering is preserved exactly (it intentionally
 * differs from the element's init-time {@code stringWidth} sizing).
 */
public final class ConstantRenderer implements ElementRenderer {

	/**
	 * Creates a {@code ConstantRenderer}.
	 */
	public ConstantRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Constant c = (Constant) el;
		int x = c.getX();
		int y = c.getY();
		Orientation orientation = c.getOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, c);

		// draw box
		Graphics2D gg = (Graphics2D) g;
		Rectangle r = AwtGeom.awt(c.getRect());
		gg.setColor(Color.BLACK);
		Rectangle drawn = new Rectangle(r.x, r.y, r.width, r.height);
		gg.draw(drawn);

		// draw value inside box
		FontMetrics fm = g.getFontMetrics();
		String str = Util.convert(c.getValue(), c.getBase(), true);
		Rectangle2D t = fm.getStringBounds(str, g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int dx = (int) ((r.width - tw) / 2);
		int dy = (int) ((r.height - th) / 2 + fm.getAscent());
		if (orientation == Orientation.LEFT || orientation == Orientation.RIGHT) {
			g.drawString(str, x + dx, y - Geometry.SPACING / 2 + dy);
		} else {
			g.drawString(str, x + dx, y + dy);
		}

		// draw output
		ElementRenderSupport.drawPut(g, c.getOutputList().get(0));

	} // end of draw method

} // end of ConstantRenderer class
