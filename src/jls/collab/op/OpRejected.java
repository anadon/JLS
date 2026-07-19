package jls.collab.op;

/**
 * Thrown when a {@link CircuitOp} fails validation against the circuit
 * it is being applied to (issue #167): the addressed element does not
 * exist, lacks the capability the op needs, or the mutation would break
 * a model invariant. The circuit is unchanged when this is thrown.
 */
public class OpRejected extends Exception {

	/** Serialization version identifier. */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a rejection with a human-readable reason.
	 *
	 * @param reason Why the op does not validate.
	 */
	public OpRejected(String reason) {

		super(reason);
	} // end of constructor

} // end of OpRejected class
