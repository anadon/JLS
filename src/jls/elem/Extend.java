package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * 1-input, n-outputs, where all outputs are equal to the input.
 * 
 * @author David A. Poplawski
 */
public class Extend extends Gate implements TriProp {
	
	// properties
	private boolean loadTriState = false;

	/**
	 * Create extend.
	 * 
	 * @param circuit The circuit this extend is in.
	 */
	public Extend(Circuit circuit) {
		
		super(circuit);
		
		// create image for draw
		int s = JLSInfo.spacing;
		int s2 = s/2;
		int s4 = s/4;
		Line2D left = new Line2D.Double(0,s,s2,s);
		Line2D vert = new Line2D.Double(s2,0,s2,2*s);
		Line2D top = new Line2D.Double(s2,0,s,0);
		Line2D mid = new Line2D.Double(s2,s2,s,s2);
		Line2D bottom = new Line2D.Double(s2,2*s,s,2*s);
		Ellipse2D dot1 = new Ellipse2D.Double(3*s4-1,s-1,2,2);
		Ellipse2D dot1a = new Ellipse2D.Double(3*s4,s,1,1);
		Ellipse2D dot2 = new Ellipse2D.Double(3*s4-1,s+s2-1,2,2);
		Ellipse2D dot2a = new Ellipse2D.Double(3*s4,s+s2,1,1);
		Arc2D topArc = new Arc2D.Double(s-s4,-s4,s2,s2,0,90,Arc2D.OPEN);
		Line2D upper = new Line2D.Double(s+s4,0,s+s4,3*s4);
		Arc2D midArc1 = new Arc2D.Double(s+s4,s2,s2,s2,180,90,Arc2D.OPEN);
		Arc2D midArc2 = new Arc2D.Double(s+s4,s,s2,s2,90,90,Arc2D.OPEN);
		Line2D lower = new Line2D.Double(s+s4,s+s4,s+s4,2*s);
		Arc2D bottomArc = new Arc2D.Double(s-s4,2*s-s4,s2,s2,270,90,Arc2D.OPEN);
		Line2D right = new Line2D.Double(s+s2,s,2*s,s);
		gateShape = new GeneralPath(left);
		gateShape.append(vert,false);
		gateShape.append(top,false);
		gateShape.append(mid,false);
		gateShape.append(bottom,false);
		gateShape.append(dot1,false);
		gateShape.append(dot1a,false);
		gateShape.append(dot2,false);
		gateShape.append(dot2a,false);
		gateShape.append(topArc,false);
		gateShape.append(upper,false);
		gateShape.append(midArc1,false);
		gateShape.append(midArc2,false);
		gateShape.append(lower,false);
		gateShape.append(bottomArc,false);
		gateShape.append(right,false);
	} // end of constructor
	
	/**
	 * Initialize this element in a GUI context.
	 * 
	 * @param g The graphics object to use.
	 * @param editWindow The editor window this circuit is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		boolean ok = super.setup(g,editWindow,x,y,"Extend");
		
		// must have at least 2 output bits
		if (ok && bits < 2) {
			JOptionPane.showMessageDialog(JLSInfo.frame,"must have at least 2 bits of output");
			return false;
		}
		
		// finish up initialization
		if (ok) {
			numInputs = 1;
		}
		return ok;
		
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
		super.init(g);
		inputs.clear();
		int s = JLSInfo.spacing;
		switch (orientation) {
		case left:
			inputs.add(new Input("input0",this,4*s,s,1));
			break;
		case right:
			inputs.add(new Input("input0",this,0,s,1));
			break;
		case up:
			inputs.add(new Input("input0",this,s,4*s,1));
			break;
		case down:
			inputs.add(new Input("input0",this,s,0,1));
			break;
		}
		if (loadTriState) {
			outputs.get(0).loadSetTriState();
		}
	} // end of init method
	
	/**
	 * Copy this element.
	 * 
	 * @return A copy of this extend.
	 */
	public Element copy() {
		
		Extend it = new Extend(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		super.save(output,"Extend",outputs.get(0).isTriState());
	} // end of save method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("tristate")) {
			loadTriState = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Display info about this extend.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText("1 bit to " + bits + " bits");
	} // end of showInfo method

	/**
	 * An expand is just wire so has no propagation delay.
	 * 
	 * @return false.
	 */
	public boolean hasTiming() {
		
		return false;
	} // end of hasTiming method
	
	/**
	 * Propagate tri-state to output.
	 * 
	 * @param which True if tristate, false if not.
	 */
	public void setTriState(boolean which) {
		
		for (Output out : outputs) {
			out.setTriState(which);
		}
	} // end of setTriState method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
	
	/**
	 * Initialize this element by setting its output pin to 0 or null.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		Output out = outputs.get(0);
		if (out.isTriState()) {
			out.setValue(null);
		}
		else {
			out.setValue(new BitSet(bits));
		}

	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// get the input value
		BitSet value = inputs.get(0).getValue();
		
		// create new output value
		BitSet newValue = null;
		if (value != null) {
			newValue = new BitSet(bits);
			if (value.cardinality() != 0) {
				newValue.flip(0,bits);	// all ones
			}
		}
		
		// propagate new value to output
		Output out = outputs.get(0);
		out.propagate(newValue,now,sim);
		
	} // end of react method
	
} // end of Extend element
