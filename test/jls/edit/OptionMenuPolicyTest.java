package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.edit.OptionMenuPolicy.Entry;
import jls.edit.OptionMenuPolicy.Kind;
import jls.elem.AndGate;
import jls.elem.Constant;
import jls.elem.Element;
import jls.elem.JumpStart;
import jls.elem.Register;
import jls.elem.Wire;

/**
 * Headless pins for the right-click option menu's per-element content
 * (issue #86, H3 close-out / P6). Before the {@link OptionMenuPolicy}
 * extraction this decision lived inline in
 * {@code SimpleEditor.makeOptionMenu} between Swing calls, so no
 * headless test could state which items an AND gate, a constant, a
 * wire, or a locked element actually offers. Each test builds real
 * elements through the circuit loader (the same idiom as
 * {@link DragCandidateBoundTest}) and asserts the exact entry list,
 * including the state-dependent watch and probe labels. Menu
 * <em>dispatch</em> is display-territory and stays pinned by
 * {@code jls.ui.PopupOperationBehaviorTest} and
 * {@code jls.ui.EditActionMatrixTest} on the xvfb substrate.
 */
class OptionMenuPolicyTest {

	/**
	 * Load a built circuit text, failing loudly on loader errors.
	 *
	 * @param text the circuit text to load.
	 * @return the loaded circuit.
	 */
	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("policy");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * The sole element of the given class in the circuit.
	 *
	 * @param <T> the element class.
	 * @param circuit the circuit to search.
	 * @param type the element class to find.
	 * @return the one element of that class.
	 */
	private static <T> T sole(Circuit circuit, Class<T> type) {
		T found = null;
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				if (found != null) {
					throw new AssertionError(
							"more than one " + type.getSimpleName());
				}
				found = type.cast(el);
			}
		}
		if (found == null) {
			throw new AssertionError("no " + type.getSimpleName()
					+ " in the circuit");
		}
		return found;
	}

	/** An unattached 2-input AND gate. */
	private static AndGate andGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		return sole(load(cb.build()), AndGate.class);
	}

	/** An unattached constant. */
	private static Constant constant() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.constant(1);
		return sole(load(cb.build()), Constant.class);
	}

	/** An unattached D register, loaded as watched ("int watch 1"). */
	private static Register register() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.register(4, 0, "D");
		return sole(load(cb.build()), Register.class);
	}

	/** An unattached, unwatched jump start. */
	private static JumpStart jumpStart() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.jumpStart("j", 1);
		return sole(load(cb.build()), JumpStart.class);
	}

	/** A wire between a constant and an output pin. */
	private static Wire wire() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int c = cb.constant(1);
		int pin = cb.outputPin("out", 1);
		cb.wire(c, "output", pin, "input");
		return sole(load(cb.build()), Wire.class);
	}

	/** The backing ops of the OP entries, in menu order. */
	private static List<EditOp> ops(List<Entry> entries) {
		return entries.stream()
				.filter(e -> e.kind() == Kind.OP)
				.map(Entry::requireOp)
				.toList();
	}

	/** The one entry backed by the given op. */
	private static Entry entryFor(List<Entry> entries, EditOp op) {
		return entries.stream()
				.filter(e -> e.kind() == Kind.OP && e.requireOp() == op)
				.reduce((a, b) -> {
					throw new AssertionError("duplicate " + op);
				})
				.orElseThrow(() -> new AssertionError("no " + op));
	}

	/**
	 * An AND gate offers timing plus both rotates and flip - and no
	 * modify: gates do not implement {@code Editable}, exactly as the
	 * pre-extraction makeOptionMenu behaved (the extraction is
	 * behavior-preserving).
	 */
	@Test
	void andGateGetsTimingRotatesAndFlip() throws Exception {
		List<Entry> entries = OptionMenuPolicy.entries(andGate());
		assertEquals(List.of(EditOp.TIMING, EditOp.ROTATE_CW,
				EditOp.ROTATE_CCW, EditOp.FLIP), ops(entries));
		assertEquals(entries.size(), ops(entries).size(),
				"gate menu must contain only OP entries");
	}

	/**
	 * A constant leads with its quick-shortcuts submenu, then modify,
	 * then the reorientation items.
	 */
	@Test
	void constantLeadsWithQuickShortcutsSubmenu() throws Exception {
		List<Entry> entries = OptionMenuPolicy.entries(constant());
		assertEquals(Kind.QUICK, entries.get(0).kind(),
				"quick-shortcuts submenu must come first");
		assertEquals(OptionMenuPolicy.QUICK_LABEL, entries.get(0).label());
		assertEquals(List.of(EditOp.MODIFY, EditOp.ROTATE_CW,
				EditOp.ROTATE_CCW, EditOp.FLIP), ops(entries));
	}

	/**
	 * A wire offers exactly one entry - probe - and its label follows
	 * the probe state: Attach without one, Remove with one.
	 */
	@Test
	void wireProbeLabelFollowsProbeState() throws Exception {
		Wire wire = wire();
		List<Entry> bare = OptionMenuPolicy.entries(wire);
		assertEquals(1, bare.size(), "a wire's menu is probe only");
		assertEquals(OptionMenuPolicy.ATTACH_PROBE_LABEL,
				entryFor(bare, EditOp.PROBE).label());

		wire.attachProbe("p0");
		assertEquals(OptionMenuPolicy.REMOVE_PROBE_LABEL,
				entryFor(OptionMenuPolicy.entries(wire),
						EditOp.PROBE).label());
	}

	/**
	 * A watchable element's watch entry flips its label with the
	 * watched state, and view-contents is always present alongside.
	 */
	@Test
	void watchLabelFollowsWatchedState() throws Exception {
		Register reg = register();
		assertTrue(reg.isWatched(), "builder loads the register watched");
		List<Entry> watched = OptionMenuPolicy.entries(reg);
		assertEquals(OptionMenuPolicy.UNWATCH_LABEL,
				entryFor(watched, EditOp.WATCH).label());
		entryFor(watched, EditOp.VIEW_VALUE);

		reg.setWatched(false);
		assertEquals(OptionMenuPolicy.WATCH_LABEL,
				entryFor(OptionMenuPolicy.entries(reg),
						EditOp.WATCH).label());
	}

	/**
	 * The full register menu, in order: modify, timing, watch, view,
	 * rotates, flip (Editable + Timed + Watchable + Rotatable).
	 */
	@Test
	void registerGetsTheFullEditableMenu() throws Exception {
		assertEquals(List.of(EditOp.MODIFY, EditOp.TIMING, EditOp.WATCH,
				EditOp.VIEW_VALUE, EditOp.ROTATE_CW, EditOp.ROTATE_CCW,
				EditOp.FLIP), ops(OptionMenuPolicy.entries(register())));
	}

	/**
	 * A jump start gets the popup-only matching-end entry, after its
	 * watch/view/flip entries (it flips but never rotates).
	 */
	@Test
	void jumpStartGetsMatchingEndEntry() throws Exception {
		List<Entry> entries = OptionMenuPolicy.entries(jumpStart());
		assertEquals(List.of(EditOp.WATCH, EditOp.VIEW_VALUE, EditOp.FLIP),
				ops(entries));
		Entry last = entries.get(entries.size() - 1);
		assertEquals(Kind.MATCH_JUMP, last.kind());
		assertEquals(OptionMenuPolicy.MATCH_JUMP_LABEL, last.label());
	}

	/**
	 * Uneditable (locked) elements keep only their non-mutating
	 * entries: view-contents on a watchable element, probe on a wire;
	 * everything else disappears.
	 */
	@Test
	void uneditableElementsKeepOnlyViewAndProbe() throws Exception {
		Register reg = register();
		reg.makeUneditable();
		List<Entry> regEntries = OptionMenuPolicy.entries(reg);
		assertEquals(List.of(EditOp.VIEW_VALUE), ops(regEntries));
		assertEquals(1, regEntries.size());

		Wire wire = wire();
		wire.makeUneditable();
		List<Entry> wireEntries = OptionMenuPolicy.entries(wire);
		assertEquals(List.of(EditOp.PROBE), ops(wireEntries));
		assertEquals(1, wireEntries.size());

		JumpStart js = jumpStart();
		js.makeUneditable();
		assertEquals(List.of(EditOp.VIEW_VALUE),
				ops(OptionMenuPolicy.entries(js)),
				"locked jump start loses watch, flip, and matching-end");
	}

	/**
	 * Only OP entries carry a backing operation; asking the other
	 * kinds is a programming error and fails fast (the editor calls
	 * {@code requireOp()} only behind its kind switch).
	 */
	@Test
	void nonOpEntriesRefuseRequireOp() throws Exception {
		assertThrows(IllegalStateException.class,
				() -> Entry.quick().requireOp());
		assertThrows(IllegalStateException.class,
				() -> Entry.matchJump().requireOp());
	}
}
