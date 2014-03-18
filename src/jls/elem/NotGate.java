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

	// default properties
	private static int defaultDelay = 5;

	// info from previously created NOT gate
	private static int previousBits = defaultBits;
	private static Orientation previousOrientation = defaultOrientation;

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
		
		boolean ok = super.setup(g,editWindow,x,y,"NOT");
		
		// finish up initialization
		if (ok) {
			numInputs = 1;
			previousBits = bits;
			previousOrientation = orientation;
		}
		return ok;
		
	} // end of setup method
	
	/**
	 * Copy this element.
	 * 
	 * @return A copy of this NOT gate.
	 */
	public Element copy() {
		
		NotGate it = new NotGate(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		super.save(output,"NotGate",false);
	} // end of save method
	
	/**
	 * Display info about this NOT gate.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		super.showInfo(info,"NOT");
	} // end of showInfo method
	
	/**
	 * Set characteristics to previous values.
	 */
	public void setToPrevious() {
		
		bits = previousBits;
		orientation = previousOrientation;
	} // end of setToPrevious method

	/**
	 * Get default propagation delay.
	 */
	public int getDefaultDelay() {
		
		return defaultDelay;
	} // end of getDefaultDelay method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
			
	private BitSet toBeValue;
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 1.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {

		// set output pin to 0
		Output out = (Output)(outputs.toArray()[0]);
		BitSet bitval = new BitSet(1);
		out.setValue(bitval);
		
		// post event to make output become 1 at time 0
		BitSet one = new BitSet(1);
		one.set(0,bits);
		sim.post(new SimEvent(0,this,one));
		
		// set to-be value
		toBeValue = (BitSet)one.clone();
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// if the input has changed ...
		if (todo == null) {
			
			// NOT the input bits
			BitSet value = ((Input)(inputs.toArray()[0])).getValue();
			if (value == null)
				value = new BitSet();
			value = (BitSet)value.clone();
			value.flip(0,bits);
			
			// if new value is different from the value propagating through
			// this gate, then post an event
			if (!value.equals(toBeValue)) {
				toBeValue = (BitSet)value.clone();
				sim.post(new SimEvent(now+propDelay,this,value));
			}
		}
		else {
			
			// get the new output value
			BitSet newValue = (BitSet)todo;
			
			// send to output
			Output out = (Output)(outputs.toArray()[0]);
			out.propagate(newValue,now,sim);
		}
		
	} // end of react method
	
} // end of NotGate class
