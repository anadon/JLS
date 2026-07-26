package jls.elem;

import java.io.*;
import java.util.*;

import jls.*;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.sim.*;

/**
 * Input pin of a subcircuit.
 *
 * @author David A. Poplawski
 */
public final class InputPin extends Pin implements TriProp {

	/**
	 * Create a new input pin.
	 *
	 * @param circ The circuit this pin will be in.
	 */
	public InputPin(Circuit circ) {

		super(circ);
	} // end of constructor

	/**
	 * The pin kind for user-facing messages.
	 */
	@Override
	protected String pinKind() {

		return "Input";
	} // end of pinKind method

	/**
	 * Initialize internal info for this element.
	 * Most work done in superclass, but output point added here.
	 *
	 * @param g The Graphics object used to compute the size of the name.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		super.init(g);
		if(orientation == Orientation.RIGHT)
		{
			Output out = new Output("output",this,width,Geometry.SPACING,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}
		if(orientation == Orientation.LEFT)
		{
			Output out = new Output("output",this,0,Geometry.SPACING,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}
		if(orientation == Orientation.DOWN)
		{
			Output out = new Output("output",this,width/2,height,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}
		if(orientation == Orientation.UP)
		{
			Output out = new Output("output",this,width/2,0,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}

	} // end of init method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT InputPin");
		if (outputs.get(0).isTriState()) {
			output.println(" int tristate 1");
		}
		super.save(output);
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		InputPin it = new InputPin(getCircuit());
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Display info about this element.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		String tri = "";
		if (outputs.get(0).isTriState())
			tri = " (tri-state) ";
		return bits + " bit input pin" + tri + ", value = " +
				BitSetUtils.toDisplay(currentValue,bits);
	} // end of showInfo method

	/**
	 * Set this element to tri-state or not and propagate to output(s).
	 *
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
	public void setTriState(boolean which) {

		for (Output output : outputs) {
			output.setTriState(which);
		}
	} // end of setTriState method

	/**
	 * Return a string representation of this InputPin.
	 */
	@Override
	public String toString() {

		return "[InputPin " + name + "(" + bits + " bits)]";
	} // end of toString method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * Initialize output to 0 or null.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		currentValue = new BitSet();
		if (getCircuit().isImported()) {
			SubCircuit sub = getCircuit().getSubElement();
			if (sub == null)
				throw new IllegalStateException(
						"imported input pin has no subcircuit element");
			Input input = sub.getInput(getName());
			if (input.isAttached()) {
				WireEnd end = input.getWireEnd();
				if (end == null)
					throw new IllegalStateException("attached input has no wire end");
				if (end.isTriState())
					currentValue = null;
			}
		}
		Output out = outputs.get(0);
		if (currentValue == null) {
			out.setValue(null);
		}
		else {
			out.setValue((BitSet)currentValue.clone());
		}
	} // end of initSim method

	/**
	 * React to an event by sending the value it got to everything it is
	 * connected to.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo The value to send.
	 */
	@Override
	public void react(long now, Simulator sim, @org.jspecify.annotations.Nullable Object todo) {

		// send to output
		Output out = outputs.get(0);
		BitSet value = (BitSet)todo;
		if (value == null) {
			currentValue = null;
			out.propagate(value,now,sim);
		}
		else {
			currentValue = (BitSet)value.clone();
			out.propagate((BitSet)value.clone(),now,sim);
		}

	} // end of react method

	/**
	 * Display current value.
	 *
	 * @param whereX the x-coordinate on screen (unused here).
	 * @param whereY the y-coordinate on screen (unused here).
	 */
	@Override
	public void showCurrentValue(int whereX, int whereY) {

		String value = "off";
		if (currentValue != null) {
			String hex = BitSetUtils.ToString(currentValue,16);
			String unsigned = BitSetUtils.ToString(currentValue,10);
			String signed = BitSetUtils.ToStringSigned(currentValue,bits);
			value = "0x" + hex + " (" + unsigned + " unsigned, " + signed + " signed)";
		}
		TellUser.info(null, value, "Information");
	} // end of showCurrentValue method

} // end of InputPin class
