package jls.module;

/**
 * The lifecycle SPI one module implements (issue #220,
 * grand-architecture §4.2 rule 7): a static, side-effect-free
 * {@link #manifest()} the resolver reads without running module code,
 * plus the two-phase {@link #register(ExtensionRegistry)} /
 * {@link #start()} pair that {@link ModuleRuntime} drives. The two
 * phases are the only cycle escape hatch the model offers: every module
 * registers before any module starts, so a started module may safely
 * resolve references to anything any other module published during
 * registration.
 *
 * <p>Registration publishes through the typed {@link ExtensionRegistry}
 * the runtime hands each module (issue #223, grand-architecture §4.3):
 * a module contributes its implementations to the host's declared
 * {@link ExtensionPoint}s and never names another module — the
 * inversion-of-control seam. The host declares the points up front; a
 * contribution to an undeclared point fails loudly.</p>
 *
 * <p>Deliberately deferred from this slice (issue #223): sealing this
 * interface over the compiled-in module set — it is plain for now so
 * tests and future tenants can implement it freely.</p>
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
	 * offers by contributing to the host's declared extension points.
	 * Called exactly once per module, in topological order, before
	 * <em>any</em> module's {@link #start()}. Must not touch any other
	 * module — the other module may not have registered yet — but may
	 * freely contribute to {@code registrar}, whose declared points the
	 * host published before boot.
	 *
	 * @param registrar The host's typed extension registry to contribute
	 *            implementations into.
	 */
	void register(ExtensionRegistry registrar);

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
