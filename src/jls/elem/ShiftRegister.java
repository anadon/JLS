package jls.elem;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.*;
import jls.sim.*;
import java.io.PrintWriter;
import java.util.BitSet;
import org.jspecify.annotations.Nullable;

/**
 * Combinational barrel shifter (issue #122). The bsiever fork lineage
 * shipped this element as "ShiftRegister" in its 4.6 release, and that
 * save tag is kept here so fork-authored circuits load upstream; note
 * that despite the name it holds no state - it is a Mux-like
 * combinational element that shifts its data input by the value on its
 * amount input.
 *
 * Semantics (derived from the fork source at bsiever/JLS@038a5b67, per
 * issue #122 section 10): output = input shifted by amount, where the
 * shift is logical left (zero fill), logical right (zero fill), or
 * arithmetic right (sign fill), fixed per instance. The amount input is
 * ceil(log2(bits)) wide. New value propagates after the instance's
 * delay, with the standard redundant-event suppression (section 6.2 of
 * docs/simulation-semantics.md).
 *
 * GUI classes are referenced fully qualified, not imported: new files
 * in jls.elem may not import AWT/Swing (issue #77,
 * HeadlessCoreRatchetTest).
 *
 * @author David A. Poplawski
 */
public final class ShiftRegister extends LogicElement implements Timed {

	/** The shift-kind rule, shared by the dialog and the loader
	 *  (issue #52 pattern: one string, two surfaces). */
	static final String TYPE_CONSTRAINT =
			"Shift type must be LogicalLeft, LogicalRight or ArithmeticRight";

	/** The width rule, shared by the dialog and the loader. */
	static final String BITS_CONSTRAINT = "Must have at least 2 bits";

	/**
	 * The width rule: a 1-bit shifter has a 0-bit amount input.
	 *
	 * @param bits The proposed data width.
	 *
	 * @return the violated constraint message, or null if valid.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#shiftRegisterBitsRuleIsOneStringOnTwoSurfaces()
	 */
	public static @Nullable String checkBits(int bits) {

		return bits < 2 ? BITS_CONSTRAINT : null;
	} // end of checkBits method

	/**
	 * The data width a new instance starts with (issue #77: read by the
	 * GUI-side creation dialog to seed its field).
	 *
	 * @return the default number of data bits.
	 */
	public static int defaultBits() {

		return defaultBits;
	} // end of defaultBits method

	// shift kinds (save-file names are the enum names, fixed by the
	// fork's 4.6 file format)
	/**
	 * The three shift behaviors an instance can be fixed to: shift left
	 * with zero fill, shift right with zero fill, or shift right with
	 * sign fill (arithmetic). Each constant's name is also its save-file
	 * tag, so the names must not change.
	 */
	private enum Type {
		/** Shift toward the high bit, filling with zeros. */
		LogicalLeft,
		/** Shift toward the low bit, filling with zeros. */
		LogicalRight,
		/** Shift toward the low bit, filling with copies of the sign bit. */
		ArithmeticRight
	};

	// default values
	/** The shift kind a new instance starts with. */
	private static final Type defaultType = Type.LogicalLeft;
	/** The data width a new instance starts with. */
	private static final int defaultBits = 8;
	/** The propagation delay a new instance starts with. */
	private static final int defaultPropDelay = 25;

	// saved properties
	/** The shift kind of this instance. */
	private Type type = defaultType;
	/** The width of the data input and output, in bits. */
	private int bits = defaultBits;
	/** The propagation delay of this instance. */
	private int propDelay = defaultPropDelay;
	/** Which side of the element the output is on. */
	private Orientation outputOrientation = Orientation.RIGHT;
	/** Which side of the element the amount input is on. */
	private Orientation amountOrientation = Orientation.DOWN;

	/**
	 * Create a new shift register element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public ShiftRegister(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Set the data width (issue #77: applied by the GUI-side creation
	 * dialog).
	 *
	 * @param bits The number of data bits.
	 */
	public void setDataBits(int bits) {

		this.bits = bits;
	} // end of setDataBits method

	/**
	 * Fix this instance to shift logically left (zero fill) (issue #77:
	 * applied by the GUI-side creation dialog).
	 */
	public void setShiftLogicalLeft() {

		type = Type.LogicalLeft;
	} // end of setShiftLogicalLeft method

	/**
	 * Fix this instance to shift logically right (zero fill) (issue #77:
	 * applied by the GUI-side creation dialog).
	 */
	public void setShiftLogicalRight() {

		type = Type.LogicalRight;
	} // end of setShiftLogicalRight method

