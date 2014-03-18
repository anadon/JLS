package jls;

import jls.sim.*;
import java.awt.*;
import javax.help.*;

/**
 * Constants for JLS.
 * 
 * @author David A. Poplawski
 */
public final class JLSInfo { 
	
	// version info


	public static final String build = "<p>[built on March 18, 2014 at 11:30 AM]";
	public static final int vers = 4;
	public static final int release = 1;
	public static final int buildNum = 5;
	public static final int year = 2014;
	
	public static final String version = "JLS " + vers + "." + release + 
		" (Michigan Technological University)";
	
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
	public static boolean isApplet = false;				// set to true by applet
	public static boolean batch = false;				// batch mode
	public static boolean printTrace = false;			// print signal trace
	public static boolean imgexport = false;			// export image from command line
	public static HelpBroker hb = null;
	public enum Orientation { UP, DOWN, LEFT, RIGHT; }
	public static Color gridColor = 
		new Color(240,240,240);							// editor window grid
	public static Color backgroundColor =  Color.white;	// editor window grid
	public static String loadError = "";				// error message when loading a circuit
	
	/**
	 * Private constructor to keep this class from being instantiated.
	 */
	private JLSInfo() {}

} // end of JLSInfo class
