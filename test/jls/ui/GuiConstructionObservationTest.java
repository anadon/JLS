package jls.ui;

import static jls.ui.CircuitAssert.assertConnected;
import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
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
import jls.elem.Input;
import jls.elem.NotGate;
import jls.elem.OrGate;
import jls.elem.Output;
import jls.elem.Put;

/**
 * GUI construction-and-observation primer (follow-up to issue #91's Layer 2).
 *
 * <p>Building something large in JLS -- a datapath, a whole CPU -- is a
 * <em>GUI</em> activity: a student drops elements from the palette and wires
 * them on the canvas. This test exercises that real-user path end to end
 * through the live editor (palette activation, canvas focus handoff, keyboard
 * placement, coincidence wiring) and then <b>observes the result</b> by
 * exporting a PNG of the constructed circuit with {@link Circuit#exportImage}.
 * The screenshot is written under {@code target/gui-observation/} so a human
 * can look at exactly what the GUI session produced -- the observability the
 * batch/headless path (used to generate the RV32I CPU elsewhere in this repo)
 * cannot give.
 *
 * <p>It is deliberately small and robust (an OR + NOT connected pair, the
 * tutorial's first circuit, built with gates that have no required dialog
 * fields) so it is a dependable primer for a larger GUI-only construction
 * exercise rather than a brittle mega-build. The point it pins: a real user's
 * GUI construction of a working circuit can be driven and screenshotted
 * under the #162 Xvfb substrate.
 *
 * <p>Tagged {@code display}: runs under
 * {@code xvfb-run -a mvn -B verify -Djls.test.headless=false}.
 */
@Tag("display")
@Timeout(120)
class GuiConstructionObservationTest {

	private volatile boolean accepting;
	private Thread acceptor;

	@BeforeEach
	void requireDisplayAndStartAcceptor() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
		// element creation dialogs are modal; a daemon accepts them by default
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
	void buildsACircuitInTheGuiAndScreenshotsIt() throws Exception {
		Circuit circuit = new Circuit("guiprimer");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {

			// place an OR gate from the palette
			JButton orButton = findToolbarButton(ui.editor, "OR gate");
			assertNotNull(orButton, "toolbar has an \"OR gate\" button");
			SwingUtilities.invokeAndWait(orButton::doClick);
			ui.waitFor(() -> present(circuit, OrGate.class), "OR gate created");
			ui.waitFor(ui::canvasIsFocusOwner, "palette handed focus to canvas");
			ui.focusCanvas();
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);
			OrGate or = assertElementPresent(circuit, OrGate.class);
			Input orIn = firstOf(or, Input.class);
			int ix = orIn.getX();
			int iy = orIn.getY();

			// place a NOT gate and move its output onto the OR input so
			// committing wires them (the tutorial's coincidence connection)
			JButton notButton = findToolbarButton(ui.editor, "NOT gate");
			assertNotNull(notButton, "toolbar has a \"NOT gate\" button");
			SwingUtilities.invokeAndWait(notButton::doClick);
			ui.waitFor(() -> present(circuit, NotGate.class), "NOT gate created");
			ui.waitFor(ui::canvasIsFocusOwner, "second palette handoff to canvas");
			ui.focusCanvas();
			NotGate not = assertElementPresent(circuit, NotGate.class);
			Output notOut = firstOf(not, Output.class);
			driveFollowingToward(ui, () -> new Point(notOut.getX(), notOut.getY()),
					ix, iy);
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);
			assertConnected(circuit, not, or);

			// observe: export a PNG of the GUI-constructed circuit
			File dir = new File("target/gui-observation");
			dir.mkdirs();
			File png = new File(dir, "gui-built-circuit.png");
			SwingUtilities.invokeAndWait(() -> {
				try {
					circuit.exportImage(png.getPath());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			assertTrue(png.isFile() && png.length() > 0,
					"a screenshot PNG of the constructed circuit was written to "
							+ png.getPath());
			BufferedImage img = ImageIO.read(png);
			assertNotNull(img, "the screenshot is a readable image");
			assertTrue(img.getWidth() > 0 && img.getHeight() > 0,
					"the screenshot has real pixels (" + img.getWidth() + "x"
							+ img.getHeight() + ")");
		}
	}

	// ---- helpers (mirroring EditorKeyboardConstructionTest) ----

	private void driveFollowingToward(EditorGestureSupport ui,
			Supplier<Point> cur, int tx, int ty) throws Exception {
		for (int i = 0; i < 400; i++) {
			Point p = cur.get();
			if (p.x == tx && p.y == ty) {
				return;
			}
			if (p.x < tx) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			} else if (p.x > tx) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_LEFT);
			} else if (p.y < ty) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_DOWN);
			} else {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_UP);
			}
		}
		fail("keyboard could not reach (" + tx + "," + ty + "); last at "
				+ cur.get());
	}

	private static boolean present(Circuit circuit, Class<?> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return true;
			}
		}
		return false;
	}

	private static <T extends Put> T firstOf(Element el, Class<T> kind) {
		for (Put p : el.getAllPuts()) {
			if (kind.isInstance(p)) {
				return kind.cast(p);
			}
		}
		fail("no " + kind.getSimpleName() + " put on " + el);
		return null;
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