	/**
	 * Fix this instance to shift arithmetically right (sign fill) (issue
	 * #77: applied by the GUI-side creation dialog).
	 */
	public void setShiftArithmeticRight() {

		type = Type.ArithmeticRight;
	} // end of setShiftArithmeticRight method

	/**
	 * Set which side of the element the output is on (issue #77: applied
	 * by the GUI-side creation dialog).
	 *
	 * @param o The output orientation.
	 */
	public void setOutputOrientation(Orientation o) {

		outputOrientation = o;
	} // end of setOutputOrientation method

	/**
	 * Set which side of the element the amount input is on (issue #77:
	 * applied by the GUI-side creation dialog).
	 *
	 * @param o The amount-input orientation.
	 */
	public void setAmountOrientation(Orientation o) {

		amountOrientation = o;
	} // end of setAmountOrientation method

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// canonical geometry (output RIGHT), transformed to the current
		// output orientation (#24); the amount side is independent of
		// that transform, so its put is placed directly from its own
		// orientation - the same scheme as Mux, whose geometry this
		// element inherited in the fork
		int s = Geometry.SPACING;
		GridTransform.Chain t = placement();
		jls.core.GridSize d = t.size();
		width = d.width();
		height = d.height();

		// determine number of amount bits
		int sbits = 32 - Integer.numberOfLeadingZeros(bits-1);

		// create amount input
		if(amountOrientation == Orientation.DOWN)
		{
			inputs.add(new Input("amount",this,s,height,sbits));
		}
		else if(amountOrientation == Orientation.UP)
		{
			inputs.add(new Input("amount",this,s,0,sbits));
		}
		else if(amountOrientation == Orientation.LEFT)
		{
			inputs.add(new Input("amount",this,0,s,sbits));
		}
		else if(amountOrientation == Orientation.RIGHT)
		{
			inputs.add(new Input("amount",this,width,s,sbits));
		}

