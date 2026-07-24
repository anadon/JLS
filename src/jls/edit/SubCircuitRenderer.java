package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.SubCircuit;

/**
 * GUI-side drawing for {@link SubCircuit} (issue #77): the bounding box,
 * the name-separator line, the centered subcircuit name, and the
 * per-orientation input/output pin labels. Moved verbatim from the former
 * {@code SubCircuit.draw}, with field reads replaced by the element's
 * public accessors so the model stays headless.
 */
public final class SubCircuitRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		SubCircuit sc = (SubCircuit) el;
		int x = sc.getX();
		int y = sc.getY();
		int width = sc.getWidth();
		int height = sc.getHeight();
		String name = sc.getName();
		Orientation orientation = sc.getOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, sc);

		// draw box
		int s = Geometry.SPACING;
		g.setColor(Color.BLACK);
		g.drawRect(x, y, width, height);
		int yy = y + height - 2 * s;
		g.drawLine(x, yy, x + width, yy);

		// draw subcircuit name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		Rectangle2D rect = fm.getStringBounds(name, g);
		int dx = (int) Math.round((width - rect.getWidth()) / 2);
		int dy = (int) Math.round((2 * s - rect.getHeight()) / 2);
		g.drawString(name, x + dx, yy + dy + ascent);

		// draw inputs and outputs
		for (Input input : sc.getInputList()) {
			input.draw(g);
			String inputName = input.getName();
			rect = fm.getStringBounds(inputName, g);
			g.setColor(Color.BLACK);

			if (orientation == Orientation.RIGHT) {
				dx = Geometry.POINT_DIAMETER / 2 + 1;
				dy = ascent - (int) Math.round(rect.getHeight() / 2);
			} else {
				dx = (int) (width - rect.getWidth() - (Geometry.POINT_DIAMETER / 2 + 1));
				dy = ascent - (int) Math.round(rect.getHeight() / 2);
			}
			g.drawString(inputName, x + dx, input.getY() + dy);
		}
		for (Output output : sc.getOutputList()) {
			output.draw(g);
			String outputName = output.getName();
			rect = fm.getStringBounds(outputName, g);
			g.setColor(Color.BLACK);
			if (orientation == Orientation.LEFT) {
				dx = Geometry.POINT_DIAMETER / 2 + 1;
				dy = ascent - (int) Math.round(rect.getHeight() / 2);
			} else {
				dx = (int) (width - rect.getWidth() - (Geometry.POINT_DIAMETER / 2 + 1));
				dy = ascent - (int) Math.round(rect.getHeight() / 2);
			}
			g.drawString(outputName, x + dx, output.getY() + dy);
		}

	} // end of draw method

} // end of SubCircuitRenderer class
