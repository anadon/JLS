package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.elem.Adder;

/**
 * Palette buttons, their mirror menu items, and element-dialog form
 * fields are resolvable by stable component identity (issue #210): every
 * palette button is named {@code palette.<slug>}, every mirror item in
 * the popup "elements" menu is named {@code menu.elements.<slug>}, and
 * element-dialog inputs carry {@code dialog.<slug>.<field>} names with a
 * {@code JLabel.setLabelFor} association. The scheme is documented in
 * {@code docs/component-naming.md}; the slugs come from the
 * {@link jls.elem.ElementRegistry} tags.
 *
 * <p>Before the fix, all 30 palette buttons returned {@code null} from
 * {@code getName()} (issue #210 P1: {@code palette icon buttons=30 with
 * getName()!=null=0}), so name-based lookup found nothing and automation
 * fell back to tooltip/class/positional heuristics.</p>
 *
 * <p>Tagged {@code display}: boots a real editor tool bar and opens a
 * real element creation dialog, so it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(120)
class ComponentIdentityTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	@AfterEach
	void disposeStrayWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	/**
	 * Every palette button carries a distinct {@code palette.<slug>} name,
	 * and a representative lookup by name (no tooltip, class, or index
	 * heuristics) resolves the adder button.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void everyPaletteButtonIsResolvableByStableName() throws Exception {
		try (EditorGestureSupport ui =
				new EditorGestureSupport(new Circuit("identity"))) {
			List<JButton> buttons = new ArrayList<>();
			collectPaletteButtons(ui.editor, buttons);

			Set<String> names = new HashSet<>();
			for (JButton button : buttons) {
				String name = button.getName();
				if (name != null && name.startsWith("palette.")) {
					assertTrue(names.add(name),
							"palette button name '" + name + "' is unique");
				} else {
					// the only tooltip-bearing tool-bar button outside the
					// palette scheme is the Import menu control
					assertEquals("Import", button.getText(),
							"tool-bar button (tooltip '"
									+ button.getToolTipText()
									+ "') lacks a palette.<slug> name");
				}
			}
			// issue #210 P1/P2: 0 of 30 named before the fix, 30 after;
			// issue #201 added RegisterFile and FieldExtend, so the census
			// is now 32. This literal is a deliberate independent tripwire:
			// deriving it from Palette.entries() would make the check
			// tautological, and deriving it from ElementRegistry would be
			// wrong (the registry carries non-palette types like WireEnd).
			assertEquals(32, names.size(),
					"all 32 element palette buttons resolve by palette.<slug>"
							+ " name (P1 was 0 of 30)");

			// the #91-style lookup: resolve by name alone
			JButton adder = findByName(ui.editor, "palette.adder");
			assertNotNull(adder,
					"findByName(\"palette.adder\") resolves the adder button");
			assertEquals("adder", adder.getToolTipText(),
					"the button named palette.adder is the adder palette entry");
		}
	}

	/**
	 * The mirror items in the popup "elements" menu carry
	 * {@code menu.elements.<slug>} names, one per palette entry.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void mirrorElementsMenuItemsCarryStableNames() throws Exception {
		try (EditorGestureSupport ui =
				new EditorGestureSupport(new Circuit("identity-menu"))) {
			// the "elements" menu lives in the canvas popup; raise it
			ui.rightPress(300, 300);
			JMenuItem elementsEntry = ui.popupItem("elements");
			assertTrue(elementsEntry instanceof JMenu,
					"the popup's \"elements\" entry is a submenu");
			JMenu elements = (JMenu) elementsEntry;

			List<String> names = new ArrayList<>();
			SwingUtilities.invokeAndWait(() -> {
				for (int i = 0; i < elements.getItemCount(); i++) {
					JMenuItem item = elements.getItem(i);
					if (item != null) {
						names.add(item.getName());
					}
				}
			});
			assertEquals(32, names.size(),
					"the elements menu mirrors the 32 palette entries");
			Set<String> distinct = new HashSet<>();
			for (String name : names) {
				assertNotNull(name, "every mirror menu item has a name");
				assertTrue(name.startsWith("menu.elements."),
						"mirror item name '" + name
								+ "' follows the menu.elements.<slug> scheme");
				assertTrue(distinct.add(name),
						"mirror item name '" + name + "' is unique");
			}
			assertTrue(names.contains("menu.elements.adder"),
					"the adder mirror item is named menu.elements.adder");
		}
	}

	/**
	 * A representative element dialog (Create Adder) exposes its bits
	 * field by the stable name {@code dialog.adder.bits}, associates the
	 * "Input Bits" label with it via {@code labelFor}, and names its
	 * OK/Cancel buttons {@code dialog.ok}/{@code dialog.cancel}.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void adderDialogFieldIsNamedAndLabelAssociated() throws Exception {
		Circuit circuit = new Circuit("identity-dialog");
		BufferedImage img = new BufferedImage(400, 300,
				BufferedImage.TYPE_INT_RGB);

		AtomicBoolean fieldNamed = new AtomicBoolean();
		AtomicBoolean fieldLabelled = new AtomicBoolean();
		AtomicBoolean okNamed = new AtomicBoolean();
		AtomicBoolean cancelNamed = new AtomicBoolean();
		CountDownLatch probed = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(1);

		// prober: inspect the modal dialog while it is up, then close it
		// through the cancel button it just resolved by name
		Thread prober = new Thread(() -> {
			while (probed.getCount() > 0) {
				SwingUtilities.invokeLater(() -> {
					for (Window w : Window.getWindows()) {
						if (!(w instanceof JDialog) || !w.isVisible()) {
							continue;
						}
						Component field =
								findComponentByName(w, "dialog.adder.bits");
						if (field == null) {
							continue;
						}
						fieldNamed.set(field instanceof JTextField);
						fieldLabelled.set(hasLabelFor(w, field));
						Component ok = findComponentByName(w, "dialog.ok");
						okNamed.set(ok instanceof JButton);
						Component cancel =
								findComponentByName(w, "dialog.cancel");
						cancelNamed.set(cancel instanceof JButton);
						probed.countDown();
						if (cancel instanceof JButton cancelButton) {
							cancelButton.doClick();
						} else {
							w.dispose();
						}
					}
				});
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					return;
				}
			}
		}, "adder-dialog-prober");
		prober.setDaemon(true);
		prober.start();

		SwingUtilities.invokeLater(() -> {
			try {
				jls.edit.ElementDialogs.setup(new Adder(circuit),
						img.createGraphics(), new JPanel(), 100, 100);
			} finally {
				done.countDown();
			}
		});
		assertTrue(done.await(60, TimeUnit.SECONDS),
				"the Create Adder dialog came down");
		assertTrue(probed.await(5, TimeUnit.SECONDS),
				"the prober found a dialog exposing dialog.adder.bits");

		assertTrue(fieldNamed.get(),
				"dialog.adder.bits resolves to the bits text field");
		assertTrue(fieldLabelled.get(),
				"a JLabel in the dialog has labelFor pointing at the bits "
						+ "field (screen readers announce it)");
		assertTrue(okNamed.get(), "the OK button is named dialog.ok");
		assertTrue(cancelNamed.get(),
				"the Cancel button is named dialog.cancel");
	}

	/** Collect every tool-bar palette button (icon button with tooltip). */
	private static void collectPaletteButtons(Container root,
			List<JButton> out) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button
					&& button.getToolTipText() != null) {
				out.add(button);
			}
			if (c instanceof Container inner) {
				collectPaletteButtons(inner, out);
			}
		}
	}

	/** Resolve a button by component name, the #91 lookup idiom. */
	private static JButton findByName(Container root, String name) {
		Component c = findComponentByName(root, name);
		return c instanceof JButton button ? button : null;
	}

	/** Depth-first component-name lookup. */
	private static Component findComponentByName(Container root,
			String name) {
		for (Component c : root.getComponents()) {
			if (name.equals(c.getName())) {
				return c;
			}
			if (c instanceof Container inner) {
				Component found = findComponentByName(inner, name);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/** Whether some JLabel in the tree points at the field via labelFor. */
	private static boolean hasLabelFor(Container root, Component field) {
		for (Component c : root.getComponents()) {
			if (c instanceof JLabel label && label.getLabelFor() == field) {
				return true;
			}
			if (c instanceof Container inner
					&& hasLabelFor(inner, field)) {
				return true;
			}
		}
		return false;
	}
}
