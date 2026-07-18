package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;

/**
 * Toggle whether an element's value is watched during batch simulation
 * (issue #167). Self-inverse.
 *
 * @param id The stable id of the element to toggle.
 */
public record ToggleWatched(ElementId id) implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Element el = Ops.resolve(circuit, id);
		if (!el.canWatch()) {
			throw new OpRejected("element '" + id + "' cannot be watched");
		}
		el.setWatched(!el.isWatched());
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Element el = Ops.resolve(before, id);
		if (!el.canWatch()) {
			throw new OpRejected("element '" + id + "' cannot be watched");
		}
		return this;
	} // end of invert method

	@Override
	public void save(PrintWriter output) {

		output.println("OP ToggleWatched");
		Ops.saveString(output, "id", id.toString());
		output.println("END");
	} // end of save method

} // end of ToggleWatched record
