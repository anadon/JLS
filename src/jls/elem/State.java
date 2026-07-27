package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jls.BitSetUtils;
import jls.TellUser;
import jls.core.Geometry;
import jls.sim.Simulator;

/**
 * A single state.
 */
public class State {

	// properties
	/** the state machine this state is in. */
	private StateMachine machine;	// the state machine this state is in
	/** the name of this state. */
	private String name;
	/** true if this is the initial state of the machine. */
	private boolean initial = false;
	/** the outputs asserted by this state. */
	private Vector<Out> outs = new Vector<Out>();
	/** the transitions from this state. */
	private Set<Transition> trans = new HashSet<Transition>();
	/** the x-coordinate of the center of this state. */
	private int x;
	/** the y-coordinate of the center of this state. */
	private int y;
	/** the diameter of the circle representing this state. */
	private int diameter;
	/**
	 * Tracks the bit width and direction of a signal used within this state,
	 * so signal usage can be checked for consistency. The reference count
	 * records how many outputs/transitions use the signal; the entry is
	 * removed from the bitmap when it drops to zero.
	 */
	private static class Check {
		/** the bit width of the signal. */
		public int bits;
		/** true if the signal is an input, false if an output. */
		public boolean isInput;	// false if output
		/** the number of outputs/transitions using the signal. */
		public int refs; // delete from map when 0

		/**
		 * Create an empty check record.
		 */
		private Check() {
		}
	};
	/** signal names mapped to bit width/direction info, for consistency checks. */
	private Map <String,Check> bitmap = new HashMap<String,Check>();
	// signal names to number of bits, for consistency check

	// other temporary properties
	/** the saved x-coordinate, for restoring the position after a move. */
	private int savex;
	/** the saved y-coordinate, for restoring the position after a move. */
	private int savey;
	/** true if this state is drawn highlighted. */
	private boolean highlight;
	/**
	 * the most recently highlighted transition, so it can be deleted;
	 * null until {@link #highlightTrans} highlights one (genuinely
	 * optional).
	 */
	private @org.jspecify.annotations.Nullable Transition lastHighlighted;
	/**
	 * the output currently being constructed during a load; null until a
	 * load begins an output block ({@link #setOutputValue}).
	 */
	private @org.jspecify.annotations.Nullable Out buildOut;
	/**
	 * the transition currently being constructed during a load; null until
	 * a load begins a transition block ({@link #setTransValue}).
	 */
	private @org.jspecify.annotations.Nullable Transition buildTrans;

	/**
	 * Outputs from this state.
	 */
	public static class Out {

		/**
		 * the output signal name; null on a freshly constructed output
		 * until it is populated (two-phase build: {@link
		 * State#setOutputValue}/{@link State#addOutput}/{@link State#copy}
		 * set it before the output is used).
		 */
		public @org.jspecify.annotations.Nullable String signal;
		/** the number of bits in the output signal. */
		public int bits;
		/** the value the signal is set to in this state. */
		public long value;

		/**
		 * Create an empty output.
		 */
		private Out() {
		}

		/**
		 * Text representation of this output.
		 *
		 * @return the signal name, its bit count, and its value.
		 */
		@Override
		public String toString() { return signal + "[" + bits + "] = " + value; }
	} // end of Out class

	/**
	 * Transitions from a given state
	 */
	public static class Transition {

		/** the input signal tested by the condition (empty if none). */
		public String signal = "";
		/** the number of bits in the condition signal. */
		public int bits = 1;
		/** true if the condition tests for equality, false for inequality. */
		public boolean equal = true;
		/** the value the condition signal is compared with. */
		public int value = 0;
		/** true if this transition is always taken. */
		public boolean unconditional = true;
		/** true if this transition is taken when no other condition holds. */
		public boolean other = false;
		/**
		 * the state this transition goes to; null until linked (two-phase
		 * load: {@link State#linkTrans}/{@link State#fixTrans} resolve
		 * {@link #nextStateName} to a real reference).
		 */
		public @org.jspecify.annotations.Nullable State nextState;
		/**
		 * the name of the state this transition goes to, or null once
		 * {@link #nextState} has been linked; temporarily used during load.
		 */
		public @org.jspecify.annotations.Nullable String nextStateName = null;
		/** the intermediate points the transition line is drawn through. */
		public Vector<jls.core.GridPoint>points = new Vector<jls.core.GridPoint>();
		/** the corner point currently highlighted, or null if none. */
		public jls.core.@org.jspecify.annotations.Nullable GridPoint highlight = null;
		/** index into {@link #points} of the highlighted corner, or -1. */
		public int highlightIndex = -1;
		/** true if this transition is drawn highlighted. */
		public boolean highlighted;

		/**
		 * Create a transition with default (unconditional) settings.
		 */
		private Transition() {
		}

