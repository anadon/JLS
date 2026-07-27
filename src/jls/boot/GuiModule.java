package jls.boot;

import java.util.Set;

import jls.edit.GuiExtensionPoints;
import jls.edit.Palette;
import jls.edit.PaletteEntry;
import jls.module.Activation;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleManifest;

/**
 * The compiled-in GUI module (issue #220): contributes the built-in
 * palette to the {@code gui.palette-contributor} seam
 * (grand-architecture §4.3 seam 2) — one {@link PaletteEntry} per row of
 * the static {@link Palette} table. Requires {@link CoreModule}, so it
 * registers and starts after the model layer. {@link Palette} and
 * {@link PaletteEntry} reference only {@code jls.elem}, so contributing
 * them touches no AWT and boot stays safe in headless modes; {@code
 * start()} likewise binds nothing Swing.
 */
public final class GuiModule implements JlsModule {

	/** The stable module id. */
	public static final String ID = "gui";

	/** Create the GUI module; its state is the static palette table. */
	public GuiModule() {
	} // end of constructor

	@Override
	public ModuleManifest manifest() {

		return new ModuleManifest(ID, 1, Set.of(), Set.of(CoreModule.ID),
				Set.of(), Set.of(), Set.of(), new Activation.Eager());
	} // end of manifest method

	@Override
	public void register(ExtensionRegistry registrar) {

		for (PaletteEntry entry : Palette.entries()) {
			registrar.contribute(
					GuiExtensionPoints.PALETTE_CONTRIBUTOR, ID, entry);
		}
	} // end of register method

	@Override
	public void start() {
		// AWT-free: the editor pulls the palette from the seam on demand;
		// nothing here initializes Swing
	} // end of start method

} // end of GuiModule class
