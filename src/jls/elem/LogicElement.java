package jls.elem;

import jls.*;
import jls.sim.*;

import java.io.PrintWriter;
import java.util.*;

/**
 * Superclass for active elements.
 * Contains common data and methods.
 * 
 * @author David A. Poplawski
 */
public abstract class LogicElement extends Element implements Reacts {
	
	// run time properties
	private int lx;					// position if not snapped to grid line
	private int ly;
	private int savex;				// position to return to if bad placement
	private int savey;
	protected Vector<Input> inputs = new Vector<Input>();
	protected Vector<Output> outputs = new Vector<Output>();
	
	/**
	 * Construct a new logic element.
	 */
	public LogicElement(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Set initial position with snap-to grid lines.
	 * 
	 * @param x Initial x-coordinate.
	 * @param y Initial y-coordinate.
	 */
	public void setXY(int x, int y) {
		
		// save non snap-to position
		lx = x;
		ly = y;
		
		// compute snap-to position
		int s = JLSInfo.spacing;
		int nx = (x+s/2)/s*s;
		int ny = (y+s/2)/s*s;
		
		// save snap-to position
		super.setXY(nx,ny);
	} // end of setXY method
	
	/**
	 * Set value (in load context).
	 * Need to grab x and y in order to snap correctly.
	 * 
	 * @param name The name of the value.
	 * @param value The initial value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("x")) {
			lx = value;
		}
		else if (name.equals("y")) {
			ly = value;
		}
		super.setValue(name,value);
	} // end of setValue method
	
	/**
	 * Copy values to new object.
	 * 
	 * @param it The new object.
	 */
	public void copy(LogicElement it) {
		
		it.lx = lx;
		it.ly = ly;
		it.savex = savex;
		it.savey = savey;
		super.copy(it);
	} // end of copy method
	
	/**
	 * Move with snap to grid lines.
	 * 
	 * @param dx Distance to move horizontally.
	 * @param dy Distance to move vertically.
	 */
	public void move(int dx, int dy) {
		
		// modify non snap-to position
		lx += dx;
		ly += dy;
		
		// compute and set snap-to position
		int s = JLSInfo.spacing;
		int nx = (lx+s/2)/s*s;
		int ny = (ly+s/2)/s*s;
		super.setXY(nx,ny);
		
		// move all attached wire ends
		for (Input input : inputs) {
			WireEnd end = input.getWireEnd();
			if (end != null)
				end.move(dx,dy);
		}
		for (Output output : outputs) {
			WireEnd end = output.getWireEnd();
			if (end != null)
				end.move(dx,dy);
		}
		
	} // end of move method
	
	/**
	 * Save current coordinates of this element and all attached wire ends
	 * in case move doesn't work.
	 */
	public void savePosition() {
		
		// save non snap-to position
		savex = lx;
		savey = ly;
		
		// save snap-to position
		super.savePosition();
		
		// save position of inputs and attached wire ends
		for (Input input : inputs) {
			input.savePosition();
			if (input.isAttached()) {
				WireEnd end = input.getWireEnd();
				end.savePosition();
			}
		}
		for (Output output : outputs) {
			output.savePosition();
			if (output.isAttached()) {
				WireEnd end = output.getWireEnd();
				end.savePosition();
			}
		}
	} // end of savePosition method
	
	/**
	 * Restore saved coordinates of this element and all attached wire ends
	 * when move doesn't work.
	 */
	public void restorePosition() {
		
		// restore non snap-to position
		lx = savex;
		ly = savey;
		
		// restore snap-to position
		super.restorePosition();
		
		// restore position of puts and attached wire ends
		for (Input input : inputs) {
			input.restorePosition();
			if (input.isAttached()) {
				WireEnd end = input.getWireEnd();
				end.restorePosition();
			}
		}
		for (Output output : outputs) {
			output.restorePosition();
			if (output.isAttached()) {
				WireEnd end = output.getWireEnd();
				end.restorePosition();
			}
		}
		
	} // end of restorePosition method
	
	/**
	 * Fix the position of this element, and all attached wire ends.
	 */
	public void fixPosition() {
		
		// make non snap-to position the same as the snap-to position
		lx = x;
		ly = y;
		
		// fix position of attached wire ends
		for (Input input : inputs) {
			if (input.isAttached()) {
				WireEnd end = input.getWireEnd();
				end.fixPosition();
			}
		}
		for (Output output : outputs) {
			if (output.isAttached()) {
				WireEnd end = output.getWireEnd();
				end.fixPosition();
			}
		}
	} // end of fixPosition method
	
	/**
	 * Remove this element from the circuit.
	 * Unattaches from all wire nets.
	 * Forces wire nets it disconnects from to recheck their information.
	 * 
	 * @param circuit The circuit it is being removed from.
	 */
	public void remove(Circuit circ) {
		
		for (Put p : inputs) {
			if (p.isAttached()) {
				WireEnd end = p.getWireEnd();
				end.setPut(null);
				end.getNet().recheck();
			}
		}
		for (Put p : outputs) {
			if (p.isAttached()) {
				WireEnd end = p.getWireEnd();
				end.setPut(null);
				end.getNet().recheck();
			}
		}
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Find input or output in this element that a given x,y is close to (if any).
	 * Clears all touching flags.
	 * Close is less than JLSInfo.spacing/2 in any direction.
	 * 
	 * @param x The given x-coordinate.
	 * @param y The given y-coordinate.
	 * 
	 * @return the input or output that the given coordinates are close to,
	 * or null if not close to any in this element.
	 */
	public Put getPut(int x, int y) {
		
		for (Input input : inputs) {
			//input.setTouching(false);
			if (Math.abs(x-input.getX()) < JLSInfo.spacing/2 &&
					Math.abs(y-input.getY()) < JLSInfo.spacing/2) {
				return input;
			}
		}
		for (Output output : outputs) {
			//output.setTouching(false);
			if (Math.abs(x-output.getX()) < JLSInfo.spacing/2 &&
					Math.abs(y-output.getY()) < JLSInfo.spacing/2) {
				return output;
			}
		}
		return null;
	} // end of getPut method
	
	/**
	 * Set/reset touching flag for all inputs and outputs.
	 */
	public void setTouching(boolean setting) {
		
		for (Put p : inputs) {
			p.setTouching(setting);
		}
		for (Put p : outputs) {
			p.setTouching(setting);
		}
	} // end of setTouching method
	
	/**
	 * Get all inputs and outputs.
	 * Generally overridden.
	 * 
	 * @return a set of all inputs and outputs.
	 */
	public Set<Put> getAllPuts() {
		
		Set<Put>all = new HashSet<Put>(inputs);
		all.addAll(outputs);
		return all;
	} // end of getAllPuts method
	
	/**
	 * Get put by name.
	 * 
	 * @param name The name of the put.
	 * 
	 * @return the put, or null if no such put.
	 */
	public Put getPut(String name) {
		
		for (Put p : inputs) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		for (Put p : outputs) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	} // end of getPut method
	
	/**
	 * Get input by name.
	 * 
	 * @param name The name of the input.
	 * 
	 * @return the input, or null if no such input.
	 */
	public Input getInput(String name) {
		
		for (Input p : inputs) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	} // end of getInput method

	/**
	 * Get ouput by name.
	 * 
	 * @param name The name of the output.
	 * 
	 * @return the output, or null if no such output.
	 */
	public Output getOutput(String name) {
		
		for (Output p : outputs) {
			if (name.equals(p.getName())) {
				return p;
			}
		}
		return null;
	} // end of getOutput method

	/**
	 * Detach from all connections.
	 * Delete all inputs and outputs, getting ready to init all over again.
	 */
	protected void detach() {
		
		for (Input input : inputs) {
			if (input.isAttached()) {
				WireEnd end = input.getWireEnd();
				end.setPut(null);
				end.getNet().recheck();
			}
		}
		for (Output output : outputs) {
			if (output.isAttached()) {
				WireEnd end = output.getWireEnd();
				end.setPut(null);
				end.getNet().recheck();
			}
		}
		inputs.clear();
		outputs.clear();
	} // end of detach method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
	
	/**
	 * Initialize for simulation.
	 * 
	 * @param sim The simulator object.
	 */
	public void initSim(Simulator sim) {
		
		System.out.println("initSim not implemented: " + getClass().getName());
	} // end of initSim method
				
	/**
	 * Initialize all inputs to 0.
	 */
	public void initInputs() {
		
		for (Input in : inputs) {
			in.setValue(BitSetUtils.Create((long)0));
		}
	} // end of initInputs method
	
	/**
	 * Reset propagation delay (overridden by most elements).
	 */
	public void resetPropDelay() {
		
	} // end of resetPropDelay method
	
	/**
	 * Set propagation delay (overridden by most elements).
	 * 
	 * @param newDelay The new delay value.
	 */
	public void setDelay(int newDelay) {
		
	} // end of setDelay method

	/**
	 * Get the name of this element.
	 * Overridden in elements that have names.
	 */
	public String getName() {
		
		return "";
	} // end of getName method
	
	/**
	 * Get the fully qualified name of this element, in the form
	 * sub1.sub2.elem, where sub1, sub2, etc. are subcircuit names
	 * and elem is the element name.
	 * 
	 * @return the fully qualified name.
	 */
	public String getFullName() {
		
		Circuit circ = getCircuit();
		String name = getName();
		while (circ.isImported()) {
			name = circ.getSubElement().getName() + "." + name;
			circ = circ.getSubElement().getCircuit();
		}
		return name;
	} // end of getFullName method
	
	/**
	 * For elements that don't react.
	 * Overridden by most elements.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		throw new UnsupportedOperationException("no react");
	} // end of react method
	
	//-----------------------------------------------------------------------
	// these shouldn't be called
	
	public int getBits() {
		
		throw new UnsupportedOperationException("no getBits");
	} // end of getBits method
	
	public BitSet getCurrentValue() {
		
		throw new UnsupportedOperationException("no getCurrentValue");
	} // end of getCurrentValue method
	
} // end of LogicElement class
