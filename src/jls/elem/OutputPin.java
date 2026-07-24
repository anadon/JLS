package jls.elem;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.*;
import jls.sim.*;
import java.io.*;
import java.util.BitSet;

/**
 * Output pin of a subcircuit.
 * 
 * @author David A. Poplawski
 */
public final class OutputPin extends Pin implements TriProp {

	/**
	 * Create a new output pin.
	 * 
	 * @param circ
	 *            The circuit this pin will be in.
	 */
	public OutputPin(Circuit circ) {

		super(circ);
		orientation = Orientation.RIGHT;
	} // end of constructor
	
	/**
	 * Return a string version of this element.
	 * 
	 * @return the string.
	 */
	@Override
	public String toString() {
		
		return "OutputPin[" + super.toString() + "]";
	} // end of toString method

	/**
	 * The pin kind for user-facing messages.
	 */
	@Override
	protected String pinKind() {

		return "Output";
	} // end of pinKind method

	/**
	 * Initialize internal info for this element. Most work done in superclass,
	 * but input point added here.
	 * 
	 * @param g
	 *            The Graphics object used to compute the size of the name.
	 */
	@Override
	public void init(jls.core.TextMetrics g) {

		super.init(g);
		if (orientation == Orientation.RIGHT) {
			inputs.add(new Input("input", this, 0, Geometry.SPACING, bits));
		} else if (orientation == Orientation.LEFT) {
			inputs.add(new Input("input", this, width, Geometry.SPACING, bits));
		} else if (orientation == Orientation.DOWN) {
			inputs.add(new Input("input", this, width / 2, 0, bits));
		} else if (orientation == Orientation.UP) {
			inputs.add(new Input("input", this, width / 2, height, bits));
		}
	} // end of init method

	/**
	 * Save this element.
	 * 
	 * @param output
	 *            The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT OutputPin");
		if (getCircuit().isImported()) {
			SubCircuit sub = getCircuit().getSubElement();
			if (sub.getOutput(name).isTriState()) {
				output.println(" int tristate 1");
			}
		}
		super.save(output);
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		OutputPin it = new OutputPin(circuit);
		it.inputs.add(inputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Display info about this element.
	 * 
	 * @param info
	 *            The JLabel to display with.
	 */
	@Override
	public String infoText() {

		String value = ", value = " + BitSetUtils.toDisplay(currentValue, bits);
		String tri = "";
		if (inputs.get(0).isAttached()) {
			if (inputs.get(0).getWireEnd().getNet().isTriState())
				tri = " (tri-state) ";
		}
		return bits + " bit output pin" + tri + value;
	} // end of showInfo method

	/**
	 * Set this pin as tristate or not. If part of a subcircuit, propagate
	 * tristate status to output.
	 * 
	 * @param which
	 *            True to make this pin tristate, false to make it not.
	 */
	@Override
	public void setTriState(boolean which) {

		if (!getCircuit().isImported())
			return;
		SubCircuit sub = getCircuit().getSubElement();
		Output put = (Output) sub.getPut(name);
		if (put != null)
			put.setTriState(which);
	} // end of setTriState

	/**
	 * See if this element is tristate at load time.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean isLoadTriState() {

		return loadTriState;
	} // end of isLoadTriState

	// -------------------------------------------------------------------------------
	// Simulation
	// -------------------------------------------------------------------------------

	/**
	 * Initialize current value to 0 or null.
	 * 
	 * @param sim
	 *            Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		Input in = inputs.get(0);
		if (in.isAttached()) {
			WireEnd end = in.getWireEnd();
			if (end.isTriState()) {
				currentValue = null;
				return;
			}
		}
		currentValue = new BitSet();
	} // end of initSim method

	/**
	 * React to an event by sending the input value to the output pin in the
	 * containing subcircuit element, unless this circuit is not a subcircuit.
	 * 
	 * @param now
	 *            The current simulation time.
	 * @param sim
	 *            The simulator to post events to.
	 * @param todo
	 *            Unused.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {

		// send to output
		Input in = inputs.get(0);
		BitSet value = in.getValue();
		if (value == null)
			currentValue = null;
		else
			currentValue = (BitSet) value.clone();
		if (circuit.isImported()) {
			SubCircuit sub = circuit.getSubElement(); // the subcircuit
														// element
			sub.send(this, value, now, sim);
		}

	} // end of react method

	/**
	 * Display current value.
	 * 
	 * @param where
	 *            Unused.
	 */
	@Override
	public void showCurrentValue(int whereX, int whereY) {

		TellUser.info(null, BitSetUtils.toDisplay(
				currentValue, bits), "Information");
	} // end of showCurrentValue method

} // end of OutputPin class
