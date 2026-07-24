package jls.edit;

/**
 * The single registration point for the GUI-side element renderers and
 * creation dialogs extracted by the #77 element long-tail. As each
 * element is converted to a headless model, its renderer and (if any)
 * its dialog are registered here; {@link ElementRenderers} and
 * {@link ElementDialogs} each trigger this once, before any draw or
 * placement, so the registrations are in place for both the interactive
 * GUI and the headless image-export path.
 *
 * <p>Empty today (issue #77 W0 P0.5): the registry seams exist and every
 * element still draws and sets itself up, so behavior is unchanged. The
 * element wave adds one {@code ElementRenderers.register(...)} (and, for
 * elements with a dialog, {@code ElementDialogs.register(...)}) line here
 * per converted element.
 */
final class BuiltinElementRenderers {

	/** True once the registrations have run, so it happens exactly once. */
	private static boolean installed = false;

	private BuiltinElementRenderers() {} // no instances

	/**
	 * Register every extracted element renderer and dialog. Idempotent.
	 */
	static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;

		// The element wave (#77 W2) registers converted elements here:
		//   ElementRenderers.register(Foo.class, new FooRenderer());
		//   ElementDialogs.register(Foo.class, new FooDialog());
		ElementRenderers.register(jls.elem.Adder.class, new AdderRenderer());
		ElementDialogs.register(jls.elem.Adder.class, new AdderDialog());
		ElementRenderers.register(jls.elem.TriState.class, new TriStateRenderer());
		ElementDialogs.register(jls.elem.TriState.class, new TriStateDialog());
		ElementRenderers.register(jls.elem.SigGen.class, new SigGenRenderer());
		ElementDialogs.register(jls.elem.SigGen.class, new SigGenDialog(true));
		ElementDialogs.registerChange(jls.elem.SigGen.class,
				new SigGenDialog(false));
	} // end of install method

} // end of BuiltinElementRenderers class
