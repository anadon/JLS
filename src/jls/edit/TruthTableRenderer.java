package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.TruthTable;

/**
 * GUI-side drawing for {@link TruthTable} (issue #77): the circuit-canvas
 * box with the name band and the per-signal input/output labels. Moved
 * verbatim from the former {@code TruthTable.draw}, with field reads
 * replaced by the element's public accessors so the model stays headless.
 */
public final class TruthTableRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		TruthTable tt = (TruthTable) el;
		int x = tt.getX();
		int y = tt.getY();
		int width = tt.getWidth();
		int height = tt.getHeight();

		// set up
		int s = Geometry.SPACING;
		int d2 = Geometry.POINT_DIAMETER/2;
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;

		// draw context
		ElementRenderSupport.drawHighlight(g, tt);

		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);
		g.drawLine(x,y+height-2*s,x+width,y+height-2*s);

		// draw name
		String dname = tt.getName();
		if (dname.isEmpty()) {
			dname = "Logic";
		}
		int w = fm.stringWidth(dname);
		g.drawString(dname,x+(width-w)/2,y+height-s-fontHeight/2+ascent);

		// draw inputs and outputs and their names
		for (Input input : tt.getInputList()) {
			int dy = input.getY();
			g.setColor(Color.black);
			g.drawString(input.getName(),x+d2,dy-fontHeight/2+ascent);
			input.draw(g);
		}
		for (Output output : tt.getOutputList()) {
			int dy = output.getY();
			int ow = fm.stringWidth(output.getName());
			g.setColor(Color.black);
			g.drawString(output.getName(),x+width-ow-d2,dy-fontHeight/2+ascent);
			output.draw(g);
		}

	} // end of draw method

} // end of TruthTableRenderer class
