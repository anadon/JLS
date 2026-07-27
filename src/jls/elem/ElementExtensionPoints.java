package jls.elem;

import jls.module.ExtensionPoint;

/**
 * The extension points published by the circuit-model layer (issue
 * #223): the typed identities modules contribute element descriptors
 * through. Constants live here, in the seam's home package, so
 * {@code jls.module} stays a pure mechanism that names no contract —
 * the host layer owns its own seams. Catalogued in
 * {@code docs/extension-points.md} and pinned by
 * {@code ExtensionPointCatalogTest}.
 */
public final class ElementExtensionPoints {

	/**
	 * The element-provider seam (issue #78, grand-architecture §4.3
	 * seam 1): contributions are {@link ElementType} descriptors —
	 * tag, aliases, class, factory — exactly the rows of the shipped
	 * {@link ElementRegistry} table, which is this point's built-in
	 * contribution set. Consumed by the loader, batch tooling, and
	 * the HDL cell map (#61); external providers (#212) plug in here
	 * when that demand gate opens.
	 */
	public static final ExtensionPoint<ElementType> ELEMENT_PROVIDER =
			new ExtensionPoint<ElementType>("elem.element-provider",
					ElementType.class);

	/** Not instantiable: a constant holder only. */
	private ElementExtensionPoints() {
	} // end of constructor

} // end of ElementExtensionPoints class
