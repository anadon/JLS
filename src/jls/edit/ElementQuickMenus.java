package jls.edit;

import java.math.BigInteger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jls.elem.Constant;
import jls.elem.Element;

/**
 * GUI-side "quick change" shortcut menus (issue #77). The former
 * {@code Element.setupQuickMenu}/{@code Constant.setupQuickMenu} built
 * Swing menus inside the model; that Swing now lives here, and the editor
 * asks this builder for an element's quick menu. Only {@link Constant}
 * has one today (a value shortcut submenu).
 */
public final class ElementQuickMenus {

	/**
	 * Creates {@code ElementQuickMenus}.
	 */
	private ElementQuickMenus() {
	} // no instances

	/**
	 * Build the quick-change menu for an element, mirroring the element's
	 * former {@code setupQuickMenu}. Only called for elements whose
	 * {@code quickChange()} is true.
	 *
	 * @param el The element the menu is for.
	 * @param sed The editor to reset and repaint after a quick change.
	 * @return the menu item (a submenu) to add to the popup.
	 * @throws UnsupportedOperationException if the element has no quick menu.
	 */
	public static JMenuItem build(Element el, SimpleEditor sed) {

		if (el instanceof Constant c) {
			return constantMenu(c, sed);
		}
		throw new UnsupportedOperationException("setupQuickMenu");
	} // end of build method

	/**
	 * The constant's value-shortcut submenu: set to 0 or 1, add 1, or
	 * subtract 1.
	 *
	 * @param c The constant the menu is for.
	 * @param sed The editor to reset and repaint after a quick change.
	 * @return the constant's value-shortcut submenu.
	 */
	private static JMenu constantMenu(Constant c, SimpleEditor sed) {

		JMenu quick = new JMenu("shortcuts");
		JMenuItem zero = new JMenuItem("0");
		JMenuItem one = new JMenuItem("1");
		JMenuItem plus = new JMenuItem("add 1");
		JMenuItem minus = new JMenuItem("subtract 1");
		quick.add(zero);
		quick.add(one);
		quick.add(plus);
		quick.add(minus);
		zero.addActionListener(e -> apply(c, sed, () -> c.setValue(BigInteger.ZERO)));
		one.addActionListener(e -> apply(c, sed, () -> c.setValue(BigInteger.ONE)));
		plus.addActionListener(e -> apply(c, sed, c::incrementValue));
		minus.addActionListener(e -> apply(c, sed, c::decrementValue));
		return quick;
	} // end of constantMenu method

	/**
	 * Apply a value change and, if the constant is in a live editor,
	 * re-size (quickReset) and repaint — the former actionPerformed tail.
	 *
	 * @param c The constant whose value changed.
	 * @param sed The editor to reset after the change.
	 * @param change The value change to apply.
	 */
	private static void apply(Constant c, SimpleEditor sed, Runnable change) {

		change.run();
		Editor ed = Editors.of(c.getCircuit());
		if (ed != null) {
			sed.quickReset();
			ed.repaint();
		}
	} // end of apply method

} // end of ElementQuickMenus class
