package jls.edit;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.jspecify.annotations.Nullable;

import jls.elem.Element;

/**
 * The GUI-side element creation-dialog registry (issue #77). Element
 * placement dispatches through {@link #setup} instead of calling
 * {@code element.setup(...)} directly, so an element whose dialog has
 * moved off Swing into an {@link ElementDialog} uses that dialog, while
 * an unconverted element still runs its own {@code setup}. Empty until
 * the element wave populates {@link BuiltinElementRenderers#install()},
 * so this dispatch is byte-identical when no dialog is registered.
 */
public final class ElementDialogs {

	/** Registered creation dialogs, keyed by element class. */
	private static final Map<Class<?>, ElementDialog> BY_TYPE =
			new HashMap<Class<?>, ElementDialog>();
	/** Registered change (edit) dialogs, keyed by element class. */
	private static final Map<Class<?>, ElementDialog> CHANGE_BY_TYPE =
			new HashMap<Class<?>, ElementDialog>();

	static {
		BuiltinElementRenderers.install();
	}

	/**
	 * No instances.
	 */
	private ElementDialogs() {} // no instances

	/**
	 * Register the creation dialog for an element type.
	 *
	 * @param type The element class.
	 * @param dialog Its dialog.
	 */
	public static void register(Class<? extends Element> type,
			ElementDialog dialog) {
		BY_TYPE.put(type, dialog);
	} // end of register method

	/**
	 * Register the change (edit) dialog for an element type. The dialog's
	 * {@code setup(...)} runs the change form (both are "present the
	 * dialog and apply the result"), so a single relocated dialog class
	 * can serve creation and change.
	 *
	 * @param type The element class.
	 * @param dialog Its change dialog.
	 */
	public static void registerChange(Class<? extends Element> type,
			ElementDialog dialog) {
		CHANGE_BY_TYPE.put(type, dialog);
	} // end of registerChange method

	/**
	 * Run an element's change (edit) dialog through its registered change
	 * dialog, or through the element's own {@code change} if none is
	 * registered.
	 *
	 * @param el The element being changed.
	 * @param g The graphics context (for sizing).
	 * @param editWindow The editor panel to parent the dialog to.
	 * @param x The element's x-coordinate.
	 * @param y The element's y-coordinate.
	 * @return true if the element's size changed and it must be
	 *         repositioned, matching {@code Element.change}.
	 */
	public static boolean change(Element el, Graphics g, JPanel editWindow,
			int x, int y) {
		@Nullable ElementDialog dialog = CHANGE_BY_TYPE.get(el.getClass());
		if (dialog != null) {
			return dialog.setup(el, g, editWindow, x, y);
		}
		// every changeable element now registers a dialog (issue #77); an
		// element with none simply has no change dialog, matching the
		// former Element.change default of "did not change".
		return false;
	} // end of change method

	/**
	 * Run an element's creation dialog through its registered dialog, or
	 * through the element's own {@code setup} if none is registered.
	 *
	 * @param el The element being created.
	 * @param g The graphics context (for sizing).
	 * @param editWindow The editor panel to parent the dialog to.
	 * @param x The drop x-coordinate.
	 * @param y The drop y-coordinate.
	 * @return true if the user cancelled, matching {@code Element.setup}.
	 */
	public static boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {
		@Nullable ElementDialog dialog = BY_TYPE.get(el.getClass());
		if (dialog != null) {
			return dialog.setup(el, g, editWindow, x, y);
		}
		// every element with a creation dialog now registers one (issue
		// #77); an element with none is placed without a dialog, matching
		// the former Element.setup default of "not cancelled".
		return false;
	} // end of setup method

} // end of ElementDialogs class
