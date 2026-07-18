package jls.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import jls.Circuit;
import jls.JLSInfo;
import jls.edit.Editor;
import jls.sim.InteractiveSimulator;

/**
 * Layer-2 gesture harness (issue #91): boots a real {@link Editor} on a
 * circuit and drives its mouse state machine with synthetic
 * {@link MouseEvent}s dispatched to the canvas on the EDT.
 *
 * Synthetic events, not {@link java.awt.Robot}: the editor's move,
 * select, and menu paths read {@code event.getX()/getY()} (not
 * {@code getMousePosition()}), so they are fully drivable without a real
 * pointer - which makes the suite deterministic and fast instead of
 * fighting Robot/Xvfb timing. Palette placement is the one flow that
 * genuinely needs a live pointer; it is covered separately by
 * {@link DialogConstructionSmokeTest} (which runs each element's
 * {@code setup()}), so it is deliberately out of scope here.
 *
 * Everything the harness touches survives the #84 SimpleEditor
 * decomposition: the canvas (found structurally as the scroll pane's
 * mouse-listening viewport view), the popup menus (found by item text),
 * and the circuit model asserted by {@link CircuitAssert}. It still
 * needs a display for the popup {@code show()} calls and font metrics,
 * so it is tagged {@code display} and runs under the #162 substrate.
 */
final class EditorGestureSupport implements AutoCloseable {

	final JFrame frame;
	final Editor editor;
	final Circuit circuit;

	/**
	 * The clipboard circuit handed to the {@link Editor}. The editor's
	 * Cut/Copy popup actions copy the selection into this circuit and
	 * Paste reads it back, so tests assert clipboard state through here.
	 */
	final Circuit clipboardCircuit;

	private final Component canvas;
	private long when = 1_000_000L;

