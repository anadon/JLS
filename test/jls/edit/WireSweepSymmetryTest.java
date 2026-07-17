package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.elem.Constant;
import jls.elem.Element;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * Regression tests for the wire-drag collision asymmetry closed by
 * SimpleEditor.wireCollidesAlongSpan.
 *
 * <p>Dragging an element into a wire body has always been an overlap:
 * the main overlap() loop calls sel.intersects(wire), which line-tests
 * the wire against the element's rectangle. But the wire-drag direction
 * only checked the dragged wire ends and wire-end landings -- the wire
 * BODY sweeping across a stationary element was never tested, so the
 * same forbidden geometry (a wire crossing an element) could be created
 * by dragging the wire but not by dragging the element.</p>
 *
 * <p>wireCollidesAlongSpan closes this with the exact predicate the
 * reverse direction uses (elm.intersects(wire)), so the two directions
 * cannot disagree; each test here asserts that agreement explicitly.
 * Deliberately preserved behavior is pinned too: wire-over-wire
 * crossings stay legal, a wire never collides with an element an end of
 * it sits on (so attached wires do not collide with their own element),
 * landing on a stationary wire end still flags, and elements moving
 * rigidly with the selection are skipped.</p>
 */
class WireSweepSymmetryTest {

	/** A circuit containing one Constant block at (240,120), 24x24. */
	private static Circuit withConstant() throws Exception {
		String text = "CIRCUIT sweep\n"
				+ "ELEMENT Constant\n int id 0\n int x 240\n int y 120\n"
				+ " int width 24\n int height 24\n Int value 1\n int base 10\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ENDCIRCUIT\n";
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)), "fixture must load");
		assertTrue(circuit.finishLoad(null), "fixture must finish loading");
		return circuit;
	}

	private static Constant constantIn(Circuit circuit) {
		for (Element el : circuit.getElements()) {
			if (el instanceof Constant) {
				return (Constant) el;
			}
		}
		throw new AssertionError("fixture has a Constant");
	}

	/** A dangling wire between two free ends, added to the circuit. */
	private static Wire wire(Circuit circuit, int x1, int y1, int x2, int y2) {
		WireEnd e1 = new WireEnd(circuit);
		e1.setXY(x1, y1);
		WireEnd e2 = new WireEnd(circuit);
		e2.setXY(x2, y2);
		Wire w = new Wire(e1, e2);
		e1.addWire(w);
		e2.addWire(w);
		circuit.addElement(e1);
		circuit.addElement(e2);
		circuit.addElement(w);
		return w;
	}

	private static Set<Element> endsOf(Wire w) {
		Set<Element> selected = new HashSet<Element>();
		WireEnd e1 = w.getEnd();
		selected.add(e1);
		selected.add(w.getOtherEnd(e1));
		return selected;
	}

	/**
	 * A wire body crossing a stationary element collides -- and by the
	 * SAME verdict the reverse drag direction (element into wire) has
	 * always produced.
	 */
	@Test
	void wireSweepingAcrossElementCollidesLikeTheReverseDrag()
			throws Exception {
		Circuit circuit = withConstant();
		Constant block = constantIn(circuit);
		Wire w = wire(circuit, 180, 132, 324, 132); // crosses the block

		boolean reverse = block.intersects(w);
		boolean forward = SimpleEditor.wireCollidesAlongSpan(circuit,
				endsOf(w), w.getEnd(), w);

		assertTrue(reverse, "element dragged into the wire overlaps");
		assertTrue(forward, "wire dragged across the element overlaps too");
		assertEquals(reverse, forward, "the two drag directions must agree");
	}

	/**
	 * A wire body clear of the element collides in neither direction.
	 */
	@Test
	void clearWireCollidesInNeitherDirection() throws Exception {
		Circuit circuit = withConstant();
		Constant block = constantIn(circuit);
		Wire w = wire(circuit, 180, 180, 324, 180); // passes below the block

		boolean reverse = block.intersects(w);
		boolean forward = SimpleEditor.wireCollidesAlongSpan(circuit,
				endsOf(w), w.getEnd(), w);

		assertFalse(reverse, "no overlap dragging the element");
		assertFalse(forward, "no overlap dragging the wire");
		assertEquals(reverse, forward, "the two drag directions must agree");
	}

	/**
	 * Wires crossing wires stay legal: schematic wires cross freely
	 * without connecting, in both directions before and after the fix.
	 */
	@Test
	void wireCrossingWireStaysLegal() throws Exception {
		Circuit circuit = new Circuit("crossings");
		Wire horizontal = wire(circuit, 480, 132, 600, 132);
		Wire vertical = wire(circuit, 540, 72, 540, 192); // crosses it

		assertFalse(SimpleEditor.wireCollidesAlongSpan(circuit,
				endsOf(vertical), vertical.getEnd(), vertical),
				"dragging a wire across another wire is not an overlap");
		assertFalse(SimpleEditor.wireCollidesAlongSpan(circuit,
				endsOf(horizontal), horizontal.getEnd(), horizontal),
				"nor from the other wire's point of view");
	}

	/**
	 * A wire hanging off an element (an end on or inside the element's
	 * rectangle, as attached wires are) does not collide with that
	 * element -- Wire.intersects excludes wires whose endpoint the
	 * rectangle contains, in both drag directions alike.
	 */
	@Test
	void wireHangingOffAnElementDoesNotCollideWithIt() throws Exception {
		Circuit circuit = withConstant();
		Constant block = constantIn(circuit);
		Wire w = wire(circuit, 252, 132, 360, 132); // end inside the block

		boolean reverse = block.intersects(w);
		boolean forward = SimpleEditor.wireCollidesAlongSpan(circuit,
				endsOf(w), w.getEnd(), w);

		assertFalse(reverse, "an element ignores wires ending on it");
		assertFalse(forward, "and the wire ignores the element it hangs off");
		assertEquals(reverse, forward, "the two drag directions must agree");
	}

	/**
	 * The pre-existing wire-end landing check is preserved: a wire body
	 * dragged onto a stationary free wire end still collides.
	 */
	@Test
	void landingOnAStationaryWireEndStillCollides() throws Exception {
		Circuit circuit = new Circuit("landing");
		Wire w = wire(circuit, 660, 132, 780, 132);
		WireEnd stationary = new WireEnd(circuit);
		stationary.setXY(720, 132); // mid-span
		circuit.addElement(stationary);

		assertTrue(SimpleEditor.wireCollidesAlongSpan(circuit,
				endsOf(w), w.getEnd(), w),
				"a wire dragged onto a stationary wire end overlaps");
	}

	/**
	 * Elements in the moving selection are skipped: they move rigidly
	 * with the wire, exactly as the main overlap() candidate loop skips
	 * them, so a group drag does not collide with itself.
	 */
	@Test
	void elementsMovingWithTheSelectionAreSkipped() throws Exception {
		Circuit circuit = withConstant();
		Constant block = constantIn(circuit);
		Wire w = wire(circuit, 180, 132, 324, 132); // crosses the block

		Set<Element> selected = endsOf(w);
		selected.add(block); // the block moves with the wire

		assertFalse(SimpleEditor.wireCollidesAlongSpan(circuit,
				selected, w.getEnd(), w),
				"a selected element moving with the wire is not an overlap");
	}
}
