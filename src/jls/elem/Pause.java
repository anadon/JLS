package jls.elem;

import jls.core.Geometry;
import jls.*;
import jls.sim.*;
import java.io.*;

import java.util.*;

/**
 * Pause element.
 * Causes simulator to pause when input is asserted.
 * 
 * @author David A. Poplawski
 */
public final class Pause extends LogicElement {

	/**
	 * Create a new pause element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Pause(Circuit circuit) {

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
	 * Prune unattached inputs before drawing: draw only the attached
	 * inputs, or all four if none are attached. This is the model-mutating
	 * step the former {@code draw} ran first; the GUI-side renderer calls
	 * it before drawing (issue #77).
	 */
	public void pruneDetachedInputs() {

		int s = Geometry.SPACING;

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

		Pause it = new Pause(circuit);
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

		output.println("ELEMENT Pause");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Display info about this element.
	 * 
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		return "pause simulation";
	} // end of showInfo method


	//	-------------------------------------------------------------------------------
	//	Simulation
	//	-------------------------------------------------------------------------------

	/** The input value assumed at simulation start: all zeros, or null when the input net is tri-state. */
	private BitSet currentValue = new BitSet();

	/**
	 * Initialize simulation.
	 * 
	 * @param sim The simulator.
	 *
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#pausePausesOnlyOnNonZeroInput()
	 */
	@Override
	public void initSim(Simulator sim) {

		for (Input input : inputs) {
			if (!input.isAttached())
				continue;
			if (input.getWireEnd().getNet().isTriState()) {
				currentValue = null;
			}
			else {
				currentValue = new BitSet();
			}
		}
	} // end of initSim method

	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Should be null.
	 *
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#pausePausesOnlyOnNonZeroInput()
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {

		// find the attached input
		for (Input input : inputs) {
			if (!input.isAttached())
				continue;

			// get its current value
			BitSet in = input.getValue();
			if (in != null && in.cardinality() != 0)
				sim.pause(true);
			return;
		}
	} // end of react method

} // end of Pause class
