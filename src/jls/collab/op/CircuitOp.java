package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;

import jls.Circuit;

/**
 * One editor mutation, reified as a validated, invertible, serializable
 * command (issue #167, collab Stage 0b). The vocabulary is deliberately
 * closed - a sealed interface over data-only records - because it is the
 * future network surface of collaborative editing (issue #163): nothing
 * in an op names a file path, a class, or code, and every kind validates
 * the circuit's invariants before mutating anything.
 *
 * Contract:
 *
 * <ul>
 * <li>{@link #apply} either performs the whole mutation or throws
 * {@link OpRejected} having changed nothing (validate first, mutate
 * after).</li>
 * <li>{@link #invert} is computed against the pre-apply circuit; applying
 * the op and then its inverse returns the circuit to its prior canonical
 * bytes (issue #166 is the oracle).</li>
 * <li>{@link #save} writes the save-format idiom (typed lines between
 * {@code OP <kind>} and {@code END}); {@link CircuitOpReader} is its
 * exact inverse.</li>
 * </ul>
 *
 * Elements are addressed by stable id (issue #165), never by object
 * reference, so an op means the same thing on a restored or replicated
 * circuit as on the one it was recorded against.
 */
public sealed interface CircuitOp
		permits ToggleWatched, AttachProbe, RemoveProbe, RotateElement,
				FlipElement, MoveElements, AddElements, RemoveElements {

	/**
	 * Validate this op against the circuit and perform it. Validation
	 * failures throw before any mutation happens.
	 *
	 * @param circuit The circuit to mutate.
	 * @param g A graphics context for geometry recomputation (rotate and
	 *            flip re-derive element size from font metrics), or null
	 *            when the op kind needs none.
	 *
	 * @throws OpRejected if the op does not validate against this
	 *             circuit; the circuit is unchanged.
	 */
	void apply(Circuit circuit, Graphics g) throws OpRejected;

	/**
	 * The op that undoes this one, computed from the circuit as it is
	 * <em>before</em> this op is applied.
	 *
	 * @param before The pre-apply circuit.
	 *
	 * @return the inverse op.
	 *
	 * @throws OpRejected if this op would not validate against the given
	 *             circuit (an op that cannot apply has no inverse).
	 */
	CircuitOp invert(Circuit before) throws OpRejected;

	/**
	 * Serialize this op in the save-format idiom. Deterministic: the
	 * same op always writes the same bytes.
	 *
	 * @param output The output writer.
	 */
	void save(PrintWriter output);

} // end of CircuitOp interface
