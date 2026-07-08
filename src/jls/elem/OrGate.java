package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;
import javax.swing.*;

/**
 * n-input OR gate(s).
 * 
 * @author David A. Poplawski
 */
public class OrGate extends Gate {

	// identity and shared previous-settings state (#22)
	private static final Kind KIND =
			new Kind("OR","OrGate",-1,10);
	
	/**
	 * Create OR gate.
	 * 
	 * @param circuit The circuit this OR gate is in.
	 */
	public OrGate(Circuit circuit) {
		
		super(circuit);
		
		// create image for this gate for later use
		int s = JLSInfo.spacing;
		Point2D.Double pright = new Point2D.Double(2*s,s);
		Point2D.Double ptop = new Point2D.Double(0,0);
		Point2D.Double pbottom = new Point2D.Double(0,2*s);
		Point2D.Double c1 = new Point2D.Double();
		Point2D.Double c2 = new Point2D.Double();
		
		c1.setLocation(pright.x-1,pright.y-3);
		c2.setLocation(ptop.x+s,ptop.y);
		CubicCurve2D top = new CubicCurve2D.Double();
		top.setCurve(pright,c1,c2,ptop);
		
		c1.setLocation(ptop.x+s/4,ptop.y+s);
		c2.setLocation(pbottom.x+s/4,pbottom.y-s);
		CubicCurve2D side = new CubicCurve2D.Double();
		side.setCurve(ptop,c1,c2,pbottom);
		
		c1.setLocation(pbottom.x+s,pbottom.y);
		c2.setLocation(pright.x-1,pright.y+3);
		CubicCurve2D bottom = new CubicCurve2D.Double();
		bottom.setCurve(pbottom,c1,c2,pright);
		gateShape = new GeneralPath(top);
		gateShape.append(side,true);
		gateShape.append(bottom,true);
		gateShape.closePath();
	} // end of constructor
	
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
	 * OR the input bits (absent inputs count as 0).
	 */
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.or(inVal);
		}
		return value;
	} // end of computeOutput method


} // end of OrGate class
