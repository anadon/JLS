package jls.elem;

import java.io.*;
import java.math.*;
import java.util.*;

import jls.*;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.sim.*;

/**
 * Constant output value.
 *
 * @author David A. Poplawski
 */
public final class Constant extends LogicElement
		implements Rotatable, Editable, QuickEditable {

	// named constants
	/** Default constant value. */
	private static final BigInteger defaultValue = BigInteger.ZERO;
	/** Default display radix. */
	private static final int defaultBase = 10;

	// saved properties
	/** The value this constant outputs. */
	private BigInteger value = defaultValue;
	/** The radix the value is displayed in (2, 10 or 16). */
	private int base = defaultBase;
	/** Which way this constant faces. */
	private Orientation orientation = Orientation.RIGHT;

	// running properties
	/** Value of the previously created constant. */
	private static BigInteger previousValue = defaultValue;
	/** Display radix of the previously created constant. */
	private static int previousBase = defaultBase;
	/** Orientation of the previously created constant. */
	private static Orientation previousOrientation = Orientation.RIGHT;

	/**
	 * Create a new constant element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public Constant(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		int s = Geometry.SPACING;
		// set up size if there is a graphics object
		if (g != null) {

			// if element already has a size, use it
			if (width == 0 && height == 0) {
				jls.core.TextMetrics fm = g;
				int w = fm.stringWidth(Util.convert(value,base,true))+s;
				width = Math.max((w+s/2)/s*s,2*s);	// ceiling in spacings
				if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT)
				{
					height = 0;	// not really, but bounding rectangle will be large enough
				}
				else
				{
					height = 2*s;
				}
			}

		}
		// create output
		if(orientation == Orientation.LEFT)
		{
			outputs.add(new Output("output",this,0,0,0));
		}
		else if(orientation == Orientation.RIGHT)
		{
			outputs.add(new Output("output",this,width,0,0));
		}
		else if(orientation == Orientation.UP)
		{
			outputs.add(new Output("output",this,width/2,0,0));
		}
		else if(orientation == Orientation.DOWN)
		{
			outputs.add(new Output("output",this,width/2,height,0));
		}
	} // end of init method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** The persistence attributes specific to constants. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.BigIntAttribute("value") {
			/**
			 * Read the constant's value for persistence.
			 *
			 * @param el The Constant element to read from.
			 * @return the stored value.
			 */
			@Override
			protected BigInteger get(Element el) { return ((Constant)el).value; }
			/**
			 * Write the constant's value when loading.
			 *
			 * @param el The Constant element to write to.
			 * @param v The value to store.
			 */
			@Override
			protected void set(Element el, BigInteger v) { ((Constant)el).value = v; }
		},
		new Attribute.IntAttribute("base") {
			/**
			 * Read the constant's display radix for persistence.
			 *
			 * @param el The Constant element to read from.
			 * @return the stored base.
			 */
			@Override
			protected int get(Element el) { return ((Constant)el).base; }
			/**
			 * Write the constant's display radix when loading.
			 *
			 * @param el The Constant element to write to.
			 * @param v The base to store.
			 */
			@Override
			protected void set(Element el, int v) { ((Constant)el).base = v; }
		},
		new Attribute.StringAttribute("orient") {
			/**
			 * Read the constant's orientation as a string for persistence.
			 *
			 * @param el The Constant element to read from.
			 * @return the orientation name.
			 */
			@Override
			protected String get(Element el) {
				return ((Constant)el).orientation.toString();
			}
			/**
			 * Write the constant's orientation from a string when loading;
			 * unknown strings leave the orientation unchanged.
			 *
			 * @param el The Constant element to write to.
			 * @param v The orientation name to parse.
			 */
			@Override
			protected void set(Element el, String v) {
				// unknown strings leave the orientation unchanged,
				// as the handwritten loader always did
				for (Orientation o : Orientation.values()) {
					if (o.toString().equals(v)) {
						((Constant)el).orientation = o;
					}
				}
			}
		}
	);

	/** Base attributes plus this element's own, in save order. */
	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		Constant it = new Constant(getCircuit());
		it.outputs.add(outputs.get(0).copy(it));
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

		output.println("ELEMENT Constant");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Get the value of this constant (for consumers of the circuit
	 * model, e.g. the HDL exporter, issue #60).
	 *
	 * @return the value.
	 */
	public BigInteger getValue() {

		return value;
	} // end of getValue method

	/**
	 * Set the value of this constant (for the relocated creation/change
	 * dialog, issue #77).
	 *
	 * @param value The new value.
	 */
	public void setValue(BigInteger value) {

		this.value = value;
	} // end of setValue method

	/**
	 * Get the display radix of this constant (for the relocated
	 * creation/change dialog and renderer, issue #77).
	 *
	 * @return the base.
	 */
	public int getBase() {

		return base;
	} // end of getBase method

	/**
	 * Set the display radix of this constant (for the relocated
	 * creation/change dialog, issue #77).
	 *
	 * @param base The new base.
	 */
	public void setBase(int base) {

		this.base = base;
	} // end of setBase method

	/**
	 * Get the orientation of this constant (for the relocated creation
	 * dialog and renderer, issue #77).
	 *
	 * @return the orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the orientation of this constant (for the relocated creation
	 * dialog, issue #77).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Display info about this element.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		return "a constant value";
	} // end of showInfo method

	/**
	 * Get the rectangle bounding this element.
	 *
	 * @return the rectangle bounding this element.
	 */
	@Override
	public jls.core.Bounds getRect() {
		if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT)
		{
			return new jls.core.Bounds(x,y-Geometry.SPACING/2,width,height+Geometry.SPACING);
		}
		else
			return new jls.core.Bounds(x,y,width,height);

	} // end of getRect method

	/**
	 * Set values to those from previous constant element created.
	 */
	public void setToPrevious() {

		value = previousValue;
		base = previousBase;
		orientation = previousOrientation;
	} // end of setToPrevious method

	/**
	 * Save this constant's current value, radix and orientation as the
	 * defaults for the next constant created (for the relocated creation
	 * dialog, issue #77).
	 */
	public void saveAsPrevious() {

		previousValue = value;
		previousBase = base;
		previousOrientation = orientation;
	} // end of saveAsPrevious method

	/**
	 * Tells if a constant is capable of rotatating, can only rotate when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		return !outputs.get(0).isAttached();
	}

	/**
	 *  This method will rotate the constant if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		if(direction == Orientation.LEFT)
		{
			orientation = orientation.ccw();

		}
		else if(direction == Orientation.RIGHT)
		{
			orientation = orientation.cw();
		}
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Tells if a constant is capable of flipping, can only flip when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !outputs.get(0).isAttached();
	}

	/**
	 * This method will flip a constant
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		orientation = orientation.flipped();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Detach the constant, reset its size, and re-initialize so it grows
	 * to fit a newly-entered value (for the relocated change dialog, issue
	 * #77). This runs the former {@code Constant.change} resize step.
	 *
	 * @param g The graphics context used to recompute the size.
	 */
	public void resizeToFit(jls.core.TextMetrics g) {

		detach();
		width = 0;
		height = 0;
		init(g);
	} // end of resizeToFit method

	/**
	 * See if the given value fits in the box on the screen.
	 *
	 * @param g The graphics context (for font metrics).
	 * @param value The value to check.
	 *
	 * @return true if it fits, false if not.
	 */
	public boolean valueFits(jls.core.TextMetrics g, String value) {

		int s = Geometry.SPACING;
		jls.core.TextMetrics fm = g;
		int w = fm.stringWidth(value)+s;
		int rw = Math.max((w+s/2)/s*s,2*s);
		return rw <= width;

	} // end of valueFits method

	/**
	 * Add one to this constant's value (for the relocated quick-change
	 * shortcut menu, issue #77).
	 */
	public void incrementValue() {

		value = value.add(BigInteger.ONE);
	} // end of incrementValue method

	/**
	 * Subtract one from this constant's value, but not below zero (for the
	 * relocated quick-change shortcut menu, issue #77).
	 */
	public void decrementValue() {

		value = value.subtract(BigInteger.ONE);
		value = value.max(BigInteger.ZERO);
	} // end of decrementValue method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/**
	 * Initialize this element by generating an output and sending it.
	 *
	 * @param sim The simulator to post events to.
	 */
	@Override
	public void initSim(Simulator sim) {

		// create value
		BitSet bitval = BitSetUtils.Create(value);

		// set output so propagate will work
		BitSet opposite = (BitSet)bitval.clone();
		opposite.flip(0);	// at least one bit different
		Output out = outputs.get(0);
		out.setValue(opposite);

		// post output event
		sim.post(new SimEvent(0,this,bitval));
	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	@Override
	public void react(long now, Simulator sim, @org.jspecify.annotations.Nullable Object todo) {

		// get the new output value; a Constant has no inputs, so it only
		// ever reacts to its own posted value events (todo is never null)
		BitSet newValue = (BitSet)todo;
		if (newValue == null) {
			throw new IllegalStateException(
					"constant reacted without a value to output");
		}

		// send correct number of bits to output
		Output out = (Output)(outputs.toArray()[0]);
		if (!out.isAttached())
			return;
		WireEnd end = out.getWireEnd();
		if (end == null)
			throw new IllegalStateException("attached output has no wire end");
		int bits = end.getBits();
		BitSet mask = new BitSet(bits);
		mask.set(0,bits);
		newValue.and(mask);
		out.propagate(newValue,now,sim);

	} // end of react method

} // end of Constant class
