package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import jls.collab.op.MoveElements;
import jls.collab.op.OpRejected;
import jls.collab.op.OpSink;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Pin;
import jls.elem.SubCircuit;
import jls.elem.Wire;

/**
 * The move-selection drag commit's migration behind the OpSink seam
 * (issue #167), pinned at the Swing-free plan builder
 * {@link SimpleEditor#moveSelectionPlan} (the editor itself cannot be
 * constructed headless):
 *
 * <ul>
 * <li>P1 - the op-plan path (restore the pre-drag positions, then apply
 * one {@link MoveElements}) produces the same canonical bytes (#166) as
 * the inline {@code move} + {@code fixPosition} commit it replaces, for
 * single- and multi-element pure relocations, on constructed and on
 * save/load-restored circuits alike. For an eligible drop the inline
 * commit's {@code connect()} and {@code removeCoLinear()} do nothing -
 * that is exactly the boundary the predicate enforces - so the inline
 * mutation reduces to the drag plus {@code fixPosition};</li>
 * <li>fallback - a drop that would form a connection, a wired element
 * (whose move drags a wire end and can re-form its net), a wire, and a
 * subcircuit all have no plan and take the inline path unchanged;</li>
 * <li>rejection - a {@link MoveElements} that would push an element off
 * the canvas is rejected and leaves the circuit byte-identical;</li>
 * <li>undo granularity - the plan submitted through a batch
 * {@link OpSink#submitAll} records exactly one markChanged per
 * gesture.</li>
 * </ul>
 */
class MoveGestureTest {

