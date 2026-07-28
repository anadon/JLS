package jls.boot;

import java.util.List;

import jls.collab.op.OpExtensionPoints;
import jls.edit.GuiExtensionPoints;
import jls.elem.ElementExtensionPoints;
import jls.hdl.HdlExtensionPoints;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleActivationException;
import jls.module.ModuleResolutionException;
import jls.module.ModuleRuntime;

/**
 * The compiled-in module assembly (issue #220, grand-architecture §4):
 * the single place that names JLS's shipped subsystems as
 * {@link JlsModule} manifests, declares the host's typed extension
 * points, and boots them through {@link ModuleRuntime}'s
 * resolve → register → start path. The application entry point calls
 * {@link #boot()} once at startup; consumers read the accumulated
 * contributions back through the returned runtime's
 * {@link ModuleRuntime#extensionRegistry()}.
 *
 * <p>Four points are declared up front (the host publishes seams and
 * never names a module): {@link ElementExtensionPoints#ELEMENT_PROVIDER}
 * (core), {@link GuiExtensionPoints#PALETTE_CONTRIBUTOR} (gui),
 * {@link HdlExtensionPoints#EXPORTER} (hdl), and
 * {@link OpExtensionPoints#OP_OBSERVER} (collab). Four modules are
 * wired: {@link CoreModule}, {@link GuiModule}, {@link HdlModule}, and
 * {@link CollabModule}. Booting is side-effect-free with respect to
 * observable behavior — the registry is populated but nothing reads it
 * for dispatch yet — so batch, HDL, and GUI behavior is unchanged and
 * stays pinned by the existing golden suite.</p>
 */
public final class JlsModules {

	/** Not instantiable: a static boot entry point only. */
	private JlsModules() {
	} // end of constructor

	/**
	 * The host's declared extension points, in declaration order: one
	 * per shipped subsystem seam.
	 *
	 * @return a fresh, empty {@link ExtensionRegistry} over the four
	 *         declared points, ready for modules to contribute into.
	 */
	public static ExtensionRegistry registry() {

		return new ExtensionRegistry(List.of(
				ElementExtensionPoints.ELEMENT_PROVIDER,
				GuiExtensionPoints.PALETTE_CONTRIBUTOR,
				HdlExtensionPoints.EXPORTER,
				OpExtensionPoints.OP_OBSERVER));
	} // end of registry method

	/**
	 * The four compiled-in modules, in arbitrary order (the resolver
	 * imposes the deterministic topological start order).
	 *
	 * @return a fresh list of the shipped module instances.
	 */
	public static List<JlsModule> modules() {

		return List.of(new CoreModule(), new GuiModule(), new HdlModule(),
				new CollabModule());
	} // end of modules method

	/**
	 * Resolve, register, and eagerly start the four compiled-in modules
	 * over a freshly declared extension registry. Called once at
	 * application startup, for every run mode.
	 *
	 * @return the booted runtime, its
	 *         {@link ModuleRuntime#extensionRegistry()} carrying every
	 *         module's contributions.
	 *
	 * @throws ModuleResolutionException if the manifests do not resolve.
	 * @throws ModuleActivationException if a module's registration or
	 *             eager start throws.
	 */
	public static ModuleRuntime boot()
			throws ModuleResolutionException, ModuleActivationException {

		return ModuleRuntime.boot(modules(), registry());
	} // end of boot method

} // end of JlsModules class
