package jls.module;

/**
 * Thrown when a set of module manifests cannot be resolved into one
 * startup order (issue #220): a duplicate module id, a hard
 * requirement with zero providers, a requirement with more than one
 * provider and no declared alternative, or a dependency/ordering
 * cycle. The message names the offenders — the token and its requirer,
 * the competing providers, or the full cycle path — so the failure is
 * diagnosable from a single read (§4.2 rule 5: fail loud, never paper
 * over).
 */
public class ModuleResolutionException extends Exception {

	/** Serialization version, required of every Exception subclass. */
	private static final long serialVersionUID = 1L;

	/**
	 * Create the exception with a message naming the offenders.
	 *
	 * @param message the full description of why resolution failed.
	 */
	public ModuleResolutionException(String message) {

		super(message);
	} // end of constructor

} // end of ModuleResolutionException class
