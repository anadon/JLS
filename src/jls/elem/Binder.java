package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.Circuit;
import jls.sim.Simulator;

/**
 * Bind multiple input wires (or bundles) into a single bundle.
 * 
 * @author David A. Poplawski
 */
public final class Binder extends Group implements TriProp {
	
	
	/**
	 * Create a new binder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Binder(Circuit circuit) {
		
		super(circuit);
	} // end of constructor

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(java.awt.Graphics g) {
		
		// set up height and width
		super.init(g);
		
		// set up inputs
		int s = Geometry.SPACING;
		if(orientation == Orientation.RIGHT)
		{
			int ypos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, 0, ypos, e.getSize()));
				ypos += s;
			}
		
			// set up output
			outputs.add(new Output("output",this,width,((ranges.size()-1)/2+1)*s,bits));
			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
		else if(orientation == Orientation.LEFT)
		{
			int ypos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, width, ypos, e.getSize()));
				ypos += s;
			}
			
			// set up output
			outputs.add(new Output("output",this,0,((ranges.size()-1)/2+1)*s,bits));
			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
		else if(orientation == Orientation.DOWN)
		{
			int xpos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, xpos, 0, e.getSize()));
				xpos += s;
			}
			// set up output
			outputs.add(new Output("output",this,((ranges.size()-1)/2+1)*s,height,bits));

			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
		else if(orientation == Orientation.UP)
		{
			int xpos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, xpos, height, e.getSize()));
				xpos += s;
			}
			// set up output
			outputs.add(new Output("output",this,((ranges.size()-1)/2+1)*s,0,bits));

			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
	} // end of init method
	
	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {
		
		Binder it = new Binder(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Binder");
		super.save(output);
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(javax.swing.JLabel info) {

		info.setText("bundle " + bits + " bits");
	} // end of showInfo method
	
	/**
	 * Set this element to tri-state or not.
	 * Output will be tri-state if and only if all attached inputs are.
	 * Don't propagate if no change.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
	public void setTriState(boolean which) {
		
		// save current state
		boolean saveState = this.triState;
		
		// see if all attached inputs are tri-state
		int tri = 0;
		for (Input input : inputs) {
			if (input.isAttached() && input.getWireEnd().isTriState()) {
				tri += 1;
			}
		}
		
		// set tri-state if all inputs are tri-state
		int numInputs = inputs.size();
		if (numInputs > 0 && numInputs == tri) {
			triState = true;
		}
		else {
			triState = false;
		}
		
		// don't propagate if no change
		if (triState == saveState)
			return;
		
		// propagate to outputs
		for (Output out : outputs) {
			out.setTriState(triState);
		}
	} // end of setTriState method
	
	
	/**
	 *  This method will rotate the binder if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, java.awt.Graphics g)
	{
		super.rotate(direction, g);
		init(g);
	}
	
	/**
	 * This method will flip a binder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(java.awt.Graphics g)
	{
		super.flip(g);
		init(g);
	}
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize this element by setting its output to 0 or null.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		Output out = outputs.get(0);
		BitSet value = null;
		if (!out.isTriState()) {
			value = new BitSet();
		}
		outputs.get(0).setValue(value);

	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		// create output value
		BitSet newValue = new BitSet(bits);
		
		// get the input values and set bits in the output value
		int inNum = 0;
		boolean allOff = true;
		for (Entry e : ranges) {
			BitSet value = inputs.get(inNum).getValue();
			
			// make a tristate off be a 0
			if (value == null) {
				value = new BitSet();
			}
			else {
				allOff = false;
			}
			int inp = 0;
			for(int i : e.getValues()) {
				boolean val = value.get(inp);
				newValue.set(i, val);
				inp += 1;
			}
			inNum += 1;
		}
		
		// send value to output
		if (allOff) {
			outputs.get(0).propagate(null,now,sim);
		}
		else {
			outputs.get(0).propagate(newValue,now,sim);
		}
		
	} // end of react method
	
} // end of Binder class
