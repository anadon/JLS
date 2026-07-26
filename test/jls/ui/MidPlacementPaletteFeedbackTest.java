package jls.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.elem.AndGate;
import jls.elem.Element;
import jls.elem.OrGate;

/**
 * Selecting a palette element while another element is mid-placement is
 * acknowledged instead of silently swallowed (issue #207). The editor's
 * {@code setup()} refuses new elements while {@code currentState != idle}
 * - defensible on its own, but before the fix the refusal had no
 * observable effect, so a stuck placement made every further palette
 * click read as a frozen editor. The adjudicated remedy is
 * acknowledge-and-ignore: the refused click sets the info status label to
 * "finish or cancel the current placement first" and the in-flight
 * placement is left untouched.
 *
 * <p>Tagged {@code display}: needs a display for the gate creation
 * dialog (accepted by a background thread with its defaults, the
 * {@link KeyboardPlacementFaithfulTest} idiom) and font metrics, so it
 * runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(120)
class MidPlacementPaletteFeedbackTest {

	/** The #207 acknowledgement shown for a refused palette selection. */
	private static final String ACKNOWLEDGEMENT =
			"finish or cancel the current placement first";

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
							JButton ok = findButtonByText(w, "OK");
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
	void stopAcceptorAndDisposeWindows() throws Exception {
		stopAcceptor();
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	private void stopAcceptor() throws Exception {
		accepting = false;
		if (acceptor != null) {
			acceptor.join(1000);
		}
	}

	/**
	 * Enter the placing state with an OR gate, then click the AND gate
	 * palette button: the click is refused (no AND gate, no new dialog)
	 * but acknowledged in the info status label, so the editor no longer
	 * reads as frozen (issue #207 P2; P1 observed neither dialog nor any
	 * status text).
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void refusedPaletteClickWhilePlacingIsAcknowledgedInStatusLabel()
			throws Exception {
		Circuit circuit = new Circuit("mid-placement");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			// resolve both palette buttons by their #210 stable names
			JButton orButton = findByName(ui.editor, "palette.orgate");
			assertNotNull(orButton, "palette.orgate resolves");
			JButton andButton = findByName(ui.editor, "palette.andgate");
			assertNotNull(andButton, "palette.andgate resolves");

			// choose the OR gate; the acceptor OKs its creation dialog and
			// the editor enters the placing state (gate follows the cursor)
			SwingUtilities.invokeAndWait(orButton::doClick);
			ui.waitFor(() -> present(circuit, OrGate.class),
					"OR gate created");
			ui.waitFor(ui::canvasIsFocusOwner,
					"choosing from the palette handed focus to the canvas");

			// no acceptor from here on: were the refused click to open a
			// dialog anyway (the regression), nothing would dismiss it and
			// the doClick below would hang into the test timeout
			stopAcceptor();

			// mid-placement, click a different palette element
			SwingUtilities.invokeAndWait(andButton::doClick);

			// acknowledge-and-ignore: the info label explains the refusal...
			ui.waitFor(() -> {
				try {
					return labelShowing(ui.editor, ACKNOWLEDGEMENT);
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}, "the info label acknowledges the refused palette click");

			// ...and the selection was ignored: no AND gate, no new dialog,
			// and the in-flight OR gate placement is untouched
			assertFalse(present(circuit, AndGate.class),
					"the refused selection created no AND gate");
			assertFalse(anyDialogVisible(),
					"the refused selection opened no dialog");
			assertTrue(present(circuit, OrGate.class),
					"the in-flight OR gate placement is untouched");
		}
	}

	/** Whether a JLabel in the tree currently shows the given text. */
	private static boolean labelShowing(Container root, String text)
			throws Exception {
		AtomicBoolean found = new AtomicBoolean();
		SwingUtilities.invokeAndWait(
				() -> found.set(findLabelWithText(root, text)));
		return found.get();
	}

	private static boolean findLabelWithText(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof JLabel label && text.equals(label.getText())) {
				return true;
			}
			if (c instanceof Container inner
					&& findLabelWithText(inner, text)) {
				return true;
			}
		}
		return false;
	}

	private static boolean anyDialogVisible() throws Exception {
		AtomicBoolean visible = new AtomicBoolean();
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				if (w instanceof JDialog && w.isVisible()) {
					visible.set(true);
				}
			}
		});
		return visible.get();
	}

	private static boolean present(Circuit circuit, Class<?> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return true;
			}
		}
		return false;
	}

	private static JButton findByName(Container root, String name) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button && name.equals(button.getName())) {
				return button;
			}
			if (c instanceof Container inner) {
				JButton found = findByName(inner, name);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static JButton findButtonByText(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button
					&& text.equals(button.getText())) {
				return button;
			}
			if (c instanceof Container inner) {
				JButton found = findButtonByText(inner, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
