package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.module.Activation;
import jls.module.ExtensionPoint;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleActivationException;
import jls.module.ModuleManifest;
import jls.module.ModuleResolutionException;
import jls.module.ModuleRuntime;

/**
 * Pins the two-phase lifecycle contract of the module runtime from
 * issue #220 — every {@code register()} precedes every {@code start()},
 * each exactly once, both in topological order; eager modules start at
 * boot and lazy ones do not; commands, events, and demand start their
 * modules first-time-only; activation transitively starts the sole
 * provider of each {@code requires} token while {@code optional} /
 * {@code after} / {@code before} never trigger anything; a throwing
 * {@code start()} becomes a permanent {@code ModuleActivationException}
 * naming the module, rethrown on every later touch and never retried;
 * the whole thing is deterministic across shuffled input permutations;
 * and resolution failures propagate from {@code boot()} untouched.
 */
class ModuleRuntimeTest {

	/**
	 * A recording module: appends {@code "register:<id>"} and
	 * {@code "start:<id>"} to a shared log, counts calls, and
	 * optionally throws from {@code start()}.
	 */
	private static final class FakeModule implements JlsModule {

		/** The static descriptor this fake reports. */
		private final ModuleManifest manifest;

		/** The log shared by every module in the scenario. */
		private final List<String> log;

		/** Whether start() throws instead of succeeding. */
		private final boolean failOnStart;

		/** How many times register() ran. */
		private int registerCalls;

		/** How many times start() ran (including a throwing run). */
		private int startCalls;

		FakeModule(ModuleManifest manifest, List<String> log,
				boolean failOnStart) {
			this.manifest = manifest;
			this.log = log;
			this.failOnStart = failOnStart;
		}

		@Override
		public ModuleManifest manifest() {
			return manifest;
		}

		@Override
		public void register(ExtensionRegistry registrar) {
			registerCalls++;
			log.add("register:" + manifest.id());
		}

		@Override
		public void start() {
			startCalls++;
			log.add("start:" + manifest.id());
			if (failOnStart) {
				throw new IllegalStateException(
						"boom in " + manifest.id());
			}
		}

	} // end of FakeModule class

	/** Shorthand manifest with every axis explicit. */
	private static ModuleManifest manifest(String id,
			Set<String> provides, Set<String> requires,
			Set<String> optional, Set<String> after, Set<String> before,
			Activation activation) {
		return new ModuleManifest(id, 1, provides, requires, optional,
				after, before, activation);
	}

	/** A module with an id, an activation, and nothing else. */
	private static FakeModule bare(String id, Activation activation,
			List<String> log) {
		return new FakeModule(manifest(id, Set.of(), Set.of(), Set.of(),
				Set.of(), Set.of(), activation), log, false);
	}

	/** A module requiring the given tokens. */
	private static FakeModule requiring(String id, Set<String> requires,
			Activation activation, List<String> log) {
		return new FakeModule(manifest(id, Set.of(), requires, Set.of(),
				Set.of(), Set.of(), activation), log, false);
	}

