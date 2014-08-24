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

		/*
		 System.out.print(now + ": post called for element " +
		 ((LogicElement)(event.getCallBack())).getName());
		 */
		if (dupCheck.add(event)) {
			// System.out.print(" - added to queue");
			eventQueue.add(event);
		}
		// System.out.println();
	} // end of post method

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
