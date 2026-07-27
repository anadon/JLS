package jls.module;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The two-phase module runner (issue #220, grand-architecture §4.2
 * rules 6-8): {@link #boot(Collection)} resolves the manifests once
 * into one deterministic topological order via {@link ModuleResolver},
 * runs phase 1 — {@link JlsModule#register()} on <em>every</em> module,
 * in that order — and then phase 2, starting only what the declared
 * {@link Activation} triggers demand:
 *
 * <ul>
 * <li>{@link Activation.Eager} modules start at boot, in topological
 * order.</li>
 * <li>{@link Activation.OnCommand} / {@link Activation.OnEvent} /
 * {@link Activation.OnDemand} modules stay registered-but-dormant
 * until {@link #dispatchCommand(String)}, {@link #fireEvent(String)},
 * or {@link #demand(String)} first touches them (VS Code
 * {@code activationEvents} / Emacs autoloads).</li>
 * <li>Starting a module first starts, in topological order, the sole
 * provider of each of its {@code requires} tokens, transitively —
 * whatever trigger that provider declared. {@code optional},
 * {@code after}, and {@code before} never cause activation.</li>
 * </ul>
 *
 * <p>Every module runs a start-once state machine: registered, then
 * started or failed. A start that throws marks the module failed and
 * surfaces as a {@link ModuleActivationException} naming the module
 * and phase; every later touch rethrows that same exception — a failed
 * module is never silently retried. All trigger methods are idempotent
 * and report whether they started anything; unknown commands, events,
 * and tokens are graceful no-ops.</p>
 */
public final class ModuleRuntime {

	/** The start-once state machine of one registered module. */
	private enum State {
		/** Phase 1 ran; phase 2 has not. */
		REGISTERED,
		/** Phase 2 ran and returned normally. */
		STARTED,
		/** Phase 2 threw; every later touch rethrows. */
		FAILED
	} // end of State enum

	/** Modules by id, iteration in topological order. */
	private final Map<String, JlsModule> modulesById;

	/** Manifests by id, iteration in topological order. */
	private final Map<String, ModuleManifest> manifestsById;

	/** Topological position of each module id, for closure sorting. */
	private final Map<String, Integer> topoIndex;

	/**
	 * The capability index: every token maps to the id-sorted set of
	 * modules offering it — each module's concrete id plus everything
	 * in its {@code provides}. Precomputed locally; the resolver
	 * already guaranteed every {@code requires} token exactly one
	 * provider.
	 */
	private final Map<String, SortedSet<String>> providersByToken;

	/** Lifecycle state of each module id. */
	private final Map<String, State> states;

	/** The failure of each {@link State#FAILED} module, for rethrow. */
	private final Map<String, ModuleActivationException> failures;

	/** Ids of started modules, in the order they started. */
	private final List<String> startedIds;

	/**
	 * Wire the resolved structures; {@link #boot(Collection)} then
	 * drives the two phases.
	 *
	 * @param order The manifests in resolved topological order.
	 * @param byId The modules keyed by their manifest id.
	 */
	private ModuleRuntime(List<ModuleManifest> order,
			Map<String, JlsModule> byId) {

		modulesById = new LinkedHashMap<>();
		manifestsById = new LinkedHashMap<>();
		topoIndex = new HashMap<>();
		providersByToken = new HashMap<>();
		states = new HashMap<>();
		failures = new HashMap<>();
		startedIds = new ArrayList<>();
		for (ModuleManifest manifest : order) {
			String id = manifest.id();
			modulesById.put(id,
					Objects.requireNonNull(byId.get(id)));
			manifestsById.put(id, manifest);
			topoIndex.put(id, topoIndex.size());
			providersByToken
					.computeIfAbsent(id, t -> new TreeSet<>()).add(id);
			for (String token : manifest.provides()) {
				providersByToken
						.computeIfAbsent(token, t -> new TreeSet<>())
						.add(id);
			}
		}
	} // end of constructor

	/**
	 * Resolve, register, and eagerly start a module set. Each module's
	 * {@link JlsModule#manifest()} is read exactly once; the manifests
	 * are resolved into one deterministic order (duplicate ids, missing
	 * or ambiguous requirements, and cycles fail loudly there); phase 1
	 * then calls {@link JlsModule#register()} on every module in that
	 * order; phase 2 starts the {@link Activation.Eager} modules — and,
	 * transitively, their {@code requires} providers — in that order.
	 *
	 * @param modules The modules to run, in any order.
	 *
	 * @return the booted runtime, ready to dispatch triggers.
	 *
	 * @throws ModuleResolutionException if the manifests do not resolve
	 *             (propagated from {@link ModuleResolver} untouched).
	 * @throws ModuleActivationException if a module's
	 *             {@code register()} or an eager {@code start()}
	 *             throws.
	 */
	public static ModuleRuntime boot(Collection<JlsModule> modules)
			throws ModuleResolutionException,
			ModuleActivationException {

		List<JlsModule> snapshot = List.copyOf(modules);
		List<ModuleManifest> manifests = new ArrayList<>();
		for (JlsModule module : snapshot) {
			manifests.add(module.manifest());
		}
		List<ModuleManifest> order = ModuleResolver.resolve(manifests);
		// the resolver rejected duplicate ids, so this map is total
		Map<String, JlsModule> byId = new HashMap<>();
		for (int i = 0; i < snapshot.size(); i++) {
			byId.put(manifests.get(i).id(), snapshot.get(i));
		}
		ModuleRuntime runtime = new ModuleRuntime(order, byId);
		runtime.registerAll();
		runtime.startEager();
		return runtime;
	} // end of boot method

	/**
	 * First-time invocation of a named command: start every dormant
	 * module whose activation is {@link Activation.OnCommand} on this
	 * command, in topological order, each with its transitive
	 * {@code requires} providers first. Idempotent — modules already
	 * started are untouched.
	 *
	 * @param command The command identifier being invoked.
	 *
	 * @return true if any module started; false if every matching
	 *         module was already started or no module declares this
	 *         command.
	 *
	 * @throws ModuleActivationException if a start throws, or a touched
	 *             module already failed.
	 */
	public boolean dispatchCommand(String command)
			throws ModuleActivationException {

		boolean started = false;
		for (ModuleManifest manifest : manifestsById.values()) {
			if (manifest
					.activation() instanceof Activation.OnCommand trigger
					&& trigger.command().equals(command)) {
				started |= activate(manifest.id());
			}
		}
		return started;
	} // end of dispatchCommand method

	/**
	 * First firing of a named event: start every dormant module whose
	 * activation is {@link Activation.OnEvent} on this event, in
	 * topological order, each with its transitive {@code requires}
	 * providers first. Idempotent — modules already started are
	 * untouched.
	 *
	 * @param event The event identifier being fired.
	 *
	 * @return true if any module started; false if every matching
	 *         module was already started or no module declares this
	 *         event.
	 *
	 * @throws ModuleActivationException if a start throws, or a touched
	 *             module already failed.
	 */
	public boolean fireEvent(String event)
			throws ModuleActivationException {

		boolean started = false;
		for (ModuleManifest manifest : manifestsById.values()) {
			if (manifest
					.activation() instanceof Activation.OnEvent trigger
					&& trigger.event().equals(event)) {
				started |= activate(manifest.id());
			}
		}
		return started;
	} // end of fireEvent method

	/**
	 * First real use of a capability: start every dormant module whose
	 * concrete id matches the token or whose {@code provides} contains
	 * it, in topological order, each with its transitive
	 * {@code requires} providers first — whatever trigger the provider
	 * declared, because a demanded capability must exist (the literal
	 * meaning of issue #212's demand gate). Idempotent — modules
	 * already started are untouched.
	 *
	 * @param token The demanded capability token or concrete id.
	 *
	 * @return true if any module started; false if every provider was
	 *         already started or no module offers the token.
	 *
	 * @throws ModuleActivationException if a start throws, or a touched
	 *             module already failed.
	 */
	public boolean demand(String token)
			throws ModuleActivationException {

		SortedSet<String> providers = providersByToken.get(token);
		if (providers == null) {
			return false;
		}
		boolean started = false;
		for (String id : inTopoOrder(providers)) {
			started |= activate(id);
		}
		return started;
	} // end of demand method

	/**
	 * The ids of every started module, in the order they started.
	 *
	 * @return an immutable snapshot of the start log.
	 */
	public List<String> startedIds() {

		return List.copyOf(startedIds);
	} // end of startedIds method

	/**
	 * Whether a module has started.
	 *
	 * @param id The concrete module id.
	 *
	 * @return true if the module's {@code start()} ran and returned
	 *         normally; false while it is dormant, after it failed, or
	 *         when no module has the id.
	 */
	public boolean isStarted(String id) {

		return states.get(id) == State.STARTED;
	} // end of isStarted method

	/**
	 * Phase 1: {@link JlsModule#register()} on every module, in
	 * topological order, exactly once.
	 *
	 * @throws ModuleActivationException if a registration throws,
	 *             naming the module and the phase.
	 */
	private void registerAll() throws ModuleActivationException {

		for (Map.Entry<String, JlsModule> e : modulesById.entrySet()) {
			try {
				e.getValue().register();
			} catch (RuntimeException cause) {
				throw new ModuleActivationException("module '"
						+ e.getKey() + "' failed during register",
						cause);
			}
			states.put(e.getKey(), State.REGISTERED);
		}
	} // end of registerAll method

	/**
	 * Phase 2, eager pass: activate every {@link Activation.Eager}
	 * module in topological order.
	 *
	 * @throws ModuleActivationException if an eager start throws.
	 */
	private void startEager() throws ModuleActivationException {

		for (ModuleManifest manifest : manifestsById.values()) {
			if (manifest.activation() instanceof Activation.Eager) {
				activate(manifest.id());
			}
		}
	} // end of startEager method

	/**
	 * Start one module and, first, the transitive closure of its
	 * {@code requires} providers, all in topological order. Modules
	 * already started are skipped; a failed module in the closure
	 * rethrows its recorded failure. {@code optional}, {@code after},
	 * and {@code before} contribute nothing to the closure.
	 *
	 * @param rootId The id of the module being activated.
	 *
	 * @return true if this call started at least one module.
	 *
	 * @throws ModuleActivationException if a start throws, or a module
	 *             in the closure already failed.
	 */
	private boolean activate(String rootId)
			throws ModuleActivationException {

		Set<String> seen = new HashSet<>();
		Deque<String> pending = new ArrayDeque<>();
		seen.add(rootId);
		pending.add(rootId);
		while (!pending.isEmpty()) {
			String id = pending.remove();
			ModuleManifest manifest = Objects
					.requireNonNull(manifestsById.get(id));
			for (String token : manifest.requires()) {
				// sole by the resolver's zero/ambiguity checks
				String provider = Objects
						.requireNonNull(providersByToken.get(token))
						.first();
				if (seen.add(provider)) {
					pending.add(provider);
				}
			}
		}
		boolean started = false;
		for (String id : inTopoOrder(seen)) {
			started |= startOne(id);
		}
		return started;
	} // end of activate method

	/**
	 * Run one module's start-once state machine: a started module is a
	 * no-op, a failed one rethrows its recorded failure, and a
	 * registered one runs {@link JlsModule#start()} — recording either
	 * the start or, if it throws, a permanent failure naming the module
	 * and the phase.
	 *
	 * @param id The id of the module to start.
	 *
	 * @return true if the module started on this call.
	 *
	 * @throws ModuleActivationException if the start throws now, or
	 *             threw on an earlier touch.
	 */
	private boolean startOne(String id)
			throws ModuleActivationException {

		State state = states.get(id);
		if (state == State.STARTED) {
			return false;
		}
		if (state == State.FAILED) {
			throw Objects.requireNonNull(failures.get(id));
		}
		try {
			Objects.requireNonNull(modulesById.get(id)).start();
		} catch (RuntimeException cause) {
			ModuleActivationException failure =
					new ModuleActivationException("module '" + id
							+ "' failed during start", cause);
			states.put(id, State.FAILED);
			failures.put(id, failure);
			throw failure;
		}
		states.put(id, State.STARTED);
		startedIds.add(id);
		return true;
	} // end of startOne method

	/**
	 * The given module ids sorted into topological order.
	 *
	 * @param ids The ids to sort; every id must be a known module.
	 *
	 * @return the ids, smallest topological position first.
	 */
	private List<String> inTopoOrder(Collection<String> ids) {

		TreeMap<Integer, String> ordered = new TreeMap<>();
		for (String id : ids) {
			ordered.put(Objects.requireNonNull(topoIndex.get(id)), id);
		}
		return new ArrayList<>(ordered.values());
	} // end of inTopoOrder method

} // end of ModuleRuntime class
