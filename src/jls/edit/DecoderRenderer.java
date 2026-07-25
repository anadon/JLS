package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Decoder;
import jls.elem.Element;

/**
 * GUI-side drawing for {@link Decoder} (issue #77): the box and the
 * per-orientation input/output width and "decoder" labels. Moved verbatim
 * from the former {@code Decoder.draw}, with field reads replaced by the
 * element's public accessors so the model stays headless.
 */
public final class DecoderRenderer implements ElementRenderer {

	/**
	 * Creates a {@code DecoderRenderer}.
	 */
	public DecoderRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Decoder d = (Decoder) el;
		int x = d.getX();
		int y = d.getY();
		int width = d.getWidth();
		int height = d.getHeight();
		Orientation orientation = d.getOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, d);

		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);

		// draw values inside box
		FontMetrics fm = g.getFontMetrics();
		if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT)
		{
			String inout = d.getInout();
			String dec = d.getDec();
			Rectangle2D t = fm.getStringBounds(inout,g);
			g.drawString(inout,x+(int)(width-t.getWidth())/2,
					y+(int)(height/2-t.getHeight())/2+fm.getAscent());
			t = fm.getStringBounds(dec,g);
			g.drawString(dec,x+(int)(width-t.getWidth())/2,
					y+Geometry.SPACING+(int)(height/2-t.getHeight())/2+fm.getAscent());
		}
		else if(orientation == Orientation.UP)
		{
			Rectangle2D t = fm.getStringBounds("1", g);
			g.drawString("1", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+5);
			g.drawString("|", x+(width-(int)t.getWidth())/2, y+fm.getHeight() + 20);
			g.drawString("n", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+ 35);
		}
		else if(orientation == Orientation.DOWN)
		{
			Rectangle2D t = fm.getStringBounds("1", g);
			g.drawString("n", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+5);
			g.drawString("|", x+(width-(int)t.getWidth())/2, y+fm.getHeight() + 20);
			g.drawString("1", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+ 35);
		}

		// draw input and output
		ElementRenderSupport.drawPut(g, d.getInputList().get(0));
		ElementRenderSupport.drawPut(g, d.getOutputList().get(0));

	} // end of draw method

} // end of DecoderRenderer class
