package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jls.KeyPad;

/**
 * Every button in the KeyPad exposes a non-blank accessible name
 * (issue #75 accessibility). The keypad-toggle button (the KeyPad
 * itself) and the hide button are icon-only (down.gif / up.gif), so
 * without an explicit accessible name a screen reader focusing them
 * would announce nothing — they were the last blank-accessible-name
 * components in src/. The reset button carried only the bare text "C",
 * which announces as the letter rather than what it does.
 *
 * This test reads {@code getAccessibleContext().getAccessibleName()}
 * on the real buttons inside the real keypad JDialog — the same
 * accessibility channel an AT queries — modeled on
 * {@link PaletteButtonAccessibilityTest}, with the KeyPad construction
 * and window-reflection plumbing of {@link KeyPadDismissalTest}.
 *
 * Tagged {@code display}: constructs real Swing components, so it runs
 * under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false) and skips
 * headless. The headless counterpart is
 * {@code jls.KeyPadAccessibilityPinTest}.
 */
@Tag("display")
class KeyPadAccessibilityTest {

	private JFrame owner;
	private JTextField field;
	private KeyPad pad;

	@BeforeEach
	void requireDisplay() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
		SwingUtilities.invokeAndWait(() -> {
			owner = new JFrame("keypad-a11y-test");
			field = new JTextField(10);
			pad = new KeyPad(field, 16, 0, owner);
			owner.getContentPane().add(field);
			owner.getContentPane().add(pad);
			owner.pack();
		});
	}

	@AfterEach
	void tearDown() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (pad != null) {
				pad.close();
			}
			if (owner != null) {
				owner.dispose();
			}
		});
	}

	/** The keypad's popup window, reached through its private field. */
	private JDialog padWindow() throws Exception {
		Field win = KeyPad.class.getDeclaredField("win");
		win.setAccessible(true);
		return (JDialog) win.get(pad);
	}

	/**
	 * Every AbstractButton in the keypad window announces a non-blank
	 * accessible name — the digit buttons derive theirs from their
	 * text, and the icon-only hide button and the reset button carry
	 * the explicit names set in the constructor.
	 */
	@Test
	void everyKeypadButtonHasAnAccessibleName() throws Exception {
		JDialog win = padWindow();
		SwingUtilities.invokeAndWait(() -> {
			int buttons = 0;
			for (Component c : win.getContentPane().getComponents()) {
				if (!(c instanceof AbstractButton button)) {
					continue;
				}
				buttons += 1;
				String name = button.getAccessibleContext()
						.getAccessibleName();
				assertFalse(name == null || name.isBlank(),
						"keypad button (name '" + button.getName()
								+ "', text '" + button.getText()
								+ "') exposes a non-blank accessible"
								+ " name, got '" + name + "'");
			}
			assertEquals(18, buttons,
					"the base-16 keypad has 16 digits + reset + hide");
		});
	}

	/**
	 * The icon-only toggle and hide buttons and the bare-"C" reset
	 * button announce what they do, both to a screen reader (accessible
	 * name) and to a sighted user hovering them (tooltip), and carry
	 * stable component names for the #91 harness.
	 */
	@Test
	void iconOnlyButtonsAnnounceTheirPurpose() throws Exception {
		JDialog win = padWindow();
		SwingUtilities.invokeAndWait(() -> {
			// the toggle button is the KeyPad itself
			assertEquals("Show keypad",
					pad.getAccessibleContext().getAccessibleName(),
					"the icon-only keypad-toggle button announces"
							+ " itself");
			assertEquals("Show keypad", pad.getToolTipText(),
					"the toggle button has a matching tooltip");
			assertEquals("keypad.toggle", pad.getName(),
					"the toggle button carries its harness name");

			AbstractButton hide = byName(win, "keypad.hide");
			assertEquals("Hide keypad",
					hide.getAccessibleContext().getAccessibleName(),
					"the icon-only hide button announces itself");
			assertEquals("Hide keypad", hide.getToolTipText(),
					"the hide button has a matching tooltip");

			AbstractButton reset = byName(win, "keypad.reset");
			assertEquals("Clear to default value",
					reset.getAccessibleContext().getAccessibleName(),
					"the reset button announces its action, not the"
							+ " bare letter C");
			assertEquals("Clear to default value",
					reset.getToolTipText(),
					"the reset button has a matching tooltip");
		});
	}

	/** Find the button with the given component name in the window. */
	private static AbstractButton byName(JDialog win, String name) {
		for (Component c : win.getContentPane().getComponents()) {
			if (c instanceof AbstractButton button
					&& name.equals(button.getName())) {
				return button;
			}
		}
		throw new AssertionError("no keypad button named " + name);
	}

} // end of KeyPadAccessibilityTest class
