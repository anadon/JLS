package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
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
 * Pins the KeyPad's standard dismissal semantics (issue #86 H1, landed
 * with the #103/#104 rebuild): Escape hides the keypad without
 * touching the entered text (P1), focus loss hides it (the
 * click-outside path - the listener is exercised at event level, since
 * xvfb has no window manager to deliver real focus transitions), and
 * the old hidden right-click-on-digit gesture is gone - no JLS mouse
 * listener remains on the digit or reset buttons (P3).
 *
 * Same display tagging and headless discipline as
 * {@link DialogConstructionSmokeTest}.
 */
@Tag("display")
class KeyPadDismissalTest {

	private JFrame owner;
	private JTextField field;
	private KeyPad pad;

	@BeforeEach
	void requireDisplay() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
		SwingUtilities.invokeAndWait(() -> {
			owner = new JFrame("keypad-test");
			field = new JTextField(10);
			pad = new KeyPad(field, 10, 0, owner);
			owner.getContentPane().add(field);
			owner.getContentPane().add(pad);
			owner.pack();
			owner.setVisible(true);
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

	private void openPad() throws Exception {
		SwingUtilities.invokeAndWait(() -> pad.doClick());
	}

	private boolean padVisible() throws Exception {
		JDialog win = padWindow();
		AtomicReference<Boolean> visible = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> visible.set(win.isVisible()));
		return visible.get();
	}

	/**
	 * Poll the keypad's visibility until it reaches {@code want} or a
	 * ~5s bound elapses, then return the final state. Showing and
	 * dismissing the dialog realize asynchronously, so a single read
	 * races the window manager on a loaded CI runner (observed on #196);
	 * polling makes the open/dismiss assertions deterministic.
	 *
	 * @param want The visibility being waited for.
	 * @return the keypad's visibility after waiting.
	 */
	private boolean waitPadVisible(boolean want) throws Exception {
		for (int i = 0; i < 200; i++) {
			if (padVisible() == want) {
				return want;
			}
			Thread.sleep(25);
		}
		return padVisible();
	}

	@Test
	void escapeDismissesWithoutTouchingTheValue() throws Exception {
		openPad();
		assertTrue(waitPadVisible(true), "keypad should open on button click");
		JDialog win = padWindow();

		// enter a digit, then Escape
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : new Window[] { win }) {
				assertNotNull(w);
			}
			// digit "3" is a child button labeled 3
			clickDigit(win, "3");
		});
		assertEquals("3", field.getText(),
				"digit buttons must keep filling the field");
		SwingUtilities.invokeAndWait(() -> win.getRootPane()
				.dispatchEvent(new KeyEvent(win.getRootPane(),
						KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
						0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED)));
		assertFalse(waitPadVisible(false), "Escape must dismiss the keypad");
		assertEquals("3", field.getText(),
				"Escape must not change the entered text");
	}

	@Test
	void focusLossDismisses() throws Exception {
		openPad();
		assertTrue(waitPadVisible(true), "keypad should open on button click");
		JDialog win = padWindow();
		SwingUtilities.invokeAndWait(() -> win.dispatchEvent(
				new WindowEvent(win, WindowEvent.WINDOW_LOST_FOCUS)));
		assertFalse(waitPadVisible(false),
				"losing focus (clicking outside) must dismiss the keypad");
	}

	@Test
	void noHiddenRightClickDismissalRemains() throws Exception {
		// P3: the undiscoverable right-click-on-digit exit is gone -
		// no JLS-defined mouse listener on any keypad button (only
		// Swing's own plaf listeners remain)
		JDialog win = padWindow();
		SwingUtilities.invokeAndWait(() -> {
			for (java.awt.Component c : win.getContentPane()
					.getComponents()) {
				if (!(c instanceof JButton)) {
					continue;
				}
				for (MouseListener l : ((JButton) c)
						.getMouseListeners()) {
					assertFalse(l.getClass().getName()
							.startsWith("jls."),
							"keypad button still carries a JLS mouse"
									+ " listener: " + l.getClass());
				}
			}
		});
	}

	/** Click the digit button with the given label inside the window. */
	private static void clickDigit(JDialog win, String label) {
		for (java.awt.Component c : win.getContentPane()
				.getComponents()) {
			if (c instanceof JButton
					&& label.equals(((JButton) c).getText())) {
				((JButton) c).doClick();
				return;
			}
		}
		throw new AssertionError("no digit button labeled " + label);
	}

} // end of KeyPadDismissalTest class
