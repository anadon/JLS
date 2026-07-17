package jls.edit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Binder;
import jls.elem.Element;
import jls.elem.Put;
import jls.elem.TriState;
import jls.elem.WireEnd;
import jls.elem.WireNet;

/**
 * Regression tests for the bundle tri-state/normal connect predicate
 * (issue #118, prior art AmityWilder/JLS@e4bbd25).
 *
 * <p>SimpleEditor.canConnect used to classify the incoming, still-dangling
 * wire end's net unconditionally: a freshly drawn wire's net is not yet
 * attached to anything, so isTriState() was false and the end was counted
 * as a *normal* connection -- refusing every attach to a bundle that
 * already had tri-state wires ("Cannot connect both tri-state and normal
 * wires to a bundle"), even when the user was wiring a tri-state bus
 * (issue #118 P1).</p>
 *
 * <p>The tally now lives in SimpleEditor.mixesTriStateAndNormal and only
 * classifies the incoming end when its net has at least one attached end.
 * The guard is net-wide, not the fork's end.isAttached() (which the
 * isDangling gate has already forced false, making that guard skip the
 * classification always): a dangling end whose net already has a normal
 * driver through another, attached end must still be refused on a
 * tri-state bundle (issue #118 P2 / section 9 refinement).</p>
 */
class TriStateBundleConnectTest {

	// fixture element positions (the load-time id map is not retained,
	// so tests locate elements by class and position)
	private static final int TRI_DRIVER_X = 60, TRI_DRIVER_Y = 60;
	private static final int TRI_BUNDLE_X = 240, TRI_BUNDLE_Y = 60;
	private static final int NORM_DANGLING_X = 168, NORM_DANGLING_Y = 252;
	private static final int TRI_DANGLING_X = 168, TRI_DANGLING_Y = 372;
	private static final int NORM_BUNDLE_X = 240, NORM_BUNDLE_Y = 240;

	// element ids in the fixture text (load references only)
	private static final int TRI_DRIVER = 0;	// TriState feeding the tri bundle
	private static final int TRI_BUNDLE = 1;	// Binder with a tri-state wire on input "1"
	private static final int NORM_DRIVER = 4;	// Constant feeding the normal incoming wire
	private static final int NORM_DANGLING = 6;	// dangling end of the Constant's wire
	private static final int TRI_DRIVER_2 = 7;	// TriState feeding the tri incoming wire
	private static final int TRI_DANGLING = 9;	// dangling end of that wire
	private static final int NORM_BUNDLE = 10;	// Binder with a normal wire on input "1"

	/**
	 * The fixture, in the on-disk circuit format (loaded headless):
	 * a Binder with a tri-state wire already attached to input "1" and
	 * input "0" free; a Binder with a normal (Constant-driven) wire
	 * already attached to input "1"; and two candidate incoming wires,
	 * each with one attached end and one dangling end -- one driven by a
	 * Constant (normal net) and one by a TriState (tri-state net).
	 */
	private static String fixture() {
		StringBuilder t = new StringBuilder("CIRCUIT bundles\n");

		// tri-state bundle: TriState output -> Binder input "1"
		t.append("ELEMENT TriState\n int id ").append(TRI_DRIVER)
				.append("\n int x 60\n int y 60\n int bits 1\n int delay 10\n")
				.append(" String Gorient \"RIGHT\"\n String Corient \"DOWN\"\nEND\n");
		t.append("ELEMENT Binder\n int id ").append(TRI_BUNDLE)
				.append("\n int x 240\n int y 60\n int bits 2\n")
				.append(" String orient \"RIGHT\"\n String noncontig \"true\"\n")
				.append(" int tristate 1\n pair 0 0\n pair 1 1\nEND\n");
		t.append(wire(2, TRI_DRIVER, "output", 3, TRI_BUNDLE, "1", true, 120, 72));

		// normal incoming wire: Constant output -> dangling end
		t.append("ELEMENT Constant\n int id ").append(NORM_DRIVER)
				.append("\n int x 60\n int y 240\n int width 24\n int height 24\n")
				.append(" Int value 1\n int base 10\n String orient \"RIGHT\"\nEND\n");
		t.append(wire(5, NORM_DRIVER, "output", NORM_DANGLING, -1, null, false, 120, 252));

		// tri-state incoming wire: TriState output -> dangling end
		t.append("ELEMENT TriState\n int id ").append(TRI_DRIVER_2)
				.append("\n int x 60\n int y 360\n int bits 1\n int delay 10\n")
				.append(" String Gorient \"RIGHT\"\n String Corient \"DOWN\"\nEND\n");
		t.append(wire(8, TRI_DRIVER_2, "output", TRI_DANGLING, -1, null, true, 120, 372));

		// normal bundle: Constant output -> Binder input "1"
		t.append("ELEMENT Binder\n int id ").append(NORM_BUNDLE)
				.append("\n int x 240\n int y 240\n int bits 2\n")
				.append(" String orient \"RIGHT\"\n String noncontig \"true\"\n")
				.append(" int tristate 0\n pair 0 0\n pair 1 1\nEND\n");
		t.append("ELEMENT Constant\n int id 11\n int x 420\n int y 240\n")
				.append(" int width 24\n int height 24\n Int value 1\n int base 10\n")
				.append(" String orient \"RIGHT\"\nEND\n");
		t.append(wire(12, 11, "output", 13, NORM_BUNDLE, "1", false, 456, 252));

		t.append("ENDCIRCUIT\n");
		return t.toString();
	}

