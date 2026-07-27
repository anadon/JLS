package jls.module;

/**
 * The lifecycle SPI one module implements (issue #220,
 * grand-architecture §4.2 rule 7): a static, side-effect-free
 * {@link #manifest()} the resolver reads without running module code,
 * plus the two-phase {@link #register()} / {@link #start()} pair that
 * {@link ModuleRuntime} drives. The two phases are the only cycle
 * escape hatch the model offers: every module registers before any
 * module starts, so a started module may safely resolve references to
 * anything any other module published during registration.
 *
 * <p>Deliberately deferred from this slice (issue #223): sealing this
 * interface over the compiled-in module set — it is plain for now so
 * tests and future tenants can implement it freely — and the registrar
 * parameter through which {@code register()} will publish typed
 * extension points; until that parameter exists, {@code register()}
 * takes no arguments and publishes nothing beyond the module's own
 * construction.</p>
 */
public interface JlsModule {

	/**
	 * The static descriptor of this module. Must be stable: every call
	 * returns an equal manifest, with no side effects — the runtime
	 * reads it once at boot and reasons from that snapshot.
	 *
	 * @return this module's manifest.
	 */
	ModuleManifest manifest();

	/**
	 * Phase 1: construct internal state and publish what this module
	 * offers. Called exactly once per module, in topological order,
	 * before <em>any</em> module's {@link #start()}. Must not touch any
	 * other module — the other module may not have registered yet.
	 */
	void register();

	/**
	 * Phase 2: resolve references to other modules and go live. Called
	 * at most once, in topological order, after every module has
	 * registered — at boot for {@link Activation.Eager} modules,
	 * on first trigger otherwise. May use anything a dependency
	 * published, because every {@code requires} provider is started
	 * first.
	 */
	void start();

} // end of JlsModule interface
