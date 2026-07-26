package jls.elem;

import jls.core.Geometry;
import jls.core.GridPoint;
import jls.core.GridSize;
import jls.core.Orientation;
import jls.*;
import jls.sim.*;
import java.io.PrintWriter;
import java.util.BitSet;
import org.jspecify.annotations.Nullable;

/**
 * Tri-state buffer(s).
 * 
 * @author David A. Poplawski
 */
public final class TriState extends LogicElement implements Timed {
	
	// defaults
	/** Default number of bits (gates). */
	private static final int defaultBits = 1;
	/** Default propagation delay. */
	private static final int defaultPropDelay = 5;

	// saved properties
	/** The number of bits (parallel tri-state gates). */
	private int bits = defaultBits;
	/** The propagation delay of this element. */
	private int propDelay = defaultPropDelay;
	/** The direction the output points. */
	private Orientation gateOrientation = Orientation.RIGHT;
	/** The side the control input is on. */
	private Orientation controlOrientation = Orientation.DOWN;
	/**
	 * Create a new Gate object.
	 * Subclass constructors do most of the work.
	 * 
	 * @param circuit The circuit this element is part of.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	public TriState(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Set the number of bits (issue #77: applied by the GUI-side dialog).
	 *
	 * @param bits The new number of bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the gate orientation (issue #77: applied by the GUI-side dialog).
	 *
	 * @param orientation The new gate orientation.
	 */
	public void setGateOrientation(Orientation orientation) {

		this.gateOrientation = orientation;
	} // end of setGateOrientation method

	/**
	 * Set the control orientation (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param orientation The new control orientation.
	 */
	public void setControlOrientation(Orientation orientation) {

		this.controlOrientation = orientation;
	} // end of setControlOrientation method

