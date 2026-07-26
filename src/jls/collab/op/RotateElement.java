package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;
import jls.core.Orientation;
import jls.edit.SwingTextMetrics;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Rotatable;

/**
 * Rotate an element a quarter turn (issue #167). Inverse: the opposite
 * quarter turn.
 *
 * @param id The stable id of the element to rotate.
 * @param clockwise True for clockwise, false for counterclockwise.
 */
public record RotateElement(ElementId id, boolean clockwise)
		implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Element el = Ops.resolve(circuit, id);
		if (!(el instanceof Rotatable rot) || !rot.canRotate()) {
			throw new OpRejected("element '" + id + "' cannot rotate");
		}
		rot.rotate(clockwise ? Orientation.RIGHT
				: Orientation.LEFT, SwingTextMetrics.forGraphics(g));
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Element el = Ops.resolve(before, id);
		if (!(el instanceof Rotatable rot) || !rot.canRotate()) {
			throw new OpRejected("element '" + id + "' cannot rotate");
		}
		return new RotateElement(id, !clockwise);
	} // end of invert method

	@Override
	public void save(PrintWriter output) {

		output.println("OP RotateElement");
		Ops.saveString(output, "id", id.toString());
		Ops.saveInt(output, "cw", clockwise ? 1 : 0);
		output.println("END");
	} // end of save method

} // end of RotateElement record
