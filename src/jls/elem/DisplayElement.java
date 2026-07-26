package jls.elem;

import jls.*;

import org.jspecify.annotations.Nullable;

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
	 * @param circuit the circuit this element belongs to, or null for a
	 *            detached header/sentinel element.
	 */
	public DisplayElement(@Nullable Circuit circuit) {
		
		super(circuit);
	} // end of constructor

} // end of DisplayElement class
