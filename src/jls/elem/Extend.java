package jls.elem;

import jls.core.Geometry;
import jls.*;
import jls.sim.*;
import java.io.*;
import java.util.*;

/**
 * 1-input, n-outputs, where all outputs are equal to the input.
 * 
 * @author David A. Poplawski
 */
public final class Extend extends Gate implements TriProp {
	
	// identity (#22); previous-settings unused: Extend has no repeat button
	/** The kind descriptor shared by all Extend gates. */
	private static final Kind KIND =
			new Kind("Extend","Extend",1,0);
	
	/**
	 * The kind descriptor for this gate.
	 */
	@Override
	protected Kind kind() {
		
		return KIND;
	} // end of kind method
	
	// properties
	/** Saved tri-state flag from the circuit file, applied when loading finishes. */
	private boolean loadTriState = false;

	/**
	 * Create extend.
	 * 
	 * @param circuit The circuit this extend is in.
	 */
	public Extend(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The Extend body: the vertical spreader symbol with two dot pairs and
	 * three open arcs (issue #77 model/render split - the symbol as headless
	 * data). Byte-identical to the former inline {@code GeneralPath}: each
	 * primitive is appended with connect=false in the same order, so
	 * {@link Gate#gatePathFrom(GateOutline)} rebuilds the same path.
	 */
	@Override
	public GateOutline outline() {

		int s = Geometry.SPACING;
		int s2 = s/2;
		int s4 = s/4;
		return GateOutline.builder()
				.line(false, 0, s, s2, s)
				.line(false, s2, 0, s2, 2*s)
				.line(false, s2, 0, s, 0)
				.line(false, s2, s2, s, s2)
				.line(false, s2, 2*s, s, 2*s)
				.ellipse(false, 3*s4-1, s-1, 2, 2)
				.ellipse(false, 3*s4, s, 1, 1)
				.ellipse(false, 3*s4-1, s+s2-1, 2, 2)
				.ellipse(false, 3*s4, s+s2, 1, 1)
				.arc(false, s-s4, -s4, s2, s2, 0, 90)
				.line(false, s+s4, 0, s+s4, 3*s4)
				.arc(false, s+s4, s2, s2, s2, 180, 90)
				.arc(false, s+s4, s, s2, s2, 90, 90)
				.line(false, s+s4, s+s4, s+s4, 2*s)
				.arc(false, s-s4, 2*s-s4, s2, s2, 270, 90)
				.line(false, s+s2, s, 2*s, s)
				.build();
	} // end of outline method
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g Unused.
	 */
	@Override
	public void init(java.awt.Graphics g) {

		super.init(g);
		inputs.clear();
		int s = Geometry.SPACING;
		switch (orientation) {
		case LEFT:
			inputs.add(new Input("input0",this,4*s,s,1));
			break;
		case RIGHT:
			inputs.add(new Input("input0",this,0,s,1));
			break;
		case UP:
			inputs.add(new Input("input0",this,s,4*s,1));
			break;
		case DOWN:
			inputs.add(new Input("input0",this,s,0,1));
			break;
		}
		if (loadTriState) {
			outputs.get(0).loadSetTriState();
		}
	} // end of init method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {
		
		super.save(output,"Extend",outputs.get(0).isTriState());
	} // end of save method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, int value) {
		
		if (name.equals("tristate")) {
			loadTriState = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Display info about this extend.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public String infoText() {

		return "1 bit to " + bits + " bits";
	} // end of showInfo method

	/**
	 * An expand is just wire so has no propagation delay.
	 * 
	 * @return false.
	 */
	@Override
	public boolean hasTiming() {
		
		return false;
	} // end of hasTiming method
	
	/**
	 * Propagate tri-state to output.
	 * 
	 * @param which True if tristate, false if not.
	 */
	@Override
	public void setTriState(boolean which) {
		
		for (Output out : outputs) {
			out.setTriState(which);
		}
	} // end of setTriState method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * The extended value: all output bits copy the single input bit.
	 * Extend overrides initSim/react below because, unlike the other
	 * gates, it propagates immediately (no delay) and passes tri-state
	 * (null) values through.
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = inputs.get(0).getValue();
		BitSet newValue = new BitSet(bits);
		if (value != null && value.cardinality() != 0) {
			newValue.flip(0,bits);
		}
		return newValue;
	} // end of computeOutput method

	/**
	 * Initialize this element by setting its output pin to 0 or null.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		Output out = outputs.get(0);
		if (out.isTriState()) {
			out.setValue(null);
		}
		else {
			out.setValue(new BitSet(bits));
		}

	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		// get the input value
		BitSet value = inputs.get(0).getValue();
		
		// create new output value
		BitSet newValue = null;
		if (value != null) {
			newValue = new BitSet(bits);
			if (value.cardinality() != 0) {
				newValue.flip(0,bits);	// all ones
			}
		}
		
		// propagate new value to output
		Output out = outputs.get(0);
		out.propagate(newValue,now,sim);
		
	} // end of react method
	
} // end of Extend element
