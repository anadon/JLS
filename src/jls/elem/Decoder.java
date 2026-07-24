package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.BitSetUtils;
import jls.Circuit;
import jls.sim.SimEvent;
import jls.sim.Simulator;

/**
 * n-input, 2^n-output decoder.
 * 
 * @author David A. Poplawski
 */
public final class Decoder extends LogicElement implements Timed {
	
	// default values
	/** Default number of input bits. */
	private static final int defaultBits = 1;
	/** Default propagation delay. */
	private static final int defaultPropDelay = 15; 
	
	// saved properties
	/** Number of input bits (the decoder has 2^bits outputs). */
	private int bits = defaultBits;
	/** Propagation delay of this decoder. */
	private int propDelay = defaultPropDelay;
	//Orientation is based off of where the inputs are
	/** Which side the input is on. */
	private Orientation orientation = Orientation.LEFT;
	
	// running properties
	/** The "decoder" label drawn on the element, abbreviated to "dec" if it doesn't fit. */
	private String dec;
	/** The input/output width label drawn on the element (e.g. "1-n"), oriented to match the element. */
	private String inout;

	/**
	 * Create a new decoder element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public Decoder(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The direction this decoder faces (issue #77: read by the GUI-side
	 * renderer and dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * The input/output width label drawn inside the box (issue #77: read by
	 * the GUI-side renderer).
	 *
	 * @return the input/output width label.
	 */
	public String getInout() {

		return inout;
	} // end of getInout method

	/**
	 * The "decoder" label drawn inside the box (issue #77: read by the
	 * GUI-side renderer).
	 *
	 * @return the decoder label.
	 */
	public String getDec() {

		return dec;
	} // end of getDec method

	/**
	 * Set the number of input bits (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param bits The new number of input bits.
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

		int s = Geometry.SPACING;
		int outs = 1 << bits;
	
		if(orientation == Orientation.LEFT)
		{
			inout = bits + "-" + outs;
		}
		else if(orientation == Orientation.UP)
		{
			inout = bits + "\n | \n" + outs;
		}
		else if(orientation == Orientation.DOWN)
		{
			inout = outs + "\n | \n" + bits;
		}
		else if(orientation == Orientation.RIGHT)
		{
			inout = outs + "-" + bits;
		}
		dec = " decoder ";
		
		// set up size if there is a graphics object
		if (g != null) {
			
			// if element already has a size, use it
			if (width == 0 && height == 0) {
				
				if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT)
				{
					width = 5*s;
					height = 2*s;
				}
				else
				{
					width = 2 * s;
					height = 5 * s;
				}
				jls.core.TextMetrics fm = g;
				int bw = fm.stringWidth(inout);
				if (bw > width && orientation == Orientation.LEFT) {
					inout = "1-n";
				}
				else if(bw > width && orientation == Orientation.RIGHT)
				{
					inout = "n-1";
				}
				int dw = fm.stringWidth(dec);
				if (dw > width) {
					dec = "dec";
				}
			}
			
		}
		
		
		if(orientation == Orientation.LEFT)
		{	
			// create input
			inputs.add(new Input("input",this,0,s,bits));
			// create output
			outputs.add(new Output("output",this,width,s,1<<bits));
		}
		else if(orientation == Orientation.RIGHT)
		{
			// create input
			inputs.add(new Input("input",this,width,s,bits));
			// create output
			outputs.add(new Output("output",this,0,s,1<<bits));
		}
		else if(orientation == Orientation.DOWN)
		{
			// create input
			inputs.add(new Input("input",this,s,height,bits));
			// create output
			outputs.add(new Output("output",this,s,0,1<<bits));
		}
		else if(orientation == Orientation.UP)
		{
			// create input
			inputs.add(new Input("input",this,s,0,bits));
			// create output
			outputs.add(new Output("output",this,s,height,1<<bits));
		}
	} // end of init method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes: bits, delay and orientation. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the input bit width from the given decoder.
			 *
			 * @param el The decoder to read from.
			 * @return the number of input bits.
			 */
			@Override
			protected int get(Element el) { return ((Decoder)el).bits; }
			/**
			 * Store the input bit width into the given decoder.
			 *
			 * @param el The decoder to write to.
			 * @param v The number of input bits.
			 */
			@Override
			protected void set(Element el, int v) { ((Decoder)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from the given decoder.
			 *
			 * @param el The decoder to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((Decoder)el).propDelay; }
			/**
			 * Store the propagation delay into the given decoder.
			 *
			 * @param el The decoder to write to.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Decoder)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the orientation from the given decoder.
			 *
			 * @param el The decoder to read from.
			 * @return the current orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Decoder)el).orientation;
			}
			/**
			 * Store the orientation into the given decoder.
			 *
			 * @param el The decoder to write to.
			 * @param o The new orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Decoder)el).orientation = o;
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
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT Decoder");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		Decoder it = new Decoder(circuit);
		it.inout = inout;
		it.dec = dec;
		it.inputs.add(inputs.get(0).copy(it));
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
		
		return bits + " to " + (1<<bits) + " decoder";

	} // end of showInfo method

	/**
	 * Decoders have timing info (propagation delay).
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
		
		propDelay = defaultPropDelay;
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
	 * Tells if a decoder is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	}
	
	/**
	 *  This method will rotate the decoder if it is rotateable.
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
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * This method will flip a decoder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.TextMetrics g)
	{
		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a decoder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	}

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The output value this decoder will take on once its propagation delay elapses. */
	private BitSet toBeValue;
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// set output pin to 0
		Output out = outputs.get(0);
		BitSet zero = new BitSet(1);
		out.setValue(zero);
		
		// set post output change to 1
		BitSet one = new BitSet(1);
		one.flip(0);
		sim.post(new SimEvent(0,this,one));
		
		// set to-be value
		toBeValue = (BitSet)one.clone();
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
			
			// get the input value
			BitSet value = inputs.get(0).getValue();
			if (value == null)
				value = new BitSet();
			
			// create new output value
			int inval = BitSetUtils.ToInt(value);
			BitSet newValue = new BitSet(inval+1);
			newValue.set(inval);

			// if new value is different from the value propagating through
			// the decoder, then post an event
			if (!newValue.equals(toBeValue)) {
				toBeValue = (BitSet)newValue.clone();
				sim.post(new SimEvent(now+propDelay,this,newValue));
			}
		}
		else {
			
			// get the new output value
			BitSet newValue = (BitSet)todo;
			
			// send to output
			Output out = outputs.get(0);
			out.propagate(newValue,now,sim);
		}
	} // end of react method
	
} // end of Decoder class
