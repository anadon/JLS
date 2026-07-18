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
	/** Pending events, ordered by (time, seq) - see SimEvent.compareTo. */
	protected PriorityQueue<SimEvent> eventQueue = new PriorityQueue<SimEvent>();
	/** The pending events, for duplicate suppression in post(). */
	protected Set<SimEvent>dupCheck = new HashSet<SimEvent>();
	/** The circuit being simulated. */
	protected Circuit circuit = null;
	/** The current simulation time. */
	protected long now = 0;
	/** The simulation time limit. */
	protected long maxTime = JLSInfo.defaultTimeLimit;
	/**
	 * Whether a stop has been requested. Volatile: set from the EDT
	 * (Stop button), read in the sim thread's event loop (issue #49,
	 * finding H7).
	 */
	protected volatile boolean stopping = false;
	/** This simulator, as passed to element initSim/react callbacks. */
	protected Simulator me = null;
	/** The test-vector file name, or null if none. */
	protected String testFileName = null;

	/**
	 * Create a simulator. Subclasses set {@link #me} so element
	 * callbacks receive the concrete simulator.
	 */
	protected Simulator() {
	} // end of constructor


	/**
	 * Set the circuit that will be simulated.
	 * 
	 * @param circ The circuit.
	 * @see jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @see jls.BatchSimulationGoldenTest#simulate()
	 * @see jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @see jls.ElementSimulationGoldenTest#simulate()
	 * @see jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @see jls.SequentialGoldenTest#simulate()
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.ShiftRegisterTest#simulate()
	 * @see jls.SimulationSemanticsRegressionTest#agreeingTriStateDriversDoNotWarn()
	 * @see jls.SimulationSemanticsRegressionTest#constantValueIsMaskedToTheNetWidth()
	 * @see jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 * @see jls.SimulationSemanticsRegressionTest#multiDriverConflictResolvesDeterministicallyAndWarnsOnce()
	 * @see jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 * @see jls.SimulationSemanticsRegressionTest#stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
	 * @see jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 * @see jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 * @see jls.elem.MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()
	 */
	public void setCircuit(Circuit circ) {

		circuit = circ;
	} // end of setCircuit method

	/**
	 * Set the time limit for the simulation.
	 * 
	 * @param limit The time limit.
	 * @see jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 * @see jls.BatchSimulationGoldenTest#simulate()
	 * @see jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 * @see jls.ElementSimulationGoldenTest#simulate()
	 * @see jls.ElementSimulationGoldenTest#stopHaltsTheSimulationEarly()
	 * @see jls.SequentialGoldenTest#simulate()
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.ShiftRegisterTest#simulate()
	 * @see jls.SimulationSemanticsRegressionTest#agreeingTriStateDriversDoNotWarn()
	 * @see jls.SimulationSemanticsRegressionTest#constantValueIsMaskedToTheNetWidth()
	 * @see jls.SimulationSemanticsRegressionTest#multiDriverConflictResolvesDeterministicallyAndWarnsOnce()
	 * @see jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 * @see jls.SimulationSemanticsRegressionTest#stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
	 * @see jls.VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()
	 * @see jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 * @see jls.elem.MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()
	 */
	public void setTimeLimit(long limit) {

		maxTime = limit;
	} // end of setTimeLimit method

	/**
	 * Set the simulation test input file name.
	 * 
	 * @param name The test file name, or null if none.
	 * @see jls.SequentialGoldenTest#simulateWithVectors()
	 * @see jls.SimulationSemanticsRegressionTest#stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce()
	 * @see jls.VcdExportGoldenTest#testVectorStimulusVcdMatchesGoldenAndCoversHiZ()
	 */
	public void setTestFile(String name) {

		testFileName = name;
	} // end of setTestFile method

	/**
	 * Initialize all inputs in the circuit.
	 * 
	 * @param circuit The circuit to initialize.
	 * @see jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 */
	public void initInputs(Circuit circuit) {

		for (Element element : circuit.getElementsInStableOrder()) {
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
	 *
	 * @see jls.SimulationSemanticsRegressionTest.CountingSimulator#post()
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

		// initialize all elements, in the circuit's canonical stable-id
		// order (#181): initSim posts the time-0 events, and same-time
		// events fire in posting order (SimEvent seq), so seeding from
		// the element HashSet would make order-sensitive circuits -
		// cross-coupled latches, multi-driver nets - settle differently
		// between runs. Stable-id order makes the seed, and with it
		// every simulated value, a pure function of circuit content.
		for (Element el : circuit.getElementsInStableOrder()) {
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

			beforeReact();

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
	 */
	protected void beforeReact() {
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
