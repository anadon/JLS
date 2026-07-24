package jls.edit;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import jls.elem.Element;
import jls.elem.Text;

/**
 * GUI-side drawing for {@link Text} (issue #77): builds the element's own
 * font from its name/style/size and draws each line, in the text's chosen
 * color. Moved verbatim from the former {@code Text.draw}, with field
 * reads replaced by the element's public accessors and the size
 * recomputation applied back through the element's setters (the original
 * draw mutated {@code width}/{@code height} before the highlight, so this
 * renderer does the same to stay byte-identical).
 */
public final class TextRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Text t = (Text) el;

		// save current graphics object
		Graphics myg = g.create();

		// draw the text
		int bi = 0;
		if (t.isBold()) bi |= Font.BOLD;
		if (t.isItalic()) bi |= Font.ITALIC;
		myg.setFont(new Font(t.getFontName(), bi, t.getFontSize()));
		FontMetrics fm = myg.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = ascent + descent;
		int th = 0;
		int tw = 0;
		for (String str : t.getLines()) {
			th += height;
			tw = Math.max(tw, fm.stringWidth(str));
		}
		t.setHeight(th);
		t.setWidth(tw);

		ElementRenderSupport.drawHighlight(g, t);
		int y = t.getY() + ascent;
		myg.setColor(t.getColor());
		for (String str : t.getLines()) {
			myg.drawString(str, t.getX(), y);
			y += height;
		}

	} // end of draw method

} // end of TextRenderer class