	/** Two unwired pins whose puts (at 180 and 300) do not coincide. */
	private static String unwiredPairText() {
		return "CIRCUIT moves\n"
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
	 * Two source pins whose puts sit on facing body edges: pin A's put
	 * at (156,132), pin B's put at (360,132). Dragging A right by 204
	 * lands its put exactly on B's, the put-to-put coincidence
	 * connect() turns into a wire.
	 */
	private static String facingPinsText() {
		return "CIRCUIT facing\n"
				+ "ELEMENT InputPin\n int id 0\n int x 120\n int y 120\n"
				+ " String sid \"pin:1\"\n String name \"A\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\nEND\n"
				+ "ELEMENT InputPin\n int id 1\n int x 360\n int y 120\n"
				+ " String sid \"pin:2\"\n String name \"B\"\n"
				+ " int bits 1\n int watch 0\n"
				+ " String orient \"LEFT\"\nEND\nENDCIRCUIT\n";
	}

	/** The same two pins joined by a one-segment wire net. */
	private static String wiredPairText() {
		return "CIRCUIT wired\n"
				+ "ELEMENT InputPin\n int id 0\n int x 120\n int y 120\n"
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

	/** A mutable selection set over the given elements. */
	private static Set<Element> setOf(Element... elements) {
		Set<Element> selected = new HashSet<Element>();
		for (Element el : elements) {
			selected.add(el);
		}
		return selected;
	}

	/** Picks the same selection out of independently loaded circuits. */
	private interface Selector {
		Set<Element> select(Circuit circuit);
	}

	/**
	 * Drive the selection through a live drag: save the pre-drag
	 * positions and translate every element by the delta, then reindex
	 * as the editor's mouseDragged does, so the plan builder reads the
	 * same post-move geometry connect() would.
	 */
	private static void drag(Circuit circuit, Set<Element> selected,
			int dx, int dy) {
		for (Element el : selected) {
			el.savePosition();
		}
		for (Element el : selected) {
			el.move(dx, dy);
		}
		circuit.reindexAfterMove(selected);
	}

	/**
	 * P1 harness: the op path (drag, plan, restore the pre-drag
	 * positions, apply the one MoveElements) byte-matches the inline
	 * commit (drag, fixPosition) for a pure relocation - connect() and
	 * removeCoLinear() are no-ops on an eligible drop - on the
	 * constructed circuit and on a save/load-restored one.
	 */
	private static void assertMovePlanParity(String text,
			Selector selector, int dx, int dy) throws Exception {
		for (boolean restore : new boolean[] { false, true }) {
			Circuit viaOp = loadText(text);
			Circuit inline = loadText(text);
			if (restore) {
				viaOp = restored(viaOp);
				inline = restored(inline);
			}

			Set<Element> opSel = selector.select(viaOp);
			drag(viaOp, opSel, dx, dy);
			List<CircuitOp> plan =
					SimpleEditor.moveSelectionPlan(viaOp, opSel, dx, dy);
			assertTrue(plan != null, "a pure relocation must have a plan");
			assertEquals(1, plan.size(), "one MoveElements expected");
			assertTrue(plan.get(0) instanceof MoveElements,
					"the single op must be the move");
			for (Element el : opSel) {
				el.restorePosition();
			}
			apply(viaOp, plan);

			Set<Element> inSel = selector.select(inline);
			drag(inline, inSel, dx, dy);
			for (Element el : inSel) {
				el.fixPosition();
			}

			assertEquals(save(inline), save(viaOp),
					(restore ? "restored" : "constructed")
							+ ": plan and inline move must produce "
							+ "identical bytes");
		}
	}

	/** Apply a plan the way the editor's opSink batch does. */
	private static void apply(Circuit circuit, List<CircuitOp> plan)
			throws Exception {
		for (CircuitOp op : plan) {
			op.apply(circuit, graphics());
		}
	}

	// ------------------------------------------------------------------
	// P1: plan path vs inline path, byte parity
	// ------------------------------------------------------------------

	/**
	 * A single pin dropped in empty space plans to one MoveElements
	 * whose application byte-matches the inline drag + fixPosition.
	 */
	@Test
	void singleElementRelocationMatchesInline() throws Exception {
		assertMovePlanParity(unwiredPairText(),
				c -> select(c, el -> el instanceof Pin
						&& "A".equals(el.getName())),
				60, 120);
	}

	/**
	 * Both pins dragged together by one delta plan to a single
	 * MoveElements carrying both ids, byte-matching the inline drag.
	 */
	@Test
	void multiElementRelocationMatchesInline() throws Exception {
		assertMovePlanParity(unwiredPairText(),
				c -> select(c, el -> el instanceof Pin), 0, 120);
	}

	/**
	 * The plan carries every selected element's stable id and never
	 * mutates the circuit while planning.
	 */
	@Test
	void planCarriesEveryIdAndDoesNotMutate() throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		Set<Element> selected = select(circuit, el -> el instanceof Pin);
		drag(circuit, selected, 0, 120);
		String afterDrag = save(circuit);
		List<CircuitOp> plan =
				SimpleEditor.moveSelectionPlan(circuit, selected, 0, 120);
		assertTrue(plan != null && plan.size() == 1, "one move expected");
		List<ElementId> ids = ((MoveElements) plan.get(0)).ids();
		assertEquals(2, ids.size(), "both pins must be in the move");
		Set<ElementId> want = new HashSet<ElementId>();
		for (Element el : selected) {
			want.add(el.getStableId());
		}
		assertEquals(want, new HashSet<ElementId>(ids),
				"the move must address exactly the selection");
		assertEquals(afterDrag, save(circuit),
				"planning must never mutate the circuit");
	}

	// ------------------------------------------------------------------
	// fallback: drops the vocabulary must not claim
	// ------------------------------------------------------------------

	/**
	 * A drop whose put lands on a non-selected element's put would make
	 * connect() form a wire, so it has no plan and takes the inline
	 * commit.
	 */
	@Test
	void connectFormingDropHasNoPlan() throws Exception {
		Circuit circuit = loadText(facingPinsText());
		// pin A's put starts at (156,132); a +204 drag lands it exactly
		// on pin B's put at (360,132), the coincidence connect() wires
		Set<Element> selected = select(circuit, el -> el instanceof Pin
				&& "A".equals(el.getName()));
		drag(circuit, selected, 204, 0);
		assertNull(SimpleEditor.moveSelectionPlan(circuit, selected, 204, 0),
				"a drop that forms a connection must have no plan");
	}

	/**
	 * A wired element's move drags its attached wire end and can re-form
	 * the net (the connect()/removeCoLinear() work MoveElements does
	 * not), so a selection carrying an attached put has no plan.
	 */
	@Test
	void wiredElementHasNoPlan() throws Exception {
		Circuit circuit = loadText(wiredPairText());
		Set<Element> selected = select(circuit, el -> el instanceof Pin
				&& "A".equals(el.getName()));
		assertNull(SimpleEditor.moveSelectionPlan(circuit, selected, 0, 120),
				"a wired element must take the inline path");
	}

	/**
	 * A wire (or wire end) in the selection has no plan; the inline path
	 * owns wire-end drag/connect fix-up.
	 */
	@Test
	void wireSelectionHasNoPlan() throws Exception {
		Circuit circuit = loadText(wiredPairText());
		Set<Element> selected = select(circuit, el -> el instanceof Wire);
		assertNull(SimpleEditor.moveSelectionPlan(circuit, selected, 0, 120),
				"a wire selection must take the inline path");
	}

	/**
	 * A subcircuit still has no plan - its op kind is deferred - so the
	 * gesture takes the inline fallback.
	 */
	@Test
	void subCircuitSelectionHasNoPlan() throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		SubCircuit sub = new SubCircuit(circuit);
		circuit.addElement(sub);
		assertNull(SimpleEditor.moveSelectionPlan(circuit, setOf(sub), 0, 12),
				"a subcircuit selection must have no plan");
	}

