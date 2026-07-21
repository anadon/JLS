package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.edit.EditOp;
import jls.elem.AndGate;
import jls.elem.Element;
import jls.elem.Gate;

/**
 * Keyboard editing operations verified through the REAL focus subsystem
 * (issue #75 verification hardening). Every editing key a user presses -
 * arrow-nudge, undo, redo, rotate, flip, delete, wire-start, escape - is
 * driven here through
 * {@link EditorGestureSupport#pressKeyThroughFocusOwner}, which dispatches
 * to the live {@link java.awt.KeyboardFocusManager} focus owner rather than
 * to a hardcoded canvas reference. The canvas is first made the genuine
 * focus owner ({@link EditorGestureSupport#focusCanvas}); every key then
 * reaches it exactly as a real keystroke would, so a regression that broke
 * key delivery to the focus owner (the class of the #75 H2 tool-bar focus
 * bug) turns these tests red where the old {@code pressKey} would force-feed
 * the canvas and stay green.
 *
 * <p>These close the enumerated keyboard-construction gaps that were
 * exercised only via {@code canvas.dispatchEvent} (nudge + its untested undo
 * leg, delete, wire-start, escape) or not driven by any keystroke at all
 * (rotate, flip, undo/redo). The selection each op acts on is established
 * with a rubber-band drag (the mechanism under test is the op key, not the
 * selection gesture); the op key itself is routed faithfully.</p>
 *
 * <p>Tagged {@code display}: needs a real display and the real focus
 * subsystem, so it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(120)
class KeyboardEditingFaithfulTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/** A circuit with a single, unattached AND gate that can rotate/flip. */
	private static Circuit oneGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("kbd-edit");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	/** A circuit with a single input pin - a watchable element (canWatch). */
	private static Circuit onePin() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.inputPin("A", 1);
		Circuit circuit = new Circuit("kbd-watch");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	/** The sole watchable Pin in the current circuit. */
	private static jls.elem.Pin solePin(EditorGestureSupport ui) {
		for (Element el : ui.currentCircuit().getElements()) {
			if (el instanceof jls.elem.Pin p) {
				return p;
			}
		}
		throw new AssertionError("no Pin in the circuit");
	}

	private static int centerX(Element el) {
		return el.getX() + el.getRect().width / 2;
	}

	private static int centerY(Element el) {
		return el.getY() + el.getRect().height / 2;
	}

	/** Rubber-band select the gate and make the canvas the real focus owner. */
	private static void selectGateAndFocusCanvas(EditorGestureSupport ui,
			Element gate) throws Exception {
		int x0 = gate.getX() - 40, y0 = gate.getY() - 40;
		int x1 = gate.getX() + gate.getRect().width + 40;
		int y1 = gate.getY() + gate.getRect().height + 40;
		ui.leftDrag(x0, y0, x1, y1);
		ui.waitFor(gate::isHighlighted, "gate rubber-band selected");
		ui.focusCanvas();
		assertTrue(ui.canvasIsFocusOwner(),
				"canvas is the genuine focus owner before the op key");
	}

	/** The sole Gate in the current (possibly rebuilt) circuit. */
	private static Gate soleGate(EditorGestureSupport ui) {
		for (Element el : ui.currentCircuit().getElements()) {
			if (el instanceof Gate g) {
				return g;
			}
		}
		throw new AssertionError("no Gate in the circuit");
	}

	/**
	 * The element's persisted save block - the real, user-visible form
	 * rotate and flip mutate (orientation string, recomputed geometry). A
	 * clean observable that changes iff the op actually ran.
	 */
	private static String save(Element el) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		el.save(pw);
		pw.flush();
		return sw.toString();
	}

	/**
	 * An arrow key routed through the real focus owner nudges the selected
	 * gate one grid step (the undoable {@code MoveElements} path), and Ctrl+Z
	 * routed the same way undoes it - pinning both the faithful nudge and the
	 * previously untested undo-of-nudge leg.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void arrowNudgeThroughFocusOwnerIsUndoableWithCtrlZ() throws Exception {
		Circuit circuit = oneGate();
		int step = JLSInfo.spacing;
		String os = System.getProperty("os.name");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			selectGateAndFocusCanvas(ui, gate);

			int beforeX = gate.getX();
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			ui.waitFor(() -> soleGate(ui).getX() == beforeX + step,
					"an arrow through the focus owner nudged the gate one step");

			// Ctrl+Z (platform undo) through the focus owner restores it
			KeyStroke undo = EditOp.UNDO.accelerator(os);
			ui.pressKeyThroughFocusOwner(undo.getKeyCode(), undo.getModifiers());
			ui.waitFor(() -> soleGate(ui).getX() == beforeX,
					"Ctrl+Z through the focus owner undid the nudge");
		}
	}

	/**
	 * After undoing a nudge, the platform redo key routed through the focus
	 * owner re-applies it - pinning redo-via-keystroke, which no test drove
	 * before.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void redoThroughFocusOwnerReappliesTheNudge() throws Exception {
		Circuit circuit = oneGate();
		int step = JLSInfo.spacing;
		String os = System.getProperty("os.name");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			selectGateAndFocusCanvas(ui, gate);

			int beforeX = gate.getX();
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			ui.waitFor(() -> soleGate(ui).getX() == beforeX + step,
					"gate nudged one step");
			KeyStroke undo = EditOp.UNDO.accelerator(os);
			ui.pressKeyThroughFocusOwner(undo.getKeyCode(), undo.getModifiers());
			ui.waitFor(() -> soleGate(ui).getX() == beforeX, "nudge undone");

			KeyStroke redo = EditOp.REDO.accelerator(os);
			ui.pressKeyThroughFocusOwner(redo.getKeyCode(), redo.getModifiers());
			ui.waitFor(() -> soleGate(ui).getX() == beforeX + step,
					"redo through the focus owner re-applied the nudge");
		}
	}

	/**
	 * The plain-R rotate key routed through the focus owner rotates the
	 * selected gate - the first end-to-end proof that the rotate binding
	 * reaches its shared Action through the real event path (previously only
	 * object identity was asserted).
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void rotateKeyRotatesThroughFocusOwner() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			String before = save(gate);
			selectGateAndFocusCanvas(ui, gate);

			ui.pressKeyThroughFocusOwner(KeyEvent.VK_R);
			ui.waitFor(() -> !save(soleGate(ui)).equals(before),
					"R through the focus owner rotated the gate");
			assertNotEquals(before, save(soleGate(ui)),
					"rotate changed the gate's persisted form");
		}
	}

	/**
	 * The plain-F flip key routed through the focus owner flips the selected
	 * gate.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void flipKeyFlipsThroughFocusOwner() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			String before = save(gate);
			selectGateAndFocusCanvas(ui, gate);

			ui.pressKeyThroughFocusOwner(KeyEvent.VK_F);
			ui.waitFor(() -> !save(soleGate(ui)).equals(before),
					"F through the focus owner flipped the gate");
			assertNotEquals(before, save(soleGate(ui)),
					"flip changed the gate's persisted form");
		}
	}

	/**
	 * The Delete key routed through the focus owner removes the selection -
	 * the faithful counterpart of the identity-plus-canvas-dispatch Delete
	 * check in {@link EditActionMatrixTest}.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void deleteKeyRemovesThroughFocusOwner() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			selectGateAndFocusCanvas(ui, gate);

			ui.pressKeyThroughFocusOwner(KeyEvent.VK_DELETE);
			ui.waitFor(() -> ui.currentCircuit().getElements().stream()
					.noneMatch(el -> el instanceof AndGate),
					"Delete through the focus owner removed the gate");
		}
	}

	/**
	 * The Ctrl/Cmd+W watch key routed through the real focus owner toggles the
	 * watched state of the selected element - the behavioral proof of the #75
	 * headline overload resolution. Ctrl+W and plain-W used to be the same
	 * stroke doing two jobs; they are now split (plain W starts a wire,
	 * {@link #wireStartAndEscapeThroughFocusOwner} pins that), and this fires
	 * the Ctrl+W half and observes the element's watched flag flip. Nothing
	 * fired Watch before - only object identity was asserted - so a regression
	 * that left Ctrl+W wired to a disabled or no-op Action, or collapsed the
	 * overload back onto one stroke, turns this red.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void watchKeyTogglesWatchedThroughFocusOwner() throws Exception {
		Circuit circuit = onePin();
		String os = System.getProperty("os.name");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			jls.elem.Pin pin = solePin(ui);
			assertTrue(pin.canWatch(), "the input pin is watchable");
			boolean before = pin.isWatched();
			selectGateAndFocusCanvas(ui, pin);

			KeyStroke watch = EditOp.WATCH.accelerator(os);
			ui.pressKeyThroughFocusOwner(watch.getKeyCode(),
					watch.getModifiers());
			ui.waitFor(() -> solePin(ui).isWatched() != before,
					"Ctrl+W through the focus owner toggled the watched flag");
			assertNotEquals(before, solePin(ui).isWatched(),
					"Ctrl+W changed the element's watched state");
		}
	}

	/**
	 * The plain-W wire-start key routed through the focus owner starts a wire
	 * at the keyboard caret, and Escape routed the same way abandons it -
	 * pinning the Ctrl/Cmd+W -&gt; plain-W wire-start move and the cancel key
	 * through the real event path.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void wireStartAndEscapeThroughFocusOwner() throws Exception {
		Circuit circuit = new Circuit("kbd-wire");
		int step = JLSInfo.spacing;
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			ui.focusCanvas();
			assertTrue(ui.canvasIsFocusOwner(), "canvas owns focus");

			// drive the caret to a clear grid point with faithful arrows
			driveCaretFaithful(ui, 20 * step, 10 * step);
			ui.pressKeyThroughFocusOwner(KeyEvent.VK_W);
			ui.waitFor(() -> present(circuit, "jls.elem.WireEnd"),
					"W through the focus owner started a wire");

			ui.pressKeyThroughFocusOwner(KeyEvent.VK_ESCAPE);
			ui.waitFor(() -> !present(circuit, "jls.elem.WireEnd"),
					"Escape through the focus owner abandoned the wire");
		}
	}

	/**
	 * Drive the keyboard caret to a target grid point using only faithful,
	 * focus-owner-routed arrow keys.
	 *
	 * @param ui the harness.
	 * @param tx target x.
	 * @param ty target y.
	 * @throws Exception on EDT failure.
	 */
	private static void driveCaretFaithful(EditorGestureSupport ui, int tx,
			int ty) throws Exception {
		for (int i = 0; i < 600; i++) {
			java.awt.Point c = ui.keyboardCaret();
			if (c != null && c.x == tx && c.y == ty) {
				return;
			}
			if (c == null || c.x > tx) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_LEFT);
			} else if (c.x < tx) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_RIGHT);
			} else if (c.y < ty) {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_DOWN);
			} else {
				ui.pressKeyThroughFocusOwner(KeyEvent.VK_UP);
			}
		}
		throw new AssertionError("caret could not reach (" + tx + "," + ty
				+ "); last at " + ui.keyboardCaret());
	}

	/** Whether the circuit holds an element of the named type. */
	private static boolean present(Circuit circuit, String typeName) {
		Class<?> type;
		try {
			type = Class.forName(typeName);
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sanity guard: the shared rotate/flip/delete/undo/redo Actions the
	 * faithful keys drive are the same objects the popup and menu bar use, so
	 * proving the key path also proves those surfaces. Kept here so a
	 * refactor that re-split a surface fails alongside the behavioral checks.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void faithfulKeysDriveTheSharedActions() throws Exception {
		Circuit circuit = oneGate();
		String os = System.getProperty("os.name");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			for (EditOp op : new EditOp[] { EditOp.ROTATE_CW, EditOp.FLIP,
					EditOp.DELETE, EditOp.UNDO, EditOp.REDO,
					EditOp.WIRE_START }) {
				KeyStroke ks = op.accelerator(os);
				assertNotNull(ui.canvasActionForStroke(ks),
						"canvas binds " + op + " key " + ks);
				assertSame(ui.editor.editAction(op),
						ui.canvasActionForStroke(ks),
						"canvas " + op + " binding is the shared Action");
			}
			// touch a rect so the import is used and the gate is real
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			Rectangle r = gate.getRect();
			assertTrue(r.width > 0 && r.height > 0, "gate has bounds");
			assertTrue(centerX(gate) > 0 && centerY(gate) > 0, "gate centered");
		}
	}
}
