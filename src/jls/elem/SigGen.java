package jls.elem;

import java.io.*;
import java.util.*;

import jls.*;
import jls.core.Geometry;
import jls.sim.*;

/**
 * Signal generator.
 *
 * @author David A. Poplawski
 */
public final class SigGen extends SigSim implements Editable {

	// named constants
	/** Title drawn inside the element's box. */
	private final String title = " Signal Generator ";
	/** Width and height of the edit dialog's text pane, in pixels. */

	// saved properties
	/** The signal specification text entered by the user. */
	private String signals = "";

	/**
	 * Create new element.
	 *
	 * @param circuit The circuit this element is in.
	 */
	public SigGen(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Initialize this element.
	 *
	 * @param g Unused.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// do nothing if no graphics object
		if (g == null)
			return;

		// do nothing if element already has a size
		if (width != 0 || height != 0)
			return;

		int s = Geometry.SPACING;
		jls.core.TextMetrics fm = g;
		int w = fm.stringWidth(title);
		width = (w+s-1)/s*s;
		height = 2*s;
	} // end of init method

	/**
	 * The signal-generator title text (issue #77: read by the GUI-side
	 * renderer).
	 *
	 * @return the title string.
	 */
	public String getTitle() {

		return title;
	} // end of getTitle method

	/**
	 * The current signal specification (issue #77: read by the GUI-side
	 * dialog).
	 *
	 * @return the signal specification text.
	 */
	public String getSignals() {

		return signals;
	} // end of getSignals method

	/**
	 * Set the signal specification (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param signals The new signal specification text.
	 */
	public void setSignals(String signals) {

		this.signals = signals;
	} // end of setSignals method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT SigGen");
		super.save(output);
		output.println("END");
	} // end of save method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes. The
	// handwritten save escaped backslash, quote and newline exactly as
	// Attribute.StringAttribute does.
	/** Declarations of this element's own saved attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("signals") {
			/**
			 * Read the signals attribute from the given element.
			 *
			 * @param el The SigGen to read from.
			 * @return the element's signal specification.
			 */
			@Override
			protected String get(Element el) { return ((SigGen)el).signals; }
			/**
			 * Store the signals attribute into the given element.
			 *
			 * @param el The SigGen to write to.
			 * @param v The signal specification to store.
			 */
			@Override
			protected void set(Element el, String v) { ((SigGen)el).signals = v; }
		}
	);

	/** Base attributes followed by this element's own, in save order. */
	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Make a copy of this element.
	 *
	 * @return an exact copy of this element.
	 */
	@Override
	public SigGen copy() {

		SigGen it = new SigGen(getCircuit());
		super.copy(it);
		return it;
	} // end of copy method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/**
	 * Parse signal specification and post all events.
	 * If signal generator is in an imported circuit, do nothing.
	 *
	 * @param sim The simulator.
	 */
	@Override
	public void initSim(Simulator sim) {

		// do nothing if in an imported circuit
		if (getCircuit().isImported())
			return;

		Scanner input = new Scanner(signals);

		super.initSim(sim,input);
	} // end of initSim method

	/**
	 * Print or display an error about the signal specification.
	 *
	 * @param msg An error message.
	 */
	@Override
	protected void specError(String msg) {

		if (JLSInfo.noWindow()) {
			System.out.println("error in test file");
			System.out.println(msg);
			System.exit(1);
		}
		else {
			TellUser.error(null,"error in test file: " + msg, "Error");
			return;
		}
	} // end of specError method

} // end of SigGen class
