package jls.core;

/**
 * Fixed model-geometry constants shared by the circuit model, its
 * simulation, and persistence (issue #77). These are pure integers with
 * no GUI dependency — grid spacing, connection-point and state-machine
 * dimensions, and the circuit coordinate extent — relocated here from
 * the former {@code jls.JLSInfo} app hub so headless code can size and
 * place elements without reaching into the GUI layer.
 *
 * <p>The GUI window's own initial size ({@code JLSInfo.windowsize}) is
 * not here — it is a view concern, not model geometry.
 */
public final class Geometry {

	/** Width and height of the (square) circuit drawing area, in pixels. */
	public static final int CIRCUITSIZE = 1000;
	/** Grid spacing used for snap-to positioning, in pixels. */
	public static final int SPACING = 12;
	/** Diameter of input and output connection points, in pixels. */
	public static final int POINT_DIAMETER = 6;
	/** Diameter of state machine states, in pixels. */
	public static final int STATE_DIAMETER = 40;
	/** Size of state machine transition arrowheads, in pixels. */
	public static final int ARROW_SIZE = 6;

	private Geometry() {} // no instances

} // end of Geometry class
