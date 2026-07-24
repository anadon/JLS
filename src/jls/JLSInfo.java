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
	// Model-geometry constants moved to jls.core.Geometry (issue #77):
	// circuitsize -> Geometry.CIRCUITSIZE, spacing -> Geometry.SPACING,
	// pointDiameter -> Geometry.POINT_DIAMETER, stateDiameter ->
	// Geometry.STATE_DIAMETER, arrowSize -> Geometry.ARROW_SIZE.
	// semantic colors moved into the nested Palette holder (issue #77):
	// keeping them out of JLSInfo's own static initializer means the
	// headless core can touch JLSInfo (batch, sim, loadError) without the
	// JVM ever loading java.awt.Color - the Palette's initializer runs
	// only when GUI code first reads a color, which never happens in a
	// batch/headless run. The HeadlessCoreCanaryTest pins that.
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
	// Orientation moved to jls.core.Orientation (issue #77): it is pure
	// model geometry and belongs in the headless core, not the app hub.
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
	 * The GUI editor color theme (issue #76), held in its own class so the
	 * headless core can touch {@link JLSInfo} without the JVM loading
	 * {@code java.awt.Color}: a nested type's static initializer runs only
	 * when that type is first used, and only GUI code ever reads a color.
	 * The fields are mutable statics set from the active {@code Theme};
	 * {@code Theme.apply()} is the only intended writer.
	 */
	public static final class Palette {

		private Palette() {} // no instances

		/** Color used when connections line up (touch). */
		public static Color touchColor = Theme.DEFAULT.touch();
		/** Color used for highlighted elements. */
		public static Color highlightColor = Theme.DEFAULT.highlight();
		/** Color used for selected elements. */
		public static Color selectionColor = Theme.DEFAULT.selection();
		/** Color used for watched elements. */
		public static Color watchColor = Theme.DEFAULT.watch();
		/** Color used for wires carrying a non-zero value. */
		public static Color nonZeroColor = Theme.DEFAULT.nonZero();
		/** Color used for the initial state of a state machine. */
		public static Color initialStateColor = Theme.DEFAULT.initialState();
		/** Color used for wires with no value (HiZ). */
		public static Color wireOffColor = Theme.DEFAULT.wireOff();
		/** Color used for wires with an all-zero value. */
		public static Color wireZeroColor = Theme.DEFAULT.wireZero();
		/** Color of the editor window grid. */
		public static Color gridColor = Theme.DEFAULT.grid();
		/** Color of the editor window background. */
		public static Color backgroundColor = Theme.DEFAULT.background();
	} // end of Palette class

	/**
	 * Whether the run has no window to parent dialogs on: batch mode with
	 * no main frame. Headless-core code (issue #77) asks through this
	 * helper so it never mentions the AWT {@code Frame} type itself - the
	 * one reference stays here, in the app hub, where AWT is allowed.
	 *
	 * @return true in batch mode with no main window.
	 */
	public static boolean noWindow() {
		return batch && frame == null;
	} // end of noWindow method

	/**
	 * Private constructor to keep this class from being instantiated.
	 */
	private JLSInfo() {}

} // end of JLSInfo class
