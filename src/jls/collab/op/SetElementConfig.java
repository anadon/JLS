package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.SubCircuit;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * Reconfigure one element in place from its serialized block (issue
 * #167): the commit-time op behind every attribute dialog and every
 * ordered-row editor (state machine, truth table, signal generator).
 * The editor's dialogs edit an element's state locally; when the user
 * commits, the reconfigured element is serialized ({@link
 * ElementBlocks}) and submitted as one op carrying that exact byte
 * form. This is the H1 refinement issue #167 section 9 sanctions -
 * "set-attribute" and "edit-ordered-rows" are one commit-time op kind,
 * not several, because JLS elements have no uniform typed-attribute
 * API: an element's whole reconfigured save block <em>is</em> the
 * change unit.
 *
 * The block must describe the same element - same stable id (issue
 * #165) and same type - as the one addressed: a reconfigure changes an
 * element's attributes, never its identity or kind (that is a remove
 * plus an add). Inverse: the same op carrying the element's
 * pre-change block, a byte-exact restore rather than a snapshot
 * fallback.
 *
 * Like {@link RemoveElements}, this op does not touch wiring: an
 * element with a wire attached to any of its puts is rejected, because
 * replacing the element mints fresh puts its inverse could not
 * reattach. Reconfiguring a wired element keeps its inline path (under
 * the snapshot-undo safety net) until the wiring vocabulary lands.
 * Jump starts and ends - whose names link across elements - are
 * likewise out of scope here.
 *
 * @param id The stable id of the element to reconfigure.
 * @param block The reconfigured element's serialized block, declaring
 *            the same stable id and type as the addressed element.
 */
public record SetElementConfig(ElementId id, String block)
		implements CircuitOp {

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Element old = validate(circuit, g);

		// old is gone first so its registered name (if any) is freed
		// before the reconfigured element - which may keep that name -
		// re-registers it; the scratch dry run in validate proved the
		// re-load and init below cannot fail
		old.remove(circuit);
		Element fresh = ElementBlocks.load(circuit, block);
		try {
			fresh.init(g);
		} catch (Exception ex) {
			throw new OpRejected("the reconfigured element could not "
					+ "initialize"
					+ (ex.getMessage() == null ? ""
							: ": " + ex.getMessage()));
		}
		circuit.addElement(fresh);
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Element old = validate(before, null);
		return new SetElementConfig(id, ElementBlocks.saveBlock(old));
	} // end of invert method

	/**
	 * Vet the reconfigure against the circuit without touching it: the
	 * addressed element must resolve to an editable, unwired, plain
	 * element, and the block must load into a scratch circuit as the
	 * same-typed, same-id element and initialize cleanly.
	 *
	 * @param circuit The circuit the op would mutate.
	 * @param g A graphics context for the init dry run, or null when the
	 *            caller only needs the resolved element (invert).
	 *
	 * @return the element the op addresses, still in the circuit.
	 *
	 * @throws OpRejected if any check fails; the circuit is untouched
	 *             either way.
	 */
	private Element validate(Circuit circuit, Graphics g)
			throws OpRejected {

		Element old = Ops.resolve(circuit, id);
		rejectUnsupported(old, "the addressed element");
		requireUnwired(circuit, old);

		Element fresh = ElementBlocks.load(new Circuit(""), block);
		if (!fresh.getStableId().equals(id)) {
			throw new OpRejected("the reconfigured block declares stable "
					+ "id '" + fresh.getStableId() + "', not the '" + id
					+ "' this op addresses");
		}
		if (!fresh.getClass().equals(old.getClass())) {
			throw new OpRejected("a reconfigure may not change element "
					+ "'" + id + "' from a "
					+ old.getClass().getSimpleName() + " to a "
					+ fresh.getClass().getSimpleName());
		}
		requireNameFree(circuit, old, fresh);
		if (g != null) {
			try {
				fresh.init(g);
			} catch (Exception ex) {
				throw new OpRejected("the reconfigured element could not "
						+ "initialize"
						+ (ex.getMessage() == null ? ""
								: ": " + ex.getMessage()));
			}
		}
		return old;
	} // end of validate method

	/**
	 * Reject an element kind this op does not reconfigure: wires and
	 * wire ends, subcircuits, jump starts and ends, and uneditable
	 * elements.
	 *
	 * @param el The element to check.
	 * @param role Which side of the op it is, for the error message.
	 *
	 * @throws OpRejected if the element is of an unsupported kind.
	 */
	private static void rejectUnsupported(Element el, String role)
			throws OpRejected {

		if (el instanceof Wire || el instanceof WireEnd) {
			throw new OpRejected(role + " is a wire; wires are "
					+ "reconfigured through the wiring op kinds");
		}
		if (el instanceof SubCircuit) {
			throw new OpRejected(role + " is a subcircuit; subcircuits "
					+ "are reconfigured through the subcircuit op kind");
		}
		if (el instanceof JumpStart || el instanceof JumpEnd) {
			throw new OpRejected(role + " is a jump, whose name links "
					+ "across elements; jumps are out of scope for "
					+ "reconfigure");
		}
		if (el.isUneditable()) {
			throw new OpRejected(role + " is uneditable and cannot be "
					+ "reconfigured");
		}
	} // end of rejectUnsupported method

	/**
	 * Reject reconfiguring an element that has a wire attached to any of
	 * its puts: the replacement would mint fresh puts, orphaning the
	 * wire in a way the inverse could not restore (the {@link
	 * RemoveElements} invariant).
	 *
	 * @param circuit The circuit to scan.
	 * @param target The element being reconfigured.
	 *
	 * @throws OpRejected if any wire end attaches to the target.
	 */
	private static void requireUnwired(Circuit circuit, Element target)
			throws OpRejected {

		for (Element el : circuit.getElements()) {
			if (!(el instanceof WireEnd)) {
				continue;
			}
			WireEnd end = (WireEnd) el;
			if (end.isAttached()
					&& end.getPut().getElement() == target) {
				throw new OpRejected("element '" + target.getStableId()
						+ "' has a wire attached and cannot be "
						+ "reconfigured by this op");
			}
		}
	} // end of requireUnwired method

	/**
	 * Reject a reconfigure whose new name is already taken by a
	 * different element. The element's own current name is never a
	 * collision - reconfiguring an element without renaming it, or
	 * renaming it and back, must pass.
	 *
	 * @param circuit The circuit to check against.
	 * @param old The element as it is now.
	 * @param fresh The reconfigured element.
	 *
	 * @throws OpRejected if the new name belongs to another element.
	 */
	private static void requireNameFree(Circuit circuit, Element old,
			Element fresh) throws OpRejected {

		String newName = fresh.getName();
		if (newName == null || newName.isEmpty()) {
			return;
		}
		if (newName.equals(old.getName())) {
			return;
		}
		if (circuit.hasName(newName)) {
			throw new OpRejected("the reconfigured name '" + newName
					+ "' is already used by another element");
		}
	} // end of requireNameFree method

	@Override
	public void save(PrintWriter output) {

		output.println("OP SetElementConfig");
		Ops.saveString(output, "id", id.toString());
		Ops.saveString(output, "block", block);
		output.println("END");
	} // end of save method

} // end of SetElementConfig record
