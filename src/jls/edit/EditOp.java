package jls.edit;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import jls.MenuAcceleratorPolicy;

/**
 * The editing operations that the interactive editor exposes through
 * more than one surface (issue #75).
 *
 * <p>Before this enum each operation existed as several independent
 * copies: a popup {@code JMenuItem} wired to a giant source-equality
 * {@code actionPerformed} switch, and a separate anonymous
 * {@code AbstractAction} registered in the canvas key bindings. Adding a
 * menu-bar item would have made a third copy. Instead each editor builds
 * exactly one {@link javax.swing.Action} per {@code EditOp}
 * (see {@link SimpleEditor#editAction(EditOp)}) and the popup menus, the
 * canvas key bindings, and the menu-bar Edit and Element menus all reuse
 * that single {@code Action} object &mdash; so the three surfaces are no
 * longer separate copies.</p>
 *
 * <p>Each constant carries the human-readable label its menu items show.
 * The accelerator keystroke is not stored here: it is platform-aware and
 * comes from {@link jls.MenuAcceleratorPolicy} /
 * {@link DeleteKeyPolicy} when the editor builds the {@code Action}, so
 * this enum stays a pure, headless-safe descriptor.</p>
 *
 * @jls.testedby jls.ui.EditActionMatrixTest
 */
public enum EditOp {

	/** Attach or remove a probe on the selected wire. */
	PROBE("Probe"),
	/** Toggle watched status of the selected element. */
	WATCH("Watch"),
	/** View or modify the selected element's details. */
	MODIFY("Modify"),
	/** Change the selected element's propagation delay or access time. */
	TIMING("Change Timing"),
	/** Show the selected element's current simulated value. */
	VIEW_VALUE("View Contents"),
	/** Cut the selection to the clipboard. */
	CUT("Cut"),
	/** Copy the selection to the clipboard. */
	COPY("Copy"),
	/** Paste the clipboard contents. */
	PASTE("Paste"),
	/** Delete the selection. */
	DELETE("Delete"),
	/** Make the selection uneditable. */
	LOCK("Lock"),
	/** Select every element in the circuit. */
	SELECT_ALL("Select All"),
	/** Undo the latest change. */
	UNDO("Undo"),
	/** Redo the latest undone change. */
	REDO("Redo"),
	/** Rotate the selected element clockwise. */
	ROTATE_CW("Rotate Clockwise"),
	/** Rotate the selected element counter-clockwise. */
	ROTATE_CCW("Rotate Counter-Clockwise"),
	/** Flip the selected element. */
	FLIP("Flip"),
	/** Start drawing a wire. */
	WIRE_START("Wire(s)"),
	/** Close the current circuit. */
	CLOSE("Close");

	/** The human-readable label the operation's menu items display. */
	private final String label;

	/**
	 * Create an edit operation with its menu label.
	 *
	 * @param label the human-readable label for menu items.
	 */
	EditOp(String label) {

		this.label = label;
	} // end of EditOp constructor

	/**
	 * The human-readable label the operation's menu items display. The
	 * popup Watch and Probe items override it with a state-dependent
	 * caption (Watch/Un-watch, Attach/Remove Probe), but the menu-bar
	 * items and every other surface use this stable label.
	 *
	 * @return the menu label.
	 */
	public String label() {

		return label;
	} // end of label method

	/**
	 * The accelerator keystroke this operation's menu items and canvas
	 * key binding use (issue #75). This is the single source of truth for
	 * the operation's shortcut, shared by the editor (which tags its
	 * shared {@link javax.swing.Action} with it) and the main window's
	 * Edit and Element menus (which show and window-scope it), so the
	 * surfaces never drift. The mask is the platform menu modifier (Cmd
	 * on macOS, Ctrl elsewhere), derived headless-safely from the given
	 * {@code os.name} rather than from {@code Toolkit}.
	 *
	 * @param osName value of the os.name system property (null-safe).
	 * @return the accelerator keystroke; never null.
	 */
	public KeyStroke accelerator(String osName) {

		int mask = MenuAcceleratorPolicy.menuMask(osName);
		switch (this) {
			case PROBE:
				return KeyStroke.getKeyStroke(KeyEvent.VK_P, mask);
			case WATCH:
				return KeyStroke.getKeyStroke(KeyEvent.VK_W, mask);
			case MODIFY:
				return KeyStroke.getKeyStroke(KeyEvent.VK_M, mask);
			case TIMING:
				return KeyStroke.getKeyStroke(KeyEvent.VK_T, mask);
			case VIEW_VALUE:
				return MenuAcceleratorPolicy.viewValueStroke();
			case CUT:
				return KeyStroke.getKeyStroke(KeyEvent.VK_X, mask);
			case COPY:
				return KeyStroke.getKeyStroke(KeyEvent.VK_C, mask);
			case PASTE:
				return KeyStroke.getKeyStroke(KeyEvent.VK_V, mask);
			case DELETE:
				return DeleteKeyPolicy.menuAccelerator(osName);
			case LOCK:
				return KeyStroke.getKeyStroke(KeyEvent.VK_L, mask);
			case SELECT_ALL:
				return KeyStroke.getKeyStroke(KeyEvent.VK_A, mask);
			case UNDO:
				return KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask);
			case REDO:
				return MenuAcceleratorPolicy.redoDisplayed(osName);
			case ROTATE_CW:
				return MenuAcceleratorPolicy.rotateCwStroke();
			case ROTATE_CCW:
				return MenuAcceleratorPolicy.rotateCcwStroke();
			case FLIP:
				return MenuAcceleratorPolicy.flipStroke();
			case WIRE_START:
				return MenuAcceleratorPolicy.wireStartStroke();
			case CLOSE:
				return KeyStroke.getKeyStroke(KeyEvent.VK_E, mask);
			default:
				throw new AssertionError("no accelerator for " + this);
		}
	} // end of accelerator method

} // end of EditOp enum
