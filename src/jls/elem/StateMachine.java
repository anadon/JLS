package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.jspecify.annotations.Nullable;

import jls.BitSetUtils;
import jls.Circuit;
import jls.TellUser;
import jls.core.Geometry;
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
 * The state machine editor and simulation code.
 *
 * @author David A. Poplawski
 */
public final class StateMachine extends LogicElement
		implements Timed {

	// default values
	/** Propagation delay a new state machine starts with. */
	private static final int defaultPropDelay = 30;
	/** Trigger edge a new state machine starts with. */
	private static int defaultTrigger = 1;	// 1=rising, -1=falling

	// the initial-state rule (issue #52, M13), stated once: the editor
	// refuses to close without an initial state, and simulation start
	// falls back benignly when a hand-edited file lacks one
	/** The user-visible message when the initial-state rule is violated. */
	static final String INITIAL_STATE_CONSTRAINT =
			"A state machine needs an initial state - mark one before closing";

	// saved properties
	/** Time units between a triggering clock edge and the new output values. */
	private int propDelay = defaultPropDelay;
	/** The states of this machine. */
	private Set<State> states = new HashSet<State>();
	/** Triggering clock edge: 1 for rising, -1 for falling. */
	private int trigger = defaultTrigger;
	/** The name of this state machine. */
	private String name = "";

	// for loading from file
	/**
	 * Parser state while reading a state machine from a file, naming which
	 * kind of element the loader is currently building.
	 */
	private enum LoadState {
		/** Reading the machine's own attributes. */
		machine,
		/** The last line read started a new state. */
		newState,
		/** The last line read set an output of the state being built. */
		newOutput,
		/** The last line read set a transition of the state being built. */
		newTransition
	};
	/** Where the file loader is in the state/output/transition structure. */
	private LoadState loadState = LoadState.machine;
	/** The state currently being built by the file loader. Null until the
	 *  first "state" attribute is read. */
	private @Nullable State buildState;


	/**
	 * Create a new adder element.
	 *
	 * @param circuit The circuit this element is part of.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#stateMachineInitialStateRuleIsSharedWithSimStart()
	 * @jls.testedby jls.elem.ParameterValidationTest#stateMachineWithoutInitialStateSurvivesSimInit()
	 */
	public StateMachine(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Find the state marked initial.
	 *
	 * @return the initial state, or null if none is marked.
	 */
	private @Nullable State findInitialState() {

		for (State state : states) {
			if (state.isInitial()) {
				return state;
			}
		}
		return null;
	} // end of findInitialState method

	/**
	 * The initial-state rule, shared by the editor dialog (which refuses
	 * to close without one) and simulation start (which falls back
	 * benignly), issue #52 M13.
	 *
	 * @return the violated constraint message, or null if an initial
	 *         state exists.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#stateMachineInitialStateRuleIsSharedWithSimStart()
	 */
	public @Nullable String checkInitialState() {

		return findInitialState() == null ? INITIAL_STATE_CONSTRAINT : null;
	} // end of checkInitialState method


	/**
	 * Initialize internal info for this element.
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
				String dname = name;
				if (name.isEmpty())
					dname = "State Machine";
				width = fm.stringWidth(" " + dname + " ");
				for (State state : states) {
					width = Math.max(width,state.getWidthInfo(fm));
				}
				width = Math.max(width,fm.stringWidth("clock"));
				width = (width+s-1)/s*s+s;
			}
		}

		// create sorted lists of input and output signals
		SortedSet<String> inputList = new TreeSet<String>();
		SortedSet<String> outputList = new TreeSet<String>();
		for (State state : states) {
			inputList.addAll(state.getInputs());
			outputList.addAll(state.getOutputs());
		}

		// create new list alternating elements from the two lists
		Set<String>	saveInputs = new HashSet<String>(inputList);
		Vector<String> pins = new Vector<String>(inputList.size()+outputList.size());
		boolean takeFromInput = true;
		while (inputList.size()+ outputList.size() > 0) {
			if (takeFromInput) {
				takeFromInput = false;
				if (!inputList.isEmpty()) {
					String pin = inputList.first();
					pins.add(pin);
					inputList.remove(pin);
				}
			}
			else {
				takeFromInput = true;
				if (!outputList.isEmpty()) {
					String pin = outputList.first();
					pins.add(pin);
					outputList.remove(pin);
				}
			}
		}

		// create input and output signals and determine height
		height = s;
		for (String signal : pins) {
			if (saveInputs.contains(signal)) {
				Input in = new Input(signal,this,0,height,inputBits(signal));
				inputs.add(in);
				height += s;
			}
			else {
				Output out = new Output(signal,this,width,height,outputBits(signal));
				outputs.add(out);
				height += s;
			}
		}
		height += 4*s;

		inputs.add(new Input("clock",this,0,height-s,1));

	} // end of init method

	/**
	 * Determine number of bits in a given input.
	 *
	 * @param signal The input signal name.
	 *
	 * @return The number of bits in that signal.
	 */
	private int inputBits(String signal) {

		for (State state : states) {
			int bits = state.inputBits(signal);
			if (bits > 0)
				return bits;
		}
		return 0; // should never do this
	} // end of inputBits method

	/**
	 * Determine number of bits in a given output.
	 *
	 * @param signal The output signal name.
	 *
	 * @return The number of bits in that signal.
	 */
	private int outputBits(String signal) {

		for (State state : states) {
			int bits = state.outputBits(signal);
			if (bits > 0)
				return bits;
		}
		return 0; // should never do this
	} // end of inputBits method


	/**
	 * Save this element in a file.
	 *
	 * @param output The PrintWriter to write to.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT StateMachine");
		super.save(output);
		// canonical state order (#180): the state set is a HashSet and
		// State has no hashCode override, so iterating it directly would
		// emit the state blocks in identity-hash order - different bytes
		// per run for the same machine, breaking canonical serialization
		// (#166) and stateHash (#163). Sort by the states' own content.
		java.util.List<State> ordered = new java.util.ArrayList<State>(states);
		ordered.sort(State.saveOrder());
		for (State state : ordered) {
			state.save(output);
		}
		output.println("END");
	} // end of save method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's simple attributes. The state
	// list (state/output/trans/next/pair lines) is structured data and
	// stays handwritten in save(), the setValue load state machine and
	// copy().
	/** This element's own saved attributes (name, delay, trig). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read this machine's name for the "name" attribute.
			 */
			@Override
			protected String get(Element el) { return ((StateMachine)el).name; }
			/**
			 * Write this machine's name from the "name" attribute.
			 */
			@Override
			protected void set(Element el, String v) { ((StateMachine)el).name = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read this machine's propagation delay for the "delay" attribute.
			 */
			@Override
			protected int get(Element el) { return ((StateMachine)el).propDelay; }
			/**
			 * Write this machine's propagation delay from the "delay" attribute.
			 */
			@Override
			protected void set(Element el, int v) { ((StateMachine)el).propDelay = v; }
		},
		new Attribute.IntAttribute("trig") {
			/**
			 * Read this machine's trigger edge for the "trig" attribute.
			 */
			@Override
			protected int get(Element el) { return ((StateMachine)el).trigger; }
			/**
			 * Write this machine's trigger edge from the "trig" attribute.
			 */
			@Override
			protected void set(Element el, int v) { ((StateMachine)el).trigger = v; }
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
	 * Set a String instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#stateMachineInitialStateRuleIsSharedWithSimStart()
	 */
	@Override
	public void setValue(String name, String value) {

		switch (loadState) {
		case machine:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			} else {
				// the attribute registry handles "name"
				super.setValue(name,value);
			}
			break;
		case newState:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			}
			else if (name.equals("output")) {
				loadState = LoadState.newOutput;
				if (buildState != null) buildState.setOutputValue(value);
			}
			else if (name.equals("trans")) {
				loadState = LoadState.newTransition;
				if (buildState != null) buildState.setTransValue(name,value);
			}
			break;
		case newOutput:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			}
			else if (name.equals("output")) {
				loadState = LoadState.newOutput;
				if (buildState != null) buildState.setOutputValue(value);
			}
			else if (name.equals("trans")) {
				loadState = LoadState.newTransition;
				if (buildState != null) buildState.setTransValue(name,value);
			}
			break;
		case newTransition:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			}
			else if (name.equals("output")) {
				loadState = LoadState.newOutput;
				if (buildState != null) buildState.setOutputValue(value);
			}
			else if (name.equals("trans") || name.equals("next")) {
				loadState = LoadState.newTransition;
				if (buildState != null) buildState.setTransValue(name,value);
			}
			break;
		}
	} // end of setValue (string) method

	/**
	 * Set an int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#stateMachineInitialStateRuleIsSharedWithSimStart()
	 */
	@Override
	public void setValue(String name, int value) {

		switch (loadState) {
		case machine:
			// the attribute registry handles "trig" and "delay"
			super.setValue(name,value);
			break;
		case newState:
			if (buildState != null) buildState.setValue(name,value);
			break;
		case newOutput:
			if (buildState != null) buildState.setOutputValue(name,value);
			break;
		case newTransition:
			if (buildState != null) buildState.setTransValue(name,value);
			break;
		}

	} // end of setValue (int) method

	/**
	 * Set a long instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, long value) {

		switch (loadState) {
		case newOutput:
			if (buildState != null) buildState.setOutputValue(name,value);
			break;
		case machine:
		case newState:
		case newTransition:
			break;
		}

	} // end of setValue (long) method

	/**
	 * Set a pair of int instance variable values (during a load).
	 *
	 * @param v1 The first value.
	 * @param v2 The second value.
	 */
	@Override
	public void setPair(int v1, int v2) {

		if (buildState != null) buildState.setTransPair(v1,v2);
	} // end of setPair method

	/**
	 * Can't copy state machines.
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

		// create new element; the attribute registry copies name, delay
		// and trigger in super.copy below
		StateMachine it = new StateMachine(getCircuit());

		// copy all the states
		Map<String,State> stateMap = new HashMap<String,State>();
		for (State state : states) {
			State st = state.copy(it);
			it.states.add(st);
			stateMap.put(state.getName(),st);
		}

		// link transitions to states
		for (State istate : it.states) {
			istate.linkTrans(stateMap);
		}

		// copy inputs and outputs
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}

		// finish up
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

		String trig = "falling";
		if (trigger == 1) {
			trig = "rising";
		}
		String msg = "state machine (" + trig + " edge triggered), ";
		if (currentState == null) {
			return msg + "no current state";
		}
		else {
			return msg + "current state = " + currentState.getName();
		}
	} // end of showInfo method

	/**
	 * Get the name of this state machine.
	 *
	 * @return the name.
	 */
	@Override
	public String getName() {

		return name;
	} // end of getName method

	/**
	 * Get the set of states in this state machine.
	 *
	 * @return the set of states.
	 */
	public Set<State> getStates() {

		return states;
	} // end of getStates method

	/**
	 * Get all inputs from this state machine.
	 *
	 * @return all outputs.
	 */
	public Vector<Input> getInputs() {

		return inputs;
	} // end of getInputs method

	/**
	 * Get all outputs from this state machine.
	 *
	 * @return all outputs.
	 */
	public Vector<Output> getOutputs() {

		return outputs;
	} // end of getOutputs method

	/**
	 * Get the triggering clock edge (issue #77: the GUI-side editor
	 * dialog reads and writes it through these accessors).
	 *
	 * @return 1 for rising, -1 for falling.
	 */
	public int getTrigger() {

		return trigger;
	} // end of getTrigger method

	/**
	 * Set the triggering clock edge.
	 *
	 * @param trigger 1 for rising, -1 for falling.
	 */
	public void setTrigger(int trigger) {

		this.trigger = trigger;
	} // end of setTrigger method

	/**
	 * Set this state machine's name (issue #77: the editor dialog applies
	 * the entered name here).
	 *
	 * @param name The new name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * Replace this machine's state set (issue #77: the editor dialog
	 * restores the pre-edit states on cancel).
	 *
	 * @param states The state set to install.
	 */
	public void setStates(Set<State> states) {

		this.states = states;
	} // end of setStates method

	/**
	 * Detach from all wires and recompute size (issue #77: the editor
	 * dialog calls this when a change altered the input/output signature,
	 * reproducing the former {@code change} body's detach/re-init step).
	 *
	 * @param g The Graphics object to use to determine size.
	 */
	public void reinit(jls.core.TextMetrics g) {

		detach();
		width = 0;
		height = 0;
		init(g);
	} // end of reinit method

	/**
	 * State machines have timing info (propagation delay).
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
	 * State machines can be modified.
	 *
	 * @return true.
	 */
	@Override
	public boolean canChange() {

		return true;
	} // end of canChange method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/** The most recently seen clock input value. */
	private int oldClock;
	/** True between a triggering clock edge and the new output values. */
	private boolean busy = false;	// true between edge and outputs value
	/** The state the machine is currently in. Null until {@link #initSim}
	 *  selects the starting state. */
	private @Nullable State currentState;
	/** True once the no-matching-transition warning has been shown. */
	private boolean noMatchReported = false;	// no-matching-transition warned already? (#98, S5)

	/**
	 * Initialize this element by setting its outputs to 0,
	 * busy to false, and currentState to the initial state.
	 *
	 * @param sim Unused.
	 *
	 * @jls.testedby jls.elem.ParameterValidationTest#stateMachineWithoutInitialStateSurvivesSimInit()
	 */
	@Override
	public void initSim(Simulator sim) {

		// set all outputs to 0
		for (Output output : outputs) {
			output.setValue(new BitSet());
		}

		// set latest clock input value to 0
		oldClock = 0;

		// re-arm the no-matching-transition diagnostic (#98, S5)
		noMatchReported = false;

		// find the initial state (the same rule the editor dialog
		// enforces at OK, issue #52 M13)
		currentState = findInitialState();

		// hand-edited files can still lack one (no initial state, or a
		// zero-state machine); fall back to an arbitrary state rather
		// than NPE the simulator
		if (currentState == null) {
			if (states.isEmpty()) {
				// stay busy so a clock edge never asks the (nonexistent)
				// current state for a successor
				busy = true;
				return;
			}
			currentState = states.iterator().next();
		}

		// send all initial state output values
		currentState.sendOutputs(0,sim);

		// make not busy
		busy = false;

	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo PinChanged if an input has changed, otherwise the state
	 *             being entered.
	 */
	@Override
	public void react(long now, Simulator sim, SimEvent.Payload todo) {

		switch (todo) {

		// if an input has changed ...
		case PinChanged _ -> {

			// get clock input value
			BitSet cval = getInput("clock").getValue();
			if (cval == null)
				cval = new BitSet();
			int newClock = (int)(BitSetUtils.ToLong(cval));

			// if still doing a transition, don't start another
			if (busy) {
				oldClock = newClock;
				return;
			}

			// check for proper clock change
			if (trigger == 1) {	// rising edge
				if (!(oldClock == 0 && newClock == 1)) {
					oldClock = newClock;
					return;
				}
			}
			else {	// falling edge
				if (!(oldClock == 1 && newClock == 0)) {
					oldClock = newClock;
					return;
				}
			}

			// do a transition, so figure out next state.
			// currentState is null only for a zero-state machine, which
			// initSim leaves permanently busy, so react returns above
			// before reaching here; guard anyway rather than NPE.
			State cur = currentState;
			if (cur == null) {
				oldClock = newClock;
				return;
			}
			State newState = cur.getNextState();

			// if no transition matches, stay in the current state:
			// remember the clock value so later edges are still
			// recognized, and tell the user once instead of silently
			// freezing (issue #98, S5)
			if (newState == null) {
				oldClock = newClock;
				if (!noMatchReported) {
					noMatchReported = true;
					TellUser.warn(null,
							"state machine \"" + name + "\": no transition"
							+ " matches from state \""
							+ cur.getName() + "\" at time " + now
							+ "; staying in the current state",
							"Simulation");
				}
				return;
			}

			// save clock value and make state machine busy
			oldClock = newClock;
			busy = true;

			// post event
			sim.post(new SimEvent(now+propDelay,this,
					new StateChanged(newState)));
		}

		// the transition completing: enter the new state
		case StateChanged(State next) -> {

			// set the new state
			currentState = next;

			// send values to outputs
			next.sendOutputs(now,sim);

			// no longer busy
			busy = false;
		}

		case NewValue _, TriStateOff _, MemoryRead _, MemoryWrite _,
				TableOutput _ ->
			throw new IllegalStateException("unexpected payload: " + todo);
		}

	} // end of react method

} // end of StateMachine class
