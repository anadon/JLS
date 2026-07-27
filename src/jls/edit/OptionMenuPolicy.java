package jls.edit;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import jls.elem.Editable;
import jls.elem.Element;
import jls.elem.JumpStart;
import jls.elem.QuickEditable;
import jls.elem.Rotatable;
import jls.elem.Timed;
import jls.elem.Watchable;
import jls.elem.Wire;

/**
 * The per-element content policy for the editor's right-click option
 * menu (issue #86, H3 close-out).
 *
 * <p>{@code SimpleEditor.makeOptionMenu} used to decide <em>which</em>
 * items an element's context menu shows inline, between Swing calls, so
 * the decision was untestable headless (the surefire JVM runs with
 * {@code java.awt.headless=true}, where the editor cannot be
 * constructed). This class is that decision, extracted verbatim: a pure
 * function from an element's capability traits (issue #78) and state to
 * an ordered list of typed {@link Entry} values. The editor maps each
 * entry onto its existing shared menu items ({@link EditOp}-backed
 * {@code Action}s from issue #75, the quick-shortcuts submenu, the
 * matching-jump item) without adding or reordering anything, so the
 * menu a user sees is exactly this list.</p>
 *
 * <p>Like {@link DeleteKeyPolicy} and
 * {@link KeyboardConstructionPolicy}, the class names no AWT or Swing
 * type, so every branch is unit-testable headless.</p>
 *
 * @jls.testedby jls.edit.OptionMenuPolicyTest
 */
final class OptionMenuPolicy {

	/** Watch item label when the element is not being watched. */
	static final String WATCH_LABEL = "Watch Element";

	/** Watch item label when the element is already being watched. */
	static final String UNWATCH_LABEL = "Un-watch Element";

	/** Probe item label when the wire has no probe. */
	static final String ATTACH_PROBE_LABEL = "Attach Probe";

	/** Probe item label when the wire already has a probe. */
	static final String REMOVE_PROBE_LABEL = "Remove Probe";

	/** Label of the matching-jump-end item (JumpStart only). */
	static final String MATCH_JUMP_LABEL = "Create Matching End";

	/** Label of the quick-shortcuts submenu (built by
	 * {@link ElementQuickMenus}, which owns the submenu contents). */
	static final String QUICK_LABEL = "shortcuts";

	/**
	 * Prevents instantiation; the class holds only static policy methods.
	 */
	private OptionMenuPolicy() {
		// static use only
	} // end of OptionMenuPolicy constructor

	/**
	 * The kind of surface a menu entry maps to.
	 */
	enum Kind {

		/** The element's quick-shortcuts submenu (issue #77's
		 * {@code ElementQuickMenus} builds it). */
		QUICK,

		/** A menu item backed by an {@link EditOp} shared Action. */
		OP,

		/** The create-matching-jump-end item, the one popup-only
		 * operation without a shared Action (issue #86 note: the last
		 * legacy ActionListener). */
		MATCH_JUMP

	} // end of Kind enum

	/**
	 * One entry of an element's option menu, in menu order.
	 *
	 * @param kind which surface the entry maps to.
	 * @param op the backing operation for {@link Kind#OP} entries,
	 *           null for the other kinds.
	 * @param label the label the item shows; for state-labelled entries
	 *              (watch, probe) it already reflects the element state.
	 */
	record Entry(Kind kind, @Nullable EditOp op, String label) {

		/**
		 * The quick-shortcuts submenu entry.
		 *
		 * @return the QUICK entry.
		 */
		static Entry quick() {

			return new Entry(Kind.QUICK, null, QUICK_LABEL);
		} // end of quick method

		/**
		 * An operation-backed entry with the operation's stable label.
		 *
		 * @param op the backing operation.
		 * @return the OP entry.
		 */
		static Entry of(EditOp op) {

			return new Entry(Kind.OP, op, op.label());
		} // end of of method

		/**
		 * An operation-backed entry with a state-dependent label.
		 *
		 * @param op the backing operation.
		 * @param label the label reflecting the element's state.
		 * @return the OP entry.
		 */
		static Entry of(EditOp op, String label) {

			return new Entry(Kind.OP, op, label);
		} // end of of method

		/**
		 * The create-matching-jump-end entry.
		 *
		 * @return the MATCH_JUMP entry.
		 */
		static Entry matchJump() {

			return new Entry(Kind.MATCH_JUMP, null, MATCH_JUMP_LABEL);
		} // end of matchJump method

		/**
		 * The backing operation of an {@link Kind#OP} entry, for
		 * callers past the kind check (keeps NullAway provable).
		 *
		 * @return the backing operation; never null.
		 * @throws IllegalStateException if the entry is not OP-backed.
		 */
		EditOp requireOp() {

			if (op == null)
				throw new IllegalStateException(
						"entry " + kind + " has no backing operation");
			return op;
		} // end of requireOp method

	} // end of Entry record

	/**
	 * The option-menu content for one element, in menu order. Element
	 * kind decides through capability traits which operations apply;
	 * {@code isUneditable()} suppresses every mutating entry, leaving
	 * view (any {@link Watchable}) and probe (any {@link Wire}) as the
	 * only entries an uneditable element offers. The multi-selection
	 * items (cut/copy/delete/lock) are appended by the editor, not
	 * decided here.
	 *
	 * @param el The element the menu is being shown for.
	 * @return the ordered menu entries; may be empty.
	 */
	static List<Entry> entries(Element el) {

		List<Entry> entries = new ArrayList<Entry>();
		boolean editable = !el.isUneditable();
		if (el instanceof QuickEditable && editable) {
			entries.add(Entry.quick());
		}
		if (el instanceof Editable && editable) {
			entries.add(Entry.of(EditOp.MODIFY));
		}
		if (el instanceof Timed && editable) {
			entries.add(Entry.of(EditOp.TIMING));
		}
		if (el instanceof Watchable watchable) {
			if (editable) {
				entries.add(Entry.of(EditOp.WATCH,
						watchable.isWatched() ? UNWATCH_LABEL : WATCH_LABEL));
			}
			entries.add(Entry.of(EditOp.VIEW_VALUE));
		}
		if (el instanceof Rotatable rot && rot.canRotate() && editable) {
			entries.add(Entry.of(EditOp.ROTATE_CW));
			entries.add(Entry.of(EditOp.ROTATE_CCW));
		}
		if (el instanceof Rotatable rot && rot.canFlip() && editable) {
			entries.add(Entry.of(EditOp.FLIP));
		}
		if (el instanceof JumpStart && editable) {
			entries.add(Entry.matchJump());
		}
		if (el instanceof Wire wire) {
			entries.add(Entry.of(EditOp.PROBE,
					wire.hasProbe() ? REMOVE_PROBE_LABEL : ATTACH_PROBE_LABEL));
		}
		return entries;
	} // end of entries method

} // end of OptionMenuPolicy class
