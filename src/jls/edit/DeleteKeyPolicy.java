package jls.edit;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;

import javax.swing.KeyStroke;

/**
 * Key-binding policy for deleting selected elements (issue #127).
 *
 * Deletion was bound only to forward-<kbd>Delete</kbd> ({@code VK_DELETE}),
 * a key Apple laptop and compact keyboards do not have — Mac users could
 * not delete from the keyboard at all. Two fork lineages patched this
 * independently (jwetzell 2016, bsiever 2024/25).
 *
 * The policy, decided from the issue #127 Backspace-consumer census
 * (no other Backspace consumer exists in the editor's canvas focus
 * context, and the canvas bindings live in the WHEN_FOCUSED input map,
 * so focused text components keep their own Backspace):
 * <ul>
 * <li>the editor canvas binds <em>both</em> plain <kbd>Delete</kbd> and
 *     plain <kbd>⌫</kbd> Backspace to the delete action, on every
 *     platform ({@link #canvasBindings()});</li>
 * <li>a menu item can display only one accelerator, so the popup shows
 *     the key the platform's keyboards actually have: Backspace on
 *     macOS, Delete everywhere else ({@link #menuAccelerator(String)}).
 *     Both keys work everywhere regardless of what is displayed.</li>
 * </ul>
 *
 * Like {@code jls.ToolkitPolicy}, the decision is a pure function of an
 * injected {@code os.name} value, so the whole matrix is unit-testable
 * headless (the surefire JVM runs with {@code java.awt.headless=true},
 * where the editor itself cannot be constructed).
 */
final class DeleteKeyPolicy {

	private DeleteKeyPolicy() {
		// static use only
	}

	/**
	 * The keystrokes the editor canvas binds to the delete action, on
	 * every platform: plain forward-Delete and plain Backspace, no
	 * modifiers.
	 *
	 * @return the canvas delete bindings.
	 */
	static List<KeyStroke> canvasBindings() {

		return List.of(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
				KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
	} // end of canvasBindings method

	/**
	 * The single accelerator the delete menu item displays: Backspace on
	 * macOS (whose keyboards have no forward-Delete key), forward-Delete
	 * everywhere else.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator to display.
	 */
	static KeyStroke menuAccelerator(String osName) {

		if (isMac(osName))
			return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
	} // end of menuAccelerator method

	/**
	 * Whether an os.name value names macOS. Apple has used "Mac OS X",
	 * "OS X", and "macOS" over the years; all contain "mac" or "os x".
	 *
	 * @param osName value of the os.name system property, or null.
	 *
	 * @return true for macOS.
	 */
	static boolean isMac(String osName) {

		if (osName == null)
			return false;
		String os = osName.toLowerCase(Locale.ROOT);
		return os.contains("mac") || os.contains("os x");
	} // end of isMac method

} // end of DeleteKeyPolicy class
