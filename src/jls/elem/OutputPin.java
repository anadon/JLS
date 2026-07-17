package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;

import javax.swing.*;

/**
 * Output pin of a subcircuit.
 * 
 * @author David A. Poplawski
 */
public class OutputPin extends Pin implements TriProp {

	/**
	 * Create a new output pin.
	 * 
	 * @param circ
	 *            The circuit this pin will be in.
	 */
	public OutputPin(Circuit circ) {

		super(circ);
		orientation = JLSInfo.Orientation.RIGHT;
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
	 * Display dialog to get pin name and bits.
	 * 
	 * @param g
	 *            The Graphics object to use to determine the name's size.
	 * @param editWindow
	 *            The editor window this pin is displayed in.
	 * @param x
	 *            The x-coordinate of the last known mouse position.
	 * @param y
	 *            The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		return super.setup(g, editWindow, x, y, "Output");
	} // end of setup method

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
	public void init(Graphics g) {

		super.init(g);
		if (orientation == JLSInfo.Orientation.RIGHT) {
			inputs.add(new Input("input", this, 0, JLSInfo.spacing, bits));
		} else if (orientation == JLSInfo.Orientation.LEFT) {
			inputs.add(new Input("input", this, width, JLSInfo.spacing, bits));
		} else if (orientation == JLSInfo.Orientation.DOWN) {
			inputs.add(new Input("input", this, width / 2, 0, bits));
		} else if (orientation == JLSInfo.Orientation.UP) {
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
	public void showInfo(JLabel info) {

		String value = ", value = " + BitSetUtils.toDisplay(currentValue, bits);
		String tri = "";
		if (inputs.get(0).isAttached()) {
			if (inputs.get(0).getWireEnd().getNet().isTriState())
				tri = " (tri-state) ";
		}
		info.setText(bits + " bit output pin" + tri + value);
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
	public void showCurrentValue(Point where) {

		TellUser.info(JLSInfo.frame, BitSetUtils.toDisplay(
				currentValue, bits), "Information");
	} // end of showCurrentValue method

} // end of OutputPin class
