package jls.core;

/**
 * The four cardinal directions an element can face in the editor,
 * used to drive drawing and to rotate or flip elements.
 *
 * <p>Relocated from {@code jls.JLSInfo.Orientation} to the headless
 * {@code jls.core} package (issue #77): orientation is pure model
 * geometry with no GUI dependency, so it belongs in the core the
 * simulator and persistence layers share.
 */
public enum Orientation {
	/** Facing up. */
	UP,
	/** Facing down. */
	DOWN,
	/** Facing left. */
	LEFT,
	/** Facing right. */
	RIGHT;

	/**
	 * The orientation after a quarter-turn counterclockwise (what
	 * rotating an element "left" does to each of its orientations).
	 *
	 * @return the orientation one quarter-turn counterclockwise from this one.
	 */
	public Orientation ccw() {
		switch (this) {
		case LEFT: return DOWN;
		case DOWN: return RIGHT;
		case RIGHT: return UP;
		default: return LEFT;
		}
	} // end of ccw method

	/**
	 * The orientation after a quarter-turn clockwise (rotating an
	 * element "right").
	 *
	 * @return the orientation one quarter-turn clockwise from this one.
	 */
	public Orientation cw() {
		switch (this) {
		case LEFT: return UP;
		case UP: return RIGHT;
		case RIGHT: return DOWN;
		default: return LEFT;
		}
	} // end of cw method

	/**
	 * The opposite orientation (what flipping an element does).
	 *
	 * @return the opposite orientation.
	 */
	public Orientation flipped() {
		switch (this) {
		case LEFT: return RIGHT;
		case RIGHT: return LEFT;
		case UP: return DOWN;
		default: return UP;
		}
	} // end of flipped method

	/**
	 * The orientation named by a saved-file string, or the given
	 * current value if the string names none (the handwritten
	 * loaders always ignored unknown strings).
	 *
	 * @param value The orientation name read from a saved file.
	 * @param current The orientation to fall back on if value names none.
	 * @return the orientation named by value, or current if there is no match.
	 */
	public static Orientation parse(String value, Orientation current) {
		for (Orientation o : values()) {
			if (o.toString().equals(value)) {
				return o;
			}
		}
		return current;
	} // end of parse method
} // end of Orientation enum
