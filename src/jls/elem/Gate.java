package jls.elem;

import java.io.*;
import java.util.BitSet;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.sim.*;

/**
 * Superclass of all simple gates (AND, OR, etc).
 * Contains common info and method.
 *
 * @author David A. Poplawski
 */
public abstract sealed class Gate extends LogicElement
		permits AndGate, DelayGate, Extend, NandGate, NorGate, NotGate,
		OrGate, XorGate {

	// named constants
	// gates share the one Orientation enum (issue #78 H3); only
	// their persistence differs - lowercase names, kept byte-identical
	// by the "orientation" attribute below
	/** Default number of inputs. */
	protected static final int defaultInputs = 2;
	/** Default number of gates (bits). */
	protected static final int defaultBits = 1;
	/** Default orientation. */
	protected static final Orientation defaultOrientation = Orientation.RIGHT;

	// saved properties
	/** Number of inputs to this gate. */
	protected int numInputs = defaultInputs;
	/** Number of gates (bits). */
	protected int bits = defaultBits;
	/** Which way this gate faces. */
	protected Orientation orientation = defaultOrientation;
	/** Propagation delay of this gate. */
	protected int propDelay;

	/**
	 * Create a new Gate object.
	 * Subclass constructors do most of the work.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public Gate(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Identity and shared "remember previous settings" state for one gate
	 * kind (#22). Each subclass holds a single static Kind, replacing the
	 * per-class copies of the display name, save name, default delay, and
	 * previous-settings statics. What remains in a gate subclass is its
	 * genuine content: the outline shape and computeOutput().
	 */
	protected static final class Kind {

		/** The human-readable name of this gate kind. */
		private final String displayName;	// e.g. "AND"
		/** The save-file tag of this gate kind. */
		private final String saveName;		// e.g. "AndGate" (a frozen tag:
											// must match SaveTags / spec §7)
		/** The forced input count, or -1 if the user chooses. */
		private final int fixedInputs;		// forced input count, or -1 if the
											// user chooses
		/** The default propagation delay for this gate kind. */
		private final int defaultDelay;
		/** Input count of the previously created gate of this kind. */
		private int previousInputs = defaultInputs;
		/** Bits (gate count) of the previously created gate of this kind. */
		private int previousBits = defaultBits;
		/** Orientation of the previously created gate of this kind. */
		private Orientation previousOrientation = defaultOrientation;

		/**
		 * Create a kind descriptor for one gate class.
		 *
		 * @param displayName The human-readable name (e.g. "AND").
		 * @param saveName The save-file tag (frozen; must match the
		 *                 SaveTags table and spec §7).
		 * @param fixedInputs The forced input count, or -1 if the user chooses.
		 * @param defaultDelay The default propagation delay for this kind.
		 */
		protected Kind(String displayName, String saveName, int fixedInputs,
				int defaultDelay) {

			this.displayName = displayName;
			this.saveName = saveName;
			this.fixedInputs = fixedInputs;
			this.defaultDelay = defaultDelay;
		} // end of constructor

	} // end of Kind class

	/**
	 * The kind descriptor for this gate.
	 *
	 * @return the (static, per-class) kind.
	 */
	protected abstract Kind kind();

	/**
	 * Remember the accepted settings for the next gate of the same kind.
	 * Called by the GUI-side creation dialog ({@code jls.edit.GateDialog})
	 * after the user accepts, so the "Repeat Previous" button and
	 * {@link #setToPrevious()} reproduce them. A gate with a fixed input
	 * count keeps its previous count unchanged.
	 */
	public void rememberPrevious() {

		Kind k = kind();
		if (k.fixedInputs < 0) {
			k.previousInputs = numInputs;
		}
		k.previousBits = bits;
		k.previousOrientation = orientation;
	} // end of rememberPrevious method

	/**
	 * Save this element in a file, under its kind's save name.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		save(output,kind().saveName,false);
	} // end of save method

	/**
	 * Display info about this gate.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		String type = kind().displayName;
		if (bits == 1)
			return numInputs + "-input " + type + " gate";
		else
			return bits + " " + numInputs + "-input " + type + " gate";
	} // end of showInfo method

	/**
	 * Initialize internal info for this element.
	 * Sets up size, inputs and outputs.
	 *
	 * @param g Unused.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// set up size
		if (orientation == Orientation.LEFT || orientation == Orientation.RIGHT) {
			width = Geometry.SPACING*4;
			height = Geometry.SPACING*(Math.max(numInputs,3)-1);
		}
		else { // up or down
			width = Geometry.SPACING*(Math.max(numInputs,3)-1);
			height = Geometry.SPACING*4;
		}

		Output out;

		if (orientation == Orientation.LEFT || orientation == Orientation.RIGHT) {

			int inx = 0;
			int outx = Geometry.SPACING*4;
			if (orientation == Orientation.LEFT) {
				inx = Geometry.SPACING*4;
				outx = 0;
			}

			// set up output
			int dist = (Math.max(numInputs,4)-3)/2*Geometry.SPACING;
			out = new Output("output",this,outx,dist+Geometry.SPACING,bits);
			outputs.add(out);

			// set up inputs
			if (numInputs == 1) { // not or delay gate
				inputs.add(new Input("input0",this,inx,Geometry.SPACING,bits));
			}
			else {
				int yc = 0;
				for (int i=0; i<numInputs; i+=1) {
					inputs.add(new Input("input"+i,this,inx,yc,bits));
					if (numInputs == 2)
						yc += 2*Geometry.SPACING;
					else
						yc += Geometry.SPACING;
				}
			}

		}
		else { // up or down
			int iny = 0;
			int outy = Geometry.SPACING*4;
			if (orientation == Orientation.UP) {
				iny = Geometry.SPACING*4;
				outy = 0;
			}

			// set up output
			int dist = (Math.max(numInputs,4)-3)/2*Geometry.SPACING;
			out = new Output("output",this,dist+Geometry.SPACING,outy,bits);
			outputs.add(out);

			// set up inputs
			if (numInputs == 1) { // not or delay gate
				inputs.add(new Input("input0",this,Geometry.SPACING,iny,bits));
			}
			else {
				int xc = 0;
				for (int i=0; i<numInputs; i+=1) {
					inputs.add(new Input("input"+i,this,xc,iny,bits));
					if (numInputs == 2)
						xc += 2*Geometry.SPACING;
					else
						xc += Geometry.SPACING;
				}
			}
		}
	} // end of init method

	/**
	 * The headless outline geometry of this gate's body symbol (issue #77
	 * model/render split). Each gate leaf describes its symbol as data by
	 * returning a {@link GateOutline} here; the GUI-side
	 * {@code jls.edit.GateRenderer} builds the general path from it each
	 * paint, so the gate model holds no AWT.
	 *
	 * @return the outline model for this gate's body symbol.
	 */
	public abstract GateOutline outline();

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for the attributes shared by every gate kind.
	/** The persistence attributes shared by every gate kind. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bits (gate width) attribute from a gate.
			 */
			@Override
			protected int get(Element el) { return ((Gate)el).bits; }
			/**
			 * Write the bits (gate width) attribute into a gate.
			 */
			@Override
			protected void set(Element el, int v) { ((Gate)el).bits = v; }
		},
		new Attribute.IntAttribute("numInputs") {
			/**
			 * Read the input-count attribute from a gate.
			 */
			@Override
			protected int get(Element el) { return ((Gate)el).numInputs; }
			/**
			 * Write the input-count attribute into a gate.
			 */
			@Override
			protected void set(Element el, int v) { ((Gate)el).numInputs = v; }
		},
		new Attribute.StringAttribute("orientation") {
			/**
			 * Read the orientation attribute from a gate as the
			 * lowercase enum name gates have always saved.
			 */
			@Override
			protected String get(Element el) {
				return ((Gate)el).orientation.name()
						.toLowerCase(Locale.ROOT);
			}
			/**
			 * Set a gate's orientation from a saved enum name, leaving it
			 * unchanged if the name is not recognized.
			 */
			@Override
			protected void set(Element el, String v) {
				// gates historically persist lowercase names ("up"),
				// unlike the shared "orient" attribute's uppercase;
				// unknown strings leave the orientation unchanged
				for (Orientation o : Orientation.values()) {
					if (o.name().toLowerCase(Locale.ROOT).equals(v)) {
						((Gate)el).orientation = o;
					}
				}
			}
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation-delay attribute from a gate.
			 */
			@Override
			protected int get(Element el) { return ((Gate)el).propDelay; }
			/**
			 * Write the propagation-delay attribute into a gate.
			 */
			@Override
			protected void set(Element el, int v) { ((Gate)el).propDelay = v; }
		}
	);

	/** Base attributes plus the shared gate attributes, in save order. */
	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus the shared gate attributes, in save order
	 * (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Copy this gate.
	 *
	 * @return A copy of this gate.
	 */
	@Override
	public Element copy() {

		try {
			Gate it = getClass().getConstructor(Circuit.class).newInstance(circuit);
			copy(it);
			return it;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("gate copy failed", ex);
		}
	} // end of copy method

	/**
	 * Copy info in this element to another element.
	 *
	 * @param el The element to copy to.
	 */
	@Override
	public void copy(Element el) {

		Gate it = (Gate)el;
		it.outputs.add(outputs.get(0).copy(it));
		for (Input in : inputs) {
			it.inputs.add(in.copy(it));
		}
		super.copy(el);
		return;
	} // end of copy method

	/**
	 * Gate sizes are a pure function of input count and orientation,
	 * recomputed by init() on every load, so they are not saved (#21).
	 */
	@Override
	protected boolean sizeIsRecomputedOnLoad() {

		return true;
	} // end of sizeIsRecomputedOnLoad method

	/**
	 * Save this element in a file.
	 *
	 * @param output The output writer.
	 * @param type The type of gate (e.g., "AND"), all capitals.
	 * @param triState If true, save that this element has a tri-state output.
	 */
	public void save(PrintWriter output, String type, boolean triState) {

		output.println("ELEMENT " + type);
		super.save(output);
		if (triState) {
			output.println(" int tristate 1");
		}
		output.println("END");
	} // end of save method

	/**
	 * The display name of this gate's kind (e.g. "AND"). Used by the
	 * GUI-side creation dialog ({@code jls.edit.GateDialog}) to lay out
	 * the form and title.
	 *
	 * @return the kind's display name.
	 */
	public String getKindName() {

		return kind().displayName;
	} // end of getKindName method

	/**
	 * Get the number of inputs to this gate. Used by the GUI-side renderer
	 * and creation dialog after the model went headless.
	 *
	 * @return the number of inputs.
	 */
	public int getNumInputs() {

		return numInputs;
	} // end of getNumInputs method

	/**
	 * Get the number of gates (bits). Used by the GUI-side creation dialog.
	 *
	 * @return the number of gates (bits).
	 */
	@Override
	public int getBits() {

		return bits;
	} // end of getBits method

	/**
	 * Get the orientation this gate faces. Used by the GUI-side renderer
	 * and creation dialog.
	 *
	 * @return the orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the number of inputs. Used by the GUI-side creation dialog.
	 *
	 * @param numInputs The number of inputs.
	 */
	public void setNumInputs(int numInputs) {

		this.numInputs = numInputs;
	} // end of setNumInputs method

	/**
	 * Set the number of gates (bits). Used by the GUI-side creation dialog.
	 *
	 * @param bits The number of gates (bits).
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the orientation this gate faces. Used by the GUI-side creation
	 * dialog.
	 *
	 * @param orientation The orientation this gate faces.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Get the rectangle bounding this element.
	 * For gates this will be 1/2 space higher and lower (for left/right gates)
	 * or 1/2 space wider on each side (for up/down gates).
	 *
	 * @return a rectangle that bounds the element on the screen.
	 * @jls.testedby jls.ui.EditorGestureTest#rubberBandSelectHighlightsEnclosedElements()
	 */
	@Override
	public jls.core.Bounds getRect() {

		if (orientation == Orientation.LEFT || orientation == Orientation.RIGHT) {
			return new jls.core.Bounds(x,y-Geometry.SPACING/2,width,height+Geometry.SPACING);
		}
		else {
			return new jls.core.Bounds(x-Geometry.SPACING/2,y,width+Geometry.SPACING,height);
		}
	} // end of getRect method

	/**
	 * Set characteristics to the values of the previously created gate of
	 * this kind. Gates with a fixed input count keep their current count,
	 * matching the historical per-gate behavior.
	 */
	public void setToPrevious() {

		Kind k = kind();
		if (k.fixedInputs < 0) {
			numInputs = k.previousInputs;
		}
		bits = k.previousBits;
		orientation = k.previousOrientation;
	} // end of setToPrevious method

	/**
	 * Get default propagation delay for this gate kind.
	 *
	 * @return the default propagation delay.
	 */
	public int getDefaultDelay() {

		return kind().defaultDelay;
	} // end of getDefaultDelay method

	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {

		propDelay = getDefaultDelay();
	} // end of resetPropDelay method

	/**
	 * Gates have timing info (propagation delay).
	 *
	 * @return true.
	 */
	@Override
	public boolean hasTiming() {

		return true;
	} // end of hasTiming method

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
	 *  This method will rotate the gate if it is rotate-able.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		// one shared enum (issue #78 H3), so the quarter-turn is its
		// ccw()/cw() instead of a hand-rolled transition table
		if (direction == Orientation.LEFT) {
			orientation = orientation.ccw();
		}
		else {
			orientation = orientation.cw();
		}
		width = 0;
		height = 0;
		inputs.clear();
		outputs.clear();
		init(g);
	}

	/**
	 * Tells if a gate is capable of rotating, can only rotate when inputs or outputs have no attachments.
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
	 * Tells if a gate is capable of flipping, can only flip when inputs or outputs have no attachments.
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
	 * This method will flip a gate
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/** The output value currently propagating through this gate. */
	private @Nullable BitSet toBeValue;

	/**
	 * Compute this gate's output value from its current input values,
	 * with absent (null) values counting as zero. This is the only
	 * simulation behavior that differs between the gate kinds (#22);
	 * the event handling below is shared.
	 *
	 * @return the output value.
	 */
	protected abstract BitSet computeOutput();

	/**
	 * Initialize this element: the output pin starts at 0, and a gate
	 * whose output for all-zero inputs is not 0 (NAND, NOR, NOT) posts
	 * an event to drive that value at time 0.
	 *
	 * @param sim The simulator to post events to.
	 */
	@Override
	public void initSim(Simulator sim) {

		// set output pin
		Output out = outputs.get(0);
		out.setValue(new BitSet(1));

		// drive the all-zero-inputs output value
		BitSet initial = computeOutput();
		if (!initial.isEmpty()) {
			sim.post(new SimEvent(0,this,initial));
		}
		toBeValue = (BitSet)initial.clone();
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

		// if the input has changed ...
		if (todo == null) {

			BitSet value = computeOutput();

			// if new value is different from the value propagating through
			// this gate, then post an event
			if (!value.equals(toBeValue)) {
				toBeValue = (BitSet)value.clone();
				sim.post(new SimEvent(now+propDelay,this,value));
			}
		}
		else {

			// send the new output value to the output
			BitSet newValue = (BitSet)todo;
			outputs.get(0).propagate(newValue,now,sim);
		}
	} // end of react method

} // end of Gate class
