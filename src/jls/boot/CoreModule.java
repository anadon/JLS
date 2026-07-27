package jls.boot;

import java.util.Set;

import jls.elem.ElementExtensionPoints;
import jls.elem.ElementRegistry;
import jls.elem.ElementType;
import jls.module.Activation;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleManifest;

/**
 * The compiled-in circuit-model module (issue #220): the kernel every
 * other module depends on. It contributes the shipped
 * {@link ElementRegistry} table — one {@link ElementType} descriptor per
 * registered element — to the {@code elem.element-provider} seam, which
 * makes the element registry the canonical built-in instance of the
 * extension-point mechanism (grand-architecture §4.3 seam 1). Eager, so
 * the model layer is live from boot.
 */
public final class CoreModule implements JlsModule {

	/** The stable module id every other compiled-in module requires. */
	public static final String ID = "core";

	/** Create the core module; all state is the static element table. */
	public CoreModule() {
	} // end of constructor

	@Override
	public ModuleManifest manifest() {

		return new ModuleManifest(ID, 1, Set.of(), Set.of(), Set.of(),
				Set.of(), Set.of(), new Activation.Eager());
	} // end of manifest method

	@Override
	public void register(ExtensionRegistry registrar) {

		for (ElementType type : ElementRegistry.all()) {
			registrar.contribute(
					ElementExtensionPoints.ELEMENT_PROVIDER, ID, type);
		}
	} // end of register method

	@Override
	public void start() {
		// nothing to resolve: the model layer has no cross-module
		// references to bind at go-live
	} // end of start method

} // end of CoreModule class
