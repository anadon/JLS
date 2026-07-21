package jls.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.DefaultExceptionHandler;
import jls.JLSInfo;
import jls.JLSStart;
import jls.edit.Editor;
import jls.sim.Simulator;

/**
 * Selecting an editor tab hands keyboard focus to that editor's canvas
 * (issue #75 accessibility, item 7). The tab-change listener calls
 * {@code Editor.focusOnCanvas()} so a keyboard user's next key reaches the
 * drawing surface without first tabbing off the tab strip. Previously only a
 * source grep for {@code "ed.focusOnCanvas()"} pinned this; nothing observed
 * the real focus owner become the canvas after a tab selection. This boots
 * the real {@link JLSStart}, adds an editor (which selects its tab), and
 * waits for the live {@link KeyboardFocusManager} focus owner to become that
 * editor's canvas.
 *
 * <p>Tagged {@code display}: {@code new JLSStart()} is a visible frame, so
 * it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(60)
class TabSelectionFocusTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * After a circuit's editor tab becomes selected, the real focus owner is
	 * that editor's canvas.
	 *
	 * @throws Exception on reflection or EDT failure.
	 */
	@Test
	void selectingAnEditorTabFocusesItsCanvas() throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			AtomicReference<Editor> edRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				jls.setupEditor(new Circuit("tabfocus"), "tabfocus");
				edRef.set(jls.getVisibleEditor());
			});
			Editor ed = edRef.get();
			assertNotNull(ed, "setupEditor made an editor visible");
			Component canvas = findCanvas(ed);
			assertNotNull(canvas, "the editor has a drawing canvas");

			waitFor(() -> {
				try {
					return focusOwner() == canvas;
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}, "selecting the editor tab moved focus to its canvas");
			assertTrue(true, "focus reached the canvas via focusOnCanvas");
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

	/** The live keyboard-focus owner, read on the EDT. */
	private static Component focusOwner() throws Exception {
		AtomicReference<Component> owner = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> owner.set(KeyboardFocusManager
				.getCurrentKeyboardFocusManager().getFocusOwner()));
		return owner.get();
	}

	/** Poll a condition to a ~10s bound. */
	private static void waitFor(java.util.function.BooleanSupplier cond,
			String what) {
		for (int i = 0; i < 400; i++) {
			if (cond.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("interrupted waiting for " + what);
			}
		}
		fail("timed out waiting for " + what);
	}

	/** The drawing canvas: the scroll pane's mouse-listening view. */
	private static Component findCanvas(Container root) {
		for (Component c : root.getComponents()) {
			if (c instanceof JScrollPane pane) {
				Component view = pane.getViewport().getView();
				if (view instanceof MouseInputListener
						|| view instanceof java.awt.event.MouseListener) {
					return view;
				}
			}
			if (c instanceof Container inner) {
				Component found = findCanvas(inner);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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
		return ref.get();
	}
}
