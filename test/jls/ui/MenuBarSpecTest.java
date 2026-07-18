package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.DefaultExceptionHandler;
import jls.JLSInfo;
import jls.JLSStart;
import jls.sim.Simulator;

/**
 * Menu-bar specification test (issue #91, acceptance criterion P3):
 * boots the real {@link JLSStart} main window, renders its entire menu
 * bar into a canonical one-line-per-item text form - nesting by tabs,
 * with {@code [disabled]} and {@code [accelerator]} markers - and
 * compares it against a declared expectation table. Any drift in menu
 * items, ordering, submenu structure, startup enabled state, or
 * accelerator bindings (F5 run / F7 stop, pinning part of #75's
 * accelerator work) fails with a readable tree diff.
 *
 * The renderer itself is pinned by assert-the-assertion tests over a
 * synthetic menu bar (per the {@link jls.ui} package discipline), so a
 * walker that ignored enabled state, accelerators, separators, or
 * nesting could not silently pass.
 *
 * Tagged {@code display}: {@code new JLSStart()} is a visible
 * {@code JFrame}, so this runs under the #162 substrate
 * ({@code xvfb-run -a mvn -B verify -Djls.test.headless=false}) and
 * self-skips headless. The renderer tests need no frame but live here
 * because building even unattached Swing menus asks for a toolkit.
 */
@Tag("display")
@Timeout(60)
class MenuBarSpecTest {

	/**
	 * The declared expectation table for the whole menu bar: every
	 * item in order, one line per item, submenu items indented by one
	 * tab, {@code [disabled]} after the text for items disabled at
	 * startup (currently none - all items are always enabled and
	 * no-op without a selected editor), and the accelerator in
	 * brackets last. Update this table deliberately when the menus
	 * change; it is the reviewable record of the menu surface.
	 */
	private static final String EXPECTED_MENU_TREE = """
			File
			\tNew
			\tOpen
			\tSave
			\tSave As
			\tPrint...
			\t\tEntire circuit
			\t\tJust visible window
			\tImport
			\tExport Image
			\tClose
			\tExit
			Simulator
			\tShow Simulator Window
			\tHide Simulator Window
			\tRun (in background) [F5]
			\tStop (background simulator) [F7]
			Global
			\tReset all propagation delays
			\tRemove all probes
			\tUnwatch all elements
			\tExpand circuit drawing area by 10%
			\tChange editor window grid color
			\tChange editor window background color
			Help
			\tAbout
			\tTutorial
			\t\tIntroduction
			\t\t4-Bit Counter
			\t\tFull Adder
			\t\tSign Extension
			\tContents
			""";