		/**
		 * Move the currently-highlighted corner point to a new location
		 * (issue #77: replaces the GUI mutating a shared {@code Point}
		 * in place; {@link jls.core.GridPoint} is immutable, so the point is
		 * replaced by value and the highlight marker re-pointed).
		 *
		 * @param x The new x-coordinate.
		 * @param y The new y-coordinate.
		 */
		public void moveHighlight(int x, int y) {

			if (highlightIndex >= 0 && highlightIndex < points.size()) {
				jls.core.GridPoint np = new jls.core.GridPoint(x, y);
				points.set(highlightIndex, np);
				highlight = np;
			}
		} // end of moveHighlight method
	} // end of Transition class

	/**
	 * Create a new state.
	 * Make its diameter large enough to contain the name.
	 *
	 * @param machine The state machine element this state is part of.
	 * @param name The name of the state.
	 * @param g The Graphics object to use, or null when building headless
	 *          (e.g. during a file load, before layout).
	 */
	public State(StateMachine machine, String name, jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		this.machine = machine;
		this.name = name;
		if (g != null) {
			jls.core.TextMetrics fm = g;
			diameter = Math.max(fm.stringWidth("  " + name + "  "),Geometry.STATE_DIAMETER);
		}
	} // end of constructor

	/**
	 * Make a copy of this state.
	 * *
	 * @param it The new state machine.
	 *
	 * @return A copy of this state.
	 */
	public State copy(StateMachine it) {

		// create new state and add to map
		State newState = new State(it,name,null);
		//stateMap.put(name,newState);

		// fill in basic info
		newState.x = x;
		newState.y = y;
		newState.diameter = diameter;
		newState.initial = initial;

		// make copy of all outs
		for (Out out : outs) {
			Out newOut = new Out();
			newOut.signal = out.signal;
			newOut.value = out.value;
			newOut.bits = out.bits;
			newState.outs.add(newOut);
		}

		// make copy of all transitions
		for (Transition tran : trans) {
			Transition newTrans = new Transition();
			newTrans.signal = tran.signal;
			newTrans.unconditional = tran.unconditional;
			newTrans.other = tran.other;
			newTrans.equal = tran.equal;
			newTrans.value = tran.value;
			newTrans.bits = tran.bits;
			State tranNext = tran.nextState;
			if (tranNext == null) {
				throw new IllegalStateException(
						"copying a transition whose next state is not linked");
			}
			newTrans.nextStateName = tranNext.getName();
			for (jls.core.GridPoint p : tran.points) {
				newTrans.points.add(new jls.core.GridPoint(p.x(),p.y()));
			}
			newState.trans.add(newTrans);
		}

		// make a copy of the signal bitmap
		newState.bitmap = new HashMap<String,Check>();
		for (String sig : bitmap.keySet()) {
			Check ch = bitmap.get(sig);
			Check newch = new Check();
			newch.bits = ch.bits;
			newch.isInput = ch.isInput;
			newch.refs = ch.refs;
			newState.bitmap.put(sig, newch);
		}
		return newState;
	} // end of copy method

	/**
	 * Replace all symbolic links to next states with actual references.
	 *
	 * @param stateMap A map from state name to state.
	 */
	public void linkTrans(Map<String,State>stateMap) {

		for (Transition tran : trans) {
			if (tran.nextStateName != null) {
				tran.nextState = stateMap.get(tran.nextStateName);
			}
		}
	} // end of linkTrans method

	/**
	 * The canonical order state blocks are saved in (#180): by state
	 * name - unique within a machine, the create/rename dialogs enforce
	 * it - tie-broken by grid position, so even a hand-edited file with
	 * a duplicated name still gets a total, content-derived order. The
	 * state set is a HashSet, so without this order two loads of one
	 * machine would save its states in different, identity-hash orders.
	 *
	 * @return the comparator ordering states for save.
	 *
	 * @jls.testedby jls.DeterministicSaveTest#stateMachineSavesStatesInNameOrder()
	 * @jls.testedby jls.DeterministicSaveTest#stateMachineStateHashIsContentDetermined()
	 */
	static java.util.Comparator<State> saveOrder() {

		return java.util.Comparator.comparing((State state) -> state.name)
				.thenComparingInt(state -> state.x)
				.thenComparingInt(state -> state.y);
	} // end of saveOrder method

	/**
	 * The canonical order a state's transition blocks are saved in
	 * (#180): unconditional ("always") first, then "else", then
	 * conditionals by (signal, eq flag, value, bits), tie-broken by the
	 * next state's name. The transition dialog forbids two conditionals
	 * on the same signal and value, so the key is a total order for any
	 * machine the editor can produce.
	 *
	 * @return the comparator ordering transitions for save.
	 */
	private static java.util.Comparator<Transition> transitionSaveOrder() {

		return java.util.Comparator
				.comparingInt((Transition tr) ->
						tr.unconditional ? 0 : tr.other ? 1 : 2)
				.thenComparing(tr -> tr.signal)
				.thenComparingInt(tr -> tr.equal ? 0 : 1)
				.thenComparingInt(tr -> tr.value)
				.thenComparingInt(tr -> tr.bits)
				.thenComparing(tr -> {
					State ns = tr.nextState;
					if (ns == null) {
						throw new IllegalStateException(
								"saving a transition whose next state is not linked");
					}
					return ns.getName();
				});
	} // end of transitionSaveOrder method

