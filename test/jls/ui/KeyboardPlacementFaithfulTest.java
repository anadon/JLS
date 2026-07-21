package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Element;
import jls.elem.OrGate;

/**
 * Enter places (commits) a keyboard-positioned element, verified through the
 * real focus subsystem (issue #75 keyboard construction, behavior "Enter
 * places the chosen element"). {@link FocusFaithfulKeyboardTest} proves an
 * arrow routed through the live focus owner moves the placing gate; this
 * proves the follow-on Enter, routed the same way, commits it: after Enter
 * the editor is idle, so a subsequent arrow moves only the caret and the
 * gate stays put. Driving every key through
 * {@link EditorGestureSupport#pressKeyThroughFocusOwner} makes the test red
 * if key delivery to the focus owner breaks, where the old
 * {@code canvas.dispatchEvent} path stayed green.
 *
 * <p>Tagged {@code display}: needs a display for the gate creation dialog
 * (accepted by a background thread with its defaults) and font metrics.</p>
 */
@Tag("display")
@Timeout(120)
class KeyboardPlacementFaithfulTest {

	private volatile boolean accepting;
	private Thread acceptor;

	@BeforeEach
	void requireDisplayAndStartAcceptor() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
		accepting = true;
		acceptor = new Thread(() -> {
			while (accepting) {
				SwingUtilities.invokeLater(() -> {
					for (Window w : Window.getWindows()) {
						if (w.isVisible() && w instanceof JDialog) {
							JButton ok = findButton(w, "OK");
							if (ok != null) {
								ok.doClick();
							}
						}
					}
				});
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					return;
				}
			}
		}, "gate-dialog-acceptor");
		acceptor.setDaemon(true);
		acceptor.start();
	}

	@AfterEach
	void stopAcceptor() throws Exception {
		accepting = false;
		if (acceptor != null) {
			acceptor.join(1000);
		}
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	/**
	 * Choose an OR gate from the palette (which hands focus to the canvas),
	 * move it one grid step with a focus-owner-routed arrow, commit it with a
	 * focus-owner-routed Enter, then prove the commit took: a further arrow
	 * leaves the gate where it was placed (the editor is idle, so the arrow
	 * moves only the caret).
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void enterThroughFocusOwnerCommitsThePlacedGate() throws Exception {
		Circuit circuit = new Circuit("kbd-place");
		int step = JLSInfo.spacing;
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			JButton orButton = findToolbarButton(ui.editor, "OR gate");
			assertNotNull(orButton, "toolbar has an \"OR gate\" button");
			SwingUtilities.invokeAndWait(orButton::doClick);
			ui.waitFor(() -> present(circuit, OrGate.class), "OR gate created");
			ui.waitFor(ui::canvasIsFocusOwner,
					"choosing from the palette handed focus to the canvas");

			OrGate or = assertElementPresent(circuit, OrGate.class);
			int beforeX = or.getX();
			// arrow through the focus owner moves the placing gate one step
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			ui.waitFor(() -> or.getX() == beforeX + step,
					"an arrow moved the placing gate one grid step");
			int placedX = or.getX();

			// Enter through the focus owner commits the placement
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);

			// after the commit the editor is idle: a further arrow moves only
			// the caret, so the committed gate stays where it was placed. Wait
			// on a POSITIVE observable that the arrow was processed - the caret
			// moving - rather than sleeping a fixed interval; then assert the
			// gate did not move. (Were the editor still in the placing state -
			// the regression this guards - the same arrow would move the gate,
			// failing the assertion.)
			java.awt.Point caretBefore = ui.keyboardCaret();
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			ui.waitFor(() -> {
				try {
					return !java.util.Objects.equals(ui.keyboardCaret(),
							caretBefore);
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}, "the post-commit arrow moved the caret (so it was processed)");
			assertEquals(placedX, sole(ui).getX(),
					"Enter committed the placement: the gate no longer follows "
							+ "the arrow keys");
		}
	}

	private static OrGate sole(EditorGestureSupport ui) {
		for (Element el : ui.currentCircuit().getElements()) {
			if (el instanceof OrGate g) {
				return g;
			}
		}
		throw new AssertionError("no OR gate in the circuit");
	}

	private static boolean present(Circuit circuit, Class<?> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return true;
			}
		}
		return false;
	}

	private static JButton findToolbarButton(Container root, String tooltip) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button
					&& tooltip.equals(button.getToolTipText())) {
				return button;
			}
			if (c instanceof Container inner) {
				JButton found = findToolbarButton(inner, tooltip);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static JButton findButton(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button && text.equals(button.getText())) {
				return button;
			}
			if (c instanceof Container inner) {
				JButton found = findButton(inner, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
