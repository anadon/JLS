package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
 * Self-proof of the focus-faithful key driver (issue #75 verification
 * hardening). The rest of the keyboard suite drives keys with
 * {@link EditorGestureSupport#pressKey}, which calls
 * {@code canvas.dispatchEvent(keyEvent)} on a hardcoded canvas reference -
 * it force-feeds the canvas regardless of where the AWT focus subsystem
 * would actually deliver the key. That bypass is exactly why a real focus
 * bug (choosing a gate from the tool bar left focus on the button, so a
 * user's arrow/Enter never reached the canvas) passed the suite until an
 * adversarial review caught it.
 *
 * <p>This test drives keys instead through
 * {@link EditorGestureSupport#pressKeyThroughFocusOwner}, which reads the
 * live {@link java.awt.KeyboardFocusManager} focus owner at dispatch time -
 * the same authority the real event pipeline consults. It is built so it
 * goes RED when the real path is broken and PASSES only when it works, in
 * two legs:</p>
 *
 * <ul>
 * <li><b>Leg 1 (faithfulness guard).</b> With focus forced onto a
 * non-canvas component (a palette button), an arrow routed through the
 * focus owner reaches the button, so the canvas caret never moves. This
 * leg fails if the driver secretly force-feeds the canvas the way the old
 * {@code pressKey} does - it is the falsification of the bypass itself.</li>
 * <li><b>Leg 2 (the real path).</b> Choosing the gate from the palette (a
 * keyboard-equivalent {@code doClick}) must, via the #75 H2
 * {@code setup()} focus handoff, move focus to the canvas; an arrow then
 * routed through the focus owner moves the placing gate one grid step. This
 * leg depends on the handoff: remove the {@code requestFocusInWindow} in
 * {@code setup()} and focus stays on the button, the arrow goes to the
 * button, the gate never moves - RED. The old dispatch-to-canvas path would
 * pass leg 2 even with focus stranded, so it could never prove the
 * handoff.</li>
 * </ul>
 *
 * <p>Tagged {@code display}: needs a real display for the gate creation
 * dialog and font metrics, so it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false). A background
 * thread accepts the modal creation dialog with its defaults, mirroring
 * {@link EditorKeyboardConstructionTest} and {@link PaletteDropTest}.</p>
 */
@Tag("display")
@Timeout(120)
class FocusFaithfulKeyboardTest {

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
	 * Prove the driver routes keys through the real focus owner: an arrow
	 * routed while the palette button holds focus does not move the canvas
	 * caret (leg 1), and after the palette choice hands focus to the canvas
	 * an arrow routed through the focus owner moves the placing gate one
	 * grid step (leg 2).
	 */
	@Test
	void keyboardInputRoutesThroughTheRealFocusOwnerNotTheCanvas()
			throws Exception {
		Circuit circuit = new Circuit("focus-faithful");
		int step = JLSInfo.spacing;
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {

			JButton orButton = findToolbarButton(ui.editor, "OR gate");
			assertNotNull(orButton, "toolbar has an \"OR gate\" button");

			// ---- Leg 1: faithfulness guard ------------------------------
			// Force focus onto the palette button (a stand-in for a
			// focus-stranding regression) and route an arrow through the
			// LIVE focus owner. A focus-faithful driver delivers it to the
			// button, so the canvas keyboard caret stays unset. The old
			// pressKey -> canvas.dispatchEvent path would have moved the
			// caret regardless of focus; this assertion is exactly what
			// falsifies that bypass.
			ui.giveFocusTo(orButton);
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			assertNull(ui.keyboardCaret(),
					"an arrow routed to the focused palette button did not "
							+ "move the canvas caret");

			// ---- Leg 2: the real path -----------------------------------
			// Choose the OR gate the keyboard-equivalent way (activate the
			// focused button). The #75 H2 setup() handoff must move focus to
			// the canvas; without it, focus stays on the button and leg 2
			// fails. Then route an arrow through the live focus owner: the
			// editor is in the placing state, so the gate follows one grid
			// step - observable proof the key reached the canvas through the
			// real focus subsystem.
			SwingUtilities.invokeAndWait(orButton::doClick);
			ui.waitFor(() -> present(circuit, OrGate.class), "OR gate created");
			// Wait for the real handoff to settle before routing the key -
			// the modal creation dialog closes asynchronously and focus
			// returns to the canvas a beat later, exactly as a real user
			// waits for the dialog to dismiss before pressing arrows. With
			// the setup() handoff broken this wait times out (focus never
			// reaches the canvas), so leg 2 still goes RED on that
			// regression.
			ui.waitFor(ui::canvasIsFocusOwner,
					"choosing from the palette handed focus to the canvas");
			OrGate or = assertElementPresent(circuit, OrGate.class);
			int beforeX = or.getX();
			// Now route the arrow through the LIVE focus owner: the editor is
			// in the placing state, so the gate follows one grid step. This
			// observes that the key genuinely reaches the canvas through the
			// real focus subsystem - not force-fed to a hardcoded reference.
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			ui.waitFor(() -> or.getX() == beforeX + step,
					"an arrow routed through the focus owner moved the placing "
							+ "OR gate one grid step (proves the key reached the "
							+ "canvas via the real focus subsystem)");
		}
	} // end of keyboardInputRoutesThroughTheRealFocusOwnerNotTheCanvas method

	/** Whether the circuit holds an element of the given type. */
	private static boolean present(Circuit circuit, Class<?> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return true;
			}
		}
		return false;
	} // end of present method

	/** The toolbar button for the given tooltip (set by makeElement). */
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
	} // end of findToolbarButton method

	/** The button with the given text inside a dialog (e.g. "OK"). */
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
	} // end of findButton method

} // end of FocusFaithfulKeyboardTest class