	/**
	 * This state's transitions in the canonical (#180) save order of
	 * {@link #transitionSaveOrder()}: unconditional first, then "else",
	 * then conditionals by (signal, eq flag, value, bits). The HDL
	 * exporter (issue #59) walks transitions through this accessor so
	 * the emitted if/else-if chains are deterministic and match the
	 * order the file format pins, instead of the HashSet's
	 * identity-hash order.
	 *
	 * @return a fresh list of this state's transitions, canonically
	 *         ordered.
	 *
	 * @jls.testedby jls.hdl.HdlPolicyTest#stateMachineInitialStateTakesCodeZero()
	 * @jls.testedby jls.hdl.VerilogExportGoldenTest#assertGolden()
	 */
	public java.util.List<Transition> getTransitionsInSaveOrder() {

		java.util.List<Transition> ordered =
				new java.util.ArrayList<Transition>(trans);
		ordered.sort(transitionSaveOrder());
		return ordered;
	} // end of getTransitionsInSaveOrder method

	/**
	 * Save information about this state.
	 *
	 * @param output The PrintWriter to write to.
	 */
	public void save(PrintWriter output) {

		output.println(" String state \"" + name + "\"");
		output.println("  int x " + x);
		output.println("  int y " + y);
		output.println("  int diameter " + diameter);
		output.println("  int init " + (initial ? 1 : 0));
		for (Out out : outs) {
			output.println("  String output \"" + out.signal + "\"");
			output.println("   long value " + out.value);
			output.println("   int bits " + out.bits);
		}
		// canonical transition order (#180): trans is a HashSet and
		// Transition has no hashCode override, so direct iteration would
		// write the trans blocks in identity-hash order. Sort by the
		// transitions' own content instead.
		java.util.List<Transition> orderedTrans =
				new java.util.ArrayList<Transition>(trans);
		orderedTrans.sort(transitionSaveOrder());
		for (Transition tr : orderedTrans) {
			if (tr.unconditional)
				output.println("  String trans \"always\"");
			else if (tr.other)
				output.println("  String trans \"else\"");
			else {
				output.println("  String trans \"" + tr.signal + "\"");
				if (tr.equal)
					output.println("   int eq 0");
				else
					output.println("   int eq 1");
				output.println("   int value " + tr.value);
				output.println("   int bits " + tr.bits);
			}
			State trNext = tr.nextState;
			if (trNext == null) {
				throw new IllegalStateException(
						"saving a transition whose next state is not linked");
			}
			output.println("   String next \"" + trNext.getName() + "\"");
			for (jls.core.GridPoint p : tr.points) {
				output.println("    pair " + p.x() + " " + p.y());
			}
		}
	} // end of save method

	/**
	 * Set an int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {

		if (name.equals("x")) {
			x = value;
		} else if (name.equals("y")) {
			y = value;
		} else if (name.equals("diameter")) {
			diameter = value;
		} else if (name.equals("init")) {
			if (value == 0)
				initial = false;
			else
				initial = true;
		}
	} // end of setValue method

	/**
	 * Set an output String instance variable value (during a load).
	 *
	 * @param value The instance variable value.
	 */
	public void setOutputValue(String value) {

		buildOut = new Out();
		buildOut.signal = value;
		outs.add(buildOut);
	} // end of setOutputValue method

	/**
	 * Set an output int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setOutputValue(String name, int value) {

		if (name.equals("bits")) {
			Out out = buildOut;
			if (out == null) {
				throw new IllegalStateException(
						"output bits set before the output signal was read");
			}
			out.bits = value;
			Check ch = bitmap.get(out.signal);
			if (ch == null) {
				ch = new Check();
				ch.bits = out.bits;
				ch.isInput = false;
				ch.refs = 1;
				bitmap.put(out.signal,ch);
			}
			else {
				ch.refs += 1;
			}
		}
	} // end of setOutputValue method

	/**
	 * Set an output long instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setOutputValue(String name, long value) {

		if (name.equals("value")) {
			Out out = buildOut;
			if (out == null) {
				throw new IllegalStateException(
						"output value set before the output signal was read");
			}
			out.value = value;
		}
	} // end of setOutputValue method

	/**
	 * Set an transition String instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setTransValue(String name, String value) {

		if (name.equals("trans")) {
			buildTrans = new Transition();
			buildTrans.unconditional = false;
			buildTrans.other = false;
			if (value.equals("always"))
				buildTrans.unconditional = true;
			else if (value.equals("else"))
				buildTrans.other = true;
			else
				buildTrans.signal = value;
			trans.add(buildTrans);
		}
		else if (name.equals("next")) {

			Transition tr = buildTrans;
			if (tr == null) {
				throw new IllegalStateException(
						"transition next set before the transition was started");
			}

			// link to state if possible
			for (State state : machine.getStates()) {
				if (state.getName().equals(value)) {
					tr.nextState = state;
					return;
				}
			}

			// else save name for link to be set when state is read in
			tr.nextStateName = value;
		}
	} // end of setTransValue method

	/**
	 * Set a transition int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setTransValue(String name, int value) {

		Transition tr = buildTrans;
		if (tr == null) {
			throw new IllegalStateException(
					"transition property set before the transition was started");
		}
		if (name.equals("eq")) {
			if (value == 0)
				tr.equal = true;
			else
				tr.equal = false;
		}
		else if (name.equals("value")) {
			tr.value = value;
		}
		else if (name.equals("bits")) {
			tr.bits = value;
			Check ch = bitmap.get(tr.signal);
			if (ch == null) {
				if (!tr.unconditional && !tr.other) {
					ch = new Check();
					ch.bits = tr.bits;
					ch.isInput = true;
					ch.refs = 1;
					bitmap.put(tr.signal,ch);
				}
			}
			else {
				ch.refs += 1;
			}
		}
	} // end of setTransValue method

	/**
	 * Set a pair of int instance variable values (during a load).
	 *
	 * @param v1 The first value.
	 * @param v2 The second value.
	 */
	public void setTransPair(int v1, int v2) {

		Transition tr = buildTrans;
		if (tr == null) {
			throw new IllegalStateException(
					"transition pair set before the transition was started");
		}
		tr.points.add(new jls.core.GridPoint(v1,v2));

	} // end of setPair method

