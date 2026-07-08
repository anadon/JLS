package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;

import javax.swing.*;

/**
 * n-input AND gate(s).
 * 
 * @author David A. Poplawski
 */
public class AndGate extends Gate {
	
	// identity and shared previous-settings state (#22)
	private static final Kind KIND =
			new Kind("AND","AndGate",-1,10);
	
	/**
	 * Create AND gate.
	 * 
	 * @param circuit The circuit this AND gate is in.
	 */
	public AndGate(Circuit circuit) {
		
		super(circuit);
		
		// create image for draw
		int s = JLSInfo.spacing;
		Line2D top = new Line2D.Double(s,0,0,0);
		Line2D side = new Line2D.Double(0,0,0,s*2);
		Line2D bottom = new Line2D.Double(0,s*2,s,s*2);
		Arc2D arc = new Arc2D.Double(0,0,s*2,s*2,-90,180,Arc2D.Double.OPEN);
		gateShape = new GeneralPath(top);
		gateShape.append(side,true);
		gateShape.append(bottom,true);
		gateShape.append(arc,true);
		gateShape.closePath();
	} // end of construcor
	
	/**
	 * The kind descriptor for this gate.
	 */
	protected Kind kind() {
		
		return KIND;
	} // end of kind method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * AND the input bits (absent inputs count as 0).
	 */
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		value.set(0,bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.and(inVal);
		}
		return value;
	} // end of computeOutput method


} // end of AndGate class
