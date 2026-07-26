package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Geometry;
import jls.core.GridPoint;
import jls.core.GridSize;
import jls.core.Orientation;
import jls.sim.*;
import jls.sim.SimEvent.MemoryRead;
import jls.sim.SimEvent.MemoryWrite;
import jls.sim.SimEvent.NewValue;
import jls.sim.SimEvent.PinChanged;
import jls.sim.SimEvent.StateChanged;
import jls.sim.SimEvent.TableOutput;
import jls.sim.SimEvent.TriStateOff;

/**
 * Multiplexor.
 *
 * @author David A. Poplawski
 */
public final class Mux extends LogicElement implements Timed, Rotatable {

	// default values
	/** Default number of data inputs. */
	private static final int defaultInputs = 2;
	/** Default number of bits per input. */
	private static final int defaultBits = 1;
	/** Default propagation delay. */
	private static final int defaultPropDelay = 25;

	// saved properties
	/** The number of data inputs. */
	private int numInputs = defaultInputs;
	/** The number of bits in each input and the output. */
	private int bits = defaultBits;
	/** The propagation delay of this element. */
	private int propDelay = defaultPropDelay;
	/** The direction the output points. */
	private Orientation outputOrientation = Orientation.RIGHT;
	/** The side the selector input is on. */
	private Orientation selectorOrientation = Orientation.DOWN;

	/**
	 * Create a new multiplexor element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public Mux(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The number of data inputs (issue #77: read by the GUI-side renderer).
	 *
	 * @return the current number of data inputs.
	 */
	public int getNumInputs() {

		return numInputs;
	} // end of getNumInputs method

	/**
	 * The direction the output points (issue #77: read by the GUI-side
	 * renderer and dialog).
	 *
	 * @return the current output orientation.
	 */
	public Orientation getOutputOrientation() {

		return outputOrientation;
	} // end of getOutputOrientation method

	/**
	 * Set the number of data inputs (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param numInputs The new number of data inputs.
	 */
	public void setNumInputs(int numInputs) {

		this.numInputs = numInputs;
	} // end of setNumInputs method

	/**
	 * Set the number of bits (issue #77: applied by the GUI-side dialog).
	 *
	 * @param bits The new number of bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the output orientation (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param orientation The new output orientation.
	 */
	public void setOutputOrientation(Orientation orientation) {

		this.outputOrientation = orientation;
	} // end of setOutputOrientation method

	/**
	 * Set the selector orientation (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param orientation The new selector orientation.
	 */
	public void setSelectorOrientation(Orientation orientation) {

		this.selectorOrientation = orientation;
	} // end of setSelectorOrientation method

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// canonical geometry (output RIGHT), transformed to the current
		// output orientation (#24); the selector side is independent of
		// that transform (input order never mirrors with the selector),
		// so its put is placed directly from its own orientation
		int s = Geometry.SPACING;
		GridTransform.Chain t = placement();
		GridSize d = t.size();
		width = d.width();
		height = d.height();

		// determine number of select bits
		int sbits = 32 - Integer.numberOfLeadingZeros(numInputs-1);

		// create select input
		if(selectorOrientation == Orientation.DOWN)
		{
			inputs.add(new Input("select",this,s,height,sbits));
		}
		else if(selectorOrientation == Orientation.UP)
		{
			inputs.add(new Input("select",this,s,0,sbits));
		}
		else if(selectorOrientation == Orientation.LEFT)
		{
			inputs.add(new Input("select",this,0,s,sbits));
		}
		else if(selectorOrientation == Orientation.RIGHT)
		{
			inputs.add(new Input("select",this,width,s,sbits));
		}

