package jls.sim;

import jls.*;
import jls.elem.*;
import jls.edit.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import javax.print.PrintService;
import javax.swing.*;
import javax.swing.text.AbstractDocument;

import java.util.*;

/**
 * Event driven circuit simulator.
 *
 * @author David A. Poplawski
 */
public abstract class Simulator {

	// properties
	protected PriorityQueue<SimEvent> eventQueue = new PriorityQueue<SimEvent>();
	protected Set<SimEvent>dupCheck = new HashSet<SimEvent>();
	protected Circuit circuit = null;
	protected long now = 0;
	protected long maxTime = JLSInfo.defaultTimeLimit;
	protected boolean stopping = false;
	protected Simulator me = null;
	protected String testFileName = null;


	/**
	 * Set the circuit that will be simulated.
	 * 
	 * @param circ The circuit.
	 */
	public void setCircuit(Circuit circ) {

		circuit = circ;
	} // end of setCircuit method

	/**
	 * Set the time limit for the simulation.
	 * 
	 * @param limit The time limit.
	 */
	public void setTimeLimit(long limit) {

		maxTime = limit;
	} // end of setTimeLimit method

	/**
	 * Set the simulation test input file name.
	 * 
	 * @param name The test file name, or null if none.
	 */
	public void setTestFile(String name) {

		testFileName = name;
	} // end of setTestFile method

	/**
	 * Initialize all inputs in the circuit.
	 * 
	 * @param circuit The circuit to initialize.
	 */
	public void initInputs(Circuit circuit) {

		for (Element element : circuit.getElements()) {
			if (element instanceof LogicElement) {
				LogicElement el = (LogicElement)element;
				el.initInputs();
			}
		}
	} // end of initInputs method

	/**
	 * Enqueue an event.
	 *
	 * @param event The event to enqueue.
	 */
	public void post(SimEvent event) {

		if (dupCheck.add(event)) {
			eventQueue.add(event);
		}
	} // end of post method

	/**
	 * Reset the simulation state and initialize every element:
	 * clock to 0, queues emptied, all input points and logic elements
	 * (re)initialized. The common preamble of every simulation run.
	 */
	protected void initSimulation() {

		stopping = false;
		now = 0;
		eventQueue.clear();
		dupCheck.clear();

		// initialize all input points
		initInputs(circuit);

		// initialize all elements
		for (Element el : circuit.getElements()) {
			if (el instanceof LogicElement) {
				LogicElement lel = (LogicElement)el;
				lel.initSim(me);
			}
		}
	} // end of initSimulation method

	/**
	 * The event loop, shared by every simulation mode (#25): dequeue the
	 * next event, advance the clock, enforce the time limit, and let the
	 * event react. Mode-specific behavior (pausing, stepping, tracing,
	 * display updates) is expressed through the hooks below; the loop
	 * itself must stay mode-agnostic.
	 *
	 * The head event is polled only after beforeEvent() approves; a hook
	 * that needs the upcoming event's time (e.g. stepping) may peek. The
	 * queue is only modified on this thread, so peek-then-poll returns
	 * the same event.
	 */
	protected void runEventLoop() {

		while (!stopping && !eventQueue.isEmpty() && now <= maxTime) {

			// let the mode pause/step; re-check loop conditions if it did
			if (!beforeEvent())
				continue;

			// get the next event
			SimEvent event = eventQueue.poll();
			dupCheck.remove(event);

			// update clock
			now = event.getTime();

			// quit if after time limit
			if (now > maxTime) {
				now = maxTime;
				break;
			}

			beforeReact(event);

			// make the event happen
			event.getCallBack().react(now,me,event.getTodo());

			afterEvent(event);
		}
	} // end of runEventLoop method

	/**
	 * Hook called before the next event is dequeued. A mode can block
	 * (pause), or set state and decline this iteration.
	 *
	 * @return true to proceed with the next event, false to re-check the
	 *         loop conditions from the top.
	 */
	protected boolean beforeEvent() {

		return true;
	} // end of beforeEvent method

	/**
	 * Hook called after the clock has advanced to the event's time but
	 * before the event reacts (e.g. to update a clock display).
	 *
	 * @param event The event about to react.
	 */
	protected void beforeReact(SimEvent event) {
	} // end of beforeReact method

	/**
	 * Hook called after the event has reacted (e.g. to record traces).
	 *
	 * @param event The event that just reacted.
	 */
	protected void afterEvent(SimEvent event) {
	} // end of afterEvent method

	/**
	 * Stop the simulation.
	 */
	public abstract void stop();

	/**
	 * Pause or resume the simulation.
	 *
	 * @param which True to pause the simulation, false to resume it.
	 */
	public abstract void pause(boolean which);

} // end of Simulator class
