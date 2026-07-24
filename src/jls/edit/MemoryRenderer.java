package jls.edit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import jls.JLSInfo;
import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.Memory;
import jls.elem.Output;

/**
 * GUI-side drawing for {@link Memory} (issue #77): the watched background,
 * the box, the "capacity x bits" spec and name, and the per-port
 * addr/data/out/WE/OE/CS labels. Moved verbatim from the former
 * {@code Memory.draw}, with field reads replaced by the element's public
 * accessors so the model stays headless.
 */
public final class MemoryRenderer implements ElementRenderer {

	@Override
	public void draw(Graphics g, Element el) {

		Memory m = (Memory) el;
		int x = m.getX();
		int y = m.getY();
		int width = m.getWidth();
		int height = m.getHeight();

		// draw watched background
		int s = Geometry.SPACING;
		int d2 = Geometry.POINT_DIAMETER/2;
		if (m.isWatched()) {
			g.setColor(JLSInfo.watchColor);
			g.fillRect(x,y,width,height);
		}

		// draw context
		ElementRenderSupport.drawHighlight(g, m);

		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);

		// draw specs inside box
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		String specs = m.getSpecs();
		Rectangle2D t = fm.getStringBounds(specs,g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int dx = (int)((width-tw)/2);
		int dy = (int)(s-th/2+ascent);
		g.drawString(specs,x+dx,y+dy);

		// draw name inside box
		String name = m.getName();
		t = fm.getStringBounds(name,g);
		tw = t.getWidth();
		th = t.getHeight();
		dx = (int)((width-tw)/2);
		dy = (int)(3*s);
		g.drawString(name,x+dx,y+dy);

		// draw address input and label
		Input addr = m.getInputList().get(0);
		int lx = addr.getX();
		int ly = addr.getY();
		int h = fm.getAscent()+fm.getDescent();
		g.setColor(Color.black);
		g.drawString("addr",lx+d2,ly-h/2+ascent);
		ElementRenderSupport.drawPut(g, addr);

		// draw data input and label
		if (m.isRAM()) {
			Input in = m.getInputList().get(1);
			lx = in.getX();
			ly = in.getY();
			h = fm.getAscent()+fm.getDescent();
			g.setColor(Color.black);
			g.drawString("data",lx+d2,ly-h/2+ascent);
			ElementRenderSupport.drawPut(g, in);
		}

		// draw data output and label
		Output out = m.getOutputList().get(0);
		lx = out.getX();
		ly = out.getY();
		g.setColor(Color.black);
		g.drawString("out",lx-fm.stringWidth("out")-d2,ly-h/2+ascent);
		ElementRenderSupport.drawPut(g, out);

		// draw WE input and label
		int inum = 1;
		if (m.isRAM()) {
			Input we = m.getInputList().get(2);
			lx = we.getX();
			ly = we.getY();
			int w = fm.stringWidth("WE");
			g.setColor(Color.black);
			g.drawString("WE",lx-w/2,ly-d2-fm.getDescent());
			g.drawLine(lx-w/2,ly-h-1,lx+w/2,ly-h-1);
			ElementRenderSupport.drawPut(g, we);
			inum = 3;
		}

		// draw OE input and label
		Input oe = m.getInputList().get(inum);
		lx = oe.getX();
		ly = oe.getY();
		int w = fm.stringWidth("OE");
		g.setColor(Color.black);
		g.drawString("OE",lx-w/2,ly-d2-fm.getDescent());
		g.drawLine(lx-w/2,ly-h-1,lx+w/2,ly-h-1);
		ElementRenderSupport.drawPut(g, oe);

		// draw CS input and label
		Input cs = m.getInputList().get(inum+1);
		lx = cs.getX();
		ly = cs.getY();
		w = fm.stringWidth("CS");
		g.setColor(Color.black);
		g.drawString("CS",lx-w/2,ly-d2-fm.getDescent());
		g.drawLine(lx-w/2,ly-h-1,lx+w/2,ly-h-1);
		ElementRenderSupport.drawPut(g, cs);

	} // end of draw method

} // end of MemoryRenderer class