		// create data input and output
		jls.core.GridPoint in = t.map(0,s);
		inputs.add(new Input("input",this,in.x(),in.y(),bits));
		jls.core.GridPoint out = t.map(2*s,s);
		outputs.add(new Output("output",this,out.x(),out.y(),bits));

	} // end of init method

	/**
	 * The transform from canonical geometry (output RIGHT) to the current
	 * output orientation.
	 *
	 * @return the transform chain mapping canonical points to displayed points.
	 */
	private GridTransform.Chain placement() {

		int s = Geometry.SPACING;
		GridTransform.Chain t = GridTransform.chain(2*s,2*s);
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

	/**
	 * The output orientation of this shift register (issue #77: a read-only
	 * accessor the GUI-side {@code ShiftRegisterRenderer} uses so the "in"
	 * label placement can leave the model).
	 *
	 * @return the output orientation.
	 */
	public Orientation getOutputOrientation() {

		return outputOrientation;
	} // end of getOutputOrientation method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes. The names
	// and save order are the fork's, so 4.6-era fork files load
	// unchanged (issue #122 H2).
	/** This element's own persisted attributes, in the fork's save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("type") {
			/**
			 * Read the shift kind as its save-file name.
			 *
			 * @param el The shift register to read.
			 *
			 * @return the shift kind's enum name.
			 */
			@Override
			protected String get(Element el) {
				return ((ShiftRegister)el).type.name();
			}
			/**
			 * Set the shift kind from its save-file name.
			 *
			 * @param el The shift register to modify.
			 * @param v The shift kind's enum name.
			 *
			 * @throws IllegalArgumentException if the name is not a known kind.
			 */
			@Override
			protected void set(Element el, String v) {
				// an unknown kind is rejected, not guessed (issue #122
				// section 9: fail with the element named)
				for (Type t : Type.values()) {
					if (t.name().equals(v)) {
						((ShiftRegister)el).type = t;
						return;
					}
				}
				throw new IllegalArgumentException(TYPE_CONSTRAINT);
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the data width.
			 *
			 * @param el The shift register to read.
			 *
			 * @return the number of data bits.
			 */
			@Override
			protected int get(Element el) { return ((ShiftRegister)el).bits; }
			/**
			 * Validate and set the data width.
			 *
			 * @param el The shift register to modify.
			 * @param v The proposed number of data bits.
			 *
			 * @throws IllegalArgumentException if the width is below the minimum.
			 */
			@Override
			protected void set(Element el, int v) {
				String violated = checkBits(v);
				if (violated != null) {
					throw new IllegalArgumentException(violated);
				}
				((ShiftRegister)el).bits = v;
			}
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay.
			 *
			 * @param el The shift register to read.
			 *
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((ShiftRegister)el).propDelay; }
			/**
			 * Set the propagation delay.
			 *
			 * @param el The shift register to modify.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((ShiftRegister)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("iOrient") {
			/**
			 * Read the output orientation.
			 *
			 * @param el The shift register to read.
			 *
			 * @return the output orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((ShiftRegister)el).outputOrientation;
			}
			/**
			 * Set the output orientation.
			 *
			 * @param el The shift register to modify.
			 * @param o The new output orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((ShiftRegister)el).outputOrientation = o;
			}
		},
		new Attribute.OrientationAttribute("sOrient") {
			/**
			 * Read the amount-input orientation.
			 *
			 * @param el The shift register to read.
			 *
			 * @return the amount-input orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((ShiftRegister)el).amountOrientation;
			}
			/**
			 * Set the amount-input orientation.
			 *
			 * @param el The shift register to modify.
			 * @param o The new amount-input orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((ShiftRegister)el).amountOrientation = o;
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

		output.println("ELEMENT ShiftRegister");
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

		ShiftRegister it = new ShiftRegister(getCircuit());
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

		String kind = "";
		switch (type) {
		case LogicalLeft: kind = "logical left"; break;
		case LogicalRight: kind = "logical right"; break;
		default: kind = "arithmetic right"; break;
		}
		return bits + " bit " + kind + " shift register";
	} // end of showInfo method

	/**
	 * Shift registers have timing info (propagation delay).
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
	 * Tells if a shift register is capable of flipping, can only flip
	 * when inputs or outputs have no attachments.
	 *
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip() {

		for (Input i : inputs) {
			if (i.isAttached()) {
				return false;
			}
		}
		for (Output o : outputs) {
			if (o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canFlip method

	/**
	 * This method will flip a shift register's amount input side.
	 *
	 * @param g The current graphics context to facilitate recalculation
	 *          of size when flipping.
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		amountOrientation = amountOrientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	/**
	 * This method will rotate the shift register if it is rotateable.
	 *
	 * @param direction The direction to rotate.
	 * @param g The current graphics context for use in recalculating size.
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		if (direction == Orientation.LEFT) {
			amountOrientation = amountOrientation.ccw();
			outputOrientation = outputOrientation.ccw();
		}
		else if (direction == Orientation.RIGHT) {
			amountOrientation = amountOrientation.cw();
			outputOrientation = outputOrientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of rotate method

	/**
	 * Tells if a shift register is capable of rotating, can only rotate
	 * when inputs or outputs have no attachments.
	 *
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate() {

		for (Input i : inputs) {
			if (i.isAttached()) {
				return false;
			}
		}
		for (Output o : outputs) {
			if (o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canRotate method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/** The value currently propagating toward the output. */
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
	 * @param todo Null if an input change, the new output value otherwise.
	 */
	@Override
	public void react(long now, Simulator sim, @org.jspecify.annotations.Nullable Object todo) {

		// if an input has changed ...
		if (todo == null) {

			// get the amount to shift
			BitSet bw = inputs.get(0).getValue();
			if (bw == null)
				bw = new BitSet();
			int amount = BitSetUtils.ToInt(bw);

			// get the data input
			BitSet input = inputs.get(1).getValue();
			if (input == null)
				input = new BitSet();

			// shift
			BitSet newValue = new BitSet(bits);
			switch (type) {
			case LogicalLeft:
				for (int i = bits-1; i >= 0; i -= 1) {
					if (i-amount >= 0) {
						newValue.set(i,input.get(i-amount));
					}
				}
				break;
			case LogicalRight:
				for (int i = 0; i < bits; i += 1) {
					if (i+amount < bits) {
						newValue.set(i,input.get(i+amount));
					}
				}
				break;
			default: // ArithmeticRight
				for (int i = 0; i < bits; i += 1) {
					if (i+amount < bits) {
						newValue.set(i,input.get(i+amount));
					} else {
						// copy sign bit
						newValue.set(i,input.get(bits-1));
					}
				}
				break;
			}

			// if new value is different from the value propagating
			// through the shifter, then post an event
			if (!newValue.equals(toBeValue)) {
				toBeValue = (BitSet)newValue.clone();
				sim.post(new SimEvent(now+propDelay,this,newValue));
			}
		}
		else {

			// get the new output value
			BitSet value = (BitSet)todo;

			// send to output
			Output out = outputs.get(0);
			out.propagate(value,now,sim);
		}

	} // end of react method

} // end of ShiftRegister class
