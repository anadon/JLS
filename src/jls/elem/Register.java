package jls.elem;

import java.io.PrintWriter;
import java.math.*;
import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Geometry;
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
 * n-bit register (level or edge triggered).
 *
 * @author David A. Poplawski
 */
public final class Register extends LogicElement
		implements Timed, Watchable, Rotatable, Editable {

	// register types
	/**
	 * The triggering modes a register can have: Latch (level triggered),
	 * PosFF (positive-edge triggered), and NegFF (negative-edge triggered).
	 */
	private enum Type {
		/** Transparent latch (level triggered). */
		Latch,
		/** Positive-edge triggered flip-flop. */
		PosFF,
		/** Negative-edge triggered flip-flop. */
		NegFF};

	// default values
	/** Default register name (empty until the user supplies one). */
	private static final String defaultName = "";
	/** Default trigger type (level-triggered latch). */
	private static final Register.Type defaultType = Type.Latch;
	/** Default number of bits. */
	private static final int defaultBits = 1;
	/** Default initial value (zero). */
	private static final BigInteger defaultInitValue = BigInteger.ZERO;
	/** Default radix for displaying/entering the initial value. */
	private static final int defaultBase = 10;
	/** Default propagation delay in simulation time units. */
	private static final int defaultPropDelay = 50;

	// saved properties
	/** The name of this register. */
	private String name = defaultName;
	/** The trigger type of this register (latch, pos-edge or neg-edge FF). */
	private Type type = defaultType;
	/** The number of bits in this register. */
	private int bits = defaultBits;
	/** The value this register holds when simulation starts. */
	private BigInteger initialValue = defaultInitValue;
	/** The radix used to display/enter the initial value in the edit dialog. */
	private int base = defaultBase;
	/** The propagation delay of this register. */
	private int propDelay = defaultPropDelay;
	/** True if this register's value is watched during simulation. */
	private boolean watched = false;
	/** The direction this register faces (position of D/C inputs vs Q outputs). */
	private Orientation orientation = Orientation.RIGHT;

	/**
	 * Create a new register element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public Register(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The direction this register faces (issue #77: read by the GUI-side
	 * renderer and dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the orientation (issue #77: applied by the GUI-side dialog).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Set the name (issue #77: applied by the GUI-side dialog after the
	 * circuit's name table has been updated).
	 *
	 * @param name The new name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * Set the number of bits (issue #77: applied by the GUI-side dialog).
	 *
	 * @param bits The new number of bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * The radix used to display/enter the initial value in the edit
	 * dialog (issue #77: read/written by the GUI-side dialog).
	 *
	 * @return the current display radix.
	 */
	public int getBase() {

		return base;
	} // end of getBase method

	/**
	 * Set the display radix (issue #77: applied by the GUI-side dialog).
	 *
	 * @param base The new display radix.
	 */
	public void setBase(int base) {

		this.base = base;
	} // end of setBase method

	/**
	 * Set the trigger type by its save-file name: "latch", "pff" or "nff"
	 * (issue #77: applied by the GUI-side dialog). Unknown strings leave
	 * the type unchanged, as the file loader does.
	 *
	 * @param v The type name.
	 */
	public void setTypeName(String v) {

		if (v.equals("latch"))
			type = Type.Latch;
		else if (v.equals("pff"))
			type = Type.PosFF;
		else if (v.equals("nff"))
			type = Type.NegFF;
	} // end of setTypeName method

	/**
	 * Apply a new initial value and reset the displayed current value to
	 * it (issue #77: applied by the GUI-side dialog, mirroring the file
	 * loader).
	 *
	 * @param value The new initial value.
	 */
	public void applyInitialValue(BigInteger value) {

		initialValue = value.add(BigInteger.ZERO);
		currentValue = BitSetUtils.Create(initialValue);
	} // end of applyInitialValue method

	/**
	 * Detach wires and recompute size after a modify grew the name past
	 * the box (issue #77: called by the GUI-side dialog in place of the
	 * former {@code change} resize block).
	 *
	 * @param g The Graphics object to use for sizing.
	 */
	public void resizeForNewName(jls.core.TextMetrics g) {

		detach();
		width = 0;
		height = 0;
		init(g);
	} // end of resizeForNewName method

	/**
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// set up size if there is a graphics object
		if (g != null) {

			if (width == 0 && height == 0) {
				int s = Geometry.SPACING;
				jls.core.TextMetrics fm = g;
				int w = fm.stringWidth(" " + name + " ");
				width = Math.max((w+s/2)/s*s,2*s)+2*s;
				if(width % (2*s) != 0)
				{
					width += s;
				}
				if(orientation == Orientation.LEFT || orientation == Orientation.RIGHT)
				{
					height = 5*s;
				}
				else
				{
					height = 6*s;
				}
			}

		}


		int s = Geometry.SPACING;
		if(orientation == Orientation.RIGHT)
		{
			// create inputs
			inputs.add(new Input("D",this,0,s,bits));
			inputs.add(new Input("C",this,0,4*s,1));

			// create outputs
			outputs.add(new Output("Q",this,width,s,bits));
			outputs.add(new Output("notQ",this,width,4*s,bits));
		}
		else if(orientation == Orientation.LEFT)
		{
			// create inputs
			inputs.add(new Input("D",this,width,s,bits));
			inputs.add(new Input("C",this,width,4*s,1));

			// create outputs
			outputs.add(new Output("Q",this,0,s,bits));
			outputs.add(new Output("notQ",this,0,4*s,bits));
		}
		else if(orientation == Orientation.UP)
		{
			// create inputs
			inputs.add(new Input("D",this,s,height,bits));
			inputs.add(new Input("C",this,width-s,height,1));

			// create outputs
			outputs.add(new Output("Q",this,s,0,bits));
			outputs.add(new Output("notQ",this,width-s,0,bits));
		}
		else if(orientation == Orientation.DOWN)
		{
			// create inputs
			inputs.add(new Input("D",this,s,0,bits));
			inputs.add(new Input("C",this,width-s,0,1));

			// create outputs
			outputs.add(new Output("Q",this,s,height,bits));
			outputs.add(new Output("notQ",this,width-s,height,bits));
		}
	} // end of init method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes (name, bits, init, orient, delay, type, watch). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the register's name for saving.
			 */
			@Override
			protected String get(Element el) { return ((Register)el).name; }
			/**
			 * Load the register's name and register it with the circuit.
			 */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((Register)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the register's name field without re-registering it.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((Register)to).name = ((Register)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the register's bit width for saving.
			 */
			@Override
			protected int get(Element el) { return ((Register)el).bits; }
			/**
			 * Load the register's bit width.
			 */
			@Override
			protected void set(Element el, int v) { ((Register)el).bits = v; }
		},
		new Attribute.BigIntAttribute("init") {
			/**
			 * Read the register's initial value for saving.
			 */
			@Override
			protected BigInteger get(Element el) {
				return ((Register)el).initialValue;
			}
			/**
			 * Load the initial value and reset the displayed current value.
			 */
			@Override
			protected void set(Element el, BigInteger v) {
				// the handwritten loader (and copy) also reset the
				// displayed current value
				Register reg = (Register)el;
				reg.initialValue = v.add(BigInteger.ZERO);
				reg.currentValue = BitSetUtils.Create(v);
			}
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the register's orientation for saving.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Register)el).orientation;
			}
			/**
			 * Load the register's orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Register)el).orientation = o;
			}
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the register's propagation delay for saving.
			 */
			@Override
			protected int get(Element el) { return ((Register)el).propDelay; }
			/**
			 * Load the register's propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Register)el).propDelay = v; }
		},
		new Attribute.StringAttribute("type") {
			/**
			 * Read the register's type as its save-file name for saving.
			 */
			@Override
			protected String get(Element el) {
				switch (((Register)el).type) {
				case PosFF: return "pff";
				case NegFF: return "nff";
				default: return "latch";
				}
			}
			/**
			 * Load the register's type from its save-file name.
			 */
			@Override
			protected void set(Element el, String v) {
				// unknown strings leave the type unchanged, as the
				// handwritten loader did
				if (v.equals("latch"))
					((Register)el).type = Type.Latch;
				else if (v.equals("pff"))
					((Register)el).type = Type.PosFF;
				else if (v.equals("nff"))
					((Register)el).type = Type.NegFF;
			}
		},
		new Attribute.IntAttribute("watch") {
			/**
			 * Read the register's watched flag (1/0) for saving.
			 */
			@Override
			protected int get(Element el) { return ((Register)el).watched ? 1 : 0; }
			/**
			 * Load the register's watched flag.
			 */
			@Override
			protected void set(Element el, int v) { ((Register)el).watched = v != 0; }
		}
	);

	/** Base element attributes followed by this element's own, in save order. */
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

		output.println("ELEMENT Register");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Can't copy registers ('cause they have names).
	 *
	 * @return false;
	 */
	public boolean canCopy() {

		return false;
	} // end of canCopy method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		Register it = new Register(getCircuit());
		it.base = base;	// display radix is not a saved attribute
		it.inputs.add(inputs.get(0).copy(it));
		it.inputs.add(inputs.get(1).copy(it));
		it.outputs.add(outputs.get(0).copy(it));
		it.outputs.add(outputs.get(1).copy(it));
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

		String ty = "";
		switch (type) {
		case Latch: ty = "latch"; break;
		case PosFF: ty = "positive edge triggered flip-flop"; break;
		case NegFF: ty = "negative edge triggered flip-flop"; break;
		}
		return bits + " bit " + ty + ", value = " +
				BitSetUtils.toDisplay(currentValue,bits);
	} // end of showInfo method

	/**
	 * Get the name of this register.
	 *
	 * @return the name.
	 */
	@Override
	public String getName() {

		return name;
	} // end of getName method

	/**
	 * Get the number of bits in this register.
	 *
	 * @return the number of bits.
	 */
	@Override
	public int getBits() {

		return bits;
	} // end of getBits method

	/**
	 * Remove name from list of element names in this circuit.
	 *
	 * @param circ A reference back to the circuit the element is in.
	 */
	@Override
	public void remove(Circuit circ) {

		circ.removeName(name);
		super.remove(circ);
	} // end of remove method

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
	 * Set the initial value of this register.
	 *
	 * @param value The initial value.
	 */
	public void setInitialValue(BigInteger value) {

		initialValue = value.add(BigInteger.ZERO);
	} // end of setInitialValue method

	/**
	 * Get the initial value of this register (for consumers of the
	 * circuit model, e.g. the HDL exporter, issue #60).
	 *
	 * @return the initial value.
	 */
	public BigInteger getInitialValue() {

		return initialValue;
	} // end of getInitialValue method

	/**
	 * The register type as its save-file name: "latch" (transparent
	 * latch), "pff" (positive-edge flip-flop) or "nff" (negative-edge
	 * flip-flop). The Type enum itself stays private; this mirrors the
	 * "type" attribute the file format already pins (issue #60).
	 *
	 * @return the type name.
	 */
	public String getTypeName() {

		switch (type) {
		case PosFF: return "pff";
		case NegFF: return "nff";
		default: return "latch";
		}
	} // end of getTypeName method

	/**
	 * Tells if a register is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate() {

		for(Input i : inputs) {
			if(i.isAttached()) {
				return false;
			}
		}
		for(Output o : outputs) {
			if(o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canRotate method

	/**
	 * This method will rotate the register if it is rotateable.
	 *
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		if(direction == Orientation.LEFT) {
			orientation = orientation.ccw();
		}
		else if(direction == Orientation.RIGHT) {
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of rotate method

	/**
	 * Tells if a register is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip() {

		for(Input i : inputs) {
			if(i.isAttached()) {
				return false;
			}
		}
		for(Output o : outputs) {
			if(o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canFlip method

	/**
	 * This method will flip a register
	 *
	 * @param g The current graphics context to facilitate recalculation of size when flipping
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

	/**
	 * See if this register is watched.
	 *
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {

		return watched;
	} // end of isWatched method

	/**
	 * Set whether this register is watched or not.
	 *
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {

		watched = state;
	} // end of setWatched method

	/**
	 * Print current value to stdout.
	 *
	 * @param prefix qualified name.
	 */
	public void printValue(String prefix) {

		if (prefix.isEmpty()) {
			System.out.printf("Register %s: %s\n", name, BitSetUtils.toDisplay(currentValue,bits));
		}
		else {
			System.out.printf("Register %s.%s: %s\n", prefix, name, BitSetUtils.toDisplay(currentValue,bits));
		}

	} // end of printValue method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/** The value scheduled to appear on the outputs after the propagation delay. */
	private @Nullable BitSet toBeValue;
	/** The value currently stored in this register. */
	private BitSet currentValue = new BitSet();
	/** The most recent value seen on the clock (C) input. */
	private int currentC;

	/**
	 * Get the current value stored in this register.
	 *
	 * @return the current value.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 */
	@Override
	public BitSet getCurrentValue() {

		return (BitSet)currentValue.clone();
	} // end of getCurrentValue method

	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 *
	 * @param sim Unused.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 */
	@Override
	public void initSim(Simulator sim) {

		// set current value and to-be value to the initial value
		// (assign the field, not a shadowing local - issue #98, S2)
		currentValue = BitSetUtils.Create(initialValue);
		toBeValue = (BitSet)currentValue.clone();
		currentC = 0;

		// set output pins to 0
		Output q = outputs.get(0);
		q.setValue(new BitSet(1));
		Output notq = outputs.get(1);
		notq.setValue(new BitSet(1));

		// post output event at time 0 to drive the initial value
		sim.post(new SimEvent(0,this,
				new NewValue((BitSet)currentValue.clone())));

	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo PinChanged if an input has changed, otherwise the value to output.
	 */
	@Override
	public void react(long now, Simulator sim, SimEvent.Payload todo) {

		switch (todo) {

		// if an input has changed ...
		case PinChanged _ -> {

			BitSet inVal = inputs.get(1).getValue();
			if (inVal == null)
				inVal = new BitSet();
			int c = (int)(BitSetUtils.ToLong(inVal));
			BitSet d = inputs.get(0).getValue();
			if (d == null)
				d = new BitSet();
			switch (type) {
			case Latch:
				if (c == 0)
					break;
				if (d.equals(toBeValue))
					break;
				toBeValue = (BitSet)d.clone();
				sim.post(new SimEvent(now+propDelay,this,
						new NewValue((BitSet)d.clone())));
				break;
			case PosFF:
				if (currentC == 1)
					break;
				if (c == 0)
					break;
				if (d.equals(toBeValue))
					break;
				toBeValue = (BitSet)d.clone();
				sim.post(new SimEvent(now+propDelay,this,
						new NewValue((BitSet)d.clone())));
				break;
			case NegFF:
				if (currentC == 0)
					break;
				if (c == 1)
					break;
				if (d.equals(toBeValue))
					break;
				toBeValue = (BitSet)d.clone();
				sim.post(new SimEvent(now+propDelay,this,
						new NewValue((BitSet)d.clone())));
				break;
			}
			currentC = c;
		}

		// the new output value arriving
		case NewValue(BitSet newQ) -> {

			// save for watch
			currentValue = (BitSet)newQ.clone();

			// make copy and complement
			BitSet qOut = (BitSet)newQ.clone();
			BitSet notQOut = (BitSet)newQ.clone();
			notQOut.flip(0,bits);

			// send to outputs
			Output q = outputs.get(0);
			q.propagate(qOut,now,sim);
			Output notq = outputs.get(1);
			notq.propagate(notQOut,now,sim);
		}

		case TriStateOff _, StateChanged _, MemoryRead _, MemoryWrite _,
				TableOutput _ ->
			throw new IllegalStateException("unexpected payload: " + todo);
		}

	} // end of react method

	/**
	 * Display current value.
	 *
	 * @param whereX the x-coordinate on screen (unused here).
	 * @param whereY the y-coordinate on screen (unused here).
	 */
	@Override
	public void showCurrentValue(int whereX, int whereY) {

		String hex = BitSetUtils.ToString(currentValue,16);
		String unsigned = BitSetUtils.ToString(currentValue,10);
		String signed = BitSetUtils.ToStringSigned(currentValue,bits);
		String value = "0x" +hex + " (" + unsigned + " unsigned, " + signed + " signed)";
		TellUser.info(null, value, "Information");
	} // end of showCurrentValue method

} // end of Register class
