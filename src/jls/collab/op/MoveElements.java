package jls.collab.op;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Wire;

/**
 * Translate a group of elements by one delta (issue #167). The group is
 * whatever the gesture committed - typically a selection including its
 * attached wire ends. Inverse: the negated delta over the same group.
 *
 * Validation is atomic: every id must resolve and no element may end up
 * with negative coordinates, checked before anything moves.
 *
 * @param ids The stable ids of the elements to move; no duplicates.
 * @param dx The x delta in pixels.
 * @param dy The y delta in pixels.
 */
public record MoveElements(List<ElementId> ids, int dx, int dy)
		implements CircuitOp {

	/**
	 * Defensive copy: the record's list is immutable whatever the caller
	 * passed (issue #94 discipline).
	 */
	public MoveElements {

		ids = List.copyOf(ids);
	} // end of compact constructor

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		if (ids.isEmpty()) {
			throw new OpRejected("a move needs at least one element");
		}
		Set<ElementId> seen = new HashSet<ElementId>();
		List<Element> targets = new ArrayList<Element>(ids.size());
		for (ElementId id : ids) {
			if (!seen.add(id)) {
				throw new OpRejected("the move lists element '" + id
						+ "' twice");
			}
			targets.add(Ops.resolve(circuit, id));
		}
		for (Element el : targets) {
			// wires follow their ends; their own rect is derived
			if (el instanceof Wire) {
				continue;
			}
			Rectangle r = el.getRect();
			if (r.x + dx < 0 || r.y + dy < 0) {
				throw new OpRejected("moving element '" + el.getStableId()
						+ "' by (" + dx + "," + dy
						+ ") would leave the canvas");
			}
		}
		for (Element el : targets) {
			el.move(dx, dy);
		}
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		for (ElementId id : ids) {
			Ops.resolve(before, id);
		}
		return new MoveElements(ids, -dx, -dy);
	} // end of invert method

	@Override
	public void save(PrintWriter output) {

		output.println("OP MoveElements");
		for (ElementId id : ids) {
			Ops.saveString(output, "id", id.toString());
		}
		Ops.saveInt(output, "dx", dx);
		Ops.saveInt(output, "dy", dy);
		output.println("END");
	} // end of save method

} // end of MoveElements record