		// create inputs and output
		for (int i=0; i<numInputs; i+=1) {
			GridPoint p = t.map(0,(i+1)*s);
			inputs.add(new Input("input"+i,this,p.x(),p.y(),bits));
		}
		GridPoint p = t.map(2*s,(numInputs+1)/2*s);
		outputs.add(new Output("output",this,p.x(),p.y(),bits));

	} // end of init method

	/**
	 * The transform from canonical geometry (output RIGHT) to the current
	 * output orientation (issue #77: read by the GUI-side renderer).
	 *
	 * @return the transform for the current output orientation.
	 */
	public GridTransform.Chain placement() {

		int s = Geometry.SPACING;
		GridTransform.Chain t = GridTransform.chain(2*s,(numInputs+1)*s);
		switch (outputOrientation) {
		case RIGHT:
			break;
		case LEFT:
			t.mirrorX();
			break;
		case UP:
			t.rotateCCW();
			break;
		case DOWN:
			t.rotateCCW().mirrorY();
			break;
		}
		return t;
	} // end of placement method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes (inputs, bits, delay, orientations). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("inputs") {
			/**
			 * Read the number of data inputs from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the number of inputs.
			 */
			@Override
			protected int get(Element el) { return ((Mux)el).numInputs; }
			/**
			 * Set the number of data inputs on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param v The new number of inputs.
			 */
			@Override
			protected void set(Element el, int v) { ((Mux)el).numInputs = v; }
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((Mux)el).bits; }
			/**
			 * Set the bit width on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param v The new bit width.
			 */
			@Override
			protected void set(Element el, int v) { ((Mux)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((Mux)el).propDelay; }
			/**
			 * Set the propagation delay on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Mux)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("iOrient") {
			/**
			 * Read the output orientation from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the output orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Mux)el).outputOrientation;
			}
			/**
			 * Set the output orientation on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param o The new output orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Mux)el).outputOrientation = o;
			}
		},
		new Attribute.OrientationAttribute("sOrient") {
			/**
			 * Read the selector orientation from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the selector orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Mux)el).selectorOrientation;
			}
			/**
			 * Set the selector orientation on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param o The new selector orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Mux)el).selectorOrientation = o;
			}
		}
	);

	/** Base attributes followed by this element's own, in save order. */
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

		output.println("ELEMENT Mux");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 *
	 * @return a copy of this element.
	 */
	@Override
	public Element copy() {

		Mux it = new Mux(getCircuit());
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

		return numInputs + " input, " + bits + " bit multiplexor";
	} // end of showInfo method

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
	 * Tells if a mux is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		boolean success = true;
		for(Input i : inputs)
		{
			if(i.isAttached())
			{
				success = false;
				break;
			}
		}
		for(Output o : outputs)
		{
			if(o.isAttached())
			{
				success = false;
				break;
			}
		}
		return success;
	}

	/**
	 * This method will flip a mux's selector
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		selectorOrientation = selectorOrientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 *  This method will rotate the mux if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		if(direction == Orientation.LEFT)
		{
			selectorOrientation = selectorOrientation.ccw();
			outputOrientation = outputOrientation.ccw();
		}
		else if(direction == Orientation.RIGHT)
		{
			selectorOrientation = selectorOrientation.cw();
			outputOrientation = outputOrientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Tells if a mux is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		boolean success = true;
		for(Input i : inputs)
		{
			if(i.isAttached())
			{
				success = false;
				break;
			}
		}
		for(Output o : outputs)
		{
			if(o.isAttached())
			{
				success = false;
				break;
			}
		}
		return success;
	}


//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/**
	 * The value scheduled to reach the output, to suppress redundant events.
	 * Null before {@link #initSim(Simulator)} seeds it at simulation start.
	 */
	private @Nullable BitSet toBeValue;

	/**
	 * Initialize this element by setting its output and to-be value to 0.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		// set outputs to 0
		BitSet zero = new BitSet(1);
		outputs.get(0).setValue(zero);

		// set to-be value
		toBeValue = (BitSet)zero.clone();
	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo PinChanged if an input change, the new output value otherwise.
	 */
	@Override
	public void react(long now, Simulator sim, SimEvent.Payload todo) {

		switch (todo) {

		// if an input has changed ...
		case PinChanged _ -> {

			// get the selector input
			BitSet bw = inputs.get(0).getValue();
			if (bw == null)
				bw = new BitSet();
			int which = BitSetUtils.ToInt(bw);

			// get the selected input
			BitSet newValue;
			if (which >= numInputs) {
				newValue = new BitSet(1);
			}
			else {
				newValue = inputs.get(which+1).getValue();
				if (newValue == null)
					newValue = new BitSet();
			}

			// if new value is different from the value propagating through
			// the mux, then post an event
			if (!newValue.equals(toBeValue)) {
				toBeValue = (BitSet)newValue.clone();
				sim.post(new SimEvent(now+propDelay,this,new NewValue(newValue)));
			}
		}

		// the new output value arriving
		case NewValue(BitSet value) -> {

			// send to output
			Output sumOut = outputs.get(0);
			sumOut.propagate(value,now,sim);
		}

		case TriStateOff _, StateChanged _, MemoryRead _, MemoryWrite _,
				TableOutput _ ->
			throw new IllegalStateException("unexpected payload: " + todo);
		}

	} // end of react method

} // end of Mux class
