package jls.elem;

import java.io.PrintWriter;
import java.util.*;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.sim.*;

/**
 * Starting point of a named wire.
 *
 * @author David A. Poplawski
 */
public final class JumpStart extends LogicElement
		implements TriProp, Watchable {

	// default value
	/** The default bit width for a new jump start. */
	private static final int defaultBits = 1;

	// saved properties
	/**
	 * The wire name (shared with the matching jump ends), or null until set
	 * by a load or by {@link #setName} after construction.
	 */
	private @Nullable String name;
	/** The bit width of the wire. */
	private int bits = defaultBits;
	/** True if this jump start's value is watched during simulation. */
	private boolean watched = false;

	/** Which direction the element faces. */
	private Orientation orientation = Orientation.LEFT;

	/**
	 * Create a new adder element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public JumpStart(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Set the wire name (issue #77: applied by the GUI-side dialog).
	 *
	 * @param name The new wire name.
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

		// create input
		if(orientation == Orientation.LEFT) {
			inputs.add(new Input("input",this,0,0,bits));
		}
		else if(orientation == Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,0,bits));
		}

		// save name in jumpstart list in this circuit
		if (name == null)
			throw new IllegalStateException("jump start name not set yet");
		getCircuit().addJumpStart(name,this);

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
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the name of the given jump start for saving.
			 *
			 * @param el The jump start being saved.
			 * @return the jump start's name.
			 */
			@Override
			protected String get(Element el) {
				String n = ((JumpStart)el).name;
				if (n == null)
					throw new IllegalStateException("jump start has no name to save");
				return n;
			}
			/**
			 * Store a loaded name into the given jump start and register
			 * it with the circuit.
			 *
			 * @param el The jump start being loaded.
			 * @param v The name read from the file.
			 */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((JumpStart)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the name field from one jump start to another without
			 * registering the name with the circuit.
			 *
			 * @param from The jump start to copy from.
			 * @param to The jump start to copy into.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((JumpStart)to).name = ((JumpStart)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width of the given jump start for saving.
			 *
			 * @param el The jump start being saved.
			 * @return the jump start's bit width.
			 */
			@Override
			protected int get(Element el) { return ((JumpStart)el).bits; }
			/**
			 * Store a loaded bit width into the given jump start.
			 *
			 * @param el The jump start being loaded.
			 * @param v The bit width read from the file.
			 */
			@Override
			protected void set(Element el, int v) { ((JumpStart)el).bits = v; }
		},
		new Attribute.IntAttribute("watch") {
			/**
			 * Read the watched flag of the given jump start as 1 or 0 for
			 * saving.
			 *
			 * @param el The jump start being saved.
			 * @return 1 if watched, 0 otherwise.
			 */
			@Override
			protected int get(Element el) { return ((JumpStart)el).watched ? 1 : 0; }
			/**
			 * Store a loaded watched flag into the given jump start.
			 *
			 * @param el The jump start being loaded.
			 * @param v Non-zero to mark it watched, zero otherwise.
			 */
			@Override
			protected void set(Element el, int v) { ((JumpStart)el).watched = v != 0; }
		},
		new Attribute.OrientationAttribute("orientation") {
			/**
			 * Read the orientation of the given jump start for saving.
			 *
			 * @param el The jump start being saved.
			 * @return the jump start's orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((JumpStart)el).orientation;
			}
			/**
			 * Store a loaded orientation into the given jump start.
			 *
			 * @param el The jump start being loaded.
			 * @param o The orientation read from the file.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((JumpStart)el).orientation = o;
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

		output.println("ELEMENT JumpStart");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		JumpStart it = new JumpStart(getCircuit());
		it.inputs.add(inputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Get the name of this jumpstart.
	 *
	 * @return the name.
	 * @jls.testedby jls.ui.CircuitAssert#jumpAlias()
	 */
	@Override
	public @Nullable String getName() {

		return name;
	} // end of getName method

	/**
	 * Get the direction of this jumpstart.
	 *
	 * @return orientation
	 */
	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * Get the number of bits in this element.
	 *
	 * @return the number of bits.
	 */
	@Override
	public int getBits() {

		return bits;
	} // end of getBits method

	/**
	 * Display info about this element.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		return bits + " bit wire name, value = " +
				BitSetUtils.toDisplay(currentValue,bits);
	} // end of showInfo method

	/**
	 * Remove this element and all jump ends with the same name.

	 * @param circ A reference back to the circuit the element is in.
	 */
	@Override
	public void remove(Circuit circ) {

		// remove from list of jump starts and list of names in circuit
		if (name == null)
			throw new IllegalStateException("jump start name not set yet");
		getCircuit().removeName(name);
		circ.removeJumpStart(name);

		// remove corresonding jump ends
		Set<Element> rems = new HashSet<Element>();
		for (Element el : circ.getElements()) {
			if (el instanceof JumpEnd end) {
				if (name.equals(end.getName()))
					rems.add(el);
			}
		}
		for (Element el : rems) {
			el.remove(circ);
		}

		// remove itself
		super.remove(circ);
	} // end of remove method

	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached());
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

		inputs.clear();

		if(orientation == Orientation.LEFT) {
			inputs.add(new Input("input",this,0,0,bits));
		}
		else if(orientation == Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,0,bits));
		}
	}

	/**
	 * A jump start can be watched.
	 *
	 * @return true.
	 */
	@Override
	public boolean canWatch() {

		return true;
	} // end of canWatch method

	/**
	 * See if this jumpstart is watched.
	 *
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {

		return watched;
	} // end of isWatched method

	/**
	 * Set whether this jumpstart is watched or not.
	 *
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {

		watched = state;
	} // end of setWatched method

	/**
	 * Set this element to tri-state or not.
	 * Propagate tri-state property to the other end(s) of the jump.
	 *
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
	public void setTriState(boolean which) {

		String myName = getName();
		for (Element el : getCircuit().getElements()) {
			if (!(el instanceof JumpEnd jend))
				continue;
			if (myName != null && myName.equals(jend.getName()))
				jend.setTriState(which);
		}
	} // end of setTriState method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/** The current value of the named wire during simulation, or null when tri-stated off. */
	private @Nullable BitSet currentValue = new BitSet();
	/** The jump ends with a matching name, built at simulation start. */
	private Set<JumpEnd> jumpEnds = new HashSet<JumpEnd>();

	/**
	 * Get the current value.
	 *
	 * @return the current value.
	 */
	@Override
	public @Nullable BitSet getCurrentValue() {

		if (currentValue == null)
			return null;
		else
			return (BitSet)currentValue.clone();
	} // end of getCurrentValue method

	/**
	 * Initialize this element by creating a set of all matching jump ends
	 * and setting the current value to 0.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		// create set of matching jump ends
		if (name == null)
			throw new IllegalStateException("jump start name not set yet");
		jumpEnds.clear();
		for (Element el : getCircuit().getElements()) {
			if (!(el instanceof JumpEnd jend))
				continue;
			if (name.equals(jend.getName())) {
				jumpEnds.add(jend);
			}
		}

		// set current value to 0
		currentValue = new BitSet();

	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	@Override
	public void react(long now, Simulator sim, @org.jspecify.annotations.Nullable Object todo) {


		// get the input value
		currentValue = inputs.get(0).getValue();
		if (currentValue != null)
			currentValue = (BitSet)currentValue.clone();

		// send to all matching jump ends
		for (JumpEnd jend : jumpEnds) {
			BitSet newValue = null;
			if (currentValue != null)
				newValue = (BitSet)currentValue.clone();
			sim.post(new SimEvent(now,jend,newValue));
		}
	} // end of react method

} // end of JumpStart class
