package jls;

import jls.sim.*;
import java.awt.*;

/**
 * Constants for JLS.
 * 
 * @author David A. Poplawski
 */
public final class JLSInfo {

	// version identity, single-sourced from the pom: the build filters
	// the project version into jls/version.properties (issue #36); the
	// "dev" fallback covers IDE/exploded runs without the resource
	public static final String versionString = loadVersion();

	public static final String version = "JLS " + versionString;

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
	public static final int windowsize = 600;
	public static final int circuitsize = 1000;			// square circuit (can be increased)
	public static final int spacing = 12;					// for snap-to
	public static final int pointDiameter = 6;			// for inputs and outputs
	public static final int stateDiameter = 40;			// state machine states
	public static final int arrowSize = 6;				// state machine arrows
	public static final Color touchColor = Color.green;	// when connections line up
	public static final Color highlightColor =
		Color.pink;										// when elements are highlighted
	public static final Color selectionColor = 
		new Color(240,240,240);							// when elements are selected
	public static final Color watchColor =
		Color.cyan;										// watched elements
	public static final Color nonZeroColor =
		Color.red;										// wires with non-zero values
	public static final Color initialStateColor =
		Color.lightGray;									// initial states of state machines
	public static final int checkPointFreq = 10;			// how many changes between checkpoint file writes
	public static final int undoStackDepth = 10;			// maximum number of undos
	public static final long defaultTimeLimit = 100000000;		// default simulation time
	public static Frame frame = null;					// for dialog boxes
	public static Simulator sim = null;					// for undo/redo
	public static boolean batch = false;				// batch mode
	public static boolean printTrace = false;			// print signal trace
	public static boolean imgexport = false;			// export image from command line
	public enum Orientation { UP, DOWN, LEFT, RIGHT;

		/**
		 * The orientation after a quarter-turn counterclockwise (what
		 * rotating an element "left" does to each of its orientations).
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
	public static Color gridColor = 
		new Color(240,240,240);							// editor window grid
	public static Color backgroundColor =  Color.white;	// editor window grid
	public static String loadError = "";				// error message when loading a circuit
	
	/**
	 * Private constructor to keep this class from being instantiated.
	 */
	private JLSInfo() {}

} // end of JLSInfo class
