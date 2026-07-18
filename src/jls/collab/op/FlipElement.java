package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;

/**
 * Flip (mirror) an element (issue #167). Self-inverse.
 *
 * @param id The stable id of the element to flip.
 */
public record FlipElement(ElementId id) implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Element el = Ops.resolve(circuit, id);
		if (!el.canFlip()) {
			throw new OpRejected("element '" + id + "' cannot flip");
		}
		el.flip(g);
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Element el = Ops.resolve(before, id);
		if (!el.canFlip()) {
			throw new OpRejected("element '" + id + "' cannot flip");
		}
		return this;
	} // end of invert method

	@Override
	public void save(PrintWriter output) {

		output.println("OP FlipElement");
		Ops.saveString(output, "id", id.toString());
		output.println("END");
	} // end of save method

} // end of FlipElement record
