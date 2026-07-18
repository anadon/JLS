package jls;

import java.awt.Color;
import java.util.List;

/**
 * A named set of semantic editor colors (issue #76).
 *
 * Each component is a semantic role, not a raw color: what a wire
 * carrying a non-zero value looks like, what a connection point about
 * to attach looks like, and so on.  The drawing code keeps reading the
 * corresponding {@link JLSInfo} statics; {@link #apply()} rewrites
 * those statics so every consumer follows the active theme without
 * being edited.
 *
 * Two variants ship today.  {@link #DEFAULT} is the color-vision-safe
 * palette adopted for issue #76: its value/touch pair is orange/blue
 * (built around the Okabe-Ito palette) instead of the classic
 * red/green, which is indistinguishable under deuteranopia.  Every
 * pair of wire states stays at least 25 CIE76 delta-E apart under
 * normal, deuteranopic, and protanopic vision - enforced by
 * {@code ThemeTest}.  {@link #CLASSIC} reproduces the original JLS
 * palette exactly, so no user is forced off the old colors.
 *
 * A dark variant is deliberately absent for now: element bodies and
 * labels are still drawn with hardcoded black in ~126 call sites, so a
 * dark background would render them illegible.  The record already
 * carries every role a dark variant needs; it can be added once the
 * foreground sweep lands.
 *
 * @param name The user-visible theme name (also the persistence key).
 * @param touch Connection points and wires lining up for attachment.
 * @param highlight Highlighted (selected) elements and wires.
 * @param selection Fill behind elements being selected.
 * @param watch Fill marking watched elements.
 * @param nonZero Wires carrying a non-zero value.
 * @param initialState Fill of a state machine's initial state.
 * @param wireOff Wires carrying no value (tri-state HiZ).
 * @param wireZero Wires carrying an all-zero value.
 * @param grid The editor window grid.
 * @param background The editor window background.
 *
 * @author David A. Poplawski
 */
public record Theme(String name, Color touch, Color highlight,
		Color selection, Color watch, Color nonZero, Color initialState,
		Color wireOff, Color wireZero, Color grid, Color background) {

	/**
	 * The color-vision-safe default palette (issue #76): blue for
	 * touching, orange for non-zero values, lavender highlight, gray
	 * HiZ - all wire-state pairs distinguishable under deuteranopia
	 * and protanopia.
	 */
	public static final Theme DEFAULT = new Theme("default",
			new Color(0x00, 0x72, 0xB2),		// touch: Okabe-Ito blue
			new Color(0xB8, 0xA5, 0xE3),		// highlight: lavender
			new Color(240, 240, 240),			// selection
			new Color(0x56, 0xB4, 0xE9),		// watch: sky blue
			new Color(0xE6, 0x9F, 0x00),		// nonZero: Okabe-Ito orange
			Color.lightGray,					// initialState
			new Color(0x70, 0x70, 0x70),		// wireOff: gray (drawn dashed)
			Color.black,						// wireZero
			new Color(240, 240, 240),			// grid
			Color.white);						// background

	/**
	 * The original JLS palette, exactly as hardcoded before issue #76,
	 * kept selectable for continuity.
	 */
	public static final Theme CLASSIC = new Theme("classic",
			Color.green,						// touch
			Color.pink,							// highlight
			new Color(240, 240, 240),			// selection
			Color.cyan,							// watch
			Color.red,							// nonZero
			Color.lightGray,					// initialState
			Color.blue,							// wireOff
			Color.black,						// wireZero
			new Color(240, 240, 240),			// grid
			Color.white);						// background

	/** All shipped themes, in menu order. */
	private static final List<Theme> ALL = List.of(DEFAULT, CLASSIC);

	/** The theme most recently applied (never null). */
	private static Theme active = DEFAULT;

	/**
	 * All shipped themes, in the order they should be offered to the
	 * user.
	 *
	 * @return an immutable list of every selectable theme.
	 */
	public static List<Theme> all() {

		return ALL;
	} // end of all method

	/**
	 * The theme most recently applied via {@link #apply()}, which is
	 * {@link #DEFAULT} until any theme is applied.
	 *
	 * @return the active theme, never null.
	 */
	public static Theme active() {

		return active;
	} // end of active method

	/**
	 * The theme with the given name, or {@link #DEFAULT} when the name
	 * is unknown or null (e.g. a stale persisted preference).
	 *
	 * @param name The theme name to look up.
	 *
	 * @return the named theme, or the default theme as a fallback.
	 */
	public static Theme byName(String name) {

		for (Theme theme : ALL) {
			if (theme.name.equals(name)) {
				return theme;
			}
		}
		return DEFAULT;
	} // end of byName method

	/**
	 * Make this theme the active one by rewriting the {@link JLSInfo}
	 * color statics every draw call site reads.  Callers are
	 * responsible for repainting open editors afterwards.
	 */
	public void apply() {

		install(this);
	} // end of apply method

	/**
	 * Rewrite the {@link JLSInfo} color statics from a theme and
	 * record it as the active one.
	 *
	 * @param theme The theme to install.
	 */
	private static void install(Theme theme) {

		JLSInfo.touchColor = theme.touch;
		JLSInfo.highlightColor = theme.highlight;
		JLSInfo.selectionColor = theme.selection;
		JLSInfo.watchColor = theme.watch;
		JLSInfo.nonZeroColor = theme.nonZero;
		JLSInfo.initialStateColor = theme.initialState;
		JLSInfo.wireOffColor = theme.wireOff;
		JLSInfo.wireZeroColor = theme.wireZero;
		JLSInfo.gridColor = theme.grid;
		JLSInfo.backgroundColor = theme.background;
		active = theme;
	} // end of install method

} // end of Theme record
