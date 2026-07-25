package jls.elem;

import jls.core.Geometry;
import jls.*;
import jls.core.Orientation;

import java.util.*;

/**
 * Delay gate.
 * Logically neutral, simply delays a signal change by a given amount.
 * 
 * @author David A. Poplawski
 */
public final class DelayGate extends Gate implements Timed {
	
	// identity (#22); previous-settings unused: DELAY has its own dialog
	/** The kind descriptor (names and pin counts) for DELAY gates. */
	private static final Kind KIND =
			new Kind("DELAY","DelayGate",1,0);
	
	/**
	 * The kind descriptor for this gate.
	 */
	@Override
	protected Kind kind() {
		
		return KIND;
	} // end of kind method
	
	/**
	 * Create DELAY gate.
	 *
	 * @param circuit The circuit this DELAY gate is in.
	 */
	public DelayGate(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The DELAY gate body: a right-pointing triangle (issue #77 model/render
	 * split - the symbol as headless data). Byte-identical to the former
	 * inline {@code GeneralPath}: two connected lines then a connected line
	 * back, closed, so {@code GateRenderer.gatePathFrom(GateOutline)} rebuilds the
	 * same closed path.
	 */
	@Override
	public GateOutline outline() {

		int s = Geometry.SPACING;
		return GateOutline.builder()
				.line(false, 0, 0, 2*s, s)
				.line(true, 2*s, s, 0, 2*s)
				.line(true, 0, 2*s, 0, 0)
				.close()
				.build();
	} // end of outline method
	
	/**
	 * Set the number of gates (bits). Used by the GUI-side creation dialog
	 * ({@code jls.edit.DelayGateDialog}) after the model went headless.
	 *
	 * @param bits The number of gates (bits).
	 */
	@Override
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the number of inputs. Used by the GUI-side creation dialog.
	 *
	 * @param numInputs The number of inputs.
	 */
	@Override
	public void setNumInputs(int numInputs) {

		this.numInputs = numInputs;
	} // end of setNumInputs method

	/**
	 * Set the orientation. Used by the GUI-side creation dialog.
	 *
	 * @param orientation The orientation this gate faces.
	 */
	@Override
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Display info about this DELAY gate.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {
		
		String pd = ", delay = " + propDelay;
		if (bits == 1)
			return "DELAY gate" + pd;
		else
			return bits + " DELAY gates" + pd;
	} // end of showInfo method
	
	/**
	 * Cannot reset propagation delay.
	 */
	@Override
	public void resetPropDelay() {
		
		// do nothing
	} // end of resetPropDelay method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * A delay gate outputs its input unchanged (after the propagation delay).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = inputs.get(0).getValue();
		if (value == null)
			return new BitSet();
		return (BitSet)value.clone();
	} // end of computeOutput method


} // end of DelayGate class
