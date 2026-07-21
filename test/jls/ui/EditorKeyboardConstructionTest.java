package jls.ui;

import static jls.ui.CircuitAssert.assertConnected;
import static jls.ui.CircuitAssert.assertElementPresent;
import static jls.ui.CircuitAssert.assertNotConnected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

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
 * H2 acceptance test (issue #75): build the tutorial's first two-gate
 * circuit -- an OR gate and a NOT gate wired output-to-input, the A + ~B
 * skeleton of tutorial 1 -- with no pointing device. Every construction
 * step after the palette choice is driven through the REAL focus subsystem:
 * each key is dispatched to the live {@link java.awt.KeyboardFocusManager}
 * focus owner ({@link EditorGestureSupport#pressKeyThroughFocusOwner}), not
 * to a hardcoded canvas reference, so the keystroke reaches whatever
 * component a real user's key would. Arrow keys move the following element,
 * the grid caret, and the selection; Enter places elements, activates the
 * caret, and commits; the port-to-port connection is made the way the
 * tutorial itself makes it, by moving the NOT gate's output onto an OR
 * input so placement wires them. No {@link java.awt.Robot} and no mouse
 * events reach the canvas.
 *
 * <p>Choosing an element from the palette still activates the toolbar
 * button (a keyboard user tabs to it and presses it); this is done with
 * {@code doClick}, a keyboard-equivalent activation, not a pointer
 * gesture. The gate's creation dialog is real and modal, so a background
 * thread accepts it with its defaults (mirroring {@link PaletteDropTest}).
 * Because the keys route through the live focus owner, the test re-checks
 * after <em>each</em> palette choice that the #75 {@code setup()} handoff
 * put focus on the canvas ({@link EditorGestureSupport#canvasIsFocusOwner});
 * a focus-stranding regression on the first OR-gate choice or the second
 * NOT-gate choice therefore turns this red where a canvas-force-feeding
 * driver would stay green. Everything that positions, places, nudges, and
 * connects the gates is keyboard-only.</p>
 *
 * <p>The connection uses the editor's coincidence wiring (moving a port
 * onto another creates the wire) rather than free-hand wire drawing with
 * W: JLS grows a wire's bounds by a full grid spacing, so a short wire
 * drawn directly between two abutting gate ports registers as overlapping
 * both gates -- the same constraint the mouse faces, which is exactly why
 * the tutorial connects adjacent gates by touching their ports. The W
 * wire-start key is still exercised (it starts and cancels a wire); the
 * two-gate circuit itself is built the tutorial's way.</p>
 *
 * <p>Tagged {@code display}: needs a real display for the creation dialog
 * and font metrics, so it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(120)
class EditorKeyboardConstructionTest {

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
	 * Lay out and wire the OR + NOT two-gate circuit with the keyboard
	 * alone, exercising the caret, placement, selection nudging, the wire
	 * key, and coincidence wiring, then assert the resulting model is
	 * exactly the connected pair the mouse tutorial produces.
	 */
	@Test
	void keyboardBuildsTheTwoGateCircuit() throws Exception {
		Circuit circuit = new Circuit("keyboard");
		int step = JLSInfo.spacing;
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {

			// 1. choose the OR gate from the palette and place it with Enter
			JButton orButton = findToolbarButton(ui.editor, "OR gate");
			assertNotNull(orButton, "toolbar has an \"OR gate\" button");
			SwingUtilities.invokeAndWait(orButton::doClick);
			ui.waitFor(() -> present(circuit, OrGate.class), "OR gate created");
			// #75 H2: choosing from the palette must hand focus to the
			// canvas, or a real keyboard user's next arrow/Enter would go to
			// the tool-bar button, not the canvas. Pin that handoff (without
			// the setup() requestFocusInWindow this times out).
			ui.waitFor(ui::canvasIsFocusOwner,
					"choosing from the palette moved focus to the canvas");
			// The line above is the handoff PROOF (it times out if setup()
			// failed to move focus - see Mutation A). Then pin the canvas as
			// the KFM focus owner deterministically before faithful driving:
			// under the WM-less #162 Xvfb the live focus owner can read null
			// for a beat after a modal dialog closes even though the canvas is
			// the frame's focus owner, which a driver reading the live owner
			// at press time would trip over. focusCanvas() polls the KFM until
			// the canvas genuinely owns focus, so every key below lands there.
			ui.focusCanvas();
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);
			OrGate or = assertElementPresent(circuit, OrGate.class);

			// 2. arrow-key nudge of a selected element: move the caret onto
			// the OR gate, Enter to select it, then an arrow to nudge it one
			// grid step through the undoable move op
			Rectangle orRect = or.getRect();
			driveCaretToward(ui, orRect.x + orRect.width / 2,
					orRect.y + orRect.height / 2);
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);
			int beforeX = or.getX();
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			assertEquals(beforeX + step, or.getX(),
					"arrow key nudged the selected OR gate one grid step");
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER); // finish, back to idle

			// the OR input we will wire to, read after the nudge
			Input orIn = firstOf(or, Input.class);
			int ix = orIn.getX();
			int iy = orIn.getY();

			// 3. the W key starts a wire at the caret, and Esc abandons it -
			// pin that the wire tool is reachable from the keyboard
			driveCaretToward(ui, ix - 6 * step, iy);
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_W);
			ui.waitFor(() -> present(circuit, wireEndClass()),
					"W started a wire");
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ESCAPE);
			ui.waitFor(() -> !present(circuit, wireEndClass()),
					"Esc abandoned the wire");

			// 4. choose the NOT gate and, with arrow keys only, move it so
			// its output lands exactly on the OR input; Enter then places and
			// wires them, the way the tutorial connects abutting gates
			JButton notButton = findToolbarButton(ui.editor, "NOT gate");
			assertNotNull(notButton, "toolbar has a \"NOT gate\" button");
			SwingUtilities.invokeAndWait(notButton::doClick);
			ui.waitFor(() -> present(circuit, NotGate.class), "NOT gate created");
			// #75 H2, second palette handoff: choosing the SECOND tool
			// (after a modal creation dialog) must AGAIN hand focus to the
			// canvas, or the keyboard user's placement keys would strand on
			// the tool-bar button. The faithful driver below routes through
			// the live focus owner, so this re-check is load-bearing: if the
			// second handoff regressed, the caret keys would reach the button
			// (or throw for no focus owner), not the canvas.
			ui.waitFor(ui::canvasIsFocusOwner,
					"choosing the NOT gate handed focus back to the canvas");
			// handoff proven; pin the KFM owner deterministically (see the
			// OR-gate note above) before the faithful placement keys below
			ui.focusCanvas();
			NotGate not = assertElementPresent(circuit, NotGate.class);
			Output notOut = firstOf(not, Output.class);

			// not yet wired
			assertNotConnected(circuit, not, or);

			driveFollowingToward(ui, () -> new Point(notOut.getX(), notOut.getY()),
					ix, iy);
			assertEquals(new Point(ix, iy),
					new Point(notOut.getX(), notOut.getY()),
					"keyboard moved the NOT output onto the OR input");
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);

			// the keyboard-built circuit is the tutorial's connected pair
			assertElementPresent(circuit, OrGate.class);
			assertElementPresent(circuit, NotGate.class);
			assertConnected(circuit, not, or);
		}
	} // end of keyboardBuildsTheTwoGateCircuit method

	/**
	 * Draw a wire in open space with the keyboard alone (issue #75):
	 * position the caret, W to start, Enter to anchor and begin the
	 * segment, arrow keys to run the wire's free end out. A real Wire
	 * appears in the model, drawn with no pointing device, and its far end
	 * follows the arrow keys. Esc then abandons the in-progress end. This
	 * pins the free-hand wire tool; the two-gate test connects abutting
	 * gate ports the tutorial's coincidence way, the robust path when a
	 * wire would abut a gate body.
	 */
	@Test
	void keyboardDrawsAWireInOpenSpace() throws Exception {
		Circuit circuit = new Circuit("wire");
		int step = JLSInfo.spacing;
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {

			// stage the canvas as the genuine focus owner, then drive every
			// key through the live focus owner (no palette choice precedes
			// this test, so there is no setup() handoff to rely on)
			ui.focusCanvas();

			// caret to a clear grid point, well away from any element
			driveCaretToward(ui, 20 * step, 10 * step);
			int startX = ui.keyboardCaret().x;
			int startY = ui.keyboardCaret().y;

			// W starts the wire, Enter anchors it and begins a drawn segment
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_W);
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ENTER);
			ui.waitFor(() -> present(circuit, wireClass()),
					"the keyboard started drawing a wire");

			// run the free end out four grid steps to the right; the caret
			// (the wire's moving end) tracks it there
			for (int i = 0; i < 4; i++) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			}
			assertEquals(new Point(startX + 4 * step, startY),
					ui.keyboardCaret(),
					"arrow keys ran the wire's free end four grid steps out");

			// the drawn wire now spans the four grid steps the arrows ran out
			Element wire = firstPresent(circuit, wireClass());
			Rectangle wr = wire.getRect();
			assertEquals(4 * step, wr.width - 2 * step,
					"wire spans the four grid steps (bounds grow one spacing "
							+ "each side)");

			// Esc abandons the in-progress end
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ESCAPE);
		}
	} // end of keyboardDrawsAWireInOpenSpace method

	/**
	 * Press arrow keys until the point the supplier reports reaches the
	 * target grid coordinates. Used to move a following element (its port
	 * read live) into place with the keyboard.
	 *
	 * @param ui the gesture harness.
	 * @param cur supplies the current tracked point (read after each key).
	 * @param tx the target x.
	 * @param ty the target y.
	 * @throws Exception if a key dispatch fails.
	 */
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
	} // end of driveFollowingToward method

	/**
	 * Press arrow keys until the keyboard caret reaches the target grid
	 * coordinates, initializing it with the first press.
	 *
	 * @param ui the gesture harness.
	 * @param tx the target x.
	 * @param ty the target y.
	 * @throws Exception if a key dispatch fails.
	 */
	private void driveCaretToward(EditorGestureSupport ui, int tx, int ty)
			throws Exception {
		for (int i = 0; i < 600; i++) {
			Point c = ui.keyboardCaret();
			if (c != null && c.x == tx && c.y == ty) {
				return;
			}
			if (c == null) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_LEFT);
			} else if (c.x < tx) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			} else if (c.x > tx) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_LEFT);
			} else if (c.y < ty) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_DOWN);
			} else {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_UP);
			}
		}
		fail("caret could not reach (" + tx + "," + ty + "); last at "
				+ ui.keyboardCaret());
	} // end of driveCaretToward method

	/** Whether the circuit holds an element of the given type. */
	private static boolean present(Circuit circuit, Class<?> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return true;
			}
		}
		return false;
	} // end of present method

	/** The WireEnd class, looked up by name to avoid an elem import. */
	private static Class<?> wireEndClass() {
		return byName("jls.elem.WireEnd");
	} // end of wireEndClass method

	/** The Wire class, looked up by name to avoid an elem import. */
	private static Class<?> wireClass() {
		return byName("jls.elem.Wire");
	} // end of wireClass method

	/** A class looked up by name, failing the test if it is missing. */
	private static Class<?> byName(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	} // end of byName method

	/** The first element of the given type, or a test failure if none. */
	private static Element firstPresent(Circuit circuit, Class<?> type) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return el;
			}
		}
		fail("no " + type.getSimpleName() + " present");
		return null; // unreachable
	} // end of firstPresent method

	/** The first put of the given kind on the element. */
	private static <T extends Put> T firstOf(Element el, Class<T> kind) {
		for (Put p : el.getAllPuts()) {
			if (kind.isInstance(p)) {
				return kind.cast(p);
			}
		}
		fail("no " + kind.getSimpleName() + " put on " + el);
		return null; // unreachable
	} // end of firstOf method

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

} // end of EditorKeyboardConstructionTest class