	/**
	 * Fix any transitions pointing at this state symbolically to real references.
	 */
	public void fixTrans() {

		// fix any transitions ending in this state
		for (State state : machine.getStates()) {
			for (Transition tr : state.trans) {
				if (tr == buildTrans)
					continue;	// catch it later
				if (tr.nextStateName != null && tr.nextStateName.equals(name)) {
					tr.nextState = this;
					tr.nextStateName = null;
				}
			}
		}
	} // end of fixTrans method

	/**
	 * Get the number of bits in a given input signal.
	 * If this state doesn't have a transition using that signal,
	 * return 0.
	 *
	 * @param signal The input signal name.
	 *
	 * @return the number of bits, or 0 if signal not used.
	 */
	public int inputBits(String signal) {

		for (Transition tran : trans) {
			if (tran.signal.equals(signal))
				return tran.bits;
		}
		return 0;
	} // end of inputBits method

	/**
	 * Get the number of bits in a given output signal.
	 * If this state doesn't have an output using that signal,
	 * return 0.
	 *
	 * @param signal The output signal name.
	 *
	 * @return the number of bits, or 0 if signal not used.
	 */
	public int outputBits(String signal) {

		for (Out out : outs) {
			if (signal.equals(out.signal))
				return out.bits;
		}
		return 0;
	} // end of inputBits method

	/**
	 * See if a given point is inside this state.
	 *
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 *
	 * @return true if it is, false if not.
	 */
	public boolean contains(int x, int y) {

		int r = diameter/2;
		if ((this.x-x)*(this.x-x)+(this.y-y)*(this.y-y) < r*r) {
			return true;
		}
		else {
			return false;
		}
	} // end of contains method

	/**
	 * See if this state overlaps another state.
	 *
	 * @param other the other state
	 *
	 * @return true if the states overlap, false if not.
	 */
	public boolean overlaps(State other) {

		int d = Geometry.STATE_DIAMETER;
		int ox = other.x;
		int oy = other.y;
		if ((x-ox)*(x-ox)+(y-oy)*(y-oy) <= d*d) {
			return true;
		}
		else {
			return false;
		}
	} // end of overlaps method

	/**
	 * See if this state is completely inside the given rectangle.
	 *
	 * @param rect The rectangle.
	 *
	 * @return true if it is inside, false if not.
	 */
	public boolean isInside(jls.core.Bounds rect) {

		return rect.contains(getRect());
	} // end of isInside method

	/**
	 * Set/reset highlight property of this state.
	 *
	 * @param which True to highlight, false not to.
	 */
	public void setHighlight(boolean which) {

		highlight = which;
	} // end of setHighlight method

	/**
	 * Move state by a given amount.
	 * Also move every point in this state's transitions if the
	 * end state of those transitions is also selected.
	 *
	 * @param dx The distance to move in the x direction.
	 * @param dy The distance to move in the y direction.
	 * @param selected The selected states.
	 */
	public void move(int dx, int dy, Set<State> selected) {

		x += dx;
		y += dy;
		for (Transition tr : trans) {
			if (selected.contains(tr.nextState)) {
				for (int i = 0; i < tr.points.size(); i += 1) {
					jls.core.GridPoint p = tr.points.get(i);
					tr.points.set(i, new jls.core.GridPoint(p.x()+dx, p.y()+dy));
				}
			}
		}
	} // end of move method

	/**
	 * Move state to new position.
	 *
	 * @param x The new x-coordinate.
	 * @param y The new y-coordinate.
	 */
	public void moveTo(int x, int y) {

		this.x = x;
		this.y = y;
	} // end of moveTo method

	/**
	 * Save the current position of this state.
	 */
	public void savePosition() {

		savex = x;
		savey = y;
	} // end of savePosition method

	/**
	 * Restore the saved position of this state.
	 */
	public void restorePosition() {

		x = savex;
		y = savey;
	} // end of restorePosition method

