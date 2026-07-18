package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;
import jls.elem.ElementId;
import jls.elem.Wire;

/**
 * Remove the probe from a wire (issue #167). Inverse:
 * {@link AttachProbe} carrying the removed probe's name.
 *
 * @param id The stable id of the probed wire.
 */
public record RemoveProbe(ElementId id) implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Wire wire = AttachProbe.resolveWire(circuit, id);
		if (!wire.hasProbe()) {
			throw new OpRejected("wire '" + id + "' has no probe");
		}
		wire.removeProbe();
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Wire wire = AttachProbe.resolveWire(before, id);
		if (!wire.hasProbe()) {
			throw new OpRejected("wire '" + id + "' has no probe");
		}
		return new AttachProbe(id, wire.getProbe());
	} // end of invert method

	@Override
	public void save(PrintWriter output) {

		output.println("OP RemoveProbe");
		Ops.saveString(output, "id", id.toString());
		output.println("END");
	} // end of save method

} // end of RemoveProbe record
