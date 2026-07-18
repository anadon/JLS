package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.SubCircuit;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * Remove a group of elements from a circuit (issue #167). Inverse: an
 * {@link AddElements} carrying each removed element's serialized block
 * ({@link ElementBlocks}) - a true, byte-exact restore, not a snapshot
 * fallback.
 *
 * The group must be removable without touching wiring: wires and wire
 * ends are not removable here (they get their own op kinds), and an
 * element with a wire attached to any of its puts is rejected, because
 * detaching would mutate wire state this op's inverse could not
 * restore. The delete gesture's wired selections keep their inline path
 * (under the snapshot-undo safety net) until the wiring vocabulary
 * lands.
 *
 * Removing a jump start cascades to its same-name jump ends in the
 * editor, so validation requires the group to be closed under that
 * cascade: a jump start may only be removed together with every one of
 * its jump ends (a jump end alone is fine).
 *
 * @param ids The stable ids of the elements to remove; no duplicates.
 */
public record RemoveElements(List<ElementId> ids) implements CircuitOp {

	/**
	 * Defensive copy: the record's list is immutable whatever the caller
	 * passed (issue #94 discipline).
	 */
	public RemoveElements {

		ids = List.copyOf(ids);
	} // end of compact constructor

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		List<Element> targets = validate(circuit);

		// jump ends first: removing a jump start cascades to same-name
		// ends, and validation guarantees they are all in the group, so
		// this order makes each removal a plain single removal
		for (Element el : targets) {
			if (el instanceof JumpEnd) {
				el.remove(circuit);
			}
		}
		for (Element el : targets) {
			if (!(el instanceof JumpEnd)) {
				el.remove(circuit);
			}
		}
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		List<Element> targets = validate(before);
		targets.sort(Comparator.comparing(Element::getStableId));
		List<String> blocks = new ArrayList<String>(targets.size());
		for (Element el : targets) {
			blocks.add(ElementBlocks.saveBlock(el));
		}
		return new AddElements(blocks);
	} // end of invert method

	/**
	 * Resolve and vet the whole group before anything is removed: every
	 * id must resolve to a removable, editable, unwired element, and
	 * jump starts must bring every one of their jump ends along.
	 *
	 * @param circuit The circuit the op would mutate.
	 *
	 * @return the resolved elements, in id order.
	 *
	 * @throws OpRejected if any check fails; the circuit is untouched
	 *             either way.
	 */
	private List<Element> validate(Circuit circuit) throws OpRejected {

		if (ids.isEmpty()) {
			throw new OpRejected("a removal needs at least one element");
		}
		Set<ElementId> seen = new HashSet<ElementId>();
		List<Element> targets = new ArrayList<Element>(ids.size());
		for (ElementId id : ids) {
			if (!seen.add(id)) {
				throw new OpRejected("the removal lists element '" + id
						+ "' twice");
			}
			Element el = Ops.resolve(circuit, id);
			if (el instanceof Wire || el instanceof WireEnd) {
				throw new OpRejected("wires are removed through the "
						+ "wiring op kinds, not through element removal");
			}
			if (el instanceof SubCircuit) {
				throw new OpRejected("subcircuits are removed through "
						+ "the subcircuit op kinds, not through element "
						+ "removal");
			}
			if (el.isUneditable()) {
				throw new OpRejected("element '" + id
						+ "' is uneditable and cannot be removed");
			}
			targets.add(el);
		}
		Set<Element> group = new HashSet<Element>(targets);
		for (Element el : circuit.getElements()) {
			if (!(el instanceof WireEnd)) {
				continue;
			}
			WireEnd end = (WireEnd) el;
			if (end.isAttached()
					&& group.contains(end.getPut().getElement())) {
				throw new OpRejected("element '"
						+ end.getPut().getElement().getStableId()
						+ "' has a wire attached and cannot be removed "
						+ "by this op");
			}
		}
		for (Element el : targets) {
			if (!(el instanceof JumpStart)) {
				continue;
			}
			for (Element other : circuit.getElements()) {
				if (other instanceof JumpEnd
						&& el.getName().equals(other.getName())
						&& !group.contains(other)) {
					throw new OpRejected("jump start '" + el.getName()
							+ "' still has a jump end not included in "
							+ "the removal");
				}
			}
		}
		return targets;
	} // end of validate method

	@Override
	public void save(PrintWriter output) {

		output.println("OP RemoveElements");
		for (ElementId id : ids) {
			Ops.saveString(output, "id", id.toString());
		}
		output.println("END");
	} // end of save method

} // end of RemoveElements record
