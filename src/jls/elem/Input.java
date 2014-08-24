package jls.elem;

import java.util.*;

/**
 * Input point on an element.
 * WireEnds connect to these.
 * 
 * @author David A. Poplawski
 */
public class Input extends Put {

	/**
	 * Construct a new input point.
	 * 
	 * @param name The name of the point.
	 * @param element The element it is part of.
	 * @param xr The x-coordinate of the center of the input relative to the upper left
	 * 		corner of the element it is in.
	 * @param yr The y-coordinate of the center of the input relative to the upper left
	 * 		corner of the element it is in.
	 * @param bits The number of bits in the input.
	 */
	public Input(String name, LogicElement element, int xr, int yr, int bits) {
		
		super(name, element, xr, yr, bits);
	} // end of constructor
	
	/**
	 * Make a copy of this input.
	 * 
	 * @param element The element this input will be part of.
	 * 
	 * @return A copy.
	 */
	public Input copy(LogicElement element) {
		
		Input p = new Input(name,element,xr,yr,bits);
		myCopy = p;
		return p;
	} // end of copy method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
			
	/**
	 * Set the value of this input.
	 * 
	 * @param value The new value.
	 */
	public void setValue(BitSet value) {
		
		currentValue = value;
	} // end of setValue method
	
	/**
	 * Get the current value of this input.
	 * 
	 * @return the current value.
	 */
	public BitSet getValue() {
		
		return currentValue;
	} // end of getValue method

} // end of Input class
