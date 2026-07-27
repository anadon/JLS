package jls.module;

import java.util.Set;

/**
 * The static descriptor of one module (issue #220, grand-architecture
 * §4.1): everything the resolver needs to decide existence, ambiguity,
 * and order, readable <em>without</em> running any module code — the
 * universal precondition for topological ordering and lazy activation.
 *
 * <p>Two separate axes, deliberately (§4.2 rule 1): {@code requires}
 * and {@code optional} answer <em>does it exist / is it a gate</em>;
 * {@code after} and {@code before} answer <em>when do I run</em>. A
 * {@code requires} edge implies an {@code after} edge, but an
 * {@code after} edge alone pulls nothing in — systemd's
 * {@code Requires=} vs {@code After=}.</p>
 *
 * <p>Capability tokens (§4.2 rule 3): a {@code requires} entry is
 * satisfied by any module whose {@code provides} contains that token,
 * or by a module whose concrete {@code id} matches it (Debian
 * {@code Provides} / virtual packages). No version solver (§4.2 rule
 * 4): one jar means one version of everything; {@code apiVersion} is a
 * single integer checked only for external modules, where major means
 * break.</p>
 *
 * @param id The stable unique module name; must be non-blank.
 * @param apiVersion The single-integer SPI version this module was
 *            built against (major = break; no ranges, no solver).
 * @param provides Capability tokens this module publishes, in addition
 *            to its concrete id; each must be non-blank.
 * @param requires Hard dependencies (capability token or concrete id):
 *            missing means the resolve fails loudly; each must be
 *            non-blank and never the module's own id.
 * @param optional Soft dependencies: use-if-present, never a gate, and
 *            never a reason to load the target; each must be non-blank
 *            and never the module's own id.
 * @param after Ordering only — run after these, if present; pulls
 *            nothing in; each must be non-blank and never the module's
 *            own id.
 * @param before Ordering only — run before these, if present; pulls
 *            nothing in; each must be non-blank and never the module's
 *            own id.
 * @param activation The trigger on which this module's logic comes
 *            alive (§4.2 rule 7).
 */
public record ModuleManifest(
		String id,
		int apiVersion,
		Set<String> provides,
		Set<String> requires,
		Set<String> optional,
		Set<String> after,
		Set<String> before,
		Activation activation) {

	/**
	 * Validate and defensively copy (issue #94 discipline): the record
	 * holds immutable sets whatever the caller passed, every token is
	 * non-blank, and no dependency or ordering axis names the module
	 * itself.
	 *
	 * @throws IllegalArgumentException if the id or any token is
	 *             blank, or if requires, optional, after, or before
	 *             contains the module's own id.
	 */
	public ModuleManifest {

		if (id.isBlank()) {
			throw new IllegalArgumentException("blank module id");
		}
		provides = copyOf(provides, "provides", id);
		requires = copyOf(requires, "requires", id);
		optional = copyOf(optional, "optional", id);
		after = copyOf(after, "after", id);
		before = copyOf(before, "before", id);
		noSelfEdge(requires, "requires", id);
		noSelfEdge(optional, "optional", id);
		noSelfEdge(after, "after", id);
		noSelfEdge(before, "before", id);
	} // end of compact constructor

	/**
	 * An immutable copy of one token set with every token checked
	 * non-blank.
	 *
	 * @param tokens The caller's token set.
	 * @param axis The manifest field being validated, for the message.
	 * @param id The module id, for the message.
	 *
	 * @return an immutable copy of the tokens.
	 *
	 * @throws IllegalArgumentException if any token is blank.
	 */
	private static Set<String> copyOf(Set<String> tokens, String axis,
			String id) {

		Set<String> copy = Set.copyOf(tokens);
		for (String token : copy) {
			if (token.isBlank()) {
				throw new IllegalArgumentException("blank " + axis
						+ " token in module '" + id + "'");
			}
		}
		return copy;
	} // end of copyOf method

	/**
	 * Reject an axis that names the module itself.
	 *
	 * @param tokens The already-copied token set.
	 * @param axis The manifest field being validated, for the message.
	 * @param id The module id.
	 *
	 * @throws IllegalArgumentException if the set contains the id.
	 */
	private static void noSelfEdge(Set<String> tokens, String axis,
			String id) {

		if (tokens.contains(id)) {
			throw new IllegalArgumentException("module '" + id
					+ "' names itself in " + axis);
		}
	} // end of noSelfEdge method

} // end of ModuleManifest record
