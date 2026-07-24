package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.BitSet;

import jls.BitSetUtils;
import jls.core.Geometry;
import jls.elem.Display;
import jls.elem.Element;
import jls.elem.Input;

/**
 * GUI-side drawing for {@link Display} (issue #77): the rounded box and
 * the value (or "HiZ") centered inside it, plus the attached inputs.
 * Moved verbatim from the former {@code Display.draw}, with field reads
 * replaced by the element's public accessors so the model stays headless.
 * The former draw's model mutation (pruning detached inputs) is preserved
 * by calling {@link Display#prune()} first, in the same order as before,
 * because saved-file bytes depend on the resulting input set.
 */
public final class DisplayRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Display d = (Display) el;

		// prune detached inputs first (model mutation formerly done in draw)
		d.prune();

		int x = d.getX();
		int y = d.getY();
		int width = d.getWidth();
		int height = d.getHeight();
		int base = d.getBase();
		int bits = d.getBits();
		BitSet currentValue = d.getCurrentValue();

		// draw context
		ElementRenderSupport.drawHighlight(g, d);

		// draw shape
		int s = Geometry.SPACING;
		g.setColor(Color.black);
		g.drawRect(x,y,width,height);
		g.drawRoundRect(x,y,width,height,s,s);

		// draw value inside shape
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		String value = " HiZ ";
		if (currentValue != null) {
			value = BitSetUtils.ToString(currentValue,base);
			switch (base) {
			case 2:
				while (value.length() < bits)
					value = "0" + value;
				value = value + "B";
				break;
			case 16:
				while (value.length()*4 < bits)
					value = "0" + value;
				value = "0x" + value;
			}
			value = " " + value + " ";
		}
		int sw = fm.stringWidth(value);
		g.drawString(value,x+(width-sw)/2,y+(height-hi)/2+ascent);

		// draw attached inputs
		for (Input input : d.getInputList()) {
			ElementRenderSupport.drawPut(g, input);
		}
	} // end of draw method

} // end of DisplayRenderer class
