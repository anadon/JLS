package jls.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The typed extension-point registry (issue #223, grand-architecture
 * §4.3): the host declares its {@link ExtensionPoint}s up front,
 * modules contribute implementations to declared points, and consumers
 * read the contributions back in deterministic contribution order. The
 * host never names a module; the registry is the one place the two
 * sides meet.
 *
 * <p>Closed by construction: contributing to a point that was not
 * declared fails loudly, naming the point and the contributor — an
 * undeclared seam is a design gap to surface, never a silent no-op.
 * Type safety is enforced at contribution time via
 * {@link ExtensionPoint#contract()}, so a raw-typed contribution of
 * the wrong class fails at the boundary with a {@code
 * ClassCastException}, not later at some distant read site.</p>
 *
 * <p>Data structure only, like the rest of this package: no lifecycle,
 * no activation, no discovery. Wiring the registry into the module
 * runtime's two-phase lifecycle is a later slice of issue #220. Not
 * thread-safe; populate during startup, read afterwards.</p>
 */
public final class ExtensionRegistry {

	/** The declared points, keyed by id, in declaration order. */
	private final Map<String, ExtensionPoint<?>> pointsById;

	/** Contributions per point id, in contribution order. */
	private final Map<String, List<Object>> contributionsById;

	/**
	 * Create a registry over a fixed set of declared points.
	 *
	 * @param points The extension points this host publishes, in the
	 *            order they should be reported by {@link #points()}.
	 *
	 * @throws IllegalArgumentException if two points share an id.
	 */
	public ExtensionRegistry(Collection<ExtensionPoint<?>> points) {

		this.pointsById = new LinkedHashMap<String, ExtensionPoint<?>>();
		this.contributionsById = new LinkedHashMap<String, List<Object>>();
		for (ExtensionPoint<?> point : points) {
			ExtensionPoint<?> previous =
					pointsById.putIfAbsent(point.id(), point);
			if (previous != null) {
				throw new IllegalArgumentException(
						"duplicate extension point id '" + point.id()
								+ "': declared for contract "
								+ previous.contract().getName()
								+ " and again for contract "
								+ point.contract().getName());
			}
			contributionsById.put(point.id(),
					new ArrayList<Object>());
		}
	} // end of constructor

	/**
	 * Contribute one implementation to a declared point. Contributions
	 * are dispensed by {@link #contributions(ExtensionPoint)} in the
	 * order they arrive here.
	 *
	 * @param <T> The point's contract type.
	 * @param point The declared point being contributed to.
	 * @param moduleId The contributing module's id, for attribution in
	 *            diagnostics; must be non-blank.
	 * @param impl The implementation to contribute.
	 *
	 * @throws IllegalArgumentException if the point was not declared
	 *             to the constructor, or the module id is blank.
	 * @throws ClassCastException if a raw-typed caller passes an impl
	 *             that is not an instance of the point's contract.
	 */
	public <T> void contribute(ExtensionPoint<T> point, String moduleId,
			T impl) {

		if (moduleId.isBlank()) {
			throw new IllegalArgumentException(
					"blank module id contributing to extension point '"
							+ point.id() + "'");
		}
		requireDeclared(point, moduleId);
		contributions(point.id()).add(point.contract().cast(impl));
	} // end of contribute method

	/**
	 * The contributions made to a declared point so far, in
	 * contribution order.
	 *
	 * @param <T> The point's contract type.
	 * @param point The declared point to read.
	 *
	 * @return an immutable snapshot of the contributions, oldest
	 *         first; empty when nothing has contributed.
	 *
	 * @throws IllegalArgumentException if the point was not declared
	 *             to the constructor.
	 */
	public <T> List<T> contributions(ExtensionPoint<T> point) {

		requireDeclared(point, null);
		List<T> snapshot = new ArrayList<T>();
		for (Object impl : contributions(point.id())) {
			snapshot.add(point.contract().cast(impl));
		}
		return List.copyOf(snapshot);
	} // end of contributions method

	/**
	 * The points this host declared, in declaration order.
	 *
	 * @return an immutable set of the declared points.
	 */
	public Set<ExtensionPoint<?>> points() {

		return Collections.unmodifiableSet(
				new LinkedHashSet<ExtensionPoint<?>>(
						pointsById.values()));
	} // end of points method

	/**
	 * Reject a point this registry never declared. A point matches
	 * only if the declared point with its id is the same point (same
	 * id <em>and</em> contract): a same-id point with a different
	 * contract is an impostor, not a match.
	 *
	 * @param point The point a caller presented.
	 * @param moduleId The contributor to name in the diagnostic, or
	 *            null when the caller is a reader.
	 *
	 * @throws IllegalArgumentException if the point is not declared.
	 */
	private void requireDeclared(ExtensionPoint<?> point,
			@Nullable String moduleId) {

		if (!point.equals(pointsById.get(point.id()))) {
			throw new IllegalArgumentException(
					"extension point '" + point.id() + "' (contract "
							+ point.contract().getName()
							+ ") is not declared by this registry"
							+ (moduleId == null ? ""
									: "; rejected contribution from"
											+ " module '" + moduleId
											+ "'"));
		}
	} // end of requireDeclared method

	/**
	 * The live contribution list for a declared point id.
	 *
	 * @param id The declared point id.
	 *
	 * @return the mutable backing list.
	 */
	private List<Object> contributions(String id) {

		List<Object> list = contributionsById.get(id);
		if (list == null) {
			throw new IllegalStateException(
					"no contribution list for declared point '" + id
							+ "'");
		}
		return list;
	} // end of contributions method

} // end of ExtensionRegistry class