	/**
	 * Get bounding rectangle for this state.
	 *
	 * @return the bounding rectangle.
	 */
	public jls.core.Bounds getRect() {

		int d = diameter;
		int r = d/2;
		return new jls.core.Bounds(x-r,y-r,d,d);
	} // end of getRect method

	/**
	 * Get the name of this state.
	 *
	 * @return the name.
	 */
	public String getName() {

		return name;
	} // end of getName method

	/**
	 * See if this state has any transitions.
	 *
	 * @return true if it does, false if it does not.
	 */
	public boolean hasTransitions() {

		return !trans.isEmpty();
	} // end of hasTransitions method

	/**
	 * Change the name of this state if it fits.
	 *
	 * @param name The new name.
	 * @param g The Graphics object to use.
	 *
	 * @return true if the name fits and was changed, false if not.
	 */
	public boolean changeName(String name, jls.core.TextMetrics g) {

		jls.core.TextMetrics fm = g;
		int w = fm.stringWidth("  " + name + "  ");
		if (w > diameter) {
			return false;
		}
		else {
			this.name = name;
			return true;
		}
	} // end of changeName method

	/**
	 * Validate a filled-in transition against this state's existing
	 * transitions and, if consistent, add it. Extracted from the former
	 * {@code newTransition} (issue #77): the GUI-side transition dialog
	 * ({@code jls.edit.StateMachineDialog}) builds and fills the
	 * {@link Transition}, then hands it here for the headless
	 * model-consistency checks and the bitmap bookkeeping.
	 *
	 * @param newTrans The filled-in transition to add.
	 * @param mainDialog The dialog to display error messages in.
	 */
	/**
	 * Build (but do not yet add) a candidate transition from this state to
	 * another (issue #77): the pre-dialog half of the former
	 * {@code newTransition}. The GUI-side dialog fills in the returned
	 * transition's condition, then hands it back to
	 * {@link #addTransition}. Returns null for a self-transition with
	 * fewer than three midpoints (it would not look good), matching the
	 * former behavior.
	 *
	 * @param to The state the transition is to.
	 * @param points The midpoints (the last one is not used).
	 *
	 * @return the candidate transition, or null if it should not be made.
	 */
	public @org.jspecify.annotations.Nullable Transition buildTransition(State to, Vector<jls.core.GridPoint> points) {

		// if transition is to the same state, then make sure
		// there are at least three points in the transition else
		// it won't look good
		if (this == to) {
			if (points.size() < 3) {
				return null;
			}
		}

		// make a new transition
		Transition newTrans = new Transition();
		newTrans.nextState = to;
		newTrans.signal = "";
		newTrans.bits = -1;
		for (int i=0; i<points.size()-1; i+=1) {
			newTrans.points.add(points.get(i));
		}

		// load transition with known info
		for (Transition oldTrans : trans) {
			if (!oldTrans.signal.isEmpty()) {
				newTrans.signal = oldTrans.signal;
			}
			if (oldTrans.bits != 0) {
				newTrans.bits = oldTrans.bits;
			}
		}
		return newTrans;
	} // end of buildTransition method

	/**
	 * Validate a filled-in transition against this state's existing
	 * transitions and, if consistent, add it.
	 *
	 * @param newTrans The filled-in transition to add.
	 */
	public void addTransition(Transition newTrans) {

		{

			// get properties of existing transition set
			boolean hasUnconditional = false;
			boolean hasOther = false;
			String signal = "";
			BitSet tested = new BitSet();
			int bits = 0;
			for (Transition oldTrans : trans) {
				if (oldTrans.unconditional) {
					hasUnconditional = true;
				}
				else if (oldTrans.other) {
					hasOther = true;
				}
				else if (oldTrans.equal) {
					signal = oldTrans.signal;
					bits = oldTrans.bits;
					tested.set(oldTrans.value);
				}
				else {
					signal = oldTrans.signal;
					bits = oldTrans.bits;
					BitSet temp = new BitSet();
					temp.set(oldTrans.value);
					temp.flip(0,1<<bits);
					tested.or(temp);
				}
			}

			// now test new transition for problems
			if (hasUnconditional) {
				TellUser.error(null,
						"State already has an unconditional transition",
						"Error");
				return;
			}
			if (newTrans.unconditional && !trans.isEmpty()) {
				TellUser.error(null,
						"Can't add an unconditional transition",
						"Error");
				return;
			}
			if (newTrans.other) {
				if (trans.isEmpty()) {
					TellUser.error(null,
							"State can't have just an else transition",
							"Error");
					return;
				}
				if (hasOther) {
					TellUser.error(null,
							"State already has an else transition",
							"Error");
					return;
				}
				if (tested.cardinality() == 1<<bits) {
					TellUser.error(null,
							"Existing transitions cover all possible cases",
							"Error");
					return;
				}
				trans.add(newTrans);
				return;
			}
			if (!signal.isEmpty() && !signal.equals(newTrans.signal)) {
				TellUser.error(null,
						"Can't test different signals in same state",
						"Error");
				return;
			}
			if (bits > 0 && bits != newTrans.bits) {
				TellUser.error(null,
						"Bits differ",
						"Error");
				return;
			}

			// check for inconsistent use of signal throughout state machine
			Check ch = bitmap.get(newTrans.signal);
			if (ch == null) {
				if (!newTrans.unconditional && !newTrans.other) {
					ch = new Check();
					ch.bits = newTrans.bits;
					ch.isInput = true;
					ch.refs = 1;
					bitmap.put(newTrans.signal,ch);
				}
			}
			else {
				if (!ch.isInput) {
					TellUser.error(null,
							"This signal is already an output", "Error");
					return;
				}
				if (ch.bits != newTrans.bits) {
					TellUser.error(null,
							"Bits not consistent with previous use of this signal",
							"Error");
					return;
				}
				ch.refs += 1;
			}
			if (newTrans.equal) {
				if (tested.get(newTrans.value)) {
					TellUser.error(null,
							"This value already tested in an existing transition",
							"Error");
					return;
				}
				tested.set(newTrans.value);
				if (hasOther && tested.cardinality() == 1<<bits) {
					TellUser.error(null,
							"New transition would make else transition impossible",
							"Error");
					return;
				}
				trans.add(newTrans);
				return;
			}
			if (!newTrans.equal) {
				if (hasOther) {
					TellUser.error(null,
							"New transition would make else transition impossible",
							"Error");
					return;
				}
				BitSet temp = new BitSet();
				temp.set(newTrans.value);
				temp.flip(0,1<<bits);
				if (temp.intersects(tested)) {
					TellUser.error(null,
							"These values already tested in existing transition(s)",
							"Error");
					return;
				}
				trans.add(newTrans);
			}
		}
	} // end of newTransition method

