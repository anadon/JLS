package jls.elem;

import jls.core.Geometry;
import jls.*;
import jls.sim.*;
import java.io.*;
import java.util.*;

/**
 * Stop element.
 * Causes simulator to terminate when input is asserted.
 * 
 * @author David A. Poplawski
 */
public final class Stop extends LogicElement {

	/**
	 * Create a new stop element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Stop(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g Unused.
	 */
	@Override
	public void init(jls.core.TextMetrics g) {
		
		// set up size
		int s = Geometry.SPACING;
		width = s * 2;
		height = width;
		
		// create inputs
		inputs.add(new Input("input0",this,0,s,1));
		inputs.add(new Input("input1",this,s,0,1));
		inputs.add(new Input("input2",this,s,2*s,1));
		inputs.add(new Input("input3",this,2*s,s,1));
	} // end of init method

	/**
	 * Prune unattached inputs, leaving only the attached ones drawn (or all
	 * four when none are attached). Formerly the leading, model-mutating
	 * step of {@code draw}; the GUI-side renderer runs it before drawing.
	 */
	public void pruneDetachedInputs() {

		int s = Geometry.SPACING;

		// draw only the attached inputs, or all four if none attached

		// get unattached inputs
		Vector<Input>detach = new Vector<Input>(4);
		for (Input input : inputs) {
			if (!input.isAttached())
				detach.add(input);
		}

		// if there are one, two or three unattached ones
		int count = detach.size();
		if (count > 0 && count < 4) {

			// remove unattached inputs
			inputs.removeAll(detach);

			// if no inputs left, put all four back
			if (inputs.size() == 0) {
				inputs.add(new Input("input0",this,0,s,1));
				inputs.add(new Input("input1",this,s,0,1));
				inputs.add(new Input("input2",this,s,2*s,1));
				inputs.add(new Input("input3",this,2*s,s,1));
			}
		}
	} // end of pruneDetachedInputs method


	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {
		
		Stop it = new Stop(circuit);
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
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
		
		output.println("ELEMENT Stop");
		super.save(output);
		output.println("END");
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public String infoText() {
		
		return "stop simulation";
	} // end of showInfo method
	

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize simulation.
	 * 
	 * @param sim The simulator.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// do nothing
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Should be null.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		// find the attached input
		for (Input input : inputs) {
			if (!input.isAttached())
				continue;
			
			// if value is a 1, stop
			BitSet in = input.getValue();
			if (in != null && in.cardinality() != 0) {
				sim.stop();
				return;
			}
		}
	} // end of react method
	
} // end of Stop class
