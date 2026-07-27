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
import jls.collab.op.CircuitOp;
import jls.collab.op.OpRejected;
import jls.collab.op.OpSink;
import jls.collab.op.RemoveElements;
import jls.collab.op.RemoveWire;
import jls.elem.Element;
import jls.elem.JumpStart;
import jls.elem.Pin;
import jls.elem.Wire;

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
 * unwired selections, fully-wired selections, and the jump-start
 * cascade;</li>
 * <li>fallback - a partially selected net has no plan, so the gesture
 * takes the inline path unchanged (the RemoveWire-plus-AddWire
 * composition for clipped nets is a follow-up);</li>
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
	// fallback: selections the vocabulary cannot express yet
	// ------------------------------------------------------------------

	/**
	 * A wired element selected without its net leaves the net partially
	 * covered: no plan, so the gesture takes the inline fallback (the
	 * clipped-net composition is a follow-up per
	 * docs/operation-layer.md).
	 */
	@Test
	void partiallySelectedNetHasNoPlan() throws Exception {
		Circuit circuit = loadText(wiredPairText());
		String before = save(circuit);
		assertNull(SimpleEditor.deleteSelectionPlan(circuit,
				select(circuit, el -> el instanceof Pin)),
				"a partially covered net must have no plan");
		assertEquals(before, save(circuit),
				"planning must never mutate the circuit");
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
} // end of DeleteGestureTest class
