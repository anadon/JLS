package jls.elem;

import java.io.*;
import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import jls.*;
import jls.core.Geometry;
import jls.core.GridPoint;
import jls.core.Orientation;
import jls.sim.*;

/**
 * Clock element.
 *
 * @author David A. Poplawski
 */
public final class Clock extends LogicElement {

	// default values
	/** The cycle time offered by the creation dialog's keypad. */
	private static int defaultCycleTime = 2;
	/** The one time offered by the creation dialog's keypad. */
	private static int defaultOneTime = defaultCycleTime/2;

	// one constraint string, two surfaces: dialog and loader (issue #52);
	// non-positive times cause a zero-delay repost livelock at t=0
	/** Constraint message for a non-positive cycle time. */
	static final String CYCLE_CONSTRAINT =
			"Cycle time must be a positive number of time units";
	/** Constraint message for an invalid one time. */
	static final String ONE_CONSTRAINT =
			"One time must be positive and less than the cycle time";

	/**
	 * The cycle-time rule, shared by the dialog and the loader.
	 *
	 * @param cycleTime The proposed cycle time.
	 *
	 * @return the violated constraint message, or null if valid.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#clockCycleTimeRuleIsOneStringOnTwoSurfaces()
	 */
	public static @Nullable String checkCycleTime(int cycleTime) {

		return cycleTime < 1 ? CYCLE_CONSTRAINT : null;
	} // end of checkCycleTime method

	/**
	 * The one-time rule as the dialog enforces it. The loader checks only
	 * positivity (it cannot see both fields at once for legacy files) but
	 * rejects with the same constraint string.
	 *
	 * @param cycleTime The proposed cycle time.
	 * @param oneTime The proposed one time.
	 *
	 * @return the violated constraint message, or null if valid.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#clockOneTimeRuleIsOneStringOnTwoSurfaces()
	 */
	public static @Nullable String checkOneTime(int cycleTime, int oneTime) {

		return (oneTime < 1 || oneTime >= cycleTime) ? ONE_CONSTRAINT : null;
	} // end of checkOneTime method

	// properties
	/** The clock period, in simulated time units. */
	private int cycleTime = defaultCycleTime;
	/** The number of time units per cycle the output is 1. */
	private int oneTime = defaultOneTime;
	/** Which way the output pin faces. */
	private Orientation orientation = Orientation.RIGHT;

	/**
	 * Create a new clock element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public Clock(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The direction this clock's output pin faces (issue #77: read by the
	 * GUI-side dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the cycle time (issue #77: applied by the GUI-side dialog).
	 *
	 * @param cycleTime The new cycle time.
	 */
	public void setCycleTime(int cycleTime) {

		this.cycleTime = cycleTime;
	} // end of setCycleTime method

