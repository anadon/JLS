package jls.collab.op;

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

} // end of OpSink interface
