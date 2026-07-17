package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;

import javax.swing.*;

/**
 * n-input NAND gate(s).
 * 
 * @author David A. Poplawski
 */
public class NandGate extends Gate {

	// identity and shared previous-settings state (#22)
	private static final Kind KIND =
			new Kind("NAND","NandGate",-1,5);
	
	/**
	 * Create NAND gate
	 * 
	 * @param circuit The circuit this AND gate is in.
	 */
	public NandGate(Circuit circuit) {
		
		super(circuit);
		
		// create image for this gate for later use
		int s = JLSInfo.spacing;
		int d = JLSInfo.pointDiameter;
		Line2D top = new Line2D.Double(s/2,0,0,0);
		Line2D side = new Line2D.Double(0,0,0,s*2);
		Line2D bottom = new Line2D.Double(0,s*2,s/2,s*2);
		Arc2D arc = new Arc2D.Double(-s/2,0,s*2,s*2,-90,180,Arc2D.Double.OPEN);
		gateShape = new GeneralPath(top);
		gateShape.append(side,true);
		gateShape.append(bottom,true);
		gateShape.append(arc,true);
		gateShape.closePath();
		Ellipse2D bubble = new Ellipse2D.Double(s+d,s-d/2,d,d);
		gateShape.append(bubble,false);
	} // end of constructor
	
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
	 * NAND the input bits (absent inputs count as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		value.set(0,bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.and(inVal);
		}
		value.flip(0,bits);
		return value;
	} // end of computeOutput method


} // end of NandGate class
