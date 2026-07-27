package jls.boot;

import java.util.Set;

import jls.module.Activation;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleManifest;

/**
 * The compiled-in collaboration module (issue #220): the manifest for
 * the op-stream layer that owns the {@code collab.op-observer} seam
 * (grand-architecture §4.3 seam 4). JLS ships no built-in
 * {@code OpSink} observer today — undo snapshots and replication (#171)
 * plug in here later — so this module contributes nothing yet; it
 * exists so the layer is a first-class citizen of the module graph
 * rather than implicit package coupling. Requires {@link CoreModule},
 * so it registers and starts after the model layer, and is AWT-free.
 */
public final class CollabModule implements JlsModule {

	/** The stable module id. */
	public static final String ID = "collab";

	/** Create the collab module; it holds no per-instance state. */
	public CollabModule() {
	} // end of constructor

	@Override
	public ModuleManifest manifest() {

		return new ModuleManifest(ID, 1, Set.of(), Set.of(CoreModule.ID),
				Set.of(), Set.of(), Set.of(), new Activation.Eager());
	} // end of manifest method

	@Override
	public void register(ExtensionRegistry registrar) {
		// no built-in op observer ships today; the seam stays declared
		// and empty until a first-party sink (undo, replication #171)
		// contributes here
	} // end of register method

	@Override
	public void start() {
		// nothing to resolve at go-live
	} // end of start method

} // end of CollabModule class