	/**
	 * Skip the whole class in headless runs; the display-tagged
	 * surefire execution supplies a real or virtual display.
	 */
	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * P3: the real main-window menu bar matches the declared table -
	 * items, order, nesting, enabled state, and accelerators.
	 *
	 * @throws Exception on reflection or EDT failure.
	 */
	@Test
	void menuBarMatchesTheDeclaredExpectationTable() throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			AtomicReference<String> rendered = new AtomicReference<>();
			AtomicReference<Boolean> glueBeforeHelp = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				JMenuBar bar = jls.getJMenuBar();
				rendered.set(render(bar));
				// the component after Global (index 3) is the glue
				// that right-aligns Help, not a menu
				glueBeforeHelp.set(
						!(bar.getComponent(3) instanceof JMenu));
			});
			assertEquals(EXPECTED_MENU_TREE, rendered.get(),
					"menu bar drifted from the declared table");
			assertEquals(Boolean.TRUE, glueBeforeHelp.get(),
					"Help must be right-aligned by glue after Global");
		}
		finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window w : Window.getWindows()) {
					w.dispose();
				}
			});
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}

	/**
	 * Assert-the-assertion: the renderer reports text, nesting,
	 * disabled state, accelerators, and separators - a walker that
	 * dropped any of those channels would fail here on a synthetic
	 * bar built to exercise each one.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void rendererReportsEveryChannelOfTheSpec() throws Exception {
		AtomicReference<String> rendered = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			JMenuBar bar = new JMenuBar();
			JMenu top = new JMenu("Top");
			JMenuItem plain = new JMenuItem("Plain");
			top.add(plain);
			JMenuItem off = new JMenuItem("Off");
			off.setEnabled(false);
			top.add(off);
			top.addSeparator();
			JMenuItem keyed = new JMenuItem("Keyed");
			keyed.setAccelerator(KeyStroke.getKeyStroke("control S"));
			top.add(keyed);
			JMenu sub = new JMenu("Sub");
			sub.add(new JMenuItem("Inner"));
			top.add(sub);
			bar.add(top);
			rendered.set(render(bar));
		});
		// the modifier's display text is the platform's (usually
		// "Ctrl"); the structure around it is what this test pins
		String ctrl = InputEvent.getModifiersExText(
				InputEvent.CTRL_DOWN_MASK);
		assertEquals("Top\n"
				+ "\tPlain\n"
				+ "\tOff [disabled]\n"
				+ "\t--\n"
				+ "\tKeyed [" + ctrl + "+S]\n"
				+ "\tSub\n"
				+ "\t\tInner\n", rendered.get(),
				"renderer must report every spec channel");
	}

	/**
	 * Assert-the-assertion: two bars differing only in an
	 * accelerator render differently, so the table comparison cannot
	 * pass on a bar whose bindings drifted.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void acceleratorDriftChangesTheRendering() throws Exception {
		AtomicReference<String> withF5 = new AtomicReference<>();
		AtomicReference<String> withF6 = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			withF5.set(render(oneItemBar("F5")));
			withF6.set(render(oneItemBar("F6")));
		});
		assertNotEquals(withF5.get(), withF6.get(),
				"an accelerator change must change the rendering");
	}

	/**
	 * Build a one-menu, one-item bar whose item is bound to the given
	 * key, for the drift-detection test.
	 *
	 * @param key A {@link KeyStroke#getKeyStroke(String)} spec.
	 * @return the menu bar.
	 */
	private static JMenuBar oneItemBar(String key) {
		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu("Menu");
		JMenuItem item = new JMenuItem("Item");
		item.setAccelerator(KeyStroke.getKeyStroke(key));
		menu.add(item);
		bar.add(menu);
		return bar;
	}

	/**
	 * Construct the real JLS main window on the EDT. {@code JLSStart}
	 * normally comes up through {@code start()}, which installs the
	 * static exception handler first; the harness supplies one by
	 * reflection so direct construction does not NPE (a harness gap,
	 * not an app bug - the app cannot reach the constructor without
	 * it). The caller restores {@code JLSInfo.frame}/{@code .sim} and
	 * disposes all windows.
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

	/**
	 * Render a menu bar to the canonical text form: one line per
	 * item in traversal order, nesting shown by leading tabs,
	 * {@code [disabled]} after the text of a disabled item, {@code --}
	 * for a separator, and the accelerator (modifiers+key) in
	 * brackets last. Call on the EDT.
	 *
	 * @param bar The menu bar to render.
	 * @return the canonical rendering.
	 */
	private static String render(JMenuBar bar) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bar.getMenuCount(); i++) {
			JMenu menu = bar.getMenu(i);
			if (menu != null) {	// glue and struts return null
				renderItem(menu, 0, sb);
			}
		}
		return sb.toString();
	}

	/**
	 * Render one item (and, for menus, its children) into the
	 * canonical form.
	 *
	 * @param item The item to render.
	 * @param depth Nesting depth (0 for top-level menus).
	 * @param sb The rendering being built.
	 */
	private static void renderItem(JMenuItem item, int depth,
			StringBuilder sb) {
		sb.append("\t".repeat(depth)).append(item.getText());
		if (!item.isEnabled()) {
			sb.append(" [disabled]");
		}
		KeyStroke key = item.getAccelerator();
		if (key != null) {
			sb.append(" [").append(describe(key)).append(']');
		}
		sb.append('\n');
		if (item instanceof JMenu menu) {
			for (int i = 0; i < menu.getItemCount(); i++) {
				JMenuItem child = menu.getItem(i);
				if (child == null) {	// separator
					sb.append("\t".repeat(depth + 1)).append("--\n");
				}
				else {
					renderItem(child, depth + 1, sb);
				}
			}
		}
	}

	/**
	 * A stable, human-readable form of an accelerator:
	 * {@code Ctrl+S}, {@code F5}.
	 *
	 * @param key The accelerator.
	 * @return the readable form.
	 */
	private static String describe(KeyStroke key) {
		String text = KeyEvent.getKeyText(key.getKeyCode());
		int modifiers = key.getModifiers();
		if (modifiers == 0) {
			return text;
		}
		return InputEvent.getModifiersExText(modifiers) + "+" + text;
	}

	/**
	 * Sanity guard for the table itself: the expectation is a
	 * constant, so a stray edit that blanks it would make the P3 test
	 * vacuous; pin that it still declares all four menus.
	 */
	@Test
	void expectationTableStillDeclaresAllFourMenus() {
		for (String menu : new String[] { "File\n", "Simulator\n",
				"Global\n", "Help\n" }) {
			assertTrue(EXPECTED_MENU_TREE.contains(menu),
					"expectation table lost top-level menu " + menu.trim());
		}
	}
}
