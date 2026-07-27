package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.collab.op.AddWire;
import jls.collab.op.CircuitOp;
import jls.collab.op.OpRejected;
import jls.collab.op.OpSink;
import jls.collab.op.RemoveElements;
import jls.collab.op.RemoveWire;
import jls.elem.Element;
import jls.elem.JumpStart;
import jls.elem.Pin;
import jls.elem.SubCircuit;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * The delete-selection gesture's migration behind the OpSink seam
 * (issue #167), pinned at the Swing-free plan builder
 * {@link SimpleEditor#deleteSelectionPlan} (the
 * {@link SimpleEditor#startWireGesture} precedent - the editor itself
 * cannot be constructed headless):
 *
 * <ul>
 * <li>P1 - the op-plan path produces the same canonical bytes (#166)
 * as the inline {@code el.remove(circuit)} loop it replaces, for
 * unwired selections, fully-wired selections, the jump-start cascade,
 * and - since the RemoveWire-plus-AddWire-survivors composition - for
 * selections that only clip a net, on constructed and on
 * save/load-restored circuits alike;</li>
 * <li>fallback - only subcircuits, in-progress wiring, and uneditable
 * cascades still have no plan and take the inline path unchanged;</li>
 * <li>undo granularity - a plan submitted through a batch
 * {@link OpSink#submitAll} records exactly one markChanged per
 * gesture, however many ops the plan holds.</li>
 * </ul>
 */
class DeleteGestureTest {

	/**
	 * A one-segment wire net between an input pin and an output pin,
	 * every element with a declared stable id (the CircuitOpTest
	 * fixture).
	 */
	private static String wiredPairText() {
		return "CIRCUIT wired\n"
				+ "ELEMENT InputPin\n"
				+ " int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 1\n int x 360\n int y 120\n"
				+ " String sid \"pin:2\"\n String name \"B\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"LEFT\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 2\n int x 180\n int y 120\n"
				+ " String sid \"we:1\"\n"
				+ " String put \"output\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n int id 3\n int x 300\n int y 120\n"
				+ " String sid \"we:2\"\n"
				+ " String put \"input\"\n ref attach 1\n ref wire 2\n"
				+ "END\nENDCIRCUIT\n";
	}

	/** The two pins of {@link #wiredPairText} without the net. */
	private static String unwiredPairText() {
		return "CIRCUIT wired\n"
				+ "ELEMENT InputPin\n int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 1\n int x 360\n int y 120\n"
				+ " String sid \"pin:2\"\n String name \"B\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"LEFT\"\nEND\nENDCIRCUIT\n";
	}

	/**
	 * A three-segment chain between an input pin and an output pin:
	 * pin:1 - we:1 - we:2 - we:3 - we:4 - pin:2. With {@code triState},
	 * the driving pin and the whole net are tri-state.
	 */
	private static String chainText(boolean triState) {
		String tri = triState ? " int tristate 1\n" : "";
		return "CIRCUIT chain\n"
				+ "ELEMENT InputPin\n" + tri
				+ " int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 1\n int x 600\n int y 120\n"
				+ " String sid \"pin:2\"\n String name \"B\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"LEFT\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 2\n int x 180\n int y 120\n"
				+ " String sid \"we:1\"\n" + tri
				+ " String put \"output\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n int id 3\n int x 300\n int y 120\n"
				+ " String sid \"we:2\"\n" + tri
				+ " ref wire 2\n ref wire 4\nEND\n"
				+ "ELEMENT WireEnd\n int id 4\n int x 420\n int y 120\n"
				+ " String sid \"we:3\"\n" + tri
				+ " ref wire 3\n ref wire 5\nEND\n"
				+ "ELEMENT WireEnd\n int id 5\n int x 540\n int y 120\n"
				+ " String sid \"we:4\"\n" + tri
				+ " String put \"input\"\n ref attach 1\n ref wire 4\n"
				+ "END\nENDCIRCUIT\n";
	}

	/**
	 * A three-end net: an attached driving end that fans out to an
	 * attached end (over a probed wire) and to a dangling end (the
	 * CircuitOpTest fixture).
	 */
	private static String fanOutText() {
		return "CIRCUIT fan\n"
				+ "ELEMENT InputPin\n int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT OutputPin\n int id 1\n int x 360\n int y 120\n"
				+ " String sid \"pin:2\"\n String name \"B\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"LEFT\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 2\n int x 180\n int y 120\n"
				+ " String sid \"we:1\"\n String put \"output\"\n"
				+ " ref attach 0\n ref wire 3\n probe 3 \"tap\"\n"
				+ " ref wire 4\nEND\n"
				+ "ELEMENT WireEnd\n int id 3\n int x 300\n int y 120\n"
				+ " String sid \"we:2\"\n String put \"input\"\n"
				+ " ref attach 1\n ref wire 2\n probe 2 \"tap\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 4\n int x 180\n int y 240\n"
				+ " String sid \"we:3\"\n ref wire 2\nEND\n"
				+ "ENDCIRCUIT\n";
	}

	/**
	 * A jump start wired to a one-segment dangling net, plus its
	 * same-name unwired jump end.
	 */
	private static String wiredJumpText() {
		return "CIRCUIT jumpnet\n"
				+ "ELEMENT JumpStart\n int id 0\n int x 180\n int y 120\n"
				+ " String sid \"js:1\"\n String name \"js\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orientation \"LEFT\"\nEND\n"
				+ "ELEMENT JumpEnd\n int id 1\n int x 420\n int y 240\n"
				+ " String sid \"je:2\"\n String name \"js\"\n"
				+ " int bits 1\n String orientation \"LEFT\"\nEND\n"
				+ "ELEMENT WireEnd\n int id 2\n int x 180\n int y 120\n"
				+ " String sid \"we:1\"\n"
				+ " String put \"input\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n int id 3\n int x 300\n int y 120\n"
				+ " String sid \"we:2\"\n ref wire 2\nEND\n"
				+ "ENDCIRCUIT\n";
	}

	/** A jump start and its same-name jump end, both unwired. */
	private static String jumpPairText() {
		return "CIRCUIT jumps\n"
				+ "ELEMENT JumpStart\n int id 0\n int x 180\n int y 60\n"
				+ " String sid \"js:1\"\n String name \"js\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orientation \"LEFT\"\nEND\n"
				+ "ELEMENT JumpEnd\n int id 1\n int x 300\n int y 60\n"
				+ " String sid \"je:2\"\n String name \"js\"\n"
				+ " int bits 1\n String orientation \"LEFT\"\nEND\n"
				+ "ENDCIRCUIT\n";
	}

	/** An uneditable ("fixed") constant, alone in a circuit. */
	private static String fixedConstantText() {
		return "CIRCUIT locked\n"
				+ "ELEMENT Constant\n int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n Int value 1\n"
				+ " int base 10\n int fixed 1\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ENDCIRCUIT\n";
	}

	/** A circuit loaded headlessly from inline save-format text. */
	private static Circuit loadText(String text) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(SwingTextMetrics.of(graphics())),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * A circuit restored through a save/load round trip (issue #167
	 * section 10: ops validated on a live circuit may behave
	 * differently on a restored object graph).
	 */
	private static Circuit restored(Circuit circuit) throws Exception {
		Circuit copy = new Circuit("");
		assertTrue(copy.load(new Scanner(save(circuit))),
				() -> "restore load failed: " + JLSInfo.loadError);
		assertTrue(copy.finishLoad(SwingTextMetrics.of(graphics())),
				() -> "restore finishLoad failed: " + JLSInfo.loadError);
		return copy;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	private static Graphics2D graphics() {
		return new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
				.createGraphics();
	}

	/** All elements matching the predicate, as a selection set. */
	private static Set<Element> select(Circuit circuit,
			Predicate<Element> p) {
		Set<Element> selected = new HashSet<Element>();
		for (Element el : circuit.getElements()) {
			if (p.test(el)) {
				selected.add(el);
			}
		}
		assertTrue(!selected.isEmpty(), "fixture lacks a needed element");
		return selected;
	}

	/** The wire end with the given stable id. */
	private static WireEnd endBySid(Circuit circuit, String sid) {
		for (Element el : circuit.getElements()) {
			if (el instanceof WireEnd end
					&& end.getStableId().toString().equals(sid)) {
				return end;
			}
		}
		throw new AssertionError("fixture lacks wire end " + sid);
	}

	/** The wire between the two ends with the given stable ids. */
	private static Wire wireBetween(Circuit circuit, String sidA,
			String sidB) {
		WireEnd a = endBySid(circuit, sidA);
		WireEnd b = endBySid(circuit, sidB);
		for (Wire wire : a.getWires()) {
			if (wire.getOtherEnd(a) == b) {
				return wire;
			}
		}
		throw new AssertionError(
				"fixture lacks a wire " + sidA + "-" + sidB);
	}

	/** A mutable selection set over the given elements. */
	private static Set<Element> setOf(Element... elements) {
		Set<Element> selected = new HashSet<Element>();
		for (Element el : elements) {
			selected.add(el);
		}
		return selected;
	}

	/** Apply a plan the way the editor's opSink batch does. */
	private static void apply(Circuit circuit, List<CircuitOp> plan)
			throws Exception {
		for (CircuitOp op : plan) {
			op.apply(circuit, graphics());
		}
	}

	/** The editor's inline fallback loop, verbatim. */
	private static void removeInline(Circuit circuit,
			Set<Element> selected) {
		for (Element el : selected) {
			el.remove(circuit);
		}
	}

	/** Picks the same selection out of independently loaded circuits. */
	private interface Selector {
		Set<Element> select(Circuit circuit);
	}

	/**
	 * P1 harness: the plan of the given shape, applied, byte-matches
	 * the inline removal of the same selection - on the constructed
	 * circuit and on a save/load-restored one (issue #167 section 10).
	 */
	private static void assertPlanParity(String text, Selector selector,
			int expectedOps) throws Exception {
		for (boolean restore : new boolean[] { false, true }) {
			Circuit viaPlan = loadText(text);
			Circuit inline = loadText(text);
			if (restore) {
				viaPlan = restored(viaPlan);
				inline = restored(inline);
			}
			List<CircuitOp> plan = SimpleEditor.deleteSelectionPlan(
					viaPlan, selector.select(viaPlan));
			assertTrue(plan != null, "the selection must have a plan");
			assertEquals(expectedOps, plan.size(),
					"unexpected plan shape");
			apply(viaPlan, plan);
			removeInline(inline, selector.select(inline));
			assertEquals(save(inline), save(viaPlan),
					(restore ? "restored" : "constructed")
							+ ": plan and inline delete must produce "
							+ "identical bytes");
		}
	}

	// ------------------------------------------------------------------
	// P1: plan path vs inline path, byte parity
	// ------------------------------------------------------------------

	/**
	 * An unwired selection plans to a single RemoveElements whose
	 * application byte-matches the inline removal loop.
	 */
	@Test
	void unwiredSelectionPlanMatchesInlineDelete() throws Exception {
		Circuit viaPlan = loadText(unwiredPairText());
		Circuit inline = loadText(unwiredPairText());
		Set<Element> selected = select(viaPlan, el -> el instanceof Pin);
		List<CircuitOp> plan =
				SimpleEditor.deleteSelectionPlan(viaPlan, selected);
		assertTrue(plan != null, "an unwired selection must have a plan");
		assertEquals(1, plan.size(), "one RemoveElements expected");
		assertTrue(plan.get(0) instanceof RemoveElements,
				"the single op must be the element removal");
		apply(viaPlan, plan);
		removeInline(inline, select(inline, el -> el instanceof Pin));
		assertEquals(save(inline), save(viaPlan),
				"plan and inline delete must produce identical bytes");
	}

	/**
	 * A fully-wired selection plans to RemoveWire-per-net followed by
	 * one RemoveElements over the then-unwired elements, and the plan
	 * byte-matches the inline removal of the same selection.
	 */
	@Test
	void wiredSelectionPlanMatchesInlineDelete() throws Exception {
		Circuit viaPlan = loadText(wiredPairText());
		Circuit inline = loadText(wiredPairText());
		Set<Element> selected = select(viaPlan,
				el -> el instanceof Pin || el instanceof Wire);
		List<CircuitOp> plan =
				SimpleEditor.deleteSelectionPlan(viaPlan, selected);
		assertTrue(plan != null, "a wholly-wired selection must have a plan");
		assertEquals(2, plan.size(),
				"one RemoveWire plus one RemoveElements expected");
		assertTrue(plan.get(0) instanceof RemoveWire,
				"the net removal must come first");
		assertTrue(plan.get(1) instanceof RemoveElements,
				"the element removal must come after the net removal");
		apply(viaPlan, plan);
		removeInline(inline, select(inline,
				el -> el instanceof Pin || el instanceof Wire));
		assertEquals(save(inline), save(viaPlan),
				"plan and inline delete must produce identical bytes");
	}

	/**
	 * Deleting a whole single-segment net by selecting only its wire
	 * (the popup delete on a wire) also plans, to a single RemoveWire,
	 * and byte-matches the inline wire removal cascade.
	 */
	@Test
	void wireOnlySelectionPlansToARemoveWire() throws Exception {
		Circuit viaPlan = loadText(wiredPairText());
		Circuit inline = loadText(wiredPairText());
		Set<Element> selected = select(viaPlan, el -> el instanceof Wire);
		List<CircuitOp> plan =
				SimpleEditor.deleteSelectionPlan(viaPlan, selected);
		assertTrue(plan != null, "a whole-net wire selection must plan");
		assertEquals(1, plan.size(), "one RemoveWire expected");
		assertTrue(plan.get(0) instanceof RemoveWire,
				"the single op must be the net removal");
		apply(viaPlan, plan);
		removeInline(inline, select(inline, el -> el instanceof Wire));
		assertEquals(save(inline), save(viaPlan),
				"plan and inline wire delete must produce identical bytes");
	}

	/**
	 * A selected jump start expands with its same-name jump ends, the
	 * cascade the editor's inline removal performs and RemoveElements
	 * validation requires.
	 */
	@Test
	void jumpStartSelectionExpandsWithItsEnds() throws Exception {
		Circuit viaPlan = loadText(jumpPairText());
		Circuit inline = loadText(jumpPairText());
		Set<Element> selected = select(viaPlan,
				el -> el instanceof JumpStart);
		List<CircuitOp> plan =
				SimpleEditor.deleteSelectionPlan(viaPlan, selected);
		assertTrue(plan != null, "a jump-start selection must have a plan");
		assertEquals(1, plan.size(), "one RemoveElements expected");
		assertEquals(2, ((RemoveElements) plan.get(0)).ids().size(),
				"the plan must carry the jump start and its jump end");
		apply(viaPlan, plan);
		removeInline(inline, select(inline,
				el -> el instanceof JumpStart));
		assertEquals(save(inline), save(viaPlan),
				"plan and inline cascade must produce identical bytes");
	}

	// ------------------------------------------------------------------
	// P1, clipped nets: RemoveWire + AddWire-survivors composition
	// ------------------------------------------------------------------

	/**
	 * Deleting the middle segment of a three-segment chain splits the
	 * net in two: the plan is RemoveWire of the whole net followed by
	 * two survivor AddWires, ordered by each component's lowest end
	 * stable id, and byte-matches the inline segment delete.
	 */
	@Test
	void middleSegmentDeletePlansTwoSurvivorAddWires() throws Exception {
		Circuit circuit = loadText(chainText(false));
		String before = save(circuit);
		List<CircuitOp> plan = SimpleEditor.deleteSelectionPlan(circuit,
				setOf(wireBetween(circuit, "we:2", "we:3")));
		assertTrue(plan != null, "a clipped net must now have a plan");
		assertEquals(3, plan.size(),
				"RemoveWire plus two survivor AddWires expected");
		assertTrue(plan.get(0) instanceof RemoveWire,
				"the whole-net removal must come first");
		AddWire first = (AddWire) plan.get(1);
		AddWire second = (AddWire) plan.get(2);
		assertTrue(first.blocks().get(0).contains("\"we:1\""),
				"the we:1/we:2 component must come first (lowest end id)");
		assertTrue(second.blocks().get(0).contains("\"we:3\""),
				"the we:3/we:4 component must come second");
		assertEquals(before, save(circuit),
				"planning must never mutate the circuit");
		assertPlanParity(chainText(false),
				c -> setOf(wireBetween(c, "we:2", "we:3")), 3);
	}

	/**
	 * Deleting a leaf segment cascades like the inline path: the end
	 * that kept no wire vanishes and detaches from its pin, and the one
	 * surviving component re-attaches only the surviving anchor.
	 */
	@Test
	void leafSegmentDeleteCascadesAndDetaches() throws Exception {
		Circuit circuit = loadText(chainText(false));
		List<CircuitOp> plan = SimpleEditor.deleteSelectionPlan(circuit,
				setOf(wireBetween(circuit, "we:1", "we:2")));
		assertTrue(plan != null, "a clipped net must now have a plan");
		assertEquals(2, plan.size(),
				"RemoveWire plus one survivor AddWire expected");
		AddWire survivor = (AddWire) plan.get(1);
		assertEquals(1, survivor.attach().size(),
				"only the surviving pin may anchor the survivor");
		assertEquals("pin:2", survivor.attach().get(0).toString(),
				"the survivor must re-attach the un-deleted pin only");
		assertEquals(3, survivor.blocks().size(),
				"the we:2/we:3/we:4 component must survive whole");
		assertPlanParity(chainText(false),
				c -> setOf(wireBetween(c, "we:1", "we:2")), 2);
	}

	/**
	 * Tri-state is recomputed per surviving component, mirroring
	 * WireNet.makeNet: deleting the driver's side leaves a survivor
	 * whose blocks carry no tri-state flag (and whose TriProp sinks the
	 * inline path clears stay cleared), while deleting the far side
	 * keeps the driver and with it the flag.
	 */
	@Test
	void triStateFollowsTheSurvivingDriver() throws Exception {
		// driver-side delete: the survivor is no longer tri-state
		Circuit lost = loadText(chainText(true));
		List<CircuitOp> lostPlan = SimpleEditor.deleteSelectionPlan(lost,
				setOf(wireBetween(lost, "we:1", "we:2")));
		assertTrue(lostPlan != null && lostPlan.size() == 2,
				"RemoveWire plus one survivor AddWire expected");
		for (String block : ((AddWire) lostPlan.get(1)).blocks()) {
			assertTrue(!block.contains("tristate"),
					"a survivor without its driver must not be tri-state");
		}
		assertPlanParity(chainText(true),
				c -> setOf(wireBetween(c, "we:1", "we:2")), 2);

		// far-side delete: the driver survives and so does the flag
		Circuit kept = loadText(chainText(true));
		List<CircuitOp> keptPlan = SimpleEditor.deleteSelectionPlan(kept,
				setOf(wireBetween(kept, "we:3", "we:4")));
		assertTrue(keptPlan != null && keptPlan.size() == 2,
				"RemoveWire plus one survivor AddWire expected");
		for (String block : ((AddWire) keptPlan.get(1)).blocks()) {
			assertTrue(block.contains(" int tristate 1\n"),
					"a survivor keeping its driver must stay tri-state");
		}
		assertPlanParity(chainText(true),
				c -> setOf(wireBetween(c, "we:3", "we:4")), 2);
	}

	/**
	 * A wired element deleted without its net - the case that used to
	 * have no plan and fall back inline - now plans to RemoveWire of
	 * the whole net, an AddWire re-adding it with the attachment to the
	 * deleted element dropped, and the RemoveElements; the composition
	 * byte-matches the inline detach.
	 */
	@Test
	void wiredElementWithoutItsNetPlansToDetachComposition()
			throws Exception {
		Circuit circuit = loadText(wiredPairText());
		String before = save(circuit);
		List<CircuitOp> plan = SimpleEditor.deleteSelectionPlan(circuit,
				select(circuit, el -> el instanceof Pin
						&& "A".equals(el.getName())));
		assertTrue(plan != null,
				"a wired element without its net must now have a plan");
		assertEquals(3, plan.size(),
				"RemoveWire, survivor AddWire, RemoveElements expected");
		assertTrue(plan.get(0) instanceof RemoveWire,
				"the whole-net removal must come first");
		AddWire survivor = (AddWire) plan.get(1);
		assertEquals(List.of("pin:2"),
				survivor.attach().stream().map(Object::toString).toList(),
				"the deleted element's anchor must be dropped");
		assertTrue(!survivor.blocks().get(0).contains(" String put "),
				"the end attached to the deleted element must arrive "
						+ "detached");
		assertTrue(plan.get(2) instanceof RemoveElements,
				"the element removal must come last");
		assertEquals(before, save(circuit),
				"planning must never mutate the circuit");
		assertPlanParity(wiredPairText(),
				c -> select(c, el -> el instanceof Pin
						&& "A".equals(el.getName())), 3);
	}

	/**
	 * Probes follow their wires: a probe on a removed wire disappears
	 * with it, a probe on a kept wire travels in the survivor's blocks.
	 */
	@Test
	void probesFollowTheirWires() throws Exception {
		// the probed wire is removed: no probe in the survivor
		Circuit dropped = loadText(fanOutText());
		List<CircuitOp> droppedPlan = SimpleEditor.deleteSelectionPlan(
				dropped, setOf(wireBetween(dropped, "we:1", "we:2")));
		assertTrue(droppedPlan != null && droppedPlan.size() == 2,
				"RemoveWire plus one survivor AddWire expected");
		for (String block : ((AddWire) droppedPlan.get(1)).blocks()) {
			assertTrue(!block.contains(" probe "),
					"a probe on a removed wire must not survive");
		}
		assertPlanParity(fanOutText(),
				c -> setOf(wireBetween(c, "we:1", "we:2")), 2);

		// the unprobed wire is removed: the kept wire keeps its probe
		Circuit kept = loadText(fanOutText());
		List<CircuitOp> keptPlan = SimpleEditor.deleteSelectionPlan(kept,
				setOf(wireBetween(kept, "we:1", "we:3")));
		assertTrue(keptPlan != null && keptPlan.size() == 2,
				"RemoveWire plus one survivor AddWire expected");
		assertTrue(((AddWire) keptPlan.get(1)).blocks().get(0)
				.contains(" probe 3 \"tap\"\n"),
				"a probe on a kept wire must travel with it");
		assertPlanParity(fanOutText(),
				c -> setOf(wireBetween(c, "we:1", "we:3")), 2);
	}

	/**
	 * The jump-start expansion composes with the survivor path: the
	 * jump start leaves with its jump end while its clipped net is
	 * re-added detached, byte-matching the inline cascade.
	 */
	@Test
	void jumpStartExpansionComposesWithAPartialNet() throws Exception {
		Circuit circuit = loadText(wiredJumpText());
		List<CircuitOp> plan = SimpleEditor.deleteSelectionPlan(circuit,
				select(circuit, el -> el instanceof JumpStart));
		assertTrue(plan != null,
				"a wired jump start must now have a plan");
		assertEquals(3, plan.size(),
				"RemoveWire, survivor AddWire, RemoveElements expected");
		AddWire survivor = (AddWire) plan.get(1);
		assertTrue(survivor.attach().isEmpty(),
				"the survivor net must arrive dangling");
		assertEquals(2, ((RemoveElements) plan.get(2)).ids().size(),
				"the plan must carry the jump start and its jump end");
		assertPlanParity(wiredJumpText(),
				c -> select(c, el -> el instanceof JumpStart), 3);
	}

	// ------------------------------------------------------------------
	// fallback: selections the vocabulary cannot express
	// ------------------------------------------------------------------

	/**
	 * A subcircuit still has no plan - its op kind is deferred - so the
	 * gesture takes the inline fallback.
	 */
	@Test
	void subCircuitSelectionHasNoPlan() throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		SubCircuit sub = new SubCircuit(circuit);
		circuit.addElement(sub);
		assertNull(SimpleEditor.deleteSelectionPlan(circuit, setOf(sub)),
				"a subcircuit selection must have no plan");
	}

	/**
	 * In-progress wiring (the startwire end, no wires yet) still has no
	 * plan; the inline path owns gesture-local cleanup.
	 */
	@Test
	void inProgressWiringHasNoPlan() throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		Set<Element> selected = new HashSet<Element>();
		SimpleEditor.startWireGesture(circuit, selected, 240, 240);
		assertNull(SimpleEditor.deleteSelectionPlan(circuit, selected),
				"in-progress wiring must have no plan");
	}

	/**
	 * An uneditable selection produces no ops - the gesture's guard
	 * dialogs before any mutation - and planning leaves the circuit
	 * byte-identical.
	 */
	@Test
	void uneditableSelectionHasNoPlanAndLeavesBytesIdentical()
			throws Exception {
		Circuit circuit = loadText(fixedConstantText());
		String before = save(circuit);
		assertNull(SimpleEditor.deleteSelectionPlan(circuit,
				select(circuit, el -> true)),
				"an uneditable selection must produce no ops");
		assertEquals(before, save(circuit),
				"planning must never mutate the circuit");
	}

	/** An empty selection has no plan (the inline path no-ops it). */
	@Test
	void emptySelectionHasNoPlan() throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		assertNull(SimpleEditor.deleteSelectionPlan(circuit,
				new HashSet<Element>()),
				"an empty selection must have no plan");
	}

	// ------------------------------------------------------------------
	// undo granularity: one markChanged per gesture
	// ------------------------------------------------------------------

	/**
	 * A sink shaped like the editor's opSink: submit records per op,
	 * and the submitAll override applies every op then records exactly
	 * once - the shape that keeps a wired delete a single undo
	 * snapshot.
	 */
	private static final class BatchSink implements OpSink {

		private final Circuit circuit;
		private final List<Integer> marks = new ArrayList<Integer>();
		private int applied = 0;

		private BatchSink(Circuit circuit) {
			this.circuit = circuit;
		}

		@Override
		public void submit(CircuitOp op) throws OpRejected {
			op.apply(circuit, graphics());
			applied += 1;
			marks.add(applied);
		}

		@Override
		public void submitAll(List<CircuitOp> ops) throws OpRejected {
			for (CircuitOp op : ops) {
				op.apply(circuit, graphics());
				applied += 1;
			}
			marks.add(applied);
		}
	} // end of BatchSink class

	/**
	 * The wired-delete plan submitted as one batch records exactly one
	 * markChanged for the whole gesture; the same plan pushed op by op
	 * through the default submitAll would fragment into one record per
	 * op - the reason the editor overrides it.
	 */
	@Test
	void batchSubmitRecordsExactlyOneMarkChangedPerGesture()
			throws Exception {
		Circuit circuit = loadText(wiredPairText());
		Set<Element> selected = select(circuit,
				el -> el instanceof Pin || el instanceof Wire);
		List<CircuitOp> plan =
				SimpleEditor.deleteSelectionPlan(circuit, selected);
		assertTrue(plan != null && plan.size() == 2,
				"the wired delete must plan to two ops");
		BatchSink sink = new BatchSink(circuit);
		sink.submitAll(plan);
		assertEquals(2, sink.applied, "both ops must apply");
		assertEquals(List.of(2), sink.marks,
				"one markChanged, after all ops, per gesture");
	}

	/**
	 * The clipped-net composition batches the same way: however many
	 * survivor AddWires the plan holds, one gesture records exactly one
	 * markChanged.
	 */
	@Test
	void partialNetPlanBatchesAsOneGesture() throws Exception {
		Circuit circuit = loadText(chainText(false));
		List<CircuitOp> plan = SimpleEditor.deleteSelectionPlan(circuit,
				setOf(wireBetween(circuit, "we:2", "we:3")));
		assertTrue(plan != null && plan.size() == 3,
				"the middle-segment delete must plan to three ops");
		BatchSink sink = new BatchSink(circuit);
		sink.submitAll(plan);
		assertEquals(3, sink.applied, "all three ops must apply");
		assertEquals(List.of(3), sink.marks,
				"one markChanged, after all ops, per gesture");
	}
} // end of DeleteGestureTest class
