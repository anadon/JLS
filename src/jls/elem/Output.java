package jls.elem;

import jls.sim.*;
import java.util.*;

/**
 * Output point on an element.
 * WireEnds connect to these.
 * 
 * @author David A. Poplawski
 */
public class Output extends Put {
	
	// properties
	private boolean triState = false;

	/**
	 * Construct a new output point.
	 * 
	 * @param name The name of this point.
	 * @param element The element it is part of.
	 * @param xr The x-coordinate of the center of the output relative the upper left
	 * 		corner of the element this put is in.
	 * @param yr The y-coordinate of the center of the output relative the upper left
	 * 		corner of the element this put is in.
	 * @param bits The number of bits in the output.  0 implies an arbitrary number.
	 */
	public Output(String name, LogicElement element, int xr, int yr, int bits) {
		
		super(name, element, xr, yr, bits);
	} // end of constructor
	
	/**
	 * Return a string version of this element.
	 * 
	 * @return the string verison of all the properties.
	 */
	public String toString() {
		
		return "Output[" + super.toString() + ",tristate=" + triState + "]";
	} // end of toString method
	
	/**
	 * Make a copy of this output.
	 * 
	 * @param element The element this output will be part of.
	 * 
	 * @return A copy.
	 */
	public Output copy(LogicElement element) {
		
		Output p = new Output(name,element,xr,yr,bits);
		p.triState = triState;
		myCopy = p;
		return p;
	} // end of copy method
	
	/**
	 * Set tri-state during a load.
	 */
	public void loadSetTriState() {
		
		triState = true;
	} // end of loadSetTriState method
	
	/**
	 * Make this output be tri-state or not.
	 * Propagate to an attached wire, if one.
	 * 
	 * @param which True to make this output be tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		triState = which;
		if (isAttached()) {
			getWireEnd().getNet().setTriState(which);
		}
	} // end of setTriState method
	
	/**
	 * Find out if this output is tri-state.
	 * 
	 * @return true if it is tri-state, false if not.
	 */
	public boolean isTriState() {
		
		return triState;
	} // end of isTriState method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
		
	/**
	 * Set the current value of this output and the wire net it is connected to.
	 * Should only be used by initSim.
	 * Does not make a copy, so make sure value does not change.
	 * 
	 * @param value The value.
	 */
	public void setValue(BitSet value) {
		
		currentValue = value;
		if (isAttached())
			getWireEnd().getNet().setValue(value);
	} // end of setValue method
	
	/**
	 * Get the current value of this output.
	 * 
	 * @return a copy of the current value.
	 */
	public BitSet getValue() {
		
		return currentValue;
	} // end of getValue method
	
	/**
	 * Send value to all inputs connected to this output.
	 * Value not sent if it is the same as the current output value.
	 * Current value of this output saved.
	 * Uses wire net to do the work.
	 * 
	 * @param value The value to send.
	 * @param now The current time.
	 * @param sim The simulator to post events to.
	 */
	public void propagate(BitSet value, long now, Simulator sim) {
		
		// don't do anything if the value hasn't changed
		if (currentValue == null) {
			if (value == null)
				return;
		}
		else if (currentValue.equals(value)) {
			return;
		}
		
		// save value
		if (value == null) {
			currentValue = null;
		}
		else {
			currentValue = (BitSet)value.clone();
		}
		
		// can't send if output is not attached
		if (!isAttached()) {
			return;
		}
		
		// get wire net of attached wire end
		WireNet net = getWireEnd().getNet();
		
		// send value
		net.propagate(value,now,sim);
		
	} // end of propagate method
	

} // end of Output class