	/**
	 * A two-end wire in the file format. End A (id) attaches to fromPut of
	 * fromElement; end B (id+... given explicitly) attaches to toPut of
	 * toElement, or dangles if toElement is negative. Saved tri-state
	 * nets carry "int tristate 1" on their wire ends, exactly as
	 * WireEnd.save writes them.
	 */
	private static String wire(int endA, int fromElement, String fromPut,
			int endB, int toElement, String toPut, boolean triState,
			int x, int y) {
		StringBuilder t = new StringBuilder();
		t.append("ELEMENT WireEnd\n int id ").append(endA)
				.append("\n int x ").append(x).append("\n int y ").append(y);
		if (triState)
			t.append("\n int tristate 1");
		t.append("\n String put \"").append(fromPut)
				.append("\"\n ref attach ").append(fromElement)
				.append("\n ref wire ").append(endB).append("\nEND\n");
		t.append("ELEMENT WireEnd\n int id ").append(endB)
				.append("\n int x ").append(x + 48)
				.append("\n int y ").append(y);
		if (triState)
			t.append("\n int tristate 1");
		if (toElement >= 0)
			t.append("\n String put \"").append(toPut)
					.append("\"\n ref attach ").append(toElement);
		t.append("\n ref wire ").append(endA).append("\nEND\n");
		return t.toString();
	}

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("bundles");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		// Binder.init sizes itself from font metrics, so finishLoad gets
		// an offscreen Graphics (headless-safe), as in AllElementsRoundTripTest
		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		boolean ok = circuit.finishLoad(g);
		g.dispose();
		assertTrue(ok, () -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/** The fixture element of the given class at the given position. */
	private static <T extends Element> T elementAt(Circuit circuit,
			Class<T> type, int x, int y) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el) && el.getX() == x && el.getY() == y)
				return type.cast(el);
		}
		assertNotNull(null, "fixture must contain a " + type.getSimpleName()
				+ " at (" + x + "," + y + ")");
		return null; // unreachable
	}

	/** The free input put ("0") of a bundle. */
	private static Put freeInput(Circuit circuit, int x, int y) {
		Element binder = elementAt(circuit, Binder.class, x, y);
		Put put = binder.getPut("0");
		assertNotNull(put, "binder must have an input named \"0\"");
		assertFalse(put.isAttached(), "input \"0\" must be free");
		return put;
	}

	/**
	 * A freshly drawn wire end: exactly what SimpleEditor's mousePressed
	 * creates when a wire drag starts -- a dangling end in a brand-new,
	 * unattached net.
	 */
	private static WireEnd freshEnd(Circuit circuit) {
		WireEnd end = new WireEnd(circuit);
		end.setXY(228, 72);
		end.init(circuit);
		circuit.addElement(end);
		WireNet net = new WireNet();
		net.add(end);
		end.setNet(net);
		return end;
	}

	/** A dangling wire end loaded from the fixture, sanity-checked. */
	private static WireEnd danglingEnd(Circuit circuit, int x, int y,
			boolean expectTriState) {
		WireEnd end = elementAt(circuit, WireEnd.class, x, y);
		assertTrue(end.isDangling(),
				"end at (" + x + "," + y + ") must be dangling");
		assertTrue(end.isTriState() == expectTriState,
				"end at (" + x + "," + y + ") net tri-state must be "
						+ expectTriState);
		return end;
	}

	// ------------------------------------------------------------------
	// P1: a fresh wire may attach to a tri-state bundle
	// ------------------------------------------------------------------

	/**
	 * Issue #118 P1. Pre-fix, the fresh end's unattached net reported
	 * isTriState() == false, was tallied as a normal connection, and the
	 * attach was refused.
	 */
	@Test
	void freshWireMayAttachToTriStateBundle() throws Exception {
		Circuit circuit = load(fixture());
		Put free = freeInput(circuit, TRI_BUNDLE_X, TRI_BUNDLE_Y);

		// the bundle really does have a tri-state wire attached
		Put attached = elementAt(circuit, Binder.class, TRI_BUNDLE_X, TRI_BUNDLE_Y).getPut("1");
		assertTrue(attached.isAttached() && attached.getWireEnd().isTriState(),
				"fixture: input \"1\" must have a tri-state wire");

		assertFalse(SimpleEditor.mixesTriStateAndNormal(freshEnd(circuit), free),
				"a freshly drawn wire's unattached net says nothing about"
						+ " the connection being made and must not be"
						+ " counted as a normal connection (#118 P1)");
	}

	/** The symmetric case: a fresh wire may also attach to a normal bundle. */
	@Test
	void freshWireMayAttachToNormalBundle() throws Exception {
		Circuit circuit = load(fixture());
		Put free = freeInput(circuit, NORM_BUNDLE_X, NORM_BUNDLE_Y);
		assertFalse(SimpleEditor.mixesTriStateAndNormal(freshEnd(circuit), free),
				"a freshly drawn wire must attach to a normal bundle");
	}

	// ------------------------------------------------------------------
	// P2: genuinely mixed connections are still refused
	// ------------------------------------------------------------------

	/**
	 * Issue #118 P2, the H1 refutation probe (section 9): the incoming
	 * end is dangling, but its net already has a normal driver through
	 * its other, attached end -- so it must still be refused on a
	 * tri-state bundle. The fork's end.isAttached() guard gets this
	 * wrong: after the isDangling gate it never counts the incoming net
	 * at all.
	 */
	@Test
	void normalDrivenWireIsStillRefusedOnTriStateBundle() throws Exception {
		Circuit circuit = load(fixture());
		Put free = freeInput(circuit, TRI_BUNDLE_X, TRI_BUNDLE_Y);
		WireEnd end = danglingEnd(circuit, NORM_DANGLING_X, NORM_DANGLING_Y, false);
		assertTrue(SimpleEditor.mixesTriStateAndNormal(end, free),
				"a wire whose net has an attached normal driver must be"
						+ " refused on a tri-state bundle (#118 P2)");
	}

	/** And the mirror image: a tri-state net on a normal bundle is refused. */
	@Test
	void triStateWireIsStillRefusedOnNormalBundle() throws Exception {
		Circuit circuit = load(fixture());
		Put free = freeInput(circuit, NORM_BUNDLE_X, NORM_BUNDLE_Y);
		WireEnd end = danglingEnd(circuit, TRI_DANGLING_X, TRI_DANGLING_Y, true);
		assertTrue(SimpleEditor.mixesTriStateAndNormal(end, free),
				"a wire whose net has an attached tri-state driver must be"
						+ " refused on a normal bundle");
	}

	// ------------------------------------------------------------------
	// like-to-like attaches stay allowed
	// ------------------------------------------------------------------

	@Test
	void triStateWireMayAttachToTriStateBundle() throws Exception {
		Circuit circuit = load(fixture());
		Put free = freeInput(circuit, TRI_BUNDLE_X, TRI_BUNDLE_Y);
		WireEnd end = danglingEnd(circuit, TRI_DANGLING_X, TRI_DANGLING_Y, true);
		assertFalse(SimpleEditor.mixesTriStateAndNormal(end, free),
				"tri-state wire on tri-state bundle must be allowed");
	}

	@Test
	void normalWireMayAttachToNormalBundle() throws Exception {
		Circuit circuit = load(fixture());
		Put free = freeInput(circuit, NORM_BUNDLE_X, NORM_BUNDLE_Y);
		WireEnd end = danglingEnd(circuit, NORM_DANGLING_X, NORM_DANGLING_Y, false);
		assertFalse(SimpleEditor.mixesTriStateAndNormal(end, free),
				"normal wire on normal bundle must be allowed");
	}

	// ------------------------------------------------------------------
	// non-bundle puts are out of this predicate's scope
	// ------------------------------------------------------------------

	@Test
	void nonGroupPutsNeverMix() throws Exception {
		Circuit circuit = load(fixture());
		Element tri = elementAt(circuit, TriState.class, TRI_DRIVER_X, TRI_DRIVER_Y);
		Put input = tri.getPut("input");
		assertNotNull(input, "TriState must have an input put");
		WireEnd end = danglingEnd(circuit, NORM_DANGLING_X, NORM_DANGLING_Y, false);
		assertFalse(SimpleEditor.mixesTriStateAndNormal(end, input),
				"the mixed-connection rule applies only to bundles");
	}
}