	/**
	 * Turn on/off initial state status.
	 * Not responsible for turning off initial state status in another state.
	 *
	 * @param which True to make this state the initial state, false to make it
	 * 				not be initial.
	 */
	public void setInitial(boolean which) {

		initial = which;
	} // end of setInitial method

	/**
	 * Get the location of the center of this state.
	 *
	 * @return The x,y coordinates of the center.
	 */
	public jls.core.GridPoint getLocation() {

		return new jls.core.GridPoint(x,y);
	} // end of getLocation method

	/**
	 * Remove all transitions from this state to the given state
	 * (because the given state is being removed).
	 *
	 * @param other The state being removed.
	 */
	public void removeTrans(State other) {

		Set <Transition> removes = new HashSet<Transition>();
		for (Transition tr : trans) {
			if (tr.nextState == other) {
				removes.add(tr);

				// clean up signal use info
				Check ch = bitmap.get(tr.signal);
				if (ch != null) {
					ch.refs -= 1;
					if (ch.refs == 0) {
						bitmap.remove(tr.signal);
					}
				}
			}
		}
		trans.removeAll(removes);
	} // end of removeTrans method

	/**
	 * Highlight any transition corner that is close to a given point.
	 *
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 *
	 * @return the transition whose corner is highlighted, else null. Its
	 *         highlighted corner can then be dragged via {@link
	 *         Transition#moveHighlight(int,int)} (issue #77).
	 */
	public @org.jspecify.annotations.Nullable Transition highlightTransPoints(int x, int y) {

		int ds = Geometry.POINT_DIAMETER*Geometry.POINT_DIAMETER;
		for (Transition tr : trans) {
			tr.highlight = null;
			tr.highlightIndex = -1;
			for (int i = 0; i < tr.points.size(); i += 1) {
				jls.core.GridPoint p = tr.points.get(i);
				if ((p.x()-x)*(p.x()-x)+(p.y()-y)*(p.y()-y) < ds) {
					tr.highlight = p;
					tr.highlightIndex = i;
					return tr;
				}
			}
		}
		return null;
	} // end of highlightTransPoints method

	/**
	 * Highlight any transition that has a line segment close to a given point.
	 * Also save reference to the highlighted transition so it can be deleted
	 * if the user wants to.
	 *
	 * @param xp The x-coordinate of the given point.
	 * @param yp The y-coordinate of the given point.
	 *
	 * @return true if some transition is highlighted, false if none
	 */
	public boolean highlightTrans(int xp, int yp) {

		// for all transitions...
		int d = Geometry.POINT_DIAMETER;
		for (Transition tran : trans) {

			// un-highlight it
			tran.highlighted = false;

			// the next state is linked by the time the machine is drawn
			State next = tran.nextState;
			if (next == null) {
				throw new IllegalStateException(
						"highlighting a transition whose next state is not linked");
			}

			// if a single segment line...
			if (tran.points.isEmpty()) {
				double maind =
					jls.core.SegmentGeometry.ptSegDist(x,y,next.x,next.y,xp,yp);
				if (maind < d && !contains(xp,yp) && !next.contains(xp,yp)) {
					tran.highlighted = true;
					lastHighlighted = tran;
					return true;
				}
			}
			else {

				// check out first segment
				jls.core.GridPoint p = tran.points.get(0);
				double maind = jls.core.SegmentGeometry.ptSegDist(x,y,p.x(),p.y(),xp,yp);
				if (maind < d && !contains(xp,yp)) {
					tran.highlighted = true;
					lastHighlighted = tran;
					return true;
				}

				// check out middle segments
				for (int i=1; i<tran.points.size(); i+=1) {
					jls.core.GridPoint p1 = tran.points.get(i-1);
					jls.core.GridPoint p2 = tran.points.get(i);
					maind = jls.core.SegmentGeometry.ptSegDist(p1.x(),p1.y(),p2.x(),p2.y(),xp,yp);
					if (maind < d) {
						tran.highlighted = true;
						lastHighlighted = tran;
						return true;
					}
				}

				// check out last segment
				p = tran.points.get(tran.points.size()-1);
				maind = jls.core.SegmentGeometry.ptSegDist(p.x(),p.y(),next.x,next.y,xp,yp);
				if (maind < d && !next.contains(xp,yp)) {
					tran.highlighted = true;
					lastHighlighted = tran;
					return true;
				}
			}
		}
		return false;
	} // end of highlightTrans method

