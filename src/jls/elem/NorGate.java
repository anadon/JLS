package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;

import javax.swing.*;

/**
 * n-input NOR gate(s).
 * 
 * @author David A. Poplawski
 */
public class NorGate extends Gate {

	// identity and shared previous-settings state (#22)
	private static final Kind KIND =
			new Kind("NOR","NorGate",-1,5);
	
	/**
	 * Create NOR gate.
	 * 
	 * @param circuit The circuit this NOR gate is in.
	 */
	public NorGate(Circuit circuit) {
		
		super(circuit);
		
		// create image for this gate for later use
		int s = JLSInfo.spacing;
		int d = JLSInfo.pointDiameter;
		Point2D.Double pright = new Point2D.Double(2*s-d,s);
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
		Ellipse2D bubble = new Ellipse2D.Double(2*s-d,s-d/2,d,d);
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
	 * NOR the input bits (absent inputs count as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.or(inVal);
		}
		value.flip(0,bits);
		return value;
	} // end of computeOutput method


} // end of NorGate class
