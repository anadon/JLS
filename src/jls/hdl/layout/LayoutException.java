package jls.hdl.layout;

/**
 * A {@link SchematicLayouter} could not produce a contract-satisfying
 * layout (issue #62): the engine failed to run (for the out-of-process
 * ELK runner: process spawn or protocol failure), or its output broke
 * the {@link LayoutInvariants} drawing contract. Import treats this as
 * "no geometry": connectivity is still correct, the user places
 * elements by hand.
 */
public class LayoutException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Builds the exception with a message naming what failed.
	 *
	 * @param message what failed, in user-reportable terms
	 */
	public LayoutException(String message) {
		super(message);
	}

	/**
	 * Builds the exception wrapping an underlying failure.
	 *
	 * @param message what failed, in user-reportable terms
	 * @param cause the underlying failure
	 */
	public LayoutException(String message, Throwable cause) {
		super(message, cause);
	}

} // end of LayoutException class
