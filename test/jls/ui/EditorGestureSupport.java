package jls.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
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
	 * Whether the edit canvas currently owns the keyboard focus. Used to
	 * pin the #75 H2 fix: choosing an element from the tool bar must hand
	 * focus to the canvas so the arrow/Enter placement keys reach it
	 * without the user first tabbing off the palette.
	 *
	 * @return true when the canvas is the focus owner.
	 */
	boolean canvasIsFocusOwner() {
		return canvas.isFocusOwner();
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
	 * A middle-button drag (issue #74 pan gesture): press BUTTON2, drag
	 * through an intermediate point, release. The editor's pan path reads
	 * {@code getXOnScreen()}, which the synthetic event derives from the
	 * (realized) canvas' on-screen location plus the component x, so the
	 * on-screen delta equals the component delta the test passes.
	 */
	void middleDrag(int fromX, int fromY, int toX, int toY) throws Exception {
		dispatch(MouseEvent.MOUSE_PRESSED, fromX, fromY, MouseEvent.BUTTON2,
				InputEvent.BUTTON2_DOWN_MASK, false);
		int midX = (fromX + toX) / 2;
		int midY = (fromY + toY) / 2;
		dispatch(MouseEvent.MOUSE_DRAGGED, midX, midY, MouseEvent.NOBUTTON,
				InputEvent.BUTTON2_DOWN_MASK, false);
		dispatch(MouseEvent.MOUSE_DRAGGED, toX, toY, MouseEvent.NOBUTTON,
				InputEvent.BUTTON2_DOWN_MASK, false);
		dispatch(MouseEvent.MOUSE_RELEASED, toX, toY, MouseEvent.BUTTON2,
				0, false);
	}

	/**
	 * A Ctrl/Cmd+wheel notch over a canvas point (issue #74 zoom-at-cursor,
	 * the {@code applyZoom} path). Carries the platform menu-shortcut
	 * modifier the editor tests for, so it drives the real zoom branch of
	 * {@code mouseWheelMoved} rather than the plain-scroll branch.
	 *
	 * @param x  canvas-relative x the notch is centered on.
	 * @param y  canvas-relative y the notch is centered on.
	 * @param up true to zoom in (negative wheel rotation), false to zoom
	 *           out.
	 */
	void ctrlWheel(int x, int y, boolean up) throws Exception {
		int menuMask = java.awt.Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMaskEx();
		int rotation = up ? -1 : 1;
		java.awt.event.MouseWheelEvent e = new java.awt.event.MouseWheelEvent(
				canvas, java.awt.event.MouseWheelEvent.MOUSE_WHEEL, when++,
				menuMask, x, y, 0, 0, 0, false,
				java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation,
				(double) rotation);
		SwingUtilities.invokeAndWait(() -> canvas.dispatchEvent(e));
	}

	/**
	 * The scroll pane's current view position (top-left visible model*scale
	 * pixel), read on the EDT. Combined with {@link #zoomScale()} this lets
	 * a test compute the on-screen position of any model point and pin the
	 * zoom-at-cursor screen-fixedness property (P3) at the editor level.
	 *
	 * @return the JViewport view position.
	 * @throws Exception if the EDT dispatch fails.
	 */
	java.awt.Point viewPosition() throws Exception {
		AtomicReference<java.awt.Point> p = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			Component c = canvas.getParent();
			if (c instanceof javax.swing.JViewport vp) {
				p.set(vp.getViewPosition());
			} else {
				p.set(new java.awt.Point(0, 0));
			}
		});
		return p.get();
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

	/**
	 * The currently visible popup-menu item with the given text (issue
	 * #75). Used to prove the popup item and the canvas key binding
	 * dispatch through the same shared {@link javax.swing.Action}.
	 *
	 * @param text the item's label.
	 * @return the menu item.
	 */
	JMenuItem popupItem(String text) {
		return waitForMenuItem(text);
	}

	/**
	 * Whether a showing popup menu currently offers an item with the given
	 * text - a single, non-blocking probe (unlike {@link #popupItem}, which
	 * polls to a long bound and fails on timeout). Lets a test retry the
	 * right-press that raises the popup when the first press did not
	 * materialize it (a popup-show timing flake under the WM-less #162 Xvfb).
	 *
	 * @param text the item's label.
	 * @return true if the item is showing in a visible popup right now.
	 * @throws Exception if the EDT dispatch fails.
	 */
	boolean isPopupItemShowing(String text) throws Exception {
		AtomicReference<JMenuItem> found = new AtomicReference<>();
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
		return found.get() != null;
	}

	/**
	 * The action registered in the canvas' ActionMap under the given key
	 * (issue #75), read on the EDT. The shared editing Actions are stored
	 * here by the canvas key bindings, so this lets a test assert the
	 * binding points at {@code editor.editAction(op)} rather than a
	 * separate copy.
	 *
	 * @param key the ActionMap key (e.g. "do cut").
	 * @return the bound action, or null if none.
	 * @throws Exception if the EDT dispatch fails.
	 */
	javax.swing.Action canvasAction(String key) throws Exception {
		AtomicReference<javax.swing.Action> a = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			if (canvas instanceof javax.swing.JComponent jc) {
				a.set(jc.getActionMap().get(key));
			}
		});
		return a.get();
	}

	/**
	 * The action a keystroke resolves to through the canvas' WHEN_FOCUSED
	 * maps (issue #75): {@code actionMap.get(inputMap.get(stroke))}, read
	 * on the EDT. Returns null if either map lacks the entry, so a test
	 * can pin a binding end to end - removing the InputMap stroke or the
	 * ActionMap entry makes this null, failing an identity assertion.
	 *
	 * @param stroke the keystroke to resolve.
	 * @return the bound action, or null if the stroke is unbound.
	 * @throws Exception if the EDT dispatch fails.
	 */
	javax.swing.Action canvasActionForStroke(javax.swing.KeyStroke stroke)
			throws Exception {
		AtomicReference<javax.swing.Action> a = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			if (canvas instanceof javax.swing.JComponent jc) {
				Object name = jc.getInputMap().get(stroke);
				if (name != null) {
					a.set(jc.getActionMap().get(name));
				}
			}
		});
		return a.get();
	}

	/**
	 * Dispatch a KEY_PRESSED to the canvas on the EDT (issue #75),
	 * exercising the same WHEN_FOCUSED InputMap the user's keystroke
	 * would. Mirrors the synthetic-mouse idiom: no {@link java.awt.Robot},
	 * so it stays deterministic under Xvfb.
	 *
	 * @param keyCode the {@link java.awt.event.KeyEvent} VK_ code.
	 * @param modifiers the extended modifier mask (0 for none).
	 * @throws Exception if the EDT dispatch fails.
	 */
	void pressKey(int keyCode, int modifiers) throws Exception {
		java.awt.event.KeyEvent e = new java.awt.event.KeyEvent(canvas,
				java.awt.event.KeyEvent.KEY_PRESSED, when++, modifiers,
				keyCode, java.awt.event.KeyEvent.CHAR_UNDEFINED);
		SwingUtilities.invokeAndWait(() -> canvas.dispatchEvent(e));
	}

	/**
	 * Dispatch an unmodified KEY_PRESSED to the canvas on the EDT.
	 *
	 * @param keyCode the {@link java.awt.event.KeyEvent} VK_ code.
	 * @throws Exception if the EDT dispatch fails.
	 */
	void pressKey(int keyCode) throws Exception {
		pressKey(keyCode, 0);
	}

	// ------------------------------------------------------------------
	// focus-faithful key driver (issue #75 verification hardening)
	// ------------------------------------------------------------------

	/**
	 * The component that currently owns the keyboard focus, read on the EDT
	 * from the real {@link KeyboardFocusManager} - the same authority the
	 * AWT event pipeline consults when routing a user's keystroke. Unlike
	 * the hardcoded {@code canvas} the synthetic {@link #pressKey}
	 * dispatches to, this reflects where a real key would actually land, so
	 * a test built on it goes red if a focus regression strands focus off
	 * the canvas (the exact class of the #75 H2 tool-bar focus bug).
	 *
	 * @return the live focus owner, or null if no component in this JVM owns
	 *         the focus.
	 * @throws Exception if the EDT dispatch fails.
	 */
	Component focusOwner() throws Exception {
		AtomicReference<Component> owner = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> owner.set(KeyboardFocusManager
				.getCurrentKeyboardFocusManager().getFocusOwner()));
		return owner.get();
	}

	/**
	 * Dispatch a KEY_PRESSED to the LIVE keyboard-focus owner on the EDT
	 * (issue #75 verification hardening), instead of to the hardcoded canvas
	 * as {@link #pressKey} does. This is the focus-faithful key path: it
	 * reads {@code KeyboardFocusManager.getFocusOwner()} at dispatch time,
	 * so the keystroke reaches whatever component a real user's key would -
	 * the canvas' WHEN_FOCUSED bindings fire only when the canvas genuinely
	 * holds the focus. A regression that leaves focus stranded on the
	 * tool-bar button (the exact #75 H2 bug) therefore turns a test driven
	 * through here red, where {@link #pressKey} stays green by force-feeding
	 * the canvas regardless of focus.
	 *
	 * <p>Focus-owner dispatch, not {@link java.awt.Robot#keyPress}: under
	 * the #162 window-manager-less Xvfb a single {@code Robot} key press
	 * auto-repeats into many KEY_PRESSED events (empirically ~30 for one
	 * press/release), which makes exact "moved one grid step" assertions
	 * flaky; dispatching to the focus owner delivers exactly one event while
	 * still honoring the real focus subsystem - it routes to the button and
	 * not the canvas when focus is on the button, and vice versa (both
	 * verified empirically). The faithfulness we need is "which component
	 * receives the key", and that is the focus owner, read live here.</p>
	 *
	 * @param keyCode the {@link java.awt.event.KeyEvent} VK_ code.
	 * @param modifiers the extended modifier mask (0 for none).
	 * @throws Exception if there is no focus owner, or the EDT dispatch
	 *         fails.
	 */
	void pressKeyThroughFocusOwner(int keyCode, int modifiers)
			throws Exception {
		Component owner = focusOwner();
		if (owner == null) {
			throw new AssertionError("no focus owner to route the key to; a "
					+ "real keystroke would have nowhere to go either");
		}
		java.awt.event.KeyEvent e = new java.awt.event.KeyEvent(owner,
				java.awt.event.KeyEvent.KEY_PRESSED, when++, modifiers,
				keyCode, java.awt.event.KeyEvent.CHAR_UNDEFINED);
		SwingUtilities.invokeAndWait(() -> owner.dispatchEvent(e));
	}

	/**
	 * Dispatch an unmodified KEY_PRESSED to the live keyboard-focus owner on
	 * the EDT (issue #75 verification hardening).
	 *
	 * @param keyCode the {@link java.awt.event.KeyEvent} VK_ code.
	 * @throws Exception if there is no focus owner, or the EDT dispatch
	 *         fails.
	 */
	void pressKeyThroughFocusOwner(int keyCode) throws Exception {
		pressKeyThroughFocusOwner(keyCode, 0);
	}

	/**
	 * Move the keyboard focus to the given component and wait for the real
	 * {@link KeyboardFocusManager} to confirm the handoff (issue #75
	 * verification hardening). Used to set up a non-canvas focus owner so a
	 * test can prove {@link #pressKeyThroughFocusOwner} honors it - a
	 * deliberate stand-in for a focus-stranding regression.
	 *
	 * @param target the component to focus.
	 * @throws Exception if the EDT dispatch fails.
	 */
	void giveFocusTo(Component target) throws Exception {
		SwingUtilities.invokeAndWait(target::requestFocusInWindow);
		waitFor(() -> {
			try {
				return focusOwner() == target;
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}, "focus to move to " + target.getClass().getSimpleName());
	}

	/**
	 * Move the keyboard focus to the edit canvas and wait for the real
	 * {@link KeyboardFocusManager} to confirm the handoff (issue #75
	 * verification hardening). Used to stage the canvas as the genuine focus
	 * owner before a keyboard-editing test routes keys through
	 * {@link #pressKeyThroughFocusOwner}: once the canvas truly owns focus,
	 * routing a key through the live focus owner delivers it to the canvas
	 * exactly as a real user's keystroke would, so the InputMap/ActionMap
	 * wiring is exercised through the real event path rather than force-fed to
	 * a hardcoded reference.
	 *
	 * @throws Exception if the EDT dispatch fails.
	 */
	void focusCanvas() throws Exception {
		giveFocusTo(canvas);
	}

	/**
	 * The editor's keyboard construction caret (issue #75), read on the
	 * EDT, or null if the keyboard has not been used to point yet. Lets a
	 * keyboard-only construction test see where the next placement or wire
	 * endpoint will land between key presses.
	 *
	 * @return a copy of the caret point, or null.
	 * @throws Exception if the EDT dispatch fails.
	 */
	java.awt.Point keyboardCaret() throws Exception {
		AtomicReference<java.awt.Point> p = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> p.set(editor.keyboardCaret()));
		return p.get();
	}

	// ------------------------------------------------------------------
	// zoom (issue #74) - drive the editor's public zoom API on the EDT
	// ------------------------------------------------------------------

	/**
	 * Zoom the editor in one ladder stop, on the EDT.
	 *
	 * @throws Exception if the EDT dispatch fails.
	 */
	void zoomIn() throws Exception {
		SwingUtilities.invokeAndWait(editor::zoomIn);
	}

	/**
	 * Zoom the editor out one ladder stop, on the EDT.
	 *
	 * @throws Exception if the EDT dispatch fails.
	 */
	void zoomOut() throws Exception {
		SwingUtilities.invokeAndWait(editor::zoomOut);
	}

	/**
	 * Reset the editor to 100% zoom, on the EDT.
	 *
	 * @throws Exception if the EDT dispatch fails.
	 */
	void zoomReset() throws Exception {
		SwingUtilities.invokeAndWait(editor::zoomReset);
	}

	/**
	 * Fit the whole circuit into the canvas, on the EDT.
	 *
	 * @throws Exception if the EDT dispatch fails.
	 */
	void zoomToFit() throws Exception {
		SwingUtilities.invokeAndWait(editor::zoomToFit);
	}

	/**
	 * The editor's current zoom scale, read on the EDT.
	 *
	 * @return the zoom scale (1.0 = 100%).
	 * @throws Exception if the EDT dispatch fails.
	 */
	double zoomScale() throws Exception {
		AtomicReference<Double> s = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> s.set(editor.getZoomScale()));
		return s.get();
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
		// ~10s budget (400 * 25ms): display polls run on shared, often
		// heavily loaded CI runners where a 5s bound flakes under load.
		for (int i = 0; i < 400; i++) {
			if (condition.getAsBoolean()) {
				return;
			}
			pause(25);
		}
		throw new AssertionError("timed out waiting for " + what);
	}

	private JMenuItem waitForMenuItem(String text) {
		// ~10s budget: opening a popup and materializing its items can
		// exceed a 5s bound on a loaded CI runner (observed on #196).
		for (int i = 0; i < 400; i++) {
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
