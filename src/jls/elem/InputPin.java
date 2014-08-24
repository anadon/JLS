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
public class InputPin extends Pin implements TriProp {
	
	// properties
	private boolean loadTriState = false;
	
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
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		return super.setup(g,editWindow,x,y,"Input");
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * Most work done in superclass, but output point added here.
	 * 
	 * @param g The Graphics object used to compute the size of the name.
	 */
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
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// set up shape
		int s = JLSInfo.spacing;
		Polygon p = new Polygon();
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			p.addPoint(x,y);
			p.addPoint(x+width-s,y);
			p.addPoint(x+width,y+height/2);
			p.addPoint(x+width-s,y+height);
			p.addPoint(x,y+height);
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			p.addPoint(x+s, y);
			p.addPoint(x, y+height/2);
			p.addPoint(x+s, y+height);
			p.addPoint(x+width, y+height);
			p.addPoint(x+width, y);
			
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			p.addPoint(x, y);
			p.addPoint(x+width, y);
			p.addPoint(x+width, y+height-s);
			p.addPoint(x+width/2, y+height);
			p.addPoint(x, y+height-s);	
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			p.addPoint(x+width/2, y);
			p.addPoint(x+width, y+s);
			p.addPoint(x+width, y+height);
			p.addPoint(x, y+height);
			p.addPoint(x, y+s);
		}
		// draw watched background
		if (watched) {
			g.setColor(JLSInfo.watchColor);
			g.fillPolygon(p);
		}
		
		// draw context
		super.draw(g);
		
		// draw box
		Rectangle original = getRect();
		Rectangle r = new Rectangle(original.x,original.y,original.width-s,original.height);
		g.setColor(Color.BLACK);
		g.drawPolygon(p);
		
		// draw name inside box
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D t = fm.getStringBounds(name,g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int bx = x;
		int by = y;
		int bwidth = width;
		int bheight = height;
		if (orientation == JLSInfo.Orientation.LEFT) {
			bx = x + s;
			bwidth = width - s;
		}
		else if (orientation == JLSInfo.Orientation.RIGHT) {
			bwidth = width - s;
		}
		else if (orientation == JLSInfo.Orientation.UP) {
			by = y + s;
			bheight = height - s;
		}
		else { // DOWN
			bheight = height - s;
		}
		int dx = (int) ((bwidth - tw) / 2);
		int dy = (int) ((bheight - th) / 2 + fm.getAscent());

		g.drawString(name, bx + dx, by + dy);
		// draw output
		outputs.get(0).draw(g);
		
	} // end of draw method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT InputPin");
		if (outputs.get(0).isTriState()) {
			output.println(" int tristate 1");
		}
		super.save(output);
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
	 * Copy this element.
	 */
	public Element copy() {
		
		InputPin it = new InputPin(circuit);
		it.name = name;
		it.bits = bits;
		it.orientation = orientation;
		it.watched = watched;
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		String tri = "";
		if (outputs.get(0).isTriState())
			tri = " (tri-state) ";
		info.setText(bits + " bit input pin" + tri + ", value = " +
				BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method

	/**
	 * Print current value to stdout.
	 * 
	 * @param qual Qualified subcircuit name.
	 */
	public void printValue(String qual) {
		
		if (qual.equals("")) {
			System.out.printf("Input Pin %s: %s\n", name, BitSetUtils.toDisplay(currentValue,bits));
		}
		else {
			System.out.printf("Input Pin %s.%s: %s\n", qual, name, BitSetUtils.toDisplay(currentValue,bits));
		}
		
	} // end of printValue method
	
	/**
	 * Set this element to tri-state or not and propagate to output(s).
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		for (Output output : outputs) {
			output.setTriState(which);
		}
	} // end of setTriState method
	
	/**
	 * Remove this element, but only if it is not part of a subcircuit.
	 * 
	 * @param circuit The circuit this element is in.
	 */
	public void remove(Circuit circ) {
		
		if (circ.isImported()) {
			JOptionPane.showMessageDialog(JLSInfo.frame,
					"Can't remove input pin " + name + " from a subcircuit");
			return;
		}
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Tells if a InputPin is capable of rotatating, can only rotate when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		return !outputs.get(0).isAttached();
	}
	
	/**
	 *  This method will rotate the InputPin if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT)
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT)
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
		}
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a InputPin is capable of flipping, can only flip when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		return !outputs.get(0).isAttached();
	}
	
	/**
	 * This method will flip an InputPin
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			orientation = JLSInfo.Orientation.RIGHT;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			orientation = JLSInfo.Orientation.LEFT;
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			orientation = JLSInfo.Orientation.DOWN;
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			orientation = JLSInfo.Orientation.UP;
		}
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method
	
	/**
	 * Return a string representation of this InputPin.
	 */
	public String toString() {
		
		return "[InputPin " + name + "(" + bits + " bits)]";
	} // end of toString method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	private BitSet currentValue = new BitSet();

	/**
	 * Get the current value.
	 * 
	 * @return the current value.
	 */
	public BitSet getCurrentValue() {
		
		if (currentValue == null)
			return null;
		else
			return (BitSet)currentValue.clone();
	} // end of getCurrentValue method
	
	/**
	 * Initialize output to 0 or null.
	 * 
	 * @param sim Unused.
	 */
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
	public void showCurrentValue(Point where) {
		
		String value = "off";
		if (currentValue != null) {
			String hex = BitSetUtils.ToString(currentValue,16);
			String unsigned = BitSetUtils.ToString(currentValue,10);
			String signed = BitSetUtils.ToStringSigned(currentValue,bits);
			value = "0x" + hex + " (" + unsigned + " unsigned, " + signed + " signed)";
		}
		JOptionPane.showMessageDialog(JLSInfo.frame, value, "Information",
				JOptionPane.INFORMATION_MESSAGE);
	} // end of showCurrentValue method

} // end of InputPin class
