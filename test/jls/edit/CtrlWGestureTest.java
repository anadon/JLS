package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.edit.SimpleEditor.CtrlW;
import jls.edit.SimpleEditor.State;
import jls.edit.SimpleEditor.WireStart;
import jls.elem.Constant;
import jls.elem.Element;
import jls.elem.WireEnd;

/**
 * Regression tests for issue #126: an idle-state ctrl-W starts a wire
 * regardless of the current (hover) selection, clearing that selection
 * as part of the gesture.
 *
 * <p>The gesture is pinned at two seams SimpleEditor exposes for exactly
 * this purpose: the pure dispatch function
 * {@link SimpleEditor#ctrlWGesture} and the Swing-free model half
 * {@link SimpleEditor#startWireGesture}. This is the compensating
 * control for the key-press-to-pixels path (the ToolkitPolicyTest
 * pattern): the editor itself cannot be constructed under the suite's
 * {@code -Djava.awt.headless=true}, because EditWindow's constructor
 * calls {@code Toolkit.getMenuShortcutKeyMaskEx()}, which throws
 * HeadlessException without a display.</p>
 */
class CtrlWGestureTest {

	/** A one-constant circuit, loaded headless like the other edit tests. */
	private static Circuit oneConstant() throws Exception {
		String text = "CIRCUIT one\n"
				+ "ELEMENT Constant\n int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n Int value 1\n int base 10\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ENDCIRCUIT\n";
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/** The circuit's constant, hover-selected the way idle mouse motion
	 * does it: highlighted and in the selection set. */
	private static Element hoverSelect(Circuit circuit, Set<Element> selected) {
		Element constant = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof Constant)
				constant = el;
		}
		assertNotNull(constant, "fixture must contain the constant");
		constant.setHighlight(true);
		selected.add(constant);
		return constant;
	}

	// ------------------------------------------------------------------
	// dispatch: the #126 gate itself
	// ------------------------------------------------------------------

	/** P1's dispatch half: with an element hover-selected, idle ctrl-W
	 * must start a wire. Pre-#126 the empty-selection gate sent this to
	 * the watch toggle instead. */
	@Test
	void idleStartsWireDespiteSingleSelection() {
		assertEquals(CtrlW.START_WIRE, SimpleEditor.ctrlWGesture(State.idle, 1));
	}

	/** Pre-#126, an idle multi-selection (overlapping hover) fell through
	 * to nothing at all. */
	@Test
	void idleStartsWireDespiteMultiSelection() {
		assertEquals(CtrlW.START_WIRE, SimpleEditor.ctrlWGesture(State.idle, 3));
	}

	/** The original gesture — idle, nothing selected — is unchanged. */
	@Test
	void idleStartsWireWithEmptySelection() {
		assertEquals(CtrlW.START_WIRE, SimpleEditor.ctrlWGesture(State.idle, 0));
	}

	/** P2's sibling-gesture pin: the watch toggle is still reachable with
	 * one element selected outside idle (e.g. after a select-rectangle,
	 * State.selected). */
	@Test
	void watchToggleStillReachableFromSelectedState() {
		assertEquals(CtrlW.TOGGLE_WATCH,
				SimpleEditor.ctrlWGesture(State.selected, 1));
	}

	/** Everything else stays a no-op, exactly as before. */
	@Test
	void otherStatesDoNothing() {
		assertEquals(CtrlW.NONE, SimpleEditor.ctrlWGesture(State.selected, 0));
		assertEquals(CtrlW.NONE, SimpleEditor.ctrlWGesture(State.selected, 2));
		assertEquals(CtrlW.NONE, SimpleEditor.ctrlWGesture(State.drawire, 0));
		assertEquals(CtrlW.NONE, SimpleEditor.ctrlWGesture(State.moving, 4));
	}

	// ------------------------------------------------------------------
	// model half: what starting the wire does to circuit and selection
	// ------------------------------------------------------------------

	/** P1's model half: starting a wire over a hover selection clears it
	 * (including its highlight) and leaves the new wire end, at the
	 * requested spot, as the sole selection, in the circuit, on a fresh
	 * net. */
	@Test
	void startWireClearsSelectionAndSelectsNewEnd() throws Exception {
		Circuit circuit = oneConstant();
		Set<Element> selected = new HashSet<Element>();
		Element constant = hoverSelect(circuit, selected);

		WireStart start = SimpleEditor.startWireGesture(circuit, selected,
				120, 60);

		// the old selection is gone, unhighlighted
		assertFalse(selected.contains(constant),
				"hover selection must be cleared");
		assertFalse(circuit.getHighlighted().contains(constant),
				"hover highlight must be cleared");

		// the new wire end is the selection, in the circuit, where asked
		assertEquals(1, selected.size(), "wire end must be sole selection");
		assertTrue(selected.contains(start.end));
		assertTrue(circuit.getElements().contains(start.end),
				"wire end must be added to the circuit");
		assertEquals(120, start.end.getX());
		assertEquals(60, start.end.getY());

		// and it is on a fresh net of its own
		assertNotNull(start.end.getNet(), "wire end must get a net");
		assertEquals(Set.of(start.end), start.end.getNet().getAllEnds());
	}

	/** The overlap-feedback key must reflect the selection before the
	 * clear. Keyed off the selection afterwards it would always be true
	 * (the new wire end becomes the selection) — the exact pitfall the
	 * fork fixed in AmityWilder/JLS@b1f1573. */
	@Test
	void overlapFeedbackKeyedOffPreClearSelection() throws Exception {
		Circuit circuit = oneConstant();

		// with a hover selection: feedback wanted
		Set<Element> selected = new HashSet<Element>();
		hoverSelect(circuit, selected);
		assertTrue(SimpleEditor.startWireGesture(circuit, selected, 60, 60)
				.hadSelection, "a cleared selection must request feedback");

		// without one: no feedback, even though the selection is
		// non-empty (it holds the new wire end) after the call
		Set<Element> empty = new HashSet<Element>();
		WireStart start = SimpleEditor.startWireGesture(circuit, empty,
				180, 60);
		assertTrue(empty.contains(start.end));
		assertFalse(start.hadSelection,
				"an empty selection must not request feedback");
	}

	/** Starting a wire from empty selection behaves byte-for-byte like
	 * the pre-#126 code path: end created, selected, netted. */
	@Test
	void startWireFromEmptySelectionMatchesOldBehavior() throws Exception {
		Circuit circuit = oneConstant();
		Set<Element> selected = new HashSet<Element>();

		WireStart start = SimpleEditor.startWireGesture(circuit, selected,
				240, 120);

		assertEquals(1, selected.size());
		assertTrue(selected.contains(start.end));
		assertTrue(circuit.getElements().contains(start.end));
		assertTrue(start.end instanceof WireEnd);
		assertEquals(Set.of(start.end), start.end.getNet().getAllEnds());
	}
}
