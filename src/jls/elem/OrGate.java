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

	// default properties
	private static int defaultDelay = 10;
	
	// info from previously created OR gate
	private static int previousInputs = defaultInputs;
	private static int previousBits = defaultBits;
	private static Orientation previousOrientation = defaultOrientation;
	
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

		boolean ok = super.setup(g,editWindow,x,y,"OR");
		
		// finish up initialization
		if (ok) {
			previousInputs = numInputs;
			previousBits = bits;
			previousOrientation = orientation;
		}
		return ok;
		
	} // end of init method

	/**
	 * Copy this element.
	 * 
	 * @return A copy of this OR gate.
	 */
	public Element copy() {
		
		OrGate it = new OrGate(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		super.save(output,"OrGate",false);
	} // end of save method
	
	/**
	 * Display info about this OR gate.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		super.showInfo(info,"OR");
	} // end of showInfo method
	
	/**
	 * Set characteristics to previous values.
	 */
	public void setToPrevious() {
		
		numInputs = previousInputs;
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
		 * Initialize this element by setting its output pin and to-be value to 0.
		 * 
		 * @param sim Unused.
		 */
		public void initSim(Simulator sim) {
			
			// set output pin
			Output out = (Output)(outputs.toArray()[0]);
			BitSet bitval = new BitSet(1);
			out.setValue(bitval);
			
			// set to-be value
			toBeValue = new BitSet(1);
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
				
				// OR the input bits
				BitSet value = new BitSet(bits);
				for (Input input : inputs) {
					BitSet inVal = input.getValue();
					if (inVal == null)
						inVal = new BitSet();
					value.or(inVal);
				}
				
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

} // end of OrGate class
