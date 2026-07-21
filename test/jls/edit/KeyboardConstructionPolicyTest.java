package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import jls.JLSInfo;
import jls.edit.KeyboardConstructionPolicy.Nudge;

/**
 * Headless unit tests for the pure keyboard-construction policy
 * (issue #75, H2). The arrow-to-direction mapping, the grid-step
 * arithmetic, the commit keystroke, and the grid snap are all pure
 * functions with no display dependency, so they are pinned here rather
 * than in the display suite. Each assertion would fail if a sign or a
 * key mapping were flipped, so the policy cannot silently drift from the
 * editor that relies on it.
 */
class KeyboardConstructionPolicyTest {

	/** Each arrow key maps to its own direction; nothing else does. */
	@Test
	void arrowKeysMapToDirections() {

		assertEquals(Optional.of(Nudge.UP),
				Nudge.fromKey(KeyEvent.VK_UP));
		assertEquals(Optional.of(Nudge.DOWN),
				Nudge.fromKey(KeyEvent.VK_DOWN));
		assertEquals(Optional.of(Nudge.LEFT),
				Nudge.fromKey(KeyEvent.VK_LEFT));
		assertEquals(Optional.of(Nudge.RIGHT),
				Nudge.fromKey(KeyEvent.VK_RIGHT));
	} // end of arrowKeysMapToDirections method

	/** A non-arrow key yields no direction, so it can fall through. */
	@Test
	void nonArrowKeyMapsToNothing() {

		assertTrue(Nudge.fromKey(KeyEvent.VK_ENTER).isEmpty());
		assertTrue(Nudge.fromKey(KeyEvent.VK_W).isEmpty());
		assertTrue(Nudge.fromKey(KeyEvent.VK_A).isEmpty());
	} // end of nonArrowKeyMapsToNothing method

	/**
	 * The deltas carry the right sign and magnitude: up/down move only in
	 * y, left/right only in x, each by exactly one step.
	 */
	@Test
	void deltasHaveCorrectSigns() {

		int step = 12;

		assertEquals(0, Nudge.UP.dx(step));
		assertEquals(-step, Nudge.UP.dy(step));

		assertEquals(0, Nudge.DOWN.dx(step));
		assertEquals(step, Nudge.DOWN.dy(step));

		assertEquals(-step, Nudge.LEFT.dx(step));
		assertEquals(0, Nudge.LEFT.dy(step));

		assertEquals(step, Nudge.RIGHT.dx(step));
		assertEquals(0, Nudge.RIGHT.dy(step));
	} // end of deltasHaveCorrectSigns method

	/** UP and DOWN, LEFT and RIGHT, are exact opposites. */
	@Test
	void oppositeDirectionsCancel() {

		int step = JLSInfo.spacing;

		assertEquals(0, Nudge.UP.dy(step) + Nudge.DOWN.dy(step));
		assertEquals(0, Nudge.LEFT.dx(step) + Nudge.RIGHT.dx(step));
	} // end of oppositeDirectionsCancel method

	/** The commit keystroke is plain Enter, no modifier. */
	@Test
	void commitStrokeIsPlainEnter() {

		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
				KeyboardConstructionPolicy.commitStroke());
	} // end of commitStrokeIsPlainEnter method

	/**
	 * The snap rounds a coordinate down to the grid, including negative
	 * and already-on-grid values, so a wire started at the caret lands on
	 * the grid ports elements expose.
	 */
	@Test
	void snapRoundsDownToGrid() {

		int step = JLSInfo.spacing;

		assertEquals(0, KeyboardConstructionPolicy.snap(0, step));
		assertEquals(0, KeyboardConstructionPolicy.snap(step - 1, step));
		assertEquals(step, KeyboardConstructionPolicy.snap(step, step));
		assertEquals(step, KeyboardConstructionPolicy.snap(step + 1, step));
		assertEquals(2 * step,
				KeyboardConstructionPolicy.snap(2 * step + step / 2, step));
		assertEquals(-step, KeyboardConstructionPolicy.snap(-1, step));

		// the snapped value is always a grid multiple at or below input
		int snapped = KeyboardConstructionPolicy.snap(5 * step + 7, step);
		assertEquals(0, snapped % step);
		assertTrue(snapped <= 5 * step + 7);
		assertFalse(snapped + step <= 5 * step + 7);
	} // end of snapRoundsDownToGrid method

} // end of KeyboardConstructionPolicyTest class