	/**
	 * Initialize internal info for this element.
	 * Sets up size, inputs and output.
	 *
	 * @param g Unused.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// canonical geometry (gate RIGHT, control DOWN), transformed to
		// the current orientation pair (#24)
		int s = Geometry.SPACING;
		GridTransform.Chain t = placement();
		GridSize d = t.size();
		width = d.width();
		height = d.height();
		GridPoint in = t.map(0, s);
		GridPoint ctl = t.map(2*s, 3*s);
		GridPoint outAt = t.map(4*s, s);
		inputs.add(new Input("input",this,in.x(),in.y(),bits));
		inputs.add(new Input("control",this,ctl.x(),ctl.y(),1));
		Output out = new Output("output",this,outAt.x(),outAt.y(),bits);
		outputs.add(out);
		out.setTriState(true);
	} // end of init method

	/**
	 * The transform from canonical geometry (gate RIGHT, control DOWN)
	 * to the current orientation pair.
	 *
	 * @return the transform for the current orientation pair.
	 */
	public GridTransform.Chain placement() {

		// the control must be perpendicular to the gate; loads with an
		// invalid pair have always failed, so keep rejecting them
		boolean gateHorizontal =
				gateOrientation == Orientation.LEFT
				|| gateOrientation == Orientation.RIGHT;
		boolean controlHorizontal =
				controlOrientation == Orientation.LEFT
				|| controlOrientation == Orientation.RIGHT;
		if (gateHorizontal == controlHorizontal) {
			throw new IllegalStateException(
					"invalid TriState orientation combination: gate "
					+ gateOrientation + ", control " + controlOrientation);
		}
		int s = Geometry.SPACING;
		GridTransform.Chain t = GridTransform.chain(4*s, 3*s);
		switch (gateOrientation) {
		case RIGHT:
			if (controlOrientation == Orientation.UP) {
				t.mirrorY();
			}
			break;
		case LEFT:
			if (controlOrientation == Orientation.UP) {
				t.rotate180();
			}
			else {
				t.mirrorX();
			}
			break;
		case UP:
			t.rotateCCW();
			if (controlOrientation == Orientation.LEFT) {
				t.mirrorX();
			}
			break;
		default: // DOWN
			if (controlOrientation == Orientation.LEFT) {
				t.rotateCW();
			}
			else {
				t.rotateCCW().mirrorY();
			}
			break;
		}
		return t;
	} // end of placement method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes (bits, delay, orientations). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit-width from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((TriState)el).bits; }
			/**
			 * Set the bit-width on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param v The new bit-width.
			 */
			@Override
			protected void set(Element el, int v) { ((TriState)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((TriState)el).propDelay; }
			/**
			 * Set the propagation delay on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((TriState)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("Gorient") {
			/**
			 * Read the gate orientation from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the gate orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((TriState)el).gateOrientation;
			}
			/**
			 * Set the gate orientation on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param o The new gate orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((TriState)el).gateOrientation = o;
			}
		},
		new Attribute.OrientationAttribute("Corient") {
			/**
			 * Read the control orientation from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the control orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((TriState)el).controlOrientation;
			}
			/**
			 * Set the control orientation on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param o The new control orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((TriState)el).controlOrientation = o;
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
	 * Save this element in a file.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT TriState");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 *
	 * @return a copy of this tri-state.
	 */
	@Override
	public Element copy() {

		TriState it = new TriState(getCircuit());
		it.inputs.add(inputs.get(0).copy(it));
		it.inputs.add(inputs.get(1).copy(it));
		it.outputs.add(outputs.get(0).copy(it));
		it.outputs.get(0).setTriState(true);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this and gate.
	 * 
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {
		
		if (bits == 1)
			return "tri-state gate";
		else
			return bits + " tri-state gates";
	} // end of showInfo method

	/**
	 * Tri-states have timing info (propagation delay).
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
	 * Tells if a tristate is capable of flipping, can only flip when inputs or outputs have no attachments.
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
	 * This method will flip a tristate's control input
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		controlOrientation = controlOrientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a tristate is capable of rotatating, can only rotate when inputs or outputs have no attachments.
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
	
	/**
	 *  This method will rotate the tristate if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		if(direction == Orientation.LEFT)
		{
			controlOrientation = controlOrientation.ccw();
			gateOrientation = gateOrientation.ccw();
		}
		else if(direction == Orientation.RIGHT)
		{
			controlOrientation = controlOrientation.cw();
			gateOrientation = gateOrientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	// the value scheduled to reach the output, null meaning off (HiZ);
	// used to suppress redundant output events (issue #98, S6)
	/** The value scheduled to reach the output, null meaning off (HiZ). */
	private @Nullable BitSet toBeValue;

	/**
	 * Initialize this element by setting its output pin to off (null).
	 *
	 * @param sim Unused.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	@Override
	public void initSim(Simulator sim) {

		// set output pin
		Output out = outputs.get(0);
		out.setValue(null);
		toBeValue = null;

	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	@Override
	public void react(long now, Simulator sim, @org.jspecify.annotations.Nullable Object todo) {
		
		// if the input has changed ...
		if (todo == null) {
			
			// get control input
			BitSet control = inputs.get(1).getValue();
			if (control ==  null)
				control = new BitSet();

			// if it is zero, turn off output
			// (but not if it is already off or turning off - #98, S6)
			if (!control.get(0)) {
				if (toBeValue == null)
					return;
				toBeValue = null;
				sim.post(new SimEvent(now+propDelay,this,"off"));
			}
			else {

				// get the data input and send it to the output
				// (but not if that value is already on the way - #98, S6)
				BitSet value = inputs.get(0).getValue();
				if (value == null)
					value = new BitSet();
				else
					value = (BitSet)value.clone();
				if (value.equals(toBeValue))
					return;
				toBeValue = (BitSet)value.clone();
				sim.post(new SimEvent(now+propDelay,this,value));
			}

		}
		
		// if gate is turning off, propagate null
		else if (todo instanceof String) {
			
			Output out = outputs.get(0);
			out.propagate(null,now,sim);
		}
		else {
			
			// get the new output value
			BitSet newValue = (BitSet)todo;
		
			// propagate value
			Output out = outputs.get(0);
			out.propagate(newValue,now,sim);
		}
		
	} // end of react method
	
} // end of TriState method
