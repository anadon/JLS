package jls.module;

/**
 * Thrown when a module's lifecycle phase fails (issue #220): its
 * {@code register()} or {@code start()} threw, or a phase was demanded
 * of a module that already failed. The message names the failing
 * module id and the phase, and the cause is the module's original
 * exception when there is one — so the failure is diagnosable from a
 * single read (§4.2 rule 5: fail loud, never paper over). A failed
 * module stays failed: the runtime rethrows this exception on every
 * later touch and never silently retries.
 */
public class ModuleActivationException extends Exception {

	/** Serialization version, required of every Exception subclass. */
	private static final long serialVersionUID = 1L;

	/**
	 * Create the exception with a message naming the failing module id
	 * and phase, wrapping the module's own exception.
	 *
	 * @param message the full description of which module failed in
	 *            which phase.
	 * @param cause the exception the module's lifecycle method threw.
	 */
	public ModuleActivationException(String message, Throwable cause) {

		super(message, cause);
	} // end of constructor

} // end of ModuleActivationException class
