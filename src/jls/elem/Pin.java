package jls.elem;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.*;

import java.io.*;
import java.util.BitSet;

/**
 * Superclass of input and output pins.
 * Contains common data and methods.
 * 
 * @author David A. Poplawski
 */
public abstract sealed class Pin extends LogicElement
		implements Watchable
		permits InputPin, OutputPin {
	
	// saved properties
	/** The name of this pin. */
	protected String name;
	/** The number of bits in this pin. */
	protected int bits;
	/** Whether this pin is watched (shown in the signal trace). */
	protected boolean watched = false;
	/** The direction this pin faces. */
	protected Orientation orientation = Orientation.RIGHT;

	/**
	 * Create a new input pin.
	 * 
	 * @param circ The circuit this pin will be in.
	 */
	public Pin(Circuit circ) {
		
		super(circ);
	} // end of constructor
	
	/**
	 * Return a string version of this element.
	 * 
	 * @return the string.
	 */
	@Override
	public String toString() {
		
		return name + ",bits=" + bits + ",watched=" + watched + ",hashCode=" + hashCode();
	} // end of toString method
	
	/**
	 * Get the name of this pin.
	 * 
	 * @return the name.
	 * @jls.testedby jls.BatchSimulationGoldenTest#simulate()
	 * @jls.testedby jls.ElementSimulationGoldenTest#pinValue()
	 * @jls.testedby jls.SequentialGoldenTest#simulate()
	 * @jls.testedby jls.SequentialGoldenTest#simulateWithVectors()
	 * @jls.testedby jls.ShiftRegisterTest#pinValue()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#pinValue()
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this pin.
	 * 
	 * @return the number of bits.
	 */
	@Override
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Get the orientation this pin faces (for the relocated renderer and
	 * creation dialog, issue #77).
	 *
	 * @return the orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the name of this pin (for the relocated creation dialog, issue
	 * #77). The caller registers the name with the circuit.
	 *
	 * @param name The new name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * Set the number of bits in this pin (for the relocated creation
	 * dialog, issue #77).
	 *
	 * @param bits The new bit width.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the orientation this pin faces (for the relocated creation
	 * dialog, issue #77).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Initialize internal info for this element.
	 * Computes the size of the pin as a function of the name.
	 *
	 * @param g Graphics object used to compute the size of the name.
	 */
	@Override
	public void init(java.awt.Graphics g) {

		// set up size if needed
		if (g != null) {

			if (width == 0 && height == 0) {
				int s = Geometry.SPACING;
				java.awt.FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(" " + name + " ");
				if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT)
				{
					width = Math.max((w+s/2)/s*s,2*s)+s;	// ceiling in spacings
					height = 2*s;
				}
				else
				{
					width = Math.max((w+s/2)/s*s,2*s);
					if(width % (2*s) != 0)
					{
						width += s;
					}
					height = 3*s;
				}
			}
		}
	} // end of init method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for the attributes shared by both pins.
	/** The saved attributes shared by both pin types, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/** Read the pin's name for saving. */
			@Override
			protected String get(Element el) { return ((Pin)el).name; }
			/** Load the pin's name and register it with the circuit. */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((Pin)el).name = v;
				el.getCircuit().addName(v);
			}
			/** Copy the pin's name to another element without re-registering it. */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten pin copies assigned the field
				// without registering the name
				((Pin)to).name = ((Pin)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/** Read the pin's bit width for saving. */
			@Override
			protected int get(Element el) { return ((Pin)el).bits; }
			/** Load the pin's bit width. */
			@Override
			protected void set(Element el, int v) { ((Pin)el).bits = v; }
		},
		new Attribute.IntAttribute("watch") {
			/** Read the pin's watched flag as an int (1 if watched, else 0). */
			@Override
			protected int get(Element el) { return ((Pin)el).watched ? 1 : 0; }
			/** Load the pin's watched flag from an int (non-zero means watched). */
			@Override
			protected void set(Element el, int v) { ((Pin)el).watched = v != 0; }
		},
		new Attribute.OrientationAttribute("orient") {
			/** Read the pin's orientation for saving. */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Pin)el).orientation;
			}
			/** Load the pin's orientation. */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Pin)el).orientation = o;
			}
		}
	);

	/** Base attributes plus the shared pin attributes, in save order. */
	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus the shared pin attributes, in save order
	 * (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method
	
	/**
	 * Pins cannot be copied (copy/paste).
	 * 
	 * @return false.
	 */
	public boolean canCopy() {
		
		return false;
	} // end of canCopy method
	
	/**
	 * Save this element in a file.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * The pin kind for user-facing messages ("Input" or "Output").
	 *
	 * @return the pin kind, "Input" or "Output".
	 */
	protected abstract String pinKind();

	/**
	 * Load-time tri-state marker (" int tristate 1" in saved files).
	 */
	protected boolean loadTriState = false;

	/**
	 * Set an int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, int value) {

		if (name.equals("tristate")) {
			loadTriState = true;
		} else {
			super.setValue(name, value);
		}
	} // end of setValue method

	/**
	 * Print current value to stdout.
	 *
	 * @param qual Qualified subcircuit name.
	 */
	public void printValue(String qual) {

		if (qual.isEmpty()) {
			System.out.printf("%s Pin %s: %s\n", pinKind(), name,
					BitSetUtils.toDisplay(currentValue, bits));
		}
		else {
			System.out.printf("%s Pin %s.%s: %s\n", pinKind(), qual, name,
					BitSetUtils.toDisplay(currentValue, bits));
		}
	} // end of printValue method

	/**
	 * Remove this element, but only if it is not part of a subcircuit.
	 *
	 * @param circ The circuit this element is in.
	 */
	@Override
	public void remove(Circuit circ) {

		if (circ.isImported()) {
			TellUser.error(JLSInfo.frame,
					"Can't remove " + pinKind().toLowerCase() + " pin "
					+ name + " from a subcircuit", "Error");
			return;
		}
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method

	/**
	 * A pin can rotate or flip only when its put has no attachment.
	 *
	 * @return false if the put has a wire attached, true otherwise.
	 */
	@Override
	public boolean canRotate() {

		for (Input in : inputs) {
			if (in.isAttached()) {
				return false;
			}
		}
		for (Output out : outputs) {
			if (out.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canRotate method

	/**
	 * A pin can rotate or flip only when its put has no attachment.
	 *
	 * @return false if the put has a wire attached, true otherwise.
	 */
	@Override
	public boolean canFlip() {

		return canRotate();
	} // end of canFlip method

	/**
	 * Rotate this pin, rebuilding its geometry and put.
	 *
	 * @param direction The direction to rotate.
	 * @param g The current graphics context for size recalculation.
	 */
	@Override
	public void rotate(Orientation direction, java.awt.Graphics g) {

		if (direction == Orientation.LEFT) {
			orientation = orientation.ccw();
		}
		else if (direction == Orientation.RIGHT) {
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of rotate method

	/**
	 * Flip this pin, rebuilding its geometry and put.
	 *
	 * @param g The current graphics context for size recalculation.
	 */
	@Override
	public void flip(java.awt.Graphics g) {

		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	// -----------------------------------------------------------------
	// Simulation state shared by both pins
	// -----------------------------------------------------------------

	/** The current simulated value of this pin. */
	protected BitSet currentValue = new BitSet();

	/**
	 * Get the current value.
	 *
	 * @return the current value.
	 * @jls.testedby jls.BatchSimulationGoldenTest#simulate()
	 * @jls.testedby jls.ElementSimulationGoldenTest#pinValue()
	 * @jls.testedby jls.SequentialGoldenTest#simulate()
	 * @jls.testedby jls.SequentialGoldenTest#simulateWithVectors()
	 * @jls.testedby jls.ShiftRegisterTest#pinValue()
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#pinValue()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()
	 */
	@Override
	public BitSet getCurrentValue() {

		if (currentValue == null)
			return null;
		else
			return (BitSet) currentValue.clone();
	} // end of getCurrentValue method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public String infoText() {

		return bits + " bit input pin";
	} // end of showInfo method
	/**
	 * A pin be watched.
	 * 
	 * @return true.
	 */
	@Override
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this pin is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this pin is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
} // end of Pin class
