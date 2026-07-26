package jls.elem;

import java.io.*;
import java.util.*;

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
 * Receiving end of a named wire.
 *
 * @author David A. Poplawski
 */
public final class JumpEnd extends LogicElement implements Rotatable {

	// default value
	/** Default number of bits in the named wire. */
	private static final int defaultBits = 1;

	/**
	 * Message shown when the END gesture is invoked with no named wires
	 * to connect to (#131).
	 */
	public static final String NO_NAMED_WIRES =
			"No named wires exist. Name a wire with START first.";

	// saved properties
	/** Number of bits in the named wire. */
	private int bits = defaultBits;
	/**
	 * Name of the named wire this end connects to, or null until set by a
	 * load or by {@link #setName} after construction.
	 */
	private @Nullable String name;

	// running properties
	/** True if the saved file marked the output tri-state. */
	private boolean loadTriState = false;

	/** Which way the element points. */
	private Orientation orientation = Orientation.RIGHT;

	/**
	 * Create a new wire jump end.
	 *
	 * @param circuit The circuit this element is part of.
	 *
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#endGestureFailsFastWhenNoNamedWiresExist()
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#endGestureStillReachesDialogWithANamedWire()
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#matchGesturePresetNameBypassesGuardAndDialog()
	 */
	public JumpEnd(Circuit circuit) {

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
	 * Set the orientation (issue #77: applied by the GUI-side dialog).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * Get the orientation of this jump end (issue #77: read by the
	 * GUI-side renderer and dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		if (g != null) {

			if (width == 0 && height == 0) {

				// set up size
				int s = Geometry.SPACING;
				jls.core.TextMetrics fm = g;
				int w = fm.stringWidth(" " + name + " ")+s;
				width = Math.max((w+s/2)/s*s,2*s);	// ceiling in spacings
				height = 0;	// not really, but bounding rectangle will be large enough
			}

		}

		// create output
		Output out;
		if(orientation == Orientation.RIGHT)
			out = new Output("output",this,width,0,bits);
		else
			out = new Output("output",this,0,0,bits);
		outputs.add(out);
		if (loadTriState) {
			out.loadSetTriState();
		}

	} // end of init method

	/**
	 * Get the rectangle bounding this element.
	 *
	 * @return the rectangle bounding this element.
	 */
	@Override
	public jls.core.Bounds getRect() {

		return new jls.core.Bounds(x,y-Geometry.SPACING/2,width,height+Geometry.SPACING);
	} // end of getRect method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes. The
	// " int tristate 1" line reflects derived output state, not a plain
	// field, so it stays hand-printed in save() and hand-loaded in
	// setValue below.
	/** This element's own saved attributes, in save order (#23). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the wire name to be written out for this element.
			 *
			 * @param el The JumpEnd being saved.
			 * @return the wire name.
			 */
			@Override
			protected String get(Element el) {
				String n = ((JumpEnd)el).name;
				if (n == null)
					throw new IllegalStateException("jump end has no name to save");
				return n;
			}
			/**
			 * Restore the wire name during a load, registering it with the
			 * circuit so later lookups resolve.
			 *
			 * @param el The JumpEnd being loaded.
			 * @param v The wire name read from the file.
			 */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((JumpEnd)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the wire name from one element to another without
			 * re-registering it with the circuit.
			 *
			 * @param from The source JumpEnd.
			 * @param to The destination JumpEnd.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((JumpEnd)to).name = ((JumpEnd)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width to be written out for this element.
			 *
			 * @param el The JumpEnd being saved.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((JumpEnd)el).bits; }
			/**
			 * Restore the bit width during a load.
			 *
			 * @param el The JumpEnd being loaded.
			 * @param v The number of bits read from the file.
			 */
			@Override
			protected void set(Element el, int v) { ((JumpEnd)el).bits = v; }
		},
		new Attribute.OrientationAttribute("orientation") {
			/**
			 * Read the orientation to be written out for this element.
			 *
			 * @param el The JumpEnd being saved.
			 * @return the element's orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((JumpEnd)el).orientation;
			}
			/**
			 * Restore the orientation during a load.
			 *
			 * @param el The JumpEnd being loaded.
			 * @param o The orientation read from the file.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((JumpEnd)el).orientation = o;
			}
		}
	);

	/** Base attributes followed by this element's own, in save order (#23). */
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
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT JumpEnd");
		super.save(output);
		if (outputs.get(0).isTriState())
			output.println(" int tristate 1");
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		JumpEnd it = new JumpEnd(getCircuit());
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Get the name of this jump end.
	 *
	 * @return the name.
	 *
	 * @jls.testedby jls.ui.CircuitAssert#jumpAlias()
	 */
	@Override
	public @Nullable String getName() {

		return name;
	} // end of getName method

	/**
	 * Display info about this element.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		return bits + " bit wire connection, value = " +
				BitSetUtils.toDisplay(currentValue,bits);
	} // end of showInfo method

	/**
	 * Set this element to tri-state or not.
	 *
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {

		for (Output out : outputs) {
			out.setTriState(which);
		}
	} // end of setTriState method

	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(outputs.get(0).isAttached());
	}

	/**
	 * This method will flip an adder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		if(orientation == Orientation.LEFT)
		{
			orientation = Orientation.RIGHT;
		}
		else if(orientation == Orientation.RIGHT)
		{
			orientation = Orientation.LEFT;
		}

		outputs.clear();

		// create output
		Output out;
		if(orientation == Orientation.RIGHT)
			out = new Output("output",this,width,0,bits);
		else
			out = new Output("output",this,0,0,bits);
		outputs.add(out);
		if (loadTriState) {
			out.loadSetTriState();
		}
	}

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/** The value currently on the output, or null when tri-stated off. */
	private @Nullable BitSet currentValue = new BitSet();

	/**
	 * Initialize this element by setting its output to 0.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		// set output to 0 or off
		Output out = outputs.get(0);
		if (out.isTriState()) {
			currentValue = null;
			out.setValue(null);
		}
		else {
			currentValue = new BitSet();
			BitSet bitval = new BitSet(1);
			out.setValue(bitval);
		}

	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo The value to send along (TriStateOff for the undriven
	 *             value).
	 */
	@Override
	public void react(long now, Simulator sim, SimEvent.Payload todo) {

		// get the input value and send it to the output
		Output out = outputs.get(0);
		switch (todo) {

		case TriStateOff _ -> {
			currentValue = null;
			out.propagate(null,now,sim);
		}

		case NewValue(BitSet value) -> {
			currentValue = (BitSet)value.clone();
			out.propagate((BitSet)value.clone(),now,sim);
		}

		case PinChanged _, StateChanged _, MemoryRead _, MemoryWrite _,
				TableOutput _ ->
			throw new IllegalStateException("unexpected payload: " + todo);
		}

	} // end of react method

	/**
	 * Set the name of the named wire this end connects to, bypassing the
	 * selection dialog (used by the editor's match gesture).
	 *
	 * @param newName The wire name to attach to.
	 *
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#matchGesturePresetNameBypassesGuardAndDialog()
	 */
	public void setName(String newName) {
		name = newName;
	}

} // end of JumpEnd method
