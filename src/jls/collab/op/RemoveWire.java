package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Output;
import jls.elem.Put;
import jls.elem.TriProp;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * Remove one whole wire net from a circuit (issue #167): every wire
 * end, every segment, every probe, detaching every put the net is
 * attached to - the same end state the editor's inline wire deletion
 * reaches. The net is addressed by the stable id (issue #165) of any
 * one of its wire ends. Inverse: an {@link AddWire} carrying the net's
 * serialized wire-end blocks ({@link NetBlocks}) - a true, byte-exact
 * restore, not a snapshot fallback, mirroring how
 * {@link RemoveElements} inverts through {@link ElementBlocks}.
 *
 * Net granularity is deliberate: segments and ends are not
 * independently addressable mutations, because removing a segment
 * re-partitions and re-derives the surrounding net in ways only whole
 * nets serialize exactly. A gesture that deletes one segment of a
 * larger net will travel as this op followed by {@link AddWire} ops for
 * the surviving pieces.
 *
 * Detaching a tri-state net from a tri-state-propagating element clears
 * that element's tri-state property, exactly as the editor's inline
 * detach does; the {@link AddWire} inverse re-arms it.
 *
 * @param id The stable id of any wire end of the net to remove.
 */
public record RemoveWire(ElementId id) implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		List<WireEnd> ends = validate(circuit);

		// every segment, from every end (each wire appears twice)
		Set<Wire> wires = new LinkedHashSet<Wire>();
		for (WireEnd end : ends) {
			wires.addAll(end.getWires());
		}

		// detach puts first, mirroring WireEnd.remove(Wire, Circuit):
		// an output just detaches, and a tri-state net clears the
		// tri-state property of the elements it drove
		for (WireEnd end : ends) {
			Put p = end.getPut();
			if (p == null) {
				continue;
			}
			p.setAttached(null);
			end.setPut(null);
			if (!(p instanceof Output) && end.getNet().isTriState()
					&& p.getElement() instanceof TriProp pin) {
				pin.setTriState(false);
			}
		}

		for (Wire wire : wires) {
			circuit.remove(wire);
		}
		for (WireEnd end : ends) {
			circuit.remove(end);
		}
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		return NetBlocks.toAddWire(validate(before));
	} // end of invert method

	/**
	 * Resolve the addressed end and vet its whole net before anything
	 * is removed.
	 *
	 * @param circuit The circuit the op would mutate.
	 *
	 * @return the net's wire ends, in stable-id order.
	 *
	 * @throws OpRejected if the id does not address a removable wire
	 *             net; the circuit is untouched either way.
	 */
	private List<WireEnd> validate(Circuit circuit) throws OpRejected {

		Element el = Ops.resolve(circuit, id);
		if (!(el instanceof WireEnd end)) {
			throw new OpRejected("element '" + id
					+ "' is not a wire end");
		}
		List<WireEnd> ends;
		try {
			ends = new ArrayList<WireEnd>(end.getNet().getAllEnds());
		} catch (IllegalStateException ex) {
			throw new OpRejected("wire end '" + id
					+ "' is not part of a completed wire net");
		}
		for (WireEnd e : ends) {
			if (e.isUneditable()) {
				throw new OpRejected("the net contains an uneditable "
						+ "wire end and cannot be removed");
			}
			Put p = e.getPut();
			if (p != null && p.getElement() == null) {
				throw new OpRejected("the net is attached to an "
						+ "in-progress connection and cannot be "
						+ "removed");
			}
			for (Wire w : e.getWires()) {
				if (w.isUneditable()) {
					throw new OpRejected("the net contains an "
							+ "uneditable wire and cannot be removed");
				}
			}
		}
		ends.sort(Comparator.comparing(Element::getStableId));
		return ends;
	} // end of validate method

	@Override
	public void save(PrintWriter output) {

		output.println("OP RemoveWire");
		Ops.saveString(output, "id", id.toString());
		output.println("END");
	} // end of save method

} // end of RemoveWire record
