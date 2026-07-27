package jls.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Resolves a set of {@link ModuleManifest}s into one deterministic
 * startup order (issue #220, grand-architecture §4.2 rules 1-6).
 * Resolution happens once, at startup, and fails loudly:
 *
 * <ul>
 * <li>Duplicate module ids are an error.</li>
 * <li>Every {@code requires} token must have exactly one provider — a
 * module whose {@code provides} contains the token or whose concrete
 * id equals it (Debian {@code Provides} style). Zero providers is a
 * loud error naming the token and its requirer; more than one is an
 * ambiguity error naming all of them (a declared-alternative syntax is
 * deliberately absent this slice).</li>
 * <li>{@code optional} is never a gate and never an edge: an absent
 * target is a graceful no-op, and a present one is not pulled in or
 * ordered against unless an {@code after}/{@code before} edge says
 * so.</li>
 * <li>{@code requires} implies {@code after}; {@code after} and
 * {@code before} alone order against every present provider of their
 * token and pull nothing in.</li>
 * <li>The order is Kahn's topological sort with an id-sorted
 * tie-break, so it is a pure function of the manifest set —
 * independent of input, discovery, or iteration order.</li>
 * <li>A cycle is a {@link ModuleResolutionException} reporting the
 * full cycle path (Jenkins/IntelliJ style), never papered over.</li>
 * </ul>
 */
public final class ModuleResolver {

	/** Pure algorithm; never instantiated. */
	private ModuleResolver() {
	} // end of constructor

	/**
	 * Resolve manifests into the deterministic startup order.
	 *
	 * @param manifests The manifests to resolve, in any order.
	 *
	 * @return the manifests in startup order: every module after the
	 *         single provider of each of its {@code requires} tokens
	 *         and after/before every present provider its ordering
	 *         axes name, ties broken by id.
	 *
	 * @throws ModuleResolutionException if two manifests share an id,
	 *             a {@code requires} token has zero or several
	 *             providers, or the dependency/ordering edges form a
	 *             cycle.
	 */
	public static List<ModuleManifest> resolve(
			Collection<ModuleManifest> manifests)
			throws ModuleResolutionException {

		Map<String, ModuleManifest> byId = indexById(manifests);
		Map<String, SortedSet<String>> providers = indexProviders(
				byId.values());

		// successors[u] = modules that must start after u; an edge
		// u -> v also bumps v's indegree for Kahn's algorithm
		Map<String, SortedSet<String>> successors = new TreeMap<>();
		Map<String, Integer> indegree = new TreeMap<>();
		for (String id : byId.keySet()) {
			successors.put(id, new TreeSet<>());
			indegree.put(id, 0);
		}
		for (ModuleManifest m : byId.values()) {
			for (String token : m.requires()) {
				String provider = soleProvider(token, m.id(), providers);
				if (!provider.equals(m.id())) {
					addEdge(provider, m.id(), successors, indegree);
				}
			}
			// optional: no edges ever - not a gate, not an ordering
			for (String token : m.after()) {
				for (String provider : presentProviders(token,
						providers)) {
					if (!provider.equals(m.id())) {
						addEdge(provider, m.id(), successors, indegree);
					}
				}
			}
			for (String token : m.before()) {
				for (String provider : presentProviders(token,
						providers)) {
					if (!provider.equals(m.id())) {
						addEdge(m.id(), provider, successors, indegree);
					}
				}
			}
		}

		// Kahn's topological sort; the ready set is id-sorted so ties
		// resolve identically for every input permutation
		TreeSet<String> ready = new TreeSet<>();
		for (Map.Entry<String, Integer> e : indegree.entrySet()) {
			if (e.getValue() == 0) {
				ready.add(e.getKey());
			}
		}
		List<ModuleManifest> order = new ArrayList<>();
		Set<String> emitted = new HashSet<>();
		while (!ready.isEmpty()) {
			String id = ready.pollFirst();
			order.add(byId.get(id));
			emitted.add(id);
			for (String next : Objects
					.requireNonNull(successors.get(id))) {
				int remaining = Objects
						.requireNonNull(indegree.get(next)) - 1;
				indegree.put(next, remaining);
				if (remaining == 0) {
					ready.add(next);
				}
			}
		}
		if (order.size() < byId.size()) {
			SortedSet<String> stuck = new TreeSet<>(byId.keySet());
			stuck.removeAll(emitted);
			throw new ModuleResolutionException(
					"module dependency cycle: "
							+ cyclePath(successors, stuck));
		}
		return order;
	} // end of resolve method

	/**
	 * Index manifests by id, rejecting duplicates.
	 *
	 * @param manifests The manifests to index.
	 *
	 * @return an id-sorted map of the manifests.
	 *
	 * @throws ModuleResolutionException if two manifests share an id.
	 */
	private static Map<String, ModuleManifest> indexById(
			Collection<ModuleManifest> manifests)
			throws ModuleResolutionException {

		Map<String, ModuleManifest> byId = new TreeMap<>();
		for (ModuleManifest m : manifests) {
			if (byId.put(m.id(), m) != null) {
				throw new ModuleResolutionException(
						"duplicate module id '" + m.id() + "'");
			}
		}
		return byId;
	} // end of indexById method

	/**
	 * The capability index: every token maps to the id-sorted set of
	 * modules offering it — each module's concrete id plus everything
	 * in its {@code provides}.
	 *
	 * @param manifests The deduplicated manifests.
	 *
	 * @return the token-to-provider-ids index.
	 */
	private static Map<String, SortedSet<String>> indexProviders(
			Collection<ModuleManifest> manifests) {

		Map<String, SortedSet<String>> providers = new HashMap<>();
		for (ModuleManifest m : manifests) {
			providers.computeIfAbsent(m.id(), t -> new TreeSet<>())
					.add(m.id());
			for (String token : m.provides()) {
				providers.computeIfAbsent(token, t -> new TreeSet<>())
						.add(m.id());
			}
		}
		return providers;
	} // end of indexProviders method

	/**
	 * The single provider a {@code requires} token resolves to.
	 *
	 * @param token The required capability token or concrete id.
	 * @param requirer The id of the requiring module, for messages.
	 * @param providers The capability index.
	 *
	 * @return the sole provider's id (possibly the requirer itself,
	 *         when a module provides its own requirement).
	 *
	 * @throws ModuleResolutionException if zero or several modules
	 *             provide the token.
	 */
	private static String soleProvider(String token, String requirer,
			Map<String, SortedSet<String>> providers)
			throws ModuleResolutionException {

		SortedSet<String> offering = providers.get(token);
		if (offering == null || offering.isEmpty()) {
			throw new ModuleResolutionException("module '" + requirer
					+ "' requires '" + token
					+ "', but no module provides it");
		}
		if (offering.size() > 1) {
			throw new ModuleResolutionException("module '" + requirer
					+ "' requires '" + token + "', but "
					+ offering.size() + " modules provide it: "
					+ offering);
		}
		return offering.first();
	} // end of soleProvider method

	/**
	 * Every present provider of an ordering token — possibly none,
	 * because {@code after}/{@code before} pull nothing in.
	 *
	 * @param token The ordering token or concrete id.
	 * @param providers The capability index.
	 *
	 * @return the id-sorted providers, empty when the token is absent.
	 */
	private static SortedSet<String> presentProviders(String token,
			Map<String, SortedSet<String>> providers) {

		SortedSet<String> offering = providers.get(token);
		return offering == null ? new TreeSet<>() : offering;
	} // end of presentProviders method

	/**
	 * Record one edge: {@code from} must start before {@code to}.
	 *
	 * @param from The module that starts first.
	 * @param to The module that starts after it.
	 * @param successors The successor sets, updated in place.
	 * @param indegree The indegree counts, updated in place; a
	 *            duplicate edge bumps nothing.
	 */
	private static void addEdge(String from, String to,
			Map<String, SortedSet<String>> successors,
			Map<String, Integer> indegree) {

		if (Objects.requireNonNull(successors.get(from)).add(to)) {
			indegree.merge(to, 1, Integer::sum);
		}
	} // end of addEdge method

	/**
	 * The cycle path among the modules Kahn's algorithm could not
	 * emit, rendered in dependency direction ({@code a -> b} reads "a
	 * depends on / is ordered after b"). Deterministic: the walk
	 * starts at the smallest stuck id and always follows the smallest
	 * stuck predecessor.
	 *
	 * @param successors The full successor sets.
	 * @param stuck The ids left with unresolved predecessors.
	 *
	 * @return the path, e.g. {@code "a -> b -> c -> a"}.
	 */
	private static String cyclePath(
			Map<String, SortedSet<String>> successors,
			SortedSet<String> stuck) {

		// invert the edges among the stuck modules: every stuck module
		// keeps at least one stuck predecessor, so the walk below must
		// revisit a module, and where it does is a cycle
		Map<String, SortedSet<String>> preds = new TreeMap<>();
		for (String id : stuck) {
			preds.put(id, new TreeSet<>());
		}
		for (String from : stuck) {
			for (String to : Objects
					.requireNonNull(successors.get(from))) {
				if (stuck.contains(to)) {
					Objects.requireNonNull(preds.get(to)).add(from);
				}
			}
		}
		List<String> walk = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		String node = stuck.first();
		while (seen.add(node)) {
			walk.add(node);
			node = Objects.requireNonNull(preds.get(node)).first();
		}
		List<String> cycle = walk.subList(walk.indexOf(node),
				walk.size());
		return String.join(" -> ", cycle) + " -> " + node;
	} // end of cyclePath method

} // end of ModuleResolver class
