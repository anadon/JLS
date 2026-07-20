package jls;

import jls.sim.*;
import java.awt.*;

import org.jspecify.annotations.Nullable;

/**
 * Constants for JLS.
 * 
 * @author David A. Poplawski
 */
public final class JLSInfo {

	// version identity, single-sourced from the pom: the build filters
	// the project version into jls/version.properties (issue #36); the
	// "dev" fallback covers IDE/exploded runs without the resource
	/** The project version, single-sourced from the pom (or "dev" when unavailable). */
	public static final String versionString = loadVersion();

	/** The program name and version ("JLS " followed by the version string). */
	public static final String version = "JLS " + versionString;

	/**
	 * Read the project version single-sourced from the build-filtered
	 * jls/version.properties resource, falling back to "dev" when the
	 * resource is missing, unreadable, or still holds the unfiltered
	 * "${...}" placeholder (issue #36).
	 *
	 * @return the resolved version string, or "dev" if none is available.
	 */
	private static String loadVersion() {

		try (java.io.InputStream in =
				JLSInfo.class.getResourceAsStream("/jls/version.properties")) {
			if (in != null) {
				java.util.Properties props = new java.util.Properties();
				props.load(in);
				String v = props.getProperty("version");
				if (v != null && !v.isEmpty() && !v.startsWith("${")) {
					return v;
				}
			}
		}
		catch (java.io.IOException ignored) {
		}
		return "dev";
	} // end of loadVersion method
	
	// miscellaneous parameters
	/** Initial width and height of the main window, in pixels. */
	public static final int windowsize = 600;
	/** Width and height of the (square) circuit drawing area, in pixels. */
	public static final int circuitsize = 1000;			// square circuit (can be increased)
	/** Grid spacing used for snap-to positioning, in pixels. */
	public static final int spacing = 12;					// for snap-to
	/** Diameter of input and output connection points, in pixels. */
	public static final int pointDiameter = 6;			// for inputs and outputs
	/** Diameter of state machine states, in pixels. */
	public static final int stateDiameter = 40;			// state machine states
	/** Size of state machine transition arrowheads, in pixels. */
	public static final int arrowSize = 6;				// state machine arrows
	// semantic colors; set from the active Theme (issue #76), so they
	// are mutable statics like gridColor below. Theme.apply() is the
	// only intended writer.
	/** Color used when connections line up (touch). */
	public static Color touchColor =
		Theme.DEFAULT.touch();							// when connections line up
	/** Color used for highlighted elements. */
	public static Color highlightColor =
		Theme.DEFAULT.highlight();						// when elements are highlighted
	/** Color used for selected elements. */
	public static Color selectionColor =
		Theme.DEFAULT.selection();						// when elements are selected
	/** Color used for watched elements. */
	public static Color watchColor =
		Theme.DEFAULT.watch();							// watched elements
	/** Color used for wires carrying a non-zero value. */
	public static Color nonZeroColor =
		Theme.DEFAULT.nonZero();							// wires with non-zero values
	/** Color used for the initial state of a state machine. */
	public static Color initialStateColor =
		Theme.DEFAULT.initialState();					// initial states of state machines
	/** Color used for wires with no value (HiZ). */
	public static Color wireOffColor =
		Theme.DEFAULT.wireOff();							// wires with no value (HiZ)
	/** Color used for wires with an all-zero value. */
	public static Color wireZeroColor =
		Theme.DEFAULT.wireZero();						// wires with an all-zero value
	/** Number of circuit changes between checkpoint file writes. */
	public static final int checkPointFreq = 10;			// how many changes between checkpoint file writes
	/** Maximum number of undos remembered. */
	public static int undoStackDepth = 10;			// maximum number of undos
	/** Default simulation time limit. */
	public static final long defaultTimeLimit = 100000000;		// default simulation time
	/**
	 * The main window frame, used as the parent of dialog boxes.
	 * Null until the main window is created; set once at startup and
	 * never cleared thereafter.
	 */
	public static @Nullable Frame frame = null;					// for dialog boxes
	/**
	 * The simulator, referenced by undo/redo. Null until a simulation
	 * is started; may be reassigned across simulation runs and reverts
	 * to null when no simulation is active.
	 */
	public static @Nullable Simulator sim = null;					// for undo/redo
	/** True when JLS is running in batch mode. */
	public static boolean batch = false;				// batch mode
	/** True when a signal trace should be printed. */
	public static boolean printTrace = false;			// print signal trace
	/** True when exporting an image from the command line. */
	public static boolean imgexport = false;			// export image from command line
	/** True when exporting HDL from the command line (issue #60). */
	public static boolean hdlexport = false;			// export HDL from command line (#60)
	/** True when re-saving a circuit as plain text from the command line (issue #129). */
	public static boolean textsave = false;				// re-save as plain text from command line (#129)
	/**
	 * The four cardinal directions an element can face in the editor,
	 * used to drive drawing and to rotate or flip elements.
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
	}
	/** Color of the editor window grid. */
	public static Color gridColor =
		Theme.DEFAULT.grid();							// editor window grid
	/** Color of the editor window background. */
	public static Color backgroundColor =
		Theme.DEFAULT.background();						// editor window background
	/** Error message from the most recent circuit load failure, or "" if none. */
	public static String loadError = "";				// error message when loading a circuit
	/**
	 * Structured detail behind loadError, or null if none (issue #58).
	 * Null until a circuit load fails; cleared back to null when a load
	 * succeeds or when the failure is otherwise reset.
	 */
	public static @Nullable LoadError lastLoadError = null;		// structured detail behind loadError (#58)

	/**
	 * Publish a load failure (or clear it with null). The legacy
	 * {@link #loadError} string stays in sync as a derived view so
	 * existing readers keep working; new code should prefer the
	 * structured {@link #lastLoadError} (issue #58 addendum).
	 *
	 * @param error The load error, or null to clear the state.
	 */
	public static void setLoadError(@Nullable LoadError error) {

		lastLoadError = error;
		loadError = error == null ? "" : error.render();
	} // end of setLoadError method

	/**
	 * Private constructor to keep this class from being instantiated.
	 */
	private JLSInfo() {}

} // end of JLSInfo class