	EditorGestureSupport(Circuit circuit) throws Exception {
		this.circuit = circuit;
		this.clipboardCircuit = new Circuit("clipboard");
		// undo/redo restore snapshots through JLSInfo.sim, which only the
		// full JLSStart GUI sets. The harness builds an Editor directly,
		// so it must supply the simulator the editor's undo path expects
		// (without this, undo NPEs - a harness gap, not an app bug).
		if (JLSInfo.sim == null) {
			JLSInfo.sim = new InteractiveSimulator();
		}

		AtomicReference<JFrame> frameRef = new AtomicReference<>();
		AtomicReference<Editor> editorRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			JFrame f = new JFrame("gesture-harness");
			JTabbedPane tabs = new JTabbedPane();
			Editor ed = new Editor(tabs, circuit, circuit.getName(),
					clipboardCircuit);
			tabs.addTab(circuit.getName(), ed);
			f.add(tabs);
			f.setSize(1000, 800);
			f.setLocation(0, 0);
			f.setVisible(true);
			frameRef.set(f);
			editorRef.set(ed);
		});
		frame = frameRef.get();
		editor = editorRef.get();
		canvas = findCanvas(editor);
		if (canvas == null) {
			throw new AssertionError("could not find the edit canvas");
		}
		SwingUtilities.invokeAndWait(() -> {}); // let the frame realize
		forcePaint(); // establishes the undo base snapshot (first paint)
	}

	/**
	 * Force a synchronous paint of the canvas. The editor pushes its
	 * undo base snapshot on the first {@code paintComponent}; synthetic
	 * gestures fire before the window manager's async first paint, so
	 * without this the undo stack has no base and undo is a no-op.
	 */
	void forcePaint() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Graphics g = canvas.getGraphics();
			if (g != null) {
				canvas.paint(g);
				g.dispose();
			}
		});
	}

	/**
	 * The editor's current circuit. Undo/redo rebuild the circuit
	 * through the load path and swap in a fresh instance
	 * ({@code finishDo}: {@code circuit = newCopy}), so the reference
	 * the harness was constructed with goes stale after an undo -
	 * always read live state through here.
	 */
	Circuit currentCircuit() {
		return editor.getCircuit();
	}

	@Override
	public void close() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	// ------------------------------------------------------------------
	// gestures - each dispatches synchronously on the EDT
	// ------------------------------------------------------------------

	private void dispatch(int id, int x, int y, int button,
			int modifiers, boolean popup) throws Exception {
		MouseEvent e = new MouseEvent(canvas, id, when++, modifiers,
				x, y, 1, popup, button);
		SwingUtilities.invokeAndWait(() -> canvas.dispatchEvent(e));
	}

	/** Left-press, drag through an intermediate point, release. */
	void leftDrag(int fromX, int fromY, int toX, int toY) throws Exception {
		dispatch(MouseEvent.MOUSE_PRESSED, fromX, fromY,
				MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK, false);
		int midX = (fromX + toX) / 2;
		int midY = (fromY + toY) / 2;
		dispatch(MouseEvent.MOUSE_DRAGGED, midX, midY, MouseEvent.NOBUTTON,
				InputEvent.BUTTON1_DOWN_MASK, false);
		dispatch(MouseEvent.MOUSE_DRAGGED, toX, toY, MouseEvent.NOBUTTON,
				InputEvent.BUTTON1_DOWN_MASK, false);
		dispatch(MouseEvent.MOUSE_RELEASED, toX, toY, MouseEvent.BUTTON1,
				0, false);
	}

	/** A left click (press then release) at a point, no drag. */
	void leftClick(int x, int y) throws Exception {
		dispatch(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON1,
				InputEvent.BUTTON1_DOWN_MASK, false);
		dispatch(MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1,
				0, false);
	}

	/** A right press (the popup trigger the editor checks for). */
	void rightPress(int x, int y) throws Exception {
		dispatch(MouseEvent.MOUSE_PRESSED, x, y, MouseEvent.BUTTON3,
				InputEvent.BUTTON3_DOWN_MASK, true);
	}

	/** A buttonless pointer move (drives the placing-state preview). */
	void moveTo(int x, int y) throws Exception {
		dispatch(MouseEvent.MOUSE_MOVED, x, y, MouseEvent.NOBUTTON, 0, false);
	}

	/**
	 * Warp the real pointer to a canvas-relative point. The one editor
	 * path synthetic events cannot drive is paste: it reads
	 * {@code getMousePosition()} (the genuine pointer), not an event
	 * coordinate, and aborts if the pointer is not over the canvas. Under
	 * the #162 Xvfb substrate the virtual pointer is exclusively ours, so
	 * a single XTEST warp keeps paste drivable without adopting
	 * Robot-driven clicking anywhere else.
	 *
	 * @param x canvas-relative x to park the pointer at
	 * @param y canvas-relative y to park the pointer at
	 */
	void warpPointerTo(int x, int y) throws Exception {
		java.awt.Point base = canvas.getLocationOnScreen();
		java.awt.Robot robot = new java.awt.Robot();
		robot.mouseMove(base.x + x, base.y + y);
		robot.waitForIdle();
	}

	/**
	 * Click the visible popup-menu item with the given text. The editor
	 * shows its option/new menus with {@code show(this, x, y)}; the item
	 * is found by walking every window's popup and doClick'd, which
	 * fires the same actionPerformed the user's click would.
	 */
	void clickPopupItem(String text) throws Exception {
		JMenuItem item = waitForMenuItem(text);
		SwingUtilities.invokeAndWait(item::doClick);
	}

	// ------------------------------------------------------------------
	// waiting and lookup
	// ------------------------------------------------------------------

	void pause(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Poll the model until a condition holds, or fail after a bound. */
	void waitFor(java.util.function.BooleanSupplier condition, String what) {
		for (int i = 0; i < 200; i++) {
			if (condition.getAsBoolean()) {
				return;
			}
			pause(25);
		}
		throw new AssertionError("timed out waiting for " + what);
	}

	private JMenuItem waitForMenuItem(String text) {
		for (int i = 0; i < 200; i++) {
			AtomicReference<JMenuItem> found = new AtomicReference<>();
			try {
				SwingUtilities.invokeAndWait(() -> {
					for (Window w : Window.getWindows()) {
						JMenuItem item = findMenuItem(w, text);
						if (item != null) {
							found.set(item);
							return;
						}
					}
					found.set(findMenuItem(frame, text));
				});
			} catch (Exception e) {
				throw new AssertionError("EDT lookup failed", e);
			}
			if (found.get() != null) {
				return found.get();
			}
			pause(25);
		}
		throw new AssertionError("timed out waiting for popup item '"
				+ text + "'");
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

	private static JMenuItem findMenuItem(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof JPopupMenu menu && menu.isVisible()) {
				for (Component mc : menu.getComponents()) {
					if (mc instanceof JMenuItem item
							&& text.equals(item.getText())) {
						return item;
					}
				}
			}
			if (c instanceof JMenuItem item && text.equals(item.getText())
					&& item.isShowing()) {
				return item;
			}
			if (c instanceof Container inner) {
				JMenuItem found = findMenuItem(inner, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
