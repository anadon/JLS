package jls.boot;

import java.util.Set;

import jls.hdl.HdlExtensionPoints;
import jls.hdl.VerilogEmitter;
import jls.hdl.VhdlEmitter;
import jls.module.Activation;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleManifest;

/**
 * The compiled-in HDL-export module (issue #220): contributes the two
 * built-in emitters — {@link VerilogEmitter} then {@link VhdlEmitter} —
 * to the {@code hdl.exporter} seam (grand-architecture §4.3 seam 3),
 * each via its public no-arg constructor and the {@code emit(HdlModel)}
 * contract. Requires {@link CoreModule}, so it registers and starts
 * after the model layer. The emitters are pure string builders that
 * touch no AWT, so boot stays safe in headless HDL and batch modes.
 */
public final class HdlModule implements JlsModule {

	/** The stable module id. */
	public static final String ID = "hdl";

	/** Create the HDL module; its state is the two stateless emitters. */
	public HdlModule() {
	} // end of constructor

	@Override
	public ModuleManifest manifest() {

		return new ModuleManifest(ID, 1, Set.of(), Set.of(CoreModule.ID),
				Set.of(), Set.of(), Set.of(), new Activation.Eager());
	} // end of manifest method

	@Override
	public void register(ExtensionRegistry registrar) {

		registrar.contribute(HdlExtensionPoints.EXPORTER, ID,
				new VerilogEmitter());
		registrar.contribute(HdlExtensionPoints.EXPORTER, ID,
				new VhdlEmitter());
	} // end of register method

	@Override
	public void start() {
		// nothing to resolve: the emitters are stateless and pull their
		// model from the caller at export time
	} // end of start method

} // end of HdlModule class