	/** An empty selection has no plan (the inline path no-ops it). */
	@Test
	void emptySelectionHasNoPlan() throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		assertNull(SimpleEditor.moveSelectionPlan(circuit,
				new HashSet<Element>(), 0, 12),
				"an empty selection must have no plan");
	}

	// ------------------------------------------------------------------
	// rejection: off-canvas moves change nothing
	// ------------------------------------------------------------------

	/**
	 * A MoveElements that would push an element off the canvas is
	 * rejected atomically and leaves the circuit byte-identical.
	 */
	@Test
	void offCanvasMoveIsRejectedAndLeavesBytesIdentical()
			throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		String before = save(circuit);
		Element a = select(circuit, el -> el instanceof Pin
				&& "A".equals(el.getName())).iterator().next();
		MoveElements offCanvas =
				new MoveElements(List.of(a.getStableId()), -360, 0);
		assertThrows(OpRejected.class,
				() -> offCanvas.apply(circuit, graphics()),
				"pin A at x=120 moved by -360 leaves the canvas");
		assertEquals(before, save(circuit),
				"a rejected move must not mutate the circuit");
	}

	// ------------------------------------------------------------------
	// undo granularity: one markChanged per gesture
	// ------------------------------------------------------------------

	/**
	 * A sink shaped like the editor's opSink: submitAll applies every op
	 * then records exactly once, the shape that keeps a move a single
	 * undo snapshot.
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
	 * The move plan submitted as one batch records exactly one
	 * markChanged for the gesture.
	 */
	@Test
	void batchSubmitRecordsExactlyOneMarkChangedPerGesture()
			throws Exception {
		Circuit circuit = loadText(unwiredPairText());
		Set<Element> selected = select(circuit, el -> el instanceof Pin);
		drag(circuit, selected, 0, 120);
		List<CircuitOp> plan =
				SimpleEditor.moveSelectionPlan(circuit, selected, 0, 120);
		assertTrue(plan != null && plan.size() == 1,
				"the relocation must plan to one op");
		for (Element el : selected) {
			el.restorePosition();
		}
		BatchSink sink = new BatchSink(circuit);
		sink.submitAll(plan);
		assertEquals(1, sink.applied, "the single op must apply");
		assertEquals(List.of(1), sink.marks,
				"one markChanged, after all ops, per gesture");
	}
} // end of MoveGestureTest class
