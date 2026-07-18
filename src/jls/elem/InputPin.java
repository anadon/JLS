package jls.elem;

import jls.*;
import jls.sim.*;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;

import java.util.*;

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
	 * Display dialog to get pin name and bits.
	 * 
	 * @param g The Graphics object to use to determine the name's size.
	 * @param editWindow The editor window this pin is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		return super.setup(g,editWindow,x,y,"Input");
	} // end of setup method

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
	public void init(Graphics g) {

		super.init(g);
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			Output out = new Output("output",this,width,JLSInfo.spacing,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			Output out = new Output("output",this,0,JLSInfo.spacing,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}
		if(orientation == JLSInfo.Orientation.DOWN)
		{
			Output out = new Output("output",this,width/2,height,bits);
			outputs.add(out);
			if (loadTriState) {
				out.loadSetTriState();
			}
		}
		if(orientation == JLSInfo.Orientation.UP)
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
		
		InputPin it = new InputPin(circuit);
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {
		
		String tri = "";
		if (outputs.get(0).isTriState())
			tri = " (tri-state) ";
		info.setText(bits + " bit input pin" + tri + ", value = " +
				BitSetUtils.toDisplay(currentValue,bits));
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
		if (circuit.isImported()) {
			SubCircuit sub = circuit.getSubElement();
			Input input = sub.getInput(name);
			if (input.isAttached()) {
				if (input.getWireEnd().isTriState())
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
	public void react(long now, Simulator sim, Object todo) {

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
	 * @param where Unused.
	 */
	@Override
	public void showCurrentValue(Point where) {
		
		String value = "off";
		if (currentValue != null) {
			String hex = BitSetUtils.ToString(currentValue,16);
			String unsigned = BitSetUtils.ToString(currentValue,10);
			String signed = BitSetUtils.ToStringSigned(currentValue,bits);
			value = "0x" + hex + " (" + unsigned + " unsigned, " + signed + " signed)";
		}
		TellUser.info(JLSInfo.frame, value, "Information");
	} // end of showCurrentValue method

} // end of InputPin class
