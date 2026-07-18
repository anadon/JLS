package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;

/**
 * Add one or more elements to a circuit from their serialized blocks
 * (issue #167). Each block is the exact byte form the element's own save
 * method writes ({@link ElementBlocks}), so an added element is
 * indistinguishable from a loaded one; each block must declare the
 * stable id (issue #165) the element arrives with. Inverse:
 * {@link RemoveElements} over those ids.
 *
 * Wiring never travels here: blocks describing wires or wire ends are
 * rejected (they get their own op kinds), so an added element always
 * arrives unwired.
 *
 * Validation is atomic and mirrors the editor's paste rules: every
 * block must load, every stable id must be new, element names must not
 * collide (within the op or with the circuit), a jump start's name must
 * not duplicate an existing one, and a jump end must have a source -
 * a matching jump start already in the circuit or arriving in this same
 * op. Nothing is added unless every block passes.
 *
 * @param blocks The serialized element blocks, in the order they are to
 *            be added.
 */
public record AddElements(List<String> blocks) implements CircuitOp {

	/**
	 * Defensive copy: the record's list is immutable whatever the caller
	 * passed (issue #94 discipline).
	 */
	public AddElements {

		blocks = List.copyOf(blocks);
	} // end of compact constructor

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		validate(circuit);

		// every block validated against a scratch circuit above, so the
		// re-load against the real target cannot fail; init before
		// addElement, the same order Circuit.finishLoad uses
		for (String block : blocks) {
			Element el = ElementBlocks.load(circuit, block);
			try {
				el.init(g);
			} catch (Exception ex) {
				throw new OpRejected("element could not initialize"
						+ (ex.getMessage() == null ? ""
								: ": " + ex.getMessage()));
			}
			circuit.addElement(el);
		}
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		List<Element> loaded = validate(before);
		List<ElementId> ids = new ArrayList<ElementId>(loaded.size());
		for (Element el : loaded) {
			ids.add(el.getStableId());
		}
		return new RemoveElements(ids);
	} // end of invert method

	/**
	 * Validate every block against the target circuit without touching
	 * it: blocks are loaded into a scratch circuit, then checked as a
	 * group for stable-id and name collisions and for jump-end sources.
	 *
	 * @param target The circuit the op would mutate.
	 *
	 * @return the throwaway elements the blocks describe, in block
	 *         order (owned by the scratch circuit, never by the target).
	 *
	 * @throws OpRejected if any block fails any check; the target is
	 *             untouched either way.
	 */
	private List<Element> validate(Circuit target) throws OpRejected {

		if (blocks.isEmpty()) {
			throw new OpRejected("an add needs at least one element "
					+ "block");
		}
		Circuit scratch = new Circuit("");
		List<Element> loaded = new ArrayList<Element>(blocks.size());
		Set<ElementId> sids = new HashSet<ElementId>();
		Set<String> jumpStartsArriving = new HashSet<String>();
		for (String block : blocks) {
			Element el = ElementBlocks.load(scratch, block);
			if (!sids.add(el.getStableId())) {
				throw new OpRejected("two blocks declare the same stable "
						+ "id '" + el.getStableId() + "'");
			}
			for (Element existing : target.getElements()) {
				if (existing.getStableId().equals(el.getStableId())) {
					throw new OpRejected("the circuit already has an "
							+ "element with stable id '"
							+ el.getStableId() + "'");
				}
			}
			if (el.getRect().x < 0 || el.getRect().y < 0) {
				throw new OpRejected("element '" + el.getStableId()
						+ "' would arrive off the canvas");
			}
			if (el instanceof JumpStart) {
				jumpStartsArriving.add(el.getName());
			}
			loaded.add(el);
		}
		Set<String> names = new HashSet<String>();
		for (Element el : loaded) {
			String name = el.getName();
			if (el instanceof JumpEnd) {
				// a jump end needs a source; its name legitimately
				// duplicates its jump start's, so it skips name checks
				if (target.getJumpStart(name) == null
						&& !jumpStartsArriving.contains(name)) {
					throw new OpRejected("jump end '" + name
							+ "' would arrive with no matching jump "
							+ "start");
				}
				continue;
			}
			if (el instanceof JumpStart
					&& target.getJumpStart(name) != null) {
				throw new OpRejected("the circuit already has a jump "
						+ "start named '" + name + "'");
			}
			if (name != null && !name.isEmpty()) {
				if (target.hasName(name)) {
					throw new OpRejected("the circuit already has an "
							+ "element named '" + name + "'");
				}
				if (!names.add(name)) {
					throw new OpRejected("two blocks declare the same "
							+ "element name '" + name + "'");
				}
			}
		}
		return loaded;
	} // end of validate method

	@Override
	public void save(PrintWriter output) {

		output.println("OP AddElements");
		for (String block : blocks) {
			Ops.saveString(output, "block", block);
		}
		output.println("END");
	} // end of save method

} // end of AddElements record
