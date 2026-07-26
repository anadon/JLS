package jls.elem;

import java.io.*;
import java.util.*;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.sim.*;

/**
 * Circuit element connecting this circuit to an imported subcircuit.
 *
 * @author David A. Poplawski
 */
public final class SubCircuit extends LogicElement
		implements TriProp, Rotatable, Editable {

	// properties
	/**
	 * The imported subcircuit this element represents, or null until set by
	 * {@link #setSubCircuit} / a load after construction.
	 */
	private @Nullable Circuit subCircuit;		// the subcircuit
	/**
	 * The name this subcircuit has in the circuit it was imported into, or
	 * null until set by {@link #setName} / a load after construction.
	 */
	private @Nullable String name;			// the name in the circuit imported into
	/** Map from this element's inputs to the corresponding input pins in the subcircuit. */
	private Map<Input,InputPin> inmap = new HashMap<Input,InputPin>();
	/** Map from the subcircuit's output pins to this element's corresponding outputs. */
	private Map<OutputPin,Output> outmap = new HashMap<OutputPin,Output>();
	/** The direction this element faces (side its inputs are on). */
	private Orientation orientation = Orientation.RIGHT;

	/**
	 * Create printable view of this element.
	 *
	 * @return the name, orientation and input/output pin names.
	 */
	@Override
	public String toString() {

		String result = "SubCircuit[" + name + ",";
		switch (orientation) {
		case RIGHT: result += "right"; break;
		case LEFT: result += "left"; break;
		case UP: result += "up"; break;
		case DOWN: result += "down"; break;
		}
		result += ",inputs={";
		boolean firstTime = true;
		for (Input p : inmap.keySet()) {
			if (!firstTime)
				result += ",";
			firstTime = false;
			result += p.getName();
		}
		result += "},outputs={";
		firstTime = true;
		for (OutputPin p : outmap.keySet()) {
			if (!firstTime)
				result += ',';
			firstTime = false;
			result += p.getName();
		}
		result += "}]";
		return result;
	} // end of toString method

	/**
	 * Create a new subcircuit element.
	 *
	 * @param circuit The circuit this element is part of.
	 * @jls.testedby jls.edit.SaveAsNameCheckTest#importedCircuitsAreSkipped()
	 * @jls.testedby jls.elem.HollowVsFilledCollisionTest#probe()
	 */
	public SubCircuit(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Save the subcircuit that this element represents.
	 *
	 * @param subCirc The subcircuit.
	 */
	public void setSubCircuit(Circuit subCirc) {

		subCircuit = subCirc;
	} // end of setSubCircuit method

	/**
	 * Get the subcircuit this element represents.
	 *
	 * @return the subcircuit.
	 * @jls.testedby jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 */
	public Circuit getSubCircuit() {

		if (subCircuit == null)
			throw new IllegalStateException("subcircuit not set yet");
		return subCircuit;
	} // end of getSubCircuit method

	/**
	 * Get the (local) name of this subcircuit element (not the circuit it represents).
	 *
	 * @return the name.
	 */
	@Override
	public String getName() {

		if (name == null)
			throw new IllegalStateException("subcircuit name not set yet");
		return name;
	} // end of getName method

	/**
	 * Set the (local) name of this subcircuit element.
	 *
	 * @param name The name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * The direction this subcircuit faces (issue #77: read by the GUI-side
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
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// determine width if needed
		int s = Geometry.SPACING;
		if (g != null) {
			if (width == 0 && height == 0) {
				jls.core.TextMetrics fm = g;
				width = fm.stringWidth(" " + name + " ");
				for (Element el : getSubCircuit().getElements()) {
					if (el instanceof InputPin || el instanceof OutputPin) {
						Pin p = (Pin)el;
						String pinName = p.getName();
						width = Math.max(width,fm.stringWidth(pinName));
					}
				}
				width = (width+s-1)/s*s+s;
			}

		}

		// create sorted lists of input pins and output pins
		Comparator<Pin> cmp = new Comparator<Pin>() {
			/**
			 * Order two pins alphabetically by name.
			 *
			 * @param s1 The first pin.
			 * @param s2 The second pin.
			 *
			 * @return negative, zero or positive as s1's name orders before,
			 *         equal to or after s2's name.
			 */
			@Override
			public int compare(Pin s1, Pin s2) {
				return (s1.getName()).compareTo(s2.getName());
			}
		};
		SortedSet<InputPin> inputList = new TreeSet<InputPin>(cmp);
		SortedSet<OutputPin> outputList = new TreeSet<OutputPin>(cmp);
		for (Element el : getSubCircuit().getElements()) {
			if (el instanceof InputPin pin) {
				inputList.add(pin);
			}
			else if (el instanceof OutputPin pin) {
				outputList.add(pin);
			}
		}

		// create new list alternating elements from the two lists
		Vector<Pin> pins = new Vector<Pin>(inputList.size()+outputList.size());
		boolean fromInput = true;
		while (inputList.size()+ outputList.size() > 0) {
			if (fromInput) {
				fromInput = false;
				if (!inputList.isEmpty()) {
					InputPin pin = inputList.first();
					pins.add(pin);
					inputList.remove(pin);
				}
			}
			else {
				fromInput = true;
				if (!outputList.isEmpty()) {
					OutputPin pin = outputList.first();
					pins.add(pin);
					outputList.remove(pin);
				}
			}
		}

		// create inputs and outputs and determine height
		height = s;
		for (Pin el : pins) {
			if (el instanceof InputPin pin) {
				Input in;
				if(orientation == Orientation.RIGHT)
				{
					in = new Input(pin.getName(),this,0,height,pin.getBits());
				}
				else
				{
					in = new Input(pin.getName(),this,width,height,pin.getBits());
				}
				inputs.add(in);
				height += s;
				inmap.put(in,pin);
			}
			else if (el instanceof OutputPin pin) {
				Output out;
				if(orientation == Orientation.RIGHT)
				{
					out = new Output(pin.getName(),this,width,height,pin.getBits());
				}
				else
				{
					out = new Output(pin.getName(),this,0,height,pin.getBits());
				}
				outputs.add(out);
				height += s;
				outmap.put(pin,out);
				for (Input input : pin.inputs) {
					if (input.isAttached()) {
						WireEnd end = input.getWireEnd();
							if (end == null)
								throw new IllegalStateException("attached input has no wire end");
							out.setTriState(end.isTriState());
					}
				}
				if (pin.isLoadTriState()) {
					out.loadSetTriState();
				}
			}
		}
		height += 2*s;

	} // end of init method

	/**
	 * Save this element in a file.
	 *
	 * @param output The PrintWriter to write to.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT SubCircuit");
		output.println(" String orient \"" + orientation.toString() + "\"");
		super.save(output);
		getSubCircuit().save(output);
		output.println("END");
	} // end of save method

	/**
	 * A subcircuit block needs whatever format version its nested
	 * circuit needs - a file states its version once, at the top
	 * (issue #79), so the requirement propagates up through here.
	 *
	 * @return the minimum format version for the nested circuit.
	 */
	@Override
	public int saveFormatVersion() {

		return getSubCircuit().formatVersionNeeded();
	} // end of saveFormatVersion method

	/**
	 * Set a String instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, String value) {

		if (name.equals("orient")) {
			if(value.equals("LEFT"))
			{
				orientation = Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				orientation = Orientation.RIGHT;
			}

		} else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		// create infrastructure
		SubCircuit it = new SubCircuit(getCircuit());
		it.name = name;
		Circuit itSub = new Circuit(getSubCircuit().getName());
		it.subCircuit = itSub;
		itSub.setImported(it);
		it.orientation = orientation;

		// make a set of all elements except attached wire ends in subcircuit
		Set<Element> elements = new HashSet<Element>();
		for (Element el : getSubCircuit().getElements()) {
			if (el instanceof WireEnd end) {
				if (end.isAttached())
					continue;
			}
			elements.add(el);
		}
		Util.copy(elements,itSub);
		Util.partition(itSub);

		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}

		// build maps
		for (Input input : it.inputs) {
			for (Element el : itSub.getElements()) {
				if (!(el instanceof InputPin pin))
					continue;
				if (java.util.Objects.equals(input.getName(), pin.getName())) {
					it.inmap.put(input,pin);
				}
			}
		}
		for (Output output : it.outputs) {
			for (Element el : itSub.getElements()) {
				if (!(el instanceof OutputPin pin))
					continue;
				if (java.util.Objects.equals(output.getName(), pin.getName())) {
					it.outmap.put(pin,output);
				}
			}
		}

		// finish up
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Remove this element from the circuit.
	 * Unattaches from all wire nets.
	 * Forces wire nets it disconnects from to recheck their information.
	 *
	 * @param circ The circuit it is being removed from.
	 */
	@Override
	public void remove(Circuit circ) {

		if (name != null)
			circ.removeName(name);
		super.remove(circ);
	} // end of remove method

	/**
	 * Display info about this element.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		return getSubCircuit().getName() + " (a subcircuit)";
	} // end of showInfo method

	/**
	 * Set whether elements in the subcircuit are watched. The subcircuit
	 * itself is not {@link Watchable}; this propagates the setting to the
	 * watchable elements (and nested subcircuits) it contains.
	 *
	 * @param which True to set all watchable elements to watched, false to make them
	 *        not watched.
	 */
	public void setWatched(boolean which) {

		for (Element element : getSubCircuit().getElements()) {
			if (element instanceof Watchable watchable) {
				watchable.setWatched(which);
			}
			else if (element instanceof SubCircuit sub) {
				sub.setWatched(which);
			}
		}
	} // end of setWatched method

	/**
	 * Reset propagation delays of all elements in the subcircuit.
	 */
	@Override
	public void resetPropDelay() {

		getSubCircuit().resetAllDelays();
	} // end of resetPropDelay method

	/**
	 * Remap all input and output pins to inputs and outputs.
	 * Used by undo/redo (finishDo).
	 *
	 * @param circ The new circuit containing new versions of the input and output pins.
	 */
	public void remapPins(Circuit circ) {

		inmap.clear();
		outmap.clear();
		for (Element el : circ.getElements()) {
			if (el instanceof InputPin pin) {
				for (Input in : inputs) {
					if (java.util.Objects.equals(in.getName(), pin.getName())) {
						inmap.put(in,pin);
					}
				}
			}
			else if (el instanceof OutputPin pin) {
				for (Output out : outputs) {
					if (java.util.Objects.equals(out.getName(), pin.getName())) {
						outmap.put(pin,out);
					}
				}
			}
		}
	} // end of remapPins method

	// used by setTriState to avoid infinite recursion when an
	// input of this elements is connected to a tri-state propagating output
	/** Input pins already visited by setTriState, to avoid infinite recursion. */
	private Set<InputPin> pinsChecked = new HashSet<InputPin>();

	/**
	 * Set all subcircuit input pins to tri-state or not.
	 *
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
	public void setTriState(boolean which) {

		for (Input input : inputs) {
			InputPin p = inmap.get(input);
			if (p == null)
				throw new IllegalStateException("input has no mapped subcircuit input pin");

			// if already checked, don't do it again
			if (pinsChecked.contains(p))
				continue;

			if (!input.isAttached()) {

				// unattached are not tri-state, so propagate false
				pinsChecked.add(p);
				p.setTriState(false);
				pinsChecked.remove(p);
			}
			else {

				// attached can be either, so find out which and propagate
				WireEnd w = input.getWireEnd();
				if (w == null)
					throw new IllegalStateException("attached input has no wire end");
				boolean is = w.isTriState();
				pinsChecked.add(p);
				p.setTriState(is);
				pinsChecked.remove(p);
			}
		}
	} // end of setTriState method

	/**
	 * See if this element can be flipped.
	 * It can be if none of its inputs or outputs are attached yet.
	 *
	 * @return true if it can be, false if not.
	 */
	@Override
	public boolean canFlip() {
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
	} // end of canFlip method

	/**
	 * Flip this element in the display.
	 *
	 * @param g The Graphics object used to draw this element.
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {
		if(orientation == Orientation.LEFT)
		{
			orientation = Orientation.RIGHT;
		}
		else if(orientation == Orientation.RIGHT)
		{
			orientation = Orientation.LEFT;
		}
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
	 * Initialize inputs of this element, and of all elements in the subcircuit,
	 * to 0.
	 */
	@Override
	public void initInputs() {

		// initialize this element's inputs
		super.initInputs();

		// initialize subcircuit's elements
		for (Element element : getSubCircuit().getElementsInStableOrder()) {
			if (element instanceof LogicElement el) {
				el.initInputs();
			}
		}

	} // end of initInputs method

	/**
	 * Initialize this element by setting all output pins of this element to 0 or null
	 * and initializing all elements in the subcircuit.
	 *
	 * @param sim The simulator for this circuit.
	 */
	@Override
	public void initSim(Simulator sim) {

		// set all output pins
		for (Output out : outputs) {
			if (out.isTriState())
				out.setValue(null);
			else
				out.setValue(new BitSet());
		}

		// canonical seed order at every nesting depth (#181): the inner
		// circuit's time-0 events must be posted in stable-id order for
		// the same reason as Simulator.initSimulation's top-level walk
		for (Element el : getSubCircuit().getElementsInStableOrder()) {
			if (el instanceof LogicElement lel) {
				lel.initSim(sim);
			}
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
	public void react(long now, Simulator sim, @org.jspecify.annotations.Nullable Object todo) {

		// send all input values to input pins of subcircuit
		for (Input in : inputs) {
			InputPin pin = inmap.get(in);
			if (pin == null)
				throw new IllegalStateException("input has no mapped subcircuit input pin");
			BitSet copy = null;
			if (in.getValue() != null)
				copy = (BitSet)in.getValue().clone();
			sim.post(new SimEvent(now,pin,copy));
		}

	} // end of react method

	/**
	 * Send value to the output corresponding to given OutputPin.
	 *
	 * @param pin The output pin.
	 * @param value The value.
	 * @param now The current time.
	 * @param sim The simulator.
	 */
	public void send(OutputPin pin, @Nullable BitSet value, long now, Simulator sim) {

		Output out = outmap.get(pin);
		if (out == null)
			throw new IllegalStateException("no output mapped for the given output pin");
		out.propagate(value,now,sim);
	} // end of send method

} // end of SubCircuit class
