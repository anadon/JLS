package jls.edit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Gate;
import jls.elem.GateOutline;
import jls.elem.Output;

/**
 * GUI-side drawing for every {@link Gate} leaf (issue #77): the outline
 * symbol built from the gate's headless {@link GateOutline}, transformed
 * per orientation, plus the input/output connector lines and puts. Moved
 * verbatim from the former {@code Gate.draw}, with the private
 * {@code gatePathFrom} helper, so the gate models stay headless. The
 * {@code GeneralPath} is rebuilt from {@code gate.outline()} on every draw
 * (the model holds no AWT), replacing the former cached {@code gateShape}.
 */
public final class GateRenderer implements ElementRenderer {

	/**
	 * Creates a {@code GateRenderer}.
	 */
	public GateRenderer() {
	}

	@Override
	public void draw(Graphics g, Element el) {

		Gate gate = (Gate) el;
		int x = gate.getX();
		int y = gate.getY();
		int numInputs = gate.getNumInputs();
		Orientation orientation = gate.getOrientation();

		// draw context
		ElementRenderSupport.drawHighlight(g, gate);

		// build the outline path from the gate's headless symbol model
		// (issue #77 model/render split); the AWT GeneralPath is rebuilt
		// each draw so the gate holds no AWT
		GeneralPath gateShape = gatePathFrom(gate.outline());

		// draw the gate
		int s = Geometry.SPACING;
		int dist = (Math.max(numInputs,4)-3)/2*s;
		int ox = 0;
		int oy = 0;
		AffineTransform trans = new AffineTransform();
		if (orientation == Orientation.RIGHT) {
			trans.translate(x+s,y+dist);
			ox = -s;
		}
		else if (orientation == Orientation.LEFT) {
			trans.translate(x+s,y+dist);
			trans.rotate(Math.toRadians(180),s,s);
			ox = s;
		}
		else if (orientation == Orientation.UP) {
			trans.translate(x+dist,y+s);
			trans.rotate(Math.toRadians(-90),s,s);
			oy = s;
		}
		else if (orientation == Orientation.DOWN) {
			trans.translate(x+dist,y+s);
			trans.rotate(Math.toRadians(90),s,s);
			oy = -s;
		}
		GeneralPath temp = (GeneralPath)(gateShape.clone());
		temp.transform(trans);
		g.setColor(Color.black);
		Graphics2D gg = (Graphics2D)g;
		gg.draw(temp);

		// draw input/output points and line to them
		Output out = gate.getOutputList().get(0);
		double inc = 2.0*s/(2*numInputs);
		if (orientation == Orientation.LEFT || orientation == Orientation.RIGHT) {

			// output
			int lx = out.getX();
			g.drawLine(lx,y+dist+s,lx+ox,y+dist+s);
			ElementRenderSupport.drawPut(g, out);

			// inputs
			double ye = y + dist + inc;
			for (int p=0; p<numInputs; p+=1) {
				lx = gate.getInputList().get(p).getX();
				int ly = gate.getInputList().get(p).getY();
				g.setColor(Color.black);
				g.drawLine(lx,ly,lx-ox,(int)(ye+0.5));
				ye += inc*2;
				ElementRenderSupport.drawPut(g, gate.getInputList().get(p));
			}
		}
		else { // up or down

			// output
			int ly = out.getY();
			g.drawLine(x+dist+s,ly,x+dist+s,ly+oy);
			ElementRenderSupport.drawPut(g, out);

			// inputs
			double xe = x + dist + inc;
			for (int p=0; p<numInputs; p+=1) {;
			ly = gate.getInputList().get(p).getY();
			int lx = gate.getInputList().get(p).getX();
			g.setColor(Color.black);
			g.drawLine(lx,ly,(int)(xe+0.5),ly-oy);
			xe += inc*2;
			ElementRenderSupport.drawPut(g, gate.getInputList().get(p));
			}
		}

	} // end of draw method

	/**
	 * Translate a headless {@link GateOutline} into the exact
	 * {@link GeneralPath} the gate leaves used to build inline: each
	 * primitive is reconstructed as the same {@code java.awt.geom} shape
	 * and appended (or used to open the path) with the same connect flag,
	 * so the rendered symbol is byte-for-byte unchanged. Moved verbatim
	 * from the former {@code Gate.gatePathFrom}.
	 *
	 * @param model The outline to translate.
	 *
	 * @return the general path for the gate body.
	 */
	private static GeneralPath gatePathFrom(GateOutline model) {

		GeneralPath path = null;
		for (GateOutline.Segment seg : model.segments()) {
			double[] c = seg.coords();
			Shape shape = switch (seg.kind()) {
				case LINE ->
						new Line2D.Double(c[0], c[1], c[2], c[3]);
				case ARC ->
						new Arc2D.Double(c[0], c[1], c[2], c[3], c[4], c[5],
								Arc2D.OPEN);
				case CUBIC ->
						new CubicCurve2D.Double(c[0], c[1], c[2], c[3],
								c[4], c[5], c[6], c[7]);
				case ELLIPSE ->
						new Ellipse2D.Double(c[0], c[1], c[2], c[3]);
			};
			if (path == null) {
				path = new GeneralPath(shape);
			}
			else {
				path.append(shape, seg.connect());
			}
			if (seg.closeAfter()) {
				path.closePath();
			}
		}
		return path;
	} // end of gatePathFrom method

} // end of GateRenderer class