	/**
	 * Delete last highlighted transition.
	 */
	public void deleteLastHighlighted() {

		// nothing to delete unless a transition was highlighted
		Transition last = lastHighlighted;
		if (last == null) {
			return;
		}

		// clean up signal use info
		Check ch = bitmap.get(last.signal);
		if (ch != null) {
			ch.refs -= 1;
			if (ch.refs == 0) {
				bitmap.remove(last.signal);
			}
		}

		// remove transition
		trans.remove(last);

		// if all that's left is an else transition, make it be unconditional
		if (trans.size() == 1) {
			for (Transition tran : trans) {
				if (tran.other) {
					tran.other = false;
					tran.unconditional = true;
				}
			}
		}
	} // end of deleteLastHighlighted method

	/**
	 * Delete all transitions from this state.
	 */
	public void deleteAllTrans() {

		// clean up input bit map
		for (Transition tran : trans) {
			Check ch = bitmap.get(tran.signal);
			if (ch != null) {
				ch.refs -= 1;
				if (ch.refs == 0) {
					bitmap.remove(tran.signal);
				}
			}
		}

		trans.clear();
	} // end of deleteAllTrans method

	/**
	 * Get the maximum width (in pixels) of all input and output names in
	 * this state.
	 *
	 * @param fm A FontMetrics object to use.
	 *
	 * @return the maximum width.
	 */
	public int getWidthInfo(jls.core.TextMetrics fm) {

		int width = 0;
		for (Out out : outs) {
			String sig = out.signal;
			if (sig == null) {
				throw new IllegalStateException(
						"output signal name not set");
			}
			width = Math.max(width,fm.stringWidth(sig));
		}
		for (Transition tr : trans) {
			width = Math.max(width,fm.stringWidth(tr.signal));
		}
		return width;
	} // end of getWidthInfo method

	/**
	 * Get set of all input signals used by transitions from this state.
	 *
	 * @return the set of input signal names.
	 */
	public Set<String> getInputs() {

		Set<String> inputs = new HashSet<String>();
		for (Transition tr : trans) {
			if (!tr.signal.isEmpty() && !tr.signal.equals("else"))
				inputs.add(tr.signal);
		}
		return inputs;
	} // end of getInputs method

	/**
	 * Get set of all output signals asserted by this state.
	 *
	 * @return the set of output signal names.
	 */
	public Set<String> getOutputs() {

		Set<String> outputs = new HashSet<String>();
		for (Out out : outs) {
			String sig = out.signal;
			if (sig == null) {
				throw new IllegalStateException(
						"output signal name not set");
			}
			outputs.add(sig);
		}
		return outputs;
	} // end of getInputs method

	//-----------------------------
	// simulation
	//-----------------------------

	/**
	 * Get the next state from a given one given the current input values.
	 *
	 * @return The next state according, or null of no next state.
	 */
	public @org.jspecify.annotations.Nullable State getNextState() {

		// set up default return value
		State newState = null;

		// check transitions looking for match
		for (Transition tran : trans) {


			// if unconditional, then simply go to its next state
			if (tran.unconditional)
				return tran.nextState;

			// if "else", save nextState in case no other transition matches
			else if (tran.other) {
				newState = tran.nextState;
				continue;
			}

			// if a match with a conditional, then go to its next state
			else {
				BitSet inp = machine.getInput(tran.signal).getValue();
				if (inp ==  null)
					inp = new BitSet();
				long inputValue = BitSetUtils.ToLong(inp);
				if (tran.equal && inputValue == tran.value)
					return tran.nextState;
				else if (!tran.equal && inputValue != tran.value)
					return tran.nextState;
			}
		}

		// return the default (null or "else")
		return newState;
	} // end of getNextState method

	/**
	 * Send all out values of this state to the state machine outputs.
	 * Unspecified outputs will be made 0.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator.
	 */
	public void sendOutputs(long now, Simulator sim) {

		Set<Output> sent = new HashSet<Output>();

		// send out explicitly specified values
		for (Out out : outs) {
			String sig = out.signal;
			if (sig == null) {
				throw new IllegalStateException(
						"output signal name not set");
			}
			Output output = machine.getOutput(sig);
			BitSet value = BitSetUtils.Create(out.value);
			output.propagate(value,now,sim);
			sent.add(output);
		}

		// send 0 to all unspecified outputs
		for (Output output : machine.getOutputs()) {
			if (!sent.contains(output)) {
				output.propagate(new BitSet(),now,sim);
			}
		}
	} // end of sendOutputs method

