package jls.module;

/**
 * The typed identity of one seam modules contribute to (issue #223,
 * grand-architecture §4.3): a stable string id plus the contract type
 * every contribution must implement. The host publishes points and
 * never names a module — the inversion-of-control seam — so the
 * concrete API surface of the module program is the set of these
 * values, catalogued in {@code docs/extension-points.md} and pinned by
 * {@code ExtensionPointCatalogTest}.
 *
 * <p>Data-only, like {@link ModuleManifest}: a point carries identity
 * and contract, nothing else. Contributions are held and dispensed by
 * {@link ExtensionRegistry}, which uses {@link #contract()} as the
 * runtime witness that every contribution really is a {@code T} — the
 * classic type-safe heterogeneous container key.</p>
 *
 * @param <T> The contract type contributions to this point implement.
 *
 * @param id The stable unique point name (kebab-case, dot-prefixed by
 *            home area, e.g. {@code "hdl.exporter"}); must be
 *            non-blank.
 * @param contract The class token of the contract every contribution
 *            must be assignable to; must be non-null.
 */
public record ExtensionPoint<T>(String id, Class<T> contract) {

	/**
	 * Validate the point's identity.
	 *
	 * @throws IllegalArgumentException if the id is blank or the
	 *             contract token is null.
	 */
	public ExtensionPoint {

		if (id.isBlank()) {
			throw new IllegalArgumentException("blank extension point id");
		}
		if (contract == null) {
			throw new IllegalArgumentException(
					"no contract type for extension point '" + id + "'");
		}
	} // end of compact constructor

} // end of ExtensionPoint record
