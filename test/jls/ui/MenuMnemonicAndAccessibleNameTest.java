package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.DefaultExceptionHandler;
import jls.JLSInfo;
import jls.JLSStart;
import jls.MenuAcceleratorPolicy;
import jls.sim.Simulator;

/**
 * Accessibility properties an assistive technology actually reads, checked
 * on the REAL booted menu bar (issue #75 verification hardening). The prior
 * coverage was a proxy: {@link jls.MenuAcceleratorPolicyTest} pins the
 * policy's mnemonic <em>values</em> as a pure function and greps
 * {@code JLSStart} source for {@code setMnemonic(...)} on a subset of menus;
 * no test read {@code menu.getMnemonic()} or
 * {@code getAccessibleContext().getAccessibleName()} on the real components a
 * screen reader consumes. A regression that deleted a {@code setMnemonic}
 * call (Edit/Element/View were never grepped) would have passed.
 *
 * <p>This boots {@link JLSStart} and reads the accessibility channel
 * directly: every top-level menu exposes its mnemonic, the mnemonics are all
 * distinct (so Alt+letter is unambiguous), and every menu item (and submenu)
 * exposes a non-blank accessible name equal to its visible label - the name
 * a screen reader announces.</p>
 *
 * <p>Tagged {@code display}: {@code new JLSStart()} is a visible frame, so
 * it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(60)
class MenuMnemonicAndAccessibleNameTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * Every top-level menu on the real bar carries the mnemonic the policy
	 * declares (View outside the policy uses the literal V), and all seven
	 * are distinct so Alt+letter opens an unambiguous menu.
	 *
	 * @throws Exception on reflection or EDT failure.
	 */
	@Test
	void everyTopLevelMenuExposesADistinctMnemonic() throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			// menu label -> expected mnemonic key code
			Map<String, Integer> expected = new LinkedHashMap<>();
			expected.put("File", MenuAcceleratorPolicy.fileMenuMnemonic());
			expected.put("Edit", MenuAcceleratorPolicy.editMenuMnemonic());
			expected.put("Element",
					MenuAcceleratorPolicy.elementMenuMnemonic());
			expected.put("Simulator",
					MenuAcceleratorPolicy.simulatorMenuMnemonic());
			expected.put("View", KeyEvent.VK_V);
			expected.put("Global", MenuAcceleratorPolicy.globalMenuMnemonic());
			expected.put("Help", MenuAcceleratorPolicy.helpMenuMnemonic());

			AtomicReference<Map<String, Integer>> actualRef =
					new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				Map<String, Integer> actual = new LinkedHashMap<>();
				JMenuBar bar = jls.getJMenuBar();
				for (int i = 0; i < bar.getMenuCount(); i++) {
					JMenu menu = bar.getMenu(i);
					if (menu != null) {
						actual.put(menu.getText(),
								Integer.valueOf(menu.getMnemonic()));
					}
				}
				actualRef.set(actual);
			});
			Map<String, Integer> actual = actualRef.get();

			for (Map.Entry<String, Integer> e : expected.entrySet()) {
				Integer got = actual.get(e.getKey());
				assertNotNull(got, "menu bar has a " + e.getKey() + " menu");
				assertEquals(e.getValue().intValue(), got.intValue(),
						e.getKey() + " menu mnemonic (a deleted setMnemonic "
								+ "would read 0)");
				assertFalse(got.intValue() == 0,
						e.getKey() + " menu has a non-zero mnemonic");
			}
			// distinct across the seven menus
			assertEquals(expected.size(),
					(int) expected.values().stream().distinct().count(),
					"the top-level mnemonics must be distinct for Alt+letter");
		}
		finally {
			disposeAll();
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}

	/**
	 * Every menu item and submenu on the real bar exposes a non-blank
	 * accessible name equal to its visible label - the name a screen reader
	 * announces.
	 *
	 * @throws Exception on reflection or EDT failure.
	 */
	@Test
	void everyMenuItemExposesItsLabelAsAccessibleName() throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			AtomicReference<String> failure = new AtomicReference<>();
			AtomicReference<Integer> checked = new AtomicReference<>(0);
			SwingUtilities.invokeAndWait(() -> {
				JMenuBar bar = jls.getJMenuBar();
				for (int i = 0; i < bar.getMenuCount(); i++) {
					JMenu menu = bar.getMenu(i);
					if (menu != null) {
						checkAccessibleNames(menu, failure, checked);
					}
				}
			});
			assertTrue(checked.get().intValue() > 20,
					"walked the whole menu tree (checked " + checked.get()
							+ " items)");
			assertNull(failure.get(), failure.get());
		}
		finally {
			disposeAll();
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}

	/**
	 * Assert this item's accessible name equals its non-blank label, then
	 * recurse into a submenu's children. Records the first failure.
	 */
	private static void checkAccessibleNames(JMenuItem item,
			AtomicReference<String> failure, AtomicReference<Integer> checked) {
		String text = item.getText();
		String name = item.getAccessibleContext().getAccessibleName();
		checked.set(Integer.valueOf(checked.get().intValue() + 1));
		if (text == null || text.isBlank()) {
			if (failure.get() == null) {
				failure.set("a menu item has a blank label");
			}
			return;
		}
		if (!text.equals(name)) {
			if (failure.get() == null) {
				failure.set("menu item '" + text + "' accessible name was '"
						+ name + "', not its label");
			}
		}
		if (item instanceof JMenu menu) {
			for (int j = 0; j < menu.getItemCount(); j++) {
				JMenuItem child = menu.getItem(j);
				if (child != null) { // null == separator
					checkAccessibleNames(child, failure, checked);
				}
			}
		}
	}

	private static void disposeAll() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	/**
	 * Construct the real JLS main window on the EDT (mirrors
	 * {@link MenuBarSpecTest}).
	 *
	 * @return the constructed main window.
	 * @throws Exception on reflection or EDT failure.
	 */
	private static JLSStart bootMainWindow() throws Exception {
		Field handler = JLSStart.class.getDeclaredField("exHandler");
		handler.setAccessible(true);
		if (handler.get(null) == null) {
			handler.set(null, new DefaultExceptionHandler());
		}
		AtomicReference<JLSStart> ref = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> ref.set(new JLSStart()));
		assertNotNull(ref.get().getJMenuBar(), "main window has a menu bar");
		return ref.get();
	}
}