	@Test
	void allRegistersPrecedeAllStartsExactlyOnceInTopoOrder() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule core = bare("core", new Activation.Eager(), log);
		FakeModule gui = requiring("gui", Set.of("core"),
				new Activation.Eager(), log);
		FakeModule batch = requiring("batch", Set.of("core"),
				new Activation.Eager(), log);
		ModuleRuntime runtime = ModuleRuntime
				.boot(List.of(gui, batch, core));
		assertEquals(List.of("register:core", "register:batch",
				"register:gui", "start:core", "start:batch",
				"start:gui"), log,
				"phase 1 must fully precede phase 2, both in topo"
						+ " order with the id-sorted tie-break");
		assertEquals(1, core.registerCalls);
		assertEquals(1, gui.registerCalls);
		assertEquals(1, batch.registerCalls);
		assertEquals(1, core.startCalls);
		assertEquals(List.of("core", "batch", "gui"),
				runtime.startedIds());
	}

	@Test
	void eagerStartsAtBootAndLazyDoesNot() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule kernel = bare("kernel", new Activation.Eager(), log);
		FakeModule lazyCommand = bare("cmd.mod",
				new Activation.OnCommand("export"), log);
		FakeModule lazyEvent = bare("evt.mod",
				new Activation.OnEvent("opened"), log);
		FakeModule lazyDemand = bare("dem.mod",
				new Activation.OnDemand(), log);
		ModuleRuntime runtime = ModuleRuntime.boot(
				List.of(lazyCommand, lazyEvent, lazyDemand, kernel));
		assertTrue(runtime.isStarted("kernel"));
		assertFalse(runtime.isStarted("cmd.mod"),
				"an OnCommand module must stay dormant at boot");
		assertFalse(runtime.isStarted("evt.mod"),
				"an OnEvent module must stay dormant at boot");
		assertFalse(runtime.isStarted("dem.mod"),
				"an OnDemand module must stay dormant at boot");
		assertEquals(List.of("kernel"), runtime.startedIds());
		assertEquals(1, lazyCommand.registerCalls,
				"dormant modules still register in phase 1");
	}

	@Test
	void dispatchCommandStartsFirstTimeOnly() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule exporter = bare("hdl.export",
				new Activation.OnCommand("export"), log);
		ModuleRuntime runtime = ModuleRuntime.boot(List.of(exporter));
		assertTrue(runtime.dispatchCommand("export"),
				"the first dispatch must start the module");
		assertTrue(runtime.isStarted("hdl.export"));
		assertFalse(runtime.dispatchCommand("export"),
				"a repeat dispatch must start nothing");
		assertEquals(1, exporter.startCalls,
				"start() must run exactly once");
	}

	@Test
	void fireEventStartsFirstTimeOnly() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule collab = bare("collab",
				new Activation.OnEvent("session-opened"), log);
		ModuleRuntime runtime = ModuleRuntime.boot(List.of(collab));
		assertTrue(runtime.fireEvent("session-opened"));
		assertFalse(runtime.fireEvent("session-opened"),
				"a repeat event must start nothing");
		assertEquals(1, collab.startCalls);
	}

	@Test
	void demandMatchesConcreteIdAndProvidesToken() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule byId = bare("board.db", new Activation.OnDemand(),
				log);
		FakeModule byToken = new FakeModule(
				manifest("elem.registry", Set.of("element-registry"),
						Set.of(), Set.of(), Set.of(), Set.of(),
						new Activation.OnDemand()),
				log, false);
		ModuleRuntime runtime = ModuleRuntime
				.boot(List.of(byId, byToken));
		assertTrue(runtime.demand("board.db"),
				"demand by concrete id must start the module");
		assertTrue(runtime.demand("element-registry"),
				"demand by provides token must start the provider");
		assertTrue(runtime.isStarted("elem.registry"));
		assertFalse(runtime.demand("element-registry"),
				"a repeat demand must start nothing");
		assertEquals(1, byToken.startCalls);
	}

	@Test
	void unknownCommandEventAndTokenAreGracefulNoOps() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule module = bare("core",
				new Activation.OnCommand("real"), log);
		ModuleRuntime runtime = ModuleRuntime.boot(List.of(module));
		assertFalse(runtime.dispatchCommand("ghost"));
		assertFalse(runtime.fireEvent("ghost"));
		assertFalse(runtime.demand("ghost"));
		assertFalse(runtime.isStarted("ghost"),
				"an unknown id is simply not started");
		assertEquals(0, module.startCalls,
				"unknown triggers must start nothing");
		assertEquals(List.of(), runtime.startedIds());
	}

	@Test
	void activationTransitivelyStartsRequiresChainInTopoOrder() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule a = bare("a", new Activation.OnDemand(), log);
		FakeModule b = requiring("b", Set.of("a"),
				new Activation.OnDemand(), log);
		FakeModule c = requiring("c", Set.of("b"),
				new Activation.OnDemand(), log);
		ModuleRuntime runtime = ModuleRuntime.boot(List.of(c, a, b));
		assertTrue(runtime.demand("c"));
		assertEquals(List.of("a", "b", "c"), runtime.startedIds(),
				"activating c must first start its transitive"
						+ " providers, in topo order");
		assertTrue(runtime.isStarted("a"));
		assertTrue(runtime.isStarted("b"));
	}

	@Test
	void optionalAfterAndBeforeNeverTriggerActivation() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule soft = bare("soft", new Activation.OnDemand(), log);
		FakeModule late = bare("late", new Activation.OnDemand(), log);
		FakeModule early = bare("early", new Activation.OnDemand(),
				log);
		FakeModule user = new FakeModule(
				manifest("user", Set.of(), Set.of(), Set.of("soft"),
						Set.of("late"), Set.of("early"),
						new Activation.OnDemand()),
				log, false);
		ModuleRuntime runtime = ModuleRuntime
				.boot(List.of(soft, late, early, user));
		assertTrue(runtime.demand("user"));
		assertEquals(List.of("user"), runtime.startedIds(),
				"optional, after, and before must pull nothing into"
						+ " activation");
		assertFalse(runtime.isStarted("soft"));
		assertFalse(runtime.isStarted("late"));
		assertFalse(runtime.isStarted("early"));
	}

	@Test
	void throwingStartFailsPermanentlyNamingModuleAndPhase() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule broken = new FakeModule(
				manifest("broken", Set.of(), Set.of(), Set.of(),
						Set.of(), Set.of(),
						new Activation.OnCommand("go")),
				log, true);
		ModuleRuntime runtime = ModuleRuntime.boot(List.of(broken));
		ModuleActivationException first = assertThrows(
				ModuleActivationException.class,
				() -> runtime.dispatchCommand("go"));
		assertTrue(first.getMessage().contains("broken"),
				"the failing module must be named: "
						+ first.getMessage());
		assertTrue(first.getMessage().contains("start"),
				"the failing phase must be named: "
						+ first.getMessage());
		assertTrue(first.getCause() instanceof IllegalStateException,
				"the module's own exception must be the cause");
		assertFalse(runtime.isStarted("broken"));
		ModuleActivationException again = assertThrows(
				ModuleActivationException.class,
				() -> runtime.dispatchCommand("go"));
		assertSame(first, again,
				"a failed module must rethrow its recorded failure");
		ModuleActivationException demanded = assertThrows(
				ModuleActivationException.class,
				() -> runtime.demand("broken"));
		assertSame(first, demanded,
				"every later touch rethrows; no other trigger path"
						+ " may retry");
		assertEquals(1, broken.startCalls,
				"a failed start must never be silently retried");
	}

	@Test
	void throwingRegisterFailsBootNamingModuleAndPhase() {
		List<String> log = new ArrayList<>();
		JlsModule broken = new JlsModule() {

			@Override
			public ModuleManifest manifest() {
				return ModuleRuntimeTest.manifest("reg.broken",
						Set.of(), Set.of(), Set.of(), Set.of(),
						Set.of(), new Activation.OnDemand());
			}

			@Override
			public void register(ExtensionRegistry registrar) {
				throw new IllegalStateException("register boom");
			}

			@Override
			public void start() {
				log.add("start:reg.broken");
			}

		};
		ModuleActivationException ex = assertThrows(
				ModuleActivationException.class,
				() -> ModuleRuntime.boot(List.of(broken)));
		assertTrue(ex.getMessage().contains("reg.broken"),
				"the failing module must be named: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("register"),
				"the failing phase must be named: " + ex.getMessage());
		assertEquals(List.of(), log,
				"no start may run when registration fails");
	}

	@Test
	void bootIsDeterministicAcrossInputPermutations() throws
			ModuleResolutionException, ModuleActivationException {
		// the resolver test's layered graph, all eager, so the start
		// log itself is the determinism witness
		List<String> expectedLog = new ArrayList<>();
		List<String> expectedStarted = new ArrayList<>();
		Random shuffler = new Random(220);
		for (int permutation = 0; permutation < 25; permutation++) {
			List<String> log = new ArrayList<>();
			List<JlsModule> modules = new ArrayList<>(List.of(
					new FakeModule(manifest("core", Set.of("kernel"),
							Set.of(), Set.of(), Set.of(), Set.of(),
							new Activation.Eager()), log, false),
					new FakeModule(
							manifest("elem.registry",
									Set.of("element-registry"),
									Set.of("kernel"), Set.of(),
									Set.of(), Set.of(),
									new Activation.Eager()),
							log, false),
					requiring("gui", Set.of("element-registry"),
							new Activation.Eager(), log),
					new FakeModule(manifest("hdl", Set.of(),
							Set.of("element-registry"), Set.of(),
							Set.of(), Set.of("batch"),
							new Activation.Eager()), log, false),
					requiring("batch", Set.of("element-registry"),
							new Activation.Eager(), log)));
			Collections.shuffle(modules, shuffler);
			ModuleRuntime runtime = ModuleRuntime.boot(modules);
			if (permutation == 0) {
				expectedLog.addAll(log);
				expectedStarted.addAll(runtime.startedIds());
				assertEquals(List.of("core", "elem.registry", "gui",
						"hdl", "batch"), expectedStarted);
			} else {
				assertEquals(expectedLog, log,
						"the full register/start log must not depend"
								+ " on discovery order (permutation "
								+ permutation + ")");
				assertEquals(expectedStarted, runtime.startedIds());
			}
		}
	}

	/** A test-only extension point contributions read back through. */
	private static final ExtensionPoint<String> POINT =
			new ExtensionPoint<String>("test.point", String.class);

	/**
	 * A module that records the registrar it was handed and contributes
	 * one string to {@link #POINT} during phase 1.
	 */
	private static final class ContribModule implements JlsModule {

		/** The static descriptor this module reports. */
		private final ModuleManifest manifest;

		/** The string this module contributes to the test point. */
		private final String contribution;

		/** The registrar phase 1 handed this module, or null. */
		private ExtensionRegistry seenRegistrar;

		ContribModule(ModuleManifest manifest, String contribution) {
			this.manifest = manifest;
			this.contribution = contribution;
		}

		@Override
		public ModuleManifest manifest() {
			return manifest;
		}

		@Override
		public void register(ExtensionRegistry registrar) {
			seenRegistrar = registrar;
			registrar.contribute(POINT, manifest.id(), contribution);
		}

		@Override
		public void start() {
			// no cross-module references to bind
		}

	} // end of ContribModule class

	@Test
	void bootHandsRegistrarToRegisterAndReadsContributionsInTopoOrder()
			throws ModuleResolutionException, ModuleActivationException {
		ContribModule core = new ContribModule(
				manifest("core", Set.of(), Set.of(), Set.of(), Set.of(),
						Set.of(), new Activation.Eager()),
				"a");
		ContribModule dependent = new ContribModule(
				manifest("dependent", Set.of(), Set.of("core"), Set.of(),
						Set.of(), Set.of(), new Activation.Eager()),
				"b");
		ExtensionRegistry registry =
				new ExtensionRegistry(List.of(POINT));
		ModuleRuntime runtime = ModuleRuntime
				.boot(List.of(dependent, core), registry);
		assertSame(registry, runtime.extensionRegistry(),
				"the runtime must expose the host's registry");
		assertSame(registry, core.seenRegistrar,
				"register() must receive the non-null host registry");
		assertSame(registry, dependent.seenRegistrar);
		assertEquals(List.of("a", "b"),
				registry.contributions(POINT),
				"contributions read back in deterministic topological"
						+ " register order, independent of input order");
	}

	@Test
	void bootCollectionOverloadStartsWithAnEmptyRegistry() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> log = new ArrayList<>();
		FakeModule core = bare("core", new Activation.Eager(), log);
		FakeModule gui = requiring("gui", Set.of("core"),
				new Activation.Eager(), log);
		ModuleRuntime runtime = ModuleRuntime.boot(List.of(gui, core));
		assertEquals(List.of("register:core", "register:gui",
				"start:core", "start:gui"), log,
				"the single-argument overload keeps the pre-existing"
						+ " register/start behavior");
		assertEquals(List.of("core", "gui"), runtime.startedIds());
		assertTrue(runtime.extensionRegistry().points().isEmpty(),
				"the single-argument overload boots over an empty,"
						+ " point-less registry");
	}

	@Test
	void bootPropagatesResolutionFailuresUntouched() {
		List<String> log = new ArrayList<>();
		FakeModule orphan = requiring("orphan",
				Set.of("board-constraints"), new Activation.Eager(),
				log);
		ModuleResolutionException ex = assertThrows(
				ModuleResolutionException.class,
				() -> ModuleRuntime.boot(List.of(orphan)));
		assertTrue(ex.getMessage().contains("board-constraints"),
				"the resolver's diagnostic must pass through: "
						+ ex.getMessage());
		assertEquals(0, orphan.registerCalls,
				"nothing may register when resolution fails");
		FakeModule one = bare("dup", new Activation.Eager(), log);
		FakeModule two = bare("dup", new Activation.Eager(), log);
		assertThrows(ModuleResolutionException.class,
				() -> ModuleRuntime.boot(List.of(one, two)),
				"duplicate ids must be rejected via the resolver");
	}

} // end of ModuleRuntimeTest class
