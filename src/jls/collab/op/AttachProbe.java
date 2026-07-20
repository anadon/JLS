package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Wire;

/**
 * Attach a named probe to a wire (issue #167). The name is resolved
 * before the op exists - prompting the user is the editor's business,
 * never the op's - so the op is pure data. Inverse: {@link RemoveProbe}.
 *
 * @param id The stable id of the wire to probe.
 * @param name The probe name; must be non-empty.
 */
public record AttachProbe(ElementId id, String name) implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Wire wire = resolveWire(circuit, id);
		if (name == null || name.isEmpty()) {
			throw new OpRejected("a probe needs a non-empty name");
		}
		if (wire.hasProbe()) {
			throw new OpRejected("wire '" + id + "' already has a probe");
		}
		wire.attachProbe(name);
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Wire wire = resolveWire(before, id);
		if (wire.hasProbe()) {
			throw new OpRejected("wire '" + id + "' already has a probe");
		}
		return new RemoveProbe(id);
	} // end of invert method

	@Override
	public void save(PrintWriter output) {

		output.println("OP AttachProbe");
		Ops.saveString(output, "id", id.toString());
		Ops.saveString(output, "name", name);
		output.println("END");
	} // end of save method

	/**
	 * Resolve a stable id that must address a wire.
	 *
	 * @param circuit The circuit to search.
	 * @param id The stable id.
	 *
	 * @return the wire.
	 *
	 * @throws OpRejected if the id is unknown or not a wire's.
	 */
	static Wire resolveWire(Circuit circuit, ElementId id)
			throws OpRejected {

		Element el = Ops.resolve(circuit, id);
		if (!(el instanceof Wire wire)) {
			throw new OpRejected("element '" + id + "' is not a wire");
		}
		return wire;
	} // end of resolveWire method

} // end of AttachProbe record
