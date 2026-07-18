package jls;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Locale;

import javax.swing.KeyStroke;

/**
 * Menu-bar accelerator, mnemonic, and canvas key-binding policy
 * (issue #75).
 *
 * Before this policy existed the main menu bar carried not a single
 * accelerator or mnemonic outside Simulator Run/Stop (F5/F7): there was
 * no Ctrl/Cmd+S, +O, or +N anywhere, so a menu-browsing user was never
 * shown a shortcut and a keyboard user could not reach file operations
 * at all. Rotate and flip had no key binding, and redo was Ctrl+Y on
 * every platform, violating the macOS Shift+Cmd+Z convention.
 *
 * The policy centralizes those decisions:
 * <ul>
 * <li>standard file-operation accelerators on the platform menu
 *     modifier (Cmd on macOS, Ctrl elsewhere): N (new), O (open),
 *     S (save), Shift+S (save as), Q (exit);</li>
 * <li>redo displayed as Shift+Cmd+Z on macOS with Cmd+Y kept bound as
 *     a day-one alias per the issue #75 adjudication, and Ctrl+Y
 *     unchanged on Windows/Linux;</li>
 * <li>plain-key canvas bindings for rotate (R clockwise,
 *     Shift+R counter-clockwise) and flip (F);</li>
 * <li>a distinct mnemonic for each top-level menu (File, Simulator,
 *     Global, Help) so the menu bar is reachable with Alt+letter.</li>
 * </ul>
 *
 * Like {@code jls.edit.DeleteKeyPolicy} and the toolkit policy, every
 * platform decision is a pure function of an injected {@code os.name}
 * value — never of {@code Toolkit} (which throws HeadlessException
 * under surefire's {@code java.awt.headless=true}) — so the whole
 * matrix is unit-testable headless. The menu-shortcut modifier the
 * policy derives (Meta on macOS, Ctrl elsewhere) matches what
 * {@code Toolkit.getMenuShortcutKeyMaskEx()} reports on those
 * platforms at runtime.
 *
 * @see jls.MenuAcceleratorPolicyTest
 */
public final class MenuAcceleratorPolicy {

	/**
	 * Prevents instantiation; the class holds only static policy
	 * methods.
	 */
	private MenuAcceleratorPolicy() {
		// static use only
	} // end of MenuAcceleratorPolicy constructor

	/**
	 * Whether an os.name value names macOS. Apple has used "Mac OS X",
	 * "OS X", and "macOS" over the years; all contain "mac" or "os x".
	 *
	 * @param osName value of the os.name system property, or null.
	 *
	 * @return true for macOS.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#isMacRecognizesAppleOsNames()
	 */
	public static boolean isMac(String osName) {

		if (osName == null)
			return false;
		String os = osName.toLowerCase(Locale.ROOT);
		return os.contains("mac") || os.contains("os x");
	} // end of isMac method

	/**
	 * The platform menu-shortcut modifier: the Command key on macOS,
	 * Ctrl everywhere else. This is the headless-safe equivalent of
	 * {@code Toolkit.getMenuShortcutKeyMaskEx()}.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the extended modifier mask for menu accelerators.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#macUsesCommandOthersUseControl()
	 */
	public static int menuMask(String osName) {

		return isMac(osName) ? InputEvent.META_DOWN_MASK
				: InputEvent.CTRL_DOWN_MASK;
	} // end of menuMask method

	/**
	 * The File&gt;New accelerator: platform modifier + N.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#fileAcceleratorsUseThePlatformMask()
	 */
	public static KeyStroke newCircuit(String osName) {

		return KeyStroke.getKeyStroke(KeyEvent.VK_N, menuMask(osName));
	} // end of newCircuit method

	/**
	 * The File&gt;Open accelerator: platform modifier + O.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#fileAcceleratorsUseThePlatformMask()
	 */
	public static KeyStroke open(String osName) {

		return KeyStroke.getKeyStroke(KeyEvent.VK_O, menuMask(osName));
	} // end of open method

