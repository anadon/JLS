package jls.elem;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Vector;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.BitSetUtils;
import jls.Circuit;
import jls.sim.Simulator;

/**
 * Display an input value on the circuit editor screen.
 * 
 * @author David A. Poplawski
 */
public final class Display extends LogicElement {
	
	// constants
	/** The input width a new display starts with. */
	private final int defaultBits = 1;
	
	// properties
	/** The width of the displayed input, in bits. */
	private int bits = defaultBits;
	/** The radix (2, 10 or 16) the value is displayed in. */
	private int base = 10;
	
	// legacy
	/** Legacy single-input orientation marker; -1 means "new save format". */
	int orient = -1;
	
	/**
	 * Create new element.
	 * 
	 * @param circuit The circuit this element is in.
	 */
	public Display(Circuit circuit) {
		
		super(circuit);
	} // end of constructor

	/**
	 * The number of input bits (issue #77: read by the GUI-side renderer).
	 *
	 * @return the number of bits.
	 */
	public int getBits() {

		return bits;
	} // end of getBits method

	/**
	 * The display radix (issue #77: read by the GUI-side renderer).
	 *
	 * @return the radix (2, 10 or 16).
	 */
	public int getBase() {

		return base;
	} // end of getBase method

	/**
	 * Set the number of bits (issue #77: applied by the GUI-side dialog).
	 *
	 * @param bits The new number of bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the display radix (issue #77: applied by the GUI-side dialog).
	 *
	 * @param base The new radix (2, 10 or 16).
	 */
	public void setBase(int base) {

		this.base = base;
	} // end of setBase method

	/**
	 * Initialize this element.
	 *
	 * @param g Unused.
	 */
	@Override
	public void init(jls.core.TextMetrics g) {

		// set up size
		int s = Geometry.SPACING;
		if (g != null) {

			// use existing size if it has one
			if (width == 0 && height == 0) {
				height = 2*s;
				jls.core.TextMetrics fm = g;
				BigInteger maxVal = new BigInteger("1").shiftLeft(bits).subtract(new BigInteger("1"));
				
				String longest = maxVal.toString(base).replaceAll(".","0");
				switch (base) {
				case 2:
					longest = longest + "B";
					break;
				case 16:
					longest = "0x" + longest;
				}
				int strLen = fm.stringWidth(" " + longest + " ");
				width = (strLen+2*s-1)/(2*s)*(2*s);
			}
		}
		
		// create input
		if(orient < 0) { // New save format
			inputs.add(new Input("input0",this,0,s,bits)); // Left
			inputs.add(new Input("input1",this,width/2,0,bits)); // Top
			inputs.add(new Input("input2",this,width/2,height,bits)); // Bottom
			inputs.add(new Input("input3",this,width,s,bits)); // Right
		} else { // Old save format
			switch(orient) {
			case 0:
				inputs.add(new Input("input",this,width/2,0,bits)); // Top
				break;
			case 1:
				inputs.add(new Input("input",this,width/2,height,bits)); // Bottom
				break;
			case 2:
				inputs.add(new Input("input",this,0,s,bits)); // Left
				break;
			case 3:
				inputs.add(new Input("input",this,width,s,bits)); // Right
				break;
			}
		}
		
	} // end of init method

	/**
	 * Prune detached inputs (issue #77: the model-mutating step formerly
	 * run inside {@code draw}, now called first by the GUI-side renderer).
	 * If one, two or three of the four inputs are unattached they are
	 * removed; if that would leave none, all four are restored. Saved-file
	 * bytes depend on the resulting input set, so the renderer runs this in
	 * the same order the old draw did.
	 */
	public void prune() {

		// draw attached input (draw all four if nothing is attached)
		// get unattached inputs
		int s = Geometry.SPACING;
		Vector<Input>detach = new Vector<Input>(4);
		for (Input input : inputs) {
			if (!input.isAttached())
				detach.add(input);
		}

		// if there are one, two or three unattached ones
		int count = detach.size();
		if (count > 0 && count < 4) {

			// remove unattached inputs
			inputs.removeAll(detach);

			// if one input remains, fix its bit number

			// if no inputs left, put all four back
			if (inputs.size() == 0) {
				inputs.add(new Input("input0",this,0,s,bits)); // Left
				inputs.add(new Input("input1",this,width/2,0,bits)); // Top
				inputs.add(new Input("input2",this,width/2,height,bits)); // Bottom
				inputs.add(new Input("input3",this,width,s,bits)); // Right
			}
		}
	} // end of prune method

	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT Display");
		super.save(output);
		output.println("END");
	} // end of save method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own persisted attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit count from the given display element.
			 */
			@Override
			protected int get(Element el) { return ((Display)el).bits; }
			/**
			 * Store the bit count into the given display element.
			 */
			@Override
			protected void set(Element el, int v) { ((Display)el).bits = v; }
		},
		new Attribute.IntAttribute("base") {
			/**
			 * Read the display radix from the given display element.
			 */
			@Override
			protected int get(Element el) { return ((Display)el).base; }
			/**
			 * Store the display radix into the given display element.
			 */
			@Override
			protected void set(Element el, int v) { ((Display)el).base = v; }
		},
		new Attribute.IntAttribute("orient") {
			// legacy single-input save format marker; only saved when
			// present in the loaded file
			/**
			 * Read the legacy orientation marker from the given display element.
			 */
			@Override
			protected int get(Element el) { return ((Display)el).orient; }
			/**
			 * Store the legacy orientation marker into the given display element.
			 */
			@Override
			protected void set(Element el, int v) { ((Display)el).orient = v; }
			/**
			 * Suppress saving the orientation marker unless a legacy value was loaded.
			 */
			@Override
			protected boolean omitted(Element el) {
				// -1 is the "no marker" sentinel; 0 (Top) is a valid
				// legacy value and must be re-saved (issue #57)
				return ((Display)el).orient < 0;
			}
			/**
			 * Deliberately skip copying the legacy orientation marker.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy never carried the legacy marker
				// to the copy
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
	 * Copy this element.
	 *
	 * @return a copy of this element.
	 */
	@Override
	public Element copy() {

		Display it = new Display(circuit);
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
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
		
		return bits + " bit display, value = " + BitSetUtils.toDisplay(currentValue,bits);
	} // end of showInfo method
	
	/**
	 *  This method will rotate the display if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.TextMetrics g)
	{
		// No-op
	}
	
	/**
	 * Tells if a display is capable of rotatating, can only rotate when input has no attachment.
	 * @return False if input has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		// Displays cannot be rotated ever since they implement the Stop behavior for inputs.
		return false;
	}
	
	/**
	 * Tells if a display is capable of flippinging, can only flip when input has no attachment.
	 * @return False if input has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		// Displays cannot be flipped ever since they implement the Stop behavior for inputs.
		return false;
	}
	
	/**
	 * This method will flip a display
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.TextMetrics g)
	{
		// No-op
	}

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The value being displayed; null shows as HiZ. */
	private BitSet currentValue = new BitSet();

	/**
	 * The value currently being displayed (issue #77: read by the GUI-side
	 * renderer); null shows as HiZ.
	 *
	 * @return the current value, or null for HiZ.
	 */
	public BitSet getCurrentValue() {

		return currentValue;
	} // end of getCurrentValue method

	/**
	 * Set the current display value to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		Input in = inputs.get(0);
		if (in.isAttached() && in.getWireEnd().getNet().isTriState()) {
			currentValue = null;
		}
		else {
			currentValue = new BitSet();
		}
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
		
		currentValue = inputs.get(0).getValue();
	} // end of react method

} // end of Display class
