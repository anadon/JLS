package jls.edit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.BitSet;

import jls.JLSInfo;
import jls.elem.Element;
import jls.elem.Wire;

/**
 * GUI-side drawing for {@link Wire} (issue #77): the value-coloured wire
 * segment, its second-channel stroke (issue #76), and the probe label.
 * Moved verbatim from the former {@code Wire.draw}, with field reads
 * replaced by the element's public accessors so the model stays headless.
 */
public final class WireRenderer implements ElementRenderer {

	/**
	 * The stroke communicating a wire's value state independently of
	 * color (issue #76): a wire carrying no value (tri-state HiZ) is
	 * dashed, a wire carrying a non-zero value is thick, and a wire
	 * carrying all zeros is a plain thin line.  This is the second
	 * visual channel that keeps the states distinguishable without
	 * relying on color vision.  Moved verbatim from {@code Wire.strokeFor}.
	 *
	 * @param value The wire's value (null when off/HiZ).
	 *
	 * @return the stroke to draw the wire with.
	 */
	public static BasicStroke strokeFor(BitSet value) {

		if (value == null) {
			return new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f,
					new float[] {4.0f, 3.0f}, 0.0f);
		}
		if (!value.isEmpty()) {
			return new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND);
		}
		return new BasicStroke(1.0f);
	} // end of strokeFor method

	@Override
	public void draw(Graphics g, Element el) {

		Wire w = (Wire) el;

		BitSet value = w.hasNet() ? w.getValue() : null;
		if (w.isTouching()) {
			g.setColor(JLSInfo.touchColor);
		}
		else if (w.isHighlighted()) {
			g.setColor(JLSInfo.highlightColor);
		}
		else if (value == null) { // off
			g.setColor(JLSInfo.wireOffColor);
		}
		else if (!(value.isEmpty())) {
			g.setColor(JLSInfo.nonZeroColor);
		}
		else {
			g.setColor(JLSInfo.wireZeroColor);
		}
		int x1 = w.getEnd().getX();
		int y1 = w.getEnd().getY();
		int x2 = w.getEnd2().getX();
		int y2 = w.getEnd2().getY();
		if (g instanceof Graphics2D g2) {
			// value state is also carried by the stroke (issue #76)
			Stroke saved = g2.getStroke();
			g2.setStroke(strokeFor(value));
			g2.drawLine(x1,y1,x2,y2);
			g2.setStroke(saved);
		}
		else {
			g.drawLine(x1,y1,x2,y2);
		}

		// draw probe name if there is one
		if (!w.hasProbe())
			return;
		String probeName = w.getProbe();
		FontMetrics fm = g.getFontMetrics();
		int len = fm.stringWidth(probeName);
		g.setColor(Color.BLUE);

		// handle orientation cases
		if (y1 == y2) {

			// horizontal
			int mid = (x1+x2)/2;
			g.drawString(probeName,mid-len/2,y1-fm.getDescent());
		}
		else if (x1 == x2) {

			// vertical
			int mid = (y1+y2)/2;
			g.drawString(probeName,x1+1,mid);
		}
		else {

			// compute slope
			double slope = (double)(y2-y1)/(x2-x1);
			int midx = (x1+x2)/2;
			int midy = (y1+y2)/2;
			if (slope > 0) {

				// down to the right
				g.drawString(probeName,midx,midy-fm.getDescent());
			}
			else {

				// up to the right
				g.drawString(probeName,midx-len,midy-fm.getDescent());
			}
		}
	} // end of draw method

} // end of WireRenderer class
