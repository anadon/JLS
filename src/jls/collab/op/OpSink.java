package jls.collab.op;

import java.util.List;

/**
 * The single entry point through which circuit mutations flow (issue
 * #167): the editor submits ops here instead of mutating inline, and
 * the collaboration layer (issue #163) observes the same entry point.
 * An implementation validates and applies the op, then performs the
 * change bookkeeping the surrounding application needs (undo snapshot,
 * checkpoint, changed flag - {@code SimpleEditor.markChanged()} during
 * the Stage 0b migration).
 */
public interface OpSink {

	/**
	 * Validate, apply, and record one operation.
	 *
	 * @param op The operation to perform.
	 *
	 * @throws OpRejected if the op does not validate against the current
	 *             circuit; nothing is changed or recorded.
	 */
	void submit(CircuitOp op) throws OpRejected;

	/**
	 * Validate, apply, and record a batch of operations that together
	 * express one user gesture (issue #167: a wired selection delete
	 * travels as one {@code RemoveWire} per attached net followed by a
	 * {@code RemoveElements}). This default submits each op in order,
	 * which records once per op; an implementation whose recording is
	 * per-gesture (the editor's one-undo-snapshot-per-gesture
	 * {@code markChanged()}) overrides this to apply every op first and
	 * record exactly once.
	 *
	 * @param ops The operations to perform, in order.
	 *
	 * @throws OpRejected if an op does not validate against the current
	 *             circuit; ops before it in the list have been applied
	 *             and recorded, ops after it have not been touched.
	 */
	default void submitAll(List<CircuitOp> ops) throws OpRejected {

		for (CircuitOp op : ops) {
			submit(op);
		}
	} // end of submitAll method

} // end of OpSink interface
