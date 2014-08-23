package jls.elem;

import jls.*;

/**
 * Superclass for "elements" that are just for display (not active).
 * Currently just Text, but eventually some drawing capability like colored lines,
 * shaded boxes, etc.
 * 
 * @author David A. Poplawski
 */
public abstract class DisplayElement extends Element {
	
	public DisplayElement(Circuit circuit) {
		
		super(circuit);
	} // end of constructor

} // end of DisplayElement class
