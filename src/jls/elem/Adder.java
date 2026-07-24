package jls.elem;

import jls.core.Geometry;
import jls.core.GridPoint;
import jls.core.Orientation;
import jls.*;
import jls.sim.*;

import java.io.*;
import java.util.*;

/**
 * An n-bit adder with carry in and carry out (n chosen by user).
 * Propagation delay is proportional to the number of bits.
 * 
 * @author David A. Poplawski
 */
public final class Adder extends LogicElement implements Timed {
	
	// default values
	/** The default number of bits. */
	private static final int defaultBits = 1;
	/** The default propagation delay per bit. */
	private static final int defaultPropDelay = 30;

	// saved properties
	/** The number of bits in each addend and the sum. */
	private int bits = defaultBits;
	/** The propagation delay, in simulated time units. */
	private int propDelay = defaultPropDelay;
	/** Which way the adder faces. */
	private Orientation orientation = Orientation.RIGHT;

	/**
	 * Create a new adder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Adder(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * The direction this adder faces (issue #77: read by the GUI-side
	 * renderer and dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the number of bits (issue #77: applied by the GUI-side dialog).
	 *
	 * @param bits The new number of bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the orientation (issue #77: applied by the GUI-side dialog).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.TextMetrics g) {

		// canonical geometry (RIGHT), transformed to the current
		// orientation (#24)
		int s = Geometry.SPACING;
		height = 4*s;
		width = 4*s;
		GridTransform.Chain t = placement();
		GridPoint a = t.map(0,s);
		GridPoint b = t.map(0,3*s);
		GridPoint cin = t.map(2*s,0);
		GridPoint sum = t.map(4*s,2*s);
		GridPoint cout = t.map(2*s,4*s);
		inputs.add(new Input("A",this,a.x(),a.y(),bits));
		inputs.add(new Input("B",this,b.x(),b.y(),bits));
		inputs.add(new Input("Cin",this,cin.x(),cin.y(),1));
		outputs.add(new Output("S",this,sum.x(),sum.y(),bits));
		outputs.add(new Output("Cout",this,cout.x(),cout.y(),1));
	} // end of init method

	/**
	 * The transform from canonical geometry (RIGHT) to the current
	 * orientation.
	 *
	 * @return the transform chain for the current orientation.
	 */
	private GridTransform.Chain placement() {

		int s = Geometry.SPACING;
		GridTransform.Chain t = GridTransform.chain(4*s, 4*s);
		switch (orientation) {
		case RIGHT:
			break;
		case LEFT:
			t.mirrorX();
			break;
		case DOWN:
			t.rotateCW().mirrorX();
			break;
		case UP:
			t.rotateCW().mirrorX().mirrorY();
			break;
		}
		return t;
	} // end of placement method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes: bits, delay, orient (#23). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width from the given adder.
			 *
			 * @param el The adder to read from.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((Adder)el).bits; }
			/**
			 * Set the bit width on the given adder.
			 *
			 * @param el The adder to modify.
			 * @param v The new number of bits.
			 */
			@Override
			protected void set(Element el, int v) { ((Adder)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from the given adder.
			 *
			 * @param el The adder to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((Adder)el).propDelay; }
			/**
			 * Set the propagation delay on the given adder.
			 *
			 * @param el The adder to modify.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Adder)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the orientation from the given adder.
			 *
			 * @param el The adder to read from.
			 * @return the current orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Adder)el).orientation;
			}
			/**
			 * Set the orientation on the given adder.
			 *
			 * @param el The adder to modify.
			 * @param o The new orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Adder)el).orientation = o;
			}
		}
	);

	/** Base attributes plus this element's own, in save order (#23). */
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
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Adder");
		super.save(output);
		output.println("END");
	} // end of save method
	
	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {
		
		Adder it = new Adder(circuit);
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
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
		
		return bits + " bit adder";
	} // end of showInfo method

	/**
	 * Adders have timing info (propagation delay).
	 * 
	 * @return true.
	 */
	@Override
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {
		
		propDelay = bits * defaultPropDelay;
	} // end of resetPropDelay method

	/**
	 * Get the propagation delay in this element.
	 * 
	 * @return the current delay.
	 */
	@Override
	public int getDelay() {
		
		return propDelay;
	} // end of getDelay method
	
	/**
	 * Set the propagation delay in this element.
	 * 
	 * @param temp The new delay amount.
	 */
	@Override
	public void setDelay(int temp) {
		
		propDelay = temp;
	} // end of setDelay method
	
	/**
	 * Tells if an adder is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		return !(inputs.get(0).isAttached() || inputs.get(1).isAttached() 
				|| inputs.get(2).isAttached() || outputs.get(0).isAttached() 
				|| outputs.get(1).isAttached());
	}
	
	/**
	 *  This method will rotate the adder if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.TextMetrics g)
	{
		if(direction == Orientation.LEFT)
		{
			orientation = orientation.ccw();
		}
		else if(direction == Orientation.RIGHT)
		{
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		init(g);
	}
	
	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached() || inputs.get(1).isAttached() 
				|| inputs.get(2).isAttached() || outputs.get(0).isAttached() 
				|| outputs.get(1).isAttached());
	}
	
	/**
	 * This method will flip an adder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.TextMetrics g)
	{
		orientation = orientation.flipped();
		outputs.clear();
		inputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The sum-and-carry value currently propagating through the adder. */
	private BitSet toBeValue;
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// set output pins to 0
		BitSet zero = new BitSet(1);
		for (Output output : outputs) {
			output.setValue((BitSet)zero.clone());
		}
		
		// set to-be values
		toBeValue = (BitSet)zero.clone();
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		// if the input has changed ...
		if (todo == null) {
			
			// get the input values
			BitSet a = inputs.get(0).getValue();
			if (a == null)
				a = new BitSet();
			BitSet b = inputs.get(1).getValue();
			if (b == null)
				b = new BitSet();
			BitSet cin = inputs.get(2).getValue();
			if (cin == null)
				cin = new BitSet();
			boolean c = true;
			if (cin.cardinality() == 0) {
				c = false;
			}
			
			// create new output values
			BitSet allsum = BitSetUtils.SumCarry(c,a,b);
		
			// if new value is different from the value propagating through
			// the adder, then post an event
			if (!allsum.equals(toBeValue)) {
				toBeValue = (BitSet)allsum.clone();
				sim.post(new SimEvent(now+propDelay,this,allsum));
			}
		}
		else {
			
			// get the new output value
			BitSet allsum = (BitSet)todo;
			
			// break into sum and carry
			BitSet sum = (BitSet)allsum.clone();
			BitSet carry = new BitSet(1);
			carry.set(0,sum.get(bits));
			sum.clear(bits);
			
			// send to outputs
			Output sumOut = outputs.get(0);
			sumOut.propagate(sum,now,sim);
			Output carryOut = outputs.get(1);
			carryOut.propagate(carry,now,sim);
		}
	} // end of react method

} // end of Adder class
