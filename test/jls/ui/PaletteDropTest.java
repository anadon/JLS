package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.elem.AndGate;

/**
 * Layer-2 palette-click characterization (issue #91, the outstanding P2
 * acceptance criterion): synthesize a click on a toolbar palette entry and
 * assert the resulting element lands in the circuit at the drop location.
 *
 * Boots a real {@link jls.edit.Editor} the same way
 * {@link EditorGestureSupport} does, then walks the component tree
 * (mirroring {@code EditorGestureSupport.findMenuItem}'s pattern) to find
 * the "AND gate" toolbar button {@code SimpleEditor.makeElement} builds.
 * {@code doClick()} on that button fires the same
 * {@code actionPerformed}/{@code setup(...)} path a real palette click
 * would; it is not a {@link java.awt.Robot} gesture, so unlike the true
 * pointer-driven drop described by {@code EditorGestureSupport}'s
 * javadoc, this exercises only the toolbar's own default-drop-point
 * branch (no live pointer over the canvas). The AND gate's creation
 * dialog is still real and modal, so a background thread accepts it with
 * its defaults (mirroring {@code DialogConstructionSmokeTest}'s watcher,
 * which instead cancels) while the test thread's {@code doClick} blocks
 * on the EDT.
 *
 * Tagged {@code display}: needs a real display for the dialog and
 * toolbar layout, so it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).
 */
@Tag("display")
@Timeout(60)
class PaletteDropTest {

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

	@Test
	void clickingThePaletteAndGateButtonAddsItToTheCircuit() throws Exception {
		Circuit circuit = new Circuit("golden");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			JButton andButton = findToolbarButton(ui.editor, "AND gate");
			assertNotNull(andButton, "toolbar has an \"AND gate\" button");

			// the click fires SimpleEditor's actionPerformed -> setup(new
			// AndGate(circuit), true), which shows the (modal) creation
			// dialog synchronously; the acceptor thread above clicks its
			// OK button with the form's defaults so this returns
			SwingUtilities.invokeAndWait(andButton::doClick);

			AndGate gate = assertElementPresent(circuit, AndGate.class);

			// on-grid: snapped to the JLSInfo.spacing grid, like any
			// placed element
			GeometryAssert.assertOnGrid(gate);

			// within the visible canvas viewport: the toolbar-invoked
			// default drop point is derived from the scroll pane's
			// current view position and size (SimpleEditor.setup,
			// fromToolBar branch), so the element must land inside it
			JScrollPane pane = findScrollPane(ui.editor);
			assertNotNull(pane, "editor canvas is hosted in a scroll pane");
			Point viewPos = pane.getViewport().getViewPosition();
			Rectangle viewport = new Rectangle(viewPos, pane.getViewport().getExtentSize());
			assertTrue(viewport.contains(gate.getRect()),
					CircuitAssert.describe(gate) + " with bounds " + gate.getRect()
							+ " is not within the visible canvas viewport " + viewport);
		}
	}

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
	}

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
	}

	/** The scroll pane hosting the edit canvas. */
	private static JScrollPane findScrollPane(Container root) {
		for (Component c : root.getComponents()) {
			if (c instanceof JScrollPane pane) {
				return pane;
			}
			if (c instanceof Container inner) {
				JScrollPane found = findScrollPane(inner);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
