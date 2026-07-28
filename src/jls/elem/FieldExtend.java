package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import jls.Circuit;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.sim.SimEvent;
import jls.sim.SimEvent.MemoryRead;
import jls.sim.SimEvent.MemoryWrite;
import jls.sim.SimEvent.NewValue;
import jls.sim.SimEvent.PinChanged;
import jls.sim.SimEvent.StateChanged;
import jls.sim.SimEvent.TableOutput;
import jls.sim.SimEvent.TriStateOff;
import jls.sim.Simulator;

/**
 * Sign- or zero-extend-from-field element (issue #201): a k-bit input is
 * widened to an n-bit output, filling the high {@code n-k} bits either
 * with the input's sign bit (sign extension) or with zeros (zero
 * extension), per a saved fill mode. Where {@link Extend} replicates a
 * single input bit to N outputs, this widens a whole field - exactly the
 * immediate-generator/ALU boilerplate (Splitter + Extend + Binder) a
 * datapath otherwise hand-wires.
 *
 * <p>Geometry, rotation and timing follow {@link Decoder}: one input on
 * one edge, one output on the opposite edge, oriented by where the input
 * sits.
 *
 * @author Josh Marshall
 */
public final class FieldExtend extends LogicElement
		implements Timed, Rotatable, Editable {

	/** The two fill modes: sign extension or zero extension. */
	private enum Fill {
		/** Fill the high output bits with the input's sign bit. */
		Sign,
		/** Fill the high output bits with zeros. */
		Zero};

	// default values
	/** Default number of input (field) bits. */
	private static final int defaultInBits = 1;
	/** Default number of output bits. */
	private static final int defaultOutBits = 2;
	/** Default fill mode (sign extension). */
	private static final Fill defaultFill = Fill.Sign;
	/** Default propagation delay. */
	private static final int defaultPropDelay = 10;

	// saved properties
	/** Number of input (field) bits. */
	private int inBits = defaultInBits;
	/** Number of output bits. */
	private int outBits = defaultOutBits;
	/** The high-bit fill mode. */
	private Fill fill = defaultFill;
	/** Propagation delay of this element. */
	private int propDelay = defaultPropDelay;
	/** Which side the input is on. */
	private Orientation orientation = Orientation.LEFT;

	/**
	 * Create a new field-extend element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public FieldExtend(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The direction this element faces (the side the input is on; read by
	 * the GUI-side renderer and dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the orientation (applied by the GUI-side dialog).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Get the number of input (field) bits.
	 *
	 * @return the number of input bits.
	 */
	public int getInBits() {

		return inBits;
	} // end of getInBits method

	/**
	 * Get the number of output bits.
	 *
	 * @return the number of output bits.
	 */
	public int getOutBits() {

		return outBits;
	} // end of getOutBits method

	/**
	 * Whether this element sign-extends (true) or zero-extends (false)
	 * (read by the GUI-side dialog).
	 *
	 * @return true for sign extension, false for zero extension.
	 */
	public boolean isSignExtend() {

		return fill == Fill.Sign;
	} // end of isSignExtend method

	/**
	 * Set the input/output widths and fill mode (applied by the GUI-side
	 * dialog).
	 *
	 * @param inBits The new input width.
	 * @param outBits The new output width.
	 * @param sign True for sign extension, false for zero extension.
	 */
	public void setValues(int inBits, int outBits, boolean sign) {

		this.inBits = inBits;
		this.outBits = outBits;
		this.fill = sign ? Fill.Sign : Fill.Zero;
	} // end of setValues method

	/**
	 * Initialize internal info for this element.
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

				if (orientation == Orientation.LEFT
						|| orientation == Orientation.RIGHT) {
					width = 4*s;
					height = 2*s;
				}
				else {
					width = 2*s;
					height = 4*s;
				}
			}
		}

		if (orientation == Orientation.LEFT) {
			inputs.add(new Input("input",this,0,s,inBits));
			outputs.add(new Output("output",this,width,s,outBits));
		}
		else if (orientation == Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,s,inBits));
			outputs.add(new Output("output",this,0,s,outBits));
		}
		else if (orientation == Orientation.DOWN) {
			inputs.add(new Input("input",this,s,height,inBits));
			outputs.add(new Output("output",this,s,0,outBits));
		}
		else if (orientation == Orientation.UP) {
			inputs.add(new Input("input",this,s,0,inBits));
			outputs.add(new Output("output",this,s,height,outBits));
		}
	} // end of init method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes: widths, fill, delay, orientation. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("inbits") {
			/**
			 * Read the input width from the given element.
			 */
			@Override
			protected int get(Element el) { return ((FieldExtend)el).inBits; }
			/**
			 * Store the input width into the given element.
			 */
			@Override
			protected void set(Element el, int v) { ((FieldExtend)el).inBits = v; }
		},
		new Attribute.IntAttribute("outbits") {
			/**
			 * Read the output width from the given element.
			 */
			@Override
			protected int get(Element el) { return ((FieldExtend)el).outBits; }
			/**
			 * Store the output width into the given element.
			 */
			@Override
			protected void set(Element el, int v) { ((FieldExtend)el).outBits = v; }
		},
		new Attribute.StringAttribute("mode") {
			/**
			 * Read the fill mode as its save-file name.
			 */
			@Override
			protected String get(Element el) {
				return ((FieldExtend)el).fill == Fill.Sign ? "sign" : "zero";
			}
			/**
			 * Load the fill mode from its save-file name; unknown strings
			 * leave it unchanged.
			 */
			@Override
			protected void set(Element el, String v) {
				if (v.equals("sign"))
					((FieldExtend)el).fill = Fill.Sign;
				else if (v.equals("zero"))
					((FieldExtend)el).fill = Fill.Zero;
			}
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from the given element.
			 */
			@Override
			protected int get(Element el) { return ((FieldExtend)el).propDelay; }
			/**
			 * Store the propagation delay into the given element.
			 */
			@Override
			protected void set(Element el, int v) { ((FieldExtend)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the orientation from the given element.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((FieldExtend)el).orientation;
			}
			/**
			 * Store the orientation into the given element.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((FieldExtend)el).orientation = o;
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

		output.println("ELEMENT FieldExtend");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		FieldExtend it = new FieldExtend(getCircuit());
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

		return inBits + " bits to " + outBits + " bits, "
				+ (fill == Fill.Sign ? "sign" : "zero") + " extend";
	} // end of showInfo method

	/**
	 * Get the number of output bits in this element.
	 *
	 * @return the number of output bits.
	 */
	@Override
	public int getBits() {

		return outBits;
	} // end of getBits method

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
	 * Tells if this element can rotate; only when nothing is attached.
	 *
	 * @return false if any put has a wire attached, true otherwise.
	 */
	@Override
	public boolean canRotate() {

		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	} // end of canRotate method

	/**
	 * Rotate this element if it is rotatable.
	 *
	 * @param direction The direction to rotate.
	 * @param g The current graphics context for recalculating size.
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

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
	 * Tells if this element can flip; only when nothing is attached.
	 *
	 * @return false if any put has a wire attached, true otherwise.
	 */
	@Override
	public boolean canFlip() {

		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	} // end of canFlip method

	/**
	 * Flip this element.
	 *
	 * @param g The current graphics context for recalculating size.
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/**
	 * The extended output value for the current input: the low bits copy
	 * the input field, and the high bits are the sign bit (sign mode) or
	 * zero (zero mode). A null (tri-state) input passes through as null.
	 *
	 * @return the extended value, or null if the input is tri-state.
	 */
	private @Nullable BitSet computeOutput() {

		BitSet value = inputs.get(0).getValue();
		if (value == null) {
			return null;
		}
		BitSet result = new BitSet(outBits);
		int copy = Math.min(inBits, outBits);
		for (int i = 0; i < copy; i += 1) {
			if (value.get(i)) {
				result.set(i);
			}
		}
		if (fill == Fill.Sign && inBits > 0 && inBits < outBits
				&& value.get(inBits - 1)) {
			result.set(inBits, outBits);
		}
		return result;
	} // end of computeOutput method

	/**
	 * Initialize this element by setting its output pin to 0.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		outputs.get(0).setValue(new BitSet(outBits));
	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo PinChanged when the input changes.
	 */
	@Override
	public void react(long now, Simulator sim, SimEvent.Payload todo) {

		switch (todo) {

		case PinChanged _ ->
			outputs.get(0).propagate(computeOutput(),now,sim);

		case NewValue _, TriStateOff _, StateChanged _, MemoryRead _,
				MemoryWrite _, TableOutput _ ->
			throw new IllegalStateException("unexpected payload: " + todo);
		}
	} // end of react method

} // end of FieldExtend class
