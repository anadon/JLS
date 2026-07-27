package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.Scanner;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.elem.AndGate;
import jls.elem.Constant;
import jls.elem.Element;

/**
 * Layer-2 editor gesture characterization (issues #91 and #84). Each
 * test loads a circuit into a real {@link jls.edit.Editor} and drives
 * the SimpleEditor mouse state machine with synthetic events - move,
 * rubber-band select, right-click delete, undo/redo - then asserts the
 * resulting circuit model. These are the safety net for the #84
 * decomposition: they touch only surfaces that survive it (canvas
 * events, popup item texts, the circuit model), so the decomposition is
 * correct exactly when this suite stays green.
 *
 * Tagged {@code display}: runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false), self-skips
 * headless. Fast and deterministic - no Robot, no getMousePosition.
 */
@Tag("display")
@Timeout(60)
class EditorGestureTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/** A circuit with a single AND gate near the top-left of the canvas. */
	private static Circuit oneGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static int centerX(Element el) {
		return el.getX() + el.getRect().width() / 2;
	}

	private static int centerY(Element el) {
		return el.getY() + el.getRect().height() / 2;
	}

	@Test
	void pressAndDragMovesAnElement() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int fromX = centerX(gate), fromY = centerY(gate);
			int beforeX = gate.getX(), beforeY = gate.getY();

			ui.leftDrag(fromX, fromY, fromX + 120, fromY + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved");
			GeometryAssert.assertOnGrid(gate);
			// the move must land near the drag delta (grid-snapped)
			assertTrue(Math.abs(gate.getX() - (beforeX + 120)) <= 12
					&& Math.abs(gate.getY() - (beforeY + 96)) <= 12,
					"gate moved to roughly the drop point: "
							+ gate.getX() + "," + gate.getY());
		}
	}

	@Test
	void rubberBandSelectHighlightsEnclosedElements() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			// drag a rectangle from empty space fully around the gate
			int x0 = gate.getX() - 40, y0 = gate.getY() - 40;
			int x1 = gate.getX() + gate.getRect().width() + 40;
			int y1 = gate.getY() + gate.getRect().height() + 40;
			ui.leftDrag(x0, y0, x1, y1);
			ui.waitFor(gate::isHighlighted,
					"gate highlighted by rubber-band select");
		}
	}

	@Test
	void rightClickDeleteRemovesTheElement() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			ui.rightPress(centerX(gate), centerY(gate));
			ui.clickPopupItem("Delete");
			ui.waitFor(() -> circuit.getElements().stream()
					.noneMatch(el -> el instanceof AndGate),
					"gate deleted");
		}
	}

	@Test
	void undoRestoresADeletedElementAndRedoRemovesItAgain()
			throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			ui.rightPress(centerX(gate), centerY(gate));
			ui.clickPopupItem("Delete");
			ui.waitFor(() -> circuit.getElements().stream()
					.noneMatch(el -> el instanceof AndGate), "gate deleted");

			// undo lives on the empty-canvas popup; open it well away
			// from where the gate was. Undo swaps in a fresh circuit
			// instance, so observe the editor's live circuit, not the
			// stale reference this test was built with.
			ui.rightPress(700, 600);
			ui.clickPopupItem("Undo");
			ui.waitFor(() -> ui.currentCircuit().getElements().stream()
					.anyMatch(el -> el instanceof AndGate),
					"gate restored by undo");

			ui.rightPress(700, 600);
			ui.clickPopupItem("Redo");
			ui.waitFor(() -> ui.currentCircuit().getElements().stream()
					.noneMatch(el -> el instanceof AndGate),
					"gate removed again by redo");
		}
	}

	@Test
	void movingOneOfTwoElementsLeavesTheOtherPut() throws Exception {
		// hand-positioned so the two elements are well separated - the
		// builder's auto-layout packs them 24px apart, which would let a
		// press aimed at one land on the other
		String text = "CIRCUIT golden\n"
				+ "ELEMENT AndGate\n int id 0\n int x 300\n int y 300\n"
				+ " int width 48\n int height 24\n int bits 1\n"
				+ " int numInputs 2\n String orientation \"right\"\n"
				+ " int delay 10\nEND\n"
				+ "ELEMENT Constant\n int id 1\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n Int value 5\n"
				+ " int base 10\n String orient \"RIGHT\"\nEND\n"
				+ "ENDCIRCUIT\n";
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);

		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			Constant constant = assertElementPresent(circuit, Constant.class);
			int constX = constant.getX(), constY = constant.getY();

			ui.leftDrag(centerX(gate), centerY(gate),
					centerX(gate) + 96, centerY(gate) + 120);
			ui.waitFor(() -> gate.getY() != constY || gate.getX() != constX
					|| gate.getX() != constX, "gate moved");
			assertEquals(constX, constant.getX(),
					"the un-dragged constant must not move in x");
			assertEquals(constY, constant.getY(),
					"the un-dragged constant must not move in y");
			assertNotEquals(gate.getX() + "," + gate.getY(),
					constX + "," + constY, "the gate did move");
		}
	}

	/**
	 * The delete-selection gesture end-to-end through the op path
	 * (issue #167): rubber-band select a wired pair - the selection the
	 * plan builder maps to RemoveWire + RemoveElements - press the
	 * delete key, and everything goes; ONE undo restores the whole
	 * selection, pinning the one-undo-snapshot-per-gesture batch
	 * submit (a fragmented submit would need one undo per op). Byte
	 * parity of the op path against the inline path is pinned
	 * headlessly by jls.edit.DeleteGestureTest; bytes are not compared
	 * here because undo's restore re-runs display init, which sets
	 * layout fields (element width/height) the pre-gesture circuit
	 * only gains once it has been drawn.
	 */
	@Test
	void deleteKeyRemovesAWiredSelectionAndOneUndoRestoresIt()
			throws Exception {
		String text = "CIRCUIT golden\n"
				+ "ELEMENT InputPin\n int id 0\n int x 120\n int y 120\n"
				+ " String name \"A\"\n int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 1\n int x 360\n int y 120\n"
				+ " String name \"B\"\n int bits 1\n int watch 0\n"
				+ " String orient \"LEFT\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 2\n int x 180\n int y 120\n"
				+ " String put \"output\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n int id 3\n int x 300\n int y 120\n"
				+ " String put \"input\"\n ref attach 1\n ref wire 2\n"
				+ "END\nENDCIRCUIT\n";
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);

		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			assertTrue(circuit.getElements().stream()
					.anyMatch(el -> el instanceof jls.elem.Wire),
					"the fixture must start wired");

			// Select All (canvas popup), then delete. A rubber band is
			// deliberately not used: what it catches depends on
			// screen-to-circuit offsets, and a partial catch (say, just
			// the wire) still plans and applies cleanly - it just is
			// not the pins-plus-net composition this test pins.
			ui.rightPress(700, 600);
			ui.clickPopupItem("Select All");
			ui.waitFor(() -> circuit.getElements().stream()
					.filter(Element::isHighlighted).count() >= 3,
					"select-all highlighted the pins and the wire");

			int beforeCount = circuit.getElements().size();
			ui.pressKey(java.awt.event.KeyEvent.VK_DELETE);
			ui.waitFor(() -> ui.currentCircuit().getElements().isEmpty(),
					"the wired selection deleted in one gesture");

			// ONE undo restores everything: the whole plan was one
			// snapshot, not one per op
			ui.rightPress(700, 600);
			ui.clickPopupItem("Undo");
			ui.waitFor(() -> ui.currentCircuit().getElements().size()
					== beforeCount, "one undo restored every element");
			Circuit restored = ui.currentCircuit();
			assertTrue(restored.getElements().stream()
					.anyMatch(el -> el instanceof jls.elem.Wire),
					"the wire net came back with the same undo");
			assertTrue(restored.getElements().stream()
					.anyMatch(el -> el instanceof jls.elem.InputPin),
					"the input pin came back with the same undo");
			assertTrue(restored.getElements().stream()
					.anyMatch(el -> el instanceof jls.elem.OutputPin),
					"the output pin came back with the same undo");
		}
	}

	/**
	 * The move-selection drag commit end-to-end through the op path
	 * (issue #167): a lone gate dragged into empty space is a pure
	 * relocation, so the plan builder maps it to one MoveElements and
	 * the drop lands at the drag delta - matching the inline commit,
	 * whose byte parity is pinned headlessly by
	 * jls.edit.MoveGestureTest. A single undo then restores the
	 * pre-drag position, pinning the one-undo-snapshot-per-gesture
	 * batch submit. Positions rather than full bytes are asserted for
	 * the reason the delete test notes: undo's restore re-runs display
	 * init, which sets layout fields the pre-gesture circuit only gains
	 * once drawn.
	 */
	@Test
	void dragMovesAnElementAndOneUndoRestoresIt() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			int fromX = centerX(gate), fromY = centerY(gate);

			ui.leftDrag(fromX, fromY, fromX + 120, fromY + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved by the drag");
			GeometryAssert.assertOnGrid(gate);
			assertTrue(Math.abs(gate.getX() - (beforeX + 120)) <= 12
					&& Math.abs(gate.getY() - (beforeY + 96)) <= 12,
					"the op drop lands at the drag delta, like the inline "
							+ "commit: " + gate.getX() + "," + gate.getY());

			// one undo restores the pre-drag position; the drag was one
			// snapshot, so a single undo suffices. Undo swaps in a fresh
			// circuit, so read the editor's live circuit.
			ui.rightPress(700, 600);
			ui.clickPopupItem("Undo");
			ui.waitFor(() -> {
				AndGate g = assertElementPresent(
						ui.currentCircuit(), AndGate.class);
				return g.getX() == beforeX && g.getY() == beforeY;
			}, "one undo restored the pre-drag position");
		}
	}

}
