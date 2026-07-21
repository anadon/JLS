package jls.edit;

import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.KeyStroke;

/**
 * The keyboard-only construction bindings and pure geometry that let a
 * user build a circuit with no pointing device (issue #75, the H2 half).
 *
 * <p>The interactive editor is normally driven by the mouse: elements are
 * dropped, dragged, and wired by pointing. This policy supplies the
 * platform-independent pieces the editor needs to offer the same
 * capabilities from the keyboard - an arrow-key grid caret, Enter to
 * place or commit, arrow-key nudging of a selection, and W to start a
 * wire at the caret - so a keyboard user can lay out and wire the
 * tutorial's first two-gate circuit unaided.</p>
 *
 * <p>Every method here is a pure, Swing-free, headless-safe function of
 * its arguments (no {@code Toolkit}, no component state), which is why it
 * lives beside {@link DeleteKeyPolicy} rather than inside the Swing
 * editor: the arrow-to-direction mapping and the grid-step arithmetic can
 * then be unit-tested without a display. The editor owns the stateful
 * half (the caret position, the wire state machine); this class owns only
 * the decisions.</p>
 *
 * @jls.testedby jls.edit.KeyboardConstructionPolicyTest
 */
public final class KeyboardConstructionPolicy {

	/**
	 * Prevents instantiation; the class holds only static policy methods
	 * and the {@link Nudge} enum.
	 */
	private KeyboardConstructionPolicy() {
		// static use only
	} // end of KeyboardConstructionPolicy constructor

	/**
	 * One of the four grid directions an arrow key nudges the caret, a
	 * placing element, a wire end, or the selection in (issue #75). The
	 * enum keeps the arrow-key-to-delta decision in one pure place so the
	 * editor's several arrow-driven modes (caret motion, placement
	 * nudging, wire extension, selection nudging) all agree on which way
	 * each key moves and never drift.
	 */
	public enum Nudge {

		/** Toward smaller y (up the screen). */
		UP(0, -1),
		/** Toward larger y (down the screen). */
		DOWN(0, 1),
		/** Toward smaller x (left). */
		LEFT(-1, 0),
		/** Toward larger x (right). */
		RIGHT(1, 0);

		/** The x sign of this direction (-1, 0, or +1). */
		private final int sx;
		/** The y sign of this direction (-1, 0, or +1). */
		private final int sy;

		/**
		 * Create a direction with its unit x and y signs.
		 *
		 * @param sx the x sign (-1, 0, or +1).
		 * @param sy the y sign (-1, 0, or +1).
		 */
		Nudge(int sx, int sy) {

			this.sx = sx;
			this.sy = sy;
		} // end of Nudge constructor

		/**
		 * The direction an arrow key requests, if any. Only the four
		 * arrow keys map; every other key code yields an empty result so
		 * the editor can let non-arrow keys fall through to their own
		 * bindings.
		 *
		 * @param vk a {@link KeyEvent} VK_ key code.
		 * @return the direction, or empty if the key is not an arrow.
		 */
		public static Optional<Nudge> fromKey(int vk) {

			switch (vk) {
				case KeyEvent.VK_UP:
					return Optional.of(UP);
				case KeyEvent.VK_DOWN:
					return Optional.of(DOWN);
				case KeyEvent.VK_LEFT:
					return Optional.of(LEFT);
				case KeyEvent.VK_RIGHT:
					return Optional.of(RIGHT);
				default:
					return Optional.empty();
			}
		} // end of fromKey method

		/**
		 * The x movement for this direction over one grid step.
		 *
		 * @param step the grid step size in model units (never negative).
		 * @return the signed x delta (a multiple of the step).
		 */
		public int dx(int step) {

			return sx * step;
		} // end of dx method

		/**
		 * The y movement for this direction over one grid step.
		 *
		 * @param step the grid step size in model units (never negative).
		 * @return the signed y delta (a multiple of the step).
		 */
		public int dy(int step) {

			return sy * step;
		} // end of dy method

	} // end of Nudge enum

	/**
	 * The keystroke that places a following element, commits a wire
	 * vertex, or activates the caret (issue #75): plain Enter, no
	 * modifier, on every platform. Enter is the universal "commit" key and
	 * the canvas has no text field to steal it from.
	 *
	 * @return the commit keystroke.
	 *
	 * @jls.testedby jls.edit.KeyboardConstructionPolicyTest#commitStrokeIsPlainEnter()
	 */
	public static KeyStroke commitStroke() {

		return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
	} // end of commitStroke method

	/**
	 * Snap a model coordinate down to the nearest grid line at or below
	 * it (issue #75). The keyboard caret only ever visits grid points, so
	 * that a wire started or ended at the caret lands exactly on the grid
	 * ports elements expose - which is how the editor's coincidence-based
	 * {@code connect} joins port to port with no pointing.
	 *
	 * @param coord a model coordinate.
	 * @param step the grid step size in model units (must be positive).
	 * @return the largest multiple of the step not greater than coord.
	 *
	 * @jls.testedby jls.edit.KeyboardConstructionPolicyTest#snapRoundsDownToGrid()
	 */
	public static int snap(int coord, int step) {

		return Math.floorDiv(coord, step) * step;
	} // end of snap method

} // end of KeyboardConstructionPolicy class
