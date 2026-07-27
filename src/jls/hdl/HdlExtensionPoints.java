package jls.hdl;

import jls.module.ExtensionPoint;

/**
 * The extension points published by the HDL-export layer (issue
 * #223): the typed identities modules contribute emitters through.
 * Constants live here, in the seam's home package, so
 * {@code jls.module} stays a pure mechanism that names no contract —
 * the host layer owns its own seams. Catalogued in
 * {@code docs/extension-points.md} and pinned by
 * {@code ExtensionPointCatalogTest}.
 */
public final class HdlExtensionPoints {

	/**
	 * The exporter seam (issue #60, grand-architecture §4.3 seam 3):
	 * contributions are {@link HdlEmitter} implementations that
	 * render an {@link HdlModel} as source text in one language. The
	 * built-in contributions are {@link VerilogEmitter} and
	 * {@link VhdlEmitter}; board-aware export (#213) and bitstream
	 * handoff (#215) extend this seam.
	 */
	public static final ExtensionPoint<HdlEmitter> EXPORTER =
			new ExtensionPoint<HdlEmitter>("hdl.exporter",
					HdlEmitter.class);

	/** Not instantiable: a constant holder only. */
	private HdlExtensionPoints() {
	} // end of constructor

} // end of HdlExtensionPoints class
