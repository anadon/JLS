package jls.elem;

import jls.*;

/**
 * Superclass for "elements" that are just for display (not active).
 * Currently just Text, but eventually some drawing capability like colored lines,
 * shaded boxes, etc.
 * 
 * @author David A. Poplawski
 */
public abstract sealed class DisplayElement extends Element
		permits Text {
	
	/**
	 * Create a new display element within the given circuit.
	 *
	 * @param circuit the circuit this element belongs to.
	 */
	public DisplayElement(Circuit circuit) {
		
		super(circuit);
	} // end of constructor

} // end of DisplayElement class
