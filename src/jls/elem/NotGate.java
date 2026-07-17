package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Not gate(s) (inverter).
 * 
 * @author David A. Poplawski
 */
public class NotGate extends Gate {

	// identity and shared previous-settings state (#22)
	private static final Kind KIND =
			new Kind("NOT","NotGate",1,5);

	/**
	 * Create NOT gate.
	 * 
	 * @param circuit The circuit this NOT gate is in.
	 */
	public NotGate(Circuit circuit) {
		
		super(circuit);
		
		// create image for draw
		int s = JLSInfo.spacing;
		int d = JLSInfo.pointDiameter;
		Line2D top = new Line2D.Double(0,0,2*s-d,s);
		Line2D bottom = new Line2D.Double(2*s-d,s,0,2*s);
		Line2D side = new Line2D.Double(0,2*s,0,0);
		gateShape = new GeneralPath(top);
		gateShape.append(bottom,true);
		gateShape.append(side,true);
		gateShape.closePath();
		Ellipse2D bubble = new Ellipse2D.Double(2*s-d,s-d/2,d,d);
		gateShape.append(bubble,false);
	} // end of construcor
	
	/**
	 * The kind descriptor for this gate.
	 */
	@Override
	protected Kind kind() {
		
		return KIND;
	} // end of kind method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * NOT the input bits (an absent input counts as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = inputs.get(0).getValue();
		if (value == null)
			value = new BitSet();
		else
			value = (BitSet)value.clone();
		value.flip(0,bits);
		return value;
	} // end of computeOutput method


} // end of NotGate class