	/**
	 * Get the x-coordinate of this state.
	 *
	 * @return the x-coordinate.
	 */
	public int getX() {

		return x;
	} // end of getX method

	/**
	 * Get the diameter of the circle representing this state (issue #77:
	 * the GUI-side renderer reads it to draw the circle and lay out
	 * transition endpoints).
	 *
	 * @return the diameter.
	 */
	public int getDiameter() {

		return diameter;
	} // end of getDiameter method

	/**
	 * Get this state's transitions (issue #77: the GUI-side renderer and
	 * dialog iterate them to draw and edit the machine).
	 *
	 * @return the set of transitions.
	 */
	public Set<Transition> getTransitions() {

		return trans;
	} // end of getTransitions method

	/**
	 * Get every state in this state's machine (issue #77: the GUI-side
	 * transition dialog scans them to find an existing signal's bit
	 * width).
	 *
	 * @return the machine's states.
	 */
	public Set<State> getMachineStates() {

		return machine.getStates();
	} // end of getMachineStates method

	/**
	 * Get this state's asserted outputs (issue #77: the GUI-side output
	 * dialog and print summary read them).
	 *
	 * @return the outputs.
	 */
	public Vector<Out> getOuts() {

		return outs;
	} // end of getOuts method

	/**
	 * Validate and add an output to this state (issue #77): the
	 * GUI-side output dialog collects the signal/value/bits and hands
	 * them here for the headless conflict and bitmap-consistency checks.
	 *
	 * @param signal The (trimmed) output signal name.
	 * @param value The value the signal is set to.
	 * @param bits The number of bits in the signal.
	 *
	 * @return the added output, or null if the input was rejected.
	 */
	public @org.jspecify.annotations.Nullable Out addOutput(String signal, long value, int bits) {

		if (signal.isEmpty()) {
			TellUser.error(null, "Missing signal name", "Error");
			return null;
		}
		if (Math.log(value+1)/Math.log(2) > bits) {
			TellUser.error(null,
					"Value too large for number of bits", "Error");
			return null;
		}

		// make sure this output doesn't conflict with existing outputs
		// in this state
		for (Out out : outs) {
			if (signal.equals(out.signal)) {
				TellUser.error(null,
						"Already an output with this name", "Error");
				return null;
			}
		}

		// check for consistency for this signal throughout state machine
		Check ch = bitmap.get(signal);
		if (ch == null) {
			ch = new Check();
			ch.bits = bits;
			ch.isInput = false;
			ch.refs = 1;
			bitmap.put(signal, ch);
		}
		else {
			if (ch.isInput) {
				TellUser.error(null,
						"This signal is already an input", "Error");
				return null;
			}
			if (ch.bits != bits) {
				TellUser.error(null,
						"Bits not consistent with previous use of this signal",
						"Error");
				return null;
			}
			ch.refs += 1;
		}
		Out newOut = new Out();
		newOut.signal = signal;
		newOut.value = value;
		newOut.bits = bits;
		outs.add(newOut);
		return newOut;
	} // end of addOutput method

	/**
	 * Delete the output at the given index and update the signal bitmap
	 * (issue #77: the headless half of the former {@code EditOutputs}
	 * delete button).
	 *
	 * @param pos The index of the output to remove.
	 */
	public void deleteOutput(int pos) {

		Out out = outs.get(pos);
		Check ch = bitmap.get(out.signal);
		if (ch != null) {
			ch.refs -= 1;
			if (ch.refs == 0) {
				bitmap.remove(out.signal);
			}
		}
		outs.remove(pos);
	} // end of deleteOutput method

	/**
	 * Get the y-coordinate of this state.
	 *
	 * @return the y-coordinate.
	 */
	public int getY() {

		return y;
	} // end of getY method

	/**
	 * Set the x-coordinate of this state
	 *
	 * @param newx The new x-coordinate.
	 */
	public void setX(int newx) {

		x = newx;
	} // end of setX method


	/**
	 * Set the y-coordinate of this state
	 *
	 * @param newy The new y-coordinate.
	 */
	public void setY(int newy) {

		y = newy;
	} // end of setY method

	/**
	 * See if this state is the initial state.
	 *
	 * @return true if it is, false if it is not.
	 */
	public boolean isInitial() {

		return initial;
	} // end of isInitial method

	/**
	 * See if this state is drawn highlighted (issue #77: the GUI-side
	 * renderer reads it to pick the fill).
	 *
	 * @return true if highlighted, false if not.
	 */
	public boolean isHighlighted() {

		return highlight;
	} // end of isHighlighted method

	/**
	 * Get total number of output signals from this state.
	 *
	 * @return the total number of output signals.
	 */
	public int numOuts() {

		return outs.size();
	} // end of numOuts method

} // end of State class
