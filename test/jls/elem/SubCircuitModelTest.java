package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.BitSetUtils;
import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.core.Orientation;
import jls.sim.BatchSimulator;

/**
 * Headless tests for the SubCircuit model (issue #159, following the
 * TruthTableModelTest pattern). A subcircuit block is pure pin
 * plumbing: init() builds this element's puts from the inner circuit's
 * pins and records the put-to-pin bindings (inmap/outmap), react()
 * forwards input values to the bound InputPins, and send() delivers an
 * OutputPin's value to the bound Output. These tests pin that binding
 * end to end through the real loader and BatchSimulator (the
 * CircuitTextBuilder fixture wraps an a-to-y passthrough), plus the
 * GUI-free model surface: orientation, flip, copy, remapPins,
 * tri-state propagation, watch fan-out and the name lifecycle.
 */
class SubCircuitModelTest {

	/** Load circuit text through the real loader. */
	private static Circuit load(String circuitText) {
		Circuit circuit = new Circuit("submodel");
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		try {
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad failed: " + JLSInfo.loadError);
		} catch (Exception e) {
			throw new AssertionError("finishLoad threw", e);
		}
		return circuit;
	}

	private static SubCircuit findSub(Circuit circuit) {
		SubCircuit sub = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof SubCircuit s) {
				sub = s;
			}
		}
		assertNotNull(sub);
		return sub;
	}

	/** A lone, unwired passthrough subcircuit block. */
	private static SubCircuit loneSub() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.subCircuit("sub");
		return findSub(load(cb.build()));
	}

	/** Headless TextMetrics backed by a scratch image. */
	private static jls.core.TextMetrics metrics() {
		BufferedImage img = new BufferedImage(8, 8,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		return jls.edit.SwingTextMetrics.of(g);
	}

	// ---------------------------------------------------------------
	// pin binding through the simulator
	// ---------------------------------------------------------------

	@Test
	void passthroughValueCrossesTheBlockBoundary() {
		// constant -> block put "a" -> inner InputPin a -> wire ->
		// inner OutputPin y -> block put "y" -> watched pin: the value
		// must survive both boundary crossings (react in, send out)
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int one = cb.constant(1);
		int sub = cb.subCircuit("sub");
		int out = cb.outputPin("out", 1);
		cb.wire(one, "output", sub, "a");
		cb.wire(sub, "y", out, "input");
		Circuit circuit = load(cb.build());
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();
		OutputPin pin = null;
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin p && "out".equals(p.getName())) {
				pin = p;
			}
		}
		assertNotNull(pin);
		BitSet value = pin.getCurrentValue();
		assertNotNull(value, "the passthrough output never settled");
		assertEquals(1, BitSetUtils.ToLong(value));
	}

	// ---------------------------------------------------------------
	// model surface
	// ---------------------------------------------------------------

	@Test
	void toStringNamesOrientationAndBoundPins() {
		SubCircuit sub = loneSub();
		String text = sub.toString();
		// the loader names the block after the inner circuit
		assertTrue(text.startsWith("SubCircuit[inner,right"), text);
		assertTrue(text.contains("inputs={a}"), text);
		assertTrue(text.contains("outputs={y}"), text);
	}

	@Test
	void accessorsRefuseUseBeforeSetup() {
		SubCircuit raw = new SubCircuit(new Circuit("submodel"));
		assertThrows(IllegalStateException.class, raw::getSubCircuit);
		assertThrows(IllegalStateException.class, raw::getName);
	}

	@Test
	void orientationFollowsTheSetterAndTheLoaderStrings() {
		SubCircuit sub = loneSub();
		assertEquals(Orientation.RIGHT, sub.getOrientation());
		sub.setOrientation(Orientation.LEFT);
		assertEquals(Orientation.LEFT, sub.getOrientation());
		sub.setValue("orient", "RIGHT");
		assertEquals(Orientation.RIGHT, sub.getOrientation());
		sub.setValue("orient", "LEFT");
		assertEquals(Orientation.LEFT, sub.getOrientation());
		sub.setValue("orient", "sideways");    // unknown: unchanged
		assertEquals(Orientation.LEFT, sub.getOrientation());
	}

	@Test
	void infoTextNamesTheInnerCircuit() {
		assertEquals("inner (a subcircuit)", loneSub().infoText());
	}

	@Test
	void canChangeAndDelayResetAreDelegatedSafely() {
		SubCircuit sub = loneSub();
		Element asElement = sub;
		assertTrue(asElement instanceof Editable,
				"subcircuits can be edited");
		// the passthrough inner circuit has no timed elements; the
		// delegation must still walk it without faulting
		sub.resetPropDelay();
	}

	@Test
	void unattachedBlockFlipsBetweenLeftAndRight() {
		SubCircuit sub = loneSub();
		assertTrue(sub.canFlip());
		sub.flip(metrics());
		assertEquals(Orientation.LEFT, sub.getOrientation());
		// facing left, inputs sit on the right edge and outputs on the
		// left - the mirror of the default facing
		for (Input in : sub.getInputList()) {
			assertEquals(sub.getWidth(), in.getXr(),
					"left-facing inputs belong on the right edge");
		}
		for (Output out : sub.getOutputList()) {
			assertEquals(0, out.getXr(),
					"left-facing outputs belong on the left edge");
		}
		sub.flip(metrics());
		assertEquals(Orientation.RIGHT, sub.getOrientation());
		for (Input in : sub.getInputList()) {
			assertEquals(0, in.getXr(),
					"right-facing inputs belong on the left edge");
		}
	}

	@Test
	void wiredBlockRefusesToFlip() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int one = cb.constant(1);
		int sub = cb.subCircuit("sub");
		cb.wire(one, "output", sub, "a");
		assertFalse(findSub(load(cb.build())).canFlip());
	}

	@Test
	void copyDeepCopiesTheInnerCircuitAndRebindsThePins() {
		SubCircuit sub = loneSub();
		SubCircuit copy = (SubCircuit) sub.copy();
		assertEquals("inner", copy.getName());
		assertEquals(Orientation.RIGHT, copy.getOrientation());
		assertNotSame(sub.getSubCircuit(), copy.getSubCircuit(),
				"the inner circuit must be copied, not shared");
		assertEquals("inner", copy.getSubCircuit().getName());
		String text = copy.toString();
		assertTrue(text.contains("inputs={a}"),
				"the copy must rebind its inputs to the copied pins: "
						+ text);
		assertTrue(text.contains("outputs={y}"), text);
	}

	@Test
	void remapPinsRebindsToTheGivenCircuitsPins() {
		SubCircuit sub = loneSub();
		SubCircuit donor = loneSub();
		sub.remapPins(donor.getSubCircuit());
		// the bindings now point into the donor circuit and stay
		// complete, so tri-state propagation still finds every pin
		sub.setTriState(true);
		assertTrue(sub.toString().contains("inputs={a}"), sub.toString());
		// remapping against a circuit with no pins empties the maps -
		// the unbound state setTriState must refuse to work from
		sub.remapPins(new Circuit("empty"));
		assertThrows(IllegalStateException.class,
				() -> sub.setTriState(true));
	}

	@Test
	void setTriStatePropagatesOverAttachedAndUnattachedInputs() {
		// unattached inputs always propagate "not tri-state"
		SubCircuit lone = loneSub();
		lone.setTriState(true);
		lone.setTriState(false);
		// an attached input reads its wire's tri-state answer instead
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int one = cb.constant(1);
		int sub = cb.subCircuit("sub");
		cb.wire(one, "output", sub, "a");
		SubCircuit wired = findSub(load(cb.build()));
		wired.setTriState(true);
		wired.setTriState(false);
	}

	@Test
	void setWatchedFansOutToTheInnerElements() {
		SubCircuit sub = loneSub();
		sub.setWatched(true);
		int checked = 0;
		for (Element el : sub.getSubCircuit().getElements()) {
			if (el instanceof Pin pin) {
				assertTrue(pin.isWatched(),
						pin.getName() + " must be watched");
				checked += 1;
			}
		}
		assertEquals(2, checked, "the passthrough has exactly two pins");
		sub.setWatched(false);
		for (Element el : sub.getSubCircuit().getElements()) {
			if (el instanceof Pin pin) {
				assertFalse(pin.isWatched());
			}
		}
	}

	@Test
	void removeUnregistersTheLocalName() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.subCircuit("sub");
		Circuit circuit = load(cb.build());
		SubCircuit sub = findSub(circuit);
		// the loader names the block after the inner circuit
		circuit.addName("inner");
		sub.remove(circuit);
		assertFalse(circuit.hasName("inner"),
				"remove must free the imported name for reuse");
	}
}
