package jls.edit;

import jls.module.ExtensionPoint;

/**
 * The extension points published by the GUI layer (issue #223): the
 * typed identities modules contribute editor-facing descriptors
 * through. Constants live here, in the seam's home package, keeping
 * {@code jls.module} AWT-free and mechanism-only — the host layer
 * owns its own seams. Catalogued in {@code docs/extension-points.md}
 * and pinned by {@code ExtensionPointCatalogTest}.
 */
public final class GuiExtensionPoints {

	/**
	 * The palette-contributor seam (issue #78, grand-architecture
	 * §4.3 seam 2): contributions are {@link PaletteEntry}
	 * descriptors — the GUI half of the two-layer element descriptor
	 * (icon, fallback text, tooltip, toolbar group, help topic) whose
	 * core half is {@code jls.elem.ElementType}. The built-in
	 * contributions are the rows of the static {@link Palette} table;
	 * the #84 SimpleEditor decomposition routes palette construction
	 * through this seam.
	 */
	public static final ExtensionPoint<PaletteEntry> PALETTE_CONTRIBUTOR =
			new ExtensionPoint<PaletteEntry>("gui.palette-contributor",
					PaletteEntry.class);

	/** Not instantiable: a constant holder only. */
	private GuiExtensionPoints() {
	} // end of constructor

} // end of GuiExtensionPoints class