	/**
	 * The File&gt;Save accelerator: platform modifier + S.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#fileAcceleratorsUseThePlatformMask()
	 */
	public static KeyStroke save(String osName) {

		return KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask(osName));
	} // end of save method

	/**
	 * The File&gt;Save As accelerator: platform modifier + Shift + S,
	 * the near-universal desktop convention.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#saveAsIsShiftedSave()
	 */
	public static KeyStroke saveAs(String osName) {

		return KeyStroke.getKeyStroke(KeyEvent.VK_S,
				menuMask(osName) | InputEvent.SHIFT_DOWN_MASK);
	} // end of saveAs method

	/**
	 * The File&gt;Exit accelerator: platform modifier + Q (Cmd+Q is the
	 * macOS quit convention; Ctrl+Q is the common Linux/Windows
	 * application-quit binding).
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#fileAcceleratorsUseThePlatformMask()
	 */
	public static KeyStroke exit(String osName) {

		return KeyStroke.getKeyStroke(KeyEvent.VK_Q, menuMask(osName));
	} // end of exit method

	/**
	 * The redo accelerator a menu item displays: Shift+Cmd+Z on macOS
	 * (the platform convention), Ctrl+Y everywhere else (the
	 * historical JLS binding, unchanged on Windows/Linux).
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the accelerator to display.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#macRedoIsShiftCommandZ()
	 * @see jls.MenuAcceleratorPolicyTest#otherPlatformsRedoStaysControlY()
	 */
	public static KeyStroke redoDisplayed(String osName) {

		if (isMac(osName))
			return KeyStroke.getKeyStroke(KeyEvent.VK_Z,
					InputEvent.META_DOWN_MASK
							| InputEvent.SHIFT_DOWN_MASK);
		return KeyStroke.getKeyStroke(KeyEvent.VK_Y,
				InputEvent.CTRL_DOWN_MASK);
	} // end of redoDisplayed method

	/**
	 * Every keystroke the editor canvas binds to redo. On macOS that
	 * is the conventional Shift+Cmd+Z plus Cmd+Y — the historical JLS
	 * binding kept as a day-one alias per the issue #75 adjudication
	 * (rebindings never strand users trained on the old keys). On
	 * other platforms it is Ctrl+Y alone, exactly as before.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 *
	 * @return the redo bindings, displayed accelerator first.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#macRedoKeepsTheOldBindingAsAnAlias()
	 * @see jls.MenuAcceleratorPolicyTest#otherPlatformsRedoStaysControlY()
	 */
	public static List<KeyStroke> redoBindings(String osName) {

		if (isMac(osName))
			return List.of(redoDisplayed(osName),
					KeyStroke.getKeyStroke(KeyEvent.VK_Y,
							InputEvent.META_DOWN_MASK));
		return List.of(redoDisplayed(osName));
	} // end of redoBindings method

	/**
	 * The clockwise-rotate canvas binding: plain R, no modifier, on
	 * every platform. A plain key cannot collide with any menu-mask
	 * chord, and the canvas has no text field to steal typing from.
	 *
	 * @return the rotate-clockwise keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#rotateAndFlipArePlainKeys()
	 */
	public static KeyStroke rotateCwStroke() {

		return KeyStroke.getKeyStroke(KeyEvent.VK_R, 0);
	} // end of rotateCwStroke method

	/**
	 * The counter-clockwise-rotate canvas binding: Shift+R on every
	 * platform, the reverse of plain-R clockwise rotation.
	 *
	 * @return the rotate-counter-clockwise keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#rotateAndFlipArePlainKeys()
	 */
	public static KeyStroke rotateCcwStroke() {

		return KeyStroke.getKeyStroke(KeyEvent.VK_R,
				InputEvent.SHIFT_DOWN_MASK);
	} // end of rotateCcwStroke method

	/**
	 * The flip canvas binding: plain F, no modifier, on every
	 * platform.
	 *
	 * @return the flip keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#rotateAndFlipArePlainKeys()
	 */
	public static KeyStroke flipStroke() {

		return KeyStroke.getKeyStroke(KeyEvent.VK_F, 0);
	} // end of flipStroke method

	/**
	 * The view-current-value canvas binding: plain V, no modifier, on
	 * every platform. Historically this was menu-mask + S, which would
	 * shadow the menu bar's Save accelerator whenever the canvas had
	 * focus — the exact "Ctrl+S saves from any focus location" trap
	 * issue #75 exists to close — so this is a deliberate rebinding
	 * with no alias: keeping the old stroke bound would keep shadowing
	 * Save (the issue's "alias where unambiguous" rule).
	 *
	 * @return the view-value keystroke.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#viewValueIsPlainVNotMaskedS()
	 */
	public static KeyStroke viewValueStroke() {

		return KeyStroke.getKeyStroke(KeyEvent.VK_V, 0);
	} // end of viewValueStroke method

	/**
	 * The File menu mnemonic (Alt+F opens the menu on platforms whose
	 * look and feel shows mnemonics; macOS ignores them by design).
	 *
	 * @return the File menu mnemonic key code.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#menuMnemonicsAreDistinct()
	 */
	public static int fileMenuMnemonic() {

		return KeyEvent.VK_F;
	} // end of fileMenuMnemonic method

	/**
	 * The Simulator menu mnemonic.
	 *
	 * @return the Simulator menu mnemonic key code.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#menuMnemonicsAreDistinct()
	 */
	public static int simulatorMenuMnemonic() {

		return KeyEvent.VK_S;
	} // end of simulatorMenuMnemonic method

	/**
	 * The Global menu mnemonic.
	 *
	 * @return the Global menu mnemonic key code.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#menuMnemonicsAreDistinct()
	 */
	public static int globalMenuMnemonic() {

		return KeyEvent.VK_G;
	} // end of globalMenuMnemonic method

	/**
	 * The Help menu mnemonic.
	 *
	 * @return the Help menu mnemonic key code.
	 *
	 * @see jls.MenuAcceleratorPolicyTest#menuMnemonicsAreDistinct()
	 */
	public static int helpMenuMnemonic() {

		return KeyEvent.VK_H;
	} // end of helpMenuMnemonic method

} // end of MenuAcceleratorPolicy class