	/**
	 * Set the one time (issue #77: applied by the GUI-side dialog).
	 *
	 * @param oneTime The new one time.
	 */
	public void setOneTime(int oneTime) {

		this.oneTime = oneTime;
	} // end of setOneTime method

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
	 * @param g Unused.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		// canonical geometry (RIGHT), transformed to the current
		// orientation (#24)
		int s = Geometry.SPACING;
		width = 2*s;
		height = 2*s;
		GridTransform.Chain t = GridTransform.chain(2*s, 2*s);
		switch (orientation) {
		case RIGHT:
			break;
		case LEFT:
			t.mirrorX();
			break;
		case UP:
			t.rotateCCW();
			break;
		case DOWN:
			t.rotateCW();
			break;
		}
		GridPoint p = t.map(2*s, s);
		outputs.add(new Output("output",this,p.x(),p.y(),1));

	} // end of init method

	/**
	 * Save thiselement.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT Clock");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Get the cycle time (period) of this clock, in simulated time
	 * units (for consumers of the circuit model, e.g. the HDL exporter,
	 * issue #60).
	 *
	 * @return the cycle time.
	 */
	public int getCycleTime() {

		return cycleTime;
	} // end of getCycleTime method

	/**
	 * Get the number of time units per cycle this clock's output is 1.
	 *
	 * @return the one time.
	 */
	public int getOneTime() {

		return oneTime;
	} // end of getOneTime method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes: cycle, one, orient (#23). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("cycle") {
			/**
			 * Read the cycle time from the given clock.
			 *
			 * @param el The clock to read.
			 *
			 * @return the cycle time.
			 */
			@Override
			protected int get(Element el) { return ((Clock)el).cycleTime; }
			/**
			 * Store a validated cycle time on the given clock.
			 *
			 * @param el The clock to update.
			 * @param v The new cycle time.
			 *
			 * @throws IllegalArgumentException if the cycle time is not positive.
			 */
			@Override
			protected void set(Element el, int v) {
				String violated = checkCycleTime(v);
				if (violated != null) {
					throw new IllegalArgumentException(violated);
				}
				((Clock)el).cycleTime = v;
			}
		},
		new Attribute.IntAttribute("one") {
			/**
			 * Read the one time from the given clock.
			 *
			 * @param el The clock to read.
			 *
			 * @return the one time.
			 */
			@Override
			protected int get(Element el) { return ((Clock)el).oneTime; }
			/**
			 * Store a validated one time on the given clock.
			 *
			 * @param el The clock to update.
			 * @param v The new one time.
			 *
			 * @throws IllegalArgumentException if the one time is not positive.
			 */
			@Override
			protected void set(Element el, int v) {
				if (v < 1) {
					throw new IllegalArgumentException(ONE_CONSTRAINT);
				}
				((Clock)el).oneTime = v;
			}
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the orientation from the given clock.
			 *
			 * @param el The clock to read.
			 *
			 * @return the orientation.
			 */
			@Override
			protected Orientation getOrientation(Element el) {
				return ((Clock)el).orientation;
			}
			/**
			 * Store the orientation on the given clock.
			 *
			 * @param el The clock to update.
			 * @param o The new orientation.
			 */
			@Override
			protected void setOrientation(Element el, Orientation o) {
				((Clock)el).orientation = o;
			}
		}
	);

	/** Base attributes plus this element's own, in save order (#23). */
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
	 */
	@Override
	public Element copy() {

		Clock it = new Clock(getCircuit());
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

		return "clock, cycle time = " + cycleTime +
				" (zero for " + (cycleTime-oneTime) + ", one for " + oneTime + ")";
	} // end of showInfo method

	/**
	 * Clock values can be changed.
	 *
	 * @return true.
	 */
	@Override
	public boolean canChange() {

		return true;
	} // end of canChange method

	/**
	 * Tells if a clock is capable of rotatating, can only rotate when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		return !outputs.get(0).isAttached();
	}

	/**
	 *  This method will rotate the clock if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(Orientation direction, jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		if(direction == Orientation.LEFT)
		{
			orientation = orientation.ccw();
		}
		else if(direction == Orientation.RIGHT)
		{
			orientation = orientation.cw();
		}
		outputs.remove(0);
		init(g);
	}

	/**
	 * Tells if a clock is capable of flipping, can only flip when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !outputs.get(0).isAttached();
	}

	/**
	 * This method will flip a clock
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g)
	{
		orientation = orientation.flipped();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		// set output pin
		Output out = outputs.get(0);
		BitSet zero = new BitSet(1);
		out.setValue(zero);
		BitSet one = new BitSet();
		one.flip(0);
		sim.post(new SimEvent(cycleTime-oneTime,this,one));

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

		// send new value; a Clock has no inputs, so it only ever
		// reacts to its own posted value events (todo is never null)
		BitSet send = (BitSet)todo;
		if (send == null) {
			throw new IllegalStateException(
					"clock reacted without a value to output");
		}
		Output out = outputs.get(0);
		out.propagate(send,now,sim);

		// post next event
		BitSet next = (BitSet)send.clone();
		next.flip(0);
		int when = oneTime;
		if (send.cardinality() == 0) {
			when = cycleTime - oneTime;
		}
		sim.post(new SimEvent(now+when,this,next));

	} // end of react method

} // end of Clock class
